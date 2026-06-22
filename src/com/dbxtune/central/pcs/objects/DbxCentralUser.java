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
package com.dbxtune.central.pcs.objects;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.utils.StringUtil;

public class DbxCentralUser
{
	/**
	 * Bitmask flags for the {@code Status} column in {@code DbxCentralUsers}.
	 * Multiple flags may be combined with bitwise OR, e.g. {@code ACTIVE | PW_CHANGE_REQUIRED = 33}.
	 */
	public enum UserStatus
	{
		ACTIVE              (1),
		PENDING_APPROVAL    (2),
		LOCKED              (4),
		REJECTED            (8),
		EMAIL_VERIFIED      (16),
		PW_CHANGE_REQUIRED  (32);

		private final int _bit;
		UserStatus(int bit) { _bit = bit; }

		public int getBit() { return _bit; }

		public static boolean isSet(int status, UserStatus flag) { return (status & flag._bit) != 0; }
		public static int     set  (int status, UserStatus flag) { return  status | flag._bit; }
		public static int     clear(int status, UserStatus flag) { return  status & ~flag._bit; }

		/** Returns true if the status int represents a usable (non-locked, non-pending) account. */
		public static boolean isLoginAllowed(int status)
		{
			return isSet(status, ACTIVE)
				&& !isSet(status, LOCKED)
				&& !isSet(status, PENDING_APPROVAL)
				&& !isSet(status, REJECTED);
		}

		/** Human-readable label for the primary state. */
		public static String toLabel(int status)
		{
			if (isSet(status, REJECTED))         return "Rejected";
			if (isSet(status, LOCKED))           return "Locked";
			if (isSet(status, PENDING_APPROVAL)) return "Pending";
			if (isSet(status, ACTIVE))           return "Active";
			return "Unknown";
		}

		public static UserStatus fromBit(int bit)
		{
			for (UserStatus s : values())
			{
				if (s._bit == bit) 
				{
					return s;
				}
			}
			throw new IllegalArgumentException("Unknown UserStatus bit: " + bit);
		}
	}

	// ---- core identity ----
	private String       _username  = "";
	private String       _password  = "";
	private List<String> _roles     = new ArrayList<>();
	private String       _email     = "";

	// ---- extended fields (DB version 17+) ----
	private int       _status         = UserStatus.ACTIVE.getBit();
	private Timestamp _addDate        = null;
	private Timestamp _updateDate     = null;
	private Timestamp _lastLoginDate  = null;
	private String    _source         = null;
	private String    _fullName       = null;
	private String    _requestReason  = null;
	private String    _approvedBy     = null;
	private Timestamp _approveDate    = null;

	// ---- extended fields (DB version 18+) ----
	private int       _loginFailCount = 0;

	public DbxCentralUser()
	{
	}

	public DbxCentralUser(String username, String password, String email, String roleCsv)
	{
		_username = username;
		_password = password;
		_email    = email;
		_roles    = StringUtil.commaStrToList(roleCsv);
	}

	// ---- getters / setters ----

	public String getUsername()      { return _username; }
	public String getPassword()      { return _password; }
	public String getEmail()         { return _email; }
	public String[] getRoles()       { return _roles.toArray(new String[_roles.size()]); }

	public int       getStatus()        { return _status; }
	public Timestamp getAddDate()       { return _addDate; }
	public Timestamp getUpdateDate()    { return _updateDate; }
	public Timestamp getLastLoginDate() { return _lastLoginDate; }
	public String    getSource()        { return _source; }
	public String    getFullName()      { return _fullName; }
	public String    getRequestReason() { return _requestReason; }
	public String    getApprovedBy()    { return _approvedBy; }
	public Timestamp getApproveDate()    { return _approveDate; }
	public int       getLoginFailCount() { return _loginFailCount; }

	public void setStatus        (int status)           { _status         = status; }
	public void setAddDate       (Timestamp ts)         { _addDate        = ts; }
	public void setUpdateDate    (Timestamp ts)         { _updateDate     = ts; }
	public void setLastLoginDate (Timestamp ts)         { _lastLoginDate  = ts; }
	public void setSource        (String source)        { _source         = source; }
	public void setFullName      (String fullName)      { _fullName       = fullName; }
	public void setRequestReason (String reason)        { _requestReason  = reason; }
	public void setApprovedBy    (String approvedBy)    { _approvedBy     = approvedBy; }
	public void setApproveDate   (Timestamp approveDate){ _approveDate    = approveDate; }
	public void setLoginFailCount(int count)            { _loginFailCount = count; }

	public boolean isLoginAllowed() { return UserStatus.isLoginAllowed(_status); }
}
