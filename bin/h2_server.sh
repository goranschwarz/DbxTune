#!/bin/bash 
#-----------------------------------------------------------------------------
# Description: Generic Shell script to start/stop H2 server 
#-----------------------------------------------------------------------------
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
progname=$(basename $0)

##
## Setup some basics
##
export DBXTUNE_HOME=${DBXTUNE_HOME:-$scriptPath}
export DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-$DBXTUNE_HOME/data}

##
## Get H2 JAR file
## If we find several files versions, use the NEWEST version, and print a warning
## (older h2 jars are kept in lib, to upgrade old database files, see: dbxtune.sh h2upgrade)
##
h2JarFiles=( $(find ${DBXTUNE_HOME}/lib -maxdepth 1 -name "h2-*.jar" | sort -V) )
h2JarFile="${h2JarFiles[${#h2JarFiles[@]}-1]}"
if [ ${#h2JarFiles[@]} -gt 1 ]
then
	echo ""
	echo "Warning: Found several h2 jar files at ${DBXTUNE_HOME}/lib"
	echo "         I will use the newest jar file '${h2JarFile}'"
	echo ""
fi

##
## Assign some local variables based on input parameters
##
cmd="$1"
port="$2"
baseDir="$3"

##
## Check if baseDir parameter exists, if NOT use DBXTUNE_SAVE_DIR
##
if [ -z "${baseDir}" ]
then
	baseDir="${DBXTUNE_SAVE_DIR}"
fi

##
## port
##
portStart="" ## Used when starting
portStop=""  ## Used if we want to stop on a specific port
if [ ! -z ${port} ]
then
	portStart="-tcpPort ${port}"
	portStop=":${port}"
fi

##
## Do the work
##
case ${cmd} in
	start)
		cd ${DBXTUNE_HOME}
		echo "Starting H2 Database Server, db baseDir='${baseDir}'"
		java -cp ${h2JarFile} org.h2.tools.Server -tcp -tcpAllowOthers ${portStart} -ifExists -baseDir ${baseDir} &
		;;

	stop)
		echo "Shutting down H2 Database Server"
		java -cp ${h2JarFile} org.h2.tools.Server -tcpShutdown "tcp://localhost${portStop}"
		;;

	*)
		echo ""
		echo "Usage: ${progname} start|stop [port] [h2BaseDirectory]"
		echo ""
		exit 1
		;;
esac
