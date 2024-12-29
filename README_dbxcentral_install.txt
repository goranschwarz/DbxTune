##################################################################################################
## Manual installation instructions for DbxCentral - The central Web Console to view Trend graphs
##################################################################################################
NOTE 1: Right now, Windows isn't "really" supported... Which means there are no start scripts etc...
        Let me know if running DbxCentral on Windows is a deal breaker for you :)

For a big picture look at the file $DBXTUNE_HOME/doc/dbxtune-central.pptx


##--------------------------------------
## Initial work:
## Make a connection to the host you are going to install on (for the moment Linux/Unix is only tested/supported)
## - ssh username@dbxtune.acme.com
##   you can use any Unix/Linux user, but I recommend creating a separate user for the purpose, for example 'dbxtune' or 'dbxuser'
##--------------------------------------

##--------------------------------------
## Download and install the binaries into ${HOME}/dbxtune/
##--------------------------------------

## download Software, https://sourceforge.net/projects/asetune/files/
timestamp="2018-11-05"   ## the downloaded file are named: dbxtune_YYYY-MM-DD.zip
cd ${HOME}
mkdir ${HOME}/dbxtune
cd ${HOME}/dbxtune

## put the downloaded ZIP file user ${HOME}/dbxtune
## or download it again: 
##    wget https://sourceforge.net/projects/asetune/files/dbxtune_${timestamp}.zip/download
##    mv download dbxtune_${timestamp}.zip

cd ${HOME}/dbxtune
mkdir dbxtune_${timestamp}
ln -s dbxtune_${timestamp} 0    ## creates a "pointer" to the latest working release
cd 0
unzip ../dbxtune_${timestamp}.zip
chmod 755 bin/*.sh
cd ..


##--------------------------------------
## Some extra info about the DbxTune directory structures...
## DbxTune/DbxCentral consists of 2 distinct directory locations
##   1: The binary software, which resides under ${HOME}/dbxtune/0   (where 0 is a soft link to the latest sw release)
##   2: User configurations, log files, customized start files, etc... under: ${HOME}/.dbxtune
## Splitting the "software" and "user configuration" into two distinct locations makes it easier to "switch" between different DbxTune versions, or upgrade/install new versions.
##--------------------------------------


##--------------------------------------
## Create "appdir" (or user specifics)
## The below command creates directory ${HOME}/.dbxtune/ and copies some files to it
##--------------------------------------
cd ${HOME}/dbxtune
./0/bin/dbxcentral.sh --createAppDir

		##
		## Below is output from when I did it
		## Note: this example was done with a zip file: dbxtune_2018-06-28.zip 
		##      (which is an earlier version than you are installing, so there might be some differences in the output)
		##
		dbxtune@gorans-ub2:~/dbxtune$ 0/bin/dbxcentral.sh --createAppDir

		NOTE: You can set local environment in file: /home/dbxtune/.dbxtune/DBXTUNE.env

		NOTE: The DBXTUNE_JVM_PARAMETER_FILE: /home/dbxtune/.dbxtune/.dbxtune_jvm_settings.properties did NOT EXIST
		NOTE: Set/Change JVM Memory parameters by setting Environment variable: DBXTUNE_JVM_MEMORY_PARAMS

		================================================================
		Information about some environment variables
		----------------------------------------------------------------
		SYBASE=/opt/sybase/16.0
		DBXTUNE_HOME=/home/dbxtune/dbxtune/dbxtune_2018-06-28
		DBXTUNE_SAVE_DIR=/home/dbxtune/dbxtune/dbxtune_2018-06-28/data
		DBXTUNE_JAVA_HOME=
		JAVA_HOME=

		================================================================
		Checking Java Version
		----------------------------------------------------------------
		JAVA Location:       /usr/bin/java
		JAVA Version String: 1.8.0_171
		JAVA Version Number: 18
		NOTE: Java is a 64 bit, DbxTune will be allowed to use more memory
		JVM_MEMORY_PARAMS=-Xmx2048m -Xms64m
		JVM_GC_PARAMS=
		JVM_PARAMS=-noverify -XX:-UseGCOverheadLimit

		================================================================
		Starting DbxTuneCentral
		----------------------------------------------------------------
		java -Xmx2048m -Xms64m -noverify -XX:-UseGCOverheadLimit -Duser.language=en -Dsybase.home=/opt/sybase/16.0 -DSYBASE=/opt/sybase/16.0 -DAPPL_HOME=/home/dbxtune/dbxtune/dbxtune_2018-06-28 -DDBXTUNE_HOME=/home/dbxtune/dbxtune/dbxtune_2018-06-28 -DDBXTUNE_SAVE_DIR=/home/dbxtune/dbxtune/dbxtune_2018-06-28/data -splash:lib/db2tune_splash.jpg com.dbxtune.central.DbxTuneCentral --createAppDir
		2018-06-28 18:01:22 DEBUG com.dbxtune.central.DbxTuneCentral - parseCommandLine: swith='a', value='null'.
		2018-06-28 18:01:22 WARN  com.dbxtune.utils.Configuration - Can't find any configuration named 'USER_TEMP', creating a new one.
		2018-06-28 18:01:22 WARN  com.dbxtune.utils.Configuration - Can't find any configuration named 'USER_CONF', creating a new one.
		2018-06-28 18:01:22 WARN  com.dbxtune.utils.Configuration - Can't find any configuration named 'SYSTEM_CONF', creating a new one.
		Found that 'DBXTUNE_HOME' is not a soft link. DBXTUNE_HOME='/home/dbxtune/dbxtune/dbxtune_2018-06-28', BUT the parent directory has a '0' soft link. I'm going to use that instead.
		Setting DBXTUNE_HOME='/home/dbxtune/dbxtune/0' during the application dir creation/upgrade.
		Creating directory '/home/dbxtune/.dbxtune' - to hold various files for DbxTuneCentral
		Creating directory '/home/dbxtune/.dbxtune/log' - where log files are stored.
		Creating directory '/home/dbxtune/.dbxtune/conf' - where properties/configuration files are located...
		Creating directory '/home/dbxtune/.dbxtune/dbxc' - where user/localized DbxCentral files are located...
		Creating directory '/home/dbxtune/.dbxtune/dbxc/bin' - DbxCentral local start files.
		Creating directory '/home/dbxtune/.dbxtune/dbxc/log' - DbxCentral log files.
		Creating directory '/home/dbxtune/.dbxtune/dbxc/conf' - DbxCentral local configuration files.
		Creating directory '/home/dbxtune/.dbxtune/dbxc/data' - DbxCentral database recording files. (NOTE: make a soft-link to location which has enough storage.)
		  * Create Symbolic Link from '/home/dbxtune/.dbxtune/dbxc/bin/list_ALL.sh' to '/home/dbxtune/dbxtune/0/bin/dbxc_list_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/dbxtune/.dbxtune/dbxc/bin/start_ALL.sh' to '/home/dbxtune/dbxtune/0/bin/dbxc_start_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/dbxtune/.dbxtune/dbxc/bin/stop_ALL.sh' to '/home/dbxtune/dbxtune/0/bin/dbxc_stop_ALL.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Create Symbolic Link from '/home/dbxtune/.dbxtune/dbxc/bin/dbxPassword.sh' to '/home/dbxtune/dbxtune/0/bin/dbxPassword.sh' succeeded. - Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_dbxcentral.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to DbxCentral Server.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_dbxcentral.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_asetune.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to AseTune Collectors.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_asetune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_rstune.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to RsTune Collectors.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_rstune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_sqlservertune.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to SqlServerTune Collectors.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_sqlservertune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_postgrestune.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to PostgresTune Collectors.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_postgrestune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/start_mysqltune.sh' to directory '/home/dbxtune/.dbxtune/dbxc/bin' succeeded. - Change this for customer specializations to MySqlTune Collectors.
			** chmod 755, to file '/home/dbxtune/.dbxtune/dbxc/bin/start_mysqltune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/SERVER_LIST' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - What Servers should be started/listed/stopped by 'dbxc_{start|list|stop}_ALL.sh'
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/DBX_CENTRAL.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Config file for DbxCentral Server
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/AlarmEventOverride.example.txt' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - just an example file.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/ase.GENERIC.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Sybase ASE
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/rs.GENERIC.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Sybase Replication Server
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/mysql.GENERIC.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for MySQL
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/postgres.GENERIC.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Postgres
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/conf/sqlserver.GENERIC.conf' to directory '/home/dbxtune/.dbxtune/dbxc/conf' succeeded. - Example/template Config file for Microsoft SQL-Server
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/xtract_install_dbxtune.sh' to directory '/home/dbxtune/dbxtune/0' succeeded. - Helper script to install new 'public/beta/in-development' DbxTune Software
			** chmod 755, to file '/home/dbxtune/dbxtune/0/xtract_install_dbxtune.sh'.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/DBXTUNE.env' to directory '/home/dbxtune/.dbxtune' succeeded. - Environment file that will be sources by various start scripts.
		  * Copy file '/home/dbxtune/dbxtune/0/resources/dbxcentral/scripts/interfaces' to directory '/home/dbxtune/.dbxtune' succeeded. - Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).
		dbxtune@gorans-ub2:~/dbxtune$

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
##   step 3: add Postgres to configuration (or any of the other implementations, only small differences, which you will figure out)
##--------------------------------------

########################################
## step 1: add one/several ASE's to configuration
########################################

##--------------------------------------
## Add ASE servers to the interfaces file, or make a link to the real interfaces file
## - add entries manually by 'printf'
## - or: add entries manually by 'vi'
## - or: create a softlink (to already existing interfaces)
## NOTE 1: The servernames in the intefaces file SHOULD be the same as the servers @@servername   (this is IMPORTANT)
## NOTE 2: This step is only for Sybase Products like: ASE, IQ, RepServer, RepAgentX
##
cd ${HOME}/.dbxtune/
printf "\nASE_NAME\n\tquery tcp ether HOSTNAME PORT\n\n" >> ${HOME}/.dbxtune/interfaces
#vi interfaces
#rm interfaces
#ln -s /opt/sybase/interfaces interfaces

##--------------------------------------
## Describe what servers that should be monitored
## What collectors should be managed, started/stopped with:
##     ~/dbxtune/0/bin/dbxc_start_ALL.sh / ~/dbxtune/0/bin/dbxc_stop_ALL.sh 
##  or ~/.dbxtune/dbxc/bin/start_ALL.sh  / ~/.dbxtune/dbxc/bin/stop_ALL.sh
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
## The DBMS username and OS username we are using when connecting to ASE
## Note: you only need to specify "-S srvName" if you have the same username with *different* passwords to different server names...
##       So if you have same password for user xxx on all servers, you do not need to specify the -S flag
##       The logic is: if we can't find the servername in the password file we will "fallback" to an "generic" username entry (which has no server-name specification)
##
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<os_user>   -P<passwd> [-S <os_host_name>]
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]

		## Below is an example of how I did it
		## ASE: GORANS_UB1_DS, GORAN_UB2_DS: has password "secret"
		## ASE: GORAN_UB3_DS: has password "secret1"
		## OS:  has password "secret" on all hosts

		# Add OS user "gorans", with password "secret" on all unspecified OS hosts
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Ugorans -Psecret

		===================================================
		 File content: /home/dbxtune/.passwd.enc
		---------------------------------------------------
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		---------------------------------------------------

		# Add DBMS user "sa", with password "secret" on all unspecified ASE's
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret

		===================================================
		 File content: /home/dbxtune/.passwd.enc
		---------------------------------------------------
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		sa: U2FsdGVkX1/V9HABSg+SQ2j5+IxWsTVr1TCdRpMim4A=
		---------------------------------------------------

		# Add DBMS user "sa", with password "secret1" on ASE "GORAN_UB3_DS"
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret1 -SGORAN_UB3_DS

		===================================================
		 File content: /home/dbxtune/.passwd.enc
		---------------------------------------------------
		gorans: U2FsdGVkX1/TSgmIfJZ4e9CvmLzWiXgVj1rNuzQHBuo=
		sa: U2FsdGVkX1/V9HABSg+SQ2j5+IxWsTVr1TCdRpMim4A=
		sa: GORAN_UB3_DS: U2FsdGVkX1+Dx3T9JFgv2EtAr4GuA7USsG7ocljOhR4=
		---------------------------------------------------


##--------------------------------------
## Edit NO-GUI Configuration files
##     - add your counters between: BEGIN/END: Performance Counter Settings
##     - set 'offline.sampleTime=##' to the sample interval you want
##     - This could also be overidden/specified (-i,--interval <seconds>) in start_xxxtune.sh or in the SERVER_LIST (as a parameter to start_xxxtune.sh)
## or: create new config files, done with AseTune or SqlServerTune in GUI mode (or any of the dbxTune implementations)
##     using a wizard: Menu->Tools->Create 'Record Session â€“ Template file' Wizard...
##     then copy the content to the 'ase.GENERIC.conf'
##
cd ${HOME}/.dbxtune/dbxc/conf
vi ase.GENERIC.conf

##--------------------------------------
## Edit NO-GUI START files, possibly change
##     - dbmsUser=sa
##     - osUser=dbxtune
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
		## Which starts DbxCentral and 3 ASE collectors
		
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./start_ALL.sh
		Sourcing local environment from: /home/dbxtune/.dbxtune/DBXTUNE.env

		 * Starting DbxTune CENTRAL (with nohup in background), output to /home/dbxtune/.dbxtune/dbxc/log/DBX_CENTRAL.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORANS_UB1_DS' (with nohup in background, output to /home/dbxtune/.dbxtune/dbxc/log/GORANS_UB1_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORAN_UB2_DS' (with nohup in background, output to /home/dbxtune/.dbxtune/dbxc/log/GORAN_UB2_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Starting monitoring of server 'GORAN_UB3_DS' (with nohup in background, output to /home/dbxtune/.dbxtune/dbxc/log/GORAN_UB3_DS.console).
		   Sleeping for 3 before continuing with next server

		 * Started all servers, Now LIST them

		 >> INFO: DbxCentral - CENTRAL SERVER, PID=6404
		 >> INFO: AseTune - Collector for 'GORANS_UB1_DS' is running... PID=6550
		 >> INFO: AseTune - Collector for 'GORAN_UB2_DS' is running... PID=6694
		 >> INFO: AseTune - Collector for 'GORAN_UB3_DS' is running... PID=6881

		DbxTune Collector Count: 3   (in conf/SERVER_LIST: 3)
		DbxCentral Server Count: 1

		OK: All registered collectors and DbxCentral are running.

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

##
## Note: if you want DbxCentral to respond on port 80 instead of 8080
## look at -- https://www.eclipse.org/jetty/documentation/9.2.22.v20170531/setting-port80-access.html
##
## or in short, redirect port 80 to 8080, as root do: 
##     /sbin/iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
##
## The above is NOT persisted, and is cleared on machine reboot.    
## Possibly look at: https://www.systutorials.com/how-to-make-iptables-ip6tables-configurations-permanent-across-reboot-on-centos-7-linux/       
## or similar pages...
##
## Possible with firewall as well
##     firewall-cmd --add-forward-port=port=80:proto=tcp:toport=8080
##     firewall-cmd --list-forward-ports
##     firewall-cmd --runtime-to-permanent
##





########################################
## step 2: add SQL-Server
########################################

##--------------------------------------
## Preparations of the SQL Server DBMS, Step 1 -- RECOMENDED
##     - create a specific user in SQL Server, which is used to monitor the system with 
##--------------------------------------

	## Create a random password, in Linux you could do:
	dbxtunePasswd=$(cat /dev/urandom | tr -cd '[:alnum:]' | fold -w24 | head -n1); echo ${dbxtunePasswd}
	## Or use any online password generator: https://www.google.com/search?q=generate+password+online
	
	## as any SQL Server login with 'sysadmin' create the login 'dbxtune'
	CREATE LOGIN [dbxtune] WITH PASSWORD=N'the_long_and_arbitrary_password', DEFAULT_DATABASE=[master], CHECK_POLICY=OFF, CHECK_EXPIRATION=OFF;
	GRANT VIEW SERVER STATE          TO [dbxtune]; -- To view Server level statistics (like most DMV's)
	GRANT VIEW ANY DEFINITION        TO [dbxtune]; -- To be able to view Availability Groups, etc
	GRANT CONNECT ANY DATABASE       TO [dbxtune]; -- To access DMV's in each DB, to get DB space used and index statistics
	GRANT ADMINISTER BULK OPERATIONS TO [dbxtune]; -- On Linux: if you want to read the file '/var/opt/mssql/mssql.conf' when saving DBMS Configuration
	## To let the user read the errorlog (via SQL)
	use master;
	CREATE USER [dbxtune] FOR LOGIN [dbxtune];
	GRANT EXEC ON xp_readerrorlog TO [dbxtune];
	
	## Note: If you want to inspect 'Job Scheduler', dbxtune needs access to database 'msdb' 
	USE msdb;
	CREATE USER dbxtune FOR LOGIN dbxtune;
	ALTER ROLE db_datareader ADD MEMBER dbxtune;

	## and sp_Blitz* (if you havn't got them installed, now would be a good time: https://www.brentozar.com/blitz/ )
	## Note: If you want to execute 'sp_blitz' and are using a non 'sysadmin' account (like we do above with the login 'dbxtune')
	## you need to follow 'https://www.brentozar.com/askbrent/', look for 'How to Grant Permissions to Non-DBAs'
	## The 'sp_blitz' may be used to do all sorts of 'healthchecks' every time you connect to the monitored server.
	use master;
	GRANT EXEC ON sp_Blitz      TO [dbxtune];  -- Used for quick health check
	GRANT EXEC ON sp_BlitzLock  TO [dbxtune];  -- Used to list deadlocks (for Daily Summary Reports, if we have had deadlocks during that day)
	GRANT EXEC ON sp_BlitzIndex TO [dbxtune];  -- [not used by dbxtune yet] -- To get some Index Information
	GRANT EXEC ON sp_BlitzCache TO [dbxtune];  -- [not used by dbxtune yet] -- To get info from the Plan Cache
	GRANT EXEC ON sp_BlitzFirst TO [dbxtune];  -- [not used by dbxtune yet] -- To ...

	## Test that we can login to SQL Server with the 'dbxtune' user
	sqlcmd -Ssrvname -Udbxtune -Pthe_long_and_arbitrary_password

##--------------------------------------
## For SQL Server on WINDOWS: Preparations to monitor OS, Step 1 -- RECOMENDED
##     - create a specific OS user on host that runs SQL Server, which is used to monitor the OS system with 
##--------------------------------------

	## Install a SSH Server
	- Install instructions for OpenSSH on Windows Server 2019 & Windows 10:
	- https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse
	- OR get MSI package from https://github.com/PowerShell/Win32-OpenSSH/releases (pick the latest release)

	## Add Local or Active Directory Account
	net user /add dbxtune long_and_arbitrary_password

	## Allow user to get perf counters by issuing 'typeperf ...'
	net localgroup "Performance Log Users" dbxtune /add
	
	## Allow user to get "disk space used" on local drives, by issuing 'powershell gwmi win32_logicaldisk' over the SSH Connection
	net localgroup administrators dbxtune /add
	# Instead of the 'administrator' thing above, you can do the following:
	#   WMI Needs "Remote Enable" for the user to allow 'gwmi win32_logicaldisk', and SSH is considered to be a "Remote Operation"
	#   Look at https://github.com/microsoft/vscode-remote-release/issues/2648#issuecomment-1646047396
	#   That worked for me... Then the command didn't need 'administrator' role...
	#   NOTE: There might be other solutions as well, let me know your best solution for this 
	#         (Hopefully "someone" can provide a command line instructions instead of GUI clicking)


##--------------------------------------
## describe what SQL-Servers that should be monitored
##
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
		# I added the following entries (which is hosted on Linux)
		gorans-ub2-ss ; 1 ; SQL-Server on Linux ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

		# On windows it might look like (hopefully you dont have to specify '\instancename' because backslashes in names are evil)
		winHostName\instName  ; 1 ; SQL-Server on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

##--------------------------------------
## Add passwords for the servers (both DBMS user and OS/SSH user)
## (Note: for SQL-Server on Windows we can currently NOT monitor the OS)
##
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]

		## Below is an example of how I did it
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret2 -Sgorans-ub2-ss

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
##     - osUser=dbxtune
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
./start_sqlservertune.sh gorans-ub2-ss

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
##    - check: more ${HOME}/.dbxtune/dbxc/log/gorans-ub2-ss.console
##    - check: more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console
##
refresh the browser... or open a new tab: http://dbxtune.acme.com:8080/






########################################
## step 3: add Postgres (or MySql, or any other implementation)
########################################

##--------------------------------------
## Preparations of the Postgres DBMS, Step 1 -- RECOMENDED
##     - create a specific user in Postgres, which is used to monitor the system with 
##--------------------------------------

	## Create a random password:
	dbxtunePasswd=$(cat /dev/urandom | tr -cd '[:alnum:]' | fold -w24 | head -n1); echo ${dbxtunePasswd}
	
	## as the postgres user (or any other admin account)
	psql 
	CREATE USER dbxtune WITH PASSWORD 'the-long-and-arbitrary-password';
	GRANT pg_monitor TO dbxtune;
	GRANT pg_read_all_data TO dbxtune;
	-- ALTER USER dbxtune WITH SUPERUSER; -- This is probably NOT needed, but it can be used as a temporary workaround
	
	## Test that we can login to Postgres with the 'dbxtune' user
	psql --username dbxtune --host $(hostname) --port 5432 


##--------------------------------------
## Preparations of the Postgres DBMS, Step 2 -- RECOMENDED
##     - Make sure that the extension 'pg_stat_statements' are enabled 
## Note: search the internet for 'pg_stat_statements configuration' and you will find plenty of results
##--------------------------------------
	psql --username dbxtune --host $(hostname) --port 5432 
	select * from pg_stat_statements;
	## If the above works, no need to continue!
	
	## If 'pg_stat_statements' is NOT enabled... Lets enable it:
	
	## Check current config
	psql 
	select name, setting, sourcefile from pg_settings where name = 'shared_preload_libraries'
	
	## Change the configuration file (column 'sourcefile' from above SQL)
	vi ...postgresInstallDir.../postgres.conf
	shared_preload_libraries = 'pg_stat_statements'
	#-- Other pg_stat_statistics config parameters can possibly be found here: https://docs.yugabyte.com/preview/explore/query-1-performance/pg-stat-statements/
	
	## Restart Postgres
	pg_ctl restart
	
	## as the postgres admin user
	psql 
	CREATE EXTENSION pg_stat_statements;
	
	## Now test with dbxtune user
	psql --username dbxtune --host $(hostname) --port 5432 
	select * from pg_stat_statements;

##--------------------------------------
## Preparations of the Postgres DBMS, Step 3 -- RECOMENDED
##     - If you want to track "what" the server/pid's are Waiting on, we need another extention 
##     - Make sure that the extension 'pg_wait_sampling' are enabled 
## Note: search the internet for 'pg_wait_sampling configuration' and you will find some results
##--------------------------------------
	psql --username dbxtune --host $(hostname) --port 5432 
	select * from pg_wait_sampling_profile;
	## If the above works, no need to continue!

	## Check current config
	psql 
	select name, setting, sourcefile from pg_settings where name = 'shared_preload_libraries'
	## 'pg_wait_sampling' should be in there
	## If not fix the config (edit config file or)
	## ALTER SYSTEM SET shared_preload_libraries TO 'pg_stat_statements, pg_wait_sampling'
	## Then restart Postgres

	## as the postgres admin user
	psql 
	CREATE EXTENSION pg_wait_sampling;
	
	## Now test with dbxtune user
	psql --username dbxtune --host $(hostname) --port 5432 
	select * from pg_wait_sampling_profile;


##--------------------------------------
## Preparations of the Postgres DBMS, Step 4 -- RECOMENDED
##     - If you want to monitor the error log (needs Version 15) 
##     - And JSON Logging https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/ 
##--------------------------------------
	## We need access to some system functions from user 'dbxtune'
	GRANT EXECUTE ON FUNCTION pg_current_logfile()     TO dbxtune
	GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO dbxtune
	GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO dbxtune



##--------------------------------------
## describe what SQL-Servers that should be monitored
##
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
		# I added the following entries (which is hosted on Linux)
		gorans-ub2-pg ; 1 ; Postgress 9.5 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>

##--------------------------------------
## Add passwords for the servers (both DBMS user and OS/SSH user)
## (Note: for SQL-Server on Windows you need to install a SSH server, and possibly create a local user)
## Note: For the SSH User: If you want to use SSH private/public key, this can be done, in the 'postgres.GENERIC.conf' put 'conn.sshKeyFile=/.../.ssh/id_rsa' or use cmdline switch '-k /.../.ssh/id_rsa' in 'start_postgrestune.sh'
##       Also verify that you can do ssh using that key, using: ssh username@hostname -i /.../.ssh/id_rsa
##       Or: /dbxtune-install-dir/bin/dbxtune.sh sshtest ## You will have to create a input file... a template is printed if not specified 
##
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<dbms_user> -P<passwd> -S <dbms_srv_name>

		## Below is an example of how I did it
		dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Udbxtune -Psecret3 -Sgorans-ub2-pg

##--------------------------------------
## Edit NO-GUI Configuration files
##     - add your counters between: BEGIN/END: Performance Counter Settings
##     - set 'offline.sampleTime=##' to the sample interval you want
##
cd ${HOME}/.dbxtune/dbxc/conf
vi postgres.GENERIC.conf

##--------------------------------------
## Edit NO-GUI START files, possibly change
##     - dbmsUser=dbxtune
##     - osUser=gorans
##
cd ${HOME}/.dbxtune/dbxc/bin
vi start_postgrestune.sh

##--------------------------------------
## TEST start, to see that it works as expected
## If it comes up without errors, do Ctrl-C to stop it
## Hint 1, you should see something like: ... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.
## Hint 2, If it's 'Login problems', and you suspect it's the wrong password, edit 'start_postgrestune.sh' and uncomment row: export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"
##         Then look for '### DEBUG ###' in the output
##
cd ${HOME}/.dbxtune/dbxc/bin
./start_postgrestune.sh gorans-ub2-pg

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
##    - check: more ${HOME}/.dbxtune/dbxc/log/gorans-ub2-pg.console
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
##   - Click any of the "server" buttons and choose an action you want to do with this server
##     * Possibly: 'Show System Selected Graphs' or 'Show ALL Available Graphs' -- to view graphs
##     * or 'Tail Collector Console File' if you want to see the log file of this collector... 
##     * or 'Open Latest Daily Summary Report'. Note: This file is created at midnight every day.
##
##   - If you want to create a "specialized" profile, click: "New Profile..." 
##     * type the server and graph name you want to add
##     * continue with the above until you have a "set" of graphs you want to have
##     * Press "Save as new Profile...", give it a proper name, and description, then press "Save"
##     * Your profile will show up as a button at the "Profiles" section
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
##   Here you can do some administration...
##   - Remove Any off the below servers (NOTE: stop collector before doing this)
##   - Disable Any off the below servers... (it will not show up in the server list)
##   - Enable Any off the below servers... (below list has been disabled)
##   - Change profiles... what graphs should be visible in the System and User profiles.
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


