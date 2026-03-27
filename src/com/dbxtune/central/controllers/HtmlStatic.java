/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.central.controllers;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.dbxtune.utils.StringUtil;

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
		writer.println("  <script type='text/javascript' src='/scripts/jquery/jquery-3.7.1.min.js'></script> ");
		writer.println();
		writer.println("  <!-- JS: Moment; used by: ChartJs, DateRangePicker --> ");
		writer.println("  <script type='text/javascript' src='/scripts/moment/moment.js'></script> ");
		writer.println("  <script type='text/javascript' src='/scripts/moment/moment-duration-format.js'></script> ");
		writer.println();
		writer.println("  <!-- JS: Bootstrap --> ");
//		writer.println("  <script type='text/javascript' src='/scripts/popper/1.12.9/popper.min.js'></script> ");
//		writer.println("  <script type='text/javascript' src='/scripts/bootstrap/4.0.0/js/bootstrap.min.js'></script> ");
		writer.println("  <script type='text/javascript' src='/scripts/bootstrap/4.6.2/js/bootstrap.bundle.min.js'></script>"); // Bundle also includes popper
		writer.println();
		writer.println("  <!-- JS: DbxCentral --> ");
		writer.println("  <script type='text/javascript' src='/scripts/dbxtune/js/dbxcentral.utils.js'></script> ");
		writer.println("  <script type='text/javascript' src='/scripts/dbxtune/js/dbxLoginModal.js'></script> ");
		writer.println();
		writer.println("  <!--  ");
		writer.println("    ======================================================================= ");
		writer.println("    == CSS imports - STYLES SHEETS GOES HERE ");
		writer.println("    ======================================================================= ");
		writer.println("  --> ");
		writer.println("  <!-- CSS: DbxCentral --> ");
		writer.println("  <link rel='stylesheet' href='/scripts/dbxtune/css/dbxcentral.css'> ");
		writer.println();
		writer.println("  <!-- CSS: Bootstrap --> ");
//		writer.println("  <link rel='stylesheet' href='/scripts/bootstrap/4.0.0/css/bootstrap.min.css'> ");
		writer.println("  <link rel='stylesheet' href='/scripts/bootstrap/4.6.2/css/bootstrap.min.css'> ");
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
				writer.println("            <a class='dropdown-item' href='#' onclick='dbxOpenSettings(); return false;'> <i class='fa fa-cog'></i> Settings</a> ");
				writer.println("            <a class='dropdown-item' href='/logout'> <i class='fa fa-sign-out'></i> Logout</a> ");
				writer.println("            </div> ");
				writer.println("          </li> ");
				writer.println("        </div> ");
				writer.println("        <!-- IS LOGGED OUT --> ");
				writer.println("        <div id='dbx-nb-isLoggedOut-div'> ");
				writer.println("          <a class='nav-link' href='#'> <i class='fa fa-sign-in'></i> <span data-toggle='tooltip' title='Log in as a specific user.'>Login</span></a> ");
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
		sb.append("\n");
		sb.append("	//----------------------------------------------------------- \n");
		sb.append("	// Settings dialog \n");
		sb.append("	//----------------------------------------------------------- \n");
		sb.append("	function dbxOpenSettings() \n");
		sb.append("	{ \n");
		sb.append("		$('#dbx-settings-email-msg').html(''); \n");
		sb.append("		$('#dbx-settings-pw-msg').html(''); \n");
		sb.append("		$('#dbx-settings-current-pw, #dbx-settings-new-pw, #dbx-settings-confirm-pw').val(''); \n");
		sb.append("		$.ajax({ \n");
		sb.append("			url: '/api/user/settings?op=profile', \n");
		sb.append("			method: 'GET', \n");
		sb.append("			dataType: 'json', \n");
		sb.append("			success: function(r) { \n");
		sb.append("				$('#dbx-settings-username').val(r.username || ''); \n");
		sb.append("				$('#dbx-settings-email').val(r.email || ''); \n");
		sb.append("			} \n");
		sb.append("		}); \n");
		sb.append("		$('#dbx-settings-dialog').modal('show'); \n");
		sb.append("	} \n");
		sb.append("\n");
		sb.append("	function dbxSaveEmail() \n");
		sb.append("	{ \n");
		sb.append("		var email = $('#dbx-settings-email').val().trim(); \n");
		sb.append("		if (!email || !email.includes('@')) { $('#dbx-settings-email-msg').html('<span class=\"text-danger\">Please enter a valid email address.</span>'); return; } \n");
		sb.append("		$('#dbx-settings-email-msg').html('<span class=\"text-muted\">Saving...</span>'); \n");
		sb.append("		$.ajax({ \n");
		sb.append("			url: '/api/user/settings', method: 'POST', \n");
		sb.append("			data: { op: 'changeEmail', email: email }, dataType: 'json', \n");
		sb.append("			success: function(r) { var cls = r.success ? 'text-success' : 'text-danger'; $('#dbx-settings-email-msg').html('<span class=\"'+cls+'\">'+r.message+'</span>'); }, \n");
		sb.append("			error: function() { $('#dbx-settings-email-msg').html('<span class=\"text-danger\">Request failed.</span>'); } \n");
		sb.append("		}); \n");
		sb.append("	} \n");
		sb.append("\n");
		sb.append("	function dbxSavePassword() \n");
		sb.append("	{ \n");
		sb.append("		var cur = $('#dbx-settings-current-pw').val(); \n");
		sb.append("		var np  = $('#dbx-settings-new-pw').val(); \n");
		sb.append("		var cp  = $('#dbx-settings-confirm-pw').val(); \n");
		sb.append("		if (!cur)          { $('#dbx-settings-pw-msg').html('<span class=\"text-danger\">Enter your current password.</span>'); return; } \n");
		sb.append("		if (np.length < 6) { $('#dbx-settings-pw-msg').html('<span class=\"text-danger\">New password must be at least 6 characters.</span>'); return; } \n");
		sb.append("		if (np !== cp)     { $('#dbx-settings-pw-msg').html('<span class=\"text-danger\">Passwords do not match.</span>'); return; } \n");
		sb.append("		$('#dbx-settings-pw-msg').html('<span class=\"text-muted\">Saving...</span>'); \n");
		sb.append("		$.ajax({ \n");
		sb.append("			url: '/api/user/settings', method: 'POST', \n");
		sb.append("			data: { op: 'changePassword', currentPassword: cur, newPassword: np, confirmPassword: cp }, dataType: 'json', \n");
		sb.append("			success: function(r) { \n");
		sb.append("				var cls = r.success ? 'text-success' : 'text-danger'; \n");
		sb.append("				$('#dbx-settings-pw-msg').html('<span class=\"'+cls+'\">'+r.message+'</span>'); \n");
		sb.append("				if (r.success) $('#dbx-settings-current-pw, #dbx-settings-new-pw, #dbx-settings-confirm-pw').val(''); \n");
		sb.append("			}, \n");
		sb.append("			error: function() { $('#dbx-settings-pw-msg').html('<span class=\"text-danger\">Request failed.</span>'); } \n");
		sb.append("		}); \n");
		sb.append("	} \n");

		if (addScriptTag)
			sb.append("</script>");

		// Settings modal HTML (Bootstrap 4) -- placed outside the <script> block
		sb.append("\n");
		sb.append("<!-- ============================================================ -->\n");
		sb.append("<!-- Settings Modal (Bootstrap 4)                                 -->\n");
		sb.append("<!-- ============================================================ -->\n");
		sb.append("<div class='modal fade' id='dbx-settings-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-settings-title'>\n");
		sb.append("    <div class='modal-dialog' role='document'>\n");
		sb.append("        <div class='modal-content'>\n");
		sb.append("            <div class='modal-header'>\n");
		sb.append("                <h5 class='modal-title' id='dbx-settings-title'><i class='fa fa-cog'></i> Account Settings</h5>\n");
		sb.append("                <button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>\n");
		sb.append("            </div>\n");
		sb.append("            <div class='modal-body'>\n");
		sb.append("                <div class='form-group'>\n");
		sb.append("                    <label class='small font-weight-bold'>Username</label>\n");
		sb.append("                    <input type='text' class='form-control form-control-sm' id='dbx-settings-username' readonly>\n");
		sb.append("                </div>\n");
		sb.append("                <hr>\n");
		sb.append("                <h6>Change Email</h6>\n");
		sb.append("                <div class='form-group mt-2'>\n");
		sb.append("                    <input type='email' class='form-control form-control-sm' id='dbx-settings-email' placeholder='Email address'>\n");
		sb.append("                </div>\n");
		sb.append("                <button class='btn btn-sm btn-primary' type='button' onclick='dbxSaveEmail()'>Update Email</button>\n");
		sb.append("                <div id='dbx-settings-email-msg' class='mt-1 small'></div>\n");
		sb.append("                <hr>\n");
		sb.append("                <h6>Change Password</h6>\n");
		sb.append("                <div class='form-group mt-2'>\n");
		sb.append("                    <input type='password' class='form-control form-control-sm' id='dbx-settings-current-pw' placeholder='Current password' autocomplete='current-password'>\n");
		sb.append("                </div>\n");
		sb.append("                <div class='form-group'>\n");
		sb.append("                    <input type='password' class='form-control form-control-sm' id='dbx-settings-new-pw' placeholder='New password (min 6 characters)' autocomplete='new-password'>\n");
		sb.append("                </div>\n");
		sb.append("                <div class='form-group'>\n");
		sb.append("                    <input type='password' class='form-control form-control-sm' id='dbx-settings-confirm-pw' placeholder='Confirm new password' autocomplete='new-password'>\n");
		sb.append("                </div>\n");
		sb.append("                <button class='btn btn-sm btn-primary' type='button' onclick='dbxSavePassword()'>Change Password</button>\n");
		sb.append("                <div id='dbx-settings-pw-msg' class='mt-1 small'></div>\n");
		sb.append("            </div>\n");
		sb.append("            <div class='modal-footer'>\n");
		sb.append("                <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>\n");
		sb.append("            </div>\n");
		sb.append("        </div>\n");
		sb.append("    </div>\n");
		sb.append("</div>\n");

		return sb.toString();
	}
}
