(defproject pav_profile_timeline_worker "0.1.2-SNAPSHOT"
  :description "Process user activities from Redis and update the users timeline."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/core.async "0.2.374"]
                 [com.taoensso/carmine "2.12.1" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/faraday "1.8.0" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [clojure-msgpack "1.1.2"]
                 [clojurewerkz/elastisch "2.1.0"]
                 [environ "1.0.0"]
                 [prismatic/schema "1.0.3"]
								 [clj-http "2.0.0"]]
  :plugins [[lein-environ "1.0.0"]
            [lein-release "1.0.5"]]
  :lein-release {:scm :git
                 :deploy-via :lein-install
                 :build-uberjar true}
  :min-lein-version "2.0.0"
  :main com.pav.profile.timeline.worker.main
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :profiles {:uberjar {:aot [com.pav.profile.timeline.worker.main]
                       :uberjar-name "pav-profile-timeline-worker.jar"}
						 :dev     {:dependencies [[org.clojure/tools.namespace "0.2.4"]
																			[midje "1.7.0"]]
											 :env          {:redis-url                          "redis://127.0.0.1:6379"
																			:es-url                             "http://localhost:9200"
																			:dynamo-endpoint                    "http://localhost:8000"
																			:dynamo-user-table-name             "users"
																			:dynamo-usertimeline-table-name     "usertimeline"
																			:dynamo-usernotification-table-name "usernotifications"
																			:dynamo-comment-details-table-name  "comment-details"
																			:access-key                         "Whatever"
																			:secret-key                         "whatever"
																			:timeline-queue                     "redismq::queue_name::user-timelineevent-queue"
																			:notification-queue									"redismq::queue_name::user-notification-queue"
																			:email-notification-queue					  "redismq::queue_name::email-notification-queue"
																			:newsfeed-notification-queue				"redismq::queue_name::user-newsfeed-queue"
																			:redis-notification-pubsub					"redis::notifications::pubsub"
																			:mandril-api-key 										"key"
																			:mandril-comment-template						"comment-reply-template-dev"}
											 :source-paths ["dev" "src"]
											 :plugins      [[lein-midje "3.1.3"]]}})
