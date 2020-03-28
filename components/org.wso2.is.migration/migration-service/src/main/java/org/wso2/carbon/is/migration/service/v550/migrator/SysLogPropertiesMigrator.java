/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.is.migration.service.v550.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.service.v550.RegistryDataManager;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;

/**
 * SysLogPropertiesMigrator.
 */
public class SysLogPropertiesMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(SysLogPropertiesMigrator.class);

    @Override
    public void migrate() throws MigrationClientException {

        migrateSysLogPropertiesPassword();
    }

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    private void migrateSysLogPropertiesPassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on SYSLOG_PROPERTIES file");
        try {
            try {
                RegistryDataManager.getInstance().migrateSysLogPropertyPassword(isIgnoreForInactiveTenants(),
                        isContinueOnError());
            } catch (UserStoreException e) {
                throw new MigrationClientException("Error while retrieving all tenants. ", e);
            } catch (RegistryException e) {
                throw new MigrationClientException("Error while accessing registry and loading SYSLOG_PROPERTIES file" +
                        ".", e);
            } catch (CryptoException e) {
                throw new MigrationClientException("Error while encrypting/decrypting SYSLOG_PROPERTIES password. ", e);
            }
        } catch (MigrationClientException e) {
            if (!isContinueOnError()) {
                throw e;
            }
            log.error("Error while migrating SLOG_PROPERTIES registry resource.", e);
        }
    }
}
