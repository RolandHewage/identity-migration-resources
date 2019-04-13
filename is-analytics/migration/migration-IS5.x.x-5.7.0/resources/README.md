Migration of IS Analytics from DAS to SP [IS 5.7.0 from 5.x.x]
==============================================================

Introduction
============
Analytics profile of WSO2 IS 5.6.0 & below [IS 5.x.x] are based on WSO2 DAS.
Analytics profile of WSO2 IS 5.7.0 is based on WSO2 Stream Processor.
When upgrading from WSO2 IS 5.x.x to 5.7.0, you are moving from an instance of WSO2 DAS to WSO2 Stream Processor. IS Analytics table migration is an indirect process where we need to convert the Analytics tables into RDBMS tables by running spark scripts using the CarbonJDBC provider packed with DAS. The "migISAnalytics.sh/migISAnalytics.bat" scripts create the IS Analytics tables & the "migISAnalyticsSpark" spark script migrate the data.

How to Build?
=============
1. Go to the $project home folder & execute following command from the project home folder to generate the final Jar file. 

$mvn clean install

2. A new single jar named “migIS.one-jar.jar” is created in the $project/target folder together with its dependency jars.

How to Migrate ?
================
1. Copy the built jar file "migIS.one-jar.jar" and "migISAnalytics.sh/migISAnalytics.bat" file into a same directory location.

2. Execute the "migISAnalytics.sh/migISAnalytics.bat" file. 

$./migISAnalytics.sh

3. Enter the correct database details in the command prompt.

4. The tables related to IS Analytics are created in the relevant database specified by the user.

5. Next  add the datasource configuration to the master-datasources.xml file in {IS_ANALYTICS_HOME}/repository/conf/datasources  folder of the old DAS product

	Eg:-
```
	<datasource>
            <name>IS_ANALYTICS_DB</name>
            <description>The datasource used for is-analytics persisted data</description>
            <jndiConfig>
                <name>jdbc/IS_ANALYTICS_DB</name>
            </jndiConfig>
            <definition type="RDBMS">
                <configuration>
                    <url>jdbc:mysql://localhost:3306/IS_ANALYTICS_DB?autoReconnect=true&amp;relaxAutoCommit=true&amp;useSSL=false</url>
                    <username>root</username>
                    <password>root</password>
                    <driverClassName>com.mysql.jdbc.Driver</driverClassName>
                    <maxActive>50</maxActive>
                    <maxWait>60000</maxWait>
                    <testOnBorrow>true</testOnBorrow>
                    <validationQuery>SELECT 1</validationQuery>
                    <validationInterval>30000</validationInterval>
                    <defaultAutoCommit>false</defaultAutoCommit>
                </configuration>
            </definition>
    	</datasource>
```

6. Add the JDBC driver to the {IS_ANALYTICS_HOME}/repository/components/lib folder

7. Next run the Analytic profile of the old version of WSO2 IS which is based on 
WSO2 DAS using the following command. 

$./wso2server.sh

8. Log in to the management console and go to [Main Home -> Manage -> Batch Analytics -> Scripts] and add the New Analytics Script ("migISAnalyticsSpark" spark script). 

9. Then execute in the background (Important to execute in background. If not it would throw a thread death error) the "migISAnalyticsSpark" spark script to migrate the data related to the analytics profile. The migrated data would be stored in the RDBMS database specified by the user.

10. Run the Analytic profile of the new version of WSO2 IS which is based on WSO2 SP to generate the rest of the IS Analytics tables using the following command. 

$./worker.sh

11. Run the IS Product & send an event to populate data to aggregation tables.

12. Run the Analytic dashboard of the new version of WSO2 IS to view the migrated statistics using the following command. 

$./dashboard.sh 

