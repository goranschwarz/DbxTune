-------------------------------------------------
1: create the database perfdemo somewhere
-------------------------------------------------
db.sql can be used as a example
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i db.sql

-------------------------------------------------
2: create the destination objects
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i DestTab1.sql
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i DestTab2.sql
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i DestTab3.sql
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i otherTables.sql

-------------------------------------------------
3: install the consumer procs and queue system
-------------------------------------------------
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i consumeProcs.sql
isql -Usa -P -SGORAN_1_DS -Dperfdemo -w999 -i qsystem.sql










c:
cd C:\projects\dbxtune\src\com\dbxtune\perftest\sql

set ASE_USER=sa
set ASE_PASS=sybase
set ASE_SRV=GORAN_15702_DS

set ASE_DBN=perfdemo_hdd
set ASE_DBN=perfdemo

isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i db.sql

isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i DestTab1.sql
isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i DestTab2.sql
isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i DestTab3.sql
isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i otherTables.sql

isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i consumeProcs.sql
isql -U%ASE_USER% -P%ASE_PASS% -S%ASE_SRV% -D%ASE_DBN% -w999 -i qsystem.sql

