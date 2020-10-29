CREATE PROCEDURE skip_index_if_exists(indexName varchar(64), tableName varchar(64), tableColumns varchar(255))
BEGIN
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN
        END;
        SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, tableColumns); PREPARE stmt FROM @s;
        EXECUTE stmt;
    END;
END;

CALL skip_index_if_exists('UM_ATTR_NAME_VALUE_INDEX', 'UM_USER_ATTRIBUTE', '(UM_ATTR_NAME, UM_ATTR_VALUE)');

DROP PROCEDURE IF EXISTS skip_index_if_exists;
