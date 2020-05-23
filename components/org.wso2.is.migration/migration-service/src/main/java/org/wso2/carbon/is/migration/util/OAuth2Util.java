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
package org.wso2.carbon.is.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.tokenprocessor.EncryptionDecryptionPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.is.migration.service.Migrator;

import java.util.ArrayList;
import java.util.List;

public class OAuth2Util {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Util.class);

    public static boolean isTokenEncryptionEnabled() throws MigrationClientException {

        TokenPersistenceProcessor persistenceProcessor = null;
        try {
            persistenceProcessor = OAuthServerConfiguration.getInstance()
                    .getPersistenceProcessor();
        } catch (IdentityOAuth2Exception e) {
            throw new MigrationClientException("Error while reading the TokenPersistenceProcessor from identity.xml."
                    , e);
        }
        return (persistenceProcessor instanceof EncryptionDecryptionPersistenceProcessor);
    }

    public static void migrateOauth2Tokens(int chunkSize, Migrator migrator) throws MigrationClientException {

        int offset = 0;
        log.info("{} Migration starting on OAuth2 access token table with offset {} and limit {}.",
                Constant.MIGRATION_LOG, offset, chunkSize);
        try {
            while (true) {
                List<OauthTokenInfo> oauthTokenList;
                oauthTokenList = TokenDAO.getInstance().getAllEncryptedAccessTokensAndRefreshTokensFromDB(migrator,
                        offset,
                        chunkSize);

                if (oauthTokenList.isEmpty()) {
                    break;
                }
                List<OauthTokenInfo> updatedOauthTokenList = transformTokensFromOldToNewEncryption(oauthTokenList);
                TokenDAO.getInstance().updateTheNewTokensToDB(updatedOauthTokenList, migrator);
                offset += oauthTokenList.size();
                log.info("Access token migration completed for offset {} and limit {}.", offset, chunkSize);

            }
        } catch (MigrationClientException e) {
            String errorMessage = "Error while migrating Oauth2 tokens.";
            if (migrator.isContinueOnError()) {
                log.error(errorMessage, e);
            } else {
                throw new MigrationClientException(errorMessage, e);
            }
        }

    }

    public static void migrateAuthzCodes(int chunkSize, Migrator migrator) throws MigrationClientException {

        int offset = 0;
        log.info("{} Migration starting on OAuth2 authorization code table with offset {} and limit {}.",
                Constant.MIGRATION_LOG, offset, chunkSize);
        try {
            while (true) {
                List<AuthzCodeInfo> authzCodeInfoList;
                authzCodeInfoList = AuthzCodeDAO.getInstance().getAllEncryptedAuthzCodesFromDB(migrator,
                        offset,
                        chunkSize);

                if (authzCodeInfoList.isEmpty()) {
                    break;
                }
                List<AuthzCodeInfo> updatedAuthzCodeInfoList =
                        transformAuthzCodesFromOldToNewEncryption(authzCodeInfoList);
                AuthzCodeDAO.getInstance().updateNewEncryptedAuthzCodes(updatedAuthzCodeInfoList, migrator);
                offset += authzCodeInfoList.size();
                log.info("Access token migration completed for offset {} and limit {}.", offset, chunkSize);

            }
        } catch (MigrationClientException e) {
            String errorMessage = "Error while migrating authorization codes.";
            if (migrator.isContinueOnError()) {
                log.error(errorMessage, e);
            } else {
                throw new MigrationClientException(errorMessage, e);
            }
        }
    }

    public static void mirateClientSecrets(Migrator migrator) throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on OAuth2 consumer apps table.");
        List<ClientSecretInfo> clientSecretInfoList;
        try {
            clientSecretInfoList = OAuthDAO.getInstance().getAllClientSecrets(migrator);
            List<ClientSecretInfo> updatedClientSecretInfoList =
                    transformCientSecretsFromOldToNewEncryption(clientSecretInfoList);
            OAuthDAO.getInstance().updateNewClientSecrets(updatedClientSecretInfoList, migrator);
        } catch (MigrationClientException e) {
            String errorMessage = "Error while migrating client secrets.";
            if (migrator.isContinueOnError()) {
                log.error(errorMessage, e);
            } else {
                throw new MigrationClientException(errorMessage, e);
            }

        }
    }

    public static List<OauthTokenInfo> transformTokensFromOldToNewEncryption(List<OauthTokenInfo> oauthTokenList)
            throws MigrationClientException {

        List<OauthTokenInfo> updatedOauthTokenList = new ArrayList<>();
        for (OauthTokenInfo oauthTokenInfo : oauthTokenList) {
            String newEncryptedAccessToken =
                    EncryptionUtil.transformToSymmetric(oauthTokenInfo.getAccessToken());
            String newEncryptedRefreshToken =
                    EncryptionUtil.transformToSymmetric(oauthTokenInfo.getRefreshToken());
            OauthTokenInfo updatedOauthTokenInfo = new OauthTokenInfo(newEncryptedAccessToken,
                    newEncryptedRefreshToken, oauthTokenInfo.getTokenId());
            updatedOauthTokenList.add(updatedOauthTokenInfo);

        }
        return updatedOauthTokenList;
    }

    public static List<AuthzCodeInfo> transformAuthzCodesFromOldToNewEncryption(List<AuthzCodeInfo> authzCodeInfoList)
            throws MigrationClientException {

        List<AuthzCodeInfo> updatedAuthzCodeInfoList = new ArrayList<>();
        for (AuthzCodeInfo authzCodeInfo : authzCodeInfoList) {
            String newEncryptedAuthzCode =
                    EncryptionUtil.transformToSymmetric(authzCodeInfo.getAuthorizationCode());
            AuthzCodeInfo updatedAuthzCodeInfo = new AuthzCodeInfo(newEncryptedAuthzCode, authzCodeInfo.getCodeId());
            updatedAuthzCodeInfoList.add(updatedAuthzCodeInfo);

        }
        return updatedAuthzCodeInfoList;
    }

    public static List<ClientSecretInfo> transformCientSecretsFromOldToNewEncryption(
            List<ClientSecretInfo> clientSecretInfoList)
            throws MigrationClientException {

        List<ClientSecretInfo> updatedClientSecretInfoList = new ArrayList<>();
        for (ClientSecretInfo clientSecretInfo : clientSecretInfoList) {
            String newEncryptedClientSecret =
                    EncryptionUtil.transformToSymmetric(clientSecretInfo.getClientSecret());
            ClientSecretInfo updatedclientSecretInfo =
                    new ClientSecretInfo(newEncryptedClientSecret, clientSecretInfo.getId());
            updatedClientSecretInfoList.add(updatedclientSecretInfo);

        }
        return updatedClientSecretInfoList;
    }

}
