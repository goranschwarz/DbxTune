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
package com.dbxtune.alarm.events.oracle;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventOraAccountProblem
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * The password of an account that can be used (it is not locked) expires soon, or has already expired.<br>
	 * When it has expired the application that uses the account can't log in until the password is changed.
	 *
	 * @param cm
	 * @param account           Name of the account (in a CDB: 'containerName:username')
	 * @param accountStatus     ACCOUNT_STATUS, for example: OPEN, EXPIRED(GRACE), EXPIRED
	 * @param expiryDate        When the password expires
	 * @param expiresInDays     Days until it expires (negative: it has already expired)
	 * @param profile           The profile of the account (where the life time of a password is set)
	 * @param thresholdInDays
	 */
	public AlarmEventOraAccountProblem(CountersModel cm, String account, String accountStatus, String expiryDate, double expiresInDays, String profile, int thresholdInDays)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"PasswordExpires:" + account, // extraInfo
				AlarmEvent.Category.OTHER,
				expiresInDays <= 0 ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				(expiresInDays <= 0
					? "The password of account '" + account + "' HAS EXPIRED (" + (-expiresInDays) + " days ago)"
					: "The password of account '" + account + "' expires in " + expiresInDays + " days")
				+ " in Server '" + cm.getServerName() + "'. ExpiryDate='" + expiryDate + "', AccountStatus='" + accountStatus + "', Profile='" + profile + "'. Change the password (or the PASSWORD_LIFE_TIME of the profile) before the application is locked out. (thresholdInDays=" + thresholdInDays + ")",
				thresholdInDays
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("account=" + account + ",accountStatus=" + accountStatus + ",expiresInDays=" + expiresInDays);
	}

	/**
	 * An account is locked because of too many failed logins (ACCOUNT_STATUS is LOCKED(TIMED)).<br>
	 * That is someone guessing passwords, or an application that still uses an old password.
	 *
	 * @param cm
	 * @param account           Name of the account (in a CDB: 'containerName:username')
	 * @param accountStatus     ACCOUNT_STATUS, for example: LOCKED(TIMED), EXPIRED(GRACE) & LOCKED(TIMED)
	 * @param lockDate          When it was locked
	 * @param profile           The profile of the account (where FAILED_LOGIN_ATTEMPTS and PASSWORD_LOCK_TIME is set)
	 */
	public AlarmEventOraAccountProblem(CountersModel cm, String account, String accountStatus, String lockDate, String profile)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"LockedByFailedLogins:" + account, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Account '" + account + "' is LOCKED after too many failed logins in Server '" + cm.getServerName() + "'. LockDate='" + lockDate + "', AccountStatus='" + accountStatus + "', Profile='" + profile + "'. Someone is guessing the password, or an application still uses an old password.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("account=" + account + ",accountStatus=" + accountStatus + ",lockDate=" + lockDate);
	}
}
