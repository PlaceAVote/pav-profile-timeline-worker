(ns com.pav.profile.timeline.worker.component.notification-event-handler
	(:require [com.stuartsierra.component :as comp]
						[com.pav.profile.timeline.worker.component.common :refer [EventHandler]]
						[com.pav.profile.timeline.worker.functions.functions :as f]
						[com.pav.profile.timeline.worker.messages.notification :as ne]
						[clojure.tools.logging :as log]))

(defn parse-and-persist [{:keys [dynamo-opts es-conn]}
												 {:keys [comment-details-table notification-table]}
												 {:keys [type] :as event}]
	(let [event (case type
								"commentreply" (ne/new-comment-reply-notification
																 (f/parse-comment-reply-notification
																	 es-conn dynamo-opts
																	 comment-details-table event))
								nil)]
		(if event
			(do (f/publish-dynamo-notification dynamo-opts notification-table event)
					{:status :success})
			{:status :error})))

(defrecord NotificationEventHandler [connection-opts dynamo-tables]
	comp/Lifecycle EventHandler
	(start [component]
		(log/info "Starting NotificationEventHandler")
		component)
	(stop [component]
		(log/info "Stopping NotificationEventHandler")
		component)
	(handle-event [_ evt]
		(parse-and-persist connection-opts dynamo-tables evt)))

(defn new-notification-event-handler [connection-opts dynamo-tables]
	(map->NotificationEventHandler {:connection-opts connection-opts
																	:dynamo-tables dynamo-tables}))