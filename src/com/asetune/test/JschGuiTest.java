/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.ssh.RemoteFileSystemView;
import com.asetune.ssh.SshConnection;
import com.asetune.ssh.SshConnection.ExecOutput;
import com.asetune.utils.SwingUtils;
import com.jcraft.jsch.ChannelExec;

import net.miginfocom.swing.MigLayout;

public class JschGuiTest
{
	private static Logger _logger = Logger.getLogger(JschGuiTest.class);


	public static void main(String[] args)
	{
		// Set Log4J Properties
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
		catch(Exception ex) { ex.printStackTrace();	}

//		DummyApp app = new DummyApp("gorans.org", "gorans", "1niss2e", "", "id; uname -a; hostname; date", "iostat -xdtck 5");
		DummyApp app = new DummyApp("pg-1a-gs"  , "gorans", "1niss2e", "", "id; uname -a; hostname; date", "iostat -xdtck 5");

        app.setVisible(true);
        
        System.out.println("--------- END ---------");

//		try
//		{
//			String user = "gorans";
//			String host = "gorans.org";
//
//
//			SshConnection ssh2 = new SshConnection(host, 22, user, "passwd", null);
//			ssh2.connect();
//			
//			String cmd = "uname -a";
////			String cmd = "iostat -xdck 3";
////			String cmd = "/home/sybase/xxx.sh";
////			String cmd = "/home/sybase/xxx.sh 3";
//
//			ExecOutput out = ssh2.execCommandOutput(cmd);
//			System.out.println("OUT: " + out);
//			
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//		}
	}
	
	
	private static class DummyApp
	extends JFrame
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

		public DummyApp(String hostname, String username, String password, String keyfile, String cmd1, String cmd2)
		{
			init();
			
			_hostname_txt.setText(hostname);
			_username_txt.setText(username);
			_password_txt.setText(password);
			_keyFile_txt .setText(keyfile);
			_command1_txt.setText(cmd1);
			_command2_txt.setText(cmd2);
		}

		JLabel     _hostname_lbl  = new JLabel("Hostname");
		JTextField _hostname_txt  = new JTextField();

		JLabel     _username_lbl  = new JLabel("Username");
		JTextField _username_txt  = new JTextField();

		JLabel     _password_lbl  = new JLabel("Password");
		JTextField _password_txt  = new JTextField();

		JLabel     _keyFile_lbl  = new JLabel("Key File");
		JTextField _keyFile_txt  = new JTextField();

		JLabel     _command1_lbl = new JLabel("Command");
		JTextField _command1_txt = new JTextField();
		JButton    _exec1_but    = new JButton("Execute Short Cmd");

		JLabel     _command2_lbl = new JLabel("Command");
		JTextField _command2_txt = new JTextField();
		JButton    _exec2_but    = new JButton("Execute Streaming");

		JTextArea  _output_txt    = new JTextArea();
		
		JButton _connect_but    = new JButton("Connect");
		JButton _reConnect_but  = new JButton("ReConnect");
		JButton _disConnect_but = new JButton("Close Connection");
		JButton _rFileView_but  = new JButton("Remote File View");
		JButton _close_but      = new JButton("Close Window");

		SshConnection _conn = null;

		public void init()
		{
			setDefaultLookAndFeelDecorated(true);
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setSize(600, 800);

			JPanel panel = new JPanel(new MigLayout());

			//---------------------------------------------------
			JPanel upperPanel = new JPanel(new MigLayout());

			upperPanel.add(_hostname_lbl , "");
			upperPanel.add(_hostname_txt , "pushx, growx, wrap");

			upperPanel.add(_username_lbl , "");
			upperPanel.add(_username_txt , "pushx, growx, wrap");

			upperPanel.add(_password_lbl , "");
			upperPanel.add(_password_txt , "pushx, growx, wrap");
			
			upperPanel.add(_keyFile_lbl , "");
			upperPanel.add(_keyFile_txt , "pushx, growx, wrap 20");
			
			upperPanel.add(_command1_lbl , "");
			upperPanel.add(_command1_txt , "split, pushx, growx");
			upperPanel.add(_exec1_but    , "wrap");
			
			upperPanel.add(_command2_lbl , "");
			upperPanel.add(_command2_txt , "split, pushx, growx");
			upperPanel.add(_exec2_but    , "wrap");

			_command1_txt.addActionListener(this);
			_exec1_but   .addActionListener(this);

			_command2_txt.addActionListener(this);
			_exec2_but   .addActionListener(this);
			
			
			//---------------------------------------------------
			_output_txt.setVisible(true);
			JScrollPane output_scrcoll = new JScrollPane(_output_txt);


			//---------------------------------------------------
			JPanel lowerPanel = new JPanel(new MigLayout());

			lowerPanel.add(_connect_but   , "");
			lowerPanel.add(_reConnect_but , "");
			lowerPanel.add(_disConnect_but, "");
			lowerPanel.add(_rFileView_but , "");
			lowerPanel.add(_close_but     , "wrap");

			_connect_but   .addActionListener(this);
			_reConnect_but .addActionListener(this);
			_disConnect_but.addActionListener(this);
			_rFileView_but .addActionListener(this);
			_close_but     .addActionListener(this);
			
			//---------------------------------------------------
			panel.add(upperPanel, "grow, wrap");
			panel.add(lowerPanel, "growx, wrap");

			
//			setContentPane(panel);
			getContentPane().add(upperPanel, BorderLayout.NORTH);
			getContentPane().add(output_scrcoll, BorderLayout.CENTER);
			getContentPane().add(lowerPanel, BorderLayout.SOUTH);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			System.out.println("actionPerformed(): e=" + e);

			Object source = e.getSource();
			
			if (_close_but.equals(source))
			{
				System.out.println("---CLOSE---");
				this.setVisible(false);
				System.exit(0);
			}

			if (_exec1_but.equals(source) || _command1_txt.equals(source))
			{
				System.out.println("---EXEC-SHORT---");
				action_execute_short(e);
			}

			if (_exec2_but.equals(source) || _command2_txt.equals(source))
			{
				System.out.println("---EXEC-LONG---");
				action_execute_stream(e);
			}

			if (_connect_but.equals(source))
			{
				System.out.println("---CONNECT---");
				if (_conn == null)
				{
					_conn = new SshConnection(_hostname_txt.getText(), 22, _username_txt.getText(), _password_txt.getText(), _keyFile_txt.getText());			
					_conn.setGuiOwner(this);
				}

				try
				{
					_conn = sshConnect();
//					_conn.connect();
//					System.out.println("-------- PID=" + _conn.getPid() );
				}
				catch (Exception ex)
				{
					_conn = null;
					ex.printStackTrace();
				}
			}

			if (_reConnect_but.equals(source))
			{
				System.out.println("---RE-CONNECT---");
				try
				{
					_conn.close();
					_conn.reconnect();
//					System.out.println("-------- PID=" + _conn.getPid() );
				}
				catch (Exception ex)
				{
					_conn = null;
					ex.printStackTrace();
				}
			}

			if (_disConnect_but.equals(source))
			{
				System.out.println("---DISCONNECT---");
				if (_conn != null)
				{
					_conn.close();
					_conn = null;
				}
			}

			if (_rFileView_but.equals(source))
			{
				System.out.println("---REMOTE FILE VIEW---");
				
				if (_conn == null)
				{
					_output_txt.setText("--- SORRY NOT YET CONNECTED...");
					return;
				}

				try
				{
					RemoteFileSystemView rfsv = new RemoteFileSystemView(_conn);
					
					String hostPortLabel = _hostname_txt.getText() + ":" + 22;
					String filename = "";

					FileSystemView dummyFsv  = new FileSystemView()
					{
						String newFolderString = UIManager.getString("FileChooser.other.newFolder");

						public File createNewFolder(File var1) throws IOException {
							if (var1 == null) {
								throw new IOException("Containing directory is null:");
							} else {
								File var2 = this.createFileObject(var1, newFolderString);
								if (var2.exists()) {
									throw new IOException("Directory already exists:" + var2.getAbsolutePath());
								} else {
									var2.mkdirs();
									return var2;
								}
							}
						}
					};

//	    			JFileChooser fc = new JFileChooser(filename, dummyFsv);
	    			JFileChooser fc = new JFileChooser(filename, rfsv);
//	    			JFileChooser fc = new JFileChooser(fsv);
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//					fc.setApproveButtonText("Choose");
					fc.setDialogTitle("SSH Remote Files at "+hostPortLabel+" (NOTE: This is NOT working in a good manner)");
//					fc.setAccessory( new JLabel("NOTE: This is NOT working in a good manner...") );
					
//					String str = _logFilename_txt.getText();
//					if (StringUtil.hasValue(str))
//						fc.setSelectedFile(new File(str));

	    			int returnVal = fc.showOpenDialog(this);
	    			if(returnVal == JFileChooser.APPROVE_OPTION)
	    			{
	    				// NOTE: well the FileChooser seems to return windows file path (if we run this on windows) so strip some stuff off
	    				String selectedFile = fc.getSelectedFile().getAbsolutePath();
	    				if (selectedFile.startsWith("c:") || selectedFile.startsWith("C:"))
	    					selectedFile = selectedFile.substring(2);
	    				selectedFile = selectedFile.replace('\\', '/');

	    				_output_txt.setText("Selected file: |" + selectedFile + "|\n");
//	    				_logFilename_txt.setText( selectedFile );
	    			}
				}
				catch (Exception e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		private void action_execute_short(ActionEvent e)
		{
			if (_conn == null)
			{
				_output_txt.setText("--- SORRY NOT YET CONNECTED...");
				return;
			}
			String command = _command1_txt.getText();
			_output_txt.setText("Executing Short command: |" + command + "|\n");

			try
			{
				ExecOutput out = _conn.execCommandOutput(command);
				_output_txt.append(out + "");
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		private void action_execute_stream(ActionEvent e)
		{
			if (_conn == null)
			{
				_output_txt.setText("--- SORRY NOT YET CONNECTED...");
				return;
			}
			String command = _command2_txt.getText();
			_output_txt.setText("Executing Streaming command: |" + command + "|\n");

			try
			{
				final ChannelExec channel = _conn.execCommand(command);
				
//				ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
//				ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
						int sleepCount          = 0;
						int sleepTimeMultiplier = 3; 
						int sleepTimeMax        = 250;

						try
						{
							InputStream in = channel.getInputStream();
							InputStream err = channel.getExtInputStream();

							int maxBufLen = 1024;
							byte[] tmpBuffer = new byte[maxBufLen];
							while (true)
							{
								while (in.available() > 0)
								{
									int i = in.read(tmpBuffer, 0, maxBufLen);
									if ( i < 0 )
										break;
//									outputBuffer.write(tmp, 0, i);
									String str = "STDOUT: " + new String(tmpBuffer, 0, i, "UTF-8");
//									System.out.println(str);
									_output_txt.append( str );
									_output_txt.setCaretPosition( _output_txt.getDocument().getLength() );
								}
								while (err.available() > 0)
								{
									int i = err.read(tmpBuffer, 0, maxBufLen);
									if ( i < 0 )
										break;
//									errorBuffer.write(tmp, 0, i);
									String str = "STDERR: " + new String(tmpBuffer, 0, i, "UTF-8");
									System.err.println(str);
									_output_txt.append( str );
									_output_txt.setCaretPosition( _output_txt.getDocument().getLength() );
								}
								if ( channel.isClosed() )
								{
									if ( (in.available() > 0) || (err.available() > 0) )
										continue;

									//System.out.println("exit-status: " + channel.getExitStatus());
									_output_txt.append("exit-status: " + channel.getExitStatus());
									_output_txt.setCaretPosition( _output_txt.getDocument().getLength() );
									break;
								}
								try
								{
									sleepCount++;
									int sleepMs = Math.min(sleepCount * sleepTimeMultiplier, sleepTimeMax);;

									if (_logger.isDebugEnabled())
										_logger.debug("waitForData(), sleep(" + sleepMs + "). command=|" + command + "|.");

									System.out.println("waitForData(), sleep(" + sleepMs + "). command=|" + command + "|.");
									
									Thread.sleep(sleepMs);
								}
								catch (Exception ee)
								{
									ee.printStackTrace();
								}
							}

							channel.disconnect();				
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				};
				t.start();

//				System.out.println("output: " + outputBuffer.toString("UTF-8"));
//				System.out.println("error: " + errorBuffer.toString("UTF-8"));

//				_output_txt.setText(out + "");
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		private SshConnection sshConnect()
		{
			final String user    = _username_txt.getText();
			final String passwd  = _password_txt.getText();
			final String host    = _hostname_txt.getText();
			final String portStr = "22";
			final String keyFile = _keyFile_txt.getText();

			int port = 22;
			try {port = Integer.parseInt(portStr);} 
			catch(NumberFormatException ignore) {}

System.out.println("sshConnect(): host='"+host+"', port='"+port+"', user='"+user+"', passwd='"+passwd+"', keyFile='"+keyFile+"'");
			
			final SshConnection sshConn = new SshConnection(host, port, user, passwd, keyFile);
			WaitForExecDialog wait = new WaitForExecDialog(this, "SSH Connecting to "+host+", with user "+user);
			sshConn.setWaitForDialog(wait);

			BgExecutor waitTask = new BgExecutor(wait)
			{
				@Override
				public Object doWork()
				{
					try
					{
						sshConn.connect();
					}
					catch (Exception e) 
					{
						SwingUtils.showErrorMessage("SSH Connect failed", "SSH Connection to "+host+":"+portStr+" with user '"+user+"' Failed.", e);
//						sshConn = null;
					}
					return null;
				}
			};
			wait.execAndWait(waitTask);
//			if (sshConn == null)
//				return false;
			if (sshConn.isConnected())
				return sshConn;
			else 
				return null;
		}
	}
}
