/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import asemon.cm.CountersModel;
import asemon.utils.StringUtil;

public class PersistContainer
{
	private static Logger _logger          = Logger.getLogger(PersistContainer.class);

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	/** This one is maintained from the PersistWriter, which is responsible for starting/ending storage sessions. 
	 * If _sessionStartTime is set by a user, it will be over written by the PersistCoiunterHandler or the PersistWriter */
	protected Timestamp           _sessionStartTime = null;
	protected Timestamp           _mainSampleTime   = null;
	protected long                _sampleInterval   = 0;
	protected String              _serverName       = null;
	protected String              _onHostname       = null;
	protected List<CountersModel> _counterObjects   = null;

	protected boolean             _startNewSample   = false;



	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
//	public PersistContainer(Timestamp sessionStartTime, Timestamp mainSampleTime, String serverName, String onHostname)
//	{
//		_sessionStartTime = sessionStartTime;
//		_mainSampleTime   = mainSampleTime;
//		_serverName       = serverName;
//		_onHostname       = onHostname;
//	}
	public PersistContainer(Timestamp mainSampleTime, String serverName, String onHostname)
	{
		_sessionStartTime = null;
		_mainSampleTime   = mainSampleTime;
		_serverName       = serverName;
		_onHostname       = onHostname;
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	/** Set the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public void setSessionStartTime(Timestamp startTime)    { _sessionStartTime = startTime; }
	/** Set the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public void setMainSampleTime(Timestamp mainSampleTime) { _mainSampleTime = mainSampleTime; }
	public void setServerName(String serverName)            { _serverName = serverName; }
	public void setOnHostname(String onHostname)            { _onHostname = onHostname; }
	
	/** This can be used to "force" a new sample, for example if you want to 
	 * start a new session on reconnects to the monitored ASE server */
	public void setStartNewSample(boolean startNew) { _startNewSample = startNew; }

	/** Get the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public Timestamp getSessionStartTime() { return _sessionStartTime; }
	/** Get the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public Timestamp getMainSampleTime()   { return _mainSampleTime; }
	public long      getSampleInterval()   { return _sampleInterval; }
	public String    getServerName()       { return _serverName; }
	public String    getOnHostname()       { return _onHostname; }
	public boolean   getStartNewSample()   { return _startNewSample; }

	public CountersModel getCm(String name)
	{
		if (_counterObjects == null)
			return null;

		for (Iterator<CountersModel> it = _counterObjects.iterator(); it.hasNext();)
        {
	        CountersModel cm = it.next();
	        if (cm.getName().equals(name))
	        	return cm;
        }
		return null;
	}

	public void add(CountersModel cm)
	{
		if (_counterObjects == null)
			_counterObjects = new ArrayList<CountersModel>(20);

		if (_logger.isDebugEnabled())
			_logger.debug("PersistContainer.add: name="+StringUtil.left(cm.getName(),20)+", timeHead="+cm.getSampleTimeHead()+", sampleTime="+cm.getSampleTime()+", interval="+cm.getSampleInterval());

		// Hmmm, this can probably be done better
		// The _sampleInterval might be to low or to high
		// It should span ThisSampleTime -> untillNextSampleTime
		long cmInterval = cm.getSampleInterval();
		if (_sampleInterval < cmInterval)
			_sampleInterval = cmInterval;
		
		_counterObjects.add(cm.copyForStorage());
	}

	public boolean equals(PersistContainer pc)
	{
		if (pc == null)	         return false;
		if (_mainSampleTime == null) return false;
		
		return _mainSampleTime.equals(pc.getMainSampleTime());
	}
	
	/**
	 * The HEAD sample time might not be the one we campare to<br>
	 * So lets try to see if the Timestamp is within the "head sample time" + sampleInterval
	 * 
	 * @param ts Typically a sampleTime from a CounterModel and not the "head" sample time
	 * @return true if within the interval
	 */
	public boolean equalsApprox(Timestamp ts)
	{
		if (ts == null)	         return false;
		if (_mainSampleTime == null) return false;
		
		if (    _mainSampleTime.equals(ts) 
		     || ( ts.after(_mainSampleTime) && ts.getTime() <= (_mainSampleTime.getTime() + _sampleInterval) ) 
		   )
			return true;
		return false;
	}

	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}

}
