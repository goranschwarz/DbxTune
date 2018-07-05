package com.asetune.sql;

import org.apache.log4j.Logger;

import com.asetune.utils.StringUtil;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

/**
 * Helper class that tries to parse a SQL Statement and replace <i>constants</i> into question marks (?)<br>
 * This may be used to anonymize data<br>
 * Or it could be used to look for SQL statements that are repeated (but with <i>stripped</i> where clauses
 */
public class StatementNormalizer
{
	private static Logger _logger = Logger.getLogger(StatementNormalizer.class);

	static class ReplaceConstantValues extends ExpressionDeParser
	{
		@Override
		public void visit(InExpression inExpression) 
		{
			StringBuilder sb = getBuffer();
			
			if (inExpression.getLeftExpression() == null) {
				inExpression.getLeftItemsList().accept(this);
			} else {
				inExpression.getLeftExpression().accept(this);
				if (inExpression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_RIGHT) {
					sb.append("(+)");
				}
			}
			if (inExpression.isNot()) {
				sb.append(" NOT");
			}
			sb.append(" IN ");

			inExpression.getRightItemsList().accept(this);

			// Replace: "IN (?, ?, ?)" with "IN (...)" 
			int start = sb.lastIndexOf(" IN (");
			int end   = sb.lastIndexOf(")") + 1;
			sb.replace(start, end, " IN (...)");
		}

		@Override
		public void visit(DoubleValue doubleValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(HexValue hexValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(LongValue longValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(StringValue stringValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(DateValue dateValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(TimestampValue timestampValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(TimeValue timeValue) {
			this.getBuffer().append("?");
		}

		@Override
	    public void visit(UserVariable var) {
			String varStr = var.toString();
			// "@p0" or "@p9754" then do "?" because it's a "runtime value sent in dynamic sql"...
			// but if @someName then keep the original variable name
			if (varStr.matches("^@p[0-9]+$"))
				varStr = "?";
			this.getBuffer().append(varStr);
	    }

	}

	private StringBuilder      _buffer;
	private ExpressionDeParser _exprDeParser;
	private SelectDeParser     _selectDeParser;
	private StatementDeParser  _stmtDeParser;

	public StatementNormalizer()
	{
		_buffer         = new StringBuilder();
		_exprDeParser   = new ReplaceConstantValues();
		_selectDeParser = new SelectDeParser(_exprDeParser, _buffer);
		_stmtDeParser   = new StatementDeParser(_exprDeParser, _selectDeParser, _buffer);

		_exprDeParser.setSelectVisitor(_selectDeParser);
		_exprDeParser.setBuffer(_buffer);
	}

	public String normalizeStatement(String sql) 
	throws JSQLParserException
	{
		if (StringUtil.isNullOrBlank(sql))
			return "";

		Statement stmt = CCJSqlParserUtil.parse(sql);

		stmt.accept(_stmtDeParser);

		// Get string and truncate the buffter for next statement
		String normalizedSql = _buffer.toString();
		_buffer.setLength(0);

		if (StringUtil.isNullOrBlank(normalizedSql))
			return "";

		return normalizedSql;
	}

	public String normalizeStatementNoThrow(String sql) 
	{
		try
		{
			return normalizeStatement(sql);
		}
		catch(JSQLParserException | TokenMgrError ex)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Problems normalizing SQL=|"+sql+"|", ex);

_logger.info("Problems normalizing SQL=|"+sql+"|", ex);
			
			return "";
			//return ex.getCause()+"";
		}
	}

//	public static String normalizeStatement(String sql) throws JSQLParserException
//	{
//		StringBuilder buffer = new StringBuilder();
//		ExpressionDeParser expr = new ReplaceConstantValues();
//
//		SelectDeParser selectDeparser = new SelectDeParser(expr, buffer);
//		expr.setSelectVisitor(selectDeparser);
//		expr.setBuffer(buffer);
//		StatementDeParser stmtDeparser = new StatementDeParser(expr, selectDeparser, buffer);
//
//		Statement stmt = CCJSqlParserUtil.parse(sql);
//
//		stmt.accept(stmtDeparser);
//		return stmtDeparser.getBuffer().toString();
//	}

	public static void main(String[] args) throws JSQLParserException
	{
		StatementNormalizer sn = new StatementNormalizer();
		System.out.println(sn.normalizeStatement("SELECT 'abc', 5 FROM mytable WHERE col='test'"));
		System.out.println(sn.normalizeStatement("SELECT 'abc', 5 FROM mytable WHERE col in('a','b','c')"));
		System.out.println(sn.normalizeStatement("SELECT \"abc\", 5 FROM mytable WHERE col in(\"a\",'b','c') and c99 = \"abc\" ---"));
		System.out.println(sn.normalizeStatement("UPDATE table1 A SET A.columna = 'XXX' WHERE A.cod_table = 'YYY'"));
		System.out.println(sn.normalizeStatement("INSERT INTO example (num, name, address, tel) VALUES (1, 'name', 'test ', '1234-1234')"));
		System.out.println(sn.normalizeStatement("DELETE FROM table1 where col=5 and col2=4"));
		System.out.println(sn.normalizeStatementNoThrow("declare @xxx select @xxx=id from dummy1"));
		System.out.println(sn.normalizeStatementNoThrow("SELECT a.xforeta, a.personnummer_2, a.Fornamn, a.Efternamn, a.status, CASE WHEN pp.xPerson IS NULL THEN 0 ELSE 1 END AS protected, a.employeeid AS anstallningsnr, e.xid AS employmentid, e.xanstallda, e.dateemployed, e.datequit, e.terminated, f.xid, f.foretag, f.orgnr, f.xmaklar, f.ansvass, f.xLivAdmAssistent, f.xcreated_by, f.xcreated, f.xmodified_by, f.xmodified, CASE WHEN x.varde IS NULL THEN 0 ELSE 1 END AS blocked, k.xid AS kostnadsstalleid, k.namn AS kostnadsstallenamn, k.xForeta AS kostnadsstalleforetag, pc.Name AS pensionCategory, a.pkassa, prem.plannamn, ca.name AS collectiveAgreementName FROM employmentExtended e JOIN Anstallda a ON e.xanstallda = a.xid JOIN foretag f ON e.xforetag = f.xid LEFT JOIN PensionCategory pc ON a.xPensionCategory = pc.xID LEFT JOIN kostnadsstallen k ON (k.xid <> 0 and a.xkostnadsstalle = k.xid) LEFT JOIN personparameter pp ON a.xid = pp.xperson AND pp.value = '1' AND pp.xparametertype = (SELECT xid FROM PERSONPARAMETERTYPE WHERE code = 'PROTECTED_INFO') LEFT JOIN (SELECT varde FROM xkod WHERE typ = 'MM_AuthXID') x on x.varde = cast(f.xid as varchar) LEFT JOIN premieplaner prem ON a.xpremplan = prem.xid LEFT JOIN collectiveagreementemployee cae ON a.xid = cae.xanstallda LEFT JOIN collectiveagreement ca ON cae.xcollectiveagreement = ca.xid WHERE f.status2 = 1 AND  a.personnummer_2 IN (@p0,@p1,@p2,@p3,@p4,@p5,@p6,@p7,@p8,@p9,@p10,@p11,@p12,@p13,@p14,@p15,@p16,@p17,@p18,@p19,@p20,@p21,@p22,@p23,@p24,@p25,@p26,@p27,@p28,@p29,@p30,@p31,@p32,@p33,@p34,@p35,@p36,@p37,@p38,@p39,@p40,@p41,@p42,@p43,@p44,@p45,@p46,@p47,@p48,@p49,@p50,@p51,@p52,@p53,@p54,@p55,@p56,@p57,@p58,@p59,@p60,@p61,@p62,@p63,@p64,@p65,@p66,@p67,@p68,@p69,@p70,@p71,@p72,@p73,@p74,@p75,@p76,@p77,@p78,@p79,@p80,@p81,@p82,@p83,@p84,@p85,@p86,@p87,@p88,@p89,@p90,@p91,@p92,@p93,@p94,@p95,@p96,@p97,@p98,@p99,@p100,@p101,@p102,@p103,@p104,@p105,@p106,@p107,@p108,@p109,@p110,@p111,@p112,@p113,@p114,@p115,@p116,@p117,@p118,@p119,@p120,@p121,@p122,@p123,@p124,@p125,@p126,@p127,@p128,@p129,@p130,@p131,@p132,@p133,@p134,@p135,@p136,@p137,@p138,@p139,@p140,@p141,@p142,@p143,@p144,@p145,@p146,@p147,@p148,@p149,@p150,@p151,@p152,@p153,@p154,@p155,@p156,@p157,@p158,@p159,@p160,@p161,@p162,@p163,@p164,@p165,@p166,@p167,@p168,@p169,@p170,@p171,@p172,@p173,@p174,@p175,@p176,@p177,@p178,@p179,@p180,@p181,@p182,@p183,@p184,@p185,@p186,@p187,@p188,@p189,@p190,@p191,@p192,@p193,@p194,@p195,@p196,@p197,@p198,@p199,@p200,@p201,@p202,@p203,@p204,@p205,@p206,@p207,@p208,@p209,@p210,@p211,@p212,@p213,@p214,@p215,@p216,@p217,@p218,@p219,@p220,@p221,@p222,@p223,@p224,@p225,@p226,@p227,@p228,@p229,@p230,@p231,@p232,@p233,@p234,@p235,@p236,@p237,@p238,@p239,@p240,@p241,@p242,@p243,@p244,@p245,@p246,@p247,@p248,@p249,@p250,@p251,@p252,@p253,@p254,@p255,@p256,@p257,@p258,@p259,@p260,@p261,@p262,@p263,@p264,@p265,@p266,@p267,@p268,@p269,@p270,@p271,@p272,@p273,@p274,@p275,@p276,@p277,@p278,@p279,@p280,@p281,@p282,@p283,@p284,@p285,@p286,@p287,@p288,@p289,@p290,@p291,@p292,@p293,@p294,@p295,@p296,@p297,@p298,@p299,@p300,@p301,@p302,@p303,@p304,@p305,@p306,@p307,@p308,@p309,@p310,@p311,@p312,@p313,@p314,@p315,@p316,@p317,@p318,@p319,@p320,@p321,@p322,@p323,@p324,@p325,@p326,@p327,@p328,@p329,@p330,@p331,@p332,@p333,@p334,@p335,@p336,@p337,@p338,@p339,@p340,@p341,@p342,@p343,@p344,@p345,@p346,@p347,@p348,@p349,@p350,@p351,@p352,@p353,@p354,@p355,@p356,@p357,@p358,@p359,@p360,@p361,@p362,@p363,@p364,@p365,@p366,@p367,@p368,@p369,@p370,@p371,@p372,@p373,@p374,@p375,@p376,@p377,@p378,@p379,@p380,@p381,@p382,@p383,@p384,@p385,@p386,@p387,@p388,@p389,@p390,@p391,@p392,@p393,@p394,@p395,@p396,@p397,@p398,@p399,@p400,@p401,@p402,@p403,@p404,@p405,@p406,@p407,@p408,@p409,@p410,@p411,@p412,@p413,@p414,@p415,@p416,@p417,@p418,@p419,@p420,@p421,@p422,@p423,@p424,@p425,@p426,@p427,@p428,@p429,@p430,@p431,@p432,@p433,@p434,@p435,@p436,@p437,@p438,@p439,@p440,@p441,@p442,@p443,@p444,@p445,@p446,@p447,@p448,@p449,@p450,@p451,@p452,@p453,@p454,@p455,@p456,@p457,@p458,@p459,@p460,@p461,@p462,@p463,@p464,@p465,@p466,@p467,@p468,@p469,@p470,@p471,@p472,@p473,@p474,@p475,@p476,@p477,@p478,@p479,@p480,@p481,@p482,@p483,@p484,@p485,@p486,@p487,@p488,@p489,@p490,@p491,@p492,@p493,@p494,@p495,@p496,@p497,@p498,@p499) AND f.Orgnr = @p500 ORDER BY e.dateEmployed DESC"));
		System.out.println(sn.normalizeStatementNoThrow("SELECT name, organizationNumber FROM customer.CustomerCompany WHERE organizationNumber = @p0"));
		System.out.println(sn.normalizeStatementNoThrow("SELECT name, organizationNumber FROM customer.CustomerCompany WHERE organizationNumber = @someName"));
		

		//System.out.println(sn.normalizeStatement("dump DATABASE master to \"sybackup::-SERV netbackup.maxm.se -CLIENT prod-a-ase.maxm.se -POL Sybase_ASE -SCHED Default-Application-Backup\""));
	}

	// public static void main(String[] args)
	// {
	// try
	// {
	// String sql ="SELECT NAME, ADDRESS, COL1 FROM USER WHERE SSN IN
	// ('11111111111111', '22222222222222');";
	// Select select = (Select) CCJSqlParserUtil.parse(sql);
	//
	// StringBuilder sb = new StringBuilder();
	// StatementDeParser stDeParser = new StatementDeParser(sb);
	//
	//
	// }
	// catch(Exception ex)
	// {
	// ex.printStackTrace();
	// }
	// }
}
