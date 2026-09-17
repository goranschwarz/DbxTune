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
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CmSettingsHelper.Type;
import com.dbxtune.cm.CmSettingsHelper.UrlInputValidator;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HttpUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	/**
	 * One 'route.N' entry: send to 'url' if the server name matches 'regex' AND the DbxCentral server group matches 'groupRegex'.
	 * A null regex/groupRegex is not checked, but at least one of them is set.
	 */
	private static final class Route
	{
		final String regex;
		final String groupRegex;
		final String url;
		Route(String regex, String groupRegex, String url) { this.regex = regex; this.groupRegex = groupRegex; this.url = url; }
	}

	/**
	 * Resolve which Webhook URL to send this AlarmEvent to.<br>
	 * Evaluates the configured routes in order (route.1 .. route.9), first route that matches wins:
	 * its 'regex' (if set) must match the AlarmEvent's ServiceName (server name), and its 'groupRegex' (if set)
	 * must match the DbxCentral server group. If none matches (or none are configured) the default/fallback
	 * URL (property {@link #PROPKEY_url}) is used.
	 * <p>
	 * A server that is not within any group, or whose group is not known (yet), never matches a 'groupRegex'.
	 */
	private String resolveUrl(AlarmEvent alarmEvent)
	{
		String serviceName = alarmEvent.getServiceName();

		// Only resolved when a route actually needs it, since it may have to ask DbxCentral (cached)
		DbxCentralServerGroup.Lookup group = null;

		for (Route route : _routes)
		{
			if (route.regex != null && (serviceName == null || ! serviceName.matches(route.regex)))
				continue;

			if (route.groupRegex != null)
			{
				if (group == null)
					group = DbxCentralServerGroup.get(alarmEvent);

				if ( ! group.isKnown() || ! group.getName().matches(route.groupRegex) )
					continue;
			}

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

		return "accent"; // blue: INFO (used to be "default", which gave INFO no colour at all)
	}

	/**
	 * The emoji that starts the card: it is what makes the alarm type readable at a glance, and unlike colour it
	 * also shows in the Teams activity feed and notification preview.
	 * <ul>
	 *   <li>CANCEL - check mark</li>
	 *   <li>RE-RAISE - repeat arrows</li>
	 *   <li>RAISE - red circle (ERROR), warning sign (WARNING), information sign (INFO and anything else)</li>
	 * </ul>
	 * (The warning and information signs are older than coloured circles, so they render on every client font.)
	 */
	String getStatusIcon(String action, AlarmEvent alarmEvent)
	{
		if (ACTION_CANCEL  .equals(action)) return ICON_RESOLVED;
		if (ACTION_RE_RAISE.equals(action)) return ICON_RERAISE;

		if (AlarmEvent.Severity.ERROR  .equals(alarmEvent.getSeverity())) return ICON_ERROR;
		if (AlarmEvent.Severity.WARNING.equals(alarmEvent.getSeverity())) return ICON_WARNING;
		return ICON_INFO;
	}

	/**
	 * The icon in front of an alarm row in the Active Alarms summary: the same emoji the status line uses for a new
	 * alarm of that severity, so a row reads like the card it came from. A bullet when the severity is unknown
	 * (eg the "+N more" row).
	 */
	static String getSeverityIcon(String severity)
	{
		if ("ERROR"  .equalsIgnoreCase(severity)) return ICON_ERROR;
		if ("WARNING".equalsIgnoreCase(severity)) return ICON_WARNING;
		if ("INFO"   .equalsIgnoreCase(severity)) return ICON_INFO;
		return BULLET;
	}

	/** "ERROR <dot> NEW ALARM", "STILL ACTIVE <dot> WARNING", "RESOLVED" */
	String getStatusText(String action, AlarmEvent alarmEvent)
	{
		String severity = String.valueOf(alarmEvent.getSeverity());

		if (ACTION_CANCEL  .equals(action)) return "RESOLVED";
		if (ACTION_RE_RAISE.equals(action)) return "STILL ACTIVE " + MIDDLE_DOT + " " + severity;
		return severity + " " + MIDDLE_DOT + " NEW ALARM";
	}

	/** TextBlock colour for the status line: good (CANCEL), attention (ERROR), warning (WARNING), accent (INFO) */
	String getStatusColor(String action, AlarmEvent alarmEvent)
	{
		if (ACTION_CANCEL.equals(action)) return "good";

		if (AlarmEvent.Severity.ERROR  .equals(alarmEvent.getSeverity())) return "attention";
		if (AlarmEvent.Severity.WARNING.equals(alarmEvent.getSeverity())) return "warning";
		return "accent";
	}

	private static final DateTimeFormatter SHORT_TIME      = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter SHORT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/**
	 * A short time for the timing line at the top of the card: just <code>HH:mm</code> when 'time' is on the
	 * same day as 'now' (local time zone, same as the full <code>${crTimeStr}</code>), otherwise <code>yyyy-MM-dd HH:mm</code>.
	 *
	 * @param time  epoch milliseconds, 0 or less means "not set" (for example the cancel time of an active alarm)
	 * @param now   epoch milliseconds that decides what "today" is
	 * @return the short time, or "" when 'time' is not set
	 */
	static String toShortTime(long time, long now)
	{
		if (time <= 0)
			return "";

		ZoneId        zone = ZoneId.systemDefault();
		LocalDateTime ldt  = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime();
		boolean       today = ldt.toLocalDate().equals(Instant.ofEpochMilli(now).atZone(zone).toLocalDate());

		return (today ? SHORT_TIME : SHORT_DATE_TIME).format(ldt);
	}

	/**
	 * How much detail the Active Alarms Summary is rendered with.
	 * <p>
	 * We start at the top and step down until the Card fits inside the Microsoft Teams size limit,
	 * so a quiet day gets the informative message and a bad day still gets a message at all.
	 */
	private enum SummaryDetail
	{
		/** One row per alarm, with its complete extraInfo: "LowDbFreeSpace (goran_16)", "LowDbFreeSpace (model)" */
		FULL,

		/** One row per distinct alarm name, counted: "LowDbFreeSpace x3" */
		COMPACT,

		/** No summary at all -- last resort, the alarm itself matters more than the summary */
		NONE
	}

	/**
	 * Build the full Teams "message" envelope, wrapping an Adaptive Card.
	 * <p>
	 * The card is rendered from the Velocity template in {@link #PROPKEY_cardTemplate}, so the layout can
	 * be changed without touching any Java. See {@link #createCardWithFallback} for how a broken template
	 * is prevented from losing the alarm.
	 * <p>
	 * The Active Alarms Summary is rendered as informatively as will fit: we build the Card, and if it
	 * comes out over the Teams size limit we rebuild it with a terser summary. Note that the Card can
	 * be pushed over the limit by the alarm's own extendedDescription (which can hold a lot of SQL
	 * text), so the summary is what gives way, not the alarm.
	 */
	private String createTeamsJsonContent(String action, AlarmEvent alarmEvent)
	{
		// Fetch ONCE, even if we end up rendering the Card more than once
		ActiveAlarmSummary.Result summary = getActiveAlarmSummary(action, alarmEvent);

		String json = null;
		for (SummaryDetail detail : SummaryDetail.values())
		{
			json = createCardWithFallback(action, alarmEvent, summary, detail);

			int sizeInBytes = json.getBytes(StandardCharsets.UTF_8).length;
			if (sizeInBytes <= _summaryMaxCardSizeKb * 1024)
				return json;

			if (_logger.isDebugEnabled())
				_logger.debug("Teams Card was " + sizeInBytes + " bytes, which is above the limit of " + (_summaryMaxCardSizeKb * 1024)
						+ ". Retrying with a terser Active Alarms Summary than '" + detail + "'.");
		}

		// Even without a summary it's too big -- send it anyway, the alarm is what matters
		_logger.info("Teams Card is above the configured limit of " + _summaryMaxCardSizeKb + " KB even without the Active Alarms Summary. Sending it as is.");
		return json;
	}

	/**
	 * Render the card, and make sure that what comes out is valid JSON.
	 * <p>
	 * A card that is not valid JSON is rejected by Teams, which means the alarm is <b>lost</b>. So an
	 * unvalidated card is never sent:
	 * <ol>
	 *   <li>the configured template ({@link #PROPKEY_cardTemplate})</li>
	 *   <li>if that throws, or its output is not valid JSON: the built-in default template, and an ERROR
	 *       in the log (once, until the template works again - so a broken template cannot flood the log)</li>
	 *   <li>if even the built-in template fails: a minimal card that uses no template at all</li>
	 * </ol>
	 */
	private String createCardWithFallback(String action, AlarmEvent alarmEvent, ActiveAlarmSummary.Result summary, SummaryDetail detail)
	{
		boolean isCustomTemplate = ! DEFAULT_cardTemplate.equals(_cardTemplate);

		if (isCustomTemplate)
		{
			try
			{
				String json = renderCard(_cardTemplate, _titleTemplate, _descTemplate, WriterUtils.getActiveAlarmList(), action, alarmEvent, summary, detail);
				validateCardJson(json);

				_hasLoggedTemplateProblem = false;
				return json;
			}
			catch (Throwable t)
			{
				String msg = "AlarmWriter '" + getName() + "': The Teams card template in '" + PROPKEY_cardTemplate + "' failed, "
						+ "so the built-in default card is sent instead. Please fix the template. Caught: " + t;

				if (_hasLoggedTemplateProblem)
				{
					if (_logger.isDebugEnabled())
						_logger.debug(msg, t);
				}
				else
				{
					_hasLoggedTemplateProblem = true;
					_logger.error(msg);
				}
			}
		}

		try
		{
			String json = renderCard(DEFAULT_cardTemplate, _titleTemplate, _descTemplate, WriterUtils.getActiveAlarmList(), action, alarmEvent, summary, detail);
			validateCardJson(json);
			return json;
		}
		catch (Throwable t)
		{
			_logger.error("AlarmWriter '" + getName() + "': The BUILT-IN default Teams card template failed, which is a bug. Sending a minimal card instead. Caught: " + t, t);
			return createLastResortCard(action, alarmEvent);
		}
	}

	/**
	 * Render a card template.
	 * <p>
	 * Every standard variable (<code>${description}</code>, <code>${serviceName}</code>, ...) is JSON escaped,
	 * so a template can simply write <code>"text": "${description}"</code>. On top of those, the card template
	 * gets the Teams specific variables below.
	 */
	String renderCard(String cardTemplate, String titleTemplate, String descTemplate, List<AlarmEvent> activeAlarmList,
			String action, AlarmEvent alarmEvent, ActiveAlarmSummary.Result summary, SummaryDetail detail)
	throws Exception
	{
		return renderCard(cardTemplate, titleTemplate, descTemplate, activeAlarmList, action, alarmEvent, summary, detail, null);
	}

	/**
	 * @param previewContext  Only for the Template Editor preview: variables that replace the runtime values in all three
	 *                        templates, see {@link WriterUtils#createPreviewContext()}. null at runtime.
	 */
	private String renderCard(String cardTemplate, String titleTemplate, String descTemplate, List<AlarmEvent> activeAlarmList,
			String action, AlarmEvent alarmEvent, ActiveAlarmSummary.Result summary, SummaryDetail detail, Map<String, Object> previewContext)
	throws Exception
	{
		// The title/desc templates are plain text: render them WITHOUT escaping, then escape the result ONCE.
		// (Escaping them as well would double escape anything they pull in, eg ${description}.)
		String title       = WriterUtils.createMessageFromTemplate(action, alarmEvent, activeAlarmList, titleTemplate, true, null, alarmEvent.getDbxCentralUrl(), null, null, previewContext);
		String description = WriterUtils.createMessageFromTemplate(action, alarmEvent, activeAlarmList, descTemplate,  true, null, alarmEvent.getDbxCentralUrl(), null, null, previewContext);

		boolean showSummary = summary != null && ! SummaryDetail.NONE.equals(detail);
		boolean full        = SummaryDetail.FULL.equals(detail);

		Map<String, Object> extra = new LinkedHashMap<>();

		// Preview values first; they are plain words, but JSON escape them like every other standard variable
		if (previewContext != null)
		{
			for (Map.Entry<String, Object> entry : previewContext.entrySet())
				extra.put(entry.getKey(), entry.getValue() instanceof String ? TemplateJson.esc((String) entry.getValue()) : entry.getValue());
		}

		// An Adaptive Card starts with a "$schema" key, which Velocity would read as an undefined variable
		// and throw on. Defining it as itself means a card pasted from adaptivecards.io works unchanged.
		extra.put("schema"                    , "$schema");

		extra.put("cardTitle"                 , TemplateJson.esc(title));
		extra.put("cardDescription"           , TemplateJson.esc(description));
		extra.put("cardStyle"                 , getCardStyle(action, alarmEvent));
		extra.put("statusIcon"                , getStatusIcon(action, alarmEvent));
		extra.put("statusText"                , TemplateJson.esc(getStatusText(action, alarmEvent)));
		extra.put("statusColor"               , getStatusColor(action, alarmEvent));

		// Short times for the timing line: "HH:mm" when it happened today, else "yyyy-MM-dd HH:mm" ("" when not set)
		long now = System.currentTimeMillis();
		extra.put("crTimeShort"               , toShortTime(alarmEvent.getCrTime()     , now));
		extra.put("reRaiseTimeShort"          , toShortTime(alarmEvent.getReRaiseTime(), now));
		extra.put("cancelTimeShort"           , toShortTime(alarmEvent.getCancelTime() , now));

		// Decided on the RAW values: an escaped value is never blank ("\n" is two characters), so testing
		// the escaped variable would show an empty-looking block for a description that is only whitespace.
		extra.put("hasCardDescription"        , StringUtil.hasValue(description));
		extra.put("hasExtendedDescription"    , StringUtil.hasValue(alarmEvent.getExtendedDescription()));
		extra.put("extendedDescriptionLength" , alarmEvent.getExtendedDescription() == null ? 0 : alarmEvent.getExtendedDescription().length());
		extra.put("hasExtraInfo"              , StringUtil.hasValue(alarmEvent.getExtraInfo()));
		extra.put("hasDbxCentralUrl"          , StringUtil.hasValue(alarmEvent.getDbxCentralUrl()));

		// The summary as body elements: with a leading comma (to follow another body element), and without one
		// (to be the 'items' of a Container, eg one that is collapsed behind a toggle)
		List<String> summaryElements = showSummary ? createSummaryElements(summary, full) : Collections.emptyList();
		extra.put("activeAlarmsSummaryTeams"     , summaryElements.isEmpty() ? "" : "," + String.join(",", summaryElements));
		extra.put("activeAlarmsSummaryTeamsItems", String.join(",", summaryElements));
		extra.put("hasActiveAlarmsSummary"       , showSummary);
		extra.put("activeAlarmsSummaryCount"     , showSummary ? summary.entries.size() : 0);
		extra.put("activeAlarmsSummaryServers", showSummary ? ActiveAlarmSummary.toServerAlarms(summary, getActiveAlarmSummaryMaxRows(), full) : Collections.emptyList());

		return WriterUtils.createMessageFromTemplate(action, alarmEvent, activeAlarmList, cardTemplate, true,
				TemplateJson.createTranslationMap(), alarmEvent.getDbxCentralUrl(), null, null, extra);
	}

	/**
	 * The Active Alarms Summary as Adaptive Card body elements, one JSON object per element. The template gets them
	 * as <code>${activeAlarmsSummaryTeams}</code> (with a leading comma, to follow another body element) and
	 * <code>${activeAlarmsSummaryTeamsItems}</code> (without, to be the items of a Container).
	 * <pre>
	 * Active Alarms - Sybase Servers (4)
	 * GORAN_UB3_DS
	 *   * OsLoadAverage
	 *   * LowDbFreeSpace (goran_16)
	 *   * LowDbFreeSpace (model)
	 * </pre>
	 * Built with Jackson, so it is correctly escaped whatever the server or alarm names contain.
	 */
	private List<String> createSummaryElements(ActiveAlarmSummary.Result result, boolean full)
	throws IOException
	{
		ObjectMapper   om       = new ObjectMapper();
		List<JsonNode> elements = new ArrayList<>();

		// Header
		ObjectNode header = om.createObjectNode();
		header.put("type", "Container");
		header.put("separator", true);
		header.put("spacing", "Medium");
		ObjectNode headerText = header.putArray("items").addObject();
		headerText.put("type", "TextBlock");
		headerText.put("text", ActiveAlarmSummary.createHeader(result, getActiveAlarmSummaryGroup()));
		headerText.put("weight", "Bolder");
		headerText.put("wrap", true);
		elements.add(header);

		if ( ! result.entries.isEmpty() )
		{
			// One Container per server: the name in bold, then one bullet per alarm.
			// NOTE: separate TextBlocks rather than one markdown list, so the rendering does not depend on how
			//       a particular Teams client treats newlines and "- " inside a TextBlock.
			for (ActiveAlarmSummary.ServerAlarms srv : ActiveAlarmSummary.toServerAlarms(result, getActiveAlarmSummaryMaxRows(), full))
			{
				ObjectNode container = om.createObjectNode();
				container.put("type", "Container");
				container.put("spacing", "Small");
				ArrayNode items = container.putArray("items");

				ObjectNode name = items.addObject();
				name.put("type", "TextBlock");
				name.put("text", srv.getSrvName());
				name.put("wrap", true);
				if (srv.isMore())
					name.put("isSubtle", true);
				else
					name.put("weight", "Bolder");

				for (int a = 0; a < srv.getAlarms().size(); a++)
				{
					ObjectNode line = items.addObject();
					line.put("type", "TextBlock");
					line.put("text", getSeverityIcon(srv.getSeverities().get(a)) + " " + srv.getAlarms().get(a));
					line.put("wrap", true);
					line.put("spacing", "None");
				}

				elements.add(container);
			}

			// Be honest about how fresh the "other servers" part is
			if (ActiveAlarmSummary.showFreshnessNote(result))
			{
				ObjectNode note = om.createObjectNode();
				note.put("type", "TextBlock");
				note.put("text", ActiveAlarmSummary.FRESHNESS_NOTE);
				note.put("wrap", true);
				note.put("isSubtle", true);
				note.put("size", "Small");
				note.put("spacing", "Small");
				elements.add(note);
			}
		}

		List<String> json = new ArrayList<>();
		for (JsonNode element : elements)
			json.add(om.writeValueAsString(element));

		return json;
	}

	/**
	 * Template Editor preview. The card template is rendered exactly like at runtime (JSON escaping, the card
	 * variables, an example summary) and validated, and shown pretty printed. The title/desc templates are plain
	 * templates, and are left to the default.
	 */
	@Override
	public String createTemplatePreview(String propKey, String template, String action, AlarmEvent exampleEvent, List<AlarmEvent> exampleAlarms, Configuration conf)
	throws Exception
	{
		if ( ! PROPKEY_cardTemplate.equals(propKey) )
			return super.createTemplatePreview(propKey, template, action, exampleEvent, exampleAlarms, conf);

		// The card shows the result of the title/desc templates, so use the ones being edited
		String titleTemplate = conf == null ? DEFAULT_titleTemplate : conf.getProperty(PROPKEY_titleTemplate, DEFAULT_titleTemplate);
		String descTemplate  = conf == null ? DEFAULT_descTemplate  : conf.getProperty(PROPKEY_descTemplate,  DEFAULT_descTemplate);

		ActiveAlarmSummary.Result exampleSummary = ActiveAlarmSummary.createExampleResult(exampleAlarms);

		String json = renderCard(template, titleTemplate, descTemplate, exampleAlarms, action, exampleEvent, exampleSummary, SummaryDetail.FULL, WriterUtils.createPreviewContext());

		try
		{
			return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(VALIDATING_READER.readTree(json));
		}
		catch (IOException ex)
		{
			throw new TemplatePreviewException("The template renders, but NOT to valid JSON - at runtime the built-in default card would be sent instead. " + ex.getMessage(), json, ex);
		}
	}

	/** The card template produces a Teams message holding an Adaptive Card. */
	@Override
	public boolean hasDesignerCard(String propKey)
	{
		return PROPKEY_cardTemplate.equals(propKey);
	}

	/**
	 * Just the card: <code>attachments[0].content</code>. The Adaptive Card Designer wants only that, and reports
	 * <i>Unknown property "attachments"</i> / <i>Invalid or missing card type</i> for the whole message.
	 */
	@Override
	public String createDesignerCard(String propKey, String previewText)
	throws Exception
	{
		if ( ! hasDesignerCard(propKey) )
			return super.createDesignerCard(propKey, previewText);

		JsonNode message = VALIDATING_READER.readTree(previewText);
		JsonNode card    = message.path("attachments").path(0).path("content");

		if ( ! card.isObject() )
			throw new Exception("There is no card at 'attachments[0].content' in the message, so there is nothing to copy. "
					+ "The template must keep the Teams message envelope: { \"type\": \"message\", \"attachments\": [ { \"contentType\": ..., \"content\": { ...the card... } } ] }");

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(card);
	}

	/** Strict: throws unless 'json' is exactly one JSON object. Trailing garbage counts as invalid. */
	private static void validateCardJson(String json)
	throws IOException
	{
		JsonNode node = VALIDATING_READER.readTree(json);
		if (node == null || ! node.isObject())
			throw new IOException("The card template did not produce a JSON object.");
	}

	private static final ObjectReader VALIDATING_READER = new ObjectMapper().reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	/** A minimal card that uses no template at all. Only used if even the built-in template fails. */
	private String createLastResortCard(String action, AlarmEvent alarmEvent)
	{
		try
		{
			ObjectMapper om   = new ObjectMapper();
			ObjectNode   root = om.createObjectNode();
			root.put("type", "message");

			ObjectNode attachment = root.putArray("attachments").addObject();
			attachment.put("contentType", "application/vnd.microsoft.card.adaptive");

			ObjectNode content = attachment.putObject("content");
			content.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
			content.put("type", "AdaptiveCard");
			content.put("version", "1.4");
			ArrayNode body = content.putArray("body");

			ObjectNode title = body.addObject();
			title.put("type", "TextBlock");
			title.put("text", action + ": " + alarmEvent.getServiceName() + " - " + alarmEvent.getAlarmClassAbriviated());
			title.put("weight", "Bolder");
			title.put("size", "Medium");
			title.put("wrap", true);

			ObjectNode desc = body.addObject();
			desc.put("type", "TextBlock");
			desc.put("text", StringUtil.toStr(alarmEvent.getDescription()));
			desc.put("wrap", true);

			ObjectNode note = body.addObject();
			note.put("type", "TextBlock");
			note.put("text", "(The card template failed, see the collector log. This is a minimal fallback card.)");
			note.put("isSubtle", true);
			note.put("wrap", true);

			return om.writeValueAsString(root);
		}
		catch (Exception ex)
		{
			// Jackson serializing a small tree of strings - cannot realistically fail
			return "{\"type\":\"message\",\"attachments\":[]}";
		}
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
		list.add( new CmSettingsHelper("card-template",                   PROPKEY_cardTemplate,     String .class, conf.getProperty       (PROPKEY_cardTemplate,     DEFAULT_cardTemplate),     DEFAULT_cardTemplate,     "<html>"
				+ "Velocity template producing the complete Teams message: a <code>{\"type\":\"message\", \"attachments\":[...]}</code> envelope holding an Adaptive Card. "
				+ "Change it to change the layout of the card. Tip: design the card at <code>https://adaptivecards.io/designer</code> (host app: Microsoft Teams) and paste it in.<br>"
				+ "<br>"
				+ "<b>Escaping:</b> all the standard variables (<code>${description}</code>, <code>${serviceName}</code>, ...) are already JSON escaped, so just write <code>\"text\": \"${description}\"</code>. "
				+ "If you loop over raw objects (<code>$activeAlarmList</code>, <code>$activeAlarmsSummaryServers</code>) use <code>$Json.str($value)</code>.<br>"
				+ "<br>"
				+ "<b>Card specific variables:</b> <code>${cardTitle}</code> and <code>${cardDescription}</code> (the results of 'title-template' and 'desc-template'), "
				+ "<code>${cardStyle}</code> (good/attention/warning/accent), "
				+ "<code>${statusIcon}</code>, <code>${statusText}</code> and <code>${statusColor}</code> (an emoji, eg 'ERROR - NEW ALARM' / 'STILL ACTIVE - WARNING' / 'RESOLVED', and a TextBlock colour), "
				+ "<code>${crTimeShort}</code>, <code>${reRaiseTimeShort}</code> and <code>${cancelTimeShort}</code> (just 'HH:mm' when it happened today, else 'yyyy-MM-dd HH:mm', empty when not set; the full times are still in <code>${crTimeStr}</code> etc), "
				+ "<code>$hasCardDescription</code>, <code>$hasExtendedDescription</code>, <code>$hasExtraInfo</code>, <code>$hasDbxCentralUrl</code> (booleans for <code>#if</code>), "
				+ "<code>$extendedDescriptionLength</code> (the length of the extended description BEFORE escaping; when it is 1024 characters or more the built-in card shows a short 'left out' line instead), "
				+ "<code>${activeAlarmsSummaryTeams}</code> (the Active Alarms Summary as ready made body elements, WITH a leading comma), "
				+ "<code>${activeAlarmsSummaryTeamsItems}</code> (the same WITHOUT the leading comma, for the items of a Container), "
				+ "<code>$hasActiveAlarmsSummary</code> and <code>$activeAlarmsSummaryCount</code>, and "
				+ "<code>$activeAlarmsSummaryServers</code> (the same summary as objects: <code>srvName</code>, <code>alarms</code>, <code>severities</code> (one per alarm line), <code>more</code>).<br>"
				+ "<br>"
				+ "<b>Safety:</b> if the template fails or produces invalid JSON, the built-in default card is sent instead and an ERROR is written to the log - an alarm is never lost because of the template.<br>"
				+ "NOTE: this is rendered by Velocity inside DbxTune, so the designer's own templating expressions (<code>${$root.xxx}</code>) are not supported - use the variables above."
				+ "</html>"));
		list.add( new CmSettingsHelper("isReRaiseEnabled",                PROPKEY_isReRaiseEnabled, Boolean.class, conf.getBooleanProperty(PROPKEY_isReRaiseEnabled, DEFAULT_isReRaiseEnabled), DEFAULT_isReRaiseEnabled, "If the Alarm Hander should send an event every time it receives an event. or if it should just be called on RAISE and CANCEL"));

		list.add( new CmSettingsHelper("DbxCentralUrl",                   PROPKEY_dbxCentralUrl,    String .class, conf.getProperty       (PROPKEY_dbxCentralUrl   , DEFAULT_dbxCentralUrl   ), DEFAULT_dbxCentralUrl   , "Where is the DbxCentral located, if you want your template/messages to include it using ${dbxCentralUrl}", new UrlInputValidator()));

		// Active Alarms Summary: the generic settings live in AlarmWriterAbstract, so every writer shares them
		list.addAll( getActiveAlarmSummarySettings() );

		list.add( new CmSettingsHelper("activeAlarms-summary-maxCardSizeKb",         PROPKEY_summaryMaxCardSizeKb, Integer.class, conf.getIntProperty(PROPKEY_summaryMaxCardSizeKb, DEFAULT_summaryMaxCardSizeKb), DEFAULT_summaryMaxCardSizeKb, "<html>Microsoft Teams rejects Adaptive Cards above roughly 28 KB.<br>If the Card ends up bigger than this, the summary is re-rendered in a terser form (dropping the 'extraInfo' and just counting, eg <code>LowDbFreeSpace x3</code>), and as a last resort dropped entirely. The alarm itself is never dropped.</html>"));

		// Routing rules: route-1 .. route-9, first matching route wins, otherwise falls back to 'URL' above
		for (int i=1; i<=MAX_ROUTES; i++)
		{
			String propKeyRegex      = replaceRouteNum(PROPKEY_routeRegex,      i);
			String propKeyGroupRegex = replaceRouteNum(PROPKEY_routeGroupRegex, i);
			String propKeyUrl        = replaceRouteNum(PROPKEY_routeUrl,        i);

			list.add( new CmSettingsHelper("route-"+i+"-regex",      propKeyRegex,      String.class, conf.getProperty(propKeyRegex,      DEFAULT_routeRegex     ), DEFAULT_routeRegex,      "<html>Route rule #"+i+": If the AlarmEvent's ServerName matches this regexp (full-string match, same as the filter rules above), the message is sent to 'route-"+i+"-url' instead of the default 'URL'.<br>If 'route-"+i+"-groupRegex' is also set, BOTH must match.<br>Rules are evaluated in order route-1 -&gt; route-"+MAX_ROUTES+", first match wins. Leave both blank to disable this rule.<br>Example: <code>prod.*</code></html>", new RegExpInputValidator()));
			list.add( new CmSettingsHelper("route-"+i+"-groupRegex", propKeyGroupRegex, String.class, conf.getProperty(propKeyGroupRegex, DEFAULT_routeGroupRegex), DEFAULT_routeGroupRegex, "<html>Route rule #"+i+": If the DbxCentral <b>server group</b> matches this regexp (full-string match), the message is sent to 'route-"+i+"-url' instead of the default 'URL'.<br>If 'route-"+i+"-regex' is also set, BOTH must match.<br>The group is the <code>#FORMAT; GROUP; name</code> this server is in, in DbxCentral's SERVER_LIST file (the overview page layout), so moving a server to another group there also moves its alarms to another channel.<br>A server that is not within any group, or whose group is not known (DbxCentral not reachable since startup, or not configured), never matches, so the next rule or the default 'URL' is used.<br>Example: <code>Sybase.*</code></html>", new RegExpInputValidator()));
			list.add( new CmSettingsHelper("route-"+i+"-url",        propKeyUrl,        String.class, conf.getProperty(propKeyUrl,        DEFAULT_routeUrl       ), DEFAULT_routeUrl,        "The Channel Webhook URL to use when route rule #"+i+" matches.", new UrlInputValidator()));
		}

		return list;
	}

	private static String replaceRouteNum(String propKey, int num)
	{
		return propKey.replace("<N>", ""+num);
	}

	/** The property value as a regexp, null if blank. Throws if it's not a valid regexp. */
	private static String getValidRegexOrNull(Configuration conf, String propKey, String defaultValue)
	throws Exception
	{
		String regex = conf.getProperty(propKey, defaultValue);
		if (StringUtil.isNullOrBlank(regex))
			return null;

		try
		{
			Pattern.compile(regex);
		}
		catch (PatternSyntaxException ex)
		{
			throw new Exception("The property '" + propKey + "' with value '" + regex + "' is not a valid regular expression. Caught: " + ex, ex);
		}
		return regex;
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

	private int                 _summaryMaxCardSizeKb = DEFAULT_summaryMaxCardSizeKb;

	private String              _cardTemplate         = DEFAULT_cardTemplate;

	/** So a broken card template logs ONE error, not one per alarm. Reset when the template works again. */
	private volatile boolean    _hasLoggedTemplateProblem = false;

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
		// First matching route wins (ServerName regex AND/OR server group regex), otherwise fall back to '_url'
		//------------------------------------------
		_routes.clear();
		for (int i=1; i<=MAX_ROUTES; i++)
		{
			String propKeyRegex      = replaceRouteNum(PROPKEY_routeRegex,      i);
			String propKeyGroupRegex = replaceRouteNum(PROPKEY_routeGroupRegex, i);
			String propKeyUrl        = replaceRouteNum(PROPKEY_routeUrl,        i);

			String regex      = getValidRegexOrNull(conf, propKeyRegex,      DEFAULT_routeRegex);
			String groupRegex = getValidRegexOrNull(conf, propKeyGroupRegex, DEFAULT_routeGroupRegex);
			String url        = conf.getProperty(propKeyUrl, DEFAULT_routeUrl);

			if ( regex == null && groupRegex == null )
				continue; // this route slot is not used

			if ( StringUtil.isNullOrBlank(url) )
				throw new Exception("The property '" + propKeyUrl + "' is mandatory when '" + propKeyRegex + "' or '" + propKeyGroupRegex + "' is set, for the AlarmWriter named '" + getName() + "'.");

			try
			{
				new URL(url);
			}
			catch (MalformedURLException ex)
			{
				throw new Exception("The property '" + propKeyUrl + "' with value '" + url + "' seems to be malformed. Caught: " + ex, ex);
			}

			_routes.add(new Route(regex, groupRegex, url));
		}

		if (_routes.stream().anyMatch(r -> r.groupRegex != null) && StringUtil.isNullOrBlank(WriterUtils.getDbxCentralFetchUrl(conf)))
		{
			_logger.warn("AlarmWriter '" + getName() + "': A route has a 'groupRegex', but this Collector does not talk to DbxCentral, so the server group can never be known "
					+ "and those routes will never match. Configure 'PersistWriterToDbxCentral.url' or '" + PROPKEY_dbxCentralUrl + "'.");
		}

		//------------------------------------------
		// Active Alarms Summary
		//------------------------------------------
		_summaryMaxCardSizeKb = conf.getIntProperty(PROPKEY_summaryMaxCardSizeKb, DEFAULT_summaryMaxCardSizeKb);

		//------------------------------------------
		// Card template
		//------------------------------------------
		_cardTemplate = conf.getProperty(PROPKEY_cardTemplate, DEFAULT_cardTemplate);
		if (StringUtil.isNullOrBlank(_cardTemplate))
			_cardTemplate = DEFAULT_cardTemplate;

		// Try it once now, with a dummy alarm, so a broken template shows up in the log at startup
		// rather than at 3 in the morning. It does not stop the writer: a broken template falls back
		// to the built-in card at runtime anyway.
		try
		{
			AlarmEvent dummy = new AlarmEventDummy("dummy-server", "dummy-info", "dummy-extraInfo", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, -1, null, "Dummy description, used to test the card template at startup", "", -1);
			validateCardJson(renderCard(_cardTemplate, _titleTemplate, _descTemplate, Collections.emptyList(), ACTION_RAISE, dummy, null, SummaryDetail.NONE));
		}
		catch (Throwable t)
		{
			_logger.warn("AlarmWriter '" + getName() + "': The Teams card template in '" + PROPKEY_cardTemplate + "' does not work. "
					+ "The built-in default card will be sent until it is fixed. Caught: " + t);
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
		_logger.info("    " + StringUtil.left(PROPKEY_cardTemplate,     spaces) + ": " + (DEFAULT_cardTemplate.equals(_cardTemplate) ? "(built-in default)" : _cardTemplate));
		_logger.info("    " + StringUtil.left(PROPKEY_isReRaiseEnabled, spaces) + ": " + _isReRaiseEnabled);

		// Active Alarms Summary (generic part lives in AlarmWriterAbstract)
		printActiveAlarmSummaryConfig();
		if (isActiveAlarmSummaryEnabled(ACTION_RAISE))
			_logger.info("    " + StringUtil.left(PROPKEY_summaryMaxCardSizeKb, 50) + ": " + _summaryMaxCardSizeKb);

		if (_routes.isEmpty())
		{
			_logger.info("    No routing rules configured, all messages goes to the default 'URL' above.");
		}
		else
		{
			_logger.info("    Routing rules (first match wins, otherwise fallback to the default 'URL' above):");
			for (int i=0; i<_routes.size(); i++)
			{
				Route route = _routes.get(i);
				_logger.info("        [" + (i+1) + "]"
						+ (route.regex      == null ? "" : " regex='"      + route.regex      + "'")
						+ (route.groupRegex == null ? "" : " groupRegex='" + route.groupRegex + "'")
						+ " -> url='" + route.url + "'");
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

	public static final String  PROPKEY_routeGroupRegex = "AlarmWriterToTeams.route.<N>.groupRegex";
	public static final String  DEFAULT_routeGroupRegex = null;

	public static final String  PROPKEY_routeUrl        = "AlarmWriterToTeams.route.<N>.url";
	public static final String  DEFAULT_routeUrl        = null;

	/** Microsoft Teams rejects Adaptive Cards above roughly 28 KB, so stay below that with some margin */
	public static final String  PROPKEY_summaryMaxCardSizeKb = "AlarmWriterToTeams.activeAlarms.summary.maxCardSizeKb";
	public static final int     DEFAULT_summaryMaxCardSizeKb = 25;

	// NOTE: These are built from their code points on purpose, so this source file stays pure ASCII.
	//       build.xml does not set an "encoding" on the <javac> task, which means javac falls back on
	//       the platform default charset -- and these characters are rendered in the Teams Card.
	private static final String BULLET     = Character.toString((char) 0x2022); // "*"   (bullet)
	private static final String MIDDLE_DOT = Character.toString((char) 0x00B7); // "."   (middle dot)

	// Status emoji, built from their code points (see the NOTE above about keeping this file pure ASCII)
	private static final String ICON_ERROR    = codePoints(0x1F534);        // red circle
	private static final String ICON_WARNING  = codePoints(0x26A0, 0xFE0F); // warning sign
	private static final String ICON_INFO     = codePoints(0x2139, 0xFE0F); // information source
	private static final String ICON_RERAISE  = codePoints(0x1F501);        // clockwise arrows (repeat)
	private static final String ICON_RESOLVED = codePoints(0x2705);         // check mark button

	private static String codePoints(int... codePoints)
	{
		return new String(codePoints, 0, codePoints.length);
	}

	public static final String  PROPKEY_cardTemplate     = "AlarmWriterToTeams.card.template";
	public static final String  DEFAULT_cardTemplate     = createDefaultCardTemplate();

	/**
	 * The built-in card template.
	 * <ul>
	 *   <li>A coloured status band: an emoji + a coloured status word (${statusIcon} ${statusText}), then
	 *       "server <dot> alarm", then the title template - so RAISE / RE-RAISE / CANCEL and ERROR / WARNING / INFO
	 *       can be told apart at a glance, in light and dark theme, and in the notification preview.</li>
	 *   <li>The description, and one line of timing that depends on the action.</li>
	 *   <li>The details (facts + extended description) and the Active Alarms summary are collapsed behind
	 *       toggle buttons, so the card stays short. A resolved alarm (CANCEL) gets no details.</li>
	 * </ul>
	 * Every optional body element starts with a comma, so it can be left out without breaking the JSON.
	 * <code>\\u00b7</code> in the text is the JSON escape for a middle dot.
	 */
	public static String createDefaultCardTemplate()
	{
		return ""
			+ "{\n"
			+ "  \"type\": \"message\",\n"
			+ "  \"summary\": \"$type - ${serverDisplayName} - ${alarmClassAbriviated}\",\n" // instead of just saying: "Workflows" or "Workflows posted a new message" -- This might be used instead... For the moment it does NOT work... But who know in the future...
			+ "  \"attachments\": [\n"
			+ "    {\n"
			+ "      \"contentType\": \"application/vnd.microsoft.card.adaptive\",\n"
			+ "      \"content\": {\n"
			+ "        \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\",\n"
			+ "        \"type\": \"AdaptiveCard\",\n"
			+ "        \"version\": \"1.4\",\n"
			+ "        \"previewText\": \"$type - ${serverDisplayName} - ${alarmClassAbriviated}\",\n" // instead of just saying: "Workflows" or "Workflows posted a new message" -- This might be used instead... For the moment it does NOT work... But who know in the future...
			+ "        \"body\": [\n"
			+ "\n"
			+ "          {\n"
			+ "            \"type\": \"Container\", \"style\": \"${cardStyle}\", \"bleed\": true,\n"
			+ "            \"items\": [\n"
			+ "              { \"type\": \"TextBlock\", \"text\": \"${statusIcon} ${statusText}\", \"weight\": \"Bolder\", \"size\": \"Medium\", \"color\": \"${statusColor}\", \"wrap\": true },\n"
			+ "              { \"type\": \"TextBlock\", \"text\": \"${serverDisplayName} \\u00b7 ${alarmClassAbriviated}\", \"weight\": \"Bolder\", \"size\": \"Large\", \"wrap\": true, \"spacing\": \"Small\" },\n"
			+ "              { \"type\": \"TextBlock\", \"text\": \"${cardTitle}\", \"isSubtle\": true, \"size\": \"Small\", \"wrap\": true, \"spacing\": \"None\" }\n"
			+ "            ]\n"
			+ "          }\n"
			+ "\n"
			+ "#if( $hasCardDescription )\n"
			+ "         ,{ \"type\": \"TextBlock\", \"text\": \"${cardDescription}\", \"wrap\": true, \"spacing\": \"Medium\" }\n"
			+ "#end\n"
			+ "#if( $type == 'CANCEL' )\n"
			+ "         ,{ \"type\": \"TextBlock\", \"text\": \"Was ${severity} \\u00b7 lasted ${fullDuration} \\u00b7 resolved ${cancelTimeShort}\", \"isSubtle\": true, \"size\": \"Small\", \"wrap\": true, \"spacing\": \"Small\" }\n"
			+ "#elseif( $type == 'RE-RAISE' )\n"
			+ "         ,{ \"type\": \"TextBlock\", \"text\": \"Active for ${fullDuration} \\u00b7 re-raised ${reRaiseCount} times \\u00b7 first raised ${crTimeShort}\", \"isSubtle\": true, \"size\": \"Small\", \"wrap\": true, \"spacing\": \"Small\" }\n"
			+ "#else\n"
			+ "         ,{ \"type\": \"TextBlock\", \"text\": \"Raised ${crTimeShort}\", \"isSubtle\": true, \"size\": \"Small\", \"wrap\": true, \"spacing\": \"Small\" }\n"
			+ "#end\n"
			+ "\n"
			+ "#if( $type != 'CANCEL' )\n"
			+ "         ,{\n"
			+ "            \"type\": \"Container\", \"id\": \"details\", \"isVisible\": false, \"separator\": true, \"spacing\": \"Medium\",\n"
			+ "            \"items\": [\n"
			+ "              {\n"
			+ "                \"type\": \"FactSet\",\n"
			+ "                \"facts\": [\n"
			+ "                  { \"title\": \"Type\",         \"value\": \"${type}\" },\n"
			+ "                  { \"title\": \"Server\",       \"value\": \"${serviceName}\" },\n"
			+ "                  { \"title\": \"Alarm\",        \"value\": \"${alarmClassAbriviated}\" },\n"
			+ "                  { \"title\": \"Collector\",    \"value\": \"${serviceInfo}\" },\n"
			+ "                  { \"title\": \"Category\",     \"value\": \"${category}\" },\n"
			+ "                  { \"title\": \"Severity\",     \"value\": \"${severity}\" },\n"
			+ "                  { \"title\": \"State\",        \"value\": \"${state}\" }\n"
			+ "#if( $hasExtraInfo )\n"
			+ "                 ,{ \"title\": \"Info\",         \"value\": \"${extraInfo}\" }\n"
			+ "#end\n"
			+ "                 ,{ \"title\": \"Raise Time\",   \"value\": \"${crTimeStr}\" }\n"
			+ "#if( $type == 'RE-RAISE' )\n"
			+ "                 ,{ \"title\": \"ReRaise Time\", \"value\": \"${reRaiseTimeStr}\" }\n"
			+ "#end\n"
			+ "                 ,{ \"title\": \"Active Cnt\",   \"value\": \"${activeAlarmCount}\" }\n"
			+ "                ]\n"
			+ "              }\n"
			+ "#if( $hasExtendedDescription && $extendedDescriptionLength < 1024 )\n"
			+ "             ,{ \"type\": \"TextBlock\", \"text\": \"${extendedDescription}\", \"wrap\": true, \"isSubtle\": true, \"fontType\": \"Monospace\", \"spacing\": \"Small\" }\n"
			+ "#elseif( $hasExtendedDescription )\n"
			+ "             ,{ \"type\": \"TextBlock\", \"text\": \"Extended description left out (${extendedDescriptionLength} characters)#if( $hasDbxCentralUrl ) - see DbxCentral#end\", \"wrap\": true, \"isSubtle\": true, \"size\": \"Small\", \"spacing\": \"Small\" }\n"
			+ "#end\n"
			+ "            ]\n"
			+ "          }\n"
			+ "#end\n"
			+ "\n"
			+ "#if( $activeAlarmsSummaryCount > 0 )\n"
			+ "         ,{ \"type\": \"Container\", \"id\": \"activeAlarms\", \"isVisible\": false, \"separator\": true, \"spacing\": \"Medium\", \"items\": [ ${activeAlarmsSummaryTeamsItems} ] }\n"
			+ "#elseif( $hasActiveAlarmsSummary )\n"
			+ "         ,{ \"type\": \"TextBlock\", \"text\": \"No other active alarms\", \"isSubtle\": true, \"size\": \"Small\", \"wrap\": true, \"spacing\": \"Medium\" }\n"
			+ "#end\n"
			+ "\n"
			+ "#if( $type != 'CANCEL' || $activeAlarmsSummaryCount > 0 || $hasDbxCentralUrl )\n"
			+ "         ,{\n"
			+ "            \"type\": \"ActionSet\", \"spacing\": \"Medium\",\n"
			+ "            \"actions\": [\n"
			+ "#set( $sep = \"\" )\n"
			+ "#if( $type != 'CANCEL' )\n"
			+ "              ${sep}{ \"type\": \"Action.ToggleVisibility\", \"title\": \"Details\", \"targetElements\": [ \"details\" ] }\n"
			+ "#set( $sep = \",\" )\n"
			+ "#end\n"
			+ "#if( $activeAlarmsSummaryCount > 0 )\n"
			+ "              ${sep}{ \"type\": \"Action.ToggleVisibility\", \"title\": \"Active alarms (${activeAlarmsSummaryCount})\", \"targetElements\": [ \"activeAlarms\" ] }\n"
			+ "#set( $sep = \",\" )\n"
			+ "#end\n"
			+ "#if( $hasDbxCentralUrl )\n"
			+ "              ${sep}{ \"type\": \"Action.OpenUrl\", \"title\": \"View in DbxCentral\", \"url\": \"${dbxCentralUrl}\" }\n"
			+ "#end\n"
			+ "            ]\n"
			+ "          }\n"
			+ "#end\n"
			+ "        ]\n"
			+ "      }\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}\n"
			;
	}

}
