package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.asetune.sql.pipe.PipeCommand;
import com.asetune.sql.pipe.PipeCommandException;

/**
 * Read a file or a String with 'go' terminations.<br>
 * Just like isql would do it.
 * <p>
 * 'isql' commands that are implemented
 * <ul>
 *     <li>go [count] - execute the above SQL Statement(s), <code>[count]</code> is how many times the statements should be executed.</li>
 *     <li>exit       - do not continue</li>
 *     <li>quit       - do not continue</li>
 *     <li>reset      - skip above statements and continue with next batch</li>
 * </ul>
 * 'isql' commands that are <b>NOT</b> implemented
 * <ul>
 *     <li>!! command - OS Commands are not supported.</li>
 *     <li>vi         - Editing the batch is not supported</li>
 * </ul>
 * 
 * @author gorans
 */
public class AseSqlScriptReader
{
	/** sql in case of String input */
	private String         _sqlStr                = null;

	/** The file in case of a File input */
	private File           _file                  = null;

	/** read sql after last 'go' executor */
	private boolean        _execWithoutGoAtTheEnd = false;

	private int            _multiExecCount        = 0;
	private PipeCommand    _pipeCommand           = null;
	private int            _batchStartLine        = -1;
	private int            _batchNumber           = -1;
	private int            _totalBatchCount       = -1;

	/** keep track of where in the file we are */
	private int            _lineInReader          = 0;
	private Reader         _reader                = null;
	private BufferedReader _bReader               = null;


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

	public static boolean hasCommandTerminator(String sqlStr)
	{
		Reader         reader;
		BufferedReader bReader;
		
		if (sqlStr == null)
			throw new IllegalArgumentException("Input string can't be null.");

		reader = new StringReader(sqlStr);
		bReader = new BufferedReader(reader);

		boolean hasTerminator = false;

		try
		{
			// Get lines from the reader
			String lastRow = "";
			String row;
			for (row = bReader.readLine(); row != null; row = bReader.readLine())
			{
				if (StringUtil.isNullOrBlank(row))
					continue;
				lastRow = row;
			}
	
			//---------------------------------
			// Now investigate lastRow
			//---------------------------------
	
			row = lastRow;

			// if end of batch 'go [###]'
			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
			{
				hasTerminator = true;
			}
	
			// if end of batch 'reset'
			if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
			{
				hasTerminator = true;
			}
			
			// do no more (kind of EOF): 'quit/exit'
			if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
			     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
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
				if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
				{
					totalBatchCount++;
					rowsInLastBatch = 0;
				}

				// if end of batch 'reset'
				if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
				{
					rowsInLastBatch = 0;
					continue;
				}
				
				// do no more (kind of EOF): 'quit/exit'
				if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
				     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
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
	throws IOException, PipeCommandException
	{
		_multiExecCount = 1;
		_pipeCommand    = null;
		_batchNumber++;
		_batchStartLine = _lineInReader;

		StringBuilder batchBuffer = new StringBuilder();

		// Get lines from the reader
		String row;
		for (row = _bReader.readLine(); row != null; row = _bReader.readLine())
		{
			_lineInReader++;

			// if end of batch 'go [###]'
//			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") )
			if (row.equalsIgnoreCase("go") || row.startsWith("go ") || row.startsWith("GO ") || row.startsWith("go|") || row.startsWith("GO|") )
			{
				// Format of the 'go' could be: 'go[ #][|pipeCmd]' or in other words
				// go
				// go 10
				// go 10|pipeCmd
				// go 10 |pipeCmd
				// go 10 | pipeCmd
				// go|pipeCmd
				// go |pipeCmd
				// go | pipeCmd
				if (row.length() > 3)
				{
					int pipePos = row.indexOf('|');
					String goNumberStr = null;
					String goPipeStr   = null;
					
					if (pipePos > -1)
					{
						goNumberStr = row.substring(2, pipePos).trim();
						goPipeStr   = row.substring(pipePos + 1).trim();;
					}
					else
					{
						goNumberStr = row.substring(2).trim();
					}

					// Get go ## 
					if ( ! StringUtil.isNullOrBlank(goNumberStr) )
					{
						// get how many 
						try { _multiExecCount = Integer.parseInt( goNumberStr ); }
						catch (NumberFormatException ignore) {}
					}

					// Get go | pipeCmd
					if ( ! StringUtil.isNullOrBlank(goPipeStr) )
					{
						_pipeCommand = new PipeCommand(goPipeStr);
					}

//					// get how many 
//					try { _multiExecCount = Integer.parseInt( row.substring(3) ); }
//					catch (NumberFormatException ignore) {}
				}

				return batchBuffer.toString();
			}

			// if end of batch 'reset'
			if (row.equalsIgnoreCase("reset") || row.startsWith("reset ") || row.startsWith("RESET "))
			{
				// reset the batch
				batchBuffer = new StringBuilder();
				_batchStartLine = _lineInReader;

				continue;
			}
			
			// do no more (kind of EOF): 'quit/exit'
			if (    row.equalsIgnoreCase("quit") || row.startsWith("quit ") || row.startsWith("QUIT ") 
			     || row.equalsIgnoreCase("exit") || row.startsWith("exit ") || row.startsWith("EXIT ") )
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
		
		String sql_5 = 
			"select 5.1 \n" +
			"go \n" +
			
			"select 5.2 \n" +
			"go 100\n" +
			
			"select 5.3.1 \n" +
			"select 5.3.2 \n" +
			"go\n" +
			
			"select 'disregard this batch' \n" +
			"reset \n" +
			
			"select 5.4 \n" +
			"go \n" +
			
			"select 5.5 with pipeCmd \n" +
			"go|grep 'some grep str'\n" +
			
			"select 5.6 multiGo and pipeCmd \n" +
			"go 10 | grep 'some_other_grep_str'\n" +
			
			"select 'do not show-exit' \n" +
			"exit \n" +
			
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
		System.out.println("STOP");
	}
}
