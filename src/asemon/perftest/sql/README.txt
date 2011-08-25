-------------------------------------------------
1: create the database perfdemo somewhere
-------------------------------------------------
db.sql can be used as a example
isql -Usa -P -SGORAN_1_DS -i db.sql

-------------------------------------------------
2: create the destination objects
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -i DestTab1.sql
isql -Usa -P -SGORAN_1_DS -i DestTab2.sql
isql -Usa -P -SGORAN_1_DS -i DestTab3.sql
isql -Usa -P -SGORAN_1_DS -i otherTables.sql

-------------------------------------------------
3: install the consumer procs and queue system
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -i consumeProcs.sql
isql -Usa -P -SGORAN_1_DS -i qsystem.sql

