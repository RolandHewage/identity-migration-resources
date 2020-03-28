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

/**
 * Constant.
 */
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
    public static final String DATA_SOURCE_TYPE_POSTGRESQL = "postgresql";
    public static final String DATA_SOURCE_TYPE_MSSQL = "mssql";
    public static final String DATA_SOURCE_TYPE_DB2 = "db2";

    public static final String SQL_STATEMENT_TYPE_SOURCE = "source";
    public static final String SQL_STATEMENT_TYPE_TARGET = "target";

    public static final String TABLE_NAME_SUFFIX_SYNC = "_S";
    public static final String TABLE_NAME_SUFFIX_SYNC_VERSION = "_SV";
    public static final String TRIGGER_NAME_SUFFIX_INSERT = "_IT";
    public static final String TRIGGER_NAME_SUFFIX_UPDATE = "_UT";
    public static final String TRIGGER_NAME_SUFFIX_DELETE = "_DT";
    public static final String TRIGGER_TIMING_AFTER = "AFTER";
    public static final String TRIGGER_TIMING_BEFORE = "BEFORE";
    public static final String SELECTION_POLICY_FOR_EACH_ROW = "FOR EACH ROW";

    public static final String JDBC_META_DATA_COLUMN_NAME = "COLUMN_NAME";
    public static final String JDBC_META_DATA_TYPE_NAME = "TYPE_NAME";
    public static final String JDBC_META_DATA_COLUMN_DEF = "COLUMN_DEF";
    public static final String JDBC_META_DATA_COLUMN_SIZE = "COLUMN_SIZE";
    public static final String COLUMN_TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String COLUMN_TYPE_TIMESTAMP_WITHOUT_TIME_ZONE = "TIMESTAMP WITHOUT TIME ZONE";
    public static final String COLUMN_TYPE_CHAR = "CHAR";
    public static final String COLUMN_TYPE_VARCHAR = "VARCHAR";
    public static final String COLUMN_TYPE_BLOB = "BLOB";
    public static final String COLUMN_TYPE_INT = "INT";
    public static final String COLUMN_TYPE_INT4 = "INT4";
    public static final String COLUMN_TYPE_INT8 = "INT8";
    public static final String COLUMN_TYPE_BIGINT = "BIGINT";
    public static final String COLUMN_TYPE_SERIAL = "SERIAL";
    public static final String COLUMN_ATTRIBUTE_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COLUMN_NAME_SYNC_ID = "SYNC_ID";
    public static final String COLUMN_NAME_ACTION = "ACTION";

    public static final String TABLE_ATTRIBUTE_PRIMARY_KEY = "PRIMARY KEY";

    public static final String PRODUCT_VERSION_V530 = "5.3.0";
    public static final String PRODUCT_VERSION_V560 = "5.6.0";
    public static final String PRODUCT_VERSION_V570 = "5.7.0";

    public static final String SQL_DELIMITER_H2_MYSQL_MSSQL_POSGRES = "//";
    public static final String SQL_DELIMITER_DB2_ORACLE = "/";

    public static final String ENTRY_FILED_ACTION_INSERT = "INSERT";
    public static final String ENTRY_FILED_ACTION_UPDATE = "UPDATE";
    public static final String ENTRY_FILED_ACTION_DELETE = "DELETE";

    public static final String SYNC_OPERATION_INSERT = "INSERT";
    public static final String SYNC_OPERATION_UPDATE = "UPDATE";
    public static final String SYNC_OPERATION_DELETE = "DELETE";

    public static final String COLUMN_ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String COLUMN_REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String COLUMN_ACCESS_TOKEN_HASH = "ACCESS_TOKEN_HASH";
    public static final String COLUMN_REFRESH_TOKEN_HASH = "REFRESH_TOKEN_HASH";

    public static final String COLUMN_AUTHORIZATION_CODE = "AUTHORIZATION_CODE";
    public static final String COLUMN_AUTHORIZATION_CODE_HASH = "AUTHORIZATION_CODE_HASH";
    public static final String COLUMN_USER_DOMAIN = "USER_DOMAIN";
    public static final String COLUMN_IDP_ID = "IDP_ID";

    public static final String TABLE_IDN_OAUTH2_AUTHORIZATION_CODE = "IDN_OAUTH2_AUTHORIZATION_CODE";
    public static final String TABLE_IDN_OAUTH2_ACCESS_TOKEN = "IDN_OAUTH2_ACCESS_TOKEN";

    public static final String COLUMN_UM_USER_ID = "UM_USER_ID";

    public static final String PROPERTY_NAME_HASH = "hash";
    public static final String PROPERTY_NAME_ALGORITHM = "algorithm";

    public static final String JVM_PROPERTY_CONFIG_FILE_PATH = "configFile";
    public static final String JVM_PROPERTY_PREPARE_SYNC = "prepareSync";
    public static final String JVM_PROPERTY_GENERATE_DDL = "generateDDL";
    public static final String JVM_PROPERTY_SYNC_DATA = "syncData";
    public static final String JVM_PROPERTY_SOURCE_VERSION = "sourceVersion";
    public static final String JVM_PROPERTY_TARGET_VERSION = "targetVersion";
    public static final String JVM_PROPERTY_SYNC_TABLES = "syncTables";
    public static final String JVM_PROPERTY_UM_SCHEMA = "umSchema";
    public static final String JVM_PROPERTY_REG_SCHEMA = "regSchema";
    public static final String JVM_PROPERTY_IDENTITY_SCHEMA = "identitySchema";
    public static final String JVM_PROPERTY_CONSENT_SCHEMA = "consentSchema";
    public static final String JVM_PROPERTY_SYNC_INTERVAL = "syncInterval";
    public static final String JVM_PROPERTY_BATCH_SIZE = "batchSize";

    public static final long DEFAULT_SYNC_INTERVAL = 5000;
    public static final int DEFAULT_BATCH_SIZE = 100;

    public static final String DELIMITER = "DELIMITER";
    public static final String DELIMITER_DOUBLE_SLASH = "//";
    public static final String DELIMITER_COMMA = ";";
    public static final String DBSCRIPTS_LOCATION = "dbscripts";
    public static final String SYNC_TOOL_SCRIPT_LOCATION = "sync-tool";
    public static final String SQL_FILE_EXTENSION = ".sql";
    public static final String POSTGRESQL_PRODUCT_NAME = "PostgreSQL";
    public static final String FOREIGN_KEY_VIOLATION_ERROR_CODE_POSTGRESQL = "23503";
    public static final String FEDERATED = "FEDERATED";
}
