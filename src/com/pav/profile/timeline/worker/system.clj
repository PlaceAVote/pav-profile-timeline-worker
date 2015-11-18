(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan]]
            [environ.core :refer [env]]
            [clojurewerkz.elastisch.rest :as esr]
            [com.pav.profile.timeline.worker.component.redis :refer [new-redis-queue-consumer
                                                                     new-redis-timeline-publisher]]
            [com.pav.profile.timeline.worker.handler.component :refer [new-event-handler]]))

(defn new-system []
  (component/system-map 
   :publish-evt-chan (chan 100)
   :processed-evt-chan (chan 100)
   :es-conn (esr/connect (:es-url env))
   :timeline-event-listener (component/using (new-redis-queue-consumer (:redis-url env) (:input-queue env) (:processing-queue env) 3)
                                             {:event-channel :publish-evt-chan})
   :vote-event-handler (component/using (new-event-handler "vote")
                                        {:publisher :publish-evt-chan
                                         :processed-evt-chan :processed-evt-chan
                                         :es-conn :es-conn})
   :redis-timeline-publisher (component/using (new-redis-timeline-publisher (:redis-url env) (:processing-queue env) 3)
                                              {:processed-evt-channel :processed-evt-chan})))
