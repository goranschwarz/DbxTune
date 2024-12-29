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

package com.dbxtune.alarm.writers.snpp;

import java.io.IOException;

/**
 * SNPP Message Class
 *
 * @author Don Seiler <don@seiler.us>
 * @version $Id$
 */
public class Message {

	// Maximum SNPP Level Supported
	public static final int MAX_SNPP_LEVEL = 3;

	// Message states
	public static final int STATE_UNDEF = -1;
	public static final int STATE_CONN = 0;
	public static final int STATE_PAGE = 11;
	public static final int STATE_MESS = 12;
	public static final int STATE_SEND = 13;
	public static final int STATE_QUIT = 14;
	public static final int STATE_DATA = 21;
	public static final int STATE_LOGI = 22;
	public static final int STATE_LEVE = 23;
	public static final int STATE_COVE = 24;
	public static final int STATE_SUBJ = 25;
	public static final int STATE_HOLD = 26;
	public static final int STATE_CALL = 27;
	public static final int STATE_ALER = 28;


	/** Connection to server */
	private Connection conn = null;


	/** Message state */
	private int state = STATE_UNDEF;


	/** Pager (recipient) */
	private String pager = null;


	/** Message */
	private String message = null;


	/** 
	 * Data 
	 *
	 * A message cannot send both MESS and DATA.
	 */
	private String[] data = null;


	/** CallerIdentifier */
	private String callerIdentifier = null;


	/** Subject */
	private String subject = null;


	/** Login */
	private String login = null;


	/** Password */
	private String password = null;


	/** Service Level */
	private int serviceLevel = -1;


	/** Coverage Area */
	private int coverageArea = -1;


	/** Hold Until YYMMDDHHMISS (+/- GMT diff) */
	private String holdUntil = null;


	/** Alert Override 0-DoNotAlert or 1-Alert */
	private boolean alertOverride = false;


	/** Level */
	private int level = MAX_SNPP_LEVEL;


	/** Track this response */
	private String sendResponse = null;

	/** Socket connection timeout in millis.  Zero is an infinite timeout. **/
	int socketConnectTimeout = 0;

	/** Socket read timeout in millis. Zero is an infinite timeout. **/
	private int socketInputTimeout = 0;
	

	/** Constructor */
	public Message() {
	}


	/** Constructor */
	public Message (String host,
					int port,
					String pager) {
		setPager(pager);
		
		setConnectionInfo(host, port);
	}


	/** Constructor */
	public Message (String host,
					int port,
					String pager,
					String message) {
		setPager(pager);
		setMessage(message);
		
		setConnectionInfo(host, port);
	}


	/** 
	 * Constructor 
	 *
	 * @deprecated
	 */
	public Message (String host,
					int port,
					String sender,
					String pager,
					String message) {
		setCallerIdentifier(sender);
		setPager(pager);
		setMessage(message);

		setConnectionInfo(host, port);
	}


	/** Returns state */
	public int getState () {
		return state;
	}


	/** Returns callerIdentifier */
	public String getCallerIdentifier () {
		return callerIdentifier;
	}


	/**
	 * Returns recipient 
	 *
	 * @deprecated
	 */
	public String getRecipient () {
		return getPager();
	}


	/** Returns pager */
	public String getPager() {
		return pager;
	}


	/** Returns message */
	public String getMessage () {
		return message;
	}


	/** Sets connection info */
	public void setConnectionInfo(String host, int port) {
		conn = new Connection(host, port);
		state = STATE_CONN;
	}

	/** Get the socket SO_TIMEOUT (read) timeout, in milliseconds **/
	public int getSocketInputTimeout() {
		return socketInputTimeout;
	}

	/**
	 * Enable/disable the socket SO_TIMEOUT with the specified (read) timeout, in milliseconds.
	 * The timeout must be > 0. A timeout of zero is interpreted as an infinite timeout.
	 *
	 * @param socketInputTimeout
     	*/
	public void setSocketInputTimeout(int socketInputTimeout) {
		if (socketInputTimeout >= 0) {
			this.socketInputTimeout = socketInputTimeout;
		}
	}

	/** Get the server connection timeout, in milliseconds **/
	public int getSocketConnectTimeout() {
		return socketConnectTimeout;
	}

	/**
	 * Set the server connection timeout value in milliseconds.
	 * A timeout of zero is interpreted as an infinite timeout.
	 *
	 * @param socketConnectTimeout
     	*/
	public void setSocketConnectTimeout(int socketConnectTimeout) {
		if (socketConnectTimeout >= 0) {
			this.socketConnectTimeout = socketConnectTimeout;
		}
	}

	/** Sets state */
	public void setState(int state) {
		this.state = state;
	}


	/** 
	 * Sets sender 
	 *
	 * @deprecated
	 */
	public void setSender(String sender) {
		setCallerIdentifier(sender);
	}


	/** Sets callerIdentifer */
	public void setCallerIdentifier(String callerIdentifier) {
		this.callerIdentifier = callerIdentifier;
	}


	/** 
	 * Sets recipient 
	 *
	 * @deprecated
	 */
	public void setRecipient(String recipient) {
		setPager(recipient);
	}


	/** Sets pager */
	public void setPager(String pager) {
		this.pager = pager;
	}


	/** Sets message */
	public void setMessage(String message) {
		// Check for \n chars
		this.message = message;
	}


	/** Returns SNPP level of this message */
	public int getLevel() {
		return level;
	}


	/** Sets SNPP level of this message */
	public void setLevel(int level) {
		if (level <= MAX_SNPP_LEVEL)
			this.level = level;
		else
			this.level = MAX_SNPP_LEVEL;
	}


	/** Sets subject */
	public void setSubject(String subject) {
		this.subject = subject;
	}


	/** Returns subject */
	public String getSubject() {
		return subject;
	}


	/** Sets data */
	public void setData(String[] data) {
		this.data = data;
	}


	/** Returns data */
	public String[] getData() {
		return data;
	}


	/** Sets alert override */
	public void setAlertOverride(boolean alertOverride) {
		this.alertOverride = alertOverride;
	}


	/** Returns alert override */
	public boolean getAlertOverride() {
		return alertOverride;
	}


	/**
	 * Sets holdUntil
	 *
	 * Must be in YYMMDDHHMISS format
	 */
	public void setHoldUntil(String holdUntil) {
		this.holdUntil = holdUntil;
	}


	/** Returns "hold until" value */
	public String getHoldUntil() {
		return holdUntil;
	}


	/** Sets login */
	public void setLogin(String login) {
		this.login = login;
	}


	/** Sets login with password */
	public void SetLogin(String login, String password) {
		this.login = login;
		this.password = password;
	}


	/** Returns login */
	public String getLogin() {
		return login;
	}


	/** Sets coverage area */
	public void setCoverageArea(int coverageArea) {
		this.coverageArea = coverageArea;
	}


	/** Returns coverage area */
	public int getCoverageArea() {
		return coverageArea;
	}


	/** Sets service level */
	public void setServiceLevel(int serviceLevel) {
		this.serviceLevel = serviceLevel;
	}


	/** Returns service level */
	public int getServiceLevel() {
		return serviceLevel;
	}


	/** Returns response from SEND command */
	public String getSENDResponse() {
		return sendResponse;
	}


	//
	// Function to format and send messages
	//

	/**
	 * Connects to server and sends message.
	 *
	 * @throws	SocketCloseException	if error while closing socket
	 * @throws	IOException		If bad socket I/O with SNPP server.
	 * @throws	Exception		If bad or unknown server response received.
	 */
	public void send() throws SocketCloseException, IOException, Exception {
		handleResponse(conn.connect(socketConnectTimeout, socketInputTimeout));
	}


	/**
	 * Handles response from server.
	 *
	 * We ignore all 500 messages (Command Not Implemented) from optional Level 2
	 * functions (optional as defined in RFC 1861).
	 */
	private void handleResponse(String response) throws IOException, Exception {

		// XXX PAGE should be sent AFTER LEVE, ALER, HOLD, COVE
		
		switch (state) {
			case STATE_CONN:
				if (response.startsWith("220"))
					if (level >= 2)
						sendLOGICommand();
					else
						sendPAGECommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_LOGI:
                // 230 is a success response that Hylafax server software uses, for some reason.
				if (response.startsWith("230") || response.startsWith("250") || response.startsWith("500"))
					sendCOVECommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_COVE:
				if (response.startsWith("250") || response.startsWith("500"))
					sendLEVECommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_LEVE:
				if (response.startsWith("250") || response.startsWith("500"))
					sendHOLDCommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_HOLD:
				if (response.startsWith("250") || response.startsWith("500"))
					sendALERCommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_ALER:
				if (response.startsWith("250") || response.startsWith("500")) 
					sendPAGECommand();
				else
					errorOut(response);
				break;
				

			case STATE_PAGE:
				if (response.startsWith("250")) {
					if (level >= 2)
						sendCALLCommand();
					else
						sendMESSCommand();
				} else
					errorOut(response);
				break;

				
			case STATE_CALL:
				if (response.startsWith("250") || response.startsWith("500"))
					sendSUBJCommand();
				else
					errorOut(response);
				break;
				
				
			case STATE_SUBJ:
				if (response.startsWith("250") || response.startsWith("500"))
					sendMESSCommand();
				else
					errorOut(response);
				break;

				
			case STATE_MESS:
				if (response.startsWith("250"))
					sendSENDCommand();
				else
					errorOut(response);
				break;
				
			case STATE_DATA:
				if (response.startsWith("354"))
					sendDATAData();
				else if (response.startsWith("250"))
					sendSENDCommand();
				else
					errorOut(response);
				break;

				
			case STATE_SEND:
				// Accept level 1-3 success messages
				sendResponse = response;
				if (response.startsWith("250")
						|| response.startsWith("860") 
						|| response.startsWith("960"))
					sendQUITCommand();
				else
					errorOut(response);
				break;

				
			case STATE_QUIT:
				if (response.startsWith("221"))
					conn.close();
				else
					errorOut(response);
				break;
		}
	}


	private void sendPAGECommand() throws IOException, Exception {
		state = STATE_PAGE;
		String msg = "PAGE " + pager;
		handleResponse(conn.send(msg));
	}


	private void sendCALLCommand() throws IOException, Exception {
		if (callerIdentifier != null) {
			state = STATE_CALL;
			String msg = "CALL " + callerIdentifier;
			handleResponse(conn.send(msg));
		} else {
			sendMESSCommand();
		}
	}


	private void sendCOVECommand() throws IOException, Exception {
		if (coverageArea > 0) {
			state = STATE_COVE;
			String msg = "COVE " + coverageArea;
			handleResponse(conn.send(msg));
		} else {
			sendLEVECommand();
		}
	}


	private void sendLEVECommand() throws IOException, Exception {
		if (serviceLevel > 0) {
			state = STATE_LEVE;
			String msg = "LEVE " + serviceLevel;
			handleResponse(conn.send(msg));
		} else {
			sendHOLDCommand();
		}
	}


	private void sendHOLDCommand() throws IOException, Exception {
		if (holdUntil != null) {
			state = STATE_HOLD;
			String msg = "HOLD " + holdUntil;
			handleResponse(conn.send(msg));
		} else {
			sendALERCommand();
		}
	}


	private void sendALERCommand() throws IOException, Exception {
		if (alertOverride) {
			state = STATE_ALER;
			handleResponse(conn.send("ALER"));
		} else {
			sendPAGECommand();
		}
	}


	private void sendSUBJCommand() throws IOException, Exception {
		if (subject != null) {
			state = STATE_SUBJ;
			String msg = "SUBJ " + subject;
			handleResponse(conn.send(msg));
		} else {
			sendMESSCommand();
		}
	}


	private void sendMESSCommand() throws IOException, Exception {
		state = STATE_MESS;
		if ((level >= 2)&&(message == null)) {
			sendDATACommand();
		} else {
			String msg = "MESS " + message;
			handleResponse(conn.send(msg));
		}
	}


	private void sendSENDCommand() throws IOException, Exception {
		state = STATE_SEND;
		handleResponse(conn.send("SEND"));
	}


	private void sendQUITCommand() throws IOException, Exception {
		state = STATE_QUIT;
		handleResponse(conn.send("QUIT"));
	}


	private void sendLOGICommand() throws IOException, Exception {
		if (login != null) {
			state = STATE_LOGI;
            String msg = "LOGI "+ login + ((password != null) ? " " + password : "");
			handleResponse(conn.send(msg));
		} else {
			sendCOVECommand();
		}
	}


	private void sendDATACommand() throws IOException, Exception {
		state = STATE_DATA;
		handleResponse(conn.send("DATA"));
	}


	private void sendDATAData() throws IOException, Exception {
		for (int i=0; i < data.length; i++) {
			conn.send(data[i], false);
		}
		handleResponse(conn.send("."));
	}


	private void errorOut(String badResponse) throws Exception {
		throw new Exception (badResponse);
	}
}
