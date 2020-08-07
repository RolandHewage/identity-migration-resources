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

import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.service.Migrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_PAGINATED_TOKENS_WITH_MYSQL;
import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_PAGINATED_TOKENS_WITH_OTHER;
import static org.wso2.carbon.is.migration.util.SQLConstants.UPDATE_ENCRYPTED_ACCESS_TOKEN;

/**
 * DAO class for retrieving and updating Oauth2 tokens from DB.
 */
public class TokenDAO {

    private static TokenDAO instance = new TokenDAO();

    private TokenDAO() {

    }

    public static TokenDAO getInstance() {

        return instance;
    }

    public List<OauthTokenInfo> getAllEncryptedAccessTokensAndRefreshTokensFromDB(Migrator migrator, int offset,
                                                                                  int limit)
            throws MigrationClientException {

        List<OauthTokenInfo> oauthTokenInfoList = new ArrayList<>();
        try {
            try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {
                connection.setAutoCommit(false);
                String sql;
                boolean mysqlQueryUsed = false;
                if (connection.getMetaData().getDriverName().contains("MySQL")
                        // We can't use the similar thing like above with DB2.Check
                        // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/java/src/tpc/imjcc_rjvjdapi.html#imjcc_rjvjdapi__d70364e1426
                        || connection.getMetaData().getDatabaseProductName().contains("DB2")
                        || connection.getMetaData().getDriverName().contains("H2")
                        || connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    sql = RETRIEVE_PAGINATED_TOKENS_WITH_MYSQL;
                    mysqlQueryUsed = true;
                } else {
                    sql = RETRIEVE_PAGINATED_TOKENS_WITH_OTHER;
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    // In mysql type queries, limit and offset values are changed.
                    if (mysqlQueryUsed) {
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
                            oauthTokenInfoList.add(tokenInfo);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while retrieving access tokens and refresh tokens from " +
                    "IDN_OAUTH2_ACCESS_TOKEN table.", e);
        }
        return oauthTokenInfoList;

    }

    public void updateTheNewTokensToDB(List<OauthTokenInfo> updatedOauthTokenList, Migrator migrator)
            throws MigrationClientException {

        try {
            try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ENCRYPTED_ACCESS_TOKEN)) {
                    for (OauthTokenInfo oauthTokenInfo : updatedOauthTokenList) {
                        preparedStatement.setString(1, oauthTokenInfo.getAccessToken());
                        preparedStatement.setString(2, oauthTokenInfo.getRefreshToken());
                        preparedStatement.setString(5, oauthTokenInfo.getTokenId());
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new MigrationClientException("SQL error while update new encrypted token ", e);
                }

            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while getting connection to IDN_OAUTH2_ACCESS_TOKEN to update " +
                    "the tokens.", e);
        }
    }

}
