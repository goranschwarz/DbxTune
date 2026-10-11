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
dbmsUser=dbxtune

osUser=dbxtune

cfgFile=${dbxCentralBase}/conf/oracle.GENERIC.conf
logDir=${dbxCentralBase}/log
saveDir=${dbxCentralBase}/data
#logFile=${logDir}/${srvName%%.*}.log    ## only keep 'host1' of the srvName (host1.acme.com)

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
##
## NOTE: The server name for Oracle is: host:port/serviceName  (example: prod-ora1:1521/ORCL)
##       Since it contains ':' and '/' you MUST also pass: -A alternateSrvName  (example: -A prod-ora1-ORCL)
##       The alternate server name is used for log files, H2 Database Recordings, DbxCentral SchemaName, etc...
##       In the SERVER_LIST file it looks like:
##         prod-ora1:1521/ORCL ; 1 ; Some Desc ; ${DBXTUNE_CENTRAL_BASE}/bin/start_oracletune.sh <SRVNAME> -A prod-ora1-ORCL
##----------------------------------------------
${dbxSwHome}/bin/oracletune.sh -n ${cfgFile} -U${dbmsUser} -S${srvName} -u${osUser} -L ${logDir} --savedir ${saveDir} ${extraParams}

## Or if you do NOT wan to monitor OS
#${dbxSwHome}/bin/oracletune.sh -n ${cfgFile} -U${dbmsUser} -S${srvName}            -L ${logDir} --savedir ${saveDir} ${extraParams}
