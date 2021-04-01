#! /bin/bash

##
## Set some switch defaults
##
longList=0
printSummary=1
onlyRunning=0


##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.dbxtune/DBXTUNE.env ]
then
	#echo "Sourcing local environment from: ${HOME}/.dbxtune/DBXTUNE.env"
	. ${HOME}/.dbxtune/DBXTUNE.env
fi


##----------------------------------------------
## Override some settings/environments
##----------------------------------------------
dbxCentralBase=${DBXTUNE_CENTRAL_BASE:-${HOME}/.dbxtune/dbxc}


##
## Parse command line options
##
UNKNOWN_PARAMS=()
while [[ $# -gt 0 ]]
do
	key="$1"

	case $key in
	
	-l|--long)
		longList=1
		shift # past value
		;;

	-n|--noSummary)
		printSummary=0
		shift # past value
		;;

	-r|--onlyRunning)
		onlyRunning=1
		shift # past value
		;;

	*)    # unknown option
		UNKNOWN_PARAMS+=("$1") # save it in an array for later
		shift # past argument
		;;
	esac
done
set -- "${UNKNOWN_PARAMS[@]}" # restore positional parameters

if [ ${#UNKNOWN_PARAMS[@]} -gt 0 ]
then
	echo ""
	echo "Error: Unknown option(s): ${UNKNOWN_PARAMS[@]}"
	echo ""
	echo "Usage: $(basename $0) [-r] [-l] [-n]"
	echo "     -r   Print only running servers (used to check during stop_ALL)"
	echo "     -l   long output, prints ps output"
	echo "     -n   no nummary"
	echo ""
	exit 1
fi


##
## Check for ANY pids
##
pidList=$(ps -efww | egrep 'com.asetune.*Tune ' | grep -v grep | awk '{printf "%s ", $2}')
pidListCental=$(ps -efww | grep 'com.asetune.central.DbxTuneCentral' | grep -v grep | awk '{printf "%s ", $2}')

echo ""
if [ -z "${pidList}" ] 
then
	echo " * No DbxTune COLLECTOR processes is running..."
	echo ""
fi

if [ -z "${pidListCental}" ] 
then
	echo " * No DbxTune CENTAL Server processes is running..."
	echo ""
fi

## If no pids, just exit
if [ -z "${pidList}" ] && [ -z "${pidListCental}" ]
then
	exit 0
fi


##-------------------------------------
## -r option... Just do THIS Part of the code
##
if [ ${onlyRunning} -gt 0 ]
then
	if [ ! -z "${pidList}" ]
	then
		for pid in ${pidList}
		do
			fullCmd=$(ps -ww --pid ${pid} --format cmd | grep java)
			params=( $( getopt -q -o D:S:A: -l serverAlias: -- ${fullCmd} ) )
			srvNameOrAlias=""
			withAliasDesc=""

			for (( j=0; j<${#params[@]}; j++ ))
			do
				if [ "${params[j]}" == "-S" ]
				then
					srvName=${params[j+1]}
				fi

				if [ "${params[j]}" == "-A" ] || [ "${params[j]}" == "--serverAlias" ]
				then
					srvNameOrAlias=$(echo ${params[j+1]} | tr -d \')
					withAliasDesc=" with alias name '${srvNameOrAlias}'"
				fi

				if [[ ${params[j]} =~ com.asetune.*Tune ]]
				then
					appName=$(echo ${params[j]} | tr -d \' | cut -d. -f3)
				fi
			done

			if [ -z "${appName}" ]
			then
				appName="DbxTune"
			fi

			if [ ${longList} -eq 0 ]
			then
				echo " >> ${appName}: ${srvName}${withAliasDesc}, PID=${pid}"
			else
				echo ""
				echo " >> ${appName}: ${srvName}${withAliasDesc}, PID=${pid}"
				echo "======================================================"
				ps -fww -p ${pid}
				echo "======================================================"
			fi
		done
	fi
	if [ ! -z "${pidListCental}" ]
	then
		if [ ${longList} -eq 0 ]
		then
			echo " >> DbxCentral: DBXTUNE_CENTRAL, PID=${pidListCental}"
		else
			echo ""
			echo " >> DbxCentral: DBXTUNE_CENTRAL, PID=${pidListCental}"
			echo "======================================================"
			ps -fww -p ${pid}
			echo "======================================================"
		fi
	fi

	pidListArray=( ${pidList} )
	pidListCentralArray=( ${pidListCental} )

	cnt=$(( ${#pidListArray[@]} + ${#pidListCentralArray[@]} ))
	exit ${cnt}
fi

##-------------------------------------
## Print REGISTERED Servers
##
## Get list of servers to start from file: ${dbxCentralBase}/conf/SERVER_LIST
## Fields in this file
##   1 - ASE SERVERNAME
##   2 - 1=Enabled, 0=Disabled
##   3 - Some explanation for the role of this server
##

## remove known pids from the below list (if registered servers)
unRegisteredPidListArray=( ${pidList} )

srvCount=0
srvUpCount=0
srvDownCount=0
srvDownList=() ## empty array
srvList=$(cat ${dbxCentralBase}/conf/SERVER_LIST | sed -e '/^[[:space:]]*$/d' -e '/^#/d' |  awk -F\; '{ if (int($2) > 0) print $1 }')
for srvName in ${srvList}
do
	srvCount=$(( srvCount++ ))
	
	## SQL Server specifics. escape \ into \\ otherwise ps will fuck up.
	escapedSrvName=$(echo "${srvName}" | sed 's/\\/\\\\/g')

	##
	## Check if server is already running
	##
	cnt=$(ps -efww | grep java | egrep 'com.asetune.*Tune' | egrep "\-S[ ]?${escapedSrvName} " | wc -l)
	pid=$(ps -efww | grep java | egrep 'com.asetune.*Tune' | egrep "\-S[ ]?${escapedSrvName} " | awk '{print $2}')

	## server is STARTED
	if [ ${cnt} -gt 0 ]
	then
		srvUpCount=$(( srvCount++ ))

		## remove from "all" list
		unRegisteredPidListArray=( $(echo "${unRegisteredPidListArray[@]/${pid}/}") )

		## Get detailes about this PID
		srvNameOrAlias=""
		withAliasDesc=""
		fullCmd=$(ps --pid ${pid} --format cmd | grep java)
		params=( $( getopt -q -o D:S:A: -l serverAlias: -- ${fullCmd} ) )
		for (( j=0; j<${#params[@]}; j++ ))
		do
			#if [ "${params[j]}" == "-S" ]
			#then
			#	srvName=${params[j+1]}
			#fi

			if [ "${params[j]}" == "-A" ] || [ "${params[j]}" == "--serverAlias" ]
			then
				srvNameOrAlias=$(echo ${params[j+1]} | tr -d \')
				withAliasDesc=" with alias name '${srvNameOrAlias}'"
			fi


			if [[ ${params[j]} =~ com.asetune.*Tune ]]
			then
				appName=$(echo ${params[j]} | tr -d \' | cut -d. -f3)
			fi
		done

		if [ -z "${appName}" ]
		then
			appName="DbxTune"
		fi

		if [ ${longList} -eq 0 ]
		then
			#echo " >> INFO: ${appName} - Collector for '${srvName}' is running... PID=${pid}"
			printf " >> INFO: %-15s - Collector for '${srvName}'${withAliasDesc} is running... PID=${pid}\n" "${appName}"
		else
			echo ""
			echo " >> INFO: ${appName} - Collector for '${srvName}'${withAliasDesc} is running... PID=${pid}"
			echo "======================================================"
			echo "${fullCmd}"
			echo "======================================================"
		fi

	else ## server is DOWN
		
		srvDownList+=($srvName)
		srvDownCount=$(( srvDownCount + 1 ))

		#echo " ----------------------------------------------------"
		#echo " >> WARN: Collector for '${srvName} is NOT running... RUN ./start_ALL.sh to start all servers that are DOWN."
		#echo " ----------------------------------------------------"
	fi
done

if [ ! -z "${pidListCental}" ]
then
	for pid in ${pidListCental}
	do
		if [ ${longList} -eq 0 ]
		then
			#echo " >> INFO: DbxCentral - CENTRAL SERVER, PID=${pidListCental}"
			printf " >> INFO: %-15s - CENTRAL SERVER, PID=${pidListCental}\n" "DbxCentral"
		else
			echo ""
			echo " >> INFO: DbxCentral - CENTRAL SERVER, PID=${pidListCental}"
			echo "======================================================"
			ps -fww -p ${pid}
			echo "======================================================"
		fi
	done
fi

##-------------------------------------
## Print UN-REGISTERED Servers
##
if [ ${#unRegisteredPidListArray[@]} -gt 0 ]
then
	echo ""
	echo "List of UN-REGISTERED servers (not in ${dbxCentralBase}/conf/SERVER_LIST)"

	for pid in ${unRegisteredPidListArray[@]}
	do
		fullCmd=$(ps --pid ${pid} --format cmd | grep java)
		params=( $( getopt -q -o D:S: -- ${fullCmd} ) )
		for (( j=0; j<${#params[@]}; j++ ))
		do
			if [ "${params[j]}" == "-S" ]
			then
				srvName=$(echo ${params[j+1]} | tr -d \')
			fi

			if [[ ${params[j]} =~ com.asetune.*Tune ]]
			then
				appName=$(echo ${params[j]} | tr -d \' | cut -d. -f3)
			fi
		done

		if [ -z "${appName}" ]
		then
			appName="DbxTune"
		fi

		if [ ${longList} -eq 0 ]
		then
			echo " >> INFO: ${appName} - Collector for ${srvName} is running... PID=${pid}"
		else
			echo ""
			echo " >> INFO: ${appName} - Collector for ${srvName} is running... PID=${pid}"
			echo "======================================================"
			echo "${fullCmd}"
			echo "======================================================"
		fi
	done
fi

##-------------------------------------
## Print NOT RUNNING REGISTERED Servers
##
if [ ${#srvDownList[@]} -gt 0 ]
then
	echo ""
	echo "======================================================"
	echo " WARNING: Servers are DOWN, below is a list of servers that are NOT started (but exists in ${dbxCentralBase}/conf/SERVER_LIST)"
	echo "          To start them, RUN: ./start_ALL.sh"
	echo "======================================================"
	for srvName in ${srvDownList[@]}
	do
		echo " >> WARN: Collector for '${srvName}' is NOT running..."
	done
fi


##
## Make the vars into arrays
##
pidList=( ${pidList} )
pidListCentral=( ${pidListCental} )

if [ ${printSummary} -gt 0 ]
then
	echo ""
	echo "DbxTune Collector Count: ${#pidList[@]}   (in ${dbxCentralBase}/conf/SERVER_LIST: ${srvCount})"
	echo "DbxCentral Server Count: ${#pidListCentral[@]}"
	echo ""
	if [ ${srvDownCount} -gt 0 ]
	then
		echo "WARNING: ${srvDownCount} Collectors are down: ${srvDownList[@]}"
		echo ""
	fi
	if [ ${#pidListCentral[@]} -eq 0 ]
	then
		echo "WARNING: DbxTune Central Server IS DOWN"
		echo ""
	fi
	
	if [ ${srvDownCount} -eq 0 ] && [ ${#pidListCentral[@]} -gt 0 ]
	then
		echo "OK: All registered collectors and DbxCentral are running."
		echo ""
	fi
fi

cnt=$(( ${#pidList[@]} + ${#pidListCentral[@]} ))
exit ${cnt}

