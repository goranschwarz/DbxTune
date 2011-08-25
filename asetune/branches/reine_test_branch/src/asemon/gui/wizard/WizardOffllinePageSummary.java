/**
 * @author <a href="mailto:larrei@gmail.com">Reine Lindqvist</a>
 */
package asemon.gui.wizard;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;

/**
 * @author qlarrei
 *
 */
public class WizardOffllinePageSummary implements WizardResultProducer {

    private static final long serialVersionUID = 1L;
	private static Logger _logger          = Logger.getLogger(WizardOffllinePageSummary.class);

	/* (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#cancel(java.util.Map)
	 */
	public boolean cancel(Map arg0) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#finish(java.util.Map)
	 */
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

		String msg = ""	+ "The file '" 
		       + (fnDir!=null?fnDir:fn) + "' is now produced\n"
		       + "You should perform the following steps.\n"
		       + "1. Make sure the script asemon_nogui.bat refers to this file.\n"
		       + "2. Invoke asemon_nogui.bat.";
		summary = Summary.create (msg, wizardData);


        return summary;
	}

}
