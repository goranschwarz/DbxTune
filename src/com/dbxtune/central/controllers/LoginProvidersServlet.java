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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.oauth.OAuthProvider;
import com.dbxtune.central.oauth.OAuthProviderRegistry;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns the list of enabled OAuth login providers as JSON.
 * <p>
 * Mapped to {@code GET /api/login/providers} (public — no authentication required).
 * <p>
 * The response is consumed by {@code dbxLoginModal.js} to dynamically inject
 * "Sign in with …" buttons into the login dialog.
 *
 * <h3>Example response</h3>
 * <pre>
 * [
 *   {
 *     "id":          "entra",
 *     "displayName": "Microsoft",
 *     "iconHtml":    "&lt;svg …&gt;&lt;/svg&gt;",
 *     "startUrl":    "/oauth/start?provider=entra"
 *   }
 * ]
 * </pre>
 */
public class LoginProvidersServlet
extends HttpServlet
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	@JsonPropertyOrder({ "id", "displayName", "iconHtml", "startUrl" })
	public static class ProviderInfo
	{
		public String id;
		public String displayName;
		public String iconHtml;
		public String startUrl;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		List<ProviderInfo> result = new ArrayList<>();

		for (OAuthProvider p : OAuthProviderRegistry.getInstance().getEnabledProviders())
		{
			ProviderInfo info = new ProviderInfo();
			info.id          = p.getProviderId();
			info.displayName = p.getDisplayName();
			info.iconHtml    = p.getIconHtml();
			info.startUrl    = req.getContextPath() + "/oauth/start?provider=" + p.getProviderId();
			result.add(info);
		}

		_logger.debug("LoginProvidersServlet: returning {} enabled provider(s)", result.size());

		ObjectMapper om = Helper.createObjectMapper();
		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store");
		om.writeValue(resp.getWriter(), result);
	}
}
