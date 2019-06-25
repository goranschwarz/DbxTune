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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.utils.StringUtil;

public class DbxTuneReportsServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private final String REPORTS_DIR  = DbxTuneCentral.getAppReportsDir();

	ServletOutputStream out = null;
	HttpServletResponse _resp = null;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		_resp = resp;
		out = resp.getOutputStream();


		String operation     = req.getParameter("op");
		String inputName     = req.getParameter("name");

		if (StringUtil.isNullOrBlank(operation))
			operation = "view";

		if (StringUtil.isNullOrBlank(inputName))
			throw new ServletException("No input parameter named 'name'.");

		if ("view".equalsIgnoreCase(operation))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			String filename = REPORTS_DIR + File.separatorChar + inputName;
			File f = new File(filename);
			
			if (f.exists())
			{
				String content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
				out.println(content);
			}
			else
			{
				out.println("File '" + f + "' didn't exist.");
			}
		}
		if ("viewLatest".equalsIgnoreCase(operation))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			File f = getLastReportFileForServer(inputName);

			if (f == null)
			{
				out.println("No Report Files was found for server '" + inputName + "'.");
			}
			else
			{
				if (f.exists())
				{
					String content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
					out.println(content);
				}
				else
				{
					out.println("File '" + f + "' didn't exist.");
				}
			}
		}
		else if ("remove".equalsIgnoreCase(operation))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			String filename = REPORTS_DIR + File.separatorChar + inputName;
			File f = new File(filename);
			
			if (f.exists())
			{
				if (f.delete())
					out.println("Removed report named '" + inputName + "'.");
				else
					out.println("Problems removing report named '" + inputName + "'.");
			}
			else
			{
				out.println("File '" + f + "' didn't exist.");
			}
			
			// refresh the local page
			String redirect = resp.encodeRedirectURL(req.getContextPath() + "/overview#reportfiles");
			resp.sendRedirect(redirect);
		}
		else
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			out.println("No operation '" + operation + "'.");
			//throw new ServletException("No operation '" + operation + "'.");
		}

		out.flush();
		out.close();
	}
	
	private File getLastReportFileForServer(String forSrvName)
	{
		File retFile = null;
		
		for (String file : OverviewServlet.getFilesInReportsDir())
		{
			File f = new File(file);
			if (f.isDirectory())
				continue;
			
			String filename   = f.getName();
			if ( ! filename.startsWith(forSrvName + ".") )
				continue;
			
			retFile = f;
		}
		return retFile;
	}
}
