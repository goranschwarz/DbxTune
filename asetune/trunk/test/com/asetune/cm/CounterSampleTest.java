package com.asetune.cm;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.asetune.CounterControllerAbstract;
import com.asetune.ICounterController;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;

public class CounterSampleTest
{
	private static DbxConnection _conn;
	private static CountersModel _cm;

	@BeforeClass
	public static void setupDb() throws Exception
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

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
				"Test", // name, 
				null,     // groupName, 
				"select * from t1", 
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
	
	}

	@AfterClass
	public static void teardownDb() throws Exception
	{
		_conn.close();
	}

	@Test
	public void test() throws Exception
	{
		CounterSample cs1 = new CounterSample("test", true, new String[]{"c1", "c2", "c3"}, null);
		CounterSample cs2 = new CounterSample("test", true, new String[]{"c1", "c2", "c3"}, null);

		cs1.getSample(_cm, _conn, "select * from t1", _cm.getPk());
		cs2.getSample(_cm, _conn, "select * from t1", _cm.getPk());
		
		System.out.println("s1=" + cs1.debugToString() );
		
		
//		fail("Not yet implemented");
	}

}
