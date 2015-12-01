(ns com.pav.profile.timeline.worker.utils.util-test
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.faraday :as far]
            [environ.core :refer [env]]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]))

(def redis-conn {:spec {:host "127.0.0.1" :port 6379}})

(def dynamo-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))
(def timeline-table (:dynamo-usertimeline-table-name env))
(def comment-details-table-name (:dynamo-comment-details-table-name env))
(def notification-table (:dynamo-usernotification-table-name env))

(def queue (:input-queue env))

(defn queue-event [evt]
  (let [v (-> (ch/generate-string evt)
              msg/pack)]
    (wcar redis-conn (car-mq/enqueue queue v))))

(defn retrieve-dynamo-notifications [user_id]
	(far/query dynamo-opts notification-table {:user_id [:eq user_id]}))

(defn retrieve-dynamo-timeline [user_id]
  (far/query dynamo-opts timeline-table {:user_id [:eq user_id]}))

(defn flush-redis []
  (wcar redis-conn
        (car/flushall)
        (car/flushdb)))

(defn delete-tables []
  (println "deleting table")
  (try
    (far/delete-table dynamo-opts user-table)
    (far/delete-table dynamo-opts timeline-table)
    (far/delete-table dynamo-opts notification-table)
    (far/delete-table dynamo-opts comment-details-table-name)
  (catch Exception e (println e))))

(defn create-tables []
  (println "creating table")
  (try
    (far/create-table dynamo-opts user-table [:user_id :s]
			{:gsindexes [{:name "user-email-idx"
										:hash-keydef [:email :s]
										:throughput {:read 5 :write 10}}]
			 :throughput {:read 5 :write 10}
			 :block? true})
    (far/create-table dynamo-opts timeline-table [:user_id :s]
			{:range-keydef [:timestamp :n]
			 :throughput {:read 5 :write 10}
			 :block? true})
		(far/create-table dynamo-opts notification-table [:user_id :s]
			{:range-keydef [:timestamp :n]
			 :throughput {:read 5 :write 10}
			 :block? true})
    (far/create-table dynamo-opts comment-details-table-name [:comment_id :s]
      {:gsindexes [{:name "bill-comment-idx"
                    :hash-keydef [:bill_id :s]
                    :range-keydef [:comment_id :s]
                    :throughput {:read 5 :write 10}}]
       :throughput {:read 5 :write 10}
       :block? true})
    (catch Exception e (println e))))

(defn clean-dynamo-tables []
  (delete-tables)
  (create-tables))

(defn create-comment [comment]
  (far/put-item dynamo-opts comment-details-table-name comment))