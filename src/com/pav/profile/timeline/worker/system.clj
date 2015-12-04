(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [com.pav.profile.timeline.worker.component.event-handler :refer [new-redis-queue-consumer]]
            [com.pav.profile.timeline.worker.messages.handlers :refer [timeline-builder]]))

(defn new-system []
  (component/system-map
   :timeline-event-consumer (new-redis-queue-consumer (:redis-url env) (:input-queue env) timeline-builder 3)))
