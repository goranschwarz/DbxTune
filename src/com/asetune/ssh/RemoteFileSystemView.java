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
// https://github.com/raodj/peace/blob/4af338d3093586a8dd19062b865a84d25b0a2d4f/gui/src/org/peace_tools/core/session/RemoteFile.java
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
//-------------------------------------------------------------

//package org.peace_tools.core.session;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.filechooser.FileSystemView;

import org.apache.log4j.Logger;

import com.asetune.ssh.SshConnection2.ExecOutput;

//import org.peace_tools.generic.Log.LogLevel;
//import org.peace_tools.generic.ProgrammerLog;
//import org.peace_tools.generic.UserLog;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
* A custom view of the file-system on a remote server. 
*
* <p>This class provides a customized implementation of the FileSystemView 
* class. The customized implementation enables the use of a standard
* JFileChooser to browse and select files on a remote server. 
* API. In other words, from a user-experience there is practically no
* difference between choosing a file on the local machine versus 
* choosing a file (or directory) on a remote machine. This class
* can be used as suggested below:</p>
* 
* <pre>
* {@code
*      RemoteServerSession rss   = new RemoteServerSession(getWindow(), getServer());
*      RemoteFileSystemView rfsv = new RemoteFileSystemView(rss);
*      JFileChooser jfc = new JFileChooser(rfsv);
*		//... more code to use jfc goes here.
*      rfsv.disconnect();	
* }
* </pre>
* 
* <p>Specifically, this class provides a session that can be used to
* interact with the local PC using the same API as that used for
* remote hosts. The consistent API eases development of the core GUI
* modules.</p>
* 
* <p>This class heavily uses the RemoteFile class. It returns instances
* of RemoteFile for all the API methods that require return of File object.
* These classes use the JSch library that provides a pure Java-based
* implementation of SSH (Secure Shell) protocol for establishing
* SFTP (Secure File Transfer Protocol) connections. JSch SSH
* supports the recent SSH-2 protocol. Please refer to JSch SSH website
* for further details: {@link http://www.jcraft.com/jsch/} </p>
*/
public class RemoteFileSystemView extends FileSystemView {

	private static Logger _logger = Logger.getLogger(RemoteFileSystemView.class);

	/**
	 * The JSch session that is used to obtain file information from 
	 * a remote server.
	 * 
	 * <p>This instance variable tracks the JSch session that has been created
	 * for this file system view. This instance object is created once in
	 * the constructor and is never changed during the life time of this
	 * object.</p>
	 * 
	 * <p>Note that JSch seems a bit touch about session and it is best
	 * to serialize access/use of the session from multiple threads. So
	 * access/use the session from a synchronized block as shown below:
	 * 
	 * <pre>
	 * {@code
	 * 	   synchronized(session) {
	 *         // do stuff with session here.
	 *     }
	 * }
	 * 
	 * 
	 * </pre>
	 * 
	 *  </p>
	 */
	private final Session  session;
	
	/**
	 * The JSch SFTP channel over which file information is retrieved.
	 * 
	 * This instance variable is created in the constructor once a 
	 * successful session has been established with a remote Unix/Linux
	 * machine. This channel is used to determine default home directory
	 * and present directory that are required for successful operation
	 * of this class. 
	 */
	private final ChannelSftp sftp;
	
	/**
	 * The set of canonical mount points that are treated as logical drives
	 * by this file system view.
	 * 
	 * This set contains the canonical mount points (as reported by mount
	 * Unix/Linux command) that are treated as logical drives. This value
	 * is used by the {@link #isDrive(File)} method. The values in this
	 * set are populated by the constructor by executing the {@code mount}
	 * command on the remote server, processing its outputs, and extracting
	 * the mount points.
	 */
	private final Set<String> remoteDrives = new TreeSet<String>();
	
	/**
	 * The only constructor for this class.
	 * 
	 * The constructor performs the following tasks:
	 * 
	 * <ol>
	 * 
	 * <li>It first requests the remote server session to connect to the 
	 * remote server by calling the {@link RemoteServerSession#connect()}
	 * method. This call handles obtaining password from the user etc.</li>
	 * 
	 * <li>Once the connection with the remote server has been established
	 * it obtains the JSch session via call to {@link RemoteServerSession#getConnection()}
	 * method. The returned value is used as the {@link #session} for this
	 * object.</li>
	 * 
	 * <li>Next, it creates a new SFTP channel using the {@link #session}
	 * and connects the SFTP channel to the remote server for further use.</li> 
	 * 
	 * <li>Finally, it executes the {@code mount} command on the remote machine
	 * to determine the mount points to be treated as logical drives. It parses
	 * the output of {@code mount} command line-by-line and populates the
	 * {@link #remoteDrives} set.</li>
	 * 
	 * </ol>
	 * 
	 * @param remoteServer A PEACE session to the remote server whose 
	 * file system(s) are to be browsed. Currently, this class only supports
	 * Unix/Linux file systems. Consequently, it is important to ensure that the
	 * OS type (see: {@link RemoteServerSession#getOSType()} for details) is
	 * appropriate for this class.
	 * 
	 * @throws Exception This method merely exposes the exceptions thrown by
	 * various helper methods call by the constructor. It does not handle any of
	 * the exceptions.
	 * 
	 * @see {@link RemoteServerSession#connect()}
	 */
//	public RemoteFileSystemView(RemoteServerSession remoteServer) throws Exception {
//		remoteServer.connect();
//		session = remoteServer.getConnection();
//	    sftp    = (ChannelSftp) session.openChannel("sftp");
//	    sftp.connect();
//	    // OK, try and obtain mount points for detecting logical drives on
//	    // the remote machine. We don't fail if mount command fails.
//	    try {
//	    	String cmdResults[] = new String[2];
//	    	final int exitCode = remoteServer.exec("mount", cmdResults);
//	    	if ((exitCode != 0) || (cmdResults[1].length() > 0)) {
//	    		throw new IOException("Error running mount command: " + cmdResults[1]);
//	    	}
//	    	// Process the mount output to extract the mount points. The output is
//	    	// in the form (we need the third word):
//	    	//     proc on /proc type proc (rw)
//	    	//     sysfs on /sys type sysfs (rw)
//	    	//     devpts on /dev/pts type devpts (rw,gid=5,mode=620)
//	    	final String[] outputLines = cmdResults[0].split("\n");
//	    	for(final String line: outputLines) {
//	    		// Locate the position of the "on" word and use the word after it
//	    		// as the path to a physical drive.
//	    		final int startIndex = line.indexOf(" on ");
//	    		if (startIndex == -1) {
//	    			// Hmmm...something is off here. Skip this line
//	    			continue;
//	    		}
//	    		// Get space position after the thrid word.
//	    		final int endIndex = line.indexOf(' ', startIndex + 4);
//	    		if (endIndex == -1) {
//	    			// Hmmm...something is off here. Skip this line
//	    			continue;
//	    		}
//	    		// Use the word as the mount point path.
//	    		remoteDrives.add(line.substring(startIndex + 4, endIndex));
//	    	}
//	    	// Always add the default root drive if list of remoteDrives is empty.
//	    	if (remoteDrives.isEmpty()) {
//	    		remoteDrives.add("/");
//	    	}
//	    } catch (Exception e) {
//
//	    	_logger.info("Unable to detect drives by running mount command. Will proceed with default.");
//	    }
//	}
	public RemoteFileSystemView(SshConnection2 sshConn) 
	throws Exception 
	{
		if (sshConn == null)
			throw new Exception("A valid SSH Connection must be passed. sshConn=" + sshConn);
		
		if ( ! sshConn.isConnected() )
			sshConn.connect();

		session = sshConn.getConnection();
		sftp    = (ChannelSftp) session.openChannel("sftp");
		sftp.connect();

		// OK, try and obtain mount points for detecting logical drives on
		// the remote machine. We don't fail if mount command fails.
		try 
		{
			ExecOutput out = sshConn.execCommandOutput("mount");
			if ((out.getExitCode() != 0) || !out.hasValueStdOut())
			{
				throw new IOException("Error running mount command: " + out.getStdErr());
			}
			
			// Process the mount output to extract the mount points. The output is
			// in the form (we need the third word):
			//     proc on /proc type proc (rw)
			//     sysfs on /sys type sysfs (rw)
			//     devpts on /dev/pts type devpts (rw,gid=5,mode=620)
			final String[] outputLines = out.getStdOut().split("\n");
			for(final String line: outputLines) 
			{
//System.out.println("MOUNT-LINE: |" + line + "|");
				// Locate the position of the "on" word and use the word after it
				// as the path to a physical drive.
				final int startIndex = line.indexOf(" on ");
//System.out.println("    --startIndex=" + startIndex);
				if (startIndex == -1) 
				{
					// Hmmm...something is off here. Skip this line
					continue;
				}
				// Get space position after the third word.
				final int endIndex = line.indexOf(' ', startIndex + 4);
//System.out.println("    --endIndex=" + endIndex);
				if (endIndex == -1) 
				{
					// Hmmm...something is off here. Skip this line
					continue;
				}
//System.out.println("    --ADD=|" + line.substring(startIndex + 4, endIndex) + "|.");
				// Use the word as the mount point path.
				remoteDrives.add(line.substring(startIndex + 4, endIndex));
			}

			// Always add the default root drive if list of remoteDrives is empty.
			if (remoteDrives.isEmpty()) 
			{
				remoteDrives.add("/");
			}
		}
		catch (Exception e) 
		{
	    	_logger.info("Unable to detect drives by running mount command. Will proceed with default.");
	    }
	}

	/**
	 * Convenience method to disconnect the SFTP and overall JSch session used
	 * by this class.
	 * 
	 * This is just a convenience method that can be used to wind-down the
	 * SFTP and JSch session used by this class. It is important to call
	 * this method to enusure connections are appropriately closed to avoid
	 * any hanging connections.
	 */
	public void disconnect() {
		sftp.disconnect();
		session.disconnect();
	}

//	@Override
//	public Icon getSystemIcon(File f)
//	{
////		System.out.println("SshFileSystemView.getSystemIcon(f='"+f+"'.");
//        return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
//
////		System.out.println("SshFileSystemView.getSystemIcon(f='"+f+"') <<-- "+super.getSystemIcon(f));
////		return super.getSystemIcon(f);
//	}
//
//	@Override
//	protected File createFileSystemRoot(File f)
//	{
////		System.out.println("SshFileSystemView.createFileSystemRoot(f='"+f+"') <<-- "+super.createFileSystemRoot(f));
//		return super.createFileSystemRoot(f);
//	}

	
	
	
	//------------------------------------------------------------------
	// The following methods override default implementation for FileSystemView
	//------------------------------------------------------------------

	
	/**
	 * This is a MUST to override, otherwise the Windows Look And Feel will: 
	 *   - Not show any name
	 *   - NullPointException
	 */
	@Override
	public String getSystemDisplayName(File f)
	{
		String xxx = super.getSystemDisplayName(f);
		if (xxx == null)
		{
			xxx = f.toString();
//			xxx = f.getName();
//			xxx = xxx.replace('\\', '/');
		}
//		System.out.println("++++ --------- getSystemDisplayName(f=|"+f+"|): <<< |"+xxx+"|");
		return xxx;
	}

//	@Override
//	public String getSystemTypeDescription(File f)
//	{
//		String xxx = super.getSystemTypeDescription(f);
//		System.out.println("++++ --------- getSystemTypeDescription(f=|"+f+"|): <<< |"+xxx+"|");
//		return xxx;
//	}
//
//	@Override
//	public Icon getSystemIcon(File f)
//	{
////		return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
////		Icon xxx = super.getSystemIcon(f);
//		Icon xxx = UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
////		System.out.println("++++ --------- getSystemIcon(f=|"+f+"|): <<< |"+xxx+"|");
//		return xxx;
//	}
//
//	@Override
//	public boolean isFileSystem(File f)
//	{
//		boolean xxx = super.isFileSystem(f);
//		System.out.println("++++ --------- isFileSystem(f=|"+f+"|): <<< |"+xxx+"|");
//		return xxx;
//	}
//
//	@Override
//	public boolean isFileSystemRoot(File dir)
//	{
//		boolean xxx = super.isFileSystemRoot(dir);
//		System.out.println("++++ --------- isFileSystemRoot(dir=|"+dir+"|): <<< |"+xxx+"|");
//		return xxx;
//	}
//
//	@Override
//	public boolean isComputerNode(File dir)
//	{
//		boolean xxx = super.isComputerNode(dir);
//		System.out.println("++++ --------- isComputerNode(dir=|"+dir+"|): <<< |"+xxx+"|");
//		return xxx;
//	}
//
//	@Override
//	protected File createFileSystemRoot(File f)
//	{
//		File xxx = super.createFileSystemRoot(f);
//		System.out.println("++++ --------- createFileSystemRoot(f=|"+f+"|): <<< |"+xxx+"|");
//		return xxx;
//	}

	
	
	
	/**
	 * Creates a new folder named "NewFolder" in a given directory.
	 * 
	 * This method merely creates a new folder with the name "NewFolder" in the
	 * given parent directory. It implements the corresponding abstract method
	 * in the base class.
	 * 
	 * @param parentDir The parent directory in which a new folder is to be
	 * created.
	 * 
	 * @Exception IOException This method throws an exception if the new directory
	 * could not be created.
	 */
	@Override
	public File createNewFolder(File parentDir) throws IOException {
		RemoteFile subDir = new RemoteFile(parentDir.getAbsolutePath() + "/" + "NewFolder", session);
		subDir.mkdir();
//System.out.println("--------- createNewFolder(parentDir=|"+parentDir+"|): <<< subDir=|"+subDir+"|");
		return subDir;
	}

	/**
	 * Return the user's default starting directory on this server.
	 * 
	 * This method overrides the implementation in the base class to
	 * return the default directory on the remote server. This method
	 * essentially returns the present working directory in which 
	 * the SFTP session starts as the default directory. This is typically
	 * the user's home directory.
	 * 
	 * @return The file that corresponds to the default starting
	 * directory (for SFTP session) on the remote server.
	 */
	@Override
	public File getDefaultDirectory() {
		File defaultDir = null;
		try {
			String pwd      = null;
			SftpATTRS attrs = null;
			synchronized (sftp) {
				pwd   = sftp.pwd();
				attrs = sftp.stat(pwd);
			}
			defaultDir = new RemoteFile(pwd, pwd, attrs, session);
		} catch (SftpException e) {
//			ProgrammerLog.log(e);
//			UserLog.log(LogLevel.NOTICE, "RemoteFileSystemView", e.getMessage());

			_logger.info("Problems getting default directory. " + e.getMessage());
		}
//System.out.println("--------- getDefaultDirectory(): <<< |"+defaultDir+"|");
		return defaultDir;
	}

	/**
	 * Return the user's home directory on this server.
	 * 
	 * This method overrides the implementation in the base class to
	 * return the home directory on the remote server. This method
	 * essentially returns the home directory directory as reported 
	 * by the SFTP session ({@link #sftp}).
	 * 
	 * @return The file that corresponds to the home directory
	 * for the user on the remote server.
	 */
	@Override
	public File getHomeDirectory() {
		File homeDir = null;
		try {
			String path     = null;
			SftpATTRS attrs = null;
			synchronized (sftp) {
				path  = sftp.getHome();
				attrs = sftp.stat(path);
			}
			homeDir = new RemoteFile(path, path, attrs, session);
		} catch (SftpException e) {
			e.printStackTrace();
		}
//System.out.println("--------- getHomeDirectory(): <<< |"+homeDir+"|");
		return homeDir;
	}

	/**
	 * Return the various root directories on the remote server.
	 * 
	 * This method overrides the implementation in the base class to
	 * return just the one root directory on Unix/Linux box, namely
	 * "/". This method
	 * 
	 * @return A list of RemoteFile objects. Since this class assumes
	 * it is operating on a Unix/Linux box this method returns only
	 * a single entry in the returned array.
	 */
	@Override
	public File[] getRoots() {
		RemoteFile rootDirs[] = null;
		try {
			String path     = "/";
			SftpATTRS attrs = null;
			synchronized (sftp) {
				attrs = sftp.stat(path);
			}
			rootDirs    = new RemoteFile[1];
			rootDirs[0] = new RemoteFile(path, attrs, session);
		} catch (SftpException e) {
			e.printStackTrace();
			rootDirs = new RemoteFile[0];
		}
//System.out.println("--------- getRoots(): <<< |"+rootDirs+"|");
		return rootDirs;
	}

	/**
	 * Helper method to create a new RemoteFile object to represent a
	 * given file (or folder) under another parent folder.
	 * 
	 * @param parent The File object representing a directory.
	 * 
	 * @param fileName The name of a file or folder which exists in parent
	 * 
	 * @return A remote file object that represents the compound path
	 * to the given file that is logically present within the given parent
	 * folder. 
	 */
	@Override
	public File getChild(File parent, String fileName) {
//		return new RemoteFile(parent.getAbsolutePath() + "/" + fileName, session);
		File f = new RemoteFile(parent.getAbsolutePath() + "/" + fileName, session);
//System.out.println("--------- getChild(parent=|"+parent+"|, filename=|"+fileName+"|): <<< |"+f+"|");
		return f;
	}

	/**
	 * Get the list of files (and sub-directories) in a given directory 
	 * while optionally excluding hidden files.
	 * 
	 * This method essentially calls the {@link RemoteFile#listFiles(boolean)}
	 * method if the given dir is a RemoteFile. If dir is not a RemoteFile
	 * then this method simply returns the value return by the base class 
	 * method (that is {@link FileSystemView#getFiles(File, boolean)}).
	 * 
	 * @param dir The directory whose files and sub-folders are to be returned. 
	 * 
	 * @param useFileHiding If this flag is true then hidden files (that is
	 * files whose names begins with a period) are not included in the 
	 * list of files returned by this method.
	 * 
	 * @return A list of RemoteFile objects that represent the files in
	 * the given directory.
	 */
	@Override
	public File[] getFiles(File dir, boolean useFileHiding) {
		if (dir instanceof RemoteFile) {
			final RemoteFile rf = (RemoteFile) dir;
//System.out.println("--------- getFiles(dir=|"+dir+"|, useFileHiding="+useFileHiding+"): <<< RemoteFile|"+rf.listFiles(useFileHiding)+"|");
			return rf.listFiles(useFileHiding);
		}
//System.out.println("--------- getFiles(dir=|"+dir+"|, useFileHiding="+useFileHiding+"): <<< |"+dir.listFiles()+"|");
		return dir.listFiles();
	}

	/**
	 * Return the parent directory for a given directory.
	 * 
	 * This method merely returns the value returned by call to
	 * {@link RemoteFile#getParent()} if dir is parent) or 
	 * {@link File#getParent()} if dir is a regular File object.
	 * 
	 * @param dir The directory (or file) whose parent directory is 
	 * to be returned.
	 * 
	 * @return The parent directory of dir, or null if dir is null.
	 * Note that the returned object is a RemoteFile if dir is 
	 * a RemoteFile. Otherwise the returned object is a regular File.
	 */
	@Override
	public File getParentDirectory(File dir) {
		if (dir == null) {
			return null;
		}
//System.out.println("--------- getParentDirectory(dir=|"+dir+"|): <<< |"+dir.getParentFile()+"|");
		return dir.getParentFile();
	}

	/**
	 * Method to determine if the given directory represents a logical drive.
	 * 
	 * This method is invoked to determine if the given file represents a 
	 * logical drive. This information is typically used to display a suitable
	 * icon against the file name to provide visual feedback to the user. This
	 * method essentially returns true if the canonical path matches the
	 * entries in the {@link #remoteDrives} set.
	 * 
	 * @param dir The file to be checked to determine if it represents a 
	 * logical drive.
	 * 
	 * @return This method returns true if the path represented by dir represents
	 * a logical drive on the remote server. Otherwise it returns false.
	 */
	@Override
	public boolean isDrive(File dir) {
		if (dir == null) {
			return false;
		}
//System.out.println("--------- isDrive(dir=|"+dir+"|): <<< |"+remoteDrives.contains(dir.getAbsolutePath())+"|");
		return remoteDrives.contains(dir.getAbsolutePath());
	}


	/**
	 * Determine if the given directory maps to a floppy disk drive.
	 * 
	 * Currently we have no mechanism to reliably detect if a given
	 * remote directory is mounted on a floppy disk. Consequently this
	 * method simply returns false.
	 * 
	 * @param dir The directory to be checked to see if it is mounted
	 * on a floppy disk.
	 * 
	 * @return This method always returns false.
	 */
	@Override
	public boolean isFloppyDrive(File dir) {
//System.out.println("--------- isFloppyDrive(dir=|"+dir+"|): <<< |false|");
		return false;
	}

	/**
	 * Method to determine if the given file is a hidden file.
	 * 
	 * This method overrides the default implementation in the base
	 * class to simply return the value of {@link RemoteFile#isHidden()}
	 * if parameter f is a RemoteFile or the value of {@link File#isHidden()}
	 * is the parameter f is a regular File object.
	 * 
	 * @param f The file to be determined if it is deemed a hidden file.
	 * 
	 * @return This method returns true if the given file is considered a
	 * hidden file. 
	 */
	@Override
	public boolean isHiddenFile(File f) {
//System.out.println("--------- isHiddenFile(f=|"+f+"|): <<< |"+f.isHidden()+"|");
		return f.isHidden();
	}

	/**
	 * Method to determine is the file folder is a parent of a given file.
	 * 
	 * This method obtain the parent folder of the given file by calling
	 * {@link RemoteFile#getParent()} and returns true if this value is
	 * the same as the folder's absolute path.
	 * 
	 * @param folder The directory to be checked if it contain the given
	 * file.
	 * 
	 * @param file The file (or sub-directory) to be checked to see if it
	 * is within the given folder.
	 * 
	 * @return This method returns true if folder is the parent directory
	 * that contains the given file.
	 */
	@Override
	public boolean isParent(File folder, File file) {
		final String parentPath = file.getParent();
//System.out.println("--------- isParent(folder=|"+folder+"|, file=|"+file+"|): <<< |"+parentPath.equals(folder.getAbsoluteFile())+"|");
		return (parentPath.equals(folder.getAbsoluteFile()));
	}

	/**
	 * Method to determine if the given file is the root directory.
	 * 
	 * This method overrides the default implementation in the base class
	 * to handle remote Unix/Linux file systems.
	 * 
	 * @param f The file to checked to see if it represents the root ("/")
	 * of a Unix/Linux file system on the remote server.
	 * 
	 * @return This method returns true if the absolute path of the given
	 * file is the root (that is "/"). Otherwise this method returns false.
	 */
	@Override
	public boolean isRoot(File f) {
//System.out.println("--------- isRoot(f=|"+f+"|): <<< |xxxxxxxxxxxxx|");
		if (f == null) {
			return false;
		}
		return f.getAbsolutePath().equals("/");
	}

	/**
	 * Method to determine if the given file is a directory that the user
	 * can view..
	 * 
	 * This method overrides the default implementation in the base class
	 * to handle remote Unix/Linux file systems.
	 * 
	 * @param f The file to checked to see if its contents can be traversed
	 * by the user.
	 * 
	 * @return This method returns true if f is a directory and the user has
	 * read permissions on it. Otherwise this method returns false.
	 */
	@Override
	public Boolean isTraversable(File f) {
//System.out.println("--------- isTraversable(f=|"+f+"|): <<< |"+((f != null) && f.isDirectory() && f.canRead())+"|");
		return (f != null) && f.isDirectory() && f.canRead();
	}
	
  /**
   * Returns a File object constructed in dir from the given filename.
   * 
   * This method overrides the default implementation in the base class
   * to create and return a RemoteFile object.
   * 
   * @param dir The directory that should logically contain the given
   * filename. This parameter can be null.  If it is null, then the
   * file is assumed to be in the present working directory on the
   * remote server.
   * 
   * @param filename The name of the file that is contained in the 
   * given directory.
   * 
   * @return A RemoteFile object that represents the directory
   * and the filename.
   */
	@Override
  public File createFileObject(File dir, String filename) {
//System.out.println(">>> createFileObject(dir=|"+dir+"|, filename=|"+filename+"|): dir="+(dir==null?"---null---":dir.getClass().getName()));
//System.out.println("--------- createFileObject(dir=|"+dir+"|, filename=|"+filename+"|): <<< |xxxxxxxxx|");
      if(dir == null) {
          return new RemoteFile(filename, session);
      } else {
          return new RemoteFile(dir + "/" + filename, session);
      }
  }

  /**
   * Returns a File object constructed from the given path string.
   * 
   * This method overrides the default implementation in the base
   * class to create and return a RemoteFile object.
   * 
   * @param path The path to be converted to a suitable File. This
   * parameter cannot be empty.
   * 
   * @return A RemoteFile object that logically represents the
   * given path.
   */
	@Override
  public File createFileObject(String path) {
//System.out.println("--------- createFileObject(path=|"+path+"|): <<< |xxxxxxxxx|");
      return new RemoteFile(path, session);
  }
}