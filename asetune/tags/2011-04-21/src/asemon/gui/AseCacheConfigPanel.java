package asemon.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import asemon.AseCacheConfig;

public class AseCacheConfigPanel
extends JPanel
{
	private static final long serialVersionUID = 1L;

	private JTextArea	_cacheConfig           = new JTextArea();
	private JScrollPane _cacheConfigScroll     = new JScrollPane(_cacheConfig);


	public AseCacheConfigPanel()
	{
		setLayout( new BorderLayout() );
		add(_cacheConfigScroll, BorderLayout.CENTER);
		
		init();
	}


	private void init()
	{
		AseCacheConfig aseCacheConfig = AseCacheConfig.getInstance();
		if ( aseCacheConfig.isInitialized() )
		{
			_cacheConfig.setText( aseCacheConfig.getConfig() );
		}
		else
		{
			_cacheConfig.setText( "Not yet Initialized, please connect first." );
		}
	}
}
