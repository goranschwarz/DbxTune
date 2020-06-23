/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModelAppend;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class TabularCntrPanelAppend 
extends TabularCntrPanel
{
	private static final long serialVersionUID = 1L;
//	private static Logger _logger = Logger.getLogger(TabularCntrPanelAppend.class);

	private JCheckBox _showAllRows_chk;

	public TabularCntrPanelAppend(CountersModelAppend cm)
	{
		super(cm);
		init();
	}

    private void init()
    {
    }
    
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		final CountersModelAppend cm = (CountersModelAppend) getCm();
		
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		_showAllRows_chk = new JCheckBox("Show All Rows");

		_showAllRows_chk.setToolTipText("<html>Show <b>all</b> records in the table, or just the last records fetched.</html>");
		_showAllRows_chk.setSelected( cm.showAllRecords() );
		
		_showAllRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				cm.setShowAllRecords( ((JCheckBox)e.getSource()).isSelected() );
				cm.fireTableDataChanged();
			}
		});
		
		// ADD to panel
		panel.add(_showAllRows_chk,   "wrap");

		return panel;
	}

//	@Override
//	public void checkLocalComponents()
//	{
//	}
	
}
