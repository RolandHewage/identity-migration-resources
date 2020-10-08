UPDATE sp_app 
SET 
	app_name = 'My Account', 
	is_saas_app = 1,
	description = 'This is the my account application.'
WHERE ID = (SELECT APP_ID FROM SP_INBOUND_AUTH WHERE INBOUND_AUTH_KEY = 'USER_PORTAL' AND TENANT_ID = -1234);

DO $$ DECLARE con_name varchar(200); BEGIN select constraint_name into con_name from information_schema.table_constraints WHERE constraint_schema = current_schema() and table_name = 'idn_oidc_property' and constraint_type = 'FOREIGN KEY' limit 1; execute 'alter table idn_oidc_property drop constraint '||con_name; END $$;

UPDATE idn_oauth_consumer_apps 
SET 
	consumer_key= 'MY_ACCOUNT', 
	callback_url = REPLACE(callback_url, 'user-portal/login', 'myaccount/login'),
	app_name = 'My Account' 
WHERE 
	consumer_key = 'USER_PORTAL' AND tenant_id = -1234;

UPDATE IDN_OIDC_PROPERTY 
SET 
	CONSUMER_KEY= 'MY_ACCOUNT'
WHERE 
	CONSUMER_KEY = 'USER_PORTAL' AND TENANT_ID = -1234;

ALTER TABLE idn_oidc_property
ADD FOREIGN KEY (CONSUMER_KEY) REFERENCES IDN_OAUTH_CONSUMER_APPS(CONSUMER_KEY) ON DELETE CASCADE;

DELETE FROM IDN_OIDC_PROPERTY WHERE TENANT_ID = -1234 AND CONSUMER_KEY = 'MY_ACCOUNT' AND PROPERTY_KEY = 'tokenRevocationWithIDPSessionTermination';
DELETE FROM IDN_OIDC_PROPERTY WHERE TENANT_ID = -1234 AND CONSUMER_KEY = 'MY_ACCOUNT' AND PROPERTY_KEY = 'tokenBindingValidation';
DELETE FROM IDN_OIDC_PROPERTY WHERE TENANT_ID = -1234 AND CONSUMER_KEY = 'MY_ACCOUNT' AND PROPERTY_KEY = 'tokenBindingType';

INSERT INTO IDN_OIDC_PROPERTY (TENANT_ID, CONSUMER_KEY, PROPERTY_KEY, PROPERTY_VALUE) 
SELECT -1234, 'MY_ACCOUNT', 'tokenRevocationWithIDPSessionTermination', 'true'
WHERE EXISTS (SELECT * FROM IDN_OAUTH_CONSUMER_APPS WHERE IDN_OAUTH_CONSUMER_APPS.CONSUMER_KEY = 'MY_ACCOUNT');

INSERT INTO IDN_OIDC_PROPERTY (TENANT_ID, CONSUMER_KEY, PROPERTY_KEY, PROPERTY_VALUE) 
SELECT -1234, 'MY_ACCOUNT', 'tokenBindingValidation', 'true'
WHERE EXISTS (SELECT * FROM IDN_OAUTH_CONSUMER_APPS WHERE IDN_OAUTH_CONSUMER_APPS.CONSUMER_KEY = 'MY_ACCOUNT');

INSERT INTO IDN_OIDC_PROPERTY (TENANT_ID, CONSUMER_KEY, PROPERTY_KEY, PROPERTY_VALUE) 
SELECT -1234, 'MY_ACCOUNT', 'tokenBindingType', 'sso-session'
WHERE EXISTS (SELECT * FROM IDN_OAUTH_CONSUMER_APPS WHERE IDN_OAUTH_CONSUMER_APPS.CONSUMER_KEY = 'MY_ACCOUNT');

UPDATE sp_inbound_auth
SET
	inbound_auth_key = 'MY_ACCOUNT'
WHERE 
	inbound_auth_key = 'USER_PORTAL' AND tenant_id=-1234;

DELETE FROM sp_app WHERE app_name = 'User Portal'AND tenant_id <> -1234;
DELETE FROM sp_inbound_auth WHERE inbound_auth_key = 'User Portal' AND tenant_id = -1234;
DELETE FROM idn_oauth_consumer_apps WHERE consumer_key LIKE 'USER_PORTAL%.com' AND tenant_id <> -1234;
