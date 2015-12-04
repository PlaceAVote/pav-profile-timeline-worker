(ns com.pav.profile.timeline.worker.component.event-handler-test
  (:use midje.sweet)
  (:require [com.pav.profile.timeline.worker.utils.util-test :refer :all]
            [user])
  (:import (java.util Date)))

(def hr2-114-title "To amend title XVIII of the Social Security Act to repeal the Medicare sustainable growth rate and strengthen Medicare access by improving physician payments and making other improvements, to reauthorize the Children's Health Insurance Program, and for other purposes.")


(against-background [(before :facts (do (flush-redis)
                                        (clean-dynamo-tables)
																				(clean-congress-index)
																				(bootstrap-bills)))]
  (fact "Publish a user vote event, confirm it is in the users dynamo timeline"
        (try
          (let [vote-evt {:bill_id "hr2-114" :timestamp 14567 :user_id "user101" :vote true :vote-id "vote1"
                          :type "vote"}]
            (user/go)
            (queue-timeline-event vote-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user101")) => (contains (assoc vote-evt
																																				:bill_title hr2-114-title)))
        (catch Exception e (println e))
        (finally
          (user/stop))))

	(fact "Publish a user comment event, confirm it is in the users timeline"
        (try
          (let [comment-evt {:bill_id "hr2-114" :timestamp 14567 :author_img_url "http://img.url"
                             :author "user101" :body "Comment Body" :parent_id nil :has_children false
                             :score 0 :type "comment"}]
            (user/go)
            (queue-timeline-event comment-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user101")) => (contains (assoc comment-evt
																																				:bill_title hr2-114-title)))
          (catch Exception e (println e))
          (finally
            (user/stop))))

	(fact "Publish a user following event, confirm it is in the users timeline"
        (try
          (let [followinguser-evt {:type      "followinguser" :user_id "user101" :following_id "user102"
                                   :timestamp (.getTime (Date.))}]
            (user/go)
            (queue-timeline-event followinguser-evt)
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
            (queue-timeline-event followinguser-evt)
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
            (queue-timeline-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user102")) => (contains (assoc likecomment-evt
																																				:bill_title hr2-114-title)))
          (catch Exception e (println e))
          (finally
            (user/stop))))

	(fact "Publish a dislike comment event, confirm it is in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "dislikecomment" :user_id "user102"}]
            (user/go)
            (queue-timeline-event likecomment-evt)
            (Thread/sleep 4000)
            (first (retrieve-dynamo-timeline "user102")) => (contains (assoc likecomment-evt
																																				:bill_title hr2-114-title)))
          (catch Exception e (println e))
          (finally
            (user/stop))))

	(fact "Publish an event with an invalid event type, confirm it is NOT in the users timeline"
        (try
          (let [likecomment-evt {:bill_id "hr2-114" :author_img_url "http://img.url"
                                 :author "user101" :body "Comment Body" :parent_id nil :has_children false
                                 :score 0 :type "invalidtype"}]
            (user/go)
            (queue-timeline-event likecomment-evt)
            (Thread/sleep 4000)
            (retrieve-dynamo-timeline "user101") => [])
          (catch Exception e (println e))
          (finally
            (user/stop))))

	(fact "Publish comment reply event, verify comment reply notification is sent to author of parent comment"
		(try
			(let [parent-comment {:comment_id "comment:1" :bill_id "hr2-114" :author "user102"
														:author_first_name "John" :author_last_name "Rambo"
														:timestamp (.getTime (Date.)) :has_children true :parent_id  nil
														:body "I'm the parent" :score 1 }
						comment-evt {:bill_id "hr2-114" :timestamp 14567 :author_img_url "http://img.url"
												 :author "user101" :author_first_name "Peter" :author_last_name "Pan"
												 :body "I'm the reply" :parent_id "comment:1" :has_children true :comment_id "comment:2"
												 :score 0 :type "commentreply"}
						expected-reply-notification {:user_id "user102" :author "user101" :author_first_name "Peter" :bill_id "hr2-114"
																				 :author_last_name "Pan" :type "commentreply" :read false :comment_id "comment:2"
																				 :timestamp 14567 :body "I'm the reply" :author_img_url "http://img.url"
																				 :bill_title hr2-114-title}
						_ (create-comment parent-comment)]
				(user/go)
				(queue-notification comment-evt)
				(Thread/sleep 4000)
				(println "Actual" (first (retrieve-dynamo-notifications "user102")))
				(println "Desired" expected-reply-notification)
				(first (retrieve-dynamo-notifications "user102")) => (contains expected-reply-notification))
			(catch Exception e (println e))
			(finally
				(user/stop)))))
