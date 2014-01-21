package com.asetune.test;


public class HtmlParserTest
{
	public static void main(String[] args)
	{
		String urlStr = "http://google.com";
		if (args.length >= 1)
		{
			urlStr = args[0];
			if ( ! urlStr.startsWith("http://") )
				urlStr = "http://" + urlStr;
		}
			
		System.out.println("Usage: progname url");
		System.out.println("URL: "+urlStr);

System.out.println("DOESN'T WORK, NEED TO UNCOMMENT");

//		Document doc;
//		try
//		{
//			// need http protocol
//			doc = Jsoup.connect(urlStr).get();
//
//			// get page title
//			String title = doc.title();
//			System.out.println("title : " + title);
//
//			// get all links
//			Elements links = doc.select("a[href]");
//			for (Element link : links)
//			{
//
//				// get the value from href attribute
//				System.out.println("\nlink : " + link.attr("href"));
//				System.out.println("text : " + link.text());
//
//			}
//
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
	}
}
