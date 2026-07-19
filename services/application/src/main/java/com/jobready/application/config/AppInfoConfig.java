package com.jobready.application.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gauge carrying the running version as a label, so Grafana can
 * correlate releases with behaviour changes: app_info{version="sha-abc1234"} 1.
 * APP_VERSION is the image tag injected by Helm; "dev" when run locally.
 */
@Configuration
public class AppInfoConfig {

    @Bean
    public Gauge appInfo(MeterRegistry registry, @Value("${APP_VERSION:dev}") String version) {
        return Gauge.builder("app_info", () -> 1.0)
                .description("Deployed application version (value is constant 1)")
                .tag("version", version)
                .register(registry);
    }
}
