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
package com.asetune.cm.mysql.gui;

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
import com.asetune.cm.mysql.CmSysDiskIo;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSysDiskIoPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmSysDiskIoPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmSysDiskIo.CM_NAME;

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


	private static final String  PROPKEY_generateRwPct = PROP_PREFIX + ".graph.generate.io.read_write_pct";
	private static final boolean DEFAULT_generateRwPct = true;

//	private static final String  PROPKEY_generateRvsApfPct = PROP_PREFIX + ".graph.generate.io.normalRead_vs_apfRead_pct";
//	private static final boolean DEFAULT_generateRvsApfPct = true;

	private static final String  PROPKEY_generateTotalIo = PROP_PREFIX + ".graph.generate.io.total";
	private static final boolean DEFAULT_generateTotalIo = true;

	private static final String  PROPKEY_generateRead    = PROP_PREFIX + ".graph.generate.io.read";
	private static final boolean DEFAULT_generateRead    = false;

//	private static final String  PROPKEY_generateApfRead = PROP_PREFIX + ".graph.generate.io.read.apf";
//	private static final boolean DEFAULT_generateApfRead = false;

	private static final String  PROPKEY_generateWrite   = PROP_PREFIX + ".graph.generate.io.write";
	private static final boolean DEFAULT_generateWrite   = false;

	private static final String  PROPKEY_generateMisc    = PROP_PREFIX + ".graph.generate.io.misc";
	private static final boolean DEFAULT_generateMisc    = false;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,     DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,       DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,         DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,        DEFAULT_showLegend);
		Configuration.registerDefaultValue(PROPKEY_generateRwPct,     DEFAULT_generateRwPct);
//		Configuration.registerDefaultValue(PROPKEY_generateRvsApfPct, DEFAULT_generateRvsApfPct);
		Configuration.registerDefaultValue(PROPKEY_generateTotalIo,   DEFAULT_generateTotalIo);
		Configuration.registerDefaultValue(PROPKEY_generateRead,      DEFAULT_generateRead);
//		Configuration.registerDefaultValue(PROPKEY_generateApfRead,   DEFAULT_generateApfRead);
		Configuration.registerDefaultValue(PROPKEY_generateWrite,     DEFAULT_generateWrite);
		Configuration.registerDefaultValue(PROPKEY_generateMisc,      DEFAULT_generateMisc);
	}

	public CmSysDiskIoPanel(CountersModel cm)
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
		boolean generateDummy     = conf.getBooleanProperty(PROPKEY_generateDummy,     DEFAULT_generateDummy);
		boolean generateRwPct     = conf.getBooleanProperty(PROPKEY_generateRwPct,     DEFAULT_generateRwPct);
//		boolean generateRvsApfPct = conf.getBooleanProperty(PROPKEY_generateRvsApfPct, DEFAULT_generateRvsApfPct);
		boolean generateTotalIo   = conf.getBooleanProperty(PROPKEY_generateTotalIo,   DEFAULT_generateTotalIo);
		boolean generateRead      = conf.getBooleanProperty(PROPKEY_generateRead,      DEFAULT_generateRead);
//		boolean generateApfRead   = conf.getBooleanProperty(PROPKEY_generateApfRead,   DEFAULT_generateApfRead);
		boolean generateWrite     = conf.getBooleanProperty(PROPKEY_generateWrite,     DEFAULT_generateWrite);
		boolean generateMisc      = conf.getBooleanProperty(PROPKEY_generateMisc,      DEFAULT_generateMisc);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int LogicalName_pos = dataTable.findViewColumn("file");
			int TotalIOs_pos    = dataTable.findViewColumn("total");
			int Reads_pos       = dataTable.findViewColumn("count_read");
			int Writes_pos      = dataTable.findViewColumn("count_write");
			int Misc_pos        = dataTable.findViewColumn("count_misc"); 

//			int ReadsPct_pos    = dataTable.findViewColumn("ReadsPct");
//			int APFReadsPct_pos = dataTable.findViewColumn("APFReadsPct");
//			int WritesPct_pos   = dataTable.findViewColumn("WritesPct");

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				double sumReads    = 0;
//				double sumApfReads = 0;
				double sumWrites   = 0;
				double sumMisc     = 0;

				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String LogicalName = (String)dataTable.getValueAt(r, LogicalName_pos);
					Number TotalIOs    = (Number)dataTable.getValueAt(r, TotalIOs_pos);
					Number Reads       = (Number)dataTable.getValueAt(r, Reads_pos);
					Number Writes      = (Number)dataTable.getValueAt(r, Writes_pos);
					Number Misc        = (Number)dataTable.getValueAt(r, Misc_pos);

//					Number ReadsPct    = (Number)dataTable.getValueAt(r, ReadsPct_pos);
//					Number APFReadsPct = (Number)dataTable.getValueAt(r, APFReadsPct_pos);
//					Number WritesPct   = (Number)dataTable.getValueAt(r, WritesPct_pos);

					// This looked like a good idea, but it did not look good at all
					//String LogicalNameTotalIo = LogicalName + " (R="+ReadsPct+"%,APFR="+APFReadsPct+"%,W="+WritesPct+"%)";

//					if (_logger.isDebugEnabled())
//						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": LogicalName("+LogicalName_pos+")='"+LogicalName+"', TotalIOs("+TotalIOs_pos+")='"+TotalIOs+"', Reads("+Reads_pos+")='"+Reads+"', APFReads("+APFReads_pos+")='"+APFReads+"', Writes("+Writes_pos+")='"+Writes+"'.");

					sumReads    += Reads   .doubleValue();
//					sumApfReads += APFReads.doubleValue();
					sumWrites   += Writes  .doubleValue();
					sumMisc     += Misc    .doubleValue();

					if (generateTotalIo)
						dataset.addValue(TotalIOs.doubleValue(), LogicalName, "TotalIOs");

					if (generateRead)
						dataset.addValue(Reads.doubleValue(),    LogicalName, "Reads");

//					if (generateApfRead)
//						dataset.addValue(APFReads.doubleValue(), LogicalName, "APFReads");

					if (generateWrite)
						dataset.addValue(Writes.doubleValue(),   LogicalName, "Writes");

					if (generateMisc)
						dataset.addValue(Misc.doubleValue(),     LogicalName, "Misc");
				}
				
				if (generateRwPct)
				{
					dataset.addValue(sumReads,  "Reads",  "ReadWritePct");
					dataset.addValue(sumWrites, "Writes", "ReadWritePct");
				}

//				if (generateRvsApfPct)
//				{
//					double sumNormalReads = sumReads - sumApfReads;
//
//					dataset.addValue(sumNormalReads, "NormalReads", "NormalReadVsApfRead");
//					dataset.addValue(sumApfReads,    "APFReads",    "NormalReadVsApfRead");
//				}
				
			}
		}
		else
		{
//			generateDummy = true;
			if (generateDummy)
			{
				dataset.addValue(1, "Dummy Device A", "TotalIOs");
				dataset.addValue(2, "Dummy Device B", "TotalIOs");
				dataset.addValue(3, "Dummy Device C", "TotalIOs");
				dataset.addValue(4, "Dummy Device D", "TotalIOs");

				dataset.addValue(5, "Dummy Device E", "Reads");
				dataset.addValue(6, "Dummy Device F", "Reads");
				dataset.addValue(7, "Dummy Device G", "Reads");

				dataset.addValue(8, "Dummy Device A", "APFReads");
				dataset.addValue(9, "Dummy Device B", "APFReads");

				dataset.addValue(10,"Dummy Device C", "Writes");
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
//		final String          chartTitle              = "Device IO Graph - by TotalIOs, Reads, APFReads, Writes";
		final String          chartTitle              = "Device IO Graph - by TotalIOs, Reads, Writes, Misc";
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

		// Use 70% for table, 30% for graph;
		return 7 * (mainSplitPane.getSize().width/10);
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

		final JCheckBox sampleLatencyInMs_chk = new JCheckBox("Latence in Milli Seconds (instead of pica seconds)", Configuration.getCombinedConfiguration().getBooleanProperty(CmSysDiskIo.PROPKEY_sample_LatencyInMs, CmSysDiskIo.DEFAULT_sample_LatencyInMs));

		final JCheckBox generateRwPct_chk     = new JCheckBox("Read/Write Pct");
//		final JCheckBox generateRvsApfPct_chk = new JCheckBox("Read/APF Pct");
		final JCheckBox generateTotalIo_chk   = new JCheckBox("TotalIOs");
		final JCheckBox generateRead_chk      = new JCheckBox("Reads");
//		final JCheckBox generateApfRead_chk   = new JCheckBox("APFReads");
		final JCheckBox generateWrite_chk     = new JCheckBox("Writes");
		final JCheckBox generateMisc_chk      = new JCheckBox("Misc");

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

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		generateRwPct_chk    .setToolTipText("<html>Include 'Read/Write Percentage' data in the Graph.</html>");
//		generateRvsApfPct_chk.setToolTipText("<html>Include 'Normal Read verses AsyncPreFetch Read Percentage' data in the Graph.</html>");
		generateTotalIo_chk  .setToolTipText("<html>Include 'TotalIOs' data in the Graph.</html>");
		generateRead_chk     .setToolTipText("<html>Include 'Reads'    data in the Graph.</html>");
//		generateApfRead_chk  .setToolTipText("<html>Include 'APFReads' data in the Graph.</html>");
		generateWrite_chk    .setToolTipText("<html>Include 'Writes'   data in the Graph.</html>");
		generateMisc_chk     .setToolTipText("<html>Include 'Misc'     data in the Graph.</html>");

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		graphType_cbx.setSelectedItem(orientation);

		generateRwPct_chk    .setSelected(conf.getBooleanProperty(PROPKEY_generateRwPct,     DEFAULT_generateRwPct));
//		generateRvsApfPct_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateRvsApfPct, DEFAULT_generateRvsApfPct));
		generateTotalIo_chk  .setSelected(conf.getBooleanProperty(PROPKEY_generateTotalIo,   DEFAULT_generateTotalIo));
		generateRead_chk     .setSelected(conf.getBooleanProperty(PROPKEY_generateRead,      DEFAULT_generateRead));
//		generateApfRead_chk  .setSelected(conf.getBooleanProperty(PROPKEY_generateApfRead,   DEFAULT_generateApfRead));
		generateWrite_chk    .setSelected(conf.getBooleanProperty(PROPKEY_generateWrite,     DEFAULT_generateWrite));
		generateMisc_chk     .setSelected(conf.getBooleanProperty(PROPKEY_generateMisc,      DEFAULT_generateMisc));

		enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));

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

		generateRwPct_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateRwPct, ((JCheckBox)e.getSource()).isSelected());
			}
		});
//		generateRvsApfPct_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_generateRvsApfPct, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
		generateTotalIo_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateTotalIo, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		generateRead_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateRead, ((JCheckBox)e.getSource()).isSelected());
			}
		});
//		generateApfRead_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_generateApfRead, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});
		generateWrite_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateWrite, ((JCheckBox)e.getSource()).isSelected());
			}
		});
		generateMisc_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(PROPKEY_generateMisc, ((JCheckBox)e.getSource()).isSelected());
			}
		});

		sampleLatencyInMs_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(CmSysDiskIo.PROPKEY_sample_LatencyInMs, ((JCheckBox)e.getSource()).isSelected());
				
				// This will tell the CM to generate a new SQL Statement
				getCm().setSql(null);
			}
		});
				
		// ADD to panel
		panel.add(sampleLatencyInMs_chk, "wrap");
		
		panel.add(enableGraph_chk,       "split");
		panel.add(graphType_lbl,         "");
		panel.add(graphType_cbx,         "wrap");

		panel.add(generateTotalIo_chk,   "split");
		panel.add(generateRead_chk,      "");
//		panel.add(generateApfRead_chk,   "");
		panel.add(generateWrite_chk,     "");
		panel.add(generateRwPct_chk,     "");
//		panel.add(generateRvsApfPct_chk, "wrap");
		panel.add(generateMisc_chk,      "wrap");

		panel.add(showLegend_chk,        "wrap");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);
		
		return panel;
	}
}
