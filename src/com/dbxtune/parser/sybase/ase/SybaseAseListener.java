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

import org.antlr.v4.runtime.tree.ParseTreeListener;

public interface SybaseAseListener extends ParseTreeListener {
	void enterMultExpr(SybaseAseParser.MultExprContext ctx);
	void exitMultExpr(SybaseAseParser.MultExprContext ctx);

	void enterStringType(SybaseAseParser.StringTypeContext ctx);
	void exitStringType(SybaseAseParser.StringTypeContext ctx);

	void enterConstantList(SybaseAseParser.ConstantListContext ctx);
	void exitConstantList(SybaseAseParser.ConstantListContext ctx);

	void enterUnionClause(SybaseAseParser.UnionClauseContext ctx);
	void exitUnionClause(SybaseAseParser.UnionClauseContext ctx);

	void enterArgList(SybaseAseParser.ArgListContext ctx);
	void exitArgList(SybaseAseParser.ArgListContext ctx);

	void enterOrderBy(SybaseAseParser.OrderByContext ctx);
	void exitOrderBy(SybaseAseParser.OrderByContext ctx);

	void enterBinaryType(SybaseAseParser.BinaryTypeContext ctx);
	void exitBinaryType(SybaseAseParser.BinaryTypeContext ctx);

	void enterExpressionList(SybaseAseParser.ExpressionListContext ctx);
	void exitExpressionList(SybaseAseParser.ExpressionListContext ctx);

	void enterInsertStatement(SybaseAseParser.InsertStatementContext ctx);
	void exitInsertStatement(SybaseAseParser.InsertStatementContext ctx);

	void enterFunctionParams(SybaseAseParser.FunctionParamsContext ctx);
	void exitFunctionParams(SybaseAseParser.FunctionParamsContext ctx);

	void enterSetStatement(SybaseAseParser.SetStatementContext ctx);
	void exitSetStatement(SybaseAseParser.SetStatementContext ctx);

	void enterFunctionList(SybaseAseParser.FunctionListContext ctx);
	void exitFunctionList(SybaseAseParser.FunctionListContext ctx);

	void enterNullPart(SybaseAseParser.NullPartContext ctx);
	void exitNullPart(SybaseAseParser.NullPartContext ctx);

	void enterProcedureName(SybaseAseParser.ProcedureNameContext ctx);
	void exitProcedureName(SybaseAseParser.ProcedureNameContext ctx);

	void enterFunctionParam(SybaseAseParser.FunctionParamContext ctx);
	void exitFunctionParam(SybaseAseParser.FunctionParamContext ctx);

	void enterCommitStatement(SybaseAseParser.CommitStatementContext ctx);
	void exitCommitStatement(SybaseAseParser.CommitStatementContext ctx);

	void enterOrderDirection(SybaseAseParser.OrderDirectionContext ctx);
	void exitOrderDirection(SybaseAseParser.OrderDirectionContext ctx);

	void enterSetPart(SybaseAseParser.SetPartContext ctx);
	void exitSetPart(SybaseAseParser.SetPartContext ctx);

	void enterIsolationClause(SybaseAseParser.IsolationClauseContext ctx);
	void exitIsolationClause(SybaseAseParser.IsolationClauseContext ctx);

	void enterParamType(SybaseAseParser.ParamTypeContext ctx);
	void exitParamType(SybaseAseParser.ParamTypeContext ctx);

	void enterBeginTransactionStatement(SybaseAseParser.BeginTransactionStatementContext ctx);
	void exitBeginTransactionStatement(SybaseAseParser.BeginTransactionStatementContext ctx);

	void enterWaitforStatement(SybaseAseParser.WaitforStatementContext ctx);
	void exitWaitforStatement(SybaseAseParser.WaitforStatementContext ctx);

	void enterParamDeclBlock(SybaseAseParser.ParamDeclBlockContext ctx);
	void exitParamDeclBlock(SybaseAseParser.ParamDeclBlockContext ctx);

	void enterTypeoption(SybaseAseParser.TypeoptionContext ctx);
	void exitTypeoption(SybaseAseParser.TypeoptionContext ctx);

	void enterHashList(SybaseAseParser.HashListContext ctx);
	void exitHashList(SybaseAseParser.HashListContext ctx);

	void enterPlanClause(SybaseAseParser.PlanClauseContext ctx);
	void exitPlanClause(SybaseAseParser.PlanClauseContext ctx);

	void enterSegmentList(SybaseAseParser.SegmentListContext ctx);
	void exitSegmentList(SybaseAseParser.SegmentListContext ctx);

	void enterColumnList(SybaseAseParser.ColumnListContext ctx);
	void exitColumnList(SybaseAseParser.ColumnListContext ctx);

	void enterSetExpr(SybaseAseParser.SetExprContext ctx);
	void exitSetExpr(SybaseAseParser.SetExprContext ctx);

	void enterBreakStatement(SybaseAseParser.BreakStatementContext ctx);
	void exitBreakStatement(SybaseAseParser.BreakStatementContext ctx);

	void enterColumnPart(SybaseAseParser.ColumnPartContext ctx);
	void exitColumnPart(SybaseAseParser.ColumnPartContext ctx);

	void enterSimpleFunctionParam(SybaseAseParser.SimpleFunctionParamContext ctx);
	void exitSimpleFunctionParam(SybaseAseParser.SimpleFunctionParamContext ctx);

	void enterParamName(SybaseAseParser.ParamNameContext ctx);
	void exitParamName(SybaseAseParser.ParamNameContext ctx);

	void enterJoinCond(SybaseAseParser.JoinCondContext ctx);
	void exitJoinCond(SybaseAseParser.JoinCondContext ctx);

	void enterBitOp(SybaseAseParser.BitOpContext ctx);
	void exitBitOp(SybaseAseParser.BitOpContext ctx);

	void enterComplexFactor(SybaseAseParser.ComplexFactorContext ctx);
	void exitComplexFactor(SybaseAseParser.ComplexFactorContext ctx);

	void enterProcedureExecute(SybaseAseParser.ProcedureExecuteContext ctx);
	void exitProcedureExecute(SybaseAseParser.ProcedureExecuteContext ctx);

	void enterFloatType(SybaseAseParser.FloatTypeContext ctx);
	void exitFloatType(SybaseAseParser.FloatTypeContext ctx);

	void enterSaveTransactionStatement(SybaseAseParser.SaveTransactionStatementContext ctx);
	void exitSaveTransactionStatement(SybaseAseParser.SaveTransactionStatementContext ctx);

	void enterOrderPart(SybaseAseParser.OrderPartContext ctx);
	void exitOrderPart(SybaseAseParser.OrderPartContext ctx);

	void enterHavingClause(SybaseAseParser.HavingClauseContext ctx);
	void exitHavingClause(SybaseAseParser.HavingClauseContext ctx);

	void enterParamPart(SybaseAseParser.ParamPartContext ctx);
	void exitParamPart(SybaseAseParser.ParamPartContext ctx);

	void enterSimpleFactor(SybaseAseParser.SimpleFactorContext ctx);
	void exitSimpleFactor(SybaseAseParser.SimpleFactorContext ctx);

	void enterPrefetchSize(SybaseAseParser.PrefetchSizeContext ctx);
	void exitPrefetchSize(SybaseAseParser.PrefetchSizeContext ctx);

	void enterRollbackStatement(SybaseAseParser.RollbackStatementContext ctx);
	void exitRollbackStatement(SybaseAseParser.RollbackStatementContext ctx);

	void enterParamset(SybaseAseParser.ParamsetContext ctx);
	void exitParamset(SybaseAseParser.ParamsetContext ctx);

	void enterTableViewName(SybaseAseParser.TableViewNameContext ctx);
	void exitTableViewName(SybaseAseParser.TableViewNameContext ctx);

	void enterIntoClause(SybaseAseParser.IntoClauseContext ctx);
	void exitIntoClause(SybaseAseParser.IntoClauseContext ctx);

	void enterSignExpr(SybaseAseParser.SignExprContext ctx);
	void exitSignExpr(SybaseAseParser.SignExprContext ctx);

	void enterValuePart(SybaseAseParser.ValuePartContext ctx);
	void exitValuePart(SybaseAseParser.ValuePartContext ctx);

	void enterDeleteStatement(SybaseAseParser.DeleteStatementContext ctx);
	void exitDeleteStatement(SybaseAseParser.DeleteStatementContext ctx);

	void enterNotExpr(SybaseAseParser.NotExprContext ctx);
	void exitNotExpr(SybaseAseParser.NotExprContext ctx);

	void enterPartitionName(SybaseAseParser.PartitionNameContext ctx);
	void exitPartitionName(SybaseAseParser.PartitionNameContext ctx);

	void enterBetweenPart(SybaseAseParser.BetweenPartContext ctx);
	void exitBetweenPart(SybaseAseParser.BetweenPartContext ctx);

	void enterListPart(SybaseAseParser.ListPartContext ctx);
	void exitListPart(SybaseAseParser.ListPartContext ctx);

	void enterStatementBlock(SybaseAseParser.StatementBlockContext ctx);
	void exitStatementBlock(SybaseAseParser.StatementBlockContext ctx);

	void enterLabelStatement(SybaseAseParser.LabelStatementContext ctx);
	void exitLabelStatement(SybaseAseParser.LabelStatementContext ctx);

	void enterFactor(SybaseAseParser.FactorContext ctx);
	void exitFactor(SybaseAseParser.FactorContext ctx);

	void enterRelExpr(SybaseAseParser.RelExprContext ctx);
	void exitRelExpr(SybaseAseParser.RelExprContext ctx);

	void enterErrorPart(SybaseAseParser.ErrorPartContext ctx);
	void exitErrorPart(SybaseAseParser.ErrorPartContext ctx);

	void enterOrderList(SybaseAseParser.OrderListContext ctx);
	void exitOrderList(SybaseAseParser.OrderListContext ctx);

	void enterElsePart(SybaseAseParser.ElsePartContext ctx);
	void exitElsePart(SybaseAseParser.ElsePartContext ctx);

	void enterParamBlock(SybaseAseParser.ParamBlockContext ctx);
	void exitParamBlock(SybaseAseParser.ParamBlockContext ctx);

	void enterTableNameOptions(SybaseAseParser.TableNameOptionsContext ctx);
	void exitTableNameOptions(SybaseAseParser.TableNameOptionsContext ctx);

	void enterFormatString(SybaseAseParser.FormatStringContext ctx);
	void exitFormatString(SybaseAseParser.FormatStringContext ctx);

	void enterSelectStatement(SybaseAseParser.SelectStatementContext ctx);
	void exitSelectStatement(SybaseAseParser.SelectStatementContext ctx);

	void enterRaiseErrorStatement(SybaseAseParser.RaiseErrorStatementContext ctx);
	void exitRaiseErrorStatement(SybaseAseParser.RaiseErrorStatementContext ctx);

	void enterScalartype(SybaseAseParser.ScalartypeContext ctx);
	void exitScalartype(SybaseAseParser.ScalartypeContext ctx);

	void enterSetOptions(SybaseAseParser.SetOptionsContext ctx);
	void exitSetOptions(SybaseAseParser.SetOptionsContext ctx);

	void enterAltName(SybaseAseParser.AltNameContext ctx);
	void exitAltName(SybaseAseParser.AltNameContext ctx);

	void enterOnOffPart(SybaseAseParser.OnOffPartContext ctx);
	void exitOnOffPart(SybaseAseParser.OnOffPartContext ctx);

	void enterGotoStatement(SybaseAseParser.GotoStatementContext ctx);
	void exitGotoStatement(SybaseAseParser.GotoStatementContext ctx);

	void enterIdentityColumn(SybaseAseParser.IdentityColumnContext ctx);
	void exitIdentityColumn(SybaseAseParser.IdentityColumnContext ctx);

	void enterHostVariable(SybaseAseParser.HostVariableContext ctx);
	void exitHostVariable(SybaseAseParser.HostVariableContext ctx);

	void enterExpression(SybaseAseParser.ExpressionContext ctx);
	void exitExpression(SybaseAseParser.ExpressionContext ctx);

	void enterWhereClause(SybaseAseParser.WhereClauseContext ctx);
	void exitWhereClause(SybaseAseParser.WhereClauseContext ctx);

	void enterProcedureDef(SybaseAseParser.ProcedureDefContext ctx);
	void exitProcedureDef(SybaseAseParser.ProcedureDefContext ctx);

	void enterReadOnlyClause(SybaseAseParser.ReadOnlyClauseContext ctx);
	void exitReadOnlyClause(SybaseAseParser.ReadOnlyClauseContext ctx);

	void enterMultistatementBlock(SybaseAseParser.MultistatementBlockContext ctx);
	void exitMultistatementBlock(SybaseAseParser.MultistatementBlockContext ctx);

	void enterSystemOptions(SybaseAseParser.SystemOptionsContext ctx);
	void exitSystemOptions(SybaseAseParser.SystemOptionsContext ctx);

	void enterIntoOption(SybaseAseParser.IntoOptionContext ctx);
	void exitIntoOption(SybaseAseParser.IntoOptionContext ctx);

	void enterReturnStatement(SybaseAseParser.ReturnStatementContext ctx);
	void exitReturnStatement(SybaseAseParser.ReturnStatementContext ctx);

	void enterIntType(SybaseAseParser.IntTypeContext ctx);
	void exitIntType(SybaseAseParser.IntTypeContext ctx);

	void enterArithmeticOp(SybaseAseParser.ArithmeticOpContext ctx);
	void exitArithmeticOp(SybaseAseParser.ArithmeticOpContext ctx);

	void enterWaitforSpan(SybaseAseParser.WaitforSpanContext ctx);
	void exitWaitforSpan(SybaseAseParser.WaitforSpanContext ctx);

	void enterPlusExpr(SybaseAseParser.PlusExprContext ctx);
	void exitPlusExpr(SybaseAseParser.PlusExprContext ctx);

	void enterPartitionRangeRule(SybaseAseParser.PartitionRangeRuleContext ctx);
	void exitPartitionRangeRule(SybaseAseParser.PartitionRangeRuleContext ctx);

	void enterUserType(SybaseAseParser.UserTypeContext ctx);
	void exitUserType(SybaseAseParser.UserTypeContext ctx);

	void enterFunction(SybaseAseParser.FunctionContext ctx);
	void exitFunction(SybaseAseParser.FunctionContext ctx);

	void enterSegmentName(SybaseAseParser.SegmentNameContext ctx);
	void exitSegmentName(SybaseAseParser.SegmentNameContext ctx);

	void enterRangePart(SybaseAseParser.RangePartContext ctx);
	void exitRangePart(SybaseAseParser.RangePartContext ctx);

	void enterArgument(SybaseAseParser.ArgumentContext ctx);
	void exitArgument(SybaseAseParser.ArgumentContext ctx);

	void enterSimpleName(SybaseAseParser.SimpleNameContext ctx);
	void exitSimpleName(SybaseAseParser.SimpleNameContext ctx);

	void enterPartitionClause(SybaseAseParser.PartitionClauseContext ctx);
	void exitPartitionClause(SybaseAseParser.PartitionClauseContext ctx);

	void enterRangeList(SybaseAseParser.RangeListContext ctx);
	void exitRangeList(SybaseAseParser.RangeListContext ctx);

	void enterValueList(SybaseAseParser.ValueListContext ctx);
	void exitValueList(SybaseAseParser.ValueListContext ctx);

	void enterPrintStatement(SybaseAseParser.PrintStatementContext ctx);
	void exitPrintStatement(SybaseAseParser.PrintStatementContext ctx);

	void enterSqlPart(SybaseAseParser.SqlPartContext ctx);
	void exitSqlPart(SybaseAseParser.SqlPartContext ctx);

	void enterSimpleValue(SybaseAseParser.SimpleValueContext ctx);
	void exitSimpleValue(SybaseAseParser.SimpleValueContext ctx);

	void enterGroupBy(SybaseAseParser.GroupByContext ctx);
	void exitGroupBy(SybaseAseParser.GroupByContext ctx);

	void enterStatementList(SybaseAseParser.StatementListContext ctx);
	void exitStatementList(SybaseAseParser.StatementListContext ctx);

	void enterStatement(SybaseAseParser.StatementContext ctx);
	void exitStatement(SybaseAseParser.StatementContext ctx);

	void enterGlobalVariable(SybaseAseParser.GlobalVariableContext ctx);
	void exitGlobalVariable(SybaseAseParser.GlobalVariableContext ctx);

	void enterDegreeOfParallelism(SybaseAseParser.DegreeOfParallelismContext ctx);
	void exitDegreeOfParallelism(SybaseAseParser.DegreeOfParallelismContext ctx);

	void enterProgram(SybaseAseParser.ProgramContext ctx);
	void exitProgram(SybaseAseParser.ProgramContext ctx);

	void enterMiscType(SybaseAseParser.MiscTypeContext ctx);
	void exitMiscType(SybaseAseParser.MiscTypeContext ctx);

	void enterSimpleExpression(SybaseAseParser.SimpleExpressionContext ctx);
	void exitSimpleExpression(SybaseAseParser.SimpleExpressionContext ctx);

	void enterHashPart(SybaseAseParser.HashPartContext ctx);
	void exitHashPart(SybaseAseParser.HashPartContext ctx);

	void enterFromClause(SybaseAseParser.FromClauseContext ctx);
	void exitFromClause(SybaseAseParser.FromClauseContext ctx);

	void enterAndExpr(SybaseAseParser.AndExprContext ctx);
	void exitAndExpr(SybaseAseParser.AndExprContext ctx);

	void enterFactorList(SybaseAseParser.FactorListContext ctx);
	void exitFactorList(SybaseAseParser.FactorListContext ctx);

	void enterPartitionListRule(SybaseAseParser.PartitionListRuleContext ctx);
	void exitPartitionListRule(SybaseAseParser.PartitionListRuleContext ctx);

	void enterErrorNumber(SybaseAseParser.ErrorNumberContext ctx);
	void exitErrorNumber(SybaseAseParser.ErrorNumberContext ctx);

	void enterCoalesceExpression(SybaseAseParser.CoalesceExpressionContext ctx);
	void exitCoalesceExpression(SybaseAseParser.CoalesceExpressionContext ctx);

	void enterPrintPart(SybaseAseParser.PrintPartContext ctx);
	void exitPrintPart(SybaseAseParser.PrintPartContext ctx);

	void enterCasePart(SybaseAseParser.CasePartContext ctx);
	void exitCasePart(SybaseAseParser.CasePartContext ctx);

	void enterBrowseClause(SybaseAseParser.BrowseClauseContext ctx);
	void exitBrowseClause(SybaseAseParser.BrowseClauseContext ctx);

	void enterAmbigousStringTypes(SybaseAseParser.AmbigousStringTypesContext ctx);
	void exitAmbigousStringTypes(SybaseAseParser.AmbigousStringTypesContext ctx);

	void enterIfThenElseStatement(SybaseAseParser.IfThenElseStatementContext ctx);
	void exitIfThenElseStatement(SybaseAseParser.IfThenElseStatementContext ctx);

	void enterCaseExpression(SybaseAseParser.CaseExpressionContext ctx);
	void exitCaseExpression(SybaseAseParser.CaseExpressionContext ctx);

	void enterValueList2(SybaseAseParser.ValueList2Context ctx);
	void exitValueList2(SybaseAseParser.ValueList2Context ctx);

	void enterSqlExecute(SybaseAseParser.SqlExecuteContext ctx);
	void exitSqlExecute(SybaseAseParser.SqlExecuteContext ctx);

	void enterNullifExpression(SybaseAseParser.NullifExpressionContext ctx);
	void exitNullifExpression(SybaseAseParser.NullifExpressionContext ctx);

	void enterDeclareStatement(SybaseAseParser.DeclareStatementContext ctx);
	void exitDeclareStatement(SybaseAseParser.DeclareStatementContext ctx);

	void enterIndexPart(SybaseAseParser.IndexPartContext ctx);
	void exitIndexPart(SybaseAseParser.IndexPartContext ctx);

	void enterLogicalOp(SybaseAseParser.LogicalOpContext ctx);
	void exitLogicalOp(SybaseAseParser.LogicalOpContext ctx);

	void enterUpdateStatement(SybaseAseParser.UpdateStatementContext ctx);
	void exitUpdateStatement(SybaseAseParser.UpdateStatementContext ctx);

	void enterExecuteStatement(SybaseAseParser.ExecuteStatementContext ctx);
	void exitExecuteStatement(SybaseAseParser.ExecuteStatementContext ctx);

	void enterComputeClause(SybaseAseParser.ComputeClauseContext ctx);
	void exitComputeClause(SybaseAseParser.ComputeClauseContext ctx);

	void enterDefaulttype(SybaseAseParser.DefaulttypeContext ctx);
	void exitDefaulttype(SybaseAseParser.DefaulttypeContext ctx);

	void enterNumberOfPartitions(SybaseAseParser.NumberOfPartitionsContext ctx);
	void exitNumberOfPartitions(SybaseAseParser.NumberOfPartitionsContext ctx);

	void enterConstant(SybaseAseParser.ConstantContext ctx);
	void exitConstant(SybaseAseParser.ConstantContext ctx);

	void enterObjectName(SybaseAseParser.ObjectNameContext ctx);
	void exitObjectName(SybaseAseParser.ObjectNameContext ctx);

	void enterRelOp(SybaseAseParser.RelOpContext ctx);
	void exitRelOp(SybaseAseParser.RelOpContext ctx);

	void enterSetValue(SybaseAseParser.SetValueContext ctx);
	void exitSetValue(SybaseAseParser.SetValueContext ctx);

	void enterBeginEndStatement(SybaseAseParser.BeginEndStatementContext ctx);
	void exitBeginEndStatement(SybaseAseParser.BeginEndStatementContext ctx);

	void enterDateType(SybaseAseParser.DateTypeContext ctx);
	void exitDateType(SybaseAseParser.DateTypeContext ctx);

	void enterContinueStatement(SybaseAseParser.ContinueStatementContext ctx);
	void exitContinueStatement(SybaseAseParser.ContinueStatementContext ctx);

	void enterParamValue(SybaseAseParser.ParamValueContext ctx);
	void exitParamValue(SybaseAseParser.ParamValueContext ctx);

	void enterListList(SybaseAseParser.ListListContext ctx);
	void exitListList(SybaseAseParser.ListListContext ctx);

	void enterTruncateStatement(SybaseAseParser.TruncateStatementContext ctx);
	void exitTruncateStatement(SybaseAseParser.TruncateStatementContext ctx);

	void enterSinglestatementBlock(SybaseAseParser.SinglestatementBlockContext ctx);
	void exitSinglestatementBlock(SybaseAseParser.SinglestatementBlockContext ctx);

	void enterPartitionRoundrobinRule(SybaseAseParser.PartitionRoundrobinRuleContext ctx);
	void exitPartitionRoundrobinRule(SybaseAseParser.PartitionRoundrobinRuleContext ctx);

	void enterWhileStatement(SybaseAseParser.WhileStatementContext ctx);
	void exitWhileStatement(SybaseAseParser.WhileStatementContext ctx);

	void enterPartitionHashRule(SybaseAseParser.PartitionHashRuleContext ctx);
	void exitPartitionHashRule(SybaseAseParser.PartitionHashRuleContext ctx);

	void enterJoinList(SybaseAseParser.JoinListContext ctx);
	void exitJoinList(SybaseAseParser.JoinListContext ctx);

	void enterJoinFactor(SybaseAseParser.JoinFactorContext ctx);
	void exitJoinFactor(SybaseAseParser.JoinFactorContext ctx);

	void enterDllName(SybaseAseParser.DllNameContext ctx);
	void exitDllName(SybaseAseParser.DllNameContext ctx);

	void enterPrintMessage(SybaseAseParser.PrintMessageContext ctx);
	void exitPrintMessage(SybaseAseParser.PrintMessageContext ctx);

	void enterColumnExpression(SybaseAseParser.ColumnExpressionContext ctx);
	void exitColumnExpression(SybaseAseParser.ColumnExpressionContext ctx);

	void enterCaseList(SybaseAseParser.CaseListContext ctx);
	void exitCaseList(SybaseAseParser.CaseListContext ctx);

	void enterJoinType(SybaseAseParser.JoinTypeContext ctx);
	void exitJoinType(SybaseAseParser.JoinTypeContext ctx);

	void enterInPart(SybaseAseParser.InPartContext ctx);
	void exitInPart(SybaseAseParser.InPartContext ctx);

	void enterOnClause(SybaseAseParser.OnClauseContext ctx);
	void exitOnClause(SybaseAseParser.OnClauseContext ctx);
}
