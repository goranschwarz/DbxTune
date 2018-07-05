<%@page import="java.net.InetAddress"%>
<html>

<head>
	<title>AseTune - Central</title>
</head>
<style type='text/css'>
  table {border-collapse: collapse;}
  th, td {border: 1px solid black; text-align: left; padding: 2px;}
  tr:nth-child(even) {background-color: #f2f2f2;}
</style>

<body>
<%
	String atLocation = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();	
%>

<h1>AseTune - Central - <%= atLocation %></h1>

List of stuff that can be done
<ul>
  <li> <a href='overview'           > Overview of Alarms and Recordings </a> </li>
  <li> <a href='admin'              > Admin this central instance </a> </li>
  <li> <a href='not-yet-implemented'> View online and historical performance charts for the monitored servers </a> </li>
  <li> <a href='not-yet-implemented'> Download the native/desktop AseTune application</a> </li>
</ul>


<a href="dummyfile.txt">dummyfile.txt</a>
</body>

</html>
