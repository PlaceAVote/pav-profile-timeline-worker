FROM clojure

WORKDIR /app
COPY . /app

RUN cp target/pav-profile-timeline-worker.jar pav-profile-timeline-worker.jar

RUN ls -ltr

CMD java -jar pav-profile-timeline-worker.jar
