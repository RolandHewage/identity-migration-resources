INSERT INTO IDN_CONFIG_TYPE (ID, NAME, DESCRIPTION) VALUES
('f4e83b8a-d1c4-a0d6-03a7-d48e268c60c5', 'PK_JWT_CONFIGURATION', 'A resource type to keep the tenant private key jwt configuration.');

ALTER TABLE IDN_OIDC_JTI ADD TENANT_ID INTEGER NOT NULL DEFAULT -1;

DECLARE @COMMAND NVARCHAR(200);SELECT @COMMAND='ALTER TABLE IDN_OIDC_JTI DROP CONSTRAINT ' + NAME + ';' FROM   sys.key_constraints WHERE  [type] = 'PK' AND [parent_object_id] = Object_id('dbo.IDN_OIDC_JTI');EXEC (@COMMAND);

ALTER TABLE IDN_OIDC_JTI ADD CONSTRAINT IDN_OIDC_JTI_KEY PRIMARY KEY (JWT_ID,TENANT_ID);

