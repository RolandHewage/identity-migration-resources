/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.is.data.sync.system.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.is.data.sync.system.SyncDataTask;
import org.wso2.is.data.sync.system.SyncService;
import org.wso2.is.data.sync.system.config.Configuration.ConfigurationBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.wso2.is.data.sync.system.util.Constant.JVM_PROPERTY_CONFIG_FILE_PATH;
import static org.wso2.is.data.sync.system.util.Constant.JVM_PROPERTY_GENERATE_DDL;
import static org.wso2.is.data.sync.system.util.Constant.JVM_PROPERTY_PREPARE_SYNC;
import static org.wso2.is.data.sync.system.util.Constant.JVM_PROPERTY_SYNC_DATA;

/**
 * SyncClientComponent.
 */
@Component(
        name = "org.wso2.carbon.is.sync.client",
        immediate = true
)
public class SyncClientComponent {

    private static final Log log = LogFactory.getLog(SyncClientComponent.class);
    private RealmService realmService;
    private SyncService syncService;

    /**
     * -DsyncData - Enable data sync related operations.
     * -DprepareSync - creates the sync triggers and tables.
     * -DgenerateDDL - (works with -DprepareSync) only generates DDLs and write to a file.
     * <p>
     * For additional configurations, see {@link ConfigurationBuilder}
     *
     * @param context ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {

            String dataSync = System.getProperty(JVM_PROPERTY_SYNC_DATA);
            String prepareSync = System.getProperty(JVM_PROPERTY_PREPARE_SYNC);

            String configFilePath = System.getProperty(JVM_PROPERTY_CONFIG_FILE_PATH);

            Properties properties = new Properties();
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                try (InputStream inputStream = new FileInputStream(configFile)) {
                    properties.load(inputStream);
                }
            }

            if (prepareSync != null) {
                syncService = new SyncService(properties);
                String generateDDL = System.getProperty(JVM_PROPERTY_GENERATE_DDL);
                syncService.generateScripts(generateDDL != null);
            } else if (dataSync != null) {
                syncService = new SyncService(properties);
                syncService.run();
            }
        } catch (Throwable e) {
            log.error("Error occurred while running data sync client.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (syncService != null) {
            List<SyncDataTask> syncDataTaskList = syncService.getSyncDataTaskList();
            for (SyncDataTask syncDataTask : syncDataTaskList) {
                syncDataTask.shutdown();
            }
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {

        this.realmService = null;
    }

    @Reference(
            name = "identityCoreInitializedEventService",
            service = IdentityCoreInitializedEvent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityCoreInitializedEventService"
    )
    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }
}
