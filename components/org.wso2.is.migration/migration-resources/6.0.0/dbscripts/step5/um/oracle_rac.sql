DECLARE
	con_name0    VARCHAR2(100);
	con_name1    VARCHAR2(100);
	command      VARCHAR2(200);
	databasename VARCHAR2(100);

BEGIN
	SELECT sys_context('userenv', 'current_schema') INTO databasename FROM dual;
	
	BEGIN
		SELECT a.constraint_name
		INTO con_name0
		FROM all_cons_columns a
		JOIN all_constraints c ON a.owner = c.owner AND a.constraint_name = c.constraint_name
		WHERE c.constraint_type = 'U' AND a.table_name = 'UM_USER' AND UPPER(a.OWNER) = UPPER(databasename) AND a.column_name='UM_USER_ID';
		
		IF con_name0 IS NOT NULL THEN
			command := 'ALTER TABLE UM_USER DROP CONSTRAINT ' || con_name0;
			dbms_output.Put_line(command);
			EXECUTE IMMEDIATE command;
		END IF;
		
		EXCEPTION WHEN NO_DATA_FOUND THEN
			dbms_output.Put_line('Unique key not found');
	END;
	
	BEGIN
		SELECT a.constraint_name
		INTO con_name1
		FROM all_cons_columns a
		JOIN all_constraints c ON a.owner = c.owner AND a.constraint_name = c.constraint_name
		WHERE c.constraint_type = 'U' AND a.table_name = 'UM_USER' AND UPPER(a.OWNER) = UPPER(databasename) AND a.column_name='UM_USER_NAME';
		
		IF TRIM(con_name1) IS NOT NULL THEN
			dbms_output.Put_line('Unique key (UM_USER_NAME,UM_TENANT_ID) is already exists');
		END IF;
		
		EXCEPTION WHEN NO_DATA_FOUND THEN
			EXECUTE IMMEDIATE 'ALTER TABLE UM_USER ADD UNIQUE(UM_USER_NAME,UM_TENANT_ID)';
	END;
END;
/

ALTER TABLE UM_USER ADD UNIQUE(UM_USER_ID)
/

CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2)
  IS
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  dbms_output.put_line('Skipped');
END;
/

CALL add_index_if_not_exists('CREATE UNIQUE INDEX INDEX_UM_USERNAME_UM_TENANT_ID ON UM_USER(UM_USER_NAME, UM_TENANT_ID)')
/

DROP PROCEDURE add_index_if_not_exists
/

ALTER TABLE UM_TENANT ADD UM_ORG_UUID VARCHAR(36) DEFAULT NULL
/
