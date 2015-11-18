(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan]]
            [environ.core :refer [env]]
            [clojurewerkz.elastisch.rest :as esr]
            [com.pav.profile.timeline.worker.component.redis :refer [new-redis-queue-consumer
                                                                     new-redis-timeline-publisher]]
            [com.pav.profile.timeline.worker.functions.functions :refer [event-transducer]]))

(defn new-system []
  (component/system-map 
   :publish-evt-chan (chan 100 (event-transducer (esr/connect (:es-url env)) "congress"))
   :es-conn (esr/connect (:es-url env))
   :timeline-event-listener (component/using (new-redis-queue-consumer (:redis-url env) (:input-queue env) (:processing-queue env) 3)
                                             {:publish-evt-chan :publish-evt-chan})
   :redis-timeline-publisher (component/using (new-redis-timeline-publisher (:redis-url env) (:processing-queue env) 3)
                                              {:publish-evt-chan :publish-evt-chan})))
