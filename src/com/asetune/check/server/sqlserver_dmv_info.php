<?php
	require("gorans_functions.php");

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
	$isAzure            = getUrlParam('isAzure');

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
		$clientAppName = "SqlServerTune";
	}

	//------------------------------------------
	// Now connect to the database
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

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
		$IsNullable         = getUrlParam('IsNullable'  . "-$bc");
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
			$isAzure,

			$rowId,
			$expectedRows,

			'$type',
			'$TableName',
			$TableID,
			'$ColumnName',
			$ColumnID,
			'$TypeName',
			$Length,
			$IsNullable,
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
