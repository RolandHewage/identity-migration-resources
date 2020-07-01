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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.EntryField;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringJoiner;
import java.util.TimeZone;

import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_BIGINT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_BLOB;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_CHAR;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_INT;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_TIMESTAMP;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_TYPE_VARCHAR;
import static org.wso2.is.data.sync.system.util.Constant.JDBC_META_DATA_COLUMN_DEF;
import static org.wso2.is.data.sync.system.util.Constant.JDBC_META_DATA_COLUMN_NAME;
import static org.wso2.is.data.sync.system.util.Constant.JDBC_META_DATA_COLUMN_SIZE;
import static org.wso2.is.data.sync.system.util.Constant.JDBC_META_DATA_TYPE_NAME;
import static org.wso2.is.data.sync.system.util.Constant.POSTGRESQL_PRODUCT_NAME;
import static org.wso2.is.data.sync.system.util.Constant.TABLE_NAME_SUFFIX_SYNC;
import static org.wso2.is.data.sync.system.util.Constant.TABLE_NAME_SUFFIX_SYNC_VERSION;
import static org.wso2.is.data.sync.system.util.Constant.TRIGGER_NAME_SUFFIX_DELETE;
import static org.wso2.is.data.sync.system.util.Constant.TRIGGER_NAME_SUFFIX_INSERT;
import static org.wso2.is.data.sync.system.util.Constant.TRIGGER_NAME_SUFFIX_UPDATE;

/**
 * CommonUtil.
 */
public class CommonUtil {

    private static Log log = LogFactory.getLog(CommonUtil.class);

    private CommonUtil() {

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

    public static String getDeleteTriggerName(String tableName) {

        return getFormattedName(tableName, TRIGGER_NAME_SUFFIX_DELETE);
    }

    public static String getScripId(String scheme, String type) {

        return String.join("_", scheme, type);
    }

    public static String generateColumnList(List<ColumnData> columnData) {

        StringJoiner columnJoiner = new StringJoiner(", ");

        for (ColumnData columnEntry : columnData) {
            columnJoiner.add(getColumnEntryString(columnEntry));
        }
        return columnJoiner.toString();
    }

    private static String getColumnEntryString(ColumnData columnEntry) {

        String columnEntryString;
        if (COLUMN_TYPE_TIMESTAMP.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_INT.equalsIgnoreCase(columnEntry.getType()) ||
                COLUMN_TYPE_BIGINT.equalsIgnoreCase(columnEntry.getType())) {
            columnEntryString = columnEntry.getName() + " " + columnEntry.getType();
        } else {
            String columnTemplate = "%s %s (%d)";
            columnEntryString = String.format(columnTemplate, columnEntry.getName(), columnEntry.getType(),
                    columnEntry.getSize());
        }
        return columnEntryString;
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

    public static EntryField<?> convertResultToEntryField(ResultSet resultSet, ColumnData columnData)
            throws SQLException {

        String columnType = columnData.getType();
        String columnName = columnData.getName();
        EntryField<?> entryField;
        if (COLUMN_TYPE_VARCHAR.equals(columnType) || COLUMN_TYPE_CHAR.equals(columnType)) {
            entryField = new EntryField<>(resultSet.getString(columnName));
        } else if (COLUMN_TYPE_INT.equals(columnType)) {
            entryField = new EntryField<>(resultSet.getInt(columnName));
        } else if (COLUMN_TYPE_BIGINT.equals(columnType)) {
            entryField = new EntryField<>(resultSet.getLong(columnName));
        } else if (COLUMN_TYPE_TIMESTAMP.equals(columnType)) {
            entryField = new EntryField<>(resultSet.getTimestamp(columnName, Calendar.getInstance(
                    TimeZone.getTimeZone("UTC"))));
        } else if (COLUMN_TYPE_BLOB.equals(columnType)) {
            entryField = new EntryField<>(resultSet.getBinaryStream(columnName));
        } else {
            entryField = new EntryField<>(resultSet.getObject(columnName));
        }
        return entryField;
    }

    public static void convertEntryFieldToStatement(PreparedStatement ps, EntryField entryField, int index) throws
            SQLException {

        Object value = null;
        if (entryField != null) {
            value = entryField.getValue();
        }

        if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) value, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        } else if (value instanceof InputStream) {
            ps.setBinaryStream(index, (InputStream) value);
        } else {
            ps.setObject(index, value);
        }
    }

    public static List<ColumnData> getColumnData(String tableName, Connection connection) throws SyncClientException {

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            List<ColumnData> columnDataList = new ArrayList<>();

            if (isIdentifierNamesMaintainedInLowerCase(connection)) {
                tableName = tableName.toLowerCase();
            }

            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName,
                    null)) {
                while (resultSet.next()) {
                    String name = resultSet.getString(JDBC_META_DATA_COLUMN_NAME);
                    String type = resultSet.getString(JDBC_META_DATA_TYPE_NAME);
                    int size = resultSet.getInt(JDBC_META_DATA_COLUMN_SIZE);
                    String columnDefaultVal = resultSet.getString(JDBC_META_DATA_COLUMN_DEF);

                    ColumnData columnData = new ColumnData(name, type, size);
                    columnData.setDefaultValue(columnDefaultVal);
                    columnDataList.add(columnData);
                }
            }
            return columnDataList;
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table metadata of source table: " + tableName, e);
        }
    }

    /**
     * Check whether the database dialect maintains identifier (i.e. table, column) names in lower case. (Eg: Postgres)
     *
     * @param connection JDBC connection.
     * @return True if maintained in lower case, else false.
     */
    public static boolean isIdentifierNamesMaintainedInLowerCase(Connection connection) {

        String applicationName = null;

        try {
            applicationName = connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            log.error("Error occurred while retrieving database metadata.");
        }

        return StringUtils.isNotEmpty(applicationName) && applicationName.equals(POSTGRESQL_PRODUCT_NAME);
    }

    public static List<String> getPrimaryKeys(String tableName, Connection connection) throws SyncClientException {

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> primaryKeys = new ArrayList<>();

            if (isIdentifierNamesMaintainedInLowerCase(connection)) {
                tableName = tableName.toLowerCase();
            }

            try (ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName)) {
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME");
                    primaryKeys.add(name);
                }
            }
            return primaryKeys;
        } catch (SQLException e) {
            throw new SyncClientException("Error while retrieving table primary metadata of source table: " + tableName,
                    e);
        }
    }

    /**
     * Retrieve a value from journal entry.
     *
     * @param entry Journal Entry.
     * @param key   Key of the required entry field.
     * @return Value of the target entry field.
     * @deprecated Use getObjectValueFromEntry(JournalEntry entry, String key, boolean isLowerCaseIdentifiers).
     */
    @Deprecated
    public static <T> T getObjectValueFromEntry(JournalEntry entry, String key) {

        EntryField<?> entryField = entry.get(key);
        T value = null;
        if (entryField != null) {
            value = (T) entryField.getValue();
        }
        return value;
    }

    /**
     * Retrieve a value from journal entry.
     *
     * @param entry                  Journal Entry.
     * @param key                    Key of the required entry field.
     * @param isLowerCaseIdentifiers Whether the database dialect mains identifiers in lower case.
     * @return Value of the target entry field.
     */
    public static <T> T getObjectValueFromEntry(JournalEntry entry, String key, boolean isLowerCaseIdentifiers) {

        if (isLowerCaseIdentifiers) {
            key = key.toLowerCase();
        }

        EntryField<?> entryField = entry.get(key);
        T value = null;
        if (entryField != null) {
            value = (T) entryField.getValue();
        }
        return value;
    }
}
