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

package org.wso2.is.data.sync.system.database.dialect.impl;

import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.database.TableMetaData;
import org.wso2.is.data.sync.system.database.dialect.Table;
import org.wso2.is.data.sync.system.database.dialect.Trigger;
import org.wso2.is.data.sync.system.exception.SyncClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_CREATE_TRIGGER_MYSQL;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DROP_TABLE_MYSQL;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DROP_TRIGGER_MYSQL;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ATTRIBUTE_AUTO_INCREMENT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_NAME_ACTION;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_BIGINT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_INT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_TIMESTAMP;
import static org.wso2.is.data.sync.system.util.Constant.SYNC_OPERATION_DELETE;
import static org.wso2.is.data.sync.system.util.Constant.TABLE_ATTRIBUTE_PRIMARY_KEY;

/**
 * Produces MySQL specific database dialects.
 */
public class MySQLDatabaseDialect extends ANSIDatabaseDialect {

    public static String generateColumnList(List<ColumnData> columnData) {

        StringJoiner columnJoiner = new StringJoiner(", ");

        for (ColumnData columnEntry : columnData) {
            columnJoiner.add(getColumnEntryString(columnEntry));
        }
        return columnJoiner.toString();
    }

    private static String getColumnEntryString(ColumnData columnEntry) {

        String columnEntryString;
        if (COLUMN_TYPE_TIMESTAMP.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_INT.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_BIGINT.equalsIgnoreCase(columnEntry.getType())) {

            // Let the database assign default sizes for the filtered column.
            // Column format: "COLUMN_NAME COLUMN_TYPE DEFAULT DEFAULT_VALUE".
            columnEntryString = columnEntry.getName() + " " + columnEntry.getType();
            if (columnEntry.getDefaultValue() != null) {
                columnEntryString = columnEntryString + " DEFAULT " + columnEntry.getDefaultValue();
            }
        } else {
            // Setting the columns size for other columns.
            // Column format: "COLUMN_NAME COLUMN_TYPE (COLUMN_SIZE) eg: VARCHAR".
            String columnTemplate = "%s %s (%d)";
            columnEntryString = String.format(columnTemplate, columnEntry.getName(), columnEntry.getType(),
                    columnEntry.getSize());
            if (columnEntry.getDefaultValue() != null) {
                columnEntryString = columnEntryString + " DEFAULT '" + columnEntry.getDefaultValue() + "'";
            }
        }

        if (columnEntry.isAutoIncrement()) {
            // Setting the AUTO_INCREMENT attribute to a column.
            // Column format: "COLUMN_NAME COLUMN_TYPE (COLUMN_SIZE) AUTO_INCREMENT".
            columnEntryString = columnEntryString + " " + COLUMN_ATTRIBUTE_AUTO_INCREMENT;
        }
        return columnEntryString;
    }

    @Override
    public List<String> generateCreateTable(Table table) throws SyncClientException {

        String tableName = table.getName();
        TableMetaData tableMetaData = table.getTableMetaData();

        String sql = "CREATE TABLE IF NOT EXISTS %s (%s)";
        List<ColumnData> columnDataList = tableMetaData.getColumnDataList();
        String tableColumnList = generateColumnList(columnDataList);

        List<String> primaryKeys = tableMetaData.getPrimaryKeys();
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (String primaryKey : primaryKeys) {
                joiner.add(primaryKey);
            }
            tableColumnList = tableColumnList + ", " + TABLE_ATTRIBUTE_PRIMARY_KEY + " (" + joiner + ")";
        }
        return Collections.singletonList(String.format(sql, tableName, tableColumnList));
    }

    @Override
    public List<String> generateCreateTrigger(Trigger trigger) throws SyncClientException {

        List<String> sqlStatements = new ArrayList<>();
        String triggerName = trigger.getName();
        String sourceTableName = trigger.getSourceTableName();
        String targetTableName = trigger.getTargetTableName();
        String triggerType = trigger.getTriggerTiming();
        String triggerEvent = trigger.getTriggerEvent();
        String selectionPolicy = trigger.getSelectionPolicy();
        TableMetaData tableMetaData = trigger.getTableMetaData();
        List<ColumnData> columnDataList = tableMetaData.getColumnDataList();

        StringJoiner columnJoiner = new StringJoiner(",");
        StringJoiner columnValueJoiner = new StringJoiner(",");

        for (ColumnData columnEntry : columnDataList) {
            columnJoiner.add(columnEntry.getName());

            if (SYNC_OPERATION_DELETE.equals(triggerEvent)) {
                columnValueJoiner.add("OLD." + columnEntry.getName());
            } else {
                columnValueJoiner.add("NEW." + columnEntry.getName());
            }
        }

        // Add ACTION column to the trigger.
        columnJoiner.add(COLUMN_NAME_ACTION);
        columnValueJoiner.add(String.format("'%s'", triggerEvent));

        // CREATE TRIGGER {triggerName} {triggerType} {triggerEvent} ON {sourceTableName} {selectionPolicy} BEGIN
        // INSERT INTO {targetTableName} ({columnNames}) VALUES ({values}); END;
        String triggerStatement = String.format(SQL_TEMPLATE_CREATE_TRIGGER_MYSQL, triggerName, triggerType,
                triggerEvent, sourceTableName, selectionPolicy, targetTableName,
                columnJoiner, columnValueJoiner);
        sqlStatements.add(triggerStatement);
        return sqlStatements;
    }

    @Override
    public List<String> generateDropTrigger(String name) throws SyncClientException {

        // DROP TRIGGER IF EXISTS %s
        return Collections.singletonList(String.format(SQL_TEMPLATE_DROP_TRIGGER_MYSQL, name));
    }

    @Override
    public List<String> generateDropTable(String name) throws SyncClientException {

        // DROP TABLE IF EXISTS %s
        return Collections.singletonList(String.format(SQL_TEMPLATE_DROP_TABLE_MYSQL, name));
    }
}
