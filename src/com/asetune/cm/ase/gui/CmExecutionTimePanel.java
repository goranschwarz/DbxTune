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

import java.awt.BorderLayout;
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

import org.apache.log4j.Logger;
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
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;
import org.jfree.util.TableOrder;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmExecutionTime;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmExecutionTimePanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmExecutionTimePanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmExecutionTime.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_enableGraph   = PROP_PREFIX + ".graph.enable";
	private static final boolean DEFAULT_enableGraph   = true;

	private static final String  PROPKEY_graphType     = PROP_PREFIX + ".graph.type";
	private static final String  VALUE_graphType_PIE   = "PIE";
	private static final String  VALUE_graphType_BAR   = "BAR";
	private static final String  DEFAULT_graphType     = VALUE_graphType_BAR;

	private static final String  PROPKEY_showLegend    = PROP_PREFIX + ".graph.show.legend";
	private static final boolean DEFAULT_showLegend    = true;

	private static final String  PROPKEY_generateExecutionCnt        = PROP_PREFIX + ".graph.generate.ExecutionCnt";
	private static final boolean DEFAULT_generateExecutionCnt        = true;

	private static final String  PROPKEY_generateExecutionTime       = PROP_PREFIX + ".graph.generate.ExecutionTime";
	private static final boolean DEFAULT_generateExecutionTime       = true;

	private static final String  PROPKEY_generateExecutionTimePerCnt = PROP_PREFIX + ".graph.generate.ExecutionTimePerCnt";
	private static final boolean DEFAULT_generateExecutionTimePerCnt = true;

	private static final String  PROPKEY_generateCpuUsagePct         = PROP_PREFIX + ".graph.generate.CpuUsagePct";
	private static final boolean DEFAULT_generateCpuUsagePct         = true;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,               DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,                 DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,                   DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,                  DEFAULT_showLegend);
		Configuration.registerDefaultValue(PROPKEY_generateExecutionCnt,        DEFAULT_generateExecutionCnt);
		Configuration.registerDefaultValue(PROPKEY_generateExecutionTime,       DEFAULT_generateExecutionTime);
		Configuration.registerDefaultValue(PROPKEY_generateExecutionTimePerCnt, DEFAULT_generateExecutionTimePerCnt);
		Configuration.registerDefaultValue(PROPKEY_generateCpuUsagePct,         DEFAULT_generateCpuUsagePct);
	}

	public CmExecutionTimePanel(CountersModel cm)
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
		boolean generateDummy               = conf.getBooleanProperty(PROPKEY_generateDummy,               DEFAULT_generateDummy);
		boolean generateExecutionCnt        = conf.getBooleanProperty(PROPKEY_generateExecutionCnt,        DEFAULT_generateExecutionCnt);
		boolean generateExecutionTime       = conf.getBooleanProperty(PROPKEY_generateExecutionTime,       DEFAULT_generateExecutionTime);
		boolean generateExecutionTimePerCnt = conf.getBooleanProperty(PROPKEY_generateExecutionTimePerCnt, DEFAULT_generateExecutionTimePerCnt);
		boolean generateCpuUsagePct         = conf.getBooleanProperty(PROPKEY_generateCpuUsagePct,         DEFAULT_generateCpuUsagePct);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int OperationName_pos       = dataTable.findViewColumn("OperationName");
			int ExecutionCnt_pos        = dataTable.findViewColumn("ExecutionCnt");
			int ExecutionTime_pos       = dataTable.findViewColumn("ExecutionTime");
			int ExecutionTimePerCnt_pos = dataTable.findViewColumn("ExecutionTimePerCnt");
			int CpuUsagePct_pos         = dataTable.findViewColumn("CpuUsagePct");

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			double CpuUsagePctSUM = 0; // Sum of ALL CpuUsagePct rows... then we can calculate "idle" 
			if (cm != null)
			{
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String OperationName       = (String)dataTable.getValueAt(r, OperationName_pos);
					Number ExecutionCnt        = (Number)dataTable.getValueAt(r, ExecutionCnt_pos);
					Number ExecutionTime       = (Number)dataTable.getValueAt(r, ExecutionTime_pos);
					Number ExecutionTimePerCnt = (Number)dataTable.getValueAt(r, ExecutionTimePerCnt_pos);
					Number CpuUsagePct         = (Number)dataTable.getValueAt(r, CpuUsagePct_pos);

					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": OperationName("+OperationName_pos+")='"+OperationName+"', ExecutionCnt("+ExecutionCnt_pos+")='"+ExecutionCnt+"', ExecutionTime("+ExecutionTime_pos+")='"+ExecutionTime+"', ExecutionTimePerCnt("+ExecutionTimePerCnt_pos+")='"+ExecutionTimePerCnt+"', CpuUsagePct("+CpuUsagePct_pos+")='"+CpuUsagePct+"'.");

					// add 0 if null value...
					if (ExecutionCnt       == null) ExecutionCnt        = new Double(0);
					if (ExecutionTime      == null) ExecutionTime       = new Double(0);
					if (ExecutionTimePerCnt== null) ExecutionTimePerCnt = new Double(0);
					if (CpuUsagePct        == null) CpuUsagePct         = new Double(0);

					if (generateExecutionCnt)        dataset.addValue(ExecutionCnt       .doubleValue(), OperationName, "ExecutionCnt");
					if (generateExecutionTime)       dataset.addValue(ExecutionTime      .doubleValue(), OperationName, "ExecutionTime");
					if (generateExecutionTimePerCnt) dataset.addValue(ExecutionTimePerCnt.doubleValue(), OperationName, "ExecutionTimePerCnt");
					if (generateCpuUsagePct)         dataset.addValue(CpuUsagePct        .doubleValue(), OperationName, "CpuUsagePct");
					
					CpuUsagePctSUM += CpuUsagePct.doubleValue();
				}
				
				// Add CPU IDLE value for CpuUsagePct Graph
				if (dataTable.getRowCount() > 0)
					if (generateCpuUsagePct) dataset.addValue( (100.0 - CpuUsagePctSUM), "IDLE", "CpuUsagePct");
			}
		}
		else
		{
//			generateDummy = true;
			if (generateDummy)
			{
				dataset.addValue(1,   "Unknown",     "ExecutionCnt");
				dataset.addValue(2,   "Execution",   "ExecutionCnt");
				dataset.addValue(3,   "Sorting",     "ExecutionCnt");
				dataset.addValue(4,   "Compilation", "ExecutionCnt");

				dataset.addValue(123, "Unknown",     "ExecutionTime");
				dataset.addValue(234, "Execution",   "ExecutionTime");
				dataset.addValue(345, "Sorting",     "ExecutionTime");
				dataset.addValue(456, "Compilation", "ExecutionTime");
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
		final String          chartTitle              = "System Execution Count/Time Graph";
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
//			plot.setLimit(0.05); // 5% or less goes to "Others"

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
			renderer.setBaseItemLabelsVisible(true);

		    ItemLabelPosition ilp = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER, TextAnchor.CENTER, 0.0D);
		    renderer.setPositiveItemLabelPositionFallback(ilp);
		    renderer.setNegativeItemLabelPositionFallback(ilp);

		    // set label to be: pct, label, value   
		    // 3=pct, 0=dataLabel, 2=dataValue
		    StandardCategoryItemLabelGenerator scilg = new StandardCategoryItemLabelGenerator("{3}, {0}, {2}", NumberFormat.getInstance());
		    renderer.setBaseItemLabelGenerator(scilg);
		    renderer.setBaseItemLabelsVisible(true);
		    renderer.setBaseItemLabelFont(defaultLabelFont);

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

		// Use 30% for table, 57% for graph;
		return 5 * (mainSplitPane.getSize().width/10);
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

		final JCheckBox enableExecutionCnt_chk        = new JCheckBox("ExecutionCnt");
		final JCheckBox enableExecutionTime_chk       = new JCheckBox("ExecutionTime");
		final JCheckBox enableExecutionTimePerCnt_chk = new JCheckBox("ExecutionTimePerCnt");
		final JCheckBox enableCpuUsagePct_chk         = new JCheckBox("CpuUsagePct");

		final JCheckBox enableGraph_chk     = new JCheckBox("Show Graph");
		final JCheckBox showLegend_chk      = new JCheckBox("Show Legend");

		String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		final JLabel            graphType_lbl    = new JLabel("Type");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

		String tooltip;
		tooltip = 
			"<html>" +
			"If you want to include/exclude data in the graphs, use the 'filters' panel.<br>" +
			"The Graph is based on what is visible in the Table..." +
			"</html>";
		panel.setToolTipText(tooltip);

		enableGraph_chk.setToolTipText("Do you want the Graph to be visible at all...");
		showLegend_chk .setToolTipText("Show Legend, which describes all data value types");

		enableExecutionCnt_chk        .setToolTipText("Show number of Execution Count per type");
		enableExecutionTime_chk       .setToolTipText("Show Execution Time per type");
		enableExecutionTimePerCnt_chk .setToolTipText("Show Execution Time for each Execution Count per type");
		enableCpuUsagePct_chk         .setToolTipText("Show CPU Usage per 'operations category'");

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		graphType_cbx.setSelectedItem(orientation);

		enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));

		enableExecutionCnt_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateExecutionCnt,        DEFAULT_generateExecutionCnt));
		enableExecutionTime_chk      .setSelected(conf.getBooleanProperty(PROPKEY_generateExecutionTime,       DEFAULT_generateExecutionTime));
		enableExecutionTimePerCnt_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateExecutionTimePerCnt, DEFAULT_generateExecutionTimePerCnt));
		enableCpuUsagePct_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateCpuUsagePct,         DEFAULT_generateCpuUsagePct));

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
		showLegend_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_showLegend, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		enableExecutionCnt_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateExecutionCnt, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		enableExecutionTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateExecutionTime, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		enableExecutionTimePerCnt_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateExecutionTimePerCnt, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		enableCpuUsagePct_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateCpuUsagePct, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		// ADD to panel
		panel.add(enableGraph_chk,      "split");
		panel.add(graphType_lbl,        "");
		panel.add(graphType_cbx,        "wrap");

		panel.add(enableExecutionCnt_chk,        "split");
		panel.add(enableExecutionTime_chk,       "");
		panel.add(enableExecutionTimePerCnt_chk, "");
		panel.add(enableCpuUsagePct_chk,         "wrap");

		panel.add(showLegend_chk,       "wrap");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);
		
		return panel;
	}
}
