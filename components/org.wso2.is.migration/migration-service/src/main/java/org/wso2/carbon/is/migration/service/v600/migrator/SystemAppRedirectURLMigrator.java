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
 * Migration implementation for updating the redirect URLs of System Applications.
 */
public class SystemAppRedirectURLMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(SystemAppRedirectURLMigrator.class);

    private static final String UPDATE_REDIRECT_URL_SQL =
            "UPDATE IDN_OAUTH_CONSUMER_APPS SET CALLBACK_URL = ? WHERE CONSUMER_KEY = ?";

    private static final String CONSOLE_REDIRECT_URL = "consoleRedirectUrl";
    private static final String MYACCOUNT_REDIRECT_URL = "myaccountRedirectUrl";

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        if (getMigratorConfig() == null || getMigratorConfig().getParameters() == null) {
            log.info("SystemAppRedirectURLMigrator parameters not provided. ");
            return;
        }

        log.info("Started migrating system application redirect URLs.");
        String consoleRedirectUrl = getMigratorConfig().getParameters().getProperty(CONSOLE_REDIRECT_URL);
        String myaccountRedirectUrl = getMigratorConfig().getParameters().getProperty(MYACCOUNT_REDIRECT_URL);
        try (Connection connection = getDataSource(Schema.IDENTITY.getName()).getConnection()) {
            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_REDIRECT_URL_SQL)) {
                if (StringUtils.isNotBlank(consoleRedirectUrl)) {
                    preparedStatement.setString(1, consoleRedirectUrl);
                    preparedStatement.setString(2, "CONSOLE");
                    preparedStatement.executeUpdate();
                }
                if (StringUtils.isNotBlank(myaccountRedirectUrl)) {
                    preparedStatement.setString(1, myaccountRedirectUrl);
                    preparedStatement.setString(2, "MY_ACCOUNT");
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                JDBCPersistenceUtil.rollbackTransaction(connection);
                connection.setAutoCommit(autoCommitStatus);
                throw new MigrationClientException("An error occurred while updating system application" +
                        " redirect URLs.", e);
            }

            JDBCPersistenceUtil.commitTransaction(connection);
            connection.setAutoCommit(autoCommitStatus);
        } catch (SQLException e) {
            throw new MigrationClientException("An error occurred while updating system application" +
                    " redirect URLs.", e);
        }
        log.info("System application redirect URL migration complete.");
    }
}
