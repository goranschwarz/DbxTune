package asemon.test;

import java.awt.Color;

import asemon.utils.SwingUtils;

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
