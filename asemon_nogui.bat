@echo off
setlocal

set SYBASE=c:\sybase

set ASEMON_HOME=%~dp0

:stripHome
if not _%ASEMON_HOME:~-1%==_\ goto checkEnv
set ASEMON_HOME=%ASEMON_HOME:~0,-1%
goto stripHome

:checkEnv

set ASEMON_SAVE_DIR=%ASEMON_HOME%\data

set JCONNECT_HOME=%SYBASE%\jConnect-5_5

rem set JAVA_HOME=%SYBASE_JRE%
set JAVA_HOME=%SYBASE%\shared-1_0\JRE-1_4
set JAVA_HOME=C:\java\jdk1.5.0_06

set JVM_PARAMS=-Xmx500m -Dcom.sun.management.jmxremote
rem --- set JVM_PARAMS=-Xmx500m -Dcom.sun.management.jmxremote


IF "%SYBASE%"=="" GOTO no_sybase

IF "%JAVA_HOME%"=="" GOTO no_javahome

IF "%ASEMON_HOME%"=="" GOTO no_asemonhome

IF "%JCONNECT_HOME%"=="" GOTO no_jconnecthome


set NOGUI=-Dasemon.gui=false -Dasemon.store.config=offline.props
set EXTRA=%NOGUI%

set PATH=%JAVA_HOME%\bin;%PATH%

set classpath=%ASEMON_HOME%\classes
set classpath=%classpath%;%ASEMON_HOME%\lib\asemon.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jconn3.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\dsparser.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jcommon-1.0.14.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\log4j-1.2.15.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jfreechart-1.0.11.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\h2.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\wizard.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\miglayout-3.6.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\swingx-0.9.5.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\jchart2d-3.1.0.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\planviewer.jar
set classpath=%classpath%;%ASEMON_HOME%\lib\oncrpc.jar

cd %ASEMON_HOME%
java  %JVM_PARAMS% -Dsybase.home=%SYBASE% -DSYBASE=%SYBASE% -DASEMON_HOME=%ASEMON_HOME% -DASEMON_SAVE_DIR=%ASEMON_SAVE_DIR% %EXTRA% asemon.Asemon
goto exit_asemon

:no_asemonhome
echo "Error..."
echo "Must set the ASEMON_HOME variable to the place where you installed asemon."
goto exit_asemon

:no_javahome
echo "Error..."
echo "Must set the JAVA_HOME variable to the place where JDK or JRE is installed."
goto exit_asemon

:no_jconnecthome
echo "Error..."
echo "Must set the JCONNECT_HOME variable to the place where jConnect-5_5 is installed."
goto exit_asemon


:no_sybase
echo "Error..."
echo "Must set the SYBASE variable."
goto exit_asemon

:exit_asemon
pause
endlocal

