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
/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License v3.
 ******************************************************************************/
package com.dbxtune.utils;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResult;
import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResultType;
import com.dbxtune.pcs.report.content.SparklineJfreeChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code SpaceForecast} is the top-level orchestrator for disk and database space
 * capacity forecasting within the DbxTune monitoring platform.
 *
 * <p>It ingests historical free-space time-series data (typically 30 days of per-minute
 * or downsampled hourly samples) from a DbxTune central database, applies a multi-stage
 * statistical pipeline, and produces a {@link SpaceForecastResult} per monitored mount
 * point or database. Each result describes the current health state, fill-rate trend,
 * estimated time-to-full (ETA), and a confidence rating for that estimate.</p>
 *
 *
 * <h2>Lifecycle &amp; Fluent API</h2>
 * <p>Instances are built with a fluent builder-style API and execution is triggered
 * explicitly via {@link #doForecast()}:</p>
 * <pre>{@code
 * SpaceForecast forecast = new SpaceForecast(conn, srvName, cmName, graphName)
 *         .setDays(30)
 *         .setDownsamplePeriod(60)        // bucket raw samples into 60-min averages
 *         .setCriticalThresholdDays(7)
 *         .setWarningThresholdDays(14)
 *         .doForecast();
 *
 * List<SpaceForecastResult> criticals = forecast.getSeverityCriticals();
 * String html = forecast.generateHtmlReport(true);
 * String json = forecast.toJson();
 * }</pre>
 *
 * <p>Results can be serialised to JSON and deserialised back via {@link #toJson()} /
 * {@link #fromJson(String)}, allowing cached forecasts to be re-rendered without
 * re-querying the database.</p>
 *
 *
 * <h2>Statistical Pipeline (inside {@code SpaceForecastEngine})</h2>
 * <p>The inner {@code SpaceForecastEngine} class executes the analysis in four
 * sequential phases:</p>
 *
 * <ol>
 *   <li><b>Data loading &amp; downsampling.</b> Raw samples are fetched for a
 *       configurable look-back window ({@code DiskForecast.sample.days}, default 30 days)
 *       and bucketed into fixed-width time windows (default 60 min) to reduce noise and
 *       smooth measurement jitter. Series whose most recent sample is older than
 *       {@code DiskForecast.staleThresholdHours} (default 48 h) are silently discarded,
 *       preventing phantom alerts for decommissioned disks or dropped databases.</li>
 *
 *   <li><b>Baseline linear regression.</b> Ordinary least-squares regression is applied
 *       over smoothed free-space values (MB) versus elapsed hours, yielding:
 *       <ul>
 *         <li>{@code slopeMbPerHour} &mdash; average rate of free-space change per hour
 *             (negative&nbsp;=&nbsp;filling up, positive&nbsp;=&nbsp;space being
 *             reclaimed);</li>
 *         <li>{@code acceleration} &mdash; second-order signal indicating whether the
 *             fill rate is speeding up or slowing down;</li>
 *         <li>{@code r2} (R&sup2;) &mdash; coefficient of determination used as a
 *             <em>confidence qualifier</em>, not a hard gate. A high R&sup2; yields an
 *             {@code EtaConfidence.HIGH} label; a weak signal degrades confidence to
 *             {@code MEDIUM} or {@code LOW} without suppressing the ETA entirely.</li>
 *       </ul>
 *       When both slope and R&sup2; are meaningful, a linear depletion ETA
 *       ({@code hoursToFull}) is projected forward from the current free space.</li>
 *
 *   <li><b>Temporary-usage (TMP) dip detection.</b> The algorithm scans the time-series
 *       for local minima that drop significantly below their neighbouring samples
 *       (controlled by {@code DiskForecast.dipNeighborDropFactor}, default 5&times; the
 *       {@code cleanupJumpMbThreshold} of 500 MB). Each detected dip represents a burst
 *       of temporary file activity &mdash; nightly ETL jobs, database backups, sort
 *       spills, etc. The <em>effective drop magnitude</em> is computed as:
 *       <pre>
 *       effectiveDrop = max( P95 drop over full look-back window,
 *                            max drop observed in the most recent 7 days )
 *       </pre>
 *       This hybrid strategy resists being poisoned by historical outliers while
 *       reacting quickly to new, larger usage regimes.</li>
 *
c *
 *
 * <h2>Output &amp; Reporting</h2>
 * <p>Each {@link SpaceForecastResult} carries the full analytical context: health state,
 * slope, R&sup2;, ETA in hours and days, ETA confidence, TMP dip statistics (count,
 * min/avg/max drop, overflow ETA), first/last free-space snapshots, total-size change
 * events, and an embedded sparkline chart (via {@code SparklineJfreeChart}) for inline
 * HTML rendering.</p>
 *
 * <p>The inner {@code Report} class assembles these results into a self-contained HTML
 * table &mdash; optionally a full HTML page &mdash; with colour-coded severity rows and
 * sparklines. Reports can also be written to a file and opened in the system browser via
 * {@link #generateHtmlReport(String)}.</p>
 *
 * <p>Convenience filter methods split results by severity at both the instance and
 * static level:</p>
 * <ul>
 *   <li>{@link #getSeverityCriticals()} / {@link #getSeverityCriticals(List)}</li>
 *   <li>{@link #getSeverityWarnings()}  / {@link #getSeverityWarnings(List)}</li>
 *   <li>{@link #getSeverityOthers()}    / {@link #getSeverityOthers(List)}</li>
 * </ul>
 *
 *
 * <h2>Configuration</h2>
 * <p>All tuning parameters are externalised through DbxTune's {@code Configuration}
 * system and exposed as {@code PROPKEY_*} / {@code DEFAULT_*} constants, making every
 * knob overridable via a properties file without recompilation. Key parameters:</p>
 * <ul>
 *   <li>{@code DiskForecast.sample.days} (default 30) &mdash; look-back window in
 *       days;</li>
 *   <li>{@code DiskForecast.criticalThresholdDays} (default 7) &mdash; critical alert
 *       horizon;</li>
 *   <li>{@code DiskForecast.warningThresholdDays} (default 14) &mdash; warning alert
 *       horizon;</li>
 *   <li>{@code DiskForecast.cleanupJumpMbThreshold} (default 500 MB) &mdash; minimum
 *       drop size that qualifies as a cyclic cleanup event;</li>
 *   <li>{@code DiskForecast.anomalyZScoreThreshold} (default 3.0&sigma;) &mdash;
 *       z-score cutoff for outlier rejection;</li>
 *   <li>{@code SpaceForecast.criticalThresholdHours} / {@code warningThresholdHours}
 *       &mdash; fine-grained hour-level overrides that supersede the day-level
 *       defaults above.</li>
 * </ul>
 *
 *
 * <h2>Design Constraints &amp; Guarantees</h2>
 * <ul>
 *   <li>Fully stateless per {@link #doForecast()} call &mdash; no cross-run memory or
 *       accumulated state.</li>
 *   <li>Slope-based ETA is suppressed when regression quality is too poor to be credible,
 *       preventing misleading alerts on noisy or irregular series.</li>
 *   <li>Cyclic and linear depletion behaviours are modelled independently; a disk that
 *       is cleaned up nightly will not trigger a spurious CRITICAL from slope alone.</li>
 *   <li>Does <em>not</em> enforce policy &mdash; it produces risk indicators only.
 *       Alerting decisions are delegated to the surrounding monitoring framework.</li>
 * </ul>
 *
 * @see SpaceForecastResult
 * @see DiskHealthState
 * @see EtaConfidence
 * @see CriticalReason
 *
 */
public class SpaceForecast
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static void openInBrowser(String f)
	{
		openInBrowser(new File(f));
	}
	private static void openInBrowser(File f)
	{
		File reportFile = f;//report.getReportFile();
		if (reportFile == null)
			return;

		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();
			if ( desktop.isSupported(Desktop.Action.BROWSE) )
			{
				try
				{
//					desktop.browse(rr.getUrl().toURI());
					desktop.browse(reportFile.toURI());
				}
				catch (Exception ex)
				{
					_logger.error("Problems when open the URL '"+reportFile+"'. Caught: "+ex, ex);
				}
			}
		}
	}

	private static class CheckEntry
	{
		public CheckEntry(String srvName, String cmName, String graphName)
		{
			this(srvName, cmName, graphName, null);
		}
		public CheckEntry(String srvName, String cmName, String graphName, String lableName)
		{
			this.srvName   = srvName;
			this.cmName    = cmName;
			this.graphName = graphName;
			this.labelName = lableName;
		}
		String srvName;
		String cmName;
		String graphName;
		String labelName;
	}

	public static void main(String[] args) throws Exception
	{
//		String url = "jdbc:h2:tcp://dbxtune-vm.maxm.se/DBXTUNE_CENTRAL_DB;IFEXISTS=TRUE";
		String url = "jdbc:h2:tcp://192.168.0.164/DBXTUNE_CENTRAL_DB;IFEXISTS=TRUE";
		String username = "sa";
		String password = "";

		Connection jdbcConn = DriverManager.getConnection(url, username, password);
		DbxConnection conn = DbxConnection.createDbxConnection(jdbcConn);

		int days = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sampleDays, 30);
		int downsample = 60;
//		int downsample = 10;

		List<CheckEntry> list = new ArrayList<>();

//		list.add( new CheckEntry("MM-GCP-DW", "CmOsDiskSpace", "FsAvailableMb") );
//		list.add( new CheckEntry("MM-GCP-DW", "CmDatabases",   "DbDataSizeLeftMbGraph") );
//		list.add( new CheckEntry("MM-GCP-DW", "CmDatabases",   "DbLogSizeLeftMbGraph") );

//		list.add( new CheckEntry("MM-OP-SQL", "CmOsDiskSpace", "FsAvailableMb") );
//		list.add( new CheckEntry("MM-OP-SQL", "CmDatabases",   "DbDataSizeLeftMbGraph") );
//		list.add( new CheckEntry("MM-OP-SQL", "CmDatabases",   "DbLogSizeLeftMbGraph") );

//		list.add( new CheckEntry("MM-OP-JVSDB", "CmOsDiskSpace", "FsAvailableMb") ); 
//		list.add( new CheckEntry("MM-OP-JVSDB", "CmDatabases",   "DbDataSizeLeftMbGraph") );
//		list.add( new CheckEntry("MM-OP-JVSDB", "CmDatabases",   "DbLogSizeLeftMbGraph") );

//		list.add( new CheckEntry("PROD_A2_ASE", "CmOsDiskSpace",   "FsAvailableMb") );
//		list.add( new CheckEntry("PROD_A2_ASE", "CmOpenDatabases", "DbDataSizeLeftMbGraph") );
//		list.add( new CheckEntry("PROD_A2_ASE", "CmOpenDatabases", "DbLogSizeLeftMbGraph") );

//		list.add( new CheckEntry("prod-a1-psql", "CmOsDiskSpace", "FsAvailableMb") );

		list.add( new CheckEntry("DbxcLocalMetrics", "CmOsDiskSpace", "FsAvailableMb") );
		
//		for (CheckEntry entry : list)
//		{
//			System.out.println("");
//			System.out.println("==== detecting: srvName='" + entry.srvName + "', cmName=='" + entry.cmName + "', graphName='" + entry.graphName + "' ====");
//
//			SpaceForecastEngine engine = new SpaceForecastEngine(conn, entry.srvName, entry.cmName, entry.graphName);
//			List<SpaceForecastResult> results = engine.runForecast(days, downsample);
//
//			System.out.println("==== Forecast Results ====");
//			for (SpaceForecastResult r : results)
//			{
//				System.out.println(r);
//			}
//
//			String filename = "C:/tmp/disk-report." + entry.srvName + "." + entry.cmName + "." + entry.graphName + ".html";
//			System.out.println("Writing HTML to file: " + filename);
//			engine.generateHtmlReport(results, filename);
//			
//			String json = SpaceForecastResult.toJson(results);
//			System.out.println("JSON:" + json);
//
//			ObjectMapper mapper = new ObjectMapper();
//			List<SpaceForecastResult> javaObj = mapper.readValue(json, new TypeReference<List<SpaceForecastResult>>() {});
//
//			String filename2 = "C:/tmp/disk-report." + entry.srvName + "." + entry.cmName + "." + entry.graphName + ".xxx.html";
//			System.out.println("Writing HTML to file: " + filename2);
//			engine.generateHtmlReport(javaObj, filename2);
//		}
		for (CheckEntry entry : list)
		{
			System.out.println("");
			System.out.println("==== detecting: srvName='" + entry.srvName + "', cmName=='" + entry.cmName + "', graphName='" + entry.graphName + "' ====");

			SpaceForecast forcast = new SpaceForecast(conn, entry.srvName, entry.cmName, entry.graphName)
					.setDays(days)
					.setDownsamplePeriod(downsample)
					.doForecast();

			String filename = "C:/tmp/disk-report." + entry.srvName + "." + entry.cmName + "." + entry.graphName + ".html";
			System.out.println("Writing HTML to file: " + filename);
			forcast.generateHtmlReport(filename);
			openInBrowser(filename);
			
			String json = forcast.toJson();
			System.out.println("JSON:" + json);

			SpaceForecast forcastFromJson = fromJson(json);

			String filename2 = "C:/tmp/disk-report." + entry.srvName + "." + entry.cmName + "." + entry.graphName + ".xxx.html";
			System.out.println("Writing HTML to file: " + filename2);
			forcastFromJson.generateHtmlReport(filename2);
			openInBrowser(filename2);
		}
	}

	public static final String  PROPKEY_tmpDipsMinThreshold = "SpaceForecast.tmpDipsMinThreshold";
	public static final int     DEFAULT_tmpDipsMinThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_tmpDipsMinThreshold, 2048);
	private int _tmpDipsMinThreshold = DEFAULT_tmpDipsMinThreshold;
	public int           getTmpDipsMinThreshold()              { return _tmpDipsMinThreshold; }
	public SpaceForecast setTmpDipsMinThreshold(int threshold) { _tmpDipsMinThreshold  = threshold; return this; }
	
	public static final String  PROPKEY_criticalThresholdHours = "SpaceForecast.criticalThresholdHours";
	public static final String  PROPKEY_warningThresholdHours  = "SpaceForecast.warningThresholdHours";

	public static final int     DEFAULT_criticalThresholdHours = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_criticalThresholdHours           , 7  * 24);
	public static final int     DEFAULT_warningThresholdHours  = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_warningThresholdHours            , 14 * 24);

	private int _criticalThresholdHours = DEFAULT_criticalThresholdHours;
	private int _warningThresholdHours  = DEFAULT_warningThresholdHours;
	
	public int getCriticalThresholdHours() { return _criticalThresholdHours; }
	public int getWarningThresholdHours () { return _warningThresholdHours;  }

	public SpaceForecast setCriticalThresholdHours(int criticalThresholdHours) { _criticalThresholdHours = criticalThresholdHours;     return this; }
	public SpaceForecast setCriticalThresholdDays (int criticalThresholdDays ) { _criticalThresholdHours = criticalThresholdDays * 24; return this; }
	public SpaceForecast setWarningThresholdHours (int warningThresholdHours ) { _warningThresholdHours  = warningThresholdHours;      return this; }
	public SpaceForecast setWarningThresholdDays  (int warningThresholdDays  ) { _warningThresholdHours  = warningThresholdDays  * 24; return this; }
	

	public static final String  PROPKEY_sparklineChartWidth = "SpaceForecast.sparklineChartWidth";
	public static final int     DEFAULT_sparklineChartWidth = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sparklineChartWidth, 800);
	private int _sparklineChartWidth = DEFAULT_sparklineChartWidth;
	public int           getSparklineChartWidth()          { return _sparklineChartWidth; }
	public SpaceForecast setSparklineChartWidth(int width) { _sparklineChartWidth  = width; return this; }
	
	private int _days             = DEFAULT_sampleDays;
	private int _downsamplePeriod = 60; // DEFAULT_downsamplePeriod;

	public SpaceForecast setDays(int days)
	{
		_days = days;
		return this;
	}
	public SpaceForecast setDownsamplePeriod(int downsamplePeriod)
	{
		_downsamplePeriod = downsamplePeriod;
		return this;
	}

	public int getDays() { return _days; }
	public int getDownsamplePeriod() { return _downsamplePeriod; }
	

	
	private SpaceForecastEngine _forecastEngine;
	private List<SpaceForecastResult> _forecastResults = new ArrayList<>();
//	public void addForecastResult(SpaceForecastResult result)
//	{
//		_forecastResults.add(result);
//	}
	public List<SpaceForecastResult> getForecastResult()
	{
		return _forecastResults;
	}

	private DbxConnection _conn;
	private String _srvName;
	private String _cmName;
	private String _graphName;
	
	public String getSrvName()   { return _srvName; }
	public String getCmName()    { return _cmName; }
	public String getGraphName() { return _graphName; }

	public void   setSrvName  (String str) { _srvName   = str; }
	public void   setCmName   (String str) { _cmName    = str; }
	public void   setGraphName(String str) { _graphName = str; }
	
	public SpaceForecast() {}
	public SpaceForecast(DbxConnection conn, String srvName, String cmName, String graphName) 
	{
		_conn      = conn;
		_srvName   = srvName;
		_cmName    = cmName;
		_graphName = graphName;
	}
	
	public SpaceForecast doForecast() 
	throws Exception
	{
		_forecastEngine = new SpaceForecastEngine(_conn, _srvName, _cmName, _graphName);

		List<SpaceForecastResult> results = _forecastEngine.runForecast( getDays(), getDownsamplePeriod() );
		_forecastResults.addAll(results);
		
		return this;
	}

	public void generateHtmlReport(String filename) 
	throws IOException
	{
		Report report = new Report();
		report.generateHtmlReport(_forecastResults, filename);
	}

	public String generateHtmlReport(boolean createFullHtml) 
	{
		Report report = new Report();
		return report.generateHtmlReport(_forecastResults, createFullHtml, null);
	}

	public String generateHtmlReport(boolean createFullHtml, String beforeText)
	{
		Report report = new Report();
		return report.generateHtmlReport(_forecastResults, createFullHtml, beforeText);
	}

	public static String generateHtmlReportBegin(String srvName, int days, int samplePeriod)
	{
		return new ReportHelper().generateHtmlReportBegin(srvName, days, samplePeriod);
	}
	public static String generateHtmlReportEnd()
	{
		return new ReportHelper().generateHtmlReportEnd();
	}
	

	
	public String toJson()
	throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
	
	public static SpaceForecast fromJson(String json) 
	throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		SpaceForecast forecast = mapper.readValue(json, SpaceForecast.class);
		
		return forecast;
	}

	
	
	// ETA confidence levels based on R�
	enum EtaConfidence 
	{ 
		HIGH, 
		MEDIUM, 
		LOW 
	}

	enum DiskHealthState
	{
		STABLE        ("Slope is near-flat with no imminent risk"), 
		FILLING       ("Negative slope but ETA is beyond all alert thresholds"), 
		ACCELERATING  ("Fill rate is measurably increasing over time"), 
		WARNING       ("Linear ETA or TMP overflow ETA falls within the warning window (default &le; 14 days)"), 
		CRITICAL      ("Linear ETA or TMP overflow ETA falls within the critical window (default &le; 7 days)"), 
		CYCLIC_CLEANUP("Series exhibits a sawtooth pattern consistent with periodic cleanup jobs; slope-based fill rate is considered unreliable in this context"), 
		TMP_FILE_RISK ("Temporary usage dips exist but the baseline trend alone does not trigger WARNING/CRITICAL; risk is driven by workloads that may overflow remaining headroom"), 
		ANOMALY       ("A single anomalous data point (z-score above the configured threshold, default 3.0&sigma;) was detected that skewed the regression");

		public final String description;

		private DiskHealthState(String description)
		{
			this.description = description;
		}
	}

	enum CriticalReason
	{
		SLOPE,      // baseline linear trend will fill disk
		TMP_FILES,  // tmp/backup dip will overflow disk
		NONE
	}

	public static final String  PROPKEY_sampleDays                        = "DiskForecast.sample.days";
	public static final String  PROPKEY_minSamples                        = "DiskForecast.minSamples";
	public static final String  PROPKEY_rollingWindowSize                 = "DiskForecast.rollingWindowSize";
//	public static final String  PROPKEY_r2Threshold                       = "DiskForecast.r2Threshold";
	public static final String  PROPKEY_anomalyZScoreThreshold            = "DiskForecast.anomalyZScoreThreshold";
	public static final String  PROPKEY_cleanupJumpMbThreshold            = "DiskForecast.cleanupJumpMbThreshold";
	public static final String  PROPKEY_criticalThresholdDays             = "DiskForecast.criticalThresholdDays";
	public static final String  PROPKEY_warningThresholdHDays             = "DiskForecast.warningThresholdDays";
	public static final String  PROPKEY_dipNeighborDropFactor             = "DiskForecast.dipNeighborDropFactor";  // NEW
	public static final String  PROPKEY_staleThresholdHours               = "DiskForecast.staleThresholdHours";
	public static final String  PROPKEY_zeroValueThresholdMb              = "DiskForecast.zeroValueThresholdMb";

	public static final int     DEFAULT_sampleDays                        = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_sampleDays                 , 30);
	public static final int     DEFAULT_minSamples                        = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_minSamples                 , 30);
	public static final int     DEFAULT_rollingWindowSize                 = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_rollingWindowSize          , 24);
//	public static final double  DEFAULT_r2Threshold                       = Configuration.getCombinedConfiguration().getDoubleProperty (PROPKEY_r2Threshold                , 0.6);
	public static final double  DEFAULT_anomalyZScoreThreshold            = Configuration.getCombinedConfiguration().getDoubleProperty (PROPKEY_anomalyZScoreThreshold     , 3.0);
	public static final double  DEFAULT_cleanupJumpMbThreshold            = Configuration.getCombinedConfiguration().getDoubleProperty (PROPKEY_cleanupJumpMbThreshold     , 500);
	public static final int     DEFAULT_criticalThresholdDays             = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_criticalThresholdDays      , 7);
	public static final int     DEFAULT_warningThresholdDays              = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_warningThresholdHDays      , 14);
	public static final double  DEFAULT_dipNeighborDropFactor             = Configuration.getCombinedConfiguration().getDoubleProperty (PROPKEY_dipNeighborDropFactor      , 5.0); // NEW: dip must be this many times cleanupJumpMbThreshold vs neighbors
	public static final int     DEFAULT_staleThresholdHours               = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_staleThresholdHours        , 48);  // discard if no data in last 48h
	public static final int     DEFAULT_zeroValueThresholdMb              = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_zeroValueThresholdMb       , 1);   // treat values <= 1 MB as "offline/invalid"

//	public static class SpaceForecastEngine
	public class SpaceForecastEngine
	{
		private final DbxConnection conn;
//		private final String schemaName;
//		private final String tableName;

		private final String srvName;
		private final String cmName;
		private final String graphName;

		private final int      minSamples             = DEFAULT_minSamples;
		private final int      rollingWindowSize      = DEFAULT_rollingWindowSize;
		// NOTE: R� is no longer used to suppress ETA � it qualifies confidence instead.
		// r2Threshold is kept for reference but classify() no longer gates on it.
//		private final double   r2Threshold            = 0.001; // effectively disabled as gate
		private final double   anomalyZScoreThreshold = DEFAULT_anomalyZScoreThreshold;
		private final double   cleanupJumpMbThreshold = DEFAULT_cleanupJumpMbThreshold;
		private final double   dipNeighborDropFactor  = DEFAULT_dipNeighborDropFactor; // NEW
		private final Duration criticalThreshold      = Duration.ofDays(DEFAULT_criticalThresholdDays);
		private final Duration warningThreshold       = Duration.ofDays(DEFAULT_warningThresholdDays);
		private final int      staleThresholdHours    = DEFAULT_staleThresholdHours;
		private final double   zeroValueThresholdMb   = DEFAULT_zeroValueThresholdMb;

//		DiskForecastEngine(DbxConnection conn, String schemaName, String tableName)
//		{
//			this.conn = conn;
//			this.schemaName = schemaName;
//			this.tableName  = tableName;
//		}

		public SpaceForecastEngine(DbxConnection conn, String srvName, String cmName, String graphName)
		{
			this.conn      = conn;
			this.srvName   = srvName;
			this.cmName    = cmName;
			this.graphName = graphName;
		}

//		public List<SpaceForecastResult> runForecast(Duration lookback, int downSampleInMinutes) 
		public List<SpaceForecastResult> runForecast(int days, int downSampleInMinutes) 
		throws Exception
		{
			Duration lookback = Duration.ofDays(days);

//			Map<String, List<DiskSample>> rawData = loadSamples(lookback);
//			Map<String, List<DiskSample>> data    = downsample(rawData, Duration.ofMinutes(60));
			Map<String, DiskInfoEntry> rawData = loadSamples(lookback);
			
			// Down sample -- Group in "buckets" of X Minutes
			Map<String, DiskInfoEntry> data = rawData;
			if (downSampleInMinutes >= 0)
				data = downSample(rawData, Duration.ofMinutes(downSampleInMinutes));

			List<SpaceForecastResult> results = new ArrayList<>();
//			for (Map.Entry<String, List<DiskSample>> e : data.entrySet())
			for (Entry<String, DiskInfoEntry> e : data.entrySet())
			{
				String           mountPoint = e.getKey();
				DiskInfoEntry    diskInfo   = e.getValue();
				List<DiskSample> samples    = diskInfo.samples;

				if (samples.size() < minSamples)
					continue;

				// Discard stale series - last sample is too old (disk/db was dropped)
				Instant lastSeen = samples.get(samples.size() - 1).timestamp;
				if (Duration.between(lastSeen, Instant.now()).toHours() > staleThresholdHours)
				{
//					_logger.info("Discarding stale series '" + mountPoint + "' - last sample: " + lastSeen + " (" + Duration.between(lastSeen, Instant.now()).toHours() + "h ago)");
					_logger.info("Discarding stale series '" + mountPoint + "' - last sample: " + lastSeen + " (" + Duration.between(lastSeen, Instant.now()).toHours() + "h ago)");
					continue;
				}

				// Compute and add
				SpaceForecastResult result = compute(mountPoint, diskInfo);
				if (result != null)
				{
					result.days                = days;
					result.downSampleInMinutes = downSampleInMinutes;

					results.add(result);
				}
			}
			return results;
		}

//		private Map<String, List<DiskSample>> loadSamples(Duration lookback) 
		private Map<String, DiskInfoEntry> loadSamples(Duration lookback) 
		throws Exception
		{
			// This is how a typical Graph Table looks like:
			// So We want to:
			//  - Skip some columns: SessionStartTime, CmSampleTime
			//  - Use: SessionSampleTime as the "timestamp" when data was collected
			//  - Use: Rest of the columns as a "disk-name" or "db-name" collection value 
			
			// select * from "SERVER_NAME"."CmOsDiskSpace_FsAvailableMb"
			// +----------------------+-----------------------+-----------------------+------+-------------+------------+---------------+------------+
			// |SessionStartTime      |SessionSampleTime      |CmSampleTime           |C:    |D: [SQL Data]|E: [SQL Log]|F: [SQL Backup]|G: [Temp DB]|
			// +----------------------+-----------------------+-----------------------+------+-------------+------------+---------------+------------+
			// |2026-02-13 23:03:21.91|2026-02-20 11:49:35.237|2026-02-20 11:49:36.125|59�263|    1�561�089|     354�599|      1�047�134|     503�690|
			// |2026-02-13 23:03:21.91|2026-02-20 11:50:07.203|2026-02-20 11:50:08.102|59�262|    1�561�089|     354�599|      1�047�134|     503�690|
			// |2026-02-13 23:03:21.91|2026-02-20 11:50:39.19 |2026-02-20 11:50:40.337|59�262|    1�561�089|     354�599|      1�047�134|     503�690|
			// |2026-02-13 23:03:21.91|2026-02-20 11:51:11.453|2026-02-20 11:51:12.44 |59�262|    1�561�089|     354�599|      1�047�134|     503�690|
			// |2026-02-13 23:03:21.91|2026-02-20 11:51:43.53 |2026-02-20 11:51:44.77 |59�262|    1�561�089|     354�599|      1�047�134|     503�690|
			// +----------------------+-----------------------+-----------------------+------+-------------+------------+---------------+------------+

			// select * from "SERVER_NAME"."CmDatabases_DbDataSizeLeftMbGraph"
			// +----------------------+-----------------------+-----------------------+----+---------+-------------+------------+-----+----------+----------+---------+--------+-----+-----+------------------+
			// |SessionStartTime      |SessionSampleTime      |CmSampleTime           |FPA |Gladiator|Datawarehouse|ReportServer|Shelf|Leveranser|Provanalys|Analytics|tempdb  |msdb |model|ReportServerTempDB|
			// +----------------------+-----------------------+-----------------------+----+---------+-------------+------------+-----+----------+----------+---------+--------+-----+-----+------------------+
			// |2026-02-13 23:03:21.91|2026-02-20 11:50:39.19 |2026-02-20 11:50:39.21 |56,9|     24,3|    387�602,7|        78,2|513,1|     238,1|        22|  4�156,4|20�444,6|492,9|  5,1|             188,4|
			// |2026-02-13 23:03:21.91|2026-02-20 11:51:11.453|2026-02-20 11:51:11.473|56,9|     24,3|    387�602,7|        78,2|513,1|     238,1|        22|  4�156,4|20�444,6|492,9|  5,1|             188,4|
			// |2026-02-13 23:03:21.91|2026-02-20 11:51:43.53 |2026-02-20 11:51:43.55 |56,9|     24,3|    387�602,7|        78,2|513,1|     238,1|        22|  4�156,4|20�444,5|492,9|  5,1|             188,4|
			// |2026-02-13 23:03:21.91|2026-02-20 11:52:15.847|2026-02-20 11:52:15.87 |56,9|     24,3|    387�602,7|        78,2|513,1|     238,1|        22|  4�156,4|20�444,6|492,9|  5,1|             188,4|
			// |2026-02-13 23:03:21.91|2026-02-20 11:52:47.82 |2026-02-20 11:52:47.843|56,9|     24,3|    387�602,7|        78,2|513,1|     238,1|        22|  4�156,4|20�444,5|492,9|  5,1|             188,4|
			// +----------------------+-----------------------+-----------------------+----+---------+-------------+------------+-----+----------+----------+---------+--------+-----+-----+------------------+

			
			String tsColumn = "SessionSampleTime";
			String tabName = "[" + cmName + "_" + graphName + "]";
			if (srvName != null && !srvName.isBlank())
				tabName = "[" + srvName + "]." + tabName;

			String sql =
				"SELECT * FROM " + tabName + " " +
				"WHERE [" + tsColumn + "] >= ? " +
				"ORDER BY [" + tsColumn + "]";

			sql = conn.quotifySqlString(sql);

			Instant from = Instant.now().minus(lookback);
			
			// Add 1 extra minute
			from = from.minus(1, ChronoUnit.MINUTES);

			// The return object
//			Map<String, List<DiskSample>> result = new HashMap<>();
			Map<String, DiskInfoEntry> result = new HashMap<>();

			Instant firstTs = null; // Remember first Timestamp, so we can get PCT and TotalSize
			Instant lastTs  = null; // Remember last  Timestamp, so we can get PCT and TotalSize

			try (PreparedStatement ps = conn.prepareStatement(sql))
			{
				ps.setTimestamp(1, Timestamp.from(from));

				try (ResultSet rs = ps.executeQuery())
				{
					ResultSetMetaData meta = rs.getMetaData();
					int cols = meta.getColumnCount();

					List<Integer> diskIndexes = new ArrayList<>();
					List<String>  diskNames   = new ArrayList<>();

					// Read columns that contains "disk/db" names
					for (int i = 1; i <= cols; i++)
					{
						String name = meta.getColumnLabel(i);
						if ("SessionSampleTime".equalsIgnoreCase(name)) continue;
						if ("SessionStartTime" .equalsIgnoreCase(name)) continue;
						if ("CmSampleTime"     .equalsIgnoreCase(name)) continue;

						diskIndexes.add(i);
						diskNames  .add(name);
					}

//					for (String d : diskNames)
//						result.put(d, new ArrayList<>());

					while (rs.next())
					{
						Instant ts = rs.getTimestamp(tsColumn).toInstant();

						lastTs = ts;
						if (firstTs == null)
							firstTs = ts;

						for (int i = 0; i < diskIndexes.size(); i++)
						{
							double value = rs.getDouble(diskIndexes.get(i));
							if ( ! rs.wasNull() )
							{
								String diskName = diskNames.get(i);
								
								DiskInfoEntry diskInfoEntry = result.computeIfAbsent(diskName, k -> new DiskInfoEntry(diskName));
								diskInfoEntry.addSample(ts, value);
							}
						}
					}
				}
			}

			// Get extra properties for each "disk/db"
			// This can/would be part of the report/output 
			//  - Last Total Size
			//  - Last Percent Usage
			boolean getExtraInfo = true;
			if (getExtraInfo)
			{
				String rawTableName;

				rawTableName = "noTableName";
				if (graphName.equals("FsAvailableMb"))         rawTableName = cmName + "_FsSizeMb";
				if (graphName.equals("DbDataSizeLeftMbGraph")) rawTableName = cmName + "_DbDataSizeMbGraph";
				if (graphName.equals("DbLogSizeLeftMbGraph"))  rawTableName = cmName + "_DbLogSizeMbGraph";

				if (DbUtils.checkIfTableExistsNoThrow(conn, null, srvName, rawTableName))
				{
					tabName = "[" + rawTableName + "]";
					if (srvName != null && !srvName.isBlank())
						tabName = "[" + srvName + "]." + tabName;

					sql = "SELECT 'first', * \n"
						+ "FROM " + tabName + " \n"
						+ "WHERE [" + tsColumn + "] = (SELECT MIN(s.[SessionSampleTime]) FROM " + tabName + " s WHERE s.[SessionSampleTime] >= ?) \n"

						+ "UNION ALL \n"

						+ "SELECT 'last', * \n"
						+ "FROM " + tabName + " \n"
						+ "WHERE [" + tsColumn + "] >= ? \n"

						+ "ORDER BY [" + tsColumn + "] \n"
						;

					sql = conn.quotifySqlString(sql);

					try (PreparedStatement ps = conn.prepareStatement(sql))
					{
						ps.setTimestamp(1, Timestamp.from(firstTs));
						ps.setTimestamp(2, Timestamp.from(lastTs));
						
						try (ResultSet rs = ps.executeQuery())
						{
							List<String> existingColNames = DbUtils.getColumnNames(rs.getMetaData());

							while(rs.next())
							{
								String type = rs.getString(1);

								for (String diskName : result.keySet())
								{
									// The disk name may not be part of this resultset... since it's another table
									if ( ! existingColNames.contains(diskName) )
										continue;
										
									Instant ts = rs.getTimestamp(tsColumn).toInstant();
									double val = rs.getDouble(diskName);
//System.out.println("SIZE: firstTs='" + firstTs + "', rawTableName='" + rawTableName + "', type='" + type + "', ts='" + ts + "', val='" + val + "'.");

									if ( ! rs.wasNull() )
									{
										DiskInfoEntry diskInfoEntry = result.get(diskName);
										if ("first".equals(type))
										{
											diskInfoEntry.firstTotalSizeMb   = val;
											diskInfoEntry.firstTotalSizeMbTs = ts;
										}

										if ("last".equals(type))
										{
											diskInfoEntry.lastTotalSizeMb   = val;
											diskInfoEntry.lastTotalSizeMbTs = ts;
										}
									}
								}
							}
						}
					}
					
					// Get "events" where data/log/os disk grows/shrinks
					// Foreach column... we need to execute a separate SQL
					boolean useManySqlStatements_and_returnFewerRows = false;
					if (useManySqlStatements_and_returnFewerRows)
					{
						Set<String> existingColNames = DbUtils.getColumnNamesNoThrow(conn, srvName, rawTableName);
						for (String name : existingColNames)
						{
							if ("SessionSampleTime".equalsIgnoreCase(name)) continue;
							if ("SessionStartTime" .equalsIgnoreCase(name)) continue;
							if ("CmSampleTime"     .equalsIgnoreCase(name)) continue;
							
							if (name.contains("]"))
								name = name.replace("]", "]]");

							sql = ""
								+ "WITH [chunks] AS ( \n"
								+ "    SELECT \n"
								+ "        '" + name + "' AS [dbname] \n"
								+ "        ,[" + name+ "]  AS [val] \n"
								+ "        ,[" + tsColumn + "] \n"
								+ "        ,LAG([" + name + "]) OVER (ORDER BY [" + tsColumn + "]) AS [prevVal] \n"
								+ "    FROM " + tabName + " \n"
								+ "    WHERE [" + tsColumn + "] >= ? \n"
								+ ") \n"
								+ "SELECT \n"
								+ "     [dbname] \n"
								+ "    ,[" + tsColumn + "] \n"
								+ "    ,[val] as [newSize] \n"
								+ "    ,[val] - [prevVal] AS [sizeDiff] \n"
								+ "FROM [chunks] \n"
								+ "WHERE [val] != [prevVal] \n"
								+ "ORDER BY [" + tsColumn + "] \n"
								;

							sql = conn.quotifySqlString(sql);

							try (PreparedStatement ps = conn.prepareStatement(sql))
							{
								ps.setTimestamp(1, Timestamp.from(firstTs));
								
								try (ResultSet rs = ps.executeQuery())
								{
									while (rs.next())
									{
										String    dbname     = rs.getString   (1);
										Timestamp ts         = rs.getTimestamp(2);
										int       newSizeMb  = rs.getInt      (3);
										int       sizeDiffMb = rs.getInt      (4);

										SizeChangedEvent sizeChangedEvent = new SizeChangedEvent(dbname, ts, newSizeMb, sizeDiffMb);
										
										DiskInfoEntry diskInfoEntry = result.get(name);
										if (diskInfoEntry != null)
										{
											diskInfoEntry.addSizeChangedEvent(sizeChangedEvent);
										}
									}
								}
							}
						}
					}
					else // Use ONE SQL Statement and do The logic on the Client side ... Which one is FASTEST ???
					{
						sql = "SELECT * \n"
								+ "FROM " + tabName + "\n"
								+ "WHERE [" + tsColumn + "] >= ? \n"
								+ "ORDER BY [" + tsColumn + "] \n"
								;
						sql = conn.quotifySqlString(sql);

						try (PreparedStatement ps = conn.prepareStatement(sql))
						{
							ps.setTimestamp(1, Timestamp.from(firstTs));
							
							try (ResultSet rs = ps.executeQuery())
							{
								ResultSetMetaData meta = rs.getMetaData();
								int colCount = meta.getColumnCount();

								// Discover DB/disk columns dynamically
								List<String> dbColumns = new ArrayList<>();
								for (int i = 1; i <= colCount; i++)
								{
									String name = meta.getColumnName(i);
									if ("SessionSampleTime".equalsIgnoreCase(name)) continue;
									if ("SessionStartTime" .equalsIgnoreCase(name)) continue;
									if ("CmSampleTime"     .equalsIgnoreCase(name)) continue;
									dbColumns.add(name);
								}

								Map<String, Integer> previousValues = new LinkedHashMap<>();

								while (rs.next())
								{
									Timestamp ts = rs.getTimestamp(tsColumn);

									for (String name : dbColumns)
									{
										int current = rs.getInt(name);
										
										if (previousValues.containsKey(name))
										{
											int previous = previousValues.get(name);
											
											if (current != previous)
											{
												int sizeDiffMb = current - previous;
												
												SizeChangedEvent sizeChangedEvent = new SizeChangedEvent(name, ts, current, sizeDiffMb);
												
												DiskInfoEntry diskInfoEntry = result.get(name);
												if (diskInfoEntry != null)
													diskInfoEntry.addSizeChangedEvent(sizeChangedEvent);
											}
										}

										previousValues.put(name, current);
									}
								}
							}
						}
					}
				}
			
				//  - Last Percent Usage
				rawTableName = "noTableName";
				if (graphName.equals("FsAvailableMb"))         rawTableName = cmName + "_FsUsedPct";
				if (graphName.equals("DbDataSizeLeftMbGraph")) rawTableName = cmName + "_DbDataSizeUsedPctGraph";
				if (graphName.equals("DbLogSizeLeftMbGraph"))  rawTableName = cmName + "_DbLogSizeUsedPctGraph";

				if (DbUtils.checkIfTableExistsNoThrow(conn, null, srvName, rawTableName))
				{
					tabName = "[" + rawTableName + "]";
					if (srvName != null && !srvName.isBlank())
						tabName = "[" + srvName + "]." + tabName;
					
					sql = "SELECT 'first', * \n"
						+ "FROM " + tabName + " \n"
						+ "WHERE [" + tsColumn + "] = (SELECT MIN(s.[SessionSampleTime]) FROM " + tabName + " s WHERE s.[SessionSampleTime] >= ?) \n"

						+ "UNION ALL \n"

						+ "SELECT 'last', * \n"
						+ "FROM " + tabName + " \n"
						+ "WHERE [" + tsColumn + "] >= ? \n"

						+ "ORDER BY [" + tsColumn + "] \n"
						;

					sql = conn.quotifySqlString(sql);

					try (PreparedStatement ps = conn.prepareStatement(sql))
					{
						ps.setTimestamp(1, Timestamp.from(firstTs));
						ps.setTimestamp(2, Timestamp.from(lastTs));
						
						try (ResultSet rs = ps.executeQuery())
						{
							List<String> existingColNames = DbUtils.getColumnNames(rs.getMetaData());

							while(rs.next())
							{
								String type = rs.getString(1);

								for (String diskName : result.keySet())
								{
									// The disk name may not be part of this resultset... since it's another table
									if ( ! existingColNames.contains(diskName) )
										continue;
										
									Instant ts = rs.getTimestamp(tsColumn).toInstant();
									double val = rs.getDouble(diskName);

									if ( ! rs.wasNull() )
									{
										DiskInfoEntry diskInfoEntry = result.get(diskName);
										if ("first".equals(type))
										{
											diskInfoEntry.firstPercentUsage   = val;
											diskInfoEntry.firstPercentUsageTs = ts;
										}

										if ("last".equals(type))
										{
											diskInfoEntry.lastPercentUsage   = val;
											diskInfoEntry.lastPercentUsageTs = ts;
										}
									}
								}
							}
						}
					}
				}
			}
			
			return result;
		}

//		private Map<String, DiskInfoEntry> downSample(Map<String, DiskInfoEntry> rawData, Duration bucket)
//		{
//			long bucketMillis = bucket.toMillis();
//			Map<String, List<DiskSample>> downsampled = new HashMap<>();
//
//			for (String disk : rawData.keySet())
//			{
//				List<DiskSample> samples = rawData.get(disk).samples;
//				samples.sort(Comparator.comparing(s -> s.timestamp));
//
//				List<DiskSample> buckets = new ArrayList<>();
//				if (!samples.isEmpty())
//				{
//					Instant bucketStart = samples.get(0).timestamp;
//					double minValue = samples.get(0).freeMb;
//
//					for (DiskSample s : samples)
//					{
//						if (Duration.between(bucketStart, s.timestamp).toMillis() < bucketMillis)
//						{
//							minValue = Math.min(minValue, s.freeMb);
//						}
//						else
//						{
//							buckets.add(new DiskSample(bucketStart, minValue));
//							bucketStart = s.timestamp;
//							minValue = s.freeMb;
//						}
//					}
//					buckets.add(new DiskSample(bucketStart, minValue));
//				}
//				downsampled.put(disk, buckets);
//			}
//			
//			Map<String, DiskInfoEntry> outMap = new HashMap<>();
//			for (Entry<String, DiskInfoEntry> inEntry : rawData.entrySet())
//			{
//				List<DiskSample> samples = downsampled.get(inEntry.getKey());
//				DiskInfoEntry outEntry = new DiskInfoEntry(inEntry.getValue(), samples);
//
//				outMap.put(outEntry.name, outEntry);
//			}
//			
//			return outMap;
//		}
		/**
		 * Downsamples raw per-minute samples into fixed, clock-aligned buckets.
		 *
		 * <p>Buckets are anchored to UTC epoch boundaries so that every disk series
		 * snaps to the same grid (e.g. always 07:00, 08:00 � never 07:02, 08:03),
		 * eliminating the timestamp drift that occurs when buckets are anchored to
		 * the first sample's arrival time.
		 *
		 * <p>Within each bucket {@link DiskSample#freeMbMin} is set to the <b>minimum</b>
		 * of all raw readings in that window. This ensures that short-lived temporary
		 * file events (files created and dropped within a single bucket period) are
		 * not averaged away and remain visible to dip detection downstream.
		 * {@link DiskSample#freeMbAvg} is also computed and stored for potential
		 * future use (e.g. an average-based regression), but is not used in the
		 * current analysis pipeline.
		 *
		 * <p>The final (most-recent) partial bucket is only emitted if it covers at
		 * least half a full bucket's worth of elapsed time, preventing a small sliver
		 * of fresh data from over-influencing the most-recent slope.
		 *
		 * <p>Gaps in the data (missing buckets) are handled naturally � empty buckets
		 * are simply skipped rather than filled with synthetic zero values.
		 *
		 * @param rawData  one {@link DiskInfoEntry} per disk / database name
		 * @param bucket   desired bucket width (e.g. {@code Duration.ofMinutes(60)})
		 * @return         a new map with the same keys, each holding a clock-aligned,
		 *                 min-aggregated (with avg stored) sample list
		 */
		private Map<String, DiskInfoEntry> downSample(Map<String, DiskInfoEntry> rawData, Duration bucket)
		{
			final long bucketMs   = bucket.toMillis();
			final long halfBucket = bucketMs / 2;

			Map<String, DiskInfoEntry> outMap = new HashMap<>();

			for (Entry<String, DiskInfoEntry> inEntry : rawData.entrySet())
			{
				List<DiskSample> samples = inEntry.getValue().samples;
				samples.sort(Comparator.comparing(s -> s.timestamp));

				List<DiskSample> result = new ArrayList<>();

				if (!samples.isEmpty())
				{
					long   currentBucketStart = floorToBucket(samples.get(0).timestamp.toEpochMilli(), bucketMs);
					double min                = Double.MAX_VALUE;
					double sum                = 0.0;
					int    count              = 0;
					Instant lastSampleTs      = samples.get(samples.size() - 1).timestamp;

					for (DiskSample s : samples)
					{
						long sampleBucket = floorToBucket(s.timestamp.toEpochMilli(), bucketMs);

						if (sampleBucket == currentBucketStart)
						{
							// Same bucket � accumulate
							min    = Math.min(min, s.freeMbMin);
							sum   += s.freeMbMin;
							count++;
						}
						else
						{
							// Bucket boundary crossed � emit the completed bucket
							if (count > 0)
							{
								result.add(new DiskSample(
										Instant.ofEpochMilli(currentBucketStart),
										count,
										min,          // freeMb    = min (primary)
										sum / count   // freeMbAvg = mean (for future use)
										));
							}

							// Advance to the bucket this sample belongs in.
							// Note: we jump directly to sampleBucket rather than
							// incrementing by one bucket at a time, so gaps in the
							// data produce no synthetic empty buckets.
							currentBucketStart = sampleBucket;
							min   = s.freeMbMin;
							sum   = s.freeMbMin;
							count = 1;
						}
					}

					// Emit the final (possibly partial) bucket only if it contains
					// at least half a bucket's worth of elapsed time, so a 2-minute
					// sliver of recent data does not distort the most-recent slope.
					if (count > 0)
					{
						long elapsedInBucket = lastSampleTs.toEpochMilli() - currentBucketStart;
						if (elapsedInBucket >= halfBucket)
						{
							result.add(new DiskSample(
									Instant.ofEpochMilli(currentBucketStart),
									count,
									min,
									sum / count
									));
						}
					}
				}

				outMap.put(inEntry.getKey(), new DiskInfoEntry(inEntry.getValue(), result));
			}

			return outMap;
		}

		/**
		 * Snaps an epoch-millisecond timestamp down to the nearest bucket boundary.
		 * For example, with a 60-minute bucket: 07:43:12 UTC -> 07:00:00 UTC.
		 */
		private long floorToBucket(long epochMs, long bucketMs)
		{
			return (epochMs / bucketMs) * bucketMs;
		}

		/**
		 * 
		 * @param mount
		 * @param diskInfo
		 * @return
		 */
		private SpaceForecastResult compute(String mount, DiskInfoEntry diskInfo)
		{
			List<DiskSample> samples = diskInfo.samples;
			
			samples.sort(Comparator.comparing(s -> s.timestamp));

			// Separate out zero/near-zero samples (offline events) before any analysis
			long zeroMbEventCount = samples.stream()
				.filter(s -> s.freeMbMin < zeroValueThresholdMb)
				.count();

			List<DiskSample> cleanSamples = samples.stream()
				.filter(s -> s.freeMbMin > zeroValueThresholdMb)
				.collect(java.util.stream.Collectors.toList());

			// If after filtering we have too few samples, bail out
			if (cleanSamples.size() < minSamples)
				return null; // handled in runForecast() with null check

			// Use cleanSamples for all regression/analysis � not the raw samples
			// but keep original samples for sparkline so zeros are visible
			
			SimpleRegression regression = new SimpleRegression();
			Instant first = cleanSamples.get(0).timestamp;

			for (DiskSample s : cleanSamples)
			{
				double hours = Duration.between(first, s.timestamp).toMillis() / 3600000.0;
				regression.addData(hours, s.freeMbMin);
			}

			double slope       = regression.getSlope();
			double slopeStdErr = regression.getSlopeStdErr();
			double r2          = regression.getRSquare();
			double current     = cleanSamples.get(cleanSamples.size() - 1).freeMbMin;

			if (diskInfo.lastFreeSizeMb > current)
				current = diskInfo.lastFreeSizeMb;

			double ciLower = slope - 1.96 * slopeStdErr;
			double ciUpper = slope + 1.96 * slopeStdErr;

			// NEW: compute ETA regardless of R� � R� only qualifies confidence
			Double hoursToFull = null;
			Instant predicted = null;

			if (slope < -1)
			{
				hoursToFull = current / Math.abs(slope);
				predicted = Instant.now().plus(Duration.ofMillis((long)(hoursToFull * 3600000)));
			}

			// NEW: R� -> ETA confidence (replaces old binary gate)
			EtaConfidence etaConfidence = r2 >= 0.6 ? EtaConfidence.HIGH
										: r2 >= 0.3 ? EtaConfidence.MEDIUM
										:             EtaConfidence.LOW;

			double acceleration = computeRollingAcceleration(cleanSamples);
			boolean anomaly     = detectAnomaly(cleanSamples, regression);
			boolean sawtooth    = detectSawtooth(cleanSamples);

			// NEW: analyze backup/temp-files dips to find min reached and projected overflow risk
			TmpFilesDipStats dipStats         = analyzeTmpFilesDips(mount, cleanSamples, slope);
			boolean          tmpFilesWillFill = dipStats.tmpFilesWillFillDisk;

			// hours until baseline can no longer absorb the max tmp/backup
//			Double hoursUntilTmpFilesOverflow = null;
//			Instant predictedTmpFilesOverflow = null;
//			if (sawtooth && dipStats.maxTmpFilesDropMb > 0)
//			{
//				if (current < dipStats.maxTmpFilesDropMb)
//				{
//					// Already overflowing NOW � baseline can't absorb the max TmpFiles
//					hoursUntilTmpFilesOverflow = 0.0;
//					predictedTmpFilesOverflow  = Instant.now();
//				}
//				else if (slope < 0)
//				{
//					double h = (dipStats.maxTmpFilesDropMb - current) / slope;
//					if (h > 0)
//					{
//						hoursUntilTmpFilesOverflow = h;
//						predictedTmpFilesOverflow  = Instant.now().plus(Duration.ofMillis((long)(h * 3600000)));
//					}
//				}
//			}
			// hours until baseline can no longer absorb tmp/backup
			Double hoursUntilTmpFilesOverflow = null;
			Instant predictedTmpFilesOverflow = null;

			// Only compute TMP ETA if:
			//  - sawtooth pattern detected
			//  - dip magnitude exists
			//  - slope is meaningfully negative
			//  - regression has signal (avoid R� noise instability)
			if (sawtooth
					&& dipStats.maxTmpFilesDropMb > 0
					&& slope < -5                    // avoid near-zero slope instability
					&& r2 >= 0.30)                   // regression must have signal
			{
				if (current < dipStats.maxTmpFilesDropMb)
				{
					// Already insufficient headroom NOW
					hoursUntilTmpFilesOverflow = 0.0;
					predictedTmpFilesOverflow  = Instant.now();
				}
				else
				{
					double h = (dipStats.maxTmpFilesDropMb - current) / slope;

					if (h > 0)
					{
						hoursUntilTmpFilesOverflow = h;
						predictedTmpFilesOverflow  = Instant.now().plus(Duration.ofMillis((long)(h * 3600000)));
					}
				}
			}

			DiskHealthState state = classify(slope, acceleration, anomaly, sawtooth,
					hoursToFull, tmpFilesWillFill, hoursUntilTmpFilesOverflow,
					zeroMbEventCount);

			CriticalReason criticalReason = CriticalReason.NONE;
			if (state == DiskHealthState.CRITICAL)
			{
				// TmpFiles-driven CRITICAL: sawtooth + overflow ETA within critical threshold
				if (sawtooth && tmpFilesWillFill
						&& hoursUntilTmpFilesOverflow != null
						&& Duration.ofMillis((long)(hoursUntilTmpFilesOverflow * 3600000)).compareTo(criticalThreshold) < 0)
				{
					criticalReason = CriticalReason.TMP_FILES;
				}
				else
				{
					criticalReason = CriticalReason.SLOPE;
				}
			}

			return new SpaceForecastResult(
				srvName, cmName + "_" + graphName, mount, "",
				slope, r2, etaConfidence, current,
				acceleration, anomaly, sawtooth,
				state, hoursToFull, predicted,
				ciLower, ciUpper,
				dipStats, hoursUntilTmpFilesOverflow, predictedTmpFilesOverflow,
				zeroMbEventCount,
				criticalReason,
				diskInfo
			);
		}

		/**
		 * 
		 * @param samples
		 * @return
		 */
		private double computeRollingAcceleration(List<DiskSample> samples)
		{
			if (samples.size() < rollingWindowSize * 2) 
				return 0;

			return computeWindowSlope(samples.subList(samples.size() - rollingWindowSize, samples.size()))
				 - computeWindowSlope(samples.subList(0, rollingWindowSize));
		}

		/**
		 * 
		 * @param window
		 * @return
		 */
		private double computeWindowSlope(List<DiskSample> window)
		{
			SimpleRegression reg = new SimpleRegression();
			Instant first = window.get(0).timestamp;
			for (DiskSample s : window)
			{
				double hours = Duration.between(first, s.timestamp).toMillis() / 3600000.0;
				reg.addData(hours, s.freeMbMin);
			}
			return reg.getSlope();
		}

		/**
		 * 
		 * @param samples
		 * @param regression
		 * @return
		 */
		private boolean detectAnomaly(List<DiskSample> samples, SimpleRegression regression)
		{
			Instant first = samples.get(0).timestamp;
			double mean = 0; 
			double m2 = 0; 
			int n = 0;

			for (DiskSample s : samples)
			{
				double hours = Duration.between(first, s.timestamp).toMillis() / 3600000.0;
				double residual = s.freeMbMin - regression.predict(hours);
				n++; 
				double delta = residual - mean; 
				mean += delta / n; 
				m2 += delta * (residual - mean);
			}

			if (n < 2) 
				return false;

			double stddev = Math.sqrt(m2 / (n - 1));
			if (stddev == 0) 
				return false;

			DiskSample last     = samples.get(samples.size()-1);
			double     hours    = Duration.between(first,last.timestamp).toMillis()/3600000.0;
			double     residual = last.freeMbMin - regression.predict(hours);
			double     z        = Math.abs((residual-mean)/stddev);

			return     z > anomalyZScoreThreshold;
		}

		/**
		 * 
		 * @param samples
		 * @return
		 */
		private boolean detectSawtooth(List<DiskSample> samples)
		{
			int cycles = 0;
			for (int i=1;i<samples.size();i++)
			{
				double delta = samples.get(i).freeMbMin - samples.get(i-1).freeMbMin;
				if (delta>cleanupJumpMbThreshold) 
					cycles++;
			}
			return cycles >= 2;
		}

		/**
		 * Analyze backup/TmpFiles dip events � points significantly lower than their neighbors.
		 * Identifies each dip's minimum, the drop size, and whether a future TmpFiles is
		 * projected to push free space below zero given the current baseline trend.
		 * @param mount 
		 *
		 * @param samples  hourly samples (sorted ascending)
		 * @param slope    MB/hour baseline trend (negative = filling)
		 */
		private TmpFilesDipStats analyzeTmpFilesDips(String mount, List<DiskSample> samples, double slope)
		{
			double dipDropThreshold = cleanupJumpMbThreshold * dipNeighborDropFactor;

			List<Double>  dipMinima  = new ArrayList<>();
			List<Instant> dipMinTs   = new ArrayList<>();  // timestamp of each minimum
			List<Double>  dropSizes  = new ArrayList<>();
			List<Instant> dropSizeTs = new ArrayList<>();  // timestamp of each drop peak

			// Need at least 2 neighbors on each side
			for (int i = 2; i < samples.size() - 2; i++)
			{
				double neighborsAvg = (samples.get(i-2).freeMbMin
									+  samples.get(i-1).freeMbMin
									+  samples.get(i+1).freeMbMin
									+  samples.get(i+2).freeMbMin) / 4.0;

				double drop = neighborsAvg - samples.get(i).freeMbMin;
				if (drop > dipDropThreshold)
				{
					// Deduplicate: if very close to previous dip, keep only the deepest
					if (!dipMinima.isEmpty() && i - 2 < samples.size() && dropSizes.get(dropSizes.size()-1) > 0)
					{
						// Simple dedup: skip if previous entry was within 3 hours
						// (tracked via list size vs i comparison would need index tracking;
						//  for simplicity we rely on the threshold being high enough)
					}
					dipMinima .add(samples.get(i).freeMbMin);
					dipMinTs  .add(samples.get(i).timestamp);
					dropSizes .add(drop);
					dropSizeTs.add(samples.get(i).timestamp);
				}
			}

			if (dipMinima.isEmpty())
				return new TmpFilesDipStats(Double.MAX_VALUE, null, 0, 0, null, 0, false);

			int    dipCount = dipMinima.size();
			double avgDrop  = dropSizes.stream().mapToDouble(d -> d).average().getAsDouble();

			// Find index of overall minimum (worst free space during a dip)
			int minIdx = 0;
			for (int i = 1; i < dipMinima.size(); i++)
				if (dipMinima.get(i) < dipMinima.get(minIdx)) minIdx = i;
			double  overallMin  = dipMinima.get(minIdx);
			Instant overallMinTs = dipMinTs.get(minIdx);

			// Find index of largest drop
//			int maxIdx = 0;
//			for (int i = 1; i < dropSizes.size(); i++)
//			{
//				if (dropSizes.get(i) > dropSizes.get(maxIdx))
//				{
//					maxIdx = i;
//				}
//			}
//			double  maxDrop   = dropSizes .get(maxIdx);
//			Instant maxDropTs = dropSizeTs.get(maxIdx);

//			// Use 95th percentile instead of absolute max to remove outliers
//			List<Double> sortedDrops = new ArrayList<>(dropSizes);
//			Collections.sort(sortedDrops);
//
//			int p95Index = (int)Math.floor(0.95 * (sortedDrops.size() - 1));
//			double effectiveDrop = sortedDrops.get(p95Index);
//
//			// We no longer use absolute max for risk modeling
//			double maxDrop = effectiveDrop;
//			Instant maxDropTs = dropSizeTs.get(p95Index);


			// ---- Hybrid TMP drop modeling (P95 + 7d adaptive max) ----
			// 1) P95 over full window (30d)
			List<Double> sortedDrops = new ArrayList<>(dropSizes);
			Collections.sort(sortedDrops);

			int p95Index = (int)Math.floor(0.95 * (sortedDrops.size() - 1));
			double p95Drop = sortedDrops.get(p95Index);

			// 2) Max drop in last 7 days (adaptive regime detection)
			Instant latestTs = samples.get(samples.size() - 1).timestamp;
			Instant sevenDaysAgo = latestTs.minus(Duration.ofDays(7));

			double maxLast7Days = 0.0;
			Instant maxLast7DaysTs = null;

			for (int i = 0; i < dropSizes.size(); i++)
			{
				if (dropSizeTs.get(i).isAfter(sevenDaysAgo) && dropSizes.get(i) > maxLast7Days)
				{
					maxLast7Days = dropSizes.get(i);
					maxLast7DaysTs = dropSizeTs.get(i);
				}
			}

			// 3) Hybrid effective drop
			double maxDrop;
			Instant maxDropTs;

			if (maxLast7Days > p95Drop)
			{
				maxDrop = maxLast7Days;
				maxDropTs = maxLast7DaysTs;
			}
			else
			{
				maxDrop = p95Drop;
				maxDropTs = dropSizeTs.get(p95Index);
			}

			// Project: will the baseline (at current trend) drop low enough that
			// a max-sized TmpFiles will push free space to zero?
			// Check over the next 90 days in weekly steps.
//			double currentBaseline = samples.get(samples.size()-1).availableMb;
//			boolean willFill = false;
//			for (int weeksAhead = 1; weeksAhead <= 13; weeksAhead++)
//			{
//				double projectedBaseline = currentBaseline + slope * weeksAhead * 7 * 24;
//				if (projectedBaseline - maxDrop < 0)
//				{
//					willFill = true;
//					break;
//				}
//			}
			double currentBaseline = samples.get(samples.size()-1).freeMbMin;

			// Check immediately - already can't absorb max TmpFiles right now
//			boolean willFill = (currentBaseline - maxDrop < 0);
			
			// Only consider immediate risk if baseline trend is negative and significant
			boolean willFill = false;

			if (slope < -5 && (currentBaseline - maxDrop < 0))
			{
				willFill = true;
			}			
//System.out.println(">>>> analyzeTmpFilesDips >>> NAME=" + mount + ", currentBaseline=" + currentBaseline + ",maxDrop=" + maxDrop + ", xxx=" + (currentBaseline - maxDrop) + ", willFill@1: " + willFill);

//			// If not already critical, check forward 90 days
//			if (!willFill && slope < 0)
//			{
//				for (int weeksAhead = 1; weeksAhead <= 13; weeksAhead++)
//				{
//					double projectedBaseline = currentBaseline + slope * weeksAhead * 7 * 24;
//					if (projectedBaseline - maxDrop < 0)
//					{
//						willFill = true;
//						break;
//					}
//				}
//			}
			// If not already critical, check forward # days
			if (!willFill && slope < 0)
			{
				int checkDaysAhead = getCriticalThresholdHours() / 24;
				
				for (int futureDay = 1; futureDay <= (checkDaysAhead + 1); futureDay++)
				{
					double projectedBaseline = currentBaseline + slope * futureDay * 24;
//System.out.println(">>>> analyzeTmpFilesDips >>> NAME=" + mount + ", futureDay=" + futureDay + ", projectedBaseline=" + projectedBaseline + ", maxDrop=" + maxDrop + ", willFail=" + (projectedBaseline - maxDrop < 0));

					if (projectedBaseline - maxDrop < 0)
					{
						willFill = true;
						break;
					}
				}
			}

			return new TmpFilesDipStats(overallMin, overallMinTs, avgDrop, maxDrop, maxDropTs, dipCount, willFill);
		}

		/**
		 * classify() now checks TmpFiles overflow risk BEFORE generic anomaly/sawtooth,
		 * and uses hoursUntilTmpFilesOverflow for CRITICAL/WARNING thresholds when relevant.
		 */
		private DiskHealthState classify(double slope, double acceleration, boolean anomaly,
				boolean sawtooth, Double hoursToFull,
				boolean tmpFilesWillFill, Double hoursUntilTmpFilesOverflow,
				long zeroMbEventCount)
		{
			if (zeroMbEventCount > 0)
			{
				// Only suppress states that were CAUSED by zero readings.
				// First check if there's a real slope-driven CRITICAL/WARNING � if so, let it through.
				boolean realFillThreat = hoursToFull != null
						&& Duration.ofMillis((long)(hoursToFull * 3600000))
								   .compareTo(warningThreshold) < 0;

				if (!realFillThreat)
				{
					// No real fill threat � suppress fake sawtooth and fake dip-CRITICAL
					if (sawtooth)
						return DiskHealthState.ANOMALY;

					if (tmpFilesWillFill && hoursUntilTmpFilesOverflow != null && hoursUntilTmpFilesOverflow == 0.0)
						return DiskHealthState.ANOMALY;
				}
				// If realFillThreat is true, fall through to normal classification below
			}

//			// offline/zero readings detected � override sawtooth and dip-driven CRITICAL
//			// since those are caused by fake zeros, not real disk events
//			if (zeroMbEventCount > 0)
//			{
//				if (sawtooth)	
//					return DiskHealthState.ANOMALY; // sawtooth was fake cleanup jumps from zero
//				
//				if (tmpFilesWillFill && hoursUntilTmpFilesOverflow != null && hoursUntilTmpFilesOverflow == 0.0)
//					return DiskHealthState.ANOMALY; // CRITICAL caused by inflated maxBackupDrop from zero
//			}

			// Check TmpFiles overflow risk first � more specific than generic sawtooth
			if (sawtooth && tmpFilesWillFill && hoursUntilTmpFilesOverflow != null)
			{
				Duration eta = Duration.ofMillis((long)(hoursUntilTmpFilesOverflow * 3600000));
				if (eta.compareTo(criticalThreshold) < 0) return DiskHealthState.CRITICAL;
				if (eta.compareTo(warningThreshold)  < 0) return DiskHealthState.WARNING;
				return DiskHealthState.TMP_FILE_RISK; // sawtooth + overflow projected but not imminent
			}

			// Anomaly check (moved after TmpFiles risk so sawtooth+anomaly doesn't hide the real issue)
			if (anomaly) return DiskHealthState.ANOMALY;

			// Generic sawtooth without overflow risk
			if (sawtooth) return DiskHealthState.CYCLIC_CLEANUP;

			// Linear fill checks
			if (hoursToFull != null)
			{
				Duration eta = Duration.ofMillis((long)(hoursToFull * 3600000));
				if (eta.compareTo(criticalThreshold) < 0) return DiskHealthState.CRITICAL;
				if (eta.compareTo(warningThreshold)  < 0) return DiskHealthState.WARNING;
			}

			if (acceleration < -5) return DiskHealthState.ACCELERATING;
			if (slope < -1)        return DiskHealthState.FILLING;

			return DiskHealthState.STABLE;
		}
	}

	private static class ReportHelper
	{
		public String generateHtmlReportBegin(String label, int days, int samplePeriod)
		{
			StringBuilder html = new StringBuilder();

			html.append("<!DOCTYPE html> \n \n");
			html.append("<html> \n");

			// HEAD Start
			html.append("<head> \n");
			html.append("<meta charset='UTF-8'> \n");
			html.append("<meta name='viewport' content='width=device-width, initial-scale=1'> \n");

			HtmlUtils.createCssLinkTag(html,  "/scripts/bootstrap/4.6.2/css/bootstrap.min.css"          , "https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css");

			HtmlUtils.createJsScriptTag(html, "/scripts/jquery/jquery-3.7.1.min.js"                     , "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js");
			HtmlUtils.createJsScriptTag(html, "/scripts/jquery-sparklines/2.1.2/jquery.sparkline.min.js", "https://cdnjs.cloudflare.com/ajax/libs/jquery-sparklines/2.1.2/jquery.sparkline.min.js");
			HtmlUtils.createJsScriptTag(html, "/scripts/sorttable/sorttable.js"                         , "http://www.dbxtune.com/sorttable.js");

			html.append("</head> \n");
			// HEAD End

			// BODY START
			html.append("<body class='bg-light'> \n");
			html.append("<div class='container-fluid mt-4'> \n");

			html.append("<h2 class='mb-3'>Disk Forecast Report - ").append(label).append("</h2> \n");

			html.append("<p class='text-muted'>");
			html.append("Generated: "    ).append(TimeUtils.toStringYmdHms(System.currentTimeMillis())).append("<br> \n");
//			html.append("Server Name: "  ).append( srvName      ).append("<br> \n");
			html.append("Days Sampled: " ).append( days         ).append("<br> \n");
			html.append("Downsampled to ").append( samplePeriod ).append(" Minutes <br> \n");
			html.append("</p> \n");

			return html.toString();
		}

		public String generateHtmlReportEnd()
		{
			StringBuilder html = new StringBuilder();

			html.append("</body> \n");
			html.append("</html> \n");
			
			return html.toString();
		}
	}
	private class Report
	{
		/**
		 * Generates a Bootstrap styled HTML report with:
		 *  - Sorting
		 *  - Tooltips
		 *  - Severity badges
		 *  - Inline sparklines (jQuery-sparkline)
		 *  - Downsampled hourly/minimum values to reduce size
		 *  - NEW: TmpFiles risk columns (min during TmpFiles, avg/max TmpFiles size, overflow ETA)
		 */
		public void generateHtmlReport(List<SpaceForecastResult> results, String file) 
		throws IOException
		{
			String html = generateHtmlReport(results, true, null);
			try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8))
			{
				fw.write(html);
			}
		}
		
		/**
		 * Generates a Bootstrap styled HTML report with:
		 *  - Sorting
		 *  - Tooltips
		 *  - Severity badges
		 *  - Inline sparklines (jQuery-sparkline)
		 *  - Downsampled hourly/minimum values to reduce size
		 *  - NEW: TmpFiles risk columns (min during TmpFiles, avg/max TmpFiles size, overflow ETA)
		 */
		public String generateHtmlReport(List<SpaceForecastResult> results, boolean createFullHtml, String beforeText) 
		{
			StringBuilder html = new StringBuilder();

			// If we create SparkLines -- We need to execute some JavaScript code... Lets put that in here
			Map<String, String> jsScriptOutMap = new LinkedHashMap<>();
			
//			DecimalFormatSymbols dfs = new DecimalFormatSymbols();
//			dfs.setGroupingSeparator(' ');
//			DecimalFormat nf = new DecimalFormat("#,###", dfs);

			if (createFullHtml)
			{
				String label     = results.isEmpty() ? "" : results.get(0).srvName;
				String extraName = results.isEmpty() ? "" : results.get(0).extraName;
				if (StringUtil.hasValue(extraName))
					label = label + " - " + extraName;

				html.append( generateHtmlReportBegin(label, getDays(), getDownsamplePeriod()) );
//				html.append("<!DOCTYPE html> \n \n");
//				html.append("<html> \n");
//
//				// HEAD Start
//				html.append("<head> \n");
//				html.append("<meta charset='UTF-8'> \n");
//				html.append("<meta name='viewport' content='width=device-width, initial-scale=1'> \n");
//
//				HtmlUtils.createCssLinkTag(html,  "/scripts/bootstrap/4.6.2/css/bootstrap.min.css"          , "https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css");
//
//				HtmlUtils.createJsScriptTag(html, "/scripts/jquery/jquery-3.7.1.min.js"                     , "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js");
//				HtmlUtils.createJsScriptTag(html, "/scripts/jquery-sparklines/2.1.2/jquery.sparkline.min.js", "https://cdnjs.cloudflare.com/ajax/libs/jquery-sparklines/2.1.2/jquery.sparkline.min.js");
//				HtmlUtils.createJsScriptTag(html, "/scripts/sorttable/sorttable.js"                         , "http://www.dbxtune.com/sorttable.js");
//
//				html.append("</head> \n");
//				// HEAD End
//
//				// BODY START
//				html.append("<body class='bg-light'> \n");
//				html.append("<div class='container-fluid mt-4'> \n");
//
//				html.append("<h2 class='mb-3'>Disk Forecast Report - ").append(label).append("</h2> \n");
//
//				html.append("<p class='text-muted'>");
//				html.append("Generated: "    ).append(TimeUtils.toStringYmdHms(System.currentTimeMillis())).append("<br> \n");
////				html.append("Server Name: "  ).append( getSrvName()          ).append("<br> \n");
//				html.append("Days Sampled: " ).append( getDays()             ).append("<br> \n");
//				html.append("Downsampled to ").append( getDownsamplePeriod() ).append(" Minutes <br> \n");
//				html.append("</p> \n");
			}

			// Write something before the report
			if (StringUtil.hasValue(beforeText))
			{
				html.append("<p class='text-muted'>");
				html.append(beforeText).append(" \n");
				html.append("</p> \n");
			}

			//---------------------------------------
			// Summary -- Summary:   CRITICAL: 1 WARNING: 0 OK: 3 No Data: 0  Critical Threshold: 168 hours (7 days) Warning Threshold: 336 hours (14 days)
			//---------------------------------------
			long criticalCnt = results.stream().filter(r -> DiskHealthState.CRITICAL.equals(r.state)).count();
			long warningCnt  = results.stream().filter(r -> DiskHealthState.WARNING .equals(r.state)).count();
			long otherCnt    = results.size() - criticalCnt - warningCnt;

			String criticalTheshold = getCriticalThresholdHours() + " hours <i>(" + getCriticalThresholdHours()/24 + " days)</i>";
			String warningTheshold  = getWarningThresholdHours()  + " hours <i>(" + getWarningThresholdHours() /24 + " days)</i>";

//			html.append("  <div> \n");
//			html.append("    <b>Summary: &nbsp; </b> \n");
//			html.append("    <span class='badge bg-" + SpaceForecastResult.getStateBadgeColor(DiskHealthState.CRITICAL) + "'>" + DiskHealthState.CRITICAL + " </span> ").append(criticalCnt).append("&emsp; \n");
//			html.append("    <span class='badge bg-" + SpaceForecastResult.getStateBadgeColor(DiskHealthState.WARNING)  + "'>" + DiskHealthState.WARNING  + " </span> ").append(warningCnt) .append("&emsp; \n");
//			html.append("    <span class='badge bg-" + SpaceForecastResult.getStateBadgeColor(DiskHealthState.STABLE)   + "'>" + "OTHER"                  + " </span> ").append(otherCnt)   .append("&emsp; \n");
//			html.append("    <span>&emsp;&emsp;</span> \n"); // Just some extra space
//			html.append("    <span class='badge bg-" + SpaceForecastResult.getStateBadgeColor(DiskHealthState.CRITICAL) + "'>" + DiskHealthState.CRITICAL + " Threshold </span> ").append(criticalTheshold).append("&emsp; \n");
//			html.append("    <span class='badge bg-" + SpaceForecastResult.getStateBadgeColor(DiskHealthState.WARNING)  + "'>" + DiskHealthState.WARNING  + " Threshold </span> ").append(warningTheshold) .append("&emsp; \n");
//			html.append("  </div> \n");

			html.append("  <div> \n");
			html.append("    <b>Summary: &nbsp; </b> \n");
			html.append("    " + SpaceForecastResult.getStateBadge(DiskHealthState.CRITICAL)                       + " ").append(criticalCnt)     .append("&emsp; \n");
			html.append("    " + SpaceForecastResult.getStateBadge(DiskHealthState.WARNING)                        + " ").append(warningCnt)      .append("&emsp; \n");
			html.append("    " + SpaceForecastResult.getStateBadge(DiskHealthState.STABLE,   "OTHER")              + " ").append(otherCnt)        .append("&emsp; \n");
			html.append("    <span>&emsp;&emsp;</span> \n"); // Just some extra space
			html.append("    " + SpaceForecastResult.getStateBadge(DiskHealthState.CRITICAL, "CRITICAL Threshold") + " ").append(criticalTheshold).append("&emsp; \n");
			html.append("    " + SpaceForecastResult.getStateBadge(DiskHealthState.WARNING,  "WARNING Threshold")  + " ").append(warningTheshold) .append("&emsp; \n");
			html.append("  </div> \n");
			
			
			// Bootstrap 5 (doesn't work to 100%)
//			String thSpacer = "<th class='border-0 opacity-0 bg-transparent' style='width:10px;'>&nbsp;</th>";
//			String tdSpacer = "<td class='border-0 opacity-0 bg-transparent''>&nbsp;</td>";
			// Bootstrap 4 (works better)
			String thSpacer = "<th class='border-0 bg-white' style='width:10px;'>&nbsp;</th> \n";
			String tdSpacer = "<td class='border-0 bg-white'>&nbsp;</td> \n";

			html.append("<table id='diskTable' class='sortable table table-sm table-striped table-hover table-bordered'> \n");

			// Header with tooltips
			html.append("<thead class='table-dark'> \n");

			boolean printSummaryHeaders = true; // But then we wont be able to SORT the columns
			if (printSummaryHeaders)
			{
//				int    days        = results.isEmpty() ? DEFAULT_sampleDays : results.get(0).days;
//				String sourceTable = results.isEmpty() ? "" : results.get(0).sourceTable;
//				String srvName     = results.isEmpty() ? "" : results.get(0).srvName;
				int    days             = getDays();
				int    downsamplePeriod = getDownsamplePeriod();
				String cmName           = getCmName();
				String graphName        = getGraphName();
				String srvName          = getSrvName();

				String graphList   = getDbxCentalGraphListCsv(cmName, graphName);
				String baseGraph   = "<a href='/graph.html?subscribe=false&cs=dark&startTime=" + days + "d&sessionName=" + srvName + "<START>&graphList=" + graphList + "&gcols=1&sampleType=AUTO' target='_blank' title='Open a set of Graphs in DbxCentral'>DbxGraphs</a>";
				String tpmUsagedDbxtuneGraphLinks = baseGraph.replace("<START>", getDbxGraphMarkTimeForTmp    (results)); //"&markTime=" + getDbxGraphMarkTimeForTmp    (results));
				String totSizeDbxtuneGraphLinks   = baseGraph.replace("<START>", getDbxGraphMarkTimeForTotSize(results)); //"&markTime=" + getDbxGraphMarkTimeForTotSize(results));
				String trendDbxtuneGraphLinks     = baseGraph.replace("<START>", "");

				html.append("<tr> \n");
				html.append("  <th nowrap colspan='9' title='Using SimpleRegression Model to guess when space will be used up'>Mathematical Algorithm to Guess when we run out of space</th> \n");
				html.append(   thSpacer);
				html.append("  <th nowrap colspan='3' title='Tip: Hover over |Free First| or |Free Last| to see a timestamp'>Free Space in Period (last " + days + "d)</th> \n");
				html.append(   thSpacer);
				html.append("  <th nowrap colspan='5' title='Tip: Hover over |Min Tmp| or |Max Tmp| to see a timestamp'>Temporary Usage " + tpmUsagedDbxtuneGraphLinks + "</th> \n");
				html.append(   thSpacer);
				html.append("  <th nowrap colspan='7' title='Tip: Hover over |SizeDiff| to se if when the disk/db was expanded or shrinked'>Total Allocated Disk/DB Size " + totSizeDbxtuneGraphLinks + "</th> \n");
				html.append(   thSpacer);
				html.append("  <th nowrap colspan='3' title='Tip: Hover over the chart to see when the change happened'>Charting the Data Points " + trendDbxtuneGraphLinks + "</th> \n");
				html.append("</tr> \n");
			}

			html.append("<tr> \n");
			addHeader(html, "Name"          , "Mount point/drive letter or DB Name");
			addHeader(html, "State"         , "Health classification\n"
			                                  + " - STABLE         &mdash; Slope is near-flat with no imminent risk \n"
			                                  + " - FILLING        &mdash; Negative slope but ETA is beyond all alert thresholds \n"
			                                  + " - ACCELERATING   &mdash; Fill rate is measurably increasing over time \n"
			                                  + " - WARNING        &mdash; Linear ETA or TMP overflow ETA falls within the warning window (default &le; 14 days) \n"
			                                  + " - CRITICAL       &mdash; Linear ETA or TMP overflow ETA falls within the critical window (default &le; 7 days) \n"
			                                  + " - CYCLIC_CLEANUP &mdash; Series exhibits a sawtooth pattern consistent with periodic cleanup jobs; slope-based fill rate is considered unreliable in this context \n"
			                                  + " - TMP_FILE_RISK  &mdash; Temporary usage dips exist but the baseline trend alone does not trigger WARNING/CRITICAL; risk is driven by workloads that may overflow remaining headroom \n"
			                                  + " - ANOMALY        &mdash; A single anomalous data point (z-score above the configured threshold, default 3.0&sigma;) was detected that skewed the regression \n"
			                                  + "");
			addHeader(html, "Slope Hour"    , "MB per Hour consumption rate\nPositive Number = Decreasing/Gaining Space\nNegative Number = Increasing/Allocating Space");
			addHeader(html, "Slope Day"     , "MB per Day consumption rate\nPositive Number = Decreasing/Gaining Space\nNegative Number = Increasing/Allocating Space");
			addHeader(html, "Accel"         , "Change in slope (rolling window)");
			addHeader(html, "R&sup2;"       , "Regression goodness-of-fit (does NOT predict ETA - only indicates confidence).\n Higher is better (1.0 = Perfect)");
			addHeader(html, "ETA Confi"     , "ETA Confidence \nHIGH: R&sup2; >= 0.6 reliable trend \nMEDIUM: R&sup2;>=0.3 \nLOW: R&sup2;<0.3 noisy data");
			addHeader(html, "ETA (h)"       , "Estimated hours until full (baseline trend)");
			addHeader(html, "ETA (d)"       , "Estimated days until full (baseline trend)");

			html.append(thSpacer);
			addHeader(html, "Free First"    , "Free MB at FIRST sample");
			addHeader(html, "Free Last"     , "Free MB at LAST sample");
			addHeader(html, "Free Diff"     , "Free MB Difference between First and Last sample");

			html.append(thSpacer);
			addHeader(html, "Tmp Dips"      , "Number of detected TmpFiles dip events");
			addHeader(html, "Min Tmp"       , "Lowest free MB seen during any Backup or TmpFiles dip");
//			addHeader(html, "Min Tmp"       , "Minimum MB consumed during Backup or TmpFiles");
			addHeader(html, "Avg Tmp"       , "Average MB consumed during Backup or TmpFiles (GB)");
			addHeader(html, "Max Tmp"       , "Largest MB consumed during a single Backup or TmpFiles run (GB)");
			addHeader(html, "Tmp ETA"       , "Days until baseline shrinks enough that max Backups or TmpFiles overflows disk");

			html.append(thSpacer);
			addHeader(html, "Size First"    , "Size of this Disk in MB at FIRST sample.");
			addHeader(html, "Size Last"     , "Size of this Disk in MB at LAST sample.");
			addHeader(html, "Ch"            , "Numer of Change Events.\nMaking The Disk/DB area Larger or Smaller");
			addHeader(html, "Size Diff"     , "How many MB is the difference from FIRST to LAST entry. \nOr how many MB have the disk grown/shrinked in period.\nPositive Number = GROW\nNegative Number = SHRINK");
			addHeader(html, "Diff/Day"      , "Average diff per day (or how much the Disk/DB area actually changed.\nNote: The value are derived from DiskChangeEvents (sumPerDay/numOfDaysWithChange)");
			addHeader(html, "First Used %"  , "Percent Used for this disk at FIRST sample");
			addHeader(html, "Last Used %"   , "Percent Used for this disk at LAST sample");
			
			html.append(thSpacer);
			addHeader(html, "Trend Free MB (last " + getDays() + "d)" , "Sparkline of FREE Space (hover for details)\nNOTE: Red dots in the graph indicates that we had Disk/DB Size Expansion. (for exact details/size hover over |Size Diff| in the previous section of the table.|)");
			addHeader(html, "Data Points"   , "Number of Datapoints in the Sparkline, and also number of Datapoints used for analyze.\nFormat: dataPoints / days");
			addHeader(html, "Name"          , "Mount point/drive letter or DB Name");
			html.append("</tr> \n");
			html.append("</thead> \n");

			// TABLE BODY
			html.append("<tbody> \n");

			// Loop all the records... one row for each record
			for (SpaceForecastResult r : results)
			{
				if (r == null)
					continue;

				html.append("<tr> \n");
				//------------------------------------------------------------------------------
				// "Disk", "State", "Slope Hour", "Slope Day", "Acel", "R2", "ETA Confidence", "ETA (h)", "ETA (d)"
				html.append("  <td " + r.getTdaName()          + ">" + r.getFmtName()          + "</td> \n");
				html.append("  <td " + r.getTdaState()         + ">" + r.getFmtState()         + "</td> \n");
				html.append("  <td " + r.getTdaSlopeHour()     + ">" + r.getFmtSlopeHour()     + "</td> \n");
				html.append("  <td " + r.getTdaSlopeDay()      + ">" + r.getFmtSlopeDay()      + "</td> \n");
				html.append("  <td " + r.getTdaAcceleration()  + ">" + r.getFmtAcceleration()  + "</td> \n");
				html.append("  <td " + r.getTdaRSquared()      + ">" + r.getFmtRSquared()      + "</td> \n");
				html.append("  <td " + r.getTdaEtaConfidence() + ">" + r.getFmtEtaConfidence() + "</td> \n");
				html.append("  <td " + r.getTdaEtaHours()      + ">" + r.getFmtEtaHours()      + "</td> \n");
				html.append("  <td " + r.getTdaEtaDays()       + ">" + r.getFmtEtaDays()       + "</td> \n");
				

				html.append(tdSpacer);
				//------------------------------------------------------------------------------
				// "Free First", "Free Last", "Free Diff"
				html.append("  <td " + r.getTdaFreeFirst()     + ">"    + r.getFmtFreeFirst()     + "</td> \n");
				html.append("  <td " + r.getTdaFreeLast()      + "><b>" + r.getFmtFreeLast()      + "</b></td> \n");
				html.append("  <td " + r.getTdaFreeDiff()      + ">"    + r.getFmtFreeDiff()      + "</td> \n");

				html.append(tdSpacer);
				//------------------------------------------------------------------------------
				// "Tmp Dips", "Min Tmp", "Avg Tmp", "Max Tmp", "Tmp ETA"
				int tmpMinDangerThreshold = getTmpDipsMinThreshold();
				int tmpCriticalThreshold  = getCriticalThresholdHours();
				int tmpWarningThreshold   = getWarningThresholdHours();
				
				html.append("  <td " + r.getTdaTmpDipsCount()                                    + ">" + r.getFmtTmpDipsCount() + "</td> \n");
				html.append("  <td " + r.getTdaTmpMin(tmpMinDangerThreshold)                     + ">" + r.getFmtTmpMin()       + "</td> \n");
				html.append("  <td " + r.getTdaTmpAvg()                                          + ">" + r.getFmtTmpAvg()       + "</td> \n");
				html.append("  <td " + r.getTdaTmpMax()                                          + ">" + r.getFmtTmpMax()       + "</td> \n");
				html.append("  <td " + r.getTdaTmpEta(tmpCriticalThreshold, tmpWarningThreshold) + ">" + r.getFmtTmpEta()       + "</td> \n");
				
				
				html.append(tdSpacer);
				//------------------------------------------------------------------------------
				// "Size First", "Size Last", "Ch", "Size Diff", "Diff/Day", "First Used %", "Last Used %"
				html.append("  <td " + r.getTdaSizeFirst()        + ">" + r.getFmtSizeFirst()        + "</td> \n");
				html.append("  <td " + r.getTdaSizeLast()         + ">" + r.getFmtSizeLast()         + "</td> \n");
				html.append("  <td " + r.getTdaSizeChangeCount()  + ">" + r.getFmtSizeChangeCount()  + "</td> \n");
				html.append("  <td " + r.getTdaSizeDiffTotal()    + ">" + r.getFmtSizeDiffTotal()    + "</td> \n");
				html.append("  <td " + r.getTdaSizeDiffPerDay()   + ">" + r.getFmtSizeDiffPerDay()   + "</td> \n");
				html.append("  <td " + r.getTdaSizeFirstUsedPct() + ">" + r.getFmtSizeFirstUsedPct() + "</td> \n");
				html.append("  <td " + r.getTdaSizeLastUsedPct()  + ">" + r.getFmtSizeLastUsedPct()  + "</td> \n");
				
				html.append(tdSpacer);
				//------------------------------------------------------------------------------
				// "Sparkline", "Data Points", "Name"
				String sparkline = getSparcLineFreeMbAndChangeEvent(r, jsScriptOutMap);
    			html.append("  <td nowrap>").append( sparkline ).append("</td> \n");    // Trend Free MB Chart
				
				html.append("  <td " + r.getTdaDataPointsAndDays() + ">" + r.getFmtDataPointsAndDays() + "</td> \n");
				html.append("  <td " + r.getTdaName()              + ">" + r.getFmtName()              + "</td> \n");

				
				html.append("</tr> \n");
			}

			html.append("</tbody> \n");
			html.append("</table> \n");
			
			if (createFullHtml)
			{
				html.append("</div> \n");
			}

			
//			if (true)
//			{
//				boolean debug = true;
//				html.append("\n");
//				html.append("  <!-- ########################### --> \n");
//				html.append("  <!-- Execute when page is LOADED --> \n");
//				html.append("  <!-- ########################### --> \n");
//				html.append("  <script> \n");
//				html.append("  window.addEventListener('load', () => \n");
//				html.append("  { \n");
//				html.append("      console.log('Page is loaded, now initializing, SparkLines...'); \n");
//				
//				// DEBUG: To defer it a bit using: setTimeout(() => { jsCodeThatInitializesAllSparkLines; }, 2000); 
//				if (debug)
//					html.append("      setTimeout(() => { \n");
//
//				for (String jsCode : jsScriptOutMap.values())
//				{
//					html.append(jsCode).append(" \n");
//				}
//				html.append("      // now show the Min|Max: ### indicator \n");
//				html.append("      $('.space-forecast-sparkline-ind-val').css('display', 'block'); \n");
////				html.append("      $('.space-forecast-sparkline-ind-val').css({'display':'block', 'position':'absolute', 'z-index':'2', 'left':'2px', 'top':'-2px', 'font-size':'9px', 'font-family':'Tahoma,Arial', 'color':'black'}); \n");
//				
//				// DEBUG: To defer it a bit
//				if (debug)
//					html.append("      }, 2000); \n");
//
//				html.append("  }); \n");
//				html.append("  </script> \n");
//			}
			for (String jsCode : jsScriptOutMap.values())
			{
				html.append("<script> \n");
				html.append(jsCode).append(" \n");
				html.append("</script> \n");
			}
			

			if (createFullHtml)
			{
				generateHtmlReportEnd();
			}

			return html.toString();
		}

		private String getDbxGraphMarkTimeForTmp(List<SpaceForecastResult> results)
		{
			double minVal = Double.MAX_VALUE;
			Instant time = null;;
			for (SpaceForecastResult r : results)
			{
				if (r.dipStats.minDuringTmpFilesMb > 0)
				{
					if (r.dipStats.minDuringTmpFilesMb < minVal)
					{
						minVal = r.dipStats.minDuringTmpFilesMb;
						time   = r.dipStats.minDuringTmpFilesMbTs;
					}
				}
			}
			if (time != null)
			{
				return "&markTime=" + TimeUtils.toStringYmdHms(time);
			}
			return "";
		}

		private String getDbxGraphMarkTimeForTotSize(List<SpaceForecastResult> results)
		{
//			double minVal = Double.MIN_VALUE;
			double minVal = Double.NEGATIVE_INFINITY;
			Instant time = null;;
			for (SpaceForecastResult r : results)
			{
				Double firstLastSizeMbDiff = null;
				if (r.diskInfoEntry.firstTotalSizeMb != null && r.diskInfoEntry.lastTotalSizeMb != null)
					firstLastSizeMbDiff = r.diskInfoEntry.lastTotalSizeMb - r.diskInfoEntry.firstTotalSizeMb;

				if (firstLastSizeMbDiff != null)
				{
					if (firstLastSizeMbDiff > minVal)
					{
						minVal = firstLastSizeMbDiff;
						if (r.diskInfoEntry.sizeChangedEvents != null && !r.diskInfoEntry.sizeChangedEvents.isEmpty())
						{
							// Get LAST timestamp of the sizeChangedEvents
							time = r.diskInfoEntry.sizeChangedEvents.get( r.diskInfoEntry.sizeChangedEvents.size()-1 ).ts;
						}
					}
				}
			}
			if (time != null)
			{
				return "&markTime=" + TimeUtils.toStringYmdHms(time);
			}
			return "";
		}

		private String getDbxCentalGraphListCsv(String cmName, String graphName)
		{
			// OS
			//     CmOsDiskSpace_FsSizeMb
			//     CmOsDiskSpace_FsAvailableMb
			//     CmOsDiskSpace_FsUsedMb
			//     CmOsDiskSpace_FsUsedPct
			// SQL Server
			//     CmDatabases_DbSizeMb
			//     CmDatabases_Db{Data|Log}SizeMbGraph
			//     CmDatabases_Db{Data|Log}SizeLeftMbGraph
			//     CmDatabases_Db{Data|Log}SizeUsedMbGraph
			//     CmDatabases_Db{Data|Log}SizeUsedPctGraph
			// Sybase ASE
			//     CmOpenDatabases_DbSizeMb
			//     CmOpenDatabases_Db{Data|Log}SizeMbGraph
			//     CmOpenDatabases_Db{Data|Log}SizeLeftMbGraph
			//     CmOpenDatabases_Db{Data|Log}SizeUsedMbGraph
			//     CmOpenDatabases_Db{Data|Log}SizeUsedPctGraph
			
//			String cmName      = StringUtils.substringBefore(sourceTable, "_");
//			String graphName   = StringUtils.substringAfter (sourceTable, "_");
			String type = "";
			if (graphName.equals("FsAvailableMb"))         type = "OS";
			if (graphName.equals("DbDataSizeLeftMbGraph")) type = "DBMS_DATA";
			if (graphName.equals("DbLogSizeLeftMbGraph"))  type = "DBMS_WAL";
			
			String graphList = "";
			if ("OS".equals(type))
			{
				graphList = ""
						+  "CmOsDiskSpace_FsSizeMb"
						+ ",CmOsDiskSpace_FsAvailableMb"
						+ ",CmOsDiskSpace_FsUsedMb"
						+ ",CmOsDiskSpace_FsUsedPct"
						;
			} 
			else if ("DBMS_DATA".equals(type)) 
			{
				if ("CmDatabases".equals(cmName))
				{
					// SQL Server
					graphList = ""
							+  "CmDatabases_DbSizeMb"
							+ ",CmDatabases_DbDataSizeMbGraph"
							+ ",CmDatabases_DbDataSizeLeftMbGraph"
							+ ",CmDatabases_DbDataSizeUsedMbGraph"
							+ ",CmDatabases_DbDataSizeUsedPctGraph"
							;
				}
				else
				{
					// Sybase ASE
					graphList = ""
							+  "CmOpenDatabases_DbSizeMb"
							+ ",CmOpenDatabases_DbDataSizeMbGraph"
							+ ",CmOpenDatabases_DbDataSizeLeftMbGraph"
							+ ",CmOpenDatabases_DbDataSizeUsedMbGraph"
							+ ",CmOpenDatabases_DbDataSizeUsedPctGraph"
							;
				}
			} 
			else if ("DBMS_WAL".equals(type)) 
			{
				if ("CmDatabases".equals(cmName))
				{
					// SQL Server
					graphList = ""
							+  "CmDatabases_DbSizeMb"
							+ ",CmDatabases_DbLogSizeMbGraph"
							+ ",CmDatabases_DbLogSizeLeftMbGraph"
							+ ",CmDatabases_DbLogSizeUsedMbGraph"
							+ ",CmDatabases_DbLogSizeUsedPctGraph"
							;
				}
				else
				{
					// Sybase ASE
					graphList = ""
							+  "CmOpenDatabases_DbSizeMb"
							+ ",CmOpenDatabases_DbLogSizeMbGraph"
							+ ",CmOpenDatabases_DbLogSizeLeftMbGraph"
							+ ",CmOpenDatabases_DbLogSizeUsedMbGraph"
							+ ",CmOpenDatabases_DbLogSizeUsedPctGraph"
							;
				}
			}
			return graphList;
		}

//		public String getSparcLineFreeMb(List<DiskSample> samples, Map<String, String> jsScriptOutMap) 
//		{
//			if (samples == null || samples.isEmpty()) 
//				return "";
//
//			StringJoiner      values   = new StringJoiner(",");
//			StringJoiner      tooltips = new StringJoiner(",");
//			DateTimeFormatter fmtDate  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//			DateTimeFormatter fmtTime  = DateTimeFormatter.ofPattern("HH:mm");
//			ZoneId            zone     = ZoneId.systemDefault();
//
//			SparklineResult sparcLineData = new SparklineResult(SparklineResultType.MIN);
//
//			for (int i = 0; i < samples.size(); i++) 
//			{
//				DiskSample current = samples.get(i);
//				ZonedDateTime startZdt = current.timestamp.atZone(zone);
//
//				// Add it to the "sparkline image creator"
//				sparcLineData.values.add(current.freeMb);
//				sparcLineData.beginTs.add(Timestamp.from(current.timestamp));
//				
//				// Decide end time: Either next sample start time, or (if last) same as start
//				ZonedDateTime endZdt;
//				if (i < samples.size() - 1) 
//				{
//					endZdt = samples.get(i + 1).timestamp.atZone(zone);
//				} 
//				else 
//				{
//					endZdt = startZdt;
//				}
//
//				values.add(String.valueOf(Math.round(current.freeMb)));
//
//				// Tooltip format: "value;date;StartTime;endTime"
//				String tip = String.format("%d;%s;%s;%s",
//						Math.round(current.freeMb),
//						startZdt.format(fmtDate),
//						startZdt.format(fmtTime),
//						endZdt.format(fmtTime)
//						);
//				tooltips.add(tip);
//			}
//
//			String jfreeChartInlinePng = SparklineJfreeChart.create(sparcLineData, SparklineResultType.MIN, 1);
//			
//			String html = String.format("<span class='space-forecast-sparkline' data-values='%s' data-tooltip='%s'>%s</span>", values, tooltips, jfreeChartInlinePng);
//
//			String javaScript = ""
//					+ "$('.space-forecast-sparkline').each(function() \n"
//					+ "{ \n"
//					+ "  var values = $(this).data('values') .split(',').map(Number); \n"
//					+ "  var tips   = $(this).data('tooltip').split(','); \n"
//					+ "  $(this).sparkline(values, \n"
//					+ "  { \n"
//					+ "    type:'line', width:'400px', lineColor:'blue', fillColor:false, \n"
//					+ "    spotRadius:3, highlightSpotColor:'red', highlightLineColor:'red', \n"
//					+ "    tooltipFormatter:function(sparkline, options, fields) \n"
//					+ "    { \n"
//					//			+ "      return tips[fields.offset] + '<br><br>'; \n"
//					+ "      var parts = tips[fields.offset].split(';'); \n"
//					+ "      return parseInt(parts[0], 10).toLocaleString() + ' MB<br><br>' + parts[1] + ' &emsp;&emsp;<br>' +  parts[2] + ' - ' + parts[3] + '<br><br>'; \n"
//					+ "    } \n"
//					+ "  }); \n"
//					+ "}); \n"
//					;
//			jsScriptOutMap.put("run-once", javaScript); 
//
//			return html;
//		}

		public boolean isBetween(Instant target, Instant start, Instant end) 
		{
		    return !target.isBefore(start) && !target.isAfter(end);
		}
//		public String getSparcLineFreeMbAndChangeEvent(List<DiskSample> samples, List<SizeChangedEvent> changeEvent, Map<String, String> jsScriptOutMap) 
		public String getSparcLineFreeMbAndChangeEvent(SpaceForecastResult forecastResult, Map<String, String> jsScriptOutMap) 
		{
			List<DiskSample>       samples     = forecastResult.diskInfoEntry.samples;
			List<SizeChangedEvent> changeEvent = forecastResult.diskInfoEntry.sizeChangedEvents;
			
		    if (samples == null || samples.isEmpty()) 
		        return "";

		    StringJoiner values       = new StringJoiner(",");
		    StringJoiner tooltips     = new StringJoiner(",");
		    StringJoiner changeEvents = new StringJoiner(",");
		    
		    DateTimeFormatter fmtDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		    DateTimeFormatter fmtTime = DateTimeFormatter.ofPattern("HH:mm");
		    ZoneId zone = ZoneId.systemDefault();

			SparklineResult sparcLineData = new SparklineResult(SparklineResultType.MIN);

			for (int i = 0; i < samples.size(); i++) 
		    {
		        DiskSample current = samples.get(i);
		        ZonedDateTime startZdt = current.timestamp.atZone(zone);
		        ZonedDateTime endZdt   = (i < samples.size() - 1) ? samples.get(i + 1).timestamp.atZone(zone) : startZdt;

				// Add it to the "sparkline image creator"
				sparcLineData.values.add(current.freeMbMin);
				sparcLineData.beginTs.add(Timestamp.from(current.timestamp));

				// Add t0 "main line"
		        values.add(String.valueOf(Math.round(current.freeMbMin)));

		        // Add DiskExpantion indicators -- If disk has been extended from previous sample then add "expansion" indicator
		        Instant startInstant = startZdt.toInstant();
		        Instant endInstant   = endZdt  .toInstant();
		        boolean diskExpanded = changeEvent.stream().anyMatch(e -> isBetween(e.ts, startInstant, endInstant));
		        
//		        changeEvents.add( diskExpanded ? "1" : "0"); // only true/false
		        changeEvents.add( diskExpanded ? current.freeMbMin+"" : "0"); // Set to same value as Free MB so we can position the graph at the *correct* height

		        // Tooltip
		        String tip = String.format("%d;%s;%s;%s",
		                Math.round(current.freeMbMin),
		                startZdt.format(fmtDate),
		                startZdt.format(fmtTime),
		                endZdt  .format(fmtTime)
		                );
		        tooltips.add(tip);
		    }

			int imgWidth = getSparklineChartWidth();
			String jfreeChartInlinePng = SparklineJfreeChart.create(sparcLineData, SparklineResultType.MIN, 1, imgWidth, -1);

			forecastResult.sparklineImage = jfreeChartInlinePng;

			String html = String.format("<span class='space-forecast-sparkline' data-values='%s' data-tooltips='%s' data-changes='%s'>%s</span>", 
					values, tooltips, changeEvents, jfreeChartInlinePng);

		    String javaScript = ""
		            + "$('.space-forecast-sparkline').each(function() \n"
		            + "{ \n"
		            + "  var values  = $(this).data('values' ) .toString().split(',').map(Number); \n"
		            + "  var changes = $(this).data('changes') .toString().split(',').map(function(v){ return v === '0' ? null : v; }); \n"
		            + "  var tips    = $(this).data('tooltips').split(','); \n"
		            + "  var cWidth  = " + imgWidth + "; \n"
		            + "\n"
		            + "  $(this).sparkline(values, \n"
		            + "  { \n"
		            + "    type: 'line', \n"
		            + "    width: cWidth + 'px', \n"
		            + "    lineColor: 'blue', \n"
		            + "    fillColor: false, \n"
//		            + "    chartRangeMin: 0, \n"
		            + "    spotRadius: 3, \n"
		            + "    highlightSpotColor: 'blue', \n"
		            + "    highlightLineColor: 'blue', \n"
		            + "    minSpotColor: false, \n"  // do NOT mark this
		            + "    maxSpotColor: false, \n"  // do NOT mark this
		            + "    spotColor: false, \n"
		            + "    tooltipFormatter: function(sparkline, options, fields) \n"
		            + "    { \n"
		            + "      var parts = tips[fields.offset].split(';'); \n"
		            + "      return parseInt(parts[0], 10).toLocaleString() + ' MB<br><br>' + parts[1] + ' &emsp;&emsp;<br>' +  parts[2] + ' - ' + parts[3] + '<br><br>'; \n"
		            + "    } \n"
		            + "  }); \n"
		            + "\n"
		            + "  $(this).sparkline(changes, \n"
		            + "  { \n"
		            + "    composite: true, \n"
		            + "    type: 'line', \n"
		            + "    width: cWidth + 'px', \n"
		            + "    lineColor: 'transparent', \n"
		            + "    fillColor: false, \n"
//		            + "    chartRangeMin: 0, \n"
		            + "    chartRangeMax: Math.max.apply(Math, values), \n" // note: 'values' is the JS array variable of "data"
		            + "    spotColor: 'red', \n"
		            + "    minSpotColor: 'red', \n" // it seems to use the default color (orange) for "min/max" values... 
		            + "    maxSpotColor: 'red', \n"
		            + "    spotRadius: 3, \n"  // Make it a bit larger so it's easy to see (3 is same height as the "hover over" size)
		            + "    highlightSpotColor: 'red', \n"
		            + "    tooltipFormat: 'Size Expansion<br><br>' \n"
		            + "  }); \n"
		            + "}); \n";

		    jsScriptOutMap.put("run-once", javaScript); 
		    return html;
		}
		
		
		private void addHeader(StringBuilder html, String name, String tooltip)
		{
			html.append("<th nowrap style='cursor:pointer' ")
				.append("data-bs-toggle='tooltip' title='")
				.append(tooltip)
				.append("'>")
				.append(name)
				.append("</th> \n");
		}

//		private String getStateBadgeColor(DiskHealthState state)
//		{
//			switch(state)
//			{
//				case CRITICAL:       return "danger";   
//				case WARNING:        return "warning";  
//				case TMP_FILE_RISK:  return "warning";  
//				case ACCELERATING:   return "info";     
//				case FILLING:        return "secondary";
//				case CYCLIC_CLEANUP: return "primary";  
//				case ANOMALY:        return "dark";     
//				default:             return "success";
//			}
//		}

//		private String getStateBadge(DiskHealthState state)
//		{
//			String color = getStateBadgeColor(state);
//			return "<span class='badge bg-" + color + "'>" + state + "</span>";
//		}


//		// confidence badge for ETA reliability
//		private String getConfidenceBadge(EtaConfidence confidence)
//		{
//			switch (confidence)
//			{
//				case HIGH:   return "<span class='badge bg-success'>HIGH</span>";
//				case MEDIUM: return "<span class='badge bg-warning text-dark'>MEDIUM</span>";
//				case LOW:    return "<span class='badge bg-secondary'>LOW</span>";
//				default:     return "-";
//			}
//		}		
	}

	// NEW: value object for TmpFiles dip analysis results
	static class TmpFilesDipStats
	{
		public double  minDuringTmpFilesMb;    // Lowest free MB seen during any dip
		public Instant minDuringTmpFilesMbTs;  // When the worst minimum occurred
		public double  avgTmpFilesDropMb;      // Average MB consumed per TmpFiles
		public double  maxTmpFilesDropMb;      // Largest single TmpFiles in MB
		public Instant maxTmpFilesDropMbTs;    // When the largest dip occurred
		public int     dipCount;               // Number of detected TmpFiles events
		public boolean tmpFilesWillFillDisk;   // Projected to overflow within 90 days

		@JsonGetter("minDuringTmpFilesMbTs")  public Long getMinDuringTmpFilesMbTs()  { return minDuringTmpFilesMbTs  == null ? null : minDuringTmpFilesMbTs.toEpochMilli(); }
		@JsonGetter("maxTmpFilesDropMbTs")    public Long getMaxTmpFilesDropMbTs()    { return maxTmpFilesDropMbTs    == null ? null : maxTmpFilesDropMbTs  .toEpochMilli(); }

		@JsonSetter("minDuringTmpFilesMbTs")  public void setMinDuringTmpFilesMbTs(long ts) { this.minDuringTmpFilesMbTs = Instant.ofEpochMilli(ts); }
		@JsonSetter("maxTmpFilesDropMbTs")    public void setMaxTmpFilesDropMbTs(long ts)   { this.maxTmpFilesDropMbTs   = Instant.ofEpochMilli(ts); }

		// Empty constructor (for JSON)
		public TmpFilesDipStats() {}

		public TmpFilesDipStats(
				double  minDuringTmpFilesMb, 
				Instant minDuringTmpFilesMbTs, 
				double  avgTmpFilesDropMb,
				double  maxTmpFilesDropMb, 
				Instant maxTmpFilesDropMbTs, 
				int     dipCount, 
				boolean tmpFilesWillFillDisk)
		{
			this.minDuringTmpFilesMb   = minDuringTmpFilesMb;
			this.minDuringTmpFilesMbTs = minDuringTmpFilesMbTs;
			this.avgTmpFilesDropMb     = avgTmpFilesDropMb;
			this.maxTmpFilesDropMb     = maxTmpFilesDropMb;
			this.maxTmpFilesDropMbTs   = maxTmpFilesDropMbTs;
			this.dipCount              = dipCount;
			this.tmpFilesWillFillDisk  = tmpFilesWillFillDisk;
		}
	}

	static class SizeChangedEvent
	{
		public String  dbname;
		public Instant ts;
		public int     newSizeMb;
		public int     sizeDiffMb;

		@JsonGetter("ts")  public Long getTs()        { return this.ts == null ? null : this.ts.toEpochMilli(); }
		@JsonSetter("ts")  public void setTs(long ts) { this.ts = Instant.ofEpochMilli(ts); }
		
		// Empty constructor (for JSON)
		public SizeChangedEvent() {}

		public SizeChangedEvent(String dbname, Timestamp ts, int newSizeMb, int sizeDiffMb)
		{
			this.dbname     = dbname;
			this.ts         = ts.toInstant();
			this.newSizeMb  = newSizeMb;
			this.sizeDiffMb = sizeDiffMb;
		}
	}
	
	/**
	 * Holds information for one Disk or Database
	 */
	public static class DiskInfoEntry
	{
		public String name;
		public List<DiskSample> samples = new ArrayList<>();

		public Double  firstFreeSizeMb;
		public Instant firstFreeSizeMbTs;
		public Double  lastFreeSizeMb;
		public Instant lastFreeSizeMbTs;

		public Double  firstTotalSizeMb;
		public Instant firstTotalSizeMbTs;
		public Double  firstPercentUsage;
		public Instant firstPercentUsageTs;

		public Double  lastTotalSizeMb;
		public Instant lastTotalSizeMbTs;
		public Double  lastPercentUsage;
		public Instant lastPercentUsageTs;
		
		public List<SizeChangedEvent> sizeChangedEvents = new ArrayList<>();

		@JsonGetter("firstFreeSizeMbTs")   public Long getFirstFreeSizeMbTs()   { return firstFreeSizeMbTs   == null ? null : firstFreeSizeMbTs  .toEpochMilli(); }
		@JsonGetter("lastFreeSizeMbTs")    public Long getLastFreeSizeMbTs()    { return lastFreeSizeMbTs    == null ? null : lastFreeSizeMbTs   .toEpochMilli(); }
		
		@JsonGetter("firstTotalSizeMbTs")  public Long getFirstTotalSizeMbTs()  { return firstTotalSizeMbTs  == null ? null : firstTotalSizeMbTs .toEpochMilli(); }
		@JsonGetter("firstPercentUsageTs") public Long getFirstPercentUsageTs() { return firstPercentUsageTs == null ? null : firstPercentUsageTs.toEpochMilli(); }
		@JsonGetter("lastTotalSizeMbTs")   public Long getLastTotalSizeMbTs()   { return lastTotalSizeMbTs   == null ? null : lastTotalSizeMbTs  .toEpochMilli(); }
		@JsonGetter("lastPercentUsageTs")  public Long getLastPercentUsageTs()  { return lastPercentUsageTs  == null ? null : lastPercentUsageTs .toEpochMilli(); }

		@JsonSetter("firstFreeSizeMbTs")   public void setFirstFreeSizeMbTs(long ts)   { this.firstFreeSizeMbTs   = Instant.ofEpochMilli(ts); }
		@JsonSetter("lastFreeSizeMbTs")    public void setLastFreeSizeMbTs(long ts)    { this.lastFreeSizeMbTs    = Instant.ofEpochMilli(ts); }

		@JsonSetter("firstTotalSizeMbTs")  public void setFirstTotalSizeMbTs(long ts)  { this.firstTotalSizeMbTs  = Instant.ofEpochMilli(ts); }
		@JsonSetter("firstPercentUsageTs") public void setFirstPercentUsageTs(long ts) { this.firstPercentUsageTs = Instant.ofEpochMilli(ts); }
		@JsonSetter("lastTotalSizeMbTs")   public void setLastTotalSizeMbTs(long ts)   { this.lastTotalSizeMbTs   = Instant.ofEpochMilli(ts); }
		@JsonSetter("lastPercentUsageTs")  public void setLastPercentUsageTs(long ts)  { this.lastPercentUsageTs  = Instant.ofEpochMilli(ts); }

		// Empty constructor (for JSON)
		public DiskInfoEntry() {}

		public DiskInfoEntry(String name)
		{
			this.name = name;
		}

		public DiskInfoEntry(DiskInfoEntry from, List<DiskSample> newSamples)
		{
			this.name = from.name;

			this.firstFreeSizeMb     = from.firstFreeSizeMb;
			this.firstFreeSizeMbTs   = from.firstFreeSizeMbTs;
			this.lastFreeSizeMb      = from.lastFreeSizeMb;
			this.lastFreeSizeMbTs    = from.lastFreeSizeMbTs;

			this.firstTotalSizeMb    = from.firstTotalSizeMb;
			this.firstTotalSizeMbTs  = from.firstTotalSizeMbTs;
			this.firstPercentUsage   = from.firstPercentUsage;
			this.firstPercentUsageTs = from.firstPercentUsageTs;

			this.lastTotalSizeMb     = from.lastTotalSizeMb;
			this.lastTotalSizeMbTs   = from.lastTotalSizeMbTs;
			this.lastPercentUsage    = from.lastPercentUsage;
			this.lastPercentUsageTs  = from.lastPercentUsageTs;
			
			this.sizeChangedEvents   = from.sizeChangedEvents;

			this.samples = newSamples;
		}


		public void addSample(Instant ts, double minValue)
		{
			this.samples.add(new DiskSample(ts, 1, minValue, null));
			
			this.firstFreeSizeMb   = this.samples.get(0).freeMbMin;
			this.firstFreeSizeMbTs = this.samples.get(0).timestamp;

			this.lastFreeSizeMb    = this.samples.get( this.samples.size()-1 ).freeMbMin;
			this.lastFreeSizeMbTs  = this.samples.get( this.samples.size()-1 ).timestamp;
		}

		public void addSizeChangedEvent(SizeChangedEvent event)
		{
			this.sizeChangedEvents.add(event);
		}
	}

//	static class DiskSample
//	{
//		public Instant timestamp;
//		public double  freeMb;
//
//		@JsonGetter("ts")     public long   getTimestamp()   { return timestamp.toEpochMilli(); }
//		@JsonGetter("freeMb") public double getAvailableMb() { return freeMb; }
//
//		@JsonSetter("ts")   public void setTimestamp(long ts)   { this.timestamp = Instant.ofEpochMilli(ts); }
//
//		// Empty constructor (for JSON)
//		public DiskSample() {}
//		
//		public DiskSample(Instant timestamp, double freeMb)
//		{
//			this.timestamp = timestamp;
//			this.freeMb    = freeMb;
//		}
//	}
	static class DiskSample
	{
		public Instant timestamp;
		public int cnt;
		public double  freeMbMin;   // minimum free space within the bucket (= raw value for non-downsampled entries)
		public Double  freeMbAvg;   // (can be null) mean free space within the bucket (= freeMb for non-downsampled entries) not currently used in analysis but available for future trend/regression improvements

		@JsonGetter("ts")        public long   getTimestamp() { return timestamp.toEpochMilli(); }
		@JsonGetter("cnt")       public long   getCount()     { return cnt; }
		@JsonGetter("freeMbMin") public double getFreeMbMin() { return freeMbMin; }
		@JsonGetter("freeMbAvg") public double getFreeMbAvg() { return NumberUtils.round(freeMbAvg, 1); }

		@JsonSetter("ts") public void setTimestamp(long ts) { this.timestamp = Instant.ofEpochMilli(ts); }

		// Empty constructor (for JSON deserialisation)
		public DiskSample() {}

		// Downsampled bucket constructor -- min and avg are distinct
		public DiskSample(Instant timestamp, int count, double freeMbMin, Double freeMbAvg)
		{
			this.timestamp = timestamp;
			this.cnt       = count;
			this.freeMbMin = freeMbMin;
			this.freeMbAvg = freeMbAvg;
		}
	}

	public static class SpaceForecastResult
	{
		public String srvName;
		public String sourceTable;
		public String mount;
		public String extraName;
		public SpaceType spaceType = SpaceType.UNKNOWN;

		public int    downSampleInMinutes;
		public int    days;

		public double           slopeMbPerHour;
		public double           r2;
		public EtaConfidence    etaConfidence;       // NEW: replaces r2 as binary gate
		public double           currentAvailable;
		public double           acceleration;
		public boolean          anomaly;
		public boolean          sawtooth;
		public DiskHealthState  state;
		public CriticalReason   criticalReason;         // why CRITICAL was assigned
		public Double           hoursToFull;
		public Instant          predictedFullTime;
		public double           ciLower;
		public double           ciUpper;
		public TmpFilesDipStats dipStats;                   // NEW
		public Double           hoursUntilTmpFilesOverflow; // NEW
		public Instant          predictedTmpFilesOverflow;  // NEW
		public long             zeroMbEventCount; // number of zero/offline readings detected
		public DiskInfoEntry    diskInfoEntry;

		private String sparklineImage;
		
		public enum SpaceType
		{
			OS_DISK  ("OS Disk"),
			DBMS_DATA("DBMS Data"),
			DBMS_WAL ("DBMS Wal/Log"),
			UNKNOWN  ("Unknown");
			
			public final String label;

			private SpaceType(String label) 
			{
				this.label = label;
			}
		};

		@JsonGetter("predictedFullTime")         public Long getPredictedFullTime()         { return predictedFullTime         == null ? null : predictedFullTime        .toEpochMilli(); }
		@JsonGetter("predictedTmpFilesOverflow") public Long getPredictedTmpFilesOverflow() { return predictedTmpFilesOverflow == null ? null : predictedTmpFilesOverflow.toEpochMilli(); } // NEW

		@JsonSetter("predictedFullTime")         public void setPredictedFullTime(long ts)         { this.predictedFullTime         = Instant.ofEpochMilli(ts); }
		@JsonSetter("predictedTmpFilesOverflow") public void setPredictedTmpFilesOverflow(long ts) { this.predictedTmpFilesOverflow = Instant.ofEpochMilli(ts); }

		// Empty constructor (for JSON)
		public SpaceForecastResult() {}

		public SpaceForecastResult(
			String srvName, String sourceTable, String mount, String extraName,
			double slopeMbPerHour, double r2, EtaConfidence etaConfidence, double currentAvailable,
			double acceleration, boolean anomaly, boolean sawtooth, DiskHealthState state,
			Double hoursToFull, Instant predictedFullTime,
			double ciLower, double ciUpper,
			TmpFilesDipStats dipStats, Double hoursUntilTmpFilesOverflow, Instant predictedTmpFilesOverflow,
			long zeroMbEventCount, CriticalReason criticalReason, DiskInfoEntry diskInfoEntry)
		{
			this.srvName                    = srvName;
			this.sourceTable                = sourceTable;
			this.mount                      = mount;
			this.extraName                  = extraName;
											
			this.slopeMbPerHour             = slopeMbPerHour;
			this.r2                         = r2;
			this.etaConfidence              = etaConfidence;
			this.currentAvailable           = currentAvailable;
			this.acceleration               = acceleration;
			this.anomaly                    = anomaly;
			this.sawtooth                   = sawtooth;
			this.state                      = state;
			this.hoursToFull                = hoursToFull;
			this.predictedFullTime          = predictedFullTime;
			this.ciLower                    = ciLower;
			this.ciUpper                    = ciUpper;
			this.dipStats                   = dipStats;
			this.hoursUntilTmpFilesOverflow = hoursUntilTmpFilesOverflow;
			this.predictedTmpFilesOverflow  = predictedTmpFilesOverflow;
			this.zeroMbEventCount           = zeroMbEventCount;
			this.criticalReason             = criticalReason;
			this.diskInfoEntry              = diskInfoEntry;
			
			if (sourceTable.endsWith("_FsAvailableMb"))         this.spaceType = SpaceType.OS_DISK;
			if (sourceTable.endsWith("_DbDataSizeLeftMbGraph")) this.spaceType = SpaceType.DBMS_DATA;
			if (sourceTable.endsWith("_DbLogSizeLeftMbGraph"))  this.spaceType = SpaceType.DBMS_WAL;

			// Set extraCol based on table name
			if (StringUtil.isNullOrBlank(extraName))
			{
				this.extraName = this.spaceType.label;
			}
		}

		public static String toJson(List<SpaceForecastResult> results)
		{
			try
			{
				ObjectMapper mapper = new ObjectMapper();
				return mapper.writeValueAsString(results);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Failed to serialize results to JSON", e);
			}
		}

		/**
		 * Create a HTML Table from a ResultSetTableModel.<br>
		 * The parameter 'rowSpec' will create one row in the created table for each columns fetched from the 'sourceRstm'.
		 * 
		 * @param htmlClassname Table className
		 * @return
		 */
		public String toHtmlKeyValueTable(String htmlClassname)
		{
			LinkedHashMap<String, String> map = new LinkedHashMap<>();

			map.put("Name"              , getFmtName());
			map.put("Status"            , getFmtState());
			map.put("Slope Hour"        , getFmtSlopeHour());
			map.put("Slope Day"         , getFmtSlopeDay());
			map.put("Acceleration"      , getFmtAcceleration());
			map.put("R&sup2;"           , getFmtRSquared());
			map.put("ETA Confidence"    , getFmtEtaConfidence());
			map.put("ETA Hours"         , getFmtEtaHours());
			map.put("ETA Days"          , getFmtEtaDays());

			map.put("<hr><!-- sep-1 -->", "<hr>"); // Just a blank cell/line
			map.put("Free First"        , getFmtFreeFirst());
			map.put("Free Last"         , getFmtFreeLast());
			map.put("Free Diff"         , getFmtFreeDiff());
			
			map.put("<hr><!-- sep-2 -->", "<hr>"); // Just a blank cell/line
			map.put("Tmp Dips Count"    , getFmtTmpDipsCount());
			map.put("Tmp Min"           , getFmtTmpMin());
			map.put("Tmp Avg"           , getFmtTmpAvg());
			map.put("Tmp Max"           , getFmtTmpMax());
			map.put("Tmp ETA"           , getFmtTmpEta());

			map.put("<hr><!-- sep-3 -->", "<hr>"); // Just a blank cell/line
			map.put("Size First"        , getFmtSizeFirst());
			map.put("Size Last"         , getFmtSizeLast());
			map.put("Size Change Count" , getFmtSizeChangeCount());
			map.put("Size Diff"         , getFmtSizeDiffTotal());
			map.put("Size Diff/Day"     , getFmtSizeDiffPerDay());
			map.put("Size First Used %" , getFmtSizeFirstUsedPct());
			map.put("Size Last Used %"  , getFmtSizeLastUsedPct());

			map.put("<hr><!-- sep-4 -->", "<hr>"); // Just a blank cell/line
			map.put("Sparkline"         , sparklineImage);
			map.put("Data Points"       , getFmtDataPointsAndDays());
			
			String htmlTableSrt = StringUtil.toHtmlTable(map);
			return htmlTableSrt;
		}

		
//		private static String getStateBadgeColor(DiskHealthState state)
//		{
//			switch(state)
//			{
//				case CRITICAL:       return "danger";   
//				case WARNING:        return "warning";  
//				case TMP_FILE_RISK:  return "warning";  
//				case ACCELERATING:   return "info";     
//				case FILLING:        return "secondary";
//				case CYCLIC_CLEANUP: return "primary";  
//				case ANOMALY:        return "dark";     
//				default:             return "success";
//			}
//		}
//
//		private String getStateBadge(DiskHealthState state)
//		{
//			String color = getStateBadgeColor(state);
//			return "<span class='badge bg-" + color + "'>" + state + "</span>";
//		}
//
//
//		// confidence badge for ETA reliability
//		private String getConfidenceBadge(EtaConfidence confidence)
//		{
//			switch (confidence)
//			{
//				case HIGH:   return "<span class='badge bg-success'>HIGH</span>";
//				case MEDIUM: return "<span class='badge bg-warning text-dark'>MEDIUM</span>";
//				case LOW:    return "<span class='badge bg-secondary'>LOW</span>";
//				default:     return "-";
//			}
//		}		
		
		private static final String BADGE_STYLE =
				"display:inline-block; padding:3px 7px; border-radius:4px; " +
						"font-size:11px; font-weight:bold; font-family:Arial,sans-serif;";

		private static String getStateBadgeHexColor(DiskHealthState state)
		{
			switch(state)
			{
				case CRITICAL:       return "#dc3545";
				case WARNING:        return "#ffc107";
				case TMP_FILE_RISK:  return "#ffc107";
				case ACCELERATING:   return "#0dcaf0";
				case FILLING:        return "#6c757d";
				case CYCLIC_CLEANUP: return "#0d6efd";
				case ANOMALY:        return "#212529";
				default:             return "#198754";
			}
		}

			private static String getStateBadgeTextColor(DiskHealthState state)
			{
				switch(state)
				{
					case WARNING:
					case TMP_FILE_RISK:
					case ACCELERATING:   return "#000";
					default:             return "#fff";
				}
			}

			private static String getStateBadge(DiskHealthState state)
			{
				return getStateBadge(state, state.toString(), null);
			}

			private static String getStateBadge(DiskHealthState state, String label)
			{
				return getStateBadge(state, label, null);
			}

			private static String getStateBadge(DiskHealthState state, String label, String title)
			{
				String titleAttr = (title != null && !title.isEmpty()) ? "title='" + title + "' " : "title='" + state.description + "' ";

				return "<span " + titleAttr + "style='" + BADGE_STYLE
						+ "background-color:" + getStateBadgeHexColor(state) + "; "
						+ "color:"            + getStateBadgeTextColor(state) + ";'>"
						+ label
						+ "</span>";
			}

			private String getConfidenceBadge(EtaConfidence confidence)
			{
				switch (confidence)
				{
					case HIGH:   return "<span style='" + BADGE_STYLE + "background-color:#198754; color:#fff;'>HIGH</span>";
					case MEDIUM: return "<span style='" + BADGE_STYLE + "background-color:#ffc107; color:#000;'>MEDIUM</span>";
					case LOW:    return "<span style='" + BADGE_STYLE + "background-color:#6c757d; color:#fff;'>LOW</span>";
					default:     return "-";
				}
			}

		// helpers
		private String helperGetChangedEventsStr()
		{
			String sizeChangedEventsStr = "Size Changes: " + diskInfoEntry.sizeChangedEvents.size() + (diskInfoEntry.sizeChangedEvents.isEmpty() ? "" : "   (Below: List in Reverse Order)") + "\n";
			
			for (int cnt = diskInfoEntry.sizeChangedEvents.size()-1; cnt >= 0; cnt--)
			{
				SizeChangedEvent event = diskInfoEntry.sizeChangedEvents.get(cnt);
				// Note: NO Single Quotes in here...
				sizeChangedEventsStr += " " + (cnt + 1) + " - ts=" + TimeUtils.toStringYmdHms(event.ts) + ", diffMb=" + event.sizeDiffMb + ", newSizeMb=" + event.newSizeMb + "\n";
			}
			
			return sizeChangedEventsStr;
		}
		
		private String helperGetSizeDiffPerDayTimestampStr()
		{
			//------------------------------------------------------------------------------
			// "Size First", "Size Last", "Ch", "Size Diff", "Diff/Day", "First Used %", "Last Used %"

			// Map: YYYY-MM-DD : avgDiffPerDay (sorted by YYYY-MM-DD)
			Map<LocalDate, Double> sumSizeDiffPerDayMap = diskInfoEntry.sizeChangedEvents.stream()
					.filter(e -> e.ts != null)
					.collect(Collectors.groupingBy(
							e -> e.ts.atZone(ZoneId.systemDefault()).toLocalDate(),
							() -> new TreeMap<LocalDate, Double>(Comparator.reverseOrder()), 
							Collectors.summingDouble(e -> e.sizeDiffMb)	
					));

			// Create some strings that will be used in below TD output
			String sizeDiffPerDayTtStr = "";
			if ( ! sumSizeDiffPerDayMap.isEmpty() )
			{
				sizeDiffPerDayTtStr = sumSizeDiffPerDayMap.entrySet().stream()
						.map(e -> String.format(Locale.US, "%s: %,.0f MB", e.getKey(), e.getValue()))
						.collect(Collectors.joining("\n"));					
			}
			return sizeDiffPerDayTtStr;
		}

//		private String helperGetFmtState()
//		{
//			String badge = getStateBadge(state);
//			if (state == DiskHealthState.CRITICAL)
//			{
//				switch (criticalReason)
//				{
//					case SLOPE:
//						badge += " <span class='badge bg-danger' title='Critical Reason: Baseline slope will fill disk within critical threshold'>Slope</span>";
//						break;
//
//					case TMP_FILES:
//						badge += " <span class='badge bg-danger' title='Critical Reason: Max tmpFiles/backup dip exceeds current free space'>Tmp</span>";
//						break;
//
//					case NONE:
//						break;
//
//					default:
//						break;
//				}
//			}
//			if (zeroMbEventCount > 0)
//				badge += " <span class='badge bg-warning text-dark' title='" + zeroMbEventCount + " zero/offline reading(s) excluded from analysis, but still part of the Sparkline'>!! " + zeroMbEventCount + " Zero-MB</span>";
//
//			return badge;
//		}
		private String helperGetFmtState()
		{
			String badge = getStateBadge(state);

			if (state == DiskHealthState.CRITICAL)
			{
				switch (criticalReason)
				{
					case SLOPE:
						badge += " " + getStateBadge(DiskHealthState.CRITICAL, "Slope", "Critical Reason: Baseline slope will fill disk within critical threshold");
						break;

					case TMP_FILES:
						badge += " " + getStateBadge(DiskHealthState.CRITICAL, "Tmp", "Critical Reason: Max tmpFiles/backup dip exceeds current free space");
						break;

					case NONE:
					default:
						break;
				}
			}

			if (zeroMbEventCount > 0)
			{
				badge += " " + getStateBadge(DiskHealthState.WARNING, "!! " + zeroMbEventCount + " Zero-MB", zeroMbEventCount + " zero/offline reading(s) excluded from analysis, but still part of the Sparkline");
			}

			return badge;
		}
		
		//------------------------------------------------------------------------
		// Get HTML Table <td Attributes> , used when creating HTML Table
		//------------------------------------------------------------------------
		private String getTdaName()             { return "nowrap"; }
		private String getTdaState()            { return "nowrap"; }
		private String getTdaSlopeHour()        { return "nowrap align='right'"; }
		private String getTdaSlopeDay()         { return "nowrap align='right'"; }
		private String getTdaAcceleration()     { return "nowrap align='right'"; }
		private String getTdaRSquared()         { return "nowrap align='right'"; }
		private String getTdaEtaConfidence()    { return "nowrap"; }
		private String getTdaEtaHours()         { return "nowrap align='right'"; }
		private String getTdaEtaDays()          { return "nowrap align='right'"; }

		private String getTdaFreeFirst()        { return "nowrap align='right' title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.firstFreeSizeMbTs) + "'"; }
		private String getTdaFreeLast()         { return "nowrap align='right' title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.lastFreeSizeMbTs ) + "'"; }
		private String getTdaFreeDiff()         { return "nowrap align='right'"; }

		private String getTdaTmpDipsCount()     { return "nowrap align='right'"; }
		private String getTdaTmpMin(int danger) { return "nowrap align='right' " + (dipStats.minDuringTmpFilesMb < danger ? " class='table-danger'" : "") 
		                                                                         + "title='Timestamp: " + TimeUtils.toStringYmdHms(dipStats.minDuringTmpFilesMbTs, "-") + "'"
		                                                                         ; }
		private String getTdaTmpAvg()           { return "nowrap align='right'"; }
		private String getTdaTmpMax()           { return "nowrap align='right' title='Timestamp: " + TimeUtils.toStringYmdHms(dipStats.maxTmpFilesDropMbTs, "-") + "'"; }
		private String getTdaTmpEta(int dangerHours, int warningHours)
		{
			String style = "";
			if (hoursUntilTmpFilesOverflow != null)
			{
				style = hoursUntilTmpFilesOverflow < dangerHours  ? " class='table-danger'"
				      : hoursUntilTmpFilesOverflow < warningHours ? " class='table-warning'"
				      :                      "";
			}

			return "nowrap align='right'" + style; 
		}

		private String getTdaSizeFirst()        { return "nowrap title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.firstTotalSizeMbTs) + "'"; }
		private String getTdaSizeLast()         { return "nowrap title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.lastTotalSizeMbTs ) + "'"; }
		private String getTdaSizeChangeCount()  { return "nowrap title='" + helperGetChangedEventsStr()           + "' data-toggle='tooltip'"; }
		private String getTdaSizeDiffTotal()    { return "nowrap title='" + helperGetChangedEventsStr()           + "' data-toggle='tooltip'"; }
		private String getTdaSizeDiffPerDay()   { return "nowrap title='" + helperGetSizeDiffPerDayTimestampStr() + "' data-toggle='tooltip'"; }
		private String getTdaSizeFirstUsedPct() { return "nowrap title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.firstPercentUsageTs) + "'"; }
		private String getTdaSizeLastUsedPct()  { return "nowrap title='Timestamp: " + TimeUtils.toStringYmdHms(diskInfoEntry.lastPercentUsageTs ) + "'"; }

		private String getTdaDataPoints()       { return "nowrap"; }
		private String getTdaDataPointsAndDays(){ return "nowrap align='right' title='Samples: " + diskInfoEntry.samples.size() + "\nFirst TS: " + TimeUtils.toStringYmdHms(diskInfoEntry.samples.get(0).timestamp) + "\nLast TS: " + TimeUtils.toStringYmdHms(diskInfoEntry.samples.get(diskInfoEntry.samples.size()-1).timestamp) + "'"; }


		//------------------------------------------------------------------------
		// Get Formatted data, used when creating HTML Table
		//------------------------------------------------------------------------
		private String getFmtName()             { return mount; }
		private String getFmtState()            { return helperGetFmtState(); }
		private String getFmtSlopeHour()        { return String.format(Locale.US, "%,.1f MB", slopeMbPerHour); }
		private String getFmtSlopeDay()         { return String.format(Locale.US, "%,.1f MB", slopeMbPerHour * 24); }
		private String getFmtAcceleration()     { return String.format(Locale.US, "%,.1f", acceleration); }
		private String getFmtRSquared()         { return String.format(Locale.US, "%,.3f", r2); }
		private String getFmtEtaConfidence()    { return getConfidenceBadge(etaConfidence); }
		private String getFmtEtaHours()         { return hoursToFull == null ? "&infin;" : String.format(Locale.US, "%,.0f", hoursToFull); }
		private String getFmtEtaDays()          { return hoursToFull == null ? "&infin;" : String.format(Locale.US, "%,.0f", hoursToFull / 24); }

		private String getFmtFreeFirst()        { return String.format(Locale.US, "%,.0f MB", diskInfoEntry.firstFreeSizeMb); }
		private String getFmtFreeLast()         { return String.format(Locale.US, "%,.0f MB", diskInfoEntry.lastFreeSizeMb); }
		private String getFmtFreeDiff()         { Double firstLastFreeMbDiff = null; if (diskInfoEntry.firstFreeSizeMb != null && diskInfoEntry.lastFreeSizeMb != null) { firstLastFreeMbDiff = diskInfoEntry.lastFreeSizeMb - diskInfoEntry.firstFreeSizeMb; } return firstLastFreeMbDiff == null ? "-" : String.format(Locale.US, "%,.0f MB", firstLastFreeMbDiff); }

		private String getFmtTmpDipsCount()     { return dipStats.dipCount <= 0 ? "-"   : dipStats.dipCount + ""; }
		private String getFmtTmpMin()           { return dipStats.dipCount <= 0 ? "n/a" : String.format(Locale.US, "%,.0f MB", dipStats.minDuringTmpFilesMb); }
		private String getFmtTmpAvg()           { return dipStats.dipCount <= 0 ? "n/a" : String.format(Locale.US, "%,.1f GB", dipStats.avgTmpFilesDropMb / 1024); }
		private String getFmtTmpMax()           { return dipStats.dipCount <= 0 ? "n/a" : String.format(Locale.US, "%,.1f GB", dipStats.maxTmpFilesDropMb / 1024); }
		private String getFmtTmpEta()           { return dipStats.dipCount <= 0 ? "n/a" : hoursUntilTmpFilesOverflow == null ? "&infin;" : String.format(Locale.US, "%,.0f d", hoursUntilTmpFilesOverflow / 24.0); }

		private String getFmtSizeFirst()        { return diskInfoEntry.firstTotalSizeMb    == null ? "-" : String.format(Locale.US, "%,.0f MB", diskInfoEntry.firstTotalSizeMb ); }
		private String getFmtSizeLast()         { return diskInfoEntry.lastTotalSizeMb     == null ? "-" : String.format(Locale.US, "%,.0f MB", diskInfoEntry.lastTotalSizeMb  ); }
		private String getFmtSizeChangeCount()  { return diskInfoEntry.sizeChangedEvents.isEmpty() ? "-" : diskInfoEntry.sizeChangedEvents.size() + ""; }
		private String getFmtSizeDiffTotal()    
		{
			Double firstLastSizeMbDiff = null;
			if (diskInfoEntry.firstTotalSizeMb != null && diskInfoEntry.lastTotalSizeMb != null)
				firstLastSizeMbDiff = diskInfoEntry.lastTotalSizeMb - diskInfoEntry.firstTotalSizeMb;
			if (firstLastSizeMbDiff != null && firstLastSizeMbDiff == 0)
				firstLastSizeMbDiff = null;

			return firstLastSizeMbDiff == null ? "-" : String.format(Locale.US, "%,.0f MB", firstLastSizeMbDiff);
		}
		private String getFmtSizeDiffPerDay()   
		{
			// Map: YYYY-MM-DD : avgDiffPerDay (sorted by YYYY-MM-DD)
			Map<LocalDate, Double> sumSizeDiffPerDayMap = diskInfoEntry.sizeChangedEvents.stream()
					.filter(e -> e.ts != null)
					.collect(Collectors.groupingBy(
							e -> e.ts.atZone(ZoneId.systemDefault()).toLocalDate(),
							() -> new TreeMap<LocalDate, Double>(Comparator.reverseOrder()), 
							Collectors.summingDouble(e -> e.sizeDiffMb)	
					));
			// Get avgPerDay / numOfDays
			double totalAvgSizeDiffPerDay = sumSizeDiffPerDayMap.values().stream()
					.mapToDouble(Double::doubleValue)
					.average()
					.orElse(0.0);

			// Create some strings that will be used in below TD output
			String sizeDiffPerDayStr   = "-";
			if (totalAvgSizeDiffPerDay > 0)
			{
				sizeDiffPerDayStr   = String.format(Locale.US, "%,.0f MB", totalAvgSizeDiffPerDay);
			}
			
			return sizeDiffPerDayStr; 
		}
		private String getFmtSizeFirstUsedPct() { return diskInfoEntry.firstPercentUsage == null ? "-" : String.format(Locale.US, "%,.1f%%" , diskInfoEntry.firstPercentUsage); }
		private String getFmtSizeLastUsedPct()  { return diskInfoEntry.lastPercentUsage  == null ? "-" : String.format(Locale.US, "%,.1f%%" , diskInfoEntry.lastPercentUsage ); }

		private String getFmtDataPoints()       { return diskInfoEntry.samples.size() + ""; }
		private String getFmtDataPointsAndDays() 
		{
			List<DiskSample> samples = diskInfoEntry.samples;
			Instant firstSample = samples.get(0).timestamp;
			Instant lastSample  = samples.get(samples.size()-1).timestamp;
			long diffHours = ChronoUnit.HOURS.between(firstSample, lastSample);
			String numDataStr = diffHours + "h";
			if (diffHours > 48)
			{
				long diffDays = ChronoUnit.DAYS.between(firstSample, lastSample);
				if (diffHours % 24 > 12) // round to nearest day, since above truncates to LOWER day
					diffDays++;
				numDataStr = diffDays + "d";
			}
			return samples.size() + " / " + numDataStr;
		}
		
		
		@Override
		public String toString()
		{
			return "mount="                   + String.format("%-20s", mount)
					+" | state="              + String.format("%-15s", state)
					+" | slope="              + String.format("%.2f",  slopeMbPerHour)
					+" | R2="                 + String.format("%.3f",  r2)
					+" | confidence="         + etaConfidence
					+" | current="            + currentAvailable
					+" | ETA="                + hoursToFull
					+" | TmpFilesMinMb="      + (dipStats.dipCount > 0 ? String.format("%.0f", dipStats.minDuringTmpFilesMb) : "n/a")
					+" | TmpFilesMaxDrop="    + (dipStats.dipCount > 0 ? String.format("%.0f GB", dipStats.maxTmpFilesDropMb/1024) : "n/a")
					+" | TmpFilesOverflowETA="+ (hoursUntilTmpFilesOverflow != null ? String.format("%.0f h / %.1f d", hoursUntilTmpFilesOverflow, hoursUntilTmpFilesOverflow/24) : "n/a"
					+" | zeroMbEventCount="   + zeroMbEventCount
					);
		}
	}

//	public void toJson(PrintWriter out, List<DiskForecastResult> results)
//	{
//		out.append( DiskForecastResult.toJson(results) );
//	}
//
//	public void generateHtmlReport(PrintWriter out, List<DiskForecastResult> results)
//	{
//		out.append( "-NOT-YET-IMPLEMENTED-" );
//	}

	@JsonIgnore
	public List<SpaceForecastResult> getSeverity(DiskHealthState state)
	{
		List<SpaceForecastResult> results = getForecastResult();
		
		List<SpaceForecastResult> out = results.stream()
			.filter(r -> state.equals(r.state))
			.collect(Collectors.toList());

		return out;
	}

	@JsonIgnore
	public List<SpaceForecastResult> getSeverityCriticals()
	{
		return getSeverity(DiskHealthState.CRITICAL);
	}

	@JsonIgnore
	public List<SpaceForecastResult> getSeverityWarnings()
	{
		return getSeverity(DiskHealthState.WARNING);
	}

	@JsonIgnore
	public List<SpaceForecastResult> getSeverityOthers()
	{
		List<SpaceForecastResult> results = getForecastResult();

		List<SpaceForecastResult> out = results.stream()
			.filter(r -> ! (DiskHealthState.CRITICAL.equals(r.state) || DiskHealthState.WARNING.equals(r.state)) )
			.collect(Collectors.toList());

		return out;
	}
	
	public static List<SpaceForecastResult> getSeverity(List<SpaceForecastResult> results, DiskHealthState state)
	{
		List<SpaceForecastResult> out = results.stream()
			.filter(r -> state.equals(r.state))
			.collect(Collectors.toList());

		return out;
	}

	public static List<SpaceForecastResult> getSeverityCriticals(List<SpaceForecastResult> results)
	{
		return getSeverity(results, DiskHealthState.CRITICAL);
	}

	public static List<SpaceForecastResult> getSeverityWarnings(List<SpaceForecastResult> results)
	{
		return getSeverity(results, DiskHealthState.WARNING);
	}

	public static List<SpaceForecastResult> getSeverityOthers(List<SpaceForecastResult> results)
	{
		List<SpaceForecastResult> out = results.stream()
			.filter(r -> ! (DiskHealthState.CRITICAL.equals(r.state) || DiskHealthState.WARNING.equals(r.state)) )
			.collect(Collectors.toList());

		return out;
	}
}
