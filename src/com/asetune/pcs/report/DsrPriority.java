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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.pcs.report;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.central.pcs.objects.DbxCentralServerDescription;
import com.asetune.utils.Configuration;
import com.asetune.utils.LoggingConsole;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class DsrPriority
implements AutoCloseable
{
	private static Logger _logger = Logger.getLogger(DsrPriority.class);

	public static final String  PROPKEY_priorityEnabled                 = "DailySummaryReport.priority.enabled";
	public static final boolean DEFAULT_priorityEnabled                 = true;

	public static final String  PROPKEY_priorityDisabledServernameRegex = "DailySummaryReport.priority.disabled.servername.regex";
	public static final String  DEFAULT_priorityDisabledServernameRegex = "";

	public static final String  PROPKEY_priorityFile                    = "DailySummaryReport.priority.file";
	public static final String  DEFAULT_priorityFile                    = "${tmpDir}/DbxTune/DbxCentral/DailySummaryReport.priority";

	public static final String  PROPKEY_priorityMaxWaitTimeMinutes      = "DailySummaryReport.priority.max.wait.time.minutes";
	public static final int     DEFAULT_priorityMaxWaitTimeMinutes      = 60;

	public static final String  PROPKEY_prioritySleepTimeSeconds        = "DailySummaryReport.priority.sleep.time.seconds";
	public static final int     DEFAULT_prioritySleepTimeSeconds        = 10;

	public static final String  PROPKEY_priorityCoreCountParallelFactor = "DailySummaryReport.priority.core.count.parallel.factor";
//	public static final double  DEFAULT_priorityCoreCountParallelFactor = 0.4; // this is "modest" and probably to under-allocated
//	public static final double  DEFAULT_priorityCoreCountParallelFactor = 0.6; // slightly under-allocated
	public static final double  DEFAULT_priorityCoreCountParallelFactor = 0.7; // probably the right choice
//	public static final double  DEFAULT_priorityCoreCountParallelFactor = 0.8; // probably the right choice
//	public static final double  DEFAULT_priorityCoreCountParallelFactor = 1.0; // 1 to 1 (but we are using more thread so this will over-allocate)
//	public static final double  DEFAULT_priorityCoreCountParallelFactor = 1.2; // more over-allocation

	
	
	private RandomAccessFile _raf;
	private FileChannel _channel;
	private FileLock _lock;
	private long _lockCallTime;
	private long _lockSuccessTime;
	private long _lockWaitTimeMs;

	private PriorityList _priorityList;

	
	public enum State
	{
		NOT_STARTED, 
		RUNNING, 
		FINISHED
	};
	
	
	public DsrPriority()
	{
	}

	/**
	 * Opens the Progress file. An EXLUSIVE lock will be held on the file until it's closed<br>
	 * Typical usage is:
	 * <pre>
	 *     try (DsrPriority dsrPriority = DsrPriority.createAndOpen())
	 *     {
	 *         // Do "stuff" with the: dsrPriority object
	 *         // the 'dsrPriority object' is auto closed, due to 'try-with-resources'
	 *     }
	 *     catch (IOException ex)
	 *     {
	 *         // Handle exception
	 *     }
	 * </pre>
	 * 
	 * @return a newly created DsrPriority object
	 * @throws IOException 
	 */
	public static DsrPriority createAndOpen() 
	throws IOException
	{
		DsrPriority dsrPriority = new DsrPriority();
		
		dsrPriority.open();

		return dsrPriority;
	}

	/**
	 * Opens the Progress file. An EXLUSIVE lock will be held on the file until it's closed<br>
	 * 
	 * @return the DsrPriority object we are opening
	 * @throws IOException
	 */
	private DsrPriority open() 
	throws IOException
	{
		String progresFileName = getProgresFilename();
		
		_raf = new RandomAccessFile(progresFileName, "rw");
		_channel = _raf.getChannel();

		// Lock the file in EXCLUSIVE mode (wait for others to release there locks before continuing)
		_lockCallTime = System.currentTimeMillis();
		// LOCK
		_lock = _channel.lock(); // NOTE: This may block... 
		// statistics
		_lockSuccessTime = System.currentTimeMillis();
		_lockWaitTimeMs = _lockSuccessTime - _lockCallTime;

		if (_lockWaitTimeMs > 500)
		{
			_logger.info("Waited for exclusive file lock on file '" + progresFileName + "' for " + _lockWaitTimeMs + " ms.");
		}

		// Read the information
		ObjectMapper mapper = new ObjectMapper();
		_priorityList = mapper.readValue(_raf, PriorityList.class);
		
		return this;
	}


//	@Override
//	public void close() 
//	throws IOException
//	{
//		if (_lock    != null) _lock   .release();
//		if (_channel != null) _channel.close();
//		if (_raf     != null) _raf    .close();
//	}

	@Override
	public void close() 
	{
		if (_lock    != null) try { _lock   .release(); } catch(IOException ex) { _logger.warn("Problems closing: _lock"   , ex); }
		if (_channel != null) try { _channel.close();   } catch(IOException ex) { _logger.warn("Problems closing: _channel", ex); }
		if (_raf     != null) try { _raf    .close();   } catch(IOException ex) { _logger.warn("Problems closing: _raf"    , ex); }
	}
	

	/**
	 * Save a JSON structure of the DsrPriority object
	 * 
	 * @throws IOException
	 */
	private void savePriorityFile()
	throws IOException
	{
		_raf.seek(0);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(_raf, _priorityList);

		// Is the "end-of-the-file" overwritten or kept???
	}

//	private void readPriorityFile()
//	throws Exception
//	{
//		ObjectMapper mapper = new ObjectMapper();
//		_priorityList = mapper.readValue(_raf, PriorityList.class);
//	}

	/**
	 * Set a State for a specific server
	 * @param serverName
	 * @param state
	 */
	private void setState(String serverName, State state)
	{
		boolean entryFound = false;
		for (PriorityEntry entry : _priorityList.getPriorityList())
		{
			if (serverName.equals(entry.getServerName()))
			{
				entryFound = true;
				entry.setState(state);
				
				if (State.RUNNING.equals(state))
				{
//					entry.setStartTime(new Timestamp(System.currentTimeMillis()));
					entry.setStartTime(new Timestamp(System.currentTimeMillis()) + "");
				}

				if (State.FINISHED.equals(state))
				{
//					entry.setEndTime(new Timestamp(System.currentTimeMillis()));
					entry.setEndTime(new Timestamp(System.currentTimeMillis()) + "");
				}
			}
		}
		
		if ( ! entryFound )
		{
			_logger.warn("When calling the internal method setState(" + state + "). The servername '" + serverName + "' was not found.");
			//throw new NameNotFoundException("The servername '" + serverName + "' was not found.");
		}
	}


	@SuppressWarnings("unused")
	@JsonPropertyOrder(value = {"coreCount", "parallelDegree", "priorityList"}, alphabetic = true)
	private static class PriorityList
	{
		List<PriorityEntry> _priorityList = new ArrayList<>();

		int _coreCount;
		int _parallelDegree;

		public PriorityList()
		{
			double factor = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_priorityCoreCountParallelFactor, DEFAULT_priorityCoreCountParallelFactor);
			
			_coreCount = Runtime.getRuntime().availableProcessors();
			_parallelDegree = (int) Math.round( _coreCount * factor );
			
			// CoreCount degree must be at least 1
			if (_coreCount < 1) 
				_coreCount = 1;

			// Parallel degree must be at least 1
			if (_parallelDegree < 1)
				_parallelDegree = 1;
		}

		/* ---------- GET methods ---------- */
		public List<PriorityEntry> getPriorityList()
		{ 
			return _priorityList; 
		}
		public int getCoreCount()
		{ 
			// CoreCount degree must be at least 1
			if (_coreCount < 1) 
				_coreCount = 1;

			return _coreCount; 
		}
		public int getParallelDegree() 
		{
			// Parallel degree must be at least 1
			if (_parallelDegree < 1) 
				_parallelDegree = 1;

			return _parallelDegree; 
		}

		/* ---------- SET methods ---------- */
		public void setPriorityList(List<PriorityEntry> priorityList)
		{
			_priorityList = priorityList; 
		}
		public void setCoreCount(int coreCount)
		{
			_coreCount = coreCount; 

			// CoreCount degree must be at least 1
			if (_coreCount < 1) 
				_coreCount = 1;
		}
		public void setParallelDegree(int parallelDegree)               
		{ 
			_parallelDegree = parallelDegree; 

			// Parallel degree must be at least 1
			if (_parallelDegree < 1) 
				_parallelDegree = 1;
		}

		/* ---------- OTHER methods ---------- */
		public String toJson() 
		throws JsonProcessingException
		{
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			return mapper.writeValueAsString(this);
		}

		public void addServer(String srvName)
		{
			PriorityEntry entry = new PriorityEntry();

			entry._priorityOrder = _priorityList.size() + 1;
			entry._serverName    = srvName;
			entry._state         = State.NOT_STARTED;
			entry._startTime     = null;
			entry._endTime       = null;

			_priorityList.add(entry);
		}
		
		public PriorityEntry getServerEntry(String serverName)
		{
			for (PriorityEntry entry : _priorityList)
			{
				if (serverName.equals(entry.getServerName()))
				{
					return entry;
				}
			}
			return null;
		}
	}

	@SuppressWarnings("unused")
	@JsonPropertyOrder(value = {"priorityOrder", "serverName", "state", "startTime", "endTime", "duration"}, alphabetic = true)
	private static class PriorityEntry
	{
		int       _priorityOrder;
		String    _serverName;
		State     _state;
//		Timestamp _startTime;
//		Timestamp _endTime;
		String    _startTime;
		String    _endTime;
		String    _duration;

		public int       getPriorityOrder() { return _priorityOrder; }
		public String    getServerName()    { return _serverName; }
		public State     getState()         { return _state; }
//		public Timestamp getStartTime()     { return _startTime; }
//		public Timestamp getEndTime()       { return _endTime; }
		public String    getStartTime()     { return _startTime; }
		public String    getEndTime()       { return _endTime; }
		public String    getDuration()      { return _duration; }

		public void setPriorityOrder(int       priorityOrder) { _priorityOrder = priorityOrder; }
		public void setServerName   (String    serverName   ) { _serverName    = serverName;    }
		public void setState        (State     state        ) { _state         = state;         }
//		public void setStartTime    (Timestamp startTime    ) { _startTime     = startTime;     }
//		public void setEndTime      (Timestamp endTime      ) { _endTime       = endTime;       }
		public void setStartTime    (String    startTime    ) { _startTime     = startTime;     }
//		public void setEndTime      (String    endTime      ) { _endTime       = endTime;       }
		public void setDuration     (String    duration     ) { _duration      = duration;      }
		public void setEndTime(String endTime)
		{
			_endTime  = endTime;

			if (StringUtil.hasValue(_startTime) && StringUtil.hasValue(_endTime))
			{
				try
				{
					Timestamp startTs = TimeUtils.parseToTimestamp(_startTime);
					Timestamp endTs   = TimeUtils.parseToTimestamp(_endTime);
					
					long durationMs = endTs.getTime() - startTs.getTime();
					_duration = TimeUtils.msToTimeStrDHMS(durationMs);
				}
				catch (Exception ex) { /* ignore */ }
			}
		}
	}


	
	/*
	 * ==================================================================================
	 * Below are static methods...
	 * ==================================================================================
	 */
	

	/**
	 * Check if the DSR Priority is enabled or disabled
	 * 
	 * @param serverName
	 * @return
	 */
	public static boolean isEnabled(String serverName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		// GLOBAL enabled/disabled
		boolean isEnabled = conf.getBooleanProperty(PROPKEY_priorityEnabled, DEFAULT_priorityEnabled);

		// If not GLOBALY Disabled, it might be disabled for a specific server.
		if (isEnabled)
		{
			String disabledServernameRegex = conf.getProperty(PROPKEY_priorityDisabledServernameRegex, DEFAULT_priorityDisabledServernameRegex);
			
			if (StringUtil.hasValue(serverName) && StringUtil.hasValue(disabledServernameRegex))
			{
				if (serverName.matches(disabledServernameRegex))
				{
					_logger.info("Daily Summary Report Priority, the servername='" + serverName + "', matches the disabledServernameRegex='" + disabledServernameRegex + "'.");
					isEnabled = false;
				}
			}
		}

		return isEnabled;
	}

	
	/**
	 * Get name of the Daily Summary Report PROGRESS file
	 * @return
	 */
	public static String getProgresFilename()
	{
		// Using RAW so we do NOT replace ${tmpDir} with blank... 
		String filename = Configuration.getCombinedConfiguration().getPropertyRaw(PROPKEY_priorityFile, DEFAULT_priorityFile);

		// replace '${tmpDir}'
		filename = filename.replace("${tmpDir}", System.getProperty("java.io.tmpdir"));

		return filename;
	}

	/**
	 * Called from a scheduler or similar (from DbxCentral), to create a NEW file that will be used by any of the collectors that will create a Daily Summary Report
	 */
	public static void createOrReplaceProgresFile()
	{
		String progresFilename = getProgresFilename();

		String filename = StringUtil.hasValue(DbxTuneCentral.getAppConfDir()) ? DbxTuneCentral.getAppConfDir() + "/SERVER_LIST" : "conf/SERVER_LIST";
		File srvListFile = new File(filename);
		if (srvListFile.exists())
		{
			Map<String, DbxCentralServerDescription> map = new HashMap<>();
			try
			{
				map = DbxCentralServerDescription.getFromFile(filename);
			}
			catch (IOException ex)
			{
				_logger.warn("Problems reading file '" + srvListFile + "'. This is used to sort the 'sessions list'. Skipping this... Caught: "+ex);
			}

			if ( ! map.isEmpty() )
			{
				PriorityList prioList = new PriorityList();
				
				// for each serverName in the list, add to
				for (String srvName : map.keySet())
				{
					// Should we remove "some" servers that may not be "local" (on the same machine as DbxCentral)
					// well all the Collectors in the 'SERVER_LIST' file *should* be local...

					prioList.addServer(srvName);
				}
				
				// First remove the file ???
				// This so: If some old process is still there, the file will get a new i-node (at least in windows)
				// NOTE: this will be good on Linux, but will it work on Windows???)
				File progresFile = new File(progresFilename);
				if (progresFile.exists())
				{
					progresFile.delete();
				}

				// Write the file
				try
				{
					String json = prioList.toJson();
					FileUtils.write(progresFile, json, StandardCharsets.UTF_8);

					_logger.info("Success writing a new DSR Progress File '" + progresFile + "'.");
				}
				catch (Exception ex)
				{
					_logger.error("Problems writing a new DSR Progress File '" + progresFile + "', in method: createOrReplaceProgresFile()", ex);
				}
			}
		}
		else
		{
			_logger.info("Creation of Daily Summary Report Priority File '" + progresFilename + "' will not be done. SERVER_LIST file '" + srvListFile + "' did not exist.");
		}

	}
	
	
	public static boolean waitforOtherDsr(String serverName)
	{
		int maxWaitTimeMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_priorityMaxWaitTimeMinutes, DEFAULT_priorityMaxWaitTimeMinutes);
		int maxWaitTimeSeconds = maxWaitTimeMinutes * 60;
		
		try
		{
			return waitforOtherDsr(serverName, maxWaitTimeSeconds);
		}
		catch(TimeoutException ex)
		{
			_logger.error("Problems 'TimeoutException' when waiting for other Daily Summary Report Priority for serverName='" + serverName + "'. Continuing anyway.", ex);
			return false;
		}
		catch(InterruptedException ex)
		{
			_logger.error("Problems 'InterruptedException' when waiting for other Daily Summary Report Priority for serverName='" + serverName + "'. Continuing anyway.", ex);
			return false;
		}
	}
	
	public static boolean waitforOtherDsr(String serverName, int maxWaitTimeSeconds)
	throws TimeoutException, InterruptedException
	{
		File progressFile = new File(getProgresFilename());
		if ( ! progressFile.exists() )
		{
			_logger.info("Skipping 'waitforOtherDsr(serverName=" + serverName + ")' due to that the Progress file did NOT exist. progressFile='" + progressFile.getAbsolutePath() + "'.");
			return true;
		}
		
		boolean granted = false;

		// Take a start time so we can timeout after 'maxWaitTimeSeconds'
		long startTime = System.currentTimeMillis();

		while (granted == false)
		{
			int inStateNotStarted = 0;
			int inStateRunning    = 0;
			int inStateFinished   = 0;
			
			int parallelDegree = -1;
			int srvNamePriorityOrder = -1;
			
			List<String> waitInfoServerList = new ArrayList<>();
			
//			try (DsrPriority dsrPriority = new DsrPriority().open())
			try (DsrPriority dsrPriority = DsrPriority.createAndOpen())
			{
				PriorityEntry srvEntry = dsrPriority._priorityList.getServerEntry(serverName);
				if (srvEntry == null)
				{
					// NOTE: Should we GRANT if it's not found... or should we wait for "everybody else"
					return true; 
				}

				// Get this servers order in the priority file
				srvNamePriorityOrder = srvEntry.getPriorityOrder();
				
				for (PriorityEntry entry : dsrPriority._priorityList.getPriorityList())
				{
					if (entry.getPriorityOrder() < srvNamePriorityOrder)
					{
						if      (State.NOT_STARTED.equals(entry.getState())) { inStateNotStarted++; }
						else if (State.RUNNING    .equals(entry.getState())) { inStateRunning++;    }
						else if (State.FINISHED   .equals(entry.getState())) { inStateFinished++;   }
						
						waitInfoServerList.add("{order=" + entry.getPriorityOrder() + ", name='" + entry.getServerName() + "', state='" + entry.getState() + "', startTime='" + entry.getStartTime() + "'}");
					}
				}

				// if we want to simulate holding the EXLUSIVE file lock
				//Thread.sleep(10_000);
				
				// if we are allowed to continue
				parallelDegree = dsrPriority._priorityList.getParallelDegree();
				int conSidderedStatesCount = inStateNotStarted + inStateRunning;
				if (conSidderedStatesCount < parallelDegree)
				{
					granted = true;
				}
				
				if (granted)
				{
					// Set the new STATE
					dsrPriority.setState(serverName, State.RUNNING);

					// Save the file... when we leave this try block, the FILE LOCK is also released. 
					dsrPriority.savePriorityFile();
					
					long secondsWaited = TimeUtils.secondsDiffNow(startTime);
					_logger.info("Granted DSR Priority for serverName='" + serverName + "', we waited " + secondsWaited + " seconds before we were granted access.");
					return granted;
				}
			}
			catch (IOException ex)
			{
				_logger.error("Problems when checking 'granted=" + granted + "' for other Daily Summary Report Priority for serverName='" + serverName + "'. Continuing with next iteration check...", ex);
			}
			
			if ( ! granted )
			{
				long secondsWaited = TimeUtils.secondsDiffNow(startTime);
				if (secondsWaited > maxWaitTimeSeconds)
				{
					throw new TimeoutException("Timed out waiting on DSR Priority for serverName='" + serverName + "'. secondsWaited=" + secondsWaited + ", maxWaitTimeSeconds=" + maxWaitTimeSeconds + ", waitInfoServerList=" + waitInfoServerList);
				}
				else
				{
					int sleepTimeInSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_prioritySleepTimeSeconds, DEFAULT_prioritySleepTimeSeconds);
					
					_logger.info("Waiting for DSR Priority before we can continue for serverName='" + serverName + "', parallelDegree=" + parallelDegree + ", srvNamePriorityOrder=" + srvNamePriorityOrder + ", inStateNotStarted=" + inStateNotStarted + ", inStateRunning=" + inStateRunning + ", inStateFinished=" + inStateFinished + ". Sleeping for " + sleepTimeInSec + " seconds before next attempt. WaitInfo: secondsWaited=" + secondsWaited + ", maxWaitTimeSeconds=" + maxWaitTimeSeconds + ", waitInfoServerList=" + waitInfoServerList);
					Thread.sleep(sleepTimeInSec * 1000);
				}
			}
		}
		return granted;
	}

	/**
	 * Set the state to finished!
	 * 
	 * @param serverName
	 */
	public static void setStateFinnished(String serverName)
//	throws TimeoutException, InterruptedException
	{
		File progressFile = new File(getProgresFilename());
		if ( ! progressFile.exists() )
		{
			_logger.info("Skipping 'setStateFinnished(serverName=" + serverName + ")' due to that the Progress file did NOT exist. progressFile='" + progressFile.getAbsolutePath() + "'.");
			return;
		}

//		try (DsrPriority dsrPriority = new DsrPriority().open())
		try (DsrPriority dsrPriority = DsrPriority.createAndOpen())
		{
			// Set the new STATE
			dsrPriority.setState(serverName, State.FINISHED);
			
			// Save the file... when we leave this try block, the FILE LOCK is also released. 
			dsrPriority.savePriorityFile();

			_logger.info("Success when setting Daily Summary Report Priority to 'FINISHED' for serverName='" + serverName + "'.");
		}
		catch (Exception ex)
		{
			_logger.error("Problems when setting Daily Summary Report Priority to 'FINISHED' for serverName='" + serverName + "'. Continuing anyway.", ex);
		}
	}
	

	
	
	
	
	public static void main_xxx(String[] args)
	{
		PriorityList pl = new PriorityList();
//		pl._coreCount = 4;
//		pl._parallelDegree = 3;
		pl.addServer("PROD_A1_ASE");
		pl.addServer("PROD_REP");
		pl.addServer("PROD_B1_ASE");
		pl.addServer("prod-a1-mssql");
		pl.addServer("prod-b1-mssql");
		
		try
		{
			System.out.println("JSON=|" + pl.toJson() + "|");
		}
		catch (JsonProcessingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		LoggingConsole.init(false, false);

		System.out.println("Usage: dsr_priority_test create|start|stop [servername]");

		for (int i=0; i<args.length; i++)
			System.out.println(" - Params[" + i + "]: |" + args[i]+ "|.");

		if (args.length <= 0)
			System.exit(1);
		
		String cmd     = args[0].toLowerCase();
		String srvName = args.length > 1 ? args[1] : null;

		System.out.println("-----------------------------------");
		System.out.println("cmd='"     + cmd     + "'.");
		System.out.println("srvName='" + srvName + "'.");
		System.out.println("-----------------------------------");

		System.out.println("ProgressFile=|" + DsrPriority.getProgresFilename() + "|.");
		
		if ("create".equals(cmd))
		{
			DsrPriority.createOrReplaceProgresFile();
		}
		else if ("start".equals(cmd))
		{
			System.out.println(">>>> START: -WAIT- FOR DSR ");
			boolean ok = DsrPriority.waitforOtherDsr(srvName);
			System.out.println("<<<< START: -WAIT- FOR DSR ... ok=" + ok);
		}
		else if ("stop".equals(cmd))
		{
			System.out.println(">>>> STOP: -WAIT- FOR DSR ");
			DsrPriority.setStateFinnished(srvName);
			System.out.println("<<<< STOP: -WAIT- FOR DSR ");
		}
		else
		{
			System.out.println("UNKNOWN COMMAND '" + cmd + "'.");
		}
	}

	// -----------------------------------------------------
	// The Priority file looks like the following
	// -----------------------------------------------------
	//
	// #After 'creation'
	// -------------------
	//  {
	//    "coreCount" : 2,
	//    "parallelDegree" : 2,
	//    "priorityList" : [ 
	//      {"priorityOrder" : 1, "serverName" : "PROD_A1_ASE",   "state" : "NOT_STARTED", "startTime" : null, "endTime" : null}
	//     ,{"priorityOrder" : 2, "serverName" : "PROD_REP",      "state" : "NOT_STARTED", "startTime" : null, "endTime" : null}
	//     ,{"priorityOrder" : 3, "serverName" : "PROD_B1_ASE",   "state" : "NOT_STARTED", "startTime" : null, "endTime" : null}
	//     ,{"priorityOrder" : 4, "serverName" : "prod-a1-mssql", "state" : "NOT_STARTED", "startTime" : null, "endTime" : null} 
	//     ,{"priorityOrder" : 5, "serverName" : "prod-b1-mssql", "state" : "NOT_STARTED", "startTime" : null, "endTime" : null} 
	//    ]
	//  }
	// 
	// # When 2 first are running
	// ----------------------------
	//  {
	//    "coreCount" : 2,
	//    "parallelDegree" : 2,
	//    "priorityList" : [ 
	//      {"priorityOrder" : 1, "serverName" : "PROD_A1_ASE",   "state" : "RUNNING",     "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : null}
	//     ,{"priorityOrder" : 2, "serverName" : "PROD_REP",      "state" : "RUNNING",     "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : null}
	//     ,{"priorityOrder" : 3, "serverName" : "PROD_B1_ASE",   "state" : "NOT_STARTED", "startTime" : null,                  "endTime" : null}
	//     ,{"priorityOrder" : 4, "serverName" : "prod-a1-mssql", "state" : "NOT_STARTED", "startTime" : null,                  "endTime" : null} 
	//     ,{"priorityOrder" : 5, "serverName" : "prod-b1-mssql", "state" : "NOT_STARTED", "startTime" : null,                  "endTime" : null} 
	//    ]
	//  }
	// 
	// # When 2 first are done and next 2 servers are running
	// ----------------------------------------------------------
	//  {
	//    "coreCount" : 2,
	//    "parallelDegree" : 2,
	//    "priorityList" : [ 
	//      {"priorityOrder" : 1, "serverName" : "PROD_A1_ASE",   "state" : "FINISHED",    "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : "YYYY-MM-DD hh:mm:ss"}
	//     ,{"priorityOrder" : 2, "serverName" : "PROD_REP",      "state" : "FINISHED",    "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : "YYYY-MM-DD hh:mm:ss"}
	//     ,{"priorityOrder" : 3, "serverName" : "PROD_B1_ASE",   "state" : "RUNNING",     "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : null}
	//     ,{"priorityOrder" : 4, "serverName" : "prod-a1-mssql", "state" : "RUNNING",     "startTime" : "YYYY-MM-DD hh:mm:ss", "endTime" : null} 
	//     ,{"priorityOrder" : 5, "serverName" : "prod-b1-mssql", "state" : "NOT_STARTED", "startTime" : null,                  "endTime" : null} 
	//    ]
	//  }
}
