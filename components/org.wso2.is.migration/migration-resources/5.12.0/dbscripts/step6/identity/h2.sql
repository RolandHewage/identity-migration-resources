--<![CDATA[Start of Procedure]]>--
CREATE ALIAS MIGRATE_IDN_OAUTH2_DEVICE_FLOW AS $$
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
@CODE
void migrateIdnOauthDeviceFlow(final Connection conn) throws SQLException {

    boolean isExists = false;
    PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='IDN_OAUTH2_DEVICE_FLOW' AND COLUMN_NAME='QUANTIFIER'");
    ResultSet results =  ps.executeQuery();
    while (results.next()) {
        isExists = results.getBoolean(1);
    }

    if (!isExists) {
        ps = conn.prepareStatement("ALTER TABLE IDN_OAUTH2_DEVICE_FLOW ADD QUANTIFIER INTEGER DEFAULT 0");
        ps.execute();
        ps = conn.prepareStatement("ALTER TABLE IDN_OAUTH2_DEVICE_FLOW DROP CONSTRAINT USER_CODE");
        ps.execute();
        ps = conn.prepareStatement("ALTER TABLE IDN_OAUTH2_DEVICE_FLOW ADD CONSTRAINT USRCDE_QNTFR_CONSTRAINT UNIQUE (USER_CODE, QUANTIFIER)");
        ps.execute();
    }
}
$$;
--<![CDATA[End of Procedure]]>--
