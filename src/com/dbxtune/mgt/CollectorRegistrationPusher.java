/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
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
package com.dbxtune.mgt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Pushes the local collector info file ({@code SRVNAME.dbxtune}) to DbxCentral
 * via {@code POST /api/cc/mgt/collector/register}.
 *
 * <h3>Why this exists</h3>
 * When DbxCentral runs on a different host than the collector, the info file
 * written to {@code $HOME/.dbxtune/info/SRVNAME.dbxtune} on the collector's
 * host is never visible to DbxCentral. By pushing the content at collector
 * startup, DbxCentral can store it locally and use it for proxying requests.
 *
 * <h3>Configuration</h3>
 * Set {@code DbxTune.central.url=http://central-host:8080} in the collector's
 * configuration. If this property is absent or blank, the push is silently
 * skipped (suitable for standalone / same-host deployments).
 *
 * <h3>Retry</h3>
 * If DbxCentral is not reachable at startup, a daemon background thread retries
 * every 60 seconds until the first successful delivery.
 */
public class CollectorRegistrationPusher
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_centralUrl = "DbxTune.central.url";

	/** Seconds between retry attempts when Central is not reachable. */
	private static final int RETRY_INTERVAL_SEC = 60;

	/** HTTP connect+request timeout per attempt. */
	private static final int HTTP_TIMEOUT_SEC = 10;

	private static final HttpClient _httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SEC))
			.build();

	/**
	 * Reads the local info file and pushes its content to DbxCentral.
	 * If the Central URL is not configured, the method returns immediately.
	 * If the push fails, a background retry thread is started automatically.
	 *
	 * @param srvName       server/alias name (used as the filename on Central)
	 * @param infoFilePath  absolute path to the local {@code SRVNAME.dbxtune} file
	 */
	public static void push(String srvName, String infoFilePath)
	{
		Configuration cfg        = Configuration.getCombinedConfiguration();
		String        centralUrl = cfg.getPropertyRaw(PROPKEY_centralUrl, null);

		if (StringUtil.isNullOrBlank(centralUrl))
		{
			_logger.debug("CollectorRegistrationPusher: '" + PROPKEY_centralUrl + "' is not configured — skipping push to DbxCentral.");
			return;
		}
		centralUrl = centralUrl.trim().replaceAll("/+$", ""); // strip trailing slashes

		// No need to push when Central is on the same host — the info file is already local
		String centralHost = centralUrl.replaceAll("(?i)^https?://", "").replaceAll("[:/].*", "");
		if ("localhost".equalsIgnoreCase(centralHost) || "127.0.0.1".equals(centralHost))
		{
			_logger.debug("CollectorRegistrationPusher: Central URL points to localhost (" + centralUrl + ") — skipping push (file is already local).");
			return;
		}

		String token = cfg.getPropertyRaw(NoGuiManagementServer.PROPKEY_collectorRegToken,
		                                  NoGuiManagementServer.DEFAULT_collectorRegToken);

		_logger.info("CollectorRegistrationPusher: Pushing collector info to DbxCentral at '" + centralUrl + "' for server '" + srvName + "'.");

		final String fSrvName      = srvName;
		final String fInfoFilePath = infoFilePath;
		final String fCentralUrl   = centralUrl;
		final String fToken        = token;

		// Try once immediately; if it fails, hand off to the retry thread
		if (!attempt(fSrvName, fInfoFilePath, fCentralUrl, fToken))
			startRetryThread(fSrvName, fInfoFilePath, fCentralUrl, fToken);
	}

	// -------------------------------------------------------------------------

	private static boolean attempt(String srvName, String infoFilePath, String centralUrl, String token)
	{
		try
		{
			String body = new String(Files.readAllBytes(Paths.get(infoFilePath)), StandardCharsets.UTF_8);

			String url = centralUrl + "/api/cc/mgt/collector/register"
					+ "?srv=" + URLEncoder.encode(srvName, StandardCharsets.UTF_8.name());

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(HTTP_TIMEOUT_SEC))
					.header("Content-Type", "text/plain; charset=UTF-8")
					.header("Authorization", "Bearer " + token)
					.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300)
			{
				_logger.info("CollectorRegistrationPusher: Successfully registered with DbxCentral. srv='" + srvName + "', status=" + response.statusCode());
				return true;
			}
			else
			{
				_logger.warn("CollectorRegistrationPusher: DbxCentral returned HTTP " + response.statusCode()
						+ " for srv='" + srvName + "'. body=" + response.body());
				return false;
			}
		}
		catch (IOException ex)
		{
			_logger.warn("CollectorRegistrationPusher: Failed to reach DbxCentral at '" + centralUrl
					+ "' for srv='" + srvName + "': " + ex.getMessage());
			return false;
		}
		catch (Exception ex)
		{
			_logger.warn("CollectorRegistrationPusher: Unexpected error for srv='" + srvName + "': " + ex.getMessage(), ex);
			return false;
		}
	}

	private static void startRetryThread(String srvName, String infoFilePath, String centralUrl, String token)
	{
		Thread t = new Thread(() -> {
			_logger.info("CollectorRegistrationPusher: Starting background retry thread (every " + RETRY_INTERVAL_SEC + "s) for srv='" + srvName + "'.");
			while (!Thread.currentThread().isInterrupted())
			{
				try { Thread.sleep(RETRY_INTERVAL_SEC * 1000L); }
				catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }

				if (attempt(srvName, infoFilePath, centralUrl, token))
					break; // success — stop retrying
			}
			_logger.info("CollectorRegistrationPusher: Retry thread exiting for srv='" + srvName + "'.");
		}, "CollectorRegistrationPusher-" + srvName);
		t.setDaemon(true);
		t.start();
	}
}
