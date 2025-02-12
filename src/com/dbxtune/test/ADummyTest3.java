/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.test;

import java.text.NumberFormat;

public class ADummyTest3
{

	public static void main(String[] args)
	{
	    System.out.println("User's number (DEFAULT LOCALE): " + NumberFormat.getNumberInstance().format(1.123));
	    System.out.println("User's number (DEFAULT LOCALE): " + NumberFormat.getNumberInstance().format(100.123));
	    System.out.println("User's number (DEFAULT LOCALE): " + NumberFormat.getNumberInstance().format(1000.123));
	    System.out.println("User's number (DEFAULT LOCALE): " + NumberFormat.getNumberInstance().format(1000000.123));
	    System.out.println("User's number (DEFAULT LOCALE): " + NumberFormat.getNumberInstance().format(123456789.123));

//		String str="b6Awj8ncl5wouLWbVKwYM+Ev+Y/+nwExI0tld4EZQJVluWLy/qR8l4T473vzr5eMNS";
//		
//		int ENC_KEY_LEN_C = 32;
//		byte[] enckeys = new byte[] {(byte)103, (byte)207, (byte)212, (byte)118, (byte)157, (byte)24,  (byte)103, (byte)175, (byte)198, (byte)191, (byte)100, (byte)99,  (byte)106,  (byte)70, (byte)208, (byte)187, (byte)4,  (byte)180, (byte)100, (byte)88,  (byte)194, (byte)213, (byte)198, (byte)188, (byte)204, (byte)180, (byte)209, (byte)78, (byte)211, (byte)240, (byte)127, (byte)128};
//
//		byte[] b = str.getBytes(Charset.forName("ISO-8859-1"));
//		byte[] c = new byte[b.length];
//		System.out.println("b.length="+b.length);
//
//		for (int i=0; i<b.length; i++)
//		{
//			int p=i;
//			if (i>1)
//				p
//			c[i] = (byte) ( ((int)b[i]) ^ ((int)enckeys[(p) % ENC_KEY_LEN_C]) );
////			System.out.println("b["+i+"]="+b[i] + "      c["+i+"]="+c[i]);
//			System.out.format("b[%d]=%3d, c[%d]=%3d\n", i, b[i], i, c[i]);
//		}
//		
//		
////		for (int i=0; i<b.length; i++)
////		{
////			c[i] ^= b[i];
////			System.out.println("c["+i+"]="+b[c]);
////		}
////        *tmp_area_ptr ^= enckeys[i % ENC_KEY_LEN_C];
//
	}

}
