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
package com.dbxtune.graph;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPosition;
import org.jfree.chart.axis.CategoryLabelWidthType;
import org.jfree.chart.axis.CategoryTick;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
//import org.jfree.text.TextBlock;
//import org.jfree.ui.RectangleEdge;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.ui.RectangleEdge;

public class CategoryAxisSparselyLabeled
extends CategoryAxis
{
	private static final long serialVersionUID = 1L;

	/** The number of ticks to label. */
//	private final int         labeledTicks;
	private SimpleDateFormat _sdf;

	private boolean[] _hiddenLabels;

//	/**
//	 * Construct an axis without a label.
//	 * 
//	 * @param labeledTicks
//	 *            show only this many labeled ticks
//	 */
//	public SparselyLabeledCategoryAxis(int labeledTicks)
//	{
//		this.labeledTicks = labeledTicks;
//		_sdf = null
//	}

//	/**
//	 * Construct and axis with a label.
//	 * 
//	 * @param labeledTicks
//	 *            show only this many labeled ticks
//	 * @param label
//	 *            the axis label
//	 */
//	public SparselyLabeledCategoryAxis(int labeledTicks, SimpleDateFormat sdf, String label)
//	{
//		super(label);
//		this.labeledTicks = labeledTicks;
//		_sdf = sdf;
//	}
	public CategoryAxisSparselyLabeled(String label)
	{
		super(label);
	}

	@SuppressWarnings("rawtypes")
	private Comparable format(Comparable category)
	{
		if (_sdf != null)
		{
			if ( category instanceof java.util.Date) 
				category = _sdf.format( (java.util.Date)category );
		}

		return category;
	}
	
	public void setDateFormat(SimpleDateFormat sdf)
	{
		_sdf = sdf;
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	public List<CategoryTick> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge)
//	{
//		List<CategoryTick> standardTicks = super.refreshTicks(g2, state, dataArea, edge);
//		if ( standardTicks.isEmpty() )
//		{
//			return standardTicks;
//		}
//		int tickEvery = standardTicks.size() / labeledTicks;
//		if ( tickEvery < 1 )
//		{
//			return standardTicks;
//		}
//
//		// Replace a few labels with blank ones
//		List<CategoryTick> fixedTicks = new ArrayList<CategoryTick>(standardTicks.size());
//
//		// Skip the first tick so your 45degree labels don't fall of the edge
//		CategoryTick tick = standardTicks.get(0);
//		fixedTicks.add(new CategoryTick(format(tick.getCategory()), new TextBlock(), tick.getLabelAnchor(), tick.getRotationAnchor(), tick.getAngle()));
//
//		for (int i = 1; i < standardTicks.size(); i++)
//		{
//			tick = standardTicks.get(i);
//			if ( i % tickEvery == 0 )
//			{
//				Comparable category = tick.getCategory();
////				TextBlock textBlock = TextUtils.createTextBlock(tick.getText(), getTickLabelFont(), getTickLabelPaint());
//				TextBlock textBlock = createLabel(tick.getText(), l * r, edge, g2);
//				TextBlock textBlock = TextUtilities.createTextBlock(category.toString(),
//						getTickLabelFont(category), getTickLabelPaint(category), width,
//						this.maximumCategoryLabelLines, new G2TextMeasurer(g2));
//
//				
//				System.out.println("add(tick): tick.getCategory()="+tick.getCategory().getClass().getSimpleName());
//				fixedTicks.add(new CategoryTick(format(tick.getCategory()), textBlock, tick.getLabelAnchor(), tick.getRotationAnchor(), tick.getAngle()));
////				fixedTicks.add(tick);
//			}
//			else
//			{
//				System.out.println("add(LONG_TICK)");
//				fixedTicks.add(new CategoryTick(format(tick.getCategory()), new TextBlock(), tick.getLabelAnchor(), tick.getRotationAnchor(), tick.getAngle()));
//			}
//		}
//		return fixedTicks;
//	}
	

    public boolean isTickLabelVisible(int categoryIndex)
	{
//System.out.println("_hiddenLabels="+_hiddenLabels);
    	if (_hiddenLabels == null)
    		return true;

//System.out.println("_hiddenLabels["+categoryIndex+"]="+_hiddenLabels[categoryIndex]);
    	
    	return ! _hiddenLabels[categoryIndex];
	}

//	@Override
//	public void drawTickMarks(Graphics2D g2, double cursor, Rectangle2D dataArea, RectangleEdge edge, AxisState state) 
//	{
//		// In here we could probably check if this TackLabel is visible and DRAW a line... 
//		super.drawTickMarks(g2, cursor, dataArea, edge, state);
//	}
	@Override
    public void drawTickMarks(Graphics2D g2, double cursor, Rectangle2D dataArea, RectangleEdge edge, AxisState state) 
	{
        Plot p = getPlot();
        if (p == null) {
            return;
        }
        CategoryPlot plot = (CategoryPlot) p;
        double il = getTickMarkInsideLength();
        double ol = getTickMarkOutsideLength();
        Line2D line = new Line2D.Double();
        List categories = plot.getCategoriesForAxis(this);
        g2.setPaint(getTickMarkPaint());
        g2.setStroke(getTickMarkStroke());
        int categoryIndex = 0;

        if (edge.equals(RectangleEdge.TOP)) {
            Iterator iterator = categories.iterator();
            while (iterator.hasNext()) {
                Comparable key = (Comparable) iterator.next();
                double il2 = il;
                double ol2 = ol;
                if (isTickLabelVisible(categoryIndex))
                	ol2 += 4;

                double x = getCategoryMiddle(key, categories, dataArea, edge);
                line.setLine(x, cursor, x, cursor + il2);
                g2.draw(line);
                line.setLine(x, cursor, x, cursor - ol2);
                g2.draw(line);

                categoryIndex++;
            }
            state.cursorUp(ol);
        }
        else if (edge.equals(RectangleEdge.BOTTOM)) {
            Iterator iterator = categories.iterator();
            while (iterator.hasNext()) {
                Comparable key = (Comparable) iterator.next();
                double il2 = il;
                double ol2 = ol;
                if (isTickLabelVisible(categoryIndex))
                	ol2 += 4;

                double x = getCategoryMiddle(key, categories, dataArea, edge);
                line.setLine(x, cursor, x, cursor - il2);
                g2.draw(line);
                line.setLine(x, cursor, x, cursor + ol2);
                g2.draw(line);

                categoryIndex++;
            }
            state.cursorDown(ol);
        }
        else if (edge.equals(RectangleEdge.LEFT)) {
            Iterator iterator = categories.iterator();
            while (iterator.hasNext()) {
                Comparable key = (Comparable) iterator.next();
                double il2 = il;
                double ol2 = ol;
                if (isTickLabelVisible(categoryIndex))
                	ol2 += 2;

                double y = getCategoryMiddle(key, categories, dataArea, edge);
                line.setLine(cursor, y, cursor + il2, y);
                g2.draw(line);
                line.setLine(cursor, y, cursor - ol2, y);
                g2.draw(line);

                categoryIndex++;
            }
            state.cursorLeft(ol);
        }
        else if (edge.equals(RectangleEdge.RIGHT)) {
            Iterator iterator = categories.iterator();
            while (iterator.hasNext()) {
                double il2 = il;
                double ol2 = ol;
                if (isTickLabelVisible(categoryIndex))
                	ol2 += 2;

                Comparable key = (Comparable) iterator.next();
                double y = getCategoryMiddle(key, categories, dataArea, edge);
                line.setLine(cursor, y, cursor - il2, y);
                g2.draw(line);
                line.setLine(cursor, y, cursor + ol2, y);
                g2.draw(line);

                categoryIndex++;
            }
            state.cursorRight(ol);
        }
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge)
	{

		List ticks = new ArrayList();

		// sanity check for data area...
		if ( dataArea.getHeight() <= 0.0 || dataArea.getWidth() < 0.0 )
		{
			return ticks;
		}

		CategoryPlot plot       = (CategoryPlot) getPlot();
		List         categories = plot.getCategoriesForAxis(this);
		double       max        = 0.0;

		if ( categories != null )
		{
			_hiddenLabels = new boolean[categories.size()];
			
			CategoryLabelPosition position = this.getCategoryLabelPositions().getLabelPosition(edge);
			float                 r        = this.getMaximumCategoryLabelWidthRatio();
			if ( r <= 0.0 )
			{
				r = position.getWidthRatio();
			}

			float l;
			if ( position.getWidthType() == CategoryLabelWidthType.CATEGORY )
			{
				l = (float) calculateCategorySize(categories.size(), dataArea, edge);
			}
			else
			{
				if ( RectangleEdge.isLeftOrRight(edge) )
				{
					l = (float) dataArea.getWidth();
				}
				else
				{
					l = (float) dataArea.getHeight();
				}
			}

			int categoryIndex = 0;
			Iterator iterator = categories.iterator();
			while (iterator.hasNext())
			{
				Comparable category = format((Comparable) iterator.next());

				g2.setFont(getTickLabelFont(category));
				//float labelWidth = l * r;
				float labelWidth  = g2.getFontMetrics().stringWidth(category.toString());
				
//				labelWidth = labelWidth * 1.5f; // Add some extra space to the label to create some spacing

//System.out.println("---------- l="+1+", r="+r+", labelWidth="+labelWidth);

				TextBlock label = createLabel(category, labelWidth, edge, g2);
				if ( edge == RectangleEdge.TOP || edge == RectangleEdge.BOTTOM )
				{
					max = Math.max(max, calculateTextBlockHeight(label, position, g2));
					max = Math.max(max, calculateTextBlockWidth(label, position, g2));
				}
				else if ( edge == RectangleEdge.LEFT || edge == RectangleEdge.RIGHT )
				{
					max = Math.max(max, calculateTextBlockWidth(label, position, g2));
					max = Math.max(max, calculateTextBlockHeight(label, position, g2));
				}

				Tick tick = new CategoryTick(category, label, position.getLabelAnchor(), position.getRotationAnchor(), position.getAngle());
				ticks.add(tick);
				
				categoryIndex = categoryIndex + 1;
			}

			// If the labels do not fit in the area... remove some
//			System.out.println("max=" + max);
			if (dataArea.getWidth() < (max * categories.size()))
			{
				double areaWidth =  dataArea.getWidth();
				
				// How many ticks can we have on this label
				int labeledTicks = (int) ((areaWidth / (max * categories.size())) * 100.0);
				int tickEvery = ticks.size() / labeledTicks;

//				System.out.println("xxx=" + ((areaWidth / (max * categories.size())) * 100.0));
//				System.out.println("labeledTicks=" + labeledTicks + ", tickEvery=" + tickEvery);
//				System.out.println("areaWidth=" + areaWidth);
//				System.out.println("dataArea.getBounds().width="+dataArea.getBounds().width);
//				System.out.println("allLabelWidth=" + (max * categories.size()));
//				System.out.println("dataArea=" + dataArea);
				
				if (tickEvery == 0)
					tickEvery = 1;
					
				List<Tick> tmpTicks = new ArrayList();
				for (int i = 0; i < ticks.size(); i++)
				{
					CategoryTick ct = (CategoryTick) ticks.get(i);
					
//					boolean addVisibleEntry = ( i % tickEvery == 0 );
					boolean addVisibleEntry = ( i % tickEvery == 0 || ((i+1 == ticks.size()) && (i % tickEvery > 3)) );
					if (addVisibleEntry)
					{
//						System.out.println(i + ": add - " + ct.getCategory());
						tmpTicks.add(ct);
						_hiddenLabels[i] = false;
					}
					else
					{
//						System.out.println(i + ": add-emty");
						tmpTicks.add(new CategoryTick(format(ct.getCategory()), new TextBlock(), ct.getLabelAnchor(), ct.getRotationAnchor(), ct.getAngle()));
						_hiddenLabels[i] = true;
					}
				}
				
				ticks = tmpTicks;
			}
		}
		state.setMax(max);
		return ticks;
	}
}













//-------------------------------------------------------------------------------------
// http://jfree.org/forum/viewtopic.php?t=15345
//-------------------------------------------------------------------------------------
///**
// * This class enhances <code>CategoryAxis</code> in that it allows
// * to skip some labels to be printed in the category axis.
// * However, it does not display tooltips on the labels.
// */
//public class CategoryAxisSkipLabels extends CategoryAxis
//{
//  private static final int DEFAULT_INTERVAL = 1;
//  private int m_interval;
//
//  /** Default constructor. */
//  public CategoryAxisSkipLabels()
//  {
//    this(null, DEFAULT_INTERVAL);
//  }
//
//  /**
//   * Constructs an axis with a label.
//   * @param label Axis label (may be null).
//   */
//  public CategoryAxisSkipLabels(String label)
//  {
//    this(label, DEFAULT_INTERVAL);
//  }
//
//  /**
//   * Constructs a category axis with a label and an interval.
//   * @param label Axis label (may be null).
//   * @param interval This number controls the labels to be printed.
//   * For instance, if <code>interval = 1</code>, all labels are printed; if
//   * <code>interval = 10</code>, only one of every 10 labels are printed (first label
//   * is always printed).
//   */
//  public CategoryAxisSkipLabels(String label, int interval)
//  {
//    super(label);
//    m_interval = interval;
//  }
//
//  /**
//   * Draws the category labels and returns the updated axis state.
//   * NOTE: This method redefines the corresponding one in <code>CategoryAxis</code>,
//   * and is a copy of that, with added control to skip some labels to be printed.
//   * 
//   * @param g2 the graphics device (<code>null</code> not permitted).
//   * @param dataArea the area inside the axes (<code>null</code> not
//   *          permitted).
//   * @param edge the axis location (<code>null</code> not permitted).
//   * @param state the axis state (<code>null</code> not permitted).
//   * @param plotState collects information about the plot (<code>null</code>
//   *          permitted).
//   * 
//   * @return The updated axis state (never <code>null</code>).
//   */
//  protected AxisState drawCategoryLabels(Graphics2D g2, Rectangle2D dataArea,
//                                         RectangleEdge edge, AxisState state,
//                                         PlotRenderingInfo plotState)
//  {
//    if (state == null)
//    {
//      throw new IllegalArgumentException("Null 'state' argument.");
//    }
//
//    if (isTickLabelsVisible())
//    {
//      g2.setFont(getTickLabelFont());
//      g2.setPaint(getTickLabelPaint());
//      List ticks = refreshTicks(g2, state, dataArea, edge);
//      state.setTicks(ticks);
//
//      int categoryIndex = 0;
//      Iterator iterator = ticks.iterator();
//      while (iterator.hasNext())
//      {
//        CategoryTick tick = (CategoryTick) iterator.next();
//        g2.setPaint(getTickLabelPaint());
//
//        CategoryLabelPosition position = getCategoryLabelPositions()
//            .getLabelPosition(edge);
//        double x0 = 0.0;
//        double x1 = 0.0;
//        double y0 = 0.0;
//        double y1 = 0.0;
//        if (edge == RectangleEdge.TOP)
//        {
//          x0 = getCategoryStart(categoryIndex, ticks.size(), dataArea, edge);
//          x1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea, edge);
//          y1 = state.getCursor() - getCategoryLabelPositionOffset();
//          y0 = y1 - state.getMax();
//        }
//        else if (edge == RectangleEdge.BOTTOM)
//        {
//          x0 = getCategoryStart(categoryIndex, ticks.size(), dataArea, edge);
//          x1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea, edge);
//          y0 = state.getCursor() + getCategoryLabelPositionOffset();
//          y1 = y0 + state.getMax();
//        }
//        else if (edge == RectangleEdge.LEFT)
//        {
//          y0 = getCategoryStart(categoryIndex, ticks.size(), dataArea, edge);
//          y1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea, edge);
//          x1 = state.getCursor() - getCategoryLabelPositionOffset();
//          x0 = x1 - state.getMax();
//        }
//        else if (edge == RectangleEdge.RIGHT)
//        {
//          y0 = getCategoryStart(categoryIndex, ticks.size(), dataArea, edge);
//          y1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea, edge);
//          x0 = state.getCursor() + getCategoryLabelPositionOffset();
//          x1 = x0 - state.getMax();
//        }
//        Rectangle2D area = new Rectangle2D.Double(x0, y0, (x1 - x0), (y1 - y0));
//        Point2D anchorPoint =
//          RectangleAnchor.coordinates(area, position.getCategoryAnchor());
//
//        // THIS CODE IS NOW CONTROLLED BY THE "IF" =============
//        if (categoryIndex % m_interval == 0)
//        {
//          TextBlock block = tick.getLabel();
//          block.draw(g2, (float) anchorPoint.getX(), (float) anchorPoint.getY(),
//                     position.getLabelAnchor(), (float) anchorPoint.getX(),
//                     (float) anchorPoint.getY(), position.getAngle());
//          Shape bounds = block.calculateBounds(g2, (float) anchorPoint.getX(),
//                                               (float) anchorPoint.getY(),
//                                               position.getLabelAnchor(),
//                                               (float) anchorPoint.getX(),
//                                               (float) anchorPoint.getY(),
//                                               position.getAngle());
//          if (plotState != null)
//          {
//            EntityCollection entities = plotState.getOwner().getEntityCollection();
//            if (entities != null)
//            {
//              //String tooltip = (String) categoryLabelToolTips.get(tick.getCategory());
//              String tooltip = null;
//              entities.add(new TickLabelEntity(bounds, tooltip, null));
//            }
//          }
//        }
//        // END IF ========================================
//
//        categoryIndex++;
//      }
//
//      if (edge.equals(RectangleEdge.TOP))
//      {
//        double h = state.getMax();
//        state.cursorUp(h);
//      }
//      else if (edge.equals(RectangleEdge.BOTTOM))
//      {
//        double h = state.getMax();
//        state.cursorDown(h);
//      }
//      else if (edge == RectangleEdge.LEFT)
//      {
//        double w = state.getMax();
//        state.cursorLeft(w);
//      }
//      else if (edge == RectangleEdge.RIGHT)
//      {
//        double w = state.getMax();
//        state.cursorRight(w);
//      }
//    }
//    return state;
//  }
//}














//-------------------------------------------------------------------------------------
// http://www.jfree.org/forum/viewtopic.php?t=4915
//-------------------------------------------------------------------------------------
//import java.awt.Font;
//import java.awt.Graphics2D;
//import java.awt.Insets;
//import java.awt.font.FontRenderContext;
//import java.awt.font.LineMetrics;
//import java.awt.geom.Rectangle2D;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//import org.jfree.chart.axis.*;
//import org.jfree.chart.plot.CategoryPlot;
//
//public class UniqueLabelsCategoryAxis extends CategoryAxis {
//
//  private int _labelLength;
//
//  public UniqueLabelsCategoryAxis(String label, int labelLength) {
//    super(label);
//    _labelLength = labelLength;
//  }
//
//  /**
//   * Returns only unique labels.
//   *
//   * @param categoryLabel
//   * @param uniqueLabels
//   * @return
//   */
//  public String getUniqueLabel(String categoryLabel, ArrayList uniqueLabels) {
//    String searchLabel = categoryLabel.substring(0, _labelLength);
//    String rv;
//
//    // if this label already exists in the array, return a blank string
//    if (uniqueLabels.contains(searchLabel)) {
//      rv = "";
//    }
//    else {
//      uniqueLabels.add(searchLabel);
//      rv = searchLabel;
//    }
//    return rv;
//  }
//
//  public void refreshTicksHorizontal(Graphics2D g2,
//                                     Rectangle2D plotArea, Rectangle2D dataArea,
//                                     AxisLocation location) {
//    ArrayList uniqueLabels = new ArrayList();
//
//    //this.maxTickLineCount = 1;
//    getTicks().clear();
//    CategoryPlot categoryPlot = (CategoryPlot) getPlot();
//    List categories = categoryPlot.getCategories();
//    if (categories != null) {
//      Font font = getTickLabelFont();
//      g2.setFont(font);
//      FontRenderContext frc = g2.getFontRenderContext();
//      int categorySkip = 0;
//      int categoryIndex = 0;
//      float maxWidth = (float) (dataArea.getWidth() / categories.size() * 0.9f);
//      float xx = 0.0f;
//      float yy = 0.0f;
//      Iterator iterator = categories.iterator();
//      while (iterator.hasNext()) {
//        Object category = iterator.next();
//
//        if (categorySkip != 0) {
//          ++categoryIndex;
//          --categorySkip;
//          continue;
//        }
//
//        //String label = category.toString();
//        String label = getUniqueLabel(category.toString(), uniqueLabels);
//
//        Rectangle2D labelBounds = font.getStringBounds(label, frc);
//        LineMetrics metrics = font.getLineMetrics(label, frc);
//        float catX = (float) getCategoryMiddle(categoryIndex,
//            categories.size(),
//            dataArea, location);
//        if (getVerticalCategoryLabels()) {
//          xx = (float) (catX + labelBounds.getHeight() / 2 - metrics.getDescent());
//          if (location == AxisLocation.TOP) {
//            yy = (float) (dataArea.getMinY() - getTickLabelInsets().bottom);
//            // - labelBounds.getWidth());
//          }
//          else {
//            yy = (float) (dataArea.getMaxY() + getTickLabelInsets().top
//                          + labelBounds.getWidth());
//          }
//          getTicks().add(new Tick(category, label, xx, yy));
//          /*
//          if (this.skipCategoryLabelsToFit) {
//            categorySkip = (int) ((labelBounds.getHeight() - maxWidth / 2)
//                                  / maxWidth) + 1;
//          }
//          */
//        }
//        /*
//        else if (labelBounds.getWidth() > maxWidth) {
//          if (this.skipCategoryLabelsToFit) {
//            xx = (float) (catX - maxWidth / 2);
//            if (location == AxisLocation.TOP) {
//              yy = (float) (dataArea.getMinY() - getTickLabelInsets().bottom
//                            - metrics.getDescent()
//                            - metrics.getLeading());
//            }
//            else {
//              yy = (float) (dataArea.getMaxY() + getTickLabelInsets().top
//                            + metrics.getHeight()
//                            - metrics.getDescent());
//            }
//            getTicks().add(new Tick(category, label, xx, yy));
//            categorySkip = (int) ((labelBounds.getWidth() - maxWidth / 2)
//                                  / maxWidth) + 1;
//          }
//          else {
//            String[] labels = breakLine(label, (int) maxWidth, frc);
//            Tick[] ts = new Tick[labels.length];
//            for (int i = 0; i < labels.length; i++) {
//              labelBounds = font.getStringBounds(labels[i], frc);
//              xx = (float) (catX - labelBounds.getWidth() / 2);
//              if (location == AxisLocation.TOP) {
//                yy = (float) (dataArea.getMinY() - getTickLabelInsets().bottom
//                              - (labels.length - i) * metrics.getHeight()
//                              + metrics.getAscent());
//              }
//              else {
//                yy = (float) (dataArea.getMaxY() + getTickLabelInsets().top
//                              + (i + 1) * (metrics.getHeight())
//                              - metrics.getDescent());
//              }
//              ts[i] = new Tick(category, labels[i], xx, yy);
//            }
//            if (labels.length > this.maxTickLineCount) {
//              this.maxTickLineCount = labels.length;
//            }
//            getTicks().add(ts);
//          }
//        }
//        else {
//          xx = (float) (catX - labelBounds.getWidth() / 2);
//          if (location == AxisLocation.TOP) {
//            yy = (float) (dataArea.getMinY() - getTickLabelInsets().bottom
//                          - metrics.getLeading()
//                          - metrics.getDescent());
//          }
//          else {
//            yy = (float) (dataArea.getMaxY() + getTickLabelInsets().top
//                          + metrics.getHeight()
//                          - metrics.getDescent());
//          }
//          getTicks().add(new Tick(category, label, xx, yy));
//        } */
//        categoryIndex = categoryIndex + 1;
//      }
//    }
//  }
//}
