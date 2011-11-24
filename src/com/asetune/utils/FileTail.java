package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Session;

import com.asetune.hostmon.SshConnection;

/**
 * Class that does "tail" on any file
 * <p>
 * The file can be local, or remote<br>
 * Remote files are supported/backed by a SSH connection
 * <p>
 * Any listeners has to implement <code>FileTail.TraceListener</code> which is called for every row.
 * 
 * @author gorans
 *
 */
public class FileTail 
{
	private static Logger _logger = Logger.getLogger(FileTail.class);

	/** Called for every new row appended to the file */
	public interface TraceListener
	{
		/**
		 * A new line has been added to the tailed log file
		 * @param row The new line that has been added to the tailed log file
		 */
		public void newTraceRow(String row);
	}

	private enum TailType {LOCAL, SSH};
	
	private SshConnection _sshConn = null;

	private TailType _execMode = null;
	private Thread _thread = null;

	/** How frequently to check for file changes */
	private long _sleepTime = 250;

	private String _filename = null;
	/** The log file to tail */
	private File _localFile = null;

	/**
	 * Defines whether the log file tailer should include the entire contents of
	 * the existing log file or tail from the end of the file when the tailer starts
	 */
	private boolean _startAtBeginning = false;

	/** Is the tailer currently tailing? */
	private boolean _running = false;

	/** Listeners */
	private Set<TraceListener> _listeners = new LinkedHashSet<TraceListener>();

	/**
	 */
	public FileTail(String filename)
	{
		_filename = filename;
		_localFile = new File(filename);
		
		_execMode = TailType.LOCAL;
	}

	/**
	 * Creates a new log file tailer
	 *
	 * @param file 
	 *            The file to tail
	 * @param refreshInterval
	 *            How often to check for updates to the log file (default = 1000ms)
	 * @param startAtBeginning
	 *            Should the tailer simply tail or should it process the entire
	 *            file and continue tailing (true) or simply start tailing from
	 *            the end of the file
	 */
	public FileTail(String filename, boolean startAtBeginning)
	{
		_filename         = filename;
		_localFile        = new File(filename);

		_startAtBeginning = startAtBeginning;

		_execMode = TailType.LOCAL;
	}

	/**
	 * Do remote "tail"
	 * @param sshConn A SshConnection already connected to the backend
	 * @param filename Name of the file
	 * @param startAtBeginning start from "top" of the file
	 */
	public FileTail(SshConnection sshConn, String filename, boolean startAtBeginning)
	{
		_sshConn          = sshConn;
		_filename         = filename;
		_startAtBeginning = startAtBeginning;

		_execMode = TailType.SSH;
	}

	public void setSleepTime(long sleepTime)
	{
		_sleepTime = sleepTime;
	}
	public long getSleepTime()
	{
		return _sleepTime;
	}


	public void addTraceListener(TraceListener l)
	{
		_listeners.add(l);
	}

	public void removeTraceListener(TraceListener l)
	{
		_listeners.remove(l);
	}

	protected void fireNewTraceRow(String row)
	{
		for (TraceListener tl : _listeners)
			tl.newTraceRow(row);
	}

	public String getFilename()
	{
		return _filename;
	}

	/**
	 * Check if the file exists, which probably should be done before starting the tail...
	 * @return
	 */
	public boolean doFileExist()
	{
		if (_execMode == TailType.LOCAL)
		{
			if (_localFile == null)
				return false;

			return _localFile.exists();
		}
		else if (_execMode == TailType.SSH)
		{
			return _sshConn.doFileExist(_filename);
		}
		else
		{
			throw new RuntimeException("Unknown execution type '"+_execMode+"'.");
		}
	}

	/**
	 * Remove the tail file
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean removeFile()
	throws Exception
	{
		if (_execMode == TailType.LOCAL)
		{
			if (_localFile == null)
				return false;

			return _localFile.delete();
		}
		else if (_execMode == TailType.SSH)
		{
			if (_sshConn == null)
				return false;

			if (_sshConn.isClosed())
				return false;

			return _sshConn.removeFile(_filename);
		}
		else
		{
			throw new RuntimeException("Unknown execution type '"+_execMode+"'.");
		}
	}

	/**
	 * Start the tail thread
	 */
	public void start()
	{
		Runnable execCode;
		if (_execMode == TailType.SSH)
		{
			execCode = createSshTailCode();
		}
		else if (_execMode == TailType.LOCAL)
		{
			execCode = createLocalFileTailCode();
		}
		else
		{
			throw new RuntimeException("Unknown execution type '"+_execMode+"'.");
		}
			
		_thread = new Thread(execCode);

		_thread.setName(getName());
		_thread.setDaemon(true);
		_thread.start();
	}

	/**
	 * Name of the thread
	 * @return
	 */
	private String getName()
	{
		return "FileTail("+_execMode+"): "+_filename;
	}

	/**
	 * Stop the tail thread
	 */
	public void shutdown()
	{
		_running = false;
	}

	/** just print a start message */
	protected void printStartMessage()
	{
		_logger.info("Starting "+getName());
	}
	/** just print a stop message */
	protected void printStopMessage()
	{
		_logger.info("The '"+getName()+"' has now been stopped.");
	}

	/** Get commund used by the SSH connection */
	private String getCommand()
	{
		String os = _sshConn.getOsName();
		String opt = "-f";
		if ( ! StringUtil.isNullOrBlank(os) )
		{
			if      (os.equals("Linux")) opt = "-n 999 -f";
			else if (os.equals("SunOS")) opt = "-999f";
			else if (os.equals("HP-UX")) opt = "-999f";
			else if (os.equals("AIX"))   opt = "-999f";
		}
		return "tail " + opt + " " + _filename;
	}

	/**
	 * Create the code that does the tail over SSH
	 * @return
	 */
	private Runnable createSshTailCode()
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				printStartMessage();

				_running = true;

				Session sess = null;
				try
				{
					_logger.info("Executing command '"+getCommand()+"'.");
					sess = _sshConn.execCommand(getCommand());
				}
				catch (IOException e)
				{
//					addException(e);
					_logger.error("Problems when executing OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
					_running = false;
					return;
				}

				/*
				 * Advanced:
				 * The following is a demo on how one can read from stdout and
				 * stderr without having to use two parallel worker threads (i.e.,
				 * we don't use the Streamgobblers here) and at the same time not
				 * risking a deadlock (due to a filled SSH2 channel window, caused
				 * by the stream which you are currently NOT reading from =).
				 */

				/* Don't wrap these streams and don't let other threads work on
				 * these streams while you work with Session.waitForCondition()!!!
				 */

//				BufferedReader stdout  = new BufferedReader(new InputStreamReader(sess.getStdout()));
//				BufferedReader stderr  = new BufferedReader(new InputStreamReader(sess.getStderr()));
				InputStream stdout = sess.getStdout();
				InputStream stderr = sess.getStderr();
				
				Charset osCharset = Charset.forName(_sshConn.getOsCharset());

				byte[] buffer = new byte[16*1024]; // 16K
				while(_running)
				{
					try
					{
						if ((stdout.available() == 0) && (stderr.available() == 0))
						{
							/* Even though currently there is no data available, it may be that new data arrives
							 * and the session's underlying channel is closed before we call waitForCondition().
							 * This means that EOF and STDOUT_DATA (or STDERR_DATA, or both) may
							 * be set together.
							 */

							int conditions = sess.waitForCondition(
									  ChannelCondition.STDOUT_DATA 
									| ChannelCondition.STDERR_DATA
									| ChannelCondition.EOF, 
									30*1000);

							// Wait no longer than 30 seconds
							if ((conditions & ChannelCondition.TIMEOUT) != 0)
							{
								// A timeout occurred.
								_logger.debug(">>>> A timeout occurred... continue... <<<<");
								//throw new IOException("Timeout while waiting for data from peer.");
							}

							// Here we do not need to check separately for CLOSED, since CLOSED implies EOF
							if ((conditions & ChannelCondition.EOF) != 0)
							{
								// The remote side won't send us further data...
								if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0)
								{
									// ... and we have consumed all data in the local arrival window.
									_logger.info("Received EOF from the command '"+getCommand()+"'.");
//									addException(new Exception("Received EOF from the command at time: "+new Timestamp(System.currentTimeMillis())+", \nThe module will be restarted, and the command '"+getCommand()+"' re-executed."));
				/*<--*/				break;
								}
							}

							// OK, either STDOUT_DATA or STDERR_DATA (or both) is set.

							// You can be paranoid and check that the library is not going nuts:
							// if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0)
							//	throw new IllegalStateException("Unexpected condition result (" + conditions + ")");
						}

						/* If you below replace "while" with "if", then the way the output appears on the local
						 * stdout and stder streams is more "balanced". Additionally reducing the buffer size
						 * will also improve the interleaving, but performance will slightly suffer.
						 * OKOK, that all matters only if you get HUGE amounts of stdout and stderr data =)
						 */
						while (stdout.available() > 0)
						{
							int len = stdout.read(buffer);
							if (len > 0) // this check is somewhat paranoid
							{
								// NOTE if charset convertion is needed, use: new String(buffer, CHARSET)
								String row = null;
								BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
								while ((row = sr.readLine()) != null)
								{
									fireNewTraceRow(row);
								}
							}
						}

						while (stderr.available() > 0)
						{
							int len = stderr.read(buffer);
							if (len > 0) // this check is somewhat paranoid
							{
								String row = null;
								BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
								while ((row = sr.readLine()) != null)
								{
									if (row != null && row.toLowerCase().indexOf("command not found") >= 0)
									{
										_logger.error("FileTail(SSH): was the command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
									}
//									System.out.println("STDERR: "+row);
									fireNewTraceRow(row);
									//_logger.error("Received on STDERR: "+row);
								}
							}
						}
					}
					catch (IOException e)
					{
						_logger.error("Problems when reading output from the OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
						_running = false;
					}
				}

				if (sess != null)
					sess.close();

				_running = false;
				printStopMessage();

			} // end: run()
		};
	} // end: method


	/**
	 * Create the code that does the tail on any "local" file, local = Files that can be accessed by the local machine
	 * @return
	 */
	private Runnable createLocalFileTailCode()
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				printStartMessage();

				// The file pointer keeps track of where we are in the file
				long filePointer = 0;

				// Determine start point
				if (_startAtBeginning)
					filePointer = 0;
				else
					filePointer = _localFile.length();

				try
				{
					// Start 
					_running = true;

					RandomAccessFile raf = new RandomAccessFile(_localFile, "r");
					while (_running)
					{
						try
						{
							// Compare the length of the file to the file pointer
							long fileLength = _localFile.length();
							if (fileLength < filePointer)
							{
								// Log file must have been rotated or deleted;
								// reopen the file and reset the file pointer
								raf = new RandomAccessFile(_localFile, "r");
								filePointer = 0;
							}

							if (fileLength > filePointer)
							{
								// There is data to read
								raf.seek(filePointer);
								String row = raf.readLine();
								while (row != null)
								{
									fireNewTraceRow(row);
									row = raf.readLine();
								}
								filePointer = raf.getFilePointer();
							}

							// Sleep for the specified interval
							Thread.sleep(_sleepTime);
						}
						catch (InterruptedException e)
						{
						}
						catch (FileNotFoundException e)
						{
						}
						catch (IOException e)
						{
						}
					}

					// Close the file that we are tailing
					raf.close();
				} 
				catch (FileNotFoundException e)
				{
					_logger.error("Problems tailing file", e);
				}
				catch (IOException e)
				{
					_logger.error("Problems tailing file", e);
				}

				printStopMessage();

			} // end: run()
		};
	} // end: method
}

