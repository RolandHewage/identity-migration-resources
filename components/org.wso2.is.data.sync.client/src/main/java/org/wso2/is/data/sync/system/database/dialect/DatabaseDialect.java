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

package org.wso2.is.data.sync.system.database.dialect;

import org.wso2.is.data.sync.system.exception.SyncClientException;

import java.util.List;

/**
 * Interface for database dialects. SQL statements relate to database operations are exposed through this.
 * To perform a given operation, different database flavors may use different type of SQL statements. This interface
 * can be used to provide implementations of different database flavors which provide database flavor specific SQL
 * statements.
 */
public interface DatabaseDialect {

    /**
     * Generate SQL statements for creating a trigger.
     *
     * @param trigger Trigger model containing trigger information.
     * @return List of SQL statements related to trigger creation.
     * @throws SyncClientException If error occurs while generating SQL statements.
     */
    List<String> generateCreateTrigger(Trigger trigger) throws SyncClientException;

    /**
     * Generate SQL statements for creating a table.
     *
     * @param table Table model containing table information.
     * @return List of SQL statements related to table creation.
     * @throws SyncClientException If error occurs while generating SQL statements.
     */
    List<String> generateCreateTable(Table table) throws SyncClientException;

    /**
     * Generate SQL statements for deleting a trigger.
     *
     * @param name Name of the trigger to be dropped.
     * @return List of SQL statements related to trigger deletion.
     * @throws SyncClientException If error occurs while generating SQL statements.
     * @deprecated Use generateDropTrigger(String triggerName, String targetTableName).
     */
    @Deprecated
    List<String> generateDropTrigger(String name) throws SyncClientException;

    /**
     * Generate SQL statements for deleting a trigger.
     *
     * @param triggerName     Name of the trigger to be dropped.
     * @param targetTableName Name of the target table which the trigger should be created on.
     * @return List of SQL statements related to trigger deletion.
     * @throws SyncClientException If error occurs while generating SQL statements.
     */
    default List<String> generateDropTrigger(String triggerName, String targetTableName) throws SyncClientException {

        return generateDropTrigger(triggerName);
    }

    /**
     * Generate SQL statements for deleting a table.
     *
     * @param name Name of the table to be dropped.
     * @return List of SQL statements related to table deletion.
     * @throws SyncClientException If error occurs while generating SQL statements.
     */
    List<String> generateDropTable(String name) throws SyncClientException;
}
