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
package com.dbxtune.central.llm;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.dbxtune.utils.StringUtil;

/**
 * Shrink a SQL Server ShowPlan XML document before it's embedded in an LLM prompt.
 * <p>
 * <b>LLM-prompt use only</b> - called from a single place, {@link LlmClientAbstract#buildPrompt}.
 * Does not touch the plan anywhere else: the graphical plan viewer (qp.js, driven by
 * {@code dbxShowplan.js}), raw plan retrieval/storage ({@code QueryStoreServlet}), or MCP tooling
 * ({@code McpHandler}) all keep using their own, unmodified copy of the XML.
 * <p>
 * Element/attribute names below are cross-checked against Microsoft's published schema
 * (<a href="https://schemas.microsoft.com/sqlserver/2004/07/showplan/sql2019/showplanxml.xsd">
 * showplanxml.xsd</a>, SQL 2019), not just observed samples - see each rule's javadoc for the
 * schema detail that justifies it.
 * <p>
 * Each transformation step below documents, in one place, exactly what it removes/collapses and
 * why it's safe for LLM-prompt purposes specifically (i.e. what a tuning recommendation actually
 * needs vs. what's compiler/scheduler bookkeeping). Falls back to returning the original,
 * unmodified XML text if the document doesn't parse (e.g. a truncated "live" plan captured
 * mid-execution, or an unexpected shape) - never worse than sending the raw plan.
 */
public class SqlServerPlanXmlShrinker
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * {@code RunTimeCountersPerThread} attributes that are additive across threads (each thread
	 * processed a disjoint slice of the work, so the totals are meaningful): row/read/CPU counters.
	 */
	private static final Set<String> SUM_ATTRS = new HashSet<>(Arrays.asList(
			"ActualRows", "ActualRowsRead", "ActualExecutions", "ActualCPUms", "Batches",
			"ActualScans", "ActualLogicalReads", "ActualPhysicalReads", "ActualReadAheads",
			"ActualLobLogicalReads", "ActualLobPhysicalReads", "ActualLobReadAheads",
			"ActualRebinds", "ActualRewinds", "ActualPageServerReads", "ActualPageServerReadAheads",
			"ActualLobPageServerReads", "ActualLobPageServerReadAheads", "ActualLocallyAggregatedRows"));

	/**
	 * {@code RunTimeCountersPerThread} attributes that take the MAX across threads, not the sum -
	 * threads run concurrently, so wall-clock elapsed time is bounded by the slowest thread, not
	 * the sum of all of them (summing would wildly overstate how long the operator actually ran).
	 */
	private static final Set<String> MAX_ATTRS = new HashSet<>(Arrays.asList("ActualElapsedms"));

	/**
	 * {@code RunTimeCountersPerThread} attributes that are "1" if ANY thread reports "1" - these
	 * are completion flags (did this thread finish its scan), not counters, so OR is the correct
	 * combination, not sum (which would produce a meaningless value > 1) or first-wins (which could
	 * incorrectly report "still running" for an operator that actually did complete).
	 */
	private static final Set<String> ANY_ONE_ATTRS = new HashSet<>(Arrays.asList("ActualEndOfScans"));

	/**
	 * {@code RunTimeCountersPerThread} attributes dropped entirely, on every thread's element,
	 * whether or not there's more than one thread to aggregate. Confirmed against the SQL 2019
	 * showplanxml.xsd attribute list for this element - these are per-thread scheduler/task
	 * bookkeeping, not query-tuning content, and critically must NOT fall into the generic
	 * "unknown numeric attribute -> sum" fallback in {@link #combine}: {@code TaskAddr}/
	 * {@code SchedulerId}/{@code BrickId} are opaque internal ids, and the six *Time attributes are
	 * wall-clock timestamps - summing either across threads produces a garbage number that looks
	 * like a real counter but means nothing.
	 */
	private static final Set<String> DROP_COUNTER_ATTRS = new HashSet<>(Arrays.asList(
			"TaskAddr", "SchedulerId", "BrickId",
			"FirstActiveTime", "LastActiveTime", "OpenTime", "FirstRowTime", "LastRowTime", "CloseTime"));

	/**
	 * Element local names dropped wholesale, wherever they appear:
	 * <ul>
	 * <li>{@code OptimizerHardwareDependentProperties} - compile-time hardware estimates (available
	 *     memory grant/pages cached/DOP the optimizer assumed) - describes the compiling machine,
	 *     not the query; never actionable in a rewrite/index recommendation.
	 * <li>{@code StatementSetOptions} - the ANSI_NULLS/QUOTED_IDENTIFIER/... session options the
	 *     statement compiled under. Occasionally the root cause of an unexpected plan choice, but
	 *     essentially never what a tuning recommendation acts on; dropped to keep the common case lean.
	 * <li>{@code OptimizerStatsUsage} - a flat list of every statistics object consulted at compile
	 *     time (one {@code StatisticsInfo} row per column-set of every table touched, each with
	 *     Database/Schema/Table/Statistics/ModificationCount/SamplingPercent/LastUpdate) - on a plan
	 *     with N tables this is O(N) rows of compile-time bookkeeping, often the single largest
	 *     block in a plan that references many tables. Trade-off: this does drop stats-staleness
	 *     visibility ("was this plan compiled against outdated/low-sampled stats") - acceptable here
	 *     since that's rarely the actionable finding for a SQL-rewrite/index suggestion, but worth
	 *     knowing if advice quality suffers on plans where stale stats are the actual root cause.
	 * </ul>
	 */
	private static final Set<String> DROP_ELEMENTS = new HashSet<>(Arrays.asList(
			"OptimizerHardwareDependentProperties", "StatementSetOptions", "OptimizerStatsUsage"));

	/**
	 * Attribute local names dropped wherever found: {@code QueryHash}/{@code QueryPlanHash} (opaque
	 * hex hashes used by SQL Server internally to group similar plans/queries - meaningless to an
	 * LLM, not tied to anything else in the prompt) and {@code SecurityPolicyApplied} (a boolean
	 * about Row-Level Security involvement - not query-tuning content).
	 */
	private static final Set<String> DROP_ATTRS = new HashSet<>(Arrays.asList(
			"QueryHash", "QueryPlanHash", "SecurityPolicyApplied"));

	private SqlServerPlanXmlShrinker()
	{
	}

	public static String shrink(String xmlPlan)
	{
		if (StringUtil.isNullOrBlank(xmlPlan))
			return xmlPlan;

		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setExpandEntityReferences(false);
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			// Suppress Xerces' default behavior of printing parse errors to stderr - a truncated "live"
			// plan (captured mid-execution, see dbxShowplan.js's _ssPlanType()) is an expected, already
			// logged-via-fallback case here, not something that should also spam the server console.
			db.setErrorHandler(new org.xml.sax.ErrorHandler()
			{
				@Override public void warning(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { }
				@Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
				@Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
			});
			Document doc = db.parse(new InputSource(new StringReader(xmlPlan)));

			Element root = doc.getDocumentElement();
			collapseRunTimeCounters(root);
			collapseScalarStringOperators(root);
			dropOutputLists(root);
			dropKnownLowValueNodes(root);
			stripWhitespaceText(root);

			return serialize(doc);
		}
		catch (Exception ex)
		{
			_logger.debug("shrink(): could not parse/shrink plan XML, sending it unmodified. reason={}", ex.toString());
			return xmlPlan;
		}
	}

	/**
	 * Collapse every operator's {@code RunTimeCountersPerThread} children (one per thread, on
	 * "actual"/"live" - i.e. executed, not just estimated - parallel plans) into a single aggregated
	 * element, and strip {@link #DROP_COUNTER_ATTRS} from every counter element regardless of thread
	 * count. This is the dominant size reducer on wide-parallel actual plans: DOP=8 means 8
	 * near-identical, dozen-plus-attribute blocks per operator, collapsed here to 1.
	 * <p>
	 * Aggregation preserves every total a tuning recommendation needs (rows processed, logical/
	 * physical reads, CPU time) via {@link #SUM_ATTRS}/{@link #MAX_ATTRS}/{@link #ANY_ONE_ATTRS} -
	 * only per-thread skew detail is lost, replaced by a {@code ThreadCount} attribute noting how
	 * many threads were merged. Serial plans (thread count 1, the common case for OLTP-style
	 * statements) and estimated plans (no counters at all) have nothing to aggregate and pass
	 * through the attribute-cleanup only.
	 */
	private static void collapseRunTimeCounters(Element root)
	{
		NodeList runtimeInfoList = root.getElementsByTagNameNS("*", "RunTimeInformation");
		for (int i = 0; i < runtimeInfoList.getLength(); i++)
		{
			Element runtimeInfo = (Element) runtimeInfoList.item(i);
			NodeList counters = runtimeInfo.getElementsByTagNameNS("*", "RunTimeCountersPerThread");

			int threadCount = counters.getLength();
			if (threadCount == 0)
				continue;

			if (threadCount == 1)
			{
				stripCounterDropAttrs((Element) counters.item(0));
				continue;
			}

			Map<String, String> aggregated = new LinkedHashMap<>();
			for (int c = 0; c < counters.getLength(); c++)
			{
				Element counter = (Element) counters.item(c);
				NamedNodeMap attrs = counter.getAttributes();
				for (int a = 0; a < attrs.getLength(); a++)
				{
					Attr attr  = (Attr) attrs.item(a);
					String name  = attr.getLocalName() != null ? attr.getLocalName() : attr.getName();
					String value = attr.getValue();

					if ("Thread".equals(name) || DROP_COUNTER_ATTRS.contains(name))
						continue; // "Thread" is replaced by ThreadCount below; DROP_COUNTER_ATTRS - see javadoc

					aggregated.merge(name, value, (oldVal, newVal) -> combine(name, oldVal, newVal));
				}
			}

			// Fold the aggregate into the first per-thread element (also clearing any of its own
			// DROP_COUNTER_ATTRS values, since those were skipped above and would otherwise survive
			// unchanged from thread 0), drop the rest.
			Element first = (Element) counters.item(0);
			first.removeAttribute("Thread");
			stripCounterDropAttrs(first);
			for (Map.Entry<String, String> e : aggregated.entrySet())
				first.setAttribute(e.getKey(), e.getValue());
			first.setAttribute("ThreadCount", String.valueOf(threadCount));

			for (int c = counters.getLength() - 1; c >= 1; c--)
				runtimeInfo.removeChild(counters.item(c));
		}
	}

	private static void stripCounterDropAttrs(Element counter)
	{
		for (String attrName : DROP_COUNTER_ATTRS)
			counter.removeAttribute(attrName);
	}

	private static String combine(String attrName, String a, String b)
	{
		if (MAX_ATTRS.contains(attrName))
			return formatNum(Math.max(parseNum(a), parseNum(b)));

		if (ANY_ONE_ATTRS.contains(attrName))
			return ("1".equals(a) || "1".equals(b)) ? "1" : a;

		if (SUM_ATTRS.contains(attrName))
			return formatNum(parseNum(a) + parseNum(b));

		// Unknown attribute (a future SQL Server version added something not in SUM/MAX/ANY_ONE/DROP
		// above): if both sides parse as numbers, sum them as a reasonable default for what is, after
		// all, a per-thread counter; otherwise keep the first thread's value rather than guess how to
		// combine non-numeric data.
		if (isNumeric(a) && isNumeric(b))
			return formatNum(parseNum(a) + parseNum(b));

		return a;
	}

	private static boolean isNumeric(String s)
	{
		if (s == null)
			return false;
		try { Double.parseDouble(s); return true; }
		catch (NumberFormatException ex) { return false; }
	}
	private static double parseNum(String s)
	{
		try { return Double.parseDouble(s); }
		catch (Exception ex) { return 0; }
	}
	private static String formatNum(double d)
	{
		if (d == Math.floor(d) && !Double.isInfinite(d))
			return String.valueOf((long) d);
		return String.valueOf(d);
	}

	/**
	 * Collapse every element that carries a {@code ScalarString} attribute down to just that
	 * attribute, dropping its child element tree.
	 * <p>
	 * Per the showplanxml.xsd, {@code ScalarString} is an optional attribute on the base
	 * {@code ScalarType} used by every {@code ScalarOperator} - and SQL Server always populates it
	 * with the exact human-readable text of the expression the operator represents, e.g.
	 * {@code "[Transaktion].[Legal_enhetskod]=[Verifikation].[Legal_enhetskod] AND ..."}. The child
	 * elements ({@code Logical}/{@code Compare}/{@code Identifier}/{@code ColumnReference}/
	 * {@code Const}/{@code Intrinsic}/{@code Convert}/...) encode that <i>exact same expression</i>
	 * a second time, as a machine-parseable operator tree - useful to whatever originally executed
	 * the query, not to an LLM reading the {@code ScalarString} text. This single rule collapses
	 * join predicates ({@code ProbeResidual}), filter predicates ({@code Predicate}), and computed-
	 * column expressions ({@code ComputeScalar}'s {@code DefinedValue}s) wherever they appear -
	 * on a plan with many joins/computed columns, this and {@link #dropOutputLists} are typically
	 * the largest reduction of the whole transform, since both scale with operator count.
	 */
	private static void collapseScalarStringOperators(Element el)
	{
		if (el.hasAttribute("ScalarString"))
		{
			while (el.getFirstChild() != null)
				el.removeChild(el.getFirstChild());
			return; // nothing left to recurse into
		}

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
				collapseScalarStringOperators((Element) n);
		}
	}

	/**
	 * Drop every {@code OutputList} element (the list of columns a {@code RelOp} passes upward to
	 * its parent operator).
	 * <p>
	 * Per the showplanxml.xsd, {@code OutputList} is required on every {@code RelOp} - so on a plan
	 * with many stacked operators (e.g. a long join chain), the same handful of in-flight columns
	 * gets fully-qualified (Database/Schema/Table/Alias/Column) and re-listed at every single
	 * operator level, one entry per column per operator. This is derived pass-through bookkeeping
	 * (what a node happens to carry upward), not diagnostic content - the things that actually
	 * explain a performance problem (operator type, estimated vs. actual rows, join/filter
	 * predicates via {@code ScalarString}, table/index names via {@code Object}) live elsewhere in
	 * the tree and are untouched by this rule.
	 */
	private static void dropOutputLists(Element root)
	{
		NodeList list = root.getElementsByTagNameNS("*", "OutputList");
		for (int i = list.getLength() - 1; i >= 0; i--)
		{
			Node n = list.item(i);
			n.getParentNode().removeChild(n);
		}
	}

	/** Drop {@link #DROP_ELEMENTS} wholesale and {@link #DROP_ATTRS} wherever found - see their javadoc for what/why. */
	private static void dropKnownLowValueNodes(Element root)
	{
		for (String elemName : DROP_ELEMENTS)
		{
			NodeList list = root.getElementsByTagNameNS("*", elemName);
			for (int i = list.getLength() - 1; i >= 0; i--)
			{
				Node n = list.item(i);
				n.getParentNode().removeChild(n);
			}
		}

		removeAttrsRecursively(root);
	}

	private static void removeAttrsRecursively(Element el)
	{
		for (String attrName : DROP_ATTRS)
			el.removeAttribute(attrName);

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
				removeAttrsRecursively((Element) n);
		}
	}

	/**
	 * Remove whitespace-only text nodes (pure formatting - the original document's indentation)
	 * so re-serialization doesn't reproduce it; zero information loss.
	 */
	private static void stripWhitespaceText(Node node)
	{
		NodeList children = node.getChildNodes();
		for (int i = children.getLength() - 1; i >= 0; i--)
		{
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty())
				node.removeChild(child);
			else if (child.getNodeType() == Node.ELEMENT_NODE)
				stripWhitespaceText(child);
		}
	}

	private static String serialize(Document doc) throws Exception
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}
}
