(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest :as esr]
            [taoensso.faraday :as far]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [environ.core :refer [env]]
						[clojure.tools.logging :as log]
						[taoensso.carmine :as car :refer (wcar)])
  (:import (java.util Date)))

(def es-conn (esr/connect (:es-url env)))

(def client-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))
(def timeline-table (:dynamo-usertimeline-table-name env))
(def notification-table (:dynamo-usernotification-table-name env))
(def comment-details-table (:dynamo-comment-details-table-name env))

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

(defn parse-comment-reply-notification [evt]
	(let [{author :author} (far/get-item client-opts comment-details-table {:comment_id (:parent_id evt)}
												 {:return ["author"]})]
		(if author
			(->> (assoc evt :user_id author :read false)
				   (add-bill-title es-conn "congress")))))

(defn unpack-event [evt]
  (-> (msg/unpack evt)
      (ch/parse-string true)))

(defn publish-to-dynamo-timeline [timeline-event]
  (try
    (far/put-item client-opts timeline-table timeline-event)
    (catch Exception e (log/error (str "Error writing to table " timeline-table ", with " timeline-event ", " e)))))

(defn publish-dynamo-notification [notification-event]
	(far/put-item client-opts notification-table notification-event))