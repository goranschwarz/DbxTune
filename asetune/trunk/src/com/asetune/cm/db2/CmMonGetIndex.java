package com.asetune.cm.db2;

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

public class CmMonGetIndex
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmMonGetTable.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetIndex.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Index Activity</h4>" + 
		"Fixme." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		/* RS> Col# Label                                                  JDBC Type Name          Guessed DBMS type Source Table */
		/* RS> ---- -----------------------------------------------        ----------------------- ----------------- ------------- */
//		/* RS> 1    */ "TABSCHEMA",                                       /* java.sql.Types.VARCHAR  VARCHAR(128)      SAMPLE.-none- */
//		/* RS> 2    */ "TABNAME",                                         /* java.sql.Types.VARCHAR  VARCHAR(128)      SAMPLE.-none- */
//		/* RS> 3    */ "IID",                                             /* java.sql.Types.SMALLINT SMALLINT          SAMPLE.-none- */
//		/* RS> 4    */ "MEMBER",                                          /* java.sql.Types.SMALLINT SMALLINT          SAMPLE.-none- */
//		/* RS> 5    */ "DATA_PARTITION_ID",                               /* java.sql.Types.INTEGER  INTEGER           SAMPLE.-none- */
//		/* RS> 6    */ "NLEAF",                                           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
//		/* RS> 7    */ "NLEVELS",                                         /* java.sql.Types.SMALLINT SMALLINT          SAMPLE.-none- */
		/* RS> 8    */ "INDEX_SCANS",                                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 9    */ "INDEX_ONLY_SCANS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 10   */ "KEY_UPDATES",                                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 11   */ "INCLUDE_COL_UPDATES",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 12   */ "PSEUDO_DELETES",                                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 13   */ "DEL_KEYS_CLEANED",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 14   */ "ROOT_NODE_SPLITS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 15   */ "INT_NODE_SPLITS",                                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 16   */ "BOUNDARY_LEAF_NODE_SPLITS",                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 17   */ "NONBOUNDARY_LEAF_NODE_SPLITS",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 18   */ "PAGE_ALLOCATIONS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 19   */ "PSEUDO_EMPTY_PAGES",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 20   */ "EMPTY_PAGES_REUSED",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 21   */ "EMPTY_PAGES_DELETED",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 22   */ "PAGES_MERGED",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 23   */ "OBJECT_INDEX_L_READS",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 24   */ "OBJECT_INDEX_P_READS",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 25   */ "OBJECT_INDEX_GBP_L_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 26   */ "OBJECT_INDEX_GBP_P_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 27   */ "OBJECT_INDEX_GBP_INVALID_PAGES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 28   */ "OBJECT_INDEX_LBP_PAGES_FOUND",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 29   */ "OBJECT_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 30   */ "INDEX_JUMP_SCANS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 31   */ "OBJECT_INDEX_CACHING_TIER_L_READS",               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 32   */ "OBJECT_INDEX_CACHING_TIER_PAGES_FOUND",           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 33   */ "OBJECT_INDEX_CACHING_TIER_GBP_INVALID_PAGES",     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
		/* RS> 34   */ "OBJECT_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND", /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none- */
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

		return new CmMonGetIndex(counterController, guiController);
	}

	public CmMonGetIndex(ICounterController counterController, IGuiController guiController)
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
//		setBackgroundDataPollingEnabled(true, false);
		
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
//		return new CmMonGetTablePanel(this);
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
			mtd.addTable("MON_GET_TABLE",  "fixme.");

			mtd.addColumn("MON_GET_TABLE", "xxx",    "<html>xxx</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TABSCHEMA");
		pkCols.add("TABNAME");
		pkCols.add("IID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * FROM TABLE(MON_GET_INDEX('','',-2))";

		return sql;
	}
}
