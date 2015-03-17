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
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$prodName          = mysql_real_escape_string($prodName);

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
	mysql_query($sql) or die("ERROR: " . mysql_error());


	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());

	echo "DONE: \n";
?>
