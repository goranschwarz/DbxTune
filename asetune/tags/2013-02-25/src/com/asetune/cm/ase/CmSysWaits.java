package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmSysWaitsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysWaits
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSysWaits.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysWaits.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waits";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What different resources are the ASE Server waiting for. <br>" +
		"<br>" +
		"<b>Tip:</b> Hover over the WaitEventID, then you get a tooltip trying to describe the WaitEventID." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSysWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "wait event timing=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"WaitTime", "Waits"};

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

		return new CmSysWaits(counterController, guiController);
	}

	public CmSysWaits(ICounterController counterController, IGuiController guiController)
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
		return new CmSysWaitsPanel(this);
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
		// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
		//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
		msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");

		return msgHandler;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monSysWaits", "WaitTimePerWait", "<html>" +
			                                                   "Wait time in seconds per wait. formula: diff.WaitTime / diff.Waits<br>" +
			                                                   "Since WaitTime here is in seconds, this value will also be in seconds." +
			                                                "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
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

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("WaitEventID");

		return pkCols;
	}

	@Override
	public String getSqlInitForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String monWaitInfoWhere = "";
		if (aseVersion >= 15700)
			monWaitInfoWhere = "where Language = ''en_US''";

		String sql =
			"/*------ Create permanent tables for monWaitEventInfo & monWaitClassInfo in tempdb. -------*/ \n" +
			"/*------ hopefully this is less expensive than doing the join via CIS -------*/ \n" +
			"if ((select object_id('tempdb.guest.monWaitEventInfo')) is null) \n" +
			"   exec('select * into tempdb.guest.monWaitEventInfo from master..monWaitEventInfo "+monWaitInfoWhere+"') \n" +
			"\n" +
			"if ((select object_id('tempdb.guest.monWaitClassInfo')) is null) \n" +
			"   exec('select * into tempdb.guest.monWaitClassInfo from master..monWaitClassInfo "+monWaitInfoWhere+"') \n" +
			"";
	
		return sql;
	}
	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";


		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		cols1 += "Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits \n";
		if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion < 15000) )
		{
		}
		cols2 += ", WaitTimePerWait = CASE WHEN Waits > 0 \n" +
		         "                         THEN convert(numeric(10,3), (WaitTime + 0.0) / Waits) \n" +
		         "                         ELSE convert(numeric(10,3), 0.0) \n" +
		         "                     END \n";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monSysWaits W, tempdb.guest.monWaitEventInfo I, tempdb.guest.monWaitClassInfo C \n" +
			"where W.WaitEventID=I.WaitEventID and I.WaitClassID=C.WaitClassID \n" +
			"order by " + (isClusterEnabled ? "W.WaitEventID, InstanceID" : "W.WaitEventID") + "\n";

		return sql;
	}

	/** 
	 * Compute the WaitTimePerWait for diff values
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		int WaitTime,        Waits;
		int WaitTimeId = -1, WaitsId = -1;

		double calcWaitTimePerWait;
		int WaitTimePerWaitId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitTimePerWait")) WaitTimePerWaitId = colId;
			else if (colName.equals("WaitTime"))        WaitTimeId        = colId;
			else if (colName.equals("Waits"))           WaitsId           = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			WaitTime = ((Number)diffData.getValueAt(rowId, WaitTimeId)).intValue();
			Waits    = ((Number)diffData.getValueAt(rowId, WaitsId   )).intValue();

			// int totIo = Reads + APFReads + Writes;
			if (Waits > 0)
			{
				// WaitTimePerWait = WaitTime / Waits;
				calcWaitTimePerWait = WaitTime / (Waits * 1.0);

				BigDecimal newVal = new BigDecimal(calcWaitTimePerWait).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, WaitTimePerWaitId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerWaitId);
		}
	}
}