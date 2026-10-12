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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
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
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgaStat
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgaStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "PGA";
	public static final String   HTML_DESC        =
		"<html>" +
		"The PGA: the private memory of all server processes together (sorts, hash joins, session memory).<br>" +
		"<br>" +
		"The most important rows:" +
		"<ul>" +
		"  <li><b>aggregate PGA target parameter</b> - what Oracle tries to stay below (<code>pga_aggregate_target</code>). It is a target, not a limit.</li>" +
		"  <li><b>total PGA allocated</b>            - what the processes have right now.</li>" +
		"  <li><b>total PGA inuse</b>                - what of that is really in use.</li>" +
		"  <li><b>maximum PGA allocated</b>          - the highest value since the instance was started.</li>" +
		"  <li><b>over allocation count</b>          - how many times Oracle had to go above the target. If it grows, the target is too small.</li>" +
		"  <li><b>cache hit percentage</b>           - how much of the sort/hash work that fitted in memory (since the instance was started). Below 100 means work was done on disk (see the tab 'Temp Usage').</li>" +
		"</ul>" +
		"From 12.1 there is also a hard limit (<code>pga_aggregate_limit</code>): above that sessions are terminated with ORA-04036, which sends an alarm from the tab 'Alert Log'.<br>" +
		"<br>" +
		"<b>Note</b>: In the 'diff' and 'rate' views only the counters makes sense (over allocation count, bytes processed, extra bytes read/written, PGA memory freed back to OS).<br>" +
		"<b>Note</b>: In a Container Database, connected to CDB$ROOT: the values are for the whole instance (what each PDB uses is not shown).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$PGASTAT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VALUE"};

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

		return new CmPgaStat(counterController, guiController);
	}

	public CmPgaStat(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_PGA_MB     = "PgaMb";
	public static final String GRAPH_NAME_OVER_ALLOC = "PgaOverAlloc";

	// The rows (NAME) in the MB graph
	private static final String[] STATS_PGA_MB = new String[] {"aggregate PGA target parameter", "total PGA allocated", "total PGA inuse", "maximum PGA allocated"};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_PGA_MB,
			"PGA, Target/Allocated/In Use in MB", 	                // Menu CheckBox text
			"PGA Memory in MB: Target, Allocated, In Use and the Maximum since startup (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"PGA Target MB", "PGA Allocated MB", "PGA In Use MB", "PGA Max Allocated MB (since startup)"},
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OVER_ALLOC,
			"PGA, Over Allocations", 	                // Menu CheckBox text
			"PGA Over Allocations in the sample: times Oracle had to go above the PGA Target (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Over Allocation Count"},
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$PGASTAT");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// 11.2 and above
		String sql = getSql("");

		// 12.1 and above: V$PGASTAT has CON_ID, and in some versions (seen in 23, not in 18) one set of rows for every container.
		// We want ONE set: the one with the lowest CON_ID we can see. Connected to CDB$ROOT that is 0 = the whole instance, connected to a PDB it's that PDB
		if (versionInfo.getLongVersion() >= Ver.ver(12,1))
		{
			String sqlCon = getSql("where p.CON_ID = (select min(x.CON_ID) from V$PGASTAT x) \n");

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCon;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCon + ") where 1=2"))
				{
					sql = sqlCon;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL with CON_ID (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, all rows of V$PGASTAT will be used. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private static String getSql(String whereOneContainer)
	{
		// One row for every PGA statistic (name, value, unit). Newer versions has more rows
		// VALUE_MB is there for the ones that are in bytes
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "select \n"
			+ "     p.NAME \n"
			+ "    ,CAST(p.VALUE as NUMBER(28,2))                                               as VALUE \n"
			+ "    ,p.UNIT \n"
			+ "    ,CAST(case when p.UNIT = 'bytes' then p.VALUE / 1048576 end as NUMBER(18,1)) as VALUE_MB \n"
			+ "from V$PGASTAT p \n"
			+ whereOneContainer
			+ "order by p.NAME \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The PGA: the private memory of all server processes together (sorts, hash joins, session memory).");

			mtd.addColumn(CM_NAME, "NAME",     "<html>Name of the statistic.</html>");
			mtd.addColumn(CM_NAME, "VALUE",    "<html>The value, in the unit of column UNIT.</html>");
			mtd.addColumn(CM_NAME, "UNIT",     "<html>bytes, percent, or NULL for a count.</html>");
			mtd.addColumn(CM_NAME, "VALUE_MB", "<html>VALUE in MB, for the rows where UNIT is bytes.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0)
			return;

		if (GRAPH_NAME_PGA_MB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[STATS_PGA_MB.length];
			for (int i = 0; i < arr.length; i++)
			{
				Double val = this.getAbsValueAsDouble(createPkStr(STATS_PGA_MB[i]), "VALUE_MB");
				arr[i] = (val == null) ? 0d : val;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OVER_ALLOC.equals(tgdp.getName()))
		{
			// The counter is since the instance was started: so use what was added in this sample
			Double val = this.hasDiffData() ? this.getDiffValueAsDouble(createPkStr("over allocation count"), "VALUE") : null;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), new Double[] {(val == null) ? 0d : val});
		}
	}
}
