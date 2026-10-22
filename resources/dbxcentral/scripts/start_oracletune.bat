setlocal

rem ##--------------------------------------------------------------------
rem ## move all (except the first) cmdline parameters into CMDLINE_ARGS
rem ##--------------------------------------------------------------------
set srvName=%1
shift
set "CMDLINE_ARGS="
:parse_cmd_line
if "%~1" neq "" (
  set CMDLINE_ARGS=%CMDLINE_ARGS% %1
  shift
  goto :parse_cmd_line
)
if defined CMDLINE_ARGS set CMDLINE_ARGS=%CMDLINE_ARGS:~1%
rem echo Passed cmdline parameters: %CMDLINE_ARGS%

rem ##----------------------------------------------
rem ## Source environment
rem ##----------------------------------------------
IF NOT DEFINED DBXTUNE_USER_ENV_FILE set DBXTUNE_USER_ENV_FILE=%USERPROFILE%\.dbxtune\DBXTUNE.env.bat
if exist "%DBXTUNE_USER_ENV_FILE%" (
	echo .
	echo -----------------------------------------------------------------------
	echo Sourcing local environment from: %DBXTUNE_USER_ENV_FILE%
	echo -----------------------------------------------------------------------
	call "%DBXTUNE_USER_ENV_FILE%"
)

rem ##----------------------------------------------
rem ## Some basic directories
rem ##----------------------------------------------
IF NOT DEFINED DBXTUNE_CENTRAL_BASE set DBXTUNE_CENTRAL_BASE=%USERPROFILE%\.dbxtune\dbxc
IF NOT DEFINED DBXTUNE_HOME         set DBXTUNE_HOME=%USERPROFILE%\dbxtune\0

IF NOT DEFINED DBXTUNE_CENTRAL_CONF set DBXTUNE_CENTRAL_CONF=%DBXTUNE_CENTRAL_BASE%\conf
IF NOT DEFINED DBXTUNE_CENTRAL_DATA set DBXTUNE_CENTRAL_DATA=%DBXTUNE_CENTRAL_BASE%\data
IF NOT DEFINED DBXTUNE_CENTRAL_LOG  set DBXTUNE_CENTRAL_LOG=%DBXTUNE_CENTRAL_BASE%\log

rem ##----------------------------------------------
rem ## NOTE: The server name for Oracle is: host:port/serviceName  (example: prod-ora1:1521/ORCL)
rem ##       Since it contains ':' and '/' you MUST also pass: -A alternateSrvName  (example: -A prod-ora1-ORCL)
rem ##       The alternate server name is used for log files, H2 Database Recordings, DbxCentral SchemaName, etc...
rem ##       In the SERVER_LIST file it looks like:
rem ##         prod-ora1:1521/ORCL ; 1 ; Some Desc ; ${DBXTUNE_CENTRAL_BASE}/bin/start_oracletune.bat <SRVNAME> -A prod-ora1-ORCL
rem ##       This is also why the log is a DIRECTORY here (the log file gets the alternate server name)
rem ##----------------------------------------------

rem ##----------------------------------------------
rem ## set LOCAL environment
rem ##----------------------------------------------

set cfgFile=%DBXTUNE_CENTRAL_CONF%\oracle.GENERIC.conf
set logDir=%DBXTUNE_CENTRAL_LOG%
set saveDir=%DBXTUNE_CENTRAL_DATA%

rem ##----------------------------------------------
rem ## The Oracle login to monitor with. How to create it: see DbmsSetup\oracle.txt
rem ## In a Container Database, connected to CDB$ROOT, the login is: c##dbxtune
rem ## You need to add the password using: dbxPassword.bat set -UdbmsUser -SsrvName -PtheSecretPassword
rem set dbmsUser=c##dbxtune
rem ##----------------------------------------------
set dbmsUser=dbxtune

rem ##----------------------------------------------
rem ## For OS monitoring set username
rem set osUserSwitch=-u dbxtune
rem ##----------------------------------------------
set osUserSwitch=

rem ##----------------------------------------------
rem ## Override some parameters based on the %srvName%
rem ##----------------------------------------------
IF "%srvName%" == "xxx" set dbmsUser=someUserName
IF "%srvName%" == "yyy" set cfgFile=%DBXTUNE_CENTRAL_CONF%\oracle.YYY.conf

rem ##----------------------------------------------
rem ## Start
rem ##----------------------------------------------

%DBXTUNE_HOME%\bin\oracletune.bat -n %cfgFile% -U%dbmsUser% -S%srvName% %osUserSwitch% -L %logDir% --savedir %saveDir% %CMDLINE_ARGS%

endlocal
