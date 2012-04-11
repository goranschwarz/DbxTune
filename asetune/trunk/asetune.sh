#! /bin/bash
#=======================================================================
# NOTE: This one needs more work...
#-----------------------------------------------------------------------

#SYBASE=${SYBASE:-/opt/sybase}
ASETUNE_HOME=${ASETUNE_HOME:-./}

export ASETUNE_SAVE_DIR=$ASETUNE_HOME/data
export JCONNECT_HOME=$SYBASE/jConnect-6_0

export JAVA_HOME=${JAVA_HOME:-$SYBASE_JRE6}

export JVM_PARAMS=-Xmx1024m
#export JVM_PARAMS=$JVM_PARAMS -Dhttp.proxyHost=www-proxy.ericsson.se -Dhttp.proxyPort=8080
#export JVM_PARAMS=$JVM_PARAMS -Dcom.sun.management.jmxremote
#export JVM_PARAMS=$JVM_PARAMS -Djava.net.useSystemProxies=true

export EXTRA=
#export DEBUG_OPTION=S-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=2323,server=y,suspend=n
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=times
export DEBUG_OPTIONS=

export SPLASH=-splash:lib/asetune_splash.jpg

export CLASSPATH=$ASETUNE_HOME/classes
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/asetune.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/jconn3.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/jconn4.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/dsparser.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/log4j-1.2.16.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/h2-1.3.166.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/wizard.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/miglayout-swing-4.2.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/miglayout-core-4.2.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/swingx-core-1.6.2-2.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/jchart2d-3.2.2.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/planviewer.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/commons-cli-1.2.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/proxy-vole_20111102.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/ganymed-ssh2-build251beta1.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/rsyntaxtextarea.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/autocomplete.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/jcommon-1.0.16.jar
export CLASSPATH=$CLASSPATH:$ASETUNE_HOME/lib/jfreechart-1.0.13.jar


export PATH=$JAVA_HOME/bin:$PATH
cd $ASETUNE_HOME

echo SYBASE=$SYBASE
echo ASETUNE_HOME=$ASETUNE_HOME
echo ASETUNE_SAVE_DIR=$ASETUNE_SAVE_DIR
echo CLASSPATH=$CLASSPATH
echo JAVA_HOME=$JAVA_HOME

#------------------------------------------------------------------------
#--- CHECK current Java Version
#------------------------------------------------------------------------
java com.asetune.utils.JavaVersion 6
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




java -cp $CLASSPATH $JVM_PARAMS -Dsybase.home=$SYBASE -DSYBASE=$SYBASE -DASETUNE_HOME=$ASETUNE_HOME -DASETUNE_SAVE_DIR=$ASETUNE_SAVE_DIR $EXTRA $DEBUG_OPTIONS $SPLASH com.asetune.AseTune $@


