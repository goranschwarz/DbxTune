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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.gui.DdlViewerModel2.DbEntry;
import com.dbxtune.gui.DdlViewerModel2.ObjectEntry;
import com.dbxtune.gui.DdlViewerModel2.TypeEntry;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.TreeTableNavigationEnhancer;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.gui.swing.WaitForExecDialog.BgExecutor;
import com.dbxtune.pcs.DdlDetails;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class DdlViewer
extends JDialog
implements ActionListener, TreeTableNavigationEnhancer.ActionExecutor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	private JPanel          _treeViewPanel     = null;
	private JPanel          _detailesPanel     = null;
	private GTabbedPane     _tabbedPane        = null;
	private JSplitPane      _splitPane         = null;

	private JPanel          _object_panel      = null;
	private JPanel          _optDiag_panel     = null;
	private JPanel          _depends_panel     = null;
	private JPanel          _extraInfo_panel   = null;

	private JLabel          _searchFor_lbl     = new JLabel("Search");
	private JTextField      _searchFor_txt     = new JTextField();

	private JLabel          _dependParent_lbl  = new JLabel("Parent");
	private JTextField      _dependParent_txt  = new JTextField();
	private JLabel          _dependsList_lbl   = new JLabel("Depends List");
	private JTextField      _dependsList_txt   = new JTextField();
	private JLabel          _source_lbl        = new JLabel("Source");
	private JTextField      _source_txt        = new JTextField();

	private RSyntaxTextAreaX _object_txt       = new RSyntaxTextAreaX();
	private RTextScrollPane  _object_scroll    = new RTextScrollPane(_object_txt);

	private RSyntaxTextAreaX _optDiag_txt      = new RSyntaxTextAreaX();
	private RTextScrollPane  _optDiag_scroll   = new RTextScrollPane(_optDiag_txt);

	private RSyntaxTextAreaX _depends_txt      = new RSyntaxTextAreaX();
	private RTextScrollPane  _depends_scroll   = new RTextScrollPane(_depends_txt);

	private RSyntaxTextAreaX _extraInfo_txt    = new RSyntaxTextAreaX();
	private RTextScrollPane  _extraInfo_scroll = new RTextScrollPane(_extraInfo_txt);

	private JXTreeTable     _treeTable         = null;
	private JPopupMenu      _tablePopupMenu    = null;

	public DdlViewer()
	{
		super();
		init();
	}


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
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
//		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool.png");
//		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool_32.png");
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ddl_view_tool.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/ddl_view_tool_32.png");
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

		setLayout( new BorderLayout() );
		
		loadProps();

		_treeViewPanel   = createTreeViewPanel();
		_detailesPanel   = createDetailesPanel();

		_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _treeViewPanel, _detailesPanel);
		add(_splitPane, BorderLayout.CENTER);

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

	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);

		// Change focus to the table... so that keyboard navigation is possible
		setFocus();
	}

	/** call this when window is closing */
	private void distroy()
	{
		// Memory doesn't seems to be released, I don't know why, so lets try to reset some data structures
//		_traceOut_txt    = null;
//		_traceOut_scroll = null;
//
//		_proc_txt    = null;
//		_proc_scroll = null;
//		_procCache   = null;
		
		dispose();
	}

	private JPanel createTreeViewPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

//		JScrollPane scroll = new JScrollPane(createTreeSpSysmon());
		JScrollPane scroll = new JScrollPane(createTreeTable());
		
		panel.add(_searchFor_lbl, "gap 5 5 5 0, split");
		panel.add(_searchFor_txt, "pushx, growx, wrap");
		
		panel.add(scroll, "push, grow, wrap");
		
		_searchFor_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TreeTableModel ttm = _treeTable.getTreeTableModel();
				if (ttm != null && ttm instanceof DdlViewerModel2)
				{
					DdlViewerModel2 model = (DdlViewerModel2) ttm;

					String text = _searchFor_txt.getText().trim();
					DdlDetails ddlDetails = DdlViewerSearchForDialog.open(DdlViewer.this, model.getDdlDetails(), text);
					if (ddlDetails != null)
						setViewEntry(ddlDetails.getDbname(), ddlDetails.getObjectName());
				}
				else
				{
					throw new RuntimeException("DdlViewer: 'Search for' expected TreeTableModel of 'DdlViewerModel2'.");
				}
			}
		});
		
		return panel;
	}

	private JPanel createDetailesPanel()
	{
		JPanel panel = SwingUtils.createPanel("Details", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow,fill]", ""));

		_dependParent_lbl.setToolTipText("This would be parent, if we did depend on anything.");
		_dependParent_txt.setToolTipText("This would be parent, if we did depend on anything.");
		_dependsList_lbl .setToolTipText("This depends on the following objects.");
		_dependsList_txt .setToolTipText("This depends on the following objects.");
		_source_lbl      .setToolTipText("Source in "+Version.getAppName()+" where the DDL Information was requested.");
		_source_txt      .setToolTipText("Source in "+Version.getAppName()+" where the DDL Information was requested.");

		_object_panel    = createObjectPanel();
		_optDiag_panel   = createOptDiagPanel();
		_depends_panel   = createDependsPanel();
		_extraInfo_panel = createExtraInfoPanel();

		_tabbedPane      = new GTabbedPane("DdlVeiewer_MainTabbedPane");
		_tabbedPane.add("Object Information", _object_panel);
		_tabbedPane.add("optdiag",            _optDiag_panel);
		_tabbedPane.add("sp_depends",         _depends_panel);
		_tabbedPane.add("Extra Information",  _extraInfo_panel);

		
		panel.add(_dependParent_lbl, "gap 5 5 5");
		panel.add(_dependParent_txt, "gap 5 5 5, pushx, wrap");

		panel.add(_dependsList_lbl, "gap 5 5");
		panel.add(_dependsList_txt, "gap 5 5, pushx, wrap");

		panel.add(_source_lbl,      "gap 5 5, ");
		panel.add(_source_txt,      "gap 5 5, pushx, wrap");

		panel.add(_tabbedPane,      "span, push, grow, wrap");

		return panel;
	}

	private JPanel createObjectPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_object_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_object_scroll, this);

		panel.add(_object_scroll, "push, grow, wrap");

		return panel;
	}

	private JPanel createDependsPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_depends_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_depends_scroll, this);

		panel.add(_depends_scroll, "push, grow, wrap");

		return panel;
	}

	private JPanel createOptDiagPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_optDiag_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_optDiag_scroll, this);

		panel.add(_optDiag_scroll, "push, grow, wrap");

		return panel;
	}

	private JPanel createExtraInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("TreeView", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_extraInfo_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_extraInfo_scroll, this);
		
		// Add "Right click menu options"
		JPopupMenu menu = _extraInfo_txt.getPopupMenu();

		JMenuItem mi = new JMenuItem("Open in 'ASE Showplan Viewer'...");
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String planText = _extraInfo_txt.getText();
				AsePlanViewer.getInstance().loadXmlDeferred(planText);
			}
		});

		menu.addSeparator();
		menu.add(mi);



		panel.add(_extraInfo_scroll, "push, grow, wrap");

		return panel;
	}

	private JXTreeTable createTreeTable() 
	{
		// Extend the JXTable to get tooltip stuff
		_treeTable = new JXTreeTable()
		{
	        private static final long serialVersionUID = 0L;

			@Override
			public String getToolTipText(MouseEvent e) 
			{
				String tip = null;
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				if (row >= 0)
				{
					row = super.convertRowIndexToModel(row);
				}
				return tip;
			}
//			/** set some color to CM NAME SUM COLUMNS */
//			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
//			{
//				Component c = super.prepareRenderer(renderer, row, column);
//				if (column >= 5) // FIXME: 5 should NOT be hard coded...
//					c.setBackground(TAB_CMNAMESUM_COL_BG);
//				return c;
//			}
		};


		WaitForExecDialog wait = new WaitForExecDialog(this, "Reading DDL information from Offline storage.");

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				_logger.info("Starting a thread that that will read the offline database entries.");
				getWaitDialog().setState("Reading offline entries.");

				List<DdlDetails> ddlObjects = PersistReader.getInstance().getDdlObjects(false);
//				DdlViewerModel model = new DdlViewerModel(ddlObjects);

				_logger.info("Ending the thread that that read the offline database entries.");
				
				return ddlObjects;
//				return model;
			}
		};
		Object retObj = wait.execAndWait(bgExec);

		if (retObj != null && retObj instanceof DdlViewerModel)
		{
			DdlViewerModel model = (DdlViewerModel)retObj;
			_treeTable.setTreeTableModel(model);
		}

		if (retObj != null && retObj instanceof List)
		{
			List<DdlDetails> ddlObjects = (List<DdlDetails>)retObj;
			DdlViewerModel2 model = new DdlViewerModel2(ddlObjects);
			_treeTable.setTreeTableModel(model);
		}

//		if (retObj != null && retObj instanceof List)
//		{
//			List<DdlDetails> ddlObjects = (List<DdlDetails>)retObj;
//			DefaultTreeTableModel model = createModel(ddlObjects);
//			_treeTable.setTreeTableModel(model);
//		}
//System.out.println("RowCount(): "+_treeTable.getRowCount());
//for (int r=0; r<_treeTable.getRowCount(); r++)
//{
//	System.out.println("------------ROW-"+r+"---------------------");
//	
//	TreePath tp = _treeTable.getPathForRow(r);
//	System.out.println("TreePath("+r+"): "+tp);
//
//	System.out.println("TreePath("+r+"):tp.getPathCount()        : "+tp.getPathCount());
//	System.out.println("TreePath("+r+"):tp.getLastPathComponent(): "+tp.getLastPathComponent());
////	tp.getPathComponent(element)
//
//	Object[] oa  = tp.getPath();
//
//	for (Object o : oa)
//	{
//		System.out.println("Object[] of tp.getPath()("+r+"): "+o);
//	}
//}


//		List<DdlDetails> ddlObjects = PersistReader.getInstance().getDdlObjects(false);
//		DdlViewerModel model = new DdlViewerModel(ddlObjects);

//		_treeTable.setTreeTableModel(model);
		_treeTable.setTreeCellRenderer(new IconRenderer());

//		_treeTable.setHighlighters(_highliters);

//		_treeTable.setModel(GuiLogAppender.getTableModel());
		_treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_treeTable.packAll(); // set size so that all content in all cells are visible
		_treeTable.setSortable(true);
		_treeTable.setColumnControlVisible(true);

//		_treeTable.addKeyListener(new TreeTableNavigationEnhancer(_treeTable, this));
		new TreeTableNavigationEnhancer(_treeTable, this);
//		_treeTable.getColumnModel().getColumn(0).setWidth(250);

		_tablePopupMenu = createDataTablePopupMenu();
		_treeTable.setComponentPopupMenu(_tablePopupMenu);

		
//		JScrollPane scroll = new JScrollPane(_treeTable);
//		_watermark = new Watermark(scroll, "");

//		panel.add(scroll, BorderLayout.CENTER);
//		panel.add(scroll, "width 100%, height 100%");
//		return panel;
		
		return _treeTable;
	}

	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				_treeTable.requestFocus();

				_treeTable.getColumnModel().getColumn(0).setWidth(250);
//				_treeTable.getColumnModel().getColumn(0).setMinWidth(250);
				_treeTable.getColumnModel().getColumn(0).setPreferredWidth(250);
//				_treeTable.packAll(); // set size so that all content in all cells are visible
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/** 
	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
	 * If you want to add stuff to the menu, its better to use 
	 * getTabPopupMenu(), then add entries to the menu. This is much 
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createDataTablePopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup        = new JPopupMenu();
		JMenuItem searchFor     = new JMenuItem("Search for...");
		JMenuItem show          = new JMenuItem("Show");
		JMenuItem showParent    = new JMenuItem("Show Parent Object");
		JMenuItem dependsList   = new JMenuItem("Get Dependent List...");
		JMenuItem captureSource = new JMenuItem("Get what component that was responsible for this capture");

		popup.add(searchFor);
		popup.add(show);
		popup.add(showParent);
//		popup.add(cfgView);
//		popup.add(pcsDbInfo);

		searchFor.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
//				DdlViewerModel2 model = new DdlViewerModel2(ddlObjects);
				TreeTableModel ttm = _treeTable.getTreeTableModel();
				if (ttm != null && ttm instanceof DdlViewerModel2)
				{
					DdlViewerModel2 model = (DdlViewerModel2) ttm;

					DdlDetails ddlDetails = DdlViewerSearchForDialog.open(DdlViewer.this, model.getDdlDetails(), "");
					if (ddlDetails != null)
					{
						setViewEntry(ddlDetails.getDbname(), ddlDetails.getObjectName());
					}
				}
				else
				{
					throw new RuntimeException("DdlViewer: 'Search for' expected TreeTableModel of 'DdlViewerModel2'.");
				}
			}
		});

		show.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				doActionShow();
			}
		});

		showParent.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				int row = _treeTable.getSelectedRow();
				TreePath tp = _treeTable.getPathForRow(row);//.getLastPathComponent();
				Object o = tp.getLastPathComponent();
				if (o instanceof ObjectEntry)
				{
					ObjectEntry oe = (ObjectEntry) o;
					setViewEntry(oe._dbname, oe._dependParent);
				}
			}
		});

		if (popup.getComponentCount() == 0)
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
			return null;
		}
		else
			return popup;
	}

	@Override
	public void doActionShow()
	{
		int row = _treeTable.getSelectedRow();
//		Object o = _treeTable.getValueAt(row, 0);
//		_logger.info("SHOW_BUT was pressed, currentRow="+row+", value='"+o+"', objType="+(o==null?"null":o.getClass().getName()));

		TreePath tp = _treeTable.getPathForRow(row);//.getLastPathComponent();
		Object o = tp.getLastPathComponent();
//System.out.println("doActionShow(), row="+row+", o="+(o==null?"null":o.getClass().getName()));
		
		

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		if (o instanceof ObjectEntry)
		{
			ObjectEntry oe = (ObjectEntry) o;
			loadObjectDetailes(reader, oe);
		}
	}


	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// Various code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------

	/**
	 * Position to current object in the view
	 * 
	 * @param dbname The name of the database where the object exists (null or empty, search all databases)
	 * @param objectName Name of the object
	 */
//	public boolean setViewEntry(String dbname, String objectName)
//	{
////		_treeTable.setSel
//		// TODO: implement this
//
////		_treeTable.getPathForRow(1);
//		_treeTable.getTreeTableModel();
//		return false;
//	}

	public boolean setViewEntry(String dbname, String objectName)
	{
//System.out.println("BEGIN:setViewEntry(dbname='"+dbname+"', objectname='"+objectName+"')");
		
		if (objectName != null && objectName.startsWith("*"))
		{
			dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;
		}

		TreeTableModel tm = _treeTable.getTreeTableModel();
		if (tm instanceof DdlViewerModel2)
		{
			DdlViewerModel2 ddlm = (DdlViewerModel2)tm;
			AbstractMutableTreeTableNode tn = (AbstractMutableTreeTableNode)ddlm.getRoot();

			tn = findEntry(tn, dbname, objectName, 0);
			if (tn == null)
			{
				String htmlMsg = "<html>The object '"+objectName+"' wasn't found in the DDL View storage.</html>";
				SwingUtils.showInfoMessageExt(this, "Object not found", htmlMsg, null, (JPanel)null);

				return false;
			}
			else
			{
//				System.out.println("findEntry(): TN ="+(tn ==null?"-null-":tn.getClass().getName()));
//				System.out.println("findEntry(): VAL="+(tn ==null?"-null-":tn.toString()));

				TreeTableNode[] pathArr = ddlm.getPathToRoot(tn);
//				System.out.println("PATH="+pathArr);

				TreePath currentPath = new TreePath(pathArr);
				
//				_treeTable.setSelectionPath(currentPath);
				_treeTable.expandPath(currentPath);
				_treeTable.scrollPathToVisible(currentPath);

				int row = _treeTable.getRowForPath(currentPath);
				_treeTable.setRowSelectionInterval(row, row);

				// Load detailes for current entry
				doActionShow();

//				TreePath currentPath = new TreePath(tn.getPath());
//				_tree.setSelectionPath(currentPath);
//				_tree.expandPath(currentPath);
//				_tree.scrollPathToVisible(currentPath);

				return true;
			}
		}
//		System.out.println("END:setViewEntry(dbname='"+dbname+"', objectname='"+objectName+"')");
		return false;
	}

	private AbstractMutableTreeTableNode findEntry(AbstractMutableTreeTableNode tn, String dbname, String objectName, int level)
	{
		for (Enumeration e = tn.children(); e.hasMoreElements();)
		{
			tn = (AbstractMutableTreeTableNode) e.nextElement();
//			Object obj = tn.getUserObject();
			Object obj = tn;
//System.out.println(level+": OBJ="+StringUtil.left((obj==null?"-null-":obj.getClass().getName()),50) + ", VAL="+obj);

			AbstractMutableTreeTableNode fnr = null;

			if (obj instanceof DbEntry)
			{
				DbEntry dbe = (DbEntry) obj;
				if (dbe._dbname.equalsIgnoreCase(dbname) || dbname == null)
					fnr = findEntry(dbe, dbname, objectName, level+1);
				if (fnr != null)
					return fnr;
			}
			if (obj instanceof TypeEntry)
			{
				TypeEntry te = (TypeEntry) obj;
				fnr = findEntry(te, dbname, objectName, level+1);
				if (fnr != null)
					return fnr;
			}
			if (obj instanceof ObjectEntry)
			{
				ObjectEntry oe = (ObjectEntry) obj;
				if (oe._name.equalsIgnoreCase(objectName))
					return oe;
			}
		}
		return null;
	}
	

	private void loadObjectDetailes(PersistReader reader, ObjectEntry oe)
	{
//		System.out.println("loadObjectDetailes(): oe="+oe);
		if ( !oe._objectTextIsLoaded || !oe._optdiagTextIsLoaded || !oe._dependsTextIsLoaded || !oe._extraInfoTextIsLoaded )
		{
			DdlDetails ddld = reader.getDdlDetailes(oe._dbname, oe._type, oe._name, oe._owner);
			if (ddld != null)
			{
				oe._objectText            = ddld.getObjectText();
				oe._objectTextIsLoaded    = true;

				oe._optdiagText           = ddld.getOptdiagText();
				oe._optdiagTextIsLoaded   = true;

				oe._dependsText           = ddld.getDependsText();
				oe._dependsTextIsLoaded   = true; 

				oe._extraInfoText         = ddld.getExtraInfoText();
				oe._extraInfoTextIsLoaded = true;
			}
		}
		_object_txt   .setText(oe._objectText);
		_optDiag_txt  .setText(oe._optdiagText);
		_depends_txt  .setText(oe._dependsText);
		_extraInfo_txt.setText(oe._extraInfoText);

		_object_txt   .setCaretPosition(0);
		_optDiag_txt  .setCaretPosition(0);
		_depends_txt  .setCaretPosition(0);
		_extraInfo_txt.setCaretPosition(0);

		_dependParent_txt.setText( oe._dependParent );
		_dependsList_txt .setText( oe._dependList );
		_source_txt      .setText( oe._source );
	}


	/**
	 * IMPLEMENTS: ActionListener
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
//		Object source = e.getSource();

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

		//------------------
		// WINDOW
		//------------------
		if (_splitPane != null)
			conf.setLayoutProperty("ddlview.dialog.splitPane.dividerLocation",  _splitPane.getDividerLocation());


		conf.setLayoutProperty("ddlview.dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("ddlview.dialog.window.height", this.getSize().height);
		conf.setLayoutProperty("ddlview.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("ddlview.dialog.window.pos.y",  this.getLocationOnScreen().y);

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
		int width  = conf.getLayoutProperty("ddlview.dialog.window.width",  SwingUtils.hiDpiScale(900));
		int height = conf.getLayoutProperty("ddlview.dialog.window.height", SwingUtils.hiDpiScale(740));
		int x      = conf.getLayoutProperty("ddlview.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("ddlview.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		
		int divLoc = conf.getLayoutProperty("ddlview.dialog.splitPane.dividerLocation",  -1);
		if (divLoc < 0)
			divLoc = width / 3;
		_splitPane.setDividerLocation(divLoc);
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	

	public static class IconRenderer extends DefaultTreeCellRenderer 
	{
		private static final long	serialVersionUID	= 1L;
		ImageIcon _DB_icon;
		ImageIcon _U_icon;
		ImageIcon _S_icon;
		ImageIcon _P_icon;
		ImageIcon _V_icon;
		ImageIcon _D_icon;
		ImageIcon _R_icon;
		ImageIcon _TR_icon;
		ImageIcon _SS_icon;

		public IconRenderer() 
		{
			_DB_icon = SwingUtils.readImageIcon(Version.class, "images/highlighter_db.png");
			_U_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_user_table.png");
			_S_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_system_table.png");
			_P_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_procedure.png");
			_V_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_view.png");
			_D_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_default_value.png");
			_R_icon  = SwingUtils.readImageIcon(Version.class, "images/highlighter_rule.png");
			_TR_icon = SwingUtils.readImageIcon(Version.class, "images/highlighter_trigger.png");
			
			_SS_icon = SwingUtils.readImageIcon(Version.class, "images/highlighter_statement_cache.png");
		}

		@Override 
		public Component getTreeCellRendererComponent(JTree tree, Object value,	boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) 
		{
//			return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			Component scomp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (value instanceof DbEntry)
			{
				if (PersistentCounterHandler.STATEMENT_CACHE_NAME.equals( ((DbEntry)value)._dbname ) )
				{
					setIcon(getIcon("SS"));
					setText( ((DbEntry)value).getDisplayString() );
				}
				else
				{
					setIcon(getIcon("DB"));
					setText( ((DbEntry)value).getDisplayString() );
				}
			}
			else if (value instanceof TypeEntry)
			{
				setIcon(getIcon(((TypeEntry)value)._type));
				setText( ((TypeEntry)value).getDisplayString() );
			}
			else if (value instanceof ObjectEntry)
			{
				setIcon(getIcon(((ObjectEntry)value)._type));
				setText( ((ObjectEntry)value).getDisplayString() );
			}
			else 
			{
//				setIcon(sampleIcon);
			}
			return scomp;
		}
		
		public ImageIcon getIcon(String type)
		{
			if      ("DB".equals(type)) return _DB_icon;
			else if ("U" .equals(type)) return _U_icon;
			else if ("S" .equals(type)) return _S_icon;
			else if ("P" .equals(type)) return _P_icon;
			else if ("V" .equals(type)) return _V_icon;
			else if ("D" .equals(type)) return _D_icon;
			else if ("R" .equals(type)) return _R_icon;
			else if ("TR".equals(type)) return _TR_icon;
			else if ("SS".equals(type)) return _SS_icon;
			else return null;
		}
	}

	public static String getTypeName(String type)
	{
		if      ("DB".equals(type)) return "Databases";
		else if ("U" .equals(type)) return "User Tables";
		else if ("S" .equals(type)) return "System Tables";
		else if ("P" .equals(type)) return "Procedures";
		else if ("V" .equals(type)) return "Views";
		else if ("D" .equals(type)) return "Defaults";
		else if ("R" .equals(type)) return "Rules";
		else if ("TR".equals(type)) return "Triggers";
		else if ("SS".equals(type)) return "Statement Cache";
		else return type;
	}

// 
//	One of the following object types:
//		� C � computed column
//		� D � default
//		� DD � decrypt default
//		� F � SQLJ function
//		� L � log
//		� N � partition condition
//		� P � Transact-SQL or SQLJ procedure
//		� PR � prepare objects (created by Dynamic SQL)
//		� R � rule
//		� RI � referential constraint
//		� S � system table
//		� TR � trigger
//		� U � user table
//		� V � view
//		� XP � extended stored procedure.
	
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	private static Connection jdbcConnect(String appname, String driver, String url, String user, String passwd)
	{
		try
		{
			Class.forName(driver).newInstance();
			Properties props = new Properties();
			props.put("user", user);
			props.put("password", passwd);
	
			_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
			Connection conn = DriverManager.getConnection(url, props);
	
			return conn;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			JOptionPane.showMessageDialog(null, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	
	
	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		Configuration conf1 = new Configuration(AppDir.getAppStoreDir() + "/asetune.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);
		
		Configuration.setSearchOrder(Configuration.USER_TEMP);

		System.setProperty("DBXTUNE_SAVE_DIR", "c:/projects/dbxtune/data");
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
		}
		

		String driver = "org.h2.Driver";
//		String url    = "jdbc:h2:file:C:\\projects\\_customer_visits\\1and1_germany\\2011-11-15\\spam_prod_b_2011-11-15;IFEXISTS=TRUE";
//		String url    = "jdbc:h2:file:C:\\projects\\dbxtune\\data\\GORAN_1_DS_2011-12-01;IFEXISTS=TRUE";
//		String url    = "jdbc:h2:file:E:\\spam_prod_b_2011-11-30;IFEXISTS=TRUE;AUTO_SERVER=TRUE";
		String url    = "jdbc:h2:file:C:\\projects\\_customer_visits\\1and1_germany\\2011-11-30\\spam_prod_b_2011-12-02;IFEXISTS=TRUE;AUTO_SERVER=TRUE";

//		Connection conn = jdbcConnect("xxx", driver, url, "sa", "");
		DbxConnection conn = DbxConnection.createDbxConnection( jdbcConnect("xxx", driver, url, "sa", "") );
		PersistReader reader = new PersistReader(conn);
		PersistReader.setInstance(reader);
		
		DdlViewer xxx = new DdlViewer();
		xxx.setVisible(true);
	}
}
