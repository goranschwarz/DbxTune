==============================================================================
 DbxTune
==============================================================================
DbxTune is a collection of programs to Tune various DBMS dialects
The project started out as AseMon, later renamed to AseTune
Then I decided to extend the tool to embrace other DBMS types as well
Hence the recent change to DbxTune 


==============================================================================
 About the Author: Goran Schwarz (goran_schwarz@hotmail.com)
==============================================================================
I have been working for Sybase and SAP for 21 years, as a consultant...
One of my thing is doing Performance & Tuning work, and Sybase didn't have
any good tools for "digging" into the various Performance Counters (and also 
record a session that could be investigated later on), so I created my own 
tool (AseTune) to be able to perform my duties at customer sites.


==============================================================================
 Available tools
==============================================================================
* AseTune       - Tune Sybase/SAP Adaptive Server Enterprise
* RaxTune       - Tune Sybase/SAP Replication Agent X (not a DBMS, but still possible to capture statistics)
* RsTune        - Tune Sybase/SAP Replication Server  (not a DBMS, but still possible to capture statistics)
* IqTune        - Tune Sybase/SAP IQ - The Column Store database
* HanaTune      - Tune SAP HANA - The InMemory combined Column/Row Store database
* SqlServerTune - Tune Microsoft SQL-Server
* OracleTune    - Tune Oracle DBMS
 
* SQL Window    - Simple Query tool, connect to all of the above server types and execute commands (and any JDBC compliant DBMS)  
* Tail Window   - Tail any file, if the file is located on Unix/Linux, simply open a SSH connection for the tail.
* PerfDemo      - A tool that can be used to stress load ASE - used for demo purposes of AseTune


==============================================================================
 Requirements
==============================================================================
* Java 7 and above (Java 6 is still working, but it I will remove support for that soon)
 

==============================================================================
 How to Install
==============================================================================
Well there is not really an installer, just follow these simple steps
* unpack the ZIP in some directory 
* start any of the tools
  - windows: start the BAT file (xxxTune.bat)
  - Linux/Unix/Mac: start the SH file (xxxTune.sh)
* Connect to any server you want to tune/monitor
 


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
  Status: low functionality, a lot of things to-do here (not sure if it will be released) 

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

------------------------------------------------------------------------------
-- SqlServerTune -------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: Not that much functionality, needs MORE work before released. 

------------------------------------------------------------------------------
-- OracleTune ----------------------------------------------------------------
------------------------------------------------------------------------------
  This tool is new in release 2015-06-01
  Status: Not that much functionality, needs A LOT MORE work before released. 

------------------------------------------------------------------------------
-- SQL Window ----------------------------------------------------------------
------------------------------------------------------------------------------
This tool has also been around for a long time, and it has grown to
be more and more potent.

Here are some of the functionality
 * Code Completion: 
 * Command History:
 * xxx: 
  
------------------------------------------------------------------------------
-- Tail Window ---------------------------------------------------------------
------------------------------------------------------------------------------

------------------------------------------------------------------------------
-- PerfDemo ------------------------------------------------------------------
------------------------------------------------------------------------------




doCleanup("ALTER TABLE asemon_connect_info        ADD connTypeStr        varchar(30)  AFTER userName");
doCleanup("ALTER TABLE asemon_connect_info        ADD prodName           varchar(30)  AFTER srvUserRoles");
doCleanup("ALTER TABLE asemon_connect_info        ADD prodVersionStr     varchar(255) AFTER prodName");
doCleanup("ALTER TABLE asemon_connect_info        ADD jdbcDriver         varchar(60)  AFTER srvSapSystemInfo");
doCleanup("ALTER TABLE asemon_connect_info        ADD jdbcUrl            varchar(255) AFTER jdbcDriver");
doCleanup("ALTER TABLE asemon_connect_info        ADD jdbcDriverName     varchar(255) AFTER jdbcUrl");
doCleanup("ALTER TABLE asemon_connect_info        ADD jdbcDriverVersion  varchar(255) AFTER jdbcDriverName");




  