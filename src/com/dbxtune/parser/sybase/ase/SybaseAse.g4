grammar SybaseAse;


@header
{
package com.dbxtune.parser.sybase.ase;
}



program 
    :   procedureDef
    |   statement+
    ;

//Procedure declaration.
procedureDef
    :   CREATE (PROCEDURE | PROC) procedureName paramDeclBlock? (WITH RECOMPILE)? AS 
        (statementList | (EXTERNAL NAME dllName)) 
 //       -> ^(TN_PROCDEF procedureName 
 //                    ^(TN_PARAMS paramDeclBlock)?
 //                    ^(TN_CODE statementList)?
 //                    ^(SYB_DLL dllName)?)
    ;

//List of statements in a stored procedure. 
statementList
    :   statement (SEMI? statement)* SEMI?
 //       -> statement+
    ;   
        
executeStatement
    :   (EXEC | EXECUTE)?  (procedureExecute | sqlExecute)
 //       -> ^(TN_CALL procedureExecute)? 
 //          ^(SYB_SQL_CALL sqlExecute)?
    ;
    
procedureExecute
    :   (hostVariable EQ)? procedureName paramBlock? (WITH RECOMPILE)?
 //       -> procedureName 
 //           ^(TN_PARAMVARS paramBlock)?
 //                   ^(TN_RETURNVAR hostVariable)?
    ;

//Execute command can be used to execute T-SQL commands by passing them in as string literals.  
sqlExecute
    :   LPAREN? sqlPart (PLUS sqlPart)* RPAREN? 
 //       -> ^(SYB_SQL_PART sqlPart) ^(SYB_SQL_PART sqlPart)*
    ;   

//Part of the T-SQL command to be executed. 
sqlPart
    :   STRING_LIT | hostVariable
    ;

//Parameters for the procedure being executed by the Execute Procedure command.
paramBlock
    :   paramPart (COMMA paramPart)* 
 //       -> ^(TN_PARAM paramPart) ^(TN_PARAM paramPart)*
    ;   
    
paramPart
    :   (hostVariable EQ)? ( paramValue | (hostVariable (OUTPUT | OUT)?))
    ;       

paramValue
    :   constant | ID
    ;   

//The name of the dynamic link library (DLL) or shared library containing the functions that implement 
//the extended stored procedure.
dllName    
    :   ID
    |   STRING_LIT  
    ;
          
procedureName          
    :   objectName (SEMI (PLUS | MINUS)? INT_LIT)? //-> objectName
    ;

//Object names can be of the form ID.ID.ID.ID  Done as ID.ID for now. More causes errors.   
objectName
    :   (ID DOT)? ID
    ;   

//The parameters for the procedure being created.
paramDeclBlock
    :   (LPAREN)? paramset (RPAREN)? //-> paramset
    ;   
    
paramset:   (paramName paramType) (COMMA paramName paramType)* 
//        -> ^(TN_PARAM paramName paramType) ^(TN_PARAM paramName paramType)*  
    ;   
    
paramName
    :   (STRUDEL objectName | STRUDEL STRUDEL objectName) //-> objectName 
    ;

//Sybase types differ fron Ingres types. Hence the node SYB_TYPE.   
paramType
    :   scalartype typeoption* //-> ^(SYB_TYPE scalartype) typeoption*
    ;
    
//Sybase types. 
scalartype
    :   intType
    |   floatType
    |   stringType 
    |   binaryType
    |   dateType
    |   miscType
    |   userType
    ;
    
typeoption
    :   (defaulttype)   //-> ^(SYB_DEFAULT defaulttype)
    |   (OUTPUT | OUT)
    ;   

//Assigns a default value to a parameter.   
defaulttype
    :   EQ constant //-> constant
    ;
    
statementBlock
    :   multistatementBlock | singlestatementBlock
    ;                   

//Multiple statements in an If-Else statement or While loop should be enclosed with Begin - End.    
multistatementBlock
    :   BEGIN statement (SEMI? statement)* SEMI? END //-> ^(TN_CODE statement+)
    ;

singlestatementBlock
    :   statement SEMI? //-> ^(TN_CODE statement) 
    ;
    
beginEndStatement
    :   multistatementBlock
        //-> ^(SYB_BEGIN_END multistatementBlock)
    ;   

//Sybase statements. 
statement
    :   ifThenElseStatement
    |   returnStatement
    |   beginTransactionStatement
    |   commitStatement 
    |   rollbackStatement
    |   saveTransactionStatement
    |   declareStatement
    |   printStatement
    |   breakStatement
    |   continueStatement
    |   gotoStatement
    |   labelStatement
    |   waitforStatement
    |   beginEndStatement
    |   raiseErrorStatement
    |   setStatement
    |   selectStatement
    |   insertStatement
    |   deleteStatement
    |   updateStatement
    |   truncateStatement
    |   whileStatement
    |   executeStatement
    ;   
    
ifThenElseStatement
    :   IF expression planClause? statementBlock elsePart?
        //-> ^(TN_IF ^(TN_TEST expression) statementBlock elsePart?)
    ;

elsePart
    :   ELSE statementBlock //-> statementBlock
    ;   
    
whileStatement
    :   WHILE expression planClause? statementBlock
        //-> ^(TN_WHILE ^(TN_TEST expression) statementBlock)
    ;   
    
breakStatement
    :   BREAK
        //-> ^(TN_EXITLOOP)
    ;   
    
continueStatement
    :   CONTINUE
        //-> ^(SYB_CONTINUE)
    ;   
    
beginTransactionStatement
    :   BEGIN (TRAN | TRANSACTION) simpleName?
        //-> ^(SYB_BEGIN_TRAN simpleName?) 
    ;   
    
commitStatement
    :   COMMIT (TRAN | TRANSACTION | WORK)? simpleName? 
        //-> ^(TN_COMMIT simpleName?)
    ;
    
rollbackStatement
    :   ROLLBACK (TRAN | TRANSACTION | WORK)? simpleName?
        //-> ^(TN_ROLLBACK simpleName?)
    ;       
        
saveTransactionStatement
    :   SAVE TRANSACTION simpleName
        //-> ^(SYB_SAVETRANS simpleName)
    ;       
        
returnStatement
    :   RETURN (expression)?
        //-> ^(TN_RETURN expression?)
    ;

//Goto label.   
gotoStatement
    :   GOTO ID     //-> ^(SYB_GOTO ID)
    ;
    
//Label for the Goto statement. 
labelStatement
    :   ID COLON    //-> ^(SYB_LABEL ID)
    ;   
    
//The WaitFor statement specifies a specific time, a time interval, or an event for the execution of a 
//statement block, stored procedure, or transaction.
waitforStatement
    :   WAITFOR ( (DELAY waitforSpan) | (TIME waitforSpan) | ERROREXIT | PROCESSEXIT | MIRROREXIT )+ 
        //-> ^(SYB_WAITFOR waitforSpan?)
    ;   
    
waitforSpan
    :   STRING_LIT | hostVariable
    ;   

//Declares local variables for a procedure. 
declareStatement
    :   DECLARE paramset //-> ^(SYB_DECLARE paramset)
    ;

    
raiseErrorStatement
    :   RAISERROR errorNumber printMessage? (COMMA argList)?
            (WITH ERRORDATA columnList)?
//            -> ^(TN_ERROR errorNumber printMessage?
//                ^(SYB_ARGUMENTS argList)?
//                ^(SYB_ERRORDATA columnList)?)
    ;   

//The error number for the Raise Error statement.   
errorNumber
    :   hostVariable | INT_LIT
    ;   


//Sybase Select.    
selectStatement
    :   SELECT ( ALL | DISTINCT )? (TOP INT_LIT)? columnList 
        intoClause?
        fromClause?
        whereClause?
        groupBy?
        havingClause? 
        unionClause*
        orderBy?
        computeClause*
        readOnlyClause?
        isolationClause?
        browseClause?
        planClause? 
//        -> ^(TN_SELECT DISTINCT?
//            ^(TN_COLS columnList)
//            ^(SYB_INTO intoClause)?
//            ^(TN_FROM fromClause)?
//            ^(TN_WHERE whereClause)?
//                ^(TN_GROUPBY groupBy)?
//                ^(TN_HAVING havingClause)?
//                ^(TN_UNION unionClause)*
//                ^(TN_ORDERBY orderBy)?) 
    ; 

    
//Write a message to screen. Same as Ingres Message statement.  
printStatement
    :   PRINT formatString COMMA argList  //-> ^(SYB_FORMAT_STRING formatString argList)
    |   PRINT printMessage         //-> ^(SYB_MSG printMessage)
    ;
    
formatString
    :   STRING_LIT
    ;
    
printMessage
    :   printPart (PLUS printPart)* //String concatenation
    ;   
    
printPart
    :   STRING_LIT 
    |   hostVariable
    ;   

//List of arguments for the Print statement or the Raise Error statement.
argList
    :   argument (COMMA argument)* //-> argument argument*
    ;

argument
    :   hostVariable | constant | ID
    |   expression
    ;

insertStatement
    :   INSERT (INTO)? objectName (LPAREN columnList RPAREN)? 
            ( valuePart | (selectStatement planClause?) )
            //-> ^(TN_INSERT objectName ^(TN_COLS columnList)? valuePart? selectStatement?)
    ;   
    
valuePart
    :   VALUES LPAREN valueList RPAREN
    ;   
    
//The list of values to be inserted.    
valueList
    :   simpleValue (COMMA simpleValue)*
        //-> ^(TN_PARAM simpleValue) ^(TN_PARAM simpleValue)* 
    ;   
    
simpleValue
    :   expression | DEFAULT
    ;
    
deleteStatement
    :   DELETE (FROM)? objectName fromClause? whereClause? planClause?
//        -> ^(TN_DELETE objectName
//                   ^(TN_FROM fromClause)?
//                   ^(TN_WHERE whereClause)?)
    ;   
    
updateStatement
    :   UPDATE objectName setPart fromClause? whereClause? planClause?
//        -> ^(TN_UPDATE objectName
//                   ^(TN_UPDATE_SETBLOCK setPart)
//               ^(TN_FROM fromClause)?
//               ^(TN_WHERE whereClause)?)               
    ;
    
truncateStatement
    :   TRUNCATE TABLE objectName 
//        -> ^(SYB_TRUNCATE objectName)
    ;   
    
caseExpression
    :   CASE a=expression? caseList (ELSE b=expression)? END
//        -> ^(TN_CASE $a? caseList ^(TN_CASE_ELSE $b)?)
    ;   
    
caseList
    :   casePart casePart* 
//        -> ^(TN_CASE_PART casePart) ^(TN_CASE_PART casePart)*
    ;   
    
casePart
    :   WHEN columnExpression THEN expression
//        -> ^(TN_CASE_TEST columnExpression) ^(TN_CASE_VALUE expression)
    ;   
    
coalesceExpression
    :   COALESCE LPAREN expressionList RPAREN
//        -> ^(TN_COALESCE expressionList)
    ;

//The list of expressions to be evaluated for the Coalesce expression.  
expressionList
    :   expression COMMA expression (COMMA expression)*
//        -> ^(TN_EXPR expression)*
    ;   
    
nullifExpression
    :   NULLIF LPAREN expressionList RPAREN
//        -> ^(TN_NULLIF expressionList)
    ;   

//Set part for the Update statement.
setPart
    :   SET setExpr (COMMA setExpr)* 
//        -> ^(TN_SET setExpr) ^(TN_SET setExpr)*
    ;

setExpr
    :   (objectName | hostVariable) EQ (expression | NULL | (LPAREN selectStatement RPAREN))     
    ;       

//List of columns.
columnList
    :   columnPart (COMMA columnPart)* //-> ^(TN_COL columnPart) ^(TN_COL columnPart)*
    ;
    
//altName is the alias used for a column name. 
columnPart
    :   (altName EQ )? columnExpression ( (AS)? altName)? 
//        -> ^(TN_COL_NAME altName)? ^(TN_COL_VALUE columnExpression)
    ;
    
altName 
    :   a=objectName | constant
//        -> $a? constant?
    ;       
    
columnExpression
    :   a=identityColumn   //-> ^(SYB_ID $a)
    |   ((objectName DOT)? '*')
    |   expression      
    ;   

//Sets the identity column for a table. 
identityColumn
    :   (objectName DOT)? SYB_IDENTITY (EQ expression)?
    ;

//The Select Into clause.   
intoClause
    :   INTO objectName
        (ON segmentName)?
        partitionClause?
            ( LOCK (DATAROWS | DATAPAGES | ALLPAGES)+ )?
            ( WITH intoOption (COMMA intoOption)*)?
//            -> objectName
    ;

segmentName
    :   objectName
    ;
    
partitionName
    :   objectName
    ;
        
intoOption
    :   ( MAX_ROWS_PER_PAGE | EXP_ROW_SIZE | RESERVEPAGEGAP | IDENTITY_GAP ) EQ INT_LIT
            (EXISTING TABLE simpleName)?
            ((EXTERNAL ( TABLE | FILE ))? AT STRING_LIT (COLUMN DELIMITER constant)?)?
    ;

partitionClause
    :   PARTITION BY ( 
        RANGE LPAREN columnList RPAREN partitionRangeRule 
    |   HASH LPAREN columnList RPAREN partitionHashRule 
    |   LIST LPAREN columnList RPAREN partitionListRule 
    |   ROUNDROBIN (LPAREN columnList RPAREN)? partitionRoundrobinRule )
    ;

partitionRangeRule
    :   LPAREN rangeList RPAREN 
    ;
    
rangeList
    :   rangePart (COMMA rangePart)*
    ;

rangePart
    :   partitionName? VALUES LTE LPAREN valueList2 RPAREN (ON segmentName)?
    ;   
    
valueList2
    :   ( constant | MAX ) (COMMA ( constant | MAX ))*
    ;   

partitionHashRule
    :   LPAREN hashList RPAREN 
    |   numberOfPartitions (ON LPAREN segmentList RPAREN)?
    ;
    
segmentList
    :   segmentName (COMMA segmentName)*
    ;   

numberOfPartitions
    :   INT_LIT
    ;
    
hashList
    :   hashPart (COMMA hashPart)*
    ;

hashPart
    :   partitionName (ON segmentName)?
    ;   

partitionListRule
    :   LPAREN listList RPAREN
    ;
    
listList
    :   listPart (COMMA listPart)*
    ;   
    
listPart
    :   (partitionName)? VALUES LPAREN constantList RPAREN (ON segmentName)?
    ;   
    
constantList
    :   constant (COMMA constant)?
    ;   

partitionRoundrobinRule
    :   partitionHashRule
    ;   

//The from clause for a Select statement.   
fromClause
    :   FROM joinList
//        -> joinList
    ;
    
joinList
    :   joinFactor (((joinType? JOIN) | COMMA) joinCond)*
//        -> joinFactor ^(TN_JOIN joinCond)*
    ;
    
joinType
    :   (INNER 
    |   LEFT (OUTER)? 
    |   RIGHT (OUTER)?)
    ;

joinCond
    :   joinFactor onClause?
    ;   
    
//This has been separated out to allow joins within parenthesis to be evaluated first.
joinFactor
    :   a=tableViewName | (LPAREN joinList RPAREN)
//        -> $a? joinList?
    ;   
    
tableViewName
    :   objectName (AS? simpleName)? READPAST? tableNameOptions?           
//            -> ^(TN_TABLE_NAME simpleName)? ^(TN_TABLE_VALUE objectName) tableNameOptions?
    ;   
    
tableNameOptions
    :   (LPAREN systemOptions+ RPAREN)?
            (HOLDLOCK | NOHOLDLOCK)? 
            (READPAST)?
            (SHARED)?
//            -> ^(SYB_INDEX systemOptions)?
    ;   
    
systemOptions
    :   a=indexPart 
    |   (PARALLEL (degreeOfParallelism)?) 
    |   (PREFETCH prefetchSize) 
    |   (LRU | MRU)
//        -> $a
    ;   
    
indexPart
    :   INDEX simpleName
//        -> simpleName
    ;       
    
degreeOfParallelism
    :   INT_LIT
    ;
    
prefetchSize   
    :   INT_LIT
    ;

//The on clause for the Join condition in a Select. 
onClause
    :   ON columnExpression
//        -> ^(TN_ON columnExpression)
    ;       

//The where clause for a Select statement.
whereClause
    :   WHERE columnExpression //-> columnExpression
    |   WHERE CURRENT OF simpleName //Option for delete statement.
    ;   

//The groupBy clause for a Select statement.   
groupBy
    :   GROUP BY (ALL)? factorList //-> factorList
    ;   
    
factorList
    :   expression (COMMA expression)* //-> expression expression*
    ;   
    
//The having clause for a Select statement. 
havingClause
    :   HAVING columnExpression //-> columnExpression
    ;
    
//The union clause for a Select statement.  
unionClause
    :   UNION ALL? selectStatement 
//        -> ALL? selectStatement
    ;
    
//The orderBy clause for a Select statement.   
orderBy
    :   ORDER BY orderList //-> orderList
    ;
    
orderList
    :   orderPart (COMMA orderPart)* //-> orderPart orderPart*
    ;   
    
orderPart
    :   expression orderDirection? 
    ;       
    
orderDirection 
    :   ASC | DESC
    ;

//The compute clause for a Select statement.
computeClause
    :   COMPUTE functionList 
            (BY columnList)?
    ;
    
functionList
    :   function LPAREN functionParams? RPAREN 
            (COMMA function LPAREN functionParams? RPAREN)*
    ;   

//The read only clause for a Select statement.  
readOnlyClause
    :   FOR (READ ONLY | UPDATE (OF columnList)?)
    ;
    
//The isolation clause for a Select statement.  
isolationClause
    :   AT? ISOLATION LEVEL? 
            ( READ UNCOMMITTED | READ COMMITTED | REPEATABLE READ | SERIALIZABLE | INT_LIT )
    ;
    
//The browse clause for a Select statement. 
browseClause
    :   FOR BROWSE
    ;

//The plan clause specifies an abstract plan to be used to optimize a query.    
planClause
    :   PLAN STRING_LIT
    ;

setStatement
    :   SET (a=setExpr | (setValue setOptions* onOffPart?))
//        -> ^(SYB_SET $a? setValue? setOptions* onOffPart?)
    ;
    
//Includes keywords which can appear as a setValue.    
setValue
    :   LOCK | PLAN | PREFETCH | TABLE | (TRANSACTION isolationClause) | ID
    ;   

//Includes keywords which can appear as a setOption.   
setOptions
    :   COMMA? ( (WITH PASSWD) | FOR | LONG | (ON ENDTRAN) | TIME | simpleFactor )
    ;   
    
onOffPart
    :   OFF 
    |   (ON (WITH errorPart)?) 
    |   (CHARSET (WITH errorPart)?)
    |   DEFAULT
    ;   
    
errorPart
    :   ERROR | NO_ERROR
    ;       
    
//planName, indexName, tableName, transactionName, savepointName
simpleName
    :   ID
    ;
    
//Expressions in Sybase.    
expression
    :   simpleExpression
    |   selectStatement
    |   caseExpression
    |   nullifExpression
    |   coalesceExpression
    ;   

//Used in simple expression.        
nullPart
    :   (IS NOT?) NULL
    ;   
    
simpleExpression
    :   andExpr (OR andExpr)*  
    ;   

andExpr 
    :   notExpr (AND notExpr)*
    ;
    
notExpr
    :   (NOT)? relExpr nullPart?
    ;   
    
relExpr
    :   plusExpr (relOp plusExpr)* (betweenPart | inPart)?
    ;   

plusExpr 
    :   multExpr ((PLUS | MINUS | BITAND | BITOR | EXOR) multExpr)*
    ;
    
multExpr
    :   signExpr ((MULT | DIVIDE | MODE) signExpr)*
    ;
    
signExpr
    :   (MINUS | PLUS | BITNOT)? factor 
    ;
    
betweenPart
    :   NOT? BETWEEN notExpr AND notExpr 
//        -> NOT? ^(TN_BETWEEN notExpr notExpr)
    ;
    
inPart :   NOT? IN LPAREN functionParams? RPAREN //-> NOT? ^(TN_IN functionParams?)
    ;       
    
factor  
    :   complexFactor //Keep this before simpleFactor.
    |   simpleFactor
    ;
    
simpleFactor
    :   constant    
    |   hostVariable
    |   objectName
    ;
    
complexFactor
    :   function LPAREN functionParams? RPAREN //-> ^(TN_FUNC function functionParams?)
    |   LPAREN expression RPAREN
    ;           
    
constant:   INT_LIT
    |   FLOAT_LIT
    |   STRING_LIT
    |   NULL
    ;

//Locereas system-defined global variables start with '@@'. 
//The list includes global variable names that are also Sybase keywordsal variables start with '@' wh.
hostVariable
    :   globalVariable //-> ^(SYB_GLOBAL_VAR globalVariable)
    |   (STRUDEL (ERROR | IDENTITY | ISOLATION | objectName))  //-> COLON[":"] ERROR? IDENTITY? ISOLATION? objectName?                                                                
    ;
    
globalVariable
    :   (STRUDEL STRUDEL (ERROR | IDENTITY | ISOLATION | objectName)) //-> COLON[":"] COLON ERROR? IDENTITY? ISOLATION? objectName?
    ;
    
//The list includes function names that are also Sybase keywords.   
function
    :   MAX | EXISTS | LEFT | RIGHT | OBJECT_NAME | IDENTITY | ANY | ALL | ID 
    ;
    
//The parameters for a function.    
functionParams
    :   (selectStatement
    |   expression AS scalartype //The cast function uses this format.
    |   simpleFunctionParam) //Keep this last always.
 //       -> ^(TN_PARAM selectStatement? expression?)? simpleFunctionParam?
    ;       
    
simpleFunctionParam   
    :   functionParam (COMMA functionParam)* 
//        -> ^(TN_PARAM functionParam) ^(TN_PARAM functionParam)*
    ;
    
functionParam
    :   '*'
    |   expression 
    |   scalartype
    ;   
    
//Sybase operators.
    
arithmeticOp
    :   PLUS | MINUS | MULT | DIVIDE | MODE
    ;
    
relOp  :   EQ | NEQ | GT | GTE | LT | LTE | NGT | NLT | (NOT? LIKE) | OUTJOIN
    ;
    
logicalOp      
    :   NOT | AND | OR
    ;
    
bitOp
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

intType:   ((UNSIGNED)? (INTEGER | SMALLINT | INT | BIGINT)) 
    |   TINYINT     //-> SYB_TINYINT
    ;
    
floatType
    :   FLOAT LPAREN INT_LIT RPAREN
    |   FLOAT                       //-> SYB_FLOAT
    |   REAL | (DOUBLE PRECISION)
    |   DECIMAL LPAREN INT_LIT COMMA INT_LIT RPAREN //-> SYB_DECIMAL LPAREN INT_LIT COMMA INT_LIT RPAREN
    |   DECIMAL LPAREN INT_LIT RPAREN           //-> SYB_DECIMAL LPAREN INT_LIT COMMA[","] INT_LIT["0"] RPAREN //In Ingres, the default scale is 0.
    |   NUMERIC LPAREN INT_LIT COMMA INT_LIT RPAREN
    |   NUMERIC LPAREN INT_LIT RPAREN           //-> NUMERIC LPAREN INT_LIT COMMA[","] INT_LIT["0"] RPAREN //In Ingres, the default scale is 0.
    |   (DECIMAL | NUMERIC)             //-> SYB_DECIMAL
    ;
dateType
    :   DATE 
    |   TIME //-> SYB_TIME 
    |   DATETIME | SMALLDATETIME | TIMESTAMP
    ;

stringType
    :   (CHAR | VARCHAR | UNICHAR | UNIVARCHAR | ambigousStringTypes) (LPAREN INT_LIT RPAREN)?
    |   TEXT | UNITEXT
    ;   
    
ambigousStringTypes 
    :       
        NCHAR    //-> SYB_NCHAR 
    |   NVARCHAR //-> SYB_NVARCHAR
    ;
    
binaryType
    :   (BINARY | VARBINARY) (LPAREN INT_LIT RPAREN)?
    |   IMAGE
    ;   
      
miscType
    :   MONEY | SMALLMONEY | BIT
    ;   
    
userType
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
GO      :   G O;
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
//STRING_LIT
//    :   SINGLE_STRINGDELIM (SINGLE_EMBEDDEDQUOTE | ~(SINGLE_STRINGDELIM))* SINGLE_STRINGDELIM
//    |   DOUBLE_STRINGDELIM (DOUBLE_EMBEDDEDQUOTE | ~(DOUBLE_STRINGDELIM))* DOUBLE_STRINGDELIM
//    ;   
STRING_LIT
    :   '\'' ('\'\'' | ~'\'')* '\''
    |   '"'  ('""'   | ~'"' )* '"'
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
WS  :   ('\t' | ' ' | '\r' | '\n' | '\u000C') -> skip;
//COMMENT :   '/*' ( options {greedy=false;} : .)* '*/' -> skip;
COMMENT :   '/*' ()*? '*/' -> skip;
LINE_COMMENT
    :   '--' ~('\n'|'\r')* '\r'? '\n' -> skip;

// Allow/skip: so we do not get syntax errors on isql execution word 'go'
// go
// go 10
// go | grep wahtever
// go | bcp toTable -Usa -Pxxx -Sserver
ISQL_SEND
    :   '\r'? '\n' ('go'|'GO') ~('\n'|'\r')* '\r'? '\n' -> skip;
