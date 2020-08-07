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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.VersionAdvice;
import org.wso2.is.data.sync.system.pipeline.transform.model.TokenInfo;
import org.wso2.is.data.sync.system.util.EncryptionUtil;
import org.wso2.is.data.sync.system.util.OAuth2Util;

import java.util.List;

import static org.wso2.is.data.sync.system.util.CommonUtil.getObjectValueFromEntry;
import static org.wso2.is.data.sync.system.util.CommonUtil.isIdentifierNamesMaintainedInLowerCase;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ACCESS_TOKEN;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_REFRESH_TOKEN;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntryForToken;

@VersionAdvice(version = "5.11.0", tableName = "IDN_OAUTH2_ACCESS_TOKEN")
public class OAuthTokenDataTransformerV5110 implements DataTransformer {

    private Log log = LogFactory.getLog(OAuthTokenDataTransformerV5110.class);
    private String oldEncryptionAlgorithm;

    public OAuthTokenDataTransformerV5110(String oldEncryptionAlgorithmConfigured) {

        this.oldEncryptionAlgorithm = oldEncryptionAlgorithmConfigured;
    }

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        try {
            boolean tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();
            boolean isColumnNameInsLowerCase = isIdentifierNamesMaintainedInLowerCase(context.getTargetConnection());
            for (JournalEntry entry : journalEntryList) {
                String accessToken = getObjectValueFromEntry(entry, COLUMN_ACCESS_TOKEN,
                        isColumnNameInsLowerCase);
                String refreshToken = getObjectValueFromEntry(entry, COLUMN_REFRESH_TOKEN,
                        isColumnNameInsLowerCase);
                TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken);
                if (tokenEncryptionEnabled) {
                    EncryptionUtil.setCurrentEncryptionAlgorithm(oldEncryptionAlgorithm);
                    OAuth2Util.transformTokensFromOldToNewEncryption(tokenInfo);
                }
                updateJournalEntryForToken(entry, tokenInfo, isColumnNameInsLowerCase);
            }

        } catch (IdentityOAuth2Exception e) {
            throw new SyncClientException("Error while checking token encryption server configurations", e);
        }
        return journalEntryList;
    }
}
