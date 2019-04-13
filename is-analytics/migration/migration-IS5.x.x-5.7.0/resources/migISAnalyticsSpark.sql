-- ===========================================================================
--   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
--
--   Licensed under the Apache License, Version 2.0 (the "License");
--   you may not use this file except in compliance with the License.
--   You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
--   Unless required by applicable law or agreed to in writing, software
--   distributed under the License is distributed on an "AS IS" BASIS,
--   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--   See the License for the specific language governing permissions and
--   limitations under the License.
-- ===========================================================================
	CREATE TEMPORARY TABLE isAuthenticationAnalyticsPerHourMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_AuthStatPerHour",
        mergeSchema "true");
        
        CREATE TEMPORARY TABLE AuthStatAgg_HOURS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "AuthStatAgg_HOURS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, localUsername STRING, rolesCommaSeparated STRING, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE AuthStatAgg_HOURS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2000','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, userStoreDomain, (cast(sum(authFirstSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, first(localUsername), CASE WHEN first(rolesCommaSeparated) = "NOT_AVAILABLE" THEN concat(concat(',',last(rolesCommaSeparated)),',') ELSE concat(concat(',',first(rolesCommaSeparated)),',') END AS rolesCommaSeparated, remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, sum(authFirstSuccessCount) as AGG_SUM_firstLoginValue
        FROM isAuthenticationAnalyticsPerHourMig
        GROUP BY meta_tenantId, year, month, day, hour, username, serviceProvider, identityProvider, region, remoteIp, userStoreDomain, identityProviderType;                             
          
        CREATE TEMPORARY TABLE isAuthenticationAnalyticsPerDayMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_AuthStatPerDay",
        schema "meta_tenantId INT -i, year INT -i, month INT -i, day INT -i, username STRING -i -f, localUsername STRING -i -f, rolesCommaSeparated STRING -i -f, 
        serviceProvider STRING -i -f, identityProvider STRING -i -f, region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, 
        authStepSuccessCount LONG -sp, authFirstSuccessCount LONG -sp, identityProviderType STRING -i -f, facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, month, day, username, localUsername, rolesCommaSeparated, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE AuthStatAgg_DAYS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "AuthStatAgg_DAYS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, localUsername STRING, rolesCommaSeparated STRING, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE AuthStatAgg_DAYS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2001','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, userStoreDomain, (cast(sum(authFirstSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, first(localUsername), CASE WHEN first(rolesCommaSeparated) = "NOT_AVAILABLE" THEN concat(concat(',',last(rolesCommaSeparated)),',') ELSE concat(concat(',',first(rolesCommaSeparated)),',') END AS rolesCommaSeparated, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, sum(authFirstSuccessCount) as AGG_SUM_firstLoginValue
        FROM isAuthenticationAnalyticsPerDayMig
        GROUP BY meta_tenantId, year, month, day, username, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType;   
        
        CREATE TEMPORARY TABLE isAuthenticationAnalyticsPerMonthMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_AuthStatPerMonth",
        schema "meta_tenantId INT -i, year INT -i, month INT -i, username STRING -i -f, localUsername STRING -i -f, rolesCommaSeparated STRING -i -f, 
        serviceProvider STRING -i -f, identityProvider STRING -i -f, region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, 
        authStepSuccessCount LONG -sp, authFirstSuccessCount LONG -sp, identityProviderType STRING -i -f, facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, month, username, localUsername, rolesCommaSeparated, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE AuthStatAgg_MONTHS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "AuthStatAgg_MONTHS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, localUsername STRING, rolesCommaSeparated STRING, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE AuthStatAgg_MONTHS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2002','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, userStoreDomain, (cast(sum(authFirstSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, first(localUsername), CASE WHEN first(rolesCommaSeparated) = "NOT_AVAILABLE" THEN concat(concat(',',last(rolesCommaSeparated)),',') ELSE concat(concat(',',first(rolesCommaSeparated)),',') END AS rolesCommaSeparated, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, sum(authFirstSuccessCount) as AGG_SUM_firstLoginValue
        FROM isAuthenticationAnalyticsPerMonthMig
        GROUP BY meta_tenantId, year, month, username, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType;                   
                            
        CREATE TEMPORARY TABLE isAuthenticationAnalyticsPerYearMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_AuthStatPerYear",
        schema "meta_tenantId INT -i, year INT -i, username STRING -i -f, localUsername STRING -i -f, rolesCommaSeparated STRING -i -f, 
        serviceProvider STRING -i -f, identityProvider STRING -i -f, region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, 
        authStepSuccessCount LONG -sp, authFirstSuccessCount LONG -sp, identityProviderType STRING -i -f, facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, username, localUsername, rolesCommaSeparated, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE AuthStatAgg_YEARS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "AuthStatAgg_YEARS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN,  identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, localUsername STRING, rolesCommaSeparated STRING, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE AuthStatAgg_YEARS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2003','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, userStoreDomain, (cast(sum(authFirstSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, first(localUsername), CASE WHEN first(rolesCommaSeparated) = "NOT_AVAILABLE" THEN concat(concat(',',last(rolesCommaSeparated)),',') ELSE concat(concat(',',first(rolesCommaSeparated)),',') END AS rolesCommaSeparated, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, sum(authFirstSuccessCount) as AGG_SUM_firstLoginValue
        FROM isAuthenticationAnalyticsPerYearMig
        GROUP BY meta_tenantId, year, username, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType;                    
                            
        CREATE TEMPORARY TABLE isRoleAuthenticationAnalyticsPerHourMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_RoleAuthStatPerHour",
        mergeSchema "true");
        
        CREATE TEMPORARY TABLE RoleAggregation_HOURS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "RoleAggregation_HOURS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), token STRING(100), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, token, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE RoleAggregation_HOURS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2000','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, role as token, userStoreDomain, ((cast(sum(authStepSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) or (cast(sum(authSuccessCount) as BOOLEAN) and cast(instr(CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END,',') as BOOLEAN))) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, 0 as AGG_SUM_firstLoginValue
        FROM isRoleAuthenticationAnalyticsPerHourMig
        GROUP BY meta_tenantId, year, month, day, hour, username, role, serviceProvider, identityProvider, remoteIp, region, userStoreDomain, identityProviderType;                    
                            
        CREATE TEMPORARY TABLE isRoleAuthenticationAnalyticsPerDayMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_RoleAuthStatPerDay",
        schema "meta_tenantId INT -i, year INT -i, month INT -i, day INT -i, username STRING -i -f, role STRING -i -f, serviceProvider STRING -i -f, identityProvider STRING -i -f, 
        region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, authStepSuccessCount LONG -sp, identityProviderType STRING -i -f, 
        facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, month, day, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE RoleAggregation_DAYS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "RoleAggregation_DAYS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), token STRING(100), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, token, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE RoleAggregation_DAYS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2001','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, role as token, userStoreDomain, ((cast(sum(authStepSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) or (cast(sum(authSuccessCount) as BOOLEAN) and cast(instr(CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END,',') as BOOLEAN))) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, 0 as AGG_SUM_firstLoginValue
        FROM isRoleAuthenticationAnalyticsPerDayMig
        GROUP BY meta_tenantId, year, month, day, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType;                    
                            
        CREATE TEMPORARY TABLE isRoleAuthenticationAnalyticsPerMonthMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_RoleAuthStatPerMonth",
        schema "meta_tenantId INT -i, year INT -i, month INT -i, username STRING -i -f, role STRING -i -f, serviceProvider STRING -i -f, identityProvider STRING -i -f, 
        region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, authStepSuccessCount LONG -sp, identityProviderType STRING -i -f, 
        facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, month, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE RoleAggregation_MONTHS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "RoleAggregation_MONTHS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), token STRING(100), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, token, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE RoleAggregation_MONTHS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2002','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, role as token, userStoreDomain, ((cast(sum(authStepSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) or (cast(sum(authSuccessCount) as BOOLEAN) and cast(instr(CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END,',') as BOOLEAN))) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, 0 as AGG_SUM_firstLoginValue
        FROM isRoleAuthenticationAnalyticsPerMonthMig
        GROUP BY meta_tenantId, year, month, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType;                    
                            
        CREATE TEMPORARY TABLE isRoleAuthenticationAnalyticsPerYearMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_RoleAuthStatPerYear",
        schema "meta_tenantId INT -i, year INT -i, username STRING -i -f, role STRING -i -f, serviceProvider STRING -i -f, identityProvider STRING -i -f, 
        region STRING -i -f, userStoreDomain STRING -i -f, authSuccessCount LONG -sp, authFailureCount LONG -sp, authStepSuccessCount LONG -sp, identityProviderType STRING -i -f, 
        facetStartTime STRING -i -f, _timestamp LONG",
        primaryKeys "meta_tenantId, year, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType", 
        mergeSchema "false");
        
        CREATE TEMPORARY TABLE RoleAggregation_YEARS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "RoleAggregation_YEARS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, username STRING, serviceProvider STRING(100), identityProvider STRING(100), region STRING(45), token STRING(100), userStoreDomain STRING(50), 
        isFirstLogin BOOLEAN, identityProviderType STRING, AGG_LAST_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, remoteIp STRING, timestamp LONG, 
        AGG_SUM_successValue LONG, AGG_SUM_failureValue LONG, AGG_SUM_stepSuccessValue LONG, AGG_SUM_firstLoginValue LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, region, token, userStoreDomain, isFirstLogin, identityProviderType");
        
        INSERT INTO TABLE RoleAggregation_YEARS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2003','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, username, serviceProvider, identityProvider, CASE WHEN region = "" THEN "NOT_AVAILABLE" ELSE region END AS region, role as token, userStoreDomain, ((cast(sum(authStepSuccessCount) as BOOLEAN) and cast(sum(authSuccessCount) as BOOLEAN)) or (cast(sum(authSuccessCount) as BOOLEAN) and cast(instr(CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END,',') as BOOLEAN))) as isFirstLogin, CASE WHEN first(identityProviderType) is null THEN "SSO" ELSE first(identityProviderType) END AS identityProviderType, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, meta_tenantId, '' as remoteIp, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,'01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as timestamp, sum(authSuccessCount) as AGG_SUM_successValue, sum(authFailureCount) as AGG_SUM_failureValue, sum(authStepSuccessCount) as AGG_SUM_stepSuccessValue, 0 as AGG_SUM_firstLoginValue
        FROM isRoleAuthenticationAnalyticsPerYearMig
        GROUP BY meta_tenantId, year, username, role, serviceProvider, identityProvider, region, userStoreDomain, identityProviderType; 
        
        CREATE TEMPORARY TABLE isSessionAnalyticsPerHourMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_SessionStatPerHour",
        mergeSchema "true");
        
        CREATE TEMPORARY TABLE SessionAggregation_HOURS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SessionAggregation_HOURS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, AGG_LAST_EVENT_TIMESTAMP LONG, activeSessionCount LONG, AGG_SUM_newSessionCount LONG, AGG_SUM_terminatedSessionCount LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, meta_tenantId");
        
        INSERT INTO TABLE SessionAggregation_HOURS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2000','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, meta_tenantId, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':',hour,'00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, sum(activeSessionCount) as activeSessionCount, sum(newSessionCount) as AGG_SUM_newSessionCount, sum(terminatedSessionCount) as AGG_SUM_terminatedSessionCount
        FROM isSessionAnalyticsPerHourMig
        GROUP BY meta_tenantId, year, month, day, hour;                    
                            
        CREATE TEMPORARY TABLE isSessionAnalyticsPerDayMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_SessionStatPerDay",
        mergeSchema "true");
        
        CREATE TEMPORARY TABLE SessionAggregation_DAYS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SessionAggregation_DAYS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, AGG_LAST_EVENT_TIMESTAMP LONG, activeSessionCount LONG, AGG_SUM_newSessionCount LONG, AGG_SUM_terminatedSessionCount LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, meta_tenantId");
        
        INSERT INTO TABLE SessionAggregation_DAYS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2001','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, meta_tenantId, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,day),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, sum(activeSessionCount) as activeSessionCount, sum(newSessionCount) as AGG_SUM_newSessionCount, sum(terminatedSessionCount) as AGG_SUM_terminatedSessionCount
        FROM isSessionAnalyticsPerDayMig
        GROUP BY meta_tenantId, year, month, day;                    
                            
        CREATE TEMPORARY TABLE isSessionAnalyticsPerMonthMig USING CarbonAnalytics
        OPTIONS (tableName "org_wso2_is_analytics_stream_SessionStatPerMonth",
        mergeSchema "true");
        
        CREATE TEMPORARY TABLE SessionAggregation_MONTHS USING CarbonJDBC
        OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SessionAggregation_MONTHS",
        schema "AGG_TIMESTAMP LONG, AGG_EVENT_TIMESTAMP LONG, meta_tenantId INTEGER, AGG_LAST_EVENT_TIMESTAMP LONG, activeSessionCount LONG, AGG_SUM_newSessionCount LONG, AGG_SUM_terminatedSessionCount LONG",
        primaryKeys "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, meta_tenantId");
        
        INSERT INTO TABLE SessionAggregation_MONTHS
        SELECT (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-','2002','01','01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_TIMESTAMP, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_EVENT_TIMESTAMP, meta_tenantId, (cast(to_unix_timestamp(concat_ws(' ',concat_ws('-',year,month,'01'),concat_ws(':','00','00','00'),'+0000'),'yyyy-MM-dd hh:mm:ss Z') as LONG) * 1000) as AGG_LAST_EVENT_TIMESTAMP, sum(activeSessionCount) as activeSessionCount, sum(newSessionCount) as AGG_SUM_newSessionCount, sum(terminatedSessionCount) as AGG_SUM_terminatedSessionCount
        FROM isSessionAnalyticsPerMonthMig
        GROUP BY meta_tenantId, year, month;                    
        
        CREATE TEMPORARY TABLE sessionInfoMig USING CarbonAnalytics OPTIONS (tableName
        "ORG_WSO2_IS_ANALYTICS_STREAM_SESSIONINFO", schema "meta_tenantId INT -i -f, sessionId STRING -i -f,
        startTimestamp LONG -i, renewTimestamp LONG -i, terminationTimestamp LONG -i, endTimestamp LONG -i, year INT,
        month INT, day INT, hour INT, minute INT, duration LONG -f -sp, isActive BOOLEAN -i -f, username STRING -i -f,
        userstoreDomain STRING -i -f, remoteIp STRING -i -f, region STRING -i, tenantDomain STRING -i -f,
        serviceProvider STRING -i -f, identityProviders STRING -i -f, rememberMeFlag BOOLEAN -i -f, userAgent STRING -i -f, 
        usernameWithTenantDomainAndUserstoreDomain STRING -i -f, _timestamp LONG -i", primaryKeys "meta_tenantId,
        sessionId", mergeSchema "false");
        
        CREATE TEMPORARY TABLE SessionInformationTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SessionInformationTable",
	schema "meta_tenantId INTEGER, sessionId STRING, startTime STRING, terminateTime STRING, endTime STRING, duration LONG, isActive BOOLEAN, username STRING, userstoreDomain STRING, remoteIp STRING, region STRING, tenantDomain STRING, serviceProvider STRING, identityProviders STRING, rememberMeFlag BOOLEAN, userAgent STRING, userStore STRING, currentTime STRING, startTimestamp LONG, renewTimestamp LONG, terminationTimestamp LONG, endTimestamp LONG, timestamp LONG",
	indices "username, userstoreDomain, tenantDomain",
	primaryKeys "meta_tenantId, sessionId");
	
	INSERT INTO TABLE SessionInformationTable
	SELECT meta_tenantId, sessionId, date_format(cast((startTimestamp/1000) as TIMESTAMP),'yyyy-MM-dd HH:mm:ss') as startTime, from_unixtime((terminationTimestamp/1000)) as terminateTime, from_unixtime((endTimestamp/1000)) as endTime, duration as duration, isActive as isActive, username as username, userstoreDomain as userstoreDomain, remoteIp as remoteIp, region as region, tenantDomain as tenantDomain, serviceProvider, identityProviders, rememberMeFlag, userAgent, usernameWithTenantDomainAndUserstoreDomain as userStore, from_unixtime((_timestamp/1000)) as currentTime, startTimestamp, renewTimestamp, terminationTimestamp, endTimestamp, _timestamp as timestamp
	FROM sessionInfoMig;                    
                            
        CREATE TEMPORARY TABLE processedOverallAuthenticationMig USING CarbonAnalytics OPTIONS (tableName
        "ORG_WSO2_IS_ANALYTICS_STREAM_PROCESSEDOVERALLAUTHENTICATION", schema "meta_tenantId INT -i, contextId STRING -i,
        eventId STRING, eventType STRING -i, authenticationSuccess BOOLEAN -i, username STRING -i, localUsername STRING -i,
        userStoreDomain STRING -i, tenantDomain STRING, remoteIp STRING -i, region STRING -i, inboundAuthType STRING, serviceProvider STRING -i,
        rememberMeEnabled BOOLEAN, forceAuthEnabled BOOLEAN, passiveAuthEnabled BOOLEAN,
        rolesCommaSeparated STRING -i, authenticationStep STRING, identityProvider STRING -i, authStepSuccess BOOLEAN -i,
        stepAuthenticator STRING, isFirstLogin BOOLEAN -i, identityProviderType STRING -i, _timestamp LONG -i", primaryKeys "meta_tenantId,
        contextId, eventId, eventType", mergeSchema "false");
        
        CREATE TEMPORARY TABLE OverallAuthTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "OverallAuthTable",
	schema "meta_tenantId INTEGER, contextId STRING, eventId STRING, eventType STRING, username STRING, localUsername STRING, userStoreDomain STRING, tenantDomain STRING, remoteIp STRING, region STRING, inboundAuthType STRING, serviceProvider STRING, rememberMeEnabled BOOLEAN, forceAuthEnabled BOOLEAN, passiveAuthEnabled BOOLEAN, rolesCommaSeparated STRING, authenticationStep STRING, identityProvider STRING, authenticationSuccess BOOLEAN, authStepSuccess BOOLEAN, stepAuthenticator STRING, isFirstLogin BOOLEAN, identityProviderType STRING, utcTime STRING, timestamp LONG",
	primaryKeys "meta_tenantId, eventId, eventType");
	
	INSERT INTO TABLE OverallAuthTable
	SELECT meta_tenantId, contextId, eventId, eventType, username, localUsername, userStoreDomain, tenantDomain, remoteIp, region, inboundAuthType, serviceProvider, rememberMeEnabled, forceAuthEnabled, passiveAuthEnabled, rolesCommaSeparated, authenticationStep, identityProvider, authenticationSuccess, authStepSuccess, stepAuthenticator, isFirstLogin, identityProviderType, from_unixtime((_timestamp/1000)) as utcTime, _timestamp as timestamp
	FROM processedOverallAuthenticationMig;  
	
	CREATE TEMPORARY TABLE loginSuccessAfterMultipleFailuresMig USING CarbonAnalytics OPTIONS (tableName
        "ORG_WSO2_IS_ANALYTICS_STREAM_LOGINSUCCESSAFTERMULTIPLEFAILURES", schema "meta_tenantId INT -i, username STRING -i,
        severity INT -i, msg STRING, tenantDomain STRING -i, _timestamp LONG -i", mergeSchema "false");
        
        CREATE TEMPORARY TABLE SuspiciousAlertTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SuspiciousAlertTable",
	schema "meta_tenantId INTEGER, username STRING, severity INTEGER, msg STRING, tenantDomain STRING, timestamp LONG, currentTime STRING",
	primaryKeys "meta_tenantId, username, msg");
	
	INSERT INTO TABLE SuspiciousAlertTable
	SELECT meta_tenantId, username, severity, msg, tenantDomain, _timestamp as timestamp, from_unixtime((_timestamp/1000)) as currentTime
	FROM loginSuccessAfterMultipleFailuresMig;                  
                            
        CREATE TEMPORARY TABLE allIsAlertsStreamMig USING CarbonAnalytics OPTIONS (tableName
        "ORG_WSO2_IS_ANALYTICS_ALLISALERTSSTREAM", schema "meta_tenantId INT -i, type STRING -i -f,
        tenantDomain STRING -i -f, msg STRING -i, severity INT -i, alertTimestamp LONG -i, userReadableTime STRING -i,
        _timestamp LONG -i", mergeSchema "false");
        
        CREATE TEMPORARY TABLE SecurityAlertTypeTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "SecurityAlertTypeTable",
	schema "meta_tenantId INTEGER, alertId STRING, type STRING, tenantDomain STRING, msg STRING, severity INTEGER, alertTimestamp LONG, userReadableTime STRING",
	primaryKeys "meta_tenantId, alertId");
	
	INSERT INTO TABLE SecurityAlertTypeTable
	SELECT meta_tenantId, md5(concat_ws(' ', meta_tenantId, type, tenantDomain, msg, severity, alertTimestamp, userReadableTime)) as alertId, type, tenantDomain, msg, severity, alertTimestamp, userReadableTime 
	FROM allIsAlertsStreamMig;                     
                            
        CREATE TEMPORARY TABLE longSessionsMig USING CarbonAnalytics OPTIONS (tableName
        "ORG_WSO2_IS_ANALYTICS_STREAM_LONGSESSIONS", schema "meta_tenantId INT -i, tenantDomain STRING,
        sessionId STRING -i, username STRING, duration LONG, avgDuration DOUBLE, _timestamp LONG -i", primaryKeys "meta_tenantId,
        sessionId", mergeSchema "false");
        
        CREATE TEMPORARY TABLE AlertLongSessionsTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "AlertLongSessionsTable",
	schema "meta_tenantId INTEGER, tenantDomain STRING, sessionId STRING, username STRING, duration LONG, avgDuration DOUBLE, timestamp LONG, currentTime STRING",
	primaryKeys "meta_tenantId, sessionId",
	indices "username");
	
	INSERT INTO TABLE AlertLongSessionsTable
	SELECT meta_tenantId, tenantDomain, sessionId, username, duration, avgDuration, _timestamp as timestamp, from_unixtime((_timestamp/1000)) as currentTime 
        FROM longSessionsMig
        GROUP BY meta_tenantId, tenantDomain, sessionId, username, duration, avgDuration, _timestamp;                     
                            
        CREATE TEMPORARY TABLE activeSessionTableMig USING CarbonAnalytics OPTIONS (tableName 
	"ORG_WSO2_IS_ANALYTICS_STREAM_ACTIVESESSIONS", schema "meta_tenantId INT -i -f, sessionId STRING -i -f, 
	startTimestamp LONG -i, renewTimestamp LONG -i, terminationTimestamp LONG -i, year INT, month INT, day INT, hour INT, minute INT, 
	action INT -i -f, username STRING -i -f, userstoreDomain STRING -i -f, remoteIp STRING -i -f, region STRING -i -f, tenantDomain STRING -i -f, 
	serviceProvider STRING -i -f, identityProviders STRING -i -f, rememberMeFlag BOOLEAN, userAgent STRING -i -f, 
	usernameWithTenantDomainAndUserstoreDomain STRING -i -f, _timestamp LONG -i", primaryKeys "meta_tenantId, sessionId", mergeSchema "false"); 
	
	CREATE TEMPORARY TABLE ActiveSessionsTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "ActiveSessionsTable",
	schema "meta_tenantId INTEGER, sessionId STRING, startTimestamp LONG, renewTimestamp LONG, terminationTimestamp LONG, action INTEGER, username STRING, userstoreDomain STRING, remoteIp STRING, region STRING, tenantDomain STRING, serviceProvider STRING, identityProviders STRING, rememberMeFlag BOOLEAN, userAgent STRING, userStore STRING, timestamp LONG",
	primaryKeys "meta_tenantId, sessionId"); 
	
	INSERT INTO TABLE ActiveSessionsTable
	SELECT meta_tenantId, sessionId, startTimestamp, renewTimestamp, terminationTimestamp, action, username, userstoreDomain, remoteIp, region, tenantDomain, serviceProvider, identityProviders, rememberMeFlag, userAgent, usernameWithTenantDomainAndUserstoreDomain as userStore, _timestamp as timestamp 
	FROM activeSessionTableMig;                    
                            
        CREATE TEMPORARY TABLE ActiveSessionCountTable USING CarbonJDBC
	OPTIONS (dataSource "IS_ANALYTICS_DB", tableName "ActiveSessionCountTable",
	schema "meta_tenantId INTEGER, activeCount LONG",
	primaryKeys "meta_tenantId");                    
                            
        INSERT INTO TABLE ActiveSessionCountTable
	SELECT meta_tenantId, count(sessionId) as activeCount 
	FROM activeSessionTableMig
	GROUP BY meta_tenantId;                    
