package com.asetune.central.controllers;

public class HtmlStatic
{

	public static String getOverviewHead()
	{
		return
		"  <!--  \n" +
		"    ======================================================================= \n" +
		"    == JS imports - JAVA SCRIPTS GOES HERE \n" +
		"    ======================================================================= \n" +
		"  --> \n" +
		"  <!-- JS: JQuery --> \n" +
		"  <script type='text/javascript' src='/scripts/jquery/jquery-3.2.1.min.js'></script> \n" +
		"   \n" +
		"  <!-- JS: Moment; used by: ChartJs, DateRangePicker --> \n" +
		"  <script type='text/javascript' src='/scripts/moment/moment.js'></script> \n" +
		"  <script type='text/javascript' src='/scripts/moment/moment-duration-format.js'></script> \n" +
        " \n" +
		"  <!-- JS: Bootstrap --> \n" +
        "  <script src='/scripts/popper/1.12.9/popper.min.js'></script> \n" +
		"  <script src='/scripts/bootstrap/js/bootstrap.min.js'></script> \n" +
        " \n" +
		"  <!-- JS: DbxCentral --> \n" +
		"  <script type='text/javascript' src='/scripts/dbxcentral.utils.js'></script> \n" +
        " \n" +
		"  <!--  \n" +
		"    ======================================================================= \n" +
		"    == CSS imports - STYLES SHEETS GOES HERE \n" +
		"    ======================================================================= \n" +
		"  --> \n" +
		"  <!-- CSS: DbxCentral --> \n" +
		"  <link rel='stylesheet' href='/scripts/css/dbxcentral.css'> \n" +
        " \n" +
		"  <!-- CSS: Bootstrap --> \n" +
		"  <link rel='stylesheet' href='/scripts/bootstrap/css/bootstrap.min.css'> \n" +
        " \n" +
		"  <!-- CSS: Font Awsome --> \n" +
		"  <link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css'> \n" +
		"";
		
	}

	public static String getOverviewNavbar()
	{
		return
		"  <nav class='navbar navbar-expand-md navbar-dark sticky-top bg-dark mb-0'> \n" +
		"    <a class='navbar-brand' href='/'>DbxCentral</a> \n" +
		"    <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarCollapse' aria-controls='navbarCollapse' aria-expanded='false' aria-label='Toggle navigation'> \n" +
		"        <span class='navbar-toggler-icon'></span> \n" +
		"    </button> \n" +
		"    <div class='collapse navbar-collapse' id='navbarCollapse'> \n" +
		"      <ul class='navbar-nav mr-auto'> \n" +
		"        <li class='nav-item active'> \n" +
		"          <a class='nav-link' href='/overview'>Servers<span class='sr-only'>(current)</span></a> \n" +
		"        </li> \n" +
		"        <li class='nav-item'> \n" +
		"          <a class='nav-link' href='/admin/admin.html'>Admin</a> \n" +
		"        </li> \n" +
		"        <li class='nav-item'> \n" +
		"          <a class='nav-link' href='/desktop_app.html'>Desktop App</a> \n" +
		"        </li> \n" +
		"      </ul> \n" +
		"    </div> \n" +
		"  </nav> \n" +
		"";
	}
}
