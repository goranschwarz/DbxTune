/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.alarm.writers;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.pcs.IPersistWriter;
import com.asetune.pcs.PersistWriterToHttpJson;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public abstract class AlarmWriterAbstract
implements IAlarmWriter
{
	private static Logger _logger = Logger.getLogger(AlarmWriterAbstract.class);

	public final static String ACTION_RAISE    = "RAISE";
	public final static String ACTION_RE_RAISE = "RE-RAISE";
	public final static String ACTION_CANCEL   = "CANCEL";

	private Configuration _configuration = null;

	@Override
	public void init(Configuration conf) throws Exception
	{
		setConfiguration(conf);
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

	public static final String  PROPKEY_dbxCentralUrl = "AlarmWriter.dbxCentralUrl";
	public static final String  DEFAULT_dbxCentralUrl = "";

	@Override
	/**
	 * Get the configuration for 'AlarmWriter.dbxCentralUrl'<br>
	 * If it's not configured, we try to do <i>best effort</i> to find the name in <i>other</i> configurations
	 */
	public String getDbxCentralUrl()
	{
		String dbxCentralUrl = getConfiguration().getProperty(PROPKEY_dbxCentralUrl, DEFAULT_dbxCentralUrl);
		
		if (StringUtil.isNullOrBlank(dbxCentralUrl))
		{
			// if it's not configured...
			// can we grab the URL from PCS PersistWriterToHttpJsonRest
			// and if it points to 'localhost:8080' then -->> get current hostname
			if (PersistentCounterHandler.hasInstance())
			{
				PersistentCounterHandler pcs = PersistentCounterHandler.getInstance();
				List<IPersistWriter> writers = pcs.getWriters();
				for (IPersistWriter writer : writers)
				{
					if (writer instanceof PersistWriterToHttpJson)
					{
						try
						{
							PersistWriterToHttpJson dbxCentralWriter = (PersistWriterToHttpJson) writer;
							Configuration conf = dbxCentralWriter.getConfig();
							String url = conf.getProperty("PersistWriterToHttpJson.url", null);

//							public static final String  PROPKEY_url               = "PersistWriterToHttpJson.url";
//							public static final String  DEFAULT_url               = "http://localhost:8080/api/pcs/receiver";
							
							// yes we found it...
							if (url != null && url.endsWith("/api/pcs/receiver"))
							{
								URI uri = new URI(url);
								String host = uri.getHost();
								int    port = uri.getPort(); // -1 if not defined
									
								// Try to replace 'localhost' with our current hostname...
								if ("localhost".equals(host))
								{
									host = InetAddress.getLocalHost().getCanonicalHostName();
								}
									
								if (port < 0)
									dbxCentralUrl = "http://" + host;
								else
									dbxCentralUrl = "http://" + host + ":" + port;
							}
						}
						catch(Exception ignore) {}
					}
				}
			}
		}
		
		return dbxCentralUrl;
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

		list.add(new CmSettingsHelper("Category Keep",   replaceAlarmWriterName(PROPKEY_filter_keep_category  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_category  ), DEFAULT_filter_keep_category  ), DEFAULT_filter_keep_category  , "Only for the 'Category' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated."  +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("Category Skip",   replaceAlarmWriterName(PROPKEY_filter_skip_category  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_category  ), DEFAULT_filter_skip_category  ), DEFAULT_filter_skip_category  , "Discard 'Category' listed (regexp is used). Before this rule the 'keep' rules are evaluated."                    +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("Severity Keep",   replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_severity  ), DEFAULT_filter_keep_severity  ), DEFAULT_filter_keep_severity  , "Only for the 'Severity' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated."  +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("Severity Skip",   replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_severity  ), DEFAULT_filter_skip_severity  ), DEFAULT_filter_skip_severity  , "Discard 'Severity' listed (regexp is used). Before this rule the 'keep' rules are evaluated."                    +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("State Keep",      replaceAlarmWriterName(PROPKEY_filter_keep_state     ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_keep_state     ), DEFAULT_filter_keep_state     ), DEFAULT_filter_keep_state     , "Only for the 'State' listed (regexp is used, blank=not-used). After this rule the 'skip' rule is evaluated."     +regexpTestPage, new RegExpInputValidator()));
		list.add(new CmSettingsHelper("State Skip",      replaceAlarmWriterName(PROPKEY_filter_skip_state     ), String .class, conf.getProperty(replaceAlarmWriterName(PROPKEY_filter_skip_state     ), DEFAULT_filter_skip_state     ), DEFAULT_filter_skip_state     , "Discard 'State' listed (regexp is used). Before this rule the 'keep' rules are evaluated."                       +regexpTestPage, new RegExpInputValidator()));

		list.add(new CmSettingsHelper("Affects UpTime",  replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), Boolean.class, conf.getBooleanProperty(replaceAlarmWriterName(PROPKEY_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime), DEFAULT_filter_alwaysSendAlarmOnErrorsThatAffectsUpTime, "Always Send Alarm On Errors That Affects UpTime. which is if the AlarmEvent has: Severity=ERROR and ServiceState=AFFECTED"));

		return list;
	}
	//----------------------------------------------------------------
	// END: Filter handling
	//----------------------------------------------------------------

}
