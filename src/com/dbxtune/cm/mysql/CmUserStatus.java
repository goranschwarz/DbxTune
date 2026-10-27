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
package com.dbxtune.cm.mysql;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.MySqlVariablesDictionary;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmUserStatus
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmUserStatus.class.getSimpleName();
	public static final String   SHORT_NAME       = "User Status";
	public static final String   HTML_DESC        =
		"<html>"
		+ "The status variables per user (performance_schema.status_by_user, the numeric ones): the sessions of the user that are connected now, and the ones that have ended.<br>"
		+ "<br>"
		+ "<b>VALUE_TYPE</b>: <b>counter</b> = it only grows (Diff and Rate is what happened in the sample), <b>gauge</b> = a value right now (shown as it is in Abs, Diff and Rate).<br>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "status_by_user"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VARIABLE_VALUE"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmUserStatus(counterController, guiController);
	}

	public CmUserStatus(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmGlobalStatusPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "The status variables per user (the numeric ones).");

			mtd.addColumn(CM_NAME, "USER",           "<html>Name of the user. '-system-' = the server's own (background) threads.</html>");
			mtd.addColumn(CM_NAME, "VARIABLE_NAME",  "<html>Name of the status variable. (hover over a cell to see what it is)</html>");
			mtd.addColumn(CM_NAME, "VARIABLE_VALUE", "<html>Value of the status variable, for the sessions of the user (connected now, and ended).<br>"
			                                       + "For a <b>counter</b> (VALUE_TYPE): Diff and Rate is what happened in the sample. For a <b>gauge</b>: the value right now, in Abs, Diff and Rate.</html>");
			mtd.addColumn(CM_NAME, "VALUE_TYPE",     "<html><b>counter</b> = it only grows. <b>gauge</b> = a value right now.<br><b>Formula</b>: gauge if the name matches the list in CmGlobalStatus.GAUGE_REGEX.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.status_by_user");
	}

	/**
	 * A gauge (VALUE_TYPE = 'gauge') is a value right now, not a counter that only grows.<br>
	 * The framework makes a diff (and a rate) of EVERY row in a DIFF column, so for a gauge we put back the absolute value in diff.
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_type  = diffData.findColumn("VALUE_TYPE");
		int pos_value = diffData.findColumn("VARIABLE_VALUE");
		if (pos_type == -1 || pos_value == -1)
			return;

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			if ( ! "gauge".equals(diffData.getValueAt(r, pos_type)) )
				continue;

			int absRow = newSample.getRowNumberForPkValue(diffData.getPkValue(r));
			if (absRow != -1)
				diffData.setValueAt(newSample.getValueAt(absRow, pos_value), r, pos_value);
		}
	}

	/** Same as in localCalculation(): for a gauge the rate is the absolute value (which is in diff by now) */
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
		int pos_type  = rateData.findColumn("VALUE_TYPE");
		int pos_value = rateData.findColumn("VARIABLE_VALUE");
		if (pos_type == -1 || pos_value == -1)
			return;

		for (int r = 0; r < rateData.getRowCount(); r++)
		{
			if ( ! "gauge".equals(rateData.getValueAt(r, pos_type)) )
				continue;

			// The rate is a BigDecimal with 1 decimal (as computeRatePerSec() makes it)
			int diffRow = diffData.getRowNumberForPkValue(rateData.getPkValue(r));
			if (diffRow != -1 && diffData.getValueAt(diffRow, pos_value) instanceof Number)
				rateData.setValueAt(new BigDecimal( ((Number)diffData.getValueAt(diffRow, pos_value)).doubleValue() ).setScale(1, RoundingMode.HALF_EVEN), r, pos_value);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("USER");
		pkCols.add("VARIABLE_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = 
			"select /* ${cmCollectorName} */ \n" +
			"       IFNULL(USER, '-system-') as USER \n" +
			"      ,VARIABLE_NAME \n" +
			"      ,cast(VARIABLE_VALUE as signed integer) as VARIABLE_VALUE \n" +
			"      ,CASE WHEN VARIABLE_NAME REGEXP '" + CmGlobalStatus.GAUGE_REGEX + "' THEN 'gauge' ELSE 'counter' END as VALUE_TYPE \n" +
			"from performance_schema.status_by_user \n" +
			"where VARIABLE_VALUE REGEXP '^[[:digit:]]+$' \n" +
			"  and length(VARIABLE_VALUE) <= 19  -- MaxBigInt=9,223,372,036,854,775,807 so discard numbers that are *big*... Note: the cast to signed int, becomes a bigint \n"
			;

		return sql;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("VARIABLE_NAME".equals(colName))
		{
			return cellValue == null ? null : MySqlVariablesDictionary.getInstance().getDescriptionHtml(cellValue.toString());
		}

		if ("VARIABLE_VALUE".equals(colName) )
		{
			int pos_key = findColumn("VARIABLE_NAME");
			if (pos_key >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_key);
				if (cellVal instanceof String)
				{
					return MySqlVariablesDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}
