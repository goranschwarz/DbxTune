package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminStatsBacklog
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminStatsBacklog.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminStatsBacklog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Stats Backlog";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Server Stable Queueu Backlog Information</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"admin_stats_backlog"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"Obs",
		"Last"
//		"Max",
//		"Avg ttl/obs"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmAdminStatsBacklog(counterController, guiController);
	}

	public CmAdminStatsBacklog(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_QUEUE_SIZE = "QueueSize";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_QUEUE_SIZE,       new TrendGraphDataPoint(GRAPH_NAME_QUEUE_SIZE,       labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_QUEUE_SIZE,
				"Backlog Size from 'admin statistics, backlog' in MB (Absolute Value)", // Menu CheckBox text
				"Backlog Size from 'admin statistics, backlog' in MB (Absolute Value)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
		{
			int size = 0;
			for (int i = 0; i < this.size(); i++)
			{
				String monitorVal = this.getAbsString(i, "Monitor");
				if (monitorVal.indexOf("SQMRBacklogSeg") >= 0)
					size++;
			}
			
			// Write 1 "line" for every SQMRBacklogSeg row
			Double[] dArray = new Double[size];
			String[] lArray = new String[dArray.length];
			int i2 = 0;
			for (int i = 0; i < dArray.length; i++)
			{
				String monitorVal = this.getAbsString(i, "Monitor");
				if (monitorVal.indexOf("SQMRBacklogSeg") >= 0)
				{
    				lArray[i2] = this.getAbsString       (i, "Instance");
    				dArray[i2] = this.getAbsValueAsDouble(i, "Last");
    				i2++;
				}
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}
	

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();
		
		msgHandler.addDiscardMsgNum(0);

		return msgHandler;
	}

	
//	1> admin statistics, backlog
//	Report Time:		01/29/15 05:41:28 PM
//	RS> Col# Label       JDBC Type Name         Guessed DBMS type
//	RS> ---- ----------- ---------------------- -----------------
//	RS> 1    Instance    java.sql.Types.CHAR    char(255)        
//	RS> 2    Monitor     java.sql.Types.CHAR    char(31)         
//	RS> 3    Obs         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	|Instance                                       |Monitor        |Obs|Last|Max|Avg ttl/obs|
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	|SQMR, 101:0 GORAN_1_ERSSD.GORAN_1_ERSSD, 0, DSI|*SQMRBacklogSeg|0  |0   |0  |0          |
//	|SQMR, 102:0 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 102:1 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 103:0 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 103:1 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 104:0 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 104:1 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 105:0 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 105:1 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	Rows 9
//	(9 rows affected)
//	===============================================================================
//	Report Time:		01/29/15 05:41:28 PM
//	RS> Col# Label       JDBC Type Name         Guessed DBMS type
//	RS> ---- ----------- ---------------------- -----------------
//	RS> 1    Instance    java.sql.Types.CHAR    char(255)        
//	RS> 2    Monitor     java.sql.Types.CHAR    char(31)         
//	RS> 3    Obs         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	|Instance                                       |Monitor          |Obs|Last|Max|Avg ttl/obs|
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	|SQMR, 101:0 GORAN_1_ERSSD.GORAN_1_ERSSD, 0, DSI|*SQMRBacklogBlock|0  |0   |0  |0          |
//	|SQMR, 102:0 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 102:1 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 103:0 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 103:1 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 104:0 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 104:1 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 105:0 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 105:1 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	Rows 9
//	(9 rows affected)
//	===============================================================================


//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("admin_stats_backlog",  "");

			mtd.addColumn("admin_stats_backlog", "Instance",    "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Monitor",     "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Obs",         "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Last",        "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Max",         "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Avg ttl/obs", "<html>FIXME</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");
		pkCols.add("Monitor");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin statistics, backlog";
		return sql;
	}
}
