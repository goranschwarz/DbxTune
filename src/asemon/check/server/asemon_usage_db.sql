mysql asemon_stat -u asemon_stat -p

CREATE DATABASE asemon_stat;

GRANT ALL ON asemon_stat.* TO asemon_stat@localhost IDENTIFIED BY "asemon";

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

        java_version            varchar(15),
        java_vm_version         varchar(15),
        java_vm_vendor          varchar(30),
        java_home               varchar(50),
        java_class_path         varchar(512),
        memory                  varchar(10),
        os_name                 varchar(20),
        os_version              varchar(10),
        os_arch                 varchar(10)
);
