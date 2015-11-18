(ns com.pav.profile.timeline.worker.main
  (:require [com.pav.profile.timeline.worker.system :refer [new-system]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(def system nil)

(defn -main [& args]
  (alter-var-root #'system
                  (constantly (new-system)))
  (alter-var-root #'system component/start)
  (while true (println "I'm alive")))
