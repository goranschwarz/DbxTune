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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.Dataset;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

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
	
	private JFreeChart _chart;
	private Dataset    _dataset;

	private String     _problem;
	private Exception  _exception;
	private int        _defaultQueryTimeout = 0; 

	private String     _preComment;
	private String     _postComment;

	
	private ReportEntryAbstract _reportEntry;
	private DbxConnection _conn;
	private ChartType     _chartType;
	private String        _cmName;
	private String        _graphName;
	private String        _graphTitle;
	
	@Override public JFreeChart          getChart()               { return _chart;               }
	@Override public Dataset             getDataset()             { return _dataset;             }
	@Override public ReportEntryAbstract getReportEntry()         { return _reportEntry;         }
	          public DbxConnection       getConnection()          { return _conn;                }
	          public ChartType           getChartType()           { return _chartType;           }
	          public String              getCmName()              { return _cmName;              }
	          public String              getGraphName()           { return _graphName;           }
	@Override public String              getGraphTitle()          { return _graphTitle;          }
	@Override public String              getProblem()             { return _problem;             }
	@Override public Exception           getException()           { return _exception;           }
	@Override public int                 getDefaultQueryTimeout() { return _defaultQueryTimeout; }
	@Override public String              getPreComment()          { return _preComment;          }
	@Override public String              getPostComment()         { return _postComment;         }

	public void setChart              (JFreeChart          chart      ) { _chart               = chart;       }
	public void setDataset            (Dataset             dataset    ) { _dataset             = dataset;     }
	public void setReportEntry        (ReportEntryAbstract reportEntry) { _reportEntry         = reportEntry; }
	public void setConnection         (DbxConnection       conn       ) { _conn                = conn;        }
	public void setChartType          (ChartType           chartType  ) { _chartType           = chartType;   }
	public void setCmName             (String              cmName     ) { _cmName              = cmName;      }
	public void setGraphName          (String              graphName  ) { _graphName           = graphName;   }
	public void setGraphTitle         (String              graphTitle ) { _graphTitle          = graphTitle;  }
	public void setProblem            (String              problem    ) { _problem             = problem;     }
	public void setException          (Exception           ex         ) { _exception           = ex;          }
	public void setDefaultQueryTimeout(int                 seconds    ) { _defaultQueryTimeout = seconds;     }
	public void setPreComment         (String              preComment ) { _preComment          = preComment;  }
	public void setPostComment        (String              postComment) { _postComment         = postComment; }

	public ReportChartAbstract(ReportEntryAbstract reportEntry, DbxConnection conn, ChartType chartType, String cmName, String graphTitle)
	{
		_reportEntry     = reportEntry;
		_conn            = conn;
		_chartType       = chartType;
		_cmName          = cmName;
		_graphTitle      = graphTitle;
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
		writeAsHtmlInlineImage(sb);
		sb.append("<br>\n");

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
			int     width       = 1900;
			int     height      = 300;
			boolean encodeAlpha = false;
			int     compression = 0;

			boolean asPng = true;
			if (asPng)
			{
				// Write the HTML Meta data
				writer.append("<img width='" + width + "' height='" + height + "' src='data:image/png;base64,");
				
				// Write the Base64 representation of the image
				ChartUtils.writeChartAsPNG(base64out, _chart, width, height, encodeAlpha, compression);

				// Write the HTML end tag
				writer.append("'>");
			}
			else
			{
				// Write the HTML Meta data
				writer.append("<img width='" + width + "' height='" + height + "' src='data:image/jpeg;base64,");
				
				// Write the Base64 representation of the image
				ChartUtils.writeChartAsJPEG(base64out, _chart, width, height);

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


	//----------------------------------------------------------
	// Below are code for creating Graph/Chart images that can be included in the HTML Report
	//----------------------------------------------------------
}
