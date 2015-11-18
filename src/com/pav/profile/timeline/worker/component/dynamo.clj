(ns com.pav.profile.timeline.worker.component.dynamo
  (:require [com.stuartsierra.component :as comp]
            [clojure.core.async :as c]
            [clojure.tools.logging :as log]
            [taoensso.faraday :as far]))

(defn start-publishing-events [client-opts table-name processed-evt-chan]
  (c/thread
    (loop []
      (let [evt (c/<!! processed-evt-chan)]
        (when evt
          (try
            (far/put-item client-opts table-name (:new-msg evt))
          (catch Exception e (log/info (str "Error writing to table " table-name ", with " (:new-msg evt) ", " e))))))
      (recur))))

(defrecord DynamoDBPublisher [client-opts table-name processed-evt-chan]
  comp/Lifecycle
  (start [component]
    (log/info "Starting DynamoDBPublisher")
    (start-publishing-events client-opts table-name processed-evt-chan)
    (log/info "Started DynamoDBPublisher")
    component)
  (stop [component]
    (log/info "Stopping DynamoDBPublisher")
    component))

(defn new-dynamodb-timeline-publisher [client-opts table-name]
  (map->DynamoDBPublisher {:client-opts client-opts
                           :table-name table-name}))