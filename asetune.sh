#! /bin/bash
#=======================================================================
# NOTE: This one needs more work...
#-----------------------------------------------------------------------
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

shortAppName=asetune
longAppName=AseTune
javaMainClass=com.asetune.AseTune
javaSplashScreen=lib/asetune_splash.jpg

#----------------------------------
#--- XXX_HOME
#----------------------------------
APPL_HOME_propName="DBXTUNE_HOME"

export DBXTUNE_HOME=${DBXTUNE_HOME:-$scriptPath}
export APPL_HOME=${DBXTUNE_HOME}

#----------------------------------
#--- XXX_SAVE_DIR
#----------------------------------
APPL_SAVE_DIR_propName="DBXTUNE_SAVE_DIR"

export DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-$APPL_HOME/data}
export APPL_SAVE_DIR=${DBXTUNE_SAVE_DIR}


#------------------------------------------------------------------------
#--- get JVM Parameters from file:
#------------------------------------------------------------------------
DBXTUNE_JVM_PARAMETER_FILE=${HOME}/.asetune/.dbxtune_jvm_settings.properties
if [ -f ${DBXTUNE_JVM_PARAMETER_FILE} ]
then
	echo "Reading DBXTUNE_JVM_PARAMETER_FILE: ${DBXTUNE_JVM_PARAMETER_FILE}"
	cat ${DBXTUNE_JVM_PARAMETER_FILE}
	. ${DBXTUNE_JVM_PARAMETER_FILE} ## source the file...
else
	echo "The DBXTUNE_JVM_PARAMETER_FILE: ${DBXTUNE_JVM_PARAMETER_FILE} did NOT EXIST"
fi

#----------------------------------
#--- Java
#----------------------------------
export JAVA_HOME=${JAVA_HOME:-$SYBASE_JRE7}

echo "NOTE: Set/Change JVM Memory parameters by setting Environment variable: DBXTUNE_JVM_MEMORY_PARAMS" 

export JVM_MEMORY_PARAMS_32=${DBXTUNE_JVM_MEMORY_PARAMS:-"-Xmx1024m -Xms64m"}
export JVM_MEMORY_PARAMS_64=${DBXTUNE_JVM_MEMORY_PARAMS:-"-Xmx2048m -Xms64m"}

export JVM_GC_PARAMS_32=${DBXTUNE_JVM_GC_PARAMS:-""}
export JVM_GC_PARAMS_64=${DBXTUNE_JVM_GC_PARAMS:-""}

#export JVM_PARAMS=${DBXTUNE_JVM_PARAMS:-"-noverify"}
export JVM_PARAMS=${DBXTUNE_JVM_PARAMS:-"-noverify -XX:-UseGCOverheadLimit"}
## -XX:-UseGCOverheadLimit ## will turn off 'OutOfMemoryError: GC overhead limit exceeded' -- https://stackoverflow.com/questions/1393486/error-java-lang-outofmemoryerror-gc-overhead-limit-exceeded
## which might happen if we collects to many SQLText objects...
## but this is also *dangerous* to disable, since the JVM will eat *much* more CPU when this happens...
##     - The GC throws this exception when too much time is spent in garbage collection for too little return, eg. 98% of CPU time is spent on GC and less than 2% of heap is recovered.
##     - This feature is designed to prevent applications from running for an extended period of time while making little or no progress because the heap is too small.


export EXTRA=
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=times
export DEBUG_OPTIONS=

export SPLASH=-splash:${javaSplashScreen}

export CLASSPATH=${APPL_HOME}/classes
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/asetune.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jtds-1.3.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/dsparser.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-1.2.17.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-1.4.194.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/wizard.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-swing-4.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-core-4.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/swingx-all-1.6.5-1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jchart2d-3.2.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/planviewer.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-lang3-3.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-io-2.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-csv-1.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-cli-1.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-codec-1.10.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/proxy-vole_20131209.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/ganymed-ssh2-build251beta1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rsyntaxtextarea.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/autocomplete.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rstaui.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/language_support.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jcommon-1.0.21.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jfreechart-1.0.17.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/antlr-4.0-complete.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/juniversalchardet-1.0.3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/DDLGen.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/simplemagic-1.6.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsqlparser-0.9.6.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/bcprov-jdk15on-157.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-compiler-3.0.7.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/janino-3.0.7.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/balloontip-1.2.4.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/reflections-0.9.11.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/guava-20.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javassist-3.21.0-GA.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/cloning-1.9.6.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/objenesis-2.6.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/syslog-java-client-1.1.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javax.mail.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-email-1.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsendnsca-2.1.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/slf4j-api-1.7.25.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/slf4j-log4j12-1.7.25.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/velocity-engine-core-2.0.jar

export CLASSPATH=${CLASSPATH}:${HOME}/.asetune/jdbc_drivers/*
export CLASSPATH=${CLASSPATH}:${HOME}/.asetune/lib/*
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jdbc_drivers/*
export CLASSPATH=${CLASSPATH}:${EXTRA_JDBC_DRIVERS}

export PATH=${DBXTUNE_JAVA_HOME}/bin:${JAVA_HOME}/bin:${PATH}
cd ${APPL_HOME}

echo "================================================================"
echo "Information about some environment variables"
echo "----------------------------------------------------------------"
echo SYBASE=${SYBASE}
echo ${APPL_HOME_propName}=${APPL_HOME}
echo ${APPL_SAVE_DIR_propName}=${APPL_SAVE_DIR}
echo CLASSPATH=${CLASSPATH}
echo DBXTUNE_JAVA_HOME=${DBXTUNE_JAVA_HOME}
echo JAVA_HOME=${JAVA_HOME}

#------------------------------------------------------------------------
#--- CHECK current Java Version
#------------------------------------------------------------------------
java com.asetune.utils.JavaVersion 7
if [ $? -ne 0 ]
then
	echo "-----------------------------------------------------------------------"
	echo "Error: Use a higher java version."
	echo "-----------------------------------------------------------------------"
	echo "The java installation can be pointed out using the variable JAVA_HOME"
	echo "Current DBXTUNE_JAVA_HOME variable is set to ${DBXTUNE_JAVA_HOME}"
	echo "Current JAVA_HOME         variable is set to ${JAVA_HOME}"
	echo "-----------------------------------------------------------------------"
	
	exit 1
fi

#------------------------------------------------------------------------
#--- Parse some command line parameters
#--- - reset SPLASH if we have -noGui command line switch
#------------------------------------------------------------------------
for arg in "$@"
do
	case "$arg" in
	-n*)      export SPLASH="";;
	--noGui*) export SPLASH="";;
	esac
done


#------------------------------------------------------------------------
#--- SET memory parameters, if 64 bit java: add more memory
#------------------------------------------------------------------------
export JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS_32}
export JVM_GC_PARAMS=${JVM_GC_PARAMS_32}
java com.asetune.utils.JavaBitness
if [ $? -eq 64 ]
then
	export JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS_64}
	export JVM_GC_PARAMS=${JVM_GC_PARAMS_64}
	echo "NOTE: Java is a 64 bit, AseTune will be allowed to use more memory"
fi
echo "JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS}"
echo "JVM_GC_PARAMS=${JVM_GC_PARAMS}"

#------------------------------------------------------------------------
#--- Add various command line switches depending on platform
#------------------------------------------------------------------------
OS=`uname`
if [ "${OS}" == "Darwin" ]
then
	echo "================================================================"
	echo "Mac OS: doing some specific settings for Java/Swing on Mac"
	echo "----------------------------------------------------------------"
	OS=mac
	export JVM_PARAMS="$JVM_PARAMS -Xdock:name=${longAppName}"
	export JVM_PARAMS="$JVM_PARAMS -Xdock:icon=${APPL_HOME}/lib/${shortAppName}_icon_32.gif"
	export JVM_PARAMS="$JVM_PARAMS -Dapple.laf.useScreenMenuBar=true"
	export JVM_PARAMS="$JVM_PARAMS -Dcom.apple.mrj.application.apple.menu.about.name=${longAppName}"
#	export JVM_PARAMS="$JVM_PARAMS -Dapple.awt.brushMetalLook=true"
	export JVM_PARAMS="$JVM_PARAMS -Dapple.awt.textantialiasing=true"
	export JVM_PARAMS="$JVM_PARAMS -Dcom.apple.macos.smallTabs=true"
fi
echo "JVM_PARAMS=${JVM_PARAMS}"




#echo java ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_PARAMS} -Duser.language=en -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DBXTUNE_JVM_SWITCHES} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} $@
      java ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_PARAMS} -Duser.language=en -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DBXTUNE_JVM_SWITCHES} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} $@


