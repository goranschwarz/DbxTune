/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
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
package com.dbxtune.central.controllers.cc.mgt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Central-side endpoint that receives a collector's self-registration POST and
 * writes the info content to DbxCentral's local info directory.
 *
 * <pre>
 *   POST /api/cc/mgt/collector/register?srv=SRVNAME
 *   Authorization: Bearer DbxTune-CollectorReg-2026-v1
 *   Content-Type: text/plain; charset=UTF-8
 *
 *   [raw properties file content]
 * </pre>
 *
 * <h3>Result</h3>
 * Writes (or overwrites) {@code $DBXTUNE_HOME/.dbxtune/info/SRVNAME.dbxtune}
 * on DbxCentral's host. {@link com.dbxtune.central.controllers.OverviewServlet}
 * and {@link com.dbxtune.central.controllers.cc.ProxyHelper} read files from
 * that directory, so the registered collector becomes immediately visible without
 * any restart.
 *
 * <h3>Auth</h3>
 * Requests must carry {@code Authorization: Bearer <token>} where the token
 * matches {@link NoGuiManagementServer#DEFAULT_collectorRegToken} (or the
 * configured override {@link NoGuiManagementServer#PROPKEY_collectorRegToken}).
 */
public class CollectorRegisterServlet
extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		// --- Auth check ---
		String expectedToken = Configuration.getCombinedConfiguration()
				.getPropertyRaw(NoGuiManagementServer.PROPKEY_collectorRegToken,
				                NoGuiManagementServer.DEFAULT_collectorRegToken);

		String authHeader = req.getHeader("Authorization");
		String bearerPrefix = "Bearer ";
		if (authHeader == null || !authHeader.startsWith(bearerPrefix))
		{
			_logger.warn("CollectorRegisterServlet: Missing or malformed Authorization header from " + req.getRemoteHost());
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized", "Missing Authorization: Bearer header");
			return;
		}
		String receivedToken = authHeader.substring(bearerPrefix.length()).trim();
		if (!expectedToken.equals(receivedToken))
		{
			_logger.warn("CollectorRegisterServlet: Invalid registration token from " + req.getRemoteHost());
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "forbidden", "Invalid registration token");
			return;
		}

		// --- Server name ---
		String srvName = req.getParameter("srv");
		if (StringUtil.isNullOrBlank(srvName))
		{
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "missing-param", "Missing required parameter: srv");
			return;
		}
		srvName = srvName.trim();

		// Only allow safe filename characters
		if (!srvName.matches("[A-Za-z0-9_\\-\\.]+"))
		{
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid-param", "Invalid server name");
			return;
		}

		// --- Read body ---
		String body;
		try (InputStream in = req.getInputStream())
		{
			body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		if (StringUtil.isNullOrBlank(body))
		{
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "empty-body", "Request body is empty");
			return;
		}

		// --- Write info file ---
		String infoDir = DbxTuneCentral.getAppInfoDir();
		File   dir     = new File(infoDir);
		if (!dir.exists())
			dir.mkdirs();

		File infoFile = new File(dir, srvName + ".dbxtune");
		try (FileOutputStream fos = new FileOutputStream(infoFile, false))
		{
			fos.write(body.getBytes(StandardCharsets.UTF_8));
		}

		_logger.info("CollectorRegisterServlet: Registered collector '" + srvName
				+ "' from " + req.getRemoteHost()
				+ " — wrote '" + infoFile.getAbsolutePath() + "'");

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getOutputStream().println("{\"status\":\"ok\",\"srv\":\"" + srvName + "\"}");
	}
}
