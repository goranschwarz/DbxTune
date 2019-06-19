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
package com.asetune.cm;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import javax.swing.event.HyperlinkEvent;

import org.apache.log4j.Logger;

import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.focusabletip.ResolverReturn;
import com.asetune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.xmenu.SqlSentryPlanExplorer;

public class CmToolTipSupplierDefault
implements GTable.ITableTooltip, ToolTipHyperlinkResolver
{
	private static Logger _logger = Logger.getLogger(CmToolTipSupplierDefault.class);
	
	public static final String  PROPKEY_TABLE_TOOLTIP_FOCUSABLE = "<CMNAME>.table.tooltip.focusable";
	public static final boolean DEFAULT_TABLE_TOOLTIP_FOCUSABLE = false;

	public static final String  PROPKEY_TABLE_TOOLTIP_SHOW_PK = "<CMNAME>.table.tooltip.show.pk";
	public static final boolean DEFAULT_TABLE_TOOLTIP_SHOW_PK = true;

	public static final String  PROPKEY_TABLE_TOOLTIP_SHOW_ALL = "<CMNAME>.table.tooltip.show.all";
	public static final boolean DEFAULT_TABLE_TOOLTIP_SHOW_ALL = false;

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
		
		// Show PK/ALL columns in a table
		boolean useFocusableTt = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_FOCUSABLE.replace("<CMNAME>", _cm.getName()), DEFAULT_TABLE_TOOLTIP_FOCUSABLE); 
		boolean showPkCols     = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_SHOW_PK  .replace("<CMNAME>", _cm.getName()), DEFAULT_TABLE_TOOLTIP_SHOW_PK); 
		boolean showAllCols    = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_SHOW_ALL .replace("<CMNAME>", _cm.getName()), DEFAULT_TABLE_TOOLTIP_SHOW_ALL); 
		if (showPkCols || showAllCols)
		{
			String ttExtraInfo = "";
			if (true)
			{
				ttExtraInfo += "<br>";
				String propName;
				
				propName = PROPKEY_TABLE_TOOLTIP_FOCUSABLE.replace("<CMNAME>", _cm.getName());
				ttExtraInfo += "<a href='" + CmToolTipSupplierDefault.SET_PROPERTY_TEMP + propName + "=" + (!useFocusableTt) + "'>" + (useFocusableTt ? "Disable" : "Enable") + "</a> - Focusable Tooltip.<br>";

				propName = PROPKEY_TABLE_TOOLTIP_SHOW_PK.replace("<CMNAME>", _cm.getName());
				ttExtraInfo += "<a href='" + CmToolTipSupplierDefault.SET_PROPERTY_TEMP + propName + "=" + (!showPkCols) + "'>" + (showPkCols ? "Disable" : "Enable") + "</a> - Show PK Columns.<br>";

				propName = PROPKEY_TABLE_TOOLTIP_SHOW_ALL.replace("<CMNAME>", _cm.getName());
				ttExtraInfo += "<a href='" + CmToolTipSupplierDefault.SET_PROPERTY_TEMP + propName + "=" + (!showAllCols) + "'>" + (showAllCols ? "Disable" : "Enable") + "</a> - Show ALL Columns.<br>";
				
				ttExtraInfo += "<b>Note</b>: Above items can be set/controlled from:<br> " 
				            + "&emsp; &emsp; Option Panel, lower right corner, <i>checkbox</i> icon/button<br>"
				            + "&emsp; &emsp; Or from the table right click menu.<br>";
				//ttExtraInfo += "<hr>";
			}

			StringBuilder sb = new StringBuilder();
			sb.append( useFocusableTt ? GTable.TOOLTIP_TYPE_FOCUSABLE : GTable.TOOLTIP_TYPE_NORMAL );
			sb.append("<html>");
			sb.append("<table border=0 cellpadding=1 cellspacing=0>");

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
			sb.append("<tr><td colspan='2'><hr></td></tr>"); // --------------- Horizontal ruler
			
			if (showAllCols)
			{
				int whatData = _cm.getDataSource();
				List<String> cols = _cm.getColNames(whatData);
				for (String col : cols)
				{
					Object val = _cm.getValue(whatData, modelRow, col, true);
					sb.append("<tr><td><b>").append(col).append("</b>&nbsp;</td> <td>").append(val).append("</td></tr>");
				}
			}
			else
			{
				sb.append("<tr><td><b>").append(colName).append("</b>&nbsp;</td> <td>").append(cellValue).append("</td></tr>");
			}
			sb.append("<tr><td colspan='2'><hr></td></tr>"); // --------------- Horizontal ruler
			
			sb.append("</table>");
			sb.append(ttExtraInfo);
			sb.append("</html>");

			return sb.toString();
		}
		
		return null;
	}


	/** used to specify that a HTML LINK should be opened in EXTERNAL Browser */
	public static final String OPEN_IN_EXTERNAL_BROWSER         = "OPEN-IN-EXTERNAL-BROWSER:";
	public static final String OPEN_IN_SENTRY_ONE_PLAN_EXPLORER = "OPEN-IN-SENTRY-ONE-PLAN-EXPLORER:";
	public static final String SET_PROPERTY_TEMP                = "SET-PROPERTY-TEMP:";

	protected static File createTempFile(String prefix, String suffix, byte[] bytes)
	throws IOException
	{
		// add "." if the suffix doesn't have that
		if (StringUtil.hasValue(suffix) && !suffix.startsWith("."))
			suffix = "." + suffix;

		File tmpFile = File.createTempFile(prefix, suffix);
		tmpFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(tmpFile);
		fos.write(bytes);
		fos.close();
		
		return tmpFile;
	}

	@Override
	public ResolverReturn hyperlinkResolv(HyperlinkEvent event)
	{
		String desc = event.getDescription();
		if (_logger.isDebugEnabled())
		{
			_logger.debug("");
			_logger.debug("##################################################################################");
			_logger.debug("hyperlinkResolv(): event.getDescription()  ="+event.getDescription());
			_logger.debug("hyperlinkResolv(): event.getURL()          ="+event.getURL());
			_logger.debug("hyperlinkResolv(): event.getEventType()    ="+event.getEventType());
			_logger.debug("hyperlinkResolv(): event.getSourceElement()="+event.getSourceElement());
			_logger.debug("hyperlinkResolv(): event.getSource()       ="+event.getSource());
			_logger.debug("hyperlinkResolv(): event.toString()        ="+event.toString());
		}

		if (desc.startsWith(OPEN_IN_EXTERNAL_BROWSER))
		{
			String urlStr = desc.substring(OPEN_IN_EXTERNAL_BROWSER.length());
			try
			{
				return ResolverReturn.createOpenInExternalBrowser(event, urlStr);
			}
			catch (MalformedURLException e)
			{
				_logger.warn("Problems open URL='"+urlStr+"', in external Browser.", e);
			}
		}

		if (desc.startsWith(OPEN_IN_SENTRY_ONE_PLAN_EXPLORER))
		{
			String urlStr = desc.substring(OPEN_IN_SENTRY_ONE_PLAN_EXPLORER.length());
			if (urlStr.startsWith("file:///"))
				urlStr = urlStr.substring("file:///".length());
			
			File tempFile = new File(urlStr);
			SqlSentryPlanExplorer.openSqlPlanExplorer(tempFile);
			
			return ResolverReturn.createDoNothing(event);
			//return null;
		}

		if (desc.startsWith(SET_PROPERTY_TEMP))
		{
			String str = desc.substring(SET_PROPERTY_TEMP.length());
			return ResolverReturn.createSetProperyTemp(event, str);
		}

		return ResolverReturn.createOpenInCurrentTooltipWindow(event);
	}
}
