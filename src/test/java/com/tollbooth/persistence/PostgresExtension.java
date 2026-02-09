package com.tollbooth.persistence;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresExtension implements BeforeAllCallback, AfterAllCallback {

  private static PostgreSQLContainer<?> postgres;

  private static PostgreSQLContainer<?> getPostgres() {
    if (postgres == null) {
      @SuppressWarnings("resource")
      PostgreSQLContainer<?> container =
          new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("16"))
              .withDatabaseName("postgres")
              .withUsername("postgres")
              .withPassword("password");
      postgres = container;
    }
    return postgres;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    PostgreSQLContainer<?> container = getPostgres();
    container.start();
    // Set system property in postgres:// URI format that PostgresConfig.buildDataSource()
    // expects
    String databaseUrl =
        String.format(
            "postgres://postgres:password@%s:%s/postgres",
            container.getHost(), container.getMappedPort(5432));
    System.setProperty("spring.datasource.url", databaseUrl);
    assertThat(container.isRunning()).isTrue();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty("spring.datasource.url");
    if (postgres != null && postgres.isRunning()) {
      postgres.close();
    }
  }
}
