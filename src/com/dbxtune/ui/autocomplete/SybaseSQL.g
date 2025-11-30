grammar SybaseSQL;

options {
    language=Java;
    output=AST;
    backtrack = true;
    //k=1;
}

tokens {
    // Generic Section
    TN_PROCDEF;
    TN_PARAMS;
    TN_RESULTS;
    TN_LOCALS;
    TN_CODE;
    TN_PARAM;
    TN_PARAM_COMMA;
    TN_LOCAL;
    TN_TODO;
    TN_COMMIT;
    TN_ROLLBACK;
    TN_CREATESYNONYM;
    TN_MESSAGE;
    TN_CALL;
    TN_PARAMVARS;
    TN_RESULTVARS;
    TN_RETURNVAR;
    TN_ERROR;
    TN_TEST;
    TN_IF;
    TN_WHILE;
    TN_EXITLOOP;
    TN_ASSIGN;
    TN_RETURN;
    TN_RESULT;
    TN_SELECT;
    TN_COLS;
    TN_COL;
    TN_COL_NAME;
    TN_COL_VALUE;
    TN_FROM;
    TN_WHERE;
    TN_GROUPBY;
    TN_WHERE;
    TN_ORDERBY;
    TN_HAVING;
    TN_RAISE;
    TN_NAME;
    TN_JOIN;
    TN_ON;
    TN_FUNC;
    TN_UNION;
    TN_UPDATE;
    TN_UPDATE_SETBLOCK;
    TN_SET;
    TN_IN;
    TN_BETWEEN;
    TN_COL_STAR;
    TN_INSERT;
    TN_SELECTLOOP;
    TN_EVENT_REGISTER;
    TN_EVENT_UNREGISTER;
    TN_DELETE;
    TN_IGNORE;
    TN_LABEL;

    //These have been used in the Sybase grammar and need adding to the Ingers grammars.
    TN_CASE;
    TN_CASE_PART;
    TN_CASE_ELSE;
    TN_CASE_TEST;
    TN_CASE_VALUE;
    TN_COALESCE;
    TN_EXPR;
    TN_NULLIF;  
    TN_TABLE_NAME;
    TN_TABLE_VALUE; 
    
    SYB_CONTINUE;
    SYB_DLL;
    SYB_TYPE;
    SYB_INTO;   
    SYB_ID;
    SYB_INDEX;
    SYB_TABLE;
    SYB_ARGUMENTS;
    SYB_USER_TYPE;
    SYB_LABEL;
    SYB_GOTO;
    SYB_WAITFOR;
    SYB_BEGIN_END;
    SYB_BEGIN_TRAN;
    SYB_SQL_CALL;
    SYB_SQL_PART;
    SYB_ERRORDATA;
    SYB_SET;
    SYB_TRUNCATE;
    SYB_DEFAULT;
    SYB_DECLARE;
    SYB_SAVETRANS;
    SYB_TIME;
    SYB_NCHAR;
    SYB_NVARCHAR;
    SYB_FLOAT;
    SYB_DECIMAL;
    SYB_TINYINT;
    SYB_GLOBAL_VAR;
    SYB_FORMAT_STRING;
    SYB_MSG;
}

@lexer::header {

/*
 * Copyright (C) 2010 Ingres Corp.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see
 * <http://www.gnu.org/licenses/> or
 * <http://www.ingres.com/about/licenses/gpl.php> .
 */

package com.ingres.antlr.idiom;

/**
 * The Sybase Lexer that takes an input stream of chars for a Sybase TSQL DB proc
 * and produces a token stream to send to the Sybase Parser. 
 *
 * <!--                                                         
 * Class           : SybaseSQLLexer                                                                                                                  
 * System Name     : IngresDBProcConverter                                                       
 * Sub-System Name : NA
 * 
 * Version History
 *
 * Version  Date        Who     Description
 * -------  ----------- -----   -----------
 * 1.0      11 Sep 2009 chesh01 Original version.
 * -->
 */}

@parser::header {package com.ingres.antlr.idiom;
/**
 * The Sybase Parser that takes a token stream provided by the SybaseSQLLexer
 * and parses the tokens, producing an Abstract Syntax Tree (AST). 
 *
 * <!--                                                         
 * Class           : SybaseSQLParser                                                                                                                   
 * System Name     : IngresDBProcConverter                                                       
 * Sub-System Name : NA
 * 
 * Version History
 *
 * Version  Date        Who     Description
 * -------  ----------- -----   -----------
 * 1.0      11 Sep 2009 chesh01 Original version.
 * -->
 */}

@rulecatch {
        catch (RecognitionException re)
        {
            // Use reportError to write out the details of the exception. This will not write out all erroneous
            // tokens though.
            // reportError(re);

            // This consumes tokens until it begins to find tokens that it can parse.
            recover(input, re);
            
            // Now set the stop index of the return tree to be the last token consumed during recovery.
            retval.stop = input.get(input.index());
            
            for (int i =retval.start.getTokenIndex(); i <= retval.stop.getTokenIndex(); i++)
            {
                Token t = input.get(i);
                t.setChannel(Token.HIDDEN_CHANNEL);
                
                if (i == retval.start.getTokenIndex())
                {
                    t.setText("/* TODO: Convert to Ingres Syntax." + '\r' + '\n' + t.getText());
                }
                else if (i == retval.stop.getTokenIndex())
                {
                    t.setText(t.getText() + " */" + '\r' + '\n');
                }
            }
            
            retval.tree = null;
            
//          // Create an AST rewrite stream to add all erroneous tokens to.
//            RewriteRuleTokenStream stream_err = new RewriteRuleTokenStream(adaptor,
//                                                                           "token TN_ERROR_TOKEN");
//          // Create a root node to add the error tokens to.                                                                   
//            Object err_root = (Object)adaptor.nil();
//                
//            if (state.backtracking == 0)
//            {
//              // Add all tokens since the last parsed token upto the last token error token consumed.
//                for (int i = retval.start.getTokenIndex(); i <= input.index(); i++)
//                {
//                    stream_err.add(input.get(i));
//                }
//
//              // Create a root node of TN_ERROR_TOKEN to add the erroneous tokens to.
//                err_root = (Object)adaptor.becomeRoot((Object)adaptor.create(TN_ERROR_TOKEN,
//                                                                             "TN_ERROR_TOKEN"),
//                                                      err_root);
//
//              // Add the erroneous tokens to the root error node token.
//                while (stream_err.hasNext())
//                {
//                    adaptor.addChild(err_root, stream_err.nextNode());
//                }
//
//              // Set the return tree for the method to the error token we have created.
//                retval.tree = err_root;
//
//              // Tidy up the node we are returning and set the token start/stop boundaries.
//                retval.tree = (Object)adaptor.rulePostProcessing(err_root);
//                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
//            }
        }
}
 
// Rules


program :   procedure_def;

//Procedure declaration.
procedure_def
    :   CREATE (PROCEDURE | PROC) procedure_name paramDeclBlock? (WITH RECOMPILE)? AS 
        (statement_list | (EXTERNAL NAME dll_name)) 
        -> ^(TN_PROCDEF procedure_name 
                     ^(TN_PARAMS paramDeclBlock)?
                     ^(TN_CODE statement_list)?
                     ^(SYB_DLL dll_name)?)
    ;

//List of statements in a stored procedure. 
statement_list
    :   statement (SEMI? statement)* SEMI?
        -> statement+
    ;   
        
execute_statement
    :   (EXEC | EXECUTE)?  (procedure_execute | sql_execute)
        -> ^(TN_CALL procedure_execute)? 
           ^(SYB_SQL_CALL sql_execute)?
    ;
    
procedure_execute
    :   (host_variable EQ)? procedure_name paramBlock? (WITH RECOMPILE)?
        -> procedure_name 
            ^(TN_PARAMVARS paramBlock)?
                    ^(TN_RETURNVAR host_variable)?
    ;

//Execute command can be used to execute T-SQL commands by passing them in as string literals.  
sql_execute
    :   LPAREN? sql_part (PLUS sql_part)* RPAREN? 
        -> ^(SYB_SQL_PART sql_part) ^(SYB_SQL_PART sql_part)*
    ;   

//Part of the T-SQL command to be executed. 
sql_part
    :   STRING_LIT | host_variable
    ;

//Parameters for the procedure being executed by the Execute Procedure command.
paramBlock
    :   param_part (COMMA param_part)* 
        -> ^(TN_PARAM param_part) ^(TN_PARAM param_part)*
    ;   
    
param_part
    :   (host_variable EQ)? ( param_value | (host_variable (OUTPUT | OUT)?))
    ;       

param_value
    :   constant | ID
    ;   

//The name of the dynamic link library (DLL) or shared library containing the functions that implement 
//the extended stored procedure.
dll_name    
    :   ID
    |   STRING_LIT  
    ;
          
procedure_name          
    :   object_name (SEMI (PLUS | MINUS)? INT_LIT)? -> object_name
    ;

//Object names can be of the form ID.ID.ID.ID  Done as ID.ID for now. More causes errors.   
object_name
    :   (ID DOT)? ID
    ;   

//The parameters for the procedure being created.
paramDeclBlock
    :   (LPAREN)? paramset (RPAREN)? -> paramset
    ;   
    
paramset:   (param_name param_type) (COMMA param_name param_type)* 
        -> ^(TN_PARAM param_name param_type) ^(TN_PARAM param_name param_type)*  
    ;   
    
param_name
    :   (STRUDEL object_name | STRUDEL STRUDEL object_name) -> object_name 
    ;

//Sybase types differ fron Ingres types. Hence the node SYB_TYPE.   
param_type
    :   scalartype typeoption* //-> ^(SYB_TYPE scalartype) typeoption*
    ;
    
//Sybase types. 
scalartype
    :   int_type
    |   float_type
    |   string_type 
    |   binary_type
    |   date_type
    |   misc_type
    |   user_type
    ;
    
typeoption
    :   (defaulttype)   -> ^(SYB_DEFAULT defaulttype)
    |   (OUTPUT | OUT)
    ;   

//Assigns a default value to a parameter.   
defaulttype
    :   EQ constant -> constant
    ;
    
statement_block
    :   multistatement_block | singlestatement_block
    ;                   

//Multiple statements in an If-Else statement or While loop should be enclosed with Begin - End.    
multistatement_block
    :   BEGIN statement (SEMI? statement)* SEMI? END -> ^(TN_CODE statement+)
    ;

singlestatement_block
    :   statement SEMI? -> ^(TN_CODE statement) 
    ;
    
begin_end_statement
    :   multistatement_block
        -> ^(SYB_BEGIN_END multistatement_block)
    ;   

//Sybase statements. 
statement
    :   if_then_else_statement
    |   return_statement
    |   begin_transaction_statement
    |   commit_statement 
    |   rollback_statement
    |   save_transaction_statement
    |   declare_statement
    |   print_statement
    |   break_statement
    |   continue_statement
    |   goto_statement
    |   label_statement
    |   waitfor_statement
    |   begin_end_statement
    |   raise_error_statement
    |   set_statement
    |   select_statement
    |   insert_statement
    |   delete_statement
    |   update_statement
    |   truncate_statement
    |   while_statement
    |   execute_statement
    ;   
    
if_then_else_statement
    :   IF expression plan_clause? statement_block else_part?
        -> ^(TN_IF ^(TN_TEST expression) statement_block else_part?)
    ;

else_part
    :   ELSE statement_block -> statement_block
    ;   
    
while_statement
    :   WHILE expression plan_clause? statement_block
        -> ^(TN_WHILE ^(TN_TEST expression) statement_block)
    ;   
    
break_statement
    :   BREAK
        -> ^(TN_EXITLOOP)
    ;   
    
continue_statement
    :   CONTINUE
        -> ^(SYB_CONTINUE)
    ;   
    
begin_transaction_statement
    :   BEGIN (TRAN | TRANSACTION) simple_name?
        -> ^(SYB_BEGIN_TRAN simple_name?) 
    ;   
    
commit_statement
    :   COMMIT (TRAN | TRANSACTION | WORK)? simple_name? 
        -> ^(TN_COMMIT simple_name?)
    ;
    
rollback_statement
    :   ROLLBACK (TRAN | TRANSACTION | WORK)? simple_name?
        -> ^(TN_ROLLBACK simple_name?)
    ;       
        
save_transaction_statement
    :   SAVE TRANSACTION simple_name
        -> ^(SYB_SAVETRANS simple_name)
    ;       
        
return_statement
    :   RETURN (expression)?
        -> ^(TN_RETURN expression?)
    ;

//Goto label.   
goto_statement
    :   GOTO ID     -> ^(SYB_GOTO ID)
    ;
    
//Label for the Goto statement. 
label_statement
    :   ID COLON    -> ^(SYB_LABEL ID)
    ;   
    
//The WaitFor statement specifies a specific time, a time interval, or an event for the execution of a 
//statement block, stored procedure, or transaction.
waitfor_statement
    :   WAITFOR ( (DELAY waitfor_span) | (TIME waitfor_span) | ERROREXIT | PROCESSEXIT | MIRROREXIT )+ 
        -> ^(SYB_WAITFOR waitfor_span?)
    ;   
    
waitfor_span
    :   STRING_LIT | host_variable
    ;   

//Declares local variables for a procedure. 
declare_statement
    :   DECLARE paramset -> ^(SYB_DECLARE paramset)
    ;

    
raise_error_statement
    :   RAISERROR error_number print_message? (COMMA arg_list)?
            (WITH ERRORDATA column_list)?
            -> ^(TN_ERROR error_number print_message?
                ^(SYB_ARGUMENTS arg_list)?
                ^(SYB_ERRORDATA column_list)?)
    ;   

//The error number for the Raise Error statement.   
error_number
    :   host_variable | INT_LIT
    ;   


//Sybase Select.    
select_statement
    :   SELECT ( ALL | DISTINCT )? (TOP INT_LIT)? column_list 
        into_clause?
        from_clause?
        where_clause?
        group_by?
        having_clause? 
        union_clause*
        order_by?
        compute_clause*
        read_only_clause?
        isolation_clause?
        browse_clause?
        plan_clause? 
        -> ^(TN_SELECT DISTINCT?
            ^(TN_COLS column_list)
            ^(SYB_INTO into_clause)?
            ^(TN_FROM from_clause)?
            ^(TN_WHERE where_clause)?
                ^(TN_GROUPBY group_by)?
                ^(TN_HAVING having_clause)?
                ^(TN_UNION union_clause)*
                ^(TN_ORDERBY order_by)?) 
    ; 

    
//Write a message to screen. Same as Ingres Message statement.  
print_statement
    :   PRINT format_string COMMA arg_list  -> ^(SYB_FORMAT_STRING format_string arg_list)
    |   PRINT print_message         -> ^(SYB_MSG print_message)
    ;
    
format_string
    :   STRING_LIT
    ;
    
print_message
    :   print_part (PLUS print_part)* //String concatenation
    ;   
    
print_part
    :   STRING_LIT 
    |   host_variable
    ;   

//List of arguments for the Print statement or the Raise Error statement.
arg_list
    :   argument (COMMA argument)* -> argument argument*
    ;

argument
    :   host_variable | constant | ID
    |   expression
    ;

insert_statement
    :   INSERT (INTO)? object_name (LPAREN column_list RPAREN)? 
            ( value_part | (select_statement plan_clause?) )
            -> ^(TN_INSERT object_name ^(TN_COLS column_list)? value_part? select_statement?)
    ;   
    
value_part
    :   VALUES LPAREN value_list RPAREN
    ;   
    
//The list of values to be inserted.    
value_list
    :   simple_value (COMMA simple_value)*
        -> ^(TN_PARAM simple_value) ^(TN_PARAM simple_value)* 
    ;   
    
simple_value
    :   expression | DEFAULT
    ;
    
delete_statement
    :   DELETE (FROM)? object_name from_clause? where_clause? plan_clause?
        -> ^(TN_DELETE object_name
                   ^(TN_FROM from_clause)?
                   ^(TN_WHERE where_clause)?)
    ;   
    
update_statement
    :   UPDATE object_name set_part from_clause? where_clause? plan_clause?
        -> ^(TN_UPDATE object_name
                   ^(TN_UPDATE_SETBLOCK set_part)
               ^(TN_FROM from_clause)?
               ^(TN_WHERE where_clause)?)               
    ;
    
truncate_statement
    :   TRUNCATE TABLE object_name 
        -> ^(SYB_TRUNCATE object_name)
    ;   
    
case_expression
    :   CASE a=expression? case_list (ELSE b=expression)? END
        -> ^(TN_CASE $a? case_list ^(TN_CASE_ELSE $b)?)
    ;   
    
case_list
    :   case_part case_part* 
        -> ^(TN_CASE_PART case_part) ^(TN_CASE_PART case_part)*
    ;   
    
case_part
    :   WHEN column_expression THEN expression
        -> ^(TN_CASE_TEST column_expression) ^(TN_CASE_VALUE expression)
    ;   
    
coalesce_expression
    :   COALESCE LPAREN expression_list RPAREN
        -> ^(TN_COALESCE expression_list)
    ;

//The list of expressions to be evaluated for the Coalesce expression.  
expression_list
    :   expression COMMA expression (COMMA expression)*
        -> ^(TN_EXPR expression)*
    ;   
    
nullif_expression
    :   NULLIF LPAREN expression_list RPAREN
        -> ^(TN_NULLIF expression_list)
    ;   

//Set part for the Update statement.
set_part
    :   SET set_expr (COMMA set_expr)* 
        -> ^(TN_SET set_expr) ^(TN_SET set_expr)*
    ;

set_expr
    :   (object_name | host_variable) EQ (expression | NULL | (LPAREN select_statement RPAREN))     
    ;       

//List of columns.
column_list
    :   column_part (COMMA column_part)* -> ^(TN_COL column_part) ^(TN_COL column_part)*
    ;
    
//alt_name is the alias used for a column name. 
column_part
    :   (alt_name EQ )? column_expression ( (AS)? alt_name)? 
        -> ^(TN_COL_NAME alt_name)? ^(TN_COL_VALUE column_expression)
    ;
    
alt_name 
    :   a=object_name | constant
        -> $a? constant?
    ;       
    
column_expression
    :   a=identity_column   -> ^(SYB_ID $a)
    |   ((object_name DOT)? '*')
    |   expression      
    ;   

//Sets the identity column for a table. 
identity_column
    :   (object_name DOT)? SYB_IDENTITY (EQ expression)?
    ;

//The Select Into clause.   
into_clause
    :   INTO object_name
        (ON segment_name)?
        partition_clause?
            ( LOCK (DATAROWS | DATAPAGES | ALLPAGES)+ )?
            ( WITH into_option (COMMA into_option)*)?
            -> object_name
    ;

segment_name
    :   object_name
    ;
    
partition_name
    :   object_name
    ;
        
into_option
    :   ( MAX_ROWS_PER_PAGE | EXP_ROW_SIZE | RESERVEPAGEGAP | IDENTITY_GAP ) EQ INT_LIT
            (EXISTING TABLE simple_name)?
            ((EXTERNAL ( TABLE | FILE ))? AT STRING_LIT (COLUMN DELIMITER constant)?)?
    ;

partition_clause
    :   PARTITION BY ( 
        RANGE LPAREN column_list RPAREN partition_range_rule 
    |   HASH LPAREN column_list RPAREN partition_hash_rule 
    |   LIST LPAREN column_list RPAREN partition_list_rule 
    |   ROUNDROBIN (LPAREN column_list RPAREN)? partition_roundrobin_rule )
    ;

partition_range_rule
    :   LPAREN range_list RPAREN 
    ;
    
range_list
    :   range_part (COMMA range_part)*
    ;

range_part
    :   partition_name? VALUES LTE LPAREN value_list2 RPAREN (ON segment_name)?
    ;   
    
value_list2
    :   ( constant | MAX ) (COMMA ( constant | MAX ))*
    ;   

partition_hash_rule
    :   LPAREN hash_list RPAREN 
    |   number_of_partitions (ON LPAREN segment_list RPAREN)?
    ;
    
segment_list
    :   segment_name (COMMA segment_name)*
    ;   

number_of_partitions
    :   INT_LIT
    ;
    
hash_list
    :   hash_part (COMMA hash_part)*
    ;

hash_part
    :   partition_name (ON segment_name)?
    ;   

partition_list_rule
    :   LPAREN list_list RPAREN
    ;
    
list_list
    :   list_part (COMMA list_part)*
    ;   
    
list_part
    :   (partition_name)? VALUES LPAREN constant_list RPAREN (ON segment_name)?
    ;   
    
constant_list
    :   constant (COMMA constant)?
    ;   

partition_roundrobin_rule
    :   partition_hash_rule
    ;   

//The from clause for a Select statement.   
from_clause
    :   FROM join_list
        -> join_list
    ;
    
join_list
    :   join_factor (((join_type? JOIN) | COMMA) join_cond)*
        -> join_factor ^(TN_JOIN join_cond)*
    ;
    
join_type
    :   (INNER 
    |   LEFT (OUTER)? 
    |   RIGHT (OUTER)?)
    ;

join_cond
    :   join_factor on_clause?
    ;   
    
//This has been separated out to allow joins within parenthesis to be evaluated first.
join_factor
    :   a=table_view_name | (LPAREN join_list RPAREN)
        -> $a? join_list?
    ;   
    
table_view_name
    :   object_name (AS? simple_name)? READPAST? table_name_options?           
            -> ^(TN_TABLE_NAME simple_name)? ^(TN_TABLE_VALUE object_name) table_name_options?
    ;   
    
table_name_options
    :   (LPAREN system_options+ RPAREN)?
            (HOLDLOCK | NOHOLDLOCK)? 
            (READPAST)?
            (SHARED)?
            -> ^(SYB_INDEX system_options)?
    ;   
    
system_options
    :   a=index_part 
    |   (PARALLEL (degree_of_parallelism)?) 
    |   (PREFETCH prefetch_size) 
    |   (LRU | MRU)
        -> $a
    ;   
    
index_part
    :   INDEX simple_name
        -> simple_name
    ;       
    
degree_of_parallelism
    :   INT_LIT
    ;
    
prefetch_size   
    :   INT_LIT
    ;

//The on clause for the Join condition in a Select. 
on_clause
    :   ON column_expression
        -> ^(TN_ON column_expression)
    ;       

//The where clause for a Select statement.
where_clause
    :   WHERE column_expression -> column_expression
    |   WHERE CURRENT OF simple_name //Option for delete statement.
    ;   

//The group_by clause for a Select statement.   
group_by
    :   GROUP BY (ALL)? factor_list -> factor_list
    ;   
    
factor_list
    :   expression (COMMA expression)* -> expression expression*
    ;   
    
//The having clause for a Select statement. 
having_clause
    :   HAVING column_expression -> column_expression
    ;
    
//The union clause for a Select statement.  
union_clause
    :   UNION ALL? select_statement 
        -> ALL? select_statement
    ;
    
//The order_by clause for a Select statement.   
order_by
    :   ORDER BY order_list -> order_list
    ;
    
order_list
    :   order_part (COMMA order_part)* -> order_part order_part*
    ;   
    
order_part
    :   expression order_direction? 
    ;       
    
order_direction 
    :   ASC | DESC
    ;

//The compute clause for a Select statement.
compute_clause
    :   COMPUTE function_list 
            (BY column_list)?
    ;
    
function_list
    :   function LPAREN functionParams? RPAREN 
            (COMMA function LPAREN functionParams? RPAREN)*
    ;   

//The read only clause for a Select statement.  
read_only_clause
    :   FOR (READ ONLY | UPDATE (OF column_list)?)
    ;
    
//The isolation clause for a Select statement.  
isolation_clause
    :   AT? ISOLATION LEVEL? 
            ( READ UNCOMMITTED | READ COMMITTED | REPEATABLE READ | SERIALIZABLE | INT_LIT )
    ;
    
//The browse clause for a Select statement. 
browse_clause
    :   FOR BROWSE
    ;

//The plan clause specifies an abstract plan to be used to optimize a query.    
plan_clause
    :   PLAN STRING_LIT
    ;

set_statement
    :   SET (a=set_expr | (set_value set_options* on_off_part?))
        -> ^(SYB_SET $a? set_value? set_options* on_off_part?)
    ;
    
//Includes keywords which can appear as a set_value.    
set_value
    :   LOCK | PLAN | PREFETCH | TABLE | (TRANSACTION isolation_clause) | ID
    ;   

//Includes keywords which can appear as a set_option.   
set_options
    :   COMMA? ( (WITH PASSWD) | FOR | LONG | (ON ENDTRAN) | TIME | simple_factor )
    ;   
    
on_off_part
    :   OFF 
    |   (ON (WITH error_part)?) 
    |   (CHARSET (WITH error_part)?)
    |   DEFAULT
    ;   
    
error_part
    :   ERROR | NO_ERROR
    ;       
    
//plan_name, index_name, table_name, transaction_name, savepoint_name
simple_name
    :   ID
    ;
    
//Expressions in Sybase.    
expression
    :   simple_expression
    |   select_statement
    |   case_expression
    |   nullif_expression
    |   coalesce_expression
    ;   

//Used in simple expression.        
null_part
    :   (IS NOT?) NULL
    ;   
    
simple_expression
    :   and_expr (OR and_expr)*  
    ;   

and_expr 
    :   not_expr (AND not_expr)*
    ;
    
not_expr
    :   (NOT)? rel_expr null_part?
    ;   
    
rel_expr
    :   plus_expr (rel_op plus_expr)* (between_part | in_part)?
    ;   

plus_expr 
    :   mult_expr ((PLUS | MINUS | BITAND | BITOR | EXOR) mult_expr)*
    ;
    
mult_expr
    :   sign_expr ((MULT | DIVIDE | MODE) sign_expr)*
    ;
    
sign_expr
    :   (MINUS | PLUS | BITNOT)? factor 
    ;
    
between_part
    :   NOT? BETWEEN not_expr AND not_expr 
        -> NOT? ^(TN_BETWEEN not_expr not_expr)
    ;
    
in_part :   NOT? IN LPAREN functionParams? RPAREN -> NOT? ^(TN_IN functionParams?)
    ;       
    
factor  
    :   complex_factor //Keep this before simple_factor.
    |   simple_factor
    ;
    
simple_factor
    :   constant    
    |   host_variable
    |   object_name
    ;
    
complex_factor
    :   function LPAREN functionParams? RPAREN -> ^(TN_FUNC function functionParams?)
    |   LPAREN expression RPAREN
    ;           
    
constant:   INT_LIT
    |   FLOAT_LIT
    |   STRING_LIT
    |   NULL
    ;

//Locereas system-defined global variables start with '@@'. 
//The list includes global variable names that are also Sybase keywordsal variables start with '@' wh.
host_variable
    :   global_variable -> ^(SYB_GLOBAL_VAR global_variable)
    |   (STRUDEL (ERROR | IDENTITY | ISOLATION | object_name))  -> COLON[":"] ERROR? IDENTITY? ISOLATION? object_name?                                                                
    ;
    
global_variable
    :   (STRUDEL STRUDEL (ERROR | IDENTITY | ISOLATION | object_name)) -> COLON[":"] COLON ERROR? IDENTITY? ISOLATION? object_name?
    ;
    
//The list includes function names that are also Sybase keywords.   
function
    :   MAX | EXISTS | LEFT | RIGHT | OBJECT_NAME | IDENTITY | ANY | ALL | ID 
    ;
    
//The parameters for a function.    
functionParams
    :   (select_statement
    |   expression AS scalartype //The cast function uses this format.
    |   simple_function_param) //Keep this last always.
        -> ^(TN_PARAM select_statement? expression?)? simple_function_param?
    ;       
    
simple_function_param   
    :   function_param (COMMA function_param)* 
        -> ^(TN_PARAM function_param) ^(TN_PARAM function_param)*
    ;
    
function_param
    :   '*'
    |   expression 
    |   scalartype
    ;   
    
//Sybase operators.
    
arithmetic_op
    :   PLUS | MINUS | MULT | DIVIDE | MODE
    ;
    
rel_op  :   EQ | NEQ | GT | GTE | LT | LTE | NGT | NLT | (NOT? LIKE) | OUTJOIN
    ;
    
logical_op      
    :   NOT | AND | OR
    ;
    
bit_op
    :   NOT | AND | OR | EXOR
    ;
        
//Lexer 

BITAND  :   '&';
BITNOT  :   '~';
BITOR   :   '|';
COLON   :   ':'; 
COMMA   :   ',';
COMMENTEND
    :   '*/';
COMMENTSTART
    :   '/*';
DIVIDE  :   '/';
DOT :   '.'; 
EQ  :   '=';
EXOR    :   '^';
EXPO    :   '**'; //Not using EXP cos EXP is a function in Sybase.
EXPON   :   'e';
GT  :   '>';
GTE :   '>=';
LBRACE  :   '{';
LPAREN  :   '(';
LT  :   '<';
LTE :   ('<=' | '=<');
MINUS   :   '-';
MODE    :   '%';
MULT    :   '*';
NEQ :   ('<>' | '!=' | '^=');
NGT :   '!>';
NLT :   '!<';
OUTJOIN :   '*=' | '=*';
PLUS    :   '+';
RBRACE  :   '}';
RPAREN  :   ')';
SEMI    :   ';';
SPLUS   :   '||';
STRUDEL :   '@';
            
// Sybase data types.

int_type:   ((UNSIGNED)? (INTEGER | SMALLINT | INT | BIGINT)) 
    |   TINYINT     -> SYB_TINYINT
    ;
    
float_type
    :   FLOAT LPAREN INT_LIT RPAREN
    |   FLOAT                       -> SYB_FLOAT
    |   REAL | (DOUBLE PRECISION)
    |   DECIMAL LPAREN INT_LIT COMMA INT_LIT RPAREN -> SYB_DECIMAL LPAREN INT_LIT COMMA INT_LIT RPAREN
    |   DECIMAL LPAREN INT_LIT RPAREN           -> SYB_DECIMAL LPAREN INT_LIT COMMA[","] INT_LIT["0"] RPAREN //In Ingres, the default scale is 0.
    |   NUMERIC LPAREN INT_LIT COMMA INT_LIT RPAREN
    |   NUMERIC LPAREN INT_LIT RPAREN           -> NUMERIC LPAREN INT_LIT COMMA[","] INT_LIT["0"] RPAREN //In Ingres, the default scale is 0.
    |   (DECIMAL | NUMERIC)             -> SYB_DECIMAL
    ;
date_type
    :   DATE 
    |   TIME -> SYB_TIME 
    |   DATETIME | SMALLDATETIME | TIMESTAMP
    ;

string_type
    :   (CHAR | VARCHAR | UNICHAR | UNIVARCHAR | ambigous_string_types) (LPAREN INT_LIT RPAREN)?
    |   TEXT | UNITEXT
    ;   
    
ambigous_string_types 
    :       
        NCHAR    -> SYB_NCHAR 
    |   NVARCHAR -> SYB_NVARCHAR
    ;
    
binary_type
    :   (BINARY | VARBINARY) (LPAREN INT_LIT RPAREN)?
    |   IMAGE
    ;   
      
misc_type
    :   MONEY | SMALLMONEY | BIT
    ;   
    
user_type
    :   ID //-> ^(SYB_USER_TYPE ID)
    ;
            
//Sybase Keywords           

ALL :   A L L; 
ALLPAGES:   A L L P A G E S;
AND :   A N D | '&';
ANSIDATE:   A N S I D A T E;
ANY :   A N Y;
AS  :   A S;    
ASC     :   A S C;
AT  :   A T; 
BEGIN   :   B E G I N; 
BETWEEN :   B E T W E E N;
BIGINT  :   B I G I N T; 
BINARY  :   B I N A R Y;
BIT :   B I T;
BOOLEAN :   B O O L E A N;
BREAK   :   B R E A K;
BROWSE  :   B R O W S E;    
BY  :   B Y;
BYTE    :   B Y T E;  
CASE    :   C A S E;  
CHAR    :   C H A R;
CHARSET :   C H A R S E T;
COALESCE:   C O A L E S C E;
COLUMN  :   C O L U M N;
COMMIT  :   C O M M I T;
COMMITTED
    :   C O M M I T T E D;
COMPUTE :   C O M P U T E;
CONTINUE:   C O N T I N U E;
CREATE  :   C R E A T E;
CURRENT :   C U R R E N T;
DATA    :   D A T A;
DATAPAGES
    :   D A T A P A G E S;
DATAROWS:   D A T A R O W S;
DATE    :   D A T E;
DATETIME:   D A T E T I M E;
DEC :   D E C;
DECIMAL :   D E C I M A L;
DECLARE :   D E C L A R E;
DEFAULT :   D E F A U L T;
DELAY   :   D E L A Y;
DELETE  :   D E L E T E;
DELIMITER
    :   D E L I M I T E R;
DESC    :   D E S C;    
DETERMINISTIC
    :   D E T E R M I N I S T I C;  
DISTINCT:   D I S T I N C T;    
DOUBLE  :   D O U B L E;
DYNAMIC :   D Y N A M I C;   
ELSE    :   E L S E;
END :   E N D;
ENDTRAN :   E N D T R A N;
ERROR   :   E R R O R;
ERRORDATA
    :   E R R O R D A T A;
ERROREXIT  
    :   E R R O R E X I T;
EXEC    :   E X E C;  
EXECUTE :   E X E C U T E;  
EXISTING:   E X I S T I N G; 
EXISTS  :   E X I S T S;
EXP_ROW_SIZE
    :   E X P '_' R O W '_' S I Z E;
EXTERNAL:   E X T E R N A L; 
FILE    :   F I L E;
FLOAT   :   F L O A T;
FOR     :   F O R;
FROM    :   F R O M;
GOTO    :   G O T O;
GROUP   :   G R O U P;
HASH    :   H A S H;
HAVING  :   H A V I N G;
HOLDLOCK 
    :   H O L D L O C K;
IDENTITY:   I D E N T I T Y;
IDENTITY_GAP
    :   I D E N T I T Y '_' G A P;
IF  :   I F;
IMAGE   :   I M A G E;
IN  :   I N;
INNER   :   I N N E R; 
INDEX   :   I N D E X;
INOUT   :   I N O U T;
INSERT  :   I N S E R T;
INT :   I N T;
INTO    :   I N T O;
INTEGER :   I N T E G E R;
IS  :   I S;
ISOLATION
    :   I S O L A T I O N;
JAVA    :   J A V A;
JOIN    :   J O I N;
LEFT    :   L E F T;
LEVEL   :   L E V E L;
LIKE    :   L I K E;
LIST    :   L I S T;
LOCK    :   L O C K;   
LONG    :   L O N G;
LRU     :   L R U;
MAX :   M A X;
MAX_ROWS_PER_PAGE
    :   M A X '_' R O W S '_' P E R '_' P A G E;  
MIRROREXIT
    :   M I R R O R E X I T;    
MODIFIES:   M O D I F I E S;  
MONEY   :   M O N E Y;
MRU :   M R U;
NAME    :   N A M E;
NCHAR   :   N C H A R;
NO_ERROR:   N O '_' E R R O R;
NOHOLDLOCK
    :   N O H O L D L O C K;
NOT :   N O T | '~';
NULL    :   N U L L;
NULLIF  :   N U L L I F;
NUMERIC :   N U M E R I C;
NVARCHAR:   N V A R C H A R;
OBJECT_NAME
    :   O B J E C T '_' N A M E;
OF  :   O F;
OFF :   O F F;
ON  :   O N;
ONLY    :   O N L Y;
OR  :   O R | '|';
ORDER   :   O R D E R;
OUT :   O U T;
OUTER   :   O U T E R;
OUTPUT  :   O U T P U T;
PARALLEL:   P A R A L L E L;
PARAMETER 
    :   P A R A M E T E R;
PARTITION
    :   P A R T I T I O N;  
PASSWD  :   P A S S W D;    
PLAN    :   P L A N;
PRECISION
    :   P R E C I S I O N;
PREFETCH:   P R E F E T C H;
PRINT   :   P R I N T;  
PROC    :   P R O C;    
PROCEDURE 
    :   P R O C E D U R E; 
PROCESSEXIT
    :   P R O C E S S E X I T;  
RAISERROR
    :   R A I S E R R O R;  
RANGE   :   R A N G E;
READ    :   R E A D;
READPAST:   R E A D P A S T;
REAL    :   R E A L;
RECOMPILE
    :   R E C O M P I L E;
REPEATABLE
    :   R E P E A T A B L E;    
RESERVEPAGEGAP
    :   R E S E R V E P A G E G A P;    
RESULT  :   R E S U L T;
RETURN  :   R E T U R N;
RIGHT   :   R I G H T;
ROLLBACK:   R O L L B A C K;
ROUNDROBIN
    :   R O U N D R O B I N;
SAVE    :   S A V E;    
SELECT  :   S E L E C T;
SERIALIZABLE
    :   S E R I A L I Z A B L E;
SET :   S E T;  
SETS    :   S E T S;
SHARED  :   S H A R E D;
SHORT   :   S H O R T;
SMALLDATETIME
    :   S M A L L D A T E T I M E;
SMALLINT:   S M A L L I N T;
SMALLMONEY
    :   S M A L L M O N E Y;
SQL :   S Q L;
STYLE   :   S T Y L E;
SYB_IDENTITY
    :   S Y B '_' I D E N T I T Y;
TABLE   :   T A B L E;
TEXT    :   T E X T;
THEN    :   T H E N;
TIME    :   T I M E; 
TIMESTAMP : T I M E S T A M P;
TINYINT :   T I N Y I N T;
TOP :   T O P;
TRAN    :   T R A N;
TRANSACTION
    :   T R A N S A C T I O N;
TRUNCATE:   T R U N C A T E;    
UNCOMMITTED
    :   U N C O M M I T T E D;  
UNICHAR :   U N I C H A R; 
UNION   :   U N I O N;
UNITEXT :   U N I T E X T;
UNIVARCHAR
    :   U N I V A R C H A R;
UNSIGNED:   U N S I G N E D;
UPDATE  :   U P D A T E;
VALUES  :   V A L U E S;
VARBINARY
    :   V A R B I N A R Y;
VARCHAR :   V A R C H A R;
WAITFOR :   W A I T F O R;
WHEN    :   W H E N;
WHERE   :   W H E R E;
WHILE   :   W H I L E;
WITH    :   W I T H;     
WORK    :   W O R K;  


fragment ALPHANUM:  (ALPHA|'0'..'9');
fragment ALPHA  :   'a'..'z'|'A'..'Z'|'_' | '#' | '$';

fragment A:('a'|'A');
fragment B:('b'|'B');
fragment C:('c'|'C');
fragment D:('d'|'D');
fragment E:('e'|'E');
fragment F:('f'|'F');
fragment G:('g'|'G');
fragment H:('h'|'H');
fragment I:('i'|'I');
fragment J:('j'|'J');
fragment K:('k'|'K');
fragment L:('l'|'L');
fragment M:('m'|'M');
fragment N:('n'|'N');
fragment O:('o'|'O');
fragment P:('p'|'P');
fragment Q:('q'|'Q');
fragment R:('r'|'R');
fragment S:('s'|'S');
fragment T:('t'|'T');
fragment U:('u'|'U');
fragment V:('v'|'V');
fragment W:('w'|'W');
fragment X:('x'|'X');
fragment Y:('y'|'Y');
fragment Z:('z'|'Z');


FLOAT_LIT
    :   ('0'..'9')* DECIMAL_CHAR ('0'..'9')*
    ;   
INT_LIT 
    :   ('0'..'9')+ (EXPON ('0'..'9'))?
    ;   
    
//String literals in Sybase can be enclosed in single or double quotes. 
STRING_LIT
    :   SINGLE_STRINGDELIM (SINGLE_EMBEDDEDQUOTE | ~(SINGLE_STRINGDELIM))* SINGLE_STRINGDELIM
    |   DOUBLE_STRINGDELIM (DOUBLE_EMBEDDEDQUOTE | ~(DOUBLE_STRINGDELIM))* DOUBLE_STRINGDELIM
    ;   
    
fragment SINGLE_STRINGDELIM
    :    '\''
    ;
    
fragment DOUBLE_STRINGDELIM
    :   '"'
    ;           
    
//A single embedded quote is used when a single quote needs to be used inside a string literal 
//enclosed in single quotes.        
fragment SINGLE_EMBEDDEDQUOTE
    :   '\'\''
    ;

//A double embedded quote is used when a double quote needs to be used inside a string literal 
//enclosed in double quotes.    
fragment DOUBLE_EMBEDDEDQUOTE
    :   '""'
    ;   

fragment DECIMAL_CHAR
    :   DOT
    ;   
    
fragment EOS    
    :   ';'
    ;

ID  :   ALPHA (ALPHANUM)*
    |   '"' ALPHA (ALPHANUM)* '"'
    |       '[' ALPHA (ALPHANUM)* ']'
    ;

// We probably will want to preserve comments.
// TODO look at adding comments back into the rewrite tree.
WS  :   ('\t' | ' ' | '\r' | '\n' | '\u000C')   {$channel = HIDDEN;};
COMMENT :   '/*' ( options {greedy=false;} : .)* '*/' {$channel = HIDDEN;};
LINE_COMMENT
    :   '--' ~('\n'|'\r')* '\r'? '\n' {$channel = HIDDEN;};
