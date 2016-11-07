package com.asetune.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshTesting2
{
	public static String	  PROMPT = "GO1GO2GO3";
	final private static char LF	 = '\n';
	final private static char CR	 = '\r';

	public static String join(final Collection<String> str, final String sep)
	{
		final StringBuilder sb = new StringBuilder();
		final Iterator<String> i = str.iterator();
		while (i.hasNext())
		{
			sb.append(i.next());
			if ( i.hasNext() )
			{
				sb.append(sep);
			}
		}
		return sb.toString();
	}

	public static void main(String[] args) throws IOException
	{
		Connection c = new Connection("192.168.0.112");
		try
		{
			c.connect();
			if ( !c.authenticateWithPassword("gorans", "1niss2e") )
			{
				throw new IOException("Authentification failed");
			}
			Session s = c.openSession();
			s.requestDumbPTY();
			s.startShell();
			
			InputStream is = s.getStdout(); 
			OutputStream os = s.getStdin(); 
			InputStream es = s.getStderr();
			
			final PushbackInputStream pbis = new PushbackInputStream(new StreamGobbler(is));
			final Collection<String> lines = new LinkedList<String>();

			writeCmd(os, pbis, "PS1=" + PROMPT);
			readTillPrompt(pbis, null);
System.out.println("Exit Status: " + s.getExitStatus() );

			writeCmd(os, pbis, "ls -l --color=never");
			readTillPrompt(pbis, lines);
			System.out.println("Out: " + join(lines, Character.toString(LF)));
			lines.clear();
System.out.println("Exit Status: " + s.getExitStatus() );

			writeCmd(os, pbis, "free -m");
			readTillPrompt(pbis, lines);
			System.out.println("Out: " + join(lines, Character.toString(LF)));
			lines.clear();
System.out.println("Exit Status: " + s.getExitStatus() );
		}
		finally
		{
			c.close();
		}
	}

	public static void writeCmd(final OutputStream os, final PushbackInputStream is, final String cmd) throws IOException
	{
		System.out.println("In: " + cmd);
		os.write(cmd.getBytes());
		os.write(LF);
		skipTillEndOfCommand(is);
	}

	public static void readTillPrompt(final InputStream is, final Collection<String> lines) throws IOException
	{
		final StringBuilder cl = new StringBuilder();
		boolean eol = true;
		int match = 0;
		while (true)
		{
			final char ch = (char) is.read();
			switch (ch)
			{
			case CR:
			case LF:
				if ( !eol )
				{
					if ( lines != null )
					{
						lines.add(cl.toString());
					}
					cl.setLength(0);
				}
				eol = true;
				break;
			default:
				if ( eol )
				{
					eol = false;
				}
				cl.append(ch);
				break;
			}

			if ( cl.length() > 0 && match < PROMPT.length() && cl.charAt(match) == PROMPT.charAt(match) )
			{
				match++;
				if ( match == PROMPT.length() )
				{
					return;
				}
			}
			else
			{
				match = 0;
			}
		}
	}

	public static void skipTillEndOfCommand(final PushbackInputStream is) throws IOException
	{
		boolean eol = false;
		while (true)
		{
			final char ch = (char) is.read();
			switch (ch)
			{
			case CR:
			case LF:
				eol = true;
				break;
			default:
				if ( eol )
				{
					is.unread(ch);
					return;
				}
			}
		}
	}
}
