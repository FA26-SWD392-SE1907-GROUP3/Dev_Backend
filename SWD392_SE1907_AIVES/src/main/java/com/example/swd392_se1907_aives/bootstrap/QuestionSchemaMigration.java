package com.example.swd392_se1907_aives.bootstrap;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/** Applies the idempotent compatibility changes after Hibernate has updated mapped columns. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.database.apply-compatibility-migrations",
        havingValue = "true",
        matchIfMissing = true)
public class QuestionSchemaMigration implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        try (var connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) return;
        }
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/06_whole_attempt_rubric.sql"))
                .execute(dataSource);
    }
}
