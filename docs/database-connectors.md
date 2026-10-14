# Database Connectors

Use connectors when loading data from live databases instead of CSV snapshots.

## YAML Configuration

```yaml
connections:
  analyticsDb:
    provider: azure-sql          # azure-sql | gcp-cloudsql | aws-rds | generic
    host: myserver.database.windows.net
    port: 1433
    database: MainDB
    label: "Primary DB"
    auth:
      type: spn                  # password | windows | spn | managed-identity | keyvault | iam
      tenantId: "..."
      clientId: "..."
      clientSecret: "..."
    options:
      encrypt: "true"
      loginTimeout: "30"
```

## Providers

| Provider | Aliases | Default port | JDBC prefix |
|---|---|---|---|
| `azure-sql` | `azure`, `mssql` | 1433 | `jdbc:sqlserver://` |
| `gcp-cloudsql` | `gcp`, `cloudsql` | 5432 | `jdbc:postgresql://` |
| `aws-rds` | `aws`, `rds` | 3306 | `jdbc:mysql://` |
| `generic` | — | — | custom via `options.jdbcUrl` |

`gcp-cloudsql` and `aws-rds` also support `options.dbType` = `postgresql`, `mysql`, or `sqlserver`.

## Authentication Types

| Type | Required fields | Description |
|---|---|---|
| `password` | `username`, `password` | Standard DB login |
| `windows` | none | Windows Integrated / Kerberos |
| `spn` | `tenantId`, `clientId`, `clientSecret` | Azure AD Service Principal |
| `managed-identity` | none | Azure Managed Identity |
| `keyvault` | `keyvault.vaultUrl`, `usernameSecret`, `passwordSecret` | Resolve secrets from Azure Key Vault |
| `iam` | `username`, `awsRegion` | AWS IAM auth token |

## Usage

```java
Map<String, ConnectionConfig> configs = ...;
ConnectorRegistry registry = ConnectorRegistry.fromConfigs(configs);

Map<String, Boolean> health = registry.testAll();

try (Connection conn = registry.get("analyticsDb").getConnection()) {
    PreparedStatement ps = conn.prepareStatement("SELECT * FROM records");
    ResultSet rs = ps.executeQuery();
}

DatabaseDataSource ds = new DatabaseDataSource(
    registry.get("analyticsDb"),
    "SELECT * FROM reference_values",
    "ReferenceData");

registry.close();
```

## Runtime Dependencies for Auth/Drivers

These dependencies are optional and only needed for the auth/database path you use.

| Use case | Dependency |
|---|---|
| SQL Server + `spn`/`managed-identity` | `com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre17` |
| Azure Key Vault auth | `com.azure:azure-identity:1.11.0` + `com.azure:azure-security-keyvault-secrets:4.7.0` |
| AWS IAM auth | `software.amazon.awssdk:rds:2.24.0` + `software.amazon.awssdk:auth:2.24.0` |
| GCP PostgreSQL | `org.postgresql:postgresql:42.7.1` |
| MySQL (GCP/AWS) | `com.mysql:mysql-connector-j:8.3.0` |

