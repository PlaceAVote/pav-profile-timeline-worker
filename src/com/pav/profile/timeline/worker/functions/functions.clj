(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [taoensso.faraday :as far]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [environ.core :refer [env]]
						[clojure.tools.logging :as log]
						[taoensso.carmine :as car :refer (wcar)]
						[clj-http.client :as client])
  (:import (java.util Date UUID)))

(defn add-event-id [evt]
	(assoc evt :event_id (.toString (UUID/randomUUID))))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn add-users-first-last-names [evt dynamo-opts user-table user_id]
  (-> (far/get-item dynamo-opts user-table {:user_id user_id} {:attrs [:first_name :last_name]})
      (merge evt)))

(defn add-users-email [{:keys [user_id] :as evt} dynamo-opts user-table]
	(log/info "EMAIL: " (far/get-item dynamo-opts user-table {:user_id user_id} {:attrs [:email]}))
	(-> (far/get-item dynamo-opts user-table {:user_id user_id} {:attrs [:email]})
		  (merge evt)))

(defn parse-vote [es-conn evt]
  (add-bill-title es-conn "congress" evt))

(defn parse-comment [es-conn evt]
  (-> (add-bill-title es-conn "congress" evt)
		  (merge {:liked false :disliked false})
      (assoc :score 0 :user_id (:author evt))))

(defn parse-followinguser [dynamo-opts user-table evt]
  (add-users-first-last-names evt dynamo-opts user-table (:following_id evt)))

(defn parse-followedbyuser [dynamo-opts user-table evt]
  (add-users-first-last-names evt dynamo-opts user-table (:follower_id evt)))

(defn parse-like-comment [es-conn dynamo-opts user-table evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names dynamo-opts user-table (:author evt))
      (assoc :timestamp (.getTime (Date.)))
			(merge {:liked true :disliked false})))

(defn parse-dislike-comment [es-conn dynamo-opts user-table evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names dynamo-opts user-table (:author evt))
      (assoc :timestamp (.getTime (Date.)))
			(merge {:liked false :disliked true})))

(defn parse-comment-reply-notification [es-conn dynamo-opts comment-details-table evt]
	(let [{author :author} (far/get-item dynamo-opts comment-details-table {:comment_id (:parent_id evt)}
												 {:return ["author"]})]
		(if (and author (not (= (:author evt) author)))
			(->> (assoc evt :user_id author :read false :notification_id (.toString (UUID/randomUUID)))
				   (add-bill-title es-conn "congress")))))

(defn parse-comment-reply-email-notification [es-conn dynamo-opts comment-details-table user-table evt]
	(-> (parse-comment-reply-notification es-conn dynamo-opts comment-details-table evt)
		  (add-users-email dynamo-opts user-table)))

(defn unpack-event [evt]
  (-> (msg/unpack evt)
      (ch/parse-string true)))

(defn pack-event [evt]
	(-> (ch/generate-string evt) msg/pack))

(defn publish-to-dynamo-timeline [dynamo-opts timeline-table timeline-event]
	(log/info "Timeline event being published " timeline-event)
  (try
    (far/put-item dynamo-opts timeline-table timeline-event)
	(catch Exception e (log/error (str "Error writing to table " timeline-table ", with " timeline-event ", " e)))))

(defn publish-dynamo-notification [dynamo-opts notification-table notification-event]
	(log/info "Notification event being published " notification-event)
	(try
		(far/put-item dynamo-opts notification-table notification-event)
	(catch Exception e (log/error (str "Error writing to table " notification-table ", with " notification-event ", " e)))))

(defn publish-redis-notification [redis-conn redis-topic event]
	(wcar redis-conn (car/publish redis-topic (pack-event event))))

(defn build-email-header [api-key template]
	{:key api-key :template_name template
	 :template_content [] :async true})

(defn build-comment-reply-body
	[message {:keys [email author_first_name author_last_name
									 author_img_url bill_title body]}]
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

(defn publish-comment-reply-email [api-key template event]
	(let [body (-> (build-email-header api-key template)
								 (build-comment-reply-body event)
							   ch/generate-string)]
		(log/info "Email Body being sent to mandril " body)
		(try
			(client/post "https://mandrillapp.com/api/1.0/messages/send-template.json" {:body body})
		(catch Exception e (log/error "Error sending email " e)))))

