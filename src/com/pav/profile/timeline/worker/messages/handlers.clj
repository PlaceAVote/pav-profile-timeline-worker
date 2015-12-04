(ns com.pav.profile.timeline.worker.messages.handlers
	(:require [com.pav.profile.timeline.worker.messages.timeline :as te]
						[com.pav.profile.timeline.worker.functions.functions :as f]
						[msgpack.clojure-extensions]
						[environ.core :refer [env]]))

(defn timeline-builder [{:keys [type] :as event}]
	(let [event (case type
								"vote" 					 (te/new-vote-event (f/parse-vote event))
								"comment" 			 (te/new-comment-event (f/parse-comment event))
								"followinguser"  (te/new-following-event (f/parse-followinguser event))
								"followedbyuser" (te/new-followedby-event (f/parse-followedbyuser event))
								"likecomment" 	 (te/new-comment-event (f/parse-like-comment event))
								"dislikecomment" (te/new-comment-event (f/parse-dislike-comment event))
								nil)]
		(if event
			(do (f/publish-to-dynamo-timeline event)
					{:status :success})
			{:status :error})))