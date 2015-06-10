FROM clojure:lein-2.7.1-alpine

COPY . /opt/app
WORKDIR /opt/app

RUN lein deps
RUN lein uberjar

CMD ["java", "-jar", "/opt/app/target/realtime.jar"]
