#! /bin/bash
#=======================================================================
# NOTE: This one needs more work...
#-----------------------------------------------------------------------

SYBASE=${SYBASE:-/opt/sybase}
ASEMON_HOME=${ASEMON_HOME:-./}

export ASEMON_SAVE_DIR=$ASEMON_HOME/data
export JCONNECT_HOME=$SYBASE/jConnect-6_0

export JAVA_HOME=${JAVA_HOME:-$SYBASE_JRE6}

export JVM_PARAMS=-Xmx500m
#export JVM_PARAMS=$JVM_PARAMS -Dhttp.proxyHost=www-proxy.ericsson.se -Dhttp.proxyPort=8080
#export JVM_PARAMS=$JVM_PARAMS -Dcom.sun.management.jmxremote
#export JVM_PARAMS=$JVM_PARAMS -Djava.net.useSystemProxies=true

export EXTRA=
#export DEBUG_OPTION=S-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=2323,server=y,suspend=n
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=times
export DEBUG_OPTIONS=

export SPLASH=-splash:classes/asemon/images/asemon_splash.jpg

export CLASSPATH=$ASEMON_HOME/classes
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/asemon.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/jconn3.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/jconn4.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/dsparser.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/log4j-1.2.16.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/h2.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/wizard.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/miglayout-3.6.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/swingx-core-1.6.2.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/jchart2d-3.2.0.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/planviewer.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/commons-cli-1.2.jar
export CLASSPATH=$CLASSPATH:$ASEMON_HOME/lib/proxy-vole_20100914.jar


export PATH=$JAVA_HOME/bin:$PATH
cd $ASEMON_HOME

echo SYBASE=$SYBASE
echo ASEMON_HOME=$ASEMON_HOME
echo ASEMON_SAVE_DIR=$ASEMON_SAVE_DIR
echo CLASSPATH=$CLASSPATH
echo JAVA_HOME=$JAVA_HOME

#------------------------------------------------------------------------
#--- CHECK current Java Version
#------------------------------------------------------------------------
java asemon.utils.JavaVersion 6
if [ $? -ne 0 ]
then
	echo "-----------------------------------------------------------------------"
	echo "Error: Use a higher java version."
	echo "-----------------------------------------------------------------------"
	echo "The java installation can be pointed out using the variable JAVA_HOME"
	echo "Current JAVA_HOME variable is set to ${JAVA_HOME}"
	echo "-----------------------------------------------------------------------"
	
	exit 1
fi




java -cp $CLASSPATH $JVM_PARAMS -Dsybase.home=$SYBASE -DSYBASE=$SYBASE -DASEMON_HOME=$ASEMON_HOME -DASEMON_SAVE_DIR=$ASEMON_SAVE_DIR $EXTRA $DEBUG_OPTIONS $SPLASH asemon.Asemon


