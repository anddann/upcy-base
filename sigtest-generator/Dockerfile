FROM openjdk:8

ENTRYPOINT [ "java", "-jar", "/usr/share/cveanalyzer/cveanalyzer.jar"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY target/lib /usr/share/cveanalyzer/lib

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/cveanalyzer/cveanalyzer.jar


RUN apt update
RUN apt install maven gradle -y
ENV ACTOR_LIMIT=10
ENV SOOT_TIMEOUT_SECONDS=180
