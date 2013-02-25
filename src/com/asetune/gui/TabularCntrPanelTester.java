/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.plaf.TabbedPaneUI;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.Version;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;




/**
 */
public class TabularCntrPanelTester extends JFrame 
{

//	private JDesktopPane samplesFrame = new JDesktopPane();
//	private JList list = new JList();
	
    private static final long serialVersionUID = 1L;

	JTabbedPane _tabs = null;
	JTabbedPane _tabs1 = null;
	JTabbedPane _tabs2 = null;
	JTabbedPane _tabs3 = null;
	JTabbedPane _tabs4 = null;
	JTabbedPane _tabs5 = null;
	JTabbedPane _tabs6 = null;
	JTabbedPane _tabs7 = null;
	boolean _showTabsHeader4 = true;

	/**
	 * Constructor
	 */
	public TabularCntrPanelTester(String title) 
	{
		super(title);
		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/table_16.gif"));
		setIconImage(icon.getImage());

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
		
//		MigLayout layout = new MigLayout();
//		setLayout(layout);

//        _tabs = new JTabbedPane();
//      _tabs = new GTabbedPane();
//      _tabs = new CloseableTabbedPane();
        _tabs = new XXTabbedPane();
        _tabs1 = new XXTabbedPane();
        _tabs2 = new XXTabbedPane();
        _tabs3 = new XXTabbedPane();
        _tabs4 = new XXTabbedPane();
        _tabs5 = new XXTabbedPane();
        _tabs6 = new XXTabbedPane();
        _tabs7 = new XXTabbedPane();
//		_tabs.setUI(new TestPlaf(_tabs.getUI()));
        System.out.println("XXXXXXXX: UIClassID="+_tabs.getUIClassID()+", UI="+_tabs.getUI());
//		_tabs.add(  "0-Summary",     new TabularCntrPanel("0-Summary") );
//		_tabs.add(  "1-Object",      new TabularCntrPanel("1-Object") );
//		_tabs.add(  "2-Processes",   new TabularCntrPanel("2-Processes") );
//		_tabs.add(  "3-Databases",   new TabularCntrPanel("3-Databases") );
//		_tabs.add(  "4-Waits",       new TabularCntrPanel("4-Waits") );
//		_tabs.add(  "5-Engines",     new TabularCntrPanel("5-Engines") );
//		_tabs.add(  "6-Data Caches", new TabularCntrPanel("6-Data Caches") );
//		_tabs.add(  "7-Pools",       new TabularCntrPanel("7-Pools") );
//		_tabs.add(  "8-Devices",     new TabularCntrPanel("8-Devices") );

		_tabs1.add(  "Objects",              new TabularCntrPanel("Objects") );
		_tabs1.add(  "Cached Procedures",    new TabularCntrPanel("Cached Procedures") );
		_tabs1.add(  "Procedure Call Stack", new TabularCntrPanel("Procedure Call Stack") );
		_tabs1.add(  "Active Objects",       new TabularCntrPanel("Active Objects") );
		_tabs1.add(  "Active Statements",    new TabularCntrPanel("Active Statements") );
		_tabs1.add(  "Blocking",             new TabularCntrPanel("Blocking") );
		_tabs1.add(  "Missing Statistics",   new TabularCntrPanel("Missing Statistics") );
		_tabs1.add(  "QP Metrics",           new TabularCntrPanel("QP Metrics") );
		
		_tabs2.add(  "Processes",        new TabularCntrPanel("Processes") );
		_tabs2.add(  "SPID Wait",        new TabularCntrPanel("SPID Wait") );
		_tabs2.add(  "Databases",        new TabularCntrPanel("Databases") );
		_tabs2.add(  "Temp Db",          new TabularCntrPanel("Temp Db") );
		_tabs2.add(  "Waits",            new TabularCntrPanel("Waits") );
		_tabs2.add(  "Engines",          new TabularCntrPanel("Engines") );
		_tabs2.add(  "System Load",      new TabularCntrPanel("System Load") );
		_tabs2.add(  "Spinlock Sum",     new TabularCntrPanel("Spinlock Sum") );
		_tabs2.add(  "Sysmon Raw",       new TabularCntrPanel("Sysmon Raw") );
		_tabs2.add(  "RepAgent",         new TabularCntrPanel("RepAgent") );
		_tabs2.add(  "Errorlog",         new TabularCntrPanel("Errorlog") );
		_tabs2.add(  "Deadlock",         new TabularCntrPanel("Deadlock") );
		_tabs2.add(  "Lock Timeout",     new TabularCntrPanel("Lock Timeout") );
		_tabs2.add(  "sp_monitorconfig", new TabularCntrPanel("sp_monitorconfig") );

		_tabs3.add(  "Devices",     new TabularCntrPanel("Devices") );
		_tabs3.add(  "IO Sum",      new TabularCntrPanel("IO Sum") );
		_tabs3.add(  "IO Queue",    new TabularCntrPanel("IO Queue") );

		_tabs4.add(  "Caches",                   new TabularCntrPanel("Caches") );
		_tabs4.add(  "Pools",                    new TabularCntrPanel("Pools") );
		_tabs4.add(  "Procedure Cache Load",     new TabularCntrPanel("Procedure Cache Load") );
		_tabs4.add(  "Cached Objects",           new TabularCntrPanel("Cached Objects") );
		_tabs4.add(  "Proc Cache Module Usage",  new TabularCntrPanel("Proc Cache Module Usage") );
		_tabs4.add(  "Proc Cache Memeory Usage", new TabularCntrPanel("Proc Cache Memeory Usage") );
		_tabs4.add(  "Statement Cache",          new TabularCntrPanel("Statement Cache") );
		_tabs4.add(  "Statement Cache Details",  new TabularCntrPanel("Statement Cache Details") );

		_tabs5.add(  "OS Disk Stat",     new TabularCntrPanel("OS Disk Stat") );
		_tabs5.add(  "OS CPU(vmstat)",   new TabularCntrPanel("OS CPU(vmstat)") );
		_tabs5.add(  "OS CPU(mpstat)",   new TabularCntrPanel("OS CPU(mpstat)") );

//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );
//		_tabs.add(  "",   new TabularCntrPanel("") );


		_tabs.add(  "Summary",  new TabularCntrPanel("Summary") );
		_tabs.add(  "Objects/Access",       _tabs1 );
		_tabs.add(  "Server",        _tabs2 );
		_tabs.add(  "Disk",          _tabs3 );
		_tabs.add(  "Caches",        _tabs4 );
		_tabs.add(  "Host Monitor",  _tabs5 );
//		_tabs.add(  "Miscellaneous", _tabs6 );
		_tabs.add(  "User Defined",  _tabs7 );
		
		
		add(_tabs);
		
//		_tabs.setUI(new BasicTabbedPaneUI() 
//		{  
//			@Override  
//			protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) 
//			{
//				System.out.println("calculateTabAreaHeight() tabPlacement="+tabPlacement);
////				if (tabPlacement == 4 && _showTabsHeader4) 
////				{
//					return super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);  
////				} 
////				else 
////				{
////					return 0;
////				}
//			}
//			@Override
//			protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) 
//			{
//				System.out.println("paintTab() tabPlacement="+tabPlacement);
////				if (tabPlacement == 4 && _showTabsHeader4) 
////				{
//					super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);  
////				}
//			}
//		});
		
		JPanel xxx = new JPanel();
		xxx.add(new JLabel("Trace Test"));
		JButton traceTest_but = new JButton("Trace Test Buttom");
		JButton enableDisableTest_but = new JButton("Enable/Disable tab '4-Waits'");
		xxx.add(traceTest_but);
		xxx.add(enableDisableTest_but);
		traceTest_but.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if (_tabs instanceof XXTabbedPane)
					((XXTabbedPane)_tabs).xxx();
			}
		});
		enableDisableTest_but.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
//				_tabs.setEnabledAt(4, !_tabs.isEnabledAt(4));
//				_tabs.getTabComponentAt(4).setVisible( !_tabs.getTabComponentAt(4).isVisible() );
				_showTabsHeader4 = ! _showTabsHeader4;
			}
		});
		_tabs.add("xxx", xxx);

		
		_tabs.add("yyy", new JButton("yyy_button_adksj"));
		
	}
	
	private class XXTabbedPane extends JTabbedPane
	{
		private static final long	serialVersionUID	= 1L;
		//		public void paintComponent(Graphics g) 
//		{
//			super.paintComponent(g);
//			xxx();
//		}
//		public void paintBorder(Graphics g) 
//		{
//			super.paintBorder(g);
//			xxx();
//		}
//	    int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
		public void xxx() 
		{
//		    int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
//		    int tabCount = getUI().getTabRunCount(this);
		    int tabCount = getTabCount();
		    for (int t=0; t<tabCount; t++)
		    {
		    	getComponentAt(t);
			    Rectangle r = getUI().getTabBounds(this, t);
			    Graphics g = this.getGraphics().create(r.x, r.y, r.width, r.height);
			    System.out.println("getTabBounds(xxx,"+t+"): Rectangle="+r+", g="+g);
			    
			    doDummyPaint((Graphics2D)g);
		    }
		    
		}
		private void doDummyPaint(Graphics2D g) 
		{
			Rectangle r = g.getClipBounds();
		    System.out.println("doDummyPaint(Rectangle="+r);
			g.setColor(Color.GREEN);
//			g.drawLine(0, 0, 10, 10);
			int pX = r.width - 4;
			int pY = 3;
			int pW = 2;
			int pH = r.height - 3;
//			int pX = r.x;
//			int pY = r.y;
//			int pW = r.width;
//			int pH = r.height;
//		    g.drawRect(pX, pY, pW, pH);
		    g.fillRect(pX, pY, pW, pH);
		    System.out.println("pX="+pX+", pY="+pY+", pW="+pW+", pH="+pH+"");
		}
//		Rectangle r = getDecorationBounds();
//		Graphics2D g = (Graphics2D)graphics;
//		GeneralPath triangle = new GeneralPath();
//		triangle.moveTo(r.x + SIZE/2, r.y);
//		triangle.lineTo(r.x + SIZE-1, r.y + SIZE-1);
//		triangle.lineTo(r.x, r.y + SIZE-1);
//		triangle.closePath();
//		g.setColor(Color.yellow);
//		g.fill(triangle);
//		g.setColor(Color.black);
//		g.draw(triangle);
//		g.drawLine(r.x + SIZE/2, r.y + 3, r.x + SIZE/2, r.y + SIZE*3/4 - 2);
//		g.drawLine(r.x + SIZE/2, r.y + SIZE*3/4+1, r.x + SIZE/2, r.y + SIZE - 4);
		
	}
	static class TestPlaf 
//	extends BasicTabbedPaneUI
	extends TabbedPaneUI
	{
		TabbedPaneUI dc = null;
		TestPlaf(TabbedPaneUI tpUI)
		{
			dc = tpUI;
		}
//		public Dimension getMaximumSize(JComponent c) {
//			return dc.getMaximumSize(c);
//		}
//
//		public Dimension getMinimumSize(JComponent c) {
//			return dc.getMinimumSize(c);
//		}

		@Override
		public Rectangle getTabBounds(JTabbedPane pane, int i) {
			return dc.getTabBounds(pane, i);
		}

		@Override
		public int getTabRunCount(JTabbedPane pane) {
			return dc.getTabRunCount(pane);
		}

//		public void installUI(JComponent c) {
//			dc.installUI(c);
//		}

		@Override
		public void paint(Graphics g, JComponent c) {
			dc.paint(g, c);
		}
	    @Override
		public void update(Graphics g, JComponent c) {
			dc.update(g, c);
        }

		@Override
		public int tabForCoordinate(JTabbedPane pane, int x, int y) {
			return dc.tabForCoordinate(pane, x, y);
		}

//		public void uninstallUI(JComponent c) {
//			dc.uninstallUI(c);
//		}
		//override to return our layoutmanager
//		protected LayoutManager createLayoutManager()
//		{
//			LayoutManager layoutManager = super.createLayoutManager();
//			System.out.println("super.createLayoutManager() = "+layoutManager);
//			return new TestPlafLayout();
//		}
//
//		//add 40 to the tab size to allow room for the close button and 8 to the height
//		protected Insets getTabInsets(int tabPlacement,int tabIndex)
//		{
//			//note that the insets that are returned to us are not copies.
//			Insets defaultInsets = (Insets)super.getTabInsets(tabPlacement,tabIndex).clone();
//			defaultInsets.right += 40;
//			defaultInsets.top += 4;
//			defaultInsets.bottom += 4;
//			return defaultInsets;
//		}

//		class TestPlafLayout extends TabbedPaneLayout
//		{
//			//a list of our close buttons
//			java.util.ArrayList closeButtons = new java.util.ArrayList();
//
//			public void layoutContainer(Container parent)
//			{
//				super.layoutContainer(parent);
//				//ensure that there are at least as many close buttons as tabs
//				while(tabPane.getTabCount() > closeButtons.size())
//				{
//					closeButtons.add(new CloseButton(closeButtons.size()));
//				}
//				Rectangle rect = new Rectangle();
//				int i;
//				for(i = 0; i < tabPane.getTabCount();i++)
//				{
//					rect = getTabBounds(i,rect);
//					JButton closeButton = (JButton)closeButtons.get(i);
//					//shift the close button 3 down from the top of the pane and 20 to the left
//					closeButton.setLocation(rect.x+rect.width-20,rect.y+5);
//					closeButton.setSize(15,15);
//					tabPane.add(closeButton);
//				}
//
//				for(;i < closeButtons.size();i++)
//				{
//					//remove any extra close buttons
//					tabPane.remove((JButton)closeButtons.get(i));
//				}
//			}
//
//			// implement UIResource so that when we add this button to the 
//			// tabbedpane, it doesn't try to make a tab for it!
//			class CloseButton extends JButton implements javax.swing.plaf.UIResource
//			{
//				public CloseButton(int index)
//				{
//					super(new CloseButtonAction(index));
//					setToolTipText("Close this tab");
//
//					//remove the typical padding for the button
//					setMargin(new Insets(0,0,0,0));
//					addMouseListener(new MouseAdapter()
//					{
//						public void mouseEntered(MouseEvent e)
//						{
//							setForeground(new Color(255,0,0));
//						}
//						public void mouseExited(MouseEvent e)
//						{
//							setForeground(new Color(0,0,0));
//						}
//					});
//				}
//			}
//
//			class CloseButtonAction extends AbstractAction
//			{
//				int index;
//				public CloseButtonAction(int index)
//				{
//					super("x");
//					this.index = index;
//				}
//
//				public void actionPerformed(ActionEvent e)
//				{
//					tabPane.remove(index);
//				}
//			}	// End of CloseButtonAction
//		}	// End of TestPlafLayout
	}	// End of static class TestPlaf


	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try 
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				
				Properties log4jProps = new Properties();
				//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
				log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
				log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
				log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
				log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
				PropertyConfigurator.configure(log4jProps);

				Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
				Configuration.setInstance(Configuration.USER_TEMP, conf1);

				Configuration conf2 = new Configuration("c:\\projects\\asetune\\asetune.properties");
				Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);

				AseConnectionFactory.setHostPort("goransxp", "5000");
				AseConnectionFactory.setUser("sa");
				AseConnectionFactory.setPassword("");
				AseConnectionFactory.setAppName(Version.getAppName()+"-TabularCntrlPanelTester");


				TabularCntrPanelTester frame = new TabularCntrPanelTester("MigLayout Samples");
//				frame.setMinimumSize(   new Dimension(500,500) );
//				frame.setMaximumSize(   new Dimension(500,500) );
//				frame.setPreferredSize( new Dimension(500,500) );
				frame.setSize(1000, 600);
				frame.setVisible(true);
			}
		});
	}
}
