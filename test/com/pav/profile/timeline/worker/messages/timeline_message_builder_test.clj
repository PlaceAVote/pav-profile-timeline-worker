(ns com.pav.profile.timeline.worker.messages.timeline-message-builder-test
	(:use midje.sweet)
	(:import (java.util Date))
	(:require [com.pav.profile.timeline.worker.messages.timeline :as te]
						[schema.core :as s]))

(fact "Construct Timeline Event Message, with valid vote event payload"
	(let [vote-event {:event_id   "asdas"
									 	:type       "vote"
									 	:bill_id    "hr2-114"
									 	:bill_title "bill title"
									 	:timestamp  (.getTime (Date.))
									 	:user_id "user101"
									 	:vote true
									 	:vote-id "sadasdsa"}]
		(s/with-fn-validation (te/new-vote-event vote-event)) => vote-event
		(s/with-fn-validation (te/new-vote-event (dissoc vote-event :user_id))) => (throws RuntimeException)))

(fact "Construct Timeline Event Message, with valid comment event payload"
	(let [comment-event {:event_id   "asdas"
											 :type       "comment"
											 :author "user101"
											 :author_first_name "john"
											 :author_last_name "rambo"
											 :author_img_url "img.com"
											 :bill_id    "hr2-114"
											 :bill_title "bill title"
											 :body "my body"
											 :comment_id "id"
											 :has_children false
											 :parent_id nil
											 :score 0
											 :timestamp  (.getTime (Date.))
											 :user_id "user101"}
				comment-event-with-parent_id (assoc comment-event :parent_id "2312321")]
		(s/with-fn-validation (te/new-comment-event comment-event)) => comment-event
		(s/with-fn-validation (te/new-comment-event comment-event-with-parent_id)) => comment-event-with-parent_id
		(s/with-fn-validation (te/new-comment-event (dissoc comment-event :user_id))) => (throws RuntimeException)))

(fact "Construct Timeline Event Message, with valid following event payload"
	(let [following-event {:event_id "213213" :type "followinguser" :user_id "user101" :following_id "user102"
												 :timestamp (.getTime (Date.)) :first_name "John" :last_name "Rambo"}]
		(s/with-fn-validation (te/new-following-event following-event)) => following-event
		(s/with-fn-validation (te/new-following-event (dissoc following-event :user_id))) => (throws RuntimeException)))

(fact "Construct Timeline Event Message, with valid followedby event payload"
	(let [followedby-event {:event_id "213213" :type "followedbyuser" :user_id "user102" :follower_id "user101"
												 :timestamp (.getTime (Date.)) :first_name "John" :last_name "Rambo"}]
		(s/with-fn-validation (te/new-followedby-event followedby-event)) => followedby-event
		(s/with-fn-validation (te/new-followedby-event (dissoc followedby-event :user_id))) => (throws RuntimeException)))

(fact "Construct Timeline Event Message, with valid liked comment event payload"
	(let [likedcomment {:event_id   "asdas"
											:type       "likecomment"
											:author "user101"
											:author_first_name "john"
											:author_last_name "rambo"
											:author_img_url "img.com"
											:bill_id    "hr2-114"
											:bill_title "bill title"
											:body "my body"
											:comment_id "id"
											:has_children false
											:parent_id nil
											:score 0
											:timestamp  (.getTime (Date.))
											:user_id "user101"}]
		(s/with-fn-validation (te/new-comment-event likedcomment)) => likedcomment
		(s/with-fn-validation (te/new-comment-event (dissoc likedcomment :user_id))) => (throws RuntimeException)))

(fact "Construct Timeline Event Message, with valid disliked comment event payload"
	(let [likedcomment {:event_id   "asdas"
											:type       "dislikecomment"
											:author "user101"
											:author_first_name "john"
											:author_last_name "rambo"
											:author_img_url "img.com"
											:bill_id    "hr2-114"
											:bill_title "bill title"
											:body "my body"
											:comment_id "id"
											:has_children false
											:parent_id nil
											:score 0
											:timestamp  (.getTime (Date.))
											:user_id "user101"}]
		(s/with-fn-validation (te/new-comment-event likedcomment)) => likedcomment
		(s/with-fn-validation (te/new-comment-event (dissoc likedcomment :user_id))) => (throws RuntimeException)))