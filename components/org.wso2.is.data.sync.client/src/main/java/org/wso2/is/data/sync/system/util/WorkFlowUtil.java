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
package org.wso2.is.data.sync.system.util;

import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.is.data.sync.system.exception.SyncClientException;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest.CREDENTIAL;

/**
 * Util class to transform encryption of workflow credentials.
 */
public class WorkFlowUtil {

    public static WorkflowRequest transformWorkFlowCredentialsFromOldToNewEncryption(
            WorkflowRequest workflowRequest) throws SyncClientException {

        Map<String, Object> requestParams = new HashMap<String, Object>();
        if(workflowRequest != null) {
            for (RequestParameter parameter : workflowRequest.getRequestParameters()) {
                if (CREDENTIAL.equals(parameter.getName())) {
                    Object encryptedCredential = parameter.getValue();
                    String newEncryptedWorkFlowCredential =
                            EncryptionUtil.transformToSymmetric(encryptedCredential.toString());
                    parameter.setValue(newEncryptedWorkFlowCredential);
                }
            }
        }

        return workflowRequest;
    }
}
