@echo off
setlocal
rem mode con:cols=800 lines=3000

set title=OracleTune-console
title %title%

rem ------------------------------------------------------------------------
rem --- set ORACLETUNE_HOME to current directory if NOT already set
rem ------------------------------------------------------------------------
IF "%ORACLETUNE_HOME%"=="" set ORACLETUNE_HOME=%~dp0

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase



rem ------------------------------------------------------------------------
rem --- remove last '\' char from: ORACLETUNE_HOME and SYBASE
rem --- ORACLETUNE_HOME exists every time, while SYBASE is NOT mandatory
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
IF %ORACLETUNE_HOME:~-1%==\ SET ORACLETUNE_HOME=%ORACLETUNE_HOME:~0,-1%

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
set APPL_HOME=%ORACLETUNE_HOME%
set ORACLETUNE_SAVE_DIR=%ORACLETUNE_HOME%\data

rem set JAVA_HOME=%SYBASE_JRE%
rem set JAVA_HOME=%SYBASE%\shared-1_0\JRE-1_4
rem set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_07



rem ------------------------------------------------------------------------
rem --- set JVM MEMORY parameters
rem ------------------------------------------------------------------------
echo NOTE: Set/Change JVM Memory parameters by setting Environment variable: ORACLETUNE_JVM_MEMORY_PARAMS 

set JVM_MEMORY_PARAMS_32=-Xmx1024m -Xms64m
set JVM_MEMORY_PARAMS_64=-Xmx2048m -Xms64m

IF DEFINED ORACLETUNE_JVM_MEMORY_PARAMS set JVM_MEMORY_PARAMS_32=%ORACLETUNE_JVM_MEMORY_PARAMS%
IF DEFINED ORACLETUNE_JVM_MEMORY_PARAMS set JVM_MEMORY_PARAMS_64=%ORACLETUNE_JVM_MEMORY_PARAMS%


rem ------------------------------------------------------------------------
rem --- set JVM GARBAGE COLLECTION parameters
rem ------------------------------------------------------------------------
set JVM_GC_PARAMS_32=
set JVM_GC_PARAMS_64=

IF DEFINED ORACLETUNE_JVM_GC_PARAMS set JVM_GC_PARAMS_32=%ORACLETUNE_JVM_GC_PARAMS%
IF DEFINED ORACLETUNE_JVM_GC_PARAMS set JVM_GC_PARAMS_64=%ORACLETUNE_JVM_GC_PARAMS%


rem ------------------------------------------------------------------------
rem --- set OTHER JVM parameters   and   DEBUG stuff
rem ------------------------------------------------------------------------
rem --- set JVM_PARAMS=%JVM_PARAMS% -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
rem --- set JVM_PARAMS=%JVM_PARAMS% -Xrunhprof:cpu=samples,depth=16
rem --- set JVM_PARAMS=%JVM_PARAMS% -Dhttp.proxyHost=www-proxy.domain.com -Dhttp.proxyPort=8080
rem --- set JVM_PARAMS=%JVM_PARAMS% -Dcom.sun.management.jmxremote
rem --- set JVM_PARAMS=%JVM_PARAMS% -Djava.net.useSystemProxies=true

set EXTRA=%NOGUI%
rem --- set DEBUG_OPTIONS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=2323,server=y,suspend=n
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=times
set DEBUG_OPTIONS=

set SPLASH=-splash:lib/oracletune_splash.jpg



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
rem IF NOT DEFINED SYBASE GOTO no_sybase
IF NOT DEFINED ORACLETUNE_HOME GOTO no_oracletunehome
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
set classpath=%ORACLETUNE_HOME%\classes
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\asetune.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jconn4.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jconn3.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jtds-1.2.7.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\ngdbc.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\dsparser.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\log4j-1.2.17.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\h2-1.3.176.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\wizard.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\miglayout-swing-4.2.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\miglayout-core-4.2.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\swingx-all-1.6.5-1.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jchart2d-3.2.2.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\planviewer.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\commons-cli-1.2.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\proxy-vole_20131209.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\ganymed-ssh2-build251beta1.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\rsyntaxtextarea.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\autocomplete.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\rstaui.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jcommon-1.0.21.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\jfreechart-1.0.17.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\antlr-4.0-complete.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\juniversalchardet-1.0.3.jar
set classpath=%classpath%;%ORACLETUNE_HOME%\lib\DDLGen.jar

set classpath=%classpath%;%USERPROFILE%\.asetune\jdbc_drivers\*
set classpath=%classpath%;%EXTRA_JDBC_DRIVERS%

rem --- echo %CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%ORACLETUNE_JAVA_HOME%\bin;%JAVA_HOME%\bin;%PATH%



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
java com.asetune.utils.JavaVersion 6
IF %ERRORLEVEL% NEQ 0 GOTO to_low_java_version



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
	echo NOTE: Java is a 64 bit, OracleTune will be allowed to use more memory
)
echo JVM_MEMORY_PARAMS=%JVM_MEMORY_PARAMS%
echo JVM_GC_PARAMS=%JVM_GC_PARAMS%



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem --- pushd \\192.168.0.130\xxx\yyy     if it's a UNC path like the example, pushd will map a network drive, which will be unmounted at popd
rem ------------------------------------------------------------------------
pushd %ORACLETUNE_HOME%
REM echo %CLASSPATH%

java %JVM_MEMORY_PARAMS% %JVM_GC_PARAMS% %JVM_PARAMS% -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DAPPL_HOME="%SQLW_HOME%" -DORACLETUNE_HOME="%ORACLETUNE_HOME%" -DORACLETUNE_SAVE_DIR="%ORACLETUNE_SAVE_DIR%" %EXTRA% %DEBUG_OPTIONS% %SPLASH% com.asetune.OracleTune %*

IF %ERRORLEVEL% NEQ 0 GOTO unexpected_error
goto exit_oracletune



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_oracletunehome
echo -----------------------------------------------------------------------
echo Error: no ORACLETUNE_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the ORACLETUNE_HOME variable to the place where you installed OracleTune.
echo -----------------------------------------------------------------------
goto exit_oracletune

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_oracletune

:to_low_java_version
echo -----------------------------------------------------------------------
echo Error: Use a higher java version.
echo -----------------------------------------------------------------------
echo The java installation can be pointed out using the variable JAVA_HOME
echo Current ORACLETUNE_JAVA_HOME variable is set to %ORACLETUNE_JAVA_HOME%
echo Current JAVA_HOME            variable is set to %JAVA_HOME%
echo -----------------------------------------------------------------------
goto exit_oracletune

:unexpected_error
echo .
echo -----------------------------------------------------------------------
echo Unexpected Error: Return code from java was NOT 0
echo -----------------------------------------------------------------------
echo If you have problems to start the tool, please email me the above output
echo And I will make sure to solve the issue for you!
echo -----------------------------------------------------------------------
echo Mail to: goran_schwarz@hotmail.com
echo Subject: OracleTune starting problem
echo -----------------------------------------------------------------------
echo .
goto exit_oracletune

:no_sybase
echo -----------------------------------------------------------------------
echo Error: no SYBASE environment variable.
echo -----------------------------------------------------------------------
echo 1: Set the env variable SYBASE to where sybase software is installed
echo 2: If you do not have sybase software on this machine, 'set SYBASE=c:\sybase' or any directory
echo    The SYBASE variable is just a pointer where to find the sql.ini file.
echo .
echo If you have a sql.ini file somewhere, OracleTune looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_oracletune

:exit_oracletune
popd

pause
endlocal
