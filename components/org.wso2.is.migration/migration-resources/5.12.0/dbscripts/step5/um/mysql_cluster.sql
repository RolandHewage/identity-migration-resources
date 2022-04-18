CREATE PROCEDURE skip_index_if_exists(indexName varchar(64), tableName varchar(64), tableColumns varchar(255))
BEGIN
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN
        END;
        SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, tableColumns); PREPARE stmt FROM @s;
        EXECUTE stmt;
    END;
END;

CREATE INDEX INDEX_UM_USERNAME_UM_TENANT_ID ON UM_USER(UM_USER_NAME, UM_TENANT_ID);

DROP PROCEDURE IF EXISTS skip_index_if_exists;
