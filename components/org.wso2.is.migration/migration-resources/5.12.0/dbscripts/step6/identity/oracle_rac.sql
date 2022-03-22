BEGIN
    execute immediate 'ALTER TABLE IDN_OAUTH2_DEVICE_FLOW ADD QUANTIFIER INTEGER DEFAULT 0';
    execute immediate 'ALTER TABLE IDN_OAUTH2_DEVICE_FLOW ADD CONSTRAINT USRCDE_QNTFR_CONSTRAINT UNIQUE (USER_CODE, QUANTIFIER)';
    execute immediate 'ALTER TABLE IDN_OAUTH2_DEVICE_FLOW DROP CONSTRAINT USER_CODE';
    dbms_output.put_line('created');
exception WHEN OTHERS THEN
    dbms_output.put_line('skipped');
END;
/

UPDATE IDP_METADATA SET NAME = 'account.lock.handler.lock.on.max.failed.attempts.enable'
WHERE NAME = 'account.lock.handler.enable'
/

INSERT INTO IDN_CONFIG_TYPE (ID, NAME, DESCRIPTION)
SELECT '669b99ca-cdb0-44a6-8cae-babed3b585df', 'Publisher', 'A resource type to keep the event publisher configurations' FROM DUAL
WHERE NOT EXISTS (SELECT * FROM IDN_CONFIG_TYPE WHERE id = '669b99ca-cdb0-44a6-8cae-babed3b585df' OR name = 'Publisher')
/
