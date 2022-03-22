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

BEGIN TRANSACTION;
UPDATE IDP_METADATA SET NAME = 'account.lock.handler.lock.on.max.failed.attempts.enable'
WHERE NAME = 'account.lock.handler.enable';
COMMIT;

INSERT INTO IDN_CONFIG_TYPE(ID, NAME, DESCRIPTION)
SELECT ID, NAME, DESCRIPTION FROM (
	SELECT
		'669b99ca-cdb0-44a6-8cae-babed3b585df' AS ID,
		'Publisher' AS NAME,
		'A resource type to keep the event publisher configurations' AS DESCRIPTION
) temp
WHERE NOT EXISTS (SELECT * FROM IDN_CONFIG_TYPE ict WHERE ict.id = temp.id OR ict.name = temp.name);
