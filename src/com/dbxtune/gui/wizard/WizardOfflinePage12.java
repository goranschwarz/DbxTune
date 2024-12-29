/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

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

import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage12
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

	public WizardOfflinePage12()
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