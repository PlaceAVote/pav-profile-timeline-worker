(ns com.pav.profile.timeline.worker.events.notifications)

(defrecord CommentReplyNotification [user_id author author_first_name author_last_name
																		 bill_id type read comment_id timestamp])

(defn new-comment-reply-notification [{:keys [author]}
																		 {:keys [author_first_name author_last_name bill_id comment_id timestamp] :as comment-event}]
	(map->CommentReplyNotification {:user_id author
																	:author (:author comment-event)
																	:author_first_name author_first_name
																	:author_last_name author_last_name
																	:read false
																	:type "commentreply"
																	:bill_id bill_id
																	:comment_id comment_id
																	:timestamp (bigint timestamp)}))
