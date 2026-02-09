package com.tollbooth.config;

import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class PostgresConfig {

  private static final String SCHEMA = "public";

  @Value("${spring.datasource.url}")
  private String databaseUrl;

  @Value("${spring.datasource.username}")
  private String databaseUsername;

  @Value("${spring.datasource.password}")
  private String databasePassword;

  @Bean
  DataSource dataSource() throws URISyntaxException {
    return buildDataSource(databaseUrl, databaseUsername, databasePassword, 20);
  }

  @Bean
  Flyway flyway(DataSource dataSource) {
    Flyway flyway =
        Flyway.configure().schemas(SCHEMA).cleanDisabled(true).dataSource(dataSource).load();
    flyway.migrate();
    return flyway;
  }

  @Bean
  @DependsOn("flyway")
  NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Bean
  @DependsOn("flyway")
  PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  public static DataSource buildDataSource(
      String databaseUrl, String username, String password, int poolSize)
      throws URISyntaxException {
    var dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    String dbUrl;
    String dbUsername;
    String dbPassword;

    // Check if URL is already in JDBC format (e.g., jdbc:postgresql://host:port/db)
    if (databaseUrl.startsWith("jdbc:")) {
      // Use JDBC URL directly and use provided username/password
      dbUrl = databaseUrl;
      dbUsername = username;
      dbPassword = password;
    } else {
      // Legacy format: postgres://user:pass@host:port/db
      // Parse credentials from URI
      URI dbUri = new URI(databaseUrl);
      String userInfo = dbUri.getUserInfo();
      if (userInfo != null && userInfo.contains(":")) {
        String[] credentials = userInfo.split(":", 2);
        dbUsername = credentials[0];
        dbPassword = credentials.length > 1 ? credentials[1] : "";
      } else {
        // Fallback to provided username/password if URI doesn't have userInfo
        dbUsername = username;
        dbPassword = password;
      }
      // Construct JDBC URL from URI components
      dbUrl =
          StringUtils.join(
              "jdbc:postgresql://", dbUri.getHost(), ":", dbUri.getPort(), dbUri.getPath());
    }

    dataSource.setUrl(dbUrl);
    dataSource.setUsername(dbUsername);
    dataSource.setPassword(dbPassword);
    dataSource.setInitialSize(poolSize);
    dataSource.setMaxActive(poolSize);
    dataSource.setMinIdle(poolSize);
    dataSource.setMaxIdle(poolSize);
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setValidationQuery("SELECT 1");
    dataSource.setTestOnReturn(true);
    dataSource.setTestOnBorrow(true);
    dataSource.setTestWhileIdle(true);
    return dataSource;
  }
}
