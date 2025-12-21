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
package com.dbxtune.central.controllers;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;

public class LetsEncryptAcmeChallengeServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		String clientHost = request.getRemoteHost();
		String clientIp   = request.getRemoteAddr();

		String uri      = request.getRequestURI();
		String lastPart = uri.substring(uri.lastIndexOf('/') + 1);

		_logger.info("LetsEncrypt Acme Challenge Client Info: ip='" + clientIp + "', host='" + clientHost + "'. called: " + uri);

		if (_logger.isDebugEnabled())
		{
			String servletPath = request.getServletPath();
			String pathInfo    = request.getPathInfo();

			_logger.debug("LetsEncryptAcmeChallengeServlet: servletPath=|" + servletPath + "|");
			_logger.debug("LetsEncryptAcmeChallengeServlet: pathInfo   =|" + pathInfo    + "|");
			_logger.debug("LetsEncryptAcmeChallengeServlet: uri        =|" + uri         + "|");
			_logger.debug("LetsEncryptAcmeChallengeServlet: lastPart   =|" + lastPart    + "|");
		}
		
		
		String acmeChallangeDir      = DbxTuneCentral.getAppConfDir() + "/" + "ssl_acme-challenge" + "/";
		String acmeChallangeFileName = acmeChallangeDir + lastPart;

		// Create the directory if doesn't exists!
		if (true)
		{
			File acmeChallangeTmp = new File(acmeChallangeDir);
			if ( ! acmeChallangeTmp.exists() )
			{
				boolean created = acmeChallangeTmp.mkdir();
				if (created)
					_logger.info("LetsEncrypt Acme Challenge File Directory '" + acmeChallangeTmp + "' did NOT exist. So it was created.");
				else
					_logger.error("LetsEncrypt Acme Challenge File Directory '" + acmeChallangeTmp + "' did NOT exist. Error when creating it.");
			}
		}

		String payload = "";
		try
		{
			File acmeChallangeFile = new File(acmeChallangeFileName);
			if (acmeChallangeFile.exists())
			{
				payload = Files.readString(acmeChallangeFile.toPath());
				payload = payload.trim();
				_logger.info("LetsEncrypt Acme Challenge File '" + acmeChallangeFile + "' returned data |" + payload + "|.");
			}
			else
			{
				_logger.error("LetsEncrypt Acme Challenge File '" + acmeChallangeFile + "' does NOT EXISTS.");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "LetsEncrypt Acme Challenge File '" + acmeChallangeFile + "' does NOT EXISTS.");
			}
		}
		catch (Exception ex)
		{
			_logger.error("LetsEncrypt Acme Challenge File '" + acmeChallangeFileName + "' does NOT EXISTS.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "LetsEncrypt Acme Challenge File '" + acmeChallangeFileName + "' Exception: " + ex);
		}

		out.println(payload);

		out.flush();
		out.close();
	}
}
