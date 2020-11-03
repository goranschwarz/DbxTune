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
package com.asetune.graph;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;

public class CategoryPlotSparselyLabeled 
extends CategoryPlot
{
	private static final long serialVersionUID = 1L;

//	public CategoryPlotSparselyLabeled(CategoryDataset dataset, CategoryAxisSparselyLabeled categoryAxis, ValueAxis valueAxis, StackedBarRenderer render)
	public CategoryPlotSparselyLabeled(CategoryDataset dataset, CategoryAxisSparselyLabeled categoryAxis, ValueAxis valueAxis, CategoryItemRenderer render)
	{
		super(dataset, categoryAxis, valueAxis, render);
	}

	/**
	 * Draws the domain gridlines for the plot, if they are visible.
	 *
	 * @param g2  the graphics device.
	 * @param dataArea  the area inside the axes.
	 *
	 * @see #drawRangeGridlines(Graphics2D, Rectangle2D, List)
	 */
	@Override
	protected void drawDomainGridlines(Graphics2D g2, Rectangle2D dataArea) 
	{
		if (!isDomainGridlinesVisible()) 
			return;

		CategoryAnchor anchor = getDomainGridlinePosition();
		RectangleEdge domainAxisEdge = getDomainAxisEdge();
		CategoryDataset dataset = getDataset();
		if (dataset == null) 
			return;

		CategoryAxis axis = getDomainAxis();
		if (axis != null) 
		{
			int columnCount = dataset.getColumnCount();
			for (int c = 0; c < columnCount; c++) 
			{
				double xx = axis.getCategoryJava2DCoordinate(anchor, c, columnCount, dataArea, domainAxisEdge);
				CategoryItemRenderer renderer1 = getRenderer();
				if (renderer1 != null) 
				{
					if (axis instanceof CategoryAxisSparselyLabeled)
					{
						CategoryAxisSparselyLabeled catAxis = (CategoryAxisSparselyLabeled) axis;
						if (catAxis.isTickLabelVisible(c))
							renderer1.drawDomainGridline(g2, this, dataArea, xx);
					}
					else
					{
						renderer1.drawDomainGridline(g2, this, dataArea, xx);
					}
				}
			}
		}
	}
}
