package com.asetune.gui;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.Timestamp;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.asetune.AseConfigText;
import com.asetune.AseConfigText.ConfigType;
import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.pcs.PersistReader;


public class AseConfigTextPanel
extends JPanel
{
	private static final long serialVersionUID = 1L;

	private ConfigType   _type                = null;
	private JTextArea	_textConfig           = new JTextArea();
	private JScrollPane _textConfigScroll     = new JScrollPane(_textConfig);


	public AseConfigTextPanel(ConfigType type)
	{
		_type = type;

		setLayout( new BorderLayout() );
		add(_textConfigScroll, BorderLayout.CENTER);
		
		init();
	}

	public void refresh()
	{
		Timestamp  ts        = null;
		boolean    hasGui    = AseTune.hasGUI();
		boolean    isOffline = false;
		Connection conn      = null;

		if (GetCounters.getInstance().isMonConnected())
		{
			ts        = null;
			isOffline = false;
			conn      = GetCounters.getInstance().getMonConnection();
		}
		else
		{
			ts        = null; // NOTE: this will not work, get the value from somewhere
			isOffline = true;
			conn      = PersistReader.getInstance().getConnection();
		}

		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
//		aseConfigText.refresh(conn, ts);
		aseConfigText.initialize(conn, hasGui, isOffline, ts);

		// refresh when the configuration was taken.
		_textConfig.setText( aseConfigText.getConfig() );
	}

	private void init()
	{
		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
		if ( aseConfigText.isInitialized() )
		{
			_textConfig.setText( aseConfigText.getConfig() );
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
