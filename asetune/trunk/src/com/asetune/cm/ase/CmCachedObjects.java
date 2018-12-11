package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmCachedObjectsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmCachedObjects
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmCachedObjects.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmCachedObjects.class.getSimpleName();
	public static final String   SHORT_NAME       = "Cached Objects";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What Tables is located in the 'data cache' and what cache is it bound to</p>" +
		"<b>This could be a bit heavy to sample, so use with caution. At least before ASE 15.5 ESD#4</b><br>" +
		"Tip:<br>" +
		"<ul>" +
		"    <li>Use ABSolute counter values to check how many MB the table are using.</li>" +
		"    <li>Use Diff or Rate to check if the memory print is increasing or decreasing.</li>" +
		"    <li>Use the checkpox 'Pause Data Polling' wait for a minute, then enable polling again, that will give you a longer sample period!</li>" +
		"</ul>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - An Index.</li>" +
		"</ul>" +
	"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedObject"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"TableCachedPct", "CacheUsagePct"};
	public static final String[] DIFF_COLUMNS     = new String[] {"CachedKBDiff", "TotalSizeKBDiff"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmCachedObjects(counterController, guiController);
	}

	public CmCachedObjects(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_topRows                = PROP_PREFIX + ".sample.topRows";
	public static final boolean DEFAULT_sample_topRows                = false;

	public static final String  PROPKEY_sample_topRowsCount           = PROP_PREFIX + ".sample.topRows.count";
	public static final int     DEFAULT_sample_topRowsCount           = 2000;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows,                DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_topRowsCount,           DEFAULT_sample_topRowsCount);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Limit num of rows",     PROPKEY_sample_topRows      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_topRows      , DEFAULT_sample_topRows      ), DEFAULT_sample_topRows     , "Get only first # rows (select top # ...) true or false"   ));
		list.add(new CmSettingsHelper("Limit num of rowcount", PROPKEY_sample_topRowsCount , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRowsCount , DEFAULT_sample_topRowsCount ), DEFAULT_sample_topRowsCount, "Get only first # rows (select top # ...), number of rows" ));

		return list;
	}

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmCachedObjectsPanel(this);
	}

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
			mtd.addColumn("monCachedObject", "TableCachedPct",   
			                                                "<html>" +
			                                                    "How much of the Table is cached.<br> " +
			                                                    "<b>Formula</b>: TableCachedPct = CachedKB/(TotalSizeKB*1.0) * 100.0<br>" +
			                                                "</html>");
			mtd.addColumn("monCachedObject", "TotalCacheSizeKB",   
			                                                "<html>" +
			                                                    "Total Cache Size of the Data Cache where the table is bound to.<br> " +
			                                                    "<b>Formula</b>: <br>select CacheID, TotalCacheSizeKB = sum(AllocatedKB) <br>from master..monCachePool <br>group by CacheID<br>" +
			                                                "</html>");
			mtd.addColumn("monCachedObject", "CacheUsagePct",   
			                                                "<html>" +
			                                                    "How much of the Cache does this Table use.<br> " +
			                                                    "<b>Formula</b>: CacheUsagePct = CachedKB/(TotalCacheSizeKB*1.0) * 100.0<br>" +
			                                                "</html>");
			mtd.addColumn("monCachedObject", "CachedKBDiff",
			                                                "<html>" +
			                                                    "How many KB of this Table is cached, since previous sample<br> " +
			                                                    "<b>Formula</b>: simply CachedKB as diff calculated value<br>" +
			                                                "</html>");
			mtd.addColumn("monCachedObject", "TotalSizeKBDiff", 
			                                                "<html>" +
			                                                    "Total Table Size in KB, since previous sample.<br> " +
			                                                    "<b>Formula</b>: simply TotalSizeKB as diff calculated value<br>" +
			                                                "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("DBID");
		pkCols.add("OwnerUserID");
		pkCols.add("ObjectID");
		pkCols.add("IndexID");

		if (srvVersion >= Ver.ver(15,0))
			pkCols.add("PartitionID");

		pkCols.add("CacheID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String topRows      = ""; // 'top 500' if only first 500 rows should be displayed
		
		String preDropTempTables = 
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo \n" +
			"go \n";

		String populateTempTables = 
			"/*------ Snapshot, cache size -------*/ \n" +
			"select CacheName, CacheID, TotalCacheSizeKB = sum(AllocatedKB) \n" +
			"into #cacheInfo \n" +
			"from master..monCachePool \n" +
			"group by CacheName, CacheID \n";
		
		String postDropTempTables = 
			"/*------ drop tempdb objects -------*/ \n" +
			"drop table #cacheInfo\n";

		addDropTempTable("#cacheInfo");

		// TOP ROWS
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf.getBooleanProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows))
		{
			int rowCount = conf.getIntProperty(PROPKEY_sample_topRowsCount, DEFAULT_sample_topRowsCount);
			topRows = "top " + rowCount + " ";

			_logger.warn("CM='"+getName()+"'. Limiting number of rows fetch. Adding phrase '"+topRows+"' at the start of the SQL Statement.");
		}

		if (isClusterEnabled)
		{
			cols1 += "M.InstanceID, ";
		}

		String TableCachedPct    = "";
		String TotalSizeKB_where = "";
		if (srvVersion >= Ver.ver(15,0))
		{
			TableCachedPct    = "TableCachedPct = convert(numeric(5,1), M.CachedKB/(M.TotalSizeKB*1.0) * 100.0), \n";
			TotalSizeKB_where = "  and M.TotalSizeKB > 0 \n";
		}
		
		cols1 += "M.DBID, M.OwnerUserID, M.ObjectID, M.IndexID, M.DBName, OwnerName=isnull(M.OwnerName, 'dbo'), M.ObjectName, M.ObjectType, \n";
		cols2 += "";
		cols3 += "M.CachedKB, CachedKBDiff=M.CachedKB, "+TableCachedPct+"M.CacheName, M.CacheID, \n" +
		         "T.TotalCacheSizeKB, CacheUsagePct = convert(numeric(5,1), M.CachedKB/(T.TotalCacheSizeKB*1.0) * 100.0)";

		if (srvVersion >= Ver.ver(15,5,0,4)) // dont really know when this was introduced, but it was in my 15.5.0 ESD#4
			cols2 += "M.ProcessesAccessing, \n";

		if (srvVersion >= Ver.ver(15,0))
			cols2 += "M.PartitionID, M.PartitionName, M.TotalSizeKB, TotalSizeKBDiff=M.TotalSizeKB, \n";

		String sql = 
			preDropTempTables + 
			"\n" +
			populateTempTables +
			"\n" +
			"/*------ SQL to get data -------*/ \n" +
			"select " + topRows + cols1 + cols2 + cols3 + "\n" +
			"from master..monCachedObject M, #cacheInfo T \n" +
			"where M.CacheID = T.CacheID \n" +
			TotalSizeKB_where +
			"order by M.CachedKB desc\n" +
			"\n" +
			postDropTempTables;

		return sql;
	}

	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return 10;
	}

	/**
	 * discard "DDL Save" when: Objects with IndexID > 0 and ObjectType == "system table"
	 */
	@Override
	public boolean sendDdlDetailsRequestForSpecificRow(String dBName, String objectName, int row, CounterSample absData, CounterSample diffData, CounterSample rateData)
	{
		// If ObjectType is "system table", then do NOT send it for DDL Storage
		int ObjectType_pos = absData.findColumn("ObjectType");
		if (ObjectType_pos == -1)
			return true;

		Object ObjectType_obj = absData.getValueAt(row, ObjectType_pos);
		if (ObjectType_obj instanceof String)
		{
			String ObjectType = (String) ObjectType_obj;
			if ("system table".equals(ObjectType))
				return false;
		}

		// If IndexID is above 0, then do NOT send it for DDL Storage
		int IndexID_pos = absData.findColumn("IndexID");
		if (IndexID_pos == -1)
			return true;

		Object IndexID_obj = absData.getValueAt(row, IndexID_pos);
		if (IndexID_obj instanceof Number)
		{
			int IndexID = ((Number)IndexID_obj).intValue();
			if (IndexID > 0)
				return false;
		}

		// Lets send it for DDL Storage
		return true;
	}
}
