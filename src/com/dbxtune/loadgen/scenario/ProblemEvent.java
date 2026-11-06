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
package com.dbxtune.loadgen.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.loadgen.Journal;
import com.dbxtune.loadgen.LgSession;

/**
 * What happened during one problem scenario event. Written to the journal as one "problem" line.
 */
public class ProblemEvent
{
	private final String _startTs = Journal.now();
	private String _endTs;
	private final List<Map<String, Object>> _sessions = new ArrayList<>();
	private final Map<String, Object> _details = new LinkedHashMap<>();
	private String _note;
	private String _error;

	/** Add a session that took part, with its role (holder, waiter, victim...) */
	public synchronized void addSession(LgSession s, String role)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("spid",    s.getSpid());
		m.put("login",   s.getLogin());
		m.put("appName", s.getAppName());
		m.put("role",    role);
		_sessions.add(m);
	}

	/** Free form details: the SQL, table, rows, durations... */
	public synchronized ProblemEvent put(String key, Object value) { _details.put(key, value); return this; }

	public void setNote (String note)  { _note  = note; }
	public void setError(String error) { _error = error; }
	public void end()                  { _endTs = Journal.now(); }

	public String getStartTs() { return _startTs; }
	public String getEndTs()   { return _endTs; }
	public String getError()   { return _error; }

	public synchronized Map<String, Object> toMap(String scenario, Map<String, Object> expected)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("ts",       Journal.now());
		m.put("type",     "problem");
		m.put("scenario", scenario);
		m.put("startTs",  _startTs);
		m.put("endTs",    _endTs);
		m.put("sessions", new ArrayList<>(_sessions));
		m.put("details",  new LinkedHashMap<>(_details));
		m.put("note",     _note);
		m.put("error",    _error);
		m.put("expected", expected);
		return m;
	}
}
