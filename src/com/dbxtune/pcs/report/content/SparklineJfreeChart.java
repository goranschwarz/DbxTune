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
package com.dbxtune.pcs.report.content;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Random;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeListener;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResult;

public class SparklineJfreeChart
{
	private JFreeChart _chart;
	private SparklineResult _data;
	
	public static String create(SparklineResult data)
	{
		SparklineJfreeChart chart = new SparklineJfreeChart(data);
		return chart.toHtmlInlineImage();
	}

	public SparklineJfreeChart(SparklineResult data)
	{
		_data = data;
		init();
//		init_test();
	}

	public void init()
	{
		TimeSeriesCollection dataSet = new TimeSeriesCollection();
		TimeSeries data = new TimeSeries("Sparkline");

		boolean allValuesAreZero = true;
		
		// Add data here
		for (int i=0; i<_data.values.size(); i++)
		{
//			Integer   val = _data.values.get(i);
			Number    val = _data.values.get(i);
			Timestamp bts = _data.beginTs.get(i);
			
			if (val != null && val.doubleValue() != 0d)
				allValuesAreZero = false;
			
			data.add(new Minute(bts), val);
		}

		dataSet.addSeries(data);

		// The sparkline is created by setting a bunch of the visible 
		// properties on the domain, range axis and the XYPlot 
		// to false
		DateAxis x = new DateAxis();
		x.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));
		x.setTickLabelsVisible(false);
		x.setTickMarksVisible(false);
		x.setAxisLineVisible(false);
		x.setNegativeArrowVisible(false);
		x.setPositiveArrowVisible(false);
		x.setVisible(false);
		x.setAxisLinePaint(Color.RED);

		NumberAxis y = new NumberAxis();
		y.setTickLabelsVisible(false);
		y.setTickMarksVisible(false);
		y.setAxisLineVisible(false);
		y.setNegativeArrowVisible(false);
		y.setPositiveArrowVisible(false);
		y.setVisible(false);
		if (allValuesAreZero)
			y.setRange(new Range(0,1)); // So '0' do not end up in the middle of the chart
		
		
		XYPlot plot = new XYPlot();
//		plot.setInsets(new RectangleInsets(-1, -1, 0, 0));
		plot.setInsets(new RectangleInsets(-1, -1, 1, 0)); // Bottom[param:3]=1 -->> to address a MS Office rendering issue 
//		plot.setBackgroundPaint(Color.WHITE);  // This is the background color on the line/chart area (the outside is set on the "_chart" object at the end
		plot.setDataset(dataSet);
		plot.setDomainAxis(x);
		plot.setDomainGridlinesVisible(false);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setRangeAxis(y);
		plot.setRenderer(new StandardXYItemRenderer(StandardXYItemRenderer.LINES));

		// Print the MAX Data value in the "Top left corner"
		plot.addAnnotation(new MaxXYAnnotation(_data.getMaxValue()));

		_chart = new JFreeChart(
				null,
				JFreeChart.DEFAULT_TITLE_FONT,
				plot, false
				);
		_chart.setBorderVisible(false);

		// Background color is normally Gray, when we set inserts[bottom=1], the non filled area gets "gray", so this fixes that
//		_chart.setBackgroundPaint(Color.WHITE); // White DID NOT work... But Yellow / Blue etc works, I dint know what the issue is? 
//		_chart.setBackgroundPaint(CHART_BG_COLOR);
	}
//	public final static Color CHART_BG_COLOR = Color.CYAN; // CYAN becomes "some other" color...
//	public final static Color CHART_BG_COLOR = new Color(254, 254, 254, 0); // (white transparent) WHITE DID NOT WORK, so doing an "almost" white .. this did NOT Work either...
//	public final static Color CHART_BG_COLOR = new Color(254, 254, 254, 255); // (white transparent) WHITE DID NOT WORK, so doing an "almost" white .. this did NOT Work either...
//	public final static Color CHART_BG_COLOR = new Color(0, 0, 0, 0); // (black transparent) WHITE DID NOT WORK, so doing an "almost" white .. this did NOT Work either...

	public void init_test()
	{
		TimeSeriesCollection dataSet = new TimeSeriesCollection();
//		Day day = new Day();
//		TimeSeries data = new TimeSeries("Sparkline", day.getClass());
		TimeSeries data = new TimeSeries("Sparkline");

//		// XXX add real data here
//		Random r = new Random();
//		Calendar c = Calendar.getInstance();
//		for(int i = 0; i < 100; i++) 
//		{
//			int val = r.nextInt(100);
//			if(val < 50)
//				val += 50;
//			c.add(Calendar.DATE, 7);
//			Date date = c.getTime();
//			data.add(new Day(date), val);
//		}
		
//		SimpleDateFormat sdf_HM  = new SimpleDateFormat("HH:mm");
//		SimpleDateFormat sdf_YMD = new SimpleDateFormat("yyyy-MM-dd");
//		String tooltip = sdf_HM.format(bts) + " - " + sdf_HM.format(ets) + " @ [" + sdf_YMD.format(bts) + "]";

		Number maxValue = -99999;
		for (int i=0; i<_data.values.size(); i++)
		{
//			Integer   val = _data.values.get(i);
			Number    val = _data.values.get(i);
			Timestamp bts = _data.beginTs.get(i);
			
			data.add(new Minute(bts), val);
			
			maxValue = Math.max(maxValue.doubleValue(), val.doubleValue());
		}

		dataSet.addSeries(data);

		// The sparkline is created by setting a bunch of the visible 
		// properties on the domain, range axis and the XYPlot 
		// to false
		DateAxis x = new DateAxis();
		x.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));
		x.setTickLabelsVisible(false);
		x.setTickMarksVisible(false);
		x.setAxisLineVisible(false);
		x.setNegativeArrowVisible(false);
		x.setPositiveArrowVisible(false);
		x.setVisible(false);

		NumberAxis y = new NumberAxis();
		y.setTickLabelsVisible(false);
		y.setTickMarksVisible(false);
		y.setAxisLineVisible(false);
		y.setNegativeArrowVisible(false);
		y.setPositiveArrowVisible(false);
		y.setVisible(false);
		
//		y.setTickLabelFont(new Font("Tahoma", Font.PLAIN, 8));
//		y.setTickLabelFont(y.getTickLabelFont().deriveFont(8f));
//		y.setRange(0,33);
//		y.setFixedAutoRange(33d);
//		y.setTickMarkPaint(Color.cyan);
//		y.setLabelPaint(Color.cyan);
//		y.setTickLabelPaint(Color.cyan);
		
//		Range rangeData = y.getRange();
//		rangeData = new Range(rangeData.getLowerBound(),rangeData.getUpperBound());
//		y.setRange(rangeData);

		XYPlot plot = new XYPlot();
		plot.setInsets(new RectangleInsets(-1, -1, 0, 0));
		plot.setDataset(dataSet);
		plot.setDomainAxis(x);
		plot.setDomainGridlinesVisible(false);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setRangeAxis(y);
		plot.setRenderer(new StandardXYItemRenderer(StandardXYItemRenderer.LINES));

////		XYTextAnnotation annotation = new XYTextAnnotation("Hello!", 10.0, 10.0);
//		XYTextAnnotation annotation = new XYTextAnnotation("Hello!", 0.58, 0.52);
//		annotation.setFont(new Font("Tahoma", Font.PLAIN, 8));
////	    XYTitleAnnotation annotation = new XYTitleAnnotation(10d, 10d, new TextTitle("xxxxxxxxxx"));
//		plot.addAnnotation(annotation);

//		TextLine tl = new TextLine("xxxxxxxxxx");
		
		
//	    plot.setBackgroundPaint(Color.lightGray);
//		plot.getRenderer().set

//	    final Marker start = new ValueMarker(3400000.0);
//	    start.setPaint(Color.blue);
//	    start.setLabel("Max Value");
////	    start.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
////	    start.setLabelTextAnchor(TextAnchor.TOP_LEFT);
//	    start.setLabelAnchor(RectangleAnchor.TOP_LEFT);
//	    start.setLabelTextAnchor(TextAnchor.TOP_LEFT);
//	    plot.addRangeMarker(start);

//		LegendTitle lt = new LegendTitle(plot);
//		lt.setItemFont(new Font("Tahoma", Font.PLAIN, 8));
////		lt.setBackgroundPaint(new Color(200, 200, 255, 100));
//		lt.setFrame(new BlockBorder(Color.white));
////		lt.setPosition(RectangleEdge.BOTTOM);
////		XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, lt,RectangleAnchor.BOTTOM_RIGHT);
//		XYTitleAnnotation ta = new XYTitleAnnotation(0.08, 0.90, lt,RectangleAnchor.BOTTOM_RIGHT);
//		plot.addAnnotation(ta);

//		TextTitle tt = new TextTitle("This is\na multiline text\nto demonstrate the wrapping");
//		tt.setTextAlignment(HorizontalAlignment.LEFT);
//		AreaTitleAnnotation ata = new AreaTitleAnnotation(tt);
//		XYTextAnnotation
//		ata.setAnnotationInsets(new RectangleInsets(10, 10, 10, 10));
//		ata.setAnnotationAnchor(RectangleAnchor.TOP_LEFT);
//		plot.addAnnotation(ata);		

//		XYTextAnnotation an = new XYTextAnnotation("Hello!", 0.58, 0.52);
//		plot.addAnnotation(an);

		plot.addAnnotation(new MaxXYAnnotation(maxValue));
		
		
		_chart = new JFreeChart(
				null,
				JFreeChart.DEFAULT_TITLE_FONT,
				plot, false
				);
		_chart.setBorderVisible(false);

//		TextTitle chartinfo = new TextTitle( "Chart Info" );
//		chartinfo.setPosition( TextTitle.BOTTOM ); // position the title below X axis
//		chartinfo.setHorizontalAlignment( TextTitle.LEFT ); // set the horizontal alignment
//		_chart.addSubtitle( chartinfo ); // Here chart is the instance of JFreeChart
		
		
//		try {
//			ChartUtils.saveChartAsPNG(
//					new File("c:\\tmp\\sparkline.png"),
//					_chart,
//					100,
//					30
//					);
//		} catch(IOException e) {
//			System.err.println("Failed to render chart as png: " + e.getMessage());
//			e.printStackTrace();
//		}
	}

	/**
	 * Simple Annotation to Draw a MAX value on the TOP LEFT of the chart
	 */
	private class MaxXYAnnotation extends XYTextAnnotation
	{
		private static final long serialVersionUID = 1L;

		private Number _maxNumber;
		private String _str;
		private Font _font = new Font("Tahoma", Font.PLAIN, 9);

		public MaxXYAnnotation(Number maxNumber)
		{
			super("dummy", 0, 0);
			_maxNumber = maxNumber;
			if (_maxNumber == null)
				_str = "Max: -";
			else
				_str = "Max: " + NumberFormat.getInstance().format(_maxNumber);
		}
		
		@Override
		public void removeChangeListener(AnnotationChangeListener listener)
		{
		}
		
		@Override
		public void addChangeListener(AnnotationChangeListener listener)
		{
		}
		
		@Override
		public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis, int rendererIndex, PlotRenderingInfo info)
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setFont(_font);
			g2.setColor(Color.DARK_GRAY);
			g2.drawString(_str, 2, 7);
		}
	};
	
	public String toHtmlInlineImage()
	{
		if (_chart == null)
		{
			return "<b>Chart object is NULL... in SparklineJfreeChart.toHtmlInlineImage()</b> <br> \n";
		}
		
		try
		{
//			OutputStream out = new FileOutputStream("c:/tmp/xxx.png");
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			Base64OutputStream b64o = new Base64OutputStream(bo, true);

			// writeChartAsPNG produces the same size with compression="default" (just using with, height), compression=0 and compression=9
			// So no difference here... 
			int     width         = 350;
//			int     height        = 18; //30;
			int     height        = 19; //30;
//			int     paddingBottom = 2;
			boolean encodeAlpha   = false;
			int     compression   = 0;
//			ChartUtils.writeChartAsPNG(b64o, _chart, 2048, 300);
//			ChartUtils.writeChartAsPNG(b64o, _chart, width, height, encodeAlpha, compression);
			ReportChartAbstract.writeChartAsPNG(b64o, _chart, width, height, encodeAlpha, compression);
//			ChartUtils.writeChartAsJPEG(b64o, _chart, width, height);

			String base64Str = new String(bo.toByteArray());

			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/png;base64," + base64Str + "'>";
//			String htmlStr = "<img width='" + width + "' height='" + (height+paddingBottom) + "' style='padding-bottom:" + paddingBottom + "px' src='data:image/png;base64," + base64Str + "'>"; // // padding-bottom:1px -->> to address a MS Office rendering issue
			
//			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/jpeg;base64," + base64Str + "'>";
			return htmlStr;
		}
		catch (IOException ex)
		{
//			_exception = ex;
//			_problem = ex.toString();

			return ex.toString();
		}
	}
	
	
	private static class DummyWindow 
	extends ApplicationFrame
	{
		private static final long serialVersionUID = 1L;

		public DummyWindow(String title, SparklineJfreeChart chart)
		{
			super(title);

			ChartPanel panel = new ChartPanel(chart._chart);
			panel.setFillZoomRectangle(true);
			panel.setMouseWheelEnabled(true);
			
			panel.setPreferredSize(new java.awt.Dimension(500, 270));
			setContentPane(panel);
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args)
	{
		
		// save to a file in PNG format
		try
		{
			Random r = new Random();
			
			SparklineResult data = new SparklineResult();
			for (int i = 0; i < 100; i++)
			{
				data.beginTs.add(new Timestamp(2020, 11, 24, 13, i, 0, 0));
				
				int val = i;
				val = r.nextInt(50);
				data.values .add(val);
			}
			
			
			SparklineJfreeChart chart = new SparklineJfreeChart(data);
			//return chart.toHtmlInlineImage();

			boolean doWindow = false;
			boolean doFile   = true;

			if (doWindow)
			{
				DummyWindow dummyWindow = new DummyWindow("Dummy Window", chart);
				dummyWindow.pack();
				RefineryUtilities.centerFrameOnScreen(dummyWindow);
				dummyWindow.setVisible(true);
			}

			if (doFile)
			{
				File outFile = new File("c:\\tmp\\Sparky.png");
				ChartUtils.saveChartAsPNG(outFile, chart._chart, 350, 18);

				if ( Desktop.isDesktopSupported() )
				{
					Desktop desktop = Desktop.getDesktop();
					if ( desktop.isSupported(Desktop.Action.BROWSE) )
					{
						try
						{
							desktop.browse(outFile.toURI());
						}
						catch (Exception ex)
						{
							System.out.println("Problems Open the file in a Web Browser. URL '" + outFile.toURI() + "'.");
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
}
