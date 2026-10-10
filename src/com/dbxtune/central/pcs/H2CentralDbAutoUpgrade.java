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
package com.dbxtune.central.pcs;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.H2CentralDbCopy3.DbType;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.H2FileFormat;
import com.dbxtune.utils.H2FileFormat.OldFormatAction;
import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.StringUtil;

/**
 * Called at DbxCentral startup, <b>before</b> the Central database is opened.
 * <p>
 * If the DbxCentral H2 database file is written by an older H2 version (that the current H2 can't read),
 * it is upgraded by copying it into a new file (see {@link H2DbFileUpgrader}).
 * By default the original file is kept as a backup.
 */
public class H2CentralDbAutoUpgrade
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Prefix for all properties: action, keepBackup, spaceFactor, spaceMarginMb, jar.format.N */
	public static final String  PROPKEY_prefix        = "DbxTuneCentral.h2.oldFormat.";

	public static final String  PROPKEY_action        = PROPKEY_prefix + "action";
	public static final OldFormatAction DEFAULT_action = OldFormatAction.COPY_UPGRADE;

	public static final String  PROPKEY_keepBackup    = PROPKEY_prefix + "keepBackup";
	public static final boolean DEFAULT_keepBackup    = true;

	/**
	 * Check the Central H2 database file format, and upgrade it if needed.
	 *
	 * @throws Exception if the database can't be used (and we could not upgrade it), DbxCentral should NOT continue to start
	 */
	public static void checkAndUpgrade(Configuration conf)
	throws Exception
	{
		String url = StringUtil.envVariableSubstitution(conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL, CentralPersistWriterJdbc.DEFAULT_JDBC_URL));
		if (url == null || ! url.startsWith("jdbc:h2:file:"))
			return;

		File dbFile = new H2UrlHelper(url).getDbFile(true);
		if (dbFile == null)
			return;

		if ( ! dbFile.exists() )
		{
			File pageStoreFile = new File(dbFile.getAbsolutePath().replace(H2FileFormat.MV_DB_SUFFIX, ".h2.db"));
			if (pageStoreFile.exists())
				_logger.error("Found an old H2 'PageStore' database file '" + pageStoreFile + "' (H2 1.3 or earlier). This can NOT be upgraded automatically.");
			return;
		}

		_logger.info(H2FileFormat.describe(dbFile));

		if ( ! H2FileFormat.needsUpgrade(dbFile) )
			return;

		int fileFormat = H2FileFormat.readFormat(dbFile);
		String manualHint = "The database can be upgraded manually with: dbxtune.sh h2upgrade -f '" + dbFile + "' -e";

		// What should we do
		OldFormatAction action = H2FileFormat.parseAction(conf.getProperty(PROPKEY_action), DEFAULT_action);
		_logger.info("The H2 database file '" + dbFile + "' has format " + fileFormat + " which can't be read by H2 " + H2FileFormat.getCurrentH2Version() + ". Action: " + PROPKEY_action + "=" + action);

		if (OldFormatAction.ERROR.equals(action))
		{
			String msg = "The DbxCentral H2 database file '" + dbFile + "' is written by an older H2 version (format " + fileFormat + ") and property '" + PROPKEY_action + "' is '" + action + "'. "
					+ manualHint + "  Or set '" + PROPKEY_action + "=" + OldFormatAction.COPY_UPGRADE + "' to upgrade it automatically at startup.";
			_logger.error(msg);
			throw new Exception(msg);
		}

		// Do the upgrade
		H2DbFileUpgrader.Options opt = H2DbFileUpgrader.Options.fromConfig(conf, PROPKEY_prefix, DEFAULT_keepBackup);
		opt.user   = conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME, "sa");
		opt.passwd = conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD, "");

		H2DbFileUpgrader.Result res = H2DbFileUpgrader.upgrade(dbFile, DbType.DBXCENTRAL, opt);
		if ( ! res.isOk() )
		{
			String msg = "DbxCentral can NOT start, the H2 database upgrade failed (" + res.status + "): " + res.message + " " + manualHint;
			_logger.error(msg);
			throw new Exception(msg);
		}
	}
}
