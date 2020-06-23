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

public interface ConnectionProgressCallback
{
	public static final int TASK_STATUS_CURRENT     = 0;
	public static final int TASK_STATUS_SUCCEEDED   = 1;
	public static final int TASK_STATUS_SKIPPED     = 2;
	public static final int TASK_STATUS_FAILED      = 3;
	public static final int TASK_STATUS_FAILED_LAST = 4;

	public static final int FINAL_STATUS_SUCCEEDED   = 1;
	public static final int FINAL_STATUS_FAILED      = 2;
	
	public void setTaskStatus(String taskName, int status);
	public void setTaskStatus(String taskName, int status, Object infoObj);

	public void setFinalStatus(int status);
	public void setFinalStatus(int status, Object infoObj);
}
