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

package org.wso2.is.data.sync.system.pipeline.transform.v570;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
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

import static org.wso2.is.data.sync.system.util.CommonUtil.getObjectValueFromEntry;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ACCESS_TOKEN;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_ACCESS_TOKEN_HASH;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_REFRESH_TOKEN;
import static org.wso2.is.data.sync.system.util.Constant.COLUMN_REFRESH_TOKEN_HASH;
import static org.wso2.is.data.sync.system.util.Constant.PROPERTY_NAME_ALGORITHM;
import static org.wso2.is.data.sync.system.util.Constant.PROPERTY_NAME_HASH;
import static org.wso2.is.data.sync.system.util.OAuth2Util.hashTokens;
import static org.wso2.is.data.sync.system.util.OAuth2Util.transformEncryptedTokens;
import static org.wso2.is.data.sync.system.util.OAuth2Util.updateJournalEntryForToken;

@VersionAdvice(version = "5.7.0", tableName = "IDN_OAUTH2_ACCESS_TOKEN")
public class OAuthTokenDataTransformerV570 implements DataTransformer{

    private static String hashingAlgorithm = OAuthServerConfiguration.getInstance().getHashAlgorithm();

    @Override
    public List<JournalEntry> transform(List<JournalEntry> journalEntryList, PipelineContext context)
            throws SyncClientException {

        try {
            boolean encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();
            boolean tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();

            for (JournalEntry entry : journalEntryList) {

                String accessToken = (String) getObjectValueFromEntry(entry, COLUMN_ACCESS_TOKEN);
                String refreshToken = (String) getObjectValueFromEntry(entry, COLUMN_REFRESH_TOKEN);
                String accessTokenHash = (String) getObjectValueFromEntry(entry, COLUMN_ACCESS_TOKEN_HASH);
                String refreshTokenHash = (String) getObjectValueFromEntry(entry, COLUMN_REFRESH_TOKEN_HASH);

                TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken, accessTokenHash, refreshTokenHash);
                if (encryptionWithTransformationEnabled) {
                    try {
                        transformEncryptedTokens(tokenInfo);
                        if (StringUtils.isBlank(accessTokenHash)) {
                            hashTokens(tokenInfo);
                        } else {
                            reHashWithHashingAlgorithm(tokenInfo, hashingAlgorithm);
                        }
                        updateJournalEntryForToken(entry, tokenInfo);
                    } catch (CryptoException e) {
                        throw new SyncClientException("Error while transforming encrypted tokens", e);
                    }
                } else if (!tokenEncryptionEnabled) {
                    if (StringUtils.isBlank(accessTokenHash)) {
                        hashTokens(tokenInfo);
                        updateJournalEntryForToken(entry, tokenInfo);
                    } else {
                        reHashWithHashingAlgorithm(tokenInfo, hashingAlgorithm);
                    }
                }
            }
        } catch (IdentityOAuth2Exception e) {
            throw new SyncClientException("Error while checking token encryption server configurations", e);
        }
        return journalEntryList;
    }

    private void reHashWithHashingAlgorithm(TokenInfo tokenInfo, String hashAlgorithm) {

        JSONObject accessTokenHashObject;
        JSONObject refreshTokenHashObject;
        String accessTokenHash = tokenInfo.getAccessTokenHash();
        String refreshTokenHash = tokenInfo.getRefreshTokenHash();

        try {
            //If hash column already is a JSON value, no need to update the record
            new JSONObject(accessTokenHash);
        } catch (JSONException e) {
            //Exception is thrown because the hash value is not a json
            accessTokenHashObject = getJsonObjectWithHash(hashAlgorithm, accessTokenHash);
            tokenInfo.setAccessTokenHash(accessTokenHashObject.toString());

            refreshTokenHashObject = getJsonObjectWithHash(hashAlgorithm, refreshTokenHash);
            tokenInfo.setRefreshTokenHash(refreshTokenHashObject.toString());
        }
    }

    private JSONObject getJsonObjectWithHash(String hashAlgorithm, String accessTokenHash) {

        JSONObject tokenHashObject = new JSONObject();
        tokenHashObject.put(PROPERTY_NAME_ALGORITHM, hashAlgorithm);
        tokenHashObject.put(PROPERTY_NAME_HASH, accessTokenHash);
        return tokenHashObject;
    }
}
