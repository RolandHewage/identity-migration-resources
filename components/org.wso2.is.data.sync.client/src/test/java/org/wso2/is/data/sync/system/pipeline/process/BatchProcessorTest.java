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

package org.wso2.is.data.sync.system.pipeline.process;

import org.testng.annotations.Test;
import org.wso2.is.data.sync.system.config.Configuration;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineConfiguration;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.persist.Persistor;
import org.wso2.is.data.sync.system.pipeline.result.TransactionResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.testng.Assert.*;

public class BatchProcessorTest {

    @Test
    public void testPollJournal() throws Exception {

//        Configuration configuration = new Configuration("5.3.0", "5.7.0");
//        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(configuration,
//                                                                                "IDN_OAUTH2_ACCESS_TOKEN");
//
//        try (Connection sourceConnection = DriverManager.getConnection("jdbc:mysql://localhost/carbon?user=root&password=root");
//             Connection targetConnection = DriverManager.getConnection("jdbc:mysql://localhost/carbonnew?user=root&password=root")) {
//
//            targetConnection.setAutoCommit(false);
//            PipelineContext context = new PipelineContext(sourceConnection, targetConnection, pipelineConfiguration);
//
//            BatchProcessor batchProcessor = new BatchProcessor();
//            List<JournalEntry> journalEntries = batchProcessor.pollJournal(context);
//
//            Persistor persistor = new Persistor();
//            List<TransactionResult> persist = persistor.persist(journalEntries, context);
//
//            targetConnection.commit();
//
//            System.out.println("some");
//        }
    }
}