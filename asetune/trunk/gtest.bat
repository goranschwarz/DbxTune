@echo off
setlocal

set title=gtest-console
title %title%

set MAIN_CLASS=com.asetune.tools.sqlw.QueryWindow
set MAIN_CLASS=com.asetune.utils.JdbcDriverHelper

rem ------------------------------------------------------------------------
rem --- set SQLW_HOME to current directory if NOT already set
rem ------------------------------------------------------------------------
IF "%SQLW_HOME%"=="" set SQLW_HOME=%~dp0

rem --- IF "%SYBASE%"=="" set SYBASE=c:\sybase



rem ------------------------------------------------------------------------
rem --- remove last '\' char from: SQLW_HOME and SYBASE
rem --- SQLW_HOME exists every time, while SYBASE is NOT mandatory
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
IF %SQLW_HOME:~-1%==\ SET SQLW_HOME=%SQLW_HOME:~0,-1%

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
set SQLW_SAVE_DIR=%SQLW_HOME%\data

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

set SPLASH=-splash:lib/sqlw_splash.jpg



rem ------------------------------------------------------------------------
rem --- if environment is not properly set, do something about it
rem --- this might mean goto an exit point
rem ------------------------------------------------------------------------
rem IF NOT DEFINED SYBASE GOTO no_sybase
IF NOT DEFINED SQLW_HOME GOTO no_asetunehome
rem IF NOT DEFINED JAVA_HOME GOTO no_javahome



rem ------------------------------------------------------------------------
rem --- setup the CLASSPATH
rem ------------------------------------------------------------------------
set classpath=%SQLW_HOME%\classes
set classpath=%classpath%;%SQLW_HOME%\lib\asetune.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jconn4.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jconn3.jar
rem set classpath=%classpath%;%SQLW_HOME%\lib\jtds-1.2.7.jar
set classpath=%classpath%;%SQLW_HOME%\lib\dsparser.jar
set classpath=%classpath%;%SQLW_HOME%\lib\log4j-1.2.17.jar
set classpath=%classpath%;%SQLW_HOME%\lib\h2-1.3.169.jar
set classpath=%classpath%;%SQLW_HOME%\lib\wizard.jar
set classpath=%classpath%;%SQLW_HOME%\lib\miglayout-swing-4.2.jar
set classpath=%classpath%;%SQLW_HOME%\lib\miglayout-core-4.2.jar
set classpath=%classpath%;%SQLW_HOME%\lib\swingx-all-1.6.4.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jchart2d-3.2.2.jar
set classpath=%classpath%;%SQLW_HOME%\lib\planviewer.jar
set classpath=%classpath%;%SQLW_HOME%\lib\commons-cli-1.2.jar
set classpath=%classpath%;%SQLW_HOME%\lib\proxy-vole_20121203.jar
set classpath=%classpath%;%SQLW_HOME%\lib\ganymed-ssh2-build251beta1.jar
set classpath=%classpath%;%SQLW_HOME%\lib\rsyntaxtextarea.jar
set classpath=%classpath%;%SQLW_HOME%\lib\autocomplete.jar
set classpath=%classpath%;%SQLW_HOME%\lib\rstaui.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jcommon-1.0.17.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jfreechart-1.0.14.jar

rem set classpath=%classpath%;%SQLW_HOME%\lib\SybaseParser_0.5.1.121_alpha.jar
set classpath=%classpath%;%SQLW_HOME%\lib\ngdbc.jar
set classpath=%classpath%;%SQLW_HOME%\lib\gsp.jar
set classpath=%classpath%;%SQLW_HOME%\lib\jsqlparser.jar
set classpath=%classpath%;%SQLW_HOME%\lib\antlr-4.0-complete.jar

rem --- echo %CLASSPATH%



rem ------------------------------------------------------------------------
rem --- set PATH, just add JAVA_HOME at the start
rem ------------------------------------------------------------------------
set PATH=%JAVA_HOME%\bin;%PATH%



rem ------------------------------------------------------------------------
rem --- CHECK current Java Version
rem ------------------------------------------------------------------------
java com.asetune.utils.JavaVersion 6
IF %ERRORLEVEL% NEQ 0 GOTO to_low_java_version



rem ------------------------------------------------------------------------
rem --- START: just call java, it should have been added to the path priviously
rem ------------------------------------------------------------------------
cd %SQLW_HOME%
REM echo %CLASSPATH%

java  %JVM_PARAMS% -Dsybase.home="%SYBASE%" -DSYBASE="%SYBASE%" -DSQLW_HOME="%SQLW_HOME%" -DSQLW_SAVE_DIR="%SQLW_SAVE_DIR%" %EXTRA% %DEBUG_OPTIONS% %SPLASH% %MAIN_CLASS% %*

IF %ERRORLEVEL% NEQ 0 GOTO unexpected_error
goto exit_sqlw



rem ------------------------------------------------------------------------
rem --- Various exit points
rem ------------------------------------------------------------------------

:no_sqlwhome
echo -----------------------------------------------------------------------
echo Error: no SQLW_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the SQLW_HOME variable to the place where you installed SqlWindow.
echo -----------------------------------------------------------------------
goto exit_sqlw

:no_javahome
echo -----------------------------------------------------------------------
echo Error: no JAVA_HOME environment variable.
echo -----------------------------------------------------------------------
echo Must set the JAVA_HOME variable to the place where JDK or JRE is installed.
echo -----------------------------------------------------------------------
goto exit_sqlw

:to_low_java_version
echo -----------------------------------------------------------------------
echo Error: Use a higher java version.
echo -----------------------------------------------------------------------
echo The java installation can be pointed out using the variable JAVA_HOME
echo Current JAVA_HOME variable is set to %JAVA_HOME%
echo -----------------------------------------------------------------------
goto exit_sqlw

:unexpected_error
echo .
echo -----------------------------------------------------------------------
echo Unexpected Error: Return code from java was NOT 0
echo -----------------------------------------------------------------------
echo If you have problems to start the tool, please email me the above output
echo And I will make sure to solve the issue for you!
echo -----------------------------------------------------------------------
echo Mail to: goran_schwarz@hotmail.com
echo Subject: SqlW starting problem
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
echo If you have a sql.ini file somewhere, SqlWindow looks for it under %%SYBASE%%\ini\sql.ini.
echo If you do not have a sql.ini file, just set SYBASE=c:\whatever
echo    and then you can to specify to what host and port when connecting to the ASE server.
echo -----------------------------------------------------------------------
goto exit_sqlw

:exit_sqlw
pause
endlocal
