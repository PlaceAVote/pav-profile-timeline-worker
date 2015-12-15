(ns com.pav.profile.timeline.worker.component.timeline-event-handler
	(:require [com.stuartsierra.component :as comp]
						[clojure.tools.logging :as log]
						[com.pav.profile.timeline.worker.messages.timeline :as te]
						[com.pav.profile.timeline.worker.component.common :refer [EventHandler]]
						[com.pav.profile.timeline.worker.functions.functions :as f]))

(defn parse-and-persist [{:keys [dynamo-opts es-conn]}
												 {:keys [user-table timeline-table]}
												 {:keys [type] :as event}]
	(let [event (case type
								"vote" 					 (te/new-vote-event (f/parse-vote es-conn event))
								"comment" 			 (te/new-comment-event (f/parse-comment es-conn event))
								"followinguser"  (te/new-following-event (f/parse-followinguser dynamo-opts user-table event))
								"followedbyuser" (te/new-followedby-event (f/parse-followedbyuser dynamo-opts user-table event))
								"likecomment" 	 (te/new-comment-event (f/parse-like-comment es-conn dynamo-opts user-table event))
								"dislikecomment" (te/new-comment-event (f/parse-dislike-comment es-conn dynamo-opts user-table event))
								nil)]
		(if event
			(do (f/publish-to-dynamo-timeline dynamo-opts timeline-table (f/add-event-id event))
					{:status :success})
			{:status :error})))

(defrecord TimelineEventHandler [connection-opts dynamo-tables]
	comp/Lifecycle EventHandler
	(start [component]
		(log/info "Starting TimelineEventHandler")
		component)
	(stop [component]
		(log/info "Stopping TimelineEventHandler")
		component)
	(handle-event [_ evt]
		(parse-and-persist connection-opts dynamo-tables evt)))

(defn new-timeline-event-handler [connection-opts dynamo-tables]
	(map->TimelineEventHandler {:connection-opts connection-opts
															:dynamo-tables dynamo-tables}))
