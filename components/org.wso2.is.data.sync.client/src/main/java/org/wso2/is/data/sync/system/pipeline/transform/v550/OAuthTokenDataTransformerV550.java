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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.is.data.sync.system.exception.SyncClientException;
import org.wso2.is.data.sync.system.pipeline.EntryField;
import org.wso2.is.data.sync.system.pipeline.JournalEntry;
import org.wso2.is.data.sync.system.pipeline.PipelineContext;
import org.wso2.is.data.sync.system.pipeline.transform.DataTransformer;
import org.wso2.is.data.sync.system.pipeline.transform.VersionAdvice;
import org.wso2.is.data.sync.system.pipeline.transform.model.TokenInfo;
import org.wso2.is.data.sync.system.util.OAuth2Util;

import java.util.List;

import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ACCESS_TOKEN;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ACCESS_TOKEN_HASH;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_REFRESH_TOKEN;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_REFRESH_TOKEN_HASH;
import static org.wso2.is.data.sync.system.util.OAuth2Util.hashTokens;
import static org.wso2.is.data.sync.system.util.OAuth2Util.transformEncryptedTokens;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntry;

@VersionAdvice(version = "5.5.0", tableName = "IDN_OAUTH2_ACCESS_TOKEN")
public class OAuthTokenDataTransformerV550 implements DataTransformer {

    private Log log = LogFactory.getLog(OAuthTokenDataTransformerV550.class);

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        try {
            boolean encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();
            boolean tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();

            for (JournalEntry entry : journalEntryList) {

                String accessToken = (String) getEntryValue(entry, COLUMN_ACCESS_TOKEN);
                String refreshToken = (String) getEntryValue(entry, COLUMN_REFRESH_TOKEN);
                String accessTokenHash = (String) getEntryValue(entry, COLUMN_ACCESS_TOKEN_HASH);
                String refreshTokenHash = (String) getEntryValue(entry, COLUMN_REFRESH_TOKEN_HASH);

                TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken, accessTokenHash, refreshTokenHash);

                if (encryptionWithTransformationEnabled) {
                    try {
                        transformEncryptedTokens(tokenInfo);
                        if (StringUtils.isBlank(accessTokenHash)) {
                            hashTokens(tokenInfo);
                        }
                        updateJournalEntry(entry, tokenInfo);
                    } catch (CryptoException e) {
                        throw new SyncClientException("Error while transforming encrypted tokens", e);
                    }
                } else if (!tokenEncryptionEnabled) {
                    if (StringUtils.isBlank(accessTokenHash)) {
                        hashTokens(tokenInfo);
                        updateJournalEntry(entry, tokenInfo);
                    }
                }
            }
        } catch (IdentityOAuth2Exception e) {
            throw new SyncClientException("Error while checking token encryption server configurations", e);
        }
        return journalEntryList;
    }

    private Object getEntryValue(JournalEntry entry, String key) {

        EntryField entryField = entry.get(key);
        Object value = null;
        if (entryField != null) {
            value = entryField.getValue();
        }
        return value;
    }
}
