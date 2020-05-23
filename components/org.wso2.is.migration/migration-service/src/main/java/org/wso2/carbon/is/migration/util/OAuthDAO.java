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

import static org.wso2.carbon.is.migration.util.SQLConstants.RETRIEVE_ALL_CONSUMER_SECRETS;
import static org.wso2.carbon.is.migration.util.SQLConstants.UPDATE_CONSUMER_SECRET;

public class OAuthDAO {

    private static OAuthDAO instance = new OAuthDAO();

    private OAuthDAO() {

    }

    public static OAuthDAO getInstance() {

        return instance;
    }

    public List<ClientSecretInfo> getAllClientSecrets(Migrator migrator) throws MigrationClientException {

        List<ClientSecretInfo> clientSecretInfoList = new ArrayList<>();
        try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(RETRIEVE_ALL_CONSUMER_SECRETS);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    clientSecretInfoList
                            .add(new ClientSecretInfo(resultSet.getString("CONSUMER_SECRET"),
                                    resultSet.getInt("ID")));
                }
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while retrieving client secrets from IDN_OAUTH_CONSUMER_APPS.",
                    e);
        }

        return clientSecretInfoList;
    }

    public void updateNewClientSecrets(List<ClientSecretInfo> updatedClientSecretList, Migrator migrator)
            throws MigrationClientException {

        try (Connection connection = migrator.getDataSource(Schema.IDENTITY.getName()).getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_CONSUMER_SECRET)) {
                for (ClientSecretInfo clientSecretInfo : updatedClientSecretList) {
                    preparedStatement.setString(1, clientSecretInfo.getClientSecret());
                    preparedStatement.setInt(2, clientSecretInfo.getId());
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new MigrationClientException("SQL error while retrieving and updating client secrets. ", e);
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while getting connection to IDN_OAUTH_CONSUMER_APPS table to " +
                    "update client secrets.", e);
        }
    }
}
