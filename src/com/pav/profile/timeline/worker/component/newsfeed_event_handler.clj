(ns com.pav.profile.timeline.worker.component.newsfeed-event-handler
	(:require [com.stuartsierra.component :as comp]
						[clojure.tools.logging :as log]
						[com.pav.profile.timeline.worker.component.common :refer [EventHandler]]))

(defn parse-and-persist [{:keys [dynamo-opts es-conn]}
												 {:keys [user-table timeline-table]}
												 {:keys [type] :as event}]
	(let [event (case type
								"vote" ""
								nil)]
		(if event
			(do;(f/publish-to-dynamo-timeline dynamo-opts timeline-table event)
					{:status :success})
			{:status :error})))

(defrecord NewsfeedEventHandler [connection-opts dynamo-tables]
	comp/Lifecycle EventHandler
	(start [component]
		(log/info "Starting Newsfeed Event Handler")
		component)
	(stop [component]
		(log/info "Stopping Newsfeed Event Handler")
		component)
	(handle-event [_ evt]
		(parse-and-persist connection-opts dynamo-tables evt)))

(defn new-newsfeed-event-handler [connection-opts dynamo-tables]
	(map->NewsfeedEventHandler {:connection-opts connection-opts
															:dynamo-tables dynamo-tables}))
