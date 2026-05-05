package com.homestay3.homestaybackend.model.notification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNotificationBoundaryTest {

    private static final String SERVICE_IMPL_DIR =
            "src/main/java/com/homestay3/homestaybackend/service/impl";

    private static final List<String> ORDER_NOTIFICATION_SOURCES = List.of(
            "OrderLifecycleServiceImpl.java",
            "OrderNotificationServiceImpl.java",
            "OrderTimeoutService.java",
            "CheckInServiceImpl.java",
            "CheckOutServiceImpl.java",
            "CheckInReminderService.java");

    @Test
    void orderLifecycleServicesDoNotDependOnRawNotificationPersistenceEnums() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String sourceFile : ORDER_NOTIFICATION_SOURCES) {
            String source = Files.readString(sourcePath(sourceFile));
            if (source.contains("import com.homestay3.homestaybackend.model.enums.NotificationType;")) {
                violations.add(sourceFile + " imports NotificationType directly");
            }
            if (source.contains("import com.homestay3.homestaybackend.model.enums.EntityType;")) {
                violations.add(sourceFile + " imports EntityType directly");
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void orderLifecycleServicesCreateNotificationsThroughCommandObject() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String sourceFile : ORDER_NOTIFICATION_SOURCES) {
            List<String> lines = Files.readAllLines(sourcePath(sourceFile));
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (!line.contains("notificationService.createNotification(")) {
                    continue;
                }
                if (usesNotificationCreateCommand(lines, index)) {
                    continue;
                }
                violations.add(sourceFile + ":" + (index + 1) + " calls createNotification without NotificationCreateCommand");
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    private static boolean usesNotificationCreateCommand(List<String> lines, int startIndex) {
        int endIndex = Math.min(lines.size(), startIndex + 4);
        for (int index = startIndex; index < endIndex; index++) {
            if (lines.get(index).contains("NotificationCreateCommand.")) {
                return true;
            }
        }
        return false;
    }

    private static Path sourcePath(String sourceFile) {
        Path backendRelative = Path.of(SERVICE_IMPL_DIR, sourceFile);
        if (Files.exists(backendRelative)) {
            return backendRelative;
        }
        return Path.of("homestay-backend").resolve(backendRelative);
    }
}
