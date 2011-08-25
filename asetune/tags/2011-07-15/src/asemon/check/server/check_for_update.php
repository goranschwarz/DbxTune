<?php
	//------------------------------------------
	// DEFINE latest version information
	$ASEMON_LATEST_VERSION_SRC = 224;
	$ASEMON_LATEST_VERSION_STR = "2.0.0";
	$ASEMON_LATEST_VERSION_STR = "2011-04-21";
	$DOWNLOAD_URL              = "sourceforge.net/projects/asemon-gui/files/";
	$WHATSNEW_URL              = "www.asemon.se/history.html";
	$SEND_OPTIONS              = "sendConnectInfo=true, sendUdcInfo=true, sendCounterUsageInfo=true";
	$FEEDBACK_URL              = "www.asemon.se/feedback.html";
	$FEEDBACK_URL              = "";

	//------------------------------------------
	// DEFINE latest version information
	if ($_POST["clientSourceVersion"] < $ASEMON_LATEST_VERSION_SRC)
	{
		echo "ACTION:UPGRADE:$ASEMON_LATEST_VERSION_STR:$DOWNLOAD_URL:$WHATSNEW_URL\n";
		echo "OPTIONS: $SEND_OPTIONS\n";
		echo "FEEDBACK: $FEEDBACK_URL\n";
	}
	else
	{
		echo "ACTION:NO-UPGRADE\n";
		echo "OPTIONS: $SEND_OPTIONS\n";
		echo "FEEDBACK: $FEEDBACK_URL\n";
	}

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
	$sun_desktop             = $_POST['sun_desktop'];
	$user_country            = $_POST['user_country'];
	$user_language           = $_POST['user_language'];
	
	$java_version            = $_POST['java_version'];
	$java_vm_version         = $_POST['java_vm_version'];
	$java_vm_vendor          = $_POST['java_vm_vendor'];
	$java_home               = $_POST['java_home'];
	$java_class_path         = $_POST['java_class_path'];
	$memory                  = $_POST['memory'];
	$os_name                 = $_POST['os_name'];
	$os_version              = $_POST['os_version'];
	$os_arch                 = $_POST['os_arch'];

	// If user is 'gorans', get out of here otherwise I will flod the log with personal entries
	if ( $user_name == "gorans" )
	{
		if ( $debug == "true" )
			echo "DEBUG STOP PROCESSING: CHECK FOR UPDATE, user is '$user_name'.\n";
		exit;
	}

	//------------------------------------------
	// Now connect to the database and insert a usage record
//	mysql_connect("localhost", "asemon_stat", "asemon") or die(mysql_error());
//	mysql_select_db("asemon_stat") or die(mysql_error());

//	$db=mysql_connect("localhost", "asemon_se", "qazZSE44") or die(mysql_error());
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

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
		sun_desktop,
		user_country,
		user_language,

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
		'$sun_desktop',
		'$user_country',
		'$user_language',

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
	mysql_query($sql) or die("ERROR: " . mysql_error());

	//------------------------------------------
	// Send a responce id
	printf("CHECK_ID: %d\n", mysql_insert_id());

	//------------------------------------------
	// Close connection to the database
	mysql_close() or die("ERROR: " . mysql_error());
?>
