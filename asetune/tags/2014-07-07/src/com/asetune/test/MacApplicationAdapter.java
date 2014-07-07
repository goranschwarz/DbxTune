package com.asetune.test;

//import javax.swing.JOptionPane;
//import com.apple.eawt.ApplicationAdapter;
//import com.apple.eawt.ApplicationEvent;
//
//public class MacApplicationAdapter
//extends ApplicationAdapter
//{
//	private DesktopShield	handler;
//
//	public MacApplicationAdapter(DesktopShield handler)
//	{
//		this.handler = handler;
//	}
//
//	public void handleQuit(ApplicationEvent e)
//	{
//		System.exit(0);
//	}
//
//	public void handleAbout(ApplicationEvent e)
//	{
//		// tell the system we're handling this, so it won't display
//		// the default system "about" dialog after ours is shown.
//		e.setHandled(true);
//		JOptionPane.showMessageDialog(null, "Show About dialog here");
//	}
//
//	public void handlePreferences(ApplicationEvent e)
//	{
//		JOptionPane.showMessageDialog(null, "Show Preferences dialog here");
//	}
//}

//import com.apple.eawt.AboutHandler;
//import com.apple.eawt.AppEvent;
//import com.apple.eawt.AppEvent.QuitEvent;
//import com.apple.eawt.Application;
//import com.apple.eawt.QuitHandler;
//import com.apple.eawt.QuitResponse;
//import com.gui.Tabs;
//
//public class MacApplicationAdapter implements AboutHandler, QuitHandler
//{
//	// Constructor to register/install the necessary handler's
//	public MacApplicationAdapter()
//	{
//		Application.getApplication().setAboutHandler(this);
//		Application.getApplication().setQuitHandler(this);
//	}
//
//	// Implemented method to catch the About menu item
//	@Override
//	public void handleAbout(AppEvent.AboutEvent e)
//	{
//		Tabs.switchAbout();
//	}
//
//	// Implemented method to catch the Quit menu item
//	@Override
//	public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1)
//	{
//		Tabs.quit();
//	}
//}

public class MacApplicationAdapter
{
}
