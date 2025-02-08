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
package com.dbxtune.cm.rax;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.rax.gui.CmSummaryPanel;
import com.dbxtune.gui.ISummaryPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
//extends CountersModel
extends CmSummaryAbstract
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"xxxx"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();
		addPostRefreshTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_XXX             = "xxx";
//	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;

	private void addTrendGraphs()
	{
	}
//	private void addTrendGraphs()
//	{
//		String[] labels_xxx            = new String[] { "Hour", "Minute", "Second"};
//		
//		addTrendGraphData(GRAPH_NAME_XXX,             new TrendGraphDataPoint(GRAPH_NAME_XXX,             labels_xxx));
//
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_XXX,
//				"Dummy Graph", 	                        // Menu CheckBox text
//				"Dummy Graph showing hour, minute, second", // Label 
//				labels_xxx, 
//				true,  // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
//	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
//		CounterController.getInstance().setSummaryPanel( summaryPanel );
		getCounterController().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "ra_status";
	}
	
//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
////		long srvVersion = getServerVersion();
//
//		//---------------------------------
//		// GRAPH:
//		//---------------------------------
//		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
//		{	
//			Double[] arr = new Double[3];
//
//			int ms = (int) (System.currentTimeMillis() % 1000l);
//			ms = ms < 0 ? ms+1000 : ms;
//
//			Calendar now = Calendar.getInstance();
//			int hour   = now.get(Calendar.HOUR_OF_DAY);
//			int minute = now.get(Calendar.MINUTE);
//			int second = now.get(Calendar.SECOND);
//			
////			arr[0] = this.getAbsValueAsDouble (0, "Connections");
////			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
////			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
//			arr[0] = Double.valueOf(hour);
//			arr[1] = Double.valueOf(minute);
//			arr[2] = Double.valueOf(second);
//			_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");
//
//			// Set the values
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
//		}
//
//	}

	
	@Override
	public void doPostRefresh(LinkedHashMap<String, CountersModel> refreshedCms)
	{
//		CountersModel cmRaStat = CounterController.getInstance().getCmByName(CmRaStatistics.CM_NAME);
		CountersModel cmRaStat = getCounterController().getCmByName(CmRaStatistics.CM_NAME);
		if (cmRaStat == null)
		{
//			System.out.println("summary: doPostRefresh(): CmRaStatistics == null, return");
			return;
		}

		// Grab some information from CmRaStatistics, and insert into the Summary Storage
		// At the end call setSummaryData() to re-read values from the Model to the GUI 
		// Doing it in this way, seems a bit clumsy, but it will make reading from the "offline" storage *much* easier...

//		addRow(getCounterSampleAbs(),  "Time replication last started", cmRaStat.getAbsString ("Time replication last started", "Value"));
//		addRow(getCounterSampleDiff(), "Time replication last started", cmRaStat.getDiffString("Time replication last started", "Value"));
//		addRow(getCounterSampleRate(), "Time replication last started", cmRaStat.getRateString("Time replication last started", "Value"));
//
//		addRow(getCounterSampleAbs(),  "Time statistics last reset",    cmRaStat.getAbsString ("Time statistics last reset",    "Value"));
//		addRow(getCounterSampleDiff(), "Time statistics last reset",    cmRaStat.getDiffString("Time statistics last reset",    "Value"));
//		addRow(getCounterSampleRate(), "Time statistics last reset",    cmRaStat.getRateString("Time statistics last reset",    "Value"));
//
//		addRow(getCounterSampleAbs(),  "Number of LTL commands sent",   cmRaStat.getAbsString ("Number of LTL commands sent",   "NumberValue"));
//		addRow(getCounterSampleDiff(), "Number of LTL commands sent",   cmRaStat.getDiffString("Number of LTL commands sent",   "NumberValue"));
//		addRow(getCounterSampleRate(), "Number of LTL commands sent",   cmRaStat.getRateString("Number of LTL commands sent",   "NumberValue"));
//
//		addRow(getCounterSampleAbs(),  "Total operations scanned",      cmRaStat.getAbsString ("Total operations scanned",      "NumberValue"));
//		addRow(getCounterSampleDiff(), "Total operations scanned",      cmRaStat.getDiffString("Total operations scanned",      "NumberValue"));
//		addRow(getCounterSampleRate(), "Total operations scanned",      cmRaStat.getRateString("Total operations scanned",      "NumberValue"));
//
//		addRow(getCounterSampleAbs(),  "Total transactions processed",  cmRaStat.getAbsString ("Total transactions processed",  "NumberValue"));
//		addRow(getCounterSampleDiff(), "Total transactions processed",  cmRaStat.getDiffString("Total transactions processed",  "NumberValue"));
//		addRow(getCounterSampleRate(), "Total transactions processed",  cmRaStat.getRateString("Total transactions processed",  "NumberValue"));
//
//		addRow(getCounterSampleAbs(),  "Total bytes sent",              cmRaStat.getAbsString ("Total bytes sent",              "NumberValue"));
//		addRow(getCounterSampleDiff(), "Total bytes sent",              cmRaStat.getDiffString("Total bytes sent",              "NumberValue"));
//		addRow(getCounterSampleRate(), "Total bytes sent",              cmRaStat.getRateString("Total bytes sent",              "NumberValue"));
//
//		addRow(getCounterSampleAbs(),  "Total bytes sent - KB",         cmRaStat.getAbsString ("Total bytes sent - KB",         "NumberValue"));
//		addRow(getCounterSampleDiff(), "Total bytes sent - KB",         cmRaStat.getDiffString("Total bytes sent - KB",         "NumberValue"));
//		addRow(getCounterSampleRate(), "Total bytes sent - KB",         cmRaStat.getRateString("Total bytes sent - KB",         "NumberValue"));
//                                                                                                                                
//		addRow(getCounterSampleAbs(),  "Total bytes sent - MB",         cmRaStat.getAbsString ("Total bytes sent - MB",         "NumberValue"));
//		addRow(getCounterSampleDiff(), "Total bytes sent - MB",         cmRaStat.getDiffString("Total bytes sent - MB",         "NumberValue"));
//		addRow(getCounterSampleRate(), "Total bytes sent - MB",         cmRaStat.getRateString("Total bytes sent - MB",         "NumberValue"));

//		ISummaryPanel sumPanel = CounterController.getInstance().getSummaryPanel(); 
		ISummaryPanel sumPanel = getCounterController().getSummaryPanel(); 
		if (sumPanel != null)
		{
//			System.out.println("summary: doPostRefresh(): has TabPanel()");
			sumPanel.setSummaryData(this, true);
		}
	}

//	private void addRow(CounterSample cs, String key, String value)
//	{
//		List<Object> row = new ArrayList<Object>(2);
//		row.add(key);
//		row.add(value);
//		cs.addRow(row);
//	}

// Column layout... key/value based or pivot table style...	
// Column layout... (key, value):
//	private static DbxTuneResultSetMetaData _xrstm = new DbxTuneResultSetMetaData();
//	static
//	{
//		_xrstm.addStrColumn       ("key",      1,  false, 255,   "FIXME: description");
//		_xrstm.addStrColumn       ("strval",   2,  false, 100,   "FIXME: description");
//		_xrstm.addBigDecimalColumn("numval",   3,  true,  20, 2, "FIXME: description");
//
//		_xrstm.setPkCol("key");
//	}
// OR: Column layout... (one row, one column for each value)
//	private static DbxTuneResultSetMetaData _xrstm = new DbxTuneResultSetMetaData();
//	static
//	{
//		_xrstm.addStrColumn       ("c1",   1,  false, 255,   "FIXME: description");
//		_xrstm.addStrColumn       ("c2",   2,  false, 100,   "FIXME: description");
//		_xrstm.addBigDecimalColumn("c3",   3,  true,  20, 2, "FIXME: description");
//		_xrstm.addBigDecimalColumn("c4",   3,  true,  20, 2, "FIXME: description");
//		_xrstm.addBigDecimalColumn("c5",   3,  true,  20, 2, "FIXME: description");
//		_xrstm.addBigDecimalColumn("c6",   3,  true,  20, 2, "FIXME: description");
//		_xrstm.addBigDecimalColumn("c7",   3,  true,  20, 2, "FIXME: description");
//
//		_xrstm.setPkCol("key");
//	}
	
//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//	}
// The below is "stolen" from CmRaStatistics and needs to be modified 
//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		int  State_pos  = -1;
//		int  Action_pos = -1;
//
//		// Set the "new" column layout
//		newSample.setColumnNames (_xrstm.getColumnNames());
//		newSample.setSqlType     (_xrstm.getSqlTypes());
//		newSample.setSqlTypeNames(_xrstm.getSqlTypeNames());
//		newSample.setColClassName(_xrstm.getClassNames());
//
//		// Resize the array of PK, because we will have added 2 columns
//		newSample.setPkColArray(_xrstm.getPkColArray());
////		newSample.initPkStructures();
//
////		if ( ! hasResultSetMetaData() )
//			setResultSetMetaData(_xrstm);
//
//		// Find column Id's
//		List<String> colNames = newSample.getColNames();
//		if (colNames == null)
//			return;
//		for (int colId = 0; colId < colNames.size(); colId++)
//		{
//			String colName = (String) colNames.get(colId);
//			if      (colName.equals("State"))  State_pos  = colId;
//			else if (colName.equals("Action")) Action_pos = colId;
//		}
//
//		if (State_pos == -1 || Action_pos == -1)
//			return;
//
//		final BigDecimal KB = new BigDecimal(1024);
//		final BigDecimal MB = new BigDecimal(1024 * 1024);
//        
//		// Loop on all newSample rows, parse some of them, then add new records for the one parsed
//		int rowc = newSample.getRowCount();
//		for (int rowId = 0; rowId < rowc; rowId++)
//		{
//			String component  = (String) newSample.getValueAt(rowId, Component_pos);
//			String statistics = (String) newSample.getValueAt(rowId, Statistic_pos);
//			String value      = (String) newSample.getValueAt(rowId, Value_pos);
//
////			Long numberValue = null;
////			try	{ numberValue = Long.valueOf(value); }
////			catch(NumberFormatException nfe) {System.out.println("problems converting row="+rowId+", value='"+value+"'.");}
//
//			// Try to convert to numbers
//			BigDecimal numberValue = null;
//			try	{ numberValue = new BigDecimal(value); }
//			catch(NumberFormatException nfe) { /*ignore*/ }
////			catch(NumberFormatException nfe) { System.out.println("problems converting row="+rowId+", value='"+value+"'."); }
//
//			String desc = RaxCounterDict.getDesc(statistics);
//
//			newSample.setValueAt(numberValue, rowId, 3);
//			newSample.setValueAt(desc,        rowId, 4);
//			
//			// Transform java memory into MB and add a new entry for it
//			try
//			{
//			}
//			catch(Exception ex)
//			{
//			}
//		}
//	}
	
}
