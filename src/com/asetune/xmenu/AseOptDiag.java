/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.asetune.Version;
import com.asetune.gui.LineNumberedPaper;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.OSCommand;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.SwingWorker;


/**
 * @author gorans
 */
public class AseOptDiag 
extends XmenuActionBase 
{
	private String      _dbname  = null;
	private String      _tabname = null;

	/**
	 * 
	 */
	public AseOptDiag() 
	{
		super();
	}

	/* (non-Javadoc)
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	@Override 
	public void doWork() 
	{
		_dbname  = getParamValue(0);
		_tabname = getParamValue(1);

		showCallStack();
	}

	public void execOptDiag(JFrame frame, JTextArea textarea)
	{
		String aseSrv    = AseConnectionFactory.getServer();
		String aseUser   = AseConnectionFactory.getUser();
		String asePasswd = AseConnectionFactory.getPassword();

		String cmdPath  = "";
//		String optdiag  = Configuration.getInstance(Configuration.CONF).getProperty("optdiag", "${SYBASE}/${SYBASE_ASE}/bin/optdiag");
		Configuration conf = Configuration.getCombinedConfiguration();
		String optdiag  = conf.getProperty("optdiag", "${SYBASE}/${SYBASE_ASE}/bin/optdiag");

		String cmd = cmdPath + optdiag + " statistics " + _dbname + ".." + _tabname + " -U"+aseUser + " -P"+asePasswd + " -S"+aseSrv;
		//optdiag statistics goran..TestDest_1 -Usa -P -Sgoransxp:5000
		
		textarea.setText("Executing OS Command: "+cmd+"\nWaiting for it to finnish...");
		try
		{
			OSCommand osCmd = OSCommand.execute(cmd);
			String retVal = osCmd.getOutput();

			textarea.setText(cmd + "\n\n" + retVal);
		}
		catch (Exception e)
		{
			textarea.setText("Problems executing the Operating system command:\n"+cmd+"\n\n"+e.getMessage());
			SwingUtils.showErrorMessage(frame, "Problems Executing OptDiag", 
					"Problems executing the Operating system command:\n\n" +
					cmd+"\n\n"+e.getMessage()+"\n\n" +
					"If the optdiag binary cant be found, you can specify what binary to run using the\n" +
					"property 'optdiag=...' In the "+Version.getAppName()+" properties file '"+Configuration.getCombinedConfiguration().getFilename()+"'.\n\n"
					, e);
			frame.dispose();
		}
	}

	public void showCallStack()
	{
		JPanel textPanel = new JPanel();
		//final JTextArea procText = new JTextArea();
		final JTextArea textarea  = new LineNumberedPaper(0,0);
		final JFrame textFrame = new JFrame("Optdiag for "+_dbname+".."+_tabname);

		textFrame.addWindowListener(new WindowAdapter()
		{
			@Override 
			public void windowClosing(WindowEvent e)
			{
			}
		});

		ActionListener action = new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				textFrame.dispose();
			}
		};

		JScrollPane scrollPane = new JScrollPane(textarea);
		textPanel.setLayout(new BorderLayout());
		textarea.setBackground(Color.white);
		textarea.setEnabled(true);
		textarea.setEditable(false);

		textPanel.add("Center", scrollPane);
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(action);
		buttonPanel.add(closeButton);
		textPanel.add("South",buttonPanel); 
		textFrame.getContentPane().add("Center", textPanel);
		textFrame.setSize(800, 800);

		SwingWorker w = new SwingWorker()
		{
			@Override 
			public Object construct()
	        {
				execOptDiag(textFrame, textarea);
	            return null;
	        }			
		};
		w.start();
		
		textFrame.setVisible(true);
	}

}
