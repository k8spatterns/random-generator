package io.k8spatterns.examples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Simple toggle to switch on/off the health check
@Component
public class HealthToggleIndicator implements HealthIndicator {

    @Value("${healthy:true}")
    private boolean healthy;

    @Override
    public Health health() {
        Health.Builder builder = healthy ? Health.up() : Health.down();
        Runtime r = Runtime.getRuntime();
        return builder
            .withDetail("toggle", healthy)
            .withDetail("usedMemory", format(r.totalMemory() - r.freeMemory()))
            .withDetail("totalMemory", format(r.totalMemory()))
            .withDetail("maxMemory", format(r.maxMemory()))
            .withDetail("freeMemory", format(r.freeMemory()))
            .withDetail("availableProcessors", r.availableProcessors())
            .build();
    }

    public void toggle() {
        this.healthy = !this.healthy;
    }

    public static String format(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB",
                             (double)v / (1L << (z*10)),
                             " KMGTPE".charAt(z));
    }
}
