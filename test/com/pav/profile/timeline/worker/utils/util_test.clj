(ns com.pav.profile.timeline.worker.utils.util-test
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.faraday :as far]
            [environ.core :refer [env]]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
						[clojurewerkz.elastisch.rest :refer [connect]]
						[clojurewerkz.elastisch.rest.index :refer [create delete update-mapping]]
						[clojurewerkz.elastisch.rest.bulk :as ersb]))

(def connection (connect))
(def test-bills [(ch/parse-string (slurp "test-resources/bills/hr2/data.json") true)
								 (ch/parse-string (slurp "test-resources/bills/hr1104/data.json") true)
								 (ch/parse-string (slurp "test-resources/bills/hr620/data.json") true)])

(defn clean-congress-index []
	(delete connection "congress")
	(create connection "congress"))

(defn bootstrap-bills []
	(ersb/bulk-with-index-and-type connection "congress" "bill" (ersb/bulk-index test-bills)))

(def redis-conn {:spec {:host "127.0.0.1" :port 6379}})

(def dynamo-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))
(def timeline-table (:dynamo-usertimeline-table-name env))
(def comment-details-table-name (:dynamo-comment-details-table-name env))
(def notification-table (:dynamo-usernotification-table-name env))

(def timeline-queue (:timeline-queue env))
(def notification-queue (:notification-queue env))

(defn queue-event [queue evt]
  (let [v (-> (ch/generate-string evt)
              msg/pack)]
    (wcar redis-conn (car-mq/enqueue queue v))))

(defn queue-notification [evt]
  (queue-event notification-queue evt))

(defn queue-timeline-event [evt]
	(queue-event timeline-queue evt))

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
			{:gsindexes [{:name "notification_id-index"
										:hash-keydef [:notification_id :s]
										:throughput {:read 5 :write 10}}]
       :range-keydef [:timestamp :n]
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