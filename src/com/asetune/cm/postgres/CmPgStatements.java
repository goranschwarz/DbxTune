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
package com.asetune.cm.postgres;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_statements"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"calls",
			"total_time",
			"rows",
			"shared_blks_hit",
			"shared_blks_read",
			"shared_blks_dirtied",
			"shared_blks_written",
			"local_blks_hit",
			"local_blks_read",
			"local_blks_dirtied",
			"local_blks_written",
			"temp_blks_read",
			"temp_blks_written",
			"blk_read_time",
			"blk_write_time"
	};
//	RS> Col# Label               JDBC Type Name         Guessed DBMS type
//	RS> ---- ------------------- ---------------------- -----------------
//	RS> 1    userid              java.sql.Types.BIGINT  oid              
//	RS> 2    dbid                java.sql.Types.BIGINT  oid              
//	RS> 3    queryid             java.sql.Types.BIGINT  int8                //// NOTE introduced in 9.4
//	RS> 4    query               java.sql.Types.VARCHAR text(2147483647) 
//	RS> 5    calls               java.sql.Types.BIGINT  int8             
//	RS> 6    total_time          java.sql.Types.DOUBLE  float8           
//	RS> 7    rows                java.sql.Types.BIGINT  int8             
//	RS> 8    shared_blks_hit     java.sql.Types.BIGINT  int8             
//	RS> 9    shared_blks_read    java.sql.Types.BIGINT  int8             
//	RS> 10   shared_blks_dirtied java.sql.Types.BIGINT  int8             
//	RS> 11   shared_blks_written java.sql.Types.BIGINT  int8             
//	RS> 12   local_blks_hit      java.sql.Types.BIGINT  int8             
//	RS> 13   local_blks_read     java.sql.Types.BIGINT  int8             
//	RS> 14   local_blks_dirtied  java.sql.Types.BIGINT  int8             
//	RS> 15   local_blks_written  java.sql.Types.BIGINT  int8             
//	RS> 16   temp_blks_read      java.sql.Types.BIGINT  int8             
//	RS> 17   temp_blks_written   java.sql.Types.BIGINT  int8             
//	RS> 18   blk_read_time       java.sql.Types.DOUBLE  float8           
//	RS> 19   blk_write_time      java.sql.Types.DOUBLE  float8           

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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgStatements(counterController, guiController);
	}

	public CmPgStatements(ICounterController counterController, IGuiController guiController)
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
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_SQL_STATEMENTS = "SqlStatements";
	
	public static final String  PROPKEY_createExtension = "CmPgStatements.if.missing.pg_stat_statements.createExtension";
	public static final boolean DEFAULT_createExtension = true; 

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SQL_STATEMENTS,
			"SQL Statement Calls", 	                           // Menu CheckBox text
			"SQL Statement Calls per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] {"calls"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_SQL_STATEMENTS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueSum("calls");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	
	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("userid");
		pkCols.add("dbid");
		pkCols.add("queryid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		// 'queryid' was introduced in 9.4 so lets simulate a query id in earlier versions
		String queryid = "";
		if (srvVersion < Ver.ver(9,4))
		{
//			queryid = "    ,md5(query) as queryid \n";
			queryid = "    ,('x'||substr(md5(query),1,16))::bit(64)::bigint as queryid \n";
		}
			
		// TODO: calculate the per_xxx for diff values... localCalculation()...
		return  "select \n" +
				"     CASE WHEN s.calls > 0 THEN s.total_time      / s.calls ELSE 0 END as avg_time_per_call \n" +
				"    ,CASE WHEN s.calls > 0 THEN s.rows            / s.calls ELSE 0 END as avg_rows_per_call \n" +
				"    ,CASE WHEN s.rows  > 0 THEN s.shared_blks_hit / s.rows  ELSE 0 END as shared_blks_hit_per_row \n" +
				"    ,d.datname \n" +
				"    ,u.usename \n" +
				queryid +
				"    ,s.* \n" +
				"    ,cast(0 as integer) AS \"dupMergeCount\" \n" +
				"from pg_stat_statements s \n" +
				"left outer join pg_catalog.pg_database d ON s.dbid   = d.oid      \n" +
				"left outer join pg_catalog.pg_user     u ON s.userid = u.usesysid \n" +
				"where s.calls > 1 \n" +
				"";
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// query
		if ("query".equals(colName))
		{
			return cellValue == null ? null : toHtmlString(cellValue.toString());
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}


	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since SIX is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	@Override
	protected boolean beforeRefreshGetData(DbxConnection conn) throws Exception
	{
		String sql = "SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'pg_stat_statements' AND c.relkind = 'v' ";
		
		// Check if 'pg_stat_statements' exists... 
		// and possibly create it...
		boolean exists = false;
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			while(rs.next())
				exists = true;
		}
		
		if ( ! exists )
		{
			_logger.warn("When checking Counters Model '"+getName()+"', named '"+getDisplayName()+"' The table 'pg_stat_statements' do not exists.");

			_logger.warn("Possible solution for 'pg_stat_statements': (1: check that file 'PG_INST_DIR/lib/pg_stat_statements.so' exists, if not install postgres contrib package), (2: check that config 'shared_preload_libraries' contains 'pg_stat_statements'), (3: optional: configure 'pg_stat_statements.max=10000', 'pg_stat_statements.track=all' in the config file and restart postgres), (4: execute: CREATE EXTENSION pg_stat_statements), (5: verify with: SELECT * FROM pg_stat_statements). or look at https://www.postgresql.org/docs/current/static/pgstatstatements.html");

			boolean createExtension = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_createExtension, DEFAULT_createExtension);
			if (createExtension)
			{
				_logger.info("AUTO-FIX: The table 'pg_stat_statements' did not exists... Trying to create it, this will simply work if you are authorized. to DISABLE the 'CREATE EXTENSION pg_stat_statements' set config '" + PROPKEY_createExtension + "=false'.");

				try (Statement stmnt = conn.createStatement())
				{
					_logger.info("AUTO-FIX: Executing: CREATE EXTENSION pg_stat_statements");

					stmnt.executeUpdate("CREATE EXTENSION pg_stat_statements");

					_logger.info("AUTO-FIX: Success executing: CREATE EXTENSION pg_stat_statements");

					// Check if the table is accessible after 'CREATE EXTENSION pg_stat_statements'
					try( Statement stmnt2 = conn.createStatement(); ResultSet rs = stmnt.executeQuery("SELECT * FROM pg_stat_statements where dbid = -999"); )
					{
						while(rs.next())
							; // just loop the RS, no rows will be found...
						exists = true;
					}
					catch (SQLException exSelChk)
					{
						exists = false;
						_logger.warn("AUTO-FIX: FAILED when checking table 'pg_stat_statements' after 'CREATE EXTENSION pg_stat_statements'. Caught: " + exSelChk);
						
						// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
						//System.setProperty(PROPKEY_createExtension, "false"); 
					}
				}
				catch(SQLException exCrExt)
				{
					exists = false;
					_logger.warn("AUTO-FIX: FAILED when executing 'CREATE EXTENSION pg_stat_statements'. Caught: " + exCrExt);

					// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
					//System.setProperty(PROPKEY_createExtension, "false"); 
				}
			}
		}
		
		return exists;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		boolean isOk = false;
		String  message = "";
		
		// Check if the table exists, since it's optional and needs to be installed
		try
		{
			isOk = beforeRefreshGetData(conn);
		}
		catch (Exception ex)
		{
			isOk = false;
			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' The table 'pg_stat_statements' do not exists. This is an optional component. Caught: "+ex);
			message = ex.getMessage();
		}

		if (isOk)
		{
			_logger.info("Check dependencies SUCCESS: Table 'pg_stat_statements' exists when checking Counters Model '"+getName()+"'.");
			return true;
		}
		
		setActive(false, 
				"The table 'pg_stat_statements' do not exists.\n"
				+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html\n"
				+ "\n"
				+ "Or possibly issue: CREATE EXTENSION pg_stat_statements \n"
				+ "\n"
				+ message);

		TabularCntrPanel tcp = getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>"
					+ "The table 'pg_stat_statements' do not exists.<br>"
					+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html<br>"
					+ "<br>"
					+ "Or possibly issue: CREATE EXTENSION pg_stat_statements <br>"
					+ "<br>"
					+ message+"</html>");
		}
		
		return false;
	}	

//	@Override
//	public boolean checkDependsOnOther(Connection conn)
//	{
//		// Check if the table exists, since it's optional and needs to be installed
//		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("SELECT * FROM pg_stat_statements where dbid = -999"); )
//		{
//			while(rs.next())
//				;
//			return true; // <<<<<<<---------- RETURN
//		}
//		catch (SQLException ex)
//		{
//			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' The table 'pg_stat_statements' do not exists. This is an optional component. Caught: "+ex);
//
//			// If the table do NOT exist... try to create it!
//			if ((ex+"").contains("relation \"pg_stat_statements\" does not exist"))
//			{
//				_logger.warn("Possible solution for 'pg_stat_statements' errors: (1: check that file 'PG_INST_DIR/lib/pg_stat_statements.so' exists, if not install postgres contrib package), (2: check that config 'shared_preload_libraries' contains 'pg_stat_statements'), (3: optional: configure 'pg_stat_statements.max=10000', 'pg_stat_statements.track=all' in the config file and restart postgres), (4: execute: CREATE EXTENSION pg_stat_statements), (5: verify with: SELECT * FROM pg_stat_statements). or look at https://www.postgresql.org/docs/current/static/pgstatstatements.html");
//
//				boolean createExtension = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_createExtension, DEFAULT_createExtension);
//				if (createExtension)
//				{
//					_logger.info("The table 'pg_stat_statements' did not exists... Trying to create it, this will simply work if you got enough authorization. to DISABLE the set config '" + PROPKEY_createExtension + "=false'.");
//					try (Statement stmnt = conn.createStatement())
//					{
//						_logger.info("Executing: CREATE EXTENSION pg_stat_statements");
//						stmnt.executeUpdate("CREATE EXTENSION pg_stat_statements");
//
//						_logger.info("Success when executing: CREATE EXTENSION pg_stat_statements");
//
//						// Check if the table exists, since it's optional and needs to be installed
//						try( Statement stmnt2 = conn.createStatement(); ResultSet rs2 = stmnt.executeQuery("SELECT * FROM pg_stat_statements where dbid = -999"); )
//						{
//							while(rs2.next())
//								;
//							return true; // <<<<<<<---------- RETURN
//						}
//						catch (SQLException ex3)
//						{
//							_logger.warn("FAILED when checking table 'pg_stat_statements' after 'CREATE EXTENSION pg_stat_statements'. Caught: "+ex);
//						}
//					}
//					catch(SQLException ex2)
//					{
//						_logger.warn("FAILED when executing 'CREATE EXTENSION pg_stat_statements'. Caught: " + ex2);
//					}
//				}
//			}
//
//			setActive(false, 
//					"The table 'pg_stat_statements' do not exists.\n"
//					+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html\n"
//					+ "\n"
//					+ "Or possibly issue: CREATE EXTENSION pg_stat_statements \n"
//					+ "\n"
//					+ ex.getMessage());
//
//			TabularCntrPanel tcp = getTabPanel();
//			if (tcp != null)
//			{
//				tcp.setUseFocusableTips(true);
//				tcp.setToolTipText("<html>"
//						+ "The table 'pg_stat_statements' do not exists.<br>"
//						+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html<br>"
//						+ "<br>"
//						+ "Or possibly issue: CREATE EXTENSION pg_stat_statements <br>"
//						+ "<br>"
//						+ ex.getMessage()+"</html>");
//			}
//
//			return false; // <<<<<<<---------- RETURN
//		}
//	}	
}
