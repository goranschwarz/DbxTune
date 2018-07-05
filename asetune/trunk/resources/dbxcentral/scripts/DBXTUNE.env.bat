rem ##----------------------------------------------
rem ## Environment files for DBXTUNE
rem ##----------------------------------------------

rem ## Where is the DBXTUNE software installed
rem IF "%DBXTUNE_HOME%"=="" set set DBXTUNE_HOME=%systemdrive%%homepath%\dbxtune\0

rem ## Base directory for where should we store
rem ##  * LOG   files
rem ##  * COFIG files
rem ##  * DATA  files
rem ##  * START scripts

rem ## If DBXTUNE_CENTRAL_BASE is not already set
IF "%DBXTUNE_CENTRAL_BASE%"=="" set set DBXTUNE_CENTRAL_BASE=%systemdrive%%homepath%\.dbxtune

rem ## If DBXTUNE_USER_HOME is not already set
IF "%DBXTUNE_USER_HOME%"=="" set set DBXTUNE_USER_HOME=%systemdrive%%homepath%\.dbxtune

rem ## if you have Used Defined Alarm Handler Source code, where is that located
rem ## the default is ${DBXTUNE_HOME}/resources/alarm-handler-src
rem IF "%DBXTUNE_UD_ALARM_SOURCE_DIR%"=="" set set DBXTUNE_UD_ALARM_SOURCE_DIR=%DBXTUNE_USER_HOME%\resources\alarm-handler-src


rem ## If SYBASE is not already set
IF "%SYBASE%"=="" set SYBASE=%DBXTUNE_USER_HOME%

rem ## Offline/recordings H2 database files will be saved here
rem ## This would normally be a soft-link to a directory that holds the recordings
rem ## The directory should normally be 300MB to 1TB so we can have a longer history
IF "%DBXTUNE_SAVE_DIR%"=="" set DBXTUNE_SAVE_DIR=%DBXTUNE_USER_HOME%\dbxc\data

rem ## Various LOG files will be saved here
IF "%DBXTUNE_LOG_DIR%"=="" set DBXTUNE_LOG_DIR=%DBXTUNE_USER_HOME%\dbxc\log

rem ## Various CONFIG files will be saved here
IF "%DBXTUNE_CONF_DIR%"=="" set DBXTUNE_CONF_DIR=%DBXTUNE_USER_HOME%\dbxc\conf

