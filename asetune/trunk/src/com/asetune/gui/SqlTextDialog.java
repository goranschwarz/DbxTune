package com.asetune.gui;

import java.awt.BorderLayout;
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
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class SqlTextDialog
extends JDialog
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(DdlViewer.class);
	private static final long serialVersionUID = 1L;

	private JPanel          _top_panel         = null;
	private JPanel          _ok_panel          = null;

	private JButton         _close_but         = new JButton("Close");

	private RSyntaxTextArea _object_txt        = new RSyntaxTextArea();
	private RTextScrollPane _object_scroll     = new RTextScrollPane(_object_txt);

	private JPopupMenu      _tablePopupMenu    = null;


	public SqlTextDialog(Window owner)
	{
		this(owner, null, null);
	}

	public SqlTextDialog(Window owner, String text)
	{
		this(owner, text, null);
	}

	public SqlTextDialog(Window owner, String text, String sqlDialect)
	{
		super();
		
		_owner      = owner;
		_sqlText    = text;
		_sqlDialect = sqlDialect;
		
		if (StringUtil.isNullOrBlank(_sqlDialect))
			_sqlDialect = AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL;

		init();
	}

	private Window _owner      = null;
	private String _sqlText    = null;
	private String _sqlDialect = null;

	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init()
	{
		setTitle("DDL View"); // Set window title
		
		// Set the icon
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ddlgen_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/ddlgen_32.png");
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
		
		_object_txt.setSyntaxEditingStyle(_sqlDialect);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_object_scroll, this);

		_object_txt.setText(_sqlText);

		panel.add(_object_scroll, "push, grow, wrap");

		return panel;
	}

	private JPanel createOkPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
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
    		conf.setLayoutProperty("ddlgen.dialog.window.width",  this.getSize().width);
    		conf.setLayoutProperty("ddlgen.dialog.window.height", this.getSize().height);
    		conf.setLayoutProperty("ddlgen.dialog.window.pos.x",  this.getLocationOnScreen().x);
    		conf.setLayoutProperty("ddlgen.dialog.window.pos.y",  this.getLocationOnScreen().y);
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
		int width  = conf.getLayoutProperty("ddlgen.dialog.window.width",  SwingUtils.hiDpiScale(600));
		int height = conf.getLayoutProperty("ddlgen.dialog.window.height", SwingUtils.hiDpiScale(240));
		int x      = conf.getLayoutProperty("ddlgen.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("ddlgen.dialog.window.pos.y",  -1);
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