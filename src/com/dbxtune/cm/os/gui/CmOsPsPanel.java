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
package com.dbxtune.cm.os.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.utils.Configuration;

public class CmOsPsPanel
extends CmOsGenericPanel
{
	private static final long    serialVersionUID      = 1L;

	public CmOsPsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private JLabel     _sample_topRows_lbl;
	private JTextField _sample_topRows_txt;

	@Override
	protected JPanel configLocalOptionsPanel(JPanel panel)
	{
		if (panel instanceof LocalOptionsConfigPanel)
		{
			((LocalOptionsConfigPanel)panel).setLocalOptionsConfigChanges(new LocalOptionsConfigChanges()
			{
				@Override
				public void configWasChanged(String propName, String propVal)
				{
					Configuration conf = Configuration.getCombinedConfiguration();
					
//					list.add(new CmSettingsHelper("Top Rows", PROPKEY_top, Integer.class, conf.getIntProperty(PROPKEY_top, DEFAULT_top), DEFAULT_top, "Number of top rows."));

					_sample_topRows_txt.setText(""+ conf.getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top));
				}
			});
		}

		Configuration conf = Configuration.getCombinedConfiguration();

		_sample_topRows_lbl = new JLabel("Top Rows");
		_sample_topRows_txt = new JTextField(""+ conf.getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top), 5);

		_sample_topRows_lbl.setToolTipText("<html>Only first # rows from ps</html>");
		_sample_topRows_txt.setToolTipText("<html>Only first # rows from ps</html>");
		
		_sample_topRows_lbl.setVisible(true);
		_sample_topRows_txt.setVisible(true);


		panel.add( _sample_topRows_lbl, "split, hidemode 3");  // or: span
		panel.add( _sample_topRows_txt, "hidemode 3, wrap");

		_sample_topRows_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsPs.PROPKEY_top, _sample_topRows_txt.getText());
					tempConf.save();
				}
			}
		});
		
		return panel;
	}
}
