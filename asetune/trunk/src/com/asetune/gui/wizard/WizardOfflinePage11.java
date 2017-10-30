/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage11
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "filename";
	private static final String WIZ_DESC = "Filename";
	private static final String WIZ_HELP = "A filename where this information should be stored to.";

	private JTextField _storeFile = new JTextField("");
	private JCheckBox  _previewFile = new JCheckBox("Preview the output file when closing the wizard", true);

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage11()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		_storeFile.setName("storeFile");

		add(new JLabel("Filename"));
		add(_storeFile, "growx");
		JButton button = new JButton("...");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_STORE_FILE");
		add(button, "wrap");

		add(_previewFile, "span, wrap");

//		add(new JLabel("Filename 23"));
//		add(new JTextField(), "growx");
//		add(new JButton(",,,"), "wrap");
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
		if ( _storeFile.getText().trim().length() <= 0) problem += "Filename, ";

		putWizardData("to-be-discarded.previewFile", _previewFile.isSelected()+"");

		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		
		return problem.length() == 0 ? null : "Following fields cant be empty: "+problem;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (name.equals("BUTTON_STORE_FILE"))
		{
//			String envNameSaveDir    = DbxTune.getInstance().getAppSaveDirEnvName();  // ASETUNE_SAVE_DIR
			String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
			String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

			JFileChooser fc = new JFileChooser();
			if (envNameSaveDirVal != null)
				fc.setCurrentDirectory(new File(envNameSaveDirVal));

			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
				File file = fc.getSelectedFile();

				//This is where a real application would open the file.
				String filename = file.getAbsolutePath();
				//System.out.println("Opening '" + filename + "'.");

				_storeFile.setText( filename );
	        }
		}
	}
}