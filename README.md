## Random Number Generator Service

This is a very simple Spring Boot service which just generates a random number and returns it as JSON:

```
curl -sL http://localhost:8080/ | jq .
{
  "random": 838265052,
  "id": "58da4c5b-ba06-438e-84f8-1a30581baeb8"
}
```

Start the service with either `./mvnw spring-boot:run` or, after building with `./mvnw package`, with `java -jar target/random-generator-*.jar`

An image can be created directly via `./mvnw package fabric8:build`.

Refer to the [Image Builder](https://github.com/k8spatterns/examples/tree/master/advanced/ImageBuilder) pattern in the Kubernetes Patterns books [example repository](https://github.com/k8spatterns/examples) how this example is build from within a Kubernetes cluster.