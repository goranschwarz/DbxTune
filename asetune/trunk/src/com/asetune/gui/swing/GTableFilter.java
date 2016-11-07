package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.sort.RowFilters;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
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
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.WithinGroupExpression;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

public class GTableFilter 
extends JPanel
{
	private static Logger _logger = Logger.getLogger(GTableFilter.class);
	private static final long serialVersionUID = 1L;

	public static final int ROW_COUNT_LAYOUT_RIGHT = 1;
	public static final int ROW_COUNT_LAYOUT_LEFT  = 2;

	private JXTable    _table      = null;

	private JLabel     _filter_lbl = new JLabel("Filter: ");
//	private JTextField _filter_txt = new JTextField();
//	private GTextFieldWithCompletion _filter_txt = new GTextFieldWithCompletion();
	private GTextField _filter_txt = new GTextField();
	private JLabel     _filter_cnt = new JLabel();
	
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
	
	public GTableFilter(JXTable table)
	{
		this(table, ROW_COUNT_LAYOUT_RIGHT);
	}
	public GTableFilter(JXTable table, int rowCountLayout)
	{
		_table = table;
		_rowCntLayout = rowCountLayout;
		initComponents();
	}

	public int getDeferredFilterSleepTime() { return _deferredFilterSleepTime; }
	public int getDeferredFilterThreshold() { return _deferredFilterThreshold; }
	
	public void setDeferredFilterSleepTime(int timeInMs) { _deferredFilterSleepTime = timeInMs; }
	public void setDeferredFilterThreshold(int rows)     { _deferredFilterThreshold = rows; }
	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setLayout(new MigLayout("insets 0 0 0 0"));   // insets Top Left Bottom Right

		// Add Code Completion to the text field
		_filter_txt.addCompletion(_table);
		
		if (_rowCntLayout == ROW_COUNT_LAYOUT_RIGHT)
		{
			add(_filter_lbl, "");
			add(_filter_txt, "growx, pushx");
			add(_filter_cnt, "wrap");
		}
		else // ROW_COUNT_LAYOUT_LEFT
		{
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
				if (_table.getModel().getRowCount() < _deferredFilterThreshold)
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

		// Set some ToolTip
		_filter_lbl.setToolTipText(FILTER_TOOLTIP);
		_filter_txt.setToolTipText(FILTER_TOOLTIP);
		_filter_cnt.setToolTipText("How many rows does the table contain. (visibleRows/actualRows");
		
		// Set row count label to table size
		resetFilter();
	}
	
	public void applyFilter()
	{
		try
		{
			String searchStringNoTrim = _filter_txt.getText();
			String searchString       = _filter_txt.getText().trim();
			if ( searchString.length() <= 0 ) 
				_table.setRowFilter(null);
			else
			{
				if (searchStringNoTrim.toUpperCase().startsWith("WHERE "))
				{
					// Parse SQL Like
					SimpleSqlWhereTableFilter.setFilterForWhere(_table, searchStringNoTrim.substring("where ".length()).trim());
				}
				else
				{
    				// Create a array with all visible columns... hence: it's only those we want to search
    				// Note the indices are MODEL column index
    				int[] mcols = new int[_table.getColumnCount()];
    				for (int i=0; i<mcols.length; i++)
    					mcols[i] = _table.convertColumnIndexToModel(i);
    
    				_table.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString, mcols));
				}
			}
			_filter_txt.setToolTipText(FILTER_TOOLTIP);
			_filter_txt.setBackground( UIManager.getColor("TextField.background") );
		}
		catch (Throwable t)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Stacktrace for WHERE parsing", t);

			if (t instanceof JSQLParserException)
				t = ExceptionUtils.getRootCause(t);

			_filter_txt.setToolTipText("<html><pre><code>" + StringUtil.toHtmlString(t) + "</code></pre></html>");
			_filter_txt.setBackground(ERROR_COLOR);
		}
		
		NumberFormat nf = NumberFormat.getInstance();
		String rowc = nf.format(_table.getRowCount()) + "/" + nf.format(_table.getModel().getRowCount());
		_filter_cnt.setText(rowc);
	}

	public void resetFilter()
	{
		_filter_txt.setText("");
		_table.setRowFilter(null);

		NumberFormat nf = NumberFormat.getInstance();
		String rowc = nf.format(_table.getRowCount()) + "/" + nf.format(_table.getModel().getRowCount());
		_filter_cnt.setText(rowc);
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
	 * This is where the WHERE clause is parsed...
	 */
	private static class SimpleSqlWhereTableFilter
	implements ExpressionVisitor
	{
		public static void setFilterForWhere(JXTable table, String whereClause)
		throws Exception
		{
			if (table == null || whereClause == null)
				return;

			// If the string starts with "where" remove that before parsing
			if (whereClause.toUpperCase().startsWith("WHERE "))
				whereClause = whereClause.substring("where ".length()).trim();

			// If there is no string, set to NO filter
			if (whereClause.trim().length() == 0)
			{
				table.setRowFilter(null);
				return;
			}

			// Now parse the "where" string and set filter...
			Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause);
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("-------------------------------------------------------------");
				_logger.debug("FULL EXPR: "+expr);
			}

			@SuppressWarnings("unused")
			SimpleSqlWhereTableFilter swpv = new SimpleSqlWhereTableFilter(table, expr);
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
			table.setRowFilter(_andOrStack.peek());
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
			ALocalFilter<TableModel, Integer> thisLevel = _andOrStack.pop();
			ALocalFilter<TableModel, Integer> lastLevel = _andOrStack.peek();
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
			};
			
			
			if (_logger.isDebugEnabled()) in("---> InExpression-visitor: "+expr);
			if ( expr.getLeftExpression() != null )
			{
				expr.getLeftExpression().accept(this);
			}
			else if ( expr.getLeftItemsList() != null )
			{
				expr.getLeftItemsList().accept(ilv);
			}
			expr.getRightItemsList().accept(ilv);
			if (_logger.isDebugEnabled()) in("<--- InExpression-visitor: "+expr);
			
			
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

		//-------------------------------------------------------
		// IS NULL
		//-------------------------------------------------------
		@Override public void visit(IsNullExpression expr)
		{
//			throw new FilterParserException("Operation 'IsNullExpression' not yet implemeted."); 
			if (_logger.isDebugEnabled()) in("---> IsNullExpression-visitor: "+expr);
			expr.getLeftExpression().accept(this);
			if (_logger.isDebugEnabled()) out("<--- IsNullExpression-visitor: "+expr);

			addRowFilter(new RowFilterOpValue(RowFilterOpValue.FILTER_OP_IS_NULL, _lastColIndex, null));
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
				_lastStrValue.replace("''", "'");
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
		@Override public void visit(WithinGroupExpression expr)        { throw new FilterParserException("Operation 'WithinGroupExpression' not yet implemeted."); }
		@Override public void visit(AnalyticExpression expr)           { throw new FilterParserException("Operation 'AnalyticExpression' not yet implemeted."); }
		@Override public void visit(Modulo expr)                       { throw new FilterParserException("Operation 'Modulo' not yet implemeted."); }
		@Override public void visit(CastExpression expr)               { throw new FilterParserException("Operation 'CastExpression' not yet implemeted."); }
		@Override public void visit(BitwiseXor expr)                   { throw new FilterParserException("Operation 'BitwiseXor' not yet implemeted."); }
		@Override public void visit(BitwiseOr expr)                    { throw new FilterParserException("Operation 'BitwiseOr' not yet implemeted."); }
		@Override public void visit(BitwiseAnd expr)                   { throw new FilterParserException("Operation 'BitwiseAnd' not yet implemeted."); }
		@Override public void visit(Matches expr)                      { throw new FilterParserException("Operation 'Matches' not yet implemeted."); }
		@Override public void visit(Concat expr)                       { throw new FilterParserException("Operation 'Concat' not yet implemeted."); }
		@Override public void visit(AnyComparisonExpression expr)      { throw new FilterParserException("Operation 'AnyComparisonExpression' not yet implemeted."); }
		@Override public void visit(AllComparisonExpression expr)      { throw new FilterParserException("Operation 'AllComparisonExpression' not yet implemeted."); }
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
		@Override public void visit(Between expr)                      { throw new FilterParserException("Operation 'Between' not yet implemeted."); }
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
		@Override public void visit(SignedExpression expr)             { throw new FilterParserException("Operation 'SignedExpression' not yet implemeted."); }
		@Override public void visit(Function expr)                     { throw new FilterParserException("Operation 'Function' not yet implemeted."); }
		@Override public void visit(NullValue expr)                    { throw new FilterParserException("Operation 'NullValue' not yet implemeted."); }
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
