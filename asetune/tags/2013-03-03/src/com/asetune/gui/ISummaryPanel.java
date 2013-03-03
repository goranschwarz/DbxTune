package com.asetune.gui;

import javax.swing.Icon;
import javax.swing.event.TableModelListener;

import com.asetune.cm.CountersModel;

public interface ISummaryPanel
extends TableModelListener //, GTabbedPane.ShowProperties
{

	public String getPanelName();

	public Icon getIcon();

	public CountersModel getCm();

	public String getDescription();

	public int getClusterView();

	public void setWatermarkText(String msgShort);

	public void addTrendGraph(TrendGraph tg);

	public TrendGraphDashboardPanel getGraphPanel();

	public void setLocalServerName(String string);

	public void clearSummaryData();

	public void clearGraph();

	public void setSummaryData(CountersModel cm);

	public void setWatermark();

	public void saveLayoutProps();

	public void resetGoToTabSettings(String tabName);

}
