package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.asetune.parser.ParserProperties;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public abstract class ToolTipSupplierAbstract
implements ToolTipSupplier
{
	private static Logger _logger = Logger.getLogger(ToolTipSupplierAbstract.class);

	public  static String  PROPKEY_SHOW_TABLE_INFO = "TooltipSupplier.abstract.show.table.info";
	public  static boolean DEFAULT_SHOW_TABLE_INFO = true;
	
	public  static String  PROPKEY_showDialogOnFirstTimeCompletionProviderLookup = "TooltipSupplier.abstract.showDialog.firstTimeCompletionProviderLookup";
	public  static boolean DEFAULT_showDialogOnFirstTimeCompletionProviderLookup = true;
	
	protected Window                     _guiOwner            = null;
	protected CompletionProviderAbstract _compleationProvider = null; 
	protected ConnectionProvider         _connectionProvider  = null;

	private List<TtpEntry> _ttpEntryList = null;

	public ToolTipSupplierAbstract(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		_guiOwner            = owner;
		_compleationProvider = compleationProvider;
		_connectionProvider  = connectionProvider;

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
		String tt = getHtmlTextFor(word);

		// If nothing was found in the Tooltip supplier
		// Go and check if the word exists in the Completion Provider
		if (StringUtil.isNullOrBlank(tt) && getShowTableInformation())
		{
			if (_compleationProvider != null)
			{
				boolean cpNeedsRefresh = _compleationProvider.needRefresh();
				boolean showDialog = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogOnFirstTimeCompletionProviderLookup, DEFAULT_showDialogOnFirstTimeCompletionProviderLookup);

				if (cpNeedsRefresh && showDialog)
				{
					// CheckBox: Do you want to display object information
					JCheckBox showObjectInfo = new JCheckBox("Show Table/Procedure/Object information, fetched from Code Completion Subsystem.", getShowTableInformation());
					showObjectInfo.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null)
								return;
							conf.setProperty(PROPKEY_SHOW_TABLE_INFO, ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});

					// Create a check box that will be passed to the message
					JCheckBox chk = new JCheckBox("Show this dialog next time ToolTip wants to access the Code Completion and it needs to be refreshed.", showDialog);
					chk.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null)
								return;
							conf.setProperty(PROPKEY_showDialogOnFirstTimeCompletionProviderLookup, ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					JPanel panel = new JPanel(new MigLayout());
					panel.add(showObjectInfo, "wrap");
					panel.add(chk,            "wrap");

					String htmlStr = 
						"<html>" +
						"<h3>Explanation: ToolTip Provider functionality</h3>" +
						"When <i>hovering</i> over a text in the editor, the following happens.<br>" +
						"<ul>" +
						"  <li>Current word is sent to <b>"+getName()+" ToolTip Provider</b><br>" +
						"      The ToolTip Provider will display: <i>explanation</i>, <code>syntax</code> or help text.<br>" +
						"      If nothing is found, next bullet will be done.</li>" +
						"  <li>Current word is sent to The <b>Code Completion Subsystem</b><br>" +
						"      here we try to get column/parameter or generic information about the object.</li>" +
						"</ul>" +
						"When sending the word to the Code Completion Subsystem, it was empty, or simply needs to be refreshed.<br>" +
						"The refresh may take a couple of seconds if your system has a lot of objects.<br>" +
						"<br>" +
						"If you want Column/Parameter lookup for Tables/Procedures, simply press 'close' to continue...<br>" +
						"But if you are not interested in this functionality, 'uncheck' the checkbox below and press 'close' to continue...<br>" +
						"<br>" +
						"This functionality can be enabled/disabled under:<br>" +
						"Button: 'Options' -&gt; 'ToolTip Provider' -&gt; 'Show Table/Column information'<br>" +
						"<br>" +
						"<b>Tip</b>: To use the Code Completion Subsystem press <b>Ctrl-Space</b> in the editor.<br>" +
						"Then choose a Table name from the list<br>" +
						"To do Code Completion for column names, use SQL table alias<br>" +
						"Example: <code>select * from someTableName x where x.</code><b>&lt;Ctrl-Space&gt;</b><br>" +
						"</html>";

					SwingUtils.showInfoMessageExt(_guiOwner, "ToolTip Provider", htmlStr, null, panel);
				} // end: showDialog 

				// Is the option *still* on
				if (getShowTableInformation())
				{
					tt = _compleationProvider.getToolTipTextForObject(word, fullWord);
				}
			}
		}

		return tt;
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

	/**
	 * Set if we should try get table information from the CodeCompletion by calling method 
	 * getToolTipTextForObject(word, fullWord) and show that as a tooltip if the word wasn't 
	 * found in the local dictionary
	 * 
	 * @param show true or false
	 */
	public void setShowTableInformation(boolean show)
	{
		Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); 
		if (tmp != null) 
		{ 
			tmp.setProperty(PROPKEY_SHOW_TABLE_INFO, show); 
			tmp.save(); 
		}
	}

	/**
	 * Get if we should try get table information from the CodeCompletion by calling method 
	 * getToolTipTextForObject(word, fullWord) and show that as a tooltip if the word wasn't 
	 * found in the local dictionary
	 * 
	 * @return show true or false
	 */
	public boolean getShowTableInformation()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_SHOW_TABLE_INFO, DEFAULT_SHOW_TABLE_INFO);
	}
}
