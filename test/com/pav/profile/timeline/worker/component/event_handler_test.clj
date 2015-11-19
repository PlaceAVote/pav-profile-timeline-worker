(ns com.pav.profile.timeline.worker.component.event-handler-test
  (:use midje.sweet)
  (:require [com.pav.profile.timeline.worker.utils.util-test :refer :all]
            [user])
  (:import (java.util Date)))

(against-background [(before :facts (do (flush-redis)
                                        (clean-dynamo-tables)))]
  (fact "Publish a user vote event, confirm it is in the users redis/dynamo timeline"
        (try
          (let [vote-evt {:bill_id "hr2-114" :created_at 14567 :timestamp 14567 :user_id "user101" :vote true :vote-id "vote1"
                          :type "vote"}]
            (user/go)
            (queue-event vote-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains vote-evt)
            (first (retrieve-dynamo-timeline "user101")) => (contains vote-evt))
        (catch Exception e (println e))
        (finally
          (user/stop))))

  (fact "Publish a user comment event, confirm it is in the users timeline"
        (try
          (let [comment-evt {:bill_id "hr2-114" :timestamp 14567 :author_img_url "http://img.url"
                             :author "user101" :body "Comment Body" :parent_id nil :has_children false
                             :score 0 :type "comment"}]
            (user/go)
            (queue-event comment-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains comment-evt)
            (first (retrieve-dynamo-timeline "user101")) => (contains comment-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a user following event, confirm it is in the users timeline"
        (try
          (let [followinguser-evt {:type      "followinguser" :user_id "user101" :following_id "user102"
                                   :timestamp (.getTime (Date.))}]
            (user/go)
            (queue-event followinguser-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains followinguser-evt)
            (first (retrieve-dynamo-timeline "user101")) => (contains followinguser-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a user follower event, confirm it is in the users timeline"
        (try
          (let [followinguser-evt {:type      "followedbyuser" :user_id "user102" :follower_id "user101"
                                   :timestamp (.getTime (Date.))}]
            (user/go)
            (queue-event followinguser-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => nil
            (first (retrieve-redis-timeline "user102")) => (contains followinguser-evt)
            (first (retrieve-dynamo-timeline "user102")) => (contains followinguser-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a like comment event, confirm it is in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "likecomment"}]
            (user/go)
            (queue-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains likecomment-evt)
            (first (retrieve-dynamo-timeline "user101")) => (contains likecomment-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a dislike comment event, confirm it is in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "dislikecomment"}]
            (user/go)
            (queue-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-redis-timeline "user101")) => (contains likecomment-evt)
            (first (retrieve-dynamo-timeline "user101")) => (contains likecomment-evt))
          (catch Exception e (println e))
          (finally
            (user/stop)))))
