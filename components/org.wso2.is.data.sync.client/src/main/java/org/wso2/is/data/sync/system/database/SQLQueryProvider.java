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

package org.wso2.is.data.sync.system.database;

import java.util.HashMap;
import java.util.Map;

/**
 * SQLQueryProvider.
 */
public class SQLQueryProvider {

    public static final String SQL_TEMPLATE_SELECT_SYNC_ID_KEY = "SQL_TEMPLATE_SELECT_SYNC_ID";
    public static final String SQL_TEMPLATE_SELECT_SYNC_ID = "SELECT SYNC_ID FROM %s";

    public static final String SQL_TEMPLATE_SELECT_MAX_SYNC_ID_KEY = "SQL_TEMPLATE_SELECT_MAX_SYNC_ID";
    public static final String SQL_TEMPLATE_SELECT_MAX_SYNC_ID = "SELECT MAX(SYNC_ID) FROM %s";

    public static final String SQL_TEMPLATE_INSERT_SYNC_ID_KEY = "SQL_TEMPLATE_INSERT_SYNC_ID_KEY";
    public static final String SQL_TEMPLATE_INSERT_SYNC_ID = "INSERT INTO %s (SYNC_ID) VALUES (?)";

    public static final String SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY =
            "SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL";
    public static final String SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL = "SELECT SYNC_ID, %s, ACTION " +
            "FROM %s WHERE SYNC_ID > ? AND SYNC_ID < " +
            "? ORDER BY SYNC_ID ASC";

    public static final String SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY = "SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY";
    public static final String SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY = "SELECT %s FROM %s WHERE %s";

    public static final String SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY = "SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY";
    public static final String SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY = "UPDATE %s SET %s WHERE %s";

    public static final String SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY_KEY = "SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY";
    public static final String SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY = "DELETE FROM %s WHERE %s";

    public static final String SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY = "SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY";
    public static final String SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY = "INSERT INTO %s (%s) VALUES (%s)";

    public static final String SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY = "SQL_TEMPLATE_UPDATE_SYNC_VERSION";
    public static final String SQL_TEMPLATE_UPDATE_SYNC_VERSION = "UPDATE %s SET SYNC_ID = ?";

    public static final String SQL_TEMPLATE_DROP_TABLE_MYSQL_KEY = "SQL_TEMPLATE_DROP_TABLE_MYSQL";
    public static final String SQL_TEMPLATE_DROP_TABLE_MYSQL = "DROP TABLE IF EXISTS %s";

    public static final String SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL_KEY = "SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL";
    public static final String SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL = "CREATE TABLE %s (SYNC_ID INT NOT NULL " +
            "AUTO_INCREMENT, %s, PRIMARY KEY (SYNC_ID))";

    public static final String SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL_KEY =
            "SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL";
    public static final String SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL = "CREATE TABLE IF NOT EXISTS %s " +
            "(SYNC_ID INT)";

    public static final String SQL_TEMPLATE_CREATE_TRIGGER_MYSQL_KEY = "SQL_TEMPLATE_CREATE_TRIGGER_MYSQL";
    public static final String SQL_TEMPLATE_CREATE_TRIGGER_MYSQL = "CREATE TRIGGER %s %s %s ON %s %s " +
            "BEGIN INSERT INTO %s (%s) VALUES (%s); END";
    public static final String SQL_TEMPLATE_CREATE_TRIGGER_POSTGRES = "CREATE TRIGGER %s %s %s ON %s %s " +
            "EXECUTE PROCEDURE %s()";

    public static final String SQL_TEMPLATE_CREATE_FUNCTION_POSTGRES = "CREATE OR REPLACE FUNCTION %s() " +
            "RETURNS TRIGGER AS $%s$" +
            "   BEGIN" +
            "      INSERT INTO %s (%s) VALUES (%s);" +
            "      RETURN NEW;" +
            "   END;" +
            "$%s$ LANGUAGE plpgsql;";

    public static final String SQL_TEMPLATE_DROP_TRIGGER_MYSQL_KEY = "SQL_TEMPLATE_DROP_TRIGGER_MYSQL";
    public static final String SQL_TEMPLATE_DROP_TRIGGER_MYSQL = "DROP TRIGGER IF EXISTS %s";
    public static final String SQL_TEMPLATE_DROP_TRIGGER_POSTGRES = "DROP TRIGGER IF EXISTS %s ON %s";
    public static final String SQL_TEMPLATE_DROP_TRIGGER_ORACLE = "DROP TRIGGER %s";
    public static final String SQL_TEMPLATE_SELECT_SOURCE_IDP_ID = "SELECT ID FROM IDP INNER JOIN %s " +
            "ON %s.TENANT_ID =  IDP.TENANT_ID AND IDP.NAME = 'LOCAL' LIMIT 1";

    private static Map<String, String> queryHolder = new HashMap<>();

    static {

        queryHolder.put(SQL_TEMPLATE_SELECT_SYNC_ID_KEY, SQL_TEMPLATE_SELECT_SYNC_ID);
        queryHolder.put(SQL_TEMPLATE_SELECT_MAX_SYNC_ID_KEY, SQL_TEMPLATE_SELECT_MAX_SYNC_ID);
        queryHolder.put(SQL_TEMPLATE_INSERT_SYNC_ID_KEY, SQL_TEMPLATE_INSERT_SYNC_ID);
        queryHolder.put(SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL_KEY, SQL_TEMPLATE_SELECT_SOURCE_SYNC_DATA_MYSQL);
        queryHolder.put(SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY_KEY, SQL_TEMPLATE_SELECT_TARGET_SYNC_ENTRY);
        queryHolder.put(SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY_KEY, SQL_TEMPLATE_INSERT_TARGET_SYNC_ENTRY);
        queryHolder.put(SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY, SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY);
        queryHolder.put(SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY_KEY, SQL_TEMPLATE_DELETE_TARGET_SYNC_ENTRY);
        queryHolder.put(SQL_TEMPLATE_UPDATE_SYNC_VERSION_KEY, SQL_TEMPLATE_UPDATE_SYNC_VERSION);
        queryHolder.put(SQL_TEMPLATE_DROP_TABLE_MYSQL_KEY, SQL_TEMPLATE_DROP_TABLE_MYSQL);
        queryHolder.put(SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL_KEY, SQL_TEMPLATE_CREATE_SYNC_TABLE_MYSQL);
        queryHolder.put(SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL_KEY, SQL_TEMPLATE_CREATE_SYNC_VERSION_TABLE_MYSQL);
        queryHolder.put(SQL_TEMPLATE_CREATE_TRIGGER_MYSQL_KEY, SQL_TEMPLATE_CREATE_TRIGGER_MYSQL);
        queryHolder.put(SQL_TEMPLATE_DROP_TRIGGER_MYSQL_KEY, SQL_TEMPLATE_DROP_TRIGGER_MYSQL);
    }

    public static String getQuery(String queryKey) {

        return queryHolder.get(queryKey);
    }
}
