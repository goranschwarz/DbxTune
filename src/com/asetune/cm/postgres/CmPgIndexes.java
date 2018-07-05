package com.asetune.cm.postgres;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgIndexesPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgIndexes
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgTables.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgIndexes.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row for each index in the current database, showing statistics about accesses to that specific index." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_all_indexes"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"idx_scan",
			"idx_tup_read",
			"idx_tup_fetch"
	};
	
//	RS> Col# Label         JDBC Type Name         Guessed DBMS type Source Table       
//	RS> ---- ------------- ---------------------- ----------------- -------------------
//	RS> 1    relid         java.sql.Types.BIGINT  oid               pg_stat_all_indexes
//	RS> 2    indexrelid    java.sql.Types.BIGINT  oid               pg_stat_all_indexes
//	RS> 3    schemaname    java.sql.Types.VARCHAR name(2147483647)  pg_stat_all_indexes
//	RS> 4    relname       java.sql.Types.VARCHAR name(2147483647)  pg_stat_all_indexes
//	RS> 5    indexrelname  java.sql.Types.VARCHAR name(2147483647)  pg_stat_all_indexes
//	RS> 6    idx_scan      java.sql.Types.BIGINT  int8              pg_stat_all_indexes
//	RS> 7    idx_tup_read  java.sql.Types.BIGINT  int8              pg_stat_all_indexes
//	RS> 8    idx_tup_fetch java.sql.Types.BIGINT  int8              pg_stat_all_indexes
	
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

		return new CmPgIndexes(counterController, guiController);
	}

	public CmPgIndexes(ICounterController counterController, IGuiController guiController)
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

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,           DEFAULT_sample_systemTables);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Tables", PROPKEY_sample_systemTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables  , DEFAULT_sample_systemTables  ), DEFAULT_sample_systemTables, "Sample System Tables" ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgIndexesPanel(this);
	}


	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		pkCols.add("relid");
		pkCols.add("indexrelid");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String tabName = "pg_catalog.pg_stat_user_indexes";

		// Sample System Tables
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemTables, DEFAULT_sample_systemTables))
		{
			tabName = "pg_catalog.pg_stat_all_indexes";
		}

		return "select current_database() as dbname, * from "+tabName;
	}
}
