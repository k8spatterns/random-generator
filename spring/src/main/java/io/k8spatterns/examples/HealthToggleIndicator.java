package io.k8spatterns.examples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Simple toggel to swith on/off the health check
@Component
public class HealthToggleIndicator implements HealthIndicator {

    @Value("${healthy:true}")
    private boolean healthy;

    @Override
    public Health health() {
        Health.Builder builder = healthy ? Health.up() : Health.down();
        return builder.withDetail("toggle", healthy).build();
    }

    public void toggle() {
        this.healthy = !this.healthy;
    }
}
