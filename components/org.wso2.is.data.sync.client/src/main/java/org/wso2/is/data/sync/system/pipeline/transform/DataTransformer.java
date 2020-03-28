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

import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;

import java.util.List;

/**
 * This interface represents a data transformer between two product versions. The implementations of this class will
 * be able to transform a journal entry from one format to another before persisting the data for data syncing.
 */
public interface DataTransformer {

    /**
     * Transform journal entries from one format to another.
     *
     * @param journalEntryList List of {@link JournalEntry} objects to be transformed.
     * @param context          Context of the current data sync pipeline
     * @return Transformed list of {@link JournalEntry}.
     * @throws SyncClientException If error occurs while data transformation.
     */
    List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException;

}
