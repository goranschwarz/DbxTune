/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.ssh;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.filechooser.FileSystemView;

import ch.ethz.ssh2.SFTPv3DirectoryEntry;
import ch.ethz.ssh2.SFTPv3FileAttributes;

///**
// * This class is a wrapper for <code>SFTPFile</code>. This class is to be used
// * by {@link javax.swing.JFileChooser JFileChooser}which requires an instance
// * of <code>File</code> to it.
// * <p>
// * Most operations on the file wrapped by this class are <b>not allowed </b>.
// * <code>SFTPFile</code> is meant to contain information about files on a
// * remote server, and <code>SFTPFileFile</code> is also meant for the same. It
// * merely provides a <code>File</code> like interface to <code>SFTPFile</code>.
// * </p>
// * <p>
// * For any operations on the file, use the information about the file from an
// * instance of this class and use
// * {@link com.sshtools.j2ssh.SftpClient SftpClient} or any wrapper around
// * it. You might use any other SFTPClient also.
// * </p>
// * <p>
// * The {@link SFTPFileFile#getHost() getHostURL}, {@link #getPort() getPort} and
// * {@link SFTPFileFile#getPasswordAuthentication() getPasswordAuthentication} 
// * methods return the host and authentication information to connect to the
// * host.
// * </p>
// * 
// * @see java.io.File File
// * 
// */
/**
 * THIS IS NOT READY FOR USE.... IT NEEDS *MORE* WORK... or it needs to be deleted and replaced by something else. 
 * @author Goran Schwarz
 */
public class SshFile extends File //implements Comparable
{
	/**
	 * Used for object serialization
	 */
	private static final long	   serialVersionUID	= 1566302834356038498L;

	//
	// Instance variables
	//

	// /**
	// * The sftpFile instance backing the wrapper
	// */
	// private SftpFile sftpFile;
	//
	/**
	 * The file system view which created this.
	 */
	private FileSystemView		   fileSystemView;

//	/**
//	 * The host connected to.
//	 */
//	private String				   host;
//
//	/**
//	 * The port connected to.
//	 */
//	private int					   port				= 22;
//
//	/**
//	 * User credentials for the remote host.
//	 */
//	private PasswordAuthentication passwordAuthentication;

	//
	// Constructors
	//

	private SFTPv3DirectoryEntry _sftDirEntry;

	public SshFile(SFTPv3DirectoryEntry sftDirEntry)
	{
		super(sftDirEntry.filename);
		_sftDirEntry = sftDirEntry;
	}

	private static String constructorFix(File f)
	{
		if (f == null)
			return "";
		
		String fStr = f.toString();
		if ("\\".equals(fStr) || "\\".equals(fStr))
			return "/";
		
		return fStr + "/";
	}
	public SshFile(SFTPv3DirectoryEntry sftDirEntry, File path)
	{
		super(constructorFix(path) + sftDirEntry.filename);
		_sftDirEntry = sftDirEntry;
	}

	public SshFile(String filename, SFTPv3FileAttributes sftFileAttr)
	{
		super(filename);
		_sftDirEntry = new SFTPv3DirectoryEntry();
		_sftDirEntry.filename   = filename;
		_sftDirEntry.longEntry  = filename;
		_sftDirEntry.attributes = sftFileAttr;
	}
	
//	/**
//	 * Constructs a new FTPFileFile object.
//	 * 
//	 * @param sftpFile The FTPFile instance to wrap
//	 * @param fileSystemView The file system view which created this instance.
//	 */
//	public SshFile(SftpFile sftpFile, FileSystemView fileSystemView)
//	{
//		super(sftpFile.getAbsolutePath());
//		this.sftpFile = sftpFile;
//		this.fileSystemView = fileSystemView;
//		if ( null != fileSystemView && fileSystemView instanceof SFTPRemoteFileSystemView )
//		{
//			SFTPRemoteFileSystemView sftpFileSystemView = (SFTPRemoteFileSystemView) fileSystemView;
//			this.host = sftpFileSystemView.getHost();
//			this.port = sftpFileSystemView.getPort();
//			this.passwordAuthentication = sftpFileSystemView.getPasswordAuthentication();
//		}
//	}

	//
	// Accessors
	//

//	/**
//	 * @return Returns the passwordAuthentication.
//	 */
//	public PasswordAuthentication getPasswordAuthentication()
//	{
//		return passwordAuthentication;
//	}
//
//	/**
//	 * @return Returns the hostURL.
//	 */
//	public String getHost()
//	{
//		return host;
//	}
//
//	/**
//	 * @return Returns the port connected to
//	 */
//	public int getPort()
//	{
//		return port;
//	}

	public String getSftpName()
	{
		return _sftDirEntry.filename;
	}

	//-----------------------------------------------------------------
	// Overridden methods from java.io.File
	//-----------------------------------------------------------------

//	/**
//	 * @return Returns the sftpFile.
//	 */
//	public SftpFile getSftpFile()
//	{
//
//		return this.sftpFile;
//	}

	/**
	 * @see java.io.File#isAbsolute()
	 */
	@Override
	public boolean isAbsolute()
	{
//		return !this.sftpFile.isLink();
		return !_sftDirEntry.attributes.isSymlink();
	}

	/**
	 * @see java.io.File#isDirectory()
	 */
	@Override
	public boolean isDirectory()
	{
//		return this.sftpFile.isDirectory();
		return _sftDirEntry.attributes.isDirectory();
	}

	/**
	 * @see java.io.File#isFile()
	 */
	@Override
	public boolean isFile()
	{
//		return this.sftpFile.isFile();
		return _sftDirEntry.attributes.isRegularFile();
	}

	/**
	 * @see java.io.File#lastModified()
	 */
	@Override
	public long lastModified()
	{
//		return sftpFile.getAttributes().getModifiedTime().longValue();
		return _sftDirEntry.attributes.mtime;
	}

	/**
	 * Always returns true. Read access is always allowed.
	 * 
	 * @see java.io.File#canRead()
	 */
	@Override
	public boolean canRead()
	{
		return true;
	}

	/**
	 * Always returns false. For more information, see note {@link SFTPFileFile
	 * above}.
	 * 
	 * @see java.io.File#canWrite()
	 */
	@Override
	public boolean canWrite()
	{
		return false;
	}

	/**
	 * @see java.io.File#compareTo(java.io.File)
	 */
	@Override
	public int compareTo(File pathname)
	{
		return this.getPath().compareToIgnoreCase(pathname.getPath());
	}

//	/**
//	 * @see java.lang.Comparable#compareTo(java.lang.Object)
//	 */
//	public int compareTo(Object o)
//	{
//		return compareTo((File) o);
//	}

	/**
	 * Operation not supported.
	 * 
	 * @throws IOException
	 *             Since this operation is not supported.
	 * @see java.io.File#createNewFile()
	 */
	@Override
	public boolean createNewFile() throws IOException
	{
		throw new IOException("READ-ONLY view");
	}

	/**
	 * Operation not supported, always returns false
	 * 
	 * @throws UnsupportedOperationException
	 *             Since this operation is not supported.
	 * @see java.io.File#delete()
	 */
	@Override
	public boolean delete()
	{
		return false;
	}

	/**
	 * Operation not supported. Nothing is returned.
	 * 
	 * @see java.io.File#deleteOnExit()
	 */
	@Override
	public void deleteOnExit()
	{
		// No Operation
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if ( (obj != null) && (obj instanceof File) )
		{
			return compareTo((File) obj) == 0;
		}
		return false;
	}

	/**
	 * Returns <code>true</code> always. Any file for which an FTPFile instance
	 * can be got is assumed to exist.
	 * 
	 * @see java.io.File#exists()
	 */
	@Override
	public boolean exists()
	{
		return true;
	}

	/**
	 * Returns the same instance!
	 * 
	 * @see java.io.File#getAbsoluteFile()
	 */
	@Override
	public File getAbsoluteFile()
	{
		return this;
	}

	/**
	 * Returns the path!
	 * 
	 * @see java.io.File#getAbsolutePath()
	 */
	@Override
	public String getAbsolutePath()
	{
		return this.getPath();
	}

	/**
	 * Returns the instance as the canonical file
	 * 
	 * @see java.io.File#getCanonicalFile()
	 */
	@Override
	public File getCanonicalFile() throws IOException
	{
		return this;
	}

	/**
	 * Always returns the unix style path from the root.
	 * 
	 * @see java.io.File#getCanonicalPath()
	 */
	@Override
	public String getCanonicalPath() throws IOException
	{
		return this.getPath();
	}

	/**
	 * Returns the name of the file
	 * 
	 * @see java.io.File#getName()
	 */
	@Override
	public String getName()
	{
//		return this.sftpFile.getFilename();
		return _sftDirEntry.filename;
	}

	/**
	 * Returns the path of the parent, if a file system view is present, or
	 * <code>null</code>.
	 * 
	 * @see java.io.File#getParent()
	 */
	@Override
	public String getParent()
	{
		if ( null != fileSystemView )
		{
			return fileSystemView.getParentDirectory(this).getPath();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns the parent directory, if a file system view is present, or
	 * <code>null</code>.
	 * 
	 * @see java.io.File#getParentFile()
	 */
	@Override
	public File getParentFile()
	{
		if ( null != fileSystemView )
		{
			return fileSystemView.getParentDirectory(this);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns the path in the unix style.
	 * 
	 * @see java.io.File#getPath()
	 */
	@Override
	public String getPath()
	{
//		return this.sftpFile.getAbsolutePath();
//		return this.getAbsolutePath();
		return super.getPath();
	}

	/**
	 * Returns false. No files are hidden!
	 * 
	 * @see java.io.File#isHidden()
	 */
	@Override
	public boolean isHidden()
	{
		return false;
	}

	/**
	 * Returns the size of the FTP file instance
	 * 
	 * @see java.io.File#length()
	 */
	@Override
	public long length()
	{
//		return this.sftpFile.getAttributes().getSize().longValue();
		return _sftDirEntry.attributes.size;
	}

	/**
	 * Operation is not supported. Use the {@link SFTPRemoteFileSystemView
	 * SFTPRemoteFileSystemView}for this.
	 * 
	 * @see java.io.File#list()
	 * @return An empty String array
	 */
	@Override
	public String[] list()
	{
		return new String[0];
	}

	/**
	 * Operation is not supported. Use the {@link SFTPRemoteFileSystemView
	 * SFTPRemoteFileSystemView}for this.
	 * 
	 * @see java.io.File#list(java.io.FilenameFilter)
	 * @return An empty String array
	 */
	@Override
	public String[] list(FilenameFilter filter)
	{
		return new String[0];
	}

	/**
	 * Operation is not supported. Use the {@link SFTPRemoteFileSystemView
	 * SFTPRemoteFileSystemView}for this.
	 * 
	 * @see java.io.File#listFiles()
	 * @return An empty File array
	 */
	@Override
	public File[] listFiles()
	{
//		return new SFTPFileFile[0];
		return new SshFile[0];
	}

	/**
	 * Operation is not supported. Use the {@link SFTPRemoteFileSystemView
	 * SFTPRemoteFileSystemView}for this.
	 * 
	 * @see java.io.File#listFiles(java.io.FileFilter)
	 * @return An empty File array
	 */
	@Override
	public File[] listFiles(FileFilter filter)
	{
//		return new SFTPFileFile[0];
		return new SshFile[0];
	}

	/**
	 * Operation is not supported. Use the {@link SFTPRemoteFileSystemView
	 * SFTPRemoteFileSystemView}for this.
	 * 
	 * @see java.io.File#listFiles(java.io.FilenameFilter)
	 * @return An empty File array
	 */
	@Override
	public File[] listFiles(FilenameFilter filter)
	{
//		return new SFTPFileFile[0];
		return new SshFile[0];
	}

	/**
	 * The operation is not supported, always returns <code>false</code>
	 * 
	 * @see java.io.File#mkdir()
	 */
	@Override
	public boolean mkdir()
	{
		return false;
	}

	/**
	 * The operation is not supported, always returns <code>false</code>
	 * 
	 * @see java.io.File#mkdirs()
	 */
	@Override
	public boolean mkdirs()
	{
		return false;
	}

	/**
	 * The operation is not supported, always returns <code>false</code>
	 * 
	 * @see java.io.File#renameTo(java.io.File)
	 */
	@Override
	public boolean renameTo(File dest)
	{
		return false;
	}

	/**
	 * The operation is not supported, always returns <code>false</code>
	 * 
	 * @see java.io.File#setLastModified(long)
	 */
	@Override
	public boolean setLastModified(long time)
	{
		return false;
	}

	/**
	 * The operation is not supported, always returns <code>false</code>
	 * 
	 * @see java.io.File#setReadOnly()
	 */
	@Override
	public boolean setReadOnly()
	{
		return false;
	}

	/**
	 * @see java.io.File#toURI()
	 */
	@Override
	public URI toURI()
	{
		try
		{
			String p = slashify(this.getPath(), this.isDirectory());
			if ( p.startsWith("//") )
				p = "//" + p;
			return new URI("file", null, p, null);
		}
		catch (URISyntaxException x)
		{
			throw new Error(x); // Can't happen
		}
	}

	/**
	 * @see java.io.File#toURL()
	 */
	@Override
	public URL toURL() throws MalformedURLException
	{
		return new URL("file", "", slashify(this.getPath(), this.isDirectory()));
	}

	/**
	 * Creates a slash representation of the file
	 * 
	 * @param path
	 *            The path to slashify
	 * @param isDirectory
	 *            Is it a directory
	 * @return The slashified path
	 */
	private String slashify(String path, boolean isDirectory)
	{
		String p = path;
		if ( !p.startsWith("/") )
			p = "/" + p;
		if ( !p.endsWith("/") && isDirectory )
			p = p + "/";
		return p;
	}

}
