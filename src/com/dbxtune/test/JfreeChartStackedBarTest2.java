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
package com.dbxtune.test;

import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class JfreeChartStackedBarTest2 extends ApplicationFrame
{
	private static final long serialVersionUID = 1L;

	public JfreeChartStackedBarTest2(String titel)
	{
		super(titel);

		final XYDataset dataset    = createDataset();
		final JFreeChart      chart      = createChart(dataset);
		final ChartPanel      chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 350));
		setContentPane(chartPanel);
	}

	private XYDataset createDataset()
	{
//		double[][] data = new double[][] { { 210, 300, 320, 265, 299, 200 }, { 200, 304, 201, 201, 340, 300 }, };
//		return DatasetUtilities.createCategoryDataset("Team ", "Match", data);

		TimeTableXYDataset dataset = new TimeTableXYDataset ();

		Timestamp baseTs   = new Timestamp(System.currentTimeMillis());
//		baseTs.setMinutes(0);
//		baseTs.setSeconds(0);
//		baseTs.setNanos(0);
		
		for (int i = 0; i < 6*24; i++)
		{
			Timestamp ts   = new Timestamp( baseTs.getTime() + (i*10*60*1000)); // add one minute for every loop

			for (int j = 1; j <= 5; j++)
			{
//				double    val  = new Double(i);
				double    val  = ThreadLocalRandom.current().nextDouble(1, 30);

				String    grp  = "ZERO";
				if (j % 1 == 0) grp = "ONE";
				if (j % 2 == 0) grp = "TWO";
				if (j % 3 == 0) grp = "Three";
				if (j % 4 == 0) grp = "Four";
				if (j % 5 == 0) grp = "Five";

//				System.out.println("dataset.addValue(val='" + val + "', grp='" + grp + "', ts='" + ts + "')");
//				dataset.addValue(val, grp, ts);
//				dataset.add(new Second(ts), val, grp);
				dataset.add(new Minute(ts), val, grp);
//				dataset.add(new Hour(ts), val, grp);
			}
		}
		
		return dataset;
	}

	private static JFreeChart createStackedBarChart(String title, String domainAxisLabel, String rangeAxisLabel, XYDataset dataset, PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls)
	{

	    DateAxis dateaxis = new DateAxis("Date");
	    dateaxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
	    NumberAxis numberaxis = new NumberAxis("Y");

//	    StackedXYBarRenderer stackedxybarrenderer = new StackedXYBarRenderer(0.10000000000000001D);
	    StackedXYBarRenderer stackedxybarrenderer = new StackedXYBarRenderer();
	    stackedxybarrenderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
//	    stackedxybarrenderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
//	    stackedxybarrenderer.setDefaultItemLabelsVisible(true);
//	    stackedxybarrenderer.setDrawBarOutline(false);
	    stackedxybarrenderer.setShadowVisible(false);
	    stackedxybarrenderer.setBarPainter(new StandardXYBarPainter());

	    XYPlot xyplot = new XYPlot(dataset, dateaxis, numberaxis, stackedxybarrenderer);
	    
	    JFreeChart jfreechart = new JFreeChart("Stacked XY Bar Chart demo 2", xyplot);
	    
	    return jfreechart;

	}
//	private static JFreeChart createStackedBarChart(String title, String domainAxisLabel, String rangeAxisLabel, XYDataset tablexydataset, PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls)
//	{
//        DateAxis dateaxis = new DateAxis("Date");
//        dateaxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
//        dateaxis.setLowerMargin(0.01D);
//        dateaxis.setUpperMargin(0.01D);
//
//        NumberAxis numberaxis = new NumberAxis("Count");
//        numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//        numberaxis.setUpperMargin(0.10000000000000001D);
//
//        StackedXYBarRenderer stackedxybarrenderer = new StackedXYBarRenderer(0.14999999999999999D);
//        stackedxybarrenderer.setDrawBarOutline(false);
//        stackedxybarrenderer.setDefaultItemLabelsVisible(true);
//        stackedxybarrenderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
//        stackedxybarrenderer.setDefaultPositiveItemLabelPosition(
//                new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
//        stackedxybarrenderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator("{0} : {1} = {2}",
//                new SimpleDateFormat("yyyy"), new DecimalFormat("0")));
//
//        XYPlot xyplot = new XYPlot(tablexydataset, dateaxis, numberaxis, stackedxybarrenderer);
//        JFreeChart jfreechart = new JFreeChart("Holes-In-One / Double Eagles", xyplot);
//
//        jfreechart.removeLegend();
//        jfreechart.addSubtitle(new TextTitle("PGA Tour, 1983 to 2003"));
//        TextTitle texttitle = new TextTitle(
//                "http://www.golfdigest.com/majors/masters/index.ssf?/majors/masters/gw20040402albatross.html",
//                new Font("Dialog", 0, 8));
//        jfreechart.addSubtitle(texttitle);
//        jfreechart.setTextAntiAlias(RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
//        LegendTitle legendtitle = new LegendTitle(xyplot);
//        legendtitle.setBackgroundPaint(Color.white);
//        legendtitle.setFrame(new BlockBorder());
//        legendtitle.setPosition(RectangleEdge.BOTTOM);
//        jfreechart.addSubtitle(legendtitle);
//        return jfreechart;
//	}

    private JFreeChart createChart(final XYDataset dataset)
	{

//		final JFreeChart chart = ChartFactory.createStackedBarChart("Stacked Bar Chart ", "", "Score", dataset, PlotOrientation.VERTICAL, true, true, false);
		final JFreeChart chart = createStackedBarChart("Stacked Bar Chart ", "", "Score", dataset, PlotOrientation.VERTICAL, true, true, false);

//		chart.setBackgroundPaint(new Color(249, 231, 236));
//
//		CategoryPlot plot = chart.getCategoryPlot();
////		plot.getRenderer().setSeriesPaint(0, new Color(128, 0, 0));
////		plot.getRenderer().setSeriesPaint(1, new Color(0, 0, 255));
//
//		CategoryAxis axis = plot.getDomainAxis();
//		axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
//		
//        ValueAxis timeAxis = new DateAxis("timeAxisLabel");
//        timeAxis.setLowerMargin(0.02);  // reduce the default margins
//        timeAxis.setUpperMargin(0.02);
//
//        plot = new cat        
////        NumberAxis valueAxis = new NumberAxis("valueAxisLabel");
////        valueAxis.setAutoRangeIncludesZero(false);  // override default
////        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);
        
		return chart;
	}

	public static void main(final String[] args)
	{

		final JfreeChartStackedBarTest2 demo = new JfreeChartStackedBarTest2("Stacked Bar Chart");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}
}
