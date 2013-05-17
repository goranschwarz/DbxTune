@echo off
title PerfDemo-console
setlocal

rem ------------------------------------------------------------------------
rem --- set PERF_DEMO_HOME to current directory if NOT already set
rem ------------------------------------------------------------------------
IF "%PERF_DEMO_HOME%"=="" set PERF_DEMO_HOME=%~dp0

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase



rem ------------------------------------------------------------------------
rem --- remove last '\' char
rem ------------------------------------------------------------------------
:stripHome
if not _%PERF_DEMO_HOME:~-1%==_\ goto afterStripHome
set PERF_DEMO_HOME=%PERF_DEMO_HOME:~0,-1%
goto stripHome
:afterStripHome



rem ------------------------------------------------------------------------
rem --- set some default environment variables
rem ------------------------------------------------------------------------
set PERF_DEMO_SAVE_DIR=%PERF_DEMO_HOME%\data

rem set JAVA_HOME=%SYBASE_JRE%
rem set JAVA_HOME=%SYBASE%\shared-1_0\JRE-1_4
rem set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_07



rem ------------------------------------------------------------------------
rem --- set JVM parameters   and   DEBUG stuff
rem ------------------------------------------------------------------------
set JVM_PARAMS=-Xmx700m
rem --- set JVM_PARAMS=%JVM_PARAMS% -Dhttp.proxyHost=www-proxy.ericsson.se -Dhttp.proxyPort=8080
rem --- set JVM_PARAMS=%JVM_PARAMS% -Dcom.sun.management.jmxremote
rem --- set JVM_PARAMS=%JVM_PARAMS% -Djava.net.useSystemProxies=true

set EXTRA=%NOGUI%
rem --- set DEBUG_OPTIONS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=2323,server=y,suspend=n
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
rem --- set DEBUG_OPTIONS=-agentlib:hprof=cpu=times
set DEBUG_OPTIONS=



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
IF "%SYBASE%"=="" GOTO no_sybase
IF "%PERF_DEMO_HOME%"=="" GOTO no_perfdemohome
rem --- IF "%JAVA_HOME%"=="" GOTO no_javahome



rem ------------------------------------------------------------------------
rem --- setup the CLASSPATH
rem ------------------------------------------------------------------------
set classpath=%PERF_DEMO_HOME%\classes
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\asetune.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\jconn3.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\jconn4.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\jtds-1.2.7.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\dsparser.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\log4j-1.2.17.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\h2-1.3.169.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\wizard.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\miglayout-swing-4.2.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\miglayout-core-4.2.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\swingx-all-1.6.4.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\jchart2d-3.2.2.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\planviewer.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\commons-cli-1.2.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\proxy-vole_20121203.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\ganymed-ssh2-build251beta1.jar
set classpath=%classpath%;%PERF_DEMO_HOME%\lib\antlr-4.0-complete.jar

rem --- echo %CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%JAVA_HOME%\bin;%PATH%



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem ------------------------------------------------------------------------
cd %PERF_DEMO_HOME%
REM echo %CLASSPATH%

java  %JVM_PARAMS% -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DPERF_DEMO_HOME="%PERF_DEMO_HOME%" -DPERF_DEMO_SAVE_DIR="%PERF_DEMO_SAVE_DIR%" %EXTRA% %DEBUG_OPTIONS% com.asetune.perftest.PerfDemo %*

goto exit_perfdemo



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_perfdemohome
echo -----------------------------------------------------------------------
echo Error: no PERF_DEMO_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the PERF_DEMO_HOME variable to the place where you installed perfdemo.
echo -----------------------------------------------------------------------
goto exit_perfdemo

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_perfdemo

:no_sybase
echo -----------------------------------------------------------------------
echo Error: no SYBASE environment variable.
echo -----------------------------------------------------------------------
echo 1: Set the env variable SYBASE to where sybase software is installed
echo 2: If you do not have sybase software on this machine, 'set SYBASE=c:\sybase' or any directory
echo    The SYBASE variable is just a pointer where to find the sql.ini file.
echo .
echo If you have a sql.ini file somewhere, perfdemo looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_perfdemo

:exit_perfdemo
rem pause
endlocal
