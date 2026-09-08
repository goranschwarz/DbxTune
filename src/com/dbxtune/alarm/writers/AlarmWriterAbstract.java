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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CmSettingsHelper.UrlInputValidator;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public abstract class AlarmWriterAbstract
implements IAlarmWriter
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public final static String ACTION_RAISE    = "RAISE";
	public final static String ACTION_RE_RAISE = "RE-RAISE";
	public final static String ACTION_CANCEL   = "CANCEL";

	private Configuration _configuration = null;

	@Override
	public void init(Configuration conf) throws Exception
	{
		setConfiguration(conf);
		initActiveAlarmSummary(conf);
	}

	public void setConfiguration(Configuration conf)
	{
		_configuration = conf;
	}

	public Configuration getConfiguration()
	{
		return _configuration;
	}

	@Override
	public void startService()
	{
	}

	@Override
	public void stopService()
	{
	}

	/**
	 * What is this AlarmWriter named to...
	 */
	@Override
	public String getName() 
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public void endOfScan(List<AlarmEvent> activeAlarms)
	{
	}

	@Override
	public void restoredAlarms(List<AlarmEvent> restoredAlarms)
	{
	}

	//----------------------------------------------------------------
	// BEGIN: Active Alarms Summary
	//
	// A summary of what is STILL ACTIVE, which a writer can append to the messages it sends.
	// It lives here (and not in the individual writers) for the same reason the 'filter' handling
	// does: every writer wants it, and it should only be configured/maintained in one place.
	//
	// The keys use the same '<AlarmWriterName>' placeholder as the filters above, so they resolve to
	// eg 'AlarmWriterToTeams.activeAlarms.summary.enabled' / 'AlarmWriterToMail.activeAlarms.summary.enabled'.
	//----------------------------------------------------------------
	public static final String  PROPKEY_summaryEnabled        = "<AlarmWriterName>.activeAlarms.summary.enabled";
//	public static final boolean DEFAULT_summaryEnabled        = false;
	public static final boolean DEFAULT_summaryEnabled        = true;

	public static final String  PROPKEY_summaryUrl            = "<AlarmWriterName>.activeAlarms.summary.url";
	public static final String  DEFAULT_summaryUrl            = null;

	public static final String  PROPKEY_summaryGroup          = "<AlarmWriterName>.activeAlarms.summary.group";
	public static final String  DEFAULT_summaryGroup          = ActiveAlarmSummary.GROUP_SAME;

	public static final String  PROPKEY_summaryOnActions      = "<AlarmWriterName>.activeAlarms.summary.onActions";
//	public static final String  DEFAULT_summaryOnActions      = ACTION_RAISE + "," + ACTION_RE_RAISE + "," + ACTION_CANCEL;
	public static final String  DEFAULT_summaryOnActions      = ACTION_RAISE + "," + ACTION_CANCEL;

	public static final String  PROPKEY_summaryMaxRows        = "<AlarmWriterName>.activeAlarms.summary.maxRows";
	public static final int     DEFAULT_summaryMaxRows        = 25;

	public static final String  PROPKEY_summarySkipMuted      = "<AlarmWriterName>.activeAlarms.summary.skipMuted";
	public static final boolean DEFAULT_summarySkipMuted      = true;

	public static final String  PROPKEY_summaryCacheSec       = "<AlarmWriterName>.activeAlarms.summary.cacheSec";
	public static final int     DEFAULT_summaryCacheSec       = 60;

	public static final String  PROPKEY_summaryTimeoutSec     = "<AlarmWriterName>.activeAlarms.summary.timeoutSec";
	public static final int     DEFAULT_summaryTimeoutSec     = 10;

	private boolean            _summaryEnabled     = DEFAULT_summaryEnabled;
	private String             _summaryGroup       = DEFAULT_summaryGroup;
	private String             _summaryUrl         = null; // resolved, after the dbxCentralUrl fallback
	private int                _summaryMaxRows     = DEFAULT_summaryMaxRows;
	private Set<String>        _summaryOnActions   = new LinkedHashSet<>();
	private ActiveAlarmSummary _activeAlarmSummary = null;

	/** Read the 'activeAlarms.summary.*' settings. Called from {@link #init(Configuration)}. */
	private void initActiveAlarmSummary(Configuration conf)
	{
		_summaryEnabled = conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_summaryEnabled), DEFAULT_summaryEnabled);
		_summaryGroup   = conf.getProperty       (replaceAlarmWriterName(PROPKEY_summaryGroup  ), DEFAULT_summaryGroup);
		_summaryMaxRows = conf.getIntProperty    (replaceAlarmWriterName(PROPKEY_summaryMaxRows), DEFAULT_summaryMaxRows);

		boolean skipMuted  = conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_summarySkipMuted ), DEFAULT_summarySkipMuted);
		int     cacheSec   = conf.getIntProperty    (replaceAlarmWriterName(PROPKEY_summaryCacheSec  ), DEFAULT_summaryCacheSec);
		int     timeoutSec = conf.getIntProperty    (replaceAlarmWriterName(PROPKEY_summaryTimeoutSec), DEFAULT_summaryTimeoutSec);

		_summaryOnActions.clear();
		for (String action : StringUtil.commaStrToList(conf.getProperty(replaceAlarmWriterName(PROPKEY_summaryOnActions), DEFAULT_summaryOnActions)))
			_summaryOnActions.add(action.trim().toUpperCase());

		_summaryUrl = resolveSummaryUrl(conf);

		if (_summaryEnabled)
		{
			if (StringUtil.isNullOrBlank(_summaryUrl))
			{
				_logger.info("AlarmWriter '" + getName() + "': The Active Alarms Summary is enabled, but this Collector is not configured to "
						+ "send data to DbxCentral, and no explicit '" + replaceAlarmWriterName(PROPKEY_summaryUrl) + "' is set. "
						+ "The summary will hold alarms from THIS Collector only, and no call to DbxCentral will be made.");
			}

			_activeAlarmSummary = new ActiveAlarmSummary(_summaryUrl, _summaryGroup, skipMuted, cacheSec, timeoutSec);
		}
	}

	/**
	 * Work out which DbxCentral to ask for the <i>other</i> servers active alarms.
	 * <p>
	 * Deliberately <b>not</b> {@link #getDbxCentralUrl()}: that one never returns blank - when nothing
	 * is configured it derives {@code http://<local hostname>:<port>} so that messages always have
	 * <i>something</i> to link to. Good for a link, useless for a fetch: on a Collector that does not
	 * talk to DbxCentral at all it would make us HTTP GET a host that isn't there, once per cache
	 * period, forever.
	 * <p>
	 * Order:
	 * <ol>
	 *   <li>{@code <AlarmWriterName>.activeAlarms.summary.url} - explicit, always wins</li>
	 *   <li>the PCS writer's URL - its presence <b>proves</b> this Collector sends to DbxCentral, and
	 *       it is by definition an address this Collector can reach</li>
	 *   <li>{@code DbxCentral.public.base.url} - explicitly configured, but it is the <i>public</i>
	 *       URL, which may sit behind a proxy we cannot reach from here</li>
	 *   <li>otherwise null - local alarms only, and no HTTP call at all</li>
	 * </ol>
	 */
	private String resolveSummaryUrl(Configuration conf)
	{
		// 1) explicitly configured for this writer
		String url = getProp(conf, replaceAlarmWriterName(PROPKEY_summaryUrl));
		if (StringUtil.hasValue(url))
			return url;

		// 2) do we actually send to DbxCentral? If so, use that host.
		String pcsUrl = getProp(conf, "PersistWriterToDbxCentral.url");
		if (StringUtil.isNullOrBlank(pcsUrl))
			pcsUrl = getProp(conf, "PersistWriterToHttpJson.url");

		if (StringUtil.hasValue(pcsUrl))
		{
			try
			{
				// strip the path, eg 'http://host:80/api/pcs/receiver' -> 'http://host:80'
				URL u = new URL(pcsUrl);
				return u.getProtocol() + "://" + u.getHost() + (u.getPort() < 0 ? "" : ":" + u.getPort());
			}
			catch (MalformedURLException ex)
			{
				_logger.info("AlarmWriter '" + getName() + "': Could not parse the PCS URL '" + pcsUrl + "' when looking for DbxCentral. Caught: " + ex);
			}
		}

		// 3) the public base URL, if someone set it explicitly
		url = getProp(conf, PROPKEY_dbxCentralUrl);
		if (StringUtil.hasValue(url))
			return url;

		// 4) this Collector does not talk to DbxCentral -> local alarms only
		return null;
	}

	/** Look in the passed Configuration first, then in the combined one. */
	private static String getProp(Configuration conf, String propName)
	{
		String val = conf == null ? null : conf.getProperty(propName, null);
		if (StringUtil.hasValue(val))
			return val;

		return Configuration.getCombinedConfiguration().getProperty(propName, null);
	}

	/** Is the Active Alarms Summary turned on, and wanted for this particular action? */
	public boolean isActiveAlarmSummaryEnabled(String action)
	{
		return _summaryEnabled && _activeAlarmSummary != null && _summaryOnActions.contains(action);
	}

	/**
	 * Get the summary of what is currently active, or <b>null</b> if it's disabled or unavailable.
	 * <p>
	 * NOTE: This may do an HTTP call to DbxCentral (cached, and fail-soft), so call it <b>once</b>
	 * per message and reuse the result.
	 */
	public ActiveAlarmSummary.Result getActiveAlarmSummary(String action, AlarmEvent alarmEvent)
	{
		if ( ! isActiveAlarmSummaryEnabled(action) )
			return null;

		return _activeAlarmSummary.get(action, alarmEvent);
	}

	/** Max number of server rows a writer should render */
	public int getActiveAlarmSummaryMaxRows()
	{
		return _summaryMaxRows;
	}

	/** The group we summarize: 'same', 'all', or explicit name(s). Used for the header text. */
	public String getActiveAlarmSummaryGroup()
	{
		return _summaryGroup;
	}

	/** Add these to the writers {@code getAvailableSettings()}, the same way the filters are added. */
	public List<CmSettingsHelper> getActiveAlarmSummarySettings()
	{
		List<CmSettingsHelper> list = new ArrayList<>();

		Configuration conf = Configuration.getCombinedConfiguration();

		String pkEnabled    = replaceAlarmWriterName(PROPKEY_summaryEnabled);
		String pkUrl        = replaceAlarmWriterName(PROPKEY_summaryUrl);
		String pkGroup      = replaceAlarmWriterName(PROPKEY_summaryGroup);
		String pkOnActions  = replaceAlarmWriterName(PROPKEY_summaryOnActions);
		String pkMaxRows    = replaceAlarmWriterName(PROPKEY_summaryMaxRows);
		String pkSkipMuted  = replaceAlarmWriterName(PROPKEY_summarySkipMuted);
		String pkCacheSec   = replaceAlarmWriterName(PROPKEY_summaryCacheSec);
		String pkTimeoutSec = replaceAlarmWriterName(PROPKEY_summaryTimeoutSec);

		list.add( new CmSettingsHelper("activeAlarms-summary-enabled",    pkEnabled,    Boolean.class, conf.getBooleanProperty(pkEnabled,    DEFAULT_summaryEnabled),    DEFAULT_summaryEnabled,    "<html>Append a summary of all <b>currently active</b> alarms to the message.<br>The alarms for <i>this</i> Collector come from the local AlarmHandler (real time), all <i>other</i> servers are fetched from DbxCentral (and are therefore only as fresh as their last delivery to DbxCentral).</html>"));
		list.add( new CmSettingsHelper("activeAlarms-summary-url",        pkUrl,        String .class, conf.getProperty       (pkUrl,        DEFAULT_summaryUrl),        DEFAULT_summaryUrl,        "<html>Base URL to DbxCentral, used to fetch the active alarms for the <b>other</b> servers, eg: <code>http://dbxcentral:8080</code><br>If left blank, '" + PROPKEY_dbxCentralUrl + "' is used. If that is also blank, the summary will only hold alarms from <b>this</b> Collector.</html>", new UrlInputValidator()));
		list.add( new CmSettingsHelper("activeAlarms-summary-group",      pkGroup,      String .class, conf.getProperty       (pkGroup,      DEFAULT_summaryGroup),      DEFAULT_summaryGroup,      "<html>Which servers to include in the summary.<br><ul><li><code>same</code> = only servers in the same <b>GROUP</b> as this server. DbxCentral resolves this from the '#FORMAT; GROUP; ...' lines in its <code>conf/SERVER_LIST</code> file, so it automatically follows any changes made there.</li><li><code>all</code> = every server DbxCentral knows about.</li><li>Anything else = one or several explicit group name(s), comma separated. Example: <code>Production Servers</code></li></ul></html>"));
		list.add( new CmSettingsHelper("activeAlarms-summary-onActions",  pkOnActions,  String .class, conf.getProperty       (pkOnActions,  DEFAULT_summaryOnActions),  DEFAULT_summaryOnActions,  "<html>Which message types that should carry the summary. Comma separated.<br>Valid values: <code>" + ACTION_RAISE + "</code>, <code>" + ACTION_RE_RAISE + "</code>, <code>" + ACTION_CANCEL + "</code></html>"));
		list.add( new CmSettingsHelper("activeAlarms-summary-maxRows",    pkMaxRows,    Integer.class, conf.getIntProperty    (pkMaxRows,    DEFAULT_summaryMaxRows),    DEFAULT_summaryMaxRows,    "<html>Max number of <b>server</b> rows in the summary (one row per server, listing that servers alarms). Any servers above this are collapsed into a 'and N more server(s)' row.</html>"));
		list.add( new CmSettingsHelper("activeAlarms-summary-skipMuted",  pkSkipMuted,  Boolean.class, conf.getBooleanProperty(pkSkipMuted,  DEFAULT_summarySkipMuted),  DEFAULT_summarySkipMuted,  "Discard alarms that are currently muted in DbxCentral."));
		list.add( new CmSettingsHelper("activeAlarms-summary-cacheSec",   pkCacheSec,   Integer.class, conf.getIntProperty    (pkCacheSec,   DEFAULT_summaryCacheSec),   DEFAULT_summaryCacheSec,   "<html>Reuse the list fetched from DbxCentral for this many seconds.<br>This is what makes an 'alarm storm' (many alarms in the same scan) do <b>one</b> call to DbxCentral instead of one per alarm.</html>"));
		list.add( new CmSettingsHelper("activeAlarms-summary-timeoutSec", pkTimeoutSec, Integer.class, conf.getIntProperty    (pkTimeoutSec, DEFAULT_summaryTimeoutSec), DEFAULT_summaryTimeoutSec, "Connect/request timeout when fetching the active alarms from DbxCentral. If it fails, the message is still sent, just without the other servers."));

		return list;
	}

	/** Log the Active Alarms Summary configuration. Call this from the writers {@code printConfig()}. */
	public void printActiveAlarmSummaryConfig()
	{
		int spaces = 50;

		_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_summaryEnabled), spaces) + ": " + _summaryEnabled);
		if (_summaryEnabled)
		{
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_summaryUrl      ), spaces) + ": " + _summaryUrl + (StringUtil.isNullOrBlank(_summaryUrl) ? "   <<-- Not sending to DbxCentral, so ONLY alarms from THIS Collector are listed (no HTTP call is made)." : ""));
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_summaryGroup    ), spaces) + ": " + _summaryGroup);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_summaryOnActions), spaces) + ": " + _summaryOnActions);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_summaryMaxRows  ), spaces) + ": " + _summaryMaxRows);
		}
	}
	//----------------------------------------------------------------
	// END: Active Alarms Summary
	//----------------------------------------------------------------

	public static final String  PROPKEY_dbxCentralUrl = DailySummaryReportAbstract.PROPKEY_DbxCentralPublicBaseUrl;
	public static final String  DEFAULT_dbxCentralUrl = DailySummaryReportAbstract.DEFAULT_DbxCentralPublicBaseUrl;

	public static String static_getDbxCentralUrl()
	{
		// Properties OVERRIDES any of the below logics
		String baseUrl = Configuration.getCombinedConfiguration().getProperty(DailySummaryReportAbstract.PROPKEY_DbxCentralPublicBaseUrl, DailySummaryReportAbstract.DEFAULT_DbxCentralPublicBaseUrl);
		if (StringUtil.hasValue(baseUrl))
		{
			return baseUrl;
		}

		return DailySummaryReportAbstract.getDbxCentralPublicBaseUrl();
	}

	@Override
	/**
	 * Get the configuration for 'AlarmWriter.dbxCentralUrl'<br>
	 * If it's not configured, we try to do <i>best effort</i> to find the name in <i>other</i> configurations
	 */
	public String getDbxCentralUrl()
	{
		return static_getDbxCentralUrl();
		
//		String dbxCentralUrl = getConfiguration().getProperty(PROPKEY_dbxCentralUrl, DEFAULT_dbxCentralUrl);
//		
//		if (StringUtil.isNullOrBlank(dbxCentralUrl))
//		{
//			// if it's not configured...
//			// can we grab the URL from PCS PersistWriterToHttpJsonRest
//			// and if it points to 'localhost:8080' then -->> get current hostname
//			if (PersistentCounterHandler.hasInstance())
//			{
//				PersistentCounterHandler pcs = PersistentCounterHandler.getInstance();
//				List<IPersistWriter> writers = pcs.getWriters();
//				for (IPersistWriter writer : writers)
//				{
//					if (writer instanceof PersistWriterToHttpJson)
//					{
//						try
//						{
//							PersistWriterToHttpJson dbxCentralWriter = (PersistWriterToHttpJson) writer;
//							Configuration conf = dbxCentralWriter.getConfig();
//							String url = conf.getProperty("PersistWriterToHttpJson.url", null);
//
////							public static final String  PROPKEY_url               = "PersistWriterToHttpJson.url";
////							public static final String  DEFAULT_url               = "http://localhost:8080/api/pcs/receiver";
//							
//							// yes we found it...
//							if (url != null && url.endsWith("/api/pcs/receiver"))
//							{
//								URI uri = new URI(url);
//								String host = uri.getHost();
//								int    port = uri.getPort(); // -1 if not defined
//									
//								// Try to replace 'localhost' with our current hostname...
//								if ("localhost".equals(host))
//								{
//									host = InetAddress.getLocalHost().getCanonicalHostName();
//									
//									// This is probably a host on GCP or possibly AWS, Azure or similar
//									// GCP & AWS: ".internal"
//									// Azure:     ".internal.cloudapp.net" or ".reddog.microsoft.com"
//									// Then grab the 'hostname' or 'ip', and do a DNS Lookup on it 
////									if (host != null && (host.endsWith(".internal") || host.endsWith(".internal.cloudapp.net") || host.endsWith(".reddog.microsoft.com")))
////									{
////										// Using InetAddress... for the lookup... Did not work
////										// >> So we probably need to fallback on: https://github.com/dnsjava/dnsjava
////										// >> Or just use the Property 'AlarmWriter.dbxCentralUrl = http://dbxtune.acme.com' instead...
////									}
//								}
//									
//								if (port < 0)
//									dbxCentralUrl = "http://" + host;
//								else
//									dbxCentralUrl = "http://" + host + ":" + port;
//							}
//						}
//						catch(Exception ignore) {}
//					}
//				}
//			}
//		}
//		
//		return dbxCentralUrl;
	}

	//----------------------------------------------------------------
	// BEGIN: Filter handling
	//----------------------------------------------------------------
	public static final String  PROPKEY_filter_keep_alarmClass = "<AlarmWriterName>.filter.keep.alarmClass";
	public static final String  DEFAULT_filter_keep_alarmClass = "";

	public static final String  PROPKEY_filter_skip_alarmClass = "<AlarmWriterName>.filter.skip.alarmClass";
	public static final String  DEFAULT_filter_skip_alarmClass = "";

	public static final String  PROPKEY_filter_keep_servername = "<AlarmWriterName>.filter.keep.serverName";
	public static final String  DEFAULT_filter_keep_servername = "";

	public static final String  PROPKEY_filter_skip_servername = "<AlarmWriterName>.filter.skip.serverName";
	public static final String  DEFAULT_filter_skip_servername = "";

	public static final String  PROPKEY_filter_keep_category   = "<AlarmWriterName>.filter.keep.category";
	public static final String  DEFAULT_filter_keep_category   = "";

	public static final String  PROPKEY_filter_skip_category   = "<AlarmWriterName>.filter.skip.category";
	public static final String  DEFAULT_filter_skip_category   = "";

	public static final String  PROPKEY_filter_keep_severity   = "<AlarmWriterName>.filter.keep.severity";
	public static final String  DEFAULT_filter_keep_severity   = "";

	public static final String  PROPKEY_filter_skip_severity   = "<AlarmWriterName>.filter.skip.severity";
	public static final String  DEFAULT_filter_skip_severity   = "";

	public static final String  PROPKEY_filter_keep_state      = "<AlarmWriterName>.filter.keep.state";
	public static final String  DEFAULT_filter_keep_state      = "";

	public static final String  PROPKEY_filter_skip_state      = "<AlarmWriterName>.filter.skip.state";
	public static final String  DEFAULT_filter_skip_state      = "";

	public static final String  PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime = "<AlarmWriterName>.filter.alwaysSendAlarmOnErrorsThatAffectsUpTime";
	public static final boolean DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime = true;

	@Override
	public void printFilterConfig()
	{
		Configuration conf = getConfiguration();

		String keep_alarmClass_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_alarmClass), DEFAULT_filter_keep_alarmClass);
		String skip_alarmClass_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_alarmClass), DEFAULT_filter_skip_alarmClass);
		String keep_servername_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_servername), DEFAULT_filter_keep_servername);
		String skip_servername_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_servername), DEFAULT_filter_skip_servername);
		String keep_category_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_category  ), DEFAULT_filter_keep_category);
		String skip_category_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_category  ), DEFAULT_filter_skip_category);
		String keep_severity_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), DEFAULT_filter_keep_severity);
		String skip_severity_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), DEFAULT_filter_skip_severity);
		String keep_state_regExp      = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_state     ), DEFAULT_filter_keep_state);
		String skip_state_regExp      = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_state     ), DEFAULT_filter_skip_state);
		
		boolean alwaysSendAlarmOnErrorsThatAffectsUpTime = conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime);

		if (StringUtil.isNullOrBlank(
				keep_alarmClass_regExp +
				skip_alarmClass_regExp +
				keep_servername_regExp +
				skip_servername_regExp +
				keep_category_regExp   +
				skip_category_regExp   +
				keep_severity_regExp   +
				skip_severity_regExp   +
				keep_state_regExp      +
				skip_state_regExp
				))
		{
			_logger.info("NO Filter Configuration for Alarm Writer Module: "+getName());
		}
		else
		{
			int spaces = 1 + getName().length() + ".filter.keep.serverName".length();
			
			_logger.info("Filter Configuration for Alarm Writer Module: "+getName());
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_keep_alarmClass), spaces) + ": " + keep_alarmClass_regExp);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_skip_alarmClass), spaces) + ": " + skip_alarmClass_regExp);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_keep_servername), spaces) + ": " + keep_servername_regExp);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_skip_servername), spaces) + ": " + skip_servername_regExp);
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_keep_category  ), spaces) + ": " + keep_category_regExp  );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_skip_category  ), spaces) + ": " + skip_category_regExp  );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), spaces) + ": " + keep_severity_regExp  );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), spaces) + ": " + skip_severity_regExp  );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_keep_state     ), spaces) + ": " + keep_state_regExp     );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_skip_state     ), spaces) + ": " + skip_state_regExp     );
			_logger.info("    " + StringUtil.left(replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), spaces) + ": " + alwaysSendAlarmOnErrorsThatAffectsUpTime);
			_logger.info("DbxCentral URL for Alarm Writer Module: "+getName());
			_logger.info("    " + PROPKEY_dbxCentralUrl + ": " + getDbxCentralUrl());
		}
	}
	
	/** Simply replace a tag &lt;AlarmWriterName&gt; with the name of the AlarmWriter */ 
	public String replaceAlarmWriterName(String propKey)
	{
		return replaceAlarmWriterName(getName(), propKey);
	}
	public static String replaceAlarmWriterName(String alarmWriterName, String propKey)
	{
		return propKey.replace("<AlarmWriterName>", alarmWriterName);
	}
	
	@Override
	public boolean doAlarm(AlarmEvent ae)
	{
		Configuration conf = getConfiguration();
		
		// if we ALWAYS should send alarm for this AlarmEvent, exit early...
		if (ae.alwaysSend())
			return true;

		String alarmClass = ae.getAlarmClassAbriviated()  + "";
		String serverName = ae.getServiceName()           + "";
		String category   = ae.getCategory()              + "";
		String severity   = ae.getSeverity()              + "";
		String state      = ae.getState()                 + "";

		// 
		String keep_alarmClass_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_alarmClass), DEFAULT_filter_keep_alarmClass);
		String skip_alarmClass_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_alarmClass), DEFAULT_filter_skip_alarmClass);
		String keep_servername_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_servername), DEFAULT_filter_keep_servername);
		String skip_servername_regExp = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_servername), DEFAULT_filter_skip_servername);
		String keep_category_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_category  ), DEFAULT_filter_keep_category);
		String skip_category_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_category  ), DEFAULT_filter_skip_category);
		String keep_severity_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), DEFAULT_filter_keep_severity);
		String skip_severity_regExp   = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), DEFAULT_filter_skip_severity);
		String keep_state_regExp      = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_state     ), DEFAULT_filter_keep_state);
		String skip_state_regExp      = conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_state     ), DEFAULT_filter_skip_state);

		boolean alwaysSendAlarmOnErrorsThatAffectsUpTime = conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime);
		
		// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
		// Below is more readable, from a variable context point-of-view, but HARDER to understand
		boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)

		// alarmClass: Keep & Skip rules
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keep_alarmClass_regExp) ||   alarmClass.matches(keep_alarmClass_regExp ))); //     matches the KEEP alarmClass regexp
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skip_alarmClass_regExp) || ! alarmClass.matches(skip_alarmClass_regExp ))); // NO match in the SKIP alarmClass regexp

		// servername: Keep & Skip rules
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keep_servername_regExp) ||   serverName.matches(keep_servername_regExp ))); //     matches the KEEP serverName regexp
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skip_servername_regExp) || ! serverName.matches(skip_servername_regExp ))); // NO match in the SKIP serverName regexp

		// category: Keep & Skip rules
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keep_category_regExp)   ||   category  .matches(keep_category_regExp   ))); //     matches the KEEP category   regexp
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skip_category_regExp)   || ! category  .matches(skip_category_regExp   ))); // NO match in the SKIP category   regexp

		// severity: Keep & Skip rules
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keep_severity_regExp)   ||   severity  .matches(keep_severity_regExp   ))); //     matches the KEEP severity   regexp
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skip_severity_regExp)   || ! severity  .matches(skip_severity_regExp   ))); // NO match in the SKIP severity   regexp

		// state: Keep & Skip rules
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keep_state_regExp)      ||   state     .matches(keep_state_regExp      ))); //     matches the KEEP state      regexp
		doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skip_state_regExp)      || ! state     .matches(skip_state_regExp      ))); // NO match in the SKIP state      regexp

		// if we have passed all the filters... 
		// if the alarm is an ERROR and service state is AFFECTED
		// Then always send an Alarm
		if ( doAlarm && AlarmEvent.Severity.ERROR.equals(ae.getSeverity()) && AlarmEvent.ServiceState.AFFECTED.equals(ae.getState()) )
		{
			if (alwaysSendAlarmOnErrorsThatAffectsUpTime)
				doAlarm = true;
		}
		
		return doAlarm;
	}

	@Override
//	public List<CmSettingsHelper> getLocalAlarmWriterFilterSettings()
	public List<CmSettingsHelper> getAvailableFilters()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		List<CmSettingsHelper> list = new ArrayList<>();
		
		String regexpTestPage = " Regexp test page: http://www.regexplanet.com/advanced/java/index.html";
		
		list.add(new CmSettingsHelper("AlarmClass Keep", replaceAlarmWriterName(PROPKEY_filter_keep_alarmClass), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_alarmClass), DEFAULT_filter_keep_alarmClass), DEFAULT_filter_keep_alarmClass, "Only for the 'AlarmClass' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated."+regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("AlarmClass Skip", replaceAlarmWriterName(PROPKEY_filter_skip_alarmClass), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_alarmClass), DEFAULT_filter_skip_alarmClass), DEFAULT_filter_skip_alarmClass, "Discard 'AlarmClass' listed (regexp is used). Before this rule the 'keep' rules are evaluated."                  +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("ServerName Keep", replaceAlarmWriterName(PROPKEY_filter_keep_servername), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_servername), DEFAULT_filter_keep_servername), DEFAULT_filter_keep_servername, "Only for the 'ServerName' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated."+regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ServerName Skip", replaceAlarmWriterName(PROPKEY_filter_skip_servername), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_servername), DEFAULT_filter_skip_servername), DEFAULT_filter_skip_servername, "Discard 'ServerName' listed (regexp is used). Before this rule the 'keep' rules are evaluated."                  +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("Category Keep",   replaceAlarmWriterName(PROPKEY_filter_keep_category  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_category  ), DEFAULT_filter_keep_category  ), DEFAULT_filter_keep_category  , "Only for the 'Category' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated. Example values: CPU, DOWN, SPACE, SRV_CONFIG, LOCK, HADR, RPO."  +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("Category Skip",   replaceAlarmWriterName(PROPKEY_filter_skip_category  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_category  ), DEFAULT_filter_skip_category  ), DEFAULT_filter_skip_category  , "Discard 'Category' listed (regexp is used). Before this rule the 'keep' rules are evaluated. Example values: CPU, DOWN, SPACE, SRV_CONFIG, LOCK, HADR, RPO."                    +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("Severity Keep",   replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), DEFAULT_filter_keep_severity  ), DEFAULT_filter_keep_severity  , "Only for the 'Severity' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated. Example values: INFO, WARNING, ERROR."  +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("Severity Skip",   replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), DEFAULT_filter_skip_severity  ), DEFAULT_filter_skip_severity  , "Discard 'Severity' listed (regexp is used). Before this rule the 'keep' rules are evaluated. Example values: INFO, WARNING, ERROR."                    +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("State Keep",      replaceAlarmWriterName(PROPKEY_filter_keep_state     ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_state     ), DEFAULT_filter_keep_state     ), DEFAULT_filter_keep_state     , "Only for the 'State' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated. Example values: UP, AFFECTED, DOWN."     +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("State Skip",      replaceAlarmWriterName(PROPKEY_filter_skip_state     ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_state     ), DEFAULT_filter_skip_state     ), DEFAULT_filter_skip_state     , "Discard 'State' listed (regexp is used). Before this rule the 'keep' rules are evaluated. Example values: UP, AFFECTED, DOWN."                       +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("Affects UpTime",  replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), Boolean.class, conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime, "Always Send Alarm On Errors That Affects UpTime. which is if the AlarmEvent has: Severity=ERROR and ServiceState=AFFECTED"));

		return list;
	}
	//----------------------------------------------------------------
	// END: Filter handling
	//----------------------------------------------------------------

}
