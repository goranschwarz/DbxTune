package com.asetune.cm;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;

public class CmToolTipSupplierDefault
implements GTable.ITableTooltip
{
	public static final String  PROPKEY_TABLE_TOOLTIP_SHOW_PK = "<CMNAME>.table.tooltip.show.pk";
	public static final boolean DEFAULT_TABLE_TOOLTIP_SHOW_PK = true;

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
		if (_cm == null)
			return null;
		
		// Show PK columns in a table
		boolean showPkCols = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_SHOW_PK.replace("<CMNAME>", _cm.getName()), DEFAULT_TABLE_TOOLTIP_SHOW_PK); 
		if (showPkCols)
		{
			StringBuilder sb = new StringBuilder(GTable.TOOLTIP_TYPE_NORMAL);
			sb.append("<html><table border=0 cellpadding=1 cellspacing=0>");

			Map<String, String> pkValMap = _cm.getPkRewriteMap(modelRow);
			if (pkValMap != null)
			{
				for (String key : pkValMap.keySet())
				{
					String val = pkValMap.get(key);
					sb.append("<tr><td><b>").append(key).append("</b>&nbsp;</td> <td>").append(val).append("</td></tr>");
				}
			}
			else
			{
				List<String> pkList = _cm.getPk();
				if (pkList == null || (pkList != null && pkList.isEmpty()))
					return null;

				String pkVal = _cm.getAbsPkValue(modelRow);
				String[] pkValArr = pkVal.split("\\|");

				for (int i=0; i<pkList.size(); i++)
				{
					String val = i < pkValArr.length ? pkValArr[i] : "i="+i+", arr.length="+pkValArr.length;
					sb.append("<tr><td><b>").append(pkList.get(i)).append("</b>&nbsp;</td> <td>").append(val).append("</td></tr>");
				}
			}
//			sb.append("<tr><td><hr></td><td><hr></td></tr>"); // --------------- Horizontal ruler
			sb.append("<tr><td colspan='2'><hr></td></tr>"); // --------------- Horizontal ruler
			sb.append("<tr><td><b>").append(colName).append("</b>&nbsp;</td> <td>").append(cellValue).append("</td></tr>");
			
			sb.append("</table></html>");

			return sb.toString();
		}
		
		return null;
	}
}
