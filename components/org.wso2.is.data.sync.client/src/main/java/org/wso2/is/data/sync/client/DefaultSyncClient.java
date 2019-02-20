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

package org.wso2.is.data.sync.client;

import org.wso2.is.data.sync.client.config.SyncClientConfigManager;
import org.wso2.is.data.sync.client.datasource.ColumnData;
import org.wso2.is.data.sync.client.datasource.DataSourceManager;
import org.wso2.is.data.sync.client.datasource.SQLStatement;
import org.wso2.is.data.sync.client.datasource.TableMetaData;
import org.wso2.is.data.sync.client.exception.SyncClientException;
import org.wso2.is.data.sync.client.util.Constant;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_CREATE_TRIGGER_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_DROP_TABLE_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_DROP_TRIGGER_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_INSERT_SYNC_ID_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_SELECT_MAX_SYNC_ID_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_SELECT_SYNC_ID_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.getQuery;
import static org.wso2.is.data.sync.client.util.Constant.COLUMN_NAME_MAX_SYNC_ID;
import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_COLUMN_NAME;
import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_COLUMN_SIZE;
import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_TYPE_NAME;
import static org.wso2.is.data.sync.client.util.Constant.SQL_STATEMENT_TYPE_SOURCE;
import static org.wso2.is.data.sync.client.util.Constant.SQL_STATEMENT_TYPE_TARGET;
import static org.wso2.is.data.sync.client.util.Util.generateColumnList;
import static org.wso2.is.data.sync.client.util.Util.getInsertTriggerName;
import static org.wso2.is.data.sync.client.util.Util.getSyncTableName;
import static org.wso2.is.data.sync.client.util.Util.getSyncVersionTableName;
import static org.wso2.is.data.sync.client.util.Util.getUpdateTriggerName;

public class DefaultSyncClient implements SyncClient {

    protected SyncClientConfigManager configManager = new SyncClientConfigManager();

    @Override
    public String getSyncSourceVersion() {

        return configManager.getStartVersion();
    }

    @Override
    public String getSyncTargetVersion() {

        return configManager.getEndVersion();
    }

    @Override
    public void syncData(String tableName) throws SyncClientException {

        ExecutorService dataSyncExecutor = Executors.newSingleThreadExecutor();

        dataSyncExecutor.execute(() -> {

            Thread.currentThread().setName("sync-executor-" + tableName);

            try {

                String syncVersionTableName = getSyncVersionTableName(tableName);
                String syncTableName = getSyncTableName(tableName);

                TableMetaData tableMetaData = new TableMetaData.Builder().setColumnData(getColumnData(tableName))
                                                                         .setPrimaryKeys(getPrimaryKeys(tableName))
                                                                         .build();
                while (true) {
                    boolean canSleep;
                    int targetSyncId = getOrInsertDefaultTargetSyncId(tableName, syncVersionTableName);
                    int sourceMaxSyncId = getSourceMaxSyncId(tableName, syncTableName);

                    System.out.println("For table: " + tableName + " source max sync ID: " + sourceMaxSyncId + " " +
                                       "target sync ID: " + targetSyncId);

                    if (sourceMaxSyncId > targetSyncId) {

                        List<Map<String, Object>> results = getSyncDataList(tableName, syncTableName, tableMetaData,
                                                                            targetSyncId);
                        canSleep = syncToTarget(tableName, syncVersionTableName, tableMetaData, results);
                    } else {
                        System.out.println("No data to sync for: " + tableName);
                        canSleep = true;
                    }
                    if (canSleep) {
                        try {
                            Thread.sleep(configManager.getSyncIntervalInMillis());
                        } catch (InterruptedException e) {
                            throw new SyncClientException("Error while sleeping thread: " + Thread
                                    .currentThread().getName(), e);
                        }
                    }
                }
            } catch (SyncClientException e) {
                throw new RuntimeException("Error occurred while executing the sync task for table:" + tableName);
            }
        });
    }

    protected boolean syncToTarget(String tableName, String syncVersionTableName, TableMetaData metaData,
                                 List<Map<String, Object>> results) throws SyncClientException {

        boolean canSleep;
        try (Connection targetCon = DataSourceManager.getTargetConnection(getSchema(tableName))) {

            String sqlUpdate = getTargetUpdateQuery(tableName, metaData);
            String sqlInsert = getTargetInsertQuery(tableName, metaData);

            targetCon.setAutoCommit(false);
            try (PreparedStatement psUpdate = targetCon.prepareStatement(sqlUpdate);
                 PreparedStatement psInsert = targetCon.prepareStatement(sqlInsert)) {

                int lastSyncId = -1;
                for (Map<String, Object> result : results) {
                    String sql = getTargetSearchQuery(tableName, metaData);

                    try (PreparedStatement ps2 = targetCon.prepareStatement(sql)) {

                        setPSForSelectTarget(metaData, result, ps2);
                        // System.out.println("2: " + ps2.toString());
                        try (ResultSet rs1 = ps2.executeQuery()) {
                            if (rs1.next()) {
                                setPSForUpdateTarget(metaData, result, psUpdate);
                                System.out.println("Updating entry: " + psUpdate);
                                psUpdate.addBatch();
                            } else {
                                setPSForInsertTarget(metaData, result, psInsert);
                                psInsert.addBatch();
                                System.out.println("Inserting entry: " + psInsert);
                            }
                            lastSyncId = (Integer) result.get(COLUMN_NAME_MAX_SYNC_ID);
                        }
                    }
                }
                if (lastSyncId > 0) {
                    int[] insertResults = psInsert.executeBatch();
                    int[] updateResults = psUpdate.executeBatch();

                    if (isUpdateSuccessful(insertResults, updateResults)) {
                        updateSyncVersion(syncVersionTableName, targetCon, lastSyncId);
                        targetCon.commit();
                    } else {
                        targetCon.rollback();
                    }

                    canSleep = insertResults.length == 0 && updateResults.length == 0;
                } else {
                    canSleep = true;
                    targetCon.rollback();
                }
//                                                System.out.println("3: " + Arrays.toString(insertResults));
//                                                System.out.println("4: " + Arrays.toString(updateResults));
//                                                System.out.println("5: last sync ID: " + lastSyncId);
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while obtaining sync data from of target schema: "
                                          + getSchema(tableName), e);
        }
        return canSleep;
    }

    protected void setPSForSelectTarget(TableMetaData metaData, Map<String, Object> result, PreparedStatement ps2)
            throws SQLException {

        List<String> primaryKeys = metaData.getPrimaryKeys();
        for (int i = 0; i < primaryKeys.size(); i++) {
            ps2.setObject(i + 1, result.get(primaryKeys.get(i)));
        }
    }

    protected String getTargetSearchQuery(String tableName, TableMetaData metaData) {

        // SELECT %s FROM %s WHERE %s
        String sql = getQuery(SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY);
        sql = String.format(sql, metaData.getColumns(), tableName, metaData.getSearchFilter());
        return sql;
    }

    protected String getTargetInsertQuery(String tableName, TableMetaData metaData) {

        // INSERT INTO %s (%s) VALUES (%s)
        String sqlInsert = getQuery(SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY);
        sqlInsert = String.format(sqlInsert, tableName, metaData.getColumns(), metaData.getParameters());
        return sqlInsert;
    }

    protected String getTargetUpdateQuery(String tableName, TableMetaData metaData) {

        // UPDATE %s SET %s WHERE %s
        String sqlUpdate = getQuery(SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY);
        sqlUpdate = String.format(sqlUpdate, tableName, metaData.getUpdateFilter(), metaData
                .getSearchFilter());
        return sqlUpdate;
    }

    protected List<Map<String, Object>> getSyncDataList(String tableName, String syncTableName, TableMetaData
            tableMetaData, int targetSyncId) throws SyncClientException {

        // SELECT MAX(SYNC_ID) AS MAX_SYNC_ID, %s FROM %s WHERE SYNC_ID > ? AND SYNC_ID < ? GROUP
        // BY %s ORDER BY SYNC_ID ASC
        String sql = getQuery(SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY);
        sql = String.format(sql, tableMetaData.getColumns(), syncTableName,
                            String.join(",", tableMetaData.getPrimaryKeys()));
        List<Map<String, Object>> results = new ArrayList<>();

        String schema = getSchema(tableName);
        try (Connection sourceCon = DataSourceManager.getSourceConnection(schema)) {
            try (PreparedStatement ps = sourceCon.prepareStatement(sql)) {
                ps.setInt(1, targetSyncId);
                ps.setInt(2, targetSyncId + configManager.getBatchSize());

                //System.out.println("1: " + ps.toString());
                try (ResultSet rs = ps.executeQuery()) {

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> columnNames = new ArrayList<>();

                    for (int i = 0; i < columnCount; i++) {
                        columnNames.add(metaData.getColumnName(i + 1));
                    }
                    while (rs.next()) {
                        Map<String, Object> resultEntry = new HashMap<>();
                        for (String columnName : columnNames) {
                            resultEntry.put(columnName, rs.getObject(columnName));
                        }
                        results.add(resultEntry);
                    }
                }
            } catch (SQLException e) {
                throw new SyncClientException("Error while obtaining sync data from table: " + syncTableName + " of " +
                                              "schema: " + schema, e);
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while obtaining a connection from schema: " + schema, e);
        }
        return results;
    }

    @Override
    public boolean canSyncData(String tableName) throws SyncClientException {
        return true;
    }

    @Override
    public String getSchema(String tableName) throws SyncClientException {
        return Constant.SCHEMA_TYPE_IDENTITY;
    }

    @Override
    public List<SQLStatement> generateSyncScripts(String tableName) throws SyncClientException {

        List<SQLStatement> scripts = new ArrayList<>();
        List<ColumnData> columnData = getColumnData(tableName);

        scripts.add(generateSyncTableDropStatement(tableName));
        scripts.addAll(generateDropTriggerStatement(tableName));
        scripts.add(generateSyncTableCreateStatement(tableName, columnData));
        scripts.addAll(generateCreateTriggerStatement(tableName, columnData));
        scripts.add(generateSyncVersionTableCreateStatement(tableName));

        return scripts;
    }

    protected void updateSyncVersion(String syncVersionTableName, Connection targetCon, int lastSyncId)
            throws SQLException {

        // UPDATE %s SET SYNC_ID = ?
        String updateSyncVersion = getQuery(SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY);
        updateSyncVersion = String.format(updateSyncVersion, syncVersionTableName);
        try (PreparedStatement ps = targetCon.prepareStatement
                (updateSyncVersion)) {
            ps.setInt(1, lastSyncId);
            ps.executeUpdate();
        }
    }

    private boolean isUpdateSuccessful(int[] insertResults, int[] updateResults) {

        return Arrays.stream(insertResults)
                     .noneMatch(value -> value == PreparedStatement.EXECUTE_FAILED) &&
               Arrays.stream(updateResults)
                  .noneMatch(value -> value == PreparedStatement.EXECUTE_FAILED);
    }

    protected void setPSForInsertTarget(TableMetaData metaData, Map<String, Object> rs, PreparedStatement
            psTargetInsert) throws SQLException, SyncClientException {

        List<ColumnData> columnDataList = metaData.getColumnDataList();
        for (int i = 0; i < columnDataList.size(); i++) {
            psTargetInsert.setObject(i + 1, rs.get(columnDataList.get(i).getName()));
        }
    }

    protected void setPSForUpdateTarget(TableMetaData metaData, Map<String, Object> rs, PreparedStatement psTargetUpdate)
            throws SQLException, SyncClientException {

        List<String> primaryKeys = metaData.getPrimaryKeys();
        List<String> nonPrimaryKeys = metaData.getNonPrimaryKeys();

        for (int i = 0; i < nonPrimaryKeys.size(); i++) {
            psTargetUpdate.setObject(i + 1, rs.get(nonPrimaryKeys.get(i)));
        }
        for (int i = 0; i < primaryKeys.size(); i++) {
            psTargetUpdate.setObject(nonPrimaryKeys.size() + 1 + i, rs.get(primaryKeys.get(i)));
        }
    }

    private List<ColumnData> getColumnData(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);

        try (Connection connection = DataSourceManager.getSourceConnection(schema)) {

            DatabaseMetaData metaData = connection.getMetaData();
            List<ColumnData> columnDataList = new ArrayList<>();

            try(ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                while (resultSet.next()) {
                    String name = resultSet.getString(JDBC_META_DATA_COLUMN_NAME);
                    String type = resultSet.getString(JDBC_META_DATA_TYPE_NAME);
                    int size = resultSet.getInt(JDBC_META_DATA_COLUMN_SIZE);

                    ColumnData columnData = new ColumnData(name, type, size);
                    columnDataList.add(columnData);
                }
            }
            return columnDataList;
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table metadata of source table: " + tableName, e);
        }
    }

    private List<String> getPrimaryKeys(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);

        try (Connection connection = DataSourceManager.getSourceConnection(schema)) {

            DatabaseMetaData metaData = connection.getMetaData();
            List<String> primaryKeys = new ArrayList<>();

            try(ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName)) {
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME");
                    primaryKeys.add(name);
                }
            }
            return primaryKeys;
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table primary metadata of source table: " + tableName,
                                          e);
        }
    }

    private SQLStatement generateSyncTableDropStatement(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);
        // DROP TABLE IF EXISTS %s
        String sql = getQuery(SQL_TEMPLATE_DROP_TABLE_MYSQL_KEY);
        sql = String.format(sql, getSyncTableName(tableName));

        return new SQLStatement(schema, sql, SQL_STATEMENT_TYPE_SOURCE);
    }

    private SQLStatement generateSyncTableCreateStatement(String tableName, List<ColumnData> columnData) throws
            SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);

        // CREATE TABLE % (SYNC_ID INT NOT NULL AUTO_INCREMENT, %s, PRIMARY KEY (SYNC_ID))
        String createSyncTableSql = getQuery(SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL_KEY);
        createSyncTableSql = String.format(createSyncTableSql, getSyncTableName(tableName), generateColumnList
                (columnData));

        return new SQLStatement(schema, createSyncTableSql, SQL_STATEMENT_TYPE_SOURCE);
    }

    private SQLStatement generateSyncVersionTableCreateStatement(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);

        // CREATE TABLE IF NOT EXISTS %s (SYNC_ID INT)
        String createSyncVersionTableSql = getQuery(SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL_KEY);
        createSyncVersionTableSql = String.format(createSyncVersionTableSql, getSyncVersionTableName(tableName));

        return new SQLStatement(schema, createSyncVersionTableSql, SQL_STATEMENT_TYPE_TARGET);
    }

    private List<SQLStatement> generateCreateTriggerStatement(String tableName, List<ColumnData> columnData) throws
            SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);
        List<SQLStatement> triggerSqlStatements = new ArrayList<>();
        StringJoiner columnJoiner = new StringJoiner(",");
        StringJoiner columnValueJoiner = new StringJoiner(",");

        for (ColumnData columnEntry : columnData) {
            columnJoiner.add(columnEntry.getName());
            columnValueJoiner.add("NEW." + columnEntry.getName());
        }

        // CREATE TRIGGER %%s BEFORE %%s ON %s FOR EACH ROW BEGIN INSERT INTO %s (%s) VALUES (%s); END
        String createTriggerSql = getQuery(SQL_TEMPLATE_CREATE_TRIGGER_MYSQL_KEY);
        createTriggerSql = String.format(createTriggerSql, tableName, getSyncTableName(tableName), columnJoiner
                .toString(), columnValueJoiner.toString());

        String insertTriggerSql = String.format(createTriggerSql, getInsertTriggerName(tableName), "INSERT");
        String updateTriggerSql = String.format(createTriggerSql, getUpdateTriggerName(tableName), "UPDATE");

        triggerSqlStatements.add(new SQLStatement(schema, insertTriggerSql, SQL_STATEMENT_TYPE_SOURCE));
        triggerSqlStatements.add(new SQLStatement(schema, updateTriggerSql, SQL_STATEMENT_TYPE_SOURCE));

        return triggerSqlStatements;
    }

    private List<SQLStatement> generateDropTriggerStatement(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);
        List<SQLStatement> triggerSqlStatements = new ArrayList<>();

        // DROP TRIGGER IF EXISTS %s
        String dropTriggerSql = getQuery(SQL_TEMPLATE_DROP_TRIGGER_MYSQL_KEY);

        String dropInsertTriggerSql = String.format(dropTriggerSql, getInsertTriggerName(tableName));
        String dropUpdateTriggerSql = String.format(dropTriggerSql, getUpdateTriggerName(tableName));
        triggerSqlStatements.add(new SQLStatement(schema, dropInsertTriggerSql, SQL_STATEMENT_TYPE_SOURCE));
        triggerSqlStatements.add(new SQLStatement(schema, dropUpdateTriggerSql, SQL_STATEMENT_TYPE_SOURCE));

        return triggerSqlStatements;
    }

    protected int getSourceMaxSyncId(String tableName, String syncTableName) throws SyncClientException {

        int sourceMaxSyncId = 0;
        try (Connection connection = DataSourceManager.getSourceConnection(getSchema(tableName))) {

            // SELECT MAX(SYNC_ID) FROM %s
            String sql = getQuery(SQL_TEMPLATE_SELECT_MAX_SYNC_ID_KEY);
            sql = String.format(sql, syncTableName);
            try (PreparedStatement ps1 = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps1.executeQuery()) {
                    // If the SYNC_VERSION table is empty, set SYNC_ID to 0;
                    if (rs.next()) {
                        sourceMaxSyncId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table metadata of target table: " +
                                          syncTableName, e);
        }
        return sourceMaxSyncId;
    }

    protected int getOrInsertDefaultTargetSyncId(String tableName, String syncVersionTableName)
            throws SyncClientException {

        int targetSyncId = 0;
        try (Connection connection = DataSourceManager.getTargetConnection(getSchema(tableName))) {

            // SELECT SYNC_ID FROM %s
            String sql = getQuery(SQL_TEMPLATE_SELECT_SYNC_ID_KEY);
            sql = String.format(sql, syncVersionTableName);

            try (PreparedStatement ps1 = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps1.executeQuery()) {
                    // If the SYNC_VERSION table is empty, set SYNC_ID to 0;
                    if (!rs.next()) {
                        // INSERT INTO %s (SYNC_ID) VALUES (?)
                        sql = getQuery(SQL_TEMPLATE_INSERT_SYNC_ID_KEY);
                        sql = String.format(sql, syncVersionTableName);
                        try (PreparedStatement ps2 = connection.prepareStatement(sql)) {
                            ps2.setInt(1, 0);
                            ps2.executeUpdate();
                        }
                    } else {
                        targetSyncId = rs.getInt("SYNC_ID");
                    }
                }
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table metadata of target table: " +
                                          syncVersionTableName, e);
        }
        return targetSyncId;
    }
}
