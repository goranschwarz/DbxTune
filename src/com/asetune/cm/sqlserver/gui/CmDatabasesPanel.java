/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.sqlserver.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.asetune.CounterController;
import com.asetune.CounterControllerSqlServer;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmDatabases;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmDatabasesPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmDatabasesPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmDatabases.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_plotOrientation          = PROP_PREFIX + ".graph.PlotOrientation";
	private static final String  VALUE_plotOrientation_AUTO       = "AUTO";
	private static final String  VALUE_plotOrientation_VERTICAL   = "VERTICAL";
	private static final String  VALUE_plotOrientation_HORIZONTAL = "HORIZONTAL";
	private static final String  DEFAULT_plotOrientation          = VALUE_plotOrientation_AUTO;

	private static final String  PROPKEY_enableLogGraph      = PROP_PREFIX + ".graph.log.enable";
	private static final boolean DEFAULT_enableLogGraph      = true;

	private static final String  PROPKEY_enableDataGraph     = PROP_PREFIX + ".graph.data.enable";
	private static final boolean DEFAULT_enableDataGraph     = true;

	private static final String  PROPKEY_enableOsDiskGraph   = PROP_PREFIX + ".graph.osDisk.enable";
	private static final boolean DEFAULT_enableOsDiskGraph   = true;

	private static final String CHART_TITLE_LOG     = "Transaction Log Space Usage in Percent";
	private static final String CHART_TITLE_DATA    = "Data Space Usage in Percent";
	private static final String CHART_TITLE_OS_DISK = "Operating System Disk Space Usage in Percent";

//	private JCheckBox sampleSpaceusage_chk;
	private JCheckBox sampleShowplan_chk;
	private JCheckBox sampleMonSqlText_chk;
//	private JCheckBox spaceusageInMb_chk;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,     DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_plotOrientation,   DEFAULT_plotOrientation);
		Configuration.registerDefaultValue(PROPKEY_enableLogGraph,    DEFAULT_enableLogGraph);
		Configuration.registerDefaultValue(PROPKEY_enableDataGraph,   DEFAULT_enableDataGraph);
		Configuration.registerDefaultValue(PROPKEY_enableOsDiskGraph, DEFAULT_enableOsDiskGraph);
	}

	public CmDatabasesPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

//		// LIGHT_BLUE = in 'DUMP DATABASE'
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.dumpdb");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number backupInProg = (Number) adapter.getValue(adapter.getColumnIndex("BackupInProgress"));
//				return backupInProg != null && backupInProg.intValue() > 0;
//			}
//		}, SwingUtils.parseColor(colorStr, TrendGraphColors.VERY_LIGHT_BLUE), null));

		// YELLOW = OLDEST OPEN TRANSACTION above 0
		if (conf != null) colorStr = conf.getProperty(getName()+".color.oldestOpenTranInSeconds");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number oldestOpenTranInSec = (Number) adapter.getValue(adapter.getColumnIndex("OldestTranInSeconds"));
				return oldestOpenTranInSec != null && oldestOpenTranInSec.doubleValue() > 0.0;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

//		// PINK = TRANSACTION LOG at 90%
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.almostFullTranslog");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number almostLogFull = (Number) adapter.getValue(adapter.getColumnIndex("LogSizeUsedPct"));
//				return almostLogFull != null && almostLogFull.doubleValue() > 90.0;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
//
//		// RED = FULL TRANSACTION LOG
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.fullTranslog");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number isLogFull = (Number) adapter.getValue(adapter.getColumnIndex("TransactionLogFull"));
//				return isLogFull != null && isLogFull.intValue() > 0;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));

//		// RED (or 1 cell) = LAST BACKUP FAILED
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.lastBackupFailed");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				int lastBackupFailed_mpos = adapter.getColumnIndex("LastBackupFailed");
//				int mcol = adapter.convertColumnIndexToModel(adapter.column);
//				if (mcol == lastBackupFailed_mpos)
//				{
//					Number lastBackupFailed = (Number) adapter.getValue(lastBackupFailed_mpos);
//					return lastBackupFailed != null && lastBackupFailed.intValue() > 0;
//				}
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	private CategoryDataset createDatasetForLog(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy = conf.getBooleanProperty(PROPKEY_generateDummy, DEFAULT_generateDummy);

		DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int DBName_pos           = dataTable.findViewColumn("DBName");
			int LogSizeFreeInMb_pos  = dataTable.findViewColumn("LogSizeFreeInMb"); // numeric(10,1)
			int LogSizeUsedPct_pos   = dataTable.findViewColumn("LogSizeUsedPct");  // numeric(10,1)
//			int DataSizeFreeInMb_pos = dataTable.findViewColumn("DataSizeFreeInMb"); // numeric(10,1)
//			int DataSizeUsedPct_pos  = dataTable.findViewColumn("DataSizeUsedPct");  // numeric(10,1)

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Integer> dbList = DbSelectionForGraphsDialog.getDbsInGraphList(cm);
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String DBName           = (String)dataTable.getValueAt(r, DBName_pos);
					Number LogSizeFreeInMb  = (Number)dataTable.getValueAt(r, LogSizeFreeInMb_pos);
					Number LogSizeUsedPct   = (Number)dataTable.getValueAt(r, LogSizeUsedPct_pos);
//					Number DataSizeFreeInMb = (Number)dataTable.getValueAt(r, DataSizeFreeInMb_pos);
//					Number DataSizeUsedPct  = (Number)dataTable.getValueAt(r, DataSizeUsedPct_pos);

					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": DBName("+DBName_pos+")='"+DBName+"', LogSizeFreeInMb("+LogSizeFreeInMb_pos+")='"+LogSizeFreeInMb+"', LogSizeUsedPct("+LogSizeUsedPct_pos+")='"+LogSizeUsedPct+"'.");

					if (LogSizeFreeInMb == null || LogSizeUsedPct == null)
						continue;
					
					if (dbList.keySet().contains(DBName))
					{
						String sizeStr = NumberFormat.getNumberInstance().format(LogSizeFreeInMb);
						categoryDataset.addValue(LogSizeUsedPct,  "FREE MB: " + sizeStr,  DBName);
//						categoryDataset.addValue(LogSizeUsedPct,  LogSizeFreeInMb +" MB FREE Log",  DBName);
//						categoryDataset.addValue(DataSizeUsedPct, DataSizeFreeInMb+" MB FREE Data", DBName);
					}
				}
			}
		}
		else
		{
			if (generateDummy)
			{
				for(int i=1; i<=30; i++)
				{
					double freePct = Math.random() * 100.0;
					double usedPct = 100.0 - freePct;
					BigDecimal freeMb = new BigDecimal(Math.random() * 1000.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

					categoryDataset.addValue(usedPct, "FREE MB: "+freeMb, "dummy_db_"+i);
				}
			}
		}

		return categoryDataset;
	}

	private CategoryDataset createDatasetForData(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy = conf.getBooleanProperty(PROPKEY_generateDummy, DEFAULT_generateDummy);

		DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int DBName_pos           = dataTable.findViewColumn("DBName");
//			int LogSizeFreeInMb_pos  = dataTable.findViewColumn("LogSizeFreeInMb"); // numeric(10,1)
//			int LogSizeUsedPct_pos   = dataTable.findViewColumn("LogSizeUsedPct");  // numeric(10,1)
			int DataSizeFreeInMb_pos = dataTable.findViewColumn("DataSizeFreeInMb"); // numeric(10,1)
			int DataSizeUsedPct_pos  = dataTable.findViewColumn("DataSizeUsedPct");  // numeric(10,1)

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Integer> dbList = DbSelectionForGraphsDialog.getDbsInGraphList(cm);
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String DBName           = (String)dataTable.getValueAt(r, DBName_pos);
//					Number LogSizeFreeInMb  = (Number)dataTable.getValueAt(r, LogSizeFreeInMb_pos);
//					Number LogSizeUsedPct   = (Number)dataTable.getValueAt(r, LogSizeUsedPct_pos);
					Number DataSizeFreeInMb = (Number)dataTable.getValueAt(r, DataSizeFreeInMb_pos);
					Number DataSizeUsedPct  = (Number)dataTable.getValueAt(r, DataSizeUsedPct_pos);

//					if (_logger.isDebugEnabled())
//						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": DBName("+DBName_pos+")='"+DBName+"', LogSizeFreeInMb("+LogSizeFreeInMb_pos+")='"+LogSizeFreeInMb+"', LogSizeUsedPct("+LogSizeUsedPct_pos+")='"+LogSizeUsedPct+"'.");
					if (_logger.isDebugEnabled())
					_logger.debug("createDataset():GRAPH-DATA: "+getName()+": DBName("+DBName_pos+")='"+DBName+"', DataSizeFreeInMb("+DataSizeFreeInMb_pos+")='"+DataSizeFreeInMb+"', DataSizeUsedPct("+DataSizeUsedPct_pos+")='"+DataSizeUsedPct+"'.");

					if (DataSizeFreeInMb == null || DataSizeUsedPct == null)
						continue;
					
					if (dbList.keySet().contains(DBName))
					{
						String sizeStr = NumberFormat.getNumberInstance().format(DataSizeFreeInMb);
//						categoryDataset.addValue(LogSizeUsedPct,  LogSizeFreeInMb +" MB FREE",  DBName);
						categoryDataset.addValue(DataSizeUsedPct, "FREE MB: " + sizeStr, DBName);
					}
				}
			}
		}
		else
		{
			if (generateDummy)
			{
				for(int i=1; i<=30; i++)
				{
					double freePct = Math.random() * 100.0;
					double usedPct = 100.0 - freePct;
					BigDecimal freeMb = new BigDecimal(Math.random() * 1000.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

					categoryDataset.addValue(usedPct, "FREE MB: "+freeMb, "dummy_db_"+i);
				}
			}
		}

		return categoryDataset;
	}

	
	private static class DiskOsEntry
	{
		public DiskOsEntry(String name, Double freeMb, Double usedPct)
		{
			_name    = name;
			_freeMb  = freeMb;
			_usedPct = usedPct;
		}

		String _name;
		Double _freeMb;
		Double _usedPct;
	}
	private CategoryDataset createDatasetForOsDisk(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy = conf.getBooleanProperty(PROPKEY_generateDummy, DEFAULT_generateDummy);

		DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int LogOsDisk_pos           = dataTable.findViewColumn("LogOsDisk");
			int LogOsFileName_pos       = dataTable.findViewColumn("LogOsFileName");
			int LogOsDiskFreeMb_pos     = dataTable.findViewColumn("LogOsDiskFreeMb");
			int LogOsDiskUsedPct_pos    = dataTable.findViewColumn("LogOsDiskUsedPct");
			
			int DataOsDisk_pos          = dataTable.findViewColumn("DataOsDisk");
			int DataOsFileName_pos      = dataTable.findViewColumn("DataOsFileName");
			int DataOsDiskFreeMb_pos    = dataTable.findViewColumn("DataOsDiskFreeMb");
			int DataOsDiskUsedPct_pos   = dataTable.findViewColumn("DataOsDiskUsedPct");
			
			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				// The databases are PROBABALY SHARING same DISK so...
				// add stuff to a Map, and later add it to categoryDataset
				Map<String, DiskOsEntry> diskMap = new LinkedHashMap<>();
				
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String LogOsDisk           = (String)dataTable.getValueAt(r, LogOsDisk_pos);
					String LogOsFileName       = (String)dataTable.getValueAt(r, LogOsFileName_pos);
					Number LogOsDiskFreeMb     = (Number)dataTable.getValueAt(r, LogOsDiskFreeMb_pos);
					Number LogOsDiskUsedPct    = (Number)dataTable.getValueAt(r, LogOsDiskUsedPct_pos);
					
					String DataOsDisk          = (String)dataTable.getValueAt(r, DataOsDisk_pos);
					String DataOsFileName      = (String)dataTable.getValueAt(r, DataOsFileName_pos);
					Number DataOsDiskFreeMb    = (Number)dataTable.getValueAt(r, DataOsDiskFreeMb_pos);
					Number DataOsDiskUsedPct   = (Number)dataTable.getValueAt(r, DataOsDiskUsedPct_pos);
					
					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-OS-DISK: "+getName()+": "
								+ "LogOsDisk("         + LogOsDisk_pos         + ")='" + LogOsDisk         + "', "
								+ "LogOsFileName("     + LogOsFileName_pos     + ")='" + LogOsFileName     + "', "
								+ "LogOsDiskFreeMb("   + LogOsDiskFreeMb_pos   + ")='" + LogOsDiskFreeMb   + "', "
								+ "LogOsDiskUsedPct("  + LogOsDiskUsedPct_pos  + ")='" + LogOsDiskUsedPct  + "', "
								+ "DataOsDisk("        + DataOsDisk_pos        + ")='" + DataOsDisk        + "', "
								+ "DataOsFileName("    + DataOsFileName_pos    + ")='" + DataOsFileName    + "', "
								+ "DataOsDiskFreeMb("  + DataOsDiskFreeMb_pos  + ")='" + DataOsDiskFreeMb  + "', "
								+ "DataOsDiskUsedPct(" + DataOsDiskUsedPct_pos + ")='" + DataOsDiskUsedPct + "'.");

					String logOsName  = LogOsDisk;
					String dataOsName = DataOsDisk;
					
					if (StringUtil.isNullOrBlank(logOsName))
						logOsName = CounterControllerSqlServer.resolvFileNameToDirectory(LogOsFileName);

					if (StringUtil.isNullOrBlank(dataOsName))
						dataOsName = CounterControllerSqlServer.resolvFileNameToDirectory(DataOsFileName);
					
					DiskOsEntry logEntry  = diskMap.get(logOsName);
					DiskOsEntry dataEntry = diskMap.get(dataOsName);

					if (logEntry == null)
					{
						logEntry = new DiskOsEntry(logOsName, LogOsDiskFreeMb.doubleValue(), LogOsDiskUsedPct.doubleValue());
						diskMap.put(logOsName, logEntry);
					}
					
					if (dataEntry == null)
					{
						dataEntry = new DiskOsEntry(dataOsName, DataOsDiskFreeMb.doubleValue(), DataOsDiskUsedPct.doubleValue());
						diskMap.put(dataOsName, dataEntry);
					}
				}
				
				// Finally ADD Entries in the Map to the DataSet
				for (DiskOsEntry entry : diskMap.values())
				{
					String sizeStr = NumberFormat.getNumberInstance().format(entry._freeMb);
					categoryDataset.addValue(entry._usedPct, "FREE MB: " + sizeStr, entry._name);
				}
			}
		}
		else
		{
			if (generateDummy)
			{
				for(int i=1; i<=30; i++)
				{
					double freePct = Math.random() * 100.0;
					double usedPct = 100.0 - freePct;
					BigDecimal freeMb = new BigDecimal(Math.random() * 1000.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

					categoryDataset.addValue(usedPct, "FREE MB: "+freeMb, "dummy_db_"+i);
				}
			}
		}

		return categoryDataset;
	}

	private JFreeChart createChart(CategoryDataset dataset, String title)
	{
		// Get Graph Orientation: VERTICAL or HORIZONTAL
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_AUTO);

		PlotOrientation orientation = PlotOrientation.VERTICAL;
		if (orientationStr.equals(VALUE_plotOrientation_VERTICAL))   orientation = PlotOrientation.VERTICAL;
		if (orientationStr.equals(VALUE_plotOrientation_HORIZONTAL)) orientation = PlotOrientation.HORIZONTAL;
		if (orientationStr.equals(VALUE_plotOrientation_AUTO))
		{
			if (dataset.getRowCount() <= 10)
				orientation = PlotOrientation.HORIZONTAL;
		}

		JFreeChart chart = ChartFactory.createStackedBarChart(
				null,                     // Title
				null,                     // categoryAxisLabel
				null,                     // valueAxisLabel
				dataset,                  // dataset
				orientation,              // orientation
				false,                    // legend
				true,                     // tooltips
				false);                   // urls

		chart.setBackgroundPaint(getBackground());
//		chart.setTitle(new TextTitle("Transaction Log Space Usage in Percent", TextTitle.DEFAULT_FONT));
		chart.setTitle(new TextTitle(title, TextTitle.DEFAULT_FONT));

		//------------------------------------------------
		// code from TabularCntrlPanel
		//------------------------------------------------
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setNoDataMessage("No data available");
		StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();

		if (orientation.equals(PlotOrientation.VERTICAL))
		{
			ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0);
			renderer.setDefaultPositiveItemLabelPosition(p1);
			
			ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0);
			renderer.setPositiveItemLabelPositionFallback(p2);

			// Tilt DBName 45 degree left
			CategoryAxis domainAxis = plot.getDomainAxis();
			if (orientation.equals(PlotOrientation.VERTICAL))
				domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		}
		else
		{
			ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0);
			renderer.setDefaultPositiveItemLabelPosition(p1);
			
			ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, 0);
			renderer.setPositiveItemLabelPositionFallback(p2);
		}

		
		StandardCategoryItemLabelGenerator scilg = new StandardCategoryItemLabelGenerator("{0}", NumberFormat.getInstance());
		renderer.setDefaultItemLabelGenerator(scilg);
		
		renderer.setDefaultItemLabelsVisible(true);

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setUpperBound(100);

//		for (int r=0; r<dataset.getRowCount(); r++)
//			for (int c=0; c<dataset.getColumnCount(); c++)
//				System.out.println("dataset investigate: r="+r+", c="+c+", val="+dataset.getValue(r, c));

		for (int r=0; r<dataset.getRowCount(); r++)
		{
			Number val = dataset.getValue(r, r);
			if (val == null)
				continue;
			if (val.intValue() >= 90)
				renderer.setSeriesPaint(r, Color.RED);
			else if (val.intValue() >= 80)
				renderer.setSeriesPaint(r, Color.ORANGE);
			else
				renderer.setSeriesPaint(r, Color.GREEN);
		}

		return chart;
	}


	@Override
	protected int getDefaultMainSplitPaneDividerLocation()
	{
		return 150;
	}

	@Override
	protected JPanel createExtendedInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);
//		panel.setLayout(new BorderLayout());
		panel.setLayout(new MigLayout("ins 0"));

//		// Create Graph
//		JFreeChart chartLog = createChart(createDatasetForLog(null), CHART_TITLE_LOG);
//		panel.add( new ChartPanel(chartLog) );
//
//		JFreeChart chartData = createChart(createDatasetForData(null), CHART_TITLE_DATA);
//		panel.add( new ChartPanel(chartData) );

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean enableLogGraph    = conf.getBooleanProperty(PROPKEY_enableLogGraph,    DEFAULT_enableLogGraph);
		boolean enableDataGraph   = conf.getBooleanProperty(PROPKEY_enableDataGraph,   DEFAULT_enableDataGraph);
		boolean enableOsDiskGraph = conf.getBooleanProperty(PROPKEY_enableOsDiskGraph, DEFAULT_enableOsDiskGraph);

		// Remove and add components to panel
		panel.removeAll();

		// Create Graph
		if (enableLogGraph)
		{
			JFreeChart chartLog = createChart(createDatasetForLog(null), CHART_TITLE_LOG);
			panel.add( new ChartPanel(chartLog), "grow, push" );
		}

		if (enableDataGraph)
		{
    		JFreeChart chartData = createChart(createDatasetForData(null), CHART_TITLE_DATA);
    		panel.add( new ChartPanel(chartData), "grow, push" );
		}
		
		if (enableOsDiskGraph)
		{
    		JFreeChart chartOsDisk = createChart(createDatasetForOsDisk(null), CHART_TITLE_OS_DISK);
    		panel.add( new ChartPanel(chartOsDisk), "grow, push" );
		}
		
		if (enableLogGraph == false && enableDataGraph == false && enableOsDiskGraph == false)
			panel.add( new JLabel("Graph NOT Enabled", JLabel.CENTER), "grow, push" );

		// Size of the panel
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane != null)
			mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());

		return panel;
	}

	@Override
	public void updateExtendedInfoPanel()
	{
		JPanel panel     = getExtendedInfoPanel();
		GTable dataTable = getDataTable();
		if (panel     == null) return;

		// If the panel is so small, make it bigger 
		int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
		if (dividerLocation == 0)
		{
			JSplitPane mainSplitPane = getMainSplitPane();
			if (mainSplitPane != null)
				mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
		}
		
//		if ( ! isMonConnected() )
//			dataTable = null;

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean enableLogGraph    = conf.getBooleanProperty(PROPKEY_enableLogGraph,    DEFAULT_enableLogGraph);
		boolean enableDataGraph   = conf.getBooleanProperty(PROPKEY_enableDataGraph,   DEFAULT_enableDataGraph);
		boolean enableOsDiskGraph = conf.getBooleanProperty(PROPKEY_enableOsDiskGraph, DEFAULT_enableOsDiskGraph);

		// Remove and add components to panel
		panel.removeAll();

		// Create Graph
		if (enableLogGraph)
		{
			JFreeChart chartLog = createChart(createDatasetForLog(dataTable), CHART_TITLE_LOG);
			panel.add( new ChartPanel(chartLog), "grow, push" );
		}

		if (enableDataGraph)
		{
    		JFreeChart chartData = createChart(createDatasetForData(dataTable), CHART_TITLE_DATA);
    		panel.add( new ChartPanel(chartData), "grow, push" );
		}
		
		if (enableOsDiskGraph)
		{
    		JFreeChart chartOsDisk = createChart(createDatasetForOsDisk(dataTable), CHART_TITLE_OS_DISK);
    		panel.add( new ChartPanel(chartOsDisk), "grow, push" );
		}
		
		if (enableLogGraph == false && enableDataGraph == false && enableOsDiskGraph == false)
			panel.add( new JLabel("Graph NOT Enabled", JLabel.CENTER), "grow, push" );

		// Needs to be done since we remove and add content to the panel
		panel.validate();
		panel.repaint();
	}

	private void helperActionSave(String key, boolean b)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		conf.setProperty(key, b);
		conf.save();
		
		updateExtendedInfoPanel();
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

//		final JCheckBox sampleSpaceusage_chk = new JCheckBox("Sample Spaceusage");;
//		spaceusageInMb_chk   = new JCheckBox("Spaceusage in MB");
//		sampleSpaceusage_chk = new JCheckBox("Sample Spaceusage details");
		sampleShowplan_chk   = new JCheckBox("Showplan");
		sampleMonSqlText_chk = new JCheckBox("SQL Text");
		
		final JCheckBox enableLogGraph_chk    = new JCheckBox("Tran Log Graph");
		final JCheckBox enableDataGraph_chk   = new JCheckBox("Data Graph");
		final JCheckBox enableOsDiskGraph_chk = new JCheckBox("OS Disk Graph");
		final JButton resetMoveToTab_but      = new JButton("Reset 'Move to Tab' settings");
		final JButton dblist_but              = new JButton("Set 'Graph' databases");

		String[] graphTypeArr = {"Auto", "Vertical", "Horizontal"};
		final JLabel            graphType_lbl    = new JLabel("Graph Orientation");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

		enableLogGraph_chk   .setToolTipText("Show the graph for '"+CHART_TITLE_LOG+"'.");
		enableDataGraph_chk  .setToolTipText("Show the graph for '"+CHART_TITLE_DATA+"'.");
		enableOsDiskGraph_chk.setToolTipText("Show the graph for '"+CHART_TITLE_OS_DISK+"'.");

		String tooltip =
			"<html>" +
			"Reset the option: To automatically switch to this tab when any Database(s) Transaction log is <b>full</b>.<br>" +
			"Next time this happens, a popup will ask you what you want to do." +
			"</html>";
		resetMoveToTab_but.setToolTipText(tooltip);

		dblist_but   .setToolTipText("<html>What databases should be apart of the graphs, both in the Summary Panel, and this local graph area.<br></html>");
		tooltip = 
			"<html>" +
			"Do you want the 'Transaction Log Size Graph' to be presented 'Standing' or 'Laying'.<br>" +
			"'Auto' means: " +
			"<ul>" +
			"    <li>'Laying' if <b>less</b> than 10 databases is included.</li>" +
			"    <li>'Standing' if <b>more</b> than 10 databases is included.</li>" +
			"</ul>" +
			"</html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

//		sampleSpaceusage_chk.setToolTipText("Execute spaceusage(dbid) on every sample. Note this may take some extra recources. Only available in ASE 15.7 SP64 & SP136 and above.");
//		spaceusageInMb_chk  .setToolTipText("<html>"
//				+ "Calculate spaceusage in MB instead of pages.<br>"
//				+ "This is only for the spaceusage columns: 'ReservedPages, UsedPages, UnUsedPages, DataPages, DataPagesReal, IndexPages, IndexPagesReal, LobPages' in ASE 16.0<br>"
//				+ "</html>");
		sampleShowplan_chk  .setToolTipText("<html>Get sp_showplan on on SPID's that has an <b>open transaction</b>.</html>");
		sampleMonSqlText_chk.setToolTipText("<html>Get SQL Text (from monProcessSQLText) on on SPID's that has an <b>open transaction</b>.</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		enableLogGraph_chk   .setSelected(conf.getBooleanProperty(PROPKEY_enableLogGraph,    DEFAULT_enableLogGraph));
		enableDataGraph_chk  .setSelected(conf.getBooleanProperty(PROPKEY_enableDataGraph,   DEFAULT_enableDataGraph));
		enableOsDiskGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableOsDiskGraph, DEFAULT_enableOsDiskGraph));
//		sampleSpaceusage_chk .setSelected(conf.getBooleanProperty(CmDatabases.PROPKEY_sample_spaceusage, CmDatabases.DEFAULT_sample_spaceusage));
//		spaceusageInMb_chk   .setSelected(conf.getBooleanProperty(CmDatabases.PROPKEY_spaceusageInMb,    CmDatabases.DEFAULT_spaceusageInMb));
		sampleShowplan_chk   .setSelected(conf.getBooleanProperty(CmDatabases.PROPKEY_sample_showplan  , CmDatabases.DEFAULT_sample_showplan));
		sampleMonSqlText_chk .setSelected(conf.getBooleanProperty(CmDatabases.PROPKEY_sample_monSqlText, CmDatabases.DEFAULT_sample_monSqlText));
		
		// Set initial value for Graph Orientation
		String orientationStr = conf.getProperty(PROPKEY_plotOrientation, DEFAULT_plotOrientation);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_plotOrientation_AUTO))       orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_plotOrientation_VERTICAL))   orientation = graphTypeArr[1];
		if (orientationStr.equals(VALUE_plotOrientation_HORIZONTAL)) orientation = graphTypeArr[2];
		graphType_cbx.setSelectedItem(orientation);
		
		// ACTION events
//		sampleSpaceusage_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(CmDatabases.PROPKEY_sample_spaceusage, ((JCheckBox)e.getSource()).isSelected());
//				getCm().setSql(null); // Causes SQL Statement to be recreated
//			}
//		});
//		spaceusageInMb_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(CmDatabases.PROPKEY_spaceusageInMb, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
		sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmDatabases.PROPKEY_sample_showplan, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});
		sampleMonSqlText_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmDatabases.PROPKEY_sample_monSqlText, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});
		enableLogGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_enableLogGraph, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		enableDataGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_enableDataGraph, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		enableOsDiskGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_enableOsDiskGraph, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		resetMoveToTab_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChangeToJTabDialog.resetSavedSettings(getPanelName());
//				getCm().getGuiController().resetGoToTabSettings(getPanelName()); // TODO: maybe implement something like this
				CounterController.getSummaryPanel().resetGoToTabSettings(getPanelName());
			}
		});
		dblist_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = getDisplayCm();
				if (cm == null)
					cm = getCm();

				int rc = DbSelectionForGraphsDialog.showDialog(MainFrame.getInstance(), cm);
				if (rc == JOptionPane.OK_OPTION)
					updateExtendedInfoPanel();
			}
		});
		graphType_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;
				
				String type = graphType_cbx.getSelectedItem().toString();
				if (type.equals("Auto"))       conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_AUTO);
				if (type.equals("Vertical"))   conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_VERTICAL);
				if (type.equals("Horizontal")) conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_HORIZONTAL);
				conf.save();
				
				updateExtendedInfoPanel();
			}
		});

		// ADD to panel
		panel.add(dblist_but,            "span, split");
		panel.add(resetMoveToTab_but,    "wrap");
		panel.add(graphType_lbl,         "split");
		panel.add(graphType_cbx,         "wrap");
		panel.add(enableLogGraph_chk,    "span, split");
		panel.add(enableDataGraph_chk,   "");
		panel.add(enableOsDiskGraph_chk, "wrap");
//		panel.add(sampleSpaceusage_chk,  "wrap");
//		panel.add(sampleSpaceusage_chk,  "span, split");
//		panel.add(spaceusageInMb_chk,    "wrap");
//		panel.add(sampleShowplan_chk,    "");
//		panel.add(sampleMonSqlText_chk,  "wrap");

		return panel;
	}
}
