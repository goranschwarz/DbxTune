/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.loadgen.scenario;

import java.util.Random;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.LoadGen;
import com.dbxtune.loadgen.Vendor;

/**
 * What a scenario can use while running (implemented by the RunEngine).
 */
public interface RunContext
{
	LoadGen getLoadGen();
	Vendor  getVendor();

	/** A random generator for the calling thread */
	Random  rnd();

	int getCustomers();
	int getProducts();
	int getWarehouses();
	int getCategories();

	/** The highest order id (grows when NewOrder runs) */
	int  getMaxOrderId();
	void orderCreated(int orderId);

	/** A customer id: customer 1 (the "big" one) now and then, otherwise any */
	int randomCustomer();
	/** A product id: product 1 (the "hot" one) now and then, otherwise any */
	int randomProduct();
	/** An order id among the newest ~5% */
	int randomRecentOrder();

	/** true when the run is ending: stop as soon as possible */
	boolean isStopping();
	/** milliseconds left of the run */
	long getRemainingMs();
	/** sleep, but return early when the run is ending */
	void sleep(long ms);

	/**
	 * Open a new session for a problem scenario: next login from the pool, program name "dbxt-loadgen:{scenario}".
	 * The caller must close it.
	 */
	LgSession openSession(Scenario sc) throws Exception;

	/**
	 * Get the next value of a counter in counter_table, in its own short transaction.
	 * The session must not be in a transaction.
	 */
	long nextVal(LgSession s, String counterName) throws Exception;
}
