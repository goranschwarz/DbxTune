/**
 */

package com.asetune.gui;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.IAxis;
import info.monitorenter.gui.chart.IPointPainter;
import info.monitorenter.gui.chart.IToolTipType;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.ITracePoint2D;
import info.monitorenter.gui.chart.TracePoint2D;
import info.monitorenter.gui.chart.ZoomableChart;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterDate;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterSimple;
import info.monitorenter.gui.chart.pointpainters.APointPainter;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyMinimumViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.gui.chart.traces.painters.ATracePainter;
import info.monitorenter.gui.chart.views.ChartPanel;
import info.monitorenter.util.Range;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.log4j.Logger;

import com.asetune.GetCounters;
import com.asetune.TrendGraphDataPoint;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;





public class TrendGraph
implements ActionListener, MouseListener
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(TrendGraph.class);

	public static final int TYPE_BY_COL = CountersModel.GRAPH_TYPE_BY_COL;
	public static final int TYPE_BY_ROW = CountersModel.GRAPH_TYPE_BY_ROW;

	private static final int PANEL_MIN_HEIGHT_DEFAULT = 113;
	private static final int PANEL_MIN_HEIGHT_NOT_USED = -1;

	private int _udGraphType = TYPE_BY_COL;
	private int _colorPtr = 0;

	// private boolean initialized=false;
	private ITrace2D[]         _series;
	private ITrace2D           _dummySeries           = null;    // Used to avoid NPE when not connected
	private ChartPanel         _chartPanel;
	private String             _graphName;
	private String             _labelName;
	private String[]           _seriesNames	          = null;
	private String[]           _dataColNames          = {};
	private String[]           _dataMethods           = {};
	private Chart2D            _chart;
	private boolean	           _pctGraph;
	private java.util.Date	   _oldTs	              = null;
	private static int         _chartMaxSaveTimeInSec = 10*60;   // number of seconds saved in the chart
	private JCheckBoxMenuItem  _chkboxMenuItem        = null;
	private JPanel             _panel                 = null;
	private JPopupMenu         _panelPopupMenu        = null;
	private int                _panelMinHeight        = PANEL_MIN_HEIGHT_NOT_USED;
	private CountersModel      _cm                    = null;
	private boolean            _initialVisible        = true;
	SimpleDateFormat           _sdf                   = new SimpleDateFormat("H:mm:ss.SSS");

	// The name of the graph, jachrt2d not good enough.
	private Watermark          _watermark            = null;
	
	// TimeLine stuff
	private MyTracePainter     _tracePainter         = null;
//	private TimeLineWriter     _timeLineWriter       = null;
	private MyTracePoint2D     _currentTimeLinePoint = null;
	private JPanel             _labelPanel           = null; // fetched & guessed during initialization

	/** What is the last point we have requested a tooltip for */
	private ITracePoint2D _lastToolTipPoint = null;

	static
	{
//		_logger.setLevel(Level.TRACE);
	}

	/**
	 * Create a Trend Graph
	 * @param name        The name of the graph, keep this short without any spaces
	 * @param chkboxText  The menu JCheckBox button text
	 * @param label       The label on the displayed graph
	 * @param seriesNames a Array of name series, this is the text displayed at the bottom of the graph 
	 * @param pct         If this a percent graph (max 100 values in here)
	 * @param cm          The CounterModel that this graph is using 
	 * @param initialVisible If the graph should be visible in the summary at "initial" startup, when properties has been saved for the graph, the initial visible state will be reflected from that. 
	 * @param panelMinHeight Initial minimum height of the chart panel, when properties has been saved for the graph, the initial minimum height will be reflected from that.
	 */
	public TrendGraph(String name, String chkboxText, String label, String[] seriesNames, boolean pct, CountersModel cm, boolean initialVisible, int panelMinHeight)
	{
		_graphName      = name;
		_labelName      = label;
		_seriesNames    = seriesNames;
		_cm             = cm;
		_initialVisible = initialVisible;
		_panelMinHeight = panelMinHeight;

		Configuration conf = Configuration.getCombinedConfiguration();
		
		boolean showToolTip = conf.getBooleanProperty("TrendGraph.showToolTip", false);
		
		_pctGraph = pct;
//		_chart = new Chart2D();
		_chart = new ZoomableChart();
		_series = new ITrace2D[1];
		_dummySeries = new Trace2DLtd(1) ;

		_tracePainter   = new MyTracePainter();


		/*
		** Use the feature below when it is available. Current version 3.10 does not have it
		** but it is in the repository of jChart2D.
		*/
		_chart.setUseAntialiasing(true);
		
		_chart.getAxisX().setFormatter(new LabelFormatterDate((SimpleDateFormat) DateFormat.getTimeInstance()));
		_chart.getAxisY().setFormatter(new LabelFormatterSimple());
		_chart.getAxisX().getAxisTitle().setTitle(null);
		_chart.getAxisY().getAxisTitle().setTitle(null);
		_chart.getAxisX().setPaintGrid(true);
		_chart.getAxisY().setPaintGrid(true);
		
		// Add a mouse listener...
		_chart.addMouseListener(this);

		_chartPanel = new ChartPanel(_chart);
		_watermark = new Watermark(_chartPanel, _labelName);

		_logger.debug("showToolTip = " + showToolTip); 
		if(showToolTip)
		{
//			_chart.setToolTipType(Chart2D.ToolTipType.VALUE_SNAP_TO_TRACEPOINTS);
			_chart.setToolTipType(new MyTooltip());
			_chart.enablePointHighlighting(true);
		}

		if (chkboxText != null  &&  !chkboxText.equals(""))
		{
//			_chkboxMenuItem = new JCheckBoxMenuItem(chkboxText, _initialVisible);
			_chkboxMenuItem = new JCheckBoxMenuItem(chkboxText, true);
			_chkboxMenuItem.addActionListener( this );
		}

		if (_pctGraph)
		{
			//Seem to be a small bug in jChart2D when adding Border and traceName, some space vanishes.
			//Increase by 20% in height.
			_chart.getAxisY().setRangePolicy(new RangePolicyFixedViewport(new Range(0,100)));
//			_chart.getAxisY().setMajorTickSpacing(20);
//			_chart.getAxisY().setMinorTickSpacing(20);
		}
		else
		{
			_chart.getAxisY().setRangePolicy(new RangePolicyMinimumViewport(new Range(0,1)));
		}
		
		//Add dummy datapoint
		_dummySeries.setName(".");
//		_dummySeries.addTracePainter(_tracePainter);
		_chart.addTrace(_dummySeries);
		_dummySeries.addPoint(new MyTracePoint2D( (new java.util.Date()).getTime() , 0.0));
		
		// load some saved properties.
		loadProps();
	}
	
	//------------------------------------------
	// BEGIN: implementing MouseListener
	//------------------------------------------
	@Override public void mouseEntered (MouseEvent e) 
	{
		_lastToolTipPoint = null;
		_chart.setRequestedRepaint(true);
	}
	@Override public void mouseExited  (MouseEvent e) 
	{
		_lastToolTipPoint = null;
		_chart.setRequestedRepaint(true);
	}
	@Override public void mousePressed (MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseClicked (MouseEvent e)
	{
		// if is RIGHT CLICK
		if ( SwingUtilities.isRightMouseButton(e) )
		{
			if (e.getClickCount() == 1)
			{
				_logger.debug("SINGLE-RIGHT-CLICK");
			} // end: single-click
		} // end: right-click
		else if ( SwingUtilities.isLeftMouseButton(e) )
		{ // begin LEFT-CLICK

			// SINGLE click
			if (e.getClickCount() == 1)
			{
			}
			else // DOUBLE click
			{
				  ITracePoint2D tp = _chart.translateMousePosition(e);
				  MainFrame.getInstance().setTimeLinePoint((long)tp.getX());
			} // end: double-click
		} // end: left-click
	}
	//------------------------------------------
	// END: implementing MouseListener
	//------------------------------------------

	/** Get the short name of the ThrendGraph */
	public String getName()  { return _graphName; }

	/** Get the label name of the ThrendGraph, this is what is displayed at the top of the Graph */
	public String getLabel() { return _labelName; }

	/** Set the label name of the ThrendGraph, this is what is displayed at the top of the Graph */
	public void setLabel(String label)
	{
		_labelName = label;
		_watermark.setWatermarkText(_labelName);
	}
	
	private Color nextColor()
	{ 
		if( (_colorPtr % TrendGraphColors._colors.length)==0 ) 
			_colorPtr = 0; 
		return TrendGraphColors._colors[_colorPtr++];
	}
	
	/** For how long do we save/display data in the graph */
	public static int getChartMaxHistoryTimeInMinutes()
	{
		return _chartMaxSaveTimeInSec / 60;
	}

	/** 
	 * Set where in the graph a "vertical bar" should be displayed 
	 */
	public void setTimeLineMarker(Timestamp time)
	{
		if (time == null)
			setTimeLineMarker(0);
		else
			setTimeLineMarker(time.getTime());
	}

	/** 
	 * Set where in the graph a "vertical bar" should be displayed 
	 */
	public void setTimeLineMarker(long time)
	{
		_logger.trace("setTimeLineMarker(time="+time+"): name='"+_graphName+"'.");

		if (_currentTimeLinePoint != null)
		{
			_currentTimeLinePoint.setTimeLineMarked(false);
		}

		_currentTimeLinePoint = null;

		// This was a reset
		if (time == 0)
			return;

		if (_series.length == 0)
			return;

		ITrace2D s = _series[0];
		if (s != null)
		{
			MyTracePoint2D tpSmaller = null;
			MyTracePoint2D tpBigger  = null;
			for (Iterator<ITracePoint2D> it = s.iterator(); it.hasNext();)
			{
				MyTracePoint2D p = (MyTracePoint2D) it.next();
				double graphPoint = p.getX();
				if( graphPoint == time )
				{
					_currentTimeLinePoint = p;
					break;
				}
				else
				{
					if( graphPoint < time )
						tpSmaller = p;
					else if (tpBigger == null)
						tpBigger = p;
				}
			}
			// Ok, we did not find it, but choose the nearest one
			if (_currentTimeLinePoint == null)
			{
				if (tpSmaller != null && tpBigger != null)
				{
					_currentTimeLinePoint = tpSmaller;
					if (_logger.isTraceEnabled())
						_logger.trace("TrendGraph='"+_labelName+"', no '"+time+"' was found, Choosing a SMALLER timeLinePoint");
				}
			}
		}
		if (_currentTimeLinePoint != null)
		{
			_logger.trace("TrendGraph='"+_labelName+"', setTimeLineMarked(true)");
			_currentTimeLinePoint.setTimeLineMarked(true);
			//_series[0].firePointChanged(_currentTimeLinePoint,  TracePoint2D.STATE_CHANGED);
		}
		else
		{
			_logger.trace("TrendGraph='"+_labelName+"', did not find point "+time);
		}
	}

	/**
	 * Set how many minutes the chart should contain.
	 * 
	 * @param minutes
	 */
	public void setChartMaxHistoryTimeInMinutes(int minutes)
	{
		_chartMaxSaveTimeInSec = minutes * 60;
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int chartMaxSamples = (_chartMaxSaveTimeInSec/refreshIntervalInSec);
		
//		_logger.debug("refreshIntervalInSec= " + refreshIntervalInSec 
//				+ " chartMaxSaveTimeInSec=" + chartMaxSaveTimeInSec 
//				+ " chartMaxSamples=" + chartMaxSamples);
		if (_series != null)
		{
			for (int i = 0; i < _series.length; i++)
			{
				// Check if its initialized
				if (_series[i] != null)
				{
					((Trace2DLtd)_series[i]).setMaxSize(chartMaxSamples);
					
					// Initialize the uninitialized part of the graph if we increase the history size.
					java.util.Date starttime;
					if(_series[i].getMinX() != 0)
					{
						starttime = new java.util.Date((long) _series[i].getMaxX());
					}
					else
					{
						starttime = new java.util.Date();	
						_logger.warn("Using local system time as starttime.");
					}
					
					if (_series[i].getSize() < chartMaxSamples)
					{
						ITrace2D prevTrace = _series[i];
						ITrace2D newTrace = new Trace2DLtd(chartMaxSamples);
						_series[i] = newTrace;

						_chart.removeTrace(prevTrace);
						_chart.addTrace(newTrace);
						
						newTrace.setColor(prevTrace.getColor());
						newTrace.setName( prevTrace.getName());
						newTrace.setZIndex(prevTrace.getZIndex());
						newTrace.addTracePainter(_tracePainter);

						long pointTime = 0;
						Iterator<ITracePoint2D> iter = prevTrace.iterator();
						int cnt = chartMaxSamples;
						boolean firstTP = true;
						while(true)
						{
							pointTime = starttime.getTime() - (refreshIntervalInSec * cnt * 1000);
							if( pointTime >= ((long)prevTrace.getMinX()) )
							{
								if(iter.hasNext())
								{
//									MyTracePoint2D tp = (MyTracePoint2D) iter.next();
									ITracePoint2D tp = iter.next();
									
//									if( _logger.isDebugEnabled() && firstTP )
//									{
//										_logger.debug(series[i].getName() + "-Series[" + i + "] " 
//												+ "First TP:" + sdf.format(new java.util.Date(pointTime)) + " Cnt:" + cnt);
//									}
									
									if( _logger.isDebugEnabled() && firstTP && (long)prevTrace.getMinX() != (long)tp.getX() )
									{
										_logger.debug(_series[i].getName() + "-Series[" + i + "] DiffTimeSec=" 
												+ ((long)tp.getX() - (long)prevTrace.getMinX())/1000
												+ " MinX != tp.getX()"
												+ " PointTime:" + _sdf.format(new java.util.Date(pointTime)) + " Cnt:" + cnt);
									}
									firstTP = false;
									newTrace.addPoint(tp);
								}
								else
								{
									break;
								}
							}
							else
							{
								//_logger.debug(series[i].getName() + "-Series[" + i + "]" + " PointTime:" + sdf.format(new java.util.Date(pointTime)) + " Cnt:" + cnt);
								newTrace.addPoint(new MyTracePoint2D( pointTime, 0.0));
							}
							cnt--;
						}
					}

					if( _logger.isDebugEnabled() && _series[i].getMaxSize() != _series[i].getSize() )
						_logger.debug(_series[i].getName() + "-Series[" + i + "] maxSize=" + _series[i].getMaxSize() 
								+ " size=" + _series[i].getSize());
					
					//Not sure if the RingBuffer throws more than I expect. Therefore the hysteresis of refreshIntervalInSec * 1
					if( _logger.isDebugEnabled() 
							&& Math.abs(((long)_series[i].getMaxX()-(long)_series[i].getMinX())/1000 - _chartMaxSaveTimeInSec) > refreshIntervalInSec  )
					{
						_logger.debug(_series[i].getName() + "-Series[" + i + "] " 
							+ "Incorrect timespan:" + ((long)_series[i].getMaxX()-(long)_series[i].getMinX())/1000 + " "
							+ "chartMaxSaveTimeInSec:" + _chartMaxSaveTimeInSec + " "
							+ "PrevMaxX:" + _sdf.format(starttime) + " "
							+ "Started at:" + _sdf.format(new java.util.Date(starttime.getTime() - (refreshIntervalInSec * (chartMaxSamples) * 1000))) + " "
							+ "Min:" + _sdf.format(new java.util.Date((long)_series[i].getMinX())) + " "
							+ "Max:" + _sdf.format(new java.util.Date((long)_series[i].getMaxX())) 
							);
					}
				}
			}
		}
	}

	public ChartPanel getChartPanel()
	{
		return _chartPanel;
	}

	public String[] getDataColNames()
	{
		return _dataColNames;
	}
	public String[] getDataMethods()
	{
		return _dataMethods;
	}
	public String[] getDataLabels()
	{
		return _seriesNames;
	}
	public void setGraphCalculations(String[] dataCols, String[] dataOpers)
	{
		_dataColNames = dataCols;
		_dataMethods  = dataOpers;
	}

	/**  TYPE_BY_COL or TYPE_BY_ROW */
	public int getGraphType()
	{
		return _udGraphType;
	}

	/**  TYPE_BY_COL or TYPE_BY_ROW */
	public void setGraphType(int type)
	{
		if (type != TYPE_BY_COL && type != TYPE_BY_ROW)
		{
			throw new IllegalArgumentException("Graph type can only be 'TYPE_BY_COL/byCol' or 'TYPE_BY_ROW/byRow'.");
		}
		_udGraphType = type;
	}

	/** Add graph data */
	public void addPoint(TrendGraphDataPoint tgdp)
	{
		if (tgdp == null)
			return;

		addPoint(tgdp.getDate(), tgdp.getData(), tgdp.getLabel());
	}

//	/** Add graph data
//	 * @deprecated This is now done in CountersModel
//	 */
//	public void addPoint()
//	{
//		if ( _dataColNames.length == 0)
//			return;
//
//		if ( isGraphEnabled() && _cm != null)
//		{
//			if (_udGraphType == TYPE_BY_COL)
//			{
//				Double[] dataArray = new Double[_dataColNames.length];
//				for (int i=0; i<_dataColNames.length; i++)
//				{
//					String colName = _dataColNames[i];
//					String op      = _dataMethods[i];
//					Double data    = null;
//	
//					if      (op.equals("rateMax"))       data = _cm.getRateValueMax(colName);
//					else if (op.equals("rateMin"))       data = _cm.getRateValueMin(colName);
//					else if (op.equals("rateAvg"))       data = _cm.getRateValueAvg(colName);
//					else if (op.equals("rateAvgGtZero")) data = _cm.getRateValueAvgGtZero(colName);
//					else if (op.equals("rateSum"))       data = _cm.getRateValueSum(colName);
//	
//					else if (op.equals("diffMax"))       data = _cm.getDiffValueMax(colName);
//					else if (op.equals("diffMin"))       data = _cm.getDiffValueMin(colName);
//					else if (op.equals("diffAvg"))       data = _cm.getDiffValueAvg(colName);
//					else if (op.equals("diffAvgGtZero")) data = _cm.getDiffValueAvgGtZero(colName);
//					else if (op.equals("diffSum"))       data = _cm.getDiffValueSum(colName);
//	
//					else if (op.equals("absMax"))        data = _cm.getAbsValueMax(colName);
//					else if (op.equals("absMin"))        data = _cm.getAbsValueMin(colName);
//					else if (op.equals("absAvg"))        data = _cm.getAbsValueAvg(colName);
//					else if (op.equals("absAvgGtZero"))  data = _cm.getAbsValueAvgGtZero(colName);
//					else if (op.equals("absSum"))        data = _cm.getAbsValueSum(colName);
//					else
//					{
//						_logger.warn("Graph named '"+_graphName+"' has unknown operator '"+op+"' for column '"+colName+"'.");
//					}
//	
//					dataArray[i] = data;
//				}
//				if (_logger.isDebugEnabled())
//				{
//					String debugStr = "Graph named '" + _graphName + "', type 'byCol' add data: ";
//					for (int i=0; i<_dataColNames.length; i++)
//					{
//						debugStr += _dataColNames[i] + "='" + dataArray[i] + "', ";
//					}
//					_logger.debug(debugStr);
//				}
//	
//				addPoint((java.util.Date) _cm.getTimestamp(), dataArray);
//			}
//			else if (_udGraphType == TYPE_BY_ROW)
//			{
//				String[] labelArray = new String[_cm.getRowCount()];
//				Double[] dataArray  = new Double[_cm.getRowCount()];
//
//				int labelPos = _cm.findColumn(_seriesNames[0]);
//				
//				for (int i=0; i<_cm.getRowCount(); i++)
//				{
//					Object labelObj = null;
//					String colName  = _dataColNames[0];
//					String op       = _dataMethods[0];
//					Double data     = null;
//
//					// Compose a LABEL
//					if (labelPos >= 0)
//						labelObj = _cm.getValueAt(i, labelPos);
//					else
//					{
//						if      (op.equals("rateVal")) labelObj = _cm.getRatePkValue(i);
//						else if (op.equals("diffVal")) labelObj = _cm.getDiffPkValue(i);
//						else if (op.equals("absVal"))  labelObj = _cm.getAbsPkValue(i);
//					}
//					if (labelObj == null)
//						labelObj = "row-"+i;
//
//					// Get data
//					if      (op.equals("rateVal")) data = _cm.getRateValueAsDouble(i, colName);
//					else if (op.equals("diffVal")) data = _cm.getDiffValueAsDouble(i, colName);
//					else if (op.equals("absVal"))  data = _cm.getAbsValueAsDouble(i,  colName);
//					else
//					{
//						_logger.warn("Graph named '"+_graphName+"' has unknown operator '"+op+"' for column '"+colName+"'.");
//					}
//
//					labelArray[i] = labelObj.toString();
//					dataArray[i]  = data;
//				}
//
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("Graph named '" + _graphName + "', type 'byRow' add data: ");;
//					for (int i=0; i<dataArray.length; i++)
//					{
//						_logger.debug(" :::: row="+i+", data='"+dataArray[i]+"', label='"+labelArray[i]+"'.");
//					}
//				}
//
//				addPoint((java.util.Date) _cm.getTimestamp(), dataArray, labelArray);
//			}
//			else
//			{
//				_logger.warn("Unknown graph type("+_udGraphType+").");
//			}
//		} // end: isGraphEnabled()
//	} // end: method

//	public void addPoint(java.util.Date s, Double val)
//	{
//		MyTracePoint2D tp;
//		int refreshIntervalInSec = MainFrame.getRefreshInterval();
//		int chartMaxSamples = (_chartMaxSaveTimeInSec/refreshIntervalInSec);
//		boolean graphNeedInit = false;
//		
//		if (s == null)
//			return;
//
//		// to avoid duplicate values
//		if (_oldTs != null)
//			if (_oldTs.equals(s))
//				return;
//		_oldTs = s;
//
//		if (_series[0] == null)
//		{
//			String seriesName = null;
//			if (_seriesNames != null && _seriesNames.length >= 1)
//			{
//				seriesName = _seriesNames[0];
//			}
//			if (seriesName == null)
//			{
//				seriesName = "Unknown";
//			}
//			_series[0] = new Trace2DLtd(chartMaxSamples);
//			_series[0].setRenderer(_chart); // needed in jChart2D 3.2.1  
//			_series[0].setZIndex(new Integer(_colorPtr));
//			_series[0].setColor(nextColor());
//			_series[0].setName(seriesName);
//			_series[0].addTracePainter(_tracePainter);
//			
//			_chart.addTrace(_series[0]);
//			graphNeedInit = true;
//			//_logger.debug(series[0].getName() + "Series[" + 0 + "] Z=" + series[0].getZIndex());
//		}
//		tp = new MyTracePoint2D(s.getTime(), val.doubleValue());
//
//		_series[0].addPoint(tp);
//		
//		if(graphNeedInit)
//		{
//			_logger.debug(_series[0].getName() + "-Series[" + 0 + "] graphNeedInit=" + graphNeedInit);
//			initGraph(_series[0], refreshIntervalInSec, -1);
//		}
//	}

	public void addPoint(java.util.Date s, Double[] val)
	{
		addPoint(s, val, null);
	}

	public void addPoint(java.util.Date s, Double[] val, String[] name)
	{
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int chartMaxSamples = (_chartMaxSaveTimeInSec/refreshIntervalInSec);
		addPoint(s, val, name, chartMaxSamples, -1);
	}

	public void addPoint(java.util.Date s, Double[] val, String[] name, Timestamp startPeriod, Timestamp endPeriod)
	{
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int secondsInSamplpe = (int) (endPeriod.getTime() - startPeriod.getTime()) / 1000;
		int chartMaxSamples = (secondsInSamplpe/refreshIntervalInSec);
		addPoint(s, val, name, chartMaxSamples, endPeriod.getTime());
	}

	public void addPoint(java.util.Date s, Double[] val, String[] name, int chartMaxSamples, long startTime)
	{
		MyTracePoint2D tp;
		boolean graphNeedInit = false;
		
		if (s == null)
			return;

		// to avoid duplicate values
		if (_oldTs != null)
			if (_oldTs.equals(s))
				return;

		_oldTs = s;

		// Resize the series array...
		ITrace2D[] prevSeries = null;
		if (val.length > _series.length)
		{
			_logger.debug("Resize the series array from " + _series.length + " to " + val.length + ".");
			prevSeries = _series;
			_series = new ITrace2D[val.length];
			//System.arraycopy(prevSeries, 0, series, 0, prevSeries.length);
			_logger.debug("AFTER Resize the series array is " + _series.length + ".");
		}

		for (int i = 0; i < val.length; i++)
		{
			// Check if its initialized
			if (_series[i] == null)
			{
				// Try get a name, first try the objects names, then the passed
				// ones
				String seriesName = null;
				if (seriesName == null && name != null && name.length > i)
				{
					seriesName = name[i];
				}
				if (seriesName == null && prevSeries != null && prevSeries.length > i && prevSeries[i] != null)
				{
					seriesName = prevSeries[i].getName();
				}
				if (seriesName == null && _seriesNames != null && _seriesNames.length > i)
				{
					seriesName = _seriesNames[i];
				}
				if (seriesName == null)
				{
					seriesName = "Unknown";
				}
				
				_series[i] = new Trace2DLtd(chartMaxSamples);
				_series[i].setRenderer(_chart); // needed in jChart2D 3.2.1  
				_series[i].setZIndex(new Integer(_colorPtr));
				_series[i].setColor(nextColor());
				_series[i].setName(seriesName);
				//_series[i].setStroke(new BasicStroke((float) 1.2));
				if (i == 0)
					_series[i].addTracePainter(_tracePainter);
				
				_chart.addTrace(_series[i]);

				graphNeedInit = true;
				//_logger.debug(series[i].getName() + "-Series[" + i + "] Z=" + series[i].getZIndex());

				// Set a highlighter for the trace: 
				Set<IPointPainter<?>> highlighters = _series[i].getPointHighlighters();
				highlighters.clear();
				_series[i].addPointHighlighter(new TooltipPointPainter());
			}
			//_logger.debug("val["+i+"] isnull:" + (val[i]==null?"null":val[i].toString()));

			// ADD the point
			tp = new MyTracePoint2D((double)s.getTime(), (val[i]==null ? 0 : val[i].doubleValue()));

			_series[i].addPoint(tp);
			
			if(graphNeedInit && i == 0)
			{
				_logger.debug(_series[i].getName() + "-Series[" + i + "] graphNeedInit=" + graphNeedInit);

				int refreshIntervalInSec = MainFrame.getRefreshInterval();
				initGraph(_series[i], refreshIntervalInSec, startTime);
			}
		}
	}

	//Currently initGraph expects there to be only one TracePoint in the trace.
	private void initGraph(ITrace2D trace, int refreshIntervalInSec, long time)
	{
//		long startTime;
		String threadName = Thread.currentThread().getName();
		_logger.debug("initGraph() was called from threadName='"+threadName+"'.");

//		if ( ! threadName.startsWith("AWT-EventQueue") )
		if ( ! SwingUtils.isEventQueueThread() )
		{
			// Cleanup the dummy trace now that we have real data to show.
			Runnable eventQueueExec = new Runnable()
			{
				public void run()
				{
					if(_dummySeries != null)
					{
						_dummySeries.removeAllPoints();
						_chart.removeTrace(_dummySeries);
						_dummySeries = null;
					}
				}
			};
			try{ SwingUtilities.invokeAndWait(eventQueueExec); }
			catch(Exception ignore) { }
		}
		else
		{
			// Don't know why this was done with SwingUtilities.invokeAndWait()
			// but when called from offline view, it's called from the "EventQueue" thread, 
			// which leads to an Exception that we cant do it...
			if(_dummySeries != null)
			{
				_dummySeries.removeAllPoints();
				_chart.removeTrace(_dummySeries);
				_dummySeries = null;
			}
		}
		
		Iterator<ITracePoint2D> iter = trace.iterator();
		ITracePoint2D tp = null;

		if( iter.hasNext() )
			tp = iter.next();
		
		if(tp != null && tp.getX() != trace.getMinX())
			_logger.warn("Trace minimal value is different from the only datapoint expected");

		if (time < 0)
		{
			if(tp != null)
			{
				time = (long) tp.getX();
			}
			else
			{
				time = (new java.util.Date().getTime());
				_logger.warn("Using local system time as starttime.");
			}
		}

		int maxSize = trace.getMaxSize();
		int bigThreshold = 1000;
		int bigDivider   = 10;
		for(int cnt=maxSize; cnt>0; cnt--)
		{
			long pointtime = time - (refreshIntervalInSec * cnt * 1000);

			if (maxSize > bigThreshold || cnt == maxSize || cnt == 1)
			{
				if ( (cnt % bigDivider) == 0 )
					trace.addPoint(new MyTracePoint2D( pointtime , 0.0));
			}
			else
			{
				trace.addPoint(new MyTracePoint2D( pointtime , 0.0));
			}
//			trace.addPoint(new MyTracePoint2D( pointtime , 0.0));
		}
		trace.addPoint(tp);


		// GET LABEL JPanel
		Component[] comp = _chartPanel.getComponents();
		for (int i=0; i<comp.length; i++)
		{
			if (comp[i].getClass().getName().equals("javax.swing.JPanel"))
			{
				if (_labelPanel == null)
				{
					_labelPanel = (JPanel) comp[i]; 
//					_timeLineWriter = new TimeLineWriter(_labelPanel);
				}
			}
		}
	}

	public void clearGraph()
	{
		Runnable doWork = new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < _series.length; i++)
				{
					if(_series[i] != null)
						_series[i].removeAllPoints();
					_series[i] = null;
				}
				_chart.removeAllTraces();
				_colorPtr = 0;

				//Add dummy datapoint
				_dummySeries = new Trace2DLtd(1) ;
				_dummySeries.setName(".");
				_chart.addTrace(_dummySeries);
				_dummySeries.addPoint(new MyTracePoint2D( (new java.util.Date()).getTime() , 0.0));
			}
		};

		// NOTE: In JChart2D version 3.2.1, a deadlock happens sometimes if this is done from the "reader" thread
		if ( ! SwingUtils.isEventQueueThread() )
		{
			try{ SwingUtilities.invokeAndWait(doWork); }
			catch(Exception ignore) { }
		}
		else
			doWork.run();
		
	}


	public JCheckBoxMenuItem getViewMenuItem()
	{
		return _chkboxMenuItem;
	}

	public boolean isGraphEnabled()
	{
		if (_chkboxMenuItem != null)
			return _chkboxMenuItem.isSelected();
		else
			return false;
	}

	public void setEnable(Boolean enabled)
	{
		boolean isEnabled = isGraphEnabled();
		if (enabled != isEnabled)
		{
			if (_chkboxMenuItem != null)
				_chkboxMenuItem.doClick();
		}
	}

	/** Get the JMeny attached to the JPanel */
	public JPopupMenu getPanelPopupMenu()
	{
		return _panelPopupMenu;
	}

	/**
	 * Creates the JMenu on the Component, this can be override by a subclass.
	 * <p>
	 * If you want to add stuff to the menu, its better to use
	 * getPanelPopupMenu(), then add entries to the menu. This is much better than subclass this Class
	 */
	public JPopupMenu createPanelPopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();
		for (JComponent comp : createPopupMenuComponents())
			popup.add(comp);
		
		if ( popup.getComponentCount() == 0 )
		{
			_logger.warn("No PopupMenu has been assigned for the Graph in the panel '" + _graphName + "'.");
			return null;
		}
		else
			return popup;
	}

	/**
	 * Create the Menu components that can be used my a JMenu or a JPopupMenu
	 * @return a List of Menu components (null is never returned, instead a empty List)
	 */
	private List<JComponent> createPopupMenuComponents()
	{
		ArrayList<JComponent> list = new ArrayList<JComponent>();

		//------------------------------------------------------------
		JMenuItem  mi = new JMenuItem("Set Graph Minimum Height");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String key1 = "Minimum Height";

				LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
				in.put(key1, Integer.toString( (_panelMinHeight <= 0 ? PANEL_MIN_HEIGHT_DEFAULT : _panelMinHeight) ));

				Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "Graph Sizing", in, false);

				if (results != null)
				{
					_panelMinHeight = Integer.parseInt(results.get(key1));
					_panel.setMinimumSize(new Dimension(-1, _panelMinHeight));
					// only setMinimumSize() did not resize the panel straight away, so setSize() was also added.
					_panel.setSize(new Dimension(-1, _panelMinHeight));

					saveProps();
				}
			}
		});

		//------------------------------------------------------------
		mi = new JMenuItem("Set Graph Minimum Height to DEFAULT");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_panelMinHeight = PANEL_MIN_HEIGHT_NOT_USED;
				int panelMinHeight = _panelMinHeight <= 0 ? PANEL_MIN_HEIGHT_DEFAULT : _panelMinHeight;

				_panel.setMinimumSize(new Dimension(-1, panelMinHeight));
				// only setMinimumSize() did not resize the panel straight away, so setSize() was also added.
				_panel.setSize(new Dimension(-1, panelMinHeight));

				saveProps();
			}
		});

		//------------------------------------------------------------
//		mi = new JMenuItem("Reorder the Graph Location...");
		mi = new JMenuItem("Change the Graph Order Layout...");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int ret = TrendGraphPanelReorderDialog.showDialog(MainFrame.getInstance(), SummaryPanel.getInstance().getGraphPanel());
				if (ret == JOptionPane.OK_OPTION)
				{
				}
			}
		});

		//------------------------------------------------------------
		list.add( new JSeparator() );

		
		//------------------------------------------------------------
		mi = new JMenuItem("Disable this graph");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (isGraphEnabled())
					getViewMenuItem().doClick();
			}
		});

		//------------------------------------------------------------
		// TODO: Enable other graphs
		JMenu menu = new JMenu("Enable other graphs");
		list.add(menu);

		final JPopupMenu popupMenu = menu.getPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				popupMenu.removeAll();

				for (CountersModel cm : GetCounters.getCmList())
				{
					// Now create menu items in the correct order
					for (String name : cm.getTrendGraphNames())
					{
						final TrendGraph tg = cm.getTrendGraph(name);
	
						JCheckBoxMenuItem mi = new JCheckBoxMenuItem();
						mi.setText(tg.getLabel());
						mi.setSelected(tg.isGraphEnabled());
						mi.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								tg.getViewMenuItem().doClick();
							}
						});
	
						popupMenu.add(mi);
					}
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});
		

//		if ( list.size() == 0 )
//			_logger.warn("No PopupMenu has been assigned for the Graph in the panel '" + _graphName + "'.");

		return list;
	}


	/**
	 * Create a JButton that can enable/disable available Graphs for a specific CounterModel
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	public static JButton createGraphAccessButton(JButton button, final String cmName)
	{
		if (cmName == null || (cmName != null && cmName.trim().equals("")) )
			throw new IllegalArgumentException("createGraphAccessButton() cmName can't be null or empty.");

		if (button == null)
			button = new JButton();

		button.setToolTipText("<html>What Trend Graphs are attached to this Performance Counter. Also enable/disable the graph(s).</html>");

		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
		button.setText("");
		button.setContentAreaFilled(false);
		button.setMargin( new Insets(0,0,0,0) );

		// Do PopupMenu
		final JPopupMenu trendGraphPopupMenu = new JPopupMenu();
		trendGraphPopupMenu.add(new JMenuItem("No Trend Graphs's is Available"));
		button.setComponentPopupMenu(trendGraphPopupMenu);
		trendGraphPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				trendGraphPopupMenu.removeAll();

				// Get the CM: try short name and long name
				CountersModel cm = GetCounters.getCmByName(cmName);
				if (cm == null)
					cm = GetCounters.getCmByDisplayName(cmName);
				
				if (cm == null)
				{
					trendGraphPopupMenu.add(new JMenuItem("Performance Counter named '"+cmName+"' couldn't be found."));
				}
				else
				{
					// Now create menu items in the correct order
					for (String name : cm.getTrendGraphNames())
					{
						final TrendGraph tg = cm.getTrendGraph(name);
	
						JCheckBoxMenuItem mi = new JCheckBoxMenuItem();
						mi.setText(tg.getLabel());
						mi.setSelected(tg.isGraphEnabled());
						mi.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								tg.getViewMenuItem().doClick();
							}
						});
	
						trendGraphPopupMenu.add(mi);
					}
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}

	public JPanel getPanel()
	{
		if (_panel == null)
		{
			_panel = new JPanel();

			// Add a right click menu
			_panelPopupMenu = createPanelPopupMenu();
			_panel.setComponentPopupMenu(_panelPopupMenu);

			// hook into the CHART PANELS popup menu
			// - add one JSeparator
			// - add one sub menu "AseTune Menu"
			for (MouseListener ml : _chartPanel.getChart().getMouseListeners())
			{
				// Loop all the MenuListeners until we find the Charts Listener 
				if (ml instanceof info.monitorenter.gui.chart.events.PopupListener)
				{
					info.monitorenter.gui.chart.events.PopupListener cpl = (info.monitorenter.gui.chart.events.PopupListener) ml;
					JPopupMenu chartPopupMenu = cpl.getPopup();

					if (chartPopupMenu != null)
					{
						chartPopupMenu.add(new JSeparator());
	
						JMenu aseTuneMenu = new JMenu(Version.getAppName()+" Menu");
						chartPopupMenu.add(aseTuneMenu);
	
						// Create some menu entries and add those to the sub menu...
						for (JComponent comp : createPopupMenuComponents())
							aseTuneMenu.add(comp);
					}
				}
			}

			// ...
			_panel.setLayout(new BorderLayout());
//			_panel.setPreferredSize(new Dimension(406, 160));
			// If NOT set, use 104 pixels
			_panel.setMinimumSize(new Dimension(-1, (_panelMinHeight <= 0 ? PANEL_MIN_HEIGHT_DEFAULT : _panelMinHeight) ));
			_panel.setBorder(BorderFactory.createLoweredBevelBorder());
			_panel.add(this.getChartPanel(), null);

//			if ( ! _initialVisible )
//				_panel.setVisible(false);

			// NOTE: probably not the best place to have this... move later on...
			if ( ! _loadProps_menuItem_checkbox )
				_chkboxMenuItem.doClick();
		}

		return _panel;
	}

	public CountersModel getCm()
	{
		return _cm;
	}
	public void setCm(CountersModel cm)
	{
		_cm = cm;
	}

	public void actionPerformed(ActionEvent e)
	{
		Object s = e.getSource();
		_logger.trace("TrendGraphAction.actionPerformed(), ActionEvent(classname)="+s.getClass().getName());
		if (s instanceof JMenuItem)
		{
			JMenuItem source = (JMenuItem) s;
			_logger.trace("Action event detected. Event source: " + source.getText() + " (an instance of " + source.getClass().getName() + ")");
    		
			if ( _panel != null && _chkboxMenuItem != null)
			{
				boolean isSelected = _chkboxMenuItem.isSelected();
				_panel.setVisible( isSelected );

				saveProps();
			}
		}
	}

	private void saveProps()
  	{
		Configuration tempProps = Configuration.getInstance(Configuration.USER_TEMP);
		String graphName = this.getName();

		if (tempProps != null)
		{
			tempProps.setProperty("MainFrame.menu."+graphName+".checkbox",  _chkboxMenuItem.isSelected());
			tempProps.setProperty("Graph."+graphName+".minHeight",          _panelMinHeight);

			tempProps.save();
		}
	}

	boolean _loadProps_menuItem_checkbox = true;
	private void loadProps()
	{
//		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
		Configuration tempProps = Configuration.getCombinedConfiguration();
		String graphName = this.getName();
		
		if (tempProps != null)
		{
			_loadProps_menuItem_checkbox = tempProps.getBooleanProperty("MainFrame.menu."+graphName+".checkbox",  _initialVisible);
			_panelMinHeight = tempProps.getIntProperty("Graph."+graphName+".minHeight",  _panelMinHeight);
		}
	}
	

	
	
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//---- SUBCLASES BELOW THIS POINT ----------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------	
	
	//------------------------------------------------------------
	/** Used to write the LABEL on the top of the graph */
	//------------------------------------------------------------
	private class Watermark
	extends AbstractComponentDecorator
	{
		public Watermark(JComponent target, String text)
		{
			super(target);
			if (text != null)
				_text = text;
		}
		private String		_text	= "";
		private Graphics2D	g		= null;
		private Rectangle	r		= null;
	
		public void paint(Graphics graphics)
		{
			if (_text == null || _text != null && _text.equals(""))
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			
			Font f = UIManager.getDefaults().getFont("Label.font").deriveFont(14f).deriveFont(Font.BOLD);
			g.setFont(f);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			FontMetrics fm = g.getFontMetrics();
			int strWidth = fm.stringWidth(_text);
			int xPos = (r.width - strWidth) / 2;
			int yPos = (int) fm.getHeight() - 2;
			g.drawString(_text, xPos, yPos);
		}
	
		public void setWatermarkText(String text)
		{
			_text = text;
			_logger.debug("setWatermarkText: to '" + _text + "'.");
			repaint();
		}
	}


	//------------------------------------------------------------
	/** Specialized trace point to be able to write a TimeLineMarker in the graph */
	//------------------------------------------------------------
	private class MyTracePoint2D
	extends TracePoint2D
	{
        private static final long serialVersionUID = 1L;
		private boolean _timelineMarked = false;

		public MyTracePoint2D(double value, double value2)
		{
			super(value, value2);
		}
		
		public boolean isTimeLineMarked() {return _timelineMarked;}
		public void    setTimeLineMarked(boolean b) {_timelineMarked = b;}
	}

	//------------------------------------------------------------
	/** Specialized Painter to be able to write a TimeLineMarker in the graph */
	//------------------------------------------------------------
	private class MyTracePainter
	extends ATracePainter 
	{
		private static final long serialVersionUID = 1L;

		private PointPainterTimeLineMark m_pointPainter;

		public MyTracePainter() 
		{
			this.m_pointPainter = new PointPainterTimeLineMark();
		}

		/** @see info.monitorenter.gui.chart.ITracePainter#endPaintIteration(java.awt.Graphics) */
		public void endPaintIteration(final Graphics g2d) 
		{
			if (g2d != null) 
			{
				this.m_pointPainter.paintPoint(this.getPreviousX(), this.getPreviousY(), 0, 0, g2d, this.getPreviousPoint());
			}
			this.m_pointPainter.endPaintIteration(g2d);
		}
		/** @see info.monitorenter.gui.chart.traces.painters.ATracePainter#startPaintIteration(java.awt.Graphics) */
		public void startPaintIteration(final Graphics g2d) 
		{
			this.m_pointPainter.startPaintIteration(g2d);
		}
		/** @see info.monitorenter.gui.chart.traces.painters.ATracePainter#paintPoint(int, int, int, int, java.awt.Graphics, info.monitorenter.gui.chart.TracePoint2D) */
		public void paintPoint(final int absoluteX, final int absoluteY, final int nextX, final int nextY, final Graphics g, final ITracePoint2D original) 
		{
			super.paintPoint(absoluteX, absoluteY, nextX, nextY, g, original);
			this.m_pointPainter.paintPoint(absoluteX, absoluteY, nextX, nextY, g, original);
		}
	}

	//------------------------------------------------------------
	/** Specialized Painter to be able to write a TimeLineMarker in the graph */
	//------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private class PointPainterTimeLineMark 
	extends APointPainter 
	{
		/** Generated <code>serialVersionUID</code>. */
		private static final long serialVersionUID = -6317473632026920774L;

		public PointPainterTimeLineMark() 
		{
		}

		/** @see info.monitorenter.gui.chart.IPointPainter#paintPoint(int, int, int, int, java.awt.Graphics, info.monitorenter.gui.chart.TracePoint2D) */
		public void paintPoint(final int absoluteX, final int absoluteY, final int nextX, final int nextY, final Graphics g, final ITracePoint2D original) 
		{
			if (original instanceof MyTracePoint2D)
			{
				MyTracePoint2D my = (MyTracePoint2D) original;
				if (my.isTimeLineMarked())
				{
					if (_logger.isTraceEnabled())
						_logger.trace("PointPainterTimeLineMark.paintPoint(): graphName="+StringUtil.left(_graphName,15)+", absoluteX="+absoluteX+", absoluteY="+absoluteY+", nextX="+nextX+", nextY="+nextY+".");	

					// Set the color to write the timeline marker
					Color saveColor = g.getColor();
					g.setColor(Color.DARK_GRAY);

					// Write some special stuff to indicate WHERE we are positioned in the Graph
					g.fillRect(nextX, 0, 3, 999);

					// RESTORE original Color
					g.setColor(saveColor);
				}
			}
		}
	}


	//------------------------------------------------------------
	/** tooltip which displays the label title and it's value. */
	//------------------------------------------------------------
	private class MyTooltip
	implements IToolTipType
	{
		/**
		 * @see info.monitorenter.gui.chart.IToolTipType#getDescription()
		 */
		@Override
		public String getDescription() 
		{
			return "Show the label title and it's value.";
		}

		/**
		 * @see info.monitorenter.gui.chart.Chart2D.ToolTipType#getToolTipText(java.awt.event.MouseEvent)
		 */
		@Override
		public String getToolTipText(final Chart2D chart, final MouseEvent me) 
		{
			String result;
			final ITracePoint2D point = chart.getNearestPointManhattan(me);

			// What is the last point we have requested q tooltip for
			_lastToolTipPoint = point;

			if (point == null)
				return "TrendGraph.getToolTipText(): point=null, can't continue.";

			// We need the axes of the point for correct formatting (expensive...).
			ITrace2D trace = point.getListener();
			IAxis xAxis = chart.getAxisX(trace);
			IAxis yAxis = chart.getAxisY(trace);

			if (xAxis == null)
				return "TrendGraph.getToolTipText(): xAxis=null, can't continue.";
			if (yAxis == null)
				return "TrendGraph.getToolTipText(): yAxis=null, can't continue.";

			chart.setRequestedRepaint(true);

			StringBuffer buffer = new StringBuffer();
			buffer.append("<html>");

			String yAxisName = yAxis.getAxisTitle().getTitle();
			if (yAxisName == null) yAxisName = trace.getName();
			if (yAxisName == null) yAxisName = "Value";
			buffer.append(yAxisName).append(": ");
			buffer.append(yAxis.getFormatter().format(point.getY()));
			buffer.append("<br>");

			buffer.append("Time: ");
			buffer.append(xAxis.getFormatter().format(point.getX()));
			buffer.append("<br>");

			buffer.append("</html>");
			result = buffer.toString();
			return result;
		}
	}

	//------------------------------------------------------------
	/** Draw a circle around the trace point we are pointing at in the tooltip. */
	//------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private class TooltipPointPainter
	extends APointPainter
	{
		private static final long	serialVersionUID	= 1L;
		@Override
		public void paintPoint(int absoluteX, int absoluteY, int nextX, int nextY, Graphics g, ITracePoint2D original)
		{
			if (original != null && original.equals(_lastToolTipPoint))
			{
				// draw a circle
				int discSize = 4;
				int halfDiscSize = 2;
				g.drawOval(absoluteX - halfDiscSize, absoluteY - halfDiscSize, discSize, discSize);
			}
		}
	}

}
