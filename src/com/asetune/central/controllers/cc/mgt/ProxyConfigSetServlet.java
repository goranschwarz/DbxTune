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
package com.asetune.central.controllers.cc.mgt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.controllers.cc.ProxyHelper;

public class ProxyConfigSetServlet 
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// Get "basic" stuff... implemented in parent class
		getSrvInfo(req);
		
		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("Can't find Base URL for Counter Collector named '" + getSrvName() + "'.");
			// TODO: send some status message and possibly a JSON "error" object
			return;
		}
		

		URL url = new URL(getCollectorBaseUrl() + "/api/mgt/config/set");
		HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();

		// set request header if required
		httpConn.setRequestProperty("Authentication", getMgtAuthentication());
//		httpConn.setRequestProperty("header1", "value1");

		httpConn.setRequestMethod("POST");

		// PROXY: Transfer data from the Caller to the HTTP end point 
		sendData(req, httpConn);

		// PROXY: Transfer data from the HTTP Call to the caller 
		sendResult(httpConn, resp, APPLICATION_JSON);
	}

}
