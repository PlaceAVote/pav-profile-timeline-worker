(ns com.pav.profile.timeline.worker.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
						[clojurewerkz.elastisch.rest :as esr]
            [com.pav.profile.timeline.worker.component.redis-event-consumer :refer [new-redis-queue-consumer]]
						[com.pav.profile.timeline.worker.component.timeline-event-handler :refer [new-timeline-event-handler]]
						[com.pav.profile.timeline.worker.component.notification-event-handler :refer [new-notification-event-handler]]
						[com.pav.profile.timeline.worker.component.email-event-handler :refer [new-email-event-handler]]))

(def es-conn (esr/connect (:es-url env)))

(def client-opts {:access-key (:access-key env)
									:secret-key (:secret-key env)
									:endpoint (:dynamo-endpoint env)})

(def connection-opts {:dynamo-opts client-opts
											:es-conn es-conn})

(def dynamo-tables {:timeline-table        (:dynamo-usertimeline-table-name env)
										:user-table            (:dynamo-user-table-name env)
										:notification-table    (:dynamo-usernotification-table-name env)
										:comment-details-table (:dynamo-comment-details-table-name env)})

(def mandril-opts {:api-key 				 (:mandril-api-key env)
									 :comment-template (:mandril-comment-template env)})

(defn new-system []
	(component/system-map
		:timeline-event-handler 		 (new-timeline-event-handler connection-opts dynamo-tables)
		:notification-event-handler  (new-notification-event-handler connection-opts dynamo-tables)
		:email-event-handler				 (new-email-event-handler connection-opts dynamo-tables mandril-opts)
		:timeline-event-consumer 		 (component/using (new-redis-queue-consumer (:redis-url env) (:timeline-queue env) 3)
																	 {:message-handler :timeline-event-handler})
		:notification-event-consumer (component/using (new-redis-queue-consumer (:redis-url env) (:notification-queue env) 3)
																	 {:message-handler :notification-event-handler})
		:email-notification-worker 	 (component/using (new-redis-queue-consumer (:redis-url env) (:email-notification-queue env) 3)
																	 {:message-handler :email-event-handler})))
