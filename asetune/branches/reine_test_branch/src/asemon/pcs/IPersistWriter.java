/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import asemon.CountersModel;
import asemon.utils.Configuration;

/**
 * Implement you'r own forwarding of Alarms to various subsystems.
 * 
 * @author Goran Schwarz
 */
public interface IPersistWriter 
{
	/**
	 * The init() method will be called
	 * so it can configure itself. Meaning reading the "props" and initialize itself.
	 * 
	 * @param props The Configuration (basically a Properties object)
	 * @throws Exception when the initialization fails, you will 
	 *         throw an Exception to tell what was wrong
	 */
	public void init(Configuration props)	
	throws Exception;

	public void saveSession(PersistContainer cont);
	
	public boolean isDdlCreated(CountersModel cm); 
	public void    markDdlAsCreated(CountersModel cm);
	public boolean saveDdl(CountersModel cm);

	public void saveCounters(CountersModel cm);

	/** Before each "sample loop" this method will be called. */
	public void beginOfSample();
	/** After each "sample loop" this method will be called. */
	public void endOfSample();
	
	/**
	 * The writer has to have some cind of name...
	 * 
	 * @return name of the Writer
	 */
	public String getName();
}
