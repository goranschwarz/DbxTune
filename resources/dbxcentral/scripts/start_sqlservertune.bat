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
rem ## set LOCAL environment
rem ##----------------------------------------------

set cfgFile=%DBXTUNE_CENTRAL_CONF%\sqlserver.GENERIC.conf
set logFile=%DBXTUNE_CENTRAL_LOG%\%srvName%.log
set saveDir=%DBXTUNE_CENTRAL_DATA%

rem ##----------------------------------------------
rem ## For Windows integratedSecurity: set dbmsUser=integratedSecurity
rem set dbmsUser=dbxtune
rem set dbmsUser=integratedSecurity
rem ##----------------------------------------------
set dbmsUser=integratedSecurity

rem ##----------------------------------------------
rem ## For OS monitoring set username
rem set osUserSwitch=-u dbxtune
rem ##----------------------------------------------
set osUserSwitch= 

rem ##----------------------------------------------
rem ## Override some parameters based on the %srvName%
rem ##----------------------------------------------
IF "%srvName%" == "xxx" set dbmsUser=someUserName

rem ##----------------------------------------------
rem ## Start
rem ##----------------------------------------------
rem %DBXTUNE_HOME%\bin\sqlservertune.bat -C %cfgFile% --savedir %saveDir% -L %logFile%

%DBXTUNE_HOME%\bin\sqlservertune.bat -n %cfgFile% -U%dbmsUser% -S%srvName% %osUserSwitch% -L %logFile% --savedir %saveDir% %CMDLINE_ARGS%

endlocal
