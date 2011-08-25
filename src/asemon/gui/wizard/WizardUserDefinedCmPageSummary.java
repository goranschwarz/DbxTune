/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import asemon.gui.swing.MultiLineLabel;
import asemon.utils.Configuration;


public class WizardUserDefinedCmPageSummary
extends WizardPage
//implements ActionListener
{
    private static final long serialVersionUID = 1L;
//	private static Logger _logger          = Logger.getLogger(WizardUserDefinedCmPage2.class);

	private static final String WIZ_NAME = "Apply";
	private static final String WIZ_DESC = "Apply or Store";
	private static final String WIZ_HELP1 = "Here is the property entries you need to add to the configuration file to create a User Defined Counter Model.\nYou can either copy paste the text into the configuration file or you can check the 'Append to Config File' checkbox to append the text at the end of the config file.";
//	private static final String WIZ_HELP2 = "Add the Counter Model to the GUI, NOTE: this will NOT be saved in the configuration file.";

	private boolean    _firtsTimeRender = true;

	private JTextArea	_props_txt       = new JTextArea();
	private JCheckBox	_appendToCfg_chk = new JCheckBox("Append to Config file", false);
	private JCheckBox	_addTmpToCfg_chk = new JCheckBox("Add Temporary to GUI", false);

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPageSummary()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		String cfgFile = Configuration.getInstance(Configuration.USER_CONF).getFilename();
		setLayout(new MigLayout("", "[grow]", ""));

		_props_txt      .setName("propText");
		_appendToCfg_chk.setName("appendToCfg");
		_addTmpToCfg_chk.setName("addTmpToCfg");
		
		add( new MultiLineLabel(WIZ_HELP1), "wmin 100, span, pushx, growx, wrap" );
		add(new JScrollPane(_props_txt), "growx, pushx, height 100%, wrap");

//		JButton button;
//		button = new JButton("Append to Config file");
//		button.setToolTipText("Append the above text to the end of the configuration file '"+cfgFile+"'. The GUI has to be restarted for the changes to take affect.");
//		button.addActionListener(this);
//		button.putClientProperty("NAME", "BUTTON_APPEND");
//		add(button, "wrap 10");
//
//		add( new MultiLineLabel(WIZ_HELP2), "wmin 100, span, pushx, growx, wrap" );
//
//		button = new JButton("Add Temporary to GUI");
//		button.setToolTipText("Add the Counter Model to the GUI, NOTE: this will NOT be saved in the configuration file.");
//		button.addActionListener(this);
//		button.putClientProperty("NAME", "BUTTON_ADD");
//
//		add(button, "wrap 10");

		_appendToCfg_chk.setToolTipText("Append the above text to the end of the configuration file '"+cfgFile+"'. The GUI has to be restarted for the changes to take affect.");
		add(_appendToCfg_chk, "wrap");

		_addTmpToCfg_chk.setToolTipText("Add the Counter Model to the GUI, NOTE: this will NOT be saved in the configuration file.");
		add(_addTmpToCfg_chk, "wrap");
		
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		if ( !_appendToCfg_chk.isSelected() && !_addTmpToCfg_chk.isSelected() )
		{
			return "One of the check boxes must be selected.";
		}

		if (_appendToCfg_chk.isSelected())
		{
//			Configuration conf = Configuration.getInstance(Configuration.CONF);
			Configuration conf = Configuration.getCombinedConfiguration();
			String cfgFile = conf.getFilename();
			
			if (cfgFile == null || (cfgFile != null && cfgFile.trim().length() == 0) )
				return "The Configuration has no config file attatched.";

			String udcName = (String) getWizardDataMap().get("name");
			String key     = "udc." + udcName + ".name";

			String exists  = conf.getProperty(key);
			if (exists != null)
			{
				return "The key '"+key+"' already exists in the file '"+cfgFile+"'.";
			}
		}
		
		return null;
	}

	/** Called when we enter the page */
	@SuppressWarnings("unchecked")
	@Override
	protected void renderingPage()
    {
		_props_txt.setText(WizardUserDefinedCmResultProducer.createPropsStr( getWizardDataMap() ));
		if (_firtsTimeRender)
		{
		}
	    _firtsTimeRender = false;
    }

//	public void actionPerformed(ActionEvent ae)
//	{
//		JComponent src = (JComponent) ae.getSource();
//		String name = (String)src.getClientProperty("NAME");
//		if (name == null)
//			name = "-null-";
//
//		System.out.println("Source("+name+"): " + src);
//
//		if (name.equals("BUTTON_APPEND"))
//		{
//			Configuration conf = Configuration.getInstance(Configuration.CONF);
//			String cfgFile = conf.getFilename();
//
//			String udcName = getKey("name", getWizardDataMap());
//			String key     = "udc." + udcName + ".name";
//			String exists  = conf.getProperty(key);
//			if (exists != null)
//			{
//				JOptionPane.showMessageDialog(
//						this, 
//						"The key '"+key+"' already exists in the file '"+cfgFile+"'.",
//						"Error", JOptionPane.ERROR_MESSAGE);
//			}
//			else
//			{
////				// APPEND TO FILE
////				conf.append(_props_txt.getText());
////				// merge the properties into current configuration...
////				conf.add(createConf(getWizardDataMap());
//			}
//		}
//		if (name.equals("BUTTON_ADD"))
//		{
//			try
//			{
//				Configuration conf = createConf(getWizardDataMap());
//				int failCount = GetCounters.createUserDefinedCounterModels(conf);
//				
//				if (failCount != 0)
//				{
//					MainFrame.openLogViewer();
//				}
//			}
//			catch(Throwable t)
//			{
//				MainFrame.openLogViewer();
//				SwingUtils.showErrorMessage("Adding User Defined Counter", 
//						"Problems when adding the User Defined Counter.\n\n" 
//							+ t.getMessage(), t);
//			}
//		}
//
//	}
}
