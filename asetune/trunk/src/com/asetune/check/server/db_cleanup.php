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
	require("gorans_functions.php");

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$doAction = $_GET['doAction'];

	if ( $doAction == "testVersion" )
	{
		$version = $_GET['version'];
		echo "input version = '" . $version . "'<br>";
		$versionConv = versionFix_toLongLong($version);
		echo "conv  version = '" . $versionConv . "'<br>";
		echo "print version = '" . versionDisplayLongLong($versionConv) . "'<br>";
		die("----END---<br>");
	}


	function doQuery($dbconn, $sql)
	{
		echo "EXEC: <pre>$sql</pre><br>\n";
		// sending query
		$result = mysqli_query($dbconn, $sql);
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: SQL Failed");
		}
		htmlResultset($userIdCache, $result, $sql);
	}

	function doCleanup($dbconn, $sql)
	{
		echo "EXEC: <code>$sql</code><br>\n";
//		echo "EXEC: <pre>$sql</pre><br>\n";
		mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));
		printf("<br>\n");
	}

	function describe($dbconn, $tabname)
	{
		echo "DESCRIBE: <code>$tabname</code><br>\n";

		$sql = "DESCRIBE $tabname";
		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: Query to show fields from table failed");
		}

		$fields_num = mysqli_num_fields($result);

		// printing table header
		echo "<table border=\"0\" class=\"sortable\">";
		echo "<tr>";
		// printing table headers
		for($i=0; $i<$fields_num; $i++)
		{
			$field = mysqli_fetch_field($result);
			echo "<td nowrap>{$field->name}</td>";
		}
		echo "</tr>\n";

		// printing table rows
		while($row = mysqli_fetch_row($result))
		{
			echo "<tr>";

			// $row is array... foreach( .. ) puts every element
			// of $row to $cell variable
			$col=-1;
			foreach($row as $cell)
			{
				$col++;
				$finfo = mysqli_fetch_field_direct($result, $col);
				$colname = $finfo->name;

				//$cellCont = nl2br($cell, false);
				//echo "<td nowrap>$cellCont</td>";
				echo "<td nowrap>$cell</td>";
			}

			echo "</tr>\n";
		}
		echo "</table>\n";
		mysqli_free_result($result);

		printf("<br>\n");
	}

	function printFile($filename)
	{
		echo "<br> File Content: $filename <br>\n";

		$lines = file($filename);
		foreach ($lines as $line_num => $line)
			print "<code><font color=blue>Line #{$line_num}</font> : " . $line . "<br /></code>\n";
	}

	function installProc($dbconn, $procname)
	{
		$filename = $procname . ".sql";

		$fh = fopen($filename, 'r');
		$sql = fread($fh, filesize($filename));
		fclose($fh);

		// drop
		doCleanup($dbconn, "DROP PROCEDURE IF EXISTS $procname");

		mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn) . "<br>" . printFile($filename) );
//		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));
		printf("<br>\n");
		printf("SUCCESS loading file: $filename<br>\n");
	}

	
//	function upgradeVersionToLongLong($dbconn, $tabname, $idColName, $verColName)
//	{
//		echo "upgradeVersionToLongLong: <code>$tabname</code><br>\n";
//
//		$sql = "SELECT distinct $idColName, $verColName FROM $tabname WHERE $verColName > 0 AND $verColName < 1000000000000"; // 1 00 00 0000 0000
//		echo "upgradeVersionToLongLong - SQL: <code>$sql</code><br>\n";
//
//		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
//		if (!$result) {
//			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
//			die("ERROR: Query to show fields from table failed");
//		}
//		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));
//
//		// Add SQL Statements to an list/array
//		$sqlList = array();
//		while($row = mysqli_fetch_row($result))
//		{
//			$id         = $row[0];
//			$curVersion = $row[1];
//			$newVersion = versionFix($curVersion);
//			$updateStr = "UPDATE $tabname SET $verColName = $newVersion WHERE $idColName = $id -- oldVel = $curVersion";
//
//			array_push($sqlList, $updateStr);
//		}
//		mysqli_free_result($result);
//
//		// printing table rows
//		$r = 0;
//		foreach ($sqlList as &$sqlStr) 
//		{
//			$r++;
//			
//			//printf(" ---row[$r] " . $sqlStr . "<br>\n");
//			
//			printf("EXEC[$r]: <code>$sqlStr</code><br>\n");
//			mysqli_query($dbconn, $sqlStr) or die("ERROR: " . mysqli_error($dbconn));
//			printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));
//			printf("<br>\n");
//		}
//		printf("-END-<br>\n");
//	}
	function upgradeVersion_asemon_connect_info($dbconn, $doExec)
	{
		$tabname = "asemon_connect_info";
		echo "upgradeVersion(): <code>$tabname</code><br>\n";

		$sql = "SELECT checkId, serverAddTime, connectId, srvVersion" 
			 . " FROM $tabname WHERE srvVersion > 0 AND srvVersion < 1000000000000"; // 1 00 00 0000 0000
		echo "upgradeVersion() - SQL: <code>$sql</code><br>\n";

		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));

		// Add SQL Statements to an list/array
		$sqlList = array();
		while($row = mysqli_fetch_row($result))
		{
			$checkId        = $row[0];
			$serverAddTime  = $row[1];
			$connectId      = $row[2];
			$curVersion     = $row[3];
			$newVersion = versionFix($curVersion);
			$updateStr = "UPDATE $tabname SET srvVersion = $newVersion WHERE checkId = $checkId AND serverAddTime = '$serverAddTime' AND connectId = $connectId -- oldVel = $curVersion";

			array_push($sqlList, $updateStr);
		}
		mysqli_free_result($result);

		printf("EXEC: <code>START TRANSACTION</code><br>\n");
		mysqli_query($dbconn, "START TRANSACTION") or die("ERROR: " . mysqli_error($dbconn));
		
		// printing table rows
		$r = 0;
		foreach ($sqlList as &$sqlStr) 
		{
			$r++;
			
			if ($doExec != 1)
			{
				printf(" ---row[$r] " . $sqlStr . "<br>\n");
			}
			else
			{
				printf("EXEC[$r]: <code>$sqlStr</code><br>\n");
				mysqli_query($dbconn, $sqlStr) or die("ERROR: " . mysqli_error($dbconn));
				$rowc = mysqli_affected_rows($dbconn);
				printf("Records affected: %d<br>\n", $rowc);
				if ($rowc != 1)
					die("ERROR: Expected row count is 1. and we received $rowc. -------- ROLLBACK AND EXIT -------------\n<br>");
				printf("<br>\n");
			}
		}
		
		printf("EXEC: <code>COMMIT</code><br>\n");
		mysqli_query($dbconn, "COMMIT") or die("ERROR: " . mysqli_error($dbconn));
		printf("-END-<br>\n");
	}
	function upgradeVersion_asemon_mda_info($dbconn, $doExec)
	{
		$tabname = "asemon_mda_info";
		echo "upgradeVersion(): <code>$tabname</code><br>\n";

		$sql = "SELECT type, srvVersion, isClusterEnabled, TableName, ColumnName" 
			 . " FROM $tabname WHERE srvVersion > 0 AND srvVersion < 1000000000000"; // 1 00 00 0000 0000
		echo "upgradeVersion() - SQL: <code>$sql</code><br>\n";

		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));

		// Add SQL Statements to an list/array
		$sqlList = array();
		while($row = mysqli_fetch_row($result))
		{
			$type              = $row[0];
			$srvVersion        = $row[1];
			$isClusterEnabled  = $row[2];
			$TableName         = $row[3];
			$ColumnName        = $row[4];
			$newVersion = versionFix($srvVersion);
			$updateStr = "UPDATE $tabname SET srvVersion = $newVersion WHERE type = '$type' AND srvVersion = $srvVersion AND isClusterEnabled = $isClusterEnabled AND TableName = '$TableName' AND ColumnName = '$ColumnName' -- oldVel = $srvVersion";

			array_push($sqlList, $updateStr);
		}
		mysqli_free_result($result);

		printf("EXEC: <code>START TRANSACTION</code><br>\n");
		mysqli_query($dbconn, "START TRANSACTION") or die("ERROR: " . mysqli_error($dbconn));
		
		// printing table rows
		$r = 0;
		foreach ($sqlList as &$sqlStr) 
		{
			$r++;
			
			if ($doExec != 1)
			{
				printf(" ---row[$r] " . $sqlStr . "<br>\n");
			}
			else
			{
				printf("EXEC[$r]: <code>$sqlStr</code><br>\n");
				mysqli_query($dbconn, $sqlStr) or die("ERROR: " . mysqli_error($dbconn));
				$rowc = mysqli_affected_rows($dbconn);
				printf("Records affected: %d<br>\n", $rowc);
				if ($rowc != 1)
					die("ERROR: Expected row count is 1. and we received $rowc. -------- ROLLBACK AND EXIT -------------\n<br>");
				printf("<br>\n");
			}
		}
		
		printf("EXEC: <code>COMMIT</code><br>\n");
		mysqli_query($dbconn, "COMMIT") or die("ERROR: " . mysqli_error($dbconn));
		printf("-END-<br>\n");
	}
	function upgradeVersion_sqlw_connect_info($dbconn, $doExec)
	{
		$tabname = "sqlw_connect_info";
		echo "upgradeVersion(): <code>$tabname</code><br>\n";

		$sql = "SELECT sqlwCheckId, serverAddTime, connectId, srvVersionInt" 
			 . " FROM $tabname WHERE srvVersionInt > 0 AND srvVersionInt < 1000000000000"; // 1 00 00 0000 0000
		echo "upgradeVersion() - SQL: <code>$sql</code><br>\n";

		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));

		// Add SQL Statements to an list/array
		$sqlList = array();
		while($row = mysqli_fetch_row($result))
		{
			$sqlwCheckId    = $row[0];
			$serverAddTime  = $row[1];
			$connectId      = $row[2];
			$curVersion     = $row[3];
			$newVersion = versionFix($curVersion);
			$updateStr = "UPDATE $tabname SET srvVersionInt = $newVersion WHERE sqlwCheckId = $sqlwCheckId AND serverAddTime = '$serverAddTime' AND connectId = $connectId -- oldVel = $curVersion";

			array_push($sqlList, $updateStr);
		}
		mysqli_free_result($result);

		printf("EXEC: <code>START TRANSACTION</code><br>\n");
		mysqli_query($dbconn, "START TRANSACTION") or die("ERROR: " . mysqli_error($dbconn));
		
		// printing table rows
		$r = 0;
		foreach ($sqlList as &$sqlStr) 
		{
			$r++;
			
			if ($doExec != 1)
			{
				printf(" ---row[$r] " . $sqlStr . "<br>\n");
			}
			else
			{
				printf("EXEC[$r]: <code>$sqlStr</code><br>\n");
				mysqli_query($dbconn, $sqlStr) or die("ERROR: " . mysqli_error($dbconn));
				$rowc = mysqli_affected_rows($dbconn);
				printf("Records affected: %d<br>\n", $rowc);
				if ($rowc != 1)
					die("ERROR: Expected row count is 1. and we received $rowc. -------- ROLLBACK AND EXIT -------------\n<br>");
				printf("<br>\n");
			}
		}
		
		printf("EXEC: <code>COMMIT</code><br>\n");
		mysqli_query($dbconn, "COMMIT") or die("ERROR: " . mysqli_error($dbconn));
		printf("-END-<br>\n");
	}
	function upgradeVersion_sqlw_usage_info($dbconn, $doExec)
	{
		$tabname = "sqlw_usage_info";
		echo "upgradeVersion(): <code>$tabname</code><br>\n";

		$sql = "SELECT sqlwCheckId, serverAddTime, connectId, srvVersionInt" 
			 . " FROM $tabname WHERE srvVersionInt > 0 AND srvVersionInt < 1000000000000"; // 1 00 00 0000 0000
		echo "upgradeVersion() - SQL: <code>$sql</code><br>\n";

		$result = mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));
		if (!$result) {
			echo mysqli_errno($dbconn) . ": " . mysqli_error($dbconn) . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		printf("Records affected: %d<br>\n", mysqli_affected_rows($dbconn));

		// Add SQL Statements to an list/array
		$sqlList = array();
		while($row = mysqli_fetch_row($result))
		{
			$sqlwCheckId    = $row[0];
			$serverAddTime  = $row[1];
			$connectId      = $row[2];
			$curVersion     = $row[3];
			$newVersion = versionFix($curVersion);
			$updateStr = "UPDATE $tabname SET srvVersionInt = $newVersion WHERE sqlwCheckId = $sqlwCheckId AND serverAddTime = '$serverAddTime' AND connectId = $connectId -- oldVel = $curVersion";

			array_push($sqlList, $updateStr);
		}
		mysqli_free_result($result);

		printf("EXEC: <code>START TRANSACTION</code><br>\n");
		mysqli_query($dbconn, "START TRANSACTION") or die("ERROR: " . mysqli_error($dbconn));

		// printing table rows
		$r = 0;
		foreach ($sqlList as &$sqlStr) 
		{
			$r++;
			
			if ($doExec != 1)
			{
				printf(" ---row[$r] " . $sqlStr . "<br>\n");
			}
			else
			{
				printf("EXEC[$r]: <code>$sqlStr</code><br>\n");
				mysqli_query($dbconn, $sqlStr) or die("ERROR: " . mysqli_error($dbconn));
				$rowc = mysqli_affected_rows($dbconn);
				printf("Records affected: %d<br>\n", $rowc);
				if ($rowc != 1)
					die("ERROR: Expected row count is 1. and we received $rowc. -------- ROLLBACK AND EXIT -------------\n<br>");
				printf("<br>\n");
			}
		}

		printf("EXEC: <code>COMMIT</code><br>\n");
		mysqli_query($dbconn, "COMMIT") or die("ERROR: " . mysqli_error($dbconn));
		printf("-END-<br>\n");
	}



	//------------------------------------------
	// Now connect to the database
	//-----
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	echo "<h1>Connected to database</h1>\n";

	if ( $doAction == "cleanup" )
	{
		doCleanup($dbconn, "delete from asemon_usage where user_name = 'rlarsson'");
		doCleanup($dbconn, "delete from asemon_usage where user_name = 'gorans'");
		doCleanup($dbconn, "delete from asemon_usage where user_name = ''");

		doCleanup($dbconn, "delete from asemon_usage              where user_name in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from asemon_connect_info       where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from asemon_mda_info           where userName  in('gorans', 'i063783') AND verified IS NULL");
		doCleanup($dbconn, "delete from asemon_udc_info           where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from asemon_counter_usage_info where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from asemon_error_info         where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from asemon_error_info2        where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from sqlw_usage                where user_name in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from sqlw_connect_info         where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from sqlw_usage_info           where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from dbxc_store_info           where userName  in('gorans', 'i063783') ");
		doCleanup($dbconn, "delete from dbxc_store_srv_info       where userName  in('gorans', 'i063783') ");

		// based on HOSTNAME
		doCleanup($dbconn, "delete from asemon_connect_info       where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from asemon_mda_info           where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) AND verified IS NULL");
		doCleanup($dbconn, "delete from asemon_udc_info           where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from asemon_counter_usage_info where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from asemon_error_info         where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from asemon_error_info2        where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from dbxc_store_info           where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from dbxc_store_srv_info       where checkId in(select rowid from asemon_usage where clientHostName in('gorans-ub2')) ");
		doCleanup($dbconn, "delete from asemon_usage              where clientHostName in('gorans-ub2') ");
		
		// doCleanup($dbconn, "delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'");
		// doCleanup($dbconn, "delete from asemon_udc_info");
		// doCleanup($dbconn, "update asemon_usage set clientAsemonVersion = '2.1.0.1.dev' where clientAsemonVersion = '2.1.1.dev'");

		//doCleanup($dbconn, "delete from asemon_error_info         where appVersion  like '2.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info2        where appVersion  like '2.%' ");

		//doCleanup($dbconn, "delete from asemon_error_info         where logLocation  like 'com.asetune.RefreshProcess.refreshStmt(RefreshProcess.java:%)%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logStacktrace  like '%SQLException, Error writing DDL to Persistent Counter DB. Caught: org.h2.jdbc.JdbcSQLException: Unknown data type%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logStacktrace  like 'java.lang.ClassCastException: java.lang.% cannot be cast to java.lang.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logStacktrace  like 'java.lang.NumberFormatException: Infinite or NaN%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problem when doing fireTableStructureChanged() or fireTableDataChanged()%' AND logStacktrace  like '%java.lang.ArrayIndexOutOfBoundsException: %' ");

		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems in AWT/Swing Event Dispatch Thread, Caught: java.lang.ClassCastException%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'ASE ''installmaster'' script may be of a faulty version. ASE Version is%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Setting option ''initCounters.useMonTablesVersion'' to%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'ASE Configuration option ''enable monitoring'' is NOT enabled.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'You need ''mon_role'' to access monitoring tables%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmIoQueue).getCnt : 247 Arithmetic overflow during explicit conversion of NUMERIC%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 12052 Collection of monitoring data for table ''%'' requires that the ''%'' configuration option(s) be enabled.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems when connecting to a ASE Server.%JZ006: %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems when connecting to a ASE Server.%JZ00L: %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems in AWT/Swing Event Dispatch Thread, Caught: java.lang.OutOfMemoryError: Java heap space%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Login will be aborted due to: Config options ''enable monitoring'' is still not enabled%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems getting basic status info in ''Counter get loop'', reverting back to ''static values''. SQL %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmSummary).getCnt : 1204 ASE has run out of LOCKS. Re-run your command when there are fewer active users%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(% type=''system.properties'', key=''http.proxyPort'', val=%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(% type=''system.properties'', key=''http.proxyHost'', val=%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(CmEngines).getCnt : 207 Invalid column name ''HkgcOverflowsDcomp''.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 1204 ASE has run out of LOCKS. Re-run your command %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problems when executing DDL sql statement: create table %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problem when executing the ''init'' SQL statement: % Create permanent tables for monW%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 208 tempdb.guest.monWaitEventInfo not found. Specify owner.objectname%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Monitoring tables must be installed ( please apply ''$SYBASE/$SYBASE_ASE/scripts/installmontables'' %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 12036 Collection of monitoring data for table ''%'' requires that the ''%'' configuration %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Monitor% command not found%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 0 SQL Warning in(%) Messages: AseMsgNum=3621, com.sybase.%.jdbc.SybSQLWarning: Command has been aborted.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'This is NOT a valid offline database. No AseTune system table exists.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'sp_list_unused_indexes.sql: Msg=%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problem loading the script ''sp_list_unused_indexes.sql''.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Trying to Re-Initializing Performance Counter ''%'' shortName=''%'', After receiving MsgNumber ''%'', with Description %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'MonTablesDictionary:initialize, problems executing: sp_version. Exception: Stored procedure ''sp_version'' not found%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Duplicate key in ''CmIoQueue'', a row for the key%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%Connection is already closed%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'You may need ''sybase_ts_role'' to access some DBCC functionality or other commands used while monitoring.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Continuing with a minimal environment. Config options ''enable monitoring'' is still not enabled%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Option ''initCounters.useMonTablesVersion'' is %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%Cannot find an available index descriptor for an index. Increase the value of ''number of open indexes''. %' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Monitoring tables must be installed%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%Command has been aborted.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'checkAndSetAseConfig(): Problems when configuring%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'SamplingCnt(%).getCnt : 207 Invalid column name ''HkgcOverflowsDcomp''%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'The DDL Storage queue has % entries. The persistent writer might not keep in pace.%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'The persistent queue has % entries. The persistent writer might not keep in pace.%' ");

		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like 'Problem when re-connecting to monitored server%' ");
		//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '% : 701 There is not enough procedure cache to run this procedure, trigger%' ");

		// Cleanup (ALL) error information
		//doCleanup($dbconn, "delete from asemon_error_info");
		//doCleanup($dbconn, "delete from asemon_error_info2");

//USED FOR TEMPLATE
//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%' ");
//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%' ");
//doCleanup($dbconn, "delete from asemon_error_info         where logMessage  like '%' ");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "errors_old_versions" )
	{
		doCleanup($dbconn, "delete from asemon_error_info  where appVersion < '4.0.0' ");
		doCleanup($dbconn, "delete from asemon_error_info2 where appVersion < '4.0.0' ");
	}
	else if ( $doAction == "dbmaint" )
	{
		// doCleanup($dbconn, "alter table asemon_connect_info add column usePcs     varchar(5)");
		// doCleanup($dbconn, "alter table asemon_connect_info add column pcsConfig  varchar(400)");

		// doCleanup($dbconn, "ALTER TABLE asemon_error_info DROP PRIMARY KEY");
		// doCleanup($dbconn, "ALTER TABLE asemon_error_info ADD PRIMARY KEY (checkId, sendCounter, serverAddTime)");
		// doCleanup($dbconn, "ALTER TABLE asemon_error_info MODIFY logMessage varchar(4096)");
		// doCleanup($dbconn, "ALTER TABLE asemon_usage ADD callerIpAddress varchar(20)");

		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info       ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info       DROP PRIMARY KEY");
		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info       ADD  PRIMARY KEY (checkId, connectId, serverAddTime)");

		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info DROP PRIMARY KEY");
		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info ADD  PRIMARY KEY (checkId, connectId, clientTime, cmName)");

		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info       MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER isClusterEnabled");
		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER userName");

		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info       MODIFY srvIpPort varchar(60)");


		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info CHANGE clientTime sessionStartTime TIMESTAMP");
		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info ADD sessionType      varchar(10)     AFTER serverAddTime");
		// doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info ADD sessionEndTime   timestamp NULL  AFTER sessionStartTime");

		// doCleanup($dbconn, "ALTER TABLE asemon_usage MODIFY callerIpAddress varchar(20) AFTER clientCanonicalHostName");

		// doCleanup($dbconn, "ALTER TABLE asemon_usage ADD user_home varchar(50) AFTER user_name");

		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvSortOrderId   varchar(5)  AFTER srvVersionStr");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvSortOrderName varchar(30) AFTER srvSortOrderId");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvCharsetId   varchar(5)  AFTER srvSortOrderName");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvCharsetName varchar(30) AFTER srvCharsetId");

		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvUserRoles varchar(80)  AFTER srvUser");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info       MODIFY srvUserRoles varchar(160)");

		//doCleanup($dbconn, "ALTER TABLE asemon_mda_info ADD TableID int AFTER TableName");
		//doCleanup($dbconn, "ALTER TABLE asemon_mda_info ADD type char(1) FIRST");
		//doCleanup($dbconn, "ALTER TABLE asemon_mda_info DROP PRIMARY KEY");
		//doCleanup($dbconn, "ALTER TABLE asemon_mda_info ADD PRIMARY KEY (type, srvVersion, isClusterEnabled, TableName, ColumnName)");

		//doCleanup($dbconn, "UPDATE asemon_mda_info SET userName = '-save-' WHERE userName in ('gorans', '-fixed-', '-gorans-save-') ");
		//doCleanup($dbconn, "UPDATE asemon_mda_info SET userName = 'gorans' WHERE userName in ('-save-') ");

		// doCleanup($dbconn, "ALTER TABLE asemon_usage ADD clientExpireDate varchar(10) AFTER clientAsemonVersion");

		// doCleanup($dbconn, "ALTER TABLE asemon_mda_info ADD verified char(1) AFTER userName");

		// doCleanup($dbconn, "ALTER TABLE asemon_connect_info CHANGE srvUserRoles srvUserRoles varchar(300)");

		// doCleanup($dbconn, "ALTER TABLE asemon_usage ADD sun_arch_data_model varchar(10) AFTER java_vm_vendor");

		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvIsSapSystem varchar(20)  AFTER srvSortOrderName");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD srvSapSystemInfo varchar(40)  AFTER srvSortOrderName");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info DROP COLUMN srvIsSapSystem");

		//doCleanup($dbconn, "ALTER TABLE asemon_usage ADD COLUMN appStartupTime varchar(10)  AFTER clientAsemonVersion");
		//doCleanup($dbconn, "ALTER TABLE sqlw_usage   ADD COLUMN appStartupTime varchar(10)  AFTER clientAppVersion");

		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info ADD sshTunnelInfo varchar(100)  AFTER srvIpPort");
		//doCleanup($dbconn, "ALTER TABLE asemon_connect_info MODIFY srvIpPort varchar(100)");

		//doCleanup($dbconn, "ALTER TABLE sqlw_usage CHANGE sqlwCheckId sqlwCheckId int not null auto_increment");

		//doCleanup($dbconn, "ALTER TABLE asemon_usage ADD clientAppName      varchar(30)     AFTER clientSourceVersion");
		//doCleanup($dbconn, "UPDATE asemon_usage SET clientAppName = 'AseTune', serverAddTime=serverAddTime");

		//doCleanup($dbconn, "ALTER TABLE asemon_usage ADD COLUMN screenResolution varchar(100)  AFTER callerIpAddress");
		//doCleanup($dbconn, "ALTER TABLE asemon_usage ADD COLUMN hiDpiScale       varchar(20)   AFTER screenResolution");

		//doCleanup($dbconn, "ALTER TABLE sqlw_usage ADD COLUMN screenResolution varchar(100)  AFTER callerIpAddress");
		//doCleanup($dbconn, "ALTER TABLE sqlw_usage ADD COLUMN hiDpiScale       varchar(20)   AFTER screenResolution");

		//doCleanup($dbconn, "
		//CREATE TABLE asemon_mda_info...
		//");
		//doCleanup($dbconn, "drop table sqlw_usage_info");
		//doCleanup($dbconn, "
		//CREATE TABLE sqlw_usage_info
		//(
		//	sqlwCheckId             int,
		//	serverAddTime           timestamp,
		//	clientTime              timestamp,
		//	userName                varchar(30),
		//
		//	connectId               int,
		//
		//	connTypeStr             varchar(30),
		//	prodName                varchar(30),
		//	srvVersionInt           int,
		//
		//	connectTime             timestamp,
		//	disconnectTime          timestamp,
		//
		//	execMainCount           int,
		//	execBatchCount          int,
		//	execTimeTotal           int,
		//	execTimeSqlExec         int,
		//	execTimeRsRead          int,
		//	rsCount                 int,
		//	rsRowsCount             int,
		//	iudRowsCount            int,
		//	sqlWarningCount         int,
		//	sqlExceptionCount       int,
		//
		//	PRIMARY KEY (sqlwCheckId, connectId, serverAddTime)
		//);
		//");


		// FIX VERSION for INT columns
//		doCleanup($dbconn, "update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 1000000 AND srvVersion > 0");
//		doCleanup($dbconn, "update asemon_mda_info         set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 1000000 AND srvVersion > 0");

		// FIX VERSION for CHAR columns
//		doCleanup($dbconn, "update asemon_error_info       set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");
//		doCleanup($dbconn, "update asemon_error_info2      set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");
//		doCleanup($dbconn, "update asemon_error_info_save  set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");

        // FIX offline-read settings...
//		doCleanup($dbconn, "update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = -1   WHERE srvVersion < 0");





		// FIX VERSION for INT columns
//		doCleanup($dbconn, "update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 1000000 AND srvVersion > 0");
//		doCleanup($dbconn, "update asemon_mda_info         set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 1000000 AND srvVersion > 0");

		// FIX VERSION for CHAR columns
//		doCleanup($dbconn, "update asemon_error_info       set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");
//		doCleanup($dbconn, "update asemon_error_info2      set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");
//		doCleanup($dbconn, "update asemon_error_info_save  set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 1000000");

        // FIX offline-read settings...
//		doCleanup($dbconn, "update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = -1   WHERE srvVersion < 0");



//---------------------------------------------------------------------
//--- make the update (for integers)
//update version_xxx
//   set ver    = ((ver/1000)*100000) + ((ver-((ver/1000)*1000))/10*100) + ((ver-((ver/1000)*1000))%10)
//where (ver < 1570050 or ver >= 1600000) AND ver < 100000000
//
//update version_xxx
//   set ver = ver*100
//where (ver/1000) >= 1570 and (ver-((ver/1000)*1000)) >= 50 AND ver < 100000000 -- 15.7 SP50 and higher
//
//
//--- make the update (for strings)
//update version_xxx
//   set verStr = cast( ((cast(verStr as int)/1000)*100000) + ((cast(verStr as int)-((cast(verStr as int)/1000)*1000))/10*100) + ((cast(verStr as int)-((cast(verStr as int)/1000)*1000))%10) as varchar(12))
//where (cast(verStr as int) < 1570050 or cast(verStr as int) >= 1600000) AND cast(verStr as int) < 100000000
//
//update version_xxx
//   set verStr = cast( cast(verStr as int)*100 as varchar(12))
//where (cast(verStr as int)/1000) >= 1570 and (cast(verStr as int)-((cast(verStr as int)/1000)*1000)) >= 50 AND cast(verStr as int) < 100000000-- 15.7 SP50 and higher
//---------------------------------------------------------------------

//sqlw_connect_info       int (srvVersionInt)
//asemon_connect_info     int
//asemon_mda_info         int
//asemon_error_info       varchar
//asemon_error_info2      varchar
//asemon_error_info_save  varchar

//		//-------------------------------------------
//		// sqlw_connect_info
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update sqlw_connect_info
//			   set srvVersionInt = ((srvVersionInt DIV 1000)*100000) + ((srvVersionInt-((srvVersionInt DIV 1000)*1000)) DIV 10*100) + ((srvVersionInt-((srvVersionInt DIV 1000)*1000)) MOD 10)
//			where (srvVersionInt < 1570050 or srvVersionInt >= 1600000) AND srvVersionInt < 100000000 AND srvVersionInt > 0
//		");
//		doCleanup($dbconn, "
//			update sqlw_connect_info
//			   set srvVersionInt = srvVersionInt*100
//			where (srvVersionInt DIV 1000) >= 1570 and (srvVersionInt-((srvVersionInt DIV 1000)*1000)) >= 50 AND srvVersionInt < 100000000 AND srvVersionInt > 0
//		");


//		//-------------------------------------------
//		// asemon_mda_info
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update asemon_mda_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion = ((srvVersion DIV 1000)*100000) + ((srvVersion-((srvVersion DIV 1000)*1000)) DIV 10*100) + ((srvVersion-((srvVersion DIV 1000)*1000)) MOD 10)
//			where (srvVersion < 1570050 or srvVersion >= 1600000) AND srvVersion < 100000000 AND srvVersion > 0
//		");
//		doCleanup($dbconn, "
//			update asemon_mda_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion = srvVersion*100
//			where (srvVersion DIV 1000) >= 1570 and (srvVersion-((srvVersion DIV 1000)*1000)) >= 50 AND srvVersion < 100000000 AND srvVersion > 0
//		");

//		//-------------------------------------------
//		// asemon_connect_info
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update asemon_connect_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion = ((srvVersion DIV 1000)*100000) + ((srvVersion-((srvVersion DIV 1000)*1000)) DIV 10*100) + ((srvVersion-((srvVersion DIV 1000)*1000)) MOD 10)
//			where (srvVersion < 1570050 or srvVersion >= 1600000) AND srvVersion < 100000000 AND srvVersion > 0
//		");
//		doCleanup($dbconn, "
//			update asemon_connect_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion = srvVersion*100
//			where (srvVersion DIV 1000) >= 1570 and (srvVersion-((srvVersion DIV 1000)*1000)) >= 50 AND srvVersion < 100000000 AND srvVersion > 0
//		");

//		//-------------------------------------------
//		// asemon_error_info
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update asemon_error_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( ((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*100000) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) DIV 10*100) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) MOD 10) ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) < 1570050 or CONVERT(srvVersion,SIGNED INTEGER) >= 1600000) AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");
//		doCleanup($dbconn, "
//			update asemon_error_info
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( CONVERT(srvVersion,SIGNED INTEGER)*100 ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) DIV 1000) >= 1570 and (CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) >= 50 AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");

//		//-------------------------------------------
//		// asemon_error_info2
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update asemon_error_info2
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( ((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*100000) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) DIV 10*100) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) MOD 10) ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) < 1570050 or CONVERT(srvVersion,SIGNED INTEGER) >= 1600000) AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");
//		doCleanup($dbconn, "
//			update asemon_error_info2
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( CONVERT(srvVersion,SIGNED INTEGER)*100 ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) DIV 1000) >= 1570 and (CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) >= 50 AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");

//		//-------------------------------------------
//		// asemon_error_info2
//		//-------------------------------------------
//		doCleanup($dbconn, "
//			update asemon_error_info_save
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( ((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*100000) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) DIV 10*100) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) MOD 10) ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) < 1570050 or CONVERT(srvVersion,SIGNED INTEGER) >= 1600000) AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");
//		doCleanup($dbconn, "
//			update asemon_error_info_save
//			   set serverAddTime = serverAddTime,
//			       srvVersion    = CAST( ( CONVERT(srvVersion,SIGNED INTEGER)*100 ) AS char(30))
//			where (CONVERT(srvVersion,SIGNED INTEGER) DIV 1000) >= 1570 and (CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) >= 50 AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");




//		//--------------------------------------------------------------------
//		// TEST SELECT for VersionUpdate to BIG  (interger columns)
//		//--------------------------------------------------------------------
//		doQuery($dbconn, "select count(*) from asemon_mda_info");
//		doQuery($dbconn, "
//			select distinct srvVersion, ((srvVersion DIV 1000)*100000) + ((srvVersion-((srvVersion DIV 1000)*1000)) DIV 10*100) + ((srvVersion-((srvVersion DIV 1000)*1000)) MOD 10)
//			from asemon_mda_info
//			where (srvVersion < 1570050 or srvVersion >= 1600000) AND srvVersion < 100000000 AND srvVersion > 0
//		");
//		doQuery($dbconn, "
//			select distinct srvVersion, srvVersion*100
//			from asemon_mda_info
//			where (srvVersion DIV 1000) >= 1570 and (srvVersion-((srvVersion DIV 1000)*1000)) >= 50 AND srvVersion < 100000000 AND srvVersion > 0
//		");
//		//--------------------------------------------------------------------
//		// TEST SELECT for VersionUpdate to BIG  (CHAR columns)
//		//--------------------------------------------------------------------
//		doQuery($dbconn, "select count(*) from asemon_error_info");
//		doQuery($dbconn, "
//			select distinct CONVERT(srvVersion,SIGNED INTEGER), ((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*100000) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) DIV 10*100) + ((CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) MOD 10)
//			from asemon_error_info
//			where (CONVERT(srvVersion,SIGNED INTEGER) < 1570050 or CONVERT(srvVersion,SIGNED INTEGER) >= 1600000) AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");
//		doQuery($dbconn, "
//			select distinct CONVERT(srvVersion,SIGNED INTEGER), CONVERT(srvVersion,SIGNED INTEGER)*100
//			from asemon_error_info
//			where (CONVERT(srvVersion,SIGNED INTEGER) DIV 1000) >= 1570 and (CONVERT(srvVersion,SIGNED INTEGER)-((CONVERT(srvVersion,SIGNED INTEGER) DIV 1000)*1000)) >= 50 AND CONVERT(srvVersion,SIGNED INTEGER) < 100000000 AND CONVERT(srvVersion,SIGNED INTEGER) > 0
//		");


//		//--------------------------------------------------------------------
//		// Remove the 'on update CURRENT_TIMESTAMP' on tables
//		//--------------------------------------------------------------------
//		doCleanup($dbconn, "ALTER TABLE sqlw_usage                CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE sqlw_connect_info         CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE sqlw_usage_info           CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_usage              CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_connect_info       CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_mda_info           CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_udc_info           CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info         CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info2        CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info_save    CHANGE serverAddTime serverAddTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ");

//		//--------------------------------------------------------------------
//		// add clientAppName to all tables
//		//--------------------------------------------------------------------
//		// ADD clientAppName
//		doCleanup($dbconn, "ALTER TABLE asemon_connect_info        ADD clientAppName varchar(30) AFTER clientTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_mda_info            ADD clientAppName varchar(30) AFTER clientTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_udc_info            ADD clientAppName varchar(30) AFTER clientTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_counter_usage_info  ADD clientAppName varchar(30) AFTER serverAddTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info          ADD clientAppName varchar(30) AFTER clientTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info2         ADD clientAppName varchar(30) AFTER clientTime");
//		doCleanup($dbconn, "ALTER TABLE asemon_error_info_save     ADD clientAppName varchar(30) AFTER clientTime");

//		// update all tables with the default value of AseTune
//		doCleanup($dbconn, "UPDATE asemon_connect_info        SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_mda_info            SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_udc_info            SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_counter_usage_info  SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_error_info          SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_error_info2         SET clientAppName = 'AseTune'");
//		doCleanup($dbconn, "UPDATE asemon_error_info_save     SET clientAppName = 'AseTune'");

//		doCleanup($dbconn, "alter table asemon_connect_info add connTypeStr       varchar(30)");
//		doCleanup($dbconn, "alter table asemon_connect_info add prodName          varchar(30)");
//		doCleanup($dbconn, "alter table asemon_connect_info add prodVersionStr    varchar(255)");
//		doCleanup($dbconn, "alter table asemon_connect_info add jdbcUrl           varchar(255)");
//		doCleanup($dbconn, "alter table asemon_connect_info add jdbcDriverClass   varchar(60)");
//		doCleanup($dbconn, "alter table asemon_connect_info add jdbcDriverName    varchar(255)");
//		doCleanup($dbconn, "alter table asemon_connect_info add jdbcDriverVersion varchar(255)");

//		doCleanup($dbconn, "ALTER TABLE asemon_connect_info CHANGE srvUserRoles  srvUserRoles  varchar(1024)");
//		doCleanup($dbconn, "ALTER TABLE asemon_connect_info CHANGE srvVersionStr srvVersionStr varchar(255)");

//		doCleanup($dbconn, "ALTER TABLE asemon_connect_info        ADD srvPageSizeInKb varchar(8) AFTER srvVersionStr");
//		doCleanup($dbconn, "ALTER TABLE sqlw_connect_info          ADD srvPageSizeInKb varchar(8) AFTER srvUser");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "check" )
	{
		describe($dbconn, "asemon_usage");
		describe($dbconn, "asemon_connect_info");
		describe($dbconn, "asemon_mda_info");
		describe($dbconn, "asemon_udc_info");
		describe($dbconn, "asemon_counter_usage_info");
		describe($dbconn, "asemon_error_info");
		describe($dbconn, "asemon_error_info2");
		describe($dbconn, "asemon_error_info_save");
		describe($dbconn, "sqlw_usage");
		describe($dbconn, "sqlw_connect_info");
		describe($dbconn, "sqlw_usage_info");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "reCreateProcs" )
	{
		installProc($dbconn, "full_mda_version_report");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "srvToLongLong" )
	{
		// exec: http://www.dbxtune.com/db_cleanup.php?doAction=srvToLongLong
//		upgradeVersion_asemon_connect_info($dbconn, 1);
//		upgradeVersion_asemon_mda_info    ($dbconn, 1);
//		upgradeVersion_sqlw_connect_info  ($dbconn, 1);
//		upgradeVersion_sqlw_usage_info    ($dbconn, 1);
		die("----END---<br>");
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
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "<h2>Other options</h2>\n";
	echo "    <A HREF=\"http://www.dbxtune.com/db_cleanup.php?doAction=errors_old_versions\" >Cleanup old versions errors: < '4.0.0'</A>\n";
?>
<BR>
-END-
<BR>

</BODE>
</HTML>
