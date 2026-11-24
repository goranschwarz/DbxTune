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
package com.dbxtune.alarm.writers;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.ICounterController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEvent.ServiceState;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.DbxTuneSample.AlarmEntry;
import com.dbxtune.ui.autocomplete.completions.ShorthandCompletionX;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

import freemarker.core.ParseException;
import freemarker.core.TemplateClassResolver;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class WriterUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * A template that could not be rendered: it does not parse, or something in it failed (unknown variable, a Java method that threw).
	 * <p>
	 * Unchecked, like the template exceptions were before: a writer that gets one sends nothing for that alarm
	 * (the AlarmHandler logs it), the Template Editor / web config page show it with the line and column.
	 */
	public static class TemplateRenderException
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
		private final String _kind;
		private final int    _line;
		private final int    _column;

		public TemplateRenderException(String kind, String message, int line, int column, Throwable cause)
		{
			super(message, cause);
			_kind   = kind;
			_line   = line;
			_column = column;
		}

		/** "FreeMarker Parser Error" or "FreeMarker Template Error" */
		public String getKind()         { return _kind; }
		/** 1-based, 0 when not known */
		public int    getLineNumber()    { return _line; }
		/** 1-based, 0 when not known */
		public int    getColumnNumber() { return _column; }
	}

	/** One FreeMarker configuration for all alarm templates (thread safe once set up) */
	private static final freemarker.template.Configuration _templateConf = createTemplateConf();

	/** Parsed templates, by template text: a template is rendered for every alarm, but rarely changes */
	private static final Map<String, Template> _templateCache = Collections.synchronizedMap(new LinkedHashMap<String, Template>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;
		@Override protected boolean removeEldestEntry(Map.Entry<String, Template> eldest) { return size() > 64; }
	});

	private static freemarker.template.Configuration createTemplateConf()
	{
		freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER); // an error is an error (shown in the editor, logged by the AlarmHandler)
		cfg.setLogTemplateExceptions(false);   // we throw them, the caller logs them
		cfg.setWrapUncheckedExceptions(true);  // a Java method that throws becomes a TemplateException, with the line in the template
		cfg.setNumberFormat("computer");       // 1234567, not 1,234,567
		cfg.setBooleanFormat("c");             // ${hasServerGroup} prints true/false (as Velocity did)
		cfg.setAPIBuiltinEnabled(false);
		cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER); // no ?new: a template must not create arbitrary Java objects
		return cfg;
	}

	/** Parse a template (or take it from the cache) */
	private static Template getTemplate(String templateText)
	{
		Template template = _templateCache.get(templateText);
		if (template != null)
			return template;

		// A forgotten old (Velocity) template: FreeMarker prints its '#if( $x )' as plain text, or fails on its ${...} with a confusing message. Say what it is.
		String velocityHint = templateText.matches("(?s).*(?<![</])#\\{?(if|foreach|set|elseif)\\}?\\s*\\(.*") ? " (This looks like an old Velocity template: the templates are FreeMarker since DbxTune 2026-11, see https://freemarker.apache.org/docs/dgui.html)" : "";

		try
		{
			template = new Template("AlarmTemplate", new StringReader(templateText), _templateConf);
		}
		catch (ParseException ex)
		{
			throw new TemplateRenderException("FreeMarker Parser Error", ex.getEditorMessage() + " [line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + "]" + velocityHint, ex.getLineNumber(), ex.getColumnNumber(), ex);
		}
		catch (IOException ex) // can not happen with a StringReader
		{
			throw new TemplateRenderException("FreeMarker Parser Error", ex.toString(), 0, 0, ex);
		}

		if ( ! velocityHint.isEmpty() ) // once per template text (it is cached)
			_logger.warn("An alarm template is rendered with the Velocity directives as plain text." + velocityHint + " Template starts with: " + StringUtils.abbreviate(templateText.replace('\n', ' '), 100));

		_templateCache.put(templateText, template);
		return template;
	}

	/** A Java class whose static methods templates can call, eg <code>${StringUtil.format('%-30s', type)}</code> */
	private static Object staticModel(Class<?> clazz)
	{
		try
		{
			TemplateHashModel statics = ((DefaultObjectWrapper) _templateConf.getObjectWrapper()).getStaticModels();
			return statics.get(clazz.getName());
		}
		catch (TemplateModelException ex)
		{
			throw new RuntimeException("Could not expose '" + clazz.getName() + "' to the alarm templates.", ex);
		}
	}

	/** The server group that template previews (and the example Active Alarms Summary) show */
	public static final String EXAMPLE_SERVER_GROUP = "Example Group";

	/**
	 * Variables that a template PREVIEW should see instead of the runtime values, to pass as 'extraContext'.
	 * (In the Template Editor there is no DbxCentral to ask, so <code>${serverGroup}</code> would always be empty.)
	 */
	public static Map<String, Object> createPreviewContext()
	{
		Map<String, Object> map = new HashMap<>();
		map.put("serverGroup"   , EXAMPLE_SERVER_GROUP);
		map.put("hasServerGroup", true);
		return map;
	}

	/** The example alarm a template PREVIEW renders (the Template Editor, the web config page and the template validator) */
	public static AlarmEvent createPreviewExampleEvent()
	{
		AlarmEvent ae = new AlarmEventDummy("GORAN_1_DS", "SomeCmName", "SomeExtraInfo", AlarmEvent.Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);
		ae.createAlarmOptionsMessage(new DummyCm(), "Dummy2"); // fills ${alarmOptions}
		return ae;
	}

	/** A Counter Model with a few alarm settings: the example alarm's <code>${alarmOptions}</code> (the table telling how to change the alarm) */
	private static final String  PROPKEY_alarm_dummy_1         = "DummyCm.alarm.system.on.Dummy1";
	private static final boolean DEFAULT_alarm_dummy_1         = true;

	private static final String  PROPKEY_alarm_dummy_2         = "DummyCm.alarm.system.on.Dummy2";
	private static final boolean DEFAULT_alarm_dummy_2         = true;
	private static final String  PROPKEY_alarm_dummy_2_KeepMsg = "DummyCm.alarm.system.on.Dummy2.keep.regex";
	private static final String  DEFAULT_alarm_dummy_2_KeepMsg = "";
	private static final String  PROPKEY_alarm_dummy_2_SkipMsg = "DummyCm.alarm.system.on.Dummy2.skip.regex";
	private static final String  DEFAULT_alarm_dummy_2_SkipMsg = "(xxx|yyy)";

	private static class DummyCm
	extends CountersModel
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getName()
		{
			return "DummyCm";
		}

		@Override
		public List<CmSettingsHelper> getLocalAlarmSettings()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			List<CmSettingsHelper> list = new ArrayList<>();

			CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
			
			list.add(new CmSettingsHelper("Dummy1"        , isAlarmSwitch, PROPKEY_alarm_dummy_1        , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_dummy_1        , DEFAULT_alarm_dummy_1        ), DEFAULT_alarm_dummy_1          , "On Error 1601, send 'AlarmEventConfigResourceIsUsedUp'." ));

			list.add(new CmSettingsHelper("Dummy2"        , isAlarmSwitch, PROPKEY_alarm_dummy_2        , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_dummy_2        , DEFAULT_alarm_dummy_2        ), DEFAULT_alarm_dummy_2        , "On messages starting with 'background task message:', send 'AlarmEventBackgroundMessage'." ));
			list.add(new CmSettingsHelper("Dummy2 Keep Msg"              , PROPKEY_alarm_dummy_2_KeepMsg, String.class,  conf.getProperty       (PROPKEY_alarm_dummy_2_KeepMsg, DEFAULT_alarm_dummy_2_KeepMsg), DEFAULT_alarm_dummy_2_KeepMsg, "Keep messages in 'Background Message', That contains this RegEx. After this rule the 'skip' rule is evaluated. (blank=disables this rule). " , new RegExpInputValidator() ));
			list.add(new CmSettingsHelper("Dummy2 Skip Msg"              , PROPKEY_alarm_dummy_2_SkipMsg, String.class,  conf.getProperty       (PROPKEY_alarm_dummy_2_SkipMsg, DEFAULT_alarm_dummy_2_SkipMsg), DEFAULT_alarm_dummy_2_SkipMsg, "Skip messages in 'Background Message', That contains this RegEx. Before this rule the 'keep' rule is evaluated. (blank=disables this rule). ", new RegExpInputValidator() ));

			return list;
		}
		
	}

	/** Example alarms for a template PREVIEW: <code>$activeAlarmList</code> and the example Active Alarms Summary */
	public static List<AlarmEvent> createPreviewExampleAlarms()
	{
		List<AlarmEvent> list = new ArrayList<>();
		for (int i=0; i<10; i++)
			list.add(new AlarmEventDummy("GORAN_" + i + "_DS", "SomeCmName-" + i, "SomeExtraInfo-" + i, AlarmEvent.Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 150+i, "This is an Alarm Example with the data value of '" + (150+i) + "'", "Extended Description goes here - " + i, 0));
		return list;
	}

	/**
	 * Base URL of the DbxCentral this Collector can FETCH things from (active alarms, its server group),
	 * or null if this Collector does not talk to DbxCentral.
	 * <p>
	 * Deliberately <b>not</b> {@link AlarmWriterAbstract#getDbxCentralUrl()}: that one never returns blank - when nothing
	 * is configured it derives {@code http://<local hostname>:<port>} so that messages always have
	 * <i>something</i> to link to. Good for a link, useless for a fetch: on a Collector that does not
	 * talk to DbxCentral at all it would make us HTTP GET a host that isn't there, forever.
	 * <p>
	 * Order:
	 * <ol>
	 *   <li>the PCS writer's URL - its presence <b>proves</b> this Collector sends to DbxCentral, and
	 *       it is by definition an address this Collector can reach</li>
	 *   <li>{@code DbxCentral.public.base.url} - explicitly configured, but it is the <i>public</i>
	 *       URL, which may sit behind a proxy we cannot reach from here</li>
	 *   <li>otherwise null - no HTTP call at all</li>
	 * </ol>
	 *
	 * @param conf  Looked in first, then the combined configuration. May be null.
	 */
	public static String getDbxCentralFetchUrl(Configuration conf)
	{
		// 1) do we actually send to DbxCentral? If so, use that host.
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
				_logger.info("Could not parse the PCS URL '" + pcsUrl + "' when looking for DbxCentral. Caught: " + ex);
			}
		}

		// 2) the public base URL, if someone set it explicitly
		String url = getProp(conf, AlarmWriterAbstract.PROPKEY_dbxCentralUrl);
		if (StringUtil.hasValue(url))
			return url;

		// 3) this Collector does not talk to DbxCentral
		return null;
	}

	/** Look in the passed Configuration first, then in the combined one. */
	static String getProp(Configuration conf, String propName)
	{
		String val = conf == null ? null : conf.getProperty(propName, null);
		if (StringUtil.hasValue(val))
			return val;

		return Configuration.getCombinedConfiguration().getProperty(propName, null);
	}

	/**
	 * The name THIS Collector's server is known by in DbxCentral (the schema name in the Central database).
	 * <p>
	 * This mirrors {@code PersistContainer.getServerNameOrAlias()}: the alias if we have one,
	 * otherwise the stripped DBMS server name.
	 * <p>
	 * NOTE: Do <b>not</b> use {@code ICounterController.getServerName()} here -- that one prefers the
	 * <i>displayName</i>, which is not what the Central database uses as the schema name.
	 *
	 * @param fallbackEvent  Used if the CounterController can't tell us. May be null.
	 * @return The name, or null if unknown
	 */
	public static String getCollectorServerName(AlarmEvent fallbackEvent)
	{
		try
		{
			if (CounterController.hasInstance())
			{
				ICounterController cc = CounterController.getInstance();
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
				_logger.debug("Problems getting the Collector server name from the CounterController. Falling back on the AlarmEvent. Caught: " + t, t);
		}

		// Fallback. NOTE: getServiceName() does not always hold the server name (for RepServer WS it
		// holds 'LDS.dbname'), see the TODO in AlarmEvent.private_getServerName()
		return fallbackEvent == null ? null : fallbackEvent.getServiceName();
	}


//	/**
//	 * Take a list of AlarmEvent and fill in the template values...
//	 * 
//	 * @param writerName                  writer who calls this method
//	 * @param activeAlarmList             List of "active" alarms
//	 * @param template                    the FreeMarker template
//	 * @param doTrim                      remove whitespaces newlines etc from start and end
//	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
//	 * @param dbxCentralUrl               Where can the DbxCentral be found;
//	 * @return                            The resolved template
//	 * @throws TemplateRenderException    If the template does not parse, or something in it fails (eg an undefined variable)
//	 */
//	public static String createMessageFromTemplate(String writerName, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
//	{
//		return createMessageFromTemplate(writerName, null, activeAlarmList, template, doTrim, trMap, dbxCentralUrl);
//	}

	/**
	 * Take the AlarmEntry (from DbxTuneSample) and fill in the template values...
	 * 
	 * @param action                      RAISE or RE-RAISE or CANCEL
	 * @param pcsAlarmEntry               The AlarmEntry object (NOTE: This is NOT the same as a AlarmEvent)
	 * @param template                    the FreeMarker template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * @param dbxCentralUrl               Where can the DbxCentral be found;
	 * @return                            The resolved template
	 * @throws TemplateRenderException    If the template does not parse, or something in it fails (eg an undefined variable)
	 */
	public static String createMessageFromTemplate(String action, AlarmEntry pcsAlarmEntry, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
	{
		return createMessageFromTemplate(action, pcsAlarmEntry, null, template, doTrim, trMap, dbxCentralUrl);
	}

	/**
	 * Take the AlarmEvent and fill in the template values...
	 * 
	 * @param action                      RAISE or RE-RAISE or CANCEL
	 * @param alarmEvent                  The AlarmEvent object
	 * @param template                    the FreeMarker template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * @param dbxCentralUrl               Where can the DbxCentral be found;
	 * @return                            The resolved template
	 * @throws TemplateRenderException    If the template does not parse, or something in it fails (eg an undefined variable)
	 */
	/**
	 * The currently active alarms of THIS Collector, which templates see as <code>$activeAlarmList</code>.
	 * <p>
	 * Callers of the long overloads MUST pass this (not null), otherwise <code>$activeAlarmList</code> is
	 * not put in the template model, and any template looping over it fails with an undefined variable.
	 *
	 * @return the live list, or null if there is no AlarmHandler (eg on DbxCentral)
	 */
	public static List<AlarmEvent> getActiveAlarmList()
	{
		return AlarmHandler.hasInstance() ? AlarmHandler.getInstance().getAlarmList() : null;
	}

	public static String createMessageFromTemplate(String action, AlarmEvent alarmEvent, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
	{
		List<AlarmEvent> activeAlarmList = getActiveAlarmList();

		return createMessageFromTemplate(action, alarmEvent, activeAlarmList, template, doTrim, trMap, dbxCentralUrl);
	}

	/**
	 * Take the AlarmEvent and fill in the template values...
	 * 
	 * @param action                      RAISE or RE-RAISE or CANCEL  (or the writer who calls this method) 
	 * @param alarmObject                 The AlarmEvent or pcsAlarmEntry object
	 * @param activeAlarmList             List of "active" alarms
	 * @param template                    the FreeMarker template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * @param dbxCentralUrl               Where can the DbxCentral be found;
	 * 
	 * @return                            The resolved template
	 * @throws TemplateRenderException    If the template does not parse, or something in it fails (eg an undefined variable)
	 */
	public static String createMessageFromTemplate(String action, Object alarmObject, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
	{
		return createMessageFromTemplate(action, alarmObject, activeAlarmList, template, doTrim, trMap, dbxCentralUrl, null, null);
	}

	/**
	 * Same as above, but also exposes a pre-rendered "Active Alarms Summary" to the template, as
	 * <code>${activeAlarmsSummaryHtml}</code> and <code>${activeAlarmsSummaryText}</code>.
	 * <p>
	 * NOTE: Both variables are <b>always</b> put in the template model (as an empty string when a
	 * writer does not supply them), because FreeMarker fails on an undefined variable.
	 * That way a template can use them unconditionally.
	 */
	public static String createMessageFromTemplate(String action, Object alarmObject, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl, String activeAlarmsSummaryHtml, String activeAlarmsSummaryText)
	{
		return createMessageFromTemplate(action, alarmObject, activeAlarmList, template, doTrim, trMap, dbxCentralUrl, activeAlarmsSummaryHtml, activeAlarmsSummaryText, null);
	}

	/**
	 * Same as above, plus writer specific variables.
	 *
	 * @param extraContext  Extra template variables, put in the context <b>as is</b> after all the standard ones
	 *                      (so they override a standard variable with the same name). NOTE: they do NOT pass
	 *                      through <code>trMap</code>, the caller is responsible for escaping them. May be null.
	 */
	public static String createMessageFromTemplate(String action, Object alarmObject, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl, String activeAlarmsSummaryHtml, String activeAlarmsSummaryText, Map<String, Object> extraContext)
	{
		// Since parameter "alarmObject" is Object, we need to check valid classes
		AlarmEvent alarmEvent    = null;
		AlarmEntry pcsAlarmEntry = null;
		
		if (alarmObject instanceof AlarmEvent)
		{
			alarmEvent = (AlarmEvent) alarmObject;
		}
		else if (alarmObject instanceof AlarmEntry)
		{
			pcsAlarmEntry = (AlarmEntry) alarmObject;
		}
		else
		{
			throw new RuntimeException("Unhandled object '" + alarmObject.getClass().getName() + "'. You can only pass 'com.dbxtune.alarm.events.AlarmEvent' and 'com.dbxtune.central.pcs.DbxTuneSample.AlarmEntry' objects to this method.");
		}
		
		String dbxCentralBaseUrl = AlarmWriterAbstract.static_getDbxCentralUrl(); //"http://" + StringUtil.getHostnameWithDomain();
//		try
//		{
//			URI uri = URI.create(dbxCentralUrl.replace(" ", "%20")); // Translate spaces to "%20"
//			dbxCentralBaseUrl = uri.getScheme() + "://" + uri.getRawAuthority();
//		}
//		catch (Exception ex) 
//		{
//			_logger.warn("Problems parsing URL into a BaseUrl. Passed URL=|" + dbxCentralUrl + "|. Caught: " + ex);
//		}
		
		// Parse first (cached): a template that does not parse is reported before we collect any values
		Template fmTemplate = getTemplate(template);

		// Not "starts with": a template can start with directives (<#if>/<#assign>) before the <html>
		boolean isHtmlTemplate = StringUtils.containsIgnoreCase(template, "<html>");


		Map<String, Object> context = new HashMap<>();

		// Java classes whose static methods a template can call
		context.put("StringUtil" , staticModel(StringUtil.class));   // com.dbxtune.utils.StringUtil      eg: ${StringUtil.format('%-30s', type)}
		context.put("Version"    , staticModel(Version.class));      // com.dbxtune.Version               eg: ${Version.getAppName()}
		context.put("StringUtils", staticModel(StringUtils.class));  // org.apache.commons.lang3.StringUtils eg: ${StringUtils.repeat('-', 30)}
		context.put("Json"       , staticModel(TemplateJson.class)); // JSON escaping, for templates that produce JSON: ${Json.str(value)} or "${Json.esc(value)}"
		// NOTE: java.lang.System is NOT exposed (it was with Velocity, but no template used it, and System.exit()/getenv() from a template is a risk)


		// Add TYPE to context
		context.put("type", action);

		// Dbx Central URL
		context.put("dbxCentralUrl", dbxCentralUrl);

		// serverDisplayName
		String serverDisplayName = null;
		if (StringUtil.isNullOrBlank(serverDisplayName) && alarmEvent    != null)           serverDisplayName = alarmEvent   .getServiceName();
		if (StringUtil.isNullOrBlank(serverDisplayName) && pcsAlarmEntry != null)           serverDisplayName = pcsAlarmEntry.getServiceName();
		if (StringUtil.isNullOrBlank(serverDisplayName) && CounterController.hasInstance()) serverDisplayName = CounterController.getInstance().getServerDisplayName();
		// NOTE: through trMap like every other alarm value. It used to be put raw, which is harmless for HTML
		//       but breaks a JSON template (Teams) if the name holds a quote, and a Slack message if it holds & < >
		context.put("serverDisplayName"        , StringUtil.toStr(serverDisplayName, trMap));

		// The DbxCentral SERVER_LIST group THIS Collector's server is in. Only PEEKS at what is already known,
		// so rendering a template never makes an HTTP call. "" when not within any group, or not known (yet).
		String serverGroup = alarmEvent != null ? DbxCentralServerGroup.peek().getNameOrEmpty() : "";
		context.put("serverGroup"              , StringUtil.toStr(serverGroup, trMap));
		context.put("hasServerGroup"           , StringUtil.hasValue(serverGroup));
//FIXME; change the template to be ${serverDisplayName}


//		// ADD information from DbxCentral SERVER_LIST file (if we got any)
//		String serverName = alarmEvent.getServiceName();
//		try
//		{
//			Map<String, DbxCentralServerDescription> dbxCentralSrvMap = DbxCentralServerDescription.getFromFile();
//			DbxCentralServerDescription entry = dbxCentralSrvMap.get(serverName);
//			if (entry != null)
//			{
//				context.put("dbxCentralServerNameDescription", entry.getDescription());
//			}
//			else
//			{
//				_logger.info("No DbxCentral 'SERVER_LIST' was found for serverName '" + serverName + "' in file '" + DbxCentralServerDescription.getDefaultFile() + "'. I wont be able to add template tags for 'SERVER_LIST' for serverName='" + serverName + "'.");
//			}
//		}
//		catch(IOException ex)
//		{
//			_logger.info("Problems reading DBX Central 'SERVER_LIST' file '" + DbxCentralServerDescription.getDefaultFile() + "'. I wont be able to add template tags for 'SERVER_LIST' for serverName='" + serverName + "'.");
//		}
		
		
		if (alarmEvent != null)
		{
			// put (basic) AlarmEvent fields
			context.put("alarmClass"                 , StringUtil.toStr( alarmEvent.getAlarmClass()                 ,trMap ));
			context.put("serviceType"                , StringUtil.toStr( alarmEvent.getServiceType()                ,trMap ));
			context.put("serviceName"                , StringUtil.toStr( alarmEvent.getServiceName()                ,trMap ));
			context.put("serviceInfo"                , StringUtil.toStr( alarmEvent.getServiceInfo()                ,trMap ));
			context.put("extraInfo"                  , StringUtil.toStr( alarmEvent.getExtraInfo()                  ,trMap ));
			context.put("category"                   , StringUtil.toStr( alarmEvent.getCategory()                   ,trMap ));
			context.put("severity"                   , StringUtil.toStr( alarmEvent.getSeverity()                   ,trMap ));
			context.put("state"                      , StringUtil.toStr( alarmEvent.getState()                      ,trMap ));
			context.put("data"                       , StringUtil.toStr( alarmEvent.getData()                       ,trMap ));
			context.put("description"                , StringUtil.toStr( alarmEvent.getDescription()                ,trMap ));

			// And some extra/extended AlarmEvent fields
			context.put("duration"                   , StringUtil.toStr( alarmEvent.getFullDuration(true)           ,trMap ));
			context.put("alarmDuration"              , StringUtil.toStr( alarmEvent.getAlarmDuration()              ,trMap ));
			context.put("fullDuration"               , StringUtil.toStr( alarmEvent.getFullDuration()               ,trMap ));
			context.put("fullDurationAdjustmentInSec", StringUtil.toStr( alarmEvent.getFullDurationAdjustmentInSec(),trMap ));
			context.put("reRaiseCount"               , StringUtil.toStr( alarmEvent.getReRaiseCount()               ,trMap ));
			context.put("crTimeStr"                  , StringUtil.toStr( alarmEvent.getCrTimeStr()                  ,trMap ));
			context.put("reRaiseTimeStr"             , StringUtil.toStr( alarmEvent.getReRaiseTimeStr()             ,trMap ));
			context.put("timeToLive"                 , StringUtil.toStr( alarmEvent.getTimeToLive()                 ,trMap ));
			context.put("alarmClassAbriviated"       , StringUtil.toStr( alarmEvent.getAlarmClassAbriviated()       ,trMap ));
			context.put("extendedDescription"        , StringUtil.toStr( isHtmlTemplate ? alarmEvent.getExtendedDescriptionHtml()        : alarmEvent.getExtendedDescription() ,trMap ));
			context.put("reRaiseDescription"         , StringUtil.toStr( alarmEvent.getReRaiseDescription()         ,trMap ));
			context.put("reRaiseExtendedDescription" , StringUtil.toStr( isHtmlTemplate ? alarmEvent.getReRaiseExtendedDescriptionHtml() : alarmEvent.getReRaiseExtendedDescription() ,trMap ));
			context.put("reRaiseData"                , StringUtil.toStr( alarmEvent.getReRaiseData()                ,trMap ));
			context.put("cancelDescription"          , StringUtil.toStr( alarmEvent.getCancelDescription()          ,trMap ));
			context.put("extendedDescriptionAtCancel", alarmEvent.extendedDescriptionSetAtCancel()); // true: the details are from the CANCEL (how it looks when the alarm went away), not from the raise
			context.put("cancelTimeStr"              , StringUtil.toStr( alarmEvent.getCancelTimeStr()              ,trMap ));
			context.put("crAgeInMs"                  , StringUtil.toStr( alarmEvent.getCrAgeInMs()                  ,trMap ));
			context.put("isActive"                   , StringUtil.toStr( alarmEvent.isActive()                      ,trMap ));
			context.put("activeAlarmCount"           , StringUtil.toStr( alarmEvent.getActiveAlarmCount()           ,trMap ));
			context.put("dbxCentralUrl"              , StringUtil.toStr( alarmEvent.getDbxCentralUrl()              ,trMap ));
			context.put("dbxCentralBaseUrl"          , StringUtil.toStr( dbxCentralBaseUrl                          ,trMap ));
			context.put("alarmId"                    , StringUtil.toStr( alarmEvent.getAlarmId()                    ,trMap ));
			context.put("alarmOptions"               , StringUtil.toStr( alarmEvent.getAlarmOptions()               ,trMap ));
		}
		
		if (pcsAlarmEntry != null)
		{
			// put (basic) AlarmEvent fields
			context.put("alarmClass"                 , StringUtil.toStr( pcsAlarmEntry.getAlarmClass()                 ,trMap ));
			context.put("serviceType"                , StringUtil.toStr( pcsAlarmEntry.getServiceType()                ,trMap ));
			context.put("serviceName"                , StringUtil.toStr( pcsAlarmEntry.getServiceName()                ,trMap ));
			context.put("serviceInfo"                , StringUtil.toStr( pcsAlarmEntry.getServiceInfo()                ,trMap ));
			context.put("extraInfo"                  , StringUtil.toStr( pcsAlarmEntry.getExtraInfo()                  ,trMap ));
			context.put("category"                   , StringUtil.toStr( pcsAlarmEntry.getCategory()                   ,trMap ));
			context.put("severity"                   , StringUtil.toStr( pcsAlarmEntry.getSeverity()                   ,trMap ));
			context.put("state"                      , StringUtil.toStr( pcsAlarmEntry.getState()                      ,trMap ));
			context.put("data"                       , StringUtil.toStr( pcsAlarmEntry.getData()                       ,trMap ));
			context.put("description"                , StringUtil.toStr( pcsAlarmEntry.getDescription()                ,trMap ));

			// And some extra/extended AlarmEvent fields
			context.put("duration"                   , StringUtil.toStr( pcsAlarmEntry.getFullDuration()               ,trMap ));
			context.put("alarmDuration"              , StringUtil.toStr( pcsAlarmEntry.getAlarmDuration()              ,trMap ));
			context.put("fullDuration"               , StringUtil.toStr( pcsAlarmEntry.getFullDuration()               ,trMap ));
			context.put("fullDurationAdjustmentInSec", StringUtil.toStr( pcsAlarmEntry.getFullDurationAdjustmentInSec(),trMap ));
			context.put("reRaiseCount"               , StringUtil.toStr( pcsAlarmEntry.getRepeatCnt()                  ,trMap ));
			context.put("crTimeStr"                  , StringUtil.toStr( pcsAlarmEntry.getCreationTime()               ,trMap ));
			context.put("reRaiseTimeStr"             , StringUtil.toStr( pcsAlarmEntry.getReRaiseTime()                ,trMap ));
			context.put("timeToLive"                 , StringUtil.toStr( pcsAlarmEntry.getTimeToLive()                 ,trMap ));
			context.put("alarmClassAbriviated"       , StringUtil.toStr( pcsAlarmEntry.getAlarmClassAbriviated()       ,trMap ));
			context.put("extendedDescription"        , StringUtil.toStr( pcsAlarmEntry.getExtendedDescription()        ,trMap ));
			context.put("reRaiseDescription"         , StringUtil.toStr( pcsAlarmEntry.getReRaiseDescription()         ,trMap ));
			context.put("reRaiseExtendedDescription" , StringUtil.toStr( pcsAlarmEntry.getReRaiseExtendedDescription() ,trMap ));
			context.put("reRaiseData"                , StringUtil.toStr( pcsAlarmEntry.getReRaiseData()                ,trMap ));
			context.put("cancelDescription"          , StringUtil.toStr( pcsAlarmEntry.getCancelDescription()          ,trMap ));
			context.put("cancelTimeStr"              , StringUtil.toStr( pcsAlarmEntry.getCancelTime()                 ,trMap ));
			context.put("crAgeInMs"                  , StringUtil.toStr( pcsAlarmEntry.getCreationAgeInMs()            ,trMap ));
			context.put("isActive"                   , StringUtil.toStr( pcsAlarmEntry.isActive()                      ,trMap ));
			context.put("activeAlarmCount"           , StringUtil.toStr( -1                                            ,trMap ));
//			context.put("dbxCentralUrl"              , StringUtil.toStr( pcsAlarmEntry.getDbxCentralUrl()              ,trMap ));
			context.put("dbxCentralUrl"              , StringUtil.toStr( "-unknown-"                                   ,trMap ));
			context.put("dbxCentralBaseUrl"          , StringUtil.toStr( dbxCentralBaseUrl                             ,trMap ));
			context.put("alarmId"                    , StringUtil.toStr( pcsAlarmEntry.getAlarmId()                    ,trMap ));
			context.put("alarmOptions"               , StringUtil.toStr( pcsAlarmEntry.getAlarmOptions()               ,trMap ));
		}

		if (activeAlarmList != null)
		{
			context.put("activeAlarmList" , activeAlarmList);
		}

		// Always present, so a template can reference them without an "undefined variable" error.
		// Empty when the writer has the Active Alarms Summary turned off.
		context.put("activeAlarmsSummaryHtml", activeAlarmsSummaryHtml == null ? "" : activeAlarmsSummaryHtml);
		context.put("activeAlarmsSummaryText", activeAlarmsSummaryText == null ? "" : activeAlarmsSummaryText);

		// Writer specific variables (eg the Teams card template). Put as-is, and last, so they win.
		if (extraContext != null)
		{
			for (Map.Entry<String, Object> entry : extraContext.entrySet())
				context.put(entry.getKey(), entry.getValue());
		}


		// Here is where the substitution happens
		StringWriter writer = new StringWriter();
		try
		{
			fmTemplate.process(context, writer);
		}
		catch (TemplateException ex)
		{
			// eg an undefined variable, or a Java method that threw.
			// One line for the editor/log: without FreeMarker's "----" Tip section, and its own '[in template "AlarmTemplate" at ...]' (we add [line, column])
			int    line   = ex.getLineNumber()   == null ? 0 : ex.getLineNumber();
			int    column = ex.getColumnNumber() == null ? 0 : ex.getColumnNumber();
			String msg    = ex.getMessageWithoutStackTop();
			msg = msg.split("\n----", 2)[0].replaceAll("\\s*\\[in template \"AlarmTemplate\"[^\\]]*\\]", "").replaceAll("\\s*\\n\\s*", " ").trim();
			throw new TemplateRenderException("FreeMarker Template Error", msg + " [line " + line + ", column " + column + "]", line, column, ex);
		}
		catch (IOException ex) // can not happen with a StringWriter
		{
			throw new TemplateRenderException("FreeMarker Template Error", ex.toString(), 0, 0, ex);
		}

		String output = writer.toString();
		if (doTrim)
			return output.trim();
		else
			return output;
	}

	public static CompletionProvider createCompletionProvider()
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		Map<String, String> desc = new HashMap<>();
		desc.put("generalDescription"         , "<html> <h2>Just overview information... no variable</h2>"
		                                            + "The text in this editor is a <b>template</b>, and all ${someValue} will be changed into real values.<br>"
		                                            + "Watch the top text panel for how you template will be resolved.<br>"
		                                            + "<br>"
		                                            + "The templates use the <b>FreeMarker</b> template language, see <a href='https://freemarker.apache.org/docs/dgui.html'>https://freemarker.apache.org/docs/dgui.html</a><br>"
		                                            + "<br>"
		                                            + "Some hints...<br>"
		                                            + "<ul>"
		                                            + "   <li>Lines that only hold <code>&lt;#...&gt;</code> tags print nothing (not even a newline), so you can lay out the logic on several lines.</li>"
		                                            + "   <li>Text operations are <i>built-ins</i>, written with a <code>?</code><br>"
		                                            + "       Example 1: <code>${type?lower_case}</code> results in <code>raise</code> instead of RAISE<br>"
		                                            + "       Example 2: <code>${type[1..3]}</code> results in <code>AIS</code> instead of RAISE<br>"
		                                            + "       Example 3: <code>${alarmClass?replace('Dummy', 'Awsome')}</code> results in <code>AlarmEventAwsome</code> instead of <code>AlarmEventDummy</code><br>"
		                                            + "       Example 4: <code>&lt;#if extraInfo?has_content&gt;...&lt;/#if&gt;</code> is true when the value is not empty<br>"
		                                            + "       All built-ins: <a href='https://freemarker.apache.org/docs/ref_builtins_string.html'>https://freemarker.apache.org/docs/ref_builtins_string.html</a>"
		                                            + "   </li>"
		                                            + "   <li>You can use special variable <code>StringUtils</code> for extended String functionality<br>"
		                                            + "       see: <a href='https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html'>https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html</a><br>"
		                                            + "       Example 1: <code>${StringUtils.repeat('-', 30)}</code> gives a line of 30 dashes.<br>"
		                                            + "   </li>"
		                                            + "   <li>You can use special variable <code>StringUtil</code> for some other extended String functionality<br>"
		                                            + "       one of those will be to use the java <code>format(...)</code> to format data.<br>"
		                                            + "       Example 1: <code>${StringUtil.format('%-30s %-10s', alarmClass, type)}</code> which will result in left justified strings with a lenght of 30 and 10.<br>"
		                                            + "   </li>"
		                                            + "   <li>A reusable piece of text is a <i>macro</i>: <code>&lt;#macro row label&gt;&lt;tr&gt;&lt;td&gt;${label}&lt;/td&gt;&lt;td&gt;&lt;#nested&gt;&lt;/td&gt;&lt;/tr&gt;&lt;/#macro&gt;</code>, used as <code>&lt;@row 'Server'&gt;${serverDisplayName}&lt;/@row&gt;</code></li>"
		                                            + "</ul>"
		                                            + "Conditional logic: <code>&lt;#if data == '-1'&gt; XXXX &lt;#else&gt; YYYY &lt;/#if&gt;</code><br>"
		                                            + "For more info see: <a href='https://freemarker.apache.org/docs/ref_directive_if.html'>https://freemarker.apache.org/docs/ref_directive_if.html</a><br>"
		                                            + "<br>"
		                                            + "Below is a small example of that; it will result in different output depending content of the ${type} variable<br>"
		                                            + "<pre>"
		                                            + "&lt;#-- An &lt;#assign&gt; line, uncommented, lets you test the template with another type --&gt;\n"
		                                            + "&lt;#-- &lt;#assign type = 'RE-RAISE'&gt; --&gt;\n"
		                                            + "&lt;#if type == 'RAISE'&gt;\n"
		                                            + "    ${type} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#elseif type == 'RE-RAISE'&gt;\n"
		                                            + "    ${type} - (${duration}) - ${reRaiseCount}:${reRaiseDescription} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#elseif type == 'CANCEL'&gt;\n"
		                                            + "    ${type} (${duration}) - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#else&gt;\n"
		                                            + "    UNKNOWN Action type...\n"
		                                            + "&lt;/#if&gt;\n"
		                                            + "</pre>"
		                                            + "</html>");
		desc.put("type"                       , "<html> <h2>type</h2>"
		                                            + "This is the Alarm Type, typically <b>RAISE</b>, <b>RE-RAISE</b> or <b>CANCEL</b><br>"
		                                            + "<br>"
		                                            + "If you want to generate different text depending on the <i>type</i> then you can use<br>"
		                                            + "the FreeMarker <code>&lt;#if&gt;</code> directive.<br>"
		                                            + "Below is an example of that:"
		                                            + "<pre>"
		                                            + "&lt;#-- An &lt;#assign&gt; line, uncommented, lets you test the template with another type --&gt;\n"
		                                            + "&lt;#-- &lt;#assign type = 'RE-RAISE'&gt; --&gt;\n"
		                                            + "&lt;#if type == 'RAISE'&gt;\n"
		                                            + "    ${type} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#elseif type == 'RE-RAISE'&gt;\n"
		                                            + "    ${type} - (${duration}) - ${reRaiseCount}:${reRaiseDescription} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#elseif type == 'CANCEL'&gt;\n"
		                                            + "    ${type} (${duration}) - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "&lt;#else&gt;\n"
		                                            + "    UNKNOWN Action type...\n"
		                                            + "&lt;/#if&gt;\n"
		                                            + "</pre>"
		                                            + "</html>");
		desc.put("alarmClass"                 , "<html> <h2>alarmClass                 </h2> Class name of the alarm.                                                                                                                             <br><br>Example: <code>AlarmEventHighCpuUtilazation</code>      </html>");
		desc.put("serviceType"                , "<html> <h2>serviceType                </h2> Type of Service, this would typically be <code>" + Version.getAppName() + "<code> </html>");
		desc.put("serviceName"                , "<html> <h2>serviceName                </h2> Name of the service, this would be the DBMS Server Name, or possibly the hostname of the server we are monitoring.                                   <br><br>Example: <code>GORAN_1_DS</code>     </html>");
		desc.put("serviceInfo"                , "<html> <h2>serviceInfo                </h2> Name of the Counter Model that detected the problem.                                                                                                 <br><br>Example: <code>CmSummary</code>      </html>");
		desc.put("extraInfo"                  , "<html> <h2>extraInfo                  </h2> In some cases a Alarm attches extra parameters/information. For instance CmOpenDatabases puts the database name in here.                             <br><br>Example: <code>PML</code>            </html>");
		desc.put("category"                   , "<html> <h2>category                   </h2> What <b>Category</b> this alarm has. Known severities are 'OTHER', 'CPU', 'SPACE', 'SRV_CONFIG', 'LOCK' and 'DOWN'.                                  <br><br>Example: <code>CPU</code>            </html>");
		desc.put("severity"                   , "<html> <h2>severity                   </h2> What <b>Severity</b> this alarm has. Known severities are 'INFO', 'WARNING' and 'ERROR'.                                                             <br><br>Example: <code>WARNING</code>        </html>");
		desc.put("state"                      , "<html> <h2>state                      </h2> What Service <b>State</b> is attached to the alarm. Known states are 'UP', 'AFFECTED' and 'DOWN'.                                                    <br><br>Example: <code>DOWN</code>           </html>");
		desc.put("data"                       , "<html> <h2>data                       </h2> Raw datapoint the alarm was based on. For Example in a AlarmEventHighCpuUtilazation, it will be the CPU Usage in percent   </html>");
		desc.put("description"                , "<html> <h2>description                </h2> A Short text description trying to describe the alam, hopefully with some data points in there as well  </html>");
		desc.put("duration"                   , "<html> <h2>duration                   </h2> In a CANCEL action, this will be for how long the Alarm was active for(including adjustment for setFullDurationAdjustmentInSec()), in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("alarmDuration"              , "<html> <h2>alarmDuration              </h2> In a CANCEL action, this will be for how long the Alarm was active for, in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("fullDuration"               , "<html> <h2>fullDuration               </h2> In a CANCEL action, this will be for how long the Alarm was active for (including adjustment for setFullDurationAdjustmentInSec()), in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("fullDurationAdjustmentInSec", "<html> <h2>fullDurationAdjustmentInSec</h2> value of alarmEvent.getFullDurationAdjustmentInSec()</html>");
		desc.put("reRaiseCount"               , "<html> <h2>reRaiseCount               </h2> How many times has this Alarm been re-raised. (a re-raise is sent every time the alars is still <b>above</b> the threshold, you have specified.  </html>");
		desc.put("crTimeStr"                  , "<html> <h2>crTimeStr                  </h2> When the Alarm was originally Created.                                                                                                               <br><br>Example: <code>2017-09-30 00:34:45.123</code>      </html>");
		desc.put("reRaiseTimeStr"             , "<html> <h2>reRaiseTimeStr             </h2> When the Alarm was re/raised.                                                                                                                        <br><br>Example: <code>2017-09-30 00:34:45.123</code>      </html>");
		desc.put("timeToLive"                 , "<html> <h2>timeToLive                 </h2> Number of milliseconds an alarm is expected to live. This is used if a Counter Collector/Model has the <i>postpone</i> field set. Then that CounterModel will not be sending an new Alarm for next couple of seconds.  </html>");
		desc.put("alarmClassAbriviated"       , "<html> <h2>alarmClassAbriviated       </h2> Same as the <code>alarmClass</code> field, but it's abriviated. (removing <cade>AlarmEvent</code>)                                                   <br><br>Example: <code>HighCpuUtilazation</code> instead of <code>AlarmEventHighCpuUtilazation</code>     </html>");
		desc.put("extendedDescription"        , "<html> <h2>extendedDescription        </h2> In some cases the Alarm might choose to fill in some <b>extra</b> information, which might be usable for example in the <code>AlarmWriterToMail</code> or similar writers. </html>");
		desc.put("reRaiseDescription"         , "<html> <h2>reRaiseDescription         </h2> Same as the <code>description</code>, but this would be the latest description when the Alarm was re-raised.   </html>");
		desc.put("reRaiseExtendedDescription" , "<html> <h2>reRaiseExtendedDescription </h2> Same as the <code>extendedDescription</code>, but this would be the latest extendedDescription when the Alarm was re-raised.   </html>");
		desc.put("reRaiseData"                , "<html> <h2>reRaiseData                </h2> Same as the <code>data</code>, but this would be the latest extendedDescription when the Alarm was re-raised.   </html>");
		desc.put("cancelDescription"          , "<html> <h2>cancelDescription          </h2> Only on <b>CANCEL</b>: a short text about how things look when the Alarm went away. For example the CPU Usage now, or for SrvDown if the DBMS was restarted or not.<br>Empty if the Alarm does not set one.  </html>");
		desc.put("cancelTimeStr"              , "<html> <h2>cancelTimeStr              </h2> What time the Alarm was cancelled, this would only be availabe when <code>type</code> is CANCEL.                                                     <br><br>Example: <code>2017-09-30 00:55:12.345</code>      </html>");
		desc.put("crAgeInMs"                  , "<html> <h2>crAgeInMs                  </h2> How many milleseconds has pased since the Alarm was Created.  </html>");
		desc.put("isActive"                   , "<html> <h2>isActive                   </h2> Boolean status flag if the Alarm is still Active, which would be true when <code>type</code> is RAISE and RE-RAISE.                                  <br><br>Example: <code>true</code> or <code>false</code>      </html>");
		desc.put("activeAlarmCount"           , "<html> <h2>activeAlarmCount           </h2> Get number of <b>active</b> alarms in the AlarmHandler. This can be used to simply print out how many active alarms we have for the moment. </html>");
		desc.put("dbxCentralUrl"              , "<html> <h2>dbxCentralUrl              </h2> Get URL to view the Alarm Period in DbxCentral. </html>");
		desc.put("dbxCentralBaseUrl"          , "<html> <h2>dbxCentralBaseUrl          </h2> Get BASE URL where DbxCentral is located. </html>");
		desc.put("alarmId"                    , "<html> <h2>alarmId                    </h2> Get UUID of the Alarm. </html>");
		desc.put("alarmOptions"               , "<html> <h2>alarmOptions               </h2> Information about how to Disable or Change the Alarm Settings/Options. </html>");
		

		provider.addCompletion(new ShorthandCompletionX(provider, "_[generalDescription]"      , ""                              ,  null, desc.get("generalDescription"        )));
		provider.addCompletion(new ShorthandCompletionX(provider, "type"                       , "${type}"                       ,  null, desc.get("type"                      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmClass"                 , "${alarmClass}"                 ,  null, desc.get("alarmClass"                )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceType"                , "${serviceType}"                ,  null, desc.get("serviceType"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceName"                , "${serviceName}"                ,  null, desc.get("serviceName"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceInfo"                , "${serviceInfo}"                ,  null, desc.get("serviceInfo"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "extraInfo"                  , "${extraInfo}"                  ,  null, desc.get("extraInfo"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "category"                   , "${category}"                   ,  null, desc.get("category"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "severity"                   , "${severity}"                   ,  null, desc.get("severity"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "state"                      , "${state}"                      ,  null, desc.get("state"                     )));
		provider.addCompletion(new ShorthandCompletionX(provider, "data"                       , "${data}"                       ,  null, desc.get("data"                      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "description"                , "${description}"                ,  null, desc.get("description"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "duration"                   , "${duration}"                   ,  null, desc.get("duration"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmDuration"              , "${alarmDuration}"              ,  null, desc.get("alarmDuration"             )));
		provider.addCompletion(new ShorthandCompletionX(provider, "fullDuration"               , "${fullDuration}"               ,  null, desc.get("fullDuration"              )));
		provider.addCompletion(new ShorthandCompletionX(provider, "fullDurationAdjustmentInSec", "${fullDurationAdjustmentInSec}",  null, desc.get("fullDurationAdjustmentInSec")));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseCount"               , "${reRaiseCount}"               ,  null, desc.get("reRaiseCount"              )));
		provider.addCompletion(new ShorthandCompletionX(provider, "crTimeStr"                  , "${crTimeStr}"                  ,  null, desc.get("crTimeStr"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseTimeStr"             , "${reRaiseTimeStr}"             ,  null, desc.get("reRaiseTimeStr"            )));
		provider.addCompletion(new ShorthandCompletionX(provider, "timeToLive"                 , "${timeToLive}"                 ,  null, desc.get("timeToLive"                )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmClassAbriviated"       , "${alarmClassAbriviated}"       ,  null, desc.get("alarmClassAbriviated"      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "extendedDescription"        , "${extendedDescription}"        ,  null, desc.get("extendedDescription"       )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseDescription"         , "${reRaiseDescription}"         ,  null, desc.get("reRaiseDescription"        )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseExtendedDescription" , "${reRaiseExtendedDescription}" ,  null, desc.get("reRaiseExtendedDescription")));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseData"                , "${reRaiseData}"                ,  null, desc.get("reRaiseData"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "cancelDescription"          , "${cancelDescription}"          ,  null, desc.get("cancelDescription"         )));
		provider.addCompletion(new ShorthandCompletionX(provider, "cancelTimeStr"              , "${cancelTimeStr}"              ,  null, desc.get("cancelTimeStr"             )));
		provider.addCompletion(new ShorthandCompletionX(provider, "crAgeInMs"                  , "${crAgeInMs}"                  ,  null, desc.get("crAgeInMs"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "isActive"                   , "${isActive}"                   ,  null, desc.get("isActive"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmCount"           , "${activeAlarmCount}"           ,  null, desc.get("activeAlarmCount"          )));
		provider.addCompletion(new ShorthandCompletionX(provider, "dbxCentralUrl"              , "${dbxCentralUrl}"              ,  null, desc.get("dbxCentralUrl"             )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmId"                    , "${alarmId}"                    ,  null, desc.get("alarmId"                   )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmOptions"               , "${alarmOptions}"               ,  null, desc.get("alarmOptions"              )));

		provider.addCompletion(new ShorthandCompletionX(provider, "serverDisplayName"          , "${serverDisplayName}"          ,  null, "<html>The command line switch <i>--displayName</i> or the ServerName. This can for example be used in the <b>mail subject</b> if the servernames are cryptical.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "serverGroup"                , "${serverGroup}"                ,  null, "<html>The DbxCentral <b>server group</b> this server is in: the <code>#FORMAT; GROUP; name</code> in DbxCentral's SERVER_LIST file.<br>Empty when the server is not within any group, or when the group is not known (yet). The preview shows <code>" + EXAMPLE_SERVER_GROUP + "</code>.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "hasServerGroup"             , "<#if hasServerGroup>${serverGroup}</#if>" ,  null, "<html>true when <code>${serverGroup}</code> has a value.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "dbxCentralUrl"              , "${dbxCentralUrl}"              ,  null, "<html>Some writers want to add a <i>link</i> where the DbxCentral can be located. (easy to click)</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "dbxCentralBaseUrl"          , "${dbxCentralBaseUrl}"          ,  null, "<html>Some writers want to add a <i>link</i> where the DbxCentral can be located. (easy to click)</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmList"            , "<#list activeAlarmList as alarm>\n${alarm.serviceName} - ${alarm.state} - ${alarm.description}\n</#list>" ,  null, "<html>Some writers want to have access to the 'activeAlarmList', where you can loop around the active alarms...</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryHtml"  , "${activeAlarmsSummaryHtml}" ,  null, "<html>A ready made <b>HTML</b> summary of all currently active alarms (this Collector in real time, plus the other servers in the same DbxCentral GROUP).<br>Empty string if '&lt;AlarmWriterName&gt;.activeAlarms.summary.enabled' is false.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryText"  , "${activeAlarmsSummaryText}" ,  null, "<html>A ready made <b>plain text</b> summary of all currently active alarms (this Collector in real time, plus the other servers in the same DbxCentral GROUP).<br>Empty string if '&lt;AlarmWriterName&gt;.activeAlarms.summary.enabled' is false.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "Json.str"                 , "${Json.str(description)}"   ,  null, "<html>A complete, quoted and escaped <b>JSON string</b>. Use it when a template produces JSON (eg the Teams card) and you loop over raw objects like <code>activeAlarmList</code> or <code>activeAlarmsSummaryServers</code>.<br>The standard variables such as <code>${description}</code> are already escaped in a JSON template.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "Json.esc"                 , "${Json.esc(description)}"   ,  null, "<html>Like <code>${Json.str()}</code>, but without the surrounding quotes, for use inside quotes you wrote yourself.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryTeams" , "${activeAlarmsSummaryTeams}" ,  null, "<html><b>Teams card template only.</b> The Active Alarms Summary as ready made Adaptive Card body elements, WITH a leading comma (empty when there is nothing to show). Place it inside the card's <code>\"body\": [ ... ]</code> array, after at least one element.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryServers", "<#list activeAlarmsSummaryServers as srv>\n,{ \"type\": \"TextBlock\", \"text\": ${Json.str(srv.srvName)}, \"weight\": \"Bolder\" }\n<#list srv.alarms as alarm>\n,{ \"type\": \"TextBlock\", \"text\": ${Json.str(alarm)}, \"spacing\": \"None\" }\n</#list>\n</#list>" ,  null, "<html><b>Teams card template only.</b> The Active Alarms Summary as objects, for your own layout: <code>srvName</code>, <code>alarms</code> (a list of lines, one per distinct alarm name) and <code>more</code> (true for the trailing 'and N more server(s)' entry). They are NOT pre-escaped, use <code>${Json.str()}</code>.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "cardTitle"                , "${cardTitle}"              ,  null, "<html><b>Teams card template only.</b> The result of 'AlarmWriterToTeams.title.template', JSON escaped.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "cardDescription"          , "${cardDescription}"        ,  null, "<html><b>Teams card template only.</b> The result of 'AlarmWriterToTeams.desc.template', JSON escaped.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "cardStyle"                , "${cardStyle}"              ,  null, "<html><b>Teams card template only.</b> Adaptive Card Container style for this alarm: <code>good</code> (CANCEL), <code>attention</code> (ERROR), <code>warning</code> (WARNING) or <code>accent</code> (INFO).</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "statusIcon"               , "${statusIcon}"             ,  null, "<html><b>Teams card template only.</b> An emoji for the alarm type: check mark (CANCEL), repeat arrows (RE-RAISE), red circle / warning sign / information sign (RAISE with ERROR / WARNING / INFO). Unlike colour, it also shows in the Teams notification preview.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "statusText"               , "${statusText}"             ,  null, "<html><b>Teams card template only.</b> The alarm type as words: <code>ERROR &middot; NEW ALARM</code>, <code>STILL ACTIVE &middot; WARNING</code> or <code>RESOLVED</code>. JSON escaped.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "statusColor"              , "${statusColor}"            ,  null, "<html><b>Teams card template only.</b> A TextBlock <code>color</code> for the status line: <code>good</code> (CANCEL), <code>attention</code> (ERROR), <code>warning</code> (WARNING) or <code>accent</code> (INFO).</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "crTimeShort"              , "${crTimeShort}"            ,  null, "<html><b>Teams card template only.</b> The raise time as a short time: just <code>HH:mm</code> when it happened today, otherwise <code>yyyy-MM-dd HH:mm</code>. Empty when not set.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseTimeShort"         , "${reRaiseTimeShort}"       ,  null, "<html><b>Teams card template only.</b> The latest re-raise time as a short time: just <code>HH:mm</code> when it happened today, otherwise <code>yyyy-MM-dd HH:mm</code>. Empty when not set.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "cancelTimeShort"          , "${cancelTimeShort}"        ,  null, "<html><b>Teams card template only.</b> The cancel time as a short time: just <code>HH:mm</code> when it happened today, otherwise <code>yyyy-MM-dd HH:mm</code>. Empty when not set.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryCount" , "${activeAlarmsSummaryCount}" ,  null, "<html><b>Teams card template only.</b> How many active alarms the summary holds (0 when there are none, or the summary is off). Use with <code>hasActiveAlarmsSummary</code> to tell 'none' from 'off'.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmsSummaryTeamsItems", "${activeAlarmsSummaryTeamsItems}" ,  null, "<html><b>Teams card template only.</b> The Active Alarms Summary as body elements WITHOUT a leading comma, to be the <code>items</code> of a Container - eg one that is collapsed behind an <code>Action.ToggleVisibility</code>.</html>"));

		provider.addCompletion(new ShorthandCompletionX(provider, "StringUtil"                 , "${StringUtil.format('%-20s', type)}"     ,  null, "<html>Access DbxTune StringUtil, which for example has format(...) see: <a href='https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html'>https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html</a></html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "Version"                    , "${Version.getAppName()}"                  ,  null, "<html>Access DbxTune Version, which has: getAppName(), getBuildStr() </html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "StringUtils"                , "${StringUtils.xxx(type)}"                 ,  null, "<html>Access Apache Commons StringUtils, which has some methods, see: <a href='https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html'>https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html</a></html>"));
		
		return provider;
	}

	public static void main(String[] args)
	{
		Configurator.setRootLevel(Level.TRACE);
		
		AlarmEvent ae = new AlarmEventDummy("GORAN_1_DS", "SomeCmName", "SomeExtraInfo", AlarmEvent.Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);

		String str = createMessageFromTemplate(AlarmWriterAbstract.ACTION_RAISE, ae, "TEST: ${type} - ${alarmClass} --- ${description?truncate(10)}", true, null, "http://DUMMY-dbxcentral:" + DbxTuneCentral.getWebHttpPort());
		System.out.println("OUT: " + str);
	}
}

