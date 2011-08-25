/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.cm;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


public class CountersModelUserDefined
    extends CountersModel
{
    private static final long serialVersionUID = 505823624208175944L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(CountersModelUserDefined.class);

	/** a Map<Integer, String> that holds a Version and a specific SQL statement */
	private Map<Integer,String> _sqlVerStr = null;


	public CountersModelUserDefined
	(
			String              name,             // Name of the Counter Model
			String              sql,              // SQL Used to grab a sample from the counter data
			Map<Integer,String> sqlVerStr,        // a Map<Integer, String> that holds a Version and a specific SQL statement
			List<String>        pkList,           // A list of columns that will be used during diff calculations to lookup values in previous samples
			String[]            diffColumns,      // Columns to do diff calculations on
			String[]            pctColumns,       // Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
			String[]            monTables,        // What monitor tables are accessed in this query, used for TOOLTIP lookups
			String[]            dependsOnRole,    // what roles do we need
			String[]            dependsOnConfig,  // Check that these configurations are above 0
			int                 dependsOnVersion, // What version of ASE do we need to sample for this CounterModel
			int                 dependsOnCeVersion, // What version of ASE-CE do we need to sample for this CounterModel
			boolean             negativeDiffCountersToZero // if diff calculations is negative, reset the counter to zero.
	)
	{
		super(name, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, false);
		
		_sqlVerStr = sqlVerStr;
	}

	public void initSql()
	{
		int aseVersion = getServerVersion();

		// Treat version specific SQL
		if (_sqlVerStr != null)
		{
			// Check/get version specific SQL strings
			int     sqlVersionHigh = -1;
			for (Map.Entry<Integer,String> entry : _sqlVerStr.entrySet()) 
			{
				Integer key = entry.getKey();
				//String val = entry.getValue();
				//do stuff here
	
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
	
				String val = (String) _sqlVerStr.get( new Integer(sqlVersionHigh) );
				setSql(val);
			}
		}
	}
}
