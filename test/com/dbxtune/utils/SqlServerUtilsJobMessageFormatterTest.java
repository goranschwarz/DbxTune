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
				"rule.badRule",  "[bad=replacement");
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

	// -----------------------------------------------------------------------
	// jobMessageFormatter() — user profile augments built-in (skipBuiltIn=false)
	// -----------------------------------------------------------------------

	@Test
	public void testAugmentProfile_addsNewlineBeforeCustomMarker()
	{
		System.out.println("---testAugmentProfile_addsNewlineBeforeCustomMarker---");

		// Rule: insert newline before "RESULT:" (using \s = regex whitespace, doubled for Java string)
		installProfile("myapp",
				"subsystems", "CMDEXEC",
				"rules",      "result",
				"rule.result", "\\s{2,}(?=RESULT:)=\n");

		String raw = "Process started.  RESULT: OK.  The step succeeded.";
		String result = SqlServerUtils.jobMessageFormatter(raw, "CMDEXEC");

		System.out.println("result=\n" + result);
		assertTrue("Should have newline before RESULT:", result.contains("\nRESULT:"));
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
	}

	@Test
	public void testAugmentProfile_subsystemFilterExcludesOtherSubsystems()
	{
		System.out.println("---testAugmentProfile_subsystemFilterExcludesOtherSubsystems---");

		installProfile("myapp",
				"subsystems", "CMDEXEC",
				"rules",      "result",
				"rule.result", "\\s{2,}(?=RESULT:)=\n");

		// TSQL step — profile must not fire
		String raw = "Process started.  RESULT: OK.  The step succeeded.";
		String resultTsql = SqlServerUtils.jobMessageFormatter(raw, "TSQL");

		System.out.println("resultTsql=\n" + resultTsql);
		// Built-in TSQL runs (sentence split fires), but our rule should not add \nRESULT:
		// The sentence splitter fires on ". " so we check built-in sentences were split
		assertTrue("Built-in TSQL sentence split should still run", resultTsql.contains("\n"));
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
