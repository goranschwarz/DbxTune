# DbxTune
DbxTune is a collection of programs to **Tune**/Monitor various DBMS dialects.  

The project started out as AseMon, which later was renamed to AseTune.  
ASE due to the fact that the tool only could talk to Sybase/SAP Adaptive Server Enterprise   
Then I decided to extend the tool to embrace other DBMS types as well!  
Hence the recent change to DbxTune


## How the DbxTune Tools family works

There are a couple of ways you can use the DbxTune tool set
1. **GUI mode**: view what's happening in the server **right now** (simple and easy)  
   _Note_: you can set up a in memory window so you can _go back_ and view **details** from any Collector _tab_
2. **GUI mode**: Same as above. But at the same time: **record** everything that you see in the GUI to a _recording database_  
   _Note_: if you _just_ want to record an hour at night time: Simply tell it to record between 03:00 - 04:00
3. **NO GUI mode**: If you want DbxTune to **constantly** collect data and store it in a _recording database_, 
   which you can **view** when _someone_ comes and ask you: "What's happening in the system, it's much 
   _slower_ now than _a week ago_... Whats' the problem?"
4. **GUI mode**: View/replay any recording you have done previously (in GUI mode, or NO-GUI mode)  
   Instead of connecting to a live DBMS, you just connect to the recorded _offline_ database, then click on a _timeline_ to position yourself within the recording to view **all** the details that was recorded.
5. **DbxCentral**: Have a _dashboard_ on the TV so that _anybody_ can view the Performance **Trend** Graphs.  
   And also make sure to record the activity in the test/development environments so that the developers can **see** that changes they make in the system has a positive or a negative effect on CPU Usage and/or Disk IO usage!
6. **Alarm(s)**: If you now have decide to recording performance data _all the time_, you can simply add 
   _thresholds_ to various metrics, and send an alarm when they are crossed!
   

   
## About the Author: Goran Schwarz

I was working for Sybase and SAP for 21 years (from 1994 to end-of 2015), as a consultant...  
One of my specialties was Performance & Tuning work, and Sybase didn't have any good tools for _digging_ into the various Performance Counters (and also record a session that could be investigated on later), **so I created my own tool** (AseTune) to be able to perform my duties at customer sites.

I left SAP 2015/2016, and now I work as a consultant for B3 Consulting Group - https://b3.se  
So if you are looking for help... I can help you... Contact me at: goran_schwarz@hotmail.com




## Available tools

All the below tools need enhancements, please let me know what, and how you can contribute with ideas and/or code :)

| Name          | Status             | Description                                                  |
| ------------- | ------------------ | ------------------------------------------------------------ |
| AseTune       | Excellent          | Tune Sybase/SAP Adaptive Server Enterprise                   |
| IqTune        | Good               | Tune Sybase/SAP IQ - The Column Store database               |
| SqlServerTune | Needs Improvements | Tune Microsoft SQL-Server                                    |
| PostgresTune  | Needs Improvements | Tune Postgres DBMS                                           |
| MySqlTune     | Needs Improvements | Tune MySQL DBMS (5.7 and later)                              |
| OracleTune    | Initial State      | Tune Oracle DBMS                                             |
| DB2Tune       | Initial State      | Tune DB2 LUW DBMS (LUW = Linux Unix Windows, mainframe is **not** supported) |
| RaxTune       | Good               | Tune Sybase/SAP Replication Agent X (not a DBMS, but still possible to capture statistics) |
| RsTune        | Good               | Tune Sybase/SAP Replication Server  (not a DBMS, but still possible to capture statistics) |
| HanaTune      | Needs Improvements | Tune SAP HANA - The In-Memory combined Column/Row Store database |

### Other tools
| Name        | Status | Description                                                  |
| ----------- | ------ | ------------------------------------------------------------ |
| DbxCentral  | Beta   | A WebServer that handles centralized metrics if/when you use NO-GUI collectors (see below for more info) |
| SQL Window  | Good   | Simple Query tool, connect to all of the above server types and execute commands (and any JDBC compliant DBMS) |
| Tail Window | Good   | Tail any file, if the file is located on Unix/Linux, simply open a SSH connection for the tail. |
| PerfDemo    | Beta   | A tool that can be used to stress load ASE - used for demo purposes of AseTune |




## DBX Central - Short Overview
A centralized web service

The idea behind this centralized server is that we are collecting **summary** information 
from any or all collectors in a central database, from there we can view **trend-graphs** via a web browser.

The central database will contain **less** information than the individual offline databases, where the collectors saved the data.  
Therefor _overview_ information can be saved for a longer time period...
In other words: the central database only holds data for trend graphs, while the individual collector databases contains **all** sampled details, and therefor are much bigger.

The central web server also _pushes_ out chart/graph data to web browsers, which makes
it possible to have a _live_ _dashboard_ (without having to refresh the browser all the time).  
If you are not _happy_ with the _dashboard_ and you want to create your own _layout_.  
You can for example use **Grafana** or some other tool to display the metrics data stored in the central database.

If we need to view any **details** from a recording. Then we just start the native DbxTune
application and connect to the _offline_ database.
So the central server is _just_ to view trends of how the system is working. 

For setup/installation: `README_dbxcentral_install.txt`  
For a big picture look at the file: `doc/dbxtune-central.pptx`

If you want a _preview_ how the DbxCentral looks like, you can check out http://gorans.org:8080 which is my test environment! You wont be able to see any traffic here, the DBMS instances are just idling, but hopefully you get the idea of how your system will look like...  
_Note:_ Please be gentle with the system :) 



## Some comments from Sourceforge
- I just downloaded it and started randomly pointing and clicking, but the ease of use and the amount of data that is available is really impressive. 
- This new version is a great piece of work. Thank you! Mr. Schwarz. I was immediately able to see poorly-performing queries on our production box and figure out what indices needed to be added, including creating covering indices. If you administer ASE, you **need** this. Period.
- After using Sybase ASE for a quarter century and writing a million dumb scripts to try and sus out perf data, AseTune is revolutionary. I am in awe of what you've done here, and I hope I can convince my employer they ought to contribute.
- Excellent tool; good alternative to Asemon as no intermediate server is required and can also be used in batch mode. Thanks for this good work
- works perfectly.

for more details see: https://sourceforge.net/projects/asetune/reviews



## Requirements
* Java 8 and above



## How to Install
Well there is not really an installer, just follow these simple steps
- unpack the ZIP in some directory 
- start any of the tools (files are located in the 'bin' directory)
  * windows: start the BAT file (xxxTune.bat)
  * Linux/Unix/Mac: start the SH file (xxxTune.sh)  
    Note: first have to change the shell scripts to be executable: `chmod 755 bin/*.sh`
- Connect to any server you want to tune/monitor

If you want to setup DbxCentral - The central Web Console to view Trend graphs  
See: `README_dbxcentral_install.txt`



## License
- GPL v3 (see `doc/COPYING` or `LICENSE.txt`)
- For commercial licenses, contact me, at: goran_schwarz@hotmail.com



## Tool Details and Status

------------------------------------------------------------------------------
### AseTune
This is the most complete tool. This because it has been around for 10 years

------------------------------------------------------------------------------
### RaxTune
Get values from ra_statistics and produce X number of graphs  
This tool was new in release 2015-06-01

------------------------------------------------------------------------------
### RsTune
Get values from various admin who, xxx  
Get values from admin statistics, which all tabs under "counters" are based on  
This tool was new in release 2015-06-01  
Status: low functionality, a lot of things to-do here

------------------------------------------------------------------------------
### IqTune
This tool was new in release 2015-06-01  
Status: much more can be done (kind of _entry_ level)

------------------------------------------------------------------------------
### HanaTune
This tool was new in release 2015-06-01  
Status: Not that much functionality, needs **A LOT MORE** work...  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### SqlServerTune
This tool was new in release 2015-06-01  
Status: Not that much functionality, needs MORE work... (soon at _entry_ level)  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### OracleTune
This tool was new in release 2015-06-01  
Status: Not that much functionality, needs **A LOT MORE** work...  
Note: Recording capability doesn't work that good for the moment (JDBC ResultSet MetaData returns "strange" data types for Oracle Columns, which I need to "map" into something more appropriate for H2 or any other "normal" database)  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### PostgresTune
This tool was new in release 2016-11-06  
Status: Not that much functionality, needs MORE work... (soon at _entry_ level)  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### MySqlTune
This tool was new in release 2018-11-05  
Status: Not that much functionality, needs MORE work...  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### DB2Tune
This tool was new in release 2018-11-05  
Status: Not that much functionality, needs **A LOT MORE** work...  
Tip: Create 'User Defined Counters' where you can add your own SQL and Performance Counters...

------------------------------------------------------------------------------
### SQL Window
This tool has also been around for a long time, and it has grown to be more and more potent.

Here are some of the functionality
- Code completion Ctrl+space 
- Command history
- Execute favorite commands (added by the user)
- A bunch of predefined _SQL Shortcut commands_
- Tail the Server error log (done via SSH if it's not located on your box)
- View Server configurations (show all, or just the changed ones)
- View JDBC MetaData structures
- Etc, etc...
  
------------------------------------------------------------------------------
### Tail Window
Tail any file, if it's a remote file it uses SSH to do the "remote" tail.  
It has a remote "file view" to select files on the remote server

------------------------------------------------------------------------------
### PerfDemo
Small utility to execute load in a ASE  
This utility is mainly used by me when doing demos of AseTune...  
If you want to use it, you need to install some server side objects.  

- Unzip `lib/asetune.jar` and go to directory `src/com/asetune/perftest/sql`
- Look at the `README.txt` in above directory
- if you have problems, please send me an email at: goran_schwarz@hotmail.com

------------------------------------------------------------------------------

