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
package com.asetune.test;

import java.awt.Color;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.asetune.graph.CategoryAxisSparselyLabeled;
import com.asetune.graph.CategoryPlotSparselyLabeled;

public class JfreeChartStackedBarTest1 extends ApplicationFrame
{
	private static final long serialVersionUID = 1L;

	public JfreeChartStackedBarTest1(String titel)
	{
		super(titel);

		final CategoryDataset dataset    = createDataset();
		final JFreeChart      chart      = createChart(dataset);
		final ChartPanel      chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 350));
		setContentPane(chartPanel);
	}

	private CategoryDataset createDataset()
	{
//		double[][] data = new double[][] { { 210, 300, 320, 265, 299, 200 }, { 200, 304, 201, 201, 340, 300 }, };
//		return DatasetUtilities.createCategoryDataset("Team ", "Match", data);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Timestamp baseTs   = new Timestamp(System.currentTimeMillis());
//		baseTs.setMinutes(0);
//		baseTs.setSeconds(0);
//		baseTs.setNanos(0);
		
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		for (int i = 0; i < 6*24; i++)
		{
			Timestamp tsx   = new Timestamp( baseTs.getTime() + (i*10*60*1000)); // add one minute for every loop

//			String ts = sdf.format(tsx);
			Timestamp ts = tsx;
			
			for (int j = 1; j <= 5; j++)
			{
//				double    val  = new Double(i);
				double    val  = ThreadLocalRandom.current().nextDouble(1, 100);

				String    grp  = "ZERO";
				if (j % 1 == 0) grp = "ONE";
				if (j % 2 == 0) grp = "TWO";
				if (j % 3 == 0) grp = "Three";
				if (j % 4 == 0) grp = "Four";
				if (j % 5 == 0) grp = "Five";

				
				System.out.println("dataset.addValue(val='" + val + "', grp='" + grp + "', ts='" + ts + "')");
				dataset.addValue(val, grp, ts);
			}
		}
		
		return dataset;
	}

	
	private static JFreeChart createStackedBarChart(String title, String domainAxisLabel, String rangeAxisLabel, CategoryDataset dataset, PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls)
	{
//		ParamChecks.nullNotPermitted(orientation, "orientation");
//		ChartFactory.createStackedBarChart(title, domainAxisLabel, rangeAxisLabel, dataset, orientation, legend, tooltips, urls)

		CategoryAxisSparselyLabeled       categoryAxis = new CategoryAxisSparselyLabeled(domainAxisLabel);
//		CategoryAxis       categoryAxis = new CategoryAxis(domainAxisLabel);
		ValueAxis          valueAxis    = new NumberAxis(rangeAxisLabel);

		categoryAxis.setDateFormat(new SimpleDateFormat("HH:mm"));
//		categoryAxis.setMaximumCategoryLabelLines(24);
//		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
//		categoryAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 6));
//		categoryAxis.setMaximumCategoryLabelWidthRatio(2.0f);
		
		System.out.println("=================" + dataset.getColumnKey(0));
		System.out.println("=================" + dataset.getRowKeys());
		
//		StackedBarRenderer render = new StackedBarRenderer();
		StackedBarRenderer render = new StackedBarRenderer()
		{
			public LegendItemCollection getLegendItems() 
			{
				LegendItemCollection xxx = super.getLegendItems();
				for (int i = 0; i < xxx.getItemCount(); i++)
				{
					System.out.println("------ getLegendItems() ----- ["+i+"]="+xxx.get(i).getLabel());
				}
				
//				LegendItemCollection zzz = new LegendItemCollection();
//				for (int i = xxx.getItemCount(); i>0; i--)
//				{
//					zzz.add(xxx.get(i-1));
//				}
//
//				for (int i = 0; i < zzz.getItemCount(); i++)
//				{
//					System.out.println("------ getLegendItems(zzz) ----- ["+i+"]="+zzz.get(i).getLabel());
//				}
//				return zzz;
				
				return xxx;
			};
		};
		if ( tooltips )
		{
			render.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
		}
		if ( urls )
		{
			render.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());
		}

//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, render);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, render);

		plot.setOrientation(orientation);
//		plot.setOrientation(PlotOrientation.HORIZONTAL);
		
//		BarRenderer render = (BarRenderer)plot.getRenderer();
		render.setShadowVisible(false);
//		renderer.setDrawBarOutline(true);
		render.setBarPainter(new StandardBarPainter());
		plot.setDomainGridlinesVisible(true);

		plot.setDomainGridlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.WHITE);
		plot.setBackgroundPaint(Color.LIGHT_GRAY);

		
//		plot.setShadowVisible(false);
//		plot.setBarPainter(new StandardXYBarPainter());

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		chart.setBackgroundPaint(Color.WHITE);

System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: render.getLegendItems() = " + render.getLegendItems().getItemCount());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: plot.getLegendItems() = " + plot.getLegendItems().getItemCount());
		
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: render.getColumnCount() = " + render.getColumnCount());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: render.getRowCount()    = " + render.getRowCount());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: plot.getDatasetCount()       = " + plot.getDatasetCount());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: plot.getDomainAxisCount()    = " + plot.getDomainAxisCount());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: categoryAxis.getMaximumCategoryLabelLines()    = " + categoryAxis.getMaximumCategoryLabelLines());
		
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: plot.getRenderer().getLegendItems() == " + plot.getRenderer().getLegendItems().getItemCount() );
//plot.getRenderer().setSeriesItemLabelsVisible(0, false);
//plot.getRenderer().setSeriesItemLabelsVisible(2, false);
//plot.getRenderer().setSeriesItemLabelsVisible(3, false);
//		System.out.println("XXXXXXXXXXXXXXX: categoryAxis.getMaximumCategoryLabelLines() = " + categoryAxis.getMaximumCategoryLabelLines());
//		categoryAxis.setMaximumCategoryLabelLines(10);

		return chart;

	}

    private JFreeChart createChart(final CategoryDataset dataset)
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

		final JfreeChartStackedBarTest1 demo = new JfreeChartStackedBarTest1("Stacked Bar Chart");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}
}
