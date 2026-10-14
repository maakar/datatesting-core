package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * Username + password authentication.
 * The simplest and most universal auth mechanism.
 */
public class UsernamePasswordAuthStrategy implements AuthStrategy {

    private final String username;
    private final String password;

    public UsernamePasswordAuthStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        if (username != null) properties.setProperty("user", username);
        if (password != null) properties.setProperty("password", password);
        return jdbcUrl;
    }

    @Override
    public String description() {
        return "Username/Password (user=" + username + ")";
    }
}

