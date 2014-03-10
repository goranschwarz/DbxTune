--use perfdemo
go


set nocount on
go


/*
** If not in correct database, terminate
*/
declare @dbname varchar(30)
select @dbname = db_name()
if (@dbname = "master")
begin
	print "ERROR: Sorry you are in the wrong database, you should not install into the 'master' database."
	select syb_quit()
end
go

declare @dbname varchar(30)
select @dbname = db_name()
print 'Installing into dbname ''%1!'', at server ''%2!''', @dbname, @@servername
go





/*
** ----------------------------------------------------------
** -- QUEUE TABLE
** ----------------------------------------------------------
*/
if ((select object_id('TestQueue')) is not null)
begin
	print "  drop table: TestQueue"
	drop table TestQueue
end
go
print "create table: TestQueue"
go
create table TestQueue
(
	type		varchar(30)				not null,
	c1		varchar(255)				not null
)
lock datarows
go

--create clustered index TestQueue on TestQueue(qid)
go



/*
** ----------------------------------------------------------
** -- Statistics table
** ----------------------------------------------------------
*/
if ((select object_id('qStatInfo')) is not null)
begin
	print "  drop table: qStatInfo"
	drop table qStatInfo
end
go
print "create table: qStatInfo"
go
create table qStatInfo
(
	operation	varchar(30)	not null,
	execCounter	int		not null,
	lastExecTime	datetime	not null,
	
	lastExecTimeMs	int		not null,
	avgExecTimeMs	int		not null,
	minExecTimeMs	int		not null,
	maxExecTimeMs	int		not null,
	
	primary key(operation)
)
go





if ((select object_id('setAppStatus')) is not null)
begin
	print "  drop proc: setAppStatus"
	drop proc setAppStatus
end
go
print "create proc: setAppStatus"
go
/*=====================================================================**
** PROCEDURE: setAppStatus
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
	** Use the three fields to control the application
	**
	** clientapplname: is used to identify if this is a queueConsumer queueProducer or queueController
	**                 known values are: qConsumer, qProducer, qController
	**
	** clienthostname: In what status the queueXXX is in (waiting for q entries, xxx, yyy)
	**
	** clientname:     For example if "queueController" wants to pause the queueConsumer
	**                 it sets this value to "qConsumer:pause"
	**
** - @xxx      fixme
** - @yyy      fixme
** - @zzz      fixme
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @xxx      fixme
** - @yyy      fixme
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-02	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure setAppStatus
(
	@qThreadType		varchar(30) = null,
	@qThreadInState		varchar(30) = null,
	@msgToOtherThread	varchar(30) = null
)
as
begin
	if (@qThreadType      is not null) 
		set clientapplname @qThreadType

--	if (@qThreadInState   is not null) set clienthostname @qThreadInState
--	if (@msgToOtherThread is not null) set clientname     @msgToOtherThread

	set clienthostname @qThreadInState
	set clientname     @msgToOtherThread
	
	return 0
end
go






if ((select object_id('getAppStatus')) is not null)
begin
	print "  drop proc: getAppStatus"
	drop proc getAppStatus
end
go
print "create proc: getAppStatus"
go
/*=====================================================================**
** PROCEDURE: getAppStatus
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
** - @qThreadType      Type of thread we want info about
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @qThreadInState   xxx
** - @msgFromThread    xxx
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
** 1 - NO thread of the type found
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-02	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure getAppStatus
(
	@qThreadType		varchar(30) = null,
	@qThreadInState		varchar(30) = null OUT,
	@msgFromThread		varchar(30) = null OUT
)
as
begin
	select 
		--clientapplname, 
		@qThreadInState = clienthostname, 
		@msgFromThread  = clientname
	from master..sysprocesses 
	where clientapplname = @qThreadType
	
	if (@@rowcount = 0)
	begin
		select 
			@qThreadInState = "NOT-FOUND", 
			@msgFromThread  = "NO-THREAD-TYPE-FOUND"
		
		return 1
	end
end
go






if ((select object_id('qIsPaused')) is not null)
begin
	print "  drop proc: qIsPaused"
	drop proc qIsPaused
end
go
print "create proc: qIsPaused"
go
/*=====================================================================**
** PROCEDURE: qIsPaused
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
** - @returnStatusAsSelect     if > 0: do select "STOPPED", if we are stopped
**                             else simply use return code.
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @xxx      fixme
** - @yyy      fixme
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
** 1 - Controller say we should STOP this thread
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-03	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure qIsPaused
(
	@returnStatusAsSelect int    = null
)
as
begin
	declare @paused int,
	        @rc     int

	declare
		@qThreadInState        varchar(30),
		@msgFromThread         varchar(30)


	select	@paused = 0

set flushmessage on

	----------------------------------------------------
	-- CHECK NULL input, and set to DEFAULT VALUES
	----------------------------------------------------
	if (@returnStatusAsSelect is NULL) select @returnStatusAsSelect = 1

	/*
	** Check if we are PAUSED
	*/
	select @paused = 1
	while (@paused > 0)
	begin
		select @paused = 0

		-----------------------------------------------
		-- get info from the controller
		-----------------------------------------------
		exec @rc = getAppStatus 'qController', 
			@qThreadInState = @qThreadInState OUT, 
			@msgFromThread  = @msgFromThread  OUT

		if (@msgFromThread like 'PAUSE%')
		begin
			select @paused = 1

			/*** SET STATUS ***/
			exec setAppStatus "qConsumer", "-PAUSED-", "by controller"
			print "qConsumer spid=%1!, is PAUSED by controller.", @@spid
		end
		if (@msgFromThread like 'STOP%')
		begin
			select @paused = 1

			/*** SET STATUS ***/
			exec setAppStatus "qConsumer", "-STOPPED-", "by controller"
			print "qConsumer spid=%1!, is STOPPED by controller.", @@spid

			/*** EXIT POINT ***/
			select exitType = "STOPPED"
/*<--*/			return 1
		end
		if (@rc != 0)
		begin
			select @paused = 1

			/*** SET STATUS ***/
			exec setAppStatus "qConsumer", "-PAUSED-", "NO controller available"
			print "qConsumer spid=%1!, is PAUSED NO controller available.", @@spid
		end

		if (@paused > 0)
		begin
			waitfor delay "00:00:02"
		end
	end

/*<--*/	return(0)
end
go






if ((select object_id('qConsumer')) is not null)
begin
	print "  drop proc: qConsumer"
	drop proc qConsumer
end
go
print "create proc: qConsumer"
go
/*=====================================================================**
** PROCEDURE: qConsumer
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
** - @xxx      fixme
** - @yyy      fixme
** - @zzz      fixme
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @xxx      fixme
** - @yyy      fixme
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-01	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure qConsumer
(
	@batchSize     int    = null,
	@updQueueStat  int    = null,
	@useTrans      int    = null
)
as
begin
	declare 
		@rc                    int,
		@paused                int,
		@count                 int,
		@startTime             datetime,
		@execTimeInMs          int,
		@destExecStartTime     datetime,
		@destExecTimeInMs      int,
		@destExecBatchTimeInMs int,
		@destExecStatCount     int,
		@msg                   varchar(30)

	declare 
		@c_type  varchar(30),
		@c_c1    varchar(255)

	select	@count  = 0,
		@paused = 0

set flushmessage on

	----------------------------------------------------
	-- CHECK NULL input, and set to DEFAULT VALUES
	----------------------------------------------------
	if (@batchSize    is NULL) select @batchSize    = 10
	if (@updQueueStat is NULL) select @updQueueStat = 1
	if (@useTrans     is NULL) select @useTrans     = 1

	if (@batchSize < 1)
	begin
		print "INFO: @batchSize cant be 0 (or < 1), setting: @batchSize=1"
		select @batchSize = 1
	end

	print "INFO: 'Batch Size' is set to: %1!",         @batchSize
	print "INFO: 'Update Queue Stats' is set to: %1!", @updQueueStat
	print "INFO: 'Use Transaction' is set to: %1!",    @useTrans

	if (@useTrans < 1)
	begin
		print "WARNING: TRANSACTION handling is turned OFF..."
	end


	exec setAppStatus "qConsumer", "-START-"

	/*** CHECK if we are paused by the controller ***/
	exec @rc = qIsPaused
	if (@rc != 0)
/*<--*/		return @rc
	
	/*
	** Loop until thereare NO more rows in the queue table
	*/
	while (1=1)
	begin
		select @count = 0
		select @startTime = getdate()

		/*
		** Get the mergeToMoClass as a comma separated list
		** Use a cursor for this...
		**
		** Example: CellCarrier,Carrier
		*/
		DECLARE qConsumerCurs cursor
			for
				select
					type, 
					c1
				from
					TestQueue
				readpast

			for update
	--		for read only

		OPEN qConsumerCurs 
		FETCH qConsumerCurs INTO @c_type, @c_c1

		if (@useTrans > 0)
		BEGIN TRAN

		WHILE (@@sqlstatus = 0)
		BEGIN
			select @count = @count + 1

			if ((@count % @batchSize) = 0)
			begin
				--print "DEBUG: commit/begin tran. @count %1!.", @count

				/*** SET STATUS ***/
				exec setAppStatus "qConsumer", "-COMMIT-"

				if (@useTrans > 0)
				COMMIT TRAN

				/*** WRITE STATISTICS ***/
				if (@updQueueStat > 0)
				begin
					/*** SET STATUS ***/
					exec setAppStatus "qConsumer", "-WRITE_STAT-"
					if exists(select * from qStatInfo where operation = @c_type)
					begin
						update qStatInfo
						   set execCounter  = execCounter + @batchSize,
						       lastExecTime = getdate(),
						       lastExecTimeMs = @destExecTimeInMs,
						       avgExecTimeMs  = -1,
						       minExecTimeMs  = -1,
						       maxExecTimeMs  = -1
						 where operation = @c_type
					end
					else
					begin
						insert into qStatInfo(operation, execCounter, lastExecTime, lastExecTimeMs, avgExecTimeMs, minExecTimeMs, maxExecTimeMs)
						values(@c_type, 1, getdate(), @destExecTimeInMs, -1, -1, -1)
					end

					-- Reset some counters
					select @destExecStatCount = 0, @destExecBatchTimeInMs = 0
				end

				/*** CHECK if we are paused by the controller ***/
				exec @rc = qIsPaused
				if (@rc != 0)
/*<--*/					return @rc

				if (@useTrans > 0)
				BEGIN TRAN
			end

			/* LOCK the current row, so that no other worker can pick up same row */
			--update TestQueue 
			--   set someColumn="some value" 
			--where current of qConsumerCurs


			/*** SET STATUS ***/
			select @msg = "count="+convert(varchar(10),@count)+", batchSize="+convert(varchar(10),@batchSize)
			exec setAppStatus "qConsumer", "-EXEC-", @msg

			-- record when we started to execute
			select @destExecStartTime = getdate()

			/*** DO THE WORK ***/
			--print "DEBUG: sleep 1 sec. @count %1!, c1 = '%2!'.", @count, @c_c1
			--waitfor delay "00:00:00:300"

			/* use various consumer types */
			if      (@c_type = 'dest1') exec consumeDest1 @c_c1, @count
			else if (@c_type = 'dest2') exec consumeDest2 @c_c1, @count
			else if (@c_type = 'dest3') exec consumeDest3 @c_c1, @count
--			else if (@c_type = 'type1') exec consumeType1 @c_c1
--			else if (@c_type = 'type2') exec consumeType3 @c_c1
--			else if (@c_type = 'type3') exec consumeType3 @c_c1
			else
			begin
				declare @execStr varchar(255)
				select @execStr = "exec consume"+@c_type+" '"+@c_c1+"'"
				exec(@execStr)
			end

			-- calculate some statistics
			select @destExecTimeInMs      = datediff(ms, @destExecStartTime, getdate())
			select @destExecStatCount     = @destExecStatCount + 1
			select @destExecBatchTimeInMs = @destExecBatchTimeInMs + @destExecTimeInMs

			/* DELETE the treated record from the queue table */
			delete from TestQueue where current of qConsumerCurs

			/*** SET STATUS ***/
			exec setAppStatus "qConsumer", "-NEXT_ROW-"

			FETCH qConsumerCurs INTO @c_type, @c_c1
		END

		if (@useTrans > 0)
		COMMIT TRAN

		CLOSE qConsumerCurs 
		DEALLOCATE CURSOR qConsumerCurs 

		/*
		** Check if we should EXIT or not
		*/
		if exists (select * from TestQueue)
		begin
			print "DEBUG: more rows in TestQueue, continuing"
		end
		else
		begin
			--print "DEBUG: NO-MORE-ROWS in TestQueue, exiting"
			--break
			
			/*** SET STATUS ***/
			exec setAppStatus "qConsumer", "-NO_MORE_ROWS-", "sleeping for 10 seconds"

			select @execTimeInMs = datediff(ms, @startTime, getdate())
			--print "DEBUG: NO-MORE-ROWS in TestQueue, sleeping for 10 seconds... ExecTimeInMs=%1!.", @execTimeInMs
			waitfor delay "00:00:10"

			/*** CHECK if we are paused by the controller ***/
			exec @rc = qIsPaused
			if (@rc != 0)
/*<--*/				return @rc
		end
	end
	
	select exitType = "NORMAL_EXIT"
/*<--*/	return(0)
end
go






if ((select object_id('qGenerate')) is not null)
begin
	print "  drop proc: qGenerate"
	drop proc qGenerate
end
go
print "create proc: qGenerate"
go
/*=====================================================================**
** PROCEDURE: qGenerate
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
** - @xxx      fixme
** - @yyy      fixme
** - @zzz      fixme
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @xxx      fixme
** - @yyy      fixme
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-01	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure qGenerate
(
	@batchSize     int            = 10000,
	@type          varchar(30)    = null
)
as
begin
	declare 
		@count        int,
		@startTime    datetime,
		@execTimeInMs int

	declare 
		@c_c1    varchar(255)

	select @count = 0

	if (@batchSize is NULL)
		select @batchSize = 10000

	if (@type = '')
		select @type = null

set flushmessage on

	print "proc: qGenerate, will do '%1!' iterations of type '%2!'.", @batchSize, @type

	BEGIN TRAN

	/*
	** Loop 
	*/
	while (@count < @batchSize)
	begin
		select @count = @count + 1
		
		if (@type is null)
		begin
			insert into TestQueue select 'dest1', newid()
			insert into TestQueue select 'dest2', newid()
			insert into TestQueue select 'dest3', newid()
		end
		else
		begin
			insert into TestQueue select @type, newid()
		end
	end

	COMMIT TRAN

	print "proc: qGenerate, I have now done '%1!' iterations of type '%2!'.", @batchSize, @type
	
	return(0)
end
go





if ((select object_id('qGen2')) is not null)
begin
	print "  drop proc: qGen2"
	drop proc qGen2
end
go
print "create proc: qGen2"
go
/*=====================================================================**
** PROCEDURE: qGen2
**---------------------------------------------------------------------**
** Description:
**
** INTERNAL fixme
**
** fixme
**
**---------------------------------------------------------------------**
** Input parameters:
**
** - @xxx      fixme
** - @yyy      fixme
** - @zzz      fixme
**
**---------------------------------------------------------------------**
** output parameters:
**
** - @xxx      fixme
** - @yyy      fixme
**
**---------------------------------------------------------------------**
** output select:
**
** - none
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0 - OK
**
**---------------------------------------------------------------------**
** Error codes:
**
** - none
**
**---------------------------------------------------------------------**
** History:
**
** xxxx-xx-xx	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure qGen2
(
	@batchSize     int    = 10000
)
as
begin
	declare 
		@count        int,
		@startTime    datetime,
		@execTimeInMs int,
		@maxVal       int

	declare 
		@c_c1    varchar(255)

	select @count = 0
	select @maxVal = isnull(convert(int, max(convert(int,c1))),0) from TestQueue

set flushmessage on


	BEGIN TRAN

	/*
	** Loop
	*/
	while (@count < @batchSize)
	begin
		select @count = @count + 1
		
		insert into TestQueue select convert(varchar(30), @maxVal + @count)
	end

	COMMIT TRAN
	
	return(0)
end
reset
go
