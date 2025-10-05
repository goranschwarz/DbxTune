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
package com.dbxtune.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Base64;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.mgt.NoGuiManagementServer;

public class ServletUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Check for Basic or Bearer authentication
	 * 
	 * @param request      Request object of the Servlet
	 * @param response     Response object of the Servlet
	 * @param out          If we want to write something back, do it here...
	 * @return true = Authenticated, false = Failure
	 * @throws IOException
	 */
	public static boolean checkBasicOrTokenAuthentication(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out) 
	throws IOException
	{
		// Check if we got authorization header
		String authHeader = request.getHeader("Authorization");

		// If no header 'Authorization' then check for QueryString parameter 'access_token'...
		if (authHeader == null)
		{
			String accessToken = request.getParameter("access_token");
			if (accessToken != null)
			{
				if      (accessToken.startsWith("Basic "))  { authHeader = accessToken; }
				else if (accessToken.startsWith("Bearer ")) { authHeader = accessToken; }
				else                                        { authHeader = "Bearer " + accessToken; }
			}
		}
		
		// Still no 'Authorization' header or 'access_token' parameter...
		if ( authHeader == null )
		{
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.println("{ \"error\" : \"Missing Authorization header\" }");
			return false;
		}


		// Get basic/bearer tokens...
		String expectedBasicToken;
		String expectedBearerToken;

		if (DbxTuneCentral.APP_NAME.equals(Version.getAppName()))
		{
			expectedBasicToken  = Configuration.getCombinedConfiguration().getProperty(DbxTuneCentral.PROPKEY_NOGUI_MANAGEMENT_http_auth_Basic , null);
			expectedBearerToken = Configuration.getCombinedConfiguration().getProperty(DbxTuneCentral.PROPKEY_NOGUI_MANAGEMENT_http_auth_Bearer, null);
		}
		else
		{
			expectedBasicToken  = Configuration.getCombinedConfiguration().getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Basic , null);
			expectedBearerToken = Configuration.getCombinedConfiguration().getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Bearer, null);
		}
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Authentication: expectedBasicToken ='" + expectedBasicToken  + "' for URL='" + request.getRequestURL() + "'.");
			_logger.debug("Authentication: expectedBearerToken='" + expectedBearerToken + "' for URL='" + request.getRequestURL() + "'.");
		}

		//------------------------------------------------------
		if ( authHeader.startsWith("Bearer ") )
		{
			String expectedToken = expectedBearerToken;
			if (expectedToken == null)
			{
				_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' Bearer Autentication NOT ENABLED. The call was rejected.");

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("{ \"error\" : \"Bearer Authentication is NOT ENABLED, not authorized\" }");
				return false;
			}
			
			if (expectedToken.startsWith("Bearer "))
				expectedToken = expectedToken.substring("Bearer ".length()).trim();

			String token = authHeader.substring("Bearer ".length()).trim();
			
			if ( expectedToken.equals(token) )
			{
				_logger.info("Success Bearer Authorization to URL '" + request.getRequestURL() + "'.");
				return true;
			}
			else
			{
				_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' with the wrong token='" + token + "'. The call was rejected.");

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("{ \"error\" : \"Invalid token, not authorized\" }");
				return false;
			}
		}
		//------------------------------------------------------
		else if ( authHeader.startsWith("Basic ") )
		{
			String expectedToken = expectedBasicToken;
			if (expectedToken == null)
			{
				_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' Basic Autentication NOT ENABLED. The call was rejected.");

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("{ \"error\" : \"Basic Authentication is NOT ENABLED, not authorized\" }");
				return false;
			}

			if (expectedToken.startsWith("Basic "))
				expectedToken = expectedToken.substring("Basic ".length()).trim();


			// Decode Base64 credentials (username:password)
			String   base64Credentials = authHeader.substring("Basic ".length());
			String   credentials       = new String(Base64.getDecoder().decode(base64Credentials));
			String[] values            = credentials.split(":", 2);
			
			String username = values[0];
			String password = values[1];
			
			String expectedUser     = "admin";
			String expectedPassword = expectedToken;

			if ( expectedToken.equals(base64Credentials) )
			{
				_logger.info("Success Basic Authorization to URL '" + request.getRequestURL() + "'.");
				return true;
			}
			else if ( expectedUser.equals(username) && expectedPassword.equals(password) )
			{
				_logger.info("Success Basic Authorization to URL '" + request.getRequestURL() + "'.");
				return true;
			}
			else
			{
				_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' with the wrong user='" + username + "', password='" + password + "'. The call was rejected.");

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader("WWW-Authenticate", "Basic realm=\"DbxTuneBasicAuthRealm\"");
				return false;
			}
			
		}
		//------------------------------------------------------
		else
		{
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.println("{ \"error\" : \"Invalid Authorization header. Expected: Bearer or Basic\" }");
			return false;
		}
	}




//	public static boolean checkTokenAuthentication(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out, String expectedToken) 
//	throws IOException
//	{
//		String authHeader = request.getHeader("Authorization");
//
//		if ( authHeader == null || !authHeader.startsWith("Bearer ") )
//		{
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			out.println("{ \"error\" : \"Missing or invalid Authorization header\" }");
//			return false;
//		}
//
//		String token = authHeader.substring("Bearer ".length()).trim();
//		if ( expectedToken.equals(token) )
//		{
//			_logger.info("Success Bearer Authorization to URL '" + request.getRequestURL() + "'.");
//			return true;
//		}
//		else
//		{
//			_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' with the wrong token='" + token + "'. The call was rejected.");
//
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			out.println("{ \"error\" : \"Invalid token, not authorized\" }");
//			return false;
//		}
//	}
//
//	public static boolean checkBasicAuthentication(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out, String expectedUsername, String expectedPassword) 
//	throws IOException
//	{
//		String authHeader = request.getHeader("Authorization");
//
//		if ( authHeader == null || !authHeader.startsWith("Basic ") )
//		{
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			response.setHeader("WWW-Authenticate", "Basic realm=\"DbxTuneBasicAuthRealm\"");
//			_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' without having 'Authorization' in the header. The call was rejected.");
//			return false;
//		}
//
//		// Decode Base64 credentials (username:password)
//		String   base64Credentials = authHeader.substring("Basic ".length());
//		String   credentials       = new String(Base64.getDecoder().decode(base64Credentials));
//		String[] values            = credentials.split(":", 2);
//		
//		String username = values[0];
//		String password = values[1];
//
//		if ( expectedUsername.equals(username) && expectedPassword.equals(password) )
//		{
//			_logger.info("Success Basic Authorization to URL '" + request.getRequestURL() + "'.");
//			return true;
//		}
//		else
//		{
//			_logger.warn("Someone tried to access URL '" + request.getRequestURL() + "' with the wrong user='" + username + "', password='" + password + "'. The call was rejected.");
//
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			response.setHeader("WWW-Authenticate", "Basic realm=\"DbxTuneBasicAuthRealm\"");
//			return false;
//		}
//	}
}
