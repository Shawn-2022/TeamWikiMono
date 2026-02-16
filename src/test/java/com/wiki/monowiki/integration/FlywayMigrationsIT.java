package com.wiki.monowiki.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke integration test to ensure: - Spring Boot context starts - Flyway migrations execute against real Postgres - PostgreSQL ENUM types exist (roles/status/audit types)
 */
@SpringBootTest
@Testcontainers
class FlywayMigrationsIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("monowiki").withUsername("postgres").withPassword("postgres");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayExecutedAndEnumsExist() {
	assertThat(enumExists("user_role")).isTrue();
	assertThat(enumExists("article_status")).isTrue();
	assertThat(enumExists("audit_event_type")).isTrue();
	assertThat(enumExists("audit_entity_type")).isTrue();
    }

    private boolean enumExists(String enumName) {
	Integer count = jdbc.queryForObject("""
		select count(*) from pg_type t
		  join pg_namespace n on n.oid = t.typnamespace
		where t.typtype = 'e'
		  and t.typname = ?
		""", Integer.class, enumName);
	return count != null && count > 0;
    }
}
