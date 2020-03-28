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

package org.wso2.is.data.sync.system.pipeline.persist;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.database.TableMetaData;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.EntryField;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.result.TransactionResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.getQuery;
import static org.wso2.is.data.sync.system.util.CommonUtil.convertEntryFieldToStatement;
import static org.wso2.is.data.sync.system.util.CommonUtil.getColumnData;
import static org.wso2.is.data.sync.system.util.CommonUtil.getPrimaryKeys;
import static org.wso2.is.data.sync.system.util.Constant.ENTRY_FILED_ACTION_DELETE;
import static org.wso2.is.data.sync.system.util.Constant.ENTRY_FILED_ACTION_INSERT;
import static org.wso2.is.data.sync.system.util.Constant.ENTRY_FILED_ACTION_UPDATE;
import static org.wso2.is.data.sync.system.util.Constant.FOREIGN_KEY_VIOLATION_ERROR_CODE_POSTGRESQL;

/**
 * The persistence stage of the data sync pipeline.
 * The list of {@link JournalEntry} provided by the pipeline predecessors will be persisted at this stage.
 */
public class Persistor {

    private static final Log log = LogFactory.getLog(Persistor.class);

    public List<TransactionResult> persist(List<JournalEntry> transformedEntryList, PipelineContext context)
            throws SyncClientException {

        List<TransactionResult> transactionResults = new ArrayList<>();
        if (transformedEntryList == null || transformedEntryList.isEmpty()) {
            return transactionResults;
        }
        try {

            PipelineConfiguration pipelineConfiguration = context.getPipelineConfiguration();
            String tableName = pipelineConfiguration.getTableName();

            Connection targetConnection = context.getTargetConnection();
            TableMetaData tableMetaData = new TableMetaData.Builder().setColumnData(
                    getColumnData(tableName, targetConnection)).setPrimaryKeys(
                    getPrimaryKeys(tableName, targetConnection)).build();

            String sqlUpdate = getTargetUpdateQuery(tableName, tableMetaData);
            String sqlInsert = getTargetInsertQuery(tableName, tableMetaData);
            String sqlDelete = getTargetDeleteQuery(tableName, tableMetaData);

            try (PreparedStatement psUpdate = targetConnection.prepareStatement(sqlUpdate);
                 PreparedStatement psInsert = targetConnection.prepareStatement(sqlInsert);
                 PreparedStatement psDelete = targetConnection.prepareStatement(sqlDelete)) {
                for (JournalEntry entry : transformedEntryList) {
                    String sql = getTargetSearchQuery(tableName, tableMetaData);

                    try (PreparedStatement ps = targetConnection.prepareStatement(sql)) {

                        Map<String, EntryField<?>> rowEntry = entry.getRowEntry();
                        setPSForSelectTarget(tableMetaData, rowEntry, ps);
                        try (ResultSet rs = ps.executeQuery()) {

                            try {
                                if (rs.next()) {
                                    if (ENTRY_FILED_ACTION_DELETE.equals(entry.getOperation())) {
                                        setPSForDeleteTarget(tableMetaData, rowEntry, psDelete);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Deleting entry: " + psDelete);
                                        }
                                        psDelete.executeUpdate();
                                    } else if (ENTRY_FILED_ACTION_INSERT.equals(entry.getOperation()) ||
                                            ENTRY_FILED_ACTION_UPDATE.equals(entry.getOperation())) {

                                        setPSForUpdateTarget(tableMetaData, rowEntry, psUpdate);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Updating entry: " + psUpdate);
                                        }
                                        psUpdate.executeUpdate();
                                    }
                                } else {
                                    if (ENTRY_FILED_ACTION_DELETE.equals(entry.getOperation())) {

                                        // Ignore delete operation on none extant target entry.
                                    } else if (ENTRY_FILED_ACTION_INSERT.equals(entry.getOperation()) ||
                                            ENTRY_FILED_ACTION_UPDATE.equals(entry.getOperation())) {

                                        setPSForInsertTarget(tableMetaData, rowEntry, psInsert);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Inserting entry: " + psInsert);
                                        }
                                        psInsert.executeUpdate();
                                    }
                                }
                                TransactionResult result = new TransactionResult(entry, true);
                                transactionResults.add(result);
                            } catch (SQLException e) {
                                if (e instanceof SQLIntegrityConstraintViolationException ||
                                        /*
                                        In Postgres, the thrown 'PGSQLException' does not extend from
                                        'SQLIntegrityConstraintViolationException'.
                                        Hence, we need to handle it with the 'SQL State' for the current operation,
                                        which is '23503', indicating that there was a foreign key constraint violation.
                                         */
                                        (StringUtils.isNotEmpty(e.getSQLState()) &&
                                                e.getSQLState().equals(FOREIGN_KEY_VIOLATION_ERROR_CODE_POSTGRESQL))) {
                                    // Ignore. this will be recovered.
                                    if (log.isDebugEnabled()) {
                                        log.debug("SQL constraint violation occurred while data sync. ", e);
                                    }
                                } else {
                                    log.error("Error occurred while data sync. ", e);
                                }
                                TransactionResult result = new TransactionResult(entry, false, e);
                                transactionResults.add(result);
                                // If there is one failure, there is no need to continue processing the other results.
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new SyncClientException("Error while obtaining sync data from of target table.", e);
        }
        return transactionResults;
    }

    protected String getTargetInsertQuery(String tableName, TableMetaData metaData) {

        // INSERT INTO %s (%s) VALUES (%s)
        String sqlInsert = getQuery(SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY);
        sqlInsert = String.format(sqlInsert, tableName, metaData.getColumns(), metaData.getParameters());
        return sqlInsert;
    }

    protected String getTargetDeleteQuery(String tableName, TableMetaData metaData) {

        // DELETE FROM %s WHERE %s
        String sqlDelete = getQuery(SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY_KEY);
        sqlDelete = String.format(sqlDelete, tableName, metaData.getSearchFilter());
        return sqlDelete;
    }

    protected String getTargetUpdateQuery(String tableName, TableMetaData metaData) {

        // UPDATE %s SET %s WHERE %s
        String sqlUpdate = getQuery(SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY);
        sqlUpdate = String.format(sqlUpdate, tableName, metaData.getUpdateFilter(), metaData
                .getSearchFilter());
        return sqlUpdate;
    }

    protected String getTargetSearchQuery(String tableName, TableMetaData metaData) {

        // SELECT %s FROM %s WHERE %s
        String sql = getQuery(SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY);
        sql = String.format(sql, metaData.getColumns(), tableName, metaData.getSearchFilter());
        return sql;
    }

    protected void setPSForSelectTarget(TableMetaData metaData, Map<String, EntryField<?>> fields, PreparedStatement ps)
            throws SQLException {

        List<String> primaryKeys = metaData.getPrimaryKeys();
        for (int i = 0; i < primaryKeys.size(); i++) {
            EntryField<?> entryField = fields.get(primaryKeys.get(i));
            ps.setObject(i + 1, entryField.getValue());
        }
    }

    protected void setPSForInsertTarget(TableMetaData metaData, Map<String, EntryField<?>> fields, PreparedStatement
            psTargetInsert) throws SQLException, SyncClientException {

        List<ColumnData> columnDataList = metaData.getColumnDataList();
        for (int i = 0; i < columnDataList.size(); i++) {
            EntryField<?> entryField = fields.get(columnDataList.get(i).getName());
            Object value = null;
            if (entryField != null) {
                value = entryField.getValue();
            }
            psTargetInsert.setObject(i + 1, value);
        }
    }

    protected void setPSForUpdateTarget(TableMetaData metaData, Map<String, EntryField<?>> fields, PreparedStatement
            psTargetUpdate) throws SQLException, SyncClientException {

        List<String> primaryKeys = metaData.getPrimaryKeys();
        List<String> nonPrimaryKeys = metaData.getNonPrimaryKeys();

        for (int i = 0; i < nonPrimaryKeys.size(); i++) {
            EntryField<?> entryField = fields.get(nonPrimaryKeys.get(i));
            convertEntryFieldToStatement(psTargetUpdate, entryField, i + 1);
        }
        for (int i = 0; i < primaryKeys.size(); i++) {
            EntryField<?> entryField = fields.get(primaryKeys.get(i));
            convertEntryFieldToStatement(psTargetUpdate, entryField, (nonPrimaryKeys.size() + 1 + i));
        }
    }

    protected void setPSForDeleteTarget(TableMetaData metaData, Map<String, EntryField<?>> fields, PreparedStatement
            psTargetUpdate) throws SQLException, SyncClientException {

        List<String> primaryKeys = metaData.getPrimaryKeys();

        for (int i = 0; i < primaryKeys.size(); i++) {
            EntryField<?> entryField = fields.get(primaryKeys.get(i));
            convertEntryFieldToStatement(psTargetUpdate, entryField, i + 1);
        }
    }
}
