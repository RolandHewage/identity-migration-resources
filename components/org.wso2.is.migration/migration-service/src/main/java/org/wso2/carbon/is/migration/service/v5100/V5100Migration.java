package org.wso2.carbon.is.migration.service.v5100;

import org.wso2.carbon.is.migration.VersionMigration;

public class V5100Migration extends VersionMigration {

    @Override
    public String getPreviousVersion() {
        return "5.9.0";
    }

    @Override
    public String getCurrentVersion() {
        return "5.10.0";
    }
}
