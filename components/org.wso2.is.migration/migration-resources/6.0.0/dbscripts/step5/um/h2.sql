DROP ALIAS IF EXISTS ALTER_UM_USER;

CREATE ALIAS ALTER_UM_USER AS $$ void alterUmUser(final Connection conn) throws SQLException {
    String const_name = "";
    PreparedStatement ps = conn.prepareStatement("SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME WHERE tc.TABLE_NAME ='UM_USER' AND tc.CONSTRAINT_TYPE = 'UNIQUE' AND kcu.COLUMN_NAME ='UM_USER_ID'");
    ResultSet results =  ps.executeQuery();

    while (results.next()) {
        const_name  = results.getString("CONSTRAINT_NAME");
    }
    if (!const_name.equals("")) {
        ps = conn.prepareStatement("ALTER TABLE UM_USER DROP CONSTRAINT " + const_name);
        ps.execute();
    }

    ps = conn.prepareStatement("ALTER TABLE UM_USER ADD UNIQUE(UM_USER_ID)");
    ps.execute();

    ps = conn.prepareStatement("SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME WHERE tc.TABLE_NAME ='UM_USER' AND tc.CONSTRAINT_TYPE = 'UNIQUE' AND kcu.COLUMN_NAME ='UM_USER_NAME'");
    results =  ps.executeQuery();
    if (results.next() == false) {
        ps = conn.prepareStatement("ALTER TABLE UM_USER ADD UNIQUE(UM_USER_NAME,UM_TENANT_ID)");
        ps.execute();
    }
} $$;

CALL ALTER_UM_USER();

DROP ALIAS IF EXISTS ALTER_UM_USER;

CREATE UNIQUE INDEX IF NOT EXISTS INDEX_UM_USERNAME_UM_TENANT_ID ON UM_USER(UM_USER_NAME, UM_TENANT_ID);

ALTER TABLE UM_TENANT ADD UM_ORG_UUID VARCHAR(36) DEFAULT NULL;
