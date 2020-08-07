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
package org.wso2.carbon.is.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.is.migration.service.Migrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkFlowUtil {

    private static final String CREDENTIAL = "Credential";
    private static final Logger log = LoggerFactory.getLogger(WorkFlowUtil.class);

    public static void migrateWorkFlowRequestCredential(Migrator migrator) throws MigrationClientException {

        try {
            List<WorkflowRequest> workFlowRequestList = WorkFlowDAO.getInstance().getAllWorkflowRequests(migrator);
            List<WorkflowRequest> updatedtworkFlowRequestList =
                    transformWorkFlowCredentialsFromOldToNewEncryption(workFlowRequestList);
            WorkFlowDAO.getInstance().updateWorkFlowRequests(updatedtworkFlowRequestList, migrator);
        } catch (MigrationClientException e) {
            String errorMessage = "Error while migrating workflow request ";
            if (migrator.isContinueOnError()) {
                log.error(errorMessage, e);
            } else {
                throw new MigrationClientException(errorMessage, e);
            }
        }
    }

    public static List<WorkflowRequest> transformWorkFlowCredentialsFromOldToNewEncryption(
            List<WorkflowRequest> workflowRequests)
            throws MigrationClientException {

        List<WorkflowRequest> updatedWorkFlowRequests = new ArrayList<>();
        for (WorkflowRequest workflowRequest : workflowRequests) {
            Map<String, Object> requestParams = new HashMap<String, Object>();
            for (RequestParameter parameter : workflowRequest.getRequestParameters()) {
                if (CREDENTIAL.equals(parameter.getName())) {
                    Object encryptedCredential = parameter.getValue();
                    String newEncryptedWorkFlowCredential =
                            EncryptionUtil.transformToSymmetric(encryptedCredential.toString());
                    parameter.setValue(newEncryptedWorkFlowCredential);
                }
            }

            WorkflowRequest updatedWorkFlowRequest = workflowRequest;
            updatedWorkFlowRequests.add(updatedWorkFlowRequest);

        }
        return updatedWorkFlowRequests;
    }

}
