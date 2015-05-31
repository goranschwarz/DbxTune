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
	$clientAppName      = getUrlParam('clientAppName');
	$userName           = getUrlParam('userName');

	$connectId          = getUrlParam('connectId');
	$srvVersion         = versionFix(getUrlParam('srvVersion'));
//	$srvVersion         = getUrlParam('srvVersion');
	$isClusterEnabled   = getUrlParam('isClusterEnabled');

	$srvName            = getUrlParam('srvName');
	$srvIpPort          = getUrlParam('srvIpPort');
	$sshTunnelInfo      = getUrlParam('sshTunnelInfo');
	$srvUser            = getUrlParam('srvUser');
	$srvUserRoles       = getUrlParam('srvUserRoles');
	$srvVersionStr      = getUrlParam('srvVersionStr');
	$srvSortOrderId     = getUrlParam('srvSortOrderId');
	$srvSortOrderName   = getUrlParam('srvSortOrderName');
	$srvCharsetId       = getUrlParam('srvCharsetId');
	$srvCharsetName     = getUrlParam('srvCharsetName');
	$srvSapSystemInfo   = getUrlParam('srvSapSystemInfo');

	$usePcs             = getUrlParam('usePcs');
	$pcsConfig          = getUrlParam('pcsConfig');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;

	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}

	//------------------------------------------
	// Now connect to the database and insert a usage record
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$srvName       = mysql_real_escape_string($srvName);
	$srvIpPort     = mysql_real_escape_string($srvIpPort);
	$sshTunnelInfo = mysql_real_escape_string($sshTunnelInfo);
	$srvUser       = mysql_real_escape_string($srvUser);
	$srvVersionStr = mysql_real_escape_string($srvVersionStr);

	$pcsConfig     = mysql_real_escape_string($pcsConfig);

	$sql = "insert into asemon_connect_info
	(
		checkId,
		serverAddTime,
		clientTime,
		clientAppName,
		userName,

		connectId,
		srvVersion,
		isClusterEnabled,

		srvName,
		srvIpPort,
		sshTunnelInfo,
		srvUser,
		srvUserRoles,
		srvVersionStr,
		srvSortOrderId,
		srvSortOrderName,
		srvCharsetId,
		srvCharsetName,
		srvSapSystemInfo,

		usePcs,
		pcsConfig
	)
	values
	(
		$checkId,
		NOW(),
		'$clientTime',
		'$clientAppName',
		'$userName',

		$connectId,
		$srvVersion,
		$isClusterEnabled,

		'$srvName',
		'$srvIpPort',
		'$sshTunnelInfo',
		'$srvUser',
		'$srvUserRoles',
		'$srvVersionStr',
		'$srvSortOrderId',
		'$srvSortOrderName',
		'$srvCharsetId',
		'$srvCharsetName',
		'$srvSapSystemInfo',

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
	// CHECK if client should send MDA information
	// This is if ASE Version has NOT been saved previously
	// SELECT count(*) from asemon_mda_info where srvVersion = $srvVersion and isClusterEnabled = $isClusterEnabled
	$sql = "
		IF EXISTS (SELECT 1 FROM asemon_mda_info WHERE srvVersion = $srvVersion AND isClusterEnabled = $isClusterEnabled)
		THEN
			SELECT 1 as 'has_ase_version';
		ELSE
			SELECT 0 as 'has_ase_version';
		END IF;
		";
	$sql = "SELECT count(*) FROM asemon_mda_info WHERE srvVersion = $srvVersion AND isClusterEnabled = $isClusterEnabled ";

	if ( $debug == "true" )
		echo "DEBUG EXECUTING SQL: $sql\n";

	$hasMdaInfo = 1;
	$result = mysql_query($sql);
	if (!$result)
	{
		if ( $debug == "true" )
			echo "DEBUG NO resultset for query\n";
	}
	else
	{
		while($row = mysql_fetch_row($result))
		{
			$hasMdaInfo = $row[0];
			if ( $debug == "true" )
				echo "DEBUG read row: $hasMdaInfo \n";
		}
		mysql_free_result($result);
	}
	if ( $debug == "true" )
		echo "DEBUG hasMdaInfo: $hasMdaInfo \n";
	if ($hasMdaInfo == 0)
		echo "SEND_MDA_INFO: true\n";


	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());

	echo "DONE: \n";
?>
