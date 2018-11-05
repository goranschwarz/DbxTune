==============================================================================
 DbxTune
==============================================================================
DbxTune is a collection of programs to Tune various DBMS dialects
The project started out as AseMon, later renamed to AseTune
Then I decided to extend the tool to embrace other DBMS types as well
Hence the recent change to DbxTune (or it will soon be renamed to DbxTune)



==============================================================================
 About the Author: Goran Schwarz (goran_schwarz@hotmail.com)
==============================================================================
I have been working for Sybase and SAP for 21 years (from 1994), as a consultant...
One of my thing is doing Performance & Tuning work, and Sybase didn't have
any good tools for "digging" into the various Performance Counters (and also 
record a session that could be investigated later on), so I created my own 
tool (AseTune) to be able to perform my duties at customer sites.

I left SAP 2015/2016, and now I work as a consultant for B3IT - http://b3.se
So if you are looking for help, I can help you...



==============================================================================
 Available tools
==============================================================================
* AseTune       - Tune Sybase/SAP Adaptive Server Enterprise
* DbxCentral    - A WebServer that handles centralized metrics if/when you use NO-GUI collectors (see below for more info)
* SQL Window    - Simple Query tool, connect to all of the above server types and execute commands (and any JDBC compliant DBMS)  
* Tail Window   - Tail any file, if the file is located on Unix/Linux, simply open a SSH connection for the tail.
* PerfDemo      - A tool that can be used to stress load ASE - used for demo purposes of AseTune

The below tools are in beta status...
If you want to use them simply start them from the "bin" directory
The tools need enhancements, please let me know what, and how you can contribute with ideas and/or code :)

* RaxTune       - Tune Sybase/SAP Replication Agent X (not a DBMS, but still possible to capture statistics)
* RsTune        - Tune Sybase/SAP Replication Server  (not a DBMS, but still possible to capture statistics)
* IqTune        - Tune Sybase/SAP IQ - The Column Store database
* HanaTune      - Tune SAP HANA - The InMemory combined Column/Row Store database
  --------------- None SAP/Sybase DBMS products:
* SqlServerTune - Tune Microsoft SQL-Server
* PostgresTune  - Tune Postgres DBMS
* MySqlTune     - Tune MySQL DBMS (5.7 and later)
* OracleTune    - Tune Oracle DBMS
* DB2Tune       - Tune DB2 LUW DBMS (LUW = Linux Unix Windows, mainframe isn't supported)



==============================================================================
 DBX Central - short overview (NOTE: Still in beta)
==============================================================================
A centralized web service

The idea behind this centralized server is that we are collecting "summary" information 
from all collectors in a central database, from there we can view trend-graphs via a web browser.

The central database will contain *less* information than the individual offline databases.
Therefor "overview" information can be saved for a longer period...

The central web server can also "pushes" out chart/graph data to web browsers, which makes
it possible to have a "live" dashboard (without having to refresh the browser all the time).  

If we need to view any *details* from a recording. Then we will have to start the native DbxTune
application and connect to the "offline" database.
So the central server is "just" to view trends of how the system is working. 

For setup/installation: README_dbxcentral_install.txt
For a big picture look at the file: doc/dbxtune-central.pptx



==============================================================================
 Requirements
==============================================================================
* Java 7 and above (Java 7 is still working, but it I *will* move to Java 8 on next release)



==============================================================================
 How to Install
==============================================================================
Well there is not really an installer, just follow these simple steps
* unpack the ZIP in some directory 
* start any of the tools (files are located in the 'bin' directory)
  - windows: start the BAT file (xxxTune.bat)
  - Linux/Unix/Mac: start the SH file (xxxTune.sh)
* Connect to any server you want to tune/monitor

* If you want to setup DbxCentral - The central Web Console to view Trend graphs
  See: README_dbxcentral_install.txt
 


==============================================================================
 License
==============================================================================
* GPL v3 (see doc/COPYING or LICENSE.txt)
* For commercial licenses, contact me, at: goran_schwarz@hotmail.com



==============================================================================
 Tool Details and Status
==============================================================================

------------------------------------------------------------------------------
-- AseTune -------------------------------------------------------------------
------------------------------------------------------------------------------
This is the most complete tool. This because it has been around for 7-10 years


------------------------------------------------------------------------------
-- RaxTune -------------------------------------------------------------------
------------------------------------------------------------------------------
- get values from ra_statistics and produce X number of graphs
  This tool is new in release 2015-06-01

------------------------------------------------------------------------------
-- RsTune --------------------------------------------------------------------
------------------------------------------------------------------------------
- get values from various admin who, xxx
- get values from admin statistics, which all tabs under "counters" are based on
  This tool is new in release 2015-06-01
  Status: low functionality, a lot of things to-do here (not sure if it will be released, please send me feedback) 

------------------------------------------------------------------------------
-- IqTune --------------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: much more can be done (kind of "entry" level) 

------------------------------------------------------------------------------
-- HanaTune ------------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: Not that much functionality, needs MORE work before released. 
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- SqlServerTune -------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: Not that much functionality, needs MORE work before released. 
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- OracleTune ----------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: Not that much functionality, needs A LOT MORE work before released. 
  Note: Recording capability doesn't work that good for the moment (JDBC ResultSet MetaData returns "strange" data types for Oracle Columns, which I need to "map" into something more appropriate for H2 or any other "normal" database)
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- PostgresTune --------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2016-11-06
  Status: Not that much functionality, needs A LOT MORE work before released. 
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- MySqlTune -----------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2018-11-05
  Status: Not that much functionality, needs A LOT MORE work before released. 
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- DB2Tune -------------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2018-11-05
  Status: Not that much functionality, needs A LOT MORE work before released. 
  Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
-- SQL Window ----------------------------------------------------------------
------------------------------------------------------------------------------
This tool has also been around for a long time, and it has grown to
be more and more potent.

Here are some of the functionality
 * Code completion Ctrl+space 
 * Command history
 * Execute favorite commands (added by the user)
 * A bunch of predefined "SQL Shortcut commands"
 * Tail the Server error log (done via SSH if it’s not located on your box)
 * View Server configurations (show all, or just the changed ones)
 * View JDBC MetaData structures
 * Etc, etc...
  
------------------------------------------------------------------------------
-- Tail Window ---------------------------------------------------------------
------------------------------------------------------------------------------
  Tail any file, if it's a remote file it uses SSH to do the "remote" tail.
  It has a remote "file view" to select files on the remote server

------------------------------------------------------------------------------
-- PerfDemo ------------------------------------------------------------------
------------------------------------------------------------------------------
  Small utility to execute load in a ASE
  This utility is mainly used by me when doing demos of AseTune...
  If you want to use it, you need to install some server side objects.
  * Unzip lib/asetune.jar and go to directory 'src/com/asetune/perftest/sql'
  * Look at the README.txt in above directory
  * if you have problems, plese send me an email at: goran_schwarz@hotmail.com


