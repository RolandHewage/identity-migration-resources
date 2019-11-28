ALTER TABLE IDN_SAML2_ASSERTION_STORE ADD COLUMN ASSERTION BYTEA;

DO $$ BEGIN BEGIN ALTER TABLE IDN_OAUTH2_AUTHORIZATION_CODE ADD COLUMN IDP_ID INTEGER NOT NULL DEFAULT -1;	 ALTER TABLE IDN_OAUTH2_AUTHORIZATION_CODE ALTER COLUMN IDP_ID DROP DEFAULT; EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'column IDP_ID already exists in IDN_OAUTH2_AUTHORIZATION_CODE.'; END;	BEGIN ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN ADD COLUMN IDP_ID INTEGER NOT NULL DEFAULT -1;	 ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN ALTER COLUMN IDP_ID DROP DEFAULT; EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'column IDP_ID already exists in IDN_OAUTH2_ACCESS_TOKEN.'; END;	BEGIN ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN_AUDIT ADD COLUMN IDP_ID INTEGER NOT NULL DEFAULT -1;	 ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN_AUDIT ALTER COLUMN IDP_ID DROP DEFAULT; EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'column IDP_ID already exists in IDN_OAUTH2_ACCESS_TOKEN_AUDIT.'; END; END$$;

CREATE OR REPLACE FUNCTION add_idp_id_to_con_app_key_if_token_id_present() RETURNS void AS $$ begin if (SELECT count(*) FROM pg_indexes WHERE tablename = 'idn_oauth2_access_token' AND indexname = 'con_app_key' AND indexdef LIKE '%' || 'token_id' || '%') > 0 then ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN DROP CONSTRAINT IF EXISTS CON_APP_KEY; ALTER TABLE IDN_OAUTH2_ACCESS_TOKEN ADD CONSTRAINT CON_APP_KEY UNIQUE (CONSUMER_KEY_ID, AUTHZ_USER, TOKEN_ID, USER_DOMAIN, USER_TYPE, TOKEN_SCOPE_HASH, TOKEN_STATE, TOKEN_STATE_ID, IDP_ID); end if; END;$$ LANGUAGE plpgsql;

select add_idp_id_to_con_app_key_if_token_id_present();

CREATE TABLE IF NOT EXISTS IDN_AUTH_USER (
	USER_ID VARCHAR(255) NOT NULL,
	USER_NAME VARCHAR(255) NOT NULL,
	TENANT_ID INTEGER NOT NULL,
	DOMAIN_NAME VARCHAR(255) NOT NULL,
	IDP_ID INTEGER NOT NULL,
	PRIMARY KEY (USER_ID),
	CONSTRAINT USER_STORE_CONSTRAINT UNIQUE (USER_NAME, TENANT_ID, DOMAIN_NAME, IDP_ID));

CREATE OR REPLACE FUNCTION skip_index_if_exists(nameOfTheIndex varchar(64),tableName varchar(64), tableColumns varchar(64)) RETURNS void AS $$ declare s varchar(1000); declare result INTEGER; begin SELECT COUNT(1) into result FROM pg_indexes WHERE indexname = nameOfTheIndex; IF result = 0 THEN s :=  CONCAT('CREATE INDEX ' , nameOfTheIndex , ' ON ' , tableName, tableColumns); execute s; end if; END;$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS IDN_AUTH_USER_SESSION_MAPPING (
	USER_ID VARCHAR(255) NOT NULL,
	SESSION_ID VARCHAR(255) NOT NULL,
	CONSTRAINT USER_SESSION_STORE_CONSTRAINT UNIQUE (USER_ID, SESSION_ID));

SELECT skip_index_if_exists('idx_user_id', 'idn_auth_user_session_mapping', '(user_id)');

SELECT skip_index_if_exists('idx_session_id', 'idn_auth_user_session_mapping', '(session_id)');

SELECT skip_index_if_exists('idx_oca_um_tid_ud_apn','idn_oauth_consumer_apps','(username,tenant_id,user_domain, app_name)');

SELECT skip_index_if_exists('idx_spi_app','sp_inbound_auth','(app_id)');

SELECT skip_index_if_exists('idx_iop_tid_ck','idn_oidc_property','(tenant_id,consumer_key)');

SELECT skip_index_if_exists('idx_at_au_tid_ud_ts_ckid', 'idn_oauth2_access_token', '(authz_user, tenant_id, user_domain, token_state, consumer_key_id)');

SELECT skip_index_if_exists('idx_at_at', 'idn_oauth2_access_token', '(access_token)');

SELECT skip_index_if_exists('idx_at_au_ckid_ts_ut', 'idn_oauth2_access_token', '(authz_user, consumer_key_id, token_state, user_type)');

SELECT skip_index_if_exists('idx_at_rth', 'idn_oauth2_access_token', '(refresh_token_hash)');

SELECT skip_index_if_exists('idx_at_rt', 'idn_oauth2_access_token', '(refresh_token)');

SELECT skip_index_if_exists('idx_ac_ckid', 'idn_oauth2_authorization_code', '(consumer_key_id)');

SELECT skip_index_if_exists('idx_ac_tid', 'idn_oauth2_authorization_code', '(token_id)');

SELECT skip_index_if_exists('idx_ac_ac_ckid', 'idn_oauth2_authorization_code', '(authorization_code, consumer_key_id)');

SELECT skip_index_if_exists('idx_sc_tid', 'idn_oauth2_scope', '(tenant_id)');

SELECT skip_index_if_exists('idx_sc_n_tid', 'idn_oauth2_scope', '(name, tenant_id)');

SELECT skip_index_if_exists('idx_sb_scpid', 'idn_oauth2_scope_binding', '(scope_id)');

SELECT skip_index_if_exists('idx_oror_tid', 'idn_oidc_req_object_reference', '(token_id)');

SELECT skip_index_if_exists('idx_ats_tid', 'idn_oauth2_access_token_scope', '(token_id)');

SELECT skip_index_if_exists('idx_auth_user_un_tid_dn', 'idn_auth_user', '(user_name, tenant_id, domain_name)');

SELECT skip_index_if_exists('idx_auth_user_dn_tod', 'idn_auth_user', '(domain_name, tenant_id)');

DROP FUNCTION skip_index_if_exists(varchar,varchar,varchar);

DROP FUNCTION add_idp_id_to_con_app_key_if_token_id_present();
