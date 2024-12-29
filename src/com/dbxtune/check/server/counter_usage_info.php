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


	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId            = getUrlParam('checkId');
	$connectId          = getUrlParam('connectId');
	$clientTime         = getUrlParam('clientTime');   // removed in versions after 2011-11-09
	$clientAppName      = getUrlParam('clientAppName');
	$sessionType        = getUrlParam('sessionType');
	$sessionStartTime   = getUrlParam('sessionStartTime');
	$sessionEndTime     = getUrlParam('sessionEndTime');
	$userName           = getUrlParam('userName');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.
	if (empty($connectId))
		$connectId = -1;

	// 'clientTime' was moved into 'sessionStartTime' in versions after 2011-11-09
	if (empty($sessionStartTime))
		$sessionStartTime= $clientTime;

	if (empty($sessionType))
		$sessionType= "online";

	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}


	// Copy all values AFTER userName to an array
	$cpArr = array();
	$doCopy = 0;
	$dataArray = getDataArray();
	foreach ($dataArray as $key => $value)
	{
		if ( $debug == "true" )
			printf("Key='%s', value='%s', doCopy=%d\n", $key, $value, $doCopy);

		if ( $doCopy == 1 )
			$cpArr[$key] = urldecode("$value");

		if ( $key == "userName")
			$doCopy = 1;
	}
	if ( $debug == "true" )
		print_r($cpArr);

	//------------------------------------------
	// Now connect to the database
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	$addSequence = 0;
	// Insert one row for every UDC key/value
	foreach ($cpArr as $key => $value)
	{
		$addSequence = $addSequence + 1;

		if ( $debug == "true" )
			printf("Key='%s', Value='%s', addSequence=%d\n", $key, $value, $addSequence);

		$sql = "insert into asemon_counter_usage_info
		(
			checkId,
			serverAddTime,
			clientAppName,
			sessionType,
			sessionStartTime,
			sessionEndTime,
			userName,
			connectId,

			cmName,
			addSequence,
			refreshCount,
			sumRowCount
		)
		values
		(
			$checkId,
			NOW(),
			'$clientAppName',
			'$sessionType',
			'$sessionStartTime',
			'$sessionEndTime',
			'$userName',
			$connectId,

			'$key',
			$addSequence,
			$value
		)";
		// NOTE: value will consist of two values, which are already separated by a comma(,)
		//       so value will be refreshCount,sumRowCount

		if ( $debug == "true" )
		{
			echo "DEBUG EXECUTING SQL: $sql\n";
		}

		//------------------------------------------
		// Do the INSERT, if errors exit
		mysqli_query($dbconn, $sql);

		$errorNumber = mysqli_errno($dbconn);
		$errorString = mysqli_error($dbconn);
		if ($errorNumber != 0)
		{
			die("ERROR: Number=" . $errorNumber . ", Message=" . $errorString);
		}
	}

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
