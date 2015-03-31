#! /bin/bash
#=======================================================================
# NOTE: This one needs more work...
#-----------------------------------------------------------------------
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

shortAppName=iqtune
longAppName=IqTune
javaMainClass=com.asetune.IqTune
javaSplashScreen=lib/iqtune_splash.jpg

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


#----------------------------------
#--- Java
#----------------------------------
export JAVA_HOME=${JAVA_HOME:-$SYBASE_JRE6}

echo "NOTE: Set/Change JVM Memory parameters by setting Environment variable: DBXTUNE_JVM_MEMORY_PARAMS" 

export JVM_MEMORY_PARAMS_32=${DBXTUNE_JVM_MEMORY_PARAMS:-"-Xmx1024m -Xms64m"}
export JVM_MEMORY_PARAMS_64=${DBXTUNE_JVM_MEMORY_PARAMS:-"-Xmx2048m -Xms64m"}

export JVM_GC_PARAMS_32=${DBXTUNE_JVM_GC_PARAMS:-""}
export JVM_GC_PARAMS_64=${DBXTUNE_JVM_GC_PARAMS:-""}

export JVM_PARAMS=${DBXTUNE_JVM_PARAMS:-""}

export EXTRA=
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=times
export DEBUG_OPTIONS=

export SPLASH=-splash:${javaSplashScreen}

export CLASSPATH=${APPL_HOME}/classes
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/asetune.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jtds-1.2.7.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/dsparser.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-1.2.17.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-1.3.176.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/wizard.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-swing-4.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-core-4.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/swingx-all-1.6.5-1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jchart2d-3.2.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/planviewer.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-cli-1.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/proxy-vole_20131209.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/ganymed-ssh2-build251beta1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rsyntaxtextarea.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/autocomplete.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rstaui.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jcommon-1.0.21.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jfreechart-1.0.17.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/antlr-4.0-complete.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/juniversalchardet-1.0.3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/DDLGen.jar

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
java com.asetune.utils.JavaVersion 6
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
	echo "NOTE: Java is a 64 bit, IqTune will be allowed to use more memory"
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




#echo java -cp ${CLASSPATH} ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_PARAMS} -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} $@
      java -cp ${CLASSPATH} ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_PARAMS} -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} $@


