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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.dbxtune.gui.swing.GButton;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class PcsAddDdlObjectDialog
extends JDialog
//extends JFrame
implements ActionListener, FocusListener
{
	private static final long serialVersionUID = 1L;

//	private JButton    _ok             = new JButton("OK");
//	private JButton    _cancel         = new JButton("Cancel");
	private JButton    _close          = new JButton("Close");

	private JLabel               _dbname_lbl           = new JLabel("DB Name");
	private JTextField           _dbname_txt           = new JTextField();

	private JLabel               _objectName_lbl           = new JLabel("Object Name");
	private JTextField           _objectname_txt           = new JTextField();

	private GButton              _addObject_but           = new GButton("Add Object");
	private JLabel               _or_lbl                  = new JLabel("or");
	private GButton              _removeObject_but        = new GButton("Remove Object");

	private PcsAddDdlObjectDialog(Window owner)
	{
//		super();
		super(owner, "PCS: Add Dummy Object to DDL Lookup/Storage", ModalityType.MODELESS);
		initComponents();
		pack();
	}

	public static void showDialog(Window owner)
	{
		PcsAddDdlObjectDialog dialog = new PcsAddDdlObjectDialog(owner);
		dialog.setLocationRelativeTo(owner);
		dialog.setFocus();
		dialog.setVisible(true);
//		dialog.dispose();

		return;
	}

	protected void initComponents()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20"));   // insets Top Left Bottom Right

		setTitle("PCS: Add Dummy Object to DDL Lookup/Storage");

		panel.add(_dbname_lbl,         "");
		panel.add(_dbname_txt,         "pushx, growx, wrap");

		panel.add(_objectName_lbl,     "");
		panel.add(_objectname_txt,     "pushx, growx, wrap");

		panel.add(_addObject_but,      "gap top 20, skip, split, hidemode 3");
		panel.add(_or_lbl,             "gap top 20, hidemode 3");
		panel.add(_removeObject_but,   "gap top 20, skip, split, hidemode 3");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_close,             "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_ok,                "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_cancel,            "tag cancel,                   split, bottom");

		setContentPane(panel);

		// ADD KEY listeners

		// ADD ACTIONS TO COMPONENTS
		_addObject_but     .addActionListener(this);
		_removeObject_but  .addActionListener(this);

		_close            .addActionListener(this);
//		_ok               .addActionListener(this);
//		_cancel           .addActionListener(this);

		// ADD Focus Listeners
		_addObject_but    .addFocusListener(this);
		_removeObject_but  .addFocusListener(this);
		
		// what should be visible...
//		_or_lbl        .setVisible(false);
//		_sendEos_but   .setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: Close ---
		if (_close.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: ADD ---
		if (_addObject_but.equals(source))
		{
			if (PersistentCounterHandler.hasInstance())
			{
				String dbName      = _dbname_txt .getText();
				String objectName  = _objectname_txt .getText();

				if (StringUtil.hasValue(dbName) && StringUtil.hasValue(objectName))
				{
					List<String> tableList = StringUtil.parseCommaStrToList(objectName, true);
					for (String name : tableList)
					{
						PersistentCounterHandler.getInstance().addDdl(dbName, name, this.getClass().getSimpleName());
						SwingUtils.showInfoMessage("Add DDL Done", "called: PersistentCounterHandler.getInstance().addDdl(dbname='" + dbName + "', name='" + name + "', source='" + this.getClass().getSimpleName() + "')");
					}
				}
				else
				{
					SwingUtils.showInfoMessage("No Data", "Both 'dbname' and 'object name' has to be filled in.");
				}
			}
			else
			{
				SwingUtils.showInfoMessage("No PCS", "PersistentCounterHandler.hasInstance() == FALSE");
			}
		}

		// --- BUTTON: REMOVE ---
		if (_removeObject_but.equals(source))
		{
			SwingUtils.showInfoMessage("NOT IMPLEMETED", "Remove: is not yet implemented.");
		}
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		Object source = null;
		if (e != null)
			source = e.getSource();
	}

	/**
	 * Set focus to a good field or button
	 */
	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				//_description_txt.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}
}
