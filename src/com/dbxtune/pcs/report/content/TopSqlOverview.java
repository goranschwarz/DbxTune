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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * The "Top SQL Statements - OVERVIEW" section (all vendors): each statement's <b>share</b> of what all the statements did in the period,
 * for each of the measures the vendor has a "Top SQL" section for (time, reads, executions...).
 * <ul>
 *   <li>No weights: a share is comparable whatever the unit, so a statement that does 60% of all disk reads is in the overview even if its time is small.</li>
 *   <li>The top ## on the largest share (in bold in the table: why the statement is there).</li>
 *   <li>Column <code>in_sections</code>: the sections the statement is in (as the sections pick them: the top ## on the measure, and only with some of it),
 *       with a link that opens the section and marks the statement's row there ({@link ReportEntryAbstract#sectionRowLink}).</li>
 * </ul>
 * The vendor's section class makes one, in its create() for the OVERVIEW report type, and writes it in writeMessageText().
 */
public class TopSqlOverview
{
	/** One measure: a "pct_" column in the overview, and (if the vendor has one) the section that is ordered by it */
	public static class Measure
	{
		final String  name;              // the column is "pct_" + name
		final String  sumExpr;           // the SQL aggregate (on the vendor's table), null = not recorded (the column is not shown)
		final String  sectionSubject;    // the subject of the section ordered by it (its anchor), null = no section
		final String  sectionLabel;      // the name of the section in the 'in_sections' column
		final boolean onlyWithSome;      // the section only has statements with some of it (> 0)
		final String  description;       // the column description
		double        minValue = 0;      // the section only has statements with at least this much (its 'having'), 0 = no limit

		public Measure(String name, String sumExpr, String sectionSubject, String sectionLabel, boolean onlyWithSome, String description)
		{
			this.name           = name;
			this.sumExpr        = sumExpr;
			this.sectionSubject = sectionSubject;
			this.sectionLabel   = sectionLabel;
			this.onlyWithSome   = onlyWithSome;
			this.description    = description;
		}

		/** The section only has statements with at least this much of the measure (for example ASE 'SQL Captured': having sum >= 1000 ms) */
		public Measure setMinValue(double minValue)
		{
			this.minValue = minValue;
			return this;
		}
	}

	private ResultSetTableModel _rstm;
	private Set<String>         _hiddenCols = new HashSet<>();
	private String              _idCol;
	private String              _textCol;

	public ResultSetTableModel getRstm() { return _rstm; }

	/**
	 * Read the overview
	 *
	 * @param entry      the section (to run the SQL)
	 * @param conn       the recording
	 * @param fromTable  the vendor's table, for example <code>[CmPgStatements_diff]</code>
	 * @param whereSql   the where clause (with "where"): the same rows the sections use
	 * @param keyCols    the columns a statement is grouped by, for example <code>[datname], [usename], [queryid]</code>
	 * @param idCol      the column that is the statement's id in the sections (marked when a link is clicked), for example <code>queryid</code>
	 * @param extraCols  more columns of the statement (aggregates, with an alias), for example <code>min([query]) as [query]</code>, may be ""
	 * @param textCol    the column with the SQL Text (a popup in the table), or null
	 * @param measures   the measures, in the order of the columns
	 * @param topRows    how many statements
	 */
	public static TopSqlOverview create(ReportEntryAbstract entry, DbxConnection conn, String fromTable, String whereSql, String keyCols, String idCol, String extraCols, String textCol, List<Measure> measures, int topRows)
	{
		TopSqlOverview ov = new TopSqlOverview();
		ov._idCol   = idCol;
		ov._textCol = textCol;

		StringBuilder sums = new StringBuilder();
		StringBuilder pcts = new StringBuilder();
		StringBuilder rnks = new StringBuilder();
		List<String>  pctCols = new ArrayList<>();
		for (Measure m : measures)
		{
			sums.append("            ,").append(m.sumExpr == null ? "cast(0 as bigint)" : m.sumExpr).append(" as [m_").append(m.name).append("] \n");
			pcts.append("        ,cast(coalesce(100.0 * [m_").append(m.name).append("] / nullif(sum([m_").append(m.name).append("]) over (), 0), 0) as DECIMAL(5,1)) as [pct_").append(m.name).append("] \n");
			// tie-break on the key: the sections do the same ('order by <measure> desc, <key>'), so a tie picks the same statements in both
			rnks.append("        ,row_number() over (order by [m_").append(m.name).append("] desc, ").append(keyCols).append(") as [r_").append(m.name).append("] \n");
			pctCols.add("[pct_" + m.name + "]");

			ov._hiddenCols.add("m_" + m.name);
			ov._hiddenCols.add("r_" + m.name);
			if (m.sumExpr == null)
				ov._hiddenCols.add("pct_" + m.name);
		}

		String sql = ""
			    + "select top " + topRows + " x.* \n"
			    + "    ,greatest(" + String.join(", ", pctCols) + ") as [largest_pct] \n"
			    + "from ( \n"
			    + "    select s.* \n"
			    + "        ,cast('' as varchar(2048)) as [in_sections] \n"
			    + pcts
			    + rnks
			    + "    from ( \n"
			    + "        select " + keyCols + " \n"
			    + (StringUtil.hasValue(extraCols) ? "            ," + extraCols + " \n" : "")
			    + sums
			    + "        from " + fromTable + " \n"
			    + "        " + whereSql.trim() + " \n"
			    + "        group by " + keyCols + " \n"
			    + "    ) s \n"
			    + ") x \n"
			    + "order by [largest_pct] desc \n"
			    + "";

		ov._rstm = entry.executeQuery(conn, sql, false, "Top SQL Overview", false); // false = do not truncate (the SQL Text)
		if (ov._rstm == null)
		{
			ov._rstm = ResultSetTableModel.createEmpty("Top SQL Overview");
			return ov;
		}

		for (int r = 0; r < ov._rstm.getRowCount(); r++)
		{
			// The sections the statement is in: the top ## on the measure (and some of it, if the section only shows those), with a link that marks its row there
			StringBuilder in = new StringBuilder();
			for (Measure m : measures)
			{
				if (m.sectionSubject == null || m.sumExpr == null)
					continue;
				int     rank   = ov._rstm.getValueAsInteger(r, "r_" + m.name, true, Integer.MAX_VALUE);
				double  value  = ov._rstm.getValueAsDouble(r, "m_" + m.name, true, 0d);
				boolean hasIt  = (! m.onlyWithSome || value > 0) && (m.minValue <= 0 || value >= m.minValue);
				if (rank <= topRows && hasIt)
					in.append(in.length() == 0 ? "" : ", ").append(ReportEntryAbstract.sectionRowLink(m.sectionSubject, m.sectionLabel, ov._rstm.getValueAsString(r, idCol))).append("&nbsp;#").append(rank);
			}
			ov._rstm.setValueAtWithOverride(in.toString(), r, ov._rstm.findColumn("in_sections"));
		}

		ov._rstm.setColumnDescription("in_sections", "The sections the statement is in, and its place there (#1 = the most). Click: go to the section, and the statement is marked there.");
		ov._rstm.setColumnDescription("largest_pct", "The largest of the pct_ columns: what the overview is ordered by (in bold in the row)");
		for (Measure m : measures)
			ov._rstm.setColumnDescription("pct_" + m.name, m.description);

		return ov;
	}

	/** Do not show this column (for example a column that is only used to find the statement) */
	public void hideColumn(String colName)
	{
		_hiddenCols.add(colName);
	}

	/**
	 * The description of the section: what the overview is, and what 'all the statements' are (the vendor's note)
	 *
	 * @param measuresText  the measures in words, for example "time, disk reads, temp written, WAL, calls and planning time"
	 * @param allStmntsNote what 'all the statements' are for this vendor (what the collector or the DBMS keeps), for example "the statements pg_stat_statements kept"
	 */
	public void setDescription(String measuresText, String allStmntsNote)
	{
		_rstm.setDescription(
				"<b>Overview</b>: the statements that use the largest <b>share</b> of what all the statements did in the period. " +
				"Each <code>pct_</code> column is the statement's share (in percent) of all the statements' " + measuresText + ". " +
				"There are no weights: a share is comparable whatever the unit, so a statement that does 60% of all the reads is here even if its time is small. " +
				"Ordered by the largest share (in bold: why the statement is here). " +
				"<code>in_sections</code> says which of the sections below the statement is in, and at what place: click it to go there (the statement is marked), the charts and the SQL Text are there.<br>" +
				"Note: 'all the statements' are " + allStmntsNote + ".<br>");
	}

	/** Write the overview table (the section links are only links in the full report: the mail does not have the other sections) */
	public void writeHtml(Writer w, ReportEntryAbstract entry, int topRows)
	throws IOException
	{
		w.append(entry.getSectionDescriptionHtml(_rstm, true));
		w.append("Row Count: " + _rstm.getRowCount() + " (the top " + topRows + " on their largest share)<br>\n");

		final boolean isFull = entry.isFullMessageType();
		TableStringRenderer tableRender = new ReportEntryAbstract.ReportEntryTableStringRenderer()
		{
			@Override
			public boolean isColumnVisible(ResultSetTableModel rstm, int col, String colName)
			{
				return ! _hiddenCols.contains(colName);
			}

			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				// The SQL Text as a popup
				if (colName.equals(_textCol))
				{
					return "<div title='Click for Details' "
							+ "data-bs-toggle='modal' "
							+ "data-bs-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(rstm.getValueAsString(row, _idCol), "")) + "' "
							+ "data-tooltip=\""   + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(rstm.getValueAsString(row, _textCol), "")) + "\" "
							+ ">&#x1F4AC;</div>";
				}
				// A numeric id (Postgres queryid): on one line, it is not that long (wrapped it makes the rows taller)
				if (colName.equals(_idCol) && objVal instanceof Number)
					return "<span style='white-space:nowrap; font-family:Consolas, \"Courier New\", monospace; font-size:11px;'>" + strVal + "</span>";

				// A long id (MySQL digest, Db2 EXECUTABLE_ID: 64 chars): small monospace that may wrap (as ReportEntryAbstract.infoValue)
				if (colName.equals(_idCol) && strVal != null && strVal.length() > 24 && strVal.matches("[^\\s<>]+"))
					return "<span style='display:inline-block; max-width:24ch; white-space:normal; word-break:break-all; font-family:Consolas, \"Courier New\", monospace; font-size:11px;'>" + strVal + "</span>";

				// The largest share in bold: that is why the statement is in the overview
				if (colName.startsWith("pct_") && objVal instanceof java.math.BigDecimal && rstm.getValueAsBigDecimal(row, "largest_pct") != null
						&& ((java.math.BigDecimal) objVal).compareTo(rstm.getValueAsBigDecimal(row, "largest_pct")) == 0)
					return "<b>" + strVal + "</b>";

				// The links to the sections: only in the full report
				if ("in_sections".equals(colName) && ! isFull)
					return strVal.replaceAll("<a [^>]*>", "").replace("</a>", "");

				return strVal;
			}
		};
		w.append(_rstm.toHtmlTableString("sortable", true, true, null, tableRender));

		// The links in 'in_sections' mark the statement's row in the section they go to
		if (isFull)
			w.append(ReportEntryAbstract.GOTO_ROW_JS);
	}
}
