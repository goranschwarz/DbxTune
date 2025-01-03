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
package com.dbxtune.tools.sqlw.msg;

import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.NumberUtils;

public class StatisticsIoTableModel
extends AbstractTableModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;
	
	private static final int UNKNOWN        = 0;
	private static final int SYBASE_ASE     = 1;
	private static final int SQLSERVER      = 2;
	private static final int SQLSERVER_2019 = 3;

	private static final int COL_COUNT_SYBASE_ASE     = 9;
	private static final int COL_COUNT_SQLSERVER      = 8;
	private static final int COL_COUNT_SQLSERVER_2019 = 12;

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
		if (_type == SQLSERVER_2019)
		{			
			switch (column)
			{
			case 0:  return "Table"; 
			case 1:  return "ScanCount";                  
			case 2:  return "LogicalReads";               
			case 3:  return "PhysicalReads";              
			case 4:  return "PageServerReads";            
			case 5:  return "ReadAheadReads";             
			case 6:  return "PageServerReadAheadReads";   
			case 7:  return "LobLogicalReads";             
			case 8:  return "LobPhysicalReads";            
			case 9:  return "LobPageServerReads";          
			case 10: return "LobReadAheadReads";           
			case 11: return "LobPageServerReadAheadReads"; 
			}
		}
		return "unknown-"+column;
	}
	
	@Override
	public int getColumnCount()
	{
		if (_type == SYBASE_ASE)     return COL_COUNT_SYBASE_ASE;
		if (_type == SQLSERVER)      return COL_COUNT_SQLSERVER;
		if (_type == SQLSERVER_2019) return COL_COUNT_SQLSERVER_2019;

		return 0;
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		if (column == 0) return String.class;
		return Integer.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return true; // Just to make it easier to copy and paste from
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
		if (_type == SQLSERVER_2019)
		{
			SqlServerEntry2019 entry = (SqlServerEntry2019) _rows.get(row);
			switch (column)
			{
			case 0:  return entry._table; 
			case 1:  return entry._scanCount;                  
			case 2:  return entry._logicalReads;               
			case 3:  return entry._physicalReads;              
			case 4:  return entry._pageServerReads;            
			case 5:  return entry._readAheadReads;             
			case 6:  return entry._pageServerReadAheadReads;   
			case 7:  return entry._lobLogicalReads;             
			case 8:  return entry._lobPhysicalReads;            
			case 9:  return entry._lobPageServerReads;          
			case 10: return entry._lobReadAheadReads;           
			case 11: return entry._lobPageServerReadAheadReads; 
			}
		}
		return "unknown-" + row + "-" + column;
	}

	public String getToolTipText(int column)
	{
		if (_type == SYBASE_ASE)
		{
			switch (column)
			{
			case 0: return "Table Name";
			case 1: return "<html> "
					+ "A <i>scan</i> can represent a number of different access methods.<br> "
					+ "The statistics io command reports the number of times a query accessed a particular table. A <i>scan</i> can represent any of these access methods: "
					+ "<ul>"
					+ "  <li>A <b>table scan</b>.</li>"
					+ "  <li>An access by way of a <b>clustered index</b>. Each time the query starts at the root page of the index and follows pointers to the data pages, it is counted as a scan.</li>"
					+ "  <li>An access by way of a <b>nonclustered index</b>. Each time the query starts at the root page of the index and follows pointers to the leaf level of the index (for a covered query) or to the data pages, it is counted.</li>"
					+ "  <li>If queries run in parallel, each worker process access to the table is counted as a scan.</li>"
					+ "</ul>"
					+ "</html>"; 
			case 2: return "Sum of <b>regular</b> and <b>apf</b> logical reads."; 
			case 3: return "Any page read request (whether it hits the cache or not) is considered a logical read; only pages not brought in by asynchronous prefetch (APF) are counted here."; 
			case 4: return "Number of times that a request brought in by an APF request was found in cache."; 
			case 5: return "Sum of <b>regular</b> and <b>apf</b> physical reads."; 
			case 6: return "Number of times a buffer was brought into cache by regular asynchronous I/O."; 
			case 7: return "Number of times that a buffer was brought into cache by APF."; 
			case 8: return "Number of buffers brought in by APF in which one or more pages were used during the query."; 
			}
		}
		if (_type == SQLSERVER)
		{
			switch (column)
			{
			case 0: return "Table Name"; 
			case 1: return "Number of seeks or scans started to retrieve all the values to construct the final dataset for the output."; 
			case 2: return "A read of a data page from memory."; 
			case 3: return "A read of a data page from disk when it is not available in memory."; 
			case 4: return "A read ahead read transfers a data page from disk to memory before it is specifically requested."; 
			case 5: return "Logical reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages."; 
			case 6: return "Physical reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages."; 
			case 7: return "Refers to read-ahead reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages."; 
			}
		}
		if (_type == SQLSERVER_2019)
		{			
			// from https://www.mssqltips.com/sqlservertip/6433/query-tuning-in-sql-server-with-set-statistics-io/
			switch (column)
			{
			case 0:  return "Table Name"; 
			case 1:  return "Number of seeks or scans started to retrieve all the values to construct the final dataset for the output.";                  
			case 2:  return "A read of a data page from memory.";               
			case 3:  return "A read of a data page from disk when it is not available in memory.";              
			case 4:  return "Refers to the transfer of a page from disk to the data buffer in memory.  The page server reads per second reflects the number of page reads across all databases.";            
			case 5:  return "A read ahead read transfers a data page from disk to memory before it is specifically requested.";             
			case 6:  return "Refers to the transfer of a page from disk to the data buffer in memory before it is specifically requested.  Reflects read-ahead read throughput in the same way that page server reads reflect physical reads.";   
			case 7:  return "Logical reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages.";             
			case 8:  return "Physical reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages.";            
			case 9:  return "Refers to the transfer of a text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages from disk to the data buffer in memory across all databases.";          
			case 10: return "Refers to read-ahead reads for text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages.";           
			case 11: return "Refers to the transfer of a text, ntext, image, varchar(max), nvarchar(max), varbinary(max), or columnstore index pages from disk to the data buffer in memory before it is specifically requested across all databases."; 
			}
		}
		return "unknown-"+column;
	}
	public String getCellToolTipText(int mrow, int mcol, int srvPageSizeKb)
	{
//		System.out.println("getCellToolTipText(): mrow=" + mrow + ", mcol=" + mcol + ", srvPageSizeKb=" + srvPageSizeKb);

		if (mcol == 0) return null; // TableName
		if (mcol == 1) return null; // Scan Count

		Object o_cellVal = getValueAt(mrow, mcol);
		if (o_cellVal != null &&  o_cellVal instanceof Integer)
		{
			int cellVal = (Integer) o_cellVal;

			if (cellVal == 0)
				return null;
			
			NumberFormat nf = NumberFormat.getInstance();

			if (_type == SYBASE_ASE)
			{
				if (srvPageSizeKb > 0)
				{
					return "<html>" + nf.format(cellVal) + " Pages is: <br>"
							+ "<b>" + nf.format(NumberUtils.round(cellVal / (1024.0/srvPageSizeKb), 1)) + " MB</b> (in " + srvPageSizeKb + "K pages) <br>"
							+ " </html>";
				}
				else
				{
					// Show for all Pages sizes, if we dont know the SrvPageSize
					return "<html>" + nf.format(cellVal) + " Pages is: <br>"
							+ "<b>" + nf.format(NumberUtils.round(cellVal / 512.0, 1)) + " MB</b> (in 2K pages) <br>"
							+ "<b>" + nf.format(NumberUtils.round(cellVal / 256.0, 1)) + " MB</b> (in 4K pages) <br>"
							+ "<b>" + nf.format(NumberUtils.round(cellVal / 128.0, 1)) + " MB</b> (in 8K pages) <br>"
							+ "<b>" + nf.format(NumberUtils.round(cellVal /  64.0, 1)) + " MB</b> (in 16K pages) <br>"
							+ "</html>";
				}
			}
			else
			{
				// SQL-Server 
				return "<html>" + nf.format(cellVal) + " Pages is: <br>"
						+ "<b>" + nf.format(NumberUtils.round(cellVal / 128.0, 1)) + " MB</b> (in 8K pages) <br>"
						+ " </html>";
			}
		}
		
		return null;
	}

	public void doSummary()
	{
		if (_type == SYBASE_ASE)
		{
			AseServerEntry sumEntry = new AseServerEntry();

			sumEntry._table           = "--SUMMARY--";
			for (StatIoEntry statIoEntry : _rows)
			{
//				System.out.println("SUM: Sybase ASE: _rows.size=" + _rows.size());
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
//			System.out.println("SUM: SQL-Server: _rows.size=" + _rows.size());
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
		else if (_type == SQLSERVER_2019)
		{
//			System.out.println("SUM: SQL-Server 2019: _rows.size=" + _rows.size());
			SqlServerEntry2019 sumEntry = new SqlServerEntry2019();

			sumEntry._table           = "--SUMMARY--";
			for (StatIoEntry statIoEntry : _rows)
			{
				SqlServerEntry2019 srvEntry = (SqlServerEntry2019) statIoEntry;
//				System.out.println("SUM: SQL-Server 2019: "+srvEntry);

				sumEntry._scanCount                   += srvEntry._scanCount                  ;
				sumEntry._logicalReads                += srvEntry._logicalReads               ;
				sumEntry._physicalReads               += srvEntry._physicalReads              ;
				sumEntry._pageServerReads             += srvEntry._pageServerReads            ;
				sumEntry._readAheadReads              += srvEntry._readAheadReads             ;
				sumEntry._pageServerReadAheadReads    += srvEntry._pageServerReadAheadReads   ;
				sumEntry._lobLogicalReads             += srvEntry._lobLogicalReads            ;
				sumEntry._lobPhysicalReads            += srvEntry._lobPhysicalReads           ;
				sumEntry._lobPageServerReads          += srvEntry._lobPageServerReads         ;
				sumEntry._lobReadAheadReads           += srvEntry._lobReadAheadReads          ;
				sumEntry._lobPageServerReadAheadReads += srvEntry._lobPageServerReadAheadReads; 
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
//		System.out.println("addMessage: msgText='"+msgText+"'");

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

//		System.out.println("addMessage: _type="+_type+", sa.length="+sa.length+", tabName='"+tabName+"', sa="+StringUtil.toCommaStrQuoted('|', sa));
//		for (int i = 0; i < sa.length; i++)
//			System.out.println("      as[" + i + "] = |" + sa[i] + "|.");

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

			_rows.add(entry);
//			System.out.println("add SYBASE ASE, size=" + _rows.size()+ ": "+entry);
		}
		else if (_type == SQLSERVER || _type == SQLSERVER_2019)
		{
			if (sa.length <= 24)
			{
				_type = SQLSERVER;
				SqlServerEntry srvEntry = new SqlServerEntry();
				entry = srvEntry;

				// This is how it looked in 200x
				// /* 2016 */ Table 'Posts'. Scan count 3, logical reads 4188092, physical reads 3, read-ahead reads 4168416, lob logical reads 0, lob physical reads 0, lob read-ahead reads 0.
				// /* 2017 */ Table 'sysschobjs'. Scan count 1, logical reads 56, physical reads 0, read-ahead reads 0, lob logical reads 0, lob physical reads 0, lob read-ahead reads 0.
				// pos:-->>       -             -    0     1 2        3     4  5         6     7 8           9    10 11  12      13    14 15  16       17    18 19  20         21    22 23
				//            ^^^^^^^^^^^^^^^^^^^^
				//            The above is removed from input
				srvEntry._table             = tabName.replace("'.", "").replace("'", ""); // Remove suroundings of table name: Table 'sysschobjs'. Scan count...
				srvEntry._scanCount         = getInt( sa[2]  );      
				srvEntry._logicalReads      = getInt( sa[5]  );   
				srvEntry._physicalReads     = getInt( sa[8]  );  
				srvEntry._readAheadReads    = getInt( sa[11] ); 
				srvEntry._lobLogicalReads   = getInt( sa[15] );
				srvEntry._lobPhysicalReads  = getInt( sa[19] );
				srvEntry._lobReadAheadReads = getInt( sa[23] );

				_rows.add(entry);
//				System.out.println("add SQL-Server, size=" + _rows.size()+ ": "+entry);
			}
			else
			{
				_type = SQLSERVER_2019;
				SqlServerEntry2019 srvEntry = new SqlServerEntry2019();
				entry = srvEntry;

				// This is how it looks in 2019
				// /*2019:*/ Table 'Posts'. Scan count 3, logical reads 803579,  physical reads 3, page server reads 0, read-ahead reads 798473, page server read-ahead reads 0, lob logical reads 0, lob physical reads 0, lob page server reads 0, lob read-ahead reads 0, lob page server read-ahead reads 0.
				// pos:-->>      -        -    0     1 2        3     4      5          6     7 8     9     10    11 12         13    14     15    16     17         18    19 20  21      22    23 24  25       26    27 28  29   30     31    32 33  34         35    36 37  38   39     40         41    42 43
				//            ^^^^^^^^^^^^^^^^^^^^

				srvEntry._table                       = tabName.replace("'.", "").replace("'", ""); // Remove suroundings of table name: Table 'sysschobjs'. Scan count...
				srvEntry._scanCount                   = getInt( sa[2]  );      
				srvEntry._logicalReads                = getInt( sa[5]  );   
				srvEntry._physicalReads               = getInt( sa[8]  );  
				srvEntry._pageServerReads             = getInt( sa[12] );  
				srvEntry._readAheadReads              = getInt( sa[15] ); 
				srvEntry._pageServerReadAheadReads    = getInt( sa[20] );  
				srvEntry._lobLogicalReads             = getInt( sa[24] );
				srvEntry._lobPhysicalReads            = getInt( sa[28] );
				srvEntry._lobPageServerReads          = getInt( sa[33] );
				srvEntry._lobReadAheadReads           = getInt( sa[37] );
				srvEntry._lobPageServerReadAheadReads = getInt( sa[43] );

				_rows.add(entry);
//				System.out.println("add: SQL-Server 2019, size=" + _rows.size()+ ": "+entry);
			}
		}	
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

	private static class SqlServerEntry2019
	extends StatIoEntry
	{
		int _scanCount;      
		int _logicalReads;   
		int _physicalReads;  
		int _pageServerReads;  
		int _readAheadReads; 
		int _pageServerReadAheadReads;  
		int _lobLogicalReads;
		int _lobPhysicalReads;
		int _lobPageServerReads;
		int _lobReadAheadReads;
		int _lobPageServerReadAheadReads;

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("table"                      ).append("=").append(_table                      ).append(", ");
			sb.append("scanCount"                  ).append("=").append(_scanCount                  ).append(", ");
			sb.append("logicalReads"               ).append("=").append(_logicalReads               ).append(", ");
			sb.append("physicalReads"              ).append("=").append(_physicalReads              ).append(", ");
			sb.append("pageServerReads"            ).append("=").append(_pageServerReads            ).append(", ");
			sb.append("readAheadReads"             ).append("=").append(_readAheadReads             ).append(", ");
			sb.append("pageServerReadAheadReads"   ).append("=").append(_pageServerReadAheadReads   ).append(", ");
			sb.append("lobLogicalReads"            ).append("=").append(_lobLogicalReads            ).append(", ");
			sb.append("lobPhysicalReads"           ).append("=").append(_lobPhysicalReads           ).append(", ");
			sb.append("lobPageServerReads"         ).append("=").append(_lobPageServerReads         ).append(", ");
			sb.append("lobReadAheadReads"          ).append("=").append(_lobReadAheadReads          ).append(", ");
			sb.append("lobPageServerReadAheadReads").append("=").append(_lobPageServerReadAheadReads).append(".");
			return sb.toString();
		}
	}
}
