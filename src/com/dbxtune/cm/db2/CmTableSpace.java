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
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

public class CmTableSpace
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Space Size";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Table Space Activity</h4>" + 
		"Fixme." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLESPACE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			/* RS> Col# Label                                                      JDBC Type Name           Guessed DBMS type Source Table  */
			/* RS> ---- ---------------------------------------------------        ------------------------ ----------------- ------------- */
//			/* RS> 1    */ "TBSP_NAME",                                           /* java.sql.Types.VARCHAR   VARCHAR(128)      SAMPLE.-none- */
//			/* RS> 2    */ "TBSP_ID",                                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 3    */ "MEMBER",                                              /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 4    */ "TBSP_TYPE",                                           /* java.sql.Types.VARCHAR   VARCHAR(10)       SAMPLE.-none- */
//			/* RS> 5    */ "TBSP_CONTENT_TYPE",                                   /* java.sql.Types.VARCHAR   VARCHAR(10)       SAMPLE.-none- */
//			/* RS> 6    */ "TBSP_PAGE_SIZE",                                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 7    */ "TBSP_EXTENT_SIZE",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 8    */ "TBSP_PREFETCH_SIZE",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 9    */ "TBSP_CUR_POOL_ID",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 10   */ "TBSP_NEXT_POOL_ID",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 11   */ "FS_CACHING",                                          /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 12   */ "TBSP_REBALANCER_MODE",                                /* java.sql.Types.VARCHAR   VARCHAR(30)       SAMPLE.-none- */
//			/* RS> 13   */ "TBSP_USING_AUTO_STORAGE",                             /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 14   */ "TBSP_AUTO_RESIZE_ENABLED",                            /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
			/* RS> 15   */ "DIRECT_READS",                                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 16   */ "DIRECT_READ_REQS",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 17   */ "DIRECT_WRITES",                                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 18   */ "DIRECT_WRITE_REQS",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 19   */ "POOL_DATA_L_READS",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 20   */ "POOL_TEMP_DATA_L_READS",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 21   */ "POOL_XDA_L_READS",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 22   */ "POOL_TEMP_XDA_L_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 23   */ "POOL_INDEX_L_READS",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 24   */ "POOL_TEMP_INDEX_L_READS",                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 25   */ "POOL_DATA_P_READS",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 26   */ "POOL_TEMP_DATA_P_READS",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 27   */ "POOL_XDA_P_READS",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 28   */ "POOL_TEMP_XDA_P_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 29   */ "POOL_INDEX_P_READS",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 30   */ "POOL_TEMP_INDEX_P_READS",                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 31   */ "POOL_DATA_WRITES",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 32   */ "POOL_XDA_WRITES",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 33   */ "POOL_INDEX_WRITES",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 34   */ "DIRECT_READ_TIME",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 35   */ "DIRECT_WRITE_TIME",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 36   */ "POOL_READ_TIME",                                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 37   */ "POOL_WRITE_TIME",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 38   */ "POOL_ASYNC_DATA_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 39   */ "POOL_ASYNC_DATA_READ_REQS",                           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 40   */ "POOL_ASYNC_DATA_WRITES",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 41   */ "POOL_ASYNC_INDEX_READS",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 42   */ "POOL_ASYNC_INDEX_READ_REQS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 43   */ "POOL_ASYNC_INDEX_WRITES",                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 44   */ "POOL_ASYNC_XDA_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 45   */ "POOL_ASYNC_XDA_READ_REQS",                            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 46   */ "POOL_ASYNC_XDA_WRITES",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 47   */ "VECTORED_IOS",                                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 48   */ "PAGES_FROM_VECTORED_IOS",                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 49   */ "BLOCK_IOS",                                           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 50   */ "PAGES_FROM_BLOCK_IOS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 51   */ "UNREAD_PREFETCH_PAGES",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 52   */ "FILES_CLOSED",                                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 53   */ "TBSP_STATE",                                          /* java.sql.Types.VARCHAR   VARCHAR(256)      SAMPLE.-none- */
			/* RS> 54   */ "TBSP_USED_PAGES",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 55   */ "TBSP_FREE_PAGES",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 56   */ "TBSP_USABLE_PAGES",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 57   */ "TBSP_TOTAL_PAGES",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 58   */ "TBSP_PENDING_FREE_PAGES",                             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 59   */ "TBSP_PAGE_TOP",                                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 60   */ "TBSP_MAX_PAGE_TOP",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 61   */ "RECLAIMABLE_SPACE_ENABLED",                           /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 62   */ "AUTO_STORAGE_HYBRID",                                 /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 63   */ "TBSP_PATHS_DROPPED",                                  /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
			/* RS> 64   */ "POOL_DATA_GBP_L_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 65   */ "POOL_DATA_GBP_P_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 66   */ "POOL_DATA_LBP_PAGES_FOUND",                           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 67   */ "POOL_DATA_GBP_INVALID_PAGES",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 68   */ "POOL_INDEX_GBP_L_READS",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 69   */ "POOL_INDEX_GBP_P_READS",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 70   */ "POOL_INDEX_LBP_PAGES_FOUND",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 71   */ "POOL_INDEX_GBP_INVALID_PAGES",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 72   */ "POOL_ASYNC_DATA_GBP_L_READS",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 73   */ "POOL_ASYNC_DATA_GBP_P_READS",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 74   */ "POOL_ASYNC_DATA_LBP_PAGES_FOUND",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 75   */ "POOL_ASYNC_DATA_GBP_INVALID_PAGES",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 76   */ "POOL_ASYNC_INDEX_GBP_L_READS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 77   */ "POOL_ASYNC_INDEX_GBP_P_READS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 78   */ "POOL_ASYNC_INDEX_LBP_PAGES_FOUND",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 79   */ "POOL_ASYNC_INDEX_GBP_INVALID_PAGES",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 80   */ "TABLESPACE_MIN_RECOVERY_TIME",                        /* java.sql.Types.TIMESTAMP TIMESTAMP         SAMPLE.-none- */
//			/* RS> 81   */ "DBPARTITIONNUM",                                      /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
			/* RS> 82   */ "POOL_XDA_GBP_L_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 83   */ "POOL_XDA_GBP_P_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 84   */ "POOL_XDA_LBP_PAGES_FOUND",                            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 85   */ "POOL_XDA_GBP_INVALID_PAGES",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 86   */ "POOL_ASYNC_XDA_GBP_L_READS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 87   */ "POOL_ASYNC_XDA_GBP_P_READS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 88   */ "POOL_ASYNC_XDA_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 89   */ "POOL_ASYNC_XDA_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 90   */ "POOL_ASYNC_READ_TIME",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 91   */ "POOL_ASYNC_WRITE_TIME",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 92   */ "TBSP_TRACKMOD_STATE",                                 /* java.sql.Types.VARCHAR   VARCHAR(32)       SAMPLE.-none- */
//			/* RS> 93   */ "STORAGE_GROUP_NAME",                                  /* java.sql.Types.VARCHAR   VARCHAR(128)      SAMPLE.-none- */
//			/* RS> 94   */ "STORAGE_GROUP_ID",                                    /* java.sql.Types.INTEGER   INTEGER           SAMPLE.-none- */
//			/* RS> 95   */ "TBSP_DATATAG",                                        /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
			/* RS> 96   */ "TBSP_LAST_CONSEC_PAGE",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 97   */ "POOL_QUEUED_ASYNC_DATA_REQS",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 98   */ "POOL_QUEUED_ASYNC_INDEX_REQS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 99   */ "POOL_QUEUED_ASYNC_XDA_REQS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 100  */ "POOL_QUEUED_ASYNC_TEMP_DATA_REQS",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 101  */ "POOL_QUEUED_ASYNC_TEMP_INDEX_REQS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 102  */ "POOL_QUEUED_ASYNC_TEMP_XDA_REQS",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 103  */ "POOL_QUEUED_ASYNC_OTHER_REQS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 104  */ "POOL_QUEUED_ASYNC_DATA_PAGES",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 105  */ "POOL_QUEUED_ASYNC_INDEX_PAGES",                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 106  */ "POOL_QUEUED_ASYNC_XDA_PAGES",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 107  */ "POOL_QUEUED_ASYNC_TEMP_DATA_PAGES",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 108  */ "POOL_QUEUED_ASYNC_TEMP_INDEX_PAGES",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 109  */ "POOL_QUEUED_ASYNC_TEMP_XDA_PAGES",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 110  */ "POOL_FAILED_ASYNC_DATA_REQS",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 111  */ "POOL_FAILED_ASYNC_INDEX_REQS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 112  */ "POOL_FAILED_ASYNC_XDA_REQS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 113  */ "POOL_FAILED_ASYNC_TEMP_DATA_REQS",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 114  */ "POOL_FAILED_ASYNC_TEMP_INDEX_REQS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 115  */ "POOL_FAILED_ASYNC_TEMP_XDA_REQS",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 116  */ "POOL_FAILED_ASYNC_OTHER_REQS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 117  */ "SKIPPED_PREFETCH_DATA_P_READS",                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 118  */ "SKIPPED_PREFETCH_INDEX_P_READS",                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 119  */ "SKIPPED_PREFETCH_XDA_P_READS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 120  */ "SKIPPED_PREFETCH_TEMP_DATA_P_READS",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 121  */ "SKIPPED_PREFETCH_TEMP_INDEX_P_READS",                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 122  */ "SKIPPED_PREFETCH_TEMP_XDA_P_READS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 123  */ "SKIPPED_PREFETCH_UOW_DATA_P_READS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 124  */ "SKIPPED_PREFETCH_UOW_INDEX_P_READS",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 125  */ "SKIPPED_PREFETCH_UOW_XDA_P_READS",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 126  */ "SKIPPED_PREFETCH_UOW_TEMP_DATA_P_READS",              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 127  */ "SKIPPED_PREFETCH_UOW_TEMP_INDEX_P_READS",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 128  */ "SKIPPED_PREFETCH_UOW_TEMP_XDA_P_READS",               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 129  */ "PREFETCH_WAIT_TIME",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 130  */ "PREFETCH_WAITS",                                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 131  */ "POOL_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 132  */ "POOL_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 133  */ "POOL_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 134  */ "POOL_ASYNC_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 135  */ "POOL_ASYNC_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 136  */ "POOL_ASYNC_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 137  */ "TBSP_NUM_CONTAINERS",                                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 138  */ "TBSP_INITIAL_SIZE",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 139  */ "TBSP_MAX_SIZE",                                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 140  */ "TBSP_INCREASE_SIZE",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 141  */ "TBSP_INCREASE_SIZE_PERCENT",                          /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
//			/* RS> 142  */ "TBSP_LAST_RESIZE_TIME",                               /* java.sql.Types.TIMESTAMP TIMESTAMP         SAMPLE.-none- */
//			/* RS> 143  */ "TBSP_LAST_RESIZE_FAILED",                             /* java.sql.Types.SMALLINT  SMALLINT          SAMPLE.-none- */
			/* RS> 144  */ "POOL_COL_L_READS",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 145  */ "POOL_TEMP_COL_L_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 146  */ "POOL_COL_P_READS",                                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 147  */ "POOL_TEMP_COL_P_READS",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 148  */ "POOL_COL_LBP_PAGES_FOUND",                            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 149  */ "POOL_COL_WRITES",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 150  */ "POOL_ASYNC_COL_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 151  */ "POOL_ASYNC_COL_READ_REQS",                            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 152  */ "POOL_ASYNC_COL_WRITES",                               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 153  */ "POOL_ASYNC_COL_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 154  */ "POOL_COL_GBP_L_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 155  */ "POOL_COL_GBP_P_READS",                                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 156  */ "POOL_COL_GBP_INVALID_PAGES",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 157  */ "POOL_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 158  */ "POOL_ASYNC_COL_GBP_L_READS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 159  */ "POOL_ASYNC_COL_GBP_P_READS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 160  */ "POOL_ASYNC_COL_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 161  */ "POOL_ASYNC_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 162  */ "POOL_QUEUED_ASYNC_COL_REQS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 163  */ "POOL_QUEUED_ASYNC_TEMP_COL_REQS",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 164  */ "POOL_QUEUED_ASYNC_COL_PAGES",                         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 165  */ "POOL_QUEUED_ASYNC_TEMP_COL_PAGES",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 166  */ "POOL_FAILED_ASYNC_COL_REQS",                          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 167  */ "POOL_FAILED_ASYNC_TEMP_COL_REQS",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 168  */ "SKIPPED_PREFETCH_COL_P_READS",                        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 169  */ "SKIPPED_PREFETCH_TEMP_COL_P_READS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 170  */ "SKIPPED_PREFETCH_UOW_COL_P_READS",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 171  */ "SKIPPED_PREFETCH_UOW_TEMP_COL_P_READS",               /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 172  */ "TBSP_NUM_QUIESCERS",                                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 173  */ "TBSP_NUM_RANGES",                                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
//			/* RS> 174  */ "CACHING_TIER",                                        /* java.sql.Types.VARCHAR   VARCHAR(8)        SAMPLE.-none- */
			/* RS> 175  */ "POOL_DATA_CACHING_TIER_L_READS",                      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 176  */ "POOL_INDEX_CACHING_TIER_L_READS",                     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 177  */ "POOL_XDA_CACHING_TIER_L_READS",                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 178  */ "POOL_COL_CACHING_TIER_L_READS",                       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 179  */ "POOL_DATA_CACHING_TIER_PAGE_WRITES",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 180  */ "POOL_INDEX_CACHING_TIER_PAGE_WRITES",                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 181  */ "POOL_XDA_CACHING_TIER_PAGE_WRITES",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 182  */ "POOL_COL_CACHING_TIER_PAGE_WRITES",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 183  */ "POOL_DATA_CACHING_TIER_PAGE_UPDATES",                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 184  */ "POOL_INDEX_CACHING_TIER_PAGE_UPDATES",                /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 185  */ "POOL_XDA_CACHING_TIER_PAGE_UPDATES",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 186  */ "POOL_COL_CACHING_TIER_PAGE_UPDATES",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 187  */ "POOL_CACHING_TIER_PAGE_READ_TIME",                    /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 188  */ "POOL_CACHING_TIER_PAGE_WRITE_TIME",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 189  */ "POOL_DATA_CACHING_TIER_PAGES_FOUND",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 190  */ "POOL_INDEX_CACHING_TIER_PAGES_FOUND",                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 191  */ "POOL_XDA_CACHING_TIER_PAGES_FOUND",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 192  */ "POOL_COL_CACHING_TIER_PAGES_FOUND",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 193  */ "POOL_DATA_CACHING_TIER_GBP_INVALID_PAGES",            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 194  */ "POOL_INDEX_CACHING_TIER_GBP_INVALID_PAGES",           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 195  */ "POOL_XDA_CACHING_TIER_GBP_INVALID_PAGES",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 196  */ "POOL_COL_CACHING_TIER_GBP_INVALID_PAGES",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 197  */ "POOL_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",        /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 198  */ "POOL_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND",       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 199  */ "POOL_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 200  */ "POOL_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",         /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 201  */ "POOL_ASYNC_DATA_CACHING_TIER_READS",                  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 202  */ "POOL_ASYNC_INDEX_CACHING_TIER_READS",                 /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 203  */ "POOL_ASYNC_XDA_CACHING_TIER_READS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 204  */ "POOL_ASYNC_COL_CACHING_TIER_READS",                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 205  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGE_WRITES",            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 206  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGE_WRITES",           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 207  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 208  */ "POOL_ASYNC_COL_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 209  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGE_UPDATES",           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 210  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGE_UPDATES",          /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 211  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 212  */ "POOL_ASYNC_COL_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 213  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGES_FOUND",            /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 214  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGES_FOUND",           /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 215  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 216  */ "POOL_ASYNC_COL_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 217  */ "POOL_ASYNC_DATA_CACHING_TIER_GBP_INVALID_PAGES",      /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 218  */ "POOL_ASYNC_INDEX_CACHING_TIER_GBP_INVALID_PAGES",     /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 219  */ "POOL_ASYNC_XDA_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 220  */ "POOL_ASYNC_COL_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 221  */ "POOL_ASYNC_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",  /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 222  */ "POOL_ASYNC_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND", /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 223  */ "POOL_ASYNC_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 224  */ "POOL_ASYNC_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 225  */ "LOB_PREFETCH_WAIT_TIME",                              /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
			/* RS> 226  */ "LOB_PREFETCH_REQS",                                   /* java.sql.Types.BIGINT    BIGINT            SAMPLE.-none- */
	};
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmTableSpace(counterController, guiController);
	}

	public CmTableSpace(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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
			mtd.addTable("MON_GET_TABLESPACE",  "fixme.");

			mtd.addColumn("MON_GET_TABLESPACE", "xxx",    "<html>xxx</html>");
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

		pkCols.add("TBSP_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		String sql = "SELECT * FROM TABLE(MON_GET_TABLESPACE('',-2))";

		String sql = ""
			    + "SELECT \n"
			    + "	 TBSP_NAME \n"
			    + "	,TBSP_ID \n"
			    + "	,MEMBER \n"
			    + "	,TBSP_TYPE \n"
			    + "	,TBSP_CONTENT_TYPE \n"
			    + "	,TBSP_USING_AUTO_STORAGE \n"
			    + "	,TBSP_AUTO_RESIZE_ENABLED \n"
			    + "	,TBSP_STATE \n"
			    + "	,TBSP_USED_PAGES \n"
			    + "	,TBSP_FREE_PAGES \n"
			    + "	,TBSP_USABLE_PAGES \n"
			    + "	,TBSP_TOTAL_PAGES \n"
			    + "	,RECLAIMABLE_SPACE_ENABLED \n"
			    + "FROM TABLE(MON_GET_TABLESPACE('',-2)) \n"
			    + "ORDER BY TBSP_FREE_PAGES ASC \n"
			    + "";

		return sql;
	}
}
