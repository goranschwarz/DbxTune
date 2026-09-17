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
import java.util.Collections;
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

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.central.controllers.AlarmActiveController;
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

	/** Severity (ERROR first), then server, then alarm name, then extraInfo -- so the rows come out in a stable order. */
	private static final Comparator<Entry> ENTRY_ORDER = Comparator
			.comparingInt((Entry e) -> severityRank(e.severity))
			.thenComparing(e -> e.srvName    == null ? "" : e.srvName)
			.thenComparing(e -> e.alarmClass == null ? "" : e.alarmClass)
			.thenComparing(e -> e.extraInfo  == null ? "" : e.extraInfo);

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
		String myServerName = WriterUtils.getCollectorServerName(currentEvent);

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

		// For "same": what DbxCentral said in the X-DbxCentral-Group header (fed by getCentralAlarms),
		// which is there even when the group has no rows to read it from.
		if (centralList != null && GROUP_SAME.equalsIgnoreCase(_group))
			groupName = DbxCentralServerGroup.peek().getName();

		if (StringUtil.isNullOrBlank(groupName) && centralList != null && ! GROUP_ALL.equalsIgnoreCase(_group))
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

		merged.sort(ENTRY_ORDER);

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

				Entry entry = toEntry(alarmEvent, myServerName);
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

	/** An AlarmEvent as a summary Entry, marked as local. */
	private static Entry toEntry(AlarmEvent alarmEvent, String srvName)
	{
		Entry entry = new Entry();

		entry.srvName      = srvName;
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

		return entry;
	}

	/**
	 * An example summary built from the passed alarms - nothing is fetched. Used to preview templates
	 * (eg in the Template Editor), so a template using the summary shows something realistic.
	 * <p>
	 * The entries are marked as coming from DbxCentral, so the preview also shows the freshness note.
	 */
	public static Result createExampleResult(List<AlarmEvent> alarms)
	{
		List<Entry> entries = new ArrayList<>();
		if (alarms != null)
		{
			for (AlarmEvent alarmEvent : alarms)
			{
				if (alarmEvent == null)
					continue;

				Entry entry = toEntry(alarmEvent, alarmEvent.getServiceName());
				entry.local = false;
				entries.add(entry);
			}
		}
		entries.sort(ENTRY_ORDER);

		return new Result(entries, true, WriterUtils.EXAMPLE_SERVER_GROUP);
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

			String groupFromRows = null;
			for (Entry entry : list)
			{
				entry.local      = false;
				entry.alarmClass = abbreviate(entry.alarmClass);

				if (groupFromRows == null && StringUtil.hasValue(entry.group))
					groupFromRows = entry.group;
			}

			// We asked with 'groupOfSrv', so the answer also tells what group THIS server is in.
			// Pass it on, so filters/routing do not have to make a call of their own.
			if (url.contains("groupOfSrv="))
			{
				String header = response.headers().firstValue(AlarmActiveController.HEADER_SERVER_GROUP).orElse(null);
				DbxCentralServerGroup.onCentralResponse(myServerName, header, groupFromRows);
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

	/** COMPACT form only: max number of distinct Alarm names listed under one server, before we say "+N more" */
	private static final int MAX_ALARM_NAMES_PER_SERVER = 6;

	private static final String MULTIPLY = Character.toString((char) 0x00D7);

	/**
	 * One server in the summary, and what is active on it: one line per <b>alarm</b>.
	 * <pre>
	 * GORAN_UB3_DS
	 *  * OsLoadAverage
	 *  * LowDbFreeSpace (goran_16)
	 *  * LowDbFreeSpace (model)
	 *  * LowDbFreeSpace (sybsecurity)
	 * </pre>
	 * The list may end with an entry where {@link #isMore()} is true: its name is "and N more server(s)"
	 * and it has no alarms, so a template that simply prints every server name still renders it sensibly.
	 * <p>
	 * NOTE: Velocity only reaches values through getters (not public fields), which is why these are getters.
	 */
	public static class ServerAlarms
	{
		private final String       _srvName;
		private final List<String> _alarms;
		private final List<String> _severities;
		private final boolean      _more;

		private ServerAlarms(String srvName, List<String> alarms, List<String> severities, boolean more)
		{
			_srvName    = srvName;
			_alarms     = alarms;
			_severities = severities;
			_more       = more;
		}

		/** The server name. On the trailing entry: "and N more server(s)" */
		public String       getSrvName() { return _srvName; }

		/** eg "LowDbFreeSpace (goran_16)". Empty on the trailing entry. */
		public List<String> getAlarms()  { return _alarms; }

		/**
		 * The severity of each line in {@link #getAlarms()}, same size and order: "ERROR", "WARNING", "INFO".
		 * A counted (compact) line gets the worst severity of the alarms it counts; a "+N more" line gets "".
		 */
		public List<String> getSeverities() { return _severities; }

		/** true for the trailing "and N more server(s)" entry */
		public boolean      isMore()     { return _more; }
	}

	/**
	 * Group a {@link Result} per server.
	 *
	 * @param result      what {@link #get(String, AlarmEvent)} returned
	 * @param maxServers  max number of servers listed, the rest collapse into one "and N more server(s)" entry
	 * @param full        true  = one line per alarm (even duplicates), with its complete extraInfo: "LowDbFreeSpace (goran_16)"<br>
	 *                    false = COMPACT, for when space is tight: one line per distinct alarm name, counted: "LowDbFreeSpace x3"
	 * @return never null
	 */
	public static List<ServerAlarms> toServerAlarms(Result result, int maxServers, boolean full)
	{
		List<ServerAlarms> list = new ArrayList<>();
		if (result == null)
			return list;

		// Group the (already sorted) alarms per server
		Map<String, List<Entry>> perServer = new LinkedHashMap<>();
		for (Entry entry : result.entries)
		{
			String srvName = StringUtil.hasValue(entry.srvName) ? entry.srvName : "-unknown-";
			perServer.computeIfAbsent(srvName, k -> new ArrayList<>()).add(entry);
		}

		int serverCount = 0;
		for (Map.Entry<String, List<Entry>> mapEntry : perServer.entrySet())
		{
			if (serverCount >= maxServers)
				break;

			List<String> severities = new ArrayList<>();
			List<String> lines      = createAlarmLines(mapEntry.getValue(), full, severities);
			list.add(new ServerAlarms(mapEntry.getKey(), lines, severities, false));
			serverCount++;
		}

		int notShownCount = perServer.size() - serverCount;
		if (notShownCount > 0)
			list.add(new ServerAlarms("and " + notShownCount + " more server(s)", Collections.emptyList(), Collections.emptyList(), true));

		return list;
	}

	/**
	 * The lines for one server, in the order the alarms were sorted (severity first).
	 * <ul>
	 *   <li>full:    one line per alarm, even duplicates, with its complete extraInfo - nothing is shortened</li>
	 *   <li>compact: one line per distinct alarm name, counted ("LowDbFreeSpace x3"), capped at
	 *                {@link #MAX_ALARM_NAMES_PER_SERVER}. Only used when the message would otherwise be too big.</li>
	 * </ul>
	 */
	private static List<String> createAlarmLines(List<Entry> alarms, boolean full, List<String> severitiesOut)
	{
		List<String> lines = new ArrayList<>();

		if (full)
		{
			for (Entry entry : alarms)
			{
				String alarmName = StringUtil.hasValue(entry.alarmClass) ? entry.alarmClass : "-unknown-";

				if (StringUtil.hasValue(entry.extraInfo))
					lines.add(alarmName + " (" + entry.extraInfo + ")");
				else
					lines.add(alarmName);
				severitiesOut.add(toSeverity(entry));
			}
			return lines;
		}

		// COMPACT: distinct names, counted.
		// The alarms are sorted severity first, so the first one seen for a name has its worst severity.
		Map<String, Integer> alarmNameCount    = new LinkedHashMap<>();
		Map<String, String>  alarmNameSeverity = new LinkedHashMap<>();
		for (Entry entry : alarms)
		{
			String alarmName = StringUtil.hasValue(entry.alarmClass) ? entry.alarmClass : "-unknown-";
			alarmNameCount.merge(alarmName, 1, Integer::sum);
			alarmNameSeverity.putIfAbsent(alarmName, toSeverity(entry));
		}

		for (Map.Entry<String, Integer> nameEntry : alarmNameCount.entrySet())
		{
			if (lines.size() >= MAX_ALARM_NAMES_PER_SERVER)
			{
				lines.add("+" + (alarmNameCount.size() - lines.size()) + " more");
				severitiesOut.add("");
				break;
			}

			// "LowDbFreeSpace x1" would just be noise, hence the count only when there is more than one
			if (nameEntry.getValue() > 1)
				lines.add(nameEntry.getKey() + " " + MULTIPLY + nameEntry.getValue());
			else
				lines.add(nameEntry.getKey());
			severitiesOut.add(alarmNameSeverity.get(nameEntry.getKey()));
		}

		return lines;
	}

	/** The entry's severity in upper case, "" when unknown (never null: Velocity would choke on a null list element). */
	private static String toSeverity(Entry entry)
	{
		return entry.severity == null ? "" : entry.severity.trim().toUpperCase();
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
	public static final String FRESHNESS_NOTE = "Note: The above is other servers reported as of their last sample to DbxCentral.";

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
	 * <pre>
	 * Active Alarms - Sybase Servers (4)
	 *
	 * GORAN_UB3_DS
	 *  * OsLoadAverage
	 *  * LowDbFreeSpace (goran_16)
	 *  * LowDbFreeSpace (model)
	 * </pre>
	 * Returns "" when there is nothing to show, so it is safe to drop straight into a template.
	 */
	public static String toText(Result result, String configuredGroup, int maxServers, boolean full)
	{
		if (result == null)
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append(createHeader(result, configuredGroup)).append("\n");

		for (ServerAlarms srv : toServerAlarms(result, maxServers, full))
		{
			sb.append("\n").append(srv.getSrvName()).append("\n");
			for (String alarm : srv.getAlarms())
				sb.append(" * ").append(alarm).append("\n");
		}

		if (showFreshnessNote(result))
			sb.append("\n(").append(FRESHNESS_NOTE).append(")\n");

		return sb.toString();
	}

	/**
	 * Render the summary as an HTML fragment, for writers that send HTML (mail): the server name in bold,
	 * and its alarms as a bulleted list.
	 * Returns "" when there is nothing to show, so it is safe to drop straight into a template.
	 */
	public static String toHtml(Result result, String configuredGroup, int maxServers, boolean full)
	{
		if (result == null)
			return "";

		StringBuilder sb = new StringBuilder();

		sb.append("<div style='margin-top: 15px;'>\n");
		sb.append("  <b>").append(StringEscapeUtils.escapeHtml4(createHeader(result, configuredGroup))).append("</b>\n");

		if ( ! result.entries.isEmpty() )
		{
			for (ServerAlarms srv : toServerAlarms(result, maxServers, full))
			{
				if (srv.isMore())
				{
					sb.append("  <div style='margin-top: 8px;'><i>").append(StringEscapeUtils.escapeHtml4(srv.getSrvName())).append("</i></div>\n");
					continue;
				}

				sb.append("  <div style='margin-top: 8px;'><b>").append(StringEscapeUtils.escapeHtml4(srv.getSrvName())).append("</b></div>\n");
				sb.append("  <ul style='margin-top: 2px; margin-bottom: 0;'>\n");
				for (String alarm : srv.getAlarms())
					sb.append("    <li>").append(StringEscapeUtils.escapeHtml4(alarm)).append("</li>\n");
				sb.append("  </ul>\n");
			}

			if (showFreshnessNote(result))
				sb.append("  <div style='margin-top: 8px; font-size: 80%; color: #808080;'>").append(StringEscapeUtils.escapeHtml4(FRESHNESS_NOTE)).append("</div>\n");
		}

		sb.append("</div>\n");

		return sb.toString();
	}

	//-------------------------------------------------------
	// helpers
	//-------------------------------------------------------

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
