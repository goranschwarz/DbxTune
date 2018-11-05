package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import com.asetune.sql.pipe.PipeCommand;
import com.asetune.sql.pipe.PipeCommandException;

/**
 * Read a file or a String with 'go' terminations.<br>
 * Just like isql would do it.
 * <p>
 * 'isql' commands that are implemented
 * <ul>
 *     <li>go [count] [subCmd] - execute the above SQL Statement(s), <code>[count]</code> is how many times the statements should be executed.</li>
 *     <li>exit       - do not continue</li>
 *     <li>quit       - do not continue</li>
 *     <li>reset      - skip above statements and continue with next batch</li>
 * </ul>
 * 'isql' commands that are <b>NOT</b> implemented
 * <ul>
 *     <li>!! command - OS Commands are not supported.</li>
 *     <li>vi         - Editing the batch is not supported</li>
 *     <li>:r         - Read another file</li>
 *     <li>:w         - Write to another file</li>
 * </ul>
 * 
 * @author gorans
 */
public class AseSqlScriptReader
{
	public static final String	PROPKEY_sqlBatchTerminator	= "AseSqlScriptReader.sqlBatchTerminator";
	public static final String	DEFAULT_sqlBatchTerminator	= "go";

	/** SQL in case of String input */
	private String          _sqlStr                = null;

	/** The file in case of a File input */
	private File            _file                  = null;

	/** read SQL after last 'go' executor */
	private boolean         _execWithoutGoAtTheEnd = false;

	private String          _goTerminator          = DEFAULT_sqlBatchTerminator.toLowerCase();
	
	/** send SQL after a ';' is the last character at the end */
	private boolean         _useSemiColonHack      = false;
	private SemiColonHelper _semiColonHelper       = null;

	/** Alternative 'go' terminator. for example HANA and Oracle uses '/' */
	private String          _alternativeGoTerminator = null;

	private int             _multiExecCount        = 0;
	private int             _multiExecWait         = 0;
	private PipeCommand     _pipeCommand           = null;
	private int             _batchStartLine        = -1;
	private int             _batchNumber           = -1;
	private int             _totalBatchCount       = -1;
	
	private int             _topRows               = -1; // option 'top #'
	private int             _bottomRows            = -1; // option 'bottom #'
	private int             _rowCount              = -1; // option 'rowc'
	private int             _asPlainText           = -1; // option 'plain'
	private int             _noData                = -1; // option 'nodata'
	private int             _appendOutput          = -1; // option 'append'
	private int             _printSql              = -1; // option 'psql' -- print SQL Statement
	private int             _printRsi              = -1; // option 'prsi' -- print ResultSet Information
	private int             _printClientTiming     = -1; // option 'time' -- print client timing
	
	/** keep track of where in the file we are */
	private int             _lineInReader          = 0;
	private Reader          _reader                = null;
	private BufferedReader  _bReader               = null;


	public AseSqlScriptReader()
	{
	}

	public AseSqlScriptReader(String goSql)
	{
		setSqlCommand(goSql);
	}

	public AseSqlScriptReader(File file) 
	throws FileNotFoundException
	{
		setFile(file);
	}

	public AseSqlScriptReader(String goSql, boolean execWithoutGoAtTheEnd)
	{
		this(goSql, execWithoutGoAtTheEnd, null);
	}

	public AseSqlScriptReader(String goSql, boolean execWithoutGoAtTheEnd, String sqlBatchTerminator)
	{
		setGoTerminator(sqlBatchTerminator);
		setSqlCommand(goSql);
		setExecWithoutGoAtTheEnd(execWithoutGoAtTheEnd);
	}

	public AseSqlScriptReader(File file, boolean execWithoutGoAtTheEnd) 
	throws FileNotFoundException
	{
		setFile(file);
		setExecWithoutGoAtTheEnd(execWithoutGoAtTheEnd);
	}

//	public AseSqlScriptReader(Reader reader, boolean execWithoutGoAtTheEnd)
//	{
//		_reader = reader;
//		setExecWithoutGoAtTheEnd(execWithoutGoAtTheEnd);
//
//		if (reader instanceof FileReader)
//			_isFileReader = true;
//		if (reader instanceof StringReader)
//			_isStringReader = true;
//	}

	
	/**
	 * A 'go' terminated string.
	 * @param sql
	 */
	public void setSqlCommand(String goSql)
	{
		_sqlStr = goSql;
		_reader = new StringReader(goSql);
		_bReader = new BufferedReader(_reader);
	}

	/** A 'go' terminated file. */
	public void setFile(String filename)
	throws FileNotFoundException
	{
		setFile(new File(filename));
	}

	/** A 'go' terminated file. */
	public void setFile(File file)
	throws FileNotFoundException
	{
		_file = null;
		if (file.exists())
			new FileNotFoundException("The input file '"+file.toString()+"' doesn't exists.");
		_file = file;

		_reader = new FileReader(_file);
		_bReader = new BufferedReader(_reader);
	}

	/**
	 * If the string or last part isn't 'go' terminated, execute it anyway.
	 * @param sql
	 */
	public void setExecWithoutGoAtTheEnd(boolean execWithoutGoAtTheEnd)
	{
		_execWithoutGoAtTheEnd = execWithoutGoAtTheEnd;
	}

	/**
	 * Set what string to use to "terminate" a SQL batch
	 * @param terminator
	 */
	public void setGoTerminator(String terminator)
	{
		if (StringUtil.isNullOrBlank(terminator))
			throw new RuntimeException("Terminator string can't be null or empty.");

		_goTerminator = terminator;
	}

	/**
	 * @return what is used to as termination string
	 */
	public String getGoTerminator()
	{
		return _goTerminator;
	}
	
	/**
	 * If the string or last part isn't 'go' terminated, execute it anyway.
	 * @param sql
	 */
	public void setSemiColonHack(boolean useSemiColonHack)
	{
		_useSemiColonHack = useSemiColonHack;

		if (_useSemiColonHack)
			_semiColonHelper = new SemiColonHelper();
		else
			_semiColonHelper = null;
	}

	/**
	 * Also use the specified string/character for executing a SQL Batch
	 * 
	 * @param alternativeGoTerminator The string to use, null resets the alternative terminator
	 */
	public void setAlternativeGoTerminator(String alternativeGoTerminator)
	{
		_alternativeGoTerminator = alternativeGoTerminator;
	}

	/**
	 * Check what char/string we are using as alternative for executing a SQL Batch
	 * 
	 * @returns alternativeGoTerminator The string to use, null if not been set.
	 */
	public String getAlternativeGoTerminator()
	{
		return _alternativeGoTerminator;
	}

	/**
	 * Check if the row is the terminator<br>
	 * 
	 * @param row input string
	 * @param terminator String to use as terminator (note: this is no-case)
	 * @return true if it's the terminator string
	 */
	public static boolean isTerminator(String row, String terminator)
	{
		return isTerminator(row, terminator, null);
	}
	/**
	 * Check if the row is the terminator
	 * 
	 * @param row input string
	 * @param terminator String to use as terminator (note: this is no-case)
	 * @param allowExtraChars 
	 * @return true if it's the terminator string
	 */
	public static boolean isTerminator(String row, String terminator, char... allowExtraChars)
	{
		if (row == null)
			return false;

		int rlen = row.length();
		int tlen = terminator.length();

		if (rlen < tlen)
			return false;

		// Check first part of the row (up to terminator length), return false if NOT same char at start
		int rp = 0; // RowPos, 
		int tp = 0; // TerminatorPos
		while (rp<rlen && tp<tlen) 
		{
			char rc = row.charAt(rp++);
			char tc = terminator.charAt(tp++);
			if (rc == tc)
				continue;

			// If characters don't match but case may be ignored,
			// try converting both characters to uppercase.
			// If the results match, then the comparison scan should continue.
			char urc = Character.toUpperCase(rc);
			char utc = Character.toUpperCase(tc);
			if (urc == utc)
				continue;

			// No more match
			return false;
		}

		// so START of the row and terminator seems to be same
		// Now lets see if NEXT character is whitespace or "allowedExtraChars", then it's a terminator
		// otherwise it's a "normal" rows
		if (rp<rlen)
		{
			char rc = row.charAt(rp++);

			if (Character.isWhitespace(rc))
				return true;

			for (int i=0; i<allowExtraChars.length; i++)
				if (allowExtraChars[i] == rc)
					return true;

			// Not whitespace or allowedChar, so NO-MATCH
			return false;
		}

		// If we got this far, it's a terminator string
		return true;

		//--------------------------
		// Below is basic logic that were used earlier
		//--------------------------
		//String goLower          = terminator.toLowerCase();
		//String goLowerWithSpace = terminator.toLowerCase() + " ";
		//String goUpperWithSpace = terminator.toUpperCase() + " ";
		//String goLowerWithPipe  = terminator.toLowerCase() + "|";
		//String goUpperWithPipe  = terminator.toUpperCase() + "|";
		//
		//if (row.equalsIgnoreCase(goLower))    return true;
		//if (row.startsWith(goLowerWithSpace)) return true;
		//if (row.startsWith(goUpperWithSpace)) return true;
		//if (row.startsWith(goLowerWithPipe))  return true;
		//if (row.startsWith(goUpperWithPipe))  return true;
		//		
		//return false;
	}

	/**
	 * Get default Configuration of the goTermination string
	 * @return
	 */
	public static String getConfiguredGoTerminator()
	{
		return Configuration.getCombinedConfiguration().getProperty(AseSqlScriptReader.PROPKEY_sqlBatchTerminator, AseSqlScriptReader.DEFAULT_sqlBatchTerminator);	
	}

	public static boolean hasCommandTerminator(String sqlStr)
	{
		return hasCommandTerminator(sqlStr, false, null, DEFAULT_sqlBatchTerminator);
	}
	public static boolean hasCommandTerminator(String sqlStr, String goTerminator)
	{
		return hasCommandTerminator(sqlStr, false, null, goTerminator);
	}
	public static boolean hasCommandTerminator(String sqlStr, String alternativeGoTerminator, String goTerminator)
	{
		return hasCommandTerminator(sqlStr, false, alternativeGoTerminator, DEFAULT_sqlBatchTerminator);
	}
	public static boolean hasCommandTerminator(String sqlStr, boolean useSemicolonHack, String alternativeGoTerminator, String goTerminator)
	{
		Reader         reader;
		BufferedReader bReader;
		
		if (sqlStr == null)
			throw new IllegalArgumentException("Input string can't be null.");

		if (StringUtil.isNullOrBlank(goTerminator))
			goTerminator = DEFAULT_sqlBatchTerminator;
		
		reader = new StringReader(sqlStr);
		bReader = new BufferedReader(reader);

		boolean hasTerminator = false;

		SemiColonHelper semiColonHelper = null;
		if (useSemicolonHack)
			semiColonHelper = new SemiColonHelper();

		try
		{
			// Get lines from the reader
			boolean hasSemicolon = false;
			String lastRow = "";
			String row;
			for (row = bReader.readLine(); row != null; row = bReader.readLine())
			{
				if (StringUtil.isNullOrBlank(row))
					continue;
				lastRow = row;
				
				if (useSemicolonHack)
				{
//					if (StringUtil.hasSemicolonAtEnd(row))
//						 hasSemicolon = true;

					semiColonHelper.processRow(row);
					if (semiColonHelper.isCompleteStatement())
						 hasSemicolon = true;
				}
					
			}
	
			//---------------------------------
			// Now investigate lastRow
			//---------------------------------
	
			row = lastRow;

			// if end of batch 'go [###]'
//			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
			if ( isTerminator(row, goTerminator, '|'))
			{
				hasTerminator = true;
			}

			// Alternative GO Terminator
			if (alternativeGoTerminator != null)
			{
				if (row.trim().equals(alternativeGoTerminator) )
					hasTerminator = true;
			}

			// semicolon hack enabled
			if (useSemicolonHack && hasSemicolon)
			{
				hasTerminator = true;
			}
	
			// if end of batch 'reset'
//			if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
			if (isTerminator(row, "reset"))
			{
				hasTerminator = true;
			}
			
			// do no more (kind of EOF): 'quit/exit'
//			if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
//			     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
			if (isTerminator(row, "quit") || isTerminator(row, "exit"))
			{
				hasTerminator = true;
			}
		}
		catch (IOException e)
		{
		}
		finally
		{
			try
			{
				bReader.close();
				reader.close();
			}
			catch (IOException ignore) {}
		}


		return hasTerminator;
	}

	/**
	 * Is there any 'go append' in the script
	 * @return true if it contains 'go append'
	 */
	public boolean isGoAppendInText()
	{
		if (_sqlStr == null)
			return false;

		Pattern goAppend = Pattern.compile("^go .*\\bappend\\b.*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		return goAppend.matcher(_sqlStr).find();

//		return _sqlStr.matches("^go .*\\bappend\\b.*$");  // case sensitive
//		return _sqlStr.matches("(?i:^go .*\\bappend\\b.*$)");  // case in-sensitive
	}

	/**
	 * Get number of batches in this Reader
	 * <p>
	 * This opens the input reader and reads thru the input.<br>
	 * So it consumes nearly as much resources as <code>getSqlBatchString()</code> the first time it's called.
	 * Consequent calls will reuse the value from first execution.
	 * <p>
	 * <code>go 10</code><br>
	 * Multiple execution directions will just count as 1
	 * 
	 * @return Number of SQL Batches in the input. 0 = no batch found, 1 = One batch found
	 */
	public int getSqlTotalBatchCount()
	throws IOException
	{
		if (_totalBatchCount < 0)
		{
			int            totalBatchCount = 0;
			int            rowsInLastBatch = 0;
			Reader         reader;
			BufferedReader bReader;
			
			if (_sqlStr != null)
				reader = new StringReader(_sqlStr);
			else if (_file != null)
				reader = new FileReader(_file);
			else
				throw new IllegalArgumentException("No input type has been assigned.");
			
			bReader = new BufferedReader(reader);

			// Get lines from the reader
			String row;
			for (row = bReader.readLine(); row != null; row = bReader.readLine())
			{
				// if end of batch 'go [###]'
//				if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") )
//				if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
				if (isTerminator(row, getGoTerminator(), '|') )
				{
					totalBatchCount++;
					rowsInLastBatch = 0;
				}

				// Alternative GO Terminator
				if (_alternativeGoTerminator != null)
				{
					if (row.trim().equals(_alternativeGoTerminator) )
					{
						totalBatchCount++;
						rowsInLastBatch = 0;
					}
				}

				// if we use the "semicolon hack"
				if (_useSemiColonHack)
				{
					_semiColonHelper.processRow(row);
					if (_semiColonHelper.isCompleteStatement())
					{
						totalBatchCount++;
						rowsInLastBatch = 0;
					}
//					if (StringUtil.hasSemicolonAtEnd(row))
//					{
//						totalBatchCount++;
//						rowsInLastBatch = 0;
//					}
				}

				// if end of batch 'reset'
//				if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
				if (isTerminator(row, "reset"))
				{
					rowsInLastBatch = 0;
					continue;
				}
				
				// do no more (kind of EOF): 'quit/exit'
//				if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
//				     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
				if (isTerminator(row, "quit") || isTerminator(row, "exit"))
				{
					rowsInLastBatch = 0;
					break;
				}

				rowsInLastBatch++;
			}
			if ( _execWithoutGoAtTheEnd && rowsInLastBatch > 0)
				totalBatchCount++;

			bReader.close();
			reader.close();

			_totalBatchCount = totalBatchCount;
		}
		return _totalBatchCount;
	}

	/** 
	 * @return start line number for current SQL batch.
	 *         If you receive a SQLException you can refer to Line in the message and then 
	 *         add <code>getStartLineForCurrentBatch()</code> to get line number in the file. 
	 */
	public int getSqlBatchStartLine()
	{
		return _batchStartLine;
	}

	/**
	 * @return Current batch number, First batch is number 0
	 */
	public int getSqlBatchNumber()
	{
		return _batchNumber;
	}

	/**
	 * When we have a 'go 10', we need to exec this 10 times.<br>
	 * So this just return how many times we are supposed to execute the SQL statement.<br>
	 * If the 'go' terminator doesn't have a count, then 1 will be returned.
	 * 
	 * @return 
	 */
	public int getMultiExecCount()
	{
		return _multiExecCount;
	}

	/**
	 * When we have a 'go 10 wait 600', we need to exec this 10 times and wait 600ms after each execution.<br>
	 * So this just return the wait period in milliseconds between each SQL Execution<br>
	 * If the 'go' terminator doesn't have wait option, then 0 will be returned.
	 * 
	 * @return 
	 */
	public int getMultiExecWait()
	{
		return _multiExecWait;
	}


	public boolean hasOption_topRows()           { return _topRows           > 0; }
	public boolean hasOption_bottomRows()        { return _bottomRows        > 0; }
	public boolean hasOption_rowCount()          { return _rowCount          > 0; }
	public boolean hasOption_asPlaintText()      { return _asPlainText       > 0; }
	public boolean hasOption_noData()            { return _noData            > 0; }
	public boolean hasOption_appendOutput()      { return _appendOutput      > 0; }
	public boolean hasOption_printSql()          { return _printSql          > 0; }
	public boolean hasOption_printRsi()          { return _printRsi          > 0; }
	public boolean hasOption_printClientTiming() { return _printClientTiming > 0; }

	// Below boolean methods, yes we use "int opt = -1" as "not specified"
	public int     getOption_topRows()           { return _topRows; }
	public int     getOption_bottomRows()        { return _bottomRows; }
	public boolean getOption_rowCount()          { return _rowCount          > 0; }
	public boolean getOption_asPlaintText()      { return _asPlainText       > 0; }
	public boolean getOption_noData()            { return _noData            > 0; }
	public boolean getOption_appendOutput()      { return _appendOutput      > 0; }
	public boolean getOption_printSql()          { return _printSql          > 0; }
	public boolean getOption_printRsi()          { return _printRsi          > 0; }
	public boolean getOption_printClientTiming() { return _printClientTiming > 0; }

	/**
	 * When we have a 'go | someSubCommand', we needs to apply some filter.
	 * 
	 * @return the pipe command, meaning everything after the '|' character (with a trim() applied on it)
	 */
	public PipeCommand getPipeCmd()
	{
		return _pipeCommand;
	}

	/**
	 * @return SQL Commands in Current batch, no more batches returns null
	 */
	public String getSqlBatchString()
	throws IOException, PipeCommandException, GoSyntaxException
	{
		_multiExecCount = 1;
		_pipeCommand    = null;
		_batchNumber++;
		_batchStartLine = _lineInReader;

		if (_useSemiColonHack)
			_semiColonHelper.reset();

		StringBuilder batchBuffer = new StringBuilder();

		// Reset some stuff (like options)
		_topRows           = -1;
		_bottomRows        = -1;
		_rowCount          = -1;
		_asPlainText       = -1;
		_noData            = -1;
		_appendOutput      = -1;
		_printSql          = -1;
		_printRsi          = -1;
		_printClientTiming = -1;
		
		// Get lines from the reader
		String row;
		for (row = _bReader.readLine(); row != null; row = _bReader.readLine())
		{
			_lineInReader++;

			//-------------------------------------------------------------------------------------
			// If the "semicolon hack" is enabled it should really be capable of "emulating" Oracle SQL*Plus
			// which means that if a "create " keyword shows up, then semicolon no longer means "send"
			// the you should wait for "end of batch" with 'go' or '/' before the SQL is sent to server
			// The SQL*Plus manual says this: http://docs.oracle.com/cd/B19306_01/server.102/b14357/ch4.htm
			// 
			// Running PL/SQL Blocks
			// You can also use PL/SQL subprograms (called blocks) to manipulate data in the database. 
			// SQL*Plus treats PL/SQL subprograms in the same manner as SQL commands, except that a 
			// semicolon (;) or a blank line does not terminate and execute a block. 
			// Terminate PL/SQL subprograms by entering a period (.) by itself on a new line. 
			// You can also terminate and execute a PL/SQL subprogram by entering a slash (/) by itself on a new line.
			// 
			// You enter the mode for entering PL/SQL statements when:
			// * You type DECLARE or BEGIN. After you enter PL/SQL mode in this way, type the remainder of your PL/SQL subprogram.
			// * You type a SQL command (such as CREATE PROCEDURE) that creates a stored procedure. 
			//   After you enter PL/SQL mode in this way, type the stored procedure you want to create.
			// 
			// SQL*Plus stores the subprograms you enter in the SQL buffer. Execute the current subprogram with a RUN or slash (/) command. 
			// A semicolon (;) is treated as part of the PL/SQL subprogram and will not execute the command.
			//
			// You might enter and execute a PL/SQL subprogram as follows:
			// declare a varchar2(8);
			// begin
			//    a := rawtohex('AB');
			//    dbms_output.put_line(a);
			//    select RAWTOHEX('AB') into a from dual;
			//    dbms_output.put_line(a);
			// end;
			//
			// Creating Stored Procedures
			// Stored procedures are PL/SQL functions, packages, or procedures. 
			// To create stored procedures, you use the following SQL CREATE commands:
			// 	  * CREATE FUNCTION
			// 	  * CREATE LIBRARY
			// 	  * CREATE PACKAGE
			// 	  * CREATE PACKAGE BODY
			// 	  * CREATE PROCEDURE
			// 	  * CREATE TRIGGER
			// 	  * CREATE TYPE
			// Entering any of these commands places you in PL/SQL mode, where you can enter your PL/SQL subprogram.
			//-------------------------------------------------------------------------------------
			//
			// So what I will try to do is:
			// "Strip of" all comments... start to look for the first word...
			// if first word is 'declare'|'begin'|'create' then *disable* "send on semicolon"

			// if we use the "semicolon hack"
			if (_useSemiColonHack)
			{
				_semiColonHelper.processRow(row);
				if (_semiColonHelper.isCompleteStatement())
				{
					batchBuffer.append(_semiColonHelper.getText());
					return batchBuffer.toString();
				}
//				if (StringUtil.hasSemicolonAtEnd(row))
//				{
//					batchBuffer.append(StringUtil.removeSemicolonAtEnd(row));
//					return batchBuffer.toString();
//				}
			}

			// Alternative GO Terminator
			if (_alternativeGoTerminator != null)
			{
				if (row.trim().equals(_alternativeGoTerminator) )
				{
					return batchBuffer.toString();
				}
			}

			// if end of batch 'go [###]'
//			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") )
//			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
//			if (row.equalsIgnoreCase(_goLower) || row.startsWith(_goLowerWithSpace) || row.startsWith(_goUpperWithSpace) || row.startsWith(_goLowerWithPipe) || row.startsWith(_goUpperWithPipe) )
			if (isTerminator(row, getGoTerminator(), '|'))
			{
				// Format of the 'go' could be: 'go[ # [wait ###][plain]][|pipeCmd]' or in other words
				// go
				// go plain
				// go 10
				// go 10 plain
				// go 10 wait 1000
				// go 10 top 100
				// go 10 bottom 100
				// go 10|pipeCmd
				// go 10 |pipeCmd
				// go 10 | pipeCmd
				// go|pipeCmd
				// go |pipeCmd
				// go | pipeCmd
//				if (row.length() > 3)
				if (row.length() > getGoTerminator().length()+1)
				{
					int pipePos = row.indexOf('|');
					String goCmdStr  = null;
					String goPipeStr = null;

					if (pipePos > -1)
					{
						goCmdStr  = row.substring(getGoTerminator().length(), pipePos).trim();
						goPipeStr = row.substring(pipePos + 1).trim();;
					}
					else
					{
						goCmdStr = row.substring(getGoTerminator().length()).trim();
					}

					// Get go ## [wait ###]
					// or simple: "parse" everything *after* the terminator string 'go'
					if ( ! StringUtil.isNullOrBlank(goCmdStr) )
					{
						_multiExecCount = 1;
						String goExecCount     = "1";
						String originGoCmdStr = goCmdStr;

						// goExecCount
						if (goCmdStr.indexOf(" ") >= 0 || goCmdStr.length() > 0)
						{
							int endPos = goCmdStr.indexOf(" ") >= 0 ? goCmdStr.indexOf(" ") : goCmdStr.length();

							goExecCount = goCmdStr.substring(0, endPos).trim();
							goCmdStr    = goCmdStr.substring(   endPos).trim();

							// get how many executions
							// if not a number, restore some stuff
							try 
							{ 
								_multiExecCount = Integer.parseInt( goExecCount ); 
							}
							catch (NumberFormatException ignore) 
							{
								goCmdStr = originGoCmdStr;
							}
						}

						// If something is left here, it must be sub commands
						// so split them on ',' and loop them all
						if (goCmdStr.length() > 0)
						{
							String[] goSubCmds = goCmdStr.split(",");
							for (String subCmd : goSubCmds)
							{
								subCmd = subCmd.trim();
								String word1 = StringUtil.word(subCmd, 0);
								String word2 = StringUtil.word(subCmd, 1);
								String word3 = StringUtil.word(subCmd, 2);

								// get wait/sleep time
								// or any options after: go [#] options
								if (StringUtil.hasValue(word1))
								{
									String error = null;

									if ("wait".equalsIgnoreCase(word1))
									{
										try { _multiExecWait = Integer.parseInt( word2 ); }
										catch (NumberFormatException nfe) 
										{
											error = "Sub command 'wait #' The parameter '"+word2+"' is not a number.";
										}
									}
									else if ("top".equalsIgnoreCase(word1))
									{
										try { _topRows = Integer.parseInt( word2 ); }
										catch (NumberFormatException nfe) 
										{
											error = "Sub command 'top #' The parameter '"+word2+"' is not a number.";
										}
									}
									else if ("bottom".equalsIgnoreCase(word1))
									{
										try { _bottomRows = Integer.parseInt( word2 ); }
										catch (NumberFormatException nfe) 
										{
											error = "Sub command 'bottom #' The parameter '"+word2+"' is not a number.";
										}
									}
									else if ("rowc".equalsIgnoreCase(word1))
									{
										_rowCount = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("plain".equalsIgnoreCase(word1))
									{
										_asPlainText = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("nodata".equalsIgnoreCase(word1))
									{
										_noData = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("append".equalsIgnoreCase(word1))
									{
										_appendOutput = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("psql".equalsIgnoreCase(word1))
									{
										_printSql = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("prsi".equalsIgnoreCase(word1))
									{
										_printRsi = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else if ("time".equalsIgnoreCase(word1))
									{
										_printClientTiming = 1;
										if (StringUtil.hasValue(word2))
											error = "Sub command '"+word1+"' does not accept any parameters.\nYou passed the parameter '"+word2+"'.";
									}
									else
									{
										error = "Unknown sub command '"+word1+"'.";
									}

									// If we have "spill" in the sub command
									// probably that next sub command wasn't comma(,) separated
									if (StringUtil.hasValue(word3))
									{
										error = "Have you forgot to comma separate different sub commands?.\nCurrent sub command looks like '"+subCmd+"'.";
									}
									
									if (error != null)
									{
										String desc = 
											error +" \n" +
											"\n" +
											"Syntax is 'go [#1] [,top #2] [,bottom #3] [,wait #4] [,plain] [,nodata] [,append] [,psql] [,prsi] [,time]'\n" +
											"\n" +
											"#1 = Number of times to repeat the command\n" +
											"#2 = Rows to read from a ResultSet.\n" +
											"#3 = Last rows to display from a ResultSet.\n" +
											"#4 = Ms to sleep after each SQL Batch send/execution.\n" +
											"\n" +
											"Description of sub commands\n" +
											"top #    - Read only first # rows in the result set\n" +
											"bottom # - Only display last # rows in the result set\n" +
											"wait #   - Wait #ms after the SQL Batch has been sent, probably used in conjunction with (multi go) 'go 10'\n" +
											"plain    - Do NOT use a GUI table for result set, instead print as plain text.\n" +
											"nodata   - Do NOT read the result set rows, just read the column headers. just do rs.next(), no rs.getString(col#)\n" +
											"append   - Do NOT clear results from previous executions. Append at the end.\n" +
											"psql     - Print the executed SQL Statement in the output\n" +
											"prsi     - Print info about the ResultSet data types etc in the output\n" +
											"time     - Print how long time the SQL Batch took, from the clients perspective\n" +
											"rowc     - Print the rowcount from JDBC driver, not the number of rows actually returned\n" +
											"\n" +
											"Example:\n" +
											"select * from tabName where ...\n" +
											"go top 100, plain\n" +
											"";
										throw new GoSyntaxException(desc);
									}
								}// end: hasValue(word1)
							} // end: for each sub command
						} // end: any sub commands
					} // end: "parse" everything *after* the 'go' terminator

					// Get go | pipeCmd
					if ( ! StringUtil.isNullOrBlank(goPipeStr) )
					{
						_pipeCommand = new PipeCommand(goPipeStr, batchBuffer.toString());
					}

//					// get how many 
//					try { _multiExecCount = Integer.parseInt( row.substring(3) ); }
//					catch (NumberFormatException ignore) {}

				} // end has 'go' terminator

				return batchBuffer.toString();
			}

			// if end of batch 'reset'
//			if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
			if (isTerminator(row, "reset"))
			{
				// reset the batch
				batchBuffer = new StringBuilder();
				_batchStartLine = _lineInReader;

				continue;
			}
			
			// do no more (kind of EOF): 'quit/exit'
//			if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
//			     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
			if (isTerminator(row, "quit") || isTerminator(row, "exit"))
				return null;

			// Append current SQL to batch buffer
			batchBuffer.append(row).append("\n");
		}
		if ( row == null && batchBuffer.length() == 0)
			return null;

		if ( _execWithoutGoAtTheEnd )
			return batchBuffer.toString();

		return null;
	}

	
	public void close()
	{
		try
		{
			_bReader.close();
			_reader.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void finalize() 
	throws Throwable
	{
		this.close();
		super.finalize();
	}

	/**
	 * Help to decide what's a SQL Statement or not when semicolon are allowed as send-to-server-terminator 
	 * or a SQL-Statement terminator within a SQL Block (stored procedure or similar)
	 * 
	 * @author gorans
	 */
	private static class SemiColonHelper
	{
		private int     _rowNumber          = 0;
		private boolean _inMultiLineComment = false;
		private boolean _inSqlBlock         = false;
		private String  _currentRow         = null;

//		private String  _regex              = "(create|alter)\\s+(procedure|proc|trigger|view|function)";

		private String  _regex1             = "(begin|declare)\\s+";
		private Pattern _pattern1           = Pattern.compile(_regex1, Pattern.CASE_INSENSITIVE);

//		private String  _regex2             = "(create|alter)\\s+(procedure|proc|trigger|view|function)";
		private String  _regex2             = "(create(\\s+or\\s+replace)?|alter)\\s+(procedure|proc|trigger|view|function)";
		private Pattern _pattern2           = Pattern.compile(_regex2, Pattern.CASE_INSENSITIVE);

		public SemiColonHelper()
		{
			reset();
		}

		/**
		 * Resets the helper class so we can read another batch.
		 */
		public void reset()
		{
			_rowNumber          = 0;
			_inMultiLineComment = false;
			_inSqlBlock         = false;
			_currentRow         = null;
		}

		/**
		 * When reading a file (or other input) line by line...<br>
		 * We need to parse the content and:
		 * <ul>
		 *    <li>disregard any leading comment comment</li>
		 *    <li>if <b>first</b> string is 'begin|declare|create', the start a SQL Block that <b>can</b> contain semicolons ';'</li>
		 * </ul>
		 * @param row
		 */
		public void processRow(String inputRow)
		{
			_rowNumber++;
			_currentRow = inputRow;
			
			String row = inputRow.trim();
			//System.out.println(">>>> SemiColonHelper.processRow(): BEGIN: _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
			

			// PARSE
			// Multi line comments, this takes a bit processing
			//------------------------------------------------

			// simple comments: /* some text */
			if (row.startsWith("/*") && row.endsWith("*/"))
			{
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: SINGLE_LINE_COMMENT: _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return; // get next line
			}

			// hmm simple comments but something after comment: /* some text */ XXXX<-This is a Statement
			// Just remove this part from the str, and continue parsing
			if (row.startsWith("/*") && row.indexOf("*/") > 0)
			{
				row = row.substring(row.indexOf("*/")+2).trim();
				//System.out.println("  << SemiColonHelper.processRow():      : SINGLE_LINE_-SPILL-: _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
			}

			// Now try multiLine comment, START
			if (row.startsWith("/*"))
			{
				_inMultiLineComment = true;
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: MLC_START          : _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return; // next row
			}

			// LAST of the row comment END: */
			if (row.endsWith("*/"))
			{
				_inMultiLineComment = false;
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: MLC_-END-          : _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return; // next line
			}

			// Comment ENDS, but not at the end of the row: end of comment */ XXXX<-This is a Statement 
			if (row.indexOf("*/") >= 0)
			{
				_inMultiLineComment = false;
				row = row.substring(row.indexOf("*/")+2).trim();
				//System.out.println("  << SemiColonHelper.processRow():      : MLC_-WITH_SPILL-   : _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
			}
				
			// If we are in Multi Line Comment, just read next row
			if (_inMultiLineComment)
			{
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: AT_MLC             : _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return;
			}

			// Nothing left in the string, get next line
			if (row.equals(""))
			{
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: BLANK_LINE         : _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return;
			}

			// The rest is comment, get next line
			if (row.startsWith("--"))
			{
				//System.out.println("  << SemiColonHelper.processRow(): -RET-: --SINGLE_LINE_COMNT: _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"'.");
				return;
			}

			// _pattern1: has (begin|declare)\\s+
			// _pattern2: has (create|alter)\\s+(procedure|proc|trigger|view|function)
			// Then start a SQL Block
			if ( ! _inSqlBlock && ( _pattern1.matcher(row).find() || _pattern2.matcher(row).find() ) )
			{
				_inSqlBlock = true;
			}
			//System.out.println("  << SemiColonHelper.processRow(): -END-: _rowNumber="+_rowNumber+", _inMultiLineComment="+_inMultiLineComment+", _inSqlBlock="+_inSqlBlock+", row='"+row+"', _currentRow='"+_currentRow+"', getText()='"+getText()+"'.");
		}

		/**
		 * This works on <b>just</b> the latest row sent to processRow()<br>
		 * If we are not in a SQL Block, then the semicolon at the end will be stripped.<br>
		 * But if we are in a SQL Block, the the same row processed with processRow will be returned.
		 * 
		 * @return str
		 */
		public Object getText()
		{
			if (_inSqlBlock)
				return _currentRow;
			else
				return StringUtil.removeSemicolonAtEnd(_currentRow);
		}

		/**
		 * Check if it's time to send the "sql batch" to the server 
		 * @return
		 */
		public boolean isCompleteStatement()
		{
			if ( ! _inSqlBlock && StringUtil.hasSemicolonAtEnd(_currentRow) )
				return true;
			
			return false;
		}		
	}
	
	public static void main(String[] args)
	{
		System.out.println("START");

//		String sql_1 = 
//			"select 1.1 \n" +
//			"select 1.2 \n" +
//			"select 1.3 \n" +
//			"go";

//		String sql_2 = 
//			"select 2.1 \n" +
//			"select 2.2 \n" +
//			"select 2.3";

//		String sql_3 = 
//			"select 3.1 \n" +
//			"go 20";

//		String sql_4 = 
//			"select 3.1 \n" +
//			"go 20 --make this 20 times";
		
		System.out.println("-START-: test case # 5");

		String sql_5 = 
			"select 5.1 \n" +
			"go \n" +
			
			"select 5.2 \n" +
			"Go 100\n" +
			
			"select 5.3.1 \n" +
			"select 5.3.2 \n" +
			"gO\n" +
			
			"select 'disregard this batch' \n" +
			"rEsEt \n" +
			
			"select 5.4 \n" +
			"GO \n" +
			
			"select 5.5 with pipeCmd \n" +
			"go|grep 'some grep str'\n" +
			
			"select 5.6 multiGo and pipeCmd \n" +
			"go 10 | grep 'some_other_grep_str'\n" +
			
			"select 'do not show-exit' \n" +
			"eXit \n" +
			
			"select 'never, we should not reach here' \n" +
			"go \n";
		try
		{
			AseSqlScriptReader sr = new AseSqlScriptReader(sql_5);
			for (String sqlBatch = sr.getSqlBatchString(); sqlBatch != null; sqlBatch = sr.getSqlBatchString() )
			{
				System.out.println();
				System.out.println("#######################################################");
				System.out.println("getSqlTotalBatchCount: " + sr.getSqlTotalBatchCount());
				System.out.println("getSqlBatchNumber    : " + sr.getSqlBatchNumber());
				System.out.println("getSqlBatchStartLine : " + sr.getSqlBatchStartLine());
				System.out.println("getMultiExecCount    : " + sr.getMultiExecCount());
				System.out.println("getPipeCmd           : " + sr.getPipeCmd());
				System.out.println("---- batch-begin --------------------------------------");
				System.out.print(sqlBatch);
				System.out.println("---- batch-end ----------------------------------------");
			}
			sr.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(PipeCommandException e)
		{
			e.printStackTrace();
		}
		catch(GoSyntaxException e)
		{
			e.printStackTrace();
		}

		System.out.println("--END--: test case # 5");
		System.out.println();


		
		System.out.println("-START-: test case # 6");

		String sql_6 = 
			"select 6.0;\n" +
			
			"select 6.1; \n" +
			
			"select 6.2    ;   \n" +
			
			"select 6.3.1 \n" +
			"from 6.3.2; \n" +
			
			"select 'disregard this batch' \n" +
			"reset \n" +
			
			"select 6.4 \n" +
			"go" +
			
			"select 6.5 --- some OK comments ; \n" +
			
			"select 6.6 ; --- some comment, not in batch \n" +
			
			"-- so this should go on the same batch \n" +
			"-- finaly terminator;\n" +
			
			"select 'do not show-exit' \n" +
			"exit \n" +
			
			"select 'never, we should not reach here' \n" +
			"go \n";
		try
		{
			AseSqlScriptReader sr = new AseSqlScriptReader(sql_6);
			sr.setSemiColonHack(true);
			for (String sqlBatch = sr.getSqlBatchString(); sqlBatch != null; sqlBatch = sr.getSqlBatchString() )
			{
				System.out.println();
				System.out.println("#######################################################");
				System.out.println("getSqlTotalBatchCount: " + sr.getSqlTotalBatchCount());
				System.out.println("getSqlBatchNumber    : " + sr.getSqlBatchNumber());
				System.out.println("getSqlBatchStartLine : " + sr.getSqlBatchStartLine());
				System.out.println("getMultiExecCount    : " + sr.getMultiExecCount());
				System.out.println("getPipeCmd           : " + sr.getPipeCmd());
				System.out.println("---- batch-begin --------------------------------------");
				System.out.print(sqlBatch);
				System.out.println("<enter-inserted-here-for-clarity>");
				System.out.println("---- batch-end ----------------------------------------");
			}
			sr.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(PipeCommandException e)
		{
			e.printStackTrace();
		}
		catch(GoSyntaxException e)
		{
			e.printStackTrace();
		}

		System.out.println("--END--: test case # 6");
		System.out.println();

		
		System.out.println("-START-: test case # 7");

		String sql_7 = 
			"\n" +
			"------\n" +
			"select 7.1; \n" +
			"select 7.2; \n" +
			
			"\n" +
			"/* single line comment */\n" +
			"select 7.2    ;   \n" +
			
			"/* \n" +
			"** multi line comment \n" +
			"*/ \n" +
			"select 7.3.1 \n" +
			"from 7.3.2; \n" +
			
			"select 'disregard this batch' \n" +
			"reset \n" +
			
			"/* \n" +
			"** multi line comment, start right after comment \n" +
			"*/create proc xxx_7.4 \n" +
			"as \n" +
			"begin \n" +
			"    select 7.4.1 from dual;\n" +
			"    select 7.4.2 from dual;\n" +
			"    select 7.4.3 from dual;\n" +
			"    select 7.4.4 from dual;\n" +
			"end; \n" +
			"go \n" +
			
			"\n" +
			"/* \n" +
			"** multi line comment, start right after comment \n" +
			"*/ \n" +
			"create procedure xxxx \n" +
			"as \n" +
			"begin \n" +
			"    select 7.5 from dual;\n" +
			"end; \n" +
			"go \n" +

			"\n" +
			"--- LAST LINE\n" +
			"select 7.6;\n" +
			
			"\n";
		try
		{
			AseSqlScriptReader sr = new AseSqlScriptReader(sql_7);
			sr.setSemiColonHack(true);
			for (String sqlBatch = sr.getSqlBatchString(); sqlBatch != null; sqlBatch = sr.getSqlBatchString() )
			{
				System.out.println();
				System.out.println("#######################################################");
				System.out.println("getSqlTotalBatchCount: " + sr.getSqlTotalBatchCount());
				System.out.println("getSqlBatchNumber    : " + sr.getSqlBatchNumber());
				System.out.println("getSqlBatchStartLine : " + sr.getSqlBatchStartLine());
				System.out.println("getMultiExecCount    : " + sr.getMultiExecCount());
				System.out.println("getPipeCmd           : " + sr.getPipeCmd());
				System.out.println("---- batch-begin --------------------------------------");
				System.out.print(sqlBatch);
				System.out.println("<enter-inserted-here-for-clarity>");
				System.out.println("---- batch-end ----------------------------------------");
			}
			sr.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(PipeCommandException e)
		{
			e.printStackTrace();
		}
		catch(GoSyntaxException e)
		{
			e.printStackTrace();
		}

		System.out.println("--END--: test case # 7");
		System.out.println();

		
		System.out.println("STOP");
	}
}
