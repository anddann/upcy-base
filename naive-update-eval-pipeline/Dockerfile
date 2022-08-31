FROM maven:3.8.4-openjdk-8

ENTRYPOINT ["/bin/bash", "-c", "exec java -jar /usr/share/myservice/myservice.jar \"$@\"", "bash"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY target/lib /usr/share/myservice/lib

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/myservice.jar
ENV ACTOR_LIMIT=100

# let the repo url always end with /
ENV MAVEN_REPO_URL=https://repo1.maven.org/maven2/
ENV PROJECTS_DOWNLOAD_URL=https://zenodo.org/record/4479015/files/projects.zip?download=1
