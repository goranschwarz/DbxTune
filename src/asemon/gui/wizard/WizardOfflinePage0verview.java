/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import asemon.gui.swing.MultiLineLabel;

public class WizardOfflinePage0verview
extends WizardPage
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "overview";
	private static final String WIZ_DESC = "Overview";
//	private static final String WIZ_HELP = "FIXME: Describes what this Wizard is all about.";

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage0verview()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		// Add a helptext
		String text = "<html>" +
			"This wizard is simply used to create a file.<br>" +
			"That file will be used by AseMon when you <b>don't</b> want to start the GUI.<br>" +
			"<br>" +
			"The file contains information about <b>what</b> Performance Counter you want to sample.<br>" +
			"Example of how to start AseMon in no-gui mode:<br>" +
			"<code>asemon --noGui theFilenameProducedByThisWizard</code><br>" +
			"<br>" +
			"Type '<code>asemon --help</code>' to get information about all start options.<br>" +
			"For example '<code>-Uuser -Ppasswd -Sserver -dsampleDB</code>' overrides the ones specified in the in-file.<br>" +
			"<br>" +
			"This feature can for example be used by You or any TechSupport personnel<br>" +
			"&nbsp; 1: TechSupport creates the file of what to sample (using this wizard).<br>" +
			"&nbsp; 2: The file is sent to the Customer.<br>" +
			"&nbsp; 3: Customer starts AseMon in offline/no-gui mode and collects counters.<br>" +
			"&nbsp; 4: Customer sends back the DB file where Performance Counters are stored.<br>" +
			"&nbsp; 5: TechSupport starts AseMon in GUI mode, load the database and analyzes.<br>" +
			"<br>" +
			"Of course TechSupport can be <b>you</b> sending of the file to a friend that wants help :)<br>" +
			"</html>";

		String note = "<html>" +
			"<b>NOTE:</b> The default database engine to store Performance Counters is " +
			"H2, http://www.h2database.com. This is a java database engine that is shipped " +
			"with AseMon. But why didn't you choose to store the info in a Sybase database?<br>" +
			"Well H2 gives you platform portability for free, so it's OK to start AseMon " +
			"on Solaris, HP or Linux, store counters locally on the server. " +
			"Then transfer the database file to Windows or whatever client platform you are " +
			"sitting on, and start to do your analysis.<br>" +
			"<br>" +
			"But if you want to store counters in a Sybase database, or somewhere else; " +
			"just specify a JDBC Driver and a URL, hopefully tables " +
			"</html>";

		add( new MultiLineLabel(text), "grow, wrap 15" );
		add( new MultiLineLabel(note), "push, bottom, wrap" );

		//add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );
	}
}

