package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.RemarkDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmObjectActivityPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmObjectActivity
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmObjectActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmObjectActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Objects";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Performance information about object/tables." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - An Index.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

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
		"PRSSelectCount", "PRSRewriteCount"};

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

		return new CmObjectActivity(counterController, guiController);
	}

	public CmObjectActivity(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmObjectActivityPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monOpenObjectActivity", "LockContPct"                    ,"<html>How many Lock Requests in percent was Blocked by another concurrent SPID's due to incompatible locking issues.<br><b>Note</b>: Do also considder number of LockWaits and not only the percentage.<br><b>Formula</b>: LockWaits / LockRequests * 100.0<br></html>");
			mtd.addColumn("monOpenObjectActivity", "TabRowCount"                    ,"<html>Table rowcount, using row_count(DBID, ObjectID) to get the count, so it can be a bit off from the actual number of rows.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+getName()+".TabRowCount=false</code>.</html>");
			mtd.addColumn("monOpenObjectActivity", "NumUsedPages"                   ,"<html>Number of Pages used by this table/index, using data_pages(DBID, ObjectID, IndexID) to get the value.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+getName()+".TabRowCount=false</code>.</html>");
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
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
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
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(getName()+".TabRowCount",  conf.getBooleanProperty(getName()+".TabRowCount", true));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(getName()+".TabRowCount")) return "Sample Table Row Count using ASE functions row_count() and data_pages()";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(getName()+".TabRowCount")) return Boolean.class.getSimpleName();
		return "";
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
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

		String TabRowCount  = "";
		String NumUsedPages = "";
		String RowsPerPage  = "";
		String DBName       = "DBName=db_name(A.DBID), \n";
		String ObjectName   = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID)), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
		String IndexName    = "";

		// ASE 15.7
		String SharedLockWaitTime    = "";
		String ExclusiveLockWaitTime = "";
		String UpdateLockWaitTime    = "";
		String ObjectCacheDate       = "";
		String ase15700_nl           = ""; // NL for this section

		if (aseVersion >= 15000)
			bigint = "bigint";

		if (aseVersion >= 15020)
		{
			TabRowCount  = "TabRowCount  = convert(bigint, row_count(A.DBID, A.ObjectID)),             -- Disable col with property: "+getName()+".TabRowCount=false\n";
			NumUsedPages = "NumUsedPages = convert(bigint, data_pages(A.DBID, A.ObjectID, A.IndexID)), -- Disable col with property: "+getName()+".TabRowCount=false\n";
			RowsPerPage  = "RowsPerPage  = convert(numeric(6,1), 0),                                   -- Disable col with property: "+getName()+".TabRowCount=false\n";
			DBName       = "A.DBName, \n";
//			ObjectName   = "A.ObjectName, \n";
			ObjectName   = "ObjectName = isnull(object_name(A.ObjectID, A.DBID), 'Obj='+A.ObjectName), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
			IndexName    = "IndexName = CASE WHEN IndexID=0 THEN convert(varchar(30),'DATA') \n" +
			               "                 ELSE convert(varchar(30), isnull(index_name(DBID, ObjectID, IndexID), '-unknown-')) \n" +
			               "            END, \n";

			// debug/trace
			Configuration conf = Configuration.getCombinedConfiguration();
			if (conf.getBooleanProperty(getName()+".ObjectName", false))
			{
				ObjectName = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
				_logger.info(getName()+".ObjectName=true, using the string '"+ObjectName+"' for ObjectName lookup.");
				ObjectName += ", \n";
			}
			if (conf.getBooleanProperty(getName()+".TabRowCount", true) == false)
			{
				TabRowCount  = "TabRowCount  = convert(bigint,-1), -- column is disabled, enable col with property: "+getName()+".TabRowCount=true\n";
				NumUsedPages = "NumUsedPages = convert(bigint,-1), -- column is disabled, enable col with property: "+getName()+".TabRowCount=true\n";
				RowsPerPage  = "RowsPerPage  = convert(bigint,-1), -- column is disabled, enable col with property: "+getName()+".TabRowCount=true\n";
				_logger.info(getName()+".TabRowCount=false, Disabling the column 'TabRowCount', 'NumUsedPages', 'RowsPerPage'.");
			}
		}
		if (aseVersion >= 15700)
		{
			SharedLockWaitTime    = "SharedLockWaitTime, ";
			ExclusiveLockWaitTime = "ExclusiveLockWaitTime, ";
			UpdateLockWaitTime    = "UpdateLockWaitTime, ";
			ObjectCacheDate       = "ObjectCacheDate, ";

			ase15700_nl           = "\n"; // NL for this section
		}

		// ASE 15.7.0 ESD#1
		String HkgcRequestsDcomp  = "";
		String HkgcPendingDcomp   = "";
		String HkgcOverflowsDcomp = "";
		String nl_15701           = ""; // NL for this section
		if (aseVersion >= 15701)
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
		if (aseVersion >= 15702)
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
		         "LockRequests=isnull(LockRequests,0), LockWaits=isnull(LockWaits,0), \n" +
		         "LockContPct = CASE WHEN isnull(LockRequests,0) > 0 \n" +
		         "                   THEN convert(numeric(10,1), ((LockWaits+0.0)/(LockRequests+0.0)) * 100.0) \n" +
		         "                   ELSE convert(numeric(10,1), 0.0) \n" +
		         "              END, \n" +
		         SharedLockWaitTime + ExclusiveLockWaitTime + UpdateLockWaitTime + ase15700_nl +
		         "LogicalReads, PhysicalReads, APFReads, PagesRead, \n" +
		         IOSize1Page + IOSize2Pages + IOSize4Pages + IOSize8Pages + nl_15702 +
		         "PhysicalWrites, PagesWritten, UsedCount, Operations, \n" +
		         TabRowCount +
		         NumUsedPages +
		         RowsPerPage +
		         // RowsInserted + RowsDeleted + RowsUpdated : will overflow if much changes, so individual converts are neccecary
		         "RowsInsUpdDel=convert("+bigint+",RowsInserted) + convert("+bigint+",RowsDeleted) + convert("+bigint+",RowsUpdated), \n" +
		         "RowsInserted, RowsDeleted, RowsUpdated, OptSelectCount, \n";
		cols2 += "";
		cols3 += ObjectCacheDate + "LastOptSelectDate, LastUsedDate";
	//	cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate, LastOptSelectDateDiff=datediff(ss,LastOptSelectDate,getdate()), LastUsedDateDiff=datediff(ss,LastUsedDate,getdate())";
	// it looked like we got "overflow" in the datediff sometimes... And I have newer really used these cols, so lets take them out for a while...
		if (aseVersion >= 15020)
		{
			cols2 += "HkgcRequests, HkgcPending, HkgcOverflows, \n";
		}
		if (aseVersion >= 15701)
		{
			cols2 += HkgcRequestsDcomp + HkgcPendingDcomp + HkgcOverflowsDcomp + nl_15701;
		}
		if (aseVersion >= 15702)
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

		if ( aseVersion >= 15030 && isClusterEnabled )
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
		if ( aseVersion >= 15501 && isClusterEnabled )
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

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monOpenObjectActivity A \n" +
			"where UsedCount > 0 OR LockRequests > 0 OR LogicalReads > 100 \n" +
//			(isClusterEnabled ? "order by 2,3,4" : "order by 1,2,3") + "\n";
			"order by LogicalReads desc \n";

		return sql;
	}

	/** 
	 * Compute the LockContPct for DIFF values
	 */
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
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
			int colPos = LockContPct_pos;
			if (LockRequests > 0)
			{
				// LockWaits / LockRequests * 100;
				double calc = ((LockWaits+0.0) / (LockRequests+0.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, colPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

			//------------------------------
			// CALC: RowsPerPage
			// If we got all columns: RowsPerPage, TabRowCount, NumUsedPages
			if (RowsPerPage_pos >= 0 && TabRowCount_pos >= 0 && NumUsedPages_pos >= 0 )
			{
				TabRowCount  = ((Number)diffData.getValueAt(rowId, TabRowCount_pos )).longValue();
				NumUsedPages = ((Number)diffData.getValueAt(rowId, NumUsedPages_pos)).longValue();

				colPos = RowsPerPage_pos;
				if (NumUsedPages > 0)
				{
					// RowsPerPage = TabRowCount / NumUsedPages;
					double calc = ((TabRowCount+0.0) / (NumUsedPages+0.0));

					BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					diffData.setValueAt(newVal, rowId, colPos);
				}
				else
					diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);
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
				
				String remark = null;
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
					colPos = Remark_pos;
					diffData.setValueAt(remark, rowId, colPos);
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
}
