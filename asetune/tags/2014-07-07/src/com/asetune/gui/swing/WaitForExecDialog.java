package com.asetune.gui.swing;

import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.StringUtil;
import com.sybase.jdbcx.SybConnection;

/**
 * This needs a lot more work
 * <p>
 * 
 * Example of how to use this:
 * <pre>
 * // Create a Waitfor Dialog
 * final WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Do Long Running Job");
 * 
 * // Create the Executor object
 * WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor()
 * {
 *     // This is the object that will be returned.
 *     ArrayList<JdbcCompletion> completionList = new ArrayList<JdbcCompletion>();
 * 
 *     public Object doWork()
 *     {
 *         List<String> resultList = new ArrayList<String>();
 *         
 *         wait.setState("Doing first step"); // will be displayed in status field, below "Do Long Running Job"
 *         // Do long running job...
 *         
 *         resultList.add("Some result(s)");
 *         wait.setState("Doing second step");
 *         
 *         return resultList; // simply return null, if this is not needed
 *     }
 * };
 * 
 * // Now Execute and wait for it to finish
 * List<String> completionList = (List)wait.execAndWait(doWork);
 * 
 * // or if you didn't return anything from the doWork() method
 * wait.execAndWait(doWork);
 * 
 * </pre>
 * 
 * @author Goran Schwarz
 */
public class WaitForExecDialog
extends JDialog
implements PropertyChangeListener, ActionListener
{
	private static Logger _logger = Logger.getLogger(WaitForExecDialog.class);
	private static final long serialVersionUID = 1L;

	private BgExecutor      _execClass       = null;
	private boolean         _normalExit      = false; // set to true when the SwingWorker ends

	private JLabel          _label           = new JLabel("Waiting...", JLabel.CENTER);

	private JLabel          _state_lbl       = new JLabel();
	private RSyntaxTextArea _extraText_txt   = null;
	private RTextScrollPane _extraText_sroll = null;
	private JButton         _cancel_but      = new JButton("Cancel");
	private boolean         _cancelWasPressed= false;
	private Connection      _conn            = null;

	/** if a JDBC Connection is passed, the cancel button is visible */
	public WaitForExecDialog(Window owner, Connection conn, String waitForLabel)
	{
		this(owner, null, waitForLabel, null);
	}

	public WaitForExecDialog(Window owner, String waitForLabel)
	{
		this(owner, null, waitForLabel, null);
	}
	public WaitForExecDialog(Window owner, String waitForLabel, String extraString)
	{
		this(owner, null, waitForLabel, null);
	}
	public WaitForExecDialog(Window owner, Connection conn, String waitForLabel, String extraString)
	{
		super((Frame)null, "Waiting...", true);
		setLayout(new MigLayout());

		if (waitForLabel != null)
			_label.setText(waitForLabel);
		_label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));

		_cancel_but.setToolTipText("CANCEL current operation.");

		_conn = conn;

		if (extraString != null)
		{
			_extraText_txt   = new RSyntaxTextArea();
			_extraText_sroll = null;new RTextScrollPane(_extraText_txt);

			RSyntaxUtilitiesX.installRightClickMenuExtentions(_extraText_txt, _extraText_sroll, this);

			_extraText_txt.setText(extraString);
		//	_extraText_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			_extraText_txt.setHighlightCurrentLine(false);
		//	_extraText_txt.setLineWrap(true);
		//	_extraText_sroll.setLineNumbersEnabled(true);
		}

		add(_label,        "push, grow, wrap");
		add(_state_lbl,    "wrap");
		add(_cancel_but,   "center, hidemode 3");
		if (extraString != null)
			add(_extraText_sroll, "push, grow, wrap");

		_cancel_but.addActionListener(this);

		pack();
		setSize( getSize().width + 50, getSize().height + 35);
		setLocationRelativeTo(owner);

		// When the "X" close window is pressed, call some method.
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (_execClass != null)
				{
					_execClass.windowClosing(_normalExit, e);
				}
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_cancel_but.equals(source))
		{
			_cancelWasPressed = true;
			if (_execClass != null)
				_execClass.cancel_private();
		}
	}

	public void markExtraText(String str)
	{
		if ( ! StringUtil.isNullOrBlank(str) && _extraText_txt != null)
		{
//			_extraText_txt.markAll(str, false, false, false);
			SearchContext context = new SearchContext();
			context.setSearchFor(str);
			SearchEngine.find(_extraText_txt, context);
		}
	}

	/**
	 * This will return TRUE, if the cancel button was pressed OR the "X" button was pressed.
	 * @return
	 */
	public boolean wasCanceled()
	{
		return _cancelWasPressed || !_normalExit;
	}

	/**
	 * This will return TRUE, if the cancel button was pressed.
	 * @return
	 */
	public boolean wasCancelPressed()
	{
		return _cancelWasPressed;
	}

	/**
	 * This will return FALSE, if the Wait window was closed with the "X" button.
	 * @return
	 */
	public boolean isNormalExit()
	{
		return _normalExit;
	}

	public String getState()
	{
		return _state_lbl.getText();
	}
	public void setState(final String string)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("WaitForExecDialog.setState('"+string+"');");

		_state_lbl.setText(string);
//		if (SwingUtilities.isEventDispatchThread())
//		{
//			_state_lbl.setText(string);
//		}
//		else
//		{
//			Runnable doAsEdt = new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					_state_lbl.setText(string);
//				}
//			};
////			SwingUtilities.invokeLater(doAsEdt);
//			try
//			{
//				SwingUtilities.invokeAndWait(doAsEdt);
//			}
//			catch (InterruptedException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			catch (InvocationTargetException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
	
	/**
	 * Called by SwingWorker on completion<br>
	 * Note: need to register on the SwingWorker using: workerThread.addPropertyChangeListener( "this SqlProgressDialog" );
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("WaitForExecDialog.propertyChange(): propName="+event.getPropertyName()+", newVal="+event.getNewValue()+", oldVal="+event.getOldValue()+", PropagationId="+event.getPropagationId()+", getSource="+event.getSource());

		// Close this window when the Swing worker has completed
		if ("state".equals(event.getPropertyName()) && StateValue.DONE == event.getNewValue()) 
		{
			_normalExit = true;
			setVisible(false);
			dispose();
		}
	}

	/**
	 * Execute the input <code>Runnable</code> in background using <code>SwingWorker</code>
	 * while the <code>Runnable</code> is executing this dialog will be visible.<br>
	 * We will wait until the <code>Runnable</code> is finished.
	 * <p>
	 * So this is basically execute a long running process without blocking the Event Dispatch Thread
	 * <p>
	 * This means that the Swing Swing Event Dispatch Thread GUI is still scheduling other work
	 * while we are waiting in the code for the Executable to end and this dialog will disappear.  
	 * @param execClass
	 */
	public Object execAndWait(final BgExecutor execClass)
	{
		return execAndWait(execClass, 0);
	}
	/**
	 * Execute the input <code>Runnable</code> in background using <code>SwingWorker</code>
	 * while the <code>Runnable</code> is executing this dialog will be visible.<br>
	 * We will wait until the <code>Runnable</code> is finished.
	 * <p>
	 * So this is basically execute a long running process without blocking the Event Dispatch Thread
	 * <p>
	 * This means that the Swing Swing Event Dispatch Thread GUI is still scheduling other work
	 * while we are waiting in the code for the Executable to end and this dialog will disappear.  
	 * @param execClass
	 * @param graceTime  if the executions takes less than X ms, then the GUI wont be showed.
	 */
	// FIXME: add method execAndWaitWithThrow or similar, that throws Exception or Throwable
	public Object execAndWait(final BgExecutor execClass, int graceTime)
	{
		_execClass = execClass;

		// Execute in a Swing Thread
		SwingWorker<Object, Object> doBgThread = new SwingWorker<Object, Object>()
		{
			@Override
			protected Object doInBackground() 
			throws Exception
			{
				try 
				{
					_execClass.setBgThread(Thread.currentThread());
					Object retObject = _execClass.doWork();
					if (_execClass.hasException())
						_logger.info("WaitForExecDialog:(at try block) has problems when doing it's work.", _execClass.getException());
						
					return retObject;
				} 
				catch (Throwable t) 
				{
					_logger.info("WaitForExecDialog:(at catch block) has problems when doing it's work.", t);
//					_logger.debug("WaitForExecDialog:(at catch block) has problems when doing it's work.", t);
				}
				return null;
			}
	
		};
		doBgThread.addPropertyChangeListener(this);
		doBgThread.execute();

		boolean canDoCancel = _execClass.canDoCancel();
		if (_conn != null && _conn instanceof SybConnection)
			canDoCancel = true;
		_cancel_but.setVisible(canDoCancel);

		// Do not show Wait GUI at once, if it's a fast execution, we do not need to show...
		if (graceTime > 0)
		{
			// Note: this can be done better with a timer, but it will do for now...
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < graceTime )
			{
				// if the bg job is done, get out of here
				if ( doBgThread.isDone() )
					break;
	
				// Sleep for 10ms, get out of here if we are interrupted.
				try { Thread.sleep(10); }
				catch (InterruptedException ignore) { break; }
			}
		}

		//the dialog will be visible until the SwingWorker is done
		if ( ! doBgThread.isDone() )
		{
			setVisible(true);
		}

		try
		{
			return doBgThread.get();
		}
		catch (InterruptedException e)
		{
			_logger.warn("execAndWait Caught: "+e, e);
			return null;
		}
		catch (ExecutionException e)
		{
			_logger.warn("execAndWait Caught: "+e, e);
			return null;
		}
	}

	/**
	 * Like the Runnable interface
	 */
	public abstract static class BgExecutor
	{
		private Thread            _bgThread    = null;
//		private boolean           _wasCanceled = false;
		private WaitForExecDialog _waitDialog  = null;
		private Throwable         _throwable   = null;

		/**
		 * Constructor
		 * @param waitDialog
		 */
		public BgExecutor(WaitForExecDialog waitDialog)
		{
			_waitDialog = waitDialog;
			if (_waitDialog == null)
				throw new RuntimeException("The waitDialog can't be null");
		}

		/**
		 * Here is where the work will be done
		 * @return
		 */
		public abstract Object doWork();

		/**
		 * Get the <code>WaitForExecDialog</code> object, so we can set states etc.
		 */
		public WaitForExecDialog getWaitDialog()
		{
			return _waitDialog;
		}

		public boolean hasException()
		{
			return _throwable != null;
		}
		public Throwable getException()
		{
			return _throwable;
		}
		public void setException(Throwable t)
		{
			_throwable = t;
		}
		/**
		 * Should the cancel button be visible or not. <br>
		 * Override this to change functionality
		 */
		public boolean canDoCancel()
		{
			return false;
		}

//		public void setCanceled(boolean b)
//		{
//			_wasCanceled = b;
//		}
//		public boolean isCanceled()
//		{
//			return _wasCanceled;
//		}
		/**
		 * Set the SwingWorkers thread
		 */
		public void setBgThread(Thread thread)
		{
			_bgThread = thread;
		}

		/**
		 * Get the SwingWorkers thread
		 */
		public Thread getBgThread()
		{
			return _bgThread;
		}

		/**
		 * If the cancel button is pressed, this method will be called
		 */
		private void cancel_private()
		{
//			setCanceled(true);

			cancel();

			Connection conn = getWaitDialog()._conn;
			if (conn != null && conn instanceof SybConnection)
			{
				try
				{
					((SybConnection)conn).cancel();
				}
				catch (SQLException e)
				{
					_logger.warn("cancel_private(): Problems doing cancel on SybConnection", e);
				}
			}

			if (getBgThread() != null)
				getBgThread().interrupt();
		}
		public void cancel()
		{
		}

		/**
		 * called if someone pressed the "X" button on the window before the bgThread has ended.<br>
		 * This will just call cancel() method if normalExit is FALSE.
		 * 
		 * @param normalExit This will be true if the SwingWorker thread has ended. and false if the "X" has been pressed.
		 */
		public void windowClosing(boolean normalExit, WindowEvent e)
		{
			if ( ! normalExit )
				cancel_private();
		}
	}
}
