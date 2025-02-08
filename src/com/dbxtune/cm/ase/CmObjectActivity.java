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
package com.dbxtune.cm.ase;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmObjectActivityPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.RemarkDictionary;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmObjectActivity
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmObjectActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Objects";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Performance information about object/tables." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - An Index (IndID > 0)</li>" +
		"    <li>VERY_LIGHT_BLUE - BLOB Text/Image Column (IndID = 255).</li>" +
		"    <li>LIGHT YELLOW - Tempdb Work Tables (LockScheme = 'WORK-TABLE').</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monOpenObjectActivity"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"LockContPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LogicalReads","PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", 
		"UsedCount", "RowsInsUpdDel", "RowsInserted", "RowsDeleted", "RowsUpdated", 
		"Operations", "LockRequests", "LockWaits", "HkgcRequests", "HkgcPending", "HkgcOverflows", 
		"OptSelectCount", "PhysicalLocks", "PhysicalLocksRetained", "PhysicalLocksRetainWaited", 
		"PhysicalLocksDeadlocks", "PhysicalLocksWaited", "PhysicalLocksPageTransfer", 
		"TransferReqWaited", "TotalServiceRequests", "PhysicalLocksDowngraded", "PagesTransferred", 
		"ClusterPageWrites", "SharedLockWaitTime", "ExclusiveLockWaitTime", "UpdateLockWaitTime", 
		"HkgcRequestsDcomp", "HkgcOverflowsDcomp", 
		"IOSize1Page", "IOSize2Pages", "IOSize4Pages", "IOSize8Pages", 
		"PRSSelectCount", "PRSRewriteCount",
		"NumLevel0Waiters",
		"Scans", "Updates", "Inserts", "Deletes"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultPostponeTime()                 { return 300; } // every 5 minute
	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; } // every 10 minute
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

		return new CmObjectActivity(counterController, guiController);
	}

	public CmObjectActivity(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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

		// Reset some config
		Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tempConf != null)
		{
			tempConf.setProperty(PROPKEY_disable_tabRowCount_timestamp, DEFAULT_disable_tabRowCount_timestamp);
			tempConf.save();
		}
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_tabRowCount            = PROP_PREFIX + ".sample.TabRowCount";
	public static final boolean DEFAULT_sample_tabRowCount            = true;

	public static final String  PROPKEY_disable_tabRowCount_onTimeout = PROP_PREFIX + ".disable.TabRowCount.onTimeoutException";
	public static final boolean DEFAULT_disable_tabRowCount_onTimeout = true;

	public static final String  PROPKEY_disable_tabRowCount_timestamp = PROP_PREFIX + ".disable.TabRowCount.timestamp"; // When we do PROPKEY_disable_tabRowCount_onTimeout, then save the time we last did it.
	public static final String  DEFAULT_disable_tabRowCount_timestamp = "";

	public static final String  PROPKEY_sample_objectName             = PROP_PREFIX + ".sample.ObjectName";
	public static final boolean DEFAULT_sample_objectName             = false;

	public static final String  PROPKEY_sample_topRows                = PROP_PREFIX + ".sample.topRows";
	public static final boolean DEFAULT_sample_topRows                = false;

	public static final String  PROPKEY_sample_topRowsCount           = PROP_PREFIX + ".sample.topRows.count";
	public static final int     DEFAULT_sample_topRowsCount           = 500;

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	public static final String  PROPKEY_sample_tempdbWorkTables       = PROP_PREFIX + ".sample.tempdb.workTables";
	public static final boolean DEFAULT_sample_tempdbWorkTables       = true;


	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_tabRowCount,            DEFAULT_sample_tabRowCount);
		Configuration.registerDefaultValue(PROPKEY_disable_tabRowCount_onTimeout, DEFAULT_disable_tabRowCount_onTimeout);
		Configuration.registerDefaultValue(PROPKEY_sample_objectName,             DEFAULT_sample_objectName);
		Configuration.registerDefaultValue(PROPKEY_sample_topRows,                DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_topRowsCount,           DEFAULT_sample_topRowsCount);
		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,           DEFAULT_sample_systemTables);
	}
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmObjectActivityPanel(this);
	}

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

			mtd.addColumn("monOpenObjectActivity", "LastScanDateDiff"               ,"<html>How many Milliseconds since last Scan.  <br><b>Formula</b>: datediff(ms, LastScanDate, getdate())<br></html>");
			mtd.addColumn("monOpenObjectActivity", "LastInsertDateDiff"             ,"<html>How many Milliseconds since last Insert.<br><b>Formula</b>: datediff(ms, LastInsertDate, getdate())<br></html>");
			mtd.addColumn("monOpenObjectActivity", "LastUpdateDateDiff"             ,"<html>How many Milliseconds since last Update.<br><b>Formula</b>: datediff(ms, LastUpdateDate, getdate())<br></html>");
			mtd.addColumn("monOpenObjectActivity", "LastDeleteDateDiff"             ,"<html>How many Milliseconds since last Delete.<br><b>Formula</b>: datediff(ms, LastDeleteDate, getdate())<br></html>");

			mtd.addColumn("monOpenObjectActivity", "LockContPct"                    ,"<html>How many Lock Requests in percent was Blocked by another concurrent SPID's due to incompatible locking issues.<br><b>Note</b>: Do also considder number of LockWaits and not only the percentage.<br><b>Formula</b>: LockWaits / LockRequests * 100.0<br></html>");
			mtd.addColumn("monOpenObjectActivity", "TabRowCount"                    ,"<html>Table rowcount, using row_count(DBID, ObjectID) to get the count, so it can be a bit off from the actual number of rows.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+PROPKEY_sample_tabRowCount+"=false</code>.</html>");
			mtd.addColumn("monOpenObjectActivity", "UsageInMb"                      ,"<html>Number of MB used by this table/index, using data_pages(DBID, ObjectID, IndexID)/(1024*1024/@@maxpagesize) to get the value.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+getName()+PROPKEY_sample_tabRowCount+"=false</code>.</html>");
			mtd.addColumn("monOpenObjectActivity", "NumUsedPages"                   ,"<html>Number of Pages used by this table/index, using data_pages(DBID, ObjectID, IndexID) to get the value.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+getName()+PROPKEY_sample_tabRowCount+"=false</code>.</html>");
			mtd.addColumn("monOpenObjectActivity", "RowsPerPage"                    ,"<html>Number of rows per page.<br><b>Formula</b>: TabRowCount/NumUsedPages</html>");
			mtd.addColumn("monOpenObjectActivity", "RowsInsUpdDel"                  ,"<html>RowsInsUpdDel = RowsInserted + RowsDeleted + RowsUpdated<br>So this is simply a summary of all DML changes on this table.</html>");
			mtd.addColumn("monOpenObjectActivity", "Remark"                         ,"<html>Some tip of what's happening with this table<br><b>Tip</b>: \"Hover\" over the cell to get more information on the Tip.</html>");
			mtd.addColumn("monOpenObjectActivity", "IndexName"                      ,"<html>Name of the index.<br><b>Formula</b>: using ASE Function index_name(DBID, ObjectID, IndexID) to get the name of the index.</html>");

//			mtd.addColumn("monOpenObjectActivity", "PhysicalLocks"                  ,"<html>Number of physical locks requested per object.</html>");
			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksRetained"          ,"<html>Number of physical locks retained. <br>You can use this to identify the lock hit ratio for each object. " +
			                                                                           "<br>Good hit ratios imply balanced partitioning for this object.</html>");
//			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksRetainWaited"      ,"<html>Number of physical lock requests waiting before a lock is retained.</html>");
			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksDeadlocks"         ,"<html>Number of times a physical lock requested returned a deadlock. " +
			                                                                           "<br>The Cluster Physical Locks subsection of sp_sysmon uses this counter to report deadlocks while acquiring physical locks for each object.</html>");
//			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksWaited"            ,"<html>Number of times an instance waits for a physical lock request.</html>");
			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksPageTransfer"      ,"<html>Number of page transfers that occurred when an instance requests a physical lock. " +
			                                                                           "<br>The Cluster Physical Locks subsection of sp_sysmon uses this counter to report the node-to-node transfer and physical-lock acquisition as a node affinity ratio for this object.</html>");
			mtd.addColumn("monOpenObjectActivity", "TransferReqWaited"              ,"<html>Number of times physical lock requests waiting before receiving page transfers.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgPhysicalLockWaitTime"        ,"<html>The average amount of time clients spend before the physical lock is granted.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgTransferReqWaitTime"         ,"<html>The average amount of time physical lock requests wait before receiving page transfers.</html>");
			mtd.addColumn("monOpenObjectActivity", "TotalServiceRequests"           ,"<html>Number of physical lock requests serviced by the Cluster Cache Manager of an instance.</html>");
			mtd.addColumn("monOpenObjectActivity", "PhysicalLocksDowngraded"        ,"<html>Number of physical lock downgrade requests serviced by the Cluster Cache Manager of an instance.</html>");
			mtd.addColumn("monOpenObjectActivity", "PagesTransferred"               ,"<html>Number of pages transferred at an instance by the Cluster Cache Manager.</html>");
			mtd.addColumn("monOpenObjectActivity", "ClusterPageWrites"              ,"<html>Number of pages written to disk by the Cluster Cache Manager of an instance.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgServiceTime"                 ,"<html>The average amount of service time spent by the Cluster Cache Manager of an instance.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgTimeWaitedOnLocalUsers"      ,"<html>The average amount of service time an instance�s Cluster Cache Manager waits due to page use by users on this instance.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgTransferSendWaitTime"        ,"<html>The average amount of service time an instance�s Cluster Cache Manager spends for page transfer.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgIOServiceTime"               ,"<html>The average amount of service time used by an instance�s Cluster Cache Manager for page transfer.</html>");
			mtd.addColumn("monOpenObjectActivity", "AvgDowngradeServiceTime"        ,"<html>The average amount of service time the Cluster Cache Manager uses to downgrade physical locks.</html>");
			mtd.addColumn("monOpenObjectActivity", "LrPerScan"                      ,"<html>Logical Reads per Scans. " +
			                                                                           "<br>This will tell you how many Logical Reads each <i>Operation/SQL-Statement</i> is using." +
			                                                                           "<br>A higher value than 3-5 for an index; probably means that it <i>scans</i> the index..." +
			                                                                           "<br>Hence: the where clauses do <b>not</i> match the starting columns of the index, or the index isn't unique enough." +
			                                                                           "<br><b>Formula</b>: if (diff.Scans > 0) diff.LogicalReads / diff.Scans else -1</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

//		pkCols.add("DBName");
//		pkCols.add("ObjectName");
		pkCols.add("DBID");
		pkCols.add("ObjectID");
		pkCols.add("IndexID");

		return pkCols;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample Table Row Count",     PROPKEY_sample_tabRowCount      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_tabRowCount      , DEFAULT_sample_tabRowCount      ), DEFAULT_sample_tabRowCount ,     "Sample Table Row Count using ASE functions row_count() and data_pages()" ));
		list.add(new CmSettingsHelper("Limit num of rows",          PROPKEY_sample_topRows          , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_topRows          , DEFAULT_sample_topRows          ), DEFAULT_sample_topRows     ,     "Get only first # rows (select top # ...) true or false"                  ));
		list.add(new CmSettingsHelper("Limit num of rowcount",      PROPKEY_sample_topRowsCount     , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRowsCount     , DEFAULT_sample_topRowsCount     ), DEFAULT_sample_topRowsCount,     "Get only first # rows (select top # ...), number of rows"                ));
		list.add(new CmSettingsHelper("Include System Tables",      PROPKEY_sample_systemTables     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables     , DEFAULT_sample_systemTables     ), DEFAULT_sample_systemTables,     "Sample ASE System Tables, dbcc traceon(3650)"                            ));
		list.add(new CmSettingsHelper("Include tempdb work Tables", PROPKEY_sample_tempdbWorkTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_tempdbWorkTables , DEFAULT_sample_tempdbWorkTables ), DEFAULT_sample_tempdbWorkTables, "Sample 'work tables' used in tempdb. (from monProcessObject)"            ));

		return list;
	}
	

	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo) 
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemTables = conf.getBooleanProperty(PROPKEY_sample_systemTables, DEFAULT_sample_systemTables);
		
		if (sample_systemTables)
			return "dbcc traceon(3650)";

		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		// this is used for, sum() or columns concatinated that will overflowing the 4 byte boundary
		//
		// In PRE 15.x, we use numeric, in post 15 we use bigint
		// well max for a bigint (8 byte int) is really: 9223372036854775807
		// which is equivalent to a numeric(19,0), but lets cahnge that in the future
		// I think 10,0 is fine, bacuase int (4 byte int) max is 2147483647 (0x7fffffff)
		// so concatinate at least 3 int should be possible... 
		String bigint = "numeric(10,0)"; // bigint if above 15.0

		if (srvVersion >= Ver.ver(15,0))
			bigint = "bigint";

		String topRows      = ""; // 'top 500' if only first 500 rows should be displayed
		
		String TabRowCount  = "";
		String UsageInMb    = "";
		String UsageInKb    = "";
		String NumUsedPages = "";
		String RowsPerPage  = "";
		String DBName       = "DBName=db_name(A.DBID), \n";
		String ObjectName   = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(255),A.ObjectID)), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
		String IndexName    = "";

		// ASE 15.7
		String SharedLockWaitTime    = "";
		String ExclusiveLockWaitTime = "";
		String UpdateLockWaitTime    = "";
		String ObjectCacheDate       = "";
		String ase15700_nl           = ""; // NL for this section

		if (srvVersion >= Ver.ver(15,0,2))
		{
			String rowCountOption = "          "; // 16.0 SP2 or 15.7 SP130 (I know it was introduced in SP130, but not in what 16 SP, so lets guess at SP2) 
			if (srvVersion >= Ver.ver(16,0,0, 2) || (srvVersion >= Ver.ver(15,7,0, 130) && srvVersion < Ver.ver(16,0)) )
				rowCountOption = ",'noblock'";

			TabRowCount  = "TabRowCount  = convert(bigint, row_count(A.DBID, A.ObjectID"+rowCountOption+")),   -- Disable col with property: "+PROPKEY_sample_tabRowCount+"=false\n";
			UsageInMb    = "UsageInMb    = convert(int, data_pages(A.DBID, A.ObjectID, A.IndexID) / (1024*1024/@@maxpagesize)), -- Disable col with property: "+PROPKEY_sample_tabRowCount+"=false\n";
			UsageInKb    = "UsageInKb    = convert(int, data_pages(A.DBID, A.ObjectID, A.IndexID) * (@@maxpagesize/1024)),      -- Disable col with property: "+PROPKEY_sample_tabRowCount+"=false\n";
			NumUsedPages = "NumUsedPages = convert(bigint, data_pages(A.DBID, A.ObjectID, A.IndexID)), -- Disable col with property: "+PROPKEY_sample_tabRowCount+"=false\n";
			RowsPerPage  = "RowsPerPage  = convert(numeric(9,1), 0),                                   -- Disable col with property: "+PROPKEY_sample_tabRowCount+"=false\n";
			DBName       = "A.DBName, \n";
//			ObjectName   = "A.ObjectName, \n";
			ObjectName   = "ObjectName = isnull(object_name(A.ObjectID, A.DBID), 'Obj='+A.ObjectName), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
			IndexName    = "IndexName = CASE WHEN IndexID=0 THEN convert(varchar(255),'DATA') \n" +
			               "                 ELSE convert(varchar(255), isnull(index_name(DBID, ObjectID, IndexID), '-unknown-')) \n" +
			               "            END, \n";

			// debug/trace
			Configuration conf = Configuration.getCombinedConfiguration();
			if (conf.getBooleanProperty(PROPKEY_sample_objectName, DEFAULT_sample_objectName))
			{
				ObjectName = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
				_logger.info(PROPKEY_sample_objectName+"=true, using the string '"+ObjectName+"' for ObjectName lookup.");
				ObjectName += ", \n";
			}
			if (conf.getBooleanProperty(PROPKEY_sample_tabRowCount, DEFAULT_sample_tabRowCount) == false)
			{
				TabRowCount  = "TabRowCount  = convert(bigint,-1), -- column is disabled, enable col with property: "+PROPKEY_sample_tabRowCount+"=true\n";
				UsageInMb    = "UsageInMb    = convert(int,   -1), -- column is disabled, enable col with property: "+PROPKEY_sample_tabRowCount+"=true\n";
				UsageInKb    = "UsageInKb    = convert(int,   -1), -- column is disabled, enable col with property: "+PROPKEY_sample_tabRowCount+"=true\n";
				NumUsedPages = "NumUsedPages = convert(bigint,-1), -- column is disabled, enable col with property: "+PROPKEY_sample_tabRowCount+"=true\n";
				RowsPerPage  = "RowsPerPage  = convert(bigint,-1), -- column is disabled, enable col with property: "+PROPKEY_sample_tabRowCount+"=true\n";
				_logger.info(PROPKEY_sample_tabRowCount+"=false, Disabling the column 'TabRowCount', 'UsageInMb', 'UsageInKb', 'NumUsedPages', 'RowsPerPage'.");
			}
		}

		if (srvVersion >= Ver.ver(15,7))
		{
			SharedLockWaitTime    = "SharedLockWaitTime, ";
			ExclusiveLockWaitTime = "ExclusiveLockWaitTime, ";
			UpdateLockWaitTime    = "UpdateLockWaitTime, ";
			ObjectCacheDate       = "ObjectCacheDate, ";

			ase15700_nl           = "\n"; // NL for this section
		}

		// ASE 15.7 SP100
		String NumLevel0Waiters    = "";
		String AvgLevel0WaitTime   = "";
		String ase1570_SP100_nl    = ""; // NL for this section

		if (srvVersion >= Ver.ver(15,7,0,100))
		{
			NumLevel0Waiters    = "NumLevel0Waiters, ";
			AvgLevel0WaitTime   = "AvgLevel0WaitTime, ";

			ase1570_SP100_nl    = "\n"; // NL for this section
		}

		// ASE 15.7.0 ESD#1
		String HkgcRequestsDcomp  = "";
		String HkgcPendingDcomp   = "";
		String HkgcOverflowsDcomp = "";
		String nl_15701           = ""; // NL for this section
		if (srvVersion >= Ver.ver(15,7,0,1))
		{
			HkgcRequestsDcomp  = "HkgcRequestsDcomp, ";
			HkgcPendingDcomp   = "HkgcPendingDcomp, ";
			HkgcOverflowsDcomp = "HkgcOverflowsDcomp, ";
			nl_15701           = "\n"; // NL for this section
		}
		
		// ASE 15.7.0 ESD#2
		String IOSize1Page        = ""; // Number of 1 page physical reads performed for the object
		String IOSize2Pages       = ""; // Number of 2 pages physical reads performed for the object
		String IOSize4Pages       = ""; // Number of 4 pages physical reads performed for the object
		String IOSize8Pages       = ""; // Number of 8 pages physical reads performed for the object
		String PRSSelectCount     = ""; // Number of times PRS (Precomputed Result Set) was selected for query rewriting plan during compilation
		String LastPRSSelectDate  = ""; // Last date the PRS (Precomputed Result Set) was selected for query rewriting plan during compilation
		String PRSRewriteCount    = ""; // Number of times PRS (Precomputed Result Set) was considered valid for query rewriting during compilation
		String LastPRSRewriteDate = ""; // Last date the PRS (Precomputed Result Set) was considered valid for query rewriting during compilation
		String nl_15702           = ""; // NL for this section

		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			IOSize1Page        = "IOSize1Page, ";        // DO DIFF CALC
			IOSize2Pages       = "IOSize2Pages, ";       // DO DIFF CALC
			IOSize4Pages       = "IOSize4Pages, ";       // DO DIFF CALC
			IOSize8Pages       = "IOSize8Pages, ";       // DO DIFF CALC
			PRSSelectCount     = "PRSSelectCount, ";     // DO DIFF CALC
			LastPRSSelectDate  = "LastPRSSelectDate, ";
			PRSRewriteCount    = "PRSRewriteCount, ";    // DO DIFF CALC
			LastPRSRewriteDate = "LastPRSRewriteDate, ";
			nl_15702           = "\n";
		}

		// ASE 16.0
		String Scans              = ""; // The number of scans on this object
		String LastScanDate       = ""; // The date of the last scan on this object
		String LastScanDateDiff   = ""; // ###: datediff(ms, LastDeleteDate, getdate())
		String Updates            = ""; // The number of updates on this object
		String LastUpdateDate     = ""; // The date of the last update on this object
		String LastUpdateDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
		String Inserts            = ""; // The date of the last update on this object
		String LastInsertDate     = ""; // The date of the last insert on this object
		String LastInsertDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
		String Deletes            = ""; // The number of deletes on this object
		String LastDeleteDate     = ""; // The date of the last delete on this object
		String LastDeleteDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
		String LrPerScan          = ""; // LogicalReads/Scans
		String nl_16000           = ""; // NL for this section

		if (srvVersion >= Ver.ver(16,0))
		{
			// note: protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days, 20 hours, 31 minutes and 23.647 seconds, the MS difference is overflowned
			
			Scans              = "Scans, ";              // DO DIFF CALC
			LastScanDate       = "LastScanDate, ";
//			LastScanDateDiff   = "LastScanDateDiff = datediff(ms, LastScanDate, getdate()), ";
			LastScanDateDiff   = "LastScanDateDiff = CASE WHEN datediff(day, LastScanDate, getdate()) >= 24 THEN -1 ELSE  datediff(ms, LastScanDate, getdate()) END, ";

			Updates            = "Updates, ";            // DO DIFF CALC
			LastUpdateDate     = "LastUpdateDate, ";
//			LastUpdateDateDiff = "LastUpdateDateDiff = datediff(ms, LastUpdateDate, getdate()), ";
			LastUpdateDateDiff = "LastUpdateDateDiff = CASE WHEN datediff(day, LastUpdateDate, getdate()) >= 24 THEN -1 ELSE  datediff(ms, LastUpdateDate, getdate()) END, ";
	
			Inserts            = "Inserts, ";            // DO DIFF CALC
			LastInsertDate     = "LastInsertDate, ";
//			LastInsertDateDiff = "LastInsertDateDiff = datediff(ms, LastInsertDate, getdate()), ";
			LastInsertDateDiff = "LastInsertDateDiff = CASE WHEN datediff(day, LastInsertDate, getdate()) >= 24 THEN -1 ELSE  datediff(ms, LastInsertDate, getdate()) END, ";

			Deletes            = "Deletes, ";            // DO DIFF CALC
			LastDeleteDate     = "LastDeleteDate, ";
//			LastDeleteDateDiff = "LastDeleteDateDiff = datediff(ms, LastDeleteDate, getdate()), ";
			LastDeleteDateDiff = "LastDeleteDateDiff = CASE WHEN datediff(day, LastDeleteDate, getdate()) >= 24 THEN -1 ELSE  datediff(ms, LastDeleteDate, getdate()) END, ";

			LrPerScan          = "LrPerScan = CASE WHEN Scans > 0 THEN convert(numeric(12,1), (LogicalReads*1.0) / (Scans*1.0)) ELSE -1 END, ";
			nl_16000           = "\n";
		}

		// ASE 16.0 SP3 PL4
		String MaxInsRowsInXact = ""; // Max number of rows inserted in any transaction for this object
		String MaxDelRowsInXact = ""; // Max number of rows deleted  in any transaction for this object
		String MaxUpdRowsInXact = ""; // Max number of rows updated  in any transaction for this object
		String nl_160_sp3_pl4   = ""; // NL for this section

		if (srvVersion >= Ver.ver(16,0,0, 3,4))
		{
			MaxInsRowsInXact = "MaxInsRowsInXact, "; // do NOT do diff calc
			MaxDelRowsInXact = "MaxDelRowsInXact, "; // do NOT do diff calc
			MaxUpdRowsInXact = "MaxUpdRowsInXact, "; // do NOT do diff calc

			nl_160_sp3_pl4   = "\n";
		}

		
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
			cols1 += "InstanceID, ";
		}

		cols1 += "A.DBID, A.ObjectID, \n" +
		         DBName +
		         ObjectName + 
		         "A.IndexID, \n" +
		         IndexName +
		         "LockScheme = lockscheme(A.ObjectID, A.DBID), \n" +
		         "Remark = convert(varchar(60), ''), \n" + // this would be a good position after X tests has been done, but put it at the end right now
		         Scans + LastScanDate + nl_16000 + 
		         LastScanDateDiff + nl_16000 +
		         "LockRequests=isnull(LockRequests,0), LockWaits=isnull(LockWaits,0), \n" +
		         "LockContPct = CASE WHEN isnull(LockRequests,0) > 0 \n" +
		         "                   THEN convert(numeric(10,1), ((LockWaits+0.0)/(LockRequests+0.0)) * 100.0) \n" +
		         "                   ELSE convert(numeric(10,1), 0.0) \n" +
		         "              END, \n" +
		         SharedLockWaitTime + ExclusiveLockWaitTime + UpdateLockWaitTime + ase15700_nl +
		         NumLevel0Waiters + AvgLevel0WaitTime + ase1570_SP100_nl +
		         LrPerScan + "LogicalReads, PhysicalReads, APFReads, PagesRead, \n" +
		         IOSize1Page + IOSize2Pages + IOSize4Pages + IOSize8Pages + nl_15702 +
		         "PhysicalWrites, PagesWritten, UsedCount, Operations, \n" +
		         TabRowCount +
		         UsageInMb + 
		         UsageInKb + 
		         NumUsedPages +
		         RowsPerPage +
		         // RowsInserted + RowsDeleted + RowsUpdated : will overflow if much changes, so individual converts are neccecary
		         "RowsInsUpdDel=convert("+bigint+",RowsInserted) + convert("+bigint+",RowsDeleted) + convert("+bigint+",RowsUpdated), \n" +
		         "RowsInserted, RowsDeleted, RowsUpdated, OptSelectCount, \n" +
		         MaxInsRowsInXact              + MaxUpdRowsInXact              + MaxDelRowsInXact   + nl_160_sp3_pl4 +
		         Inserts                       + Updates                       + Deletes            + nl_16000 +
		         LastInsertDateDiff + nl_16000 + LastUpdateDateDiff + nl_16000 + LastDeleteDateDiff + nl_16000 +
		         LastInsertDate                + LastUpdateDate                + LastDeleteDate     + nl_16000 +
		         ""; // end of cols1
		cols2 += "";
		cols3 += ObjectCacheDate + "LastOptSelectDate, LastUsedDate";
	//	cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate, LastOptSelectDateDiff=datediff(ss,LastOptSelectDate,getdate()), LastUsedDateDiff=datediff(ss,LastUsedDate,getdate())";
	// it looked like we got "overflow" in the datediff sometimes... And I have newer really used these cols, so lets take them out for a while...
		if (srvVersion >= Ver.ver(15,0,2))
		{
			cols2 += "HkgcRequests, HkgcPending, HkgcOverflows, \n";
		}
		if (srvVersion >= Ver.ver(15,7,0,1))
		{
			cols2 += HkgcRequestsDcomp + HkgcPendingDcomp + HkgcOverflowsDcomp + nl_15701;
		}
		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			cols2 += PRSSelectCount + LastPRSSelectDate + PRSRewriteCount + LastPRSRewriteDate + nl_15702;
		}


		//-------------------------------------------
		// Adding Cluster Edition Specific Counters
		//-------------------------------------------

		// ASE 15.0.3 CE
		String PhysicalLocks             = "";
		String PhysicalLocksRetained     = "";
		String PhysicalLocksRetainWaited = "";
		String PhysicalLocksDeadlocks    = "";
		String PhysicalLocksWaited       = "";
		String PhysicalLocksPageTransfer = "";
		String TransferReqWaited         = "";
		String AvgPhysicalLockWaitTime   = "";
		String AvgTransferReqWaitTime    = "";
		String TotalServiceRequests      = "";
		String PhysicalLocksDowngraded   = "";
		String PagesTransferred          = "";
		String ClusterPageWrites         = "";
		String AvgServiceTime            = "";
		String AvgTimeWaitedOnLocalUsers = "";
		String AvgTransferSendWaitTime   = "";
		String AvgIOServiceTime          = "";
		String AvgDowngradeServiceTime   = "";
		String ase15030_ce_nl            = ""; // NL for this section

		// ASE 15.5.0 ESD#1 CE
		String MaxPhysicalLockWaitTime   = "";
		String MaxTransferReqWaitTime    = "";
		String MaxServiceTime            = "";
		String AvgQueueWaitTime          = "";
		String MaxQueueWaitTime          = "";
		String MaxTimeWaitedOnLocalUsers = "";
		String MaxTransferSendWaitTime   = "";
		String MaxIOServiceTime          = "";
		String MaxDowngradeServiceTime   = "";
		String ase15501_ce_nl            = ""; // NL for this section

//		if ( srvVersion >= 15030 && isClusterEnabled )
//		if ( srvVersion >= 1503000 && isClusterEnabled )
		if ( srvVersion >= Ver.ver(15,0,3) && isClusterEnabled )
		{
			PhysicalLocks             = "PhysicalLocks, ";
			PhysicalLocksRetained     = "PhysicalLocksRetained, ";
			PhysicalLocksRetainWaited = "PhysicalLocksRetainWaited, ";
			PhysicalLocksDeadlocks    = "PhysicalLocksDeadlocks, ";
			PhysicalLocksWaited       = "PhysicalLocksWaited, ";
			PhysicalLocksPageTransfer = "PhysicalLocksPageTransfer, ";
			TransferReqWaited         = "TransferReqWaited, ";
			AvgPhysicalLockWaitTime   = "AvgPhysicalLockWaitTime, ";
			AvgTransferReqWaitTime    = "AvgTransferReqWaitTime, ";
			TotalServiceRequests      = "TotalServiceRequests, ";
			PhysicalLocksDowngraded   = "PhysicalLocksDowngraded, ";
			PagesTransferred          = "PagesTransferred, ";
			ClusterPageWrites         = "ClusterPageWrites, ";
			AvgServiceTime            = "AvgServiceTime, ";
			AvgTimeWaitedOnLocalUsers = "AvgTimeWaitedOnLocalUsers, ";
			AvgTransferSendWaitTime   = "AvgTransferSendWaitTime, ";
			AvgIOServiceTime          = "AvgIOServiceTime, ";
			AvgDowngradeServiceTime   = "AvgDowngradeServiceTime, ";
			ase15030_ce_nl            = "\n";
		}
//		if ( srvVersion >= 15501 && isClusterEnabled )
//		if ( srvVersion >= 1550010 && isClusterEnabled )
		if ( srvVersion >= Ver.ver(15,5,0,1) && isClusterEnabled )
		{
			MaxPhysicalLockWaitTime   = "MaxPhysicalLockWaitTime, ";
			MaxTransferReqWaitTime    = "MaxTransferReqWaitTime, ";
			MaxServiceTime            = "MaxServiceTime, ";
			AvgQueueWaitTime          = "AvgQueueWaitTime, ";
			MaxQueueWaitTime          = "MaxQueueWaitTime, ";
			MaxTimeWaitedOnLocalUsers = "MaxTimeWaitedOnLocalUsers, ";
			MaxTransferSendWaitTime   = "MaxTransferSendWaitTime, ";
			MaxIOServiceTime          = "MaxIOServiceTime, ";
			MaxDowngradeServiceTime   = "MaxDowngradeServiceTime, ";
			ase15501_ce_nl            = "\n";
		}
		cols2 += PhysicalLocks;
		cols2 += PhysicalLocksRetained;
		cols2 += PhysicalLocksRetainWaited;
		cols2 += PhysicalLocksDeadlocks;
		cols2 += PhysicalLocksWaited;
		cols2 += PhysicalLocksPageTransfer;
			cols2 += ase15030_ce_nl; // NL for this section
		cols2 += TransferReqWaited;
		cols2 += AvgPhysicalLockWaitTime;
		cols2 += MaxPhysicalLockWaitTime;
		cols2 += AvgTransferReqWaitTime;
		cols2 += MaxTransferReqWaitTime;
		cols2 += TotalServiceRequests;
		cols2 += PhysicalLocksDowngraded;
			cols2 += ase15030_ce_nl; // NL for this section
		cols2 += PagesTransferred;
		cols2 += ClusterPageWrites;
		cols2 += AvgServiceTime;
		cols2 += MaxServiceTime;
		cols2 += AvgTimeWaitedOnLocalUsers;
		cols2 += MaxTimeWaitedOnLocalUsers;
		cols2 += AvgTransferSendWaitTime;
		cols2 += MaxTransferSendWaitTime;
			cols2 += ase15501_ce_nl; // NL for this section
		cols2 += AvgIOServiceTime;
		cols2 += MaxIOServiceTime;
		cols2 += AvgDowngradeServiceTime;
		cols2 += MaxDowngradeServiceTime;
		cols2 += AvgQueueWaitTime;
		cols2 += MaxQueueWaitTime;
			cols2 += ase15501_ce_nl; // NL for this section


		boolean sampleWorkTables = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_tempdbWorkTables, DEFAULT_sample_tempdbWorkTables);
		String workTableSql = "";
		if (sampleWorkTables)
		{
			String wt_DBName       = "DBName, \n";
			String wt_ObjectName   = "ObjectName = ObjectName + ' [SPID=' + convert(varchar(10),SPID)+']', ";
			String wt_IndexName    = "IndexName  = convert(varchar(255),'WORK-TABLE-DATA'), ";

			String wt_TabRowCount  = "TabRowCount  = -1, \n";
//			String wt_UsageInMb    = "UsageInMb    = -1, \n";
			String wt_UsageInMb    = "UsageInMb = PartitionSize / 1024, \n"; // PartitionSize = SizeInKB  ... so this is not good enough (FIXME: small tables will be 0)
			String wt_UsageInKb    = "UsageInKb = PartitionSize, \n";
//			String wt_NumUsedPages = "NumUsedPages = -1, \n";
			String wt_NumUsedPages = "NumUsedPages = PartitionSize / (@@maxpagesize/1024), \n";
			String wt_RowsPerPage  = "RowsPerPage  = -1, \n";
			
			// ASE 15.7
			String wt_SharedLockWaitTime    = "";
			String wt_ExclusiveLockWaitTime = "";
			String wt_UpdateLockWaitTime    = "";
			String wt_ObjectCacheDate       = "";

			if (srvVersion >= Ver.ver(15,7))
			{
				wt_SharedLockWaitTime    = "SharedLockWaitTime = -1, ";
				wt_ExclusiveLockWaitTime = "ExclusiveLockWaitTime = -1, ";
				wt_UpdateLockWaitTime    = "UpdateLockWaitTime = -1, ";
				wt_ObjectCacheDate       = "ObjectCacheDate = convert(datetime, NULL), ";
			}

			// ASE 15.7 SP100
			String wt_NumLevel0Waiters    = "";
			String wt_AvgLevel0WaitTime   = "";

			if (srvVersion >= Ver.ver(15,7,0,100))
			{
				wt_NumLevel0Waiters    = "NumLevel0Waiters = -1, ";
				wt_AvgLevel0WaitTime   = "AvgLevel0WaitTime = -1, ";
			}

			// ASE 15.7.0 ESD#1
			String wt_HkgcRequestsDcomp  = "";
			String wt_HkgcPendingDcomp   = "";
			String wt_HkgcOverflowsDcomp = "";
			
			if (srvVersion >= Ver.ver(15,7,0,1))
			{
				wt_HkgcRequestsDcomp  = "HkgcRequestsDcomp = -1, ";
				wt_HkgcPendingDcomp   = "HkgcPendingDcomp = -1, ";
				wt_HkgcOverflowsDcomp = "HkgcOverflowsDcomp = -1, ";
			}
			
			// ASE 15.7.0 ESD#2
			String wt_IOSize1Page        = ""; // Number of 1 page physical reads performed for the object
			String wt_IOSize2Pages       = ""; // Number of 2 pages physical reads performed for the object
			String wt_IOSize4Pages       = ""; // Number of 4 pages physical reads performed for the object
			String wt_IOSize8Pages       = ""; // Number of 8 pages physical reads performed for the object
			String wt_PRSSelectCount     = ""; // Number of times PRS (Precomputed Result Set) was selected for query rewriting plan during compilation
			String wt_LastPRSSelectDate  = ""; // Last date the PRS (Precomputed Result Set) was selected for query rewriting plan during compilation
			String wt_PRSRewriteCount    = ""; // Number of times PRS (Precomputed Result Set) was considered valid for query rewriting during compilation
			String wt_LastPRSRewriteDate = ""; // Last date the PRS (Precomputed Result Set) was considered valid for query rewriting during compilation
			
			if (srvVersion >= Ver.ver(15,7,0,2))
			{
				wt_IOSize1Page        = "IOSize1Page = -1, ";        // DO DIFF CALC
				wt_IOSize2Pages       = "IOSize2Pages = -1, ";       // DO DIFF CALC
				wt_IOSize4Pages       = "IOSize4Pages = -1, ";       // DO DIFF CALC
				wt_IOSize8Pages       = "IOSize8Pages = -1, ";       // DO DIFF CALC
				wt_PRSSelectCount     = "PRSSelectCount = -1, ";     // DO DIFF CALC
				wt_LastPRSSelectDate  = "LastPRSSelectDate = convert(datetime, NULL), ";
				wt_PRSRewriteCount    = "PRSRewriteCount = -1, ";    // DO DIFF CALC
				wt_LastPRSRewriteDate = "LastPRSRewriteDate = convert(datetime, NULL), ";
			}

			// ASE 16.0
			String wt_Scans              = ""; // The number of scans on this object
			String wt_LastScanDate       = ""; // The date of the last scan on this object
			String wt_LastScanDateDiff   = ""; // ###: datediff(ms, LastDeleteDate, getdate())
			String wt_Updates            = ""; // The number of updates on this object
			String wt_LastUpdateDate     = ""; // The date of the last update on this object
			String wt_LastUpdateDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
			String wt_Inserts            = ""; // The date of the last update on this object
			String wt_LastInsertDate     = ""; // The date of the last insert on this object
			String wt_LastInsertDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
			String wt_Deletes            = ""; // The number of deletes on this object
			String wt_LastDeleteDate     = ""; // The date of the last delete on this object
			String wt_LastDeleteDateDiff = ""; // ###: datediff(ms, LastDeleteDate, getdate())
			String wt_LrPerScan          = "";

			if (srvVersion >= Ver.ver(16,0))
			{
				// note: protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days, 20 hours, 31 minutes and 23.647 seconds, the MS difference is overflowned
				
				wt_Scans              = "Scans = -1, ";              // DO DIFF CALC
				wt_LastScanDate       = "LastScanDate = convert(datetime, NULL), ";
				wt_LastScanDateDiff   = "LastScanDateDiff = -1, ";

				wt_Updates            = "Updates = -1, ";            // DO DIFF CALC
				wt_LastUpdateDate     = "LastUpdateDate = convert(datetime, NULL), ";
				wt_LastUpdateDateDiff = "LastUpdateDateDiff = -1, ";
		
				wt_Inserts            = "Inserts = -1, ";            // DO DIFF CALC
				wt_LastInsertDate     = "LastInsertDate = convert(datetime, NULL), ";
				wt_LastInsertDateDiff = "LastInsertDateDiff = -1, ";

				wt_Deletes            = "Deletes = -1, ";            // DO DIFF CALC
				wt_LastDeleteDate     = "LastDeleteDate = convert(datetime, NULL), ";
				wt_LastDeleteDateDiff = "LastDeleteDateDiff = -1, ";

				wt_LrPerScan          = "LrPerScan = convert(numeric(12,1), -1), ";
			}

			// ASE 16.0 SP3 PL4
			String wt_MaxInsRowsInXact = ""; // Max number of rows inserted in any transaction for this object
			String wt_MaxDelRowsInXact = ""; // Max number of rows deleted  in any transaction for this object
			String wt_MaxUpdRowsInXact = ""; // Max number of rows updated  in any transaction for this object

			if (srvVersion >= Ver.ver(16,0,0, 3,4))
			{
				wt_MaxInsRowsInXact = "MaxInsRowsInXact = -1, "; // do NOT do diff calc
				wt_MaxDelRowsInXact = "MaxDelRowsInXact = -1, "; // do NOT do diff calc
				wt_MaxUpdRowsInXact = "MaxUpdRowsInXact = -1, "; // do NOT do diff calc
			}

			
			String wt_cols1 = "";
			String wt_cols2 = "";
			String wt_cols3 = "";
			if (isClusterEnabled)
			{
				wt_cols1 += "InstanceID, ";
			}
			
			wt_cols1 += 
			         "DBID, ObjectID, \n" +
			         wt_DBName +
			         wt_ObjectName + 
			         "IndexID, \n" +
			         wt_IndexName +
			         "LockScheme = convert(varchar(30),'WORK-TABLE'), " +
			         "Remark = convert(varchar(60), ''), \n" + // this would be a good position after X tests has been done, but put it at the end right now
			         wt_Scans + wt_LastScanDate + nl_16000 + 
			         wt_LastScanDateDiff + nl_16000 +
			         "LockRequests = -1, LockWaits = -1, \n" +
			         "LockContPct  = convert(numeric(10,1), 0), \n" +
			         wt_SharedLockWaitTime + wt_ExclusiveLockWaitTime + wt_UpdateLockWaitTime + ase15700_nl +
			         wt_NumLevel0Waiters + wt_AvgLevel0WaitTime + ase1570_SP100_nl +
//			         "LogicalReads, PhysicalReads, APFReads, PagesRead, \n" +
			         wt_LrPerScan + "LogicalReads, PhysicalReads, APFReads = PhysicalAPFReads, PagesRead = -1, \n" + // Changes from the monOpenObjectActivity
			         wt_IOSize1Page + wt_IOSize2Pages + wt_IOSize4Pages + wt_IOSize8Pages + nl_15702 +
//			         "PhysicalWrites, PagesWritten, UsedCount, Operations, \n" +
			         "PhysicalWrites = -1, PagesWritten = -1, UsedCount = -1, Operations = -1, \n" + // Changes from the monOpenObjectActivity
			         wt_TabRowCount +
			         wt_UsageInMb + 
			         wt_UsageInKb + 
			         wt_NumUsedPages +
			         wt_RowsPerPage +
			         // RowsInserted + RowsDeleted + RowsUpdated : will overflow if much changes, so individual converts are neccecary
			         "RowsInsUpdDel=convert("+bigint+",-1), \n" +
//			         "RowsInserted, RowsDeleted, RowsUpdated, OptSelectCount, \n" +
			         "RowsInserted = -1, RowsDeleted = -1, RowsUpdated = -1, OptSelectCount = -1, \n" + // Changes from the monOpenObjectActivity
			         wt_MaxInsRowsInXact              + wt_MaxUpdRowsInXact              + wt_MaxDelRowsInXact   + nl_160_sp3_pl4 +
			         wt_Inserts                       + wt_Updates                       + wt_Deletes            + nl_16000 +
			         wt_LastInsertDateDiff + nl_16000 + wt_LastUpdateDateDiff + nl_16000 + wt_LastDeleteDateDiff + nl_16000 +
			         wt_LastInsertDate                + wt_LastUpdateDate                + wt_LastDeleteDate     + nl_16000 +
			         ""; // end of cols1
			wt_cols2 += "";
			wt_cols3 += wt_ObjectCacheDate + "LastOptSelectDate = convert(datetime, NULL), LastUsedDate = convert(datetime, NULL)";
			if (srvVersion >= Ver.ver(15,0,2))
			{
				wt_cols2 += "HkgcRequests = -1, HkgcPending = -1, HkgcOverflows = -1, \n";
			}
			if (srvVersion >= Ver.ver(15,7,0,1))
			{
				wt_cols2 += wt_HkgcRequestsDcomp + wt_HkgcPendingDcomp + wt_HkgcOverflowsDcomp + nl_15701;
			}
			if (srvVersion >= Ver.ver(15,7,0,2))
			{
				wt_cols2 += wt_PRSSelectCount + wt_LastPRSSelectDate + wt_PRSRewriteCount + wt_LastPRSRewriteDate + nl_15702;
			}

			// FINALLY: Compose a SQL Statement that fetches WORK-TABLES from monProcessObject
			workTableSql = "\n\n"
					+ "-----------------------------------------------------------------------\n"
					+ "-- Get 'work tables' from monProcessObject \n"
					+ "-- This is a 'union', that is resolved in AseTune (2 ResultSet's merged) \n"
					+ "-----------------------------------------------------------------------\n"
					+ "select \n"
					+ wt_cols1 + wt_cols2 + wt_cols3 + "\n"
					+ "from master.dbo.monProcessObject A\n"
					+ "where DBID = 2 \n"
					+ "  and ObjectName = 'temp worktable' \n"
					+ "";
			
			// If LOWER than ASE 15.5 --- RESET the WORK TABLE SQL
			if (srvVersion < Ver.ver(15,5))
			{
				_logger.info("Resetting 'WORK-TABLE' SQL, since version is to low. need version 15.5 and current version is "+srvVersion);
				workTableSql = "";
			}
			// If Cluster Edition --- RESET the WORK TABLE SQL
			if (isClusterEnabled)
			{
				_logger.info("Resetting 'WORK-TABLE' SQL, since it's a ASE Cluster Edition...");
				workTableSql = "";
			}
		}
		
		String sql = 
			"select " + topRows + cols1 + cols2 + cols3 + "\n" +
			"from master.dbo.monOpenObjectActivity A \n" +
			"where UsedCount > 0 OR LockRequests > 0 OR LogicalReads > 100 \n" +
//			(isClusterEnabled ? "order by 2,3,4" : "order by 1,2,3") + "\n";
			"order by LogicalReads desc \n" +
			workTableSql;

		return sql;
	}

	/**
	 * Called when a timeout has been found in the refreshGetData() method
	 */
	@Override
	public void handleTimeoutException()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// FIRST try to reset timeout if it's below the default
		if (getQueryTimeout() < getDefaultQueryTimeout())
		{
			if (conf.getBooleanProperty(PROPKEY_disable_tabRowCount_onTimeout, DEFAULT_disable_tabRowCount_onTimeout))
			{
				setQueryTimeout(getDefaultQueryTimeout(), true);
				_logger.warn("CM='"+getName()+"'. Setting Query Timeout to default of '"+getDefaultQueryTimeout()+"', from method handelTimeoutException().");
				return;
			}
		}

		// SECONDARY Disable the: TabRowCount, NumUsedPages, RowsPerPage
		// It might be that what causing the timeout
		if (conf.getBooleanProperty(PROPKEY_disable_tabRowCount_onTimeout, DEFAULT_disable_tabRowCount_onTimeout))
		{
			if (conf.getBooleanProperty(PROPKEY_sample_tabRowCount, DEFAULT_sample_tabRowCount) == true)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf == null) 
					return;
				tempConf.setProperty(PROPKEY_sample_tabRowCount, false);
				tempConf.setProperty(PROPKEY_disable_tabRowCount_timestamp, TimeUtils.toString(System.currentTimeMillis()));
				tempConf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				setSql(null);
	
				String key=PROPKEY_sample_tabRowCount;
				_logger.warn("CM='"+getName()+"'. Disabling the column 'TabRowCount', 'NumUsedPages', 'RowsPerPage', from method handelTimeoutException(). This is done by setting "+key+"=false");

				// DUMMY: check if config is set... because somewhere there is a problem when reading the value... (at least in no-gui mode)
				String dummyTest = tempConf.getProperty(PROPKEY_sample_tabRowCount);
				if (StringUtil.isNullOrBlank(dummyTest))
					_logger.warn("DEBUG: Disabling of '"+PROPKEY_sample_tabRowCount+"' was not successfull. the value is '"+dummyTest+"'. tempConf="+tempConf);
				else if ( ! dummyTest.trim().equalsIgnoreCase("false") )
					_logger.warn("DEBUG: Disabling of '"+PROPKEY_sample_tabRowCount+"' was not successfull. the value is '"+dummyTest+"'. tempConf="+tempConf);
				
				// DUMMY: check if config is set... because somewhere there is a problem when reading the value... (at least in no-gui mode)
				dummyTest = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sample_tabRowCount);
				if (StringUtil.isNullOrBlank(dummyTest))
					_logger.warn("DEBUG: Disabling of '"+PROPKEY_sample_tabRowCount+"' was not successfull. the value is '"+dummyTest+"'. from Configuration.getCombinedConfiguration()");
				else if ( ! dummyTest.trim().equalsIgnoreCase("false") )
					_logger.warn("DEBUG: Disabling of '"+PROPKEY_sample_tabRowCount+"' was not successfull. the value is '"+dummyTest+"'. from Configuration.getCombinedConfiguration()");

				// Show a popup, what we did if we are in GUI mode
				if (getGuiController() != null && getGuiController().hasGUI())
				{
					String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

					JOptionPane optionPane = new JOptionPane(
							"<html>" +
							"The query for CM '"+getName()+"' took to long... and received a Timeout.<br>" +
							"<br>" +
							"This may be caused by the function row_count(objid, dbid), which is used to get how many rows a table holds.<br>" +
							"In combination that someone was holding an exclusive table lock, which in some Ase Versions causes row_count() to block.<br>" +
							"This is only done when 'Sample Table Row Count' is enabled and ASE Version is lower than 15.7 SP130<br>" +
							"<br>" +
							"To Workaround this issue:<br>" +
							"I just disabled option 'Sample Table Row Count'... You can try to enable it again later.<br>" +
							"</html>",
							JOptionPane.INFORMATION_MESSAGE);
					JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "Disabled 'Sample Table Row Count' @ "+dateStr);
					dialog.setModal(false);
					dialog.setVisible(true);
				}
			}
		}
	}

	/**
	 * Lets reset some "timeout" options so that it will start from "scratch" again!
	 */
	@Override
	public void prepareForPcsDatabaseRollover()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// If "disable" on timeout is enabled
		if (conf.getBooleanProperty(PROPKEY_disable_tabRowCount_onTimeout, DEFAULT_disable_tabRowCount_onTimeout))
		{
			// Need TMP since we are going to save the configuration somewhere
			Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
			if (tempConf == null) 
				return;

			// check if the option is present, then remove it... This will cause it to use the value in the "base" configuration.
			if (tempConf.hasProperty(PROPKEY_sample_tabRowCount) || tempConf.hasProperty(PROPKEY_disable_tabRowCount_timestamp))
			{
				tempConf.remove(PROPKEY_sample_tabRowCount);
				tempConf.remove(PROPKEY_disable_tabRowCount_timestamp);
				tempConf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				setSql(null);
				
				_logger.info("CM='"+getName()+"'. Re-enable the column 'TabRowCount', 'NumUsedPages', 'RowsPerPage', from method prepareForPcsDatabaseRollover(). This is done by removing properties '" + PROPKEY_sample_tabRowCount + "', '" + PROPKEY_disable_tabRowCount_timestamp + "' from Configuration 'USER_TEMP' with file: " + tempConf.getFilename() );
			}
		}
	}

	/** 
	 * Compute the LockContPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int LockContPct_pos = -1;

		int LockRequests,          LockWaits;
		int LockRequests_pos = -1, LockWaits_pos = -1;
		
		int TabRowCount_pos  = -1; long TabRowCount;
		int NumUsedPages_pos = -1; long NumUsedPages;
		int RowsPerPage_pos  = -1; // we will SET this value, so only need the position

		// set some "Remark", below is columns user to draw some conclutions 
		int Remark_pos       = -1;
		int UsedCount_pos    = -1;
		int IndexID_pos      = -1;
		int LogicalReads_pos = -1;
		int RowsInserted_pos = -1;
		int PagesRead_pos    = -1;
		int APFReads_pos     = -1;
		int Scans_pos        = -1;
		int LrPerScan_pos    = -1;
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("LockRequests")) LockRequests_pos = colId;
			else if (colName.equals("LockWaits"))    LockWaits_pos    = colId;
			else if (colName.equals("LockContPct"))  LockContPct_pos  = colId;
			else if (colName.equals("TabRowCount"))  TabRowCount_pos  = colId;
			else if (colName.equals("NumUsedPages")) NumUsedPages_pos = colId;
			else if (colName.equals("RowsPerPage"))  RowsPerPage_pos  = colId;
			else if (colName.equals("Remark"))       Remark_pos       = colId;
			else if (colName.equals("UsedCount"))    UsedCount_pos    = colId;
			else if (colName.equals("IndexID"))      IndexID_pos      = colId;
			else if (colName.equals("LogicalReads")) LogicalReads_pos = colId;
			else if (colName.equals("RowsInserted")) RowsInserted_pos = colId;
			else if (colName.equals("PagesRead"))    PagesRead_pos    = colId;
			else if (colName.equals("APFReads"))     APFReads_pos     = colId;
			else if (colName.equals("Scans"))        Scans_pos        = colId;
			else if (colName.equals("LrPerScan"))    LrPerScan_pos    = colId;

			// Noo need to continue, we got all our columns
			if (    LockRequests_pos  >= 0 
			     && LockWaits_pos     >= 0 
			     && LockContPct_pos   >= 0  
			     && TabRowCount_pos   >= 0  
			     && NumUsedPages_pos  >= 0  
			     && RowsPerPage_pos   >= 0  
			     && Remark_pos        >= 0  
			     && UsedCount_pos     >= 0  
			     && IndexID_pos       >= 0  
			     && LogicalReads_pos  >= 0  
			     && RowsInserted_pos  >= 0  
			     && PagesRead_pos     >= 0  
			     && APFReads_pos      >= 0  
			   )
				break;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			LockRequests = ((Number)diffData.getValueAt(rowId, LockRequests_pos)).intValue();
			LockWaits    = ((Number)diffData.getValueAt(rowId, LockWaits_pos   )).intValue();

			//------------------------------
			// CALC: LockContPct
			int destColPos = LockContPct_pos;
			if (LockRequests > 0)
			{
				// LockWaits / LockRequests * 100;
				double calc = ((LockWaits+0.0) / (LockRequests+0.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, destColPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, destColPos);

			//------------------------------
			// CALC: RowsPerPage
			// If we got all columns: RowsPerPage, TabRowCount, NumUsedPages
			if (RowsPerPage_pos >= 0 && TabRowCount_pos >= 0 && NumUsedPages_pos >= 0 )
			{
				TabRowCount  = ((Number)diffData.getValueAt(rowId, TabRowCount_pos )).longValue();
				NumUsedPages = ((Number)diffData.getValueAt(rowId, NumUsedPages_pos)).longValue();

				destColPos = RowsPerPage_pos;
				if (NumUsedPages > 0)
				{
					// RowsPerPage = TabRowCount / NumUsedPages;
					double calc = ((TabRowCount+0.0) / (NumUsedPages+0.0));

					BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					diffData.setValueAt(newVal, rowId, destColPos);
				}
				else
					diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, destColPos);
			}

			//------------------------------
			// CALC: Remark
			// A LOT of stuff can be included here...
			if (Remark_pos >= 0 && UsedCount_pos >= 0 && IndexID_pos >= 0 )
			{
				int UsedCount    = ((Number)diffData.getValueAt(rowId, UsedCount_pos   )).intValue();
				int IndexID      = ((Number)diffData.getValueAt(rowId, IndexID_pos     )).intValue();
				int LogicalReads = ((Number)diffData.getValueAt(rowId, LogicalReads_pos)).intValue();
				int RowsInserted = ((Number)diffData.getValueAt(rowId, RowsInserted_pos)).intValue();
				int PagesRead    = ((Number)diffData.getValueAt(rowId, PagesRead_pos   )).intValue();
				int APFReads     = ((Number)diffData.getValueAt(rowId, APFReads_pos    )).intValue();
				
				String remark = null;
				if (IndexID == 0 && (PagesRead > 1000 || APFReads > 500))
				{
					remark = RemarkDictionary.PROBABLY_TABLE_SCAN;
				}
				if (IndexID == 0 && UsedCount > 0 && LogicalReads > 0)
				{
//					remark = RemarkDictionary.T_SCAN_OR_HTAB_INS;
					remark = RemarkDictionary.TABLE_SCAN;

					// Allow up to 10% variance of inserts and still consider it to be a "Table Scan"
					// But if it's more than that 10% inserts, then consider it as a "Heap Table Insert"
					// pctNear is just: 10% more or 10% less than the baseValue(UsedCount)
					if ( MathUtils.pctNear(10, UsedCount, RowsInserted) )
						remark = RemarkDictionary.HEAP_TAB_INS;
				}
				
				// If we got any remarks, set it...
				if ( ! StringUtil.isNullOrBlank(remark) )
				{
					destColPos = Remark_pos;
					diffData.setValueAt(remark, rowId, destColPos);
				}
			}
			
			//------------------------------
			// CALC: LrPerScan
			if (LogicalReads_pos != -1 && Scans_pos != -1 && LrPerScan_pos != -1)
			{
				destColPos = LrPerScan_pos;
				
				int Scans        = ((Number)diffData.getValueAt(rowId, Scans_pos        )).intValue();
				int LogicalReads = ((Number)diffData.getValueAt(rowId, LogicalReads_pos )).intValue();

				if (Scans > 0)
				{
					double calc = (LogicalReads+0.0) / (Scans+0.0);

					BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					diffData.setValueAt(newVal, rowId, destColPos);
				}
				else
				{
					diffData.setValueAt(new BigDecimal(-1).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, destColPos);
				//	diffData.setValueAt(null, rowId, destColPos);
				}
			}
		}
	} // end localCalculation

	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return 10;
	}

	@Override
	public String[] getDdlDetailsSortOnColName()
	{
		String[] sa = {"LogicalReads", "APFReads", "PhysicalReads", "LockWaits"};
		return sa;
	}
	
	@Override
	public Map<String, String> getPkRewriteMap(int modelRow)
	{
		List<String> pkList = getPk();
		if (pkList == null)
			return null;

		Map<String, String> map = new LinkedHashMap<>();
		for (String pkCol : pkList)
		{
			if      ("DBID"    .equals(pkCol)) { map.put("DBName",     getAbsString(modelRow, "DBName"));     }
			else if ("ObjectID".equals(pkCol)) { map.put("ObjectName", getAbsString(modelRow, "ObjectName")); }
			else                               { map.put(pkCol,        getAbsString(modelRow, pkCol));        }
		}
		return map;
	}
}
