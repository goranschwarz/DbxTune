# Add SQL Server to DbxCentral

**NOTE: Early checkin -- Progress in work**   

Pre requirement: That you have installed DbxCentral: https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_install.md

## Overview of the steps
- Prepare, step 1: **RECOMENDED** - Create a dedicated DBMS User for monitoring
- Prepare, step 2: **RECOMENDED** - Create a dedicated OS User for monitoring
- Add Meta data to DbxCentral (add server)
- Add User and Password information (to an encrypted file)
- Add configuration file (what we want to monitor)
- Add/Edit start file for SqlServerTune
- Test start SqlServerTune to verify that it can start
- Start up SqlServerTune in the *normal* way
- Verify that data is sent to DbxCentral


## Preparations of the SQL Server DBMS, Step 1 -- **RECOMENDED**
Create a specific user in SQL Server, which is used to monitor the system with 

#### Create a secure/random password
  - Use any online password generator: https://www.google.com/search?q=generate+password+online
  - Or you can do it in Linux:
	```
	## Create a random password, in Linux you could do:
	dbxtunePasswd=$(cat /dev/urandom | tr -cd '[:alnum:]' | fold -w24 | head -n1); echo ${dbxtunePasswd}
	```

#### Add the login to the SQL Server
```
-- as any SQL Server login with 'sysadmin' create the login 'dbxtune'
CREATE LOGIN dbxtune WITH PASSWORD         = 'the_long_and_arbitrary_password', 
                          DEFAULT_DATABASE = 'master', 
                          CHECK_POLICY     = OFF, 
                          CHECK_EXPIRATION = OFF;

-- Then grant authorization
GRANT VIEW SERVER STATE    TO dbxtune;   -- To view Server level statistics (like most DMV's)
GRANT VIEW ANY DEFINITION  TO dbxtune;   -- To be able to view Availability Groups, etc
GRANT CONNECT ANY DATABASE TO dbxtune;   -- To access DMV's in each DB, to get DB space used and index statistics

-- On Linux: if you want to read the file '/var/opt/mssql/mssql.conf' when saving DBMS Configuration in PCS
GRANT ADMINISTER BULK OPERATIONS TO dbxtune;
```

#### To let the user read the errorlog (via SQL)
```
use master;
CREATE USER dbxtune FOR LOGIN dbxtune;
GRANT EXEC ON xp_readerrorlog TO dbxtune;
```

#### If you want to inspect 'Job Scheduler', dbxtune needs access to database 'msdb' 
```
USE msdb;
CREATE USER dbxtune FOR LOGIN dbxtune;
ALTER ROLE db_datareader ADD MEMBER dbxtune;
```

#### And sp_Blitz* procedures   
(if you havn't got them installed, now would be a good time: https://www.brentozar.com/blitz/)    
- `sp_Blitz` Can be used to verify that we do not have any **major** conserns on the SQL Server.
- `sp_BlitzLock` Is used to extract Deadlock Details.

**Tip**: In the GUI Version of SqlServerTune, you can install the procedures from `Menu -> Tools -> Install some extra system stored procedures`

```
-- Note: If you want to execute 'sp_blitz' and are using a non 'sysadmin' account (like we do above with the login 'dbxtune')
-- you need to follow 'https://www.brentozar.com/askbrent/', look for 'How to Grant Permissions to Non-DBAs'
-- The 'sp_blitz' may be used to do all sorts of 'healthchecks' every time you connect to the monitored server.
use master;
GRANT EXEC ON sp_Blitz      TO dbxtune;  -- Used for quick health check (at SqlServerTune startup)
GRANT EXEC ON sp_BlitzLock  TO dbxtune;  -- Used to list deadlocks (for Daily Summary Reports, if we have had deadlocks during that day)

-- The blow are not used by SqlServerTune... But they are nice to have
GRANT EXEC ON sp_BlitzIndex TO dbxtune;  -- [not used by dbxtune yet] -- To get some Index Information
GRANT EXEC ON sp_BlitzCache TO dbxtune;  -- [not used by dbxtune yet] -- To get info from the Plan Cache
GRANT EXEC ON sp_BlitzFirst TO dbxtune;  -- [not used by dbxtune yet] -- To ...
```

#### Test that we can login to SQL Server with the `dbxtune` user
```
sqlcmd -Ssrvname -Udbxtune -Pthe_long_and_arbitrary_password
```


## For SQL Server on WINDOWS: Preparations to monitor OS, Step 1 -- **RECOMENDED**
To monitor the OS on Windows, SqlServerTune is using SSH (Secure Shell)   
The SSH Service may not be part of a normal Windows installation, so you may have to add it!

#### Install a SSH Server
- Install instructions for OpenSSH on Windows Server 2019 & Windows 10:   
  https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse
- OR get MSI package from https://github.com/PowerShell/Win32-OpenSSH/releases (pick the latest release)


#### Create a specific OS User on host that runs SQL Server, which is used to monitor the OS system with 
```
## Add Local (or Active Directory Account if the SSH Server supports it)
net user /add dbxtune long_and_arbitrary_password

## Allow user to get perf counters by issuing 'typeperf ...'
net localgroup "Performance Log Users" dbxtune /add

## I have *Switched* from 'gwmi win32_logicaldisk' to 'Get-CimInstance Win32_LogicalDisk', so the BELOW 'admin' stuff is hopefully not needed anymore... 
## Allow user to get "disk space used" on local drives, by issuing 'powershell gwmi win32_logicaldisk' over the SSH Connection
#net localgroup administrators dbxtune /add

## NOTE ## Instead of the 'administrator' thing above, you can do the following:
## - WMI Needs "Remote Enable" for the user to allow 'gwmi win32_logicaldisk', and SSH is considered to be a "Remote Operation"
##   Look at https://github.com/microsoft/vscode-remote-release/issues/2648#issuecomment-1646047396
##   That worked for me... Then the command didn't need 'administrator' role...
##   NOTE: There might be other solutions as well, let me know your best solution for this 
##         (Hopefully "someone" can provide a command line instructions instead of GUI clicking)
```

#### Some extra stuff you may need to add (if not admin)
Open `secpol.msc`    
Go to `Security Settings -> Local Policies -> User Rights Assignment`    
and add `Debug programs` which gives the user the policy `SeDebugPrivilege`, then the user will see all processes in the "CPU by Processes". Without `Debug programs` the user will only see it's own processes, which is kind of _useless_

#### What will be monitored by the above OS User
The user will extract various information from the OS, like:    
| Description        | Windows Command                                                   | Linux/Unix  Command | 
| ------------------ | ----------------------------------------------------------------- | ------------------- | 
| CPU Usage          | `typeperf -si # "\Processor(*)\*"`                      | `mpstat -P ALL`     | 
| Swapping etc       | see _Memory usage_                                                | `vmstat #`          | 
| Speed of the disks | `typeperf -si # "\PhysicalDisk(*)\*"`                   | `iostat -xdk #`     | 
| For load average   | `typeperf -si # "\System\*"`                              | `uptime`            | 
| Memory usage       | `typeperf -si # "\Paging File(_Total)\* \Memory\*"` | `cat /proc/meminfo` | 
| Network usage      | `typeperf -si # "\Network Interface(*)\*"`             | `cat /proc/net/dev` | 
| Disk space usage   | `powershell [System.IO.DriveInfo]::GetDrives()`       | `df -kP`            | 
| CPU by Processes   | `powershell Get-Process`                                     | `ps -e -ww`         | 

_(in above table: replace `#` with interval/seconds)_


#### Test that we can login via `ssh` to the host where SQL Server is running
```
ssh dbxtune@sql-server-hostname
## Then try to execute some of the above commands
```


## Add Meta data to DbxCentral (add server)
```
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
```
Example:
```
## I added the following entries (which is hosted on Linux)
gorans-ub2-ss ; 1 ; SQL-Server on Linux ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

## On windows it might look like (hopefully you dont have to specify '\instancename' because backslashes in names are evil)
winHostName\instName  ; 1 ; SQL-Server on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME>

## Here I use a "alias" name using '-A GS-1-WIN__SS_2016' to name the monitored instance...
gs-1-win:1433 ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016
```


## Add passwords for the servers (both DBMS user and OS/SSH user)
```
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]
```
Example:
```
## Below is an example of how I did it
dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Usa -Psecret2 -Sgorans-ub2-ss
```
#### If you don NOT want to use authentication via password
* For OS Monitoring (SSH) use SSH Keys
	```
	## On the host dbxtune, create a key
	ssh-keygen

	## Copy the key to the monitored host
	ssh-copy-id dbxtune@hostname_to_monitor.com
	```
* For SQL Server Connections
  * Possibly use `integratedSecurity=true` in the JDBC URL   
  * On Linux you need to use `integratedSecurity=true;authenticationScheme=JavaKerberos`    
    You would aalso need to Install and Configure Kerberos     
	Typically google for "linux setup kerberos for SQL Server authentication"


## Edit SqlServerTune Configuration files
 - Add your counters between: `BEGIN/END: Performance Counter Settings`    
   _Note: The provided template files might **NOT** contain all the "collectors" available, if so just add the `CmName` you want to add_
 - Set `offline.sampleTime=##` to the sample interval you want
```
cd ${HOME}/.dbxtune/dbxc/conf
vi sqlserver.GENERIC.conf
```

## Or Create a new SqlServerTune Configuration files
If you like to set exactly what is monitored or not, Lets create a new Config file from scratch!    
The is done with **SqlServerTune** in **GUI mode** (or any of the dbxTune implementations)   
- Using a wizard: `Menu -> Tools -> Create 'Record Session - Template file' Wizard...`   
- Then copy the content to the `sqlserver.GENERIC.conf`
- Or to a specific servername file `sqlserver.<SERVERNAME>.conf`	


## Edit SqlServerTune START file
Possibly change
 - `dbmsUser=dbxtune`
 - `osUser=dbxtune`
 - `cfgFile=<filname>` -- `sqlserver.GENERIC.conf` to `sqlserver.<SERVERNAME>.conf`
 - The start line depending on the SQL-Server is hosted on Windows or Linux (if we want the OS to be monitored)
```
cd ${HOME}/.dbxtune/dbxc/bin
vi start_sqlservertune.sh
```


## TEST start, to see that it works as expected
- Watch the output and make sure it starts in a proper way   
- If it comes up without errors, do `Ctrl-C` to stop it    

**Hint 1**: You should see something like: `... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.`   

**Hint 2**: If it's _Login problems_, and you suspect it's the wrong password, edit `start_asetune.sh` and uncomment row: `export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"`    
Then look for `DEBUG` in the output    

**Hint 3**: If it's on Windows and the `@@servername` is `hostName\instanceName`, then the below test command should be: `./start_sqlservertune.sh hostName\\instanceName`    
The double backslash is just to "escape" that it's a backslash    
BUT the `${HOME}/.dbxtune/dbxc/conf/SERVER_LIST` should only have a single backslash `hostName\instanceName`

Example:
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_sqlservertune.sh gorans-ub2-ss
```


## Now start it in the _REAL_ way
Only collectors/servers that are NOT started will be started (which is described in ${HOME}/.dbxtune/dbxc/conf/SERVER_LIST)
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_ALL.sh 
```


## Now lets check that we can see data in DbxCentral
The new SQL-Server should show up as an icon on the first page
if it doesn't work:
 - check: `more ${HOME}/.dbxtune/dbxc/log/gorans-ub2-ss.console`
 - check: `more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console`

Refresh the browser...    
Or open a new tab: http://dbxtune.acme.com:8080/



