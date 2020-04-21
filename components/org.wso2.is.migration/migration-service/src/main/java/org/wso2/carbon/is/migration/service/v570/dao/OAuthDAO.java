package org.wso2.carbon.is.migration.service.v570.dao;

import org.wso2.carbon.is.migration.service.v550.bean.AuthzCodeInfo;
import org.wso2.carbon.is.migration.service.v550.bean.OauthTokenInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * OAuthDAO.
 */
public class OAuthDAO {

    public static final String RETRIEVE_PAGINATED_TOKENS_MYSQL =
            "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_ID, ACCESS_TOKEN_HASH, REFRESH_TOKEN_HASH " +
            "FROM IDN_OAUTH2_ACCESS_TOKEN " +
            "ORDER BY TOKEN_ID " +
            "LIMIT ? OFFSET ?";
    private static final String RETRIEVE_PAGINATED_TOKENS_OTHER =
            "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_ID, ACCESS_TOKEN_HASH, REFRESH_TOKEN_HASH " +
            "FROM IDN_OAUTH2_ACCESS_TOKEN " +
            "ORDER BY TOKEN_ID " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String RETRIEVE_ALL_AUTHORIZATION_CODES_MYSQL =
            "SELECT AUTHORIZATION_CODE, CODE_ID, AUTHORIZATION_CODE_HASH " +
            "FROM IDN_OAUTH2_AUTHORIZATION_CODE " +
            "ORDER BY CODE_ID " +
            "LIMIT ? OFFSET ?";
    public static final String RETRIEVE_ALL_AUTHORIZATION_CODES_OTHER =
            "SELECT AUTHORIZATION_CODE, CODE_ID, AUTHORIZATION_CODE_HASH " +
            "FROM IDN_OAUTH2_AUTHORIZATION_CODE " +
            "ORDER BY CODE_ID " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String UPDATE_ACCESS_TOKEN = "UPDATE IDN_OAUTH2_ACCESS_TOKEN SET ACCESS_TOKEN=?, " +
            "REFRESH_TOKEN=?, ACCESS_TOKEN_HASH=?, REFRESH_TOKEN_HASH=? WHERE TOKEN_ID=?";
    public static final String UPDATE_AUTHORIZATION_CODE =
            "UPDATE IDN_OAUTH2_AUTHORIZATION_CODE SET AUTHORIZATION_CODE=?, AUTHORIZATION_CODE_HASH=? WHERE CODE_ID=?";

    private static OAuthDAO instance = new OAuthDAO();

    private OAuthDAO() {

    }

    public static OAuthDAO getInstance() {

        return instance;
    }

    /**
     * Method to retrieve access token records from database.
     *
     * @param connection
     * @return list of token info
     * @throws SQLException
     */
    public List<OauthTokenInfo> getAllAccessTokens(Connection connection, int offset, int limit) throws SQLException {

        String sql;
        boolean mysqlQueriesUsed = false;
        if (connection.getMetaData().getDriverName().contains("MySQL")
                // We can't use the similar thing like above with DB2. Check
                // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/java/src/tpc/imjcc_rjvjdapi.html#imjcc_rjvjdapi__d70364e1426
                || connection.getMetaData().getDatabaseProductName().contains("DB2")
                || connection.getMetaData().getDriverName().contains("H2")
                || connection.getMetaData().getDriverName().contains("PostgreSQL")) {
            sql = RETRIEVE_PAGINATED_TOKENS_MYSQL;
            mysqlQueriesUsed = true;
        } else {
            sql = RETRIEVE_PAGINATED_TOKENS_OTHER;
        }

        List<OauthTokenInfo> oauthTokenInfoList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            if (mysqlQueriesUsed) {
                preparedStatement.setInt(1, limit);
                preparedStatement.setInt(2, offset);
            } else {
                preparedStatement.setInt(1, offset);
                preparedStatement.setInt(2, limit);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    OauthTokenInfo tokenInfo = new OauthTokenInfo(resultSet.getString("ACCESS_TOKEN"),
                            resultSet.getString("REFRESH_TOKEN"),
                            resultSet.getString("TOKEN_ID"));
                    tokenInfo.setAccessTokenHash(resultSet.getString("ACCESS_TOKEN_HASH"));
                    tokenInfo.setRefreshTokenHash(resultSet.getString("REFRESH_TOKEN_HASH"));
                    oauthTokenInfoList.add(tokenInfo);
                }
            }
        }
        return oauthTokenInfoList;
    }

    /**
     * Method to persist modified token hash in database.
     *
     * @param updatedOauthTokenList
     * @param connection
     * @throws SQLException
     */
    public void updateNewTokenHash(List<OauthTokenInfo> updatedOauthTokenList, Connection connection)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ACCESS_TOKEN)) {
            for (OauthTokenInfo oauthTokenInfo : updatedOauthTokenList) {
                preparedStatement.setString(1, oauthTokenInfo.getAccessToken());
                preparedStatement.setString(2, oauthTokenInfo.getRefreshToken());
                preparedStatement.setString(3, oauthTokenInfo.getAccessTokenHash());
                preparedStatement.setString(4, oauthTokenInfo.getRefreshTokenHash());
                preparedStatement.setString(5, oauthTokenInfo.getTokenId());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    /**
     * Method to retrieve all the authorization codes from the database.
     *
     * @param connection
     * @return list of authorization codes
     * @throws SQLException
     */
    public List<AuthzCodeInfo> getAllAuthzCodes(Connection connection, int offset, int limit) throws SQLException {

        String sql;
        boolean mysqlQueriesUsed = false;
        if (connection.getMetaData().getDriverName().contains("MySQL")
                // We can't use the similar thing like above with DB2. Check
                // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/java/src/tpc/imjcc_rjvjdapi.html#imjcc_rjvjdapi__d70364e1426
                || connection.getMetaData().getDatabaseProductName().contains("DB2")
                || connection.getMetaData().getDriverName().contains("H2")
                || connection.getMetaData().getDriverName().contains("PostgreSQL")) {
            sql = RETRIEVE_ALL_AUTHORIZATION_CODES_MYSQL;
            mysqlQueriesUsed = true;
        } else {
            sql = RETRIEVE_ALL_AUTHORIZATION_CODES_OTHER;
        }

        List<AuthzCodeInfo> authzCodeInfoList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            if (mysqlQueriesUsed) {
                preparedStatement.setInt(1, limit);
                preparedStatement.setInt(2, offset);
            } else {
                preparedStatement.setInt(1, offset);
                preparedStatement.setInt(2, limit);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                AuthzCodeInfo authzCodeInfo;
                while (resultSet.next()) {
                    authzCodeInfo = new AuthzCodeInfo(resultSet.getString("AUTHORIZATION_CODE"),
                            resultSet.getString("CODE_ID"));
                    authzCodeInfo.setAuthorizationCodeHash(resultSet.getString("AUTHORIZATION_CODE_HASH"));
                    authzCodeInfoList.add(authzCodeInfo);
                }
            }
        }
        return authzCodeInfoList;
    }

    /**
     * Method to update the authorization code table with modified authorization code hashes.
     *
     * @param updatedAuthzCodeList List of updated authorization codes
     * @param connection           database connection
     * @throws SQLException
     */
    public void updateNewAuthzCodeHash(List<AuthzCodeInfo> updatedAuthzCodeList, Connection connection)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_AUTHORIZATION_CODE)) {
            for (AuthzCodeInfo authzCodeInfo : updatedAuthzCodeList) {
                preparedStatement.setString(1, authzCodeInfo.getAuthorizationCode());
                preparedStatement.setString(2, authzCodeInfo.getAuthorizationCodeHash());
                preparedStatement.setString(3, authzCodeInfo.getCodeId());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }
}
