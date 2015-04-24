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

//	public static final String T_SCAN_OR_HTAB_INS = "TabScan/HeapTabIns";
	public static final String TABLE_SCAN   = "TabScan";
	public static final String HEAP_TAB_INS = "HeapTabIns";
	
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
	}
	
}
