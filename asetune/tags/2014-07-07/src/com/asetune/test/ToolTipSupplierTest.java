package com.asetune.test;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.asetune.gui.focusabletip.ResolverReturn;
import com.asetune.gui.focusabletip.ResolverReturn.Type;
import com.asetune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;

public class ToolTipSupplierTest extends JFrame
{
	private static final long	serialVersionUID	= 1L;

	public ToolTipSupplierTest()
	{
		JPanel cp = new JPanel(new BorderLayout());

		RSyntaxTextAreaX textArea = new RSyntaxTextAreaX(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		sp.setFoldIndicatorEnabled(true);
		cp.add(sp);

		ToolTipSupplierDummy xxx = new ToolTipSupplierDummy();
		textArea.setToolTipSupplier(xxx);
		textArea.setToolTipHyperlinkResolver(xxx);

		setContentPane(cp);
		setTitle("Text Editor Demo");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);

	}

	public static void main(String[] args)
	{
		System.setProperty("TipWindow.JEditorPane.replacement", "org.fit.cssbox.swingbox.BrowserPane");
		
		// Start all Swing applications on the EDT.
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				new ToolTipSupplierTest().setVisible(true);
			}
		});
	}

	private class ToolTipSupplierDummy
	implements ToolTipSupplier, ToolTipHyperlinkResolver
	{
		@Override
		public String getToolTipText(RTextArea textArea, MouseEvent e)
		{
			// Force JEditorPane to use a certain font even in HTML.
			// All standard LookAndFeels, even Nimbus (!), define Label.font.
			Font font = UIManager.getFont("Label.font");
			if (font == null) 
			{
				font = new Font("SansSerif", Font.PLAIN, 12);
			}

//			https://code.google.com/p/swingbox-javahelp-viewer/source/browse/src/main/java/org/fit/cssbox/css/CSSNorm.java?r=da0121a8aea206f9ff9950590546dcfe404f4c65
			String headStyleSheet = 
				"<head>" +
				"<style type=\"text/css\">" +
				"    body { " +
//				"           background-color: #d8da3d; " +
//				"           background-color: #fafafa; " +
				"           font-family: " + font.getFamily() + "; " +
				"           font-size: "	+ font.getSize() + "pt; " +
//				"           margin: 2px; " +
				"           line-height: 0.9; "+
				"    }" +
//		        "    h3 { font-size: 1.17em; margin: .83em 0 }"+
//		        "    h3 { font-size: 1.0em; margin: .83em 0 }"+
				"</style>" +
				"</head>";

			String str = "" +
			"<html>" + 
			headStyleSheet +
			"bla bla bla<br>" +
			"<br>" +

			"<h3>Sybase ASE 15.7 Functions</h3>" +
			"<A HREF=\"http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc36271.1572/html/blocks/CHDIHGDF.htm\">http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc36271.1572/html/blocks/CHDIHGDF.htm</A>" +

			"<h3>Replication Server Commands</h3>" +
			"<A HREF=\"http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273713895825.html\">http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273713895825.html</A>" +

			"<h3>Replication Server Command: Configure Connection</h3>" +
			"<A HREF=\"http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273714028699.html\">http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273714028699.html</A>" +

			"<h3>HANA, SP6: Functions</h3>" +
			"<A HREF=\"http://help.sap.com/hana/html/_gsql_functions.html\">http://help.sap.com/hana/html/_gsql_functions.html</A>" +
			"<br>" +

			"<h3>HANA, SP7: Functions</h3>" +
			"<A HREF=\"http://help.sap.com/saphelp_hanaplatform/helpdata/en/20/a24d4b75191014afc5ac3b997d3ce2/content.htm\">http://help.sap.com/saphelp_hanaplatform/helpdata/en/20/a24d4b75191014afc5ac3b997d3ce2/content.htm</A>" +
			"<br>" +
				
			"<h3>Dummy Anchor</h3>" +
			"<A HREF=\"String Functions\">String Functions</A>" +
			"<br>" +

			"<h3>Amazon</h3>" +
			"<A HREF=\"http://www.amazon.com\">http://www.amazon.com</A>" +
			"<br>" +

			"<h3>DN</h3>" +
			"<A HREF=\"http://www.dn.se\">http://www.dn.se</A>" +
			"<br>" +

			"<h3>gorans</h3>" +
			"<A HREF=\"http://gorans.no-ip.org\">http://gorans.no-ip.org</A>" +
			"<br>";

			for (int i=0; i<60; i++)
				str += "row - "+i+"<br>";

			str += "END<br>";
			str += "</html>";

			return str;
		}

		@Override
		public ResolverReturn hyperlinkResolv(HyperlinkEvent event)
		{
			String desc = event.getDescription();
			System.out.println("hyperlinkResolv(): event.getDescription()  ="+event.getDescription());
			System.out.println("hyperlinkResolv(): event.getURL()          ="+event.getURL());
			System.out.println("hyperlinkResolv(): event.getEventType()    ="+event.getEventType());
			System.out.println("hyperlinkResolv(): event.getSourceElement()="+event.getSourceElement());
			System.out.println("hyperlinkResolv(): event.getSource()       ="+event.getSource());
			System.out.println("hyperlinkResolv(): event.toString()        ="+event.toString());

			if (desc.endsWith(".pdf"))
			{
				return new ResolverReturn(event, Type.OPEN_URL_IN_EXTERNAL_BROWSER);
			}

			if (desc.equals("String Functions"))
			{
				return new ResolverReturn(event, "<html><h1>"+desc+"<h1></html>");
			}

//			if (desc.indexOf("help.sap.com") >= 0)
//			{
//				return new ResolverReturn(event, Type.OPEN_URL_IN_EXTERNAL_BROWSER);
//			}

			return new ResolverReturn(event);
		}
	}
}
