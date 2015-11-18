(ns com.pav.profile.timeline.worker.component.switchboard
  (:require [com.stuartsierra.component :as comp]
            [clojure.core.async :as c]
            [clojure.tools.logging :as log]))


(defrecord Switchboard [processed-evt-chan redis-chan dynamo-chan]
  comp/Lifecycle
  (start [component]
    (log/info "Starting Switchboard")
    (let [multi-chan (c/mult processed-evt-chan)]
      (c/tap multi-chan redis-chan)
      (c/tap multi-chan dynamo-chan))
    (log/info "Started Switchboard")
    component)
  (stop [component]
    (log/info "Stopping Switchboard")
    component))

(defn new-switchboard []
  (map->Switchboard {}))