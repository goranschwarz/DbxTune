/**
 *	JSNPP - Java SNPP API.
 *	Copyright (C) 2016  Don Seiler <don@seiler.us>
 *	
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.asetune.alarm.writers.snpp;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * SNPP Connection Class
 *
 * @author Don Seiler <don@seiler.us>
 * @version $Id$
 */
public class Connection {

	private Socket socket = null;
	private PrintWriter out = null;
	private BufferedReader in = null;

	private String host = null;
	private int port = 444;

	/** Constructor */
	public Connection(String host, int port) {
		this.host = host;

		if (port > 0)
			this.port = port;
	}


	/**
	 * Connects to host/port of SNPP server.
	 *
	 * This method will create the socket to the SNPP server, and return the
	 * response code.  A successful connection will return a String beginning
	 * with "220", similar to this:
	 *
	 * 220 QuickPage v3.3 SNPP server ready at Tue May 17 11:48:12 2005
	 *
	 * @return Response of SNPP server to connection.
	 */
	public String connect() throws UnknownHostException, IOException {
		return connect(0, 0);
	}


	/**
	 * Connects to host/port of SNPP server, with specified timeout settings.
	 *
	 * This method will create the socket to the SNPP server, and return the
	 * response code.  A successful connection will return a String beginning
	 * with "220", similar to this:
	 *
	 * 220 QuickPage v3.3 SNPP server ready at Tue May 17 11:48:12 2005
	 *
	 * @param socketConnectTimeout	Timeout in milliseconds for connection attempt. A timeout of zero is interpreted as an infinite timeout.
	 * @param socketInputTimeout	Timeout in milliseconds to wait on responses from the SNPP server. A timeout of zero is interpreted as an infinite timeout.
	 * @return Response of SNPP server to connection.
	 */
	public String connect(int socketConnectTimeout, int socketInputTimeout)
			throws UnknownHostException, IOException, SocketTimeoutException {
		
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), socketConnectTimeout);
		socket.setSoTimeout(socketInputTimeout);

		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// This will contain the immediate response
		return in.readLine();
	}

	/**
	 * Closes connection to SNPP server.
	 * If any Exceptions are thrown while closing the streams or socket,
	 * then the first Exception encountered will be re-thrown.
	 *
	 * @throws SocketCloseException		Exception thrown while closing the socket
     	*/
	public void close() throws SocketCloseException {

		SocketCloseException sce = null;

		if (out != null) {
			try {
				out.close();
			} catch (Exception e) {
				if (sce == null) {
					sce = new SocketCloseException(e);
				}
			}
		}

		if (in != null) {
			try {
				in.close();
			} catch (Exception e) {
				if (sce == null) {
					sce = new SocketCloseException(e);
				}
			}
		}

		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				if (sce == null) {
					sce = new SocketCloseException(e);
				}
			}
		}

		if (sce != null) {
			throw sce;
		}
	}

	/** Sends data to SNPP server */
	public String send(String data) throws IOException {
		return send(data, true);
	}
	
	public String send(String data, boolean wait) throws IOException {
		String response = null;
		
		// Send command to server
		out.println(data);
		//System.out.println(data);
		
		// Read response
		if (wait) {
			response = in.readLine();
			//System.out.println(response);
		}
		
		// Return response, or null
		return response;
	}
}
