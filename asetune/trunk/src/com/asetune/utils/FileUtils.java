package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtils
{
	public static void copy(String from_name, String to_name) 
	throws IOException
	{
		copy(from_name, to_name, false);
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
	public static void copy(String from_name, String to_name, boolean confirmOverWrite) 
	throws IOException
	{
		File from_file = new File(from_name); // Get File objects from Strings
		File to_file = new File(to_name);

		// First make sure the source file exists, is a file, and is readable.
		if ( !from_file.exists() )  abort("FileCopy: no such source file: "       + from_name);
		if ( !from_file.isFile() )  abort("FileCopy: can't copy directory: "      + from_name);
		if ( !from_file.canRead() ) abort("FileCopy: source file is unreadable: " + from_name);

		// If the destination is a directory, use the source file name
		// as the destination file name
		if ( to_file.isDirectory() )
			to_file = new File(to_file, from_file.getName());

		// If the destination exists, make sure it is a writeable file
		// and ask before overwriting it. If the destination doesn't
		// exist, make sure the directory exists and is writeable.
		if ( to_file.exists() )
		{
			if ( !to_file.canWrite() )
				abort("FileCopy: destination file is unwriteable: " + to_name);

			if (confirmOverWrite)
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
			if ( !dir.canWrite() ) abort("FileCopy: destination directory is unwriteable: " + parent);
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
}
