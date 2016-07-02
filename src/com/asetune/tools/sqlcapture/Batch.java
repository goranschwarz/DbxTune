/**
 */

package com.asetune.tools.sqlcapture;

import java.util.ArrayList;
import java.util.Iterator;

class Batch
{
	protected int	   spid          = 0;
	protected int	   kpid          = 0;
	protected int	   batchId       = 0;

	protected int      dbid          = 0;
	protected int	   objectOwnerId = 0;
	protected int	   procedureId   = 0;  // Id of the StoredProc (if any)
	protected String   dbname        = ""; // Name of the database
	protected String   procedureName = ""; // name of the StoredProc (if any)

	protected int      planId        = 0;  // ID of the compiled plan
	protected int      lineNumber    = 0;  // Line number of the stored proc or the batch

	private ArrayList<StringBuilder> _sqlTextLines = new ArrayList<StringBuilder>();
	private ArrayList<StringBuilder> _showplanLines = new ArrayList<StringBuilder>();

	Batch()
	{
	}
	Batch(int bID)
	{
		this.batchId = bID;
	}
	Batch(int spid, int kpid, int batchId)
	{
		this.spid    = spid;
		this.kpid    = kpid;
		this.batchId = batchId;
	}

	public String getKey()
	{
		return this.spid + ":" + this.kpid + ":" + this.batchId;
	}

	public void addSqlTextLine(StringBuilder line)
	{
		_sqlTextLines.add(line);
	}
	public void appendSqlText(String line)
	{
		StringBuilder sb = null;

		if ( _sqlTextLines.size() == 0 )
		{
			sb = new StringBuilder(line);
			_sqlTextLines.add( sb );
		}
		else
		{
			sb = _sqlTextLines.get(_sqlTextLines.size()-1);
			sb.append(line);
		}
	}

	public void addShowplanTextLine(StringBuilder line)
	{
		addShowplanTextLine(line, false);
	}
	public void addShowplanTextLine(StringBuilder line, boolean chopLastNewLine)
	{
		// Get rid of last newline
		if (chopLastNewLine)
		{
			int len = line.length() - 1;
			if ( line.charAt(len) == '\n')
			{
				line.setCharAt(len, ' ');
			}
		}

		_showplanLines.add(line);
	}
	public void appendShowplanText(String line)
	{
		appendShowplanText(line, false);
	}
	public void appendShowplanText(String line, boolean chopLastNewLine)
	{
		if (line == null)
			return;

		StringBuilder sb = null;

		// Get rid of last newline
		if (chopLastNewLine)
		{
			int len = line.length() - 1;
			if ( len > 0 && line.charAt(len) == '\n')
			{
				line = line.substring(0, len);
			}
		}

		if ( _showplanLines.size() == 0 )
		{
			sb = new StringBuilder(line);
			_showplanLines.add( sb );
		}
		else
		{
			sb = _showplanLines.get(_showplanLines.size()-1);
			sb.append(line);
		}
	}

//	public Iterator getIterator()
//	{
//		SqlTextIterator bi = new SqlTextIterator();
//		return bi;
//	}

	public Iterator getSqlTextIterator()
	{
		SqlTextIterator bi = new SqlTextIterator();
		return bi;
	}

	public Iterator getShowplanTextIterator()
	{
		ShowplanTextIterator bi = new ShowplanTextIterator();
		return bi;
	}

	public int getBatchId()
	{
		return batchId;
	}

	public String getSqlText()
	{
		return getSqlText(false);
	}
	public String getSqlText(boolean number)
	{
		Iterator bi = this.getSqlTextIterator();
		StringBuilder sb = new StringBuilder ();
		int lineNum=0;
		while (bi.hasNext()) 
		{
			lineNum++;
			
			if (number)
				sb.append( lineNum + ": " + bi.next() + "\n" );
			else
				sb.append( bi.next() + "\n" );
		}
		return sb.toString();
	}

	public String getShowplanText()
	{
		Iterator bi = this.getShowplanTextIterator();
		StringBuilder sb = new StringBuilder ();
		while (bi.hasNext()) 
		{
			sb.append( bi.next() + "\n" );
		}
		return sb.toString();
	}
	
    
	private class SqlTextIterator implements Iterator
	{
		int	currentLine;

		SqlTextIterator()
		{
			currentLine = 0;
		}

		public boolean hasNext()
		{
			if (currentLine < _sqlTextLines.size())
				return true;
			else
				return false;
		}

		public Object next()
		{
			currentLine++;
			return _sqlTextLines.get(currentLine - 1);
		}

		public void remove()
		{
		}
	}

	private class ShowplanTextIterator implements Iterator
	{
		int	currentLine;

		ShowplanTextIterator()
		{
			currentLine = 0;
		}

		public boolean hasNext()
		{
			if (currentLine < _showplanLines.size())
				return true;
			else
				return false;
		}

		public Object next()
		{
			currentLine++;
			return _showplanLines.get(currentLine - 1);
		}

		public void remove()
		{
		}
	}
}
