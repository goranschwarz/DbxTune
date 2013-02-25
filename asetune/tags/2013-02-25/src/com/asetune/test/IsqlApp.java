package com.asetune.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.sybase.jdbcx.Debug;
import com.sybase.jdbcx.DynamicClassLoader;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybDriver;

public class IsqlApp
{
    // Severity state for displaying detailed error message info
    static final int DB_BASE_ERR = 11; 

    static final String TAG_SEPARATOR = ":";
    static final String DEFAULT_USER = "sa";
    static final String DEFAULT_PASSWORD = "";
//    static final String DEFAULT_SERVER = "jdbc:sybase:Tds:127.0.0.1:5000";
    static final String DEFAULT_SERVER = "jdbc:sybase:Tds:127.0.0.1:15700";

    static Connection _con = null;
    static Statement _stmt = null;
    static ResultSet _rs = null;
    static ResultSetMetaData _rsmd = null;
    static BufferedReader _dis = null;
    static String _user = DEFAULT_USER;
    static String _password = DEFAULT_PASSWORD;
    static String _server = DEFAULT_SERVER;
    static String _gateway = null;
    static String _charset = null;
    static String _protocol = null;
    static String _language = null;
    static String _sessionID = null;
    static String _version = null;
    static String _inputFile = null;
    static String _tagStart = null;
    static String _tagEnd = null;
    static String _sectionName = null;
    static boolean _inSection = false;
    static String _commandTerminator = null;
    static boolean _verbose = false;
    static Properties _tagList = null;
    static boolean _noexit = false;
    static boolean _escapeProcessing = true;
    static boolean _dynamicLoader = false;
    private static String _propValue = null; // Current property value
    private static Debug _debug = null;

    public static void main(String args[])
    {
        int exitCode = doIsql(args);
        if (!_noexit)
        {
            System.exit(exitCode);
        }
    }

    private static int doIsql(String args[])
    {
        resetOptions();
        _tagList = new Properties();

        try
        {
            SybDriver sybDriver = null;
            // Robust Load
            try
            {
                // First try to load the 5.x driver
                sybDriver = (SybDriver) Class.forName(
                    "com.sybase.jdbc3.jdbc.SybDriver").newInstance();
            }
            catch (Exception ex41)
            {
                if(ex41 instanceof java.lang.InstantiationException ||
                ex41 instanceof java.lang.ClassNotFoundException)
                {
                    // Oh, well - try to load the 4.x driver
                    sybDriver = (SybDriver) Class.forName(
                        "com.sybase.jdbc.SybDriver").newInstance();
                }
                else
                throw ex41; // let someone else handle it
            }

            _debug = sybDriver.getDebug();

            if (!processCommandline(args))
            {
                System.out.println(
                    "Syntax:\n" +
                    "\tIsqlApp [-U <username>] [-P <password>] [-S <servername>]\n" +
                    "\t\t[-G <gateway>] [-p <http|https>] [-D <debug-class-list>]\n" +
                    "\t\t[-C <charset>] [-L <language>] [-T <sessionID>] \n" +
                    "\t\t[-v] [-I <input command file>] [-c <command terminator>] \n" + 
                    "\t\t[-s <starting tag/section marker>] [-e <ending tag/section marker>] \n" + 
                    "\t\t[-t <tag name> <new value>] [-n <section name to execute>] \n" + 
                    "\t\t[-N] [-V <version {2,3,4,5}] -d\n");
                return (1);
            }

            // get the input command stream
            if (_inputFile != null)
            {
                try
                {
                    _dis = new BufferedReader(new FileReader(_inputFile));
                }
                catch (FileNotFoundException fnfe)
                {
                    System.out.println("Unable to open " + _inputFile + "\n\t"
                        + fnfe.toString());
                    return (1);
                }
            }
            else
            {
                _dis = new BufferedReader(new InputStreamReader(System.in));
            }

            Properties props = new Properties();
            props.put("user", _user);
            props.put("password", _password);
            if (_charset != null)
            {
                props.put("CHARSET", _charset);
            }
            if (_protocol != null)
            {
                props.put("CONNECT_PROTOCOL", _protocol);
            }
            if (_language != null)
            {
                props.put("LANGUAGE", _language);
            }
            if (_gateway != null)
            {
                props.put("proxy", _gateway);
            }
            if (_sessionID != null)
            {
                props.put("SESSION_ID", _sessionID);
            }
            if (_version != null)
            {
                props.put("JCONNECT_VERSION", _version);
            }
            if (_dynamicLoader)
            {
                Properties p2 = (Properties) props.clone();
                String dlServer = _server;

                // if PROTOCOL_CAPTURE is on - disable it for the
                // dynamic loader -- we'll trash the output file that is
                // supposed to have normal traffic - not class loading activity.
                // look out for it either in Properties
                p2.remove("PROTOCOL_CAPTURE");

                // and look out for it in the server URL
                int start = _server.indexOf("PROTOCOL_CAPTURE");
                if (start != -1)
                {
                    dlServer = _server.substring(0, start);
                    int end = _server.indexOf("&", start);
                    if (end != -1)
                    {
                        String remainder = _server.substring(end + 1);
                        dlServer += remainder;
                    }
                    if (dlServer.endsWith("?") || dlServer.endsWith("&"))
                    {
                        dlServer = dlServer.substring(0, dlServer.length() -1);
                    }
                }
                DynamicClassLoader dl = sybDriver.getClassLoader(_server ,p2);
                props.put("CLASS_LOADER", dl);
            }
            _con = DriverManager.getConnection(_server, props);
            printExceptions(_con.getWarnings());

            _stmt = _con.createStatement();
            _stmt.setEscapeProcessing(_escapeProcessing);
            _con.clearWarnings();
            while (true)
            {
                int linecount = 1;
                try
                {
                    StringBuffer query = new StringBuffer();
                    if (_inputFile == null)
                    {
                        System.out.print("\nEnter a query:\n");
                    }
                    while (true)
                    {
                        if (_inputFile == null)
                        {
                            System.out.print(linecount++ + " > ");
                            System.out.flush();
                        }
                        String line = _dis.readLine();
                        if (line == null || line.equals("quit")) 
                        {
                            close();
                            return (0);
                        }

                        if (_commandTerminator == null)
                        {
                            appendLine(query, line);
                            break;
                        }
                        else if (line.trim().equals(_commandTerminator.trim()))
                        {
                            break;
                        }
                        appendLine(query, "\n");
                        appendLine(query, line);
                    }
                    if (query.toString().length() == 0) continue;
                    if (_inputFile != null || _verbose)
                    {
                        System.out.println("executing: " + query.toString());
                    }
                    boolean results = _stmt.execute(query.toString());
                    int rsnum = 0;
                    int rowsAffected = 0;
                    do
                    {
                        printExceptions(_stmt.getWarnings());
                        _stmt.clearWarnings();
                        if (results)
                        {
                            rsnum++;
                            _rs = _stmt.getResultSet();
                            printExceptions(_rs.getWarnings());
                            _rs.clearWarnings();
                            _rsmd = _rs.getMetaData();
                            int numColumns = _rsmd.getColumnCount();
                            System.out.println("\n------------------ Result set " 
                                + rsnum + " -----------------------\n");
                            StringBuffer column = new StringBuffer("Columns:");
                            for (int i = 1; i <= numColumns; i++)
                            {
                                column.append("\t" + _rsmd.getColumnName(i));
                            }
                            System.out.println(column.toString());
                            for(int rowNum = 1; _rs.next(); rowNum++)
                            {
                                printExceptions(_rs.getWarnings());
                                _rs.clearWarnings();
                                column = new StringBuffer("[ " + rowNum + "]");
                                for (int i = 1; i <= numColumns; i++)
                                {
                                    column.append("\t" + _rs.getString(i));
                                }
                                System.out.println(column.toString());
                            }
                            // jConnect will return #Rows selected at
                            // the end of a result set and before the
                            // next getMoreResults call.
                            int rowsSelected = _stmt.getUpdateCount();
                            if (rowsSelected >= 0)
                            {
                                System.out.println(rowsSelected + " rows Affected.");
                            }
                        }
                        else
                        {
                            rowsAffected = _stmt.getUpdateCount();
                            printExceptions(_stmt.getWarnings());
                            _stmt.clearWarnings();
                            if (rowsAffected >= 0)
                            {
                                System.out.println(rowsAffected + " rows Affected.");
                            }
                        }
                        results = _stmt.getMoreResults();
                    }
                    while (results || rowsAffected != -1);
                }
                catch (SQLException sqe)
                {
                    printExceptions(sqe);
                    _stmt.cancel();
                    _stmt.clearWarnings();
                }
            }
        }
        catch (SQLException sqe)
        {
            printExceptions(sqe);
            close();
            return (1);
        }
        catch (Exception e)
        {
            System.out.println("Unexpected exception : " +
                e.toString());
            e.printStackTrace();
            close();
            return (1);
        }
    }
    static private void close()
    {
        try
        {
            if (_con != null)
            {
                _con.close();
                _con = null;
            }
        }
        catch (SQLException sqe)
        {
            System.out.println("Unexpected exception : " +
                sqe.toString() + ", sqlstate = " + sqe.getSQLState());
            sqe.printStackTrace();
        }
    }
    //
    static private boolean processCommandline(String args[])
    {
        //* DONE
        String arg;
        String tagname;
        int errorCount = 0;
        for (int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if (arg.regionMatches(0, "-", 0, 1))
            {
                try 
                {
                    switch(arg.charAt(1))
                    {
                        case 'D':
                            i += parseArguments(args, i);
                            try
                            {
                                if(_propValue != null)
                                {
                                    _debug.debug(true, _propValue);
                                }
                                else
                                {
                                    errorCount++;
                                }
                            }
                            catch (IOException ioe)
                            {
                                // ignore
                            }
                            break;
                        case 'c':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _commandTerminator = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'a':
                            _escapeProcessing = false;
                            break;
                        case 'd':
                            _dynamicLoader = true;
                            break;
                        case 'U':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _user = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'P':
                            i += parseArguments(args, i);
                            _password =(_propValue == null ? "" : _propValue);
                            break;
                        case 'G':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _gateway = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'C':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _charset = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'L':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _language = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'S':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _server = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'T':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _sessionID = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'I':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _inputFile = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'p':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _protocol = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'V':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _version = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'v':
                            _verbose = true;
                            break;
                        case 'N':
                            _noexit = true;
                            break;
                        case 's':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _tagStart = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'e':
                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _tagEnd = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 'n':

                            i += parseArguments(args, i);
                            if(_propValue != null)
                            {
                                _sectionName = _propValue;
                            }
                            else
                            {
                                errorCount++;
                            }
                            break;
                        case 't':
                            _tagList.put(args[++i], args[++i]);

                            //Eliminated the parseArguments in this case
                            //because it was trying to parse arguments for each value for t
                            //as if -t arg1 -t arg2  which is wrong.
                            break;
                        default:
                            System.out.println("Invalid command line option: " + arg);
                            errorCount++;
                            break;
                    }
                }
                catch (ArrayIndexOutOfBoundsException aioobe)
                {
                    System.out.println("missing option argument");
                    errorCount++;
                }
            }
            else
            {
                // The syntax has no non "-" arguments
                errorCount++;
            }
        }
        return(errorCount == 0);
    }

    static private void appendLine(StringBuffer query, String line)
    {
        if (_tagStart != null && _tagEnd != null)
        {
            if (_sectionName != null)
            {
                /* 
                ** check if the current line contains a section marker
                **
                ** section marker formats:
                **
                **     <tagStart>SECTION BEGIN: <sectionName><tagEnd>
                **     <tagStart>SECTION END: <sectionName><tagEnd>
                **
                **     sectionName - Name of the section.  (example: CLEANUP)
                **
                */
                String lookFor = _tagStart + "SECTION " + 
                    (_inSection ? "END: " : "BEGIN: ") +
                    _sectionName + _tagEnd;
                if (line.trim().equals(lookFor))
                {
                    _inSection = !_inSection;
                }
                if (!_inSection)
                {
                    return;
                }
            }
            /* 
            ** check if the current line contains a tag marker
            **
            ** tag marker format:
            **
            **     <tagStart><tagName>:<replaceSearch><tagEnd>
            **
            **     tagName       - tag name
            **     replaceSearch - Text to be replaced on the current
            **                     line.
            */
            int indexStart = line.indexOf(_tagStart);
            int indexEnd = line.indexOf(_tagEnd);
            if ((indexStart != -1) && (indexEnd > indexStart))
            {
                // search for the tagSeparator
                int indexSeparator = line.indexOf(TAG_SEPARATOR, indexStart);
                if (indexSeparator != -1 
                && (indexSeparator > (indexStart + _tagStart.length())) 
                    && (indexEnd > (indexSeparator + 1)))
                {
                    // check if the user wants this one replaced
                    String tagName = line.substring(indexStart + 
                        _tagStart.length(), indexSeparator);
                    String newValue = (String) _tagList.get(tagName);
                    if (newValue != null)
                    {
                        // get the string to search for (replaceSearch)
                        String replaceSearch = line.substring(indexSeparator + 1, indexEnd);

                        int indexLast = -1;

                        // search the string and replace all all occurances
                        while(true)
                        {
                            int indexSearch = line.indexOf(replaceSearch);
                            if (indexSearch == -1 || indexSearch <= indexLast)
                            {
                                break;
                            }
                            indexLast = indexSearch;
                            // replace the search value with the new value
                            line = ((indexSearch == 0) ? "" : line.substring(0, indexSearch))
                                + newValue + ((indexSearch + replaceSearch.length() > 
                                line.length()) ? "" : line.substring(indexSearch + 
                                replaceSearch.length(), line.length()));
                        }
                    }
                }
            }
        }
        query.append(line);
    }

    static private void printExceptions(SQLException sqe)
    {
        while (sqe != null)
        {

            if(sqe instanceof EedInfo)
            {

                // Error is using the addtional TDS error data.
                EedInfo eedi = (EedInfo) sqe;
                if(eedi.getSeverity() >= DB_BASE_ERR)
                {
                    boolean firstOnLine = true;
                    System.out.println("Msg " + sqe.getErrorCode() +
                        ", Level " + eedi.getSeverity() + ", State " +
                        eedi.getState() + ":");

                    if( eedi.getServerName() != null)
                    {
                        System.out.print(", Server " + eedi.getServerName());
                        firstOnLine = false;
                    }
                    if(eedi.getProcedureName() != null)
                    {
                        System.out.print( (firstOnLine ? "" : ", ") +
                            "Procedure " + eedi.getProcedureName());
                        firstOnLine = false;
                    }
                    System.out.println( (firstOnLine ? "" : ", ") +
                        "Line " + eedi.getLineNumber() +
                        ", Status " + eedi.getStatus() + 
                        ", TranState " + eedi.getTranState() + ":");
                }
                // Now print the error or warning
                System.out.println(sqe.getMessage());

            }
            else
            {

                System.out.println("Unexpected exception : " +
                    "SqlState: " + sqe.getSQLState()  +
                    " " + sqe.toString() +
                    ", ErrorCode: " + sqe.getErrorCode());
            }
            sqe = sqe.getNextException();
        }
    }

    static public void resetOptions()
    {
        _user = DEFAULT_USER;
        _password = DEFAULT_PASSWORD;
        _server = DEFAULT_SERVER;
        _gateway = null;
        _charset = null;
        _protocol = null;
        _language = null;
        _sessionID = null;
        _version = null;
        _inputFile = null;
        _tagStart = null;
        _tagEnd = null;
        _sectionName = null;
        _inSection = false;
        _commandTerminator = null;
        _verbose = false;
        _tagList = null;
        _noexit = false;
    }

    /**
    *  Parse a command line argument.  Arguments may be supplied in
    *  2 different ways:<p>
    *  -Uusername<br>
    *  -U username<br>
    *  @param    argv     Array of command line arguments
    *  @param    pos      Current argument argv position
    *  @return            A value of 1 or 0 which is used to update our loop
    *                     counter.
    */
    private static int parseArguments(String argv[],  int pos)
    {
        int argc = argv.length-1; // # arguments specified
        String   arg = argv[pos].substring(1);
        int argLen  = arg.length(); // Length of arg
        int incrementValue = 0;

        if(argLen > 1)
        {
            // The argument value follows (i.e.  -Uusername)
            _propValue = arg.substring(1);
        }
        else
        {
            if( pos == argc || argv[pos+1].regionMatches(0, "-", 0, 1) )
            {
                // We are either at the last argument or the next option
                // starts with '-'.
                _propValue= null;
            }
            else
            {
                // The argument value is the next argument (i.e.  -U username)
                _propValue = argv[pos+1];
                incrementValue= 1;
            }
        }

        return(incrementValue);
    }
    // END -- parseArguments()

}
