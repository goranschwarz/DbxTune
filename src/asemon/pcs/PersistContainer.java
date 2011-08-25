/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import asemon.CountersModel;

public class PersistContainer
{
//	private static Logger _logger          = Logger.getLogger(PersistContainer.class);

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	protected Timestamp _sampleTime = null;
	protected String    _serverName = null;
	protected List      _counterObjects = null;


	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistContainer(Timestamp sampleTime, String serverName)
	{
		_sampleTime = sampleTime;
		_serverName = serverName;
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public void setSampleTime(Timestamp sampleTime) { _sampleTime = sampleTime; }
	public void setServerName(String serverName)    { _serverName = serverName; }

	public Timestamp getSampleTime() { return _sampleTime; }
	public String    getServerName() { return _serverName; }

	public void add(CountersModel cm)
	{
		if (_counterObjects == null)
			_counterObjects = new ArrayList(20);

		_counterObjects.add(cm);
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
