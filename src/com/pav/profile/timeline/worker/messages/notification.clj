(ns com.pav.profile.timeline.worker.messages.notification
	(:require [schema.core :as s]))

(s/defrecord CommentReplyNotification
	[notification_id   :- s/Str
	 type 						 :- s/Str
	 author 					 :- s/Str
	 author_first_name :- s/Str
	 author_last_name  :- s/Str
	 author_img_url 	 :- s/Str
	 bill_id					 :- s/Str
	 bill_title        :- s/Str
	 read 						 :- s/Bool
	 comment_id     	 :- s/Str
	 body 						 :- s/Str
	 timestamp 				 :- s/Str
	 user_id 				   :- s/Str])

(s/defn new-comment-reply-notification :- CommentReplyNotification
	[{:keys [notification_id type author author_first_name author_last_name author_img_url bill_id bill_title read
					 comment_id body timestamp user_id] :as event}]
	(CommentReplyNotification. notification_id type author author_first_name author_last_name author_img_url
		bill_id bill_title read comment_id body timestamp user_id))

(s/defrecord CommentReplyEmailNotification
	[notification_id   :- s/Str
	 type 						 :- s/Str
	 author 					 :- s/Str
	 author_first_name :- s/Str
	 author_last_name  :- s/Str
	 author_img_url 	 :- s/Str
	 bill_id					 :- s/Str
	 bill_title        :- s/Str
	 read 						 :- s/Bool
	 comment_id     	 :- s/Str
	 body 						 :- s/Str
	 timestamp 				 :- s/Str
	 user_id 				   :- s/Str
	 email             :- s/Str])

(s/defn new-comment-reply-email-notification :- CommentReplyNotification
	[{:keys [notification_id type author author_first_name author_last_name author_img_url bill_id bill_title read
					 comment_id body timestamp user_id email] :as event}]
	(CommentReplyEmailNotification. notification_id type author author_first_name author_last_name author_img_url
		bill_id bill_title read comment_id body timestamp user_id email))