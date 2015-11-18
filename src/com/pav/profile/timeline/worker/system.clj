(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan]]
            [environ.core :refer [env]]
            [clojurewerkz.elastisch.rest :as esr]
            [com.pav.profile.timeline.worker.component.redis :refer [new-redis-queue-consumer
                                                                     new-redis-timeline-publisher]]
            [com.pav.profile.timeline.worker.component.dynamo :refer [new-dynamodb-timeline-publisher]]
            [com.pav.profile.timeline.worker.component.switchboard :refer [new-switchboard]]
            [com.pav.profile.timeline.worker.functions.functions :refer [event-transducer]]))

(def client-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(defn new-system []
  (component/system-map
   :es-conn (esr/connect (:es-url env))
   :redis-chan (chan 100)
   :dynamo-chan (chan 100)
   :publish-evt-chan (chan 100 (event-transducer (esr/connect (:es-url env)) "congress" client-opts (:dynamo-user-table-name env)))
   :switchboard (component/using (new-switchboard) {:processed-evt-chan :publish-evt-chan
                                                    :redis-chan :redis-chan :dynamo-chan :dynamo-chan})
   :timeline-event-listener (component/using (new-redis-queue-consumer (:redis-url env) (:input-queue env) (:processing-queue env) 3)
                                             {:publish-evt-chan :publish-evt-chan})
   :redis-timeline-publisher (component/using (new-redis-timeline-publisher (:redis-url env) (:processing-queue env) 3)
                                              {:processed-evt-chan :redis-chan})
   :dynamodb-timeline-publisher (component/using (new-dynamodb-timeline-publisher client-opts (:dynamo-user-table-name env))
                                                 {:processed-evt-chan :dynamo-chan})))
