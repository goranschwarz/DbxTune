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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.pcs.report.senders;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.alarm.writers.AlarmWriterAbstract;
import com.dbxtune.alarm.writers.DbxCentralServerGroup;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import it.sauronsoftware.cron4j.SchedulingPattern;

/**
 * Who should get a mail.
 * <p>
 * The 'to' property (eg <code>AlarmWriterToMail.to</code> or <code>ReportSenderToMail.to</code>) is either:
 * <ul>
 *   <li>A comma separated list of mail addresses: everybody gets everything</li>
 *   <li>A JSON array of rules:
 * <pre>
 * [
 *   {"name":"DBA - all prod", "serverName":"PROD_.*", "to":"dba@acme.com"},
 *   {"name":"ERP team", "serverName":"PROD_ERP", "alarmName":"(FullTranLog|SrvDown)", "to":"erp@acme.com", "cc":"erp-lead@acme.com"},
 *   {"name":"On-call nights", "serverGroup":"Production.*", "severity":"ERROR", "action":"RAISE", "time":"* 0-7,18-23 * * *", "to":"oncall@acme.com"},
 *   {"fallback":true, "to":"dba@acme.com"}
 * ]
 * </pre>
 *   A rule matches when ALL of its conditions match (regexp, full match, same as the AlarmWriter filters).
 *   The recipients of ALL matching rules are combined. A 'fallback' rule is only used when no other rule matched.
 *   <ul>
 *     <li>Conditions: serverName, serverGroup, alarmName, category, severity, state, action, time</li>
 *     <li>Other keys: to, cc, name, enabled, comment (and any key starting with '_' is a comment)</li>
 *   </ul>
 *   A rule with an unknown key, or a bad regexp/cron/value, is rejected (it never matches), the other rules still work.
 *   </li>
 * </ul>
 */
public class MailHelper
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args)
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		List<String> list;

		// test-1
		System.setProperty("xxx.test.1", "test1@acme.com");
		list = getMailToAddressForServerNameAsList("", null, "xxx.test.1", "test1");
		System.out.println("test.1: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test1@acme.com"})) ? "OK" : "---- FAIL ----");


		// test-2
		System.setProperty("xxx.test.2", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test2.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test2.srv2@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", null, "xxx.test.2", "test2");
		System.out.println("test.2: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test2.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");


		// test-3
		AlarmEvent ae = new AlarmEventDummy("test", "test", "test", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, 0, null, null, null, 0);
		System.setProperty("xxx.test.3", ("[ "
				+ "{#serverName#:#srv1#, #alarmName#:#Dummy#, #to#:#test3.onlyDummyAlarm.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv1#,                      #to#:#test3.allAlarms.srv1@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", ae, "xxx.test.3", "test3");
		System.out.println("test.3: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test3.onlyDummyAlarm.srv1@json.acme.com", "test3.allAlarms.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");


		// test-4 (regex)
		ae = new AlarmEventDummy("test", "test", "test", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, 0, null, null, null, 0);
		System.setProperty("xxx.test.4", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test4.onlyDummyAlarm.srv1@json.acme.com#, #alarmName#:#(Dummy1|Dummy2|Dummy)# }, "
				+ "{#serverName#:#srv1#, #to#:#test4.allAlarms.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv1#, #to#:#test4.noMatch.srv1@json.acme.com#, #alarmName#:#(Dummy1|Dummy2)# } "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", ae, "xxx.test.4", "test4");
		System.out.println("test.4: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test4.onlyDummyAlarm.srv1@json.acme.com", "test4.allAlarms.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");


		// test-5 -- FALLBACK
		System.setProperty("xxx.test.5", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test5.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test5.srv2@json.acme.com#}, "
				+ "{#fallback#:true, #to#:#test5.srvFallback1@json.acme.com, test5.srvFallback2@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("unknownSrv", null, "xxx.test.5", "test5");
		System.out.println("test.5: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test5.srvFallback1@json.acme.com", "test5.srvFallback2@json.acme.com"})) ? "OK" : "---- FAIL ----");


		// test-6 -- simple but with 2 mail addresses (and a 'fallback', which should NOT be used)
		System.setProperty("xxx.test.6", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test6.srv1.m1@json.acme.com, test6.srv1.m2@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test6.srv2@json.acme.com#}, "
				+ "{#fallback#:true, #to#:#test6.srvFallback1@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", null, "xxx.test.6", "test6");
		System.out.println("test.6: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test6.srv1.m1@json.acme.com", "test6.srv1.m2@json.acme.com"})) ? "OK" : "---- FAIL ----");
	}

	//----------------------------------------------------------------
	// BEGIN: Rules
	//----------------------------------------------------------------
	public static final List<String> VALID_KEYS     = Collections.unmodifiableList(Arrays.asList("name", "enabled", "fallback", "serverName", "serverGroup", "alarmName", "category", "severity", "state", "action", "time", "to", "cc", "comment"));
	public static final List<String> ACTION_VALUES  = Collections.unmodifiableList(Arrays.asList(AlarmWriterAbstract.ACTION_RAISE, AlarmWriterAbstract.ACTION_RE_RAISE, AlarmWriterAbstract.ACTION_CANCEL));
	public static final List<String> CATEGORY_VALUES = enumNames(AlarmEvent.Category.values());
	public static final List<String> SEVERITY_VALUES = enumNames(AlarmEvent.Severity.values());
	public static final List<String> STATE_VALUES    = enumNames(AlarmEvent.ServiceState.values());

	private static List<String> enumNames(Enum<?>[] values)
	{
		List<String> list = new ArrayList<>();
		for (Enum<?> e : values)
			list.add(e.name());
		return Collections.unmodifiableList(list);
	}

	/**
	 * What a rule is matched against.<br>
	 * Public fields, so the Web UI tester can send it as JSON.
	 */
	public static class Sample
	{
		public String serverName;
		public String serverGroup; // null = not known, or not in any group: a 'serverGroup' rule never matches
		public String alarmName;   // short name, eg 'FullTranLog'. null = not an alarm (a report): rules with alarm conditions are skipped
		public String category;
		public String severity;
		public String state;
		public String action;      // RAISE, RE-RAISE, CANCEL
		public long   time;        // when the alarm was raised (ms). 0 = now

		/** The sample for an alarm (or a report if alarmEvent is null). The serverGroup is NOT resolved here. */
		public static Sample of(String serverName, AlarmEvent alarmEvent, String action)
		{
			Sample s = new Sample();
			s.serverName = serverName;
			s.action     = action;
			if (alarmEvent != null)
			{
				// Long or short name: 'AlarmEventXxx' or 'Xxx'
				String alarmName = alarmEvent.getClass().getSimpleName();
				if (alarmName.startsWith("AlarmEvent"))
					alarmName = alarmName.substring("AlarmEvent".length());

				s.alarmName = alarmName;
				s.category  = alarmEvent.getCategory() + "";
				s.severity  = alarmEvent.getSeverity() + "";
				s.state     = alarmEvent.getState()    + "";
				s.time      = alarmEvent.getCrTime();
			}
			return s;
		}
	}

	/** One entry in the JSON array */
	public static class Rule
	{
		private final int    _index; // 1 based position in the JSON array
		private final String _json;  // the entry as written

		private String  _name;
		private boolean _enabled  = true;
		private boolean _fallback = false;

		private Pattern _serverName;
		private Pattern _serverGroup;
		private Pattern _alarmName;
		private Pattern _category;
		private Pattern _severity;
		private Pattern _state;
		private Pattern _action;
		private String            _time;
		private SchedulingPattern _timePattern;
		private boolean           _timeNegate;

		private final Set<String>  _to       = new LinkedHashSet<>();
		private final Set<String>  _cc       = new LinkedHashSet<>();
		private final List<String> _errors   = new ArrayList<>();
		private final List<String> _warnings = new ArrayList<>();

		private Rule(int index, String json)
		{
			_index = index;
			_json  = json;
		}

		public int          getIndex()    { return _index; }
		public String       getJson()     { return _json; }
		public String       getName()     { return StringUtil.hasValue(_name) ? _name : "#" + _index; }
		public boolean      isEnabled()   { return _enabled; }
		public boolean      isFallback()  { return _fallback; }
		public boolean      isValid()     { return _errors.isEmpty(); }
		public List<String> getErrors()   { return _errors; }
		public List<String> getWarnings() { return _warnings; }
		public Set<String>  getTo()       { return _to; }
		public Set<String>  getCc()       { return _cc; }

		/** Has a condition that only means something for an alarm (so it never matches a report) */
		public boolean isAlarmOnly()
		{
			return _alarmName != null || _category != null || _severity != null || _state != null || _action != null || _timePattern != null;
		}

		private boolean hasConditions()
		{
			return _serverName != null || _serverGroup != null || isAlarmOnly();
		}

		/** One line for logs: <code>#2 'ERP team': serverName='PROD_ERP' alarmName='(FullTranLog|SrvDown)' -> to=[erp@acme.com] cc=[lead@acme.com]</code> */
		public String describe()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("#").append(_index);
			if (StringUtil.hasValue(_name)) sb.append(" '").append(_name).append("'");
			sb.append(":");
			if (!_enabled)              sb.append(" (DISABLED)");
			if (_fallback)              sb.append(" fallback");
			if (_serverName  != null)   sb.append(" serverName='" ).append(_serverName .pattern()).append("'");
			if (_serverGroup != null)   sb.append(" serverGroup='").append(_serverGroup.pattern()).append("'");
			if (_alarmName   != null)   sb.append(" alarmName='"  ).append(_alarmName  .pattern()).append("'");
			if (_category    != null)   sb.append(" category='"   ).append(_category   .pattern()).append("'");
			if (_severity    != null)   sb.append(" severity='"   ).append(_severity   .pattern()).append("'");
			if (_state       != null)   sb.append(" state='"      ).append(_state      .pattern()).append("'");
			if (_action      != null)   sb.append(" action='"     ).append(_action     .pattern()).append("'");
			if (_time        != null)   sb.append(" time='"       ).append(_time).append("'");
			sb.append(" -> to=").append(_to);
			if (!_cc.isEmpty())         sb.append(" cc=").append(_cc);
			if (!_errors.isEmpty())     sb.append("  <<-- REJECTED: ").append(String.join("; ", _errors));
			return sb.toString();
		}
	}

	/** The parsed 'to' value: a CSV list, or the JSON rules */
	public static class Rules
	{
		private final String       _raw;
		private final boolean      _isJson;
		private final List<String> _csv   = new ArrayList<>();
		private final List<Rule>   _rules = new ArrayList<>();
		private String             _jsonError; // the whole value is unusable
		private final AtomicBoolean _hasLoggedErrors = new AtomicBoolean(false);

		private Rules(String raw)
		{
			_raw = raw == null ? "" : raw;

			String trimmed = _raw.trim();
			_isJson = trimmed.startsWith("[") || trimmed.startsWith("{");

			if ( ! _isJson )
			{
				_csv.addAll( StringUtil.commaStrToList(_raw, true) );
				return;
			}

			JsonElement root;
			try
			{
				root = new JsonParser().parse(_raw);
			}
			catch (RuntimeException ex)
			{
				_jsonError = "The value looks like JSON but can not be parsed (NO mail will be sent): " + ex.getMessage();
				return;
			}

			if ( ! root.isJsonArray() )
			{
				_jsonError = "The JSON must be an array of rules: [ {...}, {...} ] (NO mail will be sent)";
				return;
			}

			JsonArray jsonArr = root.getAsJsonArray();
			if (jsonArr.size() == 0)
			{
				_jsonError = "The JSON array has no rules (NO mail will be sent)";
				return;
			}

			int index = 0;
			for (JsonElement elem : jsonArr)
				_rules.add(parseRule(++index, elem));
		}

		public String     getRaw()       { return _raw; }
		public boolean    isJson()       { return _isJson; }
		public boolean    isCsv()        { return !_isJson; }
		public List<String> getCsv()     { return _csv; }
		public List<Rule> getRules()     { return _rules; }
		public String     getJsonError() { return _jsonError; }

		/** The JSON error, or one line per rejected rule. Empty when everything is OK. */
		public List<String> getErrors()
		{
			List<String> list = new ArrayList<>();
			if (_jsonError != null)
				list.add(_jsonError);

			for (Rule r : _rules)
				if ( ! r.isValid() )
					list.add("Rule " + r.getName() + " is IGNORED: " + String.join("; ", r.getErrors()) + "  JSON: " + r.getJson());
			return list;
		}

		public boolean hasErrors()
		{
			return _jsonError != null || _rules.stream().anyMatch(r -> !r.isValid());
		}

		/** Rule warnings (not errors) */
		public List<String> getWarnings()
		{
			List<String> list = new ArrayList<>();
			for (Rule r : _rules)
				for (String w : r.getWarnings())
					list.add("Rule " + r.getName() + ": " + w);
			return list;
		}

		/** Does any (usable) rule look at the server group? Then we need to ask DbxCentral for it. */
		public boolean needsServerGroup()
		{
			return _rules.stream().anyMatch(r -> r.isValid() && r.isEnabled() && r._serverGroup != null);
		}
	}

	/** The result: who gets the mail, and why */
	public static class Recipients
	{
		private final Set<String>  _to           = new LinkedHashSet<>();
		private final Set<String>  _cc           = new LinkedHashSet<>();
		private final List<String> _matchedRules = new ArrayList<>();
		private boolean            _usedFallback = false;

		public List<String> getTo()           { return new ArrayList<>(_to); }
		public List<String> getCc()           { return new ArrayList<>(_cc); }
		public List<String> getMatchedRules() { return _matchedRules; }
		public boolean      isUsedFallback()  { return _usedFallback; }
		public boolean      isEmpty()         { return _to.isEmpty() && _cc.isEmpty(); }
	}

	/** Parsed values are cached on the raw string: the 'to' property is read on every send, but rarely changes */
	private static final Map<String, Rules> _parseCache = new ConcurrentHashMap<>();

	/** Parse (and validate) a 'to' value. Cached, so it is cheap to call on every send. Never null. */
	public static Rules parse(String toStr)
	{
		String raw = toStr == null ? "" : toStr;

		Rules rules = _parseCache.get(raw);
		if (rules == null)
		{
			rules = new Rules(raw);
			if (_parseCache.size() > 50) // do not grow forever (eg values typed in the Web UI tester)
				_parseCache.clear();
			_parseCache.put(raw, rules);
		}
		return rules;
	}

	private static Rule parseRule(int index, JsonElement elem)
	{
		Rule r = new Rule(index, elem.toString());

		if ( ! elem.isJsonObject() )
		{
			r._errors.add("is not a JSON object {...}");
			return r;
		}

		for (Map.Entry<String, JsonElement> entry : elem.getAsJsonObject().entrySet())
		{
			String      key = entry.getKey();
			JsonElement val = entry.getValue();

			// Comments: "comment":"..." or any key starting with '_'
			if ("comment".equals(key) || key.startsWith("_"))
				continue;

			if ( ! val.isJsonPrimitive() )
			{
				r._errors.add("'" + key + "' must be a string (or true/false), not: " + val);
				continue;
			}
			String str = val.getAsString().trim();

			switch (key)
			{
			case "name":        r._name        = str; break;
			case "enabled":     r._enabled     = parseBoolean(r, key, str); break;
			case "fallback":    r._fallback    = parseBoolean(r, key, str); break;
			case "to":          r._to.addAll( StringUtil.parseCommaStrToSet(str) ); break;
			case "cc":          r._cc.addAll( StringUtil.parseCommaStrToSet(str) ); break;
			case "serverName":  r._serverName  = parseRegex(r, key, str, null); break;
			case "serverGroup": r._serverGroup = parseRegex(r, key, str, null); break;
			case "alarmName":   r._alarmName   = parseRegex(r, key, str.replace("AlarmEvent", ""), null); break; // Long or short name: 'AlarmEventXxx' or 'Xxx'
			case "category":    r._category    = parseRegex(r, key, str, CATEGORY_VALUES); break;
			case "severity":    r._severity    = parseRegex(r, key, str, SEVERITY_VALUES); break;
			case "state":       r._state       = parseRegex(r, key, str, STATE_VALUES); break;
			case "action":      r._action      = parseRegex(r, key, str, ACTION_VALUES); break;
			case "time":
				if (StringUtil.hasValue(str))
				{
					// a leading '!' means: NOT within the time range (same as the alarm time ranges)
					String cron = str.startsWith("!") ? str.substring(1).trim() : str;
					try
					{
						r._timePattern = new SchedulingPattern(cron);
						r._timeNegate  = str.startsWith("!");
						r._time        = str;
					}
					catch (RuntimeException ex)
					{
						r._errors.add("'time' value '" + str + "' is not a valid cron pattern (minute hour day-of-month month day-of-week): " + ex.getMessage());
					}
				}
				break;
			default:
				r._errors.add("unknown key '" + key + "' (valid keys: " + String.join(", ", VALID_KEYS) + ")");
			}
		}

		// Checks on the whole rule
		if (r._to.isEmpty() && r._cc.isEmpty())
			r._errors.add("has no mail address in 'to' (or 'cc')");

		if (r._fallback && r.hasConditions())
			r._errors.add("a 'fallback' rule can not have conditions (it is only used when no other rule matched)");

		if ( ! r._fallback && ! r.hasConditions() )
			r._errors.add("has no condition, use \"serverName\":\".*\" to match all servers, or \"fallback\":true");

		for (String addr : r._to) if (addr.indexOf('@') < 0) r._warnings.add("'" + addr + "' in 'to' does not look like a mail address");
		for (String addr : r._cc) if (addr.indexOf('@') < 0) r._warnings.add("'" + addr + "' in 'cc' does not look like a mail address");

		return r;
	}

	private static boolean parseBoolean(Rule r, String key, String str)
	{
		if ("true" .equalsIgnoreCase(str)) return true;
		if ("false".equalsIgnoreCase(str)) return false;
		r._errors.add("'" + key + "' must be true or false, not '" + str + "'");
		return false;
	}

	/** A full-match regexp, or null when blank (= not used). If 'knownValues' is passed, it must match at least one of them (catches typos like 'EROR'). */
	private static Pattern parseRegex(Rule r, String key, String str, List<String> knownValues)
	{
		if (StringUtil.isNullOrBlank(str))
			return null;

		final Pattern p;
		try
		{
			p = Pattern.compile(str);
		}
		catch (PatternSyntaxException ex)
		{
			r._errors.add("'" + key + "' value '" + str + "' is not a valid regexp: " + ex.getDescription());
			return null;
		}

		if (knownValues != null && knownValues.stream().noneMatch(v -> p.matcher(v).matches()))
			r._errors.add("'" + key + "' value '" + str + "' matches none of: " + String.join(", ", knownValues));

		return p;
	}

	/** Does this (valid and enabled) rule match the sample? Fallback rules are handled by the caller. */
	private static boolean isMatch(Rule r, Sample s)
	{
		// A report (no alarm) never matches a rule with alarm conditions
		if (s.alarmName == null && r.isAlarmOnly())
			return false;

		// 'serverGroup': a server whose group is not known, or that is not in any group, never matches
		if (r._serverGroup != null && s.serverGroup == null)
			return false;

		if ( ! fullMatch(r._serverName , s.serverName ) ) return false;
		if ( ! fullMatch(r._serverGroup, s.serverGroup) ) return false;
		if ( ! fullMatch(r._alarmName  , s.alarmName  ) ) return false;
		if ( ! fullMatch(r._category   , s.category   ) ) return false;
		if ( ! fullMatch(r._severity   , s.severity   ) ) return false;
		if ( ! fullMatch(r._state      , s.state      ) ) return false;
		if ( ! fullMatch(r._action     , s.action     ) ) return false;

		// 'time' is checked against when the alarm was RAISED, so a CANCEL goes to the same people as its RAISE
		if (r._timePattern != null)
		{
			boolean within = r._timePattern.match(s.time > 0 ? s.time : System.currentTimeMillis());
			if (within == r._timeNegate)
				return false;
		}

		return true;
	}

	private static boolean fullMatch(Pattern p, String value)
	{
		return p == null || p.matcher(value == null ? "" : value).matches();
	}

	/**
	 * Who gets the mail for this sample. No logging and no lookups, so the Web UI tester can use it too.
	 */
	public static Recipients getRecipients(Rules rules, Sample sample)
	{
		Recipients res = new Recipients();

		if (rules.isCsv())
		{
			res._to.addAll(rules.getCsv());
			return res;
		}

		Set<String> fallbackTo = new LinkedHashSet<>();
		Set<String> fallbackCc = new LinkedHashSet<>();
		List<String> fallbackNames = new ArrayList<>();

		for (Rule r : rules.getRules())
		{
			if ( ! r.isValid() || ! r.isEnabled() )
				continue;

			if (r.isFallback())
			{
				fallbackTo.addAll(r._to);
				fallbackCc.addAll(r._cc);
				fallbackNames.add(r.getName());
			}
			else if (isMatch(r, sample))
			{
				res._to.addAll(r._to);
				res._cc.addAll(r._cc);
				res._matchedRules.add(r.getName());
			}
		}

		if (res._matchedRules.isEmpty() && ! fallbackNames.isEmpty())
		{
			res._to.addAll(fallbackTo);
			res._cc.addAll(fallbackCc);
			res._matchedRules.addAll(fallbackNames);
			res._usedFallback = true;
		}

		// Somebody in both 'to' and 'cc' only needs one copy
		res._cc.removeAll(res._to);

		return res;
	}

	/**
	 * Who gets the mail for an alarm (or a report when alarmEvent is null).
	 * <p>
	 * Reads the property from the combined configuration on every call (so changes apply at the next send),
	 * and only asks DbxCentral for the server group if a rule uses 'serverGroup'.
	 *
	 * @param serverName   Name of the server
	 * @param alarmEvent   The alarm, or null for a report
	 * @param action       RAISE, RE-RAISE, CANCEL (null for a report)
	 * @param propKeyTo    The property, eg 'AlarmWriterToMail.to'
	 * @param defaultTo    Used if the property is not found
	 * @throws RuntimeException if the property is empty
	 */
	public static Recipients getRecipients(String serverName, AlarmEvent alarmEvent, String action, String propKeyTo, String defaultTo)
	{
		String toStr = Configuration.getCombinedConfiguration().getProperty(propKeyTo, defaultTo);

		if (StringUtil.isNullOrBlank(toStr))
			throw new RuntimeException("getRecipients('" + serverName + "'): " + propKeyTo + "=|" + toStr + "| is EMPTY... This is a Mandatory property value.");

		Rules rules = parse(toStr);

		// Errors are logged once per value (the value is cached)
		if (rules.hasErrors() && rules._hasLoggedErrors.compareAndSet(false, true))
		{
			for (String err : rules.getErrors())
				_logger.warn("Mail recipients in '" + propKeyTo + "': " + err);
		}

		Sample sample = Sample.of(serverName, alarmEvent, action);
		if (rules.needsServerGroup())
		{
			DbxCentralServerGroup.Lookup group = DbxCentralServerGroup.get(alarmEvent);
			sample.serverGroup = group.isKnown() ? group.getName() : null;
		}

		Recipients res = getRecipients(rules, sample);

		if (rules.isJson())
		{
			if (res.isEmpty())
				_logger.info("Mail recipients in '" + propKeyTo + "': No rule matched serverName='" + serverName + "', alarmName='" + sample.alarmName + "', action='" + action + "', and there is no 'fallback' rule. Nobody gets this mail.");
			else if (res.isUsedFallback())
				_logger.info("Mail recipients in '" + propKeyTo + "': No rule matched serverName='" + serverName + "', alarmName='" + sample.alarmName + "', action='" + action + "', using the 'fallback' rule. to=" + res._to + (res._cc.isEmpty() ? "" : ", cc=" + res._cc));
			else if (_logger.isDebugEnabled())
				_logger.debug("Mail recipients in '" + propKeyTo + "': serverName='" + serverName + "', alarmName='" + sample.alarmName + "', action='" + action + "' matched rules " + res._matchedRules + ". to=" + res._to + ", cc=" + res._cc);
		}

		return res;
	}
	//----------------------------------------------------------------
	// END: Rules
	//----------------------------------------------------------------

	/**
	 * Helper method to get mail addresses (the 'to' list only)
	 *
	 * @param serverName
	 * @param alarmEvent   null for a report
	 * @param propKeyTo
	 * @param defaultTo
	 * @return
	 */
	public static List<String> getMailToAddressForServerNameAsList(String serverName, AlarmEvent alarmEvent, String propKeyTo, String defaultTo)
	{
		return getRecipients(serverName, alarmEvent, null, propKeyTo, defaultTo).getTo();
	}

	/**
	 * Helper method to get mail addresses (the 'to' list only), as a comma separated string
	 *
	 * @param serverName
	 * @param alarmEvent   null for a report
	 * @param propKeyTo
	 * @param defaultTo
	 * @return
	 */
	public static String getMailToAddressForServerName(String serverName, AlarmEvent alarmEvent, String propKeyTo, String defaultTo)
	{
		return StringUtil.toCommaStr( getMailToAddressForServerNameAsList(serverName, alarmEvent, propKeyTo, defaultTo) );
	}

	/**
	 * Get all mail addresses ('to' and 'cc') from a CSV or from all (valid and enabled) JSON rules, including the 'fallback' rule.
	 *
	 * @param csvOrJson
	 * @return a list of email addresses or an empty list (never null)
	 */
	public static List<String> getAllMailToAddress(String csvOrJson)
	{
		if (StringUtil.isNullOrBlank(csvOrJson))
			return Collections.emptyList();

		Rules rules = parse(csvOrJson);
		if (rules.isCsv())
			return new ArrayList<>(rules.getCsv());

		Set<String> set = new LinkedHashSet<>();
		for (Rule r : rules.getRules())
		{
			if (r.isValid() && r.isEnabled())
			{
				set.addAll(r._to);
				set.addAll(r._cc);
			}
		}
		return new ArrayList<>(set);
	}
}
