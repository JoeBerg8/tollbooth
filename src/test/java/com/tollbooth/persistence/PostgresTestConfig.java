package com.tollbooth.persistence;

import com.tollbooth.config.PostgresConfig;
import com.tollbooth.query.Dao;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(
    basePackages = {"com.tollbooth"},
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Dao.class))
@Import(PostgresConfig.class)
public class PostgresTestConfig {}
