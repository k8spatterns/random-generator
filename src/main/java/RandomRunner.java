import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

import io.k8spatterns.examples.IoUtils;

/**
 * Simple Batch runner for creating a certain amount of entries in a log
 * holding log files. This is a standalone class used in the Batch Job and Scheduled Job patterns
 */
public class RandomRunner {

    // Simples possible way to create a random number.
    private static Random random = new Random();

    // Simple UUID to identify this server instance
    private static UUID id = UUID.randomUUID();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java -jar ... " +
                               RandomRunner.class.getCanonicalName() +
                               " path-to-file number-lines-to-create");
            System.exit(1);
        }

        String file = args[0];
        int nrLines = Integer.parseInt(args[1]);

        long overallStart = System.nanoTime();
        System.out.println("Starting to create " + nrLines + " random numbers and store in " + args[0]);
        for (int i = 0; i < nrLines; i++) {
            long start = System.nanoTime();
            String date = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                           .withZone(ZoneOffset.UTC)
                                           .format(Instant.now());
            int randomValue = random.nextInt();
            String line = date + "," + id + "," + (System.nanoTime() - start) + "," + randomValue + "\n";

            IoUtils.appendLineWithLocking(file, line);
        }
        System.out.println("Finished after " + ((System.nanoTime() - overallStart) / 1_000_000) + " ms");
    }
}
