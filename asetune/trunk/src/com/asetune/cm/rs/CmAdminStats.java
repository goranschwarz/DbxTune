package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.DbxTuneResultSetMetaData;
import com.asetune.cm.rs.gui.CmAdminStatsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrameRs;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "RS Counters";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>RepServer Monitor And Performance Counters</p>" +
		"Fetched using: <code>admin statistics,'ALL'</code>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrameRs.TCP_GROUP_MC;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		 "Obs"
		,"Total"
		,"Last"
		,"Max"
//		,"AvgTtlObs"
//		,"RateXsec"
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

		return new CmAdminStats(counterController, guiController);
	}

	public CmAdminStats(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_resetAfter          = PROP_PREFIX + ".sample.reset.after";
	public static final boolean DEFAULT_sample_resetAfter          = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter);
	}


	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

//		msgHandler.addDiscardMsgStr("===============================================================================");
		msgHandler.addDiscardMsgNum(0);
		msgHandler.addDiscardMsgNum(15539); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is created.
		msgHandler.addDiscardMsgNum(15540); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is dropped.

		// RS Configuration might be changed in the "sql init" section
		msgHandler.addDiscardMsgNum(15357); // configure replication server...: Config parameter 'stats_sampling' is modified.
		msgHandler.addDiscardMsgNum(56040); // admin stats, cancel:             Failed to cancel, there is no command in progress.

		return msgHandler;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Clear Counters", PROPKEY_sample_resetAfter , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_resetAfter  , DEFAULT_sample_resetAfter  ), DEFAULT_sample_resetAfter, CmAdminStatsPanel.TOOLTIP_sample_resetAfter ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmAdminStatsPanel(this);
	}

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
			mtd.addTable("stats",  "");

			mtd.addColumn("stats", "Instance",       "<html>FIXME: Instance</html>");
			mtd.addColumn("stats", "InstanceId",     "<html>FIXME: InstanceId</html>");
			mtd.addColumn("stats", "ModTypeInstVal", "<html>FIXME: ModTypeInstVal</html>");
			mtd.addColumn("stats", "Type",           "<html>FIXME: Type</html>");
			mtd.addColumn("stats", "Name",           "<html>FIXME: Name</html>");
			mtd.addColumn("stats", "Obs",            "<html>FIXME: Obs</html>");
			mtd.addColumn("stats", "Total",          "<html>FIXME: Total</html>");
			mtd.addColumn("stats", "Last",           "<html>FIXME: Last</html>");
			mtd.addColumn("stats", "Max",            "<html>FIXME: Max</html>");
			mtd.addColumn("stats", "AvgTtlObs",      "<html>FIXME: AvgTtlObs</html>");
			mtd.addColumn("stats", "RateXsec",       "<html>FIXME: RateXsec</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");
		pkCols.add("InstanceId"); // We might need to add this as well... dsi_workload... might return not return unique enough values in 'Instance'
		pkCols.add("Type");
		pkCols.add("Name");

		return pkCols;
		
		//------------------------------------------------------------------------
		// Below is some examples (from web error log) where it looks strange
		//------------------------------------------------------------------------
		// NEW	53491	53491	53491	3	2015-11-18 08:48:44	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.ImageHist|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.ImageHist, 11705, 11, MONITOR, AOBJEstRowSize, 1, null, 204, 204, 204, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.ImageHist, 11605, 11, MONITOR, AOBJEstRowSize, 1, null, 204, 204, 204, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                                   ^^^^^
		// 
		// NEW	53491	53491	53491	4	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.ImageHist|OBSERVER|AOBJInsertCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.ImageHist, 11705, 11, OBSERVER, AOBJInsertCommand2, 1, null, null, null, null, 0, AOBJ, 65005, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Insert command on active object.]'. 
		//     NewRow='[AOBJ, dbo.ImageHist, 11605, 11, OBSERVER, AOBJInsertCommand2, 1, null, null, null, null, 0, AOBJ, 65005, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Insert command on active object.]'.	53491	53491	
		//                                   ^^^^^
		// 
		// NEW	53491	53491	53491	5	2015-11-18 08:48:51	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.Image|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.Image, 11705, 21, MONITOR, AOBJEstRowSize, 1, null, 249, 249, 249, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.Image, 11605, 21, MONITOR, AOBJEstRowSize, 1, null, 249, 249, 249, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                               ^^^^^
		//
		// NEW	53491	53491	53491	6	2015-11-18 08:48:52	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.Image|OBSERVER|AOBJDeleteCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.Image, 11705, 21, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'. 
		//     NewRow='[AOBJ, dbo.Image, 11605, 21, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'.	53491	53491	
		//                               ^^^^^
		// 
		// NEW	53491	53491	53491	7	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.NewImageRef|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.NewImageRef, 11705, 31, MONITOR, AOBJEstRowSize, 1, null, 30, 30, 30, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.NewImageRef, 11605, 31, MONITOR, AOBJEstRowSize, 1, null, 30, 30, 30, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                                     ^^^^^
		// 
		// NEW	53491	53491	53491	8	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.asetune.cm.CounterSample	com.asetune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.NewImageRef|OBSERVER|AOBJDeleteCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.NewImageRef, 11705, 31, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'. 
		//     NewRow='[AOBJ, dbo.NewImageRef, 11605, 31, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'.	53491	53491	
		//                                     ^^^^^

	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		boolean sample_resetAfter = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_resetAfter, DEFAULT_sample_resetAfter);

		String sql = "admin statistics, 'ALL' \n";

		if (sample_resetAfter)
			sql += "admin statistics, 'RESET' \n";

		//sql = "admin statistics, 'sysmon'";

		return sql;
	}
	
	@Override
	public String getSqlInitForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String statsOn = 
			  "--admin stats, cancel -- cancel might crach RS in some cases...\n"
			+ "go \n"
			+ "configure replication server set 'stats_sampling' to 'on' \n"
			+ "go \n"
			+ "configure replication server set 'stats_engineering_counters' to 'on' \n"
			+ "go \n"
			+ "--admin statistics, reset \n"
			+ "go \n";

// NOTE: this should be empty when we pass a proper srvVersion number
String AOBJ = "trace 'on', 'dsi', 'dsi_workload' \n" + "go \n";
		if (srvVersion >= Ver.ver(15, 6))
			AOBJ = "trace 'on', 'dsi', 'dsi_workload' \n"
			     + "go \n";
		
		return statsOn + AOBJ;
	}

//	@Override
//	public String getSqlCloseForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
//	{
//		String statsOff = 
//			  "--admin stats, cancel -- cancel might crach RS in some cases...\n"
//			+ "go \n"
//			+ "--configure replication server set 'stats_sampling' to 'off' \n"
//			+ "--go \n"
//			+ "--configure replication server set 'stats_engineering_counters' to 'off' \n"
//			+ "--go \n";
//
//		// NOTE: this should be empty when we pass a proper srvVersion number
//		String AOBJ = "--trace 'off','dsi','dsi_workload' \n";
//		if (srvVersion >= Ver.ver(15, 6))
//			AOBJ = "--trace 'off','dsi','dsi_workload' \n";
//			
//		return statsOff + AOBJ;
//	}
	
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSampleAdminStats(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	
	/**
	 * Create a new Sample based on the values in CmAdminStats object
	 * <p>
	 * All Counters in the table for a specific module will be a pivot table (one "module" for one "instance" will be one row, with many columns)
	 * 
	 * @param moduleName
	 * @param xrstm 
	 * @return
	 */
	protected List<List<Object>> getModuleCounters(String moduleName, DbxTuneResultSetMetaData xrstm)
	{
		String module     = "";
		int    module_pos = -1;

		String instance     = "";
		int    instance_pos = -1;

		Integer instanceId;
		int     instanceId_pos = -1;

		String type     = "";
		int    type_pos = -1;

		String name     = "";
		int    name_pos = -1;

		Long obs     = null;
		int  obs_pos = -1;

		Long total     = null;
		int  total_pos = -1;

		Long last     = null;
		int  last_pos = -1;

//		Long rateXsec     = null;
//		int  rateXsec_pos = -1;

		CounterSample cs = getCounterSampleAbs();

		// Find column Id's
		List<String> colNames = cs.getColNames();
		if (colNames == null)
			return null;

		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Module"))      module_pos      = colId;
			else if (colName.equals("Instance"))    instance_pos    = colId;
			else if (colName.equals("InstanceId"))  instanceId_pos  = colId;
			else if (colName.equals("Type"))        type_pos        = colId;
			else if (colName.equals("Name"))        name_pos        = colId;
			else if (colName.equals("Obs"))         obs_pos         = colId;
			else if (colName.equals("Total"))       total_pos       = colId;
			else if (colName.equals("Last"))        last_pos        = colId;
//			else if (colName.equals("RateXsec"))    rateXsec_pos    = colId;
		}

		List<List<Object>> rows = new ArrayList<List<Object>>();
		List<Object>       row = new ArrayList<Object>();

		String currentInstance = "";

		// Loop on all diffData rows
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			module     = (String)  cs.getValueAt(rowId, module_pos);

			if (! moduleName.equals(module))
				continue;

			instance   = (String)  cs.getValueAt(rowId, instance_pos);
			instanceId = (Integer) cs.getValueAt(rowId, instanceId_pos);
			type       = (String)  cs.getValueAt(rowId, type_pos);
			name       = (String)  cs.getValueAt(rowId, name_pos);
			obs        = (Long)    cs.getValueAt(rowId, obs_pos);
			total      = (Long)    cs.getValueAt(rowId, total_pos);
			last       = (Long)    cs.getValueAt(rowId, last_pos);
//			rateXsec   = (Long)    cs.getValueAt(rowId, rateXsec_pos);

			if ( ! currentInstance.equals(instance) )
			{
				currentInstance = instance;

				if (xrstm.addStrColumn ("Instance",   -1, false, 255, "")) ;//System.out.println("    > module='"+module+"': Instance='"+currentInstance+"', addColumn='Instance'.");
				if (xrstm.addIntColumn ("InstanceId", -1, false,      "")) ;//System.out.println("    > module='"+module+"': Instance='"+currentInstance+"', addColumn='InstanceId'.");
				if (xrstm.addLongColumn(name,         -1, true,       "")) ;//System.out.println("    > module='"+module+"': Instance='"+currentInstance+"', addColumn='"+name+"'.");

				row = new ArrayList<Object>(xrstm.getColumnCount());
				row.add(instance);
				row.add(instanceId);
				
				rows.add(row);

				Long val;
				if      ("OBSERVER".equals(type)) val = obs;
				else if ("COUNTER" .equals(type)) val = total;
				else                              val = last;

				row.add(val);
//				System.out.println("    > -addValue='"+val+"'.");
			}
			else
			{
				if (xrstm.addLongColumn(name, -1, true, ""))
					;//System.out.println("    > module='"+module+"': Instance='"+currentInstance+"', addColumn='"+name+"'.");

				int addPos = xrstm.getColumnSqlPos(name) - 1;
				
				Long val;
				if      ("OBSERVER".equals(type)) val = obs;
				else if ("COUNTER" .equals(type)) val = total;
				else                              val = last;

				row.add(addPos, val);
//				System.out.println("    > -addValue='"+val+"'.");
			}
		}
//		System.out.println("=========================================================");
//		System.out.println("COLS("+xrstm.getColumnNames().size()+"): "+ xrstm.getColumnNames());
//		System.out.println("---------------------------------------------------------");
//		for (List<Object> r : rows)
//			System.out.println("ROW("+r.size()+"): "+r);
//		System.out.println("---------------------------------------------------------");
		
		return rows;
	}




	/**
	 * Get all rows that is in the input Set
	 * 
	 * @param counterIdSet a set of CounterId's that we want to get
	 * @return
	 */
	protected List<List<Object>> getCounterIds(int whatData, Set<Integer> counterIdSet)
	{
		Integer counterId;
		int     counterId_pos = -1;

		CounterSample cs;
		if      (whatData == DATA_ABS)  cs = getCounterSampleAbs();
		else if (whatData == DATA_DIFF) cs = getCounterSampleDiff();
		else if (whatData == DATA_RATE) cs = getCounterSampleRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available. you passed whatData="+whatData);

		// Find column Id's
		List<String> colNames = cs.getColNames();
		if (colNames == null)
			return null;

		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if (colName.equals("CounterId"))   counterId_pos   = colId;
		}

		List<List<Object>> rows = new ArrayList<List<Object>>();
		List<Object>       row = null;

		// Get all rows which is in the counterIdSet
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			row = cs.getRow(rowId);
			
			counterId = (Integer)  row.get(counterId_pos);

			if (! counterIdSet.contains(counterId))
				continue;

			rows.add(row);
		}

		return rows;
	}

//	/**
//	 * Create a new Sample based on the values in CmAdminStats object
//	 * <p>
//	 * All Counters in the table for a specific module will be a pivot table (one "module" for one "instance" will be one row, with many columns)
//	 * 
//	 * @param moduleName
//	 * @param xrstm 
//	 * @return
//	 */
//	protected List<List<Object>> getModuleSourceToDest(DbxTuneResultSetMetaData xrstm)
//	{
//		String module     = "";
//		int    module_pos = -1;
//
//		String instance     = "";
//		int    instance_pos = -1;
//
//		Integer instanceId;
//		int     instanceId_pos = -1;
//
//		Integer counterId;
//		int     counterId_pos = -1;
//
//		String type     = "";
//		int    type_pos = -1;
//
//		String name     = "";
//		int    name_pos = -1;
//
//		Long obs     = null;
//		int  obs_pos = -1;
//
//		Long total     = null;
//		int  total_pos = -1;
//
//		Long last     = null;
//		int  last_pos = -1;
//
////		Long rateXsec     = null;
////		int  rateXsec_pos = -1;
//
//		CounterSample cs = getCounterSampleAbs();
//
//		// Find column Id's
//		List<String> colNames = cs.getColNames();
//		if (colNames == null)
//			return null;
//
//		for (int colId = 0; colId < colNames.size(); colId++)
//		{
//			String colName = (String) colNames.get(colId);
//			if      (colName.equals("Module"))      module_pos      = colId;
//			else if (colName.equals("Instance"))    instance_pos    = colId;
//			else if (colName.equals("InstanceId"))  instanceId_pos  = colId;
//			else if (colName.equals("CounterId"))   counterId_pos   = colId;
//			else if (colName.equals("Type"))        type_pos        = colId;
//			else if (colName.equals("Name"))        name_pos        = colId;
//			else if (colName.equals("Obs"))         obs_pos         = colId;
//			else if (colName.equals("Total"))       total_pos       = colId;
//			else if (colName.equals("Last"))        last_pos        = colId;
////			else if (colName.equals("RateXsec"))    rateXsec_pos    = colId;
//		}
//
//		List<List<Object>> rows = new ArrayList<List<Object>>();
//		List<Object>       row = new ArrayList<Object>();
//
////		db2db
////		--	58000 (@src_dbid)				Repagent: Commands received	REPAGENT	CmdsRecv
////		--	6000  (@src_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020  (@src_ldbid)				SQM: Active queue segments	SQM	SegsActive
////		--	62000 (special: @src_ldbid, @sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
////		--	24000 (special: see code)			SQT: Commands read from queue	SQT	cmds	CmdsRead
////		--	30000 (@src_dbid)				DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		--	6000  (@dest_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020  (@dest_ldbid)				SQM: Active queue segments	SQM	SegsActive
////		--	62013 (@src_ldbid, @sqt_reader)			SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	62013 (@dest_dbid, 10)				SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	5030  (@dest_dbid)				DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
////		rsi2db
////		--	59001	(src_rsid)	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
////		--	6000	(dest_ldbid)	SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(dest_dbid)	SQM: Active queue segments	SQM	SegsActive
////		--	62013	(dest_dbid)	SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	5030	(dest_dbid)	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
////		db2rsi
////		--	58000	(src_dbid)		Repagent: Commands received	REPAGENT	CmdsRecv
////		--	6000	(src_dbid)		SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(src_dbid)		SQM: Active queue segments	SQM	SegsActive
////		--	62000	(src_ldbid, sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
////		--	24000	(src_ldbid)		SQT: Commands read from queue	SQT	cmds	CmdsRead
////		--	30000	(src_dbid)		DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		--	6000	(dest_rsid)		SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(dest_rsid)		SQM: Active queue segments	SQM	SegsActive
////		--	62013	(src_ldbid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		-- route backlog SQMR rs_statdetail.label looks like: SQMR, 16777320:0 BARCELONA15_RS_1, 0, GLOBAL RS
////		-- but the easy way to tell is that the instance_id is the @dest_rsid
////		--	62013	(dest_rsid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	4004	(dest_rsid)		RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent
//		
//		Set<Integer> counterIdSet = new HashSet<Integer>();
//		counterIdSet.add(58000); // 58000	Repagent: Commands received	REPAGENT	CmdsRecv
//		counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
//		counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
//		counterIdSet.add(62000); // 62000	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
//		counterIdSet.add(24000); // 24000	SQT: Commands read from queue	SQT	cmds	CmdsRead
//		counterIdSet.add(30000); // 30000	DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
////		counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
//		counterIdSet.add(62013); // 62013	SQMR: Unread segments	SQMR	SQMRBacklogSeg
//		counterIdSet.add(5030);  // 5030	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
//
//		// rsi2db
//		counterIdSet.add(59001); // 59001	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
//
//		// db2rsi
//		counterIdSet.add(4004);  // 4004	RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent
//
//		
//		xrstm.addStrColumn("type",               1,  false, 10, "what type is this: db2db, rsi2db, db2rsi");
//		xrstm.addStrColumn("source",             2,  false, 62, "Source of the data");
//		xrstm.addStrColumn("destination",        3,  false, 62, "Destination");
//
//		xrstm.addLongColumn("RACmdsPerSec",      4,  true, ""); // db2db
//		xrstm.addLongColumn("SQMInCmdsPerSec",   5,  true, ""); // db2db
//		xrstm.addLongColumn("SegsActiveInMB",    6,  true, ""); // db2db
//		xrstm.addLongColumn("BacklogInMB",       7,  true, ""); // db2db
//		xrstm.addLongColumn("SQMRCmdsPerSec",    8,  true, ""); // db2db
//		xrstm.addLongColumn("SQTCmdsPerSec",     9,  true, ""); // db2db
//		xrstm.addLongColumn("DISTCmdsPerSec",    10, true, ""); // db2db
//		xrstm.addLongColumn("SQMOutCmdsPerSec",  11, true, ""); // db2db
//		xrstm.addLongColumn("SegsActiveOutMB",   12, true, ""); // db2db
//		xrstm.addLongColumn("DSICmdsSec",        13, true, ""); // db2db
//
////		xrstm.addLongColumn("RSIUMsgSec",        4, true, ""); // rsi2db
////		xrstm.addLongColumn("SQMOutCmdsPerSec",  5, true, ""); // rsi2db
////		xrstm.addLongColumn("SegsActiveOut",     6, true, ""); // rsi2db
////		xrstm.addLongColumn("SegsActiveOutMB",   7, true, ""); // rsi2db
////		xrstm.addLongColumn("DSICmdsSec",        8, true, ""); // rsi2db
////
////		xrstm.addLongColumn("RACmdsPerSec",      4, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMInCmdsPerSec",   5, true, ""); // db2rsi
////		xrstm.addLongColumn("SegsActiveInMB",    6, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMRCmdsPerSec",    7, true, ""); // db2rsi
////		xrstm.addLongColumn("SQTCmdsPerSec",     8, true, ""); // db2rsi
////		xrstm.addLongColumn("DISTCmdsPerSec",    9, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMOutCmdsPerSec", 10, true, ""); // db2rsi
////		xrstm.addLongColumn("SegsActiveOutMB",  11, true, ""); // db2rsi
////		xrstm.addLongColumn("BacklogOutMB",     12, true, ""); // db2rsi
////		xrstm.addLongColumn("RSIMsgsSec",       13, true, ""); // db2rsi
//
////			values ('RS15.7', 'RateSummary db2db', 'RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Active(i)',  'Backlog(i)', 'SQMR(SQT)', 'SQT',  'DIST',   'SQM(o)',    'Active(o)',  'DSI')
////			values ('RS15.7', 'RateSummary db2rsi','RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Backlog(i)', 'SQMR(SQT)',  'SQT',       'DIST', 'SQM(o)', 'Active(r)', 'Backlog(r)', 'RSI')
////			values ('RS15.7', 'RateSummary rsi2db','RS Performance Summary (cmds/sec, backlog in MB):','RSIUser',  'SQM(i)', 'Active(i)',  'ActiveMB',   'DSI',       null,   null,     null,        null,         null)
//
//		
//		// Get all rows which is in the counterIdSet
//		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
//		{
//			row = cs.getRow(rowId);
//			
//			counterId = (Integer)  row.get(counterId_pos);
//
//			if (! counterIdSet.contains(counterId))
//				continue;
//
//			rows.add(row);
//		}
//
//		// Loop 
//		for (int rowId = 0; rowId < rows.size(); rowId++)
//		{
//			row = rows.get(rowId);
//			counterId = (Integer)  row.get(counterId_pos);
//
//			instance   = (String)  row.get(instance_pos);
//			instanceId = (Integer) row.get(instanceId_pos);
//			type       = (String)  row.get(type_pos);
//			name       = (String)  row.get(name_pos);
//			obs        = (Long)    row.get(obs_pos);
//			total      = (Long)    row.get(total_pos);
//			last       = (Long)    row.get(last_pos);
////			rateXsec   = (Long)    row.get(rateXsec_pos);
//			
//			System.out.println("instance='"+instance+"', instanceId="+instanceId+", type='"+type+"', name='"+name+"', obs="+obs+", total="+total+", last="+last+".");
//		}
//		
//		return rows;
//	}
}








//protected List<List<Object>> getModuleCounters(String moduleName, DbxTuneResultSetMetaData xrstm)
//{
//	CounterSample csTmp = getCounterSampleAbs();
//
//	if (csTmp == null)
//		throw new RuntimeException("getCounterSampleAbs() returned null");
//
//	if ( ! (csTmp instanceof CounterSampleAdminStats) )
//		throw new RuntimeException("getCounterSampleAbs() expected Object of 'CounterSampleAdminStats' but we got '"+csTmp.getClass().getName()+"'.");
//
//	CounterSampleAdminStats cs = (CounterSampleAdminStats) csTmp;
//	
//	RsStatCounterDictionary dict = RsStatCounterDictionary.getInstance();
//	for (Instance i : cs.getInstanceList())
//	{
//		// SKIP empty instances, it looks like it's only on the RSSD RepAgent (if the RSSD isn't primary)
//		if ("".equals(i._name))
//			continue;
//
//		for (Counter counter : i._counterMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("COUNTER");
//			row.add(counter._name);
//			row.add(counter._obs);
//			row.add(counter._total);
//			row.add(counter._last);
//			row.add(counter._max);
//			row.add(counter._avg_ttl_obs);
//			row.add(counter._rate_x_sec);
//
//			StatCounterEntry c = dict.getCounter(i._name, counter._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//
//		for (Monitor monitor : i._monitorMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("MONITOR");
//			row.add(monitor._name);
//			row.add(monitor._obs);
//			row.add(null);
//			row.add(monitor._last);
//			row.add(monitor._max);
//			row.add(monitor._avg_ttl_obs);
//			row.add(null);
//
//			StatCounterEntry c = dict.getCounter(i._name, monitor._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//
//		for (Observer observer : i._observerMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("OBSERVER");
//			row.add(observer._name);
//			row.add(observer._obs);
//			row.add(null);
//			row.add(null);
//			row.add(null);
//			row.add(null);
//			row.add(observer._rate_x_sec);
//
//			StatCounterEntry c = dict.getCounter(i._name, observer._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//	}
//
//	return rows;
//}
