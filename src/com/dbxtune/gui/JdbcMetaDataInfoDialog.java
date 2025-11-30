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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.sort.RowFilters;

import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.sql.SqlPickList;
import com.dbxtune.sql.conn.DbxDatabaseMetaDataSqlServer;
import com.dbxtune.tools.sqlw.ResultSetJXTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class JdbcMetaDataInfoDialog
extends JDialog
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private Window                 _owner         = null;
	                               
	private Connection             _conn          = null;

	private GTabbedPane            _tabPane         = new GTabbedPane();
                                   
	private JLabel                 _productName_lbl = new JLabel("Database Product Name");
	private JTextField             _productName_txt = new JTextField();
                                   
	private JLabel                 _productVer_lbl = new JLabel("Database Product Version");
	private JTextField             _productVer_txt = new JTextField();
                                   
	private JLabel                 _driverName_lbl = new JLabel("Driver Name");
	private JTextField             _driverName_txt = new JTextField();
	                               
	private JLabel                 _driverVer_lbl = new JLabel("Driver Version");
	private JTextField             _driverVer_txt = new JTextField();
                                   
	private JLabel                 _sql92_lbl     = new JLabel("ANSI SQL 92 Compatibility");
	private JCheckBox              _sql92_1_chk   = new JCheckBox("Entry Level");
	private JCheckBox              _sql92_2_chk   = new JCheckBox("Intermediate Grammar");
	private JCheckBox              _sql92_3_chk   = new JCheckBox("Full Grammar");
                                   
	private JLabel                 _kf_filter_lbl = new JLabel("Filter: ");
	private JTextField             _kf_filter_txt = new JTextField();
	private JLabel                 _kf_filter_cnt = new JLabel();
                                   
	private JLabel                 _md_filter_lbl = new JLabel("Filter: ");
	private JTextField             _md_filter_txt = new JTextField();
	private JLabel                 _md_filter_cnt = new JLabel();
                                   
	private JLabel                 _dt_filter_lbl = new JLabel("Filter: ");
	private JTextField             _dt_filter_txt = new JTextField();
	private JLabel                 _dt_filter_cnt = new JLabel();
                                   
	private JLabel                 _tt_filter_lbl = new JLabel("Filter: ");
	private JTextField             _tt_filter_txt = new JTextField();
	private JLabel                 _tt_filter_cnt = new JLabel();
                                   
	private JLabel                 _dp_filter_lbl = new JLabel("Filter: ");
	private JTextField             _dp_filter_txt = new JTextField();
	private JLabel                 _dp_filter_cnt = new JLabel();
                                   
	private JLabel                 _ca_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ca_filter_txt = new JTextField();
	private JLabel                 _ca_filter_cnt = new JLabel();

	private JLabel                 _sc_filter_lbl = new JLabel("Filter: ");
	private JTextField             _sc_filter_txt = new JTextField();
	private JLabel                 _sc_filter_cnt = new JLabel();

	private JLabel                 _ta_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ta_filter_txt = new JTextField();
	private JLabel                 _ta_filter_cnt = new JLabel();

	private JLabel                 _co_filter_lbl = new JLabel("Filter: ");
	private JTextField             _co_filter_txt = new JTextField();
	private JLabel                 _co_filter_cnt = new JLabel();

	private JLabel                 _pk_filter_lbl = new JLabel("Filter: ");
	private JTextField             _pk_filter_txt = new JTextField();
	private JLabel                 _pk_filter_cnt = new JLabel();

	private JLabel                 _br_filter_lbl = new JLabel("Filter: ");
	private JTextField             _br_filter_txt = new JTextField();
	private JLabel                 _br_filter_cnt = new JLabel();

	private JLabel                 _ix_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ix_filter_txt = new JTextField();
	private JLabel                 _ix_filter_cnt = new JLabel();

	private JLabel                 _ik_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ik_filter_txt = new JTextField();
	private JLabel                 _ik_filter_cnt = new JLabel();

	private JLabel                 _ek_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ek_filter_txt = new JTextField();
	private JLabel                 _ek_filter_cnt = new JLabel();

	private JLabel                 _ta_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _ta_cat_txt = new JTextField("null");
	private JLabel                 _ta_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _ta_sch_txt = new JTextField("null");
	private JLabel                 _ta_val_lbl = new JLabel("Table Name Pattern: ");
	private JTextField             _ta_val_txt = new JTextField("%");
	private JLabel                 _ta_typ_lbl = new JLabel("Table Types: ");
	private JTextField             _ta_typ_txt = new JTextField("null");
	private JLabel                 _ta_api_lbl = new JLabel("Press button to call getTables(catalog, schemaPattern, tableNamePattern, types)");

	private JLabel                 _co_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _co_cat_txt = new JTextField("null");
	private JLabel                 _co_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _co_sch_txt = new JTextField("null");
	private JLabel                 _co_val_lbl = new JLabel("Table Name Pattern: ");
	private JTextField             _co_val_txt = new JTextField("%");
	private JLabel                 _co_col_lbl = new JLabel("Columns Name: ");
	private JTextField             _co_col_txt = new JTextField("%");
	private JLabel                 _co_api_lbl = new JLabel("Press button to call getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern)");

	private JLabel                 _pk_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _pk_cat_txt = new JTextField("null");
	private JLabel                 _pk_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _pk_sch_txt = new JTextField("null");
	private JLabel                 _pk_val_lbl = new JLabel("Table Name: ");
	private JTextField             _pk_val_txt = new JTextField("");
	private JLabel                 _pk_api_lbl = new JLabel("Press button to call getTables(catalog, schemaPattern, tableName)");

	private JLabel                 _br_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _br_cat_txt = new JTextField("null");
	private JLabel                 _br_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _br_sch_txt = new JTextField("null");
	private JLabel                 _br_val_lbl = new JLabel("Table Name: ");
	private JTextField             _br_val_txt = new JTextField("");
	private JLabel                 _br_scope_lbl    = new JLabel("Scope: ");
	private JComboBox<BestRow>     _br_scope_cbx    = new JComboBox<BestRow>(BestRow.values());
	private JCheckBox              _br_nullable_chk = new JCheckBox("include columns that are nullable.", true);
	private JLabel                 _br_api_lbl = new JLabel("Press button to call getBestRowIdentifier(catalog, schema, tableName, scope, nullable)");

	private JLabel                 _ix_cat_lbl       = new JLabel("Catalog: ");
	private JTextField             _ix_cat_txt       = new JTextField("null");
	private JLabel                 _ix_sch_lbl       = new JLabel("Schema Pattern: ");
	private JTextField             _ix_sch_txt       = new JTextField("null");
	private JLabel                 _ix_val_lbl       = new JLabel("Table Name: ");
	private JTextField             _ix_val_txt       = new JTextField("");
	private JCheckBox              _ix_unique_chk    = new JCheckBox("When true, return only indices for unique values; when false, return indices regardless of whether unique or not");
	private JCheckBox              _ix_approx_chk    = new JCheckBox("When true, result is allowed to reflect approximate or out of data values; when false, results are requested to be accurate", true);
	private JCheckBox              _ix_useOrigin_chk = new JCheckBox("Use Origin JDBC Driver impementation instead of the 'modified' for DbxConnection.", false);
	private JLabel                 _ix_api_lbl       = new JLabel("Press button to call getIndexInfo(catalog, schemaPattern, tableNamePattern, unique, approximate)");

	private JLabel                 _ik_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _ik_cat_txt = new JTextField("null");
	private JLabel                 _ik_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _ik_sch_txt = new JTextField("null");
	private JLabel                 _ik_val_lbl = new JLabel("Table Name: ");
	private JTextField             _ik_val_txt = new JTextField("");
	private JLabel                 _ik_api_lbl = new JLabel("Press button to call getImportedKeys(catalog, schemaPattern, tableName)");

	private JLabel                 _ek_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _ek_cat_txt = new JTextField("null");
	private JLabel                 _ek_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _ek_sch_txt = new JTextField("null");
	private JLabel                 _ek_val_lbl = new JLabel("Table Name: ");
	private JTextField             _ek_val_txt = new JTextField("");
	private JLabel                 _ek_api_lbl = new JLabel("Press button to call getExportedKeys(catalog, schemaPattern, tableName)");

	private JLabel                 _pr_filter_lbl = new JLabel("Filter: ");
	private JTextField             _pr_filter_txt = new JTextField();
	private JLabel                 _pr_filter_cnt = new JLabel();

	private JLabel                 _pr_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _pr_cat_txt = new JTextField("null");
	private JLabel                 _pr_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _pr_sch_txt = new JTextField("null");
	private JLabel                 _pr_val_lbl = new JLabel("Procedure Name Pattern: ");
	private JTextField             _pr_val_txt = new JTextField("%");
	private JLabel                 _pr_api_lbl = new JLabel("Press button to call getProcedures(catalog, schemaPattern, procedureNamePattern)");

	private JLabel                 _fu_filter_lbl = new JLabel("Filter: ");
	private JTextField             _fu_filter_txt = new JTextField();
	private JLabel                 _fu_filter_cnt = new JLabel();

	private JLabel                 _fu_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _fu_cat_txt = new JTextField("null");
	private JLabel                 _fu_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _fu_sch_txt = new JTextField("null");
	private JLabel                 _fu_val_lbl = new JLabel("Function Name Pattern: ");
	private JTextField             _fu_val_txt = new JTextField("%");
	private JLabel                 _fu_api_lbl = new JLabel("Press button to call getFunctions(catalog, schemaPattern, functionNamePattern)");

	private JLabel                 _fc_filter_lbl = new JLabel("Filter: ");
	private JTextField             _fc_filter_txt = new JTextField();
	private JLabel                 _fc_filter_cnt = new JLabel();

	private JLabel                 _fc_cat_lbl = new JLabel("Catalog: ");
	private JTextField             _fc_cat_txt = new JTextField("null");
	private JLabel                 _fc_sch_lbl = new JLabel("Schema Pattern: ");
	private JTextField             _fc_sch_txt = new JTextField("null");
	private JLabel                 _fc_val_lbl = new JLabel("Function Name Pattern: ");
	private JTextField             _fc_val_txt = new JTextField("%");
	private JLabel                 _fc_col_lbl = new JLabel("Columns Name: ");
	private JTextField             _fc_col_txt = new JTextField("%");
	private JLabel                 _fc_api_lbl = new JLabel("Press button to call getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern)");

	private JLabel                 _ci_filter_lbl = new JLabel("Filter: ");
	private JTextField             _ci_filter_txt = new JTextField();
	private JLabel                 _ci_filter_cnt = new JLabel();

	private KeywordsFunctionsTable _kf_tab  = null;
	private MetaDataTable          _md_tab  = null;
	private DataTypeTable          _dt_tab  = null;
	private TableTypesTable        _tt_tab  = null;
	private DriverPropsTable       _dp_tab  = null;
	private CatalogsTable          _ca_tab  = null;
	private SchemasTable           _sc_tab  = null;
	private TablesTable            _ta_tab  = null;
	private ColumnsTable           _co_tab  = null;
	private TablesPkTable          _pk_tab  = null;
	private BestRowTable           _br_tab  = null;
	private IndexTable             _ix_tab  = null;
	private ImportedKeysTable      _ik_tab  = null;
	private ExportedKeysTable      _ek_tab  = null;
	private ProceduresTable        _pr_tab  = null;
	private FunctionsTable         _fu_tab  = null;
	private FunctionColumnsTable   _fc_tab  = null;
	private ConnInfoTable          _ci_tab  = null;

	private JButton                _ok     = new JButton("OK");
	private JButton                _cancel = new JButton("Cancel");

	private boolean                _okWasPressed = false;
	
	
	// Enums for various things
	public enum BestRow
	{
		NotPseudo  (DatabaseMetaData.bestRowNotPseudo,   "Best row identifier is NOT a pseudo column."), 
		Pseudo     (DatabaseMetaData.bestRowPseudo,      "Best row identifier is a pseudo column."), 
		Session    (DatabaseMetaData.bestRowSession,     "Scope of the best row identifier is the remainder of the current session."), 
		Temporary  (DatabaseMetaData.bestRowTemporary,   "Scope of the best row identifier is very temporary, lasting only while the row is being used."), 
		Transaction(DatabaseMetaData.bestRowTransaction, "Scope of the best row identifier is the remainder of the current transaction."), 
		Unknown    (DatabaseMetaData.bestRowUnknown,     "Best row identifier may or may not be a pseudo column.");
		
		private int jdbcNumber;
		private String desc;
		
		private BestRow(int jdbcNumber, String desc)
		{
			this.jdbcNumber = jdbcNumber;
			this.desc       = desc;
		}

		public int    getJdbcNumber()  { return jdbcNumber; }
		public String getDescription() { return desc; }

		@Override
		public String toString()
		{
			return "<html>" + name() + " - <font color=\"green\">" + getDescription() + "</font></html>";
		}
	};
	
	
	/** 
	 * Renderer that accepts both a boolean and a "other" values.
	 * If it's a Boolean, it will be displayed as a checkbox...
	 * Otherwise it will be displayed as a Label...
	 */
	private static class BooleanRenderer 
	implements TableCellRenderer
	{

		private TableCellRenderer _forBoolean;
		private TableCellRenderer _forLabel;

		public BooleanRenderer()
		{
			_forBoolean = new DefaultTableRenderer(new CheckBoxProvider(StringValues.EMPTY, SwingConstants.LEFT));
			_forLabel   = new DefaultTableRenderer(new LabelProvider());
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (value instanceof Boolean)
				return _forBoolean.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			else
				return _forLabel.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	public JdbcMetaDataInfoDialog(Window owner, Connection conn)
	{
//		super(owner, "JDBC Connection Information", ModalityType.APPLICATION_MODAL);
		super(owner, "JDBC Connection Information", ModalityType.MODELESS);

		_owner = owner;
		_conn  = conn;
		
		pinit();
		pack();

		applyKfFilter();
		applyMdFilter();
		applyDtFilter();
		applyTtFilter();
		applyDpFilter();
		applyCaFilter();
		applyScFilter();
		applyCiFilter();
//		applyPkFilter();
//		applyBrFilter();
//		applyIxFilter();

		SwingUtils.installEscapeButton(this, _cancel);

		// Restore windows size
		loadProps();

		setLocationRelativeTo(owner);
	}

	private void pinit()
	{
		setLayout(new MigLayout());

		_tabPane.addTab("Keyword & Functions",  createJdbcKeywordFunctionsPanel());
		_tabPane.addTab("Capabilities",         createJdbcCapabilitiesPanel());
//		_tabPane.addTab("Keywords & Functions", component);
		_tabPane.addTab("Data Types",           createJdbcDataTypesPanel());
		_tabPane.addTab("Table Types",          createTableTypePanel());
		_tabPane.addTab("Driver Propertis",     createDriverPropsPanel());
		_tabPane.addTab("Catalogs",             createCatalogPanel());
		_tabPane.addTab("Schemas",              createSchemasPanel());
		_tabPane.addTab("Tables",               createTablesPanel());
		_tabPane.addTab("Columns",              createColumnsPanel());
		_tabPane.addTab("PrimayKeys",           createTablesPkPanel());
		_tabPane.addTab("BestRow",              createBestRowPanel());
		_tabPane.addTab("IndexInfo",            createIndexPanel());
		_tabPane.addTab("FK ->",                createImportedKeysPanel());
		_tabPane.addTab("FK <-",                createExportedKeysPanel());
		_tabPane.addTab("Procedures",           createProceduresPanel());
		_tabPane.addTab("Functions",            createFunctionsPanel());
		_tabPane.addTab("FuncColumns",          createFunctionColumnsPanel());
		_tabPane.addTab("getClientInfo()",      createClientInfoPanel());

		add(createTopPanel(),        "growx, pushx, wrap");
		add(_tabPane,                "grow, push, wrap");
		add(createOkCancelPanel(),   "growx, pushx, tag right, wrap");

		
		_kf_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyKfFilter(); } });
		_md_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyMdFilter(); } });
		_dt_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyDtFilter(); } });
		_tt_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyTtFilter(); } });
		_dp_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyDpFilter(); } });
		_ca_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyCaFilter(); } });
		_sc_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyScFilter(); } });
		_ta_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyTaFilter(); } });
		_co_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyCoFilter(); } });
		_pk_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyPkFilter(); } });
		_br_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyBrFilter(); } });
		_ix_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyIxFilter(); } });
		_ik_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyIkFilter(); } });
		_ek_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyEkFilter(); } });
		_pr_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyPrFilter(); } });
		_fu_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyFuFilter(); } });
		_fc_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyFcFilter(); } });
		_ci_filter_txt.addCaretListener(new CaretListener() { @Override public void caretUpdate(CaretEvent e) { applyCiFilter(); } });

		_ok.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
				_okWasPressed = true;
				setVisible(false);
			}
		});

		_cancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
				_okWasPressed = false;
				setVisible(false);
			}
		});
		
		try { _productName_txt.setText(_conn.getMetaData().getDatabaseProductName());    } catch(Throwable e) { _productName_txt.setText(e.getMessage()); }
		try { _productVer_txt .setText(_conn.getMetaData().getDatabaseProductVersion()); } catch(Throwable e) { _productVer_txt .setText(e.getMessage()); }
		try { _driverName_txt .setText(_conn.getMetaData().getDriverName());             } catch(Throwable e) { _driverName_txt .setText(e.getMessage()); }
		try { _driverVer_txt  .setText(_conn.getMetaData().getDriverVersion());          } catch(Throwable e) { _driverVer_txt  .setText(e.getMessage()); }

		try { _sql92_1_chk.setSelected(_conn.getMetaData().supportsANSI92EntryLevelSQL());  } catch(Throwable e) { _sql92_1_chk.setSelected(false); }
		try { _sql92_2_chk.setSelected(_conn.getMetaData().supportsANSI92IntermediateSQL());} catch(Throwable e) { _sql92_2_chk.setSelected(false); }
		try { _sql92_3_chk.setSelected(_conn.getMetaData().supportsANSI92FullSQL());        } catch(Throwable e) { _sql92_3_chk.setSelected(false); }
	}
	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		conf.setLayoutProperty("JdbcMetaDataInfoDialog.size.width",  this.getSize().width);
		conf.setLayoutProperty("JdbcMetaDataInfoDialog.size.height", this.getSize().height);
		
		conf.save();
	}
	public void loadProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		// Restore windows size
		int width  = conf.getLayoutProperty("JdbcMetaDataInfoDialog.size.width",  SwingUtils.hiDpiScale(1200));
		int height = conf.getLayoutProperty("JdbcMetaDataInfoDialog.size.height", SwingUtils.hiDpiScale(800));
		setSize(width, height);
	}

	private JPanel createTopPanel()
	{
		JPanel p = new JPanel(new MigLayout());

		p.add(_productName_lbl,        "");
		p.add(_productName_txt,        "growx, pushx, wrap");

		p.add(_productVer_lbl,         "");
		p.add(_productVer_txt,         "growx, pushx, wrap");

		p.add(_driverName_lbl,         "");
		p.add(_driverName_txt,         "growx, pushx, wrap");

		p.add(_driverVer_lbl,          "");
		p.add(_driverVer_txt,          "growx, pushx, wrap");

		p.add(_sql92_lbl,              "");
		p.add(_sql92_1_chk,            "split");
		p.add(_sql92_2_chk,            "");
		p.add(_sql92_3_chk,            "wrap");

		return p;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel p = new JPanel(new MigLayout());

		p.add(new JLabel(),    "split, span, growx, pushx"); // dummy label to push everything to the right

		p.add(_ok,     "tag ok");
//		p.add(_cancel, "tag cancel");

		return p;
	}

	private JPanel createClientInfoPanel()
	{
		_ci_tab = new ConnInfoTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getClientInfo()</code></b><br>"
				+ "</html>";
		
		_ci_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_ci_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_ci_filter_lbl,           "gap 5, split");
		p.add(_ci_filter_txt,           "growx, pushx");
		p.add(_ci_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_ci_tab), "grow, push");

		return p;
	}

	private JPanel createJdbcKeywordFunctionsPanel()
	{
		_kf_tab = new KeywordsFunctionsTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData(): getSQLKeywords(), getStringFunctions(), getNumericFunctions(), getTimeDateFunctions(), getSystemFunctions()</code></b><br>"
				+ "</html>";
		
		_kf_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_kf_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_kf_filter_lbl,           "gap 5, split");
		p.add(_kf_filter_txt,           "growx, pushx");
		p.add(_kf_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_kf_tab), "grow, push");

		return p;
	}

	private JPanel createJdbcCapabilitiesPanel()
	{
		_md_tab = new MetaDataTable(_conn);
		_md_tab.getColumn(1).setCellRenderer(new BooleanRenderer());
		
		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().<i>methodName()</i></code></b><br>"
				+ "</html>";
		
		_md_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_md_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_md_filter_lbl,           "gap 5, split");
		p.add(_md_filter_txt,           "growx, pushx");
		p.add(_md_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_md_tab), "grow, push");

		return p;
	}

	private JPanel createJdbcDataTypesPanel()
	{
		_dt_tab = new DataTypeTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getTypeInfo()</code></b><br>"
				+ "</html>";
		
		_dt_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_dt_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_dt_filter_lbl,           "gap 5, split");
		p.add(_dt_filter_txt,           "growx, pushx");
		p.add(_dt_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_dt_tab), "grow, push");

		return p;
	}
	
	private JPanel createTableTypePanel()
	{
		_tt_tab = new TableTypesTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getTableTypes()</code></b><br>"
				+ "</html>";
		
		_tt_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_tt_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_tt_filter_lbl,           "gap 5, split");
		p.add(_tt_filter_txt,           "growx, pushx");
		p.add(_tt_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_tt_tab), "grow, push");

		return p;
	}

	private JPanel createDriverPropsPanel()
	{
		_dp_tab = new DriverPropsTable(_conn);

		String desc = 
				"<html>"
//				+ "<b>Below information is from <code>Connection.getMetaData().getClientInfoProperties()</code></b><br>"
				+ "<b>Below information is from <code>DriverManager.getDriver(url).getPropertyInfo(url, info)</code></b><br>"
				+ "</html>";

		
		_dp_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_dp_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_dp_filter_lbl,           "gap 5, split");
		p.add(_dp_filter_txt,           "growx, pushx");
		p.add(_dp_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_dp_tab), "grow, push");

		return p;
	}

	private JPanel createCatalogPanel()
	{
		_ca_tab = new CatalogsTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getCatalogs()</code></b><br>"
				+ "</html>";
		
		_ca_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_ca_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_ca_filter_lbl,           "gap 5, split");
		p.add(_ca_filter_txt,           "growx, pushx");
		p.add(_ca_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_ca_tab), "grow, push");

		return p;
	}

	private JPanel createSchemasPanel()
	{
		_sc_tab = new SchemasTable(_conn);

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getSchemas()</code></b><br>"
				+ "</html>";
		
		_sc_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_sc_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(_sc_filter_lbl,           "gap 5, split");
		p.add(_sc_filter_txt,           "growx, pushx");
		p.add(_sc_filter_cnt,           "gapright 5, wrap 10");
		
		p.add(new JScrollPane(_sc_tab), "grow, push");

		return p;
	}

	
	private JPanel createTablesPanel()
	{
		_ta_tab = new TablesTable(_conn);

		JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_ta_tab.refresh();
			}
		});
		
		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getTables(catalog, schemaPattern, TablePattern, types)</code></b><br>"
				+ "</html>";
		
		_ta_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_ta_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_ta_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_ta_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_ta_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");
		_ta_typ_txt.setToolTipText("<html>a list of table types, which must be from the list of table types returned from <code>getTableTypes()</code>,to include; <code>null</code> returns all types</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_ta_cat_lbl,              "");
		p1.add(_ta_cat_txt,              "growx, pushx, wrap");
		p1.add(_ta_sch_lbl,              "");
		p1.add(_ta_sch_txt,              "growx, pushx, wrap");
		p1.add(_ta_val_lbl,              "");
		p1.add(_ta_val_txt,              "growx, pushx, wrap");
		p1.add(_ta_typ_lbl,              "");
		p1.add(_ta_typ_txt,              "growx, pushx, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_ta_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_ta_filter_lbl,           "gap 5, split");
		p.add(_ta_filter_txt,           "growx, pushx");
		p.add(_ta_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_ta_tab), "grow, push");

		return p;
	}

	
	private JPanel createColumnsPanel()
	{
		_co_tab = new ColumnsTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_co_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ResultSetTableModel rstm = getTables(_conn, _co_cat_txt.getText(), _co_sch_txt.getText(), _co_val_txt.getText(), null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_co_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_co_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_co_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getColumns(catalog, schemaPattern, TablePattern, ColumnPattern)</code></b><br>"
				+ "</html>";
		
		_co_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_co_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_co_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_co_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_co_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");
		_co_col_txt.setToolTipText("<html>a column name pattern; must match the column name as it is stored in the database</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_co_cat_lbl,              "");
		p1.add(_co_cat_txt,              "growx, pushx, wrap");
		p1.add(_co_sch_lbl,              "");
		p1.add(_co_sch_txt,              "growx, pushx, wrap");
		p1.add(_co_val_lbl,              "");
		p1.add(_co_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");
		p1.add(_co_col_lbl,              "");
		p1.add(_co_col_txt,              "growx, pushx, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_co_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_co_filter_lbl,           "gap 5, split");
		p.add(_co_filter_txt,           "growx, pushx");
		p.add(_co_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_co_tab), "grow, push");

		return p;
	}

	
	private JPanel createTablesPkPanel()
	{
		_pk_tab = new TablesPkTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_pk_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				ResultSetTableModel rstm = getTables(_conn, _pk_cat_txt.getText(), _pk_sch_txt.getText(), _pk_val_txt.getText(), null);
				ResultSetTableModel rstm = getTables(_conn, _pk_cat_txt.getText(), _pk_sch_txt.getText(), "", null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_pk_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_pk_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_pk_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getPrimaryKeys(catalog, schemaPattern, TablePattern)</code></b><br>"
				+ "</html>";
		
		_pk_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_pk_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_pk_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_pk_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_pk_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_pk_cat_lbl,              "");
		p1.add(_pk_cat_txt,              "growx, pushx, wrap");
		p1.add(_pk_sch_lbl,              "");
		p1.add(_pk_sch_txt,              "growx, pushx, wrap");
		p1.add(_pk_val_lbl,              "");
		p1.add(_pk_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_pk_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_pk_filter_lbl,           "gap 5, split");
		p.add(_pk_filter_txt,           "growx, pushx");
		p.add(_pk_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_pk_tab), "grow, push");

		return p;
	}

	
	private JPanel createBestRowPanel()
	{
		_br_tab = new BestRowTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_br_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				ResultSetTableModel rstm = getTables(_conn, _br_cat_txt.getText(), _br_sch_txt.getText(), _br_val_txt.getText(), null);
				ResultSetTableModel rstm = getTables(_conn, _br_cat_txt.getText(), _br_sch_txt.getText(), "", null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_br_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_br_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_br_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getBestRowIdentifier(catalog, schemaPattern, Table, scope, nullable)</code></b><br>"
				+ "</html>";
		
		_br_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_br_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_br_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_br_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_br_val_txt.setToolTipText("<html>a table name; must match the table name as it is stored in the database</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_br_cat_lbl,              "");
		p1.add(_br_cat_txt,              "growx, pushx, wrap");
		p1.add(_br_sch_lbl,              "");
		p1.add(_br_sch_txt,              "growx, pushx, wrap");
		p1.add(_br_val_lbl,              "");
		p1.add(_br_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");
		p1.add(_br_scope_lbl,            "");
		p1.add(_br_scope_cbx,            "growx, pushx, wrap");
		p1.add(_br_nullable_chk,         "skip, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_br_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_br_filter_lbl,           "gap 5, split");
		p.add(_br_filter_txt,           "growx, pushx");
		p.add(_br_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_br_tab), "grow, push");

		return p;
	}

	
	private JPanel createIndexPanel()
	{
		_ix_tab = new IndexTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_ix_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				ResultSetTableModel rstm = getTables(_conn, _ix_cat_txt.getText(), _ix_sch_txt.getText(), _ix_val_txt.getText(), null);
				ResultSetTableModel rstm = getTables(_conn, _ix_cat_txt.getText(), _ix_sch_txt.getText(), "", null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_ix_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_ix_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_ix_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getIndexInfo(catalog, schemaPattern, TablePattern, unique, approximate)</code></b><br>"
				+ "</html>";
		
		_ix_filter_txt   .setToolTipText("Filter that does regular expression on all table cells using this value");
		_ix_filter_cnt   .setToolTipText("Visible rows / actual rows in the GUI Table");

		_ix_cat_txt      .setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_ix_sch_txt      .setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_ix_val_txt      .setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");
		_ix_useOrigin_chk.setToolTipText("<html>For some JDBC Drivers, we have 'overridden' the 'getIndexInfo' method to get some more information.<br>"
				+ "For example in SQL Server, the JDBC Driver does NOT include COLUMNSTORE indexes in the ResultSet<br>"
				+ "</html>");
		
		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_ix_cat_lbl,              "");
		p1.add(_ix_cat_txt,              "growx, pushx, wrap");
		p1.add(_ix_sch_lbl,              "");
		p1.add(_ix_sch_txt,              "growx, pushx, wrap");
		p1.add(_ix_val_lbl,              "");
		p1.add(_ix_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");
		p1.add(_ix_unique_chk,           "skip, wrap");
		p1.add(_ix_approx_chk,           "skip, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_ix_api_lbl,              "wrap 10");
		
		p1.add(_ix_useOrigin_chk,        "skip 1, hidemode 3, wrap 10");

		// HIDE '_ix_useOrigin_chk' on most DBMS (except where we have a DbxConnection implementation of DatabadseMetaData
		String dbmsProductName = "UNKNOWN";
		try 
		{
			dbmsProductName = _conn.getMetaData().getDatabaseProductName();
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems calling dbmd.getDatabaseProductName(). dbmsProductName='" + dbmsProductName + "', errorCode=" + ex.getErrorCode() + ", sqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "'. Caught: " + ex);
		}
		_ix_useOrigin_chk.setVisible(false);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MSSQL))
		{
			_ix_useOrigin_chk.setVisible(true);
		}
		

		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_ix_filter_lbl,           "gap 5, split");
		p.add(_ix_filter_txt,           "growx, pushx");
		p.add(_ix_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_ix_tab), "grow, push");

		return p;
	}

	
	private JPanel createImportedKeysPanel()
	{
		_ik_tab = new ImportedKeysTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_ik_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				ResultSetTableModel rstm = getTables(_conn, _ik_cat_txt.getText(), _ik_sch_txt.getText(), _ik_val_txt.getText(), null);
				ResultSetTableModel rstm = getTables(_conn, _ik_cat_txt.getText(), _ik_sch_txt.getText(), "", null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_ik_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_ik_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_ik_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getImportedKeys(catalog, schemaPattern, TablePattern)</code></b><br>"
				+ "</html>";
		
		_ik_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_ik_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_ik_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_ik_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_ik_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_ik_cat_lbl,              "");
		p1.add(_ik_cat_txt,              "growx, pushx, wrap");
		p1.add(_ik_sch_lbl,              "");
		p1.add(_ik_sch_txt,              "growx, pushx, wrap");
		p1.add(_ik_val_lbl,              "");
		p1.add(_ik_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_ik_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_ik_filter_lbl,           "gap 5, split");
		p.add(_ik_filter_txt,           "growx, pushx");
		p.add(_ik_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_ik_tab), "grow, push");

		return p;
	}

	
	private JPanel createExportedKeysPanel()
	{
		_ek_tab = new ExportedKeysTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_ek_tab.refresh();
			}
		});
		
		JButton getTables_but = new JButton("...");
		getTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				ResultSetTableModel rstm = getTables(_conn, _ek_cat_txt.getText(), _ek_sch_txt.getText(), _ek_val_txt.getText(), null);
				ResultSetTableModel rstm = getTables(_conn, _ek_cat_txt.getText(), _ek_sch_txt.getText(), "", null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getTables()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_ek_cat_txt.setText(pickList.getSelectedValuesAsString("TABLE_CAT"));
					_ek_sch_txt.setText(pickList.getSelectedValuesAsString("TABLE_SCHEM"));
					_ek_val_txt.setText(pickList.getSelectedValuesAsString("TABLE_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getExportedKeys(catalog, schemaPattern, TablePattern)</code></b><br>"
				+ "</html>";
		
		_ek_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_ek_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_ek_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_ek_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_ek_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_ek_cat_lbl,              "");
		p1.add(_ek_cat_txt,              "growx, pushx, wrap");
		p1.add(_ek_sch_lbl,              "");
		p1.add(_ek_sch_txt,              "growx, pushx, wrap");
		p1.add(_ek_val_lbl,              "");
		p1.add(_ek_val_txt,              "growx, pushx, split");
		p1.add(getTables_but,            "wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_ek_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_ek_filter_lbl,           "gap 5, split");
		p.add(_ek_filter_txt,           "growx, pushx");
		p.add(_ek_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_ek_tab), "grow, push");

		return p;
	}

	
	private JPanel createProceduresPanel()
	{
		_pr_tab = new ProceduresTable(_conn);

		JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_pr_tab.refresh();
			}
		});
		
		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getProcedures(catalog, schemaPattern, procedurePattern)</code></b><br>"
				+ "</html>";
		
		_pr_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_pr_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_pr_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_pr_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_pr_val_txt.setToolTipText("<html>a procedure name pattern; must match the procedure name as it is stored in the database (default is %, which means all procedure)</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_pr_cat_lbl,              "");
		p1.add(_pr_cat_txt,              "growx, pushx, wrap");
		p1.add(_pr_sch_lbl,              "");
		p1.add(_pr_sch_txt,              "growx, pushx, wrap");
		p1.add(_pr_val_lbl,              "");
		p1.add(_pr_val_txt,              "growx, pushx, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_pr_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_pr_filter_lbl,           "gap 5, split");
		p.add(_pr_filter_txt,           "growx, pushx");
		p.add(_pr_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_pr_tab), "grow, push");

		return p;
	}

	
	private JPanel createFunctionsPanel()
	{
		_fu_tab = new FunctionsTable(_conn);

		JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_fu_tab.refresh();
			}
		});
		
		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getFunctions(catalog, schemaPattern, functionPattern)</code></b><br>"
				+ "</html>";
		
		_fu_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_fu_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_fu_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_fu_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_fu_val_txt.setToolTipText("<html>a procedure name pattern; must match the procedure name as it is stored in the database (default is %, which means all procedure)</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_fu_cat_lbl,              "");
		p1.add(_fu_cat_txt,              "growx, pushx, wrap");
		p1.add(_fu_sch_lbl,              "");
		p1.add(_fu_sch_txt,              "growx, pushx, wrap");
		p1.add(_fu_val_lbl,              "");
		p1.add(_fu_val_txt,              "growx, pushx, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_fu_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_fu_filter_lbl,           "gap 5, split");
		p.add(_fu_filter_txt,           "growx, pushx");
		p.add(_fu_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_fu_tab), "grow, push");

		return p;
	}

	
	private JPanel createFunctionColumnsPanel()
	{
		_fc_tab = new FunctionColumnsTable(_conn);

		final JButton refresh_but = new JButton("Refresh");
		refresh_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_fc_tab.refresh();
			}
		});
		
		JButton getFunctions_but = new JButton("...");
		getFunctions_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ResultSetTableModel rstm = getFunctions(_conn, _fc_cat_txt.getText(), _fc_sch_txt.getText(), _fc_val_txt.getText(), null);

				// Show a list
				SqlPickList pickList = new SqlPickList(getOwner(), rstm, "getFunctions()", false);
				pickList.setVisible(true);
				
				if (pickList.wasOkPressed())
				{
					_fc_cat_txt.setText(pickList.getSelectedValuesAsString("FUNCTION_CAT"));
					_fc_sch_txt.setText(pickList.getSelectedValuesAsString("FUNCTION_SCHEM"));
					_fc_val_txt.setText(pickList.getSelectedValuesAsString("FUNCTION_NAME"));
					refresh_but.doClick();
				}
			}
		});

		String desc = 
				"<html>"
				+ "<b>Below information is from <code>Connection.getMetaData().getColumns(catalog, schemaPattern, TablePattern, ColumnPattern)</code></b><br>"
				+ "</html>";
		
		_fc_filter_txt.setToolTipText("Filter that does regular expression on all table cells using this value");
		_fc_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		_fc_cat_txt.setToolTipText("<html>a catalog name; must match the catalog name as it is stored in the database; <i>empty string</i> retrieves those without a catalog; <code>null</code> means that the catalog name should not be used to narrow the search</html>");
		_fc_sch_txt.setToolTipText("<html>a schema name pattern; must match the schema name as it is stored in the database; <i>empty string</i> retrieves those without a schema; <code>null</code> means that the schema name should not be used to narrow the search</html>");
		_fc_val_txt.setToolTipText("<html>a table name pattern; must match the table name as it is stored in the database (default is %, which means all tables)</html>");
		_fc_col_txt.setToolTipText("<html>a column name pattern; must match the column name as it is stored in the database</html>");

		JPanel p1 = new JPanel(new MigLayout());
		p1.add(_fc_cat_lbl,              "");
		p1.add(_fc_cat_txt,              "growx, pushx, wrap");
		p1.add(_fc_sch_lbl,              "");
		p1.add(_fc_sch_txt,              "growx, pushx, wrap");
		p1.add(_fc_val_lbl,              "");
		p1.add(_fc_val_txt,              "growx, pushx, split");
		p1.add(getFunctions_but,         "wrap");
		p1.add(_fc_col_lbl,              "");
		p1.add(_fc_col_txt,              "growx, pushx, wrap");

		p1.add(refresh_but,              "skip 1, split");
		p1.add(_fc_api_lbl,              "wrap 10");


		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JLabel(desc),         "gap 5 5 5 5, growx, pushx, wrap");
		p.add(p1,                       "growx, pushx, wrap");
		p.add(_fc_filter_lbl,           "gap 5, split");
		p.add(_fc_filter_txt,           "growx, pushx");
		p.add(_fc_filter_cnt,           "gapright 5, wrap 10");

		p.add(new JScrollPane(_fc_tab), "grow, push");

		return p;
	}

	
	public void applyKfFilter() { applyFilter(_kf_filter_txt, _kf_tab, _kf_filter_cnt); }
	public void applyMdFilter() { applyFilter(_md_filter_txt, _md_tab, _md_filter_cnt); }
	public void applyDtFilter() { applyFilter(_dt_filter_txt, _dt_tab, _dt_filter_cnt); }
	public void applyTtFilter() { applyFilter(_tt_filter_txt, _tt_tab, _tt_filter_cnt); }
	public void applyDpFilter() { applyFilter(_dp_filter_txt, _dp_tab, _dp_filter_cnt); }
	public void applyCaFilter() { applyFilter(_ca_filter_txt, _ca_tab, _ca_filter_cnt); }
	public void applyScFilter() { applyFilter(_sc_filter_txt, _sc_tab, _sc_filter_cnt); }
	public void applyTaFilter() { applyFilter(_ta_filter_txt, _ta_tab, _ta_filter_cnt); }
	public void applyCoFilter() { applyFilter(_co_filter_txt, _co_tab, _co_filter_cnt); }
	public void applyPkFilter() { applyFilter(_pk_filter_txt, _pk_tab, _pk_filter_cnt); }
	public void applyBrFilter() { applyFilter(_br_filter_txt, _br_tab, _br_filter_cnt); }
	public void applyIxFilter() { applyFilter(_ix_filter_txt, _ix_tab, _ix_filter_cnt); }
	public void applyIkFilter() { applyFilter(_ik_filter_txt, _ik_tab, _ik_filter_cnt); }
	public void applyEkFilter() { applyFilter(_ek_filter_txt, _ek_tab, _ek_filter_cnt); }
	public void applyPrFilter() { applyFilter(_pr_filter_txt, _pr_tab, _pr_filter_cnt); }
	public void applyFuFilter() { applyFilter(_fu_filter_txt, _fu_tab, _fu_filter_cnt); }
	public void applyFcFilter() { applyFilter(_fc_filter_txt, _fc_tab, _fc_filter_cnt); }
	public void applyCiFilter() { applyFilter(_ci_filter_txt, _ci_tab, _ci_filter_cnt); }

	public void applyFilter(JTextField filter_txt, GTable tab, JLabel cnt)
	{
		//------------------------------------------------------
		String searchString = filter_txt.getText().trim();
		if ( searchString.length() <= 0 ) 
			tab.setRowFilter(null);
		else
		{
			// Create a array with all visible columns... hence: it's only those we want to search
			// Note the indices are MODEL column index
			int[] mcols = new int[tab.getColumnCount()];
			for (int i=0; i<mcols.length; i++)
				mcols[i] = tab.convertColumnIndexToModel(i);
			
			tab.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString + ".*", mcols));
		}
		
		String rowc = tab.getRowCount() + "/" + tab.getModel().getRowCount();
		cnt.setText(rowc);
	}


	public boolean wasOkPressed()
	{
		return _okWasPressed;
	}


	/**
	 * 
	 * @param conn
	 * @param catalog
	 * @param schemaPattern
	 * @param valueNamePattern
	 * @param tableTypesStr default is to get type of TABLE
	 * @return
	 */
	public ResultSetTableModel getTables(Connection conn, String catalog, String schemaPattern, String valueNamePattern, String tableTypesStr)
	{
//		String[] tableTypes = new String[] {"TABLE"};
		String[] tableTypes = new String[] {"TABLE", "BASE TABLE"};

		if (StringUtil.isNullOrBlank(valueNamePattern))
			valueNamePattern = "";

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";
		if (tableTypesStr != null)
		{
    		if (tableTypesStr   .equalsIgnoreCase("null")) tableTypesStr    = null;
    		if (tableTypesStr   != null)                   tableTypes       = StringUtil.commaStrToArray(tableTypesStr);
		}

		try
		{
    		ResultSet rs = conn.getMetaData().getTables(catalog, schemaPattern, valueNamePattern, tableTypes);
    		ResultSetTableModel rstm = new ResultSetTableModel(rs, false, "JdbcMetaDataInfoDialog.TablesModel", null);
    		return rstm;
		}
		catch(SQLException ex)
		{
			SwingUtils.showErrorMessage(getOwner(), "Problems getting tables", "Problems getting tables", ex);
			return null;
		}
	}
	
	
	
	/**
	 * 
	 * @param conn
	 * @param catalog
	 * @param schemaPattern
	 * @param valueNamePattern
	 * @param tableTypesStr default is to get type of TABLE
	 * @return
	 */
	public ResultSetTableModel getFunctions(Connection conn, String catalog, String schemaPattern, String valueNamePattern, String tableTypesStr)
	{
//		String[] tableTypes = new String[] {"TABLE"};

		if (StringUtil.isNullOrBlank(valueNamePattern))
			valueNamePattern = "";

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";
		if (tableTypesStr != null)
		{
    		if (tableTypesStr   .equalsIgnoreCase("null")) tableTypesStr    = null;
//    		if (tableTypesStr   != null)                   tableTypes       = StringUtil.commaStrToArray(tableTypesStr);
		}

		try
		{
    		ResultSet rs = conn.getMetaData().getFunctions(catalog, schemaPattern, valueNamePattern);
    		ResultSetTableModel rstm = new ResultSetTableModel(rs, false, "JdbcMetaDataInfoDialog.FunctionsModel", null);
    		return rstm;
		}
		catch(SQLException ex)
		{
			SwingUtils.showErrorMessage(getOwner(), "Problems getting functions", "Problems getting functions", ex);
			return null;
		}
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	// Private classes for TABLES
	///////////////////////////////////////////////////////////////////////////////////////
	private abstract class ResultSetTable
	extends GTable
	{
		private static final long serialVersionUID = 1L;

		public ResultSetTable()
		{
			// Reset renderer for BigDecimal, GTable formated it in a little different way
			setDefaultRenderer(BigDecimal.class, null);

			// Set some table props
			setSortable(true);
			setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
			packAll();
			setColumnControlVisible(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			// NULL Values: SET BACKGROUND COLOR
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					// Check NULL value
					String cellValue = adapter.getString();
					if (cellValue == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(cellValue))
						return true;
					
					// Check ROWID Column
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					if (mcol == 0 && ResultSetTableModel.ROW_NUMBER_COLNAME.equals(colName))
						return true;

					return false;
				}
			}, ResultSetJXTable.NULL_VALUE_COLOR, null));
		}
	}

	private class KeywordsFunctionsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public KeywordsFunctionsTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.KeywordsFunctionsTable");

			setModel(createKeywordsFunctionsModel(conn));

			packAll();
		}
	}

	private class MetaDataTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public MetaDataTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.MetaDataTable");

			setModel(createMetaDataModel(conn));

			packAll();
		}

		
//		private Class<? extends Object> editingClass;
//
//		@Override
//		public TableCellRenderer getCellRenderer(int row, int column)
//		{
//			editingClass = null;
//			int modelColumn = convertColumnIndexToModel(column);
//			if ( modelColumn == 1 )
//			{
//				Class<? extends Object> rowClass = getModel().getValueAt(row, modelColumn).getClass();
//				return getDefaultRenderer(rowClass);
//			}
//			else
//			{
//				return super.getCellRenderer(row, column);
//			}
//		}
//
//		@Override
//		public TableCellEditor getCellEditor(int row, int column)
//		{
//			editingClass = null;
//			int modelColumn = convertColumnIndexToModel(column);
//			if ( modelColumn == 1 )
//			{
//				editingClass = getModel().getValueAt(row, modelColumn).getClass();
//				return getDefaultEditor(editingClass);
//			}
//			else
//			{
//				return super.getCellEditor(row, column);
//			}
//		}
//
//		// This method is also invoked by the editor when the value in the editor
//		// component is saved in the TableModel. The class was saved when the
//		// editor was invoked so the proper class can be created.
//		@Override
//		public Class<?> getColumnClass(int column)
//		{
//			return editingClass != null ? editingClass : super.getColumnClass(column);
//		}
	}
	
	private class DataTypeTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public DataTypeTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.DataTypeTable");

			setModel(createDataTypeModel(conn));

			packAll();
		}
	}
	

	private class TableTypesTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public TableTypesTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.TableTypesTable");

			setModel(createTableTypesModel(conn));

			packAll();
		}
	}
	

	private class DriverPropsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public DriverPropsTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.DriverPropsTable");

			setModel(createDriverPropsModel(conn));

			packAll();
		}
	}
	

	private class CatalogsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public CatalogsTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.CatalogsTable");

			setModel(createCatalogsTableModel(conn));

			packAll();
		}
	}
	

	private class SchemasTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public SchemasTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.SchemasTable");

			setModel(createSchemasTableModel(conn));

			packAll();
		}
	}
	
	private class TablesTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public TablesTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.TablesTable");
		}
		
		public void refresh()
		{
			setModel(createTablesTableModel(_conn));
			packAll();
		}
	}
	
	private class ColumnsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public ColumnsTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.ColumnsTable");
		}
		
		public void refresh()
		{
			setModel(createColumnsTableModel(_conn));
			packAll();
		}
	}
	
	private class TablesPkTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public TablesPkTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.TablesPkTable");
		}
		
		public void refresh()
		{
			setModel(createTablesPkTableModel(_conn));
			packAll();
		}
	}
	
	private class BestRowTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public BestRowTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.BestRowTable");
		}
		
		public void refresh()
		{
			setModel(createBestRowTableModel(_conn));
			packAll();
		}
	}
	
	private class IndexTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public IndexTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.IndexTable");
		}
		
		public void refresh()
		{
			setModel(createIndexTableModel(_conn));
			packAll();
		}
	}
	
	private class ImportedKeysTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public ImportedKeysTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.ImportedKeysTable");
		}
		
		public void refresh()
		{
			setModel(createImportedKeysTableModel(_conn));
			packAll();
		}
	}
	
	private class ExportedKeysTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public ExportedKeysTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.ExporetdKeysTable");
		}
		
		public void refresh()
		{
			setModel(createExportedKeysTableModel(_conn));
			packAll();
		}
	}
	
	private class ProceduresTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public ProceduresTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.ProceduresTable");
		}
		
		public void refresh()
		{
			setModel(createProceduresTableModel(_conn));
			packAll();
		}
	}
	
	private class FunctionsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public FunctionsTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.FunctionsTable");
		}
		
		public void refresh()
		{
			setModel(createFunctionsTableModel(_conn));
			packAll();
		}
	}
	
	private class FunctionColumnsTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;
		private Connection _conn = null;

		public FunctionColumnsTable(Connection conn)
		{
			super();
			_conn = conn;

			// name of the table
			setName("JdbcMetaDataInfoDialog.FunctionColumnsTable");
		}
		
		public void refresh()
		{
			setModel(createFunctionColumnsTableModel(_conn));
			packAll();
		}
	}
	
	private class ConnInfoTable
	extends ResultSetTable
	{
		private static final long serialVersionUID = 1L;

		public ConnInfoTable(Connection conn)
		{
			super();

			// name of the table
			setName("JdbcMetaDataInfoDialog.ConnInfoTable");

			setModel(createConnInfoModel(conn));

			// Set some table props
			packAll();
		}
	}


	private static void addJdbcDataTypeStr(ResultSetTableModel rstm)
	{
		// Add column 'JdbcDataTypeStr' if we find 'DATA_TYPE'
		int jdbcDataType_pos = rstm.findColumnNoCase("DATA_TYPE");
		if (jdbcDataType_pos != -1)
		{
			int addPos = jdbcDataType_pos + 1;
			rstm.addColumn("JdbcDataTypeStr", addPos, Types.VARCHAR, "varchar", "varchar(60)", 60, -1, "", String.class);
			
			// initialize value for: JdbcDataTypeStr
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				int jdbcType = rstm.getValueAsInteger(r, jdbcDataType_pos);
				String jdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType);
				
//				if (jdbcTypeStr.startsWith("java.sql."))
//					jdbcTypeStr = jdbcTypeStr.substring("java.sql.".length());

				rstm.setValueAtWithOverride(jdbcTypeStr, r, addPos);
			}
		}
		
	}
	

	///////////////////////////////////////////////////////////////////////////////////////
	// Private classes for CREATE TABLE MODELS
	///////////////////////////////////////////////////////////////////////////////////////
	public TableModel createKeywordsFunctionsModel(Connection conn)
	{
		String method = "";
		try
		{
			method = "getMetaData()"; DatabaseMetaData md = conn.getMetaData();

			method = "getSQLKeywords()";       List<String> keywords = StringUtil.commaStrToList(md.getSQLKeywords());
			method = "getStringFunctions()";   List<String> strFunc  = StringUtil.commaStrToList(md.getStringFunctions());
			method = "getNumericFunctions()";  List<String> numFunc  = StringUtil.commaStrToList(md.getNumericFunctions());
			method = "getTimeDateFunctions()"; List<String> dateFunc = StringUtil.commaStrToList(md.getTimeDateFunctions());
			method = "getSystemFunctions()";   List<String> sysFunc  = StringUtil.commaStrToList(md.getSystemFunctions());

			Vector<String> cols = new Vector<String>();
			cols.add("SQL Keyword");
			cols.add("String Functions");
			cols.add("Numeric Functions");
			cols.add("Time/Date Functions");
			cols.add("System Functions");

			int maxRows = 0;
			maxRows = Math.max(maxRows, keywords.size());
			maxRows = Math.max(maxRows, strFunc.size());
			maxRows = Math.max(maxRows, numFunc.size());
			maxRows = Math.max(maxRows, dateFunc.size());
			maxRows = Math.max(maxRows, sysFunc.size());
			
			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();

			for (int i=0; i<maxRows; i++)
			{
				Vector<Object> row = new Vector<Object>();
				
				row.add(i < keywords.size() ? keywords.get(i) : null);
				row.add(i < strFunc .size() ? strFunc .get(i) : null);
				row.add(i < numFunc .size() ? numFunc .get(i) : null);
				row.add(i < dateFunc.size() ? dateFunc.get(i) : null);
				row.add(i < sysFunc .size() ? sysFunc .get(i) : null);

				rows.add(row);
			}

			return new DefaultTableModel(rows, cols);
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData()."+method);
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createDriverPropsModel(Connection conn)
	{
		try
		{
			String url = conn.getMetaData().getURL();

			Properties info = new Properties();
			Driver driver = DriverManager.getDriver(url);
	
			DriverPropertyInfo[] attributes = driver.getPropertyInfo(url, info);

			Vector<String> cols = new Vector<String>();
			cols.add("Name");
			cols.add("Required");
			cols.add("Value");
			cols.add("Choices");
			cols.add("Description");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();

			if (attributes != null)
			{
				for (int i = 0; i < attributes.length; i++)
				{
					// get the property metadata
					String   name        = attributes[i].name;
					String[] choicesArr  = attributes[i].choices;
					boolean  required    = attributes[i].required;
					String   description = attributes[i].description;
					String   value       = attributes[i].value;

					if ("VERSIONSTRING".equals(name))
						continue;

					Vector<Object> row = new Vector<Object>();
					row.add(name);
					row.add(required);
					row.add(value);
					row.add(StringUtil.toCommaStr(choicesArr));
					row.add(description);
					rows.add(row);
				}
			}

			return new DefaultTableModel(rows, cols);
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getClientInfoProperties()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

//	public TableModel createDriverPropsModel(Connection conn)
//	{
//		try
//		{
//			ResultSet dataTypes = conn.getMetaData().getClientInfoProperties();
//			ResultSetTableModel rstm = new ResultSetTableModel(dataTypes, "JdbcMetaDataInfoDialog.DriverPropsModel");
//			return rstm;
//		}
//		catch (Throwable e)
//		{
//			Vector<String> cols = new Vector<String>();
//			cols.add("Method");
//			cols.add("Problems");
//
//			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
//			Vector<Object> row = new Vector<Object>();
//
//			row.add("conn.getMetaData().getClientInfoProperties()");
//			row.add(e.getMessage());
//			rows.add(row);
//
//			return new DefaultTableModel(rows, cols);
//		}
//	}

	public TableModel createDataTypeModel(Connection conn)
	{
		try
		{
			ResultSet dataTypes = conn.getMetaData().getTypeInfo();
			ResultSetTableModel rstm = new ResultSetTableModel(dataTypes, "JdbcMetaDataInfoDialog.DataTypeModel");

			// Add column 'JdbcDataTypeStr' after 'DATA_TYPE' (if it exists)
			addJdbcDataTypeStr(rstm);
			
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getTypeInfo()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createTableTypesModel(Connection conn)
	{
		try
		{
			ResultSet dataTypes = conn.getMetaData().getTableTypes();
			ResultSetTableModel rstm = new ResultSetTableModel(dataTypes, "JdbcMetaDataInfoDialog.TableTypesModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getTableTypes()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createConnInfoModel(Connection conn)
	{
		try
		{
			Properties props = conn.getClientInfo();

			if (props == null)
				throw new SQLException("Connection.getClientInfo() returned null. So the getClientInfo() isn't supported by the current driver.");
			
			Vector<String> cols = new Vector<String>();
			cols.add("Keys");
			cols.add("Values");

			Vector<Vector<String>> rows = new Vector<Vector<String>>();

			Enumeration<Object> e = props.keys();
			while(e.hasMoreElements())
			{
				String key = (String)e.nextElement();
				String val = props.getProperty(key);

				Vector<String> row = new Vector<String>();
				row.add(key);
				row.add(val);

				rows.add(row);
			}
			
			return new DefaultTableModel(rows, cols);
		}
		catch(Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getClientInfo()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}
	
	public TableModel createCatalogsTableModel(Connection conn)
	{
		try
		{
			ResultSet dataTypes = conn.getMetaData().getCatalogs();
			ResultSetTableModel rstm = new ResultSetTableModel(dataTypes, "JdbcMetaDataInfoDialog.CatalogsModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getCatalogs()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createSchemasTableModel(Connection conn)
	{
		try
		{
			ResultSet dataTypes = conn.getMetaData().getSchemas();
			ResultSetTableModel rstm = new ResultSetTableModel(dataTypes, "JdbcMetaDataInfoDialog.SchemasModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getSchemas()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createTablesTableModel(Connection conn)
	{
		String   catalog          = _ta_cat_txt.getText();
		String   schemaPattern    = _ta_sch_txt.getText();
		String   valueNamePattern = _ta_val_txt.getText();
		String[] tableTypes       = null;
		String   tableTypesStr    = _ta_typ_txt.getText();;

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";
		if (tableTypesStr   .equalsIgnoreCase("null")) tableTypesStr    = null;
		if (tableTypesStr   != null)                   tableTypes       = StringUtil.commaStrToArray(tableTypesStr);

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		String tableTypesDesc       = tableTypes       == null ? "null" : Arrays.asList(tableTypes)+"";
		
		String apiCall = "parameters to getTables("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+tableTypesDesc+")";
		_ta_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getTables(catalog, schemaPattern, valueNamePattern, tableTypes);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.TablesModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getTables()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createColumnsTableModel(Connection conn)
	{
		String   catalog          = _co_cat_txt.getText();
		String   schemaPattern    = _co_sch_txt.getText();
		String   valueNamePattern = _co_val_txt.getText();
		String   columnNamePattern= _co_col_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		String columnTypesDesc      = columnNamePattern== null ? "null" : '"' + columnNamePattern + '"';
		
		String apiCall = "parameters to getColumns("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+columnTypesDesc+")";
		_co_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getColumns(catalog, schemaPattern, valueNamePattern, columnNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.ColumnsModel");

			// Add column 'JdbcDataTypeStr' after 'DATA_TYPE' (if it exists)
			addJdbcDataTypeStr(rstm);
			
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getColumns()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createTablesPkTableModel(Connection conn)
	{
		String   catalog          = _pk_cat_txt.getText();
		String   schemaPattern    = _pk_sch_txt.getText();
		String   valueNamePattern = _pk_val_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getPrimaryKeys("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+")";
		_pk_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getPrimaryKeys(catalog, schemaPattern, valueNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.TablesPkModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getPrimaryKeys()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createBestRowTableModel(Connection conn)
	{
		String   catalog          = _br_cat_txt.getText();
		String   schemaPattern    = _br_sch_txt.getText();
		String   valueNamePattern = _br_val_txt.getText();
		int      scope            = ((BestRow)_br_scope_cbx.getSelectedItem()).getJdbcNumber();
		boolean  nullable         = _br_nullable_chk.isSelected();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getBestRowIdentifier("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+scope+", "+nullable+")";
		_br_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getBestRowIdentifier(catalog, schemaPattern, valueNamePattern, scope, nullable);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.BestRowModel");

			// Add column 'JdbcDataTypeStr' after 'DATA_TYPE' (if it exists)
			addJdbcDataTypeStr(rstm);
			
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getIndexInfo()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createIndexTableModel(Connection conn)
	{
		String   catalog          = _ix_cat_txt.getText();
		String   schemaPattern    = _ix_sch_txt.getText();
		String   valueNamePattern = _ix_val_txt.getText();
		boolean  unique           = _ix_unique_chk.isSelected();
		boolean  approximate      = _ix_approx_chk.isSelected();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getIndexInfo("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+unique+", "+approximate+")";
		_ix_api_lbl.setText(apiCall);

		if (_ix_useOrigin_chk.isVisible())
		{
			boolean useOriginImpl = _ix_useOrigin_chk.isSelected();
			
			System.setProperty(DbxDatabaseMetaDataSqlServer.PROPKEY_useOriginImpementation_getIndexInfo, useOriginImpl + "");
		}

		try
		{
			ResultSet rs = conn.getMetaData().getIndexInfo(catalog, schemaPattern, valueNamePattern, unique, approximate);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.IndexModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getIndexInfo()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createImportedKeysTableModel(Connection conn)
	{
		String   catalog          = _ik_cat_txt.getText();
		String   schemaPattern    = _ik_sch_txt.getText();
		String   valueNamePattern = _ik_val_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getImportedKeys("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+")";
		_ik_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getImportedKeys(catalog, schemaPattern, valueNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.ImportedKeysModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getExportedKeys()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createExportedKeysTableModel(Connection conn)
	{
		String   catalog          = _ek_cat_txt.getText();
		String   schemaPattern    = _ek_sch_txt.getText();
		String   valueNamePattern = _ek_val_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getExportedKeys("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+")";
		_ek_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getExportedKeys(catalog, schemaPattern, valueNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.ExportedKeysModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getImportedKeys()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createProceduresTableModel(Connection conn)
	{
		String   catalog          = _pr_cat_txt.getText();
		String   schemaPattern    = _pr_sch_txt.getText();
		String   valueNamePattern = _pr_val_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getProcedures("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+")";
		_pr_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getProcedures(catalog, schemaPattern, valueNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.ProceduresModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getTables()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createFunctionsTableModel(Connection conn)
	{
		String   catalog          = _fu_cat_txt.getText();
		String   schemaPattern    = _fu_sch_txt.getText();
		String   valueNamePattern = _fu_val_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		
		String apiCall = "parameters to getFunctions("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+")";
		_fu_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getFunctions(catalog, schemaPattern, valueNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.FunctionsModel");
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getTables()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}

	public TableModel createFunctionColumnsTableModel(Connection conn)
	{
		String   catalog          = _fc_cat_txt.getText();
		String   schemaPattern    = _fc_sch_txt.getText();
		String   valueNamePattern = _fc_val_txt.getText();
		String   colNamePattern   = _fc_col_txt.getText();

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
//		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";

		String catalogDesc          = catalog          == null ? "null" : '"' + catalog          + '"';
		String schemaPatternDesc    = schemaPattern    == null ? "null" : '"' + schemaPattern    + '"';
		String valueNamePatternDesc = valueNamePattern == null ? "null" : '"' + valueNamePattern + '"';
		String colNamePatternDesc   = colNamePattern   == null ? "null" : '"' + colNamePattern   + '"';
		
		String apiCall = "parameters to getFunctionColumns("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+colNamePatternDesc+")";
		_fc_api_lbl.setText(apiCall);

		try
		{
			ResultSet rs = conn.getMetaData().getFunctionColumns(catalog, schemaPattern, valueNamePattern, colNamePattern);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.FunctionColumnsModel");

			// Add column 'JdbcDataTypeStr' after 'DATA_TYPE' (if it exists)
			addJdbcDataTypeStr(rstm);
			
			return rstm;
		}
		catch (Throwable e)
		{
			Vector<String> cols = new Vector<String>();
			cols.add("Method");
			cols.add("Problems");

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			Vector<Object> row = new Vector<Object>();

			row.add("conn.getMetaData().getFunctionColumns()");
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
	}


	public TableModel createMetaDataModel(Connection conn)
	{
		Vector<String> cols = new Vector<String>();
		cols.add("Method");
		cols.add("Value");
		cols.add("Description");

		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
		Vector<Object> row;

		Object val = "";
		DatabaseMetaData md;
		try
		{
			md = conn.getMetaData();
		}
		catch (Throwable e)
		{
			row = new Vector<Object>();
			row.add("conn.getMetaData()");
			row.add(e);
			row.add(e.getMessage());
			rows.add(row);

			return new DefaultTableModel(rows, cols);
		}
		

		//-------------------------------------------------------------------------
		try { val = md.allProceduresAreCallable(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("allProceduresAreCallable()");
		row.add(val);
		row.add("Retrieves whether the current user can call all the procedures returned by the method getProcedures.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.allTablesAreSelectable(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("allTablesAreSelectable()");
		row.add(val);
		row.add("Retrieves whether the current user can use all the tables returned by the method getTables in a SELECT statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.autoCommitFailureClosesAllResultSets(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("autoCommitFailureClosesAllResultSets()");
		row.add(val);
		row.add("Retrieves whether a SQLException while autoCommit is true inidcates that all open ResultSets are closed, even ones that are holdable.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.dataDefinitionCausesTransactionCommit(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("dataDefinitionCausesTransactionCommit()");
		row.add(val);
		row.add("Retrieves whether a data definition statement within a transaction forces the transaction to commit.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.dataDefinitionIgnoredInTransactions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("dataDefinitionIgnoredInTransactions()");
		row.add(val);
		row.add("Retrieves whether this database ignores a data definition statement within a transaction.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.deletesAreDetected(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("deletesAreDetected(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether or not a visible row delete can be detected by calling the method ResultSet.rowDeleted. If the method deletesAreDetected returns false, it means that deleted rows are removed from the result set.");
		rows.add(row);
		
		try { val = md.deletesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("deletesAreDetected(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row delete can be detected by calling the method ResultSet.rowDeleted. If the method deletesAreDetected returns false, it means that deleted rows are removed from the result set.");
		rows.add(row);
		
		try { val = md.deletesAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("deletesAreDetected(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row delete can be detected by calling the method ResultSet.rowDeleted. If the method deletesAreDetected returns false, it means that deleted rows are removed from the result set.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		try { val = md.doesMaxRowSizeIncludeBlobs(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("doesMaxRowSizeIncludeBlobs()");
		row.add(val);
		row.add("Retrieves whether the return value for the method getMaxRowSize includes the SQL data types LONGVARCHAR and LONGVARBINARY.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
//		try { val = md.generatedKeyAlwaysReturned(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("generatedKeyAlwaysReturned()");
//		row.add(val);
//		row.add("Retrieves whether a generated key will always be returned if the column name(s) or index(es) specified for the auto generated key column(s) are valid and the statement succeeds.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getCatalogSeparator(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getCatalogSeparator()");
		row.add(val);
		row.add("Retrieves the String that this database uses as the separator between a catalog and table name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getCatalogTerm(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getCatalogTerm()");
		row.add(val);
		row.add("Retrieves the database vendor's preferred term for \"catalog\".");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getDatabaseMajorVersion(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getDatabaseMajorVersion()");
		row.add(val);
		row.add("Retrieves the major version number of the underlying database.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getDatabaseMinorVersion(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getDatabaseMinorVersion()");
		row.add(val);
		row.add("Retrieves the minor version number of the underlying database.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getDatabaseProductName(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getDatabaseProductName()");
//		row.add(val);
//		row.add("Retrieves the name of this database product.");
//		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getDatabaseProductVersion(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getDatabaseProductVersion()");
//		row.add(val);
//		row.add("Retrieves the version number of this database product.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getDefaultTransactionIsolation(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getDefaultTransactionIsolation()");
		row.add(val);
		row.add("Retrieves this database's default transaction isolation level.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		val = md.getDriverMajorVersion();
		row = new Vector<Object>();
		row.add("getDriverMajorVersion()");
		row.add(val);
		row.add("Retrieves this JDBC driver's major version number.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		val = md.getDriverMinorVersion();
		row = new Vector<Object>();
		row.add("getDriverMinorVersion()");
		row.add(val);
		row.add("Retrieves this JDBC driver's minor version number.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getDriverName(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getDriverName()");
//		row.add(val);
//		row.add("Retrieves the name of this JDBC driver.");
//		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getDriverVersion(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getDriverVersion()");
//		row.add(val);
//		row.add("Retrieves the version number of this JDBC driver as a String.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getExtraNameCharacters(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getExtraNameCharacters()");
		row.add(val);
		row.add("Retrieves all the \"extra\" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getIdentifierQuoteString(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getIdentifierQuoteString()");
		row.add(val);
		row.add("Retrieves the string used to quote SQL identifiers.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getJDBCMajorVersion(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getJDBCMajorVersion()");
		row.add(val);
		row.add("Retrieves the major JDBC version number for this driver.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getJDBCMinorVersion(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getJDBCMinorVersion()");
		row.add(val);
		row.add("Retrieves the minor JDBC version number for this driver.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxBinaryLiteralLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxBinaryLiteralLength()");
		row.add(val);
		row.add("Retrieves the maximum number of hex characters this database allows in an inline binary literal.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxCatalogNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxCatalogNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters that this database allows in a catalog name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxCharLiteralLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxCharLiteralLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters this database allows for a character literal.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters this database allows for a column name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnsInGroupBy(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnsInGroupBy()");
		row.add(val);
		row.add("Retrieves the maximum number of columns this database allows in a GROUP BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnsInIndex(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnsInIndex()");
		row.add(val);
		row.add("Retrieves the maximum number of columns this database allows in an index.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnsInOrderBy(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnsInOrderBy()");
		row.add(val);
		row.add("Retrieves the maximum number of columns this database allows in an ORDER BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnsInSelect(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnsInSelect()");
		row.add(val);
		row.add("Retrieves the maximum number of columns this database allows in a SELECT list.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxColumnsInTable(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxColumnsInTable()");
		row.add(val);
		row.add("Retrieves the maximum number of columns this database allows in a table.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxConnections(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxConnections()");
		row.add(val);
		row.add("Retrieves the maximum number of concurrent connections to this database that are possible.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxCursorNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxCursorNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters that this database allows in a cursor name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxIndexLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxIndexLength()");
		row.add(val);
		row.add("Retrieves the maximum number of bytes this database allows for an index, including all of the parts of the index.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxProcedureNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxProcedureNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters that this database allows in a procedure name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxRowSize(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxRowSize()");
		row.add(val);
		row.add("Retrieves the maximum number of bytes this database allows in a single row.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxSchemaNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxSchemaNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters that this database allows in a schema name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxStatementLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxStatementLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters this database allows in an SQL statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxStatements(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxStatements()");
		row.add(val);
		row.add("Retrieves the maximum number of active statements to this database that can be open at the same time.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxTableNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxTableNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters this database allows in a table name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxTablesInSelect(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxTablesInSelect()");
		row.add(val);
		row.add("Retrieves the maximum number of tables this database allows in a SELECT statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getMaxUserNameLength(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getMaxUserNameLength()");
		row.add(val);
		row.add("Retrieves the maximum number of characters this database allows in a user name.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getNumericFunctions(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getNumericFunctions()");
//		row.add(val);
//		row.add("Retrieves a comma-separated list of math functions available with this database.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getProcedureTerm(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getProcedureTerm()");
		row.add(val);
		row.add("Retrieves the database vendor's preferred term for \"procedure\".");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getResultSetHoldability(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getResultSetHoldability()");
		row.add(val);
		row.add("Retrieves this database's default holdability for ResultSet objects.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getRowIdLifetime(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getRowIdLifetime()");
		row.add(val);
		row.add("Indicates whether or not this data source supports the SQL ROWID type, and if so the lifetime for which a RowId object remains valid.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getSchemaTerm(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getSchemaTerm()");
		row.add(val);
		row.add("Retrieves the database vendor's preferred term for \"schema\".");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getSearchStringEscape(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getSearchStringEscape()");
		row.add(val);
		row.add("Retrieves the string that can be used to escape wildcard characters.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getSQLKeywords(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getSQLKeywords()");
//		row.add(val);
//		row.add("Retrieves a comma-separated list of all of this database's SQL keywords that are NOT also SQL:2003 keywords.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getSQLStateType(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getSQLStateType()");
		row.add(val);
		row.add("Indicates whether the SQLSTATE returned by SQLException.getSQLState is X/Open (now known as Open Group) SQL CLI or SQL:2003.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getStringFunctions(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getStringFunctions()");
//		row.add(val);
//		row.add("Retrieves a comma-separated list of string functions available with this database.");
//		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getSystemFunctions(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getSystemFunctions()");
//		row.add(val);
//		row.add("Retrieves a comma-separated list of system functions available with this database.");
//		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getTableTypes(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getTableTypes()");
//		row.add(val);
//		row.add("Retrieves the table types available in this database.");
//		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.getTimeDateFunctions(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getTimeDateFunctions()");
//		row.add(val);
//		row.add("Retrieves a comma-separated list of the time and date functions available with this database.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
//		try { val = md.getTypeInfo(); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("getTypeInfo()");
//		row.add(val);
//		row.add("Retrieves a description of all the data types supported by this database.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getURL(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getURL()");
		row.add(val);
		row.add("Retrieves the URL for this DBMS.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.getUserName(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("getUserName()");
		row.add(val);
		row.add("Retrieves the user name as known to this database.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.insertsAreDetected(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("insertsAreDetected(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether or not a visible row insert can be detected by calling the method ResultSet.rowInserted.");
		rows.add(row);
		
		try { val = md.insertsAreDetected( ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("insertsAreDetected(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row insert can be detected by calling the method ResultSet.rowInserted.");
		rows.add(row);
		
		try { val = md.insertsAreDetected( ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("insertsAreDetected(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row insert can be detected by calling the method ResultSet.rowInserted.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		try { val = md.isCatalogAtStart(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("isCatalogAtStart()");
		row.add(val);
		row.add("Retrieves whether a catalog appears at the start of a fully qualified table name.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.isReadOnly(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("isReadOnly()");
		row.add(val);
		row.add("Retrieves whether this database is in read-only mode.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.locatorsUpdateCopy(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("locatorsUpdateCopy()");
		row.add(val);
		row.add("Indicates whether updates made to a LOB are made on a copy or directly to the LOB.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.nullPlusNonNullIsNull(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("nullPlusNonNullIsNull()");
		row.add(val);
		row.add("Retrieves whether this database supports concatenations between NULL and non-NULL values being NULL.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.nullsAreSortedAtEnd(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("nullsAreSortedAtEnd()");
		row.add(val);
		row.add("Retrieves whether NULL values are sorted at the end regardless of sort order.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.nullsAreSortedAtStart(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("nullsAreSortedAtStart()");
		row.add(val);
		row.add("Retrieves whether NULL values are sorted at the start regardless of sort order.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.nullsAreSortedHigh(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("nullsAreSortedHigh()");
		row.add(val);
		row.add("Retrieves whether NULL values are sorted high.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.nullsAreSortedLow(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("nullsAreSortedLow()");
		row.add(val);
		row.add("Retrieves whether NULL values are sorted low.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.othersDeletesAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersDeletesAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether deletes made by others are visible.");
		rows.add(row);
		
		try { val = md.othersDeletesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersDeletesAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether deletes made by others are visible.");
		rows.add(row);
		
		try { val = md.othersDeletesAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersDeletesAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether deletes made by others are visible.");
		rows.add(row);
		// --- end
		

		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.othersInsertsAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersInsertsAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether inserts made by others are visible..");
		rows.add(row);
		
		try { val = md.othersInsertsAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersInsertsAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether inserts made by others are visible..");
		rows.add(row);
		
		try { val = md.othersInsertsAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersInsertsAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether inserts made by others are visible..");
		rows.add(row);
		// --- end		


		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.othersUpdatesAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersUpdatesAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether updates made by others are visible..");
		rows.add(row);
		
		try { val = md.othersUpdatesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersUpdatesAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether updates made by others are visible..");
		rows.add(row);
		
		try { val = md.othersUpdatesAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("othersUpdatesAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether updates made by others are visible..");
		rows.add(row);
		// --- end		
		

		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.ownDeletesAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownDeletesAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether a result set's own deletes are visible..");
		rows.add(row);
		
		try { val = md.ownDeletesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownDeletesAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether a result set's own deletes are visible..");
		rows.add(row);
		
		try { val = md.ownDeletesAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownDeletesAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether a result set's own deletes are visible..");
		rows.add(row);
		// --- end		
		

		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.ownInsertsAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownInsertsAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether a result set's own inserts are visible..");
		rows.add(row);
		
		try { val = md.ownInsertsAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownInsertsAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether a result set's own inserts are visible..");
		rows.add(row);
		
		try { val = md.ownInsertsAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownInsertsAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether a result set's own inserts are visible..");
		rows.add(row);
		// --- end		
		

		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.ownUpdatesAreVisible(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownUpdatesAreVisible(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether for the given type of ResultSet object, the result set's own updates are visible..");
		rows.add(row);
		
		try { val = md.ownUpdatesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownUpdatesAreVisible(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether for the given type of ResultSet object, the result set's own updates are visible..");
		rows.add(row);
		
		try { val = md.ownUpdatesAreVisible(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("ownUpdatesAreVisible(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether for the given type of ResultSet object, the result set's own updates are visible..");
		rows.add(row);
		// --- end		
		

		//-------------------------------------------------------------------------
		try { val = md.storesLowerCaseIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesLowerCaseIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in lower case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.storesLowerCaseQuotedIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesLowerCaseQuotedIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in lower case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.storesMixedCaseIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesMixedCaseIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in mixed case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.storesMixedCaseQuotedIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesMixedCaseQuotedIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in mixed case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.storesUpperCaseIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesUpperCaseIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in upper case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.storesUpperCaseQuotedIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("storesUpperCaseQuotedIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in upper case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsAlterTableWithAddColumn(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsAlterTableWithAddColumn()");
		row.add(val);
		row.add("Retrieves whether this database supports ALTER TABLE with add column.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsAlterTableWithDropColumn(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsAlterTableWithDropColumn()");
		row.add(val);
		row.add("Retrieves whether this database supports ALTER TABLE with drop column.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsANSI92EntryLevelSQL(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsANSI92EntryLevelSQL()");
		row.add(val);
		row.add("Retrieves whether this database supports the ANSI92 entry level SQL grammar.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsANSI92FullSQL(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsANSI92FullSQL()");
		row.add(val);
		row.add("Retrieves whether this database supports the ANSI92 full SQL grammar supported.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsANSI92IntermediateSQL(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsANSI92IntermediateSQL()");
		row.add(val);
		row.add("Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsBatchUpdates(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsBatchUpdates()");
		row.add(val);
		row.add("Retrieves whether this database supports batch updates.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCatalogsInDataManipulation(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCatalogsInDataManipulation()");
		row.add(val);
		row.add("Retrieves whether a catalog name can be used in a data manipulation statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCatalogsInIndexDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCatalogsInIndexDefinitions()");
		row.add(val);
		row.add("Retrieves whether a catalog name can be used in an index definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCatalogsInPrivilegeDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCatalogsInPrivilegeDefinitions()");
		row.add(val);
		row.add("Retrieves whether a catalog name can be used in a privilege definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCatalogsInProcedureCalls(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCatalogsInProcedureCalls()");
		row.add(val);
		row.add("Retrieves whether a catalog name can be used in a procedure call statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCatalogsInTableDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCatalogsInTableDefinitions()");
		row.add(val);
		row.add("Retrieves whether a catalog name can be used in a table definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsColumnAliasing(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsColumnAliasing()");
		row.add(val);
		row.add("Retrieves whether this database supports column aliasing.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsConvert(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsConvert()");
		row.add(val);
		row.add("Retrieves whether this database supports the JDBC scalar function CONVERT for the conversion of one JDBC type to another.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.supportsConvert(int fromType, int toType); } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("supportsConvert(int fromType, int toType)");
//		row.add(val);
//		row.add("Retrieves whether this database supports the JDBC scalar function CONVERT for conversions between the JDBC types fromType and toType.");
//		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCoreSQLGrammar(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCoreSQLGrammar()");
		row.add(val);
		row.add("Retrieves whether this database supports the ODBC Core SQL grammar.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsCorrelatedSubqueries(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsCorrelatedSubqueries()");
		row.add(val);
		row.add("Retrieves whether this database supports correlated subqueries.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsDataDefinitionAndDataManipulationTransactions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsDataDefinitionAndDataManipulationTransactions()");
		row.add(val);
		row.add("Retrieves whether this database supports both data definition and data manipulation statements within a transaction.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsDataManipulationTransactionsOnly(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsDataManipulationTransactionsOnly()");
		row.add(val);
		row.add("Retrieves whether this database supports only data manipulation statements within a transaction.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsDifferentTableCorrelationNames(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsDifferentTableCorrelationNames()");
		row.add(val);
		row.add("Retrieves whether, when table correlation names are supported, they are restricted to being different from the names of the tables.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsExpressionsInOrderBy(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsExpressionsInOrderBy()");
		row.add(val);
		row.add("Retrieves whether this database supports expressions in ORDER BY lists.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsExtendedSQLGrammar(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsExtendedSQLGrammar()");
		row.add(val);
		row.add("Retrieves whether this database supports the ODBC Extended SQL grammar.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsFullOuterJoins(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsFullOuterJoins()");
		row.add(val);
		row.add("Retrieves whether this database supports full nested outer joins.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsGetGeneratedKeys(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsGetGeneratedKeys()");
		row.add(val);
		row.add("Retrieves whether auto-generated keys can be retrieved after a statement has been executed");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsGroupBy(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsGroupBy()");
		row.add(val);
		row.add("Retrieves whether this database supports some form of GROUP BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsGroupByBeyondSelect(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsGroupByBeyondSelect()");
		row.add(val);
		row.add("Retrieves whether this database supports using columns not included in the SELECT statement in a GROUP BY clause provided that all of the columns in the SELECT statement are included in the GROUP BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsGroupByUnrelated(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsGroupByUnrelated()");
		row.add(val);
		row.add("Retrieves whether this database supports using a column that is not in the SELECT statement in a GROUP BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsIntegrityEnhancementFacility(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsIntegrityEnhancementFacility()");
		row.add(val);
		row.add("Retrieves whether this database supports the SQL Integrity Enhancement Facility.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsLikeEscapeClause(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsLikeEscapeClause()");
		row.add(val);
		row.add("Retrieves whether this database supports specifying a LIKE escape clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsLimitedOuterJoins(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsLimitedOuterJoins()");
		row.add(val);
		row.add("Retrieves whether this database provides limited support for outer joins.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMinimumSQLGrammar(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMinimumSQLGrammar()");
		row.add(val);
		row.add("Retrieves whether this database supports the ODBC Minimum SQL grammar.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMixedCaseIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMixedCaseIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result stores them in mixed case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMixedCaseQuotedIdentifiers(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMixedCaseQuotedIdentifiers()");
		row.add(val);
		row.add("Retrieves whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result stores them in mixed case.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMultipleOpenResults(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMultipleOpenResults()");
		row.add(val);
		row.add("Retrieves whether it is possible to have multiple ResultSet objects returned from a CallableStatement object simultaneously.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMultipleResultSets(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMultipleResultSets()");
		row.add(val);
		row.add("Retrieves whether this database supports getting multiple ResultSet objects from a single call to the method execute.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsMultipleTransactions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsMultipleTransactions()");
		row.add(val);
		row.add("Retrieves whether this database allows having multiple transactions open at once (on different connections).");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsNamedParameters(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsNamedParameters()");
		row.add(val);
		row.add("Retrieves whether this database supports named parameters to callable statements.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsNonNullableColumns(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsNonNullableColumns()");
		row.add(val);
		row.add("Retrieves whether columns in this database may be defined as non-nullable.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOpenCursorsAcrossCommit(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOpenCursorsAcrossCommit()");
		row.add(val);
		row.add("Retrieves whether this database supports keeping cursors open across commits.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOpenCursorsAcrossRollback(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOpenCursorsAcrossRollback()");
		row.add(val);
		row.add("Retrieves whether this database supports keeping cursors open across rollbacks.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOpenStatementsAcrossCommit(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOpenStatementsAcrossCommit()");
		row.add(val);
		row.add("Retrieves whether this database supports keeping statements open across commits.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOpenStatementsAcrossRollback(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOpenStatementsAcrossRollback()");
		row.add(val);
		row.add("Retrieves whether this database supports keeping statements open across rollbacks.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOrderByUnrelated(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOrderByUnrelated()");
		row.add(val);
		row.add("Retrieves whether this database supports using a column that is not in the SELECT statement in an ORDER BY clause.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsOuterJoins(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsOuterJoins()");
		row.add(val);
		row.add("Retrieves whether this database supports some form of outer join.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsPositionedDelete(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsPositionedDelete()");
		row.add(val);
		row.add("Retrieves whether this database supports positioned DELETE statements.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsPositionedUpdate(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsPositionedUpdate()");
		row.add(val);
		row.add("Retrieves whether this database supports positioned UPDATE statements.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);

		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);

		// ---
		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);

		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_SCROLL_INSENSITIVE, CONCUR_UPDATABLE)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);

		// ---
		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_SCROLL_SENSITIVE, CONCUR_READ_ONLY)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);

		try { val = md.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetConcurrency(TYPE_SCROLL_SENSITIVE, CONCUR_UPDATABLE)");
		row.add(val);
		row.add("Retrieves whether this database supports the given concurrency type in combination with the given result set type.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetHoldability(HOLD_CURSORS_OVER_COMMIT)");
		row.add(val);
		row.add("Retrieves whether this database supports the given result set holdability.");
		rows.add(row);

		try { val = md.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetHoldability(CLOSE_CURSORS_AT_COMMIT)");
		row.add(val);
		row.add("Retrieves whether this database supports the given result set holdability.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetType(type)");
		row.add(val);
		row.add("Retrieves whether this database supports the given result set type.");
		rows.add(row);
		
		try { val = md.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetType(type)");
		row.add(val);
		row.add("Retrieves whether this database supports the given result set type.");
		rows.add(row);
		
		try { val = md.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsResultSetType(type)");
		row.add(val);
		row.add("Retrieves whether this database supports the given result set type.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		try { val = md.supportsSavepoints(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSavepoints()");
		row.add(val);
		row.add("Retrieves whether this database supports savepoints.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSchemasInDataManipulation(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSchemasInDataManipulation()");
		row.add(val);
		row.add("Retrieves whether a schema name can be used in a data manipulation statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSchemasInIndexDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSchemasInIndexDefinitions()");
		row.add(val);
		row.add("Retrieves whether a schema name can be used in an index definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSchemasInPrivilegeDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSchemasInPrivilegeDefinitions()");
		row.add(val);
		row.add("Retrieves whether a schema name can be used in a privilege definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSchemasInProcedureCalls(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSchemasInProcedureCalls()");
		row.add(val);
		row.add("Retrieves whether a schema name can be used in a procedure call statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSchemasInTableDefinitions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSchemasInTableDefinitions()");
		row.add(val);
		row.add("Retrieves whether a schema name can be used in a table definition statement.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSelectForUpdate(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSelectForUpdate()");
		row.add(val);
		row.add("Retrieves whether this database supports SELECT FOR UPDATE statements.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsStatementPooling(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsStatementPooling()");
		row.add(val);
		row.add("Retrieves whether this database supports statement pooling.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsStoredFunctionsUsingCallSyntax(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsStoredFunctionsUsingCallSyntax()");
		row.add(val);
		row.add("Retrieves whether this database supports invoking user-defined or vendor functions using the stored procedure escape syntax.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsStoredProcedures(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsStoredProcedures()");
		row.add(val);
		row.add("Retrieves whether this database supports stored procedure calls that use the stored procedure escape syntax.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSubqueriesInComparisons(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSubqueriesInComparisons()");
		row.add(val);
		row.add("Retrieves whether this database supports subqueries in comparison expressions.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSubqueriesInExists(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSubqueriesInExists()");
		row.add(val);
		row.add("Retrieves whether this database supports subqueries in EXISTS expressions.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSubqueriesInIns(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSubqueriesInIns()");
		row.add(val);
		row.add("Retrieves whether this database supports subqueries in IN expressions.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsSubqueriesInQuantifieds(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsSubqueriesInQuantifieds()");
		row.add(val);
		row.add("Retrieves whether this database supports subqueries in quantified expressions.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsTableCorrelationNames(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTableCorrelationNames()");
		row.add(val);
		row.add("Retrieves whether this database supports table correlation names.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.supportsTransactionIsolationLevel(Connection.TRANSACTION_NONE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactionIsolationLevel(TRANSACTION_NONE)");
		row.add(val);
		row.add("Retrieves whether this database supports the given transaction isolation level.");
		rows.add(row);
		
		try { val = md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactionIsolationLevel(TRANSACTION_READ_COMMITTED)");
		row.add(val);
		row.add("Retrieves whether this database supports the given transaction isolation level.");
		rows.add(row);
		
		try { val = md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactionIsolationLevel(TRANSACTION_READ_UNCOMMITTED)");
		row.add(val);
		row.add("Retrieves whether this database supports the given transaction isolation level.");
		rows.add(row);
		
		try { val = md.supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactionIsolationLevel(TRANSACTION_REPEATABLE_READ)");
		row.add(val);
		row.add("Retrieves whether this database supports the given transaction isolation level.");
		rows.add(row);
		
		try { val = md.supportsTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactionIsolationLevel(TRANSACTION_SERIALIZABLE)");
		row.add(val);
		row.add("Retrieves whether this database supports the given transaction isolation level.");
		rows.add(row);
		// --- end
	
		
		//-------------------------------------------------------------------------
		try { val = md.supportsTransactions(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsTransactions()");
		row.add(val);
		row.add("Retrieves whether this database supports transactions.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsUnion(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsUnion()");
		row.add(val);
		row.add("Retrieves whether this database supports SQL UNION.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.supportsUnionAll(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("supportsUnionAll()");
		row.add(val);
		row.add("Retrieves whether this database supports SQL UNION ALL.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		//-------------------------------------------------------------------------
		try { val = md.updatesAreDetected(ResultSet.TYPE_FORWARD_ONLY); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("updatesAreDetected(TYPE_FORWARD_ONLY)");
		row.add(val);
		row.add("Retrieves whether or not a visible row update can be detected by calling the method ResultSet.rowUpdated.");
		rows.add(row);
		
		try { val = md.updatesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("updatesAreDetected(TYPE_SCROLL_INSENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row update can be detected by calling the method ResultSet.rowUpdated.");
		rows.add(row);
		
		try { val = md.updatesAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("updatesAreDetected(TYPE_SCROLL_SENSITIVE)");
		row.add(val);
		row.add("Retrieves whether or not a visible row update can be detected by calling the method ResultSet.rowUpdated.");
		rows.add(row);
		// --- end

		
		//-------------------------------------------------------------------------
		try { val = md.usesLocalFilePerTable(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("usesLocalFilePerTable()");
		row.add(val);
		row.add("Retrieves whether this database uses a file for each table.");
		rows.add(row);
		
		//-------------------------------------------------------------------------
		try { val = md.usesLocalFiles(); } catch(Throwable e) { val = e.getMessage(); }
		row = new Vector<Object>();
		row.add("usesLocalFiles()");
		row.add(val);
		row.add("Retrieves whether this database stores tables in a local file.");
		rows.add(row);
		
//		//-------------------------------------------------------------------------
//		try { val = md.xxx; } catch(Throwable e) { val = e.getMessage(); }
//		row = new Vector<Object>();
//		row.add("xxx");
//		row.add(val);
//		row.add("xxx");
//		rows.add(row);
		
		return new DefaultTableModel(rows, cols);
	}
	
}
