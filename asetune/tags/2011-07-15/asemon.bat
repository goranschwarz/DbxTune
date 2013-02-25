@echo off
title AseMon-console
setlocal

rem ------------------------------------------------------------------------
rem --- set ASEMON_HOME to current directory if NOT already set
rem ------------------------------------------------------------------------
IF "%ASEMON_HOME%"=="" set ASEMON_HOME=%~dp0

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase



rem ------------------------------------------------------------------------
rem --- remove last '\' char
rem ------------------------------------------------------------------------
:stripHome
if not _%ASEMON_HOME:~-1%==_\ goto afterStripHome
set ASEMON_HOME=%ASEMON_HOME:~0,-1%
goto stripHome
:afterStripHome



rem ------------------------------------------------------------------------
rem --- set some default environment variables
rem ------------------------------------------------------------------------
set ASEMON_SAVE_DIR=%ASEMON_HOME%\data

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

set SPLASH=-splash:lib/asemon_splash.jpg



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
IF "%SYBASE%"=="" GOTO no_sybase
IF "%ASEMON_HOME%"=="" GOTO no_asemonhome
rem --- IF "%JAVA_HOME%"=="" GOTO no_javahome



rem ------------------------------------------------------------------------
rem --- setup the CLASSPATH
rem ------------------------------------------------------------------------
set classpath=%ASEMON_HOME%\classes
set classpath=%classpath%;%ASEMON_HOME%\lib\asemon.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jconn3.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jconn4.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\dsparser.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\log4j-1.2.16.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\h2-1.3.155.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\wizard.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\miglayout-3.7.4-swing.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\swingx-core-1.6.2.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jchart2d-3.2.1.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\planviewer.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\commons-cli-1.2.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\proxy-vole_20110515.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\ganymed-ssh2-build251beta1.jar

rem set classpath=%classpath%;%ASEMON_HOME%\lib\h2.jar
rem set classpath=%classpath%;%ASEMON_HOME%\lib\miglayout-3.6.jar
rem set classpath=%classpath%;%ASEMON_HOME%\lib\jchart2d-3.2.0.jar
rem set classpath=%classpath%;%ASEMON_HOME%\lib\proxy-vole_20100914.jar

rem --- echo %CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%JAVA_HOME%\bin;%PATH%



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
java asemon.utils.JavaVersion 6
IF %ERRORLEVEL% NEQ 0 GOTO to_low_java_version



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem ------------------------------------------------------------------------
cd %ASEMON_HOME%
REM echo %CLASSPATH%

java  %JVM_PARAMS% -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DASEMON_HOME="%ASEMON_HOME%" -DASEMON_SAVE_DIR="%ASEMON_SAVE_DIR%" %EXTRA% %DEBUG_OPTIONS% %SPLASH% asemon.Asemon %*

goto exit_asemon



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_asemonhome
echo -----------------------------------------------------------------------
echo Error: no ASEMON_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the ASEMON_HOME variable to the place where you installed asemon.
echo -----------------------------------------------------------------------
goto exit_asemon

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_asemon

:to_low_java_version
echo -----------------------------------------------------------------------
echo Error: Use a higher java version.
echo -----------------------------------------------------------------------
echo The java installation can be pointed out using the variable JAVA_HOME
echo Current JAVA_HOME variable is set to %JAVA_HOME%
echo -----------------------------------------------------------------------
goto exit_asemon

:no_sybase
echo -----------------------------------------------------------------------
echo Error: no SYBASE environment variable.
echo -----------------------------------------------------------------------
echo 1: Set the env variable SYBASE to where sybase software is installed
echo 2: If you do not have sybase software on this machine, 'set SYBASE=c:\sybase' or any directory
echo    The SYBASE variable is just a pointer where to find the sql.ini file.
echo .
echo If you have a sql.ini file somewhere, asemon looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_asemon

:exit_asemon
pause
endlocal