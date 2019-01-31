# Super simple Dockerfile for starting up our app.
# All configuration comes from the outside (beside the default values in application.yaml)
FROM openjdk:11

EXPOSE 8080

COPY target/random-generator*jar /random-generator.jar
CMD java -jar /random-generator.jar