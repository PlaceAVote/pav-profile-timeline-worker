(ns com.pav.profile.timeline.worker.component.event-handler
	(:require [com.stuartsierra.component :as comp]
						[taoensso.carmine.message-queue :as car-mq]
						[msgpack.clojure-extensions]
						[clojure.tools.logging :as log]
						[com.pav.profile.timeline.worker.functions.functions :refer [unpack-event]]))

(defn process-events [redis-url input-queue message-handler number-of-consumers]
  (let [redis-conn {:spec {:uri redis-url}}]
    (car-mq/worker redis-conn input-queue
                   {:handler  (fn [{:keys [message]}]
                                (let [evt (unpack-event message)]
																	(message-handler evt)))
                    :monitor  (car-mq/monitor-fn input-queue 1000 5000)
                    :nthreads number-of-consumers})))

(defrecord RedisQueueConsumer [redis-url input-queue message-handler num-of-consumers]
  comp/Lifecycle
  (start [component]
    (log/info "Starting RedisQueueConsumer")
    (assoc component :worker (process-events redis-url input-queue message-handler num-of-consumers)))
  (stop [component]
    (log/info "Stopping RedisQueueConsumer")
    (update-in component [:worker] car-mq/stop)))

(defn new-redis-queue-consumer [redis-url input-queue message-handler num-of-consumers]
  (map->RedisQueueConsumer {:redis-url        redis-url
														:input-queue   		input-queue
														:message-handler  message-handler
														:num-of-consumers num-of-consumers}))