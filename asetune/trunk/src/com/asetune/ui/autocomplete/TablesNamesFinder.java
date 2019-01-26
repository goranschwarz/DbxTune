/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.ui.autocomplete;

//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//import net.sf.jsqlparser.expression.AllComparisonExpression;
//import net.sf.jsqlparser.expression.AnyComparisonExpression;
//import net.sf.jsqlparser.expression.BinaryExpression;
//import net.sf.jsqlparser.expression.CaseExpression;
//import net.sf.jsqlparser.expression.DateValue;
//import net.sf.jsqlparser.expression.DoubleValue;
//import net.sf.jsqlparser.expression.Expression;
//import net.sf.jsqlparser.expression.ExpressionVisitor;
//import net.sf.jsqlparser.expression.Function;
//import net.sf.jsqlparser.expression.InverseExpression;
//import net.sf.jsqlparser.expression.JdbcParameter;
//import net.sf.jsqlparser.expression.LongValue;
//import net.sf.jsqlparser.expression.NullValue;
//import net.sf.jsqlparser.expression.Parenthesis;
//import net.sf.jsqlparser.expression.TimeValue;
//import net.sf.jsqlparser.expression.TimestampValue;
//import net.sf.jsqlparser.expression.WhenClause;
//import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
//import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
//import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
//import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
//import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
//import net.sf.jsqlparser.expression.operators.arithmetic.Division;
//import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
//import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
//import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
//import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
//import net.sf.jsqlparser.expression.operators.relational.Between;
//import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
//import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
//import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
//import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
//import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
//import net.sf.jsqlparser.expression.operators.relational.InExpression;
//import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
//import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
//import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
//import net.sf.jsqlparser.expression.operators.relational.Matches;
//import net.sf.jsqlparser.expression.operators.relational.MinorThan;
//import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
//import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
//import net.sf.jsqlparser.schema.Column;
//import net.sf.jsqlparser.schema.Table;
//import net.sf.jsqlparser.statement.select.FromItemVisitor;
//import net.sf.jsqlparser.statement.select.Join;
//import net.sf.jsqlparser.statement.select.PlainSelect;
//import net.sf.jsqlparser.statement.select.Select;
//import net.sf.jsqlparser.statement.select.SelectVisitor;
//import net.sf.jsqlparser.statement.select.SubJoin;
//import net.sf.jsqlparser.statement.select.SubSelect;
//import net.sf.jsqlparser.statement.select.Union;

public class TablesNamesFinder
{
	
}
////public class TablesNamesFinder implements SelectVisitor, FromItemVisitor, JoinVisitor, ExpressionVisitor, ItemsListVisitor {
//public class TablesNamesFinder implements SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor
//{
//	private List<String>	tables;
//
//	public List<String> getTableList(Select select)
//	{
//		tables = new ArrayList<String>();
//		select.getSelectBody().accept(this);
//		return tables;
//	}
//
//	@Override
//	public void visit(PlainSelect plainSelect)
//	{
//		plainSelect.getFromItem().accept(this);
//
//		if ( plainSelect.getJoins() != null )
//		{
//			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();)
//			{
//				Join join = (Join) joinsIt.next();
//				join.getRightItem().accept(this);
//			}
//		}
//		if ( plainSelect.getWhere() != null )
//			plainSelect.getWhere().accept(this);
//
//	}
//
//	@Override
//	public void visit(Union union)
//	{
//		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();)
//		{
//			PlainSelect plainSelect = (PlainSelect) iter.next();
//			visit(plainSelect);
//		}
//	}
//
//	@Override
//	public void visit(Table tableName)
//	{
//		String tableWholeName = tableName.getWholeTableName();
//		tables.add(tableWholeName);
//	}
//
//	@Override
//	public void visit(SubSelect subSelect)
//	{
//		subSelect.getSelectBody().accept(this);
//	}
//
//	@Override
//	public void visit(Addition addition)
//	{
//		visitBinaryExpression(addition);
//	}
//
//	@Override
//	public void visit(AndExpression andExpression)
//	{
//		visitBinaryExpression(andExpression);
//	}
//
//	@Override
//	public void visit(Between between)
//	{
//		between.getLeftExpression().accept(this);
//		between.getBetweenExpressionStart().accept(this);
//		between.getBetweenExpressionEnd().accept(this);
//	}
//
//	@Override
//	public void visit(Column tableColumn)
//	{
//	}
//
//	@Override
//	public void visit(Division division)
//	{
//		visitBinaryExpression(division);
//	}
//
//	@Override
//	public void visit(DoubleValue doubleValue)
//	{
//	}
//
//	@Override
//	public void visit(EqualsTo equalsTo)
//	{
//		visitBinaryExpression(equalsTo);
//	}
//
//	@Override
//	public void visit(Function function)
//	{
//	}
//
//	@Override
//	public void visit(GreaterThan greaterThan)
//	{
//		visitBinaryExpression(greaterThan);
//	}
//
//	@Override
//	public void visit(GreaterThanEquals greaterThanEquals)
//	{
//		visitBinaryExpression(greaterThanEquals);
//	}
//
//	@Override
//	public void visit(InExpression inExpression)
//	{
//		inExpression.getLeftExpression().accept(this);
//		inExpression.getItemsList().accept(this);
//	}
//
//	@Override
//	public void visit(InverseExpression inverseExpression)
//	{
//		inverseExpression.getExpression().accept(this);
//	}
//
//	@Override
//	public void visit(IsNullExpression isNullExpression)
//	{
//	}
//
//	@Override
//	public void visit(JdbcParameter jdbcParameter)
//	{
//	}
//
//	@Override
//	public void visit(LikeExpression likeExpression)
//	{
//		visitBinaryExpression(likeExpression);
//	}
//
//	@Override
//	public void visit(ExistsExpression existsExpression)
//	{
//		existsExpression.getRightExpression().accept(this);
//	}
//
//	@Override
//	public void visit(LongValue longValue)
//	{
//	}
//
//	@Override
//	public void visit(MinorThan minorThan)
//	{
//		visitBinaryExpression(minorThan);
//	}
//
//	@Override
//	public void visit(MinorThanEquals minorThanEquals)
//	{
//		visitBinaryExpression(minorThanEquals);
//	}
//
//	@Override
//	public void visit(Multiplication multiplication)
//	{
//		visitBinaryExpression(multiplication);
//	}
//
//	@Override
//	public void visit(NotEqualsTo notEqualsTo)
//	{
//		visitBinaryExpression(notEqualsTo);
//	}
//
//	@Override
//	public void visit(NullValue nullValue)
//	{
//	}
//
//	@Override
//	public void visit(OrExpression orExpression)
//	{
//		visitBinaryExpression(orExpression);
//	}
//
//	@Override
//	public void visit(Parenthesis parenthesis)
//	{
//		parenthesis.getExpression().accept(this);
//	}
//
//	@Override
//	public void visit(net.sf.jsqlparser.expression.StringValue stringValue)
//	{
//	}
//
//	@Override
//	public void visit(Subtraction subtraction)
//	{
//		visitBinaryExpression(subtraction);
//	}
//
////	@Override
//	public void visitBinaryExpression(BinaryExpression binaryExpression)
//	{
//		binaryExpression.getLeftExpression().accept(this);
//		binaryExpression.getRightExpression().accept(this);
//	}
//
//	@Override
//	public void visit(ExpressionList expressionList)
//	{
//		for (Iterator iter = expressionList.getExpressions().iterator(); iter.hasNext();)
//		{
//			Expression expression = (Expression) iter.next();
//			expression.accept(this);
//		}
//
//	}
//
//	@Override
//	public void visit(DateValue dateValue)
//	{
//	}
//
//	@Override
//	public void visit(TimestampValue timestampValue)
//	{
//	}
//
//	@Override
//	public void visit(TimeValue timeValue)
//	{
//	}
//
//	@Override
//	public void visit(CaseExpression caseExpression)
//	{
//	}
//
//	@Override
//	public void visit(WhenClause whenClause)
//	{
//	}
//
//	@Override
//	public void visit(AllComparisonExpression allComparisonExpression)
//	{
//		allComparisonExpression.GetSubSelect().getSelectBody().accept(this);
//	}
//
//	@Override
//	public void visit(AnyComparisonExpression anyComparisonExpression)
//	{
//		anyComparisonExpression.GetSubSelect().getSelectBody().accept(this);
//	}
//
//	@Override
//	public void visit(SubJoin subjoin)
//	{
//		subjoin.getLeft().accept(this);
//		subjoin.getJoin().getRightItem().accept(this);
//	}
//
//	@Override
//	public void visit(Concat arg0)
//	{
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	public void visit(Matches arg0)
//	{
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	public void visit(BitwiseAnd arg0)
//	{
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	public void visit(BitwiseOr arg0)
//	{
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	public void visit(BitwiseXor arg0)
//	{
//		// TODO Auto-generated method stub
//	}
//
//}
