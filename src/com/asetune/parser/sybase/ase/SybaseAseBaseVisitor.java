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
// Generated from SybaseAse.g4 by ANTLR 4.0

package com.asetune.parser.sybase.ase;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

public class SybaseAseBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements SybaseAseVisitor<T> {
	@Override public T visitMultExpr(SybaseAseParser.MultExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitStringType(SybaseAseParser.StringTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitConstantList(SybaseAseParser.ConstantListContext ctx) { return visitChildren(ctx); }

	@Override public T visitUnionClause(SybaseAseParser.UnionClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitArgList(SybaseAseParser.ArgListContext ctx) { return visitChildren(ctx); }

	@Override public T visitOrderBy(SybaseAseParser.OrderByContext ctx) { return visitChildren(ctx); }

	@Override public T visitBinaryType(SybaseAseParser.BinaryTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitExpressionList(SybaseAseParser.ExpressionListContext ctx) { return visitChildren(ctx); }

	@Override public T visitInsertStatement(SybaseAseParser.InsertStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitFunctionParams(SybaseAseParser.FunctionParamsContext ctx) { return visitChildren(ctx); }

	@Override public T visitSetStatement(SybaseAseParser.SetStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitFunctionList(SybaseAseParser.FunctionListContext ctx) { return visitChildren(ctx); }

	@Override public T visitNullPart(SybaseAseParser.NullPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitProcedureName(SybaseAseParser.ProcedureNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitFunctionParam(SybaseAseParser.FunctionParamContext ctx) { return visitChildren(ctx); }

	@Override public T visitCommitStatement(SybaseAseParser.CommitStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitOrderDirection(SybaseAseParser.OrderDirectionContext ctx) { return visitChildren(ctx); }

	@Override public T visitSetPart(SybaseAseParser.SetPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitIsolationClause(SybaseAseParser.IsolationClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamType(SybaseAseParser.ParamTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitBeginTransactionStatement(SybaseAseParser.BeginTransactionStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitWaitforStatement(SybaseAseParser.WaitforStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamDeclBlock(SybaseAseParser.ParamDeclBlockContext ctx) { return visitChildren(ctx); }

	@Override public T visitTypeoption(SybaseAseParser.TypeoptionContext ctx) { return visitChildren(ctx); }

	@Override public T visitHashList(SybaseAseParser.HashListContext ctx) { return visitChildren(ctx); }

	@Override public T visitPlanClause(SybaseAseParser.PlanClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitSegmentList(SybaseAseParser.SegmentListContext ctx) { return visitChildren(ctx); }

	@Override public T visitColumnList(SybaseAseParser.ColumnListContext ctx) { return visitChildren(ctx); }

	@Override public T visitSetExpr(SybaseAseParser.SetExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitBreakStatement(SybaseAseParser.BreakStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitColumnPart(SybaseAseParser.ColumnPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitSimpleFunctionParam(SybaseAseParser.SimpleFunctionParamContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamName(SybaseAseParser.ParamNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitJoinCond(SybaseAseParser.JoinCondContext ctx) { return visitChildren(ctx); }

	@Override public T visitBitOp(SybaseAseParser.BitOpContext ctx) { return visitChildren(ctx); }

	@Override public T visitComplexFactor(SybaseAseParser.ComplexFactorContext ctx) { return visitChildren(ctx); }

	@Override public T visitProcedureExecute(SybaseAseParser.ProcedureExecuteContext ctx) { return visitChildren(ctx); }

	@Override public T visitFloatType(SybaseAseParser.FloatTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitSaveTransactionStatement(SybaseAseParser.SaveTransactionStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitOrderPart(SybaseAseParser.OrderPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitHavingClause(SybaseAseParser.HavingClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamPart(SybaseAseParser.ParamPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitSimpleFactor(SybaseAseParser.SimpleFactorContext ctx) { return visitChildren(ctx); }

	@Override public T visitPrefetchSize(SybaseAseParser.PrefetchSizeContext ctx) { return visitChildren(ctx); }

	@Override public T visitRollbackStatement(SybaseAseParser.RollbackStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamset(SybaseAseParser.ParamsetContext ctx) { return visitChildren(ctx); }

	@Override public T visitTableViewName(SybaseAseParser.TableViewNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitIntoClause(SybaseAseParser.IntoClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitSignExpr(SybaseAseParser.SignExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitValuePart(SybaseAseParser.ValuePartContext ctx) { return visitChildren(ctx); }

	@Override public T visitDeleteStatement(SybaseAseParser.DeleteStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitNotExpr(SybaseAseParser.NotExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionName(SybaseAseParser.PartitionNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitBetweenPart(SybaseAseParser.BetweenPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitListPart(SybaseAseParser.ListPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitStatementBlock(SybaseAseParser.StatementBlockContext ctx) { return visitChildren(ctx); }

	@Override public T visitLabelStatement(SybaseAseParser.LabelStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitFactor(SybaseAseParser.FactorContext ctx) { return visitChildren(ctx); }

	@Override public T visitRelExpr(SybaseAseParser.RelExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitErrorPart(SybaseAseParser.ErrorPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitOrderList(SybaseAseParser.OrderListContext ctx) { return visitChildren(ctx); }

	@Override public T visitElsePart(SybaseAseParser.ElsePartContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamBlock(SybaseAseParser.ParamBlockContext ctx) { return visitChildren(ctx); }

	@Override public T visitTableNameOptions(SybaseAseParser.TableNameOptionsContext ctx) { return visitChildren(ctx); }

	@Override public T visitFormatString(SybaseAseParser.FormatStringContext ctx) { return visitChildren(ctx); }

	@Override public T visitSelectStatement(SybaseAseParser.SelectStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitRaiseErrorStatement(SybaseAseParser.RaiseErrorStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitScalartype(SybaseAseParser.ScalartypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitSetOptions(SybaseAseParser.SetOptionsContext ctx) { return visitChildren(ctx); }

	@Override public T visitAltName(SybaseAseParser.AltNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitOnOffPart(SybaseAseParser.OnOffPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitGotoStatement(SybaseAseParser.GotoStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitIdentityColumn(SybaseAseParser.IdentityColumnContext ctx) { return visitChildren(ctx); }

	@Override public T visitHostVariable(SybaseAseParser.HostVariableContext ctx) { return visitChildren(ctx); }

	@Override public T visitExpression(SybaseAseParser.ExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitWhereClause(SybaseAseParser.WhereClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitProcedureDef(SybaseAseParser.ProcedureDefContext ctx) { return visitChildren(ctx); }

	@Override public T visitReadOnlyClause(SybaseAseParser.ReadOnlyClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitMultistatementBlock(SybaseAseParser.MultistatementBlockContext ctx) { return visitChildren(ctx); }

	@Override public T visitSystemOptions(SybaseAseParser.SystemOptionsContext ctx) { return visitChildren(ctx); }

	@Override public T visitIntoOption(SybaseAseParser.IntoOptionContext ctx) { return visitChildren(ctx); }

	@Override public T visitReturnStatement(SybaseAseParser.ReturnStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitIntType(SybaseAseParser.IntTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitArithmeticOp(SybaseAseParser.ArithmeticOpContext ctx) { return visitChildren(ctx); }

	@Override public T visitWaitforSpan(SybaseAseParser.WaitforSpanContext ctx) { return visitChildren(ctx); }

	@Override public T visitPlusExpr(SybaseAseParser.PlusExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionRangeRule(SybaseAseParser.PartitionRangeRuleContext ctx) { return visitChildren(ctx); }

	@Override public T visitUserType(SybaseAseParser.UserTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitFunction(SybaseAseParser.FunctionContext ctx) { return visitChildren(ctx); }

	@Override public T visitSegmentName(SybaseAseParser.SegmentNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitRangePart(SybaseAseParser.RangePartContext ctx) { return visitChildren(ctx); }

	@Override public T visitArgument(SybaseAseParser.ArgumentContext ctx) { return visitChildren(ctx); }

	@Override public T visitSimpleName(SybaseAseParser.SimpleNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionClause(SybaseAseParser.PartitionClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitRangeList(SybaseAseParser.RangeListContext ctx) { return visitChildren(ctx); }

	@Override public T visitValueList(SybaseAseParser.ValueListContext ctx) { return visitChildren(ctx); }

	@Override public T visitPrintStatement(SybaseAseParser.PrintStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitSqlPart(SybaseAseParser.SqlPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitSimpleValue(SybaseAseParser.SimpleValueContext ctx) { return visitChildren(ctx); }

	@Override public T visitGroupBy(SybaseAseParser.GroupByContext ctx) { return visitChildren(ctx); }

	@Override public T visitStatementList(SybaseAseParser.StatementListContext ctx) { return visitChildren(ctx); }

	@Override public T visitStatement(SybaseAseParser.StatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitGlobalVariable(SybaseAseParser.GlobalVariableContext ctx) { return visitChildren(ctx); }

	@Override public T visitDegreeOfParallelism(SybaseAseParser.DegreeOfParallelismContext ctx) { return visitChildren(ctx); }

	@Override public T visitProgram(SybaseAseParser.ProgramContext ctx) { return visitChildren(ctx); }

	@Override public T visitMiscType(SybaseAseParser.MiscTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitSimpleExpression(SybaseAseParser.SimpleExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitHashPart(SybaseAseParser.HashPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitFromClause(SybaseAseParser.FromClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitAndExpr(SybaseAseParser.AndExprContext ctx) { return visitChildren(ctx); }

	@Override public T visitFactorList(SybaseAseParser.FactorListContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionListRule(SybaseAseParser.PartitionListRuleContext ctx) { return visitChildren(ctx); }

	@Override public T visitErrorNumber(SybaseAseParser.ErrorNumberContext ctx) { return visitChildren(ctx); }

	@Override public T visitCoalesceExpression(SybaseAseParser.CoalesceExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitPrintPart(SybaseAseParser.PrintPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitCasePart(SybaseAseParser.CasePartContext ctx) { return visitChildren(ctx); }

	@Override public T visitBrowseClause(SybaseAseParser.BrowseClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitAmbigousStringTypes(SybaseAseParser.AmbigousStringTypesContext ctx) { return visitChildren(ctx); }

	@Override public T visitIfThenElseStatement(SybaseAseParser.IfThenElseStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitCaseExpression(SybaseAseParser.CaseExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitValueList2(SybaseAseParser.ValueList2Context ctx) { return visitChildren(ctx); }

	@Override public T visitSqlExecute(SybaseAseParser.SqlExecuteContext ctx) { return visitChildren(ctx); }

	@Override public T visitNullifExpression(SybaseAseParser.NullifExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitDeclareStatement(SybaseAseParser.DeclareStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitIndexPart(SybaseAseParser.IndexPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitLogicalOp(SybaseAseParser.LogicalOpContext ctx) { return visitChildren(ctx); }

	@Override public T visitUpdateStatement(SybaseAseParser.UpdateStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitExecuteStatement(SybaseAseParser.ExecuteStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitComputeClause(SybaseAseParser.ComputeClauseContext ctx) { return visitChildren(ctx); }

	@Override public T visitDefaulttype(SybaseAseParser.DefaulttypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitNumberOfPartitions(SybaseAseParser.NumberOfPartitionsContext ctx) { return visitChildren(ctx); }

	@Override public T visitConstant(SybaseAseParser.ConstantContext ctx) { return visitChildren(ctx); }

	@Override public T visitObjectName(SybaseAseParser.ObjectNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitRelOp(SybaseAseParser.RelOpContext ctx) { return visitChildren(ctx); }

	@Override public T visitSetValue(SybaseAseParser.SetValueContext ctx) { return visitChildren(ctx); }

	@Override public T visitBeginEndStatement(SybaseAseParser.BeginEndStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitDateType(SybaseAseParser.DateTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitContinueStatement(SybaseAseParser.ContinueStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitParamValue(SybaseAseParser.ParamValueContext ctx) { return visitChildren(ctx); }

	@Override public T visitListList(SybaseAseParser.ListListContext ctx) { return visitChildren(ctx); }

	@Override public T visitTruncateStatement(SybaseAseParser.TruncateStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitSinglestatementBlock(SybaseAseParser.SinglestatementBlockContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionRoundrobinRule(SybaseAseParser.PartitionRoundrobinRuleContext ctx) { return visitChildren(ctx); }

	@Override public T visitWhileStatement(SybaseAseParser.WhileStatementContext ctx) { return visitChildren(ctx); }

	@Override public T visitPartitionHashRule(SybaseAseParser.PartitionHashRuleContext ctx) { return visitChildren(ctx); }

	@Override public T visitJoinList(SybaseAseParser.JoinListContext ctx) { return visitChildren(ctx); }

	@Override public T visitJoinFactor(SybaseAseParser.JoinFactorContext ctx) { return visitChildren(ctx); }

	@Override public T visitDllName(SybaseAseParser.DllNameContext ctx) { return visitChildren(ctx); }

	@Override public T visitPrintMessage(SybaseAseParser.PrintMessageContext ctx) { return visitChildren(ctx); }

	@Override public T visitColumnExpression(SybaseAseParser.ColumnExpressionContext ctx) { return visitChildren(ctx); }

	@Override public T visitCaseList(SybaseAseParser.CaseListContext ctx) { return visitChildren(ctx); }

	@Override public T visitJoinType(SybaseAseParser.JoinTypeContext ctx) { return visitChildren(ctx); }

	@Override public T visitInPart(SybaseAseParser.InPartContext ctx) { return visitChildren(ctx); }

	@Override public T visitOnClause(SybaseAseParser.OnClauseContext ctx) { return visitChildren(ctx); }
}
