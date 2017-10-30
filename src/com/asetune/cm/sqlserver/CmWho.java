package com.asetune.cm.sqlserver;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmWhoPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWho
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWho.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWho.class.getSimpleName();
	public static final String   SHORT_NAME       = "Who";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>What SybasePIDs are doing what.</p>" +
			"<br>" +
			"<b>Tip:</b><br>" +
			"Sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>" +
			"Or check 'WaitEventDesc' to find out when the SPID is waiting for." +
			"<br><br>" +
			"Table Background colors:" +
			"<ul>" +
			"    <li>YELLOW - SPID is a System Processes</li>" +
			"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
			"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
			"    <li>ORANGE - SPID has an open transaction.</li>" +
			"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysprocesses"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cpu",
		"physical_io",
		"memusage"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmWho(counterController, guiController);
	}

	public CmWho(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads       = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads       = true;

	private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads, CmWhoPanel.TOOLTIP_sample_systemThreads ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWhoPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("spid");
		pkCols.add("kpid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads  = conf.getBooleanProperty(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);

		// Should we sample SYSTEM SPID's
//		String sql_sample_systemThreads = "--and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
//		if ( ! sample_systemThreads )
//			sql_sample_systemThreads = "  and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		String sql_sample_systemThreads = "--and net_address != '' -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		if ( ! sample_systemThreads )
			sql_sample_systemThreads = "  and net_address != '' -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";

		String sql = 
			"select  \n" +
			"	spid, \n" +
			"	kpid, \n" +
			"	loginame, \n" +
			"	cmd, \n" +
			"	status, \n" +
			"	blocked, \n" +
			"	open_tran, \n" +
			"	waittype, \n" +
			"	waittime, \n" +
			"	lastwaittype, \n" +
			"	waitresource, \n" +
			"	dbname = db_name(dbid), \n" +
			"	uid, \n" +
			"	cpu, \n" +
			"	physical_io, \n" +
			"	memusage, \n" +
			"	login_time, \n" +
			"	last_batch, \n" +
//			"	last_batch_ss = datediff(ss, last_batch, getdate()), \n" +
			"	last_batch_ss = CASE WHEN datediff(day, last_batch, getdate()) >= 24 THEN -1 ELSE  datediff(ss, last_batch, getdate()) END, \n" +
			"	hostname, \n" +
			"	program_name, \n" +
			"	hostprocess, \n" +
			"	nt_domain, \n" +
			"	nt_username, \n" +
			"	net_address, \n" +
			"	net_library, \n" +
			"	stmt_start, \n" +
			"	stmt_end, \n" +
			"	request_id, \n" +
			"	ecid, \n" +
			"	sql_handle, \n" +
			"	sid, \n" +
			"	context_info \n" +
			"from sys.sysprocesses \n" +
			"where 1 = 1 \n" +
			sql_sample_systemThreads;

		return sql;
	}


	/** 
	 * Maintain the _blockingSpids Map, which is accessed from the Panel
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		// Where are various columns located in the Vector 
		int pos_BlockingSPID = -1;
		CounterSample counters = diffData;
	
		if (counters == null)
			return;

		// Reset the blockingSpids Map
		_blockingSpids.clear();
		
		// put the pointer to the Map in the Client Property of the JTable, which should be visible for various places
		if (getTabPanel() != null)
			getTabPanel().putTableClientProperty("blockingSpidMap", _blockingSpids);

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if (colName.equals("blocked"))  pos_BlockingSPID  = colId;

			// Noo need to continue, we got all our columns
			if (pos_BlockingSPID >= 0)
				break;
		}

		if (pos_BlockingSPID < 0)
		{
			_logger.debug("Can't find the position for columns ('blocked'="+pos_BlockingSPID+")");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			Object o_blockingSpid = counters.getValueAt(rowId, pos_BlockingSPID);

			// Add any blocking SPIDs to the MAP
			if (o_blockingSpid instanceof Number)
			{
				if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
					_blockingSpids.put((Number)o_blockingSpid, null);
			}
		}
	}

}
