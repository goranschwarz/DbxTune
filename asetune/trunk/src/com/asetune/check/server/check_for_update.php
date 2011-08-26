<?php
	//----------------------------------------
	// FUNCTION: get params from POST or GET
	//----------------------------------------
	function getUrlParam($param)
	{
		if(!empty($_POST))
		{
			return $_POST[$param];
		}
		else if(!empty($_GET))
		{
			return urldecode($_GET[$param]);
		}

	}
	//----------------------------------------
	// FUNCTION: get params from POST or GET
	//           if first param is not set, try secondary param
	//           if none was found return the default value.
	//----------------------------------------
	function getUrlParam2($param, $altParam)
	{
		$val = getUrlParam($param);
		if (!empty($val))
			return $val;

		$val = getUrlParam($altParam);
		if (!empty($val))
			return $val;

		return $val;
	}


	//------------------------------------------
	// DEFINE latest version information
	//-------
	$ASEMON_LATEST_VERSION_SRC = 229;
	$ASEMON_LATEST_VERSION_STR = "2.1.0";
	$ASEMON_LATEST_VERSION_STR = "2011-07-15";
	$DOWNLOAD_URL              = "sourceforge.net/projects/asemon-gui/files/";
	$WHATSNEW_URL              = "www.asemon.se/history.html";
	$SEND_OPTIONS              = "sendConnectInfo=true, sendUdcInfo=true, sendCounterUsageInfo=true";
	$FEEDBACK_URL              = "2011-06-09:www.asemon.se/feedback.html";
	$FEEDBACK_URL              = "";


	//------------------------------------------
	// if debug is sent, print some extra info on the outstream
	//-------
	$debug = getUrlParam('debug');
	if ( $debug == "true" )
	{
		if(!empty($_POST))
			echo "DEBUG: getting data from _POST variable\n";
		else if(!empty($_GET))
			echo "DEBUG: getting data from _GET variable\n";
		else
			echo "DEBUG: no _POST, no _GET, so what should I do...\n";
	}


	//------------------------------------------
	// GET latest version information
	//-------
	$clientSrcVer = getUrlParam('clientSourceVersion');
	if (!isset($clientSrcVer))
		$clientSrcVer     = -1;

	if ( $clientSrcVer < 0 )
	{
		echo "ERROR: clientSourceVersion was NOT PASSED... So to few parameters was passed, NO-UPGRADE will be the result.\n";
		echo "ERROR: _POST count = " . count($_POST) . "\n";
		echo "ERROR: _GET  count = " . count($_POST) . "\n";
		$clientSrcVer = 9999999;
	}
	if ( $debug == "true" )
		echo "DEBUG: clientSrcVer = '$clientSrcVer'\n";


	//------------------------------------------
	// CHECK if later version exists
	//-------
	if ($clientSrcVer < $ASEMON_LATEST_VERSION_SRC)
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
	// Below is properties sent by the client, vstuff them into local variables
	//-------
	$clientCheckTime         = getUrlParam('clientCheckTime');

	$clientSourceDate        = getUrlParam('clientSourceDate');
	$clientSourceVersion     = getUrlParam('clientSourceVersion');
	$clientAppVersionStr     = getUrlParam2('clientAseTuneVersion', 'clientAsemonVersion');

	$clientHostName          = getUrlParam('clientHostName');
	$clientHostAddress       = getUrlParam('clientHostAddress');
	$clientCanonicalHostName = getUrlParam('clientCanonicalHostName');

	$user_name               = getUrlParam('user_name');
	$user_dir                = getUrlParam('user_dir');
	$propfile                = getUrlParam('propfile');
	$gui                     = getUrlParam('gui');
	$sun_desktop             = getUrlParam('sun_desktop');
	$user_country            = getUrlParam('user_country');
	$user_language           = getUrlParam('user_language');
	$user_timezone           = getUrlParam('user_timezone');

	$java_version            = getUrlParam('java_version');
	$java_vm_version         = getUrlParam('java_vm_version');
	$java_vm_vendor          = getUrlParam('java_vm_vendor');
	$java_home               = getUrlParam('java_home');
	$java_class_path         = getUrlParam('java_class_path');
	$memory                  = getUrlParam('memory');
	$os_name                 = getUrlParam('os_name');
	$os_version              = getUrlParam('os_version');
	$os_arch                 = getUrlParam('os_arch');


	//------------------------------------------
	// If user is 'gorans', get out of here otherwise I will flod the log with personal entries
	//-------
	if ( $user_name == "gorans" )
	{
		if ( $debug == "true" )
			echo "DEBUG STOP PROCESSING: CHECK FOR UPDATE, user is '$user_name'.\n";
		exit;
	}
	if ( empty($user_name ) )
	{
		if ( $debug == "true" )
			echo "DEBUG STOP PROCESSING: CHECK FOR UPDATE, user is '$user_name'.\n";
		exit;
	}
	if ( $user_name == "gorans" )
	{
		echo "ERROR: ---WARNING--- THE USER 'GORANS' SHOULD BE LOG DISABLED, HOPEFULLY THIS IS A TEST.\n";
	}

	//------------------------------------------
	// Now connect to the database and insert a usage record
	//-------
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
		user_timezone,

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
		'$clientAppVersionStr',

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
		'$user_timezone',

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
