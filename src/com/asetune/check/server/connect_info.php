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

	$srvVersion         = getUrlParam('srvVersion');
	$isClusterEnabled   = getUrlParam('isClusterEnabled');

	$srvName            = getUrlParam('srvName');
	$srvIpPort          = getUrlParam('srvIpPort');
	$srvUser            = getUrlParam('srvUser');
	$srvVersionStr      = getUrlParam('srvVersionStr');

	$usePcs             = getUrlParam('usePcs');
	$pcsConfig          = getUrlParam('pcsConfig');

	//------------------------------------------
	// Now connect to the database and insert a usage record
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$pcsConfig = mysql_real_escape_string($pcsConfig);

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
