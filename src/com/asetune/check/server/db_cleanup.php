<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="/favicon.ico"/>
<title>AseTune DB Cleanup</title>

<SCRIPT src="sorttable.js"></SCRIPT>
<STYLE type="text/css">
  /* Sortable tables */
  table.sortable thead {
    background-color:#eee;
    color:#666666;
    font-weight: bold;
    cursor: default;
  }
  body { font-size : 100%; font-family : Verdana,Helvetica,Arial,sans-serif; }
  h1, h2, h3 { font-size : 150%; }
  table { margin: 1em; border-collapse: collapse; font-size : 90%; }
  td, th { padding: .1em; border: 1px #ccc solid; font-size : 90%; }
  thead { background: #fc9; } </STYLE>
</HEAD>
<BODY>

<?php
	//------------------------------------------
	// Below is properties sent by the client, vstuff them into local variables
	//------------------------------------------
	$doAction = $_GET['doAction'];

	function doCleanup($sql)
	{
		echo "EXEC: <code>$sql</code><br>\n";
		mysql_query($sql) or die("ERROR: " . mysql_error());
		printf("Records affected: %d<br>\n", mysql_affected_rows());
		printf("<br>\n");
	}

	function describe($tabname)
	{
		echo "DESCRIBE: <code>$tabname</code><br>\n";

		$sql = "DESCRIBE $tabname";
		$result = mysql_query($sql) or die("ERROR: " . mysql_error());
		if (!$result) {
			echo mysql_errno() . ": " . mysql_error() . "<br>";
			die("ERROR: Query to show fields from table failed");
		}

		$fields_num = mysql_num_fields($result);

		// printing table header
		echo "<table border=\"0\" class=\"sortable\">";
		echo "<tr>";
		// printing table headers
		for($i=0; $i<$fields_num; $i++)
		{
			$field = mysql_fetch_field($result);
			echo "<td nowrap>{$field->name}</td>";
		}
		echo "</tr>\n";

		// printing table rows
		while($row = mysql_fetch_row($result))
		{
			echo "<tr>";

			// $row is array... foreach( .. ) puts every element
			// of $row to $cell variable
			$col=-1;
			foreach($row as $cell)
			{
				$col++;
				$colname = mysql_field_name($result, $col);

				//$cellCont = nl2br($cell, false);
				//echo "<td nowrap>$cellCont</td>";
				echo "<td nowrap>$cell</td>";
			}

			echo "</tr>\n";
		}
		echo "</table>\n";
		mysql_free_result($result);

		printf("<br>\n");
	}


	//------------------------------------------
	// Now connect to the database
	//-----
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	echo "<h1>Connected to database</h1>\n";

	if ( $doAction == "cleanup" )
	{
		doCleanup("delete from asemon_usage where user_name = 'rlarsson'");
		doCleanup("delete from asemon_usage where user_name = 'gorans'");
		doCleanup("delete from asemon_usage where user_name = ''");

		doCleanup("delete from asemon_usage              where user_name = 'gorans' ");
		doCleanup("delete from asemon_connect_info       where userName  = 'gorans' ");
		doCleanup("delete from asemon_udc_info           where userName  = 'gorans' ");
		doCleanup("delete from asemon_counter_usage_info where userName  = 'gorans' ");
		doCleanup("delete from asemon_error_info         where userName  = 'gorans' ");

		// doCleanup("delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'");
		// doCleanup("delete from asemon_udc_info");
		// doCleanup("update asemon_usage set clientAsemonVersion = '2.1.0.1.dev' where clientAsemonVersion = '2.1.1.dev'");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "dbmaint" )
	{
		// doCleanup("alter table asemon_connect_info add column usePcs     varchar(5)");
		// doCleanup("alter table asemon_connect_info add column pcsConfig  varchar(400)");

		// doCleanup("ALTER TABLE asemon_error_info DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_error_info ADD PRIMARY KEY (checkId, sendCounter, serverAddTime)");
		// doCleanup("ALTER TABLE asemon_error_info MODIFY logMessage varchar(4096)");
		// doCleanup("ALTER TABLE asemon_usage ADD callerIpAddress varchar(20)");

		// doCleanup("ALTER TABLE asemon_connect_info       ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup("ALTER TABLE asemon_connect_info       DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_connect_info       ADD  PRIMARY KEY (checkId, connectId, serverAddTime)");

		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD COLUMN connectId INT NOT NULL DEFAULT -1");
		// doCleanup("ALTER TABLE asemon_counter_usage_info DROP PRIMARY KEY");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD  PRIMARY KEY (checkId, connectId, clientTime, cmName)");

		// doCleanup("ALTER TABLE asemon_connect_info       MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER isClusterEnabled");
		// doCleanup("ALTER TABLE asemon_counter_usage_info MODIFY connectId INT NOT NULL  DEFAULT -1  AFTER userName");

		// doCleanup("ALTER TABLE asemon_connect_info       MODIFY srvIpPort varchar(60)");


		// doCleanup("ALTER TABLE asemon_counter_usage_info CHANGE clientTime sessionStartTime TIMESTAMP");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD sessionType      varchar(10)     AFTER serverAddTime");
		// doCleanup("ALTER TABLE asemon_counter_usage_info ADD sessionEndTime   timestamp NULL  AFTER sessionStartTime");

		// doCleanup("ALTER TABLE asemon_usage MODIFY callerIpAddress varchar(20) AFTER clientCanonicalHostName");

		// doCleanup("ALTER TABLE asemon_usage ADD user_home varchar(50) AFTER user_name");


		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else if ( $doAction == "check" )
	{
		describe("asemon_usage");
		describe("asemon_connect_info");
		describe("asemon_udc_info");
		describe("asemon_counter_usage_info");
		describe("asemon_error_info");

		echo "<i><b>--- END OF COMMANDS ---</b></i>\n";
	}
	else
	{
		echo "<h1>UNSUPPORTED COMMAND '" . $doAction . "'</h1>\n";
		echo "Commands:<br>\n";
		echo "<ul>\n";
		echo "  <li><code>cleanup</code> - Delete gorans from various tables</li>\n";
		echo "  <li><code>dbmaint</code> - Do some DB maintenace command, needs to be coded in the PHP script. Usually this section will be empty</li>\n";
		echo "  <li><code>check  </code> - Do 'DESCRIBE' on all known tables.</li>\n";
		echo "</ul>\n";
	}

	//------------------------------------------
	// Close connection to the database
	//-----
	echo "<h1>Closing database</h1>\n";
	mysql_close() or die("ERROR: " . mysql_error());
?>
<BR>
-END-
<BR>

</BODE>
</HTML>
