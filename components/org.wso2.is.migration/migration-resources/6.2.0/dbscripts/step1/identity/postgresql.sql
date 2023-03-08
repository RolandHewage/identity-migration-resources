INSERT INTO IDN_CONFIG_TYPE (ID, NAME, DESCRIPTION) VALUES
('f4e83b8a-d1c4-a0d6-03a7-d48e268c60c5', 'PK_JWT_CONFIGURATION', 'A resource type to keep the tenant private key jwt configuration.');

ALTER TABLE IDN_OIDC_JTI ADD COLUMN TENANT_ID INTEGER NOT NULL DEFAULT -1;

DO $$ DECLARE con_name varchar(200); BEGIN SELECT 'ALTER TABLE IDN_OIDC_JTI DROP CONSTRAINT ' || tc .constraint_name || ';' INTO con_name FROM information_schema.table_constraints AS tc JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name WHERE constraint_type = 'PRIMARY KEY' AND tc.table_name = 'idn_oidc_jti'; EXECUTE con_name; END $$;

ALTER TABLE IDN_OIDC_JTI ADD PRIMARY KEY (JWT_ID, TENANT_ID);
