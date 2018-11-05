#! /bin/bash

## First parameter should be the server name
srvName=$1

## Check that first parameter is NOT empty or a switch
if [ -z "${srvName}" ] || [[ ${srvName} == -* ]]
then
	echo ""
	echo "Usage: $(basename $0) servername [other-switches-to-dbxtune.sh]"
	echo ""
	exit 1
fi
## Put rest of the parameters in variable 'extraParams'
## Which will be passed as last parameters to the underlying xxxtune.sh command
shift
extraParams=$@

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
dbmsUser=mysql
#dbmsUser=dbxtune

osUser=${USER:-mysql}

#srvNameAlias="-A ${srvName}_pg"  ## if you use alias, don't forget to set the ${logFile} to the alias as well

cfgFile=${dbxCentralBase}/conf/mysql.GENERIC.conf
logDir=${dbxCentralBase}/log
saveDir=${dbxCentralBase}/data
logFile=${logDir}/${srvName%%.*}.log    ## only keep 'host1' of the srvName (host1.acme.com)

osTimeZone=$(cat /etc/timezone) ## example 'Europe/Stockholm'
JdbcOption="-O serverTimezone=${osTimeZone}"

#export DBXTUNE_JVM_SWITCHES="-DSqlCaptureBrokerAse.debug=true"
#export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"

##----------------------------------------------
## Start
##----------------------------------------------
${dbxSwHome}/bin/mysqltune.sh -n ${cfgFile} -U${dbmsUser} -S${srvName} ${srvNameAlias} ${JdbcOption} -u${osUser} -L ${logFile} --savedir ${saveDir} ${extraParams}
