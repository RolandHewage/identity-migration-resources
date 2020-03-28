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

package org.wso2.is.data.sync.system.pipeline.transform.model;

/**
 * AuthorizationCodeInfo.
 */
public class AuthorizationCodeInfo {

    private String authorizationCode;
    private String decryptedAuthorizationCode;
    private String authorizationCodeHash;
    private int idpId;

    public AuthorizationCodeInfo(String authorizationCode, String authorizationCodeHash, int idpId) {

        this.authorizationCode = authorizationCode;
        this.authorizationCodeHash = authorizationCodeHash;
        this.idpId = idpId;
    }

    public AuthorizationCodeInfo(String authorizationCode) {

        this.authorizationCode = authorizationCode;
    }

    public AuthorizationCodeInfo(String authorizationCode, String authorizationCodeHash) {

        this.authorizationCode = authorizationCode;
        this.authorizationCodeHash = authorizationCodeHash;
    }

    public String getAuthorizationCode() {

        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {

        this.authorizationCode = authorizationCode;
    }

    public String getDecryptedAuthorizationCode() {

        return decryptedAuthorizationCode;
    }

    public void setDecryptedAuthorizationCode(String decryptedAuthorizationCode) {

        this.decryptedAuthorizationCode = decryptedAuthorizationCode;
    }

    public String getAuthorizationCodeHash() {

        return authorizationCodeHash;
    }

    public void setAuthorizationCodeHash(String authorizationCodeHash) {

        this.authorizationCodeHash = authorizationCodeHash;
    }

    public int getIdpId() {

        return idpId;
    }

    public void setIdpId(int idpId) {

        this.idpId = idpId;
    }
}
