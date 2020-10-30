CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2)
  IS
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  dbms_output.put_line( 'Skipped ');
END;
/

CALL add_index_if_not_exists('CREATE INDEX UM_ATTR_NAME_VALUE_IDX ON UM_USER_ATTRIBUTE(UM_ATTR_NAME, UM_ATTR_VALUE)')
/

DROP PROCEDURE add_index_if_not_exists
/
