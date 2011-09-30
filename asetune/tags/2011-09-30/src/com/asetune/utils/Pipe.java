package com.asetune.utils;


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