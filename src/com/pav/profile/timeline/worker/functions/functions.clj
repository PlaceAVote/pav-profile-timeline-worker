(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn event-transducer [es-conn index]
  (map #(case (:type %)
         "vote"    (assoc % :new-msg (add-bill-title es-conn index (:decoded-msg %)))
         "comment" (-> (assoc % :new-msg (add-bill-title es-conn index (:decoded-msg %)))
                       (update-in [:new-msg] (fn [v]
                                               (assoc v :score 0))))
         nil)))