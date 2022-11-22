# Multistage Dockerfile for building random-generator 
FROM adoptopenjdk/maven-openjdk11 as BUILD

# Copy over source into the container
COPY spring /opt/random-generator
WORKDIR /opt/random-generator
# Build jar files
RUN mvn install

# --------------------------------
# Runtime image
FROM openjdk
# Copy over artefacts
COPY --from=BUILD /opt/random-generator/target/random-generator*jar /opt/random-generator.jar
COPY --from=BUILD /opt/random-generator/target/classes/RandomRunner.class /opt

# Setup env
WORKDIR /opt
EXPOSE 8080
USER 1001

# Execute jar file
ENTRYPOINT [ "java", "-jar", "/opt/random-generator.jar" ]
