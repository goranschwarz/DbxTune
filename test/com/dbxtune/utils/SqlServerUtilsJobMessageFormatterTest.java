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
package com.dbxtune.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the user-defined profile system in SqlServerUtils.jobMessageFormatter().
 *
 * Key properties-file escaping facts (tested here via setProperty, which bypasses
 * the file layer and delivers values exactly as written):
 *   - \n in a real .properties file  -> real newline  (loader converts it)
 *   - \\s in a real .properties file -> \s            (loader converts \\ to \)
 *
 * In setProperty() there is NO file-loader escaping, so we pass Java strings
 * directly: "\n" = real newline, "\\s" = two-char backslash-s.
 */
public class SqlServerUtilsJobMessageFormatterTest
{
	@Before
	public void setUp()
	{
		Configurator.setRootLevel(Level.WARN);
		SqlServerUtils.resetJobMessageProfilesCache();
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/**
	 * Builds a Configuration with the given profile defined, loads it through
	 * SqlServerUtils.loadJobMessageProfiles(conf), installs it as the active
	 * profile list, and returns the list for optional inspection.
	 *
	 * profileProps entries are alternating key / value strings:
	 *   buildConf("myprofile",
	 *       "trigger",     "MyApp",
	 *       "subsystems",  "CMDEXEC",
	 *       "rules",       "r1",
	 *       "rule.r1",     "STEP\\s+=\n$0")
	 */
	private List<SqlServerUtils.JobMessageProfile> installProfile(String profileName, String... profileProps)
	{
		Configuration conf = new Configuration();
		conf.setProperty(SqlServerUtils.PROPKEY_JMF_PROFILES, profileName);

		String prefix = SqlServerUtils.PROPKEY_JMF_PROFILE + profileName + ".";
		for (int i = 0; i < profileProps.length - 1; i += 2)
			conf.setProperty(prefix + profileProps[i], profileProps[i + 1]);

		List<SqlServerUtils.JobMessageProfile> profiles = SqlServerUtils.loadJobMessageProfiles(conf);

		// Install as active cache so jobMessageFormatter() picks them up
		SqlServerUtils._jobMessageProfiles = profiles;
		return profiles;
	}

	// -----------------------------------------------------------------------
	// Profile loading tests
	// -----------------------------------------------------------------------

	@Test
	public void testNoProfilesPropertyReturnsEmptyList()
	{
		System.out.println("---testNoProfilesPropertyReturnsEmptyList---");
		Configuration conf = new Configuration();
		List<SqlServerUtils.JobMessageProfile> profiles = SqlServerUtils.loadJobMessageProfiles(conf);
		assertTrue("Expected empty profile list when property is absent", profiles.isEmpty());
	}

	@Test
	public void testProfileWithBadTriggerRegexIsSkipped()
	{
		System.out.println("---testProfileWithBadTriggerRegexIsSkipped---");
		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("bad",
				"trigger", "[unclosed");
		assertTrue("Profile with invalid trigger regex should be skipped", profiles.isEmpty());
	}

	@Test
	public void testProfileWithBadRuleRegexSkipsThatRule()
	{
		System.out.println("---testProfileWithBadRuleRegexSkipsThatRule---");
		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("p",
				"rules",    "goodRule,badRule",
				"rule.goodRule", "STEP=\nSTEP",
				"rule.badRule",  "*bad=replacement");  // dangling meta char -> invalid regex
		assertEquals("Profile itself should load (bad rule is just skipped)", 1, profiles.size());
		assertEquals("Only the valid rule should be compiled", 1, profiles.get(0).rulePatterns.size());
	}

	@Test
	public void testProfileLoadedWithCorrectRuleCount()
	{
		System.out.println("---testProfileLoadedWithCorrectRuleCount---");
		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("myapp",
				"trigger",   "MyApp",
				"rules",     "r1,r2",
				"rule.r1",   "STEP=\nSTEP",
				"rule.r2",   "RESULT:=\nRESULT:");
		assertEquals(1, profiles.size());
		assertEquals(2, profiles.get(0).rulePatterns.size());
	}

	// -----------------------------------------------------------------------
	// jobMessageFormatter() — built-in behaviour unchanged (no profiles active)
	// -----------------------------------------------------------------------

	@Test
	public void testBuiltInTsqlFormattingUnchanged()
	{
		System.out.println("---testBuiltInTsqlFormattingUnchanged---");
		// No profiles installed — cache reset in setUp(), _jobMessageProfiles stays null -> loads empty list from null conf
		// Force empty list explicitly
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "Msg 208, Level 16, State 1, Line 1  Invalid object name 'foo'.  The step failed.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "TSQL");
		assertTrue("Should contain newline", result.contains("\n"));
	}

	@Test
	public void testExplicitLineBreaksBecomeNewlines()
	{
		System.out.println("---testExplicitLineBreaksBecomeNewlines---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "line1<br>line2<BR>line3<br/>line4<Br />line5\\nline6&lt;br&gt;line7";
		String result = SqlServerUtils.jobMessageFormatter(raw, null);
		assertEquals("line1\nline2\nline3\nline4\nline5\nline6\nline7", result);
	}

	@Test
	public void testBackslashNInAccountsAndPathsIsKept()
	{
		System.out.println("---testBackslashNInAccountsAndPathsIsKept---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		// Seen on gorans.org 2026-09-28: "\N" in "NT AUTHORITY\NETWORK SERVICE" became a newline
		assertEquals("Executed as user: NT AUTHORITY\\NETWORK SERVICE.\nWAITFOR DELAY '00:00:50'",
				SqlServerUtils.jobMessageFormatter("Executed as user: NT AUTHORITY\\NETWORK SERVICE. WAITFOR DELAY '00:00:50'", null));

		// lowercase "\n" in the user name of the prefix
		assertEquals("Executed as user: MAXM\\nils.\nhello",
				SqlServerUtils.jobMessageFormatter("Executed as user: MAXM\\nils. hello", null));

		// "\n" that starts a path segment
		String path = "Running C:\\Program Files\\nodejs\\node.exe now";
		assertEquals(path, SqlServerUtils.jobMessageFormatter(path, "CMDEXEC"));

		// UNC path with "\n" segments
		String unc = "python \\\\srv-1\\nas\\new_dir\\JobHandler.py --job_name x";
		assertEquals(unc, SqlServerUtils.jobMessageFormatter(unc, "CMDEXEC"));

		// ... but a literal "\n" in the text is still a newline, also right after a path
		assertEquals("Executed as user: MAXM\\nils.\nline1\nline2",
				SqlServerUtils.jobMessageFormatter("Executed as user: MAXM\\nils. line1\\nline2", null));
		assertEquals("ran C:\\Program Files\\nodejs\\node.exe exit: 0\nnext",
				SqlServerUtils.jobMessageFormatter("ran C:\\Program Files\\nodejs\\node.exe exit: 0\\nnext", "CMDEXEC"));
		assertEquals("C:\\x\\y.exe \nnext",
				SqlServerUtils.jobMessageFormatter("C:\\x\\y.exe \\nnext", "CMDEXEC")); // space, then "\n": not a path segment
	}

	// -----------------------------------------------------------------------
	// jobMessageFormatter() - TSQL: [SQLSTATE 01xxx] (Message n) stripping
	// -----------------------------------------------------------------------

	@Test
	public void testTsqlSqlState01MarkersAreRemoved()
	{
		System.out.println("---testTsqlSqlState01MarkersAreRemoved---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "Executed as user: NT SERVICE\\SQLSERVERAGENT. "
				+ "Starting load [SQLSTATE 01000] (Message 50000)  "
				+ "Rows loaded: 42 [SQLSTATE 01000] (Message 0).  "
				+ "Invalid object name 'foo'. [SQLSTATE 42S02] (Error 208).  The step failed.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "TSQL");

		System.out.println("result=\n" + result);
		assertEquals(""
				+ "Executed as user: NT SERVICE\\SQLSERVERAGENT.\n"
				+ "Starting load\n"
				+ "Rows loaded: 42\n"
				+ "Invalid object name 'foo'.\n"
				+ "  [SQLSTATE 42S02] (Error 208).\n"
				+ "The step failed.", result);
	}

	@Test
	public void testTsqlOnlySqlState01Markers_noEmptyOrDotLines()
	{
		System.out.println("---testTsqlOnlySqlState01Markers_noEmptyOrDotLines---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "Executed as user: DOM\\usr. msg one [SQLSTATE 01000] (Message 50000) [SQLSTATE 01000] (Message 50000).  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "TSQL");

		System.out.println("result=\n" + result);
		assertEquals("Executed as user: DOM\\usr.\nmsg one\nThe step succeeded.", result);
	}

	@Test
	public void testSqlState01MarkersKeptForNonTsql()
	{
		System.out.println("---testSqlState01MarkersKeptForNonTsql---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "msg one [SQLSTATE 01000] (Message 50000)";
		assertEquals(raw, SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC"));
	}

	// -----------------------------------------------------------------------
	// jobMessageFormatterHtml()
	// -----------------------------------------------------------------------

	@Test
	public void testHtml_nullAndBlank()
	{
		System.out.println("---testHtml_nullAndBlank---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		assertEquals(null, SqlServerUtils.jobMessageFormatterHtml(null, "TSQL"));
		assertEquals("",   SqlServerUtils.jobMessageFormatterHtml("",   "TSQL"));
	}

	@Test
	public void testHtml_escapesAndNewlinesAndIndent()
	{
		System.out.println("---testHtml_escapesAndNewlinesAndIndent---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "Executed as user: DOM\\usr. select * from t where a < 1 & b > \"x\". [SQLSTATE 42000] (Error 102).  The step failed.";
		String result = SqlServerUtils.jobMessageFormatterHtml(raw, "TSQL");

		System.out.println("result=\n" + result);
		assertEquals(""
				+ "Executed as user: DOM\\usr.<br>"
				+ "select * from t where a &lt; 1 &amp; b &gt; &quot;x&quot;.<br>"
				+ "&nbsp;&nbsp;[SQLSTATE 42000] (Error 102).<br>"
				+ "The step failed.", result);
	}

	@Test
	public void testHtml_explicitLineBreaksBecomeBr()
	{
		System.out.println("---testHtml_explicitLineBreaksBecomeBr---");
		SqlServerUtils._jobMessageProfiles = java.util.Collections.emptyList();

		String raw = "line1<br>line2<BR/>line3\\nline4&lt;br&gt;line5\r\nline6";
		String result = SqlServerUtils.jobMessageFormatterHtml(raw, null);
		assertEquals("line1<br>line2<br>line3<br>line4<br>line5<br>line6", result);
	}

	// -----------------------------------------------------------------------
	// jobMessageFormatter() — user profile augments built-in (skipBuiltIn=false)
	// -----------------------------------------------------------------------

	@Test
	public void testAugmentProfile_addsNewlineBeforeCustomMarker()
	{
		System.out.println("---testAugmentProfile_addsNewlineBeforeCustomMarker---");

		// Rule: insert newline before "RESULT:" (using \s = regex whitespace, doubled for Java string)
		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("myapp",
				"subsystems", "CMDEXEC",
				"rules",      "result",
				"rule.result", "\\s{2,}(?=RESULT:)=\n");
		assertEquals("The lookahead rule must compile (not be skipped)", 1, profiles.get(0).rulePatterns.size());
		assertEquals("\\s{2,}(?=RESULT:)", profiles.get(0).rulePatterns.get(0).pattern());

		// No period before "RESULT:" -> the built-in sentence split can NOT produce this newline, only our rule can
		String raw = "Process started  RESULT: OK.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		assertEquals("Process started\nRESULT: OK.\nThe step succeeded.", result);
	}

	@Test
	public void testAugmentProfile_triggerFiltersNonMatchingMessages()
	{
		System.out.println("---testAugmentProfile_triggerFiltersNonMatchingMessages---");

		installProfile("myapp",
				"trigger",    "MyApp v\\d",   // \d in Java string = regex \d (correct — no file escaping here)
				"subsystems", "CMDEXEC",
				"rules",      "step",
				"rule.step",  "STEP=\nSTEP");

		// Message does NOT contain "MyApp v<digit>" — profile must not fire
		String raw = "OtherApp started.  STEP 1 done.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		// Built-in still runs (SENTENCE_SEP fires on ". "), but our "STEP" split must NOT add an extra leading newline
		assertTrue("Profile trigger did not match — STEP should not be on its own line from our rule",
				!result.startsWith("\nSTEP"));
	}

	@Test
	public void testAugmentProfile_triggerMatchesAndRuleFires()
	{
		System.out.println("---testAugmentProfile_triggerMatchesAndRuleFires---");

		installProfile("myapp",
				"trigger",    "MyApp v\\d",
				"subsystems", "CMDEXEC",
				"rules",      "step",
				"rule.step",  "  STEP=\nSTEP");

		String raw = "MyApp v2 started.  STEP 1 done.  STEP 2 done.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		assertTrue("Profile matched — STEP lines should be split", result.contains("\nSTEP"));
		// The rule value must not be trimmed: "  STEP" (with spaces) must not become "STEP" (which gave empty lines)
		assertTrue("No empty lines expected", !result.contains("\n\n"));
	}

	@Test
	public void testAugmentProfile_subsystemFilterExcludesOtherSubsystems()
	{
		System.out.println("---testAugmentProfile_subsystemFilterExcludesOtherSubsystems---");

		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("myapp",
				"subsystems", "CMDEXEC",
				"rules",      "result",
				"rule.result", "\\s{2,}(?=RESULT:)=\n");
		assertEquals("The lookahead rule must compile (not be skipped)", 1, profiles.get(0).rulePatterns.size());

		// No period before "RESULT:" -> only our rule could split there
		String raw = "Process started  RESULT: OK.  The step succeeded.";

		// CMDEXEC step - profile fires
		String resultCmd = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");
		System.out.println("resultCmd=\n" + resultCmd);
		assertTrue("CMDEXEC: rule should have split RESULT:", resultCmd.contains("\nRESULT:"));

		// TSQL step - profile must not fire, but the built-in sentence split still runs
		String resultTsql = SqlServerUtils.jobMessageFormatter(raw, "TSQL");
		System.out.println("resultTsql=\n" + resultTsql);
		assertEquals("Process started  RESULT: OK.\nThe step succeeded.", resultTsql);
	}

	// -----------------------------------------------------------------------
	// Rule separator: regex=replacement split
	// -----------------------------------------------------------------------

	@Test
	public void testFindRuleSeparator()
	{
		System.out.println("---testFindRuleSeparator---");
		// Old form (no '=' in the regex) -> first '=' as before
		assertEquals(4,  SqlServerUtils.findRuleSeparator("STEP=\nSTEP"));
		assertEquals(4,  SqlServerUtils.findRuleSeparator("STEP=a=b"));             // replacement may contain '='

		// '=' inside groups (lookahead / negative lookahead / lookbehind) is part of the regex
		assertEquals(17, SqlServerUtils.findRuleSeparator("\\s{2,}(?=RESULT:)=\n"));
		assertEquals(6,  SqlServerUtils.findRuleSeparator("(?!=)x=y"));
		assertEquals(7,  SqlServerUtils.findRuleSeparator("(?<=:)x=y"));
		assertEquals(11, SqlServerUtils.findRuleSeparator("(?<=(a|b))x=y"));       // nested groups

		// '=' inside a character class, and escaped '=' (also '(' / ')' in a class or escaped do not count)
		assertEquals(3,  SqlServerUtils.findRuleSeparator("[=]=y"));
		assertEquals(5,  SqlServerUtils.findRuleSeparator("\\={3}=y"));
		assertEquals(3,  SqlServerUtils.findRuleSeparator("[(]=y"));
		assertEquals(2,  SqlServerUtils.findRuleSeparator("\\(=y"));

		// No separator
		assertEquals(-1, SqlServerUtils.findRuleSeparator("abc"));
		assertEquals(-1, SqlServerUtils.findRuleSeparator("(?=abc)"));
	}

	@Test
	public void testLookbehindAndEscapedEqualsRules()
	{
		System.out.println("---testLookbehindAndEscapedEqualsRules---");

		// lookbehind: newline + indent after "Rows:" ; escaped '=': each "===" divider on its own line
		List<SqlServerUtils.JobMessageProfile> profiles = installProfile("p",
				"subsystems",   "CMDEXEC",
				"rules",        "rows,section",
				"rule.rows",    "(?<=Rows:)\\s+=\n  ",
				"rule.section", "\\s*\\={3,}\\s*=\n===\n");
		assertEquals("Both rules must compile", 2, profiles.get(0).rulePatterns.size());

		String raw = "Load done Rows: 42 === Next part";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		assertEquals("Load done Rows:\n  42\n===\nNext part", result);
	}

	// -----------------------------------------------------------------------
	// jobMessageFormatter() — profile replaces built-in (skipBuiltIn=true)
	// -----------------------------------------------------------------------

	@Test
	public void testSkipBuiltIn_replacesBuiltInHandling()
	{
		System.out.println("---testSkipBuiltIn_replacesBuiltInHandling---");

		// Profile handles TSQL entirely on its own — inserts newline before "Step"
		installProfile("custom",
				"subsystems",  "TSQL",
				"skipBuiltIn", "true",
				"rules",       "stepMarker",
				"rule.stepMarker", "  Step=\nStep");

		// A message that built-in TSQL would split on SENTENCE_SEP, but our rule splits differently
		String raw = "Step 1 started.  Step 2 started.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "TSQL");

		System.out.println("result=\n" + result);
		// Our rule fires: "  Step" -> "\nStep"
		assertTrue("skipBuiltIn profile should split on Step marker", result.contains("\nStep"));
		// Built-in SENTENCE_SEP (". ") must NOT have fired (it produces ".\n", not just "\n")
		// The step succeeded ends in period but built-in was skipped so no ".\n" at that spot
	}

	@Test
	public void testSkipBuiltIn_nonSkipProfileStillRunsAfterSkipProfile()
	{
		System.out.println("---testSkipBuiltIn_nonSkipProfileStillRunsAfterSkipProfile---");

		// Two profiles: one skipBuiltIn, one augmenting
		Configuration conf = new Configuration();
		conf.setProperty(SqlServerUtils.PROPKEY_JMF_PROFILES, "skipper,augmenter");

		String prefix1 = SqlServerUtils.PROPKEY_JMF_PROFILE + "skipper.";
		conf.setProperty(prefix1 + "subsystems",  "CMDEXEC");
		conf.setProperty(prefix1 + "skipBuiltIn", "true");
		conf.setProperty(prefix1 + "rules",       "s1");
		conf.setProperty(prefix1 + "rule.s1",     "  STEP=\nSTEP");

		String prefix2 = SqlServerUtils.PROPKEY_JMF_PROFILE + "augmenter.";
		conf.setProperty(prefix2 + "subsystems",  "CMDEXEC");
		conf.setProperty(prefix2 + "skipBuiltIn", "false");
		conf.setProperty(prefix2 + "rules",       "s2");
		conf.setProperty(prefix2 + "rule.s2",     "  RESULT=\nRESULT");

		SqlServerUtils._jobMessageProfiles = SqlServerUtils.loadJobMessageProfiles(conf);

		String raw = "Started.  STEP 1.  RESULT OK.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		assertTrue("skipBuiltIn profile should have split STEP", result.contains("\nSTEP"));
		assertTrue("augmenter profile should have split RESULT", result.contains("\nRESULT"));
	}
}
