<?php
	require("gorans_functions.php");

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = getUrlParam('debug');


	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$srvVersion         = versionFix(getUrlParam('srvVersion'));

	echo "srvVersion = $srvVersion <br>";

	echo "DONE: \n";
?>
