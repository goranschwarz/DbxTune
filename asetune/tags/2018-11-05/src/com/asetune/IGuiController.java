package com.asetune;

import java.awt.Component;

import javax.swing.JPanel;

import com.asetune.gui.swing.GTabbedPane;

public interface IGuiController
{

	boolean hasGUI();

	void addPanel(JPanel panel);
//	void addPanel(TabularCntrPanel panel);
//	void addPanel(ISummaryPanel panel);

	public Component   getActiveTab();
	public GTabbedPane getTabbedPane();
	
	public void splashWindowProgress(String msg);

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 */
	public void setStatus(int type);

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @param param The actual string to set (this is only used for <code>ST_STATUS_FIELD</code>)
	 */
	public void setStatus(int type, String param);
	
	/**
	 * Get GUI "main" window, so it can be used for various message windows etc
	 * @return
	 */
	public Component getGuiHandle();

	/**
	 * A part of the properties entry key to get valid entries for the Table right click popup menu.
	 * @return for example: "ase", "iq", "rs", "hana", "oracle", "sqlserver"
	 */
	public String getTablePopupDbmsVendorString();
}
