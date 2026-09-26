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
package com.dbxtune.central.controllers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Server side helpers for pages that draw a timeline with <code>/scripts/dbxtune/js/dbxTimeline.js</code>
 * (which is built on vis-timeline, and replaced the Google Charts Timeline).
 * <p>
 * A page builds a list of rows with {@link #createRow(String, String, String, String, Timestamp, Timestamp)},
 * adds any optional fields (label, parentKey, laneKey, ...) described in dbxTimeline.js, and writes them
 * into a script tag with {@link #toScriptJson(Object)}.
 */
public class DbxTimelineRows
{
	/** JavaScript files needed by dbxTimeline.js (jQuery and moment are loaded by the page template) */
	public static final List<String> JAVASCRIPT_LIST = Arrays.asList(
			"/scripts/vis-timeline/8.5.4/vis-timeline-graph2d.min.js",
			"/scripts/dbxtune/js/dbxTimeline.js");

	/** CSS files needed by dbxTimeline.js */
	public static final List<String> CSS_LIST = Arrays.asList(
			"/scripts/vis-timeline/8.5.4/vis-timeline-graph2d.min.css");

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	/**
	 * Create one row (one bar in the timeline)
	 *
	 * @param key      Row key, bars with the same key are drawn on the same row
	 * @param text     Text on the bar
	 * @param color    Any CSS color
	 * @param tooltip  HTML tooltip, or null to get a simple default tooltip
	 * @param startTs  Start time
	 * @param endTs    End time
	 */
	public static Map<String, Object> createRow(String key, String text, String color, String tooltip, Timestamp startTs, Timestamp endTs)
	{
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("key"  , key);
		row.put("text" , text);
		row.put("color", color);
		if (tooltip != null)
			row.put("tooltip", tooltip);
		row.put("start", toIsoTs(startTs));
		row.put("end"  , toIsoTs(endTs));
		return row;
	}

	/**
	 * Timestamp as <code>yyyy-MM-ddTHH:mm:ss</code> without time zone, which JavaScript <code>new Date(str)</code> parses as local time.
	 * <br>
	 * So the browser shows the same wall clock time as the server (like the Google Timeline did)
	 */
	public static String toIsoTs(Timestamp ts)
	{
		return ts == null ? null : TimeUtils.toStringYmdHms(ts).replace(' ', 'T');
	}

	/**
	 * Same as {@link #toIsoTs(Timestamp)} but as a JavaScript literal: <code>'yyyy-MM-ddTHH:mm:ss'</code> or <code>null</code>
	 */
	public static String toJsTs(Timestamp ts)
	{
		return ts == null ? "null" : "'" + toIsoTs(ts) + "'";
	}

	/**
	 * Object as JSON, which is safe to write inside a <code>&lt;script&gt;</code> tag
	 * (a string value containing <code>&lt;/script&gt;</code> can not end the script)
	 */
	public static String toScriptJson(Object obj)
	throws JsonProcessingException
	{
		return _objectMapper.writeValueAsString(obj)
				.replace("</", "<\\/")
				.replace("<!--", "<\\!--");
	}
}
