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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.config.dict;

import com.dbxtune.sql.conn.DbxConnection;


public class MonTablesDictionaryMySql
extends MonTablesDictionaryDefault
{
    /** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(MonTablesDictionaryMySql.class);

	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		// TODO Auto-generated method stub
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
	 */
	public static void initExtraMonTablesDictionary()
	{
//		try
//		{
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			
//			if (mtd == null)
//				return;
//
//			// NOTE: the below information on all MON_* tables are copied from: http://xxxx
//
//			//---------------------------------------------------------------------------------------------------------------
//			// tablename
//			//---------------------------------------------------------------------------------------------------------------
//			mtd.addTable("t1",            "describeme");
//
//			mtd.addColumn("t1", "c1",            "describeme");
//			mtd.addColumn("t1", "c2",            "describeme");
//
//			
//
//			//---------------------------------------------------------------------------------------------------------------
//			// tablename
//			//---------------------------------------------------------------------------------------------------------------
//			mtd.addTable("t2",            "describeme");
//
//			mtd.addColumn("t2", "c1",            "describeme");
//			mtd.addColumn("t2", "c2",            "describeme");
//
//		
//		}
//		catch (NameNotFoundException e)
//		{
//			/* ignore */
//		}
	}
}
