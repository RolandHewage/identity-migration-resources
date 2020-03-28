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

package org.wso2.is.data.sync.system.pipeline.result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.util.CommonUtil;
import org.wso2.is.data.sync.system.util.Constant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.wso2.is.data.sync.system.database.SQLQueryProvider.SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY;
import static org.wso2.is.data.sync.system.database.SQLQueryProvider.getQuery;

/**
 * ResultHandler.
 */
public class ResultHandler {

    private static final Log log = LogFactory.getLog(ResultHandler.class);

    public boolean processResults(List<TransactionResult> transactionResults, PipelineContext context) {

        boolean transactionSuccess = true;

        if (transactionResults != null && !transactionResults.isEmpty()) {

            String tableName = context.getPipelineConfiguration().getTableName();
            String syncTableName = CommonUtil.getSyncTableName(tableName);
            String syncVersionTableName = CommonUtil.getSyncVersionTableName(tableName);

            for (TransactionResult transactionResult : transactionResults) {
                if (!transactionResult.isSuccess()) {
                    if (log.isDebugEnabled()) {
                        Integer syncId = (Integer) transactionResult.getJournalEntry().get(Constant.COLUMN_NAME_SYNC_ID)
                                .getValue();
                        log.debug(String.format("Error while syncing data from source table: %s to target table: %s " +
                                "with SYNC_ID: %s", syncTableName, tableName, syncId));
                    }
                    transactionSuccess = false;
                    break;
                }
            }

            if (transactionSuccess) {
                TransactionResult lastResult = transactionResults.get(transactionResults.size() - 1);
                Integer lastSyncId = (Integer) lastResult.getJournalEntry().get(Constant.COLUMN_NAME_SYNC_ID)
                        .getValue();
                try {
                    updateSyncVersion(syncVersionTableName, context.getTargetConnection(), lastSyncId);
                } catch (SQLException e) {
                    log.error("Error while updating the last sync ID to: " + lastSyncId + " in table: " +
                            syncVersionTableName);
                }
            }
        }
        return transactionSuccess;
    }

    protected void updateSyncVersion(String syncVersionTable, Connection targetCon, int lastSyncId)
            throws SQLException {

        // UPDATE %s SET SYNC_ID = ?
        String updateSyncVersion = getQuery(SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY);
        updateSyncVersion = String.format(updateSyncVersion, syncVersionTable);
        try (PreparedStatement ps = targetCon.prepareStatement
                (updateSyncVersion)) {
            ps.setInt(1, lastSyncId);
            ps.executeUpdate();
        }
    }
}
