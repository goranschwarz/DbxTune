package asemon.rstat;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcPortmapClient;
import org.acplt.oncrpc.OncRpcProtocols;


public class RstatSample {

	private static Logger _logger          = Logger.getLogger(RstatSample.class);
	private rstatClient   client           = null;
	private boolean       isPortmapAlive   = false;
    OncRpcPortmapClient   portmap          = null;
    String                _hostname        = null;
    statstime             sample           = null;

	public RstatSample(String hostname)
	{
		_hostname = hostname;
		_init();
	}
	
	public boolean isConnected()
	{
		return isPortmapAlive;
	}
	
	private void _init()
	{
		isPortmapAlive = true;
		client = null;
		try
		{

	        try {
		        portmap = new OncRpcPortmapClient(InetAddress.getByName(_hostname),OncRpcProtocols.ONCRPC_TCP, 1000);
	            portmap.ping();
	        }
	        catch (java.net.ConnectException ce) {
	    		isPortmapAlive = false;
	    		return;
	        }
	        catch ( OncRpcException e ) {
	    		isPortmapAlive = false;
	    		return;
	        }
	        
//			client = new rstatClient(InetAddress.getByName("sekax010.epk.ericsson.se"), rstat.RSTATPROG, rstat.RSTATVERS_TIME, 32780,  OncRpcProtocols.ONCRPC_UDP);
			client = new rstatClient(InetAddress.getByName(_hostname), OncRpcProtocols.ONCRPC_UDP);
			
	        client.getClient().setTimeout(10*1000);
			
			if(portmap != null)
				portmap.close();
		}
		catch(Exception e)
		{
    		isPortmapAlive = false;
		}
	}

	public int getUser() {return sample.cp_time[0];}

	public int getSystem() {return sample.cp_time[1];}

	public int getWait() {return sample.cp_time[2];}

	public int getIdle() {return sample.cp_time[3];}

	public statstime getSampleData() {return sample;}
	
	/**
	 * @param args
	 */
	public void getSample()  
	{
		double interval = 0;
		
		if( !isConnected() )
		{
			_logger.warn("Not connected to service");
			return;
		}

        try
        {
        	sample = client.RSTATPROC_STATS_3();
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("User  :  " + sample.cp_time[0] + "\n");
		sb.append("System:  " + sample.cp_time[1] + "\n");
		sb.append("Wait  :  " + sample.cp_time[2] + "\n");
		sb.append("Idle  :  " + sample.cp_time[3]);
		
		return sb.toString();
	}
	/**
	 * Note: Comparing to samples done within 1 second seem to indicate the resolution on
	 * Solaris 9 is 1 second.
	 * @param newSample
	 * @return diff in milliseconds
	 */
	public long getSampleTimeMs(RstatSample newSample)
	{
		//Use ms as unit.
		long old = (long)sample.curtime.tv_sec * 1000 + (long)sample.curtime.tv_usec/1000;
//		System.out.println("old: " + sample.curtime.tv_sec + "." + sample.curtime.tv_usec);
//		System.out.println("new: " + newSample.getSampleData().curtime.tv_sec + "." + newSample.getSampleData().curtime.tv_usec);
		long now = (long)newSample.getSampleData().curtime.tv_sec * 1000 + (long)newSample.getSampleData().curtime.tv_usec/1000;
		return (now - old);
	}
	public Timestamp getSampleTime()
	{
		//Use ms as unit.
		long tmms = (long)sample.curtime.tv_sec * 1000 + (long)sample.curtime.tv_usec/1000;
		return new Timestamp(tmms);
	}
	public Timestamp getBootTime()
	{
		//Use ms as unit.
		long tmms = (long)sample.boottime.tv_sec * 1000 + (long)sample.boottime.tv_usec/1000;
		return new Timestamp(tmms);
	}
	/**
	 *
	 * @param newSample
	 * @return Value between 0 and 1.
	 */
	public double getDiffCPULoad(RstatSample newSample)
	{
		int user1, system1, wait1, idle1, user2, system2, wait2, idle2;
		int userDiff, systemDiff, waitDiff, idleDiff;
		
		user1      = this.getUser();
		system1    = this.getSystem();
		wait1      = this.getWait();
		idle1      = this.getIdle();
		
		user2      = newSample.getUser();
		system2    = newSample.getSystem();
		wait2      = newSample.getWait();
		idle2      = newSample.getIdle();
		
		userDiff   = user2 - user1;
		systemDiff = system2 - system1;
		waitDiff   = wait2 - wait1;
		idleDiff   = idle2 - idle1;
		int sum = (userDiff + systemDiff + waitDiff + idleDiff);
		if(sum == 0)
			sum = 1;
		return (userDiff + systemDiff + waitDiff)*1.0/sum;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int user1, system1, wait1, idle1, user2, system2, wait2, idle2;
		int userDiff, systemDiff, waitDiff, idleDiff;
		long sleepTime = 1000;
		String hostname = args[0];
		RstatSample cpuSample1 = new RstatSample(hostname);
		RstatSample cpuSample2 = new RstatSample(hostname);
		
		if(cpuSample1.isConnected())
		{
			
			for(int i = 0; i < 5; i++)
			{
				cpuSample1.getSample();
				mySleep(sleepTime);
				cpuSample2.getSample();
				NumberFormat df = DecimalFormat.getPercentInstance();
				System.out.print("BootTimeStamp " + cpuSample2.getBootTime()+ " ");
				System.out.print("CurrTimeStamp " + cpuSample2.getSampleTime()+ " ");
				System.out.print(df.format(cpuSample1.getDiffCPULoad(cpuSample2)) + " SampleTimeMs=" + cpuSample1.getSampleTimeMs(cpuSample2)+" ");
				System.out.println("avenrun: " + cpuSample2.getSampleData().avenrun[0]/256.0 + " " + cpuSample2.getSampleData().avenrun[1]/256.0 + " " + cpuSample2.getSampleData().avenrun[2]/256.0 );
			}
			
		}
	}

	private static void mySleep(long sleepTimeMs) {
		try{Thread.sleep(sleepTimeMs);}catch(Exception e){}
	}

}


//System.out.println("avenrun: " + sample.avenrun[0]/256.0 + " " + sample.avenrun[1]/256.0 + " " + sample.avenrun[2]/256.0 );
//

//interval = (sample2.curtime.tv_sec + 
//		sample2.curtime.tv_usec/1000000.0) - 
//		(sample.curtime.tv_sec + 
//				sample.curtime.tv_usec/1000000.0); 

//System.out.println("Interval :  " + interval + "\n");

//int userDiff = (sample2.cp_time[0]-sample.cp_time[0]) ;
//int systemDiff = (sample2.cp_time[1]-sample.cp_time[1]) ;
//int waitDiff = (sample2.cp_time[2]-sample.cp_time[2]) ;
//int idleDiff = (sample2.cp_time[3]-sample.cp_time[3]) ;

//calcCpu = ( 
//		(userDiff + systemDiff + waitDiff)*100.0 / 
//		(userDiff + systemDiff + waitDiff + idleDiff)
//		);
