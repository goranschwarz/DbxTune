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
package com.asetune.sql.pipe;

/**
 * Debug, Info, Warning, Error message produced by a Pipe command
 */
public class PipeMessage
{
	public enum Severity
	{
		DEBUG, INFO, WARNING, ERROR
	};

	private String   _message;
	private Severity _severity;

	public PipeMessage(Severity severity, String message)
	{
		_severity = severity;
		_message  = message;
	}
	
	public String   getMessage()  { return _message; }
	public Severity getSeverity() { return _severity; }
	
	@Override
	public String toString() 
	{
		return new StringBuilder().append(_severity).append(": ").append(_message).toString();
	}
}
