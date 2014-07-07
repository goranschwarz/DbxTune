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
<A HREF="http://www.asemon.se/usage_report.php?summary_os=true"                     >Summary Report, OS Count</A>              <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_country=true"                >Summary Report, Country Count</A>         <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_version=true"                >Summary Report, Version Count</A>         <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_asever=true"                 >Summary Report, ASE Version Count</A>     <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_user=true"                   >Summary Report, on User</A>               <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?conn=first"                          >Connection Info Report (first 500)</A>         -or- <A HREF="http://www.asemon.se/usage_report.php?conn=all">ALL</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?mda=all"                             >MDA Table/Column Info, for various ASE Versions</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?udc=true"                            >User Defined Counters Info Report</A>     <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?usage=first"                         >Counter Usage Info Report (first 500)</A>      -or- <A HREF="http://www.asemon.se/usage_report.php?usage=all">ALL</A> -or- <A HREF="http://www.asemon.se/usage_report.php?usage=counter&cmName=CmEngines">CmEngines</A>, <A HREF="http://www.asemon.se/usage_report.php?usage=counter&cmName=CmObjectActivity">CmObjectActivity</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?errorInfo=sum"                       >Error Info Report, Summary</A>                 -or- <A HREF="http://www.asemon.se/usage_report.php?errorInfo=sumSave">Saved</A> <BR>
<A HREF="http://www.asemon.se/usage_report.php?errorInfo=first"                     >Error Info Report (first 500)</A>              -or- <A HREF="http://www.asemon.se/usage_report.php?errorInfo=all">ALL</A> <BR>
<A HREF="http://www.asemon.se/usage_report.php?timeoutInfo=first"                   >Timeout Info Report (first 500)</A>            -or- <A HREF="http://www.asemon.se/usage_report.php?timeoutInfo=all">ALL</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?full=true"                           >Full Report (first 300)</A>               <BR>
<A HREF="http://www.asemon.se/usage_report.php?sap=true"                            >SAP Systems Report (first 300)</A>        <BR>
<A HREF="http://www.asemon.se/usage_report.php?sqlw=true&sqlwStat=true&sqlwDiffReset=false" >SQL Window (first 300)</A>,
<A HREF="http://www.asemon.se/usage_report.php?sqlw=true&sqlwStat=true&sqlwDiffReset=true"  >WITH RESET</A>                            <BR>
<A HREF="http://www.asemon.se/usage_report.php?sqlw=true&sqlwConnId=first"                  >SQL Window Connection Info (first 300)</A><BR>
<A HREF="http://www.asemon.se/usage_report.php?sqlw=true&sqlwUsageId=first"                 >SQL Window Usage Info (first 300)</A><BR>
<BR>
<h2>Admin:</h2>
DB Cleanup:
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=cleanup"                      >cleanup</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=dbmaint"                      >dbmaint</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=check"                        >check</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=help"                         >help</A>,
<A HREF="http://www.asemon.se/db_cleanup.php?doAction=reCreateProcs"                >recreate stored procs</A>
<i>(only for admin, <b>so please do not use</b>)</i>

<?php
	require("gorans_functions.php");

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$debug                  = $_GET['debug'];
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
	$rpt_usage_cmName       = $_GET['cmName'];
	$rpt_errorInfo          = $_GET['errorInfo'];
	$rpt_timeoutInfo        = $_GET['timeoutInfo'];
	$rpt_full               = $_GET['full'];
	$rpt_sap                = $_GET['sap'];

	$rpt_sqlw               = $_GET['sqlw'];
	$rpt_sqlw_stat          = $_GET['sqlwStat'];
	$rpt_sqlw_diffReset     = $_GET['sqlwDiffReset'];
	$rpt_sqlw_ConnId        = $_GET['sqlwConnId'];
	$rpt_sqlw_UsageId       = $_GET['sqlwUsageId'];

	$del_deleteLogId        = $_GET['deleteLogId'];
	$save_saveLogId         = $_GET['saveLogId'];

	$mda_deleteVersion      = versionFix($_GET['mda_deleteVersion']);
	$mda_verifyVersion      = versionFix($_GET['mda_verifyVersion']);
	$mda_lowVersion         = versionFix($_GET['mda_lowVersion']);
	$mda_highVersion        = versionFix($_GET['mda_highVersion']);
	$mda_isCluster          = $_GET['mda_isCluster'];

	$ipDesc_key             = $_GET['ipDesc_key'];
	$ipDesc_val             = $_GET['ipDesc_val'];

	$userId_key             = $_GET['userId_key'];
	$userId_val             = $_GET['userId_val'];
	$userId_query           = $_GET['userId_query'];

	// sub command
	$rpt_onDomain            = $_GET['onDomain'];
	$rpt_onUser              = $_GET['onUser'];
	$rpt_onId                = $_GET['onId'];
	$rpt_getAppStartsForIp   = $_GET['getAppStartsForIp'];
	$rpt_getConnectForIp     = $_GET['getConnectForIp'];
	$rpt_getConnectForDomain = $_GET['getConnectForDomain'];

	$rpt_userName            = $_GET['userName'];


//	if ( $rpt_userName != "" )
//	{
////		$tmp = substr($rpt_userName, 1);
//
//		// IF its a SAP USER: then lookup something
//		// else: lookup what sessions this user has been done
//		if ( preg_match('/^[iIdD][0-9]{6}$/', $$rpt_userName) )
//		{
//			http_redirect("https://sapneth1.wdf.sap.corp/~form/handler?_APP=00200682500000002283&_EVENT=DISPLAY&00200682500000002187=" . $rpt_userName);
//			exit;
//		}
//		else
//		{
//			echo "<br>NOT-YET-SUPPORTED: userName=" . $rpt_userName;
//			exit;
//		}
//	}

	//-------------------------------------------
	// CONNECT to database
	//-------------------------------------------
//	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
//	mysql_select_db("asemon_stat") or die(mysql_error());

//	$db=mysql_connect("localhost", "asemon_se", "qazZSE44") or die(mysql_error());
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die(mysql_error());
	mysql_select_db("asemon_se", $db) or die(mysql_error());



	//------------------------------------------
	// build a form, for userIdDescription
	//------------------------------------------
	echo '<br><br>
		<b>Assign a UserName/Description for UserId: </b>
		<form action="usage_report.php" method="get">
			UserName:    <input type="text" size=20 maxlength=20  name="userId_key"   value="" />
			Description: <input type="text" size=60 maxlength=100 name="userId_val"   value="" />
		    Query:       <input type="text" size=5  maxlength=20  name="userId_query" value="*" />
			<input type="submit" value="Submit"/>
		</form>
	';
//	mysql_query("CREATE TABLE userIdDescription (userId varchar(20), description varchar(100))")  or die("ERROR: " . mysql_error());

	//------------------------------------------
	// Get a CACHE for userIdDescription, which can be used as a lookup table
	//------------------------------------------
	$userIdCache = array();
	$result = mysql_query("select upper(userId) as userId, description from userIdDescription");
	if ($result)
	{
		while ($row = mysql_fetch_assoc($result))
		{
			$userIdCache[ $row['userId'] ] = $row['description'];
		}
	}
	else
	{
		echo mysql_errno() . ": " . mysql_error() . "<br>";
	}
//	print_r($userIdCache);
//	echo "<br>";
//	echo "i063783=|" . $userIdCache['i063783'] . "|<br>";
//	echo "I063783=|" . $userIdCache['I063783'] . "|<br>";


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
		htmlResultset($userIdCache, $result, "on domain $rpt_onDomain");
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
		htmlResultset($userIdCache, $result, "on domain $rpt_onDomain");
	}

	//-------------------------------------------
	// APP-STARTS ON_CALLER_IP
	//-------------------------------------------
	if ( $rpt_getAppStartsForIp != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_usage
			WHERE callerIpAddress = '" . $rpt_getAppStartsForIp . "'
			ORDER BY serverAddTime desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "AppStarts MADE BY Caller IP Address $rpt_getAppStartsForIp");
	}

	//-------------------------------------------
	// CONNECTIONS ON_CALLER_IP
	//-------------------------------------------
	if ( $rpt_getConnectForIp != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_connect_info
			WHERE checkId IN (SELECT rowid
			                  FROM asemon_usage
			                  WHERE callerIpAddress = '" . $rpt_getConnectForIp . "'
			                 )
			ORDER BY checkId desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "CONNECTIONS MADE BY Caller IP Address $rpt_getConnectForIp");
	}

	//-------------------------------------------
	// CONNECTIONS ON_DOMAIN
	//-------------------------------------------
	if ( $rpt_getConnectForDomain != "" )
	{
		$sql = "
			SELECT *
			FROM asemon_connect_info
			WHERE checkId IN (SELECT rowid
			                  FROM asemon_usage
			                  WHERE SUBSTRING_INDEX(clientCanonicalHostName, '.', -2) = '" . $rpt_getConnectForDomain . "'
			                 )
			ORDER BY checkId desc
			";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "CONNECTIONS MADE BY Domain $rpt_getConnectForDomain");
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
		htmlResultset($userIdCache, $result, "asemon_usage on: $rpt_onId");


		// sending query
		$result = mysql_query("SELECT * FROM asemon_connect_info WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "asemon_connect_info on: $rpt_onId");

		// sending query
		$result = mysql_query("SELECT * FROM asemon_udc_info WHERE checkId = " . $rpt_onId);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "asemon_udc_info on: $rpt_onId");

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
		htmlResultset($userIdCache, $result, "asemon_counter_usage_info on: $rpt_onId");

		// NEW ERRORS
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
			WHERE checkId = $rpt_onId
			ORDER BY sendCounter
		";
		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " NEW RECORDS");

		// TIMEOUT ERRORS
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
			WHERE checkId = $rpt_onId
			ORDER BY sendCounter
		";
		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " TIMEOUT RECORDS");

		// SAVED ERRORS
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
			WHERE checkId = $rpt_onId
			ORDER BY sendCounter
		";
		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " SAVED RECORDS");

		//reset
		$sql = "";
	}


	//-------------------------------------------
	// IP CALLER, assign a description
	//-------------------------------------------
	if ( $ipDesc_key != "" )
	{
		mysql_query("DELETE FROM callerIpDescription WHERE callerIpAddress = '" . $ipDesc_key . "'")         or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO callerIpDescription values('" . $ipDesc_key . "', '" . $ipDesc_val . "')")  or die("ERROR: " . mysql_error());

		$sql = "SELECT * FROM callerIpDescription";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Caller IP Descriptions");
	}


	//-------------------------------------------
	// USER ID, assign a description
	//-------------------------------------------
	if ( $userId_key != "" || $userId_query != "" )
	{
		// get rid of leading spaces etc
		$userId_key = trim($userId_key);
		$userId_val = trim($userId_val);


//		mysql_query("DELETE FROM userIdDescription")         or die("ERROR: " . mysql_error());
		mysql_query("DELETE FROM userIdDescription where userId = ''")         or die("ERROR: " . mysql_error());

		echo "Actions taken:";
		echo "<ul>";
		if ( $userId_key != "" )
		{
			echo "<li><i>Trying to delete user '" . $userId_key . "' (if it exists)</i></li>";
			mysql_query("DELETE FROM userIdDescription WHERE upper(userId) = upper('" . $userId_key . "')")         or die("ERROR: " . mysql_error());
		}

		if ( $userId_key != "" && $userId_val != "" )
		{
			echo "<li><i>Adding userId: '" . $userId_key . "', with description '" . $userId_val . "'</i></li>";
			mysql_query("INSERT INTO userIdDescription values(upper('" . $userId_key . "'), '" . $userId_val . "')")  or die("ERROR: " . mysql_error());
		}

		//echo "userId_query: (" . $userId_query . ")<br>";
		$sqlWhere = "";
		if ( $userId_query != "" && $userId_query != "*")
		{
			$sqlWhere = " WHERE upper(userId) like upper('%" . $userId_query . "%')";
		}

		$sql = "SELECT userId, description, upper(userId) as deleteUserIdDesc, userId as user_name FROM userIdDescription" . $sqlWhere . " order by description";
		echo "<li>Getting records using SQL: <code>" . $sql . "</code></li>";
		echo "</ul>";

		// sending query
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "User ID Descriptions/Translations");


		$sql = "SELECT count(*) as DescriptionCount FROM userIdDescription" . $sqlWhere;
		$result = mysql_query($sql);
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Number of records in Translation Table");
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
		htmlResultset($userIdCache, $result, "START Count");
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
		htmlResultset($userIdCache, $result, "ASE CONNECT Count");
																			echo "	</TD>";

																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, Start Count, per day - WITHOUT SOME DOMAINS
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y-%m-%d')  as usageDate,
				count(*)             as usageCount
			FROM asemon_usage
			WHERE clientHostName <> 'asetune-virtual-machine'
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
		htmlResultset($userIdCache, $result, "START Count WITHOUT 'asetune-virtual-machine'");
																			echo "	</TD>";

																			echo "	<TD VALIGN=\"top\">";
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
		htmlResultset($userIdCache, $result, "START Count");
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
		htmlResultset($userIdCache, $result, "ASE CONNECT Count");
																			echo "	</TD>";

																			echo "	<TD VALIGN=\"top\">";
		//------------------------------------------
		// Summary Report, Start Count, per month - WITHOUT SOME DOMAINS
		//------------------------------------------
		$sql = "
			SELECT
				DATE_FORMAT(serverAddTime, '%Y %b')  as usageDate,
				count(*)             as usageCount
			FROM asemon_usage
			WHERE clientHostName <> 'asetune-virtual-machine'
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
		htmlResultset($userIdCache, $result, "START Count WITHOUT 'asetune-virtual-machine'");
																			echo "	</TD>";
																			echo "</TR>";
																			echo "</TABLE>";


		//------------------------------------------
		// Summary per: CALLER-IP-ADDRESS
		//------------------------------------------
//		$result = mysql_query("SELECT @@MAX_JOIN_SIZE");
//		htmlResultset($userIdCache, $result, "SELECT @@MAX_JOIN_SIZE");
//echo "XXX: 1<br>";
//		$result = mysql_query("select count(*) from sumCallerIpStartNow");
//		htmlResultset($userIdCache, $result, "sumCallerIpStartNow");

//		$result = mysql_query("select count(*) from sumCallerIpStartPriv");
//		htmlResultset($userIdCache, $result, "sumCallerIpStartPriv");

//		$result = mysql_query("select count(*) from callerIpDescription");
//		htmlResultset($userIdCache, $result, "callerIpDescription");


		mysql_query("SET SQL_BIG_SELECTS=1") or die("ERROR: " . mysql_error());

//		mysql_query("CREATE TABLE sumCallerIpStartNow (callerIpAddress varchar(20), usageCount int, lastStarted timestamp, pollTime timestamp)") or die("ERROR: " . mysql_error());
//		mysql_query("CREATE TABLE sumCallerIpStartPriv(callerIpAddress varchar(20), usageCount int, lastStarted timestamp, pollTime timestamp)") or die("ERROR: " . mysql_error());
//		mysql_query("CREATE TABLE callerIpDescription (callerIpAddress varchar(20), description varchar(50))")                                          or die("ERROR: " . mysql_error());

		//----------- Trunacte NOW table and pupulate it again
		mysql_query("TRUNCATE TABLE sumCallerIpStartNow") or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO    sumCallerIpStartNow(callerIpAddress, usageCount, lastStarted, pollTime)
			SELECT
--				callerIpAddress,
				COALESCE(callerIpAddress,'**********'),
				count(*),
				max(serverAddTime),
				NOW()
			FROM asemon_usage
--			WHERE callerIpAddress IS NOT NULL
			GROUP BY
				callerIpAddress
			") or die("ERROR: " . mysql_error());

//echo "XXX: 2<br>";
		//----------- GET RESULTS & PRINT IT
		$result = mysql_query("
			SELECT n.callerIpAddress,
				n.callerIpAddress AS getAppStartsForIp,
				n.callerIpAddress AS getConnectForIp,
				COALESCE(d.description,'**********') AS Description,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumCallerIpStartNow n
				LEFT JOIN sumCallerIpStartPriv p ON n.callerIpAddress = p.callerIpAddress
				LEFT JOIN callerIpDescription  d ON n.callerIpAddress = d.callerIpAddress
			ORDER BY 7 desc, 6 desc, 5 desc
			LIMIT 30");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Caller IP ADDRESS, ORDER BY START_TIME    TOP 30");

//echo "XXX: 3<br>";
		//----------- Move NOW table into PREV
		if ( $rpt_summary_diffReset == "true" )
		{
			mysql_query("TRUNCATE TABLE sumCallerIpStartPriv") or die("ERROR: " . mysql_error());
			mysql_query("INSERT INTO    sumCallerIpStartPriv (SELECT * FROM sumCallerIpStartNow)") or die("ERROR: " . mysql_error());
		}

		//------------------------------------------
		// build a form, for callerIpDescription
		//------------------------------------------
		echo '
			<b>Assign a description for: </b>
			<form action="usage_report.php" method="get">
				IP:          <input type="text" size=20 maxlength=20 name="ipDesc_key" value="" />
				Description: <input type="text" size=50 maxlength=50 name="ipDesc_val" value="" />
				<input type="submit" />
			</form>
		';


		//------------------------------------------
		// SUMMARY PER DOMAIN
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
				n.domainName AS getConnectForDomain,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumDomainStartNow n LEFT JOIN sumDomainStartPriv p ON n.domainName = p.domainName
			ORDER BY 6 desc, 5 desc, 4 desc
			LIMIT 30");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Domian Count, ORDER BY START_TIME    TOP 30");

		//----------- SECOND RESULT
		$result = mysql_query("
			SELECT n.domainName,
				CONCAT('www.', n.domainName) AS wwwDomainName,
				n.domainName AS getConnectForDomain,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumDomainStartNow n LEFT JOIN sumDomainStartPriv p ON n.domainName = p.domainName
			ORDER BY 5 desc, 4 desc, 6 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Domain Count");

		//----------- Move NOW table into PREV
		if ( $rpt_summary_diffReset == "true" )
		{
			mysql_query("TRUNCATE TABLE sumDomainStartPriv") or die("ERROR: " . mysql_error());
			mysql_query("INSERT INTO    sumDomainStartPriv (SELECT * FROM sumDomainStartNow)") or die("ERROR: " . mysql_error());
		}




		//------------------------------------------
		// FROM sybase.com and sap.corp
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
				OR SUBSTRING_INDEX(clientCanonicalHostName, '.', -2) = 'sap.corp'
			GROUP BY
				user_name
			") or die("ERROR: " . mysql_error());

		//----------- GET RESULTS & PRINT IT
		$result = mysql_query("
			SELECT n.userName as userNameUsage,
				n.userName as sybUserName,
				n.userName as sapUserName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumSybaseUsersStartNow n LEFT JOIN sumSybaseUsersStartPriv p ON n.userName = p.userName
			ORDER BY 6 desc, 5 desc, 4 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Sybase and SAP users, Start Count, order by START_TIME");

		//----------- SECOND RESULT
		$result = mysql_query("
			SELECT n.userName as userNameUsage,
				n.userName as sybUserName,
				n.userName as sapUserName,
				n.usageCount AS usageNow,
				n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
				n.lastStarted,
				p.pollTime AS lastPollTime
			FROM sumSybaseUsersStartNow n LEFT JOIN sumSybaseUsersStartPriv p ON n.userName = p.userName
			ORDER BY 4 desc");

		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Sybase and SAP users, Start Count");

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
		htmlResultset($userIdCache, $result, "Summary Report, Country Count");


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
		htmlResultset($userIdCache, $result, "Summary Report, Country Count");

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
		htmlResultset($userIdCache, $result, "Summary Report, Country Count");
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
		htmlResultset($userIdCache, $result, "Summary Report, Version Count");
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
		htmlResultset($userIdCache, $result, "Summary Report, Connected to ASE Version Count");

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
		htmlResultset($userIdCache, $result, "Summary Report, Connected to ASE Version Count");

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
		htmlResultset($userIdCache, $result, "Summary Report, Connected to ASE CLUSTER Version Count");
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
		htmlResultset($userIdCache, $result, "Summary Report, On User");
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
		htmlResultset($userIdCache, $result, $label);
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
		htmlResultset($userIdCache, $result, $label);

		//------------------------------------------
		// build a form, for Version DIFF
		//------------------------------------------
		echo '
			<b>Show new tables/columns for two differect ASE Versons</b>
			<form action="usage_report.php" method="get">
				<input type="text" size=4 name="mda" readonly="mda" value="diff" />
				Is ASE Cluster Edition (0 or 1, default=0):<input type="text" size=5  maxlength=5  name="mda_isCluster"   value="' . $mda_isCluster   . '" />
				Low Version:                               <input type="text" size=12 maxlength=12 name="mda_lowVersion"  value="' . $mda_lowVersion  . '" />
				High Version:                              <input type="text" size=12 maxlength=12 name="mda_highVersion" value="' . $mda_highVersion . '" />
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
			htmlResultset($userIdCache, $result, $label);
		}

		//------------------------------------------
		// MDA DIFF REPORT
		//------------------------------------------
		if ( $rpt_mda == "diff" )
		{
			if ( $mda_isCluster == "" )
			{
				$mda_isCluster = 0;
			}

			//-----------------------------
			$label = "MDA TABLE DIFF Report: isCluster=$mda_isCluster, low=$mda_lowVersion, High=$mda_highVersion (only new tables in HIGH Version will be visible)";
			$sql = "
				SELECT h.srvVersion,
					h.isClusterEnabled,
					h.TableName,
					h.TableID,
					h.ColumnID      as NumOfCols,
					h.Length        as NumOfParameters,
					h.Description,
					l.srvVersion
				FROM asemon_mda_info h LEFT JOIN asemon_mda_info l ON (    h.TableName        = l.TableName
				                                                       AND l.isClusterEnabled = $mda_isCluster
				                                                       AND l.srvVersion       = $mda_lowVersion
				                                                       AND l.type             = 'T')
				WHERE h.srvVersion       = $mda_highVersion
				  AND h.isClusterEnabled = $mda_isCluster
				  AND h.type             = 'T'
				HAVING l.srvVersion IS NULL
				ORDER BY h.TableID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label);

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
				FROM asemon_mda_info h LEFT JOIN asemon_mda_info l ON (    h.TableName        = l.TableName
				                                                       AND h.ColumnName       = l.ColumnName
				                                                       AND l.isClusterEnabled = $mda_isCluster
				                                                       AND l.srvVersion       = $mda_lowVersion
				                                                       AND l.type             = 'C')
				WHERE h.srvVersion       = $mda_highVersion
				  AND h.isClusterEnabled = $mda_isCluster
				  AND h.type             = 'C'
				HAVING l.srvVersion IS NULL
				ORDER BY h.TableID, h.ColumnID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label, "TableName");
		}

		//-----------------------------
		// Info for a specific VERSION
		//-----------------------------
		if ( is_numeric($rpt_mda) )
		{
			$srvVersion = versionFix($rpt_mda);
			if ( $mda_isCluster == "" )
			{
				$mda_isCluster = 0;
			}

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
				FROM asemon_mda_info m LEFT JOIN asemon_mda_info s ON (    m.TableName        = s.TableName
				                                                       AND m.ColumnName       = s.ColumnName
				                                                       AND m.isClusterEnabled = s.isClusterEnabled
				                                                       AND m.srvVersion       = s.srvVersion
				                                                       AND s.type             = 'S'
				                                                       )
				WHERE m.srvVersion       = $srvVersion
				  AND m.isClusterEnabled = $mda_isCluster
				  AND m.type             = 'C'
				HAVING s.srvVersion IS NULL
				ORDER BY m.TableID, m.ColumnID
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label, "TableName");


			//-----------------------------
			$label = "ASE Version MDA TABLE Info (ordered by TableName)";
			$sql = "
				SELECT srvVersion, isClusterEnabled, TableName, rowId, TableID, ColumnID as cols, Length as params, Description
				FROM asemon_mda_info
				WHERE type = 'T'
				  AND srvVersion       = $srvVersion
				  AND isClusterEnabled = $mda_isCluster
				ORDER BY srvVersion, isClusterEnabled, TableName
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label);


			//-----------------------------
			$label = "MDA Info Report (TABLE)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'T'
				  AND srvVersion       = $srvVersion
				  AND isClusterEnabled = $mda_isCluster
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label);

			//-----------------------------
			$label = "MDA Info Report (TABLE-COLUMNS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'C'
				  AND srvVersion       = $srvVersion
				  AND isClusterEnabled = $mda_isCluster
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label, "TableName");

			//-----------------------------
			$label = "MDA Info Report (TABLE-PARAMS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'P'
				  AND srvVersion       = $srvVersion
				  AND isClusterEnabled = $mda_isCluster
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label, "TableName");

			//-----------------------------
			$label = "MDA Info Report (SYSOBJECT/SYSCOLUMNS)";
			$sql = "
				SELECT *
				FROM asemon_mda_info
				WHERE type = 'S'
				  AND srvVersion       = $srvVersion
				  AND isClusterEnabled = $mda_isCluster
				ORDER BY srvVersion, isClusterEnabled, rowId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, $label, "TableName");

			//-----------------------------
			if ( $debug != "" )
			{
				$label = "select * FROM asemon_mda_info WHERE srvVersion = $srvVersion";
				$sql = "
					SELECT *
					FROM asemon_mda_info
					WHERE srvVersion       = $srvVersion
				      AND isClusterEnabled = $mda_isCluster
					ORDER BY rowId
				";

				// sending query
				$result = mysql_query($sql) or die("ERROR: " . mysql_error());
				if (!$result) {
					die("Query to show fields from table failed");
				}
				htmlResultset($userIdCache, $result, $label, "TableName");
			}
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
			ORDER BY serverAddTime desc, userName, udcKey
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "User Defined Counters Info Report");
	}



	//-------------------------------------------
	// COUNTER USAGE INFO
	//-------------------------------------------
	if ( $rpt_usage != "" )
	{
		if ( $rpt_usage_cmName != "" )
		{
			$label = "Counter Usage Info Report, first 500 ENGINES counters";
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
				WHERE cmName = '$rpt_usage_cmName'
				ORDER BY checkId desc, connectId, sessionStartTime, addSequence
			";
//				LIMIT 1000
		}
		else
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
		}


		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, $label);
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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo");

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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " TIMEOUT RECORDS");

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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " NEW RECORDS");

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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " TIMEOUT RECORDS");

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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo" . " SAVED RECORDS");

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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo");
		}

	}


	//-------------------------------------------
	// TIMEOUT INFO
	//-------------------------------------------
	if ( !empty($rpt_timeoutInfo) )
	{
		if ( $rpt_timeoutInfo == "first" )
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
				FROM asemon_error_info2
				ORDER BY checkId desc, sendCounter
				LIMIT 500
			";
		}

		if ( $rpt_timeoutInfo == "all" )
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
				FROM asemon_error_info2
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
			htmlResultset($userIdCache, $result, "ERROR Info Report: $rpt_errorInfo");
		}

	}


	//-------------------------------------------
	// FULL REPORT
	//-------------------------------------------
	if ( $rpt_full == "true" )
	{
//		$sql = "
//			SELECT
//				rowid,
//
//				serverAddTime,
//				clientCheckTime,
//				TIME_FORMAT(TIMEDIFF(clientCheckTime, serverAddTime), '%H:%i') AS swedenTimeZoneDiff,
//				serverSourceVersion,
//
//				clientSourceDate,
//				clientSourceVersion,
//				clientAsemonVersion,
//
//				user_country,
//				user_language,
//				user_timezone,
//				sun_desktop,
//
//				clientHostName,
//				clientHostAddress,
//				clientCanonicalHostName,
//				callerIpAddress,
//
//				user_name,
//				user_home,
//				user_dir,
//				propfile,
//				gui,
//
//				java_version,
//				java_vm_version,
//				java_vm_vendor,
//				sun_arch_data_model,
//				java_home,
//				java_class_path,
//				memory,
//				os_name,
//				os_version,
//				os_arch
//			FROM asemon_usage
//			ORDER BY rowid desc
//			LIMIT 300
//		";
		$sql = "
			SELECT *
			FROM asemon_usage
			ORDER BY rowid desc
			LIMIT 300
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "Full Report");
	}


	//-------------------------------------------
	// FULL REPORT
	//-------------------------------------------
	if ( $rpt_sap == "true" )
	{
		$sql = "
			SELECT *
			FROM asemon_usage
			WHERE user_name like 'sap%'
			ORDER BY rowid desc
			LIMIT 300
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "SAP asemon_usage Report");

		$sql = "
			SELECT *
			FROM asemon_connect_info
			WHERE srvUser like 'sap%'
			ORDER BY checkId desc
			LIMIT 300
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($userIdCache, $result, "SAP asemon_connect_info Report");
	}



	//-------------------------------------------
	// SQL Window
	//-------------------------------------------
	if ( $rpt_sqlw == "true" )
	{
		if ( $rpt_sqlw_ConnId == "first")
		{
			//------------------------------------------
			// SQLW_CONNECT_INFO ALL
			//------------------------------------------
			$sql = "
				SELECT *
				FROM sqlw_connect_info
				ORDER BY sqlwCheckId desc
				LIMIT 300
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "SQL Window Connection Info");
		}

		if ( $rpt_sqlw_UsageId == "first")
		{
			//------------------------------------------
			// SQLW_USAGE_INFO ALL
			//------------------------------------------
			$sql = "
				SELECT
					sqlwCheckId,
					serverAddTime,
					clientTime,
					userName,
					connectId,
					connTypeStr,
					prodName,
					srvVersionInt,
					connectTime,
					disconnectTime,
					TIMEDIFF(disconnectTime, connectTime) as connectDuration,
					execMainCount,
					execBatchCount,
					execTimeTotal,
					execTimeSqlExec,
					execTimeRsRead,
					rsCount,
					rsRowsCount,
					iudRowsCount,
					sqlWarningCount,
					sqlExceptionCount
				FROM sqlw_usage_info
				ORDER BY sqlwCheckId desc
				LIMIT 300
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "SQL Window Usage Info");
		}

		// a SPECIFIC SQLW session
		if ( is_numeric($rpt_sqlw_ConnId) )
		{
			//------------------------------------------
			// sqlw_usage on ID
			//------------------------------------------
			$sql = "
				SELECT *
				FROM sqlw_usage
				where sqlwCheckId = $rpt_sqlw_ConnId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "sqlw_usage on: $rpt_sqlw_ConnId");

			//------------------------------------------
			// sqlw_connect_info on ID
			//------------------------------------------
			$sql = "
				SELECT *
				FROM sqlw_connect_info
				where sqlwCheckId = $rpt_sqlw_ConnId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "sqlw_connect_info on: $rpt_sqlw_ConnId");

			//------------------------------------------
			// sqlw_usage_info on ID
			//------------------------------------------
			$sql = "
				SELECT
					sqlwCheckId,
					serverAddTime,
					clientTime,
					userName,
					connectId,
					connTypeStr,
					prodName,
					srvVersionInt,
					connectTime,
					disconnectTime,
					TIMEDIFF(disconnectTime, connectTime) as connectDuration,
					execMainCount,
					execBatchCount,
					execTimeTotal,
					execTimeSqlExec,
					execTimeRsRead,
					rsCount,
					rsRowsCount,
					iudRowsCount,
					sqlWarningCount,
					sqlExceptionCount
				FROM sqlw_usage_info
				where sqlwCheckId = $rpt_sqlw_ConnId
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "sqlw_usage_info on: $rpt_sqlw_ConnId");
		}


		if ( $rpt_sqlw_stat == "true")
		{

			echo "<H2>Statistics per DAY</H2>";
			//------------------------------------------
			// Summary Report, Start Count, per day
			//------------------------------------------
			$sql = "
				SELECT
					DATE_FORMAT(serverAddTime, '%Y-%m-%d')  as usageDate,
					count(*)             as usageCount
				FROM sqlw_usage
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
			htmlResultset($userIdCache, $result, "START Count");


			echo "<H2>Statistics per MONTH</H2>";
			//------------------------------------------
			// Summary Report, Start Count, per month
			//------------------------------------------
			$sql = "
				SELECT
					DATE_FORMAT(serverAddTime, '%Y %b')  as usageDate,
					count(*)             as usageCount
				FROM sqlw_usage
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
			htmlResultset($userIdCache, $result, "START Count");

			//------------------------------------------
			// Summary per: user_name
			//------------------------------------------
	//		mysql_query("CREATE TABLE sumSqlwStartNow (user_name varchar(20), usageCount int, lastStarted timestamp, pollTime timestamp)") or die("ERROR: " . mysql_error());
	//		mysql_query("CREATE TABLE sumSqlwStartPriv(user_name varchar(20), usageCount int, lastStarted timestamp, pollTime timestamp)") or die("ERROR: " . mysql_error());

			//----------- Trunacte NOW table and pupulate it again
			mysql_query("TRUNCATE TABLE sumSqlwStartNow") or die("ERROR: " . mysql_error());
			mysql_query("INSERT INTO    sumSqlwStartNow(user_name, usageCount, lastStarted, pollTime)
				SELECT
					user_name,
					count(*),
					max(serverAddTime),
					NOW()
				FROM sqlw_usage
				GROUP BY
					user_name
				") or die("ERROR: " . mysql_error());

	//		$result = mysql_query("select count(*) from sumSqlwStartNow");
	//		htmlResultset($userIdCache, $result, "sumSqlwStartNow");
	//		$result = mysql_query("select * from sumSqlwStartNow order by lastStarted desc");
	//		htmlResultset($userIdCache, $result, "SqlwStartNow");

	//		$result = mysql_query("select count(*) from sumSqlwStartPriv");
	//		htmlResultset($userIdCache, $result, "sumSqlwStartPriv");
	//		$result = mysql_query("select * from sumSqlwStartPriv order by lastStarted desc");
	//		htmlResultset($userIdCache, $result, "SqlwStartPriv");

	//echo "XXX: 2<br>";
			//----------- GET RESULTS & PRINT IT
			$result = mysql_query("
				SELECT
					n.user_name,
					n.user_name as sapUserName,
					n.usageCount AS usageNow,
					n.usageCount - IFNULL(p.usageCount,0) AS usageDiff,
					n.lastStarted,
					p.pollTime AS lastPollTime
				FROM sumSqlwStartNow n LEFT JOIN sumSqlwStartPriv p ON n.user_name = p.user_name
				ORDER BY 5 desc, 4 desc
				LIMIT 30");

			if (!$result) {
				echo mysql_errno() . ": " . mysql_error() . "<br>";
				die("ERROR: Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "SQLW USER USAGE, ORDER BY START_TIME    TOP 30");

	//echo "XXX: 3<br>";
			//----------- Move NOW table into PREV
			if ( $rpt_sqlw_diffReset == "true" )
			{
				mysql_query("TRUNCATE TABLE sumSqlwStartPriv") or die("ERROR: " . mysql_error());
				mysql_query("INSERT INTO    sumSqlwStartPriv (SELECT * FROM sumSqlwStartNow)") or die("ERROR: " . mysql_error());
			}





			//------------------------------------------
			// SQLW ALL
			//------------------------------------------
			$sql = "
				SELECT *
				FROM sqlw_usage
				ORDER BY sqlwCheckId desc
				LIMIT 300
			";

			// sending query
			$result = mysql_query($sql) or die("ERROR: " . mysql_error());
			if (!$result) {
				die("Query to show fields from table failed");
			}
			htmlResultset($userIdCache, $result, "SQL Window Usage Report");
		}

	}




	// Close connection to the database
	mysql_close() or die(mysql_error());
?>

<BR>
-END-
<BR>

</body>
</html>
