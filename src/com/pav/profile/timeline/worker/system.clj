(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [com.pav.profile.timeline.worker.component.event-handler :refer [new-redis-queue-consumer]]
            [com.pav.profile.timeline.worker.messages.handlers :refer [timeline-builder
																																			 notification-builder
																																			 email-notification-handler]]))

(defn new-system []
	(component/system-map
		:timeline-event-consumer (new-redis-queue-consumer (:redis-url env) (:timeline-queue env) timeline-builder 3)
		:notification-event-consumer (new-redis-queue-consumer (:redis-url env) (:notification-queue env) notification-builder 3)
		:email-notification-worker (new-redis-queue-consumer (:redis-url env) (:email-notification-queue env) email-notification-handler 3)))
