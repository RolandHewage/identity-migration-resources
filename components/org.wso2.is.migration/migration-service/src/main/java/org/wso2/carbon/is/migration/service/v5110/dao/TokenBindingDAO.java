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
package org.wso2.carbon.is.migration.service.v5110.dao;

import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.is.migration.service.v5110.bean.OIDCSPInfo;
import org.wso2.carbon.is.migration.service.v530.SQLConstants;
import org.wso2.carbon.is.migration.service.v530.util.JDBCPersistenceUtil;
import org.wso2.carbon.utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * TokenBindingDAO implementation.
 */
public class TokenBindingDAO {

    public static final String RETRIEVE_OIDC_SERVICE_PROVIDERS =
            "SELECT DISTINCT CONSUMER_KEY, TENANT_ID FROM IDN_OIDC_PROPERTY WHERE PROPERTY_KEY = ?" +
                    "AND PROPERTY_VALUE = ?";
    public static final String CHECK_TOKEN_BINDING_PROPERTY_EXISTS = "SELECT 1 FROM IDN_OIDC_PROPERTY WHERE " +
            "CONSUMER_KEY = ? AND TENANT_ID = ? AND PROPERTY_KEY = ?";
    public static final String ADD_TOKEN_BINDING_VALIDATION_PROPERTY = "INSERT INTO IDN_OIDC_PROPERTY (TENANT_ID, " +
            "CONSUMER_KEY, PROPERTY_KEY, PROPERTY_VALUE) VALUES (?, ?, ?, ?)";
    public static final String TENANT_ID = "TENANT_ID";
    public static final String CONSUMER_KEY = "CONSUMER_KEY";
    public static final String TOKEN_BINDING_TYPE = "tokenBindingType";
    public static final String COOKIE = "cookie";
    public static final String TOKEN_BINDING_VALIDATION = "tokenBindingValidation";

    private static TokenBindingDAO instance = new TokenBindingDAO();

    private TokenBindingDAO() {
    }

    public static TokenBindingDAO getInstance() {

        return instance;
    }

    /**
     * Method to retrieve OIDC service providers tenant id and consumer key.
     *
     * @param connection Database connection.
     */
    public List<OIDCSPInfo> getOIDCServiceProvidersData(Connection connection) throws SQLException {

        List<OIDCSPInfo> oidcSpInfoList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(RETRIEVE_OIDC_SERVICE_PROVIDERS)) {
            preparedStatement.setString(1, TOKEN_BINDING_TYPE);
            preparedStatement.setString(2, COOKIE);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    OIDCSPInfo oidcspInfo = new OIDCSPInfo();
                    oidcspInfo.setTenantID(resultSet.getInt(TENANT_ID));
                    oidcspInfo.setConsumerKey(resultSet.getString(CONSUMER_KEY));
                    oidcSpInfoList.add(oidcspInfo);
                }
            }
        }
        return oidcSpInfoList;
    }

    /**
     * Method to check whether token binding validation property exists.
     *
     * @param connection  Database connection.
     * @param consumerKey Consumer Key of the OIDC Service Provider.
     * @param tenantID    Tenant ID of the OIDC Service Provider.
     * @throws SQLException SQLException.
     */
    public boolean isTokenBindingValidationPropertyExists(Connection connection, String consumerKey, int tenantID)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(CHECK_TOKEN_BINDING_PROPERTY_EXISTS)) {
            preparedStatement.setString(1, consumerKey);
            preparedStatement.setInt(2, tenantID);
            preparedStatement.setString(3, TOKEN_BINDING_VALIDATION);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to add missing token binding validation property.
     *
     * @param connection  Database connection.
     * @param consumerKey Consumer Key of the OIDC Service Provider.
     * @param tenantID    Tenant ID of the OIDC Service Provider.
     * @throws MigrationClientException MigrationClientException.
     */
    public void addTokenBindingValidationParameter(Connection connection, String consumerKey, int tenantID)
            throws MigrationClientException {

        PreparedStatement prepStmt = null;
        try {
            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            prepStmt = connection.prepareStatement(ADD_TOKEN_BINDING_VALIDATION_PROPERTY,
                    new String[]{DBUtils.getConvertedAutoGeneratedColumnName
                            (dbProductName, SQLConstants.ID_COLUMN)});
            prepStmt.setInt(1, tenantID);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, TOKEN_BINDING_VALIDATION);
            prepStmt.setString(4, Boolean.TRUE.toString());
            prepStmt.executeUpdate();
            JDBCPersistenceUtil.commitTransaction(connection);
            connection.setAutoCommit(autoCommitStatus);
        } catch (SQLException e) {
            JDBCPersistenceUtil.rollbackTransaction(connection);
            throw new MigrationClientException("Error while inserting default token binding validation property " +
                    "value.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
        }
    }
}
