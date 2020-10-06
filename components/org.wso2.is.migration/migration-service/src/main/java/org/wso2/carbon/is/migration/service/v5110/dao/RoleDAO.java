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

import edu.emory.mathcs.backport.java.util.Arrays;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.is.migration.service.v5110.bean.RoleInfo;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RoleDAO implementation.
 */
public class RoleDAO {

    public static final String RETRIEVE_EXTERNAL_ROLE_DATA =
            "SELECT DISTINCT(UM_ROLE_PERMISSION.UM_ROLE_NAME), UM_ROLE_PERMISSION.UM_DOMAIN_ID, UM_DOMAIN"
                    + ".UM_DOMAIN_NAME, UM_ROLE_PERMISSION.UM_TENANT_ID FROM UM_ROLE_PERMISSION, UM_DOMAIN WHERE "
                    + "UM_ROLE_PERMISSION.UM_DOMAIN_ID = UM_DOMAIN.UM_DOMAIN_ID AND UM_ROLE_PERMISSION.UM_TENANT_ID ="
                    + " UM_DOMAIN.UM_TENANT_ID AND UM_ROLE_PERMISSION.UM_TENANT_ID=:UM_TENANT_ID; AND UM_DOMAIN"
                    + ".UM_DOMAIN_NAME NOT IN (:HYBRID_DOMAINS;)";

    public static final String UPDATE_ROLE_NAME_SQL = "UPDATE UM_ROLE_PERMISSION SET UM_ROLE_NAME=:NEW_UM_ROLE_NAME;, "
            + "UM_DOMAIN_ID=(SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME=:INTERNAL_DOMAIN; AND "
            + "UM_TENANT_ID=:UM_TENANT_ID;) WHERE UM_ROLE_NAME=:UM_ROLE_NAME; AND UM_TENANT_ID=:UM_TENANT_ID; AND "
            + "UM_DOMAIN_ID=:UM_DOMAIN_ID;";

    public static final String DELETE_ADMIN_GROUP_DATA =
            "DELETE FROM UM_ROLE_PERMISSION WHERE UM_ROLE_NAME=:UM_ROLE_NAME; AND UM_TENANT_ID=:UM_TENANT_ID; AND "
                    + "UM_DOMAIN_ID NOT IN (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = "
                    + ":UM_DOMAIN_NAME; AND UM_TENANT_ID=:UM_TENANT_ID;)";

    public static final String DELETE_TENANT_ADMIN_ROLE_DATA =
            "DELETE FROM UM_ROLE_PERMISSION WHERE UM_ROLE_NAME=:UM_ROLE_NAME; AND UM_TENANT_ID=:UM_TENANT_ID; AND "
                    + "UM_DOMAIN_ID IN (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = "
                    + ":UM_DOMAIN_NAME; AND UM_TENANT_ID=:UM_TENANT_ID;)";

    public static final String HYBRID_DOMAINS = "HYBRID_DOMAINS";
    public static final String NEW_UM_ROLE_NAME = "NEW_UM_ROLE_NAME";
    public static final String UM_ROLE_NAME = "UM_ROLE_NAME";
    public static final String UM_TENANT_ID = "UM_TENANT_ID";
    public static final String UM_DOMAIN_ID = "UM_DOMAIN_ID";
    public static final String UM_DOMAIN_NAME = "UM_DOMAIN_NAME";
    public static final String TENANT_ID = "TENANT_ID";
    public static final String INTERNAL_DOMAIN = "INTERNAL_DOMAIN";

    private static RoleDAO instance = new RoleDAO();

    private RoleDAO() {

    }

    public static RoleDAO getInstance() {

        return instance;
    }

    /**
     * Method to retrieve external role data which has permissions assigned.
     *
     * @param connection Database connection.
     * @throws SQLException SQLException.
     */
    public List<RoleInfo> getExternalRoleData(Connection connection, int tenantID) throws SQLException {

        List<RoleInfo> externalRoles = new ArrayList<>();
        Map<String, Integer> repetition = new HashMap<>();
        repetition.put(HYBRID_DOMAINS, 4);
        try (NamedPreparedStatement preparedStatement = new NamedPreparedStatement(connection,
                RETRIEVE_EXTERNAL_ROLE_DATA, repetition)) {
            List<String> hybridDomains = new ArrayList<>(Arrays.asList(new String[] {
                    UserCoreConstants.APPLICATION_DOMAIN.toUpperCase(Locale.ENGLISH),
                    UserCoreConstants.INTERNAL_DOMAIN.toUpperCase(Locale.ENGLISH),
                    UserCoreConstants.SYSTEM_DOMAIN_NAME.toUpperCase(Locale.ENGLISH),
                    UserCoreConstants.WORKFLOW_DOMAIN.toUpperCase(Locale.ENGLISH)
            }));
            preparedStatement.setInt(UM_TENANT_ID, tenantID);
            preparedStatement.setString(HYBRID_DOMAINS, hybridDomains);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    RoleInfo roleInfo = new RoleInfo();
                    roleInfo.setRoleName(resultSet.getString(UM_ROLE_NAME));
                    roleInfo.setDomainID(resultSet.getInt(UM_DOMAIN_ID));
                    roleInfo.setDomainName(resultSet.getString(UM_DOMAIN_NAME));
                    roleInfo.setTenantID(resultSet.getInt(UM_TENANT_ID));
                    externalRoles.add(roleInfo);
                }
            }
        }
        return externalRoles;
    }

    /**
     * Method to transfer permissions to new role.
     *
     * @param connection Database connection.
     * @param roleInfo   Role details.
     * @throws SQLException SQLException.
     */
    public void transferPermissionsOfRole(Connection connection, RoleInfo roleInfo, boolean isAdminRole)
            throws SQLException {

        try (NamedPreparedStatement preparedStatement = new NamedPreparedStatement(connection, UPDATE_ROLE_NAME_SQL)) {

            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);
            preparedStatement.setString(NEW_UM_ROLE_NAME, roleInfo.getInternalRoleName(isAdminRole));
            preparedStatement.setString(UM_ROLE_NAME, roleInfo.getRoleName());
            preparedStatement.setString(INTERNAL_DOMAIN, UserCoreConstants.INTERNAL_DOMAIN.toUpperCase(Locale.ENGLISH));
            preparedStatement.setInt(UM_TENANT_ID, roleInfo.getTenantID());
            preparedStatement.setInt(UM_DOMAIN_ID, roleInfo.getDomainID());
            preparedStatement.executeUpdate();
            connection.commit();
            connection.setAutoCommit(autoCommitStatus);
        }
    }

    /**
     * Method to delete admin group permissions.
     *
     * @param connection     Database connection.
     * @param adminGroupName Admin group name.
     * @param tenantID       Tenant ID.
     * @return No of affected rows.
     * @throws SQLException SQLException.
     */
    public int deleteAdminGroupPermissions(Connection connection, String adminGroupName, int tenantID)
            throws SQLException {

        try (NamedPreparedStatement preparedStatement = new NamedPreparedStatement(connection,
                DELETE_ADMIN_GROUP_DATA)) {

            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);
            preparedStatement.setString(UM_ROLE_NAME, adminGroupName);
            preparedStatement.setString(UM_DOMAIN_NAME, UserCoreConstants.INTERNAL_DOMAIN.toUpperCase(Locale.ENGLISH));
            preparedStatement.setInt(UM_TENANT_ID, tenantID);
            int rows = preparedStatement.executeUpdate();
            connection.commit();
            connection.setAutoCommit(autoCommitStatus);
            return rows;
        }
    }

    /**
     * Method to delete tenant admin role permissions.
     *
     * @param connection     Database connection.
     * @param adminGroupName Admin group name.
     * @param tenantID       Tenant ID.
     * @return No of affected rows.
     * @throws SQLException SQLException.
     */
    public void deleteTenantAdminRolePermissions(Connection connection, String adminGroupName, int tenantID)
            throws SQLException {

        try (NamedPreparedStatement preparedStatement = new NamedPreparedStatement(connection,
                DELETE_TENANT_ADMIN_ROLE_DATA)) {

            boolean autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false);
            preparedStatement.setString(UM_ROLE_NAME, adminGroupName);
            preparedStatement.setString(UM_DOMAIN_NAME, UserCoreConstants.INTERNAL_DOMAIN.toUpperCase(Locale.ENGLISH));
            preparedStatement.setInt(UM_TENANT_ID, tenantID);
            preparedStatement.executeUpdate();
            connection.commit();
            connection.setAutoCommit(autoCommitStatus);
        }
    }
}

