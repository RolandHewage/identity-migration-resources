ALTER TABLE UM_DOMAIN
    ALTER COLUMN UM_DOMAIN_NAME SET NOT NULL;

ALTER TABLE UM_HYBRID_ROLE
    ALTER COLUMN UM_ROLE_NAME SET NOT NULL;

ALTER TABLE UM_SYSTEM_ROLE
    ALTER COLUMN UM_ROLE_NAME SET NOT NULL;

DROP PROCEDURE IF EXISTS skip_constraint_if_exists;

CREATE PROCEDURE skip_constraint_if_exists(constraintName varchar(64), tableName varchar(64), tableColumns varchar(255))
BEGIN
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN
        END;
        SET @s = CONCAT('ALTER TABLE ', tableName, ' ADD CONSTRAINT ', constraintName, ' UNIQUE' tableColumns);
        PREPARE stmt FROM @s; EXECUTE stmt;
    END;
END;

CALL skip_constraint_if_exists('UM_DOMAIN', 'UC_UM_DOMAIN', '(UM_DOMAIN_NAME,UM_TENANT_ID)');
CALL skip_constraint_if_exists('UM_HYBRID_ROLE', 'UC_UM_HYBRID_ROLE', '(UM_ROLE_NAME,UM_TENANT_ID)');
CALL skip_constraint_if_exists('UM_SYSTEM_ROLE', 'UC_UM_SYSTEM_ROLE', '(UM_ROLE_NAME,UM_TENANT_ID)');
CALL skip_constraint_if_exists('UM_SYSTEM_ROLE', 'UNIQUE_REG_PATH_TENANT_ID', '(REG_PATH_VALUE,REG_TENANT_ID)');

DROP PROCEDURE IF EXISTS skip_constraint_if_exists;

DROP PROCEDURE IF EXISTS skip_index_if_exists;

CREATE PROCEDURE skip_index_if_exists(indexName varchar(64), tableName varchar(64), tableColumns varchar(255))
BEGIN
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN
        END;
        SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, tableColumns); PREPARE stmt FROM @s;
        EXECUTE stmt;
    END;
END;

CALL skip_index_if_exists('UM_ROLE_NAME_IND', 'UM_HYBRID_ROLE', '(UM_ROLE_NAME)');

DROP PROCEDURE IF EXISTS skip_index_if_exists;