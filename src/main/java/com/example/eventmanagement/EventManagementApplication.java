package com.example.eventmanagement;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EventManagementApplication {
    public static final String SPRING_DATASOURCE_URL = "spring.datasource.url";

    public static void main(String[] args) {
        configureDatabaseUrl();
        SpringApplication.run(EventManagementApplication.class, args);
    }

    private static void configureDatabaseUrl() {
        if (hasText(System.getProperty(SPRING_DATASOURCE_URL))
                || hasText(System.getenv("SPRING_DATASOURCE_URL"))) {
            return;
        }

        String databaseUrl = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("JDBC_DATABASE_URL"));
        if (!hasText(databaseUrl)) {
            return;
        }

        if (databaseUrl.startsWith("jdbc:")) {
            System.setProperty("SPRING_DATASOURCE_URL", databaseUrl);
            return;
        }

        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgresql".equalsIgnoreCase(scheme) && !"postgres".equalsIgnoreCase(scheme)) {
            return;
        }

        String host = uri.getHost();
        if (!hasText(host)) {
            throw new IllegalStateException("DATABASE_URL must contain a PostgreSQL host.");
        }

        String path = hasText(uri.getPath()) ? uri.getPath() : "";
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String query = hasText(uri.getRawQuery()) ? "?" + uri.getRawQuery() : "";

        System.setProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + host + port + path + query);
        configureDatabaseCredentials(uri.getRawUserInfo());
    }

    private static void configureDatabaseCredentials(String rawUserInfo) {
        if (!hasText(rawUserInfo)) {
            return;
        }

        int separator = rawUserInfo.indexOf(':');
        String username = separator >= 0 ? rawUserInfo.substring(0, separator) : rawUserInfo;
        String password = separator >= 0 ? rawUserInfo.substring(separator + 1) : "";

        if (!hasText(System.getProperty("spring.datasource.username"))
                && !hasText(System.getenv("SPRING_DATASOURCE_USERNAME"))) {
            System.setProperty("spring.datasource.username", decode(username));
        }

        if (!hasText(System.getProperty("spring.datasource.password"))
                && !hasText(System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
            System.setProperty("spring.datasource.password", decode(password));
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
