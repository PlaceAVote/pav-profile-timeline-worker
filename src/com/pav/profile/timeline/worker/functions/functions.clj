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
						[com.pav.profile.timeline.worker.events.notifications :refer [new-comment-reply-notification]])
  (:import (java.util Date)))

(def es-conn (esr/connect (:es-url env)))

(def client-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn add-users-first-last-names [evt user_id]
  (-> (far/get-item client-opts user-table {:user_id user_id} {:attrs [:first_name :last_name]})
      (merge evt)))

(defn parse-vote [evt]
  (add-bill-title es-conn "congress" evt))

(defn parse-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (assoc :score 0 :user_id (:author evt))))

(defn parse-followinguser [evt]
  (add-users-first-last-names evt (:following_id evt)))

(defn parse-followedbyuser [evt]
  (add-users-first-last-names evt (:follower_id evt)))

(defn parse-like-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))))

(defn parse-dislike-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))))

(defn unpack-event [evt]
  (-> (msg/unpack evt)
      (ch/parse-string true)))

(defn parse-event [{:keys [type] :as event}]
	(case type
		"vote" (parse-vote event)
		"comment" (parse-comment event)
		"followinguser" (parse-followinguser event)
		"followedbyuser" (parse-followedbyuser event)
		"likecomment" (parse-like-comment event)
		"dislikecomment" (parse-dislike-comment event)
		nil))

(defn publish-to-dynamo-timeline [client-opts table-name evt]
  (try
    (far/put-item client-opts table-name evt)
    (catch Exception e (log/error (str "Error writing to table " table-name ", with " evt ", " e)))))

(defn publish-dynamo-notification [dynamo-opts notification-table notification]
	(far/put-item dynamo-opts notification-table notification))

(defn publish-reply-notifications [dynamo-opts notification-table comment-details-table {:keys [parent_id] :as evt}]
	(if parent_id
		(let [parent-comment (far/get-item dynamo-opts comment-details-table {:comment_id parent_id})]
			(when parent-comment
				(let [notification (new-comment-reply-notification parent-comment evt)]
					(publish-dynamo-notification dynamo-opts notification-table notification))))))

(defn publish-user-notifications [dynamo-opts notification-table comment-details-table {:keys [type] :as evt}]
	(case type
		"comment" (publish-reply-notifications dynamo-opts notification-table comment-details-table evt)
		nil))