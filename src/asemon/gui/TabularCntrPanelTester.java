/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import org.apache.log4j.PropertyConfigurator;

import asemon.gui.swing.GTabbedPane;
import asemon.utils.AseConnectionFactory;
import asemon.utils.Configuration;



/**
 */
public class TabularCntrPanelTester extends JFrame 
{

//	private JDesktopPane samplesFrame = new JDesktopPane();
//	private JList list = new JList();
	
    private static final long serialVersionUID = 1L;

	JTabbedPane _tabs = null;

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

        _tabs = new GTabbedPane();
		_tabs.add(  "0-Summary",     new TabularCntrPanel("0-Summary") );
		_tabs.add(  "1-Object",      new TabularCntrPanel("1-Object") );
		_tabs.add(  "2-Processes",   new TabularCntrPanel("2-Processes") );
		_tabs.add(  "3-Databases",   new TabularCntrPanel("3-Databases") );
		_tabs.add(  "4-Waits",       new TabularCntrPanel("4-Waits") );
		_tabs.add(  "5-Engines",     new TabularCntrPanel("5-Engines") );
		_tabs.add(  "6-Data Caches", new TabularCntrPanel("6-Data Caches") );
		_tabs.add(  "7-Pools",       new TabularCntrPanel("7-Pools") );
		_tabs.add(  "8-Devices",     new TabularCntrPanel("8-Devices") );
		
		add(_tabs);

		JPanel xxx = new JPanel();
		xxx.add(new JLabel("label_adksj"));
		xxx.add(new JButton("button_adksj"));
		_tabs.add("xxx", xxx);

		
		_tabs.add("yyy", new JButton("yyy_button_adksj"));
		
	}
	


	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
				} catch (Exception e) {
					// TODO Auto-generated catch block			
					e.printStackTrace();
				}
				
				Properties log4jProps = new Properties();
				//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
				log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
				log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
				log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
				log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
				PropertyConfigurator.configure(log4jProps);

				Configuration conf1 = new Configuration("c:\\projects\\asemon\\asemon.save.properties");
				Configuration.setInstance(Configuration.TEMP, conf1);

				Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
				Configuration.setInstance(Configuration.CONF, conf2);

				AseConnectionFactory.setHost("goransxp");
				AseConnectionFactory.setPort(5000);
				AseConnectionFactory.setUser("sa");
				AseConnectionFactory.setPassword("");
				AseConnectionFactory.setAppName("AseMon-TabularCntrlPanelTester");


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
