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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class Pipe extends Thread
{
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	protected boolean      _done      = false;
	protected Object       _semaphore = new Object();
	protected InputStream  _input;
	protected OutputStream _output;


	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public Pipe(InputStream in, OutputStream out)
	{
		_input = in;
		_output = out;
		start();
	}



	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	/**
	 * wait for all bytes to be read from the input stream on written to the _output stream
	 */
	public void waitFor()
	{
		synchronized (_semaphore)
		{
			if (_done)
				return;
			try
			{
				_semaphore.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 */
	@Override
	public void run()
	{
		try
		{
			int ch;
			while ((ch = _input.read()) != -1)
			{
				_output.write(ch);
			}
		}
		catch (IOException e)
		{
			// Do nothing, just let the thread finish
		}
		finally
		{
			done();
		}
	}

	/**
	 *
	 */
	private void done()
	{
		synchronized (_semaphore)
		{
			_done = true;
			_semaphore.notifyAll();
		}
	}

}
