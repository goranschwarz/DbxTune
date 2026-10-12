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
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmLibraryCache
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLibraryCache.class.getSimpleName();
	public static final String   SHORT_NAME       = "Library Cache";
	public static final String   HTML_DESC        =
		"<html>" +
		"The library cache: where Oracle keeps parsed SQL statements, PL/SQL and the definitions of the objects they use. One row per type of object (namespace).<br>" +
		"<br>" +
		"<ul>" +
		"  <li><code>GETS</code> / <code>GET_HIT_PCT</code> - lookups of an object, and how many that found it in the cache.</li>" +
		"  <li><code>PINS</code> / <code>PIN_HIT_PCT</code> - executions (uses) of an object, and how many that found everything they needed in the cache.</li>" +
		"  <li><code>RELOADS</code>       - the object was in the cache, but had been thrown out and had to be loaded (parsed) again. Many reloads means that the shared pool is too small.</li>" +
		"  <li><code>INVALIDATIONS</code> - the object was made invalid, and has to be parsed again. Caused by DDL and by statistics gathering on the tables it uses.</li>" +
		"</ul>" +
		"The namespace <b>SQL AREA</b> is the SQL statements, <b>TABLE/PROCEDURE</b> is tables, views, procedures and packages.<br>" +
		"In the 'diff' and 'rate' views the percentages are for the sample, in 'abs' since the instance was started.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$LIBRARYCACHE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"GETS", "GETHITS", "PINS", "PINHITS", "RELOADS", "INVALIDATIONS"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmLibraryCache(counterController, guiController);
	}

	public CmLibraryCache(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_HIT_PCT = "LibCacheHitPct";
	public static final String GRAPH_NAME_RELOADS = "LibCacheReloads";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_HIT_PCT,
			"Library Cache, Hit Percent", 	                // Menu CheckBox text
			"Library Cache Hit Percent in the sample: all Namespaces together, and SQL AREA (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			new String[] {"Pin Hit % (all)", "Get Hit % (all)", "Pin Hit % (SQL AREA)", "Get Hit % (SQL AREA)"},
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_RELOADS,
			"Library Cache, Reloads and Invalidations per Second", 	                // Menu CheckBox text
			"Library Cache Reloads and Invalidations per Second, all Namespaces together (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Reloads", "Invalidations"},
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("NAMESPACE");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$LIBRARYCACHE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every namespace. This looks the same in all versions (newer versions has more namespaces)
		// GET_HIT_PCT and PIN_HIT_PCT is since the instance was started here: for DIFF and RATE they are calculated for the sample in localCalculation()
		// Note: CAST(... as NUMBER(5,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     l.NAMESPACE \n"
			+ "    ,l.GETS \n"
			+ "    ,l.GETHITS \n"
			+ "    ,CAST(100 * l.GETHITS / NULLIF(l.GETS, 0) as NUMBER(5,1)) as GET_HIT_PCT \n"
			+ "    ,l.PINS \n"
			+ "    ,l.PINHITS \n"
			+ "    ,CAST(100 * l.PINHITS / NULLIF(l.PINS, 0) as NUMBER(5,1)) as PIN_HIT_PCT \n"
			+ "    ,l.RELOADS \n"
			+ "    ,l.INVALIDATIONS \n"
			+ "from V$LIBRARYCACHE l \n"
			+ "order by l.PINS desc, l.NAMESPACE \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The library cache (parsed SQL, PL/SQL and object definitions), one row per type of object (namespace).");

			mtd.addColumn(CM_NAME, "NAMESPACE",     "<html>Type of object, the most important: <b>SQL AREA</b> = SQL statements, <b>TABLE/PROCEDURE</b> = tables, views, procedures and packages, <b>BODY</b> = package bodies, <b>TRIGGER</b>, <b>INDEX</b>.</html>");
			mtd.addColumn(CM_NAME, "GETS",          "<html>Number of lookups of an object in this namespace.</html>");
			mtd.addColumn(CM_NAME, "GETHITS",       "<html>Number of lookups where the object was found in the cache.</html>");
			mtd.addColumn(CM_NAME, "GET_HIT_PCT",   "<html>GETHITS in percent of GETS. In 'abs': since the instance was started. In 'diff' and 'rate': in the sample. NULL (or 100 in the sample) if there was no lookups.</html>");
			mtd.addColumn(CM_NAME, "PINS",          "<html>Number of times an object in this namespace was used (executed).</html>");
			mtd.addColumn(CM_NAME, "PINHITS",       "<html>Number of uses where everything that was needed was found in the cache.</html>");
			mtd.addColumn(CM_NAME, "PIN_HIT_PCT",   "<html>PINHITS in percent of PINS. In 'abs': since the instance was started. In 'diff' and 'rate': in the sample.</html>");
			mtd.addColumn(CM_NAME, "RELOADS",       "<html>Number of times an object had to be loaded (parsed) again, because it had been thrown out of the cache. Many reloads means that the shared pool is too small.</html>");
			mtd.addColumn(CM_NAME, "INVALIDATIONS", "<html>Number of times an object was made invalid (by DDL, or by statistics gathering on a table it uses), so it has to be parsed again.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * GET_HIT_PCT and PIN_HIT_PCT: The SQL gives the values since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want them for the lookups that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_GETS        = diffData.findColumn("GETS");
		int pos_GETHITS     = diffData.findColumn("GETHITS");
		int pos_GET_HIT_PCT = diffData.findColumn("GET_HIT_PCT");
		int pos_PINS        = diffData.findColumn("PINS");
		int pos_PINHITS     = diffData.findColumn("PINHITS");
		int pos_PIN_HIT_PCT = diffData.findColumn("PIN_HIT_PCT");

		if (pos_GETS == -1 || pos_GETHITS == -1 || pos_GET_HIT_PCT == -1 || pos_PINS == -1 || pos_PINHITS == -1 || pos_PIN_HIT_PCT == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: GETS, GETHITS, GET_HIT_PCT, PINS, PINHITS, PIN_HIT_PCT.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long gets    = diffData.getValueAsLong(rowId, pos_GETS);
			long getHits = diffData.getValueAsLong(rowId, pos_GETHITS);
			long pins    = diffData.getValueAsLong(rowId, pos_PINS);
			long pinHits = diffData.getValueAsLong(rowId, pos_PINHITS);

			// Nothing happened in the sample: 100 (nothing was missed)
			BigDecimal getHitPct = (gets <= 0) ? new BigDecimal(100) : MathUtils.roundToBigDecimal(Math.min(100, 100.0 * getHits / gets), 1);
			BigDecimal pinHitPct = (pins <= 0) ? new BigDecimal(100) : MathUtils.roundToBigDecimal(Math.min(100, 100.0 * pinHits / pins), 1);

			diffData.setValueAt(getHitPct, rowId, pos_GET_HIT_PCT);
			diffData.setValueAt(pinHitPct, rowId, pos_PIN_HIT_PCT);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0 || ! this.hasDiffData())
			return;

		//--------------------------------------------------------
		// Hit percent in the sample: all namespaces together, and SQL AREA
		//--------------------------------------------------------
		if (GRAPH_NAME_HIT_PCT.equals(tgdp.getName()))
		{
			double gets = 0, getHits = 0, pins = 0, pinHits = 0;
			for (int r = 0; r < this.size(); r++)
			{
				gets    += this.getDiffValueAsDouble(r, "GETS",    0d);
				getHits += this.getDiffValueAsDouble(r, "GETHITS", 0d);
				pins    += this.getDiffValueAsDouble(r, "PINS",    0d);
				pinHits += this.getDiffValueAsDouble(r, "PINHITS", 0d);
			}

			Double sqlAreaPinHitPct = this.getDiffValueAsDouble(createPkStr("SQL AREA"), "PIN_HIT_PCT");
			Double sqlAreaGetHitPct = this.getDiffValueAsDouble(createPkStr("SQL AREA"), "GET_HIT_PCT");

			Double[] arr = new Double[4];
			arr[0] = (pins <= 0) ? 100d : Math.min(100d, 100.0 * pinHits / pins);
			arr[1] = (gets <= 0) ? 100d : Math.min(100d, 100.0 * getHits / gets);
			arr[2] = (sqlAreaPinHitPct == null) ? 100d : sqlAreaPinHitPct;
			arr[3] = (sqlAreaGetHitPct == null) ? 100d : sqlAreaGetHitPct;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		// Reloads and Invalidations per second, for all namespaces together
		//--------------------------------------------------------
		if (GRAPH_NAME_RELOADS.equals(tgdp.getName()))
		{
			String[] colNames = new String[] {"RELOADS", "INVALIDATIONS"};
			Double[] arr      = new Double[colNames.length];
			for (int i = 0; i < arr.length; i++)
			{
				double sum = 0;
				for (int r = 0; r < this.size(); r++)
				{
					Double val = this.getRateValueAsDouble(r, colNames[i]);
					if (val != null && val > 0)
						sum += val;
				}
				arr[i] = sum;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
