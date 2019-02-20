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

package org.wso2.is.data.sync.system;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.config.Configuration;
import org.wso2.is.data.sync.system.database.DataSourceManager;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.DataSyncPipeline;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformerFactory;
import org.wso2.is.data.sync.system.pipeline.transform.FooTable540Transformer;
import org.wso2.is.data.sync.system.pipeline.transform.FooTable550Transformer;
import org.wso2.is.data.sync.system.pipeline.transform.v550.OAuthTokenDataTransformerV550;
import org.wso2.is.data.sync.system.pipeline.transform.v570.OAuthTokenDataTransformerV570;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

public class SyncService {

    private Configuration configuration;
    private DataSourceManager dataSourceManager;
    private List<String> syncTableList = new ArrayList<>();
    private List<DataTransformer> dataTransformers = new ArrayList<>();
    private Log log = LogFactory.getLog(SyncService.class);

    public SyncService() throws SyncClientException {

        configuration = new Configuration("5.3.0", "5.7.0");
        configuration.setBatchSize(5);

        String syncTables = System.getProperty("syncTables");
        if (syncTables != null) {
            List<String> tables = Arrays.asList(syncTables.split(","));
            if (tables.size() > 0) {
                syncTableList.addAll(tables);
            }
        }

        this.dataSourceManager = new DataSourceManager();
        initiateDataTransformers();
    }

    public void run() throws SyncClientException {

        for (String table : syncTableList) {

            String schema = dataSourceManager.getSchema(table);
            DataSource sourceDataSource = dataSourceManager.getSourceDataSource(schema);
            DataSource targetDataSource = dataSourceManager.getTargetDataSource(schema);

            DataTransformerFactory factory = new DataTransformerFactory(dataTransformers);
            PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(configuration, table, schema,
                                                                                    sourceDataSource, targetDataSource);
            DataSyncPipeline dataSyncPipeline = new DataSyncPipeline(factory, pipelineConfiguration);
            dataSyncPipeline.build();
            int syncInterval = configuration.getSyncInterval();

            Thread thread = new Thread(() -> {
                try {
                    while (true) {
                        boolean complete = dataSyncPipeline.processBatch();
                        if (complete) {
                            try {
                                log.info("Batch processing for table: " + table + " completed. Sleeping the thread " +
                                         "for: " + syncInterval + "ms.");
                                Thread.sleep(syncInterval);
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Error occurred while attempting to sleep the thread: " +
                                                           Thread.currentThread().getName());
                            }
                        }
                    }
                } catch (SyncClientException e) {
                    throw new RuntimeException("Error occurred while data syncing on table: " + table + ", schema: "
                                               + schema);
                }
            });
            thread.setName(table + "-table-sync-thread");
            thread.start();
        }
    }

    private void initiateDataTransformers() {

        dataTransformers.add(new FooTable550Transformer());
        dataTransformers.add(new FooTable540Transformer());
        dataTransformers.add(new OAuthTokenDataTransformerV550());
        dataTransformers.add(new OAuthTokenDataTransformerV570());
    }
}
