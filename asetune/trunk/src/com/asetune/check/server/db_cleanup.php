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
	function doCleanup($sql)
	{
		echo "EXEC: <code>$sql</code><br>\n";
		mysql_query($sql) or die("ERROR: " . mysql_error());
		printf("Records affected: %d<br>\n", mysql_affected_rows());
		printf("<br>\n");
	}


	//------------------------------------------
	// Now connect to the database
	//-----
	$db=mysql_connect("localhost", "asemon_se", "UuWb3ETM") or die("ERROR: " . mysql_error());
	mysql_select_db("asemon_se", $db) or die("ERROR: " . mysql_error());

	echo "<h1>Connected to database</h1>\n";

	doCleanup("delete from asemon_usage where user_name = 'rlarsson'");
	doCleanup("delete from asemon_usage where user_name = 'gorans'");
	doCleanup("delete from asemon_usage where user_name = ''");

	doCleanup("delete from asemon_usage              where user_name = 'gorans' ");
	doCleanup("delete from asemon_connect_info       where userName  = 'gorans' ");
	doCleanup("delete from asemon_udc_info           where userName  = 'gorans' ");
	doCleanup("delete from asemon_counter_usage_info where userName  = 'gorans' ");

	// doCleanup("delete from asemon_usage              where user_name = 'gorans' or user_name = 'sybase'");
	// doCleanup("delete from asemon_udc_info");

	// doCleanup("alter table asemon_connect_info add column usePcs     varchar(5)");
	// doCleanup("alter table asemon_connect_info add column pcsConfig  varchar(400)");

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
