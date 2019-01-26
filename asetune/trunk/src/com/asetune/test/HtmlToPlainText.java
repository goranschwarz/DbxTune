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
package com.asetune.test;


// Gorans home page
// java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText http://gorans.no-ip.org

// RepServer Commands
// java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273713895825.html

// command: configure replication server
// java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273714017856.html

//command: HANA ...
//java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText http://help.sap.com/hana/html/_asql_functions_data_type_conversion.html
//java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText http://help.sap.com/hana/html/sql_function_to_integer.html

// try some SQL-Server DMV's
// https://msdn.microsoft.com/en-us/library/ms173786.aspx
// java -cp .\classes;"C:/Documents and Settings/gorans/My Documents/Downloads/java/html_parser/jsoup/jsoup-1.7.3.jar" com.asetune.test.HtmlToPlainText https://msdn.microsoft.com/en-us/library/ms173786.aspx

/**
 * HTML to plain-text. This example program demonstrates the use of jsoup to convert HTML input to lightly-formatted
 * plain-text. That is divergent from the general goal of jsoup's .text() methods, which is to get clean data from a
 * scrape.
 * <p/>
 * Note that this is a fairly simplistic formatter -- for real world use you'll want to embrace and extend.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class HtmlToPlainText 
{
	public static void main(String[] args)
	{
	}
//    public static void main(String... args) throws IOException {
//        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
//        String url = args[0];

//		Document doc;
//		try
//		{
//			String urlStr = "http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc32410.1571100/doc/html/san1273713895825.html";
//
//			Connection conn = Jsoup.connect(urlStr);
//			doc = conn.get();
//			String htmlBody = doc.body().html();
//			String safeHtmlBody = Jsoup.clean(htmlBody, Whitelist.basic());
//
//			System.out.println("###############################################################");
//			System.out.println("htmlbody:");
//			System.out.println("###############################################################");
//			System.out.println(htmlBody);
//
//			System.out.println("###############################################################");
//			System.out.println("safeHtmlBody:");
//			System.out.println("###############################################################");
//			System.out.println(safeHtmlBody);
//
//			System.exit(1);
//			
//			URL url = new URL(urlStr);
////			Object xxx = url.openConnection().getContent();
////			System.out.println("XXX="+xxx);
////			if (xxx instanceof InputStream)
////			{
////				
////			}
//			StringBuilder sb = new StringBuilder();
//
//			InputStream is = url.openStream();
//			Reader rd = new InputStreamReader(is); 
//			BufferedReader br = new BufferedReader(rd);
//			String tmp=br.readLine();
//			while ( tmp!=null )
//			{
//				sb.append(tmp).append("\n");
////				System.out.println(tmp);
//				tmp=br.readLine();
//			}
//			is.close(); 
//
//			String safe = Jsoup.clean(sb.toString(), Whitelist.basic());
//			System.out.println("SAFE: \n"+safe);
//
//
//			System.exit(1);

			
//			// need http protocol
//			Connection conn = Jsoup.connect(urlStr);
//			doc = conn.get();
//			doc.
////	    	String unsafe = "<p><a href='http://example.com/' onclick='stealCookies()'>Link</a></p>";
//			String unsafe = doc.
//	   		String safe = Jsoup.clean(unsafe, Whitelist.basic());
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
    		
//System.out.println("DOESN'T WORK, NEED TO UNCOMMENT");
//	public static void main(String... args) throws IOException 
//	{
//		Validate.isTrue(args.length == 1, "usage: supply url to fetch");
//		String url = args[0];
//		
//		// fetch the specified URL and parse to a HTML DOM
//		Document doc = Jsoup.connect(url).get();
//	
//		HtmlToPlainText formatter = new HtmlToPlainText();
//		String plainText = formatter.getPlainText(doc);
//		System.out.println(plainText);
//	}
//
//    /**
//     * Format an Element to plain-text
//     * @param element the root element to format
//     * @return formatted text
//     */
//    public String getPlainText(Element element) {
//        FormattingVisitor formatter = new FormattingVisitor();
//        NodeTraversor traversor = new NodeTraversor(formatter);
//        traversor.traverse(element); // walk the DOM, and call .head() and .tail() for each node
//
//        return formatter.toString();
//    }
//
//    // the formatting rules, implemented in a breadth-first DOM traverse
//    private class FormattingVisitor 
//    implements NodeVisitor 
//    {
//        private static final int maxWidth = 80;
//        private int width = 0;
//        private StringBuilder accum = new StringBuilder(); // holds the accumulated text
//
//        // hit when the node is first seen
//        @Override
//		public void head(Node node, int depth) {
//            String name = node.nodeName();
//            if (node instanceof TextNode)
//                append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
//            else if (name.equals("li"))
//                append("\n * ");
//        }
//
//        // hit when all of the node's children (if any) have been visited
//        @Override
//		public void tail(Node node, int depth) {
//            String name = node.nodeName();
//            if (name.equals("br"))
//                append("\n");
//            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5"))
//                append("\n\n");
//            else if (name.equals("a"))
//                append(String.format(" <%s>", node.absUrl("href")));
//        }
//
//        // appends text to the string builder with a simple word wrap method
//        private void append(String text) {
//            if (text.startsWith("\n"))
//                width = 0; // reset counter if starts with a newline. only from formats above, not in natural text
//            if (text.equals(" ") &&
//                    (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
//                return; // don't accumulate long runs of empty spaces
//
//            if (text.length() + width > maxWidth) { // won't fit, needs to wrap
//                String words[] = text.split("\\s+");
//                for (int i = 0; i < words.length; i++) {
//                    String word = words[i];
//                    boolean last = i == words.length - 1;
//                    if (!last) // insert a space if not the last word
//                        word = word + " ";
//                    if (word.length() + width > maxWidth) { // wrap and reset counter
//                        accum.append("\n").append(word);
//                        width = word.length();
//                    } else {
//                        accum.append(word);
//                        width += word.length();
//                    }
//                }
//            } else { // fits as is, without need to wrap text
//                accum.append(text);
//                width += text.length();
//            }
//        }
//
//        @Override
//		public String toString() {
//            return accum.toString();
//        }
//    }
}
