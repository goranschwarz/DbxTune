package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;

public class FileUtils
{
	private static Logger _logger = Logger.getLogger(FileUtils.class);

	
	public static void copy(String from_name, String to_name) 
	throws IOException
	{
		copy(from_name, to_name, false, false);
	}

	// This example is from _Java Examples in a Nutshell_. (http://www.oreilly.com)
	// Copyright (c) 1997 by David Flanagan
	// This example is provided WITHOUT ANY WARRANTY either expressed or implied.
	// You may study, use, modify, and distribute it for non-commercial purposes.
	// For any commercial use, see http://www.davidflanagan.com/javaexamples
	/**
	 * The static method that actually performs the file copy. Before copying
	 * the file, however, it performs a lot of tests to make sure everything is
	 * as it should be.
	 */
	public static void copy(String from_name, String to_name, boolean confirmOverWrite, boolean useGuiToConfirmOverWrite) 
	throws IOException
	{
		File from_file = new File(from_name); // Get File objects from Strings
		File to_file = new File(to_name);

		// First make sure the source file exists, is a file, and is readable.
		if ( !from_file.exists() )  abort("FileCopy: no such source file: "       + from_name);
		if ( !from_file.isFile() )  abort("FileCopy: can't copy directory: "      + from_name);
		if ( !canRead(from_file) ) abort("FileCopy: source file is unreadable: " + from_name);

		// If the destination is a directory, use the source file name
		// as the destination file name
		if ( to_file.isDirectory() )
			to_file = new File(to_file, from_file.getName());

		// If the destination exists, make sure it is a writeable file
		// and ask before overwriting it. If the destination doesn't
		// exist, make sure the directory exists and is writeable.
		if ( to_file.exists() )
		{
			if ( !canWrite(to_file) )
				abort("FileCopy: destination file is unwriteable: " + to_name);

			if (confirmOverWrite)
			{
				if (useGuiToConfirmOverWrite)
				{
					String htmlMsg = "<html>" +
    						"<h3>Confirm Overwite of file</h3>" +
    						"The destination file <code>"+to_name+"</code> exists!</b><br>" +
    						"Do you want to overwrite the existing file?</b><br>" +
    						"<br>" +
    						"File copy information:<br>" +
    						"<ul>" +
    						"   <li>from: <code>" + from_name + "</code></li>" +
    						"   <li>to:   <code>" + to_name   + "</code></li>" +
    						"</ul>" +
    						"</html>";

					//FIXME: Add view capability of source/dest file

					String[] options = { "Yes - Overwite destination file", "No - Keep destination file" };
					int result = JOptionPane.showOptionDialog(null, htmlMsg, "Copy File?", 
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
					
					if (result == 0) // Yes
					{
						// Simply continue with the copy
					}
					if (result == 1) // No
					{
						return;
					}
				}
				else
				{
    				// Ask whether to overwrite it
    				System.out.print("Overwrite existing file " + to_name + "? (Y/N): ");
    				System.out.flush();
    	
    				// Get the user's response.
    				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    				String response = in.readLine();
    	
    				// Check the response. If not a Yes, abort the copy.
    				if ( !response.equals("Y") && !response.equals("y") )
    					abort("FileCopy: existing file was not overwritten.");
				}
			}
		}
		else
		{
			// if file doesn't exist, check if directory exists and is writeable.
			// If getParent() returns null, then the directory is the current dir.
			// so look up the user.dir system property to find out what that is.
			String parent = to_file.getParent(); // Get the destination directory
			if ( parent == null )
				parent = System.getProperty("user.dir"); // or CWD
			File dir = new File(parent); // Convert it to a file.

			if ( !dir.exists() )   abort("FileCopy: destination directory doesn't exist: "  + parent);
			if ( dir.isFile() )    abort("FileCopy: destination is not a directory: "       + parent);
			if ( !canWrite(dir) )  abort("FileCopy: destination directory is unwriteable: " + parent);
		}

		// If we've gotten this far, then everything is okay.
		// So we copy the file, a buffer of bytes at a time.
		FileInputStream from = null; // Stream to read from source
		FileOutputStream to = null; // Stream to write to destination
		try
		{
			from = new FileInputStream(from_file); // Create input stream
			to   = new FileOutputStream(to_file); // Create output stream

			byte[] buffer = new byte[4096]; // A buffer to hold file contents
			int bytes_read; // How many bytes in buffer

			// Read a chunk of bytes into the buffer, then write them out,
			// looping until we reach the end of the file (when read() returns
			// -1).
			// Note the combination of assignment and comparison in this while
			// loop. This is a common I/O programming idiom.
			while ((bytes_read = from.read(buffer)) != -1)
				// Read bytes until EOF
				to.write(buffer, 0, bytes_read); // write bytes
		}
		// Always close the streams, even if exceptions were thrown
		finally
		{
			if (from != null) try { from.close(); } catch (IOException e) { ; }
			if (to   != null) try { to.close();   } catch (IOException e) { ; }
		}
	}

	/** A convenience method to throw an exception */
	private static void abort(String msg) throws IOException
	{
		throw new IOException(msg);
	}
	
	
	/**
	 * Check if a file is readable or not
	 * @param filename
	 * @return
	 */
	public static boolean canRead(String filename)
	{
		File f = new File(filename);
		return canRead(f);
	}

	/**
	 * Check if a file is readable or not
	 * @param filename
	 * @return
	 */
	public static boolean canRead(File file)
	{
		if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
		{
			if ( file.isFile())
			{
				// Open the file in READ and close it...
				try { new FileInputStream(file).close(); } 
				catch (IOException e) {	return false; }
				return true;
			}
		}

		return file.canRead();
	}

	/**
	 * Check if a file is writable or not
	 * @param filename
	 * @return
	 */
	public static boolean canWrite(String filename)
	{
		File f = new File(filename);
		return canWrite(f);
	}
	/**
	 * Check if a file is writable or not
	 * @param filename
	 * @return
	 */
	public static boolean canWrite(File file)
	{
		// for windows the canWrite() is faulty
		if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
		{
			if ( file.isFile())
			{
    			// Open the file in append mode and close it...
    			try { new FileOutputStream(file, true).close(); } 
    			catch (IOException e) {	return false; }
    			return true;
			}
		}

		return file.canWrite();
	}

	
	
//	private CodepageDetectorProxy _cpDetector = null;
//	private CodepageDetectorProxy initCpDetector() 
//	{
//		CodepageDetectorProxy cpDetector = CodepageDetectorProxy.getInstance();
//
//		// Add the implementations of info.monitorenter.cpdetector.io.ICodepageDetector: 
//		// This one is quick if we deal with unicode codepages: 
//		cpDetector.add(new ByteOrderMarkDetector()); 
//
//		// The first instance delegated to tries to detect the meta charset attribut in html pages.
//		cpDetector.add(new ParsingDetector(true)); // be verbose about parsing.
//
//		// This one does the tricks of exclusion and frequency detection, if first implementation is unsuccessful:
//		cpDetector.add(JChardetFacade.getInstance()); // Another singleton.
//		cpDetector.add(ASCIIDetector.getInstance()); // Fallback, see javadoc.
//		
//		return cpDetector;
//	}

//	public static String getFileEncoding(File file)
//	{
//		if (_cpDetector == null)
//			_cpDetector = initCpDetector();
//		java.nio.charset.Charset charset = null;
//		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
//		charset = _cpDetector.detectCodepage(bis, 16384);
//		bis.close();
//		System.out.println("openFile: charset="+charset);
//		System.out.println("openFile: charset="+charset.displayName());
//		System.out.println("openFile: charset="+charset.name());
//	
//		return charset == null ? null : charset.toString()
//}

	/**
	 * Get encoding a file is using, this so we can open/read the file with the correct charset. 
	 * <p>
	 * This is using JUniversialCharsetDetector http://code.google.com/p/juniversalchardet/<br>
	 * <br>
	 * juniversalchardet is a Java port of 'universalchardet', that is the encoding detector library of Mozilla.<br>
     * The original code of universalchardet is available at <a HREF="http://lxr.mozilla.org/seamonkey/source/extensions/universalchardet/">http://lxr.mozilla.org/seamonkey/source/extensions/universalchardet/</a><br>
     * Techniques used by universalchardet are described at <a HREF="http://www.mozilla.org/projects/intl/UniversalCharsetDetection.html">http://www.mozilla.org/projects/intl/UniversalCharsetDetection.html</a><br>
     *  
	 * @param filename filename to discover
	 * @return name of the charset
	 */
	public static String getFileEncoding(String filename)
	{
		File f = new File(filename);
		return getFileEncoding(f);
	}
	
	/**
	 * Get encoding a file is using, this so we can open/read the file with the correct charset. 
	 * <p>
	 * This is using JUniversialCharsetDetector http://code.google.com/p/juniversalchardet/<br>
	 * <br>
	 * juniversalchardet is a Java port of 'universalchardet', that is the encoding detector library of Mozilla.<br>
     * The original code of universalchardet is available at <a HREF="http://lxr.mozilla.org/seamonkey/source/extensions/universalchardet/">http://lxr.mozilla.org/seamonkey/source/extensions/universalchardet/</a><br>
     * Techniques used by universalchardet are described at <a HREF="http://www.mozilla.org/projects/intl/UniversalCharsetDetection.html">http://www.mozilla.org/projects/intl/UniversalCharsetDetection.html</a><br>
     *  
	 * @param file File to discover
	 * @return name of the charset
	 */
	public static String getFileEncoding(File file)
	{
		String encoding = null;
		try
		{
			byte[] buf = new byte[4096];
			FileInputStream fis = new FileInputStream(file);

			// Construct an instance of org.mozilla.universalchardet.UniversalDetector. 
			UniversalDetector detector = new UniversalDetector(null);

			// Read from the file until the detector is "happy" 
			// Feed some data (typically several thousands bytes) to the detector by calling UniversalDetector.handleData(). 
			int nread;
			while ((nread = fis.read(buf)) > 0 && !detector.isDone())
				detector.handleData(buf, 0, nread);

			// Notify the detector of the end of data by calling UniversalDetector.dataEnd(). 
			detector.dataEnd();

			// Get the detected encoding name by calling UniversalDetector.getDetectedCharset(). 
			encoding = detector.getDetectedCharset();
			if ( encoding != null )
				_logger.debug("getFileEncoding(): Detected encoding = " + encoding);
			else
				_logger.debug("getFileEncoding(): No encoding detected.");

			// Don't forget to call UniversalDetector.reset() before you reuse the detector instance. 
			detector.reset();
			fis.close();
		}
		catch (IOException e)
		{
			_logger.debug("getFileEncoding(): Caught: "+e, e);
		}

		return encoding;
	}

}
