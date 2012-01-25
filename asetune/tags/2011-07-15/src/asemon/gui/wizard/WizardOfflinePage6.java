/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import asemon.gui.swing.MultiLineLabel;

//PAGE 4
public class WizardOfflinePage6
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "sample-time";
	private static final String WIZ_DESC = "Sample time";
	private static final String WIZ_HELP = "How long should we sleep between samples.\nThis is specified in seconds.";

	private JTextField _sampleTime          = new JTextField("60");
	private JTextField _shutdownAfterXHours = new JTextField("");

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage6()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_sampleTime.setName("sampleTime");
		_shutdownAfterXHours.setName("shutdownAfterXHours");

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(new JLabel("Sample time"));
		add(_sampleTime, "growx");
		add(new JLabel("Seconds"), "wrap 40");


		String label = "<html>Shutdown or stop the no-gui process after X number after it has been started.</html>";

		add( new MultiLineLabel(label), "span, wrap 10" );
		add(new JLabel("Shutdown after # hours"));
		add(_shutdownAfterXHours, "growx");
		add(new JLabel("Hours"));


		
		
		initData();
	}

	private void initData()
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
		if ( _sampleTime.getText().trim().length() <= 0) problem += "Sample time, ";

		// Check if it's a integer
		if (_sampleTime.getText().trim().length() > 0)
		{
			try { Integer.parseInt( _sampleTime.getText().trim() ); }
			catch (NumberFormatException e)
			{
				return "Sample time needs to be a number.";
			}
		}
		
		// Check if _shutdownAfterXHours is integer
		if (_shutdownAfterXHours.getText().trim().length() > 0)
		{
			try { Integer.parseInt( _shutdownAfterXHours.getText().trim() ); }
			catch (NumberFormatException e)
			{
				return "'Shutdown after # hours' needs to be a number or empty.";
			}
		}
		
		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		
		return problem.length() == 0 ? null : "Following fields cant be empty: "+problem;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);
	}
}
