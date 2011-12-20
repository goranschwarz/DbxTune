package com.asetune.pcs;

import java.sql.SQLException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.h2.api.DatabaseEventListener;

import com.asetune.AseTune;

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
_logger.setLevel(Level.DEBUG);
		_logger.debug("init(): url='"+url+"'.");
	}

	/**
	 * This method is called after the database has been opened.
	 * It is save to connect to the database and execute statements at this point.
	 */
	@Override
	public void opened()
	{
		_logger.debug("opened()");
	}

	/**
	 * This method is called if the disk space is very low. One strategy is to
	 * inform the user and wait for it to clean up disk space. The database
	 * should not be accessed from within this method (even to close it).
	 */
	@Override
	public void diskSpaceIsLow()
	{
		_logger.info("H2 Database Event Listener, diskSpaceIsLow()");

		if (PersistentCounterHandler.hasInstance())
		{
			if (AseTune.hasGUI())
			{
				String htmlMsg =
					"<html>" +
						"Running out of disk space for the H2 database (Persistent Counter Storage)<br>" +
						"<br>" +
						"<b>STOPPING</b> the Persistent Counter Handler<br>" +
						"No more Performance Counters will be stored, you need to:" +
						"<ul>" +
						"   <li>Disconnect</li>" +
						"   <li>Free up some disk space</li>" +
						"   <li>Connect again</li>" +
						"</ul>" +
					"</html>";

				// OK, this is non-modal, but the OK button doesnt work, fix this later, and use the X on the window instead
				JOptionPane optionPane = new JOptionPane(htmlMsg, JOptionPane.WARNING_MESSAGE);
				JDialog dialog = optionPane.createDialog(null, "H2 running out of disk space");
				dialog.setModal(false);
				dialog.setVisible(true);
//				// Needs to be non blocking...
//				SwingUtils.showWarnMessage("H2 running out of disk space", htmlMsg, null);

				_logger.warn("Running out of disk space for the H2 database (Persistent Counter Storage), Stopping the PCS Writer(s).");
			}
			else
			{
				_logger.warn("Running out of disk space for the H2 database (Persistent Counter Storage), Stopping the PCS Writer(s).");
			}
			
			// Then STOP the service
			// NOTE: This needs it's own Thread, otherwise it's the PersistCounterHandler thread
			//       that will issue the shutdown, meaning store() will be "put on hold"
			//       until this method call is done, and continue from that point. 
			Runnable shutdownPcs = new Runnable()
			{
				@Override
				public void run()
				{
					_logger.info("Issuing STOP on the Persistent Counter Storage Handler");
					PersistentCounterHandler.getInstance().stop(true, 0);
					PersistentCounterHandler.setInstance(null);					
				}
			};
			Thread shutdownThread = new Thread(shutdownPcs);
			shutdownThread.setDaemon(true);
			shutdownThread.setName("H2DbEvent:DiskSpace:StopPcs");
			shutdownThread.start();

			// Just sleep for a short while (more or less to contect switch)
			//try { Thread.sleep(100); }
			//catch(InterruptedException ignore) {}
		}
	}

	/**
	 * This method is called if an exception occurred.
	 *
	 * @param e the exception
	 * @param sql the SQL statement
	 */
	@Override
	public void exceptionThrown(SQLException ex, String sql)
	{
		_logger.debug("exceptionThrown(): SQLException="+ex+", arg1='"+sql+"'.", ex);
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
	@Override
	public void setProgress(int state, String name, int x, int max)
	{
		_logger.debug("setProgress(): state="+state+", name='"+name+"', x='"+x+"', max='"+max+"'.");
	}

	/**
	 * This method is called before the database is closed normally. It is save
	 * to connect to the database and execute statements at this point, however
	 * the connection must be closed before the method returns.
	 */
	@Override
	public void closingDatabase()
	{
		_logger.debug("H2DatabaseEventListener.closingDatabase()");
	}

}
