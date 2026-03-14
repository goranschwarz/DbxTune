/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers.ud.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.sql.CommonEedInfo;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.SqlServerConnection;
import com.dbxtune.sql.conn.TdsConnection;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.AseSqlScriptReader;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.OpenSslAesUtil;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.microsoft.sqlserver.jdbc.ISQLServerMessage;
import com.microsoft.sqlserver.jdbc.ISQLServerMessageHandler;
import com.microsoft.sqlserver.jdbc.SQLServerError;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

public class UserDefinedActionSql
extends UserDefinedActionAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	public static final String PROPKEY_dbms_sql         = "dbms.sql";
	public static final String PROPKEY_dbms_username    = "dbms.username";
	public static final String PROPKEY_dbms_password    = "dbms.password";
	public static final String PROPKEY_dbms_servername  = "dbms.servername";
	public static final String PROPKEY_dbms_dbname      = "dbms.dbname";
	public static final String PROPKEY_dbms_url         = "dbms.url";

	private enum MsgType
	{
		INFO,
		SUCCESS,
		WARNING,
		ERROR
	};

	private String _connectedToProductName;

	
//	private String       _dbms_sql       ;
	private String       _dbms_username  ;
	private String       _dbms_password  ;
	private String       _dbms_servername;
	private String       _dbms_dbname    ;
	private String       _dbms_url       ;

//	public String       getDbmsSql()        { return _dbms_sql; }
	public String       getDbmsUsername()   { return _dbms_username; }
	public String       getDbmsPassword()   { return _dbms_password; }
	public String       getDbmsServerName() { return _dbms_servername; }
	public String       getDbmsDbname()     { return _dbms_dbname; }
	public String       getDbmsUrl()        { return _dbms_url; }

//	public void setDbmsSql           (String dbms_sql       ) { _dbms_sql        = dbms_sql; }
	public void setDbmsUsername      (String dbms_username  ) { _dbms_username   = dbms_username; }
	public void setDbmsPassword      (String dbms_password  ) { _dbms_password   = dbms_password; }
	public void setDbmsServername    (String dbms_servername) { _dbms_servername = dbms_servername; }
	public void setDbmsDbname        (String dbms_dbname    ) { _dbms_dbname     = dbms_dbname; }
	public void setDbmsUrl           (String dbms_url       ) { _dbms_url        = dbms_url; }


	
	public UserDefinedActionSql(Configuration conf)
	throws Exception
	{
		super(conf);
	}

	@Override
	public List<String> getJavaScriptList()
	{
		List<String> list = new ArrayList<>();
		
		list.add("/scripts/bootstrap-table/1.12.1/bootstrap-table.js");
		list.add("/scripts/bootstrap-table/1.12.1/extensions/filter-control/bootstrap-table-filter-control.js");

		return list;
	}

	@Override
	public List<String> getCssList()
	{
		List<String> list = new ArrayList<>();
		
		list.add("/scripts/bootstrap-table/1.12.1/bootstrap-table.min.css");

		return list;
	}
	
	@Override
	public String[] getKnownParameters()
	{
		return new String[] {};
	}

	@Override
	public Map<String, String> getParameterDescription()
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		
		map.put("refresh",              "Auto refresh the page after this amount of seconds.<br>"
		                                    + "<br>"
		                                    + "Example: <code>60</code> (every 60 seconds) <br>"
		                                    + "Example: <code>0 </code> (turn OFF auto refresh, just press F5 instead) <br>"
		                                    + "<b>Default</b>: <code>" +  getPageRefreshTime() + "</code>");

		return map;
	}

	@Override
	public void checkUrlParameters(Map<String, String> parameterMap) throws Exception
	{
	}



	@Override
	public void init(Configuration conf)
	throws Exception
	{
		super.init(conf);
		
		// Do local initializations here
//		_dbms_sql        = conf.getMandatoryProperty(PROPKEY_dbms_sql       );
		_dbms_url        = conf.getMandatoryProperty(PROPKEY_dbms_url       );
		_dbms_username   = conf.getMandatoryProperty(PROPKEY_dbms_username  );
		_dbms_password   = conf.getProperty         (PROPKEY_dbms_password  , null);
		_dbms_servername = conf.getProperty         (PROPKEY_dbms_servername, null);
		_dbms_dbname     = conf.getProperty         (PROPKEY_dbms_dbname    , null);

		// -----------------------------------------------
		// BEGIN: Check for mandatory fields
		// -----------------------------------------------
		// already done with: getMandatoryProperty("prop.key")
		

		if (_dbms_servername != null && _dbms_servername.equalsIgnoreCase("FROM_FILENAME"))
		{
			_dbms_servername = getFromFileName(1, conf.getFilename());  // 1 == indexPos, logicalPos is 2 
		}

		if (_dbms_dbname != null && _dbms_dbname.equalsIgnoreCase("FROM_FILENAME"))
		{
			_dbms_dbname = getFromFileName(2, conf.getFilename());  // 2 == indexPos, logicalPos is 3
		}

		// Get password (if PROPKEY_dbms_servername is specified)
		if (_dbms_password == null)
		{
			// Note: generate a passwd in linux: echo 'thePasswd' | openssl enc -aes-128-cbc -a -salt -pass:sybase
			_dbms_password = OpenSslAesUtil.readPasswdFromFile(_dbms_username, _dbms_servername);
			if (_dbms_password == null)
			{
				String msg = "Failed getting password for user '" + _dbms_username + "' and server '" + _dbms_servername + "' from '" + OpenSslAesUtil.getPasswordFilename() + "'. Check if exists in the file.";
				_logger.error(msg);
				setDescription(msg);
				setValid(false);
				throw new Exception(msg);
			}
		}
		
		// Replace '${dbname}' from the DBMS URL
		if (_dbms_url.contains("${dbname}"))
		{
			_dbms_url = _dbms_url.replace("${dbname}", _dbms_dbname);
		}

		// Replace '${srvName}' from the DBMS URL
		if (_dbms_url.contains("${srvName}"))
		{
			_dbms_url = _dbms_url.replace("${srvName}", _dbms_servername);
		}

		// Replace '${servername}' from the DBMS URL
		if (_dbms_url.contains("${servername}"))
		{
			_dbms_url = _dbms_url.replace("${servername}", _dbms_servername);
		}

		// Replace '${ifile-hostname}', '${ifile-port}' from the DBMS URL
		if (_dbms_url.contains("${ifile-hostname}") || _dbms_url.contains("${ifile-port}"))
		{
			String iFileHostname = AseConnectionFactory.getIFirstHost(_dbms_servername);
			int    iFilePort     = AseConnectionFactory.getIFirstPort(_dbms_servername);
			
			if (iFileHostname == null)
			{
				String msg = "Cant find the server name '" + _dbms_servername + "' in the Sybase Name Server File '" + AseConnectionFactory.getIFileName() + "'. Can't continue.";
				_logger.error(msg);
				setDescription(msg);
				setValid(false);
				throw new Exception(msg);
			}
			else
			{
				if (_dbms_url.contains("${ifile-hostname}")) _dbms_url = _dbms_url.replace("${ifile-hostname}", iFileHostname);
				if (_dbms_url.contains("${ifile-port}"    )) _dbms_url = _dbms_url.replace("${ifile-port}",     iFilePort + "");
			}
		}

	}

	@Override
	public String getOnServerName()
	{
		return getDbmsServerName();
	}

	@Override
	public void createContent(PrintWriter pageOut, PrintWriter mailOut)
	throws Exception
	{
		createStyle(pageOut, mailOut);
		createModal(pageOut);
		createJsFunctions(pageOut);

		doWork(pageOut, mailOut);
	}

	private void createStyle(PrintWriter pageOut, PrintWriter mailOut)
	throws IOException
	{
		println(pageOut, mailOut, "<style>");

		println(pageOut, mailOut, "  #dbx-uda-output-container { ");
		println(pageOut, mailOut, "    display: flex; ");
		println(pageOut, mailOut, "    flex-direction: column; ");
		println(pageOut, mailOut, "    min-height: 100vh; ");
		println(pageOut, mailOut, "  } ");
		
		println(pageOut, mailOut, "  #dbx-uda-scroll-spacer { /* Forces enough room so the last div can sit at the top */ ");
		println(pageOut, mailOut, "    height: 100vh; ");
		println(pageOut, mailOut, "    order: 9999; /* Keeps it at the very bottom */ ");
		println(pageOut, mailOut, "  ");
		println(pageOut, mailOut, "  } ");

		println(pageOut, mailOut, "  .dbx-uda-sql-statement { background-color: #f8f9fa; padding: 15px; margin: 15px 0; border-left: 4px solid #007bff; font-family: 'Courier New', monospace; white-space: pre-wrap; }");
		println(pageOut, mailOut, "  .dbx-uda-modal-dialog { max-width: 700px; }");
		println(pageOut, mailOut, "  .dbx-uda-status-text { font-size: 1.1rem; color: #495057; }");
		println(pageOut, mailOut, "  .dbx-uda-query-display { background-color: #e9ecef; padding: 10px; border-radius: 4px; margin: 10px 0; font-family: monospace; font-size: 0.9rem; white-space: pre-wrap; }");
		println(pageOut, mailOut, "  .dbx-uda-timer-display { font-size: 0.95rem; color: #6c757d; margin: 5px 0; }");
		println(pageOut, mailOut, "  table td { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px; padding: 4px 8px !important; font-size: 0.85rem; }");
		println(pageOut, mailOut, "  table th { padding: 6px 8px !important; font-size: 0.9rem; }");
		println(pageOut, mailOut, "  .table-responsive { overflow-x: auto; }");
//		println(pageOut, mailOut, "  .table-responsive { }");
		println(pageOut, mailOut, "  .dbx-uda-timer-table { width: auto; margin: 10px 0; }");
		println(pageOut, mailOut, "  .dbx-uda-timer-table td:first-child { padding: 3px 10px !important; font-size: 0.9rem; white-space: nowrap; width: 200px; }");
		println(pageOut, mailOut, "  .dbx-uda-timer-table td:last-child { padding: 3px 10px !important; font-size: 0.9rem; white-space: nowrap; text-align: right; font-family: monospace; }");

		// since mail can't read bootstraps CSS from "the net"... inline them here
		println(pageOut, mailOut, "  .alert         { padding: 0.75rem 1.25rem; margin-bottom: 1rem; border: 1px solid transparent; border-radius: 0.25rem; font-family: sans-serif; } ");
		println(pageOut, mailOut, "  .alert-success { color: #0f5132; background-color: #d1e7dd; border-color: #badbcc; } ");
		println(pageOut, mailOut, "  .alert-info    { color: #055160; background-color: #cff4fc; border-color: #b6effb; } ");
		println(pageOut, mailOut, "  .alert-warning { color: #664d03; background-color: #fff3cd; border-color: #ffecb5; } ");
		println(pageOut, mailOut, "  .alert-danger  { color: #842029; background-color: #f8d7da; border-color: #f5c2c7; } ");

		println(pageOut, mailOut, "  hr { border-top: 10px solid #bbb; }");
//		println(pageOut, mailOut, "  hr.dashed  { border-top: 10px dashed #bbb; }");
//		println(pageOut, mailOut, "  hr.rounded { border-top: 10px solid #bbb; border-radius: 5px; }");

		println(pageOut, mailOut, "  .dbx-udc-plain-message {");
		println(pageOut, mailOut, "      background-color: #f8f9fa; ");
		println(pageOut, mailOut, "      padding-left: 5px; ");
		println(pageOut, mailOut, "      padding-top: 1px;  ");
		println(pageOut, mailOut, "      padding-bottom: 1px; ");
		println(pageOut, mailOut, "      margin: 2px 0; ");
		println(pageOut, mailOut, "      border-left: 4px solid #bbb; ");
		println(pageOut, mailOut, "      font-family: 'Courier New', monospace; ");
		println(pageOut, mailOut, "      white-space: pre-wrap; ");
		println(pageOut, mailOut, "  }");
		println(pageOut, mailOut, "  .dbx-udc-plain-error-message {");
		println(pageOut, mailOut, "      background-color: #f8f9fa; ");
		println(pageOut, mailOut, "      padding-left: 5px; ");
		println(pageOut, mailOut, "      padding-top: 1px;  ");
		println(pageOut, mailOut, "      padding-bottom: 1px; ");
		println(pageOut, mailOut, "      margin: 2px 0; ");
		println(pageOut, mailOut, "      border-left: 4px solid #bbb; ");
		println(pageOut, mailOut, "      font-family: 'Courier New', monospace; ");
		println(pageOut, mailOut, "      white-space: pre-wrap; ");
		println(pageOut, mailOut, "      color: red; ");
		println(pageOut, mailOut, "  }");

//		println(pageOut, mailOut, "code {  ");
//		println(pageOut, mailOut, "	  font-family: monospace;     "); // Standard monospaced font
//		println(pageOut, mailOut, "	  background-color: #f0f0f0;  "); // Light gray background
//		println(pageOut, mailOut, "	  color: #c7254e;             "); // Optional: Slight color change for contrast
//		println(pageOut, mailOut, "	  padding: 2px 4px;           "); // Space around the text
//		println(pageOut, mailOut, "	  border-radius: 4px;         "); // Slightly rounded corners
//		println(pageOut, mailOut, "	  font-size: 90%;             "); // Often sized slightly smaller than body text
//		println(pageOut, mailOut, "} ");	

		// Only styles <code> if it is inside an element with class "markdown-style"
		// Example: <div class='markdown-style'> <p>In this section, <code>word</code> mimics Markdown.</p> </div>
		println(pageOut, mailOut, ".markdown-style code { ");
//		println(pageOut, mailOut, "    font-family: var(--bs-font-monospace);  "); // Bootstrap mono font variable
		println(pageOut, mailOut, "    font-family: monospace;                 "); // Standard monospaced font
		println(pageOut, mailOut, "    background-color: #f5f5f5;              "); // light gray
		println(pageOut, mailOut, "    color: #3c444d !important;              "); // Override Bootstrap's pink/red
		println(pageOut, mailOut, "    padding: 0.2rem 0.4rem;                 "); 
		println(pageOut, mailOut, "    border-radius: 6px;                     "); 
		println(pageOut, mailOut, "    font-size: 85%;                         "); 
		println(pageOut, mailOut, "} ");

		// Target the inner div Bootstrap Table creates
		println(pageOut, mailOut, ".bootstrap-table .fixed-table-container thead th .th-inner { ");
		println(pageOut, mailOut, "    padding: 8px 8px !important; "); // Match your row padding height
		println(pageOut, mailOut, "    font-size: 0.85rem; ");           // Slightly smaller than your td (0.85rem)
		println(pageOut, mailOut, "    line-height: 1.2; ");
		println(pageOut, mailOut, "} ");

		// Force the header row height to shrink
		println(pageOut, mailOut, ".bootstrap-table thead th { ");
		println(pageOut, mailOut, "    vertical-align: bottom !important; ");
		println(pageOut, mailOut, "    padding: 0 !important; ");      // Let .th-inner handle the padding
		println(pageOut, mailOut, "} ");

		println(pageOut, mailOut, "</style>");

		// For Mail: Outlook Classic, we might want to change the CSS a bit (since MSO is rendering using Word)
		if (mailOut != null)
		{
			mailOut.println("<!--[if mso]>");
			mailOut.println("<style>");

//			// Outlook is handling padding bad in div's... So lets increase line-height and use mso-border
//			mailOut.println(".cmd-command { padding: 0 !important; mso-border-left-alt: 4pt solid #007bff; background-color: #f8f9fa; }");
//			mailOut.println(".cmd-output { background-color: #1e1e1e; color: #d4d4d4; font-family: 'Courier New', Courier, monospace; }");
//
//			// Outlook does not support border-radius, so remove that to escape strange effects
//			mailOut.println(".cmd-info, .cmd-exit-success, .cmd-exit-error { border-radius: 0px !important; }");
//
//			// Fix to force monospaced fonts in Outlook
//			mailOut.println(".cmd-stdout, .cmd-stderr { font-family: 'Courier New', Courier, monospace !important; }");

			mailOut.println(".alert-success { " +
					"background-color: #d1e7dd !important; " +
					"color: #0f5132 !important; " +
					"padding: 8pt 12pt !important; " +
					"margin: 4pt 0 !important; " +
					"}");
			mailOut.println(".alert-info { " +
					"background-color: #cff4fc !important; " +
					"color: #055160 !important; " +
					"padding: 8pt 12pt !important; " +
					"margin: 4pt 0 !important; " +
					"}");
			mailOut.println(".alert-warning { " +
					"background-color: #fff3cd !important; " +
					"color: #664d03 !important; " +
					"padding: 8pt 12pt !important; " +
					"margin: 4pt 0 !important; " +
					"}");
			mailOut.println(".alert-danger { " +
					"background-color: #f8d7da !important; " +
					"color: #842029 !important; " +
					"padding: 8pt 12pt !important; " +
					"margin: 4pt 0 !important; " +
					"}");
			
			mailOut.println(".dbx-uda-sql-statement { " +
					"font-size: 9pt !important; " +
					"mso-para-margin: 0 !important; " +
					"mso-line-height-rule: exactly; " +
					"line-height: 13pt !important; " +
					"}");
			mailOut.println(".dbx-udc-plain-message, .dbx-udc-plain-error-message { " +
					"font-size: 9pt !important; " +
					"font-family: 'Courier New', Courier, monospace !important; " +
					"mso-para-margin: 0 !important; " +
					"mso-line-height-rule: exactly; " +
					"line-height: 13pt !important; " +
					"}");
			mailOut.println(".dbx-udc-plain-error-message { color: red !important; }");

			mailOut.println("</style>");
			mailOut.println("<![endif]-->");


			//---------------------------------------------------
			// And for Everything that goes to mail
			// <table> is not defined at all so it needs to go to ALL mails
			//---------------------------------------------------
			mailOut.println("<style>");

			mailOut.println("table { " +
					"border-collapse: collapse !important; " +
					"border: 1pt solid #dee2e6 !important; " +
					"}");
			mailOut.println("table th { " +
					"background-color: #343a40 !important; " +
					"color: #ffffff !important; " +
					"font-size: 9pt !important; " +
					"padding: 4pt 8pt !important; " +
					"border: 1pt solid #454d55 !important; " +
					"}");
			mailOut.println("table td { " +
					"font-size: 9pt !important; " +
					"padding: 4pt 8pt !important; " +
					"border: 1pt solid #dee2e6 !important; " +
					"}");			

			mailOut.println("</style>");
		}
		
	}
	private void createModal(PrintWriter pageOut)
	throws IOException
	{
		pageOut.println();
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<!-- Bootstrap MODAL - While we are executing SQL Statement(s) -->");
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<div class='modal' id='dbx-uda-executionModal' data-backdrop='static' data-keyboard='false' tabindex='-1' role='dialog'>");
		pageOut.println("  <div class='modal-dialog modal-dialog-centered dbx-uda-modal-dialog'>");
		pageOut.println("    <div class='modal-content'>");
//		pageOut.println("      <div class='modal-header bg-primary text-white'>");
		pageOut.println("      <div class='modal-header bg-secondary text-white'>");
//		pageOut.println("      <div class='modal-header bg-dark text-white'>");
//		pageOut.println("      <div class='modal-header bg-success text-white'>");
//		pageOut.println("      <div class='modal-header'>");
		pageOut.println("        <h5 class='modal-title'>SQL Execution in Progress</h5>");
		pageOut.println("      </div>");
		pageOut.println("      <div class='modal-body'>");
		pageOut.println("        <div class='text-center mb-3'>");
		pageOut.println("          <div class='spinner-border text-primary' role='status'>");
		pageOut.println("            <span class='sr-only'>Loading...</span>");
		pageOut.println("          </div>");
		pageOut.println("        </div>");
		pageOut.println("        <div class='mb-3'>");
		pageOut.println("          <strong id='dbx-uda-currentStatementLabel'>Current Statement:</strong>");
		pageOut.println("          <div class='dbx-uda-query-display' id='dbx-uda-currentQuery'>Initializing...</div>");
		pageOut.println("        </div>");
		pageOut.println("        <table class='table table-sm table-bordered dbx-uda-timer-table'>");
		pageOut.println("          <tbody>");
		pageOut.println("            <tr>");
		pageOut.println("              <td><strong>Statement Execution Time:</strong></td>");
		pageOut.println("              <td id='dbx-uda-statement-timer'>0 ms</td>");
		pageOut.println("            </tr>");
		pageOut.println("            <tr>");
		pageOut.println("              <td><strong>Total Execution Time:</strong></td>");
		pageOut.println("              <td id='dbx-uda-total-timer'>0 ms</td>");
		pageOut.println("            </tr>");
		pageOut.println("          </tbody>");
		pageOut.println("        </table>");
		pageOut.println("        <div>");
		pageOut.println("          <strong>Status:</strong>");
		pageOut.println("          <p class='dbx-uda-status-text' id='dbx-uda-statusText'>Starting execution...</p>");
		pageOut.println("        </div>");
		pageOut.println("        <div id='dbx-uda-feedback' style='display: none;'>");
		pageOut.println("          <strong>Messages:</strong>");
		pageOut.println("          <div class='dbx-uda-feedback-text-class' id='dbx-uda-feedback-txt'></div>");
		pageOut.println("        </div>");
		pageOut.println("      </div>");
		pageOut.println("    </div>");
		pageOut.println("  </div>");
		pageOut.println("</div>");		
		pageOut.println();
	}
	private void createJsFunctions(PrintWriter pageOut)
	throws IOException
	{
		// JavaScript functions
		pageOut.println();
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<!-- Local JavaScript functions -->");
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<script>");
		pageOut.println("var totalStartTime = null;");
		pageOut.println("var statementStartTime = null;");
		pageOut.println("var timerInterval = null;");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function startTimers()");
		pageOut.println("{");
		pageOut.println("    totalStartTime = new Date();");
		pageOut.println("    statementStartTime = new Date();");
		pageOut.println("    if (timerInterval) clearInterval(timerInterval);");
		pageOut.println("    timerInterval = setInterval(updateTimers, 100);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function resetStatementTimer()");
		pageOut.println("{");
		pageOut.println("    statementStartTime = new Date();");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function updateTimers()");
		pageOut.println("{");
		pageOut.println("    if (totalStartTime)");
		pageOut.println("    {");
		pageOut.println("        var totalElapsedMs = (new Date() - totalStartTime);");
		pageOut.println("        document.getElementById('dbx-uda-total-timer').innerText = formatTime(totalElapsedMs);");
		pageOut.println("    }");
		pageOut.println("    if (statementStartTime)");
		pageOut.println("    {");
		pageOut.println("        var stmtElapsedMs = (new Date() - statementStartTime);");
		pageOut.println("        document.getElementById('dbx-uda-statement-timer').innerText = formatTime(stmtElapsedMs);");
		pageOut.println("    }");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function formatTime(durationInMs)");
		pageOut.println("{");
		pageOut.println("    const ms = durationInMs % 1000; ");
		pageOut.println("    const s  = Math.floor((durationInMs / 1000) % 60); ");
		pageOut.println("    const m  = Math.floor((durationInMs / 60000) % 60); ");
		pageOut.println("    const h  = Math.floor( durationInMs / 3600000); ");
        pageOut.println();
		pageOut.println("    let result = ''; ");
        pageOut.println();
		pageOut.println("    if (h  > 0) result += h  + 'h '; ");
		pageOut.println("    if (m  > 0) result += m  + 'm '; ");
		pageOut.println("    if (s  > 0) result += s  + 's '; ");
		pageOut.println("    if (ms > 0) result += ms + ' ms'; ");
        pageOut.println();
		pageOut.println("    return result.trim() || '0 ms'; ");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function stopTimers()");
		pageOut.println("{");
		pageOut.println("    if (timerInterval)");
		pageOut.println("    {");
		pageOut.println("        clearInterval(timerInterval);");
		pageOut.println("        timerInterval = null;");
		pageOut.println("    }");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function updateStatus(status)");
		pageOut.println("{");
		pageOut.println("    document.getElementById('dbx-uda-statusText').innerText = status;");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function updateQuery(queryNum, sql)");
		pageOut.println("{");
		pageOut.println("    document.getElementById('dbx-uda-currentStatementLabel').innerText = 'Current Statement ' + queryNum + ':';");
		pageOut.println("    document.getElementById('dbx-uda-currentQuery').innerText = sql;");
		pageOut.println("    resetStatementTimer();");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function getStatementTime()");
		pageOut.println("{");
		pageOut.println("    if (statementStartTime)");
		pageOut.println("    {");
		pageOut.println("        var seconds = (new Date() - statementStartTime) / 1000;");
		pageOut.println("        return formatTime(seconds);");
		pageOut.println("    }");
		pageOut.println("    return '0 ms';");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addText(msg)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'dbx-udc-text';");
		pageOut.println("    div.innerHTML = msg;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
//		out.println("    div.scrollIntoView(true);");
		pageOut.println("}");
		pageOut.println();
		
		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addPlainMessage(msg)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'dbx-udc-plain-message';");
		pageOut.println("    div.innerHTML = msg;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
//		out.println("    div.scrollIntoView(true);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addPlainErrorMessage(msg)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'dbx-udc-plain-error-message';");
		pageOut.println("    div.innerHTML = msg;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
//		out.println("    div.scrollIntoView(true);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addMessage(msg, type)");
		pageOut.println("{");
		pageOut.println("    var alertClass = 'alert-info';");
		pageOut.println("    if      (type === '" + MsgType.SUCCESS + "') alertClass = 'alert-success';");
		pageOut.println("    else if (type === '" + MsgType.ERROR   + "') alertClass = 'alert-danger';");
		pageOut.println("    else if (type === '" + MsgType.WARNING + "') alertClass = 'alert-warning';");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'alert ' + alertClass;");
		pageOut.println("    div.innerHTML = '<strong>' + msg + '</strong>';");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
//		out.println("    div.scrollIntoView(true);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addMessageWithTime(msg, type)");
		pageOut.println("{");
		pageOut.println("    var time = getStatementTime();");
		pageOut.println("    addMessage(msg + ' (' + time + ')', type);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addTable(html)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'table-responsive';");
		pageOut.println("    div.innerHTML = html;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
		pageOut.println("    var tables = div.querySelectorAll('table[data-toggle=\"table\"]');");
		pageOut.println("    tables.forEach(function(table)");
		pageOut.println("    {");
		pageOut.println("        $(table).bootstrapTable();");
		pageOut.println("    });");
//		out.println("    wrapper.scrollIntoView(true);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addSQL(sql, queryNum)");
		pageOut.println("{");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(document.createElement('br'));");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(document.createElement('hr'));");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(document.createElement('br'), document.getElementById('dbx-uda-scroll-spacer'));");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(document.createElement('hr'), document.getElementById('dbx-uda-scroll-spacer'));");
		pageOut.println();
		pageOut.println("    var h3 = document.createElement('h3');");
		pageOut.println("    h3.id = 'dbx-uda-statement-' + queryNum;");
		pageOut.println("    h3.innerText = 'Statement ' + queryNum;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(h3);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(h3, document.getElementById('dbx-uda-scroll-spacer'));");
		pageOut.println();
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'dbx-uda-sql-statement';");
		pageOut.println("    div.innerText = sql;");
//		out.println("    document.getElementById('dbx-uda-output-container').appendChild(div);");
		pageOut.println("    document.getElementById('dbx-uda-output-container').insertBefore(div, document.getElementById('dbx-uda-scroll-spacer'));");
		pageOut.println();
		pageOut.println("    <!-- Clear the message field -->");
		pageOut.println("    var modalFeedback    = document.getElementById('dbx-uda-feedback');");
		pageOut.println("    var modalFeedbackTxt = document.getElementById('dbx-uda-feedback-txt');");
		pageOut.println("    modalFeedback.style.display = 'none';");
		pageOut.println("    modalFeedbackTxt.innerText = '';");
		pageOut.println();
		pageOut.println("    h3.scrollIntoView({     ");
		pageOut.println("        behavior: 'smooth', "); // Optional: animates the scroll
		pageOut.println("        block:    'start',  "); // Aligns the top of the div to the top of the page
		pageOut.println("        inline:   'nearest' ");
		pageOut.println("    }); ");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function addModalMessage(msg)");
		pageOut.println("{");
		pageOut.println("    var modalFeedback = document.getElementById('dbx-uda-feedback');");
		pageOut.println("    modalFeedback.style.display = 'block';");
		pageOut.println();
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'dbx-udc-plain-message';");
		pageOut.println("    div.innerText = msg;");
		pageOut.println("    document.getElementById('dbx-uda-feedback-txt').appendChild(div);");
		pageOut.println("    div.scrollIntoView();");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function closeModal()");
		pageOut.println("{");
		pageOut.println("    stopTimers();");
		pageOut.println("    var modal = document.getElementById('dbx-uda-executionModal');");
		pageOut.println("    modal.style.display = 'none';");
		pageOut.println("    modal.classList.remove('show');");
		pageOut.println("    document.body.classList.remove('modal-open');");
		pageOut.println("    var backdrop = document.querySelector('.modal-backdrop');");
		pageOut.println("    if (backdrop) backdrop.remove();");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------");
		pageOut.println("function showModal()");
		pageOut.println("{");
		pageOut.println("    startTimers();");
		pageOut.println("    var modal = document.getElementById('dbx-uda-executionModal');");
		pageOut.println("    modal.style.display = 'block';");
		pageOut.println("    modal.classList.add('show');");
		pageOut.println("    document.body.classList.add('modal-open');");
		pageOut.println("    var backdrop = document.createElement('div');");
		pageOut.println("    backdrop.className = 'modal-backdrop show';");
		pageOut.println("    document.body.appendChild(backdrop);");
		pageOut.println("}");
		pageOut.println("</script>");
		pageOut.flush();
	}
	
	//----------------------------------------------------------------------
	// Below is Java code to call above JavaScript functions
	//----------------------------------------------------------------------
	private void sendStopTimers(PrintWriter pageOut, PrintWriter mailOut)
	{
		pageOut.println("<script> stopTimers(); </script>");
		pageOut.flush();
	}
	
	private void sendUpdateModalStatus(PrintWriter pageOut, PrintWriter mailOut, String status)
	{
		pageOut.println("<script> updateStatus('" + escapeJavaScript(status) + "'); </script>");
		pageOut.flush();
	}
	
	private void sendModalMessage(PrintWriter pageOut, PrintWriter mailOut, String status)
	{
		pageOut.println("<script> addModalMessage('" + escapeJavaScript(status) + "'); </script>");
		pageOut.flush();
	}
	
	private void sendUpdateCurrentQuery(PrintWriter pageOut, PrintWriter mailOut, String sql, int queryNum)
	{
		pageOut.println("<script> updateQuery(" + queryNum + ", '" + escapeJavaScript(sql) + "'); </script>");
		pageOut.flush();
	}

	private void sendAddSql(PrintWriter pageOut, PrintWriter mailOut, String sql, int queryNum)
	{
		pageOut.println("<script> addSQL('" + escapeJavaScript(sql) + "', " + queryNum + "); </script>");
		pageOut.flush();

		if (mailOut != null)
		{
			mailOut.println("<br>");
			mailOut.println("<hr>");
			mailOut.println("<h3 id='dbx-uda-statement-" + queryNum + "'>Statement " + queryNum + "</h3>");
			
//			String divClass = "dbx-uda-sql-statement";
//			mailOut.println("<div class='" + divClass + "'>" + escapeHtml(sql) + "</div>");
			
			// Modern clients
			mailOut.println("<!--[if !mso]><!-->");
			mailOut.println("<div class='dbx-uda-sql-statement'>" + escapeHtml(sql) + "</div>");
			mailOut.println("<!--<![endif]-->");

			// Outlook Classic - pre handles line breaks without paragraph gaps
			mailOut.println("<!--[if mso]>");
			mailOut.println("<pre class='dbx-uda-sql-statement'>" + escapeHtml(sql) + "</pre>");
			mailOut.println("<br>");
			mailOut.println("<![endif]-->");
	}
	}

	private void sendAddText(PrintWriter pageOut, PrintWriter mailOut, String message)
	{
		pageOut.println("<script> addText('" + escapeJavaScript(message) + "'); </script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
//			String divClass = "dbx-udc-text";
//			mailOut.println("<div class='" + divClass + "'>" + message + "</div>");
			mailOut.println(message); // already full HTML, no wrapper needed
		}
	}

	private void sendAddPlainMessage(PrintWriter pageOut, PrintWriter mailOut, String message)
	{
		pageOut.println("<script> addPlainMessage('" + escapeJavaScript(message) + "'); </script>");
		pageOut.flush();
		
//		if (mailOut != null)
//		{
//			String divClass = "dbx-udc-plain-message";
//			mailOut.println("<div class='" + divClass + "'>" + escapeHtml(message) + "</div>");
//		}
		if (mailOut != null)
		{
			// Modern clients
			mailOut.println("<!--[if !mso]><!-->");
			mailOut.println("<div class='dbx-udc-plain-message'>" + message + "</div>");
			mailOut.println("<!--<![endif]-->");

			// Outlook Classic
			mailOut.println("<!--[if mso]>");
			mailOut.println("<pre class='dbx-udc-plain-message'>" + message + "</pre>");
			mailOut.println("<![endif]-->");
		}
	}

	private void sendAddPlainErrorMessage(PrintWriter pageOut, PrintWriter mailOut, String message)
	{
		pageOut.println("<script> addPlainErrorMessage('" + escapeJavaScript(message) + "'); </script>");
		pageOut.flush();
		
//		if (mailOut != null)
//		{
//			String divClass = "dbx-udc-plain-error-message";
//			mailOut.println("<div class='" + divClass + "'>" + escapeHtml(message) + "</div>");
//		}
		if (mailOut != null)
		{
			// Modern clients
			mailOut.println("<!--[if !mso]><!-->");
			mailOut.println("<div class='dbx-udc-plain-error-message'>" + message + "</div>");
			mailOut.println("<!--<![endif]-->");

			// Outlook Classic
			mailOut.println("<!--[if mso]>");
			mailOut.println("<pre class='dbx-udc-plain-error-message'>" + message + "</pre>");
			mailOut.println("<![endif]-->");
		}
	}
	
	private void sendAddMessage(PrintWriter pageOut, PrintWriter mailOut, MsgType type, String message)
	{
		pageOut.println("<script> addMessage('" + escapeJavaScript(message) + "', '" + type + "'); </script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = "alert-info";
			if (MsgType.SUCCESS.equals(type)) divClass = "alert-success";
			if (MsgType.ERROR  .equals(type)) divClass = "alert-danger";
			if (MsgType.WARNING.equals(type)) divClass = "alert-warning";

			mailOut.println("<div class='" + divClass + "'><strong>" + message + "</strong></div>");
		}
	}
	
	private void sendAddMessageWithTime(PrintWriter pageOut, PrintWriter mailOut, MsgType type, String message, long batchStartTime)
	{
		pageOut.println("<script> addMessageWithTime('" + escapeJavaScript(message) + "', '" + type + "'); </script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = "alert-info";
			if (MsgType.SUCCESS.equals(type)) divClass = "alert-success";
			if (MsgType.ERROR  .equals(type)) divClass = "alert-danger";
			if (MsgType.WARNING.equals(type)) divClass = "alert-warning";

			String execTime = TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", System.currentTimeMillis() - batchStartTime);
			
			mailOut.println("<div class='" + divClass + "'><strong>" + escapeHtml(message) + " (" + execTime + ")</strong></div>"); ;
		}
	}
	
	private void sendFinnished(PrintWriter pageOut, PrintWriter mailOut)
	{
		pageOut.println("<script> window.scrollTo({ top: 0, behavior: 'smooth' }); </script>");
		pageOut.println("<script> document.getElementById('dbx-uda-scroll-spacer').remove();</script> "); // If we want to remove the bottom "spacer"
		pageOut.flush();
		
		if (mailOut != null)
		{
			// do nothing
		}
	}

	private int sendAddTable(PrintWriter pageOut, PrintWriter mailOut, ResultSet rs) throws SQLException
	{
		//TODO: Possibly use ResultSetTableModel.toXxx(...) so we can produce both HTML Table (aligned left/right and binary data columns) and ASCII Tables
//		ResultSetTableModel rstm = new ResultSetTableModel(rs, "query");
//		rstm.toHtmlTableString("htmlClassName"); // with a TableStringRenderer
//		rstm.toAsciiTableString();

		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();
		
		StringBuilder tableHtml = new StringBuilder();
		tableHtml.append("<table data-toggle='table' data-search='true' data-search-align='left' data-pagination='true' data-page-size='25' class='table w-auto table-striped'> \n");
		
		// Header row
		tableHtml.append("<thead><tr> \n");
		for (int col = 1; col <= columnCount; col++)
		{
			tableHtml.append("<th data-sortable='true'>").append(escapeHtml(metaData.getColumnName(col))).append("</th> \n");
		}
		tableHtml.append("</tr></thead> \n");
		
		// Data rows
		tableHtml.append("<tbody> \n");
		int rowCount = 0;
		while (rs.next())
		{
			tableHtml.append("<tr> \n");
			for (int col = 1; col <= columnCount; col++)
			{
				String value = rs.getString(col);
				tableHtml.append("<td>").append(value != null ? escapeHtml(value) : "<em class='text-muted'>NULL</em>").append("</td> \n");
			}
			tableHtml.append("</tr> \n");
			rowCount++;
		}
		tableHtml.append("</tbody> \n");
		tableHtml.append("</table> \n");
		
		String htmlTable = tableHtml.toString();
		pageOut.println("<script> addTable('" + escapeJavaScript(htmlTable) + "'); </script>");
		pageOut.flush();

		// And print to mail
		if (mailOut != null)
		{
			mailOut.println("<div style='overflow-x:auto; -webkit-overflow-scrolling:touch;'>");
			mailOut.println("<div style='min-width:max-content;'>");
			mailOut.println(htmlTable);
			mailOut.println("</div>");
			mailOut.println("</div>");
		}
		
		sendAddMessage(pageOut, mailOut, MsgType.SUCCESS, "ResultSet processed: " + rowCount + " row(s) returned");
		
		return rowCount;
	}

	
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------

	/**
	 * Here are the MAIN part
	 * @param pageOut 
	 */
	private void doWork(PrintWriter pageOut, PrintWriter mailOut) 
	throws Exception
	{
		// Show the modal NOW before starting any work
		pageOut.println("<script>showModal();</script>");
		pageOut.flush();

		// This is the OUT 'divs' we will add output to.
//		out.println("<div id='dbx-uda-output'></div>"); // We will ADD stuff in here via JavaScript
		// 'dbx-uda-output' is already written by class: UserDefinedActionServlet

		pageOut.println("<div id='dbx-uda-output-container'>"); // We will ADD stuff in here via JavaScript
		pageOut.println("  <div id='dbx-uda-scroll-spacer'></div>");
		pageOut.println("</div>");
		
		// Small delay to ensure modal is visible
		try { Thread.sleep(500);} catch (InterruptedException ignore) { /* Ignore */}

		DbxConnection conn = null;
		Statement stmt = null;
		
		long allStartTime = System.currentTimeMillis();
		
		try
		{
			String jdbcUrl  = getDbmsUrl();
			String jdbcUser = getDbmsUsername();

//			String goSql = getDbmsSql();
			String goSql = getCommand();

			// Connect to database
			sendUpdateModalStatus(pageOut, mailOut, "Connecting to DBMS server...");
//			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			conn = dbmsConnect();
			_connectedToProductName = conn.getDatabaseProductName(); 
			String dbmsProductVersion = conn.getDatabaseProductVersion();
			sendUpdateModalStatus(pageOut, mailOut, "Connected to DBMS server");
//			addMessage(pageOut, mailOut, "<div class='markdown-style'>Connected as user <code>" + jdbcUser + "</code> to DBMS URL: <code>" + jdbcUrl + "</code></div>", Status.SUCCESS);
//			addMessage(pageOut, mailOut, "<div class='markdown-style'>DBMS ServerName <code>" + conn.getDbmsServerName() + "</code>, Product Name <code>" + _connectedToProductName + "</code>, Version <code>" + dbmsProductVersion + "</code></div>", Status.SUCCESS);

			// Connect info: to PAGE
			sendAddText(pageOut, null, "<div class='alert alert-success markdown-style'><strong>Connected as user</strong> <code>" + jdbcUser + "</code> <strong>to DBMS URL</strong> <code>" + jdbcUrl + "</code></div>");
			sendAddText(pageOut, null, "<div class='alert alert-success markdown-style'><strong>DBMS ServerName</strong> <code>" + conn.getDbmsServerName() + "</code><strong>, Product Name</strong> <code>" + _connectedToProductName + "</code><strong>, Version</strong> <code>" + dbmsProductVersion + "</code></div>");

			// Connect info: to MAIL  (not so fancy output, due to rendering problems on Outlook Classic)
			if (mailOut != null)
			{
				mailOut.println("<div class='alert-success'><b>Connected to DBMS using:</b>");
				mailOut.println("<ul>");
				mailOut.println("  <li><b>User</b>: " + jdbcUser + "</li>");
				mailOut.println("  <li><b>URL</b>:  " + jdbcUrl + "</li>");
				mailOut.println("</ul>");
				mailOut.println("</div>");

				mailOut.println("<div class='alert-success'>");
				mailOut.println("<ul>");
				mailOut.println("  <li><b>ServerName</b>: "    + conn.getDbmsServerName() + "</li>");
				mailOut.println("  <li><b>Product Name</b>:  " + _connectedToProductName  + "</li>");
				mailOut.println("  <li><b>Version</b>:  "      + dbmsProductVersion       + "</li>");
				mailOut.println("</ul>");
				mailOut.println("</div>");
			}
			
			// Add Sybase and SQL Server -- Message handler...
			installConnectionMessageHandler(conn, pageOut, mailOut);
			
			if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			{
				String sql = "set flushmessage on";
				try 
				{
					Statement stmnt = conn.createStatement();
					stmnt.executeUpdate(sql);
					stmnt.close();
				}
				catch(SQLException ex)
				{
					_logger.warn("Problems executing '" + sql + "', continuing ayway.");
				}
			}
			
			stmt = conn.createStatement();

			// Get SQL Batch Terminator
//			String sqlBatchTerminator = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sqlBatchTerminator, DEFAULT_sqlBatchTerminator);
			String sqlBatchTerminator = "go";
			
			// treat each 'go' rows as a individual execution
			// readCommand(), does the job
			AseSqlScriptReader sr = new AseSqlScriptReader(goSql, true, sqlBatchTerminator);
//			if (_useSemicolonHack_chk.isSelected())
//				sr.setSemiColonHack(true);

			// loop all batches
			int batchId = 0;
			for (String sql = sr.getSqlBatchString(); sql != null; sql = sr.getSqlBatchString())
			{
				batchId++;
				
				// This can't be part of the for loop, then it just stops if empty row
				if ( StringUtil.isNullOrBlank(sql) )
					continue;

				// Remove leading/trailing spaces around the command
				sql = sql.trim();
						
				// Add SQL To the OUTPUT
				sendUpdateCurrentQuery(pageOut, mailOut, sql, batchId);
				sendAddSql(pageOut, mailOut, sql, batchId);
				pageOut.flush();
				
				sendUpdateModalStatus(pageOut, mailOut, "Sending SQL statement to DBMS...");

				
				// Foreach "go" (SQL Batch), do it in a TRY, so we can continue with next batch on exceptions
				try
				{
					long batchStartTime = System.currentTimeMillis();

					Statement stmnt  = conn.createStatement();
					ResultSet  rs    = null;
					int rowsAffected = 0;
					int totalRowsAffected = 0;

//					if (_queryTimeout > 0)
//						stmnt.setQueryTimeout(_queryTimeout);

//					sql = sqlChunc;
//					_currentSqlStatement = sql;
//					if (_logger.isDebugEnabled()) 
//					{
//						_logger.debug("EXECUTING: " + sql);
//						sb.append("\n");
//						sb.append("--#################### BEGIN: EXECUTING ################################# \n");
//						sb.append(sql);
//						sb.append("--#################### END: EXECUTING ################################### \n");
//					}

					boolean hasRs = stmnt.execute(sql);
					int resultSetCount = 0;

					// iterate through each result set
					do
					{
						// Append, messages and Warnings to output, if any
//						sb.append(getSqlWarningMsgs(stmnt, true));
						// Check for SQL warnings (like PRINT statements)
						checkAndDisplayWarnings(pageOut, mailOut, stmt);

						if(hasRs)
						{
							resultSetCount++;

							// Get next result set to work with
							rs = stmnt.getResultSet();

							// Append, messages and Warnings to output, if any
//							sb.append(getSqlWarningMsgs(stmnt, true));
							checkAndDisplayWarnings(pageOut, mailOut, stmt);

							
//							// Convert the ResultSet into a TableModel, which fits on a JTable
//							ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql, sql);
//
//							// Write ResultSet Content as a "string table"
//							if      (_rsAsAsciiTable) sb.append(tm.toAsciiTableString());
//							else if (_rsAsJson)       sb.append(tm.toJson());
//							else                      sb.append(tm.toTableString());
							sendUpdateModalStatus(pageOut, mailOut, "ResultSet " + resultSetCount + " received");
//							addMessageWithTime(out, "ResultSet " + resultSetCount + " received", Status.SUCCESS);
							
							sendUpdateModalStatus(pageOut, mailOut, "Processing ResultSet " + resultSetCount + "...");
							totalRowsAffected += sendAddTable(pageOut, mailOut, rs);
							rs.close();

							// Append, messages and Warnings to output, if any
//							sb.append(getSqlWarningMsgs(stmnt, true));
							checkAndDisplayWarnings(pageOut, mailOut, stmt);

							// Close it
							rs.close();
						}
						else
						{
	    					// Treat update/row count(s)
	    					rowsAffected = stmnt.getUpdateCount();
	    					if (rowsAffected >= 0)
	    					{
	    						totalRowsAffected += rowsAffected;
	    					//	sb.append("("+rowsAffected+" row affected)\n");
								sendUpdateModalStatus(pageOut, mailOut, "Update count received");
								sendAddMessageWithTime(pageOut, mailOut, MsgType.WARNING, "Rows affected: " + rowsAffected, batchStartTime);
	    					}
						}

						// Check if we have more result sets
						hasRs = stmnt.getMoreResults(); 

						_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
					}
					while (hasRs || rowsAffected != -1);

					// Append, messages and Warnings to output, if any
//					sb.append(getSqlWarningMsgs(stmnt, true));
					checkAndDisplayWarnings(pageOut, mailOut, stmt);

					// Close the statement
					stmnt.close();
					
//					String batchExecTime = TimeUtils.msDiffNowToTimeStr(batchStartTime);
					String batchExecTime = TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", System.currentTimeMillis() - batchStartTime);

					sendAddMessage(pageOut, mailOut, MsgType.INFO, "Total row(s) affected " + totalRowsAffected + ", Execution Time " + batchExecTime);
				}
				catch (SQLException ex)
				{
//					if (aseExceptionsToWarnings)
//					{
//						String msg = getSqlWarningMsgs(ex);
//						if (StringUtil.hasValue(msg))
//						{
//							sb.append("\n");
//							sb.append(msg);
//							sb.append("\n");
//						}
//					}
//					else
//					{
//						throw ex;
//					}
					sendUpdateModalStatus(pageOut, mailOut, "Error executing query");
//					addMessage(out, "SqlState=" + ex.getSQLState() + ", ErrorCode=" + ex.getErrorCode() + ", Message=" + ex.getMessage(), Status.ERROR);			
					String msg = ""
							+ "ErrorCode: " + ex.getErrorCode() + "<br>"
							+ "SqlState: "  + ex.getSQLState()  + "<br>"
							+ "Message: "   + ex.getMessage()   + "<br>"
							;
					sendAddMessage(pageOut, mailOut, MsgType.ERROR, msg);
				}
			}

//			out.println("<script>");
//			out.println("document.getElementById('dbx-uda-output-container').appendChild(document.createElement('br'));");
//			out.println("document.getElementById('dbx-uda-output-container').appendChild(document.createElement('br'));");
//			out.println("document.getElementById('dbx-uda-output-container').appendChild(document.createElement('hr'));");
//			out.println("</script>");
//			out.flush();
		}
		catch (SQLException e)
		{
			sendUpdateModalStatus(pageOut, mailOut, "Database connection error");
			sendAddMessage(pageOut, mailOut, MsgType.ERROR, "Database connection error: " + e.getMessage());
		}
		finally
		{
			try
			{
				pageOut.println("<script>");
				pageOut.println("document.getElementById('dbx-uda-output-container').appendChild(document.createElement('br'));");
				pageOut.println("document.getElementById('dbx-uda-output-container').appendChild(document.createElement('hr'));");
				pageOut.println("</script>");
				pageOut.flush();
				
				if (stmt != null)
				{
					sendUpdateModalStatus(pageOut, mailOut, "Closing statement...");
					stmt.close();
				}
				if (conn != null)
				{
					sendUpdateModalStatus(pageOut, mailOut, "Disconnecting from DBMS server...");
					conn.close();
					sendStopTimers(pageOut, mailOut);
					sendUpdateModalStatus(pageOut, mailOut, "Disconnected from DBMS");
					sendAddMessage(pageOut, mailOut, MsgType.SUCCESS, "Disconnected from DBMS");
				}
			}
			catch (SQLException e)
			{
				sendStopTimers(pageOut, mailOut);
				sendUpdateModalStatus(pageOut, mailOut, "Error closing resources");
				sendAddMessage(pageOut, mailOut, MsgType.ERROR, "Error closing resources: " + e.getMessage());
			}
			
			// Close modal after completion
			pageOut.println("<script>setTimeout(closeModal, 500);</script>");
			pageOut.flush();
			
//			String totalExecTime = TimeUtils.msDiffNowToTimeStr(allStartTime);
			String totalExecTime = TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", System.currentTimeMillis() - allStartTime);
			sendAddMessage(pageOut, mailOut, MsgType.INFO, "Total Execution Time " + totalExecTime);

			sendFinnished(pageOut, mailOut);
		}
	}

	
	private void checkAndDisplayWarnings(PrintWriter pageOut, PrintWriter mailOut, Statement stmt) 
	throws SQLException, IOException
	{
		writeSqlWarningMsgs(pageOut, mailOut, stmt.getWarnings(), "checkAndDisplayWarnings");
		stmt.clearWarnings();
	}
	

	/**
	 * Connect via JDBC to the configured DBMS URL. using: getDbmsUrl(), getDbmsUsername(), getDbmsPassword()
	 * @return
	 * @throws Exception
	 */
	protected DbxConnection dbmsConnect()
	throws Exception
	{
		DbxConnection conn = null;

		String jdbcUrl  = getDbmsUrl();
		String jdbcUser = getDbmsUsername();
		String jdbcPass = getDbmsPassword();
		
		if (jdbcPass == null)
			jdbcPass = "";

		ConnectionProp cp = new ConnectionProp();
		cp.setUrl(jdbcUrl);
		cp.setUsername(jdbcUser);
		cp.setPassword(jdbcPass);
		cp.setAppName(Version.getAppName());

		_logger.info("User Defined Action Name '" + getName() + "'. Connecting to URL='" + jdbcUrl + "', username='" + jdbcUser + "'.");
		if (StringUtil.hasValue(getLogFilename()))
			logInfo("User Defined Action: Connecting to URL='" + jdbcUrl + "', username='" + jdbcUser + "'.");

		conn = DbxConnection.connect(null, cp);

		return conn;
	}

	private void installConnectionMessageHandler(DbxConnection conn, final PrintWriter pageOut, final PrintWriter mailOut)
	{
		// Setup a message handler
		// Set an empty Message handler
//		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection || conn instanceof TdsConnection)
		{
			// Create a new message handler which will be used for jConnect
			SybMessageHandler newMsgHandler = new SybMessageHandler()
			{
				@Override
				public SQLException messageHandler(SQLException sqle)
				{
//					// When connecting to repserver we get those messages, so discard them
//					// Msg 32, Level 12, State 0:
//					// Server 'GORAN_1_RS', Line 0, Status 0, TranState 0:
//					// Unknown rpc received.
//					if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(_connectedToProductName))
//					{
//						if (sqle.getErrorCode() == 32)
//						{
//							if (_logger.isDebugEnabled())
//								_logger.debug("Discarding RepServer Message: "+ AseConnectionUtils.sqlExceptionToString(sqle));
//							return null;
//						}
//					}
					
//					// Increment Usage Statistics
//					if (sqle instanceof SQLWarning)
//						incSqlWarningCount();
//					else
//						incSqlExceptionCount();

					// Add it to the progress dialog
//					progress.addMessage(sqle);
//					String msg = AseConnectionUtils.sqlExceptionToString(sqle);
//					String msg = AseConnectionUtils.getSqlWarningMsgs(sqle).replace("\n", "<br>");
					try
					{
						writeSqlWarningMsgs(pageOut, mailOut, sqle, "Syb-messageHandler");
					}
					catch(IOException ex)
					{
						_logger.error("Problems in Sybase.messageHandler(), continuing anyway", ex);
					}
//					String msg = getSqlWarningMsgs(sqle).replace("\n", "<br>");
//					thisInstance.addPlainMessage(out, msg);

//					// If we want to STOP if we get any errors...
//					// Then we should return the origin Exception
//					// SQLException will abort current SQL Batch, while SQLWarnings will continue to execute
//					if (_abortOnDbMessages)
//						return sqle;

					// Downgrade ALL messages to SQLWarnings, so executions wont be interuppted.
					return AseConnectionUtils.sqlExceptionToWarning(sqle);
					
					// TODO: remove: ... jConnect: SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.

				}
			};

			if (conn instanceof SybConnection)
			{
//				curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
				((SybConnection)conn).setSybMessageHandler(newMsgHandler);
			}
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).setSybMessageHandler(newMsgHandler);
		}

		if (conn instanceof SqlServerConnection)
		{
			ISQLServerMessageHandler newMsgHandler = new ISQLServerMessageHandler()
			{
				@Override
				public ISQLServerMessage messageHandler(ISQLServerMessage srvMsg)
				{
					// Add it to the progress dialog
//					progress.addMessage(srvMsg.toSqlExceptionOrSqlWarning());
//					thisInstance.addMessage(out, srvMsg.toSqlExceptionOrSqlWarning(), Status.INFO);
//					thisInstance.addPlainMessage(out, "FIXME: " + srvMsg);
//					try
//					{
//						writeSqlWarningMsgs(out, srvMsg);
//					}
//					catch(IOException ex)
//					{
//						_logger.error("Problems in Sybase.messageHandler(), continuing anyway", ex);
//					}

//					if (srvMsg instanceof SQLServerInfoMessage)
//					{
//						incSqlWarningCount();
//					}

					if (srvMsg instanceof SQLServerError) 
					{
//						incSqlExceptionCount();

						SQLServerError errorMsg = (SQLServerError)srvMsg;
						srvMsg = errorMsg.toSQLServerInfoMessage();
					}

					try
					{
						writeSqlWarningMsgs(pageOut, mailOut, srvMsg.toSqlExceptionOrSqlWarning(), "MsSsql-messageHandler");
					}
					catch(IOException ex)
					{
						_logger.error("Problems in msSql.messageHandler(), continuing anyway", ex);
					}
					
					return srvMsg;
				}
			};
			((SqlServerConnection)conn).setMessageHandler(newMsgHandler);
		}
		
	}

	
	
	
	
	/**
	 * Most of this was grabbed from SQL Window ... and a bunch deleted
	 * @param pageOut
	 * @param sqe
	 * @param fromMethod 
	 * @throws IOException
	 */
	private void writeSqlWarningMsgs(PrintWriter pageOut, PrintWriter mailOut, SQLException sqe, String fromMethod)
	throws IOException
	{
		while (sqe != null)
		{
			int    msgNum      = sqe.getErrorCode();
			String msgText     = StringUtil.removeLastNewLine(sqe.getMessage());
			int    msgSeverity = -1;
			String objectText  = null;

			boolean isErrorMsg = true;

			StringBuilder sb = new StringBuilder();

			// Create a "common" EedInfo, which is a "container" class that contains all different EedInfo variants
			// This for both Sybase and SQL Server
			CommonEedInfo ceedi = new CommonEedInfo(sqe);
				
			if (_logger.isDebugEnabled())
				sb.append("DEBUG: classType: " + sqe.getClass().getName() + ", ceedi.hasEedInfo()=" + ceedi.hasEedInfo() + "\n");

			int scriptRow = -1;
			if (ceedi.hasEedInfo())
			{
				if (_logger.isDebugEnabled())
					sb.append("DEBUG: ErrorCode=" + ceedi.getErrorCode() + ", Severity=" + ceedi.getSeverity() + ", State=" + ceedi.getState() + ", ServerName='" + ceedi.getServerName() + "', LineNumber=" + ceedi.getLineNumber() + ", ProcedureName='" + ceedi.getProcedureName() + "', Message=" + ceedi.getMessage() + "'.\n");

				// Error is using the additional TDS error data.
				msgSeverity  = ceedi.getSeverity();
				
				// Try to figgure out what we should write out in the 'script row'
				// Normally it's *nice* to print out *where* in the "whole" document the error happened, especially syntax errors etc (and not just "within" the SQL batch, because you would have several in a file)
				// BUT: if we *call* a stored procedure, and that stored procedure produces errors, then we want to write from what "script line" (or where) the procedure was called at
				// BUT: if we are developing creating procedures/functions etc we would want *where* in the "script line" (within the prcedure text) the error is produced (easier to find syntax errors, faulty @var names, table nemaes etc...)
				int lineNumber = ceedi.getLineNumber();
				int lineNumberAdjust = 0;

				// for some product LineNumber starts at 0, so lets just adjust for this in the calculated (script row ###)
				if (    DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(_connectedToProductName)
				     || DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(_connectedToProductName) )
				{
					lineNumberAdjust = 1;

					// Parse SQL Anywhere messages that looks like: 
					//     Msg 102, Level 15, State 0:
					//     Line 0 (script row 884), Status 0, TranState 1:
					//     SQL Anywhere Error -131: Syntax error near 'x' on line 4
					// Get the last part 'on line #' as the lineNumberAdjust
					if (msgText.matches(".*on line [0-9]+[ ]*.*"))
					{
						int startPos = msgText.indexOf("on line ");
						if (startPos >= 0)
						{
							startPos += "on line ".length();
							int endPos = msgText.indexOf(" ", startPos);
							if (endPos <= 0)
								endPos = msgText.length();
							
							String lineNumStr = msgText.substring(startPos, endPos);

							try { lineNumberAdjust = Integer.parseInt(lineNumStr); }
							catch(NumberFormatException ignore) {}
						}
					}
				}

				// print messages, take some specific actions
				if (msgSeverity <= 10)
				{
					isErrorMsg = false;
//					// If message originates from a Stored Procedures
//					// do not use the Line Number from the Stored Procs, instead use the SQL Batch start...
//					// ERROR messages get's handle in a TOTAL different way
//					if (StringUtil.hasValue( ceedi.getProcedureName() ))
//					{
//						lineNumber = 1;
//
//						// If batch starts with empty lines, increment the lineNumber...
//						lineNumber += StringUtil.getFirstInputLine(currentSql);
//					}
				}

				// which row in the script was this at... not this might change if (msgSeverity > 10)
//				scriptRow = startRowInSelection + batchStartRow + lineNumber + lineNumberAdjust;
				scriptRow = lineNumber;

				// Fill in some extra information for error messages
				if (msgSeverity > 10)
				{
					boolean firstOnLine = true;
					sb.append("Msg " + sqe.getErrorCode() +
							", Level " + ceedi.getSeverity() + ", State " +
							ceedi.getState() + ":\n");

					if (StringUtil.hasValue( ceedi.getServerName() ))
					{
						sb.append("Server '" + ceedi.getServerName() + "'");
						firstOnLine = false;
					}
					if (StringUtil.hasValue( ceedi.getProcedureName() ))
					{
						sb.append( (firstOnLine ? "" : ", ") +
								"Procedure '" + ceedi.getProcedureName() + "'");
						firstOnLine = false;
					}

					String scriptRowStr = "";

					sb.append( (firstOnLine ? "" : ", ") + "Line " + ceedi.getLineNumber() + scriptRowStr);
					if (ceedi.supportsEedParams()) sb.append(", Status "    + ceedi.getStatus());
					if (ceedi.supportsTranState()) sb.append(", TranState " + ceedi.getTranState() + ":");
					sb.append("\n");
					
					if (ceedi.hasEedParams())
					{
						Map<String, Object> map = ceedi.getEedParamsAsMap();
						if ( ! map.isEmpty() )
							sb.append("Extra Error Info: ").append(map).append("\n");
					}
				}

				// Now print the error or warning
				String msg = sqe.getMessage();
				if (msg.endsWith("\n"))
					sb.append(msg);
				else
					sb.append(msg+"\n");

			} // end: hasEedInfo()
			else
			{
				// jConnect: SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( "010P4".equals(sqe.getSQLState()) )
				{
					// Simply ignore: 010P4: An output parameter was received and ignored.
					// This is when a Stored Procedure return code is returned, which is Output Parameter 1
				}
				else if ( "010SL".equals(sqe.getSQLState()) )
				{
					// IGNORE: 010SL: Out-of-date metadata accessor information was found on this database.  Ask your database administrator to load the latest scripts.
				}
				// OK, jTDS drivers etc, will have warnings etc in print statements
				// Lets try to see if it's one of those.
				else if (sqe.getErrorCode() == 0 && sqe instanceof SQLWarning)
				{
					isErrorMsg = false;

					if (StringUtil.isNullOrBlank(msgText))
						sb.append(" ");
					else
						sb.append(msgText);
				}
				else
				{
					// new Exception("DUMMY Exception to se from where things are called").printStackTrace();
					
					if (sqe instanceof SQLWarning)
					{
						isErrorMsg = false;

						if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL))
						{
							// Simplified message from SQLServer
							String msg = sqe.getMessage();
							if (_logger.isDebugEnabled())
								msg = sqe.getMessage() + "  [ErrorCode=" + sqe.getErrorCode() + ", SQLState=" + sqe.getSQLState() + "]";

							sb.append(msg);
						}
						else
						{
							String msg = "SQL-Warning: " +
									_connectedToProductName + ": ErrorCode "+sqe.getErrorCode()+", SQLState "+sqe.getSQLState()+", WarningClass: " + sqe.getClass().getName() + "\n"
									+ sqe.getMessage();
							sb.append(msg);
						}
					}
					else
					{
						String msg = "Unexpected SQL-Exception: " +
								_connectedToProductName + ": ErrorCode "+sqe.getErrorCode()+", SQLState "+sqe.getSQLState()+", ExceptionClass: " + sqe.getClass().getName() + "\n"
								+ sqe.getMessage();
						sb.append(msg);
					}

//					// Get Oracle ERROR Messages
//					if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE))
//					{
//						oracleShowErrors(_conn, resultCompList, startRowInSelection, batchStartRow, currentSql);
//
//						// also try to get the procedure text, which will be added to the message
//						// but not for print statement
//						if (_getObjectTextOnError_chk.isSelected())
//						{
////							String searchForName = "";
////							int    lineNumber    = -1;
////
////							objectText = DbUtils.getOracleObjectText(_conn, searchForName);
////							objectText = StringUtil.markTextAtLine(objectText, lineNumber, true);
//						}
//					}
				}
			} // end: NOT hasEedInfo
			
			// Add the info to the list
			if (sb.length() > 0)
			{
				// If new-line At the end, remove it
				if ( sb.charAt(sb.length()-1) == '\n' )
					sb.deleteCharAt(sb.length()-1);

				String msg = sb.toString();
				msg = escapeHtml(msg);
				msg = msg.replace("\n", "<br>");

				pageOut.println("<!-- DEBUG-FROM " + fromMethod + "at[" + TimeUtils.toString(System.currentTimeMillis()) + "]: " + msg + " -->");
				
				if (isErrorMsg)
				{
					sendModalMessage(pageOut, mailOut, msg);
					sendAddPlainErrorMessage(pageOut, mailOut, msg);
				}
				else
				{
					sendModalMessage(pageOut, mailOut, msg);
					sendAddPlainMessage(pageOut, mailOut, msg);
				}
			}

			// Get next 
			sqe = sqe.getNextException();

		} // end: loop sqe

	} // end: method

} // end: class

