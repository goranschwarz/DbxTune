<?php
	require("gorans_functions.php");

//	//----------------------------------------
//	// FUNCTION: get params from POST or GET
//	//----------------------------------------
//	function getUrlParam($param, $debug='false')
//	{
//		if(!empty($_POST))
//		{
//			$val = $_POST[$param];
//		}
//		else if(!empty($_GET))
//		{
//			$val = urldecode($_GET[$param]);
//		}
//		if ( $debug == "true" )
//			echo "DEBUG: getUrlParam='$param', val='$val'.\n";
//
//		return $val;
//	}
//	//----------------------------------------
//	// FUNCTION: get POST or GET
//	//----------------------------------------
//	function getDataArray()
//	{
//		if(!empty($_POST))
//		{
//			return $_POST;
//		}
//		else if(!empty($_GET))
//		{
//			return $_GET;
//		}
//		return array();
//	}
//	//----------------------------------------
//	// FUNCTION: toSqlNumber
//	//----------------------------------------
//	function toSqlNumber($input)
//	{
//		if ($input == "")
//			return "NULL";
//		return $input;
//	}
//	// FUNCTION: toSqlStr
//	function toSqlStr($input)
//	{
//		if ($input == "")
//			return "NULL";
//		return "'" . mysqli_real_escape_string($dbconn, $input) . "'";
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

	$srvVersion         = versionFix(getUrlParam('srvVersion'));
//	$srvVersion         = getUrlParam('srvVersion');
	$isClusterEnabled   = getUrlParam('isClusterEnabled');

	$expectedRows       = getUrlParam('expectedRows');
	$batchSize          = getUrlParam('batchSize');

	if ($batchSize == "")
	{
		die("ERROR: batchSize is unknown. batchSize='$batchSize'.");
	}
	if ( $debug == "true" )
		echo "DEBUG: batchSize='$batchSize'.\n";

	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}

	//------------------------------------------
	// Now connect to the database
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	for ( $bc = 0; $bc < $batchSize; $bc++ )
	{
		$rowId              = getUrlParam('rowId'       . "-$bc");

		$type               = getUrlParam('type'        . "-$bc");
		$TableName          = getUrlParam('TableName'   . "-$bc");
		$TableID            = getUrlParam('TableID'     . "-$bc");
		$ColumnName         = getUrlParam('ColumnName'  . "-$bc");
		$ColumnID           = getUrlParam('ColumnID'    . "-$bc");
		$TypeName           = getUrlParam('TypeName'    . "-$bc");
		$Length             = getUrlParam('Length'      . "-$bc");
		$Indicators         = getUrlParam('Indicators'  . "-$bc");
		$Description        = getUrlParam('Description' . "-$bc");

		// make some strings "safe"
		$Description = mysqli_real_escape_string($dbconn, $Description);

		$sql = "insert into asemon_mda_info
		(
			checkId,
			serverAddTime,
			clientTime,
			clientAppName,
			userName,
			verified,

			srvVersion,
			isClusterEnabled,

			rowId,
			expectedRows,

			type,
			TableName,
			TableID,
			ColumnName,
			ColumnID,
			TypeName,
			Length,
			Indicators,
			Description
		)
		values
		(
			$checkId,
			NOW(),
			'$clientTime',
			'$clientAppName',
			'$userName',
			NULL,

			$srvVersion,
			$isClusterEnabled,

			$rowId,
			$expectedRows,

			'$type',
			'$TableName',
			$TableID,
			'$ColumnName',
			$ColumnID,
			'$TypeName',
			$Length,
			$Indicators,
			'$Description'
		)";

		if ( $debug == "true" )
		{
			echo "DEBUG EXECUTING SQL: $sql\n";
		}

		//------------------------------------------
		// Do the INSERT, if errors exit (1062==Duplicate Key, which we dont kare about here...)
		mysqli_query($dbconn, $sql);

		$errorNumber = mysqli_errno($dbconn);
		$errorString = mysqli_error($dbconn);
		if ($errorNumber != 0 && $errorNumber != 1062)
		{
			die("ERROR: Number=" . $errorNumber . ", Message=" . $errorString);
		}
	}

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
