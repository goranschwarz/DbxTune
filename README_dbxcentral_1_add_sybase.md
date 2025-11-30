# Add Sybase ASE to DbxCentral

**NOTE: Early checkin -- Progress in work**   

Pre requirement: That you have installed DbxCentral: https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_install.md

## Overview of the steps
- Prepare, step 1: - Add server(s) to the interfaces file
- Prepare, step 2: - Configure Sybase ASE for monitoring
- Prepare, step 3: **RECOMENDED** - Create a dedicated DBMS User for monitoring
- Prepare, step 4: **RECOMENDED** - Create a dedicated OS User for monitoring
- Add Meta data to DbxCentral (add server)
- Add User and Password information (to an encrypted file)
- Add configuration file (what we want to monitor)
- Add/Edit start file for AseTune
- Test start AseTune to verify that it can start
- Start up AseTune in the *normal* way
- Verify that data is sent to DbxCentral




## Prepare, step 1: - Add server(s) to the interfaces file
Add ASE servers to the interfaces file, or make a link to the real interfaces file   
_Note: This step is also done for other Sybase Products like: RepServer, IQ, RepAgentX_

This can be done in several ways:
  - Add entries manually by `printf`
  - Or: add entries manually by `vi`
  - Or: create a softlink (to already existing interfaces)

**NOTE 1**: The servernames in the intefaces file **SHOULD** be the same as the servers `@@servername`    
  **This is IMPORTANT**

Examples
```
cd ${HOME}/.dbxtune/
printf "\nASE_NAME\n\tquery tcp ether HOSTNAME PORT\n\n" >> ${HOME}/.dbxtune/interfaces

#vi interfaces

## To link the interfces to and existsing file
#rm interfaces
#ln -s /opt/sybase/interfaces interfaces
```


## Prepare, step 2: Configure Sybase ASE for monitoring
This is probably already done on your system, but just in case...

#### Using AseTune GUI Tool
 - Login as `sa` _or any other login with "admin" authorization_
 - `Menu -> Tools -> Configure ASE For Monitoring...`
 - Choose what to configure...    
   _you may choose from the dropdown `Configuration Templates`_
 - Press `OK`
 
#### Manually, with `isql`
Note: The below configuration is just an example, and other monitoring configurations can be used...
```
isql -Usa -Pxxx -Sxxxx 
exec sp_configure 'enable monitoring',              1
exec sp_configure 'per object statistics active,    1
exec sp_configure 'statement statistics active,     1
exec sp_configure 'enable spinlock monitoring,      1
exec sp_configure 'execution time monitoring,       1
exec sp_configure 'enable stmt cache monitoring,    1
exec sp_configure 'capture compression statistics,  1
exec sp_configure 'object lockwait timing,          1
exec sp_configure 'process wait events,             1
exec sp_configure 'SQL batch capture,               1
exec sp_configure 'show deferred compilation text', 0
exec sp_configure 'wait event timing,               1

exec sp_configure 'lock timeout pipe active',       1
exec sp_configure 'deadlock pipe active,            1
exec sp_configure 'errorlog pipe active,            1
exec sp_configure 'threshold event monitoring,      1
exec sp_configure 'sql text pipe active,            1
exec sp_configure 'statement pipe active,           1
exec sp_configure 'plan text pipe active,           0
exec sp_configure 'nonpushdown pipe active,         0

exec sp_configure 'lock timeout pipe max messages,  500
exec sp_configure 'deadlock pipe max messages,      500
exec sp_configure 'errorlog pipe max messages,      500
exec sp_configure 'threshold event max messages,    500
exec sp_configure 'sql text pipe max messages,      1000
exec sp_configure 'statement pipe max messages,     10000
exec sp_configure 'plan text pipe max messages,     0
exec sp_configure 'nonpushdown pipe max messages,   0
exec sp_configure 'max SQL text monitored,          2048

-- To read raw ASE Config file(s)
exec sp_configure 'enable file access,              1
go

```


## Prepare, step 3: **RECOMENDED** - Create a dedicated DBMS User for monitoring
Create a specific user in Sybase ASE, which is used to monitor the system with    
_or you may simply use the `sa` login, if you are "brave" and prefer that_

#### Create a secure/random password
  - Use any online password generator: https://www.google.com/search?q=generate+password+online
  - Or you can do it in Linux:
	```
	## Create a random password, in Linux you could do:
	dbxtunePasswd=$(cat /dev/urandom | tr -cd '[:alnum:]' | fold -w24 | head -n1); echo ${dbxtunePasswd}
	```

#### Add the login to Sybase ASE
```
## as the "admin" user
isql -Usa -Pxxx -Sxxxx 
CREATE LOGIN dbxtune WITH PASSWORD 'the-long-and-arbitrary-password' 
go
sp_role 'grant', 'mon_role', 'dbxtune'
go
-- Or simply:
-- sp_role 'grant', 'sa_role', 'dbxtune'
go

## Test that we can login to Sybase ASE with the 'dbxtune' user
isql -Udbxtune -Pxxx -Sxxx  
```

## Prepare, step 4: **RECOMENDED** - Create a dedicated OS User for monitoring
Create a specific OS User on the host/OS where Sybase ASE DBMS is running, which is used to monitor the system with.    
_Or you can simply use the `sybase` OS User..._

#### What will be monitored by the above OS User
| Description        | Linux/Unix  Command | 
| ------------------ | ------------------- | 
| CPU Usage          | `mpstat -P ALL`     | 
| Swapping etc       | `vmstat #`          | 
| Speed of the disks | `iostat -xdk #`     | 
| For load average   | `uptime`            | 
| Memory usage       | `cat /proc/meminfo` | 
| Network usage      | `cat /proc/net/dev` | 
| Disk space usage   | `df -kP`            | 
| CPU by Processes   | `ps -e -ww`         | 

_(in above table: replace `#` with interval/seconds)_

#### On the host where the Sybase DBMS is running, add a user:
```
sudo adduser dbxtune
```

#### Test that we can login via `ssh` to the host where DBMS Server is running
```
ssh dbxtune@dbms-hostname
## Then try to execute some of the above commands
```


## Add Meta data to DbxCentral (add server)
What collectors should be managed, started/stopped with:
 * `~/.dbxtune/dbxc/bin/start_ALL.sh` and `~/.dbxtune/dbxc/bin/stop_ALL.sh`    
Hopefully the file is "self explainable"

Below is an Example:
```
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
```
I added the following entries
```
GORANS_UB1_DS      ; 1 ; Test 1 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
GORAN_UB2_DS       ; 1 ; Test 2 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
GORAN_UB3_DS       ; 1 ; Test 3 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
```



## Add passwords for the servers (both DBMS user and OS/SSH user)
Create passwords in file `$HOME/.passwd.enc`

The DBMS username and OS username we are using when connecting to ASE
Note: you only need to specify `-S srvName` if you have the same username with *different* passwords to different server names...    
So if you have same password for user *xxx* on all servers, you do not need to specify the `-S` flag.   
The logic is: if we can't find the servername in the password file we will "fallback" to an "generic" username entry (which has no server-name specification)
```
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<os_user>   -P<passwd> [-S <os_host_name>]
./dbxPassword.sh set -U<dbms_user> -P<passwd> [-S <dbms_srv_name>]
```

Below is an example of how I did it
```
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
```



## Edit AseTune Configuration files
What to be monitored is stored in a configuration file.
 - Add your counters between: `BEGIN/END: Performance Counter Settings`
 - Set `offline.sampleTime=##` to the sample interval you want
 - Note: The sample intervall could also be overidden/specified `-i,--interval <seconds>` in `start_xxxtune.sh` or in the SERVER_LIST (as a parameter to `start_xxxtune.sh`)   

```
cd ${HOME}/.dbxtune/dbxc/conf
vi ase.GENERIC.conf
```

## Or Create a new AseTune Configuration files
If you like to set exactly what is monitored or not, Lets create a new Config file from scratch!    
The is done with **AseTune** in **GUI mode** (or any of the dbxTune implementations)   
- Using a wizard: `Menu -> Tools -> Create 'Record Session - Template file' Wizard...`   
- Then copy the content to the `ase.GENERIC.conf`
- Or to a specific servername file `ase.GORAN_UB3_DS.conf`	



## Edit AseTune START file
Possibly change
 - `dbmsUser=dbxtune`
 - `osUser=dbxtune`
 - `cfgFile=<filename>` -- `ase.GENERIC.conf` to `ase.GORAN_UB3_DS.conf`

Example:
```
cd ${HOME}/.dbxtune/dbxc/bin
vi start_asetune.sh
```


## TEST start, to see that it works as expected
- Watch the output and make sure it starts in a proper way   
- If it comes up without errors, do `Ctrl-C` to stop it    

**Hint 1**: You should see something like: `... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.`   

**Hint 2**: If it's _Login problems_, and you suspect it's the wrong password, edit `start_asetune.sh` and uncomment row: `export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"`    
Then look for `DEBUG` in the output    

Example:
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_asetune.sh GORAN_UB3_DS
```


## Now start it in the _REAL_ way
Only collectors/servers that are NOT started will be started (which is described in `${HOME}/.dbxtune/dbxc/conf/SERVER_LIST`)
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_ALL.sh 
```

Below is an example of how I did it   
Which starts DbxCentral and 3 ASE collectors
```		
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
```


## List the collectors and DbxCentral
```
cd ${HOME}/.dbxtune/dbxc/bin
./list_ALL.sh 
```


## Now lets check that we can see data in DbxCentral
The new Sybase ASE server should show up as an icon on the first page
if it doesn't work:
 - check: `more ${HOME}/.dbxtune/dbxc/log/<servername>.console`
 - check: `more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console`

Refresh the browser...    
Or open a new tab: http://dbxtune.acme.com:8080/


