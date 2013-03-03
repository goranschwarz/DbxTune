/**
 */

package com.asetune;

import java.util.Iterator;
import java.util.Vector;

class Batch
{
	protected int	   spid          = 0;
	protected int	   kpid          = 0;
	protected int	   batchId       = 0;

	protected int      dbid          = 0;
	protected int	   procedureId   = 0;  // Id of the StoredProc (if any)
	protected String   dbname        = ""; // Name of the database
	protected String   procedureName = ""; // name of the StoredProc (if any)

	protected int      planId        = 0;  // ID of the compiled plan
	protected int      lineNumber    = 0;  // Line number of the stored proc or the batch

	private Vector	   sqlTextLines   = new Vector();
	private Vector	   showplanLines  = new Vector();

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

	public void addSqlTextLine(StringBuffer line)
	{
		sqlTextLines.add(line);
	}
	public void appendSqlText(String line)
	{
		StringBuffer sb = null;

		if ( sqlTextLines.size() == 0 )
		{
			sb = new StringBuffer(line);
			sqlTextLines.add( sb );
		}
		else
		{
			sb = (StringBuffer) sqlTextLines.lastElement();
			sb.append(line);
		}
	}

	public void addShowplanTextLine(StringBuffer line)
	{
		addShowplanTextLine(line, false);
	}
	public void addShowplanTextLine(StringBuffer line, boolean chopLastNewLine)
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

		showplanLines.add(line);
	}
	public void appendShowplanText(String line)
	{
		appendShowplanText(line, false);
	}
	public void appendShowplanText(String line, boolean chopLastNewLine)
	{
		if (line == null)
			return;

		StringBuffer sb = null;

		// Get rid of last newline
		if (chopLastNewLine)
		{
			int len = line.length() - 1;
			if ( len > 0 && line.charAt(len) == '\n')
			{
				line = line.substring(0, len);
			}
		}

		if ( showplanLines.size() == 0 )
		{
			sb = new StringBuffer(line);
			showplanLines.add( sb );
		}
		else
		{
			sb = (StringBuffer) showplanLines.lastElement();
			sb.append(line);
		}
	}

	public Iterator getIterator()
	{
		SqlTextIterator bi = new SqlTextIterator();
		return bi;
	}

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
		StringBuffer sb = new StringBuffer ();
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
		StringBuffer sb = new StringBuffer ();
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
			if (currentLine < sqlTextLines.size())
				return true;
			else
				return false;
		}

		public Object next()
		{
			currentLine++;
			return sqlTextLines.get(currentLine - 1);
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
			if (currentLine < showplanLines.size())
				return true;
			else
				return false;
		}

		public Object next()
		{
			currentLine++;
			return showplanLines.get(currentLine - 1);
		}

		public void remove()
		{
		}
	}
}
