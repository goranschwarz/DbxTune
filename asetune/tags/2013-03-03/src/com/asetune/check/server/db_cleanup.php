<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="/favicon.ico"/>
<title>AseTune DB Cleanup</title>

<SCRIPT src="sorttable.js"></SCRIPT>
<STYLE type="text/css">
  /* Sortable tables */
  table.sortable thead {
    background-color:#eee;
    color:#666666;
    font-weight: bold;
    cursor: default;
  }
  body { font-size : 100%; font-family : Verdana,Helvetica,Arial,sans-serif; }
  h1, h2, h3 { font-size : 150%; }
  table { margin: 1em; border-collapse: collapse; font-size : 90%; }
  td, th { padding: .1em; border: 1px #ccc solid; font-size : 90%; }
  thead { background: #fc9; } </STYLE>
</HEAD>
<BODY>

<?php
	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$doAction = $_GET['doAction'];

	function doCleanup($sql)
	{
		echo "EXEC: <code>$sql</code><br>\n";
		mysql_query($sql) or die("ERROR: " . mysql_error());
		printf("Records affected: %d<br>\n", mysql_affected_rows());
		printf("<br>\n");
	}

	function describe($tabname)
	{
		echo "DESCRIBE: <code>$tabname</code><br>\n";

		$sql = "DESCRIBE $tabname";
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}

		$fields_num = mysql_num_fields($result);

		// printing table header
		echo "<table border=\"0\" class=\"sortable\">";
		echo "<tr>";
		// printing table headers
		for($i=0; $i<$fields_num; $i++)
		{
			$field = mysql_fetch_field($result);
			echo "<td nowrap>{$field->name}</td>";
		}
		echo "</tr>\n";

		// printing table rows
		while($row = mysql_fetch_row($result))
		{
			echo "<tr>";

			// $row is array... foreach( .. ) puts every element
			// of $row to $cell variable
			$col=-1;
			foreach($row as $cell)
			{
				$col++;
				$colname = mysql_field_name($result, $col);

				//$cellCont = nl2br($cell, false);
				//echo "<td nowrap>$cellCont</td>";
				echo "<td nowrap>$cell</td>";
			}

			echo "</tr>\n";
		}
		echo "</table>\n";
		mysql_free_result($result);

		printf("<br>\n");
	}

	function printFile($filename)
	{
		echo "<br> File Content: $filename <br>\n";

		$lines = file($filename);
		foreach ($lines as $line_num => $line)
			print "<code><font color=blue>Line #{$line_num}</font> : " . $line . "<br /></code>\n";
	}

	function installProc($procname)
	{
		$filename = $procname . ".sql";

		$fh = fopen($filename, 'r');
		$sql = fread($fh, filesize($filename));
		fclose($fh);

		// drop
		doCleanup("DROP PROCEDURE IF EXISTS $procname");

		mysql_query($sql) or die("ERROR: " . mysql_error() . "<br>" . printFile($filename) );
//		printf("Records affected: %d<br>\n", mysql_affected_rows());
		printf("<br>\n");
		printf("SUCCESS loading file: $filename<br>\n");
	}


	//------------------------------------------
	// Now connect to the database
	//-----
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	echo "<h1>Connected to database</h1>\n";

	if ( $doAction == "cleanup" )
	{
		doCleanup("delete from asemon_usage where user_name = 'rlarsson'");
		doCleanup("delete from asemon_usage where user_name = 'gorans'");
		doCleanup("delete from asemon_usage where user_name = ''");

		doCleanup("delete from asemon_usage              where user_name in('gorans', 'i063783') ");
		doCleanup("delete from asemon_connect_info       where userName  in('gorans', 'i063783') ");
		doCleanup("delete from asemon_mda_info           where userName  in('gorans', 'i063783') AND verified IS NULL");
		doCleanup("delete from asemon_udc_info           where userName  in('gorans', 'i063783') ");
		doCleanup("delete from asemon_counter_usage_info where userName  in('gorans', 'i063783') ");
		doCleanup("delete from asemon_error_info         where userName  in('gorans', 'i063783') ");
		doCleanup("delete from asemon_error_info2        where userName  in('gorans', 'i063783') ");

		// doCleanup("delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'");
		// doCleanup("delete from asemon_udc_info");
		// doCleanup("update asemon_usage set clientAsemonVersion = '2.1.0.1.dev' where clientAsemonVersion = '2.1.1.dev'");

		//doCleanup("delete from asemon_error_info         where appVersion  like '2.%' ");
		//doCleanup("delete from asemon_error_info2        where appVersion  like '2.%' ");

		//doCleanup("delete from asemon_error_info         where logLocation  like 'com.asetune.RefreshProcess.refreshStmt(RefreshProcess.java:%)%' ");
		//doCleanup("delete from asemon_error_info         where logStacktrace  like '%SQLException, Error writing DDL to Persistent Counter DB. Caught: org.h2.jdbc.JdbcSQLException: Unknown data type%' ");
		//doCleanup("delete from asemon_error_info         where logStacktrace  like 'java.lang.ClassCastException: java.lang.% cannot be cast to java.lang.%' ");
		//doCleanup("delete from asemon_error_info         where logStacktrace  like 'java.lang.NumberFormatException: Infinite or NaN%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problem when doing fireTableStructureChanged() or fireTableDataChanged()%' AND logStacktrace  like '%java.lang.ArrayIndexOutOfBoundsException: %' ");

		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems in AWT/Swing Event Dispatch Thread, Caught: java.lang.ClassCastException%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'ASE ''installmaster'' script may be of a faulty version. ASE Version is%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Setting option ''initCounters.useMonTablesVersion'' to%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'ASE Configuration option ''enable monitoring'' is NOT enabled.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'You need ''mon_role'' to access monitoring tables%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmIoQueue).getCnt : 247 Arithmetic overflow during explicit conversion of NUMERIC%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 12052 Collection of monitoring data for table ''%'' requires that the ''%'' configuration option(s) be enabled.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems when connecting to a ASE Server.%JZ006: %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems when connecting to a ASE Server.%JZ00L: %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems in AWT/Swing Event Dispatch Thread, Caught: java.lang.OutOfMemoryError: Java heap space%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Login will be aborted due to: Config options ''enable monitoring'' is still not enabled%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems getting basic status info in ''Counter get loop'', reverting back to ''static values''. SQL %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmSummary).getCnt : 1204 ASE has run out of LOCKS. Re-run your command when there are fewer active users%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(% type=''system.properties'', key=''http.proxyPort'', val=%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(% type=''system.properties'', key=''http.proxyHost'', val=%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmEngines).getCnt : 207 Invalid column name ''HkgcOverflowsDcomp''.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 1204 ASE has run out of LOCKS. Re-run your command %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problems when executing DDL sql statement: create table %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problem when executing the ''init'' SQL statement: % Create permanent tables for monW%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 208 tempdb.guest.monWaitEventInfo not found. Specify owner.objectname%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Monitoring tables must be installed ( please apply ''$SYBASE/$SYBASE_ASE/scripts/installmontables'' %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 12036 Collection of monitoring data for table ''%'' requires that the ''%'' configuration %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Monitor% command not found%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 0 SQL Warning in(%) Messages: AseMsgNum=3621, com.sybase.%.jdbc.SybSQLWarning: Command has been aborted.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'This is NOT a valid offline database. No AseTune system table exists.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'sp_list_unused_indexes.sql: Msg=%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Problem loading the script ''sp_list_unused_indexes.sql''.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Trying to Re-Initializing Performance Counter ''%'' shortName=''%'', After receiving MsgNumber ''%'', with Description %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'MonTablesDictionary:initialize, problems executing: sp_version. Exception: Stored procedure ''sp_version'' not found%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Duplicate key in ''CmIoQueue'', a row for the key%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like '%Connection is already closed%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'You may need ''sybase_ts_role'' to access some DBCC functionality or other commands used while monitoring.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Continuing with a minimal environment. Config options ''enable monitoring'' is still not enabled%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Option ''initCounters.useMonTablesVersion'' is %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like '%Cannot find an available index descriptor for an index. Increase the value of ''number of open indexes''. %' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'Monitoring tables must be installed%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like '%Command has been aborted.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'checkAndSetAseConfig(): Problems when configuring%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 207 Invalid column name ''HkgcOverflowsDcomp''%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'The DDL Storage queue has % entries. The persistent writer might not keep in pace.%' ");
		//doCleanup("delete from asemon_error_info         where logMessage  like 'The persistent queue has % entries. The persistent writer might not keep in pace.%' ");

//USED FOR TEMPLATE
//doCleanup("delete from asemon_error_info         where logMessage  like '%' ");
//doCleanup("delete from asemon_error_info         where logMessage  like '%' ");
//doCleanup("delete from asemon_error_info         where logMessage  like '%' ");




		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "dbmaint" )
	{
		// doCleanup("alter table asemon_connect_info add column usePcs     varchar(5)");
		// doCleanup("alter table asemon_connect_info add column pcsConfig  varchar(400)");

		// doCleanup("ALTER TABLE asemon_error_info DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_error_info ADD PRIMARY KEY (checkId, sendCounter, serverAddTime)");
		// doCleanup("ALTER TABLE asemon_error_info MODIFY logMessage varchar(4096)");
		// doCleanup("ALTER TABLE asemon_usage ADD callerIpAddress varchar(20)");

		// doCleanup("ALTER TABLE asemon_connect_info       ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup("ALTER TABLE asemon_connect_info       DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_connect_info       ADD  PRIMARY KEY (checkId, connectId, serverAddTime)");

		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup("ALTER TABLE asemon_counter_usage_info DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD  PRIMARY KEY (checkId, connectId, clientTime, cmName)");

		// doCleanup("ALTER TABLE asemon_connect_info       MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER isClusterEnabled");
		// doCleanup("ALTER TABLE asemon_counter_usage_info MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER userName");

		// doCleanup("ALTER TABLE asemon_connect_info       MODIFY srvIpPort varchar(60)");


		// doCleanup("ALTER TABLE asemon_counter_usage_info CHANGE clientTime sessionStartTime TIMESTAMP");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD sessionType      varchar(10)     AFTER serverAddTime");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD sessionEndTime   timestamp NULL  AFTER sessionStartTime");

		// doCleanup("ALTER TABLE asemon_usage MODIFY callerIpAddress varchar(20) AFTER clientCanonicalHostName");

		// doCleanup("ALTER TABLE asemon_usage ADD user_home varchar(50) AFTER user_name");

		//doCleanup("ALTER TABLE asemon_connect_info ADD srvSortOrderId   varchar(5)  AFTER srvVersionStr");
		//doCleanup("ALTER TABLE asemon_connect_info ADD srvSortOrderName varchar(30) AFTER srvSortOrderId");

		//doCleanup("ALTER TABLE asemon_connect_info ADD srvUserRoles varchar(80)  AFTER srvUser");
		//doCleanup("ALTER TABLE asemon_connect_info       MODIFY srvUserRoles varchar(160)");

		//doCleanup("ALTER TABLE asemon_mda_info ADD TableID int AFTER TableName");
		//doCleanup("ALTER TABLE asemon_mda_info ADD type char(1) FIRST");
		//doCleanup("ALTER TABLE asemon_mda_info DROP PRIMARY KEY");
		//doCleanup("ALTER TABLE asemon_mda_info ADD PRIMARY KEY (type, srvVersion, isClusterEnabled, TableName, ColumnName)");

		//doCleanup("UPDATE asemon_mda_info SET userName = '-save-' WHERE userName in ('gorans', '-fixed-', '-gorans-save-') ");
		//doCleanup("UPDATE asemon_mda_info SET userName = 'gorans' WHERE userName in ('-save-') ");

		// doCleanup("ALTER TABLE asemon_usage ADD clientExpireDate varchar(10) AFTER clientAsemonVersion");

		// doCleanup("ALTER TABLE asemon_mda_info ADD verified char(1) AFTER userName");

		// doCleanup("ALTER TABLE asemon_connect_info CHANGE srvUserRoles srvUserRoles varchar(300)");

		// doCleanup("ALTER TABLE asemon_usage ADD sun_arch_data_model varchar(10) AFTER java_vm_vendor");

		//doCleanup("ALTER TABLE asemon_connect_info ADD srvIsSapSystem varchar(20)  AFTER srvSortOrderName");
		//doCleanup("ALTER TABLE asemon_connect_info ADD srvSapSystemInfo varchar(40)  AFTER srvSortOrderName");
		//doCleanup("ALTER TABLE asemon_connect_info DROP COLUMN srvIsSapSystem");

//doCleanup("
//CREATE TABLE asemon_mda_info...
//");
		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "check" )
	{
		describe("asemon_usage");
		describe("asemon_connect_info");
		describe("asemon_mda_info");
		describe("asemon_udc_info");
		describe("asemon_counter_usage_info");
		describe("asemon_error_info");
		describe("asemon_error_info2");
		describe("asemon_error_info_save");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "reCreateProcs" )
	{
		installProc("full_mda_version_report");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else
	{
		echo "<h1>UNSUPPORTED COMMAND '" . $doAction . "'</h1>\n";
		echo "Commands:<br>\n";
		echo "<ul>\n";
		echo "  <li><code>cleanup</code> - Delete gorans from various tables</li>\n";
		echo "  <li><code>dbmaint</code> - Do some DB maintenace command, needs to be coded in the PHP script. Usually this section will be empty</li>\n";
		echo "  <li><code>check  </code> - Do 'DESCRIBE' on all known tables.</li>\n";
		echo "</ul>\n";
	}

	//------------------------------------------
	// Close connection to the database
	//-----
	echo "<h1>Closing database</h1>\n";
	mysql_close() or die("ERROR: " . mysql_error());
?>
<BR>
-END-
<BR>

</BODE>
</HTML>
