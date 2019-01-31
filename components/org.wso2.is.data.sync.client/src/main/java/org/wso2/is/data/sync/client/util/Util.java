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

import org.apache.commons.lang.StringUtils;

import static org.wso2.is.data.sync.client.util.Constant.TABLE_NAME_SUFFIX_SYNC;
import static org.wso2.is.data.sync.client.util.Constant.TABLE_NAME_SUFFIX_SYNC_VERSION;
import static org.wso2.is.data.sync.client.util.Constant.TRIGGER_NAME_SUFFIX_INSERT;
import static org.wso2.is.data.sync.client.util.Constant.TRIGGER_NAME_SUFFIX_UPDATE;

public class Util {

    private Util() {

    }

    public static String getSyncTableName(String tableName) {

        return getFormattedName(tableName, TABLE_NAME_SUFFIX_SYNC);
    }

    public static String getSyncVersionTableName(String tableName) {

        return getFormattedName(tableName, TABLE_NAME_SUFFIX_SYNC_VERSION);
    }

    public static String getInsertTriggerName(String tableName) {

        return getFormattedName(tableName, TRIGGER_NAME_SUFFIX_INSERT);
    }

    public static String getUpdateTriggerName(String tableName) {

        return getFormattedName(tableName, TRIGGER_NAME_SUFFIX_UPDATE);
    }

    public static String getScripId(String scheme, String type) {

        return String.join("_", scheme, type);
    }

    public static boolean isGenerateDDL() {

        String executeScriptProperty = System.getProperty(Constant.SYSTEM_PROPERTY_GENERATE_DDL);
        return executeScriptProperty != null;
    }

    private static String getFormattedName(String tableName, String suffix) {

        String formattedName = tableName;
        if (StringUtils.isNotBlank(tableName)) {
            if (formattedName.length() + suffix.length() >= 30) {
                formattedName = tableName.substring(0, 30 - suffix.length()) + suffix;
            } else {
                formattedName = tableName + suffix;
            }
        }
        return formattedName;
    }


}
