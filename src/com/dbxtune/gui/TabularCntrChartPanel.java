/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Alarm-Coverage*, *## Batch-Coverage*
 *                                      *Error-Tracking* and *Report-Handling*
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
package com.dbxtune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

/**
 * A panel with "local graphs": graphs beside (or above) the table, made of the rows in the table.
 * <p>
 * The graphs are the ones the CM declares in {@link CountersModel#createChartDescriptors()}, which is also what the
 * web UI (DbxCentral, Counter Details) draws its charts from. So a CM says WHAT to chart in one place, and gets the
 * same graphs in both: this panel is what {@link CountersModel} creates for a CM that has chart descriptors and no
 * panel of its own.
 * <p>
 * As in the hand-coded panels (CmPCacheModuleUsagePanel is the model for this one) the graphs are made of what is
 * VISIBLE in the table: the rows that pass the filters, and the counter type (Absolute, Diff or Rate) that is shown.
 */
public class TabularCntrChartPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID = 1L;

	// The properties are per CM: <CmName>.graph.enable ...
	private static final String  PROPSUFFIX_enableGraph = ".graph.enable";
	private static final boolean DEFAULT_enableGraph    = true;

	private static final String  PROPSUFFIX_graphType   = ".graph.type";     // for the graphs that can be both a pie and bars
	private static final String  VALUE_graphType_PIE    = "PIE";
	private static final String  VALUE_graphType_BAR    = "BAR";
	private static final String  DEFAULT_graphType      = VALUE_graphType_PIE;

	private static final String  PROPSUFFIX_showLegend  = ".graph.show.legend";
	private static final boolean DEFAULT_showLegend     = true;

	// The same colors as the web UI uses for a percent bar: below the warning threshold, above it, above the critical threshold
	private static final Color   COLOR_PCT_OK   = new Color(0x28, 0xa7, 0x45);
	private static final Color   COLOR_PCT_WARN = new Color(0xfd, 0x7e, 0x14);
	private static final Color   COLOR_PCT_CRIT = new Color(0xdc, 0x35, 0x45);

	// With more bars than this they are drawn standing (and the labels tilted), otherwise lying (the labels are easier to read)
	private static final int     MAX_BARS_HORIZONTAL = 15;

	public TabularCntrChartPanel(CountersModel cm)
	{
		super(cm);
	}

	private List<CmChartDescriptor> getChartDescriptors()
	{
		CountersModel cm = getCm();
		List<CmChartDescriptor> list = cm == null ? null : cm.getChartDescriptors();
		return list == null ? Collections.<CmChartDescriptor>emptyList() : list;
	}

	private String propKey(String suffix)
	{
		return (getCm() == null ? getName() : getCm().getName()) + suffix;
	}

	//-----------------------------------------------------------------------------------
	// The data of ONE chart
	//-----------------------------------------------------------------------------------

	/** What a chart is made of: the labels (the biggest first), and for each label one value per value column */
	public static class ChartData
	{
		public List<String>   labels    = new ArrayList<>();
		public List<double[]> values    = new ArrayList<>();
		public List<Double>   barLabels = new ArrayList<>();   // the value of 'barLabelColumn' for each label (null when the descriptor has none)
	}

	private static int findViewColumn(JTable table, String colName)
	{
		for (int c = 0; colName != null && c < table.getColumnCount(); c++)
		{
			if (colName.equals(table.getColumnName(c)))
				return c;
		}
		return -1;
	}

	private static double toDouble(Object val)
	{
		if (val instanceof Number)
			return ((Number) val).doubleValue();

		try { return val == null ? 0 : Double.parseDouble(val.toString().trim()); }
		catch (NumberFormatException ex) { return 0; }
	}

	/**
	 * The rows of the table made into the labels and values of a chart, the way the descriptor says.<br>
	 * This is the same as _cmChartExtractData() in the web UI (dbxGraphCounterDetails.js): keep the two alike.
	 * (Not done here: 'extraSources'. The only CM that uses it has a panel of its own.)
	 *
	 * @return null if a column that the chart needs is not in the table (it can be hidden)
	 */
	public static ChartData extractData(CmChartDescriptor desc, JTable table)
	{
		int labelPos = findViewColumn(table, desc.labelColumn);
		if (labelPos == -1 || desc.valueColumns == null)
			return null;

		int[] valuePos = new int[desc.valueColumns.length];
		for (int i = 0; i < valuePos.length; i++)
		{
			valuePos[i] = findViewColumn(table, desc.valueColumns[i]);
			if (valuePos[i] == -1)
				return null;
		}

		int groupByPos  = findViewColumn(table, desc.groupByColumn);
		int barLabelPos = findViewColumn(table, desc.barLabelColumn);
		int filterPos   = findViewColumn(table, desc.filterColumn);

		List<Integer> prefixPos = new ArrayList<>();
		for (int i = 0; desc.labelPrefixColumns != null && i < desc.labelPrefixColumns.length; i++)
		{
			int pos = findViewColumn(table, desc.labelPrefixColumns[i]);
			if (pos != -1)
				prefixPos.add(pos);
		}

		String aggFunc = desc.groupAggFunc == null ? CmChartDescriptor.AGG_SUM : desc.groupAggFunc;

		// label -> values (rows with the same label are put together, with 'groupAggFunc')
		final Map<String, double[]> aggregated = new LinkedHashMap<>();
		Map<String, Integer>        rowCounts  = new LinkedHashMap<>();
		Map<String, Double>         barLabels  = new LinkedHashMap<>();

		for (int r = 0; r < table.getRowCount(); r++)
		{
			Object labelObj = table.getValueAt(r, groupByPos != -1 ? groupByPos : labelPos);
			String label    = labelObj == null ? "" : labelObj.toString();
			if (label.isEmpty())
				continue;

			if (desc.skipValue != null && desc.skipValue.equals(String.valueOf(table.getValueAt(r, labelPos))))
				continue;

			if (filterPos != -1 && ! String.valueOf(table.getValueAt(r, filterPos)).equals(desc.filterValue))
				continue;

			if (groupByPos == -1)
			{
				String prefix = "";
				for (int pos : prefixPos)
				{
					Object p = table.getValueAt(r, pos);
					if (p != null && ! p.toString().isEmpty())
						prefix += p + ".";
				}
				label = prefix + label;
			}

			double[] vals    = new double[valuePos.length];
			boolean  allZero = true;
			for (int i = 0; i < vals.length; i++)
			{
				vals[i] = toDouble(table.getValueAt(r, valuePos[i]));
				if (vals[i] != 0)
					allZero = false;
			}
			if (desc.skipZeroRows && allZero)
				continue;

			double[] sum = aggregated.get(label);
			if (sum == null)
			{
				aggregated.put(label, vals);
				rowCounts .put(label, 1);
			}
			else if (CmChartDescriptor.AGG_FIRST.equals(aggFunc))
			{
				continue;
			}
			else
			{
				rowCounts.put(label, rowCounts.get(label) + 1);
				for (int i = 0; i < vals.length; i++)
					sum[i] = CmChartDescriptor.AGG_MAX.equals(aggFunc) ? Math.max(sum[i], vals[i]) : sum[i] + vals[i];
			}

			if (barLabelPos != -1)
			{
				double barLabel = toDouble(table.getValueAt(r, barLabelPos));
				Double prev     = barLabels.get(label);
				barLabels.put(label, prev == null ? barLabel : Math.max(prev, barLabel));
			}
		}

		if (CmChartDescriptor.AGG_AVG.equals(aggFunc))
		{
			for (Map.Entry<String, double[]> e : aggregated.entrySet())
			{
				for (int i = 0; i < e.getValue().length; i++)
					e.getValue()[i] = e.getValue()[i] / rowCounts.get(e.getKey());
			}
		}

		// The biggest first (by the first value column)
		List<String> labels = new ArrayList<>(aggregated.keySet());
		Collections.sort(labels, (a, b) -> Double.compare(aggregated.get(b)[0], aggregated.get(a)[0]));

		// maxItems: the rest becomes ONE item
		if (desc.maxItems > 0 && labels.size() > desc.maxItems)
		{
			double[] others = new double[valuePos.length];
			for (String label : labels.subList(desc.maxItems, labels.size()))
			{
				for (int i = 0; i < others.length; i++)
					others[i] += aggregated.get(label)[i];
			}
			labels = new ArrayList<>(labels.subList(0, desc.maxItems));
			labels.add("Others");
			aggregated.put("Others", others);
		}

		ChartData data = new ChartData();
		for (String label : labels)
		{
			data.labels   .add(label);
			data.values   .add(aggregated.get(label));
			data.barLabels.add(barLabelPos == -1 ? null : barLabels.get(label));
		}
		return data;
	}

	//-----------------------------------------------------------------------------------
	// ONE chart
	//-----------------------------------------------------------------------------------

	/**
	 * @param asPie       for a descriptor that can be both (CHART_TYPE_DUAL_PIE_BAR): draw it as a pie (true) or as bars (false)
	 * @param background  the background of the panel the chart is put in
	 */
	public static JFreeChart createChart(final CmChartDescriptor desc, final ChartData data, boolean asPie, boolean showLegend, Paint background)
	{
		final Font            legendItemFont   = new Font("SansSerif", Font.PLAIN, 10);
		final RectangleInsets legendItemInsets = new RectangleInsets(0, 0, 0, 0);
		final Font            labelFont        = new Font("SansSerif", Font.PLAIN, 9);

		boolean isPie = CmChartDescriptor.CHART_TYPE_PIE.equals(desc.chartType) || (CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR.equals(desc.chartType) && asPie);

		JFreeChart chart;
		if (isPie)
		{
			// A pie is ONE value: the first value column (as in the web UI)
			String seriesName = desc.seriesLabels != null && desc.seriesLabels.length > 0 ? desc.seriesLabels[0] : desc.valueColumns[0];

			DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			for (int i = 0; i < data.labels.size(); i++)
				dataset.addValue(data.values.get(i)[0], data.labels.get(i), seriesName);

			chart = ChartFactory.createMultiplePieChart(null, dataset, TableOrder.BY_COLUMN, showLegend, true, false);

			MultiplePiePlot plot = (MultiplePiePlot) chart.getPlot();
			plot.setNoDataMessage("No data available");
			plot.setLimit(desc.pieOtherLimit);   // the slices that are smaller than this (0.05 = 5%) are put together as "Other"

			// The title below the pie says what the value is (stolen from jfreechart, MultiplePiePlot.java, constructor)
			JFreeChart subchart = plot.getPieChart();
			TextTitle seriesTitle = new TextTitle("Series Title", TextTitle.DEFAULT_FONT); // (overwritten with the name of the series when it is drawn)
			seriesTitle.setPosition(RectangleEdge.BOTTOM);
			subchart.setTitle(seriesTitle);
			subchart.setBackgroundPaint(background);

			@SuppressWarnings("rawtypes")
			PiePlot pie = (PiePlot) subchart.getPlot();
			pie.setLabelFont(labelFont);
			pie.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}, {0}, {1}", NumberFormat.getNumberInstance(), NumberFormat.getPercentInstance()));
			pie.setLabelGap(0.01D);
			pie.setMaximumLabelWidth(0.20D);
		}
		else
		{
			// One series for each value column. With the labels in the order of the data: the biggest first
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			for (int s = 0; s < desc.valueColumns.length; s++)
			{
				String seriesName = desc.seriesLabels != null && desc.seriesLabels.length > s ? desc.seriesLabels[s] : desc.valueColumns[s];
				for (int i = 0; i < data.labels.size(); i++)
					dataset.addValue(data.values.get(i)[s], seriesName, data.labels.get(i));
			}

			PlotOrientation orientation = data.labels.size() <= MAX_BARS_HORIZONTAL ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL;
			if (CmChartDescriptor.CHART_TYPE_HORIZONTAL_BAR.equals(desc.chartType))
				orientation = PlotOrientation.HORIZONTAL;

			boolean oneSeries = desc.valueColumns.length == 1;
			chart = ChartFactory.createStackedBarChart(null, null, null, dataset, orientation, showLegend && ! oneSeries, true, false);

			CategoryPlot plot = chart.getCategoryPlot();
			plot.setNoDataMessage("No data available");
			plot.getDomainAxis().setTickLabelFont(labelFont);
			plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(0.4f);
			if (orientation == PlotOrientation.VERTICAL)
				plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setTickLabelFont(labelFont);
			if (desc.isPercent)
				rangeAxis.setRange(0, 100);

			// A percent chart with one series: each bar gets the color of its value (green, orange above the warning threshold, red above the critical)
			StackedBarRenderer renderer = new StackedBarRenderer()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Paint getItemPaint(int row, int column)
				{
					if ( ! desc.isPercent || dataset.getRowCount() != 1 )
						return super.getItemPaint(row, column);

					Number val = dataset.getValue(row, column);
					if (val == null)                              return COLOR_PCT_OK;
					if (val.doubleValue() >= desc.thresholdCrit)  return COLOR_PCT_CRIT;
					if (val.doubleValue() >= desc.thresholdWarn)  return COLOR_PCT_WARN;
					return COLOR_PCT_OK;
				}
			};
			renderer.setBarPainter(new StandardBarPainter());   // plain bars (the default has a "glass" look, which makes the colors hard to tell apart)
			renderer.setShadowVisible(false);
			renderer.setDrawBarOutline(false);
			renderer.setMaximumBarWidth(0.15);
			renderer.setDefaultToolTipGenerator(plot.getRenderer().getDefaultToolTipGenerator());

			// 'barLabelColumn': a text on each bar ("FREE MB: 3 144")
			if (desc.barLabelColumn != null)
			{
				renderer.setDefaultItemLabelGenerator(new CategoryItemLabelGenerator()
				{
					@Override public String generateRowLabel   (CategoryDataset ds, int row)    { return String.valueOf(ds.getRowKey(row)); }
					@Override public String generateColumnLabel(CategoryDataset ds, int column) { return String.valueOf(ds.getColumnKey(column)); }
					@Override public String generateLabel      (CategoryDataset ds, int row, int column)
					{
						Double barLabel = row == 0 && column < data.barLabels.size() ? data.barLabels.get(column) : null;
						return barLabel == null ? null : (desc.barLabelPrefix == null ? "" : desc.barLabelPrefix) + NumberFormat.getNumberInstance().format(barLabel);
					}
				});
				renderer.setDefaultItemLabelsVisible(true);
				renderer.setDefaultItemLabelFont(labelFont);

				// Inside the bar when there is room, otherwise after it
				boolean horizontal = orientation == PlotOrientation.HORIZONTAL;
				renderer.setDefaultPositiveItemLabelPosition(horizontal
						? new ItemLabelPosition(ItemLabelAnchor.INSIDE3 , TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0)
						: new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0));
				renderer.setPositiveItemLabelPositionFallback(horizontal
						? new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3 , TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, 0)
						: new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0));
			}
			plot.setRenderer(renderer);
		}

		chart.setBackgroundPaint(background);
		chart.setTitle(new TextTitle(desc.title, TextTitle.DEFAULT_FONT));

		// The font size of the legend
		for (Object title : chart.getSubtitles())
		{
			if ( ! (title instanceof LegendTitle) )
				continue;
			LegendTitle lt = (LegendTitle) title;
			lt.setItemFont(legendItemFont);
			lt.setItemLabelPadding(legendItemInsets);
		}

		return chart;
	}

	//-----------------------------------------------------------------------------------
	// The panel
	//-----------------------------------------------------------------------------------

	/** true if the graphs go ABOVE the table (the first descriptor decides, as in the web UI), false if they go to the right of it */
	private boolean isGraphsAboveTable()
	{
		List<CmChartDescriptor> descriptors = getChartDescriptors();
		return ! descriptors.isEmpty() && CmChartDescriptor.SPLIT_VERTICAL.equals(descriptors.get(0).splitDir);
	}

	/** All the charts of the CM: beside each other when they are above the table, below each other when they are to the right of it */
	private JPanel createChartsPanel(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean asPie      = conf.getProperty(       propKey(PROPSUFFIX_graphType),  DEFAULT_graphType).equals(VALUE_graphType_PIE);
		boolean showLegend = conf.getBooleanProperty(propKey(PROPSUFFIX_showLegend), DEFAULT_showLegend);

		List<CmChartDescriptor> descriptors = getChartDescriptors();

		JPanel panel = new JPanel(isGraphsAboveTable() ? new GridLayout(1, 0) : new GridLayout(0, 1));
		for (CmChartDescriptor desc : descriptors)
		{
			ChartData data = dataTable == null ? null : extractData(desc, dataTable);
			if (data == null)
				data = new ChartData();

			panel.add( new ChartPanel(createChart(desc, data, asPie, showLegend, getBackground())) );
		}
		return panel;
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
		boolean enableGraph = conf.getBooleanProperty(propKey(PROPSUFFIX_enableGraph), DEFAULT_enableGraph);

		panel.removeAll();
		if (enableGraph)
			panel.add( createChartsPanel(null) );
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

		// 'splitRatio' is the part of the space that the TABLE gets
		List<CmChartDescriptor> descriptors = getChartDescriptors();
		double tableRatio = descriptors.isEmpty() ? 0.5 : descriptors.get(0).splitRatio;

		if (isGraphsAboveTable())
			return (int) ((1 - tableRatio) * mainSplitPane.getSize().height);

		return (int) (tableRatio * mainSplitPane.getSize().width);
	}

	@Override
	protected void setSplitPaneOptions(JSplitPane mainSplitPane, JPanel dataPanel, JPanel extendedInfoPanel)
	{
		mainSplitPane.setBorder(null);
		if (isGraphsAboveTable())
		{
			mainSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			mainSplitPane.add(extendedInfoPanel, JSplitPane.TOP);
			mainSplitPane.add(dataPanel,         JSplitPane.BOTTOM);
		}
		else
		{
			mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			mainSplitPane.add(dataPanel,         JSplitPane.LEFT);
			mainSplitPane.add(extendedInfoPanel, JSplitPane.RIGHT);
		}
		mainSplitPane.setDividerSize(5);

		mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
	}

	@Override
	public void updateExtendedInfoPanel()
	{
		JPanel panel     = getExtendedInfoPanel();
		GTable dataTable = getDataTable();
		if (panel == null) return;

		// If the panel is so small, make it bigger
		int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
		if (dividerLocation == 0)
		{
			JSplitPane mainSplitPane = getMainSplitPane();
			if (mainSplitPane != null)
				mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean enableGraph = conf.getBooleanProperty(propKey(PROPSUFFIX_enableGraph), DEFAULT_enableGraph);

		// Remove and add components to panel
		panel.removeAll();
		if (enableGraph)
			panel.add( createChartsPanel(dataTable) );
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

		final JCheckBox enableGraph_chk = new JCheckBox("Show Graph");
		final JCheckBox showLegend_chk  = new JCheckBox("Show Legend");

		final String[] graphTypeArr = {"Pie Chart", "Bar Graph"};
		final JLabel            graphType_lbl = new JLabel("Type");
		final JComboBox<String> graphType_cbx = new JComboBox<String>(graphTypeArr);

		String tooltip;
		tooltip =
			"<html>" +
			"If you want to include/exclude data in the graphs, use the 'filters' panel.<br>" +
			"The Graph is based on what is visible in the Table..." +
			"</html>";
		panel.setToolTipText(tooltip);

		enableGraph_chk.setToolTipText("Do you want the Graph to be visible at all...");
		showLegend_chk .setToolTipText("Show Legend, which describes all data value types");

		tooltip = "<html>Do you want the 'Graph' to be presented as 'Pie' or 'Bar' Graphs.<br>Some graphs are always bars (percent of a limit, for example).</html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		// SET INITIAL VALUES for components
		Configuration conf = Configuration.getCombinedConfiguration();
		graphType_cbx.setSelectedItem(conf.getProperty(propKey(PROPSUFFIX_graphType), DEFAULT_graphType).equals(VALUE_graphType_BAR) ? graphTypeArr[1] : graphTypeArr[0]);

		enableGraph_chk.setSelected(conf.getBooleanProperty(propKey(PROPSUFFIX_enableGraph), DEFAULT_enableGraph));
		showLegend_chk .setSelected(conf.getBooleanProperty(propKey(PROPSUFFIX_showLegend),  DEFAULT_showLegend));

		// ACTION LISTENERS
		enableGraph_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(propKey(PROPSUFFIX_enableGraph), ((JCheckBox)e.getSource()).isSelected());
				SwingUtils.setEnabled(getLocalOptionsPanel(), ((JCheckBox)e.getSource()).isSelected(), (JCheckBox)e.getSource());
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

				conf.setProperty(propKey(PROPSUFFIX_graphType), graphTypeArr[1].equals(graphType_cbx.getSelectedItem()) ? VALUE_graphType_BAR : VALUE_graphType_PIE);
				conf.save();

				updateExtendedInfoPanel();
			}
		});

		showLegend_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helperActionSave(propKey(PROPSUFFIX_showLegend), ((JCheckBox)e.getSource()).isSelected());
			}
		});

		// The type can only be chosen when some graph can be both
		boolean hasDualGraph = false;
		for (CmChartDescriptor desc : getChartDescriptors())
		{
			if (CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR.equals(desc.chartType))
				hasDualGraph = true;
		}

		// ADD to panel
		panel.add(enableGraph_chk,     hasDualGraph ? "split" : "wrap");
		if (hasDualGraph)
		{
			panel.add(graphType_lbl,   "");
			panel.add(graphType_cbx,   "wrap");
		}
		panel.add(showLegend_chk,      "wrap");

		// enable disable all subcomponents in panel
		SwingUtils.setEnabled(panel, enableGraph_chk.isSelected(), enableGraph_chk);

		return panel;
	}
}
