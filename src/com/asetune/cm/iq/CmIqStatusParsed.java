package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIqStatusParsed
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmIqStatusParsed.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqStatusParsed.class.getSimpleName();
	public static final String   SHORT_NAME       = "Parsed IQ Status";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqstatus_parsed"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"IntValue"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmIqStatusParsed(counterController, guiController);
	}

	public CmIqStatusParsed(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_VERSION             = "VersionGraph";
	public static final String GRAPH_NAME_COMPRESSION         = "CompressionGraph";
	public static final String GRAPH_NAME_CACHE_READS         = "CacheReadsGraph";
	public static final String GRAPH_NAME_CACHE_MAIN          = "CacheMainGraph";
	public static final String GRAPH_NAME_CACHE_TEMP          = "CacheMainTemp";
	
	private void addTrendGraphs()
	{
		String[] labels_version     = new String[] { "Active Txn Versions - Count", "Active Txn Versions - Created MB", "Active Txn Versions - Deleted MB", "Other Versions - Count", "Other Versions - MB" };
		String[] labels_compression = new String[] { "Main IQ I/O - Compression Ratio", "Temporary IQ I/O - Compression Ratio" };
		String[] labels_cache_reads = new String[] { "Main - Logical Read", "Temporary - Logical Read" };
		String[] labels_cache_main  = new String[] { "Physical Read", "Pages Created", "Pages Dirtied", "Physically Written", "Pages Destroyed" };
		String[] labels_cache_temp  = new String[] { "Physical Read", "Pages Created", "Pages Dirtied", "Physically Written", "Pages Destroyed" };

		addTrendGraphData(GRAPH_NAME_VERSION,     new TrendGraphDataPoint(GRAPH_NAME_VERSION,     labels_version));
		addTrendGraphData(GRAPH_NAME_COMPRESSION, new TrendGraphDataPoint(GRAPH_NAME_COMPRESSION, labels_compression));
		addTrendGraphData(GRAPH_NAME_CACHE_READS, new TrendGraphDataPoint(GRAPH_NAME_CACHE_READS, labels_cache_reads));
		addTrendGraphData(GRAPH_NAME_CACHE_MAIN,  new TrendGraphDataPoint(GRAPH_NAME_CACHE_MAIN,  labels_cache_main));
		addTrendGraphData(GRAPH_NAME_CACHE_TEMP,  new TrendGraphDataPoint(GRAPH_NAME_CACHE_TEMP,  labels_cache_temp));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			TrendGraph tg = null;

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_VERSION,
				"Versioning", 	                        // Menu CheckBox text
				"Versioning, using Absolute Values", // Label 
				labels_version, 
				false,  // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_COMPRESSION,
				"Compression Ratio", 	                        // Menu CheckBox text
				"Compression Ratio - in Percent, Absolute Values", // Label 
				labels_version, 
				true,  // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_CACHE_READS,
				"Cache Activity - Logical Reads", 	                        // Menu CheckBox text
				"Cache Activity - Logical Reads, per Second", // Label 
				labels_version, 
				false,  // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_CACHE_MAIN,
				"Cache IO Activity - Main", 	                        // Menu CheckBox text
				"Cache IO Activity - Main, per Second", // Label 
				labels_version, 
				false,  // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_CACHE_TEMP,
				"Cache IO Activity - Temporary", 	                        // Menu CheckBox text
				"Cache IO Activity - Temporary, per Second", // Label 
				labels_version, 
				false,  // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

		}
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
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addTable("sp_iqstatus_parsed",  "FIXME");

			mtd.addColumn("sp_iqstatus_parsed", "Name",          "<html>FIXME: Name</html>");
			mtd.addColumn("sp_iqstatus_parsed", "StrValue",      "<html>FIXME: StrValue</html>");
			mtd.addColumn("sp_iqstatus_parsed", "AddedEntry",    "<html>FIXME: AddedEntry</html>");
			mtd.addColumn("sp_iqstatus_parsed", "IntValue",      "<html>FIXME: IntValue</html>");
			mtd.addColumn("sp_iqstatus_parsed", "Unit",          "<html>FIXME: Unit</html>");
			mtd.addColumn("sp_iqstatus_parsed", "Description",   "<html>FIXME: Description</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Name");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n" +
			"    CASE \n" +
			"       WHEN trim(Name) = 'Main IQ Buffers:'      and Value like 'Used: %' THEN 'Main IQ Buffers X:' \n" +
			"       WHEN trim(Name) = 'Temporary IQ Buffers:' and Value like 'Used: %' THEN 'Temporary IQ Buffers X:' \n" +
			"       ELSE trim(Name) \n" +
			"    END as Name, \n" +
			"    Value                     as StrValue, \n" +
			"    convert(bit, 0)           as AddedEntry, \n" +
			"    CASE \n" +
			"       WHEN IsNumeric(Value) = 1 THEN convert(int, Value) \n" +
			"       ELSE -1 \n" +
			"    END as IntValue, \n" +
			"    convert(varchar(10),  '') as Unit, \n" +
			"    convert(varchar(255), '') as Description \n" +
			"from sp_iqstatus() \n";

		return sql;
	}

	@Override
	public void localCalculation(CounterSample newSample)
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int  Name_pos        = -1;
		int  StrValue_pos    = -1;

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Name"))        Name_pos        = colId;
			else if (colName.equals("StrValue"))    StrValue_pos    = colId;
		}

		if (Name_pos == -1)
			return;

		// Loop on all newSample rows, parse some of them, then add new records for the one parsed
		int rowc = newSample.getRowCount();
		for (int rowId = 0; rowId < rowc; rowId++)
		{
			String name     = (String) newSample.getValueAt(rowId, Name_pos);
			String strValue = (String) newSample.getValueAt(rowId, StrValue_pos);

			try
			{
				if ("Page Size:".equals(name)) // 131072/8192blksz/16bpp
				{
					String tmp = strValue.replace("blksz", "").replace("bpp", "");
					Scanner sc = new Scanner(tmp);
					sc.useDelimiter("/");

					addRow(newSample, "Page Size - KB:",         strValue, sc.nextInt()/1024, "KB",    "");
					addRow(newSample, "Page Size - Block Size:", strValue, sc.nextInt(),      "bytes", "");
					addRow(newSample, "Page Size - bpp:",        strValue, sc.nextInt(),      "bytes", "");
				}
				else if ("Main IQ Buffers:".equals(name)) // 510, 64Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);
					sc.useDelimiter(", ");

					addRow(newSample, "Main IQ Buffers - Count:", strValue, sc.nextInt(), "Count", "");
					addRow(newSample, "Main IQ Buffers - Size:",  strValue, sc.nextInt(), "MB", "");
				}
				else if ("Temporary IQ Buffers:".equals(name)) // 510, 64Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);
					sc.useDelimiter(", ");

					addRow(newSample, "Temporary IQ Buffers - Count:", strValue, sc.nextInt(), "Count", "");
					addRow(newSample, "Temporary IQ Buffers - Size:",  strValue, sc.nextInt(), "MB", "");
				}
				else if ("Main IQ Blocks Used:".equals(name)) // 15563 of 38400, 40%=121Mb, Max Block#: 37149
				{
					String tmp = strValue.replace("of ", "").replace("%=", " ").replace("Mb, Max Block#:", "").replace(",", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Main IQ Blocks Used - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Main IQ Blocks Used - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Main IQ Blocks Used - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Main IQ Blocks Used - Size:",  strValue, sc.nextInt(), "MB",      "");
					addRow(newSample, "Main IQ Blocks Used - Max:",   strValue, sc.nextInt(), "Blocks",  "");
				}
				else if ("Shared Temporary IQ Blocks Used:".equals(name)) // 0 of 0, 0%=0Mb, Max Block#: 0
				{
					String tmp = strValue.replace("of ", "").replace("%=", " ").replace("Mb, Max Block#:", "").replace(",", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Shared Temporary IQ Blocks Used - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Shared Temporary IQ Blocks Used - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Shared Temporary IQ Blocks Used - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Shared Temporary IQ Blocks Used - Size:",  strValue, sc.nextInt(), "MB",      "");
					addRow(newSample, "Shared Temporary IQ Blocks Used - Max:",   strValue, sc.nextInt(), "Blocks",  "");
				}
//				else if ("Shared Temporary IQ Blocks Used:".equals(name)) // 0 of 0, 0%=0Mb, Max Block#: 0
//				{
//					Pattern p = Pattern.compile("(\\d+) of (\\d+), (\\d+)%=(\\d+)Mb, Max Block#: (\\d+)");
//					Matcher m = p.matcher(strValue);
//System.out.println("name='"+name+"', strValue='"+strValue+"'.");
//System.out.println("m.groupCount()="+m.groupCount());
//for (int i = 1; i <= m.groupCount(); i++)
//	System.out.println(i+"=|"+m.group(i)+"|.");
//					addRow(newSample, "Shared Temporary IQ Blocks Used - v1:",  strValue, StringUtil.parseInt(m.group(1), -1), "Blocks",  "");
//					addRow(newSample, "Shared Temporary IQ Blocks Used - v2:",  strValue, StringUtil.parseInt(m.group(2), -1), "Blocks",  "");
//					addRow(newSample, "Shared Temporary IQ Blocks Used - v3:",  strValue, StringUtil.parseInt(m.group(3), -1), "Percent", "");
//					addRow(newSample, "Shared Temporary IQ Blocks Used - v4:",  strValue, StringUtil.parseInt(m.group(4), -1), "MB",      "");
//					addRow(newSample, "Shared Temporary IQ Blocks Used - v5:",  strValue, StringUtil.parseInt(m.group(5), -1), "Blocks",  "");
//				}
				else if ("Local Temporary IQ Blocks Used:".equals(name)) // 81 of 6400, 1%=0Mb, Max Block#: 162
				{
					String tmp = strValue.replace("of ", "").replace("%=", " ").replace("Mb, Max Block#:", "").replace(",", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Local Temporary IQ Blocks Used - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Local Temporary IQ Blocks Used - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Local Temporary IQ Blocks Used - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Local Temporary IQ Blocks Used - Size:",  strValue, sc.nextInt(), "MB",      "");
					addRow(newSample, "Local Temporary IQ Blocks Used - Max:",   strValue, sc.nextInt(), "Blocks",  "");
				}
				else if ("Main Reserved Blocks Available:".equals(name)) // 25600 of 25600, 100%=200Mb
				{
					String tmp = strValue.replace(" of ", " ").replace(", ", " ").replace("%=", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Main Reserved Blocks Available - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Main Reserved Blocks Available - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Main Reserved Blocks Available - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Main Reserved Blocks Available - Size:",  strValue, sc.nextInt(), "MB",      "");
				}
				else if ("Shared Temporary Reserved Blocks Available:".equals(name)) // 0 of 0, 0%=0Mb
				{
					String tmp = strValue.replace(" of ", " ").replace(", ", " ").replace("%=", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Shared Temporary Reserved Blocks Available - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Shared Temporary Reserved Blocks Available - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Shared Temporary Reserved Blocks Available - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Shared Temporary Reserved Blocks Available - Size:",  strValue, sc.nextInt(), "MB",      "");
				}
				else if ("Local Temporary Reserved Blocks Available:".equals(name)) // 6400 of 6400, 100%=50Mb
				{
					String tmp = strValue.replace(" of ", " ").replace(", ", " ").replace("%=", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Local Temporary Reserved Blocks Available - Used:",  strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Local Temporary Reserved Blocks Available - Total:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Local Temporary Reserved Blocks Available - Usage:", strValue, sc.nextInt(), "Percent", "");
					addRow(newSample, "Local Temporary Reserved Blocks Available - Size:",  strValue, sc.nextInt(), "MB",      "");
				}
				else if ("IQ Dynamic Memory:".equals(name)) // Current: 161mb, Max: 169mb
				{
					String tmp = strValue.replace("Current: ", "").replace("mb, Max: ", " ").replace("mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "IQ Dynamic Memory - Current:", strValue, sc.nextInt(), "MB",  "");
					addRow(newSample, "IQ Dynamic Memory - Max:",     strValue, sc.nextInt(), "MB",  "");
				}
				else if ("Main IQ Buffers X:".equals(name)) // Used: 220, Locked: 0
				{
					String tmp = strValue.replace("Used: ", "").replace(", Locked: ", " ");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Main IQ Buffers X - Used:",   strValue, sc.nextInt(), "Count",  "");
					addRow(newSample, "Main IQ Buffers X - Locked:", strValue, sc.nextInt(), "Count",  "");
				}
				else if ("Temporary IQ Buffers X:".equals(name)) // Used: 502, Locked: 0
				{
					String tmp = strValue.replace("Used: ", "").replace(", Locked: ", " ");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Temporary IQ Buffers X - Used:",   strValue, sc.nextInt(), "Count",  "");
					addRow(newSample, "Temporary IQ Buffers X - Locked:", strValue, sc.nextInt(), "Count",  "");
				}
				else if ("Main IQ I/O:".equals(name)) // I: L173911/P218 O: C2/D189/P188 D:0 C:78.6
				{
					String tmp = strValue.replace("I: L", "").replace("/P", " ").replace(" O: C", " ").replace("/D", " ").replace("/P", " ").replace(" D:", " ").replace(" C:", " ");
					tmp = tmp.substring(0, tmp.length()-2); // remove '.0' at the end: it cant be parsed 

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Main IQ I/O - Logical Read:",       strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Physical Read:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Pages Created:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Pages Dirtied:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Physically Written:", strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Pages Destroyed:",    strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Main IQ I/O - Compression Ratio:",  strValue, sc.nextInt(), "Percent", "");
				}
				else if ("Temporary IQ I/O:".equals(name)) // I: L4630665/P32948 O: C166817/D325925/P167508 D:166812 C:31.1
				{
					String tmp = strValue.replace("I: L", "").replace("/P", " ").replace(" O: C", " ").replace("/D", " ").replace("/P", " ").replace(" D:", " ").replace(" C:", " ");
					tmp = tmp.substring(0, tmp.length()-2); // remove '.0' at the end: it cant be parsed 

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Temporary IQ I/O - Logical Read:",       strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Physical Read:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Pages Created:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Pages Dirtied:",      strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Physically Written:", strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Pages Destroyed:",    strValue, sc.nextInt(), "Count",   "");
					addRow(newSample, "Temporary IQ I/O - Compression Ratio:",  strValue, sc.nextInt(), "Percent", "");
				}
				else if ("Other Versions:".equals(name)) // 0 = 0Mb
				{
					String tmp = strValue.replace(" = ", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Other Versions - Count:", strValue, sc.nextInt(), "Count", "");
					addRow(newSample, "Other Versions - Size:",  strValue, sc.nextInt(), "MB",    "");
				}
				else if ("Active Txn Versions:".equals(name)) // 0 = C:0Mb/D:0Mb
				{
					String tmp = strValue.replace(" = C:", " ").replace("Mb/D:", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Active Txn Versions - Count:",   strValue, sc.nextInt(), "Count",  "");
					addRow(newSample, "Active Txn Versions - Created:", strValue, sc.nextInt(), "MB",     "");
					addRow(newSample, "Active Txn Versions - Deleted:", strValue, sc.nextInt(), "MB",     "");
				}
				else if ("Blocks in next ISF Backup:".equals(name)) // 0 Blocks: =0Mb
				{
					String tmp = strValue.replace(" Blocks: =", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Blocks in next ISF Backup - Blocks:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Blocks in next ISF Backup - Space:",  strValue, sc.nextInt(), "MB",      "");
				}
				else if ("Blocks in next ISI Backup:".equals(name)) // 0 Blocks: =0Mb
				{
					String tmp = strValue.replace(" Blocks: =", " ").replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "Blocks in next ISI Backup - Blocks:", strValue, sc.nextInt(), "Blocks",  "");
					addRow(newSample, "Blocks in next ISI Backup - Space:",  strValue, sc.nextInt(), "MB",      "");
				}
				else if ("IQ large memory space:".equals(name)) // 0Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "IQ large memory space - Size:", strValue, sc.nextInt(), "MB",  "");
				}
				else if ("IQ large memory flexible used:".equals(name)) // 0Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "IQ large memory flexible used - Size:", strValue, sc.nextInt(), "MB",  "");
				}
				else if ("IQ large memory inflexible used:".equals(name)) // 0Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "IQ large memory inflexible used - Size:", strValue, sc.nextInt(), "MB",  "");
				}
				else if ("RLV memory limit:".equals(name)) // 2048Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "RLV memory limit - Size:", strValue, sc.nextInt(), "MB",  "");
				}
				else if ("RLV memory used:".equals(name)) // 0Mb
				{
					String tmp = strValue.replace("Mb", "");

					Scanner sc = new Scanner(tmp);

					addRow(newSample, "RLV memory used - Size:", strValue, sc.nextInt(), "MB",  "");
				}
			}
			catch(Exception ex)
			{
				_logger.warn(getName()+": Problems parsing entry name='"+name+"', strValue'"+strValue+"'. Caught: "+ex);
				_logger.debug(getName()+": Problems parsing entry name='"+name+"', strValue'"+strValue+"'. Caught: "+ex, ex);
				addRow(newSample, name+" ParseException",  strValue, -1, "ParseException",  "Caught: "+ex.toString());
			}
		}
System.out.println("AFTER: startRowc="+rowc+", endRowc="+newSample.getRowCount());
	}
	private void addRow(CounterSample newSample, String name, String strValue, int intValue, String unit, String desc)
	{
		List<Object> row = new ArrayList<Object>(5);
		row.add(name);
		row.add(strValue);
		row.add(true);
		row.add(intValue);
		row.add(unit);
		row.add(desc);
		newSample.addRow(row);
	}




	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		int srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_VERSION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[5];

			arr[0] = this.getAbsValue("Active Txn Versions - Count:",   "IntValue");
			arr[1] = this.getAbsValue("Active Txn Versions - Created:", "IntValue");
			arr[2] = this.getAbsValue("Active Txn Versions - Deleted:", "IntValue");
			arr[3] = this.getAbsValue("Other Versions - Count:",        "IntValue");
			arr[4] = this.getAbsValue("Other Versions - Size:",         "IntValue");

			//_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_COMPRESSION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValue("Main IQ I/O - Compression Ratio:",      "IntValue");
			arr[1] = this.getAbsValue("Temporary IQ I/O - Compression Ratio:", "IntValue");

			//_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CACHE_READS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValue("Main IQ I/O - Logical Read:",       "IntValue");
			arr[1] = this.getRateValue("Temporary IQ I/O - Logical Read:",  "IntValue");

			//_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CACHE_MAIN.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[5];

			arr[0] = this.getRateValue("Main IQ I/O - Physical Read:",      "IntValue");
			arr[1] = this.getRateValue("Main IQ I/O - Pages Created:",      "IntValue");
			arr[2] = this.getRateValue("Main IQ I/O - Pages Dirtied:",      "IntValue");
			arr[3] = this.getRateValue("Main IQ I/O - Physically Written:", "IntValue");
			arr[4] = this.getRateValue("Main IQ I/O - Pages Destroyed:",    "IntValue");

			//_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CACHE_TEMP.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[5];

			arr[0] = this.getRateValue("Temporary IQ I/O - Physical Read:",      "IntValue");
			arr[1] = this.getRateValue("Temporary IQ I/O - Pages Created:",      "IntValue");
			arr[2] = this.getRateValue("Temporary IQ I/O - Pages Dirtied:",      "IntValue");
			arr[3] = this.getRateValue("Temporary IQ I/O - Physically Written:", "IntValue");
			arr[4] = this.getRateValue("Temporary IQ I/O - Pages Destroyed:",    "IntValue");

			//_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

	}
}

//1> select * from sp_iqstatus()
//RS> Col# Label JDBC Type Name         Guessed DBMS type
//RS> ---- ----- ---------------------- -----------------
//RS> 1    Name  java.sql.Types.VARCHAR varchar(255)     
//RS> 2    Value java.sql.Types.VARCHAR varchar(128)     
//+-------------------------------------------+----------------------------------------------------------------+
//|Name                                       |Value                                                           |
//+-------------------------------------------+----------------------------------------------------------------+
//|Sybase IQ (TM)                             |Copyright (c) 1992-2013 by Sybase, Inc. All rights reserved.    |
//|Version:                                   |16.0.0.429/130316/P/GA/MS/Windows 2003/64bit/2013-03-16 21:30:09|
//|Time Now:                                  |2015-02-19 22:38:31.729                                         |
//|Build Time:                                |2013-03-16 21:30:09                                             |
//|File Format:                               |23 on 03/18/1999                                                |
//|Server mode:                               |IQ Server                                                       |
//|Catalog Format:                            |2                                                               |
//|Stored Procedure Revision:                 |1                                                               |
//|Page Size:                                 |131072/8192blksz/16bpp                                          |
//|Number of Main DB Files:                   |1                                                               |
//|Main Store Out Of Space:                   |N                                                               |
//|Number of Shared Temp DB Files:            |0                                                               |
//|Shared Temp Store Out Of Space:            |N                                                               |
//|Number of Local Temp DB Files:             |1                                                               |
//|Local Temp Store Out Of Space:             |N                                                               |
//|DB Blocks: 1-64000                         |IQ_SYSTEM_MAIN                                                  |
//|Local Temp Blocks: 1-12800                 |IQ_SYSTEM_TEMP                                                  |
//|Create Time:                               |2013-10-25 15:43:20.693                                         |
//|Update Time:                               |2015-02-19 11:36:15.000                                         |
//|Main IQ Buffers:                           |510, 64Mb                                                       |
//|Temporary IQ Buffers:                      |510, 64Mb                                                       |
//|Main IQ Blocks Used:                       |15563 of 38400, 40%=121Mb, Max Block#: 37149                    |
//|Shared Temporary IQ Blocks Used:           |0 of 0, 0%=0Mb, Max Block#: 0                                   |
//|Local Temporary IQ Blocks Used:            |65 of 6400, 1%=0Mb, Max Block#: 0                               |
//|Main Reserved Blocks Available:            |25600 of 25600, 100%=200Mb                                      |
//|Shared Temporary Reserved Blocks Available:|0 of 0, 0%=0Mb                                                  |
//|Local Temporary Reserved Blocks Available: |6400 of 6400, 100%=50Mb                                         |
//|IQ Dynamic Memory:                         |Current: 157mb, Max: 157mb                                      |
//|Main IQ Buffers:                           |Used: 4, Locked: 0                                              |
//|Temporary IQ Buffers:                      |Used: 4, Locked: 0                                              |
//|Main IQ I/O:                               |I: L530/P2 O: C2/D17/P16 D:0 C:100.0                            |
//|Temporary IQ I/O:                          |I: L542/P0 O: C4/D19/P18 D:0 C:100.0                            |
//|Other Versions:                            |0 = 0Mb                                                         |
//|Active Txn Versions:                       |0 = C:0Mb/D:0Mb                                                 |
//|Last Full Backup ID:                       |0                                                               |
//|Last Full Backup Time:                     |                                                                |
//|Last Backup ID:                            |0                                                               |
//|Last Backup Type:                          |None                                                            |
//|Last Backup Time:                          |                                                                |
//|DB Updated:                                |1                                                               |
//|Blocks in next ISF Backup:                 |0 Blocks: =0Mb                                                  |
//|Blocks in next ISI Backup:                 |0 Blocks: =0Mb                                                  |
//|IQ large memory space:                     |2048Mb                                                          |
//|IQ large memory flexible percentage:       |50                                                              |
//|IQ large memory flexible used:             |0Mb                                                             |
//|IQ large memory inflexible percentage:     |90                                                              |
//|IQ large memory inflexible used:           |0Mb                                                             |
//|IQ large memory anti-starvation percentage:|50                                                              |
//|DB File Encryption Status:                 |OFF                                                             |
//|RLV memory limit:                          |2048Mb                                                          |
//|RLV memory used:                           |0Mb                                                             |
//+-------------------------------------------+----------------------------------------------------------------+
//Rows 51
//(51 rows affected)




// FROM: http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc00170.1540/doc/html/san1288043219486.html

//The following is a key to understanding the Main IQ I/O and Temporary IQ I/O output codes:
//
//    I: Input
//
//    L: Logical pages read (“Finds”)
//
//    P: Physical pages read
//
//    O: Output
//
//    C Pages Created
//
//    D Pages Dirtied
//
//    P: Physically Written
//
//    D: Pages Destroyed
//
//    C: Compression Ratio
//
//Check the following information:
//
//    The lines Main IQ Blocks Used and Temporary IQ Blocks used tell you what portion of your dbspaces is in use. If the percentage of blocks in use (the middle statistic on these lines) is in the high nineties, you need to add a dbspace.
//
//    The Main IQ Blocks Used and Temporary IQ Blocks Used are calculated based on the line DB Blocks (Total Main IQ Blocks) minus Main Reserved Blocks Available and the line Temp Blocks (Total Temp IQ Blocks) minus Temporary Reserved Blocks Available since the Reserved Blocks cannot be used for user operations.
//
//    The lines Main IQ Buffers and Temporary IQ Buffers tell you the current sizes of your main and temp buffer caches.
//
//    Other Versions shows other db versions and the total space consumed. These versions will eventually be dropped when they are no longer referenced or referencable by active transactions.
//
//    Active Txn Versions shows the number of active write transactions and the amount of data they have created and destroyed. If these transactions commit, the “destroyed” data will become an old version and eventually be dropped. If they rollback, the “created” data will be freed.
//
//    Main Reserved Blocks Available and Temporary Reserved Blocks Available show the amount of reserved space that is available.
//
//    The lines Main IQ I/O and Temporary IQ I/O display I/O status in the same format as in the IQ message log.
//

