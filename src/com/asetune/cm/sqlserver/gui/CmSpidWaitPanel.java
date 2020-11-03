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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;
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
import com.asetune.cm.sqlserver.CmSpidWait;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ParameterDialog;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSpidWaitPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmSpidWaitPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSpidWait.CM_NAME;
	public static final String  TOOLTIP_sample_extraWhereClause = 
		"<html>" +
		"Add extra where clause to the query that fetches <i>data rows</i> for SPID's<br>" +
		"To check SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
		"<br>" +
		"<b>Examples:</b><br>" +
		"<b>- Only users with the login 'sa'</b><br>" +
		"<code>session_id in (select spid from sys.sysprocesses where loginame = 'sa')                                  </code><br>" +
		"<br>" +
		"<b>- Only with programs that has logged in via 'xxx'</b><br>" +
		"<code>session_id in (select spid from sys.sysprocesses where program_name = 'xxx')                             </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the host 'host99'</b><br>" +
		"<code>session_id in (select spid from sys.sysprocesses where hostname = 'host99')                              </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the IP address '192.168.0.123'</b><br>" +
		"<code>session_id in (select session_id from sys.dm_exec_connections where client_net_address = '192.168.0.123') </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in to ASE in the last 60 seconds</b><br>" +
		"<code>session_id in (select spid from sys.sysprocesses where datediff(ss,login_time,getdate()) &lt; 60)</code><br>" +
		"</html>";
	
	public static final String TOOLTIP_trendGraph_skipWaitTypeList = 
		"<html>" +
		"Edit what wait_type's that should be discarded from the Thrend Graph Summary Page<br>" +
		"<br>" +
		"If you got <b>spikes</b> in the graph, so the <b>importand</b> wait_type's gets lost...<br>" +
		"This is the place to edit what wait_type's to discard<br>" +
		"<br>" +
		"<b>Note</b>: This is a comma separeted list of TYPE's<br>" +
		"<br>" +
		"<b>Current List</b>: THE_SKIP_LIST_GOES_HERE<br>" +
		"</html>";


	private static final String  PROP_PREFIX           = CmSpidWait.CM_NAME;

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

//	private static final String  PROPKEY_includeWaitId250              = PROP_PREFIX + ".graph.include.id.250";
//	private static final boolean DEFAULT_includeWaitId250              = false;

//	private static final String  PROPKEY_includeSystemThreads          = PROP_PREFIX + ".graph.include.system.threads";
//	private static final boolean DEFAULT_includeSystemThreads          = false;

	private static final String  PROPKEY_generateEvent                 = PROP_PREFIX + ".graph.generate.event";
	private static final boolean DEFAULT_generateEvent                 = true;

	private static final String  PROPKEY_generateEventWaitTime         = PROP_PREFIX + ".graph.generate.event.waitTime";
	private static final boolean DEFAULT_generateEventWaitTime         = true;

	private static final String  PROPKEY_generateEventWaitCount        = PROP_PREFIX + ".graph.generate.event.waitCount";
	private static final boolean DEFAULT_generateEventWaitCount        = true;

	private static final String  PROPKEY_generateEventWaitTimePerCount = PROP_PREFIX + ".graph.generate.event.waitTimePerCount";
	private static final boolean DEFAULT_generateEventWaitTimePerCount = true;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_generateDummy,                 DEFAULT_generateDummy);
		Configuration.registerDefaultValue(PROPKEY_enableGraph,                   DEFAULT_enableGraph);
		Configuration.registerDefaultValue(PROPKEY_graphType,                     DEFAULT_graphType);
		Configuration.registerDefaultValue(PROPKEY_showLegend,                    DEFAULT_showLegend);
//		Configuration.registerDefaultValue(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250);
//		Configuration.registerDefaultValue(PROPKEY_includeSystemThreads,          DEFAULT_includeSystemThreads);
		Configuration.registerDefaultValue(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);
		Configuration.registerDefaultValue(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTimePerCount);
	}

	public CmSpidWaitPanel(CountersModel cm)
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

		// HIGHLIGHTER that changes color when a new SPID number is on next row...

		if (conf != null) 
			colorStr = conf.getProperty(getName()+".color.group");

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			boolean[] _rowIsHighlighted = new boolean[0];
			int       _lastRowId        = 0;     // Used to sheet on table refresh

			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.row == 0)
					return false;

				// Resize array if it's to small
				if (_rowIsHighlighted.length < adapter.getRowCount())
					_rowIsHighlighted = new boolean[adapter.getRowCount()];

				// Lets try to sheet a bit, if we are of some row as last invocation, reuse that decision
				if (_lastRowId == adapter.row)
					return _rowIsHighlighted[adapter.row];
				_lastRowId = adapter.row;

				// Lets get values of "change color" column
				int    spidCol      = adapter.getColumnIndex("session_id");
				int    thisModelRow = adapter.convertRowIndexToModel(adapter.row);
				int    prevModelRow = adapter.convertRowIndexToModel(adapter.row - 1);

				Object thisSpid    = adapter.getValueAt(thisModelRow, spidCol);
				Object prevSpid    = adapter.getValueAt(prevModelRow, spidCol);

				if (thisSpid == null) thisSpid = "dummy";
				if (prevSpid == null) prevSpid = "dummy";

				// Previous rows highlight will be a decision to keep or invert the highlight
				boolean prevRowIsHighlighted = _rowIsHighlighted[adapter.row - 1];

				if (thisSpid.equals(prevSpid))
				{
					// Use same highlight value as previous row
					boolean isHighlighted = prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
				else
				{
					// Invert previous highlight value
					boolean isHighlighted = ! prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
			}
		}, SwingUtils.parseColor(colorStr, HighlighterFactory.GENERIC_GRAY), null));
	}

	private CategoryDataset createDataset(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy                 = conf.getBooleanProperty(PROPKEY_generateDummy,                 DEFAULT_generateDummy);
//		boolean includeWaitId250              = conf.getBooleanProperty(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250);
//		boolean includeSystemThreads          = conf.getBooleanProperty(PROPKEY_includeSystemThreads,          DEFAULT_includeSystemThreads);
//		boolean generateEvent                 = conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent);
		boolean generateEvent                 = true;
		boolean generateEventWaitTime         = conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime);
		boolean generateEventWaitCount        = conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount);
		boolean generateEventWaitTimePerCount = conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTimePerCount);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int WaitType_pos         = dataTable.findViewColumn("wait_type");
			int WaitTime_pos         = dataTable.findViewColumn("wait_time_ms"); 
			int WaitCount_pos        = dataTable.findViewColumn("waiting_tasks_count");
			int WaitTimePerCount_pos = dataTable.findViewColumn("WaitTimePerCount"); 

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Double> eventWaitTime          = new LinkedHashMap<String, Double>();
				Map<String, Double> eventWaitCount         = new LinkedHashMap<String, Double>();
				Map<String, Double> eventWaitTimePerCount  = new LinkedHashMap<String, Double>();

				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String WaitType         = (String)dataTable.getValueAt(r, WaitType_pos);
					Number WaitTime         = (Number)dataTable.getValueAt(r, WaitTime_pos);
					Number WaitCount        = (Number)dataTable.getValueAt(r, WaitCount_pos);
					Number WaitTimePerCount = (Number)dataTable.getValueAt(r, WaitTimePerCount_pos);

					// UserName was introduced in ASE 15.0.2 ESD#3, so if the column wasn't found dont get it 
//					if (UserName_pos > 0)
//						UserName = (String)dataTable.getValueAt(r, UserName_pos);

					// SKIP Wait EventId 250
//					if ( ! includeWaitId250 && WaitEventID.intValue() == 250)
//						continue;

					// SKIP System Threads
//					if ( ! includeSystemThreads && "".equals(UserName) )
//						continue;

//					if (_logger.isDebugEnabled())
//						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": ClassName("+ClassName_pos+")='"+ClassName+"', EventName("+EventName_pos+")='"+EventName+"', WaitTime("+WaitTime_pos+")='"+WaitTime+"', WaitCount("+WaitCount_pos+")='"+WaitCount+"', WaitTimePerCount("+WaitTimePerCount_pos+")='"+WaitTimePerCount+"'.");
					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": WaitType["+WaitType_pos+"]='"+WaitType+"', WaitTime["+WaitTime_pos+"]='"+WaitTime+"', WaitCount["+WaitCount_pos+"]='"+WaitCount+"', WaitTimePerCount["+WaitTimePerCount_pos+"]='"+WaitTimePerCount+"'.");

					if (generateEvent)
					{
						Double sumWaitTime         = eventWaitTime        .get(WaitType);
						Double sumWaitCount        = eventWaitCount       .get(WaitType);
						Double sumWaitTimePerCount = eventWaitTimePerCount.get(WaitType);

						eventWaitTime        .put(WaitType, new Double(sumWaitTime        ==null ? WaitTime        .doubleValue() : sumWaitTime         + WaitTime        .doubleValue()) );
						eventWaitCount       .put(WaitType, new Double(sumWaitCount       ==null ? WaitCount       .doubleValue() : sumWaitCount        + WaitCount       .doubleValue()) );
						eventWaitTimePerCount.put(WaitType, new Double(sumWaitTimePerCount==null ? WaitTimePerCount.doubleValue() : sumWaitTimePerCount + WaitTimePerCount.doubleValue()) );
					}
				} // end: loop dataTable

				if (generateEvent)
				{
					if (generateEventWaitTime)
					{
						for (Map.Entry<String,Double> entry : eventWaitTime.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, key, "wait_time_ms");
						}
					}
					
					if (generateEventWaitCount)
					{
						for (Map.Entry<String,Double> entry : eventWaitCount.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, key, "waiting_task_count");
						}
					}

					if (generateEventWaitTimePerCount)
					{
						for (Map.Entry<String,Double> entry : eventWaitTimePerCount.entrySet()) 
						{
							String key = entry.getKey();
							Double val = entry.getValue();
							dataset.addValue(val, key, "WaitTimePerCount");
						}
					}
				} //end: generateEventWaitTime
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
		final String          chartTitle              = "Summary Graph - by: wait_type";
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

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
//		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setLayout(new MigLayout("flowy, ins 0, gap 0", "", "0[0]0"));

//		final JCheckBox includeWaitId250_chk              = new JCheckBox("Include '[250] waiting for incoming network data' in graphs.");
//		final JCheckBox includeSystemThreads_chk          = new JCheckBox("Include System SPID's in graphs.");
		final JCheckBox generateEvent_chk                 = new JCheckBox("Genereate Graphs for");
		final JCheckBox generateEventWaitTime_chk         = new JCheckBox("wait_time_ms");
		final JCheckBox generateEventWaitCount_chk        = new JCheckBox("waiting_tasks_count");
		final JCheckBox generateEventWaitTimePerCount_chk = new JCheckBox("WaitTimePerCount");

		final JCheckBox enableGraph_chk = new JCheckBox("Show Graph");
		final JCheckBox showLegend_chk  = new JCheckBox("Show Legend");

		String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		final JLabel            graphType_lbl    = new JLabel("Type");
		final JComboBox<String> graphType_cbx    = new JComboBox<String>(graphTypeArr);

		final RSyntaxTextAreaX extraWhereClause_txt = new RSyntaxTextAreaX();
		final JButton          extraWhereClause_but = new JButton("Apply Extra Where Clause");

		final JButton         trendGraph_settings_but = new JButton("Summary TrendGraph Settings");

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
//			"Another way to include/exclude event types in the graphs, is to use the 'filters' panel.<br>" +
//			"The Graph is based on what is visible in the Table..." +
//			"</html>";
//		includeWaitId250_chk.setToolTipText(tooltip);

//		tooltip = 
//			"<html>" +
//			"If you want to include System SPID's in graphs..<br>" +
//			"This is done by checking is 'UserName' has a value, the column 'UserName' was introduced in ASE 15.0.2 ESD#3.<br>" +
//			"<br>" +
//			"Another way to include/exclude event types in the graphs, is to use the 'filters' panel.<br>" +
//			"The Graph is based on what is visible in the Table..." +
//			"</html>";
//		includeSystemThreads_chk.setToolTipText(tooltip);

		tooltip = "<html>Do you want the Graph to be presented as 'Pie' or 'Bar' Graphs.<br></html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		generateEvent_chk.setToolTipText("<html>Include 'Events' data in the Graph.</html>");
//		generateClass_chk.setToolTipText("<html>Include 'Classes' data in the Graph.</html>");

		extraWhereClause_but.setToolTipText(TOOLTIP_sample_extraWhereClause);
		extraWhereClause_txt.setToolTipText(TOOLTIP_sample_extraWhereClause);

		tooltip = TOOLTIP_trendGraph_skipWaitTypeList.replace("THE_SKIP_LIST_GOES_HERE", Configuration.getCombinedConfiguration().getProperty(CmSpidWait.PROPKEY_trendGraph_skipWaitTypeList, CmSpidWait.DEFAULT_trendGraph_skipWaitTypeList));
		trendGraph_settings_but.setToolTipText(tooltip);

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_graphType, DEFAULT_graphType);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_graphType_PIE)) orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_graphType_BAR)) orientation = graphTypeArr[1];
		graphType_cbx.setSelectedItem(orientation);

//		includeWaitId250_chk             .setSelected(conf.getBooleanProperty(PROPKEY_includeWaitId250,              DEFAULT_includeWaitId250));
//		includeSystemThreads_chk         .setSelected(conf.getBooleanProperty(PROPKEY_includeSystemThreads,          DEFAULT_includeSystemThreads));
//		generateEvent_chk                .setSelected(conf.getBooleanProperty(PROPKEY_generateEvent,                 DEFAULT_generateEvent));
		generateEvent_chk                .setSelected(true); // It's hidden... so always true
		generateEventWaitTime_chk        .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTime,         DEFAULT_generateEventWaitTime));
		generateEventWaitCount_chk       .setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitCount,        DEFAULT_generateEventWaitCount));
		generateEventWaitTimePerCount_chk.setSelected(conf.getBooleanProperty(PROPKEY_generateEventWaitTimePerCount, DEFAULT_generateEventWaitTimePerCount));

		enableGraph_chk.setSelected(conf.getBooleanProperty(PROPKEY_enableGraph, DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(PROPKEY_showLegend,  DEFAULT_showLegend));


//		Configuration conf = Configuration.getCombinedConfiguration();
		String extraWhereClause = (conf == null ? CmSpidWait.DEFAULT_sample_extraWhereClause : conf.getProperty(CmSpidWait.PROPKEY_sample_extraWhereClause, CmSpidWait.DEFAULT_sample_extraWhereClause));

		extraWhereClause_txt.setText(extraWhereClause);
		extraWhereClause_txt.setHighlightCurrentLine(false);
		extraWhereClause_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		extraWhereClause_txt.setName(CmSpidWait.PROPKEY_sample_extraWhereClause);

		
		// ACTION LISTENERS
//		includeWaitId250_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_includeWaitId250, ((JCheckBox)e.getSource()).isSelected());
//			}
//		});

//		includeSystemThreads_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				helperActionSave(PROPKEY_includeSystemThreads,                      ((JCheckBox)e.getSource()).isSelected());
//				helperActionSave(CmSpidWait.PROPKEY_trendGraph_skipSystemThreads, ! ((JCheckBox)e.getSource()).isSelected());
//			}
//		});

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

//		generateEvent_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				boolean b = ((JCheckBox)e.getSource()).isSelected();
//				helperActionSave(PROPKEY_generateEvent, ((JCheckBox)e.getSource()).isSelected());
//				generateEventWaitTime_chk        .setEnabled(b);
//				generateEventWaitCount_chk       .setEnabled(b);
//				generateEventWaitTimePerCount_chk.setEnabled(b);
//			}
//		});
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



		extraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidWait.PROPKEY_sample_extraWhereClause, extraWhereClause_txt.getText().trim());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		trendGraph_settings_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				openPropertiesEditor();

				// Update the tooltip text
				trendGraph_settings_but.setToolTipText( TOOLTIP_trendGraph_skipWaitTypeList.replace("THE_SKIP_LIST_GOES_HERE", Configuration.getCombinedConfiguration().getProperty(CmSpidWait.PROPKEY_trendGraph_skipWaitTypeList, CmSpidWait.DEFAULT_trendGraph_skipWaitTypeList)) );
			}
		});

		JPanel panelL = SwingUtils.createPanel("Dummy Left",  false);
		JPanel panelR = SwingUtils.createPanel("Dummy Right", false);
		panelL.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panelR.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		panel.add(panelL,                             "wrap");
		panel.add(panelR,                             "push, grow, wrap");

		// ADD to panel
//		panelL.add(includeWaitId250_chk,              "wrap");
		panelL.add(enableGraph_chk,                   "split");
		panelL.add(graphType_lbl,                     "");
		panelL.add(graphType_cbx,                     "wrap");
//		panelL.add(includeSystemThreads_chk,          "wrap");

//		panelL.add(generateEvent_chk,                 "split");
		panelL.add(generateEventWaitTime_chk,         "split");
		panelL.add(generateEventWaitCount_chk,        "wrap");
		panelL.add(generateEventWaitTimePerCount_chk, "split");

		panelL.add(showLegend_chk,                    "wrap");
//		panelL.add(includeWaitId250_chk,              "wrap");

		panelR.add(extraWhereClause_txt,              "push, grow, wrap");
		panelR.add(extraWhereClause_but,              "wrap");
		panelR.add(trendGraph_settings_but,           "wrap");


		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);

		if (enableGraph_chk.isSelected())
		{
			// initial enabled or not
			generateEventWaitTime_chk        .setEnabled(generateEvent_chk.isSelected());
			generateEventWaitCount_chk       .setEnabled(generateEvent_chk.isSelected());
			generateEventWaitTimePerCount_chk.setEnabled(generateEvent_chk.isSelected());
		}

		return panel;
	}

	public static void openPropertiesEditor()
	{
		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		String key1 = "wait_type's to skip (comma separated list)";
//		String key2 = "ClassNames's to skip (comma separated list)";
//		String key3 = "UserNames's to skip (comma separated list)";
//		String key4 = "Skip System SPID's (true or false)";

		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
		in.put(key1, Configuration.getCombinedConfiguration().getProperty( CmSpidWait.PROPKEY_trendGraph_skipWaitTypeList,  CmSpidWait.DEFAULT_trendGraph_skipWaitTypeList));
//		in.put(key2, Configuration.getCombinedConfiguration().getProperty( CmSpidWait.PROPKEY_trendGraph_skipWaitClassList, CmSpidWait.DEFAULT_trendGraph_skipWaitClassList));
//		in.put(key3, Configuration.getCombinedConfiguration().getProperty( CmSpidWait.PROPKEY_trendGraph_skipUserNameList,  CmSpidWait.DEFAULT_trendGraph_skipUserNameList));
//		in.put(key4, Configuration.getCombinedConfiguration().getProperty( CmSpidWait.PROPKEY_trendGraph_skipSystemThreads, CmSpidWait.DEFAULT_trendGraph_skipSystemThreads+""));

		Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "SPID WaitEvent's to skip", in, false);

		if (results != null)
		{
			tmpConf.setProperty(CmSpidWait.PROPKEY_trendGraph_skipWaitTypeList,  results.get(key1));
//			tmpConf.setProperty(CmSpidWait.PROPKEY_trendGraph_skipWaitClassList, results.get(key2));
//			tmpConf.setProperty(CmSpidWait.PROPKEY_trendGraph_skipUserNameList,  results.get(key3));
//			tmpConf.setProperty(CmSpidWait.PROPKEY_trendGraph_skipSystemThreads, results.get(key4));

			tmpConf.save();
		}
	}
}
