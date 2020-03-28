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

package org.wso2.is.data.sync.system.database;

import org.wso2.is.data.sync.system.util.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds information on the mapping between schema and tables.
 */
public class SchemaTableMapping {

    private List<String> identityMapping = new ArrayList<>(Arrays.asList("FIDO_DEVICE_STORE",
            "IDN_ASSOCIATED_ID",
            "IDN_AUTH_SESSION_STORE",
            "IDN_AUTH_TEMP_SESSION_STORE",
            "IDN_AUTH_WAIT_STATUS",
            "IDN_BASE_TABLE",
            "IDN_CERTIFICATE",
            "IDN_CLAIM",
            "IDN_CLAIM_DIALECT",
            "IDN_CLAIM_MAPPED_ATTRIBUTE",
            "IDN_CLAIM_MAPPING",
            "IDN_CLAIM_PROPERTY",
            "IDN_IDENTITY_META_DATA",
            "IDN_IDENTITY_USER_DATA",
            "IDN_OAUTH1A_ACCESS_TOKEN",
            "IDN_OAUTH1A_REQUEST_TOKEN",
            "IDN_OAUTH2_ACCESS_TOKEN",
            "IDN_OAUTH2_ACCESS_TOKEN_AUDIT",
            "IDN_OAUTH2_ACCESS_TOKEN_SCOPE",
            "IDN_OAUTH2_ACCESS_TOKEN_SCO_SV",
            "IDN_OAUTH2_ACCESS_TOKEN_SV",
            "IDN_OAUTH2_AUTHORIZATION_CODE",
            "IDN_OAUTH2_RESOURCE_SCOPE",
            "IDN_OAUTH2_SCOPE",
            "IDN_OAUTH2_SCOPE_BINDING",
            "IDN_OAUTH2_SCOPE_VALIDATORS",
            "IDN_OAUTH_CONSUMER_APPS",
            "IDN_OIDC_JTI",
            "IDN_OIDC_PROPERTY",
            "IDN_OIDC_REQ_OBJECT_CLAIMS",
            "IDN_OIDC_REQ_OBJECT_REFERENCE",
            "IDN_OIDC_REQ_OBJ_CLAIM_VALUES",
            "IDN_OIDC_SCOPE",
            "IDN_OIDC_SCOPE_CLAIM_MAPPING",
            "IDN_OPENID_ASSOCIATIONS",
            "IDN_OPENID_REMEMBER_ME",
            "IDN_OPENID_USER_RPS",
            "IDN_PASSWORD_HISTORY_DATA",
            "IDN_RECOVERY_DATA",
            "IDN_SAML2_ARTIFACT_STORE",
            "IDN_SAML2_ASSERTION_STORE",
            "IDN_SCIM_GROUP",
            "IDN_STS_STORE",
            "IDN_THRIFT_SESSION",
            "IDN_USER_ACCOUNT_ASSOCIATION",
            "IDP",
            "IDP_AUTHENTICATOR",
            "IDP_AUTHENTICATOR_PROPERTY",
            "IDP_CLAIM",
            "IDP_CLAIM_MAPPING",
            "IDP_LOCAL_CLAIM",
            "IDP_METADATA",
            "IDP_PROVISIONING_CONFIG",
            "IDP_PROVISIONING_ENTITY",
            "IDP_PROV_CONFIG_PROPERTY",
            "IDP_ROLE",
            "IDP_ROLE_MAPPING",
            "SP_APP",
            "SP_AUTH_SCRIPT",
            "SP_AUTH_STEP",
            "SP_CLAIM_DIALECT",
            "SP_CLAIM_MAPPING",
            "SP_FEDERATED_IDP",
            "SP_INBOUND_AUTH",
            "SP_METADATA",
            "SP_PROVISIONING_CONNECTOR",
            "SP_REQ_PATH_AUTHENTICATOR",
            "SP_ROLE_MAPPING",
            "SP_TEMPLATE"
    ));

    private List<String> umMapping = new ArrayList<>(Arrays.asList("UM_ACCOUNT_MAPPING",
            "UM_CLAIM",
            "UM_CLAIM_BEHAVIOR",
            "UM_DIALECT",
            "UM_DOMAIN",
            "UM_HYBRID_REMEMBER_ME",
            "UM_HYBRID_ROLE",
            "UM_HYBRID_USER_ROLE",
            "UM_MODULE",
            "UM_MODULE_ACTIONS",
            "UM_PERMISSION",
            "UM_PROFILE_CONFIG",
            "UM_ROLE",
            "UM_ROLE_PERMISSION",
            "UM_SHARED_USER_ROLE",
            "UM_SYSTEM_ROLE",
            "UM_SYSTEM_USER",
            "UM_SYSTEM_USER_ROLE",
            "UM_TENANT",
            "UM_USER",
            "UM_USER_ATTRIBUTE",
            "UM_USER_PERMISSION",
            "UM_USER_ROLE"
    ));

    private List<String> regMapping = new ArrayList<>(Arrays.asList("REG_ASSOCIATION",
            "REG_CLUSTER_LOCK",
            "REG_COMMENT",
            "REG_CONTENT",
            "REG_CONTENT_HISTORY",
            "REG_LOG",
            "REG_PATH",
            "REG_PROPERTY",
            "REG_RATING",
            "REG_RESOURCE",
            "REG_RESOURCE_COMMENT",
            "REG_RESOURCE_HISTORY",
            "REG_RESOURCE_PROPERTY",
            "REG_RESOURCE_RATING",
            "REG_RESOURCE_TAG",
            "REG_SNAPSHOT",
            "REG_TAG"
    ));

    private List<String> consentMapping = new ArrayList<>(Arrays.asList("CM_CONSENT_RECEIPT_PROPERTY",
            "CM_PII_CATEGORY",
            "CM_PURPOSE",
            "CM_PURPOSE_CATEGORY",
            "CM_PURPOSE_PII_CAT_ASSOC",
            "CM_RECEIPT",
            "CM_RECEIPT_SP_ASSOC",
            "CM_SP_PURPOSE_ASSOC",
            "CM_SP_PURPOSE_PII_CAT_ASSOC",
            "CM_SP_PURPOSE_PURPOSE_CAT_ASSC"
    ));

    public Map<String, List<String>> getSchemaTableMapping() {

        Map<String, List<String>> schemaTableMapping = new HashMap<>();
        schemaTableMapping.put(Constant.SCHEMA_TYPE_IDENTITY, identityMapping);
        schemaTableMapping.put(Constant.SCHEMA_TYPE_UM, umMapping);
        schemaTableMapping.put(Constant.SCHEMA_TYPE_REGISTRY, regMapping);
        schemaTableMapping.put(Constant.SCHEMA_TYPE_CONSENT, consentMapping);

        return schemaTableMapping;
    }
}
