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
package com.asetune.tools.sqlw.msg;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rtextarea.RTextArea;

import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.RXTextUtilities;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.ColorUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class JAseMessage
extends JTextArea
{
	private static final long serialVersionUID = 1L;

	public static final String FOCUSABLE_TIPS_PROPERTY				= "RSTA.focusableTips";

	public static final String  PROPKEY_showToolTip = "AseMessage.tooltip.show";
	public static final boolean DEFAULT_showToolTip = true;

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean _useFocusableTips = true;

	/** The last focusable tip displayed. */
	private FocusableTip _focusableTip = null;
	
	private int _useFocusableTipAboveSize = 1000;


	private int    _msgNum      = -1;
	private String _msgText     = null;
	private int    _msgSeverity = -1;
	private int    _scriptRow   = -1;
	private int    _scriptCol   = -1;
	private String _originSql   = null;
	private String _objectText  = null; // IF stored procedure/function is passed

	private RTextArea _sqlTextArea;

	protected static Font _aseMsgFont = null;

//	public JAseMessage()
//	{
//		_init();
//	}

	public JAseMessage(final String aseMsg, String originSql)
	{
		super(aseMsg);
		_originSql   = originSql;
		init();
	}

	public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, String originSql, RTextArea sqlTextArea)
	{
		this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, -1, originSql, null, sqlTextArea);
	}

	public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, String originSql, String objectText, RTextArea sqlTextArea)
	{
		this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, -1, originSql, objectText, sqlTextArea);
	}

	public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, int scriptCol, String originSql, RTextArea sqlTextArea)
	{
		this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, scriptCol, originSql, null, sqlTextArea);
	}

	/**
	 * 
	 * @param aseMsg       Text message to print in the GUI
	 * @param msgNum       DBMS Message number
	 * @param msgText      Origin DBMS Text Message ?????
	 * @param msgSeverity  ASE Severity (if above 10 then the text will be in "red")
	 * @param scriptRow    (used to draw an red underline in the TextArea)
	 * @param scriptCol    (used to draw an red underline in the TextArea)
	 * @param originSql    Origin SQL Text that was executed
	 * @param objectText   If an "procedure/function" source code is accessible, it's the text
	 * @param sqlTextArea  Used for navigation button (next previous button)
	 */
	public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, int scriptCol, String originSql, String objectText, RTextArea sqlTextArea)
	{
		super(aseMsg);
		_msgNum      = msgNum;
		_msgText     = msgText;
		_msgSeverity = msgSeverity;
		_scriptRow   = scriptRow; // can be used to draw an red underline in the TextArea
		_scriptCol   = scriptCol; // can be used to draw an red underline in the TextArea
		_originSql   = originSql;
		_objectText  = objectText;
		_sqlTextArea = sqlTextArea;
		init();
//System.out.println("JAseMessage: msgNum="+msgNum+", msgSeverity="+msgSeverity+", msgText='"+msgText+"', aseMsg='"+aseMsg+"'.");
	}

	protected void init()
	{
		super.setEditable(false);

		if (StringUtil.hasValue(_originSql))
			ToolTipManager.sharedInstance().registerComponent(this);

		if (_aseMsgFont == null)
			_aseMsgFont = new Font("Courier", Font.PLAIN, SwingUtils.hiDpiScale(12));
		setFont(_aseMsgFont);

		if (_msgSeverity > 10)
			setForeground(ColorUtils.DARK_RED);

		setLineWrap(true);
		setWrapStyleWord(true);
//		setOpaque(false); // Transparent

		// install: GO-TO row when you inter the field
		if (_scriptRow > 0)
		{
			this.addMouseListener(new MouseListener()
			{
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
				
				@Override 
				public void mouseClicked(MouseEvent e) 
				{
					if (_sqlTextArea == null)
						return;

					try
					{
						// Move to correct line in SQL Text
						_sqlTextArea.setCaretPosition(_sqlTextArea.getLineStartOffset(_scriptRow - 1));
						RXTextUtilities.possiblyMoveLineInScrollPane(_sqlTextArea);

						// Unmark all messages
						Container parent = getParent();
						if (parent instanceof JPanel)
						{
							JPanel panel = (JPanel)parent;
							for (int i=0; i<panel.getComponentCount(); i++)
							{
								Component comp = panel.getComponent(i);
								if (comp instanceof JAseMessage)
								{
									JAseMessage msg = (JAseMessage)comp;
									msg.setBackground(Color.WHITE); //FIXME: Maybe not hardcode this color, get it from UI
								}
							}
						}
						// Mark the message
						setBackground(QueryWindow.DEFAULT_OUTPUT_ERROR_HIGHLIGHT_COLOR);
					}
					catch (BadLocationException ble)
					{ // Never happens
						UIManager.getLookAndFeel().provideErrorFeedback(_sqlTextArea);
						ble.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public String getToolTipText()
	{
		if (StringUtil.isNullOrBlank(_originSql))
			return null;

		boolean showToolTip = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showToolTip, DEFAULT_showToolTip);
		if (StringUtil.isNullOrBlank(_objectText) && showToolTip == false)
			return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<pre>");
		sb.append(getOriginSql()); // Show Origin SQL, on tooltip
		sb.append("</pre>");
		if (_objectText != null)
		{
			sb.append("<b>Procedure/Function Definition</b> Trying to mark error line in red.<br>");
			sb.append("<pre>");
			sb.append(getObjectText());
			sb.append("</pre>");
		}
		sb.append("<html>");
		return sb.toString();
	}

	/**
	 * Returns whether "focusable" tool tips are used instead of standard
	 * ones.  Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and click links in.
	 *
	 * @return Whether to use focusable tool tips.
	 * @see #setUseFocusableTips(boolean)
	 * @see FocusableTip
	 */
	public boolean getUseFocusableTips() 
	{
		return _useFocusableTips;
	}

	/**
	 * Sets whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and clink links in.
	 *
	 * @param use Whether to use focusable tool tips.
	 * @see #getUseFocusableTips()
	 * @see FocusableTip
	 */
	public void setUseFocusableTips(boolean use) 
	{
		if (use != _useFocusableTips) 
		{
			_useFocusableTips = use;
			firePropertyChange(FOCUSABLE_TIPS_PROPERTY, !use, use);
		}
	}

	/**
	 * returns true if we should use focusable tooltip for this text
	 * 
	 * @param toolTipText The tooltip text
	 * @return true if to use focusable tooltip
	 */
	public boolean getUseFocusableTipForText(String toolTipText)
	{
		int ttLen = 0;
		if (toolTipText != null)
			ttLen = toolTipText.length();

		if (ttLen > _useFocusableTipAboveSize)
			return true;

		return false;
	}

	/**
	 * Text size limit (in bytes) when we should use focusable tooltip or not 
	 * @param size if tooltip is above this size, then use focusable tooltip
	 */
	public void setUseFocusableTipsSize(int size) 
	{
		_useFocusableTipAboveSize = size;
	}

	/**
	 * Returns the tool tip to display for a mouse event at the given
	 * location.  This method is overridden to give a registered parser a
	 * chance to display a tool tip (such as an error description when the
	 * mouse is over an error highlight).
	 *
	 * @param e The mouse event.
	 */
	@Override
	public String getToolTipText(MouseEvent e) 
	{
		// Check parsers for tool tips first.
		String text = super.getToolTipText(e);

		// Do we want to use "focusable" tips?
		if (getUseFocusableTips() && getUseFocusableTipForText(text)) 
		{
			if (text != null) 
			{
				if (_focusableTip == null) 
					_focusableTip = new FocusableTip(this);

				_focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text; // Standard tool tips
	}

	public boolean hasHtmlStartTag()
	{
		// Actually it looks like Msg=6248 is used as a message number for this messages... but lets keep this below login (ordinary set showplan etc uses the same message)
		return _msgSeverity <= 10 && _msgText != null && _msgText.startsWith("<HTML>");
	}

	public boolean hasHtmlEndTag()
	{
		// Actually it looks like Msg=6248 is used as a message number for this messages... but lets keep this below login
		return _msgSeverity <= 10 && _msgText != null && _msgText.indexOf("</HTML>") > 0;
	}

	public String getFullMsgText() { return super.getText(); }
	public String getMsgText()     { return _msgText; }
	public int    getMsgNum()      { return _msgNum; }
	public int    getMsgSeverity() { return _msgSeverity; }
	public int    getScriptRow()   { return _scriptRow; }
	public int    getScriptCol()   { return _scriptCol; }
	public String getOriginSql()   { return _originSql; }
	public String getObjectText()  { return _objectText; }

	public void   setOriginSql (String sql) { _originSql  = sql; }
	public void   setObjectText(String str) { _objectText = str; }

	public String getFullMsgTextHtml()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		
		sb.append("<pre>");
		sb.append(getFullMsgText());
		sb.append("</pre>");
		sb.append("<br>");
		sb.append("<b>Note:</b> The marked red lines will disappear as soon as you change the text in the editor.");
		
		if (_objectText != null)
		{
			sb.append("<br><br>");
			sb.append("<b>Procedure/Function Definition</b> Trying to mark error line in red.<br>");
			sb.append("<pre>");
			sb.append(getObjectText());
			sb.append("</pre>");
		}

		sb.append("</html>");
		
		return sb.toString();
	}
}
