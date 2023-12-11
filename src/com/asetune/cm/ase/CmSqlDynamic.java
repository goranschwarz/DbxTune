/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.ase;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.NoValidRowsInSample;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.pcs.sqlcapture.SqlCaptureBrokerAse;
import com.asetune.pcs.sqlcapture.SqlCaptureSqlTextStatisticsSample;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSqlDynamic
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSqlDynamic.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSqlDynamic.class.getSimpleName();
//	public static final String   SHORT_NAME       = "SQL Batch";
	public static final String   SHORT_NAME       = "Dynamic SQL";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Dynamic SQL Activity<br>" +
		"What DynamicSQL Types are executed.<br>" +
		"<br>" +
		"Gets records from monSysSQLText (from the AseTune - SQL Capture Broker subsystem)<br>" + 
		"Records SQL Text like 'create proc dyn###', and 'DYNAMIC_SQL '... and count number of occurrences.<br>" + 
		"<br>" +
		"Reqirements for this to work" +
		"<ul>" +
		"  <li>You need to record the session</li>" +
		"  <li>Option 'Do SQL Capture and Store' for 'SQL Text' needs to be enabled.<br>Which is properties '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' and '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlText+"'.</li>" +
		"  <li>ASE Configuration 'sql text pipe active' and 'sql text pipe max messages', needs to be enabled.</li>" +
		"</ul>" +
		"Note: If ASE config 'sql text pipe max messages' is set to low, we might <i>miss</i> entries in the queue/event-pipe<br>" +
		"<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmSqlDynamic"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"sql text pipe active"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "diffCount"
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.OFF; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSqlDynamic(counterController, guiController);
	}

	public CmSqlDynamic(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, 
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
//		addDependsOnCm(CmXxx.CM_NAME); // CMspinlockSum must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_DYNAMIC_SQL_SEC            = "DynamicSql";
	
	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_DYNAMIC_SQL_SEC,
			"Dynamic SQL Operations per Sec", // Menu CheckBox text
			"Dynamic SQL Operations per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.AUTO, -1),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_DYNAMIC_SQL_SEC.equals(tgdp.getName()))
		{
			// Start With: 1 "line" for every row
			// but some are SKIPPED, so we need to make the array SHORTER at the end
			Double[] tmp_dArray = new Double[this.size()];
			String[] tmp_lArray = new String[tmp_dArray.length];
			
			int ai = 0; // Array Index
			for (int r=0; r<tmp_dArray.length; r++)
			{
				String operation = this.getRateString(r, "operation");

				// SKIP "unwanted" operations (which may "pollute" the graph)
				if (SqlCaptureSqlTextStatisticsSample.OPERATION_CR_OP.equals(operation))
					continue;

				String name = this.getRateString(r, "name");
				String label = name + " - " + operation;

				tmp_lArray[ai] = label;
				tmp_dArray[ai] = this.getRateValueAsDouble(r, "diffCount");
				
				// Increment ArrayIndex
				ai++;
			}

			// If we have NO records to report on... get out of here
			if (ai == 0)
				return;

			// Create new ARRAYS and copy 
			Double[] dArray = new Double[ai];
			String[] lArray = new String[ai];
			
			// Copy the "used parts" of tmp_xArray into xArray  
			System.arraycopy(tmp_dArray, 0, dArray, 0, ai);
			System.arraycopy(tmp_lArray, 0, lArray, 0, ai);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new ACopyMePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("CmSqlDynamic",  "xxx.");

			mtd.addColumn("CmSqlDynamic", "name",                 "<html><i>name</i> of the Dynamic SQL Operator."
			                                                           + "This is a bit Vendor Specifics...<br>"
			                                                           + "<ul>"
			                                                           + "  <li>dyn - jConnect: Java Prepared Statement, if you have DYNAMIC_PREPARE set to TRUE.</li>"
			                                                           + "  <li>jtd - jTDS: Java Prepared Statement.</li>"
			                                                           + "  <li>OPL - OpenLink ODBC: Prepared Statement.</li>"
			                                                           + "  <li>FIXME - CT-Lib ct_dynamic calls (not sure this has a name, it uses the <i>custom name</i> set in ct_dynamic call as far as I know).</li>"
			                                                           + "  <li>NOTE: I will add more 'names' to this list, as I discover them!</li>"
			                                                           + "</ul>"
			                                                           + "</html>");
			mtd.addColumn("CmSqlDynamic", "xOverCr",              "<html>Only valid for operation 'X', which is number of 'X' operations per 'CR'.<br>"
			                                                          + "The higher value the better<br>"
			                                                          + "If it's 1, it typically means that we are <b>not closing</b> the <i>prepared statement</i> after using it.<br>"
			                                                          + "If it's 3, it typically means that we are <b>not resuing</b> the <i>prepared statement</i>: We only do: create, execute, close. But we could be doing: create, execute <b>many times</b>, close<br>"
			                                                          + "So everything above 3 is probably good... it means that we are re-using (multiple executions on) the compiled server object (LWP).<br>"
			                                                          + "<br>"
			                                                          + "<b>NOTE</b>: This can be skewed due to the fact that entries in the 'sql text pipe max messages' <i>ring buffer</i> is to small...<br>"
			                                                          + "</html>");
			mtd.addColumn("CmSqlDynamic", "operation",            "<html>Type of operation. "
					+ "<ul>"
					+ "  <li><b>CR</b>    create proc               -- Simply says that it was a create</li>"
					+ "  <li><b>CR-OP</b> create proc ... OPERATION -- What type of operation is this</li>"
					+ "  <li><b>X</b>     DYNAMIC_SQL               -- Could be a 'Execution' or 'parameter-send' or 'close'... since no real description exists on this.</li>"
					+ "</ul>"
					+ "</html>");

			mtd.addColumn("CmSqlDynamic", "totalCount",           "<html>Number of enties found for the 'name'.</html>");
			mtd.addColumn("CmSqlDynamic", "diffCount",            "<html>Number of enties found for the 'name'.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnConfig(DbxConnection conn)
	{
		boolean ok =  super.checkDependsOnConfig(conn);
		if (ok)
		{
			if ( ! PersistentCounterHandler.hasInstance() )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. No recording is active, which this CM depends on.");
				setActive(false, "No recording is active, which this CM depends on.");
				return false;
			}
			
			Configuration conf = PersistentCounterHandler.getInstance().getConfig();

			boolean sqlCap_doSqlCaptureAndStore = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore, PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore);
			boolean sqlCap_doSqlTextInfo        = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText,            PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);

			if ( ! sqlCap_doSqlCaptureAndStore )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' is NOT enabled");
				setActive(false, "Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' is NOT enabled, which this CM depends on.");
				return false;
			}

			if ( ! sqlCap_doSqlTextInfo )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlText+"' is NOT enabled");
				setActive(false, "Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlText+"' is NOT enabled, which this CM depends on.");
				return false;
			}
		}
		return ok;
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "-- grabed from AseTune internally: PersistentCounterHandler.getInstance().getSqlCaptureBroker().getSqlTextStats(false)";
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("name");
		pkCols.add("operation");

		return pkCols;
	}

	@Override
	public void close()
	{
		super.close();

		// reset/close the SqlCapture Statistics
		// OR: maybe this should be done when the PersistentCounterHandler is stopping/closing
		if ( PersistentCounterHandler.hasInstance() )
		{
			ISqlCaptureBroker sqlCapBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
			if (sqlCapBroker != null && sqlCapBroker instanceof SqlCaptureBrokerAse)
			{
				SqlCaptureBrokerAse aseSqlCapBroker = (SqlCaptureBrokerAse)sqlCapBroker;
				aseSqlCapBroker.closeSqlTextStats();
			}
		}
	}

	
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSamplePrivate(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}
	
	private static class CounterSamplePrivate
	extends CounterSample
	{
		private static final long serialVersionUID = 1L;

		public CounterSamplePrivate(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
		{
			super(name, negativeDiffCountersToZero, diffColumns, prevSample);
		}
		
		@Override
		public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) throws SQLException, NoValidRowsInSample
		{
			if ( ! PersistentCounterHandler.hasInstance() )
				throw new SQLException("unable to retrive 'SQL Text Statistics Object'. No PersistentCounterHandler is available.");

			SqlCaptureSqlTextStatisticsSample sqlCapStat = null;
			
			PersistentCounterHandler pch = PersistentCounterHandler.getInstance();
			ISqlCaptureBroker sqlCapBroker = pch.getSqlCaptureBroker();
			if (sqlCapBroker != null && sqlCapBroker instanceof SqlCaptureBrokerAse)
			{
				SqlCaptureBrokerAse aseSqlCapBroker = (SqlCaptureBrokerAse)sqlCapBroker;
				sqlCapStat = aseSqlCapBroker.getSqlTextStats(false);
			}
			if (sqlCapStat == null)
				throw new SQLException("unable to retrive 'SQL Text Statistics Object'.");

			// update/set the current refresh time and interval
			updateSampleTime(conn, cm);

			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

			
			ResultSet rs = sqlCapStat.toResultSet();

			int rsNum = 0;
			
			ResultSetMetaData originRsmd = rs.getMetaData();
			if ( ! cm.hasResultSetMetaData() )
				cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

			// The above "remapps" some things...
			//  - Like in Oracle 'NUMBER(0,-127)' is mapped to INTEGER
			// So we should use this when calling readResultset()...
			ResultSetMetaData translatedRsmd = cm.getResultSetMetaData();


			if (readResultset(cm, rs, translatedRsmd, originRsmd, pkList, rsNum))
				rs.close();

			return true;
		}
	};
}
