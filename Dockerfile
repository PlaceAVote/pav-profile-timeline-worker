FROM clojure

WORKDIR /app
COPY . /app

RUN cp target/pav_profile_timeline_worker-0.1.0-SNAPSHOT-standalone.jar pav-profile-timeline-worker.jar

RUN ls -ltr

CMD java -jar pav-profile-timeline-worker.jar
