/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.is.migration.service.v550.migrator;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.oauth.tokenprocessor.HashingPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.service.v550.bean.AuthzCodeInfo;
import org.wso2.carbon.is.migration.service.v550.bean.ClientSecretInfo;
import org.wso2.carbon.is.migration.service.v550.bean.OauthTokenInfo;
import org.wso2.carbon.is.migration.service.v550.dao.AuthzCodeDAO;
import org.wso2.carbon.is.migration.service.v550.dao.OAuthDAO;
import org.wso2.carbon.is.migration.service.v550.dao.TokenDAO;
import org.wso2.carbon.is.migration.service.v550.util.OAuth2Util;
import org.wso2.carbon.is.migration.util.Constant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * OAuthDataMigrator.
 */
public class OAuthDataMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(OAuthDataMigrator.class);
    boolean isTokenHashColumnsAvailable = false;
    boolean isAuthzCodeHashColumnAvailable = false;
    boolean isClientSecretHashColumnsAvailable = false;

    private static final String LIMIT = "batchSize";
    private static final int DEFAULT_CHUNK_SIZE = 10000;

    @Override
    public void migrate() throws MigrationClientException {

        // Get the batch size from the configuration if it is provided. Or else use the default size of 10000.
        Properties migrationProperties = getMigratorConfig().getParameters();
        int chunkSize = DEFAULT_CHUNK_SIZE;
        if (migrationProperties.containsKey(LIMIT)) {
            chunkSize = (int) migrationProperties.get(LIMIT);
        }

        try {
            addHashColumns();
            deleteClientSecretHashColumn();
            migrateTokens(chunkSize);
            migrateAuthorizationCodes(chunkSize);
            migrateClientSecrets();
        } catch (SQLException e) {
            throw new MigrationClientException("Error while adding hash columns", e);
        }
    }

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    public void addHashColumns() throws MigrationClientException, SQLException {

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            isTokenHashColumnsAvailable = TokenDAO.getInstance().isTokenHashColumnsAvailable(connection);
            isAuthzCodeHashColumnAvailable = AuthzCodeDAO.getInstance().isAuthzCodeHashColumnAvailable(connection);
            connection.rollback();
        }
        if (!isTokenHashColumnsAvailable) {
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try {
                    TokenDAO.getInstance().addAccessTokenHashColumn(connection);
                    TokenDAO.getInstance().addRefreshTokenHashColumn(connection);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new MigrationClientException("SQL error while adding hash columns", e);
                }
            }
        }
        if (!isAuthzCodeHashColumnAvailable) {
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try {
                    AuthzCodeDAO.getInstance().addAuthzCodeHashColumns(connection);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new MigrationClientException("SQL error while adding hash columns", e);
                }
            }
        }
    }

    public void deleteClientSecretHashColumn() throws MigrationClientException, SQLException {

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            isClientSecretHashColumnsAvailable = OAuthDAO.getInstance().isConsumerSecretHashColumnAvailable(connection);
            connection.rollback();
        }
        if (isClientSecretHashColumnsAvailable) {
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try {
                    OAuthDAO.getInstance().deleteConsumerSecretHashColumn(connection);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new MigrationClientException("SQL error while delete client secret hash columns", e);
                }
            }
        }
    }

    /**
     * Method to migrate encrypted tokens/plain text tokens.
     *
     * @throws MigrationClientException
     * @throws SQLException
     */
    public void migrateTokens(int chunkSize) throws MigrationClientException, SQLException {

        int offset = 0;
        log.info("{} Migration starting on OAuth2 access token table with offset {} and limit {}.",
                Constant.MIGRATION_LOG, offset, chunkSize);

        while (true) {
            List<OauthTokenInfo> oauthTokenList;
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                oauthTokenList = TokenDAO.getInstance().getAllAccessTokensWithHash(connection, offset, chunkSize);
            }

            if (oauthTokenList.isEmpty()) {
                break;
            }

            try {
                // Migrating RSA encrypted tokens to OAEP encryption
                if (OAuth2Util.isEncryptionWithTransformationEnabled()) {
                    migrateOldEncryptedTokens(oauthTokenList);
                }
                // Migrating plaintext tokens with hashed tokens.
                if (!OAuth2Util.isTokenEncryptionEnabled()) {
                    migratePlainTextTokens(oauthTokenList);
                }
                offset += oauthTokenList.size();
                log.info("Access token migration completed for offset {} and limit {}.", offset, chunkSize);
            } catch (IdentityOAuth2Exception e) {
                throw new MigrationClientException(e.getMessage(), e);
            }
        }
    }

    public void migrateOldEncryptedTokens(List<OauthTokenInfo> oauthTokenList)
            throws MigrationClientException, SQLException, IdentityOAuth2Exception {

        log.info(Constant.MIGRATION_LOG + "Migration starting on OAuth2 access token table with encrypted tokens.");
        List<OauthTokenInfo> updatedOauthTokenList = null;
        updatedOauthTokenList = transformFromOldToNewEncryption(oauthTokenList);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            TokenDAO.getInstance().updateNewEncryptedTokens(updatedOauthTokenList, connection);
        }

    }

    /**
     * Method to migrate plain text tokens. This will add hashed tokens to acess token and refresh token hash columns.
     *
     * @param oauthTokenList list of tokens to be migrated
     * @throws IdentityOAuth2Exception
     * @throws MigrationClientException
     * @throws SQLException
     */
    public void migratePlainTextTokens(List<OauthTokenInfo> oauthTokenList)
            throws IdentityOAuth2Exception, MigrationClientException, SQLException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on OAuth2 access token table with plain text tokens.");
        try {
            List<OauthTokenInfo> updatedOauthTokenList = generateTokenHashValues(oauthTokenList);
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                TokenDAO.getInstance().updatePlainTextTokens(updatedOauthTokenList, connection);
            }
        } catch (IdentityOAuth2Exception e) {
            throw new IdentityOAuth2Exception("Error while migration plain text tokens", e);
        }
    }

    private List<OauthTokenInfo> transformFromOldToNewEncryption(List<OauthTokenInfo> oauthTokenList)
            throws MigrationClientException {

        List<OauthTokenInfo> updatedOauthTokenList = new ArrayList<>();
        TokenPersistenceProcessor hashingPersistenceProcessor = new HashingPersistenceProcessor();

        for (OauthTokenInfo oauthTokenInfo : oauthTokenList) {
            String accessToken = oauthTokenInfo.getAccessToken();
            String refreshToken = oauthTokenInfo.getRefreshToken();
            OauthTokenInfo updatedTokenInfo = null;
            if (accessToken != null) {
                try {
                    boolean accessTokenSelfContained = isBase64DecodeAndIsSelfContainedCipherText(accessToken);
                    if (!accessTokenSelfContained) {
                        byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(accessToken,
                                "RSA");
                        String newEncryptedAccessToken =
                                CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedAccessToken);
                        String accessTokenHash =
                                hashingPersistenceProcessor.getProcessedAccessTokenIdentifier(
                                        new String(decryptedAccessToken, Charsets.UTF_8));
                        updatedTokenInfo = new OauthTokenInfo(oauthTokenInfo);
                        updatedTokenInfo.setAccessToken(newEncryptedAccessToken);
                        updatedTokenInfo.setAccessTokenHash(accessTokenHash);
                    }

                    if (accessTokenSelfContained && StringUtils.isBlank(oauthTokenInfo.getAccessTokenHash())) {
                        byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(accessToken);
                        String accessTokenHash =
                                hashingPersistenceProcessor.getProcessedAccessTokenIdentifier(
                                        new String(decryptedAccessToken, Charsets.UTF_8));
                        updatedTokenInfo = new OauthTokenInfo(oauthTokenInfo);
                        updatedTokenInfo.setAccessTokenHash(accessTokenHash);
                    }
                } catch (CryptoException | IdentityOAuth2Exception e) {
                    if (isContinueOnError()) {
                        log.error("Error when migrating the access token with token id: " +
                                oauthTokenInfo.getTokenId(), e);
                    } else {
                        throw new MigrationClientException("Error when migrating the access token with token id: " +
                                oauthTokenInfo.getTokenId(), e);
                    }
                }
            } else {
                log.debug("Access token is null for token id: " + oauthTokenInfo.getTokenId());
            }

            if (refreshToken != null) {
                try {
                    boolean refreshTokenSelfContained = isBase64DecodeAndIsSelfContainedCipherText(refreshToken);
                    if (!refreshTokenSelfContained) {
                        byte[] decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(refreshToken,
                                "RSA");
                        String newEncryptedRefreshToken =
                                CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedRefreshToken);
                        String refreshTokenHash =
                                hashingPersistenceProcessor
                                        .getProcessedAccessTokenIdentifier(new String(decryptedRefreshToken,
                                                Charsets.UTF_8));
                        if (updatedTokenInfo == null) {
                            updatedTokenInfo = new OauthTokenInfo(oauthTokenInfo);
                        }
                        updatedTokenInfo.setRefreshToken(newEncryptedRefreshToken);
                        updatedTokenInfo.setRefreshTokenHash(refreshTokenHash);
                    }

                    if (refreshTokenSelfContained && StringUtils.isBlank(oauthTokenInfo.getRefreshTokenHash())) {
                        byte[] decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(refreshToken);
                        String refreshTokenHash =
                                hashingPersistenceProcessor.getProcessedAccessTokenIdentifier(
                                        new String(decryptedRefreshToken, Charsets.UTF_8));
                        if (updatedTokenInfo == null) {
                            updatedTokenInfo = new OauthTokenInfo(oauthTokenInfo);
                            updatedTokenInfo.setRefreshTokenHash(refreshTokenHash);
                        }
                    }
                } catch (CryptoException | IdentityOAuth2Exception e) {
                    if (isContinueOnError()) {
                        log.error("Error when migrating the refresh token with token id: " +
                                oauthTokenInfo.getTokenId(), e);
                    } else {
                        throw new MigrationClientException("Error when migrating the refresh token with token id: " +
                                oauthTokenInfo.getTokenId(), e);
                    }
                }
            } else {
                log.debug("Refresh token is null for token id: " + oauthTokenInfo.getTokenId());
            }

            if (updatedTokenInfo != null) {
                updatedOauthTokenList.add(updatedTokenInfo);
            }
        }
        return updatedOauthTokenList;
    }

    private boolean isBase64DecodeAndIsSelfContainedCipherText(String text) throws CryptoException {

        return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndIsSelfContainedCipherText(text);
    }

    private List<OauthTokenInfo> generateTokenHashValues(List<OauthTokenInfo> oauthTokenList)
            throws IdentityOAuth2Exception {

        List<OauthTokenInfo> updatedOauthTokenList = new ArrayList<>();

        for (OauthTokenInfo oauthTokenInfo : oauthTokenList) {
            if (StringUtils.isBlank(oauthTokenInfo.getAccessTokenHash())) {
                String accessToken = oauthTokenInfo.getAccessToken();
                String refreshToken = oauthTokenInfo.getRefreshToken();
                TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                String accessTokenHash = tokenPersistenceProcessor.getProcessedAccessTokenIdentifier(accessToken);
                String refreshTokenHash = null;
                if (refreshToken != null) {
                    refreshTokenHash = tokenPersistenceProcessor.getProcessedRefreshToken(refreshToken);
                }
                OauthTokenInfo updatedOauthTokenInfo = (new OauthTokenInfo(accessToken, refreshToken,
                        oauthTokenInfo.getTokenId()));
                updatedOauthTokenInfo.setAccessTokenHash(accessTokenHash);
                updatedOauthTokenInfo.setRefreshTokenHash(refreshTokenHash);
                updatedOauthTokenList.add(updatedOauthTokenInfo);
            }
        }
        return updatedOauthTokenList;
    }

    /**
     * Method to migrate old encrypted authorization codes/ plain text authorization codes.
     *
     * @throws MigrationClientException
     * @throws SQLException
     */
    public void migrateAuthorizationCodes(int chunkSize) throws MigrationClientException, SQLException {

        int offset = 0;
        log.info("{} Migration starting on OAuth2 authorization code table with offset {} and limit {}.",
                Constant.MIGRATION_LOG, offset, chunkSize);

        while (true) {
            List<AuthzCodeInfo> authzCodeInfoList;
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                authzCodeInfoList = AuthzCodeDAO.getInstance().getAllAuthzCodesWithHashes(connection, offset, chunkSize);
            }
            if (authzCodeInfoList.isEmpty()) {
                break;
            }
            try {
                // Migrating RSA encrypted authz codes to OAEP encryption.
                if (OAuth2Util.isEncryptionWithTransformationEnabled()) {
                    migrateOldEncryptedAuthzCodes(authzCodeInfoList);
                }
                // Migrating plaintext authz codes with hashed authz codes.
                if (!OAuth2Util.isTokenEncryptionEnabled()) {
                    migratePlainTextAuthzCodes(authzCodeInfoList);
                }
                offset += authzCodeInfoList.size();
                log.info("Authorization code migration completed with offset {} and limit {}", offset, chunkSize);
            } catch (IdentityOAuth2Exception e) {
                throw new MigrationClientException(
                        "Error while checking configurations for encryption with " + "transformation is enabled. ", e);
            } catch (SQLException e) {
                throw new MigrationClientException("Error while getting datasource connection. ", e);
            }
        }
    }

    /**
     * This method will migrate authorization codes encrypted in RSA to OAEP.
     *
     * @param authzCodeInfoList list of authz codes
     * @throws MigrationClientException
     * @throws SQLException
     */
    public void migrateOldEncryptedAuthzCodes(List<AuthzCodeInfo> authzCodeInfoList)
            throws MigrationClientException, SQLException {

        log.info(Constant.MIGRATION_LOG
                + "Migration starting on OAuth2 authorization table with encrypted authorization codes.");
        try {
            List<AuthzCodeInfo> updatedAuthzCodeInfoList = transformAuthzCodeFromOldToNewEncryption(authzCodeInfoList);
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                AuthzCodeDAO.getInstance().updateNewEncryptedAuthzCodes(updatedAuthzCodeInfoList, connection);
            }
        } catch (CryptoException e) {
            throw new MigrationClientException("Error while encrypting in new encryption algorithm.", e);
        } catch (IdentityOAuth2Exception e) {
            throw new MigrationClientException("Error while migrating old encrypted authz codes.", e);
        }

    }

    /**
     * This method will generate hash values of authorization codes and update the authorization
     * code table with those values.
     *
     * @param authzCodeInfoList
     * @throws MigrationClientException
     * @throws SQLException
     */
    public void migratePlainTextAuthzCodes(List<AuthzCodeInfo> authzCodeInfoList)
            throws MigrationClientException, SQLException {

        log.info(Constant.MIGRATION_LOG
                + "Migration starting on OAuth2 authorization code table with plain text codes.");
        try {
            List<AuthzCodeInfo> updatedAuthzCodeInfoList = generateAuthzCodeHashValues(authzCodeInfoList);
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                AuthzCodeDAO.getInstance().updatePlainTextAuthzCodes(updatedAuthzCodeInfoList, connection);
            }
        } catch (IdentityOAuth2Exception e) {
            throw new MigrationClientException("Error while migration plain text authorization codes.", e);
        }
    }

    private List<AuthzCodeInfo> transformAuthzCodeFromOldToNewEncryption(List<AuthzCodeInfo> authzCodeInfoList)
            throws CryptoException, IdentityOAuth2Exception {

        List<AuthzCodeInfo> updatedAuthzCodeInfoList = new ArrayList<>();
        for (AuthzCodeInfo authzCodeInfo : authzCodeInfoList) {
            if (!isBase64DecodeAndIsSelfContainedCipherText(authzCodeInfo.getAuthorizationCode())) {
                byte[] decryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil()
                        .base64DecodeAndDecrypt(authzCodeInfo.getAuthorizationCode(), "RSA");
                String newEncryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil()
                        .encryptAndBase64Encode(decryptedAuthzCode);
                TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                String authzCodeHash;
                authzCodeHash = tokenPersistenceProcessor
                        .getProcessedAuthzCode(new String(decryptedAuthzCode, Charsets.UTF_8));

                AuthzCodeInfo updatedAuthzCodeInfo = (new AuthzCodeInfo(newEncryptedAuthzCode,
                        authzCodeInfo.getCodeId()));
                updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);
                updatedAuthzCodeInfoList.add(updatedAuthzCodeInfo);
            } else if (isBase64DecodeAndIsSelfContainedCipherText(authzCodeInfo.getAuthorizationCode()) &&
                    StringUtils.isBlank(authzCodeInfo.getAuthorizationCodeHash())) {
                byte[] decryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil()
                        .base64DecodeAndDecrypt(authzCodeInfo.getAuthorizationCode());
                TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                String authzCodeHash;
                authzCodeHash = tokenPersistenceProcessor
                        .getProcessedAuthzCode(new String(decryptedAuthzCode, Charsets.UTF_8));

                AuthzCodeInfo updatedAuthzCodeInfo = (new AuthzCodeInfo(authzCodeInfo.getAuthorizationCode(),
                        authzCodeInfo.getCodeId()));
                updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);
                updatedAuthzCodeInfoList.add(updatedAuthzCodeInfo);
            }
        }
        return updatedAuthzCodeInfoList;
    }

    private List<AuthzCodeInfo> generateAuthzCodeHashValues(List<AuthzCodeInfo> authzCodeInfoList)
            throws IdentityOAuth2Exception {

        List<AuthzCodeInfo> updatedAuthzCodeInfoList = new ArrayList<>();
        for (AuthzCodeInfo authzCodeInfo : authzCodeInfoList) {

            if (StringUtils.isBlank(authzCodeInfo.getAuthorizationCodeHash())) {
                String authorizationCode = authzCodeInfo.getAuthorizationCode();
                TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                String authzCodeHash = tokenPersistenceProcessor.getProcessedAuthzCode(authorizationCode);
                AuthzCodeInfo updatedAuthzCodeInfo = new AuthzCodeInfo(authorizationCode, authzCodeInfo.getCodeId());
                updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);
                updatedAuthzCodeInfoList.add(updatedAuthzCodeInfo);
            }
        }
        return updatedAuthzCodeInfoList;
    }

    /**
     * Method to migrate old encrypted client secrets to new encrypted client secrets.
     *
     * @throws MigrationClientException
     */
    public void migrateClientSecrets() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on OAuth2 consumer apps table.");
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            if (OAuth2Util.isEncryptionWithTransformationEnabled()) {
                List<ClientSecretInfo> clientSecretInfoList;
                clientSecretInfoList = OAuthDAO.getInstance().getAllClientSecrets(connection);
                List<ClientSecretInfo> updatedClientSecretInfoList = null;
                updatedClientSecretInfoList = transformClientSecretFromOldToNewEncryption(clientSecretInfoList);
                OAuthDAO.getInstance().updateNewClientSecrets(updatedClientSecretInfoList, connection);
            }
        } catch (IdentityOAuth2Exception e) {
            throw new MigrationClientException("Error while checking encryption with transformation is enabled. ", e);
        } catch (SQLException e) {
            throw new MigrationClientException("Error while retrieving and updating client secrets. ", e);
        }
    }

    private List<ClientSecretInfo> transformClientSecretFromOldToNewEncryption(
            List<ClientSecretInfo> clientSecretInfoList) throws MigrationClientException {

        List<ClientSecretInfo> updatedClientSecretList = new ArrayList<>();
        for (ClientSecretInfo clientSecretInfo : clientSecretInfoList) {
            try {
                if (!CryptoUtil.getDefaultCryptoUtil()
                        .base64DecodeAndIsSelfContainedCipherText(clientSecretInfo.getClientSecret())) {
                    byte[] decryptedClientSecret = CryptoUtil.getDefaultCryptoUtil()
                            .base64DecodeAndDecrypt(clientSecretInfo.getClientSecret(), "RSA");
                    String newEncryptedClientSecret = CryptoUtil.getDefaultCryptoUtil()
                            .encryptAndBase64Encode(decryptedClientSecret);
                    ClientSecretInfo updatedClientSecretInfo = (new ClientSecretInfo(newEncryptedClientSecret,
                            clientSecretInfo.getId()));
                    updatedClientSecretList.add(updatedClientSecretInfo);
                }
            } catch (CryptoException e) {
                if (isContinueOnError()) {
                    log.error("Error when migrating the secret for client with app ID: " +
                            clientSecretInfo.getId(), e);
                } else {
                    throw new MigrationClientException("Error when migrating the secret for client with app ID: " +
                            clientSecretInfo.getId(), e);
                }
            }
        }
        return updatedClientSecretList;
    }

}
