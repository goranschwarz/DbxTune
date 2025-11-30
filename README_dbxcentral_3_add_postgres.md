# Add Postgres to DbxCentral

**NOTE: Early checkin -- Progress in work**   

Pre requirement: That you have installed DbxCentral: https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_install.md

## Overview of the steps
- Prepare, step 1: **RECOMENDED** - Create a dedicated DBMS User for monitoring
- Prepare, step 2: **RECOMENDED** - Create a dedicated OS User for monitoring
- Prepare, step 3: **RECOMENDED** - pg_stat_statements
- Prepare, step 4: **RECOMENDED** - pg_wait_sampling
- Prepare, step 5: **RECOMENDED** - Postgres errorlog
- Add Meta data to DbxCentral (add server)
- Add User and Password information (to an encrypted file)
- Add configuration file (what we want to monitor)
- Add/Edit start file for PostgresTune
- Test start PostgresTune to verify that it can start
- Start up PostgresTune in the *normal* way
- Verify that data is sent to DbxCentral

## Prepare, step 1: **RECOMENDED** - Create a dedicated DBMS User for monitoring
Create a specific user in Postgres, which is used to monitor the system with 

#### Create a secure/random password
  - Use any online password generator: https://www.google.com/search?q=generate+password+online
  - Or you can do it in Linux:
	```
	## Create a random password, in Linux you could do:
	dbxtunePasswd=$(cat /dev/urandom | tr -cd '[:alnum:]' | fold -w24 | head -n1); echo ${dbxtunePasswd}
	```

#### Add the login to Postgres
```
## as the postgres user (or any other admin account)
psql 
CREATE USER dbxtune WITH PASSWORD 'the-long-and-arbitrary-password';
GRANT pg_monitor TO dbxtune;
GRANT pg_read_all_data TO dbxtune;
-- ALTER USER dbxtune WITH SUPERUSER; -- This is probably NOT needed, but it can be used as a temporary workaround

## Test that we can login to Postgres with the 'dbxtune' user
psql --username dbxtune --host $(hostname) --port 5432 
```


## Prepare, step 2: **RECOMENDED** - Create a dedicated OS User for monitoring
Create a specific OS User on the host/OS where Postgres DBMS is running, which is used to monitor the system with.    

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

#### On the host where the Postgres DBMS is running, add a user:
```
sudo adduser dbxtune
```

#### Test that we can login via `ssh` to the host where DBMS Server is running
```
ssh dbxtune@dbms-hostname
## Then try to execute some of the above commands
```




## - Prepare, step 3: **RECOMENDED** - pg_stat_statements
Make sure that the extension `pg_stat_statements` are enabled    
Note: search the internet for 'pg_stat_statements configuration' and you will find plenty of results
```
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

## Or change the config via:
ALTER SYSTEM SET shared_preload_libraries TO 'pg_stat_statements'


## Restart Postgres
pg_ctl restart

## as the postgres admin user
psql 
CREATE EXTENSION pg_stat_statements;

## Now test with dbxtune user
psql --username dbxtune --host $(hostname) --port 5432 
select * from pg_stat_statements;
```

## Prepare, step 4: **RECOMENDED** - pg_wait_sampling
If you want to track "what" the server/pid's are Waiting on, we need another extention     
Make sure that the extension `pg_wait_sampling` are enabled    
Note: search the internet for 'pg_wait_sampling configuration' and you will find some results   
```
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
```


## Prepare, step 5: **RECOMENDED** - Postgres errorlog
If you want to monitor the error log (needs Version 15)    
And JSON Logging https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/ 
```
## We need access to some system functions from user 'dbxtune'
GRANT EXECUTE ON FUNCTION pg_current_logfile()     TO dbxtune
GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO dbxtune
GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO dbxtune
```



## Add Meta data to DbxCentral (add server)
```
cd ${HOME}/.dbxtune/dbxc/conf
vi SERVER_LIST
```
Example:
```
# I added the following entries (which is hosted on Linux)
gorans-ub2-pg ; 1 ; Postgress 12.7 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>
```


## Add passwords for the servers (both DBMS user and OS/SSH user)
Note: For the SSH User: If you want to use SSH private/public key, this can be done, in the `postgres.GENERIC.conf` put `conn.sshKeyFile=/.../.ssh/id_rsa` or use cmdline switch `-k /.../.ssh/id_rsa` in `start_postgrestune.sh`    
Also verify that you can do ssh using that key, using: `ssh username@hostname -i /.../.ssh/id_rsa`     
Or: `/dbxtune-install-dir/bin/dbxtune.sh sshtest` You will have to create a input file... a template is printed if not specified 
```
cd ${HOME}/.dbxtune/dbxc/bin
./dbxPassword.sh set -U<dbms_user> -P<passwd> -S <dbms_srv_name>
```
Example:
```
## Below is an example of how I did it
dbxtune@gorans-ub2:~/.dbxtune/dbxc/bin$ ./dbxPassword.sh set -Udbxtune -Psecret3 -Sgorans-ub2-pg
```


## Edit PostgresTune Configuration files
 - Add your counters between: `BEGIN/END: Performance Counter Settings`
 - Set `offline.sampleTime=##` to the sample interval you want
```
cd ${HOME}/.dbxtune/dbxc/conf
vi postgres.GENERIC.conf
```

## Or Create a new PostgresTune Configuration files
If you like to set exactly what is monitored or not, Lets create a new Config file from scratch!    
The is done with **PostgresTune** in **GUI mode** (or any of the dbxTune implementations)   
- Using a wizard: `Menu->Tools->Create 'Record Session - Template file' Wizard...`   
- Then copy the content to the `postgres.GENERIC.conf`
- Or to a specific servername file `postgres.<servername>.conf`	


## Edit PostgresTune START file
Possibly change
 - `dbmsUser=dbxtune`
 - `osUser=dbxtune`
 - `cfgFile=<filname>` -- `postgres.GENERIC.conf` to `postgres.<SERVERNAME>.conf`
```
cd ${HOME}/.dbxtune/dbxc/bin
vi start_postgrestune.sh
```


## TEST start, to see that it works as expected
- Watch the output and make sure it starts in a proper way   
- If it comes up without errors, do `Ctrl-C` to stop it    

**Hint 1**: You should see something like: `... Persistent Counter DB: Creating table 'CmSummary_abs' for CounterModel 'CmSummary'.`   

**Hint 2**: If it's _Login problems_, and you suspect it's the wrong password, edit `start_postgrestune.sh` and uncomment row: `export DBXTUNE_JVM_SWITCHES="-Dnogui.password.print=true"`    
Then look for `DEBUG` in the output    

Example:
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_postgrestune.sh gorans-ub2-pg
```

## Now start it in the _REAL_ way
Only collectors/servers that are NOT started will be started (which is described in `${HOME}/.dbxtune/dbxc/conf/SERVER_LIST`)
```
cd ${HOME}/.dbxtune/dbxc/bin
./start_ALL.sh 
```

## Now lets check that we can see data in DbxCentral
The new Postgres server should show up as an icon on the first page
if it doesn't work:
 - check: `more ${HOME}/.dbxtune/dbxc/log/gorans-ub2-pg.console`
 - check: `more ${HOME}/.dbxtune/dbxc/log/DBX_CENTRAL.console`

Refresh the browser...    
Or open a new tab: http://dbxtune.acme.com:8080/


