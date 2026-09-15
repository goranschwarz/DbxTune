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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.central.controllers.AlarmActiveController;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Which DbxCentral SERVER_LIST <b>group</b> is THIS Collector's server a member of?
 * <p>
 * Groups only exist in DbxCentral's {@code conf/SERVER_LIST} file, so a Collector has to ask. The answer
 * comes back in the {@code X-DbxCentral-Group} response header of {@code /api/alarm/active?groupOfSrv=<srv>},
 * which is set even when there are no active alarms (the alarm rows can't carry the group in that case).
 * <p>
 * One JVM wide value, used by the AlarmWriters for filters ({@code filter.keep.serverGroup}), routing
 * (Teams {@code route.N.groupRegex}) and templates ({@code ${serverGroup}}).
 * <ul>
 *   <li>{@link ActiveAlarmSummary} feeds it whenever it fetches with {@code groupOfSrv}, so no extra call is made
 *       while the summary is in use.</li>
 *   <li>{@link #get(AlarmEvent)} fetches on its own, but only when the value is older than {@code refreshSec},
 *       and at most once per {@code retrySec} while DbxCentral does not answer. Concurrent callers share one fetch.</li>
 *   <li>When DbxCentral does not answer, the last known value is kept.</li>
 *   <li>An old DbxCentral (no header): the group is read from the alarm rows, if there are any.</li>
 * </ul>
 * NOTE: The value follows the overview page layout in SERVER_LIST, so moving a server to another group there
 * also changes where its alarms are routed.
 */
public class DbxCentralServerGroup
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_refreshSec = "DbxCentralServerGroup.refreshSec";
	public static final int    DEFAULT_refreshSec = 300;

	public static final String PROPKEY_retrySec   = "DbxCentralServerGroup.retrySec";
	public static final int    DEFAULT_retrySec   = 60;

	public static final String PROPKEY_timeoutSec = "DbxCentralServerGroup.timeoutSec";
	public static final int    DEFAULT_timeoutSec = 5;

	public enum State
	{
		/** We never got an answer from DbxCentral (or it's not configured) */
		UNKNOWN,
		/** DbxCentral says: the server is not within any group */
		NO_GROUP,
		/** DbxCentral says: the server is in group {@link Lookup#getName()} */
		KNOWN
	};

	/** An immutable answer */
	public static final class Lookup
	{
		private final State  _state;
		private final String _name;

		private Lookup(State state, String name)
		{
			_state = state;
			_name  = name;
		}

		public State   getState()     { return _state; }
		public boolean isKnown()      { return _state == State.KNOWN; }
		public boolean isUnknown()    { return _state == State.UNKNOWN; }

		/** The group name, or null if not {@link State#KNOWN} */
		public String  getName()      { return _name; }

		/** The group name, or "" if not {@link State#KNOWN} */
		public String  getNameOrEmpty() { return _name == null ? "" : _name; }

		@Override
		public String toString()
		{
			return _state + (_name == null ? "" : "(" + _name + ")");
		}
	}

	private static final Lookup UNKNOWN  = new Lookup(State.UNKNOWN , null);
	private static final Lookup NO_GROUP = new Lookup(State.NO_GROUP, null);

	/** Current value. volatile, so peek() never has to wait for a fetch in progress. */
	private static volatile Lookup _current = UNKNOWN;

	/** The server name the current value is for */
	private static volatile String _currentSrvName = null;

	private static final Object _fetchLock = new Object();
	private static long    _lastAnswerTime   = 0; // guarded by _fetchLock
	private static long    _lastAttemptTime  = 0; // guarded by _fetchLock
	private static boolean _hasLoggedFailure = false;

	private static volatile HttpClient _httpClient = null;

	private DbxCentralServerGroup()
	{
	}

	/**
	 * The current value, WITHOUT contacting DbxCentral. Never null.
	 */
	public static Lookup peek()
	{
		return _current;
	}

	/**
	 * The current value, refreshed from DbxCentral first if it's older than {@code refreshSec}. Never null.
	 * <p>
	 * No HTTP call if DbxCentral is not configured, or if the last attempt failed less than {@code retrySec} ago.
	 *
	 * @param fallbackEvent  Used for the server name if the CounterController can't tell us. May be null.
	 */
	public static Lookup get(AlarmEvent fallbackEvent)
	{
		String srvName = WriterUtils.getCollectorServerName(fallbackEvent);
		if (StringUtil.isNullOrBlank(srvName))
			return _current;

		Configuration conf = Configuration.getCombinedConfiguration();
		long refreshMs  = conf.getIntProperty(PROPKEY_refreshSec, DEFAULT_refreshSec) * 1000L;
		long retryMs    = conf.getIntProperty(PROPKEY_retrySec,   DEFAULT_retrySec)   * 1000L;
		int  timeoutSec = conf.getIntProperty(PROPKEY_timeoutSec, DEFAULT_timeoutSec);

		synchronized (_fetchLock)
		{
			long now = System.currentTimeMillis();

			boolean sameServer = srvName.equals(_currentSrvName);
			boolean isFresh    = sameServer && (now - _lastAnswerTime) < refreshMs;
			boolean isBackOff  = sameServer && (now - _lastAttemptTime) < retryMs;

			if (isFresh || isBackOff)
				return _current;

			// Resolved here (not on every call), since it may log about a malformed PCS URL
			String baseUrl = WriterUtils.getDbxCentralFetchUrl(null);
			if (StringUtil.isNullOrBlank(baseUrl))
				return _current;

			_lastAttemptTime = now;
			fetch(baseUrl.replaceAll("/+$", ""), srvName, timeoutSec);
			return _current;
		}
	}

	/**
	 * Called with a successful {@code /api/alarm/active?groupOfSrv=<srvName>} response, by our own fetch and by
	 * {@link ActiveAlarmSummary}.
	 *
	 * @param srvName         The server name that was passed as 'groupOfSrv'
	 * @param headerValue     The raw {@code X-DbxCentral-Group} header value, null if the header was not there (old DbxCentral)
	 * @param groupFromRows   The group of the first alarm row that had one, null if none. Only used when there is no header.
	 */
	static void onCentralResponse(String srvName, String headerValue, String groupFromRows)
	{
		if (StringUtil.isNullOrBlank(srvName))
			return;

		synchronized (_fetchLock)
		{
			Lookup oldValue = _current;

			Lookup newValue;
			if (headerValue != null)
			{
				String name = URLDecoder.decode(headerValue, StandardCharsets.UTF_8).trim();
				newValue = name.isEmpty() ? NO_GROUP : new Lookup(State.KNOWN, name);
			}
			else if (StringUtil.hasValue(groupFromRows))
			{
				newValue = new Lookup(State.KNOWN, groupFromRows);
			}
			else
			{
				// Old DbxCentral and no rows to read it from: we learned nothing, keep what we have
				newValue = srvName.equals(_currentSrvName) ? oldValue : UNKNOWN;
			}

			_current         = newValue;
			_currentSrvName  = srvName;
			_lastAnswerTime  = System.currentTimeMillis();
			_lastAttemptTime = _lastAnswerTime;
			_hasLoggedFailure = false;

			if (oldValue.getState() != newValue.getState() || ! Objects.equals(oldValue.getName(), newValue.getName()))
				_logger.info("DbxCentral server group for '" + srvName + "' is now: " + newValue + " (was: " + oldValue + ")");
		}
	}

	/** Forget everything. Primarily intended for test code. */
	public static void reset()
	{
		synchronized (_fetchLock)
		{
			_current          = UNKNOWN;
			_currentSrvName   = null;
			_lastAnswerTime   = 0;
			_lastAttemptTime  = 0;
			_hasLoggedFailure = false;
		}
	}

	/** Do the HTTP call. Caller holds _fetchLock. Failures are logged and swallowed (the last known value stays). */
	private static void fetch(String baseUrl, String srvName, int timeoutSec)
	{
		String url = baseUrl + "/api/alarm/active?groupOfSrv=" + URLEncoder.encode(srvName, StandardCharsets.UTF_8);
		try
		{
			String token = Configuration.getCombinedConfiguration()
					.getPropertyRaw(NoGuiManagementServer.PROPKEY_collectorRegToken, NoGuiManagementServer.DEFAULT_collectorRegToken);

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(timeoutSec))
					.header("Accept", "application/json")
					.GET();

			if (StringUtil.hasValue(token))
				builder.header("Authorization", "Bearer " + token);

			HttpResponse<String> response = getHttpClient(timeoutSec).send(builder.build(), HttpResponse.BodyHandlers.ofString());

			int responseCode = response.statusCode();
			if (responseCode < 200 || responseCode >= 300)
				throw new Exception("Failed : HTTP error code : " + responseCode + ", from url '" + url + "'.");

			Optional<String> header = response.headers().firstValue(AlarmActiveController.HEADER_SERVER_GROUP);
			String groupFromRows = header.isPresent() ? null : getGroupFromRows(response.body());

			onCentralResponse(srvName, header.orElse(null), groupFromRows);
		}
		catch (Throwable t)
		{
			if (t instanceof InterruptedException)
				Thread.currentThread().interrupt();

			String msg = "Problems getting the server group from DbxCentral at '" + url + "'. Keeping the last known value: " + _current + ". Caught: " + t;
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
		}
	}

	/** Old DbxCentral without the header: the group of the first row that has one, or null. */
	private static String getGroupFromRows(String body)
	{
		try
		{
			List<Map<String, Object>> rows = new ObjectMapper().readValue(body, new TypeReference<List<Map<String, Object>>>() {});
			for (Map<String, Object> row : rows)
			{
				Object group = row.get("group");
				if (group != null && StringUtil.hasValue(group.toString()))
					return group.toString();
			}
		}
		catch (Exception ex)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Could not read the group from the alarm rows. Caught: " + ex, ex);
		}
		return null;
	}

	private static HttpClient getHttpClient(int timeoutSec)
	{
		HttpClient client = _httpClient;
		if (client == null)
		{
			client = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.NORMAL)
					.connectTimeout(Duration.ofSeconds(Math.max(1, timeoutSec)))
					.build();
			_httpClient = client;
		}
		return client;
	}
}
