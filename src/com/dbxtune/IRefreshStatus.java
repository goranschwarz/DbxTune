/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune;

/**
 * What is the collector doing right now (like the status bar in the Swing GUI).
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@link RefreshStatusGui}   - writes to the MainFrame status bar fields</li>
 *   <li>{@link RefreshStatusNoGui} - keeps the values in memory and pushes them to DbxCentral when a sample takes long time</li>
 * </ul>
 * The collector loop decides what implementation to use, see {@link ICounterController#setRefreshStatus(IRefreshStatus)}
 */
public interface IRefreshStatus
{
	/**
	 * What the collector does right now, for example: "Refreshing... CmSummary"<br>
	 * GUI: The status bar field (MainFrame.ST_STATUS_FIELD)
	 */
	void setStatus(String status);

	/**
	 * Details of the current step, for example: "for db 'xxx'"<br>
	 * GUI: The second status bar field (MainFrame.ST_STATUS2_FIELD)
	 */
	void setSubStatus(String subStatus);

	/** Used when no collector loop has set any implementation (for example in DbxCentral or SQL Window) */
	IRefreshStatus NO_OP = new IRefreshStatus()
	{
		@Override public void setStatus   (String status)    {}
		@Override public void setSubStatus(String subStatus) {}
	};
}
