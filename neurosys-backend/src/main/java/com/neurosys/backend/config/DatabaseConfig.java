package com.neurosys.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("[DATABASE CONFIG] Active Spring Profile: {}", activeProfile);

        String rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getenv("MYSQL_PUBLIC_URL");
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getenv("MYSQL_URL");
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getenv("MYSQLPRIVATEURL");
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getenv("DATABASE_URL");
        }

        String host = System.getenv("MYSQLHOST");
        if (host == null || host.trim().isEmpty()) {
            host = System.getenv("MYSQL_HOST");
        }
        String port = System.getenv("MYSQLPORT");
        if (port == null || port.trim().isEmpty()) {
            port = System.getenv("MYSQL_PORT");
        }
        String db = System.getenv("MYSQLDATABASE");
        if (db == null || db.trim().isEmpty()) {
            db = System.getenv("MYSQL_DATABASE");
        }
        String user = System.getenv("MYSQLUSER");
        if (user == null || user.trim().isEmpty()) {
            user = System.getenv("MYSQL_USER");
        }
        String pass = System.getenv("MYSQLPASSWORD");
        if (pass == null || pass.trim().isEmpty()) {
            pass = System.getenv("MYSQL_PASSWORD");
        }

        HikariConfig config = new HikariConfig();
        boolean configured = false;

        // 1. Direct JDBC URL
        if (rawUrl != null && rawUrl.startsWith("jdbc:mysql://")) {
            log.info("[DATABASE CONFIG] Configuring Hikari DataSource with direct JDBC URL scheme");
            config.setJdbcUrl(rawUrl);
            if (user != null) config.setUsername(user);
            if (pass != null) config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            configured = true;
        }

        // 2. Parse Raw mysql:// URI from Railway
        if (!configured && rawUrl != null && rawUrl.startsWith("mysql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                String username = user != null ? user : "root";
                String password = pass != null ? pass : "";
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }
                String hostName = uri.getHost();
                int portNum = uri.getPort() > 0 ? uri.getPort() : 3306;
                String dbName = uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "railway";

                String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        hostName, portNum, dbName);
                log.info("[DATABASE CONFIG] Configured Railway MySQL DataSource (Host: {}, Port: {}, DB: {})", hostName, portNum, dbName);
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                configured = true;
            } catch (Exception e) {
                log.error("Failed to parse raw MYSQL_URL: {}, falling back to host params", rawUrl, e);
            }
        }

        // 3. Railway Host Parameters
        if (!configured && host != null && !host.trim().isEmpty()) {
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port != null ? port : "3306", db != null ? db : "railway");
            log.info("[DATABASE CONFIG] Configured Hikari DataSource with Host Params (Host: {}, DB: {})", host, db != null ? db : "railway");
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user != null ? user : "root");
            config.setPassword(pass != null ? pass : "");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            configured = true;
        }

        // 4. Default Environment Handling
        if (!configured) {
            if ("prod".equalsIgnoreCase(activeProfile)) {
                String railwayHost = "mysql.railway.internal";
                String railwayPort = port != null ? port : "3306";
                String railwayDb = db != null ? db : "railway";
                String railwayUser = user != null ? user : "root";
                String railwayPass = pass != null ? pass : "";

                String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        railwayHost, railwayPort, railwayDb);
                log.info("[DATABASE CONFIG] Prod Profile: Target Railway MySQL Internal Networking (Host: {}, DB: {})", railwayHost, railwayDb);
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(railwayUser);
                config.setPassword(railwayPass);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else {
                String localUrl = "jdbc:mysql://localhost:3306/railway?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                log.info("[DATABASE CONFIG] Dev Profile: Configuring local MySQL DataSource (jdbc:mysql://localhost:3306/railway)");
                config.setJdbcUrl(localUrl);
                config.setUsername(user != null ? user : "root");
                config.setPassword(pass != null ? pass : "Karthik@2005");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        }

        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(10000);
        return new HikariDataSource(config);
    }
}
