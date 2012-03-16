package com.asetune.gui.swing;

import javax.swing.JButton;

public interface DockUndockManagement
{
//	/** Sets the button that could be used to dock/undock */
//	public void setDockUndockButton(JButton button);

	/** 
	 * Get a button that should be used to dock/undock<p> 
	 * The default GUI rules will be applied for the button. 
	 * Default GUI = no text, no border, Icon is fetched using getWindow{Dock|Undock}Icon() 
	 */
	public JButton getDockUndockButton();

	/**
	 * called just before the component is docked back into the TabbedPane
	 * @return true if we allow the dock operation
	 */
	public boolean beforeDock();

	/**
	 * called after the component has been docked back into the TabbedPane
	 */
	public void afterDock();

	/**
	 * called just before the component is Undocked to its own frame
	 * @return true if we allow the undock operation
	 */
	public boolean beforeUndock();

	/**
	 * called after the component has been undocked to its own frame
	 */
	public void afterUndock();

	/**
	 * 
	 */
	public void saveWindowProps(GTabbedPaneWindowProps winProps);
	public GTabbedPaneWindowProps getWindowProps();
}
