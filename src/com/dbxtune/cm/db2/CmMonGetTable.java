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
package com.dbxtune.cm.db2;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

public class CmMonGetTable
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmMonGetTable.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetTable.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Table Activity</h4>" + 
		"Fixme." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"TABLE_SCANS",
			"ROWS_READ",
			"ROWS_INSERTED",
			"ROWS_UPDATED",
			"ROWS_DELETED",
			"OVERFLOW_ACCESSES",
			"OVERFLOW_CREATES",
			"PAGE_REORGS",
			"DATA_OBJECT_L_PAGES",
			"LOB_OBJECT_L_PAGES",
			"LONG_OBJECT_L_PAGES",
			"INDEX_OBJECT_L_PAGES",
			"XDA_OBJECT_L_PAGES",
//			"DBPARTITIONNUM",
			"NO_CHANGE_UPDATES",
			"LOCK_WAIT_TIME",
			"LOCK_WAIT_TIME_GLOBAL",
			"LOCK_WAITS",
			"LOCK_WAITS_GLOBAL",
			"LOCK_ESCALS",
			"LOCK_ESCALS_GLOBAL",
//			"DATA_SHARING_STATE",
//			"DATA_SHARING_STATE_CHANGE_TIME",
			"DATA_SHARING_REMOTE_LOCKWAIT_COUNT",
			"DATA_SHARING_REMOTE_LOCKWAIT_TIME",
			"DIRECT_WRITES",
			"DIRECT_WRITE_REQS",
			"DIRECT_READS",
			"DIRECT_READ_REQS",
			"OBJECT_DATA_L_READS",
			"OBJECT_DATA_P_READS",
			"OBJECT_DATA_GBP_L_READS",
			"OBJECT_DATA_GBP_P_READS",
			"OBJECT_DATA_GBP_INVALID_PAGES",
			"OBJECT_DATA_LBP_PAGES_FOUND",
			"OBJECT_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",
			"OBJECT_XDA_L_READS",
			"OBJECT_XDA_P_READS",
			"OBJECT_XDA_GBP_L_READS",
			"OBJECT_XDA_GBP_P_READS",
			"OBJECT_XDA_GBP_INVALID_PAGES",
			"OBJECT_XDA_LBP_PAGES_FOUND",
			"OBJECT_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",
			"NUM_PAGE_DICT_BUILT",
			"STATS_ROWS_MODIFIED",
			"RTS_ROWS_MODIFIED",
			"COL_OBJECT_L_PAGES",
//			"TAB_ORGANIZATION",
			"OBJECT_COL_L_READS",
			"OBJECT_COL_P_READS",
			"OBJECT_COL_GBP_L_READS",
			"OBJECT_COL_GBP_P_READS",
			"OBJECT_COL_GBP_INVALID_PAGES",
			"OBJECT_COL_LBP_PAGES_FOUND",
			"OBJECT_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",
			"NUM_COLUMNS_REFERENCED",
			"SECTION_EXEC_WITH_COL_REFERENCES",
			"OBJECT_DATA_CACHING_TIER_L_READS",
			"OBJECT_DATA_CACHING_TIER_PAGES_FOUND",
			"OBJECT_DATA_CACHING_TIER_GBP_INVALID_PAGES",
			"OBJECT_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",
			"OBJECT_XDA_CACHING_TIER_L_READS",
			"OBJECT_XDA_CACHING_TIER_PAGES_FOUND",
			"OBJECT_XDA_CACHING_TIER_GBP_INVALID_PAGES",
			"OBJECT_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",
			"OBJECT_COL_CACHING_TIER_L_READS",
			"OBJECT_COL_CACHING_TIER_PAGES_FOUND",
			"OBJECT_COL_CACHING_TIER_GBP_INVALID_PAGES",
			"OBJECT_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",
			"EXT_TABLE_RECV_WAIT_TIME",
			"EXT_TABLE_RECVS_TOTAL",
			"EXT_TABLE_RECV_VOLUME",
			"EXT_TABLE_READ_VOLUME",
			"EXT_TABLE_SEND_WAIT_TIME",
			"EXT_TABLE_SENDS_TOTAL",
			"EXT_TABLE_SEND_VOLUME",
			"EXT_TABLE_WRITE_VOLUME"
	};

//	RS> Col# Label                                          JDBC Type Name           Guessed DBMS type Source Table 
//	RS> ---- ---------------------------------------------- ------------------------ ----------------- -------------
//	RS> 1    TABSCHEMA                                      java.sql.Types.VARCHAR   VARCHAR(128)      SAMPLE.-none-
//	RS> 2    TABNAME                                        java.sql.Types.VARCHAR   VARCHAR(128)      SAMPLE.-none-
//	RS> 3    MEMBER                                         java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none-
//	RS> 4    TAB_TYPE                                       java.sql.Types.VARCHAR   VARCHAR(14)       SAMPLE.-none-
//	RS> 5    TAB_FILE_ID                                    java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 6    DATA_PARTITION_ID                              java.sql.Types.INTEGER   INTEGER           SAMPLE.-none-
//	RS> 7    TBSP_ID                                        java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 8    INDEX_TBSP_ID                                  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 9    LONG_TBSP_ID                                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 10   TABLE_SCANS                                    java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 11   ROWS_READ                                      java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 12   ROWS_INSERTED                                  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 13   ROWS_UPDATED                                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 14   ROWS_DELETED                                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 15   OVERFLOW_ACCESSES                              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 16   OVERFLOW_CREATES                               java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 17   PAGE_REORGS                                    java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 18   DATA_OBJECT_L_PAGES                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 19   LOB_OBJECT_L_PAGES                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 20   LONG_OBJECT_L_PAGES                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 21   INDEX_OBJECT_L_PAGES                           java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 22   XDA_OBJECT_L_PAGES                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 23   DBPARTITIONNUM                                 java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none-
//	RS> 24   NO_CHANGE_UPDATES                              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 25   LOCK_WAIT_TIME                                 java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 26   LOCK_WAIT_TIME_GLOBAL                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 27   LOCK_WAITS                                     java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 28   LOCK_WAITS_GLOBAL                              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 29   LOCK_ESCALS                                    java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 30   LOCK_ESCALS_GLOBAL                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 31   DATA_SHARING_STATE                             java.sql.Types.VARCHAR   VARCHAR(19)       SAMPLE.-none-
//	RS> 32   DATA_SHARING_STATE_CHANGE_TIME                 java.sql.Types.TIMESTAMP TIMESTAMP         SAMPLE.-none-
//	RS> 33   DATA_SHARING_REMOTE_LOCKWAIT_COUNT             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 34   DATA_SHARING_REMOTE_LOCKWAIT_TIME              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 35   DIRECT_WRITES                                  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 36   DIRECT_WRITE_REQS                              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 37   DIRECT_READS                                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 38   DIRECT_READ_REQS                               java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 39   OBJECT_DATA_L_READS                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 40   OBJECT_DATA_P_READS                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 41   OBJECT_DATA_GBP_L_READS                        java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 42   OBJECT_DATA_GBP_P_READS                        java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 43   OBJECT_DATA_GBP_INVALID_PAGES                  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 44   OBJECT_DATA_LBP_PAGES_FOUND                    java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 45   OBJECT_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP       java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 46   OBJECT_XDA_L_READS                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 47   OBJECT_XDA_P_READS                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 48   OBJECT_XDA_GBP_L_READS                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 49   OBJECT_XDA_GBP_P_READS                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 50   OBJECT_XDA_GBP_INVALID_PAGES                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 51   OBJECT_XDA_LBP_PAGES_FOUND                     java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 52   OBJECT_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP        java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 53   NUM_PAGE_DICT_BUILT                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 54   STATS_ROWS_MODIFIED                            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 55   RTS_ROWS_MODIFIED                              java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 56   COL_OBJECT_L_PAGES                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 57   TAB_ORGANIZATION                               java.sql.Types.CHAR      CHAR(1)           SAMPLE.-none-
//	RS> 58   OBJECT_COL_L_READS                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 59   OBJECT_COL_P_READS                             java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 60   OBJECT_COL_GBP_L_READS                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 61   OBJECT_COL_GBP_P_READS                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 62   OBJECT_COL_GBP_INVALID_PAGES                   java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 63   OBJECT_COL_LBP_PAGES_FOUND                     java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 64   OBJECT_COL_GBP_INDEP_PAGES_FOUND_IN_LBP        java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 65   NUM_COLUMNS_REFERENCED                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 66   SECTION_EXEC_WITH_COL_REFERENCES               java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 67   OBJECT_DATA_CACHING_TIER_L_READS               java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 68   OBJECT_DATA_CACHING_TIER_PAGES_FOUND           java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 69   OBJECT_DATA_CACHING_TIER_GBP_INVALID_PAGES     java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 70   OBJECT_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 71   OBJECT_XDA_CACHING_TIER_L_READS                java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 72   OBJECT_XDA_CACHING_TIER_PAGES_FOUND            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 73   OBJECT_XDA_CACHING_TIER_GBP_INVALID_PAGES      java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 74   OBJECT_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 75   OBJECT_COL_CACHING_TIER_L_READS                java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 76   OBJECT_COL_CACHING_TIER_PAGES_FOUND            java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 77   OBJECT_COL_CACHING_TIER_GBP_INVALID_PAGES      java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 78   OBJECT_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND  java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 79   EXT_TABLE_RECV_WAIT_TIME                       java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 80   EXT_TABLE_RECVS_TOTAL                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 81   EXT_TABLE_RECV_VOLUME                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 82   EXT_TABLE_READ_VOLUME                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 83   EXT_TABLE_SEND_WAIT_TIME                       java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 84   EXT_TABLE_SENDS_TOTAL                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 85   EXT_TABLE_SEND_VOLUME                          java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-
//	RS> 86   EXT_TABLE_WRITE_VOLUME                         java.sql.Types.BIGINT    BIGINT            SAMPLE.-none-	
	
	
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

		return new CmMonGetTable(counterController, guiController);
	}

	public CmMonGetTable(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("MON_GET_TABLE",  "fixme.");

			mtd.addColumn("MON_GET_TABLE", "xxx",    "<html>xxx</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TABSCHEMA");
		pkCols.add("TABNAME");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * FROM TABLE(MON_GET_TABLE('','',-2))";

		return sql;
	}
}
