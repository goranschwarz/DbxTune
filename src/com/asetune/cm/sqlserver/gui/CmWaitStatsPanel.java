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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmWaitStats;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GButton;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmWaitStatsPanel 
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmWaitStatsPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmWaitStats.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_enableGraph   = PROP_PREFIX + ".graph.enable";
	private static final boolean DEFAULT_enableGraph   = true;

	private static final String  PROPKEY_graphType     = PROP_PREFIX + ".graph.type";
	private static final String  VALUE_graphType_PIE   = "PIE";
	private static final String  VALUE_graphType_BAR   = "BAR";
	private static final String  DEFAULT_graphType     = VALUE_graphType_PIE;

	private static final String  PROPKEY_showLegend    = PROP_PREFIX + ".graph.show.legend";
	private static final boolean DEFAULT_showLegend    = false;

//	private static final String  PROPKEY_includeWaitId250              = PROP_PREFIX + ".graph.include.id.250";
//	private static final boolean DEFAULT_includeWaitId250              = false;

	private static final String  PROPKEY_includeHiddenEntries          = PROP_PREFIX + ".graph.include.hidden";
	private static final boolean DEFAULT_includeHiddenEntries          = false;

//	private static final String  PROPKEY_hiddenEntriesList             = PROP_PREFIX + ".graph.hidden.entries";
//	private static final String  DEFAULT_hiddenEntriesList             = "OLEDB, SLEEP_TASK, LAZYWRITER_SLEEP, BROKER_TO_FLUSH, SQLTRACE_INCREMENTAL_FLUSH_SLEEP, REQUEST_FOR_DEADLOCK_SEARCH";

	private static final String  PROPKEY_generateEvent                 = PROP_PREFIX + ".graph.generate.event";
	private static final boolean DEFAULT_generateEvent                 = true;

	private static final String  PROPKEY_generateEventWaitTime         = PROP_PREFIX + ".graph.generate.event.waitTime";
	private static final boolean DEFAULT_generateEventWaitTime         = true;

	private static final String  PROPKEY_generateEventWaitCount        = PROP_PREFIX + ".graph.generate.event.waitCount";
	private static final boolean DEFAULT_generateEventWaitCount        = true;

	private static final String  PROPKEY_generateEventWaitTimePerCount = PROP_PREFIX + ".graph.generate.event.waitTimePerCount";
	private static final boolean DEFAULT_generateEventWaitTimePerCount = true;

//	private static final String  PROPKEY_generateClass                 = PROP_PREFIX + ".graph.generate.class";
//	private static final boolean DEFAULT_generateClass                 = false;
//
//	private static final String  PROPKEY_generateClassWaitTime         = PROP_PREFIX + ".graph.generate.class.waitTime";
//	private static final boolean DEFAULT_generateClassWaitTime         = true;
//
//	private static final String  PROPKEY_generateClassWaitCount        = PROP_PREFIX + ".graph.generate.class.waits";
//	private static final boolean DEFAULT_generateClassWaitCount        = true;
//
//	private static final String  PROPKEY_generateClassWaitTimePerCount = PROP_PREFIX + ".graph.generate.class.waitTimePerCount";
//	private static final boolean DEFAULT_generateClassWaitTimePerCount = true;

//	private List<String> _hiddenEntiesList = new ArrayList<String>();

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,                 DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,                   DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,                     DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,                    DEFAULT_showLegend);
//		Configuration.registerDefaultValue(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250);
		Configuration.registerDefaultValue(PROPKEY_includeHiddenEntries,          DEFAULT_includeHiddenEntries);
		Configuration.registerDefaultValue(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTime);
//		Configuration.registerDefaultValue(PROPKEY_generateClass,                 DEFAULT_generateClass);
//		Configuration.registerDefaultValue(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime);
//		Configuration.registerDefaultValue(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount);
//		Configuration.registerDefaultValue(PROPKEY_generateClassWaitTimePerCount, DEFAULT_generateClassWaitTime);
	}

	public CmWaitStatsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private static final Color SKIP_IN_LOCAL_GRAPHS_COLOR = new Color(229, 194, 149); // DARK Beige
	private static final Color SKIP_IN_TREND_GRAPHS_COLOR = new Color(255, 245, 216); // Beige
	
	private void init()
	{
//		updateHiddenList();
		
		
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// SKIP IN TREND GRAPHS
		if (conf != null) colorStr = conf.getProperty(getName()+".color.worker.parent");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Boolean SkipInTrendGraphs = (Boolean) adapter.getValue(adapter.getColumnIndex("SkipInTrendGraphs"));
				if (SkipInTrendGraphs != null && SkipInTrendGraphs == true)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, SKIP_IN_TREND_GRAPHS_COLOR), null));

		// SKIP IN LOCAL GRAPHS
		if (conf != null) colorStr = conf.getProperty(getName()+".color.worker.parent");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Boolean SkipInLocalGraphs = (Boolean) adapter.getValue(adapter.getColumnIndex("SkipInLocalGraphs"));
				if (SkipInLocalGraphs != null && SkipInLocalGraphs == true)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, SKIP_IN_LOCAL_GRAPHS_COLOR), null));
	}

	private CategoryDataset createDataset(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy                 = conf.getBooleanProperty(PROPKEY_generateDummy,                 DEFAULT_generateDummy);
//		boolean includeWaitId250              = conf.getBooleanProperty(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250);
		boolean includeHiddenEntries          = conf.getBooleanProperty(PROPKEY_includeHiddenEntries,          DEFAULT_includeHiddenEntries);
		boolean generateEvent                 = conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		boolean generateEventWaitTime         = conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		boolean generateEventWaitCount        = conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);
		boolean generateEventWaitTimePerCount = conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTimePerCount);
//		boolean generateClass                 = conf.getBooleanProperty(PROPKEY_generateClass,                 DEFAULT_generateClass);
//		boolean generateClassWaitTime         = conf.getBooleanProperty(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime);
//		boolean generateClassWaitCount        = conf.getBooleanProperty(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount);
//		boolean generateClassWaitTimePerCount = conf.getBooleanProperty(PROPKEY_generateClassWaitTimePerCount, DEFAULT_generateClassWaitTimePerCount);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		CmWaitStats cmWaitStats = (CmWaitStats) getCm();
		
		if (dataTable != null)
		{
//			int ClassName_pos        = dataTable.findViewColumn("WaitClassDesc");
			int WaitType_pos         = dataTable.findViewColumn("wait_type");
//			int WaitEventID_pos      = dataTable.findViewColumn("WaitEventID");
			int WaitTime_pos         = dataTable.findViewColumn("wait_time_ms"); 
			int WaitCount_pos        = dataTable.findViewColumn("waiting_tasks_count");
			int WaitTimePerCount_pos = dataTable.findViewColumn("WaitTimePerCount"); 

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
//				Map<String, Double> classesWaitTime         = new LinkedHashMap<String, Double>();
//				Map<String, Double> classesWaitCount        = new LinkedHashMap<String, Double>();
//				Map<String, Double> classesWaitTimePerCount = new LinkedHashMap<String, Double>();
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
//					String ClassName        = (String)dataTable.getValueAt(r, ClassName_pos);
					String WaitType         = (String)dataTable.getValueAt(r, WaitType_pos);
//					Number WaitEventID      = (Number)dataTable.getValueAt(r, WaitEventID_pos);
					Number WaitTime         = (Number)dataTable.getValueAt(r, WaitTime_pos);
					Number WaitCount        = (Number)dataTable.getValueAt(r, WaitCount_pos);
					Number WaitTimePerCount = (Number)dataTable.getValueAt(r, WaitTimePerCount_pos);

//					// SKIP Wait EventId 250
//					if ( ! includeWaitId250 && WaitEventID.intValue() == 250)
//						continue;

					// SKIP entries that are in the "hide list"
//					if ( ! includeHiddenEntries && _hiddenEntiesList != null && _hiddenEntiesList.contains(WaitType))
					if ( ! includeHiddenEntries && cmWaitStats.isEventNameInLocalGraphSkipList(WaitType))
						continue;

//					if (_logger.isDebugEnabled())
//						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": ClassName("+ClassName_pos+")='"+ClassName+"', WaitType("+WaitType_pos+")='"+WaitType+"', WaitTime("+WaitTime_pos+")='"+WaitTime+"', WaitCount("+WaitCount_pos+")='"+WaitCount+"', WaitTimePerCount("+WaitTimePerCount_pos+")='"+WaitTimePerCount+"'.");

//					if (generateClass)
//					{
//						Double sumWaitTime         = classesWaitTime       .get(ClassName);
//						Double sumWaitCount        = classesWaitCount      .get(ClassName);
//						Double sumWaitTimePerCount = classesWaitTimePerCount.get(ClassName);
//
//						classesWaitTime        .put(ClassName, new Double(sumWaitTime        ==null ? WaitTime        .doubleValue() : sumWaitTime         + WaitTime        .doubleValue()) );
//						classesWaitCount       .put(ClassName, new Double(sumWaitCount       ==null ? WaitCount       .doubleValue() : sumWaitCount        + WaitCount       .doubleValue()) );
//						classesWaitTimePerCount.put(ClassName, new Double(sumWaitTimePerCount==null ? WaitTimePerCount.doubleValue() : sumWaitTimePerCount + WaitTimePerCount.doubleValue()) );
//					}
					
					if (generateEvent)
					{
//						if (generateEventWaitTime)
//							dataset.addValue(WaitTime       .doubleValue(), "["+WaitEventID+"] " + WaitType, "EventID - WaitTime");
//
//						if (generateEventWaitCount)
//							dataset.addValue(WaitCount      .doubleValue(), "["+WaitEventID+"] " + WaitType, "EventID - WaitCount");
//
//						if (generateEventWaitTimePerCount)
//							dataset.addValue(WaitTimePerCount.doubleValue(), "["+WaitEventID+"] " + WaitType, "EventID - WaitTimePerCount");

						if (generateEventWaitTime)
							dataset.addValue(WaitTime       .doubleValue(), WaitType, "WaitType - wait_time_ms");

						if (generateEventWaitCount)
							dataset.addValue(WaitCount      .doubleValue(), WaitType, "WaitType - waiting_tasks_count");

						if (generateEventWaitTimePerCount)
							dataset.addValue(WaitTimePerCount.doubleValue(), WaitType, "WaitType - WaitTimePerCount");
					}
				}
//				if (generateClass)
//				{
//					if (generateClassWaitTime)
//					{
//						for (Map.Entry<String,Double> entry : classesWaitTime.entrySet()) 
//						{
//							String key = entry.getKey();
//							Double val = entry.getValue();
//							dataset.addValue(val, "(class) "+key, "Class - WaitTime");
//						}
//					}
//					
//					if (generateClassWaitCount)
//					{
//						for (Map.Entry<String,Double> entry : classesWaitCount.entrySet()) 
//						{
//							String key = entry.getKey();
//							Double val = entry.getValue();
//							dataset.addValue(val, "(class) "+key, "Class - WaitCount");
//						}
//					}
//
//					if (generateClassWaitTimePerCount)
//					{
//						for (Map.Entry<String,Double> entry : classesWaitTimePerCount.entrySet()) 
//						{
//							String key = entry.getKey();
//							Double val = entry.getValue();
//							dataset.addValue(val, "(class) "+key, "Class - WaitTimePerCount");
//						}
//					}
//				}
			}
		}
		else
		{
//			generateDummy = true;
			if (generateDummy)
			{
				dataset.addValue(1, "Dummy Event A", "WaitTime - by Event");
				dataset.addValue(2, "Dummy Event B", "WaitTime - by Event");
				dataset.addValue(3, "Dummy Event C", "WaitTime - by Event");
				dataset.addValue(4, "Dummy Event D", "WaitTime - by Event");

				dataset.addValue(5, "Dummy Event E", "WaitCount - by Event");
				dataset.addValue(6, "Dummy Event F", "WaitCount - by Event");
				dataset.addValue(7, "Dummy Event G", "WaitCount - by Event");

				dataset.addValue(8, "Dummy Class A", "WaitTime - by Class");
				dataset.addValue(9, "Dummy Class B", "WaitTime - by Class");

				dataset.addValue(10,"Dummy Class C", "WaitCount - by Class");
			}
		}

		return dataset;
	}
	private JFreeChart createChart(CategoryDataset dataset)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean createPieChart = conf.getProperty(       PROPKEY_graphType,  DEFAULT_graphType).equals(VALUE_graphType_PIE);
		boolean showLegend     = conf.getBooleanProperty(PROPKEY_showLegend, DEFAULT_showLegend);

		// some "statics" that can be used in the graphs
//		final String          chartTitle              = "Wait Graph - by EventID and by ClassName";
		final String          chartTitle              = "Wait Graph - by WaitType";
		final Font            defaultLegendItemFont   = new Font("SansSerif", Font.PLAIN, 7); // SwingUtils.hiDpiScale(7)); // 10 was to big for SQL-Server all values
		final RectangleInsets defaultLegendItemInsets = new RectangleInsets(0, 0, 0, 0);

		final Font            defaultLabelFont        = new Font("SansSerif", Font.PLAIN, 9); // SwingUtils.hiDpiScale(9));
		
		if (createPieChart)
		{
			final JFreeChart chart = ChartFactory.createMultiplePieChart(
					null,                   // chart title
					dataset,                // dataset
					TableOrder.BY_COLUMN,
					showLegend,             // include legend
					true,
					false
			);
			chart.setBackgroundPaint(getBackground());
			chart.setTitle(new TextTitle(chartTitle, TextTitle.DEFAULT_FONT));

			// Set FONT SIZE for legend (button what are part of the graph)
			@SuppressWarnings("unchecked")
			List<LegendTitle> legendList = chart.getSubtitles();
			for (LegendTitle lt : legendList)
			{
				lt.setItemFont(defaultLegendItemFont);
				lt.setItemLabelPadding(defaultLegendItemInsets);
			}
			
			final MultiplePiePlot plot = (MultiplePiePlot) chart.getPlot();
			plot.setNoDataMessage("No data available");
//					plot.setBackgroundPaint(Color.white);
//					plot.setOutlineStroke(new BasicStroke(1.0f));
			plot.setLimit(0.05); // 5% or less goes to "Others"

			// Set the sub-charts TITLES FONT and position (stolen from jfreechart, MultiplePiePlot.java, constructor)
			final JFreeChart subchart = plot.getPieChart();
			TextTitle seriesTitle = new TextTitle("Series Title", TextTitle.DEFAULT_FONT); // "Series Title" will be overwritten later on
	        seriesTitle.setPosition(RectangleEdge.BOTTOM);
	        subchart.setTitle(seriesTitle);
	        subchart.setBackgroundPaint(getBackground());

	        // Set properties for the "labels" that points out what in the PIE we are looking at.
			final PiePlot p = (PiePlot) subchart.getPlot();
			p.setLabelFont(defaultLabelFont);
//			p.setInteriorGap(0.30);
//			p.setBackgroundPaint(null);
//			p.setOutlineStroke(null);
			p.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}, {0}, {1}", NumberFormat.getNumberInstance(), NumberFormat.getPercentInstance()));
			p.setLabelGap(0.01D); // how close to the edges can the label be printed ??? 0.01 = 1%
			p.setMaximumLabelWidth(0.20D); // 30% of plot width
			return chart;
		}
		else
		{
			PlotOrientation orientation = PlotOrientation.VERTICAL;
		//	orientation = PlotOrientation.HORIZONTAL;

			JFreeChart chart = ChartFactory.createStackedBarChart(
					null,                     // Title
					null,                     // categoryAxisLabel
					null,                     // valueAxisLabel
					dataset,                  // dataset
					orientation,              // orientation
					showLegend,               // legend
					true,                     // tooltips
					false);                   // urls

			chart.setBackgroundPaint(getBackground());
			chart.setTitle(new TextTitle(chartTitle, TextTitle.DEFAULT_FONT));

			// Set FONT SIZE for legend (button what are part of the graph)
			@SuppressWarnings("unchecked")
			List<LegendTitle> legendList = chart.getSubtitles();
			for (LegendTitle lt : legendList)
			{
				lt.setItemFont(defaultLegendItemFont);
				lt.setItemLabelPadding(defaultLegendItemInsets);
			}

			//------------------------------------------------
			// code from TabularCntrlPanel
			//------------------------------------------------
			CategoryPlot plot = chart.getCategoryPlot();
			plot.setNoDataMessage("No data available");
//			plot.setLimit(0.05); // 5%

		    NumberAxis numAxis = (NumberAxis)plot.getRangeAxis();
		    numAxis.setNumberFormatOverride(NumberFormat.getPercentInstance());
//		    numAxis.setUpperBound(1); // 100%

			StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
			renderer.setRenderAsPercentages(true);
			renderer.setDrawBarOutline(false);
			renderer.setDefaultItemLabelsVisible(true);

		    ItemLabelPosition ilp = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER, TextAnchor.CENTER, 0.0D);
		    renderer.setPositiveItemLabelPositionFallback(ilp);
		    renderer.setNegativeItemLabelPositionFallback(ilp);

		    // set label to be: pct, label, value   
		    // 3=pct, 0=dataLabel, 2=dataValue
		    StandardCategoryItemLabelGenerator scilg = new StandardCategoryItemLabelGenerator("{3}, {0}, {2}", NumberFormat.getInstance());
		    renderer.setDefaultItemLabelGenerator(scilg);
		    renderer.setDefaultItemLabelsVisible(true);
		    renderer.setDefaultItemLabelFont(defaultLabelFont);

			return chart;
		}
	}

	@Override
	protected JPanel createExtendedInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);
		panel.setLayout(new BorderLayout());

		// Size of the panel
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane != null)
			mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean enableGraph = conf.getBooleanProperty(PROPKEY_enableGraph,DEFAULT_enableGraph);

		// Remove and add components to panel
		panel.removeAll();
		if (enableGraph)
			panel.add( new ChartPanel(createChart(createDataset(null))) );
		else
			panel.add( new JLabel("Graph NOT Enabled", JLabel.CENTER) );
		return panel;
	}

	@Override
	protected int getDefaultMainSplitPaneDividerLocation()
	{
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane == null)
			return 0;

		// Use 60% for table, 40% for graph;
		return 6 * (mainSplitPane.getSize().width/10);
	}

	@Override
	protected void setSplitPaneOptions(JSplitPane mainSplitPane, JPanel dataPanel, JPanel extendedInfoPanel)
	{
		mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setBorder(null);
		mainSplitPane.add(dataPanel,         JSplitPane.LEFT);
		mainSplitPane.add(extendedInfoPanel, JSplitPane.RIGHT);
		mainSplitPane.setDividerSize(5);

		mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
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
		
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean enableGraph = conf.getBooleanProperty(PROPKEY_enableGraph,DEFAULT_enableGraph);

		// Remove and add components to panel
		panel.removeAll();
		if (enableGraph)
			panel.add( new ChartPanel(createChart(createDataset(dataTable))) );
		else
			panel.add( new JLabel("Graph NOT Enabled", JLabel.CENTER) );

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

//		final JCheckBox includeWaitId250_chk              = new JCheckBox("Include '[250] waiting for incoming network data' in graphs.");
		final JCheckBox includeHiddenEntries_chk          = new JCheckBox("Include 'hidden' Entries in graphs.");
		final JCheckBox generateEvent_chk                 = new JCheckBox("Genereate Graphs for Events");
		final JCheckBox generateEventWaitTime_chk         = new JCheckBox("wait_time_ms");
		final JCheckBox generateEventWaitCount_chk        = new JCheckBox("waiting_tasks_count");
		final JCheckBox generateEventWaitTimePerCount_chk = new JCheckBox("WaitTimePerCount");
//		final JCheckBox generateClass_chk                 = new JCheckBox("Genereate Graphs for Classes");
//		final JCheckBox generateClassWaitTime_chk         = new JCheckBox("WaitTime");
//		final JCheckBox generateClassWaitCount_chk        = new JCheckBox("WaitCount");
//		final JCheckBox generateClassWaitTimePerCount_chk = new JCheckBox("WaitTimePerCount");

		final JCheckBox enableGraph_chk = new JCheckBox("Show Graph");
		final JCheckBox showLegend_chk  = new JCheckBox("Show Legend");

		String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		final JLabel            graphType_lbl    = new JLabel("Type");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

//		final JButton   trendGraph_settings_but = new JButton("Summary TrendGraph Settings");
//		final JButton   hiddenEntries_but = new JButton("Edit Hidden Entries")
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public String getToolTipText(MouseEvent event)
//			{
//				if (_hiddenEntiesList == null)
//					return null;
//
//				StringBuilder sb = new StringBuilder();
//				sb.append("<html>");
//				if (_hiddenEntiesList.isEmpty())
//				{
//					sb.append("<b>NO</b> entries are currently hidden.");
//				}
//				else
//				{
//					sb.append(_hiddenEntiesList.size()).append(" entries are currently hidden.<br>");
//					sb.append("Below is a list of the hidden entries.");
//					sb.append("<ul>");
//					for (String entry : _hiddenEntiesList)
//						sb.append("<li>").append(entry).append("</li>");
//					sb.append("</ul>");
//				}
//				sb.append("</html>");
//				
//				return sb.toString();
//			};
//		};
//		ToolTipManager.sharedInstance().registerComponent(hiddenEntries_but);

		final GButton resetLocalGraphSkipSet_but = new GButton("Reset Local Graph Skip List")
		{
			private static final long serialVersionUID = 1L;
			@Override
			public String getToolTipText()
			{
//				List<String> list = StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty( CmWaitStats.PROPKEY_LocalGraphsSkipSet, CmWaitStats.DEFAULT_LocalGraphsSkipSet));
				List<String> list = StringUtil.commaStrToList(CmWaitStats.DEFAULT_LocalGraphsSkipSet);
				if (list == null)
					return null;

				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append(list.size()).append(" entries will be set when pressing this button.<br>");
				sb.append("Below is a list of entries.");
				sb.append("<ul>");
				for (String entry : list)
					sb.append("<li>").append(entry).append("</li>");
				sb.append("</ul>");
				sb.append("</html>");
				
				return sb.toString();
			};
		};
		ToolTipManager.sharedInstance().registerComponent(resetLocalGraphSkipSet_but);
		resetLocalGraphSkipSet_but.setFocusable(true);
		
		final GButton resetTrendGraphSkipSet_but = new GButton("Reset Trend Graph Skip List")
		{
			private static final long serialVersionUID = 1L;
			@Override
			public String getToolTipText()
			{
//				List<String> list = StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty( CmWaitStats.PROPKEY_TrendGraphsSkipSet, CmWaitStats.DEFAULT_TrendGraphsSkipSet));
				List<String> list = StringUtil.commaStrToList(CmWaitStats.DEFAULT_TrendGraphsSkipSet);
				if (list == null)
					return null;

				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append(list.size()).append(" entries will be set when pressing this button.<br>");
				sb.append("Below is a list of entries.");
				sb.append("<ul>");
				for (String entry : list)
					sb.append("<li>").append(entry).append("</li>");
				sb.append("</ul>");
				sb.append("</html>");
				
				return sb.toString();
			};
		};
		ToolTipManager.sharedInstance().registerComponent(resetTrendGraphSkipSet_but);
		resetTrendGraphSkipSet_but.setFocusable(true);

		boolean enableSqlFilter_default = Configuration.getCombinedConfiguration().getBooleanProperty(CmWaitStats.PROPKEY_sqlSkipFilterEnabled, CmWaitStats.DEFAULT_sqlSkipFilterEnabled);
		final JCheckBox enableSqlFilter_chk = new JCheckBox("SQL Filter", enableSqlFilter_default);
		enableSqlFilter_chk.setToolTipText("<html>Filter out the most common (see tooltip for button <i>'Reset * Graph Skip List'</i>) wait_types.<br>"
				+ "Do this <b>early</b>. The SQL Statement send to the DBMS will have <code>WHERE wait_type NOT IN ('xxx','yyy')</code><br>"
				+ "The wait types filtered out wont even make it into the below table.<br>"
				+ "And the 'percent' column might show you a better reading...<br>"
				+ "<br>"
				+ "NOTE: If you are recording a session, you probably want to have this <b>off</b> so you record all available <i>wait_typs</i> and possibly filter them out later.<br>"
				+ "</html>");
		

		String tooltip;
		tooltip = 
			"<html>" +
			"If you want to include/exclude data in the graphs, use the 'filters' panel.<br>" +
			"The Graph is based on what is visible in the Table..." +
			"</html>";
		panel.setToolTipText(tooltip);

		enableGraph_chk.setToolTipText("Do you want the Graph to be visible at all...");
		showLegend_chk.setToolTipText("Show Legend, which describes all data value types");

//		tooltip = 
//			"<html>" +
//			"If you want to include 'WaitID=250, waiting for incoming network data' in graphs..<br>" +
//			"<br>" +
//			"<br>" +
//			"Another way to include/exclude event types in the graphs, is to use the 'filters' panel.<br>" +
//			"The Graph is based on what is visible in the Table..." +
//			"</html>";
//		includeWaitId250_chk.setToolTipText(tooltip);

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		generateEvent_chk.setToolTipText("<html>Include 'Events' data in the Graph.</html>");
//		generateClass_chk.setToolTipText("<html>Include 'Classes' data in the Graph.</html>");

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		graphType_cbx.setSelectedItem(orientation);

//		includeWaitId250_chk             .setSelected(conf.getBooleanProperty(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250));
		includeHiddenEntries_chk         .setSelected(conf.getBooleanProperty(PROPKEY_includeHiddenEntries,          DEFAULT_includeHiddenEntries));
		generateEvent_chk                .setSelected(conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent));
		generateEventWaitTime_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime));
		generateEventWaitCount_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount));
		generateEventWaitTimePerCount_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTimePerCount));
//		generateClass_chk                .setSelected(conf.getBooleanProperty(PROPKEY_generateClass,                 DEFAULT_generateClass));
//		generateClassWaitTime_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime));
//		generateClassWaitCount_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount));
//		generateClassWaitTimePerCount_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitTimePerCount, DEFAULT_generateClassWaitTimePerCount));

		enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));
		
//		// ACTION LISTENERS
//		includeWaitId250_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_includeWaitId250, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});

		// ACTION LISTENERS
		enableSqlFilter_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmWaitStats.PROPKEY_sqlSkipFilterEnabled, ((JCheckBox)e.getSource()).isSelected());
				getCm().setSql(null); // Will cause next refresh to create a new SQL Statement
			}
		});

		
		includeHiddenEntries_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_includeHiddenEntries, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		enableGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_enableGraph, ((JCheckBox)e.getSource()).isSelected());
				SwingUtils.setEnabled(getLocalOptionsPanel(), ((JCheckBox)e.getSource()).isSelected(), (JCheckBox)e.getSource());
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
				if (type.equals("Pie Chart")) conf.setProperty(PROPKEY_graphType, VALUE_graphType_PIE);
				if (type.equals("Bar Graph")) conf.setProperty(PROPKEY_graphType, VALUE_graphType_BAR);
				conf.save();
				
				updateExtendedInfoPanel();
			}
		});

		showLegend_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_showLegend, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		generateEvent_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean b = ((JCheckBox)e.getSource()).isSelected();
				helperActionSave(PROPKEY_generateEvent, ((JCheckBox)e.getSource()).isSelected());
				generateEventWaitTime_chk        .setEnabled(b);
				generateEventWaitCount_chk       .setEnabled(b);
				generateEventWaitTimePerCount_chk.setEnabled(b);
			}
		});
		generateEventWaitTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaitTime, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		generateEventWaitCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaitCount, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		generateEventWaitTimePerCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaitTimePerCount, ((JCheckBox)e.getSource()).isSelected());
			}
		});



//		generateClass_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				boolean b = ((JCheckBox)e.getSource()).isSelected();
//				helperActionSave(PROPKEY_generateClass, b);
//				generateClassWaitTime_chk        .setEnabled(b);
//				generateClassWaitCount_chk       .setEnabled(b);
//				generateClassWaitTimePerCount_chk.setEnabled(b);
//			}
//		});
//		generateClassWaitTime_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_generateClassWaitTime, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
//		generateClassWaitCount_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_generateClassWaitCount, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
//		generateClassWaitTimePerCount_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_generateClassWaitTimePerCount, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
//
//		trendGraph_settings_but.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				openPropertiesEditor();
//			}
//		});

//		hiddenEntries_but.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
////				openHiddenEntriesDialog();
//			}
//		});
		
		resetLocalGraphSkipSet_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CmWaitStats cm = (CmWaitStats) getCm();
				cm.resetLocalGraphSkipSet();
			}
		});
		
		resetTrendGraphSkipSet_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CmWaitStats cm = (CmWaitStats) getCm();
				cm.resetTrendGraphSkipSet();
			}
		});
		
		// ADD to panel
//		panel.add(includeWaitId250_chk,              "wrap");
		panel.add(enableGraph_chk,                   "split");
		panel.add(graphType_lbl,                     "");
		panel.add(graphType_cbx,                     "");
//		panel.add(trendGraph_settings_but,           "wrap");
		panel.add(enableSqlFilter_chk,               "wrap");

		panel.add(generateEvent_chk,                 "split");
		panel.add(generateEventWaitTime_chk,         "");
		panel.add(generateEventWaitCount_chk,        "");
		panel.add(generateEventWaitTimePerCount_chk, "wrap");

//		panel.add(generateClass_chk,                 "split");
//		panel.add(generateClassWaitTime_chk,         "");
//		panel.add(generateClassWaitCount_chk,        "");
//		panel.add(generateClassWaitTimePerCount_chk, "wrap");

		panel.add(showLegend_chk,                    "split");
//		panel.add(includeWaitId250_chk,              "wrap");
		panel.add(includeHiddenEntries_chk,          "wrap");
//		panel.add(hiddenEntries_but,                 "wrap");
		panel.add(resetLocalGraphSkipSet_but,        "split");
		panel.add(resetTrendGraphSkipSet_but,        "wrap");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);

		if (enableGraph_chk.isSelected())
		{
			// initial enabled or not
			generateEventWaitTime_chk        .setEnabled(generateEvent_chk.isSelected());
			generateEventWaitCount_chk       .setEnabled(generateEvent_chk.isSelected());
			generateEventWaitTimePerCount_chk.setEnabled(generateEvent_chk.isSelected());

//			generateClassWaitTime_chk        .setEnabled(generateClass_chk.isSelected());
//			generateClassWaitCount_chk       .setEnabled(generateClass_chk.isSelected());
//			generateClassWaitTimePerCount_chk.setEnabled(generateClass_chk.isSelected());
		}

		return panel;
	}

//	public static void openPropertiesEditor()
//	{
//		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
//
//		String key1 = "WaitEventID's to skip (comma separated list)";
//		String key2 = "ClassNames's to skip (comma separated list)";
//
//		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
//		in.put(key1, Configuration.getCombinedConfiguration().getProperty( CmWaitStats.PROPKEY_trendGraph_skipWaitIdList,    CmSpidWait.DEFAULT_trendGraph_skipWaitIdList));
//		in.put(key2, Configuration.getCombinedConfiguration().getProperty( CmWaitStats.PROPKEY_trendGraph_skipWaitClassList, CmSpidWait.DEFAULT_trendGraph_skipWaitClassList));
//
//		Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "System WaitEvent's to skip", in, false);
//
//		if (results != null)
//		{
//			tmpConf.setProperty(CmWaitStats.PROPKEY_trendGraph_skipWaitIdList,    results.get(key1));
//			tmpConf.setProperty(CmWaitStats.PROPKEY_trendGraph_skipWaitClassList, results.get(key2));
//
//			tmpConf.save();
//		}
//	}
	
//	public void openHiddenEntriesDialog()
//	{
//		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
//
//		String key1 = "wait_type(s) to skip (comma separated list)";
//
//		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
//		in.put(key1, Configuration.getCombinedConfiguration().getProperty( PROPKEY_hiddenEntriesList,    DEFAULT_hiddenEntriesList));
//
//		Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "wait_type(s) to skip", in, false);
//
//		if (results != null)
//		{
//			tmpConf.setProperty(PROPKEY_hiddenEntriesList,    results.get(key1));
//			tmpConf.save();
//			
//			updateHiddenList();
//			updateExtendedInfoPanel();
//		}
//	}
//
//	private void updateHiddenList()
//	{
//		String hidden = Configuration.getCombinedConfiguration().getProperty( PROPKEY_hiddenEntriesList, DEFAULT_hiddenEntriesList);
//		
//		_hiddenEntiesList = StringUtil.commaStrToList(hidden);
//	}
}
