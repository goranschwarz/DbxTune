/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.gui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.Version;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class TextDialog
extends JDialog
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(TextDialog.class);
	private static final long serialVersionUID = 1L;

	private JPanel          _top_panel         = null;
	private JPanel          _ok_panel          = null;

	private JButton         _close_but         = new JButton("Close");

	private RSyntaxTextAreaX _object_txt       = new RSyntaxTextAreaX();
	private RTextScrollPane  _object_scroll    = new RTextScrollPane(_object_txt);

	private JPopupMenu      _tablePopupMenu    = null;


	public TextDialog(Window owner, String title, String syntax, String text)
	{
		super();
		
		_owner  = owner;
		_title  = title;
		_syntax = syntax;
		_text   = text;
		
		if (StringUtil.isNullOrBlank(_title))
			_title = "Text";

		if (StringUtil.isNullOrBlank(_syntax))
			_syntax = AsetuneSyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;

		init();
	}

	private Window _owner  = null;
	private String _title  = null;
	private String _syntax = null;
	private String _text   = null;

	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init()
	{
		setTitle(_title); // Set window title
		
		// Set the icon
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/textview_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/textview_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

//		setLayout( new BorderLayout() );
		setLayout( new MigLayout("insets 0 0 0 0") );
		
		loadProps();

		_top_panel = createTopPanel();
		_ok_panel  = createOkPanel();

		add(_top_panel, "grow, push, wrap");
		add(_ok_panel,  "pushx, growx, wrap");
		
		pack();
		getSavedWindowProps();

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				distroy();
			}
		});
	}

	/** call this when window is closing */
	private void distroy()
	{
		// cleanup...
		dispose();
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		if (StringUtil.hasValue(_syntax))
		{
			_object_txt.setSyntaxEditingStyle(_syntax);
			RSyntaxUtilitiesX.installRightClickMenuExtentions(_object_scroll, this);
		}

		_object_txt.setText(_text);

		panel.add(_object_scroll, "push, grow, wrap");

		return panel;
	}

	private JPanel createOkPanel()
	{
		JPanel panel = SwingUtils.createPanel("ok", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
//		panel.setLayout(new MigLayout());
		
		panel.add(new JLabel(),   "pushx, growx");
		panel.add(_close_but,     "gapright 5, tag right");

		_close_but.addActionListener(this);

		return panel;
	}


	/**
	 * IMPLEMENTS: ActionListener
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_close_but.equals(source))
		{
			saveProps();
			setVisible(false);
		}

		saveProps();
	}

	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		if (isVisible())
		{
    		conf.setLayoutProperty("textview.dialog.window.width",  this.getSize().width);
    		conf.setLayoutProperty("textview.dialog.window.height", this.getSize().height);
    		conf.setLayoutProperty("textview.dialog.window.pos.x",  this.getLocationOnScreen().x);
    		conf.setLayoutProperty("textview.dialog.window.pos.y",  this.getLocationOnScreen().y);
		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
	}
	private void getSavedWindowProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		//----------------------------------
		// TAB: s
		//----------------------------------
		int width  = conf.getLayoutProperty("textview.dialog.window.width",  SwingUtils.hiDpiScale(600));
		int height = conf.getLayoutProperty("textview.dialog.window.height", SwingUtils.hiDpiScale(240));
		int x      = conf.getLayoutProperty("textview.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("textview.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
}
