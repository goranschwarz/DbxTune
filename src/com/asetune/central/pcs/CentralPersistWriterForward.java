/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.central.pcs;

import java.sql.Timestamp;

import com.asetune.central.pcs.CentralPcsWriterHandler.NotificationType;
import com.asetune.central.pcs.DbxTuneSample.CmEntry;
import com.asetune.utils.Configuration;

public class CentralPersistWriterForward
implements ICentralPersistWriter
{

	@Override
	public void init(Configuration props) throws Exception
	{
	}

	@Override
	public void close()
	{
	}

	@Override
	public Configuration getConfig()
	{
		return null;
	}

	@Override
	public String getConfigStr()
	{
		return null;
	}

	@Override
	public void printConfig()
	{
	}

	@Override
	public boolean isSessionStarted(String sessionName)
	{
		return false;
	}

	@Override
	public void setSessionStarted(String sessionName, boolean isSessionStarted)
	{
	}

	@Override
	public void setSessionStartTime(String sessionName, Timestamp sessionStartTime)
	{
	}

	@Override
	public Timestamp getSessionStartTime(String sessionName)
	{
		return null;
	}

	@Override
	public void startSession(String sessionName, DbxTuneSample cont)
	{
	}

	@Override
	public void saveSample(DbxTuneSample cont)
	{
		// SEND THE CONTAINER: somewhere else
	}

	@Override
	public void saveCounters(CmEntry cme)
	{
	}

	@Override
	public boolean beginOfSample(DbxTuneSample cont)
	{
		return true;
	}

	@Override
	public void endOfSample(DbxTuneSample cont, boolean caughtErrors)
	{
	}

	@Override
	public void notification(NotificationType type, String str)
	{
	}

	@Override
	public void startServices() throws Exception
	{
	}

	@Override
	public void stopServices(int maxWaitTimeInMs)
	{
	}

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public void storageQueueSizeWarning(int queueSize, int thresholdSize)
	{
	}

	@Override
	public CentralPcsWriterStatistics getStatistics()
	{
		return null;
	}

	@Override
	public void resetCounters()
	{
	}

}
