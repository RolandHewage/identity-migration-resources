/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.is.migration.service.v600.migrator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.service.v530.util.JDBCPersistenceUtil;
import org.wso2.carbon.is.migration.util.Schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Migration implementation for updating the token binding type of System Applications.
 */
public class SystemAppTokenBindingTypeMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(SystemAppTokenBindingTypeMigrator.class);

    private static final String UPDATE_ACCESS_TOKEN_BINDING_TYPE =
            "UPDATE IDN_OIDC_PROPERTY SET PROPERTY_VALUE = ? WHERE PROPERTY_KEY = ? AND CONSUMER_KEY = ?";

    private static final String ACCESS_TOKEN_BINDING_TYPE = "accessTokenBindingType";

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        if (getMigratorConfig() == null || getMigratorConfig().getParameters() == null) {
            log.info("SystemAppTokenBindingTypeMigrator parameters not provided. ");
            return;
        }

        log.info("Started migrating system application access token binding type");
        String accessTokenBindingType =
                getMigratorConfig().getParameters().getProperty(ACCESS_TOKEN_BINDING_TYPE);
        try (Connection connection = getDataSource(Schema.IDENTITY.getName()).getConnection()) {
            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ACCESS_TOKEN_BINDING_TYPE)) {
                if (StringUtils.isNotBlank(accessTokenBindingType)) {
                    preparedStatement.setString(1, accessTokenBindingType);
                    preparedStatement.setString(2, "tokenBindingType");
                    preparedStatement.setString(3, "CONSOLE");
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                JDBCPersistenceUtil.rollbackTransaction(connection);
                connection.setAutoCommit(autoCommitStatus);
                throw new MigrationClientException("An error occurred while updating console application" +
                        " token binding type.", e);
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ACCESS_TOKEN_BINDING_TYPE)) {
                if (StringUtils.isNotBlank(accessTokenBindingType)) {
                    preparedStatement.setString(1, accessTokenBindingType);
                    preparedStatement.setString(2, "tokenBindingType");
                    preparedStatement.setString(3, "MY_ACCOUNT");
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                JDBCPersistenceUtil.rollbackTransaction(connection);
                connection.setAutoCommit(autoCommitStatus);
                throw new MigrationClientException("An error occurred while updating myaccount application" +
                        " token binding type.", e);
            }

            JDBCPersistenceUtil.commitTransaction(connection);
            connection.setAutoCommit(autoCommitStatus);
        } catch (SQLException e) {
            throw new MigrationClientException("An error occurred while updating system application" +
                    " token binding type.", e);
        }
        log.info("System application token binding type migration complete.");
    }

}
