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
package org.wso2.is.data.sync.system.pipeline.transform.v5110;

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.model.AuthorizationCodeInfo;
import org.wso2.is.data.sync.system.util.EncryptionUtil;
import org.wso2.is.data.sync.system.util.OAuth2Util;

import java.util.List;

import static org.wso2.is.data.sync.system.util.CommonUtil.getObjectValueFromEntry;
import static org.wso2.is.data.sync.system.util.CommonUtil.isIdentifierNamesMaintainedInLowerCase;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_AUTHORIZATION_CODE;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntryForCode;

public class AuthorizationCodeDataTransformerV5110 implements DataTransformer {

    private String oldEncryptionAlgorithm;

    public AuthorizationCodeDataTransformerV5110(String oldEncryptionAlgorithmConfigured) {

        this.oldEncryptionAlgorithm = oldEncryptionAlgorithmConfigured;
    }

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        try {
            boolean tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();
            boolean isColumnNameInsLowerCase = isIdentifierNamesMaintainedInLowerCase(context.getTargetConnection());
            for (JournalEntry entry : journalEntryList) {

                String authorizationCode = getObjectValueFromEntry(entry, COLUMN_AUTHORIZATION_CODE,
                        isColumnNameInsLowerCase);
                AuthorizationCodeInfo authorizationCodeInfo = new AuthorizationCodeInfo(authorizationCode);
                if (tokenEncryptionEnabled) {
                    EncryptionUtil.setCurrentEncryptionAlgorithm(oldEncryptionAlgorithm);
                    OAuth2Util.transformAuthzCodesFromOldToNewEncryption(authorizationCodeInfo);
                    updateJournalEntryForCode(entry, authorizationCodeInfo, isColumnNameInsLowerCase);
                }
            }
        } catch (IdentityOAuth2Exception e) {
            throw new SyncClientException("Error while checking token encryption server configurations", e);
        }
        return journalEntryList;
    }
}
