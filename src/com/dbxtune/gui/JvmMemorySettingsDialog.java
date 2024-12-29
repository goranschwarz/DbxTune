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
package com.dbxtune.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class JvmMemorySettingsDialog
extends JDialog
implements ActionListener, FocusListener
{
	private static Logger _logger = Logger.getLogger(JvmMemorySettingsDialog.class);
	private static final long serialVersionUID = 1L;

	private Window             _owner                    = null;
	private String             _envPrefix                = null;
	private String             _filename                 = null;

	private String             _appName                  = null;
	
	private JButton            _ok                       = new JButton("OK");
	private JButton            _cancel                   = new JButton("Cancel");
	private JLabel             _warning_lbl              = new JLabel("Warning messages, goes in here");

	private JPanel             _infoPanel                = null;
	private JLabel             _memInfo_lbl              = new JLabel();
	private JLabel             _reboot_lbl               = new JLabel();
	private JLabel             _jvmMemory_lbl            = new JLabel("JVM Memory Switches");
	private JTextField         _jvmMemory_txt            = new JTextField("");
	private JLabel             _jvmGc_lbl                = new JLabel("JVM Garbage Collection Switches");
	private JTextField         _jvmGc_txt                = new JTextField("");

	private int                _dialogReturnSt           = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;
	
	private static final String _envMem = "JVM_MEMORY_PARAMS";
	private static final String _envGc  = "JVM_GC_PARAMS";

	private static String _appEnvMem = "SET_LATER";
	private static String _appEnvGc  = "SET_LATER";



	public static int showDialog(Frame owner, String appName, String filename)
	{
		JvmMemorySettingsDialog dialog = new JvmMemorySettingsDialog(owner, appName, filename);

		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		
		return dialog._dialogReturnSt;
	}
	public JvmMemorySettingsDialog(Frame owner, String appName, String filename)
	{
		super(owner, "Java/JVM Memory Parameters", true);

		if (StringUtil.isNullOrBlank(appName) || StringUtil.isNullOrBlank(filename))
			throw new IllegalArgumentException("JvmMemorySettingsDialog: The params appName='"+appName+"' and filename='"+filename+"', must have some values.");

		_owner        = owner;
		_appName      = appName;
		_filename     = filename;
		
		_envPrefix = "DBXTUNE";

		_appEnvMem = _envPrefix + "_" + _envMem;
		_appEnvGc  = _envPrefix + "_" + _envGc;

		init();
	}

	private void init()
	{
		setContentPane(createPanel());
		loadProps();
		
		pack();
//		SwingUtils.setLocationCenterParentWindow(null, this);
		setLocationRelativeTo(_owner);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
	}

	private JPanel createPanel()
	{
		JPanel panel = new JPanel();
		// panel.setLayout(new MigLayout("","","")); // insets Top Left Bottom
		// Right
		panel.setLayout(new MigLayout("insets 10 10", "", "")); // insets Top Left Bottom Right

		_warning_lbl  .setToolTipText("Why we can't press OK.");
		_warning_lbl  .setForeground(Color.RED);
		_warning_lbl  .setHorizontalAlignment(SwingConstants.RIGHT);
		_warning_lbl  .setVerticalAlignment(SwingConstants.BOTTOM);

		// Add panel
		_infoPanel  = createInfoPanel();
		panel.add(_infoPanel, "push, grow, wrap");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_warning_lbl,  "split, pushx, growx");
		panel.add(_ok,           "tag ok,     gap top 20, split, bottom, right, push");
		panel.add(_cancel,       "tag cancel, split, bottom");

		// ADD ACTIONS TO COMPONENTS
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		return panel;
	}
	

	private JPanel createInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("JVM Mememory Settings", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		// Tooltip
		panel          .setToolTipText("JVM Memory information.");
		_jvmMemory_lbl .setToolTipText("<html>For example: '-Xmx4096m -Xms64m' to use maximum 4GB of memory, that gets allocated in 64MB chunks.</html>");
		_jvmMemory_txt .setToolTipText(_jvmMemory_lbl.getToolTipText());
		_jvmGc_lbl     .setToolTipText("<html>For example: '-XgcPrio:throughput' to get maximum througput during GC.</html>");
		_jvmGc_txt     .setToolTipText(_jvmGc_lbl.getToolTipText());

		int jvmBitSize = StringUtil.parseInt(System.getProperty("sun.arch.data.model", "32"), 32);
		_memInfo_lbl.setText("<html>"
				+ "Available Memory on this Machine:"
				+ "<ul>"
				+ "  <li>Total Physical Memory="+Memory.getTotalPhysicalMemorySizeInMB()+" MB</li>"
				+ "  <li>Free Physical Memory="+Memory.getFreePhysicalMemorySizeInMB()+" MB</li>"
				+ "</ul>"
				+ "<br>"
				+ "This is a "+jvmBitSize+" bit Java/JVM, on a " 
					+ (jvmBitSize < 64 ? 
							"32 bit can only use 1-2 GB of memory<br>Try to download a 64 bit JVM, which gives you more memory..." : 
							"64 bit you can use <i>all</i> the 'free' memory<br>")
				+ "<br>"
				+ "The settings are stored in file '"+_filename+"'<br>"
				+ "<br>"
				+ "For JVM memory parameters examples, search the web for 'java memory settings'.<br>"
				+ "<br>"
				+ "Setting the fields to <i>empty</i> will make the application use it's default values.<br>"
				+ "<br>"
				+ "</html>");
		_reboot_lbl .setText("<html><br><b>Note</b>: The application needs to be restarted for the parameters to take effect.</html>");

		panel.add(_memInfo_lbl,        "span 2, grow, wrap 10");

		panel.add(_jvmMemory_lbl,     "");
		panel.add(_jvmMemory_txt,     "growx, pushx, wrap");

		panel.add(_jvmGc_lbl,         "");
		panel.add(_jvmGc_txt,         "growx, pushx, wrap 10");

		panel.add(_reboot_lbl,        "span 2, grow, wrap");
		_reboot_lbl.setForeground(Color.RED);

		String defMem = System.getenv(_envMem);
		String defGc  = System.getenv(_envGc);

//DBXTUNE_JVM_MEMORY_PARAMS=-Xmx4996m -Xms64m
//DBXTUNE_JVM_GC_PARAMS=
		
		if (StringUtil.isNullOrBlank(defMem)) defMem = "-Xmx####m -Xms64m";
		if (StringUtil.isNullOrBlank(defGc))  defGc  = "";

		// initial values
		_jvmMemory_lbl.setText(_appEnvMem);
		_jvmMemory_txt.setText(defMem);
		_jvmGc_lbl    .setText(_appEnvGc);
		_jvmGc_txt    .setText(defGc);

		// for validation
		_jvmMemory_txt.addActionListener(this);
		_jvmGc_txt    .addActionListener(this);

		// Focus action listener
		_jvmMemory_txt.addFocusListener(this);
		_jvmGc_txt    .addFocusListener(this);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e==null ? null : e.getSource();

		// BUT: OK
		if (_ok.equals(source))
		{
			try
			{
				saveFile();
			}
			catch(Exception ex)
			{
				SwingUtils.showErrorMessage(_owner, "Errors when writing to file", "Problems writing to file '"+_filename+"'.", ex);
				return;
			}
			saveProps();
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}
		
		// BUT: CANCEL
		if (_cancel.equals(source))
		{
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}
		
		validateInput();
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		validateInput();
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		validateInput();
	}
	
	private boolean validateInput()
	{
		String warn = "";

		if (_jvmMemory_txt.getText().contains("#"))
		{
			warn = "Memory con not contain '#', replace it with a number";
		}

		_warning_lbl.setText(warn);

		boolean ok = StringUtil.isNullOrBlank(warn);
		_ok.setEnabled(ok);

		return ok;
	}

	private void saveFile() throws IOException, FileNotFoundException
	{
		// Create the fullpath (directories) to the file AND the file itself.
		Path pathToFile = Paths.get(_filename);
		Files.createDirectories(pathToFile.getParent());
		Files.createFile(pathToFile);
		
		PrintWriter out = new PrintWriter(_filename);

		String jvmMemVal = _jvmMemory_txt.getText().trim();
		String jvmGcVal  = _jvmGc_txt    .getText().trim();

		_logger.info("Setting '" + _appEnvMem + "' to '" + jvmMemVal + "' in the file '" + _filename + "'.");
		_logger.info("Setting '" + _appEnvGc  + "' to '" + jvmGcVal  + "' in the file '" + _filename + "'.");
		
		out.println(_appEnvMem + "=" + jvmMemVal);
		out.println(_appEnvGc  + "=" + jvmGcVal);
		out.close();
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// Save generic or last one used
//		conf.setProperty("rs.connection.ra.username",     _raUsername_txt.getText());
//		conf.setProperty("rs.connection.ra.password",     _raPassword_txt.getText());
//		conf.setProperty("rs.connection.ra.savePassword", _raPassword_chk.isSelected());
//
//		// Save for this specific server.db
//		conf.setProperty("rs.connection."+_name+".ra.username",     _raUsername_txt.getText());
//		conf.setProperty("rs.connection."+_name+".ra.password",     _raPassword_txt.getText());
//		conf.setProperty("rs.connection."+_name+".ra.savePassword", _raPassword_chk.isSelected());
//
//		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// first: LOAD for this specific server.db, then use FALLBACK "last used"
		String str;
		
		// USER
//		str = conf.getProperty("rs.connection."+_name+".ra.username");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.username", "sa");
//		_raUsername_txt.setText(str);
//
//		// PASSWORD
//		str = conf.getProperty("rs.connection."+_name+".ra.password");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.password", "");
//		_raPassword_txt.setText(str);
//
//		// SAVE PASSWORD
//		str = conf.getProperty("rs.connection."+_name+".ra.savePassword");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.savePassword", "true");
//		_raPassword_chk.setSelected(Boolean.parseBoolean(str));
	}
}
