UPDATE um_hybrid_role 
SET 
	um_role_name = 'Application/My Account' 
WHERE 
	um_role_name = 'Application/User Portal' AND um_tenant_id = -1234
/

DELETE FROM um_hybrid_role WHERE um_role_name = 'Application/User Portal' AND um_tenant_id <> -1234 /
