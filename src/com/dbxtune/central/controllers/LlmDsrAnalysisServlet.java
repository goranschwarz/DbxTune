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
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AseTune;
import com.dbxtune.Db2Tune;
import com.dbxtune.MySqlTune;
import com.dbxtune.OracleTune;
import com.dbxtune.PostgresTune;
import com.dbxtune.SqlServerTune;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.central.llm.LlmClient;
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.central.llm.LlmModelEntry;
import com.dbxtune.central.llm.LlmOptimizeRequest;
import com.dbxtune.central.llm.LlmOptimizeResponse;
import com.dbxtune.pcs.report.DailySummaryReportDefault;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * "AI Analysis" of a whole Daily Summary Report: an LLM reviews the report's digest (numbers per section, no SQL text)
 * and gives a prioritized list of improvements.
 * <ul>
 *   <li>{@code GET /llm-dsr-analysis?name=<report file>} or {@code ?srv=<server>&period=<yyyy-MM-dd>} - the page (the DSR's "AI Analysis" button)</li>
 *   <li>{@code POST /api/llm/dsr-analysis} {@code {name | srv+period, serverContext, preview:true}} - the prompt, what is in the digest, and the
 *       report's last analysis ('cached'). Like the statement advice's preview it needs no login and works when the LLM feature is off.</li>
 *   <li>{@code POST /api/llm/dsr-analysis} {@code {name, serverContext, provider, model, progress}} - the LLM call, through
 *       {@link LlmSqlOptimizeServlet#runLlmCall} (the same checks, progress events, Stop and LLM log as the statement advice);
 *       the answer is also saved as the report's last analysis (see {@link #getCacheDir()}).</li>
 * </ul>
 * The digest is written into the report by {@code DailySummaryReportDefault.createLlmDigestScript()}; it is read here from
 * the report file on disk, so the browser only names the report.
 */
public class LlmDsrAnalysisServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PAGE_PATH = "/llm-dsr-analysis";
	public static final String API_PATH  = "/api/llm/dsr-analysis";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		// The parameters are read by dbxDsrAnalysis.js (never written into the HTML here)
		resp.setContentType("text/html;charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();

		out.println("<!DOCTYPE html>");
		out.println("<html lang='en-US'>");
		out.println("<head>");
		out.println("  <meta charset='UTF-8'>");
		out.println("  <title>DbxTune - AI Analysis of a Daily Summary Report</title>");
		out.println("  <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' />");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("  <style> .dbx-dsr-analysis-container { max-width: 1400px; margin: 20px auto; } </style>");
		out.println("</head>");
		out.println();
		out.println("<body>");
		out.println(HtmlStatic.getHtmlNavbar(PageSection.None, "", true));
		out.println("<div class='container-fluid dbx-dsr-analysis-container'>");
		out.println("  <h2>AI Analysis of a Daily Summary Report</h2>");
		out.println("  <div id='dsr-analysis-content'><p class='text-muted'>Loading...</p></div>");
		out.println("</div>");
		out.println();
		// fetchServerContext() of dbxLlmAdvice.js: the server configuration and hardware, from the Collector
		out.println(LlmAdviceServlet.getAdviceRendererIncludes());
		out.println("<script src='/scripts/dbxtune/js/dbxDsrAnalysis.js'></script>");
		out.println("<script> dbxDsrAnalysis.open(document.getElementById('dsr-analysis-content'), { llmEnabled: " + LlmClientRegistry.isFeatureEnabled() + " }); </script>");
		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		ObjectMapper om = Helper.createObjectMapper();

		JsonNode body;
		try
		{
			body = om.readTree(request.getReader());
		}
		catch (Exception ex)
		{
			writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "Could not parse the JSON request body: " + ex.getMessage());
			return;
		}
		String  name          = body.path("name")         .asText("");
		String  srv           = body.path("srv")          .asText("");
		String  period        = body.path("period")       .asText("");
		String  serverContext = body.path("serverContext").asText("");
		boolean preview       = body.path("preview")      .asBoolean(false);

		// Find the report and its digest
		File   reportFile;
		String digestJson;
		try
		{
			if (StringUtil.hasValue(name))
			{
				reportFile = Helper.getFileInsideDir(DbxTuneCentral.getAppReportsDir(), name);
				if (reportFile == null || ! reportFile.isFile() || ! reportFile.getName().endsWith(".html"))
				{
					writeError(om, out, response, HttpServletResponse.SC_NOT_FOUND, "not-found", "No Daily Summary Report named '" + name + "'.");
					return;
				}
				digestJson = readDigest(reportFile);
			}
			else if (StringUtil.hasValue(srv) && StringUtil.hasValue(period))
			{
				LocalDate day;
				try { day = LocalDate.parse(period); }
				catch (DateTimeParseException ex)
				{
					writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "'period' must be a date as yyyy-MM-dd, not '" + period + "'.");
					return;
				}
				Object[] found = findReportForPeriod(srv, day, om);
				if (found == null)
				{
					writeError(om, out, response, HttpServletResponse.SC_NOT_FOUND, "not-found", "No Daily Summary Report with an AI digest found for server '" + srv + "' and the day " + period + ".");
					return;
				}
				reportFile = (File)   found[0];
				digestJson = (String) found[1];
			}
			else
			{
				writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "Expected 'name' (the report file), or 'srv' and 'period' (yyyy-MM-dd).");
				return;
			}
		}
		catch (IOException ex)
		{
			_logger.warn("LlmDsrAnalysisServlet: problems reading the Daily Summary Report. name='" + name + "', srv='" + srv + "', period='" + period + "'.", ex);
			writeError(om, out, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "read-error", "Problems reading the report: " + ex.getMessage());
			return;
		}

		if (digestJson == null)
		{
			writeError(om, out, response, HttpServletResponse.SC_NOT_FOUND, "no-digest", "The report '" + reportFile.getName() + "' has no AI digest: it was created by an older version of DbxTune, or with '" + DailySummaryReportDefault.PROPKEY_llmDigestEnabled + "=false'. Create the report again to get one.");
			return;
		}

		JsonNode digest;
		try
		{
			digest = om.readTree(digestJson);
		}
		catch (Exception ex)
		{
			writeError(om, out, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "bad-digest", "The AI digest in the report '" + reportFile.getName() + "' could not be read: " + ex.getMessage());
			return;
		}

		// The prompt: LlmClientAbstract.buildPrompt() (shared by all providers), DSR branch
		LlmOptimizeRequest llmRequest = new LlmOptimizeRequest();
		llmRequest.setTask(LlmOptimizeRequest.TASK_DSR_ANALYSIS);
		llmRequest.setDsrDigest(digestJson);
		llmRequest.setDbVendor(productNameOf(digest.path("dbxApp").asText("")));
		llmRequest.setDbmsVersion(digest.path("dbmsVersion").asText(null));
		llmRequest.setSrvName(digest.path("srvName").asText(null));
		llmRequest.setServerContext(StringUtil.hasValue(serverContext) ? serverContext : null);
		llmRequest.setOrigin("Daily Summary Report - AI Analysis");
		String reportName = reportsDirRelativeName(reportFile);
		llmRequest.setReportName(reportName);

		// The real call: the same code as the statement advice (the feature/provider/model/login checks, progress, Stop, the LLM log)
		if ( ! preview )
		{
			llmRequest.setProvider(body.path("provider").asText(null));
			llmRequest.setModel   (body.path("model")   .asText(null));
			llmRequest.setProgress(body.path("progress").asBoolean(false));
			LlmOptimizeResponse answer = LlmSqlOptimizeServlet.runLlmCall(request, response, out, om, llmRequest);
			if (answer != null && answer.getAnalysis() != null) // a cut-off answer must not replace a good one (it is in the LLM log anyway)
				writeCache(om, reportName, llmRequest, answer, request.getRemoteUser());
			return;
		}

		llmRequest.setPreview(true);
		String prompt = LlmClientRegistry.getInstance().getProvider("claude").buildPrompt(llmRequest); // which provider does not matter: the prompt is built the same for all

		// The answer: the prompt + what is in the digest (for the page's list of sections) + the last analysis of this report
		ObjectNode result = om.createObjectNode();
		result.put("promptSent", prompt);
		result.set("cached", readCache(om, reportName));
		ObjectNode report = result.putObject("report");
		report.put("name",        reportName);
		report.put("url",         "/report?op=view&name=" + URLEncoder.encode(reportName, StandardCharsets.UTF_8));
		report.put("srvName",     digest.path("srvName")    .asText(""));
		report.put("dbxApp",      digest.path("dbxApp")     .asText(""));
		report.put("dbVendor",    StringUtil.nullToValue(llmRequest.getDbVendor(), "")); // for /api/llm/server-context
		report.put("periodBegin", digest.path("periodBegin").asText(""));
		report.put("periodEnd",   digest.path("periodEnd")  .asText(""));
		report.put("reportLevel", digest.path("reportLevel").asText(""));
		report.put("createdAt",   digest.path("createdAt")  .asText(""));
		ArrayNode sections = result.putArray("sections");
		for (JsonNode sect : digest.path("sections"))
		{
			ObjectNode s = sections.addObject();
			s.put("id",      sect.path("id")     .asText(""));
			s.put("subject", sect.path("subject").asText(""));
			s.put("level",   sect.path("level")  .asText(""));
			s.put("chars",   om.writeValueAsString(sect).length());
			s.put("cut",     sect.path("cut")    .asBoolean(false));
		}
		result.set("namesOnly", digest.path("namesOnly").isArray() ? digest.get("namesOnly") : om.createArrayNode());
		result.set("disabled", digest.path("disabled").isArray() ? digest.get("disabled") : om.createArrayNode());
		result.set("dropped",  digest.path("dropped") .isArray() ? digest.get("dropped")  : om.createArrayNode());
		result.put("digestChars", digestJson.length());
		result.put("hasServerContext", StringUtil.hasValue(serverContext));

		om.writeValue(out, result);
		out.flush();
		out.close();
	}

	//-------------------------------------------------------------------------
	// The cache: the LAST analysis of each report, shown at once when the page is opened.
	// Every run is also in the LLM log (LlmAdviceLog). Not in the reports dir: a file there is listed as a report
	// (/overview) and can be served by /report?op=viewLatest.
	//-------------------------------------------------------------------------

	public static final String CACHE_SUBDIR = "llm-dsr-analysis";

	public static File getCacheDir()
	{
		return new File(DbxTuneCentral.getAppDataDir(), CACHE_SUBDIR);
	}

	/** The cache file of a report ('tmp-dsr/x.html' -> 'tmp-dsr__x.html.json') */
	private static File getCacheFile(String reportName)
	{
		return new File(getCacheDir(), reportName.replace("/", "__") + ".json");
	}

	/** Save the analysis as the report's last one. Never throws: a problem here must not affect the answer. */
	private static void writeCache(ObjectMapper om, String reportName, LlmOptimizeRequest llmRequest, LlmOptimizeResponse answer, String user)
	{
		try
		{
			LlmClient     client = LlmClientRegistry.getInstance().getProvider(llmRequest.getProvider());
			LlmModelEntry model  = client == null ? null : client.getModelEntry(llmRequest.getModel());

			ObjectNode c = om.createObjectNode();
			c.put("time",            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
			c.put("user",            StringUtil.hasValue(user) ? user : "(anonymous)");
			c.put("providerId",      answer.getProviderId());
			c.put("providerName",    client == null ? answer.getProviderId() : client.getDisplayName());
			c.put("model",           answer.getModel());
			c.put("modelLabel",      model == null ? answer.getModel() : model.getLabel());
			c.put("inputTokens",     answer.getInputTokens());
			c.put("outputTokens",    answer.getOutputTokens());
			c.put("savedUrl",        answer.getSavedUrl());
			c.put("savedForDays",    answer.getSavedForDays());
			c.put("explanation",     answer.getExplanation());
			c.put("thinkingSummary", answer.getThinkingSummary());
			c.set("analysis",        answer.getAnalysis());

			File f = getCacheFile(reportName);
			f.getParentFile().mkdirs();
			om.writeValue(f, c);
		}
		catch (Exception ex)
		{
			_logger.warn("LlmDsrAnalysisServlet: problems saving the analysis of the report '" + reportName + "'. Continuing anyway.", ex);
		}
	}

	/** The report's last analysis, or null */
	private static JsonNode readCache(ObjectMapper om, String reportName)
	{
		File f = getCacheFile(reportName);
		if ( ! f.isFile() )
			return null;
		try
		{
			return om.readTree(f);
		}
		catch (Exception ex)
		{
			_logger.warn("LlmDsrAnalysisServlet: problems reading the cached analysis '" + f + "'. Ignoring it.", ex);
			return null;
		}
	}

	/**
	 * Remove the cached analyses whose report is gone (removed by hand, or by DailySummaryReport.remove.after.days).
	 * Called by the daily DataDirectoryCleaner.
	 *
	 * @return the number of files removed (or that would be, when dryRun)
	 */
	public static int removeOrphanCacheFiles(boolean dryRun)
	{
		File[] files = getCacheDir().listFiles(f -> f.isFile() && f.getName().endsWith(".json"));
		if (files == null)
			return 0;
		File reportsDir = new File(DbxTuneCentral.getAppReportsDir());
		int count = 0;
		for (File f : files)
		{
			String reportName = f.getName().substring(0, f.getName().length() - ".json".length()).replace("__", "/");
			if (new File(reportsDir, reportName).exists())
				continue;
			count++;
			if (dryRun)
				_logger.info("DSR-ANALYSIS: DRY-RUN: would remove the cached analysis '" + f + "' (the report is gone).");
			else if (f.delete())
				_logger.info("DSR-ANALYSIS: Removed the cached analysis '" + f + "' (the report is gone).");
			else
				_logger.warn("DSR-ANALYSIS: Could not remove the cached analysis '" + f + "'.");
		}
		return count;
	}

	/** The product name (as DbUtils.DB_PROD_NAME_*) of a recording app, for the prompt's vendor specifics. The app name itself for the others. */
	private static String productNameOf(String dbxApp)
	{
		if (AseTune      .APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_SYBASE_ASE;
		if (SqlServerTune.APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_MSSQL;
		if (PostgresTune .APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_POSTGRES;
		if (OracleTune   .APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_ORACLE;
		if (MySqlTune    .APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_MYSQL;
		if (Db2Tune      .APP_NAME.equals(dbxApp)) return DbUtils.DB_PROD_NAME_DB2_LUW;
		return StringUtil.hasValue(dbxApp) ? dbxApp : null;
	}

	/**
	 * The digest (JSON text) of a report file, or null when it has none. It is the last element of the report (just before
	 * {@code </body>}), written by DailySummaryReportDefault.createLlmDigestScript().
	 */
	private static String readDigest(File reportFile)
	throws IOException
	{
		String html  = Files.readString(reportFile.toPath(), StandardCharsets.UTF_8);
		String start = "id='" + DailySummaryReportDefault.LLM_DIGEST_ELEMENT_ID + "'>";
		int s = html.lastIndexOf(start);
		if (s < 0)
			return null;
		s += start.length();
		int e = html.indexOf("</script>", s);
		return e < 0 ? null : html.substring(s, e);
	}

	/**
	 * The report of a server for a day, when only those are known (the DSR's button: the file name is given when the report is
	 * saved, after it was written). The file name holds the time the report was SAVED ({srv}.{yyyy-MM-dd_HHmm}[.-NTR-|.-FYI-].html),
	 * normally just after midnight of the next day: so the files saved on the day or up to 2 days after are read, newest first,
	 * and the first one whose digest says the period begins on that day is it.
	 *
	 * @return {File, digest JSON}, or null when there is none
	 */
	private static Object[] findReportForPeriod(String srv, LocalDate day, ObjectMapper om)
	throws IOException
	{
		File dir = new File(DbxTuneCentral.getAppReportsDir());
		String prefix = FileUtils.toSafeFileName(srv) + "."; // the same as DailySummaryReportAbstract.save()
		File[] files = dir.listFiles(f -> f.isFile() && f.getName().startsWith(prefix) && f.getName().endsWith(".html"));
		if (files == null)
			return null;

		List<File> candidates = new ArrayList<>();
		for (File f : files)
		{
			String rest = f.getName().substring(prefix.length());
			if (rest.length() < 10)
				continue;
			try
			{
				LocalDate saved = LocalDate.parse(rest.substring(0, 10));
				if ( ! saved.isBefore(day) && ! saved.isAfter(day.plusDays(2)) )
					candidates.add(f);
			}
			catch (DateTimeParseException ex) { /* another server whose name starts with this one, for example 'srv.domain' */ }
		}
		candidates.sort((a, b) -> b.getName().compareTo(a.getName())); // newest first (the name holds the time)

		String dayStr = day.toString();
		for (File f : candidates)
		{
			String digestJson = readDigest(f);
			if (digestJson == null)
				continue;
			try
			{
				if (om.readTree(digestJson).path("periodBegin").asText("").startsWith(dayStr))
					return new Object[] { f, digestJson };
			}
			catch (Exception ex) { /* a broken digest: try the next */ }
		}
		return null;
	}

	/** The name of a report file relative to the reports dir (the 'name' of /report?op=view), with '/' */
	private static String reportsDirRelativeName(File reportFile)
	{
		try
		{
			File dir = new File(DbxTuneCentral.getAppReportsDir()).getCanonicalFile();
			return dir.toPath().relativize(reportFile.getCanonicalFile().toPath()).toString().replace('\\', '/');
		}
		catch (IOException ex)
		{
			return reportFile.getName();
		}
	}

	private static void writeError(ObjectMapper om, PrintWriter out, HttpServletResponse response, int statusCode, String error, String message)
	throws IOException
	{
		response.setStatus(statusCode);
		Map<String, String> err = new LinkedHashMap<>();
		err.put("error", error);
		err.put("message", message);
		om.writeValue(out, err);
		out.flush();
		out.close();
	}
}
