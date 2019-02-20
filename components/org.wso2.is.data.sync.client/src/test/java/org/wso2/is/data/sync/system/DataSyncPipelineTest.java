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

package org.wso2.is.data.sync.system;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.is.data.sync.system.config.Configuration;
import org.wso2.is.data.sync.system.database.ColumnData;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.DataSyncPipeline;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_COLUMN_NAME;
import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_COLUMN_SIZE;
import static org.wso2.is.data.sync.client.util.Constant.JDBC_META_DATA_TYPE_NAME;

public class DataSyncPipelineTest {

    @Test
    public void testProcessBatch() throws Exception {

//        DataTransformerFactory factory = new DataTransformerFactory();
//        Configuration configuration = new Configuration("5.3.0", "5.7.0");
//        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(configuration, "FOO_TABLE121");
//
//        DataSyncPipeline dataSyncPipeline = new DataSyncPipeline(factory, pipelineConfiguration);
//        dataSyncPipeline.build();
//        Assert.assertTrue(dataSyncPipeline.processBatch());
    }

    @Test
    public void test() throws Exception {

//        Connection connection = null;
//        try {
//            connection = DriverManager.getConnection("jdbc:mysql://localhost/carbon?user=root&password=root");
//
//            DatabaseMetaData metaData = connection.getMetaData();
//
//            try(ResultSet resultSet = metaData.getColumns(null, null, "IDN_AUTH_SESSION_STORE", null)) {
//                while (resultSet.next()) {
//                    String name = resultSet.getString(JDBC_META_DATA_COLUMN_NAME);
//                    String type = resultSet.getString(JDBC_META_DATA_TYPE_NAME);
//                    int size = resultSet.getInt(JDBC_META_DATA_COLUMN_SIZE);
//
//                    System.out.println(name + " " + type + " " + size);
//                }
//            }
//
//            System.out.println("===============");
//
//            try(ResultSet resultSet = metaData.getPrimaryKeys(null, null, "IDN_OAUTH2_ACCESS_TOKEN")) {
//                while (resultSet.next()) {
//                    String name = resultSet.getString(JDBC_META_DATA_COLUMN_NAME);
//                    String type = resultSet.getString("PK_NAME");
//
//                    System.out.println(name + " " + type);
//                }
//            }
//
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }



    }
}