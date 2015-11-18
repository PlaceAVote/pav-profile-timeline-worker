(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [taoensso.faraday :as far]))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn retrieve-users-first-last-names [conn table-name user_id]
  (println (far/get-item conn table-name {:user_id user_id} {:attrs [:first_name :last_name]}))
  (far/get-item conn table-name {:user_id user_id} {:attrs [:first_name :last_name]}))

(defn event-transducer [es-conn index dy-conn table-name]
  (map #(case (:type %)
         "vote"           (assoc % :new-msg (add-bill-title es-conn index (:decoded-msg %)))
         "comment"        (-> (assoc % :new-msg (add-bill-title es-conn index (:decoded-msg %)))
                              (update-in [:new-msg] (fn [v] (assoc v :score 0))))
         "followinguser"  (-> (retrieve-users-first-last-names dy-conn table-name (:following_id (:decoded-msg %)))
                              (merge (:decoded-msg %))
                              (assoc % :new-msg))
         "followeduser"  (-> (retrieve-users-first-last-names dy-conn table-name (:follower_id (:decoded-msg %)))
                              (merge (:decoded-msg %))
                              (assoc % :new-msg))
         nil)))