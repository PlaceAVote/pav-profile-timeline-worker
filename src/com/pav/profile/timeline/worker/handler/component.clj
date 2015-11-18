(ns com.pav.profile.timeline.worker.handler.component
  (:require [com.stuartsierra.component :as comp]
            [clojure.core.async :as c]
            [com.pav.profile.timeline.worker.functions.functions :refer [add-bill-title]]))


(defn start-processing-evt-type [es-conn publisher processed-evt-chan evt-type]
  (let [sub-chan (c/chan 100)
        publication (c/pub publisher #(:type %))]
    (c/sub publication evt-type sub-chan)
    (c/thread
      (loop []
        (let [val (c/<!! sub-chan)]
          (if-not (nil? val)
            (c/put! processed-evt-chan (add-bill-title es-conn "congress" val))))
        (recur)))))

(defrecord EventHandler [es-conn publisher processed-evt-chan evt-type]
  comp/Lifecycle
  (start [component]
    (println "Starting EventHandler for " evt-type)
    (start-processing-evt-type es-conn publisher processed-evt-chan evt-type)
    component)
  (stop [component]
    (println "Stopping EventHandler for " evt-type)
    component))

(defn new-event-handler [evt-type]
  (map->EventHandler {:evt-type evt-type}))