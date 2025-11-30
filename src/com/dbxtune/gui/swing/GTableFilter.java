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
package com.dbxtune.gui.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.sort.RowFilters;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AllValue;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.ArrayConstructor;
import net.sf.jsqlparser.expression.ArrayExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.CollateExpression;
import net.sf.jsqlparser.expression.ConnectByRootOperator;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonAggregateFunction;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.JsonFunction;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.OracleNamedFunctionParameter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.RowGetExpression;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.TimezoneExpression;
import net.sf.jsqlparser.expression.TryCastExpression;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.ValueListExpression;
import net.sf.jsqlparser.expression.VariableAssignment;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.XMLSerializeExpr;
//import net.sf.jsqlparser.expression.WithinGroupExpression;   // removed when we upgraded from 1.1 to 3.2
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseLeftShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.IntegerDivision;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.FullTextSearch;
import net.sf.jsqlparser.expression.operators.relational.GeometryDistance;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsDistinctExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NamedExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.expression.operators.relational.SimilarToExpression;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserTokenManager;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SubSelect;

public class GTableFilter 
extends JPanel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	public static final int ROW_COUNT_LAYOUT_RIGHT = 1;
	public static final int ROW_COUNT_LAYOUT_LEFT  = 2;

	private JXTable      _table        = null;
	private Set<JXTable> _linkedTables = new LinkedHashSet<>();

	private GLabel     _filter_lbl = new GLabel("Filter: ");
//	private JTextField _filter_txt = new JTextField();
//	private GTextFieldWithCompletion _filter_txt = new GTextFieldWithCompletion();
	private GTextField _filter_txt = new GTextField();
	private GLabel     _filter_cnt = new GLabel();
	
	private JCheckBox  _filter_chk = new JCheckBox("", true);
	private boolean    _filter_chk_visible = false;
	
	private static final String FILTER_TOOLTIP_SHORT = "<html>"
			+ "Filter what to view in the table. You can use RegExp or SQL WHERE behaviour.<br>"
			+ "<b>Tip</b>: Hover/tooltip over 'Filter:' label to see details.<br>"
			+ "</html>";

	private static final String FILTER_TOOLTIP = "<html>"
			+ "Enter a search criteria that will search <b>all</b> columns in the table<br>"
			+ "<b>Tip</b>: regular expresion can be used.<br>"
			+ "<br>"
			+ "<hr>"
			+ "<i>The below 'where' functionality, is still in develeopment, and might be buggy, but try it out...</i><br>"
			+ "<hr>"
			+ "If first word in the field starts with <code>WHERE</code> then I will apply a Simple/Limited SQL where clause to filter the table.<br>"
			+ "<b>Note</b>: Not all operators are supported, but quite a bunch...<br>"
			+ "If the serach field turns 'red', then <i>hower</i> over it, and tooltip will show the possibel problem.<br>"
			+ "<b>Tip</b>: Use 'ctrl-space' to get code completion for available columns in the Table.<br>"
			+ "<b>Tip</b>: To use regexp equals do: where name ~ '^sys.*o[a-c]' -- This is not SQL Standard but used by: Postgres and H2<br>"
			+ "<i>Regexp Operators</i>: ~ is Case Sensitive, ~* is Case InSensitive, !~ is Not equal regexp, !~* is Not equal regexp<br>"
			+ "<i>Like</i>: Is currently also using java 'regexp', the char '%' is changed into '.*', and '_' into '.'<br>"
			+ "</html>";
	private static final Color  ERROR_COLOR    = Color.RED;
	
	private int        _rowCntLayout = ROW_COUNT_LAYOUT_RIGHT;
	
	private int        _deferredFilterThreshold = 10000;
	private int        _deferredFilterSleepTime = 250;

	private Timer      _deferredFilterTimer;

	private RowFilter<? super TableModel, ? super Integer> _externalFilter;

    public GTableFilter(JXTable table)
	{
		this(table, ROW_COUNT_LAYOUT_RIGHT, false);
	}
	public GTableFilter(JXTable table, int rowCountLayout)
	{
		this(table, rowCountLayout, false);
	}
	public GTableFilter(JXTable table, int rowCountLayout, boolean showCheckbox)
	{
//		_table = table; // This is set in: setTable();
		_rowCntLayout = rowCountLayout;
		_filter_chk_visible = showCheckbox;

		setTable(table);

		initComponents();
	}
	
	public GTableFilter()
	{
		this(null);
	}

	public void setTable(JXTable table)
	{
		if (_table != null)
		{
			_logger.warn("Table is already assigned. Some listeners will also be triggered from the old tables. (I havn't YET-IMPLEMENTED a proper cleanup strategy here.");
			//new Exception("DUMMY EXCEPTION to get where it was called from").printStackTrace();

//			TableModel tm = _table.getModel();
//			if (tm != null && tm instanceof AbstractTableModel)
//			{
//				AbstractTableModel atm = (AbstractTableModel) tm;
//				TableModelListener[] tmla = atm.getTableModelListeners();
//				if (ArrayUtils.contains(tmla, this))
//					/*do*/;
//			}
			return;
		}
		
		_table = table;
		if (_table != null)
		{
			// FIXME: unregister below listeners in the current table.
		}
		else
		{
			return;
		}
		
		// Create and Add table model listener, so when the table content is changed we can update the rowcount field
		final TableModelListener tml = new TableModelListener()
		{
			@Override
			public void tableChanged(TableModelEvent e)
			{
				// event: AbstactTableModel.fireTableStructureChanged
				if ( SwingUtils.isStructureChanged(e) )
				{
					refreshCompletion();
					applyFilter();
				}

				updateRowCount();
			}
		};
		_table.getModel().addTableModelListener(tml);

		// on JTable.setModel() a PropertyChangeListener is fired with property "model"
		_table.addPropertyChangeListener("model", new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				Object oldTm = evt.getOldValue();
				Object newTm = evt.getNewValue();

				// Remove This listener from the old TableModel
				if (oldTm instanceof AbstractTableModel)
					((AbstractTableModel)oldTm).removeTableModelListener(tml);
				
				// ADD This listener to the NEW TableModel
				if (newTm instanceof AbstractTableModel)
					((AbstractTableModel)newTm).addTableModelListener(tml);
			}
		});
	}

	public int getDeferredFilterSleepTime() { return _deferredFilterSleepTime; }
	public int getDeferredFilterThreshold() { return _deferredFilterThreshold; }
	
	public void setDeferredFilterSleepTime(int timeInMs) { _deferredFilterSleepTime = timeInMs; }
	public void setDeferredFilterThreshold(int rows)     { _deferredFilterThreshold = rows; }

	public void   setText(String str) { _filter_txt.setText(str); }
	public String getText()           { return _filter_txt.getText(); }

	public boolean isFilterChkboxSelected()             { return _filter_chk.isSelected(); }
	public void    setFilterChkboxSelected(boolean val) { _filter_chk.setSelected(val); _filter_txt.setEnabled(val); }
	
//	public boolean isFilterLabelVisible()           { return _filter_lbl.isVisible(); }
//	public void    setFilterLabelVisible(boolean b) { _filter_lbl.setVisible(b); }
	public JLabel  getFilterLabel()                 { return _filter_lbl; }
	
	public void	setExternalFilter(RowFilter<? super TableModel, ? super Integer> filter) 
	{ 
		_externalFilter = filter;
		applyFilter();
	}
	public RowFilter<? super TableModel, ? super Integer> getExternalFilter()
	{ 
		return _externalFilter; 
	}

	/**
	 * Add a component that if its "changes", it will call <code>applyFilter()</code>
	 * @param comp
	 */
	public void addFilterTriggerComponent(AbstractButton comp)
	{
		comp.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyFilter();
			}
		});
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setLayout(new MigLayout("insets 0 0 0 0"));   // insets Top Left Bottom Right

		_filter_lbl.setUseFocusableTips(true);

		// Add Code Completion to the text field
		if (_table != null)
			_filter_txt.addCompletion(_table);
		_filter_txt.setUseFocusableTips(false);

		_filter_chk.setVisible(_filter_chk_visible);
		_filter_chk.setToolTipText("<html>Quick way to enable/disable this filter without removing/deleting the text content</html>");
			
		if (_rowCntLayout == ROW_COUNT_LAYOUT_RIGHT)
		{
			add(_filter_chk, "hidemode 3");
			add(_filter_lbl, "hidemode 3");
			add(_filter_txt, "growx, pushx");
			add(_filter_cnt, "wrap");
		}
		else // ROW_COUNT_LAYOUT_LEFT
		{
			add(_filter_chk, "hidemode 3");
			add(_filter_lbl, "");
			add(_filter_cnt, "");
			add(_filter_txt, "growx, pushx, wrap");
		}

		// Setup timer...
		_deferredFilterTimer = new Timer(_deferredFilterSleepTime, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyFilter();
				_deferredFilterTimer.stop();
			}
		});
		
		_filter_txt.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void removeUpdate(DocumentEvent paramDocumentEvent)  { textWasUpdated(); }
			@Override public void insertUpdate(DocumentEvent paramDocumentEvent)  { textWasUpdated(); }
			@Override public void changedUpdate(DocumentEvent paramDocumentEvent) { textWasUpdated(); }
			public void textWasUpdated()
			{
				if (_table != null && _table.getModel().getRowCount() < _deferredFilterThreshold)
				{
					applyFilter();
				}
				else
				{
					if (_deferredFilterTimer.isRunning())
						_deferredFilterTimer.restart();
					else
						_deferredFilterTimer.start();
				}
			}
		});
		
		_filter_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyFilter();
			}
		});

		// Set some ToolTip
		_filter_lbl.setToolTipText(FILTER_TOOLTIP);
		_filter_txt.setToolTipText(FILTER_TOOLTIP_SHORT);
		_filter_cnt.setToolTipText("How many rows does the table contain. (visibleRows/actualRows");
		
		// Set row count label to table size
		resetFilter();
	}
	
	public void refreshCompletion()
	{
		_filter_txt.refreshCompletion(_table);
	}

	/**
	 * Simply update the rowcount label
	 */
	public void updateRowCount()
	{
		NumberFormat nf = NumberFormat.getInstance();
		String rowc = nf.format(_table.getRowCount()) + "/" + nf.format(_table.getModel().getRowCount());
		_filter_cnt.setText(rowc);
	}

	/** 
	 * Add Any additional tables to the same filter
	 * @param source_tab
	 */
	public void addTable(JXTable table)
	{
		_linkedTables.add(table);
	}

	private void setTableRowFilter(RowFilter<? super TableModel, ? super Integer> filter)
//	private void setTableRowFilter(RowFilter<TableModel, Integer> filter)
	{
		if (filter == null && _externalFilter == null)
		{
			_table.setRowFilter(null);
			for (JXTable linkedTable : _linkedTables)
				linkedTable.setRowFilter(null);
		}
		else if (filter != null && _externalFilter == null)
		{
			_table.setRowFilter(filter);
			for (JXTable linkedTable : _linkedTables)
				linkedTable.setRowFilter(filter);
		}
		else if (filter == null && _externalFilter != null)
		{
			_table.setRowFilter(_externalFilter);
			for (JXTable linkedTable : _linkedTables)
				linkedTable.setRowFilter(_externalFilter);
		}
		else
		{
			AndFilter<TableModel, Integer> andFilter = new AndFilter<>();
			andFilter.addFilter(filter);
			andFilter.addFilter(_externalFilter);

			_table.setRowFilter(andFilter);
			for (JXTable linkedTable : _linkedTables)
				linkedTable.setRowFilter(_externalFilter);

//			List<RowFilter<? super TableModel, ? super Integer>> andList = new ArrayList<>();
//			andList.add(filter);
//			andList.add(_externalFilter);
//			
//			_table.setRowFilter(andList);
		}
	}
	
	public void applyFilter()
	{
		if (_table == null)
			return;

		// The text field should be enabled/disabled based on the Checkbox
		_filter_txt.setEnabled(_filter_chk.isSelected());

		// IF the filter checkbox is NOT set, simply reset the filter and return
		if ( _filter_chk_visible && ! _filter_chk.isSelected() )
		{
//			_table.setRowFilter(null);
			setTableRowFilter(null);
			return;
		}
		
		try
		{
			String searchStringNoTrim = _filter_txt.getText();
			String searchString       = _filter_txt.getText().trim();
			if ( searchString.length() <= 0 )
			{
//				_table.setRowFilter(null);
				setTableRowFilter(null);
			}
			else
			{
				if (searchStringNoTrim.toUpperCase().startsWith("WHERE "))
				{
					// Parse SQL Like
//					SimpleSqlWhereTableFilter.setFilterForWhere(_table, searchStringNoTrim.substring("where ".length()).trim());
					ALocalFilter<TableModel, Integer> filter = SimpleSqlWhereTableFilter.getFilterForWhere(_table, searchStringNoTrim.substring("where ".length()).trim());
					setTableRowFilter(filter);
				}
				else
				{
    				// Create a array with all visible columns... hence: it's only those we want to search
    				// Note the indices are MODEL column index
//    				int[] mcols = new int[_table.getColumnCount()];
//    				for (int i=0; i<mcols.length; i++)
//    					mcols[i] = _table.convertColumnIndexToModel(i);
    				int[] mcols = new int[_table.getModel().getColumnCount()];
    				for (int i=0; i<mcols.length; i++)
    					mcols[i] = i;
    
//    				_table.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString, mcols));
    				setTableRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString, mcols));
				}
			}
			_filter_txt.setToolTipText(FILTER_TOOLTIP_SHORT);
			_filter_txt.setBackground( UIManager.getColor("TextField.background") );
		}
		catch (Throwable t)
		{
			// Well if we "restore" a saved filter and parse that, AND we got 0 columns the parser will complain about missing column names...  
			if (_table.getColumnCount() == 0)
				return;

			if (_logger.isDebugEnabled())
				_logger.debug("Stacktrace for WHERE parsing", t);

			if (t instanceof JSQLParserException)
				t = ExceptionUtils.getRootCause(t);

			_filter_txt.setToolTipText("<html><pre><code>" + StringUtil.toHtmlString(t) + "</code></pre></html>");
			_filter_txt.setBackground(ERROR_COLOR);
		}
		updateRowCount();
	}

	public void resetFilter()
	{
		_filter_txt.setText("");
		if (_table != null)
		{
			setTableRowFilter(null);

			updateRowCount();
		}
	}

	/**
	 * Set the filter text and apply the filter
	 * @param string
	 */
	public void setFilterText(String text)
	{
		_filter_txt.setText(text);
		applyFilter();
	}

	/**
	 * Used to get row count and filter text
	 * @return
	 */
	public String getFilterInfo()
	{
		return "Filter information: visibleRows=" + _table.getRowCount() + ", actualRows=" + _table.getModel().getRowCount() + ", filterText='" + _filter_txt.getText() + "'.";
	}

	/**
	 * Check if the filer box is empty
	 * @return
	 */
	public boolean hasFilterInfo()
	{
		return StringUtil.hasValue(_filter_txt.getText());
	}

	/**
	 * This is where the WHERE clause is parsed...
	 */
	private static class SimpleSqlWhereTableFilter
	implements ExpressionVisitor
	{
//		public static void setFilterForWhere(JXTable table, String whereClause)
//		throws Exception
//		{
//			if (table == null || whereClause == null)
//				return;
//
//			// If the string starts with "where" remove that before parsing
//			if (whereClause.toUpperCase().startsWith("WHERE "))
//				whereClause = whereClause.substring("where ".length()).trim();
//
//			// If there is no string, set to NO filter
//			if (whereClause.trim().length() == 0)
//			{
//				table.setRowFilter(null);
//				return;
//			}
//
//			// Now parse the "where" string and set filter...
//			Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause);
//			
//			if (_logger.isDebugEnabled())
//			{
//				_logger.debug("-------------------------------------------------------------");
//				_logger.debug("FULL EXPR: "+expr);
//			}
//
//			@SuppressWarnings("unused")
//			SimpleSqlWhereTableFilter swpv = new SimpleSqlWhereTableFilter(table, expr);
//		}

		public static ALocalFilter<TableModel, Integer> getFilterForWhere(JXTable table, String whereClause)
		throws Exception
		{
			if (table == null || whereClause == null)
				return null;

			// If the string starts with "where" remove that before parsing
			if (whereClause.toUpperCase().startsWith("WHERE "))
				whereClause = whereClause.substring("where ".length()).trim();

			// If there is no string, set to NO filter
			if (whereClause.trim().length() == 0)
			{
				//table.setRowFilter(null);
				return null;
			}

			// Now parse the "where" string and set filter...
			Expression expr = parseWhereClause(whereClause); // <<-- in this method do: withSquareBracketQuotation(true)
//			Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause);
//			Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause, parser -> parser.withSquareBracketQuotation(true));

			if (_logger.isDebugEnabled())
			{
				_logger.debug("-------------------------------------------------------------");
				_logger.debug("FULL EXPR: "+expr);
			}

			// Create a filter
			SimpleSqlWhereTableFilter swpv = new SimpleSqlWhereTableFilter(table, expr);
			return swpv.getFilter();
		}

		/** 
		 * Workaround to get square brackets [] to work... Code "ripped" from CCJSqlParserUtil.parseCondExpression <br>
		 * And adding: parser.withSquareBracketQuotation(true);
		 */
		private static Expression parseWhereClause(String whereClause) 
		throws JSQLParserException
		{
			boolean allowPartialParse = true;
//			return CCJSqlParserUtil.parseCondExpression(whereClause);
			CCJSqlParser parser = new CCJSqlParser(new StringProvider(whereClause));
			parser.withSquareBracketQuotation(true);
			try 
			{
				Expression expr = parser.Expression();
				if (!allowPartialParse && parser.getNextToken().kind != CCJSqlParserTokenManager.EOF) 
				{
					throw new JSQLParserException("could only parse partial expression " + expr.toString());
				}
				return expr;
			} 
			catch (JSQLParserException ex) 
			{
				throw ex;
			} 
			catch (ParseException ex) 
			{
				throw new JSQLParserException(ex);
			}
		}


//		private JXTable _table = null; 
		private AbstractTableModel _tm = null; 

//		private String _lastColName  = null; 
		private int    _lastColIndex = -1;
		private String _lastStrValue = null; 
//		private Number _lastNumValue = null; 
		

		// Push/pop every time we see a AND/Or, then add new XX Filters to the last entry
		private Stack<ALocalFilter<TableModel, Integer>> _andOrStack = new Stack<ALocalFilter<TableModel, Integer>>();

		public SimpleSqlWhereTableFilter(JXTable table, Expression expr)
		{
//			_table = table;
			if ( ! (table.getModel() instanceof AbstractTableModel) )
				throw new RuntimeException("TableModel must implement AbstractTableModel.");
			_tm = (AbstractTableModel) table.getModel();

			_andOrStack.push(new AndFilter<TableModel, Integer>()); // Always start with a AND filter... 
			expr.accept(this);

			if (_logger.isDebugEnabled())
				printRowFilterStack(_andOrStack.peek(), 0);

			// Set filter on the table
			//table.setRowFilter(_andOrStack.peek());
		}

		public ALocalFilter<TableModel, Integer> getFilter()
		{
			return _andOrStack.peek();
		}

		@SuppressWarnings({ "unchecked" })
		public void printRowFilterStack(ALocalFilter<TableModel, Integer> startFilter, int level)
		{
			for (RowFilter<? super TableModel, ? super Integer> filter : startFilter.getFilterList())
			{
				_logger.debug(StringUtil.replicate("    ", level) + filter);
				if (filter instanceof AndFilter || filter instanceof OrFilter)
					printRowFilterStack((ALocalFilter<TableModel, Integer>) filter, ++level);
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <R extends TableModel> void addRowFilter(RowFilter<? super R, ? super Integer> filter)
		{
			ALocalFilter lastLevel = _andOrStack.peek();
			lastLevel.addFilter(filter);
		}

		private void startAnd()
		{
			if (_logger.isDebugEnabled()) dp("> startAndOr(): _filterStack.size()="+_andOrStack.size());
			_andOrStack.push(new AndFilter<TableModel, Integer>());
		}
		private void startOr()
		{
			if (_logger.isDebugEnabled()) dp("> startAndOr(): _filterStack.size()="+_andOrStack.size());
			_andOrStack.push(new OrFilter<TableModel, Integer>());
		}
		private void closeAnd()
		{
			closeAnd(false);
		}
		private void closeAnd(boolean isNot)
		{
			ALocalFilter<TableModel, Integer> thisLevel = _andOrStack.pop();
			ALocalFilter<TableModel, Integer> lastLevel = _andOrStack.peek();

			if ( isNot )
				lastLevel.addFilter( RowFilter.notFilter(thisLevel) );
			else
				lastLevel.addFilter(thisLevel);
			
			if (_logger.isDebugEnabled()) dp("< closeAndOr(): _andOrStack.size()="+_andOrStack.size() +", lastLevel.size()="+lastLevel.size());
		}
		private void closeOr()
		{
			closeAnd();
		}

		private int _indentLevel=0;
		private String _ps="";
		private void indent(boolean indent)
		{
			if (indent)
			{
				_indentLevel++;
				_ps = StringUtil.replicate("    ", _indentLevel);
			}
			else
			{
				_indentLevel--;
				_ps = StringUtil.replicate("    ", _indentLevel);
			}
		}
		private void in(String str)
		{
			indent(true);
			dp(str);
		}
		private void out(String str)
		{
			dp(str);
			indent(false);
		}
		private void dp(String str)
		{
			_logger.debug(_ps+str);
		}
		//-------------------------------------------------------
		// Parenthesis
		//-------------------------------------------------------
		@Override public void visit(Parenthesis expr)
		{
			if (_logger.isDebugEnabled()) in("Parenthesis-visitor(start): "+expr+", expr.getExpression()="+expr.getExpression());
			expr.getExpression().accept(this);
			if (_logger.isDebugEnabled()) out("Parenthesis-visitor(end): "+expr+", expr.getExpression()="+expr.getExpression());
		}

		//-------------------------------------------------------
		// OR
		//-------------------------------------------------------
		@Override public void visit(OrExpression expr)
		{
			startOr();
			if (_logger.isDebugEnabled()) in("OR-visitor(start): "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("OR-visitor(end): "+expr);
			closeOr();
		}

		//-------------------------------------------------------
		// AND
		//-------------------------------------------------------
		@Override public void visit(AndExpression expr)
		{ 
			startAnd();
			if (_logger.isDebugEnabled()) in("AND-visitor(start): "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("AND-visitor(end): "+expr);
			closeAnd();
		}
		
		//-------------------------------------------------------
		// = EQUAL
		//-------------------------------------------------------
		@Override public void visit(EqualsTo expr)
		{
			if (_logger.isDebugEnabled()) in("---> EqualsTo-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- EqualsTo-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_EQ, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// != NOT EQUAL
		//-------------------------------------------------------
		@Override public void visit(NotEqualsTo expr)
		{
			if (_logger.isDebugEnabled()) in("---> NotEqualsTo-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- NotEqualsTo-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_NE, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// > GREATER THAN
		//-------------------------------------------------------
		@Override public void visit(GreaterThan expr)
		{
			if (_logger.isDebugEnabled()) in("---> GreaterThan-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- GreaterThan-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_GT, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// >= GREATER or EQUAL THAN
		//-------------------------------------------------------
		@Override public void visit(GreaterThanEquals expr)
		{
			if (_logger.isDebugEnabled()) in("---> GreaterThanEquals-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- GreaterThanEquals-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_GT_OR_EQ, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// < LESS THAN
		//-------------------------------------------------------
		@Override public void visit(MinorThan expr)
		{
			if (_logger.isDebugEnabled()) in("---> MinorThan-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- MinorThan-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_LT, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// <= LESS or EQUAL THAN
		//-------------------------------------------------------
		@Override public void visit(MinorThanEquals expr)
		{
			if (_logger.isDebugEnabled()) in("---> MinorThanEquals-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- MinorThanEquals-visitor: "+expr);
			
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_LT_OR_EQ, _lastColIndex, _lastStrValue));
		}

		//-------------------------------------------------------
		// REQEXP
		// ~   = case sensitive
		// ~*  = case insensitive
		// !~  = not 
		// !~* = not 
		//-------------------------------------------------------
		@Override public void visit(RegExpMatchOperator expr)
		{
			if (_logger.isDebugEnabled()) in("---> RegExpMatchOperator-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);

			if (_logger.isDebugEnabled()) out("<--- RegExpMatchOperator-visitor: "+expr);

			switch (expr.getOperatorType())
			{
			case MATCH_CASESENSITIVE:
				addRowFilter(RowFilters.regexFilter(_lastStrValue, _lastColIndex));
				break;

			case MATCH_CASEINSENSITIVE:
				addRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, _lastStrValue, _lastColIndex));
				break;

			case NOT_MATCH_CASESENSITIVE:
				addRowFilter(RowFilter.notFilter(RowFilters.regexFilter(_lastStrValue, _lastColIndex)));
				break;

			case NOT_MATCH_CASEINSENSITIVE:
				addRowFilter(RowFilter.notFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, _lastStrValue, _lastColIndex)));
				break;

			default:
				throw new FilterParserException("Operation 'RegExpMatchOperator' type '"+expr.getOperatorType()+"' not yet implemeted.");
			}
//			addRowFilter(RowFilter.regexFilter(_lastStrValue, _lastColIndex));
//			addRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, _lastStrValue));
		}

		//-------------------------------------------------------
		// LIKE
		//-------------------------------------------------------
		@Override public void visit(LikeExpression expr)
		{
			if (_logger.isDebugEnabled()) in("---> LikeExpression-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			expr.getRightExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- LikeExpression-visitor: "+expr);

			String likeStr = _lastStrValue.replace("_", ".").replace("%", ".*");
			likeStr = "^" + likeStr + "$";
			if (expr.isNot())
				addRowFilter(RowFilter.notFilter(RowFilter.regexFilter(likeStr, _lastColIndex)));
			else
				addRowFilter(RowFilter.regexFilter(likeStr, _lastColIndex));
//			addRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, likeStr));
		}

		//-------------------------------------------------------
		// IN
		//-------------------------------------------------------
		@Override public void visit(InExpression expr)
		{
			// FIXME: This was really ugly... so revisit this and redo
//			throw new FilterParserException("Operation 'InExpression' not yet implemeted."); 

			// Add a temporary visitor for the items in the IN list
			final ArrayList<String> inListValues = new ArrayList<String>();
			ItemsListVisitor ilv = new ItemsListVisitor()
			{
				
				@Override
				public void visit(MultiExpressionList expr)
				{
					throw new FilterParserException("Operation 'InExpression:MultiExpressionList' not yet implemeted.");
				}
				
				@Override
				public void visit(ExpressionList expr)
				{
					if (_logger.isDebugEnabled()) in("---> InExpression:ExpressionList-visitor: "+expr);
					if (_logger.isDebugEnabled()) dp("     getExpressions(): "+expr.getExpressions());
					for (Expression exp : expr.getExpressions())
					{
						dp("   exp.toString(): "+exp);
						String val = exp.toString();
						if (val.startsWith("'") && val.endsWith("'"))
							val = val.substring(1, val.length()-1);

						inListValues.add(val);
					}
					if (_logger.isDebugEnabled()) out("---> InExpression:ExpressionList-visitor: "+expr);
				}
				
				@Override
				public void visit(SubSelect expr)
				{
					throw new FilterParserException("Operation 'InExpression:SubSelect' not yet implemeted.");
				}

				@Override
				public void visit(NamedExpressionList arg0)
				{
					throw new FilterParserException("Operation 'InExpression:NamedExpressionList' not yet implemeted.");
				}
			};
			
			
			if (_logger.isDebugEnabled()) in("---> InExpression-visitor: "+expr);
			if ( expr.getLeftExpression() != null )
			{
				expr.getLeftExpression().accept(this);
			}
//			else if ( expr.getLeftItemsList() != null ) // REMOVED IN JSqlParser version ???? CHECK if this any side effect... DO: expr.getLeftExpression().accept(this); handle this removed method???
//			{
//				expr.getLeftItemsList().accept(ilv);
//			}
			expr.getRightItemsList().accept(ilv);
			if (_logger.isDebugEnabled()) in("<--- InExpression-visitor: "+expr);
			
			if (expr.isNot())
			{
				startAnd();
				if (_logger.isDebugEnabled()) in("---> Simulate-AND-InExpression-visitor(start): "+expr);
				for (String str : inListValues)
				{
					addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_NE, _lastColIndex, str));
					if (_logger.isDebugEnabled()) dp("     add or value: FILTER_OP_NE, colId="+_lastColIndex+", str=|"+str+"|.");
				}
				if (_logger.isDebugEnabled()) out("<--- Simulate-AND-InExpression-visitor(out): "+expr);
				closeAnd();
			}
			else
			{
				startOr();
				if (_logger.isDebugEnabled()) in("---> Simulate-OR-InExpression-visitor(start): "+expr);
				for (String str : inListValues)
				{
					addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_EQ, _lastColIndex, str));
					if (_logger.isDebugEnabled()) dp("     add or value: FILTER_OP_EQ, colId="+_lastColIndex+", str=|"+str+"|.");
				}
				if (_logger.isDebugEnabled()) out("<--- Simulate-OR-InExpression-visitor(out): "+expr);
				closeOr();
			}
		}

		//-------------------------------------------------------
		// IS NULL
		//-------------------------------------------------------
		@Override public void visit(IsNullExpression expr)
		{
//			throw new FilterParserException("Operation 'IsNullExpression' not yet implemeted."); 
			if (_logger.isDebugEnabled()) in("---> IsNullExpression-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- IsNullExpression-visitor: "+expr);

			if (expr.isNot())
				addRowFilter(RowFilter.notFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_IS_NULL, _lastColIndex, null)));
			else
				addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_IS_NULL, _lastColIndex, null));
		}
		
		//-------------------------------------------------------
		// BETWEEN
		//-------------------------------------------------------
		@Override public void visit(Between expr)
		{
			if (_logger.isDebugEnabled()) in("---> Between-visitor: "+expr);
			
			startAnd();

			expr.getLeftExpression().accept(this);
			expr.getBetweenExpressionStart().accept(this);
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_GT_OR_EQ, _lastColIndex, _lastStrValue));

			expr.getLeftExpression().accept(this);
			expr.getBetweenExpressionEnd().accept(this);
			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_LT_OR_EQ, _lastColIndex, _lastStrValue));

			closeAnd( expr.isNot() );

			if (_logger.isDebugEnabled()) out("<--- Between-visitor: "+expr);
			
//			throw new FilterParserException("Operation 'Between' not yet implemeted.");			
		}

		//-------------------------------------------------------
		// column
		//-------------------------------------------------------
		@Override public void visit(Column expr)
		{
			if (_logger.isDebugEnabled()) dp("Column-visitor: "+expr);
			if (_logger.isDebugEnabled()) dp("   getFullyQualifiedName: "+expr.getFullyQualifiedName());
			
			// Get around the fact the JSsqlParser sees a quoted string as a quoted-identifier. 
			// Meaning: name="value" will see |"value"| as a column name... so if it starts/end with quote, just strip it of and put it as a string value.
			String key = expr.getColumnName();
			if (key.startsWith("\"") && key.endsWith("\""))
			{
				_lastStrValue = key.substring(1, key.length()-1);
			}
			else
			{
				// check for SQL-Server like quoted identifier
				if (key.startsWith("[") && key.endsWith("]"))
				{
					key = key.substring(1, key.length()-1);
				}

				int colIndex = _tm.findColumn(key);
				if (colIndex < 0)
					throw new FilterParserException("Column '"+key+"', can't be found in the table.");
				_lastColIndex = colIndex;
//				_lastColName  = key;
			}
		}

		//-------------------------------------------------------
		// value: String
		//-------------------------------------------------------
		@Override public void visit(StringValue expr)
		{
			if (_logger.isDebugEnabled()) dp("StringValue-visitor: "+expr);
			if (_logger.isDebugEnabled()) dp("   getValue: "+expr.getValue());
			_lastStrValue = expr.getValue();
//			_lastNumValue = null;
			
			// The parser do not seems to remove escaped single quotes: so do replace '' -> '
			if (_lastStrValue.indexOf("''") >= 0)
				_lastStrValue = _lastStrValue.replace("''", "'");
		}

		//-------------------------------------------------------
		// value: Long (Every number without a point or an exponential format is a LongValue)
		//-------------------------------------------------------
		@Override public void visit(LongValue expr)
		{
			if (_logger.isDebugEnabled()) dp("LongValue-visitor: "+expr);
			if (_logger.isDebugEnabled()) dp("   getValue: "+expr.getValue());
			_lastStrValue = null;
			_lastStrValue = expr.getStringValue();
//			_lastNumValue = expr.getValue();
		}

		//-------------------------------------------------------
		// value: Double (Every number with a point or a exponential format is a DoubleValue)
		//-------------------------------------------------------
		@Override public void visit(DoubleValue expr)
		{
			if (_logger.isDebugEnabled()) dp("DoubleValue-visitor: "+expr);
			if (_logger.isDebugEnabled()) dp("   getValue: "+expr.getValue());
			_lastStrValue = null;
			_lastStrValue = expr.getValue()+"";
//			_lastNumValue = expr.getValue();
		}

		//-------------------------------------------------------
		// value: when a '+' or '-' is put in front of a statement
		//-------------------------------------------------------
		@Override public void visit(SignedExpression expr)
		{
			if (_logger.isDebugEnabled()) dp("SignedExpression-visitor: "+expr);
			if (_logger.isDebugEnabled()) dp("   getSign:       "+expr.getSign());
			if (_logger.isDebugEnabled()) dp("   getExpression: "+expr.getExpression());
			
			// NOTE: This is a clumsy way to solve this... but I don't understand how to fix it in a better way...
			if ('+' == expr.getSign())
			{
				_lastStrValue = null;
				_lastStrValue = expr.getExpression() + "";
				
			}
			else if ('-' == expr.getSign())
			{
				_lastStrValue = null;
				_lastStrValue = expr + "";
			}
			else
			{
				throw new FilterParserException("Operation 'SignedExpression': unsupported value type " + expr.getClass() + " with sign " + expr.getSign());				
			}
		}

		
		@Override public void visit(DateTimeLiteralExpression expr)    { throw new FilterParserException("Operation 'DateTimeLiteralExpression' not yet implemeted."); }
		@Override public void visit(TimeKeyExpression expr)            { throw new FilterParserException("Operation 'TimeKeyExpression' not yet implemeted."); }
		@Override public void visit(OracleHint expr)                   { throw new FilterParserException("Operation 'OracleHint' not yet implemeted."); }
		@Override public void visit(RowConstructor expr)               { throw new FilterParserException("Operation 'RowConstructor' not yet implemeted."); }
		@Override public void visit(MySQLGroupConcat expr)             { throw new FilterParserException("Operation 'MySQLGroupConcat' not yet implemeted."); }
		@Override public void visit(KeepExpression expr)               { throw new FilterParserException("Operation 'KeepExpression' not yet implemeted."); }
		@Override public void visit(NumericBind expr)                  { throw new FilterParserException("Operation 'NumericBind' not yet implemeted."); }
		@Override public void visit(UserVariable expr)                 { throw new FilterParserException("Operation 'UserVariable' not yet implemeted."); }
		@Override public void visit(RegExpMySQLOperator expr)          { throw new FilterParserException("Operation 'RegExpMySQLOperator' not yet implemeted."); }
		@Override public void visit(JsonExpression expr)               { throw new FilterParserException("Operation 'JsonExpression' not yet implemeted."); }
//		@Override public void visit(RegExpMatchOperator expr)          { throw new FilterParserException("Operation 'RegExpMatchOperator' not yet implemeted."); }
		@Override public void visit(OracleHierarchicalExpression expr) { throw new FilterParserException("Operation 'OracleHierarchicalExpression' not yet implemeted."); }
		@Override public void visit(IntervalExpression expr)           { throw new FilterParserException("Operation 'IntervalExpression' not yet implemeted."); }
		@Override public void visit(ExtractExpression expr)            { throw new FilterParserException("Operation 'ExtractExpression' not yet implemeted."); }
//		@Override public void visit(WithinGroupExpression expr)        { throw new FilterParserException("Operation 'WithinGroupExpression' not yet implemeted."); }
		@Override public void visit(AnalyticExpression expr)           { throw new FilterParserException("Operation 'AnalyticExpression' not yet implemeted."); }
		@Override public void visit(Modulo expr)                       { throw new FilterParserException("Operation 'Modulo' not yet implemeted."); }
		@Override public void visit(CastExpression expr)               { throw new FilterParserException("Operation 'CastExpression' not yet implemeted."); }
		@Override public void visit(BitwiseXor expr)                   { throw new FilterParserException("Operation 'BitwiseXor' not yet implemeted."); }
		@Override public void visit(BitwiseOr expr)                    { throw new FilterParserException("Operation 'BitwiseOr' not yet implemeted."); }
		@Override public void visit(BitwiseAnd expr)                   { throw new FilterParserException("Operation 'BitwiseAnd' not yet implemeted."); }
		@Override public void visit(Matches expr)                      { throw new FilterParserException("Operation 'Matches' not yet implemeted."); }
		@Override public void visit(Concat expr)                       { throw new FilterParserException("Operation 'Concat' not yet implemeted."); }
		@Override public void visit(AnyComparisonExpression expr)      { throw new FilterParserException("Operation 'AnyComparisonExpression' not yet implemeted."); }
//		@Override public void visit(AllComparisonExpression expr)      { throw new FilterParserException("Operation 'AllComparisonExpression' not yet implemeted."); } // REMOVED in version between  3.2 -->> 4.3
		@Override public void visit(ExistsExpression expr)             { throw new FilterParserException("Operation 'ExistsExpression' not yet implemeted."); }
		@Override public void visit(WhenClause expr)                   { throw new FilterParserException("Operation 'WhenClause' not yet implemeted."); }
		@Override public void visit(CaseExpression expr)               { throw new FilterParserException("Operation 'CaseExpression' not yet implemeted."); }
		@Override public void visit(SubSelect expr)                    { throw new FilterParserException("Operation 'SubSelect' not yet implemeted."); }
//		@Override public void visit(Column expr)                       { throw new FilterParserException("Operation 'Column' not yet implemeted."); }
//		@Override public void visit(NotEqualsTo expr)                  { throw new FilterParserException("Operation 'NotEqualsTo' not yet implemeted."); }
//		@Override public void visit(MinorThanEquals expr)              { throw new FilterParserException("Operation 'MinorThanEquals' not yet implemeted."); }
//		@Override public void visit(MinorThan expr)                    { throw new FilterParserException("Operation 'MinorThan' not yet implemeted."); }
//		@Override public void visit(LikeExpression expr)               { throw new FilterParserException("Operation 'LikeExpression' not yet implemeted."); }
//		@Override public void visit(IsNullExpression expr)             { throw new FilterParserException("Operation 'IsNullExpression' not yet implemeted."); }
//		@Override public void visit(InExpression expr)                 { throw new FilterParserException("Operation 'InExpression' not yet implemeted."); }
//		@Override public void visit(GreaterThanEquals expr)            { throw new FilterParserException("Operation 'GreaterThanEquals' not yet implemeted."); }
//		@Override public void visit(GreaterThan expr)                  { throw new FilterParserException("Operation 'GreaterThan' not yet implemeted."); }
//		@Override public void visit(EqualsTo expr)                     { throw new FilterParserException("Operation 'EqualsTo' not yet implemeted."); }
//		@Override public void visit(Between expr)                      { throw new FilterParserException("Operation 'Between' not yet implemeted."); }
//		@Override public void visit(OrExpression expr)                 { throw new FilterParserException("Operation 'OrExpression' not yet implemeted."); }
//		@Override public void visit(AndExpression expr)                { throw new FilterParserException("Operation 'AndExpression' not yet implemeted."); }
		@Override public void visit(Subtraction expr)                  { throw new FilterParserException("Operation 'Subtraction' not yet implemeted."); }
		@Override public void visit(Multiplication expr)               { throw new FilterParserException("Operation 'Multiplication' not yet implemeted."); }
		@Override public void visit(Division expr)                     { throw new FilterParserException("Operation 'Division' not yet implemeted."); }
		@Override public void visit(Addition expr)                     { throw new FilterParserException("Operation 'Addition' not yet implemeted."); }
//		@Override public void visit(StringValue expr)                  { throw new FilterParserException("Operation 'StringValue' not yet implemeted."); }
//		@Override public void visit(Parenthesis expr)                  { throw new FilterParserException("Operation 'Parenthesis' not yet implemeted."); }
		@Override public void visit(TimestampValue expr)               { throw new FilterParserException("Operation 'TimestampValue' not yet implemeted."); }
		@Override public void visit(TimeValue expr)                    { throw new FilterParserException("Operation 'TimeValue' not yet implemeted."); }
		@Override public void visit(DateValue expr)                    { throw new FilterParserException("Operation 'DateValue' not yet implemeted."); }
		@Override public void visit(HexValue expr)                     { throw new FilterParserException("Operation 'HexValue' not yet implemeted."); }
//		@Override public void visit(LongValue expr)                    { throw new FilterParserException("Operation 'LongValue' not yet implemeted."); }
//		@Override public void visit(DoubleValue expr)                  { throw new FilterParserException("Operation 'DoubleValue' not yet implemeted."); }
		@Override public void visit(JdbcNamedParameter expr)           { throw new FilterParserException("Operation 'JdbcNamedParameter' not yet implemeted."); }
		@Override public void visit(JdbcParameter expr)                { throw new FilterParserException("Operation 'JdbcParameter' not yet implemeted."); }
//		@Override public void visit(SignedExpression expr)             { throw new FilterParserException("Operation 'SignedExpression' not yet implemeted."); }
		@Override public void visit(Function expr)                     { throw new FilterParserException("Operation 'Function' not yet implemeted."); }
		@Override public void visit(NullValue expr)                    { throw new FilterParserException("Operation 'NullValue' not yet implemeted."); }
		@Override public void visit(JsonOperator expr)                 { throw new FilterParserException("Operation 'JsonOperator' not yet implemeted."); }
		@Override public void visit(NotExpression expr)                { throw new FilterParserException("Operation 'NotExpression' not yet implemeted."); }

		// Going from version 1.1 to 3.2 we needed the below methods
		@Override public void visit(BitwiseRightShift expr)            { throw new FilterParserException("Operation 'BitwiseRightShift' not yet implemeted."); }
		@Override public void visit(BitwiseLeftShift expr)             { throw new FilterParserException("Operation 'BitwiseLeftShift' not yet implemeted."); }
		@Override public void visit(IntegerDivision expr)              { throw new FilterParserException("Operation 'IntegerDivision' not yet implemeted."); }
		@Override public void visit(FullTextSearch expr)               { throw new FilterParserException("Operation 'FullTextSearch' not yet implemeted."); }
		@Override public void visit(IsBooleanExpression expr)          { throw new FilterParserException("Operation 'IsBooleanExpression' not yet implemeted."); }
		@Override public void visit(ValueListExpression expr)          { throw new FilterParserException("Operation 'ValueListExpression' not yet implemeted."); }
		@Override public void visit(NextValExpression expr)            { throw new FilterParserException("Operation 'NextValExpression' not yet implemeted."); }
		@Override public void visit(CollateExpression expr)            { throw new FilterParserException("Operation 'CollateExpression' not yet implemeted."); }
		@Override public void visit(SimilarToExpression expr)          { throw new FilterParserException("Operation 'SimilarToExpression' not yet implemeted."); }
		@Override public void visit(ArrayExpression expr)              { throw new FilterParserException("Operation 'ArrayExpression' not yet implemeted."); }

		// Going from version 3.2 to 4.3 we needed the below methods
		@Override public void visit(XorExpression expr)                { throw new FilterParserException("Operation 'XorExpression' not yet implemeted."); }
		@Override public void visit(TryCastExpression expr)            { throw new FilterParserException("Operation 'TryCastExpression' not yet implemeted."); }
		@Override public void visit(RowGetExpression expr)             { throw new FilterParserException("Operation 'RowGetExpression' not yet implemeted."); }
		@Override public void visit(ArrayConstructor expr)             { throw new FilterParserException("Operation 'ArrayConstructor' not yet implemeted."); }
		@Override public void visit(VariableAssignment expr)           { throw new FilterParserException("Operation 'VariableAssignment' not yet implemeted."); }
		@Override public void visit(XMLSerializeExpr expr)             { throw new FilterParserException("Operation 'XMLSerializeExpr' not yet implemeted."); }
		@Override public void visit(TimezoneExpression expr)           { throw new FilterParserException("Operation 'TimezoneExpression' not yet implemeted."); }
		@Override public void visit(JsonAggregateFunction expr)        { throw new FilterParserException("Operation 'JsonAggregateFunction' not yet implemeted."); }
		@Override public void visit(JsonFunction expr)                 { throw new FilterParserException("Operation 'JsonFunction' not yet implemeted."); }
		@Override public void visit(ConnectByRootOperator expr)        { throw new FilterParserException("Operation 'ConnectByRootOperator' not yet implemeted."); }
		@Override public void visit(OracleNamedFunctionParameter expr) { throw new FilterParserException("Operation 'OracleNamedFunctionParameter' not yet implemeted."); }
		@Override public void visit(AllColumns expr)                   { throw new FilterParserException("Operation 'AllColumns' not yet implemeted."); }
		@Override public void visit(AllTableColumns expr)              { throw new FilterParserException("Operation 'AllTableColumns' not yet implemeted."); }
		@Override public void visit(AllValue expr)                     { throw new FilterParserException("Operation 'AllValue' not yet implemeted."); }

		// Going from version 4.3 to 4.5we needed the below methods
		@Override public void visit(IsDistinctExpression expr)         { throw new FilterParserException("Operation 'IsDistinctExpression' not yet implemeted."); }
		@Override public void visit(GeometryDistance expr)             { throw new FilterParserException("Operation 'GeometryDistance' not yet implemeted."); }
	};
	
	

	/**
	 * Local FILTER Implementation
	 */
	private static abstract class ALocalFilter<M, I> extends RowFilter<M, I>
	{
		public abstract void addFilter(RowFilter<? super M, ? super I> filter);
		public abstract int size();
		public abstract List<RowFilter<? super M, ? super I>> getFilterList();
	}
	/**
	 * Local FILTER Implementation, this is a rip off from RowFilter.orFilter, but you can add filters to it
	 */
	private static class OrFilter<M, I> extends ALocalFilter<M, I>
	{
		protected List<RowFilter<? super M, ? super I>> filters;

		public OrFilter() 
		{ 
			super(); 
			this.filters = new ArrayList<RowFilter<? super M, ? super I>>();
		}
//		public OrFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters)
//		{
//			this.filters = new ArrayList<RowFilter<? super M, ? super I>>();
//			for (RowFilter<? super M, ? super I> filter : filters)
//			{
//				if ( filter == null )
//					throw new IllegalArgumentException("Filter must be non-null");
//				this.filters.add(filter);
//			}
//		}

		@Override
		public void addFilter(RowFilter<? super M, ? super I> filter)
		{
			filters.add(filter);
		}

		@Override
		public int size()
		{
			return filters.size();
		}

		@Override
		public List<RowFilter<? super M, ? super I>> getFilterList()
		{
			return filters;
		}

		@Override
		public String toString()
		{
			return super.toString()+": size="+filters.size()+", filters="+filters;
		}

		@Override
		public boolean include(Entry<? extends M, ? extends I> value)
		{
			for (RowFilter<? super M, ? super I> filter : filters)
				if ( filter.include(value) )
					return true;
			return false;
		}
	}

	/**
	 * Local FILTER Implementation, this is a rip off from RowFilter.andFilter, but you can add filters to it
	 */
	private static class AndFilter<M, I> extends OrFilter<M, I>
	{
		public AndFilter() { super(); }
//		public AndFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters)
//		{
//			super(filters);
//		}

		@Override
		public boolean include(Entry<? extends M, ? extends I> value)
		{
			for (RowFilter<? super M, ? super I> filter : filters)
				if ( !filter.include(value) )
					return false;
			return true;
		}
	}

	/**
	 * Local FILTER Implementation...
	 */
	private static class RowFilterOpValue 
	extends RowFilter<TableModel, Integer>
//	extends RowFilter<Object, Object>
	{
		public static final int FILTER_OP_EQ       = 0;  // EQual
		public static final int FILTER_OP_NE       = 1;  // Not Equal
		public static final int FILTER_OP_GT       = 2;  // Greater Then
		public static final int FILTER_OP_LT       = 3;  // Less Then
		public static final int FILTER_OP_GT_OR_EQ = 4;  // Greater Then OR EQual
		public static final int FILTER_OP_LT_OR_EQ = 5;  // Less Then OR EQual
		public static final int FILTER_OP_IS_NULL  = 6;  // IS NULL

		/** Operator to use when filtering data: QE, NE, LT, GT */
		private int        _filterOp    = -1;

		/** String representation of what the _filterOp is applied on */
		private String     _filterVal   = null;

		/** Column id/pos in the table that we apply the operation on */	
		private int        _colId       = -1;

		/** In Case, the DataCell in the table is String, apply this regexp instead */
		private Pattern    _strPattern = null;

		/** Should we use RegExp for String values when operator is EQ or NEQ */
//		private boolean    _useRegExpForStringIn_EQ_NEQ = false;
		private boolean    _useRegExpForStringIn_EQ_NEQ = Configuration.getCombinedConfiguration().getBooleanProperty("GTableFilter.where.useRegExpForStringIn_EQ_NEQ", false);

		/** In Case, the DataCell in the table is NO String, we need to 
		 * create a object of the same type as the object DataCell. Then use
		 * compare() to check for operator (EQ NE LT GT).
		 * For the moment this is only used for Number objects. 
		 */
		private Object     _filterObj  = null;

		private String opToName()
		{
			switch (_filterOp)
			{
			case FILTER_OP_EQ:       return "EQ";
			case FILTER_OP_NE:       return "NE";
			case FILTER_OP_GT:       return "GT";
			case FILTER_OP_LT:       return "LT";
			case FILTER_OP_GT_OR_EQ: return "GTEQ";
			case FILTER_OP_LT_OR_EQ: return "LTEQ";
			case FILTER_OP_IS_NULL:  return "ISNULL";
			default:                 return "-unknown-op-"; 
			}
			
		}
		@Override
		public String toString()
		{
			return super.toString()+": "+opToName()+" (colId="+_colId+", val='"+_filterVal+"')";
		}

		//---------------------------------------------------------------
		// Constructors 
		//---------------------------------------------------------------
		public RowFilterOpValue(int op, int col, String val)
		{
			super();

			_filterOp       = op;
			_colId          = col;
			_filterVal      = val;

			// used to compare NON String values
			_filterObj = null;

			// build a regExp in case the cell value is a string
			setStrPattern();
		}

		
		//---------------------------------------------------------------
		// private helper methods 
		//---------------------------------------------------------------
		private void setStrPattern()
		{
			if (_filterVal == null || (_filterVal != null && _filterVal.trim().equals("")) )
				_strPattern = Pattern.compile(".*");
			else
				_strPattern = Pattern.compile(_filterVal);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private int compare(Object o1, Object o2)
		{
			// If both values are null return 0
			if (o1 == null && o2 == null)
			{
				return 0;
			}
			else if (o1 == null)
			{ // Define null less than everything.
				return -1;
			}
			else if (o2 == null)
			{
				return 1;
			}

			// make sure we use the collator for string compares
			if ((o1.getClass() == String.class) && (o2.getClass() == String.class))
			{
				return Collator.getInstance().compare((String) o1, (String) o2);
			}

			if ((o1.getClass().isInstance(o2)) && (o1 instanceof Comparable))
			{
				Comparable c1 = (Comparable) o1;
				return c1.compareTo(o2);
			}
			else if (o2.getClass().isInstance(o1) && (o2 instanceof Comparable))
			{
				Comparable c2 = (Comparable) o2;
				return -c2.compareTo(o1);
			}

			return Collator.getInstance().compare(o1.toString(), o2.toString());
		}

	    /**
		 * return true if the row matches the filter, eg will be displayed
		 * return false if value should be displayed, meaning it matches the filter
		 */
		private boolean showInView(Entry<? extends TableModel, ? extends Integer> entry)
		{
			Object cellValue = entry.getValue(_colId);
			
			if (_filterOp == FILTER_OP_IS_NULL)
			{
				TableModel tm = entry.getModel();
				if (tm instanceof CountersModel || tm instanceof ResultSetTableModel)
				{
					// To get the correct "NULL" display value, we need to do the following...
					//cellValue = ((CountersModel)tm).getTabPanel().getDataTable().getNullValueDisplay();
					// But lets sheet a bit... This will NOT work if you change the NULL Display Value using property 'GTable.replace.null.with=SomeOtherNullRepresantation' 
					if (GTable.DEFAULT_NULL_REPLACE.equals(cellValue))
						cellValue = null;
				}
				return cellValue == null;
			}
			
			// Handle NULL values in model.
			if (cellValue == null)
			{
				TableModel tm = entry.getModel();
				if (tm instanceof CountersModel)
				{
					// To get the correct "NULL" display value, we need to do the following...
					//cellValue = ((CountersModel)tm).getTabPanel().getDataTable().getNullValueDisplay();
					// But lets sheet a bit... This will NOT work if you change the NULL Display Value using property 'GTable.replace.null.with=SomeOtherNullRepresantation' 
					cellValue = GTable.DEFAULT_NULL_REPLACE;
				}
				else
				{
					// For all other table models, simply do NOT show null values when filtering
					return false;
				}
			}

			// Create a new object that we use to compare
			if (_filterObj == null)
			{
				String className = "-unknown-";
				try
				{
					if (cellValue instanceof Number)
					{
						className = (String) cellValue.getClass().getName();
						Class<?> clazz = Class.forName(className);
						Constructor<?> constr = clazz.getConstructor(new Class[]{String.class});
						_filterObj = constr.newInstance(new Object[]{_filterVal});
					}
					// For Timestamp, it work just as good using the String Matcher
					//else if (cellValue instanceof Timestamp)
					//{
					//	_filterObj = Timestamp.valueOf(_filterVal);
					//}
					else // make it into a String
					{
						_filterObj = _filterVal;
					}
				}
				catch (Throwable t) 
				{
					// for 'java.lang.reflect.InvocationTargetException', get the "real" problem...
					if (t instanceof InvocationTargetException)
						t = t.getCause();

					// Problems creating a object...
					// So lets go to some fall back... probably a string...
					//e.printStackTrace();
					_logger.info("Problems create a Number of the string '"+_filterVal+"' for filtering, TableCellValueClassName='"+className+"'. using String matching instead. Caught: "+t);
					_filterObj = _filterVal;
				}
			}

			// Check how runtime compare are done, print the filter, and the value we are comparing against...
			if (_logger.isTraceEnabled())
			{
				_logger.trace("showInView(): "+getClass().getSimpleName()+"@"+Integer.toHexString(hashCode())+" - "+opToName()+" - "
					+ "filter=(colId="+_colId+", strVal='"+_filterVal+"', objVal="+_filterObj+", objClass='"+(_filterObj==null?"null":_filterObj.getClass().getName())+"'), "
					+ "cell(id="+entry.getIdentifier()+", val='"+cellValue+"', class='"+(cellValue==null?"null":cellValue.getClass().getName())+"'.");
			}

			// If String, go and do reqexp
			// else USE Comparable on the objects it they implements it, 
			// otherwise do fallback on string matching
			if (_filterOp == FILTER_OP_EQ)
			{
				if (_useRegExpForStringIn_EQ_NEQ && _filterObj instanceof String)
				{
					if (_strPattern == null)
						setStrPattern();
					return _strPattern.matcher(cellValue.toString()).find();
				}
				return compare(cellValue, _filterObj) == 0;
			}
			else if (_filterOp == FILTER_OP_NE)
			{
				if (_useRegExpForStringIn_EQ_NEQ && _filterObj instanceof String)
				{
					if (_strPattern == null)
						setStrPattern();
					return !_strPattern.matcher(cellValue.toString()).find();
				}
				return compare(cellValue, _filterObj) != 0;
			}
			else if (_filterOp == FILTER_OP_LT)
			{
				return compare(cellValue, _filterObj) < 0;
			}
			else if (_filterOp == FILTER_OP_GT)
			{
				return compare(cellValue, _filterObj) > 0;
			}
			else if (_filterOp == FILTER_OP_GT_OR_EQ)
			{
				return compare(cellValue, _filterObj) >= 0;
			}
			else if (_filterOp == FILTER_OP_LT_OR_EQ)
			{
				return compare(cellValue, _filterObj) <= 0;
			}
			else
			{
//				_logger.warn("Unknown _filterOp = "+_filterOp);
			}

			// If we get here, which never really happens, show the row...
			return true; 
		}

		//---------------------------------------------------------------
		// BEGIN: implement javax.swing.RowFilter methods 
		//---------------------------------------------------------------
		@Override
		public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
		{
			return showInView(entry);
		}
//		@Override
//		public boolean include(javax.swing.RowFilter.Entry<? extends Object, ? extends Object> paramEntry)
//		{
//			return showInView(paramEntry);
//		}
		//---------------------------------------------------------------
		// END: implement javax.swing.RowFilter methods 
		//---------------------------------------------------------------
	}
	
	public static class FilterParserException
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public FilterParserException(String message)
		{
			super(message);
		}
	}
}
