package io.k8spatterns.examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

// Some share utility methods
public class IoUtils {


    // Append a line to a given file with locking
    public static void appendLineWithLocking(String file, String line) throws IOException {
        FileOutputStream out = new FileOutputStream(file, true);
        try {
            FileLock lock = out.getChannel().lock();
            try {
                out.write(line.getBytes());
            } finally {
                lock.release();
            }
        } finally {
            out.close();
        }
    }
}
