#! /bin/bash

##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.dbxtune/DBXTUNE.env ]
then
	. ${HOME}/.dbxtune/DBXTUNE.env
fi


pidList=$(ps -efww | egrep 'com.asetune.*Tune ' | grep -v grep | awk '{printf "%s ", $2}')
pidListCental=$(ps -efww | grep 'com.asetune.central.DbxTuneCentral' | grep -v grep | awk '{printf "%s ", $2}')

if [ -z "${pidList}" ]
then
	echo ""
	echo " * No DbxTune COLLECTOR processes is running..."
	echo ""
fi

if [ -z "${pidListCental}" ]
then
	echo ""
	echo " * No DbxTune CENTAL Server processes is running..."
	echo ""
fi

if [ -z "${pidList}" ] && [ -z "${pidListCental}" ]
then
	exit 0
fi


if [ ! -z "${pidList}" ]
then
	for pid in ${pidList}
	do
		echo ""
		echo "======================================================"
		ps -fww -p ${pid}
		echo "------------------------------------------------------"
		echo " * Killing pid: ${pid}"
		kill ${pid}
	done
fi


if [ ! -z "${pidListCental}" ]
then
	for pid in ${pidListCental}
	do
		echo ""
		echo "======================================================"
		echo "==== DbxTune Central Server =========================="
		echo "======================================================"
		ps -fww -p ${pid}
		echo "------------------------------------------------------"
		echo " * Killing pid: ${pid}"
		kill ${pid}
	done
fi

echo " * Sleeping for 5 seconds, then listing processes using: list_ALL.sh"
sleep 5
echo ""



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
## Check that they are stopped
##----------------------------------------------
if [ -z "${listAll}" ]
then
	echo " **** Sorry I couldn't find the 'list_ALL.sh' command."
	echo " **** So I can NOT check for stopped servers."
	echo ""
else
	while true
	do
		## List servers... if there are still some up... sleep a while longer...
		${listAll} --onlyRunning
		if [ $? -eq 0 ]
		then
			echo " All servers has now been stopped"
			echo ""
			break
		else
			echo " **** There are still some servers up and running..."
			echo " **** Sleeping 3 seconds and then testing again"
			echo ""
			sleep 3
		fi
	done
fi
