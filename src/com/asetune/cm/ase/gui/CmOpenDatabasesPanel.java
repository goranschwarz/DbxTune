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
package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
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
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmOpenDatabases;
import com.asetune.graph.TrendGraphColors;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmOpenDatabasesPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmOpenDatabasesPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmOpenDatabases.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_plotOrientation          = PROP_PREFIX + ".graph.PlotOrientation";
	private static final String  VALUE_plotOrientation_AUTO       = "AUTO";
	private static final String  VALUE_plotOrientation_VERTICAL   = "VERTICAL";
	private static final String  VALUE_plotOrientation_HORIZONTAL = "HORIZONTAL";
	private static final String  DEFAULT_plotOrientation          = VALUE_plotOrientation_AUTO;

	private static final String  PROPKEY_enableLogGraph   = PROP_PREFIX + ".graph.log.enable";
	private static final boolean DEFAULT_enableLogGraph   = true;

	private static final String  PROPKEY_enableDataGraph   = PROP_PREFIX + ".graph.data.enable";
	private static final boolean DEFAULT_enableDataGraph   = true;

	private static final String CHART_TITLE_LOG  = "Transaction Log Space Usage in Percent";
	private static final String CHART_TITLE_DATA = "Data Space Usage in Percent";

	private JCheckBox sampleSpaceusage_chk;
	private JCheckBox sampleShowplan_chk;
	private JCheckBox sampleMonSqlText_chk;
	private JCheckBox sampleLocks_chk;
//	private JCheckBox spaceusageInMb_chk;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,      DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_plotOrientation,    DEFAULT_plotOrientation);
	}


	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,   DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_plotOrientation, DEFAULT_plotOrientation);
		Configuration.registerDefaultValue(PROPKEY_enableLogGraph,  DEFAULT_enableLogGraph);
		Configuration.registerDefaultValue(PROPKEY_enableDataGraph, DEFAULT_enableDataGraph);
	}

	public CmOpenDatabasesPanel(CountersModel cm)
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

		// LIGHT_BLUE = in 'DUMP DATABASE'
		if (conf != null) colorStr = conf.getProperty(getName()+".color.dumpdb");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number backupInProg = (Number) adapter.getValue(adapter.getColumnIndex("BackupInProgress"));
				return backupInProg != null && backupInProg.intValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, TrendGraphColors.VERY_LIGHT_BLUE), null));

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

		// PINK = TRANSACTION LOG at 90%
		if (conf != null) colorStr = conf.getProperty(getName()+".color.almostFullTranslog");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number almostLogFull = (Number) adapter.getValue(adapter.getColumnIndex("LogSizeUsedPct"));
				return almostLogFull != null && almostLogFull.doubleValue() > 90.0;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = FULL TRANSACTION LOG
		if (conf != null) colorStr = conf.getProperty(getName()+".color.fullTranslog");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number isLogFull = (Number) adapter.getValue(adapter.getColumnIndex("TransactionLogFull"));
				return isLogFull != null && isLogFull.intValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

		// RED (or 1 cell) = LAST BACKUP FAILED
		if (conf != null) colorStr = conf.getProperty(getName()+".color.lastBackupFailed");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int lastBackupFailed_mpos = adapter.getColumnIndex("LastBackupFailed");
				int mcol = adapter.convertColumnIndexToModel(adapter.column);
				if (mcol == lastBackupFailed_mpos)
				{
					Number lastBackupFailed = (Number) adapter.getValue(lastBackupFailed_mpos);
					return lastBackupFailed != null && lastBackupFailed.intValue() > 0;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
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
		boolean enableLogGraph  = conf.getBooleanProperty(PROPKEY_enableLogGraph,  DEFAULT_enableLogGraph);
		boolean enableDataGraph = conf.getBooleanProperty(PROPKEY_enableDataGraph, DEFAULT_enableDataGraph);

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
		
		if (enableLogGraph == false && enableDataGraph == false)
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
		boolean enableLogGraph  = conf.getBooleanProperty(PROPKEY_enableLogGraph,  DEFAULT_enableLogGraph);
		boolean enableDataGraph = conf.getBooleanProperty(PROPKEY_enableDataGraph, DEFAULT_enableDataGraph);

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
		
		if (enableLogGraph == false && enableDataGraph == false)
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
		sampleSpaceusage_chk = new JCheckBox("Sample Spaceusage details");
		sampleShowplan_chk   = new JCheckBox("Showplan");
		sampleMonSqlText_chk = new JCheckBox("SQL Text");
		sampleLocks_chk      = new JCheckBox("Locks");
		
		final JCheckBox enableLogGraph_chk   = new JCheckBox("Tran Log Graph");
		final JCheckBox enableDataGraph_chk  = new JCheckBox("Data Graph");
		final JButton resetMoveToTab_but     = new JButton("Reset 'Move to Tab' settings");
		final JButton dblist_but             = new JButton("Set 'Graph' databases");

		String[] graphTypeArr = {"Auto", "Vertical", "Horizontal"};
		final JLabel            graphType_lbl    = new JLabel("Graph Orientation");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

		enableLogGraph_chk .setToolTipText("Show the graph for '"+CHART_TITLE_LOG+"'.");
		enableDataGraph_chk.setToolTipText("Show the graph for '"+CHART_TITLE_DATA+"'.");
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

		sampleSpaceusage_chk.setToolTipText("Execute spaceusage(dbid) on every sample. Note this may take some extra recources. Only available in ASE 15.7 SP64 & SP136 and above.");
//		spaceusageInMb_chk  .setToolTipText("<html>"
//				+ "Calculate spaceusage in MB instead of pages.<br>"
//				+ "This is only for the spaceusage columns: 'ReservedPages, UsedPages, UnUsedPages, DataPages, DataPagesReal, IndexPages, IndexPagesReal, LobPages' in ASE 16.0<br>"
//				+ "</html>");
		sampleShowplan_chk  .setToolTipText("<html>Get sp_showplan on on SPID's that has an <b>open transaction</b>.</html>");
		sampleMonSqlText_chk.setToolTipText("<html>Get SQL Text (from monProcessSQLText) on on SPID's that has an <b>open transaction</b>.</html>");
		sampleLocks_chk     .setToolTipText("<html>Get locks (from syslocks or monLocks) on on SPID's that has an <b>open transaction</b>.</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		enableLogGraph_chk .setSelected(conf.getBooleanProperty(PROPKEY_enableLogGraph,  DEFAULT_enableLogGraph));
		enableDataGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableDataGraph, DEFAULT_enableDataGraph));
		sampleSpaceusage_chk.setSelected(conf.getBooleanProperty(CmOpenDatabases.PROPKEY_sample_spaceusage, CmOpenDatabases.DEFAULT_sample_spaceusage));
//		spaceusageInMb_chk  .setSelected(conf.getBooleanProperty(CmOpenDatabases.PROPKEY_spaceusageInMb, CmOpenDatabases.DEFAULT_spaceusageInMb));
		sampleShowplan_chk  .setSelected(conf.getBooleanProperty(CmOpenDatabases.PROPKEY_sample_showplan  , CmOpenDatabases.DEFAULT_sample_showplan));
		sampleMonSqlText_chk.setSelected(conf.getBooleanProperty(CmOpenDatabases.PROPKEY_sample_monSqlText, CmOpenDatabases.DEFAULT_sample_monSqlText));
		sampleLocks_chk     .setSelected(conf.getBooleanProperty(CmOpenDatabases.PROPKEY_sample_locks,      CmOpenDatabases.DEFAULT_sample_locks));
		
		// Set initial value for Graph Orientation
		String orientationStr = conf.getProperty(PROPKEY_plotOrientation, DEFAULT_plotOrientation);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_plotOrientation_AUTO))       orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_plotOrientation_VERTICAL))   orientation = graphTypeArr[1];
		if (orientationStr.equals(VALUE_plotOrientation_HORIZONTAL)) orientation = graphTypeArr[2];
		graphType_cbx.setSelectedItem(orientation);
		
		// ACTION events
		sampleSpaceusage_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmOpenDatabases.PROPKEY_sample_spaceusage, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});
//		spaceusageInMb_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(CmOpenDatabases.PROPKEY_spaceusageInMb, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
		sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmOpenDatabases.PROPKEY_sample_showplan, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});
		sampleMonSqlText_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmOpenDatabases.PROPKEY_sample_monSqlText, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});
		sampleLocks_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmOpenDatabases.PROPKEY_sample_locks, ((JCheckBox)e.getSource()).isSelected());
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
		panel.add(dblist_but,           "span, split");
		panel.add(resetMoveToTab_but,   "wrap");
		panel.add(graphType_lbl,        "split");
		panel.add(graphType_cbx,        "wrap");
		panel.add(enableLogGraph_chk,   "span, split");
		panel.add(enableDataGraph_chk,  "wrap");
//		panel.add(sampleSpaceusage_chk, "wrap");
		panel.add(sampleSpaceusage_chk, "span, split");
//		panel.add(spaceusageInMb_chk,   "wrap");
		panel.add(sampleShowplan_chk,   "");
		panel.add(sampleMonSqlText_chk, "");
		panel.add(sampleLocks_chk,      "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			if (cm.isRuntimeInitialized())
			{
				// disable if not 16.0
//				if ( cm.getServerVersion() > Ver.ver(16,0))
				long srvVersion = cm.getServerVersion();
				if (srvVersion >= Ver.ver(15,7,0, 136) || (srvVersion >= Ver.ver(15,7,0, 64) && srvVersion < Ver.ver(15,7,0, 100)) )
				{
					sampleSpaceusage_chk.setEnabled(true);
//					spaceusageInMb_chk  .setEnabled(true);
				}
				else
				{
					sampleSpaceusage_chk.setEnabled(false);
//					spaceusageInMb_chk  .setEnabled(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmOpenDatabases.PROPKEY_sample_spaceusage, false);
					}
				}
			} // end isRuntimeInitialized
		} // end (cm != null)

		
		// CHeck if PROPS has changed by timeouts, and set the GUI acording to that
		Configuration conf = Configuration.getCombinedConfiguration();

		// PROPKEY_sample_systemTables
		boolean confProp = conf.getBooleanProperty(CmOpenDatabases.PROPKEY_sample_spaceusage, CmOpenDatabases.DEFAULT_sample_spaceusage);
		boolean guiProp  = sampleSpaceusage_chk.isSelected();

		if (confProp != guiProp)
			sampleSpaceusage_chk.setSelected(confProp);
	}
}
