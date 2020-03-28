/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.is.data.sync.system.pipeline.transform.v580;

import org.apache.commons.lang.StringUtils;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.VersionAdvice;
import org.wso2.is.data.sync.system.pipeline.transform.model.AuthorizationCodeInfo;

import java.util.List;

import static org.wso2.is.data.sync.system.util.CommonUtil.getObjectValueFromEntry;
import static org.wso2.is.data.sync.system.util.CommonUtil.isIdentifierNamesMaintainedInLowerCase;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_AUTHORIZATION_CODE;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_AUTHORIZATION_CODE_HASH;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_USER_DOMAIN;
import static org.wso2.is.data.sync.system.util.Constant.FEDERATED;
import static org.wso2.is.data.sync.system.util.Constant.TABLE_IDN_OAUTH2_AUTHORIZATION_CODE;
import static org.wso2.is.data.sync.system.util.OAuth2Util.getIdpId;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntryForCode;

/**
 * AuthorizationCodeDataTransformerV580.
 */
@VersionAdvice(version = "5.8.0", tableName = "IDN_OAUTH2_AUTHORIZATION_CODE")
public class AuthorizationCodeDataTransformerV580 implements DataTransformer {

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        boolean isColumnNameInsLowerCase = isIdentifierNamesMaintainedInLowerCase(context.getTargetConnection());
        int idpId = getIdpId(journalEntryList, context, TABLE_IDN_OAUTH2_AUTHORIZATION_CODE);

        for (JournalEntry entry : journalEntryList) {

            String authorizationCode = getObjectValueFromEntry(entry, COLUMN_AUTHORIZATION_CODE,
                    isColumnNameInsLowerCase);
            String authorizationCodeHash = getObjectValueFromEntry(entry, COLUMN_AUTHORIZATION_CODE_HASH,
                    isColumnNameInsLowerCase);
            String userDomain = getObjectValueFromEntry(entry, COLUMN_USER_DOMAIN, isColumnNameInsLowerCase);

            if (idpId != -1 && !StringUtils.equals(userDomain, FEDERATED)) {
                updateJournalEntryForCode(entry, new AuthorizationCodeInfo(authorizationCode, authorizationCodeHash,
                        idpId), isColumnNameInsLowerCase);
            }
        }

        return journalEntryList;
    }
}
