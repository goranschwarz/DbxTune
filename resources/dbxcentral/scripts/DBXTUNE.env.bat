@echo off
rem ##----------------------------------------------
rem ## Environment files for DBXTUNE
rem ##----------------------------------------------

rem ## Where is the DBXTUNE software installed
rem ## If this is NOT set here it will be set later to the directory where the dbxtune.bat is located...
rem set "DBXTUNE_HOME=x:\some\hard\coded\path"
IF NOT DEFINED DBXTUNE_HOME (
	IF EXIST "%USERPROFILE%\dbxtune\0" (
		set "DBXTUNE_HOME=%USERPROFILE%\dbxtune\0"
	)
)

rem ## Base directory for where should we store
rem ##  * LOG   files
rem ##  * COFIG files
rem ##  * DATA  files
rem ##  * START scripts

rem ## If DBXTUNE_USER_HOME is not already set
IF NOT DEFINED DBXTUNE_USER_HOME set "DBXTUNE_USER_HOME=%USERPROFILE%\.dbxtune"

rem ## if you have Used Defined Alarm Handler Source code, where is that located
rem ## the default is ${DBXTUNE_HOME}/resources/alarm-handler-src
rem IF NOT DEFINED DBXTUNE_UD_ALARM_SOURCE_DIR set "DBXTUNE_UD_ALARM_SOURCE_DIR=%DBXTUNE_USER_HOME%\resources\alarm-handler-src"


rem ## If SYBASE is not already set (where is the 'interfaces' file located)
IF NOT DEFINED SYBASE set "SYBASE=%DBXTUNE_USER_HOME%"

rem ## Offline/recordings H2 database files will be saved here
rem ## This would normally be a soft-link to a directory that holds the recordings
rem ## The directory should normally be 300MB to 1TB so we can have a longer history
IF NOT DEFINED DBXTUNE_SAVE_DIR set "DBXTUNE_SAVE_DIR=%DBXTUNE_USER_HOME%\data"

rem ## Various LOG files will be saved here
IF NOT DEFINED DBXTUNE_LOG_DIR set "DBXTUNE_LOG_DIR=%DBXTUNE_USER_HOME%\log"

rem ## Various CONFIG files will be saved here
IF NOT DEFINED DBXTUNE_CONF_DIR set "DBXTUNE_CONF_DIR=%DBXTUNE_USER_HOME%\conf"

rem ## Various INFO files can be found here
IF NOT DEFINED DBXTUNE_INFO_DIR set "DBXTUNE_INFO_DIR=%DBXTUNE_USER_HOME%\info"

rem ## REPORT files will be saved here
IF NOT DEFINED DBXTUNE_REPORTS_DIR set "DBXTUNE_REPORTS_DIR=%DBXTUNE_USER_HOME%\reports"

rem ##------------------------------------------
rem ## DbxCentral variable (same as above but for DbxCentral)
rem ##------------------------------------------
IF NOT DEFINED DBXTUNE_CENTRAL_BASE set "DBXTUNE_CENTRAL_BASE=%DBXTUNE_USER_HOME%\dbxc"

IF NOT DEFINED DBXTUNE_CENTRAL_SAVE_DIR    set "DBXTUNE_CENTRAL_SAVE_DIR=%DBXTUNE_CENTRAL_BASE%\data"
IF NOT DEFINED DBXTUNE_CENTRAL_LOG_DIR     set "DBXTUNE_CENTRAL_LOG_DIR=%DBXTUNE_CENTRAL_BASE%\log"
IF NOT DEFINED DBXTUNE_CENTRAL_CONF_DIR    set "DBXTUNE_CENTRAL_CONF_DIR=%DBXTUNE_CENTRAL_BASE%\conf"
IF NOT DEFINED DBXTUNE_CENTRAL_INFO_DIR    set "DBXTUNE_CENTRAL_INFO_DIR=%DBXTUNE_CENTRAL_BASE%\info"
IF NOT DEFINED DBXTUNE_CENTRAL_REPORTS_DIR set "DBXTUNE_CENTRAL_REPORTS_DIR=%DBXTUNE_CENTRAL_BASE%\reports"
