(ns com.pav.profile.timeline.worker.component.redis
  (:require [com.stuartsierra.component :as comp]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.core.async :as c]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]))


(defn start-processing-events [redis-url input-queue processing-queue event-channel num-of-consumers]
  (let [redis-conn {:spec {:uri redis-url}}]
    (wcar redis-conn (car/lpush input-queue (-> {:type "vote" :msg "hello there" :bill_id "hr2-114" :user_id "user101"}
                                                msg/pack)))
    (dotimes [_ num-of-consumers]
      (c/thread
       (loop []
         (let [evt (-> (wcar redis-conn (car/brpoplpush input-queue processing-queue 1000))
                       msg/unpack)]
           (if-not (nil? evt)
             (c/>!! event-channel evt)))
         (recur))))))

(defrecord RedisQueueConsumer [redis-url input-queue processing-queue event-channel num-of-consumers]
  comp/Lifecycle
  (start [component]
    (println "Starting RedisQueueConsumer")
    (start-processing-events redis-url input-queue processing-queue event-channel num-of-consumers)
    component)
  (stop [component]
    (println "Stopping RedisQueueConsumer")
    component))

(defn new-redis-queue-consumer [redis-url input-queue processing-queue num-of-consumers]
  (map->RedisQueueConsumer {:redis-url redis-url
                            :input-queue input-queue
                            :processing-queue processing-queue
                            :num-of-consumers num-of-consumers}))


(defn start-publishing-timeline-events [redis-url processing-queue event-channel num-of-consumers]
  (let [redis-conn {:spec {:uri redis-url}}]
    (dotimes [_ num-of-consumers]
      (c/thread
       (loop []
         (let [evt (c/<!! event-channel)
               timeline-key (str "timeline:" (:user_id evt))]
           (wcar redis-conn (car/zadd timeline-key (:timestamp evt) (msg/pack evt))))
         (recur))))))

(defrecord RedisTimelinePublisher [redis-url processing-queue processed-evt-channel num-of-consumers]
  comp/Lifecycle
  (start [component]
    (println "Starting RedisTimelinePublisher")
    (start-publishing-timeline-events redis-url processing-queue processed-evt-channel num-of-consumers)
    component)
  (stop [component]
    (println "Stopping RedisTimelinePublisher")
    component))

(defn new-redis-timeline-publisher [redis-url processing-queue num-of-consumers]
  (map->RedisTimelinePublisher {:redis-url redis-url
                                :processing-queue processing-queue
                                :num-of-consumers num-of-consumers}))