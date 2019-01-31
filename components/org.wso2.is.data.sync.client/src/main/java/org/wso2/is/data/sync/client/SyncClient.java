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

import org.wso2.is.data.sync.client.datasource.SQLStatement;
import org.wso2.is.data.sync.client.exception.SyncClientException;

import java.util.List;

public interface SyncClient {

    /**
     * Returns starting product version for data syncing. This is the product version which the migration starts
     * from.
     * @return Source product version of the data syncing.
     */
    String getSyncSourceVersion();

    /**
     * Returns end product version for data syncing. This is the product version which the migration ends
     * in.
     * @return Target version of the data syncing.
     */
    String getSyncTargetVersion();

    /**
     * Sync data for the given table.
     *
     * @param table Name of the table to sync data.
     * @throws SyncClientException If error occur while data sync.
     */
    void syncData(String table) throws SyncClientException;

    /**
     * Generate DDLs for data syncing. If the ddlOnly flag is set to true, the generated SQLs will return the
     * generated statements and not execute them on the source or target database.
     *
     * @param tableName Table name.
     * @return List of SQL statements required for data syncing.
     * @throws SyncClientException If error occurs while generating data sync DDLs.
     */
    List<SQLStatement> generateSyncScripts(String tableName) throws SyncClientException;

    /**
     * Checks whether this client can syn data for the given table.
     *
     * @param tableName Table name.
     * @return True if the client can sync data. False otherwise.
     * @throws SyncClientException If error occurs while checking the sync data support.
     */
    boolean canSyncData(String tableName) throws SyncClientException;

    /**
     * Get schema which the table belongs to.
     * @param tableName Table name.
     * @return Schema name
     * @throws SyncClientException If error occurs while retrieving schema.
     */
    String getSchema(String tableName) throws SyncClientException;
}
