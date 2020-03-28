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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Contains meta data related to a database table. This class holds information on, table columns, primary keys,
 * filtering strings based on primary and non primary columns.
 */
public class TableMetaData {

    private List<ColumnData> columnDataList = new ArrayList<>();
    private List<String> primaryKeys = new ArrayList<>();
    private List<String> nonPrimaryKeys = new ArrayList<>();
    private String columns;
    private String parameters;
    private String updateFilter;
    private String searchFilter;

    /**
     * Constructor for {@link TableMetaData} object.
     *
     * @param columnDataList List of {@link ColumnData} of the table.
     * @param primaryKeys    List of strings representing the primary key column names.
     * @param nonPrimaryKeys List of strings representing non primary key column names.
     * @param columns        Sting containing comma column names of the table.
     * @param parameters     Sting containing bindings for the prepared statement which matches the number of columns of
     *                       the table.
     * @param updateFilter   Sting containing comma separated of non primary key columns which can be used in update
     *                       statements.
     * @param searchFilter   Sting containing comma separated of primary key columns which can be used for search
     *                       statements.
     */
    private TableMetaData(List<ColumnData> columnDataList, List<String> primaryKeys,
                          List<String> nonPrimaryKeys, String columns, String parameters, String updateFilter,
                          String searchFilter) {

        this.columnDataList = columnDataList;
        this.primaryKeys = primaryKeys;
        this.nonPrimaryKeys = nonPrimaryKeys;
        this.columns = columns;
        this.parameters = parameters;
        this.updateFilter = updateFilter;
        this.searchFilter = searchFilter;
    }

    public List<ColumnData> getColumnDataList() {

        return columnDataList;
    }

    public List<String> getPrimaryKeys() {

        return primaryKeys;
    }

    public List<String> getNonPrimaryKeys() {

        return nonPrimaryKeys;
    }

    public String getColumns() {

        return columns;
    }

    public String getParameters() {

        return parameters;
    }

    public String getUpdateFilter() {

        return updateFilter;
    }

    public String getSearchFilter() {

        return searchFilter;
    }

    /**
     * Builder.
     */
    public static class Builder {

        private List<ColumnData> columnDataList;
        private List<String> primaryKeys;

        public Builder setColumnData(List<ColumnData> columnDataList) {

            this.columnDataList = columnDataList;
            return this;
        }

        public Builder setPrimaryKeys(List<String> primaryKeys) {

            this.primaryKeys = primaryKeys;
            return this;
        }

        public TableMetaData build() {

            StringJoiner columnJoiner = new StringJoiner(", ");
            StringJoiner valueJoiner = new StringJoiner(", ");
            StringJoiner updateJoiner = new StringJoiner(", ");
            StringJoiner searchJoiner = new StringJoiner(" AND ");

            List<String> nonPrimaryKeys = new ArrayList<>();
            for (ColumnData columnData : columnDataList) {
                String columnName = columnData.getName();
                columnJoiner.add(columnName);
                valueJoiner.add("?");

                if (!primaryKeys.contains(columnName)) {
                    updateJoiner.add(String.format("%s = ?", columnName));
                    nonPrimaryKeys.add(columnName);
                }
            }

            for (String primaryKey : primaryKeys) {
                searchJoiner.add(String.format("%s = ?", primaryKey));
            }

            String columns = columnJoiner.toString();
            String parameters = valueJoiner.toString();
            String updateFilter = updateJoiner.toString();
            String searchFilter = searchJoiner.toString();

            return new TableMetaData(columnDataList, primaryKeys, nonPrimaryKeys, columns, parameters, updateFilter,
                    searchFilter);
        }
    }
}
