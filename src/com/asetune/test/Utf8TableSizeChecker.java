/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Check if database storage of strings using 1 byte character encodings (like iso-8859-1 or other 8 bit characters)<br>
 * Needs to change the column definition if we change the database character set to UTF-8, where some characters would be expanded to use more than 1 byte.<br>
 * <br>
 * Usage: Usage: url user passwd {filename | tab1 tab2 tab3...}<br>
 * The filename should contain table names that we want to check<br>
 * Example of a filename
 * <pre>
 * # File with table names...
 * # lines starting with '#' will be discarded
 * # empty lines will also be discarded
 * t1
 * tempdb..t2
 * 
 * tempdb..t3
 * </pre>
 * 
 * @author Goran_Schwarz@hotmail.com
 *
 */
public class Utf8TableSizeChecker
{
	private Connection _conn = null;
	private LinkedHashMap<String, Integer> _maxLenMap = new LinkedHashMap<String, Integer>();
	
	public Utf8TableSizeChecker()
	{
	}

	public void connect(String url, String user, String passwd)
	throws Exception
	{
		System.out.println("Open DB connection.");
		_conn = DriverManager.getConnection(url, user, passwd);
//		_conn = AseConnectionFactory.getConnection("localhost", 5100, "tempdb", "sa", "sybase", "Utf8TableSizeChecker", null);		
	}

	public void close()
	throws SQLException
	{
		if (_conn != null)
			_conn.close();		
	}

	public void checkTable(String checkTabName)
	{
		// DO THE THING
		try
		{
			String sql = "select * from "+checkTabName;
			System.out.println("\tDO SQL: "+sql);

			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			
			ResultSetMetaData md = rs.getMetaData();
			int colCount = md.getColumnCount();

			int row = 0;
			while (rs.next())
			{
				row++;
				for (int c=1; c<=colCount; c++)
				{
					int jdbcType = md.getColumnType(c);
					if (jdbcType == Types.CHAR || jdbcType == Types.VARCHAR)
					{
						String strVal = rs.getString(c);
						int    colLen  = md.getColumnDisplaySize(c);
						int    utf8Len = getUtf8Len1(strVal);
//						int    utf8Len = getUtf8Len2(strVal);

						if (utf8Len > colLen)
						{
							String catName = md.getCatalogName(c);
							String schName = md.getSchemaName(c);
							String tabName = md.getTableName(c);
							String colName = md.getColumnLabel(c);

							System.out.println("\tWARNING: catName='"+catName+"', schemaName='"+schName+"', tabName='"+tabName+"', colName='"+colName+"', atRow="+row+", colLen="+colLen+", utf8Len="+utf8Len+", strValue='"+strVal+"'.");

							String key = catName+"."+schName+"."+tabName+"("+colName+"): colLen="+colLen;
							Integer maxLen = _maxLenMap.get(key);
							if (maxLen != null && utf8Len > maxLen)
								_maxLenMap.put(key, utf8Len);
							else if (maxLen == null)
								_maxLenMap.put(key, utf8Len);
						}
					}
				}
			}
			rs.close();
		}
		catch (Exception e)
		{
			System.out.println("\tWARNING: Problems in checkTable("+checkTabName+"): Caught: "+e);
//			e.printStackTrace();
		}
	}
	
	public void printReport()
	{
		for (String key : _maxLenMap.keySet())
		{
			int maxUtf8Len = _maxLenMap.get(key);
			System.out.println("\t"+key+", maxUtf8Len="+maxUtf8Len);
		}
	}
	
	public int getUtf8Len1(String str) 
	throws UnsupportedEncodingException
	{
		// Let the JVM do the encoding and get a byte array, which we just use the length of
		final byte[] utf8Bytes = str.getBytes("UTF-8");
		return utf8Bytes.length;
	}

	public int getUtf8Len2(CharSequence sequence)
	{
		int count = 0;
		for (int i = 0, len = sequence.length(); i < len; i++)
		{
			char ch = sequence.charAt(i);
			if ( ch <= 0x7F )
			{
				count++;
			}
			else if ( ch <= 0x7FF )
			{
				count += 2;
			}
			else if ( Character.isHighSurrogate(ch) )
			{
				count += 4;
				++i;
			}
			else
			{
				count += 3;
			}
		}
		return count;
	}


	public static List<String> getTableNames(String filename)
	{
		FileInputStream fis = null;
		BufferedReader reader = null;

		try
		{
			List<String> list = new ArrayList<String>();
			
			fis = new FileInputStream(filename);
			reader = new BufferedReader(new InputStreamReader(fis));

			System.out.println("Reading File: "+filename);

			for(String line = reader.readLine(); line != null; line = reader.readLine())
			{
				if (line.startsWith("#") || line.equals(""))
					continue;
				
				list.add(line);
			}
			reader.close();

			System.out.println("\tFound Content: "+list);
			return list;
		}
		catch (FileNotFoundException ex)
		{
			System.out.println(ex.toString());
			return null;
		}
		catch (IOException ex)
		{
			System.out.println(ex.toString());
			return null;
		}
	}	
	
	public static void main(String[] args)
	{
		String url      = "jdbc:sybase:Tds:localhost:5100";
		String user     = "sa";
		String passwd   = "sybase";
		String filename = null;
		List<String> tableList = null;

//		filename = "xxx";
//		tableList = new ArrayList<String>();
//		tableList.add("tempdb..t1");
//		tableList.add("tempdb..t2");
		
		if (args.length >= 1) url      = args[0];
		if (args.length >= 2) user     = args[1];
		if (args.length >= 3) passwd   = args[2];
		if (args.length >= 4) filename = args[3];
		if (args.length >= 5)
		{
			tableList = new ArrayList<String>();
			for (int i=3; i<args.length; i++) // start at pos 3, which is the filename
				tableList.add(args[i]);
		}

		if (passwd != null && passwd.equalsIgnoreCase("null"))
			passwd = "";
		
		System.out.println("-----------------------------------------");
		System.out.println("Usage: url user passwd filename");
		System.out.println(" NOTE: filename could be a filename or just a list of table names");
		System.out.println("-----------------------------------------");
		System.out.println("URL:      "+url);
		System.out.println("USER:     "+user);
		System.out.println("PASSWD:   "+passwd);
		if (tableList == null)
			System.out.println("filename: "+filename);
		else
			System.out.println("table list: "+tableList);
		System.out.println("-----------------------------------------");

		if (filename == null)
			return;
		
		if (tableList == null)
			tableList = getTableNames(filename);
		
		if (tableList == null || (tableList != null && tableList.size() == 0) )
		{
			System.out.println("ERROR: table list is empty...");
			System.out.println("EXITING");
			return;
		}
			
		
		Utf8TableSizeChecker tc = new Utf8TableSizeChecker();

		try
		{
    		tc.connect(url, user, passwd);
    		
    		for (String tab : tableList)
			{
    			System.out.println("CHECKING TABLE: "+tab);
        		tc.checkTable(tab);
			}

    		tc.close();

    		if (tc.getExceedCount() > 0)
    		{
    			System.out.println("");
    			System.out.println("=============================================================");
    			System.out.println("Report of table columns that needs to be extended in length ");
    			System.out.println("Number of columns exceeding DB Storage length: "+tc.getExceedCount());
    			System.out.println("-------------------------------------------------------------");
        		tc.printReport();
    			System.out.println("-------------------------------------------------------------");
    			System.out.println("");
    		}
    		else
    		{
    			System.out.println("");
    			System.out.println("=============================================================");
    			System.out.println("No table columns has UTF8 values execeeding DB Storage length");
    			System.out.println("-------------------------------------------------------------");
    			System.out.println("");
    		}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public int getExceedCount()
	{
		return _maxLenMap.size();
	}
}
