package com.asetune.test;

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
