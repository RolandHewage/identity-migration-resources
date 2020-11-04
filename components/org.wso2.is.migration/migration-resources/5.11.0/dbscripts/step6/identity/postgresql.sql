CREATE OR REPLACE FUNCTION skip_index_if_exists(nameOfTheIndex varchar(64),tableName varchar(64), tableColumns varchar(64)) RETURNS void AS $$ declare s varchar(1000); declare result INTEGER; begin SELECT COUNT(1) into result FROM pg_indexes WHERE indexname = nameOfTheIndex; IF result = 0 THEN s :=  CONCAT('CREATE INDEX ' , nameOfTheIndex , ' ON ' , tableName, tableColumns); execute s; end if; END;$$ LANGUAGE plpgsql;

SELECT skip_index_if_exists('IDX_REMOTE_FETCH_REVISION_CONFIG_ID','IDN_REMOTE_FETCH_REVISIONS','(CONFIG_ID)');

DROP FUNCTION skip_index_if_exists(varchar,varchar,varchar);
