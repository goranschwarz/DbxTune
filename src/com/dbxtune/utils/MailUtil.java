/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 ******************************************************************************/
package com.dbxtune.utils;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central utility for creating pre-configured {@link HtmlEmail} objects via a fluent builder.
 *
 * <h3>Quickstart — auto-discovered config:</h3>
 * <pre>
 *   MailUtil.createHtmlEmail()
 *       .setTo("user@acme.com")
 *       .setCc("manager@acme.com")
 *       .setSubject("Hello")
 *       .setMessage("&lt;b&gt;World&lt;/b&gt;")
 *       .send();
 * </pre>
 *
 * <h3>Explicit config:</h3>
 * <pre>
 *   MailUtil.MailConfig cfg = MailUtil.getConfig("ReportSenderToMail");
 *   MailUtil.createHtmlEmail(cfg)
 *       .setTo("user@acme.com")
 *       ...
 *       .send();
 * </pre>
 *
 * <p>Auto-discovery searches {@link #AUTO_DISCOVER_PREFIXES} in order and uses the first
 * prefix whose {@code .smtp.hostname} property is non-empty.
 * The list is intentionally mutable so callers can register additional prefixes at startup:
 * <pre>
 *   MailUtil.AUTO_DISCOVER_PREFIXES.add(0, "MyCustomSender"); // highest priority
 * </pre>
 *
 * <p>Per-property fallback: when building a {@link MailConfig} from a non-{@code "mail"} prefix,
 * any property that is empty/unset will fall back to the corresponding {@code mail.*} property
 * (i.e. the same suffix under the {@code "mail"} prefix).  This mirrors how
 * {@code UserDefinedActionAbstract} reads its SMTP settings, so a single set of
 * {@code mail.smtp.*} properties in the global config works as a universal default.
 */
public class MailUtil
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** The fallback prefix used when a per-property value is missing from the primary prefix. */
	public static final String FALLBACK_PREFIX = "mail";


	// =========================================================================
	// MailException
	// =========================================================================

	/** Checked exception thrown when email cannot be configured or sent. */
	public static class MailException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public MailException(String message)                        { super(message); }
		public MailException(String message, Throwable cause)       { super(message, cause); }
	}

	/**
	 * Ordered list of configuration prefixes tried during auto-discovery.
	 * The first prefix whose {@code .smtp.hostname} is non-empty wins.
	 * <p>
	 * Mutable by design — add your own prefix at startup if needed.
	 */
	public static final List<String> AUTO_DISCOVER_PREFIXES = new ArrayList<>(Arrays.asList(
			"mail",
			"ReportSenderToMail",
			"AlarmWriterToMail"
	));


	// =========================================================================
	// Factory methods
	// =========================================================================

	/**
	 * Create a pre-configured {@link MailBuilder} by auto-discovering SMTP settings
	 * from the first matching prefix in {@link #AUTO_DISCOVER_PREFIXES}.
	 *
	 * @throws MailException if no usable SMTP configuration is found
	 */
	public static MailBuilder createHtmlEmail()
	throws MailException
	{
		MailConfig config = autoDiscoverConfig();
		if (config == null)
		{
			String prefixes = String.join(", ", AUTO_DISCOVER_PREFIXES);
			throw new MailException(
					"No SMTP configuration found. Tried prefixes: [" + prefixes + "]. "
					+ "Set at least '" + AUTO_DISCOVER_PREFIXES.get(0) + MailConfig.SUFFIX_SMTP_HOST + "' in the configuration.");
		}
		return createHtmlEmail(config);
	}

	/**
	 * Create a pre-configured {@link MailBuilder} from an explicit {@link MailConfig}.
	 *
	 * @throws MailException if the config is not valid or the email cannot be wired up
	 */
	public static MailBuilder createHtmlEmail(MailConfig config)
	throws MailException
	{
		if (config == null || !config.isConfigured())
			throw new MailException("MailConfig is not configured (smtpHostname is missing).");

		try
		{
			HtmlEmail email = new HtmlEmail();

			email.setHostName(config.smtpHostname);
			email.setCharset("UTF-8");

			if (config.smtpPort > 0)
				email.setSmtpPort(config.smtpPort);

			if (config.sslPort > 0)
				email.setSslSmtpPort(String.valueOf(config.sslPort));

			// Only set SSL/TLS flags when explicitly enabled — calling setSSLOnConnect(false)
			// or setStartTLSEnabled(false) can interfere with the JavaMail session setup.
			if (config.useSsl)
				email.setSSLOnConnect(true);

			if (config.startTls)
				email.setStartTLSEnabled(true);

			// setStartTLSRequired makes the connection fail if the server does not support
			// STARTTLS — useful for Exchange Online / Office 365 where TLS is mandatory.
			if (config.startTlsRequired)
				email.setStartTLSRequired(true);

			// connectionTimeout is in seconds (matching UserDefinedActionAbstract convention).
			if (config.connectionTimeout > 0)
				email.setSocketConnectionTimeout(Duration.ofSeconds(config.connectionTimeout));

			if (StringUtil.hasValue(config.username))
				email.setAuthentication(config.username, config.password);

			if (StringUtil.hasValue(config.from))
				applyFromAddress(email, config.from);

			_logger.info("MailUtil.createHtmlEmail(): configPrefix='{}', host='{}', port={}, sslPort={}, useSsl={}, startTls={}, startTlsRequired={}, connTimeoutSec={}, from='{}', hasAuth={}",
					config.resolvedPrefix,
					config.smtpHostname, config.smtpPort, config.sslPort,
					config.useSsl, config.startTls, config.startTlsRequired,
					config.connectionTimeout, config.from,
					StringUtil.hasValue(config.username));

			return new MailBuilder(email);
		}
		catch (EmailException e)
		{
			throw new MailException("Failed to configure HtmlEmail: " + e.getMessage(), e);
		}
	}


	// =========================================================================
	// Config helpers
	// =========================================================================

	/**
	 * Return the first {@link MailConfig} in {@link #AUTO_DISCOVER_PREFIXES}
	 * that has a non-empty {@code smtpHostname}, or {@code null} if none found.
	 */
	public static MailConfig autoDiscoverConfig()
	{
		for (String prefix : AUTO_DISCOVER_PREFIXES)
		{
			MailConfig cfg = getConfig(prefix);
			if (cfg.isConfigured())
			{
				_logger.info("MailUtil.autoDiscoverConfig(): Using SMTP config from prefix '{}'.", prefix);
				return cfg;
			}
		}
		_logger.warn("MailUtil.autoDiscoverConfig(): No SMTP configuration found in any of: {}.", AUTO_DISCOVER_PREFIXES);
		return null;
	}

	/**
	 * Read a {@link MailConfig} from the combined configuration using the given prefix.
	 * Never returns {@code null} — check {@link MailConfig#isConfigured()} on the result.
	 */
	public static MailConfig getConfig(String prefix)
	{
		return MailConfig.from(Configuration.getCombinedConfiguration(), prefix);
	}


	// =========================================================================
	// Internal helpers
	// =========================================================================

	/**
	 * Parse {@code "Display Name <email@host.com>"} or plain {@code "email@host.com"}
	 * and apply it as the From address on the given email.
	 */
	static void applyFromAddress(HtmlEmail email, String from)
	throws EmailException
	{
		String f  = from.trim();
		int    lt = f.lastIndexOf('<');
		int    gt = f.lastIndexOf('>');
		if (lt > 0 && gt > lt)
		{
			String displayName = f.substring(0, lt).trim();
			String address     = f.substring(lt + 1, gt).trim();
			email.setFrom(address, displayName);
		}
		else
		{
			email.setFrom(f);
		}
	}


	// =========================================================================
	// MailConfig
	// =========================================================================

	/**
	 * Simple POJO holding SMTP connection parameters.
	 *
	 * <p>Populate from configuration properties using {@link #from(Configuration, String)}.
	 * The standard property suffixes are exposed as {@code SUFFIX_*} constants so that
	 * existing sender classes can reference them without duplicating strings.
	 *
	 * <p>Standard property key for a given prefix is {@code prefix + SUFFIX_SMTP_HOST}, etc.
	 *
	 * <p>Per-property fallback: if a property is not set under the given prefix, the
	 * corresponding {@code mail.*} property (i.e. {@link MailUtil#FALLBACK_PREFIX}) is tried.
	 * This means that a single set of {@code mail.smtp.*} values in the global config acts as
	 * universal defaults, exactly as {@code UserDefinedActionAbstract} uses them.
	 */
	public static class MailConfig
	{
		/** {@code .smtp.hostname} — SMTP server hostname */
		public static final String SUFFIX_SMTP_HOST         = ".smtp.hostname";
		/** {@code .smtp.port} — SMTP port (-1 = use default) */
		public static final String SUFFIX_SMTP_PORT         = ".smtp.port";
		/** {@code .ssl.port} — SSL port (-1 = use default) */
		public static final String SUFFIX_SSL_PORT          = ".ssl.port";
		/** {@code .smtp.username} — SMTP authentication username */
		public static final String SUFFIX_USERNAME          = ".smtp.username";
		/** {@code .smtp.password} — SMTP authentication password */
		public static final String SUFFIX_PASSWORD          = ".smtp.password";
		/** {@code .from} — sender address, may include display name: {@code "Name <email>"} */
		public static final String SUFFIX_FROM              = ".from";
		/** {@code .ssl.use} — enable SSL-on-connect */
		public static final String SUFFIX_USE_SSL           = ".ssl.use";
		/** {@code .start.tls} — enable STARTTLS (opportunistic) */
		public static final String SUFFIX_START_TLS         = ".start.tls";
		/** {@code .start.tls.required} — require STARTTLS (fail if not available; useful for O365) */
		public static final String SUFFIX_START_TLS_REQ     = ".start.tls.required";
		/** {@code .smtp.connect.timeout} — socket connection timeout in <b>seconds</b> (-1 = default) */
		public static final String SUFFIX_CONN_TIMEOUT      = ".smtp.connect.timeout";

		/** The prefix that was used when this config was built (informational, may be null). */
		public String  resolvedPrefix    = null;

		public String  smtpHostname      = "";
		public int     smtpPort          = -1;
		public int     sslPort           = -1;
		public String  username          = "";
		public String  password          = "";
		public String  from              = "";
		public boolean useSsl            = false;
		public boolean startTls          = false;
		public boolean startTlsRequired  = false;
		public int     connectionTimeout = -1;

		/** @return {@code true} if at minimum {@code smtpHostname} is set */
		public boolean isConfigured()
		{
			return StringUtil.hasValue(smtpHostname);
		}

		/**
		 * Build a {@code MailConfig} by reading {@code prefix + SUFFIX_*} keys
		 * from the given {@link Configuration}.
		 * <p>
		 * For any property that is empty or at its default under {@code prefix},
		 * the corresponding {@code mail.*} property is tried as a fallback
		 * (unless {@code prefix} is already {@code "mail"}).
		 * Never returns {@code null}.
		 */
		public static MailConfig from(Configuration conf, String prefix)
		{
			// Whether to also consult the flat "mail.*" fallback namespace.
			boolean useFallback = !FALLBACK_PREFIX.equals(prefix);

			MailConfig c          = new MailConfig();
			c.resolvedPrefix      = prefix;

			c.smtpHostname = str (conf, prefix, SUFFIX_SMTP_HOST,     "",    useFallback);
			c.smtpPort     = iget(conf, prefix, SUFFIX_SMTP_PORT,     -1,    useFallback);
			c.sslPort      = iget(conf, prefix, SUFFIX_SSL_PORT,      -1,    useFallback);
			c.username     = str (conf, prefix, SUFFIX_USERNAME,      "",    useFallback);
			c.password     = str (conf, prefix, SUFFIX_PASSWORD,      "",    useFallback);
			c.from         = str (conf, prefix, SUFFIX_FROM,          "",    useFallback);
			c.useSsl       = bget(conf, prefix, SUFFIX_USE_SSL,       false, useFallback);
			c.startTls     = bget(conf, prefix, SUFFIX_START_TLS,     false, useFallback);
			c.startTlsRequired = bget(conf, prefix, SUFFIX_START_TLS_REQ, false, useFallback);
			c.connectionTimeout = iget(conf, prefix, SUFFIX_CONN_TIMEOUT, -1, useFallback);
			return c;
		}

		// -- private helpers --------------------------------------------------

		private static String str(Configuration conf, String prefix, String suffix, String dflt, boolean fallback)
		{
			String v = conf.getProperty(prefix + suffix, dflt);
			if (fallback && !StringUtil.hasValue(v))
				v = conf.getProperty(FALLBACK_PREFIX + suffix, dflt);
			return v;
		}

		private static int iget(Configuration conf, String prefix, String suffix, int dflt, boolean fallback)
		{
			int v = conf.getIntProperty(prefix + suffix, dflt);
			if (fallback && v == dflt)
				v = conf.getIntProperty(FALLBACK_PREFIX + suffix, dflt);
			return v;
		}

		private static boolean bget(Configuration conf, String prefix, String suffix, boolean dflt, boolean fallback)
		{
			boolean v = conf.getBooleanProperty(prefix + suffix, dflt);
			if (fallback && v == dflt)
				v = conf.getBooleanProperty(FALLBACK_PREFIX + suffix, dflt);
			return v;
		}
	}


	// =========================================================================
	// MailBuilder — fluent wrapper around HtmlEmail
	// =========================================================================

	/**
	 * Fluent wrapper around a pre-configured {@link HtmlEmail}.
	 * All mutating methods return {@code this} so calls can be chained.
	 * Every method that touches the underlying email throws {@link MailException} on failure.
	 *
	 * <pre>
	 *   MailUtil.createHtmlEmail()
	 *       .setTo("user@acme.com")
	 *       .setCc("manager@acme.com")
	 *       .setSubject("Report ready")
	 *       .setMessage("&lt;b&gt;Your report is ready.&lt;/b&gt;")
	 *       .send();
	 * </pre>
	 */
	public static class MailBuilder
	{
		private final HtmlEmail _email;

		MailBuilder(HtmlEmail email)
		{
			_email = email;
		}

		/** Return the underlying {@link HtmlEmail} for advanced/low-level use. */
		public HtmlEmail getEmail()
		{
			return _email;
		}


		// -- recipients -------------------------------------------------------

		/** Add a To recipient. May be called multiple times. */
		public MailBuilder setTo(String address)
		throws MailException
		{
			try   { _email.addTo(address); return this; }
			catch (EmailException e) { throw new MailException("Invalid To address '" + address + "': " + e.getMessage(), e); }
		}

		/** Add multiple To recipients in one call. */
		public MailBuilder setTo(String... addresses)
		throws MailException
		{
			for (String a : addresses)
				setTo(a);
			return this;
		}

		/** Alias for {@link #setTo(String)}. */
		public MailBuilder addTo(String address)
		throws MailException
		{
			return setTo(address);
		}

		/** Add a Cc recipient. May be called multiple times. */
		public MailBuilder setCc(String address)
		throws MailException
		{
			try   { _email.addCc(address); return this; }
			catch (EmailException e) { throw new MailException("Invalid Cc address '" + address + "': " + e.getMessage(), e); }
		}

		/** Alias for {@link #setCc(String)}. */
		public MailBuilder addCc(String address)
		throws MailException
		{
			return setCc(address);
		}

		/** Add a Bcc recipient. May be called multiple times. */
		public MailBuilder setBcc(String address)
		throws MailException
		{
			try   { _email.addBcc(address); return this; }
			catch (EmailException e) { throw new MailException("Invalid Bcc address '" + address + "': " + e.getMessage(), e); }
		}


		// -- from (override the config-level from) ----------------------------

		/**
		 * Override the From address set by the config.
		 * Accepts {@code "Display Name <email@host.com>"} or plain {@code "email@host.com"}.
		 */
		public MailBuilder setFrom(String address)
		throws MailException
		{
			try   { applyFromAddress(_email, address); return this; }
			catch (EmailException e) { throw new MailException("Invalid From address '" + address + "': " + e.getMessage(), e); }
		}


		// -- subject ----------------------------------------------------------

		public MailBuilder setSubject(String subject)
		{
			_email.setSubject(subject);
			return this;
		}


		// -- body -------------------------------------------------------------

		/**
		 * Set the HTML message body. A plain-text fallback is set automatically
		 * ("Your email client does not support HTML.") unless
		 * {@link #setTextMessage(String)} is called afterwards to override it.
		 */
		public MailBuilder setMessage(String htmlMsg)
		throws MailException
		{
			try
			{
				_email.setHtmlMsg(htmlMsg);
				_email.setTextMsg("Your email client does not support HTML messages.");
				return this;
			}
			catch (EmailException e)
			{
				throw new MailException("Failed to set HTML message: " + e.getMessage(), e);
			}
		}

		/** Set an explicit plain-text body (overrides the auto-fallback from {@link #setMessage}). */
		public MailBuilder setTextMessage(String textMsg)
		throws MailException
		{
			try   { _email.setTextMsg(textMsg); return this; }
			catch (EmailException e) { throw new MailException("Failed to set text message: " + e.getMessage(), e); }
		}

		/** Set an explicit HTML body without touching the text part. */
		public MailBuilder setHtmlMessage(String htmlMsg)
		throws MailException
		{
			try   { _email.setHtmlMsg(htmlMsg); return this; }
			catch (EmailException e) { throw new MailException("Failed to set HTML message: " + e.getMessage(), e); }
		}


		// -- send -------------------------------------------------------------

		/**
		 * Send the email.
		 *
		 * @return the message-ID assigned by the SMTP server
		 * @throws MailException if the email cannot be sent for any reason
		 */
		public String send()
		throws MailException
		{
			try
			{
				return _email.send();
			}
			catch (EmailException e)
			{
				throw new MailException("Failed to send email: " + e.getMessage(), e);
			}
		}
	}
}
