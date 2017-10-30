package com.asetune.tools.sqlw.msg;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

public class StatisticsIoTableModel
extends AbstractTableModel
{
	private static Logger _logger = Logger.getLogger(StatisticsIoTableModel.class);
	private static final long serialVersionUID = 1L;
	
	private static final int UNKNOWN    = 0;
	private static final int SYBASE_ASE = 1;
	private static final int SQLSERVER  = 2;

	private static final int COL_COUNT_SYBASE_ASE = 9;
	private static final int COL_COUNT_SQLSERVER  = 8;

	private int _type = -1;
	
	private ArrayList<StatIoEntry> _rows = new ArrayList<>();

	@Override
	public int getRowCount()
	{
		return _rows.size();
	}

	@Override
	public String getColumnName(int column)
	{
		if (_type == SYBASE_ASE)
		{
			switch (column)
			{
			case 0: return "Table"; 
			case 1: return "ScanCount"; 
			case 2: return "LogicalReads-total"; 
			case 3: return "LogicalReads-regular"; 
			case 4: return "LogicalReads-apf"; 
			case 5: return "PhysicalReads-total"; 
			case 6: return "PhysicalReads-regular"; 
			case 7: return "PhysicalReads-apf"; 
			case 8: return "ApfIosUsed"; 
			}
		}
		if (_type == SQLSERVER)
		{
			switch (column)
			{
			case 0: return "Table"; 
			case 1: return "ScanCount"; 
			case 2: return "LogicalReads"; 
			case 3: return "PhysicalReads"; 
			case 4: return "ReadAheadReads"; 
			case 5: return "LobLogicalReads"; 
			case 6: return "LobPhysicalReads"; 
			case 7: return "LobReadAheadReads"; 
			}
		}
		return "unknown-"+column;
	}
	
	@Override
	public int getColumnCount()
	{
		if (_type == SYBASE_ASE) return COL_COUNT_SYBASE_ASE;
		if (_type == SQLSERVER)  return COL_COUNT_SQLSERVER;

		return 0;
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		if (column == 0) return String.class;
		return Integer.class;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		if (_type == SYBASE_ASE)
		{
			AseServerEntry entry = (AseServerEntry) _rows.get(row);
			switch (column)
			{
			case 0: return entry._table          ;
			case 1: return entry._scanCount      ;
			case 2: return entry._logReadsTotal  ;
			case 3: return entry._logReadsRegular; 
			case 4: return entry._logReadsApf    ;
			case 5: return entry._phyReadsTotal  ;
			case 6: return entry._phyReadsRegular; 
			case 7: return entry._phyReadsApf    ;
			case 8: return entry._apfIosUsed     ;
			}
		}
		if (_type == SQLSERVER)
		{
			SqlServerEntry entry = (SqlServerEntry) _rows.get(row);
			switch (column)
			{
			case 0: return entry._table            ;
			case 1: return entry._scanCount        ;
			case 2: return entry._logicalReads     ;
			case 3: return entry._physicalReads    ;
			case 4: return entry._readAheadReads   ;
			case 5: return entry._lobLogicalReads  ;
			case 6: return entry._lobPhysicalReads ;
			case 7: return entry._lobReadAheadReads; 
			}
		}
		return "unknown-" + row + "-" + column;
	}

	public void doSummary()
	{
		if (_type == SYBASE_ASE)
		{
			AseServerEntry sumEntry = new AseServerEntry();

			sumEntry._table           = "--SUMMARY--";
			for (StatIoEntry statIoEntry : _rows)
			{
				AseServerEntry srvEntry = (AseServerEntry) statIoEntry;

				sumEntry._scanCount       += srvEntry._scanCount      ;
				sumEntry._logReadsTotal   += srvEntry._logReadsTotal  ;
				sumEntry._logReadsRegular += srvEntry._logReadsRegular;
				sumEntry._logReadsApf     += srvEntry._logReadsApf    ;
				sumEntry._phyReadsTotal   += srvEntry._phyReadsTotal  ;
				sumEntry._phyReadsRegular += srvEntry._phyReadsRegular;
				sumEntry._phyReadsApf     += srvEntry._phyReadsApf    ;
				sumEntry._apfIosUsed      += srvEntry._apfIosUsed     ;
			}
			_rows.add(sumEntry);
		}
		else if (_type == SQLSERVER)
		{
			SqlServerEntry sumEntry = new SqlServerEntry();

			sumEntry._table           = "--SUMMARY--";
			for (StatIoEntry statIoEntry : _rows)
			{
				SqlServerEntry srvEntry = (SqlServerEntry) statIoEntry;

				sumEntry._scanCount         += srvEntry._scanCount        ;      
				sumEntry._logicalReads      += srvEntry._logicalReads     ;   
				sumEntry._physicalReads     += srvEntry._physicalReads    ;  
				sumEntry._readAheadReads    += srvEntry._readAheadReads   ; 
				sumEntry._lobLogicalReads   += srvEntry._lobLogicalReads  ;
				sumEntry._lobPhysicalReads  += srvEntry._lobPhysicalReads ;
				sumEntry._lobReadAheadReads += srvEntry._lobReadAheadReads;
			}
			_rows.add(sumEntry);
		}
	}

	// Strip all but numbers, and convert it into an integer
	private int getInt(String str)
	{
		str = str.replaceAll("[^0-9]", "");
		try
		{
			return Integer.parseInt(str);
		}
		catch (NumberFormatException nfe)
		{
			_logger.error("StatisticsIoTableModel.getInt(): Problem parsing the value '"+str+"'. Caught: "+nfe);
			return -99;
		}
	}


	public void addMessage(String msgText)
	{
//System.out.println("addMessage: msgText='"+msgText+"'");

		// typical ASE msg: Table: sysdevices (t1) scan count 1, logical reads: (regular=3 apf=0 total=3), physical reads: (regular=0 apf=0 total=0), apf IOs used=0
		// typical ASE msg: Table: sysdevices scan count 1, logical reads: (regular=3 apf=0 total=3), physical reads: (regular=0 apf=0 total=0), apf IOs used=0

		// typical SQL-Server msg: Table 'sysschobjs'. Scan count 1, logical reads 37, physical reads 0, read-ahead reads 0, lob logical reads 0, lob physical reads 0, lob read-ahead reads 0.

		if (_type < 0)
		{
			if      (msgText.startsWith("Table: ")) _type = SYBASE_ASE;
			else if (msgText.startsWith("Table '")) _type = SQLSERVER;
			else {
				_type = UNKNOWN;
				return;
			}
//			// A little bit more language insensitive, but then I figgured out that we are looking for 'can count' later... So lets just hope it works...
//			if      (msgText.indexOf(':')  >= 0) _type = SYBASE_ASE;
//			else if (msgText.indexOf('\'') >= 0) _type = SQLSERVER;
//			else {
//				_type = UNKNOWN;
//				return;
//			}
		}
		
		int pos = msgText.indexOf("can count ") - 1;
		if (pos < 0)
			return;

		String startAtScanCount = msgText.substring(pos);
		String tabName          = msgText.substring("Table ".length(), pos).trim();
		
		String[] sa = startAtScanCount.split(" ");
//System.out.println("addMessage: _type="+_type+", sa.length="+sa.length+", tabName='"+tabName+"', sa="+StringUtil.toCommaStr(sa, "||"));
		
		StatIoEntry entry = null;
		if (_type == SYBASE_ASE)
		{
			AseServerEntry srvEntry = new AseServerEntry();
			entry = srvEntry;

			srvEntry._table           = tabName;
			srvEntry._scanCount       = getInt( sa[2]  );
			srvEntry._logReadsTotal   = getInt( sa[7]  );
			srvEntry._logReadsRegular = getInt( sa[5]  );
			srvEntry._logReadsApf     = getInt( sa[6]  );
			srvEntry._phyReadsTotal   = getInt( sa[12] );
			srvEntry._phyReadsRegular = getInt( sa[10] );
			srvEntry._phyReadsApf     = getInt( sa[11] );
			srvEntry._apfIosUsed      = getInt( sa[15] );
		}
		else if (_type == SQLSERVER)
		{
			SqlServerEntry srvEntry = new SqlServerEntry();
			entry = srvEntry;

			srvEntry._table             = tabName.replace("'.", "").replace("'", ""); // Remove suroundings of table name: Table 'sysschobjs'. Scan count...
			srvEntry._scanCount         = getInt( sa[2]  );      
			srvEntry._logicalReads      = getInt( sa[5]  );   
			srvEntry._physicalReads     = getInt( sa[8]  );  
			srvEntry._readAheadReads    = getInt( sa[11] ); 
			srvEntry._lobLogicalReads   = getInt( sa[15] );
			srvEntry._lobPhysicalReads  = getInt( sa[19] );
			srvEntry._lobReadAheadReads = getInt( sa[23] );
		}
	
//System.out.println("add: "+entry);
		if (entry != null)
			_rows.add(entry);
	}

	private static class StatIoEntry
	{
		String _table = null;
	}

	private static class AseServerEntry
	extends StatIoEntry
	{
		int _scanCount; 
		int _logReadsTotal; 
		int _logReadsRegular; 
		int _logReadsApf; 
		int _phyReadsTotal; 
		int _phyReadsRegular; 
		int _phyReadsApf; 
		int _apfIosUsed; 
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("table"          ).append("=").append(_table          ).append(", ");
			sb.append("scanCount"      ).append("=").append(_scanCount      ).append(", ");
			sb.append("logReadsTotal"  ).append("=").append(_logReadsTotal  ).append(", ");
			sb.append("logReadsRegular").append("=").append(_logReadsRegular).append(", ");
			sb.append("logReadsApf"    ).append("=").append(_logReadsApf    ).append(", ");
			sb.append("phyReadsTotal"  ).append("=").append(_phyReadsTotal  ).append(", ");
			sb.append("phyReadsRegular").append("=").append(_phyReadsRegular).append(", ");
			sb.append("phyReadsApf"    ).append("=").append(_phyReadsApf    ).append(", ");
			sb.append("apfIosUsed"     ).append("=").append(_apfIosUsed     ).append(".");
			return sb.toString();
		}
	}

	private static class SqlServerEntry
	extends StatIoEntry
	{
		int _scanCount;      
		int _logicalReads;   
		int _physicalReads;  
		int _readAheadReads; 
		int _lobLogicalReads;
		int _lobPhysicalReads;
		int _lobReadAheadReads;

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("table"            ).append("=").append(_table            ).append(", ");
			sb.append("scanCount"        ).append("=").append(_scanCount        ).append(", ");
			sb.append("logicalReads"     ).append("=").append(_logicalReads     ).append(", ");
			sb.append("physicalReads"    ).append("=").append(_physicalReads    ).append(", ");
			sb.append("readAheadReads"   ).append("=").append(_readAheadReads   ).append(", ");
			sb.append("lobLogicalReads"  ).append("=").append(_lobLogicalReads  ).append(", ");
			sb.append("lobPhysicalReads" ).append("=").append(_lobPhysicalReads ).append(", ");
			sb.append("lobReadAheadReads").append("=").append(_lobReadAheadReads).append(".");
			return sb.toString();
		}
	}
}
