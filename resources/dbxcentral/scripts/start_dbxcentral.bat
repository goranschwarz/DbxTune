setlocal

rem ##----------------------------------------------
rem ## Source environment
rem ##----------------------------------------------
set DBXTUNE_USER_ENV_FILE=%USERPROFILE%\.dbxtune\DBXTUNE.env.bat
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
IF "%DBXTUNE_CENTRAL_BASE%" == "" set DBXTUNE_CENTRAL_BASE=%USERPROFILE%\.dbxtune\dbxc
IF "%DBXTUNE_HOME%"         == "" set DBXTUNE_HOME=%USERPROFILE%\dbxtune\0

IF "%DBXTUNE_CENTRAL_CONF%" == "" set DBXTUNE_CENTRAL_CONF=%DBXTUNE_CENTRAL_BASE%\conf
IF "%DBXTUNE_CENTRAL_DATA%" == "" set DBXTUNE_CENTRAL_DATA=%DBXTUNE_CENTRAL_BASE%\data
IF "%DBXTUNE_CENTRAL_LOG%"  == "" set DBXTUNE_CENTRAL_LOG=%DBXTUNE_CENTRAL_BASE%\log

rem ##----------------------------------------------
rem ## set LOCAL environment
rem ##----------------------------------------------

set cfgFile=%DBXTUNE_CENTRAL_CONF%\DBX_CENTRAL.conf
set logFile=%DBXTUNE_CENTRAL_LOG%\DBX_CENTRAL.log
set saveDir=%DBXTUNE_CENTRAL_DATA%

rem ##----------------------------------------------
rem ## Start
rem ##----------------------------------------------
%DBXTUNE_HOME%\bin\dbxcentral.bat -C %cfgFile% --savedir %saveDir% -L %logFile%

endlocal
