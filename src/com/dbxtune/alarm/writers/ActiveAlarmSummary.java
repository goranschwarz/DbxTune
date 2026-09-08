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
package com.dbxtune.alarm.writers;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.ICounterController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds a "what is still active" summary of Alarms, intended to be appended to the Alarm messages
 * that an AlarmWriter sends (Teams, Slack, Mail, ...).
 * <p>
 * The summary is a <b>merge</b> of two sources:
 * <ul>
 *   <li><b>local</b>   - {@link AlarmHandler#getAlarmList()}, this Collectors own alarms. Real time.</li>
 *   <li><b>central</b> - {@code GET <dbxCentralUrl>/api/alarm/active}, all <i>other</i> servers.</li>
 * </ul>
 *
 * <h2>Why a merge, and not simply "whatever DbxCentral says"</h2>
 * A Collector calls its AlarmWriters <b>synchronously</b> when an alarm is raised or cancelled, but the
 * alarm only reaches DbxCentral on the <i>next</i> {@code PersistWriterToDbxCentral.saveSample()} POST,
 * and then it still has to pass the Central PCS queue before it lands in the H2 database.
 * <p>
 * So at the moment the message is composed, DbxCentral is up to one sample-interval (plus queue depth)
 * behind. Using the Central list as-is produces messages that contradict themselves:
 * <ul>
 *   <li>a <b>RAISE</b> message where the summary does <b>not</b> list the alarm just raised</li>
 *   <li>a <b>CANCEL</b> message where the summary <b>still</b> lists the alarm just cancelled</li>
 * </ul>
 * Every alarm this Collector persists ends up in <b>one</b> schema in the Central database - its own
 * server name. Which means: the Central rows for "my" server are exactly the rows we already know
 * better ourselves. Hence we drop those, and substitute the local, real time list.
 * <p>
 * Note that alarms belonging to <i>other</i> servers are still only as fresh as their last delivery to
 * DbxCentral. That is unavoidable, and the caller is expected to say so in the rendered message rather
 * than pretend the data is live.
 *
 * <h2>Ordering inside AlarmHandler that this relies on</h2>
 * <ul>
 *   <li>RAISE    - the event is added to the active container <b>before</b> the writers are called, so it's already in the local list.</li>
 *   <li>RE-RAISE - same.</li>
 *   <li>CANCEL   - the writers are called <b>before</b> the event is removed, so it is still in the local
 *                  list, and we have to exclude it explicitly by alarmId.</li>
 * </ul>
 *
 * @author Goran Schwarz
 */
public class ActiveAlarmSummary
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Value for the 'group' parameter: use whatever group MY server is a member of (resolved by DbxCentral) */
	public static final String GROUP_SAME = "same";

	/** Value for the 'group' parameter: do not filter on group at all */
	public static final String GROUP_ALL  = "all";

	/** One active alarm. Only holds the fields we actually render. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Entry
	{
		public String  srvName;
		public String  group;
		public String  alarmClass;
		public String  serviceName;
		/** What distinguishes two alarms of the same class, eg the database name for LowDbFreeSpace */
		public String  extraInfo;
		public String  category;
		public String  severity;
		public String  state;
		public String  alarmId;
		public String  fullDuration;
		public String  description;
		public boolean isMuted;

		/** true = from the local AlarmHandler (real time), false = from DbxCentral (possibly stale) */
		@JsonIgnore
		public boolean local;
	}

	/** What {@link ActiveAlarmSummary#get(String, AlarmEvent)} returns. */
	public static class Result
	{
		/** The merged, filtered and sorted alarms. Never null. */
		public final List<Entry> entries;

		/** false if we could not reach DbxCentral, meaning 'entries' holds local alarms only. */
		public final boolean centralOk;

		/** Name of the group we summarized, or null if unknown / not filtering on a group. */
		public final String groupName;

		private Result(List<Entry> entries, boolean centralOk, String groupName)
		{
			this.entries   = entries;
			this.centralOk = centralOk;
			this.groupName = groupName;
		}
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private final String  _baseUrl;
	private final String  _group;
	private final boolean _skipMuted;
	private final int     _cacheSec;
	private final int     _timeoutSec;

	private final HttpClient _httpClient;

	// Cache of the CENTRAL half only. The local half is free to get, and MUST stay real time.
	private final Object      _cacheLock       = new Object();
	private       String      _cacheUrl        = null;
	private       List<Entry> _cacheEntries    = null;
	private       long        _cacheExpireTime = 0;

	/** So a DbxCentral that is down doesn't flood the Collector log: WARN once, then DEBUG. */
	private volatile boolean _hasLoggedFailure = false;

	/**
	 * @param baseUrl     Base URL to DbxCentral, eg 'http://dbxcentral:8080'. Blank = do not call DbxCentral at all (local alarms only).
	 * @param group       {@link #GROUP_SAME}, {@link #GROUP_ALL}, or one/several explicit group name(s), comma separated.
	 * @param skipMuted   Discard alarms that are muted in DbxCentral.
	 * @param cacheSec    Reuse the fetched Central list for this many seconds.
	 * @param timeoutSec  HTTP connect/request timeout.
	 */
	public ActiveAlarmSummary(String baseUrl, String group, boolean skipMuted, int cacheSec, int timeoutSec)
	{
		_baseUrl    = StringUtil.hasValue(baseUrl) ? baseUrl.replaceAll("/+$", "") : null;
		_group      = StringUtil.hasValue(group)   ? group.trim()                  : GROUP_SAME;
		_skipMuted  = skipMuted;
		_cacheSec   = Math.max(0, cacheSec);
		_timeoutSec = timeoutSec <= 0 ? 10 : timeoutSec;

		_httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(_timeoutSec))
				.build();
	}

	/**
	 * Get the merged summary of currently active alarms.
	 *
	 * @param action        RAISE / RE-RAISE / CANCEL, see {@code AlarmWriterAbstract.ACTION_*}
	 * @param currentEvent  The alarm event the message is about. May be null.
	 * @return A {@link Result}, or <b>null</b> if we have nothing at all to show.
	 */
	public Result get(String action, AlarmEvent currentEvent)
	{
		String myServerName = getCollectorServerName(currentEvent);

		List<Entry> localList   = getLocalAlarms(action, currentEvent, myServerName);
		List<Entry> centralList = getCentralAlarms(myServerName);

		boolean centralOk = (centralList != null);

		if (localList == null && centralList == null)
			return null;

		// Read the group name BEFORE we drop our own rows -- our own rows are the most likely
		// ones to carry it, since we asked for "the group that MY server is in".
		//
		// NOTE: Only when we actually asked for ONE group. With GROUP_ALL the rows span several
		//       groups, so the first row's group name would be an outright lie in the header.
		String groupName = null;
		if (centralList != null && ! GROUP_ALL.equalsIgnoreCase(_group))
		{
			for (Entry entry : centralList)
			{
				if (StringUtil.hasValue(entry.group))
				{
					groupName = entry.group;
					break;
				}
			}
		}
		if (StringUtil.isNullOrBlank(groupName) && ! GROUP_SAME.equalsIgnoreCase(_group) && ! GROUP_ALL.equalsIgnoreCase(_group))
			groupName = _group;

		List<Entry> merged = new ArrayList<>();

		if (localList != null)
			merged.addAll(localList);

		if (centralList != null)
		{
			for (Entry entry : centralList)
			{
				// Skip MY server -- the local list above is the authoritative source for those
				if (myServerName != null && myServerName.equals(entry.srvName))
					continue;

				merged.add(entry);
			}
		}

		if (_skipMuted)
			merged.removeIf(entry -> entry.isMuted);

		merged.sort(Comparator
				.comparingInt((Entry e) -> severityRank(e.severity))
				.thenComparing(e -> e.srvName    == null ? "" : e.srvName)
				.thenComparing(e -> e.alarmClass == null ? "" : e.alarmClass));

		return new Result(merged, centralOk, groupName);
	}

	//-------------------------------------------------------
	// LOCAL: this Collectors own alarms (real time)
	//-------------------------------------------------------
	private List<Entry> getLocalAlarms(String action, AlarmEvent currentEvent, String myServerName)
	{
		try
		{
			if ( ! AlarmHandler.hasInstance() )
				return null;

			List<AlarmEvent> alarmList = AlarmHandler.getInstance().getAlarmList();
			if (alarmList == null)
				return null;

			// On CANCEL the AlarmHandler calls the writers BEFORE it removes the event from the
			// active container, so the alarm we are cancelling right now is still in this list.
			UUID excludeAlarmId = null;
			if (AlarmWriterAbstract.ACTION_CANCEL.equals(action) && currentEvent != null)
				excludeAlarmId = currentEvent.getAlarmId();

			List<Entry> list = new ArrayList<>();

			// NOTE: getAlarmList() returns the LIVE internal list -- do not modify it, and copy out
			//       what we need. See the javadoc in IAlarmWriter.
			for (AlarmEvent alarmEvent : new ArrayList<>(alarmList))
			{
				if (alarmEvent == null)
					continue;

				if (excludeAlarmId != null && excludeAlarmId.equals(alarmEvent.getAlarmId()))
					continue;

				Entry entry = new Entry();

				entry.srvName      = myServerName;
				entry.group        = null;
				// Deliberately via abbreviate(), so the local and the Central half are formatted
				// by the exact same code and cannot drift apart
				entry.alarmClass   = abbreviate(alarmEvent.getAlarmClass());
				entry.serviceName  = alarmEvent.getServiceName();
				entry.extraInfo    = alarmEvent.getExtraInfo(); // NOTE: returns "" and not null, when unset
				entry.category     = "" + alarmEvent.getCategory();
				entry.severity     = "" + alarmEvent.getSeverity();
				entry.state        = "" + alarmEvent.getState();
				entry.alarmId      = alarmEvent.getAlarmId() == null ? null : alarmEvent.getAlarmId().toString();
				entry.fullDuration = alarmEvent.getFullDuration();
				entry.description  = alarmEvent.getDescription();

				// Muting is a DbxCentral side concept, the Collector knows nothing about it
				entry.isMuted      = false;
				entry.local        = true;

				list.add(entry);
			}

			return list;
		}
		catch (Throwable t)
		{
			_logger.warn("Problems getting the local Active Alarm list. Skipping the local part of the Active Alarm Summary. Caught: " + t, t);
			return null;
		}
	}

	//-------------------------------------------------------
	// CENTRAL: all other servers (up to one sample interval stale)
	//-------------------------------------------------------
	private List<Entry> getCentralAlarms(String myServerName)
	{
		if (StringUtil.isNullOrBlank(_baseUrl))
			return null; // Not configured -> local alarms only

		String url = createUrl(myServerName);

		// Cached?
		synchronized (_cacheLock)
		{
			if (_cacheEntries != null && url.equals(_cacheUrl) && System.currentTimeMillis() < _cacheExpireTime)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("ActiveAlarmSummary: Using cached Central alarm list (" + _cacheEntries.size() + " entries) for url '" + url + "'.");

				return _cacheEntries;
			}
		}

		try
		{
			String token = Configuration.getCombinedConfiguration()
					.getPropertyRaw(NoGuiManagementServer.PROPKEY_collectorRegToken, NoGuiManagementServer.DEFAULT_collectorRegToken);

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(_timeoutSec))
					.header("Accept", "application/json")
					.GET();

			if (StringUtil.hasValue(token))
				builder.header("Authorization", "Bearer " + token);

			HttpResponse<String> response = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

			int responseCode = response.statusCode();
			if (responseCode < 200 || responseCode >= 300)
				throw new Exception("Failed : HTTP error code : " + responseCode + ", from url '" + url + "'.");

			// NOTE: AlarmActiveController sets Content-Type 'text/html' even though it returns JSON,
			//       so do NOT look at the content type here, just parse the body.
			ObjectMapper om = new ObjectMapper();
			List<Entry> list = om.readValue(response.body(), new TypeReference<List<Entry>>() {});

			for (Entry entry : list)
			{
				entry.local      = false;
				entry.alarmClass = abbreviate(entry.alarmClass);
			}

			synchronized (_cacheLock)
			{
				_cacheUrl        = url;
				_cacheEntries    = list;
				_cacheExpireTime = System.currentTimeMillis() + (_cacheSec * 1000L);
			}

			_hasLoggedFailure = false;

			if (_logger.isDebugEnabled())
				_logger.debug("ActiveAlarmSummary: Got " + list.size() + " active alarm(s) from '" + url + "'.");

			return list;
		}
		catch (Throwable t)
		{
			// WARN on the first failure, then DEBUG, so a DbxCentral that is down for a while
			// does not flood the Collector log with one entry per alarm.
			String msg = "Problems getting the Active Alarm Summary from DbxCentral at '" + url + "'. "
					+ "The Alarm message will be sent WITHOUT the alarms from other servers. Caught: " + t;

			if (_hasLoggedFailure)
			{
				if (_logger.isDebugEnabled())
					_logger.debug(msg, t);
			}
			else
			{
				_hasLoggedFailure = true;
				_logger.warn(msg);
			}

			return null;
		}
	}

	private String createUrl(String myServerName)
	{
		String url = _baseUrl + "/api/alarm/active";

		if (GROUP_ALL.equalsIgnoreCase(_group))
			return url;

		try
		{
			if (GROUP_SAME.equalsIgnoreCase(_group))
			{
				if (StringUtil.isNullOrBlank(myServerName))
					return url; // We don't know who we are -> we can't ask for "my" group

				return url + "?groupOfSrv=" + URLEncoder.encode(myServerName, StandardCharsets.UTF_8.name());
			}

			return url + "?group=" + URLEncoder.encode(_group, StandardCharsets.UTF_8.name());
		}
		catch (Exception ex)
		{
			// URLEncoder.encode(String, String) declares UnsupportedEncodingException, but UTF-8 always exists
			return url;
		}
	}

	//-------------------------------------------------------
	// Rendering, shared by all AlarmWriters
	//-------------------------------------------------------

	/** Max number of 'extraInfo' values we list per Alarm name, before we say "+N more" */
	private static final int MAX_EXTRA_INFO_PER_ALARM = 4;

	/** Max length of a single 'extraInfo' value. Some are long, eg a job name plus a timestamp. */
	private static final int MAX_EXTRA_INFO_LENGTH    = 25;

	/** Max number of distinct Alarm names on a single server row, before we say "+N more" */
	private static final int MAX_ALARM_NAMES_PER_ROW  = 6;

	private static final String MIDDLE_DOT = Character.toString((char) 0x00B7);
	private static final String ELLIPSIS   = Character.toString((char) 0x2026);
	private static final String MULTIPLY   = Character.toString((char) 0x00D7);

	/** One rendered line of the summary: a server, and what is active on it. */
	public static class Row
	{
		/** Server name, or null for the trailing "and N more server(s)" row */
		public final String srvName;
		public final String text;

		private Row(String srvName, String text) { this.srvName = srvName; this.text = text; }
	}

	/**
	 * Turn a {@link Result} into one line per <b>server</b> (not per alarm) - which is what keeps a
	 * message small when a lot of things are broken at once.
	 * <p>
	 * Example: {@code prod-1 -> "4 <dot> OsLoadAverage, LowDbFreeSpace (goran_16, model, sybsecurity)"}
	 *
	 * @param result          what {@link #get(String, AlarmEvent)} returned
	 * @param maxRows         max number of server rows, the rest collapse into "and N more server(s)"
	 * @param withExtraInfo   true = list the 'extraInfo' values, false = just count them with "xN"
	 */
	public static List<Row> toRows(Result result, int maxRows, boolean withExtraInfo)
	{
		List<Row> rows = new ArrayList<>();
		if (result == null)
			return rows;

		// Group the (already sorted) alarms per server
		Map<String, List<Entry>> perServer = new LinkedHashMap<>();
		for (Entry entry : result.entries)
		{
			String srvName = StringUtil.hasValue(entry.srvName) ? entry.srvName : "-unknown-";
			perServer.computeIfAbsent(srvName, k -> new ArrayList<>()).add(entry);
		}

		int rowCount = 0;
		for (Map.Entry<String, List<Entry>> mapEntry : perServer.entrySet())
		{
			if (rowCount >= maxRows)
				break;

			rows.add(new Row(mapEntry.getKey(), createRowText(mapEntry.getValue(), withExtraInfo)));
			rowCount++;
		}

		int notShownCount = perServer.size() - rowCount;
		if (notShownCount > 0)
			rows.add(new Row(null, "and " + notShownCount + " more server(s)"));

		return rows;
	}

	private static String createRowText(List<Entry> alarms, boolean withExtraInfo)
	{
		// Distinct alarm names -> the 'extraInfo' of each alarm with that name (which is what tells
		// two alarms of the same class apart, eg the database name for LowDbFreeSpace).
		// LinkedHashMap/LinkedHashSet keeps the order they were sorted in (severity first), so an
		// ERROR name is listed before a WARNING one.
		Map<String, Set<String>> alarmNameToExtraInfo = new LinkedHashMap<>();
		Map<String, Integer>     alarmNameCount       = new LinkedHashMap<>();

		for (Entry entry : alarms)
		{
			String alarmName = StringUtil.hasValue(entry.alarmClass) ? entry.alarmClass : "-unknown-";

			alarmNameCount.merge(alarmName, 1, Integer::sum);

			Set<String> extraInfoSet = alarmNameToExtraInfo.computeIfAbsent(alarmName, k -> new LinkedHashSet<>());
			if (StringUtil.hasValue(entry.extraInfo))
				extraInfoSet.add(entry.extraInfo);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(alarms.size()).append(" ").append(MIDDLE_DOT).append(" ");

		int nameCount = 0;
		for (Map.Entry<String, Integer> nameEntry : alarmNameCount.entrySet())
		{
			if (nameCount >= MAX_ALARM_NAMES_PER_ROW)
			{
				sb.append(", +").append(alarmNameCount.size() - nameCount).append(" more");
				break;
			}

			if (nameCount > 0)
				sb.append(", ");

			String alarmName = nameEntry.getKey();
			sb.append(alarmName);

			Set<String> extraInfoSet = alarmNameToExtraInfo.get(alarmName);

			if (withExtraInfo && ! extraInfoSet.isEmpty())
			{
				// "LowDbFreeSpace (goran_16, model, sybsecurity)"
				sb.append(" (");
				int extraInfoCount = 0;
				for (String extraInfo : extraInfoSet)
				{
					if (extraInfoCount >= MAX_EXTRA_INFO_PER_ALARM)
					{
						sb.append(", +").append(extraInfoSet.size() - extraInfoCount).append(" more");
						break;
					}

					if (extraInfoCount > 0)
						sb.append(", ");

					sb.append(truncate(extraInfo, MAX_EXTRA_INFO_LENGTH));
					extraInfoCount++;
				}
				sb.append(")");
			}
			else if (nameEntry.getValue() > 1)
			{
				// Nothing to tell them apart, so at least say how many. "LowDbFreeSpace x1" would
				// just be noise, hence only when there is more than one.
				sb.append(" ").append(MULTIPLY).append(nameEntry.getValue());
			}

			nameCount++;
		}

		return sb.toString();
	}

	/** "Active Alarms - Production Servers (12)", or "No other active alarms" when empty. */
	public static String createHeader(Result result, String configuredGroup)
	{
		if (result == null || result.entries.isEmpty())
			return "No other active alarms";

		String header = "Active Alarms";

		if (StringUtil.hasValue(result.groupName))
			header += " " + Character.toString((char) 0x2014) + " " + result.groupName;
		else if (GROUP_ALL.equalsIgnoreCase(configuredGroup))
			header += " " + Character.toString((char) 0x2014) + " All Servers";

		return header + " (" + result.entries.size() + ")";
	}

	/** The "do not pretend this is live" note. See {@link #showFreshnessNote(Result)}. */
	public static final String FRESHNESS_NOTE = "Other servers as of their last sample to DbxCentral.";

	/**
	 * Should the {@link #FRESHNESS_NOTE} be rendered?
	 * <p>
	 * Only when the summary actually contains a row from <b>another</b> server. Our own rows come
	 * from the local AlarmHandler and are real time, so if they are all we are showing then nothing
	 * on screen is stale and the note would be a caveat about nothing.
	 * <p>
	 * This is common with {@code group=same}: DbxCentral answers fine, but the only server in our
	 * group with active alarms is us, and those rows get replaced by the local ones.
	 */
	public static boolean showFreshnessNote(Result result)
	{
		if (result == null || ! result.centralOk)
			return false;

		for (Entry entry : result.entries)
		{
			if ( ! entry.local )
				return true;
		}

		return false;
	}

	/**
	 * Render the summary as plain text, for writers that send text (Slack, plain text mail, ...).
	 * Returns "" when there is nothing to show, so it is safe to drop straight into a template.
	 */
	public static String toText(Result result, String configuredGroup, int maxRows, boolean withExtraInfo)
	{
		if (result == null)
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append(createHeader(result, configuredGroup)).append("\n");

		for (Row row : toRows(result, maxRows, withExtraInfo))
		{
			if (row.srvName == null)
				sb.append("    ").append(row.text).append("\n");
			else
				sb.append("    ").append(StringUtil.left(row.srvName, 30)).append(" ").append(row.text).append("\n");
		}

		if (showFreshnessNote(result))
			sb.append("    (").append(FRESHNESS_NOTE).append(")\n");

		return sb.toString();
	}

	/**
	 * Render the summary as an HTML fragment, for writers that send HTML (mail).
	 * Returns "" when there is nothing to show, so it is safe to drop straight into a template.
	 */
	public static String toHtml(Result result, String configuredGroup, int maxRows, boolean withExtraInfo)
	{
		if (result == null)
			return "";

		StringBuilder sb = new StringBuilder();

		sb.append("<div style='margin-top: 15px;'>\n");
		sb.append("  <b>").append(StringEscapeUtils.escapeHtml4(createHeader(result, configuredGroup))).append("</b>\n");

		if ( ! result.entries.isEmpty() )
		{
			sb.append("  <table style='border-collapse: collapse; margin-top: 5px; font-size: 90%;'>\n");
			for (Row row : toRows(result, maxRows, withExtraInfo))
			{
				sb.append("    <tr>");
				sb.append("<td style='padding: 2px 10px 2px 0; vertical-align: top; white-space: nowrap;'>")
				  .append(row.srvName == null ? "" : "<b>" + StringEscapeUtils.escapeHtml4(row.srvName) + "</b>")
				  .append("</td>");
				sb.append("<td style='padding: 2px 0;'>").append(StringEscapeUtils.escapeHtml4(row.text)).append("</td>");
				sb.append("</tr>\n");
			}
			sb.append("  </table>\n");

			if (showFreshnessNote(result))
				sb.append("  <div style='font-size: 80%; color: #808080;'>").append(StringEscapeUtils.escapeHtml4(FRESHNESS_NOTE)).append("</div>\n");
		}

		sb.append("</div>\n");

		return sb.toString();
	}

	/** Cut 'str' down to 'maxLength' chars, marking it with an ellipsis if anything was removed. */
	private static String truncate(String str, int maxLength)
	{
		if (str == null || str.length() <= maxLength)
			return str;

		return str.substring(0, maxLength - 1).trim() + ELLIPSIS;
	}

	//-------------------------------------------------------
	// helpers
	//-------------------------------------------------------

	/**
	 * The name this Collector is known by in the DbxCentral database (which is also the schema name,
	 * and therefore the 'srvName' that /api/alarm/active returns).
	 * <p>
	 * This mirrors {@code PersistContainer.getServerNameOrAlias()}: the alias if we have one,
	 * otherwise the stripped DBMS server name.
	 * <p>
	 * NOTE: Do <b>not</b> use {@code ICounterController.getServerName()} here -- that one prefers the
	 * <i>displayName</i>, which is not what the Central database uses as the schema name.
	 */
	private String getCollectorServerName(AlarmEvent fallbackEvent)
	{
		try
		{
			ICounterController cc = CounterController.getInstance();
			if (cc != null)
			{
				String aliasName = cc.getServerAliasName();
				if (StringUtil.hasValue(aliasName))
					return aliasName;

				String dbmsName = cc.getDbmsServerName();
				if (StringUtil.hasValue(dbmsName))
					return DbxTune.stripSrvName(dbmsName);
			}
		}
		catch (Throwable t)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("ActiveAlarmSummary: Problems getting the Collector server name from the CounterController. Falling back on the AlarmEvent. Caught: " + t, t);
		}

		// Fallback. NOTE: getServiceName() does not always hold the server name (for RepServer WS it
		// holds 'LDS.dbname'), see the TODO in AlarmEvent.private_getServerName()
		return fallbackEvent == null ? null : fallbackEvent.getServiceName();
	}

	/**
	 * 'com.dbxtune.alarm.events.AlarmEventFullTranLog' or 'AlarmEventFullTranLog' -&gt; 'FullTranLog'
	 * <p>
	 * NOTE: This has to end up with exactly the same string as {@link AlarmEvent#getAlarmClassAbriviated()},
	 * otherwise we would render 'FullTranLog' for our own server and 'AlarmEventFullTranLog' for all the
	 * others, side by side in the same FactSet.
	 * <p>
	 * DbxCentral already stores the abbreviated name in ALARM_ACTIVE.alarmClass - the column is fed from
	 * {@code getAlarmClassAbriviated()} in {@code CentralPersistWriterJdbc}, and
	 * {@code DbxTuneSample.setAlarmClassAbriviated()} strips a leading "AlarmEvent" once more on the way in.
	 * So both the package-strip and the "AlarmEvent"-strip below are belt-and-braces for older rows and for
	 * anything that writes the column differently.
	 */
	private static String abbreviate(String alarmClass)
	{
		if (alarmClass == null)
			return null;

		int lastDot = alarmClass.lastIndexOf('.');
		String name = lastDot < 0 ? alarmClass : alarmClass.substring(lastDot + 1);

		return name.replace("AlarmEvent", "");
	}

	/** ERROR first, then WARNING, then INFO, then anything else */
	private static int severityRank(String severity)
	{
		if (severity == null)                     return 9;
		if ("ERROR"  .equalsIgnoreCase(severity)) return 0;
		if ("WARNING".equalsIgnoreCase(severity)) return 1;
		if ("INFO"   .equalsIgnoreCase(severity)) return 2;
		return 3;
	}
}
