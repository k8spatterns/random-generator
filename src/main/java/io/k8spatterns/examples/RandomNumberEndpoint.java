package io.k8spatterns.examples;

import java.util.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RandomNumberEndpoint {

    // Simples possible way to create a random number.
    // Could be replaces by access to a hardware number generator if
    // you want to take this service seriously :)
    private static Random random = new Random();

    // Simple UUID to identify this server instance
    private static UUID id = UUID.randomUUID();

    /**
     * Endpoint returning a JSON document with a random number. It mapps to the root context
     * of this webapplication
     *
     * @return map which gets convert to JSON when returned to the client
     */
    @RequestMapping(value = "/", produces = "application/json")
    public Map getRandomNumber() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("random", random.nextInt());
        ret.put("id", id.toString());
        String environment = System.getenv("RANDOM_GENERATOR_ENV");
        if (environment != null) {
            ret.put("environemnt", environment);
        }
        return ret;
    }
}
