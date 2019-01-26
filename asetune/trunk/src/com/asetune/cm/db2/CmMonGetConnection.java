/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

public class CmMonGetConnection
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmMonGetTable.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetConnection.class.getSimpleName();
	public static final String   SHORT_NAME       = "Connection Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Connection Activity</h4>" + 
		"Fixme." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_CONNECTION"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		/* RS> Col# Label                                                  JDBC Type Name           Guessed DBMS type        Source Table  */ 
		/* RS> ---- ---------------------------------------------          ------------------------ ------------------------ ------------- */
//		/* RS> 1    */ "APPLICATION_HANDLE",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 2    */ "APPLICATION_NAME",                              /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
//		/* RS> 3    */ "APPLICATION_ID",                                /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
//		/* RS> 4    */ "MEMBER",                                        /* java.sql.Types.SMALLINT  SMALLINT                 SAMPLE.-none- */
//		/* RS> 5    */ "CLIENT_WRKSTNNAME",                             /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
//		/* RS> 6    */ "CLIENT_ACCTNG",                                 /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
//		/* RS> 7    */ "CLIENT_USERID",                                 /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
//		/* RS> 8    */ "CLIENT_APPLNAME",                               /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
//		/* RS> 9    */ "CLIENT_PID",                                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 10   */ "CLIENT_PRDID",                                  /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
//		/* RS> 11   */ "CLIENT_PLATFORM",                               /* java.sql.Types.VARCHAR   VARCHAR(12)              SAMPLE.-none- */
//		/* RS> 12   */ "CLIENT_PROTOCOL",                               /* java.sql.Types.VARCHAR   VARCHAR(10)              SAMPLE.-none- */
//		/* RS> 13   */ "SYSTEM_AUTH_ID",                                /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
//		/* RS> 14   */ "SESSION_AUTH_ID",                               /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
//		/* RS> 15   */ "COORD_MEMBER",                                  /* java.sql.Types.SMALLINT  SMALLINT                 SAMPLE.-none- */
//		/* RS> 16   */ "CONNECTION_START_TIME",                         /* java.sql.Types.TIMESTAMP TIMESTAMP                SAMPLE.-none- */
		/* RS> 17   */ "ACT_ABORTED_TOTAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 18   */ "ACT_COMPLETED_TOTAL",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 19   */ "ACT_REJECTED_TOTAL",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 20   */ "AGENT_WAIT_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 21   */ "AGENT_WAITS_TOTAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 22   */ "POOL_DATA_L_READS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 23   */ "POOL_INDEX_L_READS",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 24   */ "POOL_TEMP_DATA_L_READS",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 25   */ "POOL_TEMP_INDEX_L_READS",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 26   */ "POOL_TEMP_XDA_L_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 27   */ "POOL_XDA_L_READS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 28   */ "POOL_DATA_P_READS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 29   */ "POOL_INDEX_P_READS",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 30   */ "POOL_TEMP_DATA_P_READS",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 31   */ "POOL_TEMP_INDEX_P_READS",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 32   */ "POOL_TEMP_XDA_P_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 33   */ "POOL_XDA_P_READS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 34   */ "POOL_DATA_WRITES",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 35   */ "POOL_INDEX_WRITES",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 36   */ "POOL_XDA_WRITES",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 37   */ "POOL_READ_TIME",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 38   */ "POOL_WRITE_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 39   */ "CLIENT_IDLE_WAIT_TIME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 40   */ "DEADLOCKS",                                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 41   */ "DIRECT_READS",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 42   */ "DIRECT_READ_TIME",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 43   */ "DIRECT_WRITES",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 44   */ "DIRECT_WRITE_TIME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 45   */ "DIRECT_READ_REQS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 46   */ "DIRECT_WRITE_REQS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 47   */ "FCM_RECV_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 48   */ "FCM_RECVS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 49   */ "FCM_SEND_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 50   */ "FCM_SENDS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 51   */ "FCM_RECV_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 52   */ "FCM_SEND_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 53   */ "IPC_RECV_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 54   */ "IPC_RECV_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 55   */ "IPC_RECVS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 56   */ "IPC_SEND_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 57   */ "IPC_SEND_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 58   */ "IPC_SENDS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 59   */ "LOCK_ESCALS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 60   */ "LOCK_TIMEOUTS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 61   */ "LOCK_WAIT_TIME",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 62   */ "LOCK_WAITS",                                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 63   */ "LOG_BUFFER_WAIT_TIME",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 64   */ "NUM_LOG_BUFFER_FULL",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 65   */ "LOG_DISK_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 66   */ "LOG_DISK_WAITS_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 67   */ "NUM_LOCKS_HELD",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 68   */ "RQSTS_COMPLETED_TOTAL",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 69   */ "ROWS_MODIFIED",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 70   */ "ROWS_READ",                                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 71   */ "ROWS_RETURNED",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 72   */ "TCPIP_RECV_VOLUME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 73   */ "TCPIP_SEND_VOLUME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 74   */ "TCPIP_RECV_WAIT_TIME",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 75   */ "TCPIP_RECVS_TOTAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 76   */ "TCPIP_SEND_WAIT_TIME",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 77   */ "TCPIP_SENDS_TOTAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 78   */ "TOTAL_APP_RQST_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 79   */ "TOTAL_RQST_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 80   */ "WLM_QUEUE_TIME_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 81   */ "WLM_QUEUE_ASSIGNMENTS_TOTAL",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 82   */ "TOTAL_CPU_TIME",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 83   */ "TOTAL_WAIT_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 84   */ "APP_RQSTS_COMPLETED_TOTAL",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 85   */ "TOTAL_SECTION_SORT_TIME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 86   */ "TOTAL_SECTION_SORT_PROC_TIME",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 87   */ "TOTAL_SECTION_SORTS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 88   */ "TOTAL_SORTS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 89   */ "POST_THRESHOLD_SORTS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 90   */ "POST_SHRTHRESHOLD_SORTS",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 91   */ "SORT_OVERFLOWS",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 92   */ "TOTAL_COMPILE_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 93   */ "TOTAL_COMPILE_PROC_TIME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 94   */ "TOTAL_COMPILATIONS",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 95   */ "TOTAL_IMPLICIT_COMPILE_TIME",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 96   */ "TOTAL_IMPLICIT_COMPILE_PROC_TIME",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 97   */ "TOTAL_IMPLICIT_COMPILATIONS",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 98   */ "TOTAL_SECTION_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 99   */ "TOTAL_SECTION_PROC_TIME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 100  */ "TOTAL_APP_SECTION_EXECUTIONS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 101  */ "TOTAL_ACT_TIME",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 102  */ "TOTAL_ACT_WAIT_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 103  */ "ACT_RQSTS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 104  */ "TOTAL_ROUTINE_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 105  */ "TOTAL_ROUTINE_INVOCATIONS",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 106  */ "TOTAL_COMMIT_TIME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 107  */ "TOTAL_COMMIT_PROC_TIME",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 108  */ "TOTAL_APP_COMMITS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 109  */ "INT_COMMITS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 110  */ "TOTAL_ROLLBACK_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 111  */ "TOTAL_ROLLBACK_PROC_TIME",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 112  */ "TOTAL_APP_ROLLBACKS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 113  */ "INT_ROLLBACKS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 114  */ "TOTAL_RUNSTATS_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 115  */ "TOTAL_RUNSTATS_PROC_TIME",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 116  */ "TOTAL_RUNSTATS",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 117  */ "TOTAL_REORG_TIME",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 118  */ "TOTAL_REORG_PROC_TIME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 119  */ "TOTAL_REORGS",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 120  */ "TOTAL_LOAD_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 121  */ "TOTAL_LOAD_PROC_TIME",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 122  */ "TOTAL_LOADS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 123  */ "CAT_CACHE_INSERTS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 124  */ "CAT_CACHE_LOOKUPS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 125  */ "PKG_CACHE_INSERTS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 126  */ "PKG_CACHE_LOOKUPS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 127  */ "THRESH_VIOLATIONS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 128  */ "NUM_LW_THRESH_EXCEEDED",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 129  */ "LOCK_WAITS_GLOBAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 130  */ "LOCK_WAIT_TIME_GLOBAL",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 131  */ "LOCK_TIMEOUTS_GLOBAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 132  */ "LOCK_ESCALS_MAXLOCKS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 133  */ "LOCK_ESCALS_LOCKLIST",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 134  */ "LOCK_ESCALS_GLOBAL",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 135  */ "RECLAIM_WAIT_TIME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 136  */ "SPACEMAPPAGE_RECLAIM_WAIT_TIME",                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 137  */ "CF_WAITS",                                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 138  */ "CF_WAIT_TIME",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 139  */ "POOL_DATA_GBP_L_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 140  */ "POOL_DATA_GBP_P_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 141  */ "POOL_DATA_LBP_PAGES_FOUND",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 142  */ "POOL_DATA_GBP_INVALID_PAGES",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 143  */ "POOL_INDEX_GBP_L_READS",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 144  */ "POOL_INDEX_GBP_P_READS",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 145  */ "POOL_INDEX_LBP_PAGES_FOUND",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 146  */ "POOL_INDEX_GBP_INVALID_PAGES",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 147  */ "POOL_XDA_GBP_L_READS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 148  */ "POOL_XDA_GBP_P_READS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 149  */ "POOL_XDA_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 150  */ "POOL_XDA_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 151  */ "AUDIT_EVENTS_TOTAL",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 152  */ "AUDIT_FILE_WRITES_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 153  */ "AUDIT_FILE_WRITE_WAIT_TIME",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 154  */ "AUDIT_SUBSYSTEM_WAITS_TOTAL",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 155  */ "AUDIT_SUBSYSTEM_WAIT_TIME",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 156  */ "CLIENT_HOSTNAME",                               /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
//		/* RS> 157  */ "CLIENT_PORT_NUMBER",                            /* java.sql.Types.INTEGER   INTEGER                  SAMPLE.-none- */
		/* RS> 158  */ "DIAGLOG_WRITES_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 159  */ "DIAGLOG_WRITE_WAIT_TIME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 160  */ "FCM_MESSAGE_RECVS_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 161  */ "FCM_MESSAGE_RECV_VOLUME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 162  */ "FCM_MESSAGE_RECV_WAIT_TIME",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 163  */ "FCM_MESSAGE_SENDS_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 164  */ "FCM_MESSAGE_SEND_VOLUME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 165  */ "FCM_MESSAGE_SEND_WAIT_TIME",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 166  */ "FCM_TQ_RECVS_TOTAL",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 167  */ "FCM_TQ_RECV_VOLUME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 168  */ "FCM_TQ_RECV_WAIT_TIME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 169  */ "FCM_TQ_SENDS_TOTAL",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 170  */ "FCM_TQ_SEND_VOLUME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 171  */ "FCM_TQ_SEND_WAIT_TIME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 172  */ "LAST_EXECUTABLE_ID",                            /* java.sql.Types.VARBINARY VARCHAR FOR BIT DATA(64) SAMPLE.-none- */
//		/* RS> 173  */ "LAST_REQUEST_TYPE",                             /* java.sql.Types.VARCHAR   VARCHAR(32)              SAMPLE.-none- */
		/* RS> 174  */ "TOTAL_ROUTINE_USER_CODE_PROC_TIME",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 175  */ "TOTAL_ROUTINE_USER_CODE_TIME",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 176  */ "TQ_TOT_SEND_SPILLS",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 177  */ "EVMON_WAIT_TIME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 178  */ "EVMON_WAITS_TOTAL",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 179  */ "TOTAL_EXTENDED_LATCH_WAIT_TIME",                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 180  */ "TOTAL_EXTENDED_LATCH_WAITS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 181  */ "INTRA_PARALLEL_STATE",                          /* java.sql.Types.VARCHAR   VARCHAR(3)               SAMPLE.-none- */
		/* RS> 182  */ "TOTAL_STATS_FABRICATION_TIME",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 183  */ "TOTAL_STATS_FABRICATION_PROC_TIME",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 184  */ "TOTAL_STATS_FABRICATIONS",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 185  */ "TOTAL_SYNC_RUNSTATS_TIME",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 186  */ "TOTAL_SYNC_RUNSTATS_PROC_TIME",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 187  */ "TOTAL_SYNC_RUNSTATS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 188  */ "TOTAL_DISP_RUN_QUEUE_TIME",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 189  */ "TOTAL_PEDS",                                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 190  */ "DISABLED_PEDS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 191  */ "POST_THRESHOLD_PEDS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 192  */ "TOTAL_PEAS",                                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 193  */ "POST_THRESHOLD_PEAS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 194  */ "TQ_SORT_HEAP_REQUESTS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 195  */ "TQ_SORT_HEAP_REJECTIONS",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 196  */ "POOL_QUEUED_ASYNC_DATA_REQS",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 197  */ "POOL_QUEUED_ASYNC_INDEX_REQS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 198  */ "POOL_QUEUED_ASYNC_XDA_REQS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 199  */ "POOL_QUEUED_ASYNC_TEMP_DATA_REQS",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 200  */ "POOL_QUEUED_ASYNC_TEMP_INDEX_REQS",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 201  */ "POOL_QUEUED_ASYNC_TEMP_XDA_REQS",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 202  */ "POOL_QUEUED_ASYNC_OTHER_REQS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 203  */ "POOL_QUEUED_ASYNC_DATA_PAGES",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 204  */ "POOL_QUEUED_ASYNC_INDEX_PAGES",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 205  */ "POOL_QUEUED_ASYNC_XDA_PAGES",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 206  */ "POOL_QUEUED_ASYNC_TEMP_DATA_PAGES",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 207  */ "POOL_QUEUED_ASYNC_TEMP_INDEX_PAGES",            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 208  */ "POOL_QUEUED_ASYNC_TEMP_XDA_PAGES",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 209  */ "POOL_FAILED_ASYNC_DATA_REQS",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 210  */ "POOL_FAILED_ASYNC_INDEX_REQS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 211  */ "POOL_FAILED_ASYNC_XDA_REQS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 212  */ "POOL_FAILED_ASYNC_TEMP_DATA_REQS",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 213  */ "POOL_FAILED_ASYNC_TEMP_INDEX_REQS",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 214  */ "POOL_FAILED_ASYNC_TEMP_XDA_REQS",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 215  */ "POOL_FAILED_ASYNC_OTHER_REQS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 216  */ "PREFETCH_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 217  */ "PREFETCH_WAITS",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 218  */ "APP_ACT_COMPLETED_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 219  */ "APP_ACT_ABORTED_TOTAL",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 220  */ "APP_ACT_REJECTED_TOTAL",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 221  */ "TOTAL_CONNECT_REQUEST_TIME",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 222  */ "TOTAL_CONNECT_REQUEST_PROC_TIME",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 223  */ "TOTAL_CONNECT_REQUESTS",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 224  */ "TOTAL_CONNECT_AUTHENTICATION_TIME",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 225  */ "TOTAL_CONNECT_AUTHENTICATION_PROC_TIME",        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 226  */ "TOTAL_CONNECT_AUTHENTICATIONS",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 227  */ "POOL_DATA_GBP_INDEP_PAGES_FOUND_IN_LBP",        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 228  */ "POOL_INDEX_GBP_INDEP_PAGES_FOUND_IN_LBP",       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 229  */ "POOL_XDA_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 230  */ "COMM_EXIT_WAIT_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 231  */ "COMM_EXIT_WAITS",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 232  */ "IDA_SEND_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 233  */ "IDA_SENDS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 234  */ "IDA_SEND_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 235  */ "IDA_RECV_WAIT_TIME",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 236  */ "IDA_RECVS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 237  */ "IDA_RECV_VOLUME",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 238  */ "MEMBER_SUBSET_ID",                              /* java.sql.Types.INTEGER   INTEGER                  SAMPLE.-none- */
//		/* RS> 239  */ "IS_SYSTEM_APPL",                                /* java.sql.Types.SMALLINT  SMALLINT                 SAMPLE.-none- */
//		/* RS> 240  */ "LOCK_TIMEOUT_VAL",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 241  */ "CURRENT_ISOLATION",                             /* java.sql.Types.CHAR      CHAR(2)                  SAMPLE.-none- */
		/* RS> 242  */ "NUM_LOCKS_WAITING",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 243  */ "UOW_CLIENT_IDLE_WAIT_TIME",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 244  */ "ROWS_DELETED",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 245  */ "ROWS_INSERTED",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 246  */ "ROWS_UPDATED",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 247  */ "TOTAL_HASH_JOINS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 248  */ "TOTAL_HASH_LOOPS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 249  */ "HASH_JOIN_OVERFLOWS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 250  */ "HASH_JOIN_SMALL_OVERFLOWS",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 251  */ "POST_SHRTHRESHOLD_HASH_JOINS",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 252  */ "TOTAL_OLAP_FUNCS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 253  */ "OLAP_FUNC_OVERFLOWS",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 254  */ "DYNAMIC_SQL_STMTS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 255  */ "STATIC_SQL_STMTS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 256  */ "FAILED_SQL_STMTS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 257  */ "SELECT_SQL_STMTS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 258  */ "UID_SQL_STMTS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 259  */ "DDL_SQL_STMTS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 260  */ "MERGE_SQL_STMTS",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 261  */ "XQUERY_STMTS",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 262  */ "IMPLICIT_REBINDS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 263  */ "BINDS_PRECOMPILES",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 264  */ "INT_ROWS_DELETED",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 265  */ "INT_ROWS_INSERTED",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 266  */ "INT_ROWS_UPDATED",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 267  */ "CALL_SQL_STMTS",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 268  */ "POOL_COL_L_READS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 269  */ "POOL_TEMP_COL_L_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 270  */ "POOL_COL_P_READS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 271  */ "POOL_TEMP_COL_P_READS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 272  */ "POOL_COL_LBP_PAGES_FOUND",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 273  */ "POOL_COL_WRITES",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 274  */ "POOL_COL_GBP_L_READS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 275  */ "POOL_COL_GBP_P_READS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 276  */ "POOL_COL_GBP_INVALID_PAGES",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 277  */ "POOL_COL_GBP_INDEP_PAGES_FOUND_IN_LBP",         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 278  */ "POOL_QUEUED_ASYNC_COL_REQS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 279  */ "POOL_QUEUED_ASYNC_TEMP_COL_REQS",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 280  */ "POOL_QUEUED_ASYNC_COL_PAGES",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 281  */ "POOL_QUEUED_ASYNC_TEMP_COL_PAGES",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 282  */ "POOL_FAILED_ASYNC_COL_REQS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 283  */ "POOL_FAILED_ASYNC_TEMP_COL_REQS",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 284  */ "TOTAL_COL_TIME",                                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 285  */ "TOTAL_COL_PROC_TIME",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 286  */ "TOTAL_COL_EXECUTIONS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 287  */ "CLIENT_IPADDR",                                 /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
		/* RS> 288  */ "SQL_REQS_SINCE_COMMIT",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 289  */ "UOW_START_TIME",                                /* java.sql.Types.TIMESTAMP TIMESTAMP                SAMPLE.-none- */
//		/* RS> 290  */ "UOW_STOP_TIME",                                 /* java.sql.Types.TIMESTAMP TIMESTAMP                SAMPLE.-none- */
//		/* RS> 291  */ "PREV_UOW_STOP_TIME",                            /* java.sql.Types.TIMESTAMP TIMESTAMP                SAMPLE.-none- */
//		/* RS> 292  */ "UOW_COMP_STATUS",                               /* java.sql.Types.VARCHAR   VARCHAR(14)              SAMPLE.-none- */
//		/* RS> 293  */ "NUM_ASSOC_AGENTS",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 294  */ "ASSOCIATED_AGENTS_TOP",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 295  */ "WORKLOAD_OCCURRENCE_STATE",                     /* java.sql.Types.VARCHAR   VARCHAR(32)              SAMPLE.-none- */
		/* RS> 296  */ "POST_THRESHOLD_HASH_JOINS",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 297  */ "POOL_DATA_CACHING_TIER_L_READS",                /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 298  */ "POOL_INDEX_CACHING_TIER_L_READS",               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 299  */ "POOL_XDA_CACHING_TIER_L_READS",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 300  */ "POOL_COL_CACHING_TIER_L_READS",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 301  */ "POOL_DATA_CACHING_TIER_PAGE_WRITES",            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 302  */ "POOL_INDEX_CACHING_TIER_PAGE_WRITES",           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 303  */ "POOL_XDA_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 304  */ "POOL_COL_CACHING_TIER_PAGE_WRITES",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 305  */ "POOL_DATA_CACHING_TIER_PAGE_UPDATES",           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 306  */ "POOL_INDEX_CACHING_TIER_PAGE_UPDATES",          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 307  */ "POOL_XDA_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 308  */ "POOL_COL_CACHING_TIER_PAGE_UPDATES",            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 309  */ "POOL_CACHING_TIER_PAGE_READ_TIME",              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 310  */ "POOL_CACHING_TIER_PAGE_WRITE_TIME",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 311  */ "POOL_DATA_CACHING_TIER_PAGES_FOUND",            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 312  */ "POOL_INDEX_CACHING_TIER_PAGES_FOUND",           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 313  */ "POOL_XDA_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 314  */ "POOL_COL_CACHING_TIER_PAGES_FOUND",             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 315  */ "POOL_DATA_CACHING_TIER_GBP_INVALID_PAGES",      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 316  */ "POOL_INDEX_CACHING_TIER_GBP_INVALID_PAGES",     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 317  */ "POOL_XDA_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 318  */ "POOL_COL_CACHING_TIER_GBP_INVALID_PAGES",       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 319  */ "POOL_DATA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 320  */ "POOL_INDEX_CACHING_TIER_GBP_INDEP_PAGES_FOUND", /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 321  */ "POOL_XDA_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 322  */ "POOL_COL_CACHING_TIER_GBP_INDEP_PAGES_FOUND",   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 323  */ "TOTAL_HASH_GRPBYS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 324  */ "HASH_GRPBY_OVERFLOWS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 325  */ "POST_THRESHOLD_HASH_GRPBYS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 326  */ "EXECUTION_ID",                                  /* java.sql.Types.VARCHAR   VARCHAR(128)             SAMPLE.-none- */
		/* RS> 327  */ "POST_THRESHOLD_OLAP_FUNCS",                     /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 328  */ "EXT_TABLE_RECV_WAIT_TIME",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 329  */ "EXT_TABLE_RECVS_TOTAL",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 330  */ "EXT_TABLE_RECV_VOLUME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 331  */ "EXT_TABLE_READ_VOLUME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 332  */ "EXT_TABLE_SEND_WAIT_TIME",                      /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 333  */ "EXT_TABLE_SENDS_TOTAL",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 334  */ "EXT_TABLE_SEND_VOLUME",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 335  */ "EXT_TABLE_WRITE_VOLUME",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 336  */ "POST_THRESHOLD_COL_VECTOR_CONSUMERS",           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 337  */ "TOTAL_COL_VECTOR_CONSUMERS",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 338  */ "ACTIVE_HASH_GRPBYS",                            /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 339  */ "ACTIVE_HASH_JOINS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 340  */ "ACTIVE_OLAP_FUNCS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 341  */ "ACTIVE_PEAS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 342  */ "ACTIVE_PEDS",                                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 343  */ "ACTIVE_SORT_CONSUMERS",                         /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 344  */ "ACTIVE_SORTS",                                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 345  */ "ACTIVE_COL_VECTOR_CONSUMERS",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 346  */ "SORT_HEAP_ALLOCATED",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 347  */ "SORT_SHRHEAP_ALLOCATED",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 348  */ "TOTAL_BACKUP_TIME",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 349  */ "TOTAL_BACKUP_PROC_TIME",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 350  */ "TOTAL_BACKUPS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 351  */ "TOTAL_INDEX_BUILD_TIME",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 352  */ "TOTAL_INDEX_BUILD_PROC_TIME",                   /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 353  */ "TOTAL_INDEXES_BUILT",                           /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 354  */ "FCM_TQ_RECV_WAITS_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 355  */ "FCM_MESSAGE_RECV_WAITS_TOTAL",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 356  */ "FCM_TQ_SEND_WAITS_TOTAL",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 357  */ "FCM_MESSAGE_SEND_WAITS_TOTAL",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 358  */ "FCM_SEND_WAITS_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 359  */ "FCM_RECV_WAITS_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 360  */ "COL_VECTOR_CONSUMER_OVERFLOWS",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 361  */ "TOTAL_COL_SYNOPSIS_TIME",                       /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 362  */ "TOTAL_COL_SYNOPSIS_PROC_TIME",                  /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 363  */ "TOTAL_COL_SYNOPSIS_EXECUTIONS",                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 364  */ "COL_SYNOPSIS_ROWS_INSERTED",                    /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
//		/* RS> 365  */ "CONNECTION_REUSABILITY_STATUS",                 /* java.sql.Types.SMALLINT  SMALLINT                 SAMPLE.-none- */
//		/* RS> 366  */ "REUSABILITY_STATUS_REASON",                     /* java.sql.Types.VARCHAR   VARCHAR(255)             SAMPLE.-none- */
		/* RS> 367  */ "APPL_SECTION_INSERTS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 368  */ "APPL_SECTION_LOOKUPS",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 369  */ "LOB_PREFETCH_WAIT_TIME",                        /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 370  */ "LOB_PREFETCH_REQS",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 371  */ "FED_ROWS_DELETED",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 372  */ "FED_ROWS_INSERTED",                             /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 373  */ "FED_ROWS_UPDATED",                              /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 374  */ "FED_ROWS_READ",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 375  */ "FED_WAIT_TIME",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 376  */ "FED_WAITS_TOTAL",                               /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 377  */ "ADM_OVERFLOWS",                                 /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
		/* RS> 378  */ "ADM_BYPASS_ACT_TOTAL",                          /* java.sql.Types.BIGINT    BIGINT                   SAMPLE.-none- */
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

		return new CmMonGetConnection(counterController, guiController);
	}

	public CmMonGetConnection(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("MON_GET_CONNECTION",  "fixme.");

			mtd.addColumn("MON_GET_CONNECTION", "xxx",    "<html>xxx</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("APPLICATION_HANDLE");
		pkCols.add("APPLICATION_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from table(SYSPROC.MON_GET_CONNECTION(null, null))";

		return sql;
	}
}
