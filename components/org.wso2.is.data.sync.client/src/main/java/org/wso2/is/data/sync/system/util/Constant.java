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

package org.wso2.is.data.sync.system.util;

public class Constant {

    public static final String SCHEMA_TYPE_CARBON = "carbon";
    public static final String SCHEMA_TYPE_REGISTRY = "reg";
    public static final String SCHEMA_TYPE_UM = "um";
    public static final String SCHEMA_TYPE_IDENTITY = "identity";
    public static final String SCHEMA_TYPE_CONSENT = "consent";
    public static final String SCHEMA_TYPE_UMA = "uma";

    public static final String DATA_SOURCE_TYPE_MYSQL = "mysql";
    public static final String DATA_SOURCE_TYPE_H2 = "h2";
    public static final String DATA_SOURCE_TYPE_ORACLE = "oracle";
    public static final String DATA_SOURCE_TYPE_POSGRESQL = "postgresql";
    public static final String DATA_SOURCE_TYPE_MSSQL = "mssql";
    public static final String DATA_SOURCE_TYPE_DB2 = "db2";

    public static final String SQL_STATEMENT_TYPE_SOURCE = "source";
    public static final String SQL_STATEMENT_TYPE_TARGET = "target";

    public static final String TABLE_NAME_SUFFIX_SYNC = "_S";
    public static final String TABLE_NAME_SUFFIX_SYNC_VERSION = "_SV";
    public static final String TRIGGER_NAME_SUFFIX_INSERT = "_IT";
    public static final String TRIGGER_NAME_SUFFIX_UPDATE = "_UT";

    public static final String JDBC_META_DATA_COLUMN_NAME = "COLUMN_NAME";
    public static final String JDBC_META_DATA_TYPE_NAME = "TYPE_NAME";
    public static final String JDBC_META_DATA_COLUMN_SIZE = "COLUMN_SIZE";
    public static final String COLUMN_TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String COLUMN_TYPE_INT = "INT";
    public static final String COLUMN_TYPE_BIGINT = "BIGINT";
    public static final String COLUMN_NAME_SYNC_ID = "SYNC_ID";
    public static final String COLUMN_NAME_MAX_SYNC_ID = "MAX_SYNC_ID";

    public static final String SUPPORTED_START_VERSION_V530 = "5.3.0";
    public static final String SUPPORTED_END_VERSION_V560 = "5.6.0";
    public static final String SUPPORTED_END_VERSION_V570 = "5.7.0";

    public static final String SQL_DELIMITER_H2_MYSQL_MSSQL_POSGRES = "//";
    public static final String SQL_DELIMITER_DB2_ORACLE = "/";

    public static final String ENTRY_FILED_OPERATION_INSERT = "INSERT";
    public static final String ENTRY_FILED_OPERATION_UPDATE = "UPDATE";
    public static final String ENTRY_FILED_OPERATION_DELETE = "DELETE";

    public static final String COLUMN_ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String COLUMN_REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String COLUMN_ACCESS_TOKEN_HASH = "ACCESS_TOKEN_HASH";
    public static final String COLUMN_REFRESH_TOKEN_HASH = "REFRESH_TOKEN_HASH";
}
