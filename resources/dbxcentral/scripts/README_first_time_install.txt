##--------------------------------------
## Initial work:
## Make a connection to the host you are going to install on
## - ssh username@dbxtune.acme.com
## Check what the latest "drop" is: http://gorans.org/www/asetune/tmp/
## - Then set the latest/desired YYYY-MM-DD an the below 'timestamp' variable
##--------------------------------------

## download Software
timestamp="2018-07-04"
cd ${HOME}
mkdir ${HOME}/dbxtune
cd ${HOME}/dbxtune
wget http://gorans.org/www/asetune/tmp/asetune_${timestamp}.zip

## grab 'xtract_install_dbxtune.sh' which will do a seconds download and do chmod on some files
mkdir asetune_${timestamp}
cd asetune_${timestamp}
unzip ../asetune_${timestamp}.zip
cp resources/dbxcentral/scripts/xtract_install_dbxtune.sh ../
cd ..
chmod 755 xtract_install_dbxtune.sh

## Now execute the ‘xtract_install_dbxtune.sh’, which will download (again) and setup some basics…
./xtract_install_dbxtune.sh ${timestamp}     ## if YYYY-MM-DD is “today”, then you do not need to specify it


##--------------------------------------
## Create appdir... which copies some files to ${HOME}/.dbxtune/
##--------------------------------------
0/bin/dbxcentral.sh --createAppDir

		##
		## Below is output from when I did it
		##
		sybase@gorans-ub2:~/dbxtune$ 0/bin/dbxcentral.sh --createAppDir

		NOTE: You can set local environment in file: /home/sybase/.dbxtune/DBXTUNE.env

		NOTE: The DBXTUNE_JVM_PARAMETER_FILE: /home/sybase/.dbxtune/.dbxtune_jvm_settings.properties did NOT EXIST
		NOTE: Set/Change JVM Memory parameters by setting Environment variable: DBXTUNE_JVM_MEMORY_PARAMS

		================================================================
		Information about some environment variables
		----------------------------------------------------------------
		SYBASE=/opt/sybase/15.5
		DBXTUNE_HOME=/home/sybase/dbxtune/asetune_2018-07-04
		DBXTUNE_SAVE_DIR=/home/sybase/dbxtune/asetune_2018-07-04/data
		DBXTUNE_JAVA_HOME=
		JAVA_HOME=

		================================================================
		Checking Java Version
		----------------------------------------------------------------
		JAVA Location:       /usr/bin/java
		JAVA Version String: 1.8.0_171
		JAVA Version Number: 18
		NOTE: Java is a 64 bit, AseTune will be allowed to use more memory
		JVM_MEMORY_PARAMS=-Xmx2048m -Xms64m
		JVM_GC_PARAMS=
		JVM_PARAMS=-noverify -XX:-UseGCOverheadLimit

		================================================================
		Starting DbxTuneCentral
		----------------------------------------------------------------
		java -Xmx2048m -Xms64m -noverify -XX:-UseGCOverheadLimit -Duser.language=en -Dsybase.home=/opt/sybase/15.5 -DSYBASE=/opt/sybase/15.5 -DAPPL_HOME=/home/sybase/dbxtune/asetune_2018-07-04 -DDBXTUNE_HOME=/home/sybase/dbxtune/asetune_2018-07-04 -DDBXTUNE_SAVE_DIR=/home/sybase/dbxtune/asetune_2018-07-04/data -splash:lib/db2tune_splash.jpg com.asetune.central.DbxTuneCentral --createAppDir
		2018-06-28 18:01:22 DEBUG com.asetune.central.DbxTuneCentral - parseCommandLine: swith='a', value='null'.
		2018-06-28 18:01:22 WARN  com.asetune.utils.Configuration - Can't find any configuration named 'USER_TEMP', creating a new one.
		2018-06-28 18:01:22 WARN  com.asetune.utils.Configuration - Can't find any configuration named 'USER_CONF', creating a new one.
		2018-06-28 18:01:22 WARN  com.asetune.utils.Configuration - Can't find any configuration named 'SYSTEM_CONF', creating a new one.
		Found that 'DBXTUNE_HOME' is not a soft link. DBXTUNE_HOME='/home/sybase/dbxtune/asetune_2018-07-04', BUT the parent directory has a '0' soft link. I'm going to use that instead.
		Setting DBXTUNE_HOME='/home/sybase/dbxtune/0' during the application dir creation/upgrade.
		Creating directory '/home/sybase/.dbxtune' - to hold various files for DbxTuneCentral
		Creating directory '/home/sybase/.dbxtune/log' - where log files are stored.
		Creating directory '/home/sybase/.dbxtune/conf' - where properties/configuration files are located...
		Creating directory '/home/sybase/.dbxtune/dbxc' - where user/localized DbxCentral files are located...
		Creating directory '/home/sybase/.dbxtune/dbxc/bin' - DbxCentral local start files.
		Creating directory '/home/sybase/.dbxtune/dbxc/log' - DbxCentral log files.
		Creating directory '/home/sybase/.dbxtune/dbxc/conf' - DbxCentral local configuration files.
		Creating directory '/home/sybase/.dbxtune/dbxc/data' - DbxCentral database recording files. (NOTE: make a soft-link to location which has enough storage.)
		  * Create Symbolic Link from '/home/sybase/.dbxtune/dbxc/bin/list_ALL.sh' to '/home/sybase/dbxtune/0/bin/dbxc_list_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/sybase/.dbxtune/dbxc/bin/start_ALL.sh' to '/home/sybase/dbxtune/0/bin/dbxc_start_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/sybase/.dbxtune/dbxc/bin/stop_ALL.sh' to '/home/sybase/dbxtune/0/bin/dbxc_stop_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/sybase/.dbxtune/dbxc/bin/dbxPassword.sh' to '/home/sybase/dbxtune/0/bin/dbxPassword.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_dbxcentral.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to DbxCentral Server.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_dbxcentral.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_asetune.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to AseTune Collectors.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_asetune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_rstune.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to RsTune Collectors.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_rstune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_sqlservertune.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to SqlServerTune Collectors.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_sqlservertune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_postgrestune.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to PostgresTune Collectors.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_postgrestune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/start_mysqltune.sh' to directory '/home/sybase/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to MySqlTune Collectors.
			** chmod 755, to file '/home/sybase/.dbxtune/dbxc/bin/start_mysqltune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/SERVER_LIST' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - What Servers should be started/listed/stopped by 'dbxc_{start|list|stop}_ALL.sh'
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/DBX_CENTRAL.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Config file for DbxCentral Server
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/AlarmEventOverride.example.txt' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - just an example file.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/ase.GENERIC.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Sybase ASE
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/rs.GENERIC.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Sybase Replication Server
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/mysql.GENERIC.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for MySQL
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/postgres.GENERIC.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Postgres
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/conf/sqlserver.GENERIC.conf' to directory '/home/sybase/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Microsoft SQL-Server
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/xtract_install_dbxtune.sh' to directory '/home/sybase/dbxtune/0' succeeded. - Helper script to install new 'public/beta/in-development' DbxTune Software
			** chmod 755, to file '/home/sybase/dbxtune/0/xtract_install_dbxtune.sh'.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/DBXTUNE.env' to directory '/home/sybase/.dbxtune' succeeded. - Environment file that will be sources by various start scripts.
		  * Copy file '/home/sybase/dbxtune/0/resources/dbxcentral/scripts/interfaces' to directory '/home/sybase/.dbxtune' succeeded. - Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).
		sybase@gorans-ub2:~/dbxtune$

##--------------------------------------
## NOTE: maybe change ${HOME}/.dbxtune/dbxc/data to a filesystem with MUCH more space
##       a good idea is 300MB up to 1TB (or larger)
##--------------------------------------
# possibly do the below
cd ${HOME}/.dbxtune/dbxc
rmdir data
ln -s /path/with/much/space/dbxtune_data data
#ln -s /opt/dbxtune_data data
ll data

##--------------------------------------
## Prepare/Edit some files
##   step 1: add one/several ASE's to configuration
##   step 2: add SQL-Server to configuration (for simplicity: done after DbxCentral has been started and we have checked that it works)
##--------------------------------------

########################################
## step 1: add one/several ASE's to configuration
########################################

##--------------------------------------
## Add ASE servers to the interfaces file, or make a link to the real interfaces file
## - add entries manually by 'printf'
## - or: add entries manually by 'vi'
## - or: create a softlink (to already existing interfaces)
## NOTE 1: The servernames in the intefaces file SHOULD be the same as the servers @@servername
## NOTE 2: This step is only for Sybase Products like: ASE, IQ, RepServer, RepAgentX
##
cd ${HOME}/.dbxtune/
printf "\nASE_NAME\n\tquery tcp ether HOSTNAME PORT\n\n" >> ${HOME}/.dbxtune/interfaces
#vi interfaces
#rm interfaces
#ln -s /opt/sybase/interfaces interfaces

##--------------------------------------
## Describe what servers that should be monitored
## What collectors should be managed (started/stopped with start_ALL.sh / stop_ALL.sh)
## Hopefully the file is "self explainable"
##
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
		# I added the following entries
		GORANS_UB1_DS      ; 1 ; Test 1 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
		GORAN_UB2_DS       ; 1 ; Test 2 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
		GORAN_UB3_DS       ; 1 ; Test 3 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>

##--------------------------------------
## Add passwords for the servers (both DBMS user and OS/SSH user)
## Create passwords in file ($HOME/.passwd.enc)
## The DBMS username and OS username we are using when connecting to ASE/SQL-Server   (for SQL-Server on Windows we can currently NOT monitor the OS)
## Note: you only need to specify "-S srvName" if you have the same username with *different* passwords to different server names...
##       The logic is: if we can’t find the servername in the password file we will "fallback" to an "generic" username entry (which has no server-name specification)
##
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<os_user>   -P<passwd> [-S <os_host_name>]
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]

		## Below is an example of how I did it
		## ASE: GORANS_UB1_DS, GORAN_UB2_DS: has password secret
		## ASE: GORAN_UB3_DS: has password secret1
		## OS:  has password secret on all hosts
		
		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Ugorans -Psecret

		===================================================
		 File content: /home/sybase/.passwd.enc
		---------------------------------------------------
		sa: U2FsdGVkX19w2cApVrs5jt/gTfPHYrlvIKY3P8Yv/iQ=
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		---------------------------------------------------

		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret

		===================================================
		 File content: /home/sybase/.passwd.enc
		---------------------------------------------------
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		sa: U2FsdGVkX1/V9HABSg+SQ2j5+IxWsTVr1TCdRpMim4A=
		---------------------------------------------------

		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret1 -SGORAN_UB3_DS

		===================================================
		 File content: /home/sybase/.passwd.enc
		---------------------------------------------------
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		sa: U2FsdGVkX1/V9HABSg+SQ2j5+IxWsTVr1TCdRpMim4A=
		sa: GORAN_UB3_DS: U2FsdGVkX1+Dx3T9JFgv2EtAr4GuA7USsG7ocljOhR4=
		---------------------------------------------------

		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$

##--------------------------------------
## Edit NO-GUI Configuration files
##     - add your counters between: BEGIN/END: Performance Counter Settings
##     - set 'offline.sampleTime=##' to the sample interval you want
##     - This could also be overidden/specified (-i,--interval <seconds>) in start_xxxtune.sh or in the SERVER_LIST (as a parameter to start_xxxtune.sh)
## or: create new config files, done with AseTune or SqlServerTune in GUI mode, 
##     using a wizard: Menu->Tools->Create 'Record Session – Template file' Wizard...
##     then copy the content to the 'ase.GENERIC.conf'
##
cd ${HOME}/.dbxtune/dbxc/conf
vi ase.GENERIC.conf

##--------------------------------------
## Edit NO-GUI START files, possibly change
##     - dbmsUser=sa
##     - osUser=sybase
##     - start line depending on the SQL-Server is hosted on Windows or Linux (on Linux we want the OS to be monitored)
##
cd ${HOME}/.dbxtune/dbxc/bin
vi start_asetune.sh

##--------------------------------------
## TEST start, to see that it works as expected
## Watch the output and make sure it starts in a proper way
## If it comes up without errors, do Ctrl-C to stop it
## Hint 1, you should see something like: ... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.
## Hint 2, If it's 'Login problems', and you suspect it's the wrong password, edit 'start_asetune.sh' and uncomment row: export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"
##         Then look for '### DEBUG ###' in the output
##
cd ${HOME}/.dbxtune/dbxc/bin
./start_asetune.sh GORAN_UB3_DS

##--------------------------------------
## If everything works fine... Try to start all managed collectors and DBX-Central (start it in the *REAL* way)
## all the collectors/server and DbxCental will be started
##
## Check the logfiles under ${HOME}/.dbxtune/dbxc/log/<srvname>.log or <srvname>.console
##
cd ${HOME}/.dbxtune/dbxc/bin
./start_ALL.sh 

		## Below is an example of how I did it
		## Which starts 3 ASE collectors & DbxCentral
		
		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./start_ALL.sh
		Sourcing local environment from: /home/sybase/.dbxtune/DBXTUNE.env

		 * Starting DbxTune CENTRAL (with nohup in background), output to /home/sybase/.dbxtune/dbxc/log/DBX_CENTRAL.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORANS_UB1_DS' (with nohup in background, output to /home/sybase/.dbxtune/dbxc/log/GORANS_UB1_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORAN_UB2_DS' (with nohup in background, output to /home/sybase/.dbxtune/dbxc/log/GORAN_UB2_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORAN_UB3_DS' (with nohup in background, output to /home/sybase/.dbxtune/dbxc/log/GORAN_UB3_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Started all servers, Now LIST them

		cat: conf/SERVER_LIST: No such file or directory
		 >> INFO: DbxCentral - CENTRAL SERVER, PID=6404

		List of UN-REGISTERED servers (not in conf/SERVER_LIST)
		 >> INFO: AseTune - Collector for 'GORANS_UB1_DS' is running... PID=6550
		 >> INFO: AseTune - Collector for 'GORAN_UB2_DS' is running... PID=6694
		 >> INFO: AseTune - Collector for 'GORAN_UB3_DS' is running... PID=6881

		DbxTune Collector Count: 3   (in conf/SERVER_LIST: 0)
		DbxCentral Server Count: 1

		OK: All registered collectors and DbxCentral are running.

		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./list_ALL.sh

##--------------------------------------
## List the collectors and DbxCentral
##
cd ${HOME}/.dbxtune/dbxc/bin
./list_ALL.sh 


##--------------------------------------
## Now lets check that we can see data in DbxCentral
## if it doesn't work:
##    - check: more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console
##    - check: linux firewall, so that it allows incoming on 8080
## The admin page login (user='admin', password='admin')
##
web-browser(on your pc): http://<dbxtune-host>:8080/
for example: http://dbxtune.acme.com:8080/






########################################
## step 2: add SQL-Server
########################################

##--------------------------------------
## describe what SQL-Servers that should be monitored
##
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
		# I added the following entries (which is hosted on Linux)
		gorans-ub2      ; 1 ; SQL-Server on Linux ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

		# On windows it might look like
		winHostName\instName  ; 1 ; SQL-Server on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

##--------------------------------------
## Add passwords for the servers (both DBMS user and OS/SSH user)
##
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]

		## Below is an example of how I did it
		
		sybase@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret2 -Sgorans-ub2

##--------------------------------------
## Edit NO-GUI Configuration files
##     - add your counters between: BEGIN/END: Performance Counter Settings
##     - set 'offline.sampleTime=##' to the sample interval you want
##
cd ${HOME}/.dbxtune/dbxc/conf
vi sqlserver.GENERIC.conf

##--------------------------------------
## Edit NO-GUI START files, possibly change
##     - dbmsUser=dbxtune
##     - osUser=sybase
##     - start line depending on the SQL-Server is hosted on Windows or Linux (on Linux we want the OS to be monitored)
##
cd ${HOME}/.dbxtune/dbxc/bin
vi start_sqlservertune.sh

##--------------------------------------
## TEST start, to see that it works as expected
## If it comes up without errors, do Ctrl-C to stop it
## Hint 1, you should see something like: ... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.
## Hint 2, If it's 'Login problems', and you suspect it's the wrong password, edit 'start_sqlservertune.sh' and uncomment row: export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"
##         Then look for '### DEBUG ###' in the output
## Hint 3, if it's on Windows and the @@servername is 'hostName\instanceName', then the below test command should be: ./start_sqlservertune.sh hostName\\instanceName
##         The double backslash is just to "escape" that it's a backslash
##         BUT the ${HOME}/.dbxtune/dbxc/conf/SERVER_LIST should only have a single backslash (hostName\instanceName)
##
cd ${HOME}/.dbxtune/dbxc/bin
./start_sqlservertune.sh gorans-ub2

##--------------------------------------
## Now start it in the *REAL* way
## Only collectors/servers that are NOT started will be started (which is described in ${HOME}/.dbxtune/dbxc/conf/SERVER_LIST)
##
cd ${HOME}/.dbxtune/dbxc/bin
./start_ALL.sh 

##--------------------------------------
## Now lets check that we can see data in DbxCentral
## The new SQL-Server should show up as an icon on the first page
## if it doesn't work:
##    - check: more ${HOME}/.dbxtune/dbxc/log/gorans-ub2.console
##    - check: more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console
##
refresh the browser... or open a new tab: http://dbxtune.acme.com:8080/




######################################################################################
## Short info about the WEB Interface
##
## Start Page:
##   Button(s) under 'Quick access list' - Only shows a "subset" of all available graphs
##   Button(s) under 'Quick access list (show all available graphs)' - Shows ALL available graphs
##
##   Buttons will be "red" if the server has any ACTIVE Alarms
##   and the alarms will be visible at the bottom of the page...
##
##   - The "New Profile..." doesn't work yet
##     If you need to create a profile (choose what servers/graphs that are displayed)
##     let me know - and I can guide you on "how to do it manually" (add records in the Central DB)
##
## Server Page: http://dbxtune.acme.com:8080/overview
##   Here you will find various information like:
##   - Active Recordings
##   - Active Alarms
##   - All file(s) in the LOG Directory
##   - All file(s) in the CONF Directory
##   - Dbx Central database Iinformation
##   - Available offline/recordings databases
##   - Active Recordings, full meta-data file content
##
## Admin Page: http://dbxtune.acme.com:8080/admin/admin.html
##   Here you can dom some administration...
##   - Remove Any off the below servers (NOTE: stop collector before doing this)
##   - Disable Any off the below servers... (it will not show up in the server list)
##   - Enable Any off the below servers... (below list has been disabled)
##   - Add a server NOTE: Not yet implemented
##   - Restart DbxCentral web server
##   
## Desktop App Page: http://dbxtune.acme.com:8080/desktop_app.html
##   If any of the WEB Users wants to view the *detailed* recordings (connect to a recording database)
##   they will need the *NATIVE* application, so here they can download it...
##   and some quick install instructions.
##
## API - there are some available API's... not yet finalized
##   http://dbxtune.acme.com:8080/api/index.html will show you some short info...
##   
######################################################################################







mkdir ${HOME}/.dbxtune
mkdir ${HOME}/.dbxtune/dbxc

mkdir ${HOME}/.dbxtune/dbxc/bin    ## various start_XXX files
mkdir ${HOME}/.dbxtune/dbxc/log    ## various log files for different collectors and DbxCentral
mkdir ${HOME}/.dbxtune/dbxc/conf   ## configuration files for different collectors and DbxCentral
mkdir ${HOME}/.dbxtune/dbxc/data   ## or ln -s ${HOME}/.dbxtune/dbxc/data /somewhere/data

## link or create the interfaces file for Sybase Directory Services
cd ${HOME}/.dbxtune
#ln -s /opt/sybase/interfaces interfaces
#vi interfaces


ssh sybase@

cd ${HOME}
mkdir ${HOME}/dbxtune
cd ${HOME}/dbxtune
wget http://gorans.org/www/asetune/tmp/asetune_2018-06-26.zip


mkdir asetune_2018-06-26
cd asetune_2018-06-26
unzip ../asetune_2018-06-26.zip
cp resources/dbxcentral/scripts/xtract_install_dbxtune.sh ../
cd ..
chmod 755 xtract_install_dbxtune.sh
## Now execute the ‘xtract_install_dbxtune.sh’, which will download (again) and setup some basics…
./xtract_install_dbxtune.sh 2018-06-19     ## if YYYY-MM-DD is “today”, then you do not need to specify it


mkdir conf
#mkdir data   
ln -s /opt/dbxtune_data data
mkdir log



cp 0/resources/dbxcentral/scripts/start_ALL.sh                 ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/XXX/start_dbxcentral.sh      ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/XXX/start_asetune.sh         ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/XXX/start_sqlservertune.sh   ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/XXX/start_postgrestune.sh    ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/stop_ALL.sh                  ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/list_ALL.sh                  ${HOME}/.dbxtune/dbxc/bin/
cp 0/resources/dbxcentral/scripts/dbxPassword.sh               ${HOME}/.dbxtune/dbxc/bin/

cp 0/resources/dbxcentral/scripts/conf/DBX_CENTRAL.conf        ${HOME}/.dbxtune/dbxc/conf/
cp 0/resources/dbxcentral/scripts/conf/ase.GENERIC.conf        ${HOME}/.dbxtune/dbxc/conf/
cp 0/resources/dbxcentral/scripts/conf/sqlserver.GENERIC.conf  ${HOME}/.dbxtune/dbxc/conf/

cp 0/resources/dbxcentral/scripts/XXX/SERVER_LIST              ${HOME}/.dbxtune/dbxc/conf/

#cp 0/resources/dbxcentral/scripts/conf/interfaces              ${HOME}/.dbxtune/
