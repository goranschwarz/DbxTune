/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.db2;

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
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

public class CmMonGetBufferPool
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmMonGetTable.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetBufferPool.class.getSimpleName();
	public static final String   SHORT_NAME       = "BufferPool";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Buffer Pool Activity</h4>" + 
		"Fixme." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_BUFFERPOOL"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		/* RS> Col# Label                                                        JDBC Type Name          Guessed DBMS type Source Table    */
		/* RS> ---- -------------------------------------------------------      ----------------------- ----------------- -------------   */
//		/* RS> 1    */ "BP_NAME",                                             /* java.sql.Types.VARCHAR  VARCHAR(128)      SAMPLE.-none-   */
//		/* RS> 2    */ "MEMBER",                                              /* java.sql.Types.SMALLINT SMALLINT          SAMPLE.-none-   */
//		/* RS> 3    */ "AUTOMATIC",                                           /* java.sql.Types.SMALLINT SMALLINT          SAMPLE.-none-   */
		/* RS> 4    */ "DIRECT_READS",                                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 5    */ "DIRECT_READ_REQS",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 6    */ "DIRECT_WRITES",                                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 7    */ "DIRECT_WRITE_REQS",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 8    */ "POOL_DATA_L_READS",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 9    */ "POOL_TEMP_DATA_L_READS",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 10   */ "POOL_XDA_L_READS",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 11   */ "POOL_TEMP_XDA_L_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 12   */ "POOL_INDEX_L_READS",                                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 13   */ "POOL_TEMP_INDEX_L_READS",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 14   */ "POOL_DATA_P_READS",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 15   */ "POOL_TEMP_DATA_P_READS",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 16   */ "POOL_XDA_P_READS",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 17   */ "POOL_TEMP_XDA_P_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 18   */ "POOL_INDEX_P_READS",                                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 19   */ "POOL_TEMP_INDEX_P_READS",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 20   */ "POOL_DATA_WRITES",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 21   */ "POOL_XDA_WRITES",                                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 22   */ "POOL_INDEX_WRITES",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 23   */ "DIRECT_READ_TIME",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 24   */ "DIRECT_WRITE_TIME",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 25   */ "POOL_READ_TIME",                                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 26   */ "POOL_WRITE_TIME",                                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 27   */ "POOL_ASYNC_DATA_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 28   */ "POOL_ASYNC_DATA_READ_REQS",                           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 29   */ "POOL_ASYNC_DATA_WRITES",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 30   */ "POOL_ASYNC_INDEX_READS",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 31   */ "POOL_ASYNC_INDEX_READ_REQS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 32   */ "POOL_ASYNC_INDEX_WRITES",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 33   */ "POOL_ASYNC_XDA_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 34   */ "POOL_ASYNC_XDA_READ_REQS",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 35   */ "POOL_ASYNC_XDA_WRITES",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 36   */ "POOL_NO_VICTIM_BUFFER",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 37   */ "POOL_LSN_GAP_CLNS",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 38   */ "POOL_DRTY_PG_STEAL_CLNS",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 39   */ "POOL_DRTY_PG_THRSH_CLNS",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 40   */ "VECTORED_IOS",                                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 41   */ "PAGES_FROM_VECTORED_IOS",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 42   */ "BLOCK_IOS",                                           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 43   */ "PAGES_FROM_BLOCK_IOS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 44   */ "UNREAD_PREFETCH_PAGES",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 45   */ "FILES_CLOSED",                                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 46   */ "POOL_DATA_GBP_L_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 47   */ "POOL_DATA_GBP_P_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 48   */ "POOL_DATA_LBP_PAGES_FOUND",                           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 49   */ "POOL_DATA_GBP_INVALID_PAGES",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 50   */ "POOL_INDEX_GBP_L_READS",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 51   */ "POOL_INDEX_GBP_P_READS",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 52   */ "POOL_INDEX_LBP_PAGES_FOUND",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 53   */ "POOL_INDEX_GBP_INVALID_PAGES",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 54   */ "POOL_ASYNC_DATA_GBP_L_READS",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 55   */ "POOL_ASYNC_DATA_GBP_P_READS",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 56   */ "POOL_ASYNC_DATA_LBP_PAGES_FOUND",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 57   */ "POOL_ASYNC_DATA_GBP_INVALID_PAGES",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 58   */ "POOL_ASYNC_INDEX_GBP_L_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 59   */ "POOL_ASYNC_INDEX_GBP_P_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 60   */ "POOL_ASYNC_INDEX_LBP_PAGES_FOUND",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 61   */ "POOL_ASYNC_INDEX_GBP_INVALID_PAGES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 62   */ "POOL_XDA_GBP_L_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 63   */ "POOL_XDA_GBP_P_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 64   */ "POOL_XDA_LBP_PAGES_FOUND",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 65   */ "POOL_XDA_GBP_INVALID_PAGES",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 66   */ "POOL_ASYNC_XDA_GBP_L_READS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 67   */ "POOL_ASYNC_XDA_GBP_P_READS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 68   */ "POOL_ASYNC_XDA_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 69   */ "POOL_ASYNC_XDA_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 70   */ "POOL_ASYNC_READ_TIME",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 71   */ "POOL_ASYNC_WRITE_TIME",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 72   */ "BP_CUR_BUFFSZ",                                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 73   */ "POOL_QUEUED_ASYNC_DATA_REQS",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 74   */ "POOL_QUEUED_ASYNC_INDEX_REQS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 75   */ "POOL_QUEUED_ASYNC_XDA_REQS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 76   */ "POOL_QUEUED_ASYNC_TEMP_DATA_REQS",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 77   */ "POOL_QUEUED_ASYNC_TEMP_INDEX_REQS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 78   */ "POOL_QUEUED_ASYNC_TEMP_XDA_REQS",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 79   */ "POOL_QUEUED_ASYNC_OTHER_REQS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 80   */ "POOL_QUEUED_ASYNC_DATA_PAGES",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 81   */ "POOL_QUEUED_ASYNC_INDEX_PAGES",                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 82   */ "POOL_QUEUED_ASYNC_XDA_PAGES",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 83   */ "POOL_QUEUED_ASYNC_TEMP_DATA_PAGES",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 84   */ "POOL_QUEUED_ASYNC_TEMP_INDEX_PAGES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 85   */ "POOL_QUEUED_ASYNC_TEMP_XDA_PAGES",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 86   */ "POOL_FAILED_ASYNC_DATA_REQS",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 87   */ "POOL_FAILED_ASYNC_INDEX_REQS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 88   */ "POOL_FAILED_ASYNC_XDA_REQS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 89   */ "POOL_FAILED_ASYNC_TEMP_DATA_REQS",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 90   */ "POOL_FAILED_ASYNC_TEMP_INDEX_REQS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 91   */ "POOL_FAILED_ASYNC_TEMP_XDA_REQS",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 92   */ "POOL_FAILED_ASYNC_OTHER_REQS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 93   */ "SKIPPED_PREFETCH_DATA_P_READS",                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 94   */ "SKIPPED_PREFETCH_INDEX_P_READS",                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 95   */ "SKIPPED_PREFETCH_XDA_P_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 96   */ "SKIPPED_PREFETCH_TEMP_DATA_P_READS",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 97   */ "SKIPPED_PREFETCH_TEMP_INDEX_P_READS",                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 98   */ "SKIPPED_PREFETCH_TEMP_XDA_P_READS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 99   */ "SKIPPED_PREFETCH_UOW_DATA_P_READS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 100  */ "SKIPPED_PREFETCH_UOW_INDEX_P_READS",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 101  */ "SKIPPED_PREFETCH_UOW_XDA_P_READS",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 102  */ "SKIPPED_PREFETCH_UOW_TEMP_DATA_P_READS",              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 103  */ "SKIPPED_PREFETCH_UOW_TEMP_INDEX_P_READS",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 104  */ "SKIPPED_PREFETCH_UOW_TEMP_XDA_P_READS",               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 105  */ "PREFETCH_WAIT_TIME",                                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 106  */ "PREFETCH_WAITS",                                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 107  */ "POOL_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 108  */ "POOL_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 109  */ "POOL_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 110  */ "POOL_ASYNC_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 111  */ "POOL_ASYNC_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 112  */ "POOL_ASYNC_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 113  */ "POOL_COL_L_READS",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 114  */ "POOL_TEMP_COL_L_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 115  */ "POOL_COL_P_READS",                                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 116  */ "POOL_TEMP_COL_P_READS",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 117  */ "POOL_COL_LBP_PAGES_FOUND",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 118  */ "POOL_COL_WRITES",                                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 119  */ "POOL_ASYNC_COL_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 120  */ "POOL_ASYNC_COL_READ_REQS",                            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 121  */ "POOL_ASYNC_COL_WRITES",                               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 122  */ "POOL_ASYNC_COL_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 123  */ "POOL_COL_GBP_L_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 124  */ "POOL_COL_GBP_P_READS",                                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 125  */ "POOL_COL_GBP_INVALID_PAGES",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 126  */ "POOL_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 127  */ "POOL_ASYNC_COL_GBP_L_READS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 128  */ "POOL_ASYNC_COL_GBP_P_READS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 129  */ "POOL_ASYNC_COL_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 130  */ "POOL_ASYNC_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 131  */ "POOL_QUEUED_ASYNC_COL_REQS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 132  */ "POOL_QUEUED_ASYNC_TEMP_COL_REQS",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 133  */ "POOL_QUEUED_ASYNC_COL_PAGES",                         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 134  */ "POOL_QUEUED_ASYNC_TEMP_COL_PAGES",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 135  */ "POOL_FAILED_ASYNC_COL_REQS",                          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 136  */ "POOL_FAILED_ASYNC_TEMP_COL_REQS",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 137  */ "SKIPPED_PREFETCH_COL_P_READS",                        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 138  */ "SKIPPED_PREFETCH_TEMP_COL_P_READS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 139  */ "SKIPPED_PREFETCH_UOW_COL_P_READS",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 140  */ "SKIPPED_PREFETCH_UOW_TEMP_COL_P_READS",               /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 141  */ "BP_PAGES_LEFT_TO_REMOVE",                             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 142  */ "BP_TBSP_USE_COUNT",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 143  */ "POOL_DATA_CACHING_TIER_L_READS",                      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 144  */ "POOL_INDEX_CACHING_TIER_L_READS",                     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 145  */ "POOL_XDA_CACHING_TIER_L_READS",                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 146  */ "POOL_COL_CACHING_TIER_L_READS",                       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 147  */ "POOL_DATA_CACHING_TIER_PAGE_WRITES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 148  */ "POOL_INDEX_CACHING_TIER_PAGE_WRITES",                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 149  */ "POOL_XDA_CACHING_TIER_PAGE_WRITES",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 150  */ "POOL_COL_CACHING_TIER_PAGE_WRITES",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 151  */ "POOL_DATA_CACHING_TIER_PAGE_UPDATES",                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 152  */ "POOL_INDEX_CACHING_TIER_PAGE_UPDATES",                /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 153  */ "POOL_XDA_CACHING_TIER_PAGE_UPDATES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 154  */ "POOL_COL_CACHING_TIER_PAGE_UPDATES",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 155  */ "POOL_CACHING_TIER_PAGE_READ_TIME",                    /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 156  */ "POOL_CACHING_TIER_PAGE_WRITE_TIME",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 157  */ "POOL_DATA_CACHING_TIER_PAGES_FOUND",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 158  */ "POOL_INDEX_CACHING_TIER_PAGES_FOUND",                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 159  */ "POOL_XDA_CACHING_TIER_PAGES_FOUND",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 160  */ "POOL_COL_CACHING_TIER_PAGES_FOUND",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 161  */ "POOL_DATA_CACHING_TIER_GBP_INVALID_PAGES",            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 162  */ "POOL_INDEX_CACHING_TIER_GBP_INVALID_PAGES",           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 163  */ "POOL_XDA_CACHING_TIER_GBP_INVALID_PAGES",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 164  */ "POOL_COL_CACHING_TIER_GBP_INVALID_PAGES",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 165  */ "POOL_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",        /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 166  */ "POOL_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND",       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 167  */ "POOL_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 168  */ "POOL_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",         /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 169  */ "POOL_ASYNC_DATA_CACHING_TIER_READS",                  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 170  */ "POOL_ASYNC_INDEX_CACHING_TIER_READS",                 /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 171  */ "POOL_ASYNC_XDA_CACHING_TIER_READS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 172  */ "POOL_ASYNC_COL_CACHING_TIER_READS",                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 173  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGE_WRITES",            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 174  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGE_WRITES",           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 175  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 176  */ "POOL_ASYNC_COL_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 177  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGE_UPDATES",           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 178  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGE_UPDATES",          /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 179  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 180  */ "POOL_ASYNC_COL_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 181  */ "POOL_ASYNC_DATA_CACHING_TIER_PAGES_FOUND",            /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 182  */ "POOL_ASYNC_INDEX_CACHING_TIER_PAGES_FOUND",           /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 183  */ "POOL_ASYNC_XDA_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 184  */ "POOL_ASYNC_COL_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 185  */ "POOL_ASYNC_DATA_CACHING_TIER_GBP_INVALID_PAGES",      /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 186  */ "POOL_ASYNC_INDEX_CACHING_TIER_GBP_INVALID_PAGES",     /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 187  */ "POOL_ASYNC_XDA_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 188  */ "POOL_ASYNC_COL_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 189  */ "POOL_ASYNC_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",  /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 190  */ "POOL_ASYNC_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND", /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 191  */ "POOL_ASYNC_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 192  */ "POOL_ASYNC_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 193  */ "LOB_PREFETCH_WAIT_TIME",                              /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
		/* RS> 194  */ "LOB_PREFETCH_REQS",                                   /* java.sql.Types.BIGINT   BIGINT            SAMPLE.-none-   */
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

		return new CmMonGetBufferPool(counterController, guiController);
	}

	public CmMonGetBufferPool(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("MON_GET_BUFFERPOOL",  "fixme.");

			mtd.addColumn("MON_GET_BUFFERPOOL", "xxx",    "<html>xxx</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("BP_NAME");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from table(SYSPROC.MON_GET_BUFFERPOOL(null, null))";

		return sql;
	}
}
