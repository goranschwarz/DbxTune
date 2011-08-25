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
<A HREF="http://www.asemon.se/usage_report.php?summary=true&conn=true&udc=true&usage=true&full=true">All below reports</A> <BR>
<BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_count=true">Summary Report, Start Count</A>      <BR>
<A HREF="http://www.asemon.se/usage_report.php?summary_user=true" >Summary Report, on User</A>          <BR>
<A HREF="http://www.asemon.se/usage_report.php?conn=true"         >Connection Info Report</A>           <BR>
<A HREF="http://www.asemon.se/usage_report.php?udc=true"          >User Defined Counters Info Report</A><BR>
<A HREF="http://www.asemon.se/usage_report.php?usage=true"        >Counter Usage Info Report</A>        <BR>
<A HREF="http://www.asemon.se/usage_report.php?full=true"         >Full Report</A>                      <BR>

<?php
	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$rpt_summary_count      = $_GET['summary_count'];
	$rpt_summary_user       = $_GET['summary_user'];
	$rpt_conn               = $_GET['conn'];
	$rpt_udc                = $_GET['udc'];
	$rpt_usage              = $_GET['usage'];
	$rpt_full               = $_GET['full'];

	// sub command
	$rpt_onDomain           = $_GET['onDomain'];
	$rpt_onUser             = $_GET['onUser'];
	$rpt_onId               = $_GET['onId'];

	function htmlResultset($result, $headName)
	{
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
				else
					echo "<td nowrap>$cell</td>";
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
//	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
//	mysql_select_db("asemon_stat") or die(mysql_error());

//	$db=mysql_connect("localhost", "asemon_se", "qazZSE44") or die(mysql_error());
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die(mysql_error());
	mysql_select_db("asemon_se", $db) or die(mysql_error());

	//-------------------------------------------
	// SOME EXTRA STUFF
	//-------------------------------------------
//	$sql = "drop table asemon_counter_usage_info
//		";  mysql_query($sql) or die("ERROR: " . mysql_error());
//	$sql = "
//		";  mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "
	//		CREATE TABLE xxx
	//		(
	//				col1                 int,
	//		
	//				PRIMARY KEY (xxx, yyy)
	//		);
	//	";
	//	mysql_query($sql) or die("ERROR: " . mysql_error());

	//	$sql = "alter table asemon_usage add column sun_desktop   varchar(15)";        mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "alter table asemon_usage add column user_country  varchar(5)";         mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "alter table asemon_usage add column user_language varchar(5)";         mysql_query($sql) or die("ERROR: " . mysql_error());

	//	$sql = "alter table asemon_connect_info add column srvVersionStr varchar(100)";         mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "alter table asemon_connect_info MODIFY column srvVersionStr varchar(150)";	    mysql_query($sql) or die("ERROR: " . mysql_error());

	//	$sql = "delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_connect_info       where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_udc_info           where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_counter_usage_info where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());

//	mysql_query("delete from asemon_udc_info where udcKey like 'udc_monCIPCEndpoints%' or udcKey like 'udc_dummyDuplicate%'");
//	mysql_query("delete from asemon_udc_info");

//	mysql_query("DROP TABLE sumDomainStartPriv");
//	mysql_query("CREATE TABLE sumDomainStartPriv
//		(
//			domainName        varchar(50),
//			usageCount        int,
//			lastStarted       datetime,
//			pollTime          datetime,
//		
//			PRIMARY KEY (domainName)
//		)
//	") or die("ERROR: " . mysql_error());
//
//	mysql_query("DROP   TABLE sumDomainStartNow;");
//	mysql_query("CREATE TABLE sumDomainStartNow
//		(
//			domainName        varchar(50),
//			usageCount        int,
//			lastStarted       datetime,
//			pollTime          datetime,
//		
//			PRIMARY KEY (domainName)
//		)
//	") or die("ERROR: " . mysql_error());
//
//	mysql_query("DROP   TABLE sumSybaseUsersStartPriv;");
//	mysql_query("CREATE TABLE sumSybaseUsersStartPriv
//		(
//			userName          varchar(50),
//			usageCount        int,
//			lastStarted       datetime,
//			pollTime          datetime,
//		
//			PRIMARY KEY (userName)
//		)
//	") or die("ERROR: " . mysql_error());
//
//	mysql_query("DROP   TABLE sumSybaseUsersStartNow;");
//	mysql_query("CREATE TABLE sumSybaseUsersStartNow
//		(
//			userName          varchar(50),
//			usageCount        int,
//			lastStarted       datetime,
//			pollTime          datetime,
//		
//			PRIMARY KEY (userName)
//		)
//	") or die("ERROR: " . mysql_error());


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
		$result = mysql_query("SELECT *, (sumRowCount / refreshCount) AS avgSumRowCount FROM asemon_counter_usage_info WHERE checkId = " . $rpt_onId . " ORDER BY addSequence");
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}
		htmlResultset($result, "asemon_counter_usage_info on: $rpt_onId");
	}


	//-------------------------------------------
	// SUMMARY REPORT, COUNT
	//-------------------------------------------
	if ( $rpt_summary_count == "true" )
	{
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
		htmlResultset($result, "Start Count, per month");



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
		htmlResultset($result, "Start Count, per day");


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
		mysql_query("TRUNCATE TABLE sumDomainStartPriv") or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO    sumDomainStartPriv (SELECT * FROM sumDomainStartNow)") or die("ERROR: " . mysql_error());


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
			SELECT n.userName as sybUserName, 
				n.userName as userNameUsage,
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
			SELECT n.userName as sybUserName, 
				n.userName as userNameUsage,
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
		mysql_query("TRUNCATE TABLE sumSybaseUsersStartPriv") or die("ERROR: " . mysql_error());
		mysql_query("INSERT INTO    sumSybaseUsersStartPriv (SELECT * FROM sumSybaseUsersStartNow)") or die("ERROR: " . mysql_error());
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
	if ( $rpt_conn == "true" )
	{
		$sql = "
			SELECT * 
			FROM asemon_connect_info
			ORDER BY checkId desc
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, "Connection Info Report");
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
	if ( $rpt_usage == "true" )
	{
		$sql = "
			SELECT 
				checkId,
				serverAddTime,
				clientTime,
				userName,
				addSequence,
				cmName,
				refreshCount,
				sumRowCount,
				(sumRowCount / refreshCount) AS avgSumRowCount
			FROM asemon_counter_usage_info
			ORDER BY checkId desc, addSequence
		";

		// sending query
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			die("Query to show fields from table failed");
		}
		htmlResultset($result, "Counter Usage Info Report");
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

				clientHostName,
				clientHostAddress,
				clientCanonicalHostName,

				user_name,
				user_dir,
				propfile,
				gui,
				user_country,
				user_language,
				sun_desktop,

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
