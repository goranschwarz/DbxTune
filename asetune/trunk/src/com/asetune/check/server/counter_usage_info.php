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
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

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
		mysql_query($sql);

		$errorNumber = mysql_errno();
		$errorString = mysql_error();
		if ($errorNumber != 0)
		{
			die("ERROR: Number=" . $errorNumber . ", Message=" . $errorString);
		}
	}

	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());

	echo "DONE: \n";
?>
