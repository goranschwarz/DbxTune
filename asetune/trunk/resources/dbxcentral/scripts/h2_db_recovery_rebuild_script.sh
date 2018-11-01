#! /bin/bash
#=========================================================================
# Extract a DB to a script file and import it into a new DB
#=========================================================================

dataDir="${HOME}/.dbxtune/dbxc/data"
dbname="DBXTUNE_CENTRAL_DB"
#dbname="PROD_REP_2018-08-30"

sourceDb="${dataDir}/${dbname}"
destDb="${dataDir}/${dbname}_new"

scriptFile="${dataDir}/${dbname}_script.sql.zip"

h2Jar="${HOME}/dbxtune/0/lib/h2-*.jar"
h2UrlOpt=""
#h2UrlOpt=";DATABASE_TO_UPPER=false;COMPRESS=TRUE;WRITE_DELAY=30000;"
h2UrlOpt=";DATABASE_TO_UPPER=false;WRITE_DELAY=30000;"


# do we need to use: DATABASE_TO_UPPER=false

##
## Print some info
##
echo ""
echo " ******************************************************************************"
echo " *** printing some information."
echo " ******************************************************************************"
echo "     dbname     = ${dbname}"
echo "     dataDir    = ${dataDir}"
echo "     sourceDb   = ${sourceDb}"
echo "     destDb     = ${destDb}"
echo "     scriptFile = ${scriptFile}"
echo "     h2Jar      = ${h2Jar}"
echo "     h2UrlOpt   = ${h2UrlOpt}"
echo " ******************************************************************************"

##
## Check if destination database exists
##
if [ -f ${destDb}.mv.db ]
then
	echo ""
	echo " *** ERROR: the destination database already exists: ${destDb}"
	echo " ***   FIX: rm ${destDb}.mv.db"
	echo ""
	exit 1
fi
if [ -f ${scriptFile} ]
then
	echo ""
	echo " *** ERROR: the script file already exists: ${scriptFile}"
	echo " ***   FIX: rm ${scriptFile}"
	echo ""
	exit 1
fi

##
## read from SOURCEDB to a script file
##
echo ""
echo " ******************************************************************************"
echo " *** extracting database '${sourceDb}' to script file '${scriptFile}'."
echo " ******************************************************************************"
echo java -cp ${h2Jar} org.h2.tools.Script -url "jdbc:h2:file:${sourceDb}${h2UrlOpt}" -user sa -script ${scriptFile} -options 'compression DEFLATE'
time java -cp ${h2Jar} org.h2.tools.Script -url "jdbc:h2:file:${sourceDb}${h2UrlOpt}" -user sa -script ${scriptFile} -options 'compression DEFLATE'
javaRc=$?
echo "     java return code=${javaRc}"

echo "   * Listing script file '${scriptFile}'."
ls -Falh ${scriptFile}

if [ ${javaRc} -ne 0 ]
then
	echo ""
	echo " *** ERROR: java return code was ${javaRc}"
	echo " *** exiting..."
	echo ""
	exit ${javaRc}
fi

#exit

##
## Create the new DB and import the above created script file
##
echo ""
echo " ******************************************************************************"
echo " *** creating new database '${destDb}' and importing using script file '${scriptFile}'."
echo " ******************************************************************************"
echo java -cp ${h2Jar} org.h2.tools.RunScript -url "jdbc:h2:file:${destDb}${h2UrlOpt}" -user sa -script ${scriptFile} -options 'compression DEFLATE'
time java -cp ${h2Jar} org.h2.tools.RunScript -url "jdbc:h2:file:${destDb}${h2UrlOpt}" -user sa -script ${scriptFile} -options 'compression DEFLATE'
javaRc=$?
echo "     java return code=${javaRc}"

echo "   * Listing source and destination DB file '${sourceDb}'."
ls -Falh ${sourceDb}.mv.db
ls -Falh ${destDb}.mv.db

if [ ${javaRc} -ne 0 ]
then
	echo ""
	echo " *** ERROR: java return code was ${javaRc}"
	echo " *** exiting..."
	echo ""
	exit ${javaRc}
fi


##
## compacting the new database
##
echo ""
echo " ******************************************************************************"
echo " *** compacting the new database '${destDb}'."
echo " ******************************************************************************"
tmpFile=/tmp/h2_shutdown.$$.sql
cat <<-EOF > ${tmpFile}
	shutdown defrag;
EOF
#	shutdown compact;
#	shutdown defrag;
echo java -cp ${h2Jar} org.h2.tools.RunScript -url "jdbc:h2:file:${destDb}${h2UrlOpt}" -user sa -script ${tmpFile}
time java -cp ${h2Jar} org.h2.tools.RunScript -url "jdbc:h2:file:${destDb}${h2UrlOpt}" -user sa -script ${tmpFile}
javaRc=$?
echo "     java return code=${javaRc}"
rm -f ${tmpFile}

echo "   * Listing destination DB file '${sourceDb}' after compacting."
ls -Falh ${destDb}.mv.db

if [ ${javaRc} -ne 0 ]
then
	echo ""
	echo " *** ERROR: java return code was ${javaRc}"
	echo " *** exiting..."
	echo ""
	exit ${javaRc}
fi


##
## Cleanup
##
echo ""
echo " *** Removing script file '${scriptFile}'."
rm -f ${scriptFile}

##
## Manual steps
##
echo ""
echo " *** IMPORTANT MANUAL COMMANDS that you need to complete the operation"
echo "     * Check that the new database actually works..."
echo "     * Then move or remove the source db"
echo "       mv ${sourceDb}.mv.db ${sourceDb}_OLD.mv.db"
echo "     * Then move the NEW database to the ORIGIN database name"
echo "       mv ${destDb}.mv.db ${sourceDb}.mv.db"
echo ""



## shutdown compact;
## shutdown defrag;
