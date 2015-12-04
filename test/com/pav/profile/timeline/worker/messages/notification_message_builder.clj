(ns com.pav.profile.timeline.worker.messages.notification-message-builder
	(:use midje.sweet)
	(:import (java.util Date))
	(:require [schema.core :as s]
						[com.pav.profile.timeline.worker.messages.notification :as ne]))

(fact "Construct Notification Event, when given a comment reply event payload"
	(let [commentreply-notification {:notification_id   "1234" :type "commentreply" :author "user101"
																	 :author_first_name "John" :author_last_name "Rambo"
																	 :author_img_url "http://img.com" :bill_id "hr2-114" :bill_title "bill title"
																	 :read false :timestamp (.toString (Date.)) :user_id "user102"
																	 :body "body" :comment_id "commentid"}]
		(s/with-fn-validation (ne/new-comment-reply-notification commentreply-notification)) => commentreply-notification))

