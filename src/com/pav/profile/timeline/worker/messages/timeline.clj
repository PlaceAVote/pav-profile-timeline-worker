(ns com.pav.profile.timeline.worker.messages.timeline
	(:require [schema.core :as s]))

(s/defrecord VoteEvent
	[event_id 	:- s/Str
	 type 			:- s/Str
	 bill_id		:- s/Str
	 bill_title :- s/Str
	 timestamp 	:- Long
	 user_id 	 	:- s/Str
	 vote 		 	:- s/Bool
	 vote-id 	 	:- s/Str])

(s/defn new-vote-event :- VoteEvent
	[{:keys [event_id type bill_id bill_title timestamp user_id vote vote-id] :as event}]
	(VoteEvent. event_id type bill_id bill_title timestamp user_id vote vote-id))

(s/defrecord CommentEvent
	[event_id 					:- s/Str
	 type 							:- s/Str
	 author							:- s/Str
	 author_first_name 	:- s/Str
	 author_last_name 	:- s/Str
	 author_img_url 		:- s/Str
	 bill_id						:- s/Str
	 bill_title 				:- s/Str
	 body 							:- s/Str
	 comment_id 				:- s/Str
	 has_children 			:- s/Bool
	 parent_id 					:- (s/maybe s/Str)
	 score							:- s/Int
	 timestamp 					:- Long
	 user_id 	 					:- s/Str])

(s/defn new-comment-event :- CommentEvent
	[{:keys [event_id type author author_first_name author_last_name author_img_url bill_id bill_title body comment_id
					 has_children parent_id score timestamp user_id] :as event}]
	(CommentEvent. event_id type author author_first_name author_last_name author_img_url bill_id bill_title body
		comment_id has_children parent_id score timestamp user_id))

(s/defrecord FollowingUserEvent
	[event_id 					:- s/Str
	 type 							:- s/Str
	 following_id				:- s/Str
	 timestamp 					:- Long
	 user_id 	 					:- s/Str])

(s/defn new-following-event :- FollowingUserEvent
	[{:keys [event_id type following_id timestamp user_id] :as event}]
	(FollowingUserEvent. event_id type following_id timestamp user_id))

(s/defrecord FollowedByUserEvent
	[event_id 					:- s/Str
	 type 							:- s/Str
	 follower_id				:- s/Str
	 timestamp 					:- Long
	 user_id 	 					:- s/Str])

(s/defn new-followedby-event :- FollowedByUserEvent
	[{:keys [event_id type follower_id timestamp user_id] :as event}]
	(FollowedByUserEvent. event_id type follower_id timestamp user_id))

{:event_id "321312" :bill_id "hr2-114" :author_img_url "http://img.url"
 :author "user101" :body "Comment Body" :parent_id nil :has_children false
 :score 0 :type "likecomment" :user_id "user102"}