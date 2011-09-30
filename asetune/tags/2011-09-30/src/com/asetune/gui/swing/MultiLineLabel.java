/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.swing;

import javax.swing.JLabel;

public class MultiLineLabel
    extends JLabel
{
    private static final long serialVersionUID = 1L;
    private boolean _internalHtmlTran = false;

	public MultiLineLabel()
	{
	}

	public MultiLineLabel(final String s)
	{
		super(s);
		setText(s);
	}

	/**
	 * If the text contains '\n' <code>newline</code> it will be translated into <code>html</code>
	 * and the newline character will translated into <code><br></code>.
	 */
	public void setText(String text)
	{
		_internalHtmlTran = false;
		if (text == null)
			text = "";

		if (text.trim().startsWith("<html>"))
		{
			super.setText(text);
			return;
		}

		_internalHtmlTran = true;
		if (text.indexOf("\n") < 0)
		{
			super.setText("<html>"+text+"</html>");
			return;
		}

		String[] textBr = text.split("\n");
		StringBuilder sb = new StringBuilder();

		sb.append("<html>");
		for (int i=0; i<textBr.length; i++)
		{
			sb.append(textBr[i]);
			sb.append("<br>");
		}
		sb.append("</html>");
		
		super.setText(sb.toString());
	}

	/**
	 * Get the text from the label, if we have added any HTML tags, remove them 
	 * (<br> is restored into '\n')
	 */
	public String getTextStripHtml()
	{
		if ( ! _internalHtmlTran )
			return super.getText();

		String text = super.getText();
		if (text == null)
			return null;

		text = text.replaceAll("\\<br\\>", "\n");  // replace all <br> with '\n'
		text = text.replaceAll("\\<html\\>", "");  // strip <html> from the description.
		text = text.replaceAll("\\</html\\>", ""); // strip </html> from the description.
//		text = text.replaceAll("\\<.*?\\>", "");   // STRIP ALL HTML Tags from the description.
		return text;
	}
}
