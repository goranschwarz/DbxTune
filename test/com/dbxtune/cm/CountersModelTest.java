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
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dbxtune.CounterControllerAbstract;
import com.dbxtune.ICounterController;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;

public class CountersModelTest
{
	private static DbxConnection _conn;
	private static CountersModel _cm;

	@BeforeClass
	public static void setupDb() throws Exception
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		//		_conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""));
		_conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:test", "sa", ""));

		_conn.createStatement().executeUpdate("CREATE TABLE T1 (ID varchar(30), C1 int, C2 int, C3 int, C4 varchar(30))");
		_conn.createStatement().executeUpdate("insert into T1 values('row1', 1, 10, 100, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row2', 2, 20, 200, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row3', 3, 30, 300, 'first dummy record')");
		_conn.createStatement().executeUpdate("insert into T1 values('row4', 4, 40, 400, 'first dummy record')");
//		_conn.createStatement().executeUpdate("insert into T1 values('row4', 4, 40, 400, 'first dummy record')");
		//_conn.close();

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
		
		String[] diffCols = new String[]{"C1", "C2", "C3"};
		
		_cm = new CountersModel(
				cc,     //counterController, 
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
				0);       // defaultPostponeTime)
	
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
		assertEquals("getColumnCount", 5, _cm.getColumnCount());
		assertEquals("getRowCount"   , 4, _cm.getRowCount());
		assertEquals("getAbsValueSum" , Double.valueOf(10d), _cm.getAbsValueSum("C1"));
		assertEquals("getDiffValueSum", Double.valueOf(0d), _cm.getDiffValueSum("C1"));

		_conn.createStatement().executeUpdate("update T1 set    C1 = C1 + 1,    C2 = C2 + 2,    C3 = C3 + 3    where ID = 'row1'");
		
		_cm.refresh(_conn);
		
		assertEquals("getRowCount", 4, _cm.getRowCount());
		assertEquals("getDiffValueSum", Double.valueOf(1d), _cm.getDiffValueSum("C1"));

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
