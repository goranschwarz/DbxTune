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
	function versionFix($version)
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
	// FUNCTION: htmlResultset
	//----------------------------------------
	function htmlResultset($result, $headName, $colNameForNewLine='')
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

//				else if ( $colname == "user_name" )
//					echo "<td nowrap><A HREF=\"https://sapneth1.wdf.sap.corp/~form/handler?_APP=00200682500000002283&_EVENT=DISPLAY&00200682500000002187=" . $cell . "\" target=\"_blank\">$cell</A></td>";

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

				else if ( $colname == "srvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda_isCluster=0&mda=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "deleteSrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_deleteVersion=" . $cell . "\">$cell</A></td>";

				else if ( $colname == "verifySrvVersion" )
					echo "<td nowrap><A HREF=\"http://www.asemon.se/usage_report.php?mda=delete&mda_verifyVersion=" . $cell . "\">$cell</A></td>";
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
