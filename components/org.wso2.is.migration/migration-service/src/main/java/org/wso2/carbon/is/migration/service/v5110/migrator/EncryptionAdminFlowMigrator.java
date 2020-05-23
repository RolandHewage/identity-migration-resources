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
package org.wso2.carbon.is.migration.service.v5110.migrator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.config.Config;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.util.BPSProfileUtil;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.EncryptionUtil;
import org.wso2.carbon.is.migration.util.EventPublisherUtil;
import org.wso2.carbon.is.migration.util.KeberosSecurityPolicyUtil;
import org.wso2.carbon.is.migration.util.PolicySubscriberUtil;
import org.wso2.carbon.is.migration.util.SecondaryUserStoreUtil;
import org.wso2.carbon.is.migration.util.TenantKeyStoreUtil;

public class EncryptionAdminFlowMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(EncryptionAdminFlowMigrator.class);

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        EncryptionUtil.setCurrentEncryptionAlgorithm(this);
        migrateEventPublisherPassword();
        migrateTenantKeyStorePassword();
        migratepolicySubscriberPassword();
        migrateBPSProfilePassword();
        migrateKeberosSecurityPolicyPassword();
        migrateSecondaryUserStorePassword();
    }

    public void migrateEventPublisherPassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on event publisher files. ");
        EventPublisherUtil.migrateEventPublishers(this);
        log.info(Constant.MIGRATION_LOG + "Migrating event publishers was successful. ");
    }

    public void migrateTenantKeyStorePassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on tenant keystore passwords. ");
        TenantKeyStoreUtil.migrateKeystorePasswords(this);
        log.info(Constant.MIGRATION_LOG + "Migrating tenant keystore passwords was successful. ");
    }

    public void migratepolicySubscriberPassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on policy subscriber passwords. ");
        PolicySubscriberUtil.migrateSubscriberPassword(this);
        log.info(Constant.MIGRATION_LOG + "Migrating policy subscriber passwords was successful. ");
    }

    public void migrateBPSProfilePassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on BPS profile passwords. ");
        BPSProfileUtil.migrateBPSProfilePassword(this);
        log.info(Constant.MIGRATION_LOG + "Migrating BPS profile passwords was successful. ");
    }

    public void migrateKeberosSecurityPolicyPassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on Keberos security policy passwords. ");
        KeberosSecurityPolicyUtil.migrateSecurityPolicyPassword(this);
        log.info(Constant.MIGRATION_LOG + "Migrating Keberos security policy passwords was successful. ");
    }

    public void migrateSecondaryUserStorePassword() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on secondary userstore passwords. ");
        SecondaryUserStoreUtil.migrateSecondaryUserstorePasswords(this);
        log.info(Constant.MIGRATION_LOG + "Migrating secondary userstore passwords was successful. ");

    }

}
