package com.asetune.cm.ase;

import java.math.BigDecimal;
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
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmSpidWaitPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpidWait
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSpidWait.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpidWait.class.getSimpleName();
	public static final String   SHORT_NAME       = "SPID Wait";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What different resources are a Server SPID waiting for.<br>" +
		"<br>" +
		"<br>Note</b>: This is in experimental mode, it might take to much resources<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "process wait events=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"WaitTime", "Waits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSpidWait(counterController, guiController);
	}

	public CmSpidWait(ICounterController counterController, IGuiController guiController)
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
		
		// The flowing columns is part of difference calculation
		// But Disregarded in the filter "Do NOT show unchanged counter rows"
		// this means that even if they HAVE a value, the will be filtered OUT from the JTable
		setDiffDissColumns( new String[] {"WaitTime"} );

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
	}

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpidWaitPanel(this);
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
			mtd.addColumn("monProcessWaits", "WaitTimePerWait", 
				"<html>" +
					"Wait time in seconds per wait.<br>" +
					"Since WaitTime here is in seconds, this value will also be in seconds.<br>" +
					"<br>" +
					"<b>Formula</b>: diff.WaitTime / diff.Waits<br>" +
				"</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("KPID");
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
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause   = conf.getProperty       (PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  and " + sample_extraWhereClause + "\n";

		
		String cols = "";

		String InstanceID = ""; // in cluster
		String UserName   = ""; // in 15.0.2 esd#5

		if (isClusterEnabled)
			InstanceID = "W.InstanceID, ";

		if (aseVersion >= 15056)
			UserName = "UserName = suser_name(W.ServerUserID), ";

		cols = InstanceID + "W.SPID, W.KPID, " + UserName + "\n" +
			"Class=C.Description, Event=I.Description, \n" +
			"W.WaitEventID, W.WaitTime, W.Waits, \n" +
			"WaitTimePerWait = CASE WHEN W.Waits > 0 \n" +
			"                       THEN convert(numeric(15,3), (W.WaitTime + 0.0) / W.Waits) \n" +
			"                       ELSE convert(numeric(15,3), 0.0) \n" +
			"                  END \n";

		String sql = 
			"select " + cols +
			"from master..monProcessWaits W, tempdb.guest.monWaitEventInfo I, tempdb.guest.monWaitClassInfo C \n" +
			"where W.WaitEventID = I.WaitEventID \n" +
			"  and I.WaitClassID = C.WaitClassID \n" +
			sql_sample_extraWhereClause +
			"order by " + (isClusterEnabled ? "W.SPID, W.WaitEventID, W.InstanceID" : "W.SPID, W.WaitEventID") + "\n" +
			"";

		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_sample_extraWhereClause,  conf.getProperty(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_sample_extraWhereClause)) return CmSpidWaitPanel.TOOLTIP_sample_extraWhereClause;
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_sample_extraWhereClause)) return String .class.getSimpleName();
		return "";
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
