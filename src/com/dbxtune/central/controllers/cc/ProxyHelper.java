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
package com.dbxtune.central.controllers.cc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.OverviewServlet;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.central.pcs.DbxTuneSample;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProxyHelper 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String _srvName = null;
	// Management host/port
	private String _collectorMgtHostname = null;
	private int    _collectorMgtPort     = -1;
	private String _collectorMgtInfo     = null;
	private String _collectorAuthStr     = null;

	public static String APPLICATION_JSON = "application/json";
	public static String TEXT_HTML        = "text/html";
	
//	public static String XXXX = com.google.common.net.MediaType.JSON_UTF_8.toString();
	
	protected String getSrvName()
	{
		return _srvName;
	}

	protected String getCollectorBaseUrl()
	{
		if (_collectorMgtHostname == null || _collectorMgtPort == -1)
			return null;

		return "http://" + _collectorMgtHostname + ":" + _collectorMgtPort;
	}

	protected String getMgtAuthentication()
	{
		return _collectorAuthStr;
	}

	protected void getSrvInfo(HttpServletRequest req)
	throws IOException
	{
		_srvName = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
		
		if (_srvName == null)
			throw new IOException("Parameter 'srvName' was not given, for: " + req.getServletPath());

		System.out.println("PROXY.getSrvInfo() >>>>>>>>>>>> getServletPath = |" + req.getServletPath() + "|.");
		
		boolean xxx = true;
		if (xxx)
		{
			HttpSession session = req.getSession(false);
			System.out.println(">>>>>>>>>>>> session = |" + session + "|.");
			if (session != null)
			{
				System.out.println("          session.getAttributeNames(): " + Collections.list(session.getAttributeNames()));
			}
			
			String currentUsername = "-no-principal-";

			Principal principal = req.getUserPrincipal();
			if (principal != null)
				currentUsername = principal.getName();

			System.out.println(">>>>>>>>>>>> currentUsername = |" + currentUsername + "|.");
			String from = "from getRemoteHost='" + req.getRemoteHost() + "', currentUsername='" + currentUsername + "', by user '" + req.getRemoteUser() + "', req.getUserPrincipal()=" + req.getUserPrincipal() + ".";

			System.out.println(">>>>>>>>>>>> from: " + from);

			Principal userPrincipal = req.getUserPrincipal();
			if (userPrincipal != null)
			{
				System.out.println(">>>>>>>>>>>> userPrincipal.getName() = |" + userPrincipal.getName() + "|.");
				System.out.println(">>>>>>>>>>>> req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) = |" + req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) + "|."); 
			}
		}

		// Possibly: Get info from Central PCS Reader
		// Here we can get
		//  * if the srvName is hosted locally
		//  * info file name
		//  * possibly more "stuff"
		// And if it's a "remote" we can pass back a error message... that REMOTE Collectors is NOT yet supported
		boolean foundInfoFile = false;
		for (String file : OverviewServlet.getInfoFilesDbxTune())
		{
			File f = new File(file);
			String fSrvName = f.getName().split("\\.")[0];

System.out.println("PROXY: >>>>>>>>>>>> _srvName=|"+_srvName+"|, CHECK_FILE |" + file + "|, fSrvName=|" + fSrvName + "|.");

			if ( ! fSrvName.equals(_srvName) )
				continue;

			Configuration conf = new Configuration(file);

			_collectorMgtHostname = conf.getProperty   ("dbxtune.management.host", null);
			_collectorMgtPort     = conf.getIntProperty("dbxtune.management.port", -1);
			_collectorMgtInfo     = conf.getProperty   ("dbxtune.management.info", null);
			
			parseCollectorMgtInfo(_collectorMgtInfo);
			
			foundInfoFile = true;
System.out.println("PROXY: >>>>>>>>>>>> _srvName             =|"+_srvName+"|.");
System.out.println("PROXY: >>>>>>>>>>>> _collectorMgtHostname=|"+_collectorMgtHostname+"|.");
System.out.println("PROXY: >>>>>>>>>>>> _collectorMgtPort    =|"+_collectorMgtPort+"|.");
System.out.println("PROXY: >>>>>>>>>>>> _collectorMgtInfo    =|"+_collectorMgtInfo+"|.");
System.out.println("PROXY: >>>>>>>>>>>> _collectorAuthStr    =|"+_collectorAuthStr+"|.");
			break; // No need to continue
		}
		
		if ( ! foundInfoFile )
		{
			throw new IOException("No INFO FILE was found for server name '" + _srvName + "', cant continue.... in: " + req.getServletPath());
		}
	}
	
	private void parseCollectorMgtInfo(String collectorMgtInfo)
	{
		if (StringUtil.isNullOrBlankForAll(collectorMgtInfo))
			return;

		// This should look like: {"authorization":"Basic YWRtaW46ZnR3eXliMWVieGdTY0p6dFNSbVlIcVlVdjhIV2lJbXE="}
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(collectorMgtInfo);

			_collectorAuthStr = DbxTuneSample.getString(root, "authorization");
		}
		catch (Exception ex)
		{
			_logger.error("Problems reading 'authorization' from 'dbxtune.management.info'. Caught: " + ex);
		}

	}

	protected void sendData(HttpServletRequest req, HttpURLConnection httpConn)
	throws IOException
	{
		httpConn.setRequestProperty("Content-Type", "application/json");
		httpConn.setRequestProperty("Accept",       "application/json");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		httpConn.setUseCaches(false);

		
//		InputStream  requestInputStream = req.getInputStream();
//		OutputStream httpOutputStream   = httpConn.getOutputStream(); 
//
//		byte[] buf = new byte[8192];
//		int length;
//		while ((length = requestInputStream.read(buf)) != -1)
//		{
//			httpOutputStream.write(buf, 0, length);
//		}
//		httpOutputStream.flush();
//		httpOutputStream.close();

		// Read input
		String requestBody = IOUtils.toString(req.getReader());
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> sendData(): requestBody=|"+requestBody+"|");

		// Send to HTTP end point
		OutputStream httpOutputStream = httpConn.getOutputStream();
		byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
		httpOutputStream.write(bytes, 0, bytes.length);
		httpOutputStream.flush();
		httpOutputStream.close();
		
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> sendData -END-");
	}

	protected void sendResult(HttpURLConnection httpConn, HttpServletResponse resp, String mediaType) 
	throws IOException
	{
		// check status code
//		if(httpConn.getResponseCode() == 200) 
//		{
//		}
//		else
//		{
//		}

		// Set response headers
		if (StringUtil.hasValue(mediaType))
			resp.setContentType(mediaType);
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		// Get/Set response status
		int    httpRespCode = httpConn.getResponseCode();
		String httpRespMsg  = httpConn.getResponseMessage();
System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< sendResult: httpRespCode=" + httpRespCode + ", httpRespMsg=|" + httpRespMsg + "|.");

		resp.setStatus(httpRespCode);
//		if (httpRespCode != HttpServletResponse.SC_OK)
//		{
//			resp.sendError(httpRespCode, httpRespMsg);
//			return;
//		}

		// Should I return here... Or can I continue to send any possible data ???
		
		// Get "stream" from HTTP endpoint
		InputStream httpInputStream = httpConn.getInputStream();
		InputStream httpErrorStream = httpConn.getErrorStream();
System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< sendResult: httpInputStream=" + httpInputStream);
System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< sendResult: httpErrorStream=" + httpErrorStream);

		// Get "stream" to ServletCaller
		ServletOutputStream out = resp.getOutputStream();

		// transfer the "HTTP endpoint data" to the ServletCaller
		byte[] buf = new byte[8192];
		int length;
int totLen = 0;
		if (httpInputStream != null)
		{
			while ((length = httpInputStream.read(buf)) != -1)
			{
totLen += length;
System.out.println("<<<<<<<<<<< http INPUT Stream <<<<<<<<<<<<< sendResult: length=" + length + " data=|-not-printed-|");
//System.out.println("<<<<<<<<<<< http INPUT Stream <<<<<<<<<<<<< sendResult: length=" + length + " data=|" + new String(buf, 0, length) + "|");
				out.write(buf, 0, length);
			}
		}
		if (httpErrorStream != null)
		{
			while ((length = httpErrorStream.read(buf)) != -1)
			{
totLen += length;
System.out.println("<<<<<<<<<<< http ERROR Stream <<<<<<<<<<<<< sendResult: length=" + length + " data=|" + new String(buf, 0, length) + "|");
				out.write(buf, 0, length);
			}
		}
		out.flush();
		out.close();
		
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< sendResult -END- httpRespCode=" + httpRespCode + ", totLen=" + totLen);
	}

//	@Override
//	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
//		getSrvInfo(req);
//
//		System.out.println();
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getContextPath()=" + req.getContextPath());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getPathInfo   ()=" + req.getPathInfo());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getQueryString()=" + req.getQueryString());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getParameterMap()=" + req.getParameterMap());
//
//		if ("/config/set".equals(req.getPathInfo()))
//		{
//			System.out.println("doPost(): /config/set");
//		}
//			
//		// TODO Auto-generated method stub
//		super.doPost(req, resp);
//	}
//
//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
//		getSrvInfo(req);
//		
//		System.out.println();
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getContextPath()=" + req.getContextPath());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getPathInfo   ()=" + req.getPathInfo());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getQueryString()=" + req.getQueryString());
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getParameterMap()=" + req.getParameterMap());
//
//		final String auth = req.getHeader("Authorization");
//		System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization = |" + auth + "|.");
//		if (auth != null)
//		{
//			int firstSpace = auth.indexOf(" ");
//			String authMethod = auth.substring(0, firstSpace).trim();
//			String authToken  = auth.substring(firstSpace).trim();	
//	
//			if ("Basic".equals(authMethod))
//			{
//				byte[] decodedBytes = Base64.getDecoder().decode(authToken);
//				authToken = new String(decodedBytes);
//			}
//	
//			//	String decoded = new String( Base64.getDecoder().decode( auth.substring(firstSpace) ), StandardCharsets.UTF_8 );
//			System.out.println(">>>>>>>>>>>> ProxyHelper.GET.HEADER: Authorization.method = |" + authMethod + "|.");
//			System.out.println(">>>>>>>>>>>> ProxyHelper.GET.HEADER: Authorization.token  = |" + authToken  + "|.");
//		}
//		
//		
//		URL url = new URL("http://otherserver:otherport/url");
//		HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
//
//		// set http method if required
//		httpConn.setRequestMethod("GET");
//
//		for(String header : Collections.list(req.getHeaderNames()))
//		{
//			System.out.println("HEADER |" + header + "| = |" + req.getHeader(header) + "|.");
//		}
//		
////		httpConn.addRequestProperty(key, val);
//
//	}

//	/** Copy proxied response headers back to the servlet client. */
//	protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest, HttpServletResponse servletResponse) 
//	{
//		for (Header header : proxyResponse.getAllHeaders()) 
//		{
//			copyResponseHeader(servletRequest, servletResponse, header);
//		}
//	}

//	/** Copy request headers from the servlet client to the proxy request. */
//	protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) 
//	{
//		// Get an Enumeration of all of the header names sent by the client
//		@SuppressWarnings("unchecked")
//		Enumeration<String> enumerationOfHeaderNames = servletRequest.getHeaderNames();
//		while (enumerationOfHeaderNames.hasMoreElements()) {
//			String headerName = enumerationOfHeaderNames.nextElement();
//			copyRequestHeader(servletRequest, proxyRequest, headerName);
//		}
//	}
	
}
