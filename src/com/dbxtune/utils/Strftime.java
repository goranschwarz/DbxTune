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
package com.dbxtune.utils;

/*
 * Strftime.java
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/util/Strftime.java,v 1.2 2002/05/11 05:43:56 billbarker Exp $
 * $Revision: 1.2 $
 * $Date: 2002/05/11 05:43:56 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

/* 
 * Or possibly use the following:
 * https://cs.android.com/android/platform/superproject/+/master:external/robolectric-shadows/utils/src/main/java/org/robolectric/util/Strftime.java;l=8
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Converts dates to strings using the same format specifiers as strftime
 *
 * Note: This does not mimic strftime perfectly.  Certain strftime commands, 
 *       are not supported, and will convert as if they were literals.
 *
 *       Certain complicated commands, like those dealing with the week of the year
 *       probably don't have exactly the same behavior as strftime.
 *
 *       These limitations are due to use SimpleDateTime.  If the conversion was done
 *       manually, all these limitations could be eliminated.
 *
 *       The interface looks like a subset of DateFormat.  Maybe someday someone will make this class
 *       extend DateFormat.
 *
 * @author Bip Thelin
 * @author Dan Sandberg
 * @version $Revision: 1.2 $, $Date: 2002/05/11 05:43:56 $
 */
public class Strftime {
    protected static Properties translate;
    protected SimpleDateFormat simpleDateFormat;

    /**
     * Initialize our pattern translation
     */
    static {
        translate = new Properties();
        translate.put("a","EEE");
        translate.put("A","EEEE");
        translate.put("b","MMM");
        translate.put("B","MMMM");
        translate.put("c","EEE MMM d HH:mm:ss yyyy");

        //There's no way to specify the century in SimpleDateFormat.  We don't want to hard-code
        //20 since this could be wrong for the pre-2000 files.
        //translate.put("C", "20");
        translate.put("d","dd");
        translate.put("D","MM/dd/yy");
        translate.put("e","dd"); //will show as '03' instead of ' 3'
        translate.put("F","yyyy-MM-dd");
        translate.put("g","yy");
        translate.put("G","yyyy");
        translate.put("H","HH");
        translate.put("h","MMM");
        translate.put("I","hh");
        translate.put("j","DDD");
        translate.put("k","HH"); //will show as '07' instead of ' 7'
        translate.put("l","hh"); //will show as '07' instead of ' 7'
        translate.put("m","MM");
        translate.put("M","mm");
        translate.put("n","\n");
        translate.put("p","a");
        translate.put("P","a");  //will show as pm instead of PM
        translate.put("r","hh:mm:ss a");
        translate.put("R","HH:mm");
        //There's no way to specify this with SimpleDateFormat
        //translate.put("s","seconds since ecpoch");
        translate.put("S","ss");
        translate.put("t","\t");
        translate.put("T","HH:mm:ss");
        //There's no way to specify this with SimpleDateFormat
        //translate.put("u","day of week ( 1-7 )");

        //There's no way to specify this with SimpleDateFormat
        //translate.put("U","week in year with first sunday as first day...");

        translate.put("V","ww"); //I'm not sure this is always exactly the same

        //There's no way to specify this with SimpleDateFormat
        //translate.put("W","week in year with first monday as first day...");

        //There's no way to specify this with SimpleDateFormat
        //translate.put("w","E");
        translate.put("X","HH:mm:ss");
        translate.put("x","MM/dd/yy");
        translate.put("y","yy");
        translate.put("Y","yyyy");
        translate.put("Z","z");
        translate.put("z","Z");
        translate.put("%","%");
    }


    /**
     * Create an instance of this date formatting class
     *
     * @see #Strftime( String, java.util.Locale )
     */
    public Strftime( String origFormat ) {
        String convertedFormat = convertDateFormat( origFormat );
        simpleDateFormat = new SimpleDateFormat( convertedFormat );
    }

    /**
     * Create an instance of this date formatting class
     * 
     * @param origFormat the strftime-style formatting string
     * @param the locale to use for locale-specific conversions
     */
    public Strftime( String origFormat, Locale locale ) {
        String convertedFormat = convertDateFormat( origFormat );
        simpleDateFormat = new SimpleDateFormat( convertedFormat, locale );
    }

    /**
     * Format the date according to the strftime-style string given in the constructor.
     *
     * @param date the date to format
     * @return the formatted date
     */
    public String format( Date date ) {
        return simpleDateFormat.format( date );
    }

    /**
     * Get the timezone used for formatting conversions
     *
     * @return the timezone
     */
    public TimeZone getTimeZone() {
        return simpleDateFormat.getTimeZone();
    }

    /**
     * Change the timezone used to format dates
     *
     * @see java.util.TimeZone#setTimeZone
     */
    public void setTimeZone( TimeZone timeZone ) {
        simpleDateFormat.setTimeZone( timeZone );
    }

    /**
     * Search the provided pattern and get the C standard
     * Date/Time formatting rules and convert them to the
     * Java equivalent.
     *
     * @param pattern The pattern to search
     * @return The modified pattern
     */
    protected String convertDateFormat( String pattern ) {
        boolean inside = false;
        boolean mark = false;
        boolean modifiedCommand = false;

        StringBuffer buf = new StringBuffer();

        for(int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);

            if ( c=='%' && !mark ) {
                mark=true;
            } else {
                if ( mark ) {
                    if ( modifiedCommand ) {
                        //don't do anything--we just wanted to skip a char
                        modifiedCommand = false;
                        mark = false;
                    } else {
                        inside = translateCommand( buf, pattern, i, inside );
                        //It's a modifier code
                        if ( c=='O' || c=='E' ) {
                            modifiedCommand = true;
                        } else {
                            mark=false;
                        }
                    }
                } else {
                    if ( !inside && c != ' ' ) {
                        //We start a literal, which we need to quote
                        buf.append("'");
                        inside = true;
                    }
                    
                    buf.append(c);
                }
            }
        }

        if ( buf.length() > 0 ) {
            char lastChar = buf.charAt( buf.length() - 1 );

            if( lastChar!='\'' && inside ) {
                buf.append('\'');
            }
        }
        return buf.toString();
    }

    protected String quote( String str, boolean insideQuotes ) {
        String retVal = str;
        if ( !insideQuotes ) {
            retVal = '\'' + retVal + '\'';
        }
        return retVal;
    }

    /**
     * try to get the Java Date/Time formating associated with
     * the C standard provided
     *
     * @param c The C equivalent to translate
     * @return The Java formatting rule to use
     */
    protected boolean translateCommand( StringBuffer buf, String pattern, int index, boolean oldInside ) {
        char firstChar = pattern.charAt( index );
        boolean newInside = oldInside;

        //O and E are modifiers, they mean to present an alternative representation of the next char
        //we just handle the next char as if the O or E wasn't there
        if ( firstChar == 'O' || firstChar == 'E' ) {
            if ( index + 1 < pattern.length() ) {               
                newInside = translateCommand( buf, pattern, index + 1, oldInside );
            } else {
                buf.append( quote("%" + firstChar, oldInside ) );
            }
        } else {
            String command = translate.getProperty( String.valueOf( firstChar ) );
            
            //If we don't find a format, treat it as a literal--That's what apache does
            if ( command == null ) {
                buf.append( quote( "%" + firstChar, oldInside ) );
            } else {
                //If we were inside quotes, close the quotes
                if ( oldInside ) {
                    buf.append( '\'' );
                }
                buf.append( command );
                newInside = false;
            }
        }
        return newInside;
    }
}