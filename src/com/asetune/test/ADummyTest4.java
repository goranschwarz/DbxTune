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

import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.StyleContext;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.asetune.utils.StringUtil;

public class ADummyTest4 implements Runnable
{
	@Override
	public void run()
	{
		Font labelFont = UIManager.getFont("Label.font");
		System.out.println("Label.font is " + labelFont);
		Font textareaFont = UIManager.getFont("TextArea.font");
		System.out.println("TextArea.font is " + textareaFont);
		System.out.println("RSyntaxTextArea.font is " + RSyntaxTextArea.getDefaultFont());
		System.out.println("JTextField.font is " + (new JTextField()).getFont());
		System.out.println("JTextArea.font is " + (new JTextArea()).getFont());
		// tweak:
//		UIManager.put("Label.font", textareaFont);

		JPanel p = new JPanel();
		p.add(new JLabel("JLabel"));
		p.add(new JTextField("JTextField"));
		
		RSyntaxTextArea rsta = new RSyntaxTextArea("RSyntaxtTextArea");
		rsta.setFont( StyleContext.getDefaultStyleContext().getFont("Consolas", Font.PLAIN, labelFont.getSize()));
		p.add(rsta);

		JFrame f = new JFrame("ButtonExample");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(p);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	public static void main(String[] args)
	{
//		final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		for (GraphicsDevice device : environment.getScreenDevices())
//		{
//			System.out.println(device);
//			System.out.println("    getIDstring : " + device.getIDstring());
//			System.out.println("    getType     : " + device.getType());
//		}

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) 
		{	
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value != null && value instanceof Font)
				System.out.println("XXX: key="+StringUtil.left(key.toString(), 40)+"val="+value);
//				UIManager.put (key, f);
		}
		JLabel lll = new JLabel("xxx");
		System.out.println("JLabel.getSize          = "+lll.getSize());
		System.out.println("JLabel.getPreferredSize = "+lll.getPreferredSize());
		System.out.println("-----------------------------------------------");

		SwingUtilities.invokeLater(new ADummyTest4());

	}
}


// At "default" resolution
//XXX: OptionPane.buttonFont                   javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: List.font                               javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: TableHeader.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: Panel.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: TextArea.font                           javax.swing.plaf.FontUIResource[family=Monospaced, name=Monospaced, style=plain, size=13]
//XXX: ToggleButton.font                       javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: ComboBox.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: ScrollPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: Spinner.font                            javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: RadioButtonMenuItem.font                javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: Slider.font                             javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: EditorPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: OptionPane.font                         javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: ToolBar.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: Tree.font                               javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: CheckBoxMenuItem.font                   javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: TitledBorder.font                       javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: FileChooser.listFont                    javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: Table.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: MenuBar.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: PopupMenu.font                          javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: Label.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: MenuItem.font                           javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: MenuItem.acceleratorFont                javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: TextField.font                          javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: TextPane.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: CheckBox.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: ProgressBar.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: FormattedTextField.font                 javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: CheckBoxMenuItem.acceleratorFont        javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Menu.acceleratorFont                    javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: ColorChooser.font                       javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Menu.font                               javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: PasswordField.font                      javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: InternalFrame.titleFont                 javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: OptionPane.messageFont                  javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: RadioButtonMenuItem.acceleratorFont     javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Viewport.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: TabbedPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: RadioButton.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]
//XXX: ToolTip.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=12]
//XXX: Button.font                             javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=11]



// At "250%" resolution
//XXX: OptionPane.buttonFont                   javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: List.font                               javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: TableHeader.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: Panel.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: TextArea.font                           javax.swing.plaf.FontUIResource[family=Monospaced, name=Monospaced, style=plain, size=13]
//XXX: ToggleButton.font                       javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: ComboBox.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: ScrollPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: Spinner.font                            javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: RadioButtonMenuItem.font                javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: Slider.font                             javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: EditorPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: OptionPane.font                         javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: ToolBar.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: Tree.font                               javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: CheckBoxMenuItem.font                   javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: TitledBorder.font                       javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: FileChooser.listFont                    javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: Table.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: MenuBar.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: PopupMenu.font                          javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: Label.font                              javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: MenuItem.font                           javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: MenuItem.acceleratorFont                javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: TextField.font                          javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: TextPane.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: CheckBox.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: ProgressBar.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: FormattedTextField.font                 javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: CheckBoxMenuItem.acceleratorFont        javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Menu.acceleratorFont                    javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: ColorChooser.font                       javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Menu.font                               javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: PasswordField.font                      javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: InternalFrame.titleFont                 javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: OptionPane.messageFont                  javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: RadioButtonMenuItem.acceleratorFont     javax.swing.plaf.FontUIResource[family=Dialog,     name=Dialog,     style=plain, size=12]
//XXX: Viewport.font                           javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: TabbedPane.font                         javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: RadioButton.font                        javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//XXX: ToolTip.font                            javax.swing.plaf.FontUIResource[family=Segoe UI,   name=Segoe UI,   style=plain, size=30]
//XXX: Button.font                             javax.swing.plaf.FontUIResource[family=Tahoma,     name=Tahoma,     style=plain, size=27]
//JLabel.getSize          = java.awt.Dimension[width=0,height=0]
//JLabel.getPreferredSize = java.awt.Dimension[width=39,height=33]
//-----------------------------------------------
//Label.font is javax.swing.plaf.FontUIResource[family=Tahoma,name=Tahoma,style=plain,size=27]
//TextArea.font is javax.swing.plaf.FontUIResource[family=Monospaced,name=Monospaced,style=plain,size=13]
//RSyntaxTextArea.font is javax.swing.plaf.FontUIResource[family=Consolas,name=Consolas,style=plain,size=13]
//JTextField.font is javax.swing.plaf.FontUIResource[family=Tahoma,name=Tahoma,style=plain,size=27]
//JTextArea.font is javax.swing.plaf.FontUIResource[family=Monospaced,name=Monospaced,style=plain,size=13]



// OptionPane.buttonFont                	12 	30
// List.font                            	11 	27
// TableHeader.font                     	11 	27
// Panel.font                           	11 	27
// TextArea.font                        	13 	13
// ToggleButton.font                    	11 	27
// ComboBox.font                        	11 	27
// ScrollPane.font                      	11 	27
// Spinner.font                         	11 	27
// RadioButtonMenuItem.font             	12 	30
// Slider.font                          	11 	27
// EditorPane.font                      	11 	27
// OptionPane.font                      	12 	30
// ToolBar.font                         	12 	30
// Tree.font                            	11 	27
// CheckBoxMenuItem.font                	12 	30
// TitledBorder.font                    	11 	27
// FileChooser.listFont                 	12 	30
// Table.font                           	11 	27
// MenuBar.font                         	12 	30
// PopupMenu.font                       	12 	30
// Label.font                           	11 	27
// MenuItem.font                        	12 	30
// MenuItem.acceleratorFont             	12 	30
// TextField.font                       	11 	27
// TextPane.font                        	11 	27
// CheckBox.font                        	11 	27
// ProgressBar.font                     	11 	27
// FormattedTextField.font              	11 	27
// CheckBoxMenuItem.acceleratorFont     	12 	12
// Menu.acceleratorFont                 	12 	12
// ColorChooser.font                    	12 	12
// Menu.font                            	12 	30
// PasswordField.font                   	11 	27
// InternalFrame.titleFont              	12 	30
// OptionPane.messageFont               	12 	30
// RadioButtonMenuItem.acceleratorFont  	12 	12
// Viewport.font                        	11 	27
// TabbedPane.font                      	11 	27
// RadioButton.font                     	11 	27
// ToolTip.font                         	12 	30
// Button.font                          	11 	27
