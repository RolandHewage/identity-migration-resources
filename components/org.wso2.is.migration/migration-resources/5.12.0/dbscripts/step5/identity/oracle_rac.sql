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

DROP PROCEDURE add_index_if_not_exists
/
