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
package com.asetune.ssh;

//--------------------------------------------------------------------
// origin source code: 
// https://github.com/raodj/peace/blob/4af338d3093586a8dd19062b865a84d25b0a2d4f/gui/src/org/peace_tools/core/session/RemoteFileSystemView.java
//--------------------------------------------------------------------

//--------------------------------------------------------------------
//
//This file is part of PEACE.
//
//PEACE is free software: you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//PEACE is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with PEACE.  If not, see <http://www.gnu.org/licenses/>.
//
//Miami University makes no representations or warranties about the
//suitability of the software, either express or implied, including
//but not limited to the implied warranties of merchantability,
//fitness for a particular purpose, or non-infringement.  Miami
//University shall not be liable for any damages suffered by licensee
//as a result of using, result of using, modifying or distributing
//this software or its derivatives.
//
//By using or copying this Software, Licensee agrees to abide by the
//intellectual property laws, and all other applicable laws of the
//U.S., and the terms of GNU General Public License (version 3).
//
//Authors:   Dhananjai M. Rao          raodm@muohio.edu
//
//---------------------------------------------------------------------

//package org.peace_tools.core.session;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
* A custom extension of the File class to provide an 
* abstract representation of file and directory pathnames on
* a remote server.
* 
* <p>This class extends the standard File class to provide Unix/Linux
* file-system specific pathname strings to name files and directories.
* The functionality provided by this class is pretty identical to that
* of the parent File class. The primary difference is that the actual
* file-system that this class operates on is not local but remote --
* that is the file-system is physically on a different
* server/machine.</p>
* 
* <p>Access to a remote server's file-system is achieved via SFTP 
* (Secure File Transfer Protocol) channels established over SSH
* (Secure Shell) connections. The SSH/SFTP functionality is achieved
* using the JSch library. JSch provides an pure Java-based implementation
* of the SSH protocols. JSch SSH supports the recent SSH-2 protocol. 
* Please refer to JSch SSH web site for licensing and other details: 
* {@link http://www.jcraft.com/jsch/}.</p>
*
*/
public class RemoteFile extends File {
	/**
	 * A generated serialization UID (primarily to keep the Java
	 * compiler happy).
	 */
	private static final long serialVersionUID = 7623160956116046503L;

	/**
	 * The JSch session that is to be used by this object for accessing
	 * the remote file system. This class creates SFTP connections
	 * (on demand) to obtain additional information such as permissions,
	 * file size information, and absolute path etc.
	 */
	private final Session session;
	
	/**
	 * The file attributes associated with the remote file. The
	 * data type is provided by JSch. It is encapsulated in this class
	 * to enable convenient implementation of various methods. Note that
	 * in certain cases the attributes may have to be obtained on demand
	 * via an SFTP connection to the server. So always call the
	 * {@link #updateAttributes()} method prior to using this member.
	 * See {@link #canExecute()} method for example on how to use it.
	 */
	private SftpATTRS remoteAttrs;
	
	/**
	 * The full/canonical path to the file on the remote server.
	 * This value is filled-in on demand (to provide good performance when
	 * browsing file systems) and consequently will not
	 * be readily available for all objects. See the {@link #getAbsolutePath()}
	 * method for details on how this value is computed.
	 */
	private String fullPath;

	/**
	 * Constant to define the bit-mask to access the user-read
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This value is 
	 * specifically used in conjunction with the file permissions stored
	 * in the {@link #remoteAttrs} instance variable (see 
	 * {@link SftpATTRS#getPermissions()} method) to determine if
	 * the file owner can read a given file or directory. 
	 */
	public static final int S_IRUSR = 00400; // read by owner
	
	/**
	 * Constant to define the bit-mask to access the user-write
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used  in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance 
	 * variable (see {@link SftpATTRS#getPermissions()} method) to
	 * determine if the file owner can write to given file or directory. 
	 */
	public static final int S_IWUSR = 00200; // write by owner
	
	/**
	 * Constant to define the bit-mask to access the user-execute
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used  in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance 
	 * variable (see {@link SftpATTRS#getPermissions()} method) to
	 * determine if the file owner can search a given directory or
	 * execute a given file. 
	 */
	public static final int S_IXUSR = 00100; // execute/search by owner

	/**
	 * Constant to define the bit-mask to access the group-read
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine the group read privileges for a given file or directory. 
	 */
	public static final int S_IRGRP = 00040; // read by group
	
	/**
	 * Constant to define the bit-mask to access the group-write
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine the group write privileges for a given file or directory. 
	 */
	public static final int S_IWGRP = 00020; // write by group
	
	/**
	 * Constant to define the bit-mask to access the group-execute
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine the group execute privileges for a given file or 
	 * group search privileges for a given directory. 
	 */
	public static final int S_IXGRP = 00010; // execute/search by group

	/**
	 * Constant to define the bit-mask to access the others-read
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine read privileges for a general user (not a owner or 
	 * a group member) on a given file or directory. 
	 */
	public static final int S_IROTH = 00004; // read by others
	
	/**
	 * Constant to define the bit-mask to access the others-write
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine write privileges for a general user (not a owner or 
	 * a group member) on a given file or directory. 
	 */
	public static final int S_IWOTH = 00002; // write by others
	
	/**
	 * Constant to define the bit-mask to access the others-execute
	 * permission bit. This value is obtained from the POSIX standards
	 * (see {@code chmod} man page) used by Unix/Linux machines. This 
	 * value is specifically used in conjunction with the file 
	 * permissions stored in the {@link #remoteAttrs} instance
	 * variable (see {@link SftpATTRS#getPermissions()} method) to 
	 * determine execute privileges for a general user (not a owner or 
	 * a group member) on a given file or directory. 
	 */
	public static final int S_IXOTH = 00001; // execute/search by others

	/**
	 * Helper method to obtain a SFTP channel.
	 * 
	 * This is a refactored utility method that is used by various
	 * methods in this class to create a temporary SFTP channel
	 * using the JSch {@link #session}. 
	 * 
	 * @return On successfully establishing a JSch SFTP channel, this
	 * method returns the JSch SFTP channel for further use.
	 * 
	 * @throws SftpException If an exception occurs when attempting
	 * to establish a connection, this method throws a suitable
	 * exception.
	 * 
	 * @see #closeSftp(ChannelSftp)
	 */
	private ChannelSftp getLocalSftp() throws SftpException {
		synchronized (session) {
			try {
				ChannelSftp localSftp = (ChannelSftp) session
						.openChannel("sftp");
				localSftp.connect(60000);
				return localSftp;
			} catch (JSchException exp) {
				throw new SftpException(-1, exp.getMessage(), exp.getCause());
			}
		}
	}

	/**
	 * Helper method to close an SFTP channel.
	 * 
	 * This is a convenience method that is used by various methods in
	 * this class (specifically methods that use {@link #getLocalSftp()}
	 * method) to close an open SFTP connection. 
	 * 
	 * @param sftp The connection to be closed. This parameter can be 
	 * null and if it is null, this method performs no action.
	 */
	private void closeSftp(ChannelSftp sftp) {
		if (sftp != null) {
			sftp.disconnect();
		}
	}

	/**
	 * Helper method to obtain the file attributes (permissions, time 
	 * stamps, and size) for this object if it is not already available.
	 * 
	 * This is a utility method that is frequently invoked to
	 * check and update the file attributes. File attributes are
	 * loaded from the remote server only if the {@link #remoteAttrs}
	 * object is null. The attributes are obtained via a temporary
	 * SFTP connection.  
	 *  
	 * @throws SftpException Exception is raised when an error
	 * occurs during fetching of attributes.
	 */
	private void updateAttributes() throws SftpException {
		if (remoteAttrs != null) {
			// We already have attributes. Nothing to do.
			return;
		}
		// We don't have the file attributes. Fetch it
		// from the remote server.
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			if (fullPath == null) {
				fullPath = sftp.realpath(getPath());
			}
			remoteAttrs = sftp.stat(fullPath);
		} finally {
			closeSftp(sftp);
		}
	}

	/**
	 * Helper method to change file permissions.
	 * 
	 * This method is a helper method that is invoked from other
	 * methods in this class (for example: {@link #setExecutable(boolean)}).
	 * This method changes the permissions of the file on the
	 * remote server via a temporary SFTP connection.
	 * 
	 * @param setOrClear A boolean flag to indicate if the permissions
	 * are to be set (when flag is true) or removed (when flag is false).
	 * 
	 * @param deltaPermFlags A integer that contains the POSIX
	 * permission bits to be set or cleared.
	 * 
	 * @return This method returns true if the permissions were
	 * successfully changed. If the permissions could not be changed
	 * this method returns false.
	 */
	private boolean changePermissions(final boolean setOrClear,
			final int deltaPermFlags) {
		boolean result   = false;
		ChannelSftp sftp = null;
		try {
			// Create local SFTP. This must be closed.
			sftp = getLocalSftp();
			// Ensure we have the full canonical path 
			if (fullPath == null) {
				fullPath = sftp.realpath(getPath());
			}
			// Get attributes if we don't already have it.
			if (remoteAttrs == null) {
				remoteAttrs = sftp.stat(fullPath);
			}
			// Change the permissions.
			int permissions = remoteAttrs.getPermissions();
			if (setOrClear) {
				permissions |= deltaPermFlags;
			} else {
				permissions &= ~deltaPermFlags;
			}
			// Set permissions on local copy
			remoteAttrs.setPERMISSIONS(permissions);
			// Actually update permissions on remote file.
			sftp.setStat(fullPath, remoteAttrs);
			result = true;
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Helper method to determine if a given path is already in its
	 * canonical form.
	 * 
	 * This class is currently designed to work only with remote
	 * Unix/Linux machines whose file-systems are rooted at "/".
	 * Consequently any canonical path (the full path to a given file)
	 * will start with a "/" and will not include any relative path
	 * tags (such as: {@code /.} or {@code /..}) in it.
	 * 
	 * @param path The path to a file/directory to be checked for
	 * canonical form.
	 * 
	 * @return This method returns true if the path is in its 
	 * canonical form. Otherwise it returns false.
	 */
	private boolean isCanonical(final String path) {
		return (path.startsWith("/")) &&
			   (path.indexOf("/.") == -1);
	}
	
	/**
	 * A simple constructor.
	 * 
	 * @param path The path to the file. This path can be a relative
	 * or canonical (absolute) path. 
	 * 
	 * @param session The JSch session to be used to obtain additional
	 * information about the file on demand. This parameter cannot be
	 * null.
	 */
	public RemoteFile(String path, Session session) {
		super(path);
		this.session  = session;
		this.fullPath = (isCanonical(path) ? path : null);
		remoteAttrs   = null;
	}

	/**
	 * The copy constructor.
	 * 
	 * The copy constructor copies the JSch session, the 
	 * canonical path, and all file attributes from the 
	 * src object into this.
	 * 
	 * @param src The source RemoteFile object from where the
	 * various information is to be copied. 
	 */
	public RemoteFile(RemoteFile src) {
		super(src.getPath());
		this.session     = src.session;
		this.fullPath    = src.fullPath;
		this.remoteAttrs = src.remoteAttrs;
	}

	/**
	 * Constructor to set some values.
	 * 
	 * @param path The path to the file. This path can be a relative
	 * or canonical (absolute) path. 
	 * 
	 * @param attrs The remote attributes for the file. This object
	 * can be null.
	 * 
	 * @param session The JSch session to be used to obtain additional
	 * information about the file on demand. This parameter cannot be 
	 * null.
	 */
	public RemoteFile(String path, SftpATTRS attrs, Session session) {
		super(path);
		this.session  = session;
		this.fullPath = (isCanonical(path) ? path : null);
		remoteAttrs   = attrs;
	}

	/**
	 * Constructor to initialize all values.
	 * 
	 * @param path The path to the file. This path can be a relative
	 * or canonical (absolute) path. 
	 * 
	 * @param fullPath The canonical path to the file. This path
	 * must be in its canonical form (but no special checks are
	 * made to enforce it). 
	 * 
	 * @param attrs The remote attributes for the file. This object
	 * can be null. If it is null, it will be fetched on demand.
	 * 
	 * @param session The JSch session to be used to obtain additional
	 * information about the file on demand. This parameter cannot be 
	 * null.
	 */
	public RemoteFile(String path, String fullPath, SftpATTRS attrs,
			Session session) {
		super(path);
		assert(isCanonical(fullPath));
		this.session  = session;
		this.fullPath = fullPath;
		remoteAttrs   = attrs;
	}

	/**
	 * Obtain the attributes (file size, time stamps,
	 * POSIX permissions) for this file entry.
	 * 
	 * This method updates the file attributes (if it is not
	 * already available) by calling the {@link #updateAttributes()}
	 * method.
	 * 
	 * @return The file attributes for this file. If the attributes
	 * could not be determined this method throws an SecurityException.
	 */
	public SftpATTRS getAttributes() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return remoteAttrs;

	}

	/**
	 * Determine the user-execute permissions on this file.
	 *  
	 * This method updates the file attributes (if it is not
	 * already available) by calling the {@link #updateAttributes()}
	 * method.
	 * 
	 * @return This method returns true if the user-execute
	 * bit is set, indicating this file is executable by its owner.
	 * Otherwise this method returns false.
	 */
	@Override
	public boolean canExecute() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return ((remoteAttrs.getPermissions() & S_IXUSR) != 0);
	}

	/**
	 * Determine the user-read permissions on this file.
	 *  
	 * This method updates the file attributes (if it is not
	 * already available) by calling the {@link #updateAttributes()}
	 * method.
	 * 
	 * @return This method returns true if the user-read
	 * bit is set, indicating this file is readable by its owner.
	 * Otherwise this method returns false.
	 */
	@Override
	public boolean canRead() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return ((remoteAttrs.getPermissions() & S_IRUSR) != 0);
	}

	/**
	 * Determine the user-write permissions on this file.
	 *  
	 * This method updates the file attributes (if it is not
	 * already available) by calling the {@link #updateAttributes()}
	 * method.
	 * 
	 * @return This method returns true if the user-write
	 * bit is set, indicating this file is writable by its owner.
	 * Otherwise this method returns false.
	 */
	@Override
	public boolean canWrite() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return ((remoteAttrs.getPermissions() & S_IWUSR) != 0);
	}

	/**
	 * Atomically creates a new, empty file named by this abstract 
	 * pathname if and only if a file with this name does not yet exist.
	 * 
	 * Currently, there is no way to ensure that file creation
	 * on a remote machine proceeds in an atomic fashion. Consequently,
	 * this method does not perform any operation and simply 
	 * returns false.
	 * 
	 * @return This method returns true if the file creation 
	 * was successful. Otherwise this method returns false.
	 */
	@Override
	public boolean createNewFile() throws IOException {
		return false;
	}

	/**
	 * Deletes the physical file/directory represented by this object.
	 * 
	 * Deletes the file or directory denoted by this abstract pathname. 
	 * If this pathname denotes a directory, then the directory must be
	 * empty in order to be deleted.
	 *   
	 * @return This method returns true if the file or directory was
	 * successfully deleted. Otherwise it returns false.
	 */
	@Override
	public boolean delete() {
		boolean result = false;
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			// First ensure we have full canonical path to file.
			if (fullPath == null) {
				fullPath = sftp.realpath(getPath());
			}
			// Get file attributes
			if (remoteAttrs == null) {
				remoteAttrs = sftp.stat(fullPath);
			}
			// Perform slightly different operations depending on
			// whether the entry is a file or a directory.
			if (remoteAttrs.isDir()) {
				sftp.rmdir(getAbsolutePath());
			} else {
				sftp.rm(getAbsolutePath());
			}
			result = true;
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Check to see if another RemoteFile object refers to the same file.
	 * 
	 * This provides a simple implementation of the standard .equals()
	 * method.
	 * 
	 * @param obj The other object to be compared with.
	 * 
	 * @return This method returns true only if obj is a RemoteFile object
	 * and its session (check to ensure they are referencing the same
	 * file-system/server) and canonical path is same as this.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RemoteFile) {
			RemoteFile other = (RemoteFile) obj;
			return ((this == other) || 
					((this.session == other.session) && 
					(getCanonicalPath().equals(other.getCanonicalPath()))));
		}
		return false;
	}

	/**
	 * Tests whether the file or directory denoted by this 
	 * abstract pathname exists.
	 * 
	 * This method overrides the default implementation to check
	 * existence on the remote server's file system. This method
	 * uses the availability of {@link #remoteAttrs} as the key 
	 * to decide on existence of the file. If the {@link #remoteAttrs}
	 * is null then this method obtains the file attributes 
	 * for performing this check.
	 * 
	 * @return This method returns true if the file system
	 * contains a file denoted by this abstract path name. Otherwise
	 * it returns false.
	 */
	@Override
	public boolean exists() {
		if (remoteAttrs != null) {
			return true;
		}
		boolean result = false;
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			// Ensure we have a canonical path to work with
			if (fullPath == null) {
				fullPath = sftp.realpath(getPath());
			}
			// Obtain remote attributes.
			if ((remoteAttrs = sftp.stat(getAbsolutePath())) != null) {
				result = true;
			}
		} catch (SftpException e) {
			// e.printStackTrace();
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Returns the absolute form of this abstract pathname. 
	 * 
	 * The object returned by this method is similar to:
	 * {@code new RemoteFile(getAbsolutePath(), session)}.
	 * On Unix/Linux file-systems there isn't much difference
	 * between the absolute and canonical path. Consequently,
	 * the return value of this method is identical to
	 * that of the {@link #getCanonicalFile()} method.
	 * 
	 * @return The absolute abstract pathname denoting the same 
	 * file or directory as this abstract pathname 
	 */
	@Override
	public File getAbsoluteFile() {
		final String absPath = getAbsolutePath();
		return new RemoteFile(absPath, absPath,	remoteAttrs, session);
	}

	/** Returns the absolute pathname string of this abstract pathname.
	 * 
	 * <p>This method overrides the default implementation to return
	 * the absolute pathname string. On Unix/Linux machines there
	 * is not much difference between the absolute and canonical
	 * path names. Consequently, this method has an identical 
	 * return value as the {@link #getCanonicalPath()} method.</p>
	 * 
	 * <p>The absolute path to the file is essentially the full
	 * path returned by the {@link ChannelSftp#realpath(String)}
	 * method. The real path is determined only if the {@link #fullPath}
	 * instance variable is null.</p>
	 * 
	 * @return The absolute pathname string denoting the same file or 
	 * directory as this abstract pathname 
	 */
	@Override
	public String getAbsolutePath() {
		if (fullPath != null) {
			return fullPath;
		}
		// Make relative file name to absolute file name.
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			fullPath = sftp.realpath(getPath());
		} catch (SftpException e) {
			e.printStackTrace();
		} finally {
			closeSftp(sftp);
		}
		return fullPath;
	}

	/**
	 * Returns the canonical form of this abstract pathname. 
	 * 
	 * The object returned by this method is similar to:
	 * {@code new RemoteFile(getCanonicalPath(), session)}.
	 * On Unix/Linux file-systems there isn't much difference
	 * between the absolute and canonical path. Consequently,
	 * the return value of this method is identical to
	 * that of the {@link #getAbsoluteFile()} method.
	 * 
	 * @return The canonical abstract remote file object
	 * denoting the same file or directory as this 
	 * abstract pathname 
	 */
	@Override
	public File getCanonicalFile() {
		final String cPath = getCanonicalPath();
		return new RemoteFile(cPath, cPath, remoteAttrs, session);
	}

	/** Returns the canonical pathname string of this 
	 * abstract pathname.
	 * 
	 * <p>This method overrides the default implementation to return
	 * the canonical pathname string. On Unix/Linux machines there
	 * is not much difference between the absolute and canonical
	 * path names. Consequently, this method has an identical 
	 * return value as the {@link #getAbsolutePath()} method.</p>
	 * 
	 * <p>The canonical path to the file is essentially the full
	 * path returned by the {@link ChannelSftp#realpath(String)}
	 * method. The real path is determined only if the {@link #fullPath}
	 * instance variable is null.</p>
	 * 
	 * @return The canonical pathname string denoting the same file or 
	 * directory as this abstract pathname 
	 */
	@Override
	public String getCanonicalPath()  {
		return getAbsolutePath();
	}

	@Override
	public long getFreeSpace() {
		return 0L;
	}

	/** Returns the name of the file or directory denoted by this 
	 * abstract pathname.
	 * 
	 * This method returns just the last name in the pathname's 
	 * name sequence. If the pathname's name sequence is empty, 
	 * then the empty string is returned. 
	 * 
	 * @return The name of the file or directory denoted by this 
	 * abstract pathname, or the empty string if this pathname's 
	 * name sequence is empty.
	 */
	@Override
	public String getName() {
		String path = getPath();
		int slashPos = path.lastIndexOf('/');
		if (slashPos > 0) {
			return path.substring(slashPos + 1);
		}
		return path;
	}

	/**
	 * Returns the pathname string of this abstract pathname's parent, 
	 * or null if this pathname does not have a parent directory.
	 * 
	 * The <i>parent</i> of an abstract pathname consists of the 
	 * pathname's prefix, if any, and each name in the pathname's 
	 * name sequence except for the last. If the name sequence is 
	 * empty then the pathname does not name a parent directory.
	 * Note that the return value of this method contains the
	 * parent's path in its canonical form. 
	 * 
	 * @return The pathname string of the parent directory named
	 * by this abstract pathname, or null if this pathname does 
	 * not have a parent.
	 */
	@Override
	public String getParent() {
		if (fullPath == null) {
			fullPath = getCanonicalPath();
		}
		if (fullPath.equals("/")) {
			return null;
		}

		final int lastSlashPos = fullPath.lastIndexOf('/');
		if (lastSlashPos == -1) {
			return null;
		}
		final String parent = fullPath.substring(0, lastSlashPos);
		return (parent.length() > 0 ? parent : "/");
	}

	/**
	 * Returns the abstract pathname of this abstract pathname's parent, 
	 * or null if this pathname does not have a parent directory.
	 * 
	 * The <i>parent</i> of an abstract pathname consists of the 
	 * pathname's prefix, if any, and each name in the pathname's 
	 * name sequence except for the last. If the name sequence is 
	 * empty then the pathname does not name a parent directory.
	 * Note that the return value of this method contains the
	 * parent's path in its canonical form. 
	 * 
	 * @return The pathname object of the parent directory named
	 * by this abstract pathname, or null if this pathname does 
	 * not have a parent.
	 */
	@Override
	public File getParentFile() {
		final String parentPath = getParent();
		if (parentPath == null) {
			return null;
		}
		return new RemoteFile(parentPath, parentPath, null, session);
	}


	@Override
	public long getTotalSpace() {
		return super.getTotalSpace();
	}

	@Override
	public long getUsableSpace() {
		return getFreeSpace();
	}

	/**
	 * Tests whether this abstract pathname is absolute. 
	 * 
	 * The definition of absolute pathname is system dependent. 
	 * On Unix/Linux systems, a pathname is absolute if its 
	 * starts with a "/".
	 * 
	 * @return This method returns true if the path name 
	 * starts with a "/". Otherwise it returns false.
	 */
	@Override
	public boolean isAbsolute() {
		return (getPath().startsWith("/"));
	}

	/**
	 * Tests whether the file denoted by this abstract 
	 * pathname is a directory.
	 * 
	 * This method first checks and updates the file attributes. 
	 * File attributes are loaded from the remote server only if
	 * the {@link #remoteAttrs} object is null. The attributes 
	 * are obtained via a temporary SFTP connection.  
	 * 
	 * @return This method returns true if this RemoteFile
	 * denotes a directory. Otherwise this method returns false.
	 */
	@Override
	public boolean isDirectory() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return remoteAttrs.isDir();
	}

	/**
	 * Tests whether the file denoted by this abstract pathname is a 
	 * normal file. 
	 * 
	 * <p>A file is normal if it is not a directory and, in addition, 
	 * satisfies other system-dependent criteria. Any non-directory
	 * file created by a Java application is guaranteed to be a normal file.</p>
	 *  
	 * <p>This method first checks and updates the file attributes. 
	 * File attributes are loaded from the remote server only if
	 * the {@link #remoteAttrs} object is null. The attributes 
	 * are obtained via a temporary SFTP connection.</p>  
	 * 
	 * @return This method returns true if this RemoteFile
	 * denotes a file (and not a directory). Otherwise this 
	 * method returns false.
	 */
	@Override
	public boolean isFile() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return !remoteAttrs.isDir();
	}

	/**
	 * Tests whether the file named by this abstract pathname 
	 * is a hidden file. 
	 * 
	 * The exact definition of hidden is system-dependent. On 
	 * Unix/Linux systems, a file is considered to be hidden if
	 * its name begins with a period character ('.'). 
	 *  
	 * @return This method returns true if-and-only-if the file 
	 * denoted by this abstract pathname is hidden according to
	 * the conventions of Unix/Linux file systems. 
	 */
	@Override
	public boolean isHidden() {
		return isHidden(getPath(), true);
	}

	/**
	 * Helper method to determine if a given path denotes a hidden file.
	 * 
	 * On Unix/Linux file systems hidden files are files whose names 
	 * start with a period.
	 * 
	 * @param path The path to be tested to see if denotes a hidden file.
	 * 
	 * @param suppressHiddenFile If this flag is false, then this method
	 * checks only for "." (current directory) and ".." (parent directory)
	 * reference. If it is true, it also checks to see if the path 
	 * starts with a period. 
	 * 
	 * @return This method returns true if the file is to be deemed a
	 * hidden file. False otherwise.
	 */
	private boolean isHidden(final String path, final boolean suppressHiddenFile) {
		if ((path == null) || (".".equals(path)) || ("..".equals(path))) {
			return true;
		}
		return ((path != null) && suppressHiddenFile && path.startsWith("."));
	}

	/**
	 * Returns the time that the file denoted by this abstract pathname
	 * was last modified.
	 * 
	 * <p>This method returns the value of {@link SftpATTRS#getMTime()}.
	 * Therefore, this method first checks and updates the file attributes. 
	 * File attributes are loaded from the remote server only if
	 * the {@link #remoteAttrs} object is null. The attributes 
	 * are obtained via a temporary SFTP connection.</p>  
	 *
	 * @return A long value representing the time the file was last 
	 * modified, measured in milliseconds since the epoch 
	 * (00:00:00 GMT, January 1, 1970), or 0L if the file does 
	 * not exist or if an I/O error occurs. 
	 */
	@Override
	public long lastModified() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return remoteAttrs.getMTime();
	}

	/**
	 * Returns the length of the file denoted by this abstract pathname.
	 * 
	 * The return value is unspecified if this pathname denotes a directory. 
	 * 
	 * <p>This method returns the value of {@link SftpATTRS#getSize()}.
	 * Therefore, this method first checks and updates the file attributes. 
	 * File attributes are loaded from the remote server only if
	 * the {@link #remoteAttrs} object is null. The attributes 
	 * are obtained via a temporary SFTP connection.</p>  
	 *
	 * @return The length, in bytes, of the file denoted by this
	 * abstract pathname, or 0L if the file does not exist. Some
	 * operating systems may return 0L for pathnames denoting 
	 * system-dependent entities such as devices or pipes. 
	 */
	@Override
	public long length() {
		try {
			updateAttributes();
		} catch (SftpException e) {
			throw new SecurityException(e);
		}
		return remoteAttrs.getSize();
	}

	/**
	 * Returns an array of strings naming the files and directories
	 * in the directory denoted by this abstract pathname.
	 * 
	 * <p>If this abstract pathname does not denote a directory, 
	 * then this method returns null. Otherwise an array of 
	 * strings is returned, one for each file or directory 
	 * in the directory. Names denoting the directory itself
	 * and the directory's parent directory are not included
	 * in the result. Each string is a file name rather 
	 * than a complete path.</p>
	 * 
	 * <p>There is no guarantee that the name strings in the
	 * resulting array will appear in any specific order; 
	 * they are not, in particular, guaranteed to appear 
	 * in alphabetical order.</p>
	 * 
	 * @return An array of strings naming the files and 
	 * directories in the directory denoted by this 
	 * abstract pathname. The array will be empty if the 
	 * directory is empty. Returns null if this abstract 
	 * pathname does not denote a directory, or if an I/O 
	 * error occurs. 
	 */
	@Override
	public String[] list() {
		return list(false);
	}

	/**
	 * Helper method to eliminate hidden files from directory listings.
	 * 
	 * This method is a helper method that is called from the 
	 * {@link #list()} and {@link #list(boolean)} methods to
	 * obtain an array of strings with/without hidden files
	 * eliminated from the list. This method internally calls
	 * {@link ChannelSftp#ls(String)} to obtain the list of
	 * files.
	 * 
	 * @param suppressHiddenFiles If this parameter is true then
	 * hidden files (as reported by {@link #isHidden(String, boolean)})
	 * are not included in the response.
	 * 
	 * @return An array of strings naming the files and directories 
	 * in the directory denoted by this abstract pathname. The array
	 * will be empty if the directory is empty. Returns null if this
	 * abstract pathname does not denote a directory, or if an I/O 
	 * error occurs. 
	 */
	private String[] list(final boolean suppressHiddenFiles) {
		ArrayList<String> rfList = new ArrayList<String>(10);
		ChannelSftp sftp = null;
		try {
			// Never suppress such warnings in other spots. This
			// is needed only because JSch wants to maintain 
			// backwards compatibility with Java 4 and lower.
			@SuppressWarnings("rawtypes")
			Vector fileList = null;
			sftp = getLocalSftp();
			if (fullPath == null) {
				fullPath = sftp.realpath(getPath());
			}
			fileList = sftp.ls(fullPath);
			if (fileList != null) {
				for (int i = 0; (i < fileList.size()); i++) {
					LsEntry entry = (LsEntry) fileList.get(i);
					if (isHidden(entry.getFilename(), suppressHiddenFiles)) {
						// This is a hidden file
						continue;
					}
					String fileName = entry.getFilename();
					rfList.add(fileName);
				}
			}
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		String[] result = new String[rfList.size()];
		return rfList.toArray(result);
	}

	@Override
	public String[] list(FilenameFilter filter) {
		return super.list(filter);
	}

	/**
	 * Returns an array of abstract pathnames denoting the 
	 * files in the directory denoted by this abstract pathname.
	 * 
	 * <p>This method overrides the default implementation in the
	 * base class to provide an array of RemoteFile objects.
	 * The value returned by this method is the same as calling
	 * {@code listFiles(false)}.</p> 
	 * 
	 * <p>If this abstract pathname does not denote a directory, 
	 * then this method returns null. Otherwise an array of File
	 * objects is returned, one for each file or directory in the
	 * directory. Pathnames denoting the directory itself and the
	 * directory's parent directory are not included in the result.
	 * Each resulting abstract pathname is constructed from this 
	 * abstract pathname using the File(File, String) constructor.
	 * Therefore if this pathname is absolute then each resulting 
	 * pathname is absolute; if this pathname is relative then 
	 * each resulting pathname will be relative to the same 
	 * directory.</p>
	 * 
	 * <p>There is no guarantee that the name strings in the 
	 * resulting array will appear in any specific order; they
	 * are not, in particular, guaranteed to appear in 
	 * alphabetical order.</p>
	 * 
	 * @return An array of abstract pathnames denoting the 
	 * files and directories in the directory denoted by this
	 * abstract pathname. The array will be empty if the 
	 * directory is empty. Returns null if this abstract 
	 * pathname does not denote a directory, or if an I/O 
	 * error occurs. 
	 */
	@Override
	public File[] listFiles() {
		return listFiles(false);
	}

	/**
	 * Returns an array of abstract pathnames denoting the 
	 * files in the directory denoted by this abstract pathname
	 * (while including/excluding hidden files).
	 * 
	 * <p>If this abstract pathname does not denote a directory, 
	 * then this method returns null. Otherwise an array of File
	 * objects is returned, one for each file or directory in the
	 * directory. Pathnames denoting the directory itself and the
	 * directory's parent directory are not included in the result.
	 * Each resulting abstract pathname is constructed from this 
	 * abstract pathname using the File(File, String) constructor.
	 * Therefore if this pathname is absolute then each resulting 
	 * pathname is absolute; if this pathname is relative then 
	 * each resulting pathname will be relative to the same 
	 * directory.</p>
	 * 
	 * <p>There is no guarantee that the name strings in the 
	 * resulting array will appear in any specific order; they
	 * are not, in particular, guaranteed to appear in 
	 * alphabetical order.</p>
	 * 
	 * @param suppressHiddenFiles If this parameter is true then
	 * hidden files (as reported by {@link #isHidden(String, boolean)})
	 * are not included in the response.
	 * 
	 * @return An array of abstract pathnames denoting the 
	 * files and directories in the directory denoted by this
	 * abstract pathname. The array will be empty if the 
	 * directory is empty. Returns null if this abstract 
	 * pathname does not denote a directory, or if an I/O 
	 * error occurs. 
	 */
	public RemoteFile[] listFiles(final boolean suppressHiddenFiles) {
		ArrayList<RemoteFile> rfList = new ArrayList<RemoteFile>(128);
		ChannelSftp sftp = null;
		try {
			@SuppressWarnings("rawtypes")
			Vector fileList = null;
			sftp = getLocalSftp();
			if (fullPath == null) {
				fullPath = sftp.pwd() + "/" + getPath();
			}
			fileList = sftp.ls(fullPath);
			if (fileList != null) {
				for (int i = 0; (i < fileList.size()); i++) {
					final LsEntry entry = (LsEntry) fileList.get(i);
					if (isHidden(entry.getFilename(), suppressHiddenFiles)) {
						// The currently entry is a hidden file. Skip it
						continue;
					}
					final String path = entry.getFilename();
					final String absPath = fullPath + "/" + path;
					RemoteFile rf = new RemoteFile(path, absPath,
							entry.getAttrs(), session);
					rfList.add(rf);
				}
			}
		} catch (SftpException e) {
			// throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		RemoteFile[] result = new RemoteFile[rfList.size()];
		return rfList.toArray(result);
	}

	/**
	 * Returns an array of abstract pathnames denoting the 
	 * files and directories in the directory denoted by this 
	 * abstract pathname that satisfy the specified filter. 
	 * 
	 * The behavior of this method is the same as that of the 
	 * listFiles() method, except that the pathnames in the 
	 * returned array must satisfy the filter. If the given 
	 * filter is null then all pathnames are accepted. 
	 * Otherwise, a pathname satisfies the filter if and only if 
	 * the value true results when the FileFilter.accept(java.io.File) 
	 * method of the filter is invoked on the pathname. 
	 * 
	 * @param filter A file name filter.
	 * 
	 * @return An array of abstract pathnames denoting the files 
	 * and directories in the directory denoted by this abstract 
	 * pathname. The array will be empty if the directory is empty. 
	 * Returns null if this abstract pathname does not denote a directory, 
	 * or if an I/O error occurs. 
	 */
	@Override
	public File[] listFiles(FileFilter filter) {
		// Obtain list of all files.
		RemoteFile remoteFiles[] = listFiles(false);
		if (remoteFiles == null) {
			return null;
		}
		// Walk list of files and retain only those
		// that match the filter.
		ArrayList<RemoteFile> fileList = new ArrayList<RemoteFile>();
		for (RemoteFile rf: remoteFiles) {
			if ((filter == null) || filter.accept(rf)) {
				fileList.add(rf);
			}
		}
		return fileList.toArray(new RemoteFile[fileList.size()]);
	}

	/**
	 * Returns an array of abstract pathnames denoting the 
	 * files and directories in the directory denoted by this 
	 * abstract pathname that satisfy the specified filter. 
	 * 
	 * The behavior of this method is the same as that of the 
	 * listFiles() method, except that the pathnames in the 
	 * returned array must satisfy the filter. If the given 
	 * filter is null then all pathnames are accepted. Otherwise, 
	 * a pathname satisfies the filter if and only if the value 
	 * true results when the 
	 * {@link FilenameFilter#accept(File, String)}
	 * method of the filter is invoked on this abstract 
	 * pathname and the name of a file or directory in the 
	 * directory that it denotes.
	 * 
	 * @param filter A file name filter
	 * 
	 * @return An array of abstract pathnames denoting the 
	 * files and directories in the directory denoted by this 
	 * abstract pathname. The array will be empty if the directory
	 * is empty. Returns null if this abstract pathname does not 
	 * denote a directory, or if an I/O error occurs. 
	 */
	@Override
	public File[] listFiles(FilenameFilter filter) {
		// Obtain list of all files.
		RemoteFile remoteFiles[] = listFiles(false);
	    if (remoteFiles == null) {
	    	return null;
	    }
	    // Walk list of files and retain only those
	    // that match the filter.
	    ArrayList<RemoteFile> fileList = new ArrayList<RemoteFile>();
	    for (RemoteFile rf: remoteFiles) {
	    	if ((filter == null) || filter.accept(rf, rf.getPath())) {
	    		fileList.add(rf);
	        }
	    }
	    return fileList.toArray(new RemoteFile[fileList.size()]);
	}

	/**
	 * Creates the directory named by this abstract pathname.
	 * 
	 * This method essentially creates a new Sftp channel and 
	 * invokes {@link ChannelSftp#mkdir(String)} to create the
	 * directory.
	 * 
	 * @return This method returns true only if the directory was
	 * created. Otherwise it returns false.
	 */
	@Override
	public boolean mkdir() {
		boolean result = false;
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			sftp.mkdir(getAbsolutePath());
			result = true;
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Renames the file denoted by this abstract pathname. 
	 * 
	 * Many aspects of the behavior of this method are 
	 * inherently platform-dependent: The rename operation 
	 * might not be able to move a file from one file-system 
	 * to another, it might not be atomic, and it might not 
	 * succeed if a file with the destination abstract pathname 
	 * already exists. The return value should always be checked 
	 * to make sure that the rename operation was successful.
	 * 
	 * @return This method returns true if the renaming succeeded.
	 * Otherwise it returns false.
	 */
	@Override
	public boolean renameTo(File dest) {
		boolean result = false;
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			final String destPath = (dest instanceof RemoteFile ? dest
					.getAbsolutePath() : dest.getPath().replaceAll(
					"\\" + File.separator, "/"));
			sftp.rename(getAbsolutePath(), destPath);
			result = true;
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Sets the owner's or everybody's execute permission 
	 * for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * search (listing) privileges when execute permissions
	 * are set on the directory.
	 * 
	 * @param executable If true, sets the access permission 
	 * to allow execute operations; if false to disallow 
	 * execute operations.
	 *  
	 * @param ownerOnly - If true, the execute permission 
	 * applies only to the owner's execute permission; 
	 * otherwise, it applies to everybody (owner, group, and 
	 * others). 
	 */
	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		final int deltaPerms = S_IXUSR | (ownerOnly ? 0 : (S_IXGRP | S_IXOTH));
		return changePermissions(executable, deltaPerms);
	}

	/**
	 * Sets the owner's execute permission 
	 * for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * search (listing) privileges when execute permissions
	 * are set on the directory. An invocation of this method
	 * of the form {@code file.setExcutable(arg)} behaves in 
	 * exactly the same way as the invocation 
	 * {@code file.setExecutable(arg, true)}. 
	 * 
	 * @param executable If true, sets the access permission 
	 * to allow execute operations; if false to disallow 
	 * execute operations.
	 */
	@Override
	public boolean setExecutable(boolean executable) {
		return setExecutable(executable, true);
	}

	/**
	 * Sets the last-modified time of the 
	 * file or directory named by this abstract pathname.
	 * 
	 * <p>All platforms support file-modification times to the 
	 * nearest second, but some provide more precision. The 
	 * argument will be truncated to fit the supported precision. 
	 * If the operation succeeds and no intervening operations on 
	 * the file take place, then the next invocation of the 
	 * lastModified() method will return the (possibly truncated) 
	 * time argument that was passed to this method.</p>
	 * 
	 * <p>This method creates a temporary SFTP channel to the
	 * remote server and sets the modified time via call to
	 * {@link ChannelSftp#setMtime(String, int)}</p>
	 * 
	 * @param time The new last-modified time, measured in 
	 * milliseconds since the epoch (00:00:00 GMT, January 1, 1970) 
	 */
	@Override
	public boolean setLastModified(long time) {
		boolean result = false;
		ChannelSftp sftp = null;
		try {
			sftp = getLocalSftp();
			sftp.setMtime(getAbsolutePath(), (int) time);
			remoteAttrs = sftp.stat(getAbsolutePath());
			result = true;
		} catch (SftpException e) {
			throw new SecurityException(e);
		} finally {
			closeSftp(sftp);
		}
		return result;
	}

	/**
	 * Sets the owner's read permission for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * search (listing) privileges when read permissions
	 * are set on the directory. An invocation of this method
	 * of the form {@code file.setReadOnly(arg)} behaves in 
	 * exactly the same way as the invocation 
	 * {@code file.setWritable(false, false)}. 
	 * 
	 */
	@Override
	public boolean setReadOnly() {
		return setWritable(false, false);
	}

	/**
	 * Sets the owner's or everybody's read permission 
	 * for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * listing files privileges when read permissions
	 * are enabled on the directory.
	 * 
	 * @param readable If true, sets the access permission 
	 * to allow reading operations; if false to disallow 
	 * reading operations.
	 *  
	 * @param ownerOnly If true, the read permission 
	 * applies only to the owner's write permission; 
	 * otherwise, it applies to everybody (owner, group, and 
	 * others)
	 */
	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		final int deltaPerms = S_IRUSR | (ownerOnly ? 0 : (S_IRGRP | S_IROTH));
		return changePermissions(readable, deltaPerms);
	}

	/**
	 * Sets the owner's read permission for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * read (listing) privileges when read permissions
	 * are set on the directory. An invocation of this method
	 * of the form {@code file.setReadable(arg)} behaves in 
	 * exactly the same way as the invocation 
	 * {@code file.setReadable(arg, true)}. 
	 * 
	 * @param readable If true, sets the access permission 
	 * to allow read operations; if false to disallow 
	 * read operations.
	 */
	@Override
	public boolean setReadable(boolean readable) {
		return setReadable(readable, true);
	}

	/**
	 * Sets the owner's or everybody's write permission 
	 * for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * creating files privileges when write permissions
	 * are enabled on the directory.
	 * 
	 * @param writable If true, sets the access permission 
	 * to allow writing operations; if false to disallow 
	 * writing operations.
	 *  
	 * @param ownerOnly If true, the write permission 
	 * applies only to the owner's write permission; 
	 * otherwise, it applies to everybody (owner, group, and 
	 * others). 
	 */
	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		final int deltaPerms = S_IWUSR | (ownerOnly ? 0 : (S_IWGRP | S_IWOTH));
		return changePermissions(writable, deltaPerms);
	}

	/**
	 * Sets the owner's write permission for this abstract pathname.
	 * 
	 * If this object denotes a directory then it enables
	 * write  privileges for creating files and sub-directories
	 * when write permissions are set on the directory. An 
	 * invocation of this method
	 * of the form {@code file.setWriteable(arg)} behaves in 
	 * exactly the same way as the invocation 
	 * {@code file.setWriteable(arg, true)}. 
	 * 
	 * @param writable If true, sets the access permission 
	 * to allow write operations; if false to disallow 
	 * write operations.
	 */
	@Override
	public boolean setWritable(boolean writable) {
		return setWritable(writable, true);
	}

	/**
	 * A standard implementation for the clone() interface.
	 * 
	 * @return An identical copy of this object.
	 */
	@Override
	protected RemoteFile clone() throws CloneNotSupportedException {
		return new RemoteFile(this);
	}

	/**
	 * Fix to make a lot of things work better... (when running client on Windows, where File has a default of '\' ...
	 * Override and change any '\' into '/'
	 */
	@Override
	public String getPath()
	{
		String path = super.getPath();
		if (path != null)
			path = path.replace('\\', '/');

		return path;
	}
}