package io.k8spatterns.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Signal;

@SpringBootApplication
@RestController
public class RandomGeneratorApplication implements ApplicationContextAware  {

    // File to indicate readiness
    private static File READY_FILE = new File("/opt/random-generator-ready");

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

    // Optional url to log the data to(env: LOG_URL)
    @Value("${log.url:#{null}}")
    private String logUrl;

    // Build type (env: BUILD_TYPE)
    @Value("${build.type:#{null}}")
    private String buildType;

    // Pattern name where this service is used
    @Value("${pattern:None}")
    private String patternName;

    // Seed to use for initializing the random number generator
    @Value("${seed:0}")
    private long seed;

    // Used to allocate memory, testing resource limits
    private byte[] memoryHole;

    // Some logging
    private static Log log = LogFactory.getLog(RandomGeneratorApplication.class);

    // Health toggle
    private HealthToggleIndicator healthToggle;

    // Application context used for shutdown the app
    private ApplicationContext applicationContext;


    public RandomGeneratorApplication(HealthToggleIndicator healthToggle) {
        this.healthToggle = healthToggle;
    }

    // ======================================================================
    public static void main(String[] args) throws InterruptedException, IOException {

        // Check for a postStart generated file and log if it found one
        waitForPostStart();

        // shutdownHook & signalHandler for dealing with signals
        addShutdownHook();

        // Ensure that the ready flag is not enabled
        ready(false);

        // Delay startup if a certain envvar is set
        delayIfRequested();

        SpringApplication.run(RandomGeneratorApplication.class, args);
	}

    // Dump out some information
    @EventListener(ApplicationReadyEvent.class)
    public void dumpInfo() throws IOException {

        Map info = getSysinfo();

        log.info("=== Max Heap Memory:  " + info.get("memory.max") + " MB");
        log.info("=== Used Heap Memory: " + info.get("memory.used") + " MB");
        log.info("=== Free Memory:      " + info.get("memory.free") + " MB");
        log.info("=== Processors:       " + info.get("cpu.procs"));
    }


    // Indicate, that our application is up and running
    @EventListener(ApplicationReadyEvent.class)
    public void createReadyFile() {
        try {
            ready(true);
        } catch (IOException exp) {
            log.warn("Can't create 'ready' file " + READY_FILE +
                     " used in readiness check. Possibly running locally, so ignoring it");
        }
    }

    // Init seed if configured
    @EventListener(ApplicationReadyEvent.class)
    public void initSeed() {
        if (seed != 0) {
            random = new Random(seed);
        }
    }

    /**
     * Endpoint returning a JSON document with a random number. It maps to the root context
     * of this web application
     *
     * @return map which gets convert to JSON when returned to the client
     */
    @RequestMapping(value = "/", produces = "application/json")
    public Map getRandomNumber(@RequestParam(name = "burn", required = false) Long burn) throws IOException {
        long start = System.nanoTime();
        Map<String, Object> ret = new HashMap<>();

        // Excercise the CPU a bit if requested
        burnCpuTimeIfRequested(burn);

        // The very random number.
        int randomValue = random.nextInt();

        // Create return value
        ret.put("random", randomValue);
        ret.put("id", id.toString());

        // Write out the value to a file
        logRandomValue(randomValue, System.nanoTime() - start);
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
    public Map info() throws IOException {
        return getSysinfo();
    }

    /**
     * Shutdown called by preStop hook in the Lifecycle Conformance pattern example
     * Log and stop the Spring Boot container
     */
    @RequestMapping(value = "/shutdown")
    public void shutdown() {
        log.info("SHUTDOWN NOW");
        SpringApplication.exit(applicationContext);
    }

    /**
     * Get all log data that has been already written
     */
    @RequestMapping(value = "/logs", produces = "text/plain")
    public String logs() throws IOException {
        return getLog();
    }

    // ================================================================================================
    // Burn down some CPU time, which can be used to increase the CPU load on
    // the system
    private void burnCpuTimeIfRequested(Long burn) {
        if (burn != null) {
            for (int i = 0; i < (burn < 10_000 ? burn : 10_000) * 1_000 * 1_000; i++) {
                random.nextInt();
            }
        }
    }

    // Write out the random value to a given path (if configured) and/or an external URL
    private void logRandomValue(int randomValue, long duration) throws IOException {
        if (logFile != null) {
            logToFile(logFile, randomValue, duration);
        }

        if (logUrl != null) {
            logToUrl(new URL(logUrl), randomValue, duration);
        }
    }

    private void logToFile(String file, int randomValue, long duration) throws IOException {
        String date = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                       .withZone(ZoneOffset.UTC)
                                       .format(Instant.now());
        String line = date + "," + id + "," + duration + "," + randomValue + "\n";
        IoUtils.appendLineWithLocking(file, line);
    }

    private void logToUrl(URL url, int randomValue, long duration) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");

		String output = String.format(
            "{ \"id\": \"%s\", \"random\": \"%d\", \"duration\": \"%d\" }",
            id, randomValue, duration);

		log.info("Sending log to " + url);
		connection.setDoOutput(true);
		OutputStream os = connection.getOutputStream();
		os.write(output.getBytes());
		os.flush();
		os.close();

		int responseCode = connection.getResponseCode();
		log.info("Log delegate response: " + responseCode);
    }


    // If the given environment variable is set, sleep a bit
    private static void delayIfRequested() throws InterruptedException {
        int sleep = Integer.parseInt(System.getenv().getOrDefault("DELAY_STARTUP", "0"));
        if (sleep > 0) {
            Thread.sleep(sleep * 1000);
        }
    }

    // Get the content of the log file
    private String getLog() throws IOException {
        if (logFile == null) {
            return "";
        }
        return String.join("\n", Files.readAllLines(Paths.get(logFile)));
    }

    // Get some sysinfo
    private Map getSysinfo() throws IOException {
        Map ret = new HashMap();
        Runtime rt = Runtime.getRuntime();
        int mb = 1024  * 1024;
        ret.put("memory.max", rt.maxMemory() / mb);
        ret.put("memory.used", rt.totalMemory() / mb);
        ret.put("memory.free", rt.freeMemory() / mb);
        ret.put("cpu.procs", rt.availableProcessors());
        ret.put("id", id);
        ret.put("version", version);
        ret.put("pattern", patternName);
        if (logFile != null) {
            ret.put("logFile", logFile);
        }
        if (logUrl != null) {
            ret.put("logUrl", logUrl);
        }
        if (seed != 0) {
            ret.put("seed", seed);
        }
        if (buildType != null) {
            ret.put("build-type", buildType);
        }

        // From Downward API environment
        Map<String, String> env = System.getenv();
        for (String key : new String[] { "POD_IP", "NODE_NAME"}) {
            if (env.containsKey(key)) {
                ret.put(key, env.get(key));
            }
        }

        // From Downward API mount
        File podInfoDir = new File("/pod-info");
        if (podInfoDir.exists() && podInfoDir.isDirectory()) {
            for (String meta : new String[] { "labels", "annotations"}) {
                File file = new File(podInfoDir, meta);
                if (file.exists()) {
                    byte[] encoded = Files.readAllBytes(file.toPath());
                    ret.put(file.getName(), new String(encoded));
                }
            }
        }

        // Add environment
        ret.put("env", System.getenv());

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

    // Check for a file which has supposedly be created as part of postStart Hook
    private static void waitForPostStart() throws IOException, InterruptedException {
        if ("true".equals(System.getenv("WAIT_FOR_POST_START"))) {
            File postStartFile = new File("/opt/postStart-done");
            while (!postStartFile.exists()) {
                log.info("Waiting for postStart to be finished ....");
                Thread.sleep(10_000);
            }
            log.info("postStart Message: " + new String(Files.readAllBytes(postStartFile.toPath())));
        } else {
            log.info("No WAIT_FOR_POST_START configured");
        }
    }

    // Simple shutdown hook to catch some signals. If you have another idea to catch a SIGTERM, please
    // let us know by opening an issue in this repo or send in a pull request.
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () -> log.info(">>>> SHUTDOWN HOOK called. Possibly because of a SIGTERM from Kubernetes")));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
