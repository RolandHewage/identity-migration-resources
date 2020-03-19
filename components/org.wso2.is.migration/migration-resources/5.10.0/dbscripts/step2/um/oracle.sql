ALTER TABLE UM_USER
    ADD (
        UM_USER_ID CHAR(36) DEFAULT LOWER(regexp_replace(rawtohex(sys_guid()), '([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})', '\1-\2-\3-\4-\5')))
/

ALTER TABLE UM_USER ADD UNIQUE (UM_USER_ID)
/
