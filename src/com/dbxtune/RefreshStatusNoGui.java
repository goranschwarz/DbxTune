/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.IPersistWriter;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.pcs.PersistWriterToHttpJson;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NO-GUI: Keep what the collector is doing in memory, and push it to DbxCentral when a sample takes long time.
 * <p>
 * DbxCentral forwards the message to the web browsers (graph.html) on the WebSocket '/api/chart/broadcast-ws',
 * so you can see what the collector is refreshing right now.
 * <p>
 * A background thread checks every second:
 * <ul>
 *   <li>If the current sample has been running for more than {@link #PROPKEY_pushThresholdSec} seconds:
 *       push the status when it changes, and every {@link #HEARTBEAT_MS} ms if it has not changed (so the browser knows we are alive).</li>
 *   <li>When that sample ends: push a "refreshing=false" message, so the browser can remove the status.</li>
 * </ul>
 * The message is sent with {@link PersistWriterToHttpJson#sendRefreshStatus(String)} (also PersistWriterToDbxCentral), the writer that sends the counters to DbxCentral.
 * If no such writer is used, nothing is pushed.
 */
public class RefreshStatusNoGui
implements IRefreshStatus
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_pushEnabled      = "RefreshStatusNoGui.push.enabled";
	public static final boolean DEFAULT_pushEnabled      = true;

	public static final String  PROPKEY_pushThresholdSec = "RefreshStatusNoGui.push.thresholdSec";
	public static final int     DEFAULT_pushThresholdSec = 5;

	/** Push an unchanged status this often, so the browser knows we are still alive */
	private static final long HEARTBEAT_MS = 5_000;

	private static final ObjectMapper _mapper = new ObjectMapper();

	// Set by the collector thread, read by the push thread
	private volatile String _status        = "";
	private volatile long   _statusTime    = System.currentTimeMillis();
	private volatile String _subStatus     = "";
	private volatile long   _subStatusTime = System.currentTimeMillis();

	private final int _sampleIntervalSec;
	private ScheduledExecutorService _pushExecutor;

	// Only used by the push thread
	private String _lastPushedStatus      = null;
	private String _lastPushedSubStatus   = null;
	private long   _lastPushTime          = 0;
	private long    _pushedSampleStartTime = 0; // 0 = nothing has been pushed for the current sample
	private boolean _noWriterLogged        = false;

	/**
	 * @param sampleIntervalSec Configured sleep time between samples (only passed on to the browser)
	 */
	public RefreshStatusNoGui(int sampleIntervalSec)
	{
		_sampleIntervalSec = sampleIntervalSec;
	}

	@Override
	public void setStatus(String status)
	{
		long now = System.currentTimeMillis();

		// A new step: forget the details of the previous step (if a CM fails, it can't leave an old "for db 'xxx'" behind)
		_subStatus     = "";
		_subStatusTime = now;

		_status        = status == null ? "" : status;
		_statusTime    = now;
	}

	@Override
	public void setSubStatus(String subStatus)
	{
		_subStatus     = subStatus == null ? "" : subStatus;
		_subStatusTime = System.currentTimeMillis();
	}

	/** Start the thread that pushes the status to DbxCentral */
	public void startPushThread()
	{
		if ( ! Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_pushEnabled, DEFAULT_pushEnabled) )
		{
			_logger.info("Push of 'refresh status' to DbxCentral is disabled. (" + PROPKEY_pushEnabled + " = false)");
			return;
		}

		_pushExecutor = Executors.newSingleThreadScheduledExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "RefreshStatusPusher");
			thread.setDaemon(true);
			return thread;
		});
		_pushExecutor.scheduleWithFixedDelay(this::checkAndPush, 1, 1, TimeUnit.SECONDS);

		_logger.info("Started thread 'RefreshStatusPusher', which pushes 'refresh status' to DbxCentral when a sample takes more than " + getThresholdSec() + " seconds. (" + PROPKEY_pushThresholdSec + ")");
	}

	/** Stop the thread that pushes the status to DbxCentral */
	public void stopPushThread()
	{
		if (_pushExecutor != null)
			_pushExecutor.shutdownNow();

		_pushExecutor = null;
	}

	/** Read it every time, so it can be changed while we are running */
	private int getThresholdSec()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_pushThresholdSec, DEFAULT_pushThresholdSec);
	}

	/** Called every second by the push thread */
	private void checkAndPush()
	{
		try
		{
			if ( ! CounterController.hasInstance() )
				return;

			ICounterController cc = CounterController.getInstance();

			long    now             = System.currentTimeMillis();
			boolean refreshing      = cc.isRefreshing();
			long    sampleStartTime = cc.getRefreshStartTime();

			// The sample we have pushed status for has ended: tell the browser(s) to remove the status
			if (_pushedSampleStartTime != 0 && ( ! refreshing || sampleStartTime != _pushedSampleStartTime) )
			{
				push(cc, false, cc.getLastRefreshTimeInMs(), "", 0, "", 0);

				_pushedSampleStartTime = 0;
				_lastPushedStatus      = null;
				_lastPushedSubStatus   = null;
			}

			if ( ! refreshing )
				return;

			long sampleMs = now - sampleStartTime;
			if (sampleMs < getThresholdSec() * 1000L)
				return;

			String  status    = _status;
			String  subStatus = _subStatus;
			boolean changed   = ! status.equals(_lastPushedStatus) || ! subStatus.equals(_lastPushedSubStatus);

			if (changed || now - _lastPushTime >= HEARTBEAT_MS)
			{
				if (push(cc, true, sampleMs, status, now - _statusTime, subStatus, now - _subStatusTime))
				{
					_pushedSampleStartTime = sampleStartTime;
					_lastPushedStatus      = status;
					_lastPushedSubStatus   = subStatus;
					_lastPushTime          = now;
				}
			}
		}
		catch (Throwable t)
		{
			_logger.debug("Problems in 'RefreshStatusPusher'. Caught: " + t, t);
		}
	}

	/**
	 * Create the JSON message and send it to DbxCentral
	 * @return true if it was sent
	 */
	private boolean push(ICounterController cc, boolean refreshing, long sampleMs, String status, long statusMs, String subStatus, long subStatusMs)
	throws Exception
	{
		// Same name as DbxCentral uses for the "session" (and the browser subscribes to)
		HeaderInfo headerInfo = cc.getLastKnownHeaderInfo();
		if (headerInfo == null || StringUtil.isNullOrBlank(headerInfo.getServerNameOrAlias()))
			return false;

		if ( ! PersistentCounterHandler.hasInstance() )
			return false;

		Map<String, Object> msg = new LinkedHashMap<>();
		msg.put("type"             , "refreshStatus");
		msg.put("serverName"       , headerInfo.getServerNameOrAlias());
		msg.put("refreshing"       , refreshing);
		msg.put("status"           , status);
		msg.put("statusMs"         , statusMs);
		msg.put("subStatus"        , subStatus);
		msg.put("subStatusMs"      , subStatusMs);
		msg.put("sampleMs"         , sampleMs);
		msg.put("sampleIntervalSec", _sampleIntervalSec);

		String json = _mapper.writeValueAsString(msg);

		// Send it with the writer that sends the counters to DbxCentral:
		// PersistWriterToDbxCentral (type 'http') or PersistWriterToHttpJson (URL '.../api/pcs/receiver'), first one that accepts it
		int httpWriters = 0;
		for (IPersistWriter pw : PersistentCounterHandler.getInstance().getWriters())
		{
			if (pw instanceof PersistWriterToHttpJson)
			{
				httpWriters++;
				if (((PersistWriterToHttpJson) pw).sendRefreshStatus(json))
					return true;
			}
		}

		// Tell once (otherwise it's hard to know why nothing shows up in the browser)
		if (httpWriters == 0 && ! _noWriterLogged)
		{
			_noWriterLogged = true;
			_logger.info("No 'PersistWriterToDbxCentral' or 'PersistWriterToHttpJson' writer is used, so 'refresh status' for long running samples can NOT be sent to DbxCentral.");
		}
		return false;
	}
}
