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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorAse;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmTableStatisticsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTableStatistics
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableStatistics.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Statistics";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Sample information from systabstats and derived_stats() for all databases.<br>" +
		"<b>Note</b>: This can be heavy, especially if you enable <i>Sample Space Usage</i> functionality that uses <code>used_pages()</code> and <code>reserved_pages(). Which are used in columns 'PageUtilization', 'ActualDataPages', 'ActualIndexPages', 'ReservedPages'</code>.<br>" +
		"<b>Note</b>: set postpone time to 10 minutes or so, we don't need to sample this that often." +
		"<br>" +
		"Colors used:" +
		"<ul>" +
		"    <li>Orange - Indexes</li>" +
		"    <li>Very Light Blue - Locking Schema: APL - All Page Locking</li>" +
		"    <li>Very Light Pink - Table do <b>not</b> have any indexes</li>" +
		"    <li>Yellow - System Tables</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(15, 0);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"systabstats"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
//	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmTableStatistics(counterController, guiController);
	}

	public CmTableStatistics(ICounterController counterController, IGuiController guiController)
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
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_spaceUsage             = PROP_PREFIX + ".sample.spaceUsage";
	public static final boolean DEFAULT_sample_spaceUsage             = false;

	public static final String  PROPKEY_disable_spaceUsage_onTimeout  = PROP_PREFIX + ".disable.spaceUsage.onTimeoutException";
	public static final boolean DEFAULT_disable_spaceUsage_onTimeout  = true;

//	public static final String  PROPKEY_sample_objectName             = PROP_PREFIX + ".sample.ObjectName";
//	public static final boolean DEFAULT_sample_objectName             = false;

	public static final String  PROPKEY_sample_minPageLimit           = PROP_PREFIX + ".sample.minPageLimit";
	public static final boolean DEFAULT_sample_minPageLimit           = true;

	public static final String  PROPKEY_sample_minPageLimitCount      = PROP_PREFIX + ".sample.minPageLimit.count";
	public static final int     DEFAULT_sample_minPageLimitCount      = 1000;

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	public static final String  PROPKEY_sample_partitions             = PROP_PREFIX + ".sample.partitions";
	public static final boolean DEFAULT_sample_partitions             = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_spaceUsage,            DEFAULT_sample_spaceUsage);
		Configuration.registerDefaultValue(PROPKEY_disable_spaceUsage_onTimeout, DEFAULT_disable_spaceUsage_onTimeout);
//		Configuration.registerDefaultValue(PROPKEY_sample_objectName,            DEFAULT_sample_objectName);
		Configuration.registerDefaultValue(PROPKEY_sample_minPageLimit,          DEFAULT_sample_minPageLimit);
		Configuration.registerDefaultValue(PROPKEY_sample_minPageLimitCount,     DEFAULT_sample_minPageLimitCount);
		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,          DEFAULT_sample_systemTables);
		Configuration.registerDefaultValue(PROPKEY_sample_partitions,            DEFAULT_sample_partitions);
	}
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmTableStatisticsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

//	@Override
//	protected CmSybMessageHandler createSybMessageHandler()
//	{
//		CmSybMessageHandler msgHandler = super.createSybMessageHandler();
//
//		//msgHandler.addDiscardMsgStr("Usage information at date and time");
//		msgHandler.addDiscardMsgNum(0);
//
//		return msgHandler;
//	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample Space Usage",            PROPKEY_sample_spaceUsage        , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spaceUsage        , DEFAULT_sample_spaceUsage        ), DEFAULT_sample_spaceUsage       , "Sample Table Space Usage with ASE functions data_pages() and reserved_pages()" ));
		list.add(new CmSettingsHelper("Minimum Number of Pages",       PROPKEY_sample_minPageLimit      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_minPageLimit      , DEFAULT_sample_minPageLimit      ), DEFAULT_sample_minPageLimit     , "Sample table that has X number of pages or more"                               ));
		list.add(new CmSettingsHelper("Minimum Number of Pages Count", PROPKEY_sample_minPageLimitCount , Integer.class, conf.getIntProperty    (PROPKEY_sample_minPageLimitCount , DEFAULT_sample_minPageLimitCount ), DEFAULT_sample_minPageLimitCount, "Table must have more pages than this to be included"                           ));
		list.add(new CmSettingsHelper("Include System Tables",         PROPKEY_sample_systemTables      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables      , DEFAULT_sample_systemTables      ), DEFAULT_sample_systemTables     , "Include ASE System Tables."                                                    ));
		list.add(new CmSettingsHelper("Stats at Partition Level",      PROPKEY_sample_partitions        , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_partitions        , DEFAULT_sample_partitions        ), DEFAULT_sample_partitions       , "Sample statistics on a partition level, not summarized at the table level"     ));

		return list;
	}


	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NO PK is needed, because NO diff calc is done.
		return null;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("systabstats",  "systabstats contains one row for each clustered index, one row for each nonclustered index, one row for each table that has no clustered index, and one row for each partition.");

			mtd.addColumn("systabstats",  "DBName",     "<html>Name of the database</html>");
			mtd.addColumn("systabstats",  "OwnerName",  "<html>Table owner</html>");
			mtd.addColumn("systabstats",  "ObjectName",  "<html>Name of the table</html>");
			mtd.addColumn("systabstats",  "PartitionName", "<html>Name of the table partition. This is only visible if the Partition checkbox is checked.</html>");
			mtd.addColumn("systabstats",  "PartitionCount", "<html>" +
			                                                   "Number of table partitions.<br>" +
			                                                   "This is basically the average time it took to make a <b>read</b> IO on this device.<br>" +
			                                                   "<b>Formula</b>: sum() on records in systabstat for a specific object <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IndexCount",   "<html>" +
			                                                   "How many indexes does the table have.<br>" +
			                                                   "-1: if the row is an index.<br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IndexName",   "<html>Name of the index</html>");
			mtd.addColumn("systabstats",  "IndexID",     "<html>" +
			                                                   "ID of the index.<br>" + 
			                                                   " 0 = DATA<br>" +
			                                                   " 1 = Clustered Index<br>" +
			                                                   " > 1 = A non clustered index, <br>" +
			                                                   " 255 = Text or Image... which is <b>not</b> presented here.<br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "LockSchema",  "<html>" +
			                                                   "Locking schema the table is using. Could be: 'allpages', 'datarows' or 'datapages'<br>" +
			                                                   "<b>Formula</b>: lockscheme(tableid, dbid)<br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IsClusteredIndex", "<html>" +
			                                                   "If this index is clustered index or not.<br>" +
			                                                   "<b>Formula</b>: (sysindexes.status & 2) = 2, or (sysindexes.status2 & 256)= 256<br>" +
			                                                  "</html>");
			mtd.addColumn("systabstats",  "IsUniqueIndex", "<html>" +
					                                           "If this index is unique or not.<br>" +
					                                           "<b>Formula</b>: (sysindexes.status & 16) = 16<br>" +
			                                               "</html>");
			mtd.addColumn("systabstats",  "IndexOnPK",    "<html>" +
			                                                   "If this index is clustered index or not.<br>" +
			                                                   "<b>Formula</b>: (sysindexes.status & 2048) = 2048<br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "CreationDate", "<html>" +
			                                                   "When the table or index was created.<br>" +
			                                                   "Note: Partition creation is not handled.<br>" +
			                                                   "<b>Formula</b>: sysindexes.crdate <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "StatModDate",  "<html>" +
			                                                   "Last time the row was flushed to disk.<br>" +
			                                                   "Note: This is not the same as when last time update index statistics was executed.<br>" +
			                                                   "<b>Formula</b>: sysindexes.statmoddate <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "LastUpdateStatsDate",  "<html>" +
			                                                   "Lst time we run <code>update [index] statistics</code> on the table.<br>" +
			                                                   "Note: This is only done on data (indexid 0 or 1).<br>" +
			                                                   "Note: Not sure this is 100% correct...<br>" +
			                                                   "<b>Formula</b>: select min(stat.moddate) from dbo.sysstatistics stat where stat.formatid = 104 and stat.id = S.id -- formatid:104='Historgam Cell Weight' <br>" +
			                                             "</html>");
			mtd.addColumn("systabstats",  "DataChange",  "<html>" +
			                                                   "Tries to figure out when it's time to do <b>update [index] statistics</b> on a table.<br>" +
			                                                   "The <code>datachange</code> function measures the amount of change in the data distribution since <b>update statistics</b> last ran. <br>" +
			                                                   "Specifically, it measures the number of <b>inserts</b>, <b>updates</b>, and <b>deletes</b> that have occurred on the given object, partition, or column.<br>" +
			                                                   "So it helps you determine if running update statistics would benefit the query plan <br>" +
			                                                   "Usage: For more information, google: sybase datachange.<br>" +
			                                                   "Usage: You can also write logic that issues <code>update index statistics</code> based on the value from datachange().<br>" +
			                                                   "Tip: <i>Menu -&gt; Tools -&gt; Predefined SQL Statements -&gt; sp__updateIndexStat</i> Can help you generate SQL Statements for update index statistics based on the datachange() function.<br>" +
			                                                   "<b>Formula</b>: datachange(tabname, null, null) <br>" +
			                                             "</html>");
			mtd.addColumn("systabstats",  "SizeInMb",   "<html>" +
			                                                   "Size in MB.<br>" +
			                                                   "<b>Formula</b>: systabstat.pagecnt or systabstat.leafcnt / (1024*1024/@@maxpagesize) <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "PageCount", "<html>" +
			                                                   "How many rows does the table contain.<br>" +
			                                                   "<b>Formula</b>: systabstat.pagecnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IndexLeafCount", "<html>" +
			                                                   "How many pages does the leaf level of the index has.<br>" +
			                                                   "<b>Formula</b>: systabstat.leafcnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IndexHeight",  "<html>" +
			                                                   "Height of the index; maintained if indid is greater than 1 (how many pages from root page to leaf page).<br>" +
			                                                   "<b>Formula</b>: systabstat.indexheight <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "EmptyPageCount",     "<html>" +
			                                                   "Number of empty pages in extents allocated to the table or index.<br>" +
			                                                   "<b>Formula</b>: systabstat.emptypgcnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "RowsPerPage",  "<html>" +
			                                                   "How many rows (in average) does each of the pages contains.<br>" +
			                                                   "Note: Only reported on data pages (on indexes 'systabstat.pagecnt' is not maintained).<br>" +
			                                                   "<b>Formula</b>: systabstat.rowcnt / systabstat.pagecnt<br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "TableRowCount",    "<html>" +
			                                                   "Number of rows in the table; maintained for indid of 0 or 1<br>" +
			                                                   "<b>Formula</b>: systabstat.rowcnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "ForwardRowCount",   "<html>" +
			                                                   "Number of forwarded rows; maintained for indid of 0 or 1<br>" +
			                                                   "<b>Formula</b>: systabstat.forwrowcnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "DeletedRowCount", "<html>" +
			                                                   "Number of deleted rows in DOL tables that is logically deleted but not yet physicaly deleted.<br>" +
			                                                   "<b>Formula</b>: systabstat.delrowcnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "DataPageClusterRatio", "<html>" +
			                                                   "The data page cluster ratio for the object/index pair.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "<b>Formula</b>: derived_stat(tabname, indid, 'dpcr') <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "IndexPageClusterRatio",     "<html>" +
			                                                   "The index page cluster ratio for the object/index pair.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "<b>Formula</b>: derived_stat(tabname, indid, 'ipcr') <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "DataRowClusterRatio",  "<html>" +
			                                                   "The data row cluster ratio for the object/index pair.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "<b>Formula</b>: derived_stat(tabname, indid, 'drcr') <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "PageUtilization",    "<html>" +
			                                                   "Whats the ratio between reserved and actually used datapages.<br>" +
			                                                   "Note: Only reported on datapages.<br>" +
			                                                   "Note: This doesn't say anything how the pages are utilized (if they are half empty or not), it's just on a Extent level.<br>" +
			                                                   "Note: This may take a long time to sample. and is only sampled if option 'Sample Space Usage' is enabled.<br>" +
			                                                   "<b>Formula</b>: data_pages(dbid, tabid, 0) / reserved_pages(dbid, tabid, 0) <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "SpaceUtilization",   "<html>" +
			                                                   "The space utilization for the object/index pair.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "<b>Formula</b>: derived_stat(tabname, indid, 'sput') <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "LargeIoEfficiency", "<html>" +
			                                                   "The large I/O efficiency for the object/index pair.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "<b>Formula</b>: derived_stat(tabname, indid, 'lgio') <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "ActualDataPages", "<html>" +
			                                                   "Actual pages used to this table.<br>" +
			                                                   "Note: This may take a long time to sample. and is only sampled if option 'Sample Space Usage' is enabled.<br>" +
			                                                   "<b>Formula</b>: data_pages(dbid, tabid, 0) <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "ActualIndexPages",     "<html>" +
			                                                   "Actual pages used to this index.<br>" +
			                                                   "Note: This may take a long time to sample. and is only sampled if option 'Sample Space Usage' is enabled.<br>" +
			                                                   "<b>Formula</b>: data_pages(dbid, tabid, indid) <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "ReservedPages",  "<html>" +
			                                                   "Actual pages that is allocated for this table or index .<br>" +
			                                                   "Note: This may take a long time to sample. and is only sampled if option 'Sample Space Usage' is enabled.<br>" +
			                                                   "<b>Formula</b>: reserved_pages(dbid, tabid, 0 or 1 or indid) <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "DataPageUtilization",    "<html>" +
			                                                   "Try to figgure out how well used the data pages are actually used/filled.<br>" +
			                                                   "Usage: FIXME.<br>" +
			                                                   "Note: This is only valid for DATA or ClusteredIndex pages (indid 0 or 1).<br>" +
			                                                   "<b>Formula</b>: ((pagesize-pageHeader) / sysindexes.maxlen) / systabstat.pagecnt <br>" +
			                                              "</html>");
			mtd.addColumn("systabstats",  "DBID",         "<html>ID of the database</html>");
			mtd.addColumn("systabstats",  "OwnerID",      "<html>User ID for the owner of the table</html>");
			mtd.addColumn("systabstats",  "ObjectID",     "<html>The ASE Internal identifier of the table</html>");
			mtd.addColumn("systabstats",  "PartitionID",  "<html>The ASE Internal identifier of the specific partition.</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"tempdb"} );
		return new CounterSampleCatalogIteratorAse(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		boolean sample_spaceUsage        = conf.getBooleanProperty(PROPKEY_sample_spaceUsage,        DEFAULT_sample_spaceUsage);
		boolean sample_minPageLimit      = conf.getBooleanProperty(PROPKEY_sample_minPageLimit,      DEFAULT_sample_minPageLimit);
		int     sample_minPageLimitCount = conf.getIntProperty    (PROPKEY_sample_minPageLimitCount, DEFAULT_sample_minPageLimitCount);
		boolean sample_systemTables      = conf.getBooleanProperty(PROPKEY_sample_systemTables,      DEFAULT_sample_systemTables);
		boolean sample_partitions        = conf.getBooleanProperty(PROPKEY_sample_partitions,        DEFAULT_sample_partitions);

		// sample_spaceUsage
		String sqlCol_PageUtilization  = "	PageUtilization       = convert(bigint, -1), -- property '"+PROPKEY_sample_spaceUsage+"' is disabled.\n";
		String sqlCol_ActualDataPages  = "	ActualDataPages       = convert(bigint, -1), -- property '"+PROPKEY_sample_spaceUsage+"' is disabled.\n";
		String sqlCol_ActualIndexPages = "	ActualIndexPages      = convert(bigint, -1), -- property '"+PROPKEY_sample_spaceUsage+"' is disabled.\n";
		String sqlCol_ReservedPages    = "	ReservedPages         = convert(bigint, -1), -- property '"+PROPKEY_sample_spaceUsage+"' is disabled.\n";
		if (sample_spaceUsage)
		{
			sqlCol_PageUtilization  = "	PageUtilization       = convert(numeric(10,2), CASE WHEN I.indid > 1 OR reserved_pages(@dbid, I.id, 0) = 0 THEN NULL ELSE 100.0 * data_pages(@dbid, I.id, 0) / reserved_pages(@dbid, I.id, 0) END), \n";
			sqlCol_ActualDataPages  = "	ActualDataPages       = data_pages(@dbid, I.id, CASE WHEN I.indid in (0,1) THEN 0 ELSE -1 END), \n";
			sqlCol_ActualIndexPages = "	ActualIndexPages      = data_pages(@dbid, I.id, CASE WHEN I.indid in (0,1) THEN 1 ELSE I.indid END), \n";
			sqlCol_ReservedPages    = "	ReservedPages         = CASE WHEN I.indid != 1 THEN reserved_pages(@dbid, I.id, I.indid) ELSE reserved_pages(@dbid, I.id, 0)+reserved_pages(@dbid, I.id, 1) END, \n";
		}

		// sample_minPageLimit
		String sqlWhere_MinPageLimit = "  and (S.pagecnt + S.leafcnt) > 1 -- Property '"+PROPKEY_sample_minPageLimit+"' is '"+sample_minPageLimit+"' and '"+PROPKEY_sample_minPageLimitCount+"' to "+sample_minPageLimitCount+".\n";
		if (sample_minPageLimit)
		{
			sqlWhere_MinPageLimit    = "  and (S.pagecnt + S.leafcnt) > "+sample_minPageLimitCount+" -- Property '"+PROPKEY_sample_minPageLimit+"' is '"+sample_minPageLimit+"' and '"+PROPKEY_sample_minPageLimitCount+"' to "+sample_minPageLimitCount+".\n";
		}

		// sample_systemTables
		String sqlWhere_SystemTables = "  and S.id              > 100 -- SKIP: system tables... Property '"+PROPKEY_sample_systemTables+"' is '"+sample_systemTables+"'. \n";
		if (sample_systemTables)
		{
			sqlWhere_SystemTables    = "--and S.id              > 100 -- SKIP: system tables... Property '"+PROPKEY_sample_systemTables+"' is '"+sample_systemTables+"'.\n";
		}

		// sample_partitions
		String sqlCol_datachange     = "	DataChange            = isnull( (CASE WHEN (S.indid in (0,1)) THEN datachange(O.name, null, null) ELSE null END), -1), \n";
		String sqlCol_partirionName  = "	PartitionName         = convert(varchar(255), null), \n";
		String sqlCol_partirionCount = "	PartitionCount        = S.partition_cnt, \n";
		String sqlCol_partirionId    = "	PartitionID           = convert(int, -1) \n"; // NOTE Last column so no ',' at the end
		String sqlFrom_partitions = ""
				+ "	( select -- make the below select a 'virtual' table, aliased to S for systabstats, the SUM() is to sumarize all partitions (if there are any) \n"
				+ "			id,  \n"
				+ "			indid,  \n"
				+ "			indexheight   = avg(1.0 * indexheight), -- Height of the index; maintained if indid is greater than 1 \n"
				+ "			statmoddate   = min(      statmoddate), -- Last time the row was flushed to disk \n"
				+ "			partition_cnt = sum(      1          ), -- Number of partition \n"
				+ "			pagecnt       = sum(1.0 * pagecnt    ), -- Number of pages in the table or index \n"
				+ "			leafcnt       = sum(1.0 * leafcnt    ), -- Number of leaf pages in the index; maintained if indid is greater than 1 \n"
				+ "			emptypgcnt    = sum(1.0 * emptypgcnt ), -- Number of empty pages in extents allocated to the table or index \n"
				+ "			rowcnt        = sum(1.0 * rowcnt     ), -- Number of rows in the table; maintained for indid of 0 or 1 \n"
				+ "			forwrowcnt    = sum(1.0 * forwrowcnt ), -- Number of forwarded rows; maintained for indid of 0 or 1 \n"
				+ "			delrowcnt     = sum(1.0 * delrowcnt  )  -- Number of deleted rows \n"
				+ "		from systabstats \n"
				+ "		group by id,indid \n"
				+ "	) S,  \n";
		if (sample_partitions)
		{
			sqlCol_datachange     = "	DataChange            = isnull( (CASE WHEN (S.indid in (0,1)) THEN datachange(O.name, partition_name(S.indid, S.partitionid, @dbid), null) ELSE null END), -1), \n";
			sqlCol_partirionName  = "	PartitionName         = partition_name(S.indid, S.partitionid, @dbid), \n";
			sqlCol_partirionCount = "	PartitionCount        = convert(int, -1), \n";
			sqlCol_partirionId    = "	PartitionID           = S.partitionid \n"; // NOTE Last column so no ',' at the end
			sqlFrom_partitions = "	systabstats S,  \n";
		}

		String sql = ""
				+ "-----------------------------------------------------------------------------------------\n"
				+ "-- The following SQL statement is executed for EACH database that is in **NORMAL** state \n"
				+ "-- So the statement will have database context. \n"
				+ "-----------------------------------------------------------------------------------------\n"
				+ "declare @dbname varchar(255) \n"
				+ "declare @dbid   int \n"
				+ "\n"
				+ "-- What database are we currently in \n"
				+ "select @dbname = db_name(), @dbid = db_id() \n"
				+ "\n"
				+ "-- Get the desired statistics \n"
				+ "select \n"
				+ "	DBName                = @dbname, \n"
				+ "	OwnerName             = U.name, \n"
				+ "	ObjectName            = O.name, \n"
				+ sqlCol_partirionName
				+ sqlCol_partirionCount
				+ "	IndexCount            = CASE WHEN S.indid = 0 \n"
				+ "	                             THEN (select count(*) from sysindexes SI where S.id = SI.id and SI.indid > 0 and SI.indid < 255) \n"
				+ "	                             ELSE -1  \n"
				+ "	                        END,  \n"
	    		+ "	IndexName             = CASE WHEN S.indid = 0  \n"
	    		+ "	                             THEN convert(varchar(30),'-DATA-')  \n"
	    		+ "	                             ELSE I.name \n"
	    		+ "	                        END, \n"
	    		+ "	IndexID               = S.indid, \n"
	    		+ "	LockSchema            = lockscheme(O.id, @dbid), \n"
	    		+ "	                             -- i.status:  16  = Table is an all-pages-locked table with a clustered index. \n"
	    		+ "	                             -- I.status2: 512 = Table is an data-only-locked table with a clustered index \n"
	    		+ "	IsClusteredIndex      = convert(bit, CASE WHEN (I.status & 16) = 16 OR (I.status2 & 512) = 512  \n"
	    		+ "	                                          THEN 1  \n"
	    		+ "	                                          ELSE 0  \n"
	    		+ "	                                     END), \n"
	    		+ "	IsUniqueIndex         = convert(bit, CASE WHEN (I.status & 2) = 2 \n"
	    		+ "	                                          THEN 1  \n"
	    		+ "	                                          ELSE 0  \n"
	    		+ "	                                     END), \n"
	    		+ "	IndexOnPK             = convert(bit, CASE WHEN (I.status & 2048) = 2048 \n"
	    		+ "	                                          THEN 1  \n"
	    		+ "	                                          ELSE 0  \n"
	    		+ "	                                     END), \n"
	    		+ "	CreationDate          = I.crdate, \n"
	    		+ "	StatModDate           = S.statmoddate, \n"
	    		+ "	LastUpdateStatsDate   = CASE WHEN (S.indid in (0,1)) \n"
	    		+ "	                             THEN (select min(stat.moddate) from dbo.sysstatistics stat where stat.formatid = 100 and stat.id = S.id and len(stat.colidarray) = 1 and stat.indid = 0 and stat.statid = 0) \n"
	    		+ "	                             ELSE null \n"
	    		+ "	                        END, -- formatid:100='Column Stats' \n"
	    		+ sqlCol_datachange
	    		+ "	SizeInMb              = convert(numeric(10,1), CASE WHEN (S.indid in (0,1)) \n"
	    		+ "	                                                    THEN (S.pagecnt / (1024.0*1024.0/@@maxpagesize)) \n"
	    		+ "	                                                    ELSE (S.leafcnt / (1024.0*1024.0/@@maxpagesize)) \n"
	    		+ "	                                               END), \n"
	    		+ "	PageCount             = S.pagecnt, \n"
	    		+ "	IndexLeafCount        = S.leafcnt, \n"
	    		+ "	IndexHeight           = S.indexheight, \n"
	    		+ "	EmptyPageCount        = S.emptypgcnt, \n"
	    		+ "	RowsPerPage           = convert(numeric(10,2), CASE WHEN (S.indid in (0,1) AND S.pagecnt > 0) THEN (1.0 * S.rowcnt / S.pagecnt) \n"
	    		+ "	                                                    WHEN (S.indid > 1      AND S.leafcnt > 0) THEN (1.0 * S.rowcnt / S.leafcnt) \n"
	    		+ "	                                                    ELSE 0 \n"
	    		+ "	                                               END), \n"
	    		+ "	TableRowCount         = convert(numeric(10,0), S.rowcnt), \n"
	    		+ "	ForwardRowCount       = convert(numeric(10,0), S.forwrowcnt), \n"
	    		+ "	DeletedRowCount       = convert(numeric(10,0), S.delrowcnt), \n"
	    		+ "	DataPageClusterRatio  = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'dpcr')), \n"
	    		+ "	IndexPageClusterRatio = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'ipcr')), \n"
	    		+ "	DataRowClusterRatio   = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'drcr')), \n"
	    		+ sqlCol_PageUtilization
	    		+ "	SpaceUtilization      = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'sput')), \n"
	    		+ "	LargeIoEfficiency     = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'lgio')), \n"
	    		+ sqlCol_ActualDataPages
				+ sqlCol_ActualIndexPages
				+ sqlCol_ReservedPages
	    		+ "	DataPageUtilization   = convert (numeric(10,2), \n"
	    		+ "	    CASE WHEN S.indid in (0,1) -- IndexID: 0 = DataPages, or 1 = ClusteredIndexPages \n"
	    		+ "	    THEN -- approx algorithm (I guess): ((pagesize-pageHeader) / sysindexes.maxlen) / systabstat.pagecnt \n"
	    		+ "	        CASE WHEN (sysstat2 & 16384) = 16384 OR (sysstat2 & 32768) = 32768 -- sysstat2: 16384 = DOL-datapages, 32768 = DOL-datarows \n"
	    		+ "	             THEN -- DOL: explain what we do here \n"
	    		+ "	                 ceiling(1.0 * (CASE WHEN S.rowcnt = 0 THEN 1 ELSE S.rowcnt END) / floor((@@maxpagesize-46)/((CASE WHEN I.maxlen<10 THEN 10 ELSE I.maxlen END)+2))) / S.pagecnt \n"
	    		+ "	             ELSE -- APL: explain what we do here \n"
	    		+ "	                 ceiling(1.0 * (CASE WHEN S.rowcnt = 0 THEN 1 ELSE S.rowcnt END) / CASE WHEN floor((@@maxpagesize-32)/(I.maxlen+2)) > 255 THEN 255 ELSE floor((@@maxpagesize-32)/(I.maxlen+2)) END) / S.pagecnt \n"
	    		+ "	        END \n"
	    		+ "	    ELSE  \n"
	    		+ "	        null  \n"
	    		+ "	    END), \n"
	    		+ "	DBID                  = convert(int, @dbid), \n"
	    		+ "	OwnerID               = O.uid, \n"
	    		+ "	ObjectID              = O.id, \n"
	    		+ sqlCol_partirionId
	    		+ "from  \n"
	    		+ sqlFrom_partitions
				+ "	sysindexes I,  \n"
	    		+ "	sysobjects O,  \n"
	    		+ "	sysusers U \n"
	    		+ "where S.id               = I.id \n"
	    		+ "  and S.indid            = I.indid \n"
	    		+ "  and S.id               = O.id \n"
	    		+ "  and O.uid              = U.uid \n"
	    		+ "  and O.sysstat2 & 1024  = 0  -- SKIP: 1024 = Table is proxy tables  \n"
	    		+ "  and O.sysstat2 & 2048  = 0  -- SKIP: 2048 = Table is a proxy table created with the existing keyword. \n"
	    		+ "  and S.id              != 8  -- SKIP: syslogs \n"
	    		+ sqlWhere_SystemTables
	    		+ sqlWhere_MinPageLimit
	    		+ "\n";

		return sql;

		// http://www.softwaregems.com.au/Documents/Sybase%20GEM%20Documents/Sybase%20Statistics%20Demystified.pdf
		// sysstatistics.formatid
		// +----------+-----------+--------------------------
		// | formatid | colidarr  | 
		// +----------+-----------+--------------------------
		// | 100      | 1 entry   | Column statistics
		// | 100      | > 1 entry | Column Group statistics
		// | 102      | 1 entry   | Histogram Cell Value 
		// | 104      | 1 entry   | Histogram Cell Weight
		// | 108      | 1 entry   | DataChange Counter
		// | 110      | 1 entry   | Column Missing
		// | 110      | > 1 entry | Column Group Missing
		// +----------+-----------+--------------------------
		//


//		String sql = ""
//				+ "/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n"
//				+ "if ((select object_id('#dbList')) is not null) drop table #dbList \n"
//				+ "go \n"
//	    		+ " \n"
//				+ "-----------------------------------------------------------------------------------\n"
//				+ "-- I choosed to use a 'dummy cursor' here: This because if we received timeout errors or similar, there is no way (that I know) how to check if a curor is already open/declared \n"
//				+ "-- In other words we get error: There is already another cursor with the name 'dbCursor' at the nesting level '0'. \n"
//				+ "-- and we need to close the connection to the database to resolve the issue, while the 'table loop' do not have that problem. \n"
//				+ "-----------------------------------------------------------------------------------\n"
//	    		+ " \n"
//				+ "/*------ Create tempdb objects that holds a list of databases to check -------*/  \n"
//				+ "SELECT dbid, name \n"
//				+ "INTO  #dbList \n"
//				+ "FROM  master.dbo.sysdatabases \n"
//	    		+ "WHERE name not like 'tempdb%' \n"
//	    		+ "  AND name not in ('master', 'sybsecurity', 'sybsystemdb', 'sybsystemprocs','model', 'tempdb') \n"
//	    		+ "  AND (status  & 32)      != 32      -- ignore: Database created with for load option, or crashed while loading database, instructs recovery not to proceed \n"
//	    		+ "  AND (status2 & 256)     != 256     -- ignore: Table structure written to disk. If this bit appears after recovery completes, server may be under-configured for open databases. Use sp_configure to increase this parameter. \n"
//	    		+ "  AND (status3 & 2)       != 2       -- ignore: Database is a proxy database created by high availability. \n"
//	    		+ "  AND (status3 & 4)       != 4       -- ignore: Database has a proxy database created by high availability \n"
//	    		+ "  AND (status3 & 256)     != 256     -- ignore: User-created tempdb. \n"
//	    		+ "  AND (status3 & 4194304) != 4194304 -- ignore: archive databases \n"
//	    		+ "ORDER BY dbid \n"
//	    		+ " \n"
//	    		+ "-- If we havn't got any databases in the correct status, throw in 'tempdb', just so we return a ResultSet, even it it's empty \n"
//	    		+ "if (@@rowcount = 0) \n"
//	    		+ "begin \n"
//	    		+ "	insert into #dbList values(2, 'tempdb') \n"
//	    		+ "end \n"
//	    		+ "go \n"
//	    		+ " \n"
//	    		+ "-- Local variable, used for the 'cursor' \n"
//	    		+ "declare @dbname varchar(255) \n"
//	    		+ "declare @dbid   int \n"
//	    		+ "declare @rowcnt int \n"
//	    		+ " \n"
//	    		+ "-- Get first row from the 'cursor' \n"
//	    		+ "set rowcount 1 \n"
//	    		+ "select @dbid = dbid, @dbname = name from #dbList \n"
//	    		+ "select @rowcnt = @@rowcount \n"
//	    		+ "delete #dbList where dbid = @dbid \n"
//	    		+ "set rowcount 0 \n"
//	    		+ " \n"
//	    		+ "-- Loop all databases in the list \n"
//	    		+ "while (@rowcnt = 1)  \n"
//	    		+ "begin \n"
//	    		+ "--	print 'DEBUG ------------- >>> dbid=%1!, dbname=%2!', @dbid, @dbname \n"
//	    		+ " \n"
//	    		+ "	/* \n"
//	    		+ "	** The exec() is needed beacuse we composes a string which is executed \n"
//	    		+ "	** this was needed because the from TABLENAME cant use variables to reference tables \n"
//	    		+ "	** TIP: comment out the next line 'exec(' if you have a Editor that does Syntax Highlightning for better readability \n"
//	    		+ "	*/ \n"
//	    		+ "	exec(\" \n"
//	    		+ "	select \n"
//	    		+ "		DBName                = @dbname, \n"
//	    		+ "		OwnerName             = U.name, \n"
//	    		+ "		ObjectName            = O.name, \n"
////	    		+ "		PartitionName         = partition_name(S.indid, S.partitionid, @dbid), \n" // just prepare if we ever want to do it at a partition level
//	    		+ "		PartitionCount        = S.partition_cnt, \n"
//	    		+ "		IndexName             = CASE WHEN S.indid = 0  \n"
//	    		+ "		                             THEN convert(varchar(30),'-DATA-')  \n"
//	    		+ "		                             ELSE I.name \n"
//	    		+ "		                        END, \n"
//	    		+ "		IndexID               = S.indid, \n"
//	    		+ "		LockSchema            = lockscheme(O.id, @dbid), \n"
//	    		+ "		                             -- i.status:  16  = Table is an all-pages-locked table with a clustered index. \n"
//	    		+ "		                             -- I.status2: 512 = Table is an data-only-locked table with a clustered index \n"
////	    		+ "	--	HasClusteredIndexStr  = CASE WHEN (I.status & 16) = 16 OR (I.status2 & 512) = 512 THEN 'true ' ELSE 'false' END, \n"
//	    		+ "		HasClusteredIndex     = convert(bit, CASE WHEN (I.status & 16) = 16 OR (I.status2 & 512) = 512  \n"
//	    		+ "		                                          THEN 1  \n"
//	    		+ "		                                          ELSE 0  \n"
//	    		+ "		                                     END), \n"
//	    		+ "		StatModDate           = S.statmoddate, \n"
////	    		+ "		DataChange            = datachange(O.name, null, null), \n" // this can't be used since it requires us to *be* (have context) in the database we investigate 
//	    		+ "		SizeInMb              = convert(numeric(10,1), S.pagecnt / (1024*1024/@@maxpagesize)), \n"
//	    		+ "		PageCount             = S.pagecnt, \n"
//	    		+ "		IndexLeafCount        = S.leafcnt, \n"
//	    		+ "		IndexHeight           = S.indexheight, \n"
//	    		+ "		EmptyPageCount        = S.emptypgcnt, \n"
//	    		+ "		RowsPerPage           = convert(numeric(10,2), CASE WHEN (S.pagecnt > 0) THEN (1.0 * S.rowcnt / S.pagecnt) ELSE 0 END), \n"
//	    		+ "		TableRowCount         = convert(numeric(10,0), S.rowcnt), \n"
//	    		+ "		ForwardRowCount       = convert(numeric(10,0), S.forwrowcnt), \n"
//	    		+ "		DeletedRowCount       = convert(numeric(10,0), S.delrowcnt), \n"
//	    		+ "		DataPageClusterRatio  = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'dpcr')), \n"
//	    		+ "		IndexPageClusterRatio = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'ipcr')), \n"
//	    		+ "		DataRowClusterRatio   = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'drcr')), \n"
//	    		+ PageUtilization
////	    		+ "	--	PageUtilization       = null, \n"
////	    		+ "		PageUtilization       = convert(numeric(10,2), CASE WHEN I.indid > 1 OR reserved_pages(@dbid, I.id, 0) = 0 THEN NULL ELSE 100.0 * data_pages(@dbid, I.id, 0) / reserved_pages(@dbid, I.id, 0) END), \n"
//	    		+ "		SpaceUtilization      = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'sput')), \n"
//	    		+ "		LargeIoEfficiency     = convert(numeric(10,2), derived_stat(@dbname+'.'+U.name+'.'+O.name, S.indid, 'lgio')), \n"
//	    		+ ActualDataPages
////	    		+ "	--	ActualDataPages       = null, \n"
////	    		+ "		ActualDataPages       = data_pages(@dbid, I.id, CASE WHEN I.indid in (0,1) THEN 0 ELSE -1 END), \n"
//				+ ActualIndexPages
////	    		+ "	--	ActualIndexPages      = null, \n"
////	    		+ "		ActualIndexPages      = data_pages(@dbid, I.id, CASE WHEN I.indid in (0,1) THEN 1 ELSE I.indid END), \n"
//				+ ReservedPages
////	    		+ "	--	ReservedPages         = null, \n"
////	    		+ "		ReservedPages         = CASE WHEN I.indid != 1 THEN reserved_pages(@dbid, I.id, I.indid) ELSE reserved_pages(@dbid, I.id, 0)+reserved_pages(@dbid, I.id, 1) END, \n"
//	    		+ "		DataPageUtilization   = convert (numeric(10,2), \n"
//	    		+ "		    CASE WHEN S.indid in (0,1) -- IndexID: 0 = DataPages, or 1 = ClusteredIndexPages \n"
//	    		+ "		    THEN -- approx algorithm (I guess): ((pagesize-pageHeader) / sysindexes.maxlen) / systabstat.pagecnt \n"
//	    		+ "		        CASE WHEN (sysstat2 & 16384) = 16384 OR (sysstat2 & 32768) = 32768 -- sysstat2: 16384 = DOL-datapages, 32768 = DOL-datarows \n"
//	    		+ "		             THEN -- DOL: explain what we do here \n"
//	    		+ "		                 ceiling(1.0 * (CASE WHEN S.rowcnt = 0 THEN 1 ELSE S.rowcnt END) / floor((@@maxpagesize-46)/((CASE WHEN I.maxlen<10 THEN 10 ELSE I.maxlen END)+2))) / S.pagecnt \n"
//	    		+ "		             ELSE -- APL: explain what we do here \n"
//	    		+ "		                 ceiling(1.0 * (CASE WHEN S.rowcnt = 0 THEN 1 ELSE S.rowcnt END) / CASE WHEN floor((@@maxpagesize-32)/(I.maxlen+2)) > 255 THEN 255 ELSE floor((@@maxpagesize-32)/(I.maxlen+2)) END) / S.pagecnt \n"
//	    		+ "		        END \n"
//	    		+ "		    ELSE  \n"
//	    		+ "		        null  \n"
//	    		+ "		    END), \n"
//	    		+ "		DBID                  = convert(int, @dbid), \n"
//	    		+ "		OwnerID               = O.uid, \n"
//	    		+ "		ObjectID              = O.id \n"
////	    		+ "		,PartitionID           = S.partitionid \n" // just prepare if we ever want to do it at a partition level
//	    		+ "	from  \n"
//	    		+ "		( select -- make the below select a 'virtual' table, aliased to S for systabstats, the SUM() is to sumarize all partitions (if there are any) \n"
//	    		+ "				id,  \n"
//	    		+ "				indid,  \n"
//	    		+ "				indexheight   = avg(1.0 * indexheight), -- Height of the index; maintained if indid is greater than 1 \n"
//	    		+ "				statmoddate   = min(      statmoddate), -- Last time the row was flushed to disk \n"
//	    		+ "				partition_cnt = sum(      1          ), -- Number of partition \n"
//	    		+ "				pagecnt       = sum(1.0 * pagecnt    ), -- Number of pages in the table or index \n"
//	    		+ "				leafcnt       = sum(1.0 * leafcnt    ), -- Number of leaf pages in the index; maintained if indid is greater than 1 \n"
//	    		+ "				emptypgcnt    = sum(1.0 * emptypgcnt ), -- Number of empty pages in extents allocated to the table or index \n"
//	    		+ "				rowcnt        = sum(1.0 * rowcnt     ), -- Number of rows in the table; maintained for indid of 0 or 1 \n"
//	    		+ "				forwrowcnt    = sum(1.0 * forwrowcnt ), -- Number of forwarded rows; maintained for indid of 0 or 1 \n"
//	    		+ "				delrowcnt     = sum(1.0 * delrowcnt  )  -- Number of deleted rows \n"
//	    		+ "			from \"+@dbname+\".dbo.systabstats \n"
//	    		+ "			group by id,indid \n"
//	    		+ "		) S,  \n"
////	    		+ "		\"+@dbname+\".dbo.systabstats S,  \n" // just prepare if we ever want to do it at a partition level (then this will replace the above 'virtual table')
//	    		+ "		\"+@dbname+\".dbo.sysindexes I,  \n"
//	    		+ "		\"+@dbname+\".dbo.sysobjects O,  \n"
//	    		+ "		\"+@dbname+\".dbo.sysusers U \n"
//	    		+ "	where S.id               = I.id \n"
//	    		+ "	  and S.indid            = I.indid \n"
//	    		+ "	  and S.id               = O.id \n"
//	    		+ "	  and O.uid              = U.uid \n"
//	    		+ "	  and O.sysstat2 & 1024  = 0  -- SKIP: 1024 = Table is proxy tables  \n"
//	    		+ "	  and O.sysstat2 & 2048  = 0  -- SKIP: 2048 = Table is a proxy table created with the existing keyword. \n"
//	    		+ "	  and S.id              != 8  -- SKIP: syslogs \n"
//	    		+ SystemTables
//	    		+ MinPageLimit
//	    		+ "	\") \n"
//	    		+ "\n"
//	    		+ "	-- Get next row from the 'cursor' \n"
//	    		+ "	set rowcount 1 \n"
//	    		+ "	select @dbid = dbid, @dbname = name from #dbList \n"
//	    		+ "	select @rowcnt = @@rowcount \n"
//	    		+ "	delete #dbList where dbid = @dbid \n"
//	    		+ "	set rowcount 0 \n"
//	    		+ "end \n"
//	    		+ "go \n"
//	    		+ "\n"
//	    		+ "/*------ drop tempdb objects -------*/ \n"
//	    		+ "drop table #dbList \n"
//	    		+ "go \n";
//
//		return sql;
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
			if (conf.getBooleanProperty(PROPKEY_disable_spaceUsage_onTimeout, DEFAULT_disable_spaceUsage_onTimeout))
			{
				setQueryTimeout(getDefaultQueryTimeout(), true);
				_logger.warn("CM='"+getName()+"'. Setting Query Timeout to default of '"+getDefaultQueryTimeout()+"', from method handelTimeoutException().");
				return;
			}
		}

		// SECONDARY Disable the: TabRowCount, NumUsedPages, RowsPerPage
		// It might be that what causing the timeout
		if (conf.getBooleanProperty(PROPKEY_disable_spaceUsage_onTimeout, DEFAULT_disable_spaceUsage_onTimeout))
		{
			if (conf.getBooleanProperty(PROPKEY_sample_spaceUsage, DEFAULT_sample_spaceUsage) == true)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf == null) 
					return;
				tempConf.setProperty(PROPKEY_sample_spaceUsage, false);
				tempConf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				setSql(null);
	
				String key=PROPKEY_sample_spaceUsage;
				_logger.warn("CM='"+getName()+"'. Disabling the column 'PageUtilization', 'ActualDataPages', 'ActualIndexPages', 'ReservedPages' from method handelTimeoutException(). This is done by setting "+key+"=false");
				
				if (getGuiController() != null && getGuiController().hasGUI())
				{
					String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

					JOptionPane optionPane = new JOptionPane(
							"<html>" +
							"The query for CM '"+getName()+"' took to long... and received a Timeout.<br>" +
							"<br>" +
							"This may be caused by the function <code>data_pages()</code> or <code>reserved_pages()</code>, which is used to get how storage the table is using.<br>" +
							"<br>" +
							"To Workaround this issue:<br>" +
							"I just disabled option 'Sample Space Usage'... You can try to enable it again later.<br>" +
							"</html>",
							JOptionPane.INFORMATION_MESSAGE);
					JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "Disabled 'Sample Space Usage' @ "+dateStr);
					dialog.setModal(false);
					dialog.setVisible(true);
				}
			}
		}
	}
}
