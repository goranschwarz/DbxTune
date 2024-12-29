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
	$checkId            = getUrlParam('checkId');
	$clientTime         = getUrlParam('clientTime');
	$userName           = getUrlParam('userName');

	$connectId          = getUrlParam('connectId');

	$connectType        = getUrlParam('connectType');
	$prodName           = getUrlParam('prodName');
	$srvVersionInt      = versionFix(getUrlParam('srvVersionInt'));

	$connectTime        = getUrlParam('connectTime');
	$disconnectTime     = getUrlParam('disconnectTime');

	$execMainCount      = getUrlParam('execMainCount');
	$execBatchCount     = getUrlParam('execBatchCount');
	$execTimeTotal      = getUrlParam('execTimeTotal');
	$execTimeSqlExec    = getUrlParam('execTimeSqlExec');
	$execTimeRsRead     = getUrlParam('execTimeRsRead');
	$rsCount            = getUrlParam('rsCount');
	$rsRowsCount        = getUrlParam('rsRowsCount');
	$iudRowsCount       = getUrlParam('iudRowsCount');
	$sqlWarningCount    = getUrlParam('sqlWarningCount');
	$sqlExceptionCount  = getUrlParam('sqlExceptionCount');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;


	//------------------------------------------
	// Now connect to the database and insert a usage record
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	$prodName          = mysqli_real_escape_string($dbconn, $prodName);

	$sql = "insert into sqlw_usage_info
	(
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
	)
	values
	(
		$checkId,
		NOW(),
		'$clientTime',
		'$userName',

		$connectId,

		'$connectType',
		'$prodName',
		$srvVersionInt,

		'$connectTime',
		'$disconnectTime',

		$execMainCount,
		$execBatchCount,
		$execTimeTotal,
		$execTimeSqlExec,
		$execTimeRsRead,
		$rsCount,
		$rsRowsCount,
		$iudRowsCount,
		$sqlWarningCount,
		$sqlExceptionCount
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
