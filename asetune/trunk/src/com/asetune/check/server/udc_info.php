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
	$clientTime         = getUrlParam('clientTime');
	$userName           = getUrlParam('userName');

	// Copy all UDC rows into its own array
	$udcArr = array();
	$dataArray = getDataArray();
	foreach ($dataArray as $key => $value)
	{
		if ( $debug == "true" )
			printf("Key='%s', value='%s'\n", $key, $value);

		if (strpos($key, 'udc') === 0) // key starts with 'udc.'
			$udcArr[$key] = urldecode($value);
	}
	if ( $debug == "true" )
		print_r($udcArr);

	//------------------------------------------
	// Now connect to the database
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

// Delete the records for this user, so we can refresh the UDC=User Defined Counter
//$sql = "delete from asemon_udc_info where userName = '$userName' and serverAddTime <where older than 7 days>";
//$sql = "delete from asemon_udc_info where userName = '$userName'";
//mysql_query($sql) or die("ERROR: " . mysql_error());

	// Insert one row for every UDC key/value
	foreach ($udcArr as $udcKey => $udcValue)
	{
		//printf("udcKey='%s', udcValue='%s'\n", $udcKey, $udcValue);
		// hmmm it looks like all '.' in the key to '_', I'm not shure where this is done, in the java client side (URLConnection) or somewhere else

		$udcValueEscaped = mysql_real_escape_string($udcValue);

		$sql = "insert into asemon_udc_info
		(
			checkId,
			serverAddTime,
			clientTime,

			userName,

			udcKey,
			udcValue
		)
		values
		(
			$checkId,
			NOW(),
			'$clientTime',

			'$userName',

			'$udcKey',
			'$udcValueEscaped'
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
