package com.asetune.sql;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.sybase.jdbcx.SybConnection;

/*----------------------------------------------------------------------
** BEGIN: SqlProgressDialog
**----------------------------------------------------------------------*/ 
public class SqlProgressDialog
extends JDialog
implements PropertyChangeListener, ActionListener
{
	private static Logger _logger = Logger.getLogger(SqlProgressDialog.class);
	private static final long serialVersionUID = 1L;

	private JLabel           _allSql_lbl            = new JLabel("Executing SQL at ASE Server", JLabel.CENTER);
	private JLabel           _msg_lbl               = new JLabel("Messages for current SQL Batch", JLabel.CENTER);

	private Connection       _conn                  = null;
	private Statement        _stmnt                 = null;
	private JLabel           _state_lbl             = new JLabel();
	private boolean          _markSql               = false; // mark/move to current SQL in _allSql_txt 
	private RSyntaxTextAreaX _allSql_txt            = new RSyntaxTextAreaX();
	private RTextScrollPane  _allSql_sroll          = new RTextScrollPane(_allSql_txt);
	private RSyntaxTextAreaX _msg_txt               = new RSyntaxTextAreaX();
	private RTextScrollPane  _msg_sroll             = new RTextScrollPane(_msg_txt);
	private JButton          _cancel                = new JButton("Cancel");

	private Timer            _execSqlTimer          = new Timer(100, new ExecSqlTimerAction());
	private String           _currentExecSql        = null; 

	private boolean          _firstExec             = true;
	private long             _totalExecStartTime    = 0;
	private long             _batchExecStartTime    = 0;
	private int              _currentExecCounter    = 0;
	private int              _totalExecCount        = 0;
	private JLabel           _totalExecTimeDesc_lbl = new JLabel("Total Exec Time: ");
	private JLabel           _totalExecTimeVal_lbl  = new JLabel("-");
	private JLabel           _batchExecTimeDesc_lbl = new JLabel("Batch Exec Time: ");
	private JLabel           _batchExecTimeVal_lbl  = new JLabel("-");

	private boolean          _firstMsgWasReceived   = false;
	private List<SQLException> _msgList             = new ArrayList<SQLException>();

//	private SwingWorker<String, Object>	_swingWorker = null;

	private boolean          _cancelled             = false;

	/**
	 * This timer is started just before we execute the SQL ststement that refreshes the data
	 * And it's stopped when the execution is finnished
	 * If X ms has elipsed in the database... show some info to any GUI that we are still in refresh... 
	 */
	private class ExecSqlTimerAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			deferredTimerAction(_currentExecSql);
		}
	}

	public SqlProgressDialog(Window owner, Connection conn, String sql)
	{
//		super((Frame)null, "Waiting for server...", true);
		super(owner, "Waiting for server...", ModalityType.DOCUMENT_MODAL);
		setLayout(new MigLayout());

		_conn = conn;

		Font f = _totalExecTimeDesc_lbl.getFont();
		_allSql_lbl          .setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));
		_totalExecTimeVal_lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_batchExecTimeVal_lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

		_cancel.setToolTipText("Send a CANCEL request to the server.");

		_allSql_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//		_allSql_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		_allSql_txt.setHighlightCurrentLine(true);
		//_sql_txt.setLineWrap(true);
		//_sql_sroll.setLineNumbersEnabled(true);
//		RSyntaxUtilitiesX.installRightClickMenuExtentions(_allSql_txt, this);
		_allSql_txt.setText(sql);
		_allSql_txt.setCaretPosition(0);
		_allSql_txt.setEditable(false);

//		_msg_txt.setText(sql);
//		_msg_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		_msg_txt.setHighlightCurrentLine(false);
		//_msg_txt.setLineWrap(true);
		//_msg_txt.setLineNumbersEnabled(true);
//		RSyntaxUtilitiesX.installRightClickMenuExtentions(_msg_txt, this);
		_msg_txt.setEditable(false);
		
		add(_allSql_lbl,            "pushx, growx, wrap");
		add(_state_lbl,             "wrap");
		add(_totalExecTimeDesc_lbl, "split, width 85");
		add(_totalExecTimeVal_lbl,  "wrap");
		add(_batchExecTimeDesc_lbl, "split, width 85, hidemode 3");
		add(_batchExecTimeVal_lbl,  "wrap");
		add(_allSql_sroll,          "push, grow, wrap");
		add(_msg_lbl,               "pushx, growx, wrap, hidemode 3");
		add(_msg_sroll,             "hmin 150, push, grow, wrap, hidemode 3");
		add(_cancel,                "center");

		_msg_lbl.setVisible(false);
		_msg_sroll.setVisible(false);

		_cancel.addActionListener(this);

		pack();
		setSize( getSize().width + 100, getSize().height + 70);
		SwingUtils.setSizeWithingScreenLimit(this, 200);
		setLocationRelativeTo(owner);
	}
	
	public void setCurrentSqlText(String sql, int batchCount, int totalExecCount)
	{
		//System.out.println(">>>>>>: setCurrentSqlText(): sql="+sql);

		boolean markSql = batchCount > 1;
		boolean showBatchLabels = batchCount > 1 || totalExecCount > 1;
		if ( ! showBatchLabels )
		{
			_batchExecTimeDesc_lbl.setVisible(false);
			_batchExecTimeVal_lbl .setVisible(false);
		}
		_totalExecCount = totalExecCount;
		_markSql = markSql;
		if (_firstExec)
		{
			_firstExec = false;
			_totalExecStartTime = System.currentTimeMillis();
			_execSqlTimer.start();
		}
		//_batchExecStartTime = System.currentTimeMillis();
		setCurrentBatchStartTime(0);
		_currentExecSql = sql;
		_execSqlTimer.restart(); // dont kick off the times to early...

		// Update of WHAT SQL that is currently executed, is done by the deferredSetCurrentSqlText()
		// othetwise RSyntaxTextArea will have problems every now and then (if the scroll has moved and it needs to parse/update the visible rect)
	}

	public void setCurrentBatchStartTime(int currentBatchNumber)
	{
		_batchExecStartTime = System.currentTimeMillis();
		_currentExecCounter = currentBatchNumber;
	}

	/** 
	 * Executed by the Timer, which kicks of every X ms
	 * Updates, Total and Batch execution time...
	 * If a new SQL statement is executed, move to current executes SQL Statement
	 * 
	 * @param sql SQL Statement to move to in the dialog.
	 */
	private void deferredTimerAction(String sql)
	{
//		// 
//		if ( ! isVisible() && (_swingWorker != null && !_swingWorker.isDone()) )
//			setVisible(true);

//		_execSqlTimer.stop();
		_totalExecTimeVal_lbl.setText( TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - _totalExecStartTime) );
		if (_totalExecCount > 1)
			_batchExecTimeVal_lbl.setText( TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - _batchExecStartTime) + " (go # "+(_currentExecCounter+1)+" of "+_totalExecCount+")");
		else
			_batchExecTimeVal_lbl.setText( TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - _batchExecStartTime) );

		_currentExecSql = null;
		if (StringUtil.isNullOrBlank(sql) && _msgList.size() > 0)
			return;

//System.out.println("XXXXXX: deferredTimerAction(): sql = " + sql);
//System.out.println("XXXXXX: deferredTimerAction(): _msgList.size() = " + _msgList.size());

		if ( ! StringUtil.isNullOrBlank(sql) )
		{
			if (_markSql)
			{
				// RSyntaxTextArea seems to have problems doing the blow, so make sure to catch problems
				try
				{
					// Reset messages
					_msg_txt.setText("");
	
					// Mark current SQL
					SearchContext sc = new SearchContext();
					sc.setSearchFor(sql);
					sc.setMatchCase(true);
					sc.setWholeWord(false);
					sc.setRegularExpression(false);
	
					// Position in text...
					SearchEngine.find(_allSql_txt, sc);
	
					// Mark it
					//_allSql_txt.markAll(sql, sc.getMatchCase(), sc.getWholeWord(), sc.isRegularExpression());
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems updating current executing SQL Statement, but will continue anyway...", t);
				}
			}
		} // end: _markSql
		
		// add Messages
		if (_msgList.size() > 0)
		{
			// Copy the list and make a new one (if new messages are appended as we process them
			List<SQLException> oldMsgList = new ArrayList<SQLException>(_msgList);
			_msgList = new ArrayList<SQLException>();

			// Process messages
			for (SQLException sqle : oldMsgList)
			{
				// if RSyntaxTextArea has problem with this, catch it...
				try
				{
					// First time make the window a bit larger
					if (_firstMsgWasReceived == false)
					{
						Dimension dimW = getSize();
						dimW.height += 150;
						setSize(dimW);
	
						SwingUtils.setSizeWithingScreenLimit(this, 0);
					}
	
					_firstMsgWasReceived = true;
					_msg_lbl.setVisible(true);
					_msg_sroll.setVisible(true);
	
					String msg = AseConnectionUtils.getSqlWarningMsgs(sqle);
					_msg_txt.append(msg);
	
					_msg_txt.setCaretPosition(_msg_txt.getText().length());
	
//					SearchContext sc = new SearchContext();
//					sc.setSearchFor(msg);
//					sc.setMatchCase(true);
//					sc.setWholeWord(false);
//					sc.setRegularExpression(false);

//					// Position in text...
//					SearchEngine.find(_msg_txt, sc);
	
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems adding a message to the progress dialog, but will continue anyway...", t);
				}
			}
		}
	}

	public void setState(String string)
	{
		_state_lbl.setText(string);
	}

	public String getState()
	{
		return _state_lbl.getText();
	}
	
	public void addMessage(SQLException sqle)
	{
		_msgList.add(sqle);
	}
//	public void addMessage(SQLException sqle)
//	{
//		// if RSyntaxTextArea has problem with this, catch it...
//		try
//		{
//			// First time make the window a bit larger
//			if (_firstMsgWasReceived == false)
//			{
////				Dimension dimS = _msg_sroll.getSize();
////				dimS.height += 150;
////				_msg_sroll.setSize(dimS);
//
//				Dimension dimW = getSize();
//				dimW.height += 150;
//				setSize(dimW);
//
//				SwingUtils.setSizeWithingScreenLimit(this, 0);
//			}
//
//			_firstMsgWasReceived = true;
//			_msg_lbl.setVisible(true);
//			_msg_sroll.setVisible(true);
//
//			String msg = AseConnectionUtils.getSqlWarningMsgs(sqle);
//			_msg_txt.append(msg);
//
//			_msg_txt.setCaretPosition(_msg_txt.getText().length());
//
////			SearchContext sc = new SearchContext();
////			sc.setSearchFor(msg);
////			sc.setMatchCase(true);
////			sc.setWholeWord(false);
////			sc.setRegularExpression(false);
////
////			// Position in text...
////			SearchEngine.find(_msg_txt, sc);
//
//		}
//		catch (Throwable t) 
//		{
//			_logger.warn("Problems adding a message to the progress dialog, but will continue anyway...", t);
//		}
//	}

	/**
	 * Called by SwingWorker on completion<br>
	 * Note: need to register on the SwingWorker using: workerThread.addPropertyChangeListener( "this SqlProgressDialog" );
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		// Close this window when the Swing worker has completed
		if ("state".equals(event.getPropertyName()) && StateValue.DONE == event.getNewValue()) 
		{
			setVisible(false);
			dispose();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		// CANCEL
		if (_cancel.equals(source))
		{
			_cancelled = true;

			if (_conn != null )
			{
				// jConnect supports CANCEL on the connection level, so lets use that
				if (_conn instanceof SybConnection)
				{
					try
					{
						((SybConnection)_conn).cancel();
					}
					catch(SQLException ex)
					{
						SwingUtils.showErrorMessage("Cancel", "Problems sending cancel to ASE: "+ex, ex);
					}
				}
				// All others try to call cancel on the statement level
				else
				{
					if (_stmnt != null)
					{
						try
						{
							_stmnt.cancel();
						}
						catch(SQLException ex)
						{
							SwingUtils.showErrorMessage("Cancel", "Problems doing cancel to on the Statement level: "+ex, ex);
						}
					}
				}
			} // end: conn != null
		}
	}

	public boolean isCancelled()
	{
		return _cancelled;
	}

	public void setSqlStatement(Statement stmnt)
	{
		_stmnt = stmnt;
	}


//	@Override
//	public void setVisible(boolean state) 
//	{
//		super.setVisible(state);
//		if (state == false)
//			_execSqlTimer.stop();
//	}

	/**
	 * Wait for the background thread to execute before continue<br>
	 * If the execution takes longer than <code>graceTime</code> ms, then a dialog will be visible.
	 * 
	 * @param doBgThread
	 * @param graceTime wait this amount of ms until a dialog will be displayed.
	 */
	public void waitForExec(SwingWorker<String, Object> doBgThread, int graceTime)
	{
		_execSqlTimer.start();

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

		//the dialog will be visible until the SwingWorker is done
		if ( ! doBgThread.isDone() )
		{
			setVisible(true);
		}

		_execSqlTimer.stop();
	}
}
/*----------------------------------------------------------------------
** END: SqlProgressDialog
**----------------------------------------------------------------------*/ 
