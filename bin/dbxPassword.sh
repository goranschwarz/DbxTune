#! /bin/bash

## Setup some defaults
operation=""
userName="sa"
srvName=""
password=""
keyPassPhrase="sybase"
#passwdFile="${HOME}/.passwd.enc"
#passwdFile="$(echo ~sybase/.passwd.enc)" ## Do echo so bash will resolve it to a real/full path 
passwdFile="$(echo ~/.passwd.enc)" ## Do echo so bash will resolve it to a real/full path 
debugMode=0
listPasswdFile=1

export OPENSSL_CONF=${OPENSSL_CONF:-/dev/null}

## Get Version of openssl
## - In Version '102' use:          echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
## - In Version '111' or above use: echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
openSslVersionNum=$(openssl version | awk '{print $2}' | tr -d -c 0-9)
openSslVersionStr=$(openssl version | awk '{print $2}')


#========================================
# BEGIN COMMAND LINE PARSING
#----------------------------------------

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
	echo "usage: $(basename $0) get|set [-P passwd] [-U user] [-S srv] [-k key] [-f file] [-l] [-x]"
	echo ""
	echo "-U  user name              DEFAULT '${userName}'"
	echo "-P  Password to use"
	echo "-S  Server name            DEFAULT '${srvName}'"
	echo "-k  key passphrase         DEFAULT '${keyPassPhrase}'"
	echo "-f  filename               DEFAULT '${passwdFile}'"
	echo "-x  debug mode             DEFAULT '${debugMode}'"
	echo "-l  list passwd file       DEFAULT '${listPasswdFile}'"
	echo ""

	exit 1
}

function readCmdLine
{
	local tmpFile=/tmp/${progname}_readCmdLine_$$

	## Note: getopt do not always deliver the right return code... so lets check the STDERR also
	local scriptName=$(basename $0)
	local args=$(getopt -o "U:P:S:k:f:xl" -n ${scriptName} -- $@ 2> ${tmpFile})
	local argsRetCode=$?
	local argsError=$(cat ${tmpFile})
	rm -f ${tmpFile}
	
	if [ ${argsRetCode} -ne 0 ] || [ ! -z "${argsError}" ]
	then
		printUsage "${argsError}"
		exit 2
	fi
	
	## Put the "unhandled" switches AFTER --
	eval set -- "${args}"

	## Now loop on the cmdline input
	local lCount=0
	while true
	do
		((lCount++))
		if [ ${debugMode} -gt 0 ]
		then
			echo "DEBUG: readCmdLine; reading command line switch '$1'."
		fi

		case $1 in
			-U)    userName=$2;                              shift 2;;
			-S)    srvName=$2;                               shift 2;;
			-P)    password=$2;                              shift 2;;
			-k)    keyPassPhrase=$2;                         shift 2;;
			-f)    passwdFile=$2;                            shift 2;;
			-x)    debugMode=1;                              shift;;
			-l)    (( listPasswdFile ^= 1 ));                shift;;

			--)
				operation=$2
				shift; 
				break
				;;

			*)
				echo "WARNING: readCmdLine; at command line switch '$1', did we forget a 'shift' in the above case logic?"
				shift; 
				exit 1
				;;
		esac
	done

	#--------------------------------------------------------------
	# Check parameters
	#--------------------------------------------------------------

	#------------------
	# operation 'get'
	if [ "${operation}" == "get" ]
	then
		local dummyVar=1
		
	#------------------
	# operation 'set'
	elif [ "${operation}" == "set" ]
	then
		#------------------
		# -P
		if [ -z "${password}" ]
		then
			echo ""
			echo "ERROR: '-P password'  MUST be specified for 'set' operation"
			printUsage
		fi
		
	#------------------
	# operation '' unknown
	else
			echo ""
			echo "ERROR: operation must be 'get' or 'set', you specified '${operation}'"
			printUsage
	fi
}
#========================================
# END COMMAND LINE PARSING
#----------------------------------------

#--------------------------------
# Function:
#--------------------------------
# How to "decrypt/use" it in a script:
# sa_passwd=$(awk -F: '{if ($1=="sa") print $2}' ${HOME}/.passwd.enc | openssl enc -aes-128-cbc -a -d -salt -pass pass:sybase)
# sa_passwd=$(awk -F: '{if ($1=="sa") print $2}' ${HOME}/.passwd.enc | sed 's/v2;//' | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase -d)
#--------------------------------
function addToSybasePasswordFile
{
	local pUsername=${1}
	local pPassword=${2}
	local pSrvname=${3}

	if [ -z "${pUsername}" ]; then echo "addToSybasePasswordFile(): #1 - pUsername is mandatory parameter"; exit 1; fi
	if [ -z "${pPassword}" ]; then echo "addToSybasePasswordFile(): #2 - pPassword is mandatory parameter"; exit 1; fi

	if [ -f ${passwdFile} ]
	then
		## copy all records except the '${pUsername}' user to a new file... then move that file to the origin...
		if [ -z "${pSrvname}" ]
		then
			egrep -v "^${pUsername}:[[:space:]]*(v2;)?U2FsdGVkX1" ${passwdFile} > ${HOME}/.passwd.enc.new
		else
			egrep -v "^${pUsername}:[[:space:]]*${pSrvname}:[[:space:]]*(v2;)?U2FsdGVkX1" ${passwdFile} > ${HOME}/.passwd.enc.new
		fi
		mv -f ${HOME}/.passwd.enc.new ${passwdFile}
	else
		## or if the file didn't exists, simply create a new file
		touch ${passwdFile}
		#echo "# This is how to encrypt a password:"  >> ${HOME}/.passwd.enc
		#echo '# echo "somePassword" | openssl enc -aes-128-cbc -a -salt -pass pass:${USER}'  >> ${HOME}/.passwd.enc
	fi
	## Append the '${pUsername}' user with the encrypted password AND change it to readonly
#	encPasswd=$(echo "${pPassword}" | openssl enc -aes-128-cbc -a -salt -pass pass:${USER})
	encPrefix=""
	encPasswd=""
	if [ ${openSslVersionNum} -lt 111 ]
	then
		encPasswd=$(echo "${pPassword}" | openssl enc -aes-128-cbc -a -salt -pass pass:${keyPassPhrase})
	else
		encPrefix="v2;"
		encPasswd=$(echo "${pPassword}" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k ${keyPassPhrase})
	fi

	printf "%-20s : %-20s : %s\n" "${pUserName}" "${pHostName}" "${lEncryptPrefixStr}${lEncPasswd}" >> ~postgres/.passwd.enc
	if [ -z "${pSrvname}" ]
	then
#		echo "${pUsername}: ${encPrefix}${encPasswd}" >> ${passwdFile}
		printf "%-20s : %s\n" "${pUsername}" "${encPrefix}${encPasswd}" >> ${passwdFile}
	else
#		echo "${pUsername}: ${pSrvname}: ${encPrefix}${encPasswd}" >> ${passwdFile}
		printf "%-20s : %-20s : %s\n" "${pUsername}" "${pSrvname}" "${encPrefix}${encPasswd}" >> ${passwdFile}
	fi
	chmod 400 ${passwdFile}
}



###################################################################
###################################################################
###################################################################
### MAIN STARTS HERE
###################################################################
###################################################################
###################################################################

################################################
# Read command line paramts
################################################
readCmdLine $@

#set -x

################################################
# Do the work
################################################

################################################
## GET
################################################
if [ ${operation} == "set" ]
then
	#echo "${password}" | openssl enc -aes-128-cbc -a -salt -pass pass:${USER}
	addToSybasePasswordFile ${userName} ${password} ${srvName}

	## List the passwdfile content
	if [ ${listPasswdFile} -gt 0 ]
	then
		echo ""
		echo "==================================================="
		echo " File content: ${passwdFile}"
		echo "---------------------------------------------------"
		cat ${passwdFile}
		echo "---------------------------------------------------"
		echo ""
	fi
################################################
## SET
################################################
elif [ ${operation} == "get" ]
then
	encPasswdStr=""
	if [ -z "${srvName}" ]
	then
		## get with NO server
		encPasswdStr=$(cat ${passwdFile} | tr -d "[:blank:]" | awk -F: -v userName="${userName}" '{if (NF==2 && $1==userName) print $2}')
	else
		## get WITH server
		encPasswdStr=$(cat ${passwdFile} | tr -d "[:blank:]" | awk -F: -v userName="${userName}" -v srvName="${srvName}" '{if (NF==3 && $1==userName && $2==srvName) print $3}')

		## if NOT found go back and use "fallback" (no server)
		if [ -z "${encPasswdStr}" ]
		then
			encPasswdStr=$(cat ${passwdFile} | tr -d "[:blank:]" | awk -F: -v userName="${userName}" '{if (NF==2 && $1==userName) print $2}')
		fi
	fi
	
	
	if [ -z "${encPasswdStr}" ]
	then
		(
			echo ""
			echo "##########################################"
			echo "ERROR: Password entry could not be found for user '${userName}' and server '${srvName}' in the file '${passwdFile}'."
			echo "##########################################"
			echo ""
		) >&2 ## to stderr
		exit 1
	else
		encPasswdStrPrefix=$(echo "${encPasswdStr}" | cut -c1-3)
		if [ "${encPasswdStrPrefix}" = "v2;" ]
		then
			if [ ${openSslVersionNum} -lt 111 ]
			then
				(
					echo ""
					echo "##########################################"
					echo "ERROR: Decrypting of password version 2 is not supported by openssl version '${openSslVersionStr}'."
					echo "ERROR: Please use an older version of 'openssl'. (version 1.1.0 or lower is supported)"
					echo "ERROR: ---------"
					if [ -z "${srvName}" ]
					then
						echo "ERROR: or simply set a new password using: dbxPassword.sh set -U${userName} -PthePassword"
					else
						echo "ERROR: or simply set a new password using: dbxPassword.sh set -U${userName} -S${srvName} -PthePassword"
					fi
					echo "ERROR: ---------"
					echo "ERROR: encrypted password entry '${encPasswdStr}'."
					echo "##########################################"
					echo ""
				) >&2 ## to stderr
				exit 1
			fi

			## Remove the prefix before decrypting
			encPasswdStr=$(echo "${encPasswdStr}" | cut -c4-)

			## Decrypt
			passwdStr=$(echo "${encPasswdStr}" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k ${keyPassPhrase} -d)
			osRc=$?
		else
			if [ ${openSslVersionNum} -ge 111 ]
			then
				(
					echo ""
					echo "##########################################"
					echo "ERROR: Decrypting of password version 1 is not supported by openssl version '${openSslVersionStr}'."
					echo "ERROR: You need to upgrade the password file '${passwdFile}'."
					echo "ERROR: ---------"
					echo "ERROR: This can be done with: dbxtune.sh dbxPasswordUpgrade [${passwdFile}]"
					if [ -z "${srvName}" ]
					then
						echo "ERROR: or simply set a new password using: dbxPassword.sh set -U${userName} -PthePassword"
					else
						echo "ERROR: or simply set a new password using: dbxPassword.sh set -U${userName} -S${srvName} -PthePassword"
					fi
					echo "ERROR: ---------"
					echo "ERROR: encrypted password entry '${encPasswdStr}'."
					echo "##########################################"
					echo ""
				) >&2 ## to stderr
				exit 1
			fi

			## Decrypt
			passwdStr=$(echo "${encPasswdStr}" | openssl enc -aes-128-cbc -a -d -salt -pass pass:${keyPassPhrase})
			osRc=$?
		fi

		if [ ${osRc} -eq 0 ]
		then
			echo ${passwdStr}
		else
			(
				echo ""
				echo "##########################################"
				echo "ERROR: Problems decrypting password string '${encPasswdStr}' for user '${userName}' and server '${srvName}' in the file '${passwdFile}'."
				echo "ERROR: This could be the pass phrase, which you can set with -k passphrase"
				echo "##########################################"
				echo ""
			) >&2 ## to stderr
			exit 1
		fi
	fi
################################################
## UNKNOWN Operation
################################################
else
	(
		echo ""
		echo "##########################################"
		echo "ERROR: unknown operation '${operation}'."
		echo "##########################################"
		echo ""
	) >&2 ## to stderr
	exit 1
fi
