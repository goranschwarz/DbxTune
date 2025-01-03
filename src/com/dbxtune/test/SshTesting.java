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
package com.dbxtune.test;

import java.io.IOException;

import com.dbxtune.ssh.SshConnection;

public class SshTesting
{

	public static void main(String[] args)
	{
		SshConnection conn = new SshConnection("192.168.0.112", "gorans", "1niss2e");
		
		try
		{
			System.out.println("Connecting...");
			conn.connect();

			
			
			System.out.println("#################################################################################");
			System.out.println("Check how creating a Session for each command works...");
			try
			{
				String cmd;

				cmd = "DUMMY_VAR1=xxx; export DUMMY_VAR1; echo $$";
				System.out.println("1: " + cmd + ": Output: " + conn.execCommandOutputAsStr(cmd));

				cmd = "echo ${DUMMY_VAR1}; echo $$";
				System.out.println("2: " + cmd + ": Output: " + conn.execCommandOutputAsStr(cmd));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			System.out.println("### end...\n");
			
			
			
//			Thread.sleep(500);
//			System.out.println("#################################################################################");
//			System.out.println("Check how creating ONE Session, then execute several commands on that (which fails on the second cmd)");
//			try
//			{
//				String cmd;
//				Session sess = conn.openSession();
//
//				cmd = "DUMMY_VAR1=xxx; export DUMMY_VAR1; echo $$";
//				System.out.println("s-1: " + cmd + ": Output: " + SshSession.execCommandOutputAsStr(sess, cmd));
//				
//				cmd = "echo ${DUMMY_VAR1}; echo $$";
//				System.out.println("s-2: " + cmd + ": Output: " + SshSession.execCommandOutputAsStr(sess, cmd));
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
//			System.out.println("### end...\n");



//			Thread.sleep(500);
//			System.out.println("#################################################################################");
//			System.out.println("Check how creating a Shell, then execute several commands on that...");
//			try
//			{
//				String cmd;
//				Session sess = conn.openSession();
//				sess.startShell();
//
//				cmd = "DUMMY_VAR1=xxx; export DUMMY_VAR1; echo $$";
//				System.out.println("shell-1: " + cmd + ": Output: " + SshSession.ShellExecCommandOutputAsStr(sess, cmd));
//				
//				cmd = "echo ${DUMMY_VAR1}; echo $$";
//				System.out.println("shell-2: " + cmd + ": Output: " + SshSession.ShellExecCommandOutputAsStr(sess, cmd));
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
//			System.out.println("### end...\n");



			System.out.println("Closing...");
			conn.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
//	private static class SshSession
//	{
//		synchronized public static String execCommandOutputAsStr(Session sess, String command) 
//		throws IOException
//		{
////			if (isClosed())
////				throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");
//
////			Session sess = _conn.openSession();
//			sess.execCommand(command);
//
//			InputStream stdout = new StreamGobbler(sess.getStdout());
//			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
//
//			StringBuilder sb = new StringBuilder();
//			while (true)
//			{
//				String line = br.readLine();
//				if (line == null)
//					break;
//
//				sb.append(line);
//				sb.append("\n");
//			}
//			String output = sb.toString();
//
//			Integer rc = sess.getExitStatus();
//
////			sess.close();
//			br.close();
//			
////			_logger.debug("execCommandRetAsStr: '"+command+"' produced '"+output+"'.");
//
//			if (rc != null && rc.intValue() != 0)
//				throw new IOException("execCommandRetAsStr('"+command+"') return code not zero. rc="+rc+". Output: "+output);
//
//			return output;
//		}
//
//		synchronized public static String ShellExecCommandOutputAsStr(Session sess, String command) 
//		throws IOException
//		{
////			if (isClosed())
////				throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");
//
////			Session sess = _conn.openSession();
////			sess.execCommand(command);
//
//System.out.println("x:before:getStdout");
//			BufferedReader stdout_br = new BufferedReader(new InputStreamReader(new StreamGobbler(sess.getStdout())));
//System.out.println("x:before:write");
//			sess.getStdin().write(command.getBytes());;
//			sess.getStdin().write("\n".getBytes());;
//
//System.out.println("x:before:read:stdout");
//			StringBuilder sb = new StringBuilder();
//			while (true)
//			{
//				System.out.println("x:before:readLine()");
//				String line = stdout_br.readLine();
//				System.out.println("x:after:readLine(): line='"+line+"'.");
//				if (line == null)
//					break;
//
//				sb.append(line);
//				sb.append("\n");
//			}
//			String output = sb.toString();
//
////			Integer rc = sess.getExitStatus();
//
//System.out.println("x:before:read:stdout.close");
//			stdout_br.close();
//			
////			_logger.debug("execCommandRetAsStr: '"+command+"' produced '"+output+"'.");
//
////			if (rc != null && rc.intValue() != 0)
////				throw new IOException("execCommandRetAsStr('"+command+"') return code not zero. rc="+rc+". Output: "+output);
//
//System.out.println("x:before:return");
//			return output;
//		}
//	}
}
