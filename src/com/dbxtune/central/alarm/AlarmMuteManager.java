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
package com.dbxtune.central.alarm;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AppDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Singleton manager for alarm mute state.
 * Mutes are persisted as JSON in $DBXTUNE_CONF_DIR/alarm-mutes.json.
 * Thread-safe: uses ConcurrentHashMap with synchronized save().
 */
public class AlarmMuteManager
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final AlarmMuteManager _instance = new AlarmMuteManager();
	public  static AlarmMuteManager getInstance() { return _instance; }

	/** One mute record — also the JSON schema */
	public static class MuteRecord
	{
		public String alarmId;
		public String srvName;
		public String reason;
		public String mutedByUser;
		public String mutedTime;   // ISO-8601
		public String expiresAt;   // ISO-8601 or null (= permanent)

		public MuteRecord() {} // Jackson

		public boolean isExpired()
		{
			if (expiresAt == null) return false;
			try   { return Instant.parse(expiresAt).isBefore(Instant.now()); }
			catch (Exception e) { return false; }
		}
	}

	private final ConcurrentHashMap<String, MuteRecord> _mutes = new ConcurrentHashMap<>();
	private final ObjectMapper _om;
	private final File _file;

	private AlarmMuteManager()
	{
		_om   = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		_file = new File(AppDir.getAppConfDir() + File.separator + "alarm-mutes.json");
		load();
	}

	/** Load persisted mutes from disk, skipping any already-expired records. */
	private synchronized void load()
	{
		if (!_file.exists()) return;
		try
		{
			Map<String, MuteRecord> loaded = _om.readValue(_file,
				_om.getTypeFactory().constructMapType(HashMap.class, String.class, MuteRecord.class));
			if (loaded != null)
			{
				_mutes.clear();
				loaded.entrySet().stream()
				      .filter(e -> !e.getValue().isExpired())
				      .forEach(e -> _mutes.put(e.getKey(), e.getValue()));
			}
			_logger.info("AlarmMuteManager: loaded " + _mutes.size() + " mutes from " + _file);
		}
		catch (IOException e) { _logger.error("AlarmMuteManager: failed to load from " + _file, e); }
	}

	/** Persist current mutes to disk. */
	private synchronized void save()
	{
		try
		{
			_file.getParentFile().mkdirs();
			_om.writeValue(_file, new HashMap<>(_mutes));
		}
		catch (IOException e) { _logger.error("AlarmMuteManager: failed to save to " + _file, e); }
	}

	/** Add or update a mute. expiresHours=null or <=0 means permanent. */
	public void mute(String alarmId, String srvName, String reason, String mutedByUser, Integer expiresHours)
	{
		MuteRecord rec   = new MuteRecord();
		rec.alarmId      = alarmId;
		rec.srvName      = srvName;
		rec.reason       = reason;
		rec.mutedByUser  = (mutedByUser != null && !mutedByUser.isEmpty()) ? mutedByUser : "anonymous";
		rec.mutedTime    = Instant.now().toString();
		rec.expiresAt    = (expiresHours != null && expiresHours > 0)
		                   ? Instant.now().plus(expiresHours, ChronoUnit.HOURS).toString()
		                   : null;
		_mutes.put(alarmId, rec);
		save();
		_logger.info("AlarmMuteManager: muted alarmId=" + alarmId + " by=" + rec.mutedByUser
		           + (rec.expiresAt != null ? " expires=" + rec.expiresAt : " (permanent)"));
	}

	/** Remove a mute. */
	public void unmute(String alarmId)
	{
		if (_mutes.remove(alarmId) != null)
		{
			save();
			_logger.info("AlarmMuteManager: unmuted alarmId=" + alarmId);
		}
	}

	/** Return the MuteRecord for alarmId, or null if not muted (or expired). */
	public MuteRecord getMute(String alarmId)
	{
		MuteRecord rec = _mutes.get(alarmId);
		if (rec != null && rec.isExpired())
		{
			_mutes.remove(alarmId);
			save();
			return null;
		}
		return rec;
	}

	public boolean isMuted(String alarmId) { return getMute(alarmId) != null; }

	/** Return all non-expired mutes (immutable view). */
	public Map<String, MuteRecord> getAllMutes()
	{
		_mutes.entrySet().removeIf(e -> e.getValue().isExpired());
		return Collections.unmodifiableMap(_mutes);
	}
}
