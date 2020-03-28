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

import javax.sql.DataSource;

/**
 * Represents a SQL data source and the DB flavour related to the data source (eg: mysql, oracle).
 */
public class DataSourceEntry {

    private DataSource dataSource;
    private String type;

    public DataSourceEntry(DataSource dataSource, String type) {

        this.dataSource = dataSource;
        this.type = type;
    }

    public DataSource getDataSource() {

        return dataSource;
    }

    public String getType() {

        return type;
    }
}
