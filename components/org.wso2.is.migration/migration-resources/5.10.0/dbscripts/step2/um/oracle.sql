DECLARE
	sql_stmnt VARCHAR2(200);

   BEGIN
	sql_stmnt :='ALTER TABLE UM_USER ADD (UM_USER_ID CHAR(36) DEFAULT LOWER(regexp_replace(rawtohex(sys_guid()), ''([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})'', ''\1-\2-\3-\4-\5'')))';
	execute immediate sql_stmnt;
    dbms_output.put_line('created');
    exception WHEN OTHERS THEN
    dbms_output.put_line('skipped');
END;
/
