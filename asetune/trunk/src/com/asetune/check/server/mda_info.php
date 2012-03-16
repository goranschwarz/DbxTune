<?php
	//----------------------------------------
	// FUNCTION: get params from POST or GET
	//----------------------------------------
	function getUrlParam($param, $debug='false')
	{
		if(!empty($_POST))
		{
			$val = $_POST[$param];
		}
		else if(!empty($_GET))
		{
			$val = urldecode($_GET[$param]);
		}
		if ( $debug == "true" )
			echo "DEBUG: getUrlParam='$param', val='$val'.\n";

		return $val;
	}
	//----------------------------------------
	// FUNCTION: get POST or GET
	//----------------------------------------
	function getDataArray()
	{
		if(!empty($_POST))
		{
			return $_POST;
		}
		else if(!empty($_GET))
		{
			return $_GET;
		}
		return array();
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

	$expectedRows       = getUrlParam('expectedRows');
	$batchSize          = getUrlParam('batchSize');

	if ($batchSize == "")
	{
		die("ERROR: batchSize is unknown. batchSize='$batchSize'.");
	}
	if ( $debug == "true" )
		echo "DEBUG: batchSize='$batchSize'.\n";

	//------------------------------------------
	// Now connect to the database
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

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
		$Description = mysql_real_escape_string($Description);

		$sql = "insert into asemon_mda_info
		(
			checkId,
			serverAddTime,
			clientTime,
			userName,

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
			'$userName',

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
		mysql_query($sql);

		$errorNumber = mysql_errno();
		$errorString = mysql_error();
		if ($errorNumber != 0 && $errorNumber != 1062)
		{
			die("ERROR: Number=" . $errorNumber . ", Message=" . $errorString);
		}
	}

	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());

	echo "DONE: \n";
?>
