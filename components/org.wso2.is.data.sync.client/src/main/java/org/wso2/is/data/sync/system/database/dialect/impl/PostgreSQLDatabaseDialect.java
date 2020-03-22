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

import org.apache.commons.collections.CollectionUtils;
import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.database.TableMetaData;
import org.wso2.is.data.sync.system.database.dialect.Table;
import org.wso2.is.data.sync.system.database.dialect.Trigger;
import org.wso2.is.data.sync.system.exception.SyncClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_CREATE_FUNCTION_POSTGRES;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_CREATE_TRIGGER_POSTGRES;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DROP_TABLE_MYSQL;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DROP_TRIGGER_POSTGRES;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_NAME_ACTION;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_INT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_INT4;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_INT8;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_SERIAL;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_TIMESTAMP_WITHOUT_TIME_ZONE;
import static org.wso2.is.data.sync.system.util.Constant.SYNC_OPERATION_DELETE;
import static org.wso2.is.data.sync.system.util.Constant.TABLE_ATTRIBUTE_PRIMARY_KEY;

/**
 * Produces PosgreSQL specific database dialects.
 */
public class PostgreSQLDatabaseDialect extends ANSIDatabaseDialect {

    private static final String FUNCTION_NAME_SUFFIX = "_FUNCTION";

    @Override
    public List<String> generateCreateTable(Table table) throws SyncClientException {

        String tableName = table.getName();
        TableMetaData tableMetaData = table.getTableMetaData();

        String sql = "CREATE TABLE IF NOT EXISTS %s (%s)";
        List<ColumnData> columnDataList = tableMetaData.getColumnDataList();
        String tableColumnList = generateColumnList(columnDataList);

        List<String> primaryKeys = tableMetaData.getPrimaryKeys();
        if (CollectionUtils.isNotEmpty(primaryKeys)) {
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
        String functionName = triggerName + FUNCTION_NAME_SUFFIX;

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

        String functionStatement = String.format(SQL_TEMPLATE_CREATE_FUNCTION_POSTGRES, functionName, functionName,
                targetTableName, columnJoiner, columnValueJoiner, functionName);
        String triggerStatement = String.format(SQL_TEMPLATE_CREATE_TRIGGER_POSTGRES, triggerName, triggerType,
                triggerEvent, sourceTableName, selectionPolicy, functionName);

        sqlStatements.add(functionStatement);
        sqlStatements.add(triggerStatement);
        return sqlStatements;
    }

    @Override
    public List<String> generateDropTrigger(String triggerName, String targetTableName) throws SyncClientException {

        // DROP TRIGGER IF EXISTS %s ON %s
        return Collections.singletonList(String.format(SQL_TEMPLATE_DROP_TRIGGER_POSTGRES, triggerName,
                targetTableName));
    }

    @Override
    public List<String> generateDropTable(String name) throws SyncClientException {

        // DROP TABLE IF EXISTS %s
        return Collections.singletonList(String.format(SQL_TEMPLATE_DROP_TABLE_MYSQL, name));
    }

    private String generateColumnList(List<ColumnData> columnData) {

        StringJoiner columnJoiner = new StringJoiner(", ");

        for (ColumnData columnEntry : columnData) {
            columnJoiner.add(getColumnEntryString(columnEntry));
        }
        return columnJoiner.toString();
    }

    private String getColumnEntryString(ColumnData columnEntry) {

        String columnEntryString;

        if (COLUMN_TYPE_TIMESTAMP_WITHOUT_TIME_ZONE.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_SERIAL.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_INT.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_INT4.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_INT8.equalsIgnoreCase(columnEntry.getType())) {

            /*
            Let the database assign default sizes for the filtered column.
            Column format: "COLUMN_NAME COLUMN_TYPE DEFAULT DEFAULT_VALUE".
             */
            columnEntryString = columnEntry.getName() + " " + columnEntry.getType();

            if (columnEntry.getDefaultValue() != null) {
                columnEntryString = columnEntryString + " DEFAULT " + columnEntry.getDefaultValue();
            }
        } else {
            /*
            Setting the columns size for other columns.
            Column format: "COLUMN_NAME COLUMN_TYPE (COLUMN_SIZE) eg: VARCHAR".
             */
            String columnTemplate = "%s %s (%d)";

            columnEntryString = String.format(columnTemplate, columnEntry.getName(), columnEntry.getType(),
                    columnEntry.getSize());

            if (columnEntry.getDefaultValue() != null) {
                columnEntryString = columnEntryString + " DEFAULT " + columnEntry.getDefaultValue();
            }
        }

        if (columnEntry.isAutoIncrement()) {
            /*
            Changing the data type from INT to SERIAL for auto incrementing.
            Column format: "COLUMN_NAME SERIAL (COLUMN_SIZE)".
             */
            columnEntryString = columnEntryString.replace(columnEntry.getType(), COLUMN_TYPE_SERIAL);
        }
        return columnEntryString;
    }

    @Override
    public List<String> generateDropTrigger(String name) throws SyncClientException {

        // Not implemented.
        return null;
    }
}
