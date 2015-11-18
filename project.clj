(defproject pav_profile_timeline_worker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/core.async "0.2.374"]
                 [com.taoensso/carmine "2.12.0" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/faraday "1.8.0" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [clojure-msgpack "1.1.2"]
                 [clojurewerkz/elastisch "2.1.0"]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :env {:redis-url "redis://127.0.0.1:6379"
                         :es-url "http://localhost:9200"
                         :input-queue "redismq::queue_name::user-event-input"
                         :processing-queue "redismq::queue_name::user-event-processing"}
                   :source-paths ["dev"]}})
