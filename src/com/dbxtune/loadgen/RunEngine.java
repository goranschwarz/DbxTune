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
package com.dbxtune.loadgen;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.loadgen.scenario.ProblemEvent;
import com.dbxtune.loadgen.scenario.RunContext;
import com.dbxtune.loadgen.scenario.Scenario;
import com.dbxtune.loadgen.scenario.Scenario.Kind;

/**
 * The "run" mode: worker threads running OLTP and OLAP/BATCH scenarios by weight, a scheduler that starts
 * the PROBLEM scenarios every N minutes, a per-minute journal flush, and a watchdog that ends everything in time.
 */
public class RunEngine
implements RunContext
{
	private static final Logger _logger = LogManager.getLogger();

	private final LoadGen _lg;
	private final Vendor  _v;
	private Journal _journal;
	private Stats   _stats;

	private int _customers, _products, _warehouses, _categories, _loginCnt;
	private final AtomicInteger _maxOrderId  = new AtomicInteger();
	private final AtomicInteger _nextLogin   = new AtomicInteger();

	private long _startTime, _endTime;
	private volatile boolean _stopping;
	private volatile boolean _finished;
	private long _waveStart, _waveEnd;
	private int  _thinkMinMs, _thinkMaxMs;

	private final Set<LgSession> _openSessions = ConcurrentHashMap.newKeySet();
	private final Set<String>    _runningProblems = ConcurrentHashMap.newKeySet();

	public RunEngine(LoadGen lg)
	{
		_lg = lg;
		_v  = lg.getVendor();
	}

	//------------------------------------------------------------------
	// RunContext
	//------------------------------------------------------------------
	@Override public LoadGen getLoadGen()    { return _lg; }
	@Override public Vendor  getVendor()     { return _v; }
	@Override public Random  rnd()           { return ThreadLocalRandom.current(); }
	@Override public int     getCustomers()  { return _customers; }
	@Override public int     getProducts()   { return _products; }
	@Override public int     getWarehouses() { return _warehouses; }
	@Override public int     getCategories() { return _categories; }
	@Override public int     getMaxOrderId() { return _maxOrderId.get(); }
	@Override public void    orderCreated(int orderId) { _maxOrderId.accumulateAndGet(orderId, Math::max); }
	@Override public boolean isStopping()    { return _stopping; }
	@Override public long    getRemainingMs(){ return Math.max(0, _endTime - System.currentTimeMillis()); }

	@Override
	public int randomCustomer()
	{
		return rnd().nextInt(100) < 5 ? 1 : 2 + rnd().nextInt(_customers - 1);
	}

	@Override
	public int randomProduct()
	{
		return rnd().nextInt(100) < 20 ? 1 : 1 + rnd().nextInt(_products);
	}

	@Override
	public int randomRecentOrder()
	{
		int max = _maxOrderId.get();
		return Math.max(1, max - rnd().nextInt(Math.max(1, max / 20)));
	}

	@Override
	public void sleep(long ms)
	{
		long end = System.currentTimeMillis() + ms;
		while (!_stopping)
		{
			long left = end - System.currentTimeMillis();
			if (left <= 0)
				return;
			try { Thread.sleep(Math.min(left, 500)); }
			catch (InterruptedException ex) { Thread.currentThread().interrupt(); return; }
		}
	}

	@Override
	public LgSession openSession(Scenario sc)
	throws Exception
	{
		LgSession s = openSessionForApp(LoadGen.APPNAME_PREFIX + ":" + sc.getName());
		s.setScenario(sc.getName());
		return s;
	}

	private LgSession openSessionForApp(String appName)
	throws Exception
	{
		String login = LoadGen.LOGIN_PREFIX + (1 + (_nextLogin.getAndIncrement() % _loginCnt));
		LgSession s = _lg.connectUser(login, appName, _stats);
		_openSessions.add(s);
		_openSessions.removeIf(LgSession::isClosed);
		return s;
	}

	@Override
	public long nextVal(LgSession s, String counterName)
	throws Exception
	{
		s.begin();
		try
		{
			s.update("/* dbxt:next_val */ update counter_table set next_val = next_val + 1 where name = ?", counterName);
			long val = ((Number) s.queryValue("/* dbxt:next_val */ select next_val from counter_table where name = ?", counterName)).longValue() - 1;
			s.commit();
			return val;
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}
	}

	//------------------------------------------------------------------
	// RUN
	//------------------------------------------------------------------
	public int run()
	throws Exception
	{
		Random rnd = new Random();
		int durationSec = LoadGen.parseDurationSec(_lg.getCmd().getOptionValue("duration", "15m"), rnd);
		String intensity = _lg.getCmd().getOptionValue("intensity", "low").toLowerCase();
		if (!Arrays.asList("low", "medium", "high").contains(intensity))
			throw new IllegalArgumentException("Unknown intensity '" + intensity + "', use: low, medium, high");

		int oltpThreads = _lg.getIntProp("intensity." + intensity + ".oltpThreads", intensity.equals("low") ? 4   : intensity.equals("medium") ? 10  : 25);
		int olapThreads = _lg.getIntProp("intensity." + intensity + ".olapThreads", intensity.equals("low") ? 1   : intensity.equals("medium") ? 2   : 4);
		_thinkMinMs     = _lg.getIntProp("intensity." + intensity + ".thinkMinMs",  intensity.equals("low") ? 200 : intensity.equals("medium") ? 50  : 0);
		_thinkMaxMs     = _lg.getIntProp("intensity." + intensity + ".thinkMaxMs",  intensity.equals("low") ? 1000: intensity.equals("medium") ? 500 : 100);
		if (_lg.getCmd().hasOption("threads"))
			oltpThreads = Integer.parseInt(_lg.getCmd().getOptionValue("threads"));

		//--- What is installed?
		Map<String, String> info = new HashMap<>();
		String dbmsVersion;
		try (LgSession s = _lg.connectUser(LoadGen.LOGIN_PREFIX + "1", LoadGen.APPNAME_PREFIX + ":init", null))
		{
			for (Object[] row : s.query("select k, v from dbxt_loadgen_info"))
				info.put((String) row[0], (String) row[1]);
			_maxOrderId.set(s.queryInt("select max(order_id) from orders"));
			dbmsVersion = s.getConnection().getMetaData().getDatabaseProductVersion();
		}
		catch (Exception ex)
		{
			String msg = "Can't read the install info as " + LoadGen.LOGIN_PREFIX + "1 in '" + _lg.getDbname() + "' on '" + _lg.getSrvName() + "'. Is it installed? (After 'uninstall --keep-database' get the logins back with 'install --logins-only'.) Caught: " + ex.getMessage();
			_logger.error(msg);
			return LoadGen.EXIT_NOT_AVAILABLE;
		}
		if (!"INSTALLED".equals(info.get("status")))
		{
			String msg = "The install status is '" + info.get("status") + "', not INSTALLED. Re-install (uninstall + install).";
			_logger.error(msg);
			return LoadGen.EXIT_NOT_AVAILABLE;
		}
		_customers  = Integer.parseInt(info.get("customers"));
		_products   = Integer.parseInt(info.get("products"));
		_warehouses = Integer.parseInt(info.get("warehouses"));
		_categories = Integer.parseInt(info.get("categories"));
		_loginCnt   = Integer.parseInt(info.get("logins"));

		//--- Read all login passwords now (once), not at every connect during the run
		for (int i = 1; i <= _loginCnt; i++)
			if (_lg.readPassword(LoadGen.LOGIN_PREFIX + i) == null)
				_logger.warn("No password for '" + LoadGen.LOGIN_PREFIX + i + "' on server '" + _lg.getSrvName() + "' in '" + _lg.getPasswdFile() + "'. Sessions using that login will fail.");

		//--- Scenarios
		List<Scenario> oltp = new ArrayList<>(), olap = new ArrayList<>(), problems = new ArrayList<>();
		Map<String, Integer> weight = new LinkedHashMap<>();
		Map<String, Integer> every  = new LinkedHashMap<>();
		List<String> only    = csv(_lg.getCmd().getOptionValue("scenarios"));
		List<String> exclude = csv(_lg.getCmd().getOptionValue("exclude"));
		for (Scenario sc : Scenario.all())
		{
			String n = sc.getName();
			if (!_lg.getBoolProp("scenario." + n + ".enabled", true))         continue;
			if (!only.isEmpty() && !only.contains(n))                         continue;
			if (exclude.contains(n))                                          continue;
			if (sc.getKind() == Kind.PROBLEM && _lg.getCmd().hasOption("no-problems")) continue;

			if (sc.getKind() == Kind.PROBLEM)
			{
				int e = _lg.getScenarioIntProp(sc, "everyMinutes", sc.getDefaultEveryMinutes());
				if (e <= 0) continue;
				every.put(n, e);
				problems.add(sc);
			}
			else
			{
				int w = _lg.getScenarioIntProp(sc, "weight", sc.getDefaultWeight());
				if (w <= 0) continue;
				weight.put(n, w);
				(sc.getKind() == Kind.OLTP ? oltp : olap).add(sc);
			}
		}
		if (oltp.isEmpty()) oltpThreads = 0;
		if (olap.isEmpty()) olapThreads = 0;

		//--- Journal
		String runId = UUID.randomUUID().toString().substring(0, 8);
		Journal.removeOldFiles(Integer.parseInt(_lg.getCmd().getOptionValue("journal-keep-days", "30")));
		_journal = new Journal(_lg.getSrvName(), "run", runId);
		_lg.setJournal(_journal);
		_stats = new Stats(_journal);

		_startTime = System.currentTimeMillis();
		_endTime   = _startTime + durationSec * 1000L;
		long rampMs = Math.min(60_000, durationSec * 1000L / 5);

		// A busier 5 minute "wave" somewhere in the middle (think time x 0.3), when the run is 15 minutes or longer
		if (durationSec >= 15 * 60)
		{
			_waveStart = _startTime + rampMs + (long) (rnd.nextDouble() * (durationSec * 1000L - rampMs - 6 * 60_000));
			_waveEnd   = _waveStart + 5 * 60_000;
		}

		_journal.event("header", "kind", "run", "runId", runId, "server", _lg.getSrvName(), "vendor", _v.getName(),
				"host", _lg.getHost(), "port", _lg.getPort(), "dbmsVersion", dbmsVersion, "dbname", _lg.getDbname(),
				"toolVersion", Version.getVersionStr(), "installInfo", info.entrySet().stream().filter(e -> !e.getKey().startsWith("login.")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
				"durationSec", durationSec, "plannedEnd", Journal.ts(_endTime),
				"intensity", intensity, "oltpThreads", oltpThreads, "olapThreads", olapThreads, "thinkMs", _thinkMinMs + "-" + _thinkMaxMs,
				"waveStart", _waveStart == 0 ? null : Journal.ts(_waveStart),
				"waveEnd",   _waveEnd   == 0 ? null : Journal.ts(_waveEnd),
				"weights", weight, "problemEveryMinutes", every,
				"sqlTag", "every statement starts with /* dbxt:<scenario> */",
				"appNames", LoadGen.APPNAME_PREFIX + ":oltp, " + LoadGen.APPNAME_PREFIX + ":olap, " + LoadGen.APPNAME_PREFIX + ":<problem scenario>");

		String startMsg = "Run " + runId + " on '" + _lg.getSrvName() + "': " + durationSec + " s (ends ~" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(_endTime)) + "), intensity=" + intensity + ", oltpThreads=" + oltpThreads + ", olapThreads=" + olapThreads
				+ ", problems=" + every.keySet() + ". Journal: " + _journal.getFile();
		_logger.info(startMsg);

		Thread hook = new Thread(() -> {
			if (_finished)
				return;
			_logger.info("Shutdown requested (Ctrl-C / SIGTERM), stopping...");
			_stopping = true;
			long until = System.currentTimeMillis() + 70_000;
			while (!_finished && System.currentTimeMillis() < until)
			{
				try { Thread.sleep(200); } catch (InterruptedException ignore) { break; }
			}
		}, "lg-shutdown-hook");
		Runtime.getRuntime().addShutdownHook(hook);

		//--- Start threads
		List<Thread> workers = new ArrayList<>();
		for (int i=0; i<oltpThreads; i++)
			workers.add(startWorker("oltp", i, oltp, weight, i * rampMs / Math.max(1, oltpThreads)));
		for (int i=0; i<olapThreads; i++)
			workers.add(startWorker("olap", i, olap, weight, rampMs / 2 + i * rampMs / Math.max(1, olapThreads)));

		Thread scheduler = new Thread(() -> problemScheduler(problems, every, rampMs), "lg-problem-scheduler");
		scheduler.setDaemon(true);
		scheduler.start();

		//--- Wait for the end, flush the journal every minute
		long nextFlush = (System.currentTimeMillis() / 60_000 + 1) * 60_000;
		while (!_stopping && System.currentTimeMillis() < _endTime)
		{
			Thread.sleep(Math.min(500, Math.max(1, _endTime - System.currentTimeMillis())));
			if (System.currentTimeMillis() >= nextFlush)
			{
				_stats.flushMinute();
				nextFlush += 60_000;
				long left = Math.max(0, getRemainingMs()) / 1000;
				_logger.info(String.format("Run progress: %d%% done, %d min %02d s left, ends ~%s; %d executions, %d errors",
						(int) ((System.currentTimeMillis() - _startTime) * 100 / Math.max(1, _endTime - _startTime)), left / 60, left % 60,
						new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(_endTime)), _stats.getTotalCount(), _stats.getTotalErrors()));
			}
		}

		//--- Stop: give everything 60 s, then the watchdog cancels and closes what is left
		_stopping = true;
		_logger.info("End of run, stopping workers...");
		long deadline = System.currentTimeMillis() + 60_000;
		for (Thread t : workers)
			t.join(Math.max(1, deadline - System.currentTimeMillis()));
		while (!_runningProblems.isEmpty() && System.currentTimeMillis() < deadline)
			Thread.sleep(200);

		List<String> leftovers = new ArrayList<>();
		for (LgSession s : _openSessions)
		{
			if (!s.isClosed())
			{
				leftovers.add(s.getId() + " (" + s.getScenario() + ")");
				s.cancelAndClose();
			}
		}
		if (!leftovers.isEmpty())
		{
			_logger.warn("Watchdog closed " + leftovers.size() + " sessions that did not stop in time: " + leftovers);
			_journal.event("info", "msg", "Watchdog closed sessions that did not stop in time: " + leftovers);
		}
		for (Thread t : workers)
			t.join(5_000);

		_stats.flushMinute();

		//--- Summary
		List<Map<String, Object>> summary = _stats.getSummary();
		long actualSec = (System.currentTimeMillis() - _startTime) / 1000;
		_journal.event("summary", "scenarios", summary, "executions", _stats.getTotalCount(), "errors", _stats.getTotalErrors(), "actualDurationSec", actualSec, "leftoverSessions", leftovers.size());

		StringBuilder sb = new StringBuilder();
		sb.append("Run ").append(runId).append(" done in ").append(actualSec).append(" s: ").append(_stats.getTotalCount()).append(" executions, ").append(_stats.getTotalErrors()).append(" errors.");
		for (Map<String, Object> m : summary)
			sb.append(String.format("%n   %-18s count=%-7s errors=%-5s avgMs=%-6s p95Ms=%-6s maxMs=%-7s%s", m.get("scenario"), m.get("count"), m.get("errors"), m.get("avgMs"), m.get("p95Ms"), m.get("maxMs"),
					m.get("lastError") == null ? "" : " lastError=" + m.get("lastError")));
		_logger.info(sb.toString()); // the log also goes to the console, so no System.out here (that printed the summary twice)

		saveRunlog(runId, intensity, oltpThreads + olapThreads, summary);

		_finished = true;
		try { Runtime.getRuntime().removeShutdownHook(hook); } catch (IllegalStateException ignore) {}
		return LoadGen.EXIT_OK;
	}

	private static List<String> csv(String str)
	{
		List<String> list = new ArrayList<>();
		if (str != null)
			for (String s : str.split(","))
				if (!s.isBlank())
					list.add(s.trim());
		return list;
	}

	private void saveRunlog(String runId, String intensity, int threads, List<Map<String, Object>> summary)
	{
		String txt = summary.stream().map(m -> m.get("scenario") + "=" + m.get("count") + "/" + m.get("errors")).collect(Collectors.joining(", "));
		if (txt.length() > 3900)
			txt = txt.substring(0, 3900) + "...";
		try (LgSession s = _lg.connectUser(LoadGen.LOGIN_PREFIX + "1", LoadGen.APPNAME_PREFIX + ":runlog", null))
		{
			s.update("insert into dbxt_loadgen_runlog (run_id, start_ts, end_ts, server_name, intensity, threads, exec_count, error_count, summary) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					runId, new Timestamp(_startTime), new Timestamp(System.currentTimeMillis()), _lg.getSrvName(), intensity, threads, _stats.getTotalCount(), _stats.getTotalErrors(), txt);
		}
		catch (Exception ex)
		{
			_logger.warn("Problems saving the run in dbxt_loadgen_runlog. Caught: " + ex);
		}
	}

	//------------------------------------------------------------------
	// Workers
	//------------------------------------------------------------------
	private Thread startWorker(String type, int num, List<Scenario> scenarios, Map<String, Integer> weight, long startDelayMs)
	{
		int total = scenarios.stream().mapToInt(sc -> weight.get(sc.getName())).sum();
		Thread t = new Thread(() -> {
			sleep(startDelayMs);
			LgSession s = null;
			while (!_stopping)
			{
				try
				{
					if (s == null || s.isConnectionLost())
						s = openSessionForApp(LoadGen.APPNAME_PREFIX + ":" + type);
				}
				catch (Exception ex)
				{
					_logger.warn("Worker " + type + "-" + num + ": can't connect (retry in 10 s). Caught: " + ex.getMessage());
					_journal.event("error", "msg", "Worker " + type + "-" + num + " can't connect: " + ex.getMessage());
					sleep(10_000);
					continue;
				}

				// Pick by weight
				int pick = rnd().nextInt(total);
				Scenario sc = scenarios.get(scenarios.size() - 1);
				for (Scenario c : scenarios)
				{
					pick -= weight.get(c.getName());
					if (pick < 0) { sc = c; break; }
				}

				s.setScenario(sc.getName());
				s.setQueryTimeout(type.equals("oltp") ? 60 : 600);
				long start = System.currentTimeMillis();
				String error = null;
				try
				{
					sc.execute(this, s);
				}
				catch (Exception ex)
				{
					error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
					s.rollbackQuietly();
					if (_stopping || s.isCancelled())
						break;
					boolean connLost = s.isConnectionLost() || (ex instanceof SQLException && String.valueOf(((SQLException) ex).getSQLState()).startsWith("08"));
					_logger.debug("Worker " + type + "-" + num + " scenario " + sc.getName() + " failed: " + error);
					if (connLost)
					{
						_logger.warn("Worker " + type + "-" + num + ": connection lost (" + error + "), reconnecting in 5 s.");
						s.close();
						s = null;
						sleep(5_000);
					}
				}
				_stats.recordUnit(sc.getName(), System.currentTimeMillis() - start, error);

				// Think time (shorter during the "wave")
				long now = System.currentTimeMillis();
				double factor = (now >= _waveStart && now < _waveEnd) ? 0.3 : 1.0;
				int think = _thinkMinMs + (_thinkMaxMs > _thinkMinMs ? rnd().nextInt(_thinkMaxMs - _thinkMinMs) : 0);
				if (type.equals("olap"))
					think = think * 5 + 2000;
				sleep((long) (think * factor));
			}
			if (s != null)
				s.close();
		}, "lg-" + type + "-" + num);
		t.start();
		return t;
	}

	//------------------------------------------------------------------
	// Problem scheduler
	//------------------------------------------------------------------
	private void problemScheduler(List<Scenario> problems, Map<String, Integer> every, long rampMs)
	{
		Map<String, Long> next = new HashMap<>();
		for (Scenario sc : problems)
			next.put(sc.getName(), _startTime + rampMs + (long) (rnd().nextDouble() * every.get(sc.getName()) * 60_000L));

		while (!_stopping)
		{
			long now = System.currentTimeMillis();
			for (Scenario sc : problems)
			{
				String n = sc.getName();
				if (now < next.get(n))
					continue;
				next.put(n, next.get(n) + every.get(n) * 60_000L);

				if (getRemainingMs() < 30_000)
					continue; // too late to start something new
				if (!_runningProblems.add(n))
				{
					_logger.info("Problem '" + n + "' is still running from last time, skipping this one.");
					continue;
				}
				Thread t = new Thread(() -> runProblem(sc), "lg-problem-" + n);
				t.setDaemon(true);
				t.start();
			}
			sleep(1000);
		}
	}

	private void runProblem(Scenario sc)
	{
		ProblemEvent ev = new ProblemEvent();
		long start = System.currentTimeMillis();
		String error = null;
		_logger.info("Problem '" + sc.getName() + "' starting.");
		try
		{
			sc.runProblem(this, ev);
		}
		catch (Exception ex)
		{
			error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
			ev.setError(error);
			if (!_stopping)
				_logger.warn("Problem '" + sc.getName() + "' failed. Caught: " + error);
		}
		finally
		{
			ev.end();
			_journal.write(ev.toMap(sc.getName(), sc.getExpected(_v)));
			_stats.recordUnit(sc.getName(), System.currentTimeMillis() - start, error != null ? error : ev.getError());
			_runningProblems.remove(sc.getName());
			_logger.info("Problem '" + sc.getName() + "' done in " + ((System.currentTimeMillis() - start) / 1000) + " s" + (ev.getError() == null ? "." : ", error: " + ev.getError()));
		}
	}
}
