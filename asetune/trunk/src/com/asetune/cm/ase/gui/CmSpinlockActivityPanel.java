/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmSpinlockActivity;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmSpinlockActivityPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmSpinlockActivityPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSpinlockActivity.CM_NAME;

	private JCheckBox _sampleSpinlockSlotID_chk;

	public static final String  TOOLTIP_sample_SpinlockSlotID = 
		"<html>" +
		   "Select statistics for each individual Spinlock Instance (SpinlockSlotID).<br>" +
		   "If this is <b>not</b> selected, all Instances of a Spinlock name will be grouped and presented as one row. <i>(number of instances for each spinlock is presented)</i><br>" +
		"</html>";

	public CmSpinlockActivityPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		_sampleSpinlockSlotID_chk = new JCheckBox("View Individual Spinlock Instances", conf == null ? CmSpinlockActivity.DEFAULT_sample_SpinlockSlotID  : conf.getBooleanProperty(CmSpinlockActivity.PROPKEY_sample_SpinlockSlotID,  CmSpinlockActivity.DEFAULT_sample_SpinlockSlotID));

		_sampleSpinlockSlotID_chk.setName(CmSpinlockActivity.PROPKEY_sample_SpinlockSlotID);
		_sampleSpinlockSlotID_chk.setToolTipText(TOOLTIP_sample_SpinlockSlotID);
		panel.add(_sampleSpinlockSlotID_chk, "wrap");

		_sampleSpinlockSlotID_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpinlockActivity.PROPKEY_sample_SpinlockSlotID, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			if (cm.isRuntimeInitialized())
			{
//				boolean visible = cm.getServerVersion() >= 1570100;
				boolean visible = cm.getServerVersion() >= Ver.ver(15,7,0,100);
				_sampleSpinlockSlotID_chk.setVisible(visible);

			} // end isRuntimeInitialized

		} // end (cm != null)
	}
}
