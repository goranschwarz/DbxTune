<?php
	//----------------------------------------
	// FUNCTION: get params from POST or GET
	//----------------------------------------
	function getUrlParam($param)
	{
		if(!empty($_POST))
		{
			return $_POST[$param];
		}
		else if(!empty($_GET))
		{
			return urldecode($_GET[$param]);
		}

	}

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');


	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId       = getUrlParam('checkId');
	$sendCounter   = getUrlParam('sendCounter');
	$clientTime    = getUrlParam('clientTime');
	$userName      = getUrlParam('userName');

	$srvVersion    = getUrlParam('srvVersion');
	$appVersion    = getUrlParam('appVersion');

	$logLevel      = getUrlParam('logLevel');
	$logThreadName = getUrlParam('logThreadName');
	$logClassName  = getUrlParam('logClassName');
	$logLocation   = getUrlParam('logLocation');
	$logMessage    = getUrlParam('logMessage');
	$logStacktrace = getUrlParam('logStacktrace');


	//------------------------------------------
	// Now connect to the database and insert a usage record
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$logLevel      = mysql_real_escape_string($logLevel);
	$logThreadName = mysql_real_escape_string($logThreadName);
	$logClassName  = mysql_real_escape_string($logClassName);
	$logLocation   = mysql_real_escape_string($logLocation);
	$logMessage    = mysql_real_escape_string($logMessage);
	$logStacktrace = mysql_real_escape_string($logStacktrace);

	$sql = "insert into asemon_error_info
	(
		checkId,
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
		logStacktrace
	)
	values
	(
		$checkId,
		$sendCounter,
		NOW(),
		'$clientTime',
		'$userName',

		'$srvVersion',
		'$appVersion',

		'$logLevel',
		'$logThreadName',
		'$logClassName',
		'$logLocation',
		'$logMessage',
		'$logStacktrace'
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
