package com.quickorder.restaurant.core;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegracaoBaseIT {

  public static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("quickorder")
          .withUsername("quickorder")
          .withPassword("quickorder")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../infra/db/bootstrap-dev.sql"),
              "/docker-entrypoint-initdb.d/00-bootstrap-dev.sql");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void registrarPropriedadesDoBanco(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "quickorder_app");
    registry.add("spring.datasource.password", () -> "quickorder_app_dev");
    registry.add("spring.flyway.user", () -> "quickorder");
    registry.add("spring.flyway.password", () -> "quickorder");
    registry.add(
        "spring.flyway.locations", () -> "classpath:db/migration,classpath:db/migration-teste");
  }
}
