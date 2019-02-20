/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.is.data.sync.system.database;

import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.util.Constant;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_DB2;
import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_H2;
import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_MSSQL;
import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_MYSQL;
import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_ORACLE;
import static org.wso2.is.data.sync.system.util.Constant.DATA_SOURCE_TYPE_POSGRESQL;
import static org.wso2.is.data.sync.system.util.Constant.SQL_DELIMITER_DB2_ORACLE;
import static org.wso2.is.data.sync.system.util.Constant.SQL_DELIMITER_H2_MYSQL_MSSQL_POSGRES;

public class DataSourceManager {

    private Map<String, DataSourceEntry> dataSourceEntryListSource = new HashMap<>();
    private Map<String, DataSourceEntry> dataSourceEntryListTarget = new HashMap<>();

    public DataSourceManager() throws SyncClientException {

        Map<String, String> sourceMapping = new HashMap<>();
        sourceMapping.put(Constant.SCHEMA_TYPE_IDENTITY, "jdbc/WSO2CarbonDBSource");

        Map<String, String> targetMapping = new HashMap<>();
        targetMapping.put(Constant.SCHEMA_TYPE_IDENTITY, "jdbc/WSO2CarbonDB");

        populateDataSourceEntryList(sourceMapping, dataSourceEntryListSource);
        populateDataSourceEntryList(targetMapping, dataSourceEntryListTarget);

    }

    private void populateDataSourceEntryList(Map<String, String> dataSourceMapping, Map<String,
            DataSourceEntry> dataSourceEntryList) throws SyncClientException {

        for (Map.Entry<String, String> entry : dataSourceMapping.entrySet()) {

            String schema = entry.getKey();
            String dataSourceName = entry.getValue();

            try {
                InitialContext ctx = new InitialContext();
                DataSource dataSource = (DataSource) ctx.lookup(dataSourceName);

                try {
                    String type = getDataSourceType(dataSource);
                    dataSourceEntryList.put(schema, new DataSourceEntry(dataSource, type));
                } catch (SQLException e) {
                    throw new SyncClientException("Error while creating connection with data source: " + dataSourceName +
                                                  " of schema: " + schema);
                }
            } catch (NamingException e) {
                throw new SyncClientException("Error while data source lookup for: " + dataSourceName + " of schema: "
                                           + schema);
            }
        }
    }

    private String getDataSourceType(DataSource dataSource) throws SQLException, SyncClientException {

        String type;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_MYSQL + ".*")) {
                type = "mysql";
            } else if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_ORACLE + ".*")) {
                type = "oracle";
            } else if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_MSSQL + ".*")) {
                type = "mssql";
            } else if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_H2 + ".*")) {
                type = "h2";
            } else if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_DB2 + ".*")) {
                type = "db2";
            } else if (databaseProductName.matches("(?i).*" + DATA_SOURCE_TYPE_POSGRESQL + ".*")) {
                type = "postgresql";
            } else {
                throw new SyncClientException("Unsupported data source type: " + databaseProductName);
            }
        }
        return type;
    }

    public String getDataSourceType(String schema) {

        return DATA_SOURCE_TYPE_MYSQL;
        //return dataSourceEntryListSource.get(schema).getType();
    }

    public DataSource getSourceDataSource(String schema) {

        return dataSourceEntryListSource.get(schema).getDataSource();
    }

    public DataSource getTargetDataSource(String schema) {

        return dataSourceEntryListTarget.get(schema).getDataSource();
    }

    public Connection getSourceConnection(String schema) throws SyncClientException {

        DataSource dataSource;
        Connection connection;
        try {
            dataSource = dataSourceEntryListSource.get(schema).getDataSource();
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new SyncClientException("Error occurred while creating connection for source schema: " + schema);
        }
        return connection;
    }

    public Connection getTargetConnection(String schema) throws SyncClientException {

        DataSource dataSource;
        Connection connection;
        try {
            dataSource = dataSourceEntryListTarget.get(schema).getDataSource();
            connection = dataSource.getConnection();

        } catch (SQLException e) {
            throw new SyncClientException("Error occurred while creating connection for target schema: " + schema);
        }
        return connection;
    }

    public String getSqlDelimiter(String schema) {

        String dataSourceType = getDataSourceType(schema);
        if ("oracle".equals(dataSourceType) || "db2".equals(dataSourceType)) {
            return SQL_DELIMITER_DB2_ORACLE;
        }
        return SQL_DELIMITER_H2_MYSQL_MSSQL_POSGRES;
    }

    public String getDDLPrefix(String schema) {

        return "DELIMITER //" + System.lineSeparator() + System.lineSeparator();
    }

    public String getDDLSuffix(String schema) {

        return System.lineSeparator() + System.lineSeparator() + "DELIMITER ;";
    }

    public String getSchema(String tableName) {

        return Constant.SCHEMA_TYPE_IDENTITY;
    }
}
