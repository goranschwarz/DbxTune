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
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JTextArea;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.fife.ui.autocomplete.Completion;
import org.reflections.Reflections;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.writers.AlarmWriterAbstract;
import com.dbxtune.alarm.writers.AlarmWriterToMail;
import com.dbxtune.alarm.writers.DbxCentralServerGroup;
import com.dbxtune.alarm.writers.WriterUtils;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.ValidationException;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.pcs.report.senders.MailHelper;
import com.dbxtune.pcs.report.senders.ReportSenderToMail;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Try an AlarmWriter setting WITHOUT saving it, with this collector's own code. Used by DbxCentral's config page.
 * <p>
 * <code>POST /api/mgt/writer/try</code> with one of:
 * <ul>
 *   <li><code>{"op":"mailRoutingInfo"}</code> - values for the mail rule editor: alarm names, categories, severities, states, actions, and this server's DbxCentral group (if known)</li>
 *   <li><code>{"op":"mailRouting", "value":"&lt;the 'to' value&gt;", "sample":{"serverName":"..", "serverGroup":"..", "alarmName":"..", "category":"..", "severity":"..", "state":"..", "action":"RAISE", "time":1761650000000}}</code><br>
 *       Validates the value with {@link MailHelper}, and if a 'sample' is passed: who would get the mail. Nothing is saved.</li>
 *   <li><code>{"op":"templateInfo", "writerName":"&lt;class&gt;", "propKey":"&lt;a template setting&gt;"}</code> - for the template editor:
 *       the variables (the Template Editor's completion list), complete templates to start from, and if there is an Adaptive Card</li>
 *   <li><code>{"op":"templatePreview", "writerName":"..", "propKey":"..", "template":"..", "action":"RAISE", "values":{"&lt;propKey&gt;":"&lt;unsaved value&gt;"}, "designerCard":false}</code><br>
 *       Renders the template the way the writer renders it ({@link AlarmWriterAbstract#createTemplatePreview}, like the Template Editor),
 *       and checks it the way a save does ('validationError'). Nothing is saved.</li>
 * </ul>
 * Same authorization as {@link NoGuiConfigSetServlet} (the token DbxCentral's proxy sends).
 */
public class AlarmWriterTryServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** AlarmEvent short names ('FullTranLog'), found once */
	private static volatile List<String> _alarmNames = null;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String auth           = req.getHeader("Authorization");
		String expectedBasic  = System.getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Basic);
		String expectedBearer = System.getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Bearer);
		if (auth == null || ! (auth.equals(expectedBasic) || auth.equals(expectedBearer)) )
		{
			_logger.warn("Unauthorized writer/try attempt from host '" + req.getRemoteHost() + "'.");
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Map<String, Object> out = new LinkedHashMap<>();

		try
		{
			JsonNode root = mapper.readTree(IOUtils.toString(req.getReader()));
			String op = root.path("op").asText("");

			if ("mailRoutingInfo".equals(op))
			{
				DbxCentralServerGroup.Lookup group = DbxCentralServerGroup.peek(); // no call to DbxCentral from here
				out.put("status"     , "success");
				out.put("alarmNames" , getAlarmNames());
				out.put("categories" , MailHelper.CATEGORY_VALUES);
				out.put("severities" , MailHelper.SEVERITY_VALUES);
				out.put("states"     , MailHelper.STATE_VALUES);
				out.put("actions"    , MailHelper.ACTION_VALUES);
				out.put("validKeys"  , MailHelper.VALID_KEYS);
				out.put("serverGroup", group.isKnown() ? group.getName() : null);
			}
			else if ("mailRouting".equals(op))
			{
				// 'savedOnly' (DbxCentral sets it for a user that is not admin): check/try the SAVED rules of 'propKey', never a value from the request.
				// Only the mail recipient settings: any other key would let anybody read a saved value (eg a password) through 'csv'.
				boolean savedOnly = root.path("savedOnly").asBoolean(false);
				String  value     = root.path("value").asText("");
				if (savedOnly)
				{
					String propKey = root.path("propKey").asText("");
					if ( ! propKey.equals(AlarmWriterToMail.PROPKEY_to) && ! propKey.equals(ReportSenderToMail.PROPKEY_to) )
						throw new IllegalArgumentException("'" + propKey + "' is not a mail recipient setting.");
					value = Configuration.getCombinedConfiguration().getProperty(propKey, "");
				}
				out.put("savedOnly", savedOnly);
				MailHelper.Rules rules = MailHelper.parse(value);

				out.put("status"   , "success");
				out.put("isCsv"    , rules.isCsv());
				out.put("csv"      , rules.getCsv());
				out.put("jsonError", rules.getJsonError());
				out.put("errors"   , rules.getErrors());
				out.put("warnings" , rules.getWarnings());

				List<Map<String, Object>> ruleList = new ArrayList<>();
				for (MailHelper.Rule r : rules.getRules())
				{
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("index"    , r.getIndex());
					m.put("name"     , r.getName());
					m.put("valid"    , r.isValid());
					m.put("errors"   , r.getErrors());
					m.put("warnings" , r.getWarnings());
					m.put("alarmOnly", r.isAlarmOnly());
					ruleList.add(m);
				}
				out.put("rules", ruleList);

				if (root.hasNonNull("sample"))
				{
					MailHelper.Sample sample = mapper.treeToValue(root.get("sample"), MailHelper.Sample.class);
					MailHelper.Recipients res = MailHelper.getRecipients(rules, sample);

					Map<String, Object> m = new LinkedHashMap<>();
					m.put("to"          , res.getTo());
					m.put("cc"          , res.getCc());
					m.put("matchedRules", res.getMatchedRules());
					m.put("usedFallback", res.isUsedFallback());
					out.put("result", m);
				}
			}
			else if ("templateInfo".equals(op) || "templatePreview".equals(op))
			{
				String writerName = root.path("writerName").asText("");
				String propKey    = root.path("propKey").asText("");

				// Only AlarmWriters, checked BEFORE the class is initialized (so no static code runs for any other class name)
				Class<?> clazz = Class.forName(writerName, false, getClass().getClassLoader());
				if ( ! AlarmWriterAbstract.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers()) )
					throw new IllegalArgumentException("'" + writerName + "' is not an AlarmWriter.");

				// Like the Template Editor: a new instance, NOT init()'ed (mandatory settings may not be filled in yet, and init() may talk to the network)
				AlarmWriterAbstract writer = (AlarmWriterAbstract) clazz.getDeclaredConstructor().newInstance();
				List<CmSettingsHelper> settings = writer.getAvailableSettings();
				CmSettingsHelper setting = null;
				for (CmSettingsHelper sh : settings)
					if (propKey.equals(sh.getPropName()))
						setting = sh;
				if (setting == null || ! CmSettingsHelper.TemplateInputValidator.class.getSimpleName().equals(setting.getInputValidatorClassName()))
					throw new IllegalArgumentException("'" + propKey + "' is not a template of the AlarmWriter '" + writerName + "'.");

				out.put("status", "success");
				out.put("hasDesignerCard", writer.hasDesignerCard(propKey));

				if ("templateInfo".equals(op))
				{
					// The variables: the Template Editor's completion list (the completions for an empty text = all of them)
					List<Map<String, Object>> vars = new ArrayList<>();
					Set<String> seen = new HashSet<>();
					for (Completion c : WriterUtils.createCompletionProvider().getCompletions(new JTextArea()))
					{
						if (StringUtil.isNullOrBlank(c.getReplacementText())) // the "_[generalDescription]" entry: help, not a variable
						{
							out.put("help", c.getSummary());
							continue;
						}
						if ( ! seen.add(c.getInputText()) )
							continue;
						Map<String, Object> m = new LinkedHashMap<>();
						m.put("name"  , c.getInputText());
						m.put("insert", c.getReplacementText());
						m.put("desc"  , c.getSummary());
						vars.add(m);
					}
					out.put("variables", vars);

					// Complete templates to start from. The Swing settings table offers the same two for 'Msg-Template' (AlarmWriterSettingsTable popup)
					List<Map<String, Object>> alts = new ArrayList<>();
					if (writer instanceof AlarmWriterToMail && AlarmWriterToMail.PROPKEY_msgBodyTemplate.equals(propKey))
					{
						Map<String, Object> html = new LinkedHashMap<>();
						html.put("label", "HTML default");
						html.put("value", AlarmWriterToMail.createHtmlMsgBodyTemplate());
						alts.add(html);

						Map<String, Object> text = new LinkedHashMap<>();
						text.put("label", "Text default");
						text.put("value", AlarmWriterToMail.createTextMsgBodyTemplate());
						alts.add(text);
					}
					out.put("alternatives", alts);
				}
				else
				{
					// 'savedOnly' (DbxCentral sets it for a user that is not admin): render the SAVED value, never a template from the request,
					// since a template can reach Java classes ($System...)
					boolean savedOnly = root.path("savedOnly").asBoolean(false);
					String template = savedOnly ? StringUtil.toStr(setting.getStringValue()) : root.path("template").asText("");
					String action   = root.path("action").asText(AlarmWriterAbstract.ACTION_RAISE);
					out.put("savedOnly", savedOnly);

					// The configuration being edited, as the Template Editor gets it from the Swing settings table: every setting's
					// current value, with the page's unsaved values of this writer on top (eg the Teams card shows the edited title)
					Configuration conf = new Configuration();
					for (CmSettingsHelper sh : settings)
						if (sh.getPropName() != null && sh.getStringValue() != null)
							conf.setProperty(sh.getPropName(), sh.getStringValue());
					if ( ! savedOnly )
						root.path("values").fields().forEachRemaining(e -> conf.setProperty(e.getKey(), e.getValue().asText("")));

					try
					{
						String text = writer.createTemplatePreview(propKey, template, action, WriterUtils.createPreviewExampleEvent(), WriterUtils.createPreviewExampleAlarms(), conf);
						out.put("text", text);

						if (root.path("designerCard").asBoolean(false))
						{
							try { out.put("card", writer.createDesignerCard(propKey, text)); }
							catch (Exception ex) { out.put("cardError", ex.getMessage()); }
						}
					}
					catch (Exception ex)
					{
						// The same words as the Template Editor, and where it is (when Velocity knows)
						int line = -1, column = -1;
						if (ex instanceof ParseErrorException)        { line = ((ParseErrorException)       ex).getLineNumber(); column = ((ParseErrorException)       ex).getColumnNumber(); }
						if (ex instanceof MethodInvocationException)  { line = ((MethodInvocationException) ex).getLineNumber(); column = ((MethodInvocationException) ex).getColumnNumber(); }

						Map<String, Object> m = new LinkedHashMap<>();
						m.put("type"   , CmSettingsHelper.TemplateInputValidator.getErrorType(ex));
						m.put("message", ex.getMessage());
						m.put("line"   , line);
						m.put("column" , column);
						out.put("error", m);

						// It rendered, but the result is not usable: show WHAT came out as well
						if (ex instanceof AlarmWriterAbstract.TemplatePreviewException)
							out.put("previewText", ((AlarmWriterAbstract.TemplatePreviewException) ex).getPreviewText());
					}

					// The check a save does (all actions), so the page can tell before saving
					try
					{
						setting.isValidInput(template);
						out.put("validationError", null);
					}
					catch (ValidationException ex)
					{
						out.put("validationError", ex.getMessage());
					}
				}
			}
			else
			{
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.put("status", "FAILED");
				out.put("error" , "Unknown op '" + op + "', expecting 'mailRoutingInfo', 'mailRouting', 'templateInfo' or 'templatePreview'.");
			}
		}
		catch (Exception ex)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.put("status", "FAILED");
			out.put("error" , "Problems with the request: " + ex.getMessage());
		}

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		mapper.writeValue(resp.getWriter(), out);
	}

	/** Short names of all AlarmEvent classes ('AlarmEventFullTranLog' -> 'FullTranLog'), sorted */
	private static List<String> getAlarmNames()
	{
		if (_alarmNames == null)
		{
			TreeSet<String> names = new TreeSet<>();
			for (Class<? extends AlarmEvent> clazz : new Reflections("com.dbxtune.alarm.events").getSubTypesOf(AlarmEvent.class))
			{
				// Skip abstract base classes and internal events (eg AlarmEvent_EndOfScan)
				if (Modifier.isAbstract(clazz.getModifiers()) || clazz.getSimpleName().contains("_"))
					continue;

				String name = clazz.getSimpleName();
				names.add(name.startsWith("AlarmEvent") ? name.substring("AlarmEvent".length()) : name);
			}
			_alarmNames = new ArrayList<>(names);
		}
		return _alarmNames;
	}
}
