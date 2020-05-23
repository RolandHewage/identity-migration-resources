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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.EncryptionUtil;
import org.wso2.carbon.is.migration.util.OAuth2Util;
import org.wso2.carbon.is.migration.util.TotpSecretUtil;
import org.wso2.carbon.is.migration.util.WorkFlowUtil;

import java.util.Properties;

public class EncryptionUserFlowMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUserFlowMigrator.class);
    private static final String LIMIT = "batchSize";
    private static final int DEFAULT_CHUNK_SIZE = 10000;

    @Override
    public void dryRun() throws MigrationClientException {

        log.info("Dry run capability not implemented in {} migrator.", this.getClass().getName());
    }

    @Override
    public void migrate() throws MigrationClientException {

        EncryptionUtil.setCurrentEncryptionAlgorithm(this);
        migrateTotpSecretKey();
        migrateOauthData();
        migrateUserPasswordInWorkFlow();
    }

    public void migrateTotpSecretKey() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on TOTP secret keys. ");
        Properties migrationProperties = getMigratorConfig().getParameters();
        int chunkSize = DEFAULT_CHUNK_SIZE;
        if (migrationProperties.containsKey(LIMIT)) {
            chunkSize = (int) migrationProperties.get(LIMIT);
        }
        TotpSecretUtil.migrateTotpSecretKeys(chunkSize,this);
        log.info(Constant.MIGRATION_LOG + "Migrating TOTP secret keys was successful. ");
    }

    public void migrateOauthData() throws MigrationClientException{

        log.info(Constant.MIGRATION_LOG + "Migration starting on Oauth2 data. ");
        Properties migrationProperties = getMigratorConfig().getParameters();
        int chunkSize = DEFAULT_CHUNK_SIZE;
        if (migrationProperties.containsKey(LIMIT)) {
            chunkSize = (int) migrationProperties.get(LIMIT);
        }
        if(OAuth2Util.isTokenEncryptionEnabled()) {
            OAuth2Util.migrateOauth2Tokens(chunkSize,this);
            OAuth2Util.migrateAuthzCodes(chunkSize,this);
            OAuth2Util.mirateClientSecrets(this);
        }
        log.info(Constant.MIGRATION_LOG + "Migrating Oauth data was successful. ");
    }

    public void migrateUserPasswordInWorkFlow() throws MigrationClientException {

        log.info(Constant.MIGRATION_LOG + "Migration starting on workflow requests. ");
        WorkFlowUtil.migrateWorkFlowRequestCredential(this);
        log.info(Constant.MIGRATION_LOG + "Migrating workflow requests was successful. ");
    }
}
