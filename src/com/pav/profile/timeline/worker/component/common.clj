(ns com.pav.profile.timeline.worker.component.common)

(defprotocol EventHandler
	(handle-event [this evt] "Process by event type, will return status to indicate success or failure"))
