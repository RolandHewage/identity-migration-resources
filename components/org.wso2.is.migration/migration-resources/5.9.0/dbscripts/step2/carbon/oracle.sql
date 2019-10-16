ALTER TABLE UM_DOMAIN
ALTER COLUMN UM_DOMAIN_ID SET NOT NULL,
ALTER COLUMN UM_DOMAIN_NAME SET NOT NULL
/

ALTER TABLE UM_HYBRID_ROLE
ALTER COLUMN UM_ROLE_NAME SET NOT NULL
/

ALTER TABLE UM_SYSTEM_ROLE
ALTER COLUMN UM_ID SET NOT NULL,
ALTER COLUMN UM_ROLE_NAME SET NOT NULL
/

CREATE OR REPLACE PROCEDURE add_constraint_if_not_exists (query IN VARCHAR2, t_constraint IN VARCHAR2)
  IS
DECLARE
  v_code NUMBER;
  v_errm VARCHAR2(64);
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  v_code := SQLCODE;
  v_errm := SUBSTR(SQLERRM, 1 , 64);
  dbms_output.put_line( 'Error occurred. Error code is ' || v_code || '- ' || v_errm || '. Skipped '|| t_constraint ||' constraint creation.' );
END;
/

CALL add_index_if_not_exists('ALTER TABLE UM_DOMAIN ADD CONSTRAINT UC_UM_DOMAIN UNIQUE(UM_DOMAIN_NAME,UM_TENANT_ID)', 'UC_UM_DOMAIN')
/
CALL add_index_if_not_exists('ALTER TABLE UM_HYBRID_ROLE ADD CONSTRAINT UC_UM_HYBRID_ROLE UNIQUE(UM_ROLE_NAME,UM_TENANT_ID)', 'UC_UM_HYBRID_ROLE')
/
CALL add_index_if_not_exists('ALTER TABLE UM_SYSTEM_ROLE ADD CONSTRAINT UC_UM_SYSTEM_ROLE UNIQUE(UM_ROLE_NAME,UM_TENANT_ID)', 'UC_UM_SYSTEM_ROLE')
/
CALL add_index_if_not_exists('ALTER TABLE UM_SYSTEM_ROLE ADD CONSTRAINT UNIQUE_REG_PATH_TENANT_ID UNIQUE(REG_PATH_VALUE,REG_TENANT_ID)', 'UNIQUE_REG_PATH_TENANT_ID')
/

DROP PROCEDURE add_constraint_if_not_exists
/

CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2, t_index IN VARCHAR2)
  IS
DECLARE
  v_code NUMBER;
  v_errm VARCHAR2(64);
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  v_code := SQLCODE;
  v_errm := SUBSTR(SQLERRM, 1 , 64);
  dbms_output.put_line( 'Error occurred. Error code is ' || v_code || '- ' || v_errm || '. Skipped '|| t_index ||' index creation.' );
END;
/

CALL add_index_if_not_exists('CREATE INDEX UM_ROLE_NAME_IND ON UM_HYBRID_ROLE(UM_ROLE_NAME)', 'UM_ROLE_NAME_IND')
/

DROP PROCEDURE add_index_if_not_exists
/