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
package com.dbxtune.central.llm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.controllers.DbxTimelineRows;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Saves every "LLM Optimization Advice" request (the real LLM calls made by {@code LlmSqlOptimizeServlet},
 * not the prompt previews) so an administrator can see who asked what, and what the answer was.
 * <ul>
 *   <li>Always: one INFO line in the DbxCentral log (prefix {@code LLM-ADVICE:}).</li>
 *   <li>When {@link #PROPKEY_save} is true: one HTML file per request in {@link #getLogDir()}.
 *       The file is plain and self-contained (inline CSS, no scripts, no external references), so it
 *       can be read anywhere and does not break when JavaScript libraries are upgraded. When it is
 *       opened through {@code /admin/llm-log?op=view}, the page is re-rendered with the same renderer
 *       as the advice dialog ({@code dbxLlmAdvice.renderSavedRequest()}), which reads the values back
 *       from the fixed element ids written here.</li>
 *   <li>The first lines of each file hold a small JSON block ({@link #META_ELEMENT_ID}) with the
 *       metadata, so {@link #list(int)} does not have to read the whole file.</li>
 *   <li>Files older than {@link #PROPKEY_keepDays} are removed by {@link #removeOldFiles(boolean)},
 *       called from the daily {@code DataDirectoryCleaner}.</li>
 * </ul>
 * The directory is under the data directory, which only the admin servlet serves (the files are
 * reachable only through {@code /admin/llm-log}, protected by the {@code /admin/*} constraint).
 */
public class LlmAdviceLog
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Save each request as an HTML file. */
	public static final String  PROPKEY_save        = "DbxCentral.llm.log.save";
	public static final boolean DEFAULT_save        = true;

	/** Remove saved files older than this many days. 0 or less = never remove. */
	public static final String  PROPKEY_keepDays    = "DbxCentral.llm.log.keep.days";
	public static final int     DEFAULT_keepDays    = 90;

	/** Tell the users (one line under the advice) that requests are saved. */
	public static final String  PROPKEY_notifyUsers = "DbxCentral.llm.log.notifyUsers";
	public static final boolean DEFAULT_notifyUsers = false;

	public static final String LOG_SUBDIR      = "llm-advice-log";
	public static final String META_ELEMENT_ID = "dbx-llm-log-meta";

	private static final String  META_START      = "<script type=\"application/json\" id=\"" + META_ELEMENT_ID + "\">";
	private static final int     META_READ_BYTES = 16 * 1024;
	private static final Pattern VALID_FILE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*\\.html$");

	public static boolean isSaveEnabled()   { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_save,        DEFAULT_save); }
	public static int     getKeepDays()     { return Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_keepDays,    DEFAULT_keepDays); }
	public static boolean isNotifyUsers()   { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_notifyUsers, DEFAULT_notifyUsers); }

	/** Number of days to tell the user requests are kept, or null when users should not be told (not saving, or notice turned off). */
	public static Integer getSavedForDays()
	{
		return (isSaveEnabled() && isNotifyUsers()) ? getKeepDays() : null;
	}

	public static File getLogDir()
	{
		return new File(DbxTuneCentral.getAppDataDir(), LOG_SUBDIR);
	}

	//-------------------------------------------------------------------------
	// Save
	//-------------------------------------------------------------------------

	/**
	 * Record one LLM call. Never throws: a problem here must not affect the user's request.
	 *
	 * @param req          the HTTP request (user, client address)
	 * @param llmRequest   what the browser asked for
	 * @param client       the provider that was called
	 * @param llmResponse  the answer, or null when the call failed
	 * @param ex           the failure, or null when the call succeeded
	 * @param promptSent   the prompt that was sent (or would have been sent), may be null
	 * @param durationMs   time spent in the provider call
	 */
	public static void save(HttpServletRequest req, LlmOptimizeRequest llmRequest, LlmClient client, LlmOptimizeResponse llmResponse, Exception ex, String promptSent, long durationMs)
	{
		try
		{
			Date   now      = new Date();
			String userName = req == null ? null : req.getRemoteUser();
			String status   = (ex == null && llmResponse != null) ? "OK" : "FAILED";

			Map<String, Object> meta = new LinkedHashMap<>();
			meta.put("time",         new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now));
			meta.put("user",         StringUtil.hasValue(userName) ? userName : "(anonymous)");
			meta.put("clientAddr",   req == null ? null : req.getRemoteAddr());
			meta.put("srvName",      llmRequest.getSrvName());
			meta.put("origin",       llmRequest.getOrigin());
			meta.put("dbVendor",     llmRequest.getDbVendor());
			meta.put("dbmsVersion",  llmRequest.getDbmsVersion());
			meta.put("providerId",   client == null ? null : client.getProviderId());
			meta.put("model",        llmResponse != null && llmResponse.getModel() != null ? llmResponse.getModel() : (client == null ? null : client.getModel()));
			meta.put("status",       status);
			meta.put("durationMs",   durationMs);
			meta.put("inputTokens",  llmResponse == null ? null : llmResponse.getInputTokens());
			meta.put("outputTokens", llmResponse == null ? null : llmResponse.getOutputTokens());
			meta.put("promptChars",  promptSent == null ? 0 : promptSent.length());
			meta.put("sqlPreview",   cut(oneLine(llmRequest.getSql()), 200));
			meta.put("errorMessage", ex == null ? null : cut(oneLine(ex.getMessage()), 300));

			String fileName = null;
			if (isSaveEnabled())
			{
				String html = createHtml(meta, llmRequest, llmResponse, ex, promptSent);
				fileName = writeFile(now, (String) meta.get("user"), llmRequest.getSrvName(), html);
			}

			_logger.info("LLM-ADVICE: user='" + meta.get("user") + "', from='" + meta.get("clientAddr") + "', srv='" + meta.get("srvName") + "', origin='" + meta.get("origin") + "'"
					+ ", provider='" + meta.get("providerId") + "', model='" + meta.get("model") + "', status=" + status + ", durationMs=" + durationMs
					+ ", tokens(in/out)=" + meta.get("inputTokens") + "/" + meta.get("outputTokens") + ", promptChars=" + meta.get("promptChars")
					+ (fileName == null ? ", file=(not saved)" : ", file='" + fileName + "'"));
		}
		catch (Throwable t)
		{
			_logger.warn("LLM-ADVICE: Problems saving the LLM Optimization Advice request. Continuing anyway.", t);
		}
	}

	/** At most maxLen chars (null stays null). */
	private static String cut(String str, int maxLen)
	{
		return (str == null || str.length() <= maxLen) ? str : str.substring(0, maxLen);
	}

	private static String oneLine(String str)
	{
		return str == null ? null : str.replaceAll("\\s+", " ").trim();
	}

	/** Keep only file-name friendly characters. */
	private static String safeNamePart(String str, int maxLen)
	{
		if (StringUtil.isNullOrBlank(str))
			return "unknown";
		String s = str.replaceAll("[^A-Za-z0-9.-]", "_");
		return s.length() > maxLen ? s.substring(0, maxLen) : s;
	}

	private static String writeFile(Date now, String user, String srvName, String html)
	throws IOException
	{
		File dir = getLogDir();
		if ( ! dir.exists() && ! dir.mkdirs() )
			throw new IOException("Could not create directory '" + dir + "'.");

		String base = new SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS").format(now) + "_" + safeNamePart(user, 40) + "_" + safeNamePart(srvName, 60);
		byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
		for (int i = 1; i < 100; i++)
		{
			String name = base + (i == 1 ? "" : "_" + i) + ".html";
			try
			{
				Files.write(new File(dir, name).toPath(), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
				return name;
			}
			catch (FileAlreadyExistsException ignore)
			{
				// Two requests in the same millisecond for the same user/server: try next suffix
			}
		}
		throw new IOException("Could not find a free file name for '" + base + "' in '" + dir + "'.");
	}

	private static String esc(Object obj)
	{
		return obj == null ? "" : StringEscapeUtils.escapeHtml4(obj.toString());
	}

	/** One {@code <pre>}. The newline after the start tag is dropped by the HTML parser, so {@code textContent} returns exactly {@code text}. */
	private static String pre(String id, String text)
	{
		return "<pre id=\"" + id + "\">\n" + esc(text) + "</pre>\n";
	}

	/**
	 * The saved page. The values that {@code dbxLlmAdvice.renderSavedRequest()} reads back live in fixed
	 * element ids: {@code dbx-llm-sql}, {@code dbx-llm-optimized-sql}, {@code dbx-llm-explanation},
	 * {@code dbx-llm-error}, {@code dbx-llm-prompt}, inside {@code dbx-llm-advice}.
	 */
	static String createHtml(Map<String, Object> meta, LlmOptimizeRequest llmRequest, LlmOptimizeResponse llmResponse, Exception ex, String promptSent)
	throws IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>\n");
		sb.append("<html lang=\"en\">\n");
		sb.append("<head>\n");
		sb.append("<meta charset=\"UTF-8\">\n");
		// Metadata for the Admin page list (LlmAdviceLog.list()) - keep it at the top of the file, on ONE line
		sb.append(META_START).append(DbxTimelineRows.toScriptJson(meta)).append("</script>\n");
		sb.append("<title>").append(esc("LLM Advice - " + meta.get("time") + " - " + meta.get("user") + " - " + StringUtil.nullToValue((String)meta.get("srvName"), "unknown"))).append("</title>\n");
		sb.append("<style>\n");
		sb.append("  body { font-family: Arial, Helvetica, sans-serif; margin: 16px; color: #212529; background: #fff; }\n");
		sb.append("  table.dbx-llm-meta { border-collapse: collapse; margin-bottom: 12px; }\n");
		sb.append("  table.dbx-llm-meta td { border: 1px solid #ccc; padding: 2px 8px; vertical-align: top; }\n");
		sb.append("  table.dbx-llm-meta td:first-child { font-weight: bold; white-space: nowrap; }\n");
		sb.append("  pre, .dbx-llm-text { white-space: pre-wrap; word-break: break-word; background: #f6f8fa; border: 1px solid #ddd; padding: 8px; font-size: 0.9em; }\n");
		sb.append("  .dbx-llm-text { font-family: Arial, Helvetica, sans-serif; }\n");
		sb.append("  .dbx-llm-failed { color: #b02a37; font-weight: bold; }\n");
		sb.append("  details { margin-top: 12px; } summary { cursor: pointer; }\n");
		// Dark: follows the OS, or the DbxCentral Light/Dark choice when viewed through DbxCentral (dbxTheme.js sets <html data-theme>)
		sb.append("  @media (prefers-color-scheme: dark) {\n");
		sb.append("    html:not([data-theme=light]) body { color: #dee2e6; background: #212529; }\n");
		sb.append("    html:not([data-theme=light]) pre, html:not([data-theme=light]) .dbx-llm-text { background: #2b3035; border-color: #495057; }\n");
		sb.append("    html:not([data-theme=light]) table.dbx-llm-meta td { border-color: #495057; }\n");
		sb.append("  }\n");
		sb.append("  html[data-theme=dark] body { color: #dee2e6; background: #212529; }\n");
		sb.append("  html[data-theme=dark] pre, html[data-theme=dark] .dbx-llm-text { background: #2b3035; border-color: #495057; }\n");
		sb.append("  html[data-theme=dark] table.dbx-llm-meta td { border-color: #495057; }\n");
		sb.append("</style>\n");
		sb.append("</head>\n");
		sb.append("<body>\n");
		sb.append("<h3>LLM Optimization Advice</h3>\n");

		sb.append("<table class=\"dbx-llm-meta\">\n");
		String[][] rows = {
			{ "Time",          "time"         },
			{ "User",          "user"         },
			{ "Client Address","clientAddr"   },
			{ "Server",        "srvName"      },
			{ "Origin",        "origin"       },
			{ "DBMS Vendor",   "dbVendor"     },
			{ "DBMS Version",  "dbmsVersion"  },
			{ "Provider",      "providerId"   },
			{ "Model",         "model"        },
			{ "Status",        "status"       },
			{ "Duration (ms)", "durationMs"   },
			{ "Input Tokens",  "inputTokens"  },
			{ "Output Tokens", "outputTokens" },
		};
		for (String[] row : rows)
		{
			Object val = meta.get(row[1]);
			if (val != null)
				sb.append("<tr><td>").append(esc(row[0])).append("</td><td>").append(esc(val)).append("</td></tr>\n");
		}
		sb.append("</table>\n");

		sb.append("<div id=\"dbx-llm-advice\">\n");
		sb.append("<details>\n<summary><b>SQL Text</b></summary>\n");
		sb.append(pre("dbx-llm-sql", llmRequest.getSql()));
		sb.append("</details>\n");

		// The plan as the browser sent it (the prompt may hold a shrunk version, see SqlServerPlanXmlShrinker)
		if (StringUtil.hasValue(llmRequest.getPlan()))
		{
			sb.append("<details>\n<summary><b>Execution Plan</b></summary>\n");
			sb.append(pre("dbx-llm-plan", llmRequest.getPlan()));
			sb.append("</details>\n");
		}
		if (StringUtil.hasValue(llmRequest.getWorkloadProfile()))
		{
			sb.append("<details>\n<summary><b>Workload Profile</b></summary>\n");
			sb.append(pre("dbx-llm-workload", llmRequest.getWorkloadProfile()));
			sb.append("</details>\n");
		}

		if (llmResponse != null)
		{
			// Same layout as the advice dialog: everything folded, except the Explanation
			sb.append("<details>\n<summary><b>Suggested SQL</b></summary>\n");
			if (StringUtil.hasValue(llmResponse.getOptimizedSql()))
				sb.append(pre("dbx-llm-optimized-sql", llmResponse.getOptimizedSql()));
			else
				sb.append("<p>No SQL changes suggested.</p>\n");
			sb.append("</details>\n");

			if (StringUtil.hasValue(llmResponse.getExplanation()))
			{
				sb.append("<details open>\n<summary><b>Explanation</b></summary>\n");
				sb.append("<div id=\"dbx-llm-explanation\" class=\"dbx-llm-text\">").append(esc(llmResponse.getExplanation())).append("</div>\n");
				sb.append("</details>\n");
			}
		}
		if (ex != null)
		{
			sb.append("<h4 class=\"dbx-llm-failed\">The LLM call failed</h4>\n");
			sb.append(pre("dbx-llm-error", ex.getMessage()));
		}

		if (promptSent != null)
		{
			sb.append("<details>\n");
			sb.append("<summary><b>Prompt sent to the LLM (").append(promptSent.length()).append(" chars)</b></summary>\n");
			sb.append(pre("dbx-llm-prompt", promptSent));
			sb.append("</details>\n");
		}
		sb.append("</div>\n");
		sb.append("</body>\n");
		sb.append("</html>\n");
		return sb.toString();
	}

	//-------------------------------------------------------------------------
	// Read back (Admin page)
	//-------------------------------------------------------------------------

	/**
	 * Saved requests, newest first.
	 * @param days only files written during the last N days; 0 or less = all
	 * @return one map per file: the metadata saved in the file, plus 'name' and 'sizeBytes'
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> list(int days)
	{
		List<Map<String, Object>> result = new ArrayList<>();

		File[] files = getLogDir().listFiles((d, n) -> VALID_FILE_NAME.matcher(n).matches());
		if (files == null)
			return result;

		Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName())); // names starts with the time stamp
		long cutoff = days > 0 ? System.currentTimeMillis() - days * 24L * 3600 * 1000 : 0;

		ObjectMapper om = new ObjectMapper();
		for (File f : files)
		{
			if (f.lastModified() < cutoff)
				continue;

			Map<String, Object> entry = new LinkedHashMap<>();
			try
			{
				String json = readMetaJson(f);
				if (json != null)
					entry.putAll(om.readValue(json, Map.class));
			}
			catch (Exception ex)
			{
				_logger.warn("LLM-ADVICE: Problems reading the metadata of '" + f + "'. Caught: " + ex);
			}
			entry.put("name",      f.getName());
			entry.put("sizeBytes", f.length());
			result.add(entry);
		}
		return result;
	}

	/** The metadata JSON from the top of a saved file, or null when not found. */
	private static String readMetaJson(File f)
	throws IOException
	{
		byte[] buf;
		try (InputStream in = Files.newInputStream(f.toPath()))
		{
			buf = in.readNBytes(META_READ_BYTES);
		}
		String head  = new String(buf, StandardCharsets.UTF_8);
		int    start = head.indexOf(META_START);
		if (start < 0)
			return null;
		start += META_START.length();
		int end = head.indexOf("</script>", start);
		if (end < 0)
			return null;
		return head.substring(start, end);
	}

	/**
	 * A saved file by name, or null if the name is not a plain file name in the log directory
	 * (no path parts) or the file does not exist.
	 */
	public static File getFile(String name)
	{
		if (StringUtil.isNullOrBlank(name) || ! VALID_FILE_NAME.matcher(name).matches())
			return null;

		File f = new File(getLogDir(), name);
		return f.isFile() ? f : null;
	}

	//-------------------------------------------------------------------------
	// Cleanup
	//-------------------------------------------------------------------------

	/**
	 * Remove saved files older than {@link #PROPKEY_keepDays}.
	 * @return number of removed files (or files that would be removed in dry-run)
	 */
	public static int removeOldFiles(boolean dryRun)
	{
		int keepDays = getKeepDays();
		if (keepDays <= 0)
		{
			_logger.info("LLM-ADVICE: Not removing any saved LLM Advice requests, since '" + PROPKEY_keepDays + "' is " + keepDays + ".");
			return 0;
		}

		File[] files = getLogDir().listFiles((d, n) -> VALID_FILE_NAME.matcher(n).matches());
		if (files == null)
			return 0;

		long cutoff = System.currentTimeMillis() - keepDays * 24L * 3600 * 1000;
		int count = 0;
		for (File f : files)
		{
			if (f.lastModified() >= cutoff)
				continue;

			if (dryRun)
			{
				_logger.info("LLM-ADVICE: DRY-RUN: Would remove '" + f + "'.");
				count++;
			}
			else if (f.delete())
				count++;
			else
				_logger.warn("LLM-ADVICE: Could not remove '" + f + "'.");
		}
		_logger.info("LLM-ADVICE: " + (dryRun ? "DRY-RUN: Would have removed " : "Removed ") + count + " saved LLM Advice requests older than " + keepDays + " days from '" + getLogDir() + "'. (" + files.length + " files before cleanup)");
		return count;
	}
}
