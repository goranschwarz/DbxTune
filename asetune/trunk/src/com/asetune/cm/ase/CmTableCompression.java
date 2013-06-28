package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTableCompression
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTableCompression.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableCompression.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Compression";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Table Compression Information" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15700;
	public static final int      NEED_SRV_VERSION = 1570000;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monTableCompression"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "capture compression statistics=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CompRowInserted", "CompRowUpdated", "CompRowForward", 
		"CompRowScan", "RowDecompressed", "RowPageDecompressed", 
		"ColDecompressed", "RowCompNoneed", "PageCompNoneed", "PagesCompressed"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmTableCompression(counterController, guiController);
	}

	public CmTableCompression(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmTableCompressionPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("DBID");
		pkCols.add("ObjectID");
		pkCols.add("PartitionID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols;

		String InstanceID = "";
		if (isClusterEnabled)
			InstanceID = "InstanceID, ";

		cols = InstanceID + "DBName=db_name(DBID), TableName, \n" +
			"CompRowInserted, CompRowUpdated, CompRowForward, CompRowScan, \n" +
			"RowDecompressed, RowPageDecompressed, ColDecompressed, \n" +
			"RowCompNoneed, PageCompNoneed, \n" +
			"PagesCompressed, AvgBytesSavedPageLevel, \n" +
			"DBID, ObjectID, PartitionID";

		String sql = 
			"select " + cols + "\n" +
			"from master..monTableCompression\n" +
			"";

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
}
