/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.Version;
import com.asetune.gui.swing.MultiLineLabel;


public class WizardUserDefinedCmPage0verview
extends WizardPage
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "overview";
	private static final String WIZ_DESC = "Overview";
//	private static final String WIZ_HELP = "FIXME: Describes what this Wizard is all about.";

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage0verview()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout("", "", ""));

		// Add a helptext
		String text = "<html>" +
			"This wizard is used to create your own Performance Counter Tab.<br>" +
			"The Tab will of course be part of the GUI and showed along with the other Tabs<br>" +
			"<br>" +
			"If you have your own SQL Query that fetches information from the MDA tables " +
			"or other tables, this is the place to do it.<br>" +
			"<br>" +
			"If your application has Performance Counters stored in a database table " +
			"and you want to correlate those with the ASE counters...<br>" +
			"Go ahead and compose the query.<br>" +
			"Why not also have a Graph in the Summary Tab, that reflects the counters :)<br>" +
			"<br>" +
			"It's all done in here...<br>" +
			"</html>";

		String note = "<html>" +
			"NOTE: If you create any <b>good</b> Collector that works on the ASE level and " +
			"you want it to be part of "+Version.getAppName()+", just send me the Configuration file, or the properties " +
			"that defines the Collector. And hopefully it will be in the next release.<br>" +
			"Send it to: goran_schwarz@hotmail.com<br>" +
			"</html>";

		add( new MultiLineLabel(text), "grow, wrap" );
		add( new MultiLineLabel(note), "push, bottom, wrap" );
	}
}

