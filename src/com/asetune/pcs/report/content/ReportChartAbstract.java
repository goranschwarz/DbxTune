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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.pcs.report.content;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import com.asetune.graph.TrendGraphColors;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public abstract class ReportChartAbstract implements IReportChart
{
	private static Logger _logger = Logger.getLogger(ReportChartAbstract.class);

	public enum ChartType
	{
		LINE, 
		STACKED_BAR
	};
	
//	public final static String SQL_WHERE_PREFIX = "SQL-WHERE:";
	public final static String SKIP_COLNAME_WITH_VALUE_ABOVE = "SKIP_COLNAME_WITH_VALUE_ABOVE:";
	
	public final static int CHART_HEIGHT_100 = 300;  // 100%
	public final static int CHART_WIDTH_100  = 1900; // 100%
	public final static int CHART_HEIGHT_90  = 270;  // 90%
	public final static int CHART_WIDTH_90   = 1710; // 80%
	public final static int CHART_HEIGHT_80  = 240;  // 80%
	public final static int CHART_WIDTH_80   = 1500; // 80% 
	public final static int CHART_HEIGHT_70  = 210;  // 70%
	public final static int CHART_WIDTH_70   = 1330; // 70%
	public final static int CHART_HEIGHT_60  = 180;  // 60%
	public final static int CHART_WIDTH_60   = 1130; // 60%
	public final static int CHART_HEIGHT_50  = 150;  // 50%
	public final static int CHART_WIDTH_50   = 950;  // 50%

	public final static int CHART_HEIGHT_FULL_MSG = CHART_HEIGHT_80;
	public final static int CHART_WIDTH_FULL_MSG  = CHART_WIDTH_80; 

	public final static int CHART_HEIGHT_SHORT_MSG = CHART_HEIGHT_80;
	public final static int CHART_WIDTH_SHORT_MSG  = CHART_WIDTH_80; 
	
	
	private JFreeChart _chart;
	private Dataset    _dataset;

	protected double   _minDataValue = 0d;
	protected double   _maxDataValue = 0d;
	protected boolean  _hasMinMaxDataValue = false;


	private String     _problem;
	private Exception  _exception;
	private int        _defaultQueryTimeout = 0; 

	private String     _preComment;
	private String     _postComment;

	
	private ReportEntryAbstract _reportEntry;
	private DbxConnection _conn;
	private String        _schemaName;
	private ChartType     _chartType;
	private String        _cmName;
	private String        _graphName;
	private String        _graphTitle;
	private int           _maxValue;
	protected boolean     _isSorted;
	private String        _chartId;  // used to create 
	private static long   _chartIdCounter = 0; // incremented in the constructor for every creation.
	
	@Override public JFreeChart          getChart()               { return _chart;               }
	@Override public Dataset             getDataset()             { return _dataset;             }
	@Override public ReportEntryAbstract getReportEntry()         { return _reportEntry;         }
	          public DbxConnection       getConnection()          { return _conn;                }
	          public String              getSchemaName()          { return _schemaName;          }
	          public ChartType           getChartType()           { return _chartType;           }
	          public String              getCmName()              { return _cmName;              }
	          public String              getGraphName()           { return _graphName;           }
	@Override public String              getGraphTitle()          { return _graphTitle;          }
	@Override public String              getProblem()             { return _problem;             }
	@Override public Exception           getException()           { return _exception;           }
	@Override public int                 getDefaultQueryTimeout() { return _defaultQueryTimeout; }
	@Override public String              getPreComment()          { return _preComment;          }
	@Override public String              getPostComment()         { return _postComment;         }

	public String getSchemaNameSqlPrefix()
	{
		if (StringUtil.isNullOrBlank(_schemaName))
			return "";

		if (_conn == null)
			return "[" + _schemaName + "]."; 
		
		return _conn.getLeftQuote() + _schemaName + _conn.getRightQuote() + "."; 
	}
	
	public void setChart              (JFreeChart          chart      ) { _chart               = chart;       }
	public void setDataset            (Dataset             dataset    ) { _dataset             = dataset;     }
	public void setReportEntry        (ReportEntryAbstract reportEntry) { _reportEntry         = reportEntry; }
	public void setConnection         (DbxConnection       conn       ) { _conn                = conn;        }
	public void setSchemaName         (String              schemaName ) { _schemaName          = schemaName;  }
	public void setChartType          (ChartType           chartType  ) { _chartType           = chartType;   }
	public void setCmName             (String              cmName     ) { _cmName              = cmName;      }
	public void setGraphName          (String              graphName  ) { _graphName           = graphName;   }
	public void setGraphTitle         (String              graphTitle ) { _graphTitle          = graphTitle;  }
	public void setProblem            (String              problem    ) { _problem             = problem;     }
	public void setException          (Exception           ex         ) { _exception           = ex;          }
	public void setDefaultQueryTimeout(int                 seconds    ) { _defaultQueryTimeout = seconds;     }
	public void setPreComment         (String              preComment ) { _preComment          = preComment;  }
	public void setPostComment        (String              postComment) { _postComment         = postComment; }

	public ReportChartAbstract(ReportEntryAbstract reportEntry, DbxConnection conn, String schemaName, ChartType chartType, String cmName, String graphName, String graphTitle, int maxValue, boolean sorted)
	{
		_reportEntry     = reportEntry;
		_conn            = conn;
		_schemaName      = schemaName;
		_chartType       = chartType;
		_cmName          = cmName;
		_graphName       = graphName; 
		_graphTitle      = graphTitle;
		_maxValue        = maxValue;
		_isSorted        = sorted;
		
		_chartIdCounter++;
		_chartId         = _cmName + "_" + _chartIdCounter;
	}

	@Override
	public IReportChart create()
	{
		try
		{
			// Create the dataset
			setDataset(createDataset());
				
			// Create the graph/chart
			if (getDataset() != null)
			{
				setChart(createChart());
			}
		}
		catch (RuntimeException rte)
		{
			setProblem("Problems creating ReportChartObject, Caught: " + rte);
			setException(rte);

			_logger.warn("Problems creating ReportChartObject, caught RuntimeException.", rte);
		}

		return this;
	}

	private int getJsChartSizeHeight()
	{
		return CHART_HEIGHT_FULL_MSG;
	}
	
	private int getJsChartSizeWidth()
	{
		return CHART_WIDTH_FULL_MSG;
	}

	private int getImageSizeHeight()
	{
		return CHART_HEIGHT_FULL_MSG;
//		return CHART_HEIGHT_SHORT_MSG;
//		if (getReportEntry() != null)
//		{
//			if (getReportEntry().isFullMessageType())
//				return CHART_HEIGHT_FULL_MSG;
//			
//			if (getReportEntry().isShortMessageType())
//				return CHART_HEIGHT_SHORT_MSG;
//		}
//
//		return CHART_HEIGHT_FULL_MSG;
	}
	
	private int getImageSizeWidth()
	{
		return CHART_WIDTH_FULL_MSG;
//		if (getReportEntry() != null)
//		{
//			if (getReportEntry().isFullMessageType())
//				return CHART_WIDTH_FULL_MSG;
//			
//			if (getReportEntry().isShortMessageType())
//				return CHART_WIDTH_SHORT_MSG;
//		}
//
//		return CHART_WIDTH_FULL_MSG;
	}

	/**
	 * Can be called when creating data set to indicate what value is the min/max value in the data set<br>
	 * This can then be used to check "things" when creating the renders
	 * 
	 * @param dataValue  The value which we will do: <code>Math.min()</code> and <code>Math.max()</code> on
	 */
	public void setDatasetMinMaxValue(Double dataValue)
	{
		if (dataValue == null)
			return;

		_hasMinMaxDataValue = true;

		_minDataValue = Math.min(_minDataValue, dataValue.doubleValue());
		_maxDataValue = Math.max(_maxDataValue, dataValue.doubleValue());
	}
	public Double getDatasetMinValue() { return _minDataValue; }
	public Double getDatasetMaxValue() { return _maxDataValue; }
	
	public boolean isAllDataValuesNearZero()
	{
		// If method setDatasetMinMaxValue() wasn't called, then we dont know that min/max values, so return false
		if (_hasMinMaxDataValue == false)
			return false;

		double minVal = 0.0001;
		double maxVal = 0.0001;

//System.out.println("isAllDataValuesNearZero(): <<<<< " + ((getDatasetMinValue() >minVal && getDatasetMaxValue() < maxVal) || (getDatasetMinValue() == 0d && getDatasetMaxValue() == 0d)) + ", min=" + getDatasetMinValue() + ", max=" + getDatasetMaxValue());

		if (getDatasetMinValue() == 0d && getDatasetMaxValue() == 0d)
			return true;

		if (getDatasetMinValue() > minVal && getDatasetMaxValue() < maxVal)
			return true;

		return false;
	}

	/**
	 * 
	 * @return
	 */
	protected abstract JFreeChart createChart();

	/**
	 * 
	 * @return
	 */
	protected abstract Dataset createDataset();
	
	@Override
	public String getHtmlContent(String preText, String postText)
	{
		StringWriter writer = new StringWriter();
		
		// StringWriter do not throw exception, but the interface does, do not care about the Exception 
		try { writeHtmlContent(writer, preText, postText); }
		catch (IOException ignore) {}

		return writer.toString();
	}

	
//	@Override
//	public String getHtmlContent(String preText, String postText)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Any PROBLEMS... return at the end of this block
//		if (StringUtil.hasValue(_problem))
//		{
//			sb.append(_problem);
//			sb.append("<br>\n");
//
//			if (_exception != null)
//			{
//				sb.append("<b>Exception:</b> ").append(_exception.toString()).append("<br>\n");
//				sb.append("<pre>");
//				sb.append(StringUtil.exceptionToString(_exception));
//				sb.append("</pre>");
//			}
//
//			return sb.toString();
//		}
//
//
//		// Pre Text
//		if (StringUtil.hasValue(preText))
//		{
//			sb.append(preText);
//			sb.append("<br>\n");
//		}
//		if (StringUtil.hasValue(getPreComment()))
//		{
//			sb.append(getPreComment());
//			sb.append("<br>\n");
//		}
//
//		// CHART
//		sb.append("<br>\n");
//		sb.append(toHtmlInlineImage());
//		sb.append("<br>\n");
//
//		// Post Text
//		if (StringUtil.hasValue(postText))
//		{
//			sb.append(postText);
//			sb.append("<br>\n");
//		}
//		if (StringUtil.hasValue(getPostComment()))
//		{
//			sb.append(getPostComment());
//			sb.append("<br>\n");
//		}
//
//		return sb.toString();
//	}

	
	public String toHtmlInlineImage()
	{
		StringWriter writer = new StringWriter();
		
		// StringWriter do not throw exception, but the interface does, do not care about the Exception 
		try { writeAsHtmlInlineImage(writer); }
		catch (IOException ignore) {}

		return writer.toString();
	}
	
//	public String toHtmlInlineImage()
//	{
//		if (_chart == null)
//		{
//			return "<b>Chart object is NULL... in ReportChartObject.toHtmlInlineImage()</b> <br> \n";
//		}
//		
//		try
//		{
////			OutputStream out = new FileOutputStream("c:/tmp/xxx.png");
//			ByteArrayOutputStream bo = new ByteArrayOutputStream();
//			Base64OutputStream b64o = new Base64OutputStream(bo, true);
//
//			// writeChartAsPNG produces the same size with compression="default" (just using with, height), compression=0 and compression=9
//			// So no difference here... 
//			int     width       = 1900;
//			int     height      = 300;
//			boolean encodeAlpha = false;
//			int     compression = 0;
////			ChartUtils.writeChartAsPNG(b64o, _chart, 2048, 300);
//			ChartUtils.writeChartAsPNG(b64o, _chart, width, height, encodeAlpha, compression);
////			ChartUtils.writeChartAsJPEG(b64o, _chart, width, height);
//
//			String base64Str = new String(bo.toByteArray());
//
////			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/png;base64," + base64Str + "'>";
//			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/jpeg;base64," + base64Str + "'>";
//			return htmlStr;
//		}
//		catch (IOException ex)
//		{
//			_exception = ex;
//			_problem = ex.toString();
//
//			return "";
//		}
//	}


	@Override
	public void writeHtmlContent(Writer sb, String preText, String postText)
	throws IOException
	{
//		StringBuilder sb = new StringBuilder();

		// Any PROBLEMS... return at the end of this block
		if (StringUtil.hasValue(_problem))
		{
			sb.append(_problem);
			sb.append("<br>\n");

			if (_exception != null)
			{
				sb.append("<b>Exception:</b> ").append(_exception.toString()).append("<br>\n");
				sb.append("<pre>");
				sb.append(StringUtil.exceptionToString(_exception));
				sb.append("</pre>");
			}

			return;
		}


		// Pre Text
		if (StringUtil.hasValue(preText))
		{
			sb.append(preText);
			sb.append("<br>\n");
		}
		if (StringUtil.hasValue(getPreComment()))
		{
			sb.append(getPreComment());
			sb.append("<br>\n");
		}

		// CHART
		sb.append("<br>\n");
		//sb.append(toHtmlInlineImage());
		writeAsHtmlInlineImage(sb);           // Write as <img id='img_" + _cmName + _graphTitle + "' src='data:image/png;base64,...>
		writeAsChartJs(sb);                   // Write as <canvas id='canvas_" + _cmName + _graphTitle + "'>...</div>
		sb.append("<br>\n");

		if (_isSorted)
		{
			sb.append("<b>Note:</b> The above chart is sorted by summary of all values in each series (most active serie is first).");
			sb.append("<br>\n");
		}

		// Post Text
		if (StringUtil.hasValue(postText))
		{
			sb.append(postText);
			sb.append("<br>\n");
		}
		if (StringUtil.hasValue(getPostComment()))
		{
			sb.append(getPostComment());
			sb.append("<br>\n");
		}
	}

	public void writeAsChartJs(Writer writer)
	throws IOException
	{
		// Only write this once (keep the status field in the "head" report)
		if (getReportEntry().hasStatusEntry("chartJs_writeOnce") == false)
		{
			getReportEntry().setStatusEntry("chartJs_writeOnce");

			String name = "chartJs";
			String label = "Initializing Chart Info : "; // The extra space before ":" to align with "spark-line" a bit better 
			String topPx = "50px";
			
			writer.append("\n");
			writer.append("\n");
			writer.append("<div id='" + name + "-progress-div' style='display:none'> \n");
			writer.append("  <label for='" + name + "-progress-bar'>" + label + "</label> \n");
			writer.append("  <progress id='" + name + "-progress-bar' max='100' style='height: 20px; width:80%;'></progress> \n");
			writer.append("  <button id='" + name + "-stop-progress-but' onclick='stopChartInit()' type='button' class='btn btn-primary btn-sm'>Stop</button> \n");
			writer.append("</div>\n");

			writer.append("\n");
			writer.append("<script type='text/javascript'>\n");
			writer.append("\n");
			writer.append("    // Variable to hold all ChartJS objects \n");
			writer.append("    const chartJsListToLoad  = []; \n");
			writer.append("    const chartJsListCreated = []; \n");
			writer.append("      var chartJsListMax     = 0; \n");
			writer.append("    const chartJsConfMap     = new Map(); \n");
			writer.append("\n");
			writer.append("    // function called when pressing 'Stop' button right next to the progress bar \n");
			writer.append("    function stopChartInit() \n");
			writer.append("    { \n");
			writer.append("        console.log('-stopChartInit-');  \n");
			writer.append("        while (chartJsListToLoad.length !== 0) \n");
			writer.append("            chartJsListToLoad.shift(); \n");
			writer.append("    } \n");
			
			writer.append("    // function to be called at page load, which will initialize all Charts, (and update progressbar) \n");
			writer.append("    function loadNextChart() \n");
			writer.append("    { \n");

			writer.append("        // Enable the progresbar; \n");
			writer.append("        if (chartJsListCreated.length === 0) \n");
			writer.append("        { \n");
			writer.append("            console.log('-load-first-" + name + "-');  \n");
			writer.append("            chartJsListMax = chartJsListToLoad.length; \n");
			
			writer.append("            // show the progressbar\n");
			writer.append("            document.getElementById('" + name + "-progress-div').style.display = 'block'; \n");  // show
			
			writer.append("            // if possible move the div into the 'progress-area' or add some attributes to it. \n");
			writer.append("            if (document.getElementById('progress-area')) \n");
			writer.append("            { \n");
			writer.append("                console.log('Moving div: " + name + "-progress-div --to--> div: progress-area');  \n");
			writer.append("                $('#" + name + "-progress-div').detach().appendTo('#progress-area'); \n");
			writer.append("            } \n");
			writer.append("            else \n");
			writer.append("            { \n");
			writer.append("                console.log('Cant find div: progress-area. instead; Setting some css options for div: " + name + "-progress-div');  \n");
			writer.append("                $('#" + name + "-progress-div').css({'position':'fixed', 'background-color':'white', 'top':'" + topPx + "', 'left':'20px', 'width':'100%'}); \n");
			writer.append("            } \n");
			writer.append("        } \n");

			writer.append("        // Disable the progresbar; \n");
			writer.append("        if (chartJsListToLoad.length === 0) \n");
			writer.append("        { \n");
			writer.append("            console.log('-end-of-" + name + "-to-load-');  \n");
			writer.append("            // hide the progressbar \n");
			writer.append("            document.getElementById('" + name + "-progress-div').style.display = 'none'; \n");   // hide
			writer.append("            return; \n");
			writer.append("        } \n");
			
			writer.append("        var tagName = chartJsListToLoad.shift(); \n");
			writer.append("        var config  = chartJsConfMap.get(tagName); \n");
			
			writer.append("        var pctLoaded = chartJsListCreated.length / chartJsListMax * 100; \n");
			writer.append("        document.getElementById('" + name + "-progress-bar').value = pctLoaded; \n");

			writer.append("        console.log('-creating-chart: ' + tagName);  \n");
			writer.append("        var ctx     = document.getElementById('canvas_' + tagName).getContext('2d'); \n");
			writer.append("        var chartjs = new Chart(ctx, config); \n");
			
			writer.append("        chartJsListCreated.push(chartjs); \n");
			writer.append("\n");
			writer.append("        // HIDE the image, and SHOW the chart! \n");
			writer.append("        document.getElementById('img_'       + tagName).style.display = 'none'; \n");   // hide
			writer.append("        document.getElementById('div_chart_' + tagName).style.display = 'block'; \n");  // show

			writer.append("        // Load next chart \n");
			writer.append("        setTimeout(loadNextChart, 10); \n");
			writer.append("    }\n");
			writer.append("\n");
			writer.append("    // Call the function loadNextChart() for the FIRST time \n");
			writer.append("    document.addEventListener('DOMContentLoaded', function() \n");
			writer.append("    { \n");
			writer.append("        loadNextChart(); \n");
			writer.append("    }); \n");
			writer.append("\n");
			writer.append("\n");
			writer.append("    const chartAreaBackgroundPlugin = {                                                               \n");
			writer.append("        id: 'chartAreaBackgroundPlugin',                                                            \n");
			writer.append("        beforeDraw: function (chart, args, options) {                                               \n");
//			writer.append("            console.log('beforeDraw', chart, args, options);                                        \n");
			writer.append("            var ctx = chart.ctx;                                                                    \n");
			writer.append("            var chartArea = chart.chartArea;                                                        \n");
			writer.append("                                                                                                    \n");
			writer.append("            ctx.save();                                                                             \n");
//			writer.append("            ctx.fillStyle = 'rgba(251, 85, 85, 0.4)';                                               \n"); // Pink
			writer.append("            ctx.fillStyle = options.bgColor;                                                        \n");
			writer.append("            ctx.fillRect(chartArea.left, chartArea.top, chartArea.right - chartArea.left, chartArea.bottom - chartArea.top); \n");
			writer.append("            ctx.restore();                                                                          \n");
			writer.append("        }                                                                                           \n");
			writer.append("    };                                                                                              \n");
			writer.append("\n");
			writer.append("</script>\n");
			writer.append("\n");
			writer.append("\n");
			
			// reference/call the above using:
			// var config = {
			//     ...
			//     options: { 
			//         plugins: { 
			//             chartAreaBackgroundPlugin: { 
			//                 bgColor: 'rgba(191, 191, 191, 0.4)', 
			//             },
			//         },
			//     },
			//     plugins: [ chartAreaBackgroundPlugin ]
			// }
		}

		if (ChartType.LINE.equals(_chartType))
		{
			writeAsChartJsLineChart(writer);
		}
		else if (ChartType.STACKED_BAR.equals(_chartType))
		{
			writeAsChartJsStackedBarChart(writer);
		}
		else
		{
			throw new RuntimeException("Unhandled ChartType '" + _chartType + "'.");
		}
	}


	private void writeAsChartJsHtmlAndJsCode(Writer writer, ChartType chartType, List<String> dataTs, LinkedHashMap<String, List<Number>> seriesDataVal)
	throws IOException
	{
		String tagName    = _chartId;
		String chartLabel = _graphTitle.replace("'", "\\'"); // escape single-quote[']

		String type = null;
		if      (ChartType.LINE       .equals(chartType)) type = "line";
		else if (ChartType.STACKED_BAR.equals(chartType)) type = "bar";
		else throw new IllegalArgumentException("chartType '" + chartType + "' is not supported.");
		
		int width  = getJsChartSizeWidth();
		int height = getJsChartSizeHeight();

		// If we have a lot of "labels", the chart area will shrink, so make it slightly bigger here
		Set<String> seriesLabels = seriesDataVal.keySet();
		int legendLabelPixWidthSum = 0;
		for (String str : seriesLabels)
			legendLabelPixWidthSum += str.length() * 5; // lets assume that every char is approximately 5 pixel wide (for simplicity)
		double guessedLegendRows = Math.ceil(legendLabelPixWidthSum / width);
		if (guessedLegendRows > 1)
			height += guessedLegendRows * 17; // Assume every Legend row takes approximately 17px
		
		// Write the HTML part, with values from: getDataset()
		writer.append("\n");
		writer.append("\n");
		writer.append("<div id='div_chart_" + tagName + "' style='display:none'>\n");
		writer.append("<canvas id='canvas_" + tagName + "' width='" + width +"' height='" + height + "'></canvas> \n");
		writer.append("</div>\n");

		// Write the JavaScript part... which will hide the IMAGE and show the ChartJS object
		writer.append("\n");
		writer.append("\n");
		writer.append("<script type='text/javascript'>\n");
		
		writer.append("// Create option and a ChartJS object \n");
		writer.append("const options_" + tagName + " = { \n");
//		writer.append("const options = { \n");

		writer.append("    type: '" + type + "', \n");

		writer.append("    data: \n");
		writer.append("    { \n");
		writer.append("        labels: ["); writer.append(StringUtil.toCommaStr(dataTs)); writer.append("], \n");
		writer.append("        datasets: [ \n");

		int cnt = 0;
		for (Entry<String, List<Number>> entry : seriesDataVal.entrySet())
		{
			Color color = TrendGraphColors._colors[cnt % TrendGraphColors._colors.length];
			//String newColor = chartColors[cnt % chartColors.length];
			String newColor = "'rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")'";
			String label = entry.getKey().replace("\\", "\\\\").replace("'", "\\'"); // Replace any backslash with 2 backslash (\ -> \\) and (' -> \')
			
			writer.append("            { \n");
			writer.append("                label: '" + label + "', \n");    // The label for the dataset which appears in the legend and tooltips
			writer.append("                data: ["); writer.append(StringUtil.toCommaStr(entry.getValue())); writer.append("], \n");
			if (ChartType.LINE.equals(chartType))
			{
				writer.append("                borderWidth: 1, \n");                     // The width of the line in pixels.
				writer.append("                pointRadius: 0, \n");                     // The radius of the point shape. If set to 0, the point is not rendered.
				writer.append("                pointHitRadius: 5, \n");                  // The pixel size of the non-displayed point that reacts to mouse events.
				writer.append("                fill: false, \n");                        // How to fill the area under the line.
				writer.append("                borderColor: " + newColor + ", \n");      // The color of the line.
			}
			writer.append("                backgroundColor: " + newColor + ", \n");  // The fill color under the line.
			writer.append("            }, \n");
			cnt++;
		}
		writer.append("        ] \n");
		writer.append("    }, \n");

		writer.append("    options: { \n");
		writer.append("        responsive: false, \n");
		writer.append("        maintainAspectRatio: true, \n");
		writer.append("        plugins: { \n");
		writer.append("            title: { \n");
		writer.append("                display: true, \n");
		writer.append("                position: 'top', \n");
		writer.append("                font: { size: 20, weight: 'bold' }, \n");
		writer.append("                text: '" + chartLabel + "', \n");
		writer.append("            }, \n");
		writer.append("            legend: { \n");
		writer.append("                position: 'bottom', \n");
		writer.append("                labels: { \n");
		writer.append("                    boxWidth: 10, \n");
		writer.append("                    fontSize: 10, \n");
		writer.append("                } \n");
		writer.append("            }, \n");
		writer.append("            chartAreaBackgroundPlugin: { \n");
		writer.append("                bgColor: 'rgba(191, 191, 191, 0.4)', \n"); // Gray (ish)
		writer.append("            }, \n");
		writer.append("        }, \n"); // end: plugins
		writer.append("        scales: { \n");
		writer.append("            x: { \n");
		if (ChartType.STACKED_BAR.equals(chartType))
		{
			writer.append("                stacked: true, \n");
		}
		writer.append("                type: 'time', \n");
//		writer.append("                distribution: 'linear', \n");
		writer.append("                time: { \n");
//		writer.append("                    tooltipFormat: 'DD T', \n");
		writer.append("                    unit: 'hour', \n");
//		writer.append("                    parser: 'YYYY-MM-DDTHH:mm:ss.SSSSSSZ', \n");
		writer.append("                    stepSize: 1, \n");
		writer.append("                    tooltipFormat: 'YYYY-MM-DD (dddd) HH:mm:ss', \n");
		writer.append("                    displayFormats: { \n");
		writer.append("                        millisecond: 'HH:mm:ss', \n");
		writer.append("                        second: 'HH:mm:ss', \n");
		writer.append("                        minute: 'HH:mm:ss', \n");
		writer.append("                        hour:   'HH:mm', \n");
		writer.append("                    } \n");
		writer.append("                }, \n");
		writer.append("                ticks: { \n");
		writer.append("                    beginAtZero: true \n");
		writer.append("                }, \n");
		writer.append("                gridLines: { \n");
		writer.append("                    color: 'rgba(0, 0, 0, 0.1)', \n");
		writer.append("                    zeroLineColor: 'rgba(0, 0, 0, 0.25)', \n");
		writer.append("                }, \n");
		writer.append("            }, \n"); // end: x
		writer.append("            y: { \n");
		if (ChartType.STACKED_BAR.equals(chartType))
		{
			writer.append("                stacked: true, \n");
		}
		if (_maxValue > 0)
		{
    		writer.append("                suggestedMax: " + _maxValue + ", \n");
    		writer.append("                suggestedMin: 0, \n");
		}
		writer.append("                ticks: { \n");
		writer.append("                    beginAtZero: true, \n");
		writer.append("                }, \n");
		writer.append("                gridLines: { \n");
		writer.append("                    color: 'rgba(0, 0, 0, 0.1)', \n");
		writer.append("                    zeroLineColor: 'rgba(0, 0, 0, 0.25)', \n");
		writer.append("                }, \n");
		writer.append("            }, \n"); // end: y
		writer.append("        }, \n"); // end: scales
		writer.append("    }, \n"); // end: options
		writer.append("    plugins: [ chartAreaBackgroundPlugin ] \n");
		writer.append("} \n"); // end: var options_xxx

		// Push the 'tagName' to a list, which later will be pulled to create the ChartJS Object
		writer.append("chartJsListToLoad.push('" + tagName + "') \n");
		writer.append("chartJsConfMap.set('" + tagName + "', options_" + tagName + "); \n");
		
//		writer.append("var ctx_" + tagName + " = document.getElementById('canvas_" + tagName + "').getContext('2d'); \n");
//		writer.append("var chartjs_" + tagName + " = new Chart(ctx_" + tagName + ", options_" + tagName + "); \n");
//		writer.append("\n");
//
//		writer.append("// disable the image \n");
//		writer.append("document.getElementById('img_"       + tagName + "').style.display = 'none'; \n");   // hide
//		writer.append("document.getElementById('div_chart_" + tagName + "').style.display = 'block'; \n");  // show

		writer.append("</script>\n");
	}

	/**
	 * STACKED BAR CHART
	 * 
	 * @param writer
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void writeAsChartJsStackedBarChart(Writer writer)
	throws IOException
	{
		// Extract "data" from the "data set"
		LinkedHashMap<String, List<Number>> seriesDataVal = new LinkedHashMap<>();
		List<String> dataTs = new ArrayList<>();
		
		Dataset dataset = getDataset();
		if (dataset instanceof DefaultCategoryDataset)
		{
			DefaultCategoryDataset catDataset = (DefaultCategoryDataset) dataset;

			// Columns - contains the Timestamps
			for (int c=0; c<catDataset.getColumnCount(); c++)
			{
				Comparable colKey = catDataset.getColumnKey(c);
				dataTs.add("'" + colKey + "'");

				//System.out.println("CCCCCCCCCCCCCCC[" + c + "]: colKey=|" + colKey + "|.");
			}

			// Rows - contains the "series" (or the different parts of each bar... the: waitType1, waitType2, waitType3...
			for (int r=0; r<catDataset.getRowCount(); r++)
			{
				Comparable rowKey = catDataset.getRowKey(r);
				//System.out.println("RRRRRRRRRRRRRRR[" + r + "]: colKey=|" + rowKey + "|.");

				List<Number> dataVal = new ArrayList<>();
				seriesDataVal.put(rowKey.toString(), dataVal);

				for (int c=0; c<catDataset.getColumnCount(); c++)
				{
					Comparable colKey = catDataset.getColumnKey(c);

					Number val = catDataset.getValue(rowKey, colKey);
					dataVal.add(val);
				}
			}
		}

		// Write the HTML and JavaScript code
		writeAsChartJsHtmlAndJsCode(writer, ChartType.STACKED_BAR, dataTs, seriesDataVal);
	}

	/**
	 * LINE CHART
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeAsChartJsLineChart(Writer writer)
	throws IOException
	{
		// Problem in here might be that the series are not equal in "size/timeSamples" and "dataPoints"
		// The ChartJs object will be created as:
		// ------------------------------------------------------
		// type: 'line', 
		// data: 
		// { 
		//     labels: ['21:01', '21:02', '21:03', '21:04', '21:05', '21:06', '21:07', '21:08', '21:09', ...], 
		//     datasets: [ 
		//         { 
		//             label: 'serie-1', 
		//             data: [1, 2, 3, 4, 5, 6, 7, 8, 9, ...], // where value 1=represents->21:01 
		//             ...
		//         }, { 
		//             label: 'serie-2', 
		//             data: [null, null, null, 4, 5, 6, 7, 8, 9, ...],  // missing dataPoints for: 21:01, 21:02, 21:03
		//             ...
		//         }, { 
		//             label: 'serie-3', 
		//             data: [null, 2, 3, 4, 5, 6, null, null, null, ...],   // missing dataPoints for: 21:01 and 21:07, 21:08, 21:09
		//             ...
		//     ] 
		// }, 
		// ------------------------------------------------------
		// in above example:
		//    - "serie-2" is missing 3 "time-stamps" at the beginning 
		//    - "serie-3" is missing 1 "time-stamps" at the beginning and 3 at the end
		// We need to "adjust" the input to put the "data point" in the correct "time slot"
		// meaning we need to:
		//    - check first/last time entry in each series
		//    - Check that all series is of same size
		// If we have an issue we need to:
		//    - "shuffle" around data a bit
		//
		// Here is what I do, at the end (it's not perfect, but good enough):
		//    - use the "biggest" time-series (as a outer loop)
		//    - then "insert" data-points at "start" for each series until it has the same size as the "biggest" time-series 
		// After the "fix":
		//    - Do post/sanity checking, and if we find issues, just write WARNING messages about it (so we can fix it if cases like that arises)
		// ------------------------------------------------------
		
		Dataset dataset = getDataset();
		if (dataset instanceof TimeSeriesCollection)
		{
			TimeSeriesCollection tsDataset = (TimeSeriesCollection) dataset;

			if (tsDataset.getSeriesCount() <= 0)
			{
				_logger.warn(">>>>>>>>>> writeAsChartJsLineChart(): dataset does NOT have any series. cmName='" + _cmName + "', graphName='" + _graphName + "', graphTitle='" + _graphTitle + "'.");
				return;
			}
			
			// Extract "data" from the "data set" into the below Map's
			LinkedHashMap<String, List<Long>>   seriesDataTs  = new LinkedHashMap<>();
			LinkedHashMap<String, List<Number>> seriesDataVal = new LinkedHashMap<>();
			List<Long> dataTsMaxSerie = null;

			// Loop all series and fill: above Map's
			for (int s=0; s<tsDataset.getSeriesCount(); s++)
			{
				TimeSeries timeSeries = tsDataset.getSeries(s);

				List<Long>   dataTs  = new ArrayList<>();
				List<Number> dataVal = new ArrayList<>();

				seriesDataTs .put(timeSeries.getKey().toString(), dataTs);
				seriesDataVal.put(timeSeries.getKey().toString(), dataVal);

				for (int i=0; i<timeSeries.getItemCount(); i++)
				{
					TimeSeriesDataItem dataItem   = timeSeries.getDataItem(i);
					RegularTimePeriod  dataPeriod = dataItem.getPeriod();
					Number             dataValue  = dataItem.getValue();
					
					dataTs .add( dataPeriod.getFirstMillisecond() );
					dataVal.add( dataValue );
				}

				// Remember the **largest** TimeStamp series, first stage (protecting from null's)
				if (dataTsMaxSerie == null)
					dataTsMaxSerie = dataTs;

				// Remember the **largest** TimeStamp series
				if (dataTs.size() > dataTsMaxSerie.size())
					dataTsMaxSerie = dataTs;
			}

			// If we have "size issues" -- which I'm describing at the "top" of this method
			// - Adjust "data points" position (for now: only padding at start)
			for (Entry<String, List<Number>> entry : seriesDataVal.entrySet())
			{
				String       sName = entry.getKey();
				List<Long>   sTs   = seriesDataTs.get(sName);
				List<Number> sData = entry.getValue();

				// if: current serie has less entries, then "pad" the start with "null" values
				if (sData.size() < dataTsMaxSerie.size())
				{
//					if (_logger.isDebugEnabled())
//						_logger.debug(">>>>>>>>>> writeAsChartJsLineChart(): PADDING >>Start<< with " + (dataTsMaxSerie.size() - sData.size()) + " 'null' entries. cmName='" + _cmName + "', graphName='" + _graphName + "', serieName='" + sName + "', graphTitle='" + _graphTitle + "'.");

					Number defaultDataValue = 0;
					fillMissingEntriesForSerie(sName, dataTsMaxSerie, sTs, sData, defaultDataValue);
//					while(sData.size() < dataTsMaxSerie.size())
//						sData.add(0, null);
				}
			}
			
			// Sanity check (only print warning messages and continue)
			//   - all series should have the same size
			//   - all series "last" TS should be the same
			for (String sName : seriesDataVal.keySet())
			{
				int  xValCnt = dataTsMaxSerie.size();
				long xLastTs = dataTsMaxSerie.get(dataTsMaxSerie.size()-1); // get LAST entry

				int  sValCnt = seriesDataVal.get(sName).size();
				long sLastTs = seriesDataTs .get(sName).get(seriesDataTs.get(sName).size()-1); // get LAST entry

				if (xValCnt != sValCnt)
					_logger.warn(">>>>>>>>>> writeAsChartJsLineChart(): SANITY-CHECK[SIZE] - xValCnt=" + xValCnt + ", sValCnt=" + sValCnt + ". cmName='" + _cmName + "', graphName='" + _graphName + "', serieName='" + sName + "', graphTitle='" + _graphTitle + "'.");

				if (xLastTs != sLastTs)
					_logger.warn(">>>>>>>>>> writeAsChartJsLineChart(): SANITY-CHECK[LAST-TS] - xLastTs=" + xLastTs + ":[" + TimeUtils.toStringIso8601(xLastTs) + "], sLastTs=" + sLastTs + ":[" + TimeUtils.toStringIso8601(sLastTs) + "]. Data points might be placed in the wrong 'slot' at the timeline. cmName='" + _cmName + "', graphName='" + _graphName + "', serieName='" + sName + "', graphTitle='" + _graphTitle + "'.");
			}
			
			// Convert all Time-stamps <Long> into Strings with format "Iso8601" (example: 2022-11-02T20:59:47.040+01:00)
			// For Chart.js it will be used as 'labels', see description at the "top" of this method
			List<String> dataTsStr = new ArrayList<>();
			for (Long ts : dataTsMaxSerie)
				dataTsStr.add( "'" + TimeUtils.toStringIso8601(ts) + "'");

			// Write the HTML and JavaScript code
			writeAsChartJsHtmlAndJsCode(writer, ChartType.LINE, dataTsStr, seriesDataVal);
		}
		else
		{
			String datasetClassName = (dataset == null) ? "" : dataset.getClass().getName();
			_logger.warn(">>>>>>>>>> writeAsChartJsLineChart(): dataset is NOT instanceof 'TimeSeriesCollection', it is '" + datasetClassName + "' . cmName='" + _cmName + "', graphName='" + _graphName + "', graphTitle='" + _graphTitle + "'.");
		}
	}

	/**
	 * Fill a series with data "null" values where the  
	 * @param serieName        Just for debugging
	 * @param templateTsList   The "template" TimeStamp List which we want to use as the "solution" (or how the adjusted lists *should* look like)
	 * @param tsList
	 * @param dataList
	 * @param defaultDataValue What data value to insert on the "missing" TimeStamp position
	 */
	private static void fillMissingEntriesForSerie(String serieName, List<Long> templateTsList, List<Long> tsList, List<Number> dataList, Number defaultDataValue)
	{
		if (templateTsList == null) throw new RuntimeException("fillMissingEntriesForSerie: templateTsList can't be null");
		if (tsList         == null) throw new RuntimeException("fillMissingEntriesForSerie: tsList can't be null");
		if (dataList       == null) throw new RuntimeException("fillMissingEntriesForSerie: dataList can't be null");

		for (int p=0; p<templateTsList.size(); p++)
		{
			Long   templateTs = templateTsList.get(p);
			Long   sTs        = tsList.size() <= p ? Long.MAX_VALUE : tsList.get(p); // if "tsList" is smaller, then assume a HIGHT TimeStamp
//			Number sData      = dataList      .get(p);

			if (templateTs < sTs)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("fillMissingEntriesForSerie: serieName='" + serieName + "', inserting defaultDataValue='" + defaultDataValue + "' at pos=" + p + ". templateTs=" + templateTs + " (" + TimeUtils.toString(templateTs) + "), sTs=" + sTs + " (" + TimeUtils.toString(sTs) + ")");

				tsList  .add(p, templateTs);
				dataList.add(p, defaultDataValue);
			}
		}
		
		if (_logger.isDebugEnabled())
			_logger.debug("fillMissingEntriesForSerie: serieName='" + serieName + "', templateTsList.size=" + templateTsList.size() + ", tsList.size=" + tsList.size() + ", dataList.size=" + dataList.size());
	}


//	public static void main(String[] args)
//	{
//		String       testSerie;
//		List<Long>   templateTsList;
//		List<Long>   tsList;
//		List<Number> dataList;
//
//		// Test-1 -----------------------------------------------
//		testSerie      = "test-1";
//		templateTsList = new ArrayList<>(Arrays.asList(new Long[]{1L, 2L, 3L}));
//		tsList         = new ArrayList<>(Arrays.asList(new Long[]{    2L, 3L}));
//		dataList       = new ArrayList<>(Arrays.asList(new Long[]{    2L, 3L}));
//		
//		fillMissingEntriesForSerie(testSerie, templateTsList, tsList, dataList, null);
//		System.out.println(testSerie + ": templateTsList=" + templateTsList + ", tsList=" + tsList + ", dataList=" + dataList);
//
//		// Test-2 -----------------------------------------------
//		testSerie      = "test-2";
//		templateTsList = new ArrayList<>(Arrays.asList(new Long[]{1L, 2L, 3L}));
//		tsList         = new ArrayList<>(Arrays.asList(new Long[]{    2L}));
//		dataList       = new ArrayList<>(Arrays.asList(new Long[]{    2L}));
//		
//		fillMissingEntriesForSerie(testSerie, templateTsList, tsList, dataList, null);
//		System.out.println(testSerie + ": templateTsList=" + templateTsList + ", tsList=" + tsList + ", dataList=" + dataList);
//
//		// Test-2 -----------------------------------------------
//		testSerie      = "test-3";
//		templateTsList = new ArrayList<>(Arrays.asList(new Long[]{1L, 2L, 3L}));
//		tsList         = new ArrayList<>(Arrays.asList(new Long[]{}));
//		dataList       = new ArrayList<>(Arrays.asList(new Long[]{}));
//		
//		fillMissingEntriesForSerie(testSerie, templateTsList, tsList, dataList, null);
//		System.out.println(testSerie + ": templateTsList=" + templateTsList + ", tsList=" + tsList + ", dataList=" + dataList);
//
//	}

	public void writeAsHtmlInlineImage(Writer writer)
	throws IOException
	{
		if (_chart == null)
		{
			writer.append("<b>Chart object is NULL... in ReportChartObject.toHtmlInlineImage()</b> <br> \n");
			return;
		}

		// If we use AutoClose here, the underlying Writer is also closed...
		// try (Base64OutputStream base64out = new Base64OutputStream(new WriterOutputStream(writer, StandardCharsets.UTF_8), true);)
		try 
		{
			WriterOutputStream wos = new WriterOutputStream(writer, StandardCharsets.UTF_8);
			Base64OutputStream base64out = new Base64OutputStream(wos, true);

			// writeChartAsPNG produces the same size with compression="default" (just using with, height), compression=0 and compression=9
			// So no difference here... 
			int     width       = getImageSizeWidth();
			int     height      = getImageSizeHeight();
			boolean encodeAlpha = false;
			int     compression = 0;
//			String  tagName     = _cmName + "_" + _graphName;
			String  tagName     = _chartId;

			boolean asPng = true;
			if (asPng)
			{
				// Write the HTML Meta data
				writer.append("<img id='img_" + tagName + "' width='" + width + "' height='" + height + "' src='data:image/png;base64,");

//setWriterStartPoint(writer);
				// Write the Base64 representation of the image
//				ChartUtils.writeChartAsPNG(base64out, _chart, width, height, encodeAlpha, compression);
				writeChartAsPNG(base64out, _chart, width, height, encodeAlpha, compression);

//long size1 = getWritenChars(writer);
//System.out.println(">>>>>> CHART SIZE-1: " + size1 + ", compression=" + compression + ", tagName='" + tagName + "'.");

//compression = 9;
//setWriterStartPoint(writer);
//	writeChartAsPNG(base64out, _chart, width, height, encodeAlpha, compression);
//long size2 = getWritenChars(writer);
////System.out.println("    >> CHART SIZE-2: " + size2 + ", compression=" + compression + ", tagName='" + tagName + "'.");
//System.out.println("  -->> CHART DIFF: " + (size2 - size1) + "   (size2[c9]=" + size2 + " - size1[c0]=" + size1 + "), tagName='" + tagName + "'.");


				// Write the HTML end tag
				writer.append("'>");
			}
			else
			{
				// Write the HTML Meta data
				writer.append("<img width='" + width + "' height='" + height + "' src='data:image/jpeg;base64,");
				
				float   jpgQuality  = 0.0f; // A compression quality setting of 0.0 is most genericallyinterpreted as "high compression is important," while a setting of1.0 is most generically interpreted as "high image quality isimportant." 
				ChartRenderingInfo renderInfo = null;
//				ChartRenderingInfo renderInfo = new ChartRenderingInfo();
//				renderInfo.
				
				// Write the Base64 representation of the image
//				ChartUtils.writeChartAsJPEG(base64out, _chart, width, height);
				ChartUtils.writeChartAsJPEG(base64out, jpgQuality, _chart, width, height, renderInfo);

				// Write the HTML end tag
				writer.append("'>");
			}
			
			// We can't close them, because then the underlying Writer will be closed
			base64out.flush();
			wos.flush();
		}
		catch (IOException ex)
		{
			_exception = ex;
			_problem = ex.toString();
		}
	}
//	//------------------------------------
//	// BEGIN: debugging size of written chars/bytes for ...
//	//------------------------------------
//	private long _writerStartPoint = -1;
//	private long setWriterStartPoint(Writer writer)
//	{
//		if (writer instanceof CountingWriter)
//		{
//			CountingWriter cw = (CountingWriter) writer;
//			_writerStartPoint = cw.getCharsWritten();
//			return _writerStartPoint;
//		}
//		_writerStartPoint = -1;
//		return _writerStartPoint;
//	}
//	private long getWritenChars(Writer writer)
//	{
//		if (_writerStartPoint == -1)
//			return -1;
//
//		if (writer instanceof CountingWriter)
//		{
//			CountingWriter cw = (CountingWriter) writer;
//			long nowAt = cw.getCharsWritten();
//			return nowAt - _writerStartPoint;
//		}
//
//		return -1;
//	}
//	//------------------------------------
//	// END: debugging size of written chars/bytes for ...
//	//------------------------------------

	/**
	 * Writes a chart to an output stream in PNG format.
	 *
	 * @param out  the output stream ({@code null} not permitted).
	 * @param chart  the chart ({@code null} not permitted).
	 * @param width  the image width.
	 * @param height  the image height.
	 * @param encodeAlpha  encode alpha?
     * @param compression  the compression level (0-9).
     *
     * @throws IOException if there are any I/O errors.
     */
	public static void writeChartAsPNG(OutputStream out, JFreeChart chart, int width, int height, boolean encodeAlpha, int compression)
	throws IOException 
	{
		ChartUtils.writeChartAsPNG(out, chart, width, height, encodeAlpha, compression);

//		BufferedImage chartImage = chart.createBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB, null);
//        new PngEncoder()
//        	.withBufferedImage(chartImage)
//        	.toStream(out);

//		try( ImageOutputStream imageOut = ImageIO.createImageOutputStream(out) )
//		{
//			BufferedImage      image    = chart.createBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB, null);
//			IIOMetadata        metadata = null;
//			ImageTypeSpecifier type     = ImageTypeSpecifier.createFromRenderedImage(image);
//			ImageWriter        writer   = ImageIO.getImageWriters(type, "png").next();
//
//			ImageWriteParam param = writer.getDefaultWriteParam();
//			if (param.canWriteCompressed()) 
//			{
//				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//				param.setCompressionQuality(0.0f);
//			}
//
//			writer.setOutput(imageOut);
//			writer.write(null, new IIOImage(image, null, metadata), param);
//			writer.dispose();
//		}
//		catch(Exception ex)
//		{
//ex.printStackTrace();
//System.out.println("USING NORMAL 'ChartUtils.writeChartAsPNG' instead!");
//			ChartUtils.writeChartAsPNG(out, chart, width, height, encodeAlpha, compression);
//		}
	}

	//----------------------------------------------------------
	// Below are code for creating Graph/Chart images that can be included in the HTML Report
	//----------------------------------------------------------
}
