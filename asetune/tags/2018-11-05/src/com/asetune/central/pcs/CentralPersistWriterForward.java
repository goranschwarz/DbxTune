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
