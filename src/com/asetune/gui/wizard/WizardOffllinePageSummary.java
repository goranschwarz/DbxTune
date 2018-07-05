/**
 * @author <a href="mailto:larrei@gmail.com">Reine Lindqvist</a>
 */
package com.asetune.gui.wizard;

import java.io.File;
import java.util.Map;

import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;

import com.asetune.Version;

/**
 * @author qlarrei
 *
 */
public class WizardOffllinePageSummary implements WizardResultProducer {

    private static final long serialVersionUID = 1L;
//	private static Logger _logger          = Logger.getLogger(WizardOffllinePageSummary.class);

	/* (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#cancel(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	public boolean cancel(Map arg0) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#finish(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	public Object finish(Map wizardData) throws WizardException {
		// We will just return the wizard data here. In real life we would
		// a compute a result here
		Summary  summary;
		String   fn       = (String)wizardData.get("storeFile");
		String   fnDir    = null;
		
		try{ 
			File f = new File(fn);
			if( f.exists() )
			{
				fnDir = f.getAbsolutePath();
			}
		} catch (Exception ignore){}

		String msg = 
		         "The file '" + (fnDir!=null?fnDir:fn) + "' is now produced.\n"
		       + "\n"
		       + "To start collect counter data without the GUI, as a background process, execute:\n"
		       + Version.getAppName().toLowerCase() + " --noGui "+ (fnDir!=null?fnDir:fn) +  "\n"
		       + "\n"
		       + "This config file, along with the runtime jars, can now be shipped to a remote \n"
		       + "site where the DBMS is located, which you want to collect counter data from.\n"
		       + "\n"
		       + "The h2 database files can later be zipped and sent back for analysis.\n"
		       + "\n"
		       + "To view the data in the database:\n"
		       + "- Start "+Version.getAppName().toLowerCase()+" in normal mode\n"
		       + "- Press the connect buttom\n"
		       + "- Choose tab 'Offline Connect'\n"
		       + "- Specify the file in 'JDBC Url' or use buttom '...' to locate the file\n" 
		       + "- Press OK\n" 
		       + "- Now a Tree Table is displayed, start to view/analyze the data\n" 
		       + "";
		
		summary = Summary.create (msg, wizardData);


        return summary;
	}

}
