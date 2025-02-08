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

import static org.junit.Assert.assertEquals;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dbxtune.CounterControllerAbstract;
import com.dbxtune.ICounterController;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.SwingUtils;

public class CountersModelTest_Aggregation
{
	private static DbxConnection _conn;
	private static CountersModel _cm;

	@BeforeClass
	public static void setupDb() throws Exception
	{
//		Configurator.setRootLevel(Level.TRACE);

		//		_conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""));
		_conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:test", "sa", ""));

		_conn.createStatement().executeUpdate("CREATE TABLE T1 (ID varchar(30), C1_SUM int, C2_SUM int, C3_AVG int, C4_MIN int, C5_MAX int, DESCRIPTION varchar(30))");
		_conn.createStatement().executeUpdate("insert into T1 values('row1', 1, 10, 100, 1000, 10000, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row2', 2, 20, 200, 2000, 20000, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row3', 3, 30, 300, 3000, 30000, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row4', 4, 40, 400, 4000, 40000, 'first dummy record')");

		ICounterController cc = new CounterControllerAbstract(false)
		{
			@Override public    String     getServerTimeCmd()             { return null; /*return "select CURRENT_TIMESTAMP()";*/ }
			@Override public    void       checkServerSpecifics()         {}
			@Override public    void       initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion) throws Exception {}
			@Override protected String     getIsClosedSql()               { return "select 1"; }
			@Override public    HeaderInfo createPcsHeaderInfo()          { return null; }
			@Override public    void       createCounters(boolean hasGui) {}
		};
		cc.setMonConnection(_conn);
		
		List<String> pk = new ArrayList<>();
		pk.add("ID");
		
		String[] diffCols = new String[]{"C1_SUM", "C2_SUM", "C3_AVG", "C4_MIN", "C5_MAX"};
		
		_cm = new CountersModel(
				cc,     // counterController, 
				null,   // guiController
				"DummyCmTest", // name, 
				null,     // groupName, 
				"select * from T1", 
				pk,       // pkList, 
				diffCols, // diffColumns, 
				null,     // pctColumns, 
				null,     //monTables, 
				null,     // dependsOnRole, 
				null,     // dependsOnConfig, 
				0,        // dependsOnVersion, 
				0,        // dependsOnCeVersion, 
				true,     // negativeDiffCountersToZero, 
				false,    // systemCm, 
				0)       // defaultPostponeTime)
			{
				private static final long serialVersionUID = 1L;
			
				@Override
				public Map<String, AggregationType> createAggregateColumns()
				{
					HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

					AggregationType tmp;
					
					// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
					tmp = new AggregationType("C1_SUM", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
					tmp = new AggregationType("C2_SUM", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
					tmp = new AggregationType("C3_AVG", AggregationType.Agg.AVG);   aggColumns.put(tmp.getColumnName(), tmp);
					tmp = new AggregationType("C4_MIN", AggregationType.Agg.MIN);   aggColumns.put(tmp.getColumnName(), tmp);
					tmp = new AggregationType("C5_MAX", AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

					return aggColumns;
				}
			};
	
		_cm.init(_conn);
	}

	@AfterClass
	public static void teardownDb() throws Exception
	{
		_conn.close();
	}

	@Test
	public void test() throws Exception
	{
//		CounterSample cs1 = new CounterSample("test", true, new String[]{"c1", "c2", "c3"}, null);
//		CounterSample cs2 = new CounterSample("test", true, new String[]{"c1", "c2", "c3"}, null);
//
//		cs1.getSample(_cm, _conn, "select * from t1", _cm.getPk());
//		cs2.getSample(_cm, _conn, "select * from t1", _cm.getPk());
//		
//		System.out.println("s1=" + cs1.debugToString() );

		_cm.refresh(_conn);
		_cm.refresh(_conn);  // INVESTIGATE WHY WE NEED TO DO A SECONDS REFRESH

System.out.println("_cm.getSql()="+_cm.getSql());
System.out.println("_cm.getColumnCount()="+_cm.getColumnCount());
System.out.println("_cm.getRowCount()="+_cm.getRowCount());
		assertEquals("getColumnCount", 7, _cm.getColumnCount());
		assertEquals("getRowCount"   , 5, _cm.getRowCount());
		assertEquals("getAbsValueSum" , Double.valueOf(10d), _cm.getAbsValueSum("C1_SUM"));
		assertEquals("getDiffValueSum", Double.valueOf(0d), _cm.getDiffValueSum("C1_SUM"));

		_conn.createStatement().executeUpdate(
				"update T1 "
				+ "SET C1_SUM = C1_SUM + 10 "
				+ "   ,C2_SUM = C2_SUM + 20 "
				+ "   ,C3_AVG = C3_AVG + 30 "
				+ "   ,C4_MIN = C4_MIN + 40 "
				+ "   ,C5_MAX = C5_MAX + 50 "
				+ "where ID = 'row1' "
				+ "   OR ID = 'row4' "
				+ "");
		
		_cm.refresh(_conn);
		
		assertEquals("getRowCount", 5, _cm.getRowCount());
//		assertEquals("getDiffValueSum", Double.valueOf(1d), _cm.getDiffValueSum("C1_SUM"));
		
//		System.out.println( ">>>> ABS \n"  + _cm.toTextTableString(CountersModel.DATA_ABS , _cm.getAggregatedRowId()) );
//		System.out.println( ">>>> DIFF \n" + _cm.toTextTableString(CountersModel.DATA_DIFF, _cm.getAggregatedRowId()) );
//		System.out.println( ">>>> RATE \n" + _cm.toTextTableString(CountersModel.DATA_RATE, _cm.getAggregatedRowId()) );

		System.out.println( ">>>> ABS \n"   + SwingUtils.tableToString(_cm.getCounterData(CountersModel.DATA_ABS)) );
		System.out.println( ">>>> DIFF \n"  + SwingUtils.tableToString(_cm.getCounterData(CountersModel.DATA_DIFF)) );
		System.out.println( ">>>> RATE \n"  + SwingUtils.tableToString(_cm.getCounterData(CountersModel.DATA_RATE)) );

//		fail("Not yet implemented");
	}

//	TODO; Check Aggregated columns -- That below works as expected (with and without Aggregate Columns)
//	          - Sum
//	          - Avg
//	          - Min/Max 
//	          - etc...
//	TODO; The above so we can do Aggregated columns for: CmSqlStatements
//	TODO; Implement Mic/Max in Aggregation (and GUI Icons for Column Highlighters)
//	POSSIBLY; Implement: isAggregateRowVisible(), setAggregateRowVisible(boolean) 
}
