FROM openjdk:8

ENTRYPOINT ["/bin/bash", "-c", "exec java -jar /usr/share/myservice/myservice.jar \"$@\"", "bash"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY target/lib /usr/share/myservice/lib

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/myservice.jar


RUN apt update
RUN apt install maven gradle -y
ENV ACTOR_LIMIT=10
ENV SOOT_TIMEOUT_SECONDS=180
