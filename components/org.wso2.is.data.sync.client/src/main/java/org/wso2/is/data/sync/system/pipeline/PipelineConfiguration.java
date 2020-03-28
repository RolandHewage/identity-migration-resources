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

import org.wso2.is.data.sync.system.config.Configuration;

import javax.sql.DataSource;

/**
 * PipelineConfiguration.
 */
public class PipelineConfiguration {

    private Configuration configuration;
    private String tableName;
    private String schema;
    private DataSource sourceDataSource;
    private DataSource targetDataSource;

    public PipelineConfiguration(Configuration configuration, String tableName, String schema,
                                 DataSource sourceDataSource, DataSource targetDataSource) {

        this.configuration = configuration;
        this.tableName = tableName;
        this.schema = schema;
        this.sourceDataSource = sourceDataSource;
        this.targetDataSource = targetDataSource;
    }

    public Configuration getConfiguration() {

        return configuration;
    }

    public void setConfiguration(Configuration configuration) {

        this.configuration = configuration;
    }

    public String getTableName() {

        return tableName;
    }

    public void setTableName(String tableName) {

        this.tableName = tableName;
    }

    public String getSchema() {

        return schema;
    }

    public void setSchema(String schema) {

        this.schema = schema;
    }

    public DataSource getSourceDataSource() {

        return sourceDataSource;
    }

    public DataSource getTargetDataSource() {

        return targetDataSource;
    }
}
