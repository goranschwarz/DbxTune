/**
 * @author <a href="mailto:larrei@gmail.com">Reine Lindqvist</a>
 */
package asemon.cm;

import java.math.BigDecimal;
import java.sql.Connection;

import org.apache.log4j.Logger;

import asemon.CountersModel;
import asemon.SamplingCnt;
import asemon.rstat.RstatSample;
import asemon.utils.AseConnectionFactory;

public class CountersModelRemoteCPU extends CountersModel
{
	private static final long serialVersionUID = 2658672019678431890L;
	
	/** Log4j logging. */
	private   static      Logger _logger          = Logger.getLogger(CountersModelRemoteCPU.class);
	
	//Should be some interface with method getSample ...?
	protected RstatSample oldSample               = null;      // Contains old raw data
	protected RstatSample newSample               = null;      // Contains new raw data

	private   String      _hostname;
	private   boolean     _initiated              = false;
	
	public CountersModelRemoteCPU(String nm, String clazz, String[] pctColumns, boolean negativeDiffCountersToZero)
	{
		//Instantiate the clazz which conforms to a certain interface
		super(nm, null, /*pkList*/ null, null, pctColumns, null, negativeDiffCountersToZero);
	}

	public void initSql()
	{
		// NOP
	}
	
	//By now we should know the servername.
	private void _init()
	{
		if(_initiated)
			return;
		
		_hostname = AseConnectionFactory.getHost();
		
		oldSample = new RstatSample(_hostname);
		newSample = new RstatSample(_hostname);
		if (_isPctCol == null)
			_isPctCol = new boolean[ 1 ];
		_isPctCol[0] = true;

		_logger.info("Trying to connect to '" + _hostname + "'");
		if(!oldSample.isConnected())
		{
			_logger.info("No Rstat server on target");
			this.setActive(false, "No Rstat server on target");
		}
		else
		{
			_logger.info("CountersModelNonDbSource. getSample");
			oldSample.getSample();
			newSample.getSample();
		}
		
		_initiated = true;
	}
	
	public synchronized void refresh() throws Exception
	{
		_init();
		
		if(!oldSample.isConnected())
			return;
		oldSample = newSample;
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Entering refreshCM() method for " + this.getName());
		}
		if ( ! isSwingRefreshOK() )
		{
			// a swing refresh is in process, don't break the data, so wait for the next loop
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Exit refreshCM() method for " + getName() + ". Swingrefresh already processing.");
			}
			return;
		}
		if (_logger.isDebugEnabled())
			_logger.debug("Refreshing Counters for '"+getName()+"'.");

		// Start the timer which will be kicked of after X ms
		// This so we can do something if the refresh takes to long time
		_refreshTimer.start();
		
		newSample = new RstatSample(_hostname);
		newSample.getSample();
		
		//Do some calc, update table or something.
		this.setSampleInterval(oldSample.getSampleTimeMs(newSample));
		this.setSampleTime(newSample.getSampleTime());
		this.setCounterClearTime(oldSample.getBootTime());
		
		//Dummy to signal that we have data
		super.newSample = new SamplingCnt("DummyNewSample",false);
		super.diffData  = new SamplingCnt("DummyDiffData",false);
		
		if (tabPanel != null)
			tabPanel.setTimeInfo(getCounterClearTime(), getSampleTime(), getSampleInterval());
		
		_refreshTimer.stop();
	}
	
	public synchronized void refresh(Connection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Entering refreshCM() method for " + this.getName());
		}
		refresh();
	}
	
	public Object getValueAt(int row, int col)
	{
		int counter = 0;
		if(!oldSample.isConnected())
			return null;
		while (true)
		{
			try
			{
				return new BigDecimal( oldSample.getDiffCPULoad(newSample) * 100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			}
			catch (NullPointerException e)
			{
				counter++;
				if (counter >= 10 )
					return null;
				// This probably happens due to GetCounters thread modifies 
				// the Vector at the same time as AWT-EventQueue thread refreshes 
				// filters/cellRendering or does something...
				_logger.warn("GetValueAt(row="+row+", col="+col+"): NullPointerException, retry counter="+counter+"...");
				//Thread.yield();
				try {Thread.sleep(3);}
				catch (InterruptedException ignore) {}
			}
		}
		
//		return new Double(oldSample.getDiffCPULoad(newSample));
    }
	public int getRowCount()
	{
		int c = 1;
		return c;
    }
	public int getColumnCount()
	{
		return 1;
	}
	public String getColumnName(int column)
	{
		return "CPULoad";
	}
	public Class getColumnClass(int column)
	{
		return BigDecimal.class;
	}	
}
