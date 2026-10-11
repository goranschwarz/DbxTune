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
/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.config.dict;

import java.lang.invoke.MethodHandles;

import javax.naming.NameNotFoundException;

import com.dbxtune.sql.conn.DbxConnection;


public class MonTablesDictionaryOracle
extends MonTablesDictionaryDefault
{
	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		super.initialize(conn, hasGui);

		initExtraMonTablesDictionary();
	}


	/**
	 * NO, do not save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return false;
	}

	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 * <p>
	 * Oracle has no "dictionary" with descriptions for the V$ views, so the descriptions for the views we use is maintained here.<br>
	 * Columns that are "calculated" by a CM is described in the CM itself, see: addMonTableDictForVersion()
	 */
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			if (mtd == null)
				return;

			//---------------------------------------------------------------------------------------------------------------
			// DBA_TABLESPACES
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("DBA_TABLESPACES",  "Describes all tablespaces in the database.");

			mtd.addColumn("DBA_TABLESPACES", "TABLESPACE_NAME",  "Name of the tablespace");
			mtd.addColumn("DBA_TABLESPACES", "BLOCK_SIZE",       "Tablespace block size (in bytes)");
			mtd.addColumn("DBA_TABLESPACES", "STATUS",           "Tablespace status: ONLINE, OFFLINE or READ ONLY");
			mtd.addColumn("DBA_TABLESPACES", "CONTENTS",         "Tablespace contents: PERMANENT, TEMPORARY or UNDO");
			mtd.addColumn("DBA_TABLESPACES", "BIGFILE",          "Indicates whether the tablespace is a bigfile tablespace (YES) or a smallfile tablespace (NO). A bigfile tablespace has exactly ONE data file.");

			//---------------------------------------------------------------------------------------------------------------
			// DBA_TABLESPACE_USAGE_METRICS
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("DBA_TABLESPACE_USAGE_METRICS",  "Describes tablespace usage metrics for all types of tablespaces, including permanent, temporary, and undo tablespaces.");

			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "TABLESPACE_NAME",  "Name of the tablespace");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "USED_SPACE",       "Used space, in database blocks");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "TABLESPACE_SIZE",  "Tablespace size, in database blocks. If a data file is AUTOEXTENSIBLE this is the maximum size it can grow to.");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "USED_PERCENT",     "Percentage of used space, as a function of the maximum possible tablespace size");
		}
		catch (NameNotFoundException e)
		{
			System.out.println("Problems in cm='" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
