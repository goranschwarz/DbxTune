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
package com.asetune.config.ui;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.DefaultTableModel;

import com.asetune.config.dbms.DbmsConfigDiffEngine.Context;
import com.asetune.config.dbms.IDbmsConfig;
import com.google.common.collect.MapDifference.ValueDifference;

public class DbmsConfigDiffTableModel extends DefaultTableModel
{
	private static final long serialVersionUID = 1L;
	
	public static final String COL_NAME_diffType    = "Type";
	public static final String COL_NAME_configName  = "ConfigName";
	public static final String COL_NAME_leftIsNDef  = "LND"; // Left is Not Default configured
	public static final String COL_NAME_leftValue   = "Left";
	public static final String COL_NAME_rightIsNDef = "RND"; // Right is Not Default configured
	public static final String COL_NAME_righValue   = "Right";
	public static final String COL_NAME_description = "Description";
	
	public static final int    COL_POS_diffType    = 0;
	public static final int    COL_POS_configName  = 1;
	public static final int    COL_POS_leftIsNDef  = 2;
	public static final int    COL_POS_leftValue   = 3;
	public static final int    COL_POS_rightIsNDef = 4;
	public static final int    COL_POS_rightValue  = 5;
	public static final int    COL_POS_description = 6;
	
	public static final String COL_ToolTip_diffType    = "Could be 'SAME', 'DIFF', 'LEFT' and 'RIGHT'.";
	public static final String COL_ToolTip_configName  = "Name of the configuration parameter";
	public static final String COL_ToolTip_leftIsNDef  = "Left value is NOT a Default Configuration value.";
	public static final String COL_ToolTip_leftValue   = "Left Value";
	public static final String COL_ToolTip_rightIsNDef = "Right value is NOT a Default Configuration value.";
	public static final String COL_ToolTip_righValue   = "Right Value";
	public static final String COL_ToolTip_description = "Description of the configuration parameter";
	
	public static final String DIFF_TYPE_Same      = "=== Same";
	public static final String DIFF_TYPE_Differs   = "<-> Different";
	public static final String DIFF_TYPE_LeftOnly  = "<<< LEFT only";
	public static final String DIFF_TYPE_RightOnly = ">>> RIGHT only";


	private Context _context;
	
	public DbmsConfigDiffTableModel(Context context)
	{
		super();

		_context = context;

		init();
	}

	@Override
	public Class<?> getColumnClass(int mcol)
	{
		if (mcol == COL_POS_leftIsNDef)  return Boolean.class;
		if (mcol == COL_POS_rightIsNDef) return Boolean.class;
		
		return super.getColumnClass(mcol);
	}

	public String getToolTipText(int mcol)
	{
		switch (mcol)
		{
		case COL_POS_diffType   : return COL_ToolTip_diffType   ;
		case COL_POS_configName : return COL_ToolTip_configName ;
		case COL_POS_leftIsNDef : return COL_ToolTip_leftIsNDef ;
		case COL_POS_leftValue  : return COL_ToolTip_leftValue  ;
		case COL_POS_rightIsNDef: return COL_ToolTip_rightIsNDef;
		case COL_POS_rightValue : return COL_ToolTip_righValue  ;
		case COL_POS_description: return COL_ToolTip_description;
		}
		return null;
	}
	
	private void init()
	{
		Map<String, String>                  onlyOnLeft  = _context.getEntriesOnlyInLocalDbms();
		Map<String, String>                  onlyOnRight = _context.getEntriesOnlyInRemoteDbms();
		Map<String, String>                  sameEntries = _context.getEntriesInCommon();
		Map<String, ValueDifference<String>> diffEntries = _context.getEntriesDiffering();

		setColumnIdentifiers(new String[] {
			COL_NAME_diffType, 
			COL_NAME_configName, 
			COL_NAME_leftIsNDef, 
			COL_NAME_leftValue, 
			COL_NAME_rightIsNDef, 
			COL_NAME_righValue, 
			COL_NAME_description
		});

		IDbmsConfig lc  = _context.getLocalDbmsConfig();
		IDbmsConfig rc= _context.getRemoteDbmsConfig();

		// ONLY LEFT
		for (Entry<String, String> e : onlyOnLeft.entrySet())
		{
			addRow(new Object[] {
				DIFF_TYPE_LeftOnly,                                // TYPE
				e.getKey(),                                        // ConfigName
				lc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Left is NOT default
				e.getValue(),                                      // Left Value
				false,                                             // Right is default
				null,                                              // Right Value
				lc.getDescription(e.getKey())                      // Description
			});
		}
			
		// ONLY RIGHT
		for (Entry<String, String> e : onlyOnRight.entrySet())
		{
			addRow(new Object[] {
				DIFF_TYPE_RightOnly,                               // TYPE
				e.getKey(),                                        // ConfigName
				false,                                             // Left is NOT default
				null,                                              // Left Value
				rc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Right is default
				e.getValue(),                                      // Right Value
				rc.getDescription(e.getKey())                      // Description
			});
		}

		// DIFF
		for (Entry<String, ValueDifference<String>> e : diffEntries.entrySet())
		{
			addRow(new Object[] {
				DIFF_TYPE_Differs,                                 // TYPE
				e.getKey(),                                        // ConfigName
				lc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Left is NOT default
				e.getValue().leftValue(),                          // Left Value
				rc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Right is NOT default
				e.getValue().rightValue(),                         // Right Value
				lc.getDescription(e.getKey())                      // Description
			});
		}

		// SAME
		for (Entry<String, String> e : sameEntries.entrySet())
		{
			addRow(new Object[] {
				DIFF_TYPE_Same,                                    // TYPE
				e.getKey(),                                        // ConfigName
				lc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Left is NOT default
				e.getValue(),                                      // Left Value
				rc.getDbmsConfigEntry(e.getKey()).isNonDefault(),  // Right is NOT default
				e.getValue(),                                      // Right Value
				lc.getDescription(e.getKey())                      // Description
			});
		}

	}
}
