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
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Session;

import com.asetune.ssh.SshConnection;

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
	/** Charset that a local file consists of */
	private Charset _localFileCharset = null;

	/**
	 * Defines whether the log file tailer should include the entire contents of
	 * the existing log file or tail from the end of the file when the tailer starts
	 */
	private boolean _startAtBeginning = false;

	/** if not start at the start of the file, go back this amount of lines when starting the tail */
	private int _linesFromEnd = 10;

	/** Is the tailer currently tailing? */
	private boolean _running = false;

	/** This will be set to true right before the thread is ending */
	private boolean _shutdownIsComplete = false;

	/** Used if we want to execute some other command like |echo 'some-password' | sudo -p '' -S -u sybase tail -f /opt/.../filename| */
	private String _osCmd    = null;

	/** Used if we want to execute some other command like |echo 'some-password' | sudo -p '' -S -u sybase tail -f /opt/.../filename| */
	private String _password = null;

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
		this(filename, startAtBeginning, 0);
	}
	public FileTail(String filename, int linesFromEnd)
	{
		this(filename, false, linesFromEnd);
	}
	private FileTail(String filename, boolean startAtBeginning, int linesFromEnd)
	{
		_filename         = filename;
		_localFile        = new File(filename);

		_startAtBeginning = startAtBeginning;
		_linesFromEnd     = linesFromEnd;

		_execMode = TailType.LOCAL;
	}
	
	public void setLocalFileCharset(Charset charset)
	{
		_localFileCharset = charset;
	}

	/**
	 * Do remote "tail"
	 * @param sshConn A SshConnection already connected to the backend
	 * @param filename Name of the file
	 * @param startAtBeginning start from "top" of the file
	 */
	public FileTail(SshConnection sshConn, String filename, boolean startAtBeginning)
	{
		this(sshConn, filename, startAtBeginning, 0);
	}
	public FileTail(SshConnection sshConn, String filename, int linesFromEnd)
	{
		this(sshConn, filename, false, linesFromEnd);
	}
	private FileTail(SshConnection sshConn, String filename, boolean startAtBeginning, int linesFromEnd)
	{
		_sshConn          = sshConn;
		_filename         = filename;

		_startAtBeginning = startAtBeginning;
		_linesFromEnd     = linesFromEnd;

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

	/**
	 * If we want something else than tail to be executed<br> 
	 * Most likely some: sudo or similar<br>
	 * The following "variables" will be substituted to something better
	 * <ul>
	 *   <li><code>${cmd}     </code> to: tail -f logFileName</li>
	 *   <li><code>${password}</code> to: the password you passed in</li>
	 *   <li><code>${filename}</code> to: the name of the log file</li>
	 * </ul>
	 * @param osCmd
	 * @param passwd
	 */
	public void setOsCmd(String osCmd, String passwd)
	{
		_osCmd    = osCmd;
		_password = passwd;
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
	 * create a file.
	 * @return
	 * @throws IOException
	 */
	public boolean createFile()
	throws IOException
	{
		if (_execMode == TailType.LOCAL)
		{
			if (_localFile == null)
				throw new RuntimeException("Local file can not be null");

			return _localFile.createNewFile();
		}
		else if (_execMode == TailType.SSH)
		{
			return _sshConn.createNewFile(_filename);
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

	/**
	 * Wait until the thread has ended.
	 */
	public void waitForShutdownToComplete()
	{
		while(true)
		{
			if (_shutdownIsComplete)
			{
				_logger.info("The '"+getName()+"' has now been shutdown.");
				break;
			}

			try
			{
				_logger.info("Waiting for '"+getName()+"' to complete the shutdown.");
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				_logger.info("The '"+getName()+"' has was interrupted.");
				break;
			}
			
		}
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

	/** Get command used by the SSH connection */
	private String getCommand(boolean hidePassword)
	{
		String os  = _sshConn.getOsName();
		String opt = "-f";
		int    num = _linesFromEnd;
		if (_startAtBeginning)
			num = 999;
		if ( ! StringUtil.isNullOrBlank(os) )
		{
			if      (os.equals("Linux")) opt = "-n "+num+" -f";
			else if (os.equals("SunOS")) opt = "-"+num+"f";
			else if (os.equals("HP-UX")) opt = "-"+num+"f";
			else if (os.equals("AIX"))   opt = "-"+num+"f";
		}
		
		if (StringUtil.hasValue(_osCmd))
		{
			String tailCmd = "tail " + opt + " " + _filename;
			String osCmd   = _osCmd;
			
			osCmd = hidePassword ? osCmd.replace("${password}", "_hidden_password_") : osCmd.replace("${password}", _password);
			osCmd = osCmd.replace("${cmd}",      tailCmd);
			osCmd = osCmd.replace("${filename}", _filename);

			return osCmd;
		}
		else
		{
			return "tail " + opt + " " + _filename;
		}
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
					_logger.info("Executing command '"+getCommand(true)+"'.");
					sess = _sshConn.execCommand(getCommand(false));
				}
				catch (IOException e)
				{
//					addException(e);
					_logger.error("Problems when executing OS Command '"+getCommand(true)+"', Caught: "+e.getMessage(), e);
					_running = false;
					return;
				}

				_shutdownIsComplete = false;

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

				String strSpillOut = null;
				String strSpillErr = null;

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
									_logger.info("Received EOF from the command '"+getCommand(true)+"'.");
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
//							int len = stdout.read(buffer);
//							if (len > 0) // this check is somewhat paranoid
//							{
//								// NOTE if charset convertion is needed, use: new String(buffer, CHARSET)
//								String row = null;
//								BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
//								while ((row = sr.readLine()) != null)
//								{
//									fireNewTraceRow(row);
//								}
//							}

							int len = stdout.read(buffer);
							if (len > 0) // this check is somewhat paranoid
							{
								// NOTE if charset convertion is needed, use: new String(buffer, CHARSET)
								String strBuf = new String(buffer, 0, len, osCharset);

								// add "spill" that did NOT have a newline terminator
								if (strSpillOut != null)
								{
									strBuf = strSpillOut + strBuf;
									len += strSpillOut.length();
									strSpillOut = null;
								}

								// Check for "spill" that is after last newline
								// if we have "spill" save that to next iteration
								int lastNlPos = strBuf.lastIndexOf('\n');
								if (lastNlPos == -1) // No newline was found, save the whole string to next read
								{
									strSpillOut = strBuf;
									strBuf = null;
								} 
								else if (len > lastNlPos)  // string after last newline = save to next read
								{
									strSpillOut = strBuf.substring(lastNlPos + 1);
									strBuf      = strBuf.substring(0, lastNlPos);
								}

								// read the buffer line-by-line and "send" it to any listeners
								if (strBuf != null)
								{
									BufferedReader br = new BufferedReader(new StringReader(strBuf));
									String row = null;
									while ((row = br.readLine()) != null)
									{
										fireNewTraceRow(row);
									}
								}
							}
						}

						while (stderr.available() > 0)
						{
//							int len = stderr.read(buffer);
//							if (len > 0) // this check is somewhat paranoid
//							{
//								String row = null;
//								BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
//								while ((row = sr.readLine()) != null)
//								{
//									if (row != null && row.toLowerCase().indexOf("command not found") >= 0)
//									{
//										_logger.error("FileTail(SSH): was the command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
//									}
//									//System.out.println("STDERR: "+row);
//									fireNewTraceRow(row);
//									//_logger.error("Received on STDERR: "+row);
//								}
//							}

							int len = stderr.read(buffer);
							if (len > 0) // this check is somewhat paranoid
							{
								// NOTE if charset convertion is needed, use: new String(buffer, CHARSET)
								String strBuf = new String(buffer, 0, len, osCharset);

								// add "spill" that did NOT have a newline terminator
								if (strSpillErr != null)
								{
									strBuf = strSpillErr + strBuf;
									len += strSpillErr.length();
									strSpillErr = null;
								}

								// Check for "spill" that is after last newline
								// if we have "spill" save that to next iteration
								int lastNlPos = strBuf.lastIndexOf('\n');
								if (lastNlPos == -1) // No newline was found, save the whole string to next read
								{
									strSpillErr = strBuf;
									strBuf = null;
								} 
								else if (len > lastNlPos)  // string after last newline = save to next read
								{
									strSpillErr = strBuf.substring(lastNlPos + 1);
									strBuf      = strBuf.substring(0, lastNlPos);
								}

								// read the buffer line-by-line and "send" it to any listeners
								if (strBuf != null)
								{
									BufferedReader br = new BufferedReader(new StringReader(strBuf));
									String row = null;
									while ((row = br.readLine()) != null)
									{
										if (row != null && row.toLowerCase().indexOf("command not found") >= 0)
										{
											_logger.error("FileTail(SSH): was the command '"+getCommand(true)+"' in current $PATH, got following message on STDERR: "+row);
										}
										//System.out.println("STDERR: "+row);
										fireNewTraceRow(row);
										//_logger.error("Received on STDERR: "+row);
									}
								}
							}
						}
					}
					catch (IOException e)
					{
						_logger.error("Problems when reading output from the OS Command '"+getCommand(true)+"', Caught: "+e.getMessage(), e);
						_running = false;
					}
				}

				if (sess != null)
					sess.close();

				_running = false;
				printStopMessage();
				_shutdownIsComplete = true;

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
				LinkedList<String> startupRows = null;

				// The file pointer keeps track of where we are in the file
				long filePointer = 0;

				// Determine start point
				if (_startAtBeginning)
				{
					filePointer = 0;
				}
				else
				{
					filePointer = _localFile.length();
					// FIXME for a better estimates...
					filePointer -= _linesFromEnd * 160; // lets say one row is 160 chars wide
					if (filePointer < 0)
						filePointer = 0;
					else
						startupRows = new LinkedList<String>();
				}

				try
				{
					// Start 
					_running = true;
					_shutdownIsComplete = false;

					byte[] buffer = new byte[16*1024]; // 16K
//					byte[] buffer = new byte[1024]; // 16K
//					byte[] buffer = new byte[80]; // 16K
					String strSpill = null;

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
								int len = raf.read(buffer);
								if (len > 0)
								{
									String strBuf;
									if (_localFileCharset == null)
										strBuf = new String(buffer, 0, len);
									else
										strBuf = new String(buffer, 0, len, _localFileCharset);
									
									// add "spill" that did NOT have a newline terminator
									if (strSpill != null)
									{
										strBuf = strSpill + strBuf;
										len += strSpill.length();
//										System.out.println("++++++ SPILL-INS: |"+strSpill+"|");
//										System.out.println("++++++ SPILL-RES: |"+strBuf+"|");
										strSpill = null;
									}

									// Check for "spill" that is after last newline
									// if we have "spill" save that to next iteration
									int lastNlPos = strBuf.lastIndexOf('\n');
									if (lastNlPos == -1) // No newline was found, save the whole string to next read
									{
										strSpill = strBuf;
										strBuf = null;
//										System.out.println("------ NO-NEW-LINE - SPILL: |"+strSpill+"|");
									} 
									else if (len > lastNlPos)  // string after last newline = save to next read
									{
//										System.out.println("------ CHECK: len("+len+") > lastNlPos("+lastNlPos+").");
										
										strSpill = strBuf.substring(lastNlPos + 1);
										strBuf   = strBuf.substring(0, lastNlPos);

//										System.out.println("------ SPILL: |"+strSpill+"|");
//										System.out.println("------ KEEP:  |"+strBuf+"|");
									}

									// read the buffer line-by-line and "send" it to any listeners
									if (strBuf != null)
									{
										BufferedReader br = new BufferedReader(new StringReader(strBuf));
										String row = null;
										while ((row = br.readLine()) != null)
										{
											if (startupRows != null)
												startupRows.add(row);
											else
												fireNewTraceRow(row);
										}
									}
								}
								filePointer = raf.getFilePointer();
							}
							else
							{
								// If we are in startup mode...
								//   - remove some entries from the "saved" list
								//   - all the listeners with the rows
								//   - then remove the list to go into "normal" mode
								// else
								//   - wait for some time and check the file for new entries
								if (startupRows != null)
								{
									while(startupRows.size() > _linesFromEnd)
										startupRows.removeFirst();
									
									for (String row : startupRows)
										fireNewTraceRow(row);

									startupRows = null;
								}
								else
								{
									// Sleep for the specified interval
									Thread.sleep(_sleepTime);
								}
							}
						}
						catch (InterruptedException e)
						{
							_logger.debug("InterruptedException: Tail on file '"+_localFile+"'. Checking if 'running' is still true. Caught: "+e);
						}
						catch (FileNotFoundException e)
						{
							_logger.debug("FileNotFoundException: Tail on file '"+_localFile+"'. Continuing, hopefully it will exist in next iteration. Caught: "+e);
						}
						catch (IOException e)
						{
							_logger.debug("IOException: Tail on file '"+_localFile+"'. This is ignored, continuing. Caught: "+e);
						}
					}

					// Close the file that we are tailing
					raf.close();
				} 
				catch (FileNotFoundException e)
				{
					_logger.error("Problems Tail on file '"+_localFile+"'. Caught: "+e, e);
				}
				catch (IOException e)
				{
					_logger.error("Problems Tail on file '"+_localFile+"'. Caught: "+e, e);
				}

				printStopMessage();
				_shutdownIsComplete = true;

			} // end: run()
		};
	} // end: method
}

