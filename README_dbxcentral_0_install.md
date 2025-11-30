# Installation instructions for DbxCentral

**NOTE: Early checkin -- Progress in work**   

DbxCentral is the Central Web Console to view Trend graphs and other information.

**NOTE**: Right now, Windows isn't *really* supported to **host** DbxCentral...   
Which means there are no start scripts etc...   
Let me know if running DbxCentral on Windows is a deal breaker for you :)   
_it might work with WSL - Windows Subsysten for Linux but I hav't tested..._    

However I do work on a _starter service_ that will start all collectors etc (so stay tuned for that)

**For a big picture look at the file**: https://github.com/goranschwarz/DbxTune?tab=readme-ov-file#overview

## Overview of the installation:
 * Create a new Virtual machine that will host DbxCentral and all DbxTune collectors    
   The machine can be called anything but `dbxtune.acme.com` is a good name.
 * Create a user on the above machine (`dbxtune` is a good name)
 * Connect to the above machine
 * Download the software
 * Initialize the software (create appdir)
 * Start DbxCentral
 * Add any DbxTune collectors, by following any of the below files.
   - `Sybase`     https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_1_add_sybase.md
   - `SQL Server` https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_2_add_sql-server.md
   - `Postgres`   https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_3_add_postgres.md
 

## Initial work:
* Make a connection to the host you are going to install on (for the moment Linux/Unix is only tested/supported)   
`ssh username@dbxtune.acme.com`   
You can use any Unix/Linux user, but I recommend creating a separate user for the purpose, for example `dbxtune`    

  > **NOTE**: If the install is on **Windows** the user needs to have role `Create symbolic links`   
  > This can be done using the following commands
  > ```
  > secpol.msc
  >   - goto: "Local Policies" -> "User Rights Assigments"
  >   - open: "Create symbolic links"
  >   - add: dbxtune
  > gpupdate /force
  > ## To check if the dbxtune user can create symbolic links:
  >   - open:  runas /user:dbxtune2 cmd
  >   - check: whoami /priv | findstr SeCreateSymbolicLinkPrivilege
  >   - test:  mklink nameOfLink physicalFileNameToLinkTo
  >   ## If no luck, try reboot Windows
  > ```



## Download and install the binaries 
* A good place to install the software is: `${HOME}/dbxtune/`
* You get the software from: https://sourceforge.net/projects/asetune/files/   
  The downloaded file are named: dbxtune_YYYY-MM-DD.zip


### Example of a download, and Install (Linux)
```
timestamp="2022-12-08"  ## the downloaded file are named: dbxtune_YYYY-MM-DD.zip
cd ${HOME}
mkdir ${HOME}/dbxtune
cd ${HOME}/dbxtune

## Put the downloaded ZIP file user ${HOME}/dbxtune
## or download it again: (note: file name will soon change from 'asetune_YYYY-MM-DD.zip' to 'dbxtune_YYYY-MM-DD.zip')
wget https://sourceforge.net/projects/asetune/files/asetune_${timestamp}.zip/download -O dbxtune_${timestamp}.zip

mkdir dbxtune_${timestamp}
ln -s dbxtune_${timestamp} 0    ## creates a "pointer" to the latest working release
cd 0
unzip ../dbxtune_${timestamp}.zip
chmod 755 bin/*.sh
cd ..
```

### Example of a download, and Install (Windows)
```
set timestamp="2022-12-08"  ## the downloaded file are named: dbxtune_YYYY-MM-DD.zip
cd c:\Users\dbxtune
mkdir dbxtune
cd dbxtune

## Put the downloaded ZIP file user c:\Users\dbxtune\dbxtune
## or download it again: (note: file name will soon change from 'asetune_YYYY-MM-DD.zip' to 'dbxtune_YYYY-MM-DD.zip')
curl -L https://sourceforge.net/projects/asetune/files/asetune_%timestamp%.zip/download -o dbxtune_%timestamp%.zip

mkdir dbxtune_%timestamp%
cd dbxtune_%timestamp%
tar -xf ..\dbxtune_%timestamp%.zip
cd ..
mklink /D 0 dbxtune_%timestamp%    ## creates a "pointer" to the latest working release
```

## Some extra info about the DbxTune directory structures...
DbxTune/DbxCentral consists of 2 distinct directory locations
 1. The binary software, which resides under `${HOME}/dbxtune/0`    
    (where 0 is a soft link to the latest sw release)
 2. User configurations, log files, customized start files, etc...   
    Are located at `${HOME}/.dbxtune`

Splitting the "software" and "user configuration" into two distinct locations makes it easier 
to "switch" between different DbxTune versions, or upgrade/install new versions.


## Create "appdir", that holds: various Data and Configurations
The below command creates directory `${HOME}/.dbxtune/` and copies some files to it

```
cd ${HOME}/dbxtune
./0/bin/dbxcentral.sh --createAppDir
```
Below is output from when I did it   
**Note**: this example was done with a zip file: dbxtune_2018-06-28.zip    
*(which is an earlier version than you are installing, so there might be some differences in the output)*   
```
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
```

## Make some enhancements to the install
To allow DbxTune to use more storage space, change `${HOME}/.dbxtune/dbxc/data` to a filesystem with **MUCH** more space   
A good idea is 300MB up to 1TB (or larger)   

The detailed Database Recordings, for each collector are stored under `${HOME}/.dbxtune/dbxc/data`.
At midnight DbxCentral will estimate how much space is needed for the next 24 hours.
If it finds that we starts to run out of space, it will delete the oldest recordings...
So how much detailed history that is available is based on storage space!

- To *link* the `data` and `report` directory to another location, do the below:
	```
	cd ${HOME}/.dbxtune/dbxc

	rmdir data
	ln -s /path/with/much/space/dbxtune/data data
	ls -Fal data

	rmdir reports
	ln -s /path/with/much/space/dbxtune/reports reports
	ls -Fal reports
	```


### If you want DbxCentral to respond on port 80 instead of 8080
1. Done with the Linux firewall
	```
	firewall-cmd --add-forward-port=port=80:proto=tcp:toport=8080
	firewall-cmd --list-forward-ports
	firewall-cmd --runtime-to-permanent
	```

2. Using `iptables` to redirect port 80 to 8080, as **root** do:    
`/sbin/iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080`

	Note `iptables` methos is NOT persisted, and is cleared on machine reboot.    
	Possibly look at: https://www.systutorials.com/how-to-make-iptables-ip6tables-configurations-permanent-across-reboot-on-centos-7-linux/       
	or similar pages...



## Start DbxCentral
- This is done with a shell script `start_dbxcentral.sh` found at `${HOME}/.dbxtune/dbxc/bin`

	Example:
	```
	cd ${HOME}/.dbxtune/dbxc/bin
	./start_dbxcentral.sh
	```

	Output:
	```
	FIXME
	```


## Now lets check if we can open the Web Page of DbxCentral
Web-browser(on your pc): `http://<dbxtune-host>:8080/`   
For example: http://dbxtune.acme.com:8080/

if it **doesn't** work:
 - Check: more `${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console`
 - Check: linux firewall, so that it allows incoming on port `8080`



# Next step
Next step is to add any DbxTune collectors

`Sybase ASE` - https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_1_add_sybase.md    
`SQL Server` - https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_2_add_sql-server.md    
`Postgres`   - https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_3_add_postgres.md    










# Short info about the WEB Interface

## Start Page:
 - Each Server will show up as a _button_ with the name of the server.
   - Buttons will be "red" if the server has any ACTIVE Alarms   
   - And the alarms will be visible at the bottom of the page...

- Click any of the "server" buttons and choose an action you want to do with this server
  * Possibly: `Show System Selected Graphs` or `Show ALL Available Graphs` to view graphs
  * Or `Tail Collector Console File` if you want to see the log file of this collector... 
  * Or `Open Latest Daily Summary Report`. Note: This file is created at midnight every day.

- If you want to create a "specialized" profile, click: `New Profile...` 
  * Type the server and graph name you want to add
  * Continue with the above until you have a _set_ of graphs you want to have
  * Press `Save as new Profile...`, give it a proper name, and description, then press `Save`
  * Your profile will show up as a button at the `Profiles` section

## Server Page: http://dbxtune.acme.com:8080/overview
Here you will find various information like:
 - Active Recordings
 - Active Alarms
 - All file(s) in the LOG Directory
 - All file(s) in the CONF Directory
 - Dbx Central database Iinformation
 - Available offline/recordings databases
 - Active Recordings, full meta-data file content

## Admin Page: http://dbxtune.acme.com:8080/admin/admin.html
Here you can do some administration...   
It needs you to login, _if you havn't set a password, it will be the IP Address on the hostname_
 - Remove Any off the below servers (NOTE: stop collector before doing this)
 - Disable Any off the below servers... (it will not show up in the server list)
 - Enable Any off the below servers... (below list has been disabled)
 - Change profiles... what graphs should be visible in the System and User profiles.
 - Add a server NOTE: Not yet implemented
 - Restart DbxCentral web server

## Desktop App Page: http://dbxtune.acme.com:8080/desktop_app.html
If any of the WEB Users wants to view the *detailed* recordings (connect to a recording database)
they will need the *NATIVE* application, so here they can download it...
and some quick install instructions.

## API - there are some available API's... 
http://dbxtune.acme.com:8080/api/index.html will show you some short info...
