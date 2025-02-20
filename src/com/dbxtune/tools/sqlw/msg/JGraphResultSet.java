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
package com.dbxtune.tools.sqlw.msg;

import java.awt.Dimension;
import java.awt.Image;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import com.dbxtune.Version;
import com.dbxtune.graph.CategoryAxisSparselyLabeled;
import com.dbxtune.graph.CategoryPlotSparselyLabeled;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.pipe.PipeCommandGraph;
import com.dbxtune.sql.pipe.PipeCommandGraph.GraphType;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class JGraphResultSet
extends JComponent
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

//	private List<String> _graphMessages = null;
	private ResultSetTableModel _tm = null;
	private PipeCommandGraph _pipeCmd = null;
	private JFreeChart _chart = null;
	
	/** Used by createColumnPosList */
	private enum CreateColumnType {
		KEY_COLS, GROUP_COLS, VALUE_COLS, TS_VALUE_COLS
	};

	public static final String PROPKEY_BAR_ITEM_MARGIN = "GraphResultSet.bar.chart.item.margin";
	public static final double DEFAULT_BAR_ITEM_MARGIN = 0.075; // 7.5 %

	/**
	 * CONSTRUCTOR
	 * 
	 * @param rstm
	 * @param pipeCommandGraph
	 */
	public JGraphResultSet(final ResultSetTableModel rstm, PipeCommandGraph pipeCommandGraph)
	{
		_tm = rstm;
		_pipeCmd = pipeCommandGraph;
		
		if (_pipeCmd.isDebugEnabled())
		{
			_pipeCmd.addDebugMessage("CmdLineParams: " + _pipeCmd.getCmdStr());
			_pipeCmd.addDebugMessage("CmdLineParams: " + _pipeCmd.getCmdLineParams());
		}
	}
	

	public JFrame createWindow()
	{
		JFrame frame = new JFrame("Graph/Chart ResultSet");

		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/create_graph_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/create_graph_32.png");

		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			frame.setIconImages(iconList);
		}

		// Create info panel
		RSyntaxTextArea info_txt = new RSyntaxTextArea(4, 10);
		RTextScrollPane info_scroll = new RTextScrollPane(info_txt);
		JPanel infoPanel = SwingUtils.createPanel("Messages", true, new MigLayout("insets 0"));
		infoPanel.add(info_scroll, "push, grow");

		// Set that we can "collapse" the JSplitPane by sliding the divider up to top
		infoPanel.setMinimumSize(new Dimension(0, 0));
		info_txt.setToolTipText("<html>"
				+ "To <i>collapse</i> the information message panel.<br>"
				+ "Grab the <i>divider</i> and push at towards the <b>top</b> of the window.<br>"
				+ "<html>");

//		JPanel chartPanel = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
		JPanel chartPanel = SwingUtils.createPanel("Graph/Chart ResultSet", true, new MigLayout("insets 0"));
		
		JFreeChart chart = createChart();
//		JFreeChart chart = getChart();
//		ChartPanel jfreeChartPanel = new ChartPanel(chart);
		
		ChartPanel jfreeChartPanel = null;
		int width  = _pipeCmd.getWidthAsInt();
		int height = _pipeCmd.getHeightAsInt();
		
		if (GraphType.GANTT.equals(_pipeCmd.getGraphType()) && width == -1)
		{
			width =  ChartPanel.DEFAULT_WIDTH * 3;
			_pipeCmd.addInfoMessage("--width was not set, setting this to value: " + width);
		}

		if (GraphType.GANTT.equals(_pipeCmd.getGraphType()) && height == -1)
		{
			height = 17 * _tm.getRowCount(); // 16 pixels per row + 1 extra for space
			_pipeCmd.addInfoMessage("--height was not set, setting this to value: " + height + ", based on that we have " + _tm.getRowCount() + " tasks.");
		}

		if (width != -1 || height != -1)
		{
			if (width  == -1) width  = ChartPanel.DEFAULT_WIDTH;
			if (height == -1) height = ChartPanel.DEFAULT_HEIGHT;

			width  = Math.max(width,  ChartPanel.DEFAULT_WIDTH);
			height = Math.max(height, ChartPanel.DEFAULT_HEIGHT);

			jfreeChartPanel = new ChartPanel(chart, width, height, width, height, width, height, true, true, true, true, true, true, true);
			chartPanel.add(new JScrollPane(jfreeChartPanel), "grow, push");
		}
		else
		{
			jfreeChartPanel = new ChartPanel(chart);
			chartPanel.add(jfreeChartPanel, "grow, push");
		}
		

//		chartPanel.add(infoPanel,       "growx, pushx");
//		chartPanel.add(jfreeChartPanel, "grow, push");

		//frame.setIconImages(icons);

		// Add informational messages (created in createChart()) to the Information Panel 
		for (Message pmsg : _pipeCmd.getMessages())
		{
			info_txt.append(pmsg.toString());
			info_txt.append("\n");
		}
		_pipeCmd.clearMessages();
		
		
		// If we have messages, the make it a split pane
		if (info_txt.getText().length() > 0)
		{
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, infoPanel, chartPanel);
			frame.setContentPane(splitPane);
		}
		else
		{
			frame.setContentPane(chartPanel);
		}
		
		frame.pack();
		
		SwingUtils.setSizeWithingScreenLimit(frame, 50);
		frame.setVisible(true);
		return frame;
	}

	/**
	 * Creates any graph/chart object and put error messages in the Message List
	 * 
	 * @return
	 */
	public JFreeChart createChart()
	{
		GraphType graphType = _pipeCmd.getGraphType();
		
		// Decide what graph type the user wants
		if (GraphType.AUTO.equals(graphType))
		{
			graphType = guessGraphType();
			
			_pipeCmd.addInfoMessage("Guessed Graph/Chart Type '"+graphType+"'.");
		}

		//-----------------------------------------
		// AREA or STACKED AREA
		//-----------------------------------------
		if (GraphType.AREA.equals(graphType) || GraphType.SAREA.equals(graphType) || GraphType.STACKEDAREA.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Category' dataset for graph type '"+graphType+"'.");
			
			CategoryDataset dataset = createCategoryDataset(graphType);
			
			if (_pipeCmd.is3dEnabled())
				_pipeCmd.addInfoMessage("The '3D' property is not possible for 'area', 'sarea' or 'stackedarea', skipping this option... ");

//			if ( GraphType.SAREA.equals(graphType) || GraphType.STACKEDAREA.equals(graphType) )
//				_chart = ChartFactory.createStackedAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
//			else
//				_chart = ChartFactory.createAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
			if ( GraphType.SAREA.equals(graphType) || GraphType.STACKEDAREA.equals(graphType) )
				_chart = createStackedAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
			else
				_chart = createAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
		}
		//-----------------------------------------
		// BAR or STACKED BAR
		//-----------------------------------------
		else if (GraphType.BAR.equals(graphType) || GraphType.SBAR.equals(graphType) || GraphType.STACKEDBAR.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Category' dataset for graph type '"+graphType+"'.");

			CategoryDataset dataset = createCategoryDataset(graphType);
			
//			if (_pipeCmd.is3dEnabled())
//				if ( GraphType.SBAR.equals(graphType) || GraphType.STACKEDBAR.equals(graphType) )
//					_chart = ChartFactory.createStackedBarChart3D(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
//				else
//				{
//					_chart = ChartFactory.createBarChart3D(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
//					BarRenderer3D renderer = (BarRenderer3D) _chart.getCategoryPlot().getRenderer();
//					double margin = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_BAR_ITEM_MARGIN, DEFAULT_BAR_ITEM_MARGIN);
//					renderer.setItemMargin(margin);
//				}
//			else
//			{
				if ( GraphType.SBAR.equals(graphType) || GraphType.STACKEDBAR.equals(graphType) )
					_chart = createStackedBarChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
				else
				{
					_chart = createBarChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
					BarRenderer renderer = (BarRenderer) _chart.getCategoryPlot().getRenderer();
					double margin = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_BAR_ITEM_MARGIN, DEFAULT_BAR_ITEM_MARGIN);
					renderer.setItemMargin(margin);
				}
//			}

//			if (_pipeCmd.isShowDataValues())
//			{
//				CategoryItemRenderer renderer = _chart.getCategoryPlot().getRenderer();
//				CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("#.#"));
//				renderer.setDefaultItemLabelGenerator(generator);
//				
//				renderer.setDefaultItemLabelsVisible(true);
//			}
		}
		//-----------------------------------------
		// LINE
		//-----------------------------------------
		else if (GraphType.LINE.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Category' dataset for graph type '"+graphType+"'.");

			CategoryDataset dataset = createCategoryDataset(graphType);
			
//			if (_pipeCmd.is3dEnabled())
//				_chart = ChartFactory.createLineChart3D(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
//			else
//			{
				_chart = ChartFactory.createLineChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 
//			}
		}
		//-----------------------------------------
		// PIE
		//-----------------------------------------
		else if (GraphType.PIE.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Pie' dataset for graph type '"+graphType+"'.");

			Map<String, DefaultPieDataset> pieMap = createPieDataset(graphType);			
			
			if (pieMap.size() == 1)
			{
				DefaultPieDataset dataset = pieMap.get("DEFAULT");
				
    			if (_pipeCmd.is3dEnabled())
    				_chart = ChartFactory.createPieChart3D(_pipeCmd.getGraphTitle(), dataset);
    			else
    				_chart = ChartFactory.createPieChart(_pipeCmd.getGraphTitle(), dataset);
			}
			else
			{
				// Convert the Multiple Pie Charts to a CategoryDataset, which the createMultiplePieChart* methods are using
				DefaultCategoryDataset dataset = new DefaultCategoryDataset();
				for (Entry<String, DefaultPieDataset> entry : pieMap.entrySet())
				{
					String pieChartName        = entry.getKey();
					DefaultPieDataset pieChart = entry.getValue();
					
					for (int i=0; i<pieChart.getItemCount(); i++)
					{
						Number        value  = pieChart.getValue(i);
						Comparable<?> pieKey = pieChart.getKey(i);
						
						dataset.setValue(value, pieKey, pieChartName);
					}
				}
				
    			if (_pipeCmd.is3dEnabled())
    				_chart = ChartFactory.createMultiplePieChart3D(_pipeCmd.getGraphTitle(), dataset, TableOrder.BY_COLUMN, true, true, false);
    			else
    				_chart = ChartFactory.createMultiplePieChart(_pipeCmd.getGraphTitle(), dataset, TableOrder.BY_COLUMN, true, true, false);
			}
		}
		//-----------------------------------------
		// TIMESERIES
		//-----------------------------------------
		else if (GraphType.TS.equals(graphType) || GraphType.TIMESERIES.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Time Series' dataset for graph type '"+graphType+"'.");

			XYDataset dataset = createTimeSeriesDataset(graphType);

			if (_pipeCmd.is3dEnabled())
				_pipeCmd.addInfoMessage("The '3D' property is not possible for 'TimeSeries', skipping this option... ");

			if (_pipeCmd.isPivotEnabled())
				_pipeCmd.addInfoMessage("The 'pivot' property is not possible for 'TimeSeries', skipping this option... ");

			_chart = ChartFactory.createTimeSeriesChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset);
		}
		//-----------------------------------------
		// GANTT
		//-----------------------------------------
		else if (GraphType.GANTT.equals(graphType))
		{
			_pipeCmd.addInfoMessage("creating 'Gantt' dataset for graph type '"+graphType+"'.");

			IntervalCategoryDataset dataset = createGanttDataset(graphType);

			if (_pipeCmd.is3dEnabled())
				_pipeCmd.addInfoMessage("The '3D' property is not possible for 'Gantt', skipping this option... ");

			if (_pipeCmd.isPivotEnabled())
				_pipeCmd.addInfoMessage("The 'pivot' property is not possible for 'Gantt', skipping this option... ");

			_chart = ChartFactory.createGanttChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset);
		}


		//
		// Show actual DATA values in charts/graphs
		//
		if (_pipeCmd.isShowDataValues())
		{
			Plot plot = _chart.getPlot();
			if (plot instanceof CategoryPlot)
			{
				CategoryPlot cplot = (CategoryPlot) plot;

				if (GraphType.GANTT.equals(graphType))
				{
//					CategoryItemRenderer renderer = cplot.getRenderer();
					GanttRenderer renderer = (GanttRenderer) cplot.getRenderer();
					
					CategoryToolTipGenerator toolTipGenerator = new CategoryToolTipGenerator()
					{
						@Override
						public String generateToolTip(CategoryDataset dataset, int row, int column)
						{
							String  startMsg         = "Row=" + row + ", column=" + column + "<hr>";
							String  endMsg           = "";
							boolean borders          = false;
							boolean stripedRows      = true;
							boolean addOuterHtmlTags = true;

						//	return _tm.toHtmlTableString(row, startMsg, endMsg, borders, stripedRows, addOuterHtmlTags);
							return _tm.toHtmlTableString(column, startMsg, endMsg, borders, stripedRows, addOuterHtmlTags);
						}
					};
//					renderer.setLegendItemToolTipGenerator();
//					renderer.setSeriesToolTipGenerator(0, toolTipGenerator);
					renderer.setDefaultToolTipGenerator(toolTipGenerator);
					
					CategoryItemLabelGenerator generator = new CategoryItemLabelGenerator()
					{
						@Override
						public String generateRowLabel(CategoryDataset dataset, int row)
						{
//							return "Your Row Text  " + row;
					        return dataset.getRowKey(row).toString();
						}
						
						@Override
						public String generateColumnLabel(CategoryDataset dataset, int column)
						{
//							return "Your Column Text  " + column;
					        return dataset.getColumnKey(column).toString();
						}
						
						@Override
						public String generateLabel(CategoryDataset dataset, int row, int column)
						{
//							Object val = dataset.getValue(row, column);
//							Object ckey = dataset.getColumnKey(column);
//							Object rkey = dataset.getRowKey(row);
//							
//							return "Your Label Text:  row=" + row + ", column=" + column + ". ckey=" + ckey + ", rkey=" + rkey;
							
//							int tableModelRow = row;
							int tableModelRow = column;
							int comment_pos = _tm.findColumnNoCase("comment");
							if (comment_pos != -1)
							{
								return _tm.getValueAsString(tableModelRow, comment_pos);
							}
							return "";
						}
						
					};
					renderer.setDefaultItemLabelGenerator(generator);

					renderer.setDefaultPositiveItemLabelPosition (new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT));
					renderer.setPositiveItemLabelPositionFallback(new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_RIGHT));
					
					renderer.setDefaultItemLabelsVisible(true);
				}
				else
				{
					CategoryItemRenderer renderer = cplot.getRenderer();
//					CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("#.#"));
					CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator("{2}", NumberFormat.getNumberInstance());
					renderer.setDefaultItemLabelGenerator(generator);
					
					renderer.setDefaultItemLabelsVisible(true);
				}
			}
			else if (plot instanceof PiePlot)
			{
				PiePlot piePlot = (PiePlot) plot;
				
//				piePlot.setSimpleLabels(true);
//				PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator("{0}: {1} ({2})", new DecimalFormat("#.#"), new DecimalFormat("0%"));
//				//PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator("{1} ({2})", new DecimalFormat("#.#"), new DecimalFormat("0%"));
//				piePlot.setLabelGenerator(gen);
				
				piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})", NumberFormat.getNumberInstance(), NumberFormat.getPercentInstance()));
				piePlot.setLabelGap(0.01D); // how close to the edges can the label be printed ??? 0.01 = 1%
				piePlot.setMaximumLabelWidth(0.20D); // 30% of plot width
			}
			else if (plot instanceof XYPlot)
			{
				XYPlot xyPlot = (XYPlot) plot;
				
				XYItemRenderer renderer = xyPlot.getRenderer();
				XYItemLabelGenerator generator = new StandardXYItemLabelGenerator("{2}");
				renderer.setDefaultItemLabelGenerator(generator);

				renderer.setDefaultItemLabelsVisible(true);

				if (renderer instanceof XYLineAndShapeRenderer)
				{
					XYLineAndShapeRenderer lsr = (XYLineAndShapeRenderer) renderer;
					lsr.setDefaultShapesVisible(true);
					lsr.setUseFillPaint(true);
				}
			}
			else
			{
				_pipeCmd.addWarningMessage("Not possible to use --showDataValues for "+graphType+". (plot instance '"+plot.getClass().getSimpleName()+"' is not handled)");
				_logger.warn("The 'Plot' is NOT in instance of 'CategoryPlot/PiePlot' for graphType='"+graphType+"'. Can't set 'showDataValues'. Plot instance is '"+plot.getClass().getName()+"'.");
			}
		}

		//
		// Show boxes/shapes at each data point in charts/graphs
		//
		if (_pipeCmd.isShowShapes())
		{
			boolean successShowShapes = false;
			
			Plot plot = _chart.getPlot();
			if (plot instanceof CategoryPlot)
			{
				CategoryPlot cplot = (CategoryPlot) plot;

				CategoryItemRenderer renderer = cplot.getRenderer();
				if (renderer instanceof XYLineAndShapeRenderer)
				{
					XYLineAndShapeRenderer lsr = (XYLineAndShapeRenderer) renderer;
					lsr.setDefaultShapesVisible(true);
					lsr.setUseFillPaint(true);

					successShowShapes = true;
				}
			}
			else if (plot instanceof XYPlot)
			{
				XYPlot xyPlot = (XYPlot) plot;
				XYItemRenderer renderer = xyPlot.getRenderer();

				if (renderer instanceof XYLineAndShapeRenderer)
				{
					XYLineAndShapeRenderer lsr = (XYLineAndShapeRenderer) renderer;
					lsr.setDefaultShapesVisible(true);
					lsr.setUseFillPaint(true);

					successShowShapes = true;
				}
			}

			if ( ! successShowShapes )
			{
				_pipeCmd.addWarningMessage("Not possible to use --showShapes for "+graphType+". (plot instance '"+plot.getClass().getSimpleName()+"' is not handled)");
			}
		}

		//
		// Rotate category labels
		//
		if (_pipeCmd.getRotateCategoryLabels() > 0)
		{
			int degree = _pipeCmd.getRotateCategoryLabels();

			CategoryAxis axis = _chart.getCategoryPlot().getDomainAxis();
			
			if      (degree == 1) axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
			else if (degree == 2) axis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
			else if (degree == 3) axis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
			else if (degree == 4) axis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			else                  axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		}

		if (_chart != null)
			return _chart;

		throw new RuntimeException("Unknown GRAPH TYPE (or possibly NOT-YET-IMPLEMETED): " + graphType);
	}


	/** replacement for: ChartFactory.createStackedAreaChart(...) */
	private JFreeChart createStackedAreaChart(String graphTitle, String labelCategory, String labelValue, CategoryDataset dataset)
	{
//		return ChartFactory.createStackedAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset); 

		boolean legend   = true;
		boolean tooltips = true;
		boolean urls     = false;
		
		PlotOrientation orientation = PlotOrientation.VERTICAL;

		Args.nullNotPermitted(orientation, "orientation");
//		CategoryAxis categoryAxis = new CategoryAxis(labelCategory);
		CategoryAxisSparselyLabeled categoryAxis = new CategoryAxisSparselyLabeled(labelCategory);
		categoryAxis.setCategoryMargin(0.0);

		ValueAxis valueAxis = new NumberAxis(labelValue);

		if (StringUtil.hasValue(_pipeCmd.getKeySdf()))
			categoryAxis.setDateFormat(new SimpleDateFormat(_pipeCmd.getKeySdf()));

		StackedAreaRenderer renderer = new StackedAreaRenderer();
		if (tooltips)
			renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

		if (urls)
			renderer.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());

//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, renderer);
		plot.setOrientation(orientation);

		JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
//		currentTheme.apply(chart);
		return chart;
	}


	/** replacement for: ChartFactory.createAreaChart(...) */
	private JFreeChart createAreaChart(String graphTitle, String labelCategory, String labelValue, CategoryDataset dataset)
	{
//		return ChartFactory.createAreaChart(_pipeCmd.getGraphTitle(), _pipeCmd.getLabelCategory(), _pipeCmd.getLabelValue(), dataset);

		boolean legend   = true;
		boolean tooltips = true;
		boolean urls     = false;
		
		PlotOrientation orientation = PlotOrientation.VERTICAL;

		Args.nullNotPermitted(orientation, "orientation");
//		CategoryAxis categoryAxis = new CategoryAxis(labelCategory);
		CategoryAxisSparselyLabeled categoryAxis = new CategoryAxisSparselyLabeled(labelCategory);
		categoryAxis.setCategoryMargin(0.0);

		if (StringUtil.hasValue(_pipeCmd.getKeySdf()))
			categoryAxis.setDateFormat(new SimpleDateFormat(_pipeCmd.getKeySdf()));

		ValueAxis valueAxis = new NumberAxis(labelValue);

		AreaRenderer renderer = new AreaRenderer();
		if (tooltips)
			renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

		if (urls)
			renderer.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());

//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, renderer);
		plot.setOrientation(orientation);

		JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
//		currentTheme.apply(chart);
		return chart;
	}


	/** replacement for: ChartFactory.createStackedBarChart(...) */
	private JFreeChart createStackedBarChart(String graphTitle, String labelCategory, String labelValue, CategoryDataset dataset)
	{
//		return ChartFactory.createStackedBarChart(graphTitle, labelCategory, labelValue, dataset);
		boolean legend   = true;
		boolean tooltips = true;
		boolean urls     = false;
		
		PlotOrientation orientation = PlotOrientation.VERTICAL;

		Args.nullNotPermitted(orientation, "orientation");

//		CategoryAxis categoryAxis = new CategoryAxis(labelCategory);
		CategoryAxisSparselyLabeled categoryAxis = new CategoryAxisSparselyLabeled(labelCategory);
		ValueAxis valueAxis = new NumberAxis(labelValue);

		if (StringUtil.hasValue(_pipeCmd.getKeySdf()))
			categoryAxis.setDateFormat(new SimpleDateFormat(_pipeCmd.getKeySdf()));

		StackedBarRenderer renderer = new StackedBarRenderer();
		if (tooltips)
			renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

		if (urls) 
			renderer.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());

//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, renderer);
		plot.setOrientation(orientation);
		
		// if to many rows disable shadow and "fancy" looks
		if (dataset.getColumnCount() > 20)
		{
			renderer.setShadowVisible(false);
			renderer.setBarPainter(new StandardBarPainter());
		}
		plot.setDomainGridlinesVisible(true);
		
		JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
//		currentTheme.apply(chart);
		return chart;
	}

	/** replacement for: ChartFactory.createBarChart(...) */
	private JFreeChart createBarChart(String graphTitle, String labelCategory, String labelValue, CategoryDataset dataset)
	{
//		return ChartFactory.createBarChart(graphTitle, labelCategory, labelValue, dataset);

		boolean legend   = true;
		boolean tooltips = true;
		boolean urls     = false;
		
		PlotOrientation orientation = PlotOrientation.VERTICAL;

		Args.nullNotPermitted(orientation, "orientation");

//		CategoryAxis categoryAxis = new CategoryAxis(labelCategory);
		CategoryAxisSparselyLabeled categoryAxis = new CategoryAxisSparselyLabeled(labelCategory);
		ValueAxis valueAxis = new NumberAxis(labelValue);

		if (StringUtil.hasValue(_pipeCmd.getKeySdf()))
			categoryAxis.setDateFormat(new SimpleDateFormat(_pipeCmd.getKeySdf()));

		BarRenderer renderer = new BarRenderer();
		if (orientation == PlotOrientation.HORIZONTAL) 
		{
			ItemLabelPosition position1 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT);
			renderer.setDefaultPositiveItemLabelPosition(position1);
			ItemLabelPosition position2 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE9, TextAnchor.CENTER_RIGHT);
			renderer.setDefaultNegativeItemLabelPosition(position2);
		} 
		else if (orientation == PlotOrientation.VERTICAL) 
		{
			ItemLabelPosition position1 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
			renderer.setDefaultPositiveItemLabelPosition(position1);
			ItemLabelPosition position2 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
			renderer.setDefaultNegativeItemLabelPosition(position2);
		}
		if (tooltips)
			renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

		if (urls)
			renderer.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());

//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, renderer);
		plot.setOrientation(orientation);

		// if to many rows disable shadow and "fancy" looks
		if (dataset.getColumnCount() > 20)
		{
			renderer.setShadowVisible(false);
			renderer.setBarPainter(new StandardBarPainter());
		}
		plot.setDomainGridlinesVisible(true);

		JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
//		currentTheme.apply(chart);
		return chart;
	}

	/**
	 * Try to guess the desired Graph Type 
	 * 
	 * @return
	 */
	public GraphType guessGraphType()
	{
		int colCount = _tm.getColumnCount();
		int rowCount = _tm.getRowCount();

		GraphType graphType;

		//------------------------
		// TIMESERIES
		if (isTimeColumn(0))
		{
			graphType = GraphType.TIMESERIES;

			_pipeCmd.addInfoMessage("guessGraphType(): set graphType to '"+graphType+"'. Due to: FIRST Column is a TimeStamp");
			return graphType;
		}

		//------------------------
		// PIE
		if (colCount == 2 && isStringColumn(0) )
		{
			graphType = GraphType.PIE;

			_pipeCmd.addInfoMessage("guessGraphType(): set graphType to '"+graphType+"'. Due to: FIRST Column is a 'String' and there is only two columns");
			return graphType;
		}

		//------------------------
		// PIE
		if (rowCount == 1)
		{
			graphType = GraphType.PIE;
			_pipeCmd.setPivot(true);

			_pipeCmd.addInfoMessage("guessGraphType(): set graphType to '"+graphType+"'. ENABLE-PIVOT mode and USING ALL COLUMNS...  Due to: ROWCOUNT=1");
			return graphType;
		}

		//------------------------
		// DEFAULT RULE
		graphType = GraphType.BAR;
		
		_pipeCmd.addInfoMessage("guessGraphType(): set graphType to '"+graphType+"'. Due to: -end-of-guesses-");
		return graphType;
	}

	public ResultSetTableModel getResultSetTableModel()
	{
		return _tm;
	}

	public int getRowCount()
	{
		return _tm.getRowCount();
	}
	
	public String getMigLayoutConstrains()
	{
		return _pipeCmd.getMigLayoutConstrains();
	}


	/**
	 * Print some info...
	 * 
	 * @param msg
	 * @param colIdList
	 */
	private void printColumnListInfo(String msg, List<Integer> colIdList)
	{
		String colStr = "";
		for (Integer col : colIdList)
		{
			String colName = _tm.getColumnName(col);
			colStr += "[" + col + "]='" + colName + "', ";
		}
		if (colStr.length() > 2)
			colStr = colStr.substring(0, colStr.length()-2);

		_pipeCmd.addDebugMessage(msg + colStr);
	}

	/**
	 * Creates a List<Integer> which represents the column positions
	 * 
	 * @param type      KEY_COLS or VALUE_COLS
	 * @param list      a list of either columnPositions or ColumnNames that will be translated into column positions
	 * @param skipPosList 
	 * 
	 * @return List<Integer> which represents the column positions
	 */
	private List<Integer> createColumnPosList(CreateColumnType type, List<String> list, List<Integer> skipPosList)
	{
		List<Integer> resList = new ArrayList<>();
		
		// Defaults for KEY: Column 0 (if not specified)
		// Defaults for VAL: all columns except 0
		if (list == null || (list != null && list.isEmpty()) )
		{
			if (CreateColumnType.KEY_COLS.equals(type))
			{
				resList.add(0);
			}
			else if (CreateColumnType.VALUE_COLS.equals(type))
			{
				for (int col = 0; col < _tm.getColumnCount(); col++)
					resList.add(col);
			}
		}
		else
		{
			// if we have list, the list can contain
			// - a number position (starting at 0)
			// - a column name, which is translated into a col pos
			for (String colName : list)
			{
				int colPos = StringUtil.parseInt(colName, -1);
				if (colPos < 0)
					colPos = _tm.findColumn(colName);

				if (colPos < 0)
					throw new RuntimeException("The column '"+colName+"' is NOT part of the ResultSet. Can NOT continue...");

				if (colPos >= _tm.getColumnCount())
					throw new RuntimeException("The specified column '"+colName+"' at colPos "+colPos+" is ABOVE ResultSet column count of "+_tm.getColumnCount()+". Can NOT continue...");
				
				resList.add(colPos);
			}
		}

		// Remove all Column Positions in the SKIP LIST (probably KEY Columns when assigning VALUE Columns)
		if (skipPosList != null)
		{
			resList.removeAll(skipPosList);
		}
		
		// For VALUES: Check data types (also removes columns we can't handle)
		if (CreateColumnType.VALUE_COLS.equals(type))
		{
			resList = checkValueColumnDataType(resList);
		}
		
		if (_pipeCmd.isDebugEnabled())
		{
			if (CreateColumnType.KEY_COLS     .equals(type)) printColumnListInfo("Key Column(s): ",   resList);
			if (CreateColumnType.GROUP_COLS   .equals(type)) printColumnListInfo("Group Column(s): ", resList);
			if (CreateColumnType.VALUE_COLS   .equals(type)) printColumnListInfo("Value Column(s): ", resList);
			if (CreateColumnType.TS_VALUE_COLS.equals(type)) printColumnListInfo("Value Column(s): ", resList);
		}

		return resList;
	}

	/**
	 * Check and discards any columns that could NOT be handled
	 * @param list
	 * @return
	 */
	private List<Integer> checkValueColumnDataType(List<Integer> list)
	{
		List<Integer> keepList = new ArrayList<>();
		for (Integer col : list)
		{
			if (isNumberColumn(col) || (isStringColumn(col) && _pipeCmd.isStr2NumEnabled()) )
			{
				keepList.add(col);
			}
			else
			{
				// not a value, can't use it (defaults to null)
				String colName     = _tm.getColumnName(col);
				int jdbcColumnType = _tm.getSqlType(col); // starts at 0

				_pipeCmd.addInfoMessage("INFO: Skipping [UNSUPPORTED-COLUMN-VALUE-DATA-TYPE]: col="+col+", colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
			}
		}
		return keepList;
	}

	/**
	 * Used to compose a (combined) KEY value which can be used be the createXxxDataset() methods 
	 * @param row
	 * @param keyList
	 * @return
	 */
//	private String getRowKey(int row, List<Integer> keyList)
//	{
//		if (keyList.size() == 1)
//			return _tm.getValueAt(row, keyList.get(0)) + "";
//
//		// Add all KEY Columns
//		StringBuilder sb = new StringBuilder();
//
//		for (Integer col : keyList)
//			sb.append( _tm.getValueAt(row, keyList.get(col)) ).append("|");
//
//		// Remove last '|'
//		sb.delete(sb.length()-1, sb.length());
//		
//		return sb.toString();
//	}
	private Comparable getRowKey(int row, List<Integer> keyList)
	{
		if (keyList.size() == 1)
		{
			Object obj = _tm.getValueAt(row, keyList.get(0));
			if (obj instanceof Comparable)
				return (Comparable) _tm.getValueAt(row, keyList.get(0));
			else
				throw new RuntimeException("getRowKey(row="+row+", keyList.size()==1): The fetched object '"+obj.getClass().getName()+"' is not a instance of Comparable.");
		}

		// Add all KEY Columns
		StringBuilder sb = new StringBuilder();

		for (Integer col : keyList)
			sb.append( _tm.getValueAt(row, keyList.get(col)) ).append("|");

		// Remove last '|'
		sb.delete(sb.length()-1, sb.length());
		
		return sb.toString();
	}
	

	//#########################################################################
	//#########################################################################
	// DATA SET for AREA / BAR
	//#########################################################################
	//#########################################################################
	/**
	 * 
	 * @param graphType 
	 * @return
	 */
	public CategoryDataset createCategoryDataset(GraphType graphType)
	{
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		boolean pivot = _pipeCmd.isPivotEnabled();
		int rowCount  = _tm.getRowCount();
		int colCount  = _tm.getColumnCount();

		if (_pipeCmd.isDebugEnabled())
		{
			_pipeCmd.addDebugMessage("createCategoryDataset()");
			_pipeCmd.addDebugMessage("pivot="+pivot);
			_pipeCmd.addDebugMessage("rowCount="+rowCount);
			_pipeCmd.addDebugMessage("colCount="+colCount);
		}
		
		// Get/Compose the KEY and Values Column List
		List<Integer> keyPosList = createColumnPosList(CreateColumnType.KEY_COLS,   _pipeCmd.getKeyCols(), null);
		List<Integer> grpPosList = createColumnPosList(CreateColumnType.GROUP_COLS, _pipeCmd.getGroupCols(), null);
		List<Integer> valPosList = createColumnPosList(CreateColumnType.VALUE_COLS, _pipeCmd.getValCols(), keyPosList);

		if (valPosList.size() > 1)
		{
			if (GraphType.AREA.equals(graphType) || GraphType.BAR.equals(graphType))
				_pipeCmd.addInfoMessage("Many value column was found: You can also choose --type sbar or --type sarea for 'stacking' values.");
		}

		// Loop ROWS
		for (int row = 0; row < rowCount; row++)
		{
			// first column contains the row key...
//			String rowKey = getRowKey(row, keyPosList);
			Comparable rowKey = getRowKey(row, keyPosList);

			for (Integer col : valPosList)
			{
				String columnKey = _tm.getColumnName(col);
				if (grpPosList != null && !grpPosList.isEmpty())
				{
					columnKey  = "";
					String sep = "";

					for (Integer pos : grpPosList)
					{
						columnKey += sep + _tm.getValueAsString(row, pos);

						// Set separator to use between group names
						sep = ", ";
					}
				}

				try
				{
					// -----------------
					// ---- NUMBER -----
					// -----------------
					if (isNumberColumn(col))
					{
						Number value = (Number) _tm.getValueAsObject(row, col);

						setOrIncrementValue(dataset, row, col, pivot, value, columnKey, rowKey);
					}
					// --------------------
					// ---- TIMESTAMP -----
					// --------------------
//					else if (isTimeColumn(col))
//					{
//						Timestamp ts = (Timestamp) _tm.getValueAsObject(row, col);
//						Number value = Long.valueOf(ts.getTime());
//
//						setOrIncrementValue(dataset, row, col, pivot, value, columnKey, rowKey);
//					}
					// -------------------------------------
					// ---- STRING (convert -> double) -----
					// -------------------------------------
					else if (isStringColumn(col))
					{
						Double value = convertStringToDouble(row, col, _pipeCmd.getRemoveRegEx());

						setOrIncrementValue(dataset, row, col, pivot, value, columnKey, rowKey);
					}
				}
				catch (Exception ex)
				{
					String colName = _tm.getColumnName(col);
					String colValStr = _tm.getValueAsObject(row, col) + "";
					int jdbcColumnType = _tm.getSqlType(col);

					String msg = "Problems reading: row="+row+", col="+col+", colName='"+colName+"', colValStr='"+colValStr+"', rowKey='"+rowKey+"', columnKey='"+columnKey+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
					
					_pipeCmd.addWarningMessage(msg + " Caught: "+ex);
					_logger.error(msg, ex);
				}
			}
		}

		return dataset;
	}
//	private void setOrIncrementValue(DefaultCategoryDataset dataset, int row, int col, boolean pivot, Number value, String rowKey, String colKey)
	private void setOrIncrementValue(DefaultCategoryDataset dataset, int row, int col, boolean pivot, Number value, Comparable rowKey, Comparable colKey)
	{
		if (value == null)
			return;

//		String rKey = rowKey;
//		String cKey = colKey;
		Comparable rKey = rowKey;
		Comparable cKey = colKey;
		if (pivot)
		{
			rKey = colKey;
			cKey = rowKey;
		}

		if (_pipeCmd.isGroupByKeySdf() && cKey instanceof Date && StringUtil.hasValue(_pipeCmd.getKeySdf()))
		{
			SimpleDateFormat sdf = new SimpleDateFormat(_pipeCmd.getKeySdf());
			cKey = sdf.format((Date) cKey);
		}

		try
		{
			Number curValue = dataset.getValue(rKey, cKey);
			if (curValue == null)
			{
				dataset.setValue(value, rKey, cKey);

				if (_pipeCmd.isDebugEnabled())
					_pipeCmd.addDebugMessage("setOrIncrementValue(): ->SET-VALUE-2->: [row="+row+", col="+col+", srcColName='"+_tm.getColumnName(col)+"']:  rKey='"+rKey+"', cKey='"+cKey+"', value="+value);
			}
			else
			{
				Number newValue = curValue.doubleValue() + value.doubleValue();
				dataset.setValue(newValue, rKey, cKey);

				if (_pipeCmd.isDebugEnabled())
					_pipeCmd.addDebugMessage("setOrIncrementValue(): INCREMENT-VALUE: [row="+row+", col="+col+", srcColName='"+_tm.getColumnName(col)+"']: rKey='"+rKey+"', cKey='"+cKey+"', curValue="+curValue+", addValue="+value+", setNewValue="+newValue);
			}
		}
		catch (UnknownKeyException ex)
		{
			dataset.setValue(value, rKey, cKey);
			
			if (_pipeCmd.isDebugEnabled())
				_pipeCmd.addDebugMessage("setOrIncrementValue(): ->SET-VALUE-1->: [row="+row+", col="+col+", srcColName='"+_tm.getColumnName(col)+"']:  rKey='"+rKey+"', cKey='"+cKey+"', value="+value);
		}
	}
//	public CategoryDataset createCategoryDataset()
//	{
//		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
//
//		boolean transpose = true;
////		boolean transpose = _pipeCmd._transpose;
//
//		int rowCount = _tm.getRowCount();
//		int colCount = _tm.getColumnCount();
//
//		for (int col=1; col<colCount; col++)
//		{
//			if ( ! isNumberColumn(col) )
//			{
//				if ( isStringColumn(col) && _pipeCmd.isStr2NumEnabled() )
//					continue;
//
//				// not a value, can't use it (defaults to null)
//				String colName     = _tm.getColumnName(col);
//				int jdbcColumnType = _tm.getSqlType(col); // starts at 0
//
//				String msg = "INFO: Skipping [UNSUPPORTED-COLUMN-DATA-TYPE]: col="+col+", colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
//
//				_graphMessages.add(msg);
//			}
//		}
//
//		for (int r = 0; r < rowCount; r++)
//		{
//			// first column contains the row key...
//			Comparable<String> rowKey = _tm.getValueAt(r, 0) + "";
//
//			for (int col = 1; col < colCount; col++)
//			{
//				Comparable<String> columnKey = _tm.getColumnName(col);
//				int jdbcColumnType = _tm.getSqlType(col);
//
//				try
//				{
//					// -----------------
//					// ---- NUMBER -----
//					// -----------------
//					if (isNumberColumn(col))
//					{
//						Number value = (Number) _tm.getValueAsObject(r, col);
//						
//						if ( transpose )
//							dataset.setValue(value, columnKey, rowKey);
//						else
//							dataset.setValue(value, rowKey, columnKey);
//					}
//					// --------------------
//					// ---- TIMESTAMP -----
//					// --------------------
//					else if (isTimeColumn(col))
//					{
//						Timestamp ts = (Timestamp) _tm.getValueAsObject(r, col);
//						Number value = Long.valueOf(ts.getTime());
//
//						if ( transpose )
//							dataset.setValue(value, columnKey, rowKey);
//						else
//							dataset.setValue(value, rowKey, columnKey);
//					}
//					// -------------------------------------
//					// ---- STRING (convert -> double) -----
//					// -------------------------------------
//					else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
//					{
//						Double value = convertStringToDouble(r, col, _pipeCmd.getRemoveRegEx());
//
//						if ( transpose )
//							dataset.setValue(value, columnKey, rowKey);
//						else
//							dataset.setValue(value, rowKey, columnKey);
//					}
//				}
//				catch (Exception ex)
//				{
//					String colName = _tm.getColumnName(col);
//					String colValStr = _tm.getValueAsObject(r, col) + "";
//
//					String msg = "WARNING: Problems reading: row="+r+", col="+col+", colName='"+colName+"', colValStr='"+colValStr+"', rowKey='"+rowKey+"', columnKey='"+columnKey+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
//					
//					_graphMessages.add(msg);
//					_logger.error(msg, ex);
//				}
//			}
//		}
//
//		return dataset;
//	}


	//#########################################################################
	//#########################################################################
	// DATA SET for PIE
	//#########################################################################
	//#########################################################################
	public Map<String, DefaultPieDataset> createPieDataset(GraphType graphType)
	{
		Map<String, DefaultPieDataset> datasetMap = new LinkedHashMap<>();
		DefaultPieDataset dataset = new DefaultPieDataset();

		// ALL LOGIC Except (manyRows-ManyCols-noPivoy) will only return 1 PieDataset, so we can add this here..
		datasetMap.put("DEFAULT", dataset);
		
		boolean pivot = _pipeCmd.isPivotEnabled();
		int rowCount  = _tm.getRowCount();
		int colCount  = _tm.getColumnCount();

		if (_pipeCmd.isDebugEnabled())
		{
			_pipeCmd.addDebugMessage("createPieDataset()");
			_pipeCmd.addDebugMessage("pivot="+pivot);
			_pipeCmd.addDebugMessage("rowCount="+rowCount);
			_pipeCmd.addDebugMessage("colCount="+colCount);
		}
		
		// Get/Compose the KEY and Values Column List
		List<Integer> keyPosList = createColumnPosList(CreateColumnType.KEY_COLS,   _pipeCmd.getKeyCols(), null);
		List<Integer> valPosList = createColumnPosList(CreateColumnType.VALUE_COLS, _pipeCmd.getValCols(), keyPosList);

//		if (valPosList.size() > 1 && ! pivot) 
//		{
//			String msg = "ERROR: The ResultSet contains more than ONE value columns. WORKAROUND: Use -v colName to specify the desired column, Or possibly use -p to pivote the data.";
//
//			_graphMessages.add(msg);
//			throw new RuntimeException(msg);
//		}

		if ( rowCount == 1 && pivot )
		{
			String type = "ONE-ROW_and_PIVOT";
			_pipeCmd.addInfoMessage(""+type+" --- STRATEGY: Each column will be a piece of the pie.");

			// LOOP Columns
			for (Integer col : valPosList)
			{
				int row = 0;
				Comparable<String> rowKey = _tm.getColumnName(col);;

				// -----------------
				// ---- NUMBER -----
				// -----------------
				if (isNumberColumn(col))
				{
					Number value = (Number) _tm.getValueAsObject(row, col);
					
					dataset.setValue(rowKey, value);
				}
				// -------------------------------------
				// ---- STRING (convert -> double) -----
				// -------------------------------------
				else if (isStringColumn(col))
				{
					Double value = convertStringToDouble(row, col, _pipeCmd.getRemoveRegEx());

					dataset.setValue(rowKey, value);
				}
				else
				{
					String colName     = _tm.getColumnName(col);
					String colValStr   = _tm.getValueAsObject(row, col) + "";
					int jdbcColumnType = _tm.getSqlType(col);

					_pipeCmd.addErrorMessage(type+" unhandled row="+row+" column["+col+"]='"+colName+"', with colValStr='"+colValStr+"', datatype["+jdbcColumnType+"]='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
				}
			}
		}
		else if ( valPosList.size() == 1 )
		{
			String type = "MANY-ROW_and_ONE-VAL-COL";
			_pipeCmd.addInfoMessage(""+type+" --- STRATEGY: Duplicate keys will be summarized.");

			// keep count on duplicate values
			boolean doSumOnDuplicates = true;
			
			// Loop rows
			for (int row = 0; row < rowCount; row++)
			{
				// get column position for value 1 (index 0) 
				int col = valPosList.get(0);

//				String rowKey = getRowKey(row, keyPosList);
				Comparable rowKey = getRowKey(row, keyPosList);
				Number rowVal = null;

				// -----------------
				// ---- NUMBER -----
				// -----------------
				if (isNumberColumn(col))
				{
					rowVal = (Number) _tm.getValueAsObject(row, col);
				}
				// -------------------------------------
				// ---- STRING (convert -> double) -----
				// -------------------------------------
				else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
				{
					rowVal = convertStringToDouble(row, col, _pipeCmd.getRemoveRegEx());
				}
				else
				{
					String colName     = _tm.getColumnName(col);
					String colValStr   = _tm.getValueAsObject(row, col) + "";
					int jdbcColumnType = _tm.getSqlType(col);

					_pipeCmd.addErrorMessage(type+": unhandled row="+row+" column["+col+"]='"+colName+"', with colValStr='"+colValStr+"', datatype["+jdbcColumnType+"]='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
				}
				
				// apply with duplicate detection
				if (rowVal != null)
				{
					try
					{
						Number prevVal = dataset.getValue(rowKey);
						if (prevVal != null)
						{
							Number newVal = prevVal.doubleValue() + rowVal.doubleValue();
							
							if (_pipeCmd.isDebugEnabled())
							{
								String colName = _tm.getColumnName(col);
								_pipeCmd.addDebugMessage(type+": DUPLICATE-KEY-EXISTS_DO-SUM: row="+row+" column["+col+"]='"+colName+"', rowKey='"+rowKey+"', with existingVal="+prevVal+", rowVal="+rowVal+", NEW-SUM-VALUE="+newVal+".");
							}

							if (doSumOnDuplicates)
								dataset.setValue(rowKey, newVal);
						}
						else
						{
							dataset.setValue(rowKey, rowVal);
						}
					}
					catch (UnknownKeyException ex)
					{
						dataset.setValue(rowKey, rowVal);
					}
				}
			}
		}
		else // multiple rows AND multiple value columns
		{
			// if pivot
			//  - do SUM or AVG on all ROWS for each column...
			if (pivot)
			{
				String type = "MANY-ROW_and_MANY-COLS_and_PIVOT";
				_pipeCmd.addInfoMessage(""+type+" --- STRATEGY: Applying SUM for all records in each column.");
				
				// LOOP Columns
				for (Integer col : valPosList)
				{
					String colName = _tm.getColumnName(col);
					String rowKey  = colName;

					for (int row = 0; row < rowCount; row++)
					{
						Number rowVal = null;

						// -----------------
						// ---- NUMBER -----
						// -----------------
						if (isNumberColumn(col))
						{
							rowVal = (Number) _tm.getValueAsObject(row, col);
						}
						// -------------------------------------
						// ---- STRING (convert -> double) -----
						// -------------------------------------
						else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
						{
							rowVal = convertStringToDouble(row, col, _pipeCmd.getRemoveRegEx());
						}
						else
						{
							String colValStr   = _tm.getValueAsObject(row, col) + "";
							int jdbcColumnType = _tm.getSqlType(col);

							_pipeCmd.addErrorMessage(type+": unhandled row="+row+" column["+col+"]='"+colName+"', with colValStr='"+colValStr+"', datatype["+jdbcColumnType+"]='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
						}

						// apply with duplicate detection.. do SUM on duplicates
						if (rowVal != null)
						{
							try
							{
								Number prevVal = dataset.getValue(rowKey);
								if (prevVal != null)
								{
									Number newVal = prevVal.doubleValue() + rowVal.doubleValue();
									dataset.setValue(rowKey, newVal);
								}
								else
								{
									dataset.setValue(rowKey, rowVal);
								}
							}
							catch (UnknownKeyException ex)
							{
								dataset.setValue(rowKey, rowVal);
							}
						}
					}
				}
			} // end: pivot

			// if NOT pivot
			//  - Create ONE PIE Chart for each row
			else
			{
				String type = "MANY-ROW_and_MANY-COLS_and_NOPIVOT";
				_pipeCmd.addInfoMessage(""+type+" --- STRATEGY: Create ONE PIE Chart for each row");

				Map<String, DefaultPieDataset> pieMap = new LinkedHashMap<>();

				// Loop rows
				for (int row = 0; row < rowCount; row++)
				{
					// first column contains the row key...
					String rowKey = getRowKey(row, keyPosList) + "";
//					Comparable rowKey = getRowKey(row, keyPosList);
					Number value = null;

					DefaultPieDataset pieDataset = pieMap.get(rowKey);
					if (pieDataset == null)
					{
						pieDataset = new DefaultPieDataset();
						pieMap.put(rowKey, pieDataset);
					}
					else
					{
						_pipeCmd.addInfoMessage(type+"-KEY-EXISTS_DO-SUM: row="+row+", rowKey='"+rowKey+"', SUM will be done on all column values.");
					}
						
					// LOOP Columns
					for (Integer col : valPosList)
					{
						String columnKey = _tm.getColumnName(col);

						// -----------------
						// ---- NUMBER -----
						// -----------------
						if (isNumberColumn(col))
						{
							value = (Number) _tm.getValueAsObject(row, col);
						}
						// -------------------------------------
						// ---- STRING (convert -> double) -----
						// -------------------------------------
						else if (isStringColumn(col))
						{
							value = convertStringToDouble(row, col, _pipeCmd.getRemoveRegEx());
						}
						else
						{
							String colName     = _tm.getColumnName(col);
							String colValStr   = _tm.getValueAsObject(row, col) + "";
							int jdbcColumnType = _tm.getSqlType(col);

							_pipeCmd.addErrorMessage(type+": unhandled row="+row+" column["+col+"]='"+colName+"', with colValStr='"+colValStr+"', datatype["+jdbcColumnType+"]='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
						}

						// apply with duplicate detection.. do SUM on duplicates
						if (value != null)
						{
							try
							{
								Number prevVal = pieDataset.getValue(columnKey);
								if (prevVal != null)
								{
									Number newVal = prevVal.doubleValue() + value.doubleValue();
									pieDataset.setValue(columnKey, newVal);
								}
								else
								{
									pieDataset.setValue(columnKey, value);
								}
							}
							catch (UnknownKeyException ex)
							{
								pieDataset.setValue(columnKey, value);
							}
						}
					} // end: loop columns
				} // end: loop rows
				
				return pieMap;
			} // end: MultiRow & multiColumns
		}
		
		return datasetMap;
	}

//	public DefaultPieDataset createPieDataset()
//	{
//		DefaultPieDataset dataset = new DefaultPieDataset();
//		
//		int rowCount = _tm.getRowCount();
//		int colCount = _tm.getColumnCount();
//
//		if (colCount != 2) 
//		{
//			String msg = "ERROR: Invalid SQL, it has more than 2 columns.  PieDataSet requires 2 columns only";
//			_graphMessages.add(msg);
//			throw new RuntimeException(msg);
//		}
//
//		// CHECK COLUMN DATATYPE
//		for (int col=1; col<colCount; col++)
//		{
//			if ( ! isNumberColumn(col) )
//			{
//				if ( isStringColumn(col) && _pipeCmd.isStr2NumEnabled() )
//					continue;
//
//				// not a value, can't use it (defaults to null)
//				String colName     = _tm.getColumnName(col);
//				int jdbcColumnType = _tm.getSqlType(col); // starts at 0
//
//				String msg = "INFO: Skipping [UNSUPPORTED-COLUMN-DATA-TYPE]: col="+col+", colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
//
//				_graphMessages.add(msg);
//			}
//		}
//
//		for (int r = 0; r < rowCount; r++)
//		{
//			int col = 1;
//			Number value;
//
//			Comparable<String> key = _tm.getValueAt(r, 0) + "";
//
//			// -----------------
//			// ---- NUMBER -----
//			// -----------------
//			if (isNumberColumn(col))
//			{
//				value = (Number) _tm.getValueAsObject(r, col);
//
//				dataset.setValue(key, value);
//			}
//			// --------------------
//			// ---- TIMESTAMP -----
//			// --------------------
//			else if (isTimeColumn(col))
//			{
//				Timestamp ts = (Timestamp) _tm.getValueAsObject(r, col);
//				value = ts.getTime();
//				
//				dataset.setValue(key, value);
//			}
//			// -------------------------------------
//			// ---- STRING (convert -> double) -----
//			// -------------------------------------
//			else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
//			{
//				value = convertStringToDouble(r, col, _pipeCmd.getRemoveRegEx());
//				
//				dataset.setValue(key, value);
//			}
//		}
//		
//		return dataset;
//	}

	//#########################################################################
	//#########################################################################
	// DATA SET for TIMESERIES
	//#########################################################################
	//#########################################################################
	public XYDataset createTimeSeriesDataset(GraphType graphType)
	{
		int rowCount = _tm.getRowCount();
		int colCount = _tm.getColumnCount();

		if (_pipeCmd.isDebugEnabled())
		{
			_pipeCmd.addDebugMessage("createTimeSeriesDataset()");
//			_pipeCmd.addDebugMessage("pivot="+pivot);
			_pipeCmd.addDebugMessage("rowCount="+rowCount);
			_pipeCmd.addDebugMessage("colCount="+colCount);
		}

		// Get/Compose the KEY and Values Column List
		List<Integer> keyPosList = createColumnPosList(CreateColumnType.KEY_COLS,   _pipeCmd.getKeyCols(), null);
		List<Integer> valPosList = createColumnPosList(CreateColumnType.VALUE_COLS, _pipeCmd.getValCols(), keyPosList);

		int tsKeyCol = keyPosList.get(0);
		
		if ( ! isTimeColumn(tsKeyCol) )
		{
			String colName     = _tm.getColumnName(tsKeyCol);
			int jdbcColumnType = _tm.getSqlType(tsKeyCol);
			
			String msg = "ERROR: Invalid SQL, First column must be of a time/date/timestamp. colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
			_pipeCmd.addErrorMessage(msg);

			throw new RuntimeException(msg);
		}

		Map<String, TimeSeries> tsMap = new LinkedHashMap<>();
		//TimeSeries ts = new TimeSeries();

		for (int r = 0; r < rowCount; r++)
		{
//			for (int col=1; col<colCount; col++)
			for (int col : valPosList)
			{
				Timestamp timestamp = _tm.getValueAsTimestamp(r, tsKeyCol);
				FixedMillisecond timestampMs = new FixedMillisecond(timestamp);

				// -----------------
				// ---- NUMBER -----
				// -----------------
				if ( isNumberColumn(col) )
				{
					String colName     = _tm.getColumnName(col);

					TimeSeries ts = tsMap.get(colName);
					if (ts == null)
					{
						ts = new TimeSeries(colName);
						tsMap.put(colName, ts);
//System.out.println("Adding new TimeSeries for column '"+colName+"'.");
					}
					
					Number value = (Number) _tm.getValueAsObject(r, col);
					ts.add(timestampMs, value);
				}
				// -------------------------------------
				// ---- STRING (convert -> double) -----
				// -------------------------------------
				else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
				{
					String colName     = _tm.getColumnName(col);

					TimeSeries ts = tsMap.get(colName);
					if (ts == null)
					{
						ts = new TimeSeries(colName);
						tsMap.put(colName, ts);
//System.out.println("Adding new TimeSeries for column '"+colName+"'.");
					}

					Double value = convertStringToDouble(r, col, _pipeCmd.getRemoveRegEx());
					ts.add(timestampMs, value);
				}
			}
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		for (TimeSeries entry : tsMap.values())
		{
//System.out.println("Add TimeSeries to dataset.");
			dataset.addSeries(entry);
		}

//System.out.println("XXXXXXXXXXXXXXXXX messages.size()=" + (_graphMessages == null ? null : _graphMessages.size()) );

		return dataset;
	}
//	public XYDataset createTimeSeriesDataset()
//	{
//		int rowCount = _tm.getRowCount();
//		int colCount = _tm.getColumnCount();
//
//		if ( ! isTimeColumn(0) )
//		{
//			String colName     = _tm.getColumnName(0);
//			int jdbcColumnType = _tm.getSqlType(0); // starts at 0
//			
//			String msg = "ERROR: Invalid SQL, First column must be of a time/date/timestamp. colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
//			_graphMessages.add(msg);
//
//			throw new RuntimeException(msg);
//		}
//
//		// CHECK COLUMN DATATYPE
//		for (int col=1; col<colCount; col++)
//		{
//			if ( ! isNumberColumn(col) )
//			{
//				if ( isStringColumn(col) && _pipeCmd.isStr2NumEnabled() )
//					continue;
//
//				// not a value, can't use it (defaults to null)
//				String colName     = _tm.getColumnName(col);
//				int jdbcColumnType = _tm.getSqlType(col); // starts at 0
//
//				String msg = "INFO: Skipping [UNSUPPORTED-COLUMN-DATA-TYPE]: col="+col+", colName='"+colName+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.";
//
//				_graphMessages.add(msg);
//			}
//		}
//
//		Map<String, TimeSeries> tsMap = new LinkedHashMap<>();
//		//TimeSeries ts = new TimeSeries();
//
//		for (int r = 0; r < rowCount; r++)
//		{
//			for (int col=1; col<colCount; col++)
//			{
//				Timestamp timestamp = _tm.getValueAsTimestamp(r, 0);
//				FixedMillisecond timestampMs = new FixedMillisecond(timestamp);
//
//				// -----------------
//				// ---- NUMBER -----
//				// -----------------
//				if ( isNumberColumn(col) )
//				{
//					String colName     = _tm.getColumnName(col);
//					//int jdbcColumnType = _tm.getSqlType(col);
//
//					TimeSeries ts = tsMap.get(colName);
//					if (ts == null)
//					{
//						ts = new TimeSeries(colName);
//						tsMap.put(colName, ts);
////System.out.println("Adding new TimeSeries for column '"+colName+"'.");
//					}
//					
//					Number value = (Number) _tm.getValueAsObject(r, col);
//					ts.add(timestampMs, value);
//				}
//				// -------------------------------------
//				// ---- STRING (convert -> double) -----
//				// -------------------------------------
//				else if (isStringColumn(col) && _pipeCmd.isStr2NumEnabled())
//				{
//					String colName     = _tm.getColumnName(col);
//
//					TimeSeries ts = tsMap.get(colName);
//					if (ts == null)
//					{
//						ts = new TimeSeries(colName);
//						tsMap.put(colName, ts);
////System.out.println("Adding new TimeSeries for column '"+colName+"'.");
//					}
//
//					Double value = convertStringToDouble(r, col, _pipeCmd.getRemoveRegEx());
//					ts.add(timestampMs, value);
//				}
//			}
//		}
//
//		TimeSeriesCollection dataset = new TimeSeriesCollection();
//		for (TimeSeries entry : tsMap.values())
//		{
////System.out.println("Add TimeSeries to dataset.");
//			dataset.addSeries(entry);
//		}
//
////System.out.println("XXXXXXXXXXXXXXXXX messages.size()=" + (_graphMessages == null ? null : _graphMessages.size()) );
//
//		return dataset;
//	}

	
	//#########################################################################
	//#########################################################################
	// DATA SET for GANTT
	//#########################################################################
	//#########################################################################
	// The following was used as an example: http://www.java2s.com/Code/Java/Chart/JFreeChartGanttDemo1.htm
	
	public IntervalCategoryDataset createGanttDataset(GraphType graphType)
	{
		int rowCount = _tm.getRowCount();
		int colCount = _tm.getColumnCount();

		if (_pipeCmd.isDebugEnabled())
		{
			_pipeCmd.addDebugMessage("createGanttDataset()");
//			_pipeCmd.addDebugMessage("pivot="+pivot);
			_pipeCmd.addDebugMessage("rowCount="+rowCount);
			_pipeCmd.addDebugMessage("colCount="+colCount);
		}

		// Get/Compose the KEY and Values Column List
		List<Integer> keyPosList = createColumnPosList(CreateColumnType.KEY_COLS,      _pipeCmd.getKeyCols()  , null);
		List<Integer> grpPosList = createColumnPosList(CreateColumnType.GROUP_COLS,    _pipeCmd.getGroupCols(), null);
		List<Integer> valPosList = createColumnPosList(CreateColumnType.TS_VALUE_COLS, _pipeCmd.getValCols()  , keyPosList);

		if ( keyPosList.size() != 1)
		{
			String msg = "ERROR: Not enough KEY Columns: 1 columns must be specified, which acts as a 'task' name. keyPosList=" + keyPosList + ", getKeyCols()=" + _pipeCmd.getKeyCols();
			_pipeCmd.addErrorMessage(msg);

			throw new RuntimeException(msg);
		}
		
		if ( valPosList.size() != 2)
		{
			String msg = "ERROR: Not enough Value Columns: 2 columns must be specified, with datatypes Timestamp. A start/end date. valPosList=" + valPosList + ", getValCols()=" + _pipeCmd.getValCols();
			_pipeCmd.addErrorMessage(msg);

			throw new RuntimeException(msg);
		}
		
		int keyCol       = keyPosList.get(0);
		int groupCol     = grpPosList.isEmpty() ? -1 : grpPosList.get(0);
		int beginTimeCol = valPosList.get(0);
		int endTimeCol   = valPosList.get(1);
		
		if ( ! (isTimeColumn(beginTimeCol) || isTimeColumn(endTimeCol)) )
		{
			String tsStartColName        = _tm.getColumnName(beginTimeCol);
			int    tsStartJdbcColumnType = _tm.getSqlType(   beginTimeCol);
			
			String tsEndColName        = _tm.getColumnName(endTimeCol);
			int    tsEndJdbcColumnType = _tm.getSqlType(   endTimeCol);
			
			String msg = "ERROR: Invalid SQL: \n"
					+ " - First column must be of a time/date/timestamp. StartColName='"+tsStartColName+"', StartJdbcColumnType="+tsStartJdbcColumnType+", StartJdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(tsStartJdbcColumnType)+"'. \n"
					+ " - Second column must be of a time/date/timestamp. EndColName='"+tsEndColName+"', EndJdbcColumnType="+tsEndJdbcColumnType+", EndJdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(tsEndJdbcColumnType)+"'. \n";
			_pipeCmd.addErrorMessage(msg);

			throw new RuntimeException(msg);
		}

//		Map<String, TimeSeries> tsMap = new LinkedHashMap<>();
		//TimeSeries ts = new TimeSeries();

		Map<String, TaskSeries> tsMap = new LinkedHashMap<>();

		for (int r = 0; r < rowCount; r++)
		{
			for (int col : valPosList)
			{
//				Timestamp timestamp = _tm.getValueAsTimestamp(r, tsKeyCol);
//				FixedMillisecond timestampMs = new FixedMillisecond(timestamp);
				String taskName   = _tm.getValueAsString(r, keyCol);
				String groupName  = groupCol == -1 ? "" : _tm.getValueAsString(r, groupCol);
				
				Timestamp beginTs = _tm.getValueAsTimestamp(r, beginTimeCol);
				Timestamp endTs   = _tm.getValueAsTimestamp(r, endTimeCol);
				
				if (beginTs == null)
				{
					String msg = "BEGIN Timestamp Column is missing for: key='" + taskName + "', groupName='" + groupName + "', beginTimeCol='" + _tm.getColumnName(beginTimeCol) + "' is NULL, Skipping this row.";
					_pipeCmd.addInfoMessage(msg);
					continue;
				}
				
				if (endTs == null)
				{
					String msg = "END Timestamp Column is missing for: key='" + taskName + "', groupName='" + groupName + "', beginTimeCol='" + _tm.getColumnName(beginTimeCol) + "' is NULL, Setting this to NOW.";
					_pipeCmd.addInfoMessage(msg);

					endTs = new Timestamp(System.currentTimeMillis());
				}
				
				// Create a TASK, with a start/end date
				Task task = new Task(taskName, beginTs, endTs);

				// Add it to a series/group (the default group is '')
				TaskSeries taskSerie = tsMap.get(groupName);
				if (taskSerie == null)
				{
					taskSerie = new TaskSeries(groupName);
					tsMap.put(groupName, taskSerie);
				}
				taskSerie.add(task);
			}
		}

		// Add ALL Series to the dataset
		TaskSeriesCollection dataset = new TaskSeriesCollection();
		for (TaskSeries taskSerie : tsMap.values())
		{
			dataset.add(taskSerie);
		}

		return dataset;
	}

	/**
	 * INTERNAl: Try to convert a String representation (using obj.toString()) to a Double 
	 * 
	 * @param row
	 * @param col
	 * @param removeRegEx 
	 * 
	 * @return 
	 */
	private Double convertStringToDouble(int row, int col, String removeRegEx)
	{
		String str = null;
		Object obj = _tm.getValueAsObject(row, col);

		if (obj == null)
			return null;
		
		str = obj.toString().trim();

		if (str.equals(""))
			return null;
		
		if (removeRegEx != null)
		{
			String newStr = str.replaceAll(removeRegEx, "").trim();
			
			if (_pipeCmd.isDebugEnabled() && !str.equals(newStr))
			{
				String colName     = _tm.getColumnName(col);
				int jdbcColumnType = _tm.getSqlType(col); // starts at 0

				_pipeCmd.addDebugMessage("Changed value using RegEx '"+removeRegEx+"' for row="+row+", col="+col+", colName='"+colName+"'. From '"+str+"' to '"+newStr+"'. Extra info: origin jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
			}
			
			// Assign the new value
			str = newStr;
		}
		
		try
		{
			return Double.parseDouble(str);
		}
		catch (NumberFormatException e)
		{
			String colName     = _tm.getColumnName(col);
			int jdbcColumnType = _tm.getSqlType(col); // starts at 0

			_pipeCmd.addWarningMessage("Problems converting String to Double: Skipping row="+row+", col="+col+", colName='"+colName+"', colValStr='"+str+"', jdbcColumnType="+jdbcColumnType+", jdbcColumnTypeStr='"+ResultSetTableModel.getColumnJavaSqlTypeName(jdbcColumnType)+"'.");
			
			return null;
		}
	}
	
	/**
	 * Get the graph/chart object created in <code>createChart()</code>
	 * 
	 * @return
	 */
	public JFreeChart getChart()
	{
		return _chart;
	}

	/**
	 * Is this a "time" column
	 * 
	 * @param col column number starting at 0
	 * @return true or false
	 */
	public boolean isTimeColumn(int col)
	{
		return isTimeColumn(_tm, col);
	}

	/**
	 * Is this a "number" column
	 * 
	 * @param col column number starting at 0
	 * @return true or false
	 */
	public boolean isNumberColumn(int col)
	{
		return isNumberColumn(_tm, col);
	}

	/**
	 * Is this a "string" column
	 * 
	 * @param col column number starting at 0
	 * @return true or false
	 */
	public boolean isStringColumn(int col)
	{
		return isStringColumn(_tm, col);
	}







	/**
	 * Is this a "time" column
	 * 
	 * @param rstm ResultSetTableModel (if null then col is the JDBC data type to be checked)
	 * @param colOrType column number starting at 0 (or if rstm == null, the JDBC DataType)
	 * 
	 * @return true or false
	 */
	public static boolean isTimeColumn(ResultSetTableModel rstm, int colOrType)
	{
		int jdbcColumnType = colOrType;

		if (rstm != null)
			jdbcColumnType = rstm.getSqlType(colOrType);


		switch (jdbcColumnType) 
		{
		case Types.TIMESTAMP:
		case Types.TIME:
		case Types.DATE:
			return true;
		}

		return false;
	}

	/**
	 * Is this a "number" column
	 * 
	 * @param rstm ResultSetTableModel (if null then col is the JDBC data type to be checked)
	 * @param colOrType column number starting at 0 (or if rstm == null, the JDBC DataType)
	 * 
	 * @return true or false
	 */
	public static boolean isNumberColumn(ResultSetTableModel rstm, int colOrType)
	{
		int jdbcColumnType = colOrType;

		if (rstm != null)
			jdbcColumnType = rstm.getSqlType(colOrType);

		switch (jdbcColumnType) 
		{
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
		case Types.BIGINT:
		case Types.FLOAT:
		case Types.DOUBLE:
		case Types.DECIMAL:
		case Types.NUMERIC:
		case Types.REAL:
			return true;
		}

		return false;
	}

	/**
	 * Is this a "string" column
	 * 
	 * @param rstm ResultSetTableModel (if null then col is the JDBC data type to be checked)
	 * @param colOrType column number starting at 0 (or if rstm == null, the JDBC DataType)
	 * 
	 * @return true or false
	 */
	public static boolean isStringColumn(ResultSetTableModel rstm, int colOrType)
	{
		int jdbcColumnType = colOrType;

		if (rstm != null)
			jdbcColumnType = rstm.getSqlType(colOrType);


		switch (jdbcColumnType) 
		{
		case Types.CHAR:
		case Types.CLOB:
		case Types.LONGNVARCHAR:
		case Types.LONGVARCHAR:
		case Types.NCHAR:
		case Types.NCLOB:
		case Types.NVARCHAR:
		case Types.VARCHAR:
			return true;
		}

		return false;
	}
}
