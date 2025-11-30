set PATH=%JAVA_HOME%\bin;%PATH%
set CLASSPATH=..\..\..\..\..\..\lib\antlr-4.0-complete.jar;%CLASSPATH%

java org.antlr.v4.Tool -visitor SybaseAse.g4

rem javac *.java