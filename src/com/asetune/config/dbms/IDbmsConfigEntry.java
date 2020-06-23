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
package com.asetune.config.dbms;

public interface IDbmsConfigEntry
{
	/**
	 * @return The Name of this Configuration
	 */
	String getConfigKey(); 

	/**
	 * @return The configures value as a String representation
	 */
	String getConfigValue();
	
	/**
	 * @return If the Config Parameter belongs to a Section, return the name... 
	 */
	String getSectionName();
	
	/**
	 * @return  Check if the Config variable is pending for a restart to take affect
	 */
	boolean isPending();

	/**
	 * @return Check if the Config value is a NON-DEFAULT value (Changes from the default config value)
	 */
	boolean isNonDefault();
}
