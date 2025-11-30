# DbxTune - How to add a new flavor of DbxTune

In the below example I created the Db2Tune

_Next time a new dbxVendor is created, **update** this page with new stuff and findings ..._    
_There are X number of new things/classes that we need to add in various places_




## Basic things

#### DBMS Installation
- DB2 install - I followed: https://www.toadworld.com/platforms/ibmdb2/b/weblog/archive/2017/08/11/how-to-install-ibm-db2-developer-edition-on-centos-7-using-docker

#### JDBC Driver
- Download and put in: `lib\jdbc_drivers`   
  It will automatically be picked up in the start scripts (classpath)

#### New Icons, that is needed to be created
- `com/dbxtune/images/db2tune_splash.jpg`                - Showned at GUI startup
- `com/dbxtune/images/db2tune_icon_16.png`               - application small icon
- `com/dbxtune/images/db2tune_icon_32.png`               - application bigger icon
- `com/dbxtune/images/tcp_group_icon_db2.png`            - icon at the "server" tab

#### Start files, create: (take a copy of one of the others and change some stuff)
- `db2tune.bat`
- `db2tune.sh`
	
- Edit: `dbxtune.bat`
	```
	) ELSE IF "%APP_NAME%" == "Db2Tune" (
		set JAVA_START_CLASS="com.dbxtune.Db2Tune"
		set SPLASH=-splash:lib/db2tune_splash.jpg
	```
- Edit: `dbxtune.sh`
	```
	db2)
		shortAppName="db2tune"
		longAppName="Db2Tune"
		javaMainClass="com.dbxtune.Db2Tune"
		javaMainParams=""
		javaSplashScreen="lib/db2tune_splash.jpg"
		;;
	```

#### Build scripts (build.xml)
```
<copy file="db2tune.bat"                                   toFile="${dist}/bin/db2tune.bat"/>
<copy file="db2tune.sh"                                    toFile="${dist}/bin/db2tune.sh"/>
<copy file="${src}/com/dbxtune/images/db2tune_splash.jpg"  toFile="${dist}/lib/db2tune_splash.jpg"/>
```





## Java code

#### Change: `src/com/dbxtune/DbxTune.java`
- in the `main()`, add Db2Tune
	```
	else if ("Db2Tune".equalsIgnoreCase(_mainClassName)) _instance = new Db2Tune(cmd);
	```
 
#### Create: `src/com/dbxtune/Db2Tune.java`    
_(take a copy of MySqlTune.java for instance))_
- Change everything that says `MySql` to `Db2`
- This requires you to create X number of new files, that implements DB2 Specifics
	`src/com/dbxtune/check/CheckForUpdatesDb2.java`    
	`src/com/dbxtune/CounterControllerDb2.java`    
	`src/com/dbxtune/gui/MainFrameDb2.java`    
	`src/com/dbxtune/config/dict/MonTablesDictionaryDb2.java`    
	`src/com/dbxtune/config/dbms/Db2Config.java`    
	`src/com/dbxtune/config/dbms/Db2ConfigText.java`    

Note: From here on it's a little _hacky_ since we need to create a bunch of clases... that depends on other clases...
	
#### Create: `src/com/dbxtune/check/CheckForUpdatesDb2.java` 
_(take a copy from CheckForUpdatesMySql.java)_    
No changes needed (probably)
 
#### Create: `src/com/dbxtune/CounterControllerDb2`
- This holds CM's _CounterModel object, which is "collectors"_
- Initializes the CM's when we connect to a server
- Creates a "header" for CM's that will be stored in the PCS (Persistent Counter Storage - or save a recording)
- Specifies some behaviour and SQL queries used in the DBMS

#### Create: `src/com/dbxtune/config/dict/MonTablesDictionaryDb2.java` 
- Fill in the static helper test for table/columns if the DBMS do not have a dictionary/table which we can reads    
  If the DBMS have a internal dictionary, then look at AseTune how it's solved...

#### Create the first CM `CmSummary`
- Create package: `com/dbxtune/cm/db2` & `com/dbxtune/cm/db2/gui`
- Create: `com/dbxtune/cm/db2/CmSummary.java`    
  _(Warning `import`: don't point to the "wrong" com.dbxtune.cm.`package`.gui.CmSummaryPanel)_    
	- Change `getSqlForVersion()` to reflect what you want to get in the summary     
	  (view other CmSummary.java for inspiration) 
- Create: `com/dbxtune/cm/db2/gui/CmSummaryPanel.java`    
  _(Warning `import`: don't point to the "wrong" com.dbxtune.cm.`package`.CmSummary)_
	- Here is a *bunch* of things to do...    
	Add/Create a field for every column in the SQL statement and read the values into those fields

#### Create: `src/com/dbxtune/gui/MainFrameDb2.java`
- Change everithing `MySql` to `db2`

#### Optional _(if you want to save/view the DBMS configuration)_
- Create: `src/com/dbxtune/config/dbms/Db2Config.java`     
  This holds configuration (which is in a jTable so you can filter on it etc)
- Create: `src/com/dbxtune/config/dbms/Db2ConfigText.java`    
  This holds various other things which you want to know and store about the DBMS configuration

#### Optional _(if various components are DBMS Version dependant)_
- Create: `com.dbxtune.sql.conn.Db2Connection.java`
- Edit: `com.dbxtune.sql.conn.DbxConnection.java`
	- To handle Db2Connection and various other stuff...

#### Now start to add CmSomeName
- Add every Cm to `CounterControllerDb2.createCounters()`
- Make sure _output_ columns has proper datatypes...     
  Use `CAST(colName as datatype(length))` if you are unsure...    
  _(PCS - Persistent Counter Storage tables, will use the ResultSetMetaData to create tables)_
