/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.is.migration;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.consent.mgt.core.util.ConsentConfigParser;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.is.migration.internal.ISMigrationServiceDataHolder;
import org.wso2.carbon.is.migration.util.Constant;
import org.wso2.carbon.is.migration.util.Schema;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.dbcreator.DatabaseCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * Class for datasource management.
 */
public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);
    private static DataSourceManager dataSourceManager = null;
    private DataSource dataSource;
    private DataSource umDataSource;
    private static Map<String, DataSource> regDataSources;
    private DataSource consentDataSource;

    private DataSourceManager() {

        try {
            initIdentityDataSource();
            initUMDataSource();
            initConsentDataSource();
            initRegDataSources();
        } catch (MigrationClientException e) {
            log.error("Error while initializing datasource manager.", e);
        }
        initOracleDataSource();
    }

    public static DataSourceManager getInstance() {

        if (DataSourceManager.dataSourceManager == null) {
            DataSourceManager.dataSourceManager = new DataSourceManager();
        }
        return DataSourceManager.dataSourceManager;
    }

    public DataSource getDataSource(Schema schema) throws MigrationClientException {

        if (schema.getName().equals(Schema.IDENTITY.getName())) {
            return dataSource;
        } else if (schema.getName().equals(Schema.UM.getName())) {
            return umDataSource;
        } else if (schema.getName().equals(Schema.CONSENT.getName())) {
            return consentDataSource;
        } else if (schema.getName().equals(Schema.UMA.getName())) {
            return dataSource;
        }
        throw new MigrationClientException("DataSource is not available for " + schema);
    }

    public DataSource getDataSource(String schema) throws MigrationClientException {

        if (schema.equals(Schema.IDENTITY.getName())) {
            return dataSource;
        } else if (schema.equals(Schema.UM.getName())) {
            return umDataSource;
        } else if (schema.equals(Schema.CONSENT.getName())) {
            return consentDataSource;
        } else if (schema.equals(Schema.UMA.getName())) {
            return dataSource;
        }
        throw new MigrationClientException("DataSource is not available for " + schema);
    }

    /**
     * Return registry datasources.
     */
    public Map<String, DataSource> getRegistryDataSources() throws MigrationClientException {

        return regDataSources;
    }

    /**
     * Init Oracle specific database.
     */
    private void initOracleDataSource() {

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            if ("oracle".equals(DatabaseCreator.getDatabaseType(conn)) && ISMigrationServiceDataHolder
                    .getIdentityOracleUser() == null) {
                ISMigrationServiceDataHolder.setIdentityOracleUser(dataSource.getConnection().getMetaData()
                        .getUserName());
                log.info(Constant.MIGRATION_LOG + "Initialized identity database in Oracle.");
            }
        } catch (Exception e) {
            log.error("Error occurred while initializing identity database for Oracle.", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the identity database connection", e);
            }
        }

        try {
            conn = umDataSource.getConnection();
            if ("oracle".equals(DatabaseCreator.getDatabaseType(conn)) && ISMigrationServiceDataHolder
                    .getUmOracleUser() == null) {
                ISMigrationServiceDataHolder.setUmOracleUser(umDataSource.getConnection().getMetaData()
                        .getUserName());
                log.info(Constant.MIGRATION_LOG + "Initialized user management database in Oracle.");
            }
        } catch (Exception e) {
            log.error("Error occurred while initializing user management database for Oracle.", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the user manager database connection", e);
            }
        }

        try {
            conn = consentDataSource.getConnection();
            if ("oracle".equals(DatabaseCreator.getDatabaseType(conn)) && ISMigrationServiceDataHolder
                    .getIdentityOracleUser() == null) {
                ISMigrationServiceDataHolder.setIdentityOracleUser(consentDataSource.getConnection().getMetaData()
                        .getUserName());
                log.info(Constant.MIGRATION_LOG + "Initialized user management database in Oracle.");
            }
        } catch (Exception e) {
            log.error("Error occurred while initializing user management database for Oracle.", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the user manager database connection", e);
            }
        }
    }

    /**
     * Initialize the identity datasource.
     *
     * @throws IdentityException
     */
    private void initIdentityDataSource() throws MigrationClientException {

        try {
            OMElement persistenceManagerConfigElem = IdentityConfigParser.getInstance()
                    .getConfigElement("JDBCPersistenceManager");

            if (persistenceManagerConfigElem == null) {
                String errorMsg = "Identity Persistence Manager configuration is not available in " +
                        "identity.xml file. Terminating the JDBC Persistence Manager " +
                        "initialization. This may affect certain functionality.";
                log.error(errorMsg);
                throw new MigrationClientException(errorMsg);
            }

            OMElement dataSourceElem = persistenceManagerConfigElem.getFirstChildWithName(
                    new QName(IdentityCoreConstants.IDENTITY_DEFAULT_NAMESPACE, "DataSource"));

            if (dataSourceElem == null) {
                String errorMsg = "DataSource Element is not available for JDBC Persistence " +
                        "Manager in identity.xml file. Terminating the JDBC Persistence Manager " +
                        "initialization. This might affect certain features.";
                log.error(errorMsg);
                throw new MigrationClientException(errorMsg);
            }

            OMElement dataSourceNameElem = dataSourceElem.getFirstChildWithName(
                    new QName(IdentityCoreConstants.IDENTITY_DEFAULT_NAMESPACE, "Name"));

            if (dataSourceNameElem != null) {
                String dataSourceName = dataSourceNameElem.getText();
                Context ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup(dataSourceName);
                if (dataSource != null) {
                    log.info(Constant.MIGRATION_LOG + "Initialized the identity database successfully.");
                }
            }
        } catch (NamingException e) {
            String errorMsg = "Error when looking up the Identity Data Source.";
            throw new MigrationClientException(errorMsg, e);
        }
    }

    /**
     * Initialize the registry datasource.
     *
     * @throws MigrationClientException
     */
    private void initRegDataSources() throws MigrationClientException {

        try {
            regDataSources = new HashMap<>();
            File registryConfigXml;
            registryConfigXml = new File(CarbonUtils.getCarbonConfigDirPath(), "registry.xml");
            InputStream inStream = null;
            if (registryConfigXml.exists()) {
                inStream = new FileInputStream(registryConfigXml);
            }

            if (inStream == null) {
                throw new MigrationClientException("Error when reading the Registry Data Source configurations.");
            }
            StAXOMBuilder builder = new StAXOMBuilder(CarbonUtils.replaceSystemVariablesInXml(inStream));
            OMElement configElement = builder.getDocumentElement();

            Iterator dbConfigs = configElement.getChildrenWithName(new QName("dbConfig"));
            // Read Database configurations
            while (dbConfigs.hasNext()) {
                OMElement dbConfig = (OMElement) dbConfigs.next();
                OMElement dataSource = dbConfig.getFirstChildWithName(new QName("dataSource"));
                if (dataSource != null) {
                    String dataSourceName = dataSource.getText();
                    Context ctx;
                    try {
                        ctx = new InitialContext();
                        DataSource regDataSource = (DataSource) ctx.lookup(dataSourceName);
                        if (regDataSource == null) {
                            throw new MigrationClientException(Constant.MIGRATION_LOG + "Error when initiating " +
                                    "registry datasource.");
                        }
                        regDataSources.put(dataSourceName, regDataSource);
                    } catch (NamingException e) {
                        throw new MigrationClientException("Error when reading Registry Data Source configurations.",
                                e);
                    }
                }
            }
        } catch (CarbonException | XMLStreamException | FileNotFoundException e) {
            throw new MigrationClientException("Error when initiating the Registry Data Source.");
        }
    }

    /**
     * Initialize UM Data Source.
     *
     * @throws MigrationClientException
     */
    private void initUMDataSource() throws MigrationClientException {

        umDataSource = DatabaseUtil.getRealmDataSource(ISMigrationServiceDataHolder.getRealmService()
                .getBootstrapRealmConfiguration());
        if (umDataSource == null) {
            String errorMsg = "UM Datasource initialization error.";
            throw new MigrationClientException(errorMsg);
        }
    }

    /**
     * Initialize Consent Data Source.
     *
     * @throws MigrationClientException
     */
    private void initConsentDataSource() throws MigrationClientException {

        ConsentConfigParser configParser = new ConsentConfigParser();
        String dataSourceName = configParser.getConsentDataSource();

        if (dataSourceName == null) {
            String errorMsg = "DataSource Element is not available for Consent management";
            log.error(errorMsg);
            throw new MigrationClientException(errorMsg);
        }

        Context ctx;
        try {
            ctx = new InitialContext();
            consentDataSource = (DataSource) ctx.lookup(dataSourceName);

        } catch (NamingException e) {
            String errorMsg = "Error when looking up the Consent Data Source.";
            throw new MigrationClientException(errorMsg, e);
        }
    }

}
