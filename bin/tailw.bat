@echo off
setlocal

set title=TailWindow-console
title %title%

rem ------------------------------------------------------------------------
rem --- set TAILW_HOME to current directory if NOT already set
rem ------------------------------------------------------------------------
IF "%TAILW_HOME%"=="" set TAILW_HOME=%~dp0

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase



rem ------------------------------------------------------------------------
rem --- remove last '\' char from: TAILW_HOME and SYBASE
rem --- TAILW_HOME exists every time, while SYBASE is NOT mandatory
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
IF %TAILW_HOME:~-1%==\ SET TAILW_HOME=%TAILW_HOME:~0,-1%

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
set APPL_HOME=%TAILW_HOME%
set TAILW_SAVE_DIR=%TAILW_HOME%\data

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

set SPLASH=-splash:lib/tailw_splash.jpg



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
rem IF NOT DEFINED SYBASE GOTO no_sybase
IF NOT DEFINED TAILW_HOME GOTO no_asetunehome
rem IF NOT DEFINED JAVA_HOME GOTO no_javahome



rem ------------------------------------------------------------------------
rem --- setup the CLASSPATH
rem ------------------------------------------------------------------------
set classpath=%TAILW_HOME%\classes
set classpath=%classpath%;%TAILW_HOME%\lib\asetune.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jconn42.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jconn4.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jconn3.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jtds-1.3.1.jar
set classpath=%classpath%;%TAILW_HOME%\lib\dsparser.jar
set classpath=%classpath%;%TAILW_HOME%\lib\log4j-1.2.17.jar
set classpath=%classpath%;%TAILW_HOME%\lib\h2-SNAPSHOT.jar
set classpath=%classpath%;%TAILW_HOME%\lib\h2-1.4.200.jar
set classpath=%classpath%;%TAILW_HOME%\lib\wizard.jar
set classpath=%classpath%;%TAILW_HOME%\lib\miglayout-swing-5.2.jar
set classpath=%classpath%;%TAILW_HOME%\lib\miglayout-core-5.2.jar
set classpath=%classpath%;%TAILW_HOME%\lib\swingx-all-1.6.5-1.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jchart2d-3.2.2.jar
set classpath=%classpath%;%TAILW_HOME%\lib\planviewer.jar
set classpath=%classpath%;%TAILW_HOME%\lib\commons-text-1.9.jar
set classpath=%classpath%;%TAILW_HOME%\lib\commons-lang3-3.7.jar
set classpath=%classpath%;%TAILW_HOME%\lib\commons-io-2.6.jar
set classpath=%classpath%;%TAILW_HOME%\lib\commons-csv-1.5.jar
set classpath=%classpath%;%TAILW_HOME%\lib\commons-cli-1.4.jar
set classpath=%classpath%;%TAILW_HOME%\lib\proxy-vole_20131209.jar
set classpath=%classpath%;%TAILW_HOME%\lib\ganymed-ssh2-263.jar
set classpath=%classpath%;%TAILW_HOME%\lib\rsyntaxtextarea.jar
set classpath=%classpath%;%TAILW_HOME%\lib\autocomplete.jar
set classpath=%classpath%;%TAILW_HOME%\lib\rstaui.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jcommon-1.0.21.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jfreechart-1.5.1.jar
set classpath=%classpath%;%TAILW_HOME%\lib\juniversalchardet-2.3.0.jar
set classpath=%classpath%;%TAILW_HOME%\lib\DDLGen.jar

rem set classpath=%classpath%;%TAILW_HOME%\lib\SybaseParser_0.5.1.121_alpha.jar
set classpath=%classpath%;%TAILW_HOME%\lib\ngdbc.jar
set classpath=%classpath%;%TAILW_HOME%\lib\gsp.jar
set classpath=%classpath%;%TAILW_HOME%\lib\jsqlparser-4.3.jar
set classpath=%classpath%;%TAILW_HOME%\lib\antlr-4.0-complete.jar

set classpath=%classpath%;%USERPROFILE%\.asetune\jdbc_drivers\*
set classpath=%classpath%;%EXTRA_JDBC_DRIVERS%

rem --- echo %CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%TAILW_JAVA_HOME%\bin;%DBXTUNE_JAVA_HOME%\bin;%JAVA_HOME%\bin;%PATH%



rem ------------------------------------------------------------------------
rem --- Just for informational purposes, print out the Java Version we are using
rem ------------------------------------------------------------------------
java -version



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
java com.asetune.utils.JavaVersion 7
IF %ERRORLEVEL% NEQ 0 GOTO to_low_java_version



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem --- pushd \\192.168.0.130\xxx\yyy     if it's a UNC path like the example, pushd will map a network drive, which will be unmounted at popd
rem ------------------------------------------------------------------------
pushd %TAILW_HOME%
REM echo %CLASSPATH%

java  %JVM_PARAMS% -Duser.language=en -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DAPPL_HOME="%TAILW_HOME%" -DTAILW_HOME="%TAILW_HOME%" -DTAILW_SAVE_DIR="%TAILW_SAVE_DIR%" %EXTRA% %DEBUG_OPTIONS% %SPLASH% com.asetune.tools.tailw.LogTailWindow %*

IF %ERRORLEVEL% NEQ 0 GOTO unexpected_error
goto exit_tailw



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_tailwhome
echo -----------------------------------------------------------------------
echo Error: no TAILW_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the TAILW_HOME variable to the place where you installed TailWindow.
echo -----------------------------------------------------------------------
goto exit_tailw

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_tailw

:to_low_java_version
echo -----------------------------------------------------------------------
echo Error: Use a higher java version.
echo -----------------------------------------------------------------------
echo The java installation can be pointed out using the variable JAVA_HOME
echo Current TAILW_JAVA_HOME   variable is set to %TAILW_JAVA_HOME%
echo Current DBXTUNE_JAVA_HOME variable is set to %DBXTUNE_JAVA_HOME%
echo Current JAVA_HOME         variable is set to %JAVA_HOME%
echo -----------------------------------------------------------------------
goto exit_tailw

:unexpected_error
echo .
echo -----------------------------------------------------------------------
echo Unexpected Error: Return code from java was NOT 0
echo -----------------------------------------------------------------------
echo If you have problems to start the tool, please email me the above output
echo And I will make sure to solve the issue for you!
echo -----------------------------------------------------------------------
echo Mail to: goran_schwarz@hotmail.com
echo Subject: TailW starting problem
echo -----------------------------------------------------------------------
echo .
goto exit_asetune

:no_sybase
echo -----------------------------------------------------------------------
echo Error: no SYBASE environment variable.
echo -----------------------------------------------------------------------
echo 1: Set the env variable SYBASE to where sybase software is installed
echo 2: If you do not have sybase software on this machine, 'set SYBASE=c:\sybase' or any directory
echo    The SYBASE variable is just a pointer where to find the sql.ini file.
echo .
echo If you have a sql.ini file somewhere, TailWindow looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_tailw

:exit_tailw
popd

rem pause
endlocal
