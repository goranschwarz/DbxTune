package com.asetune.gui;

import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.utils.SwingUtils;

public class ConnectionProfileCatalog
{
	private String _name;
	public static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/conn_profile_directory_eclipse_16.png");

	public ConnectionProfileCatalog(String name)
	{
		_name = name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public String getName()
	{
		return _name;
	}
	
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
