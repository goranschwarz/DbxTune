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


##----------------------------------------------
## Check that the server names are UNIQUE in the SERVER_LIST
##----------------------------------------------
tmpSrvList=$(cat ${dbxCentralBase}/conf/SERVER_LIST | sed -e '/^[[:space:]]*$/d' -e '/^#/d' |  awk -F\; '{ if (int($2) > 0) print $1 }')
tmpServerCount=$(echo ${tmpSrvList} | wc -w)
tmpUniqueCount=$(echo ${tmpSrvList} | xargs -n1 | sort -u | xargs | wc -w)
if [ ${tmpServerCount} -ne ${tmpUniqueCount} ]
then
	echo ""
	echo "========================================================================"
	echo " ERROR: server names are NOT unique"
	echo "        All server names if field 1 (server name) must be unique."
	echo ""
	echo " Duplicate Server Count: $(( ${tmpServerCount} - ${tmpUniqueCount} ))"
	echo " Total     Server Count: ${tmpServerCount}"
	echo " Unique    Server Count: ${tmpUniqueCount}"
	echo "========================================================================"
	echo " SERVER LIST file: ${dbxCentralBase}/conf/SERVER_LIST"
	echo " Below is ALL entries in the file."
	echo "------------------------------------------------------------------------"
	echo "${tmpSrvList}"
	echo "------------------------------------------------------------------------"
	echo "Exiting..."
	echo ""
	
	exit 1
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
	nohup ${dbxCentralBase}/bin/start_dbxcentral.sh > ${dbxCentralBase}/log/DBX_CENTRAL.console 2>&1 &
	## Stop writing to console file after 100M, so we don't use to much space... During statup, the interesting things are at the start of the file 
#	nohup stdbuf -i0 -o0 -e0 ${dbxCentralBase}/bin/start_dbxcentral.sh 2>&1 | head -c 100M > ${dbxCentralBase}/log/DBX_CENTRAL.console &

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

			## Get '-A' or '--serverAlias' to use as "console log" name
			srvNameOrAlias=${srvName}
			withAliasDesc=""
			params=( $( getopt -q -o A: -l serverAlias: -- ${startCmdEval} ) )
			for (( j=0; j<${#params[@]}; j++ ))
			do
				if [ "${params[j]}" == "-A" ] || [ "${params[j]}" == "--serverAlias" ]
				then
					srvNameOrAlias=$(echo ${params[j+1]} | tr -d \')
					withAliasDesc="with alias name '${srvNameOrAlias}'"
				fi
			done

			

			echo ""
			echo " * Starting monitoring of server '${srvName}' ${withAliasDesc} (with nohup in background, output to ${dbxCentralBase}/log/${srvNameOrAlias}.console)."
			nohup ${startCmdEval} > ${dbxCentralBase}/log/${srvNameOrAlias}.console 2>&1 &
			## Stop writing to console file after 100M, so we don't use to much space... During statup, the interesting things are at the start of the file 
#			nohup ${startCmdEval} 2>&1 | head -c 100M > ${dbxCentralBase}/log/${srvNameOrAlias}.console &
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
				cat ${dbxCentralBase}/log/${srvNameOrAlias}.console
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

