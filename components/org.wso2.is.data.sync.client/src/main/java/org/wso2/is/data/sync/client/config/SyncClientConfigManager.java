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

package org.wso2.is.data.sync.client.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncClientConfigManager {

    private List<String> syncTableList = new ArrayList<>();
    private String startVersion;
    private String endVersion;
    private String scriptPath = "";
    private int batchSize = 100;
    private long syncIntervalInMillis = 1000;

    public List<String> getSyncTableList() {


        String syncTables = System.getProperty("syncTables");

        if (syncTables != null) {
            List<String> tables = Arrays.asList(syncTables.split(","));
            if (tables != null && tables.size() > 0) {
                syncTableList.addAll(tables);
            }
        }

//        syncTableList.add("IDN_OAUTH2_ACCESS_TOKEN_SCOPE");
        return syncTableList;
    }

    public void setSyncTableList(List<String> syncTableList) {
        this.syncTableList = syncTableList;
    }

    public String getStartVersion() {

        return startVersion;
    }

    public String getEndVersion() {

        return endVersion;
    }


    public String getScriptPath() {
        return scriptPath;
    }

    public int getBatchSize() {

        return batchSize;
    }

    public long getSyncIntervalInMillis() {

        return syncIntervalInMillis;
    }
}
