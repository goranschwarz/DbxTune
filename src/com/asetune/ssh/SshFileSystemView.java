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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import com.asetune.utils.StringUtil;

import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3DirectoryEntry;

// got the idea from: http://mindtreeinsight.sourceforge.net/ui/xref/com/mindtree/techworks/insight/download/sftpbrowse/SFTPRemoteFileSystemView.html
// This needs A LOT MORE before it can be used
// * create a SftpFile extends File object

/**
 * THIS IS NOT READY FOR USE.... IT NEEDS *MORE* WORK... or it needs to be deleted and replaced by something else. 
 * @author Goran Schwarz
 */
public class SshFileSystemView extends FileSystemView
{
//	private static Logger _logger = Logger.getLogger(SshFileSystemView.class);

	public static final String FILE_SYSTEM_ROOT_NAME = "/";
	public static final String FILE_SEPERATOR = "/";
	
	private SshConnection _sshConn = null;
//	private Component _parentComponent = null;
	private SFTPv3Client _sftpClient = null;
	
	private String _startDirectory;
	private String _homeDirectory;
	private SshFile _homeDirectorySshFile;
	private SshFile _rootDirectorySshFile;

	public SshFileSystemView(SshConnection sshConn)
	throws Exception
	{
		_sshConn = sshConn;
		createSftpConnection();
	}

private SshFile[] _homeAllFilesArray = null;

	private synchronized void createSftpConnection() 
//	throws SftpBrowseException 
	throws Exception 
	{
		_sftpClient = new SFTPv3Client(_sshConn.getConnection());

		if (StringUtil.hasValue(_startDirectory)) 
		{
			_sshConn.execCommandOutputAsStr("cd "+_startDirectory);
//			_sftpClient.cd(_startDirectory);
		}

		_homeDirectory = _sshConn.execCommandOutputAsStr("pwd");
		if (StringUtil.hasValue(_homeDirectory))
		{
			_homeDirectory = _homeDirectory.trim();

			try
			{
				_homeDirectorySshFile = new SshFile(_homeDirectory, _sftpClient.stat(_homeDirectory));
				_rootDirectorySshFile = new SshFile("/", _sftpClient.stat("/"));

				List<SFTPv3DirectoryEntry> v = _sftpClient.ls(_homeDirectory);
				_homeAllFilesArray = new SshFile[v.size()];
				for (int i=0; i<v.size(); i++)
				{
					_homeAllFilesArray[i] = new SshFile(v.get(i));
				}
//System.out.println("XXXXXXXXXXXXX" + StringUtil.toCommaStr(_homeAllFilesArray));
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		
//System.out.println("SshFileSystemView.constructor: _homeDirectory='"+_homeDirectory+"'.");
//		_homeDirectory = "~"; // THis will probably NOT work
	}

	/**
	 * @see javax.swing.filechooser.FileSystemView#createNewFolder(java.io.File)
	 */
	@Override
	public File createNewFolder(File containingDir) throws IOException
	{
//		SFTPv3FileHandle xxx = _sftpClient.createFile(containingDir.getName());
//		return xxx.
		// This is a read only view of the remote system only.
//		throw new SftpBrowseException("This file system view supports READ ONLY support ONLY!");
		throw new IOException("This file system view supports READ ONLY support ONLY!");
//		return null;
	}

	/**
	 * In the remote view home and default directory are considered to be the same.
	 * @see javax.swing.filechooser.FileSystemView#getDefaultDirectory()
	 */
	@Override
	public File getDefaultDirectory()
	{
		File file = getHomeDirectory();
//		System.out.println("SshFileSystemView.getDefaultDirectory() <<-- "+file);
		return file;
	}
	/**
	 * @see javax.swing.filechooser.FileSystemView#getHomeDirectory()
	 */
	@Override
	public File getHomeDirectory()
	{
//		File file = super.getHomeDirectory();
//		File file = new File(_homeDirectory);
		File file = _homeDirectorySshFile;
//		System.out.println("SshFileSystemView.getHomeDirectory() <<-- "+file);
		return file;

		// TODO Auto-generated method stub
//		return super.getHomeDirectory();
//		346 		try {
//		347 			checkConnection();
//		348 			//If home directory is the root directory then return that
//		349 			if (homeDirectory.equals(FILE_SYSTEM_ROOT_NAME)) {
//		350 				return getRoots()[0];
//		351 			}
//		352 
//		353 			// Get the file information
//		354 			SFTPFileFile sftpFileFile = null;
//		355 			try {
//		356 				String parent = homeDirectory.substring(0, homeDirectory
//		357 						.lastIndexOf(FILE_SEPERATOR));
//		358 				
//		359 				// FIXME Correct this
//		360 				List returnedFiles = sftpClient.ls(parent);
//		361 				String dirName = homeDirectory.substring(homeDirectory
//		362 						.lastIndexOf(FILE_SEPERATOR) + 1);
//		363 				for (int i = 0; i < returnedFiles.size(); i++ ) {
//		364 					SftpFile returnedFile = (SftpFile) returnedFiles.get(i);
//		365 					if (returnedFile.getFilename().equals(dirName)) {
//		366 						sftpFileFile = new SFTPFileFile(returnedFile, this);
//		367 					}
//		368 				}
//		369 			} catch (SFTPBrowseException e) {
//		370 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		371 			} catch (IOException e) {
//		372 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		373 			}
//		374 
//		375 			return sftpFileFile;
//		376 		} catch (SFTPBrowseException e) {
//		377 			logger.log(Level.WARNING, "FTBEx", e);
//		378 		}
//		379 
//		380 		return null;

	}
	
	@Override
	public Icon getSystemIcon(File f)
	{
//		System.out.println("SshFileSystemView.getSystemIcon(f='"+f+"'.");
        return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");

//		System.out.println("SshFileSystemView.getSystemIcon(f='"+f+"') <<-- "+super.getSystemIcon(f));
//		return super.getSystemIcon(f);
	}

	@Override
	protected File createFileSystemRoot(File f)
	{
//		System.out.println("SshFileSystemView.createFileSystemRoot(f='"+f+"') <<-- "+super.createFileSystemRoot(f));
		return super.createFileSystemRoot(f);
	}

	/**
	 * @see javax.swing.filechooser.FileSystemView#getRoots()
	 */
	@Override
	public File[] getRoots()
	{
//		File[] files = super.getRoots();
		File[] files = new File[1];
		files[0] = new File("/");
		files[0] = _rootDirectorySshFile;
		createFileSystemRoot(files[0]);
//		System.out.println("SshFileSystemView.getRoots() <<-- "+StringUtil.toCommaStr(files));
		return files;

		// TODO Auto-generated method stub
//		return super.getRoots();
//		388 		SFTPFileFile [] sftpFiles = null;
//		389 		try {
//		390 			checkConnection();
//		391 
//		392 			sftpClient.cd(FILE_SYSTEM_ROOT_NAME);
//		393 			SftpFile sSftpFile = new SftpFile(sftpClient.pwd(), sftpClient.stat(FILE_SYSTEM_ROOT_NAME));
//		394 			sSftpFile.getAttributes().setPermissions(String.valueOf(FileAttributes.S_IFDIR));
//		395 			SFTPFileFile sSFTPFileFile = new SFTPFileFile(sSftpFile, this);
//		396 			sftpFiles = new SFTPFileFile [1];
//		397 			sftpFiles[0] = sSFTPFileFile;
//		398 		} catch (SFTPBrowseException e) {
//		399 			logger.log(Level.WARNING, "Could not get root file", e);
//		400 		} catch (IOException e) {
//		401 			logger.log(Level.WARNING, "Could not get root file", e);
//		402 		} 
//		403 		return sftpFiles;

	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#createFileObject(java.io.File, java.lang.String)
	 */
	@Override
	public File createFileObject(File dir, String filename)
	{
		File file = super.createFileObject(dir, filename);
//		System.out.println("SshFileSystemView.createFileObject(dir="+dir+", filename='"+filename+"') -WE-SHOULD-NOT-BE-CALLED- <<-- "+file);
		return file;	

//		// We should never get here. If we ever do, call the parent and hope for the best!!!
//		_logger.debug("Calling Super with: " + dir.toString() + " " + filename);
//		return super.createFileObject(dir, filename);	
	}

	/**
	 * @see javax.swing.filechooser.FileSystemView#createFileObject(java.lang.String)
	 */
	@Override
	public File createFileObject(String path)
	{
		File file = super.createFileObject(path);
//		System.out.println("SshFileSystemView.createFileObject(path='"+path+"') -WE-SHOULD-NOT-BE-CALLED- <<-- "+file);
		return file;	

//		// We should never get here. If we ever do, call the parent and hope for the best!!!
//		_logger.debug("Calling Super with: " + path);
//		return super.createFileObject(path);
	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#getChild(java.io.File, java.lang.String)
	 */
	@Override
	public File getChild(File parent, String fileName)
	{
		File file = super.getChild(parent, fileName);
//		System.out.println("######### getChild() ############### SshFileSystemView.getChild(parent="+parent+", fileName='"+fileName+"') <<-- "+file);
		return file;	

//		// TODO Auto-generated method stub
//		return super.getChild(parent, fileName);
//		435 		if (parent instanceof SFTPFileFile) {
//		436 			SftpFile parentDir = ((SFTPFileFile) parent).getSftpFile();
//		437 			SFTPFileFile returnedFile = null;
//		438 			try {
//		439 				checkConnection();
//		440 				List returnedFiles = sftpClient.ls(parentDir.getAbsolutePath());
//		441 				
//		442 				// Check if a path name is present.
//		443 				if (fileName.indexOf(FILE_SEPERATOR) > -1) {
//		444 					fileName = fileName.substring(fileName
//		445 							.lastIndexOf(FILE_SEPERATOR) + 1);
//		446 				}
//		447 				for (int i = 0; i < returnedFiles.size(); i++ ) {
//		448 					SftpFile returnedSftpFile = (SftpFile) returnedFiles.get(i);
//		449 					if (returnedSftpFile.getFilename().equals(fileName)) {
//		450 						returnedFile = new SFTPFileFile(returnedSftpFile, this);
//		451 					}
//		452 				}
//		453 			} catch (SFTPBrowseException e) {
//		454 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		455 			} catch (IOException e) {
//		456 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		457 			}
//		458 			return returnedFile;
//		459 		} else {
//		460 			// Should never get here!!!
//		461 			logger.fine("Calling Super with: " + parent.toString() + " " + fileName);
//		462 			return super.getChild(parent, fileName);
//		463 		}
	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#getFiles(java.io.File, boolean)
	 */
	@Override
	public File[] getFiles(File dir, boolean useFileHiding)
	{
//		System.out.println("--> SshFileSystemView.getFiles(dir="+dir+", useFileHiding="+useFileHiding+")");
//		File[] files = super.getFiles(dir, useFileHiding);
//		System.out.println("SshFileSystemView.getFiles(dir="+dir+", useFileHiding="+useFileHiding+") <<-- "+StringUtil.toCommaStr(files));
//		return files;
		
		try
		{
			String dirname = dir.getAbsolutePath().replace('\\', '/');

			// Only do this the first time... otherwise we will get strange errors (PLAF subsystem stack traces, since it tries to call getFiles())
			if (_homeAllFilesArray != null && dirname.equals(_homeDirectory))
			{
				SshFile[] tmp = _homeAllFilesArray;
				_homeAllFilesArray = null;
				return tmp;
			}
			_homeDirectory = _sshConn.execCommandOutputAsStr("pwd");
			
//System.out.println("    SshFileSystemView.getFiles(): dir.getAbsolutePath()='"+dirname+"'.");
			List<SFTPv3DirectoryEntry> v = _sftpClient.ls(dirname);
//			SshFile[] sshFileArray = new SshFile[v.size()];
			ArrayList<SshFile> sshFileList = new ArrayList<SshFile>();
			for (int i=0; i<v.size(); i++)
			{
				SFTPv3DirectoryEntry entry = v.get(i);
				if ( ! (".".equals(entry.filename) || "..".equals(entry.filename)) )
				{
					sshFileList.add(new SshFile(entry, dir));
//					sshFileArray[i] = new SshFile(entry, dir);
				}
			}
			return sshFileList.toArray(new SshFile[0]);
//			return sshFileArray;
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
			return new File[0];
		}

//		// TODO Auto-generated method stub
//		return super.getFiles(dir, useFileHiding);
//		472 		if (dir instanceof SFTPFileFile && dir.isDirectory()) {
//		473 			SftpFile sftpFile = ((SFTPFileFile) dir).getSftpFile();
//		474 			String name = sftpFile.getAbsolutePath();
//		475 			try {
//		476 				checkConnection();
//		477 
//		478 				// FIXME Fix parsing
//		479 				List returnedFiles = sftpClient.ls(name);
//		480 //				SFTPFileFile [] sftpFiles = new SFTPFileFile [returnedFiles.size()];
//		481 				List modifiedFileList = new ArrayList(returnedFiles.size());
//		482 				for (int i = 0; i < returnedFiles.size(); i++ ) {
//		483 					SftpFile returnedFile = (SftpFile) returnedFiles.get(i);
//		484 					String returnedFileName = returnedFile.getFilename();
//		485 					if (returnedFileName.equals(".") || returnedFileName.equals("..")) {
//		486 						continue;
//		487 					}
//		488 					modifiedFileList.add(new SFTPFileFile(returnedFile, this));
//		489 				}
//		490 				
//		491 				SFTPFileFile [] sftpFiles = (SFTPFileFile[]) modifiedFileList.toArray(new SFTPFileFile[modifiedFileList.size()]); 
//		492 				
//		493 				return sftpFiles;
//		494 
//		495 			} catch (SFTPBrowseException e) {
//		496 				logger.log(Level.WARNING, "Could not connect to host", e);
//		497 				return new SFTPFileFile [0];
//		498 			} catch (IOException e) {
//		499 				logger.log(Level.WARNING, "Could not operate on host", e);
//		500 				return new SFTPFileFile [0];
//		501 			} catch (Exception e) {
//		502 				logger.log(Level.WARNING, "Could not operate on host", e);
//		503 				return new SFTPFileFile [0];
//		504 			}
//		505 		}
//		506 		// Should never get here
//		507 		logger.fine("Calling Super with: " + dir.toString() + " " + String.valueOf(useFileHiding));
//		508 		return super.getFiles(dir, useFileHiding);

	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#getParentDirectory(java.io.File)
	 */
	@Override
	public File getParentDirectory(File dir)
	{
//		System.out.println("######### getParentDirectory() ############### SshFileSystemView.getParentDirectory(dir="+dir+").");
//		File file = super.getParentDirectory(dir);
//		System.out.println("######### getParentDirectory() ############### SshFileSystemView.getParentDirectory(dir="+dir+") <<-- "+file);
//		return file;

//		// TODO Auto-generated method stub
//		return super.getParentDirectory(dir);

		if ( dir instanceof SshFile )
		{
			SshFile sshFile = (SshFile) dir;
			String name = sshFile.getAbsolutePath();

			if ( "/".equals(name) || "\\".equals(name) )
			{
				return null;
			}

			int pos = name.lastIndexOf("/");
			if (pos < 0)
				pos = name.lastIndexOf("\\");
			String parent = name.substring(0, pos);

			// Parent is the root
			if ( "".equals(parent) || "/".equals(parent) || "\\".equals(parent) )
			{
				return getRoots()[0];
			}

			try
			{
				String unixStyle = parent.replace('\\', '/');
//				System.out.println("---------------- UnixStyleName='"+unixStyle+"'. do sftpClient.stat()");
				return new SshFile(parent, _sftpClient.stat(unixStyle));
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
//				return null;
				return getRoots()[0];
			}

//			// Parent of the parent to list the parent
//			pos = parent.lastIndexOf("/");
//			if (pos < 0)
//				pos = parent.lastIndexOf("\\");
//			String pparent = parent.substring(0, pos);
//			if ( StringUtil.isNullOrBlank(pparent) )
//			{
//				pparent = "/";
//			}
//
//			SshFile parentFile = null;
//			try
//			{
//				// FIXME Correct list parsing
//				pparent = pparent.replace('\\', '/');
//				Vector<SFTPv3DirectoryEntry> returnedFiles = _sftpClient.ls(pparent);
//
////				String parentName = parent.substring(parent.lastIndexOf(FILE_SEPERATOR) + 1);
//				pos = parent.lastIndexOf("/");
//				if (pos < 0)
//					pos = parent.lastIndexOf("\\");
//				String parentName = parent.substring(0, pos + 1);
//
//				for (int i=0; i<returnedFiles.size(); i++)
//				{
//					SFTPv3DirectoryEntry returnedFile = (SFTPv3DirectoryEntry) returnedFiles.get(i);
//					if ( returnedFile.filename.equals(parentName) )
//					{
//						parentFile = new SshFile(returnedFile);
//					}
//				}
//			}
////			catch (SFTPBrowseException e)
////			{
////				logger.log(Level.WARNING, "Problem browsing file system", e);
////			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
////				logger.log(Level.WARNING, "Problem browsing file system", e);
//			}
//
//			if ( null == parentFile )
//			{
//				parentFile = (SshFile) getRoots()[0];
//			}
//
//			return parentFile;
		}

//		logger.fine("Calling Super with: " + dir.toString());
//		System.out.println("Calling Super with: " + dir.toString());
		return super.getParentDirectory(dir);
		
//		517 		if (dir instanceof SFTPFileFile) {
//		518 
//		519 			SftpFile sftpFile = ((SFTPFileFile) dir).getSftpFile();
//		520 			String name = sftpFile.getAbsolutePath();
//		521 			if (name.equals(FILE_SYSTEM_ROOT_NAME)) {
//		522 				return null;
//		523 			}
//		524 
//		525 			String parent = name.substring(0, name.lastIndexOf(FILE_SEPERATOR));
//		526 
//		527 			// Parent is the root
//		528 			if (parent.equals(FILE_SYSTEM_ROOT_NAME)) {
//		529 				return getRoots()[0];
//		530 			}
//		531 
//		532 			// Parent of the parent to list the parent
//		533 			String pparent = parent.substring(0, parent
//		534 					.lastIndexOf(FILE_SEPERATOR));
//		535 			if (pparent.length() == 0) {
//		536 				pparent = FILE_SYSTEM_ROOT_NAME;
//		537 			}
//		538 
//		539 			SFTPFileFile parentFile = null;
//		540 
//		541 			try {
//		542 				checkConnection();
//		543 				// FIXME Correct list parsing
//		544 				List returnedFiles = sftpClient.ls(pparent);
//		545 				
//		546 				String parentName = parent.substring(parent
//		547 						.lastIndexOf(FILE_SEPERATOR) + 1);
//		548 				for (int i = 0; i < returnedFiles.size(); i++ ) {
//		549 					SftpFile returnedFile = (SftpFile) returnedFiles.get(i);
//		550 					if (returnedFile.getFilename().equals(parentName)) {
//		551 						parentFile = new SFTPFileFile(returnedFile, this);
//		552 					}
//		553 				}
//		554 			} catch (SFTPBrowseException e) {
//		555 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		556 			} catch (IOException e) {
//		557 				logger.log(Level.WARNING, "Problem browsing file system", e);
//		558 			}
//		559 
//		560 			if (null == parentFile) {
//		561 				parentFile = (SFTPFileFile) getRoots()[0];
//		562 			}
//		563 
//		564 			return parentFile;
//		565 		}
//		566 		
//		567 		logger.fine("Calling Super with: " + dir.toString());
//		568 		return super.getParentDirectory(dir);
	}

	/**
	 * @see javax.swing.filechooser.FileSystemView#getSystemDisplayName(java.io.File)
	 */
	@Override
	public String getSystemDisplayName(File f)
	{
		if (f instanceof SshFile)
		{
//			String name = ((SshFile)f).getAbsolutePath();
			String name = ((SshFile)f).getSftpName();;
//			System.out.println("             SshFileSystemView.(SshFile)getSystemDisplayName(f="+f+") <<-- "+name);
			return name;
		}

		String str = super.getSystemDisplayName(f);
//		System.out.println("             SshFileSystemView.getSystemDisplayName(f="+f+") <<-- "+str);
		return str;

//		// TODO Auto-generated method stub
//		return super.getSystemDisplayName(f);
//		576 		if (f instanceof SFTPFileFile) {
//		577 			SftpFile sftpFile = ((SFTPFileFile) f).getSftpFile();
//		578 			String name = sftpFile.getAbsolutePath();
//		579 			if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
//		580 				return this.host;
//		581 			} else {
//		582 				return f.getName();
//		583 			}
//		584 		} else {
//		585 			logger.fine("Calling Super with: " + f.getPath());
//		586 			return super.getSystemDisplayName(f);
//		587 		}
	}
	
	/**
	 * Always returns null. The super class uses this to return special folder names such as 'Desktop' on Windows.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#getSystemTypeDescription(java.io.File)
	 */
	@Override
	public String getSystemTypeDescription(File f)
	{
//		System.out.println("SshFileSystemView.getSystemTypeDescription(f="+f+") <<-- "+null);
		return null;
	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#isComputerNode(java.io.File)
	 */
	@Override
	public boolean isComputerNode(File dir)
	{
		boolean b = super.isComputerNode(dir);
//		System.out.println("SshFileSystemView.isComputerNode(dir="+dir+") <<-- "+b);
		return b;

//		// TODO Auto-generated method stub
//		return super.isComputerNode(dir);
//		606 		if (dir instanceof SFTPFileFile) {
//		607 			SftpFile sftpFile = ((SFTPFileFile) dir).getSftpFile();
//		608 			String name = sftpFile.getAbsolutePath();
//		609 			if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
//		610 				return true;
//		611 			} else {
//		612 				return false;
//		613 			}
//		614 
//		615 		} else {
//		616 			return super.isComputerNode(dir);
//		617 		}
	}

	/**
	 * Returns false, drives not supported on remote systems
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isDrive(java.io.File)
	 */
	@Override
	public boolean isDrive(File dir)
	{
//		System.out.println("SshFileSystemView.isDrive(dir="+dir+") <<-- "+false);
		return false;
	}

	/**
	 * Determines if the file is a real file or a link to another file.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isFileSystem(java.io.File)
	 * @return <code>true</code> if it is an absolute file or <code>false</code>
	 */
	@Override
	public boolean isFileSystem(File f)
	{
		boolean b = super.isFileSystem(f);
//		System.out.println("SshFileSystemView.isFileSystem(f="+f+") <<-- "+b);
		return b;

//		// TODO Auto-generated method stub
//		return super.isFileSystem(f);
//		639 		if (f instanceof SFTPFileFile) {
//		640 			SftpFile sftpFile = ((SFTPFileFile) f).getSftpFile();
//		641 			return !sftpFile.isLink();
//		642 		}
//		643 		logger.fine("Calling Super for: " + f.toString());
//		644 		return super.isFileSystem(f);
	}
	

	/**
	 * @see javax.swing.filechooser.FileSystemView#isFileSystemRoot(java.io.File)
	 */
	@Override
	public boolean isFileSystemRoot(File dir)
	{
		boolean b = super.isFileSystemRoot(dir);
//		System.out.println("SshFileSystemView.isFileSystemRoot(dir="+dir+") <<-- "+b);
		return b;

//		// TODO Auto-generated method stub
//		return super.isFileSystemRoot(dir);
//		652 		if (dir instanceof SFTPFileFile) {
//		653 			SftpFile sftpFile = ((SFTPFileFile) dir).getSftpFile();
//		654 			String name = sftpFile.getAbsolutePath();
//		655 			if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
//		656 				return true;
//		657 			} else {
//		658 				return false;
//		659 			}
//		660 		}
//		661 		logger.fine("Calling Super for: " + dir.toString());
//		662 		return super.isFileSystemRoot(dir);
	}
	
	/**
	 * Returns false. No floppy drives are viewable.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isFloppyDrive(java.io.File)
	 */
	@Override
	public boolean isFloppyDrive(File dir)
	{
//		System.out.println("SshFileSystemView.isFloppyDrive(dir="+dir+") <<-- "+false);
		return false;
	}
	
	/**
	 * Hidden files are not supported now. Maybe later!
	 * @see javax.swing.filechooser.FileSystemView#isHiddenFile(java.io.File)
	 */
	@Override
	public boolean isHiddenFile(File f)
	{
//		System.out.println("SshFileSystemView.isHiddenFile(f="+f+") <<-- "+false);
		return false;
	}

	/**
	 * @see javax.swing.filechooser.FileSystemView#isParent(java.io.File, java.io.File)
	 */
	@Override
	public boolean isParent(File folder, File file)
	{
		boolean b = super.isParent(folder, file);
//		System.out.println("######### isParent() ############### SshFileSystemView.isParent(folder="+folder+", file="+file+") <<-- "+b);
		return b;

//		// TODO Auto-generated method stub
//		return super.isParent(folder, file);
//		691 		if (folder instanceof SFTPFileFile && file instanceof SFTPFileFile) {
//		692 			// If file is a SFTPFileFile, you will always get back an SFTPFileFile
//		693 			SFTPFileFile calculatedParent = (SFTPFileFile) getParentDirectory(file);
//		694 			String parentPath = ((SFTPFileFile) folder).getSftpFile().getAbsolutePath();
//		695 			if (parentPath.equals(calculatedParent.getSftpFile().getAbsolutePath())) {
//		696 				return true;
//		697 			} else {
//		698 				return false;
//		699 			}
//		700 		}
//		701 		logger.fine("Calling Super for: " + folder.toString() + " " + file.toString());
//		702 		return super.isParent(folder, file);
	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#isRoot(java.io.File)
	 */
	@Override
	public boolean isRoot(File f)
	{
		if (f instanceof SshFile)
		{
			String name = ((SshFile)f).getAbsolutePath();
			boolean b = "/".equals(name) || "\\".equals(name);
//			System.out.println("SshFileSystemView.(SshFile)isRoot(f="+f+") <<-- "+b);
			return b;
		}

		boolean b = super.isRoot(f);
//		System.out.println("SshFileSystemView.isRoot(f="+f+") <<-- "+b);
		return b;

//		return super.isRoot(f);

//		710 		if (null == f) {
//		711 			return false;
//		712 		}
//		713 		
//		714 		if (f instanceof SFTPFileFile) {
//		715 			SftpFile sftpFile = ((SFTPFileFile) f).getSftpFile();
//		716 			String name = sftpFile.getAbsolutePath();
//		717 			if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
//		718 				return true;
//		719 			} else {
//		720 				return false;
//		721 			}
//		722 		}
//		723 		logger.fine("Calling super for: " + f.toString());
//		724 		return super.isRoot(f);
 	}
	
	/**
	 * @see javax.swing.filechooser.FileSystemView#isTraversable(java.io.File)
	 */
	@Override
	public Boolean isTraversable(File f)
	{
		if (f instanceof SshFile)
		{
			boolean b = ((SshFile)f).isDirectory();
//			System.out.println("SshFileSystemView.(SshFile)isTraversable(f.className="+f.getClass().getName()+", f="+f+") <<-- "+b);
			return b;
		}
		Boolean b = super.isTraversable(f);
//		System.out.println("SshFileSystemView.isTraversable(f.className="+f.getClass().getName()+", f="+f+") <<-- "+b);
		return b;

//		return super.isTraversable(f);
		
// 		732 		if (f instanceof SFTPFileFile) {
//		733 			SftpFile sftpFile = ((SFTPFileFile) f).getSftpFile();
//		734 			return new Boolean(sftpFile.isDirectory());
//		735 		}
//		736 		logger.fine("Calling super for: " + f.toString());
//		737 		return super.isTraversable(f);
	}
	
}
