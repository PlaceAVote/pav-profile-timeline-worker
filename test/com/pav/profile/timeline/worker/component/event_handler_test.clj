(ns com.pav.profile.timeline.worker.component.event-handler-test
  (:use midje.sweet)
  (:require [com.pav.profile.timeline.worker.utils.util-test :refer :all]
            [user])
  (:import (java.util Date)))

(against-background [(before :facts (do (flush-redis)
                                        (clean-dynamo-tables)))]
  (fact "Publish a user vote event, confirm it is in the users dynamo timeline"
        (try
          (let [vote-evt {:bill_id "hr2-114" :created_at 14567 :timestamp 14567 :user_id "user101" :vote true :vote-id "vote1"
                          :type "vote"}]
            (user/go)
            (queue-event vote-evt)
            (Thread/sleep 4000)
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
            (first (retrieve-dynamo-timeline "user101")) => nil
            (first (retrieve-dynamo-timeline "user102")) => (contains followinguser-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a like comment event, confirm it is in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "likecomment" :user_id "user102"}]
            (user/go)
            (queue-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user102")) => (contains likecomment-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish a dislike comment event, confirm it is in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "dislikecomment" :user_id "user102"}]
            (user/go)
            (queue-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user102")) => (contains likecomment-evt))
          (catch Exception e (println e))
          (finally
            (user/stop))))

  (fact "Publish an event with an invalid event type, confirm it is NOT in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "invalidtype"}]
            (user/go)
            (queue-event likecomment-evt)
            (Thread/sleep 4000)
            (retrieve-dynamo-timeline "user101") => [])
          (catch Exception e (println e))
          (finally
            (user/stop))))

	(fact "Publish a user comment event, when user comment has a parent_id, then publish a comment reply notification
				to the user associated with the parent comment."
		(try
			(let [parent-comment {:comment_id "comment:1" :bill_id "hr2-114" :author "user102"
														:author_first_name "John" :author_last_name "Rambo"
														:timestamp (.getTime (Date.)) :has_children true :parent_id  nil
														:body "I'm the parent" :score 1 }
						expected-reply-notification {:user_id "user102" :author "user101" :author_first_name "Peter" :bill_id "hr2-114"
																				 :author_last_name "Pan" :type "commentreply" :read false :comment_id "comment:2"
																				 :timestamp 14567}
						_ (create-comment parent-comment)
						comment-evt {:bill_id "hr2-114" :timestamp 14567 :author_img_url "http://img.url"
												 :author "user101" :author_first_name "Peter" :author_last_name "Pan"
												 :body "I'm the reply" :parent_id "comment:1" :has_children false :comment_id "comment:2"
												 :score 0 :type "comment"}]
				(user/go)
				(queue-event comment-evt)
				(Thread/sleep 4000)
				(first (retrieve-dynamo-timeline "user101")) => (contains comment-evt)
				(first (retrieve-dynamo-notifications "user102")) => (contains expected-reply-notification))
			(catch Exception e (println e))
			(finally
				(user/stop)))))
