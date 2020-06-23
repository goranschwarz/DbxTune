/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.rtf.RTFEditorKit;

public class ADummyTest2
extends JFrame
{
	public ADummyTest2(String filename) 
	{
		setTitle("RTF Text Application");
		setSize(400, 240);
		setBackground(Color.gray);
		getContentPane().setLayout(new BorderLayout());

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		getContentPane().add(topPanel, BorderLayout.CENTER);

		// Create an RTF editor window
		RTFEditorKit rtf = new RTFEditorKit();
		JEditorPane editor = new JEditorPane();
		editor.setEditorKit(rtf);
		editor.setBackground(Color.white);

		// This text could be big so add a scroll pane
		JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add(editor);
		topPanel.add(scroller, BorderLayout.CENTER);

		// Load an RTF file into the editor
		try {
			FileInputStream fi = new FileInputStream(filename);
			rtf.read(fi, editor.getDocument(), 0);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException e) {
			System.out.println("I/O error");
		} catch (BadLocationException e) {
		}
	}

	public static String rtfToHtml(Reader rtf) 
	throws IOException 
	{
		JEditorPane p = new JEditorPane();
		p.setContentType("text/rtf");
		EditorKit kitRtf = p.getEditorKitForContentType("text/rtf");
		try 
		{
			kitRtf.read(rtf, p.getDocument(), 0);
			kitRtf = null;
			EditorKit kitHtml = p.getEditorKitForContentType("text/html");
			Writer writer = new StringWriter();
			kitHtml.write(writer, p.getDocument(), 0, p.getDocument().getLength());
			return writer.toString();
		} 
		catch (BadLocationException e) 
		{
			e.printStackTrace();
		}
		return null;
	}


	public static void main(String[] args)
	{
		String filename = "C:\\Documents and Settings\\gorans\\My Documents\\RepServer\\admin who.rtf";
		ADummyTest2 mainFrame = new ADummyTest2(filename);
		mainFrame.setVisible(true);
//		try
//		{
//			String htmlText = rtfToHtml(new FileReader(new File(filename)));
//			System.out.println("HTML: "+htmlText);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
	}
}
