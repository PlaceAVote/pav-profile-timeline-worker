(ns com.pav.profile.timeline.worker.component.event-handler-test
  (:use midje.sweet)
  (:require [com.pav.profile.timeline.worker.utils.util-test :refer :all]
            [user]))

(against-background [(before :facts (flush-redis))]
  (fact "Publish a user vote event, confirm it is in the users redis/dynamo timeline"
        (try
          (let [vote-evt {:bill_id "hr2-114" :created_at 14567 :timestamp 14567 :user_id "user101" :vote true :vote-id "vote1"
                          :type "vote"}]
            (user/go)
            (queue-event vote-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains vote-evt))
        (catch Exception e (println e))
        (finally
          (user/stop)))))
