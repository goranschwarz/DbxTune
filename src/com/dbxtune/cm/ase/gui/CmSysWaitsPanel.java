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
package com.dbxtune.cm.ase.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.CmSpidWait;
import com.dbxtune.cm.ase.CmSysWaits;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ParameterDialog;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSysWaitsPanel 
extends TabularCntrPanel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmSysWaits.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_enableGraph   = PROP_PREFIX + ".graph.enable";
	private static final boolean DEFAULT_enableGraph   = true;

	private static final String  PROPKEY_graphType     = PROP_PREFIX + ".graph.type";
	private static final String  VALUE_graphType_PIE   = "PIE";
	private static final String  VALUE_graphType_BAR   = "BAR";
	private static final String  DEFAULT_graphType     = VALUE_graphType_PIE;

	private static final String  PROPKEY_showLegend    = PROP_PREFIX + ".graph.show.legend";
	private static final boolean DEFAULT_showLegend    = true;

	private static final String  PROPKEY_includeWaitId250             = PROP_PREFIX + ".graph.include.id.250";
	private static final boolean DEFAULT_includeWaitId250             = false;

	private static final String  PROPKEY_generateEvent                = PROP_PREFIX + ".graph.generate.event";
	private static final boolean DEFAULT_generateEvent                = true;

	private static final String  PROPKEY_generateEventWaitTime        = PROP_PREFIX + ".graph.generate.event.waitTime";
	private static final boolean DEFAULT_generateEventWaitTime        = true;

	private static final String  PROPKEY_generateEventWaits           = PROP_PREFIX + ".graph.generate.event.waits";
	private static final boolean DEFAULT_generateEventWaits           = true;

	private static final String  PROPKEY_generateEventWaitTimePerWait = PROP_PREFIX + ".graph.generate.event.waitTimePerWait";
	private static final boolean DEFAULT_generateEventWaitTimePerWait = true;

	private static final String  PROPKEY_generateClass                = PROP_PREFIX + ".graph.generate.class";
	private static final boolean DEFAULT_generateClass                = false;

	private static final String  PROPKEY_generateClassWaitTime        = PROP_PREFIX + ".graph.generate.class.waitTime";
	private static final boolean DEFAULT_generateClassWaitTime        = true;

	private static final String  PROPKEY_generateClassWaits           = PROP_PREFIX + ".graph.generate.class.waits";
	private static final boolean DEFAULT_generateClassWaits           = true;

	private static final String  PROPKEY_generateClassWaitTimePerWait = PROP_PREFIX + ".graph.generate.class.waitTimePerWait";
	private static final boolean DEFAULT_generateClassWaitTimePerWait = true;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,                DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,                  DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,                    DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,                   DEFAULT_showLegend);
		Configuration.registerDefaultValue(PROPKEY_includeWaitId250,             DEFAULT_includeWaitId250);
		Configuration.registerDefaultValue(PROPKEY_generateEvent,                DEFAULT_generateEvent);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTime,        DEFAULT_generateEventWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaits,           DEFAULT_generateEventWaits);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTimePerWait, DEFAULT_generateEventWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateClass,                DEFAULT_generateClass);
		Configuration.registerDefaultValue(PROPKEY_generateClassWaitTime,        DEFAULT_generateClassWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateClassWaits,           DEFAULT_generateClassWaits);
		Configuration.registerDefaultValue(PROPKEY_generateClassWaitTimePerWait, DEFAULT_generateClassWaitTime);
	}

	public CmSysWaitsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}

	private CategoryDataset createDataset(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy                = conf.getBooleanProperty(PROPKEY_generateDummy,                DEFAULT_generateDummy);
		boolean includeWaitId250             = conf.getBooleanProperty(PROPKEY_includeWaitId250,             DEFAULT_includeWaitId250);
		boolean generateEvent                = conf.getBooleanProperty(PROPKEY_generateEvent,                DEFAULT_generateEvent);
		boolean generateEventWaitTime        = conf.getBooleanProperty(PROPKEY_generateEventWaitTime,        DEFAULT_generateEventWaitTime);
		boolean generateEventWaits           = conf.getBooleanProperty(PROPKEY_generateEventWaits,           DEFAULT_generateEventWaits);
		boolean generateEventWaitTimePerWait = conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerWait, DEFAULT_generateEventWaitTimePerWait);
		boolean generateClass                = conf.getBooleanProperty(PROPKEY_generateClass,                DEFAULT_generateClass);
		boolean generateClassWaitTime        = conf.getBooleanProperty(PROPKEY_generateClassWaitTime,        DEFAULT_generateClassWaitTime);
		boolean generateClassWaits           = conf.getBooleanProperty(PROPKEY_generateClassWaits,           DEFAULT_generateClassWaits);
		boolean generateClassWaitTimePerWait = conf.getBooleanProperty(PROPKEY_generateClassWaitTimePerWait, DEFAULT_generateClassWaitTimePerWait);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int ClassName_pos       = dataTable.findViewColumn("WaitClassDesc");
			int EventName_pos       = dataTable.findViewColumn("WaitEventDesc");
			int WaitEventID_pos     = dataTable.findViewColumn("WaitEventID");
			int WaitTime_pos        = dataTable.findViewColumn("WaitTime"); 
			int Waits_pos           = dataTable.findViewColumn("Waits");
			int WaitTimePerWait_pos = dataTable.findViewColumn("WaitTimePerWait"); 

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Double> classesWaitTime        = new LinkedHashMap<String, Double>();
				Map<String, Double> classesWaits           = new LinkedHashMap<String, Double>();
				Map<String, Double> classesWaitTimePerWait = new LinkedHashMap<String, Double>();
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String ClassName       = (String)dataTable.getValueAt(r, ClassName_pos);
					String EventName       = (String)dataTable.getValueAt(r, EventName_pos);
					Number WaitEventID     = (Number)dataTable.getValueAt(r, WaitEventID_pos);
					Number WaitTime        = (Number)dataTable.getValueAt(r, WaitTime_pos);
					Number Waits           = (Number)dataTable.getValueAt(r, Waits_pos);
					Number WaitTimePerWait = (Number)dataTable.getValueAt(r, WaitTimePerWait_pos);

					// SKIP Wait EventId 250
					if ( ! includeWaitId250 && WaitEventID.intValue() == 250)
						continue;

					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": ClassName("+ClassName_pos+")='"+ClassName+"', EventName("+EventName_pos+")='"+EventName+"', WaitTime("+WaitTime_pos+")='"+WaitTime+"', Waits("+Waits_pos+")='"+Waits+"', WaitTimePerWait("+WaitTimePerWait_pos+")='"+WaitTimePerWait+"'.");

					if (generateClass)
					{
						Double sumWaitTime        = classesWaitTime       .get(ClassName);
						Double sumWaits           = classesWaits          .get(ClassName);
						Double sumWaitTimePerWait = classesWaitTimePerWait.get(ClassName);

						classesWaitTime       .put(ClassName, Double.valueOf(sumWaitTime       ==null ? WaitTime       .doubleValue() : sumWaitTime        + WaitTime       .doubleValue()) );
						classesWaits          .put(ClassName, Double.valueOf(sumWaits          ==null ? Waits          .doubleValue() : sumWaits           + Waits          .doubleValue()) );
						classesWaitTimePerWait.put(ClassName, Double.valueOf(sumWaitTimePerWait==null ? WaitTimePerWait.doubleValue() : sumWaitTimePerWait + WaitTimePerWait.doubleValue()) );
					}
					
					if (generateEvent)
					{
						if (generateEventWaitTime)
							dataset.addValue(WaitTime       .doubleValue(), "["+WaitEventID+"] " + EventName, "EventID - WaitTime");

						if (generateEventWaits)
							dataset.addValue(Waits          .doubleValue(), "["+WaitEventID+"] " + EventName, "EventID - Waits");

						if (generateEventWaitTimePerWait)
							dataset.addValue(WaitTimePerWait.doubleValue(), "["+WaitEventID+"] " + EventName, "EventID - WaitTimePerWait");
					}
				}
				if (generateClass)
				{
					if (generateClassWaitTime)
					{
						for (Map.Entry<String,Double> entry : classesWaitTime.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, "(class) "+key, "Class - WaitTime");
						}
					}
					
					if (generateClassWaits)
					{
						for (Map.Entry<String,Double> entry : classesWaits.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, "(class) "+key, "Class - Waits");
						}
					}

					if (generateClassWaitTimePerWait)
					{
						for (Map.Entry<String,Double> entry : classesWaitTimePerWait.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, "(class) "+key, "Class - WaitTimePerWait");
						}
					}
				}
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

				dataset.addValue(5, "Dummy Event E", "Waits - by Event");
				dataset.addValue(6, "Dummy Event F", "Waits - by Event");
				dataset.addValue(7, "Dummy Event G", "Waits - by Event");

				dataset.addValue(8, "Dummy Class A", "WaitTime - by Class");
				dataset.addValue(9, "Dummy Class B", "WaitTime - by Class");

				dataset.addValue(10,"Dummy Class C", "Waits - by Class");
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
//		final String          chartTitle              = "Wait Event Graph - by Event and by Class";
		final String          chartTitle              = "Wait Graph - by EventID and by ClassName";
		final Font            defaultLegendItemFont   = new Font("SansSerif", Font.PLAIN, 10); // SwingUtils.hiDpiScale(10));
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


	private JCheckBox l_includeWaitId250_chk;
	private JCheckBox l_generateEvent_chk;
	private JCheckBox l_generateEventWaitTime_chk;
	private JCheckBox l_generateEventWaits_chk;
	private JCheckBox l_generateEventWaitTimePerWait_chk;
	private JCheckBox l_generateClass_chk;
	private JCheckBox l_generateClassWaitTime_chk;
	private JCheckBox l_generateClassWaits_chk;
	private JCheckBox l_generateClassWaitTimePerWait_chk;

	private JCheckBox         l_enableGraph_chk;
	private JCheckBox         l_showLegend_chk;

	private JLabel            l_graphType_lbl;
	private JComboBox<String> l_graphType_cbx;

	private JButton           l_trendGraph_settings_but;
	
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
//				Configuration conf = Configuration.getCombinedConfiguration();

				// no need to do anything (The below GUI Components is not showed locally, only in a popup
//				list.add(new CmSettingsHelper("Skip Wait ID List",      PROPKEY_trendGraph_skipWaitIdList    , String.class, conf.getProperty(PROPKEY_trendGraph_skipWaitIdList    , DEFAULT_trendGraph_skipWaitIdList    ), DEFAULT_trendGraph_skipWaitIdList   , "Skip specific WaitEventID's from beeing in ThrendGraph"            ));
//				list.add(new CmSettingsHelper("Skip Wait Class List",   PROPKEY_trendGraph_skipWaitClassList , String.class, conf.getProperty(PROPKEY_trendGraph_skipWaitClassList , DEFAULT_trendGraph_skipWaitClassList ), DEFAULT_trendGraph_skipWaitClassList, "Skip specific Event Clases from beeing in ThrendGraph"             ));
//				list.add(new CmSettingsHelper("Trend Graph Datasource", PROPKEY_trendGraph_dataSource        , String.class, conf.getProperty(PROPKEY_trendGraph_dataSource        , DEFAULT_trendGraph_dataSource        ), DEFAULT_trendGraph_dataSource       , "What column should be the source WaitTime, Waits, WaitTimePerWait" ));

				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		l_includeWaitId250_chk             = new JCheckBox("Include '[250] waiting for incoming network data' in graphs.");
		l_generateEvent_chk                = new JCheckBox("Genereate Graphs for Events");
		l_generateEventWaitTime_chk        = new JCheckBox("WaitTime");
		l_generateEventWaits_chk           = new JCheckBox("Waits");
		l_generateEventWaitTimePerWait_chk = new JCheckBox("WaitTimePerWait");
		l_generateClass_chk                = new JCheckBox("Genereate Graphs for Classes");
		l_generateClassWaitTime_chk        = new JCheckBox("WaitTime");
		l_generateClassWaits_chk           = new JCheckBox("Waits");
		l_generateClassWaitTimePerWait_chk = new JCheckBox("WaitTimePerWait");

		l_enableGraph_chk = new JCheckBox("Show Graph");
		l_showLegend_chk  = new JCheckBox("Show Legend");

		String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		l_graphType_lbl    = new JLabel("Type");
		l_graphType_cbx    = new JComboBox<String>(graphTypeArr);

		l_trendGraph_settings_but = new JButton("Summary TrendGraph Settings");

		String tooltip;
		tooltip = 
			"<html>" +
			"If you want to include/exclude data in the graphs, use the 'filters' panel.<br>" +
			"The Graph is based on what is visible in the Table..." +
			"</html>";
		panel.setToolTipText(tooltip);

		l_enableGraph_chk.setToolTipText("Do you want the Graph to be visible at all...");
		l_showLegend_chk.setToolTipText("Show Legend, which describes all data value types");

		tooltip = 
			"<html>" +
			"If you want to include 'WaitID=250, waiting for incoming network data' in graphs..<br>" +
			"<br>" +
			"<br>" +
			"Another way to include/exclude event types in the graphs, is to use the 'filters' panel.<br>" +
			"The Graph is based on what is visible in the Table..." +
			"</html>";
		l_includeWaitId250_chk.setToolTipText(tooltip);

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		l_graphType_lbl.setToolTipText(tooltip);
		l_graphType_cbx.setToolTipText(tooltip);

		l_generateEvent_chk.setToolTipText("<html>Include 'Events' data in the Graph.</html>");
		l_generateClass_chk.setToolTipText("<html>Include 'Classes' data in the Graph.</html>");

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		l_graphType_cbx.setSelectedItem(orientation);

		l_includeWaitId250_chk            .setSelected(conf.getBooleanProperty(PROPKEY_includeWaitId250,             DEFAULT_includeWaitId250));
		l_generateEvent_chk               .setSelected(conf.getBooleanProperty(PROPKEY_generateEvent,                DEFAULT_generateEvent));
		l_generateEventWaitTime_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTime,        DEFAULT_generateEventWaitTime));
		l_generateEventWaits_chk          .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaits,           DEFAULT_generateEventWaits));
		l_generateEventWaitTimePerWait_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerWait, DEFAULT_generateEventWaitTimePerWait));
		l_generateClass_chk               .setSelected(conf.getBooleanProperty(PROPKEY_generateClass,                DEFAULT_generateClass));
		l_generateClassWaitTime_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitTime,        DEFAULT_generateClassWaitTime));
		l_generateClassWaits_chk          .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaits,           DEFAULT_generateClassWaits));
		l_generateClassWaitTimePerWait_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitTimePerWait, DEFAULT_generateClassWaitTimePerWait));

		l_enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		l_showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));
		
		// ACTION LISTENERS
		l_includeWaitId250_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_includeWaitId250, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		l_enableGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_enableGraph, ((JCheckBox)e.getSource()).isSelected());
				SwingUtils.setEnabled(getLocalOptionsPanel(), ((JCheckBox)e.getSource()).isSelected(), (JCheckBox)e.getSource());
				updateExtendedInfoPanel();
			}
		});

		l_graphType_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;
				
				String type = l_graphType_cbx.getSelectedItem().toString();
				if (type.equals("Pie Chart")) conf.setProperty(PROPKEY_graphType, VALUE_graphType_PIE);
				if (type.equals("Bar Graph")) conf.setProperty(PROPKEY_graphType, VALUE_graphType_BAR);
				conf.save();
				
				updateExtendedInfoPanel();
			}
		});

		l_showLegend_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_showLegend, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		l_generateEvent_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean b = ((JCheckBox)e.getSource()).isSelected();
				helperActionSave(PROPKEY_generateEvent, ((JCheckBox)e.getSource()).isSelected());
				l_generateEventWaitTime_chk       .setEnabled(b);
				l_generateEventWaits_chk          .setEnabled(b);
				l_generateEventWaitTimePerWait_chk.setEnabled(b);
			}
		});
		l_generateEventWaitTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaitTime, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		l_generateEventWaits_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaits, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		l_generateEventWaitTimePerWait_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateEventWaitTimePerWait, ((JCheckBox)e.getSource()).isSelected());
			}
		});



		l_generateClass_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean b = ((JCheckBox)e.getSource()).isSelected();
				helperActionSave(PROPKEY_generateClass, b);
				l_generateClassWaitTime_chk       .setEnabled(b);
				l_generateClassWaits_chk          .setEnabled(b);
				l_generateClassWaitTimePerWait_chk.setEnabled(b);
			}
		});
		l_generateClassWaitTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateClassWaitTime, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		l_generateClassWaits_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateClassWaits, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		l_generateClassWaitTimePerWait_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateClassWaitTimePerWait, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		l_trendGraph_settings_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				openPropertiesEditor();
			}
		});

		// ADD to panel
//		panel.add(includeWaitId250_chk,             "wrap");
		panel.add(l_enableGraph_chk,                  "split");
		panel.add(l_graphType_lbl,                    "");
		panel.add(l_graphType_cbx,                    "");
		panel.add(l_trendGraph_settings_but,          "wrap");

		panel.add(l_generateEvent_chk,                "split");
		panel.add(l_generateEventWaitTime_chk,        "");
		panel.add(l_generateEventWaits_chk,           "");
		panel.add(l_generateEventWaitTimePerWait_chk, "wrap");

		panel.add(l_generateClass_chk,                "split");
		panel.add(l_generateClassWaitTime_chk,        "");
		panel.add(l_generateClassWaits_chk,           "");
		panel.add(l_generateClassWaitTimePerWait_chk, "wrap");

		panel.add(l_showLegend_chk,                   "split");
		panel.add(l_includeWaitId250_chk,             "wrap");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, l_enableGraph_chk.isSelected(), l_enableGraph_chk);

		if (l_enableGraph_chk.isSelected())
		{
			// initial enabled or not
			l_generateEventWaitTime_chk       .setEnabled(l_generateEvent_chk.isSelected());
			l_generateEventWaits_chk          .setEnabled(l_generateEvent_chk.isSelected());
			l_generateEventWaitTimePerWait_chk.setEnabled(l_generateEvent_chk.isSelected());

			l_generateClassWaitTime_chk       .setEnabled(l_generateClass_chk.isSelected());
			l_generateClassWaits_chk          .setEnabled(l_generateClass_chk.isSelected());
			l_generateClassWaitTimePerWait_chk.setEnabled(l_generateClass_chk.isSelected());
		}

		return panel;
	}

	public static void openPropertiesEditor()
	{
		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		String key1 = "WaitEventID's to skip (comma separated list)";
		String key2 = "ClassNames's to skip (comma separated list)";

		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
		in.put(key1, Configuration.getCombinedConfiguration().getProperty( CmSysWaits.PROPKEY_trendGraph_skipWaitIdList,    CmSpidWait.DEFAULT_trendGraph_skipWaitIdList));
		in.put(key2, Configuration.getCombinedConfiguration().getProperty( CmSysWaits.PROPKEY_trendGraph_skipWaitClassList, CmSpidWait.DEFAULT_trendGraph_skipWaitClassList));

		Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "System WaitEvent's to skip", in, false);

		if (results != null)
		{
			tmpConf.setProperty(CmSysWaits.PROPKEY_trendGraph_skipWaitIdList,    results.get(key1));
			tmpConf.setProperty(CmSysWaits.PROPKEY_trendGraph_skipWaitClassList, results.get(key2));

			tmpConf.save();
		}
	}
}
