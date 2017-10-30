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

import java.io.IOException;
import java.net.UnknownHostException;

public class SNPPExample {

	public static void main(String[] args) 
	{
		Message m = new Message("test.mysnppserver.com", 444, "don", "The system is DOWN.");
		try {
			m.send();
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Error sending message: " + e.getMessage());
		}
	}
}
