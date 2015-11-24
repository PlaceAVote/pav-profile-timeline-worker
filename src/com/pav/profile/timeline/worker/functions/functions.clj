(ns com.pav.profile.timeline.worker.functions.functions
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest :as esr]
            [taoensso.faraday :as far]
            [cheshire.core :as ch]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]
            [environ.core :refer [env]])
  (:import (java.util Date)))

(def es-conn (esr/connect (:es-url env)))

(def client-opts {:access-key (:access-key env)
                  :secret-key (:secret-key env)
                  :endpoint (:dynamo-endpoint env)})

(def user-table (:dynamo-user-table-name env))

(defn retrieve-bill-title [conn index bill_id]
  (-> (esd/get conn index "bill" bill_id)
      :_source :official_title))

(defn add-bill-title [es-conn index event]
  (assoc event :bill_title (retrieve-bill-title es-conn index (:bill_id event))))

(defn add-users-first-last-names [evt user_id]
  (-> (far/get-item client-opts user-table {:user_id user_id} {:attrs [:first_name :last_name]})
      (merge evt)))

(defn parse-vote [evt]
  (add-bill-title es-conn "congress" evt))

(defn parse-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (assoc :score 0 :user_id (:author evt))))

(defn parse-followinguser [evt]
  (add-users-first-last-names evt (:following_id evt)))

(defn parse-followedbyuser [evt]
  (add-users-first-last-names evt (:follower_id evt)))

(defn parse-like-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))))

(defn parse-dislike-comment [evt]
  (-> (add-bill-title es-conn "congress" evt)
      (add-users-first-last-names (:author evt))
      (assoc :timestamp (.getTime (Date.)))))

(defn unpack-event [evt]
  (-> (msg/unpack evt)
      (ch/parse-string true)))