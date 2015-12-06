(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest :as esr]
            [taoensso.faraday :as far]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [environ.core :refer [env]]
						[clojure.tools.logging :as log]
						[taoensso.carmine :as car :refer (wcar)]
						[clj-http.client :as client])
  (:import (java.util Date)))

(def es-conn (esr/connect (:es-url env)))

(def client-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))
(def timeline-table (:dynamo-usertimeline-table-name env))
(def notification-table (:dynamo-usernotification-table-name env))
(def comment-details-table (:dynamo-comment-details-table-name env))

(def mandril-api-key (:mandril-api-key env))
(def comment-reply-template (:mandril-comment-template env))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn add-users-first-last-names [evt user_id]
  (-> (far/get-item client-opts user-table {:user_id user_id} {:attrs [:first_name :last_name]})
      (merge evt)))

(defn add-users-email [{:keys [user_id] :as evt}]
	(-> (far/get-item client-opts user-table {:user_id user_id} {:attrs [:email]})
		(merge evt)))

(defn parse-vote [evt]
  (add-bill-title es-conn "congress" evt))

(defn parse-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
		  (merge {:liked false :disliked false})
      (assoc :score 0 :user_id (:author evt))))

(defn parse-followinguser [evt]
  (add-users-first-last-names evt (:following_id evt)))

(defn parse-followedbyuser [evt]
  (add-users-first-last-names evt (:follower_id evt)))

(defn parse-like-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))
			(merge {:liked true :disliked false})))

(defn parse-dislike-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))
			(merge {:liked false :disliked true})))

(defn parse-comment-reply-notification [evt]
	(let [{author :author} (far/get-item client-opts comment-details-table {:comment_id (:parent_id evt)}
												 {:return ["author"]})]
		(if author
			(->> (assoc evt :user_id author :read false)
				   (add-bill-title es-conn "congress")))))

(defn parse-comment-reply-email-notification [evt]
	(-> (parse-comment-reply-notification evt)
		  (add-users-email)))

(defn unpack-event [evt]
  (-> (msg/unpack evt)
      (ch/parse-string true)))

(defn publish-to-dynamo-timeline [timeline-event]
	(log/info "Timeline event being published " timeline-event)
  (try
    (far/put-item client-opts timeline-table timeline-event)
	(catch Exception e (log/error (str "Error writing to table " timeline-table ", with " timeline-event ", " e)))))

(defn publish-dynamo-notification [notification-event]
	(log/info "Notification event being published " notification-event)
	(try
		(far/put-item client-opts notification-table notification-event)
	(catch Exception e (log/error (str "Error writing to table " notification-table ", with " notification-event ", " e)))))

(defn build-email-header [template]
	{:key mandril-api-key :template_name template
	 :template_content [] :async true})

(defn build-comment-reply-body
	[message {:keys [email author_first_name author_last_name
									 author_img_url bill_title body] :as event}]
	(-> {:message {:to                [{:email email :type "to"}]
								 :important         false
								 :inline_css        true
								 :merge             true
								 :merge_language    "handlebars"
								 :global_merge_vars [{:name "author_first_name" :content author_first_name}
																		 {:name "author_last_name" :content author_last_name}
																		 {:name "author_img_url" :content author_img_url}
																		 {:name "bill_title" :content bill_title}
																		 {:name "body" :content body}]}}
		(merge message)))

(defn publish-comment-reply-email [event]
	(let [body (-> (build-email-header comment-reply-template)
								 (build-comment-reply-body event)
							   ch/generate-string)]
		(log/info "Email Body being sent to mandril " body)
		(try
			(client/post "https://mandrillapp.com/api/1.0/messages/send-template.json" {:body body})
		(catch Exception e (log/error "Error sending email " e)))))

