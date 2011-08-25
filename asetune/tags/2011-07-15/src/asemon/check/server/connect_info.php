<?php

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = $_POST['debug'];


	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId            = $_POST['checkId'];
	$clientTime         = $_POST['clientTime'];
	$userName           = $_POST['userName'];

	$srvVersion         = $_POST['srvVersion'];
	$isClusterEnabled   = $_POST['isClusterEnabled'];

	$srvName            = $_POST['srvName'];
	$srvIpPort          = $_POST['srvIpPort'];
	$srvUser            = $_POST['srvUser'];
	$srvVersionStr      = $_POST['srvVersionStr'];



	//------------------------------------------
	// Now connect to the database and insert a usage record
//	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
//	mysql_select_db("asemon_stat") or die(mysql_error());

//	$db=mysql_connect("localhost", "asemon_se", "qazZSE44") or die(mysql_error());
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$sql = "insert into asemon_connect_info
	(
		checkId,
		serverAddTime,
		clientTime,
		userName,

		srvVersion,
		isClusterEnabled,

		srvName,
		srvIpPort,
		srvUser,
		srvVersionStr
	)
	values
	(
		$checkId,
		NOW(),
		'$clientTime',
		'$userName',

		$srvVersion,
		$isClusterEnabled,

		'$srvName',
		'$srvIpPort',
		'$srvUser',
		'$srvVersionStr'
	)";

	if ( $debug == "true" )
	{
		echo "DEBUG EXECUTING SQL: $sql\n";
	}

	//------------------------------------------
	// Do the INSERT
	mysql_query($sql) or die("ERROR: " . mysql_error());

	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());

	echo "DONE: \n";
?>
