DROP PROCEDURE IF EXISTS skip_index_if_exists;

CREATE PROCEDURE skip_index_if_exists(indexName varchar(64), tableName varchar(64), tableColumns varchar(255)) BEGIN BEGIN DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END; SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, tableColumns); PREPARE stmt FROM @s; EXECUTE stmt; END; END;

CALL skip_index_if_exists('IDX_TK_VALUE_TYPE','IDN_OAUTH2_TOKEN_BINDING','(TOKEN_BINDING_VALUE, TOKEN_BINDING_TYPE)');

DROP PROCEDURE IF EXISTS skip_index_if_exists;
