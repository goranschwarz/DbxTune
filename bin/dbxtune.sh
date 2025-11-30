#! /bin/bash
##--------------------------------------------------------------
## Description:
## Launch script for DbxTune
##
##--------------------------------------------------------------
## Usage:
##   dbxtune.sh xxx
## Where xxx is the tool name that we want to start
##--------------------------------------------------------------
## History:
## 2017-10-16 Goran Schwarz    Creation
##--------------------------------------------------------------
progname=$(basename $0)
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function printUsage
{
	local errorMsg=$1

	if [ -n "${errorMsg}" ]
	then
		echo ""
		echo "#########################################################################"
		echo "ERROR: ${errorMsg}"
		echo "#########################################################################"
	fi
	echo ""
	echo "usage: $progname toolset [cmdLineSwitches]"
	echo ""
	echo "Available toolset is:"
	echo " ase         - Sybase/SAP Adaptive Server Enterprise"
	echo " iq          - Sybase/SAP IQ - The Column Store DB"
	echo " rs          - Sybase/SAP Replication Server"
	echo " rax         - Sybase/SAP Replication Agent for X"
	echo " hana        - Sybase/SAP HANA in-memory Column Store DB"
	echo " sqlserver   - Microsoft SQL-Server"
	echo " oracle      - Oracle"
	echo " postgres    - Postgres"
	echo " mysql       - MySQL"
	echo " db2         - DB2 LUW (Linux Unix Windows)"
	echo ""            
	echo " sqlw        - SQL Window a JDBC Query Tool"
	echo " central     - A Component for multiple instances"
	echo "               If you want some central Web based view"
	echo " dbxcdbcopy  - Copy a DBX Cental DB to a new destination (used to migrate to a new DBMS)"
	echo " dbxPassword - Manage Encrypted Passwords"
	echo " dbxPasswordUpgrade - Upgrade the encrypted password file from v1 to v2"
	echo "               Property/Configuration encryption utility"
	echo " configEnc   - Encrypt a password that you can insert into a config file"
	echo ""

	exit 1
}

##-------------------------------------------------------
## If no input parameters, print usage
##
if [ -z "${1}" ]
then
	printUsage
fi

##-------------------------------------------------------
## Do settings based on the toolset
##
toolset_origin="${1}"
toolset=$(echo "${1}" | tr '[:upper:]' '[:lower:]')
case "${toolset}" in
	ase)
		shortAppName="asetune"
		longAppName="AseTune"
		javaMainClass="com.dbxtune.AseTune"
		javaMainParams=""
		javaSplashScreen="lib/asetune_splash.jpg"
		;;

	iq)
		shortAppName="iqtune"
		longAppName="IqTune"
		javaMainClass="com.dbxtune.IqTune"
		javaMainParams=""
		javaSplashScreen="lib/iqtune_splash.jpg"
		;;

	rs)
		shortAppName="rstune"
		longAppName="RsTune"
		javaMainClass="com.dbxtune.RsTune"
		javaMainParams=""
		javaSplashScreen="lib/rstune_splash.jpg"
		;;

	rax)
		shortAppName="raxtune"
		longAppName="RaxTune"
		javaMainClass="com.dbxtune.RaxTune"
		javaMainParams=""
		javaSplashScreen="lib/raxtune_splash.jpg"
		;;

	hana)
		shortAppName="hanatune"
		longAppName="HanaTune"
		javaMainClass="com.dbxtune.HanaTune"
		javaMainParams=""
		javaSplashScreen="lib/hanatune_splash.jpg"
		;;

	sqlserver)
		shortAppName="sqlservertune"
		longAppName="SqlServerTune"
		javaMainClass="com.dbxtune.SqlServerTune"
		javaMainParams=""
		javaSplashScreen="lib/sqlservertune_splash.jpg"
		;;

	oracle)
		shortAppName="oracletune"
		longAppName="OracleTune"
		javaMainClass="com.dbxtune.OracleTune"
		javaMainParams=""
		javaSplashScreen="lib/oracletune_splash.jpg"
		;;

	postgres)
		shortAppName="postgrestune"
		longAppName="PostgresTune"
		javaMainClass="com.dbxtune.PostgresTune"
		javaMainParams=""
		javaSplashScreen="lib/postgrestune_splash.jpg"
		;;

	mysql)
		shortAppName="mysqltune"
		longAppName="MySqlTune"
		javaMainClass="com.dbxtune.MySqlTune"
		javaMainParams=""
		javaSplashScreen="lib/mysqltune_splash.jpg"
		;;

	db2)
		shortAppName="db2tune"
		longAppName="Db2Tune"
		javaMainClass="com.dbxtune.Db2Tune"
		javaMainParams=""
		javaSplashScreen="lib/db2tune_splash.jpg"
		;;

	sqlw)
		shortAppName="sqlw"
		longAppName="SqlW"
		javaMainClass="com.dbxtune.tools.sqlw.QueryWindow"
		javaMainParams=""
		javaSplashScreen="lib/sqlw_splash.jpg"
		;;

	tailw)
		shortAppName="tailw"
		longAppName="TailW"
		javaMainClass="com.dbxtune.tools.tailw.LogTailWindow"
		javaMainParams=""
		javaSplashScreen="lib/tailw_splash.jpg"
		;;

	central)
		shortAppName="dbxtunecentral"
		longAppName="DbxTuneCentral"
		javaMainClass="com.dbxtune.central.DbxTuneCentral"
		javaMainParams=""
		javaSplashScreen=""
		useDbxCentralEnv=1
		;;

	dsr)
		shortAppName="dsr"
		longAppName="DailySummaryReport"
		javaMainClass="com.dbxtune.pcs.report.DailySummaryReport"
		javaMainParams=""
		javaSplashScreen=""
		useDbxCentralEnv=1
		;;

	h2fix)
		shortAppName="h2fix"
		longAppName="H2CentralDbCopy"
		javaMainClass="com.dbxtune.central.pcs.H2CentralDbCopy"
		javaMainParams=""
		javaSplashScreen=""
		useDbxCentralEnv=1
		;;

	dbxcdbcopy)
		shortAppName="dbxcdbcopy"
		longAppName="DbxCentralDbCopy"
#		javaMainClass="com.dbxtune.central.pcs.H2CentralDbCopy"
#		javaMainClass="com.dbxtune.central.pcs.H2CentralDbCopy2"
		javaMainClass="com.dbxtune.central.pcs.H2CentralDbCopy3"
		javaMainParams=""
		javaSplashScreen=""
		DBXTUNE_JVM_MEMORY_PARAMS="-Xmx4096m -Xms64m"
		useDbxCentralEnv=1
		;;

	dbxpassword|dbxPassword)
		shortAppName="dbxPassword"
		longAppName="dbxPassword"
		javaMainClass="com.dbxtune.utils.DbxPassword"
		javaMainParams=""
		javaSplashScreen=""
#		DBXTUNE_JVM_MEMORY_PARAMS="-Xmx4096m -Xms64m"
		;;

	dbxpasswordupgrade|dbxPasswordUpgrade)
		shortAppName="dbxPasswordUpgrade"
		longAppName="dbxPasswordUpgrade"
		javaMainClass="com.dbxtune.utils.DbxPasswordUpgrade"
		javaMainParams=""
		javaSplashScreen=""
#		DBXTUNE_JVM_MEMORY_PARAMS="-Xmx4096m -Xms64m"
		;;

	configutil|configUtil)
		shortAppName="configUtil"
		longAppName="configUtil"
		javaMainClass="com.dbxtune.utils.Configuration"
		javaMainParams=""
		javaSplashScreen=""
#		DBXTUNE_JVM_MEMORY_PARAMS="-Xmx4096m -Xms64m"
		;;

	h2srv)
		shortAppName="h2srv"
		longAppName="H2TcpServer"
		javaMainClass="org.h2.tools.Server"
		javaMainParams="-tcp -tcpAllowOthers -ifExists"
		#javaMainParams="-tcp -tcpAllowOthers -ifExists -baseDir ${baseDir}"
		javaSplashScreen=""
		;;

	sshtest)
		shortAppName="sshtest"
		longAppName="SshTest"
		javaMainClass="com.dbxtune.ssh.SshConnectionTester"
		javaMainParams=""
		javaSplashScreen=""
		;;

	sshtest2)
		shortAppName="sshtest2"
		longAppName="SshTest2"
		javaMainClass="com.dbxtune.test.JschTest2"
		javaMainParams=""
		javaSplashScreen=""
		;;

	mailtest)
		shortAppName="mailtest"
		longAppName="MailTest"
		javaMainClass="com.dbxtune.test.MailTest"
		javaMainParams=""
		javaSplashScreen=""
		;;

	mailtest2)
		shortAppName="mailtest2"
		longAppName="MailTest2"
		javaMainClass="com.dbxtune.test.MailTest2"
		javaMainParams=""
		javaSplashScreen=""
		;;

	## If the parameter starts with "com.dbxtune." then: test "any" classname passed in
	com.dbxtune.*)
		shortAppName="classtest"
		longAppName="ClassTest"
		javaMainClass="${toolset_origin}"
		javaMainParams=""
		javaSplashScreen=""
		;;

	*)
		printUsage "Unknow toolset '${toolset}'."
		exit 1
		;;
esac

## Remove appname from the args '$@' which is passed to java at the end of the script
shift

#----------------------------------
#--- Source local environment (if we got any)
#----------------------------------
if [ -f ${DBXTUNE_USER_ENV_FILE:-${HOME}/.dbxtune/DBXTUNE.env} ]
then
	echo "Sourcing local environment from: ${HOME}/.dbxtune/DBXTUNE.env"
	. ${HOME}/.dbxtune/DBXTUNE.env
else
	echo ""
	echo "NOTE: You can set local environment in file: ${HOME}/.dbxtune/DBXTUNE.env"
fi


#------------------------------------------------------------------------
#--- If we start any of the Collectors in NO-GUI mode
#--- Then use environment variables from DbxCentral ... (set in next section)
#------------------------------------------------------------------------
## Get options '-n or --noGui'
## OSX-getopt: does not have '-o' option, and do not support -l --longoptions, nor -q
OS=$(uname)
if [ "${OS}" == "Darwin" ]
then
	params=( $( getopt n: "$@" 2>/dev/null ) )
else
	params=( $( getopt -q -o n: -l noGui: -- "$@" ) )
fi

for (( j=0; j<${#params[@]}; j++ ))
do
	if [ "${params[j]}" == "-n" ] || [ "${params[j]}" == "--noGui" ] 
	then
		useDbxCentralEnv=1
	fi
done

#------------------------------------------------------------------------
#--- If we are starting DbxCentral -- change some environment variables
#------------------------------------------------------------------------
if [ ${useDbxCentralEnv:-0} -gt 0 ]
then
	## If DbxCentral variables are NOT setm assigne some default values
	if [ -z "${DBXTUNE_CENTRAL_SAVE_DIR}"    ]; then export DBXTUNE_CENTRAL_SAVE_DIR=${HOME}/.dbxtune/dbxc/data;       fi
	if [ -z "${DBXTUNE_CENTRAL_LOG_DIR}"     ]; then export DBXTUNE_CENTRAL_LOG_DIR=${HOME}/.dbxtune/dbxc/log;         fi
	if [ -z "${DBXTUNE_CENTRAL_CONF_DIR}"    ]; then export DBXTUNE_CENTRAL_CONF_DIR=${HOME}/.dbxtune/dbxc/conf;       fi
	if [ -z "${DBXTUNE_CENTRAL_INFO_DIR}"    ]; then export DBXTUNE_CENTRAL_INFO_DIR=${HOME}/.dbxtune/dbxc/info;       fi
	if [ -z "${DBXTUNE_CENTRAL_REPORTS_DIR}" ]; then export DBXTUNE_CENTRAL_REPORTS_DIR=${HOME}/.dbxtune/dbxc/reports; fi

	## set DBXTUNE_xxx = DBXTUNE_CENTRAL_xxx
	export DBXTUNE_SAVE_DIR=${DBXTUNE_CENTRAL_SAVE_DIR}
	export DBXTUNE_LOG_DIR=${DBXTUNE_CENTRAL_LOG_DIR}
	export DBXTUNE_CONF_DIR=${DBXTUNE_CENTRAL_CONF_DIR}
	export DBXTUNE_INFO_DIR=${DBXTUNE_CENTRAL_INFO_DIR}
	export DBXTUNE_REPORTS_DIR=${DBXTUNE_CENTRAL_REPORTS_DIR}

	echo ""
	echo "---------------------------------------------------------------------------------"
	echo "USING: DbxCentral Environments. The following Environment Variables will be used"
	echo "---------------------------------------------------------------------------------"
	echo "  DBXTUNE_SAVE_DIR    = ${DBXTUNE_SAVE_DIR}"
	echo "  DBXTUNE_LOG_DIR     = ${DBXTUNE_LOG_DIR}"
	echo "  DBXTUNE_CONF_DIR    = ${DBXTUNE_CONF_DIR}"
	echo "  DBXTUNE_INFO_DIR    = ${DBXTUNE_INFO_DIR}"
	echo "  DBXTUNE_REPORTS_DIR = ${DBXTUNE_REPORTS_DIR}"
	echo "---------------------------------------------------------------------------------"
fi


#----------------------------------
#--- XXX_HOME
#----------------------------------
APPL_HOME_propName="DBXTUNE_HOME"

#export DBXTUNE_HOME=${DBXTUNE_HOME:-$scriptPath}
## If DBXTUNE_HOME is not set, set it to the same as the script dir
## and if the script dir is '/bin' remove that part
if [ -z "${DBXTUNE_HOME}" ]
then
	export DBXTUNE_HOME=${scriptPath}
	if [[ "${scriptPath}" == */bin ]]
	then
		export DBXTUNE_HOME=${scriptPath::${#scriptPath}-4}
	fi
fi
#echo "DEBUG: scriptPath=|${scriptPath}|"
#echo "DEBUG: DBXTUNE_HOME=|${DBXTUNE_HOME}|"
export APPL_HOME=${DBXTUNE_HOME}


#----------------------------------
#--- XXX_SAVE_DIR
#----------------------------------
APPL_SAVE_DIR_propName="DBXTUNE_SAVE_DIR"

if [ -d ${HOME}/.dbxtune/data ]
then
	export DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-${HOME}/.dbxtune/data}
else
	export DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-$APPL_HOME/data}
fi

export APPL_SAVE_DIR=${DBXTUNE_SAVE_DIR}



#------------------------------------------------------------------------
#--- get JVM Parameters from file:
#------------------------------------------------------------------------
if [ "${toolset}" == "sqlw" ]
then
	DBXTUNE_JVM_PARAMETER_FILE=${HOME}/.dbxtune/.sqlw_jvm_settings.properties
else
	DBXTUNE_JVM_PARAMETER_FILE=${HOME}/.dbxtune/.dbxtune_jvm_settings.properties
fi
if [ -f ${DBXTUNE_JVM_PARAMETER_FILE} ]
then
	echo ""
	echo "Reading DBXTUNE_JVM_PARAMETER_FILE: ${DBXTUNE_JVM_PARAMETER_FILE}"
	cat ${DBXTUNE_JVM_PARAMETER_FILE}
	. ${DBXTUNE_JVM_PARAMETER_FILE} ## source the file...
else
	echo ""
	echo "NOTE: The DBXTUNE_JVM_PARAMETER_FILE: ${DBXTUNE_JVM_PARAMETER_FILE} did NOT EXIST"
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
export JVM_PARAMS=${DBXTUNE_JVM_PARAMS:-"-XX:-UseGCOverheadLimit"}
## -XX:-UseGCOverheadLimit ## will turn off 'OutOfMemoryError: GC overhead limit exceeded' -- https://stackoverflow.com/questions/1393486/error-java-lang-outofmemoryerror-gc-overhead-limit-exceeded
## which might happen if we collects to many SQLText objects...
## but this is also *dangerous* to disable, since the JVM will eat *much* more CPU when this happens...
##     - The GC throws this exception when too much time is spent in garbage collection for too little return, eg. 98% of CPU time is spent on GC and less than 2% of heap is recovered.
##     - This feature is designed to prevent applications from running for an extended period of time while making little or no progress because the heap is too small.

## Preparations for JAVA Flight Recorder and Mission Control
## Note: JFR is NOT part of OpenJDK-8, only in Oracle's JDK-8 and OpenJDK-11 (and in JDK-11 I don't think UnlockCommercialFeatures is needed)
## for more info see: https://medium.com/@chrishantha/java-flight-recorder-cheat-sheet-98f5143f5f88 or https://www.baeldung.com/java-flight-recorder-monitoring
## to start a recording, use: jcmd <pid> JFR.start name=DbxTuneRecording settings=profile duration=2h 
## to dump recording to file: jcmd <pid> JFR.dump name=DbxTuneRecording filename=/tmp/filename.dump.jfr
## to stop a recording, use:  jcmd <pid> JFR.stop name=DbxTuneRecording
## to check for recordings:   jcmd <pid> JFR.check
## BTW: I'm thinking about a interface in the DbxCentral Web UI so this can be done from there... but lets see if I'm implementing that ;)
if [ ${DBXTUNE_ENABLE_JFR:-0} -gt 0 ]
then
	JVM_PARAMS="${JVM_PARAMS} -XX:+UnlockCommercialFeatures -XX:+FlightRecorder"
fi


export EXTRA=
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=samples,interval=20,depth=50
#export DEBUG_OPTIONS=-agentlib:hprof=cpu=times
export DEBUG_OPTIONS=

export SPLASH=-splash:${javaSplashScreen}

export CLASSPATH=${APPL_HOME}/classes
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/dbxtune.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/dsparser.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-core-2.24.3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-api-2.24.3.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-1.2-api-2.24.3         ## ??? Jetty is still using Log4J 1.x so we need the bridge to Version 2 here
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-slf4j2-impl-2.24.3.jar  ## Jetty is still using SL4J (Simple Log 4 Java) and this is a bridge to Log4j2
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/log4j-jul-2.24.3.jar          ## Others that are using Java Util Logging -- bridge to Log4j2
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/slf4j-api-2.0.16.jar          ## Jetty is still using SLF4J (Simple Log Facade 4 Java) and this is the API
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jul-to-slf4j-2.0.16.jar       ## Bridge for JUL to SLF4J used by SchemaCrawlerUtils
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-SNAPSHOT.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-1.4.200.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-2.1.214.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/h2-2.4.240.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/wizard.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-swing-5.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/miglayout-core-5.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/swingx-all-1.6.5-1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jchart2d-3.3.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/planviewer.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-text-1.12.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-lang3-3.17.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-io-2.17.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-csv-1.12.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-cli-1.9.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-codec-1.17.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-email-1.6.0.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javax.mail.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javax.mail-1.6.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jakarta.mail-1.6.7.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jakarta.activation-2.0.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/proxy-vole_20131209.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/proxy-vole-1.1.5.jar    #---------------
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsch-2.27.3.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rsyntaxtextarea-3.6.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/autocomplete-3.3.2.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/rstaui-3.3.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/languagesupport-3.4.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jcommon-1.0.24.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jfreechart-1.5.5.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/antlr-4.0-complete.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/juniversalchardet-2.5.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/DDLGen.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/simplemagic-1.17.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsqlparser-4.5.jar
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsqlparser-4.9.jar  #------------------
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/bcprov-jdk18on-1.72.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/commons-compiler-3.1.12.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/janino-3.1.12.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/balloontip-1.2.4.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/reflections-0.9.11.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/guava-33.3.1-jre.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javassist-3.21.0-GA.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/syslog-java-client-1.1.7.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jsendnsca-2.1.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/velocity-engine-core-2.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jide-oss-3.7.15.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jackson-annotations-2.18.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jackson-core-2.18.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jackson-databind-2.18.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/gson-2.11.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jetty/*
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/javax.servlet-api-3.1.0.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/cron4j-2.2.5.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/cron-utils-9.2.1.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/schemacrawler-16.2.4.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/sql-formatter-2.0.5.jar

#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/tomcat/*

## In Java 9 and many different JDBC Drivers, Oracles (ojdbc7.jar) needs to be first otherwise there will be stacktraces with problems of loading drivers etc...
## In Java 9 and many different JDBC Drivers, Sybase jconn4.jar needs to be added *last*
#export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jdbc_drivers/ojdbc7.jar
export CLASSPATH=${CLASSPATH}:${HOME}/.dbxtune/jdbc_drivers/*
export CLASSPATH=${CLASSPATH}:${HOME}/.dbxtune/lib/*
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jdbc_drivers/*
export CLASSPATH=${CLASSPATH}:${EXTRA_JDBC_DRIVERS}
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn42.jar
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jconn4.jar


export PATH=${DBXTUNE_JAVA_HOME}/bin:${JAVA_HOME}/bin:${PATH}
cd ${APPL_HOME}


##------------------------------------------------------------------------
## Check that we got java installed
##------------------------------------------------------------------------
command -v java >/dev/null 2>&1
if [ $? -ne 0 ]
then
	echo ""
	echo "===================================================================="
	echo "ERROR: require 'java' but it's not found...  Aborting."
	echo "       do: sudo yum install java-devel"
	echo "Note: You can set environment JAVA_HOME or DBXTUNE_JAVA_HOME to specify where java is installed."
	echo "--------------------------------------------------------------------"
	echo ""

	exit 1
fi


echo ""
echo "================================================================"
echo "Information about some environment variables"
echo "----------------------------------------------------------------"
echo SYBASE=${SYBASE}
echo ${APPL_HOME_propName}=${APPL_HOME}
echo ${APPL_SAVE_DIR_propName}=${APPL_SAVE_DIR}
#echo CLASSPATH=${CLASSPATH}
echo DBXTUNE_JAVA_HOME=${DBXTUNE_JAVA_HOME}
echo JAVA_HOME=${JAVA_HOME}


echo ""
echo "================================================================"
echo "Checking Java Version"
echo "----------------------------------------------------------------"
javaLocation=$(which java)
javaVersionStr=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
javaVersionNum=$(java -version 2>&1 | head -n 1 | awk -F'["_.]' '{print $2}') ## get MAJOR
if [ ${javaVersionNum} -eq 1 ]
then
	## if old java versioning schema (1.6.xx | 1.7.xx | 1.8.xx)
	javaVersionNum=$(java -version 2>&1 | head -n 1 | awk -F'["_.]' '{print $3}')
fi
echo "JAVA Location:       ${javaLocation}"
echo "JAVA Version String: ${javaVersionStr}"
echo "JAVA Version Number: ${javaVersionNum}"
#if [ ${javaVersionNum} -lt 7 ]
if [ ${javaVersionNum} -lt 11 ]
then
	java -version
	echo ""
	echo "===================================================================="
	echo "ERROR: require a later version of 'java'...  Aborting."
	echo "       please install and try again. (sudo yum install java-devel)"
	echo ""
	echo "Note: You can set environment JAVA_HOME or DBXTUNE_JAVA_HOME to specify where java is installed."
	echo "--------------------------------------------------------------------"
	echo ""
	exit 1
fi

##
## FIX for Java 11 or above
##
#if [ ${javaVersionNum} -ge 11 ]
#then
#	echo ""
#	echo "================================================================"
#	echo "Adjustments for java 11 and above."
#	echo "Java version ${javaVersionNum} is used. This version is missing some basic JARS, which will be added"
#	echo "----------------------------------------------------------------"
#	echo "INFO: - Adding '${APPL_HOME}/lib/jaxb-ri/*' to CLASSPATH"
#	echo "----------------------------------------------------------------"
#	echo ""
#	export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jaxb-ri/*
#fi
export CLASSPATH=${CLASSPATH}:${APPL_HOME}/lib/jaxb-ri/*


#------------------------------------------------------------------------
#--- CHECK current Java Version
#--- Below is problematic, since the package is already compiled with a "higher" java than is executed.
#------------------------------------------------------------------------
#java com.dbxtune.utils.JavaVersion 7
#if [ $? -ne 0 ]
#then
#	echo "-----------------------------------------------------------------------"
#	echo "Error: Use a higher java version."
#	echo "-----------------------------------------------------------------------"
#	echo "The java installation can be pointed out using the variable JAVA_HOME"
#	echo "Current DBXTUNE_JAVA_HOME variable is set to ${DBXTUNE_JAVA_HOME}"
#	echo "Current JAVA_HOME         variable is set to ${JAVA_HOME}"
#	echo "-----------------------------------------------------------------------"
#	
#	exit 1
#fi

#------------------------------------------------------------------------
#--- Parse some command line parameters
#--- - reset SPLASH if we have -n or --noGui command line switch
#--- in no-gui mode: set jvmNoGuiSwitch, for easier identification when using top or similar
#------------------------------------------------------------------------
isNoGui=0
noGuiSrvName=""
jvmNoGuiSwitch=""

## Get options '-n or --noGui' and '-S or --server'
## OSX-getopt: does not have '-o' option, and do not support -l --longoptions, nor -q
OS=$(uname)
if [ "${OS}" == "Darwin" ]
then
	params=( $( getopt n:S: "$@" 2>/dev/null ) )
else
	params=( $( getopt -q -o n:S: -l noGui:,server: -- "$@" ) )
fi

for (( j=0; j<${#params[@]}; j++ ))
do
	if [ "${params[j]}" == "-n" ] || [ "${params[j]}" == "--noGui" ] 
	then
		isNoGui=1
		export SPLASH="" 
		unset DISPLAY   
	fi

	if [ "${params[j]}" == "-S" ] || [ "${params[j]}" == "--server" ] 
	then
		noGuiSrvName="${params[j+1]}"
	fi
done

## If nogui: simply set a -D flag to java; so it's easier to detect using 'ps' or 'top' what server we are monitoring
if [ ${isNoGui} -gt 0 ]
then
	jvmNoGuiSwitch="-Dnogui.${longAppName}.srv=${noGuiSrvName}"
fi
if [ "${longAppName}" == "DbxTuneCentral" ]
then
	jvmNoGuiSwitch="-DDbxCentral=true"
fi


#------------------------------------------------------------------------
#--- SET memory parameters, if 64 bit java: add more memory
#------------------------------------------------------------------------
export JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS_32}
export JVM_GC_PARAMS=${JVM_GC_PARAMS_32}
java com.dbxtune.utils.JavaBitness
if [ $? -eq 64 ]
then
	export JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS_64}
	export JVM_GC_PARAMS=${JVM_GC_PARAMS_64}
	echo "NOTE: Java is a 64 bit, DbxTune will be allowed to use more memory"
fi
echo "JVM_MEMORY_PARAMS=${JVM_MEMORY_PARAMS}"
echo "JVM_GC_PARAMS=${JVM_GC_PARAMS}"

#------------------------------------------------------------------------
#--- Add various command line switches depending on platform
#------------------------------------------------------------------------
OS=$(uname)
if [ "${OS}" == "Darwin" ]
then
	echo ""
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


#------------------------------------------------------------------------
#--- Some extra JVM PARAMS, out of memory
#------------------------------------------------------------------------
dumpDir=${DBXTUNE_USER_HOME:-${HOME}/.dbxtune}
export JVM_OOM_PARAMS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${dumpDir}"


#------------------------------------------------------------------------
#--- Start the process (in loop if shutdown-with-restart)
#------------------------------------------------------------------------
echo ""
echo "================================================================"
echo "Starting ${longAppName}"
echo "Using: ${javaMainClass} ${javaMainParams} $@"
echo "----------------------------------------------------------------"

osRc=0
while true
do
	echo java ${jvmNoGuiSwitch} ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_OOM_PARAMS} ${JVM_PARAMS} -Duser.language=en -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DBXTUNE_JVM_SWITCHES} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} ${javaMainParams} $@
	     java ${jvmNoGuiSwitch} ${JVM_MEMORY_PARAMS} ${JVM_GC_PARAMS} ${JVM_OOM_PARAMS} ${JVM_PARAMS} -Duser.language=en -Dsybase.home="${SYBASE}" -DSYBASE="${SYBASE}" -DAPPL_HOME=${APPL_HOME} -D${APPL_HOME_propName}="${APPL_HOME}" -D${APPL_SAVE_DIR_propName}="${APPL_SAVE_DIR}" ${EXTRA} ${DBXTUNE_JVM_SWITCHES} ${DEBUG_OPTIONS} ${SPLASH} ${javaMainClass} ${javaMainParams} $@
	osRc=$?

	## OS Retcun code 8 = restart	     
	if [ ${osRc} -ne 8 ]
	then
		break;
	fi
done

exit ${osRc}
