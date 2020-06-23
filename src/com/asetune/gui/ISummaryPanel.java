/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

	public void setSummaryData(CountersModel cm, boolean postProcessing);

	public void setWatermark();

	public void saveLayoutProps();

	public void resetGoToTabSettings(String tabName);
	
//	public void setVisibility();
	public void setComponentProperties();
}
