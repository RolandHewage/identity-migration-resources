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

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_DROP_TRIGGER_ORACLE;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_NAME_ACTION;
import static org.wso2.is.data.sync.system.util.Constant.SYNC_OPERATION_DELETE;

/**
 * Produces Oracle specific database dialects.
 */
public class OracleDatabaseDialect extends ANSIDatabaseDialect {

    @Override
    public List<String> generateCreateTrigger(Trigger trigger) throws SyncClientException {

        // CREATE TRIGGER {triggerName} {triggerType} {triggerEvent} ON {sourceTableName} {selectionPolicy} BEGIN
        // INSERT INTO {targetTableName} ({columnNames}) VALUES ({values}); END;

        List<String> sqlStatements = new ArrayList<>();
        String triggerStr = "CREATE OR REPLACE TRIGGER %s %s %s ON %s %s BEGIN INSERT INTO %s (%s) VALUES (%s); END";

        String sourceTableName = trigger.getSourceTableName();
        String targetTableName = trigger.getTargetTableName();
        String triggerType = trigger.getTriggerTiming();
        String triggerEvent = trigger.getTriggerEvent();
        String selectionPolicy = trigger.getSelectionPolicy();
        String triggerName = trigger.getName();
        TableMetaData tableMetaData = trigger.getTableMetaData();
        List<ColumnData> columnDataList = tableMetaData.getColumnDataList();

        StringJoiner columnJoiner = new StringJoiner(",");
        StringJoiner columnValueJoiner = new StringJoiner(",");

        for (ColumnData columnEntry : columnDataList) {
            columnJoiner.add(columnEntry.getName());

            if (SYNC_OPERATION_DELETE.equals(triggerEvent)) {
                columnValueJoiner.add(":OLD." + columnEntry.getName());
            } else {
                columnValueJoiner.add(":NEW." + columnEntry.getName());
            }
        }

        // Add ACTION column to the trigger.
        columnJoiner.add(COLUMN_NAME_ACTION);
        columnValueJoiner.add(String.format("'%s'", triggerEvent));

        // CREATE TRIGGER {triggerName} {triggerType} {triggerEvent} ON {sourceTableName} {selectionPolicy} BEGIN
        // INSERT INTO {targetTableName} ({columnNames}) VALUES ({values}); END;
        String triggerStatement = String.format(triggerStr, triggerName, triggerType, triggerEvent, sourceTableName,
                selectionPolicy, targetTableName, columnJoiner, columnValueJoiner);
        sqlStatements.add(triggerStatement);
        return sqlStatements;
    }

    @Override
    public List<String> generateCreateTable(Table table) throws SyncClientException {

        return null;
    }

    @Override
    public List<String> generateDropTrigger(String name) throws SyncClientException {

        return Collections.singletonList(String.format(SQL_TEMPLATE_DROP_TRIGGER_ORACLE, name));
    }

    @Override
    public List<String> generateDropTable(String name) throws SyncClientException {

        return null;
    }
}
