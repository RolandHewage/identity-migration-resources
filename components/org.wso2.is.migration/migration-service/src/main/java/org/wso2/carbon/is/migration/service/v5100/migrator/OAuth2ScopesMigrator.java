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
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.Utility;
import org.wso2.carbon.user.api.Tenant;

import java.sql.SQLException;
import java.util.Set;

/**
 * OAuth 2 scopes migrator for tenants.
 */
public class OAuth2ScopesMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ScopesMigrator.class);

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        try {
            migrateOAuth2ScopesOfTenants();
        } catch (SQLException ex) {
            throw new MigrationClientException(
                    "Error occurred while migrating the OAuth2 internal scopes to tenants.", ex);
        }
    }

    private void migrateOAuth2ScopesOfTenants() throws MigrationClientException, SQLException {

        try {
            Set<Tenant> tenants = Utility.getTenants();
            for (Tenant tenant : tenants) {
                log.info(Constant.MIGRATION_LOG + "Started the dry run for tenant: " + tenant.getDomain());
                if (tenant.isActive() || !(isIgnoreForInactiveTenants())) {
                    log.info(Constant.MIGRATION_LOG + "Started migrating the internal OAuth2 scopes for the tenant: " +
                            tenant.getDomain());
                    OAuth2Util.initiateOAuthScopePermissionsBindings(tenant.getId());
                }
            }
        } catch (MigrationClientException e) {
            String message = Constant.MIGRATION_LOG + "Error occurred while migrating the OAuth2 resources.";
            if (isContinueOnError()) {
                log.error(message, e);
            } else {
                throw new MigrationClientException(message, e);
            }
        }
    }
}
