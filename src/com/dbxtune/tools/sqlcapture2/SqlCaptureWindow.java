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
package com.dbxtune.tools.sqlcapture2;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.IGuiController;
import com.dbxtune.Version;
import com.dbxtune.gui.DbmsVersionPanelAbstract;
import com.dbxtune.gui.DbmsVersionPanelAse;
import com.dbxtune.gui.ShowCmPropertiesDialog;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class SqlCaptureWindow
extends JFrame 
implements IGuiController
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected GTabbedPane       _mainTabbedPanel              = new GTabbedPane();

	private ConnectionProvider _connectionProvider;
	private int _spid;
	private int _kpid;

	public SqlCaptureWindow()
	{
	}
	public SqlCaptureWindow(ConnectionProvider connProvider, int spid, int kpid)
	{
		_connectionProvider = connProvider;
		if (_connectionProvider == null)
			throw new RuntimeException("ProcessDetailFrame: The passed ConnectionProvider was null.");

		try
		{
			_spid = spid;
			_kpid = kpid;
			init();
		}
		catch (Exception e)
		{
			_logger.error("In constructor", e);
		}
	}

	@Override 
	public String getTablePopupDbmsVendorString() 
	{ 
		return "ase"; 
	}

	private void init()
	{
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/asetune_icon.gif");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}
		this.setTitle("Capture SQL");

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				//this_windowClosing(e);
			}
		});

		
		setLayout(new MigLayout("insets 0 0 0 0"));
//		add( new SqlCapturePanel(), "grow, push" );
		add(_mainTabbedPanel);

		ImageIcon sumTabIcon   = SwingUtils.readImageIcon(Version.class, "images/sum.png");
		ImageIcon stmntTabIcon = SwingUtils.readImageIcon(Version.class, "images/sqlStatements.png");

		_mainTabbedPanel.addTab("Statements",   stmntTabIcon, new SqlCapturePanel(), "Active SQL Statements that are executing right now, Historical SQL Stataments that has been executed previously");
		_mainTabbedPanel.addTab("SPID Details", sumTabIcon,   new SpidDetailsPanel(), "Counters for a specififc SPID");
		
		pack();
	}

//	private void this_windowClosing(WindowEvent e)
//	{
//		if (refressProcess != null)
//		{
//			refressProcess.stopRefresh();
//			saveProps();
//		}
//
////		if (processObjectsFrame != null) processObjectsFrame.dispose();
////		if (processWaitsFrame   != null) processWaitsFrame  .dispose();
////		if (processLocksFrame   != null) processLocksFrame  .dispose();
//	}

	//-----------------------------------------------------------------------
	// BEGIN: implement IGuiController
	//-----------------------------------------------------------------------
	@Override
	public boolean hasGUI()
	{
		return true;
	}

	@Override
	public void addPanel(JPanel panel)
	{
		if (panel instanceof TabularCntrPanel)
		{
			TabularCntrPanel tcp = (TabularCntrPanel) panel;
			_mainTabbedPanel.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		}
//		else if (panel instanceof ISummaryPanel)
//		{
//			setSummaryPanel( (ISummaryPanel)panel );
//		}
		else
		{
			_mainTabbedPanel.addTab(panel.getName(), null, panel, null);
		}
	}

	@Override
	public GTabbedPane getTabbedPane()
	{
		return _mainTabbedPanel;
	}

	@Override
	public void splashWindowProgress(String msg)
	{
	}

	@Override
	public Component getActiveTab()
	{
		return _mainTabbedPanel.getSelectedComponent();
	}

	@Override
	public void setStatus(int type)
	{
	}

	@Override
	public void setStatus(int type, String param)
	{
	}
	
	@Override
	public Component getGuiHandle()
	{
		return this;
	}

	@Override
	public DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog)
	{
		return new DbmsVersionPanelAse(showCmPropertiesDialog);
	}
	//-----------------------------------------------------------------------
	// END: implement IGuiController
	//-----------------------------------------------------------------------


	public static void main(String args[]) throws Exception
	{
		// FIXME: parse input parameters

		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		// Set configuration, right click menus are in there...
//		Configuration conf = new Configuration("c:\\projects\\dbxtune\\dbxtune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}

    	ConnectionProvider connProvider = new ConnectionProvider()
		{
			@Override
			public DbxConnection getNewConnection(String appname)
			{
				DbxConnection conn = null;
				try
				{
					Properties props = new Properties();
					props.put("CHARSET", "iso_1");
					Connection c = AseConnectionFactory.getConnection("192.168.0.110:1570", null, "sa", "sybase", Version.getAppName()+"-"+appname, Version.getVersionStr(), null, props, null);
					conn = DbxConnection.createDbxConnection(c);
				}
				catch (Exception e)
				{
					System.out.println("Problems connecting: " + AseConnectionUtils.sqlExceptionToString((SQLException)e));
//					AseConnectionUtils.sqlWarningToString(e);
//					throw e;
				}
				return conn;
			}
			
			@Override
			public DbxConnection getConnection()
			{
				return null;
			}
		};


		SqlCaptureWindow xxx = new SqlCaptureWindow(connProvider, -1, -1);
		xxx.setVisible(true);
	}	
}
