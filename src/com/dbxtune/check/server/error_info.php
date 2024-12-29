<?php
	require("gorans_functions.php");

//	//----------------------------------------
//	// FUNCTION: get params from POST or GET
//	//----------------------------------------
//	function getUrlParam($param)
//	{
//		if(!empty($_POST))
//		{
//			return $_POST[$param];
//		}
//		else if(!empty($_GET))
//		{
//			return urldecode($_GET[$param]);
//		}
//
//	}

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');


	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId       = getUrlParam('checkId');
	$sendCounter   = getUrlParam('sendCounter');
	$clientTime    = getUrlParam('clientTime');
	$userName      = getUrlParam('userName');

	$srvVersion    = versionFix(getUrlParam('srvVersion'));
	$appVersion    = getUrlParam('appVersion');

	$logLevel      = getUrlParam('logLevel');
	$logThreadName = getUrlParam('logThreadName');
	$logClassName  = getUrlParam('logClassName');
	$logLocation   = getUrlParam('logLocation');
	$logMessage    = getUrlParam('logMessage');
	$logStacktrace = getUrlParam('logStacktrace');
	$clientAppName           = getUrlParam('clientAppName');


	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}


	//------------------------------------------
	// Now connect to the database and insert a usage record
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());
	
	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	$logLevel      = mysqli_real_escape_string($dbconn, $logLevel);
	$logThreadName = mysqli_real_escape_string($dbconn, $logThreadName);
	$logClassName  = mysqli_real_escape_string($dbconn, $logClassName);
	$logLocation   = mysqli_real_escape_string($dbconn, $logLocation);
	$logMessage    = mysqli_real_escape_string($dbconn, $logMessage);
	$logStacktrace = mysqli_real_escape_string($dbconn, $logStacktrace);

	//------------------------------------------
	// Normally all errors goes to 'asemon_error_info', but some errors goes to 'asemon_error_info2'
	// for example timeouts
	$toTableName = "asemon_error_info";

	if (    strpos($logStacktrace, "java.sql.SQLException: JZ006:", 0) === 0                                   // Caught IOException: java.net.SocketTimeoutException: Read timed out
	     || strpos($logStacktrace, "java.sql.SQLException: JZ0C0:", 0) === 0                                   // java.sql.SQLException: JZ0C0: Connection is already closed
	     || strpos($logStacktrace, "com.sybase.jdbc3.jdbc.SybSQLException: ERROR: Found NO database", 0) === 0 // com.sybase.jdbc3.jdbc.SybSQLException: ERROR: Found NO database that was marked for replication
	   )
	{
		$toTableName = "asemon_error_info2";
	}


	$sql = "insert into $toTableName
	(
		checkId,
		sendCounter,
		serverAddTime,
		clientTime,
		clientAppName,
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
		'$clientAppName',
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
	mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
