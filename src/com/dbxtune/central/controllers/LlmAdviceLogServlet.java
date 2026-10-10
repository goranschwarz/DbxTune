/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.llm.LlmAdviceLog;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Admin-only servlet for the saved "LLM Optimization Advice" requests (see {@link LlmAdviceLog}).
 * Mapped to {@code /admin/llm-log}, protected by the existing {@code /admin/*} security constraint.
 *
 * <p>Operations via query parameter {@code op}:
 * <ul>
 *   <li>{@code list}  - {@code days=N} (0 or missing = all): JSON {@code {saveEnabled, keepDays, notifyUsers, dir, entries:[...]}}, newest first</li>
 *   <li>{@code view}  - {@code name=<file>}: the saved page, re-rendered with the advice dialog's renderer</li>
 *   <li>{@code view}  - {@code name=<file>&raw=true}: the saved file as-is, as a download (plain, self-contained)</li>
 * </ul>
 */
public class LlmAdviceLogServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String op = StringUtil.nullToValue(req.getParameter("op"), "").trim();

		if ("list".equals(op))
		{
			handleList(req, resp);
		}
		else if ("view".equals(op))
		{
			handleView(req, resp);
		}
		else
		{
			writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown or missing operation 'op': '" + op + "'. Use 'list' or 'view'.");
		}
	}

	private void handleList(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		int days = StringUtil.parseInt(req.getParameter("days"), 0);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("saveEnabled", LlmAdviceLog.isSaveEnabled());
		result.put("keepDays",    LlmAdviceLog.getKeepDays());
		result.put("notifyUsers", LlmAdviceLog.isNotifyUsers());
		result.put("dir",         LlmAdviceLog.getLogDir().getAbsolutePath());
		result.put("entries",     LlmAdviceLog.list(days));

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		Helper.createObjectMapper().writeValue(resp.getWriter(), result);
	}

	private void handleView(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String name = req.getParameter("name");
		File   file = LlmAdviceLog.getFile(name);
		if (file == null)
		{
			writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "No saved LLM Advice request named '" + name + "'.");
			return;
		}

		_logger.info("LLM-ADVICE: Admin user '" + req.getRemoteUser() + "' viewed saved request '" + file.getName() + "'.");

		if ("true".equalsIgnoreCase(req.getParameter("raw")))
		{
			resp.setContentType("text/html");
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
			resp.setContentLengthLong(file.length());
			Files.copy(file.toPath(), resp.getOutputStream());
			return;
		}

		// The saved file is plain (no scripts). Here we add the advice dialog's renderer, with the CURRENT
		// library versions, which reads the values back from the page (dbxLlmAdvice.renderSavedRequest()).
		String html = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String enhance = "<link rel='stylesheet' href='/scripts/prism/prism-1.30.0.css'>\n"
				+ LlmAdviceServlet.getAdviceRendererIncludes() + "\n"
				+ "<script>dbxLlmAdvice.renderSavedRequest('#dbx-llm-advice');</script>\n";

		// Theme first in <head> (as on the other DbxCentral pages), so the page follows the user's Light/Dark choice
		int headPos = html.indexOf("<head>");
		if (headPos >= 0)
			html = html.substring(0, headPos + 6) + "\n<script src='/scripts/dbxtune/js/dbxTheme.js'></script>" + html.substring(headPos + 6);

		int pos = html.lastIndexOf("</body>");
		html = (pos < 0) ? html + enhance : html.substring(0, pos) + enhance + html.substring(pos);

		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().write(html);
	}

	private void writeJsonError(HttpServletResponse resp, int status, String message)
	throws IOException
	{
		Map<String, Object> err = new LinkedHashMap<>();
		err.put("success", false);
		err.put("message", message);

		resp.setStatus(status);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		new ObjectMapper().writeValue(resp.getWriter(), err);
	}
}
