<?php
	require("gorans_functions.php");

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId           = getUrlParam('checkId');
	$userName          = getUrlParam('userName');

	$srvName           = getUrlParam('srvName');
	$dbxProduct        = getUrlParam('dbxProduct');

	$firstSamleTime    = getUrlParam('firstSamleTime');
	$lastSamleTime     = getUrlParam('lastSamleTime');

	$alarmCount        = getUrlParam('alarmCount');
	$receiveCount      = getUrlParam('receiveCount');
	$receiveGraphCount = getUrlParam('receiveGraphCount');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.


	//------------------------------------------
	// Now connect to the database
	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	$sql = "insert into dbxc_store_srv_info
	(
		checkId,
		serverAddTime,
		userName,

		srvName,
		dbxProduct,

		firstSamleTime,
		lastSamleTime,

		alarmCount,
		receiveCount,
		receiveGraphCount
	)
	values
	(
		$checkId,
		NOW(),
		'$userName',

		'$srvName',
		'$dbxProduct',

		'$firstSamleTime',
		'$lastSamleTime',

		$alarmCount,
		$receiveCount,
		$receiveGraphCount
	)";

	if ( $debug == "true" )
	{
		echo "DEBUG EXECUTING SQL: $sql\n";
	}

	//------------------------------------------
	// Do the INSERT, if errors exit
	mysqli_query($dbconn, $sql);

	$errorNumber = mysqli_errno($dbconn);
	$errorString = mysqli_error($dbconn);
	if ($errorNumber != 0)
	{
		die("ERROR: Number=" . $errorNumber . ", Message=" . $errorString);
	}

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
