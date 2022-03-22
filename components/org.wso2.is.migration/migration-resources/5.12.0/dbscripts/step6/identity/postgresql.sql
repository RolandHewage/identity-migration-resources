DO $$ BEGIN BEGIN ALTER TABLE IDN_OAUTH2_DEVICE_FLOW ADD COLUMN QUANTIFIER INTEGER DEFAULT 0, DROP CONSTRAINT idn_oauth2_device_flow_user_code_key, ADD CONSTRAINT USRCDE_QNTFR_CONSTRAINT UNIQUE (USER_CODE, QUANTIFIER); EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'column QUANTIFIER already exists in IDN_OAUTH2_DEVICE_FLOW table.'; END; END $$;

BEGIN TRANSACTION;
UPDATE IDP_METADATA SET NAME = 'account.lock.handler.lock.on.max.failed.attempts.enable'
WHERE NAME = 'account.lock.handler.enable';
COMMIT;

INSERT INTO IDN_CONFIG_TYPE (ID, NAME, DESCRIPTION)
VALUES ('669b99ca-cdb0-44a6-8cae-babed3b585df', 'Publisher', 'A resource type to keep the event publisher configurations')
ON CONFLICT DO NOTHING;
