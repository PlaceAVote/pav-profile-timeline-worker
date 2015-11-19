(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [com.pav.profile.timeline.worker.component.event-handler :refer [new-redis-queue-consumer]]))

(def dynamo-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(defn new-system []
  (component/system-map
   :timeline-event-consumer (new-redis-queue-consumer (:redis-url env) (:input-queue env)
                                                      dynamo-opts (:dynamo-usertimeline-table-name env) 3)))
