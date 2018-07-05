package com.asetune.gui;

import java.util.List;

import javax.swing.JDialog;
import javax.swing.table.AbstractTableModel;

import com.asetune.pcs.DdlDetails;
import com.asetune.sql.SqlPickList;

public class DdlViewerSearchForDialog
{
	private SqlPickList      _pickList = null;

	/**
	 * Open dialog to choose a record.<br>
	 * if only one row in the table (after filter has been appied): Then return that entry<br>
	 * else: Open the dialog and choose a entry.<br>
	 * <br>
	 * If escape is pressed or Cancel was pressed, then return null.
	 * 
	 * @param owner     GUI Owner
	 * @param ddlList   List if items
	 * @param filter    Filter on what to search for
	 * 
	 * @return The entry in the ddlList
	 */
	public static DdlDetails open(JDialog owner, List<DdlDetails> ddlList, String filter)
	{
		DdlViewerSearchForDialog sfd = new DdlViewerSearchForDialog(owner, ddlList, filter);

		// if only one row in the table (after filter) Then get that row
		if (sfd.getRowCount() == 1)
		{
			int mrow = sfd.convertViewRowIndexToModel(0);
			if (mrow == -1)
				return null;
			
			return ddlList.get(mrow);
		}
		else // NO Hit or More than 0 row: Show dialog
		{
			sfd.setVisible(true);
			
			if ( ! sfd._pickList.wasOkPressed() )
				return null;
				
			int mrow = sfd.getSelectedModelRow();
			if (mrow == -1)
				return null;
			
			return ddlList.get(mrow);
		}
	}

	public DdlViewerSearchForDialog(JDialog owner, List<DdlDetails> ddlList, String filter)
	{
		AbstractTableModel atm = createTableMode(ddlList); 
		
		_pickList = new SqlPickList(owner, atm, null, true);
		_pickList.setFilter(filter);
	}

	public void setVisible(boolean val)
	{
		_pickList.setVisible(val);
	}

	public int convertViewRowIndexToModel(int vrow)
	{
		return _pickList.convertViewRowIndexToModel(vrow);
	}

	public int getSelectedModelRow()
	{
		return _pickList.getSelectedModelRow();
	}

	private int getRowCount()
	{
		return _pickList.getRowCount();
	}

	public AbstractTableModel createTableMode(List<DdlDetails> ddlList)
	{
		return new DdlViewerSearchForModel(ddlList);
	}
	
	private static class DdlViewerSearchForModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private List<DdlDetails> _ddlList = null;

		public DdlViewerSearchForModel(List<DdlDetails> ddlList)
		{
			_ddlList = ddlList;
		}

		@Override
		public int getRowCount()
		{
			return _ddlList.size();
		}

		@Override
		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public String getColumnName(int col)
		{
			switch (col)
			{
			case 0: return "DBName";
			case 1: return "ObjectName";
			}
			return super.getColumnName(col);
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			DdlDetails entry = _ddlList.get(row);
			
			switch (col)
			{
			case 0: return entry.getDbname();
			case 1: return entry.getObjectName();
			}
			
			return null;
		}
		
	}
}
