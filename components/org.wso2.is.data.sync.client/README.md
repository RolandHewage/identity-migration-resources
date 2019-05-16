# How to use the client

* Create new databases for the new Identity Server version you are migrating to.
* Unzip the new Identity Server distribution (use a WUM updated distribution if possible). This will be used as the 
data sync tool between the Identity Server versions. We will refer to this Identity Server distribution as “data sync tool” and location as <SYNC-TOOL-HOME>
* Build the project and copy the jar file in the target to the <SYNC-TOOL-HOME>/repository/components/dropins directory.
* Open <SYNC-TOOL-HOME>/repository/conf/log4j.properties file and add below content. This will create a seperate log file <SYNC-TOOL-HOME>/repository/logs/sync.log which will contain the sync tool related logs.
```
# Appender config to put sync Log.
log4j.logger.org.wso2.is.data.sync.system=INFO, SYNC_LOGFILE, SYNC_CONSOLE
log4j.additivity.org.wso2.is.data.sync.system=false
log4j.appender.SYNC_LOGFILE=org.wso2.carbon.utils.logging.appenders.CarbonDailyRollingFileAppender
log4j.appender.SYNC_LOGFILE.File=${carbon.home}/repository/logs/${instance.log}/sync.log
log4j.appender.SYNC_LOGFILE.layout=org.wso2.carbon.utils.logging.TenantAwarePatternLayout
log4j.appender.SYNC_LOGFILE.Threshold=INFO
log4j.appender.SYNC_LOGFILE.layout.ConversionPattern=[%d] %P%5p {%c} - %x %m%n

log4j.appender.SYNC_CONSOLE=org.wso2.carbon.utils.logging.appenders.CarbonConsoleAppender
log4j.appender.SYNC_CONSOLE.layout=org.wso2.carbon.utils.logging.TenantAwarePatternLayout
# ConversionPattern will be overridden by the configuration setting in the DB
log4j.appender.SYNC_CONSOLE.layout.ConversionPattern=[%d] %P%5p {%c} - %x %m%n
log4j.appender.SYNC_CONSOLE.threshold=INFO
```

* Add the data sources used in source and target Identity Server deployments involved in the migration to 
<SYNC-TOOL-HOME>/repository/conf/datasources/master-datasources.xml file.
* Create a property file with below properties as required and name it as sync.properties.

| Property | Description | Mandatory/Optional | Default value |
|:-------------|:-------------|:-----|:------------- |
| sourceVersion={version} | Source product version | Mandatory | - |
| targetVersion={version} | Target product version | Mandatory | - |
| batchSize={batch_size} | Size of a sync batch | Optional | 100 |
| syncInterval={sync_interval} | Interval in milliseconds between data sync batches | Optional | 5000 |
| syncTables={TBL_1, TBL_2} | Tables to be synced. Tables should be comma separated. | Mandatory | - |
| identitySchema={source_jndi,target_jndi} | JNDI names of source and target data sources for a identity schema. | Mandatory | - |

* Start the sync tool with below command.
  * If you want to create the required tables and triggers directly on the database.
```
sh wso2server.sh -DprepareSync -DconfigFile=<path to sync.properties file>/sync.properties
```
  * If you want to generate the DDL scripts for the required tables and triggers (after generating you need to manually 
execute them on the database).
```
sh wso2server.sh -DprepareSync -DgenerateDDL -DconfigFile=<path to sync.properties file>/sync.properties
```

* This will generate the required triggers and the metadata tables to sync the data between the databases used for the
 2 versions of the identity server. Below are the parameters used.
	
  At the moment below tables are supported to be synced.
  * IDN_IDENTITY_USER_DATA
  * IDN_OAUTH2_ACCESS_TOKEN
  * IDN_OAUTH2_ACCESS_TOKEN_SCOPE
  * IDN_OAUTH2_AUTHORIZATION_CODE

* Disable the endpoints in the WSO2 Identity Server that are not mission critical for the maintenance window. 
Endpoints that are currently allowed are listed in this section.
Create database dumps from the old databases (databases used in the old version of the Identity Server) and restore in the new databases created.
Do the data migration to the new Identity Server version as explained in here.
After successfully completing the migration, start the data sync tool with below command pointing to the same property file.

```
sh wso2server.sh -DsyncData -DconfigFile=<path to sync.properties file>/sync.properties
```

* This will start syncing data created in the old Identity Server database after taking the database dump to the new 
Identity Server database.
* Monitor the logs in the sync tool to see how many entries are synced at a given time and the data sync process is 
completed. Below line will be printed in the logs for each table you have specified to sync if there are no data to be synced.
[2019-02-27 17:26:32,388]  INFO {org.wso2.is.data.sync.system.pipeline.process.BatchProcessor} -  No data to sync for: <TABLE_NAME>
* If you have some traffic to the old version of the Identity Server, the number of entries to be synced might not 
become zero at any time. In that case, watch for the logs and decide on a point that the number of entries that are synced is a lower value.
* When the data sync is completed, switch the traffic from the old setup to the new setup.
* Allow the sync client to run for some time to sync the entries that were not synced before switching the deployments
. When the number of entries synced by the sync tool, becomes zero, stop the sync client.
