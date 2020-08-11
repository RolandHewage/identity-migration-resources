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
import org.wso2.is.data.sync.system.database.dialect.DDLGenerator;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.DataSyncPipeline;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformerFactory;
import org.wso2.is.data.sync.system.pipeline.transform.v550.AuthorizationCodeDataTransformerV550;
import org.wso2.is.data.sync.system.pipeline.transform.v550.OAuthTokenDataTransformerV550;
import org.wso2.is.data.sync.system.pipeline.transform.v570.AuthorizationCodeDataTransformerV570;
import org.wso2.is.data.sync.system.pipeline.transform.v570.OAuthTokenDataTransformerV570;
import org.wso2.is.data.sync.system.pipeline.transform.v580.AuthorizationCodeDataTransformerV580;
import org.wso2.is.data.sync.system.pipeline.transform.v580.OAuthTokenDataTransformerV580;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * SyncService.
 */
public class SyncService {

    private Configuration configuration;
    private DataSourceManager dataSourceManager;
    private List<DataTransformer> dataTransformers = new ArrayList<>();
    private DDLGenerator ddlGenerator;
    private List<String> syncTables;
    private List<SyncDataTask> syncDataTaskList = new ArrayList<>();

    private Log log = LogFactory.getLog(SyncService.class);

    public SyncService(Properties properties) throws SyncClientException {

        configuration = new Configuration.ConfigurationBuilder().build(properties);
        this.dataSourceManager = new DataSourceManager(configuration);
        initiateDataTransformers();
        syncTables = configuration.getSyncTables();
        this.ddlGenerator = new DDLGenerator(syncTables, dataSourceManager);
    }

    public List<SyncDataTask> getSyncDataTaskList() {

        return syncDataTaskList;
    }

    public void run() throws SyncClientException {

        for (String table : syncTables) {

            String schema = dataSourceManager.getSchema(table);
            DataSource sourceDataSource = dataSourceManager.getSourceDataSource(schema);
            DataSource targetDataSource = dataSourceManager.getTargetDataSource(schema);

            DataTransformerFactory factory = new DataTransformerFactory(dataTransformers);
            PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(configuration, table, schema,
                    sourceDataSource, targetDataSource);
            DataSyncPipeline dataSyncPipeline = new DataSyncPipeline(factory, pipelineConfiguration);
            dataSyncPipeline.build();
            long syncInterval = configuration.getSyncInterval();

            SyncDataTask syncDataTask = new SyncDataTask(dataSyncPipeline, table, schema, syncInterval);
            String threadName = table + "-table-sync-thread";
            Thread thread = new Thread(syncDataTask, threadName);
            thread.start();
            syncDataTaskList.add(syncDataTask);
        }
    }

    private void initiateDataTransformers() {

        dataTransformers.add(new OAuthTokenDataTransformerV550());
        dataTransformers.add(new OAuthTokenDataTransformerV570());
        dataTransformers.add(new OAuthTokenDataTransformerV580());
        dataTransformers.add(new AuthorizationCodeDataTransformerV550());
        dataTransformers.add(new AuthorizationCodeDataTransformerV570());
        dataTransformers.add(new AuthorizationCodeDataTransformerV580());
    }

    public void generateScripts(boolean ddlOnly) throws SyncClientException {

        ddlGenerator.generateScripts(ddlOnly);
    }
}
