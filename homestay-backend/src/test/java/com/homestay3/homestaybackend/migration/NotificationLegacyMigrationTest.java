package com.homestay3.homestaybackend.migration;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationLegacyMigrationTest {

    @Test
    void normalizesLegacyNotificationValues() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:notification_legacy_migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE notifications (
                            id BIGINT PRIMARY KEY,
                            type VARCHAR(50) NOT NULL,
                            entity_type VARCHAR(50),
                            updated_at TIMESTAMP
                        )
                        """);
                statement.execute("""
                        INSERT INTO notifications (id, type, entity_type, updated_at) VALUES
                        (1, 'PAID', 'ORDER', CURRENT_TIMESTAMP),
                        (2, 'COMPLETED', 'ORDER', CURRENT_TIMESTAMP),
                        (3, 'CONFIRMED', 'MESSAGE', CURRENT_TIMESTAMP),
                        (4, 'REFUNDED', 'MESSAGE', CURRENT_TIMESTAMP),
                        (5, 'CANCELLED_BY_HOST', 'MESSAGE', CURRENT_TIMESTAMP),
                        (6, 'CANCELLED_BY_USER', 'MESSAGE', CURRENT_TIMESTAMP),
                        (7, 'CANCELLED', 'MESSAGE', CURRENT_TIMESTAMP),
                        (8, 'PENDING', 'MESSAGE', CURRENT_TIMESTAMP),
                        (9, 'NEW_MESSAGE', 'MESSAGE', CURRENT_TIMESTAMP),
                        (10, 'UNKNOWN', 'UNKNOWN', CURRENT_TIMESTAMP),
                        (11, 'ORDER_CONFIRMED', 'ORDER', CURRENT_TIMESTAMP)
                        """);
            }

            Path migration = Path.of("src/main/resources/db/migration/V45__normalize_legacy_notification_values.sql");
            RunScript.execute(connection, Files.newBufferedReader(migration));

            assertEquals("PAYMENT_RECEIVED", typeById(connection, 1));
            assertEquals("ORDER_COMPLETED", typeById(connection, 2));
            assertEquals("ORDER_CONFIRMED", typeById(connection, 3));
            assertEquals("REFUND_COMPLETED", typeById(connection, 4));
            assertEquals("ORDER_CANCELLED_BY_HOST", typeById(connection, 5));
            assertEquals("ORDER_CANCELLED_BY_GUEST", typeById(connection, 6));
            assertEquals("BOOKING_CANCELLED", typeById(connection, 7));
            assertEquals("ORDER_STATUS_CHANGED", typeById(connection, 8));
            assertEquals("MESSAGE_THREAD", entityTypeById(connection, 9));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM notifications WHERE type IN ('PAID', 'COMPLETED', 'CONFIRMED', 'REFUNDED', 'CANCELLED_BY_HOST', 'CANCELLED_BY_USER', 'CANCELLED', 'PENDING')"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM notifications WHERE entity_type = 'MESSAGE'"));
        }
    }

    private String typeById(Connection connection, long id) throws Exception {
        return stringValue(connection, "SELECT type FROM notifications WHERE id = " + id);
    }

    private String entityTypeById(Connection connection, long id) throws Exception {
        return stringValue(connection, "SELECT entity_type FROM notifications WHERE id = " + id);
    }

    private String stringValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
