/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.utils;

import java.awt.BasicStroke;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

public class MovingAverageChart
{
	public static final int DEFAULT_WIDTH  = 500;
	public static final int DEFAULT_HEIGHT = 180;
	
	//---------------------------------------------------
	// BEGIN: Charting Helpers
	//---------------------------------------------------
	public static String getChartAsHtmlImage(String chartTitle, MovingAverageCounter... counterInstances)
	{
		MovingAverageChart chart = new MovingAverageChart(chartTitle, counterInstances);
		return chart.getAsHtmlInlineImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, null);
	}
	
	public static String getChartAsHtmlImage(String chartTitle, int width, int height, String id, MovingAverageCounter... counterInstances)
	{
		MovingAverageChart chart = new MovingAverageChart(chartTitle, counterInstances);
		return chart.getAsHtmlInlineImage(width, height, id);
	}
	//---------------------------------------------------
	// END: Charting Helpers
	//---------------------------------------------------

	private MovingAverageCounter[] _counterInstances;
	
	private JFreeChart _chart;
	private Dataset    _dataset;

//	private String     _chartName;
	private String     _chartTitle;

	public JFreeChart getChart()      { return _chart;      }
	public Dataset    getDataset()    { return _dataset;    }
//	public String     getChartName()  { return _chartName;  }
	public String     getChartTitle() { return _chartTitle; }

	public void setChart     (JFreeChart chart      ) { _chart      = chart;      }
	public void setDataset   (Dataset    dataset    ) { _dataset    = dataset;    }
//	public void setChartName (String     chartName  ) { _chartName  = chartName;  }
	public void setChartTitle(String     chartTitle ) { _chartTitle = chartTitle; }

	public MovingAverageChart(String chartTitle, MovingAverageCounter... counterInstances)
	{
		_chartTitle       = chartTitle;
		_counterInstances = counterInstances;

		create();
	}

	protected void create()
	{
		setDataset(createDataset());
		setChart(createChart());
	}

	protected Dataset createDataset()
	{
		return createDataset(_counterInstances);
	}
	protected Dataset createDataset(MovingAverageCounter... counterInstances)
	{
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		
		// Add all TIME and VALUES for each Counter to a Charting Series
		for (MovingAverageCounter c : counterInstances)
		{
			TimeSeries serie = new TimeSeries(c.getName());

//			for (MovingAverageCounter.Entry mae : c.getValues())
//				serie.add(new FixedMillisecond(mae.getTime()), mae.getValue());
			for (MovingAverageCounter.Entry mae : c.getValues())
				serie.addOrUpdate(new FixedMillisecond(mae.getTime()), mae.getValue());
			
			dataset.addSeries(serie);
		}
		
		return dataset;
	}

	protected JFreeChart createChart()
	{
		return createChart(getDataset(), getChartTitle());
	}

	protected JFreeChart createChart(Dataset dataset, String chartTitle)
	{
		if ( dataset == null )                  throw new RuntimeException("The passed dataset is null");
		if ( ! (dataset instanceof XYDataset) ) throw new RuntimeException("The passed dataset is of type '" + dataset.getClass() + "' it must implement 'XYDataset'.");

		String timeAxisLabel  = null;
		String valueAxisLabel = null;

		JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, timeAxisLabel, valueAxisLabel, (XYDataset)dataset, true, false, false);
		XYPlot plot = (XYPlot) chart.getPlot();
//		plot.setDomainCrosshairVisible(true);
//		plot.setRangeCrosshairVisible(true);

		// Theme - WHITE
//		plot.setBackgroundPaint(Color.WHITE);
//		plot.setDomainGridlinePaint(Color.GRAY);
//		plot.setRangeGridlinePaint(Color.GRAY);

		
		// Set thicker lines
		boolean thickLines = false;
		if (thickLines)
		{
			int seriesCount = plot.getSeriesCount();
			for (int i = 0; i < seriesCount; i++)
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
		}

		// Special Renderer that creates a LegendItem that is square and FILLED
		XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false) 
		{
			private static final long serialVersionUID = 1L;

			@Override
			public LegendItem getLegendItem(int datasetIndex, int series) 
			{
				if (getPlot() == null) 
					return null;

				XYDataset dataset = getPlot().getDataset(datasetIndex);
				if (dataset == null) 
					return null;

				String label = dataset.getSeriesKey(series).toString();
				LegendItem legendItem = new LegendItem(label, lookupSeriesPaint(series));
//				legendItem.setLine(new Rectangle2D.Double( 0.0 , 0.0 , 5.0, 5.0) );  //setLine takes a Shape, not just Lines so you can pass any Shape to it...
				legendItem.setLine(Plot.DEFAULT_LEGEND_ITEM_BOX);
				
				return legendItem;
			}
		};
		plot.setRenderer(renderer);

		// Set format of the time labels
		DateAxis axis = (DateAxis) plot.getDomainAxis();
//		axis.setDateFormatOverride(new SimpleDateFormat("dd-MMM-yyyy HH:mm"));
		axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
		
//		// for Percent Graph: set MAX to 100%
//		if (maxValue > 0)
//		{
//			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//			rangeAxis.setRange(0.0, maxValue);
//		}

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setAutoRange(true);
		rangeAxis.setAutoRangeIncludesZero(true);
		
		return chart;
	}
	
	public String getAsHtmlInlineImage(int chartWidth, int charHeight, String htmlId)
	{
		try
		{
			StringWriter sw = new StringWriter();
			
			writeAsHtmlInlineImage(sw, chartWidth, charHeight, htmlId);

			return sw.toString();
		}
		catch (IOException ex)
		{
			return "Problems Chrating chart, Caught: " + ex;
		}
	}
	
	public void writeAsHtmlInlineImage(Writer writer, int chartWidth, int charHeight, String htmlId)
	throws IOException
	{
		if (_chart == null)
		{
			writer.append("<b>Chart object is NULL... in MovingAvergaeChart.toHtmlInlineImage()</b> <br> \n");
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
			int     width       = chartWidth; //getImageSizeWidth();
			int     height      = charHeight; // getImageSizeHeight();
			boolean encodeAlpha = false;
			int     compression = 0;
//			String  tagName     = _cmName + "_" + _graphName;
			String  tagName     = htmlId; // _chartId;

			boolean asPng = true;
			if (asPng)
			{
				String id = StringUtil.isNullOrBlank(tagName) ? "" : "id='" + tagName + "' ";

				// Write the HTML Meta data
				writer.append("<img " + id + " width='" + width + "' height='" + height + "' src='data:image/png;base64,");
				
				// Write the Base64 representation of the image
				ChartUtils.writeChartAsPNG(base64out, _chart, width, height, encodeAlpha, compression);

				// Write the HTML end tag
				writer.append("'>");
			}
			
			// We can't close them, because then the underlying Writer will be closed
			base64out.flush();
			wos.flush();
		}
		catch (IOException ex)
		{
			throw ex;
		}
	}
}
