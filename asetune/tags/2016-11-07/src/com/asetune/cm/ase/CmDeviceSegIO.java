package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDeviceSegIO
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmDeviceSegIO.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDeviceSegIO.class.getSimpleName();
	public static final String   SHORT_NAME       = "Segment IO";
	public static final String   HTML_DESC        = 
		"<html>" +
		"On what Segments do we do IO on." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15700;
//	public static final int      NEED_SRV_VERSION = 1570000;
	public static final int      NEED_SRV_VERSION = Ver.ver(16,0,0, 2); // 16.0 SP2
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monDeviceSegmentIO"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {""};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PhysicalReads",
		"LogicalReads",
		"PhysicalWrites",
		"_last_dummy_column_which_do_not_exists_in_resultset_"};

// Adaptive Server Enterprise/16.0 SP02 Beta 3/EBF 24693 SMP/P/x86_64/Enterprise Linux/asecorona/0/64-bit/OPT/Tue Jun  9 10:47:20 2015

//	RS> Col# Label          JDBC Type Name         Guessed DBMS type
//	RS> ---- -------------- ---------------------- -----------------
//	RS> 1    DBID           java.sql.Types.INTEGER int              
//	RS> 2    DeviceNumber   java.sql.Types.INTEGER int              
//	RS> 3    SegmentNumber  java.sql.Types.INTEGER int              
//	RS> 4    PhysicalReads  java.sql.Types.BIGINT  bigint           
//	RS> 5    LogicalReads   java.sql.Types.BIGINT  bigint           
//	RS> 6    PhysicalWrites java.sql.Types.BIGINT  bigint           	

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmDeviceSegIO(counterController, guiController);
	}

	public CmDeviceSegIO(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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
//		return new CmDeviceSegIOPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DBID");
		pkCols.add("DeviceNumber");
		pkCols.add("SegmentNumber");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
				"select \n  \n" + 
				"    dbname = db_name(DBID), \n" +  
				"    deviceName = d.name,  \n" + 
				"    segmentName = case  \n" + 
				"                    when s.SegmentNumber = 0 then 'system' \n" + 
				"                    when s.SegmentNumber = 1 then 'default' \n" + 
				"                    when s.SegmentNumber = 2 then 'logsegment' \n" + 
				"                    else 'use_defined_segment_' + convert(varchar(10),s.SegmentNumber) \n" + 
				"                  end, \n" + 
				"    s.*  \n" + 
				"from master.dbo.monDeviceSegmentIO s, \n" +  
				"     master.dbo.sysdevices d  \n" + 
				"where s.DeviceNumber = d.vdevno  \n" + 
				"  and d.cntrltype = 0 \n";
		return sql;
	}
}