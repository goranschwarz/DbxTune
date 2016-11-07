package com.asetune.tools.sqlw.msg;

import java.awt.Font;

import javax.swing.JTextArea;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.SwingUtils;

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
