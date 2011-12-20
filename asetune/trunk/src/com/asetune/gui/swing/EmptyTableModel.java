package com.asetune.gui.swing;

import javax.swing.table.AbstractTableModel;

/**
 * Simply an empty table model which wont throw IndexOutOfBounds exceptions.
 * <p>
 * The idea is to use this one instead of DefaultTableModel
 * 
 * @author gorans
 *
 */
@SuppressWarnings("serial")
public class EmptyTableModel 
extends AbstractTableModel
{
	@Override
	public int getRowCount()
	{
		return 0;
	}

	@Override
	public int getColumnCount()
	{
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return null;
	}
}
