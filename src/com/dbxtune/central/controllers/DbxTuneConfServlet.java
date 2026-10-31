/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class DbxTuneConfServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final String CONF_DIR  = DbxTuneCentral.getAppConfDir();

	/** Show the real secret values to a logged in admin (everybody else always sees them masked) */
	public static final String  PROPKEY_showSecretsToAdmin = "DbxTuneCentral.conf.showSecretsToAdmin";
	public static final boolean DEFAULT_showSecretsToAdmin = true;

	/** What a secret is replaced with */
	public static final String SECRET_MASK = "********";

	// The last part of a property key (after the last '.') that holds a secret. Only the END of it: 'maxTokens' and
	// 'PasswordExpiresInDays' are not secrets.
	private static final Pattern SECRET_KEY_END     = Pattern.compile("(?i).*(password|passwd|pwd|secret|apikey|api_key|apitoken|token|bearer|basic|privatekey|accesskey|authorization|extraheaders)");
	private static final Pattern PASSWORD_KEY_END   = Pattern.compile("(?i).*(password|passwd|pwd)");
	// A property line, also a commented out one ('#key=value'): leading space, comment, space, key, separator, value
	private static final Pattern PROPERTY_LINE      = Pattern.compile("^(\\s*)(#+|!)?(\\s*)([^=:\\s#!]+)(\\s*[=:]\\s*)(.*)$");
	// Secrets inside a value (or any other line), for any key: a URL's password ('https://user:pass@host'),
	// URL/JDBC parameters ('?password=x', ';password=x', '&sig=x'), the secret path of webhook URLs
	// (Slack, Teams, Discord), and a DbxTune command line password ('-P secret', '--password secret')
	private static final Pattern URL_PASSWORD       = Pattern.compile("(://[^/:@\\s]+:)([^@/\\s]+)(@)");
	private static final Pattern SECRET_PARAM       = Pattern.compile("(?i)([?&;](?:password|passwd|pwd|token|access_token|apikey|api_key|key|secret|client_secret|sig|signature|code)=)([^&;\\s]+)");
	private static final Pattern WEBHOOK_PATH       = Pattern.compile("(?i)(https?://(?:hooks\\.slack\\.com|[\\w.-]*webhook\\.office\\.com|discord(?:app)?\\.com/api/webhooks))/[^\\s?\"']+");
	private static final Pattern CMDLINE_PASSWORD   = Pattern.compile("(\\s-P\\s*|\\s--password[=\\s]\\s*)(\\S+)");

	PrintWriter out = null;
	HttpServletResponse _resp = null;

	/** The real secret values are shown (an admin, and PROPKEY_showSecretsToAdmin is true) */
	private boolean _showSecrets = false;
	/** Secrets may be shown to an admin (PROPKEY_showSecretsToAdmin) */
	private boolean _showSecretsToAdmin = DEFAULT_showSecretsToAdmin;
	/** Lines in the file that hold a secret */
	private int _secretLineCount = 0;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// One object PER REQUEST: this class keeps the response/writer in instance fields (out, _resp), and the
		// servlet instance itself is shared by all concurrent requests (they would write into each other's responses).
		new DbxTuneConfServlet().handleRequest(req, resp);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		_resp = resp;
		out = resp.getWriter();

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "name", "type", "method", "discard"))
			return;

		String inputName     = Helper.getParameter(req, "name");
		String inputType     = Helper.getParameter(req, "type");
		String inputMethod   = Helper.getParameter(req, "method", "plain");
		String discardRegExp = Helper.getParameter(req, "discard");

//		System.out.println("DbxTuneConfServlet: name   = '"+inputName+"'.");
//		System.out.println("DbxTuneConfServlet: type   = '"+inputType+"'.");
//		System.out.println("DbxTuneConfServlet: method = '"+inputMethod+"'.");

		// Check for mandatory parameters
		if (StringUtil.isNullOrBlank(inputName))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find mandatory parameter 'name'.");
			return;
		}

		// CHECK: that the input file really is INSIDE the CONF_DIR and not outside (for example /etc/passwd or "sub/../../x")
		if (Helper.getFileInsideDir(CONF_DIR, inputName) == null)
		{
			_logger.warn("Rejected access to file '" + inputName + "', it is not located in the CONF dir '" + CONF_DIR + "'. From '" + req.getRemoteAddr() + "'.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file must be located in the CONF dir.");
			return;
		}
	
		// Check if file EXISTS
		File inputFile = new File(CONF_DIR + "/" + inputName);
		if ( ! inputFile.exists() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file '" + inputFile + "' doesn't exist.");
			return;
		}


		_showSecretsToAdmin = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showSecretsToAdmin, DEFAULT_showSecretsToAdmin);
		boolean isAdmin     = req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) || "admin".equals(req.getRemoteUser());
		_showSecrets        = isAdmin && _showSecretsToAdmin;

		if ("plain".equalsIgnoreCase(inputMethod))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asCode(inputName, inputType, discardRegExp);
		}
		else
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asHtmlTable(inputName, inputType, discardRegExp);
		}
	}

	private void asCode(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		File f = new File(CONF_DIR + "/" + inputName);

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<title>" + inputName + "</title>");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("<link href='/scripts/prism/prism-1.30.0.css' rel='stylesheet' />");
		out.println("<script src='/scripts/prism/prism-1.30.0.js'></script>");
		out.println("<style type='text/css'>");
		out.println("body { height: 100%; }");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println(HtmlStatic.getUserDefinedContentNavbar());
		out.println("<div class='container-fluid mt-3'>");
		out.println("<h5>" + StringEscapeUtils.escapeHtml4(f.getAbsolutePath()) + "</h5>");

		// Compile the regexp; if we got any
		Pattern pattern = null;
		if (StringUtil.hasValue(discardRegExp))
		{
    		try { pattern = Pattern.compile(discardRegExp); }
    		catch (PatternSyntaxException ex) 
    		{ 
    			out.println("<p>"); 
    			out.println("Error in 'discard' RegExp pattern. Caught: " + ex + "<br>"); 
    			out.println("So discarding will NOT be done<br>"); 
    			out.println("</p>"); 
    		}
		}
		
		
		List<String> lines = readConfLines(f, pattern);
		printSecretsNote(f);

		out.println("<div id='log'>");
////		out.println("<textarea id='logtext' style='width:100%;height:100vw; overflow: hidden;'>");
//		out.println("<textarea id='logtext'>");
		out.println("<pre>");
		out.println("<code class='language-properties line-numbers'>");

		{
			for (String line : lines)
			{
//				String prefix  = "";
//				String postfix = "";
//				if      (line.indexOf(" - INFO  - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - WARN  - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - AFFECTED ") >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - ERROR - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - ERROR   " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - TRACE - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - DEBUG - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - FATAL - " ) >= 0) { prefix = "<span style='background-color:rgb(255,  51,  51);'>"; postfix = "</span>"; }
//
//				out.print(prefix);
//				out.println(line);
//				out.print(postfix);

				line = StringUtil.toHtmlStringExceptNl(line);
				out.println(line);
			}
		}

		out.println("</code>");
		out.println("</pre>");
//		out.println("</textarea>");
		out.println("</div>");
		

		// FIXME: move codemirror to local resource
//		out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.34.0/codemirror.css'> ");
//		out.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.34.0/codemirror.js'></script> ");
//		out.println("<script> ");
////		out.println("  var editor = CodeMirror.fromTextArea('logtext', { "); // or document.getElementById("logtext")
//		out.println("  var logTextArea = document.getElementById('logtext');" );
//		out.println("  var editor = CodeMirror.fromTextArea(logTextArea, { ");
//		out.println("    mode: 'text/html', ");
//		out.println("    lineNumbers: true ");
//		out.println("  }); ");
//		out.println("</script> ");

		out.println("</div>"); // end container-fluid
		out.println(HtmlStatic.getUserDefinedContentJavaScriptAtEnd());
		out.println("</body>");
		out.println("</html>");

		out.flush();
		out.close();
	}

	private void asHtmlTable(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<title>" + inputName + "</title>");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("<!-- Tablesorter theme -->");
		out.println("<link rel='stylesheet' href='/scripts/tablesorter/2.32.0/css/theme.metro-dark.min.css'>");
		out.println("<link rel='stylesheet' href='/scripts/dbxtune/css/dbxcentral_tablesorter.css'>");
		out.println("<!-- Tablesorter scripts -->");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/2.32.0/js/jquery.tablesorter.js'></script>");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/2.32.0/js/jquery.tablesorter.widgets.js'></script>");
		out.println("</head>");

		out.println("<body>");
		out.println(HtmlStatic.getUserDefinedContentNavbar());
		out.println("<div class='container-fluid mt-3'>");
		out.println("<h5>" + StringEscapeUtils.escapeHtml4(inputName) + "</h5>");
		out.println();
//		out.println("<input type='text' id='filterInput' width='800' onkeyup='filterFunction()' placeholder='Filter...' title='Filter'>");
//		out.println("<br>");
//		out.println("<br>");
		
		// Compile the regexp; if we got any
		Pattern pattern = null;
		if (StringUtil.hasValue(discardRegExp))
		{
    		try { pattern = Pattern.compile(discardRegExp); }
    		catch (PatternSyntaxException ex) 
    		{ 
    			out.println("<p>"); 
    			out.println("Error in 'discard' RegExp pattern. Caught: " + ex + "<br>"); 
    			out.println("So discarding will NOT be done<br>"); 
    			out.println("</p>"); 
    		}
		}
		
		
		File f = new File(CONF_DIR + "/" + inputName);
		List<String> lines = readConfLines(f, pattern);
		printSecretsNote(f);

		out.println();
		out.println("<table id='dbxTable' class='alarmTableClass tablesorter'>");
		out.println("<thead> ");
		out.println("  <tr> ");
		out.println("    <th>Key</th>");
		out.println("    <th>Value</th>");
		out.println("  </tr>");
		out.println("</thead> ");

		out.println("<tbody>");
		for (String line : lines)
		{
			// Split at the FIRST '=': the value may have '=' in it (URLs, JDBC parameters)
			int eq = line.indexOf('=');
			if (eq < 0)
				continue;

			String key = line.substring(0, eq).trim();
			String val = line.substring(eq + 1).trim();

			out.println("<tr>");
			out.println("  <td nowrap>" + StringEscapeUtils.escapeHtml4(key) + "</td>");
			out.println("  <td nowrap>" + StringEscapeUtils.escapeHtml4(val) + "</td>");
			out.println("</tr>");
		}

//		out.println("<script>                                                                  ");
//		out.println("var tf = new TableFilter(document.querySelector('.alarmTableClass'), {    ");
//		out.println("    base_path: '/scripts/tablefilter/'                                    ");
//		out.println("});                                                                       ");
//		out.println("tf.init();                                                                ");
//		out.println("</script>                                                                 ");
		
		out.println("<script>                                                                  ");
		out.println("$(function() {                                                            ");
		out.println("    // call the tablesorter plugin                                        ");
		out.println("    $('#dbxTable').tablesorter({                                        ");
		out.println("         theme: 'metro-dark'                                              ");
//		out.println("        ,dateFormat : 'yyyymmdd'                                          ");
		out.println("        ,sortList: [[0,1]]                                                ");
		out.println("        ,showProcessing : true                                            ");
		out.println("        ,sortReset      : true                                            ");
		out.println("        ,widthFixed     : true                                            ");
		out.println("        ,headerTemplate : '{content} {icon}'                              ");
		out.println("        ,widgets        : [ 'uitheme', 'zebra', 'filter', 'resizable', 'stickyHeaders' ] ");
		out.println("        ,widgetOptions  : {                                               ");
		out.println("        	 storage_useSessionStorage : true                              ");
		out.println("        	,resizable_addLastColumn   : true                              ");
		out.println("        	,filter_placeholder        : { search : 'Search...' }          ");
		out.println("        	,filter_columnFilters      : true                              ");
		out.println("        }                                                                 ");
		out.println("        ,headers  : {                                                     ");
		out.println("           0: { dateFormat: 'yyyymmdd' }                                  ");
		out.println("        }                                                                 ");
		out.println("        ,debug          : false                                           ");
//		out.println("        ,debug          : true                                            ");
		out.println("    });                                                                   ");
		out.println("});                                                                       ");
		out.println("</script>                                                                 ");
		
		out.println("</tbody>");
		out.println("</table>");
		out.println("</div>"); // end container-fluid
		out.println(HtmlStatic.getUserDefinedContentJavaScriptAtEnd());
		out.println("</body></html>");


		out.flush();
		out.close();
	}

	/**
	 * Read the file: each line with its secrets masked (unless _showSecrets), lines matching the 'discard' regexp left out.
	 * Counts the lines that hold a secret in _secretLineCount.
	 */
	private List<String> readConfLines(File f, Pattern discard)
	throws ServletException
	{
		List<String> lines = new ArrayList<>();
		try ( FileInputStream in = new FileInputStream(f);
		      BufferedReader  br = new BufferedReader(new InputStreamReader(in)); )
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				String masked = maskSecrets(line);
				if ( ! masked.equals(line) )
					_secretLineCount++;

				// Discard on the MASKED line: a regexp tested on the real line would tell, one guess at a time, what a secret contains
				if (discard != null && discard.matcher(masked).find())
					continue;

				lines.add(_showSecrets ? line : masked);
			}
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '" + f + "'.", ex);
		}
		return lines;
	}

	/** A note above the content when the file has secrets: they are masked (and where the real values are), or shown to an admin */
	private void printSecretsNote(File f)
	{
		if (_secretLineCount == 0)
			return;

		String path = StringEscapeUtils.escapeHtml4(f.getAbsolutePath());
		if (_showSecrets)
		{
			out.println("<div class='alert alert-warning py-2'><b>Secrets are shown</b>, because you are logged in as an admin. "
					+ "Other users see the passwords, keys and tokens in this file masked. "
					+ "To mask them for admins too, set <code>" + PROPKEY_showSecretsToAdmin + " = false</code> in DBX_CENTRAL.conf.</div>");
		}
		else
		{
			out.println("<div class='alert alert-info py-2'><b>Secrets are masked.</b> "
					+ "Passwords, API keys, tokens and other secrets in this file are shown as <code>" + SECRET_MASK + "</code>. "
					+ "Long keys and tokens keep their last 4 characters, so you can tell which one is in use.<br>"
					+ "The real values can only be seen in the file itself, on the DbxCentral server (at OS level): <code>" + path + "</code>"
					+ (_showSecretsToAdmin ? "<br>Admins see the real values here when they are logged in." : "")
					+ "</div>");
		}
	}

	/**
	 * Is this property key a secret (password, API key, token, client secret, ...)? Only the end of the key's last
	 * part is checked: 'mail.smtp.password' and 'DbxCentral.llm.foundry.clientSecret' are, 'maxTokens' is not.
	 */
	public static boolean isSecretPropertyKey(String key)
	{
		if (key == null)
			return false;
		return SECRET_KEY_END.matcher(key.substring(key.lastIndexOf('.') + 1)).matches();
	}

	/**
	 * A line of a config file with its secrets masked: the value of a secret property (also when commented out), and
	 * secrets inside any value or line (URL passwords, URL/JDBC parameters, webhook URLs, command line passwords).
	 */
	public static String maskSecrets(String line)
	{
		if (line == null)
			return null;

		Matcher m = PROPERTY_LINE.matcher(line);
		// A commented out property is written 'key=value' - a comment like '# Note: ...' is not a property
		if (m.matches() && (m.group(2) == null || m.group(5).trim().equals("=")) && isSecretPropertyKey(m.group(4)))
			return m.group(1) + (m.group(2) == null ? "" : m.group(2)) + m.group(3) + m.group(4) + m.group(5) + maskSecretValue(m.group(4), m.group(6));

		String masked = URL_PASSWORD    .matcher(line  ).replaceAll("$1" + SECRET_MASK + "$3");
		masked        = SECRET_PARAM    .matcher(masked).replaceAll("$1" + SECRET_MASK);
		masked        = WEBHOOK_PATH    .matcher(masked).replaceAll("$1/" + SECRET_MASK);
		masked        = CMDLINE_PASSWORD.matcher(masked).replaceAll("$1" + SECRET_MASK);
		return masked;
	}

	/**
	 * The masked value of a secret property. Kept as is: an empty value that is not a password (not set), an environment variable reference
	 * ('${MY_KEY}') and a placeholder ('<key>'). 'encrypted:' stays visible. Long API keys/tokens (20+ chars) keep their
	 * last 4 characters, like the LLM providers' consoles do, so you can tell which key is in use - never for passwords.
	 */
	private static String maskSecretValue(String key, String value)
	{
		String  val        = value.trim();
		boolean isPassword = PASSWORD_KEY_END.matcher(key.substring(key.lastIndexOf('.') + 1)).matches();

		// An empty password is masked too: that a database or mail account has NO password is itself a secret.
		// An empty API key/token/secret is kept: it only says that it is not configured.
		if (val.isEmpty())
			return isPassword ? SECRET_MASK : value;

		if (val.startsWith("${") || val.matches("<[^>]*>"))
			return value;

		// In a properties file the ':' is often escaped: 'encrypted\:...'
		if (val.replace("\\:", ":").startsWith(Configuration.ENCRYPTED_PREFIX))
			return Configuration.ENCRYPTED_PREFIX + SECRET_MASK;

		String  tail       = ( ! isPassword && val.length() >= 20 ) ? val.substring(val.length() - 4) : "";
		return SECRET_MASK + tail;
	}
}
