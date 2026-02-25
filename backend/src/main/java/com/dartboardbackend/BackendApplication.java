package com.dartboardbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Dartboard Backend backend service.
 *
 * <p>Bootstraps the application context, auto-configuration, and component scanning.
 */
@SpringBootApplication
public class BackendApplication {

  /**
   * Launches the Spring Boot application.
   *
   * @param args command-line arguments forwarded to {@link SpringApplication#run}
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
