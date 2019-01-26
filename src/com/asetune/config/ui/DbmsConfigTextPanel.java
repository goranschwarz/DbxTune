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
package com.asetune.config.ui;

import java.awt.BorderLayout;
import java.sql.Timestamp;

import javax.swing.JPanel;

import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.DbxTune;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.pcs.PersistReader;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.ConnectionProvider;


public class DbmsConfigTextPanel
extends JPanel
{
	private static final long serialVersionUID = 1L;

//	private ConfigType   _type                = null;
	private String      _name                 = null;
	private IDbmsConfigText _dbmsConfigText   = null;
//	private JTextArea	_textConfig           = new JTextArea();
//	private JScrollPane _textConfigScroll     = new JScrollPane(_textConfig);

//	private RSyntaxTextArea _textConfig       = new RSyntaxTextArea();
	private RSyntaxTextAreaX _textConfig      = new RSyntaxTextAreaX();
	private RTextScrollPane _textConfigScroll = new RTextScrollPane(_textConfig, true);

	private ConnectionProvider     _connProvider    = null;

//	public DbmsConfigTextPanel(ConnectionProvider connProvider, ConfigType type)
//	{
//		_connProvider = connProvider;
//		_type = type;
//
//		setLayout( new BorderLayout() );
//		add(_textConfigScroll, BorderLayout.CENTER);
//		
//		init();
//	}

	public DbmsConfigTextPanel(DbmsConfigViewDialog connProvider, IDbmsConfigText dbmsConfigText)
	{
		_connProvider   = connProvider;
		_dbmsConfigText = dbmsConfigText;
		_name           = dbmsConfigText.getName();

		setLayout( new BorderLayout() );
		add(_textConfigScroll, BorderLayout.CENTER);
		
		init();
	}

	public void refresh()
	throws Exception
	{
		Timestamp     ts        = null;
		boolean       hasGui    = DbxTune.hasGui();
		boolean       isOffline = false;
		DbxConnection conn      = null;

//		if (GetCounters.getInstance().isMonConnected())
//		{
//			ts        = null;
//			isOffline = false;
//			conn      = GetCounters.getInstance().getMonConnection();
//		}
//		else
//		{
//			ts        = null; // NOTE: this will not work, get the value from somewhere
//			isOffline = true;
//			conn      = PersistReader.getInstance().getConnection();
//		}
		conn = _connProvider.getConnection();

		if (PersistReader.hasInstance())
		{
			if (PersistReader.getInstance().isConnected())
			{
				ts        = null; // NOTE: this will not work, get the value from somewhere
				isOffline = true;
			}
		}
		
//		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
////		aseConfigText.refresh(conn, ts);
//		aseConfigText.initialize(conn, hasGui, isOffline, ts);
		_dbmsConfigText.initialize(conn, hasGui, isOffline, ts);

		// refresh when the configuration was taken.
//		_textConfig.setText( aseConfigText.getConfig() );
		_textConfig.setText( _dbmsConfigText.getConfig() );


		// and set input to "top" so it's a bit more readable if it's a long text
		_textConfig.setCaretPosition(0);
	}

	private void init()
	{
//		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
//		if ( aseConfigText.isInitialized() )
//		{
//			_textConfig.setText( aseConfigText.getConfig() );
//
//			// and set input to "top" so it's a bit more readable if it's a long text
//			_textConfig.setCaretPosition(0);
//		}
		if ( _dbmsConfigText.isInitialized() )
		{
			_textConfig.setSyntaxEditingStyle(_dbmsConfigText.getSyntaxEditingStyle());

			_textConfig.setText( _dbmsConfigText.getConfig() );

			// and set input to "top" so it's a bit more readable if it's a long text
			_textConfig.setCaretPosition(0);
		}
		else
		{
			// NOTE: if offline and we are reading an "older" database which didn't have the configs
			//       the below String will still show...
			//       I think this is a small issue, which I'm not fixing for the moment...
			_textConfig.setText( "Not yet Initialized, please connect first." );
		}
	}
}
