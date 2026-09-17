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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class Latin1TranslitTest
{
	private static final String SQL_INSERT = "insert into #t1 (id, method, c1) values (?, ?, ?)";
	
	private static final String[] VALUES = {
		"Stevanović",        // ć -> c
		"Łódź",              // Ł -> L, ó kept, ź -> z
		"Œuvre complète",    // Œ -> OE, è kept
		"Smörgåsbord",       // all ISO-8859-1, unchanged
		"“quoted” – €5",     // “ ” -> ",  – -> -,  € -> EUR
	};
 
	public static void main(String[] args) 
	throws Exception 
	{
		if (args.length < 5) 
		{
			System.err.println("Usage: TranslitTest host port db user password [noconv]");
			System.exit(1);
		}

		String  url          = "jdbc:sybase:Tds:" + args[0] + ":" + args[1] + "/" + args[2];
		boolean useConverter = !(args.length > 5 && "noconv".equalsIgnoreCase(args[5]));
 
		Properties props = new Properties();
		props.put("user",                    args[3]);
		props.put("password",                args[4]);
		props.put("CHARSET",                 "iso_1");
		props.put("DISABLE_UNICHAR_SENDING", "true");
		if (useConverter)
			props.put("CHARSET_CONVERTER_CLASS", Latin1TranslitCharsetConverter.class.getName());
 
//		Class.forName("com.sybase.jdbc4.jdbc.SybDriver");
 
		try (Connection conn = DriverManager.getConnection(url, props)) 
		{
			System.out.println("Connected. CHARSET_CONVERTER_CLASS " + (useConverter ? "ENABLED" : "DISABLED"));
 
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate("create table #t1 (id int, method varchar(30), c1 varchar(30))");
			}
 
			// 1. Plain language request (values inlined in the SQL text)
			try (Statement stmt = conn.createStatement()) 
			{
				for (int i = 0; i < VALUES.length; i++) 
				{
					String sql = "insert into #t1 (id, method, c1) values (" + (100 + i) + ", 'language', '" + VALUES[i].replace("'", "''") + "')";
					stmt.executeUpdate(sql);
				}
			}
 
			// 2. Prepared statement, one execute per row
			try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) 
			{
				for (int i = 0; i < VALUES.length; i++) 
				{
					ps.setInt   (1, 200 + i);
					ps.setString(2, "prepared");
					ps.setString(3, VALUES[i]);
					ps.executeUpdate();
				}
			}
 
			// 3. JDBC batch
			try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) 
			{
				for (int i = 0; i < VALUES.length; i++) 
				{
					ps.setInt   (1, 300 + i);
					ps.setString(2, "batch");
					ps.setString(3, VALUES[i]);
					ps.addBatch();
				}
				int[] counts = ps.executeBatch();
				System.out.println("Batch executed, " + counts.length + " statements.");
			}
 
			// Report
			String sql = "select id, method, c1, convert(varbinary(30), c1) from #t1 order by id";
			try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) 
			{
				System.out.printf("\n");
				System.out.printf("%-9s %-20s %4s  %-20s %-44s %s\n", "Method", "javaStr", "id", "c1", "hex", "status");
				System.out.printf("%-9s %-20s %4s  %-20s %-44s %s\n", "------", "---------", "--", "--", "---", "------");
				while (rs.next()) 
				{
					int    id       = rs.getInt(1);
					String method   = rs.getString(2);
					String c1       = rs.getString(3);
					byte[] raw      = rs.getBytes(4);
					String expected = Latin1Translit.translit(VALUES[id % 100]);
					String javaStr  = VALUES[id % 100];
					String status   = expected.equals(c1) ? "OK" : "DIFF (expected: " + expected + ")";
 
					System.out.printf("%-9s %-20s %4d  %-20s 0x%-42s %s\n", method, javaStr, id, c1, hex(raw), status);
				}
			}

			// Lookup: select ... where c1 = <value>
			// Each value was inserted 3 times (language, prepared, batch), so every lookup should find 3 rows
			System.out.printf("\n");
			System.out.printf("%-9s %-20s %5s  %-15s %s\n", "Lookup", "javaStr", "rows", "ids", "status");
			System.out.printf("%-9s %-20s %5s  %-15s %s\n", "------", "-------", "----", "---", "------");

			// 4. Lookup using language request (value inlined in the SQL text)
			try (Statement stmt = conn.createStatement())
			{
				for (int i = 0; i < VALUES.length; i++)
				{
					String selectSql = "select id from #t1 where c1 = '" + VALUES[i].replace("'", "''") + "' order by id";
					try (ResultSet rs = stmt.executeQuery(selectSql))
					{
						printLookup("language", VALUES[i], rs);
					}
				}
			}

			// 5. Lookup using prepared statement (value as parameter)
			try (PreparedStatement ps = conn.prepareStatement("select id from #t1 where c1 = ? order by id"))
			{
				for (int i = 0; i < VALUES.length; i++)
				{
					ps.setString(1, VALUES[i]);
					try (ResultSet rs = ps.executeQuery())
					{
						printLookup("prepared", VALUES[i], rs);
					}
				}
			}
		}
	}

	private static void printLookup(String method, String javaStr, ResultSet rs)
	throws Exception
	{
		StringBuilder ids = new StringBuilder();
		int rows = 0;
		while (rs.next())
		{
			if (rows > 0)
				ids.append(",");
			ids.append(rs.getInt(1));
			rows++;
		}
		String status = rows == 3 ? "OK" : "DIFF (expected 3 rows)";

		System.out.printf("%-9s %-20s %5d  %-15s %s\n", method, javaStr, rows, ids, status);
	}
  
	private static String hex(byte[] b) 
	{
		if (b == null) 
			return "NULL";

		StringBuilder sb = new StringBuilder(b.length * 2);
		for (byte x : b)
			sb.append(String.format("%02x", x));

		return sb.toString();
	}
}
