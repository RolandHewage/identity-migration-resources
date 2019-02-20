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

package org.wso2.is.data.sync.client.impl.oauth;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.oauth.tokenprocessor.HashingPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.is.data.sync.client.DefaultSyncClient;
import org.wso2.is.data.sync.client.datasource.TableMetaData;
import org.wso2.is.data.sync.client.exception.SyncClientException;
import org.wso2.is.data.sync.client.impl.oauth.util.OAuth2Util;
import org.wso2.is.data.sync.client.util.Constant;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY;
import static org.wso2.is.data.sync.client.datasource.SQLQueryProvider.getQuery;
import static org.wso2.is.data.sync.client.util.Constant.SUPPORTED_END_VERSION_V570;
import static org.wso2.is.data.sync.client.util.Constant.SUPPORTED_START_VERSION_V530;

public class OAuthV530V570SyncClient extends DefaultSyncClient {

    private static final List<String> SUPPORTED_TABLES = Collections.singletonList("IDN_OAUTH2_ACCESS_TOKEN");
    private boolean encryptionWithTransformationEnabled;
    private boolean tokenEncryptionEnabled;

    public OAuthV530V570SyncClient() {

        try {
            encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();
            tokenEncryptionEnabled = OAuth2Util.isTokenEncryptionEnabled();
        } catch (SyncClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canSyncData(String tableName) throws SyncClientException {

        if (SUPPORTED_START_VERSION_V530.equals(getSyncSourceVersion()) && SUPPORTED_END_VERSION_V570.equals
                (getSyncTargetVersion()) && SUPPORTED_TABLES.contains(tableName)) {
            return true;
        }
        return false;
    }

    @Override
    public void syncData(String tableName) throws SyncClientException {


    }

    @Override
    protected boolean syncToTarget(String tableName, String syncVersionTableName, TableMetaData metaData,
                                   List<Map<String, Object>> results) throws SyncClientException {


        return super.syncToTarget(tableName, syncVersionTableName, metaData, results);
    }

    @Override
    protected String getTargetInsertQuery(String tableName, TableMetaData metaData) {
        return super.getTargetInsertQuery(tableName, metaData);
    }

    @Override
    protected String getTargetUpdateQuery(String tableName, TableMetaData metaData) {


        // UPDATE %s SET %s WHERE %s
        String sqlUpdate = getQuery(SQL_TEMPLATE_UPDATE_TARGET_SYNC_ENTRY_KEY);
        String updateFilter = metaData.getUpdateFilter();
        updateFilter = String.join(" AND ", updateFilter, "ACCESS_TOKEN_HASH=? AND REFRESH_TOKEN_HASH=?");

        String searchFilter = metaData.getSearchFilter();

        sqlUpdate = String.format(sqlUpdate, tableName, updateFilter, searchFilter);
        return sqlUpdate;
    }

    @Override
    protected void setPSForInsertTarget(TableMetaData metaData, Map<String, Object> rs, PreparedStatement psTargetInsert)
            throws SQLException, SyncClientException {

        super.setPSForInsertTarget(metaData, rs, psTargetInsert);
    }

    @Override
    protected void setPSForUpdateTarget(TableMetaData metaData, Map<String, Object> results,
                                        PreparedStatement psTargetUpdate) throws SQLException, SyncClientException {


        String accessToken = (String) results.get("ACCESS_TOKEN");
        String refreshToken = (String) results.get("REFRESH_TOKEN");
        String accessTokenHash = (String) results.get("ACCESS_TOKEN_HASH");
        String refreshTokenHash = (String) results.get("REFRESH_TOKEN_HASH");

        //migrating RSA encrypted tokens to OAEP encryption
        if (encryptionWithTransformationEnabled) {
            //migrateOldEncryptedTokens(oauthTokenList);

            try {
                if (!isBase64DecodeAndIsSelfContainedCipherText(accessToken)) {

                    byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                                            .base64DecodeAndDecrypt(accessToken, "RSA");
                    String newEncryptedAccessToken = CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode
                            (decryptedAccessToken);
                    byte[] decryptedRefreshToken = null;
                    String newEncryptedRefreshToken = null;
                    if (refreshToken != null) {
                        decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                                          .base64DecodeAndDecrypt(refreshToken, "RSA");
                        newEncryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                                             .encryptAndBase64Encode(decryptedRefreshToken);
                    }
                    TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();

                    accessTokenHash = tokenPersistenceProcessor
                            .getProcessedAccessTokenIdentifier(new String(decryptedAccessToken, Charsets.UTF_8));
                    if (refreshToken != null) {
                        refreshTokenHash = tokenPersistenceProcessor
                                .getProcessedRefreshToken(new String(decryptedRefreshToken, Charsets.UTF_8));
                    }


                } else if (isBase64DecodeAndIsSelfContainedCipherText(accessToken) && StringUtils
                        .isBlank(accessTokenHash)) {

                    byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                                            .base64DecodeAndDecrypt(accessToken);
                    byte[] decryptedRefreshToken = null;
                    if (refreshToken != null) {
                        decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                                          .base64DecodeAndDecrypt(refreshToken);
                    }
                    TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                    accessTokenHash = tokenPersistenceProcessor
                            .getProcessedAccessTokenIdentifier(new String(decryptedAccessToken, Charsets.UTF_8));
                    if (refreshToken != null) {
                        refreshTokenHash = tokenPersistenceProcessor
                                .getProcessedRefreshToken(new String(decryptedRefreshToken, Charsets.UTF_8));
                    }

                }

            } catch (CryptoException e) {
                e.printStackTrace();
            } catch (IdentityOAuth2Exception e) {
                e.printStackTrace();
            }
        }
        //migrating plaintext tokens with hashed tokens.
        if (!tokenEncryptionEnabled) {
            //migratePlainTextTokens(oauthTokenList);

            TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
            try {
                accessTokenHash = tokenPersistenceProcessor.getProcessedAccessTokenIdentifier(accessToken);
                if (refreshToken != null) {
                    refreshTokenHash = tokenPersistenceProcessor.getProcessedRefreshToken(refreshToken);
                }
            } catch (IdentityOAuth2Exception e) {
                e.printStackTrace();
            }
        }

        List<String> primaryKeys = metaData.getPrimaryKeys();
        List<String> nonPrimaryKeys = metaData.getNonPrimaryKeys();

        for (int i = 0; i < nonPrimaryKeys.size(); i++) {
            psTargetUpdate.setObject(i + 1, results.get(nonPrimaryKeys.get(i)));
        }
        for (int i = 0; i < primaryKeys.size(); i++) {
            psTargetUpdate.setObject(nonPrimaryKeys.size() + 1 + i, results.get(primaryKeys.get(i)));
        }

        int size = primaryKeys.size() + nonPrimaryKeys.size();

        psTargetUpdate.setString(++size , accessTokenHash);
        psTargetUpdate.setString(++size , refreshTokenHash);

    }

    private boolean isBase64DecodeAndIsSelfContainedCipherText(String text) throws CryptoException {

        return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndIsSelfContainedCipherText(text);
    }

    @Override
    public String getSchema(String tableName) throws SyncClientException {

        return Constant.SCHEMA_TYPE_IDENTITY;
    }
}
