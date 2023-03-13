import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Simple Batch runner for creating a certain amount of entries in a log
 * holding log files. This is a standalone class used in the Batch Job and Scheduled Job patterns
 */
public class RandomRunner {

    // Simples possible way to create a random number.
    private static final Random random = new Random();

    // Simple UUID to identify this server instance
    private static final UUID id = UUID.randomUUID();

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            System.err.println("Usage: java -jar ... " +
                               RandomRunner.class.getCanonicalName() +
                               " path-to-file number-lines-to-create [seconds-to-sleep]");
            System.exit(1);
        }

        String file = args[0];
        int nrLines = Integer.parseInt(args[1]);
        int sleep = 0;
        if (args.length == 3) {
            sleep = Integer.parseInt(args[2]);
        }
        while (true) {
            long overallStart = System.nanoTime();
            System.out.println("Starting to create " + nrLines + " random numbers and store in " + args[0]);
            for (int i = 0; i < nrLines; i++) {
                long start = System.nanoTime();
                String date = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                               .withZone(ZoneOffset.UTC)
                                               .format(Instant.now());
                int randomValue = random.nextInt();
                String line = date + "," + id + "," + (System.nanoTime() - start) + "," + randomValue + "\n";

                appendLineWithLocking(file, line);
            }
            System.out.println("Finished after " + ((System.nanoTime() - overallStart) / 1_000_000) + " ms");
            if (sleep == 0) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted, exiting");
                System.exit(1);
            }
        }
    }

    private static void appendLineWithLocking(String file, String line) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            FileLock lock = out.getChannel().lock();
            try {
                out.write(line.getBytes());
            } finally {
                lock.release();
            }
        }
    }
}
