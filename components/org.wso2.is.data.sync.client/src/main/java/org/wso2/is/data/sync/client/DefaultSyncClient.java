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
import org.wso2.is.data.sync.client.exception.SyncClientException;
import org.wso2.is.data.sync.client.util.Constant;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.is.data.sync.client.util.Constant.SQL_STATEMENT_TYPE_SOURCE;
import static org.wso2.is.data.sync.client.util.Constant.SQL_STATEMENT_TYPE_TARGET;
import static org.wso2.is.data.sync.client.util.Util.getInsertTriggerName;
import static org.wso2.is.data.sync.client.util.Util.getSyncTableName;
import static org.wso2.is.data.sync.client.util.Util.getSyncVersionTableName;
import static org.wso2.is.data.sync.client.util.Util.getUpdateTriggerName;

public class DefaultSyncClient implements SyncClient {

    protected SyncClientConfigManager configManager = new SyncClientConfigManager();

    public String getSyncSourceVersion() {

        return configManager.getStartVersion();
    }

    public String getSyncTargetVersion() {

        return configManager.getEndVersion();
    }

    public void syncData(String tableName) throws SyncClientException {

        ExecutorService dataSyncExecutor = Executors.newSingleThreadExecutor();

        dataSyncExecutor.execute(() -> {

            Thread.currentThread().setName("sync-executor-" + tableName);
            try {
                String syncVersionTableName = getSyncVersionTableName(tableName);
                int targetSyncId = 0;
                int sourceMaxSyncId = 0;
                try (Connection connection = DataSourceManager.getTargetConnection(getSchema(tableName))) {

                    String sql = "SELECT SYNC_ID FROM " + syncVersionTableName;
                    try (PreparedStatement ps1 = connection.prepareStatement(sql)) {
                        try (ResultSet rs = ps1.executeQuery()) {
                            // If the SYNC_VERSION table is empty, set SYNC_ID to 0;
                            if (!rs.next()) {
                                sql = "INSERT INTO " + syncVersionTableName + " (SYNC_ID) VALUES (?)";
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

                String syncTableName = getSyncTableName(tableName);
                List<ColumnData> columnDataList = getColumnData(tableName);
                List<String> primaryKeys = getPrimaryKeys(tableName);
                List<String> nonPrimaryKeys = new ArrayList<>();

                StringJoiner columnJoiner = new StringJoiner(", ");
                StringJoiner valueJoiner = new StringJoiner(", ");
                StringJoiner updateJoiner = new StringJoiner(" AND ");
                StringJoiner searchJoiner = new StringJoiner(" AND ");

                populateStringJoiners(columnDataList, primaryKeys, nonPrimaryKeys, columnJoiner, valueJoiner,
                                      updateJoiner, searchJoiner);

                String columnString = columnJoiner.toString();
                String valueString = valueJoiner.toString();
                String updateString = updateJoiner.toString();
                String searchString = searchJoiner.toString();

                while (true) {

                    boolean canSleep;
                    targetSyncId = getTargetSyncId(tableName, syncVersionTableName, targetSyncId);
                    sourceMaxSyncId = getSourceMaxSyncId(tableName, syncVersionTableName, sourceMaxSyncId,
                                                         syncTableName);

                    System.out.println("For table: " + tableName + " source max sync ID: " + sourceMaxSyncId + " " +
                                       "target sync ID: " + targetSyncId);
                    if (sourceMaxSyncId > targetSyncId) {

                        try (Connection sourceCon = DataSourceManager.getSourceConnection(getSchema(tableName))) {
                            String sql = "SELECT MAX(SYNC_ID), " + columnString + " FROM " + syncTableName + " WHERE " +
                                         "SYNC_ID > ? AND SYNC_ID < ? GROUP BY " + String.join(",", primaryKeys) + " " +
                                         "ORDER BY SYNC_ID ASC ";

                            try (PreparedStatement ps1 = sourceCon.prepareStatement(sql)) {
                                ps1.setInt(1, targetSyncId);
                                ps1.setInt(2, targetSyncId + configManager.getBatchSize());

                                //System.out.println("1: " + ps1.toString());
                                try (ResultSet rs = ps1.executeQuery()) {
                                    if (rs.first()) {
                                        rs.beforeFirst();
                                        sql = "SELECT " + columnString + " FROM " + tableName + " WHERE " + searchString;
                                        try (Connection targetCon = DataSourceManager.getTargetConnection(
                                                getSchema(tableName))) {
                                            String sqlUpdate = "UPDATE " + tableName + " SET " + updateString + " WHERE " +
                                                               searchString;
                                            String sqlInsert = "INSERT INTO " + tableName + " (" + columnString + ") " +
                                                               "VALUES (" + valueString + ")";
                                            targetCon.setAutoCommit(false);
                                            try (PreparedStatement psTargetUpdate = targetCon.prepareStatement(sqlUpdate);
                                                 PreparedStatement psTargetInsert = targetCon.prepareStatement(sqlInsert)) {
                                                int lastSyncId = -1;
                                                while (rs.next()) {
                                                    try (PreparedStatement ps2 = targetCon.prepareStatement(sql)) {
                                                        for (int i = 0; i < primaryKeys.size(); i++) {
                                                            ps2.setObject(i + 1, rs.getObject(primaryKeys.get(i)));
                                                        }
                                                        // System.out.println("2: " + ps2.toString());
                                                        try (ResultSet rs1 = ps2.executeQuery()) {
                                                            if (rs1.next()) {
                                                                setPreparedStatementForUpdate(primaryKeys,
                                                                                              nonPrimaryKeys, rs,
                                                                                              psTargetUpdate);
                                                                System.out.println("Updating entry: " + psTargetUpdate);
                                                                psTargetUpdate.addBatch();
                                                            } else {
                                                                setPreparedStatementForInsert(columnDataList, rs,
                                                                                              psTargetInsert);
                                                                psTargetInsert.addBatch();
                                                                System.out.println("Inserting entry " + psTargetInsert);
                                                            }
                                                            lastSyncId = rs.getInt(1);
                                                        }
                                                    }
                                                }
                                                int[] insertResults = psTargetInsert.executeBatch();
                                                int[] updateResults = psTargetUpdate.executeBatch();

                                                if (isUpdateSuccessful(insertResults, updateResults)) {
                                                    targetCon.rollback();

                                                } else {
                                                    updateSyncVersion(syncVersionTableName, targetCon, lastSyncId);
                                                    targetCon.commit();
                                                }

                                                canSleep = insertResults.length == 0 && updateResults.length == 0;
//                                                System.out.println("3: " + Arrays.toString(insertResults));
//                                                System.out.println("4: " + Arrays.toString(updateResults));
//                                                System.out.println("5: last sync ID: " + lastSyncId);
                                            }
                                        }
                                    } else {
                                        canSleep = true;
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            throw new SyncClientException("Error while retrieving table metadata of target table: " +
                                                          syncVersionTableName, e);
                        }
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
                //TODO
                e.printStackTrace();
            }
        });
    }

    private void updateSyncVersion(String syncVersionTableName, Connection targetCon, int lastSyncId)
            throws SQLException {
        String updateSyncVersion = "UPDATE " + syncVersionTableName + " SET " +
                                   "SYNC_ID = ?";
        try (PreparedStatement ps = targetCon.prepareStatement
                (updateSyncVersion)) {
            ps.setInt(1, lastSyncId);
            ps.executeUpdate();
        }
    }

    private boolean isUpdateSuccessful(int[] insertResults, int[] updateResults) {
        return Arrays.stream(insertResults)
                     .anyMatch(value -> value == PreparedStatement.EXECUTE_FAILED) ||
               Arrays.stream(updateResults)
                  .anyMatch(value -> value == PreparedStatement.EXECUTE_FAILED);
    }

    private void setPreparedStatementForInsert(List<ColumnData> columnDataList, ResultSet rs,
                                               PreparedStatement psTargetInsert) throws SQLException {
        for (int i = 0; i < columnDataList.size(); i++) {
            psTargetInsert.setObject(i + 1, rs.getObject(
                    columnDataList.get(i).getName()));
        }
    }

    private void setPreparedStatementForUpdate(List<String> primaryKeys, List<String> nonPrimaryKeys, ResultSet rs,
                                               PreparedStatement psTargetUpdate) throws SQLException {
        for (int i = 0; i < nonPrimaryKeys.size(); i++) {
            psTargetUpdate.setObject(i + 1, rs.getObject(
                    nonPrimaryKeys.get(i)));
        }
        for (int i = 0; i < primaryKeys.size(); i++) {
            psTargetUpdate.setObject(nonPrimaryKeys.size() + 1 +
                                     i, rs.getObject(primaryKeys.get(i)));
        }
    }

    private void populateStringJoiners(List<ColumnData> columnDataList, List<String> primaryKeys,
                                       List<String> nonPrimaryKeys, StringJoiner columnJoiner, StringJoiner valueJoiner,
                                       StringJoiner updateJoiner, StringJoiner searchJoiner) {
        for (ColumnData columnData : columnDataList) {

            String columnName = columnData.getName();
            columnJoiner.add(columnName);
            valueJoiner.add("?");

            if (!primaryKeys.contains(columnName)) {
                updateJoiner.add(String.format("%s = ?", columnName));
                nonPrimaryKeys.add(columnName);
            }
        }

        for (String primaryKey : primaryKeys) {
            searchJoiner.add(String.format("%s = ?", primaryKey));
        }
    }

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

    @Override
    public boolean canSyncData(String tableName) throws SyncClientException {
        return true;
    }

    @Override
    public String getSchema(String tableName) throws SyncClientException {
        return Constant.SCHEMA_TYPE_IDENTITY;
    }

    private List<ColumnData> getColumnData(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);

        try (Connection connection = DataSourceManager.getSourceConnection(schema)) {

            DatabaseMetaData metaData = connection.getMetaData();
            List<ColumnData> columnDataList = new ArrayList<>();

            try(ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME");
                    String type = resultSet.getString("TYPE_NAME");
                    int size = resultSet.getInt("COLUMN_SIZE");

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
        String sql = "DROP TABLE IF EXISTS " + getSyncTableName(tableName);

        return new SQLStatement(schema, sql, SQL_STATEMENT_TYPE_SOURCE);
    }

    private SQLStatement generateSyncTableCreateStatement(String tableName, List<ColumnData> columnData) throws
            SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);

        StringJoiner columnJoiner = new StringJoiner(", ");

        for (ColumnData columnEntry : columnData) {
            // COLUMN_NAME COLUMN_TYPE (COLUMN_SIZE)
            String columnTemplate = "%s %s(%d)";
            columnJoiner.add(String.format(columnTemplate, columnEntry.getName(), columnEntry.getType(),
                                           columnEntry.getSize()));
        }

        String createSyncTableSql = "CREATE TABLE " + getSyncTableName(tableName) + " (SYNC_ID INT NOT " +
                                    "NULL AUTO_INCREMENT, " + columnJoiner.toString() + ", PRIMARY KEY (SYNC_ID))";
        return new SQLStatement(schema, createSyncTableSql, SQL_STATEMENT_TYPE_SOURCE);
    }

    private SQLStatement generateSyncVersionTableCreateStatement(String tableName) throws SyncClientException {

        String schema = getSchema(tableName);
        String dataSourceType = DataSourceManager.getDataSourceType(schema);
        String createSyncVersionTableSql = "CREATE TABLE IF NOT EXISTS " + getSyncVersionTableName(tableName) +
                                           " (SYNC_ID INT)";

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
        String createTriggerSql = "CREATE TRIGGER %s BEFORE %s ON " + tableName + " FOR EACH ROW BEGIN INSERT " +
                                    "INTO " + getSyncTableName(tableName) + " (" + columnJoiner.toString() + ") VALUES (" +
                                    columnValueJoiner.toString() + "); END";

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

        String dropTriggerSql = "DROP TRIGGER IF EXISTS %s";

        String dropInsertTriggerSql = String.format(dropTriggerSql, getInsertTriggerName(tableName));
        String dropUpdateTriggerSql = String.format(dropTriggerSql, getUpdateTriggerName(tableName));
        triggerSqlStatements.add(new SQLStatement(schema, dropInsertTriggerSql, SQL_STATEMENT_TYPE_SOURCE));
        triggerSqlStatements.add(new SQLStatement(schema, dropUpdateTriggerSql, SQL_STATEMENT_TYPE_SOURCE));

        return triggerSqlStatements;
    }

    private int getSourceMaxSyncId(String tableName, String syncVersionTableName, int sourceMaxSyncId,
                                   String syncTableName) throws SyncClientException {

        try (Connection connection = DataSourceManager.getSourceConnection(getSchema(tableName))) {

            String sql = "SELECT MAX(SYNC_ID) FROM " + syncTableName;
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
                                          syncVersionTableName, e);
        }
        return sourceMaxSyncId;
    }

    private int getTargetSyncId(String tableName, String syncVersionTableName, int targetSyncId)
            throws SyncClientException {

        try (Connection connection = DataSourceManager.getTargetConnection(getSchema(tableName))) {

            String sql = "SELECT SYNC_ID FROM " + syncVersionTableName;
            try (PreparedStatement ps1 = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps1.executeQuery()) {
                    // If the SYNC_VERSION table is empty, set SYNC_ID to 0;
                    if (rs.next()) {
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
