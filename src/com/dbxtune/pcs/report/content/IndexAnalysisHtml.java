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
package com.dbxtune.pcs.report.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

/**
 * The "Index Analysis" section of Table Information, the same for every vendor.
 * <p>
 * The vendor code (SqlServerAbstract, AseAbstract, ...) decides everything: which indexes, why, the scripts and the texts.
 * This only lays it out, so the look (separators, &lt;code&gt;, Prism, ...) is the same everywhere and is changed in one place.
 * <p>
 * Everything handed in is plain text and is escaped here, except {@link #notesHtml}.
 * No JavaScript: this also goes into the Daily Summary Report mail.
 */
public class IndexAnalysisHtml
{
	/** Unused Indexes: the usage counters must cover at least this many days, else they say too little. The same for all vendors. */
	public static final String PROPKEY_unused_minDays = "DailySummaryReport.IndexAnalysis.unused.minDays";
	public static final int    DEFAULT_unused_minDays = 7;

	/** One finding: what to do with which indexes, why, and the script */
	public static class Finding
	{
		/** "DROP" or "MERGE" */
		public String       action;
		/** The indexes that go away */
		public List<String> dropped = new ArrayList<>();
		/** The index that stays, null for an unused index */
		public String       kept;
		/** One line per index: "Keep", "Drop" or "Index" */
		public List<IndexLine> lines = new ArrayList<>();
		/** Plain text. The index names of this finding and the column lists in parentheses are shown as code. */
		public String       why;
		/** DDL CHANGES and ROLLBACK, shown as SQL */
		public String       script;
	}

	/** "Keep: <code>definition</code>, rest" */
	public static class IndexLine
	{
		public String label;
		public String definition;
		public String rest;

		public IndexLine(String label, String definition, String rest)
		{
			this.label      = label;
			this.definition = definition;
			this.rest       = rest;
		}
	}

	/** When the index definitions were captured */
	public String              capturedTime;
	/** The general warnings, as HTML (the vendor escapes any value it puts in). Shown when there is at least one finding. */
	public List<String>        notesHtml       = new ArrayList<>();

	public List<Finding>       dedup           = new ArrayList<>();
	/** index name -> reason */
	public Map<String, String> dedupNotChecked = new LinkedHashMap<>();

	public List<Finding>       unused          = new ArrayList<>();
	/** Why the whole Unused Indexes check could not be done, null if it was done */
	public String              unusedNotChecked;
	/** Plain text above the unused findings: the rule */
	public String              unusedIntro;
	/** Plain text, warnings for the unused findings */
	public List<String>        unusedNotes     = new ArrayList<>();

	/** @return the HTML, or "" when there is nothing to tell */
	public String toHtml()
	{
		if (dedup.isEmpty() && dedupNotChecked.isEmpty() && unused.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
		sb.append("<br> \n");
		sb.append("<details class='dbx-redundant-index' style='max-width: 1200px;'> \n"); // The index table above is very wide: keep this readable

		// Collapsed by default, the counts in bold so they stand out
		String candidates = dedup.size()  + " Redundant Index candidates";
		String unusedTxt  = unused.size() + " Unused Indexes";
		sb.append("<summary>Show/Hide: Index Analysis - " + (dedup.isEmpty() ? candidates : "<b>" + candidates + "</b>")
				+ ", " + (unused.isEmpty() ? unusedTxt : "<b>" + unusedTxt + "</b>")
				+ (dedupNotChecked.isEmpty() ? "" : ", " + dedupNotChecked.size() + " not checked") + "</summary> \n");

		if ( ! dedup.isEmpty() || ! unused.isEmpty() )
		{
			sb.append("<p>From the index definitions captured " + esc(capturedTime) + ". These are candidates: nothing has been changed.</p> \n");
			sb.append("<ul> \n");
			for (String note : notesHtml)
				sb.append("<li>" + note + "</li> \n");
			sb.append("</ul> \n");
		}

		// ---- Subsection: Index De-duplication (open: the outer section is already collapsed)
		sb.append("<hr style='border-top: 1px solid #888;'> \n");
		sb.append("<details open class='dbx-index-analysis-dedup'> \n");
		sb.append("<summary><b>Index De-duplication</b> - " + dedup.size() + " candidates" + (dedupNotChecked.isEmpty() ? "" : ", " + dedupNotChecked.size() + " not checked") + "</summary> \n");

		int nr = 0;
		for (Finding f : dedup)
			appendFinding(sb, ++nr, f, "dbx-redundant-index-script");

		if ( ! dedupNotChecked.isEmpty() )
		{
			if ( ! dedup.isEmpty() ) // ends the last finding
				sb.append("<hr style='border-top: 1px dashed #bbb;'> \n");
			sb.append("<p>Not checked:</p> \n<ul> \n");
			for (Map.Entry<String, String> e : dedupNotChecked.entrySet())
				sb.append("<li>" + esc(e.getKey()) + ": " + esc(e.getValue()) + "</li> \n");
			sb.append("</ul> \n");
		}
		sb.append("</details> \n"); // END: Index De-duplication

		// ---- Subsection: Unused Indexes
		sb.append("<hr style='border-top: 1px solid #888;'> \n");
		sb.append("<details open class='dbx-index-analysis-unused'> \n");
		sb.append("<summary><b>Unused Indexes</b> - " + (unusedNotChecked != null ? "not checked" : unused.size() + " candidates") + "</summary> \n");
		if (unusedNotChecked != null)
		{
			sb.append("<p>Not checked: " + esc(unusedNotChecked) + "</p> \n");
		}
		else if ( ! unused.isEmpty() )
		{
			sb.append("<p>" + esc(unusedIntro) + "</p> \n");
			sb.append("<ul> \n");
			for (String note : unusedNotes)
				sb.append("<li>" + esc(note) + "</li> \n");
			sb.append("</ul> \n");

			nr = 0;
			for (Finding f : unused)
				appendFinding(sb, ++nr, f, "dbx-unused-index-script");
		}
		sb.append("</details> \n"); // END: Unused Indexes

		sb.append("</details> \n"); // END: Index Analysis
		sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		return sb.toString();
	}

	private static void appendFinding(StringBuilder sb, int nr, Finding f, String scriptClass)
	{
		// A line in front of every finding, so it's clear where one starts and ends
		sb.append("<hr style='border-top: 1px dashed #bbb;'> \n");

		// Index names and column lists in <code>, so they stand out from the text
		List<String> droppedHtml = new ArrayList<>();
		for (String n : f.dropped)
			droppedHtml.add("<code>" + esc(n) + "</code>");
		sb.append("<p><b>" + nr + ". " + f.action + " " + String.join(", ", droppedHtml)
				+ (f.kept == null ? "" : ("MERGE".equals(f.action) ? " into " : ", keep ") + "<code>" + esc(f.kept) + "</code>") + "</b></p> \n");

		sb.append("<ul> \n");
		for (IndexLine l : f.lines)
			sb.append("<li>" + esc(l.label) + ": <code>" + esc(l.definition) + "</code>" + esc(l.rest) + "</li> \n");
		sb.append("</ul> \n");

		if (f.why != null && ! f.why.isEmpty())
		{
			// The column lists, like ([c1], [c2] DESC), in <code>. Index names: the ones of this finding.
			String whyHtml = esc(f.why).replaceAll("\\(([^()]+)\\)", "<code>($1)</code>");
			List<String> names = new ArrayList<>(f.dropped);
			if (f.kept != null)
				names.add(f.kept);
			names.sort((a, b) -> b.length() - a.length()); // longest first, so 'ix_a' doesn't hit inside 'ix_a2'
			for (String n : names)
				whyHtml = whyHtml.replaceAll("(?<![\\w>])" + java.util.regex.Pattern.quote(esc(n)) + "(?![\\w<])", java.util.regex.Matcher.quoteReplacement("<code>" + esc(n) + "</code>"));
			sb.append("<p>Why: ").append(whyHtml).append("</p> \n");
		}

		sb.append("<details class='" + scriptClass + "'><summary>Show/Hide: DDL Changes and Rollback</summary> \n");
		sb.append("<pre style='white-space: pre-wrap;'><code class='language-sql'>").append(esc(f.script)).append("</code></pre> \n"); // Prism highlights it where it's loaded, plain in mail
		sb.append("</details> \n");
	}

	public static String esc(Object o)
	{
		return o == null ? "" : StringEscapeUtils.escapeHtml4(o.toString());
	}
}
