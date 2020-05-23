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

import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_PAGINATED_AUTHORIZATION_CODES_MYSQL;
import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_PAGINATED_AUTHORIZATION_CODES_OTHER;
import static org.wso2.carbon.is.migration.util.SQLConstants.UPDATE_ENCRYPTED_AUTHORIZATION_CODE;

public class AuthzCodeDAO {

    private static AuthzCodeDAO instance = new AuthzCodeDAO();

    private AuthzCodeDAO() {

    }

    public static AuthzCodeDAO getInstance() {

        return instance;
    }

    public List<AuthzCodeInfo> getAllEncryptedAuthzCodesFromDB(Migrator migrator, int offset, int limit)
            throws MigrationClientException {

        try {
            try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {

                String sql;
                boolean mysqlQueriesUsed = false;
                if (connection.getMetaData().getDriverName().contains("MySQL")
                        // We can't use the similar thing like above with DB2. Check
                        // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/java/src/tpc/imjcc_rjvjdapi.html#imjcc_rjvjdapi__d70364e1426
                        || connection.getMetaData().getDatabaseProductName().contains("DB2")
                        || connection.getMetaData().getDriverName().contains("H2")
                        || connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    sql = RETRIEVE_PAGINATED_AUTHORIZATION_CODES_MYSQL;
                    mysqlQueriesUsed = true;
                } else {
                    sql = RETRIEVE_PAGINATED_AUTHORIZATION_CODES_OTHER;
                }
                List<AuthzCodeInfo> authzCodeInfoList = new ArrayList<>();
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    // In mysql type queries, limit and offset values are changed.
                    if (mysqlQueriesUsed) {
                        preparedStatement.setInt(1, limit);
                        preparedStatement.setInt(2, offset);
                    } else {
                        preparedStatement.setInt(1, offset);
                        preparedStatement.setInt(2, limit);
                    }
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            AuthzCodeInfo authzCodeInfo = new AuthzCodeInfo(resultSet.getString("AUTHORIZATION_CODE"),
                                    resultSet.getString("CODE_ID"));
                            authzCodeInfoList.add(authzCodeInfo);
                        }
                    }
                }
                return authzCodeInfoList;
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while retrieving authz codes from " +
                    "IDN_OAUTH2_ACCESS_TOKEN table.", e);
        }

    }

    public void updateNewEncryptedAuthzCodes(List<AuthzCodeInfo> updatedAuthzCodeList, Migrator migrator)
            throws MigrationClientException {

        try {
            try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(UPDATE_ENCRYPTED_AUTHORIZATION_CODE)) {
                    for (AuthzCodeInfo authzCodeInfo : updatedAuthzCodeList) {
                        preparedStatement.setString(1, authzCodeInfo.getAuthorizationCode());
                        preparedStatement.setString(3, authzCodeInfo.getCodeId());
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw new MigrationClientException("SQL error while update new encrypted authz codes.", e);
                }
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while getting connection to IDN_OAUTH2_AUTHORIZATION_CODE table" +
                    " to update the authz codes.", e);
        }
    }

}
