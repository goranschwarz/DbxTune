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
package com.dbxtune.gui;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResultSetTableModelTest
{

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);
	}
	
	@Test
	public void testCopyCellContentFrom() 
	throws SQLException
	{
		// Create ResultSet - 1
		SimpleResultSet srs1 = new SimpleResultSet();
		srs1.addColumn("key",            Types.VARCHAR,       30, 0);
		srs1.addColumn("ObjectName",     Types.VARCHAR,      512, 0);
		srs1.addColumn("linenum",        Types.INTEGER,        0, 0);
		srs1.addColumn("SqlText",        Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

		srs1.addRow("k1", "obj1", 1, "sqltext-1");
		srs1.addRow("k2", "obj2", 2, "sqltext-2");
		srs1.addRow("k3", "obj3", 3, "sqltext-3");
		srs1.addRow("k4", "obj4", 4, "sqltext-4");
		srs1.addRow("k5", "obj5", 5, "sqltext-5");
		srs1.addRow("k6", "obj6", 6, "sqltext-6");
		srs1.addRow("k7", "obj7", 7, "sqltext-7");
		srs1.addRow("k8", "obj8", 8, "sqltext-8");
		srs1.addRow("k9", "obj9", 9, "sqltext-9");

		
		// Create ResultSet - 2
		SimpleResultSet srs2 = new SimpleResultSet();
		srs2.addColumn("key",            Types.VARCHAR,       30, 0);
		srs2.addColumn("ProcName",       Types.VARCHAR,      512, 0);
		srs2.addColumn("linenum",        Types.INTEGER,        0, 0);
		srs2.addColumn("PlanText",       Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

		srs2.addRow("k1", "obj1", 1, "");
		srs2.addRow("k2", "obj2", 2, "");
		srs2.addRow("k3", "obj3", 3, "");
		srs2.addRow("k4", "obj4", 4, "");
		srs2.addRow("k5", "obj5", 5, "");
		srs2.addRow("k6", "obj6", 6, "");
		srs2.addRow("k7", "obj7", 7, "");
		srs2.addRow("k8", "obj8", 8, "");
		srs2.addRow("k9", "obj9", 9, "");

		
		// Turn them into ResultSetTableModel
		ResultSetTableModel rstm1 = new ResultSetTableModel(srs1, "SRS-1");
		ResultSetTableModel rstm2 = new ResultSetTableModel(srs2, "SRS-2");

		// Copy rstm1.'SqlText' -> rstm2.'PlanText'  WHERE rstm1.ObjectName = rstm2.ProcName
		rstm2.copyCellContentFrom(rstm1, "ObjectName", "SqlText",   "ProcName", "PlanText");
		
//		System.out.println("table-1 (" + rstm1.getName() + ")\n" + rstm1.toAsciiTableString());
//		System.out.println("table-2 (" + rstm2.getName() + ")\n" + rstm2.toAsciiTableString());
		
		assertEquals("expected tab-1 and tab-2 to have equal amount of rows.", rstm1.getRowCount()   , rstm2.getRowCount());
		assertEquals("expected tab-1 and tab-2 to have equal amount of cols.", rstm1.getColumnCount(), rstm2.getColumnCount());
		
		for (int r=0; r<rstm2.getRowCount(); r++)
		{
			String sqlText  = rstm1.getValueAsString(r, "SqlText");
			String planText = rstm2.getValueAsString(r, "PlanText");

			assertEquals("expected sqtext-"+r, sqlText, planText);
		}
	}

	@Test
	public void testCopyAllRows()
	throws SQLException
	{
		// Create ResultSet - 1
		SimpleResultSet srs1 = new SimpleResultSet();
		srs1.addColumn("key",     Types.VARCHAR,       30, 0);
		srs1.addColumn("val",     Types.VARCHAR,      512, 0);

		srs1.addRow("k1", "obj1");
		srs1.addRow("k2", "obj2");
		srs1.addRow("k3", "obj3");
		srs1.addRow("k4", "obj4");
		srs1.addRow("k5", "obj5");
		srs1.addRow("k6", "obj6");
		srs1.addRow("k7", "obj7");
		srs1.addRow("k8", "obj8");
		srs1.addRow("k9", "obj9");

		
		// Turn them into ResultSetTableModel
		ResultSetTableModel rstm1 = new ResultSetTableModel(srs1, "SRS-1");

		// Copy ALL rows
		ResultSetTableModel rstm_allRow = rstm1.copy();

//		System.out.println("rstm_allRow (" + rstm_allRow.getName() + ")\n" + rstm_allRow.toAsciiTableString());
//		System.out.println("rstm_allRow rowcount: " + rstm_allRow.getRowCount());

		assertEquals("expected rowcount should be 9, this is now " + rstm_allRow.getRowCount(), 9, rstm_allRow.getRowCount());
	}

	@Test
	public void testCopyFirstRow()
	throws SQLException
	{
		// Create ResultSet - 1
		SimpleResultSet srs1 = new SimpleResultSet();
		srs1.addColumn("key",     Types.VARCHAR,       30, 0);
		srs1.addColumn("val",     Types.VARCHAR,      512, 0);

		srs1.addRow("k1", "obj1");
		srs1.addRow("k2", "obj2");
		srs1.addRow("k3", "obj3");
		srs1.addRow("k4", "obj4");
		srs1.addRow("k5", "obj5");
		srs1.addRow("k6", "obj6");
		srs1.addRow("k7", "obj7");
		srs1.addRow("k8", "obj8");
		srs1.addRow("k9", "obj9");

		
		// Turn them into ResultSetTableModel
		ResultSetTableModel rstm1 = new ResultSetTableModel(srs1, "SRS-1");

		// Copy first row
		ResultSetTableModel rstm_firstRow = rstm1.copy(0, 1, null);

		System.out.println("rstm_firstRow (" + rstm_firstRow.getName() + ")\n" + rstm_firstRow.toAsciiTableString());
		System.out.println("rstm_firstRow rowcount: " + rstm_firstRow.getRowCount());

		assertEquals("expected rowcount should be 1, this is now " + rstm_firstRow.getRowCount(), 1, rstm_firstRow.getRowCount());
	}

}
