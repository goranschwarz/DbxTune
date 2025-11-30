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
package com.dbxtune.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class OSCommand
extends Object
{
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	protected Process               _process = null;
	protected String                _command;
	protected InputStream           _input;
	protected ByteArrayOutputStream _stdOutput = new ByteArrayOutputStream();
	protected ByteArrayOutputStream _errOutput = new ByteArrayOutputStream();
	protected Pipe                  _stdOutputPipe = null;
	protected Pipe                  _errOutputPipe = null;;


	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/

	/**
	 * Creates new OSCommand.
	 * This will not execute the command @param command string representation of the command
	 */
	public OSCommand(String s)
	{
		_command = s;
	}



	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	/**
	 *
	 */
	public static OSCommand execute(String cmd)
	throws IOException
	{
		OSCommand oscmd = new OSCommand(cmd);
		oscmd.execute();
		return oscmd;
	}

	/**
	 *
	 */
	public void execute()
	throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		_process = rt.exec(_command);

		if (_input != null)
		{
			new Pipe(_input, _process.getOutputStream());
		}

		_stdOutputPipe = new Pipe(_process.getInputStream(), _stdOutput);
		_errOutputPipe = new Pipe(_process.getErrorStream(), _errOutput);
	}

	/**
	 *
	 */
	public void setInput(InputStream is)
	{
		_input = is;
	}

	/**
	 *
	 */
	public void setInput(String s)
	{
		setInput(new ByteArrayInputStream(s.getBytes()));
	}

	/**
	 * Return the error output of the command
	 * @return String
	 */
	public String getErrorOutput()
	{
		_errOutputPipe.waitFor();
		return _errOutput.toString();
	}

	/**
	 * return the standard output from the command
	 * @return String
	 */
	public String getStdOutput()
	{
		_stdOutputPipe.waitFor();
		return _stdOutput.toString();
	}

	/**
	 * return the error and standard output from the command
	 * @return String
	 */
	public String getOutput()
	{
		String err = getErrorOutput();
		String std = getStdOutput();

		if (err.length() == 0)
			return std;

		if (std.length() == 0)
			return err;

		return err + "\n" + std;
	}

	/**
	 * @return  int
	 */
	public int returnCode()
	{
		try
		{
			_process.waitFor();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		return _process.exitValue();
	}


	/**
	 * @return  int
	 */
	public void close()
	throws IOException
	{
//		_process.destroy();

//		if (_input != null)
//		{
//			_input.close();
//			_input = null;
//		}
//
//		if (_stdOutput != null)
//		{
//			_stdOutput.close();
//			_stdOutput = null;
//		}
//
//		if (_errOutput != null)
//		{
//			_errOutput.close();
//			_errOutput = null;
//		}
//
//		if (_stdOutputPipe != null)
//		{
//			_stdOutputPipe.done();
//			_stdOutputPipe = null;
//		}
//		if (_errOutputPipe != null)
//		{
//			_errOutputPipe.done();
//			_errOutputPipe = null;
//		}

		// Close the associated filedescriptors with the Runtime Process
		_process.getInputStream().close();
		_process.getOutputStream().close();
		_process.getErrorStream().close();

	}


//	/* (non-Javadoc)
//	 * @see java.lang.Object#finalize()
//	 */
//	@Override
//	protected void finalize()
//	throws Throwable
//	{
//		close();
//	}


	/*
	 * *********************************************************************
	 * ********* TEST CODE ********* TEST CODE ********* TEST CODE *********
	 * *********************************************************************
	 */

	/**
	 * main
	 */
	public static void main (String args[])
	{
		if (args.length == 0)
		{
			System.out.println( "Usage: OSCommand command1 command2 ..." );
		}

		if (args[0].equals("test1"))
		{
			int loop = Integer.parseInt(args[1]);

			for (int i=0; i<loop; i++)
			{
				try
				{
					String cmd = "echo 'loop is "+i+"'";

					System.out.println( "Command to execute: "+cmd );
					OSCommand oscmd = new OSCommand(cmd);
					oscmd.execute();
					System.out.println( "Return code: "+oscmd.returnCode() );
					System.out.println( "Output from the command:");
					System.out.println( oscmd.getOutput() );
					oscmd.close();
					oscmd = null;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					break;
				}
			}
		}
		else
		{
			for (int i=0; i<args.length; i++)
			{
				try
				{
					System.out.println( "Command to execute: '"+args[i]+"'" );
					OSCommand oscmd = new OSCommand(args[i]);
					oscmd.execute();
					System.out.println( "Return code: "+oscmd.returnCode() );
					System.out.println( "Output from the command:");
					System.out.println( oscmd.getOutput() );
					oscmd.close();
					oscmd = null;

					//OSCommand.execute(args[i]);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

		}


	} //END: main

} //END: OSCommand
