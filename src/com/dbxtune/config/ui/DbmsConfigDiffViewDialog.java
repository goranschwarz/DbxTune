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
package com.dbxtune.config.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.PinstripePainter;

import com.dbxtune.Version;
import com.dbxtune.config.dbms.DbmsConfigDiffEngine.Context;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.gui.SqlTextDialog;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.utils.ColorUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.google.common.collect.MapDifference.ValueDifference;

import net.miginfocom.swing.MigLayout;

public class DbmsConfigDiffViewDialog
//extends JDialog
extends JFrame
implements ActionListener
{
	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_COLUMN_NO_DIFF = "DiffTableModel.column.no.diff.str";
	public static final String PROPKEY_COLOR_BG_DIFF  = "DiffTableModel.color.bg.diff";
	public static final String PROPKEY_COLOR_BG_LEFT  = "DiffTableModel.color.bg.left";
	public static final String PROPKEY_COLOR_BG_RIGHT = "DiffTableModel.color.bg.right";
//	public static final String PROPKEY_COLOR_FG_PK    = "DiffTableModel.color.fg.pk";
	
//	public static final String DEFAULT_COLUMN_NO_DIFF = null;
	public static final String DEFAULT_COLOR_BG_DIFF  = "#ffffcc"; // *Light* yellow
	public static final String DEFAULT_COLOR_BG_LEFT  = "#b3ffcc"; // *Light* green
	public static final String DEFAULT_COLOR_BG_RIGHT = "#ffc2b3"; // *Light* red
//	public static final String DEFAULT_COLOR_FG_PK    = "blue";    // blue
	
//	private static final String columnNoDiffStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLUMN_NO_DIFF, DEFAULT_COLUMN_NO_DIFF);
	private static final String htmlColorDiff   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_BG_DIFF,  DEFAULT_COLOR_BG_DIFF);
	private static final String htmlColorLeft   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_BG_LEFT,  DEFAULT_COLOR_BG_LEFT);
	private static final String htmlColorRight  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_BG_RIGHT, DEFAULT_COLOR_BG_RIGHT);
//	private static final String htmlColorPk     = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_FG_PK,    DEFAULT_COLOR_FG_PK);

	// PANEL: OK-CANCEL
//	private JButton                _ok                   = new JButton("OK");
//	private JButton                _cancel               = new JButton("Cancel");
	private JButton                _close                = new JButton("Close");
	private JButton                _copyTable            = new JButton("Copy Table");
	private JButton                _ddlFixLeft           = new JButton("<< Generate DDL to Sync Left");
	private JButton                _ddlFixRight          = new JButton("Generate DDL to Sync Right >>");

	@SuppressWarnings("unused")
	private Window                 _owner                = null;

	// PANEL: LEFT - ONLINE
	private JLabel     _lOnline_srvName_lbl         = new JLabel("Server Name");
	private JTextField _lOnline_srvName_txt         = new JTextField();

	private JLabel     _lOnline_version_lbl         = new JLabel("Version");
	private JTextField _lOnline_version_txt         = new JTextField();

	private JLabel     _lOnline_url_lbl             = new JLabel("URL");
	private JTextField _lOnline_url_txt             = new JTextField();

	private JLabel     _lOnline_configDate_lbl      = new JLabel("Config Date");
	private JTextField _lOnline_configDate_txt      = new JTextField();

	// PANEL: LEFT - OFFLINE
	private JLabel     _lOffline_srvName_lbl        = new JLabel("Server Name");
	private JTextField _lOffline_srvName_txt        = new JTextField();

	private JLabel     _lOffline_version_lbl        = new JLabel("Version");
	private JTextField _lOffline_version_txt        = new JTextField();

	private JLabel     _lOffline_url_lbl            = new JLabel("URL");
	private JTextField _lOffline_url_txt            = new JTextField();

	private JLabel     _lOffline_collectNameVer_lbl = new JLabel("Collector");
	private JTextField _lOffline_collectNameVer_txt = new JTextField();

	private JLabel     _lOffline_configDate_lbl     = new JLabel("Config Date");
	private JTextField _lOffline_configDate_txt     = new JTextField();
	

	// PANEL: LEFT - ONLINE
	private JLabel     _rOnline_srvName_lbl         = new JLabel("Server Name");
	private JTextField _rOnline_srvName_txt         = new JTextField();

	private JLabel     _rOnline_version_lbl         = new JLabel("Version");
	private JTextField _rOnline_version_txt         = new JTextField();

	private JLabel     _rOnline_url_lbl             = new JLabel("URL");
	private JTextField _rOnline_url_txt             = new JTextField();

	private JLabel     _rOnline_configDate_lbl      = new JLabel("Config Date");
	private JTextField _rOnline_configDate_txt      = new JTextField();

	// PANEL: LEFT - OFFLINE
	private JLabel     _rOffline_srvName_lbl        = new JLabel("Server Name");
	private JTextField _rOffline_srvName_txt        = new JTextField();

	private JLabel     _rOffline_version_lbl        = new JLabel("Version");
	private JTextField _rOffline_version_txt        = new JTextField();

	private JLabel     _rOffline_url_lbl            = new JLabel("URL");
	private JTextField _rOffline_url_txt            = new JTextField();

	private JLabel     _rOffline_collectNameVer_lbl = new JLabel("Collector");
	private JTextField _rOffline_collectNameVer_txt = new JTextField();

	private JLabel     _rOffline_configDate_lbl     = new JLabel("Config Date");
	private JTextField _rOffline_configDate_txt     = new JTextField();
	
	// PANEL: Diff Table
//	private GTable       _tab;
	private JXTable      _tab;
	private GTableFilter _tabFilter;
	private DbmsConfigDiffTableModel _tm;

	private JLabel       _showVal_lbl               = new JLabel("Show Configuration Records for:");
	private JCheckBox    _showVal_same_chk          = new JCheckBox("Same Value on Left and Right",               false);
	private JCheckBox    _showVal_diff_chk          = new JCheckBox("Different",                                  true);
	private JCheckBox    _showVal_left_chk          = new JCheckBox("Left Side Only",                             true);
	private JCheckBox    _showVal_right_chk         = new JCheckBox("Right Side Only",                            true);
//	private JCheckBox    _showVal_same_chk          = new JCheckBox(DbmsConfigDiffTableModel.DIFF_TYPE_Same,      false);
//	private JCheckBox    _showVal_diff_chk          = new JCheckBox(DbmsConfigDiffTableModel.DIFF_TYPE_Differs,   true);
//	private JCheckBox    _showVal_left_chk          = new JCheckBox(DbmsConfigDiffTableModel.DIFF_TYPE_LeftOnly,  true);
//	private JCheckBox    _showVal_right_chk         = new JCheckBox(DbmsConfigDiffTableModel.DIFF_TYPE_RightOnly, true);


	// HOLD THE DIFF Context passed by the constructor
	private Context _diffContext;

	/**
	 * Create a Dialog to view DBMS Configuration differences
	 * @param owner          GUI handle
	 * @param diffContext    What is the Context of what to display
	 */
	public DbmsConfigDiffViewDialog(Window owner, Context diffContext)
	{
		super("DBMS Configuration Difference");
		init(owner, diffContext);
	}

	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		
		_tabFilter.applyFilter();
	}
	
	/** Initialize the Dialog */
	private void init(Window owner, Context diffContext)
	{
		_owner       = owner;
		_diffContext = diffContext;

		initComponents();

		pack();

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});

		loadProps();

		setLocationRelativeTo(owner);

//		setFocus();
	}

	protected void initComponents()
	{
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_diff_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_diff_view_32.png");
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


		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0","",""));   // insets Top Left Bottom Right

		JPanel leftOnlinePanel   = createLeftOnlinePanel  ();
		JPanel leftOfflinePanel  = createLeftOfflinePanel ();
		JPanel rightOnlinePanel  = createRightOnlinePanel ();
		JPanel rightOfflinePanel = createRightOfflinePanel();
		JPanel tablePanel        = createTablePanel();

		if (leftOnlinePanel   != null) panel.add(leftOnlinePanel,       "pushx, growx");
		if (leftOfflinePanel  != null) panel.add(leftOfflinePanel,      "pushx, growx");
		if (rightOnlinePanel  != null) panel.add(rightOnlinePanel,      "pushx, growx, wrap");
		if (rightOfflinePanel != null) panel.add(rightOfflinePanel,     "pushx, growx, wrap");
		panel.add(tablePanel,            "span, push, grow");
//		panel.add(tablePanel,            "dock center");
		panel.add(createOkCancelPanel(), "dock south");

		setContentPane(panel);
	}

	private JPanel createLeftOnlinePanel()
	{
		if ( _diffContext.isLocalOfflineConfig() )
			return null;
		
		IDbmsConfig   dbmsConfig = _diffContext.getLocalDbmsConfig();
		
		JPanel panel  = SwingUtils.createPanel("Left Side Information (online)",  true, new MigLayout());

		panel.add(_lOnline_srvName_lbl,    "");
		panel.add(_lOnline_srvName_txt,    "pushx, growx, wrap");

		panel.add(_lOnline_version_lbl,    "");
		panel.add(_lOnline_version_txt,    "pushx, growx, wrap");

		panel.add(_lOnline_url_lbl,        "");
		panel.add(_lOnline_url_txt,        "pushx, growx, wrap");

		panel.add(_lOnline_configDate_lbl, "");
		panel.add(_lOnline_configDate_txt, "pushx, growx, wrap");

		_lOnline_srvName_txt   .setText( dbmsConfig.getDbmsServerName() );
		_lOnline_version_txt   .setText( dbmsConfig.getDbmsVersionStr() );
		_lOnline_configDate_txt.setText( dbmsConfig.getTimestamp()+"");
		_lOnline_url_txt       .setText( dbmsConfig.getLastUsedUrl() );
		
		_lOnline_srvName_txt       .setCaretPosition(0);
		_lOnline_version_txt       .setCaretPosition(0);
		_lOnline_configDate_txt    .setCaretPosition(0);
		_lOnline_url_txt           .setCaretPosition(0);

		return panel;
	}

	private JPanel createLeftOfflinePanel()
	{
		if ( ! _diffContext.isLocalOfflineConfig() )
			return null;
		
		IDbmsConfig   dbmsConfig = _diffContext.getLocalDbmsConfig();
				
		JPanel panel  = SwingUtils.createPanel("Left Side Information (offline)",  true, new MigLayout());

		panel.add(_lOffline_srvName_lbl,        "");
		panel.add(_lOffline_srvName_txt,        "pushx, growx, wrap");

		panel.add(_lOffline_version_lbl,        "");
		panel.add(_lOffline_version_txt,        "pushx, growx, wrap");

		panel.add(_lOffline_url_lbl,            "");
		panel.add(_lOffline_url_txt,            "pushx, growx, wrap");

		panel.add(_lOffline_collectNameVer_lbl, "");
		panel.add(_lOffline_collectNameVer_txt, "pushx, growx, wrap");

		panel.add(_lOffline_configDate_lbl,     "");
		panel.add(_lOffline_configDate_txt,     "pushx, growx, wrap");

		_lOffline_srvName_txt   .setText( dbmsConfig.getDbmsServerName() );
		_lOffline_version_txt   .setText( dbmsConfig.getDbmsVersionStr() );
		_lOffline_configDate_txt.setText( dbmsConfig.getTimestamp()+"");
		_lOffline_url_txt       .setText( dbmsConfig.getLastUsedUrl() );

		MonRecordingInfo recInfo = dbmsConfig.getOfflineRecordingInfo();
		if (recInfo != null)
			_lOffline_collectNameVer_txt.setText(recInfo.getRecDbxAppName() + " - " + recInfo.getRecDbxVersionStr() );
		
		_lOffline_srvName_txt       .setCaretPosition(0);
		_lOffline_version_txt       .setCaretPosition(0);
		_lOffline_configDate_txt    .setCaretPosition(0);
		_lOffline_collectNameVer_txt.setCaretPosition(0);
		_lOffline_url_txt           .setCaretPosition(0);

		return panel;
	}
	private JPanel createRightOnlinePanel()
	{
		if ( _diffContext.isRemoteOfflineConfig() )
			return null;

		IDbmsConfig   dbmsConfig = _diffContext.getRemoteDbmsConfig();		
		
		JPanel panel  = SwingUtils.createPanel("Right Side Information (online)",  true, new MigLayout());

		panel.add(_rOnline_srvName_lbl,    "");
		panel.add(_rOnline_srvName_txt,    "pushx, growx, wrap");

		panel.add(_rOnline_version_lbl,    "");
		panel.add(_rOnline_version_txt,    "pushx, growx, wrap");

		panel.add(_rOnline_url_lbl,        "");
		panel.add(_rOnline_url_txt,        "pushx, growx, wrap");

		panel.add(_rOnline_configDate_lbl, "");
		panel.add(_rOnline_configDate_txt, "pushx, growx, wrap");

		_rOnline_srvName_txt   .setText( dbmsConfig.getDbmsServerName() );
		_rOnline_version_txt   .setText( dbmsConfig.getDbmsVersionStr() );
		_rOnline_configDate_txt.setText( dbmsConfig.getTimestamp()+"");
		_rOnline_url_txt       .setText( dbmsConfig.getLastUsedUrl() );

		_rOnline_srvName_txt       .setCaretPosition(0);
		_rOnline_version_txt       .setCaretPosition(0);
		_rOnline_configDate_txt    .setCaretPosition(0);
		_rOnline_url_txt           .setCaretPosition(0);

		return panel;
	}
	private JPanel createRightOfflinePanel()
	{
		if ( ! _diffContext.isRemoteOfflineConfig() )
			return null;
		
		IDbmsConfig   dbmsConfig = _diffContext.getRemoteDbmsConfig();		
		
		JPanel panel  = SwingUtils.createPanel("Right Side Information (offline)",  true, new MigLayout());

		panel.add(_rOffline_srvName_lbl,        "");
		panel.add(_rOffline_srvName_txt,        "pushx, growx, wrap");

		panel.add(_rOffline_version_lbl,        "");
		panel.add(_rOffline_version_txt,        "pushx, growx, wrap");

		panel.add(_rOffline_url_lbl,            "");
		panel.add(_rOffline_url_txt,            "pushx, growx, wrap");

		panel.add(_rOffline_collectNameVer_lbl, "");
		panel.add(_rOffline_collectNameVer_txt, "pushx, growx, wrap");

		panel.add(_rOffline_configDate_lbl,     "");
		panel.add(_rOffline_configDate_txt,     "pushx, growx, wrap");

		_rOffline_srvName_txt   .setText( dbmsConfig.getDbmsServerName() );
		_rOffline_version_txt   .setText( dbmsConfig.getDbmsVersionStr() );
		_rOffline_configDate_txt.setText( dbmsConfig.getTimestamp()+"");
		_rOffline_url_txt       .setText( dbmsConfig.getLastUsedUrl() );

		MonRecordingInfo recInfo = dbmsConfig.getOfflineRecordingInfo();
		if (recInfo != null)
			_rOffline_collectNameVer_txt.setText(recInfo.getRecDbxAppName() + " - " + recInfo.getRecDbxVersionStr() );
		
		_rOffline_srvName_txt       .setCaretPosition(0);
		_rOffline_version_txt       .setCaretPosition(0);
		_rOffline_configDate_txt    .setCaretPosition(0);
		_rOffline_collectNameVer_txt.setCaretPosition(0);
		_rOffline_url_txt           .setCaretPosition(0);

		return panel;
	}

	private JPanel createTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Difference Information", true, new MigLayout());

		// Create JTable object, and override createXXX to customize the TableHeaders ToolTip
//		_tab = new JXTable()
//		_tab = new GTable(this.getClass().getSimpleName())
		_tab = new GTable("", null, this.getClass().getSimpleName())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected JTableHeader createDefaultTableHeader()
			{
				JTableHeader tabHeader = new JXTableHeader(getColumnModel())
				{
		            private static final long serialVersionUID = 0L;

					@Override
					public String getToolTipText(MouseEvent e)
					{
						int vcol = getColumnModel().getColumnIndexAtX(e.getPoint().x);
						if (vcol == -1) return null;

						int mcol = convertColumnIndexToModel(vcol);
						if (mcol == -1) return null;

						// Now get the ToolTip from the TableModel
						String tip = null;
						if (getModel() instanceof DbmsConfigDiffTableModel)
						{
							tip = ((DbmsConfigDiffTableModel)getModel()).getToolTipText(mcol); 
						}
						return tip;
					}
				};
				return tabHeader;
			}
		};

		_tab.setSortable(true);
		_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_tab.packAll(); // set size so that all content in all cells are visible
		_tab.setColumnControlVisible(true);
		JScrollPane scroll = new JScrollPane(_tab);


		// Create and set model
		_tm = new DbmsConfigDiffTableModel(_diffContext);
		_tab.setModel(_tm);

		// Sort on column 'ConfigName'
		_tab.setSortOrder(DbmsConfigDiffTableModel.COL_NAME_configName, SortOrder.ASCENDING);

		// Create a table filter: with a checkbox, and filter initialized to "not show unchanged config values"
		_tabFilter = new GTableFilter(_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		_tabFilter.setFilterChkboxSelected(true);
		
		// Register the JCheckBox with the Filter, if anyone clicks on the JCheckBox it will fire a "applyFilter" event
		_tabFilter.addFilterTriggerComponent(_showVal_same_chk);
		_tabFilter.addFilterTriggerComponent(_showVal_diff_chk);
		_tabFilter.addFilterTriggerComponent(_showVal_left_chk);
		_tabFilter.addFilterTriggerComponent(_showVal_right_chk);

		// Create a List that will hold all OR filters for (SAME, DIFFERENT, LEFT_SIDE_ONLY, RIGHT_SIDE_ONLY)
		ArrayList<RowFilter<Object, Object>> orFilters = new ArrayList<RowFilter<Object, Object>>();
		
		// FILTER: SAME
		orFilters.add(new RowFilter<Object, Object>()
		{
			@Override
			public boolean include(Entry<? extends Object, ? extends Object> entry)
			{
				if (_showVal_same_chk.isSelected())
				{
					String diffType = entry.getStringValue(DbmsConfigDiffTableModel.COL_POS_diffType);
					return DbmsConfigDiffTableModel.DIFF_TYPE_Same.equals(diffType);
				}
				return false;
			}
		});

		// FILTER: DIFFERENT
		orFilters.add(new RowFilter<Object, Object>()
		{
			@Override
			public boolean include(Entry<? extends Object, ? extends Object> entry)
			{
				if (_showVal_diff_chk.isSelected())
				{
					String diffType = entry.getStringValue(DbmsConfigDiffTableModel.COL_POS_diffType);
					return DbmsConfigDiffTableModel.DIFF_TYPE_Differs.equals(diffType);
				}
				return false;
			}
		});

		// FILTER: LEFT
		orFilters.add(new RowFilter<Object, Object>()
		{
			@Override
			public boolean include(Entry<? extends Object, ? extends Object> entry)
			{
				if (_showVal_left_chk.isSelected())
				{
					String diffType = entry.getStringValue(DbmsConfigDiffTableModel.COL_POS_diffType);
					return DbmsConfigDiffTableModel.DIFF_TYPE_LeftOnly.equals(diffType);
				}
				return false;
			}
		});

		// FILTER: RIGHT
		orFilters.add(new RowFilter<Object, Object>()
		{
			@Override
			public boolean include(Entry<? extends Object, ? extends Object> entry)
			{
				if (_showVal_right_chk.isSelected())
				{
					String diffType = entry.getStringValue(DbmsConfigDiffTableModel.COL_POS_diffType);
					return DbmsConfigDiffTableModel.DIFF_TYPE_RightOnly.equals(diffType);
				}
				return false;
			}
		});

		// add the above filters in the list as an OR filter
		_tabFilter.setExternalFilter( RowFilter.orFilter(orFilters) );


		// Filter: do NOT show "unchanged" values
//		_tabFilter.setFilterText("where " + DbmsConfigDiffTableModel.COL_NAME_diffType + " != '" + DbmsConfigDiffTableModel.DIFF_TYPE_Same + "'");
		
		// set width of all columns to "max width of current visible values"
		_tab.packAll();


		// STRIPE -- RIGHT or LEFT -- because values values will NOT exists 
		_tab.addHighlighter( new PainterHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String diffType = adapter.getString(adapter.getColumnIndex(DbmsConfigDiffTableModel.COL_NAME_diffType));
				if (DbmsConfigDiffTableModel.DIFF_TYPE_LeftOnly.equals(diffType))
				{
					int mcol = _tab.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_rightValue || mcol == DbmsConfigDiffTableModel.COL_POS_rightIsNDef)
						return true;
				}
				if (DbmsConfigDiffTableModel.DIFF_TYPE_RightOnly.equals(diffType))
				{
					int mcol = _tab.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_leftValue || mcol == DbmsConfigDiffTableModel.COL_POS_leftIsNDef)
						return true;
				}
				return false;
			}
//		}, new PinstripePainter(Color.LIGHT_GRAY,   0, 4, 4))); //   standing/vertical stripes  ||||||||||||||||||||||||||||
//		}, new PinstripePainter(Color.LIGHT_GRAY,  90, 4, 4))); //   laying/horizontal stripes  ============================
		}, new PinstripePainter(Color.LIGHT_GRAY,  45, 4, 4))); //   45 degree stripes          ////////////////////////////
		
		
		
		// LIGHT_GREEN -- BACKGROUND color for LEFT SIDE
		_tab.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String diffType = adapter.getString(DbmsConfigDiffTableModel.COL_POS_diffType);
				if (DbmsConfigDiffTableModel.DIFF_TYPE_LeftOnly.equals(diffType))
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_diffType || mcol == DbmsConfigDiffTableModel.COL_POS_leftValue || mcol == DbmsConfigDiffTableModel.COL_POS_leftIsNDef)
						return true;
				}
				if (DbmsConfigDiffTableModel.DIFF_TYPE_Differs.equals(diffType))
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_leftValue || mcol == DbmsConfigDiffTableModel.COL_POS_leftIsNDef)
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(htmlColorLeft, ColorUtils.VERY_LIGHT_GREEN), null));
		
		// LIGHT_RED -- BACKGROUND color for RIGHT SIDE
		_tab.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String diffType = adapter.getString(DbmsConfigDiffTableModel.COL_POS_diffType);
				if (DbmsConfigDiffTableModel.DIFF_TYPE_RightOnly.equals(diffType))
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_diffType || mcol == DbmsConfigDiffTableModel.COL_POS_rightValue || mcol == DbmsConfigDiffTableModel.COL_POS_rightIsNDef)
						return true;
				}
				if (DbmsConfigDiffTableModel.DIFF_TYPE_Differs.equals(diffType))
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_rightValue || mcol == DbmsConfigDiffTableModel.COL_POS_rightIsNDef)
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(htmlColorRight, ColorUtils.VERY_LIGHT_RED), null));

		// LIGHT_YELLOW -- BACKGROUND color for DIFF column
		_tab.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String diffType = adapter.getString(DbmsConfigDiffTableModel.COL_POS_diffType);
				if (DbmsConfigDiffTableModel.DIFF_TYPE_Differs.equals(diffType))
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_diffType)
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(htmlColorDiff, ColorUtils.VERY_LIGHT_YELLOW), null));

		// BLUE -- FOREGROUND color for "ConfigName" this has differences
		_tab.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String diffType = adapter.getString(DbmsConfigDiffTableModel.COL_POS_diffType);
				if ( ! DbmsConfigDiffTableModel.DIFF_TYPE_Same.equals(diffType) )
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					if (mcol == DbmsConfigDiffTableModel.COL_POS_configName)
						return true;
				}
				return false;
			}
		}, null, Color.BLUE));

		panel.add(_tabFilter,         "pushx, growx, wrap");

		panel.add(_showVal_lbl,       "split"); // Since there are only 1 column, we can't use "span", we must use "split"
		panel.add(_showVal_same_chk,  "");
		panel.add(_showVal_diff_chk,  "");
		panel.add(_showVal_left_chk,  "");
		panel.add(_showVal_right_chk, "wrap");

		panel.add(scroll,             "push, grow");

//		_showVal_same_chk .setBackground(bg);
		_showVal_diff_chk .setBackground(SwingUtils.parseColor(htmlColorDiff,  ColorUtils.VERY_LIGHT_YELLOW));
		_showVal_left_chk .setBackground(SwingUtils.parseColor(htmlColorLeft,  ColorUtils.VERY_LIGHT_GREEN));
		_showVal_right_chk.setBackground(SwingUtils.parseColor(htmlColorRight, ColorUtils.VERY_LIGHT_RED));
		
		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
//		panel.add(_ok,     "push, tag ok");
//		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");
		panel.add(_copyTable,    "");
		panel.add(_ddlFixLeft,   "");
		panel.add(_ddlFixRight,  "");
		panel.add(new JLabel(),  "growx, pushx");
		panel.add(_close,        "tag cancel");

//		_apply.setEnabled(false);

		// Initialize some fields.

		// ADD ACTIONS TO COMPONENTS
//		_ok                  .addActionListener(this);
//		_cancel              .addActionListener(this);
//		_apply               .addActionListener(this);
		_close               .addActionListener(this);
		_copyTable           .addActionListener(this);
		_ddlFixLeft          .addActionListener(this);
		_ddlFixRight         .addActionListener(this);

		_copyTable  .setToolTipText("Copy the above table as a Textual Table into the Copy Paste buffer.");
		_ddlFixLeft .setToolTipText("<html>Generate DDL or SQL Text to set/fix the LEFT side to same value as in the RIGHT side. (only for <i>different</i> rows) <br><b>NOTE</b>: Review the text before apply the changes, (may not work as expected)</html>");
		_ddlFixRight.setToolTipText("<html>Generate DDL or SQL Text to set/fix the RIGHT side to same value as in the LEFT side. (only for <i>different</i> rows) <br><b>NOTE</b>: Review the text before apply the changes, (may not work as expected)</html>");

		// Disable some buttons if we can't generate "correction code" for Configuration
		if ( ! _diffContext.getLocalDbmsConfig().isReverseEngineeringPossible() )
		{
			_ddlFixLeft .setEnabled(false);
			_ddlFixRight.setEnabled(false);
		}
			
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

//		// --- BUTTON: CANCEL ---
//		if (_cancel.equals(source))
//		{
//			setVisible(false);
//		}
//
//		// --- BUTTON: OK ---
//		if (_ok.equals(source))
//		{
//			doApply();
//			saveProps();
//			setVisible(false);
//		}

		// --- BUTTON: CLOSE ---
		if (_close.equals(source))
		{
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: COPY-TABLE ---
		if (_copyTable.equals(source))
		{
			String textTable = SwingUtils.tableToString(_tab);

			if (textTable != null)
			{
				String leftInfo  = 
						  "==============================================================================\n"
						+ "Left Side Information \n"
						+ "------------------------------------------------------------------------------\n"
						+ "Server Name: " + _lOffline_srvName_txt   .getText() + _lOnline_srvName_txt   .getText() + " \n"
						+ "Version:     " + _lOffline_version_txt   .getText() + _lOnline_version_txt   .getText() + " \n"
						+ "URL:         " + _lOffline_url_txt       .getText() + _lOnline_url_txt       .getText() + " \n"
						+ (StringUtil.hasValue(_lOffline_collectNameVer_txt.getText()) ? ("Collector:   " + _lOffline_collectNameVer_txt.getText() + " \n") : "")
						+ "Config Date: " + _lOffline_configDate_txt.getText() + _lOnline_configDate_txt.getText() + " \n"
						+ "------------------------------------------------------------------------------\n"
						+ "\n";
				String rightInfo = 
						  "==============================================================================\n"
						+ "Right Side Information \n"
						+ "------------------------------------------------------------------------------\n"
						+ "Server Name: " + _rOffline_srvName_txt   .getText() + _rOnline_srvName_txt   .getText() + " \n"
						+ "Version:     " + _rOffline_version_txt   .getText() + _rOnline_version_txt   .getText() + " \n"
						+ "URL:         " + _rOffline_url_txt       .getText() + _rOnline_url_txt       .getText() + " \n"
						+ (StringUtil.hasValue(_rOffline_collectNameVer_txt.getText()) ? ("Collector:   " + _rOffline_collectNameVer_txt.getText() + " \n") : "")
						+ "Config Date: " + _rOffline_configDate_txt.getText() + _rOnline_configDate_txt.getText() + " \n"
						+ "------------------------------------------------------------------------------\n"
						+ "\n";
				
				String textToCopy = leftInfo + rightInfo + textTable;
				
				StringSelection data = new StringSelection(textToCopy);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(data, data);
			}
		}

		// --- BUTTON: DDL FIX LEFT ---
		if (_ddlFixLeft.equals(source))
		{
			IDbmsConfig dbmsCdg = _diffContext.getRemoteDbmsConfig();
			String comment = "Source: name='" + dbmsCdg.getDbmsServerName() + "', version='" + dbmsCdg.getDbmsVersionStr() + "'." ;

			String txt = _diffContext.getLocalDbmsConfig().reverseEngineer(createDdlFixMapUseRight(), comment);
			if (StringUtil.hasValue(txt))
			{
				SqlTextDialog dialog = new SqlTextDialog(this, txt);
				dialog.setVisible(true);
			}
		}
		// --- BUTTON: DDL FIX RIGHT ---
		if (_ddlFixRight.equals(source))
		{
			IDbmsConfig dbmsCdg = _diffContext.getLocalDbmsConfig();
			String comment = "Source: name='" + dbmsCdg.getDbmsServerName() + "', version='" + dbmsCdg.getDbmsVersionStr() + "'." ;

			String txt = _diffContext.getRemoteDbmsConfig().reverseEngineer(createDdlFixMapUseLeft(), comment);
			if (StringUtil.hasValue(txt))
			{
				SqlTextDialog dialog = new SqlTextDialog(this, txt);
				dialog.setVisible(true);
			}
		}
	}

	private Map<String, String> createDdlFixMapUseLeft()	{ return createDdlFixMap(true); }
	private Map<String, String> createDdlFixMapUseRight()	{ return createDdlFixMap(false); }

	private Map<String, String> createDdlFixMap(boolean useLeftVal)
	{
		LinkedHashMap<String, String> cfgFix = new LinkedHashMap<>();
		
		for (Entry<String, ValueDifference<String>> e : _diffContext.getEntriesDiffering().entrySet())
		{
			String key  = e.getKey();
			String lVal = e.getValue().leftValue();
			String rVal = e.getValue().rightValue();

			if (useLeftVal)
				cfgFix.put(key, lVal);
			else
				cfgFix.put(key, rVal);
		}
		return cfgFix;
	}
//	private void doApply()
//	{
//	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getClass().getSimpleName() + ".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.setProperty(base + "filter.chk"      , _tabFilter        .isFilterChkboxSelected());
			tmpConf.setProperty(base + "filter.txt"      , _tabFilter        .getText());
			tmpConf.setProperty(base + "filter.chk.same" , _showVal_same_chk .isSelected());
			tmpConf.setProperty(base + "filter.chk.diff" , _showVal_diff_chk .isSelected());
			tmpConf.setProperty(base + "filter.chk.left" , _showVal_left_chk .isSelected());
			tmpConf.setProperty(base + "filter.chk.right", _showVal_right_chk.isSelected());

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(1160);  // initial window with   if not opened before
		int     height    = SwingUtils.hiDpiScale(700);   // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = this.getClass().getSimpleName() + ".";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}

		_tabFilter        .setFilterChkboxSelected(tmpConf.getBooleanProperty(base + "filter.chk"      , true));
		_tabFilter        .setText(                tmpConf.getProperty       (base + "filter.txt"      , ""   ));
		_showVal_same_chk .setSelected(            tmpConf.getBooleanProperty(base + "filter.chk.same" , false));
		_showVal_diff_chk .setSelected(            tmpConf.getBooleanProperty(base + "filter.chk.diff" , true ));
		_showVal_left_chk .setSelected(            tmpConf.getBooleanProperty(base + "filter.chk.left" , true ));
		_showVal_right_chk.setSelected(            tmpConf.getBooleanProperty(base + "filter.chk.right", true ));
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/
}
