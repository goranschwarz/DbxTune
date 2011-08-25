<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="/favicon.ico"/>
<title>Asemon usage report</title>
</head>
<body>

<?php
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

	//	$sql = "alter table asemon_connect_info add column srvVersionStr varchar(100)";         mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "alter table asemon_connect_info MODIFY column srvVersionStr varchar(150)";	    mysql_query($sql) or die("ERROR: " . mysql_error());

	//	$sql = "delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_connect_info       where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_udc_info           where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());
	//	$sql = "delete from asemon_counter_usage_info where userName  = 'gorans' or userName  = 'sybase'";   mysql_query($sql) or die("ERROR: " . mysql_error());

	//-------------------------------------------
	// SUMMARY REPORT
	//-------------------------------------------
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
		die("Query to show fields from table failed");
	}

	$fields_num = mysql_num_fields($result);

	echo "<h1>Summary Report</h1>";
	echo "<table border='1'>";
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
		foreach($row as $cell)
			echo "<td nowrap>$cell</td>";

		echo "</tr>\n";
	}
	echo "</table>\n";
	mysql_free_result($result);


//			TIMEDIFF(clientCheckTime, serverAddTime) AS swedenTimeZoneDiff,
//			TIME_FORMAT(TIMEDIFF(clientCheckTime, serverAddTime), '%H:%i') AS swedenTimeZoneDiff,

	//-------------------------------------------
	// CONNECTION INFO
	//-------------------------------------------
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

	$fields_num = mysql_num_fields($result);

	echo "<h1>Connection Info Report</h1>";
	echo "<table border='1'>";
	echo "<tr>";
	// printing table headers
	for($i=0; $i<$fields_num; $i++)
	{
		$field = mysql_fetch_field($result);
		echo "<td>{$field->name}</td>";
	}
	echo "</tr>\n";
	// printing table rows
	while($row = mysql_fetch_row($result))
	{
		echo "<tr>";

		// $row is array... foreach( .. ) puts every element
		// of $row to $cell variable
		foreach($row as $cell)
			echo "<td nowrap>$cell</td>";

		echo "</tr>\n";
	}
	echo "</table>\n";
	mysql_free_result($result);



	//-------------------------------------------
	// UDC INFO
	//-------------------------------------------
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

	$fields_num = mysql_num_fields($result);

	echo "<h1>User Defined Counters Info Report</h1>";
	echo "<table border='1'>";
	echo "<tr>";
	// printing table headers
	for($i=0; $i<$fields_num; $i++)
	{
		$field = mysql_fetch_field($result);
		echo "<td>{$field->name}</td>";
	}
	echo "</tr>\n";
	// printing table rows
	while($row = mysql_fetch_row($result))
	{
		echo "<tr>";

		// $row is array... foreach( .. ) puts every element
		// of $row to $cell variable
		foreach($row as $cell)
			echo "<td nowrap>$cell</td>";

		echo "</tr>\n";
	}
	echo "</table>\n";
	mysql_free_result($result);



	//-------------------------------------------
	// COUNTER USAGE INFO
	//-------------------------------------------
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

	$fields_num = mysql_num_fields($result);

	echo "<h1>Counter Usage Info Report</h1>";
	echo "<table border='1'>";
	echo "<tr>";
	// printing table headers
	for($i=0; $i<$fields_num; $i++)
	{
		$field = mysql_fetch_field($result);
		echo "<td>{$field->name}</td>";
	}
	echo "</tr>\n";
	// printing table rows
	while($row = mysql_fetch_row($result))
	{
		echo "<tr>";

		// $row is array... foreach( .. ) puts every element
		// of $row to $cell variable
		foreach($row as $cell)
			echo "<td nowrap>$cell</td>";

		echo "</tr>\n";
	}
	echo "</table>\n";
	mysql_free_result($result);



	//-------------------------------------------
	// FULL REPORT
	//-------------------------------------------
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

	$fields_num = mysql_num_fields($result);

	echo "<h1>Full Report</h1>";
	echo "<table border='1'>";
	echo "<tr>";
	// printing table headers
	for($i=0; $i<$fields_num; $i++)
	{
		$field = mysql_fetch_field($result);
		echo "<td>{$field->name}</td>";
	}
	echo "</tr>\n";
	// printing table rows
	while($row = mysql_fetch_row($result))
	{
		echo "<tr>";

		// $row is array... foreach( .. ) puts every element
		// of $row to $cell variable
		foreach($row as $cell)
			echo "<td nowrap>$cell</td>";

		echo "</tr>\n";
	}
	echo "</table>\n";
	mysql_free_result($result);


	// Close connection to the database
	mysql_close() or die(mysql_error());
?>

</body>
</html>
