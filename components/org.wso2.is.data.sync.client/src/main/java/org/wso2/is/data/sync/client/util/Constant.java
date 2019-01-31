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

package org.wso2.is.data.sync.client.util;

public class Constant {

    public static final String SCHEMA_TYPE_CARBON = "carbon";
    public static final String SCHEMA_TYPE_REGISTRY = "reg";
    public static final String SCHEMA_TYPE_UM = "um";
    public static final String SCHEMA_TYPE_IDENTITY = "identity";
    public static final String SCHEMA_TYPE_CONSENT = "consent";
    public static final String SCHEMA_TYPE_UMA = "uma";

    public static final String SQL_STATEMENT_TYPE_SOURCE = "source";
    public static final String SQL_STATEMENT_TYPE_TARGET = "target";

    public static final String TABLE_NAME_SUFFIX_SYNC = "_S";
    public static final String TABLE_NAME_SUFFIX_SYNC_VERSION = "_SV";
    public static final String TRIGGER_NAME_SUFFIX_INSERT = "_IT";
    public static final String TRIGGER_NAME_SUFFIX_UPDATE = "_UT";

    public static final String SUPPORTED_START_VERSION_V530 = "5.3.0";
    public static final String SUPPORTED_END_VERSION_V560 = "5.6.0";
    public static final String SUPPORTED_END_VERSION_V570 = "5.7.0";

    public static final String SQL_DELIMITER_H2_MYSQL_MSSQL_POSGRES = "//";
    public static final String SQL_DELIMITER_DB2_ORACLE = "/";

    public static final String SYSTEM_PROPERTY_GENERATE_DDL = "generateDDL";
}
