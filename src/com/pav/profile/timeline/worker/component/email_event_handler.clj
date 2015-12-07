(ns com.pav.profile.timeline.worker.component.email-event-handler
	(:require [com.stuartsierra.component :as comp]
						[com.pav.profile.timeline.worker.component.common :refer [EventHandler]]
						[com.pav.profile.timeline.worker.functions.functions :as f]
						[com.pav.profile.timeline.worker.messages.notification :as ne]
						[clojure.tools.logging :as log]))

(defn parse-and-persist [{:keys [dynamo-opts es-conn]}
												 {:keys [comment-details-table]}
												 {:keys [api-key comment-template]}
												 {:keys [type] :as event}]
	(let [event (case type
								"commentreply" (ne/new-comment-reply-email-notification
																 (f/parse-comment-reply-notification
																	 es-conn dynamo-opts
																	 comment-details-table event))
								nil)]
		(if event
			(do (f/publish-comment-reply-email api-key comment-template event)
					{:status :success})
			{:status :error})))


(defrecord EmailEventHandler [connection-opts dynamo-tables mandril-opts]
	comp/Lifecycle EventHandler
	(start [component]
		(log/info "Starting Event Handler")
		component)
	(stop [component]
		(log/info "Stopping Event Handler")
		component)
	(handle-event [_ evt]
		(parse-and-persist connection-opts dynamo-tables mandril-opts evt)))

(defn new-email-event-handler [connection-opts dynamo-tables mandril-opts]
	(map->EmailEventHandler {:connection-opts connection-opts
													 :dynamo-tables dynamo-tables
													 :mandril-opts mandril-opts}))
