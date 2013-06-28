package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.gui.MainFrame;
import com.asetune.utils.AseConnectionUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRaSysmon
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmRaSysmon.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRaSysmon.class.getSimpleName();
	public static final String   SHORT_NAME       = "RepAgent Sysmon";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Grabs the raw counters for RepAgents from sysmonitors.</p>" +
		"NOTE: reuses data from 'Spinlock Sum', so this needs to be running as well.<br>" +
		"For the moment consider this as <b>very experimental</b>." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_REP_AGENT;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 12540;
	public static final int      NEED_SRV_VERSION = 1254000;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysmonitors"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable rep agent threads"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"value", "log_waits", "sum_log_wait", 
		"longest_log_wait", "truncpt_moved", "truncpt_gotten", "rs_connect", 
		"fail_rs_connect", "io_send", "sum_io_send_wait", "longest_io_send_wait", 
		"io_recv", "sum_io_recv_wait", "longest_io_recv_wait", "packets_sent", 
		"full_packets_sent", "sum_packet", "largest_packet",  "log_records_scanned", 
		"log_records_processed", "log_scans", "sum_log_scan", "longest_log_scan", 
		"open_xact", "maintuser_xact", "commit_xact", "abort_xact", "prepare_xact", 
		"xupdate_processed", "xinsert_processed", "xdelete_processed", 
		"xexec_processed", "xcmdtext_processed", "xwrtext_processed", 
		"xrowimage_processed", "xclr_processed", "xckpt_processed", 
		"xckpt_genxactpurge", "sqldml_processed", "bckward_schema", 
		"sum_bckward_wait", "longest_bckward_wait", "forward_schema", 
		"sum_forward_wait", "longest_forward_wait", "delayed_commit_xact", 
		"schema_reuse"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmRaSysmon(counterController, guiController);
	}

	public CmRaSysmon(ICounterController counterController, IGuiController guiController)
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
		
		// Need stored proc 'sp_asetune_ra_stats'
		// check if it exists: if not it will be created in super.init(conn)
		addDependsOnStoredProc("sybsystemprocs", "sp_asetune_ra_stats", 
			VersionInfo.SP_ASETUNE_RA_STATS_CRDATE, VersionInfo.class, 
			"sp_asetune_ra_stats.sql", AseConnectionUtils.SA_ROLE, NEED_SRV_VERSION);

		addDependsOnCm(CmSpinlockSum.CM_NAME); // CMspinlockSum must have been executed before this cm

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
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("sysmonitors", "value",                 "<html>The Counter value for this raw counter name.</html>");
			mtd.addColumn("sysmonitors", "log_waits",             "<html>Log Extension Wait:                Count</html>");
			mtd.addColumn("sysmonitors", "sum_log_wait",          "<html>Log Extension Wait:                Amount of time (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_log_wait",      "<html>Log Extension Wait:                Longest Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "truncpt_moved",         "<html>Truncation Point Movement:         Moved</html>");
			mtd.addColumn("sysmonitors", "truncpt_gotten",        "<html>Truncation Point Movement:         Gotten from RS</html>");
			mtd.addColumn("sysmonitors", "rs_connect",            "<html>Connections to Replication Server: Success</html>");
			mtd.addColumn("sysmonitors", "fail_rs_connect",       "<html>Connections to Replication Server: Failed</html>");
			mtd.addColumn("sysmonitors", "io_send",               "<html>I/O Wait from RS:                  Send, Count</html>");
			mtd.addColumn("sysmonitors", "sum_io_send_wait",      "<html>I/O Wait from RS:                  Send, Amount of Time (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_io_send_wait",  "<html>I/O Wait from RS:                  Send, Longest Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "io_recv",               "<html>I/O Wait from RS:                  Receive, Count</html>");
			mtd.addColumn("sysmonitors", "sum_io_recv_wait",      "<html>I/O Wait from RS:                  Receive, Amount of Time (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_io_recv_wait",  "<html>I/O Wait from RS:                  Receive, Longest Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "packets_sent",          "<html>Network Packet Information:        Packets Sent</html>");
			mtd.addColumn("sysmonitors", "full_packets_sent",     "<html>Network Packet Information:        Full Packets Sent</html>");
			mtd.addColumn("sysmonitors", "sum_packet",            "<html>Network Packet Information:        Amount of Bytes Sent</html>");
			mtd.addColumn("sysmonitors", "largest_packet",        "<html>Network Packet Information:        Largest Packet</html>");
			mtd.addColumn("sysmonitors", "log_records_scanned",   "<html>Log Scan Summary:                  Log Records Scanned</html>");
			mtd.addColumn("sysmonitors", "log_records_processed", "<html>Log Scan Summary:                  Log Records Processed</html>");
			mtd.addColumn("sysmonitors", "log_scans",             "<html>Log Scan Summary:                  Number of Log Scans</html>");
			mtd.addColumn("sysmonitors", "sum_log_scan",          "<html>Log Scan Summary:                  Amount of Time for Log Scans (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_log_scan",      "<html>Log Scan Summary:                  Longest Time for Log Scan (ms)</html>");
			mtd.addColumn("sysmonitors", "open_xact",             "<html>Transaction Activity:              Opened</html>");
			mtd.addColumn("sysmonitors", "maintuser_xact",        "<html>Transaction Activity:              Maintenance User</html>");
			mtd.addColumn("sysmonitors", "commit_xact",           "<html>Transaction Activity:              Commited</html>");
			mtd.addColumn("sysmonitors", "abort_xact",            "<html>Transaction Activity:              Aborted</html>");
			mtd.addColumn("sysmonitors", "prepare_xact",          "<html>Transaction Activity:              Prepared</html>");
			mtd.addColumn("sysmonitors", "delayed_commit_xact",   "<html>Transaction Activity:              Delayed Commit</html>");
			mtd.addColumn("sysmonitors", "xupdate_processed",     "<html>Log Scan Activity:                 Updates</html>");
			mtd.addColumn("sysmonitors", "xinsert_processed",     "<html>Log Scan Activity:                 Inserts</html>");
			mtd.addColumn("sysmonitors", "xdelete_processed",     "<html>Log Scan Activity:                 Deletes</html>");
			mtd.addColumn("sysmonitors", "xexec_processed",       "<html>Log Scan Activity:                 Store Procedures</html>");
			mtd.addColumn("sysmonitors", "xcmdtext_processed",    "<html>Log Scan Activity:                 DDL Log Records</html>");
			mtd.addColumn("sysmonitors", "xwrtext_processed",     "<html>Log Scan Activity:                 Writetext Log Records</html>");
			mtd.addColumn("sysmonitors", "xrowimage_processed",   "<html>Log Scan Activity:                 Text/Image Log Records</html>");
			mtd.addColumn("sysmonitors", "xclr_processed",        "<html>Log Scan Activity:                 CLRs</html>");
			mtd.addColumn("sysmonitors", "xckpt_processed",       "<html>Log Scan Activity:                 Checkpoints Processed</html>");
			mtd.addColumn("sysmonitors", "xckpt_genxactpurge",    "<html>Log Scan Activity:                 </html>");
			mtd.addColumn("sysmonitors", "sqldml_processed",      "<html>Log Scan Activity:                 SQL Statements Processed</html>");
			mtd.addColumn("sysmonitors", "bckward_schema",        "<html>Backward Schema Lookups:           Count</html>");
			mtd.addColumn("sysmonitors", "sum_bckward_wait",      "<html>Backward Schema Lookups:           Total Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_bckward_wait",  "<html>Backward Schema Lookups:           Longest Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "forward_schema",        "<html>Schema Cache:                      Count</html>");
			mtd.addColumn("sysmonitors", "sum_forward_wait",      "<html>Schema Cache:                      Total Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "longest_forward_wait",  "<html>Schema Cache:                      Longest Wait (ms)</html>");
			mtd.addColumn("sysmonitors", "schema_reuse",          "<html>Schema Cache:                      Schemas reused</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "exec sp_asetune_ra_stats ";
		return sql;
	}
}
