package com.studyflow.community;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityMigrationTest {
    @Test
    void v6BackfillsExistingUsersIntoDefaultCircle() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:community_migration_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("5"))
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (username, email, password_hash)
                    VALUES ('legacy_circle_user', 'legacy_circle_user@example.com', 'hash')
                    """);
        }

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet membership = statement.executeQuery("""
                    SELECT cm.role, cm.status
                    FROM circle_members cm
                    JOIN circles c ON c.id = cm.circle_id
                    JOIN users u ON u.id = cm.user_id
                    WHERE c.slug = 'violet-circle'
                      AND u.username = 'legacy_circle_user'
                    """);
            assertThat(membership.next()).isTrue();
            assertThat(membership.getString("role")).isEqualTo("MEMBER");
            assertThat(membership.getString("status")).isEqualTo("ACTIVE");

            ResultSet profile = statement.executeQuery("""
                    SELECT up.display_name
                    FROM user_profiles up
                    JOIN users u ON u.id = up.user_id
                    WHERE u.username = 'legacy_circle_user'
                    """);
            assertThat(profile.next()).isTrue();
            assertThat(profile.getString("display_name")).isEqualTo("legacy_circle_user");

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO circle_members (circle_id, user_id, role, status)
                    VALUES (999999, 999999, 'MEMBER', 'ACTIVE')
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }
}
