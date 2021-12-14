#! /bin/bash

### First parameter should be the server name
#srvName=$1
#
### Check that first parameter is NOT empty or a switch
#if [ -z "${srvName}" ] || [[ ${srvName} == -* ]]
#then
#	echo ""
#	echo "Usage: $(basename $0) servername [other-switches-to-dbxtune.sh]"
#	echo ""
#	exit 1
#fi
### Put rest of the parameters in variable 'extraParams'
### Which will be passed as last parameters to the underlying xxxtune.sh command
#shift
#extraParams=$@

##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.dbxtune/DBXTUNE.env ]
then
	echo "Sourcing local environment from: ${HOME}/.dbxtune/DBXTUNE.env"
	. ${HOME}/.dbxtune/DBXTUNE.env
fi

## Some basic directories
dbxCentralBase=${DBXTUNE_CENTRAL_BASE:-${HOME}/.dbxtune/dbxc}
dbxSwHome=${DBXTUNE_HOME:-${HOME}/dbxtune/0}


##----------------------------------------------
## set LOCAL environment
##----------------------------------------------

export DBXTUNE_CENTRAL_CONF=${dbxCentralBase}/conf
export DBXTUNE_CENTRAL_DATA=${dbxCentralBase}/data
export DBXTUNE_CENTRAL_LOG=${dbxCentralBase}/log
#export DBXTUNE_CENTRAL_WEB=${dbxSwHome}/resources/WebContent

export DBXTUNE_SAVE_DIR=${DBXTUNE_CENTRAL_DATA}

cfgFile=${DBXTUNE_CENTRAL_CONF}/DBX_CENTRAL.conf
logFile=${DBXTUNE_CENTRAL_LOG}/DBX_CENTRAL.log

## Possibly add this to the below start command...
#-Djava.net.preferIPv4Stack=true

##----------------------------------------------
## Start
##----------------------------------------------
${dbxSwHome}/bin/dbxcentral.sh -C ${cfgFile} --savedir ${DBXTUNE_SAVE_DIR} -L ${logFile} 
