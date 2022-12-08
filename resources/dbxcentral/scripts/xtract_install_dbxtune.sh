#! /bin/bash 

inputParam=$1
localZipFileName=""
wgetBaseDir="http://gorans.org/www/asetune/tmp"

echo ""
echo "##################################################################################"
echo "## Usage: $(basename $0) [list|YYYY-MM-DD|/full/path/to/filename.zip]"
echo "##"
echo "##        1 - No parameters, try to download todays release: $(date '+%Y-%m-%d')"
echo "##            If no release with 'todays' date exists, a list of available releses will be listed"
echo "##"
echo "##        2 - list"
echo "##            A list of available releses found at '${wgetBaseDir}'"
echo "##"
echo "##        3 - Specify a date you want to install: YYYY-MM-DD"
echo "##            If no release with 'YYYY-MM-DD' exists, a list of available releses will be listed"
echo "##"
echo "##        4 - Specify a filename to install, something like: asetune_YYYY-MM-DD.zip"
echo "##            If the file is NOT a ZIP file, we will exit"
echo "##################################################################################"
echo ""

if [ ! -z "${inputParam}" ] && [ -f ${inputParam} ] 
then
	if [[ $(file -b --mime-type "${inputParam}") == 'application/zip' ]]
	then
		echo ""
		echo "Install will use the local ZIP file: ${inputParam}"
		echo ""
		localZipFileName="$(realpath ${inputParam})"
	else
		echo ""
		echo "You passed the file '${inputParam}', which is NOT a ZIP file."
		echo "  - Pass in a valid ZIP file, you want to install."
		echo "  - Pass in a YYYY-MM-DD that will be downloaded"
		echo "  - If nothing is passwd in the release '$(date '+%Y-%m-%d')' will be downloaded and installed."
		echo "Exiting..."
		echo ""
		exit 1
	fi
else
	if [ -z "${inputParam}" ]
	then
		## set what release we want to download and install
		releaseDate="$(date '+%Y-%m-%d')"
	elif [ "${inputParam}" == "list" ]
	then
		echo ""
		echo " - Below is a listing of the last 10 releases at '${wgetBaseDir}'."
		echo "------------------------------------------------------------"
		wget -q -O - ${wgetBaseDir} | sed -n 's/.*href="\([^"]*\).*/\1/p' | grep '.zip' | tail -10 | sed -e 's/asetune_//' -e 's/.zip//' | awk '{ printf "-- %s\n", $0}'
		echo "------------------------------------------------------------"
		echo ""
		echo "Exiting..."
		echo ""
		exit 1
	else
		## Check if input look fine
		if [[ "${inputParam}" =~ ^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$ ]]
		then
			releaseDate="${inputParam}"
		else
			echo ""
			echo "ERROR: input parameter (if not a local file) should like: YYYY-MM-DD"
			echo "       You specified '${inputParam}'"
			echo ""
			echo "Exiting..."
			echo ""
			exit 1
		fi
	fi
fi

wgetFileName="asetune_${releaseDate}.zip"
wgetFullFile="${wgetBaseDir}/${wgetFileName}"

## Check if the Remote file exists
if [ -z "${localZipFileName}" ]
then
	fileCnt=$(wget -q -O - ${wgetBaseDir} | sed -n 's/.*href="\([^"]*\).*/\1/p' | grep "${wgetFileName}" | wc -l)
	#echo "--------------------fileCnt=${fileCnt}"
	#echo "--------------------wgetBaseDir=${wgetBaseDir}"
	#echo "--------------------wgetFileName=${wgetFileName}"

	if [ ${fileCnt} -eq 1 ]
	then
		echo ""
		echo " - The remote file '${wgetFullFile}' exists, continuing."
		echo ""
		localReleaseDir=$(basename --suffix=.zip "${wgetFullFile}")
	else
		echo ""
		echo " - The remote file '${wgetFullFile}' did NOT exists."
		echo " - Below is a listing of the last 10 releases at '${wgetBaseDir}'."
		echo "------------------------------------------------------------"
		wget -q -O - ${wgetBaseDir} | sed -n 's/.*href="\([^"]*\).*/\1/p' | grep '.zip' | tail -10 | sed -e 's/asetune_//' -e 's/.zip//' | awk '{ printf "-- %s\n", $0}'
		echo "------------------------------------------------------------"
		echo ""
		echo "Exiting..."
		echo ""
		exit 1
	fi
else
	localReleaseDir=$(basename --suffix=.zip "${localZipFileName}")
fi


#echo "localReleaseDir=|${localReleaseDir}|"
#echo "wgetFullFile=|${wgetFullFile}|"
#exit 1

##--------------------------------------------------------
echo ""
echo "## --- Remove current release DIR '$(pwd)/${localReleaseDir}' (if it exists), and the softlink '$(pwd)/0'"
rm -f 0
rm -f -r ${localReleaseDir}


##--------------------------------------------------------
echo ""
echo "## --- Create the new release dir '$(pwd)/${localReleaseDir}' and create a softlink '0' to this directory"
mkdir ${localReleaseDir}
ln -s ${localReleaseDir} 0


if [ -z "${localZipFileName}" ]
then
	##--------------------------------------------------------
	echo ""
	echo "## --- Remove current release ZIP '$(pwd)/${wgetFileName}' (if it exists)"
	rm -f ${wgetFileName}


	##--------------------------------------------------------
	echo ""
	echo "## --- Get a new install ZIP file from '${wgetFullFile}'"
	wget ${wgetFullFile}
fi


##--------------------------------------------------------
echo ""
echo "## --- goto the release dir '$(pwd)/${localReleaseDir}'."
cd ${localReleaseDir}
echo "## --- Current path is now '$(pwd)'"

if [ -z "${localZipFileName}" ]
then
	echo ""
	echo "## --- Unpack the FETCHED ZIP file '../${wgetFileName}' into directory '$(pwd)'."
	unzip -q ../${wgetFileName}
else
	echo ""
	echo "## --- Unpack the LOCAL ZIP file '${localZipFileName}' into directory '$(pwd)'."
	unzip -q ${localZipFileName}
fi


##--------------------------------------------------------
echo ""
echo "## --- change all *.sh file in the directory '$(pwd)/bin'."
chmod 755 bin/*.sh


##--------------------------------------------------------
echo ""
echo "## --- Go back to the base directory: cd .."
cd ..
echo "## --- Current path is now '$(pwd)'"


##--------------------------------------------------------
echo ""
echo "## --- List all files in the install directory '$(pwd)/${localReleaseDir}'"
ls -Fal ${localReleaseDir}


##--------------------------------------------------------
echo ""
echo "## --- List shell scripts in the install/bin directory '$(pwd)/${localReleaseDir}/bin/*.sh'"
ls -Fal ${localReleaseDir}/bin/*.sh


##--------------------------------------------------------
echo ""
echo "## --- Done."

