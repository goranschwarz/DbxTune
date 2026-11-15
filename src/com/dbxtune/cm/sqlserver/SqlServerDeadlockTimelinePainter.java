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
package com.dbxtune.cm.sqlserver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.dbxtune.cm.sqlserver.CmDeadlocks.Deadlock;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlProcess;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlResource;

/**
 * Draws a SQL Server deadlock as a timeline: one swim lane per session.
 * <pre>
 *  spid 77  [ holds X on customers ][ waits X on orders  ]| killed (1205)
 *  spid 78     [ holds X on orders  ][ waits X on customers ]|[ continues ]
 *           tran start ...  wait start ...           detected
 * </pre>
 * Arrows go from a waiting session to the session that holds the lock it waits for.
 * <p>
 * The columns are the known moments (transaction start and wait start of every session) in time order, each labeled with
 * its time relative to the detection, so a transaction that started hours ago and a wait of 50 ms are both readable.
 * The report has no time for when a lock was taken, so the "holds" bar starts at the transaction start.
 */
public class SqlServerDeadlockTimelinePainter
{
	private static final int WIDTH        = 960;
	private static final int MARGIN       = 14;
	private static final int LABEL_W      = 170;  // left column with spid/app
	private static final int AFTER_W      = 120;  // after the detection line
	private static final int LANE_H       = 34;
	private static final int LANE_GAP     = 26;
	private static final int HEADER_H     = 34;
	private static final int MAX_LANES    = 8;

	private static final Color BG          = Color.WHITE;
	private static final Color TEXT        = new Color(0x2C, 0x2C, 0x2A);
	private static final Color TEXT_MUTED  = new Color(0x5F, 0x5E, 0x5A);
	private static final Color HOLD_FILL   = new Color(0xE1, 0xF5, 0xEE);
	private static final Color HOLD_LINE   = new Color(0x0F, 0x6E, 0x56);
	private static final Color HOLD_TEXT   = new Color(0x08, 0x50, 0x41);
	private static final Color WAIT_FILL   = new Color(0xFC, 0xEB, 0xEB);
	private static final Color WAIT_LINE   = new Color(0xA3, 0x2D, 0x2D);
	private static final Color WAIT_TEXT   = new Color(0x79, 0x1F, 0x1F);
	private static final Color ARROW       = new Color(0xE2, 0x4B, 0x4A);
	private static final Color AXIS        = new Color(0x88, 0x87, 0x80);

	private static final Font  FONT        = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font  FONT_BOLD   = new Font(Font.SANS_SERIF, Font.BOLD,  13);
	private static final Font  FONT_SMALL  = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font  FONT_MONO   = new Font(Font.MONOSPACED, Font.PLAIN, 11);

	/** One swim lane = one session (all ecids of a spid, for parallel queries) */
	private static class Lane
	{
		int              spid;
		boolean          victim;
		List<DlProcess>  procs = new ArrayList<>();
		Timestamp        tranStart;
		long             waitTimeMs;
		List<DlResource> waitsFor = new ArrayList<>();
		List<DlResource> holds    = new ArrayList<>();
		String           waitMode;
		int              y;
		// x positions
		int              xStart;
		int              xWait;

		DlProcess first() { return procs.get(0); }
	}

	/**
	 * Create a HTML &lt;img&gt; tag with the picture as a base64 PNG.
	 * @param d      The deadlock
	 * @param count  How many deadlocks look like this one (shown in the header if above 1)
	 */
	public static String toHtmlImg(Deadlock d, int count)
	{
		try
		{
			BufferedImage img = paint(d, count);
			return "<img width='" + img.getWidth() + "' height='" + img.getHeight() + "' src='data:image/png;base64," + Base64.getEncoder().encodeToString(toPng(img)) + "'>";
		}
		catch (Exception ex)
		{
			return "<i>Could not draw the deadlock: " + ex + "</i>";
		}
	}

	/**
	 * PNG with a 256 color palette made from the picture's own most used colors
	 * (less than half the size of a RGB PNG, and the colors stay right, which they don't with Java's default palette)
	 */
	public static byte[] toPng(BufferedImage img)
	throws IOException
	{
		int w = img.getWidth();
		int h = img.getHeight();

		// Count the colors
		Map<Integer, Integer> count = new HashMap<>();
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				count.merge(img.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);

		List<Integer> colors = new ArrayList<>(count.keySet());
		colors.sort((a, b) -> Integer.compare(count.get(b), count.get(a)));

		int n = Math.max(2, Math.min(256, colors.size()));
		byte[] r = new byte[n];
		byte[] g = new byte[n];
		byte[] b = new byte[n];
		for (int i = 0; i < n && i < colors.size(); i++)
		{
			int c = colors.get(i);
			r[i] = (byte) (c >> 16);
			g[i] = (byte) (c >> 8);
			b[i] = (byte) c;
		}
		BufferedImage indexed = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(8, n, r, g, b));

		// Map each pixel to the nearest palette color (cached per color, there are only a few thousand)
		Map<Integer, Integer> nearest = new HashMap<>();
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int c = img.getRGB(x, y) & 0xFFFFFF;
				Integer idx = nearest.get(c);
				if (idx == null)
				{
					long bestDist = Long.MAX_VALUE;
					int  best     = 0;
					for (int i = 0; i < n; i++)
					{
						int  dr   = ((c >> 16) & 0xFF) - (r[i] & 0xFF);
						int  dg   = ((c >>  8) & 0xFF) - (g[i] & 0xFF);
						int  db   = ( c        & 0xFF) - (b[i] & 0xFF);
						long dist = (long) dr * dr + dg * dg + db * db;
						if (dist < bestDist)
						{
							bestDist = dist;
							best     = i;
							if (dist == 0)
								break;
						}
					}
					idx = best;
					nearest.put(c, idx);
				}
				indexed.getRaster().setSample(x, y, 0, idx);
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(indexed, "png", out);
		return out.toByteArray();
	}

	public static BufferedImage paint(Deadlock d, int count)
	{
		List<Lane> lanes = createLanes(d);

		Timestamp detect = d.eventTime;
		if (detect == null)
		{
			// should not happen: use the latest tran start
			for (Lane l : lanes)
				if (l.tranStart != null && (detect == null || l.tranStart.after(detect)))
					detect = l.tranStart;
			if (detect == null)
				detect = new Timestamp(0);
		}

		// The known moments (ms), in time order: tran start and wait start of every lane
		TreeSet<Long> moments = new TreeSet<>();
		for (Lane l : lanes)
		{
			long waitStart = detect.getTime() - l.waitTimeMs;
			if (l.tranStart != null && l.tranStart.getTime() < waitStart)
				moments.add(l.tranStart.getTime());
			if (l.waitTimeMs > 0)
				moments.add(waitStart);
		}
		if (moments.isEmpty())
			moments.add(detect.getTime() - 1);

		int plotX0  = MARGIN + LABEL_W;
		int detectX = WIDTH - MARGIN - AFTER_W;
		List<Long> momentList = new ArrayList<>(moments);
		Map<Long, Integer> momentX = new LinkedHashMap<>();
		int n = momentList.size();
		for (int i = 0; i < n; i++)
			momentX.put(momentList.get(i), plotX0 + (int) ((detectX - plotX0) * (i / (double) n)));

		// statement lines below the lanes
		List<String> stmtLines = new ArrayList<>();
		for (Lane l : lanes)
		{
			DlProcess p = l.first();
			String proc = p.getProcNameAndLine();
			String stmt = p.getStatement() == null ? "" : p.getStatement().replaceAll("\\s+", " ").trim();
			stmtLines.add("spid " + l.spid + ": " + (proc == null ? "" : proc + ": ") + stmt);
		}
		for (DlResource r : d.resources)
		{
			StringBuilder sb = new StringBuilder();
			sb.append(r.type);
			if (r.mode != null)
				sb.append(" ").append(r.mode);
			if (r.objectName != null)
				sb.append(" on ").append(r.objectName);
			if (r.indexName != null)
				sb.append(", index ").append(r.indexName);
			sb.append(": held by ").append(spids(d, r.ownerIds, r.ownerModes)).append(", wanted by ").append(spids(d, r.waiterIds, r.waiterModes));
			stmtLines.add(sb.toString());
		}

		int lanesTop = MARGIN + HEADER_H;
		int axisY    = lanesTop + lanes.size() * (LANE_H + LANE_GAP) - LANE_GAP + 16;
		int stmtTop  = axisY + 40;
		int height   = stmtTop + stmtLines.size() * 16 + 30;

		BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(BG);
			g.fillRect(0, 0, WIDTH, height);

			// Header
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			String header = (d.eventTime == null ? "" : sdf.format(d.eventTime) + "   ")
					+ (d.getVictimDbName() == null ? "" : "database " + d.getVictimDbName() + "   ")
					+ (d.getLockTypes() == null ? "" : d.getLockTypes() + "   ")
					+ (d.parallel ? "parallel query   " : "")
					+ (count > 1 ? count + " deadlocks like this" : "");
			g.setFont(FONT_BOLD);
			g.setColor(TEXT);
			g.drawString(header, MARGIN, MARGIN + 14);

			// Lanes
			for (int i = 0; i < lanes.size(); i++)
			{
				Lane l = lanes.get(i);
				l.y = lanesTop + i * (LANE_H + LANE_GAP);

				long waitStart = detect.getTime() - l.waitTimeMs;
				l.xWait  = l.waitTimeMs > 0 ? momentX.get(waitStart) : detectX;
				l.xStart = (l.tranStart != null && momentX.containsKey(l.tranStart.getTime())) ? momentX.get(l.tranStart.getTime()) : Math.max(plotX0, l.xWait - 60);

				// left label
				DlProcess p = l.first();
				g.setFont(FONT_BOLD);
				g.setColor(TEXT);
				g.drawString("spid " + l.spid, MARGIN, l.y + 14);
				g.setFont(FONT_SMALL);
				g.setColor(l.victim ? WAIT_LINE : TEXT_MUTED);
				String sub = (l.victim ? "victim  " : "") + nvl(p.app, nvl(p.login, ""));
				g.drawString(fit(g, sub, LABEL_W - 10), MARGIN, l.y + 29);

				// holds bar
				String holdsTxt = l.holds.isEmpty() ? "running" : "holds " + describe(l.holds, true, l);
				drawBar(g, l.xStart, l.y, l.xWait - l.xStart, holdsTxt, HOLD_FILL, HOLD_LINE, HOLD_TEXT, false);

				// waits bar
				if (l.waitTimeMs > 0)
				{
					String waitsTxt = "waits " + (l.waitMode == null ? "" : l.waitMode + " ") + "on " + (l.waitsFor.isEmpty() ? nvl(p.waitResource, "?") : describe(l.waitsFor, false, l)) + "  (" + formatMs(l.waitTimeMs) + ")";
					drawBar(g, l.xWait, l.y, detectX - l.xWait, waitsTxt, WAIT_FILL, WAIT_LINE, WAIT_TEXT, true);
				}

				// after detection
				if (l.victim)
				{
					g.setFont(FONT);
					g.setColor(WAIT_LINE);
					g.drawString("killed (1205)", detectX + 10, l.y + LANE_H / 2 + 5);
				}
				else
				{
					drawBar(g, detectX, l.y, AFTER_W - 10, "continues", HOLD_FILL, HOLD_LINE, HOLD_TEXT, false);
				}
			}

			// Arrows: waiting lane -> lane that holds the lock
			Map<Integer, Lane> laneBySpid = new LinkedHashMap<>();
			for (Lane l : lanes)
				laneBySpid.put(l.spid, l);
			int arrowNo = 0;
			for (Lane from : lanes)
			{
				Set<Integer> done = new LinkedHashSet<>();
				for (DlResource r : from.waitsFor)
				{
					for (String ownerId : r.ownerIds)
					{
						DlProcess owner = d.getProcess(ownerId);
						if (owner == null || owner.spid == from.spid || !done.add(owner.spid))
							continue;
						Lane to = laneBySpid.get(owner.spid);
						if (to == null)
							continue;

						int fromX = from.xWait + Math.min(40 + (arrowNo % 4) * 18, Math.max(10, (detectX - from.xWait) / 2));
						int toX   = Math.max(to.xStart + 10, to.xWait - 20 - (arrowNo % 4) * 14);
						int fromY = to.y > from.y ? from.y + LANE_H : from.y;
						int toY   = to.y > from.y ? to.y            : to.y + LANE_H;
						drawArrow(g, fromX, fromY, toX, toY);
						arrowNo++;
					}
				}
			}

			// Detection line
			Stroke old = g.getStroke();
			g.setColor(AXIS);
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
			g.drawLine(detectX, lanesTop - 8, detectX, axisY);
			g.setStroke(old);

			// Axis with the moments
			g.setColor(AXIS);
			g.drawLine(plotX0, axisY, detectX + AFTER_W - 10, axisY);
			g.setFont(FONT_SMALL);
			int lastLabelEnd = Integer.MIN_VALUE;
			for (Map.Entry<Long, Integer> e : momentX.entrySet())
			{
				int x = e.getValue();
				g.setColor(AXIS);
				g.drawLine(x, axisY - 4, x, axisY + 4);
				String lbl = "-" + formatMs(detect.getTime() - e.getKey());
				int w = g.getFontMetrics().stringWidth(lbl);
				if (x - w / 2 > lastLabelEnd + 6)
				{
					g.setColor(TEXT_MUTED);
					g.drawString(lbl, x - w / 2, axisY + 18);
					lastLabelEnd = x + w / 2;
				}
			}
			g.setColor(TEXT);
			String detLbl = "detected" + (d.eventTime == null ? "" : " " + new SimpleDateFormat("HH:mm:ss.SSS").format(d.eventTime));
			g.drawString(detLbl, Math.min(detectX - g.getFontMetrics().stringWidth(detLbl) / 2, WIDTH - MARGIN - g.getFontMetrics().stringWidth(detLbl)), axisY + 18);

			// Statements
			g.setFont(FONT_MONO);
			g.setColor(TEXT);
			for (int i = 0; i < stmtLines.size(); i++)
				g.drawString(fit(g, stmtLines.get(i), WIDTH - 2 * MARGIN), MARGIN, stmtTop + i * 16);

			// Legend
			g.setFont(FONT_SMALL);
			g.setColor(TEXT_MUTED);
			g.drawString(fit(g, "Bars start at the transaction start (the report has no time for when a lock was taken). Red = waiting for a lock. Arrow = waits for a lock held by that session.", WIDTH - 2 * MARGIN), MARGIN, height - 10);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	private static List<Lane> createLanes(Deadlock d)
	{
		Map<Integer, Lane> map = new LinkedHashMap<>();
		for (Integer spid : d.getSpids())
		{
			Lane l = new Lane();
			l.spid = spid;
			map.put(spid, l);
		}
		for (DlProcess p : d.processes)
		{
			Lane l = map.get(p.spid);
			l.procs.add(p);
			l.victim |= p.victim;
			if (p.tranStarted != null && (l.tranStart == null || p.tranStarted.before(l.tranStart)))
				l.tranStart = p.tranStarted;
			l.waitTimeMs = Math.max(l.waitTimeMs, p.waitTimeMs);
		}
		for (Lane l : map.values())
		{
			// put a process that has statement text first (parallel: the coordinator, ecid 0, is usually it)
			l.procs.sort((a, b) -> Integer.compare(a.ecid, b.ecid));

			Set<String> ids = new LinkedHashSet<>();
			for (DlProcess p : l.procs)
				ids.add(p.id);
			for (DlResource r : d.resources)
			{
				for (int i = 0; i < r.waiterIds.size(); i++)
				{
					if (ids.contains(r.waiterIds.get(i)))
					{
						if (!l.waitsFor.contains(r))
							l.waitsFor.add(r);
						if (l.waitMode == null && !r.waiterModes.get(i).isEmpty())
							l.waitMode = r.waiterModes.get(i);
					}
				}
				for (String id : r.ownerIds)
					if (ids.contains(id) && !l.holds.contains(r))
						l.holds.add(r);
			}
		}

		List<Lane> lanes = new ArrayList<>(map.values());
		if (lanes.size() > MAX_LANES)
			lanes = lanes.subList(0, MAX_LANES);
		return lanes;
	}

	/** "X on dlk.dbo.orders, S on dlk.dbo.x" (holds: with the owner mode) */
	private static String describe(List<DlResource> list, boolean holds, Lane l)
	{
		Set<String> parts = new LinkedHashSet<>();
		for (DlResource r : list)
		{
			String mode = null;
			if (holds)
			{
				for (int i = 0; i < r.ownerIds.size(); i++)
					for (DlProcess p : l.procs)
						if (p.id.equals(r.ownerIds.get(i)))
							mode = r.ownerModes.get(i);
			}
			String obj = shortName(r.getObjectOrType());
			parts.add((holds && mode != null && !mode.isEmpty() ? mode + " on " : "") + obj);
		}
		return String.join(", ", parts);
	}

	/** "dlk.dbo.orders" -> "dbo.orders" (the database is in the header) */
	private static String shortName(String name)
	{
		if (name == null)
			return "?";
		String[] p = name.split("\\.");
		return p.length == 3 ? p[1] + "." + p[2] : name;
	}

	private static void drawBar(Graphics2D g, int x, int y, int w, String text, Color fill, Color line, Color textColor, boolean dashed)
	{
		if (w < 2)
			w = 2;
		g.setColor(fill);
		g.fillRoundRect(x, y, w, LANE_H, 6, 6);
		Stroke old = g.getStroke();
		if (dashed)
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 3f}, 0f));
		g.setColor(line);
		g.drawRoundRect(x, y, w, LANE_H, 6, 6);
		g.setStroke(old);

		g.setFont(FONT);
		String t = fit(g, text, w - 10);
		if (t.isEmpty())
			return;
		g.setColor(textColor);
		g.drawString(t, x + 6, y + LANE_H / 2 + 5);
	}

	private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2)
	{
		g.setColor(ARROW);
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke(1.6f));
		g.drawLine(x1, y1, x2, y2);
		g.setStroke(old);

		double ang = Math.atan2(y2 - y1, x2 - x1);
		int len = 9;
		Polygon head = new Polygon();
		head.addPoint(x2, y2);
		head.addPoint((int) (x2 - len * Math.cos(ang - Math.PI / 7)), (int) (y2 - len * Math.sin(ang - Math.PI / 7)));
		head.addPoint((int) (x2 - len * Math.cos(ang + Math.PI / 7)), (int) (y2 - len * Math.sin(ang + Math.PI / 7)));
		g.fillPolygon(head);
	}

	/** Cut the text so it fits in the width (ends with "..."), empty if not even a few characters fit */
	private static String fit(Graphics2D g, String text, int width)
	{
		FontMetrics fm = g.getFontMetrics();
		if (fm.stringWidth(text) <= width)
			return text;
		String ell = "...";
		for (int len = text.length() - 1; len > 3; len--)
		{
			String t = text.substring(0, len) + ell;
			if (fm.stringWidth(t) <= width)
				return t;
		}
		return "";
	}

	/** 48 ms, 2.1 s, 3 min 12 s, 2 h 13 min */
	static String formatMs(long ms)
	{
		if (ms < 1000)       return ms + " ms";
		if (ms < 10_000)     return String.format(java.util.Locale.ROOT, "%.2f s", ms / 1000.0);
		if (ms < 60_000)     return String.format(java.util.Locale.ROOT, "%.1f s", ms / 1000.0);
		if (ms < 3_600_000)  return (ms / 60_000) + " min " + ((ms % 60_000) / 1000) + " s";
		return (ms / 3_600_000) + " h " + ((ms % 3_600_000) / 60_000) + " min";
	}

	/** "spid 80 (X), spid 81 (S)" */
	private static String spids(Deadlock d, List<String> ids, List<String> modes)
	{
		Set<String> parts = new LinkedHashSet<>();
		for (int i = 0; i < ids.size(); i++)
		{
			DlProcess p = d.getProcess(ids.get(i));
			String mode = i < modes.size() && !modes.get(i).isEmpty() ? " (" + modes.get(i) + ")" : "";
			parts.add((p == null ? ids.get(i) : "spid " + p.spid) + mode);
		}
		return parts.isEmpty() ? "-" : String.join(", ", parts);
	}

	private static String nvl(String s, String def)
	{
		return s == null ? def : s;
	}
}
