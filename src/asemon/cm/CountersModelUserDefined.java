/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.cm;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import asemon.CountersModel;

public class CountersModelUserDefined
    extends CountersModel
{
    private static final long serialVersionUID = 505823624208175944L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(CountersModelUserDefined.class);

	/** a Map<Integer, String> that holds a Version and a specific SQL statement */
	private Map _sqlVer = null;


	public CountersModelUserDefined(String nm, String sql, Map sqlVer, List pkList, String[] ccd, String[] ccp, String[] monTables, boolean negativeDiffCountersToZero)
	{
		super(nm, sql, pkList, ccd, ccp, monTables, negativeDiffCountersToZero);
		
		_sqlVer = sqlVer;
	}

	public void initSql()
	{
		int aseVersion = getServerVersion();

		// Treat version specific SQL
		if (_sqlVer != null)
		{
			// Check/get version specific SQL strings
			int     sqlVersionHigh = -1;
			Iterator iter = _sqlVer.entrySet().iterator();
			while(iter.hasNext())
			{
				Integer key = (Integer) iter.next();
				//String  val = (String)  _sqlVer.get(key);
	
				int sqlVersionNumInKey = key.intValue();
	
				// connected aseVersion, go and get the highest/closest sql version string 
				if (aseVersion >= sqlVersionNumInKey)
				{
					if (sqlVersionNumInKey < 12503)
					{
						_logger.warn("Reading User Defined Counter '"+getName()+"' with specialized sql for version number '"+sqlVersionNumInKey+"'. First version number that we support is 12503 (which is Ase Version 12.5.0.3 in a numbered format), disregarding this entry.");
					}
					else
					{
						if (sqlVersionHigh <= sqlVersionNumInKey)
						{
							sqlVersionHigh = sqlVersionNumInKey;
						}
					}
				}
			}
			if (sqlVersionHigh > 0)
			{
				_logger.info("Initializing User Defined Counter '"+getName()+"' with sql using version number '"+sqlVersionHigh+"'.");
	
				String val = (String) _sqlVer.get( new Integer(sqlVersionHigh) );
				setSql(val);
			}
		}
	}
}
