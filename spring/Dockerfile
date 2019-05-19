# Super simple Dockerfile for starting up our app.
# All configuration comes from the outside (beside the default values in application.yaml)
FROM openjdk:11

# Port to access the random service
EXPOSE 8080

# Install dig for network checks
RUN apt-get update \
 && apt-get install -y dnsutils jq \
 && apt-get clean

# Fat jar are copied over. Ignore version number.
COPY target/random-generator*jar /opt/random-generator.jar

# This runner is supposed to be called with java RandomRunner for calling the batch mode
COPY target/classes/RandomRunner.class /opt/

# Make it running on OpenShift, too
RUN chown -R 451900 /opt \
 && chgrp -R 0 /opt      \
 && chmod -R g=u /opt

# Arbitrary user id. This does currently not work with every example.
# We are trying to get this running, but especially in combination with
# persitent volumes (like the _Stateful Service_ example), there are issues.
# So we leave it better to user root for now.
# USER 451900

# Fire up Spring Boot's fat jar
WORKDIR /opt

# Don't use shell form, or you will have issue with shuttind down on SIGTERM as
# this is not propagated to the JVM process
CMD [ "java", "-jar",  "random-generator.jar" ]
