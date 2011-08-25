<?php
	//------------------------------------------
	// DEFINE latest version information
	$ASEMON_LATEST_VERSION_SRC = 149;
	$ASEMON_LATEST_VERSION_STR = "1.0.0";
	$DOWNLOAD_URL              = "gorans.no-ip.org/asemon/download.html";

	//------------------------------------------
	// DEFINE latest version information
	if ($_POST["clientSourceVersion"] < $ASEMON_LATEST_VERSION_SRC)
		echo "ACTION:UPGRADE:$ASEMON_LATEST_VERSION_STR:$DOWNLOAD_URL\n";
	else
		echo "ACTION:NO-UPGRADE\n";

	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	$debug = $_POST['debug'];


	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	$clientCheckTime         = $_POST['clientCheckTime'];

	$clientSourceDate        = $_POST['clientSourceDate'];
	$clientSourceVersion     = $_POST['clientSourceVersion'];
	$clientAsemonVersion     = $_POST['clientAsemonVersion'];

	$clientHostName          = $_POST['clientHostName'];
	$clientHostAddress       = $_POST['clientHostAddress'];
	$clientCanonicalHostName = $_POST['clientCanonicalHostName'];

	$user_name               = $_POST['user_name'];
	$user_dir                = $_POST['user_dir'];
	$propfile                = $_POST['propfile'];
	$gui                     = $_POST['gui'];
	
	$java_version            = $_POST['java_version'];
	$java_vm_version         = $_POST['java_vm_version'];
	$java_vm_vendor          = $_POST['java_vm_vendor'];
	$java_home               = $_POST['java_home'];
	$java_class_path         = $_POST['java_class_path'];
	$memory                  = $_POST['memory'];
	$os_name                 = $_POST['os_name'];
	$os_version              = $_POST['os_version'];
	$os_arch                 = $_POST['os_arch'];


	//------------------------------------------
	// Now connect to the database and insert a usage record
	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
	mysql_select_db("asemon_stat") or die(mysql_error());

	$sql = "insert into asemon_usage
	(
		serverAddTime,
		clientCheckTime,

		serverSourceVersion,

		clientSourceDate,
		clientSourceVersion,
		clientAsemonVersion,

		clientHostName,
		clientHostAddress,
		clientCanonicalHostName,

		user_name,
		user_dir,
		propfile,
		gui,

		java_version,
		java_vm_version,
		java_vm_vendor,
		java_home,
		java_class_path,
		memory,
		os_name,
		os_version,
		os_arch
	)
	values
	(
		NOW(),
		'$clientCheckTime',

		$ASEMON_LATEST_VERSION_SRC,

		'$clientSourceDate',
		$clientSourceVersion,
		'$clientAsemonVersion',

		'$clientHostName',
		'$clientHostAddress',
		'$clientCanonicalHostName',

		'$user_name',
		'$user_dir',
		'$propfile',
		'$gui',

		'$java_version',
		'$java_vm_version',
		'$java_vm_vendor',
		'$java_home',
		'$java_class_path',
		'$memory',
		'$os_name',
		'$os_version',
		'$os_arch'
	)";

	if ( $debug == "true" )
	{
		echo "DEBUG EXECUTING SQL: $sql\n";
	}

	//------------------------------------------
	// Do the INSERT
	mysql_query($sql) or die(mysql_error());

?>
