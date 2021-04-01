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
dbmsUser=postgres
#dbmsUser=dbxtune

osUser=${USER:-dbxtune}

cfgFile=${dbxCentralBase}/conf/postgres.GENERIC.conf
logDir=${dbxCentralBase}/log
saveDir=${dbxCentralBase}/data
#logFile=${logDir}/${srvName%%.*}.log    ## only keep 'host1' of the srvName (host1.acme.com)


#export DBXTUNE_JVM_SWITCHES="-DSqlCaptureBrokerAse.debug=true"
#export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"

## Override settings for specific server names
#case ${srvName} in
#    srvname1 | srvname2)
#        DBXTUNE_JVM_SWITCHES="-Xmx4G"
#        ;;
#    srvname3)
#        cfgFile=${dbxCentralBase}/conf/sqlserver.srvname3.conf
#        ;;
#esac

##----------------------------------------------
## Start
##----------------------------------------------
${dbxSwHome}/bin/postgrestune.sh -n ${cfgFile} -U${dbmsUser} -S${srvName} -u${osUser} -L ${logDir} --savedir ${saveDir} ${extraParams}
