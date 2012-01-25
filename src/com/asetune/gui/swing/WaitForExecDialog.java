package com.asetune.gui.swing;

import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import com.asetune.utils.StringUtil;

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

	private JLabel          _label           = new JLabel("Waiting...", JLabel.CENTER);

	private JLabel          _state_lbl       = new JLabel();
	private RSyntaxTextArea _extraText_txt   = null;
	private RTextScrollPane _extraText_sroll = null;
	private JButton         _cancel_but      = new JButton("Cancel");

	public WaitForExecDialog(Window owner, String waitForLabel)
	{
		this(owner, waitForLabel, null);
	}
	public WaitForExecDialog(Window owner, String waitForLabel, String extraString)
	{
		super((Frame)null, "Waiting...", true);
		setLayout(new MigLayout());

		if (waitForLabel != null)
			_label.setText(waitForLabel);
		_label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));

		_cancel_but.setToolTipText("CANCEL current operation.");

		if (extraString != null)
		{
			_extraText_txt   = new RSyntaxTextArea();
			_extraText_sroll = null;new RTextScrollPane(_extraText_txt);

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
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_cancel_but.equals(source))
		{
			if (_execClass != null)
				_execClass.cancel();
		}
	}

	public void markExtraText(String str)
	{
		if ( ! StringUtil.isNullOrBlank(str) && _extraText_txt != null)
			_extraText_txt.markAll(str, false, false, false);
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
	public void propertyChange(PropertyChangeEvent event) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("WaitForExecDialog.propertyChange(): propName="+event.getPropertyName()+", newVal="+event.getNewValue()+", oldVal="+event.getOldValue()+", PropagationId="+event.getPropagationId()+", getSource="+event.getSource());

		// Close this window when the Swing worker has completed
		if ("state".equals(event.getPropertyName()) && StateValue.DONE == event.getNewValue()) 
		{
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
		_execClass = execClass;

		// Execute in a Swing Thread
		SwingWorker<Object, Object> doBgThread = new SwingWorker<Object, Object>()
		{
			@Override
			protected Object doInBackground() throws Exception
			{
				try 
				{
					return _execClass.doWork();
				} 
				catch (Throwable t) 
				{
					_logger.debug("WaitForEcecDialog: has problems when doing it's work.", t);
				}
				return null;
			}
	
		};
		doBgThread.addPropertyChangeListener(this);
		doBgThread.execute();

		_cancel_but.setVisible(_execClass.canDoCancel());

		//the dialog will be visible until the SwingWorker is done
		setVisible(true); 
		
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
		/**
		 * Here is where the work will be done
		 * @return
		 */
		public abstract Object doWork();

		/**
		 * If the cancel button is pressed, this method will be called
		 */
		public void cancel()
		{
		}

		/**
		 * Should the cancel button be visible or not. 
		 */
		public boolean canDoCancel()
		{
			return false;
		}
	}
}
