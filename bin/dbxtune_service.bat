@echo off
setlocal ENABLEDELAYEDEXPANSION

rem ========================================================================
rem  DbxTune Windows Service Manager
rem  Uses Apache Procrun (prunsrv.exe) to install/uninstall/start/stop
rem  DbxTune collectors and DbxCentral as Windows services.
rem
rem  Prerequisites:
rem    - Apache Commons Daemon (prunsrv.exe) must be on PATH or in
rem      DBXTUNE_HOME\bin\prunsrv.exe
rem    - DBXTUNE_HOME environment variable must be set
rem    - Java 11+ must be installed
rem
rem  Usage:
rem    dbxtune_service.bat install   <serviceName> <toolName> [collector args...]
rem    dbxtune_service.bat uninstall <serviceName>
rem    dbxtune_service.bat start     <serviceName>
rem    dbxtune_service.bat stop      <serviceName>
rem    dbxtune_service.bat status    <serviceName>
rem
rem  Examples:
rem    dbxtune_service.bat install DbxTune__PROD_ASE ase -n config.conf -SPROD_ASE -Usa
rem    dbxtune_service.bat install DbxTune__Central  central -C dbxcentral.conf
rem    dbxtune_service.bat start   DbxTune__PROD_ASE
rem    dbxtune_service.bat stop    DbxTune__PROD_ASE
rem    dbxtune_service.bat status  DbxTune__PROD_ASE
rem ========================================================================

set SCRIPT_DIR=%~dp0

rem --- Parse arguments
set ACTION=%~1
set SERVICE_NAME=%~2

if "%ACTION%" == "" goto usage
if "%SERVICE_NAME%" == "" goto usage

rem --- Validate DBXTUNE_HOME
if not defined DBXTUNE_HOME (
    rem Try to derive from script location (script is in DBXTUNE_HOME\bin\)
    set DBXTUNE_HOME=%SCRIPT_DIR%..
)
rem Remove trailing backslash
if "%DBXTUNE_HOME:~-1%" == "\" set DBXTUNE_HOME=%DBXTUNE_HOME:~0,-1%

if not exist "%DBXTUNE_HOME%\lib\dbxtune.jar" (
    echo ERROR: DBXTUNE_HOME does not appear to be valid: %DBXTUNE_HOME%
    echo        Could not find lib\dbxtune.jar
    echo        Please set the DBXTUNE_HOME environment variable.
    exit /b 1
)

rem --- Locate prunsrv.exe
set PRUNSRV=prunsrv.exe
if exist "%DBXTUNE_HOME%\bin\prunsrv.exe" (
    set PRUNSRV=%DBXTUNE_HOME%\bin\prunsrv.exe
)

rem Verify prunsrv.exe is accessible
where "%PRUNSRV%" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if not exist "%PRUNSRV%" (
        echo ERROR: prunsrv.exe not found.
        echo        Place it in %DBXTUNE_HOME%\bin\ or add its location to PATH.
        echo        Download from: https://commons.apache.org/proper/commons-daemon/
        exit /b 1
    )
)

rem --- Set DBXTUNE_SAVE_DIR if not already set
if not defined DBXTUNE_SAVE_DIR (
    set DBXTUNE_SAVE_DIR=%USERPROFILE%\.dbxtune
)

rem --- Set log directory for service stdout/stderr
set LOG_DIR=%DBXTUNE_SAVE_DIR%\log
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

rem --- Route to the appropriate action
if /I "%ACTION%" == "install"   goto do_install
if /I "%ACTION%" == "uninstall" goto do_uninstall
if /I "%ACTION%" == "start"     goto do_start
if /I "%ACTION%" == "stop"      goto do_stop
if /I "%ACTION%" == "status"    goto do_status

echo ERROR: Unknown action '%ACTION%'
goto usage


rem ========================================================================
rem  INSTALL
rem ========================================================================
:do_install

set TOOL_NAME=%~3
if "%TOOL_NAME%" == "" (
    echo ERROR: 'install' requires a tool name as the third argument.
    echo        Valid tool names: ase, iq, rs, rax, hana, sqlserver, oracle, postgres, mysql, db2, central
    exit /b 1
)

rem --- Validate tool name
set TOOL_VALID=0
for %%t in (ase iq rs rax hana sqlserver oracle postgres mysql db2 central) do (
    if /I "%TOOL_NAME%" == "%%t" set TOOL_VALID=1
)
if "%TOOL_VALID%" == "0" (
    echo ERROR: Unknown tool name '%TOOL_NAME%'
    echo        Valid tool names: ase, iq, rs, rax, hana, sqlserver, oracle, postgres, mysql, db2, central
    exit /b 1
)

rem --- Collect remaining arguments (after action, serviceName, toolName) as start params
rem     Procrun start params are separated by semicolons
set START_PARAMS=%TOOL_NAME%
shift & shift & shift
:parse_install_args
if "%~1" neq "" (
    set START_PARAMS=!START_PARAMS!;%~1
    shift
    goto :parse_install_args
)

rem --- Build classpath
rem     Procrun classpath entries are separated by semicolons (same as normal Java classpath)
set CP=%DBXTUNE_HOME%\classes
set CP=%CP%;%DBXTUNE_HOME%\lib\dbxtune.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\dsparser.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\log4j-core-2.24.3.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\log4j-api-2.24.3.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\log4j-slf4j2-impl-2.24.3.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\log4j-jul-2.24.3.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\slf4j-api-2.0.16.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jul-to-slf4j-2.0.16.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\h2-2.4.240.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\wizard.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\miglayout-swing-5.2.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\miglayout-core-5.2.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\swingx-all-1.6.5-1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jchart2d-3.3.2.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\planviewer.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-text-1.12.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-lang3-3.17.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-io-2.17.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-csv-1.12.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-cli-1.9.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-codec-1.17.1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-email-1.6.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jakarta.mail-1.6.7.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jakarta.activation-2.0.1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\proxy-vole_20131209.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jsch-2.27.7.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\rsyntaxtextarea-3.6.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\autocomplete-3.3.2.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\rstaui-3.3.1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\languagesupport-3.4.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jcommon-1.0.24.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jfreechart-1.5.5.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\antlr-4.0-complete.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\juniversalchardet-2.5.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\DDLGen.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\simplemagic-1.17.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jsqlparser-5.3.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\bcprov-jdk18on-1.72.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\commons-compiler-3.1.12.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\janino-3.1.12.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\balloontip-1.2.4.1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\reflections-0.9.11.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\guava-33.3.1-jre.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\javassist-3.21.0-GA.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\syslog-java-client-1.1.7.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jsendnsca-2.1.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\velocity-engine-core-2.4.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jide-oss-3.7.15.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jackson-annotations-2.18.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jackson-core-2.18.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jackson-databind-2.18.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\gson-2.11.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jetty\*
set CP=%CP%;%DBXTUNE_HOME%\lib\javax.servlet-api-3.1.0.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\cron4j-2.2.5.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\cron-utils-9.2.1.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\schemacrawler-16.2.4.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\sql-formatter-2.0.5.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jaxb-ri\*
set CP=%CP%;%USERPROFILE%\.dbxtune\jdbc_drivers\*
set CP=%CP%;%USERPROFILE%\.dbxtune\lib\*
set CP=%CP%;%DBXTUNE_HOME%\lib\jdbc_drivers\*
set CP=%CP%;%DBXTUNE_HOME%\lib\jconn42.jar
set CP=%CP%;%DBXTUNE_HOME%\lib\jconn4.jar

rem --- JVM options
rem     Procrun uses '#' as separator between multiple --JvmOptions entries in a single argument,
rem     or you can specify --JvmOptions multiple times. We use '#' for compactness.
set JVM_OPTS=-Xmx4096m#-Xms64m
set JVM_OPTS=%JVM_OPTS%#-XX:-UseGCOverheadLimit
set JVM_OPTS=%JVM_OPTS%#-XX:+HeapDumpOnOutOfMemoryError
set JVM_OPTS=%JVM_OPTS%#-XX:HeapDumpPath=%DBXTUNE_SAVE_DIR%
set JVM_OPTS=%JVM_OPTS%#-Duser.language=en
set JVM_OPTS=%JVM_OPTS%#-DDBXTUNE_HOME=%DBXTUNE_HOME%
set JVM_OPTS=%JVM_OPTS%#-DDBXTUNE_SAVE_DIR=%DBXTUNE_SAVE_DIR%
set JVM_OPTS=%JVM_OPTS%#-Djava.awt.headless=true

rem --- Add user-specified JVM options if set
if defined DBXTUNE_JVM_MEMORY_PARAMS (
    set JVM_OPTS=%DBXTUNE_JVM_MEMORY_PARAMS: =#%#-XX:-UseGCOverheadLimit#-XX:+HeapDumpOnOutOfMemoryError#-XX:HeapDumpPath=%DBXTUNE_SAVE_DIR%#-Duser.language=en#-DDBXTUNE_HOME=%DBXTUNE_HOME%#-DDBXTUNE_SAVE_DIR=%DBXTUNE_SAVE_DIR%#-Djava.awt.headless=true
)

rem --- Determine Java home for Procrun
set PRUNSRV_JAVA_HOME=
if defined DBXTUNE_JAVA_HOME (
    set PRUNSRV_JAVA_HOME=%DBXTUNE_JAVA_HOME%
) else if defined JAVA_HOME (
    set PRUNSRV_JAVA_HOME=%JAVA_HOME%
)

echo.
echo Installing service: %SERVICE_NAME%
echo   Tool:       %TOOL_NAME%
echo   Start Params: %START_PARAMS%
echo   DBXTUNE_HOME: %DBXTUNE_HOME%
echo   Log Dir:      %LOG_DIR%
echo.

rem --- Install the service
"%PRUNSRV%" install %SERVICE_NAME% ^
    --Description="DbxTune Collector Service (%TOOL_NAME%)" ^
    --DisplayName="%SERVICE_NAME%" ^
    --Startup=manual ^
    --StartMode=jvm ^
    --StartClass=com.dbxtune.service.WindowsServiceWrapper ^
    --StartMethod=start ^
    --StartParams="%START_PARAMS%" ^
    --StopMode=jvm ^
    --StopClass=com.dbxtune.service.WindowsServiceWrapper ^
    --StopMethod=stop ^
    --StopTimeout=180 ^
    --Classpath="%CP%" ^
    --JvmOptions="%JVM_OPTS%" ^
    --LogPath="%LOG_DIR%" ^
    --LogPrefix=%SERVICE_NAME% ^
    --StdOutput="%LOG_DIR%\%SERVICE_NAME%-stdout.log" ^
    --StdError="%LOG_DIR%\%SERVICE_NAME%-stderr.log" ^
    --LogLevel=Info ^
    --ServiceUser=LocalSystem ^
    --PidFile=%SERVICE_NAME%.pid ^
    --StartPath="%DBXTUNE_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install service '%SERVICE_NAME%'. Error code: %ERRORLEVEL%
    echo        Make sure you are running this script as Administrator.
    exit /b %ERRORLEVEL%
)

rem --- Set Java home if found (must be done after install via update)
if defined PRUNSRV_JAVA_HOME (
    "%PRUNSRV%" update %SERVICE_NAME% --Jvm="%PRUNSRV_JAVA_HOME%\bin\server\jvm.dll"
    if %ERRORLEVEL% NEQ 0 (
        echo WARNING: Could not set JVM path. The service will use the default JVM.
        echo          Tried: %PRUNSRV_JAVA_HOME%\bin\server\jvm.dll
    )
)

echo Service '%SERVICE_NAME%' installed successfully.
echo.
echo To start:   dbxtune_service.bat start %SERVICE_NAME%
echo To stop:    dbxtune_service.bat stop %SERVICE_NAME%
echo To remove:  dbxtune_service.bat uninstall %SERVICE_NAME%
goto end


rem ========================================================================
rem  UNINSTALL
rem ========================================================================
:do_uninstall
echo Uninstalling service: %SERVICE_NAME%
"%PRUNSRV%" delete %SERVICE_NAME%
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to uninstall service '%SERVICE_NAME%'. Error code: %ERRORLEVEL%
    echo        Make sure the service is stopped and you are running as Administrator.
    exit /b %ERRORLEVEL%
)
echo Service '%SERVICE_NAME%' uninstalled successfully.
goto end


rem ========================================================================
rem  START
rem ========================================================================
:do_start
echo Starting service: %SERVICE_NAME%
net start %SERVICE_NAME%
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to start service '%SERVICE_NAME%'. Error code: %ERRORLEVEL%
    echo        Check logs at: %LOG_DIR%\%SERVICE_NAME%*
    exit /b %ERRORLEVEL%
)
goto end


rem ========================================================================
rem  STOP
rem ========================================================================
:do_stop
echo Stopping service: %SERVICE_NAME%
net stop %SERVICE_NAME%
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to stop service '%SERVICE_NAME%'. Error code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
goto end


rem ========================================================================
rem  STATUS
rem ========================================================================
:do_status
sc query %SERVICE_NAME%
goto end


rem ========================================================================
rem  USAGE
rem ========================================================================
:usage
echo.
echo DbxTune Windows Service Manager
echo.
echo Usage:
echo   %~nx0 install   ^<serviceName^> ^<toolName^> [collector args...]
echo   %~nx0 uninstall ^<serviceName^>
echo   %~nx0 start     ^<serviceName^>
echo   %~nx0 stop      ^<serviceName^>
echo   %~nx0 status    ^<serviceName^>
echo.
echo Tool names: ase, iq, rs, rax, hana, sqlserver, oracle, postgres, mysql, db2, central
echo.
echo Examples:
echo   %~nx0 install DbxTune__PROD_ASE ase -n config.conf -SPROD_ASE -Usa -Psecret
echo   %~nx0 install DbxTune__Central  central -C dbxcentral.conf
echo   %~nx0 start   DbxTune__PROD_ASE
echo   %~nx0 stop    DbxTune__PROD_ASE
echo   %~nx0 status  DbxTune__PROD_ASE
echo   %~nx0 uninstall DbxTune__PROD_ASE
echo.
exit /b 1

:end
endlocal
