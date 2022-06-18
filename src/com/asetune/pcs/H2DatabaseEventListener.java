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
package com.asetune.pcs;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.h2.api.DatabaseEventListener;

public class H2DatabaseEventListener implements DatabaseEventListener
{
	private static Logger _logger = Logger.getLogger(H2DatabaseEventListener.class);

	/**
	 * This method is called just after creating the object.
	 * This is done when opening the database if the listener is specified
	 * in the database URL, but may be later if the listener is set at
	 * runtime with the SET SQL statement.
	 *
	 * @param url - the database URL
	 */
	@Override
	public void init(String url)
	{
//_logger.setLevel(Level.DEBUG);
		_logger.info ("H2DatabaseEventListener.init(): url='"+url+"'.");
		_logger.debug("H2DatabaseEventListener.init(): url='"+url+"'.");
	}

	/**
	 * This method is called after the database has been opened.
	 * It is save to connect to the database and execute statements at this point.
	 */
	@Override
	public void opened()
	{
		_logger.info ("H2DatabaseEventListener.opened()");
		_logger.debug("H2DatabaseEventListener.opened()");
	}

//	/**
//	 * This method is called if the disk space is very low. One strategy is to
//	 * inform the user and wait for it to clean up disk space. The database
//	 * should not be accessed from within this method (even to close it).
//	 * 
//	 * hmmm, this was removed in H2 1.3.163
//	 */
//	@Override
//	public void diskSpaceIsLow()
//	{
//		_logger.info("H2 Database Event Listener, diskSpaceIsLow()");
//
//		if (PersistentCounterHandler.hasInstance())
//		{
//			if (AseTune.hasGUI())
//			{
//				String htmlMsg =
//					"<html>" +
//						"Running out of disk space for the H2 database (Persistent Counter Storage)<br>" +
//						"<br>" +
//						"<b>STOPPING</b> the Persistent Counter Handler<br>" +
//						"No more Performance Counters will be stored, you need to:" +
//						"<ul>" +
//						"   <li>Disconnect</li>" +
//						"   <li>Free up some disk space</li>" +
//						"   <li>Connect again</li>" +
//						"</ul>" +
//					"</html>";
//
//				// OK, this is non-modal, but the OK button doesnt work, fix this later, and use the X on the window instead
//				JOptionPane optionPane = new JOptionPane(htmlMsg, JOptionPane.WARNING_MESSAGE);
//				JDialog dialog = optionPane.createDialog(null, "H2 running out of disk space");
//				dialog.setModal(false);
//				dialog.setVisible(true);
////				// Needs to be non blocking...
////				SwingUtils.showWarnMessage("H2 running out of disk space", htmlMsg, null);
//
//				_logger.warn("Running out of disk space for the H2 database (Persistent Counter Storage), Stopping the PCS Writer(s).");
//			}
//			else
//			{
//				_logger.warn("Running out of disk space for the H2 database (Persistent Counter Storage), Stopping the PCS Writer(s).");
//			}
//			
//			// Then STOP the service
//			// NOTE: This needs it's own Thread, otherwise it's the PersistCounterHandler thread
//			//       that will issue the shutdown, meaning store() will be "put on hold"
//			//       until this method call is done, and continue from that point. 
//			Runnable shutdownPcs = new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					_logger.info("Issuing STOP on the Persistent Counter Storage Handler");
//					PersistentCounterHandler.getInstance().stop(true, 0);
//					PersistentCounterHandler.setInstance(null);					
//				}
//			};
//			Thread shutdownThread = new Thread(shutdownPcs);
//			shutdownThread.setDaemon(true);
//			shutdownThread.setName("H2DbEvent:DiskSpace:StopPcs");
//			shutdownThread.start();
//
//			// Just sleep for a short while (more or less to contect switch)
//			//try { Thread.sleep(100); }
//			//catch(InterruptedException ignore) {}
//		}
//	}

	/**
	 * This method is called if an exception occurred.
	 *
	 * @param e the exception
	 * @param sql the SQL statement
	 */
	@Override
	public void exceptionThrown(SQLException ex, String sql)
	{
		_logger.info ("H2DatabaseEventListener.exceptionThrown(): SQLException="+ex+", arg1='"+sql+"'.", ex);
		_logger.debug("H2DatabaseEventListener.exceptionThrown(): SQLException="+ex+", arg1='"+sql+"'.", ex);
	}

	// temporary backward compatibility for H2 older version: 1.4.*
	public void setProgress(int state, String name, int x, int max)
	{
		setProgress(state, name, (long)x, (long)max);
	}

	/**
	 * This method is called for long running events, such as recovering,
	 * scanning a file or building an index.
	 *
	 * @param state the state
	 * @param name the object name
	 * @param x the current position
	 * @param max the highest value
	 */
//	@Override // temporary removed the @Override when compiling with H2 1.4 & 2.1 in the classpath
	public void setProgress(int state, String name, long x, long max)
	{
//		int STATE_SCAN_FILE          = 0; // This state is used when scanning the database file.
//		int STATE_CREATE_INDEX       = 1; // This state is used when re-creating an index.
//		int STATE_RECOVER            = 2; // This state is used when re-applying the transaction log or rolling back uncommitted transactions.
//		int STATE_BACKUP_FILE        = 3; // This state is used during the BACKUP command.
//		int STATE_RECONNECTED        = 4; // This state is used after re-connecting to a database (if auto-reconnect is enabled).
//		int STATE_STATEMENT_START    = 5; // This state is used when a query starts.
//		int STATE_STATEMENT_END      = 6; // This state is used when a query ends.
//		int STATE_STATEMENT_PROGRESS = 7; // This state is used for periodic notification during long-running queries.

		switch (state)
		{
    		case DatabaseEventListener.STATE_STATEMENT_START:      return;
    		case DatabaseEventListener.STATE_STATEMENT_PROGRESS:   return;
    		case DatabaseEventListener.STATE_STATEMENT_END:        return;
    		case DatabaseEventListener.STATE_CREATE_INDEX:         return;
		}
		
		_logger.info ("H2DatabaseEventListener.setProgress(): state="+stateToString(state)+", at='"+x+"', max='"+max+"', name='"+name+"'.");
		_logger.debug("H2DatabaseEventListener.setProgress(): state="+stateToString(state)+", at='"+x+"', max='"+max+"', name='"+name+"'.");
	}
	
	private String stateToString(int state)
	{
		switch (state)
		{
            case DatabaseEventListener.STATE_SCAN_FILE          : return "SCAN_FILE";          // This state is used when scanning the database file.
            case DatabaseEventListener.STATE_CREATE_INDEX       : return "CREATE_INDEX";       // This state is used when re-creating an index.
            case DatabaseEventListener.STATE_RECOVER            : return "RECOVER";            // This state is used when re-applying the transaction log or rolling back uncommitted transactions.
            case DatabaseEventListener.STATE_BACKUP_FILE        : return "BACKUP_FILE";        // This state is used during the BACKUP command.
            case DatabaseEventListener.STATE_RECONNECTED        : return "RECONNECTED";        // This state is used after re-connecting to a database (if auto-reconnect is enabled).
            case DatabaseEventListener.STATE_STATEMENT_START    : return "STATEMENT_START";    // This state is used when a query starts.
            case DatabaseEventListener.STATE_STATEMENT_END      : return "STATEMENT_END";      // This state is used when a query ends.
            case DatabaseEventListener.STATE_STATEMENT_PROGRESS : return "STATEMENT_PROGRESS"; // This state is used for periodic notification during long-running queries.
		}
		return "UnknownState[" + state + "]";
	}

	/**
	 * This method is called before the database is closed normally. It is save
	 * to connect to the database and execute statements at this point, however
	 * the connection must be closed before the method returns.
	 */
	@Override
	public void closingDatabase()
	{
		_logger.info ("H2DatabaseEventListener.closingDatabase()");
		_logger.debug("H2DatabaseEventListener.closingDatabase()");
	}

}
