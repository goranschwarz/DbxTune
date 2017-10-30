package com.asetune.gui.swing;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class PromptForPassword
{

	/**
	 * Simply opens up a dialog to enter password
	 * 
	 * @param parent  Parent component (can be null)
	 * @param msg     Message to print on top of dialog (can be null)
	 * @param user    Hostname to connect to (can be null, then not visible)
	 * @param user    User to get password for
	 * 
	 * @return The password, or null if "Cancel" was pressed
	 */
	public static String show(Component parent, String msg, String hostname, String user)
	{
		JPanel panel = new JPanel(new MigLayout());

		JLabel         msg_lbl = new JLabel(msg);

		JLabel         host_lbl = new JLabel("Hostname:");
		JTextField     host_txt = new JTextField(hostname, 30);

		JLabel         user_lbl = new JLabel("User:");
		JTextField     user_txt = new JTextField(user, 20);

		JLabel         passwd_lbl = new JLabel("Password:");
		JPasswordField passwd_txt = new JPasswordField();
				
		JCheckBox      savePasswd_chk = new JCheckBox("Save Password", false);

		// Use is read only
		host_txt.setEnabled(false);
		user_txt.setEnabled(false);

		if (hostname == null)
		{
			host_lbl.setVisible(false);
			host_txt.setVisible(false);
		}
		
		if (msg != null)
		{
			panel.add(msg_lbl, "span, wrap 10");
		}
		panel.add(host_lbl,   "hidemode 3");
		panel.add(host_txt,   "growx, pushx, hidemode 3, wrap");

		panel.add(user_lbl,   "");
		panel.add(user_txt,   "growx, pushx, wrap");

		panel.add(passwd_lbl, "");
		panel.add(passwd_txt,  "growx, pushx, wrap");
		
		panel.add(savePasswd_chk, "skip, wrap");

		// Set focus of the password field
		SwingUtils.setFocus(passwd_txt);
		
		String[] options = new String[]{"OK", "Cancel"};
		int option = JOptionPane.showOptionDialog(parent, panel, 
				"Enter Password for user '"+user+"'.",
				JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
//				null, options, options[0]);
				null, options, null);

		// Note: if we want both: FocusOnPasswdField and <return> in passwdFilds to press OK, then we need to do more work (probably our own dialog with more controll)
		
		if(option == 0) // pressing OK button
		{
			String password = new String(passwd_txt.getPassword());

			// Save the password / or remove it...
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				String key = "PromptForPassword.saved."+hostname+"."+user;
    			if (savePasswd_chk.isSelected())
    			{
    				// Save the password ENCRYPTED
					conf.setProperty(key, password, true);
    			}
    			else
    			{
    				// If we dont want to save it, lets remove any password previously saved on this key
    				conf.remove(key);
    			}

    			conf.save();
			}
			return password;
		}
		return null;
	}

	/**
	 * Get the saved password for host/user
	 * @param hostname
	 * @param user
	 * @return null if it hasn't been saved, or the password
	 */
	public static String getSavedPassword(String hostname, String user)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String key = "PromptForPassword.saved."+hostname+"."+user;
		return conf.getProperty(key, null);
	}
}
