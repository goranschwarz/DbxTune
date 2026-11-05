/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.sqlserver;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

public class CmActiveStPlanStatsCompletedPctTest
{
	@Test
	public void testNormalValues()
	{
		assertEquals(new BigDecimal("50.0"),  CmActiveStPlanStats.getCompletedPct(50, 100));
		assertEquals(new BigDecimal("33.3"),  CmActiveStPlanStats.getCompletedPct(1, 3));
		assertEquals(new BigDecimal("350.0"), CmActiveStPlanStats.getCompletedPct(350, 100));
	}

	/** The value from the error in the log: Value too long for column "completed_pct NUMERIC(10, 1)": "1080269500.0 (11)" */
	@Test
	public void testTooLowEstimateFitsInTheColumn() throws Exception
	{
		BigDecimal pct = CmActiveStPlanStats.getCompletedPct(10_802_695, 1);
		assertEquals(new BigDecimal("999999999.9"), pct);

		try (Connection h2 = DriverManager.getConnection("jdbc:h2:mem:completedPctTest"); Statement st = h2.createStatement())
		{
			st.executeUpdate("create table t (completed_pct NUMERIC(10, 1))");
			try (PreparedStatement ps = h2.prepareStatement("insert into t values(?)"))
			{
				ps.setBigDecimal(1, pct);
				ps.addBatch();
				ps.executeBatch();
			}
			try (ResultSet rs = st.executeQuery("select completed_pct from t"))
			{
				rs.next();
				assertEquals(pct, rs.getBigDecimal(1));
			}
		}
	}
}
