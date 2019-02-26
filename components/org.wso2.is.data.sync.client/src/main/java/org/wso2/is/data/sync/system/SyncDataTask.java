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
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.DataSyncPipeline;

public class SyncDataTask implements Runnable {

    private DataSyncPipeline dataSyncPipeline;
    private String table;
    private String schema;
    private long syncInterval;
    private boolean active = true;
    private Log log = LogFactory.getLog(SyncDataTask.class);

    public SyncDataTask(DataSyncPipeline dataSyncPipeline, String table, String schema, long syncInterval) {

        this.dataSyncPipeline = dataSyncPipeline;
        this.table = table;
        this.schema = schema;
        this.syncInterval = syncInterval;
    }

    @Override
    public void run() {

        try {
            log.info("Sync task started for table: " + table);
            while (active) {
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
    }

    public void shutdown() {

        log.info("Shutting down sync task for table: " + table);
        dataSyncPipeline.exit();
        this.active = false;
    }
}
