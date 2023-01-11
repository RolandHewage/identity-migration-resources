/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.is.migration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.internal.ISMigrationServiceDataHolder;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.Utility;
import org.wso2.carbon.utils.dbcreator.DatabaseCreator;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import static org.wso2.carbon.is.migration.util.Constant.IDENTITY_DB_SCRIPT;
import static org.wso2.carbon.is.migration.util.Constant.UM_DB_SCRIPT;

/**
 * Migrator implementation for Schema migration.
 */
public class SchemaMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private String location;

    private Connection conn = null;
    private Statement statement;
    private String delimiter = ";";

    @Override
    public void migrate() throws MigrationClientException {

        this.location = getMigratorConfig().getParameterValue(Constant.LOCATION);
        try {
            conn = getDataSource().getConnection();
            String connURL = conn.getMetaData().getURL();
            migrateWithDBConnection();
            /* To support migration of registry and primary JDBC userstores separate, i.e., the migration client
            should provide configurations for a Registry datasource and Primary JDBC userstore datasources and handle
            their migrations separately. */
            if (isSeparateRegistryDB() && Objects.equals(getSchema(), "um")) {
                Map<String, DataSource> regDatasources = getRegistryDataSources();
                for (Map.Entry<String, DataSource> dataSource : regDatasources.entrySet()) {
                    // Updating the 'conn' object for migration of registry data sources.
                    conn = dataSource.getValue().getConnection();
                    if (!Objects.equals(connURL, conn.getMetaData().getURL())) {
                        // This is to skip the duplicate data-source migration already done for um data-source.
                        if(!dataSource.getKey().equals("jdbc/WSO2CarbonDB")) {
                            /* The new embedded H2 DB in the target migration pack can be used without migrating.
                            In case an external DB has been configured for WSO2CarbonDB, the DB needs to be recreated
                            with the new DB scripts available in the target IS pack. */
                            migrateWithDBConnection();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new MigrationClientException(e.getMessage(), e);
        }
    }

    private void migrateWithDBConnection() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Executing Identity Migration Scripts.");
        try {
            conn.setAutoCommit(false);
            String databaseType = DatabaseCreator.getDatabaseType(this.conn);
            if ("mysql".equals(databaseType)) {
                Utility.setMySQLDBName(conn);
                if (Double.compare(getDatabaseProductVersion(), 5.7) >= 0) {
                    if (isMySQLCluster()) {
                        if (Utility.isDBScriptExists(getSchema(), Constant.MYSQL_CLUSTER_5_7, location,
                                getVersionConfig().getVersion())) {
                            databaseType = Constant.MYSQL_CLUSTER_5_7;
                        } else {
                            databaseType = Constant.MYSQL_CLUSTER;
                        }
                    } else if (Utility.isDBScriptExists(getSchema(), Constant.MYSQL_5_7, location,
                            getVersionConfig().getVersion())) {
                        log.info("MySQL version is higher than 5.7. Executing 5.7 script.");
                        databaseType = Constant.MYSQL_5_7;
                    }
                } else {
                    log.info("MySQL version is lower than 5.7. Executing 5.6 script.");
                }
            }
            statement = conn.createStatement();

            String dbscriptName = Utility.getSchemaPath(getSchema(), databaseType, location, getVersionConfig()
                    .getVersion());
            executeSQLScript(dbscriptName);
            conn.commit();
            log.info(Constant.MIGRATION_LOG + "Identity DB Migration script executed successfully.");
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                log.error("An error occurred while rolling back transactions. ", e1);
            }
            log.error("An error occurred while rolling back transactions. ", e);
            if (!isContinueOnError()) {
                throw new MigrationClientException(e.getMessage(), e);
            }
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                log.error("Failed to close database statement.", e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Failed to close database connection.", e);
            }
        }
    }

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    private Double getDatabaseProductVersion() throws SQLException, MigrationClientException {

        String databaseProductVersion = this.conn.getMetaData().getDatabaseProductVersion();
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        Number number = formatter.parse(databaseProductVersion, pos);
        if (number != null) {
            log.info("Found database product version: " + number.doubleValue());
            return number.doubleValue();
        }
        throw new MigrationClientException("Error while parsing database version: " + databaseProductVersion);
    }

    private boolean isMySQLCluster() throws SQLException {

        boolean isCluster = false;
        String databaseProductVersion = this.conn.getMetaData().getDatabaseProductVersion();
        if (databaseProductVersion.contains("cluster")) {
            isCluster = true;
        }
        return isCluster;
    }

    /**
     * executes content in SQL script.
     *
     * @return StringBuffer
     * @throws Exception
     */
    private void executeSQLScript(String dbscriptName) throws Exception {

        String databaseType = DatabaseCreator.getDatabaseType(this.conn);
        boolean keepFormat = false;
        boolean isProcedure = false;

        if ("oracle".equals(databaseType)) {
            delimiter = "/";
        } else if ("db2".equals(databaseType)) {
            delimiter = "/";
        } else if ("openedge".equals(databaseType)) {
            delimiter = "/";
            keepFormat = true;
        }

        StringBuffer sql = new StringBuffer();
        BufferedReader reader = null;

        try {
            File dbScript = new File(dbscriptName);
            if (!dbScript.exists()) {
                log.info("DB script: " + dbscriptName + " is not available for this migration");
                return;
            }
            InputStream is = new FileInputStream(dbScript);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(Constant.DELIMITER)) {
                    delimiter = line.split(" ")[1];
                    continue;
                }
                if (line.contains("Start of Procedure")) {
                    isProcedure = true;
                    continue;
                }
                if (line.contains("End of Procedure")) {
                    executeSQL(sql.substring(0, sql.length() - delimiter.length()));
                    sql.replace(0, sql.length(), "");
                    isProcedure = false;
                    continue;
                }
                if (!keepFormat) {
                    if (line.startsWith("//")) {
                        continue;
                    }
                    if (line.startsWith("--")) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(line);
                    if (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("REM".equalsIgnoreCase(token)) {
                            continue;
                        }
                    }
                }
                if (isProcedure) {
                    sql.append(" ").append(line);
                    continue;
                }
                //add the oracle database owner
                if ("oracle".equals(databaseType) && line.contains("databasename :=")) {
                    if (dbscriptName.contains(IDENTITY_DB_SCRIPT)) {
                        line = "databasename := '" + ISMigrationServiceDataHolder.getIdentityOracleUser() + "';";
                    } else if (dbscriptName.contains(UM_DB_SCRIPT)) {
                        line = "databasename := '" + ISMigrationServiceDataHolder.getUmOracleUser() + "';";
                    }
                }
                sql.append(keepFormat ? "\n" : " ").append(line);

                // SQL defines "--" as a comment to EOL
                // and in Oracle it may contain a hint
                // so we cannot just remove it, instead we must end it
                if (!keepFormat && line.indexOf("--") >= 0) {
                    sql.append("\n");
                }
                if ((DatabaseCreator.checkStringBufferEndsWith(sql, delimiter))) {
                    executeSQL(sql.substring(0, sql.length() - delimiter.length()));
                    sql.replace(0, sql.length(), "");
                }
            }
            // Catch any statements not followed by ;
            if (sql.length() > 0) {
                executeSQL(sql.toString());
            }
        } catch (Exception e) {
            log.error("Error occurred while executing SQL script for migrating database", e);
            throw new Exception("Error occurred while executing SQL script for migrating database", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * executes given sql.
     *
     * @param sql
     * @throws Exception
     */
    private void executeSQL(String sql) throws Exception {

        // Check and ignore empty statements
        if ("".equals(sql.trim())) {
            return;
        }

        ResultSet resultSet = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("SQL : " + sql);
            }

            boolean ret;
            int updateCount = 0, updateCountTotal = 0;
            ret = statement.execute(sql);
            updateCount = statement.getUpdateCount();
            resultSet = statement.getResultSet();
            do {
                if (!ret && updateCount != -1) {
                    updateCountTotal += updateCount;
                }
                ret = statement.getMoreResults();
                if (ret) {
                    updateCount = statement.getUpdateCount();
                    resultSet = statement.getResultSet();
                }
            } while (ret);

            if (log.isDebugEnabled()) {
                log.debug(sql + " : " + updateCountTotal + " rows affected");
            }
            SQLWarning warning = conn.getWarnings();
            while (warning != null) {
                log.debug(warning + " sql warning");
                warning = warning.getNextWarning();
            }
            conn.clearWarnings();
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains(":")) {
                String[] errorCodes = errorMsg.split(":");
                if (errorCodes.length > 0) {
                    String errorCode = errorCodes[0];
                    if ("ORA-00955".equals(errorCode)) {
                        // Eliminating the table already exists exception for oracle and DB2 database .
                        // errorMsg for this case will be "ORA-00955: name is already used by an existing object"
                        if (log.isDebugEnabled()) {
                            log.debug("Table Already Exists. Hence, skipping table creation");
                        }
                        return;
                    }
                }
            }
            if (e.getSQLState().equals("X0Y32") || e.getSQLState().equals("42710")) {
                // Eliminating the table already exception for the derby and DB2 database types
                if (log.isDebugEnabled()) {
                    log.debug("Table Already Exists. Hence, skipping table creation");
                }
            } else {
                throw new Exception("Error occurred while executing : " + sql, e);
            }
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.error("Error occurred while closing result set.", e);
                }
            }
        }
    }

}
