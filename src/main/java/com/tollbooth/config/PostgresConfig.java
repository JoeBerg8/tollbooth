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

  @Bean
  DataSource dataSource() throws URISyntaxException {
    return buildDataSource(databaseUrl, 20);
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

  public static DataSource buildDataSource(String databaseUrl, int poolSize)
      throws URISyntaxException {
    URI dbUri = new URI(databaseUrl);
    var dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    String dbUrl =
        StringUtils.join(
            "jdbc:postgresql://", dbUri.getHost(), ":", dbUri.getPort(), dbUri.getPath());
    dataSource.setUrl(dbUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
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
