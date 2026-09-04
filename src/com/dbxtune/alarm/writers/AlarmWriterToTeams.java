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

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CmSettingsHelper.Type;
import com.dbxtune.cm.CmSettingsHelper.UrlInputValidator;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HttpUtils;
import com.dbxtune.utils.StringUtil;
import com.google.gson.stream.JsonWriter;

/**
 * Send Alarm messages to a Microsoft Teams Channel, via a Power Automate "Workflows" Webhook.
 * <p>
 * NOTE: I have not been able to test this... so this is just a STARTER... changes WILL have to be done :)<br>
 * Especially to the message format/layout
 * <p>
 * NOTE: Microsoft retired the old "Office 365 Connector" incoming webhooks during 2025.<br>
 * The way to post messages into a Teams Channel now is to create a Power Automate Workflow using the
 * trigger "When a Teams webhook request is received", which gives you a URL that accepts a POST with
 * an Adaptive Card wrapped in a <code>{"type": "message", "attachments": [...]}</code> envelope.
 */
public class AlarmWriterToTeams
extends AlarmWriterAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public boolean isCallReRaiseEnabled()
	{
		return _isReRaiseEnabled;
	}

	@Override
	public void raise(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_RAISE, alarmEvent);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_RE_RAISE, alarmEvent);
	}

	@Override
	public void cancel(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_CANCEL, alarmEvent);
	}

	/** One 'route.N' entry: send to 'url' if the server name matches 'regex' */
	private static final class Route
	{
		final String regex;
		final String url;
		Route(String regex, String url) { this.regex = regex; this.url = url; }
	}

	/**
	 * Resolve which Webhook URL to send this AlarmEvent to.<br>
	 * Evaluates the configured routes in order (route.1 .. route.9), first regex that matches
	 * the AlarmEvent's ServiceName (server name) wins. If none matches (or none are configured)
	 * the default/fallback URL (property {@link #PROPKEY_url}) is used.
	 */
	private String resolveUrl(AlarmEvent alarmEvent)
	{
		String serviceName = alarmEvent.getServiceName();

		for (Route route : _routes)
		{
			if (serviceName != null && serviceName.matches(route.regex))
				return route.url;
		}

		return _url;
	}

	/** Adaptive Card 'Container.style' color, based on the alarm state */
	private String getCardStyle(String action, AlarmEvent alarmEvent)
	{
		if (ACTION_CANCEL.equals(action))
			return "good"; // green

		if (AlarmEvent.Severity.ERROR.equals(alarmEvent.getSeverity()))
			return "attention"; // red

		if (AlarmEvent.Severity.WARNING.equals(alarmEvent.getSeverity()))
			return "warning"; // yellow/orange

		return "default";
	}

	/**
	 * Build the full Teams "message" envelope, wrapping an Adaptive Card.
	 */
	private String createTeamsJsonContent(String action, AlarmEvent alarmEvent)
	{
		String title         = WriterUtils.createMessageFromTemplate(action, alarmEvent, _titleTemplate, true, null, alarmEvent.getDbxCentralUrl());
		String description   = WriterUtils.createMessageFromTemplate(action, alarmEvent, _descTemplate,  true, null, alarmEvent.getDbxCentralUrl());
		String cardStyle     = getCardStyle(action, alarmEvent);
		String dbxCentralUrl = alarmEvent.getDbxCentralUrl();

		try
		{
			StringWriter sw = new StringWriter();
			JsonWriter w = new JsonWriter(sw);

			w.beginObject();
			w.name("type").value("message");
			w.name("attachments");
			w.beginArray();
				w.beginObject();
				w.name("contentType").value("application/vnd.microsoft.card.adaptive");
				w.name("content");
				w.beginObject();
					w.name("$schema").value("http://adaptivecards.io/schemas/adaptive-card.json");
					w.name("type").value("AdaptiveCard");
					w.name("version").value("1.4");
					w.name("body");
					w.beginArray();

						// Header: colored Container with the title
						w.beginObject();
						w.name("type").value("Container");
						w.name("style").value(cardStyle);
						w.name("bleed").value(true);
						w.name("items");
						w.beginArray();
							w.beginObject();
							w.name("type").value("TextBlock");
							w.name("text").value(title);
							w.name("weight").value("Bolder");
							w.name("size").value("Medium");
							w.name("wrap").value(true);
							w.endObject();
						w.endArray();
						w.endObject();

						// FactSet: key/value rows built directly from the AlarmEvent
						w.beginObject();
						w.name("type").value("FactSet");
						w.name("facts");
						w.beginArray();
							writeFact(w, "Type",       action);
							writeFact(w, "Server",     alarmEvent.getServiceName());
							writeFact(w, "Alarm",      alarmEvent.getAlarmClassAbriviated());
							writeFact(w, "Collector",  alarmEvent.getServiceInfo());
							writeFact(w, "Category",   "" + alarmEvent.getCategory());
							writeFact(w, "Severity",   "" + alarmEvent.getSeverity());
							writeFact(w, "State",      "" + alarmEvent.getState());
							if (StringUtil.hasValue(alarmEvent.getExtraInfo()))
								writeFact(w, "Info",   alarmEvent.getExtraInfo());

							if (ACTION_CANCEL.equals(action))
							{
								writeFact(w, "Raise Time",  alarmEvent.getCrTimeStr());
								writeFact(w, "Cancel Time", alarmEvent.getCancelTimeStr());
								writeFact(w, "Duration",    alarmEvent.getFullDuration());
							}
							else if (ACTION_RE_RAISE.equals(action))
							{
								writeFact(w, "Raise Time",   alarmEvent.getCrTimeStr());
								writeFact(w, "ReRaise Time", alarmEvent.getReRaiseTimeStr());
							}
							else
							{
								writeFact(w, "Raise Time", alarmEvent.getCrTimeStr());
							}

							writeFact(w, "Active Cnt", "" + alarmEvent.getActiveAlarmCount());
						w.endArray();
						w.endObject();

						// Description
						if (StringUtil.hasValue(description))
						{
							w.beginObject();
							w.name("type").value("TextBlock");
							w.name("text").value(description);
							w.name("wrap").value(true);
							w.name("spacing").value("Medium");
							w.endObject();
						}

						// Extended description (if any), shown in a subtle/monospace block
						String extendedDescription = alarmEvent.getExtendedDescription();
						if (StringUtil.hasValue(extendedDescription))
						{
							w.beginObject();
							w.name("type").value("TextBlock");
							w.name("text").value(extendedDescription);
							w.name("wrap").value(true);
							w.name("isSubtle").value(true);
							w.name("fontType").value("Monospace");
							w.name("spacing").value("Small");
							w.endObject();
						}

						// Button linking to DbxCentral (only if we have a URL)
						if (StringUtil.hasValue(dbxCentralUrl))
						{
							w.beginObject();
							w.name("type").value("ActionSet");
							w.name("actions");
							w.beginArray();
								w.beginObject();
								w.name("type").value("Action.OpenUrl");
								w.name("title").value("View in DbxCentral");
								w.name("url").value(dbxCentralUrl);
								w.endObject();
							w.endArray();
							w.endObject();
						}

					w.endArray(); // body
				w.endObject(); // content
				w.endObject(); // attachment
			w.endArray(); // attachments
			w.endObject(); // root

			w.close();
			return sw.toString();
		}
		catch(IOException ex)
		{
			return "" + ex;
		}
	}

	private void writeFact(JsonWriter w, String title, String value) throws IOException
	{
		w.beginObject();
		w.name("title").value(title);
		w.name("value").value(value == null ? "" : value);
		w.endObject();
	}

	/**
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 */
	private void sendMessage(String action, AlarmEvent alarmEvent)
	{
		String jsonMessage = createTeamsJsonContent(action, alarmEvent);
		String targetUrl   = resolveUrl(alarmEvent);

		if (_logger.isDebugEnabled())
			_logger.debug("SEND-JSON-TEAMS-Message: (url=" + targetUrl + "): " + jsonMessage);

		try
		{
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(targetUrl))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonMessage))
					.build();

			HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			// Check responce
			int responceCode = response.statusCode();
			if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
			{
				throw new Exception("Failed : HTTP error code : " + responceCode);
			}
			else
			{
				_logger.info("Responce code " + responceCode + " (" + HttpUtils.httpResponceCodeToText(responceCode) + "). From URL '" + targetUrl + "'. Sent JSON content: " + jsonMessage);
			}

			// Read responce and print the output...
			for (String output : response.body().split("\n"))
			{
				_logger.info("Responce from server: " + output);
				_logger.debug("Responce from server: " + output);
			}
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			_logger.error("Problems sending REST call to '" + targetUrl + "'. Caught: " + ex, ex);
		}
		catch (Exception ex)
		{
			_logger.error("Problems sending REST call to '" + targetUrl + "'. Caught: " + ex , ex);
		}
	}

	@Override
	public String getDescription()
	{
		return "Write Alarms Messages to a Microsoft Teams Channel (done via: Power Automate 'Workflows' Webhook). Optionally routes different servers to different Channels/URLs based on a ServerName regex.";
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();

		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("URL",             Type.MANDATORY, PROPKEY_url,             String .class, conf.getProperty       (PROPKEY_url,             DEFAULT_url),             DEFAULT_url,             "<html>URL to use when issuing the HTTP POST request. This is the URL you get when creating a Power Automate Workflow using the trigger 'When a Teams webhook request is received'.<br>Setting this up in Teams: In Teams, go to a Channel -> Workflows -> 'Post to a channel when a webhook request is received'<br>This is used as the <b>default/fallback</b> Channel URL if none of the 'route-N' rules below matches the ServerName.</html>", new UrlInputValidator()));
		list.add( new CmSettingsHelper("title-template",  Type.MANDATORY, PROPKEY_titleTemplate,   String .class, conf.getProperty       (PROPKEY_titleTemplate,   DEFAULT_titleTemplate),   DEFAULT_titleTemplate,   "Title Template used at the top of the Card. Note: all ${somValue} will be replaced with runtime values."));
		list.add( new CmSettingsHelper("desc-template",   Type.MANDATORY, PROPKEY_descTemplate,    String .class, conf.getProperty       (PROPKEY_descTemplate,    DEFAULT_descTemplate),    DEFAULT_descTemplate,    "Description Template used in the body of the Card. Note: all ${somValue} will be replaced with runtime values."));
		list.add( new CmSettingsHelper("isReRaiseEnabled",                PROPKEY_isReRaiseEnabled, Boolean.class, conf.getBooleanProperty(PROPKEY_isReRaiseEnabled, DEFAULT_isReRaiseEnabled), DEFAULT_isReRaiseEnabled, "If the Alarm Hander should send an event every time it receives an event. or if it should just be called on RAISE and CANCEL"));

		list.add( new CmSettingsHelper("DbxCentralUrl",                   PROPKEY_dbxCentralUrl,    String .class, conf.getProperty       (PROPKEY_dbxCentralUrl   , DEFAULT_dbxCentralUrl   ), DEFAULT_dbxCentralUrl   , "Where is the DbxCentral located, if you want your template/messages to include it using ${dbxCentralUrl}", new UrlInputValidator()));

		// Routing rules: route-1 .. route-9, first ServerName-regex match wins, otherwise falls back to 'URL' above
		for (int i=1; i<=MAX_ROUTES; i++)
		{
			String propKeyRegex = replaceRouteNum(PROPKEY_routeRegex, i);
			String propKeyUrl   = replaceRouteNum(PROPKEY_routeUrl,   i);

			list.add( new CmSettingsHelper("route-"+i+"-regex", propKeyRegex, String.class, conf.getProperty(propKeyRegex, DEFAULT_routeRegex), DEFAULT_routeRegex, "<html>Route rule #"+i+": If the AlarmEvent's ServerName matches this regexp (full-string match, same as the filter rules above), the message is sent to 'route-"+i+"-url' instead of the default 'URL'.<br>Rules are evaluated in order route-1 -&gt; route-"+MAX_ROUTES+", first match wins. Leave blank to disable this rule.<br>Example: <code>prod.*</code></html>", new RegExpInputValidator()));
			list.add( new CmSettingsHelper("route-"+i+"-url",   propKeyUrl,   String.class, conf.getProperty(propKeyUrl,   DEFAULT_routeUrl  ), DEFAULT_routeUrl,   "The Channel Webhook URL to use when 'route-"+i+"-regex' matches the ServerName.", new UrlInputValidator()));
		}

		return list;
	}

	private static String replaceRouteNum(String propKey, int num)
	{
		return propKey.replace("<N>", ""+num);
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private HttpClient _httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private String  _url              = "";
	private String  _titleTemplate    = "";
	private String  _descTemplate     = "";

	private boolean _isReRaiseEnabled = false;

	private List<Route> _routes       = new ArrayList<>();

	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '" + getName() + "'.");

		_url              = conf.getProperty       (PROPKEY_url,              DEFAULT_url);
		_titleTemplate    = conf.getProperty       (PROPKEY_titleTemplate,    DEFAULT_titleTemplate);
		_descTemplate     = conf.getProperty       (PROPKEY_descTemplate,     DEFAULT_descTemplate);
		_isReRaiseEnabled = conf.getBooleanProperty(PROPKEY_isReRaiseEnabled, DEFAULT_isReRaiseEnabled);

		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_url) )           throw new Exception("The property '" + PROPKEY_url           + "' is mandatory for the AlarmWriter named '" + getName() + "'.");
		if ( StringUtil.isNullOrBlank(_titleTemplate) ) throw new Exception("The property '" + PROPKEY_titleTemplate + "' is mandatory for the AlarmWriter named '" + getName() + "'.");
		if ( StringUtil.isNullOrBlank(_descTemplate) )  throw new Exception("The property '" + PROPKEY_descTemplate  + "' is mandatory for the AlarmWriter named '" + getName() + "'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		// Check if the URL seems to be OK...
		try
		{
			new URL(_url);
		}
		catch(MalformedURLException ex)
		{
			throw new Exception("The URL '" + _url + "' seems to be malformed. Caught: " + ex, ex);
		}

		//------------------------------------------
		// Read & validate routing rules: route.1 .. route.MAX_ROUTES
		// First ServerName-regex match wins, otherwise fall back to '_url'
		//------------------------------------------
		_routes.clear();
		for (int i=1; i<=MAX_ROUTES; i++)
		{
			String propKeyRegex = replaceRouteNum(PROPKEY_routeRegex, i);
			String propKeyUrl   = replaceRouteNum(PROPKEY_routeUrl,   i);

			String regex = conf.getProperty(propKeyRegex, DEFAULT_routeRegex);
			String url   = conf.getProperty(propKeyUrl,   DEFAULT_routeUrl);

			if ( StringUtil.isNullOrBlank(regex) )
				continue; // this route slot is not used

			if ( StringUtil.isNullOrBlank(url) )
				throw new Exception("The property '" + propKeyUrl + "' is mandatory when '" + propKeyRegex + "' is set, for the AlarmWriter named '" + getName() + "'.");

			try
			{
				Pattern.compile(regex);
			}
			catch (PatternSyntaxException ex)
			{
				throw new Exception("The property '" + propKeyRegex + "' with value '" + regex + "' is not a valid regular expression. Caught: " + ex, ex);
			}

			try
			{
				new URL(url);
			}
			catch (MalformedURLException ex)
			{
				throw new Exception("The property '" + propKeyUrl + "' with value '" + url + "' seems to be malformed. Caught: " + ex, ex);
			}

			_routes.add(new Route(regex, url));
		}
	}

	@Override
	public void printConfig()
	{
		int spaces = 35;
		_logger.info("Configuration for Alarm Writer Module: " + getName());
		_logger.info("    " + StringUtil.left(PROPKEY_url,              spaces) + ": " + _url);
		_logger.info("    " + StringUtil.left(PROPKEY_titleTemplate,    spaces) + ": " + _titleTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_descTemplate,     spaces) + ": " + _descTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_isReRaiseEnabled, spaces) + ": " + _isReRaiseEnabled);

		if (_routes.isEmpty())
		{
			_logger.info("    No routing rules configured, all messages goes to the default 'URL' above.");
		}
		else
		{
			_logger.info("    Routing rules (first ServerName-regex match wins, otherwise fallback to the default 'URL' above):");
			for (int i=0; i<_routes.size(); i++)
			{
				Route route = _routes.get(i);
				_logger.info("        [" + (i+1) + "] regex='" + route.regex + "' -> url='" + route.url + "'");
			}
		}
	}

	public static final String  PROPKEY_url              = "AlarmWriterToTeams.url";
	public static final String  DEFAULT_url              = null;

	public static final String  PROPKEY_titleTemplate    = "AlarmWriterToTeams.title.template";
	public static final String  DEFAULT_titleTemplate    = "Alarm message from ${Version.getAppName()}";

	public static final String  PROPKEY_descTemplate     = "AlarmWriterToTeams.desc.template";
	public static final String  DEFAULT_descTemplate     = "${description}";

	public static final String  PROPKEY_isReRaiseEnabled = "AlarmWriterToTeams.isReRaiseEnabled";
	public static final boolean DEFAULT_isReRaiseEnabled = false;

	/** Max number of 'route.N.regex'/'route.N.url' rule slots available */
	public static final int     MAX_ROUTES               = 9;

	public static final String  PROPKEY_routeRegex      = "AlarmWriterToTeams.route.<N>.regex";
	public static final String  DEFAULT_routeRegex      = null;

	public static final String  PROPKEY_routeUrl        = "AlarmWriterToTeams.route.<N>.url";
	public static final String  DEFAULT_routeUrl        = null;
}
