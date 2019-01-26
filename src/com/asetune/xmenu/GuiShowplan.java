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
package com.asetune.xmenu;

import com.asetune.gui.AsePlanViewer;
import com.asetune.utils.SwingUtils;

public class GuiShowplan
extends XmenuActionBase 
{
	public GuiShowplan() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		String xmlPlan  = getParamValue(0);

		if (xmlPlan == null)
			throw new RuntimeException("XML Plan string was 'null', this isn't valid.");
		
//		System.out.println("===========================================");
//		System.out.println(xmlPlan);
//		System.out.println("===========================================");

		if (xmlPlan.startsWith("<?xml version"))
		{
			AsePlanViewer pv = new AsePlanViewer(xmlPlan);
			pv.setVisible(true);
//			if (xmlPlan.indexOf("<planVersion>") >= 0)
//			{
//				AsePlanViewer pv = new AsePlanViewer(xmlPlan);
//				pv.setVisible(true);
//			}
//			else
//			{
//				String htmlMsg = 
//					"<html>" +
//					"<b>Can't find the tag '<planVersion>' in the XML.</b> <br>" +
//					"So this is NOT a 'supported' XML plan." +
//					"</html>";
//				SwingUtils.showErrorMessage("No XML input found", htmlMsg, null);
//			}
		}
		else
		{
			int len = Math.min(50, xmlPlan.length());
			String startOf = xmlPlan.substring(0, len);
			String htmlMsg = 
				"<html>" +
				"<b>This doesn't seem to be a XML string.<b>" +
				"<pre>" +
				startOf +
				"</pre>" +
				"</html>";
			SwingUtils.showErrorMessage("No XML input found", htmlMsg, null);
		}
	}
}
