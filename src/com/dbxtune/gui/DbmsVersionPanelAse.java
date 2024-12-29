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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class DbmsVersionPanelAse 
extends DbmsVersionPanelTds
{
	private static final long serialVersionUID = 1L;

	protected JCheckBox _versionIsCe_chk = new JCheckBox("Cluster Edition", false);

	public DbmsVersionPanelAse(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
	}
	
	@Override
	protected JPanel createDbmsPropertiesPanel()
	{
		JPanel p = new JPanel(new MigLayout());
		
		_versionIsCe_chk  .setToolTipText("<html>Generate SQL Information for a Cluster Edition Server</html>");

		p.add(_versionIsCe_chk, "");
		
		_versionIsCe_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				DbmsVersionPanelAse.super.stateChanged(null);
			}
		});

		return p;
	}

	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoSybaseAse(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		// Create a SQL Server version object
		DbmsVersionInfoSybaseAse versionInfo = new DbmsVersionInfoSybaseAse(ver);

		// Set ASE specifics (from any extended GUI fields)
		versionInfo.setClusterEdition(_versionIsCe_chk.isSelected());
		
		return versionInfo;
	}

	@Override
	public void loadFieldsUsingVersion(DbmsVersionInfo versionInfo)
	{
		super.loadFieldsUsingVersion(versionInfo);

		// Set local fields
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;

		_versionIsCe_chk.setSelected(aseVersionInfo.isClusterEdition());
	}
	
	@Override
	public long getMinVersion()
	{
		return Ver.ver(12,5,0,3);
	}
}
