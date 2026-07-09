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

/**
 * A single LLM backend (Claude, OpenAI, Gemini, Ollama, ...) that can turn a
 * SQL statement plus its DDL/stats/plan context into an optimization suggestion.
 * <p>
 * Implementations are registered in {@link LlmClientRegistry}.
 */
public interface LlmClient
{
	/** Short, stable id used in config properties and API requests, e.g. {@code "claude"}. */
	String getProviderId();

	/** Human readable name, e.g. {@code "Claude (Anthropic)"}. */
	String getDisplayName();

	/** True if this provider is fully configured (API key / URL present) and can be used. */
	boolean isEnabled();

	/** Call the LLM and return its optimization suggestion. */
	LlmOptimizeResponse optimize(LlmOptimizeRequest request) throws Exception;

	/**
	 * Build the exact prompt text {@link #optimize} would send, without calling the LLM. Pure/no
	 * side effects - callers can use this to show "what we tried to send" even when {@link #optimize}
	 * itself fails (e.g. the provider's HTTP call failing after the prompt was already built).
	 */
	String buildPrompt(LlmOptimizeRequest request);
}
