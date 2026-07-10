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
        JdbcDataSource dataSource = newDataSource();

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
            assertDefaultMembershipAndProfile(statement, "legacy_circle_user");

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO circle_members (circle_id, user_id, role, status)
                    VALUES (999999, 999999, 'MEMBER', 'ACTIVE')
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void latestMigrationRepairsActiveUsersMissingDefaultCircleDataAfterV6() throws Exception {
        JdbcDataSource dataSource = newDataSource();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("6"))
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (username, email, password_hash, role, status)
                    VALUES ('post_v6_missing_member', 'post_v6_missing_member@example.com', 'hash', 'MEMBER', 'ACTIVE')
                    """);
        }

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertDefaultMembershipAndProfile(statement, "post_v6_missing_member");
        }
    }

    @Test
    void latestMigrationRemovesLegacyModuleTablesButKeepsCommunityTables() throws Exception {
        JdbcDataSource dataSource = newDataSource();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(tableExists(connection, "users")).isTrue();
            assertThat(tableExists(connection, "circles")).isTrue();
            assertThat(tableExists(connection, "circle_members")).isTrue();
            assertThat(tableExists(connection, "user_profiles")).isTrue();
            assertThat(tableExists(connection, "community_topics")).isTrue();
            assertThat(tableExists(connection, "community_posts")).isTrue();
            assertThat(tableExists(connection, "community_comments")).isTrue();
            assertThat(tableExists(connection, "community_reactions")).isTrue();
            assertThat(tableExists(connection, "community_moderation_actions")).isTrue();

            assertThat(tableExists(connection, "portfolio_projects")).isFalse();
            assertThat(tableExists(connection, "github_repositories")).isFalse();
            assertThat(tableExists(connection, "project_tech_stacks")).isFalse();
            assertThat(tableExists(connection, "project_profiles")).isFalse();
            assertThat(tableExists(connection, "habit_records")).isFalse();
            assertThat(tableExists(connection, "habits")).isFalse();
            assertThat(tableExists(connection, "journals")).isFalse();
            assertThat(tableExists(connection, "daily_plans")).isFalse();
            assertThat(tableExists(connection, "note_blocks")).isFalse();
            assertThat(tableExists(connection, "notes")).isFalse();
            assertThat(tableExists(connection, "task_tags")).isFalse();
            assertThat(tableExists(connection, "tags")).isFalse();
            assertThat(tableExists(connection, "tasks")).isFalse();
            assertThat(tableExists(connection, "projects")).isFalse();
        }
    }

    private JdbcDataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:community_migration_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void assertDefaultMembershipAndProfile(Statement statement, String username) throws Exception {
        ResultSet membership = statement.executeQuery("""
                SELECT cm.role, cm.status
                FROM circle_members cm
                JOIN circles c ON c.id = cm.circle_id
                JOIN users u ON u.id = cm.user_id
                WHERE c.slug = 'ruru-community'
                  AND u.username = '%s'
                """.formatted(username));
        assertThat(membership.next()).isTrue();
        assertThat(membership.getString("role")).isEqualTo("MEMBER");
        assertThat(membership.getString("status")).isEqualTo("ACTIVE");

        ResultSet profile = statement.executeQuery("""
                SELECT up.display_name
                FROM user_profiles up
                JOIN users u ON u.id = up.user_id
                WHERE u.username = '%s'
                """.formatted(username));
        assertThat(profile.next()).isTrue();
        assertThat(profile.getString("display_name")).isEqualTo(username);
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
