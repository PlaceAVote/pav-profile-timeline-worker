(ns com.pav.profile.timeline.worker.component.redis
  (:require [com.stuartsierra.component :as comp]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.core.async :as c]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [clojure.tools.logging :as log]))

(defn unpack-event [evt]
  (let [decoded-msg (-> (msg/unpack evt)
                        (ch/parse-string true))]
    {:encoded-msg evt
     :decoded-msg decoded-msg
     :type (:type decoded-msg)}))

(defn poll-queue [redis-conn input-queue processing-queue timeout]
  (try
    (wcar redis-conn (car/brpoplpush input-queue processing-queue timeout))
  (catch Exception e
    (log/info (str "Connection with queue has dropped.  Attempting reconnect " e)))))

(defn start-processing-events [redis-url input-queue processing-queue event-channel num-of-consumers]
  (let [redis-conn {:spec {:uri redis-url}}]
    (dotimes [_ num-of-consumers]
      (c/thread
       (loop []
         (let [evt (poll-queue redis-conn input-queue processing-queue 1000)]
           (when evt
             (try
               (c/>!! event-channel (unpack-event evt))
             (catch Exception e (log/error e)))))
         (recur))))))

(defrecord RedisQueueConsumer [redis-url input-queue processing-queue publish-evt-chan num-of-consumers]
  comp/Lifecycle
  (start [component]
    (log/info "Starting RedisQueueConsumer")
    (start-processing-events redis-url input-queue processing-queue publish-evt-chan num-of-consumers)
    (log/info "Started RedisQueueConsumer")
    component)
  (stop [component]
    (log/info "Stopping RedisQueueConsumer")
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
               new-msg (:new-msg evt)
               timeline-key (str "timeline:" (:user_id new-msg))]
           (wcar redis-conn (car/zadd timeline-key (:timestamp new-msg) (-> (ch/generate-string new-msg)
                                                                            msg/pack)))
           (wcar redis-conn (car/lrem processing-queue 1 (:encoded-msg evt))))
         (recur))))))

(defrecord RedisTimelinePublisher [redis-url processing-queue publish-evt-chan num-of-consumers]
  comp/Lifecycle
  (start [component]
    (log/info "Starting RedisTimelinePublisher")
    (start-publishing-timeline-events redis-url processing-queue publish-evt-chan num-of-consumers)
    (log/info "Started RedisTimelinePublisher")
    component)
  (stop [component]
    (log/info "Stopping RedisTimelinePublisher")
    component))

(defn new-redis-timeline-publisher [redis-url processing-queue num-of-consumers]
  (map->RedisTimelinePublisher {:redis-url redis-url
                                :processing-queue processing-queue
                                :num-of-consumers num-of-consumers}))