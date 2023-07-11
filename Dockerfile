# Multistage Dockerfile for building random-generator 
FROM eclipse-temurin:17-jdk as BUILD
ARG VERSION=v1

RUN apt-get update -y \
 && apt-get install -y maven

# Copy over source into the container
COPY spring /opt/random-generator/spring
COPY pom.xml /opt/random-generator

WORKDIR /opt/random-generator
# Build jar files
RUN mvn install -P ${VERSION} -f spring/pom.xml

# --------------------------------
# Runtime image
FROM eclipse-temurin:17-jre

# Copy over artifacts
COPY --from=BUILD /opt/random-generator/spring/target/random-generator*jar /opt/random-generator.jar
COPY --from=BUILD /opt/random-generator/spring/target/classes/RandomRunner.class /opt
COPY random-generator-runner /usr/local/bin
RUN chmod a+x /usr/local/bin/random-generator-runner

# Setup env
WORKDIR /opt
EXPOSE 8080

# We need root for some examples that write to PVs. However, if possible
# you should avoid this
USER 0

# Execute jar file
ENTRYPOINT [ "java", "-jar", "/opt/random-generator.jar" ]
