/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

public class columnInClause  {

    public columnInClause(){}

//    public void printColumns(TExpression expression,TCustomSqlStatement statement){
//        System.out.println("Referenced columns:");
//        columnVisitor cv = new columnVisitor(statement);
//        expression.postOrderTraverse(cv);
//    }
//
//    public void printColumns(TGroupByItemList list,TCustomSqlStatement statement){
//        System.out.println("Referenced columns:");
//        groupByVisitor gbv = new groupByVisitor(statement);
//        list.accept(gbv);
//    }
//
//    public void printColumns(TOrderBy orderBy,TCustomSqlStatement statement){
//        System.out.println("Referenced columns:");
//        orderByVisitor obv = new orderByVisitor(statement);
//        orderBy.accept(obv);
//    }


}

//class columnVisitor implements IExpressionVisitor {
//
//    TCustomSqlStatement statement = null;
//
//    public columnVisitor(TCustomSqlStatement statement) {
//        this.statement = statement;
//    }
//
//    String getColumnWithBaseTable(TObjectName objectName){
//        String ret = "";
//        TTable table = null;
//        boolean  find = false;
//        TCustomSqlStatement lcStmt = statement;
//
//        while ((lcStmt != null) && (!find)){
//            for(int i=0;i<lcStmt.tables.size();i++){
//                table = lcStmt.tables.getTable(i);
//                for(int j=0;j<table.getObjectNameReferences().size();j++){
//                    if (objectName == table.getObjectNameReferences().getObjectName(j)){
//                        if(table.isBaseTable()){
//                            ret =  table.getTableName()+"."+objectName.getColumnNameOnly();
//                        }else{
//                            //derived table
//                            if (table.getAliasClause() != null){
//                               ret =  table.getAliasClause().toString()+"."+objectName.getColumnNameOnly();
//                            }else {
//                                ret =  objectName.getColumnNameOnly();
//                            }
//
//                            ret += "(column in derived table)";
//                        }
//                        find = true;
//                        break;
//                    }
//                }
//            }
//            if(!find){
//                lcStmt = lcStmt.getParentStmt();
//            }
//        }
//
//        return  ret;
//    }
//
//    public boolean exprVisit(TParseTreeNode pNode,boolean isLeafNode){
//         TExpression expr = (TExpression)pNode;
//         switch ((expr.getExpressionType())){
//             case simple_object_name_t:
//                 TObjectName obj = expr.getObjectOperand();
//                 if (obj.getObjectType() != TObjectName.ttobjNotAObject){
//                    System.out.println(getColumnWithBaseTable(obj));
//                 }
//                 break;
//             case function_t:
//                 functionCallVisitor fcv = new functionCallVisitor(statement);
//                 expr.getFunctionCall().accept(fcv);
//                 break;
//         }
//         return  true;
//     }
//
//}
//
//class functionCallVisitor extends TParseTreeVisitor{
//
//    TCustomSqlStatement statement = null;
//
//    public functionCallVisitor(TCustomSqlStatement statement) {
//        this.statement = statement;
//    }
//
//    public void preVisit(TExpression expression){
//        columnVisitor cv = new columnVisitor(statement);
//        expression.postOrderTraverse(cv);
//    }
//}
//
//class groupByVisitor extends TParseTreeVisitor{
//
//    TCustomSqlStatement statement = null;
//
//    public groupByVisitor(TCustomSqlStatement statement) {
//        this.statement = statement;
//    }
//
//    public void preVisit(TExpression expression){
//        columnVisitor cv = new columnVisitor(statement);
//        expression.postOrderTraverse(cv);
//    }
//}
//
//class orderByVisitor extends TParseTreeVisitor{
//
//    TCustomSqlStatement statement = null;
//
//    public orderByVisitor(TCustomSqlStatement statement) {
//        this.statement = statement;
//    }
//
//    public void preVisit(TExpression expression){
//        columnVisitor cv = new columnVisitor(statement);
//        expression.postOrderTraverse(cv);
//    }
//}
