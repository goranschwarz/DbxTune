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
package com.dbxtune.tools.sqlcapture;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.dbxtune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class ChangeSqlCaptureSettingsDialog
extends JDialog
implements ActionListener
{
	private static final long serialVersionUID = 1L;

	// PANEL: OK-CANCEL
	private JButton _ok             = new JButton("OK");
	private JButton _cancel         = new JButton("Cancel");
	private JButton _apply          = new JButton("Apply");
	private int     _dialogReturnSt = JOptionPane.CANCEL_OPTION;

	private ChangeSqlCaptureSettingsPanel   _changeSqlCapturePanel = null;
	private Configuration                   _conf                  = null;
	private String                          _topMessage            = null;

	/**
	 * Show a dialog
	 * 
	 * @param owner Gui owner
	 * @param conf A Configuration object (on OK or Apply, it will saved to this object)  , if null you can use the returned object to do whatever you like with.
	 * @param topMessage A Message to be printer oa the top of the dialog (This can be html), If null no message will be displayed
	 * @return A Configuration object, or null of cancel was pressed.
	 */
	public static Configuration showDialog(Window owner, Configuration conf, String topMessage)
	{
		ChangeSqlCaptureSettingsDialog dialog = new ChangeSqlCaptureSettingsDialog(owner, conf, topMessage);

		dialog.setVisible(true);
		Configuration retConf = dialog._changeSqlCapturePanel.getConfiguration();

		dialog.dispose();
		
		if ( dialog._dialogReturnSt == JOptionPane.CANCEL_OPTION)
			return null;
		
		return retConf;
	}

	public ChangeSqlCaptureSettingsDialog(Window owner, Configuration conf, String topMessage)
	{
		super(owner, "SQL Capture Settings", ModalityType.DOCUMENT_MODAL);
		
		_conf       = conf;
		_topMessage = topMessage;

		initComponents();
		pack();

		setLocationRelativeTo(owner);
	}

	protected void initComponents() 
	{
		setLayout(new MigLayout());   // insets Top Left Bottom Right

		add(createMainPanel(), "grow, push, span");
//		add(createOkPanel(),   "gap top 20, right");
		add(createOkPanel(),   "right");

		// Initial state for buttons
//		_apply.setEnabled(false);
		_apply.setEnabled( _conf != null );
	}

	private JPanel createMainPanel()
	{
		_changeSqlCapturePanel = new ChangeSqlCaptureSettingsPanel(_conf, _topMessage);
		return _changeSqlCapturePanel;
	}

	private JPanel createOkPanel()
	{
		// ADD the OK, Cancel, Apply buttons
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0","",""));
		panel.add(_ok,     "tag ok");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");
		
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);
		_apply .addActionListener(this);

		return panel;
	}

	private void apply()
	{
		if (_conf != null)
		{
			_changeSqlCapturePanel.setToConfoguration(_conf);
		}
		//_apply.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}
	}
}
