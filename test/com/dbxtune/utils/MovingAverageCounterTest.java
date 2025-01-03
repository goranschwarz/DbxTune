/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.dbxtune.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MovingAverageCounterTest
{

	@Test
	public void basic()
	{
		MovingAverageCounter avg1m = MovingAverageCounterManager.getInstance("basic", "basic-test-1m", 1);
		
		double val = 1;
		
		avg1m.add(val++); // 1
		avg1m.add(val++); // 2
		avg1m.add(val++); // 3
		avg1m.add(val++); // 4
		avg1m.add(val++); // 5
		avg1m.add(val++); // 6
		avg1m.add(val++); // 7
		avg1m.add(val++); // 8
		avg1m.add(val++); // 9
		avg1m.add(val++); // 10

		double avg = avg1m.getAvg(); // 1+2+3+4+5+6+7+8+9+10 / 10 ==>> 5.5
		
		assertEquals(5.5d, avg, 0.0);
	}

	@Test
	public void noValues()
	{
		MovingAverageCounter avg1m = MovingAverageCounterManager.getInstance(null, "noValues-test-1m", 1);
		
		double avg = avg1m.getAvg(); // should return 0
		
		assertEquals(0, avg, 0.0);
	}

	@Test
	public void periodNotReached()
	{
		MovingAverageCounter avg1m = MovingAverageCounterManager.getInstance("", "periodNotReached-test-1m", 1);
		
		double val = 1;
		
		avg1m.add(val++); // 1
		avg1m.add(val++); // 2
		
		double avg = avg1m.getAvg(-1, true); // 1+2 / 2 ==>> 1.5 ... but period has not been reached, so return -1
		
		assertEquals(-1d, avg, 0.0);
	}

	@Test
	public void multi()
	{
		System.out.println(">> BEGIN: MovingAverageCounterTest.multi() NOTE: test will take approx 61 seconds");

		MovingAverageCounter avg1m = MovingAverageCounterManager.getInstance("multi", "multi-test-1m", 1);

		//-----------------------------------------
		// No values
		double avg = avg1m.getAvg(-1); // NO entries, return -1
		assertEquals(-1d, avg, 0.0);


		//-----------------------------------------
		// values before "period has been reached" 
		avg1m.add(1);
		avg1m.add(2);
		
		avg = avg1m.getAvg(); // 1+2 / 2 ==>> 1.5
		assertEquals(1.5, avg, 0.0);

		// Sleep 30 seconds, then add new values
		try { Thread.sleep(30 * 1000); } catch (InterruptedException ignore) {}

		avg1m.add(3);
		avg = avg1m.getAvg(); // 1+2+3 / 3 ==>> 2.0
		assertEquals(2.0, avg, 0.0);

		// Sleep 31 seconds (passing 1 minute span)... so values 1 and 2 should disappear
		try { Thread.sleep(31 * 1000); } catch (InterruptedException ignore) {}

		//-----------------------------------------
		// values after(values: 1 & 2) entries has been "aged out" 
		avg = avg1m.getAvg(); // 3 / 1 ==>> 3.0   (values 1 and 2 should have been aged out)
		assertEquals(3.0, avg, 0.0);

		System.out.println(" < -END-: MovingAverageCounterTest.multi()");
	}

}
