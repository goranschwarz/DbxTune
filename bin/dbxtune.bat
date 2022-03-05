@echo off
setlocal
rem mode con:cols=800 lines=3000

set STARTDIR=%~dp0
set APP_NAME=%1
rem ------------------------------------------------------------------------
rem --- move all (except the first) cmdline parameters into ARGS
rem ------------------------------------------------------------------------
shift
set "CMDLINE_ARGS="
:parse_cmd_line
if "%~1" neq "" (
  set CMDLINE_ARGS=%CMDLINE_ARGS% %1
  shift
  goto :parse_cmd_line
)
if defined CMDLINE_ARGS set CMDLINE_ARGS=%CMDLINE_ARGS:~1%
echo Passed cmdline parameters: %CMDLINE_ARGS%


rem ------------------------------------------------------------------------
rem --- Set various stuff based on the passed application name
rem ------------------------------------------------------------------------
set JAVA_START_CLASS=""
set JAVA_START_PARAMS=""

IF "%APP_NAME%" == "ase" (
	set JAVA_START_CLASS=com.asetune.AseTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/asetune_splash.jpg

) ELSE IF "%APP_NAME%" == "iq" (
	set JAVA_START_CLASS=com.asetune.IqTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/iqtune_splash.jpg

) ELSE IF "%APP_NAME%" == "rs" (
	set JAVA_START_CLASS=com.asetune.RsTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/rstune_splash.jpg

) ELSE IF "%APP_NAME%" == "rax" (
	set JAVA_START_CLASS=com.asetune.RaxTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/raxtune_splash.jpg

) ELSE IF "%APP_NAME%" == "hana" (
	set JAVA_START_CLASS=com.asetune.HanaTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/hanatune_splash.jpg

) ELSE IF "%APP_NAME%" == "sqlserver" (
	set JAVA_START_CLASS=com.asetune.SqlServerTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/sqlservertune_splash.jpg

) ELSE IF "%APP_NAME%" == "oracle" (
	set JAVA_START_CLASS=com.asetune.OracleTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/oracletune_splash.jpg

) ELSE IF "%APP_NAME%" == "postgres" (
	set JAVA_START_CLASS=com.asetune.PostgresTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/postgrestune_splash.jpg

) ELSE IF "%APP_NAME%" == "mysql" (
	set JAVA_START_CLASS=com.asetune.MySqlTune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/mysqltune_splash.jpg

) ELSE IF "%APP_NAME%" == "db2" (
	set JAVA_START_CLASS=com.asetune.Db2Tune
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/db2tune_splash.jpg

) ELSE IF "%APP_NAME%" == "sqlw" (
	set JAVA_START_CLASS=com.asetune.tools.sqlw.QueryWindow
	set JAVA_START_PARAMS=
	set SPLASH=-splash:lib/sqlw_splash.jpg

) ELSE IF "%APP_NAME%" == "central" (
	set JAVA_START_CLASS=com.asetune.central.DbxTuneCentral
	set JAVA_START_PARAMS=
	set SPLASH=

) ELSE IF "%APP_NAME%" == "dsr" (
	set JAVA_START_CLASS=com.asetune.pcs.report.DailySummaryReport
	set JAVA_START_PARAMS=
	set SPLASH=

) ELSE IF "%APP_NAME%" == "h2fix" (
	set JAVA_START_CLASS=com.asetune.central.pcs.H2CentralDbCopy
	set JAVA_START_PARAMS=
	set SPLASH=

) ELSE IF "%APP_NAME%" == "dbxcdbcopy" (
	set JAVA_START_CLASS=com.asetune.central.pcs.H2CentralDbCopy2
	set JAVA_START_PARAMS=
	set SPLASH=

) ELSE IF "%APP_NAME%" == "h2srv" (
	set JAVA_START_CLASS=org.h2.tools.Server
	set JAVA_START_PARAMS=-tcp -tcpAllowOthers -ifExists
	set SPLASH=

) ELSE (
	echo ""
	echo "#########################################################################"
	echo "Unknow toolset '%APP_NAME%'"
	echo "#########################################################################"
	echo ""
	echo "usage: dbxtune toolset [cmdLineSwitches]"
	echo ""
	echo "Available toolset is:"
	echo " ase        - Sybase/SAP Adaptive Server Enterprise"
	echo " iq         - Sybase/SAP IQ - The Column Store DB"
	echo " rs         - Sybase/SAP Replication Server"
	echo " rax        - Sybase/SAP Replication Agent for X"
	echo " hana       - Sybase/SAP HANA in-memory Column Store DB"
	echo " sqlserver  - Microsoft SQL-Server"
	echo " oracle     - Oracle"
	echo " postgres   - Postgres"
	echo " mysql      - MySQL"
	echo " db2        - DB2 LUW (Linux Unix Windows)"
	echo ""
	echo " sqlw       - SQL Window a JDBC Query Tool"
	echo " central    - A Component for multiple instances"
	echo "              If you want some central Web based view"
	echo " dbxcdbcopy - Copy a DBX Cental db to a new destination (used to migrate to a new DBMS)"
	echo ""

	pause
	exit 1
)

title "%APP_NAME%-console"


rem ------------------------------------------------------------------------
rem --- set DBXTUNE_HOME to current directory if NOT already set
rem --- If STARTDIR ends with \bin\ remove that part... (c:\program\asetune\bin\ -->> c:\program\asetune\)
rem ------------------------------------------------------------------------
IF "%DBXTUNE_HOME%"=="" (
	set DBXTUNE_HOME=%STARTDIR%
	if /i [%STARTDIR:~-5%]==[\bin\] set DBXTUNE_HOME=%STARTDIR:~0,-4%
)

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase


rem ------------------------------------------------------------------------
rem --- Source environment
rem ------------------------------------------------------------------------
rem set DBXTUNE_USER_ENV_FILE=%systemdrive%%homepath%\.asetune\DBXTUNE.env.bat
set DBXTUNE_USER_ENV_FILE=%systemdrive%%homepath%\.dbxtune\DBXTUNE.env.bat
if exist "%DBXTUNE_USER_ENV_FILE%" (
	echo .
	echo -----------------------------------------------------------------------
	echo Sourcing local environment from: %DBXTUNE_USER_ENV_FILE%
	echo -----------------------------------------------------------------------
	call "%DBXTUNE_USER_ENV_FILE%"
) else (
	echo .
	echo -----------------------------------------------------------------------
	echo NOTE: you can setup local environment in file: %DBXTUNE_USER_ENV_FILE%
	echo -----------------------------------------------------------------------
)


rem ------------------------------------------------------------------------
rem --- remove last '\' char from: DBXTUNE_HOME and SYBASE
rem --- DBXTUNE_HOME exists every time, while SYBASE is NOT mandatory
rem --- Also for SYBASE
rem ---    Trim begin/end Quote (") character from the SYBASE environment variable 
rem ---    The FOR command can be used to safely remove quotes surrounding a string. 
rem ---    If SYBASE does not have quotes then it will remain unchanged.
rem ---    If SYBASE is not set, it will still NOT be set after this
rem --- Tested the below with SYBASE set to:
rem --- set SYBASE=
rem --- set SYBASE=c:\Program Files\sybase
rem --- set SYBASE=c:\Program Files\sybase\
rem --- set SYBASE="c:\Program Files\sybase"
rem --- set SYBASE="c:\Program Files\sybase\"
rem --- set SYBASE=c:\sybase
rem ------------------------------------------------------------------------
IF %DBXTUNE_HOME:~-1%==\ SET DBXTUNE_HOME=%DBXTUNE_HOME:~0,-1%

IF NOT DEFINED SYBASE goto afterStripSybase
for /f "useback tokens=*" %%a in ('%SYBASE%') do set SYBASE=%%~a
IF %SYBASE:~-1%==\ SET SYBASE=%SYBASE:~0,-1%
:afterStripSybase



rem ------------------------------------------------------------------------
rem --- set some CONSOLE windows and buffer size
rem ------------------------------------------------------------------------
rem ---set /a "winsize=(64 << 16) + 80"
rem ---set /a "bufsize=(3000 << 16) + 800"
rem --->nul reg add "hkcu\console\%title%" /v WindowSize /t REG_DWORD /d "%winsize%" /f
rem --->nul reg add "hkcu\console\%title%" /v ScreenBufferSize /t REG_DWORD /d "%bufsize%" /f



rem ------------------------------------------------------------------------
rem --- set some default environment variables
rem ------------------------------------------------------------------------
set APPL_HOME=%DBXTUNE_HOME%

IF "%DBXTUNE_SAVE_DIR%"=="" (
	if exist %systemdrive%%homepath%\.dbxtune\data (
		set DBXTUNE_SAVE_DIR=%systemdrive%%homepath%\.dbxtune\data
	) else (
		set DBXTUNE_SAVE_DIR=%DBXTUNE_HOME%\data
	)
) 

rem set JAVA_HOME=%SYBASE_JRE%
rem set JAVA_HOME=%SYBASE%\shared-1_0\JRE-1_4
rem set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_07


rem ------------------------------------------------------------------------
rem --- get JVM Parameters from file: 
rem ------------------------------------------------------------------------
IF "%APP_NAME%" == "sqlw" (
	set DBXTUNE_JVM_PARAMETER_FILE=%HOMEDRIVE%%HOMEPATH%\.dbxtune\.sqlw_jvm_settings.properties
) ELSE (
	set DBXTUNE_JVM_PARAMETER_FILE=%HOMEDRIVE%%HOMEPATH%\.dbxtune\.dbxtune_jvm_settings.properties
)
if exist "%DBXTUNE_JVM_PARAMETER_FILE%" (
	echo .
	echo -----------------------------------------------------------------------
	echo Reading DBXTUNE_JVM_PARAMETER_FILE: %DBXTUNE_JVM_PARAMETER_FILE%
	echo -----------------------------------------------------------------------
	setlocal disabledelayedexpansion
	FOR /F "tokens=1* delims==" %%i IN ("%DBXTUNE_JVM_PARAMETER_FILE%") DO (
		echo     Found parameter '%%i' which is set to '%%j'
		set "%%i=%%j"
	)
) else (
	echo .
	echo The DBXTUNE_JVM_PARAMETER_FILE: %DBXTUNE_JVM_PARAMETER_FILE% did NOT EXIST
)


rem ------------------------------------------------------------------------
rem --- set JVM MEMORY parameters
rem ------------------------------------------------------------------------
echo NOTE: Set/Change JVM Memory parameters by setting Environment variable: DBXTUNE_JVM_MEMORY_PARAMS which may be stored in %DBXTUNE_JVM_PARAMETER_FILE%

set JVM_MEMORY_PARAMS_32=-Xmx1024m -Xms64m
set JVM_MEMORY_PARAMS_64=-Xmx4096m -Xms64m

IF DEFINED DBXTUNE_JVM_MEMORY_PARAMS set JVM_MEMORY_PARAMS_32=%DBXTUNE_JVM_MEMORY_PARAMS%
IF DEFINED DBXTUNE_JVM_MEMORY_PARAMS set JVM_MEMORY_PARAMS_64=%DBXTUNE_JVM_MEMORY_PARAMS%


rem ------------------------------------------------------------------------
rem --- set JVM GARBAGE COLLECTION parameters
rem ------------------------------------------------------------------------
set JVM_GC_PARAMS_32=
set JVM_GC_PARAMS_64=

IF DEFINED DBXTUNE_JVM_GC_PARAMS set JVM_GC_PARAMS_32=%DBXTUNE_JVM_GC_PARAMS%
IF DEFINED DBXTUNE_JVM_GC_PARAMS set JVM_GC_PARAMS_64=%DBXTUNE_JVM_GC_PARAMS%


rem ------------------------------------------------------------------------
rem --- set OTHER JVM parameters   and   DEBUG stuff
rem --- -noverify is to get around: 'Caught: java.lang.VerifyError: Inconsistent stackmap frames at branch target 39' when kicking off SQL-Window from the context menu...
rem ------------------------------------------------------------------------
rem --- set JVM_PARAMS=%JVM_PARAMS% -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
rem --- set JVM_PARAMS=%JVM_PARAMS% -Xrunhprof:cpu=samples,depth=16
rem --- set JVM_PARAMS=%JVM_PARAMS% -Dhttp.proxyHost=www-proxy.domain.com -Dhttp.proxyPort=8080
rem --- set JVM_PARAMS=%JVM_PARAMS% -Djava.net.useSystemProxies=true
set JVM_PARAMS=-noverify
rem set JVM_PARAMS=-noverify -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=1s,duration=60s,name=AseTuneStartup,filename=C:\tmp\AseTuneStartup.jfr,settings=profile
rem set JVM_PARAMS=-noverify -XX:+UnlockCommercialFeatures -XX:+FlightRecorder

set EXTRA=%NOGUI%
rem --- set DEBUG_OPTIONS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=2323,server=y,suspend=n
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=times
set DEBUG_OPTIONS=



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
rem IF NOT DEFINED SYBASE GOTO no_sybase
IF NOT DEFINED DBXTUNE_HOME GOTO no_dbxtunehome
rem IF NOT DEFINED JAVA_HOME GOTO no_javahome


rem ------------------------------------------------------------------------
rem --- remove the SPLASH if -n or -noGui command line switch is specified
rem --- You got to "love" DOS for being CRYPTICAL... or NOT...
rem ------------------------------------------------------------------------
for %%c in (%*) do (
	echo(%%c|findstr /r /c:"^-n.*"      >nul && ( set SPLASH= )
	echo(%%c|findstr /r /c:"^--noGui.*" >nul && ( set SPLASH= )
)


rem ------------------------------------------------------------------------
rem --- setup the CLASSPATH
rem ------------------------------------------------------------------------
set CLASSPATH=%DBXTUNE_HOME%\classes
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\asetune.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\dsparser.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\log4j-1.2.17.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\h2-SNAPSHOT.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\h2-1.4.200.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\wizard.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\miglayout-swing-5.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\miglayout-core-5.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\swingx-all-1.6.5-1.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jchart2d-3.3.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\planviewer.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-text-1.9.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-lang3-3.7.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-io-2.6.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-csv-1.5.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-cli-1.4.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-codec-1.10.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\proxy-vole_20131209.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\ganymed-ssh2-263.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\rsyntaxtextarea.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\autocomplete.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\rstaui.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\language_support.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jcommon-1.0.21.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jfreechart-1.5.1.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\antlr-4.0-complete.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\juniversalchardet-2.3.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\DDLGen.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\simplemagic-1.14.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jsqlparser-3.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\gsp.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\bcprov-jdk15on-157.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-compiler-3.0.7.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\janino-3.0.7.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\balloontip-1.2.4.1.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\reflections-0.9.11.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\guava-20.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\javassist-3.21.0-GA.jar
rem set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\cloning-1.9.6.jar
rem set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\objenesis-2.6.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\syslog-java-client-1.1.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\javax.mail.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\commons-email-1.4.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jsendnsca-2.1.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jul-to-slf4j-1.7.29.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\slf4j-api-1.7.29.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\slf4j-log4j12-1.7.29.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\velocity-engine-core-2.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jide-oss-3.6.18.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jackson-annotations-2.9.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jackson-core-2.9.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jackson-databind-2.9.2.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\gson-2.8.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jetty\*
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\javax.servlet-api-3.1.0.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\cron4j-2.2.5.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\cron-utils-7.0.6.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\schemacrawler-16.2.4.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\sql-formatter-2.0.3.jar

rem set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\tomcat\*

rem --- In Java 9 and many different JDBC Drivers, Oracles (ojdbc7.jar) needs to be first otherwise there will be stacktraces with problems of loading drivers etc...
rem --- In Java 9 and many different JDBC Drivers, Sybase jconn4.jar needs to be added *last*
rem set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jdbc_drivers\ojdbc7.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.dbxtune\jdbc_drivers\*
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.dbxtune\lib\*
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jdbc_drivers\*
set CLASSPATH=%CLASSPATH%;%EXTRA_JDBC_DRIVERS%
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jconn42.jar
set CLASSPATH=%CLASSPATH%;%DBXTUNE_HOME%\lib\jconn4.jar


rem echo CLASSPATH=%CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%DBXTUNE_JAVA_HOME%\bin;%JAVA_HOME%\bin;%PATH%


echo .
echo -----------------------------------------------------------------------
echo Checking if Java is installed and Java VERSION.
echo -----------------------------------------------------------------------

rem ------------------------------------------------------------------------
rem --- Just for informational purposes, print out the Java Version we are using
rem ------------------------------------------------------------------------
java -version
IF %ERRORLEVEL% NEQ 0 GOTO no_java



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
rem --Parses x out of 1.x; for example 8 out of java version 1.8.0_xx
rem -- Otherwise, parses the major version; 9 out of java version 9-ea
set JAVA_VERSION_MAJOR=0
for /f "tokens=3" %%g in ('java -Xms32M -Xmx32M -version 2^>^&1 ^| findstr /i "version"') do (
  set JAVA_VERSION_MAJOR=%%g
)
set JAVA_VERSION_MAJOR=%JAVA_VERSION_MAJOR:"=%
for /f "delims=.-_ tokens=1-2" %%v in ("%JAVA_VERSION_MAJOR%") do (
  if /I "%%v" EQU "1" (
    set JAVA_VERSION_MAJOR=%%w
  ) else (
    set JAVA_VERSION_MAJOR=%%v
  )
)

rem -- @echo %JAVA_VERSION_MAJOR%
IF %JAVA_VERSION_MAJOR% LSS 7 GOTO to_low_java_version



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
rem -- java com.asetune.utils.JavaVersion 7
rem -- IF %ERRORLEVEL% NEQ 0 GOTO to_low_java_version



rem ------------------------------------------------------------------------
rem --- SET memory parameters, if 64 bit java: add more memory
rem ------------------------------------------------------------------------
java com.asetune.utils.JavaBitness
set JavaBitness=%ERRORLEVEL%
rem ---echo Java Bitness: %JavaBitness%

set JVM_MEMORY_PARAMS=%JVM_MEMORY_PARAMS_32%
set JVM_GC_PARAMS=%JVM_GC_PARAMS_32%
if %JavaBitness% == 64 (
	set JVM_MEMORY_PARAMS=%JVM_MEMORY_PARAMS_64%
	set JVM_GC_PARAMS=%JVM_GC_PARAMS_64%
	echo NOTE: Java is a 64 bit, %APP_NAME% will be allowed to use more memory
)
echo JVM_MEMORY_PARAMS=%JVM_MEMORY_PARAMS%
echo JVM_GC_PARAMS=%JVM_GC_PARAMS%



rem ------------------------------------------------------------------------
rem --- SET JVM Debugging parameters, so we can attach to it
rem ------------------------------------------------------------------------
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dcom.sun.management.jmxremote=true
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Djava.rmi.server.hostname=192.168.0.198 
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dcom.sun.management.jmxremote.port=5656
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dcom.sun.management.jmxremote.authenticate=false
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dcom.sun.management.jmxremote.ssl=false
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dcom.sun.management.jmxremote.local.only=false
rem set JVM_DEBUG_PROPS=%JVM_DEBUG_PROPS% -Dsun.io.serialization.extendedDebugInfo=true



rem ------------------------------------------------------------------------
rem --- Some extra JVM PARAMS, out of memory
rem ------------------------------------------------------------------------
set JVM_OOM_PARAMS=-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%DBXTUNE_SAVE_DIR%"



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem --- pushd \\192.168.0.130\xxx\yyy     if it's a UNC path like the example, pushd will map a network drive, which will be unmounted at popd
rem ------------------------------------------------------------------------
pushd %DBXTUNE_HOME%
rem echo CLASSPATH=%CLASSPATH%
:restart_dbxtune

echo .
echo -----------------------------------------------------------------------
echo Starting %JAVA_START_CLASS% %JAVA_START_PARAMS% %CMDLINE_ARGS%
echo -----------------------------------------------------------------------
echo java %JVM_MEMORY_PARAMS% %JVM_GC_PARAMS% %JVM_OOM_PARAMS% %JVM_PARAMS% %JVM_DEBUG_PROPS% -Duser.language=en -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DAPPL_HOME="%APPL_HOME%" -DDBXTUNE_HOME="%DBXTUNE_HOME%" -DDBXTUNE_SAVE_DIR="%DBXTUNE_SAVE_DIR%" %DBXTUNE_SYSTEM_PROPS% %EXTRA% %DEBUG_OPTIONS% %SPLASH% %JAVA_START_CLASS% %JAVA_START_PARAMS% %CMDLINE_ARGS%
echo -----------------------------------------------------------------------
     java %JVM_MEMORY_PARAMS% %JVM_GC_PARAMS% %JVM_OOM_PARAMS% %JVM_PARAMS% %JVM_DEBUG_PROPS% -Duser.language=en -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DAPPL_HOME="%APPL_HOME%" -DDBXTUNE_HOME="%DBXTUNE_HOME%" -DDBXTUNE_SAVE_DIR="%DBXTUNE_SAVE_DIR%" %DBXTUNE_SYSTEM_PROPS% %EXTRA% %DEBUG_OPTIONS% %SPLASH% %JAVA_START_CLASS% %JAVA_START_PARAMS% %CMDLINE_ARGS%

IF %ERRORLEVEL% EQU 8 GOTO restart_dbxtune

IF %ERRORLEVEL% NEQ 0 GOTO unexpected_error
goto exit_dbxtune



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_dbxtunehome
echo -----------------------------------------------------------------------
echo Error: no DBXTUNE_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the DBXTUNE_HOME variable to the place where you installed %APP_NAME%.
echo -----------------------------------------------------------------------
goto exit_dbxtune

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the DBXTUNE_JAVA_HOME or JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_dbxtune

:no_java
echo -----------------------------------------------------------------------
echo Error: no JAVA is installed.
echo -----------------------------------------------------------------------
echo Must set the DBXTUNE_JAVA_HOME or JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_dbxtune

:to_low_java_version
echo -----------------------------------------------------------------------
echo Error: Use a higher java version.
echo -----------------------------------------------------------------------
echo The java installation can be pointed out using the variable JAVA_HOME
echo Current DBXTUNE_JAVA_HOME variable is set to %DBXTUNE_JAVA_HOME%
echo Current JAVA_HOME         variable is set to %JAVA_HOME%
echo -----------------------------------------------------------------------
goto exit_dbxtune

:unexpected_error
echo .
echo -----------------------------------------------------------------------
echo Unexpected Error: Return code from java was NOT 0
echo -----------------------------------------------------------------------
echo If you have problems to start the tool, please email me the above output
echo And I will make sure to solve the issue for you!
echo -----------------------------------------------------------------------
echo Mail to: goran_schwarz@hotmail.com
echo Subject: %APP_NAME% starting problem
echo -----------------------------------------------------------------------
echo .
goto exit_dbxtune

:no_sybase
echo -----------------------------------------------------------------------
echo Error: no SYBASE environment variable.
echo -----------------------------------------------------------------------
echo 1: Set the env variable SYBASE to where sybase software is installed
echo 2: If you do not have sybase software on this machine, 'set SYBASE=c:\sybase' or any directory
echo    The SYBASE variable is just a pointer where to find the sql.ini file.
echo .
echo If you have a sql.ini file somewhere, %APP_NAME% looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_dbxtune

:exit_dbxtune
popd

rem ------------------------------------------------------------------------
rem --- If we should leave the CMD prompt open or close it on exit 
rem ------------------------------------------------------------------------
IF NOT "%DBXTUNE_PAUSE_ON_EXIT%"=="" (
	pause
)

endlocal
