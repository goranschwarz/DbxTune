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
	$checkId            = getUrlParam('checkId');
	$clientTime         = getUrlParam('clientTime');
	$userName           = getUrlParam('userName');

	$connectId          = getUrlParam('connectId');
	$srvVersion         = getUrlParam('srvVersion');
	$isClusterEnabled   = getUrlParam('isClusterEnabled');

	$srvName            = getUrlParam('srvName');
	$srvIpPort          = getUrlParam('srvIpPort');
	$srvUser            = getUrlParam('srvUser');
	$srvVersionStr      = getUrlParam('srvVersionStr');

	$usePcs             = getUrlParam('usePcs');
	$pcsConfig          = getUrlParam('pcsConfig');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;


	//------------------------------------------
	// Now connect to the database and insert a usage record
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$srvName       = mysql_real_escape_string($srvName);
	$srvIpPort     = mysql_real_escape_string($srvIpPort);
	$srvUser       = mysql_real_escape_string($srvUser);
	$srvVersionStr = mysql_real_escape_string($srvVersionStr);

	$pcsConfig     = mysql_real_escape_string($pcsConfig);

	$sql = "insert into asemon_connect_info
	(
		checkId,
		serverAddTime,
		clientTime,
		userName,

		connectId,
		srvVersion,
		isClusterEnabled,

		srvName,
		srvIpPort,
		srvUser,
		srvVersionStr,

		usePcs,
		pcsConfig
	)
	values
	(
		$checkId,
		NOW(),
		'$clientTime',
		'$userName',

		$connectId,
		$srvVersion,
		$isClusterEnabled,

		'$srvName',
		'$srvIpPort',
		'$srvUser',
		'$srvVersionStr',

		'$usePcs',
		'$pcsConfig'
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
