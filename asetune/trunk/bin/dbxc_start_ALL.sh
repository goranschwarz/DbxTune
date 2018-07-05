#! /bin/bash

##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.dbxtune/DBXTUNE.env ]
then
	echo "Sourcing local environment from: ${HOME}/.dbxtune/DBXTUNE.env"
	. ${HOME}/.dbxtune/DBXTUNE.env
fi



##----------------------------------------------
## Override some settings/environments
##----------------------------------------------
sleepTime=3
dbxCentralBase=${DBXTUNE_CENTRAL_BASE:-${HOME}/.dbxtune/dbxc}
export SYBASE=${SYBASE:-/home/sybase/.asetune}


##----------------------------------------------
## Check for list_ALL.sh
##----------------------------------------------
listAll="./list_ALL.sh"
if [ ! -f ${listAll} ]
then
	listAll="${dbxCentralBase}/bin/list_ALL.sh"
fi
if [ ! -f ${listAll} ]
then
	listAll="${DBXTUNE_HOME}/bin/dbxc_list_ALL.sh"
fi
if [ ! -f ${listAll} ]
then
	listAll=""
fi


##------------------------------------
## Function: 
##------------------------------------
function getStartCommandForServer {

	## SQL Server specifics. escape \ into \\ otherwise awk will fuck up.
	#local pSrvName=$(echo "$1" | sed 's/\\/\\\\/g')
	local pSrvName="$1"

	local tmpFile=/tmp/getStartCommandForServer.$$
	(
		echo 'function ltrim(s) { sub(/^[ \t\r\n]+/, "", s); return s }'
		echo 'function rtrim(s) { sub(/[ \t\r\n]+$/, "", s); return s }'
		echo 'function trim(s)  { return rtrim(ltrim(s)); }'
		echo ''
		echo 'function replaceSrvName(s)  { gsub(/<SRVNAME>/, srvName, s); return s }'
		echo ''
		echo 'BEGIN {'
		echo '    FS=";"'
		echo "    srvName=\"${pSrvName}\""
		echo '}'
		echo '{'
		echo '    if (trim($1) == srvName)'
		echo '        print replaceSrvName(trim($4))'
		echo '}'
	) > ${tmpFile}

	local startCmd=$(cat ${dbxCentralBase}/conf/SERVER_LIST | sed -e '/^[[:space:]]*$/d' -e '/^#/d' |  awk -f${tmpFile} -v srvName="${pSrvName}")
	
	## If command starts with '/' or '.' or '$' or '~' then return it "as is", if not append './' before the command
	if [[ ${startCmd} == /* ]] || [[ ${startCmd} == .* ]] || [[ ${startCmd} == \$* ]] || [[ ${startCmd} == \~* ]]
	then
		echo "${startCmd}"
	else
		echo "./${startCmd}"
	fi

	## Debug the awk file content
	#cat ${tmpFile} 1>&2
	#echo "|${startCmd}|" 1>&2

	rm -f ${tmpFile}
}

##
## Starting DbxCentral
##
cnt=$(ps -efww | grep java | grep '.central.DbxTuneCentral' | wc -l)
if [ ${cnt} -gt 0 ]
then
	echo ""
	echo " * INFO: DbxTune CENTRAL is already running... skipping this."
else
	echo ""
	echo " * Starting DbxTune CENTRAL (with nohup in background), output to ${dbxCentralBase}/log/DBX_CENTRAL.console)."
#	nohup ${dbxCentralBase}/bin/start_dbxcentral.sh | egrep -v '(Persisting Counters using|Sent subscription data)' > ${dbxCentralBase}/log/DBX_CENTRAL.console 2>&1 &
	nohup ${dbxCentralBase}/bin/start_dbxcentral.sh > ${dbxCentralBase}/log/DBX_CENTRAL.console 2>&1 &
	lastBgPid=$!
	echo "   Sleeping for ${sleepTime} before continuing with next server"
	sleep ${sleepTime}
	
	## Check if the PID is alive
	isPidAlive=$(ps --pid ${lastBgPid} | sed '1,1d' | wc -l)
	if [ ${isPidAlive} -eq 0 ]
	then
		echo ""
		echo "==================================================================="
		echo "ERROR: Problems Starting DbxTune CENTRAL."
		echo "------ BEGIN output from the start command ------------------------"
		cat ${dbxCentralBase}/log/DBX_CENTRAL.console
		echo "------ END   output from the start command ------------------------"
		echo ""
	fi
fi


##
## Get list of servers to start from file: ${dbxCentralBase}/conf/SERVER_LIST
## Fields in this file
##   1 - ASE SERVERNAME
##   2 - 1=Enabled, 0=Disabled
##   3 - Some explanation for the role of this server
##   4 - start command
##
srvList=$(cat ${dbxCentralBase}/conf/SERVER_LIST | sed -e '/^[[:space:]]*$/d' -e '/^#/d' |  awk -F\; '{ if (int($2) > 0) print $1 }')
for srvName in ${srvList}
do
	## SQL Server specifics. escape '\' into '\\' otherwise ps will fuck up.
	escapedSrvName=$(echo "${srvName}" | sed 's/\\/\\\\/g')

	##
	## Check if server is already running
	##
	cnt=$(ps -efww | grep java | egrep 'com.asetune.*Tune' | egrep "\-S[ ]?${escapedSrvName} " | wc -l)
	if [ ${cnt} -gt 0 ]
	then
		echo ""
		echo " * INFO: Server '${srvName} is already running... continuing with next server."
	else
		#getStartCommandForServer "${srvName}" ## For Debugging
		startCmd=$(getStartCommandForServer "${escapedSrvName}")
		if [ -z "${startCmd}" ]
		then
			echo ""
			echo " ERROR: Problems getting START Command for server '${srvName}'. no start command was found."
		else
			## evaluate / resolve any variables inside ${startCmd}
			escapedStartCmd=$(echo "${startCmd}" | sed 's/\\/\\\\/g') ## escape '\' into '\\' otherwise the non-escaped '\' will disappear in eval.
			startCmdEval=$( eval echo "${escapedStartCmd}" )
		
			echo ""
			echo " * Starting monitoring of server '${srvName}' (with nohup in background, output to ${dbxCentralBase}/log/${srvName}.console)."
#			nohup ${startCmdEval} | egrep -v 'Persisting Counters using' > ${dbxCentralBase}/log/${srvName}.console 2>&1 &
			nohup ${startCmdEval} > ${dbxCentralBase}/log/${srvName}.console 2>&1 &
			#echo "################## startCmd: ${startCmd}"
			#echo "############## startCmdEval: ${startCmdEval}"
			lastBgPid=$!

			echo "   Sleeping for ${sleepTime} before continuing with next server"
			sleep ${sleepTime}
			
			## Check if the PID is alive
			isPidAlive=$(ps --pid ${lastBgPid} | sed '1,1d' | wc -l)
			if [ ${isPidAlive} -eq 0 ]
			then
				echo ""
				echo "==================================================================="
				echo "ERROR: Problems Starting monitoring of server '${srvName}'."
				echo "  cmd: ${startCmd}"
				echo " eval: ${startCmdEval}"
				echo "------ BEGIN output from the start command ------------------------"
				cat ${dbxCentralBase}/log/${srvName}.console
				echo "------ END   output from the start command ------------------------"
				echo ""
			fi
		fi
	fi
done

##----------------------------------------------
## Check that they are stopped
##----------------------------------------------
if [ -z "${listAll}" ]
then
	echo " **** Sorry I couldn't find the 'list_ALL.sh' command."
	echo " **** So I can NOT list started servers."
	echo ""
else
	echo ""
	echo " * Started all servers, Now LIST them"

	${listAll}
fi

