ALTER TABLE UM_DOMAIN
ALTER COLUMN UM_DOMAIN_NAME SET NOT NULL;

ALTER TABLE UM_HYBRID_ROLE
ALTER COLUMN UM_ROLE_NAME SET NOT NULL;

ALTER TABLE UM_SYSTEM_ROLE
ALTER COLUMN UM_ROLE_NAME SET NOT NULL;

create or replace function create_constraint_if_not_exists (
    t_name text, c_name text, constraint_sql text
)
returns void AS
$$
begin
    if not exists (select constraint_name
                   from information_schema.constraint_column_usage
                   where table_name = t_name  and constraint_name = c_name) then
        execute constraint_sql;
    end if;
end;
$$ language 'plpgsql'

SELECT create_constraint_if_not_exists('UM_DOMAIN','UC_UM_DOMAIN','ALTER TABLE UM_DOMAIN ADD CONSTRAINT UC_UM_DOMAIN UNIQUE(UM_DOMAIN_NAME,UM_TENANT_ID);')
SELECT create_constraint_if_not_exists('UM_HYBRID_ROLE','UC_UM_HYBRID_ROLE','ALTER TABLE UM_HYBRID_ROLE ADD CONSTRAINT UC_UM_HYBRID_ROLE UNIQUE(UM_ROLE_NAME,UM_TENANT_ID);')
SELECT create_constraint_if_not_exists('UM_SYSTEM_ROLE','UC_UM_SYSTEM_ROLE','ALTER TABLE UM_SYSTEM_ROLE ADD CONSTRAINT UC_UM_SYSTEM_ROLE UNIQUE(UM_ROLE_NAME,UM_TENANT_ID);')
SELECT create_constraint_if_not_exists('UM_SYSTEM_ROLE','UNIQUE_REG_PATH_TENANT_ID','ALTER TABLE UM_SYSTEM_ROLE ADD CONSTRAINT UNIQUE_REG_PATH_TENANT_ID UNIQUE(REG_PATH_VALUE,REG_TENANT_ID);')

DROP FUNCTION create_constraint_if_not_exists;

CREATE OR REPLACE FUNCTION skip_index_if_exists(indexName varchar(64),tableName varchar(64), tableColumns varchar(64))  RETURNS void AS $$ declare s varchar(1000);  begin if to_regclass(indexName) IS NULL then s :=  CONCAT('CREATE INDEX ' , indexName , ' ON ' , tableName, tableColumns);execute s;end if;END;$$ LANGUAGE plpgsql;

SELECT skip_index_if_exists('UM_ROLE_NAME_IND','UM_HYBRID_ROLE','(UM_ROLE_NAME)');

DROP FUNCTION skip_index_if_exists;