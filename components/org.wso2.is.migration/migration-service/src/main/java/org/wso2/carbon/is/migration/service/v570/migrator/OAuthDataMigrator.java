package org.wso2.carbon.is.migration.service.v570.migrator;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.tokenprocessor.HashingPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.service.v550.bean.AuthzCodeInfo;
import org.wso2.carbon.is.migration.service.v550.bean.OauthTokenInfo;
import org.wso2.carbon.is.migration.service.v550.util.OAuth2Util;
import org.wso2.carbon.is.migration.service.v570.dao.OAuthDAO;
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

    private static final String ALGORITHM = "algorithm";
    private static final String HASH = "hash";
    private static final String LIMIT = "batchSize";

    private static final int DEFAULT_CHUNK_SIZE = 10000;
    private static String hashingAlgo = OAuthServerConfiguration.getInstance().getHashAlgorithm();

    @Override
    public void migrate() throws MigrationClientException {

        // Get the batch size from the configuration if it is provided. Or else use the default size of 10000.
        Properties migrationProperties = getMigratorConfig().getParameters();
        int chunkSize = DEFAULT_CHUNK_SIZE;
        if (migrationProperties.containsKey(LIMIT)) {
            chunkSize = (int) migrationProperties.get(LIMIT);
        }

        migrateTokenHash(chunkSize);
        migrateAuthzCodeHash(chunkSize);
    }

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    public void migrateTokenHash(int chunkSize) throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on OAuth2 access token table.");

        int offset = 0;
        log.info("Offset is set to {} and limit is set to {}", offset, chunkSize);

        while (true) {
            List<OauthTokenInfo> tokenInfoList = getTokenList(offset, chunkSize);
            if (tokenInfoList.isEmpty()) {
                break;
            }
            try {
                List<OauthTokenInfo> updateTokenInfoList = updateHashColumnValues(tokenInfoList, hashingAlgo);
                try (Connection connection = getDataSource().getConnection()) {
                    connection.setAutoCommit(false);
                    // Persists modified hash values.
                    try {
                        OAuthDAO.getInstance().updateNewTokenHash(updateTokenInfoList, connection);
                        connection.commit();
                        log.info("Access token migration completed for tokens {} to {} ", offset, chunkSize);
                        offset += tokenInfoList.size();
                    } catch (SQLException e1) {
                        connection.rollback();
                        String error = "SQL error while updating token hash";
                        throw new MigrationClientException(error, e1);
                    }
                } catch (SQLException e) {
                    String error = "SQL error while updating token hash";
                    throw new MigrationClientException(error, e);
                }
            } catch (CryptoException e) {
                throw new MigrationClientException("Error while encrypting tokens.", e);
            } catch (IdentityOAuth2Exception e) {
                throw new MigrationClientException("Error while migrating tokens.", e);
            }
        }
    }

    public void migrateAuthzCodeHash(int chunkSize) throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on Authorization code table");

        int offset = 0;
        log.info("Offset is set to {} and limit is set to {}", offset, chunkSize);

        while (true) {
            List<AuthzCodeInfo> authzCodeInfos = getAuthzCoedList(offset, chunkSize);
            if (authzCodeInfos.isEmpty()) {
                break;
            }
            try {
                List<AuthzCodeInfo> updatedAuthzCodeInfoList =
                        updateAuthzCodeHashColumnValues(authzCodeInfos, hashingAlgo);
                try (Connection connection = getDataSource().getConnection()) {
                    connection.setAutoCommit(false);
                    // Persists modified hash values.
                    try {
                        OAuthDAO.getInstance().updateNewAuthzCodeHash(updatedAuthzCodeInfoList, connection);
                        connection.commit();
                        log.info("Authorization code migration completed for tokens {} to {} ", offset, chunkSize);
                        offset += authzCodeInfos.size();
                    } catch (SQLException e1) {
                        connection.rollback();
                        String error = "SQL error while updating authorization code hash";
                        throw new MigrationClientException(error, e1);
                    }
                } catch (SQLException e) {
                    String error = "SQL error while updating authorization code hash";
                    throw new MigrationClientException(error, e);
                }
            } catch (CryptoException e) {
                throw new MigrationClientException("Error while encrypting authorization codes.", e);
            } catch (IdentityOAuth2Exception e) {
                throw new MigrationClientException("Error while migrating authorization codes.", e);
            }
        }
    }

    private List<OauthTokenInfo> getTokenList(int offset, int limit) throws MigrationClientException {

        List<OauthTokenInfo> oauthTokenList;
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            oauthTokenList = OAuthDAO.getInstance().getAllAccessTokens(connection, offset, limit);
        } catch (SQLException e) {
            String error = "SQL error while retrieving token hash";
            throw new MigrationClientException(error, e);
        }

        return oauthTokenList;
    }

    private List<AuthzCodeInfo> getAuthzCoedList(int offset, int limit) throws MigrationClientException {

        List<AuthzCodeInfo> authzCodeInfoList;
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            authzCodeInfoList = OAuthDAO.getInstance().getAllAuthzCodes(connection, offset, limit);
        } catch (SQLException e) {
            String error = "SQL error while retrieving authorization code hash";
            throw new MigrationClientException(error, e);
        }

        return authzCodeInfoList;
    }

    private boolean isBase64DecodeAndIsSelfContainedCipherText(String text) throws CryptoException {

        return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndIsSelfContainedCipherText(text);
    }

    private List<OauthTokenInfo> updateHashColumnValues(List<OauthTokenInfo> oauthTokenList, String hashAlgorithm)
            throws CryptoException, IdentityOAuth2Exception {

        List<OauthTokenInfo> updatedOauthTokenList = new ArrayList<>();
        if (oauthTokenList != null) {
            boolean encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();

            for (OauthTokenInfo tokenInfo : oauthTokenList) {

                String accessToken = tokenInfo.getAccessToken();
                String refreshToken = tokenInfo.getRefreshToken();

                if (encryptionWithTransformationEnabled) {
                    // Token OAEP encryption is enabled.
                    if (!isBase64DecodeAndIsSelfContainedCipherText(accessToken)) {
                        // Existing access tokens are not encrypted with OAEP.
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

                        OauthTokenInfo updatedOauthTokenInfo =
                                getHashedTokenInfoFromEncryptedToken(tokenInfo, newEncryptedAccessToken,
                                        newEncryptedRefreshToken,
                                        decryptedAccessToken,
                                        decryptedRefreshToken);
                        updatedOauthTokenList.add(updatedOauthTokenInfo);
                    } else {
                        // Existing access tokens are encrypted with OAEP.
                        if (StringUtils.isBlank(tokenInfo.getAccessTokenHash())) {
                            // Token hash is empty.
                            byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                    .base64DecodeAndDecrypt(accessToken);
                            byte[] decryptedRefreshToken = null;
                            if (refreshToken != null) {
                                decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                        .base64DecodeAndDecrypt(refreshToken);
                            }
                            OauthTokenInfo updatedOauthTokenInfo =
                                    getHashedTokenInfoFromEncryptedToken(tokenInfo, accessToken, refreshToken,
                                            decryptedAccessToken,
                                            decryptedRefreshToken);
                            updatedOauthTokenList.add(updatedOauthTokenInfo);
                        } else {
                            // Token hash is not empty.
                            String oldAccessTokenHash = tokenInfo.getAccessTokenHash();
                            try {
                                //If hash column already is a JSON value, no need to update the record
                                new JSONObject(oldAccessTokenHash);
                            } catch (JSONException e) {
                                //Exception is thrown because the hash value is not a json
                                buildHashedTokenInfoJson(hashAlgorithm, tokenInfo, oldAccessTokenHash);
                                updatedOauthTokenList.add(tokenInfo);
                            }
                        }
                    }
                } else if (OAuth2Util.isTokenEncryptionEnabled()) {
                    // Token encryption is enabled with RSA.
                    if (StringUtils.isBlank(tokenInfo.getAccessTokenHash())) {
                        // Hash value is not present.
                        byte[] decryptedAccessToken = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(accessToken, "RSA");
                        byte[] decryptedRefreshToken = null;
                        if (refreshToken != null) {
                            decryptedRefreshToken = CryptoUtil.getDefaultCryptoUtil()
                                    .base64DecodeAndDecrypt(refreshToken, "RSA");
                        }
                        OauthTokenInfo updatedOauthTokenInfo =
                                getHashedTokenInfoFromEncryptedToken(tokenInfo, accessToken, refreshToken,
                                        decryptedAccessToken, decryptedRefreshToken);
                        updatedOauthTokenList.add(updatedOauthTokenInfo);
                    } else {
                        // Hash value is present.
                        String oldAccessTokenHash = tokenInfo.getAccessTokenHash();
                        try {
                            //If hash column already is a JSON value, no need to update the record
                            new JSONObject(oldAccessTokenHash);
                        } catch (JSONException e) {
                            //Exception is thrown because the hash value is not a json
                            buildHashedTokenInfoJson(hashAlgorithm, tokenInfo, oldAccessTokenHash);
                            updatedOauthTokenList.add(tokenInfo);
                        }
                    }
                } else {
                    // Token encryption is not enabled.
                    if (StringUtils.isBlank(tokenInfo.getAccessTokenHash())) {
                        OauthTokenInfo updatedOauthTokenInfo = getHashedTokenInfo(tokenInfo, accessToken, refreshToken);
                        updatedOauthTokenList.add(updatedOauthTokenInfo);
                    } else {
                        String oldAccessTokenHash = tokenInfo.getAccessTokenHash();
                        try {
                            //If hash column already is a JSON value, no need to update the record
                            new JSONObject(oldAccessTokenHash);
                        } catch (JSONException e) {
                            //Exception is thrown because the hash value is not a json
                            buildHashedTokenInfoJson(hashAlgorithm, tokenInfo, oldAccessTokenHash);
                            updatedOauthTokenList.add(tokenInfo);
                        }
                    }
                }
            }
        }
        return updatedOauthTokenList;
    }

    private OauthTokenInfo getHashedTokenInfoFromEncryptedToken(OauthTokenInfo tokenInfo, String accessToken,
                                                                String refreshToken, byte[] decryptedAccessToken,
                                                                byte[] decryptedRefreshToken)
            throws IdentityOAuth2Exception {

        TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
        String accessTokenHash;
        String refreshTokenHash = null;

        accessTokenHash = tokenPersistenceProcessor
                .getProcessedAccessTokenIdentifier(new String(decryptedAccessToken, Charsets.UTF_8));
        if (refreshToken != null) {
            refreshTokenHash = tokenPersistenceProcessor
                    .getProcessedRefreshToken(new String(decryptedRefreshToken, Charsets.UTF_8));
        }

        OauthTokenInfo updatedOauthTokenInfo = (new OauthTokenInfo(accessToken,
                refreshToken,
                tokenInfo.getTokenId()));
        updatedOauthTokenInfo.setAccessTokenHash(accessTokenHash);
        if (refreshToken != null) {
            updatedOauthTokenInfo.setRefreshTokenHash(refreshTokenHash);
        }
        return updatedOauthTokenInfo;
    }

    private void buildHashedTokenInfoJson(String hashAlgorithm, OauthTokenInfo tokenInfo, String oldAccessTokenHash) {

        JSONObject accessTokenHashObject;
        JSONObject refreshTokenHashObject;
        accessTokenHashObject = new JSONObject();
        accessTokenHashObject.put(ALGORITHM, hashAlgorithm);
        accessTokenHashObject.put(HASH, oldAccessTokenHash);
        tokenInfo.setAccessTokenHash(accessTokenHashObject.toString());

        refreshTokenHashObject = new JSONObject();
        String oldRefreshTokenHash = tokenInfo.getRefreshTokenHash();
        refreshTokenHashObject.put(ALGORITHM, hashAlgorithm);
        refreshTokenHashObject.put(HASH, oldRefreshTokenHash);
        tokenInfo.setRefreshTokenHash(refreshTokenHashObject.toString());
    }

    private OauthTokenInfo getHashedTokenInfo(OauthTokenInfo tokenInfo, String accessToken, String refreshToken)
            throws IdentityOAuth2Exception {

        TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
        String accessTokenHash;
        String refreshTokenHash = null;

        accessTokenHash = tokenPersistenceProcessor.getProcessedAccessTokenIdentifier(accessToken);
        if (refreshToken != null) {
            refreshTokenHash = tokenPersistenceProcessor.getProcessedRefreshToken(refreshToken);
        }

        OauthTokenInfo updatedOauthTokenInfo = (new OauthTokenInfo(accessToken,
                refreshToken,
                tokenInfo.getTokenId()));
        updatedOauthTokenInfo.setAccessTokenHash(accessTokenHash);
        if (refreshToken != null) {
            updatedOauthTokenInfo.setRefreshTokenHash(refreshTokenHash);
        }
        return updatedOauthTokenInfo;
    }

    private AuthzCodeInfo getAuthzCodeInfo(AuthzCodeInfo authzCodeInfo, String authzCode)
            throws IdentityOAuth2Exception {

        TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
        String authzCodeHash = tokenPersistenceProcessor.getProcessedAuthzCode(authzCode);

        AuthzCodeInfo updatedAuthzCodeInfo = new AuthzCodeInfo(authzCode, authzCodeInfo.getCodeId());
        updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);

        return updatedAuthzCodeInfo;
    }

    private List<AuthzCodeInfo> updateAuthzCodeHashColumnValues(List<AuthzCodeInfo> authzCodeInfos,
                                                                String hashAlgorithm)
            throws IdentityOAuth2Exception, CryptoException {

        List<AuthzCodeInfo> updatedAuthzCodeList = new ArrayList<>();
        if (authzCodeInfos != null) {
            boolean encryptionWithTransformationEnabled = OAuth2Util.isEncryptionWithTransformationEnabled();

            for (AuthzCodeInfo authzCodeInfo : authzCodeInfos) {
                String authzCode = authzCodeInfo.getAuthorizationCode();

                if (encryptionWithTransformationEnabled) {
                    // Code encryption is enabled.
                    if (!isBase64DecodeAndIsSelfContainedCipherText(authzCode)) {
                        // Existing codes are not encrypted with OAEP.
                        byte[] decryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil()
                                .base64DecodeAndDecrypt(authzCode, "RSA");
                        String newEncryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode
                                (decryptedAuthzCode);
                        TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                        String authzCodeHash = tokenPersistenceProcessor
                                .getProcessedAuthzCode(new String(decryptedAuthzCode, Charsets.UTF_8));
                        AuthzCodeInfo updatedAuthzCodeInfo = (new AuthzCodeInfo(newEncryptedAuthzCode,
                                authzCodeInfo.getCodeId()));
                        updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);
                        updatedAuthzCodeList.add(updatedAuthzCodeInfo);
                    } else {
                        if (StringUtils.isBlank(authzCodeInfo.getAuthorizationCodeHash())) {

                            byte[] decryptedAuthzCode = CryptoUtil.getDefaultCryptoUtil()
                                    .base64DecodeAndDecrypt(authzCode);

                            TokenPersistenceProcessor tokenPersistenceProcessor = new HashingPersistenceProcessor();
                            String authzCodeHash = tokenPersistenceProcessor
                                    .getProcessedAuthzCode(new String(decryptedAuthzCode, Charsets.UTF_8));

                            AuthzCodeInfo updatedAuthzCodeInfo = (new AuthzCodeInfo(authzCode, authzCodeInfo
                                    .getCodeId()));
                            updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHash);
                            updatedAuthzCodeList.add(updatedAuthzCodeInfo);
                        }
                    }
                } else {
                    // Code encryption is not enabled.
                    if (StringUtils.isBlank(authzCodeInfo.getAuthorizationCodeHash())) {

                        AuthzCodeInfo updatedAuthzCodeInfo = getAuthzCodeInfo(authzCodeInfo, authzCode);
                        updatedAuthzCodeList.add(updatedAuthzCodeInfo);
                    } else {
                        String oldAuthzCodeHash = authzCodeInfo.getAuthorizationCodeHash();
                        try {
                            // If hash column already is a JSON value, no need to update the record
                            new JSONObject(oldAuthzCodeHash);
                        } catch (JSONException e) {
                            // Exception is thrown because the hash value is not a json
                            JSONObject authzCodeHashObject = new JSONObject();
                            authzCodeHashObject.put(ALGORITHM, hashAlgorithm);
                            authzCodeHashObject.put(HASH, oldAuthzCodeHash);
                            AuthzCodeInfo updatedAuthzCodeInfo = (new AuthzCodeInfo(authzCode, authzCodeInfo
                                    .getCodeId()));
                            updatedAuthzCodeInfo.setAuthorizationCodeHash(authzCodeHashObject.toString());
                            updatedAuthzCodeList.add(updatedAuthzCodeInfo);
                        }
                    }
                }
            }
        }
        return updatedAuthzCodeList;
    }
}
