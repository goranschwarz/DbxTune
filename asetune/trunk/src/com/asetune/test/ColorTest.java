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
package com.asetune.test;

import java.awt.Color;

import com.asetune.utils.SwingUtils;


public class ColorTest
{

	
	public static void main(String[] args)
	{
//		System.out.println("Toolkit.getDefaultToolkit(): '" + Toolkit.getDefaultToolkit() + ", classname='"+Toolkit.getDefaultToolkit().getClass().getName()+"'.");
//
//		String propnames[] = (String[])Toolkit.getDefaultToolkit().getDesktopProperty("win.propNames");
//		System.out.println("Supported windows property names:");
//		for(int i = 0; i < propnames.length; i++) 
//		{
//			Object propValue = Toolkit.getDefaultToolkit().getDesktopProperty(propnames[i]);
//			System.out.println(propnames[i] + " = '"+propValue+"', className='"+propValue.getClass().getName()+"'.");
//			
//		}
		
		String cp1a = "51.255.204";
		String cp1b = "51.255.204.111";
		String cp2a = "51,255,204";
		String cp2b = "51,255,204,111";
		String cp3a = "#33FFCC";
		String cp3b = "#33FFCCaa";
		String cp4a = "0x33FFCC";
		String cp4b = "0x33FFCCaa";
		String cp5 = "3394815";
		String cp6 = "-13369396";

		System.out.println("parseColor(cp1a) = '"+SwingUtils.parseColor(cp1a, Color.BLACK) + "'.");
		System.out.println("parseColor(cp1b) = '"+SwingUtils.parseColor(cp1b, Color.BLACK) + "'.");
		System.out.println("parseColor(cp2a) = '"+SwingUtils.parseColor(cp2a, Color.BLACK) + "'.");
		System.out.println("parseColor(cp2b) = '"+SwingUtils.parseColor(cp2b, Color.BLACK) + "'.");
		System.out.println("parseColor(cp3a) = '"+SwingUtils.parseColor(cp3a, Color.BLACK) + "'.");
		System.out.println("parseColor(cp3b) = '"+SwingUtils.parseColor(cp3b, Color.BLACK) + "'.");
		System.out.println("parseColor(cp4a) = '"+SwingUtils.parseColor(cp4a, Color.BLACK) + "'.");
		System.out.println("parseColor(cp4b) = '"+SwingUtils.parseColor(cp4b, Color.BLACK) + "'.");
		System.out.println("parseColor(cp5) = '"+SwingUtils.parseColor(cp5, Color.BLACK) + "'.");
		System.out.println("parseColor(cp6) = '"+SwingUtils.parseColor(cp6, Color.BLACK) + "'.");

		System.out.println("parseColor(RED) = '"+SwingUtils.parseColor("RED", Color.BLACK) + "'.");

		System.out.println(cp1a+"=rgb(int):"+SwingUtils.parseColor(cp1a, Color.BLACK).getRGB());
	}	
}
