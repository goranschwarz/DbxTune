<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<HTML>
<HEAD>
<TITLE>JDBC Drivers Download</TITLE>
<link rel="icon" type="image/png" href="/favicon.ico"/>
</HEAD>

<BODY>

<H1>Download Various JDBC Drivers</H1>

How to use the drivers in SQL Window<br>
<UL>
<?php
	require("gorans_functions.php");
	$toLocation = $_GET['toLocation'];

echo "<li>Down load a JDBC Driver from the list</li>";

echo "<li>Save the downloaded JDBC Driver in directory: <br>";
echo "<code><b>" . $toLocation . "</b></code> <br>";
echo "</li>";

echo "<li>Restart SQL Window (hopefully the driver will be visible in the list at the bottom)</li>";

echo "<li>Connect agin, goto tab 'JDBC'</li>";
echo "<ul>";
echo "<li><b>JDBC Driver</b> Fill in the driver name, Example: com.sybase.jdbc4.jdbc.SybDriver</li>";
echo "<li><b>JDBC Url</b>    Fill in the desired URL, Examlpe: jdbc:sybase:Tds:h1.acme.com:5000</li>";
echo "<li><b>Username</b>    The user you want to connect with</li>";
echo "<li><b>Password</b>    Some password</li>";
echo "</ul>";
?>
</UL>


<H2>Below is a table of drivers and there download URL</H2>

<b>NOTE:</b> The below driver I just googled and inserted in the list<br>
If you want to add or change this list please email me at: goran_schwarz (at) hotmail.com<br>
<br>
The list was last edited: 2013-11-14<br>
<br>

<TABLE ALIGN="left" BORDER=0 CELLSPACING=1 CELLPADDING=0">
<TR ALIGN="left" VALIGN="middle">
	<TH>Vendor</TH>
	<TH>Download URL</TH>
</TR>

<!-- Sybase ASE -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Sybase ASE, IQ, SA, RepServer &nbsp;&nbsp;</TD>
	<TD> <A HREF="http://www.sybase.com/products/allproductsa-z/softwaredeveloperkit/jconnect" target="_blank">http://www.sybase.com/products/allproductsa-z/softwaredeveloperkit/jconnect</A> </TD>
</TR>

<!-- SAP HANA -->
<TR ALIGN="left" VALIGN="middle">
	<TD> SAP HANA </TD>
	<TD> <A HREF="http://?" target="_blank">xxx.acme.com</A> </TD>
</TR>

<!-- Microsoft SQL Server -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Microsoft SQL Server </TD>
	<TD> <A HREF="http://msdn.microsoft.com/en-us/sqlserver/aa937724.aspx" target="_blank">http://msdn.microsoft.com/en-us/sqlserver/aa937724.aspx</A> </TD>
</TR>

<!-- jTDS -->
<TR ALIGN="left" VALIGN="middle">
	<TD> jTDS - MS SQL, Sybase ASE </TD>
	<TD> <A HREF="http://jtds.sourceforge.net/" target="_blank">http://jtds.sourceforge.net/</A> </TD>
</TR>

<!-- Oracle -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Oracle </TD>
	<TD> <A HREF="http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html" target="_blank">http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html</A> </TD>
</TR>

<!-- IBM DB2 -->
<TR ALIGN="left" VALIGN="middle">
	<TD> IBM DB2 </TD>
	<TD> <A HREF="http://www-01.ibm.com/support/docview.wss?uid=swg21363866" target="_blank">http://www-01.ibm.com/support/docview.wss?uid=swg21363866</A> </TD>
</TR>

<!-- Terradata -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Terradata </TD>
	<TD> <A HREF="http://downloads.teradata.com/download/connectivity/jdbc-driver" target="_blank">http://downloads.teradata.com/download/connectivity/jdbc-driver</A> </TD>
</TR>

<!-- Informix -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Informix </TD>
	<TD> <A HREF="http://?" target="_blank">xxx.acme.com</A> </TD>
</TR>

<!-- MySql -->
<TR ALIGN="left" VALIGN="middle">
	<TD> MySql </TD>
	<TD> <A HREF="http://dev.mysql.com/downloads/connector/j/" target="_blank">http://dev.mysql.com/downloads/connector/j/</A> </TD>
</TR>

<!-- H2 -->
<TR ALIGN="left" VALIGN="middle">
	<TD> H2 </TD>
	<TD> <A HREF="http://www.h2database.com/html/download.html" target="_blank">http://www.h2database.com/html/download.html</A> </TD>
</TR>

<!-- PostgreSQL -->
<TR ALIGN="left" VALIGN="middle">
	<TD> PostgreSQL </TD>
	<TD> <A HREF="http://jdbc.postgresql.org/download.html" target="_blank">http://jdbc.postgresql.org/download.html</A> </TD>
</TR>

<!-- HSQL Database Engine -->
<TR ALIGN="left" VALIGN="middle">
	<TD> HSQL Database Engine </TD>
	<TD> <A HREF="http://?" target="_blank">xxx.acme.com</A> </TD>
</TR>

<!-- Apache Derby -->
<TR ALIGN="left" VALIGN="middle">
	<TD> Apache Derby </TD>
	<TD> <A HREF="http://db.apache.org/derby/docs/10.7/devguide/cdevdvlp40653.html" target="_blank">http://db.apache.org/derby/docs/10.7/devguide/cdevdvlp40653.html</A> </TD>
</TR>

</TABLE>



</BODY>
</HTML>
