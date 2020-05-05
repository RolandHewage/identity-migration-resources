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

package org.wso2.carbon.is.migration.service.v570.dao;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.util.LambdaExceptionUtils;
import org.wso2.carbon.identity.oauth.dto.ScopeDTO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

/**
 * This class contains methods related to OIDC scopes manipulation.
 *
 * NOTE: This class duplicates the methods in the ScopeClaimMappingDAOImpl.java class in the OAuth component.
 * Hence, changes to these methods should comply with the ScopeClaimMappingDAOImpl.java vise versa.
 *
 * @since 1.0.92
 */
public class OIDCScopeDAO {

    private static final Logger log = LoggerFactory.getLogger(OIDCScopeDAO.class);

    private static final String SQL_INSERT_SCOPES = "INSERT INTO IDN_OIDC_SCOPE (NAME,TENANT_ID) VALUES (?, ?)";
    private static final String SQL_INSERT_CLAIMS =
            "INSERT INTO IDN_OIDC_SCOPE_CLAIM_MAPPING (SCOPE_ID, EXTERNAL_CLAIM_ID) " +
                    "SELECT ?,IDN_CLAIM.ID FROM IDN_CLAIM LEFT JOIN IDN_CLAIM_DIALECT " +
                    "ON IDN_CLAIM_DIALECT.ID = IDN_CLAIM.DIALECT_ID WHERE CLAIM_URI=? " +
                    "AND IDN_CLAIM_DIALECT.DIALECT_URI='http://wso2.org/oidc/claim' " +
                    "AND IDN_CLAIM_DIALECT.TENANT_ID=?";
    private static final String SQL_IS_EXISTS_SCOPE = "SELECT ID FROM IDN_OIDC_SCOPE WHERE NAME=? AND TENANT_ID=?";

    private DataSource dataSource;

    public OIDCScopeDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Persist scopes in the IDN_OIDC_SCOPES table.
     *
     * @param tenantId Tenant id of the scope.
     * @param scopeClaimsList List of scopes that should be persisted.
     * @throws IdentityOAuth2Exception When an error occurs while persisting data.
     */
    public void addScopes(int tenantId, List<ScopeDTO> scopeClaimsList) throws IdentityOAuth2Exception {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
        scopeClaimsList.forEach(LambdaExceptionUtils.rethrowConsumer((scopeDTO) -> {
            String scope = scopeDTO.getName();
            String[] claims = scopeDTO.getClaim();
            try {
                if (!this.isScopeExist(scope, tenantId)) {
                    int scopeClaimMappingId = jdbcTemplate.withTransaction(
                            (template) -> template.executeInsert(SQL_INSERT_SCOPES,
                                    (preparedStatement) -> {
                                        preparedStatement.setString(1, scope);
                                        preparedStatement.setInt(2, tenantId);
                                    }, null, true));
                    if (scopeClaimMappingId > 0 && ArrayUtils.isNotEmpty(claims)) {
                        Set<String> claimsSet = new HashSet<>(Arrays.asList(claims));
                        this.insertClaims(tenantId, scopeClaimMappingId, claimsSet);
                    }
                    if (log.isDebugEnabled() && ArrayUtils.isNotEmpty(claims)) {
                        log.debug("The scope: " + scope + " and the claims: " + Arrays.asList(claims) +
                                "are successfully" + " inserted for the tenant: " + tenantId);
                    }
                } else {
                    String errorMessage =
                            "Error while adding scopes. Duplicate scopes can not be added for the tenant: " + tenantId;
                    throw new IdentityOAuth2Exception(errorMessage);
                }
            } catch (TransactionException var8) {
                String errorMessagex = "Error while persisting new claims for the scope for the tenant: " + tenantId;
                throw new IdentityOAuth2Exception(errorMessagex, var8);
            }
        }));
    }

    private void insertClaims(int tenantId, int scopeId, Set<String> claimsList)
            throws IdentityOAuth2Exception {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
        byte scopeClaimMappingId = -1;

        try {
            jdbcTemplate.withTransaction((template) -> {
                template.executeBatchInsert(SQL_INSERT_CLAIMS, (preparedStatement) -> {
                    if (CollectionUtils.isNotEmpty(claimsList)) {
                        for (String claim : claimsList) {
                            preparedStatement.setInt(1, scopeId);
                            preparedStatement.setString(2, claim);
                            preparedStatement.setInt(3, tenantId);
                            preparedStatement.addBatch();
                            if (log.isDebugEnabled()) {
                                log.debug("Claim value :" + claim + " is added to the batch.");
                            }
                        }
                    }

                }, scopeClaimMappingId);
                return null;
            });
        } catch (TransactionException var8) {
            String errorMessage = "Error when storing oidc claims for tenant: " + tenantId;
            throw new IdentityOAuth2Exception(errorMessage, var8);
        }
    }

    private boolean isScopeExist(String scope, int tenantId) throws IdentityOAuth2Exception {

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
            Integer scopeId = jdbcTemplate.withTransaction((template) -> template
                    .fetchSingleRecord(SQL_IS_EXISTS_SCOPE, (resultSet, rowNumber) -> resultSet.getInt(1),
                            (preparedStatement) -> {
                                preparedStatement.setString(1, scope);
                                preparedStatement.setInt(2, tenantId);
                            }));
            if (scopeId == null) {
                return false;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Scope id: " + scopeId + "is returned for the tenant: " + tenantId + "and scope: "
                            + scope);
                }
                return true;
            }
        } catch (TransactionException ex) {
            String errorMessage = "Error fetching data for oidc scope: " + scope;
            throw new IdentityOAuth2Exception(errorMessage, ex);
        }
    }
}
