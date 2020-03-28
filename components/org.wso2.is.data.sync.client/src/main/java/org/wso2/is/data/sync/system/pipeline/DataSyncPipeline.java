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
import org.wso2.is.data.sync.system.config.Configuration;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.persist.Persistor;
import org.wso2.is.data.sync.system.pipeline.process.BatchProcessor;
import org.wso2.is.data.sync.system.pipeline.result.ResultHandler;
import org.wso2.is.data.sync.system.pipeline.result.TransactionResult;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Represent the model for data sync related operation. The modeling is done a four part pipeline.
 * 1. Batch processing of data to be synced.
 * 2. Data transformation of the processed data batch.
 * 3. Persisting the transformed data.
 * 4. Validating the data persistence results.
 * Once all the steps of the pipeline is completed and the data persistence results are successful, the pipeline will
 * commit the transaction for the processed batch.
 */
public class DataSyncPipeline {

    private static final Log log = LogFactory.getLog(DataSyncPipeline.class);
    private Persistor persistor;
    private DataTransformer dataTransformer;
    private BatchProcessor batchProcessor;
    private ResultHandler resultHandler;
    private DataTransformerFactory dataTransformerFactory;
    private PipelineConfiguration pipelineConfiguration;
    private boolean active = true;

    public DataSyncPipeline(DataTransformerFactory dataTransformerFactory, PipelineConfiguration
            pipelineConfiguration) {

        this.dataTransformerFactory = dataTransformerFactory;
        this.pipelineConfiguration = pipelineConfiguration;
    }

    /**
     * Processes a journal entries from the source and sync them to the target in batches.
     *
     * @throws SyncClientException If an error occurs while syncing data.
     */
    public void process() throws SyncClientException {

        boolean complete;
        do {
            complete = processBatch();
            if (!complete) {
                if (log.isDebugEnabled()) {
                    log.debug("Batch processing for table: " + pipelineConfiguration.getTableName() + " is not " +
                            "completed. Trying next batch.");
                }
            }
        } while (!complete && active);

    }

    /**
     * Processes a batch of journal entries from the source and sync them to the target.
     *
     * @return True if there are no more data available in the journal to be synced.
     * @throws SyncClientException If an error occurs while syncing data.
     */
    public boolean processBatch() throws SyncClientException {

        String schema = pipelineConfiguration.getSchema();
        Connection sourceConnection = null;
        Connection targetConnection = null;
        try {
            try {
                sourceConnection = pipelineConfiguration.getSourceDataSource().getConnection();
                sourceConnection.setAutoCommit(false);
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

            List<JournalEntry> journalEntryBatch = batchProcessor.pollJournal(context);
            List<JournalEntry> transformedJournalEntryBatch = dataTransformer.transform(journalEntryBatch, context);
            List<TransactionResult> transactionResults = persistor.persist(transformedJournalEntryBatch, context);
            boolean batchProcessingSuccess = resultHandler.processResults(transactionResults, context);

            if (batchProcessingSuccess) {
                try {
                    targetConnection.commit();
                    sourceConnection.commit();
                } catch (SQLException e) {
                    batchProcessingSuccess = false;
                    log.error("Error while committing sync transaction on table: " + pipelineConfiguration
                            .getTableName(), e);
                }
            } else {
                try {
                    targetConnection.rollback();
                    sourceConnection.rollback();
                } catch (SQLException e) {
                    log.error("Error while rolling back sync transaction on table: " + pipelineConfiguration
                            .getTableName(), e);
                }
            }
            int batchSize = context.getPipelineConfiguration().getConfiguration().getBatchSize();

            if (!batchProcessingSuccess) {
                // Processing the batch failed. Hence completing this iteration and retry in the next iteration.
                return true;
            } else if (transactionResults.size() < batchSize) {
                // Number of processed records are smaller than the batch size. Since there are no more entries to
                // process completing this iteration.
                return true;
            } else {
                // Continue to process the next batch.
                return false;
            }
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
    }

    /**
     * Builds the data sync pipeline for a given table.
     */
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
