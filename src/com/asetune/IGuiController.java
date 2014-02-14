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

}
