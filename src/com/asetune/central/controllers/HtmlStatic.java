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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.asetune.utils.StringUtil;

public class HtmlStatic
{
	public enum PageSection
	{
		None, 
		Server, 
		Admin,
		DesktopApp
	};


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
		return getHtmlHead();
	}

	public static String getHtmlHead()
	{
		StringWriter out    = new StringWriter();
		PrintWriter  writer = new PrintWriter(out);

		writer.println("  <!--  ");
		writer.println("    ======================================================================= ");
		writer.println("    == JS imports - JAVA SCRIPTS GOES HERE ");
		writer.println("    ======================================================================= ");
		writer.println("  --> ");
		writer.println("  <!-- JS: JQuery --> ");
		writer.println("  <script type='text/javascript' src='/scripts/jquery/jquery-3.7.0.min.js'></script> ");
		writer.println();
		writer.println("  <!-- JS: Moment; used by: ChartJs, DateRangePicker --> ");
		writer.println("  <script type='text/javascript' src='/scripts/moment/moment.js'></script> ");
		writer.println("  <script type='text/javascript' src='/scripts/moment/moment-duration-format.js'></script> ");
        writer.println();
		writer.println("  <!-- JS: Bootstrap --> ");
        writer.println("  <script type='text/javascript' src='/scripts/popper/1.12.9/popper.min.js'></script> ");
		writer.println("  <script type='text/javascript' src='/scripts/bootstrap/js/bootstrap.min.js'></script> ");
        writer.println();
		writer.println("  <!-- JS: DbxCentral --> ");
		writer.println("  <script type='text/javascript' src='/scripts/dbxcentral.utils.js'></script> ");
        writer.println();
		writer.println("  <!--  ");
		writer.println("    ======================================================================= ");
		writer.println("    == CSS imports - STYLES SHEETS GOES HERE ");
		writer.println("    ======================================================================= ");
		writer.println("  --> ");
		writer.println("  <!-- CSS: DbxCentral --> ");
		writer.println("  <link rel='stylesheet' href='/scripts/css/dbxcentral.css'> ");
        writer.println();
		writer.println("  <!-- CSS: Bootstrap --> ");
		writer.println("  <link rel='stylesheet' href='/scripts/bootstrap/css/bootstrap.min.css'> ");
        writer.println();
		writer.println("  <!-- CSS: Font Awsome --> ");
		writer.println("  <link rel='stylesheet' href='/scripts/font-awesome/4.4.0/css/font-awesome.min.css'> ");
		writer.println();
		
		return out.toString();
	}

	public static String getOverviewNavbar()
	{
		return getHtmlNavbar(PageSection.Server, "", true);
	}
	public static String getHtmlNavbar(PageSection pageSection, String rightTopHtml, boolean addIsLoggedIn)
	{
		String mainActive    = "";
		String mainExtraSpan = "";

		String overviewActive    = "";
		String overviewExtraSpan = "";
		
		String adminActive    = "";
		String adminExtraSpan = "";
		
		String desktopAppActive    = "";
		String desktopAppExtraSpan = "";

		if (PageSection.None.equals(pageSection))
		{
			// No Menu item is "active"
			mainActive    = "";
			mainExtraSpan = "";
		}

		if (PageSection.Server.equals(pageSection))
		{
			overviewActive    = " active";
			overviewExtraSpan = "<span class='sr-only'>(current)</span>";
		}

		if (PageSection.Admin.equals(pageSection))
		{
			adminActive    = " active";
			adminExtraSpan = "<span class='sr-only'>(current)</span>";
		}

		if (PageSection.DesktopApp.equals(pageSection))
		{
			desktopAppActive    = " active";
			desktopAppExtraSpan = "<span class='sr-only'>(current)</span>";
		}

		boolean addRightHandSide = addIsLoggedIn || StringUtil.hasValue(rightTopHtml);

		StringWriter out    = new StringWriter();
		PrintWriter  writer = new PrintWriter(out);

		writer.println();
		writer.println("  <nav class='navbar navbar-expand-md navbar-dark sticky-top bg-dark mb-0'> ");
		writer.println("    <a class='navbar-brand' href='/'>DbxCentral</a> ");
		writer.println("    <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarCollapse' aria-controls='navbarCollapse' aria-expanded='false' aria-label='Toggle navigation'> ");
		writer.println("        <span class='navbar-toggler-icon'></span> ");
		writer.println("    </button> ");
		writer.println("    <div class='collapse navbar-collapse' id='navbarCollapse'> ");
		writer.println("      <ul class='navbar-nav mr-auto'> ");
		writer.println("        <li class='nav-item" + overviewActive + "'> ");
		writer.println("          <a class='nav-link' href='/overview'>Servers" + overviewExtraSpan + "</a> ");
		writer.println("        </li> ");
		writer.println("        <li class='nav-item" + adminActive + "'> ");
		writer.println("          <a class='nav-link' href='/admin/admin.html'>Admin" + adminExtraSpan + "</a> ");
		writer.println("        </li> ");
		writer.println("        <li class='nav-item" + desktopAppActive + "'> ");
		writer.println("          <a class='nav-link' href='/desktop_app.html'>Desktop App" + desktopAppExtraSpan + "</a> ");
		writer.println("        </li> ");
		writer.println("      </ul> ");
		if (addRightHandSide)
		{
			writer.println("	  <!-- on the right hand side of the menu... --> ");
			writer.println("      <ul class='navbar-nav'> ");

			if (StringUtil.hasValue(rightTopHtml))
			{
				writer.println(rightTopHtml);
			}

			if (addIsLoggedIn)
			{
				writer.println("        <!-- IS LOGGED IN --> ");
				writer.println("        <div id='dbx-nb-isLoggedIn-div' style='display: none;'> ");
				writer.println("          <li class='nav-item dropdown'> ");
				writer.println("            <a class='nav-link dropdown-toggle' href='http://example.com' id='navbarDropdownMenuLink' data-toggle='dropdown' aria-haspopup='true' aria-expanded='false'> ");
				writer.println("              <i class='fa fa-user'></i> <span id='dbx-nb-isLoggedInUser-div'></span> <!-- current username will be in here --> ");
				writer.println("            </a> ");
				writer.println("            <div class='dropdown-menu dropdown-menu-right' aria-labelledby='navbarDropdownMenuLink'> ");
				writer.println("            <a class='dropdown-item' href='#'> <i class='fa fa-cog'></i> Settings [not-yet-implemented]</a> ");
				writer.println("            <a class='dropdown-item' href='/logout'> <i class='fa fa-sign-out'></i> Logout</a> ");
				writer.println("            </div> ");
				writer.println("          </li> ");
				writer.println("        </div> ");
				writer.println("        <!-- IS LOGGED OUT --> ");
				writer.println("        <div id='dbx-nb-isLoggedOut-div'> ");
				writer.println("          <a class='nav-link' href='/index.html?login'> <i class='fa fa-sign-in'></i> <span data-toggle='tooltip' title='Log in as a specific user.'>Login</span></a> ");
				writer.println("        </div> ");
			}
			writer.println("      </ul> <!-- end right hand side --> ");
		}
		writer.println("    </div> ");
		writer.println("  </nav> ");

		return out.toString();
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
