(ns com.pav.profile.timeline.worker.component.event-handler
  (:require [com.stuartsierra.component :as comp]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.faraday :as far]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [clojure.tools.logging :as log]
            [com.pav.profile.timeline.worker.functions.functions :refer [unpack-event
                                                                         parse-vote
                                                                         parse-comment
                                                                         parse-followinguser
                                                                         parse-followedbyuser
                                                                         parse-like-comment
                                                                         parse-dislike-comment]]))

(defn publish-to-redis-timeline [redis-conn evt]
  (let [timeline-key (str "timeline:" (:user_id evt))]
    (try
      (wcar redis-conn (car/zadd timeline-key (:timestamp evt) (-> (ch/generate-string evt)
                                                                   msg/pack)))
      {:status :success}
    (catch Exception e (log/error (str "Error publishing message to timeline: " timeline-key ", " e))))))

(defn publish-to-dynamo-timeline [client-opts table-name evt]
  (try
    (far/put-item client-opts table-name evt)
    {:status :success}
    (catch Exception e (log/error (str "Error writing to table " table-name ", with " evt ", " e)))))

(defn parse-event [event]
  (case (:type event)
    "vote" (parse-vote event)
    "comment" (parse-comment event)
    "followinguser" (parse-followinguser event)
    "followedbyuser" (parse-followedbyuser event)
    "likecomment" (parse-like-comment event)
    "dislikecomment" (parse-dislike-comment event)
    nil))

(defn process-events [redis-url dynamo-opts timeline-table input-queue number-of-consumers]
  (let [redis-conn {:spec {:uri redis-url}}]
    (car-mq/worker redis-conn input-queue
                   {:handler  (fn [{:keys [message]}]
                                (let [unpacked-evt (unpack-event message)
                                      event (parse-event unpacked-evt)]
                                  (if event
                                    (do (publish-to-redis-timeline redis-conn event)
                                        (publish-to-dynamo-timeline dynamo-opts timeline-table event))
                                    (do (log/error "Message is not a valid event type " unpacked-evt)
                                        {:status :error}))))
                    :monitor  (car-mq/monitor-fn input-queue 1000 5000)
                    :nthreads number-of-consumers})))

(defrecord RedisQueueConsumer [redis-url input-queue dynamo-opts timeline-table num-of-consumers]
  comp/Lifecycle
  (start [component]
    (log/info "Starting RedisQueueConsumer")
    (assoc component :worker (process-events redis-url dynamo-opts timeline-table input-queue num-of-consumers)))
  (stop [component]
    (log/info "Stopping RedisQueueConsumer")
    (update-in component [:worker] car-mq/stop)))

(defn new-redis-queue-consumer [redis-url input-queue dynamo-opts timeline-table num-of-consumers]
  (map->RedisQueueConsumer {:redis-url redis-url
                            :input-queue input-queue
                            :dynamo-opts dynamo-opts
                            :timeline-table timeline-table
                            :num-of-consumers num-of-consumers}))