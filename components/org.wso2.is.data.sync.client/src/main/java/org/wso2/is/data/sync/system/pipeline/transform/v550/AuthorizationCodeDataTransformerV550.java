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

package org.wso2.is.data.sync.system.pipeline.transform.v550;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.VersionAdvice;
import org.wso2.is.data.sync.system.pipeline.transform.model.AuthorizationCodeInfo;
import org.wso2.is.data.sync.system.util.OAuth2Util;

import java.util.List;

import static org.wso2.is.data.sync.system.util.CommonUtil.getObjectValueFromEntry;
import static org.wso2.is.data.sync.system.util.CommonUtil.isIdentifierNamesMaintainedInLowerCase;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_AUTHORIZATION_CODE;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_AUTHORIZATION_CODE_HASH;
import static org.wso2.is.data.sync.system.util.OAuth2Util.hashAuthorizationCode;
import static org.wso2.is.data.sync.system.util.OAuth2Util.transformEncryptedAuthorizationCode;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntryForCode;

/**
 * AuthorizationCodeDataTransformerV550.
 */
@VersionAdvice(version = "5.5.0", tableName = "IDN_OAUTH2_AUTHORIZATION_CODE")
public class AuthorizationCodeDataTransformerV550 implements DataTransformer {

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        try {
            boolean encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();
            boolean tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();
            boolean isColumnNameInsLowerCase = isIdentifierNamesMaintainedInLowerCase(context.getTargetConnection());

            for (JournalEntry entry : journalEntryList) {

                String authorizationCode = getObjectValueFromEntry(entry, COLUMN_AUTHORIZATION_CODE,
                        isColumnNameInsLowerCase);
                String authorizationCodeHash = getObjectValueFromEntry(entry, COLUMN_AUTHORIZATION_CODE_HASH,
                        isColumnNameInsLowerCase);

                AuthorizationCodeInfo authorizationCodeInfo = new AuthorizationCodeInfo(authorizationCode,
                        authorizationCodeHash);

                if (encryptionWithTransformationEnabled) {
                    try {
                        transformEncryptedAuthorizationCode(authorizationCodeInfo);
                        if (StringUtils.isBlank(authorizationCodeHash)) {
                            hashAuthorizationCode(authorizationCodeInfo);
                        }
                        updateJournalEntryForCode(entry, authorizationCodeInfo, isColumnNameInsLowerCase);
                    } catch (CryptoException e) {
                        throw new SyncClientException("Error while transforming encrypted authorization codes", e);
                    }
                } else if (!tokenEncryptionEnabled) {
                    if (StringUtils.isBlank(authorizationCodeHash)) {
                        hashAuthorizationCode(authorizationCodeInfo);
                        updateJournalEntryForCode(entry, authorizationCodeInfo, isColumnNameInsLowerCase);
                    }
                }
            }
        } catch (IdentityOAuth2Exception e) {
            throw new SyncClientException("Error while checking authorization code encryption server configurations",
                    e);
        }
        return journalEntryList;
    }
}
