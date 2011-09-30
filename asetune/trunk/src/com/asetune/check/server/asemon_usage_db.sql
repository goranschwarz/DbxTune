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

        clientHostName          varchar(40),
        clientHostAddress       varchar(20),
        clientCanonicalHostName varchar(40),

        user_name               varchar(30),
        user_dir                varchar(50),
        propfile                varchar(100),
        gui                     varchar(10),
        sun_desktop             varchar(15),
        user_country            varchar(5),
        user_language           varchar(5),
        user_timezone           varchar(15),

        java_version            varchar(20),
        java_vm_version         varchar(15),
        java_vm_vendor          varchar(30),
        java_home               varchar(50),
        java_class_path         varchar(512),
        memory                  varchar(10),
        os_name                 varchar(20),
        os_version              varchar(20),
        os_arch                 varchar(20)
);

CREATE TABLE asemon_connect_info
(
		checkId                 int,
        serverAddTime           timestamp,
        clientTime              timestamp,
        userName                varchar(30),

        srvVersion              int,
        isClusterEnabled        int,
        
		srvName                 varchar(30),
		srvIpPort               varchar(30),
		srvUser                 varchar(30),
		srvVersionStr           varchar(150),

		usePcs                  varchar(5),
		pcsConfig               varchar(400),


		PRIMARY KEY (checkId, serverAddTime)
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
        clientTime              timestamp,
        userName                varchar(30),

        cmName                  varchar(30),
        addSequence             int,
        refreshCount            int,
        sumRowCount             int,

        PRIMARY KEY (checkId, cmName)
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
	logMessage              varchar(500),
	logStacktrace           varchar(4096),

	PRIMARY KEY (checkId, sendCounter, serverAddTime)
);
