package com.stocktrader.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    public static DataSource dataSource() {
        Properties props = loadProperties();
        HikariConfig config = new HikariConfig();
        // Env vars take precedence over application.properties
        config.setJdbcUrl(env("DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/stocktrader")));
        config.setUsername(env("DB_USER", props.getProperty("db.user", "postgres")));
        config.setPassword(env("DB_PASSWORD", props.getProperty("db.password", "")));
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("Could not load application.properties, using defaults");
        }
        return props;
    }
}
