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

package org.wso2.is.data.sync.system.database.dialect;

import org.wso2.is.data.sync.system.database.TableMetaData;

/**
 * Model of a database trigger. This model holds meta data related to a trigger.
 */
public class Trigger {

    private String name;
    private String sourceTableName;
    private String targetTableName;
    private String triggerTiming; //AFTER | BEFORE
    private String triggerEvent; //INSERT | UPDATE | DELETE
    private TableMetaData tableMetaData;
    private String selectionPolicy; //FOR EACH ROW
    private String foreignKey;

    public Trigger(String name, String sourceTableName, String targetTableName, String triggerEvent,
                   TableMetaData tableMetaData,
                   String selectionPolicy, String triggerTiming) {

        this.name = name;
        this.sourceTableName = sourceTableName;
        this.targetTableName = targetTableName;
        this.triggerTiming = triggerTiming;
        this.triggerEvent = triggerEvent;
        this.tableMetaData = tableMetaData;
        this.selectionPolicy = selectionPolicy;
    }

    public Trigger(String name, String sourceTableName, String targetTableName, String triggerEvent,
                   TableMetaData tableMetaData,
                   String selectionPolicy, String triggerTiming,String foreignKey) {

        this.name = name;
        this.sourceTableName = sourceTableName;
        this.targetTableName = targetTableName;
        this.triggerTiming = triggerTiming;
        this.triggerEvent = triggerEvent;
        this.tableMetaData = tableMetaData;
        this.selectionPolicy = selectionPolicy;
        this.foreignKey = foreignKey;
    }

    public String getName() {

        return name;
    }

    public String getSourceTableName() {

        return sourceTableName;
    }

    public String getTargetTableName() {

        return targetTableName;
    }

    public String getTriggerTiming() {

        return triggerTiming;
    }

    public String getTriggerEvent() {

        return triggerEvent;
    }

    public TableMetaData getTableMetaData() {

        return tableMetaData;
    }

    public String getSelectionPolicy() {

        return selectionPolicy;
    }

    public String getForeignKey() {

        return foreignKey;
    }
}
