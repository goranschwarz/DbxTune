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
//
//	}
//	//----------------------------------------
//	// FUNCTION: get params from POST or GET
//	//           if first param is not set, try secondary param
//	//           if none was found return the default value.
//	//----------------------------------------
//	function getUrlParam2($param, $altParam)
//	{
//		$val = getUrlParam($param);
//		if (!empty($val))
//			return $val;
//
//		$val = getUrlParam($altParam);
//		if (!empty($val))
//			return $val;
//
//		return $val;
//	}
//
//	//----------------------------------------
//	// FUNCTION: get "callers" IP address
//	//----------------------------------------
//	function get_ip_address()
//	{
//		foreach (array('HTTP_CLIENT_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_X_FORWARDED', 'HTTP_X_CLUSTER_CLIENT_IP', 'HTTP_FORWARDED_FOR', 'HTTP_FORWARDED', 'REMOTE_ADDR') as $key)
//		{
//			if (array_key_exists($key, $_SERVER) === true)
//			{
//				foreach (explode(',', $_SERVER[$key]) as $ip)
//				{
//					if (filter_var($ip, FILTER_VALIDATE_IP) !== false)
//					{
//						return $ip;
//					}
//				}
//			}
//		}
//	}
//	//$callerIpAddress = get_ip_address();


	//------------------------------------------
	// DEFINE latest version information
	//-------
	$ASEMON_LATEST_VERSION_SRC = 600;
	$ASEMON_LATEST_VERSION_STR = "4.5.0";
	$ASEMON_LATEST_VERSION_STR = "2022-12-08";
	$DOWNLOAD_URL              = "sourceforge.net/projects/asetune/files/";
	$WHATSNEW_URL              = "www.dbxtune.com/history.html";
	$SEND_OPTIONS              = "sendConnectInfo=true, sendMdaInfo=true, sendMdaInfoBatchSize=5, sendUdcInfo=true, sendCounterUsageInfo=true, sendLogInfoWarning=true, sendLogInfoError=true, sendLogInfoThreshold=100";
//	$FEEDBACK_URL              = "2011-06-09:www.dbxtune.com/feedback.html";
	$FEEDBACK_URL              = "";
//	$PROBLEM_URL               = "2012-06-01:www.dbxtune.com/asetune_problem.html";
	$PROBLEM_URL               = "";

//	$FEEDBACK_URL              = $PROBLEM_URL;

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
	// SPECIAL FEEDBACK to JAVA 1.6 (1.7) users
	//-------
	$java_version = getUrlParam('java_version');
	if (preg_match('#^1.[67]#', $java_version) === 1)
	{
		$FEEDBACK_URL = "2016-04-24:www.dbxtune.com/do_java_upgrade.html";
	}

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
	//	echo "FEEDBACK: $PROBLEM_URL\n";
	}



	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//-------
	$clientCheckTime         = getUrlParam('clientCheckTime');

	$clientSourceDate        = getUrlParam('clientSourceDate');
	$clientSourceVersion     = getUrlParam('clientSourceVersion');
	$clientAppName           = getUrlParam('clientAppName');
	$clientAppVersionStr     = getUrlParam2('clientAseTuneVersion', 'clientAppVersion');
	$appStartupTime          = getUrlParam('appStartupTime');
	$clientExpireDate        = getUrlParam('clientExpireDate');

	$clientHostName          = getUrlParam('clientHostName');
	$clientHostAddress       = getUrlParam('clientHostAddress');
	$clientCanonicalHostName = getUrlParam('clientCanonicalHostName');

	$screenResolution        = getUrlParam('screenResolution');
	$hiDpiScale              = getUrlParam('hiDpiScale');

	$user_name               = getUrlParam('user_name');
	$user_home               = getUrlParam('user_home');
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
	$sun_arch_data_model     = getUrlParam('sun_arch_data_model');
	$java_home               = getUrlParam('java_home');
	$java_class_path         = getUrlParam('java_class_path');
	$memory                  = getUrlParam('memory');
	$os_name                 = getUrlParam('os_name');
	$os_version              = getUrlParam('os_version');
	$os_arch                 = getUrlParam('os_arch');

	$caller_ip               = get_ip_address();


	// Set default values for new fields that is not sent by older versions
	if ( $clientAppName == "" )
	{
		$clientAppName = "AseTune";
	}

	//------------------------------------------
	// If user is 'gorans', get out of here otherwise I will flod the log with personal entries
	//-------
	if ( $user_name == "goransXXX" )
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
	if ( $user_name == "goransXXXXXXXX" )
	{
		echo "ERROR: ---WARNING--- THE USER 'GORANS' SHOULD BE LOG DISABLED, HOPEFULLY THIS IS A TEST.\n";
	}

	//------------------------------------------
	// Now connect to the database and insert a usage record
	//-------
//	$db=mysql_connect("localhost", "dbxtune_com", "L8MucH4c") or die("ERROR: " . mysql_error());
//	mysql_select_db("dbxtune_com", $db) or die("ERROR: " . mysql_error());

	$dbconn=mysqli_connect("localhost", "dbxtune_com", "L8MucH4c", "dbxtune_com") or die("ERROR: " . mysqli_connect_error());

	//------------------------------------------
	// SQL_MODE option for this connection/session
	mysqli_query($dbconn, "SET SESSION sql_mode = 'ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");


	$sql = "insert into asemon_usage
	(
		serverAddTime,
		clientCheckTime,

		serverSourceVersion,

		clientSourceDate,
		clientSourceVersion,
		clientAppName,
		clientAsemonVersion,
		appStartupTime,
		clientExpireDate,

		clientHostName,
		clientHostAddress,
		clientCanonicalHostName,
		callerIpAddress,

		screenResolution,
		hiDpiScale,

		user_name,
		user_home,
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
		sun_arch_data_model,
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
		'$clientAppName',
		'$clientAppVersionStr',
		'$appStartupTime',
		'$clientExpireDate',

		'$clientHostName',
		'$clientHostAddress',
		'$clientCanonicalHostName',
		'$caller_ip',

		'$screenResolution',
		'$hiDpiScale',

		'$user_name',
		'$user_home',
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
		'$sun_arch_data_model',
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
	mysqli_query($dbconn, $sql) or die("ERROR: " . mysqli_error($dbconn));

	//------------------------------------------
	// Send a responce id
	printf("CHECK_ID: %d\n", mysqli_insert_id($dbconn));

	//------------------------------------------
	// Close connection to the database
	mysqli_close($dbconn) or die("ERROR: " . mysqli_error($dbconn));
?>
