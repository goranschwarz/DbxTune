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
package com.dbxtune.cm.postgres.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

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
import com.dbxtune.cm.postgres.CmPgSrvWait;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgSrvWaitPanel 
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgSrvWaitPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmPgSrvWait.CM_NAME;

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


	private static final String  PROPKEY_generateEvent                 = PROP_PREFIX + ".graph.generate.event";
	private static final boolean DEFAULT_generateEvent                 = true;

	private static final String  PROPKEY_generateEventWaitTime         = PROP_PREFIX + ".graph.generate.event.waitTime";
	private static final boolean DEFAULT_generateEventWaitTime         = true;

	private static final String  PROPKEY_generateEventWaitCount        = PROP_PREFIX + ".graph.generate.event.waitCount";
	private static final boolean DEFAULT_generateEventWaitCount        = false;


	private static final String  PROPKEY_generateClass                 = PROP_PREFIX + ".graph.generate.class";
	private static final boolean DEFAULT_generateClass                 = false;

	private static final String  PROPKEY_generateClassWaitTime         = PROP_PREFIX + ".graph.generate.class.waitTime";
	private static final boolean DEFAULT_generateClassWaitTime         = true;

	private static final String  PROPKEY_generateClassWaitCount        = PROP_PREFIX + ".graph.generate.class.waits";
	private static final boolean DEFAULT_generateClassWaitCount        = false;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,                 DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,                   DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,                     DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,                    DEFAULT_showLegend);

		Configuration.registerDefaultValue(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);

		Configuration.registerDefaultValue(PROPKEY_generateClass,                 DEFAULT_generateClass);
		Configuration.registerDefaultValue(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount);
	}

	public CmPgSrvWaitPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private CategoryDataset createDataset(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy                 = conf.getBooleanProperty(PROPKEY_generateDummy,                 DEFAULT_generateDummy);

		boolean generateEvent                 = conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		boolean generateEventWaitTime         = conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		boolean generateEventWaitCount        = conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);

		boolean generateClass                 = conf.getBooleanProperty(PROPKEY_generateClass,                 DEFAULT_generateClass);
		boolean generateClassWaitTime         = conf.getBooleanProperty(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime);
		boolean generateClassWaitCount        = conf.getBooleanProperty(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		CmPgSrvWait cmWaitStats = (CmPgSrvWait) getCm();
		
		if (dataTable != null)
		{
			int ClassName_pos        = dataTable.findViewColumn("event_type");
			int WaitType_pos         = dataTable.findViewColumn("event");
//			int WaitEventID_pos      = dataTable.findViewColumn("WaitEventID");
			int WaitTime_pos         = dataTable.findViewColumn("est_wait_time_ms"); 
			int WaitCount_pos        = dataTable.findViewColumn("wait_count");

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Double> classesWaitTime         = new LinkedHashMap<String, Double>();
				Map<String, Double> classesWaitCount        = new LinkedHashMap<String, Double>();

				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String ClassName        = (String)dataTable.getValueAt(r, ClassName_pos);
					String WaitType         = (String)dataTable.getValueAt(r, WaitType_pos);
//					Number WaitEventID      = (Number)dataTable.getValueAt(r, WaitEventID_pos);
					Number WaitTime         = (Number)dataTable.getValueAt(r, WaitTime_pos);
					Number WaitCount        = (Number)dataTable.getValueAt(r, WaitCount_pos);

//					if (_logger.isDebugEnabled())
//						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": ClassName("+ClassName_pos+")='"+ClassName+"', WaitType("+WaitType_pos+")='"+WaitType+"', WaitTime("+WaitTime_pos+")='"+WaitTime+"', WaitCount("+WaitCount_pos+")='"+WaitCount+"', WaitTimePerCount("+WaitTimePerCount_pos+")='"+WaitTimePerCount+"'.");

					if (generateClass)
					{
						Double sumWaitTime         = classesWaitTime       .get(ClassName);
						Double sumWaitCount        = classesWaitCount      .get(ClassName);

						classesWaitTime        .put(ClassName, new Double(sumWaitTime        ==null ? WaitTime        .doubleValue() : sumWaitTime         + WaitTime        .doubleValue()) );
						classesWaitCount       .put(ClassName, new Double(sumWaitCount       ==null ? WaitCount       .doubleValue() : sumWaitCount        + WaitCount       .doubleValue()) );
					}
					
					if (generateEvent)
					{
						if (generateEventWaitTime)
							dataset.addValue(WaitTime       .doubleValue(), WaitType, "WaitType - est_wait_time_ms");

						if (generateEventWaitCount)
							dataset.addValue(WaitCount      .doubleValue(), WaitType, "WaitType - wait_count");
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
							dataset.addValue(val, "(class) "+key, "Class - est_wait_time_ms");
						}
					}
					
					if (generateClassWaitCount)
					{
						for (Map.Entry<String,Double> entry : classesWaitCount.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, "(class) "+key, "Class - wait_count");
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
//			plot.setLimit(0.01); // 1% or less goes to "Others"

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

		final JCheckBox generateEvent_chk                 = new JCheckBox("Genereate Graphs for Events");
		final JCheckBox generateEventWaitTime_chk         = new JCheckBox("est_wait_time_ms");
		final JCheckBox generateEventWaitCount_chk        = new JCheckBox("wait_count");

		final JCheckBox generateClass_chk                 = new JCheckBox("Genereate Graphs for Classes");
		final JCheckBox generateClassWaitTime_chk         = new JCheckBox("est_wait_time_ms");
		final JCheckBox generateClassWaitCount_chk        = new JCheckBox("wait_count");

		final JCheckBox enableGraph_chk = new JCheckBox("Show Graph");
		final JCheckBox showLegend_chk  = new JCheckBox("Show Legend");

		String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		final JLabel            graphType_lbl    = new JLabel("Type");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

		String tooltip;

//		final JButton   trendGraph_settings_but = new JButton("Summary TrendGraph Settings");

		enableGraph_chk.setToolTipText("Do you want the Graph to be visible at all...");
		showLegend_chk.setToolTipText("Show Legend, which describes all data value types");

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		generateEvent_chk.setToolTipText("<html>Include 'Events' data in the Graph.</html>");
		generateClass_chk.setToolTipText("<html>Include 'Classes' data in the Graph.</html>");

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		graphType_cbx.setSelectedItem(orientation);

		generateEvent_chk                .setSelected(conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent));
		generateEventWaitTime_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime));
		generateEventWaitCount_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount));

		generateClass_chk                .setSelected(conf.getBooleanProperty(PROPKEY_generateClass,                 DEFAULT_generateClass));
		generateClassWaitTime_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitTime,         DEFAULT_generateClassWaitTime));
		generateClassWaitCount_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateClassWaitCount,        DEFAULT_generateClassWaitCount));

		enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));
		
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



		generateClass_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean b = ((JCheckBox)e.getSource()).isSelected();
				helperActionSave(PROPKEY_generateClass, b);
				generateClassWaitTime_chk        .setEnabled(b);
				generateClassWaitCount_chk       .setEnabled(b);
			}
		});
		generateClassWaitTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateClassWaitTime, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		generateClassWaitCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateClassWaitCount, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		
		// ADD to panel
		panel.add(enableGraph_chk,                   "split");
		panel.add(graphType_lbl,                     "");
		panel.add(graphType_cbx,                     "wrap");

		panel.add(generateEvent_chk,                 "split");
		panel.add(generateEventWaitTime_chk,         "");
		panel.add(generateEventWaitCount_chk,        "wrap");

		panel.add(generateClass_chk,                 "split");
		panel.add(generateClassWaitTime_chk,         "");
		panel.add(generateClassWaitCount_chk,        "wrap");

		panel.add(showLegend_chk,                    "split");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);

		if (enableGraph_chk.isSelected())
		{
			// initial enabled or not
			generateEventWaitTime_chk        .setEnabled(generateEvent_chk.isSelected());
			generateEventWaitCount_chk       .setEnabled(generateEvent_chk.isSelected());

			generateClassWaitTime_chk        .setEnabled(generateClass_chk.isSelected());
			generateClassWaitCount_chk       .setEnabled(generateClass_chk.isSelected());
		}

		return panel;
	}
}
