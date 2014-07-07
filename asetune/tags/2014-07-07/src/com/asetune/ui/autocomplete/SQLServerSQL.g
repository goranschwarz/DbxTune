grammar SQLServerSQL;

options {
    language=Java;
    output=AST;
    backtrack = true;
}

tokens {
        // Generic Section
        TN_ASSIGN;
        TN_BETWEEN;
        TN_CALL;
        TN_CODE;
        TN_COL;
        TN_COL_NAME;
        TN_COL_STAR;
        TN_COL_VALUE;
        TN_COLS;
        TN_COMMIT;
        TN_CREATESYNONYM;
        TN_DECLARE;
        TN_DELETE;
        TN_DROP_PROC;
        TN_ERROR;
        TN_ERROR_TOKEN;
        TN_EVENT_REGISTER;
        TN_EVENT_UNREGISTER;
        TN_EXITLOOP;
        TN_FROM;
        TN_FUNC;
        TN_GROUPBY;
        TN_HAVING;
        TN_IF;
        TN_IGNORE;
        TN_IN;
        TN_INSERT;
        TN_INTO;
        TN_JOIN;
        TN_LABEL;
        TN_LOCAL;
        TN_LOCALS;
        TN_MESSAGE;
        TN_NAME;
        TN_ON;
        TN_ORDERBY;
        TN_PARAM;
        TN_PARAM_COMMA;
        TN_PARAMS;
        TN_PARAMVARS;
        TN_PROCDEF;
        TN_RAISE;
        TN_RESULT;
        TN_RESULTS;
        TN_RESULTVARS;
        TN_RETURN;
        TN_RETURNROW;
        TN_RETURNVAR;
        TN_ROLLBACK;
        TN_ROOT;
        TN_SELECT;
        TN_SELECTLOOP;
        TN_SET;
        TN_SUBSTRING;
        TN_TEST;
        TN_TODO;
        TN_UNION;
        TN_UPDATE;
        TN_UPDATE_SETBLOCK;
        TN_WHERE;
        TN_WHILE;

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
        
        SQL_SRVR_CONTINUE;
        SQL_SRVR_METHOD;
        SQL_SRVR_TYPE;
        SQL_SRVR_INTO;
        SQL_SRVR_ARGUMENTS;
        SQL_SRVR_USER_TYPE;
        SQL_SRVR_LABEL;
        SQL_SRVR_GOTO;
        SQL_SRVR_WAITFOR;
        SQL_SRVR_BEGIN_END;
        SQL_SRVR_BEGIN_TRAN;
        SQL_SRVR_SQL_CALL;
        SQL_SRVR_SQL_PART;
        SQL_SRVR_ERRORDATA;
        SQL_SRVR_SET;
        SQL_SRVR_TRUNCATE;
        SQL_SRVR_DEFAULT;
        SQL_SRVR_WITH;
        SQL_SRVR_PIVOT;
        SQL_SRVR_GROUPBY;
        SQL_SRVR_COMPUTE;
        SQL_SRVR_FOR;
        SQL_SRVR_OPTION;
        SQL_SRVR_SAVETRANS;
        SQL_SRVR_DECLARE;
        SQL_SRVR_FETCH;
        SQL_SRVR_OPEN;
        SQL_SRVR_DEALLOCATE;
        SQL_SRVR_CLOSE;
        SQL_SRVR_OUTPUT;
        SQL_SRVR_INS_DEFAULT;
        SQL_SRVR_COLLATE;
        SQL_SERVER_HOSTVAR;
        SQL_SRVR_OUT;
        SQL_SRVR_GLOBAL_VAR;    
        SQL_SRVR_CAST;
}

@lexer::header {
//package uk.co.luminary.idiom.antlr/*NAMESPACE*/; 
package com.ingres.antlr.idiom.sqlserver;
/**
 * The SQLServer Lexer that takes an input stream of chars for a SQL Server 
 * DB proc and produces a token stream to send to the SQL Server Parser. 
 *
 * <!--                                                         
 * Class           : SQLServerLexer                                                                                                                  
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

@parser::header {
//package uk.co.luminary.idiom.antlr/*NAMESPACE*/; 
package com.ingres.antlr.idiom.sqlserver;
/**
 * The SQL Server Parser that takes a token stream provided by the SQLServerLexer
 * and parses the tokens, producing an Abstract Syntax Tree (AST). 
 *
 * <!--                                                         
 * Class           : SQLServerParser                                                                                                                   
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
 
// Rules


program :       procedure_def+;

//Procedure declaration.
procedure_def
        :       CREATE (PROCEDURE | PROC) procedure_name paramDeclBlock? 
                procedure_opt_list? 
                (FOR REPLICATION)?
                AS 
                (statement_list | (EXTERNAL NAME method_name)) SEMI?
                -> ^(TN_PROCDEF procedure_name 
                         ^(TN_PARAMS paramDeclBlock)?
                         ^(TN_CODE statement_list)?
                         ^(SQL_SRVR_METHOD method_name)?)
        ;
        
procedure_opt_list
        :       WITH procedure_options (COMMA WITH procedure_options)*
        ;
        
procedure_options
        :       ENCRYPTION
        |       RECOMPILE
        |       execute_as_clause 
        ;

execute_as_clause
        :       (EXEC | EXECUTE) AS (ID | STRING_LIT)
        ;       

//List of statements in a stored procedure.     
statement_list
        :       statement (SEMI? statement)* SEMI?
                -> statement+
        ;       
        
//Two types of execute: execute procedure and execute T-SQL command.
execute_statement
        :       (EXEC | EXECUTE)? ( procedure_execute | sql_execute )
                -> ^(TN_CALL procedure_execute)? 
                   ^(SQL_SRVR_SQL_CALL sql_execute)?
        ;
        
//Execute command can be used to execute procedures.    
procedure_execute
        :       (host_variable EQ)? procedure_name paramBlock? (WITH RECOMPILE)?
                -> procedure_name 
                        ^(TN_PARAMVARS paramBlock)?
                        ^(TN_RETURNVAR host_variable)?
        ;

//Execute command can be used to execute T-SQL commands by passing them in as string literals.  
sql_execute
        :       (LPAREN? sql_string (output_part)* RPAREN? 
                (AS ID EQ STRING_LIT)? 
                (AT object_name)?
        |       AS execute_as_part)
                -> (sql_string output_part*)? execute_as_part?
        ;

execute_as_part
        :       ID (EQ STRING_LIT)? (WITH ID ID? INTO? host_variable?)?
        ;
        
sql_string
        :       sql_part (PLUS sql_part)*
                -> ^(SQL_SRVR_SQL_PART sql_part) ^(SQL_SRVR_SQL_PART sql_part)*
        ;       
        
output_part
        :       COMMA (host_variable | constant) OUTPUT?
        ;               

//Part of the T-SQL command to be executed.     
sql_part
        :       STRING_LIT | host_variable
        ;

//Parameters for the procedure being executed by the Execute Procedure command.
paramBlock
        :       param_part (COMMA param_part)* 
                -> ^(TN_PARAM param_part) ^(TN_PARAM param_part)*
        ;       
        
param_part
        :       (host_variable assign_op)? ( param_value | (host_variable (OUTPUT | OUT)?))
        ;               

param_value
        :       constant | DEFAULT | ID
        ;       

//The name of the method of a .NET Framework assembly for a CLR stored procedure to reference.
method_name     
        :       object_name     
        ;
              
procedure_name          
        :       (object_name (SEMI (PLUS | MINUS)? INT_LIT)? 
        |       host_variable)
                -> object_name? host_variable?
        ;

//Object names can be of the form ID.ID.ID.ID   
object_name
        :       id name_part* 
                //(ID DOT)? ID  // This is the Ingres object_name. This difference causes problems in some cases.
                                // This is same for SQL Srvr and Oracle.
        ;
        
// Some keywords can be variable names based on context, here the object_name rule is split to allow ID plus common keywords, rewritten as IDs.
id      :       ID
        |       NAME ->ID[$NAME.text]
        ;       

//The parameters for the procedure being created.
paramDeclBlock
        :       LPAREN? paramset RPAREN? -> paramset
        ;       
        
paramset:       (param_name param_type) (COMMA param_name param_type)* 
                -> ^(TN_PARAM param_name param_type) ^(TN_PARAM param_name param_type)*  
        ;       
        
param_name
        :       host_variable | ID
        ;

//SQL Server types differ fron Ingres types. Hence the node SQL_SRVR_TYPE.      
param_type
        :       scalartype typeoption* 
        ;
        
//SQL Server types.     
scalartype
        :       (ID DOT)?
        (       int_type
        |       float_type
        |       string_type 
        |       binary_type
        |       date_type
        |       misc_type
        |       user_type )
        ;
        
typeoption
        :       (defaulttype)   -> ^(SQL_SRVR_DEFAULT defaulttype)
        |       (OUTPUT | OUT)  -> SQL_SRVR_OUT
        |       READONLY
        |       VARYING
        ;       

//Assigns a default value to a parameter.       
defaulttype
        :       assign_op? constant     -> constant
        ;
        
statement_block
        :       multistatement_block | singlestatement_block
        ;                                       

//Multiple statements in an If-Else statement or While loop should be enclosed with Begin - End.        
multistatement_block
        :       BEGIN statement (SEMI? statement)* SEMI? END -> ^(TN_CODE statement+)
        ;

singlestatement_block
        :       statement SEMI? -> ^(TN_CODE statement)
        ;
        
begin_end_statement
        :       BEGIN statement (SEMI? statement)* SEMI? END -> statement+
        ;       

//SQL Server statements. 
statement
        :       if_then_else_statement
        |       return_statement
        |       begin_end_statement
        |       begin_transaction_statement
        |       commit_statement 
        |       rollback_statement
        |       save_transaction_statement
        |       declare_statement
        |       print_statement
        |       break_statement
        |       continue_statement
        |       waitfor_statement
        |       raise_error_statement
        |       set_statement
        |       select_statement 
        |       goto_statement
        |       label_statement
        |       insert_statement
        |       delete_statement
        |       update_statement
        |       truncate_statement
        |       while_statement
        |       execute_statement
        |       fetch_statement
        |       open_statement
        |       deallocate_statement
        |       close_statement
        ;       
        
if_then_else_statement
        :       IF expression statement_block else_if_part* else_part?
                -> ^(TN_IF ^(TN_TEST expression) statement_block else_if_part* else_part?)
        ;
                
else_if_part
        :       ELSE IF expression statement_block
                -> ^(TN_IF ^(TN_TEST expression) statement_block)
        ;

else_part
        :       ELSE statement_block -> statement_block
        ;       
        
while_statement
        :       WHILE expression statement_block
                -> ^(TN_WHILE ^(TN_TEST expression) statement_block)
        ;       
        
break_statement
        :       BREAK
                -> ^(TN_EXITLOOP)
        ;       
        
continue_statement
        :       CONTINUE
                -> ^(SQL_SRVR_CONTINUE)
        ;       
        
begin_transaction_statement
        :       BEGIN (TRAN | TRANSACTION) (simple_name (WITH MARK STRING_LIT?)?)?
                -> ^(SQL_SRVR_BEGIN_TRAN simple_name?) 
        ;       
        
commit_statement
        :       COMMIT (TRAN | TRANSACTION | WORK)? simple_name? 
                -> ^(TN_COMMIT simple_name?)
        ;
        
rollback_statement
        :       ROLLBACK (TRAN | TRANSACTION | WORK)? simple_name?
                -> ^(TN_ROLLBACK simple_name?)
        ;               
                
save_transaction_statement
        :       SAVE (TRAN | TRANSACTION) simple_name
                -> ^(SQL_SRVR_SAVETRANS simple_name)
        ;               
                
return_statement
        :       RETURN (expression)?
                -> ^(TN_RETURN expression?)
        ;

//Goto label.   
goto_statement
        :       GOTO ID         -> ^(SQL_SRVR_GOTO ID)
        ;
        
//Label for the Goto statement. 
label_statement
        :       ID COLON        -> ^(SQL_SRVR_LABEL ID)
        ;       
        
//This rule does not cover all options of a SQL Server WaitFor statement.
waitfor_statement
        :       WAITFOR ( (DELAY waitfor_span) | (TIME waitfor_span) ) 
                -> ^(SQL_SRVR_WAITFOR waitfor_span?)
        ;       
        
waitfor_span
        :       STRING_LIT | host_variable
        ;       

//Declares local variables for a procedure.     
declare_statement
        :       DECLARE (param_name AS? scalartype) (COMMA param_name AS? scalartype)* 
                -> ^(SQL_SRVR_DECLARE 
                        ^(TN_PARAM param_name scalartype) 
                        ^(TN_PARAM param_name scalartype)* )
        ;
        
raise_error_statement
        :       RAISERROR LPAREN 
                                (error_number | print_message) 
                                (COMMA state_severity)+
                                (COMMA arg_list)? 
                          RPAREN
                          (WITH ID (COMMA ID)*)?
                -> ^(TN_ERROR error_number? print_message?
                        ^(SQL_SRVR_ERRORDATA state_severity+ arg_list?))
        ;       
        
state_severity  
        :       INT_LIT | host_variable
        ;

//The error number for the Raise Error statement.       
error_number
        :       INT_LIT
        ;       

//for_clause is used in a Select statement.
for_clause
        :       FOR (BROWSE | xml_clause)
        ;

xml_clause
        :       XML xml_part (COMMA xml_part)* 
        ;

xml_part
        :       ID ID? (LPAREN STRING_LIT RPAREN)?
        |       BINARY BASE64
        ;

//option_clause is used in Select, Delete and Update statements.
option_clause
        :       OPTION LPAREN query_hint_list RPAREN
        ;

query_hint_list
        :       query_hint (COMMA query_hint)*
        ;

query_hint
        :       query_keyword+ query_hint_opts?
        ;

query_keyword
        :       ORDER | GROUP | UNION | JOIN | FOR | PLAN | TABLE 
        |       ID | constant
        ;

query_hint_opts
        :       LPAREN optimize_for_opt RPAREN
        ;

optimize_for_opt
        :       optimize_value (COMMA optimize_value)*
        ;

optimize_value
        :       host_variable (ID | (EQ literal_constant))
        ;

literal_constant
        :       constant | ID
        ;       

//with_clause used in Select, Insert, Update and Delete statements.     
with_clause
        :       WITH common_table_expression (COMMA common_table_expression)*
        ;

common_table_expression
        :       object_name with_params? AS LPAREN select_statement RPAREN
        ;

with_params
        :       LPAREN object_name (COMMA object_name)* RPAREN
        ;       
        
query_expression
        :       query_part union_clause* -> query_part ^(TN_UNION union_clause)*
        ;               
        
query_part
        :        select_clause | (LPAREN query_expression RPAREN)+
        ;                       
        
select_clause
        :       SELECT ( ALL | DISTINCT )? top_part? column_list 
                into_clause?
                from_clause?
                where_clause?
                group_by?
                having_clause?          
                -> DISTINCT?
                        ^(TN_COLS column_list)
                        ^(SQL_SRVR_INTO into_clause)?
                        ^(TN_FROM from_clause)?
                        ^(TN_WHERE where_clause)?
                        ^(TN_GROUPBY group_by)?
                        ^(TN_HAVING having_clause)?
        ;       
        
top_part
        :       TOP LPAREN? expression RPAREN? PERCENT? (WITH TIES)?
        ;
                
//The union clause for a Select statement.      
union_clause
        :       ((UNION ALL?) | EXCEPT | INTERSECT) query_part
                -> ALL? ^(TN_SELECT query_part)
        ;       
        
//Write a message to screen. Same as Ingres Message statement.  
print_statement
        :       PRINT (print_part | (LPAREN print_part RPAREN))
                -> ^(TN_MESSAGE print_part)
        ;               
        
print_part
        :       print_message (COMMA arg_list)?
                -> print_message ^(SQL_SRVR_ARGUMENTS arg_list)?
        ;       
        
print_message
        :       message_part (PLUS message_part)* //String concatenation
        ;       
        
message_part
        :       STRING_LIT 
        |       host_variable
        |       expression
        ;       

//List of arguments for the Print statement or the Raise Error statement.
arg_list
        :       argument (COMMA argument)* -> argument argument*
        ;

argument
        :       host_variable | constant | ID
        |       expression
        ;       

//SQL Server Select.    
select_statement
        :       with_clause?
                query_expression 
                order_by?
                compute_clause*
                for_clause?
                option_clause?
                -> ^(TN_SELECT 
                        ^(SQL_SRVR_WITH with_clause)?
                        query_expression
                        ^(TN_ORDERBY order_by)?
                        ^(SQL_SRVR_COMPUTE compute_clause)*
                        ^(SQL_SRVR_FOR for_clause)?
                        ^(SQL_SRVR_OPTION option_clause)?)
        ;

insert_statement
        :       with_clause? INSERT top_part? INTO? crud_object 
                (insert_values | default_values)
                -> ^(TN_INSERT crud_object 
                        ^(SQL_SRVR_WITH with_clause)?
                        insert_values?
                        default_values?)
        ;       
        
insert_values
        :       column_list_option?
                output_clause?           
                ( value_part | select_statement | execute_statement)  
                -> ^(TN_COLS column_list_option)? 
                   ^(SQL_SRVR_OUTPUT output_clause)?
                   value_part? 
                   select_statement? 
                   execute_statement?
        ;       
        
default_values
        :       DEFAULT VALUES -> ^(SQL_SRVR_INS_DEFAULT)
        ;       
        
value_part
        :       VALUES LPAREN value_list RPAREN -> ^(TN_PARAMS value_list)
        ;       
        
//The list of values to be inserted.    
value_list
        :       simple_value (COMMA simple_value)*
                -> ^(TN_PARAM simple_value) ^(TN_PARAM simple_value)* 
        ;       
        
simple_value
        :       expression | DEFAULT
        ;
        
//SQL Server Delete.    
delete_statement
        :       with_clause? 
                DELETE 
                top_part? FROM? 
                crud_object 
                output_clause?
                from_clause? 
                where_clause? 
                option_clause?
                -> ^(TN_DELETE 
                        ^(SQL_SRVR_WITH with_clause)?
                        crud_object
                        ^(SQL_SRVR_OUTPUT output_clause)?       
                        ^(TN_FROM from_clause)?
                        ^(TN_WHERE where_clause)?
                        ^(SQL_SRVR_OPTION option_clause)?)
        ;       
                
//SQL Server Update.    
update_statement
        :       with_clause? UPDATE top_part? crud_object 
                set_part output_clause? from_clause? where_clause? option_clause?
                -> ^(TN_UPDATE 
                           ^(SQL_SRVR_WITH with_clause)?
                           crud_object
                           ^(TN_UPDATE_SETBLOCK set_part)
                           ^(SQL_SRVR_OUTPUT output_clause)?
                           ^(TN_FROM from_clause)?
                           ^(TN_WHERE where_clause)?
                           ^(SQL_SRVR_OPTION option_clause)?)                           
        ;
        
crud_object
        :       (host_variable | object_name | function_factor) with_option?
        ;

with_option
        :       WITH LPAREN table_hint+ RPAREN
        ;
        
output_clause
        :       OUTPUT output_column_list into_option?
        ;

output_column_list
        :       output_column_value (COMMA output_column_value)*
                -> output_column_value output_column_value*
        ;
        
output_column_value
        :       (column_name | expression) (AS? alt_name)? 
        ;

column_name
        :       object_name ('*')?
        ;       

into_option
        :       INTO (host_variable | object_name) column_list_option?
        ;       
        
column_list_option
        :       LPAREN column_list RPAREN -> column_list
        ;       
        
truncate_statement
        :       TRUNCATE TABLE object_name 
                -> ^(SQL_SRVR_TRUNCATE object_name)
        ;       
        
case_expression
        :       CASE a=expression? case_list (ELSE b=expression)? END
                -> ^(TN_CASE $a? case_list ^(TN_CASE_ELSE $b)?)
        ;       
        
case_list
        :       case_part case_part* 
                -> ^(TN_CASE_PART case_part) ^(TN_CASE_PART case_part)*
        ;       
        
case_part
        :       WHEN column_expression THEN expression
                -> ^(TN_CASE_TEST column_expression) ^(TN_CASE_VALUE expression)
        ;       
        
coalesce_expression
        :       COALESCE LPAREN expression_list RPAREN
                -> ^(TN_COALESCE expression_list)
        ;

//The list of expressions to be evaluated for the Coalesce expression.  
expression_list
        :       expression (COMMA expression)*
                -> ^(TN_EXPR expression)*
        ;       
        
nullif_expression
        :       NULLIF LPAREN expression_list RPAREN
                -> ^(TN_NULLIF expression_list)
        ;       

//Set part for the Update statement.
set_part
        :       SET set_expr (COMMA set_expr)* 
                -> ^(TN_SET set_expr) ^(TN_SET set_expr)*
        ;

set_expr
        :       expression
        |       ((object_name | host_variable) assign_op)? set_expr_value 
        ;       
        
set_expr_value
        :       STRUDEL? ID (DOT | '::') function_factor  
        ;       

//List of columns.
column_list
        :       column_part (COMMA column_part)* -> ^(TN_COL column_part) ^(TN_COL column_part)*
        ;
        
//alt_name is the alias used for a column name. 
column_part
        :       (alt_name EQ )? (object_name DOT)? '*' ( AS? alt_name)? -> ^(TN_COL_NAME alt_name)? ^(TN_COL_STAR object_name?)
        |       (alt_name EQ )? column_expression ( AS? alt_name)? -> ^(TN_COL_NAME alt_name)? ^(TN_COL_VALUE column_expression)
        ;
        
alt_name 
        :       a=object_name | b=constant | c=host_variable
                -> $a? $b? $c?
        ;               
        
column_expression
        :       
            object_name method_part
        |       expression
        ;       
        
//Used in the Select statement. 
method_part
        :       '::' ( function_factor | object_name ) //Keep function_factor before object_name.
        ;

//The Select Into clause.       
into_clause
        :       INTO object_name
                -> object_name
        ;

//The from clause for a Select statement.       
from_clause
        :       FROM join_list
                -> join_list
        ;
        
join_list
        :       join_factor join_start*
                
        ;
        
join_start
        :       from_option join_factor on_clause?
                -> ^(TN_JOIN join_factor on_clause?)
        ;       
        
from_option
        :       (apply_type APPLY) | (join_type? JOIN) | COMMA | PIVOT | UNPIVOT
        ;       

apply_type
        :       OUTER | CROSS
        ;
        
join_type
        :       (INNER | CROSS | ((LEFT | RIGHT | FULL) OUTER?)) ID?
        ;       
        
//This has been separated out to allow joins within parenthesis to be evaluated first.
join_factor
        :       a=table_view_name | (LPAREN join_list RPAREN)
                -> $a? join_list?
        ;       
        
table_view_name
        :       table_view_value (AS? simple_name)? table_name_options? table_column_options?       
                -> table_view_value simple_name?                   
                   table_column_options?
                   table_name_options?
        ;
        
table_view_value
        :       (LPAREN table_value_part RPAREN          
        |       function_factor 
        |       pivot_clause                    
        |       expression)
                -> table_value_part? function_factor? ^(SQL_SRVR_PIVOT pivot_clause)? expression?
        ;       
        
table_value_part
        :       table_value_constructor
        |       select_statement
        ;       

table_value_constructor
        :       VALUES table_value_list (COMMA table_value_list)*
        ;

table_value_list
        :       LPAREN value_list RPAREN
        ;       

table_column_options
        :       LPAREN column_alias_list RPAREN
                -> column_alias_list
        ;

column_alias_list
        :       column_alias (COMMA column_alias)*
                -> column_alias column_alias*
        ;

column_alias
        :       alt_name
        ;       
        
table_name_options
        :       table_sample_clause
        |       WITH LPAREN table_hint (COMMA? table_hint)* RPAREN 
        ;

table_sample_clause
        :       TABLESAMPLE (SYSTEM)? (LPAREN expression (PERCENT | ROWS)? RPAREN) 
                (REPEATABLE LPAREN expression RPAREN)? 
        ;       
        
table_hint
        :       (NOEXPAND)? (INDEX (LPAREN index_list RPAREN) | (EQ simple_factor)
                            | ID | SERIALIZABLE )
        ;       
        
index_list
        :        simple_factor (COMMA simple_factor)*
        ;
        
pivot_clause
        :       LPAREN pivot_value RPAREN
                -> pivot_value
        ;       
        
pivot_value
        :       (function_factor | ID) FOR ID IN LPAREN column_list RPAREN 
        ;

//The on clause for the Join condition in a Select.     
on_clause
        :       ON column_expression
                -> ^(TN_ON column_expression)
        ;               

//The where clause for a Select statement.
where_clause
        :       WHERE column_expression -> column_expression
        |       WHERE CURRENT OF variable_part //Option for delete statement.
        ;       

//The group_by clause for a Select statement.   
group_by
        :       GROUP BY (ALL)? factor_list (WITH (CUBE | ROLLUP))?-> factor_list
        ;       
        
factor_list
        :       expression (COMMA expression)* -> expression expression*
        ;               
        
//The having clause for a Select statement.     
having_clause
        :       HAVING column_expression -> column_expression
        ;
        
//The order_by clause for a Select statement.   
order_by
        :       ORDER BY order_list -> order_list
        ;
        
order_list
        :       order_part (COMMA order_part)* -> order_part order_part*
        ;       
        
order_part
        :       expression order_direction? 
        ;               
        
order_direction 
        :       ASC | DESC
        ;

//The compute clause for a Select statement.
compute_clause
        :       COMPUTE function_list 
                (BY column_list)?
        ;
        
function_list
        :       function_factor (COMMA function_factor)*
        ;
        
//The isolation clause for a Select statement.  
transaction_clause
        :       TRANSACTION ISOLATION LEVEL
                ( (READ UNCOMMITTED)
                | (READ COMMITTED)
                | (REPEATABLE READ)
                | SERIALIZABLE
                | SNAPSHOT )
        ;

set_statement
        :       /*(SET (set_value set_options* on_off_part?))   -> ^(SQL_SRVR_SET set_value set_options* on_off_part?)
        |       set_assign*/
                SET ( (set_value set_options* on_off_part?) | a=set_expr)       
                -> ^(SQL_SRVR_SET $a? set_value? set_options* on_off_part?) 
        ;
        
set_assign
        :       SET host_variable EQ expression         
                -> ^(TN_ASSIGN host_variable expression)
        ;
        
//Includes keywords which can appear as a set_value.    
set_value
        :       LOCK | PLAN | PREFETCH | TABLE | transaction_clause | ID
        ;               

//Includes keywords which can appear as a set_option.   
set_options
        :       COMMA? ( (WITH PASSWD) | FOR | (ON ENDTRAN) | TIME | XML | simple_factor_part )
        ;

simple_factor_part
        :       (PLUS | MINUS)? simple_factor
        ;       
        
on_off_part
        :       OFF 
        |       (ON (WITH error_part)?) 
        |       (CHARSET (WITH error_part)?)
        ;       
        
error_part
        :       ERROR | NO_ERROR
        ;               
        
//plan_name, index_name, table_name, transaction_name, savepoint_name
simple_name
        :       object_name | host_variable
        ;
        
//Expressions in SQL Server.    
expression
        :       simple_expression collation_clause?
        |       select_statement
        ;

collation_clause
        :       COLLATE ID -> ^(SQL_SRVR_COLLATE)
        ;       

//Used in simple expression.            
null_part
        :       IS NOT? NULL
        ;       
        
simple_expression
        :       and_expr ((OR | (NOT? LIKE)) and_expr)* 
        ;       

and_expr 
        :       not_expr (AND not_expr)*
        ;
        
not_expr
        :       (NOT)? rel_expr null_part?
        ;       
        
rel_expr
        :       plus_expr (rel_op plus_expr)*
        ;       

plus_expr 
        :       mult_expr ((PLUS | MINUS | BITAND | BITOR | EXOR) mult_expr)*
        ;
        
mult_expr
        :       sign_expr ((MULT | DIVIDE | MODE) sign_expr)*
        ;
        
sign_expr
        :       (MINUS | PLUS | BITNOT)? factor (between_part | in_part)? 
        ;
        
between_part
        :       NOT? BETWEEN not_expr AND not_expr 
                -> ^(TN_BETWEEN NOT? ^(TN_PARAM not_expr) ^(TN_PARAM not_expr))
        ;
        
in_part :       NOT? IN LPAREN functionParams? RPAREN -> ^(TN_IN NOT? functionParams?)
        ;               
        
factor  
        :       complex_factor //Keep this before simple_factor.
        |       simple_factor
        ;
        
simple_factor
        :       constant
        |       global_variable -> ^(SQL_SRVR_GLOBAL_VAR global_variable)
        |       host_variable
        |       object_name
        ;
        
complex_factor
        :       case_expression
        |       nullif_expression
        |       coalesce_expression 
        |       function_factor
        |       LPAREN expression RPAREN
        ;       
        
function_factor
        :       CAST LPAREN expression AS scalartype RPAREN -> ^(SQL_SRVR_CAST CAST LPAREN expression AS scalartype RPAREN)
        |       function LPAREN functionParams? RPAREN name_part* with_part?
                        -> ^(TN_FUNC function functionParams? name_part* with_part?)
        ;       
        
with_part
        :       WITH (paramDeclBlock | ID)
        ;
        
name_part
        :       DOT ID?
        ;                       
        
constant:       INT_LIT
        |       FLOAT_LIT
        |       STRING_LIT
        |       MONEY_LIT
        |       NULL
        ;

//Local variables start with '@' whereas system-defined global variables start with '@@'. 
//The list includes global variable names that are also SQL Server keywords.
host_variable
        :       STRUDEL (ERROR | ISOLATION | object_name)       
                                                // 'Error' and 'Isolation' can be used as object names, as these are 
                                                // not reserved keywords in SQLServer.
                                                // This 'Error' should be moved to object_name rule (as 'ERROR | ID'), 
                                                // along with any other tokens that may be used as object_names.
        ;
        
global_variable
        :       STRUDEL STRUDEL (ERROR -> ERROR | object_name -> object_name)
        ;
        
//The list includes function names that are also SQL Server keywords.   
function
        :       CUBE | ROLLUP | MAX -> ID["MAX"] | EXISTS | LEFT | RIGHT | OBJECT_NAME | ANY | ALL | SOME
        |       CHAR | NCHAR | UPDATE
        |       object_name 
        ;
        
//The parameters for a function.        
functionParams
        :       function_param (COMMA? SEMI? function_param)* 
                -> ^(TN_PARAM function_param) ^(TN_PARAM function_param)*
        ;
        
function_param
        :       '*' -> TN_COL_STAR
        |       DISTINCT
        |       select_statement
        |       expression (AS scalartype)?     //The cast function uses 'AS scalartype'.
        |       scalartype                      //Keep in this order always.
        ;       
        
//Cursor commands.
fetch_statement
        :       FETCH (fetch_part? FROM)? variable_part (into_part)?
                -> ^(SQL_SRVR_FETCH)
        ;

fetch_part
        :       ID ( ((PLUS | MINUS)? INT_LIT) | host_variable)?
        ;

into_part
        :       INTO host_variable (COMMA host_variable)*
        ;
        
open_statement
        :       OPEN variable_part
                -> ^(SQL_SRVR_OPEN)
        ;
        
deallocate_statement
        :       DEALLOCATE variable_part
                -> ^(SQL_SRVR_DEALLOCATE)
        ;
        
close_statement
        :       CLOSE variable_part
                -> ^(SQL_SRVR_CLOSE)
        ;
        
variable_part
        :       GLOBAL? object_name
        |       host_variable
        ;                                       
        
//SQL Server operators.
        
arithmetic_op
        :       PLUS | MINUS | MULT | DIVIDE | MODE
        ;
        
rel_op  :       EQ | NEQ | GT | GTE | LT | LTE | NGT | NLT 
        |       compound_op     
        ;
        
compound_op
        :       BITANDEQ | BITOREQ | DIVIDEEQ | EXOREQ | MINUSEQ | MODEEQ | MULTEQ | PLUSEQ
        ;       
        
logical_op              
        :       NOT | AND | OR
        ;
        
bit_op
        :       NOT | AND | OR | EXOR
        ;
        
assign_op
        :       EQ | compound_op
        ;       
                
//Lexer 

BITAND  :       '&';
BITANDEQ
        :       '&=';
BITNOT  :       '~';
BITOR   :       '|';
BITOREQ :       '|=';
COLON   :       ':'; 
COMMA   :       ',';
COMMENTEND
        :       '*/';
COMMENTSTART
        :       '/*';
DIVIDE  :       '/';
DIVIDEEQ
        :       '/=';
DOT     :       '.'; 
EQ      :       '=';
EXOR    :       '^';
EXOREQ  :       '^=';
EXPO    :       '**'; //Not using EXP cos EXP is a function in SQL Server.
EXPON   :       'e';
GT      :       '>';
GTE     :       '>=';
LBRACE  :       '{';
LPAREN  :       '(';
LT      :       '<';
LTE     :       '<=';
MINUS   :       '-';
MINUSEQ :       '-=';
MODE    :       '%';
MODEEQ  :       '%=';
MULT    :       '*';
MULTEQ  :       '*=';
NEQ     :       ('<>' | '!=');
NGT     :       '!>';
NLT     :       '!<';
PLUS    :       '+';
PLUSEQ  :       '+=';
RBRACE  :       '}';
RPAREN  :       ')';
SEMI    :       ';';
SPLUS   :       '||';
STRUDEL :       '@';
                
// SQL Server data types.

int_type:       SMALLINT | INT | BIGINT | TINYINT | INTEGER
        ;
float_type
        :       (FLOAT (LPAREN INT_LIT RPAREN)?) | REAL 
        |       (DECIMAL | NUMERIC) (LPAREN INT_LIT (COMMA INT_LIT)? RPAREN)?
        ;
date_type
        :       DATETIME | SMALLDATETIME | TIMESTAMP
        ;

string_type
        :       (CHAR | NCHAR) (LPAREN INT_LIT RPAREN)?
        |       (VARCHAR | NVARCHAR) (LPAREN (INT_LIT | MAX) RPAREN)?
        |       TEXT | NTEXT 
        ;       
        
binary_type
        :       (BINARY | VARBINARY) (LPAREN (INT_LIT | MAX) RPAREN)?
        |       IMAGE
        ;       
          
//Datatype 'table' has not been included here as it doesn't seem to be used much in Ingres.
misc_type
        :       MONEY | SMALLMONEY | BIT
        |       CURSOR | UNIQUEIDENTIFIER | SQL_VARIANT
        |       XML (LPAREN object_name+ RPAREN)?
        ;       
        
user_type
        :       ID -> ^(SQL_SRVR_USER_TYPE ID)
        ;
                
//SQL Server Keywords           

ALL     :       A L L; 
AND     :       A N D | '&';
ANY     :       A N Y;
APPLY   :       A P P L Y;
AS      :       A S;    
ASC     :       A S C;
AT      :       A T; 
BASE64  :       B A S E '6' '4';
BEGIN   :       B E G I N; 
BETWEEN :       B E T W E E N;
BIGINT  :       B I G I N T; 
BINARY  :       B I N A R Y;
BIT     :       B I T;
BOOLEAN :       B O O L E A N;
BREAK   :       B R E A K;
BROWSE  :       B R O W S E;    
BY      :       B Y;
BYTE    :       B Y T E;  
CASE    :       C A S E;
CAST    :       C A S T;  
CHAR    :       C H A R;
CHARSET :       C H A R S E T;
CLOSE   :       C L O S E;
COALESCE:       C O A L E S C E;
COLLATE :       C O L L A T E;
COMMIT  :       C O M M I T;
COMMITTED
        :       C O M M I T T E D;
COMPUTE :       C O M P U T E;
CONTINUE:       C O N T I N U E;
CREATE  :       C R E A T E;
CROSS   :       C R O S S;
CUBE    :       C U B E;
CURRENT :       C U R R E N T;
CURSOR  :       C U R S O R;
DATETIME:       D A T E T I M E;
DEALLOCATE
        :       D E A L L O C A T E;
DECIMAL :       D E C I M A L;
DECLARE :       D E C L A R E;
DEFAULT :       D E F A U L T;
DELAY   :       D E L A Y;
DELETE  :       D E L E T E;
DESC    :       D E S C;        
DISTINCT:       D I S T I N C T;
ELSE    :       E L S E;
ENCRYPTION
        :       E N C R Y P T I O N;
END     :       E N D;
ENDTRAN :       E N D T R A N;
ERROR   :       E R R O R;
EXCEPT  :       E X C E P T;
EXEC    :       E X E C;  
EXECUTE :       E X E C U T E; 
EXISTS  :       E X I S T S;
EXTERNAL:       E X T E R N A L; 
FETCH   :       F E T C H;
FLOAT   :       F L O A T;
FOR     :       F O R;
FROM    :       F R O M;
FULL    :       F U L L;
GLOBAL  :       G L O B A L;
GOTO    :       G O T O;
GROUP   :       G R O U P;
HAVING  :       H A V I N G;
IF      :       I F;
IMAGE   :       I M A G E;
IN      :       I N;
INNER   :       I N N E R; 
INDEX   :       I N D E X;
INSERT  :       I N S E R T;
INT     :       I N T;
INTEGER
        :       I N T E G E R;
INTERSECT
        :       I N T E R S E C T;
INTO    :       I N T O;
IS      :       I S;
ISOLATION
        :       I S O L A T I O N;
JOIN    :       J O I N;
LEFT    :       L E F T;
LEVEL   :       L E V E L;
LIKE    :       L I K E;
LOCK    :       L O C K;   
LONG    :       L O N G;
MARK    :       M A R K;
MAX     :       M A X;  
MONEY   :       M O N E Y;
NAME    :       N A M E;
NCHAR   :       N C H A R;
NO_ERROR:       N O '_' E R R O R;
NOEXPAND:       N O E X P A N D;
NOT     :       N O T | '~';
NTEXT   :       N T E X T;
NULL    :       N U L L;
NULLIF  :       N U L L I F;
NUMERIC :       N U M E R I C;
NVARCHAR:       N V A R C H A R;
OBJECT_NAME
        :       O B J E C T '_' N A M E;
OF      :       O F;
OFF     :       O F F;
ON      :       O N;
OPEN    :       O P E N;
OPTION  :       O P T I O N;
OR      :       O R | '|';
ORDER   :       O R D E R;
OUT     :       O U T;
OUTER   :       O U T E R;
OUTPUT  :       O U T P U T;    
PASSWD  :       P A S S W D;    
PERCENT :       P E R C E N T;
PIVOT   :       P I V O T;
PLAN    :       P L A N;
PREFETCH:       P R E F E T C H;
PRINT   :       P R I N T;      
PROC    :       P R O C;        
PROCEDURE 
        :       P R O C E D U R E;      
RAISERROR
        :       R A I S E R R O R;
READ    :       R E A D;        
READONLY:       R E A D O N L Y;
REAL    :       R E A L;
RECOMPILE
        :       R E C O M P I L E;
REPEATABLE
        :       R E P E A T A B L E;
REPLICATION
        :       R E P L I C A T I O N;  
RETURN  :       R E T U R N;
RIGHT   :       R I G H T;
ROLLBACK:       R O L L B A C K;
ROLLUP  :       R O L L U P;
ROWS    :       R O W S;        
SAVE    :       S A V E;        
SELECT  :       S E L E C T;
SERIALIZABLE
        :       S E R I A L I Z A B L E;
SET     :       S E T;
SHORT   :       S H O R T;
SMALLDATETIME
        :       S M A L L D A T E T I M E;
SMALLINT:       S M A L L I N T;
SMALLMONEY
        :       S M A L L M O N E Y;
SNAPSHOT:       S N A P S H O T;
SOME    :       S O M E;
SQL_VARIANT
        :       S Q L '_' V A R I A N T;
SYSTEM  :       S Y S T E M;    
TABLE   :       T A B L E;
TABLESAMPLE 
        :       T A B L E S A M P L E;
TEXT    :       T E X T;
THEN    :       T H E N;
TIES    :       T I E S;
TIME    :       T I M E; 
TIMESTAMP :     T I M E S T A M P;
TINYINT :       T I N Y I N T;
TOP     :       T O P;
TRAN    :       T R A N;
TRANSACTION
        :       T R A N S A C T I O N;
TRUNCATE:       T R U N C A T E;        
UNCOMMITTED
        :       U N C O M M I T T E D;  
UNION   :       U N I O N;
UNIQUEIDENTIFIER
        :       U N I Q U E I D E N T I F I E R;
UNPIVOT :       U N  P I V O T; 
UPDATE  :       U P D A T E;
VALUES  :       V A L U E S;
VARBINARY
        :       V A R B I N A R Y;
VARCHAR :       V A R C H A R;
VARYING :       V A R Y I N G;
WAITFOR :       W A I T F O R;
WHEN    :       W H E N;
WHERE   :       W H E R E;
WHILE   :       W H I L E;
WITH    :       W I T H;     
WORK    :       W O R K;  
XML     :       X M L;

ID      :       ALPHA (ALPHANUM)*
        |       '"' ( ~('"'))* '"'
        |       '[' ( ~(']'))* ']'
        ;
fragment ALPHANUM:      (ALPHA|'0'..'9');
fragment ALPHA  :       'a'..'z' | 'A'..'Z' | '_' | '#' | '$';

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
        :       MINUS? ('0'..'9')+ DECIMAL_CHAR ('0'..'9')+ (EXPON MINUS? ('0'..'9')+)?;
        
INT_LIT :       MINUS? ('0'..'9')+ (EXPON MINUS? ('0'..'9')+)?;
        
MONEY_LIT
        :       SYMBOL (FLOAT_LIT | INT_LIT)
        ;

//This can be any money sign, but only following options are included here. 
//See 'Using Monetary Data' in SQL Server 2005 Books Online.
SYMBOL  :       '$' | '£'
        ;       
        
//String literals in SQL Server can be enclosed in single or double quotes.     
STRING_LIT
        :       'N'? SINGLE_STRINGDELIM (SINGLE_EMBEDDEDQUOTE | ~(SINGLE_STRINGDELIM))* SINGLE_STRINGDELIM
        |       'N'? DOUBLE_STRINGDELIM (DOUBLE_EMBEDDEDQUOTE | ~(DOUBLE_STRINGDELIM))* DOUBLE_STRINGDELIM
        ;       
        
fragment SINGLE_STRINGDELIM
        :        '\''
        ;
        
fragment DOUBLE_STRINGDELIM
        :       '"'
        ;                       
        
//A single embedded quote is used when a single quote needs to be used inside a string literal 
//enclosed in single quotes.            
fragment SINGLE_EMBEDDEDQUOTE
        :       '\'\''
        ;

//A double embedded quote is used when a double quote needs to be used inside a string literal 
//enclosed in double quotes.    
fragment DOUBLE_EMBEDDEDQUOTE
        :       '""'
        ;       

fragment DECIMAL_CHAR
        :       DOT
        ;       
        
fragment EOS    
        :       ';'
        ;

// We probably will want to preserve comments.
// TODO look at adding comments back into the rewrite tree.
WS      :       ('\t' | ' ' | '\r' | '\n' | '\u000C')   {$channel = HIDDEN;};
COMMENT :       '/*' ( options {greedy=false;} : .)* '*/' {$channel = HIDDEN;};
LINE_COMMENT
        :       '--' ~('\n'|'\r')* '\r'? '\n' {$channel = HIDDEN;};
