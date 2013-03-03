package com.asetune;

import javax.swing.JPanel;

public interface IGuiController
{

	boolean hasGUI();

	void addPanel(JPanel panel);
//	void addPanel(TabularCntrPanel panel);
//	void addPanel(ISummaryPanel panel);

	public void splashWindowProgress(String msg);

}
