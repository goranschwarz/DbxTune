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
	$srvPageSizeInKb    = getUrlParam('srvPageSizeInKb');
	$srvSortOrderId     = getUrlParam('srvSortOrderId');
	$srvSortOrderName   = getUrlParam('srvSortOrderName');
	$srvCharsetId       = getUrlParam('srvCharsetId');
	$srvCharsetName     = getUrlParam('srvCharsetName');
	$srvSapSystemInfo   = getUrlParam('srvSapSystemInfo');

	$usePcs             = getUrlParam('usePcs');
	$pcsConfig          = getUrlParam('pcsConfig');

	$connTypeStr        = getUrlParam('connTypeStr');
	$prodName           = getUrlParam('prodName');
	$prodVersionStr     = getUrlParam('prodVersionStr');
	$jdbcUrl            = getUrlParam('jdbcUrl');
	$jdbcDriverClass    = getUrlParam('jdbcDriverClass');
	$jdbcDriverName     = getUrlParam('jdbcDriverName');
	$jdbcDriverVersion  = getUrlParam('jdbcDriverVersion');


	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;

	// remove newlines from some fields
	$srvVersionStr = trim(str_replace(array("\n", "\r"), " ", $srvVersionStr));

	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}

	// Fix SQL-Server version 
	if ( $clientAppName == "SqlServerTune" )
	{
		if (strlen($srvVersion) == 14) // 12 05 00 0030 0000
		{
			$srvVersion = parseSqlServerVersionStr($srvVersionStr, $srvVersion);
		}
	}
	
	//------------------------------------------
	// Now connect to the database and insert a usage record
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	$srvName           = mysqli_real_escape_string($dbconn, $srvName);
	$srvIpPort         = mysqli_real_escape_string($dbconn, $srvIpPort);
	$sshTunnelInfo     = mysqli_real_escape_string($dbconn, $sshTunnelInfo);
	$srvUser           = mysqli_real_escape_string($dbconn, $srvUser);
	$srvVersionStr     = mysqli_real_escape_string($dbconn, $srvVersionStr);

	$pcsConfig         = mysqli_real_escape_string($dbconn, $pcsConfig);

	$prodName          = mysqli_real_escape_string($dbconn, $prodName);
	$prodVersionStr    = mysqli_real_escape_string($dbconn, $prodVersionStr);
	$jdbcUrl           = mysqli_real_escape_string($dbconn, $jdbcUrl);
	$jdbcDriverName    = mysqli_real_escape_string($dbconn, $jdbcDriverName);
	$jdbcDriverVersion = mysqli_real_escape_string($dbconn, $jdbcDriverVersion);


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
		srvPageSizeInKb,
		srvSortOrderId,
		srvSortOrderName,
		srvCharsetId,
		srvCharsetName,
		srvSapSystemInfo,

		usePcs,
		pcsConfig,

		connTypeStr,
		prodName,
		prodVersionStr,
		jdbcUrl,
		jdbcDriverClass,
		jdbcDriverName,
		jdbcDriverVersion
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
		'$srvPageSizeInKb',
		'$srvSortOrderId',
		'$srvSortOrderName',
		'$srvCharsetId',
		'$srvCharsetName',
		'$srvSapSystemInfo',

		'$usePcs',
		'$pcsConfig',

		'$connTypeStr',
		'$prodName',
		'$prodVersionStr',
		'$jdbcUrl',
		'$jdbcDriverClass',
		'$jdbcDriverName',
		'$jdbcDriverVersion'
	)";

	if ( $debug == "true" )
	{
		echo "DEBUG EXECUTING SQL: $sql\n";
	}

	//------------------------------------------
	// Do the INSERT
	mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));

	//------------------------------------------
	// CHECK if client should send MDA information
	// This is if ASE Version has NOT been saved previously
	// SELECT count(*) from asemon_mda_info where srvVersion = $srvVersion and isClusterEnabled = $isClusterEnabled
	if ( $clientAppName == "AseTune" )
	{
		$sql = "SELECT count(*) FROM asemon_mda_info WHERE srvVersion = $srvVersion AND isClusterEnabled = $isClusterEnabled and clientAppName = 'AseTune' ";

		if ( $debug == "true" )
			echo "DEBUG EXECUTING SQL: $sql\n";

		$hasMdaInfo = 1;
		$result = mysqli_query($dbconn, $sql);
		if (!$result)
		{
			if ( $debug == "true" )
				echo "DEBUG NO resultset for query\n";
		}
		else
		{
			while($row = mysqli_fetch_row($result))
			{
				$hasMdaInfo = $row[0];
				if ( $debug == "true" )
					echo "DEBUG read row: $hasMdaInfo \n";
			}
			mysqli_free_result($result);
		}
		if ( $debug == "true" )
			echo "DEBUG hasMdaInfo: $hasMdaInfo \n";
		if ($hasMdaInfo == 0)
			echo "SEND_MDA_INFO: true\n";
	}
	else if ( $clientAppName == "RsTune" )
	{
	}
	else if ( $clientAppName == "IqTune" )
	{
	}
	else if ( $clientAppName == "SqlServerTune" )
	{
		$sql = "SELECT count(*) FROM asemon_mda_info WHERE srvVersion = $srvVersion AND isClusterEnabled = $isClusterEnabled and clientAppName = 'SqlServerTune' ";

		if ( $debug == "true" )
			echo "DEBUG EXECUTING SQL: $sql\n";

		$hasMdaInfo = 1;
		$result = mysqli_query($dbconn, $sql);
		if (!$result)
		{
			if ( $debug == "true" )
				echo "DEBUG NO resultset for query\n";
		}
		else
		{
			while($row = mysqli_fetch_row($result))
			{
				$hasMdaInfo = $row[0];
				if ( $debug == "true" )
					echo "DEBUG read row: $hasMdaInfo \n";
			}
			mysqli_free_result($result);
		}
		if ( $debug == "true" )
			echo "DEBUG hasMdaInfo: $hasMdaInfo \n";
		if ($hasMdaInfo == 0)
			echo "SEND_MDA_INFO: true\n";
	}


	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
