-------------------------------------------------
1: create the database perfdemo somewhere
-------------------------------------------------
db.sql can be used as a example
isql -Usa -P -SGORAN_1_DS -w999 -i db.sql

-------------------------------------------------
2: create the destination objects
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -w999 -i DestTab1.sql
isql -Usa -P -SGORAN_1_DS -w999 -i DestTab2.sql
isql -Usa -P -SGORAN_1_DS -w999 -i DestTab3.sql
isql -Usa -P -SGORAN_1_DS -w999 -i otherTables.sql

-------------------------------------------------
3: install the consumer procs and queue system
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -w999 -i consumeProcs.sql
isql -Usa -P -SGORAN_1_DS -w999 -i qsystem.sql











cd C:\projects\asetune\src\com\asetune\perftest\sql

isql -Usa -P -SGORAN_1_DS -w999 -i db.sql

isql -Usa -P -SGORAN_1_DS -w999 -i DestTab1.sql
isql -Usa -P -SGORAN_1_DS -w999 -i DestTab2.sql
isql -Usa -P -SGORAN_1_DS -w999 -i DestTab3.sql
isql -Usa -P -SGORAN_1_DS -w999 -i otherTables.sql

isql -Usa -P -SGORAN_1_DS -w999 -i consumeProcs.sql
isql -Usa -P -SGORAN_1_DS -w999 -i qsystem.sql

