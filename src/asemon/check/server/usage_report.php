<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="/asemon/favicon.ico"/>
<title>Asemon usage report</title>
</head>
<body>

<?php
	//-------------------------------------------
	// CONNECT to database
	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
	mysql_select_db("asemon_stat") or die(mysql_error());

	//-------------------------------------------
	// SUMMARY REPORT
	//-------------------------------------------
	$sql = "
		SELECT
			user_name,
			count(*)             as usageCount,
			max(clientCheckTime) as lastUsedDate,
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
			user_name,
			clientCanonicalHostName
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


	//-------------------------------------------
	// FULL REPORT
	//-------------------------------------------
	$sql = "
		SELECT
			rowid,

			serverAddTime,
			clientCheckTime,

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
	";

	// sending query
	$result = mysql_query($sql);
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
?>

</body>
</html>
