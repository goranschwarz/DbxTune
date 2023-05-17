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
package com.asetune.central.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

//@WebServlet(name="DownloadRecordingServlet", urlPatterns={"/download-recording"})
public class DownloadRecordingServlet
extends HttpServlet
{
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException 
	{
		String currentUsername = "-no-principal-";

		Principal principal = request.getUserPrincipal();
		if (principal != null)
			currentUsername = principal.getName();

		String from = "from getRemoteHost='" + request.getRemoteHost() + "', currentUsername='"+currentUsername+"', by user '" + request.getRemoteUser() + "'.";

		
		String recordingsDir = DbxTuneCentral.getAppDataDir();
		
		String fileName = request.getParameter("name");

		//-----------------------------------------
		// CHECK: Error / input
		//-----------------------------------------
		// Check if we are allowed do download
		boolean isDownloadRecordingEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(OverviewServlet.PROPKEY_enableDownloadRecordings, OverviewServlet.DEFAULT_enableDownloadRecordings);
		if ( ! isDownloadRecordingEnabled)
		{
//			throw new IOException("Download Recordings is NOT enabled. This can be enabled with property: " + OverviewServlet.PROPKEY_enableDownloadRecordings + " = true");
			String msg = "Download Recordings is NOT enabled. This can be enabled with property: " + OverviewServlet.PROPKEY_enableDownloadRecordings + " = true";
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
		}

		// Check if file exists
		if (StringUtil.isNullOrBlank(fileName))
		{
//			throw new FileNotFoundException("Sorry: Specify name to download with the parameter 'name'.");
			String msg = "Sorry: Specify name to download with the parameter 'name'.";
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
			
			// Or we can do something like the below...
		//	response.resetBuffer();
		//	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		//	response.setHeader("Content-Type", "application/json");
		//	response.getOutputStream().print("{\"errorMessage\":\"" + msg + "\"}");
		//	response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!			
		}

		// Check if file exists
		File downloadFile = new File(recordingsDir, fileName);
		if ( ! downloadFile.exists() )
		{
//			throw new FileNotFoundException("Sorry: The file '" + fileName + "' doesn't exists.");
			String msg = "Sorry: The file '" + fileName + "' doesn't exists.";
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
		}

		//-----------------------------------------
		// DOWNLOAD
		//-----------------------------------------
		// Download the file
//		try (FileInputStream fis = new FileInputStream(fileName); OutputStream out = response.getOutputStream()) 
//		{
//			response.setContentType("application/octet-stream"); 
//			response.setContentLengthLong( downloadFile.length() ); 
//
//			IOUtils.copy(fis, out); 
//		}

		_logger.info("Download-Start: File location on server '" + downloadFile.getAbsolutePath() + "'. " + from);
		ServletContext ctx = getServletContext();
		InputStream fis = new FileInputStream(downloadFile);
		String mimeType = ctx.getMimeType(downloadFile.getAbsolutePath());
		response.setContentType(mimeType != null ? mimeType:"application/octet-stream");
		response.setContentLengthLong(downloadFile.length());
		response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadFile.getName() + "\"");

		ServletOutputStream os = response.getOutputStream();
		byte[] bufferData = new byte[4096];
		int read=0;
		while((read = fis.read(bufferData))!= -1){
			os.write(bufferData, 0, read);
		}
		os.flush();
		os.close();
		fis.close();

		_logger.info("Download-End: File '" + downloadFile.getName() + "' downloaded to client was successfully. " + from);
	}
}
