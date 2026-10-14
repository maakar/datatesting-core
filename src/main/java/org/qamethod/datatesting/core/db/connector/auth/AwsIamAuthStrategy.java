package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * AWS IAM authentication for RDS databases.
 * <p>
 * Generates a short-lived IAM authentication token using the AWS SDK,
 * which is used as the password for the JDBC connection.
 * <p>
 * Requires {@code software.amazon.awssdk:rds} and {@code software.amazon.awssdk:auth}
 * on the classpath at runtime.
 */
public class AwsIamAuthStrategy implements AuthStrategy {

    private final String awsRegion;
    private final String host;
    private final int port;
    private final String username;

    public AwsIamAuthStrategy(String awsRegion, String host, int port, String username) {
        this.awsRegion = awsRegion;
        this.host = host;
        this.port = port;
        this.username = username;
    }

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        properties.setProperty("user", username);

        try {
            // Use reflection to avoid compile-time dependency on AWS SDK
            Class<?> rdsUtilitiesClass = Class.forName("software.amazon.awssdk.services.rds.RdsUtilities");
            Class<?> regionClass = Class.forName("software.amazon.awssdk.regions.Region");
            Class<?> tokenRequestClass = Class.forName(
                    "software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest");

            // Build the token request
            Object region = regionClass.getMethod("of", String.class).invoke(null, awsRegion);

            Object rdsUtilities = rdsUtilitiesClass.getMethod("builder").invoke(null);
            rdsUtilities = rdsUtilities.getClass().getMethod("region", regionClass).invoke(rdsUtilities, region);
            rdsUtilities = rdsUtilities.getClass().getMethod("build").invoke(rdsUtilities);

            Object tokenRequestBuilder = tokenRequestClass.getMethod("builder").invoke(null);
            tokenRequestBuilder = tokenRequestBuilder.getClass()
                    .getMethod("hostname", String.class).invoke(tokenRequestBuilder, host);
            tokenRequestBuilder = tokenRequestBuilder.getClass()
                    .getMethod("port", int.class).invoke(tokenRequestBuilder, port);
            tokenRequestBuilder = tokenRequestBuilder.getClass()
                    .getMethod("userName", String.class).invoke(tokenRequestBuilder, username);
            Object tokenRequest = tokenRequestBuilder.getClass().getMethod("build").invoke(tokenRequestBuilder);

            String authToken = (String) rdsUtilities.getClass()
                    .getMethod("generateAuthenticationToken", tokenRequestClass)
                    .invoke(rdsUtilities, tokenRequest);

            properties.setProperty("password", authToken);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "AWS SDK not on classpath. Add 'software.amazon.awssdk:rds' dependency to use IAM auth.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AWS IAM auth token: " + e.getMessage(), e);
        }

        return jdbcUrl;
    }

    @Override
    public String description() {
        return "AWS IAM (region=" + awsRegion + ", user=" + username + ")";
    }
}

