<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN">
<html>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="refresh" content="30">
<link rel="icon" type="image/png" href="/favicon.ico"/>
<title>Dummy Refresh</title>
</HEAD>
<BODY>


<?php
echo '<font size="99" face="arial"             >Current time is:</font>';
echo '<br>';
echo '<font size="99" face="arial" color="blue">' . date('Y-m-d H:i:s') . '</font> <font size="99" face="arial">Default date</font>';
echo '<br>';
date_default_timezone_set ("Europe/Stockholm");
echo '<font size="99" face="arial" color="blue">' . date('Y-m-d H:i:s') . '</font> <font size="99" face="arial">Europe/Stockholm </font>';
echo '<br>';
?>

</BODY>
</HTML>
