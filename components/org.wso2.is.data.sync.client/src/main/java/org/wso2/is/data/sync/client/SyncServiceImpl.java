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

import org.wso2.carbon.identity.core.migrate.DataSyncService;
import org.wso2.is.data.sync.client.config.SyncClientConfigManager;
import org.wso2.is.data.sync.client.datasource.DataSourceManager;
import org.wso2.is.data.sync.client.datasource.SQLStatement;
import org.wso2.is.data.sync.client.exception.SyncClientException;
import org.wso2.is.data.sync.client.util.Constant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.wso2.is.data.sync.client.util.Util.getScripId;

public class SyncServiceImpl implements DataSyncService {

    private SyncClientConfigManager configManager = new SyncClientConfigManager();
    private Map<String, SyncClient> syncClientList = new HashMap<>();

    public static void main(String[] args) throws SyncClientException {

        SyncServiceImpl syncService = new SyncServiceImpl();
        syncService.generateScripts(false);
    }

    public SyncServiceImpl() throws SyncClientException {

        List<String> syncTableList = configManager.getSyncTableList();
        List<SyncClient> syncClients = new SyncClientHolder().getSyncClients();

        for (String table : syncTableList) {
            for (SyncClient syncClient: syncClients) {
                if (syncClient.canSyncData(table)) {
                    syncClientList.put(table, createNewInstance(syncClient));
                    break;
                }
            }
        }
    }

    private SyncClient createNewInstance(SyncClient syncClient) throws SyncClientException {

        try {
            return syncClient.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SyncClientException("Error while creating an instance of: " + syncClient.getClass().getName(), e);
        }
    }

    public void generateScripts(boolean ddlOnly) throws SyncClientException {

        List<SQLStatement> sqlStatementList = aggregateScripts();

        Map<String, List<String>> scripts = aggregateDDL(sqlStatementList);

        if (ddlOnly) {
            for (Map.Entry<String, List<String>> entry : scripts.entrySet()) {

                String schema = entry.getKey();
                List<String> sqlStatements = entry.getValue();

                String sqlDelimiter = DataSourceManager.getSqlDelimiter(schema);
                String delimiter = sqlDelimiter + System.lineSeparator() + System.lineSeparator();
                StringJoiner joiner = new StringJoiner(delimiter, DataSourceManager.getDDLPrefix(schema),
                                                       sqlDelimiter + DataSourceManager.getDDLSuffix(schema));

                for (String sqlStatement : sqlStatements) {
                    joiner.add(sqlStatement);
                }

                String script = joiner.toString();

                String scriptPath = configManager.getScriptPath();
                Path path = Paths.get(scriptPath, schema + ".sql");
                System.out.println("Writing file to: " + path.toAbsolutePath());

                byte[] strToBytes = script.getBytes();

                try {
                    Files.write(path, strToBytes);
                } catch (IOException e) {
                    throw new SyncClientException("Error while generating script: " + path.toString(), e);
                }
            }
        } else {

            Map<String, List<SQLStatement>> sourceStatements = new LinkedHashMap<>();
            Map<String, List<SQLStatement>> targetStatements = new LinkedHashMap<>();

            for (SQLStatement sqlStatement : sqlStatementList) {

                if (Constant.SQL_STATEMENT_TYPE_SOURCE.equals(sqlStatement.getType())) {
                    addToStatementMap(sourceStatements, sqlStatement);
                } else {
                    addToStatementMap(targetStatements, sqlStatement);
                }
            }

            for (String schema : sourceStatements.keySet()) {
                try (Connection sourceConnection = DataSourceManager.getSourceConnection(schema)) {
                    sourceConnection.setAutoCommit(false);
                    try (Statement statement = sourceConnection.createStatement()) {
                        List<SQLStatement> sqlStatements = sourceStatements.get(schema);
                        for (SQLStatement sqlStatement : sqlStatements) {
                            System.out.println("Queuing source statement for batch operation: " + sqlStatement
                                    .getStatement());
                            statement.addBatch(sqlStatement.getStatement());
                        }
                        statement.executeBatch();
                        sourceConnection.commit();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            for (String schema : targetStatements.keySet()) {
                try (Connection targetConnection = DataSourceManager.getTargetConnection(schema)) {
                    targetConnection.setAutoCommit(false);
                    try (Statement statement = targetConnection.createStatement()) {
                        List<SQLStatement> sqlStatements = targetStatements.get(schema);
                        for (SQLStatement sqlStatement : sqlStatements) {
                            System.out.println("Queuing target statement for batch operation: " + sqlStatement
                                    .getStatement());
                            statement.addBatch(sqlStatement.getStatement());
                        }
                        statement.executeBatch();
                        targetConnection.commit();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addToStatementMap(Map<String, List<SQLStatement>> statements, SQLStatement sqlStatement) {

        if (statements.containsKey(sqlStatement.getScheme())) {
            statements.get(sqlStatement.getScheme()).add(sqlStatement);
        } else {
            statements.put(sqlStatement.getScheme(), new ArrayList<>(Collections.singleton(sqlStatement)));
        }
    }

    private List<SQLStatement> aggregateScripts() throws SyncClientException {

        List<SQLStatement> sqlStatements = new ArrayList<>();
        for (Map.Entry<String, SyncClient> syncClientEntry : syncClientList.entrySet()) {
            String tableName = syncClientEntry.getKey();
            SyncClient syncClient = syncClientEntry.getValue();

            List<SQLStatement> sqlStatementList = syncClient.generateSyncScripts(tableName);
            sqlStatements.addAll(sqlStatementList);
        }
        return sqlStatements;
    }

    private Map<String, List<String>> aggregateDDL(List<SQLStatement> sqlStatementList) throws SyncClientException {

        Map<String, List<String>> dataSyncScripts = new LinkedHashMap<>();

        for (SQLStatement sqlStatement : sqlStatementList) {

            String schema = sqlStatement.getScheme();
            String type = sqlStatement.getType();
            String statement = sqlStatement.getStatement();

            String scriptId = getScripId(schema, type);
            if (dataSyncScripts.containsKey(scriptId)) {
                dataSyncScripts.get(scriptId).add(statement);
            } else {
                List<String> statements = new ArrayList<>();
                statements.add(statement);
                dataSyncScripts.put(scriptId, statements);
            }
        }
        return dataSyncScripts;
    }

    public void syncData() throws SyncClientException {

        for (Map.Entry<String, SyncClient> entry : syncClientList.entrySet()) {

            entry.getValue().syncData(entry.getKey());
        }
    }
}
