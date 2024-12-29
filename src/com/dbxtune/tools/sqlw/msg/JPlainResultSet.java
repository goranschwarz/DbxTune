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
package com.dbxtune.tools.sqlw.msg;

import java.awt.Font;

import javax.swing.JTextArea;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.utils.SwingUtils;

public class JPlainResultSet
extends JTextArea
{
	private static final long serialVersionUID = 1L;

	private ResultSetTableModel _tm = null;
	private String _text = null;
	

	protected static Font _aseMsgFont = null;

	public JPlainResultSet()
	{
	}
	public JPlainResultSet(final ResultSetTableModel rstm)
	{
//		super(rstm.toTableString());
		_tm = rstm;
		init();
	}
	
	@Override
	public String getText()
	{
		if (_text == null)
			_text = _tm.toTableString();
		
		return _text;
	}


	public ResultSetTableModel getResultSetTableModel()
	{
		return _tm;
	}

	public int getRowCount()
	{
		return _tm.getRowCount();
	}

	protected void init()
	{
		super.setEditable(false);

		if (_aseMsgFont == null)
			_aseMsgFont = new Font("Courier", Font.PLAIN, SwingUtils.hiDpiScale(12));
		setFont(_aseMsgFont);

		setLineWrap(true);
		setWrapStyleWord(true);
//		setOpaque(false); // Transparent
	}

//	public boolean isFocusable()
//	{
//		return false;
//	}
//
//	public boolean isRequestFocusEnabled()
//	{
//		return false;
//	}
}
