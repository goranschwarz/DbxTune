<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="/favicon.ico"/>
<title>Asemon usage report</title>

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

<h1>Choose a Specific Report Below</h1>
<A HREF="http://www.asemon.se/usage_report.php?summary_count=true&diffReset=false"  >Summary Report, Start Count, NO RESET '<code>usageDiff</code>'</A>   <i>(check column '<code>lastPollTime</code>' to work out what '<code>usageDiff</code>' is from)</i> <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_count=true&diffReset=true"   >Summary Report, Start Count, WITH RESET '<code>usageDiff</code>'</A> <i>(only for admin, <b>so please do not use</b>)</i> <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_os=true"     >Summary Report, OS Count</A>              <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_country=true">Summary Report, Country Count</A>         <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_version=true">Summary Report, Version Count</A>         <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_asever=true" >Summary Report, ASE Version Count</A>     <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_user=true"   >Summary Report, on User</A>               <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?conn=first"          >Connection Info Report (first 500)</A>         -or- <A HREF="http://www.asemon.se/usage_report.php?conn=all">ALL</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?mda=all"             >MDA Table/Column Info, for various ASE Versions</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?udc=true"            >User Defined Counters Info Report</A>     <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?usage=first"         >Counter Usage Info Report (first 500)</A>      -or- <A HREF="http://www.asemon.se/usage_report.php?usage=all">ALL</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?errorInfo=sum"       >Error Info Report, Summary</A>                 -or- <A HREF="http://www.asemon.se/usage_report.php?errorInfo=sumSave">Saved</A> <BR>
<A HREF="http://www.asemon.se/usage_report.php?errorInfo=first"     >Error Info Report (first 500)</A>              -or- <A HREF="http://www.asemon.se/usage_report.php?errorInfo=all">ALL</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?full=true"           >Full Report (first 300)</A>               <BR>
<BR>
<h2>Admin:</h2>
DB Cleanup:
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=cleanup"      >cleanup</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=dbmaint"      >dbmaint</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=check"        >check</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=help"         >help</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=reCreateProcs">recreate stored procs</A>
<i>(only for admin, <b>so please do not use</b>)</i>

<?php
	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$rpt_summary_count      = $_GET['summary_count'];
	$rpt_summary_diffReset  = $_GET['diffReset'];
	$rpt_summary_os         = $_GET['summary_os'];
	$rpt_summary_country    = $_GET['summary_country'];
	$rpt_summary_version    = $_GET['summary_version'];
	$rpt_summary_asever     = $_GET['summary_asever'];
	$rpt_summary_user       = $_GET['summary_user'];
	$rpt_conn               = $_GET['conn'];
	$rpt_mda                = $_GET['mda'];
	$rpt_udc                = $_GET['udc'];
	$rpt_usage              = $_GET['usage'];
	$rpt_errorInfo          = $_GET['errorInfo'];
	$rpt_full               = $_GET['full'];

	$del_deleteLogId        = $_GET['deleteLogId'];
	$save_saveLogId         = $_GET['saveLogId'];

	$mda_deleteVersion      = $_GET['mda_deleteVersion'];
	$mda_verifyVersion      = $_GET['mda_verifyVersion'];
	$mda_lowVersion         = $_GET['mda_lowVersion'];
	$mda_highVersion        = $_GET['mda_highVersion'];

	// sub command
	$rpt_onDomain           = $_GET['onDomain'];
	$rpt_onUser             = $_GET['onUser'];
	$rpt_onId               = $_GET['onId'];

//	function get_ip_address() {
//	    foreach (array('HTTP_CLIENT_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_X_FORWARDED', 'HTTP_X_CLUSTER_CLIENT_IP', 'HTTP_FORWARDED_FOR', 'HTTP_FORWARDED', 'REMOTE_ADDR') as $key) {
//	        if (array_key_exists($key, $_SERVER) === true) {
//	            foreach (explode(',', $_SERVER[$key]) as $ip) {
//	                if (filter_var($ip, FILTER_VALIDATE_IP) !== false) {
//	                    return $ip;
//	                }
//	            }
//	        }
//	    }
//	}
//	$ip = get_ip_address();
//	echo "IP ADDRESS: " . $ip . "<br>\n";
//$url=file_get_contents("http://whatismyipaddress.com/ip/$ip");



	function htmlResultset($result, $headName, $colNameForNewLine='')
	{
		$colIdForNewLine = -1;
		$fields_num = mysql_num_fields($result);

		// printing some info about what this is
		echo "<h1>" . $headName . "</h1>";

		// printing table header
		echo "<table border=\"0\" class=\"sortable\">";
		echo "<tr>";
		// printing table headers
		for($i=0; $i<$fields_num; $i++)
		{
			$field = mysql_fetch_field($result);
			echo "<td nowrap>{$field->name}</td>";
			if ($colNameForNewLine != "")
			{
				if ($colNameForNewLine == "{$field->name}")
					$colIdForNewLine = $i;
			}
		}
		echo "</tr>\n";

		$atRow = -1;

		// printing table rows
		while($row = mysql_fetch_row($result))
		{
			$atRow++;
			echo "<tr>";

			// Make a new line, if a repeatable column changes content (make a separator)
			if ($colIdForNewLine >= 0)
			{
				$colValue = $row[$colIdForNewLine];

				if ($atRow >= 1)
				{
					if ($lastColValForNewLine != $colValue)
						echo "<tr> <td><br></td> </tr>";
				}
				$lastColValForNewLine = $colValue;
			}

			// $row is array... foreach( .. ) puts every element
			// of $row to $cell variable
			$col=-1;
			foreach($row as $cell)
			{
				$col++;
				$colname = mysql_field_name($result, $col);

				if ( $colname == "sybUserName" )
					echo "<td nowrap><A HREF=\"http://syberspase.sybase.com/compdir/mainMenu.do?keyword=$cell&submit=Go\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "userNameUsage" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onUser=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "domainName" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onDomain=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "wwwDomainName" )
					echo "<td nowrap><A HREF=\"http://$cell\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "checkId" || $colname == "rowid")
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "showLogId" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "deleteLogId" || $colname == "deleteLogId2" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=sum&deleteLogId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "saveLogId" || $colname == "saveLogId2" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=sum&saveLogId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "callerIpAddress" )
					echo "<td nowrap><A HREF=\"http://whatismyipaddress.com/ip/" . $cell . "\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "srvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "deleteSrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_deleteVersion=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "verifySrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_verifyVersion=" . $cell . "\">$cell</A></td>";
				else
				{
					$cellCont = nl2br($cell, false);
					echo "<td nowrap>$cellCont</td>";
				}
			}

			echo "</tr>\n";
		}
		echo "</table>\n";
		mysql_free_result($result);

		// printing table rows
//		while($row = mysql_fetch_row($result))
//		{
//			echo "<tr>";
//
//			// $row is array... foreach( .. ) puts every element
//			// of $row to $cell variable
//			foreach($row as $cell)
//				echo "<td nowrap>$cell</td>";
//
//			echo "</tr>\n";
//		}
//		echo "</table>\n";
//		mysql_free_result($result);
	}

	//-------------------------------------------
	// CONNECT to database
	//-------------------------------------------
//	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
//	mysql_select_db("asemon_stat") or die(mysql_error());

//	$db=mysql_connect("localhost", "asemon_se", "qazZSE44") or die(mysql_error());
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die(mysql_error());
	mysql_select_db("asemon_se", $db) or die(mysql_error());

	//-------------------------------------------
	// ON_DOMAIN
	//-------------------------------------------
	if ( $rpt_onDomain != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_usage
			WHERE SUBSTRING_INDEX(clientCanonicalHostName, '.', -2) = '" . $rpt_onDomain . "'
			ORDER BY serverAddTime desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "on domain $rpt_onDomain");
	}

	//-------------------------------------------
	// ON_USER
	//-------------------------------------------
	if ( $rpt_onUser != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_usage
			WHERE user_name = '" . $rpt_onUser . "'
			ORDER BY serverAddTime desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "on domain $rpt_onDomain");
	}

	//-------------------------------------------
	// ON_ID
	//-------------------------------------------
	if ( $rpt_onId != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_usage
			WHERE rowid = " . $rpt_onId . "
			ORDER BY serverAddTime desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_usage on: $rpt_onId");


		// sending query
		$result = mysql_query("SELECT * FROM asemon_connect_info WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_connect_info on: $rpt_onId");

		// sending query
		$result = mysql_query("SELECT * FROM asemon_udc_info WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_udc_info on: $rpt_onId");

		// sending query
		$result = mysql_query("
			SELECT checkId,
				serverAddTime,
				sessionType,
				sessionStartTime,
				sessionEndTime,
				TIMEDIFF(sessionEndTime, sessionStartTime) as sampleTime,
				userName,
				connectId,
				cmName,
				addSequence,
				refreshCount,
				sumRowCount,
				(sumRowCount / refreshCount) AS avgSumRowCount
			FROM asemon_counter_usage_info
			WHERE checkId = " . $rpt_onId . "
			ORDER BY connectId, sessionStartTime, addSequence
			");
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_counter_usage_info on: $rpt_onId");

		// sending query
		$result = mysql_query("SELECT * FROM asemon_error_info WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_error_info on: $rpt_onId");

		// sending query
		$result = mysql_query("SELECT * FROM asemon_error_info2 WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_error_info2 on: $rpt_onId");

		// sending query
		$result = mysql_query("SELECT * FROM asemon_error_info_save WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_error_info_save on: $rpt_onId");
	}


	//-------------------------------------------
	// SUMMARY REPORT, COUNT
	//-------------------------------------------
	if ( $rpt_summary_count == "true" )
	{
																			echo "<H2>Statistics per DAY</H2>";

																			echo "<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=0 >";
																			echo "<TR>";
																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, Start Count, per day
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y-%m-%d')  as usageDate,
				count(*)             as usageCount
			FROM asemon_usage
			GROUP BY
				DATE_FORMAT(serverAddTime, '%Y-%m-%d')
			ORDER BY
				1 desc
			LIMIT 30
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "START Count");
																			echo "	</TD>";

																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, Connect Count, per day
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y-%m-%d')  as usageDate,
				count(*)             as connectCount
			FROM asemon_connect_info
			GROUP BY
				DATE_FORMAT(serverAddTime, '%Y-%m-%d')
			ORDER BY
				1 desc
			LIMIT 30
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "ASE CONNECT Count");
																			echo "	</TD>";
																			echo "</TR>";
																			echo "</TABLE>";


																			echo "<H2>Statistics per MONTH</H2>";

																			echo "<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=0 >";
																			echo "<TR>";
																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, Start Count, per month
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y %b')  as usageDate,
				count(*)             as usageCount
			FROM asemon_usage
			GROUP BY
				DATE_FORMAT(serverAddTime, '%Y %b')
			ORDER BY
				DATE_FORMAT(serverAddTime, '%Y-%m') desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "START Count");
																			echo "	</TD>";

																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, CONNECT Count, per month
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y %b')  as usageDate,
				count(*)             as ConnectCount
			FROM asemon_connect_info
			GROUP BY
				DATE_FORMAT(serverAddTime, '%Y %b')
			ORDER BY
				DATE_FORMAT(serverAddTime, '%Y-%m') desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "ASE CONNECT Count");
																			echo "	</TD>";
																			echo "</TR>";
																			echo "</TABLE>";


		//------------------------------------------
		// Summary per domain
		//------------------------------------------
//			SELECT
//				SUBSTRING_INDEX(clientCanonicalHostName, '.', -2)  as domainName,
//				count(*)                                           as usageCount
//			FROM asemon_usage
//			GROUP BY
//				SUBSTRING_INDEX(clientCanonicalHostName, '.', -2)
//			ORDER BY
//				2 desc

		//----------- Trunacte NOW table and pupulate it again
		mysql_query("TRUNCATE TABLE sumDomainStartNow") or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO    sumDomainStartNow(domainName, usageCount, lastStarted, pollTime)
			SELECT
				SUBSTRING_INDEX(clientCanonicalHostName, '.', -2),
				count(*),
				max(serverAddTime),
				NOW()
			FROM asemon_usage
			GROUP BY
				SUBSTRING_INDEX(clientCanonicalHostName, '.', -2)
			") or die("ERROR: " . mysql_error());

		//----------- GET RESULTS & PRINT IT
		$result = mysql_query("
			SELECT n.domainName,
				CONCAT('www.', n.domainName) AS wwwDomainName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumDomainStartNow n LEFT JOIN sumDomainStartPriv p ON n.domainName = p.domainName
			ORDER BY 5 desc, 4 desc, 3 desc
			LIMIT 20");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Domian Count, ORDER BY START_TIME    TOP 20");

		//----------- SECOND RESULT
		$result = mysql_query("
			SELECT n.domainName,
				CONCAT('www.', n.domainName) AS wwwDomainName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumDomainStartNow n LEFT JOIN sumDomainStartPriv p ON n.domainName = p.domainName
			ORDER BY 4 desc, 3 desc, 5 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Domain Count");

		//----------- Move NOW table into PREV
		if ( $rpt_summary_diffReset == "true" )
		{
			mysql_query("TRUNCATE TABLE sumDomainStartPriv") or die("ERROR: " . mysql_error());
			mysql_query("INSERT INTO    sumDomainStartPriv (SELECT * FROM sumDomainStartNow)") or die("ERROR: " . mysql_error());
		}


		//------------------------------------------
		// FROM sybase.com
		//------------------------------------------
		//----------- Trunacte NOW table and pupulate it again
		mysql_query("TRUNCATE TABLE sumSybaseUsersStartNow") or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO    sumSybaseUsersStartNow(userName, usageCount, lastStarted, pollTime)
			SELECT
				user_name,
				count(*),
				max(serverAddTime),
				NOW()
			FROM asemon_usage
			WHERE
				SUBSTRING_INDEX(clientCanonicalHostName, '.', -2) = 'sybase.com'
			GROUP BY
				user_name
			") or die("ERROR: " . mysql_error());

		//----------- GET RESULTS & PRINT IT
		$result = mysql_query("
			SELECT n.userName as userNameUsage,
				n.userName as sybUserName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumSybaseUsersStartNow n LEFT JOIN sumSybaseUsersStartPriv p ON n.userName = p.userName
			ORDER BY 5 desc, 4 desc, 3 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Sybase users, Start Count, order by START_TIME");

		//----------- SECOND RESULT
		$result = mysql_query("
			SELECT n.userName as userNameUsage,
				n.userName as sybUserName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumSybaseUsersStartNow n LEFT JOIN sumSybaseUsersStartPriv p ON n.userName = p.userName
			ORDER BY 4 desc, 3 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Sybase users, Start Count");

		//----------- Move NOW table into PREV
		if ( $rpt_summary_diffReset == "true" )
		{
			mysql_query("TRUNCATE TABLE sumSybaseUsersStartPriv") or die("ERROR: " . mysql_error());
			mysql_query("INSERT INTO    sumSybaseUsersStartPriv (SELECT * FROM sumSybaseUsersStartNow)") or die("ERROR: " . mysql_error());
		}
	}


	//-------------------------------------------
	// SUMMARY REPORT, OS COUNT
	//-------------------------------------------
	if ( $rpt_summary_os == "true" )
	{
		$sql = "
			SELECT
				os_name, count(*) as usageCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_usage
			GROUP BY
				os_name
			ORDER BY
				2 desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Country Count");


		$sql = "
			SELECT
				os_name, clientAsemonVersion, count(*) as usageCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_usage
			GROUP BY
				os_name, clientAsemonVersion
			ORDER BY
				2 desc, 3 desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Country Count");

	}

	//-------------------------------------------
	// SUMMARY REPORT, COUNTRY COUNT
	//-------------------------------------------
	if ( $rpt_summary_country == "true" )
	{
		$sql = "
			SELECT
				clientAsemonVersion, user_country, user_language, count(*) as usageCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_usage
			GROUP BY
				clientAsemonVersion, user_country, user_language
			ORDER BY
				1 desc, 4 desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Country Count");
	}

	//-------------------------------------------
	// SUMMARY REPORT, VERSION COUNT
	//-------------------------------------------
	if ( $rpt_summary_version == "true" )
	{
		$sql = "
			SELECT
				clientAsemonVersion, count(*) as usageCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_usage
			GROUP BY
				clientAsemonVersion
			ORDER BY
				clientAsemonVersion desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Version Count");
	}

	//-------------------------------------------
	// SUMMARY REPORT, ASE VERSION COUNT
	//-------------------------------------------
	if ( $rpt_summary_asever == "true" )
	{
		$sql = "
			SELECT srvVersion, sum(isClusterEnabled) as clusterCount, count(*) as ConnectCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_connect_info
			GROUP BY srvVersion
			ORDER BY srvVersion desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Connected to ASE Version Count");

		$sql = "
			SELECT srvVersionStr, count(*) as ConnectCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_connect_info
			GROUP BY srvVersionStr
			ORDER BY srvVersionStr desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Connected to ASE Version Count");

		$sql = "
			SELECT srvVersionStr, count(*) as ConnectCount, max(serverAddTime) as lastUsedSrvDate
			FROM asemon_connect_info
			WHERE isClusterEnabled > 0
			GROUP BY srvVersionStr
			ORDER BY srvVersionStr desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, Connected to ASE CLUSTER Version Count");
	}

	//-------------------------------------------
	// SUMMARY REPORT, USER
	//-------------------------------------------
	if ( $rpt_summary_user == "true" )
	{
		$sql = "
			SELECT
				user_name,
				count(*)             as usageCount,
				max(serverAddTime)   as lastUsedSrvDate,
				gui,
				clientCanonicalHostName,
				os_name
			FROM asemon_usage
			GROUP BY
				user_name,
				gui,
				clientCanonicalHostName,
				os_name
			ORDER BY
				3 desc
	--			user_name,
	--			clientCanonicalHostName
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "Summary Report, On User");
	}

	//-------------------------------------------
	// CONNECTION INFO
	//-------------------------------------------
	if ( $rpt_conn != "" )
	{
		$label = "Counter Usage Info Report";
		$sql = "
			SELECT *
			FROM asemon_connect_info
			ORDER BY checkId desc
		";

		if ( $rpt_conn == "first" )
		{
			$sql   = $sql   . " LIMIT 500 ";
			$label = $label . ", first 500 rows";
		}
		else
		{
			$label = $label . ", ALL rows";
		}

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, $label);
	}



	//-------------------------------------------
	// MDA INFO
	//-------------------------------------------
	if ( $rpt_mda != "" )
	{
		if ( is_numeric($mda_deleteVersion) )
		{
			echo "<h4>Cleaning up table 'asemon_mda_info' for aseVersion: $del_deleteVersion </h4>\n";

			//---------
			$sql = "DELETE FROM asemon_mda_info WHERE srvVersion = $mda_deleteVersion";

			echo "EXEC: <code>$sql</code><br>\n";
			mysql_query($sql) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");
		}

		if ( is_numeric($mda_verifyVersion) )
		{
			echo "<h4>Verifying aseVersion: $del_deleteVersion in table 'asemon_mda_info'</h4>\n";

			//---------
			$sql = "UPDATE asemon_mda_info SET verified = 'Y' WHERE srvVersion = $mda_verifyVersion";

			echo "EXEC: <code>$sql</code><br>\n";
			mysql_query($sql) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");
		}

		//-----------------------------
		$label = "ASE Version MDA Info Summary";
		$sql = "
			SELECT srvVersion, isClusterEnabled, serverAddTime, userName, verified,
				srvVersion                AS verifySrvVersion,
				count(*)                  AS totalRows,
				'>>> TABLE >>>'           AS sep1,
				(SELECT count(*)          FROM asemon_mda_info i WHERE type = 'T' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as RowCountTables,
				(SELECT max(expectedRows) FROM asemon_mda_info i WHERE type = 'T' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as ExpectedTables,
				'>>> COLUMNS >>>'         AS sep2,
				(SELECT count(*)          FROM asemon_mda_info i WHERE type = 'C' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as RowCountCols,
				(SELECT max(expectedRows) FROM asemon_mda_info i WHERE type = 'C' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as ExpectedCols,
				'>>> SYSTABS >>>'         AS sep3,
				(SELECT count(*)          FROM asemon_mda_info i WHERE type = 'S' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as RowCountParams,
				(SELECT max(expectedRows) FROM asemon_mda_info i WHERE type = 'S' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as ExpectedParams,
				'>>> PARAMS >>>'          AS sep4,
				(SELECT count(*)          FROM asemon_mda_info i WHERE type = 'P' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as RowCountParams,
				(SELECT max(expectedRows) FROM asemon_mda_info i WHERE type = 'P' AND i.srvVersion = o.srvVersion AND i.isClusterEnabled= o.isClusterEnabled) as ExpectedParams,
				srvVersion                AS deleteSrvVersion
			FROM asemon_mda_info o
			GROUP BY srvVersion, isClusterEnabled
			ORDER BY srvVersion, isClusterEnabled
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, $label);

		//------------------------------------------
		// build a form, for Version DIFF
		//------------------------------------------
		echo '
			<b>Show new tables/columns for two differect ASE Versons</b>
			<form action="usage_report.php" method="get">
				<input type="text" size=4 name="mda" readonly="mda" value="diff" />
				Low Version:   <input type="text" size=5 maxlength=5 name="mda_lowVersion"  value="' . $mda_lowVersion  . '" />
				High Version:  <input type="text" size=5 maxlength=5 name="mda_highVersion" value="' . $mda_highVersion . '" />
				<input type="submit" />
			</form>

			<b>Get a full report of what tables and columns has been introduced in what release</b><br>
			<b>Note: Not yet implemented</b>
			<form action="usage_report.php" method="get">
				<input type="text" size=4 name="mda" readonly="mda" value="full" />
				<input type="submit" />
			</form>
		';

		//------------------------------------------
		// Full MDA Version history REPORT
		//------------------------------------------
		if ( $rpt_mda == "full" )
		{
			echo "<h1>Full MDA Version history REPORT, has NOT YET BEEN IMPLEMENTED.</h1>";

			//-----------------------------
			$label = "Full MDA Version history REPORT";
			$sql = "call full_mda_version_report";

//			// Change the params to you own here...
//			$mysql = new mysqli("localhost", "asemon_se", "UuWb3ETM", "asemon_se");
//
//			if (mysqli_connect_errno())
//			{
//				die(printf('MySQL Server connection failed: %s', mysqli_connect_error()));
//			}
//
//			/* execute multi query */
//			if ($mysqli->multi_query($sql))
//			{
//				do
//				{
//					/* store first result set */
//					if ($result = $mysqli->store_result())
//					{
//						while ($row = $result->fetch_row())
//						{
//							printf("%s\n", $row[0]);
//						}
//						$result->free();
//					}
//					/* print divider */
//					if ($mysqli->more_results())
//					{
//						printf("-----------------\n");
//					}
//				} while ($mysqli->next_result());
//			}
//			$mysql->close();


			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label);
		}

		//------------------------------------------
		// MDA DIFF REPORT
		//------------------------------------------
		if ( $rpt_mda == "diff" )
		{
			//-----------------------------
			$label = "MDA TABLE DIFF Report: low=$mda_lowVersion, High=$mda_highVersion (only new tables in HIGH Version will be visible)";
			$sql = "
				SELECT h.srvVersion,
					h.isClusterEnabled,
					h.TableName,
					h.TableID,
					h.ColumnID      as NumOfCols,
					h.Length        as NumOfParameters,
					h.Description,
					l.srvVersion
				FROM asemon_mda_info h LEFT JOIN asemon_mda_info l ON (    h.TableName  = l.TableName
				                                                       AND l.srvVersion = $mda_lowVersion
				                                                       AND l.type       = 'T')
				WHERE h.srvVersion = $mda_highVersion
				  AND h.type       = 'T'
				HAVING l.srvVersion IS NULL
				ORDER BY h.TableID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label);

			//-----------------------------
			$label = "MDA COLUMN DIFF Report: low=$mda_lowVersion, High=$mda_highVersion (only new columns in HIGH Version will be visible)";
			$sql = "
				SELECT h.srvVersion,
					h.isClusterEnabled,
					h.TableName,
					h.TableID,
					h.ColumnName,
					h.ColumnID,
					h.TypeName,
					h.Length,
					h.Indicators,
					h.Description,
					l.srvVersion
				FROM asemon_mda_info h LEFT JOIN asemon_mda_info l ON (    h.TableName  = l.TableName
				                                                       AND h.ColumnName = l.ColumnName
				                                                       AND l.srvVersion = $mda_lowVersion
				                                                       AND l.type       = 'C')
				WHERE h.srvVersion = $mda_highVersion
				  AND h.type       = 'C'
				HAVING l.srvVersion IS NULL
				ORDER BY h.TableID, h.ColumnID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label, "TableName");
		}

		//-----------------------------
		// Info for a specific VERSION
		//-----------------------------
		if ( is_numeric($rpt_mda) )
		{
			$srvVersion = $rpt_mda;

			//-----------------------------
			$label = "monTableColumns -- sysobject/syscolumns SANITY CHECK if name MATCHES (if 0 rows it's OK)";
			// alias 'm' = monTableColuns info
			// alias 's' = syscolumns info
			$sql = "
				SELECT
					'NAME MISSMATCH: Column does NOT exist in syscolumns' as NameMissMatch,
					m.srvVersion,
					m.isClusterEnabled,
					m.TableName,
					m.TableID,
					m.ColumnName,
					m.ColumnID,
					m.TypeName,
					m.Length,
					m.Indicators,
					m.Description,
					s.srvVersion
				FROM asemon_mda_info m LEFT JOIN asemon_mda_info s ON (    m.TableName  = s.TableName
				                                                       AND m.ColumnName = s.ColumnName
				                                                       AND m.srvVersion = s.srvVersion
				                                                       AND s.type       = 'S'

				                                                       )
				WHERE m.srvVersion = $srvVersion
				  AND m.type       = 'C'
				HAVING s.srvVersion IS NULL
				ORDER BY m.TableID, m.ColumnID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label, "TableName");


			//-----------------------------
			$label = "ASE Version MDA TABLE Info (ordered by TableName)";
			$sql = "
				SELECT srvVersion, isClusterEnabled, TableName, rowId, TableID, ColumnID as cols, Length as params, Description
				FROM asemon_mda_info
				WHERE type = 'T'
				  AND srvVersion = $srvVersion
				ORDER BY srvVersion, isClusterEnabled, TableName
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label);


			//-----------------------------
			$label = "MDA Info Report (TABLE)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'T'
				  AND srvVersion = $srvVersion
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label);

			//-----------------------------
			$label = "MDA Info Report (TABLE-COLUMNS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'C'
				  AND srvVersion = $srvVersion
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label, "TableName");

			//-----------------------------
			$label = "MDA Info Report (TABLE-PARAMS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'P'
				  AND srvVersion = $srvVersion
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label, "TableName");

			//-----------------------------
			$label = "MDA Info Report (SYSOBJECT/SYSCOLUMNS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'S'
				  AND srvVersion = $srvVersion
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, $label, "TableName");
		}

	}



	//-------------------------------------------
	// UDC INFO
	//-------------------------------------------
	if ( $rpt_udc == "true" )
	{
		$sql = "
			SELECT *
			FROM asemon_udc_info
			ORDER BY userName, udcKey
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, "User Defined Counters Info Report");
	}



	//-------------------------------------------
	// COUNTER USAGE INFO
	//-------------------------------------------
	if ( $rpt_usage != "" )
	{
		$label = "Counter Usage Info Report";
		$sql = "
			SELECT
				checkId,
				serverAddTime,
				sessionType,
				sessionStartTime,
				sessionEndTime,
				TIMEDIFF(sessionEndTime, sessionStartTime) as sampleTime,
				userName,
				connectId,
				addSequence,
				cmName,
				refreshCount,
				sumRowCount,
				(sumRowCount / refreshCount) AS avgSumRowCount
			FROM asemon_counter_usage_info
			ORDER BY checkId desc, connectId, sessionStartTime, addSequence
		";
		if ( $rpt_usage == "first" )
		{
			$sql   = $sql   . " LIMIT 500 ";
			$label = $label . ", first 500 rows";
		}
		else
		{
			$label = $label . ", ALL rows";
		}


		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, $label);
	}



	//-------------------------------------------
	// ERROR INFO
	//-------------------------------------------
	if ( !empty($rpt_errorInfo) || !empty($del_deleteLogId) || !empty($save_saveLogId) )
	{
		if ( is_numeric($del_deleteLogId) )
		{
			echo "<h4>Cleaning up table 'asemon_error_info', 'asemon_error_info2' and 'asemon_error_info_save' for checkId: $del_deleteLogId </h4>\n";

			//---------
			$sql = "DELETE from asemon_error_info where checkId = $del_deleteLogId";

			echo "EXEC: <code>$sql</code><br>\n";
			mysql_query($sql) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");

			//---------
			$sql = "DELETE from asemon_error_info2 where checkId = $del_deleteLogId";

			echo "EXEC: <code>$sql</code><br>\n";
			mysql_query($sql) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");

			//---------
			$sql = "DELETE from asemon_error_info_save where checkId = $del_deleteLogId";

			echo "EXEC: <code>$sql</code><br>\n";
			mysql_query($sql) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");

			//reset
			$sql = "";
		}

		if ( is_numeric($save_saveLogId) )
		{
			echo "<h4>Moving records from table 'asemon_error_info' and 'asemon_error_info2' to 'asemon_error_info_save' for checkId: $del_deleteLogId </h4>\n";

			//---------
			$sql1 = "INSERT into asemon_error_info_save    select * from asemon_error_info where checkId = $save_saveLogId";
			$sql2 = "DELETE from asemon_error_info where checkId = $save_saveLogId";

			echo "EXEC: <code>$sql1</code><br>\n";
			mysql_query($sql1) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());

			echo "EXEC: <code>$sql2</code><br>\n";
			mysql_query($sql2) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");

			//---------
			$sql1 = "INSERT into asemon_error_info_save    select * from asemon_error_info2 where checkId = $save_saveLogId";
			$sql2 = "DELETE from asemon_error_info2 where checkId = $save_saveLogId";

			echo "EXEC: <code>$sql1</code><br>\n";
			mysql_query($sql1) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());

			echo "EXEC: <code>$sql2</code><br>\n";
			mysql_query($sql2) or die("ERROR: " . mysql_error());
			printf("Records affected: %d<br>\n", mysql_affected_rows());
			printf("<br>\n");

			//reset
			$sql = "";
		}

		if ( $rpt_errorInfo == "sum" )
		{
			$sql = "
				SELECT
					checkId,
					checkId            as showLogId,
					max(serverAddTime) as maxServerAddTime,
					checkId            as deleteLogId,
					checkId            as saveLogId,
					userName,
					srvVersion,
					appVersion,
					count(sendCounter) as records,
					max(sendCounter)   as maxSendCounter
				FROM asemon_error_info
				GROUP BY checkId, userName, srvVersion, appVersion
				ORDER BY checkId desc
				LIMIT 500
			";
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo");

			$sql = "
				SELECT
					checkId,
					checkId            as showLogId,
					max(serverAddTime) as maxServerAddTime,
					checkId            as deleteLogId,
					checkId            as saveLogId,
					userName,
					srvVersion,
					appVersion,
					count(sendCounter) as records,
					max(sendCounter)   as maxSendCounter
				FROM asemon_error_info2
				GROUP BY checkId, userName, srvVersion, appVersion
				ORDER BY checkId desc
				LIMIT 500
			";
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo" . " TIMEOUT RECORDS");

			//reset
			$sql = "";
		}

		if ( $rpt_errorInfo == "sumSave" )
		{
			$sql = "
				SELECT
					checkId,
					checkId            as showLogId,
					max(serverAddTime) as maxServerAddTime,
					checkId            as deleteLogId,
					userName,
					srvVersion,
					appVersion,
					count(sendCounter) as records,
					max(sendCounter)   as maxSendCounter
				FROM asemon_error_info_save
				GROUP BY checkId, userName, srvVersion, appVersion
				ORDER BY checkId desc
				LIMIT 500
			";
		}

		if ( is_numeric($rpt_errorInfo) )
		{
			$sql = "
				SELECT 'NEW' as type,
					checkId,
					checkId as deleteLogId,
					checkId as saveLogId,
					sendCounter,
					serverAddTime,
					clientTime,
					userName,
					srvVersion,
					appVersion,
					logLevel,
					logThreadName,
					logClassName,
					logLocation,
					logMessage,
					checkId as deleteLogId2,
					checkId as saveLogId2,
					logStacktrace
				FROM asemon_error_info
				WHERE checkId = $rpt_errorInfo
				ORDER BY sendCounter
			";
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo" . " NEW RECORDS");

			$sql = "
				SELECT 'TIMEOUT' as type,
					checkId,
					checkId as deleteLogId,
					checkId as saveLogId,
					sendCounter,
					serverAddTime,
					clientTime,
					userName,
					srvVersion,
					appVersion,
					logLevel,
					logThreadName,
					logClassName,
					logLocation,
					logMessage,
					checkId as deleteLogId2,
					checkId as saveLogId2,
					logStacktrace
				FROM asemon_error_info2
				WHERE checkId = $rpt_errorInfo
				ORDER BY sendCounter
			";
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo" . " TIMEOUT RECORDS");

			$sql = "
				SELECT 'SAVED' as type,
					checkId,
					checkId as deleteLogId,
					sendCounter,
					serverAddTime,
					clientTime,
					userName,
					srvVersion,
					appVersion,
					logLevel,
					logThreadName,
					logClassName,
					logLocation,
					logMessage,
					checkId as deleteLogId2,
 					logStacktrace
				FROM asemon_error_info_save
				WHERE checkId = $rpt_errorInfo
				ORDER BY sendCounter
			";
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo" . " SAVED RECORDS");

			//reset
			$sql = "";
		}

		if ( $rpt_errorInfo == "first" )
		{
			$sql = "
				SELECT checkId,
					checkId as deleteLogId,
					checkId as saveLogId,
					sendCounter,
					serverAddTime,
					clientTime,
					userName,
					srvVersion,
					appVersion,
					logLevel,
					logThreadName,
					logClassName,
					logLocation,
					logMessage,
					checkId as deleteLogId2,
					checkId as saveLogId,
					logStacktrace
				FROM asemon_error_info
				ORDER BY checkId desc, sendCounter
				LIMIT 500
			";
		}

		if ( $rpt_errorInfo == "all" )
		{
			$sql = "
				SELECT checkId,
					checkId as deleteLogId,
					checkId as saveLogId,
					sendCounter,
					serverAddTime,
					clientTime,
					userName,
					srvVersion,
					appVersion,
					logLevel,
					logThreadName,
					logClassName,
					logLocation,
					logMessage,
					checkId as deleteLogId2,
					checkId as saveLogId,
					logStacktrace
				FROM asemon_error_info
				ORDER BY checkId desc, sendCounter
			";
		}

		if ( $sql != "" )
		{
			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($result, "ERROR Info Report: $rpt_errorInfo");
		}

	}



	//-------------------------------------------
	// FULL REPORT
	//-------------------------------------------
	if ( $rpt_full == "true" )
	{
		$sql = "
			SELECT
				rowid,

				serverAddTime,
				clientCheckTime,
				TIME_FORMAT(TIMEDIFF(clientCheckTime, serverAddTime), '%H:%i') AS swedenTimeZoneDiff,
				serverSourceVersion,

				clientSourceDate,
				clientSourceVersion,
				clientAsemonVersion,

				user_country,
				user_language,
				user_timezone,
				sun_desktop,

				clientHostName,
				clientHostAddress,
				clientCanonicalHostName,
				callerIpAddress,

				user_name,
				user_home,
				user_dir,
				propfile,
				gui,

				java_version,
				java_vm_version,
				java_vm_vendor,
				java_home,
				java_class_path,
				memory,
				os_name,
				os_version,
				os_arch
			FROM asemon_usage
			ORDER BY rowid desc
			LIMIT 300
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, "Full Report");
	}


	// Close connection to the database
	mysql_close() or die(mysql_error());
?>

<BR>
-END-
<BR>

</body>
</html>
