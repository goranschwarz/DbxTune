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
