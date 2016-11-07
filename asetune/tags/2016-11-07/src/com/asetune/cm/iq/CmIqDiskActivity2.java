package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * 
 * @author I063869
 *
 */
public class CmIqDiskActivity2
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqDiskActivity2.class.getSimpleName();
	public static final String   SHORT_NAME       = "disk activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>disk activity - custom</h4>" + 
		"based on values from sp_iq_statistics." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"iq_disk_activity_custom"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"stat_value"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;
	
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

		return new CmIqDiskActivity2(counterController, guiController);
	}

	public CmIqDiskActivity2(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);
		
		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

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
			mtd.addTable("iq_disk_activity_custom",  
					"Returns serial number, name, description, value, and unit specifier for each available statistic, or a specified statistic.");

			mtd.addColumn("iq_disk_activity_custom", "stat_num",  
					"<html>Serial number of a statistic</html>");
			mtd.addColumn("iq_disk_activity_custom", "stat_name",  
					"<html>Name of statistic</html>");
			mtd.addColumn("iq_disk_activity_custom", "stat_value",  
					"<html>Value of statistic</html>");
			mtd.addColumn("iq_disk_activity_custom", "stat_unit",  
					"<html>Unit specifier</html>");
			mtd.addColumn("iq_disk_activity_custom", "stat_desc", 
					"<html>Description of statistic</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		
		pkCols.add("stat_num");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n"
			+ "    stat_num, \n"
			+ "    stat_name, \n"
			+ "    stat_desc, \n"
//			+ "    stat_value, \n" // This is data type "text", so lets try to convert it
			+ "    stat_value=convert(bigint,stat_value), \n"
			+ "    stat_unit \n"
			+ "from sp_iqstatistics() \n" 
			+ "where stat_name in ( \n"
			+ "          'MainStoreDiskReads','MainStoreDiskWrites','TempStoreDiskReads','TempStoreDiskWrites','CacheDbspaceDiskReads','CacheDbspaceDiskWrites' \n"
			+ "      ) \n"
//			+ "  and IsNumeric(stat_value) = 1 \n"
			+ "";

		return sql;
	}
}
