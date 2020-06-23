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
package com.asetune.alarm.ui.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.JavaSourceClassLoader;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.MultiResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ExtendedHyperlinkListener;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.alarm.UserDefinedAlarmHandler;
import com.asetune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.asetune.cm.CountersModel;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AlarmDetailUserDefinedPanel
extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(AlarmDetailUserDefinedPanel.class);

	private JTextField      _javaFileName_txt  = new JTextField();
	private RSyntaxTextArea _javaEditor_txt    = new RSyntaxTextArea();
//	private TextEditorPane  _javaEditor_txt    = new TextEditorPane();
	private JButton         _create_but        = new JButton("Create File");
	private JLabel          _compileStatus_lbl = new JLabel("");
//	private JLabel          _info_lbl          = new JLabel("<html><i>Note: A side effect of this is that the file will automatically be saved while editing, so it can be compiled and checked for errors.</i></html>");

	private JButton         _saveFile_but      = new JButton("Save");
	private JButton         _restore_but       = new JButton("Restore");
	private JButton         _removeFile_but    = new JButton("Remove");
	private JLabel          _contentEdited_lbl = new JLabel("Changes has been made in the editor");
	
	private String          _currentCmName     = null;
	private String          _originSourceCode  = "";
	private IUserDefinedAlarmInterrogator _lastCompiledInterrogator = null;

	public AlarmDetailUserDefinedPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Details for User Defined Alarms");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		LanguageSupportFactory.get().register(_javaEditor_txt);
		RTextScrollPane scroll     = new RTextScrollPane(_javaEditor_txt, true);
		ErrorStrip      errorStrip = new ErrorStrip(     _javaEditor_txt);

		add(new JLabel("Filename"), "span, split");
		add(_javaFileName_txt,      "growx, pushx");
		add(_create_but,            "hidemode 2, wrap");
		
//		add(_info_lbl,              "hidemode 3, wrap");
		
		_javaFileName_txt .setEditable(false); // NOT Eenabled
//		_javaFileName_txt .setEnabled(false); // NOT Eenabled
		_create_but       .setVisible(false); // NOT Visible, only if file DO NOT Exists
		_compileStatus_lbl.setVisible(false); // NOT Visible, only if file DO NOT Exists
		_compileStatus_lbl.setForeground(Color.RED);

		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.add(scroll,     BorderLayout.CENTER);
		editorPanel.add(errorStrip, BorderLayout.LINE_END);
		
		add(editorPanel,             "grow, push, wrap");

		add(_compileStatus_lbl,      "hidemode 3, wrap");
		add(_saveFile_but,           "split");
		add(_restore_but,            "");
		add(_removeFile_but,         "");
		add(_contentEdited_lbl,      "hidemode 2, wrap");

		_saveFile_but     .setToolTipText("<html>"
				+ "Save the file<br>"
				+ "It also sets the User Defined Alarm Interrogator in the CM.<br>"
				+ "If last <i>implicit</i> compilatation had errors, the CM's interrrogator will be <i>un set</i> or <i>removed</i>"
				+ "</html>");
		_restore_but      .setToolTipText("Restore file to the original content before you started to make changes to it");
		_removeFile_but   .setToolTipText("REMOVE the source code file");
		_contentEdited_lbl.setToolTipText("<html>"
				+ "The editors content is not the same as before you started to make changes...<br>"
				+ "Hit the 'Restore' button to revert back to original content<br>"
				+ "Or press 'Ctrl-z' to undo some changes<br>"
				+ "</html>");
		_saveFile_but     .setEnabled(false);
		_restore_but      .setEnabled(false);
		_removeFile_but   .setEnabled(false);
		_contentEdited_lbl.setVisible(false);
		
//		_javaFileName_txt.setText();
		_javaEditor_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		scroll.setLineNumbersEnabled(true);
		scroll.setIconRowHeaderEnabled(true);

		_create_but.addActionListener(new CreateAlarmFileAction());
		

		// INSTALL: The Parser that actually is a COMPILER
		_javaEditor_txt.addParser(new LocalJavaCompiler());

		
		// ACTION: 
		_saveFile_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				save();
			}
		});

		// ACTION: 
		_restore_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_javaEditor_txt.setText(_originSourceCode);
				_javaEditor_txt.setCaretPosition(0);
				//_javaEditor_txt.discardAllEdits();
			}
		});

		// ACTION: 
		_removeFile_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String htmlMsg = "<html>"
						+ "Are you sure you want to delete/remove the source code file?<br>"
						+ "file: <code>" + _javaFileName_txt.getText() + "</code><br>"
						+ "<br>"
						+ "Tip: Instead you may want to <b>Disable</b> it, which can be done in the above panel.<br>"
						+ "<br>"
						+ "Do you want to permanently delete the file.<br>"
						+ "</html>";
				int answer = JOptionPane.showConfirmDialog(AlarmDetailUserDefinedPanel.this, htmlMsg, "Remove Source Code File", JOptionPane.YES_NO_OPTION);
				if (answer == 0) 
				{
					File f = new File(_javaFileName_txt.getText());
					f.delete();
					
					CountersModel cm = CounterController.getInstance().getCmByName(_currentCmName);
					setAlarmEntry(_alarmEntry);
//					setCm(cm);
					
					// Also un-set the CM's current interrogator
					cm.setUserDefinedAlarmInterrogator(null);
					_alarmEntry._alarmInterrogator = null;
					_alarmEntry._hasUserDefined = false;
					_alarmEntry._isUserDefinedEnabled = false;
					//_alarmEntry._modified = true;

					firePropertyChange("tableChanged", "javaEditor", _currentCmName);
				}
			}
		});
	}

	private class CreateAlarmFileAction
	implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			String msgHtml = 
				"<html>" +
				   "<h2>Create a User Defined Alarm</h2>" +
				   "The Logic in a <i>User Defined Alarm</b> is based on Java code<br>" +
				   "In here you have full controll of what/when an alarm should be raised.<br>" +
				   "<br>" +
				   "You can choose from some templates below.<br>" +
				   "<ul>" +
				   "  <li><b>Simple</b>    There is only one row in the counter.</li>" +
				   "  <li><b>Multi Row</b> The CM has a table of multiple rows, which you want to loop.</li>" +
				   "  <li><b>Advanced</b>  Multi rows, with extra checks, and raise your own Alarm Event.</li>" +
				   "  <li><b>Cancel</b> and <b>return</b></li>" +
				   "</ul>" +
				"</html>";

			Object[] options = {
					"Simple",
					"Multi Row",
					"Advanced",
					"Cancel"
					};
			int answer = JOptionPane.showOptionDialog(AlarmDetailUserDefinedPanel.this, 
				msgHtml,
				"Create a User Defined Alarm", // title
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,     //do not use a custom Icon
				options,  //the titles of buttons
				options[0]); //default button title

			String template = null;
			if      (answer == 0) { template = "ExampleSimple"; }
			else if (answer == 1) { template = "ExampleMultiRow"; }
			else if (answer == 2) { template = "ExampleAdvanced"; }
			else if (answer == 3) { template = null; }

			if (template != null)
			{
				try 
				{
					URL url = AlarmDetailUserDefinedPanel.class.getResource("examples/"+template+".txt");
					if (url == null)
					{
						_logger.error("Can't find the resource for class='"+AlarmDetailUserDefinedPanel.class+"', filename='"+template+".txt'.");
						return;
					}
					String fileContent = IOUtils.toString(url); 
					
					// Replace the template names with "runtime values"
					String replacePackage = "package com.asetune.alarm.ui.config.examples;";
					String replaceClass   = "public class " + template;
					String replaceClass2  = template + ".class";

					fileContent = fileContent.replace(replacePackage, "package " + Version.getAppName().toLowerCase() + ";");
					fileContent = fileContent.replace(replaceClass,   "public class " + _currentCmName);
					fileContent = fileContent.replace(replaceClass2,  _currentCmName + ".class");

//					String javaFileName = _javaFileName_txt.getText();
//					FileUtils.writeStringToFile(new File(javaFileName), fileContent);
					_javaEditor_txt.setText(fileContent);
//					_javaEditor_txt.load(FileLocation.create(javaFileName), null);
					_javaEditor_txt.setCaretPosition(0);
					_javaEditor_txt.discardAllEdits();
										
					//_originSourceCode = fileContent;
					
					_javaFileName_txt.setForeground(UIManager.getColor("TextField.foreground"));
					_javaFileName_txt.setToolTipText("Name of the file");
					_create_but.setVisible(false);
				}
				catch (IOException e1) 
				{ 
					e1.printStackTrace(); 
				}
			}
		}
	}

	private class LocalJavaCompiler
	implements Parser
	{
//		TextEditorPane  _textarea;
//		JLabel          _compileStatus;
//	
//		public LocalJavaCompiler(TextEditorPane textarea, JLabel compileStatus)
//		{
//			_textarea      = textarea;
//			_compileStatus = compileStatus;
//		}

		@Override
		public ParseResult parse(RSyntaxDocument doc, String style)
		{
			DefaultParseResult pr = new DefaultParseResult(this);
			try
			{
				boolean isDirty = isDirty();
				_saveFile_but     .setEnabled(isDirty);
				_restore_but      .setEnabled(isDirty);
				_removeFile_but   .setEnabled( ! _create_but.isVisible() );
				_contentEdited_lbl.setVisible(isDirty);

				pr.clearNotices();
				_compileStatus_lbl.setText("");
				_compileStatus_lbl.setVisible(false);

				// If the create_but IS visible... then there are nothing to compile... 
				if ( _create_but.isVisible() )
					return pr;

				if ( ! UserDefinedAlarmHandler.hasInstance() )
					return pr;


//				String currentCmName = (String) _javaEditor_txt.getClientProperty("currentCmName");
//				if (currentCmName == null)
//					return pr;

//				// Save the file...
//				try { _javaEditor_txt.save(); }
//				catch (IOException e)
//				{
//					System.out.println("PROBLEMS SAVING FILE.");
//					e.printStackTrace();
//				}
				
				File srcDir = new File(UserDefinedAlarmHandler.getInstance().getSourceDir());
				File[] srcDirs = new File[]{ srcDir };
				
				String javaClassName    = UserDefinedAlarmHandler.getInstance().getJavaClassName(_currentCmName);
				String javaBaseFileName = UserDefinedAlarmHandler.getInstance().getJavaBaseFileName(_currentCmName);

				try
				{
					// Get source
					String javaCode = _javaEditor_txt.getText();

					// Put source in a Map<classFileName, byre[]>
					// classFileName should look like: "package/ClassName.java"
					Map<String, byte[]> srcStrings = new HashMap<>();
					srcStrings.put(javaBaseFileName, javaCode.getBytes()); 
					
					// set the "path" where to find java source code (Resource Finder) 
					//  - first:  The in-memory HashMap<"pgkname/CmName.java", "source-code-content">
					//  - second: DIRECTORY: ${DBXTUNE_HOME}/resources/alarm-handler-src
					List<ResourceFinder> resourceFinders = new ArrayList<>();
					resourceFinders.add( new MapResourceFinder(srcStrings));
					resourceFinders.add( new PathResourceFinder(srcDirs) );
					MultiResourceFinder mrf = new MultiResourceFinder(resourceFinders);

					// Then Create a ClassLoader
					JavaSourceClassLoader cl = new JavaSourceClassLoader(getClass().getClassLoader(), mrf, null);

					// Compile and instantiate the class
					IUserDefinedAlarmInterrogator interrogator = (IUserDefinedAlarmInterrogator) cl.loadClass(javaClassName).newInstance();
					_lastCompiledInterrogator = interrogator;
				}
				catch(ClassNotFoundException ex)
				{
					_lastCompiledInterrogator = null;

					// If it's a compile error the getCouse() will contain a CompileException
					Throwable cause = ex.getCause();
					if (cause instanceof CompileException)
					{
						throw (CompileException) cause;
					}
					_compileStatus_lbl.setText("ClassNotFoundException: " + ex.getMessage());
					_compileStatus_lbl.setVisible(true);
				}
				catch (InstantiationException ex)
				{
					_logger.error("Problems compiling User Defined Alarm Interrogater", ex);
				}
				catch (IllegalAccessException ex)
				{
					_logger.error("Problems compiling User Defined Alarm Interrogater", ex);
				}
				
				return pr;

//				String javaCode = _javaEditor_txt.getText();
//
//				SimpleCompiler sc = new SimpleCompiler();
//				sc.cook(javaCode);


//				throw new Exception("Dummy");
//				return pr;
			}
			catch(CompileException e)
			{
				String msg = e.getMessage();
				
				_compileStatus_lbl.setText("Compilation Errors: "+msg);
				_compileStatus_lbl.setToolTipText(msg);
				_compileStatus_lbl.setVisible(true);

				int line = 0;
				int offset = -1;
				int length = -1;
				// org.codehaus.commons.compiler.CompileException: Line 2, Column 13: Non-abstract class "xxx.XxxYyy" must implement method "public abstract void com.asetune.alarm.IUserDefinedAlarmInterrogator.interrogateCounterData(com.asetune.cm.CountersModel)"
				if (msg.indexOf("Line ") != -1)
				{
					int linePos    = msg.indexOf("Line ");
					int colonPos   = msg.indexOf(", Column ", linePos);
					int firstComma = msg.indexOf(",",         linePos);
					int firstColon = msg.indexOf(":",         linePos);
					if ( linePos != -1 && colonPos != -1 && firstComma != -1 && firstColon != -1 )
					{
						linePos += "Line ".length();
						colonPos += ", Column ".length();

						line   = StringUtil.parseInt( msg.substring(linePos, firstComma),  1) - 1; // line starts at 0
						offset = StringUtil.parseInt( msg.substring(colonPos, firstColon), 0) - 1; // offset starts at 0
						
//						if (offset != -1)
//							try { length = _textarea.getLineEndOffset(line); } catch(BadLocationException ignore) {}
					}
				}
//System.out.println("xxxxx: DefaultParserNotice: line="+line+", offset="+offset+", length="+length);
				DefaultParserNotice notice;
				if (length == -1)
					notice = new DefaultParserNotice(this, msg, line);
				else
					notice = new DefaultParserNotice(this, msg, line, offset, length);

				pr.addNotice(notice);
				pr.setError(e);
				return pr;
			}
		}
		
		@Override
		public boolean isEnabled()
		{
			return true;
		}
		
		@Override
		public URL getImageBase()
		{
			return null;
		}
		
		@Override
		public ExtendedHyperlinkListener getHyperlinkListener()
		{
			return null;
		}
	}


	private AlarmEntry _alarmEntry = null;
	public void setAlarmEntry(AlarmEntry alarmEntry)
	{
		if ( ! UserDefinedAlarmHandler.hasInstance() )
			return;

		_alarmEntry =alarmEntry;
		String cmName = _alarmEntry._cmName;

		_originSourceCode = "";

		_currentCmName = cmName;
		_javaEditor_txt.putClientProperty("currentCmName", cmName);
		
		String javaFileName = UserDefinedAlarmHandler.getInstance().getJavaFileName(cmName);
		_javaFileName_txt.setText(javaFileName);
//		_javaFileName_txt.setBackground(UIManager.getColor("TextField.background"));
		_javaFileName_txt.setForeground(UIManager.getColor("TextField.foreground"));
		_javaFileName_txt.setToolTipText("Name of the file");
		_create_but.setVisible(false);
		
		try
		{
			File f = new File(javaFileName);
			if ( ! f.exists() )
			{
//				System.out.println("FILE do not exists '"+f.getAbsolutePath()+"'");
				throw new FileNotFoundException("File '"+f.getAbsolutePath()+"' does not exist");
				//String dummy = FileUtils.readFileToString(f); // This will throw "FileDoNotExists"
			}

//			_javaEditor_txt.load(FileLocation.create(javaFileName), null);

			String javaSourceCode = FileUtils.readFileToString(f);
			_javaEditor_txt.setText(javaSourceCode);
			_javaEditor_txt.setCaretPosition(0);
			_javaEditor_txt.discardAllEdits();
		}
		catch (IOException ex) 
		{
			_create_but.setVisible(true);

//			try {
//				_javaEditor_txt.load(FileLocation.create("Untitled.txt"), null);
//			} catch (IOException ex2) {
//				ex2.printStackTrace();
//			}

			_javaEditor_txt.setText("/* "+ex+" */");
			_javaEditor_txt.setCaretPosition(0);
			_javaEditor_txt.discardAllEdits();
			
//				_javaFileName_txt.setBackground(TrendGraphColors.VERY_LIGHT_RED);
			_javaFileName_txt.setForeground(Color.RED);
			_javaFileName_txt.setToolTipText(""+ex);
		}
//		System.out.println("xxx=" + _javaEditor_txt.getFileFullPath());

		// Remember original source code... used in isDirty()
		_originSourceCode = _javaEditor_txt.getText();
	}

//	public void setCm(CountersModel cmx)
//	{
//		_originSourceCode = "";
//
//		if (cm != null)
//		{
//			_currentCmName = cm.getName();
//			_javaEditor_txt.putClientProperty("currentCmName", cm.getName());
//			
//			String javaFileName = UserDefinedAlarmHandler.getInstance().getJavaFileName(cm);
//			_javaFileName_txt.setText(javaFileName);
////			_javaFileName_txt.setBackground(UIManager.getColor("TextField.background"));
//			_javaFileName_txt.setForeground(UIManager.getColor("TextField.foreground"));
//			_javaFileName_txt.setToolTipText("Name of the file");
//			_create_but.setVisible(false);
//			
//			try
//			{
//				File f = new File(javaFileName);
//				if ( ! f.exists() )
//				{
////					System.out.println("FILE do not exists '"+f.getAbsolutePath()+"'");
//					throw new FileNotFoundException("File '"+f.getAbsolutePath()+"' does not exist");
//					//String dummy = FileUtils.readFileToString(f); // This will throw "FileDoNotExists"
//				}
//
////				_javaEditor_txt.load(FileLocation.create(javaFileName), null);
//
//				String javaSourceCode = FileUtils.readFileToString(f);
//				_javaEditor_txt.setText(javaSourceCode);
//				_javaEditor_txt.setCaretPosition(0);
//				_javaEditor_txt.discardAllEdits();
//			}
//			catch (IOException ex) 
//			{
//				_create_but.setVisible(true);
//
////				try {
////					_javaEditor_txt.load(FileLocation.create("Untitled.txt"), null);
////				} catch (IOException ex2) {
////					ex2.printStackTrace();
////				}
//
//				_javaEditor_txt.setText("/* "+ex+" */");
//				_javaEditor_txt.setCaretPosition(0);
//				_javaEditor_txt.discardAllEdits();
//				
////				_javaFileName_txt.setBackground(TrendGraphColors.VERY_LIGHT_RED);
//				_javaFileName_txt.setForeground(Color.RED);
//				_javaFileName_txt.setToolTipText(""+ex);
//			}
////			System.out.println("xxx=" + _javaEditor_txt.getFileFullPath());
//
//			// Remember original source code... used in isDirty()
//			_originSourceCode = _javaEditor_txt.getText();
//		}
//		else
//		{
//			
//		}
//	}
	

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return ! _originSourceCode.equals(_javaEditor_txt.getText());
	}


	/**
	 * return true if this panel has any configurable data
	 * @return
	 */
	public boolean hasData()
	{
		return ! _create_but.isVisible();
	}


	/**
	 * Save settings
	 */
	public void save()
	{
		String javaFileName = _javaFileName_txt.getText();
		String content      = _javaEditor_txt.getText(); 

		try
		{
			FileUtils.writeStringToFile(new File(javaFileName), content);

			_originSourceCode = content;

			CountersModel cm = CounterController.getInstance().getCmByName(_currentCmName);
			cm.setUserDefinedAlarmInterrogator(_lastCompiledInterrogator);
			_alarmEntry._alarmInterrogator = _lastCompiledInterrogator;
			_alarmEntry._hasUserDefined = true;
			_alarmEntry._isUserDefinedEnabled = true;
			//_alarmEntry._modified = true;

			// Force reparsing...
			for (int p=0; p<_javaEditor_txt.getParserCount(); p++)
				try { _javaEditor_txt.forceReparsing(p); } catch (Throwable ignore) {} // protect from errors in RSyntaxTextArea

			firePropertyChange("tableChanged", "javaEditor", _currentCmName);
		}
		catch (IOException ex)
		{
			String htmlMsg = "<html>"
					+ "Problems saving the file: " + javaFileName + "<br>"
					+ "<br>"
					+ "Caught: " + ex + "<br>"
					+ "</html>";
			SwingUtils.showErrorMessage(this, "Problems Saving File", htmlMsg, ex);
		}
	}




	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	///// Some TEST code for recomile...
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static String createSourceCode()
	{
		boolean returnStringBuilder = true;
		returnStringBuilder = false;

		if (returnStringBuilder)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("package asetune; \n");
			sb.append(" \n");
			sb.append("import com.asetune.alarm.IUserDefinedAlarmInterrogator; \n");
			sb.append("import com.asetune.cm.CountersModel; \n");
			sb.append(" \n");
			sb.append("public class CmEngines \n");
			sb.append("implements IUserDefinedAlarmInterrogator \n");
			sb.append("{ \n");
			sb.append("	public void interrogateCounterData(CountersModel cm) \n");
			sb.append("	{ \n");
			sb.append("		System.out.println(\"11111111111111111111111111111: CmEngines --- StringBuilder\");\n");
			sb.append("	} \n");
			sb.append("} \n");

			System.out.println("##### StringBuilder ################################");
			System.out.println(sb.toString());
			System.out.println("##### StringBuilder ################################");

			return sb.toString();
		}
		else
		{
			File f = new File("C:\\projects\\AseTune\\resources\\alarm-handler-src2\\asetune\\CmEngines.txt");
			try
			{
				String content = FileUtils.readFileToString(f);
				System.out.println("##### FileUtils.readFileToString("+f+") ################################");
				System.out.println(content);
				System.out.println("##### FileUtils.readFileToString("+f+") ################################");
				return content;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return "xxx";
	}
	
	public static void main(String[] args)
	{
		File srcDir = new File("C:\\projects\\AseTune\\resources\\alarm-handler-src2");
		File[] srcDirs = new File[]{ srcDir };
		
		String className     = "asetune.CmEngines";
		String classFileName = "asetune/CmEngines.java";

		try
		{
			System.out.println("LOADING CLASS: '"+className+"', from scrDir='"+srcDir+"'.");

			Map<String, byte[]> srcStringsMap = new HashMap<>();
			String xxx= createSourceCode();
//System.out.println("XXX = "+xxx);
			srcStringsMap.put(classFileName, xxx.getBytes()); // classFileName should look like: "package/ClassName.java"
			
			List<ResourceFinder> resourceFinders = new ArrayList<>();
			resourceFinders.add( new MapResourceFinder(srcStringsMap));
			resourceFinders.add( new PathResourceFinder(srcDirs) );
			MultiResourceFinder mrf = new MultiResourceFinder(resourceFinders);
			
			JavaSourceClassLoader cl = new JavaSourceClassLoader(ClassLoader.getSystemClassLoader(), mrf, null);

//			StringBuilder sb = new StringBuilder();
//			String srcStrings = sb.toString();
//			List<ResourceFinder> resourceFinders = new ArrayList<>();
//			resourceFinders.add( new MapResourceFinder(srcStrings));
//			resourceFinders.add( new PathResourceFinder(srcDirs) );
//			MultiResourceFinder mrf = new MultiResourceFinder(resourceFinders);
//			
//	
////			JavaSourceClassLoader cl = new JavaSourceClassLoader(ClassLoader.getSystemClassLoader(), srcDirs, null);
//			JavaSourceClassLoader cl = new JavaSourceClassLoader(ClassLoader.getSystemClassLoader(), mrf, null);
////cl.setDebuggingInfo(true, true, true);
			IUserDefinedAlarmInterrogator interrogator = (IUserDefinedAlarmInterrogator) cl.loadClass(className).newInstance();
			interrogator.interrogateCounterData(null);
			System.out.println("---END---- LOADING CLASS: '"+className+"', from scrDir='"+srcDir+"'.");
		}
		catch(ClassNotFoundException ex)
		{
			Throwable cause = ex.getCause();
			if (cause instanceof CompileException)
			{
				System.out.println("111: CompileException: "+cause);
				System.exit(1);
			}

			System.out.println("222: ClassNotFoundException: "+ex);
			System.exit(1);
		}
		catch (InstantiationException ex)
		{
			ex.printStackTrace();
		}
		catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
		
	}
}






/*------------------------------------------------------------------
 * Below is an Example of a IUserDefinedAlarmInterrogator
 *------------------------------------------------------------------
package asetune;

import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.cm.CountersModel;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventHighCpuUtilization;

public class CmXxx
implements IUserDefinedAlarmInterrogator
{
	public void interrogateCounterData(CountersModel cm)
	{
		// No RATE data, get out of here (on first sample we will only have ABS data)
		if ( ! cm.hasRateData() )
			return;

		// If we havn't got any alarm handler; exit
		if ( ! AlarmHandler.hasInstance())
			return;

		// If we havn't got all desired column names; exit
		if ( ! cm.hasColumns("IdleCPUTimePct", "IdleCPUTimePct") )
			return;
		
		// Your logic
		int cpuUsagePct = (int) (100.0 - cm.getRateValueAvg("IdleCPUTimePct"));
		if (cpuUsagePct > 50)
		{
			// Create the alarm and add/send it to the AlrmHandler
			AlarmEvent alarm = new AlarmEventHighCpuUtilization(cm, cpuUsagePct);
			AlarmHandler.getInstance().addAlarm(alarm);
		}
	}
}
//------------------------------------------------------------------*/

