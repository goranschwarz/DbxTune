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
package com.dbxtune.cm;

import java.io.Serializable;

/**
 * Declarative descriptor for conditional row/cell highlighting in the web UI
 * Counter Details panel and (potentially) the native Swing GUI.
 * <p>
 * CMs that want highlighting override {@link CountersModel#getHighlighterDescriptors()}
 * and return one or more descriptors.  The servlet serialises them as JSON and
 * the JavaScript renderer applies the CSS styling automatically.
 * <p>
 * The native Swing GUI panels (e.g. CmDatabasesPanel) can also read these
 * descriptors and translate them into SwingX {@code ColorHighlighter}s, allowing
 * a single source of truth for highlighting rules.
 *
 * <h3>Condition semantics</h3>
 * <ul>
 *   <li><b>Numeric comparison:</b> {@code column OP value}
 *       — operators: GT, GE, LT, LE, EQ, NE</li>
 *   <li><b>Column-to-column comparison:</b> {@code column OP compareColumn}
 *       — operators: GT_COL, GE_COL, LT_COL, LE_COL, EQ_COL, NE_COL</li>
 *   <li><b>String matching:</b> {@code column OP value}
 *       — operators: STARTS_WITH, CONTAINS, EQUALS, NOT_EMPTY, IS_EMPTY</li>
 *   <li><b>Boolean check:</b> {@code column OP}
 *       — operators: IS_TRUE, IS_FALSE</li>
 * </ul>
 *
 * <h3>Scope</h3>
 * <ul>
 *   <li>{@link #SCOPE_ROW} — the entire row is highlighted.</li>
 *   <li>{@link #SCOPE_CELL} — only the column(s) named in {@link #highlightColumns}
 *       are highlighted (defaults to {@link #column} if not set).</li>
 * </ul>
 *
 * @see CountersModel#getHighlighterDescriptors()
 */
public class CmHighlighterDescriptor implements Serializable
{
	private static final long serialVersionUID = 1L;

	// ---- Operators ----
	// Numeric comparisons (column vs constant value)
	public static final String OP_GT = "GT";
	public static final String OP_GE = "GE";
	public static final String OP_LT = "LT";
	public static final String OP_LE = "LE";
	public static final String OP_EQ = "EQ";
	public static final String OP_NE = "NE";

	// Column-to-column comparisons
	public static final String OP_GT_COL = "GT_COL";
	public static final String OP_GE_COL = "GE_COL";
	public static final String OP_LT_COL = "LT_COL";
	public static final String OP_LE_COL = "LE_COL";
	public static final String OP_EQ_COL = "EQ_COL";
	public static final String OP_NE_COL = "NE_COL";

	// String operations
	public static final String OP_STARTS_WITH  = "STARTS_WITH";
	public static final String OP_CONTAINS     = "CONTAINS";
	public static final String OP_EQUALS       = "EQUALS";
	public static final String OP_NOT_EQUALS   = "NOT_EQUALS";
	public static final String OP_NOT_EMPTY    = "NOT_EMPTY";
	public static final String OP_IS_EMPTY     = "IS_EMPTY";

	// Boolean
	public static final String OP_IS_TRUE  = "IS_TRUE";
	public static final String OP_IS_FALSE = "IS_FALSE";

	// ---- Scope ----
	/** Highlight the entire row. */
	public static final String SCOPE_ROW  = "ROW";
	/** Highlight only specific column(s). */
	public static final String SCOPE_CELL = "CELL";


	// =========================================================================
	// Fields — all public for easy Jackson serialisation and JS consumption.
	// =========================================================================

	/** Human-readable name for this rule, e.g. "Blocked Process". */
	public String   name;

	/** Column name to evaluate in the condition. */
	public String   column;

	/** Operator — one of the OP_* constants. */
	public String   operator;

	/**
	 * Value to compare against (for numeric/string operators).
	 * Stored as Object to support both Number and String.
	 * Ignored for OP_NOT_EMPTY, OP_IS_EMPTY, OP_IS_TRUE, OP_IS_FALSE.
	 */
	public Object   value;

	/**
	 * For column-to-column operators (OP_*_COL): the other column name.
	 */
	public String   compareColumn;

	/** {@link #SCOPE_ROW} or {@link #SCOPE_CELL}. Default is ROW. */
	public String   scope = SCOPE_ROW;

	/**
	 * For CELL scope: column name(s) to highlight.
	 * Defaults to {@link #column} if not set.
	 */
	public String[] highlightColumns;

	/**
	 * Background color — any valid CSS colour string.
	 * Examples: "#FFFF00", "rgba(255,200,200,0.5)", "yellow", "pink".
	 */
	public String   bgColor;

	/**
	 * Foreground (text) color — any valid CSS colour string. Optional.
	 */
	public String   fgColor;

	/**
	 * Priority / order. Lower numbers are evaluated first.
	 * When multiple rules match the same cell, the LAST match wins
	 * (so higher priority = later in the list = overrides earlier).
	 * Default 100.
	 */
	public int      priority = 100;


	// =========================================================================
	// Fluent builder methods
	// =========================================================================

	public CmHighlighterDescriptor name(String v)               { this.name             = v;  return this; }
	public CmHighlighterDescriptor column(String v)             { this.column           = v;  return this; }
	public CmHighlighterDescriptor operator(String v)           { this.operator         = v;  return this; }
	public CmHighlighterDescriptor value(Object v)              { this.value            = v;  return this; }
	public CmHighlighterDescriptor compareColumn(String v)      { this.compareColumn    = v;  return this; }
	public CmHighlighterDescriptor scope(String v)              { this.scope            = v;  return this; }
	public CmHighlighterDescriptor highlightColumns(String...v) { this.highlightColumns = v;  return this; }
	public CmHighlighterDescriptor bgColor(String v)            { this.bgColor          = v;  return this; }
	public CmHighlighterDescriptor fgColor(String v)            { this.fgColor          = v;  return this; }
	public CmHighlighterDescriptor priority(int v)              { this.priority         = v;  return this; }

	// ---- Convenience shortcuts ----

	/** Scope = ROW (entire row highlighted). This is the default. */
	public CmHighlighterDescriptor scopeRow()                  { this.scope = SCOPE_ROW;     return this; }
	/** Scope = CELL (only specific columns highlighted). */
	public CmHighlighterDescriptor scopeCell()                 { this.scope = SCOPE_CELL;    return this; }

	/** Numeric: column &gt; value */
	public CmHighlighterDescriptor gt(String col, Number val)        { return column(col).operator(OP_GT).value(val); }
	/** Numeric: column &gt;= value */
	public CmHighlighterDescriptor ge(String col, Number val)        { return column(col).operator(OP_GE).value(val); }
	/** Numeric: column &lt; value */
	public CmHighlighterDescriptor lt(String col, Number val)        { return column(col).operator(OP_LT).value(val); }
	/** Numeric: column &lt;= value */
	public CmHighlighterDescriptor le(String col, Number val)        { return column(col).operator(OP_LE).value(val); }
	/** Numeric: column == value */
	public CmHighlighterDescriptor eq(String col, Number val)        { return column(col).operator(OP_EQ).value(val); }
	/** Numeric: column != value */
	public CmHighlighterDescriptor ne(String col, Number val)        { return column(col).operator(OP_NE).value(val); }

	/** Column-to-column: column &gt; compareColumn */
	public CmHighlighterDescriptor gtCol(String col, String other)   { return column(col).operator(OP_GT_COL).compareColumn(other); }
	/** Column-to-column: column &gt;= compareColumn */
	public CmHighlighterDescriptor geCol(String col, String other)   { return column(col).operator(OP_GE_COL).compareColumn(other); }
	/** Column-to-column: column &lt; compareColumn */
	public CmHighlighterDescriptor ltCol(String col, String other)   { return column(col).operator(OP_LT_COL).compareColumn(other); }
	/** Column-to-column: column == compareColumn */
	public CmHighlighterDescriptor eqCol(String col, String other)   { return column(col).operator(OP_EQ_COL).compareColumn(other); }
	/** Column-to-column: column != compareColumn */
	public CmHighlighterDescriptor neCol(String col, String other)   { return column(col).operator(OP_NE_COL).compareColumn(other); }
	/** Column-to-column: column &lt;= compareColumn */
	public CmHighlighterDescriptor leCol(String col, String other)   { return column(col).operator(OP_LE_COL).compareColumn(other); }

	/** String: column starts with value */
	public CmHighlighterDescriptor startsWith(String col, String val){ return column(col).operator(OP_STARTS_WITH).value(val); }
	/** String: column contains value */
	public CmHighlighterDescriptor contains(String col, String val)  { return column(col).operator(OP_CONTAINS).value(val); }
	/** String: column equals value */
	public CmHighlighterDescriptor strEquals(String col, String val)    { return column(col).operator(OP_EQUALS).value(val); }
	/** String: column does not equal value */
	public CmHighlighterDescriptor notEquals(String col, String val)    { return column(col).operator(OP_NOT_EQUALS).value(val); }
	/** String: column is not null and not empty */
	public CmHighlighterDescriptor notEmpty(String col)              { return column(col).operator(OP_NOT_EMPTY); }
	/** String: column is null or empty */
	public CmHighlighterDescriptor isEmpty(String col)               { return column(col).operator(OP_IS_EMPTY); }

	/** Boolean: column is true */
	public CmHighlighterDescriptor isTrue(String col)                { return column(col).operator(OP_IS_TRUE); }
	/** Boolean: column is false */
	public CmHighlighterDescriptor isFalse(String col)               { return column(col).operator(OP_IS_FALSE); }
}
