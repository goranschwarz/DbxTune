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
	protected Timestamp _sessionStartTime = null;
	protected Timestamp _sampleTime       = null;
	protected long      _sampleInterval   = 0;
	protected String    _serverName       = null;
	protected List      _counterObjects   = null;



	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistContainer(Timestamp sessionStartTime, Timestamp sampleTime, String serverName)
	{
		_sessionStartTime = sessionStartTime;
		_sampleTime       = sampleTime;
		_serverName       = serverName;
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public void setSampleTime(Timestamp sampleTime) { _sampleTime = sampleTime; }
	public void setServerName(String serverName)    { _serverName = serverName; }

	public Timestamp getSessionStartTime() { return _sessionStartTime; }
	public Timestamp getSampleTime()       { return _sampleTime; }
	public long      getSampleInterval()   { return _sampleInterval; }
	public String    getServerName()       { return _serverName; }

	public CountersModel getCm(String name)
	{
		if (_counterObjects == null)
			return null;

		for (Iterator it = _counterObjects.iterator(); it.hasNext();)
        {
	        CountersModel cm = (CountersModel) it.next();
	        if (cm.getName().equals(name))
	        	return cm;
        }
		return null;
	}

	public void add(CountersModel cm)
	{
		if (_counterObjects == null)
			_counterObjects = new ArrayList(20);

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
		if (_sampleTime == null) return false;
		
		return _sampleTime.equals(pc.getSampleTime());
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
		if (_sampleTime == null) return false;
		
		if (    _sampleTime.equals(ts) 
		     || ( ts.after(_sampleTime) && ts.getTime() <= (_sampleTime.getTime() + _sampleInterval) ) 
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
