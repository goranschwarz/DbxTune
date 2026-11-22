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
package com.dbxtune.cloud;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST client for Google Cloud Logging v2 (<code>https://logging.googleapis.com/v2/entries:list</code>)
 * to tail and parse Cloud SQL for PostgreSQL error logs (<code>cloudsql.googleapis.com/postgres.log</code>)
 * into the 28-column schema of Postgres <code>CmErrorLog</code>.
 */
public class GcpLoggingClient
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String LOGGING_ENTRIES_URL = "https://logging.googleapis.com/v2/entries:list";

	/**
	 * Matches Cloud SQL Postgres fixed log_line_prefix:
	 * <code>2026-11-18 14:22:01.123 UTC [12345]: [1-1] db=mydb,user=postgres ERROR:  ...</code>
	 * Also handles optional extra key=value pairs (e.g. app=...,client=...) and optional prefix without timestamp.
	 */
	private static final Pattern CLOUDSQL_PREFIX_PATTERN = Pattern.compile(
		"^(?:(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)\\s+[A-Z0-9+-]+\\s+)?"
		+ "\\[(\\d+)\\]:\\s*\\[(\\d+)-(\\d+)\\]\\s*"
		+ "(?:db=([^,\\s]*),user=([^,\\s]*)(?:,app=([^,\\s]*))?(?:,client=([^,\\s]*))?\\s*)?"
		+ "(DEBUG[1-5]?|INFO|NOTICE|WARNING|ERROR|LOG|FATAL|PANIC|DETAIL|HINT|CONTEXT|STATEMENT|QUERY|LOCATION):\\s*(.*)$",
		Pattern.DOTALL
	);

	/**
	 * Fallback pattern when a line starts directly with SEVERITY: message (without [pid]: [line-1])
	 */
	private static final Pattern SIMPLE_SEVERITY_PATTERN = Pattern.compile(
		"^(DEBUG[1-5]?|INFO|NOTICE|WARNING|ERROR|LOG|FATAL|PANIC|DETAIL|HINT|CONTEXT|STATEMENT|QUERY|LOCATION):\\s*(.*)$",
		Pattern.DOTALL
	);

	/**
	 * When <code>log_error_verbosity = verbose</code>, Postgres prefixes the message with the 5-char SQLSTATE:
	 * <code>ERROR:  23505: duplicate key value violates unique constraint ...</code>
	 */
	private static final Pattern VERBOSE_SQLSTATE_PATTERN = Pattern.compile(
		"^([0-9A-Z]{5}):\\s+(.*)$",
		Pattern.DOTALL
	);

	private final CloudConfig          _config;
	private final GcpCredentialsHelper _credentialsHelper;
	private final HttpClient           _httpClient;

	/**
	 * Cloud Logging delivers entries late and out of order (an entry can arrive after a newer one).
	 * So we read on <code>receiveTimestamp</code> (when Cloud Logging got it), from a bit before the newest one we have seen,
	 * and remove what was already read by its <code>insertId</code>.
	 */
	private static final long RECEIVE_OVERLAP_SEC   = 30;
	/** How late (in log time) an entry may arrive and still be read. Only to keep the query cheap. */
	private static final long MAX_LATE_ARRIVAL_SEC  = 3600;
	private static final int  MAX_PAGES_PER_POLL    = 20;
	private static final int  MAX_REMEMBERED_IDS    = 50_000;

	private       Instant              _watermarkInstant;   // the newest receiveTimestamp we have read
	private final Set<String>          _recentInsertIds = new LinkedHashSet<>();
	private       long                 _syntheticRowId  = 0L;

	public GcpLoggingClient(CloudConfig config, GcpCredentialsHelper credentialsHelper)
	{
		_config            = config;
		_credentialsHelper = credentialsHelper;
		_httpClient        = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		// Start tailing from now (minus 60 seconds to catch recent startup/connection events on initial connect)
		_watermarkInstant  = Instant.now().minusSeconds(60);
	}

	/**
	 * Test connectivity and permissions to GCP Cloud Logging for this Cloud SQL instance.
	 */
	public String testConnection()
	throws IOException
	{
		String token = _credentialsHelper.getAccessToken();
		String filter = buildFilter(Instant.now().minusSeconds(3600));

		ObjectNode reqJson = MAPPER.createObjectNode();
		ArrayNode resNames = reqJson.putArray("resourceNames");
		resNames.add("projects/" + _config.getProjectId());
		reqJson.put("filter", filter);
		reqJson.put("orderBy", "timestamp desc");
		reqJson.put("pageSize", 1);

		HttpRequest req = HttpRequest.newBuilder()
			.uri(URI.create(LOGGING_ENTRIES_URL))
			.timeout(Duration.ofSeconds(15))
			.header("Authorization", "Bearer " + token)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(reqJson)))
			.build();

		try
		{
			HttpResponse<String> resp = _httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200)
			{
				throw new IOException("GCP Cloud Logging returned HTTP " + resp.statusCode() + ": " + resp.body());
			}
			JsonNode root = MAPPER.readTree(resp.body());
			JsonNode entries = root.path("entries");
			int count = entries.isArray() ? entries.size() : 0;
			return "Connected to GCP Cloud Logging ('cloudsql.googleapis.com/postgres.log') for '"
				+ _config.getGcpDatabaseId() + "' (verified read access; recent entries in last hour: "
				+ (count > 0 ? "yes" : "0") + ").";
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while testing GCP Cloud Logging connection", ex);
		}
	}

	/**
	 * Fetch and parse new Postgres error-log entries since the last poll watermark.
	 */
	public synchronized List<CloudLogEntry> fetchNewLogEntries()
	throws IOException
	{
		String token   = _credentialsHelper.getAccessToken();
		int    maxRows = _config.getMaxLogRowsPerSample();

		// On receiveTimestamp (late entries are read too), with an overlap that the insertId check removes
		Instant prevWatermark = _watermarkInstant;
		Instant since         = prevWatermark.minusSeconds(RECEIVE_OVERLAP_SEC);
		String  filter        = buildFilter(since.minusSeconds(MAX_LATE_ARRIVAL_SEC)) + " AND receiveTimestamp>=\"" + since + "\"";

		List<CloudLogEntry> result = new ArrayList<>();
		String pageToken = null;
		int    pages     = 0;
		do
		{
			ObjectNode reqJson = MAPPER.createObjectNode();
			ArrayNode resNames = reqJson.putArray("resourceNames");
			resNames.add("projects/" + _config.getProjectId());
			reqJson.put("filter", filter);
			reqJson.put("orderBy", "timestamp asc");
			reqJson.put("pageSize", 1000);
			if (StringUtil.hasValue(pageToken))
				reqJson.put("pageToken", pageToken);

			HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(LOGGING_ENTRIES_URL))
				.timeout(Duration.ofSeconds(15))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(reqJson)))
				.build();

			HttpResponse<String> resp;
			try
			{
				resp = _httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				_watermarkInstant = prevWatermark; // read it all again next time (the insertId check removes what was already read)
				throw new IOException("Interrupted while polling GCP Cloud Logging", ex);
			}
			if (resp.statusCode() != 200)
			{
				_watermarkInstant = prevWatermark;
				throw new IOException("GCP Cloud Logging returned HTTP " + resp.statusCode() + ": " + resp.body());
			}

			result.addAll(parseEntriesListResponse(resp.body()));
			pageToken = MAPPER.readTree(resp.body()).path("nextPageToken").asText("");
			pages++;
		}
		while (StringUtil.hasValue(pageToken) && pages < MAX_PAGES_PER_POLL && result.size() < maxRows);

		// Not all was read: the entries are sorted on log time, not on receive time, so moving the watermark could skip some.
		// Keep it, and read the rest next time (what we already have is removed by its insertId).
		if (StringUtil.hasValue(pageToken))
		{
			_watermarkInstant = prevWatermark;
			_logger.info("GCP Cloud Logging: more entries than read in one sample (rows=" + result.size() + ", pages=" + pages + ", maxRows=" + maxRows + "). The rest is read at the next sample.");
		}

		return result;
	}

	/** The newest receiveTimestamp read so far (package-private: for the test) */
	Instant getWatermark()
	{
		return _watermarkInstant;
	}

	private String buildFilter(Instant sinceInstant)
	{
		String projectId  = _config.getProjectId();
		String databaseId = _config.getGcpDatabaseId();
		String logName    = "projects/" + projectId + "/logs/" + URLEncoder.encode("cloudsql.googleapis.com/postgres.log", StandardCharsets.UTF_8);

		StringBuilder sb = new StringBuilder();
		sb.append("resource.type=\"cloudsql_database\"");
		sb.append(" AND resource.labels.database_id=\"").append(databaseId).append("\"");
		sb.append(" AND logName=\"").append(logName).append("\"");
		if (sinceInstant != null)
		{
			sb.append(" AND timestamp>=\"").append(sinceInstant.toString()).append("\"");
		}
		if (StringUtil.hasValue(_config.getLogFilter()))
		{
			sb.append(" AND (").append(_config.getLogFilter()).append(")");
		}
		return sb.toString();
	}

	/**
	 * Parse a JSON response from <code>entries:list</code> into a list of {@link CloudLogEntry} objects,
	 * merging multi-line or consecutive DETAIL / HINT / CONTEXT / STATEMENT / LOCATION entries into their parent entry.
	 * Package-private for unit testing.
	 */
	public List<CloudLogEntry> parseEntriesListResponse(String jsonResponse)
	throws IOException
	{
		List<CloudLogEntry> result = new ArrayList<>();
		if (StringUtil.isNullOrBlank(jsonResponse))
			return result;

		JsonNode root = MAPPER.readTree(jsonResponse);
		JsonNode entriesNode = root.path("entries");
		if (!entriesNode.isArray() || entriesNode.isEmpty())
			return result;

		CloudLogEntry lastPrimaryEntry = null;

		for (JsonNode item : entriesNode)
		{
			// The watermark is the newest receiveTimestamp (when Cloud Logging got the entry): also for entries we already have
			String receiveIso = item.path("receiveTimestamp").asText("");
			if (StringUtil.hasValue(receiveIso))
			{
				try
				{
					Instant received = Instant.parse(receiveIso);
					if (_watermarkInstant == null || received.isAfter(_watermarkInstant))
						_watermarkInstant = received;
				}
				catch (Exception ignore) {}
			}

			// Already read (the overlap of the previous read)
			String insertId = item.path("insertId").asText("");
			if (StringUtil.hasValue(insertId) && _recentInsertIds.contains(insertId))
				continue;

			String timestampIso = item.path("timestamp").asText("");
			if (StringUtil.isNullOrBlank(timestampIso))
				timestampIso = receiveIso;

			Timestamp logTime = null;
			if (StringUtil.hasValue(timestampIso))
			{
				try { logTime = Timestamp.from(Instant.parse(timestampIso)); }
				catch (Exception ignore) {}
			}
			if (logTime == null)
				logTime = new Timestamp(System.currentTimeMillis());

			if (StringUtil.hasValue(insertId))
			{
				_recentInsertIds.add(insertId);
				if (_recentInsertIds.size() > MAX_REMEMBERED_IDS)
				{
					// Evict oldest insertId
					String oldest = _recentInsertIds.iterator().next();
					_recentInsertIds.remove(oldest);
				}
			}

			String cloudSeverity = item.path("severity").asText("INFO");
			String textPayload   = item.path("textPayload").asText("");
			if (StringUtil.isNullOrBlank(textPayload) && item.has("jsonPayload"))
			{
				JsonNode jp = item.get("jsonPayload");
				textPayload = jp.path("message").asText(jp.toString());
			}

			CloudLogEntry parsed = parseSingleLogPayload(textPayload, logTime, cloudSeverity, insertId);
			if (parsed == null)
				continue;

			// If this entry is a standalone continuation record (DETAIL, HINT, CONTEXT, STATEMENT, QUERY, LOCATION)
			// from the same process_id right after lastPrimaryEntry, attach it to lastPrimaryEntry!
			if (isContinuationSeverity(parsed.getErrorSeverity()) && lastPrimaryEntry != null
				&& sameProcess(lastPrimaryEntry.getProcessId(), parsed.getProcessId()))
			{
				attachContinuationField(lastPrimaryEntry, parsed.getErrorSeverity(), parsed.getMessage());
			}
			else
			{
				_syntheticRowId++;
				parsed.setRowId(_syntheticRowId);
				parsed.setLogfile("cloudsql.googleapis.com/postgres.log");
				if (StringUtil.isNullOrBlank(parsed.getSqlStateCode()))
				{
					parsed.setSqlStateCode(inferSqlState(parsed.getErrorSeverity(), parsed.getMessage()));
				}
				result.add(parsed);
				lastPrimaryEntry = parsed;
			}
		}

		return result;
	}

	/**
	 * Parse a single <code>textPayload</code> (which may itself contain embedded newlines with
	 * <code>DETAIL:</code>, <code>HINT:</code>, <code>CONTEXT:</code>, <code>STATEMENT:</code>, or <code>plan:</code>).
	 */
	public static CloudLogEntry parseSingleLogPayload(String textPayload, Timestamp fallbackLogTime, String fallbackSeverity, String insertId)
	{
		if (StringUtil.isNullOrBlank(textPayload))
			return null;

		CloudLogEntry entry = new CloudLogEntry();
		entry.setInsertId(insertId);
		entry.setLogTime(fallbackLogTime);

		String trimmed = textPayload.stripTrailing();

		Matcher m = CLOUDSQL_PREFIX_PATTERN.matcher(trimmed);
		String body;
		if (m.matches())
		{
			String pidStr     = m.group(2);
			String lineNumStr = m.group(3);
			String dbName     = m.group(5);
			String userName   = m.group(6);
			String appName    = m.group(7);
			String clientHost = m.group(8);
			String severity   = m.group(9);
			body              = m.group(10);

			if (StringUtil.hasValue(pidStr))
			{
				Integer pid = StringUtil.parseInt(pidStr, 0);
				entry.setProcessId(pid);
				entry.setSessionId("cloudsql-" + pidStr);
			}
			if (StringUtil.hasValue(lineNumStr))
			{
				entry.setSessionLineNum(StringUtil.parseLong(lineNumStr, 1L));
			}
			if (StringUtil.hasValue(dbName) && !"[unknown]".equals(dbName))
				entry.setDatabaseName(dbName);
			if (StringUtil.hasValue(userName) && !"[unknown]".equals(userName))
				entry.setUserName(userName);
			if (StringUtil.hasValue(appName) && !"[unknown]".equals(appName))
				entry.setApplicationName(appName);
			if (StringUtil.hasValue(clientHost) && !"[unknown]".equals(clientHost))
				entry.setConnectionFrom(clientHost);

			entry.setErrorSeverity( severity );
		}
		else
		{
			Matcher sm = SIMPLE_SEVERITY_PATTERN.matcher(trimmed);
			if (sm.matches())
			{
				entry.setErrorSeverity(sm.group(1));
				body = sm.group(2);
			}
			else
			{
				entry.setErrorSeverity(normalizeCloudSeverity(fallbackSeverity));
				body = trimmed;
			}
		}

		// Check if body starts with a verbose 5-character SQLSTATE code (e.g. "23505: duplicate key...")
		Matcher vm = VERBOSE_SQLSTATE_PATTERN.matcher(body);
		if (vm.matches())
		{
			entry.setSqlStateCode(vm.group(1));
			body = vm.group(2);
		}

		// Split multi-line body if it contains embedded DETAIL: / HINT: / CONTEXT: / STATEMENT: / QUERY: / LOCATION: lines
		splitEmbeddedSections(entry, body);

		return entry;
	}

	/**
	 * Splits a multi-line message body that may contain embedded PostgreSQL secondary lines
	 * (<code>DETAIL:</code>, <code>HINT:</code>, <code>CONTEXT:</code>, <code>STATEMENT:</code>, <code>QUERY:</code>, <code>LOCATION:</code>)
	 * while keeping multi-line <code>auto_explain</code> plans intact inside <code>message</code>.
	 */
	private static void splitEmbeddedSections(CloudLogEntry entry, String body)
	{
		if (body == null)
		{
			entry.setMessage("");
			return;
		}

		// Fast path: single-line message
		if (body.indexOf('\n') < 0)
		{
			entry.setMessage(body.trim());
			return;
		}

		String[] lines = body.split("\\r?\\n");
		StringBuilder currentBuf = new StringBuilder();
		String currentSection = "MESSAGE";
		boolean insideAutoExplainPlan = body.startsWith("duration:") && body.contains("plan:");

		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];

			// Strip optional repeated Cloud SQL prefix on secondary lines
			String cleanLine = line;
			Matcher pm = CLOUDSQL_PREFIX_PATTERN.matcher(line);
			String sectionKeyword = null;
			String sectionRest    = null;

			if (pm.matches() && isContinuationSeverity(pm.group(9)))
			{
				sectionKeyword = pm.group(9);
				sectionRest    = pm.group(10);
			}
			else
			{
				String ltrim = line.stripLeading();
				Matcher sm = SIMPLE_SEVERITY_PATTERN.matcher(ltrim);
				if (sm.matches() && isContinuationSeverity(sm.group(1)))
				{
					sectionKeyword = sm.group(1);
					sectionRest    = sm.group(2);
				}
			}

			// Do not split inside an auto_explain plan block unless it is a genuine STATEMENT:/CONTEXT:/LOCATION: footer
			if (sectionKeyword != null && (!insideAutoExplainPlan || "STATEMENT".equals(sectionKeyword) || "CONTEXT".equals(sectionKeyword) || "LOCATION".equals(sectionKeyword)))
			{
				saveSection(entry, currentSection, currentBuf.toString().stripTrailing());
				currentBuf.setLength(0);
				currentSection = sectionKeyword;
				currentBuf.append(sectionRest != null ? sectionRest : "");
			}
			else
			{
				if (currentBuf.length() > 0)
					currentBuf.append("\n");
				currentBuf.append(cleanLine);
			}
		}

		saveSection(entry, currentSection, currentBuf.toString().stripTrailing());
	}

	private static void saveSection(CloudLogEntry entry, String section, String content)
	{
		if ("MESSAGE".equals(section))
			entry.setMessage(content);
		else
			attachContinuationField(entry, section, content);
	}

	private static void attachContinuationField(CloudLogEntry entry, String section, String content)
	{
		if (StringUtil.isNullOrBlank(content))
			return;

		switch (section.toUpperCase())
		{
			case "DETAIL":
				entry.setDetail(appendText(entry.getDetail(), content));
				break;
			case "HINT":
				entry.setHint(appendText(entry.getHint(), content));
				break;
			case "CONTEXT":
				entry.setContext(appendText(entry.getContext(), content));
				break;
			case "STATEMENT":
				entry.setQuery(appendText(entry.getQuery(), content));
				break;
			case "QUERY":
				entry.setInternalQuery(appendText(entry.getInternalQuery(), content));
				break;
			case "LOCATION":
				entry.setLocation(appendText(entry.getLocation(), content));
				break;
		}
	}

	private static String appendText(String existing, String add)
	{
		if (StringUtil.isNullOrBlank(existing))
			return add;
		return existing + "\n" + add;
	}

	private static boolean isContinuationSeverity(String sev)
	{
		if (sev == null)
			return false;
		switch (sev.toUpperCase())
		{
			case "DETAIL":
			case "HINT":
			case "CONTEXT":
			case "STATEMENT":
			case "QUERY":
			case "LOCATION":
				return true;
			default:
				return false;
		}
	}

	private static boolean sameProcess(Integer pid1, Integer pid2)
	{
		if (pid1 == null && pid2 == null)
			return true;
		if (pid1 != null && pid2 != null)
			return pid1.equals(pid2);
		return false;
	}

	private static String normalizeCloudSeverity(String cloudSev)
	{
		if (StringUtil.isNullOrBlank(cloudSev))
			return "LOG";
		switch (cloudSev.toUpperCase())
		{
			case "EMERGENCY":
			case "ALERT":
				return "PANIC";
			case "CRITICAL":
				return "FATAL";
			case "ERROR":
				return "ERROR";
			case "WARNING":
				return "WARNING";
			case "NOTICE":
				return "NOTICE";
			case "DEBUG":
				return "DEBUG1";
			case "INFO":
			case "DEFAULT":
			default:
				return "LOG";
		}
	}

	/**
	 * Best-effort SQLSTATE inference when <code>log_error_verbosity</code> is <code>default</code>
	 * instead of <code>verbose</code>.
	 */
	public static String inferSqlState(String severity, String message)
	{
		if (StringUtil.isNullOrBlank(message))
			return "00000";

		String msg = message.toLowerCase();

		// Integrity Constraint Violations (Class 23)
		if (msg.contains("duplicate key value violates unique constraint")) return "23505";
		if (msg.contains("violates foreign key constraint"))                return "23503";
		if (msg.contains("violates not-null constraint"))                   return "23502";
		if (msg.contains("violates check constraint"))                      return "23514";
		if (msg.contains("violates exclusion constraint"))                  return "23P01";

		// Transaction Rollback / Deadlock / Serialization (Class 40)
		if (msg.contains("deadlock detected"))                              return "40P01";
		if (msg.contains("could not serialize access"))                     return "40001";

		// Operator Intervention / Timeouts / Shutdown (Class 57 & 55 & 25)
		if (msg.contains("canceling statement due to statement timeout"))   return "57014";
		if (msg.contains("canceling statement due to user request"))        return "57014";
		if (msg.contains("canceling statement due to lock timeout"))        return "55P03";
		if (msg.contains("could not obtain lock on row") || msg.contains("could not obtain lock on relation")) return "55P03";
		if (msg.contains("idle-in-transaction timeout"))                    return "25P03";
		if (msg.contains("idle_session_timeout"))                           return "57P05";
		if (msg.contains("terminating connection due to administrator command")) return "57P01";
		if (msg.contains("the database system is starting up") || msg.contains("the database system is in recovery mode") || msg.contains("the database system is shutting down")) return "57P03";

		// Insufficient Resources (Class 53)
		if (msg.contains("out of memory"))                                  return "53200";
		if (msg.contains("too many clients already") || msg.contains("remaining connection slots are reserved")) return "53300";
		if (msg.contains("no space left on device") || msg.contains("could not extend file")) return "53100";
		if (msg.contains("temporary file size exceeds temp_file_limit"))    return "53400";

		// Invalid Authorization (Class 28)
		if (msg.contains("password authentication failed") || msg.contains("no pg_hba.conf entry")) return "28P01";
		if (msg.contains("role \"") && msg.contains("\" does not exist"))   return "28000";

		// Syntax Error or Access Rule Violation (Class 42)
		if (msg.contains("permission denied for"))                          return "42501";
		if (msg.contains("syntax error at or near"))                        return "42601";
		if (msg.contains("relation \"") && msg.contains("\" does not exist")) return "42P01";
		if (msg.contains("column \"") && msg.contains("\" does not exist")) return "42703";
		if (msg.contains("function ") && msg.contains(" does not exist"))   return "42883";
		if (msg.contains("already exists"))                                 return "42P07";

		// Data Exception (Class 22)
		if (msg.contains("division by zero"))                               return "22012";
		if (msg.contains("invalid input syntax for"))                       return "22P02";
		if (msg.contains("value too long for type"))                        return "22001";
		if (msg.contains("numeric field overflow"))                         return "22003";

		// Normal LOG/INFO/NOTICE/WARNING fallback
		if ("ERROR".equalsIgnoreCase(severity) || "FATAL".equalsIgnoreCase(severity) || "PANIC".equalsIgnoreCase(severity))
			return "XX000";

		return "00000";
	}
}
