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
package com.asetune.config.dict;

import java.util.HashMap;

public class RemarkDictionary
{
	/** Instance variable */
	private static RemarkDictionary _instance = null;

	/** HashMap with descriptions */
	private HashMap<String,String> _remarkMap = new HashMap<String,String>();

	public RemarkDictionary()
	{
		init();
	}

	public static RemarkDictionary getInstance()
	{
		if (_instance == null)
			_instance = new RemarkDictionary();
		return _instance;
	}

	public void add(String key, String htmlDesc)
	{
		_remarkMap.put(key, htmlDesc);
	}

	public String getToolTipText(String id)
	{
		String desc = _remarkMap.get(id);
		return desc;
	}

	//----------------------------------------------
	// Remarks used in CmObjectActivity
	//----------------------------------------------
//	public static final String T_SCAN_OR_HTAB_INS = "TabScan/HeapTabIns";
	public static final String PROBABLY_TABLE_SCAN = "ProbTabScan";
	public static final String TABLE_SCAN          = "TabScan";
	public static final String HEAP_TAB_INS        = "HeapTabIns";
	
	//----------------------------------------------
	// Remarks used in CmStmntCacheDetails
	//----------------------------------------------
	public static final String SKEWED_EXEC_PLAN_ABS  = "SkewedExecPlan-Abs";
	public static final String SKEWED_EXEC_PLAN_DIFF = "SkewedExecPlan-Diff";


	private void init()
	{
		String key;
		String desc;

//		key = T_SCAN_OR_HTAB_INS;
//		desc = "<html>" +
//			"<h2>Table Scan OR Heap Table Insert</h2>" +
//			"This could be either a 'select that does a table scan' or a 'insert on table with no clustered index'" +
//
//			"<h3>Table Scan</h3>" +
//			"If it's a 'table scan', high LogicalReads should be visible.<br>" +
//			"Number of pages read per scan can be calculated using <code>LogicalReads/UsedCount</code><br>" +
//			"If number of pages read per scan is low (less tan 10-50 pages) then it may not be a problem.<br>" +
//			"<br>" +
//			"<b>Action:</b><br>" +
//			"Figure out what SQL statement(s) are accessing the table, what columns is used in the where " +
//			"clause, then investigate if appropriate indexing has been applied.<br>" +
//			"Also check if statistics has been updated on the table/indexes.<br>" +
//			"Use performance counter 'Active Statements' and/or the tool 'Capture SQL' to figure out what what SQL is issued.<br>" +
//
//			"<h3>Heap Table Insert</h3>" +
//			"If <b>no</b> Clustered Index exists on the table, inserts will go to the last page of the table, " +
//			"those types of tables are called 'heap tables'.<br>" +
//			"If UsedCount is approximately the same as RowsInserted, then this is mostly 'heap table' inserts." +
//			"</html>";
//		add(key, desc);

		key = PROBABLY_TABLE_SCAN;
		desc = "<html>" +
			"<h2>Probably Table Scan</h2>" +
			"This could be a 'select that does a table scan'.<br>" +
			"<b>Algorithm</b>: <code>IndexID == 0 && (PagesRead > 1000 || APFReads > 500)</code><br>" +
			"<br>" +

			"If it's a 'table scan', high LogicalReads/PhysicalReads/APFReads/PagesRead should be visible.<br>" +
			"I guess it's a <b>long</b> running table scans since the <i>normal 'TabScan' remark</i> didn't pick it up.<br>" +
			"To get current SQL Statement that is executing: check Performance counter 'Server-&gt;Active Statements'.<br>" +
			"<br>" +

			"<h3>Action:</h3>" +
			"Figure out what SQL statement(s) are accessing the table, what columns is used in the where " +
			"clause, then investigate if appropriate indexing has been applied.<br>" +
			"Also check if statistics has been updated on the table/indexes.<br>" +
			"Use performance counter 'Active Statements' and/or the tool 'Capture SQL' to figure out what what SQL is issued.<br>" +
			"</html>";
		add(key, desc);

	
	
		key = TABLE_SCAN;
		desc = "<html>" +
			"<h2>Table Scan</h2>" +
			"This could be a 'select that does a table scan'.<br>" +
			"<b>Algorithm</b>: <code>IndexID == 0 && UsedCount > 0 && LogicalReads > 0</code><br>" +
			"<br>" +

			"If it's a 'table scan', high LogicalReads should be visible.<br>" +
			"Number of pages read per scan can be calculated using <code>LogicalReads/UsedCount</code><br>" +
			"If number of pages read per scan is low (less tan 10-50 pages) then it may not be a problem.<br>" +
			"<br>" +
			"If it's a \"in-memory table scan\" or if ASE had to read pages from disk, would possible show up " +
			"as 'PhysicalReads', but in <b>most</b> cases it will show up as 'APFReads', since table scans from disk " +
			"is done (in most cases) as Asynchronous Pre Fetch.<br>" +
			"You can also check tab 'Devices' and column 'Reads' and 'APFReads'.<br>" +

			"<h3>Action:</h3>" +
			"Figure out what SQL statement(s) are accessing the table, what columns is used in the where " +
			"clause, then investigate if appropriate indexing has been applied.<br>" +
			"Also check if statistics has been updated on the table/indexes.<br>" +
			"Use performance counter 'Active Statements' and/or the tool 'Capture SQL' to figure out what what SQL is issued.<br>" +
			"</html>";
		add(key, desc);

	
	
		key = HEAP_TAB_INS;
		desc = "<html>" +
			"<h2>Heap Table Insert</h2>" +
			"This could be 'inserts on table with no clustered index'.<br>" +
			"<b>Algorithm</b>: <code>(IndexID == 0 && UsedCount > 0 && LogicalReads > 0) && UsedCount \"10 percent near\" RowsInserted</code><br>" +
			"<br>" +

			"If <b>no</b> Clustered Index exists on the table, inserts will go to the last page of the table, " +
			"those types of tables are called 'heap tables'.<br>" +
			"If UsedCount is approximately the same as RowsInserted, then this is mostly 'heap table' inserts." +

			"<h3>Action:</h3>" +
			"An clustered index, which distributes the insert location could be created." +
			"</html>";
		add(key, desc);

		
		
		key = SKEWED_EXEC_PLAN_ABS;
		desc = "<html>" +
			"<h2>Exec plan is skewed</h2>" +
			"This could be that the execution plan is reused but the execution plan was optimized for another parameter value'.<br>" +
			"This caused the execution engine to make to many IO's due to the fact that it was optimized for other constants.'.<br>" +
			"<b>Algorithm</b>: <code>LogicalIO(MinLIO & MaxLIO) differs more than 10%</code><br>" +
			"<br>" +

//			"If <b>no</b> Clustered Index exists on the table, inserts will go to the last page of the table, " +
//			"those types of tables are called 'heap tables'.<br>" +
//			"If UsedCount is approximately the same as RowsInserted, then this is mostly 'heap table' inserts." +

			"<h3>Action:</h3>" +
			"Create a nonclustered index (possibly a covered index). That contains more columns so we do not need to access base table." +
			"</html>";
		add(key, desc);

		key = SKEWED_EXEC_PLAN_DIFF;
		//desc = desc; // Use the same as for SKEWED_EXEC_PLAN_ABS
		add(key, desc);
	}
	
}
