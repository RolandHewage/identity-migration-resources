DO
$$
DECLARE con_name VARCHAR(200);
BEGIN
    SELECT tc.constraint_name
    INTO con_name FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
    JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
    WHERE constraint_type ='UNIQUE' AND tc.table_name = 'um_user' AND kcu.column_name = 'um_user_id' GROUP BY tc.constraint_name ;

    IF con_name IS NOT NULL THEN
    	EXECUTE 'ALTER TABLE um_user DROP CONSTRAINT IF EXISTS '|| con_name || ';';
    END IF;

	ALTER TABLE um_user ADD CONSTRAINT um_user_um_user_id_key UNIQUE(um_user_id);

	IF NOT EXISTS (
		SELECT * FROM information_schema.table_constraints AS tc
		JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
		JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
		WHERE constraint_type = 'UNIQUE' AND tc.table_name = 'um_user' AND kcu.column_name = 'um_user_name') THEN
            ALTER TABLE um_user ADD CONSTRAINT um_user_um_user_name_um_tenant_id_key UNIQUE(um_user_name,um_tenant_id);
	END IF;
END $$;

CREATE UNIQUE INDEX INDEX_UM_USERNAME_UM_TENANT_ID ON UM_USER(UM_USER_NAME, UM_TENANT_ID);

ALTER TABLE UM_TENANT ADD COLUMN UM_ORG_UUID VARCHAR(36) DEFAULT NULL;
