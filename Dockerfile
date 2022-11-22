# Multistage Dockerfile for building random-generator 
FROM adoptopenjdk/maven-openjdk11 as BUILD

# Copy over source into the container
COPY ./ /opt/random-generator/
WORKDIR /opt/random-generator
# Build jar files
RUN mvn install -f spring/pom.xml

# --------------------------------
# Runtime image
FROM openjdk
# Copy over artefacts
COPY --from=BUILD /opt/random-generator/spring/target/random-generator*jar /opt/random-generator.jar
COPY --from=BUILD /opt/random-generator/spring/target/classes/RandomRunner.class /opt

# Setup env
WORKDIR /opt
EXPOSE 8080
USER 1001

# Execute jar file
ENTRYPOINT [ "java", "-jar", "/opt/random-generator.jar" ]
