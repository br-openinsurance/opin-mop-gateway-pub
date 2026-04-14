package br.com.opin.mopclient;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MOP Client - Main application entry point.
 * Bootstraps and configures the context for gateway, validator and anonymization modules.
 */
@SpringBootApplication(proxyBeanMethods = false)
@EnableCaching
@EnableScheduling
public final class MopClientApplication {

    private MopClientApplication() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MopClientApplication.class);
        application.setLazyInitialization(true);
        application.setBannerMode(Banner.Mode.CONSOLE);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
