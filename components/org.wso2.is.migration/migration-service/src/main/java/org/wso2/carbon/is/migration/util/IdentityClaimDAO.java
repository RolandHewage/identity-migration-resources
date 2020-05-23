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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_ALL_TOTP_SECRET_KEY_CLAIM_DATA_WITH_MYSQL;
import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_ALL_TOTP_SECRET_KEY_CLAIM_DATA_WITH_OTHER;
import static org.wso2.carbon.is.migration.util.SQLConstants.UPDATE_TOTP_SECRET;

public class IdentityClaimDAO {

    private static IdentityClaimDAO
            instance = new IdentityClaimDAO();

    private IdentityClaimDAO() {

    }

    public static IdentityClaimDAO getInstance() {

        return instance;
    }

    public List<TotpSecretData> getAllTotpSecretData(int limit, int offset, Connection connection) throws SQLException {

        String secretKeyIdentityClaim = "http://wso2.org/claims/identity/secretkey";
        String verifiedSecretKeyIdentityClaim = "http://wso2.org/claims/identity/verifySecretkey";
        List<TotpSecretData> totpSecretDataList = new ArrayList<>();
        boolean mysqlQueryUsed = false;
        String sql;
        if (connection.getMetaData().getDriverName().contains("MySQL")
                // We can't use the similar thing like above with DB2.Check
                // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/java/src/tpc/imjcc_rjvjdapi.html#imjcc_rjvjdapi__d70364e1426
                || connection.getMetaData().getDatabaseProductName().contains("DB2")
                || connection.getMetaData().getDriverName().contains("H2")
                || connection.getMetaData().getDriverName().contains("PostgreSQL")) {
            sql = RETRIEVE_ALL_TOTP_SECRET_KEY_CLAIM_DATA_WITH_MYSQL;
            mysqlQueryUsed = true;

        } else {
            sql = RETRIEVE_ALL_TOTP_SECRET_KEY_CLAIM_DATA_WITH_OTHER;
        }
        try (PreparedStatement preparedStatement = connection
                .prepareStatement(sql)) {
            preparedStatement.setString(1, secretKeyIdentityClaim);
            preparedStatement.setString(2, verifiedSecretKeyIdentityClaim);
            // In mysql type queries, limit and offset values are changed.
            if (mysqlQueryUsed) {
                preparedStatement.setInt(3, limit);
                preparedStatement.setInt(4, offset);
            } else {
                preparedStatement.setInt(3, offset);
                preparedStatement.setInt(4, limit);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    totpSecretDataList.add(new TotpSecretData(resultSet.getString("TENANT_ID"),
                            resultSet.getString("USER_NAME"),
                            resultSet.getString("DATA_VALUE"), resultSet.getString("DATA_KEY")));
                }
            }

        }
        return totpSecretDataList;
    }

    public void updateNewTotpSecrets(List<TotpSecretData> updatedTotpSecretDataList, Connection connection)
            throws SQLException, MigrationClientException {

        connection.setAutoCommit(false);
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_TOTP_SECRET)) {
            for (TotpSecretData totpSecretData : updatedTotpSecretDataList) {
                preparedStatement.setString(1, totpSecretData.getEncryptedSeceretkeyValue());
                preparedStatement.setString(2, totpSecretData.getTenantId());
                preparedStatement.setString(3, totpSecretData.getUserName());
                preparedStatement.setString(4, totpSecretData.getDataKey());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new MigrationClientException(
                    "SQL error while retrieving datasource or database connection for identity claims table", e);
        }
    }
}
