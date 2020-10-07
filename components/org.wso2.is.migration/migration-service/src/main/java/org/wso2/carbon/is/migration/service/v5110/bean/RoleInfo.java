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
package org.wso2.carbon.is.migration.service.v5110.bean;

import org.wso2.carbon.user.core.UserCoreConstants;

/**
 * RoleInfo.
 */
public class RoleInfo {

    private String roleName;
    private int domainID;
    private String domainName;
    private int tenantID;

    public String getRoleName() {

        return roleName;
    }

    public void setRoleName(String roleName) {

        this.roleName = roleName;
    }

    public int getDomainID() {

        return domainID;
    }

    public void setDomainID(int domainID) {

        this.domainID = domainID;
    }

    public String getDomainName() {

        return domainName;
    }

    public void setDomainName(String domainName) {

        this.domainName = domainName;
    }

    public int getTenantID() {

        return tenantID;
    }

    public void setTenantID(int tenantID) {

        this.tenantID = tenantID;
    }

    public String getDomainQualifiedRoleName() {

        return domainName + UserCoreConstants.DOMAIN_SEPARATOR + roleName;
    }

    public String getInternalRoleName(boolean isAdminRole) {

        return isAdminRole ? roleName :
                UserCoreConstants.INTERNAL_SYSTEM_ROLE_PREFIX + domainName.toLowerCase().concat("_") + roleName;
    }
}
