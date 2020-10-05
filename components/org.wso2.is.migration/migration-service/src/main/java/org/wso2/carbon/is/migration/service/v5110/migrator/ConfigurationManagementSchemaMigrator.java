package org.wso2.carbon.is.migration.service.v5110.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.service.SchemaMigrator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ConfigurationManagementSchemaMigrator.
 */
public class ConfigurationManagementSchemaMigrator extends SchemaMigrator {

    private static Logger log = LoggerFactory.getLogger(ConfigurationManagementSchemaMigrator.class);
    private static final String DB_CHECK_SQL = "SELECT 1 FROM IDN_CONFIG_TYPE";

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        if (!isConfigurationManagementDatabaseStructureCreated()) {
            log.info("Configuration Management tables does not exist in the database. Hence creating. ");
            super.migrate();
        } else {
            log.info("Configuration Management tables already exist in the database. Hence skipping.");
        }

    }

    /**
     * Checks whether Configuration Management database tables are created.
     *
     * @return <code>true</code> if Configuration Management database tables exist, else <code>false</code>.
     */
    private boolean isConfigurationManagementDatabaseStructureCreated() throws MigrationClientException {

        try (Connection conn = getDataSource().getConnection();
             Statement statement = conn.createStatement()) {
            log.info("Running a query to test the database configuration management tables existence. ");
            // check whether the tables are already created with a query.
            try (ResultSet ignored = statement.executeQuery(DB_CHECK_SQL)) {
                log.info("Configuration Management tables already exists.");
            }
        } catch (SQLException e) {
            return false;
        }
        return true;
    }
}
