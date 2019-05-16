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

import java.util.List;

/**
 * Model of a schema. This includes the type of the schema (um, identity, reg, consent), JNDI names of source and
 * target data sources and the table list of the schema.
 */
public class SchemaInfo {

    private String type;
    private String sourceJndiName;
    private String targetJndiName;
    private List<String> tableList;

    public SchemaInfo(String type, String sourceJndiName, String targetJndiName, List<String> tableList) {

        this.type = type;
        this.sourceJndiName = sourceJndiName;
        this.targetJndiName = targetJndiName;
        this.tableList = tableList;
    }

    public String getType() {

        return type;
    }

    public String getSourceJndiName() {

        return sourceJndiName;
    }

    public String getTargetJndiName() {

        return targetJndiName;
    }

    public List<String> getTableList() {

        return tableList;
    }
}
