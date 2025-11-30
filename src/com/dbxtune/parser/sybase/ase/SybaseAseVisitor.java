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
// Generated from SybaseAse.g4 by ANTLR 4.0

package com.dbxtune.parser.sybase.ase;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

public interface SybaseAseVisitor<T> extends ParseTreeVisitor<T> {
	T visitMultExpr(SybaseAseParser.MultExprContext ctx);

	T visitStringType(SybaseAseParser.StringTypeContext ctx);

	T visitConstantList(SybaseAseParser.ConstantListContext ctx);

	T visitUnionClause(SybaseAseParser.UnionClauseContext ctx);

	T visitArgList(SybaseAseParser.ArgListContext ctx);

	T visitOrderBy(SybaseAseParser.OrderByContext ctx);

	T visitBinaryType(SybaseAseParser.BinaryTypeContext ctx);

	T visitExpressionList(SybaseAseParser.ExpressionListContext ctx);

	T visitInsertStatement(SybaseAseParser.InsertStatementContext ctx);

	T visitFunctionParams(SybaseAseParser.FunctionParamsContext ctx);

	T visitSetStatement(SybaseAseParser.SetStatementContext ctx);

	T visitFunctionList(SybaseAseParser.FunctionListContext ctx);

	T visitNullPart(SybaseAseParser.NullPartContext ctx);

	T visitProcedureName(SybaseAseParser.ProcedureNameContext ctx);

	T visitFunctionParam(SybaseAseParser.FunctionParamContext ctx);

	T visitCommitStatement(SybaseAseParser.CommitStatementContext ctx);

	T visitOrderDirection(SybaseAseParser.OrderDirectionContext ctx);

	T visitSetPart(SybaseAseParser.SetPartContext ctx);

	T visitIsolationClause(SybaseAseParser.IsolationClauseContext ctx);

	T visitParamType(SybaseAseParser.ParamTypeContext ctx);

	T visitBeginTransactionStatement(SybaseAseParser.BeginTransactionStatementContext ctx);

	T visitWaitforStatement(SybaseAseParser.WaitforStatementContext ctx);

	T visitParamDeclBlock(SybaseAseParser.ParamDeclBlockContext ctx);

	T visitTypeoption(SybaseAseParser.TypeoptionContext ctx);

	T visitHashList(SybaseAseParser.HashListContext ctx);

	T visitPlanClause(SybaseAseParser.PlanClauseContext ctx);

	T visitSegmentList(SybaseAseParser.SegmentListContext ctx);

	T visitColumnList(SybaseAseParser.ColumnListContext ctx);

	T visitSetExpr(SybaseAseParser.SetExprContext ctx);

	T visitBreakStatement(SybaseAseParser.BreakStatementContext ctx);

	T visitColumnPart(SybaseAseParser.ColumnPartContext ctx);

	T visitSimpleFunctionParam(SybaseAseParser.SimpleFunctionParamContext ctx);

	T visitParamName(SybaseAseParser.ParamNameContext ctx);

	T visitJoinCond(SybaseAseParser.JoinCondContext ctx);

	T visitBitOp(SybaseAseParser.BitOpContext ctx);

	T visitComplexFactor(SybaseAseParser.ComplexFactorContext ctx);

	T visitProcedureExecute(SybaseAseParser.ProcedureExecuteContext ctx);

	T visitFloatType(SybaseAseParser.FloatTypeContext ctx);

	T visitSaveTransactionStatement(SybaseAseParser.SaveTransactionStatementContext ctx);

	T visitOrderPart(SybaseAseParser.OrderPartContext ctx);

	T visitHavingClause(SybaseAseParser.HavingClauseContext ctx);

	T visitParamPart(SybaseAseParser.ParamPartContext ctx);

	T visitSimpleFactor(SybaseAseParser.SimpleFactorContext ctx);

	T visitPrefetchSize(SybaseAseParser.PrefetchSizeContext ctx);

	T visitRollbackStatement(SybaseAseParser.RollbackStatementContext ctx);

	T visitParamset(SybaseAseParser.ParamsetContext ctx);

	T visitTableViewName(SybaseAseParser.TableViewNameContext ctx);

	T visitIntoClause(SybaseAseParser.IntoClauseContext ctx);

	T visitSignExpr(SybaseAseParser.SignExprContext ctx);

	T visitValuePart(SybaseAseParser.ValuePartContext ctx);

	T visitDeleteStatement(SybaseAseParser.DeleteStatementContext ctx);

	T visitNotExpr(SybaseAseParser.NotExprContext ctx);

	T visitPartitionName(SybaseAseParser.PartitionNameContext ctx);

	T visitBetweenPart(SybaseAseParser.BetweenPartContext ctx);

	T visitListPart(SybaseAseParser.ListPartContext ctx);

	T visitStatementBlock(SybaseAseParser.StatementBlockContext ctx);

	T visitLabelStatement(SybaseAseParser.LabelStatementContext ctx);

	T visitFactor(SybaseAseParser.FactorContext ctx);

	T visitRelExpr(SybaseAseParser.RelExprContext ctx);

	T visitErrorPart(SybaseAseParser.ErrorPartContext ctx);

	T visitOrderList(SybaseAseParser.OrderListContext ctx);

	T visitElsePart(SybaseAseParser.ElsePartContext ctx);

	T visitParamBlock(SybaseAseParser.ParamBlockContext ctx);

	T visitTableNameOptions(SybaseAseParser.TableNameOptionsContext ctx);

	T visitFormatString(SybaseAseParser.FormatStringContext ctx);

	T visitSelectStatement(SybaseAseParser.SelectStatementContext ctx);

	T visitRaiseErrorStatement(SybaseAseParser.RaiseErrorStatementContext ctx);

	T visitScalartype(SybaseAseParser.ScalartypeContext ctx);

	T visitSetOptions(SybaseAseParser.SetOptionsContext ctx);

	T visitAltName(SybaseAseParser.AltNameContext ctx);

	T visitOnOffPart(SybaseAseParser.OnOffPartContext ctx);

	T visitGotoStatement(SybaseAseParser.GotoStatementContext ctx);

	T visitIdentityColumn(SybaseAseParser.IdentityColumnContext ctx);

	T visitHostVariable(SybaseAseParser.HostVariableContext ctx);

	T visitExpression(SybaseAseParser.ExpressionContext ctx);

	T visitWhereClause(SybaseAseParser.WhereClauseContext ctx);

	T visitProcedureDef(SybaseAseParser.ProcedureDefContext ctx);

	T visitReadOnlyClause(SybaseAseParser.ReadOnlyClauseContext ctx);

	T visitMultistatementBlock(SybaseAseParser.MultistatementBlockContext ctx);

	T visitSystemOptions(SybaseAseParser.SystemOptionsContext ctx);

	T visitIntoOption(SybaseAseParser.IntoOptionContext ctx);

	T visitReturnStatement(SybaseAseParser.ReturnStatementContext ctx);

	T visitIntType(SybaseAseParser.IntTypeContext ctx);

	T visitArithmeticOp(SybaseAseParser.ArithmeticOpContext ctx);

	T visitWaitforSpan(SybaseAseParser.WaitforSpanContext ctx);

	T visitPlusExpr(SybaseAseParser.PlusExprContext ctx);

	T visitPartitionRangeRule(SybaseAseParser.PartitionRangeRuleContext ctx);

	T visitUserType(SybaseAseParser.UserTypeContext ctx);

	T visitFunction(SybaseAseParser.FunctionContext ctx);

	T visitSegmentName(SybaseAseParser.SegmentNameContext ctx);

	T visitRangePart(SybaseAseParser.RangePartContext ctx);

	T visitArgument(SybaseAseParser.ArgumentContext ctx);

	T visitSimpleName(SybaseAseParser.SimpleNameContext ctx);

	T visitPartitionClause(SybaseAseParser.PartitionClauseContext ctx);

	T visitRangeList(SybaseAseParser.RangeListContext ctx);

	T visitValueList(SybaseAseParser.ValueListContext ctx);

	T visitPrintStatement(SybaseAseParser.PrintStatementContext ctx);

	T visitSqlPart(SybaseAseParser.SqlPartContext ctx);

	T visitSimpleValue(SybaseAseParser.SimpleValueContext ctx);

	T visitGroupBy(SybaseAseParser.GroupByContext ctx);

	T visitStatementList(SybaseAseParser.StatementListContext ctx);

	T visitStatement(SybaseAseParser.StatementContext ctx);

	T visitGlobalVariable(SybaseAseParser.GlobalVariableContext ctx);

	T visitDegreeOfParallelism(SybaseAseParser.DegreeOfParallelismContext ctx);

	T visitProgram(SybaseAseParser.ProgramContext ctx);

	T visitMiscType(SybaseAseParser.MiscTypeContext ctx);

	T visitSimpleExpression(SybaseAseParser.SimpleExpressionContext ctx);

	T visitHashPart(SybaseAseParser.HashPartContext ctx);

	T visitFromClause(SybaseAseParser.FromClauseContext ctx);

	T visitAndExpr(SybaseAseParser.AndExprContext ctx);

	T visitFactorList(SybaseAseParser.FactorListContext ctx);

	T visitPartitionListRule(SybaseAseParser.PartitionListRuleContext ctx);

	T visitErrorNumber(SybaseAseParser.ErrorNumberContext ctx);

	T visitCoalesceExpression(SybaseAseParser.CoalesceExpressionContext ctx);

	T visitPrintPart(SybaseAseParser.PrintPartContext ctx);

	T visitCasePart(SybaseAseParser.CasePartContext ctx);

	T visitBrowseClause(SybaseAseParser.BrowseClauseContext ctx);

	T visitAmbigousStringTypes(SybaseAseParser.AmbigousStringTypesContext ctx);

	T visitIfThenElseStatement(SybaseAseParser.IfThenElseStatementContext ctx);

	T visitCaseExpression(SybaseAseParser.CaseExpressionContext ctx);

	T visitValueList2(SybaseAseParser.ValueList2Context ctx);

	T visitSqlExecute(SybaseAseParser.SqlExecuteContext ctx);

	T visitNullifExpression(SybaseAseParser.NullifExpressionContext ctx);

	T visitDeclareStatement(SybaseAseParser.DeclareStatementContext ctx);

	T visitIndexPart(SybaseAseParser.IndexPartContext ctx);

	T visitLogicalOp(SybaseAseParser.LogicalOpContext ctx);

	T visitUpdateStatement(SybaseAseParser.UpdateStatementContext ctx);

	T visitExecuteStatement(SybaseAseParser.ExecuteStatementContext ctx);

	T visitComputeClause(SybaseAseParser.ComputeClauseContext ctx);

	T visitDefaulttype(SybaseAseParser.DefaulttypeContext ctx);

	T visitNumberOfPartitions(SybaseAseParser.NumberOfPartitionsContext ctx);

	T visitConstant(SybaseAseParser.ConstantContext ctx);

	T visitObjectName(SybaseAseParser.ObjectNameContext ctx);

	T visitRelOp(SybaseAseParser.RelOpContext ctx);

	T visitSetValue(SybaseAseParser.SetValueContext ctx);

	T visitBeginEndStatement(SybaseAseParser.BeginEndStatementContext ctx);

	T visitDateType(SybaseAseParser.DateTypeContext ctx);

	T visitContinueStatement(SybaseAseParser.ContinueStatementContext ctx);

	T visitParamValue(SybaseAseParser.ParamValueContext ctx);

	T visitListList(SybaseAseParser.ListListContext ctx);

	T visitTruncateStatement(SybaseAseParser.TruncateStatementContext ctx);

	T visitSinglestatementBlock(SybaseAseParser.SinglestatementBlockContext ctx);

	T visitPartitionRoundrobinRule(SybaseAseParser.PartitionRoundrobinRuleContext ctx);

	T visitWhileStatement(SybaseAseParser.WhileStatementContext ctx);

	T visitPartitionHashRule(SybaseAseParser.PartitionHashRuleContext ctx);

	T visitJoinList(SybaseAseParser.JoinListContext ctx);

	T visitJoinFactor(SybaseAseParser.JoinFactorContext ctx);

	T visitDllName(SybaseAseParser.DllNameContext ctx);

	T visitPrintMessage(SybaseAseParser.PrintMessageContext ctx);

	T visitColumnExpression(SybaseAseParser.ColumnExpressionContext ctx);

	T visitCaseList(SybaseAseParser.CaseListContext ctx);

	T visitJoinType(SybaseAseParser.JoinTypeContext ctx);

	T visitInPart(SybaseAseParser.InPartContext ctx);

	T visitOnClause(SybaseAseParser.OnClauseContext ctx);
}
