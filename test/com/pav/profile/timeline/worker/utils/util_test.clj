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

(def timeline-table (:dynamo-usertimeline-table-name env))

(def queue (:input-queue env))

(defn queue-event [evt]
  (let [v (-> (ch/generate-string evt)
              msg/pack)]
    (wcar redis-conn (car-mq/enqueue queue v))))

(defn retrieve-redis-timeline [user_id]
  (->> (wcar redis-conn (car/zrevrange (str "timeline:" user_id) 0 -1))
       (mapv msg/unpack)
       (mapv #(ch/parse-string % true))))

(defn flush-redis []
  (wcar redis-conn
        (car/flushall)
        (car/flushdb)))