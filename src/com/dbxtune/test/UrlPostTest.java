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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;

public class UrlPostTest
{

		public static void main(String[] args)
		{
			try 
			{
				// Construct data
				String q = "&clientSourceVersion=999&user_name=goransXXX&debug=true";

				// Send data
				URL url = new URL("http://www.dbxtune.com/xxx.php?"+q.toString());
				URLConnection conn = url.openConnection();
//				conn.setDoInput(true);
//				conn.setDoOutput(true);

//				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//				wr.write(q.toString());
//				wr.flush();
//				wr.close();
				
				System.out.println("getHeaderFields: " +conn.getHeaderFields());

				InputStream in = conn.getInputStream();
				LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
				String line;
				while ((line = lr.readLine()) != null)
				{
					System.out.println("response line "+lr.getLineNumber()+": " + line);
				}
				
			} catch (Exception ex) {
				System.out.println("error: "+ex);
			}
		}
}
