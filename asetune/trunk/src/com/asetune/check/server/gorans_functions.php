<?php
//--- BEGINING OF FILE

	//----------------------------------------
	// FUNCTION: getUrlParam from the POST or the GET (url params)
	//----------------------------------------
	function getUrlParam($param, $debug='false')
	{
		if(!empty($_POST))
		{
			$val = $_POST[$param];
		}
		else if(!empty($_GET))
		{
			$val = urldecode($_GET[$param]);
		}
		if ( $debug == "true" )
			echo "DEBUG: getUrlParam='$param', val='$val'.\n";

		return $val;
	}
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

	//----------------------------------------
	// FUNCTION: get POST or GET
	//----------------------------------------
	function getDataArray()
	{
		if(!empty($_POST))
		{
			return $_POST;
		}
		else if(!empty($_GET))
		{
			return $_GET;
		}
		return array();
	}

	//----------------------------------------
	// FUNCTION: get "callers" IP address
	//----------------------------------------
	function get_ip_address()
	{
		foreach (array('HTTP_CLIENT_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_X_FORWARDED', 'HTTP_X_CLUSTER_CLIENT_IP', 'HTTP_FORWARDED_FOR', 'HTTP_FORWARDED', 'REMOTE_ADDR') as $key)
		{
			if (array_key_exists($key, $_SERVER) === true)
			{
				foreach (explode(',', $_SERVER[$key]) as $ip)
				{
					if (filter_var($ip, FILTER_VALIDATE_IP) !== false)
					{
						return $ip;
					}
				}
			}
		}
	}
	//$callerIpAddress = get_ip_address();

	//----------------------------------------
	// FUNCTION: toSqlNumber
	//----------------------------------------
	function toSqlNumber($input)
	{
		if ($input == "")
			return "NULL";
		return $input;
	}

	//----------------------------------------
	// FUNCTION: toSqlStr
	//----------------------------------------
	function toSqlStr($input)
	{
		if ($input == "")
			return "NULL";
		return "'" . mysql_real_escape_string($input) . "'";
	}

	//----------------------------------------
	// FUNCTION: upgrade version from "small" version number to "big" version number
	//----------------------------------------
	function versionFixOLD($version)
	{
		// If the input is empty, lets just return with nothing...
		if (strlen($version) == 0)
			return $version;

		// If the input is a number below 0: do nothing its a offline-read session
		if ($version < 0)
			return $version;

		// If version is to "short" expand it to 5 characters
		while (strlen($version) <= 5)
			$version = $version . "0";

		// if its a "short" version, convert it to a "long" version
		if (strlen($version) < 7)
		{
			// Upgrade version from 12503 to 1250030
			// keep first 4 numbers
			// the last number we will expand to 3 numbers (esd#4->040, esd#4.2->042)
			// this to be able to handle SAP/Sybase New Version strings with Service Packs (ASE 15.7.0 SP101 will be 1570101)
			$baseVersion = substr($version, 0, 4);
			$esdVersion  = substr($version, 4);

			if      (strlen($esdVersion) == 1) $esdVersion = "0" . $esdVersion . "0";
			else if (strlen($esdVersion) == 2) $esdVersion = "0" . $esdVersion;
			else if (strlen($esdVersion) == 3) $esdVersion = $esdVersion;
			else $esdVersion = substr($esdVersion, 0, 3);;

			$version = $baseVersion . $esdVersion;

			return $version;
		}
		else // Do nothing if already at the new "big" version (or chop it off after  7 chars if it's to long)
		{
			return substr($version, 0, 7);
		}
	}

	//----------------------------------------
	// FUNCTION: upgrade version from "small" version number to "big" version number
	// short  version: 12540     len=5, (Major[##] Minor[#] Maint[#] Esd[#])
	// medium version: 1570100   len=7, (Major[##] Minor[#] Maint[#] SP[###])         if before 15.7 SP, then SP will have '123' 1=zero, 2=ESD Level, 3=ESD PathLevel (esd#4.2)
	// long   version: 160000101 len=5, (Major[##] Minor[#] Maint[#] SP[###] PL[##])  new stuff... for older esd[x.y]. x partwill be held by the 3 digit SP, y part will be held by PL
	//----------------------------------------
//NOTE: NOT READY YET
// test it with: http://www.asemon.se/db_cleanup.php?doAction=testVersion&version=1570100
	function versionFix($version)
	{
		// If the input is empty, lets just return with nothing...
		if (strlen($version) == 0)
			return $version;

		// If the input is a number below 0: do nothing its a offline-read session
		if ($version < 0)
			return $version;

		// If version is to "short" expand it to 5 characters
		while (strlen($version) < 5)
			$version = $version . "0";

//echo "VER = '" . $version . "'<br>";
//echo "LEN = " . strlen($version) . "<br>";
		// if its a "short" version, convert it to a "long" version (AseTune version 3.2.0 and older)
		if (strlen($version) < 7)
		{
//echo "xxxxx < 7 <br>";
			// Upgrade version from 12503 to 125000300
			// keep first 4 numbers
			// the last number we will expand to 3 numbers (esd#4->040, esd#4.2->042)
			// this to be able to handle SAP/Sybase New Version strings with Service Packs (ASE 15.7.0 SP101 will be 1570101)
			$baseVersion = substr($version, 0, 4);
			$esdVersion  = substr($version, 4);

			if      (strlen($esdVersion) == 1) $esdVersion = "00" . $esdVersion;
			else if (strlen($esdVersion) == 2) $esdVersion = "0" . $esdVersion;
			else if (strlen($esdVersion) == 3) $esdVersion = $esdVersion;
			else $esdVersion = substr($esdVersion, 0, 3);

			$plVersion = "00";
			$version = $baseVersion . $esdVersion . $plVersion;

			return $version;
		}

		// if its a "medium" version, convert it to a "long" version (AseTune version 3.3.x)
		// xxyzppp -> xxyzpppll
		if (strlen($version) < 9)
		{
//echo "YYYYY < 9 <br>";
			// Upgrade version from 1250030 to 125000300
			// Upgrade version from 1570050 to 157005000
			// Upgrade version from 1570100 to 157010000
			// Upgrade version from 1570120 to 157012000
			// keep first 4 numbers
			// the last number we will expand to 5 numbers (esd#4->00400, esd#4.2->00402)
			// this to be able to handle SAP/Sybase New Version strings with Service Packs and Patch Level
			//      ASE 12.5.4 ESD#10.1  will be 125401001
			//      ASE 15.5.0 ESD#5.2   will be 155000502
			//      ASE 15.7.0 ESD#4.2   will be 157000402
			//      ASE 15.7.0 SP51      will be 157005100
			//      ASE 15.7.0 SP100     will be 157010000
			//      ASE 15.7.0 SP120     will be 157012000
			//      ASE 15.7.0 SP01 PL01 will be 157000101
			$baseVersion = substr($version, 0, 4);
			$spVersion = substr($version, 4);
			$plVersion = "00";

			$baseVersionInt = intval($baseVersion);
			$spVersionInt   = intval($spVersion);
//echo "baseVersionInt = " . $baseVersionInt . "<br>";
//echo "spVersionInt   = " . $spVersionInt . "<br>";


			if ($baseVersionInt >= 1600)
			{
				$spVersion = substr($version, 4, 3);
				$plVersion = "00";
			}
			else if ($baseVersionInt >= 1570 && $spVersionInt >= 50) // if 15.7 SP50 or above (this is where we started with SP)
			{
				$spVersion = substr($version, 4, 3);
				$plVersion = "00";
			}
			else // Convert ESD#x.y for PRE 15.7 servers
			{
				$spVersion = "00" . substr($version, 5, 1);
				$plVersion = "0"  . substr($version, 6, 1);
			}

			$version = $baseVersion . $spVersion . $plVersion;

			return $version;
		}
		else // Do nothing if already at the new "big" version (or chop it off after  9 chars if it's to long)
		{
			return substr($version, 0, 9);
		}
	}
	// SQL UPDATE:
	//
	// Upgrade version from 1250030 to 125000300
	// Upgrade version from 1570050 to 157005000
	// Upgrade version from 1570100 to 157010000
	// Upgrade version from 1570120 to 157012000
	//
//		doCleanup("update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 100000000 AND srvVersion > 0");
//		doCleanup("update asemon_mda_info         set serverAddTime = serverAddTime, srvVersion = ((srvVersion DIV 10 * 1000) + ((srvVersion % 10)*10))   WHERE srvVersion < 100000000 AND srvVersion > 0");

		// FIX VERSION for CHAR columns
//		doCleanup("update asemon_error_info       set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 100000000");
//		doCleanup("update asemon_error_info2      set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 100000000");
//		doCleanup("update asemon_error_info_save  set serverAddTime = serverAddTime, srvVersion = ((CONVERT(srvVersion,SIGNED INTEGER) DIV 10 * 1000) + ((CONVERT(srvVersion,SIGNED INTEGER) % 10)*10))   WHERE CONVERT(srvVersion,SIGNED INTEGER) < 100000000");

        // FIX offline-read settings...
//		doCleanup("update asemon_connect_info     set serverAddTime = serverAddTime, srvVersion = -1   WHERE srvVersion < 0");



	//----------------------------------------
	// FUNCTION: htmlResultset
	//----------------------------------------
	function versionDisplay($version)
	{
		if ( ! is_numeric($version) )
			return $version;

		if (strlen($version) == 9)
		{
			$baseVersion = substr($version, 0, 4);
			$spVersion   = substr($version, 4, 3);
			$plVersion   = substr($version, 7);

			return $baseVersion . " " . $spVersion . " " . $plVersion;
		}

		return $version;
	}

	//----------------------------------------
	// FUNCTION: htmlResultset
	//----------------------------------------
	function htmlResultset($userIdCache, $result, $headName, $colNameForNewLine='')
	{
		$colIdForNewLine = -1;
		$fields_num = mysql_num_fields($result);

		// printing some info about what this is
		echo "<h1>" . $headName . "</h1>";

		// printing table header
		echo "<table border=\"0\" class=\"sortable\">";
		echo "<tr>";
		// printing table headers
		for($i=0; $i<$fields_num; $i++)
		{
			$field = mysql_fetch_field($result);
			echo "<td nowrap>{$field->name}</td>";
			if ($colNameForNewLine != "")
			{
				if ($colNameForNewLine == "{$field->name}")
					$colIdForNewLine = $i;
			}
		}
		echo "</tr>\n";

		$atRow = -1;

		// printing table rows
		while($row = mysql_fetch_row($result))
		{
			$atRow++;
			echo "<tr>";

			// Make a new line, if a repeatable column changes content (make a separator)
			if ($colIdForNewLine >= 0)
			{
				$colValue = $row[$colIdForNewLine];

				if ($atRow >= 1)
				{
					if ($lastColValForNewLine != $colValue)
						echo "<tr> <td><br></td> </tr>";
				}
				$lastColValForNewLine = $colValue;
			}

			// $row is array... foreach( .. ) puts every element
			// of $row to $cell variable
			$col=-1;
			foreach($row as $cell)
			{
				$col++;
				$colname = mysql_field_name($result, $col);

				if ( $colname == "sybUserName" )
					echo "<td nowrap><A HREF=\"http://syberspase.sybase.com/compdir/mainMenu.do?keyword=$cell&submit=Go\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "sapUserNameXXXXX" )
					echo "<td nowrap><A HREF=\"https://sapneth2.wdf.sap.corp/~form/handler?_APP=00200682500000002283&_EVENT=SEARCH&UserID=" . $cell . "\">$cell</A></td>";

//				else if ( $colname == "sapUserName" )
//					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?userName=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "sapUserName" )
					echo "<td nowrap><A HREF=\"https://sapneth1.wdf.sap.corp/~form/handler?_APP=00200682500000002283&_EVENT=DISPLAY&00200682500000002187=" . $cell . "\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "user_name" || $colname == "userName" )
				{
					// if SAP user, then link to the description page()
					if (preg_match("/[iIdD][0-9][0-9][0-9][0-9][0-9][0-9]/", $cell))
					{
						$userName = $userIdCache[ strtoupper($cell) ];
						if ( ! empty($userName) )
							$userName = " (" . $userName . ")";

						echo "<td nowrap>";
						echo "<A HREF=\"http://www.asemon.se/usage_report.php?onUser=" . $cell . "\">$cell</A>";
						echo ", SAP: <A HREF=\"https://people.wdf.sap.corp/profiles/" . $cell . "\" target=\"_blank\">$cell</A>";
						echo $userName;
						echo "</td>";
					}
					else
					{
						echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onUser=" . $cell . "\">$cell</A></td>";
//						$cellCont = nl2br($cell, false);
//						echo "<td nowrap>$cellCont</td>";
					}
				}

				else if ( $colname == "deleteUserIdDesc" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?userId_key=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "userNameUsage" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onUser=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "domainName" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onDomain=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "wwwDomainName" )
					echo "<td nowrap><A HREF=\"http://$cell\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "checkId" || $colname == "rowid")
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?onId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "sqlwCheckId" || $colname == "sqlwRowId")
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?sqlw=true&sqlwConnId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "showLogId" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "deleteLogId" || $colname == "deleteLogId2" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=sum&deleteLogId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "saveLogId" || $colname == "saveLogId2" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?errorInfo=sum&saveLogId=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "callerIpAddress" )
					echo "<td nowrap><A HREF=\"http://whatismyipaddress.com/ip/" . $cell . "\" target=\"_blank\">$cell</A></td>";

				else if ( $colname == "getAppStartsForIp" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?getAppStartsForIp=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "getConnectForIp" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?getConnectForIp=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "getConnectForDomain" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?getConnectForDomain=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "srvVersion" || $colname == "srvVersionInt" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda_isCluster=0&mda=" . $cell . "\">" . versionDisplay($cell) . "</A></td>";

				else if ( $colname == "deleteSrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_deleteVersion=" . $cell . "\">" . versionDisplay($cell) . "</A></td>";

				else if ( $colname == "verifySrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_verifyVersion=" . $cell . "\">" . versionDisplay($cell) . "</A></td>";
				else
				{
					$cellCont = nl2br($cell, false);
					echo "<td nowrap>$cellCont</td>";
				}
			}

			echo "</tr>\n";
		}
		echo "</table>\n";
		mysql_free_result($result);

		// printing table rows
//		while($row = mysql_fetch_row($result))
//		{
//			echo "<tr>";
//
//			// $row is array... foreach( .. ) puts every element
//			// of $row to $cell variable
//			foreach($row as $cell)
//				echo "<td nowrap>$cell</td>";
//
//			echo "</tr>\n";
//		}
//		echo "</table>\n";
//		mysql_free_result($result);
	}


//--- END OF FILE
?>
