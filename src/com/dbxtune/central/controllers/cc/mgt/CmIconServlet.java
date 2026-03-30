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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serves CM icon images from the classpath ({@code com/dbxtune/images/}).
 *
 * <pre>
 *   GET /api/cc/mgt/cm/icon?file=images/CmActiveStatements.png
 * </pre>
 *
 * <p>The {@code file} parameter is the value returned by
 * {@code CountersModel.getIconFile()} — typically {@code images/CmName.png}.
 * Only {@code .png}, {@code .gif}, and {@code .jpg} files under the
 * {@code images/} prefix are served; any other path is rejected with 404.</p>
 *
 * <p>Responses are cached by the browser for 24 hours (icons never change
 * at runtime).</p>
 */
public class CmIconServlet
extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Classpath root under which all CM icons live. */
	private static final String ICON_CLASSPATH_PREFIX = "/com/dbxtune/images/";

	/** Default icon served when the requested icon file is not found. */
	private static final String DEFAULT_ICON = "CmNoIcon.png";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String file = req.getParameter("file");

		if (file == null || file.trim().isEmpty())
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: file");
			return;
		}
		file = file.trim();

		// Security: only allow safe filenames — no path traversal, no directory components
		// Accept the "images/" prefix that getIconFile() returns, then extract just the filename
		if (file.contains(".."))
		{
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid path");
			return;
		}

		// Strip any leading path (e.g. "images/", "ase/", etc.) — keep only the filename
		// This is intentional: all icons live flat in /com/dbxtune/images/ regardless of
		// what subdirectory getIconFile() may reference.
		String filename = file.contains("/") ? file.substring(file.lastIndexOf('/') + 1) : file;

		// Only serve known image extensions
		String lower = filename.toLowerCase();
		if (!lower.endsWith(".png") && !lower.endsWith(".gif") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg"))
		{
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not an image file");
			return;
		}

		String resourcePath = ICON_CLASSPATH_PREFIX + filename;
		InputStream is = CmIconServlet.class.getResourceAsStream(resourcePath);

		// If the requested icon doesn't exist, serve the default icon instead of a 404.
		// This avoids repeated 404s from the browser (404 responses aren't cached) and
		// eliminates the need for client-side fallback logic.
		if (is == null)
		{
			_logger.debug("CmIconServlet: icon not found '{}', serving default icon", resourcePath);
			is = CmIconServlet.class.getResourceAsStream(ICON_CLASSPATH_PREFIX + DEFAULT_ICON);
		}

		if (is == null)
		{
			// Even the default icon is missing — nothing we can do
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Icon not found: " + filename);
			return;
		}

		try (InputStream iconStream = is)
		{
			String contentType = lower.endsWith(".png")  ? "image/png"
			                   : lower.endsWith(".gif")  ? "image/gif"
			                   :                           "image/jpeg";

			resp.setContentType(contentType);
			// Icons never change — cache for 24 hours
			resp.setHeader("Cache-Control", "public, max-age=86400");

			byte[] buf = new byte[8192];
			int    n;
			while ((n = iconStream.read(buf)) >= 0)
				resp.getOutputStream().write(buf, 0, n);
		}
	}
}
