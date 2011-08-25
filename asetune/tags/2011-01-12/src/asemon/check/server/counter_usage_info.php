<?php
	//print_r ($_POST);

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = $_POST['debug'];

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId            = $_POST['checkId'];
	$clientTime         = $_POST['clientTime'];
	$userName           = $_POST['userName'];

	// Copy all values AFTER userName to an array
	$cpArr = array();
	$doCopy = 0;
	foreach ($_POST as $key => $value)
	{
		if ( $debug == "true" )
			printf("Key='%s', value='%s', doCopy=%d\n", $key, $value, $doCopy);

		if ( $doCopy == 1 )
			$cpArr[$key] = "$value";

		if ( $key == "userName")
			$doCopy = 1;
	}

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
			clientTime,
			userName,
	
			cmName,
			addSequence,
			refreshCount,
			sumRowCount
		)
		values
		(
			$checkId,
			NOW(),
			'$clientTime',	
			'$userName',
	
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
