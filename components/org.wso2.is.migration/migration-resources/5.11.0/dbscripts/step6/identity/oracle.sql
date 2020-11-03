CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2)
  IS
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  dbms_output.put_line( 'Skipped ');
END;
/

CALL add_index_if_not_exists('CREATE INDEX IDX_REMOTE_FETCH_REVISION_CONFIG_ID ON IDN_REMOTE_FETCH_REVISIONS(CONFIG_ID)')
/

DROP PROCEDURE add_index_if_not_exists
/
