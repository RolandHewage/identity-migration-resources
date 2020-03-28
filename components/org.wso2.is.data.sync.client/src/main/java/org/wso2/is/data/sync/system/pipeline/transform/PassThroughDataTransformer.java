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

package org.wso2.is.data.sync.system.pipeline.transform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;

import java.util.List;

/**
 * This data transformer will be used when there are no custom {@link DataTransformer} instances are registered for a
 * data sync table. This transformer will return the received {@link JournalEntry} data without doing any
 * transformation.
 */
public class PassThroughDataTransformer implements DataTransformer {

    private Log log = LogFactory.getLog(PassThroughDataTransformer.class);

    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        if (log.isDebugEnabled()) {
            log.debug("PassThroughDataTransformer is engaged for table: " + context.getPipelineConfiguration()
                    .getTableName());
        }
        return journalEntryList;
    }
}
