package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.asetune.parser.ParserProperties;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public abstract class ToolTipSupplierAbstract
implements ToolTipSupplier
{
	private static Logger _logger = Logger.getLogger(ToolTipSupplierAbstract.class);

	protected Window _guiOwner = null;
	protected ConnectionProvider _connectionProvider = null;

	private List<TtpEntry> _ttpEntryList = null;

	public ToolTipSupplierAbstract(Window owner, ConnectionProvider connectionProvider)
	{
		_guiOwner           = owner;
		_connectionProvider = connectionProvider;

		try
		{
			_logger.info("Installing ToolTip Provider for '"+getName()+"'.");

			setEntryList(load());
			if (getEntryList() != null)
				_logger.info("Loaded "+_ttpEntryList.size()+" entries into the ToolTip Provider '"+getName()+"'.");
			else
				_logger.info("NO entries were loaded into the ToolTip Provider '"+getName()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn ("Problems loading entries into the ToolTip Provider '"+getName()+"'. Caught: "+e);
			_logger.debug("Problems loading entries into the ToolTip Provider '"+getName()+"'. Caught: "+e, e);
		}
	}

	public abstract String getName();

	public List<TtpEntry> getEntryList()
	{
		return _ttpEntryList;
	}

	public void setEntryList(List<TtpEntry> list)
	{
		_ttpEntryList = list;
	}

	public List<TtpEntry> load()
	throws Exception
	{
		String xmlFile = getToolTipProviderFile();
		if ( ! StringUtil.isNullOrBlank(xmlFile) )
		{
			ToolTipProviderXmlParser parser = new ToolTipProviderXmlParser();
			return parser.parseFile(xmlFile);
		}

		return null;
	}

	public String getFooter()
	{
		return null;
	}

	@Override
	public String getToolTipText(RTextArea textArea, MouseEvent e)
	{
//		System.out.println("DummyToolTip: MouseEvent                   ="+e.getPoint());
//		System.out.println("DummyToolTip: textArea.getCaretLineNumber()="+textArea.getCaretLineNumber());
//		System.out.println("DummyToolTip: textArea.getSelectedText()   ="+textArea.getSelectedText());
//		System.out.println("DummyToolTip: textArea.getToolTipLocation()="+textArea.getToolTipLocation(e));
//		
//		System.out.println("DummyToolTip: textArea.viewToModel(point)  ="+textArea.viewToModel(e.getPoint()));

		int dot = textArea.viewToModel(e.getPoint());

		int currentLine = -1;
		try { currentLine = textArea.getLineOfOffset(dot); }
		catch (BadLocationException e1) { e1.printStackTrace(); }
		
		// First check parents tooltip, it might be a parser tooltip, hhhmmmmm this didn't work..
		// HACK: loop the DB_MESSAGES if any in the Document
		@SuppressWarnings("unchecked")
		List<QueryWindow.JAseMessage> dbMessages = (List<QueryWindow.JAseMessage>) textArea.getDocument().getProperty(ParserProperties.DB_MESSAGES);
		if (dbMessages != null)
		{
			for (QueryWindow.JAseMessage msg : dbMessages)
			{
				if ( (currentLine + 1) == msg.getScriptRow() )
					return msg.getFullMsgTextHtml();
			}
		}

//		String parentText = textArea.getToolTipText();
//		if (StringUtil.hasValue(parentText))
//			return parentText;
		
		String word     = RSyntaxUtilitiesX.getCurrentWord(textArea, dot, getAllowedChars());
		String fullWord = RSyntaxUtilitiesX.getCurrentFullWord(textArea, dot);
//		System.out.println("DummyToolTip: word    ="+word);
//		System.out.println("DummyToolTip: fullWord="+fullWord);

		String selectedText = textArea.getSelectedText();
		if (selectedText != null)
		{
			word     = selectedText;
			fullWord = selectedText;
		}

		return getToolTipText(textArea, e, word, fullWord);
//		return "<html><h2>Dummy ToolTip header</h2> "+text+"<br> <ul><li>punk1 </li><li>punk 2</li><li>punkt 3</li><li>punkt 4</li></ul> <html>";
	}
	
	/** 
	 * any implementers need to return a tooltip based on what word that we are currently "hovering" over or "selected"
	 */
//	public abstract String getToolTipText(RTextArea textArea, MouseEvent e, String word, String fullWord);
	public String getToolTipText(RTextArea textArea, MouseEvent e, String word, String fullWord)
	{
		return getHtmlTextFor(word);
	}

	public String getHtmlTextFor(String word)
	{
		if (StringUtil.isNullOrBlank(word))
			return null;

		List<TtpEntry> entryList = getEntryList();
		if (entryList == null)
			return null;

		try
		{
		    Pattern pattern = Pattern.compile(word+".*", Pattern.CASE_INSENSITIVE);
			if (word.indexOf("*") >= 0) // if input has '*' change it to '.*' and add '.*' at the end. 
				pattern = Pattern.compile(word.replace("*", ".*")+".*", Pattern.CASE_INSENSITIVE);

			StringBuilder sb = new StringBuilder();
			int rows = 0;
			for (TtpEntry e : entryList)
			{
				// use regexp search
				if ( pattern.matcher(e.getCmdName()).matches() )
				{
					rows++;
					if (rows == 1)
						sb.append("<html>");

					sb.append( e.toHtml(false, "<hr>") );
				}
			}
			if (rows > 0)
			{
				String footer = getFooter();
				if (footer != null)
				{
					sb.append("<br>");
					sb.append("<hr>");
					sb.append(footer);
				}
				sb.append("</html>");
			}

			return sb.toString();
		}
		catch (PatternSyntaxException ex) 
		{
			_logger.debug("PatternSyntaxException for word '"+word+"'", ex);
		}
		return null;
	}


	/**
	 * What characters are to be considered as a word when "hovering" over it.
	 * @return
	 */
	public abstract String getAllowedChars();

	/**
	 * Get the filename, where the ToolTip provider stores it's information. 
	 * @return null if not stored in a file.
	 */
	public abstract String getToolTipProviderFile();
}
