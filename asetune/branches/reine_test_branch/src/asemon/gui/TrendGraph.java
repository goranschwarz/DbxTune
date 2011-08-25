/**
 */

package asemon.gui;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.TracePoint2D;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterDate;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterSimple;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyMinimumViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.gui.chart.views.ChartPanel;
import info.monitorenter.util.Range;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import asemon.Asemon;
import asemon.CountersModel;
import asemon.gui.swing.AbstractComponentDecorator;
import asemon.utils.Configuration;




public class TrendGraph
implements ActionListener
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(TrendGraph.class);
	
	private int _colorPtr = 0;

	// private boolean initialized=false;
	private ITrace2D[]         series;
	private ITrace2D           dummySeries           = null;    // Used to avoid NPE when not connected
	private ChartPanel         chartPanel;
	private String             _graphName;
	private String             _labelName;
	private String[]           _seriesNames	         = null;
	private String[]           _dataColNames         = {};
	private String[]           _dataMethods          = {};
	private Chart2D            chart;
	private boolean	           pctGraph;
	private java.util.Date	   oldTs	             = null;
	private static int         chartMaxSaveTimeInSec = 10*60;   // number of seconds saved in the chart
	private JCheckBoxMenuItem  _chkboxMenuItem       = null;
	private JPanel             _panel                = null;
	private CountersModel      _cm                   = null;
	SimpleDateFormat           sdf                   = new SimpleDateFormat("H:mm:ss.SSS");

	// The name of the graph, jachrt2d not good enough.
	private Watermark          _watermark            = null;

	public TrendGraph(String name, String chkboxText, String label, String[] seriesNames, boolean pct, CountersModel cm)
	{
		_graphName   = name;
		_labelName   = label;
		_seriesNames = seriesNames;
		_cm          = cm;
		Configuration aseMonProps = Asemon.getProps();
		
		boolean showToolTip = aseMonProps.getBooleanProperty("TrendGraph.showToolTip", false);
		
		pctGraph = pct;
		chart = new Chart2D();
		series = new ITrace2D[1];
		dummySeries = new Trace2DLtd(1) ;
		
		/*
		** Use the feature below when it is available. Current version 3.10 does not have it
		** but it is in the repository of jChart2D.
		chart.setUseAntialiasing(true);
		*/
		
		chart.getAxisX().setFormatter(new LabelFormatterDate((SimpleDateFormat) DateFormat.getTimeInstance()));
		chart.getAxisY().setFormatter(new LabelFormatterSimple());
		chart.getAxisX().getAxisTitle().setTitle(null);
		chart.getAxisY().getAxisTitle().setTitle(null);
		chart.getAxisX().setPaintGrid(true);
		chart.getAxisY().setPaintGrid(true);
		
		_logger.debug("showToolTip = " + showToolTip); 
		if(showToolTip)
		{
			chart.setToolTipType(Chart2D.ToolTipType.VALUE_SNAP_TO_TRACEPOINTS);
	    	}

		chartPanel = new ChartPanel(chart);
		_watermark = new Watermark(chartPanel, _labelName);


		if (chkboxText != null  &&  !chkboxText.equals(""))
		{
			_chkboxMenuItem = new JCheckBoxMenuItem(chkboxText, true);
			_chkboxMenuItem.addActionListener( this );
		}

		if (pctGraph)
		{
			//Seem to be a small bug in jChart2D when adding Border and traceName, some space vanishes.
			//Increase by 20% in height.
			chart.getAxisY().setRangePolicy(new RangePolicyFixedViewport(new Range(0,100)));
//			chart.getAxisY().setMajorTickSpacing(20);
//			chart.getAxisY().setMinorTickSpacing(20);
		}
		else
		{
			chart.getAxisY().setRangePolicy(new RangePolicyMinimumViewport(new Range(0,1)));
		}
		
		//Add dummy datapoint
		dummySeries.setName(".");
		chart.addTrace(dummySeries);
		dummySeries.addPoint(new TracePoint2D( (new java.util.Date()).getTime() , 0.0));
	}

	public String getName()  { return _graphName; }
	public String getLabel() { return _labelName; }
	
	private Color nextColor()
	{ 
		if( (_colorPtr % TrendGraphColors._colors.length)==0 ) 
			_colorPtr = 0; 
		return TrendGraphColors._colors[_colorPtr++];
	}
	
	public static int getChartMaxHistoryTimeInMinutes()
	{
		return chartMaxSaveTimeInSec / 60;
	}

	/**
	 * Set how many minutes the chart should contain.
	 * 
	 * @param minutes
	 */
	public void setChartMaxHistoryTimeInMinutes(int minutes)
	{
		chartMaxSaveTimeInSec = minutes * 60;
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int chartMaxSamples = (chartMaxSaveTimeInSec/refreshIntervalInSec);
		
//		_logger.debug("refreshIntervalInSec= " + refreshIntervalInSec 
//				+ " chartMaxSaveTimeInSec=" + chartMaxSaveTimeInSec 
//				+ " chartMaxSamples=" + chartMaxSamples);
		if (series != null)
		{
			for (int i = 0; i < series.length; i++)
			{
				// Check if its initialized
				if (series[i] != null)
				{
					((Trace2DLtd)series[i]).setMaxSize(chartMaxSamples);
					
					// Initialize the uninitialized part of the graph if we increase the history size.
					java.util.Date starttime;
					if(series[i].getMinX() != 0)
					{
						starttime = new java.util.Date((long) series[i].getMaxX());
					}
					else
					{
						starttime = new java.util.Date();	
						_logger.warn("Using local system time as starttime.");
					}
					
					if (series[i].getSize() < chartMaxSamples)
					{
						ITrace2D prevTrace = series[i];
						ITrace2D newTrace = new Trace2DLtd(chartMaxSamples);
						series[i] = newTrace;
						newTrace.setColor(prevTrace.getColor());
						newTrace.setName( prevTrace.getName());
						newTrace.setZIndex(prevTrace.getZIndex());
						
						chart.removeTrace(prevTrace);
						chart.addTrace(newTrace);
						
						long pointTime = 0;
						Iterator iter = prevTrace.iterator();
						int cnt = chartMaxSamples;
						boolean firstTP = true;
						while(true)
						{
							pointTime = starttime.getTime() - (refreshIntervalInSec * cnt * 1000);
							if( pointTime >= ((long)prevTrace.getMinX()) )
							{
								if(iter.hasNext())
								{
									TracePoint2D tp = (TracePoint2D) iter.next();
									
//									if( _logger.isDebugEnabled() && firstTP )
//									{
//										_logger.debug(series[i].getName() + "-Series[" + i + "] " 
//												+ "First TP:" + sdf.format(new java.util.Date(pointTime)) + " Cnt:" + cnt);
//									}
									
									if( _logger.isDebugEnabled() && firstTP && (long)prevTrace.getMinX() != (long)tp.getX() )
									{
										_logger.debug(series[i].getName() + "-Series[" + i + "] DiffTimeSec=" 
												+ ((long)tp.getX() - (long)prevTrace.getMinX())/1000
												+ " MinX != tp.getX()"
												+ " PointTime:" + sdf.format(new java.util.Date(pointTime)) + " Cnt:" + cnt);
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
								newTrace.addPoint(new TracePoint2D( pointTime, 0.0));
							}
							cnt--;
						}
					}

					if( _logger.isDebugEnabled() && series[i].getMaxSize() != series[i].getSize() )
						_logger.debug(series[i].getName() + "-Series[" + i + "] maxSize=" + series[i].getMaxSize() 
								+ " size=" + series[i].getSize());
					
					//Not sure if the RingBuffer throws more than I expect. Therefore the hysteresis of refreshIntervalInSec * 1
					if( _logger.isDebugEnabled() 
							&& Math.abs(((long)series[i].getMaxX()-(long)series[i].getMinX())/1000 - chartMaxSaveTimeInSec) > refreshIntervalInSec  )
					{
						_logger.debug(series[i].getName() + "-Series[" + i + "] " 
							+ "Incorrect timespan:" + ((long)series[i].getMaxX()-(long)series[i].getMinX())/1000 + " "
							+ "chartMaxSaveTimeInSec:" + chartMaxSaveTimeInSec + " "
							+ "PrevMaxX:" + sdf.format(starttime) + " "
							+ "Started at:" + sdf.format(new java.util.Date(starttime.getTime() - (refreshIntervalInSec * (chartMaxSamples) * 1000))) + " "
							+ "Min:" + sdf.format(new java.util.Date((long)series[i].getMinX())) + " "
							+ "Max:" + sdf.format(new java.util.Date((long)series[i].getMaxX())) 
							);
					}
				}
			}
		}
	}

	public ChartPanel getChartPanel()
	{
		return chartPanel;
	}

	public void setGraphCalculations(String[] dataCols, String[] dataOpers)
	{
		_dataColNames = dataCols;
		_dataMethods  = dataOpers;
	}
	
	public void addPoint()
	{
		if ( _dataColNames.length == 0)
			return;

		if ( isGraphEnabled() && _cm != null)
		{
			Double[] dataArray = new Double[_dataColNames.length];
			for (int i=0; i<_dataColNames.length; i++)
			{
				String colName = _dataColNames[i];
				String op      = _dataMethods[i];
				Double data    = null;

				if      (op.equals("rateMax"))       data = _cm.getRateValueMax(colName);
				else if (op.equals("rateMin"))       data = _cm.getRateValueMin(colName);
				else if (op.equals("rateAvg"))       data = _cm.getRateValueAvg(colName);
				else if (op.equals("rateAvgGtZero")) data = _cm.getRateValueAvgGtZero(colName);
				else if (op.equals("rateSum"))       data = _cm.getRateValueSum(colName);

				else if (op.equals("diffMax"))       data = _cm.getDiffValueMax(colName);
				else if (op.equals("diffMin"))       data = _cm.getDiffValueMin(colName);
				else if (op.equals("diffAvg"))       data = _cm.getDiffValueAvg(colName);
				else if (op.equals("diffAvgGtZero")) data = _cm.getDiffValueAvgGtZero(colName);
				else if (op.equals("diffSum"))       data = _cm.getDiffValueSum(colName);

				else if (op.equals("absMax"))        data = _cm.getAbsValueMax(colName);
				else if (op.equals("absMin"))        data = _cm.getAbsValueMin(colName);
				else if (op.equals("absAvg"))        data = _cm.getAbsValueAvg(colName);
				else if (op.equals("absAvgGtZero"))  data = _cm.getAbsValueAvgGtZero(colName);
				else if (op.equals("absSum"))        data = _cm.getAbsValueSum(colName);
				else
				{
					_logger.warn("Graph named '"+_graphName+"' has unknown operator '"+op+"' for column '"+colName+"'.");
				}

				dataArray[i] = data;
			}
//			if (_logger.isDebugEnabled())
//			{
//				String debugStr = "Graph named '" + _graphName + "', add data: ";
//				for (int i=0; i<_dataColNames.length; i++)
//				{
//					debugStr += _dataColNames[i] + "='" + dataArray[i] + "', ";
//				}
//				_logger.debug(debugStr);
//			}

			addPoint((java.util.Date) _cm.getTimestamp(), dataArray);
		}
		
	}
	public void addPoint(java.util.Date s, Double val)
	{
		TracePoint2D tp;
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int chartMaxSamples = (chartMaxSaveTimeInSec/refreshIntervalInSec);
		boolean graphNeedInit = false;
		
		if (s == null)
			return;

		// to avoid duplicate values
		if (oldTs != null)
			if (oldTs.equals(s))
				return;
		oldTs = s;

		if (series[0] == null)
		{
			String seriesName = null;
			if (_seriesNames != null && _seriesNames.length >= 1)
			{
				seriesName = _seriesNames[0];
			}
			if (seriesName == null)
			{
				seriesName = "Unknown";
			}
			series[0] = new Trace2DLtd(chartMaxSamples);
			series[0].setZIndex(new Integer(_colorPtr));
			series[0].setColor(nextColor());
			series[0].setName(seriesName);
			chart.addTrace(series[0]);
			graphNeedInit = true;
			//_logger.debug(series[0].getName() + "Series[" + 0 + "] Z=" + series[0].getZIndex());
		}
		tp = new TracePoint2D(s.getTime(), val.doubleValue());
	
		series[0].addPoint(tp);
		
		if(graphNeedInit)
		{
			_logger.debug(series[0].getName() + "-Series[" + 0 + "] graphNeedInit=" + graphNeedInit);
			initGraph(series[0], refreshIntervalInSec);
		}
		
		if (val == null)
			return;
	}

	public void addPoint(java.util.Date s, Double[] val)
	{
		addPoint(s, val, null);
	}

	public void addPoint(java.util.Date s, Double[] val, String[] name)
	{
		TracePoint2D tp;
		int refreshIntervalInSec = MainFrame.getRefreshInterval();
		int chartMaxSamples = (chartMaxSaveTimeInSec/refreshIntervalInSec);
		boolean graphNeedInit = false;
		
		if (s == null)
			return;

		// to avoid duplicate values
		if (oldTs != null)
			if (oldTs.equals(s))
				return;

		oldTs = s;

		// Resize the series array...
		if (val.length > series.length)
		{
			_logger.debug("Resize the series array from " + series.length + " to " + val.length + ".");
			ITrace2D[] prevSeries = series;
			series = new ITrace2D[val.length];
			System.arraycopy(prevSeries, 0, series, 0, prevSeries.length);
			_logger.debug("AFTER Resize the series array is " + series.length + ".");
		}

		for (int i = 0; i < val.length; i++)
		{
			// Check if its initialized
			if (series[i] == null)
			{
				// Try get a name, first try the objects names, then the passed
				// ones
				String seriesName = null;
				if (_seriesNames != null && _seriesNames.length > i)
				{
					seriesName = _seriesNames[i];
				}
				if (seriesName == null && name != null && name.length > i)
				{
					seriesName = name[i];
				}
				if (seriesName == null)
				{
					seriesName = "Unknown";
				}
				
				series[i] = new Trace2DLtd(chartMaxSamples);
				series[i].setZIndex(new Integer(_colorPtr));
				series[i].setColor(nextColor());
				series[i].setName(seriesName);
				//series[i].setStroke(new BasicStroke((float) 1.2));
				chart.addTrace(series[i]);
				graphNeedInit = true;
				//_logger.debug(series[i].getName() + "-Series[" + i + "] Z=" + series[i].getZIndex());
			}
			//_logger.debug("val["+i+"] isnull:" + (val[i]==null?"null":val[i].toString()));

			// ADD the point
			tp = new TracePoint2D((double)s.getTime(), (val[i]==null ? 0 : val[i].doubleValue()));
			
			series[i].addPoint(tp);
			
			if(graphNeedInit)
			{
				_logger.debug(series[i].getName() + "-Series[" + i + "] graphNeedInit=" + graphNeedInit);
				initGraph(series[i], refreshIntervalInSec);
			}
		}
	}

	//Currently initGraph expects there to be only one TracePoint in the trace.
	private void initGraph(ITrace2D trace, int refreshIntervalInSec)
	{
		long starttime;
		Iterator iter = trace.iterator();
		TracePoint2D tp = null;

		// Cleanup the dummy trace now that we have real data to show.
		Runnable eventQueueExec = new Runnable()
		{
			public void run()
			{
				if(dummySeries != null)
				{
					dummySeries.removeAllPoints();
					chart.removeTrace(dummySeries);
					dummySeries = null;
				}
			}
		};
		try{ SwingUtilities.invokeAndWait(eventQueueExec); }
		catch(Exception ignore) { }
		
		if( iter.hasNext() )
			tp = (TracePoint2D) iter.next();
		
		if(tp != null && tp.getX() != trace.getMinX())
			_logger.warn("Trace minimal value is different from the only datapoint expected");
		
		if(tp != null)
		{
			starttime = (long) tp.getX();
		}
		else
		{
			starttime = (new java.util.Date().getTime());		
			_logger.warn("Using local system time as starttime.");
		}

		for(int cnt = trace.getMaxSize(); cnt > 0; cnt--)
		{
			long pointtime = starttime - (refreshIntervalInSec * cnt * 1000);
			
			trace.addPoint(new TracePoint2D( pointtime , 0.0));
		}
		trace.addPoint(tp);
	}

	public void clearGraph()
	{
		for (int i = 0; i < series.length; i++)
		{
			series[i].removeAllPoints();
			series[i] = null;
		}
		chart.removeAllTraces();
		_colorPtr = 0;
		
		//Add dummy datapoint
		dummySeries = new Trace2DLtd(1) ;
		dummySeries.setName(".");
		chart.addTrace(dummySeries);
		dummySeries.addPoint(new TracePoint2D( (new java.util.Date()).getTime() , 0.0));
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

	public JPanel getPanel()
	{
		if (_panel == null)
		{
			_panel = new JPanel();
			
			_panel.setLayout(new BorderLayout());
//			_panel.setPreferredSize(new Dimension(406, 160));
			_panel.setBorder(BorderFactory.createLoweredBevelBorder());
			_panel.add(this.getChartPanel(), null);
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
		System.out.println("TrendGraphAction.actionPerformed(), ActionEvent(classname)="+s.getClass().getName());
		if (s instanceof JMenuItem)
		{
			JMenuItem source = (JMenuItem) s;
			System.out.println("Action event detected. Event source: " + source.getText() + " (an instance of " + source.getClass().getName() + ")");
    		
			if ( _panel != null && _chkboxMenuItem != null)
			{
//_logger.info("_panel.setVisible("+_chkboxMenuItem.isSelected()+").");
				_panel.setVisible( _chkboxMenuItem.isSelected() );
				//saveProps();
			}
		}
	}

	
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

}
