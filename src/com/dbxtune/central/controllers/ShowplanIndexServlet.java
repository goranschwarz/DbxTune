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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;

/**
 * Landing page linking to the three Showplan Viewers ({@link ShowplanSqlServerServlet},
 * {@link ShowplanPostgresServlet}, {@link ShowplanAseServlet}). Uses the same shared
 * {@link HtmlStatic} head/navbar as those pages so all four feel like one consistent
 * section rather than four independently-styled pages.
 */
public class ShowplanIndexServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.print(createIndexOutput());
		out.flush();
		out.close();
	}

	public static String createIndexOutput()
	{
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - Showplan Viewer</title> \n" +
				"    <meta name='robots' content='max-image-preview:large' /> \n" +
				" \n" +
				HtmlStatic.getUserDefinedContentHead() +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "", true) +
				"    <div class='container-fluid px-4 py-3' style='max-width: 900px;'> \n" +
				"      <h2>&#128202; Showplan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste a query execution plan and get an interactive graphical view of it - no login, no data sent anywhere except this server.</p> \n" +
				" \n" +
				"      <div class='card shadow-sm mb-3'> \n" +
				"        <div class='card-body'> \n" +
				"          <h5 class='card-title'>SQL Server</h5> \n" +
				"          <p class='card-text'>Paste a <code>ShowPlanXML</code> plan (e.g. from <code>SET SHOWPLAN_XML ON</code>, Management Studio's \"Save Plan As...\", or Query Store). Rendered with <a href='https://github.com/JustinPealing/html-query-plan' target='_blank' rel='noopener'>html-query-plan</a>.</p> \n" +
				"          <a class='btn btn-primary' href='/showplan/sqlserver'>Open SQL Server Showplan Viewer</a> \n" +
				"        </div> \n" +
				"      </div> \n" +
				" \n" +
				"      <div class='card shadow-sm mb-3'> \n" +
				"        <div class='card-body'> \n" +
				"          <h5 class='card-title'>PostgreSQL</h5> \n" +
				"          <p class='card-text'>Paste an <code>EXPLAIN (ANALYZE, FORMAT JSON)</code> plan. Rendered with <a href='https://github.com/dalibo/pev2' target='_blank' rel='noopener'>pev2</a>.</p> \n" +
				"          <a class='btn btn-primary' href='/showplan/postgres.html'>Open Postgres Showplan Viewer</a> \n" +
				"        </div> \n" +
				"      </div> \n" +
				" \n" +
				"      <div class='card shadow-sm mb-3'> \n" +
				"        <div class='card-body'> \n" +
				"          <h5 class='card-title'>SAP ASE (Sybase)</h5> \n" +
				"          <p class='card-text'>Paste either a <code>show_cached_plan_in_xml</code> XML plan or a classic <code>sp_showplan</code> text plan - the format is auto-detected. Rendered with DbxTune's own graphical plan viewer.</p> \n" +
				"          <a class='btn btn-primary' href='/showplan/ase'>Open ASE Showplan Viewer</a> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}
}
