package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;
import java.awt.event.MouseEvent;

import org.fife.ui.rtextarea.RTextArea;

import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierTester
extends ToolTipSupplierAbstract
{
	private ToolTipProviderXmlParser _parser = null;
	
	public ToolTipSupplierTester(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
		_parser = new ToolTipProviderXmlParser();
	}

	@Override
	public String getName() 
	{
		return "Tester";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return null;
	}

	@Override
	public String getAllowedChars()
	{
		return "_.*";
	}

	@Override
	public String getToolTipText(RTextArea textArea, MouseEvent e)
	{
//		System.out.println("ToolTipSupplierTester: MouseEvent                   ="+e.getPoint());
//		System.out.println("ToolTipSupplierTester: textArea.getCaretLineNumber()="+textArea.getCaretLineNumber());
//		System.out.println("ToolTipSupplierTester: textArea.getSelectedText()   ="+textArea.getSelectedText());
//		
//		System.out.println("ToolTipSupplierTester: textArea.viewToModel(point)  ="+textArea.viewToModel(e.getPoint()));
//		int dot = textArea.viewToModel(e.getPoint());
		
		String selectedText = textArea.getSelectedText();
		if (selectedText != null)
		{
			String toolTipStr = "<html>";

			TtpEntry ttpEntry = _parser.parseEntry(selectedText);
			if (ttpEntry == null)
				toolTipStr += "<FONT COLOR=\"red\">Parsing probably failed, ttpEntry is NULL.</FONT><br>";
			else
				toolTipStr += ttpEntry.toHtml(false, "<hr>");

//			toolTipStr += "<br>";
//			toolTipStr += "<b><code>##############################################################</code></b><br>";
//			toolTipStr += "<b>Note:</b> The above output was generated from lines in the current file.<br>";
//			toolTipStr += "<pre>";
//			toolTipStr += selectedText.replace("<", "&lt;").replace(">", "&gt;");
//			toolTipStr += "</pre>";
			if (_parser.getException() != null)
			{
				Exception ex=_parser.getException();

				toolTipStr += "<br>";
				toolTipStr += "<b><code>##############################################################</code></b><br>";
				toolTipStr += "<b>WARNING:</b> Caught exception: <code>"+ex+"</code><br>";
				toolTipStr += "<b><code>##############################################################</code></b><br>";
				toolTipStr += "<pre>";
				toolTipStr += StringUtil.stackTraceToString(ex);
				toolTipStr += "</pre>";
			}
			toolTipStr += "</html>";
			
			return toolTipStr;
		}
		

//		return getToolTipText(textArea, e, word, fullWord);
		return "<html>ToolTipSupplierTester<br><br>Select XML Entry (&lt;Entry&gt;....&lt;/Entry&gt;) you want to test, then a tooltip window will popup, so you can check how it looks.<br><br><b>Tip:</b> Use &lt;Ctrl+Space&gt; to add various fields.<html>";
	}
}
