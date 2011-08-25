/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import asemon.cm.CountersModel;
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

	public void close();	
	
	/** When we start a new session, lets call this method to get some 
	 * idea what we are about to sample. 
	 * @param cont a PersistContainer filled with <b>all</b> the available
	 *             CounterModels we could sample.
	 */
	public void startSession(PersistContainer cont);
	
	/**
	 * Save a bunch of CM's that we have sampled during this sample interval.
	 * @param cont
	 */
	public void saveSample(PersistContainer cont);
	
	public boolean isDdlCreated(CountersModel cm); 
	public void    markDdlAsCreated(CountersModel cm);
	public boolean saveDdl(CountersModel cm);

	/** save counters for this cm */
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
