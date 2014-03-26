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
	$prodVersionStr     = getUrlParam('prodVersionStr');

	$jdbcDriverName     = getUrlParam('jdbcDriverName');
	$jdbcDriverVersion  = getUrlParam('jdbcDriverVersion');
	$jdbcDriver         = getUrlParam('jdbcDriver');
	$jdbcUrl            = getUrlParam('jdbcUrl');

	$srvVersionInt      = versionFix(getUrlParam('srvVersionInt'));
	$srvName            = getUrlParam('srvName');
	$srvUser            = getUrlParam('srvUser');
	$srvCharsetName     = getUrlParam('srvCharsetName');
	$srvSortOrderName   = getUrlParam('srvSortOrderName');

	$sshTunnelInfo      = getUrlParam('sshTunnelInfo');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;


	//------------------------------------------
	// Now connect to the database and insert a usage record
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	$prodName          = mysql_real_escape_string($prodName);
	$prodVersionStr    = mysql_real_escape_string($prodVersionStr);

	$jdbcDriverName    = mysql_real_escape_string($jdbcDriverName);
	$jdbcDriverVersion = mysql_real_escape_string($jdbcDriverVersion);
	$jdbcDriver        = mysql_real_escape_string($jdbcDriver);
	$jdbcUrl           = mysql_real_escape_string($jdbcUrl);

	$srvName           = mysql_real_escape_string($srvName);
	$srvUser           = mysql_real_escape_string($srvUser);
	$srvCharsetName    = mysql_real_escape_string($srvCharsetName);
	$srvSortOrderName  = mysql_real_escape_string($srvSortOrderName);

	$sshTunnelInfo     = mysql_real_escape_string($sshTunnelInfo);

	$sql = "insert into sqlw_connect_info
	(
		sqlwCheckId,
		serverAddTime,
		clientTime,
		userName,

		connectId,
		connTypeStr,

		prodName,
		prodVersionStr,

		jdbcDriverName,
		jdbcDriverVersion,
		jdbcDriver,
		jdbcUrl,

		srvVersionInt,
		srvName,
		srvUser,
		srvCharsetName,
		srvSortOrderName,

		sshTunnelInfo
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
		'$prodVersionStr',

		'$jdbcDriverName',
		'$jdbcDriverVersion',
		'$jdbcDriver',
		'$jdbcUrl',

		$srvVersionInt,
		'$srvName',
		'$srvUser',
		'$srvCharsetName',
		'$srvSortOrderName',

		'$sshTunnelInfo'
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
