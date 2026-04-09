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
package com.dbxtune.central.utils;

import java.lang.invoke.MethodHandles;
import java.security.SecureRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.eclipse.jetty.util.security.Credential;

/**
 * Password hashing utilities using BCrypt (Bouncy Castle {@code OpenBSDBCrypt}).
 *
 * <p>BCrypt is adaptive (the cost factor can be raised as hardware improves),
 * and the salt is embedded in the output string, so no separate salt column is needed.
 * The stored format is a standard 60-character BCrypt string, e.g.:
 * <pre>$2b$12$&lt;22-char-salt&gt;&lt;31-char-hash&gt;</pre>
 *
 * <p>Legacy {@code MD5:} hashes (Jetty built-in) are still accepted by
 * {@link #toCredential(String)} so existing stored passwords continue to work.
 */
public class PasswordHashUtil
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * BCrypt cost factor (log2 of the number of rounds).
	 * 12 ≈ 300–400 ms per hash on modern hardware, which is a good balance
	 * between security and usability. Raise to 13 or 14 in the future as needed.
	 */
	private static final int    BCRYPT_COST    = 12;
	private static final String BCRYPT_VERSION = "2b"; // current standard; fixes 8-bit char bug vs "2a"

	/**
	 * Hash a plaintext password using BCrypt.
	 * Returns a self-contained 60-character string like {@code $2b$12$...}
	 * that can be stored directly in the database.
	 */
	public static String hashPassword(String plaintext)
	{
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return OpenBSDBCrypt.generate(BCRYPT_VERSION, plaintext.toCharArray(), salt, BCRYPT_COST);
	}

	/**
	 * Verify a plaintext password against a stored hash.
	 * Handles both BCrypt ({@code $2x$...}) and legacy Jetty hashes ({@code MD5:...}).
	 *
	 * @return {@code true} if the plaintext matches the stored hash
	 */
	public static boolean verifyPassword(String plaintext, String storedHash)
	{
		if (plaintext == null || storedHash == null) 
			return false;

		return toCredential(storedHash).check(plaintext);
	}

	/**
	 * Returns {@code true} if the value is already in hashed form
	 * (BCrypt {@code $2x$...} or legacy {@code MD5:...}).
	 */
	public static boolean isAlreadyHashed(String password)
	{
		if (password == null) 
			return false;

		return password.startsWith("$2") || password.startsWith("MD5:");
	}

	/**
	 * Wrap the stored password string in the appropriate Jetty {@link Credential}
	 * so that {@code DbxCentralRealm} can authenticate against it.
	 * <ul>
	 *   <li>{@code $2x$...} — BCrypt: verified with {@link OpenBSDBCrypt#checkPassword}</li>
	 *   <li>{@code MD5:...} / {@code OBF:...} / plain text — delegated to Jetty's built-in
	 *       {@link Credential#getCredential(String)} for backward compatibility
	 *       (e.g. admin's IP-address default password).</li>
	 * </ul>
	 */
	public static Credential toCredential(String stored)
	{
		if (stored != null && stored.startsWith("$2"))
			return new BcryptCredential(stored);

		// Fallback: Jetty handles MD5:, OBF:, CRYPT:, and plain text
		return Credential.getCredential(stored == null ? "" : stored);
	}


	// -------------------------------------------------------------------------
	// Inner class: BCrypt-aware Jetty Credential
	// -------------------------------------------------------------------------

	private static final class BcryptCredential extends Credential
	{
		private static final long serialVersionUID = 1L;

		private final String _bcryptHash;

		BcryptCredential(String bcryptHash)
		{
			_bcryptHash = bcryptHash;
		}

		@Override
		public boolean check(Object credentials)
		{
			if (credentials == null) return false;
			String submitted = (credentials instanceof char[])
					? new String((char[]) credentials)
					: credentials.toString();
			try
			{
				return OpenBSDBCrypt.checkPassword(_bcryptHash, submitted.toCharArray());
			}
			catch (Exception e)
			{
				_logger.warn("BCrypt password check failed", e);
				return false;
			}
		}
	}
}
