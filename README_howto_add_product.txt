==============================================================================
 DbxTune - How to add a new flavor of DbxTune
 (in the below example I created the Db2Tune)
==============================================================================

##############################################################
# Basic things
##############################################################

 * DB2 install - I followed: https://www.toadworld.com/platforms/ibmdb2/b/weblog/archive/2017/08/11/how-to-install-ibm-db2-developer-edition-on-centos-7-using-docker

 * JDBC Driver
	- Download and put in: lib\jdbc_drivers
	  It will automatically be picked up in the start scripts (classpath)

 * Icons, that is needed to be created
	- com/dbxtune/images/db2tune_splash.jpg                - Showned at GUI startup
	- com/dbxtune/images/db2tune_icon_16.png               - application small icon
	- com/dbxtune/images/db2tune_icon_32.png               - application bigger icon
	- com/dbxtune/images/tcp_group_icon_db2.png            - icon at the "server" tab

 * Start files, create: (take a copy of one of the others and change some stuff)
	db2tune.bat
	db2tune.sh
	- edit: dbxlaunch.bat, dbxlaunch.sh 
				) ELSE IF "%APP_NAME%" == "Db2Tune" (
					set JAVA_START_CLASS="com.dbxtune.Db2Tune"
					set SPLASH=-splash:lib/db2tune_splash.jpg

 * build scripts (build.xml)
		<copy file="db2tune.bat"                                   toFile="${dist}/beta/db2tune.bat"/>
	<!--<copy file="db2tune.sh"                                    toFile="${dist}/beta/db2tune.sh"/>-->
		<copy file="${src}/com/dbxtune/images/db2tune_splash.jpg"  toFile="${dist}/beta/lib/db2tune_splash.jpg"/>



##############################################################
# Java code
##############################################################

 * change: src/com/dbxtune/DbxTune.java
	- in the main(), add Db2Tune
	  -> else if ("Db2Tune"      .equalsIgnoreCase(_mainClassName)) _instance = new Db2Tune      (cmd);
 
 * create: src/com/dbxtune/Db2Tune.java (take a copy of MySqlTune.java for instance))
	- change everuthing that says 'MySql' to 'Db2'
	- This requires you to create X number of new files, that implements DB2 Specifics
		src/com/dbxtune/check/CheckForUpdatesDb2.java
		src/com/dbxtune/CounterControllerDb2
		src/com/dbxtune/gui/MainFrameDb2.java
		[src/com/dbxtune/config/dict/MonTablesDictionaryDb2.java]
		[src/com/dbxtune/config/dbms/Db2Config.java]
		[src/com/dbxtune/config/dbms/Db2ConfigText.java]

Note: From here on it's a little "hacky" since we need to create a bunch of clases... that depends on other clases...
	
 * create: src/com/dbxtune/check/CheckForUpdatesDb2.java (take a copy from CheckForUpdatesMySql.java)
	- no changes needed (probably)
 
 * create: src/com/dbxtune/config/dict/MonTablesDictionaryDb2.java 
	- Fill in the static helper test for table/columns if the DBMS do not have a dictionary/table which we can reads
	  If the DBMS have a internal dictionary, then look at AseTune how it's solved...

 * create: src/com/dbxtune/CounterControllerDb2
	- This holds CM's (CounterModel object, which is "collectors")
	- initializes the CM's when we connect to a server
	- creates a "header" for CM's that will be stored in the PCS (Persistent Counter Storage - or save a recording)
	- specifies some behaviour and SQL queries used in the DBMS

 * create the first CM (CmSummary)
	- create package: com/dbxtune/cm/db2 & com/dbxtune/cm/db2/gui
	- create: com/dbxtune/cm/db2/CmSummary.java				(note in import: dont point to the "wrong" com.dbxtune.cm.>>>db2<<<.gui.CmSummaryPanel)
		change getSqlForVersion() to reflect what you want to get in the summary (view other CmSummary.java for inspiration) 
	- create: com/dbxtune/cm/db2/gui/CmSummaryPanel.java    (note in import: dont point to the "wrong" com.dbxtune.cm.>>>db2<<<.CmSummary)
		- here is a *bunch* of things to do... add/create a field for every column in the SQL statement and read the values into those fields

 * create: src/com/dbxtune/gui/MainFrameDb2.java
	- Change everithing "MySql" to "db2" 

 * optional (if you want to save/view the DBMS configuration)
	- create: src/com/dbxtune/config/dbms/Db2Config.java
		o This holds configuration (which is in a jTable so you can filter on it etc)
	- create: src/com/dbxtune/config/dbms/Db2ConfigText.java
		o This holds various other things which you want to know and store about the DBMS configuration

 * optional (if various components are DBMS Version dependant)
	- create: com.dbxtune.sql.conn.Db2Connection.java
	- edit: com.dbxtune.sql.conn.DbxConnection.java
		- to handle Db2Connection and various other stuff...

 * now start to add CmSomeName
	- add every Cm to CounterControllerDb2.createCounters()
	- make sure columns has proper datatypes (PCS - Persistent Counter Storage tables, will use the ResultSetMetaData to create tables)
