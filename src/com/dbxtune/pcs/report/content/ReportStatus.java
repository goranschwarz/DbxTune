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
package com.dbxtune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.DailySummaryReportDefault;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * "Report Status": the Issue Level of the report (NTR / FYI / ISSUE), and which sections it comes from (with links to them)
 */
public class ReportStatus
extends ReportEntryAbstract
{
	public ReportStatus(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override public boolean hasMinimalMessageText() { return true; }
	@Override public boolean hasShortMessageText()   { return true; }
	@Override public boolean canBeDisabled()         { return false; }
	@Override public String  getSubject()            { return "Report Status"; }

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Nothing to do: the other entries have the information (when this is written, they are all created)
	}

	/** Small colored label for a level (inline style, so it also works in mail clients) */
	public static String levelBadge(IssueLevel level)
	{
		String color = "#198754"; // green
		String text  = "NTR";
		if (level == IssueLevel.FYI)   { color = "#0d6efd"; text = "FYI";   } // blue
		if (level == IssueLevel.ISSUE) { color = "#dc3545"; text = "ISSUE"; } // red

		return "<span style='background-color: " + color + "; color: white; padding: 1px 6px; border-radius: 4px; font-size: 85%; font-weight: bold;'>" + text + "</span>";
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		// Entries with something to report: ISSUE first, then FYI (in report order)
		List<IReportEntry> issueEntries = new ArrayList<>();
		List<IReportEntry> fyiEntries   = new ArrayList<>();
		for (IReportEntry entry : getReportingInstance().getReportEntries())
		{
			IssueLevel level = DailySummaryReportDefault.getIssueLevel(entry);
			if (level == IssueLevel.ISSUE) issueEntries.add(entry);
			if (level == IssueLevel.FYI)   fyiEntries  .add(entry);
		}
		List<IReportEntry> entries = new ArrayList<>(issueEntries);
		entries.addAll(fyiEntries);

		IssueLevel reportLevel = !issueEntries.isEmpty() ? IssueLevel.ISSUE : !fyiEntries.isEmpty() ? IssueLevel.FYI : IssueLevel.NONE;

		String text = "Nothing To Report";
		if (reportLevel == IssueLevel.FYI)   text = "For Your Information: nothing urgent, have a look when you have time";
		if (reportLevel == IssueLevel.ISSUE) text = "Something to act on";

		// The mail (SHORT message) shows the level in its banner at the top
		if (MessageType.SHORT_MESSAGE.equals(messageType))
		{
			if (entries.isEmpty())
				w.append("<div style='font-size:13px; color:#5d6878;'>No section has anything to report.</div>\n");
		}
		else
			w.append("<p style='font-size: 120%;'>").append(levelBadge(reportLevel)).append("&nbsp; <b>").append(text).append("</b></p>\n");

		if (entries.isEmpty())
			return;

		w.append("<table class='sortable'>\n");
		w.append("<thead><tr> <th>Level</th> <th>Section</th> <th>Reason</th> </tr></thead>\n");
		w.append("<tbody>\n");
		for (IReportEntry entry : entries)
		{
			String subject = entry.getSubject();
			String tocDiv  = StringUtil.stripAllNonAlphaNum(subject);

			// Strip off parts that may be details (as in the TOC)
			int firstLeftParentheses = subject.indexOf("(");
			if (firstLeftParentheses != -1)
				subject = subject.substring(0, firstLeftParentheses - 1).trim();

			// The mail (short/minimal message) only holds some of the sections
			boolean inThisMessage = MessageType.FULL_MESSAGE.equals(messageType)
					|| (MessageType.SHORT_MESSAGE  .equals(messageType) && entry.hasShortMessageText())
					|| (MessageType.MINIMAL_MESSAGE.equals(messageType) && entry.hasMinimalMessageText());
			String section = inThisMessage
					? "<a href='#" + tocDiv + "'>" + subject + "</a>"
					: subject + " <i>(only in the full report)</i>";

			String reason = "";
			try { reason = entry.getIssueReason(); }
			catch (RuntimeException ignore) { /* the level is shown anyway */ }

			w.append("<tr>");
			w.append(" <td>").append(levelBadge(DailySummaryReportDefault.getIssueLevel(entry))).append("</td>");
			w.append(" <td>").append(section).append("</td>");
			w.append(" <td>").append(reason == null ? "" : reason).append("</td>");
			w.append(" </tr>\n");
		}
		w.append("</tbody>\n");
		w.append("</table>\n");
	}
}
