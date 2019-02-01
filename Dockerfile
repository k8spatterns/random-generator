# Super simple Dockerfile for starting up our app.
# All configuration comes from the outside (beside the default values in application.yaml)
FROM openjdk:11

# Port to access the random service
EXPOSE 8080

# Fat jar are copied over. Ignore version number.
COPY target/random-generator*jar /random-generator.jar

# This runner is supposed to be runnable standalone in batch mode
COPY target/classes/RandomRunner.class /

# Fire up Spring Boot's fat jar
CMD java -jar /random-generator.jar