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
