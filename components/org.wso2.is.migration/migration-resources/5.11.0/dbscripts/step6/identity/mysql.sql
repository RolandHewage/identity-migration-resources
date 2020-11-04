DROP PROCEDURE IF EXISTS skip_index_if_exists;

CREATE PROCEDURE skip_index_if_exists(indexName varchar(64), tableName varchar(64), tableColumns varchar(255)) BEGIN BEGIN DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END; SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, tableColumns); PREPARE stmt FROM @s; EXECUTE stmt; END;END;

CALL skip_index_if_exists('IDX_REMOTE_FETCH_REVISION_CONFIG_ID','IDN_REMOTE_FETCH_REVISIONS','(CONFIG_ID)');

DROP PROCEDURE IF EXISTS skip_index_if_exists;
