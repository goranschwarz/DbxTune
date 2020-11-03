/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.central.controllers;

public class HtmlStatic
{

	public static String getUserDefinedContentHead()
	{
		return getOverviewHead();
	}

	public static String getUserDefinedContentNavbar()
	{
		return getOverviewNavbar();
	}

	public static String getUserDefinedContentJavaScriptAtEnd()
	{
		return getJavaScriptAtEnd(true);
	}


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
        "  <script type='text/javascript' src='/scripts/popper/1.12.9/popper.min.js'></script> \n" +
		"  <script type='text/javascript' src='/scripts/bootstrap/js/bootstrap.min.js'></script> \n" +
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
		"  <link rel='stylesheet' href='/scripts/font-awesome/4.4.0/css/font-awesome.min.css'> \n" +
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
		"	  <!-- on the right hand side of the menu... --> \n" +
		"      <ul class='navbar-nav'> \n" +
		"	    <!-- IS LOGGED IN --> \n" +
		"        <div id='dbx-nb-isLoggedIn-div' style='display: none;'> \n" +
		"          <li class='nav-item dropdown'> \n" +
		"            <a class='nav-link dropdown-toggle' href='http://example.com' id='navbarDropdownMenuLink' data-toggle='dropdown' aria-haspopup='true' aria-expanded='false'> \n" +
		"              <i class='fa fa-user'></i> <span id='dbx-nb-isLoggedInUser-div'></span> <!-- current username will be in here --> \n" +
		"            </a> \n" +
		"            <div class='dropdown-menu dropdown-menu-right' aria-labelledby='navbarDropdownMenuLink'> \n" +
		"            <a class='dropdown-item' href='#'> <i class='fa fa-cog'></i> Settings [not-yet-implemented]</a> \n" +
		"            <a class='dropdown-item' href='/logout'> <i class='fa fa-sign-out'></i> Logout</a> \n" +
		"            </div> \n" +
		"          </li> \n" +
		"        </div> \n" +
		"	    <!-- IS LOGGED OUT --> \n" +
		"        <div id='dbx-nb-isLoggedOut-div'> \n" +
		"          <a class='nav-link' href='/index.html?login'> <i class='fa fa-sign-in'></i> <span data-toggle='tooltip' title='Log in as a specific user.'>Login</span></a> \n" +
		"        </div> \n" +
		"      </ul> <!-- end right hand side --> \n" +
		"    </div> \n" +
		"  </nav> \n" +
		"";
	}

	public static String getJavaScriptAtEnd(boolean addScriptTag)
	{
		StringBuilder sb = new StringBuilder();
		
		if (addScriptTag)
			sb.append("<script>");

		sb.append("\n");
		sb.append("\n");
		sb.append("	//----------------------------------------------------------- \n");
		sb.append("	// Check for login and set props \n");
		sb.append("	//----------------------------------------------------------- \n");
		sb.append("	isLoggedIn( function(isLogedIn, asUserName)  \n");
		sb.append("	{ \n");
		sb.append("		console.log('isLoggedIn-callback: isLogedIn=|'+isLogedIn+'|, asUserName=|'+asUserName+'|.'); \n");
		sb.append("	}); \n");

		if (addScriptTag)
			sb.append("</script>");
		
		return sb.toString();
	}
}
