/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.hostmon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.dbxtune.hostmon.HostMonitorConnection.ExecutionWrapper;

/**
 * A long lived PowerShell process, which we send scripts to (on STDIN) and read the results from (on STDOUT/STDERR)
 * <p>
 * Why: On Windows every <code>powershell -Command "..."</code> has to load PowerShell and the .NET runtime, which often takes longer
 * than the command itself. With a session we only do that once (per connection).
 * <p>
 * How it works:
 * <ul>
 *   <li>PowerShell is started with {@link #START_COMMAND}, it then reads from STDIN and executes each row when it arrives</li>
 *   <li>After every script we send a "End Of Command" marker row, which is written on both STDOUT and STDERR, so we know when all output has arrived</li>
 *   <li>When STDIN is closed (by {@link #close()}, or when the SSH channel/connection goes away) PowerShell exits by itself, so it's NOT left as an orphan</li>
 * </ul>
 * This class only knows about the three streams. The process/channel is created by the caller, which also implements {@link #isClosed()}
 */
public abstract class PowershellSession
{
	/** NOTE: No quotes, $, | or % in here: So it's parsed the same way by a CMD and a PowerShell login shell */
	public static final String START_COMMAND = "powershell -NoLogo -NoProfile -NonInteractive -Command -";

	private final OutputStream _stdin;
	private final InputStream  _stdout;
	private final InputStream  _stderr;
	private final Charset      _charset;

	/** "End Of Command" marker, unique for this session */
	private final String       _marker = "DBXTUNE-EOC-" + UUID.randomUUID();

	public PowershellSession(OutputStream stdin, InputStream stdout, InputStream stderr, Charset charset)
	{
		_stdin   = stdin;
		_stdout  = stdout;
		_stderr  = stderr;
		_charset = charset;
	}

	/**
	 * @return true if the underlying process/channel is gone (then nothing more will arrive on STDOUT/STDERR)
	 */
	public abstract boolean isClosed();

	/**
	 * Close STDIN, PowerShell then gets EOF and exits by itself
	 */
	public void close()
	{
		try { _stdin.close(); }
		catch (Exception ignore) { /* ignore */ }
	}

	/**
	 * Execute a script in the session, and wait for it to finish
	 *
	 * @param script      A single line PowerShell script
	 * @param timeoutSec  Max time to wait for the script to finish (0 = wait forever)
	 * @return A already finished ExecutionWrapper, holding the scripts STDOUT, STDERR and exit status (0 = success, 1 = the last statement failed)
	 *
	 * @throws TimeoutException  if the script did not finish in time. NOTE: The session is then in a unknown state and should be closed
	 * @throws IOException       if the session was closed before the script finished
	 */
	public synchronized ExecutionWrapper execute(String script, int timeoutSec)
	throws Exception
	{
		// Only single line scripts: PowerShell executes when a newline arrives, and a incomplete statement (like a unbalanced '{') would "swallow" the marker row
		if (script.indexOf('\n') >= 0 || script.indexOf('\r') >= 0)
			throw new IllegalArgumentException("The PowerShell script must be a single line (no newlines). script=|" + script + "|.");

		byte[] buf = new byte[8192];

		// Discard anything left from a previous command (should be nothing)
		while (_stdout.available() > 0) _stdout.read(buf, 0, Math.min(_stdout.available(), buf.length));
		while (_stderr.available() > 0) _stderr.read(buf, 0, Math.min(_stderr.available(), buf.length));

		// Send the script, and then the marker row: on STDOUT (with $? = did the script succeed) and on STDERR
		// The marker is on both streams, so errors from this script can't "leak" into the next one
		String cmd = script + "\n"
		           + "Write-Output \"" + _marker + " $?\"; [Console]::Error.WriteLine(\"" + _marker + "\")\n";

		_stdin.write(cmd.getBytes(_charset)); // NOTE: raw bytes, a BOM would be executed as a "command"
		_stdin.flush();                       // NOTE: JSch do not send anything until flush()

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		int  outPos    = -1; // Position of the marker on STDOUT
		int  outEol    = -1; // Position of the newline after the marker on STDOUT
		int  errPos    = -1; // Position of the marker on STDERR
		int  errEol    = -1; // Position of the newline after the marker on STDERR
		long startTime = System.currentTimeMillis();

		while (outEol < 0 || errEol < 0)
		{
			boolean gotData = false;

			// NOTE: Only read what is available(), a blocking read() can't be timed out
			if (_stdout.available() > 0)
			{
				int len = _stdout.read(buf, 0, Math.min(_stdout.available(), buf.length));
				if (len > 0)
				{
					out.write(buf, 0, len);
					gotData = true;
				}
			}
			if (_stderr.available() > 0)
			{
				int len = _stderr.read(buf, 0, Math.min(_stderr.available(), buf.length));
				if (len > 0)
				{
					err.write(buf, 0, len);
					gotData = true;
				}
			}

			if (gotData)
			{
				// ISO-8859-1 = one char per byte, so a String position is also the byte position (the marker is plain ASCII)
				String outStr = out.toString("ISO-8859-1");
				String errStr = err.toString("ISO-8859-1");

				outPos = outStr.indexOf(_marker);
				outEol = outPos < 0 ? -1 : outStr.indexOf('\n', outPos);
				errPos = errStr.indexOf(_marker);
				errEol = errPos < 0 ? -1 : errStr.indexOf('\n', errPos);
				continue;
			}

			if (isClosed())
				throw new IOException("The PowerShell session was closed before the script finished. script=|" + script + "|.");

			long execTimeMs = System.currentTimeMillis() - startTime;
			if (timeoutSec > 0 && execTimeMs > timeoutSec * 1000L)
				throw new TimeoutException("Timeout: The PowerShell script has not finished after " + (execTimeMs / 1000) + " seconds. script=|" + script + "|.");

			Thread.sleep(5);
		}

		// The marker row on STDOUT looks like: DBXTUNE-EOC-uuid True|False
		String  outStr  = out.toString("ISO-8859-1");
		boolean success = outStr.substring(outPos + _marker.length(), outEol).trim().equalsIgnoreCase("True");

		// Everything before the marker is the output from the script
		final InputStream stdout     = new ByteArrayInputStream(out.toByteArray(), 0, outPos);
		final InputStream stderr     = new ByteArrayInputStream(err.toByteArray(), 0, errPos);
		final int         exitStatus = success ? 0 : 1;

		return new ExecutionWrapper()
		{
			@Override public void        executeCommand(String cmd)                             {}
			@Override public void        executeCommand(String cmd, boolean isStreamingCommand) {}
			@Override public InputStream getStdout()     { return stdout; }
			@Override public InputStream getStderr()     { return stderr; }
			@Override public Integer     getExitStatus() { return exitStatus; }
			@Override public int         waitForData()   { return 0; }
			@Override public boolean     isClosed()      { return true; }  // The script has finished, the session is still open
			@Override public void        close()         {}
		};
	}



	// SMALL TEST -- via main... Uses a LOCAL 'powershell' process (so only on Windows), no SSH is involved
	public static void main(String[] args)
	throws Exception
	{
		final Process[]           proc       = new Process[1];
		final PowershellSession[] session    = new PowershellSession[1];
		final boolean[]           useSession = new boolean[] {true};

		// A local "connection" which has a PowerShell session
		HostMonitorConnection conn = new HostMonitorConnectionLocalOsCmd(true)
		{
			@Override
			public ExecutionWrapper executeInPowershellSession(String psScript)
			throws Exception
			{
				if ( ! useSession[0] )
					return null;

				if (session[0] == null)
				{
					long startTime = System.currentTimeMillis();
					proc[0]    = new ProcessBuilder(START_COMMAND.split(" ")).start();
					session[0] = new PowershellSession(proc[0].getOutputStream(), proc[0].getInputStream(), proc[0].getErrorStream(), Charset.forName(getOsCharset()))
					{
						@Override public boolean isClosed() { return ! proc[0].isAlive(); }
					};
					session[0].execute("$ProgressPreference = 'SilentlyContinue'", 20);
					System.out.println("Started the PowerShell session in " + (System.currentTimeMillis() - startTime) + " ms.");
				}
				return session[0].execute(psScript, 60);
			}
		};

		// The real monitors: in the session, and as a normal command (which starts a new 'powershell' every time)
		HostMonitor[] monitors = new HostMonitor[] { MonitorPs.createMonitor(conn, false), MonitorDiskSpace.createMonitor(conn, false) };
		for (HostMonitor mon : monitors)
		{
			for (boolean inSession : new boolean[] {true, false})
			{
				useSession[0] = inSession;

				int    execCount = inSession ? 20 : 3;
				String timeStr   = "";
				int    rowCount  = -1;
				for (int i = 0; i < execCount; i++)
				{
					long    startTime = System.currentTimeMillis();
					OsTable sample    = mon.executeAndParse();
					timeStr  += (System.currentTimeMillis() - startTime) + " ";
					rowCount  = sample == null ? -1 : sample.getRowCount();
				}
				System.out.println(mon.getModuleName() + ": " + (inSession ? "SESSION" : "COMMAND") + ", rows=" + rowCount + ", ms=[" + timeStr.trim() + "]");
				System.out.println("    mode:     " + mon.getExecModeDescription());
				System.out.println("    executed: " + mon.getExecutedCommand());
			}
		}
		useSession[0] = true;

		// A failing script: exit status 1, the error on STDERR, and the session should still work
		ExecutionWrapper execWrapper = session[0].execute("Get-NoSuchCommand-xyz", 10);
		System.out.println("FAILING SCRIPT: exitStatus=" + execWrapper.getExitStatus() + ", stdoutBytes=" + execWrapper.getStdout().available() + ", stderrBytes=" + execWrapper.getStderr().available());

		execWrapper = session[0].execute("Write-Output 'still-alive'", 10);
		System.out.println("AFTER FAILURE:  exitStatus=" + execWrapper.getExitStatus() + ", stdoutBytes=" + execWrapper.getStdout().available() + ", stderrBytes=" + execWrapper.getStderr().available());

		// Multi line scripts are rejected
		try { session[0].execute("Write-Output 'a'\nWrite-Output 'b'", 10); System.out.println("MULTI LINE:     NOT rejected (ERROR)"); }
		catch (IllegalArgumentException ex) { System.out.println("MULTI LINE:     rejected (OK)"); }

		// Timeout
		long startTime = System.currentTimeMillis();
		try { session[0].execute("Start-Sleep 5", 1); System.out.println("TIMEOUT:        NO timeout (ERROR)"); }
		catch (TimeoutException ex) { System.out.println("TIMEOUT:        after " + (System.currentTimeMillis() - startTime) + " ms (OK). " + ex.getMessage()); }

		// close() = EOF on STDIN: the process should exit by itself
		session[0].close();
		System.out.println("CLOSE:          process exited=" + proc[0].waitFor(10, java.util.concurrent.TimeUnit.SECONDS));

		// A killed process: IOException (and not a timeout)
		session[0] = null;
		conn.executeInPowershellSession("Write-Output 'new-session'");
		proc[0].destroyForcibly();
		startTime = System.currentTimeMillis();
		try { session[0].execute("Write-Output 'x'", 30); System.out.println("KILLED:         NO exception (ERROR)"); }
		catch (IOException ex) { System.out.println("KILLED:         after " + (System.currentTimeMillis() - startTime) + " ms (OK). " + ex.getMessage()); }
	}
}
