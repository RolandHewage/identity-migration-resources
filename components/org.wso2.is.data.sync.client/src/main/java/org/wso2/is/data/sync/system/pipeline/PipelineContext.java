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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * PipelineContext.
 */
public class PipelineContext {

    private Connection sourceConnection;
    private Connection targetConnection;
    private PipelineConfiguration pipelineConfiguration;
    private Map<String, Object> properties = new HashMap<>();

    public PipelineContext(Connection sourceConnection, Connection targetConnection, PipelineConfiguration
            pipelineConfiguration) {

        this.sourceConnection = sourceConnection;
        this.targetConnection = targetConnection;
        this.pipelineConfiguration = pipelineConfiguration;
    }

    public Map<String, Object> getProperties() {

        return properties;
    }

    public void setProperties(Map<String, Object> properties) {

        this.properties = properties;
    }

    public Object getProperty(String key) {

        return properties.get(key);
    }

    public void addProperty(String key, Object value) {

        properties.put(key, value);
    }

    public Connection getSourceConnection() {

        return sourceConnection;
    }

    public Connection getTargetConnection() {

        return targetConnection;
    }

    public PipelineConfiguration getPipelineConfiguration() {

        return pipelineConfiguration;
    }
}
