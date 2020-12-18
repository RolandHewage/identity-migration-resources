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
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.TenantPortalMigratorUtil;
import org.wso2.carbon.is.migration.util.Utility;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.core.UserStoreException;

import java.util.Set;

/**
 * Portal migrator for tenants.
 */
public class TenantPortalMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(TenantPortalMigrator.class);

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Starting the user portal initiation for tenants.");
        Tenant[] tenants = getAllTenants();
        for (Tenant tenant : tenants) {
            try {
                // Skip if the ignoreForInactiveTenants enabled and the tenant is inactive.
                if (isIgnoreForInactiveTenants() && !tenant.isActive()) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Skipping tenant %s since it is a deactivated tenant.",
                                tenant.getDomain()));
                    }
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Starting to initiate user portal for tenant %s.", tenant.getDomain()));
                }
                TenantPortalMigratorUtil.initiatePortals(tenant.getDomain(), tenant.getId());
            } catch (IdentityApplicationManagementException | IdentityOAuthAdminException |
                    UserStoreException | RegistryException e) {
                throw new MigrationClientException(String.format(
                        "Error occurred while initiating portal for tenant %s.", tenant.getDomain()), e);
            }
        }
    }

    private Tenant[] getAllTenants() throws MigrationClientException {

        // Retrieve all the tenants.
        Set<Tenant> tenants = Utility.getTenants();
        if (tenants != null && !tenants.isEmpty()) {
            return tenants.toArray(new Tenant[0]);
        } else {
            return new Tenant[0];
        }
    }
}
