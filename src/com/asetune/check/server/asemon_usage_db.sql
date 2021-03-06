#
# Creating the database
#
mysql -u asemon_stat -p asemon -D asemon_stat

CREATE DATABASE asemon_stat;

GRANT ALL ON asemon_stat.* TO asemon_stat@localhost IDENTIFIED BY "asemon";

#
# Creating the tables
#
mysql --user=asemon_stat --password=asemon --database=asemon_stat

USE asemon_stat;

DROP TABLE asemon_usage;

CREATE TABLE asemon_usage
(
	rowid                   int   not null   auto_increment    primary key,
	
	serverAddTime           timestamp,
	clientCheckTime         timestamp,
	
	serverSourceVersion     int,
	
	clientSourceDate        timestamp,
	clientSourceVersion     int,
	clientAsemonVersion     varchar(20),
	appStartupTime          varchar(10),
	clientExpireDate        varchar(10),
	
	clientHostName          varchar(40),
	clientHostAddress       varchar(20),
	clientCanonicalHostName varchar(40),
	callerIpAddress         varchar(20),
	
	user_name               varchar(30),
	user_home               varchar(50),
	user_dir                varchar(50),
	propfile                varchar(100),
	gui                     varchar(10),
	
	java_version            varchar(20),
	java_vm_version         varchar(15),
	java_vm_vendor          varchar(30),
	java_home               varchar(50),
	java_class_path         varchar(512),
	sun_arch_data_model     varchar(10),
	memory                  varchar(10),
	os_name                 varchar(20),
	os_version              varchar(20),
	os_arch                 varchar(20),
	
	sun_desktop             varchar(15),
	user_country            varchar(5),
	user_language           varchar(5),
	user_timezone           varchar(30)
);

CREATE TABLE asemon_connect_info
(
	checkId                 int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),
	
	srvVersion              int,
	isClusterEnabled        int,
	connectId               int,
	
	srvName                 varchar(30),
	srvIpPort               varchar(100),
	sshTunnelInfo           varchar(100),
	srvUser                 varchar(30),
	srvUserRoles            varchar(300),
	srvVersionStr           varchar(150),
	srvSortOrderId          varchar(5),
	srvSortOrderName        varchar(30),
	srvCharsetId            varchar(5),
	srvCharsetName          varchar(30),
	srvSapSystemInfo        varchar(40),
	
	usePcs                  varchar(5),
	pcsConfig               varchar(400),
	
	
	PRIMARY KEY (checkId, connectId, serverAddTime)
);

CREATE TABLE asemon_mda_info
(
	type                    char(1),
	checkId                 int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),
	verified                char(1),
	
	srvVersion              int,
	isClusterEnabled        int,
	
	rowId                   int,
	expectedRows            int,
	
	TableName               varchar(255),
	TableID                 int,
	ColumnName              varchar(255),
	ColumnID                int,
	TypeName                varchar(30),
	Length                  int,
	Indicators              int,
	Description             varchar(400),
	
	PRIMARY KEY (type, srvVersion, isClusterEnabled, TableName, ColumnName)
);

CREATE TABLE asemon_udc_info
(
	checkId                 int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	
	userName                varchar(30),
	udcKey                  varchar(100),
	udcValue                varchar(1024),
	
	PRIMARY KEY (userName, udcKey)
);

CREATE TABLE asemon_counter_usage_info
(
	checkId                 int,
	serverAddTime           timestamp,
	sessionType             varchar(10),
	sessionStartTime        timestamp        NOT NULL,
	sessionEndTime          timestamp            NULL,
--	clientTime              timestamp, -- removed 2011-11-09
	userName                varchar(30),
	connectId               int,
	
	cmName                  varchar(30),
	addSequence             int,
	refreshCount            int,
	sumRowCount             int,
	
	PRIMARY KEY (checkId, connectId, clientTime, cmName)
);

CREATE TABLE asemon_error_info
(
	checkId                 int,
	sendCounter             int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),

	srvVersion              varchar(30),
	appVersion              varchar(30),

	logLevel                varchar(10),
	logThreadName           varchar(50),
	logClassName            varchar(50),
	logLocation             varchar(100),
	logMessage              varchar(4096),
	logStacktrace           varchar(4096),

	PRIMARY KEY (checkId, sendCounter, serverAddTime)
);

CREATE TABLE asemon_error_info2
(
	checkId                 int,
	sendCounter             int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),

	srvVersion              varchar(30),
	appVersion              varchar(30),

	logLevel                varchar(10),
	logThreadName           varchar(50),
	logClassName            varchar(50),
	logLocation             varchar(100),
	logMessage              varchar(4096),
	logStacktrace           varchar(4096),

	PRIMARY KEY (checkId, sendCounter, serverAddTime)
);

CREATE TABLE asemon_error_info_save
(
	checkId                 int,
	sendCounter             int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),

	srvVersion              varchar(30),
	appVersion              varchar(30),

	logLevel                varchar(10),
	logThreadName           varchar(50),
	logClassName            varchar(50),
	logLocation             varchar(100),
	logMessage              varchar(4096),
	logStacktrace           varchar(4096),

	PRIMARY KEY (checkId, sendCounter, serverAddTime)
);



CREATE TABLE sqlw_usage
(
	sqlwCheckId             int   not null   auto_increment    primary key,

	serverAddTime           timestamp,
	clientCheckTime         timestamp,
	
	serverSourceVersion     int,
	
	clientSourceDate        timestamp,
	clientSourceVersion     int,
	clientAppVersion        varchar(20),
	appStartupTime          varchar(10),
	
	clientHostName          varchar(40),
	clientHostAddress       varchar(20),
	clientCanonicalHostName varchar(40),
	callerIpAddress         varchar(20),
	
	user_name               varchar(30),
	user_home               varchar(50),
	user_dir                varchar(50),
	propfile                varchar(100),
	sun_desktop             varchar(15),
	user_country            varchar(5),
	user_language           varchar(5),
	user_timezone           varchar(15)
	
	java_version            varchar(20),
	java_vm_version         varchar(15),
	java_vm_vendor          varchar(30),
	sun_arch_data_model     varchar(10),
	java_home               varchar(50),
	memory                  varchar(10),
	os_name                 varchar(20),
	os_version              varchar(20),
	os_arch                 varchar(20)
);

CREATE TABLE sqlw_connect_info
(
	sqlwCheckId             int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),
	
	connectId               int,
	connTypeStr             varchar(30),
	
	prodName                varchar(30),
	prodVersionStr          varchar(255),

	jdbcDriver              varchar(60),
	jdbcUrl                 varchar(255),
	jdbcDriverName          varchar(255),
	jdbcDriverVersion       varchar(255),

	srvVersionInt           int,
	srvName                 varchar(30),
	srvUser                 varchar(30),
	srvCharsetName          varchar(30),
	srvSortOrderName        varchar(30),
	
	sshTunnelInfo           varchar(100),
	
	
	PRIMARY KEY (sqlwCheckId, connectId, serverAddTime)
);


CREATE TABLE sqlw_usage_info
(
	sqlwCheckId             int,
	serverAddTime           timestamp,
	clientTime              timestamp,
	userName                varchar(30),
	
	connectId               int,

	connTypeStr             varchar(30),	
	prodName                varchar(30),
	srvVersionInt           int,

	connectTime             timestamp,
	disconnectTime          timestamp,

	execMainCount           int,
	execBatchCount          int,
	execTimeTotal           int,
	execTimeSqlExec         int,
	execTimeRsRead          int,
	rsCount                 int,
	rsRowsCount             int,
	iudRowsCount            int,
	sqlWarningCount         int,
	sqlExceptionCount       int,

	PRIMARY KEY (sqlwCheckId, connectId, serverAddTime)
);

CREATE TABLE IF NOT EXISTS dbxc_store_info (
	checkId                int(11)        NOT NULL,
	serverAddTime          timestamp      NOT NULL   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	userName               varchar(30)        NULL   DEFAULT NULL,
	shutdownReason         varchar(60)        NULL   DEFAULT NULL,
	wasRestartSpecified    int(11)            NULL   DEFAULT NULL,
	writerJdbcUrl          varchar(1024)      NULL   DEFAULT NULL,
	H2DbFileSize1InMb      int(11)            NULL   DEFAULT NULL,
	H2DbFileSize2InMb      int(11)            NULL   DEFAULT NULL,
	H2DbFileSizeDiffInMb   int(11)            NULL   DEFAULT NULL,
	PRIMARY KEY (checkId,serverAddTime)
)

CREATE TABLE IF NOT EXISTS dbxc_store_srv_info (
	checkId                int(11)        NOT NULL,
	serverAddTime          timestamp      NOT NULL   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	userName               varchar(30)        NULL   DEFAULT NULL,
	srvName                varchar(30)    NOT NULL,
	dbxProduct             varchar(30)        NULL   DEFAULT NULL,
	firstSamleTime         timestamp      NOT NULL   DEFAULT '0000-00-00 00:00:00',
	lastSamleTime          timestamp          NULL   DEFAULT NULL,
	alarmCount             int(11)            NULL   DEFAULT NULL,
	receiveCount           int(11)            NULL   DEFAULT NULL,
	receiveGraphCount      int(11)            NULL   DEFAULT NULL,
	PRIMARY KEY (checkId, serverAddTime, srvName)
)
