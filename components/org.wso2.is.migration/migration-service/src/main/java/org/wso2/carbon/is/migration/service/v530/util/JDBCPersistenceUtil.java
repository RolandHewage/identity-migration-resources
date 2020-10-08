/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.is.migration.service.v530.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPersistenceUtil {

    private static final Logger log = LoggerFactory.getLogger(JDBCPersistenceUtil.class);

    /**
     * Revoke the transaction when catch then sql transaction errors.
     *
     * @param dbConnection database connection.
     */
    public static void rollbackTransaction(Connection dbConnection) {

        try {
            if (dbConnection != null) {
                dbConnection.rollback();
            }
        } catch (SQLException e) {
            log.error("An error occurred while rolling back transactions.", e);
        }
    }

    /**
     * Commit the transaction.
     *
     * @param dbConnection database connection.
     */
    public static void commitTransaction(Connection dbConnection) {

        try {
            if (dbConnection != null) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            log.error("An error occurred while commit transactions.", e);
        }
    }
}
