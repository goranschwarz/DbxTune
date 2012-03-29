/*
 *      Procedure Name  :  full_mda_version_report
 *      Database/Schema :  asemon_se
 *
 *      Description:
 *          xxx
 *
 *
 *      Tables Impacted :
 *         asemon_mda_info
 *
 *      Params:
 *         none
 *
 *      Revision History:
 *
 *         Date:          Id:              Comment:
 *         2012-03-19     Goran Schwarz    Original
 *
 */
CREATE PROCEDURE full_mda_version_report
(
--  IN name_in VARCHAR(255)
)
READS SQL DATA
BEGIN

	/*----------------------------------------------
	** All 'DECLARE' statements must come first
	*/

	-- Declare 'c_' variables to read in each record from the cursor
	DECLARE c_atAseVersion   INT;
	DECLARE c_prevAseVersion INT;
	DECLARE c_xxx            VARCHAR(255);

	-- Declare variables used just for cursor and loop control
	DECLARE noMoreRows BOOLEAN;
	DECLARE loopCntr   INT      DEFAULT 0;
	DECLARE numRows    INT      DEFAULT 0;

	-- Declare the cursor
	DECLARE mdaInfo_cur CURSOR FOR
		SELECT DISTINCT srvVersion
		FROM asemon_mda_info;

	-- Declare 'handlers' for exceptions
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET noMoreRows = TRUE;

	/*----------------------------------------------
	** Loop ASE Versions
	*/
	-- 'open' the cursor and capture the number of rows returned
	-- (the 'select' gets invoked when the cursor is 'opened')
	OPEN mdaInfo_cur;
	SELECT FOUND_ROWS() into numRows;

	the_loop: LOOP

	FETCH  mdaInfo_cur INTO c_atAseVersion;

	-- BREAK OUT OF THE LOOP IF
	-- 1: there were no records, or
	-- 2: we've processed them all
	IF noMoreRows THEN
		CLOSE mdaInfo_cur;
		LEAVE the_loop;
	END IF;

	-- the equivalent of a 'print statement' in a stored procedure
	-- it simply displays output for each loop
	select c_atAseVersion;

	-- count the number of times looped
	SET loopCntr = loopCntr + 1;

	END LOOP the_loop;

	-- 'print' the output so we can see they are the same
	select numRows, loopCntr;

END;
