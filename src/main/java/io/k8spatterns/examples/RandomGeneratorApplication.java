package io.k8spatterns.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

@SpringBootApplication
@RestController
public class RandomGeneratorApplication {

    // File to indicate readiness
    private static File READY_FILE = new File("/random-generator-ready");

	// Simples possible way to create a random number.
    // Could be replaced by access to a hardware number generator if
    // you want to take this service seriously :)
    private static Random random = new Random();

    // Simple UUID to identify this server instance
    private static UUID id = UUID.randomUUID();

    // Injected from the application properties
    @Value("${version}")
    private String version;

    // Optional path to store data in (env: LOG_FILE)
    @Value("${log.file:#{null}}")
    private String logFile;

    // Build type (env: BUILD_TYPE)
    @Value("${build.type:#{null}}")
    private String buildType;

    // Pattern name where this service is used
    @Value("${pattern:None}")
    private String patternName;

    // Used to allocate memory
    private byte[] memoryHole;

    // Some logging
    private static Log log = LogFactory.getLog(RandomGeneratorApplication.class);

    // Health toggle
    @Autowired
    private HealthToggleIndicator healthToggle;

    // ======================================================================
    public static void main(String[] args) throws InterruptedException, IOException {

        // Ensure that the ready flag is not enabled
        ready(false);

        // Delay startup if a certain envvar is set
        delayIfRequested();

        SpringApplication.run(RandomGeneratorApplication.class, args);
	}


    // Dump out some information
    @EventListener(ApplicationReadyEvent.class)
    public void dumpSysInfo() {
        Map info = getSysinfo();

        log.info("=== Max Heap Memory:  " + info.get("memory.max") + " MB");
        log.info("=== Used Heap Memory: " + info.get("memory.used") + " MB");
        log.info("=== Free Memory:      " + info.get("memory.free") + " MB");
        log.info("=== Processors:       " + info.get("cpu.procs"));
    }


    // Indicate, that our application is up and running
    @EventListener(ApplicationReadyEvent.class)
    public void createReadyFile() throws IOException {
        ready(true);
    }

    /**
     * Endpoint returning a JSON document with a random number. It maps to the root context
     * of this web application
     *
     * @return map which gets convert to JSON when returned to the client
     */
    @RequestMapping(value = "/", produces = "application/json")
    public Map getRandomNumber(@RequestParam(name = "burn", required = false) Long burn) throws IOException {
        Map<String, Object> ret = new HashMap<>();

        // Excercise the CPU a bit if requested
        burnCpuTimeIfRequested(burn);

        // The very random number.
        int randomValue = random.nextInt();

        // Create return value
        ret.put("random", randomValue);
        ret.put("id", id.toString());

        // Write out the value to a file
        logRandomValue(randomValue);
        return ret;
    }

    /**
     * Reserve some memory to simulate memory heap usage
     *
     * @param megaBytes how many MB to reserve
     */
    @RequestMapping(value = "/memory-eater")
    public void getEatUpMemory(@RequestParam(name = "mb") int megaBytes) {
        memoryHole = new byte[megaBytes * 1024 * 1024];
        random.nextBytes(memoryHole);
    }

    /**
     * Toggle the overall health of the system
     */
    @RequestMapping(value = "/toggle-live")
    public void toggleLiveness() {
        healthToggle.toggle();
    }

    /**
     * Toggle the overall health of the system
     */
    @RequestMapping(value = "/toggle-ready")
    public void toggleReadiness() throws IOException {
        ready(!READY_FILE.exists());
    }


    /**
     * Get some info
     * @return overall information including version, and id
     */
    @RequestMapping(value = "/info", produces = "application/json")
    public Map info() {
        return getSysinfo();
    }


    // ================================================================================================
    // Burn down some CPU time, which can be used to increase the CPU load on
    // the system
    private void burnCpuTimeIfRequested(Long burn) {
        if (burn != null) {
            for (int i = 0; i < (burn < 1_000 ? burn : 1_000) * 1_000 * 1_000; i++) {
                random.nextInt();
            }
        }
    }

    // Write out the random value to a given path (if configured)
    private void logRandomValue(int randomValue) throws IOException {
        if (logFile != null) {
            String date = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                           .withZone(ZoneOffset.UTC)
                                           .format(Instant.now());
            String line = date + "," + id + "," + randomValue + "\n";
            Files.write(Paths.get(logFile), line.getBytes(), CREATE, APPEND);
        }
    }

    // If the given environment variable is set, sleep a bit
    private static void delayIfRequested() throws InterruptedException {
        int sleep = Integer.parseInt(System.getenv().getOrDefault("DELAY_STARTUP", "0"));
        if (sleep > 0) {
            Thread.sleep(sleep * 1000);
        }
    }

    // Get some sysinfo
    private Map getSysinfo() {
        Map ret = new HashMap();
        Runtime rt = Runtime.getRuntime();
        int mb = 1024 * 1024;
        ret.put("memory.max", rt.maxMemory() / mb);
        ret.put("memory.used", rt.totalMemory() / mb);
        ret.put("memory.free", rt.freeMemory() / mb);
        ret.put("cpu.procs", rt.availableProcessors());
        ret.put("id", id);
        ret.put("version", version);
        ret.put("pattern", patternName);
        if (buildType != null) {
            ret.put("build-type", buildType);
        }
        return ret;
    }


    // Set the ready flag file
    static private void ready(boolean create) throws IOException {
        if (create) {
            new FileOutputStream(READY_FILE).close();
        } else {
            if (READY_FILE.exists()) {
                READY_FILE.delete();
            }
        }
    }


}
