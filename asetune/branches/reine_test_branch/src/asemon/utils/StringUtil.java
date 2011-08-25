/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * A String utility class.
 *
 * @author Robin Pedersen
 */
public class StringUtil
{
    /**
     * Counts the number of occurrances of the String <code>occurs</code> in the
     * String <code>s</code>.
     *
     * @param occurs The String to count
     * @param s The String to count in
     */
    public static int count(String occurs, String s)
    {
      int count = 0;
      int current = -1;
      int next = 0;
      next = s.indexOf(occurs);
      while ( next > -1)
      {
          count++;
          current = next;
          next = s.indexOf(occurs, current + 1);
      }
      return count;
    }

    /**
     * Split the String s into an array of strings. The String s is split at
     * each occurrence of the substring <code>splitAt</code> in <code>s</code>.
     * <p>
     * Example:
     * The string <tt>s = "onextwoxthreexfour"</tt>, with <tt>splitAt = "x"</tt>
     * is split into an array equivalent to <tt>{"one", "two", "three", "four"}
     * </tt>.
     *
     * @param splitAt Indicates where to split the string s
     * @param s String to split
     */
    public static String[] split(String splitAt, String s)
    {
      int noOfLines = count(splitAt, s) + 1;
      java.lang.String[] stringArray = new String[noOfLines];
      String remaining = s;
      int pos;
      int splitLen = splitAt.length();

      for (int i = 0; i < noOfLines - 1; i++)
      {
          pos = remaining.indexOf(splitAt);
          stringArray[i] = remaining.substring(0,pos);
          remaining = remaining.substring(pos + splitLen, remaining.length());
      }

      // last element
      stringArray[noOfLines - 1] = remaining;
      return stringArray;
    }

    /**
     */
    public static String word(String str, int number)
    {
    	String[] stra = str.split("[ \t\n\f\r]");
    	if (stra.length < number)
    	{
    		return null;
    		//throw new IndexOutOfBoundsException("This string only consists of "+stra.length+", while you want to read word "+number);
    	}
    	return stra[number];
    }
    /**
     */
    public static String lastWord(String str)
    {
    	String[] stra = str.split("[ \t\n\f\r]");
    	return stra[ stra.length - 1 ];
    }

    /**
     * Left justify a string and fill extra spaces to the right
     */
    public static String left(String str, int num)
    {
    	if (str.length() > num)
    		num = str.length();

    	int maxPadSize = num - str.length();
    	String space = "                                                                                                                                                                                                                                                               ";
    	while (space.length() < maxPadSize)
    		space += "                                                                                                                                                                                                                                                               ";

    	return (str + space).substring(0, num);
    }
    /**
     * Right justify a string and fill extra spaces to the left
     */
    public static String right(String str, int num)
    {
    	int len = num - str.length();
    	String space = "                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   ";
    	return space.substring(0, len) + str;
    }


    /**
     * All ${ENV_VARIABLE_NAME} will be substrituted with the environment variable
     * from the OperatingSystem. Some JVM's the System.getenv() does not work<br>
     * In that case go and get the value from the runtime instead...
     *
     * @param val A string that should be substituted
     * @return    The substituted string
     */
    public static String envVariableSubstitution(String val)
    {
		// Extract Environment variables
		// search for ${ENV_NAME}
		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}");
		while( compiledRegex.matcher(val).find() )
		{
			String envVal  = null;
			String envName = val.substring( val.indexOf("${")+2, val.indexOf("}") );

			// Get value for a specific env variable
			// But some java runtimes does not do getenv(),
			// then we need to revert back to getProperty() from the system property
			// then the user needs to pass that as a argument -Dxxx=yyy to the JVM
			try
			{
				envVal  = System.getenv(envName);
			}
			catch (Throwable t)
			{
				envVal = System.getProperty(envName);
				if (envVal == null)
				{
					System.out.println("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
				}
			}
			if (envVal == null)
			{
				System.out.println("The Environment variable '"+envName+"' cant be found, replacing it with an empty string ''.");
				envVal="";
			}
			// Backslashes does not work that good in replaceFirst()...
			// So change them to / instead...
			envVal = envVal.replace('\\', '/');

			// NOW substityte the ENV VARIABLE with a real value...
			val = val.replaceFirst("\\$\\{"+envName+"\\}", envVal);
		}

		return val;
    }


	public static String fill(String str, int fill)
	{
		if (str.length() < fill)
		{
			// A long string: 256 chars
			String fillStr = "                                                                                                                                                                                                                                                               ";
			return (str + fillStr).substring(0,fill);
		}
		return str;
	}


	/**
	 * Add newlines to the input string.
	 * @param str Input string
	 * @param rowLength The approx row length
	 * @param wordCountHysteresis If there are only ## words left in the input string.... keep them on the same line
	 * @param lineBreak The string used to make the new line.
	 * @return The in string with line wraps added.
	 */
	public static String makeApproxLineBreak(String str, int rowLength, int wordCountHysteresis, String lineBreak)
	{
		if (str.length() <= rowLength)
			return str;

		// ok chop the string into peaces of about (length) on each row
		StringBuffer sb = new StringBuffer();

		char[] ca = str.toCharArray();
		int c   = 0; // char position in the string
		int car = 0; // Char At Row
		while ( c < ca.length )
		{
			sb.append(ca[c]);
			c++; car++;
			// should we make a line break
			if (car >= rowLength)
			{
				// If next "space" is in reach, break of the line
				// But if it's "out of reach", make a new line at once
				int nextSpacePos = str.substring(c).trim().indexOf(' '); 
				if (nextSpacePos > 0 && nextSpacePos < Math.max(15, rowLength/4))
				{
					// Look ahead to get next "space"
					while ( (c < ca.length) && ca[c] != ' ' )
					{
						sb.append(ca[c]);
						c++;
					}
					
					// Here we could also check if we got more than X words left on the line
					// then we could choose to include them on "this" row, or make a new row for them.
					if (str.substring(c).split("\\s", wordCountHysteresis+1).length <= wordCountHysteresis)
					{
						while ( c < ca.length )
						{
							sb.append(ca[c]);
							c++;
						}
					}
					else
					{
						sb.append(lineBreak);
					}
				}
				else
				{
					sb.append(lineBreak);
				}
				car = 0;
			}
		}
		return sb.toString();
	}

	public static String getHostname()
	{
		try 
		{
			InetAddress addr = InetAddress.getLocalHost();
			
			// Get IP Address
			//byte[] ipAddr = addr.getAddress();

			// Get hostname
			String hostname = addr.getHostName();

			return hostname;
		}
		catch (UnknownHostException e) 
		{
			return null;
		}
	}


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    //// TEST CODE
    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args)
    {
		System.out.println("TEST: StringUtil BEGIN.");

		if ( ! StringUtil.lastWord(" 1 2 3 ").equals("3") )
    		System.out.println("FAILED:  test-1: StringUtil.lastWord()");

    	if ( ! StringUtil.lastWord("").equals("") )
    		System.out.println("FAILED:  test-2: StringUtil.lastWord()");

    	if ( ! StringUtil.lastWord(" 1 2\t 3 ").equals("3") )
    		System.out.println("FAILED:  test-3: StringUtil.lastWord()");

    	if ( ! StringUtil.lastWord(" 1 2 \n3").equals("3") )
    		System.out.println("FAILED:  test-4: StringUtil.lastWord()");

    	if ( ! StringUtil.left("123", 5).equals("123  ") )
    		System.out.println("FAILED:  test-1: StringUtil.left()");

    	if ( ! StringUtil.right("  123", 5).equals("  123") )
    		System.out.println("FAILED:  test-1: StringUtil.right()");

		System.out.println("TEST: StringUtil END.");
    }
}
