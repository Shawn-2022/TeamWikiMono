package com.wiki.monowiki.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource, FlywayProperties props) {

	var config = Flyway.configure().dataSource(dataSource).baselineOnMigrate(props.isBaselineOnMigrate());

	// Locations (auth/core/audit folders)
	if (!props.getLocations().isEmpty()) {
	    config.locations(props.getLocations().toArray(String[]::new));
	}

	// Table name (optional)
	if (!props.getTable().isBlank()) {
	    config.table(props.getTable());
	}

	// Schemas (Boot 4 returns List<String>)
	if (!props.getSchemas().isEmpty()) {
	    config.schemas(props.getSchemas().toArray(String[]::new));
	}

	return config.load();
    }
}
