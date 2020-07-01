<?php
	require("gorans_functions.php");

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');

	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$checkId              = getUrlParam('checkId');
	$userName             = getUrlParam('userName');

	$shutdownReason       = getUrlParam('shutdownReason');
	$wasRestartSpecified  = getUrlParam('wasRestartSpecified');

	$writerJdbcUrl        = getUrlParam('writerJdbcUrl');
	$H2DbFileSize1InMb    = getUrlParam('H2DbFileSize1InMb');
	$H2DbFileSize2InMb    = getUrlParam('H2DbFileSize2InMb');
	$H2DbFileSizeDiffInMb = getUrlParam('H2DbFileSizeDiffInMb');

	//------------------------------------------
	// Check for values that are NOT sent, some version on AseTune is NOT sending new information.


	//------------------------------------------
	// Now connect to the database
	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	$sql = "insert into dbxc_store_info
	(
		checkId,
		serverAddTime,
		userName,

		shutdownReason,
		wasRestartSpecified,

		writerJdbcUrl,
		H2DbFileSize1InMb,
		H2DbFileSize2InMb,
		H2DbFileSizeDiffInMb
	)
	values
	(
		$checkId,
		NOW(),
		'$userName',

		'$shutdownReason',
		$wasRestartSpecified,

		'$writerJdbcUrl',
		$H2DbFileSize1InMb,
		$H2DbFileSize2InMb,
		$H2DbFileSizeDiffInMb
	)";

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

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));

	echo "DONE: \n";
?>
