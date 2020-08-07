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
package org.wso2.is.data.sync.system.pipeline.transform.model;

/**
 * POJO class for TOTP secret related data.
 */
public class TotpSecretDataInfo {
    private String tenantId;

    private String userName;
    private String encryptedSeceretkeyValue;
    private String dataKey;

    public TotpSecretDataInfo(String tenantId, String userName, String encryptedSeceretkeyValue, String dataKey) {

        this.tenantId = tenantId;
        this.userName = userName;
        this.encryptedSeceretkeyValue = encryptedSeceretkeyValue;
        this.dataKey = dataKey;
    }

    public String getTenantId() {

        return tenantId;
    }

    public void setTenantId(String tenantId) {

        this.tenantId = tenantId;
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String userName) {

        this.userName = userName;
    }

    public String getEncryptedSeceretkeyValue() {

        return encryptedSeceretkeyValue;
    }

    public void setEncryptedSeceretkeyValue(String encryptedSeceretkeyValue) {

        this.encryptedSeceretkeyValue = encryptedSeceretkeyValue;
    }

    public String getDataKey() {

        return dataKey;
    }

    public void setDataKey(String dataKey) {

        this.dataKey = dataKey;
    }

}
