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

package org.wso2.is.data.sync.system.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.process.BatchProcessor;
import org.wso2.is.data.sync.system.pipeline.persist.Persistor;
import org.wso2.is.data.sync.system.pipeline.result.ResultHandler;
import org.wso2.is.data.sync.system.pipeline.result.TransactionResult;
import org.wso2.is.data.sync.system.config.Configuration;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DataSyncPipeline {

    private Persistor persistor;
    private DataTransformer dataTransformer;
    private BatchProcessor batchProcessor;
    private ResultHandler resultHandler;
    private DataTransformerFactory dataTransformerFactory;
    private PipelineConfiguration pipelineConfiguration;
    private Log log = LogFactory.getLog(DataSyncPipeline.class);
    private boolean active = true;


    public DataSyncPipeline(DataTransformerFactory dataTransformerFactory, PipelineConfiguration
            pipelineConfiguration) {

        this.dataTransformerFactory = dataTransformerFactory;
        this.pipelineConfiguration = pipelineConfiguration;
    }

    public boolean processBatch() throws SyncClientException {

        boolean complete;
        do {

            String schema = pipelineConfiguration.getSchema();
            Connection sourceConnection = null;
            Connection targetConnection = null;
            try {
                try {
                    sourceConnection = pipelineConfiguration.getSourceDataSource().getConnection();
                } catch (SQLException e) {
                    throw new SyncClientException("Error while creating source connection from data source for schema: "
                                                  + schema);
                }
                try {
                    targetConnection = pipelineConfiguration.getTargetDataSource().getConnection();
                    targetConnection.setAutoCommit(false);
                } catch (SQLException e) {
                    throw new SyncClientException("Error while creating target connection from data source for schema: "
                                                  + schema);
                }
                PipelineContext context = new PipelineContext(sourceConnection, targetConnection,
                                                              pipelineConfiguration);

                List<JournalEntry> journalEntryList = batchProcessor.pollJournal(context);
                List<JournalEntry> transform = dataTransformer.transform(journalEntryList, context);
                List<TransactionResult> transactionResults = persistor.persist(transform, context);
                boolean batchComplete = resultHandler.processResults(transactionResults, context);

                if (batchComplete) {
                    try {
                        targetConnection.commit();
                        sourceConnection.commit();
                    } catch (SQLException e) {
                        batchComplete = false;
                        log.error("Error while committing sync transaction on table: " + pipelineConfiguration
                                .getTableName(), e);
                    }
                } else {
                    try {
                        targetConnection.rollback();
                        sourceConnection.rollback();
                    } catch (SQLException e) {
                        batchComplete = false;
                        log.error("Error while rolling back sync transaction on table: " + pipelineConfiguration
                                .getTableName(), e);
                    }
                }
                complete = batchComplete && transactionResults.isEmpty();
            } finally {
                try {
                    if (sourceConnection != null) {
                        sourceConnection.close();
                    }
                    if (targetConnection != null) {
                        targetConnection.close();
                    }
                } catch (SQLException e) {
                    log.error("Error while closing connection of schema: " + schema, e);
                }
            }
        } while (!complete && active);
        return true;
    }

    public void build() {

        persistor = new Persistor();
        Configuration configuration = pipelineConfiguration.getConfiguration();
        dataTransformer = dataTransformerFactory.buildTransformer(pipelineConfiguration.getTableName(),
                                                                  configuration.getSourceVersion(),
                                                                  configuration.getTargetVersion());
        batchProcessor = new BatchProcessor();
        resultHandler = new ResultHandler();
    }

    public void exit() {

        this.active = false;
    }
}
