package com.asetune.cm.rs;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.rs.AlarmEventRsMemoryUsage;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRsMemory
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmRsMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRsMemory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Server Memory Usage in each <i>module</i></p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"mem_detail_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Memory_Consumed"
//		,"Memory_Consumed_Mb"
		};
	
	/*
	1> admin stats, mem_detail_stats
	RS> Col# Label                  JDBC Type Name         Guessed DBMS type Source Table
	RS> ---- ---------------------- ---------------------- ----------------- ------------
	RS> 1    Object/State           java.sql.Types.VARCHAR varchar(14)       -none-      
	RS> 2    Memory_Consumed        java.sql.Types.VARCHAR varchar(19)       -none-      
	RS> 3    Memory_Consumed_Mb     java.sql.Types.VARCHAR varchar(19)       -none-      
	RS> 4    Max_Memory_Consumed_Mb java.sql.Types.VARCHAR varchar(24)       -none-      
	+-------------+---------------+------------------+----------------------+
	|Object/State |Memory_Consumed|Memory_Consumed_Mb|Max_Memory_Consumed_Mb|
	+-------------+---------------+------------------+----------------------+
	|Miscellaneous|45235505       |43                |203                   |
	|SQM Metadata |29512448       |28                |43                    |
	|SQM Cache    |92281552       |88                |136                   |
	|CMD Cache    |0              |0                 |0                     |
	|SQT Cache    |0              |0                 |103                   |
	|CMD/SQT      |0              |0                 |0                     |
	|CI           |0              |0                 |0                     |
	|CMD/EXEC     |256            |0                 |0                     |
	|CMD/DIST     |0              |0                 |0                     |
	|CMD/DSIS     |0              |0                 |0                     |
	|CMD/DSIE     |0              |0                 |0                     |
	|Total        |167029761      |159               |320                   |
	+-------------+---------------+------------------+----------------------+
	 */
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmRsMemory(counterController, guiController);
	}

	public CmRsMemory(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_MEMORY_PCT      = "MemoryPct";
	public static final String GRAPH_NAME_MODULE_USAGE    = "ModuleUsage";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_PCT,
			"Stat: RS Total Memory Usage in Percent", // Menu CheckBox text
			"Stat: RS Total Memory Usage in Percent", // Label 
			new String[] {"Total RS Memory Usage"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			true, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MODULE_USAGE,
			"Stat: Memory Usage in MB per Module", // Menu CheckBox text
			"Stat: Memory Usage in MB per Module", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_MEMORY_PCT.equals(tgdp.getName()))
		{
			if (_memoryLimitSizeMb != -1)
			{
				Double[] dArray = new Double[1];
				
				Double totalMemUsed = this.getAbsValueAsDouble("Total", "Memory_Consumed_Mb");
				//Double memoryPctUsed = totalMemUsed / _memoryLimitSizeMb * 100.0;
				BigDecimal memoryPctUsed = new BigDecimal(totalMemUsed / _memoryLimitSizeMb * 100.0).setScale(2, BigDecimal.ROUND_HALF_EVEN);

				dArray[0] = memoryPctUsed.doubleValue();

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), dArray);
			}
		}

		if (GRAPH_NAME_MODULE_USAGE.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "Object/State");
				dArray[i] = this.getAbsValueAsDouble(i, "Memory_Consumed_Mb");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRsMemory(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("mem_detail_stats",  "");

			mtd.addColumn("mem_detail_stats", "Object/State",                  "<html>What module is the for</html>");
			mtd.addColumn("mem_detail_stats", "Memory_Consumed",               "<html>Memory in bytes</html>");
			mtd.addColumn("mem_detail_stats", "Memory_Consumed_Mb",            "<html>Memory in MB</html>");
			mtd.addColumn("mem_detail_stats", "Max_Memory_Consumed_Mb",        "<html>Max Memory every used by this module in MB</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Object/State");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin stats, mem_detail_stats";
		return sql;
	}
	

/*
	1> admin config , memory_limit
	RS> Col# Label         JDBC Type Name      Guessed DBMS type Source Table
	RS> ---- ------------- ------------------- ----------------- ------------
	RS> 1    Configuration java.sql.Types.CHAR char(31)          -none-      
	RS> 2    Config Value  java.sql.Types.CHAR char(255)         -none-      
	RS> 3    Run Value     java.sql.Types.CHAR char(255)         -none-      
	RS> 4    Default Value java.sql.Types.CHAR char(255)         -none-      
	RS> 5    Legal Values  java.sql.Types.CHAR char(255)         -none-      
	RS> 6    Datatype      java.sql.Types.CHAR char(255)         -none-      
	RS> 7    Status        java.sql.Types.CHAR char(255)         -none-      
	+-------------+------------+---------+-------------+----------------------------------------------------------------------------------------+--------+--------------------+
	|Configuration|Config Value|Run Value|Default Value|Legal Values                                                                            |Datatype|Status              |
	+-------------+------------+---------+-------------+----------------------------------------------------------------------------------------+--------+--------------------+
	|memory_limit |2047        |2047     |2047         |range: 0,2047 for 32-bit Replication Server, 0,2147483647 for 64-bit Replication Server.|integer |Restart not required|
	+-------------+------------+---------+-------------+----------------------------------------------------------------------------------------+--------+--------------------+
	(1 rows affected)
*/
	/** 'memory_limit' run value, on init */
	private int _memoryLimitSizeMb = -1;
	
	@Override
	public boolean doSqlInit(DbxConnection conn)
	{
		boolean superRc = super.doSqlInit(conn);

		String sql     = "";
		String cfgName = "";

		//------------------------------------------------
		// Get 'memory_limit' RUN Size on init.
		cfgName = "memory_limit";
		sql = "admin config , memory_limit";
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
			{
				String str = "";
				try 
				{
					str = rs.getString(3);
					_memoryLimitSizeMb = Integer.parseInt(str);
				} 
				catch (NumberFormatException nfe) 
				{
					_logger.error("Problem parsing/converting String Value '"+str+"' to integer for RS configuration '"+cfgName+"'. Caught: "+nfe);
					_memoryLimitSizeMb = -1;
				}
			}

			_logger.info("When initializing '"+getName()+"', Succeed get run value for RS configuration '"+cfgName+"' = "+_memoryLimitSizeMb + " MB");
		}
		catch (SQLException ex)
		{
			_memoryLimitSizeMb = -1;
			_logger.warn("When initializing '"+getName()+"', failed to get RS configuration '"+cfgName+"', continuing anyway. sql=|"+sql+"|, Caught: "+ex);
		}
		
		return superRc;
	}
	@Override
	public void doSqlClose(DbxConnection conn)
	{
		// Reset value
		_memoryLimitSizeMb = -1;
		
		super.doSqlClose(conn);
	}
	




	//--------------------------------------------------------------
	// Alarm handling
	//--------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		//-------------------------------------------------------
		// Memory Usage in PCT
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("MemoryUsedPct"))
		{
			Double totalMemUsed = cm.getAbsValueAsDouble("Total", "Memory_Consumed_Mb");
			
			if (totalMemUsed != null && _memoryLimitSizeMb != -1)
			{
				BigDecimal usedPct = new BigDecimal(totalMemUsed / _memoryLimitSizeMb * 100.0).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				int usedMemInMb = totalMemUsed.intValue();
				int freeMemInMb = _memoryLimitSizeMb - totalMemUsed.intValue();

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): usedMemInMb="+usedMemInMb+", freeMemInMb="+freeMemInMb+", usedPct="+usedPct+".");

				// level-1 (normally 60%)
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_MemoryUsedPct1, DEFAULT_alarm_MemoryUsedPct1);
				if (usedPct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm( new AlarmEventRsMemoryUsage(cm, threshold, 1, usedMemInMb, freeMemInMb, usedPct.doubleValue(), _memoryLimitSizeMb) );
				}

				// level-2 (normally 70%)
				threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_MemoryUsedPct2,  DEFAULT_alarm_MemoryUsedPct2);
				if (usedPct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm( new AlarmEventRsMemoryUsage(cm, threshold, 2, usedMemInMb, freeMemInMb, usedPct.doubleValue(), _memoryLimitSizeMb) );
				}

				// level-3 (normally 80%)
				threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_MemoryUsedPct3,  DEFAULT_alarm_MemoryUsedPct3);
				if (usedPct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm( new AlarmEventRsMemoryUsage(cm, threshold, 3, usedMemInMb, freeMemInMb, usedPct.doubleValue(), _memoryLimitSizeMb) );
				}
				
				// level-4 (normally 80%)
				threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_MemoryUsedPct4,  DEFAULT_alarm_MemoryUsedPct4);
				if (usedPct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm( new AlarmEventRsMemoryUsage(cm, threshold, 4, usedMemInMb, freeMemInMb, usedPct.doubleValue(), _memoryLimitSizeMb) );
				}
			}
		}
	} // end: method

	public static final String  PROPKEY_alarm_MemoryUsedPct1                     = CM_NAME + ".alarm.system.if.MemoryUsedPct.1.gt";
	public static final int     DEFAULT_alarm_MemoryUsedPct1                     = 60;
	
	public static final String  PROPKEY_alarm_MemoryUsedPct2                     = CM_NAME + ".alarm.system.if.MemoryUsedPct.2.gt";
	public static final int     DEFAULT_alarm_MemoryUsedPct2                     = 70;
	
	public static final String  PROPKEY_alarm_MemoryUsedPct3                     = CM_NAME + ".alarm.system.if.MemoryUsedPct.3.gt";
	public static final int     DEFAULT_alarm_MemoryUsedPct3                     = 80;
	
	public static final String  PROPKEY_alarm_MemoryUsedPct4                     = CM_NAME + ".alarm.system.if.MemoryUsedPct.4.gt";
	public static final int     DEFAULT_alarm_MemoryUsedPct4                     = 90;
	
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("MemoryUsedPct", PROPKEY_alarm_MemoryUsedPct1, Integer.class,  conf.getIntProperty(PROPKEY_alarm_MemoryUsedPct1, DEFAULT_alarm_MemoryUsedPct1), DEFAULT_alarm_MemoryUsedPct1, "If 'MemoryUsedPct' is GREATER than ## pct, send 'AlarmEventRsMemoryUsage'."));
		list.add(new CmSettingsHelper("MemoryUsedPct", PROPKEY_alarm_MemoryUsedPct2, Integer.class,  conf.getIntProperty(PROPKEY_alarm_MemoryUsedPct2, DEFAULT_alarm_MemoryUsedPct2), DEFAULT_alarm_MemoryUsedPct2, "If 'MemoryUsedPct' is GREATER than ## pct, send 'AlarmEventRsMemoryUsage'."));
		list.add(new CmSettingsHelper("MemoryUsedPct", PROPKEY_alarm_MemoryUsedPct3, Integer.class,  conf.getIntProperty(PROPKEY_alarm_MemoryUsedPct3, DEFAULT_alarm_MemoryUsedPct3), DEFAULT_alarm_MemoryUsedPct3, "If 'MemoryUsedPct' is GREATER than ## pct, send 'AlarmEventRsMemoryUsage'."));
		list.add(new CmSettingsHelper("MemoryUsedPct", PROPKEY_alarm_MemoryUsedPct4, Integer.class,  conf.getIntProperty(PROPKEY_alarm_MemoryUsedPct4, DEFAULT_alarm_MemoryUsedPct4), DEFAULT_alarm_MemoryUsedPct4, "If 'MemoryUsedPct' is GREATER than ## pct, send 'AlarmEventRsMemoryUsage'."));

		return list;
	}

}
