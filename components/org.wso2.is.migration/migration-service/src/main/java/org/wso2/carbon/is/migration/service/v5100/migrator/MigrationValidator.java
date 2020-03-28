/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.is.migration.service.v5100.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.is.migration.service.Migrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MigrationValidator.
 */
public class MigrationValidator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(MigrationValidator.class);

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        boolean hasDuplicateScope;
        try {
            hasDuplicateScope = checkScopeDuplication();
        } catch (MigrationClientException e) {
            throw new MigrationClientException("Error while checking for scope name duplication among " +
                    "IDN_OAUTH2_SCOPE and IDN_OIDC_SCOPE tables.");
        }
        if (hasDuplicateScope) {
            throw new MigrationClientException("Scopes with same name has been found from tables IDN_OAUTH2_SCOPE " +
                    "and IDN_OIDC_SCOPE.");
        }
        log.info("Scope name validation has been passed.");
    }

    private boolean checkScopeDuplication() throws MigrationClientException {

        String sql = "SELECT IDN_OAUTH2_SCOPE.NAME FROM IDN_OAUTH2_SCOPE WHERE IDN_OAUTH2_SCOPE.NAME IN " +
                "(SELECT IDN_OIDC_SCOPE.NAME FROM IDN_OIDC_SCOPE)";
        try (Connection connection = IdentityDatabaseUtil.getDBConnection()) {
            try {
                if (connection != null) {
                    connection.setAutoCommit(false);
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        try (ResultSet results = preparedStatement.executeQuery()) {
                            if (results.next()) {
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        //Ignore. Exception can be thrown when the relevant tables do not exist.
                    } finally {
                        connection.commit();
                    }
                }
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    throw new MigrationClientException("Error occurred while rolling back transactions.", e1);
                }
                throw new MigrationClientException("Error while getting duplicated scopes from IDN_OAUTH2_SCOPE and " +
                        "IDN_OIDC_SCOPE tables", e);
            }
        } catch (SQLException e) {
            throw new MigrationClientException("Error while obtaining database connection.", e);
        }
        return false;
    }
}
