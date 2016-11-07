package com.asetune.cm;

import java.awt.event.MouseEvent;

import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.swing.GTable;

public class CmToolTipSupplierDefault
implements GTable.ITableTooltip
{
	protected CountersModel _cm = null;

	public CmToolTipSupplierDefault(CountersModel cm)
	{
		_cm = cm;
	}
//	public CountersModel getCm()
//	{
//		return _cm;
//	}
//	public boolean hasCm()
//	{
//		return _cm != null;
//	}

	/**
	 * Get tooltip for a specific Table Column
	 * @param colName
	 * @return the tooltip
	 */
	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		if (_cm == null)
			return null;
		
		return MonTablesDictionaryManager.getInstance().getDescription(_cm.getMonTablesInQuery(), colName);
	}

	/**
	 * Used by the TabularCntrPanel.JTable to get tool tip on a cell level.
	 * Implement it to set specific tooltip...
	 *  
	 * @param e
	 * @param colName
	 * @param modelRow
	 * @param modelCol
	 * @return
	 */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		return null;
	}
}
