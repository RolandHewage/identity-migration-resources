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
 * TokenInfo.
 */
public class TokenInfo {

    private String accessToken;
    private String refreshToken;
    private String decryptedAccessToken;
    private String decryptedRefreshToken;
    private String accessTokenHash;
    private String refreshTokenHash;
    private int idpId;

    public TokenInfo(String accessToken, String refreshToken, String accessTokenHash, String refreshTokenHash,
                     int idpId) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.idpId = idpId;
    }

    public TokenInfo(String accessToken, String refreshToken) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public TokenInfo(String accessToken, String refreshToken, String accessTokenHash, String refreshTokenHash) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getAccessToken() {

        return accessToken;
    }

    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    public String getRefreshToken() {

        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {

        this.refreshToken = refreshToken;
    }

    public String getAccessTokenHash() {

        return accessTokenHash;
    }

    public void setAccessTokenHash(String accessTokenHash) {

        this.accessTokenHash = accessTokenHash;
    }

    public String getRefreshTokenHash() {

        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {

        this.refreshTokenHash = refreshTokenHash;
    }

    public String getDecryptedAccessToken() {

        return decryptedAccessToken;
    }

    public void setDecryptedAccessToken(String decryptedAccessToken) {

        this.decryptedAccessToken = decryptedAccessToken;
    }

    public String getDecryptedRefreshToken() {

        return decryptedRefreshToken;
    }

    public void setDecryptedRefreshToken(String decryptedRefreshToken) {

        this.decryptedRefreshToken = decryptedRefreshToken;
    }

    public int getIdpId() {

        return idpId;
    }

    public void setIdpId(int idpId) {

        this.idpId = idpId;
    }
}
