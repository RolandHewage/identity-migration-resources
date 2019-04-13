/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Initiate Database Connection to create IS_ANALYTICS tables.
 */
public class DatabaseConnection {

    /**
     * Variable which stores the database type.
     */
    private String dbType;
    /**
     * Variable which stores the database host.
     */
    private String host;
    /**
     * Variable which stores the database port.
     */
    private String port;
    /**
     * Variable which stores the database name.
     */
    private String dbName;
    /**
     * Variable which stores the database username.
     */
    private String user;
    /**
     * Variable which stores the database password.
     */
    private String pass;
    /**
     * Variable which stores the database driver location.
     */
    private String dbDriver;
    /**
     * Variable which stores the jdbcDriver.
     */
    private String jdbcDriver;
    /**
     * Variable which stores the database URL.
     */
    private String dbUrl;

    /**
     * Initialize Logger object to log messages
     */
    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class);

    /**
     * Represent the set of databases
     */
    private enum DBTYPE {
        MYSQL, POSTGRESQL, ORACLE, MSSQL;
    }

    /**
     * Object of SQL connection.
     */
    Connection connection = null;
    /**
     * Object of SQL statement.
     */
    Statement statement = null;

    /**
     * Constructor which initiate the variables dbType,host,port,user,pass,dbDriver.
     *
     * @param dbType   type of database.
     * @param host     host of database.
     * @param port     port of database.
     * @param dbName   name of database.
     * @param user     username of database.
     * @param pass     password of database.
     * @param dbDriver driver location of database.
     */
    public DatabaseConnection(String dbType, String host, String port, String dbName, String user, String pass,
                              String dbDriver) {

        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.user = user;
        this.pass = pass;
        this.dbDriver = dbDriver;
    }

    /**
     * Getter of jdbcDriver
     *
     * @return the jdbcDriver
     */
    public String getJdbcDriver() {

        return jdbcDriver;
    }

    /**
     * Setter of jdbcDriver
     *
     * @param jdbcDriver the jdbcDriver to set
     */
    public void setJdbcDriver(String jdbcDriver) {

        this.jdbcDriver = jdbcDriver;
    }

    /**
     * Getter of dbUrl
     *
     * @return the dbUrl
     */
    public String getDbUrl() {

        return dbUrl;
    }

    /**
     * Setter of dbUrl
     *
     * @param dbUrl the dbUrl to set
     */
    public void setDbUrl(String dbUrl) {

        this.dbUrl = dbUrl;
    }

    /**
     * Create IS_ANALYTICS tables in MySQL database.
     */
    public void createMySQLTables(){

        String createActiveSessionCountTable = "CREATE TABLE ActiveSessionCountTable ( meta_tenantId int(11) NOT NULL," +
                "activeCount bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId));";

        String createActiveSessionsTable = "CREATE TABLE ActiveSessionsTable ( meta_tenantId int(11) NOT NULL," +
                "sessionId varchar(254) NOT NULL, startTimestamp bigint(20) DEFAULT NULL," +
                "renewTimestamp bigint(20) DEFAULT NULL, terminationTimestamp bigint(20) DEFAULT NULL," +
                "action int(11) DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag tinyint(1) DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, timestamp bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createAuthStatAgg_HOURS = "CREATE TABLE AuthStatAgg_HOURS ( AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_DAYS = "CREATE TABLE AuthStatAgg_DAYS ( AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL, identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_MONTHS = "CREATE TABLE AuthStatAgg_MONTHS ( AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_YEARS = "CREATE TABLE AuthStatAgg_YEARS ( AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_HOURS = "CREATE TABLE RoleAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_DAYS = "CREATE TABLE RoleAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_MONTHS = "CREATE TABLE RoleAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_YEARS = "CREATE TABLE RoleAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint(20) NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin tinyint(1) NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "meta_tenantId int(11) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "AGG_SUM_successValue bigint(20) DEFAULT NULL, AGG_SUM_failureValue bigint(20) DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint(20) DEFAULT NULL, AGG_SUM_firstLoginValue bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createSessionAggregation_HOURS = "CREATE TABLE SessionAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL, AGG_EVENT_TIMESTAMP bigint(20) NOT NULL," +
                "meta_tenantId int(11) NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "activeSessionCount bigint(20) DEFAULT NULL, AGG_SUM_newSessionCount bigint(20) DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_DAYS = "CREATE TABLE SessionAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL, AGG_EVENT_TIMESTAMP bigint(20) NOT NULL," +
                "meta_tenantId int(11) NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "activeSessionCount bigint(20) DEFAULT NULL, AGG_SUM_newSessionCount bigint(20) DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_MONTHS = "CREATE TABLE SessionAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL, AGG_EVENT_TIMESTAMP bigint(20) NOT NULL," +
                "meta_tenantId int(11) NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "activeSessionCount bigint(20) DEFAULT NULL, AGG_SUM_newSessionCount bigint(20) DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_YEARS = "CREATE TABLE SessionAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint(20) NOT NULL, AGG_EVENT_TIMESTAMP bigint(20) NOT NULL," +
                "meta_tenantId int(11) NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint(20) DEFAULT NULL," +
                "activeSessionCount bigint(20) DEFAULT NULL, AGG_SUM_newSessionCount bigint(20) DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createAlertLongSessionsTable = "CREATE TABLE AlertLongSessionsTable ( " +
                "timestamp bigint(20) DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "meta_tenantId int(11) NOT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "sessionId varchar(254) NOT NULL, username varchar(254) DEFAULT NULL," +
                "duration bigint(20) DEFAULT NULL, avgDuration double DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId)," +
                "KEY AlertLongSessionsTable_INDEX (username));";

        String createOverallAuthTable = "CREATE TABLE OverallAuthTable ( " +
                "meta_tenantId int(11) NOT NULL, contextId varchar(254) DEFAULT NULL," +
                "eventId varchar(254) NOT NULL,  eventType varchar(254) NOT NULL," +
                "username varchar(254) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "userStoreDomain varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "remoteIp varchar(254) DEFAULT NULL, region varchar(254) DEFAULT NULL," +
                "inboundAuthType varchar(254) DEFAULT NULL, serviceProvider varchar(254) DEFAULT NULL," +
                "rememberMeEnabled tinyint(1) DEFAULT NULL, forceAuthEnabled tinyint(1) DEFAULT NULL," +
                "passiveAuthEnabled tinyint(1) DEFAULT NULL, rolesCommaSeparated varchar(254) DEFAULT NULL," +
                "authenticationStep varchar(254) DEFAULT NULL, identityProvider varchar(254) DEFAULT NULL," +
                "authenticationSuccess tinyint(1) DEFAULT NULL, authStepSuccess tinyint(1) DEFAULT NULL," +
                "stepAuthenticator varchar(254) DEFAULT NULL, isFirstLogin tinyint(1) DEFAULT NULL," +
                "identityProviderType varchar(254) DEFAULT NULL, utcTime varchar(254) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,eventId,eventType));";

        String createSecurityAlertTypeTable = "CREATE TABLE SecurityAlertTypeTable ( " +
                "meta_tenantId int(11) NOT NULL, alertId varchar(254) NOT NULL," +
                "type varchar(254) DEFAULT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "msg varchar(254) DEFAULT NULL, severity int(11) DEFAULT NULL," +
                "alertTimestamp bigint(20) DEFAULT NULL, userReadableTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,alertId));";

        String createSessionInformationTable = "CREATE TABLE SessionInformationTable ( " +
                "meta_tenantId int(11) NOT NULL, sessionId varchar(254) NOT NULL," +
                "startTime varchar(254) DEFAULT NULL, terminateTime varchar(254) DEFAULT NULL," +
                "endTime varchar(254) DEFAULT NULL, duration bigint(20) DEFAULT NULL," +
                "isActive tinyint(1) DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag tinyint(1) DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "startTimestamp bigint(20) DEFAULT NULL, renewTimestamp bigint(20) DEFAULT NULL," +
                "terminationTimestamp bigint(20) DEFAULT NULL, endTimestamp bigint(20) DEFAULT NULL," +
                "timestamp bigint(20) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId)," +
                "KEY SessionInformationTable_INDEX (username,userstoreDomain,tenantDomain));";

        String createSuspiciousAlertTable = "CREATE TABLE SuspiciousAlertTable ( " +
                "meta_tenantId int(11) NOT NULL, username varchar(254) NOT NULL," +
                "severity int(11) DEFAULT NULL, msg varchar(254) NOT NULL," +
                "tenantDomain varchar(254) DEFAULT NULL, timestamp bigint(20) DEFAULT NULL," +
                "currentTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,username,msg)," +
                "KEY SuspiciousAlertTable_INDEX (username));";

        List<String> al = new ArrayList<>();
        al.add(createActiveSessionCountTable);
        al.add(createActiveSessionsTable);
        al.add(createAuthStatAgg_HOURS);
        al.add(createAuthStatAgg_DAYS);
        al.add(createAuthStatAgg_MONTHS);
        al.add(createAuthStatAgg_YEARS);
        al.add(createRoleAggregation_HOURS);
        al.add(createRoleAggregation_DAYS);
        al.add(createRoleAggregation_MONTHS);
        al.add(createRoleAggregation_YEARS);
        al.add(createSessionAggregation_HOURS);
        al.add(createSessionAggregation_DAYS);
        al.add(createSessionAggregation_MONTHS);
        al.add(createSessionAggregation_YEARS);
        al.add(createAlertLongSessionsTable);
        al.add(createOverallAuthTable);
        al.add(createSecurityAlertTypeTable);
        al.add(createSessionInformationTable);
        al.add(createSuspiciousAlertTable);
        try {
            for (String s : al) {
                statement.executeUpdate(s);
            }
            LOG.info("IS_ANALYTICS tables created in MySQL");
        } catch (SQLException e) {
            LOG.error(e);
            LOG.info("Drop all existing tables in the database & re-run the script");
        }

    }

    /**
     * Create IS_ANALYTICS tables in Postgresql database.
     */
    public void createPostgresqlTables(){

        String createActiveSessionCountTable = "CREATE TABLE ActiveSessionCountTable ( meta_tenantId int NOT NULL," +
                "activeCount bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId));";

        String createActiveSessionsTable = "CREATE TABLE ActiveSessionsTable ( meta_tenantId int NOT NULL," +
                "sessionId varchar(254) NOT NULL, startTimestamp bigint DEFAULT NULL," +
                "renewTimestamp bigint DEFAULT NULL, terminationTimestamp bigint DEFAULT NULL," +
                "action int DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag boolean DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createAuthStatAgg_HOURS = "CREATE TABLE AuthStatAgg_HOURS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_DAYS = "CREATE TABLE AuthStatAgg_DAYS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_MONTHS = "CREATE TABLE AuthStatAgg_MONTHS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_YEARS = "CREATE TABLE AuthStatAgg_YEARS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_HOURS = "CREATE TABLE RoleAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_DAYS = "CREATE TABLE RoleAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_MONTHS = "CREATE TABLE RoleAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_YEARS = "CREATE TABLE RoleAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin boolean NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createSessionAggregation_HOURS = "CREATE TABLE SessionAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_DAYS = "CREATE TABLE SessionAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_MONTHS = "CREATE TABLE SessionAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_YEARS = "CREATE TABLE SessionAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createAlertLongSessionsTable = "CREATE TABLE AlertLongSessionsTable ( " +
                "timestamp bigint DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "meta_tenantId int NOT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "sessionId varchar(254) NOT NULL, username varchar(254) DEFAULT NULL," +
                "duration bigint DEFAULT NULL, avgDuration double precision DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createAlertLongSessionsTableINDEX = "CREATE INDEX AlertLongSessionsTable_INDEX " +
                "ON AlertLongSessionsTable (username);";

        String createOverallAuthTable = "CREATE TABLE OverallAuthTable ( " +
                "meta_tenantId int NOT NULL, contextId varchar(254) DEFAULT NULL," +
                "eventId varchar(254) NOT NULL,  eventType varchar(254) NOT NULL," +
                "username varchar(254) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "userStoreDomain varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "remoteIp varchar(254) DEFAULT NULL, region varchar(254) DEFAULT NULL," +
                "inboundAuthType varchar(254) DEFAULT NULL, serviceProvider varchar(254) DEFAULT NULL," +
                "rememberMeEnabled boolean DEFAULT NULL, forceAuthEnabled boolean DEFAULT NULL," +
                "passiveAuthEnabled boolean DEFAULT NULL, rolesCommaSeparated varchar(254) DEFAULT NULL," +
                "authenticationStep varchar(254) DEFAULT NULL, identityProvider varchar(254) DEFAULT NULL," +
                "authenticationSuccess boolean DEFAULT NULL, authStepSuccess boolean DEFAULT NULL," +
                "stepAuthenticator varchar(254) DEFAULT NULL, isFirstLogin boolean DEFAULT NULL," +
                "identityProviderType varchar(254) DEFAULT NULL, utcTime varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,eventId,eventType));";

        String createSecurityAlertTypeTable = "CREATE TABLE SecurityAlertTypeTable ( " +
                "meta_tenantId int NOT NULL, alertId varchar(254) NOT NULL," +
                "type varchar(254) DEFAULT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "msg varchar(254) DEFAULT NULL, severity int DEFAULT NULL," +
                "alertTimestamp bigint DEFAULT NULL, userReadableTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,alertId));";

        String createSessionInformationTable = "CREATE TABLE SessionInformationTable ( " +
                "meta_tenantId int NOT NULL, sessionId varchar(254) NOT NULL," +
                "startTime varchar(254) DEFAULT NULL, terminateTime varchar(254) DEFAULT NULL," +
                "endTime varchar(254) DEFAULT NULL, duration bigint DEFAULT NULL," +
                "isActive boolean DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag boolean DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "startTimestamp bigint DEFAULT NULL, renewTimestamp bigint DEFAULT NULL," +
                "terminationTimestamp bigint DEFAULT NULL, endTimestamp bigint DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createSessionInformationTableINDEX = "CREATE INDEX SessionInformationTable_INDEX " +
                "ON SessionInformationTable (username,userstoreDomain,tenantDomain);";

        String createSuspiciousAlertTable = "CREATE TABLE SuspiciousAlertTable ( " +
                "meta_tenantId int NOT NULL, username varchar(254) NOT NULL," +
                "severity int DEFAULT NULL, msg varchar(254) NOT NULL," +
                "tenantDomain varchar(254) DEFAULT NULL, timestamp bigint DEFAULT NULL," +
                "currentTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,username,msg));";

        String createSuspiciousAlertTableINDEX = "CREATE INDEX SuspiciousAlertTable_INDEX " +
                "ON SuspiciousAlertTable (username);";

        List<String> al = new ArrayList<>();
        al.add(createActiveSessionCountTable);
        al.add(createActiveSessionsTable);
        al.add(createAuthStatAgg_HOURS);
        al.add(createAuthStatAgg_DAYS);
        al.add(createAuthStatAgg_MONTHS);
        al.add(createAuthStatAgg_YEARS);
        al.add(createRoleAggregation_HOURS);
        al.add(createRoleAggregation_DAYS);
        al.add(createRoleAggregation_MONTHS);
        al.add(createRoleAggregation_YEARS);
        al.add(createSessionAggregation_HOURS);
        al.add(createSessionAggregation_DAYS);
        al.add(createSessionAggregation_MONTHS);
        al.add(createSessionAggregation_YEARS);
        al.add(createAlertLongSessionsTable);
        al.add(createAlertLongSessionsTableINDEX);
        al.add(createOverallAuthTable);
        al.add(createSecurityAlertTypeTable);
        al.add(createSessionInformationTable);
        al.add(createSessionInformationTableINDEX);
        al.add(createSuspiciousAlertTable);
        al.add(createSuspiciousAlertTableINDEX);
        try {
            for (String s : al) {
                statement.executeUpdate(s);
            }
            LOG.info("IS_ANALYTICS tables created in Postgresql");
        } catch (SQLException e) {
            LOG.error(e);
            LOG.info("Drop all existing tables in the database & re-run the script");
        }

    }

    /**
     * Create IS_ANALYTICS tables in Oracle database.
     */
    public void createOracleTables(){

        String createActiveSessionCountTable = "CREATE TABLE ACTIVESESSIONCOUNTTABLE ( META_TENANTID NUMBER(10)," +
                "ACTIVECOUNT NUMBER(19)," +
                "PRIMARY KEY (META_TENANTID))";

        String createActiveSessionsTable = "CREATE TABLE ACTIVESESSIONSTABLE ( META_TENANTID NUMBER(10)," +
                "SESSIONID VARCHAR2(254), STARTTIMESTAMP NUMBER(19)," +
                "RENEWTIMESTAMP NUMBER(19), TERMINATIONTIMESTAMP NUMBER(19)," +
                "ACTION NUMBER(10), USERNAME VARCHAR2(254)," +
                "USERSTOREDOMAIN VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "REGION VARCHAR2(254), TENANTDOMAIN VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(254), IDENTITYPROVIDERS VARCHAR2(254)," +
                "REMEMBERMEFLAG NUMBER(1), USERAGENT VARCHAR2(254)," +
                "USERSTORE VARCHAR2(254), TIMESTAMP NUMBER(19)," +
                "PRIMARY KEY (META_TENANTID, SESSIONID))";

        String createAuthStatAgg_HOURS = "CREATE TABLE AUTHSTATAGG_HOURS ( AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), LOCALUSERNAME VARCHAR2(254)," +
                "ROLESCOMMASEPARATED VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createAuthStatAgg_DAYS = "CREATE TABLE AUTHSTATAGG_DAYS ( AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), LOCALUSERNAME VARCHAR2(254)," +
                "ROLESCOMMASEPARATED VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createAuthStatAgg_MONTHS = "CREATE TABLE AUTHSTATAGG_MONTHS ( AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), LOCALUSERNAME VARCHAR2(254)," +
                "ROLESCOMMASEPARATED VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createAuthStatAgg_YEARS = "CREATE TABLE AUTHSTATAGG_YEARS ( AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), LOCALUSERNAME VARCHAR2(254)," +
                "ROLESCOMMASEPARATED VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createRoleAggregation_HOURS = "CREATE TABLE ROLEAGGREGATION_HOURS ( " +
                "AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), TOKEN VARCHAR2(100), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,TOKEN,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createRoleAggregation_DAYS = "CREATE TABLE ROLEAGGREGATION_DAYS ( " +
                "AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), TOKEN VARCHAR2(100), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,TOKEN,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createRoleAggregation_MONTHS = "CREATE TABLE ROLEAGGREGATION_MONTHS ( " +
                "AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), TOKEN VARCHAR2(100), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254)," +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,TOKEN,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createRoleAggregation_YEARS = "CREATE TABLE ROLEAGGREGATION_YEARS ( " +
                "AGG_TIMESTAMP NUMBER(19)," +
                "AGG_EVENT_TIMESTAMP NUMBER(19), USERNAME VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(100),  IDENTITYPROVIDER VARCHAR2(100)," +
                "REGION VARCHAR2(45), TOKEN VARCHAR2(100), USERSTOREDOMAIN VARCHAR2(50)," +
                "ISFIRSTLOGIN NUMBER(1), IDENTITYPROVIDERTYPE VARCHAR2(254), " +
                "AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10), REMOTEIP VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "AGG_SUM_SUCCESSVALUE NUMBER(19), AGG_SUM_FAILUREVALUE NUMBER(19)," +
                "AGG_SUM_STEPSUCCESSVALUE NUMBER(19), AGG_SUM_FIRSTLOGINVALUE NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,USERNAME,SERVICEPROVIDER," +
                "IDENTITYPROVIDER,REGION,TOKEN,USERSTOREDOMAIN,ISFIRSTLOGIN,IDENTITYPROVIDERTYPE))";

        String createSessionAggregation_HOURS = "CREATE TABLE SESSIONAGGREGATION_HOURS ( " +
                "AGG_TIMESTAMP NUMBER(19), AGG_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10),  AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "ACTIVESESSIONCOUNT NUMBER(19), AGG_SUM_NEWSESSIONCOUNT NUMBER(19)," +
                "AGG_SUM_TERMINATEDSESSIONCOUNT NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,META_TENANTID))";

        String createSessionAggregation_DAYS = "CREATE TABLE SESSIONAGGREGATION_DAYS ( " +
                "AGG_TIMESTAMP NUMBER(19), AGG_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10),  AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "ACTIVESESSIONCOUNT NUMBER(19), AGG_SUM_NEWSESSIONCOUNT NUMBER(19)," +
                "AGG_SUM_TERMINATEDSESSIONCOUNT NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,META_TENANTID))";

        String createSessionAggregation_MONTHS = "CREATE TABLE SESSIONAGGREGATION_MONTHS ( " +
                "AGG_TIMESTAMP NUMBER(19), AGG_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10),  AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "ACTIVESESSIONCOUNT NUMBER(19), AGG_SUM_NEWSESSIONCOUNT NUMBER(19)," +
                "AGG_SUM_TERMINATEDSESSIONCOUNT NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,META_TENANTID))";

        String createSessionAggregation_YEARS = "CREATE TABLE SESSIONAGGREGATION_YEARS ( " +
                "AGG_TIMESTAMP NUMBER(19), AGG_EVENT_TIMESTAMP NUMBER(19)," +
                "META_TENANTID NUMBER(10),  AGG_LAST_EVENT_TIMESTAMP NUMBER(19)," +
                "ACTIVESESSIONCOUNT NUMBER(19), AGG_SUM_NEWSESSIONCOUNT NUMBER(19)," +
                "AGG_SUM_TERMINATEDSESSIONCOUNT NUMBER(19)," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,META_TENANTID))";

        String createAlertLongSessionsTable = "CREATE TABLE ALERTLONGSESSIONSTABLE ( " +
                "TIMESTAMP NUMBER(19), CURRENTTIME VARCHAR2(254)," +
                "META_TENANTID NUMBER(10),  TENANTDOMAIN VARCHAR2(254)," +
                "SESSIONID VARCHAR2(254), USERNAME VARCHAR2(254)," +
                "DURATION NUMBER(19), AVGDURATION NUMBER(19,4)," +
                "PRIMARY KEY (META_TENANTID,SESSIONID))";

        String createAlertLongSessionsTableINDEX = "CREATE INDEX ALERTLONGSESSIONSTABLE_INDEX " +
                "ON ALERTLONGSESSIONSTABLE (USERNAME)";

        String createOverallAuthTable = "CREATE TABLE OVERALLAUTHTABLE ( " +
                "META_TENANTID NUMBER(10), CONTEXTID VARCHAR2(254)," +
                "EVENTID VARCHAR2(254),  EVENTTYPE VARCHAR2(254)," +
                "USERNAME VARCHAR2(254), LOCALUSERNAME VARCHAR2(254)," +
                "USERSTOREDOMAIN VARCHAR2(254), TENANTDOMAIN VARCHAR2(254)," +
                "REMOTEIP VARCHAR2(254), REGION VARCHAR2(254)," +
                "INBOUNDAUTHTYPE VARCHAR2(254), SERVICEPROVIDER VARCHAR2(254)," +
                "REMEMBERMEENABLED NUMBER(1), FORCEAUTHENABLED NUMBER(1)," +
                "PASSIVEAUTHENABLED NUMBER(1), ROLESCOMMASEPARATED VARCHAR2(254)," +
                "AUTHENTICATIONSTEP VARCHAR2(254), IDENTITYPROVIDER VARCHAR2(254)," +
                "AUTHENTICATIONSUCCESS NUMBER(1), AUTHSTEPSUCCESS NUMBER(1)," +
                "STEPAUTHENTICATOR VARCHAR2(254), ISFIRSTLOGIN NUMBER(1)," +
                "IDENTITYPROVIDERTYPE VARCHAR2(254), UTCTIME VARCHAR2(254)," +
                "TIMESTAMP NUMBER(19)," +
                "PRIMARY KEY (META_TENANTID,EVENTID,EVENTTYPE))";

        String createSecurityAlertTypeTable = "CREATE TABLE SECURITYALERTTYPETABLE ( " +
                "META_TENANTID NUMBER(10), ALERTID VARCHAR2(254)," +
                "TYPE VARCHAR2(254),  TENANTDOMAIN VARCHAR2(254)," +
                "MSG VARCHAR2(254), SEVERITY NUMBER(10)," +
                "ALERTTIMESTAMP NUMBER(19), USERREADABLETIME VARCHAR2(254)," +
                "PRIMARY KEY (META_TENANTID,ALERTID))";

        String createSessionInformationTable = "CREATE TABLE SESSIONINFORMATIONTABLE ( " +
                "META_TENANTID NUMBER(10), SESSIONID VARCHAR2(254)," +
                "STARTTIME VARCHAR2(254), TERMINATETIME VARCHAR2(254)," +
                "ENDTIME VARCHAR2(254), DURATION NUMBER(19)," +
                "ISACTIVE NUMBER(1), USERNAME VARCHAR2(254)," +
                "USERSTOREDOMAIN VARCHAR2(254), REMOTEIP VARCHAR2(254)," +
                "REGION VARCHAR2(254), TENANTDOMAIN VARCHAR2(254)," +
                "SERVICEPROVIDER VARCHAR2(254), IDENTITYPROVIDERS VARCHAR2(254)," +
                "REMEMBERMEFLAG NUMBER(1), USERAGENT VARCHAR2(254)," +
                "USERSTORE VARCHAR2(254), CURRENTTIME VARCHAR2(254)," +
                "STARTTIMESTAMP NUMBER(19), RENEWTIMESTAMP NUMBER(19)," +
                "TERMINATIONTIMESTAMP NUMBER(19), ENDTIMESTAMP NUMBER(19)," +
                "TIMESTAMP NUMBER(19)," +
                "PRIMARY KEY (META_TENANTID,SESSIONID))";

        String createSessionInformationTableINDEX = "CREATE INDEX SESSIONINFORMATIONTABLE_INDEX " +
                "ON SESSIONINFORMATIONTABLE (USERNAME,USERSTOREDOMAIN,TENANTDOMAIN)";

        String createSuspiciousAlertTable = "CREATE TABLE SUSPICIOUSALERTTABLE ( " +
                "META_TENANTID NUMBER(10), USERNAME VARCHAR2(254)," +
                "SEVERITY NUMBER(10), MSG VARCHAR2(254)," +
                "TENANTDOMAIN VARCHAR2(254), TIMESTAMP NUMBER(19)," +
                "CURRENTTIME VARCHAR2(254)," +
                "PRIMARY KEY (META_TENANTID,USERNAME,MSG))";

        String createSuspiciousAlertTableINDEX = "CREATE INDEX SUSPICIOUSALERTTABLE_INDEX " +
                "ON SUSPICIOUSALERTTABLE (USERNAME)";

        List<String> al = new ArrayList<>();
        al.add(createActiveSessionCountTable);
        al.add(createActiveSessionsTable);
        al.add(createAuthStatAgg_HOURS);
        al.add(createAuthStatAgg_DAYS);
        al.add(createAuthStatAgg_MONTHS);
        al.add(createAuthStatAgg_YEARS);
        al.add(createRoleAggregation_HOURS);
        al.add(createRoleAggregation_DAYS);
        al.add(createRoleAggregation_MONTHS);
        al.add(createRoleAggregation_YEARS);
        al.add(createSessionAggregation_HOURS);
        al.add(createSessionAggregation_DAYS);
        al.add(createSessionAggregation_MONTHS);
        al.add(createSessionAggregation_YEARS);
        al.add(createAlertLongSessionsTable);
        al.add(createAlertLongSessionsTableINDEX);
        al.add(createOverallAuthTable);
        al.add(createSecurityAlertTypeTable);
        al.add(createSessionInformationTable);
        al.add(createSessionInformationTableINDEX);
        al.add(createSuspiciousAlertTable);
        al.add(createSuspiciousAlertTableINDEX);
        try {
            for (String s : al) {
                statement.executeUpdate(s);
            }
            LOG.info("IS_ANALYTICS tables created in Oracle");
        } catch (SQLException e) {
            LOG.error(e);
            LOG.info("Drop all existing tables in the database & re-run the script");
        }

    }

    /**
     * Create IS_ANALYTICS tables in Mssql database.
     */
    public void createMssqlTables(){

        String createActiveSessionCountTable = "CREATE TABLE ActiveSessionCountTable ( meta_tenantId int NOT NULL," +
                "activeCount bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId));";

        String createActiveSessionsTable = "CREATE TABLE ActiveSessionsTable ( meta_tenantId int NOT NULL," +
                "sessionId varchar(254) NOT NULL, startTimestamp bigint DEFAULT NULL," +
                "renewTimestamp bigint DEFAULT NULL, terminationTimestamp bigint DEFAULT NULL," +
                "action int DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag BIT DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createAuthStatAgg_HOURS = "CREATE TABLE AuthStatAgg_HOURS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL, identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_DAYS = "CREATE TABLE AuthStatAgg_DAYS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_MONTHS = "CREATE TABLE AuthStatAgg_MONTHS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createAuthStatAgg_YEARS = "CREATE TABLE AuthStatAgg_YEARS ( AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "rolesCommaSeparated varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_HOURS = "CREATE TABLE RoleAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_DAYS = "CREATE TABLE RoleAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_MONTHS = "CREATE TABLE RoleAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL, identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createRoleAggregation_YEARS = "CREATE TABLE RoleAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint NOT NULL," +
                "AGG_EVENT_TIMESTAMP bigint NOT NULL, username varchar(254) NOT NULL," +
                "serviceProvider varchar(100) NOT NULL,  identityProvider varchar(100) NOT NULL," +
                "region varchar(45) NOT NULL, token varchar(100) NOT NULL, userStoreDomain varchar(50) NOT NULL," +
                "isFirstLogin BIT NOT NULL,  identityProviderType varchar(254) NOT NULL," +
                "AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "meta_tenantId int DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "AGG_SUM_successValue bigint DEFAULT NULL, AGG_SUM_failureValue bigint DEFAULT NULL," +
                "AGG_SUM_stepSuccessValue bigint DEFAULT NULL, AGG_SUM_firstLoginValue bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,username,serviceProvider," +
                "identityProvider,region,token,userStoreDomain,isFirstLogin,identityProviderType));";

        String createSessionAggregation_HOURS = "CREATE TABLE SessionAggregation_HOURS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_DAYS = "CREATE TABLE SessionAggregation_DAYS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_MONTHS = "CREATE TABLE SessionAggregation_MONTHS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createSessionAggregation_YEARS = "CREATE TABLE SessionAggregation_YEARS ( " +
                "AGG_TIMESTAMP bigint NOT NULL, AGG_EVENT_TIMESTAMP bigint NOT NULL," +
                "meta_tenantId int NOT NULL,  AGG_LAST_EVENT_TIMESTAMP bigint DEFAULT NULL," +
                "activeSessionCount bigint DEFAULT NULL, AGG_SUM_newSessionCount bigint DEFAULT NULL," +
                "AGG_SUM_terminatedSessionCount bigint DEFAULT NULL," +
                "PRIMARY KEY (AGG_TIMESTAMP,AGG_EVENT_TIMESTAMP,meta_tenantId));";

        String createAlertLongSessionsTable = "CREATE TABLE AlertLongSessionsTable ( " +
                "timestamp bigint DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "meta_tenantId int NOT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "sessionId varchar(254) NOT NULL, username varchar(254) DEFAULT NULL," +
                "duration bigint DEFAULT NULL, avgDuration float DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createAlertLongSessionsTableINDEX = "CREATE INDEX AlertLongSessionsTable_INDEX " +
                "ON AlertLongSessionsTable (username);";

        String createOverallAuthTable = "CREATE TABLE OverallAuthTable ( " +
                "meta_tenantId int NOT NULL, contextId varchar(254) DEFAULT NULL," +
                "eventId varchar(254) NOT NULL,  eventType varchar(254) NOT NULL," +
                "username varchar(254) DEFAULT NULL, localUsername varchar(254) DEFAULT NULL," +
                "userStoreDomain varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "remoteIp varchar(254) DEFAULT NULL, region varchar(254) DEFAULT NULL," +
                "inboundAuthType varchar(254) DEFAULT NULL, serviceProvider varchar(254) DEFAULT NULL," +
                "rememberMeEnabled BIT DEFAULT NULL, forceAuthEnabled BIT DEFAULT NULL," +
                "passiveAuthEnabled BIT DEFAULT NULL, rolesCommaSeparated varchar(254) DEFAULT NULL," +
                "authenticationStep varchar(254) DEFAULT NULL, identityProvider varchar(254) DEFAULT NULL," +
                "authenticationSuccess BIT DEFAULT NULL, authStepSuccess BIT DEFAULT NULL," +
                "stepAuthenticator varchar(254) DEFAULT NULL, isFirstLogin BIT DEFAULT NULL," +
                "identityProviderType varchar(254) DEFAULT NULL, utcTime varchar(254) DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,eventId,eventType));";

        String createSecurityAlertTypeTable = "CREATE TABLE SecurityAlertTypeTable ( " +
                "meta_tenantId int NOT NULL, alertId varchar(254) NOT NULL," +
                "type varchar(254) DEFAULT NULL,  tenantDomain varchar(254) DEFAULT NULL," +
                "msg varchar(254) DEFAULT NULL, severity int DEFAULT NULL," +
                "alertTimestamp bigint DEFAULT NULL, userReadableTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,alertId));";

        String createSessionInformationTable = "CREATE TABLE SessionInformationTable ( " +
                "meta_tenantId int NOT NULL, sessionId varchar(254) NOT NULL," +
                "startTime varchar(254) DEFAULT NULL, terminateTime varchar(254) DEFAULT NULL," +
                "endTime varchar(254) DEFAULT NULL, duration bigint DEFAULT NULL," +
                "isActive BIT DEFAULT NULL, username varchar(254) DEFAULT NULL," +
                "userstoreDomain varchar(254) DEFAULT NULL, remoteIp varchar(254) DEFAULT NULL," +
                "region varchar(254) DEFAULT NULL, tenantDomain varchar(254) DEFAULT NULL," +
                "serviceProvider varchar(254) DEFAULT NULL, identityProviders varchar(254) DEFAULT NULL," +
                "rememberMeFlag BIT DEFAULT NULL, userAgent varchar(254) DEFAULT NULL," +
                "userStore varchar(254) DEFAULT NULL, currentTime varchar(254) DEFAULT NULL," +
                "startTimestamp bigint DEFAULT NULL, renewTimestamp bigint DEFAULT NULL," +
                "terminationTimestamp bigint DEFAULT NULL, endTimestamp bigint DEFAULT NULL," +
                "timestamp bigint DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,sessionId));";

        String createSessionInformationTableINDEX = "CREATE INDEX SessionInformationTable_INDEX " +
                "ON SessionInformationTable (username,userstoreDomain,tenantDomain);";

        String createSuspiciousAlertTable = "CREATE TABLE SuspiciousAlertTable ( " +
                "meta_tenantId int NOT NULL, username varchar(254) NOT NULL," +
                "severity int DEFAULT NULL, msg varchar(254) NOT NULL," +
                "tenantDomain varchar(254) DEFAULT NULL, timestamp bigint DEFAULT NULL," +
                "currentTime varchar(254) DEFAULT NULL," +
                "PRIMARY KEY (meta_tenantId,username,msg));";

        String createSuspiciousAlertTableINDEX = "CREATE INDEX SuspiciousAlertTable_INDEX " +
                "ON SuspiciousAlertTable (username);";

        List<String> al = new ArrayList<>();
        al.add(createActiveSessionCountTable);
        al.add(createActiveSessionsTable);
        al.add(createAuthStatAgg_HOURS);
        al.add(createAuthStatAgg_DAYS);
        al.add(createAuthStatAgg_MONTHS);
        al.add(createAuthStatAgg_YEARS);
        al.add(createRoleAggregation_HOURS);
        al.add(createRoleAggregation_DAYS);
        al.add(createRoleAggregation_MONTHS);
        al.add(createRoleAggregation_YEARS);
        al.add(createSessionAggregation_HOURS);
        al.add(createSessionAggregation_DAYS);
        al.add(createSessionAggregation_MONTHS);
        al.add(createSessionAggregation_YEARS);
        al.add(createAlertLongSessionsTable);
        al.add(createAlertLongSessionsTableINDEX);
        al.add(createOverallAuthTable);
        al.add(createSecurityAlertTypeTable);
        al.add(createSessionInformationTable);
        al.add(createSessionInformationTableINDEX);
        al.add(createSuspiciousAlertTable);
        al.add(createSuspiciousAlertTableINDEX);
        try {
            for (String s : al) {
                statement.executeUpdate(s);
            }
            LOG.info("IS_ANALYTICS tables created in Mssql");
        } catch (SQLException e) {
            LOG.error(e);
            LOG.info("Drop all existing tables in the database & re-run the script");
        }

    }

    /**
     * Load JDBC driver. Create database connection. Create EI_ANALYTICS tables.
     */
    public void connect() {

        LOG.info("Starting migration process...");
        switch (DBTYPE.valueOf(dbType)) {
            case MYSQL:
                setJdbcDriver("com.mysql.jdbc.Driver");
                setDbUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?allowMultiQueries=true");
                LOG.info("Set JDBC driver & Database URL");
                break;
            case POSTGRESQL:
                setJdbcDriver("org.postgresql.Driver");
                setDbUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?allowMultiQueries=true");
                LOG.info("Set JDBC driver & Database URL");
                break;
            case ORACLE:
                setJdbcDriver("oracle.jdbc.driver.OracleDriver");
                setDbUrl("jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName);
                LOG.info("Set JDBC driver & Database URL");
                break;
            case MSSQL:
                setJdbcDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                setDbUrl("jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + dbName);
                LOG.info("Set JDBC driver & Database URL");
                break;
            default:
                LOG.info("Invalid Database Type");
        }

        try {
            // An abstract representation of file and directory path names.
            // This creates a new File instance by converting the given pathname
            // string into an abstract pathname.
            File file = new File(dbDriver);
            URL url = null;
            // This method constructs a file : URI that represents this abstract pathname.
            url = file.toURI().toURL();
            URLClassLoader ucl = new URLClassLoader(new URL[]{url});
            Driver driver = null;
            LOG.info("Attempting to load driver...");
            driver = (Driver) Class.forName(jdbcDriver, true, ucl).newInstance();
            DriverManager.registerDriver(new DriverWrapper(driver));
            LOG.info("Driver Loaded");

            LOG.info("Attempting to establish connection to the selected database...");
            connection = DriverManager.getConnection(dbUrl, user, pass);
            LOG.info("Connection established");

            LOG.info("Attempting to create tables in the given database...");
            statement = connection.createStatement();
            switch (DBTYPE.valueOf(dbType)) {
                case MYSQL:
                    createMySQLTables();
                    break;
                case POSTGRESQL:
                    createPostgresqlTables();
                    break;
                case ORACLE:
                    createOracleTables();
                    break;
                case MSSQL:
                    createMssqlTables();
                    break;
                default:
                    LOG.info("Invalid Database Type");
            }

        } catch (InstantiationException e) {
            LOG.error(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
        } catch (MalformedURLException e) {
            LOG.error(String.format("Error occurred while opening url, %s", e));
        } catch (ClassNotFoundException e) {
            LOG.error(String.format("Error occurred while loading driver class, %s", e));
            System.exit(1);
        } catch (SQLException e) {
            LOG.error(String.format("Error occurred while making connection to the database, %s", e));
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.error(e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOG.error(e);
                }
            }
        }
    }

    /**
     * Main class of the programme.
     *
     * @param args List of command line arguments.
     */
    public static void main(String[] args) {

        DatabaseConnection connection = new DatabaseConnection(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
        connection.connect();
    }
}

