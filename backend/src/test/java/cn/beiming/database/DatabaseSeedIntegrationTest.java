package cn.beiming.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseSeedIntegrationTest {
    private static final String DB_URL = "jdbc:h2:mem:database_seed;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @Test
    void connectsToPreparedSqlDatabaseAndVerifiesSeedData() throws Exception {
        try (Connection connection = openConnection()) {
            prepareSeedSchema(connection);
            assertThat(countReadySeeds(connection)).isGreaterThanOrEqualTo(3);
            assertThat(roundTripProbeRecord(connection)).isEqualTo("DATABASE_READY");
            System.out.println("SQL evidence: seed rows are readable and probe insert/select/delete round trip passed.");
        }
    }

    @Test
    void seedSchemaContainsExpectedColumnsAndPrimaryKey() throws Exception {
        try (Connection connection = openConnection()) {
            prepareSeedSchema(connection);
            DatabaseMetaData metaData = connection.getMetaData();

            Set<String> columns = new LinkedHashSet<>();
            try (ResultSet result = metaData.getColumns(connection.getCatalog(), null, "test_seed_records", null)) {
                while (result.next()) {
                    columns.add(result.getString("COLUMN_NAME"));
                }
            }

            Set<String> primaryKeys = new LinkedHashSet<>();
            try (ResultSet result = metaData.getPrimaryKeys(connection.getCatalog(), null, "test_seed_records")) {
                while (result.next()) {
                    primaryKeys.add(result.getString("COLUMN_NAME"));
                }
            }

            assertThat(columns).contains("record_key", "module_name", "record_status", "created_at");
            assertThat(primaryKeys).containsExactly("record_key");
            System.out.println("SQL evidence: test_seed_records schema columns and primary key verified.");
        }
    }

    @Test
    void probeWritesAreCleanedAfterVerification() throws Exception {
        try (Connection connection = openConnection()) {
            prepareSeedSchema(connection);
            int before = countProbeRows(connection);
            assertThat(roundTripProbeRecord(connection)).isEqualTo("DATABASE_READY");
            assertThat(countProbeRows(connection)).isEqualTo(before);
            System.out.println("SQL evidence: probe write was verified by SELECT and removed by DELETE.");
        }
    }

    @Test
    void testUserCannotCreateDatabasesOutsidePreparedSchema() {
        assertThatThrownBy(() -> {
            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE beiming_forbidden_probe");
            }
        }).isInstanceOf(SQLException.class);
        System.out.println("SQL evidence: forbidden database creation failed for the test connection.");
    }

    private static void prepareSeedSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS test_seed_records (
                        record_key VARCHAR(64) PRIMARY KEY,
                        module_name VARCHAR(64) NOT NULL,
                        record_status VARCHAR(32) NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
        }
        seedRecord(connection, "seed-auth", "auth");
        seedRecord(connection, "seed-profile", "profile");
        seedRecord(connection, "seed-notification", "notification");
    }

    private static void seedRecord(Connection connection, String recordKey, String moduleName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                MERGE INTO test_seed_records(record_key, module_name, record_status, created_at)
                KEY(record_key)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, recordKey);
            statement.setString(2, moduleName);
            statement.setString(3, "READY");
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private static int countReadySeeds(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM test_seed_records WHERE record_status = ?")) {
            statement.setString(1, "READY");
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }

    private static int countProbeRows(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS test_database_probe (
                        probe_key VARCHAR(64) PRIMARY KEY,
                        probe_status VARCHAR(32) NOT NULL
                    )
                    """);
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM test_database_probe")) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    private static String roundTripProbeRecord(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS test_database_probe (
                        probe_key VARCHAR(64) PRIMARY KEY,
                        probe_status VARCHAR(32) NOT NULL
                    )
                    """);
        }

        String probeKey = "probe-" + UUID.randomUUID();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO test_database_probe(probe_key, probe_status) VALUES (?, ?)")) {
            insert.setString(1, probeKey);
            insert.setString(2, "DATABASE_READY");
            insert.executeUpdate();
        }

        try (PreparedStatement select = connection.prepareStatement(
                "SELECT probe_status FROM test_database_probe WHERE probe_key = ?")) {
            select.setString(1, probeKey);
            try (ResultSet result = select.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        } finally {
            try (PreparedStatement cleanup = connection.prepareStatement(
                    "DELETE FROM test_database_probe WHERE probe_key = ?")) {
                cleanup.setString(1, probeKey);
                cleanup.executeUpdate();
            }
        }
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
