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

package org.wso2.is.data.sync.system.pipeline.process;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.database.TableMetaData;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.EntryField;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_INSERT_SYNC_ID_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_SELECT_MAX_SYNC_ID_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_SELECT_SYNC_ID_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.getQuery;
import static org.wso2.is.data.sync.system.util.CommonUtil.convertResultToEntryField;
import static org.wso2.is.data.sync.system.util.CommonUtil.getColumnData;
import static org.wso2.is.data.sync.system.util.CommonUtil.getPrimaryKeys;
import static org.wso2.is.data.sync.system.util.CommonUtil.getSyncTableName;
import static org.wso2.is.data.sync.system.util.CommonUtil.getSyncVersionTableName;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_NAME_ACTION;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_NAME_SYNC_ID;

/**
 * Initial step of the data sync pipeline.
 * <p>
 * The batch processor prepares a list of {@link JournalEntry} by polling a journal instance in the source database.
 */
public class BatchProcessor {

    private static final Log log = LogFactory.getLog(BatchProcessor.class);

    public List<JournalEntry> pollJournal(PipelineContext context) throws SyncClientException {

        List<JournalEntry> journalEntryList = new ArrayList<>();

        PipelineConfiguration pipelineConfiguration = context.getPipelineConfiguration();
        String tableName = pipelineConfiguration.getTableName();
        String syncVersionTableName = getSyncVersionTableName(tableName);
        String syncTableName = getSyncTableName(tableName);
        Connection sourceConnection = context.getSourceConnection();
        Connection targetConnection = context.getTargetConnection();
        int batchSize = pipelineConfiguration.getConfiguration().getBatchSize();

        TableMetaData tableMetaData = new TableMetaData.Builder().setColumnData(
                getColumnData(tableName, sourceConnection)).setPrimaryKeys(
                getPrimaryKeys(tableName, sourceConnection)).build();

        int targetSyncId = getOrInsertDefaultTargetSyncId(targetConnection, syncVersionTableName);
        int sourceMaxSyncId = getSourceMaxSyncId(syncTableName, sourceConnection);

        if (log.isDebugEnabled()) {
            log.info("For table: " + tableName + " source max sync ID: " + sourceMaxSyncId + " " +
                    "target sync ID: " + targetSyncId);
        }

        if (sourceMaxSyncId > targetSyncId) {
            log.info("Fetching sync data for table: " + tableName + " from source table: " + syncTableName);
            journalEntryList = getSyncDataList(syncTableName, tableMetaData,
                    targetSyncId, batchSize, sourceConnection);
            log.info("Fetched: " + journalEntryList.size() + " records for syncing for: " + tableName);
        } else {
            log.info("No data to sync for: " + tableName);
        }
        return journalEntryList;
    }

    private List<JournalEntry> getSyncDataList(String syncTableName, TableMetaData
            tableMetaData, int targetSyncId, int batchSize, Connection sourceCon) throws SyncClientException {

        List<JournalEntry> journalEntryList = new ArrayList<>();
        // SELECT SYNC_ID, %s FROM %s WHERE SYNC_ID > ? AND SYNC_ID < ? GROUP
        // BY %s ORDER BY SYNC_ID ASC
        String sql = getQuery(SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY);
        sql = String.format(sql, tableMetaData.getColumns(), syncTableName);
        try (PreparedStatement ps = sourceCon.prepareStatement(sql)) {
            ps.setInt(1, targetSyncId);
            ps.setInt(2, targetSyncId + batchSize + 1);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JournalEntry entry = new JournalEntry();
                    for (ColumnData columnData : tableMetaData.getColumnDataList()) {
                        EntryField<?> entryField = convertResultToEntryField(rs, columnData);
                        String columnName = columnData.getName();
                        entry.addEntryField(columnName, entryField);
                    }
                    EntryField<Integer> syncIdEntry = new EntryField<>(rs.getInt(COLUMN_NAME_SYNC_ID));
                    entry.addEntryField(COLUMN_NAME_SYNC_ID, syncIdEntry);

                    entry.setOperation(rs.getString(COLUMN_NAME_ACTION));
                    journalEntryList.add(entry);
                }
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while obtaining sync data from table: " + syncTableName, e);
        }
        return journalEntryList;
    }

    protected int getOrInsertDefaultTargetSyncId(Connection connection, String syncVersionTableName)
            throws SyncClientException {

        int targetSyncId = 0;
        try {

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

    protected int getSourceMaxSyncId(String syncTableName, Connection connection) throws SyncClientException {

        int sourceMaxSyncId = 0;
        try {

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
}
