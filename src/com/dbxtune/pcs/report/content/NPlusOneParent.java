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
package com.dbxtune.pcs.report.content;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.utils.Configuration;

/**
 * Finds the <b>parent</b> of a possible N+1 <i>child</i> statement: the statement whose rows per sample follow the child's executions per sample.
 * <p>
 * Used by the "Possible N+1 / Chatty Statements" sections of the Daily Summary Report (SQL Server: CmChattyStatements and Query Store, Postgres: CmPgStatements, ASE: CmChattyStatements).
 */
public class NPlusOneParent
{
	public static final String  PROPKEY_minCorrelation = "DailySummaryReport.NPlusOne.parent.minCorrelation";
	public static final double  DEFAULT_minCorrelation = 0.9;

	public static final String  PROPKEY_minExecPerRow  = "DailySummaryReport.NPlusOne.parent.minExecPerParentRow";
	public static final double  DEFAULT_minExecPerRow  = 0.5;

	public static final String  PROPKEY_maxExecPerRow  = "DailySummaryReport.NPlusOne.parent.maxExecPerParentRow";
	public static final double  DEFAULT_maxExecPerRow  = 1.5;

	// The "N" in N+1: child executions per parent EXECUTION. Statements executed in lockstep (same transaction, 1 row each) have 1.0
	public static final String  PROPKEY_minFanOut      = "DailySummaryReport.NPlusOne.parent.minExecPerParentExec";
	public static final double  DEFAULT_minFanOut      = 2.0;

	// Samples (or Query Store intervals) where BOTH the child executed and the parent returned rows
	public static final String  PROPKEY_minSamples     = "DailySummaryReport.NPlusOne.parent.minSamples";
	public static final int     DEFAULT_minSamples     = 4;

	// sum(|childExec - ratio * parentRows|) / sum(childExec) over all samples: 0 = exactly proportional
	public static final String  PROPKEY_maxFitError    = "DailySummaryReport.NPlusOne.parent.maxFitError";
	public static final double  DEFAULT_maxFitError    = 0.15;

	/** Executions and rows for one statement, per sample (or per Query Store interval) */
	public static class Series
	{
		public String dbname;
		public Map<Timestamp, long[]> perSample = new HashMap<>(); // [ExecCount, RowCount]
	}

	/** The parent found for a child statement */
	public static class ParentMatch
	{
		public String  parentKey;
		public double  corr;
		public double  execPerParentRow;   // child executions / parent rows        (about 1.0)
		public double  execPerParentExec;  // child executions / parent executions  (the "N")
	}

	/**
	 * Find the statement whose rows follow the child's executions:
	 * <ul>
	 *   <li>same database</li>
	 *   <li>child executions / parent rows about 1.0</li>
	 *   <li>child executions / parent executions at least 2 (the parent returns many rows; statements that just run together in one transaction have 1.0)</li>
	 *   <li>at least ## samples where both the child executed and the parent returned rows</li>
	 *   <li>the parent's rows per sample must follow the child's executions per sample: Pearson correlation, and proportional (small fit error)</li>
	 * </ul>
	 *
	 * @param childKey   key of the child in seriesMap
	 * @param seriesMap  all statements (key -> per sample executions and rows)
	 * @param samples    all sample times (or Query Store intervals), in order
	 * @return null if no parent was found
	 */
	public static ParentMatch findParent(String childKey, Map<String, Series> seriesMap, List<Timestamp> samples)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		double minCorrelation = conf.getDoubleProperty(PROPKEY_minCorrelation, DEFAULT_minCorrelation);
		double minExecPerRow  = conf.getDoubleProperty(PROPKEY_minExecPerRow,  DEFAULT_minExecPerRow);
		double maxExecPerRow  = conf.getDoubleProperty(PROPKEY_maxExecPerRow,  DEFAULT_maxExecPerRow);
		double minFanOut      = conf.getDoubleProperty(PROPKEY_minFanOut,      DEFAULT_minFanOut);
		int    minSamples     = conf.getIntProperty   (PROPKEY_minSamples,     DEFAULT_minSamples);
		double maxFitError    = conf.getDoubleProperty(PROPKEY_maxFitError,    DEFAULT_maxFitError);

		Series child = seriesMap.get(childKey);
		if (child == null)
			return null;

		double[] x = new double[samples.size()];
		int childSamples = 0;
		for (int i = 0; i < samples.size(); i++)
		{
			long[] v = child.perSample.get(samples.get(i));
			x[i] = v == null ? 0 : v[0];
			if (v != null && v[0] > 0)
				childSamples++;
		}
		// Too few samples: a match on the totals alone is too often a coincidence (tested: 8 of 10 were wrong with 1-2 Query Store intervals)
		if (childSamples < minSamples)
			return null;
		if (sumOf(x) <= 0)
			return null;

		ParentMatch best = null;
		for (Map.Entry<String, Series> e : seriesMap.entrySet())
		{
			String parentKey = e.getKey();
			Series parent    = e.getValue();
			if (parentKey.equals(childKey))
				continue;
			if (child.dbname != null && parent.dbname != null && ! child.dbname.equals(parent.dbname))
				continue;

			double[] y = new double[samples.size()];
			double sumX = 0, sumY = 0, sumParentExec = 0;
			int commonSamples = 0;
			for (int i = 0; i < samples.size(); i++)
			{
				long[] v = parent.perSample.get(samples.get(i));
				y[i] = v == null ? 0 : v[1];
				sumX += x[i];
				sumY += y[i];
				sumParentExec += v == null ? 0 : v[0];
				if (x[i] > 0 && y[i] > 0)
					commonSamples++;
			}
			if (sumY <= 0 || sumParentExec <= 0)
				continue;

			// Both must be active in the same samples (a correlation on 2-3 points is a coincidence)
			if (commonSamples < minSamples)
				continue;

			double ratio = sumX / sumY; // child executions per parent row
			if (ratio < minExecPerRow || ratio > maxExecPerRow)
				continue;

			double fanOut = sumX / sumParentExec; // child executions per parent execution
			if (fanOut < minFanOut)
				continue;

			// Proportional, not just correlated: child executions = ratio * parent rows, in every sample
			double absErr = 0;
			for (int i = 0; i < samples.size(); i++)
				absErr += Math.abs(x[i] - ratio * y[i]);
			double fitError = absErr / sumX;
			if (fitError > maxFitError)
				continue;

			double corr = pearson(x, y);
			if (corr < minCorrelation || (best != null && corr <= best.corr))
				continue;

			best = new ParentMatch();
			best.parentKey         = parentKey;
			best.corr              = corr;
			best.execPerParentRow  = ratio;
			best.execPerParentExec = fanOut;
		}
		return best;
	}

	private static double sumOf(double[] x)
	{
		double s = 0;
		for (double d : x)
			s += d;
		return s;
	}

	private static double pearson(double[] x, double[] y)
	{
		int n = x.length;
		if (n < 2)
			return 0;

		double mx = 0, my = 0;
		for (int i = 0; i < n; i++) { mx += x[i]; my += y[i]; }
		mx /= n; my /= n;

		double sxy = 0, sxx = 0, syy = 0;
		for (int i = 0; i < n; i++)
		{
			double dx = x[i] - mx;
			double dy = y[i] - my;
			sxy += dx * dy;
			sxx += dx * dx;
			syy += dy * dy;
		}
		if (sxx == 0 || syy == 0)
			return 0;

		return sxy / Math.sqrt(sxx * syy);
	}
}
