CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2)
  IS
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  dbms_output.put_line('Skipped');
END;
/

CALL add_index_if_not_exists('CREATE INDEX IDX_TK_VALUE_TYPE ON IDN_OAUTH2_TOKEN_BINDING(TOKEN_BINDING_VALUE, TOKEN_BINDING_TYPE)')
/

CALL add_index_if_not_exists('CREATE INDEX IDX_IOP_CK ON IDN_OIDC_PROPERTY(CONSUMER_KEY)')
/

DROP PROCEDURE add_index_if_not_exists
/

-- --------------------------- REMOVE UNUSED INDICES -----------------------------

-- IDN_OAUTH2_ACCESS_TOKEN --
DROP INDEX IDX_AT_CK_AU
/

DROP INDEX IDX_AT_AU_TID_UD_TS_CKID
/

DROP INDEX IDX_AT_AU_CKID_TS_UT
/

-- IDN_OIDC_PROPERTY --
DROP INDEX IDX_IOP_TID_CK
/
