/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Alarm-Coverage*, *## Batch-Coverage*
 *                                      *Error-Tracking* and *Report-Handling*
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
package com.dbxtune.cm;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Declarative descriptor for a chart to show alongside a CM's tabular data
 * in the web UI (Counter Details panel).
 * <p>
 * CMs that want charts override {@link CountersModel#getChartDescriptors()}
 * and return one or more descriptors. The servlet serialises them as JSON,
 * and the JavaScript renderer (Chart.js) creates the charts automatically.
 * <p>
 * This keeps all chart <em>configuration</em> in the CM (server-side) while
 * the <em>rendering</em> stays in the web UI — mirroring how the Swing GUI's
 * {@code createExtendedInfoPanel()} works, but in a declarative, UI-agnostic way.
 * <p>
 * The Swing GUI could also read these descriptors to auto-generate JFreeChart
 * panels for CMs that don't have a hand-coded panel.
 *
 * @see CountersModel#getChartDescriptors()
 */
public class CmChartDescriptor implements Serializable
{
	private static final long serialVersionUID = 1L;

	// ---- Chart types ----
	/** Simple pie chart. */
	public static final String CHART_TYPE_PIE            = "pie";
	/** Vertical bar chart. */
	public static final String CHART_TYPE_BAR            = "bar";
	/** Stacked bar chart (e.g. usage-percent bars). */
	public static final String CHART_TYPE_STACKED_BAR    = "stacked-bar";
	/** Dual mode: user can toggle between pie and stacked bar. */
	public static final String CHART_TYPE_DUAL_PIE_BAR   = "dual-pie-bar";
	/** Horizontal bar chart. */
	public static final String CHART_TYPE_HORIZONTAL_BAR = "horizontal-bar";

	// ---- Split directions ----
	/** Graph above or below the table (vertical split). */
	public static final String SPLIT_VERTICAL   = "vertical";
	/** Graph beside the table (horizontal split). */
	public static final String SPLIT_HORIZONTAL = "horizontal";


	// =========================================================================
	// Fields — all public for easy Jackson serialisation and JS consumption.
	// =========================================================================

	/** Unique id within this CM, e.g. "log-usage", "wait-pie". Used as HTML element id suffix. */
	public String   id;

	/** Chart title shown above the chart. */
	public String   title;

	/** One of the CHART_TYPE_* constants. */
	public String   chartType = CHART_TYPE_DUAL_PIE_BAR;

	/** Layout direction: "vertical" (graph top/bottom) or "horizontal" (graph beside table). */
	public String   splitDir  = SPLIT_HORIZONTAL;


	// ---- Data mapping ----

	/** Column name used for category labels (X-axis / pie slices). E.g. "DBName", "wait_type". */
	public String   labelColumn;

	/** Column names whose values form the chart series / datasets. */
	public String[] valueColumns;

	/** Human-readable names for each series (parallel to {@link #valueColumns}). Optional — defaults to column names. */
	public String[] seriesLabels;


	// ---- Aggregation / filtering ----

	/**
	 * If set, rows are aggregated by this column instead of using
	 * {@link #labelColumn} directly. E.g. group waits by "wait_event_type" (class).
	 * The grouped column becomes the label; valueColumns are aggregated per group
	 * using the function specified by {@link #groupAggFunc}.
	 */
	public String   groupByColumn;

	// ---- Aggregation functions ----
	/** Sum values (default). */
	public static final String AGG_SUM = "sum";
	/** Average values. */
	public static final String AGG_AVG = "avg";
	/** Take the maximum value. */
	public static final String AGG_MAX = "max";

	/**
	 * Aggregation function used when {@link #groupByColumn} causes multiple rows
	 * to collapse into one group. Default "sum". Use "avg" or "max" for percentages
	 * where summing produces nonsensical values (e.g. OS disk used% across databases).
	 */
	public String   groupAggFunc = AGG_SUM;

	/**
	 * Skip rows where {@link #labelColumn} equals this value.
	 * E.g. "_Total" for CmMemoryClerks which has an aggregate row.
	 */
	public String   skipValue;

	/**
	 * Skip rows where ALL {@link #valueColumns} are zero or null.
	 * Useful for sparse datasets (e.g. device IO where most devices are idle).
	 */
	public boolean  skipZeroRows = false;


	// ---- Percentage / threshold mode ----

	/**
	 * If true, values are percentages (0–100). Enables threshold colouring:
	 * green &lt; {@link #thresholdWarn}, orange &lt; {@link #thresholdCrit}, red above.
	 */
	public boolean  isPercent      = false;
	/** Orange threshold (only when {@link #isPercent} is true). */
	public double   thresholdWarn  = 80.0;
	/** Red threshold (only when {@link #isPercent} is true). */
	public double   thresholdCrit  = 90.0;


	// ---- Pie-specific ----

	/**
	 * For pie charts: slices smaller than this fraction (0.0–1.0) are grouped into "Others".
	 * Default 0.05 = 5%.
	 */
	public double   pieOtherLimit  = 0.05;


	// ---- Bar label / annotation ----

	/**
	 * Column name whose value is displayed ON each bar (e.g. "FREE MB: 3 144").
	 * Used by the web renderer to show a text label inside or next to each bar.
	 * Mirrors the native Swing GUI's {@code "FREE MB: " + sizeStr} series labels.
	 * <p>
	 * The prefix shown before the value (default "FREE MB: ").
	 * Set {@link #barLabelPrefix} to customise.
	 */
	public String   barLabelColumn  = null;

	/** Prefix text printed before the {@link #barLabelColumn} value on each bar. */
	public String   barLabelPrefix  = "FREE MB: ";


	// ---- Layout ----

	/**
	 * Split ratio (0.0–1.0): fraction of space for the <em>table</em>.
	 * E.g. 0.6 = 60% table, 40% chart. Default 0.5.
	 */
	public double   splitRatio     = 0.5;

	/**
	 * Maximum number of items to show. 0 = unlimited.
	 * For pie charts, remaining items go to "Others".
	 * For bar charts, remaining items are hidden.
	 */
	public int      maxItems       = 0;


	// =========================================================================
	// Builder-style setters for fluent construction in CM code.
	// =========================================================================

	public CmChartDescriptor id(String id)                  { this.id             = id;       return this; }
	public CmChartDescriptor title(String title)            { this.title          = title;    return this; }
	public CmChartDescriptor chartType(String type)         { this.chartType      = type;     return this; }
	public CmChartDescriptor splitDir(String dir)           { this.splitDir       = dir;      return this; }
	public CmChartDescriptor labelColumn(String col)        { this.labelColumn    = col;      return this; }
	public CmChartDescriptor valueColumns(String... cols)   { this.valueColumns   = cols;     return this; }
	public CmChartDescriptor seriesLabels(String... labels) { this.seriesLabels   = labels;   return this; }
	public CmChartDescriptor groupByColumn(String col)      { this.groupByColumn  = col;      return this; }
	public CmChartDescriptor groupAggFunc(String func)      { this.groupAggFunc   = func;     return this; }
	public CmChartDescriptor skipValue(String val)          { this.skipValue      = val;      return this; }
	public CmChartDescriptor skipZeroRows(boolean b)        { this.skipZeroRows   = b;        return this; }
	public CmChartDescriptor isPercent(boolean b)           { this.isPercent      = b;        return this; }
	public CmChartDescriptor thresholdWarn(double v)        { this.thresholdWarn  = v;        return this; }
	public CmChartDescriptor thresholdCrit(double v)        { this.thresholdCrit  = v;        return this; }
	public CmChartDescriptor pieOtherLimit(double v)        { this.pieOtherLimit  = v;        return this; }
	public CmChartDescriptor splitRatio(double v)           { this.splitRatio     = v;        return this; }
	public CmChartDescriptor maxItems(int n)                { this.maxItems       = n;        return this; }
	public CmChartDescriptor barLabelColumn(String col)     { this.barLabelColumn = col;      return this; }
	public CmChartDescriptor barLabelPrefix(String prefix)  { this.barLabelPrefix = prefix;   return this; }

	@Override
	public String toString()
	{
		return "CmChartDescriptor{id='" + id + "', chartType='" + chartType + "', label='" + labelColumn
				+ "', values=" + (valueColumns != null ? Arrays.toString(valueColumns) : "null") + "}";
	}
}
