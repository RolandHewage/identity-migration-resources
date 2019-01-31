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

package org.wso2.is.data.sync.client.impl.oauth;

import org.wso2.is.data.sync.client.DefaultSyncClient;
import org.wso2.is.data.sync.client.exception.SyncClientException;
import org.wso2.is.data.sync.client.util.Constant;

import java.util.Collections;
import java.util.List;

import static org.wso2.is.data.sync.client.util.Constant.SUPPORTED_END_VERSION_V560;
import static org.wso2.is.data.sync.client.util.Constant.SUPPORTED_START_VERSION_V530;

public class OAuthV530V560SyncClient extends DefaultSyncClient {

    private static final List<String> SUPPORTED_TABLES = Collections.singletonList("IDN_OAUTH2_ACCESS_TOKEN");

    @Override
    public boolean canSyncData(String tableName) throws SyncClientException {

        if (SUPPORTED_START_VERSION_V530.equals(getSyncSourceVersion()) && SUPPORTED_END_VERSION_V560.equals
                (getSyncTargetVersion()) && SUPPORTED_TABLES.contains(tableName)) {
            return true;
        }
        return false;
    }

    @Override
    public void syncData(String tableName) throws SyncClientException {

        //TODO: Data transformation goes here.
    }

    @Override
    public String getSchema(String tableName) throws SyncClientException {

        return Constant.SCHEMA_TYPE_IDENTITY;
    }
}
